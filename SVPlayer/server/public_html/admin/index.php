<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$loginError = null;

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && ($_POST['action'] ?? '') === 'login') {
    if (!verify_csrf($_POST['csrf_token'] ?? null)) {
        http_response_code(403);
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
    render_login_page($loginError);
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
        set_admin_flash('error', 'Action impossible. Verifiez les valeurs et reessayez.');
    }
    admin_redirect();
}

$flash = consume_admin_flash();
$generatedCode = is_string($_SESSION['generated_activation_code'] ?? null)
    ? $_SESSION['generated_activation_code']
    : null;
unset($_SESSION['generated_activation_code']);

$stats = load_admin_stats($pdo);
$codes = load_activation_codes($pdo);
$activations = load_recent_activations($pdo);
$auditLogs = load_recent_audit_logs($pdo);

render_dashboard($stats, $codes, $activations, $auditLogs, $flash, $generatedCode);

function handle_admin_action(PDO $pdo, string $action): void
{
    switch ($action) {
        case 'generate_code':
            generate_activation_code($pdo);
            return;
        case 'set_code_status':
            set_activation_code_status($pdo);
            return;
        case 'revoke_code':
            revoke_activation_code($pdo);
            return;
        case 'delete_code':
            delete_unused_activation_code($pdo);
            return;
        default:
            throw new InvalidArgumentException('Unknown admin action.');
    }
}

function generate_activation_code(PDO $pdo): void
{
    $label = mb_substr(trim((string) ($_POST['label'] ?? '')), 0, 100, 'UTF-8');
    $durationDays = filter_var($_POST['duration_days'] ?? null, FILTER_VALIDATE_INT, [
        'options' => ['min_range' => 1, 'max_range' => 3650],
    ]);
    $maxDevices = filter_var($_POST['max_devices'] ?? null, FILTER_VALIDATE_INT, [
        'options' => ['min_range' => 1, 'max_range' => 1000],
    ]);

    if ($durationDays === false || $maxDevices === false) {
        throw new InvalidArgumentException('Invalid code settings.');
    }

    $validUntil = null;
    $validUntilInput = trim((string) ($_POST['valid_until'] ?? ''));
    if ($validUntilInput !== '') {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $validUntilInput, new DateTimeZone('UTC'));
        if (!$date) {
            throw new InvalidArgumentException('Invalid date.');
        }
        $validUntil = $date->setTime(23, 59, 59)->format('Y-m-d H:i:s');
    }

    $pdo->beginTransaction();
    $plainCode = null;
    $codeId = null;

    for ($attempt = 0; $attempt < 10; $attempt++) {
        $candidate = generate_smartvision_code();
        $normalized = normalize_activation_code($candidate);

        try {
            $insertCode = $pdo->prepare(
                "INSERT INTO activation_codes
                    (code_hash, label, duration_days, max_devices, used_devices, status, valid_until, created_at, updated_at)
                 VALUES
                    (:code_hash, :label, :duration_days, :max_devices, 0, 'active', :valid_until, NOW(), NOW())"
            );
            $insertCode->execute([
                'code_hash' => activation_code_hash($normalized),
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
        throw new RuntimeException('Unable to generate code.');
    }

    $hint = 'SV-****-****-' . substr(normalize_activation_code($plainCode), -4);
    $insertMetadata = $pdo->prepare(
        "INSERT INTO activation_code_metadata (code_id, code_hint, created_by, last_used_at)
         VALUES (:code_id, :code_hint, :created_by, NULL)"
    );
    $insertMetadata->execute([
        'code_id' => $codeId,
        'code_hint' => $hint,
        'created_by' => current_admin_username(),
    ]);

    audit_admin_action($pdo, 'activation_code_created', 'activation_code', (string) $codeId, [
        'label' => $label,
        'duration_days' => $durationDays,
        'max_devices' => $maxDevices,
        'valid_until' => $validUntil,
    ]);
    $pdo->commit();

    $_SESSION['generated_activation_code'] = $plainCode;
    set_admin_flash('success', 'Code SmartVision genere. Copiez-le maintenant : il ne sera plus affiche.');
}

function set_activation_code_status(PDO $pdo): void
{
    $codeId = filter_var($_POST['code_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    $status = (string) ($_POST['status'] ?? '');
    if ($codeId === false || !in_array($status, ['active', 'disabled'], true)) {
        throw new InvalidArgumentException('Invalid status update.');
    }

    $statement = $pdo->prepare(
        "UPDATE activation_codes SET status = :status, updated_at = NOW() WHERE id = :code_id"
    );
    $statement->execute(['status' => $status, 'code_id' => $codeId]);
    if ($statement->rowCount() < 1) {
        throw new RuntimeException('Code not found.');
    }

    audit_admin_action($pdo, 'activation_code_status_changed', 'activation_code', (string) $codeId, [
        'status' => $status,
    ]);
    set_admin_flash('success', $status === 'active' ? 'Code reactive.' : 'Code desactive.');
}

function revoke_activation_code(PDO $pdo): void
{
    $codeId = filter_var($_POST['code_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($codeId === false) {
        throw new InvalidArgumentException('Invalid code id.');
    }

    $pdo->beginTransaction();

    $disableCode = $pdo->prepare(
        "UPDATE activation_codes SET status = 'disabled', updated_at = NOW() WHERE id = :code_id"
    );
    $disableCode->execute(['code_id' => $codeId]);

    $expireDevices = $pdo->prepare(
        "UPDATE devices
         SET status = 'expired', expires_at = NOW(), updated_at = NOW()
         WHERE device_id IN (
             SELECT device_id FROM device_activations
             WHERE activation_code_id = :code_id AND status = 'active'
         )"
    );
    $expireDevices->execute(['code_id' => $codeId]);

    $expireActivations = $pdo->prepare(
        "UPDATE device_activations
         SET status = 'expired', expires_at = NOW()
         WHERE activation_code_id = :code_id AND status = 'active'"
    );
    $expireActivations->execute(['code_id' => $codeId]);

    audit_admin_action($pdo, 'activation_code_revoked', 'activation_code', (string) $codeId, [
        'expired_activations' => $expireActivations->rowCount(),
    ]);
    $pdo->commit();
    set_admin_flash('success', 'Code revoque et activations associees expirees.');
}

function delete_unused_activation_code(PDO $pdo): void
{
    $codeId = filter_var($_POST['code_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($codeId === false) {
        throw new InvalidArgumentException('Invalid code id.');
    }

    $statement = $pdo->prepare(
        "DELETE FROM activation_codes
         WHERE id = :code_id
           AND used_devices = 0
           AND NOT EXISTS (
               SELECT 1 FROM device_activations WHERE activation_code_id = :activation_id
           )"
    );
    $statement->execute([
        'code_id' => $codeId,
        'activation_id' => $codeId,
    ]);
    if ($statement->rowCount() < 1) {
        throw new RuntimeException('Used code cannot be deleted.');
    }

    audit_admin_action($pdo, 'activation_code_deleted', 'activation_code', (string) $codeId);
    set_admin_flash('success', 'Code inutilise supprime.');
}

function load_admin_stats(PDO $pdo): array
{
    $codeStats = $pdo->query(
        "SELECT
            COUNT(*) AS total_codes,
            COALESCE(SUM(status = 'active'), 0) AS active_codes,
            COALESCE(SUM(GREATEST(max_devices - used_devices, 0)), 0) AS available_slots
         FROM activation_codes"
    )->fetch();
    $deviceStats = $pdo->query(
        "SELECT
            COALESCE(SUM(status = 'active'), 0) AS active_devices,
            COALESCE(SUM(status = 'pending'), 0) AS pending_devices
         FROM devices"
    )->fetch();
    $pendingSessions = (int) $pdo->query(
        "SELECT COUNT(*) FROM activation_sessions WHERE status = 'pending' AND expires_at > NOW()"
    )->fetchColumn();

    return array_merge($codeStats ?: [], $deviceStats ?: [], [
        'pending_sessions' => $pendingSessions,
    ]);
}

function load_activation_codes(PDO $pdo): array
{
    return $pdo->query(
        "SELECT
            c.id, c.label, c.duration_days, c.max_devices, c.used_devices,
            c.status, c.valid_until, c.created_at,
            m.code_hint, m.created_by, m.last_used_at,
            (SELECT COUNT(*) FROM device_activations da
             WHERE da.activation_code_id = c.id AND da.status = 'active' AND da.expires_at > NOW()) AS active_devices
         FROM activation_codes c
         LEFT JOIN activation_code_metadata m ON m.code_id = c.id
         ORDER BY c.created_at DESC
         LIMIT 100"
    )->fetchAll();
}

function load_recent_activations(PDO $pdo): array
{
    return $pdo->query(
        "SELECT da.id, da.device_id, da.status, da.starts_at, da.expires_at,
                COALESCE(m.code_hint, CONCAT('#', da.activation_code_id)) AS code_hint
         FROM device_activations da
         LEFT JOIN activation_code_metadata m ON m.code_id = da.activation_code_id
         ORDER BY da.created_at DESC
         LIMIT 20"
    )->fetchAll();
}

function load_recent_audit_logs(PDO $pdo): array
{
    return $pdo->query(
        "SELECT admin_username, action, target_type, target_id, created_at
         FROM admin_audit_logs
         ORDER BY id DESC
         LIMIT 20"
    )->fetchAll();
}

function render_login_page(?string $error): void
{
    $credentials = admin_credentials();
    $configured = $credentials['username'] !== '' && $credentials['password_hash'] !== '';
    ?>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Administration SmartVision</title>
    <?= admin_styles() ?>
</head>
<body class="login-body">
<main class="login-panel">
    <div class="brand"><span class="brand-mark">&#9654;</span><span>Smart<strong>Vision</strong></span></div>
    <h1>Administration</h1>
    <p class="muted">Gestion des codes et activations SmartVision.</p>
    <?php if (!$configured): ?>
        <div class="notice error">Administration non configuree sur le serveur.</div>
    <?php else: ?>
        <?php if ($error): ?><div class="notice error"><?= admin_escape($error) ?></div><?php endif; ?>
        <form method="post" autocomplete="on">
            <input type="hidden" name="action" value="login">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
            <label for="username">Identifiant</label>
            <input id="username" name="username" type="text" autocomplete="username" required autofocus>
            <label for="password">Mot de passe</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>
            <button class="primary wide" type="submit">Se connecter</button>
        </form>
    <?php endif; ?>
</main>
</body>
</html>
<?php
}

function render_dashboard(
    array $stats,
    array $codes,
    array $activations,
    array $auditLogs,
    ?array $flash,
    ?string $generatedCode,
): void {
    ?>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Administration SmartVision</title>
    <?= admin_styles() ?>
</head>
<body>
<header class="topbar">
    <div class="brand"><span class="brand-mark">&#9654;</span><span>Smart<strong>Vision</strong></span></div>
    <form method="post" action="/admin/logout.php">
        <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
        <button class="secondary" type="submit">Deconnexion</button>
    </form>
</header>

<main class="dashboard">
    <div class="page-title">
        <div><h1>Activations</h1><p class="muted">Codes SmartVision, appareils et historique recent.</p></div>
        <span class="admin-user"><?= admin_escape(current_admin_username()) ?></span>
    </div>

    <?php if ($flash): ?>
        <div class="notice <?= admin_escape((string) ($flash['type'] ?? '')) ?>"><?= admin_escape((string) ($flash['message'] ?? '')) ?></div>
    <?php endif; ?>

    <?php if ($generatedCode): ?>
        <section class="generated-code">
            <div><span>Nouveau code - affiche une seule fois</span><strong id="generated-code"><?= admin_escape($generatedCode) ?></strong></div>
            <button class="primary" type="button" id="copy-code">Copier</button>
        </section>
    <?php endif; ?>

    <section class="stats" aria-label="Statistiques">
        <?= stat_item('Codes actifs', $stats['active_codes'] ?? 0) ?>
        <?= stat_item('Places disponibles', $stats['available_slots'] ?? 0) ?>
        <?= stat_item('Appareils actifs', $stats['active_devices'] ?? 0) ?>
        <?= stat_item('Sessions en attente', $stats['pending_sessions'] ?? 0) ?>
    </section>

    <section class="section generate-section">
        <div class="section-heading"><h2>Generer un code</h2><p>Le code clair ne sera affiche qu une seule fois.</p></div>
        <form method="post" class="generate-form">
            <input type="hidden" name="action" value="generate_code">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
            <div><label for="label">Libelle</label><input id="label" name="label" maxlength="100" placeholder="Client ou commande"></div>
            <div><label for="duration_days">Duree (jours)</label><input id="duration_days" name="duration_days" type="number" min="1" max="3650" value="365" required></div>
            <div><label for="max_devices">Appareils</label><input id="max_devices" name="max_devices" type="number" min="1" max="1000" value="1" required></div>
            <div><label for="valid_until">Valide jusqu au</label><input id="valid_until" name="valid_until" type="date"></div>
            <button class="primary" type="submit">Generer</button>
        </form>
    </section>

    <section class="section">
        <div class="section-heading"><h2>Codes</h2><p><?= count($codes) ?> codes recents</p></div>
        <div class="table-scroll">
            <table>
                <thead><tr><th>Code</th><th>Libelle</th><th>Usage</th><th>Duree</th><th>Statut</th><th>Derniere utilisation</th><th>Actions</th></tr></thead>
                <tbody>
                <?php if ($codes === []): ?>
                    <tr><td colspan="7" class="empty">Aucun code genere.</td></tr>
                <?php endif; ?>
                <?php foreach ($codes as $code): ?>
                    <tr data-code-id="<?= (int) $code['id'] ?>" data-label="<?= admin_escape($code['label'] ?: '') ?>">
                        <td><strong><?= admin_escape($code['code_hint'] ?: '#' . $code['id']) ?></strong><small><?= admin_escape((string) $code['created_at']) ?></small></td>
                        <td><?= admin_escape($code['label'] ?: '-') ?></td>
                        <td><?= (int) $code['used_devices'] ?> / <?= (int) $code['max_devices'] ?><small><?= (int) $code['active_devices'] ?> actif(s)</small></td>
                        <td><?= (int) $code['duration_days'] ?> jours<small><?= admin_escape($code['valid_until'] ?: 'Sans limite de redemption') ?></small></td>
                        <td><span class="status <?= admin_escape((string) $code['status']) ?>"><?= admin_escape((string) $code['status']) ?></span></td>
                        <td><?= admin_escape($code['last_used_at'] ?: 'Jamais') ?></td>
                        <td class="actions">
                            <form method="post">
                                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                                <input type="hidden" name="action" value="set_code_status">
                                <input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>">
                                <input type="hidden" name="status" value="<?= $code['status'] === 'active' ? 'disabled' : 'active' ?>">
                                <button class="secondary compact" type="submit"><?= $code['status'] === 'active' ? 'Desactiver' : 'Reactiver' ?></button>
                            </form>
                            <?php if ((int) $code['used_devices'] > 0): ?>
                                <form method="post" onsubmit="return confirm('Revoquer ce code et expirer ses appareils ?');">
                                    <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                                    <input type="hidden" name="action" value="revoke_code">
                                    <input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>">
                                    <button class="danger compact" type="submit">Revoquer</button>
                                </form>
                            <?php else: ?>
                                <form method="post" onsubmit="return confirm('Supprimer definitivement ce code inutilise ?');">
                                    <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                                    <input type="hidden" name="action" value="delete_code">
                                    <input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>">
                                    <button class="danger compact" type="submit">Supprimer</button>
                                </form>
                            <?php endif; ?>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </section>

    <div class="split-sections">
        <section class="section">
            <div class="section-heading"><h2>Activations recentes</h2></div>
            <div class="table-scroll"><table><thead><tr><th>Appareil</th><th>Code</th><th>Statut</th><th>Expiration</th></tr></thead><tbody>
            <?php if ($activations === []): ?><tr><td colspan="4" class="empty">Aucune activation.</td></tr><?php endif; ?>
            <?php foreach ($activations as $activation): ?><tr><td><?= admin_escape((string) $activation['device_id']) ?></td><td><?= admin_escape((string) $activation['code_hint']) ?></td><td><?= admin_escape((string) $activation['status']) ?></td><td><?= admin_escape((string) $activation['expires_at']) ?></td></tr><?php endforeach; ?>
            </tbody></table></div>
        </section>

        <section class="section">
            <div class="section-heading"><h2>Journal admin</h2></div>
            <div class="table-scroll"><table><thead><tr><th>Date</th><th>Action</th><th>Cible</th></tr></thead><tbody>
            <?php if ($auditLogs === []): ?><tr><td colspan="3" class="empty">Aucune action.</td></tr><?php endif; ?>
            <?php foreach ($auditLogs as $log): ?><tr><td><?= admin_escape((string) $log['created_at']) ?></td><td><?= admin_escape((string) $log['action']) ?><small><?= admin_escape((string) $log['admin_username']) ?></small></td><td><?= admin_escape(trim((string) $log['target_type'] . ' ' . (string) $log['target_id'])) ?></td></tr><?php endforeach; ?>
            </tbody></table></div>
        </section>
    </div>
</main>

<?php if ($generatedCode): ?>
<script>
document.getElementById('copy-code').addEventListener('click', async () => {
    await navigator.clipboard.writeText(document.getElementById('generated-code').textContent);
    document.getElementById('copy-code').textContent = 'Copie';
});
</script>
<?php endif; ?>
</body>
</html>
<?php
}

function stat_item(string $label, mixed $value): string
{
    return '<div class="stat"><span>' . admin_escape($label) . '</span><strong>' . (int) $value . '</strong></div>';
}

function admin_styles(): string
{
    return <<<'HTML'
<style>
:root{color-scheme:dark;font-family:Inter,Arial,sans-serif;background:#050a13;color:#fff}*{box-sizing:border-box}body{margin:0;min-height:100vh;background:#050a13;color:#fff}.topbar{height:68px;padding:0 34px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #223047;background:#08111f}.brand{display:flex;align-items:center;gap:10px;font-size:20px;font-weight:750}.brand strong{color:#2389ff}.brand-mark{width:32px;height:32px;display:grid;place-items:center;border-radius:8px;background:linear-gradient(145deg,#16d9ff,#176dff 55%,#0c2f8f)}button,input{font:inherit}.dashboard{max-width:1500px;margin:0 auto;padding:30px 34px 60px}.page-title{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px}.page-title h1,.login-panel h1{margin:0;font-size:34px}.muted,.section-heading p{color:#9fadc2}.admin-user{padding:8px 12px;border:1px solid #2d3d56;border-radius:7px;color:#b8c5d7}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px}.stat{padding:18px;border:1px solid #24334b;border-radius:8px;background:#0b1423}.stat span{display:block;color:#9fadc2;font-size:14px}.stat strong{display:block;margin-top:7px;font-size:28px}.section{margin-top:16px;border-top:1px solid #24334b;background:#09121f}.section-heading{display:flex;align-items:center;justify-content:space-between;padding:18px 20px 12px}.section-heading h2{margin:0;font-size:20px}.section-heading p{margin:0}.generate-section{border:1px solid #24334b;border-radius:8px}.generate-form{display:grid;grid-template-columns:2fr 1fr 1fr 1fr auto;gap:12px;align-items:end;padding:0 20px 20px}label{display:block;margin-bottom:7px;color:#c9d5e5;font-size:13px;font-weight:650}input{width:100%;height:44px;padding:0 12px;border:1px solid #30415d;border-radius:7px;outline:none;background:#050d18;color:#fff}input:focus{border-color:#2c86ff;box-shadow:0 0 0 2px rgba(44,134,255,.18)}button{height:42px;padding:0 16px;border:1px solid transparent;border-radius:7px;color:#fff;cursor:pointer;font-weight:700}.primary{background:#1976ff;border-color:#388dff}.secondary{background:#111d2f;border-color:#33445f;color:#c6d2e3}.danger{background:#32141a;border-color:#74313d;color:#ff9ca9}.compact{height:32px;padding:0 10px;font-size:12px}.wide{width:100%;margin-top:18px}.notice{margin-bottom:16px;padding:13px 15px;border:1px solid #33445f;border-radius:7px;background:#0e1929}.notice.success{border-color:#227e58;color:#7bf0b4}.notice.error{border-color:#7d3540;color:#ff9da9}.generated-code{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;padding:18px 20px;border:1px solid #2676d4;border-radius:8px;background:#071b37}.generated-code span{display:block;color:#9fb7d6;font-size:13px}.generated-code strong{display:block;margin-top:6px;font-size:27px;letter-spacing:2px}.table-scroll{overflow:auto}table{width:100%;border-collapse:collapse}th,td{padding:12px 14px;border-top:1px solid #1d2a3f;text-align:left;vertical-align:middle;font-size:13px}th{color:#8fa0b8;font-size:11px;text-transform:uppercase}td small{display:block;margin-top:4px;color:#7f8da3}.status{display:inline-block;padding:5px 8px;border-radius:5px;font-size:11px;font-weight:750;text-transform:uppercase}.status.active{background:#103d2c;color:#73e7ad}.status.disabled,.status.expired{background:#3a1b22;color:#ff9baa}.actions{display:flex;gap:7px}.actions form{margin:0}.empty{padding:30px;text-align:center;color:#7f8da3}.split-sections{display:grid;grid-template-columns:1fr 1fr;gap:16px}.login-body{display:grid;place-items:center;padding:24px;background:radial-gradient(circle at 50% 20%,rgba(25,118,255,.23),transparent 40%),#030812}.login-panel{width:min(100%,430px);padding:30px;border:1px solid #2a3952;border-radius:12px;background:#0a1423}.login-panel .brand{margin-bottom:26px}.login-panel .muted{margin:8px 0 24px}.login-panel label{margin-top:14px}@media(max-width:1050px){.stats{grid-template-columns:repeat(2,1fr)}.generate-form{grid-template-columns:1fr 1fr}.split-sections{grid-template-columns:1fr}}@media(max-width:650px){.topbar,.dashboard{padding-left:16px;padding-right:16px}.stats,.generate-form{grid-template-columns:1fr}.page-title{align-items:flex-start;gap:12px}.actions{flex-direction:column}.generated-code{align-items:flex-start;gap:14px;flex-direction:column}}
</style>
HTML;
}
