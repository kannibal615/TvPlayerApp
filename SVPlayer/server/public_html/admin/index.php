<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';
require_once dirname(__DIR__) . '/api/commerce.php';
require_once dirname(__DIR__) . '/api/mail_service.php';

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
sv_mail_ensure_schema($pdo);
commerce_ensure_payment_schema($pdo);
$page = admin_current_page($_POST['redirect_page'] ?? $_GET['page'] ?? 'overview');
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
    header('Location: /admin/?page=' . rawurlencode($page));
    exit;
}

$query = smartvision_text_substr(trim((string) ($_GET['q'] ?? '')), 0, 80);
$stats = admin_load_stats($pdo);
$orders = admin_load_orders($pdo, $query);
$users = admin_load_users($pdo, $query);
$devicePage = admin_page_number($_GET['devices_page'] ?? 1);
$licensePage = admin_page_number($_GET['licenses_page'] ?? 1);
$devicesResult = admin_load_devices($pdo, $query, $devicePage);
$codesResult = admin_load_codes($pdo, $query, $licensePage);
$devices = $devicesResult['rows'];
$codes = $codesResult['rows'];
$auditLogs = admin_load_audit($pdo);
$slides = admin_load_slides($pdo);
$notifications = admin_load_notifications($pdo);
$messages = admin_load_contact_messages($pdo, $query);
$revenueSeries = admin_revenue_series($pdo);
$alerts = admin_build_alerts($stats);
$serverStats = admin_load_server_stats($pdo, $stats);
$paymentPacks = admin_load_payment_packs($pdo);
$gammalPayments = commerce_load_gammal_payments($pdo);
$emailAdmin = admin_load_email_admin($pdo);
$flash = consume_admin_flash();
$generatedCode = $_SESSION['generated_activation_code'] ?? null;
if (is_string($generatedCode)) {
    $generatedCode = [$generatedCode];
}
if (!is_array($generatedCode)) {
    $generatedCode = null;
}
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
    $devicesResult['pagination'],
    $codesResult['pagination'],
    $slides,
    $notifications,
    $messages,
    $serverStats,
    $paymentPacks,
    $gammalPayments,
    $emailAdmin,
    $flash,
    $generatedCode,
    $query,
    $page,
);

function admin_current_page(mixed $page): string
{
    $page = is_string($page) ? $page : 'overview';
    return in_array($page, ['overview', 'orders', 'customers', 'licenses', 'payments', 'emails', 'devices', 'notifications', 'messages', 'slides', 'server', 'audit'], true)
        ? $page
        : 'overview';
}

function admin_page_number(mixed $page): int
{
    $value = filter_var($page, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 9999]]);

    return $value === false ? 1 : (int) $value;
}

function handle_admin_action(PDO $pdo, string $action): void
{
    switch ($action) {
        case 'generate_code': admin_generate_code($pdo); break;
        case 'set_code_status': admin_set_code_status($pdo); break;
        case 'revoke_code': admin_revoke_code($pdo); break;
        case 'delete_code': admin_delete_code($pdo); break;
        case 'purge_devices': admin_purge_devices($pdo); break;
        case 'save_slide': admin_save_slide($pdo); break;
        case 'save_payment_packs': admin_save_payment_packs($pdo); break;
        case 'approve_gammal_payment': admin_approve_gammal_payment($pdo); break;
        case 'reject_gammal_payment': admin_reject_gammal_payment($pdo); break;
        case 'save_email_settings': admin_save_email_settings($pdo); break;
        case 'save_email_template': admin_save_email_template($pdo); break;
        case 'send_test_email': admin_send_test_email($pdo); break;
        case 'send_notification': admin_send_notification($pdo); break;
        case 'set_notification_status': admin_set_notification_status($pdo); break;
        case 'delete_notification': admin_delete_notification($pdo); break;
        case 'set_contact_message_status': admin_set_contact_message_status($pdo); break;
        case 'set_user_status': admin_set_user_status($pdo); break;
        case 'set_device_status': admin_set_device_status($pdo); break;
        case 'expire_device': admin_expire_device($pdo); break;
        case 'clear_device_playlist': admin_clear_device_playlist($pdo); break;
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
    $label = smartvision_text_substr(trim((string) ($_POST['label'] ?? '')), 0, 100);
    $durationDays = admin_positive_int($_POST['duration_days'] ?? null, 36500);
    $maxDevices = admin_positive_int($_POST['max_devices'] ?? null, 1000);
    $quantity = admin_positive_int($_POST['quantity'] ?? 1, 200);
    $licenseType = (string) ($_POST['license_type'] ?? 'paid');
    if (!in_array($licenseType, ['paid', 'trial', 'free', 'manual', 'promo'], true)) {
        $licenseType = 'manual';
    }
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
    $generated = [];
    $createdIds = [];
    for ($index = 0; $index < $quantity; $index++) {
        $created = create_activation_code_record(
            $pdo,
            $quantity === 1 ? $label : trim($label . ' #' . ($index + 1)),
            $durationDays,
            $maxDevices,
            $licenseType,
            $validUntil,
            current_admin_username(),
        );
        $generated[] = (string) $created['code'];
        $createdIds[] = (int) $created['id'];
    }
    audit_admin_action($pdo, 'activation_code_created', 'activation_code', implode(',', $createdIds), [
        'duration_days' => $durationDays,
        'max_devices' => $maxDevices,
        'quantity' => $quantity,
        'license_type' => $licenseType,
        'label' => $label,
    ]);
    $pdo->commit();
    $_SESSION['generated_activation_code'] = $generated;
    set_admin_flash('success', $quantity === 1 ? 'Code genere.' : $quantity . ' codes generes.');
}

function admin_set_code_status(PDO $pdo): void
{
    $codeId = admin_positive_int($_POST['code_id'] ?? null);
    $status = (string) ($_POST['status'] ?? '');
    if (!in_array($status, ['active', 'disabled', 'expired'], true)) {
        throw new InvalidArgumentException('Statut de code invalide.');
    }
    $statement = $pdo->prepare('UPDATE activation_codes SET status = :status, updated_at = NOW() WHERE id = :id');
    $statement->execute(['status' => $status, 'id' => $codeId]);
    audit_admin_action($pdo, 'activation_code_status_changed', 'activation_code', (string) $codeId, ['status' => $status]);
    set_admin_flash('success', $status === 'active' ? 'Code reactive.' : ($status === 'expired' ? 'Code expire.' : 'Code desactive.'));
}

function admin_save_payment_packs(PDO $pdo): void
{
    $plans = commerce_payment_plans();
    $statement = $pdo->prepare(
        "INSERT INTO app_settings (setting_key, setting_value)
         VALUES (:setting_key, :setting_value)
         ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
    );

    $pdo->beginTransaction();
    foreach ($plans as $planKey => $plan) {
        $url = trim((string) ($_POST['payment_url'][$planKey] ?? ''));
        $enabled = isset($_POST['payment_enabled'][$planKey]);
        if ($url !== '' && !commerce_is_valid_gammal_payment_url($url)) {
            throw new InvalidArgumentException(
                'Le lien du pack ' . (string) $plan['label'] . ' doit utiliser HTTPS sur gammal.tech.'
            );
        }
        if ($enabled && $url === '') {
            throw new InvalidArgumentException(
                'Ajoutez un lien Gammal Tech avant d activer le pack ' . (string) $plan['label'] . '.'
            );
        }

        $statement->execute([
            'setting_key' => commerce_payment_setting_key((string) $planKey, 'url'),
            'setting_value' => $url,
        ]);
        $statement->execute([
            'setting_key' => commerce_payment_setting_key((string) $planKey, 'enabled'),
            'setting_value' => $enabled ? '1' : '0',
        ]);
    }
    $pdo->commit();

    audit_admin_action($pdo, 'payment_packs_updated', 'settings', 'gammal_tech');
    set_admin_flash('success', 'Configuration des packs Gammal Tech enregistree.');
}

function admin_load_payment_packs(PDO $pdo): array
{
    $packs = [];
    foreach (commerce_payment_plans() as $planKey => $plan) {
        $packs[$planKey] = [
            'label' => (string) $plan['label'],
            'description' => (string) $plan['description'],
            'amount' => commerce_money((int) $plan['amount_cents']),
            'url' => trim((string) get_setting(
                $pdo,
                commerce_payment_setting_key((string) $planKey, 'url'),
                '',
            )),
            'enabled' => (string) get_setting(
                $pdo,
                commerce_payment_setting_key((string) $planKey, 'enabled'),
                '0',
            ) === '1',
        ];
    }

    return $packs;
}

function admin_approve_gammal_payment(PDO $pdo): void
{
    $paymentId = admin_positive_int($_POST['payment_id'] ?? null);
    $result = commerce_approve_gammal_payment($pdo, $paymentId, current_admin_username());
    sv_send_email($pdo, 'order_confirmed', (string) $result['email'], [
        'customer' => ['name' => (string) ($result['display_name'] ?: $result['email'])],
        'order' => [
            'reference' => (string) $result['order_reference'],
            'plan' => (string) $result['plan_label'],
            'amount' => commerce_money((int) $result['amount_cents'], (string) $result['currency']),
        ],
        'license' => ['code' => (string) ($result['activation_code'] ?? '')],
        'account_url' => smartvision_public_base_url() . '/account/?section=licenses',
    ]);
    sv_send_admin_notification($pdo, 'admin_notification_order_created', [
        'Commande' => (string) $result['order_reference'],
        'Transaction' => (string) $result['txn'],
        'Email client' => (string) $result['email'],
        'Pack' => (string) $result['plan_label'],
        'Validation' => current_admin_username(),
    ], '/admin/?page=orders');
    audit_admin_action($pdo, 'gammal_payment_approved', 'commerce_payment', (string) $paymentId, [
        'txn' => (string) $result['txn'],
    ]);
    set_admin_flash('success', 'Paiement approuve et licence creee.');
}

function admin_reject_gammal_payment(PDO $pdo): void
{
    $paymentId = admin_positive_int($_POST['payment_id'] ?? null);
    commerce_reject_gammal_payment($pdo, $paymentId, current_admin_username());
    audit_admin_action($pdo, 'gammal_payment_rejected', 'commerce_payment', (string) $paymentId);
    set_admin_flash('success', 'Paiement rejete. Aucune licence n a ete creee.');
}

function admin_save_email_settings(PDO $pdo): void
{
    $settings = [
        'external_services_enabled' => (string) ($_POST['external_services_enabled'] ?? '0') === '1' ? '1' : '0',
        'smtp_enabled' => (string) ($_POST['smtp_enabled'] ?? '0') === '1' ? '1' : '0',
        'smtp_host' => smartvision_text_substr(trim((string) ($_POST['smtp_host'] ?? '')), 0, 190),
        'smtp_port' => (string) admin_positive_int($_POST['smtp_port'] ?? 465, 65535),
        'smtp_secure' => in_array((string) ($_POST['smtp_secure'] ?? ''), ['ssl', 'tls'], true)
            ? (string) $_POST['smtp_secure']
            : 'ssl',
        'smtp_user' => smartvision_text_substr(trim((string) ($_POST['smtp_user'] ?? '')), 0, 255),
        'smtp_from_email' => strtolower(trim((string) ($_POST['smtp_from_email'] ?? ''))),
        'smtp_from_name' => smartvision_text_substr(trim((string) ($_POST['smtp_from_name'] ?? 'SmartVision')), 0, 120),
        'smtp_reply_to' => strtolower(trim((string) ($_POST['smtp_reply_to'] ?? ''))),
        'admin_notification_email' => strtolower(trim((string) ($_POST['admin_notification_email'] ?? ''))),
    ];
    foreach (['smtp_from_email', 'smtp_reply_to', 'admin_notification_email'] as $emailKey) {
        if ($settings[$emailKey] !== '' && !filter_var($settings[$emailKey], FILTER_VALIDATE_EMAIL)) {
            throw new InvalidArgumentException('Adresse email invalide dans la configuration SMTP.');
        }
    }
    if ($settings['smtp_host'] === '') {
        throw new InvalidArgumentException('Le serveur SMTP est obligatoire.');
    }

    $statement = $pdo->prepare(
        "INSERT INTO app_settings (setting_key, setting_value)
         VALUES (:setting_key, :setting_value)
         ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
    );
    $pdo->beginTransaction();
    foreach ($settings as $key => $value) {
        $statement->execute(['setting_key' => $key, 'setting_value' => $value]);
    }
    $pdo->commit();
    audit_admin_action($pdo, 'email_settings_updated', 'settings', 'smtp');
    set_admin_flash('success', 'Configuration email enregistree. Le mot de passe reste dans le fichier prive.');
}

function admin_save_email_template(PDO $pdo): void
{
    $templateId = admin_positive_int($_POST['template_id'] ?? null);
    $subject = smartvision_text_substr(trim((string) ($_POST['subject_template'] ?? '')), 0, 255);
    $title = smartvision_text_substr(trim((string) ($_POST['title_template'] ?? '')), 0, 255);
    $intro = smartvision_text_substr(trim((string) ($_POST['intro_html'] ?? '')), 0, 10000);
    $body = smartvision_text_substr(trim((string) ($_POST['body_html'] ?? '')), 0, 30000);
    $footer = smartvision_text_substr(trim((string) ($_POST['footer_html'] ?? '')), 0, 10000);
    if ($subject === '' || $title === '') {
        throw new InvalidArgumentException('Le sujet et le titre du template sont obligatoires.');
    }
    $pdo->prepare(
        "UPDATE email_templates
         SET subject_template = :subject, title_template = :title, intro_html = :intro,
             body_html = :body, footer_html = :footer, is_active = :is_active, updated_at = NOW()
         WHERE id = :id"
    )->execute([
        'subject' => $subject,
        'title' => $title,
        'intro' => $intro,
        'body' => $body,
        'footer' => $footer,
        'is_active' => isset($_POST['is_active']) ? 1 : 0,
        'id' => $templateId,
    ]);
    audit_admin_action($pdo, 'email_template_updated', 'email_template', (string) $templateId);
    set_admin_flash('success', 'Template email enregistre.');
}

function admin_send_test_email(PDO $pdo): void
{
    $recipient = strtolower(trim((string) ($_POST['test_recipient'] ?? '')));
    if (!filter_var($recipient, FILTER_VALIDATE_EMAIL)) {
        throw new InvalidArgumentException('Adresse de test invalide.');
    }
    $status = sv_send_email($pdo, 'admin_notification_generic', $recipient, [
        'event' => [
            'title' => 'Test email SmartVision',
            'message' => 'Ce message valide le rendu du template et la configuration SMTP.',
        ],
        'admin_event' => [
            'Environnement' => (string) (getenv('SMARTVISION_ENV') ?: 'production'),
            'Date' => gmdate('Y-m-d H:i:s') . ' UTC',
        ],
        'admin_url' => smartvision_public_base_url() . '/admin/?page=emails',
    ]);
    set_admin_flash(
        $status === 'sent' ? 'success' : 'error',
        $status === 'sent'
            ? 'Email de test envoye.'
            : 'Email journalise avec le statut ' . $status . '. Verifiez la configuration et les logs.',
    );
}

function admin_load_email_admin(PDO $pdo): array
{
    sv_mail_ensure_schema($pdo);
    $config = sv_mail_config($pdo);
    $templates = $pdo->query(
        "SELECT * FROM email_templates ORDER BY category, sort_order, id"
    )->fetchAll();
    $logs = $pdo->query(
        "SELECT id, email_type, recipient_email, subject, status, error_message, provider, template_key, created_at
         FROM email_logs ORDER BY id DESC LIMIT 80"
    )->fetchAll();
    return [
        'config' => $config,
        'password_configured' => $config['password'] !== '',
        'templates' => $templates,
        'logs' => $logs,
    ];
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

function admin_expire_device(PDO $pdo): void
{
    $deviceId = clean_device_id((string) ($_POST['device_id'] ?? ''));
    if ($deviceId === '') {
        throw new InvalidArgumentException('Appareil invalide.');
    }
    $pdo->beginTransaction();
    $pdo->prepare("UPDATE device_activations SET status = 'expired', expires_at = NOW() WHERE device_id = :device_id AND status IN ('active', 'blocked')")
        ->execute(['device_id' => $deviceId]);
    $pdo->prepare("UPDATE devices SET status = 'expired', license_status = 'expired', expires_at = NOW(), updated_at = NOW() WHERE device_id = :device_id")
        ->execute(['device_id' => $deviceId]);
    audit_admin_action($pdo, 'device_expired', 'device', $deviceId);
    $pdo->commit();
    set_admin_flash('success', 'Appareil expire.');
}

function admin_clear_device_playlist(PDO $pdo): void
{
    $deviceId = clean_device_id((string) ($_POST['device_id'] ?? ''));
    if ($deviceId === '') {
        throw new InvalidArgumentException('Appareil invalide.');
    }
    $pdo->beginTransaction();
    $pdo->prepare('DELETE FROM device_playlist_configs WHERE device_id = :device_id')
        ->execute(['device_id' => $deviceId]);
    $pdo->prepare("UPDATE devices SET xtream_status = 'missing', updated_at = NOW() WHERE device_id = :device_id")
        ->execute(['device_id' => $deviceId]);
    audit_admin_action($pdo, 'device_playlist_cleared', 'device', $deviceId);
    $pdo->commit();
    set_admin_flash('success', 'Configuration Xtream appareil supprimee.');
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

function admin_purge_devices(PDO $pdo): void
{
    $confirmation = strtoupper(trim((string) ($_POST['confirmation'] ?? '')));
    if ($confirmation !== 'PURGER') {
        throw new InvalidArgumentException('Saisissez PURGER pour confirmer la remise a zero des appareils.');
    }

    $pdo->beginTransaction();
    $pdo->exec('DELETE FROM device_playlist_configs');
    $pdo->exec('DELETE FROM activation_session_tokens');
    $pdo->exec('DELETE FROM activation_sessions');
    $pdo->exec('DELETE FROM device_activations');
    $pdo->exec('DELETE FROM devices');
    $pdo->exec("UPDATE activation_codes SET used_devices = 0, updated_at = NOW() WHERE license_type = 'trial'");
    $pdo->exec("UPDATE activation_codes SET status = 'disabled', updated_at = NOW() WHERE license_type = 'trial'");
    $pdo->exec("UPDATE activation_code_metadata SET assigned_device_id = NULL, assigned_public_device_code = NULL WHERE created_by = 'system:trial'");
    audit_admin_action($pdo, 'devices_purged', 'devices', 'all');
    $pdo->commit();
    set_admin_flash('success', 'Appareils, sessions, playlists et essais ont ete purges.');
}

function admin_save_slide(PDO $pdo): void
{
    $slideId = admin_positive_int($_POST['slide_id'] ?? null);
    $sortOrder = filter_var($_POST['sort_order'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 0, 'max_range' => 9999]]);
    if ($sortOrder === false) {
        throw new InvalidArgumentException('Ordre de slide invalide.');
    }
    $title = smartvision_text_substr(trim((string) ($_POST['title'] ?? '')), 0, 120);
    if ($title === '') {
        throw new InvalidArgumentException('Titre de slide obligatoire.');
    }
    $subtitle = clean_optional_text($_POST['subtitle'] ?? null, 255);
    $buttonLabel = clean_optional_text($_POST['button_label'] ?? null, 60);
    $buttonRoute = clean_optional_text($_POST['button_route'] ?? null, 120);
    $imageUrl = clean_optional_text($_POST['image_url'] ?? null, 500);
    $status = (string) ($_POST['status'] ?? 'active');
    if (!in_array($status, ['active', 'disabled'], true)) {
        $status = 'disabled';
    }

    $statement = $pdo->prepare(
        "UPDATE home_slider_ads
         SET sort_order = :sort_order,
             title = :title,
             subtitle = :subtitle,
             button_label = :button_label,
             button_route = :button_route,
             image_url = :image_url,
             status = :status,
             updated_at = NOW()
         WHERE id = :id"
    );
    $statement->execute([
        'sort_order' => (int) $sortOrder,
        'title' => $title,
        'subtitle' => $subtitle,
        'button_label' => $buttonLabel,
        'button_route' => $buttonRoute,
        'image_url' => $imageUrl,
        'status' => $status,
        'id' => $slideId,
    ]);
    audit_admin_action($pdo, 'home_slide_saved', 'home_slider_ads', (string) $slideId);
    set_admin_flash('success', 'Slide publicitaire mis a jour.');
}

function admin_send_notification(PDO $pdo): void
{
    ensure_app_notifications_table($pdo);
    $title = smartvision_text_substr(trim((string) ($_POST['title'] ?? '')), 0, 120);
    $message = smartvision_text_substr(trim((string) ($_POST['message'] ?? '')), 0, 1200);
    $targetScope = (string) ($_POST['target_scope'] ?? 'all');
    $targetValue = admin_normalize_notification_targets((string) ($_POST['target_value'] ?? ''));
    $priority = (string) ($_POST['priority'] ?? 'normal');
    if ($title === '' || $message === '') {
        throw new InvalidArgumentException('Titre et message sont obligatoires.');
    }
    if (!in_array($targetScope, ['all', 'devices', 'users'], true)) {
        throw new InvalidArgumentException('Ciblage notification invalide.');
    }
    if ($targetScope !== 'all' && $targetValue === '') {
        throw new InvalidArgumentException('Renseignez au moins une cible.');
    }
    if (!in_array($priority, ['normal', 'important', 'urgent'], true)) {
        $priority = 'normal';
    }
    $expiresAt = null;
    $expiresInput = trim((string) ($_POST['expires_at'] ?? ''));
    if ($expiresInput !== '') {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $expiresInput, new DateTimeZone('UTC'));
        if (!$date) {
            throw new InvalidArgumentException('Date d expiration invalide.');
        }
        $expiresAt = $date->setTime(23, 59, 59)->format('Y-m-d H:i:s');
    }

    $statement = $pdo->prepare(
        "INSERT INTO app_notifications
            (title, message, target_scope, target_value, priority, status, created_by, expires_at, created_at, updated_at)
         VALUES
            (:title, :message, :target_scope, :target_value, :priority, 'active', :created_by, :expires_at, NOW(), NOW())"
    );
    $statement->execute([
        'title' => $title,
        'message' => $message,
        'target_scope' => $targetScope,
        'target_value' => $targetScope === 'all' ? null : $targetValue,
        'priority' => $priority,
        'created_by' => current_admin_username(),
        'expires_at' => $expiresAt,
    ]);
    $notificationId = (int) $pdo->lastInsertId();
    audit_admin_action($pdo, 'notification_sent', 'app_notification', (string) $notificationId, [
        'target_scope' => $targetScope,
        'priority' => $priority,
    ]);
    set_admin_flash('success', 'Notification creee.');
}

function admin_set_notification_status(PDO $pdo): void
{
    ensure_app_notifications_table($pdo);
    $notificationId = admin_positive_int($_POST['notification_id'] ?? null);
    $status = (string) ($_POST['status'] ?? '');
    if (!in_array($status, ['active', 'disabled'], true)) {
        throw new InvalidArgumentException('Statut notification invalide.');
    }
    $pdo->prepare('UPDATE app_notifications SET status = :status, updated_at = NOW() WHERE id = :id')
        ->execute(['status' => $status, 'id' => $notificationId]);
    audit_admin_action($pdo, 'notification_status_changed', 'app_notification', (string) $notificationId, ['status' => $status]);
    set_admin_flash('success', $status === 'active' ? 'Notification reactivee.' : 'Notification desactivee.');
}

function admin_delete_notification(PDO $pdo): void
{
    ensure_app_notifications_table($pdo);
    $notificationId = admin_positive_int($_POST['notification_id'] ?? null);
    $pdo->prepare('DELETE FROM app_notifications WHERE id = :id')->execute(['id' => $notificationId]);
    audit_admin_action($pdo, 'notification_deleted', 'app_notification', (string) $notificationId);
    set_admin_flash('success', 'Notification supprimee.');
}

function admin_set_contact_message_status(PDO $pdo): void
{
    ensure_contact_messages_table($pdo);
    $messageId = admin_positive_int($_POST['message_id'] ?? null);
    $status = (string) ($_POST['status'] ?? '');
    if (!in_array($status, ['new', 'read', 'handled', 'archived'], true)) {
        throw new InvalidArgumentException('Statut message invalide.');
    }
    $pdo->prepare('UPDATE contact_messages SET status = :status, updated_at = NOW() WHERE id = :id')
        ->execute(['status' => $status, 'id' => $messageId]);
    audit_admin_action($pdo, 'contact_message_status_changed', 'contact_message', (string) $messageId, ['status' => $status]);
    set_admin_flash('success', 'Statut du message mis a jour.');
}

function admin_normalize_notification_targets(string $value): string
{
    $parts = preg_split('/[\s,;]+/', strtolower(trim($value)));
    if (!is_array($parts)) {
        return '';
    }
    $clean = [];
    foreach ($parts as $part) {
        $part = smartvision_text_substr(preg_replace('/[^a-z0-9@._:-]/', '', $part) ?: '', 0, 190);
        if ($part !== '') {
            $clean[] = $part;
        }
    }

    return implode(',', array_values(array_unique($clean)));
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

function admin_load_server_stats(PDO $pdo, array $appStats): array
{
    $databaseVersion = 'Non disponible';
    $databaseSize = 0;
    try {
        $databaseVersion = (string) $pdo->query('SELECT VERSION()')->fetchColumn();
        $sizeQuery = $pdo->prepare(
            "SELECT COALESCE(SUM(data_length + index_length), 0)
             FROM information_schema.TABLES
             WHERE table_schema = DATABASE()"
        );
        $sizeQuery->execute();
        $databaseSize = (int) $sizeQuery->fetchColumn();
    } catch (Throwable $exception) {
        error_log('SmartVision admin server database metrics failed.');
    }

    $cpanel = admin_load_cpanel_stats();

    return [
        'generated_at' => gmdate('Y-m-d H:i:s'),
        'php' => [
            'version' => PHP_VERSION,
            'memory_limit' => ini_get('memory_limit') ?: 'N/D',
            'upload_max_filesize' => ini_get('upload_max_filesize') ?: 'N/D',
            'max_execution_time' => (string) ini_get('max_execution_time'),
            'opcache' => function_exists('opcache_get_status') && @opcache_get_status(false) ? 'Actif' : 'N/D',
        ],
        'database' => [
            'version' => $databaseVersion,
            'size' => admin_format_bytes($databaseSize),
            'active_devices' => (int) ($appStats['active_devices'] ?? 0),
            'pending_sessions' => (int) ($appStats['pending_sessions'] ?? 0),
            'active_trials' => (int) ($appStats['active_trials'] ?? 0),
        ],
        'cpanel' => $cpanel,
    ];
}

function admin_load_cpanel_stats(): array
{
    $config = load_database_config();
    $host = trim((string) ($config['cpanel_host'] ?? ''));
    $username = trim((string) ($config['cpanel_username'] ?? ''));
    $token = trim((string) ($config['cpanel_token'] ?? ''));

    if ($host === '' || $username === '' || $token === '') {
        return [
            'ok' => false,
            'message' => 'Acces cPanel non configure dans le fichier prive serveur.',
            'metrics' => [],
        ];
    }

    $result = admin_cpanel_uapi_request($host, $username, $token, 'StatsBar', 'get_stats', [
        'display' => 'diskusage|bandwidthusage|mysqlusage|cpuusage|memusage|entryprocesses|subdomains|sqldatabases',
    ]);

    if (!$result['ok']) {
        return $result;
    }

    $metrics = [];
    $data = $result['data']['data'] ?? [];
    if (is_array($data)) {
        foreach ($data as $item) {
            if (!is_array($item)) {
                continue;
            }
            $id = (string) ($item['id'] ?? $item['name'] ?? $item['module'] ?? '');
            $label = (string) ($item['name'] ?? $item['label'] ?? $id);
            $value = $item['value'] ?? $item['count'] ?? $item['percent'] ?? null;
            $max = $item['max'] ?? $item['maximum'] ?? null;
            $units = (string) ($item['units'] ?? $item['unit'] ?? '');
            $percent = $item['percent'] ?? null;
            $metrics[] = [
                'id' => $id !== '' ? $id : $label,
                'label' => $label !== '' ? $label : 'Metric cPanel',
                'value' => admin_metric_value($value, $units),
                'max' => admin_metric_value($max, $units),
                'percent' => is_numeric($percent) ? (float) $percent : null,
            ];
        }
    }

    return [
        'ok' => true,
        'message' => $metrics === [] ? 'API cPanel disponible, aucune metrique retournee.' : 'API cPanel connectee.',
        'metrics' => $metrics,
    ];
}

function admin_cpanel_uapi_request(string $host, string $username, string $token, string $module, string $function, array $params = []): array
{
    $host = preg_replace('#^https?://#', '', $host) ?: $host;
    $host = rtrim($host, '/');
    if (!str_contains($host, ':')) {
        $host .= ':2083';
    }
    $url = 'https://' . $host . '/execute/' . rawurlencode($module) . '/' . rawurlencode($function);
    if ($params !== []) {
        $url .= '?' . http_build_query($params);
    }

    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 8,
            'header' => "Authorization: cpanel {$username}:{$token}\r\nAccept: application/json\r\n",
        ],
    ]);

    $response = @file_get_contents($url, false, $context);
    if ($response === false) {
        return [
            'ok' => false,
            'message' => 'API cPanel injoignable pour le moment.',
            'metrics' => [],
        ];
    }

    $json = json_decode($response, true);
    if (!is_array($json) || (int) ($json['status'] ?? 0) !== 1) {
        return [
            'ok' => false,
            'message' => 'Reponse cPanel invalide ou refusee.',
            'metrics' => [],
        ];
    }

    return [
        'ok' => true,
        'message' => 'OK',
        'data' => $json,
        'metrics' => [],
    ];
}

function admin_metric_value(mixed $value, string $units = ''): string
{
    if ($value === null || $value === '') {
        return 'N/D';
    }
    if (is_numeric($value)) {
        $number = (float) $value;
        $formatted = abs($number - round($number)) < 0.01
            ? (string) (int) round($number)
            : number_format($number, 1, ',', ' ');
        return trim($formatted . ' ' . $units);
    }
    return trim((string) $value . ' ' . $units);
}

function admin_format_bytes(int $bytes): string
{
    if ($bytes <= 0) {
        return '0 o';
    }
    $units = ['o', 'Ko', 'Mo', 'Go', 'To'];
    $index = 0;
    $value = (float) $bytes;
    while ($value >= 1024 && $index < count($units) - 1) {
        $value /= 1024;
        $index++;
    }
    return number_format($value, $index === 0 ? 0 : 1, ',', ' ') . ' ' . $units[$index];
}

function admin_search_pattern(string $query): string
{
    return '%' . str_replace(['%', '_'], ['\\%', '\\_'], $query) . '%';
}

function admin_sort_sql(string $default, array $allowed): string
{
    $sort = (string) ($_GET['sort'] ?? '');
    $dir = strtolower((string) ($_GET['dir'] ?? 'desc')) === 'asc' ? 'ASC' : 'DESC';
    $column = $allowed[$sort] ?? $allowed[$default] ?? reset($allowed);
    return $column . ' ' . $dir;
}

function admin_sort_link(string $page, string $label, string $sort, string $query): string
{
    $currentSort = (string) ($_GET['sort'] ?? '');
    $currentDir = strtolower((string) ($_GET['dir'] ?? 'desc'));
    $nextDir = $currentSort === $sort && $currentDir === 'asc' ? 'desc' : 'asc';
    $url = '/admin/?page=' . rawurlencode($page) . '&sort=' . rawurlencode($sort) . '&dir=' . rawurlencode($nextDir);
    if ($query !== '') {
        $url .= '&q=' . rawurlencode($query);
    }
    foreach (['order_status', 'customer_status', 'license_status', 'device_status', 'xtream_status', 'message_status'] as $param) {
        $value = (string) ($_GET[$param] ?? '');
        if ($value !== '') {
            $url .= '&' . rawurlencode($param) . '=' . rawurlencode($value);
        }
    }
    $indicator = $currentSort === $sort ? ($currentDir === 'asc' ? ' asc' : ' desc') : '';
    return '<a class="admin-sort-link" href="' . admin_escape($url) . '">' . admin_escape($label . $indicator) . '</a>';
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
    $whereParts = [];
    if ($query !== '') {
        $whereParts[] = "(o.order_reference LIKE :q OR u.email LIKE :q OR u.display_name LIKE :q OR o.payment_reference LIKE :q)";
        $params['q'] = admin_search_pattern($query);
    }
    $status = (string) ($_GET['order_status'] ?? '');
    if (in_array($status, ['pending', 'paid', 'cancelled'], true)) {
        $whereParts[] = 'o.status = :status';
        $params['status'] = $status;
    }
    if ($whereParts !== []) {
        $sql .= ' WHERE ' . implode(' AND ', $whereParts);
    }
    $sql .= ' ORDER BY ' . admin_sort_sql('date', [
        'reference' => 'o.order_reference',
        'client' => 'u.email',
        'plan' => 'o.plan_label',
        'amount' => 'o.amount_cents',
        'payment' => 'o.status',
        'date' => 'COALESCE(o.paid_at, o.created_at)',
    ]) . ' LIMIT 80';
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
    $whereParts = [];
    if ($query !== '') {
        $whereParts[] = '(u.email LIKE :q OR u.display_name LIKE :q)';
        $params['q'] = admin_search_pattern($query);
    }
    $status = (string) ($_GET['customer_status'] ?? '');
    if (in_array($status, ['active', 'blocked'], true)) {
        $whereParts[] = 'u.status = :status';
        $params['status'] = $status;
    }
    if ($whereParts !== []) {
        $sql .= ' WHERE ' . implode(' AND ', $whereParts);
    }
    $sql .= ' GROUP BY u.id ORDER BY ' . admin_sort_sql('created', [
        'client' => 'u.email',
        'orders' => 'orders_count',
        'spent' => 'total_spent',
        'status' => 'u.status',
        'created' => 'u.id',
    ]) . ' LIMIT 60';
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    $rows = $statement->fetchAll();
    foreach ($rows as &$row) {
        $row['details'] = admin_load_customer_detail($pdo, (int) $row['id']);
    }
    unset($row);
    return $rows;
}

function admin_load_customer_detail(PDO $pdo, int $userId): array
{
    $orders = $pdo->prepare(
        "SELECT o.id, o.order_reference, o.plan_label, o.amount_cents, o.currency, o.status, o.created_at, o.paid_at,
                c.status AS code_status, c.used_devices, c.max_devices, m.code_hint, m.assigned_device_id, m.assigned_public_device_code
         FROM activation_orders o
         LEFT JOIN activation_codes c ON c.id = o.activation_code_id
         LEFT JOIN activation_code_metadata m ON m.code_id = c.id
         WHERE o.user_id = :user_id
         ORDER BY o.id DESC
         LIMIT 8"
    );
    $orders->execute(['user_id' => $userId]);
    $orderRows = $orders->fetchAll();

    $devices = $pdo->prepare(
        "SELECT DISTINCT d.device_id, d.public_device_code, d.device_name, d.status, d.license_status,
                d.xtream_status, d.app_version, d.last_seen_at, d.expires_at
         FROM activation_orders o
         JOIN activation_codes c ON c.id = o.activation_code_id
         LEFT JOIN activation_code_metadata m ON m.code_id = c.id
         LEFT JOIN device_activations a ON a.activation_code_id = c.id
         LEFT JOIN devices d ON d.device_id = COALESCE(m.assigned_device_id, a.device_id)
         WHERE o.user_id = :user_id AND d.device_id IS NOT NULL
         ORDER BY COALESCE(d.last_seen_at, d.first_seen_at, d.created_at) DESC
         LIMIT 8"
    );
    $devices->execute(['user_id' => $userId]);

    return [
        'orders' => $orderRows,
        'devices' => $devices->fetchAll(),
    ];
}

function admin_load_device_history(PDO $pdo, string $deviceId): array
{
    $history = $pdo->prepare(
        "SELECT a.activation_type, a.status, a.starts_at, a.expires_at, a.created_at,
                c.license_type, c.duration_days, c.status AS code_status, m.code_hint
         FROM device_activations a
         LEFT JOIN activation_codes c ON c.id = a.activation_code_id
         LEFT JOIN activation_code_metadata m ON m.code_id = c.id
         WHERE a.device_id = :device_id
         ORDER BY a.id DESC
         LIMIT 10"
    );
    $history->execute(['device_id' => $deviceId]);
    return $history->fetchAll();
}

function admin_load_devices(PDO $pdo, string $query, int $page = 1): array
{
    $perPage = 25;
    $offset = ($page - 1) * $perPage;
    $params = [];
    $whereParts = [];
    if ($query !== '') {
        $whereParts[] = '(d.device_id LIKE :q OR d.public_device_code LIKE :q OR d.device_name LIKE :q OR d.app_version LIKE :q OR d.country_code LIKE :q)';
        $params['q'] = admin_search_pattern($query);
    }
    $deviceStatus = (string) ($_GET['device_status'] ?? '');
    if (in_array($deviceStatus, ['pending', 'active', 'expired', 'blocked'], true)) {
        $whereParts[] = 'd.status = :device_status';
        $params['device_status'] = $deviceStatus;
    }
    $xtreamStatus = (string) ($_GET['xtream_status'] ?? '');
    if (in_array($xtreamStatus, ['missing', 'configured', 'invalid'], true)) {
        $whereParts[] = 'd.xtream_status = :xtream_status';
        $params['xtream_status'] = $xtreamStatus;
    }
    $where = $whereParts === [] ? '' : ' WHERE ' . implode(' AND ', $whereParts);
    $count = $pdo->prepare('SELECT COUNT(*) FROM devices d' . $where);
    $count->execute($params);
    $total = (int) $count->fetchColumn();

    $sql = "SELECT d.id, d.device_id, d.public_device_code, d.device_name, d.platform, d.app_version, d.status,
                   d.license_status, d.trial_status, d.free_with_ads_status, d.xtream_status,
                   d.country_code, d.install_ip_hash, d.last_ip_hash, d.last_user_agent,
                   d.created_at, d.last_seen_at, d.first_seen_at, d.activated_at, d.expires_at,
                   a.activation_type, a.activation_code_id, a.starts_at AS activation_starts_at,
                   c.license_type, c.duration_days, m.code_hint, m.code_ciphertext,
                   CASE WHEN p.device_id IS NULL THEN 0 ELSE 1 END AS playlist_configured
            FROM devices d
            LEFT JOIN device_activations a ON a.id = (
                SELECT da.id FROM device_activations da WHERE da.device_id = d.device_id ORDER BY da.id DESC LIMIT 1
            )
            LEFT JOIN activation_codes c ON c.id = a.activation_code_id
            LEFT JOIN activation_code_metadata m ON m.code_id = c.id
            LEFT JOIN device_playlist_configs p ON p.device_id = d.device_id";
    $sql .= $where . ' ORDER BY ' . admin_sort_sql('activity', [
        'device' => 'd.public_device_code',
        'status' => 'd.status',
        'license' => 'd.license_status',
        'xtream' => 'd.xtream_status',
        'installed' => 'COALESCE(d.first_seen_at, d.created_at)',
        'expires' => 'd.expires_at',
        'activity' => 'COALESCE(d.last_seen_at, d.first_seen_at, d.created_at)',
    ]) . ", d.id DESC LIMIT {$perPage} OFFSET {$offset}";
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    $rows = $statement->fetchAll();
    foreach ($rows as &$row) {
        $row['activation_code'] = decrypt_private_value($row['code_ciphertext'] ?? null) ?: ($row['code_hint'] ?? null);
        $row['history'] = admin_load_device_history($pdo, (string) $row['device_id']);
    }
    unset($row);

    return [
        'rows' => $rows,
        'pagination' => admin_pagination($page, $perPage, $total, 'devices_page'),
    ];
}

function admin_load_codes(PDO $pdo, string $query, int $page = 1): array
{
    $perPage = 30;
    $offset = ($page - 1) * $perPage;
    $params = [];
    $whereParts = [];
    if ($query !== '') {
        $whereParts[] = '(c.label LIKE :q OR m.code_hint LIKE :q OR m.created_by LIKE :q OR m.assigned_public_device_code LIKE :q)';
        $params['q'] = admin_search_pattern($query);
    }
    $status = (string) ($_GET['license_status'] ?? '');
    if (in_array($status, ['active', 'disabled', 'expired'], true)) {
        $whereParts[] = 'c.status = :status';
        $params['status'] = $status;
    }
    $where = $whereParts === [] ? '' : ' WHERE ' . implode(' AND ', $whereParts);
    $count = $pdo->prepare('SELECT COUNT(*) FROM activation_codes c LEFT JOIN activation_code_metadata m ON m.code_id = c.id' . $where);
    $count->execute($params);
    $total = (int) $count->fetchColumn();

    $sql = "SELECT c.id, c.label, c.duration_days, c.max_devices, c.used_devices, c.license_type, c.status,
                   c.valid_until, c.created_at, m.code_hint, m.code_ciphertext, m.created_by, m.last_used_at,
                   m.assigned_device_id, m.assigned_public_device_code,
                   d.device_name, d.status AS device_status, d.last_seen_at AS device_last_seen,
                   a.expires_at AS activation_expires_at,
                   a.activation_type AS activation_type
            FROM activation_codes c
            LEFT JOIN activation_code_metadata m ON m.code_id = c.id
            LEFT JOIN devices d ON d.device_id = m.assigned_device_id
            LEFT JOIN device_activations a ON a.id = (
                SELECT da.id
                FROM device_activations da
                WHERE da.activation_code_id = c.id
                ORDER BY da.id DESC
                LIMIT 1
            )";
    $sql .= $where . ' ORDER BY ' . admin_sort_sql('created', [
        'code' => 'm.code_hint',
        'type' => 'c.license_type',
        'label' => 'c.label',
        'usage' => 'c.used_devices',
        'duration' => 'c.duration_days',
        'expiration' => 'COALESCE(a.expires_at, c.valid_until)',
        'status' => 'c.status',
        'created' => 'c.id',
    ]) . " LIMIT {$perPage} OFFSET {$offset}";
    $statement = $pdo->prepare($sql);
    $statement->execute($params);
    $rows = $statement->fetchAll();
    foreach ($rows as &$row) {
        $row['activation_code'] = decrypt_private_value($row['code_ciphertext'] ?? null) ?: ($row['code_hint'] ?? ('#' . $row['id']));
    }
    unset($row);

    return [
        'rows' => $rows,
        'pagination' => admin_pagination($page, $perPage, $total, 'licenses_page'),
    ];
}

function admin_pagination(int $page, int $perPage, int $total, string $param): array
{
    $pages = max(1, (int) ceil($total / max(1, $perPage)));
    $page = max(1, min($page, $pages));

    return [
        'page' => $page,
        'per_page' => $perPage,
        'total' => $total,
        'pages' => $pages,
        'param' => $param,
    ];
}

function admin_load_slides(PDO $pdo): array
{
    return $pdo->query(
        "SELECT id, sort_order, title, subtitle, button_label, button_route, image_url, status, updated_at
         FROM home_slider_ads ORDER BY sort_order ASC, id ASC"
    )->fetchAll();
}

function admin_load_notifications(PDO $pdo): array
{
    ensure_app_notifications_table($pdo);
    return $pdo->query(
        "SELECT id, title, message, target_scope, target_value, priority, status, created_by, expires_at, created_at, updated_at
         FROM app_notifications
         ORDER BY id DESC
         LIMIT 80"
    )->fetchAll();
}

function admin_load_contact_messages(PDO $pdo, string $query): array
{
    ensure_contact_messages_table($pdo);
    $sql = "SELECT id, name, email, subject, message, status, ip_hash, user_agent, created_at, updated_at
            FROM contact_messages";
    $params = [];
    if ($query !== '') {
        $sql .= " WHERE name LIKE :q OR email LIKE :q OR subject LIKE :q OR message LIKE :q";
        $params['q'] = admin_search_pattern($query);
    }
    $status = (string) ($_GET['message_status'] ?? '');
    if (in_array($status, ['new', 'read', 'handled', 'archived'], true)) {
        $sql .= ($params === [] ? ' WHERE' : ' AND') . ' status = :status';
        $params['status'] = $status;
    }
    $sql .= ' ORDER BY id DESC LIMIT 120';
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
    $configured = admin_auth_is_configured();
    ?><!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Administration SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=3"><link rel="stylesheet" href="/assets/admin-overrides.css?v=4"></head>
<body class="admin-login-body"><main class="admin-login-panel">
    <a class="admin-brand" href="/"><img class="admin-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player"></a>
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
    array $devicesPagination,
    array $codesPagination,
    array $slides,
    array $notifications,
    array $messages,
    array $serverStats,
    array $paymentPacks,
    array $gammalPayments,
    array $emailAdmin,
    ?array $flash,
    ?array $generatedCode,
    string $query,
    string $page,
): void {
    $maxRevenue = max(1, ...array_map(static fn(array $item): int => $item['revenue'], $revenueSeries));
    $pages = [
        'overview' => ['Vue d ensemble', 'Suivi commercial et operationnel SmartVision.'],
        'orders' => ['Commandes', 'Commandes, paiements test et codes generes.'],
        'customers' => ['Clients', 'Comptes clients et historique d achat.'],
        'licenses' => ['Licences', 'Generation manuelle et gestion des codes SmartVision.'],
        'payments' => ['Paiements', 'Packs Gammal Tech et transactions en attente de validation.'],
        'emails' => ['Emails', 'Configuration SMTP, templates transactionnels et journal des envois.'],
        'devices' => ['Appareils', 'TV associees, essais, licences et configuration Xtream.'],
        'notifications' => ['Notifications', 'Messages envoyes vers toutes les TV ou vers des cibles precises.'],
        'messages' => ['Messages clients', 'Demandes envoyees depuis le formulaire de contact.'],
        'slides' => ['Slides Home', 'Emplacements publicitaires prives visibles sur la Home.'],
        'server' => ['Serveur', 'Statistiques techniques, ressources cPanel et signaux de charge.'],
        'audit' => ['Journal', 'Dernieres actions administrateur.'],
    ];
    $heading = $pages[$page] ?? $pages['overview'];
    ?><!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Administration | SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=3"><link rel="stylesheet" href="/assets/admin-overrides.css?v=4"></head>
<body class="admin-body">
<aside class="admin-sidebar">
    <a class="admin-brand" href="/admin/"><img class="admin-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player"></a>
    <nav><?php foreach ($pages as $pageKey => $pageMeta): ?><a class="<?= $page === $pageKey ? 'active' : '' ?>" href="/admin/?page=<?= admin_escape($pageKey) ?>"><?= admin_escape($pageMeta[0]) ?></a><?php endforeach; ?></nav>
    <div class="sidebar-footer"><a href="/" target="_blank" rel="noopener">Voir le site</a><form method="post" action="/admin/logout.php"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><button type="submit">Deconnexion</button></form></div>
</aside>
<div class="admin-shell">
    <header class="admin-topbar"><form method="get" class="admin-search"><input type="hidden" name="page" value="<?= admin_escape($page) ?>"><label class="sr-only" for="admin-search">Rechercher</label><input id="admin-search" name="q" value="<?= admin_escape($query) ?>" placeholder="Rechercher client, commande, licence ou appareil"><button type="submit">Rechercher</button></form><div class="admin-account"><span><?= admin_escape(current_admin_username()) ?></span><small>Administrateur</small></div></header>
    <main class="admin-main">
        <section class="admin-page-heading"><div><h1><?= admin_escape($heading[0]) ?></h1><p><?= admin_escape($heading[1]) ?></p></div></section>
        <?php if ($flash): ?><div class="admin-notice <?= admin_escape((string) ($flash['type'] ?? '')) ?>"><?= admin_escape((string) ($flash['message'] ?? '')) ?></div><?php endif; ?>
        <?php if ($generatedCode): ?><div class="generated-license generated-license-list"><div><span>Code(s) genere(s)</span><div class="generated-code-grid"><?php foreach ($generatedCode as $generatedIndex => $plainGeneratedCode): ?><strong id="generated-code-<?= (int) $generatedIndex ?>"><?= admin_escape((string) $plainGeneratedCode) ?></strong><button class="admin-button compact primary" type="button" data-copy-target="generated-code-<?= (int) $generatedIndex ?>">Copier</button><?php endforeach; ?></div></div></div><?php endif; ?>

        <?php if ($page === 'overview'): ?>
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
        <?php endif; ?>

        <?php if ($page === 'orders'): ?>
        <section class="admin-panel" id="orders"><div class="admin-panel-heading"><div><h2>Commandes recentes</h2><p><?= count($orders) ?> resultat(s)</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="orders"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Statut<select name="order_status"><option value="">Tous</option><?php foreach (['pending', 'paid', 'cancelled'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['order_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form></div><div class="admin-table-wrap"><table><thead><tr><th><?= admin_sort_link('orders', 'Reference', 'reference', $query) ?></th><th><?= admin_sort_link('orders', 'Client', 'client', $query) ?></th><th><?= admin_sort_link('orders', 'Plan', 'plan', $query) ?></th><th><?= admin_sort_link('orders', 'Montant', 'amount', $query) ?></th><th><?= admin_sort_link('orders', 'Paiement', 'payment', $query) ?></th><th>Licence</th><th><?= admin_sort_link('orders', 'Date', 'date', $query) ?></th><th>Actions</th></tr></thead><tbody>
        <?php if ($orders === []): ?><tr><td colspan="8" class="admin-empty">Aucune commande.</td></tr><?php endif; ?>
        <?php foreach ($orders as $order): ?><tr><td><strong><?= admin_escape($order['order_reference'] ?: 'SV-' . $order['id']) ?></strong><small><?= admin_escape($order['payment_reference'] ?: '-') ?></small></td><td><?= admin_escape($order['display_name'] ?: $order['email']) ?><small><?= admin_escape($order['email']) ?></small></td><td><?= admin_escape($order['plan_label']) ?></td><td><?= commerce_money((int) $order['amount_cents'], (string) $order['currency']) ?></td><td><span class="admin-state <?= admin_escape($order['status']) ?>"><?= admin_escape($order['status']) ?></span></td><td><?= admin_escape($order['code_hint'] ?: '-') ?><small><?= admin_escape($order['code_status'] ?: '-') ?></small></td><td><?= admin_escape($order['paid_at'] ?: $order['created_at']) ?></td><td><?php if ($order['status'] === 'paid' && (int) $order['used_devices'] === 0): ?><form method="post" data-confirm="Annuler cette commande et desactiver sa licence ?"><input type="hidden" name="redirect_page" value="orders"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="cancel_order"><input type="hidden" name="order_id" value="<?= (int) $order['id'] ?>"><button class="admin-button compact danger" type="submit">Annuler</button></form><?php else: ?><span class="muted">-</span><?php endif; ?></td></tr><?php endforeach; ?>
        </tbody></table></div></section>
        <?php endif; ?>

        <?php if ($page === 'customers'): ?>
        <section class="admin-panel" id="customers"><div class="admin-panel-heading"><div><h2>Clients</h2><p><?= count($users) ?> resultat(s)</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="customers"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Statut<select name="customer_status"><option value="">Tous</option><?php foreach (['active', 'blocked'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['customer_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form></div><div class="admin-table-wrap"><table><thead><tr><th><?= admin_sort_link('customers', 'Client', 'client', $query) ?></th><th><?= admin_sort_link('customers', 'Commandes', 'orders', $query) ?></th><th><?= admin_sort_link('customers', 'Depense', 'spent', $query) ?></th><th><?= admin_sort_link('customers', 'Statut', 'status', $query) ?></th><th>Actions</th></tr></thead><tbody><?php if ($users === []): ?><tr><td colspan="5" class="admin-empty">Aucun client.</td></tr><?php endif; ?><?php foreach ($users as $user): $customerModalId = 'customer-modal-' . (int) $user['id']; ?><tr class="admin-click-row" tabindex="0" data-modal-target="<?= admin_escape($customerModalId) ?>"><td><strong><?= admin_escape($user['display_name'] ?: $user['email']) ?></strong><small><?= admin_escape($user['email']) ?></small><small>Inscrit <?= admin_escape($user['created_at'] ?: '-') ?></small></td><td><?= (int) $user['orders_count'] ?></td><td><?= commerce_money((int) $user['total_spent']) ?></td><td><span class="admin-state <?= admin_escape($user['status']) ?>"><?= admin_escape($user['status']) ?></span></td><td><form method="post"><input type="hidden" name="redirect_page" value="customers"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_user_status"><input type="hidden" name="user_id" value="<?= (int) $user['id'] ?>"><input type="hidden" name="status" value="<?= $user['status'] === 'active' ? 'blocked' : 'active' ?>"><button class="admin-button compact secondary" type="submit"><?= $user['status'] === 'active' ? 'Bloquer' : 'Reactiver' ?></button></form></td></tr><?php endforeach; ?></tbody></table></div><?php foreach ($users as $user): admin_render_customer_modal($user); endforeach; ?></section>
        <?php endif; ?>

        <?php if ($page === 'licenses'): ?>
        <section class="admin-panel generate-panel" id="licenses"><div class="admin-panel-heading"><div><h2>Licences et codes</h2><p><?= (int) $codesPagination['total'] ?> code(s) SmartVision, format public 10 caracteres.</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="licenses"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Statut<select name="license_status"><option value="">Tous</option><?php foreach (['active', 'disabled', 'expired'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['license_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form></div><form method="post" class="generate-code-form generate-code-form-wide" data-license-generator><input type="hidden" name="redirect_page" value="licenses"><input type="hidden" name="action" value="generate_code"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label>Libelle<input name="label" maxlength="100" placeholder="Client, lot ou campagne"></label><label>Type<select name="license_type"><option value="paid">Payant</option><option value="trial">Essai gratuit</option><option value="free">Gratuit</option><option value="promo">Promo</option><option value="manual">Manuel</option></select></label><label>Duree en jours<input name="duration_days" type="number" min="1" max="36500" value="365" required data-duration-days></label><label>Nombre<input name="quantity" type="number" min="1" max="200" value="1" required></label><label>Appareils<input name="max_devices" type="number" min="1" max="1000" value="1" required></label><label>Valide jusqu'au<input name="valid_until" type="date" value="<?= admin_escape(gmdate('Y-m-d', strtotime('+365 days'))) ?>" data-valid-until></label><button class="admin-button primary" type="submit">Generer</button></form><div class="admin-table-wrap"><table><thead><tr><th><?= admin_sort_link('licenses', 'Code', 'code', $query) ?></th><th><?= admin_sort_link('licenses', 'Type', 'type', $query) ?></th><th><?= admin_sort_link('licenses', 'Libelle', 'label', $query) ?></th><th>Appareil</th><th><?= admin_sort_link('licenses', 'Usage', 'usage', $query) ?></th><th><?= admin_sort_link('licenses', 'Duree', 'duration', $query) ?></th><th><?= admin_sort_link('licenses', 'Expiration', 'expiration', $query) ?></th><th><?= admin_sort_link('licenses', 'Statut', 'status', $query) ?></th><th>Origine</th><th>Actions</th></tr></thead><tbody>
        <?php if ($codes === []): ?><tr><td colspan="10" class="admin-empty">Aucun code licence.</td></tr><?php endif; ?>
        <?php foreach ($codes as $code): ?><tr data-code-id="<?= (int) $code['id'] ?>" data-label="<?= admin_escape($code['label'] ?: '') ?>"><td><strong class="license-code"><?= admin_escape((string) ($code['activation_code'] ?: '#' . $code['id'])) ?></strong><small>Genere <?= admin_escape($code['created_at']) ?></small></td><td><span class="admin-state <?= admin_escape($code['license_type'] ?: 'manual') ?>"><?= admin_escape($code['license_type'] ?: 'manual') ?></span><small><?= admin_escape($code['activation_type'] ?: '-') ?></small></td><td><?= admin_escape($code['label'] ?: '-') ?></td><td><strong><?= admin_escape($code['assigned_public_device_code'] ?: '-') ?></strong><small><?= admin_escape($code['device_name'] ?: $code['assigned_device_id'] ?: '-') ?></small></td><td><?= (int) $code['used_devices'] ?> / <?= (int) $code['max_devices'] ?><small>Derniere: <?= admin_escape($code['last_used_at'] ?: '-') ?></small></td><td><?= (int) $code['duration_days'] ?> jours</td><td><?= admin_escape($code['activation_expires_at'] ?: $code['valid_until'] ?: '-') ?></td><td><span class="admin-state <?= admin_escape($code['status']) ?>"><?= admin_escape($code['status']) ?></span><small><?= admin_escape($code['device_status'] ?: '-') ?></small></td><td><?= admin_escape($code['created_by'] ?: '-') ?></td><td class="admin-actions"><form method="post"><input type="hidden" name="redirect_page" value="licenses"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_code_status"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><input type="hidden" name="status" value="<?= $code['status'] === 'active' ? 'disabled' : 'active' ?>"><button class="admin-button compact secondary" type="submit"><?= $code['status'] === 'active' ? 'Desactiver' : 'Reactiver' ?></button></form><?php if ($code['status'] !== 'expired'): ?><form method="post" data-confirm="Marquer cette licence comme expiree ?"><input type="hidden" name="redirect_page" value="licenses"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_code_status"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><input type="hidden" name="status" value="expired"><button class="admin-button compact secondary" type="submit">Expirer</button></form><?php endif; ?><?php if ((int) $code['used_devices'] > 0): ?><form method="post" data-confirm="Revoquer ce code et expirer ses appareils ?"><input type="hidden" name="redirect_page" value="licenses"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="revoke_code"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><button class="admin-button compact danger" type="submit">Revoquer</button></form><?php elseif (!str_starts_with((string) $code['created_by'], 'customer:')): ?><form method="post" data-confirm="Supprimer ce code inutilise ?"><input type="hidden" name="redirect_page" value="licenses"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="delete_code"><input type="hidden" name="code_id" value="<?= (int) $code['id'] ?>"><button class="admin-button compact danger" type="submit">Supprimer</button></form><?php endif; ?></td></tr><?php endforeach; ?>
        </tbody></table></div><?= admin_render_pagination($codesPagination, $page, $query) ?></section>
        <?php endif; ?>

        <?php if ($page === 'payments'): ?>
        <section class="admin-panel payment-pack-panel" id="payments">
            <div class="admin-panel-heading">
                <div>
                    <h2>Packs Gammal Tech</h2>
                    <p>Configurez un lien HTTPS Gammal Tech par offre. Un pack desactive ne peut pas etre commande.</p>
                </div>
            </div>
            <form method="post" class="payment-pack-form">
                <input type="hidden" name="redirect_page" value="payments">
                <input type="hidden" name="action" value="save_payment_packs">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <div class="payment-pack-grid">
                    <?php foreach ($paymentPacks as $packKey => $pack): ?>
                    <article class="payment-pack-card">
                        <div class="payment-pack-heading">
                            <div><strong><?= admin_escape($pack['label']) ?></strong><span><?= admin_escape($pack['amount']) ?></span></div>
                            <label class="payment-pack-toggle"><input type="checkbox" name="payment_enabled[<?= admin_escape((string) $packKey) ?>]" value="1"<?= $pack['enabled'] ? ' checked' : '' ?>> Actif</label>
                        </div>
                        <p><?= admin_escape($pack['description']) ?></p>
                        <label>Lien de paiement Gammal Tech
                            <input name="payment_url[<?= admin_escape((string) $packKey) ?>]" type="url" maxlength="1000" placeholder="https://...gammal.tech/..." value="<?= admin_escape($pack['url']) ?>">
                        </label>
                    </article>
                    <?php endforeach; ?>
                </div>
                <div class="payment-pack-actions">
                    <p>Les paiements Gammal Tech doivent etre approuves et leur callback marchand configure par Gammal Tech avant la mise en production.</p>
                    <button class="admin-button primary" type="submit">Enregistrer les packs</button>
                </div>
            </form>
        </section>
        <section class="admin-panel" id="gammal-payment-reviews">
            <div class="admin-panel-heading">
                <div>
                    <h2>Retours Gammal Tech</h2>
                    <p>Une licence n’est créée qu’après validation administrateur tant que la vérification serveur Gammal n’est pas disponible.</p>
                </div>
            </div>
            <div class="admin-table-wrap"><table>
                <thead><tr><th>Transaction</th><th>Client</th><th>Pack</th><th>Montants Gammal</th><th>État</th><th>Date</th><th>Actions</th></tr></thead>
                <tbody>
                <?php if ($gammalPayments === []): ?><tr><td colspan="7" class="admin-empty">Aucun retour de paiement enregistré.</td></tr><?php endif; ?>
                <?php foreach ($gammalPayments as $payment): ?>
                <tr>
                    <td><strong><?= admin_escape($payment['txn']) ?></strong><small><?= admin_escape($payment['order_reference'] ?: '-') ?></small></td>
                    <td><?= admin_escape($payment['display_name'] ?: $payment['email']) ?><small><?= admin_escape($payment['email']) ?></small></td>
                    <td><?= admin_escape($payment['plan_label']) ?><small>Prix client <?= commerce_money((int) $payment['amount_cents'], (string) $payment['currency']) ?></small></td>
                    <td><?= commerce_money((int) $payment['reported_amount_cents'], (string) $payment['currency']) ?><small>Attendu <?= commerce_money((int) $payment['expected_amount_cents'], (string) $payment['currency']) ?></small></td>
                    <td><span class="admin-state <?= admin_escape($payment['verification_status']) ?>"><?= admin_escape($payment['verification_status']) ?></span><small>Commande <?= admin_escape($payment['order_status'] ?: '-') ?></small></td>
                    <td><?= admin_escape($payment['created_at']) ?><small><?= admin_escape($payment['reviewed_by'] ?: '-') ?></small></td>
                    <td class="admin-actions">
                        <?php if ($payment['verification_status'] === 'pending_review'): ?>
                        <form method="post" data-confirm="Confirmer ce paiement et générer la licence ?">
                            <input type="hidden" name="redirect_page" value="payments">
                            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                            <input type="hidden" name="action" value="approve_gammal_payment">
                            <input type="hidden" name="payment_id" value="<?= (int) $payment['id'] ?>">
                            <button class="admin-button compact primary" type="submit">Approuver</button>
                        </form>
                        <form method="post" data-confirm="Rejeter ce retour de paiement sans créer de licence ?">
                            <input type="hidden" name="redirect_page" value="payments">
                            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                            <input type="hidden" name="action" value="reject_gammal_payment">
                            <input type="hidden" name="payment_id" value="<?= (int) $payment['id'] ?>">
                            <button class="admin-button compact danger" type="submit">Rejeter</button>
                        </form>
                        <?php else: ?><span class="muted">Traité</span><?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; ?>
                </tbody>
            </table></div>
        </section>
        <?php endif; ?>

        <?php if ($page === 'emails'):
            $mailConfig = $emailAdmin['config'];
        ?>
        <section class="admin-panel">
            <div class="admin-panel-heading"><div><h2>Configuration SMTP HostCreed</h2><p>Le mot de passe n’est jamais stocké dans la base ni affiché ici.</p></div></div>
            <form method="post" class="generate-code-form generate-code-form-wide">
                <input type="hidden" name="redirect_page" value="emails">
                <input type="hidden" name="action" value="save_email_settings">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <label>Services externes<select name="external_services_enabled"><option value="0"<?= empty($mailConfig['external_enabled']) ? ' selected' : '' ?>>Désactivés</option><option value="1"<?= !empty($mailConfig['external_enabled']) ? ' selected' : '' ?>>Activés</option></select></label>
                <label>SMTP<select name="smtp_enabled"><option value="0"<?= empty($mailConfig['smtp_enabled']) ? ' selected' : '' ?>>Désactivé</option><option value="1"<?= !empty($mailConfig['smtp_enabled']) ? ' selected' : '' ?>>Activé</option></select></label>
                <label>Hôte<input name="smtp_host" value="<?= admin_escape($mailConfig['host']) ?>" required></label>
                <label>Port<input name="smtp_port" type="number" min="1" max="65535" value="<?= (int) $mailConfig['port'] ?>" required></label>
                <label>Sécurité<select name="smtp_secure"><option value="ssl"<?= $mailConfig['secure'] === 'ssl' ? ' selected' : '' ?>>SSL</option><option value="tls"<?= $mailConfig['secure'] === 'tls' ? ' selected' : '' ?>>STARTTLS</option></select></label>
                <label>Utilisateur SMTP<input name="smtp_user" value="<?= admin_escape($mailConfig['username']) ?>"></label>
                <label>Email expéditeur<input name="smtp_from_email" type="email" value="<?= admin_escape($mailConfig['from_email']) ?>"></label>
                <label>Nom expéditeur<input name="smtp_from_name" value="<?= admin_escape($mailConfig['from_name']) ?>"></label>
                <label>Reply-To<input name="smtp_reply_to" type="email" value="<?= admin_escape($mailConfig['reply_to']) ?>"></label>
                <label>Email administrateur<input name="admin_notification_email" type="email" value="<?= admin_escape($mailConfig['admin_email']) ?>"></label>
                <button class="admin-button primary" type="submit">Enregistrer</button>
            </form>
            <p class="muted">Mot de passe SMTP : <?= !empty($emailAdmin['password_configured']) ? 'configuré dans le fichier privé' : 'absent — ajoutez SMTP_PASSWORD dans local.properties en dev et dans la configuration privée serveur avant production' ?>.</p>
            <form method="post" class="admin-filter-row">
                <input type="hidden" name="redirect_page" value="emails">
                <input type="hidden" name="action" value="send_test_email">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <label>Destinataire test<input name="test_recipient" type="email" required></label>
                <button class="admin-button secondary" type="submit">Tester l’email</button>
            </form>
        </section>
        <section class="admin-panel">
            <div class="admin-panel-heading"><div><h2>Templates</h2><p>Les variables entre doubles accolades sont conservées lors du rendu.</p></div></div>
            <div class="slide-admin-grid">
            <?php foreach ($emailAdmin['templates'] as $template): ?>
                <form method="post" class="slide-card">
                    <input type="hidden" name="redirect_page" value="emails">
                    <input type="hidden" name="action" value="save_email_template">
                    <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                    <input type="hidden" name="template_id" value="<?= (int) $template['id'] ?>">
                    <strong><?= admin_escape($template['name']) ?></strong><small><?= admin_escape($template['template_key']) ?></small>
                    <label>Sujet<input name="subject_template" maxlength="255" value="<?= admin_escape($template['subject_template']) ?>" required></label>
                    <label>Titre<input name="title_template" maxlength="255" value="<?= admin_escape($template['title_template']) ?>" required></label>
                    <label>Introduction HTML<textarea name="intro_html"><?= admin_escape($template['intro_html']) ?></textarea></label>
                    <label>Corps HTML<textarea name="body_html"><?= admin_escape($template['body_html']) ?></textarea></label>
                    <label>Pied HTML<textarea name="footer_html"><?= admin_escape($template['footer_html']) ?></textarea></label>
                    <label><input name="is_active" type="checkbox" value="1"<?= (int) $template['is_active'] === 1 ? ' checked' : '' ?>> Template actif</label>
                    <button class="admin-button primary" type="submit">Enregistrer le template</button>
                </form>
            <?php endforeach; ?>
            </div>
        </section>
        <section class="admin-panel">
            <div class="admin-panel-heading"><div><h2>Journal email</h2><p>80 dernières tentatives. En local, un SMTP désactivé produit volontairement le statut pending.</p></div></div>
            <div class="admin-table-wrap"><table>
                <thead><tr><th>Date</th><th>Type</th><th>Destinataire</th><th>Sujet</th><th>Statut</th><th>Erreur</th></tr></thead>
                <tbody>
                <?php if ($emailAdmin['logs'] === []): ?><tr><td colspan="6" class="admin-empty">Aucun email journalisé.</td></tr><?php endif; ?>
                <?php foreach ($emailAdmin['logs'] as $log): ?><tr>
                    <td><?= admin_escape($log['created_at']) ?></td>
                    <td><?= admin_escape($log['email_type']) ?><small><?= admin_escape($log['template_key']) ?></small></td>
                    <td><?= admin_escape($log['recipient_email'] ?: '-') ?></td>
                    <td><?= admin_escape($log['subject'] ?: '-') ?></td>
                    <td><span class="admin-state <?= admin_escape($log['status']) ?>"><?= admin_escape($log['status']) ?></span></td>
                    <td><small><?= admin_escape($log['error_message'] ?: '-') ?></small></td>
                </tr><?php endforeach; ?>
                </tbody>
            </table></div>
        </section>
        <?php endif; ?>

        <?php if ($page === 'devices'): ?>
        <section class="admin-panel" id="devices"><div class="admin-panel-heading"><div><h2>Appareils</h2><p><?= (int) $devicesPagination['total'] ?> appareil(s), tries par derniere activite decroissante.</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="devices"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Etat<select name="device_status"><option value="">Tous</option><?php foreach (['pending', 'active', 'expired', 'blocked'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['device_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><label>Xtream<select name="xtream_status"><option value="">Tous</option><?php foreach (['missing', 'configured', 'invalid'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['xtream_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form><form method="post" class="purge-form" data-confirm="Supprimer tous les appareils, sessions, activations et playlists ?"><input type="hidden" name="redirect_page" value="devices"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="purge_devices"><input name="confirmation" placeholder="PURGER" aria-label="Confirmation purge"><button class="admin-button danger" type="submit">Purger appareils</button></form></div><div class="admin-table-wrap"><table><thead><tr><th><?= admin_sort_link('devices', 'Appareil', 'device', $query) ?></th><th><?= admin_sort_link('devices', 'Etat', 'status', $query) ?></th><th><?= admin_sort_link('devices', 'Licence', 'license', $query) ?></th><th><?= admin_sort_link('devices', 'Xtream', 'xtream', $query) ?></th><th><?= admin_sort_link('devices', 'Installation', 'installed', $query) ?></th><th><?= admin_sort_link('devices', 'Expiration / activite', 'activity', $query) ?></th><th>Actions</th></tr></thead><tbody>
        <?php if ($devices === []): ?><tr><td colspan="7" class="admin-empty">Aucun appareil.</td></tr><?php endif; ?>
        <?php foreach ($devices as $device): admin_render_device_row($device); endforeach; ?>
        </tbody></table></div><?= admin_render_pagination($devicesPagination, $page, $query) ?><?php foreach ($devices as $device): admin_render_device_modal($device); endforeach; ?></section>
        <?php endif; ?>

        <?php if ($page === 'notifications'): ?>
        <section class="admin-panel" id="notifications"><div class="admin-panel-heading"><div><h2>Envoyer une notification</h2><p>Pour les cibles multiples, separez les IDs, codes appareil, emails ou IDs client par virgule, espace ou point-virgule.</p></div></div><form method="post" class="notification-form"><input type="hidden" name="redirect_page" value="notifications"><input type="hidden" name="action" value="send_notification"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label class="notification-title">Titre<input name="title" maxlength="120" required placeholder="Maintenance, information, promotion..."></label><label>Priorite<select name="priority"><option value="normal">Normale</option><option value="important">Importante</option><option value="urgent">Urgente</option></select></label><label>Ciblage<select name="target_scope" data-notification-scope><option value="all">Tous les utilisateurs</option><option value="devices">Appareil(s)</option><option value="users">Client(s)</option></select></label><label>Expiration<input name="expires_at" type="date"></label><label class="notification-targets">Cibles<input name="target_value" placeholder="device_id, code public, email ou id client"></label><label class="notification-message">Message<textarea name="message" maxlength="1200" required placeholder="Message visible dans l'application TV"></textarea></label><div class="notification-submit"><button class="admin-button primary" type="submit">Envoyer la notification</button></div></form></section>
        <section class="admin-panel" id="notifications-list"><div class="admin-panel-heading"><div><h2>Notifications recentes</h2><p><?= count($notifications) ?> notification(s)</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Message</th><th>Ciblage</th><th>Priorite</th><th>Statut</th><th>Dates</th><th>Actions</th></tr></thead><tbody>
        <?php if ($notifications === []): ?><tr><td colspan="6" class="admin-empty">Aucune notification.</td></tr><?php endif; ?>
        <?php foreach ($notifications as $notification): ?><tr><td><strong><?= admin_escape($notification['title']) ?></strong><small><?= admin_escape(smartvision_text_substr((string) $notification['message'], 0, 180)) ?></small></td><td><span class="admin-state <?= admin_escape($notification['target_scope']) ?>"><?= admin_escape($notification['target_scope']) ?></span><small><?= admin_escape($notification['target_value'] ?: 'Tous') ?></small></td><td><span class="admin-state <?= admin_escape($notification['priority']) ?>"><?= admin_escape($notification['priority']) ?></span></td><td><span class="admin-state <?= admin_escape($notification['status']) ?>"><?= admin_escape($notification['status']) ?></span></td><td><?= admin_escape($notification['created_at']) ?><small>Expire: <?= admin_escape($notification['expires_at'] ?: '-') ?></small></td><td class="admin-actions"><form method="post"><input type="hidden" name="redirect_page" value="notifications"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_notification_status"><input type="hidden" name="notification_id" value="<?= (int) $notification['id'] ?>"><input type="hidden" name="status" value="<?= $notification['status'] === 'active' ? 'disabled' : 'active' ?>"><button class="admin-button compact secondary" type="submit"><?= $notification['status'] === 'active' ? 'Desactiver' : 'Reactiver' ?></button></form><form method="post" data-confirm="Supprimer cette notification ?"><input type="hidden" name="redirect_page" value="notifications"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="delete_notification"><input type="hidden" name="notification_id" value="<?= (int) $notification['id'] ?>"><button class="admin-button compact danger" type="submit">Supprimer</button></form></td></tr><?php endforeach; ?>
        </tbody></table></div></section>
        <?php endif; ?>

        <?php if ($page === 'messages'): ?>
        <section class="admin-panel" id="messages"><div class="admin-panel-heading"><div><h2>Messages clients</h2><p><?= count($messages) ?> message(s)</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="messages"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Statut<select name="message_status"><option value="">Tous</option><?php foreach (['new', 'read', 'handled', 'archived'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['message_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form></div><div class="admin-table-wrap"><table><thead><tr><th>Client</th><th>Sujet</th><th>Message</th><th>Statut</th><th>Date</th><th>Actions</th></tr></thead><tbody>
        <?php if ($messages === []): ?><tr><td colspan="6" class="admin-empty">Aucun message client.</td></tr><?php endif; ?>
        <?php foreach ($messages as $message): ?><tr><td><strong><?= admin_escape($message['name']) ?></strong><small><?= admin_escape($message['email']) ?></small></td><td><?= admin_escape($message['subject']) ?></td><td><small><?= admin_escape(smartvision_text_substr((string) $message['message'], 0, 220)) ?></small></td><td><span class="admin-state <?= admin_escape($message['status']) ?>"><?= admin_escape($message['status']) ?></span></td><td><?= admin_escape($message['created_at']) ?><small>MAJ <?= admin_escape($message['updated_at'] ?: '-') ?></small></td><td class="admin-actions"><a class="admin-button compact primary" href="mailto:<?= admin_escape(rawurlencode((string) $message['email'])) ?>?subject=<?= admin_escape(rawurlencode('Re: ' . (string) $message['subject'])) ?>">Repondre</a><form method="post"><input type="hidden" name="redirect_page" value="messages"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="set_contact_message_status"><input type="hidden" name="message_id" value="<?= (int) $message['id'] ?>"><select name="status" aria-label="Statut message"><option value="read"<?= $message['status'] === 'read' ? ' selected' : '' ?>>Lu</option><option value="handled"<?= $message['status'] === 'handled' ? ' selected' : '' ?>>Traite</option><option value="archived"<?= $message['status'] === 'archived' ? ' selected' : '' ?>>Archive</option><option value="new"<?= $message['status'] === 'new' ? ' selected' : '' ?>>Nouveau</option></select><button class="admin-button compact secondary" type="submit">OK</button></form></td></tr><tr class="message-detail-row"><td colspan="6"><strong>Message complet</strong><p><?= nl2br(admin_escape($message['message'])) ?></p><small>IP hash <?= admin_escape($message['ip_hash'] ?: '-') ?> - UA <?= admin_escape($message['user_agent'] ? 'present' : '-') ?></small></td></tr><?php endforeach; ?>
        </tbody></table></div></section>
        <?php endif; ?>

        <?php if ($page === 'slides'): ?>
        <section class="admin-panel" id="slides"><div class="admin-panel-heading"><div><h2>Slides publicitaires Home</h2><p>Images, textes et boutons exposes par l API /api/home_slides.php.</p></div></div><div class="slide-admin-grid">
        <?php if ($slides === []): ?><p class="admin-empty">Aucun slide configure. Rejouez l installation SQL.</p><?php endif; ?>
        <?php foreach ($slides as $slide): ?><form method="post" class="slide-card"><input type="hidden" name="redirect_page" value="slides"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="save_slide"><input type="hidden" name="slide_id" value="<?= (int) $slide['id'] ?>"><label>Ordre<input name="sort_order" type="number" min="0" max="9999" value="<?= (int) $slide['sort_order'] ?>"></label><label>Titre<input name="title" maxlength="120" value="<?= admin_escape($slide['title'] ?: '') ?>"></label><label>Sous-titre<textarea name="subtitle" maxlength="255"><?= admin_escape($slide['subtitle'] ?: '') ?></textarea></label><label>Bouton<input name="button_label" maxlength="60" value="<?= admin_escape($slide['button_label'] ?: '') ?>"></label><label>Route bouton<input name="button_route" maxlength="120" value="<?= admin_escape($slide['button_route'] ?: '') ?>"></label><label>Image URL<input name="image_url" maxlength="500" value="<?= admin_escape($slide['image_url'] ?: '') ?>"></label><label>Etat<select name="status"><option value="active"<?= $slide['status'] === 'active' ? ' selected' : '' ?>>Actif</option><option value="disabled"<?= $slide['status'] === 'disabled' ? ' selected' : '' ?>>Desactive</option></select></label><button class="admin-button primary" type="submit">Enregistrer le slide</button><small class="muted">MAJ <?= admin_escape($slide['updated_at'] ?: '-') ?></small></form><?php endforeach; ?>
        </div></section>
        <?php endif; ?>

        <?php if ($page === 'server'): ?>
        <section class="admin-kpis server-kpis" aria-label="Indicateurs serveur">
            <?= admin_kpi('PHP', $serverStats['php']['version'] ?? 'N/D', 'blue') ?>
            <?= admin_kpi('MySQL/MariaDB', $serverStats['database']['version'] ?? 'N/D', 'cyan') ?>
            <?= admin_kpi('Taille base', $serverStats['database']['size'] ?? 'N/D', 'green') ?>
            <?= admin_kpi('Appareils actifs', $serverStats['database']['active_devices'] ?? 0, 'green') ?>
            <?= admin_kpi('Sessions attente', $serverStats['database']['pending_sessions'] ?? 0, 'orange') ?>
            <?= admin_kpi('cPanel', !empty($serverStats['cpanel']['ok']) ? 'Connecte' : 'A verifier', !empty($serverStats['cpanel']['ok']) ? 'cyan' : 'orange') ?>
        </section>
        <div class="server-dashboard-grid">
            <section class="admin-panel server-panel"><div class="admin-panel-heading"><div><h2>Runtime application</h2><p>Derniere lecture <?= admin_escape((string) ($serverStats['generated_at'] ?? '-')) ?> UTC</p></div></div><div class="server-metric-list">
                <div><span>Memoire PHP</span><strong><?= admin_escape((string) ($serverStats['php']['memory_limit'] ?? 'N/D')) ?></strong></div>
                <div><span>Upload max</span><strong><?= admin_escape((string) ($serverStats['php']['upload_max_filesize'] ?? 'N/D')) ?></strong></div>
                <div><span>Execution max</span><strong><?= admin_escape((string) ($serverStats['php']['max_execution_time'] ?? 'N/D')) ?>s</strong></div>
                <div><span>OPcache</span><strong><?= admin_escape((string) ($serverStats['php']['opcache'] ?? 'N/D')) ?></strong></div>
                <div><span>Essais actifs</span><strong><?= (int) ($serverStats['database']['active_trials'] ?? 0) ?></strong></div>
                <div><span>Etat API cPanel</span><strong><?= admin_escape((string) ($serverStats['cpanel']['message'] ?? 'N/D')) ?></strong></div>
            </div></section>
            <section class="admin-panel server-panel"><div class="admin-panel-heading"><div><h2>Ressources cPanel</h2><p>Espace disque, bande passante, bases et processus disponibles selon le compte.</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Ressource</th><th>Utilisation</th><th>Limite</th><th>%</th></tr></thead><tbody>
            <?php if (($serverStats['cpanel']['metrics'] ?? []) === []): ?><tr><td colspan="4" class="admin-empty"><?= admin_escape((string) ($serverStats['cpanel']['message'] ?? 'Aucune metrique cPanel disponible.')) ?></td></tr><?php endif; ?>
            <?php foreach (($serverStats['cpanel']['metrics'] ?? []) as $metric): ?><tr><td><strong><?= admin_escape((string) ($metric['label'] ?? 'Metric')) ?></strong><small><?= admin_escape((string) ($metric['id'] ?? '-')) ?></small></td><td><?= admin_escape((string) ($metric['value'] ?? 'N/D')) ?></td><td><?= admin_escape((string) ($metric['max'] ?? 'N/D')) ?></td><td><?= array_key_exists('percent', $metric) && $metric['percent'] !== null ? admin_escape(number_format((float) $metric['percent'], 1, ',', ' ') . ' %') : '-' ?></td></tr><?php endforeach; ?>
            </tbody></table></div></section>
        </div>
        <?php endif; ?>

        <?php if ($page === 'audit'): ?>
        <section class="admin-panel" id="audit"><div class="admin-panel-heading"><div><h2>Journal admin</h2><p>40 dernieres actions</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Action</th><th>Cible</th></tr></thead><tbody><?php foreach ($auditLogs as $log): ?><tr><td><?= admin_escape($log['created_at']) ?></td><td><?= admin_escape($log['action']) ?><small><?= admin_escape($log['admin_username']) ?></small></td><td><?= admin_escape(trim((string) $log['target_type'] . ' ' . (string) $log['target_id'])) ?></td></tr><?php endforeach; ?></tbody></table></div></section>
        <?php endif; ?>
    </main>
</div>
<script src="/assets/admin.js?v=3" defer></script>
</body></html><?php
}

function admin_kpi(string $label, mixed $value, string $tone): string
{
    return '<div class="admin-kpi ' . admin_escape($tone) . '"><span>' . admin_escape($label) . '</span><strong>' . admin_escape((string) $value) . '</strong></div>';
}

function admin_render_pagination(array $pagination, string $page, string $query): string
{
    $totalPages = (int) ($pagination['pages'] ?? 1);
    if ($totalPages <= 1) {
        return '';
    }
    $current = (int) ($pagination['page'] ?? 1);
    $param = (string) ($pagination['param'] ?? 'list_page');
    $html = '<nav class="admin-pagination" aria-label="Pagination"><span>Page ' . $current . ' / ' . $totalPages . '</span>';
    for ($index = 1; $index <= $totalPages; $index++) {
        if ($index > 1 && $index < $totalPages && abs($index - $current) > 2) {
            if ($index === 2 || $index === $totalPages - 1) {
                $html .= '<span class="muted">...</span>';
            }
            continue;
        }
        $url = '/admin/?page=' . rawurlencode($page) . '&' . rawurlencode($param) . '=' . $index;
        if ($query !== '') {
            $url .= '&q=' . rawurlencode($query);
        }
        foreach (['sort', 'dir', 'order_status', 'customer_status', 'license_status', 'device_status', 'xtream_status', 'message_status'] as $keepParam) {
            $value = (string) ($_GET[$keepParam] ?? '');
            if ($value !== '') {
                $url .= '&' . rawurlencode($keepParam) . '=' . rawurlencode($value);
            }
        }
        $html .= '<a class="' . ($index === $current ? 'active' : '') . '" href="' . admin_escape($url) . '">' . $index . '</a>';
    }
    return $html . '</nav>';
}

function admin_render_device_row(array $device): void
{
    $deviceId = (string) $device['device_id'];
    $modalId = 'device-modal-' . preg_replace('/[^a-zA-Z0-9_-]/', '-', $deviceId);
    ?><tr class="device-row admin-click-row" tabindex="0" data-modal-target="<?= admin_escape($modalId) ?>">
        <td>
            <strong><?= admin_escape($device['public_device_code'] ?: '------') ?></strong>
            <small><?= admin_escape($device['device_name'] ?: 'Android TV') ?></small>
            <small class="mono"><?= admin_escape($deviceId) ?></small>
            <small><?= admin_escape($device['platform'] ?: 'android_tv') ?> <?= admin_escape($device['app_version'] ?: '') ?></small>
        </td>
        <td>
            <span class="admin-state <?= admin_escape($device['status']) ?>"><?= admin_escape($device['status']) ?></span>
            <small>Licence <?= admin_escape($device['license_status'] ?: '-') ?></small>
            <small>Essai <?= admin_escape($device['trial_status'] ?: '-') ?> - Pub <?= admin_escape($device['free_with_ads_status'] ?: '-') ?></small>
        </td>
        <td>
            <strong><?= admin_escape($device['activation_type'] ?: '-') ?></strong>
            <small>Code <?= admin_escape($device['activation_code'] ?: '-') ?></small>
            <small>Type <?= admin_escape($device['license_type'] ?: '-') ?> - <?= admin_escape((string) ($device['duration_days'] ?: '-')) ?> jours</small>
        </td>
        <td>
            <span class="admin-state <?= (int) $device['playlist_configured'] === 1 ? 'active' : 'pending' ?>"><?= (int) $device['playlist_configured'] === 1 ? 'Configuree' : 'Absente' ?></span>
            <small>Xtream <?= admin_escape($device['xtream_status'] ?: '-') ?></small>
        </td>
        <td>
            <?= admin_escape($device['first_seen_at'] ?: $device['created_at'] ?: '-') ?>
            <small>Active le <?= admin_escape($device['activated_at'] ?: '-') ?></small>
            <small>Pays <?= admin_escape($device['country_code'] ?: 'N/D') ?> - UA <?= admin_escape($device['last_user_agent'] ? 'present' : '-') ?></small>
        </td>
        <td>
            <?= admin_escape($device['expires_at'] ?: '-') ?>
            <small>Derniere activite <?= admin_escape($device['last_seen_at'] ?: $device['first_seen_at'] ?: '-') ?></small>
            <small>IP <?= admin_escape($device['last_ip_hash'] ? substr((string) $device['last_ip_hash'], 0, 8) : '-') ?></small>
        </td>
        <td class="admin-actions admin-actions-grid">
            <form method="post">
                <input type="hidden" name="redirect_page" value="devices">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <input type="hidden" name="action" value="set_device_status">
                <input type="hidden" name="device_id" value="<?= admin_escape($deviceId) ?>">
                <input type="hidden" name="status" value="<?= $device['status'] === 'blocked' ? 'active' : 'blocked' ?>">
                <button class="admin-button compact secondary" type="submit"><?= $device['status'] === 'blocked' ? 'Debloquer' : 'Bloquer' ?></button>
            </form>
            <form method="post" class="extend-form">
                <input type="hidden" name="redirect_page" value="devices">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <input type="hidden" name="action" value="extend_activation">
                <input type="hidden" name="device_id" value="<?= admin_escape($deviceId) ?>">
                <input name="days" type="number" min="1" max="3650" value="30" aria-label="Jours a ajouter">
                <button class="admin-button compact primary" type="submit">Prolonger</button>
            </form>
            <form method="post" data-confirm="Expirer cet appareil immediatement ?">
                <input type="hidden" name="redirect_page" value="devices">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <input type="hidden" name="action" value="expire_device">
                <input type="hidden" name="device_id" value="<?= admin_escape($deviceId) ?>">
                <button class="admin-button compact danger" type="submit">Expirer</button>
            </form>
            <form method="post" data-confirm="Supprimer la configuration Xtream de cet appareil ?">
                <input type="hidden" name="redirect_page" value="devices">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <input type="hidden" name="action" value="clear_device_playlist">
                <input type="hidden" name="device_id" value="<?= admin_escape($deviceId) ?>">
                <button class="admin-button compact secondary" type="submit">Reset Xtream</button>
            </form>
        </td>
    </tr><?php
}

function admin_render_customer_modal(array $user): void
{
    $modalId = 'customer-modal-' . (int) $user['id'];
    $details = is_array($user['details'] ?? null) ? $user['details'] : ['orders' => [], 'devices' => []];
    ?><div class="admin-modal" id="<?= admin_escape($modalId) ?>" role="dialog" aria-modal="true" aria-labelledby="<?= admin_escape($modalId) ?>-title" hidden>
        <div class="admin-modal-backdrop" data-modal-close></div>
        <section class="admin-modal-card">
            <header class="admin-modal-header">
                <div><h2 id="<?= admin_escape($modalId) ?>-title"><?= admin_escape($user['display_name'] ?: $user['email']) ?></h2><p><?= admin_escape($user['email']) ?></p></div>
                <button type="button" class="admin-modal-close" data-modal-close>Fermer</button>
            </header>
            <div class="admin-detail-grid">
                <div><span>Statut</span><strong><span class="admin-state <?= admin_escape($user['status']) ?>"><?= admin_escape($user['status']) ?></span></strong></div>
                <div><span>Commandes</span><strong><?= (int) $user['orders_count'] ?></strong></div>
                <div><span>Depense</span><strong><?= commerce_money((int) $user['total_spent']) ?></strong></div>
                <div><span>Derniere connexion</span><strong><?= admin_escape($user['last_login_at'] ?: '-') ?></strong></div>
            </div>
            <div class="admin-modal-columns">
                <section><h3>Commandes et licences</h3><?php if (($details['orders'] ?? []) === []): ?><p class="muted">Aucune commande.</p><?php endif; ?><ul class="admin-detail-list"><?php foreach (($details['orders'] ?? []) as $order): ?><li><strong><?= admin_escape($order['order_reference'] ?: 'SV-' . $order['id']) ?> - <?= admin_escape($order['plan_label']) ?></strong><span><?= commerce_money((int) $order['amount_cents'], (string) $order['currency']) ?> - <?= admin_escape($order['status']) ?> - code <?= admin_escape($order['code_hint'] ?: '-') ?></span><small>Usage <?= (int) ($order['used_devices'] ?? 0) ?> / <?= (int) ($order['max_devices'] ?? 1) ?> - appareil <?= admin_escape($order['assigned_public_device_code'] ?: '-') ?></small></li><?php endforeach; ?></ul></section>
                <section><h3>Appareils lies</h3><?php if (($details['devices'] ?? []) === []): ?><p class="muted">Aucun appareil lie.</p><?php endif; ?><ul class="admin-detail-list"><?php foreach (($details['devices'] ?? []) as $device): ?><li><strong><?= admin_escape($device['public_device_code'] ?: '------') ?> - <?= admin_escape($device['device_name'] ?: 'Android TV') ?></strong><span><?= admin_escape($device['status']) ?> / licence <?= admin_escape($device['license_status'] ?: '-') ?> / Xtream <?= admin_escape($device['xtream_status'] ?: '-') ?></span><small>Derniere activite <?= admin_escape($device['last_seen_at'] ?: '-') ?> - expire <?= admin_escape($device['expires_at'] ?: '-') ?></small></li><?php endforeach; ?></ul></section>
            </div>
        </section>
    </div><?php
}

function admin_render_device_modal(array $device): void
{
    $deviceId = (string) $device['device_id'];
    $modalId = 'device-modal-' . preg_replace('/[^a-zA-Z0-9_-]/', '-', $deviceId);
    ?><div class="admin-modal" id="<?= admin_escape($modalId) ?>" role="dialog" aria-modal="true" aria-labelledby="<?= admin_escape($modalId) ?>-title" hidden>
        <div class="admin-modal-backdrop" data-modal-close></div>
        <section class="admin-modal-card">
            <header class="admin-modal-header">
                <div><h2 id="<?= admin_escape($modalId) ?>-title"><?= admin_escape($device['public_device_code'] ?: '------') ?> - <?= admin_escape($device['device_name'] ?: 'Android TV') ?></h2><p class="mono"><?= admin_escape($deviceId) ?></p></div>
                <button type="button" class="admin-modal-close" data-modal-close>Fermer</button>
            </header>
            <div class="admin-detail-grid">
                <div><span>Etat</span><strong><span class="admin-state <?= admin_escape($device['status']) ?>"><?= admin_escape($device['status']) ?></span></strong></div>
                <div><span>Licence</span><strong><?= admin_escape($device['license_status'] ?: '-') ?></strong></div>
                <div><span>Xtream</span><strong><?= admin_escape($device['xtream_status'] ?: '-') ?></strong></div>
                <div><span>App version</span><strong><?= admin_escape($device['app_version'] ?: '-') ?></strong></div>
                <div><span>Premiere vue</span><strong><?= admin_escape($device['first_seen_at'] ?: $device['created_at'] ?: '-') ?></strong></div>
                <div><span>Derniere activite</span><strong><?= admin_escape($device['last_seen_at'] ?: '-') ?></strong></div>
                <div><span>Expiration</span><strong><?= admin_escape($device['expires_at'] ?: '-') ?></strong></div>
                <div><span>Pays / UA</span><strong><?= admin_escape($device['country_code'] ?: 'N/D') ?> / <?= admin_escape($device['last_user_agent'] ? 'present' : '-') ?></strong></div>
            </div>
            <div class="admin-modal-columns">
                <section><h3>Activation actuelle</h3><ul class="admin-detail-list"><li><strong><?= admin_escape($device['activation_type'] ?: '-') ?></strong><span>Code <?= admin_escape($device['activation_code'] ?: '-') ?> - type <?= admin_escape($device['license_type'] ?: '-') ?></span><small>Duree <?= admin_escape((string) ($device['duration_days'] ?: '-')) ?> jours - playlist <?= (int) $device['playlist_configured'] === 1 ? 'configuree' : 'absente' ?></small></li></ul></section>
                <section><h3>Historique</h3><?php if (($device['history'] ?? []) === []): ?><p class="muted">Aucun historique.</p><?php endif; ?><ul class="admin-detail-list"><?php foreach (($device['history'] ?? []) as $history): ?><li><strong><?= admin_escape($history['activation_type'] ?: '-') ?> - <?= admin_escape($history['status'] ?: '-') ?></strong><span>Code <?= admin_escape($history['code_hint'] ?: '-') ?> / <?= admin_escape($history['license_type'] ?: '-') ?></span><small><?= admin_escape($history['starts_at'] ?: $history['created_at'] ?: '-') ?> -> <?= admin_escape($history['expires_at'] ?: '-') ?></small></li><?php endforeach; ?></ul></section>
            </div>
        </section>
    </div><?php
}
