<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';
require_once dirname(__DIR__) . '/api/commerce.php';

$loginError = null;
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && ($_POST['action'] ?? '') === 'login') {
    if (!verify_csrf($_POST['csrf_token'] ?? null)) {
        $loginError = 'Session de connexion invalide. Rechargez la page.';
    } elseif (verify_admin_login((string) ($_POST['username'] ?? ''), (string) ($_POST['password'] ?? ''))) {
        try {
            audit_admin_action(db(), 'admin_login', 'admin', current_admin_username());
        } catch (Throwable $exception) {
            error_log('SmartVision admin login audit failed.');
        }
        admin_redirect();
    } else {
        $loginError = 'Identifiants invalides ou connexion temporairement verrouillee.';
    }
}

if (!is_admin_authenticated()) {
    render_admin_login($loginError);
    exit;
}

$pdo = db();
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    if (!verify_csrf($_POST['csrf_token'] ?? null)) {
        http_response_code(403);
        exit('Requete invalide.');
    }
    try {
        handle_admin_action($pdo, (string) ($_POST['action'] ?? ''));
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('SmartVision admin action failed.');
        set_admin_flash('error', $exception instanceof InvalidArgumentException
            ? $exception->getMessage()
            : 'Action impossible. Verifiez les valeurs et reessayez.');
    }
    admin_redirect();
}

$query = mb_substr(trim((string) ($_GET['q'] ?? '')), 0, 80, 'UTF-8');
$stats = admin_load_stats($pdo);
$orders = admin_load_orders($pdo, $query);
$users = admin_load_users($pdo, $query);
$devices = admin_load_devices($pdo, $query);
$codes = admin_load_codes($pdo, $query);
$auditLogs = admin_load_audit($pdo);
$revenueSeries = admin_revenue_series($pdo);
$alerts = admin_build_alerts($stats);
$flash = consume_admin_flash();
$generatedCode = is_string($_SESSION['generated_activation_code'] ?? null)
    ? $_SESSION['generated_activation_code']
    : null;
unset($_SESSION['generated_activation_code']);

render_admin_dashboard(
    $stats,
    $orders,
    $users,
    $devices,
    $codes,
    $auditLogs,
    $revenueSeries,
    $alerts,
    $flash,
    $generatedCode,
    $query,
);

function handle_admin_action(PDO $pdo, string $action): void
{
    switch ($action) {
        case 'generate_code': admin_generate_code($pdo); break;
        case 'set_code_status': admin_set_code_status($pdo); break;
        case 'revoke_code': admin_revoke_code($pdo); break;
        case 'delete_code': admin_delete_code($pdo); break;
        case 'set_user_status': admin_set_user_status($pdo); break;
        case 'set_device_status': admin_set_device_status($pdo); break;
        case 'extend_activation': admin_extend_activation($pdo); break;
        case 'cancel_order': admin_cancel_order($pdo); break;
        default: throw new InvalidArgumentException('Action admin inconnue.');
    }
}

function admin_positive_int(mixed $value, int $max = PHP_INT_MAX): int
{
    $validated = filter_var($value, FILTER_VALIDATE_INT, [
        'options' => ['min_range' => 1, 'max_range' => $max],
    ]);
    if ($validated === false) {
        throw new InvalidArgumentException('Identifiant ou valeur invalide.');
    }
    return (int) $validated;
}

function admin_generate_code(PDO $pdo): void
{
    $label = mb_substr(trim((string) ($_POST['label'] ?? '')), 0, 100, 'UTF-8');
    $durationDays = admin_positive_int($_POST['duration_days'] ?? null, 36500);
    $maxDevices = admin_positive_int($_POST['max_devices'] ?? null, 1000);
    $validUntil = null;
    $validUntilInput = trim((string) ($_POST['valid_until'] ?? ''));
    if ($validUntilInput !== '') {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $validUntilInput, new DateTimeZone('UTC'));
        if (!$date) {
            throw new InvalidArgumentException('Date invalide.');
        }
        $validUntil = $date->setTime(23, 59, 59)->format('Y-m-d H:i:s');
    }

    $pdo->beginTransaction();
    $plainCode = null;
    $codeId = null;
    for ($attempt = 0; $attempt < 12; $attempt++) {
        $candidate = generate_smartvision_code();
        try {
            $statement = $pdo->prepare(
                "INSERT INTO activation_codes
                    (code_hash, label, duration_days, max_devices, used_devices, status, valid_until, created_at, updated_at)
                 VALUES (:code_hash, :label, :duration_days, :max_devices, 0, 'active', :valid_until, NOW(), NOW())"
            );
            $statement->execute([
                'code_hash' => activation_code_hash(normalize_activation_code($candidate)),
                'label' => $label === '' ? null : $label,
                'duration_days' => $durationDays,
                'max_devices' => $maxDevices,
                'valid_until' => $validUntil,
            ]);
            $plainCode = $candidate;
            $codeId = (int) $pdo->lastInsertId();
            break;
        } catch (Throwable $exception) {
            if (!is_duplicate_key($exception)) {
                throw $exception;
            }
        }
    }
    if ($plainCode === null || $codeId === null) {
        throw new RuntimeException('Generation de code impossible.');
    }

    $metadata = $pdo->prepare(
        "INSERT INTO activation_code_metadata (code_id, code_hint, created_by, last_used_at)
         VALUES (:code_id, :code_hint, :created_by, NULL)"
    );
    $metadata->execute([
        'code_id' => $codeId,
        'code_hint' => '******' . substr(normalize_activation_code($plainCode), -4),
        'created_by' => current_admin_username(),
    ]);
    audit_admin_action($pdo, 'activation_code_created', 'activation_code', (string) $codeId, [
        'duration_days' => $durationDays,
        'max_devices' => $maxDevices,
        'label' => $label,
    ]);
    $pdo->commit();
    $_SESSION['generated_activation_code'] = $plainCode;
    set_admin_flash('success', 'Code genere. Copiez-le avant de quitter cette page.');
}

function admin_set_code_status(PDO $pdo): void
{
    $codeId = admin_positive_int($_POST['code_id'] ?? null);
    $status = (string) ($_POST['status'] ?? '');
    if (!in_array($status, ['active', 'disabled'], true)) {
        throw new InvalidArgumentException('Statut de code invalide.');
    }
    $statement = $pdo->prepare('UPDATE activation_codes SET status = :status, updated_at = NOW() WHERE id = :id');
    $statement->execute(['status' => $status, 'id' => $codeId]);
    audit_admin_action($pdo, 'activation_code_status_changed', 'activation_code', (string) $codeId, ['status' => $status]);
    set_admin_flash('success', $status === 'active' ? 'Code reactive.' : 'Code desactive.');
}

function admin_revoke_code(PDO $pdo): void
{
    $codeId = admin_positive_int($_POST['code_id'] ?? null);
    $pdo->beginTransaction();
    $pdo->prepare("UPDATE activation_codes SET status = 'disabled', updated_at = NOW() WHERE id = :id")
        ->execute(['id' => $codeId]);
    $pdo->prepare(
        "UPDATE device_activations SET status = 'expired', expires_at = NOW()
         WHERE activation_code_id = :id AND status = 'active'"
    )->execute(['id' => $codeId]);
    $pdo->prepare(
        "UPDATE devices SET status = 'expired', expires_at = NOW(), updated_at = NOW()
         WHERE device_id IN (SELECT device_id FROM device_activations WHERE activation_code_id = :id)"
    )->execute(['id' => $codeId]);
    audit_admin_action($pdo, 'activation_code_revoked', 'activation_code', (string) $codeId);
    $pdo->commit();
    set_admin_flash('success', 'Code revoque et appareils associes expires.');
}

function admin_delete_code(PDO $pdo): void
{
    $codeId = admin_positive_int($_POST['code_id'] ?? null);
    $statement = $pdo->prepare(
        "DELETE FROM activation_codes
         WHERE id = :id AND used_devices = 0
           AND NOT EXISTS (SELECT 1 FROM activation_orders WHERE activation_code_id = :order_code)
           AND NOT EXISTS (SELECT 1 FROM device_activations WHERE activation_code_id = :activation_code)"
    );
    $statement->execute(['id' => $codeId, 'order_code' => $codeId, 'activation_code' => $codeId]);
    if ($statement->rowCount() !== 1) {
        throw new InvalidArgumentException('Un code lie a une commande ou deja utilise ne peut pas etre supprime.');
    }
    audit_admin_action($pdo, 'activation_code_deleted', 'activation_code', (string) $codeId);
    set_admin_flash('success', 'Code inutilise supprime.');
}

function admin_set_user_status(PDO $pdo): void
{
    $userId = admin_positive_int($_POST['user_id'] ?? null);
    $status = (string) ($_POST['status'] ?? '');
    if (!in_array($status, ['active', 'blocked'], true)) {
        throw new InvalidArgumentException('Statut client invalide.');
    }
    $pdo->prepare('UPDATE site_users SET status = :status, updated_at = NOW() WHERE id = :id')
        ->execute(['status' => $status, 'id' => $userId]);
    audit_admin_action($pdo, 'customer_status_changed', 'site_user', (string) $userId, ['status' => $status]);
    set_admin_flash('success', $status === 'blocked' ? 'Compte client bloque.' : 'Compte client reactive.');
}

function admin_set_device_status(PDO $pdo): void
{
    $deviceId = clean_device_id((string) ($_POST['device_id'] ?? ''));
    $status = (string) ($_POST['status'] ?? '');
    if ($deviceId === '' || !in_array($status, ['active', 'blocked'], true)) {
        throw new InvalidArgumentException('Appareil ou statut invalide.');
    }
    $pdo->beginTransaction();
    if ($status === 'blocked') {
        $pdo->prepare("UPDATE devices SET status = 'blocked', updated_at = NOW() WHERE device_id = :device_id")
            ->execute(['device_id' => $deviceId]);
        $pdo->prepare("UPDATE device_activations SET status = 'blocked' WHERE device_id = :device_id AND status = 'active'")
            ->execute(['device_id' => $deviceId]);
    } else {
        $pdo->prepare(
            "UPDATE device_activations SET status = 'active'
             WHERE device_id = :device_id AND status = 'blocked' AND expires_at > NOW()"
        )->execute(['device_id' => $deviceId]);
        $pdo->prepare(
            "UPDATE devices SET status = CASE WHEN expires_at > NOW() THEN 'active' ELSE 'expired' END, updated_at = NOW()
             WHERE device_id = :device_id"
        )->execute(['device_id' => $deviceId]);
    }
    audit_admin_action($pdo, 'device_status_changed', 'device', $deviceId, ['status' => $status]);
    $pdo->commit();
    set_admin_flash('success', $status === 'blocked' ? 'Appareil bloque.' : 'Appareil debloque.');
}

function admin_extend_activation(PDO $pdo): void
{
    $deviceId = clean_device_id((string) ($_POST['device_id'] ?? ''));
    $days = admin_positive_int($_POST['days'] ?? null, 3650);
    if ($deviceId === '') {
        throw new InvalidArgumentException('Appareil invalide.');
    }
    $pdo->beginTransaction();
    $statement = $pdo->prepare(
        "UPDATE device_activations
         SET expires_at = DATE_ADD(GREATEST(expires_at, NOW()), INTERVAL {$days} DAY), status = 'active'
         WHERE device_id = :device_id ORDER BY id DESC LIMIT 1"
    );
    $statement->execute(['device_id' => $deviceId]);
    if ($statement->rowCount() !== 1) {
        throw new InvalidArgumentException('Activation introuvable pour cet appareil.');
    }
    $expiresStatement = $pdo->prepare(
        "SELECT expires_at FROM device_activations WHERE device_id = :device_id ORDER BY id DESC LIMIT 1"
    );
    $expiresStatement->execute(['device_id' => $deviceId]);
    $expiresAt = $expiresStatement->fetchColumn();
    $pdo->prepare(
        "UPDATE devices SET status = 'active', expires_at = :expires_at, updated_at = NOW() WHERE device_id = :device_id"
    )->execute(['expires_at' => $expiresAt, 'device_id' => $deviceId]);
    audit_admin_action($pdo, 'activation_extended', 'device', $deviceId, ['days' => $days]);
    $pdo->commit();
    set_admin_flash('success', 'Activation prolongee de ' . $days . ' jours.');
}

function admin_cancel_order(PDO $pdo): void
{
    $orderId = admin_positive_int($_POST['order_id'] ?? null);
    $pdo->beginTransaction();
    $statement = $pdo->prepare(
        "SELECT activation_code_id FROM activation_orders WHERE id = :id AND status = 'paid' FOR UPDATE"
    );
    $statement->execute(['id' => $orderId]);
    $codeId = $statement->fetchColumn();
    if ($codeId === false) {
        throw new InvalidArgumentException('Commande payee introuvable.');
    }
    $usage = $pdo->prepare('SELECT used_devices FROM activation_codes WHERE id = :id');
    $usage->execute(['id' => $codeId]);
    if ((int) $usage->fetchColumn() > 0) {
        throw new InvalidArgumentException('Revoquez la licence avant d annuler une commande deja activee.');
    }
    $pdo->prepare("UPDATE activation_orders SET status = 'cancelled', updated_at = NOW() WHERE id = :id")
        ->execute(['id' => $orderId]);
    $pdo->prepare("UPDATE activation_codes SET status = 'disabled', updated_at = NOW() WHERE id = :id")
        ->execute(['id' => $codeId]);
    audit_admin_action($pdo, 'order_cancelled', 'activation_order', (string) $orderId);
    $pdo->commit();
    set_admin_flash('success', 'Commande annulee et licence desactivee.');
}

function admin_load_stats(PDO $pdo): array
{
    return [
        'revenue_30' => (int) $pdo->query("SELECT COALESCE(SUM(amount_cents), 0) FROM activation_orders WHERE status = 'paid' AND paid_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)")->fetchColumn(),
        'paid_orders' => (int) $pdo->query("SELECT COUNT(*) FROM activation_orders WHERE status = 'paid'")->fetchColumn(),
        'customers' => (int) $pdo->query("SELECT COUNT(*) FROM site_users WHERE status = 'active'")->fetchColumn(),
        'active_codes' => (int) $pdo->query("SELECT COUNT(*) FROM activation_codes WHERE status = 'active'")->fetchColumn(),
        'active_devices' => (int) $pdo->query("SELECT COUNT(*) FROM devices WHERE status = 'active' AND expires_at > NOW()")->fetchColumn(),
        'active_trials' => (int) $pdo->query("SELECT COUNT(*) FROM device_activations WHERE activation_type = 'trial_demo' AND status = 'active' AND expires_at > NOW()")->fetchColumn(),
        'expiring_soon' => (int) $pdo->query("SELECT COUNT(*) FROM device_activations WHERE status = 'active' AND expires_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)")->fetchColumn(),
        'pending_sessions' => (int) $pdo->query("SELECT COUNT(*) FROM activation_sessions WHERE status = 'pending' AND expires_at > NOW()")->fetchColumn(),
        'pending_orders' => (int) $pdo->query("SELECT COUNT(*) FROM activation_orders WHERE status = 'pending'")->fetchColumn(),
        'blocked_devices' => (int) $pdo->query("SELECT COUNT(*) FROM devices WHERE status = 'blocked'")->fetchColumn(),
    ];
}

function admin_search_pattern(string $query): string
{
    return '%' . str_replace(['%', '_'], ['\\%', '\\_'], $query) . '%';
}

function admin_load_orders(PDO $pdo, string $query): array
{
    $sql = "SELECT o.id, o.order_reference, o.plan_label, o.amount_cents, o.currency, o.status,
                   o.payment_provider, o.payment_reference, o.created_at, o.paid_at,
                   u.email, u.display_name, c.status AS code_status, c.used_devices, m.code_hint
            FROM activation_orders o
            JOIN site_users u ON u.id = o.user_id
            LEFT JOIN activation_codes c ON c.id = o.activation_code_id
            LEFT JOIN activation_code_metadata m ON m.code_id = c.id";
    $params = [];
    if ($query !== '') {
        $sql .= " WHERE o.order_reference LIKE :q OR u.email LIKE :q OR u.display_name LIKE :q OR o.payment_reference LIKE :q";
        $params['q'] = admin_search_pattern($query);
    }
    $sql .= ' ORDER BY o.id DESC LIMIT 80';
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    return $statement->fetchAll();
}

function admin_load_users(PDO $pdo, string $query): array
{
    $sql = "SELECT u.id, u.email, u.display_name, u.status, u.created_at, u.last_login_at,
                   COUNT(o.id) AS orders_count,
                   COALESCE(SUM(CASE WHEN o.status = 'paid' THEN o.amount_cents ELSE 0 END), 0) AS total_spent
            FROM site_users u LEFT JOIN activation_orders o ON o.user_id = u.id";
    $params = [];
    if ($query !== '') {
        $sql .= ' WHERE u.email LIKE :q OR u.display_name LIKE :q';
        $params['q'] = admin_search_pattern($query);
    }
    $sql .= ' GROUP BY u.id ORDER BY u.id DESC LIMIT 60';
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    return $statement->fetchAll();
}

function admin_load_devices(PDO $pdo, string $query): array
{
    $sql = "SELECT d.device_id, d.device_name, d.platform, d.app_version, d.status, d.last_seen_at, d.expires_at,
                   a.activation_type, a.activation_code_id,
                   CASE WHEN p.device_id IS NULL THEN 0 ELSE 1 END AS playlist_configured
            FROM devices d
            LEFT JOIN device_activations a ON a.id = (
                SELECT da.id FROM device_activations da WHERE da.device_id = d.device_id ORDER BY da.id DESC LIMIT 1
            )
            LEFT JOIN device_playlist_configs p ON p.device_id = d.device_id";
    $params = [];
    if ($query !== '') {
        $sql .= ' WHERE d.device_id LIKE :q OR d.device_name LIKE :q OR d.app_version LIKE :q';
        $params['q'] = admin_search_pattern($query);
    }
    $sql .= ' ORDER BY d.last_seen_at DESC, d.id DESC LIMIT 80';
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    return $statement->fetchAll();
}

function admin_load_codes(PDO $pdo, string $query): array
{
    $sql = "SELECT c.id, c.label, c.duration_days, c.max_devices, c.used_devices, c.status,
                   c.valid_until, c.created_at, m.code_hint, m.created_by, m.last_used_at
            FROM activation_codes c LEFT JOIN activation_code_metadata m ON m.code_id = c.id";
    $params = [];
    if ($query !== '') {
        $sql .= ' WHERE c.label LIKE :q OR m.code_hint LIKE :q OR m.created_by LIKE :q';
        $params['q'] = admin_search_pattern($query);
    }
    $sql .= ' ORDER BY c.id DESC LIMIT 100';
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    return $statement->fetchAll();
}

function admin_load_audit(PDO $pdo): array
{
    return $pdo->query(
        "SELECT admin_username, action, target_type, target_id, created_at
         FROM admin_audit_logs ORDER BY id DESC LIMIT 40"
    )->fetchAll();
}

function admin_revenue_series(PDO $pdo): array
{
    $rows = $pdo->query(
        "SELECT DATE(paid_at) AS day, SUM(amount_cents) AS revenue, COUNT(*) AS orders_count
         FROM activation_orders
         WHERE status = 'paid' AND paid_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
         GROUP BY DATE(paid_at) ORDER BY day"
    )->fetchAll();
    $byDay = [];
    foreach ($rows as $row) {
        $byDay[(string) $row['day']] = $row;
    }
    $series = [];
    for ($offset = 6; $offset >= 0; $offset--) {
        $day = gmdate('Y-m-d', strtotime('-' . $offset . ' days'));
        $series[] = [
            'day' => $day,
            'label' => gmdate('d/m', strtotime($day)),
            'revenue' => (int) ($byDay[$day]['revenue'] ?? 0),
            'orders' => (int) ($byDay[$day]['orders_count'] ?? 0),
        ];
    }
    return $series;
}

function admin_build_alerts(array $stats): array
{
    return [
        ['level' => 'warning', 'label' => 'Licences expirant sous 7 jours', 'value' => (int) $stats['expiring_soon']],
        ['level' => 'info', 'label' => 'Sessions TV en attente', 'value' => (int) $stats['pending_sessions']],
        ['level' => 'danger', 'label' => 'Appareils bloques', 'value' => (int) $stats['blocked_devices']],
        ['level' => 'info', 'label' => 'Commandes en attente', 'value' => (int) $stats['pending_orders']],
    ];
}

function render_admin_login(?string $error): void
{
    $credentials = admin_credentials();
    $configured = $credentials['username'] !== '' && $credentials['password_hash'] !== '';
    ?><!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Administration SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=2"></head>
<body class="admin-login-body"><main class="admin-login-panel">
    <a class="admin-brand" href="/"><img src="/assets/images/smartvision-mark.png" alt=""><span>Smart<strong>Vision</strong></span></a>
    <h1>Administration</h1><p>Commandes, licences et appareils SmartVision.</p>
    <?php if (!$configured): ?><div class="admin-notice error">Administration non configuree sur le serveur.</div><?php else: ?>
    <?php if ($error): ?><div class="admin-notice error"><?= admin_escape($error) ?></div><?php endif; ?>
    <form method="post" class="admin-stack-form"><input type="hidden" name="action" value="login"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label for="username">Identifiant</label><input id="username" name="username" autocomplete="username" required autofocus><label for="password">Mot de passe</label><input id="password" name="password" type="password" autocomplete="current-password" required><button class="admin-button primary" type="submit">Se connecter</button></form>
    <?php endif; ?>
</main></body></html><?php
}

function render_admin_dashboard(
    array $stats,
    array $orders,
    array $users,
    array $devices,
    array $codes,
    array $auditLogs,
    array $revenueSeries,
    array $alerts,
    ?array $flash,
    ?string $generatedCode,
    string $query,
): void {
    $maxRevenue = max(1, ...array_map(static fn(array $item): int => $item['revenue'], $revenueSeries));
    ?><!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Administration | SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=2"></head>
<body class="admin-body">
<aside class="admin-sidebar">
    <a class="admin-brand" href="/admin/"><img src="/assets/images/smartvision-mark.png" alt=""><span>Smart<strong>Vision</strong></span></a>
    <nav><a class="active" href="#overview">Vue d'ensemble</a><a href="#orders">Commandes</a><a href="#customers">Clients</a><a href="#licenses">Licences</a><a href="#devices">Appareils</a><a href="#audit">Journal</a></nav>
    <div class="sidebar-footer"><a href="/" target="_blank" rel="noopener">Voir le site</a><form method="post" action="/admin/logout.php"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><button type="submit">Deconnexion</button></form></div>
</aside>
<div class="admin-shell">
    <header class="admin-topbar"><form method="get" class="admin-search"><label class="sr-only" for="admin-search">Rechercher</label><input id="admin-search" name="q" value="<?= admin_escape($query) ?>" placeholder="Rechercher client, commande, licence ou appareil"><button type="submit">Rechercher</button></form><div class="admin-account"><span><?= admin_escape(current_admin_username()) ?></span><small>Administrateur</small></div></header>
    <main class="admin-main">
        <section class="admin-page-heading" id="overview"><div><h1>Vue d'ensemble</h1><p>Suivi commercial et operationnel SmartVision.</p></div><a class="admin-button secondary" href="/account/" target="_blank" rel="noopener">Voir le parcours client</a></section>
        <?php if ($flash): ?><div class="admin-notice <?= admin_escape((string) ($flash['type'] ?? '')) ?>"><?= admin_escape((string) ($flash['message'] ?? '')) ?></div><?php endif; ?>
        <?php if ($generatedCode): ?><div class="generated-license"><div><span>Nouveau code</span><strong id="generated-code"><?= admin_escape($generatedCode) ?></strong></div><button class="admin-button primary" type="button" data-copy-target="generated-code">Copier</button></div><?php endif; ?>

        <section class="admin-kpis" aria-label="Indicateurs">
            <?= admin_kpi('Revenus 30 jours', commerce_money((int) $stats['revenue_30']), 'blue') ?>
            <?= admin_kpi('Commandes payees', $stats['paid_orders'], 'blue') ?>
            <?= admin_kpi('Clients actifs', $stats['customers'], 'cyan') ?>
            <?= admin_kpi('Licences actives', $stats['active_codes'], 'cyan') ?>
            <?= admin_kpi('Appareils actifs', $stats['active_devices'], 'green') ?>
            <?= admin_kpi('Essais en cours', $stats['active_trials'], 'orange') ?>
        </section>

        <div class="admin-overview-grid">
            <section class="admin-panel revenue-panel"><div class="admin-panel-heading"><div><h2>Revenus et commandes</h2><p>7 derniers jours</p></div></div><div class="revenue-chart">
                <?php foreach ($revenueSeries as $point): ?><div class="revenue-day"><div class="bar-values"><meter min="0" max="<?= $maxRevenue ?>" value="<?= (int) $point['revenue'] ?>"></meter></div><strong><?= admin_escape($point['label']) ?></strong><small><?= commerce_money((int) $point['revenue']) ?><br><?= (int) $point['orders'] ?> cmd</small></div><?php endforeach; ?>
            </div></section>
            <section class="admin-panel alerts-panel"><div class="admin-panel-heading"><div><h2>Alertes et actions</h2><p>Points a surveiller</p></div></div><ul><?php foreach ($alerts as $alert): ?><li class="<?= admin_escape($alert['level']) ?>"><span><?= admin_escape($alert['label']) ?></span><strong><?= (int) $alert['value'] ?></strong></li><?php endforeach; ?></ul></section>
        </div>

        <section class="admin-panel" id="orders"><div class="admin-panel-heading"><div><h2>Commandes recentes</h2><p><?= count($orders) ?> resultat(s)</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Reference</th><th>Client</th><th>Plan</th><th>Montant</th><th>Paiement</th><th>Licence</th><th>Date</th><th>Actions</th></tr></thead><tbody>
        <?php if ($orders === []): ?><tr><td colspan="8" class="admin-empty">Aucune commande.</td></tr><?php endif; ?>
        <?php foreach ($orders as $order): ?><tr><td><strong><?= admin_escape($order['order_reference'] ?: 'SV-' . $order['id']) ?></strong><small><?= admin_escape($order['payment_reference'] ?: '-') ?></small></td><td><?= admin_escape($order['display_name'] ?: $order['email']) ?><small><?= admin_escape($order['email']) ?></small></td><td><?= admin_escape($order['plan_label']) ?></td><td><?= commerce_money((int) $order['amount_cents'], (string) $order['currency']) ?></td><td><span class="admin-state <?= admin_escape($order['status']) ?>"><?= admin_escape($order['status']) ?></span></td><td><?= admin_escape($order['code_hint'] ?: '-') ?><small><?= admin_escape($order['code_status'] ?: '-') ?></small></td><td><?= admin_escape($order['paid_at'] ?: $order['created_at']) ?></td><td><?php if ($order['status'] === 'paid' && (int) $order['used_devices'] === 0): ?><form method="post" data-confirm="Annuler cette commande et desactiver sa licence ?"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="cancel_order"><input type="hidden" name="order_id" value="<?= (int) $order['id'] ?>"><button class="admin-button compact danger" type="submit">Annuler</button></form><?php else: ?><span class="muted">-</span><?php endif; ?></td></tr><?php endforeach; ?>
        </tbody></table></div></section>

        <div class="admin-two-columns">
            <section class="admin-panel" id="customers"><div class="admin-panel-heading"><div><h2>Clients</h2><p><?= count($users) ?> resultat(s)</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Client</th><th>Commandes</th><th>Depense</th><th>Statut</th><th>Actions</th></tr></thead><tbody><?php foreach ($users as $user): ?><tr><td><strong><?= admin_escape($user['display_name'] ?: $user['email']) ?></strong><small><?= admin_escape($user['email']) ?></small></td><td><?= (int) $user['orders_count'] ?></td><td><?= commerce_money((int) $user['total_spent']) ?></td><td><span class="admin-state <?= admin_escape($user['status']) ?>"><?= admin_escape($user['status']) ?></span></td><td><form method="post"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_user_status"><input type="hidden" name="user_id" value="<?= (int) $user['id'] ?>"><input type="hidden" name="status" value="<?= $user['status'] === 'active' ? 'blocked' : 'active' ?>"><button class="admin-button compact secondary" type="submit"><?= $user['status'] === 'active' ? 'Bloquer' : 'Reactiver' ?></button></form></td></tr><?php endforeach; ?></tbody></table></div></section>
            <section class="admin-panel" id="audit"><div class="admin-panel-heading"><div><h2>Journal admin</h2><p>40 dernieres actions</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Action</th><th>Cible</th></tr></thead><tbody><?php foreach ($auditLogs as $log): ?><tr><td><?= admin_escape($log['created_at']) ?></td><td><?= admin_escape($log['action']) ?><small><?= admin_escape($log['admin_username']) ?></small></td><td><?= admin_escape(trim((string) $log['target_type'] . ' ' . (string) $log['target_id'])) ?></td></tr><?php endforeach; ?></tbody></table></div></section>
        </div>

        <section class="admin-panel generate-panel" id="licenses"><div class="admin-panel-heading"><div><h2>Licences et codes</h2><p>Generation manuelle et gestion des codes</p></div></div><form method="post" class="generate-code-form"><input type="hidden" name="action" value="generate_code"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label>Libelle<input name="label" maxlength="100" placeholder="Client ou commande"></label><label>Duree en jours<input name="duration_days" type="number" min="1" max="36500" value="365" required></label><label>Appareils<input name="max_devices" type="number" min="1" max="1000" value="1" required></label><label>Valide jusqu'au<input name="valid_until" type="date"></label><button class="admin-button primary" type="submit">Generer un code</button></form><div class="admin-table-wrap"><table><thead><tr><th>Code</th><th>Libelle</th><th>Usage</th><th>Duree</th><th>Statut</th><th>Origine</th><th>Actions</th></tr></thead><tbody>
        <?php foreach ($codes as $code): ?><tr data-code-id="<?= (int) $code['id'] ?>" data-label="<?= admin_escape($code['label'] ?: '') ?>"><td><strong><?= admin_escape($code['code_hint'] ?: '#' . $code['id']) ?></strong><small><?= admin_escape($code['created_at']) ?></small></td><td><?= admin_escape($code['label'] ?: '-') ?></td><td><?= (int) $code['used_devices'] ?> / <?= (int) $code['max_devices'] ?></td><td><?= (int) $code['duration_days'] ?> jours</td><td><span class="admin-state <?= admin_escape($code['status']) ?>"><?= admin_escape($code['status']) ?></span></td><td><?= admin_escape($code['created_by'] ?: '-') ?></td><td class="admin-actions"><form method="post"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_code_status"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><input type="hidden" name="status" value="<?= $code['status'] === 'active' ? 'disabled' : 'active' ?>"><button class="admin-button compact secondary" type="submit"><?= $code['status'] === 'active' ? 'Desactiver' : 'Reactiver' ?></button></form><?php if ((int) $code['used_devices'] > 0): ?><form method="post" data-confirm="Revoquer ce code et expirer ses appareils ?"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="revoke_code"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><button class="admin-button compact danger" type="submit">Revoquer</button></form><?php elseif (!str_starts_with((string) $code['created_by'], 'customer:')): ?><form method="post" data-confirm="Supprimer ce code inutilise ?"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="delete_code"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><button class="admin-button compact danger" type="submit">Supprimer</button></form><?php endif; ?></td></tr><?php endforeach; ?>
        </tbody></table></div></section>

        <section class="admin-panel" id="devices"><div class="admin-panel-heading"><div><h2>Appareils</h2><p><?= count($devices) ?> resultat(s)</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Appareil</th><th>Version</th><th>Activation</th><th>Playlist</th><th>Expiration</th><th>Derniere activite</th><th>Actions</th></tr></thead><tbody>
        <?php foreach ($devices as $device): ?><tr><td><strong><?= admin_escape($device['device_name'] ?: 'Android TV') ?></strong><small><?= admin_escape($device['device_id']) ?></small></td><td><?= admin_escape($device['app_version'] ?: '-') ?></td><td><span class="admin-state <?= admin_escape($device['status']) ?>"><?= admin_escape($device['status']) ?></span><small><?= admin_escape($device['activation_type'] ?: '-') ?></small></td><td><?= (int) $device['playlist_configured'] === 1 ? 'Configuree' : 'Absente' ?></td><td><?= admin_escape($device['expires_at'] ?: '-') ?></td><td><?= admin_escape($device['last_seen_at'] ?: '-') ?></td><td class="admin-actions"><form method="post"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_device_status"><input type="hidden" name="device_id" value="<?= admin_escape($device['device_id']) ?>"><input type="hidden" name="status" value="<?= $device['status'] === 'blocked' ? 'active' : 'blocked' ?>"><button class="admin-button compact secondary" type="submit"><?= $device['status'] === 'blocked' ? 'Debloquer' : 'Bloquer' ?></button></form><form method="post" class="extend-form"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="extend_activation"><input type="hidden" name="device_id" value="<?= admin_escape($device['device_id']) ?>"><input name="days" type="number" min="1" max="3650" value="30" aria-label="Jours a ajouter"><button class="admin-button compact primary" type="submit">Prolonger</button></form></td></tr><?php endforeach; ?>
        </tbody></table></div></section>
    </main>
</div>
<script src="/assets/admin.js?v=2" defer></script>
</body></html><?php
}

function admin_kpi(string $label, mixed $value, string $tone): string
{
    return '<div class="admin-kpi ' . admin_escape($tone) . '"><span>' . admin_escape($label) . '</span><strong>' . admin_escape((string) $value) . '</strong></div>';
}
