<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';
require_once dirname(__DIR__) . '/api/commerce.php';
require_once dirname(__DIR__) . '/api/mail_service.php';
require_once dirname(__DIR__) . '/api/ads_service.php';
require_once dirname(__DIR__) . '/api/anomaly_service.php';
$privateMediaService = dirname(__DIR__) . '/api/media/private/private_media_service.php';
if (is_file($privateMediaService)) {
    require_once $privateMediaService;
} else {
    error_log('SmartVision private media service missing.');
}
$behaviorService = dirname(__DIR__) . '/api/behavior_service.php';
if (is_file($behaviorService)) {
    require_once $behaviorService;
} else {
    error_log('SmartVision behavior service missing.');
}
$deviceDiagnosticsService = dirname(__DIR__) . '/api/device_diagnostics_service.php';
if (is_file($deviceDiagnosticsService)) {
    require_once $deviceDiagnosticsService;
} else {
    error_log('SmartVision device diagnostics service missing.');
}

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
ads_ensure_schema($pdo);
anomaly_ensure_schema($pdo);
if (function_exists('behavior_ensure_schema')) {
    behavior_ensure_schema($pdo);
}
if (function_exists('private_media_ensure_schema')) {
    private_media_ensure_schema($pdo);
}
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
    $redirectUrl = '/admin/?page=' . rawurlencode($page);
    if ($page === 'emails') {
        $emailTab = (string) ($_POST['email_tab'] ?? '');
        if (in_array($emailTab, ['overview', 'list', 'settings', 'templates'], true)) {
            $redirectUrl .= '&email_tab=' . rawurlencode($emailTab);
        }
        $templateId = filter_var($_POST['template_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if ($emailTab === 'templates' && $templateId !== false) {
            $redirectUrl .= '&template_id=' . rawurlencode((string) $templateId);
        }
    }
    header('Location: ' . $redirectUrl);
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
$gammalWebhookConfig = admin_load_gammal_webhook_config($pdo);
$gammalWebhookEvents = commerce_load_gammal_webhook_events($pdo);
$emailAdmin = admin_load_email_admin($pdo);
$adsPeriod = ads_period_days($_GET['ads_period'] ?? 1);
$adsAdmin = $page === 'ads' ? admin_load_ads_admin($pdo, $adsPeriod) : [];
$anomaliesAdmin = in_array($page, ['anomalies', 'diagnostics'], true) ? admin_load_anomalies_admin($pdo) : [];
$behaviorAdmin = $page === 'segments' ? admin_load_behavior_admin($pdo) : [];
$appConfigAdmin = $page === 'features' ? admin_load_app_config_admin($pdo) : [];
$privateMediaAdmin = $page === 'private_media' ? admin_load_private_media_admin($pdo) : [];
$diagnosticsAdmin = $page === 'diagnostics' ? admin_load_diagnostics_admin($pdo) : [];
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
    $gammalWebhookConfig,
    $gammalWebhookEvents,
    $emailAdmin,
    $adsAdmin,
    $anomaliesAdmin,
    $behaviorAdmin,
    $appConfigAdmin,
    $privateMediaAdmin,
    $diagnosticsAdmin,
    $flash,
    $generatedCode,
    $query,
    $page,
);

function admin_current_page(mixed $page): string
{
    $page = is_string($page) ? $page : 'overview';
    return in_array($page, ['overview', 'orders', 'customers', 'licenses', 'payments', 'emails', 'ads', 'segments', 'anomalies', 'features', 'private_media', 'devices', 'diagnostics', 'notifications', 'messages', 'slides', 'server', 'audit'], true)
        ? $page
        : 'overview';
}

function admin_page_number(mixed $page): int
{
    $value = filter_var($page, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 9999]]);

    return $value === false ? 1 : (int) $value;
}

function admin_normalized_license_type(array $row): string
{
    $activationType = (string) ($row['activation_type'] ?? '');
    $trialStatus = (string) ($row['trial_status'] ?? '');
    $freeAdsStatus = (string) ($row['free_with_ads_status'] ?? '');
    $codeLicenseType = (string) ($row['code_license_type'] ?? $row['license_type'] ?? '');
    if ($activationType === 'free_ads' || $freeAdsStatus === 'active' || $codeLicenseType === 'free') {
        return 'free_ads';
    }
    if (in_array($activationType, ['trial_demo', 'trial_pending_xtream'], true) || in_array($trialStatus, ['active', 'pending_xtream'], true) || $codeLicenseType === 'trial') {
        return 'trial_demo';
    }
    return 'premium';
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
        case 'save_gammal_webhook_settings': admin_save_gammal_webhook_settings($pdo); break;
        case 'save_ads_settings': admin_save_ads_settings($pdo); break;
        case 'reset_ads_settings': admin_reset_ads_settings($pdo); break;
        case 'test_ads_vast': admin_test_ads_vast($pdo); break;
        case 'approve_gammal_payment': admin_approve_gammal_payment($pdo); break;
        case 'reject_gammal_payment': admin_reject_gammal_payment($pdo); break;
        case 'save_email_settings': admin_save_email_settings($pdo); break;
        case 'save_email_template': admin_save_email_template($pdo); break;
        case 'send_test_email': admin_send_test_email($pdo); break;
        case 'send_admin_email': admin_send_admin_email($pdo); break;
        case 'save_app_config': admin_save_app_config($pdo); break;
        case 'save_feature_access': admin_save_feature_access($pdo); break;
        case 'save_app_consent': admin_save_app_consent($pdo); break;
        case 'save_trending_config': admin_save_trending_config($pdo); break;
        case 'save_private_media_config': admin_save_private_media_config($pdo); break;
        case 'private_media_sync_removed': admin_private_media_sync_removed($pdo); break;
        case 'private_media_clear_cache': admin_private_media_clear_cache($pdo); break;
        case 'send_notification': admin_send_notification($pdo); break;
        case 'set_notification_status': admin_set_notification_status($pdo); break;
        case 'delete_notification': admin_delete_notification($pdo); break;
        case 'purge_notification_history': admin_purge_notification_history($pdo); break;
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

function admin_save_gammal_webhook_settings(PDO $pdo): void
{
    $projectIds = trim((string) ($_POST['gammal_webhook_project_ids'] ?? ''));
    $normalizedIds = [];
    foreach (preg_split('/[,\s;]+/', $projectIds) ?: [] as $part) {
        if ($part === '') {
            continue;
        }
        if (!preg_match('/^\d+$/', $part)) {
            throw new InvalidArgumentException('Les IDs projet Gammal doivent etre numeriques.');
        }
        $normalizedIds[] = (string) (int) $part;
    }

    $settings = [
        'gammal_webhook_project_ids' => implode(',', array_values(array_unique($normalizedIds))),
        'gammal_webhook_auto_approve' => isset($_POST['gammal_webhook_auto_approve']) ? '1' : '0',
        'gammal_webhook_public_key_manual' => trim((string) ($_POST['gammal_webhook_public_key_manual'] ?? '')),
    ];
    if ($settings['gammal_webhook_auto_approve'] === '1' && $settings['gammal_webhook_project_ids'] === '') {
        throw new InvalidArgumentException('Ajoutez au moins un ID projet Gammal avant d activer l auto-validation.');
    }
    if ($settings['gammal_webhook_public_key_manual'] !== '' && !str_contains($settings['gammal_webhook_public_key_manual'], 'BEGIN PUBLIC KEY')) {
        throw new InvalidArgumentException('La cle publique Gammal doit etre au format PEM.');
    }

    $statement = $pdo->prepare(
        "INSERT INTO app_settings (setting_key, setting_value)
         VALUES (:setting_key, :setting_value)
         ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
    );
    foreach ($settings as $key => $value) {
        $statement->execute(['setting_key' => $key, 'setting_value' => $value]);
    }

    audit_admin_action($pdo, 'gammal_webhook_settings_updated', 'settings', 'gammal_webhook', $settings);
    set_admin_flash('success', 'Configuration webhook Gammal enregistree.');
}

function admin_load_gammal_webhook_config(PDO $pdo): array
{
    return [
        'url' => smartvision_public_base_url() . '/api/gammal-webhook',
        'project_ids' => trim((string) get_setting($pdo, 'gammal_webhook_project_ids', '')),
        'auto_approve' => (string) get_setting($pdo, 'gammal_webhook_auto_approve', '0') === '1',
        'public_key_manual' => trim((string) get_setting($pdo, 'gammal_webhook_public_key_manual', '')),
    ];
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
    $smtpPassword = (string) ($_POST['smtp_password'] ?? '');
    if ($smtpPassword !== '') {
        $settings['smtp_password_ciphertext'] = encrypt_private_value($smtpPassword);
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
    set_admin_flash('success', 'Configuration email enregistree.');
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

function admin_send_admin_email(PDO $pdo): void
{
    $recipient = strtolower(trim((string) ($_POST['recipient_email'] ?? '')));
    $recipientName = '';
    $userId = filter_var($_POST['recipient_user_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($recipient === '') {
        if ($userId !== false) {
            $statement = $pdo->prepare('SELECT email, display_name FROM site_users WHERE id = :id LIMIT 1');
            $statement->execute(['id' => (int) $userId]);
            $user = $statement->fetch();
            if (is_array($user)) {
                $recipient = strtolower(trim((string) $user['email']));
                $recipientName = (string) ($user['display_name'] ?? '');
            }
        }
    }
    if (!filter_var($recipient, FILTER_VALIDATE_EMAIL)) {
        throw new InvalidArgumentException('Destinataire email invalide.');
    }

    $templateKey = smartvision_text_substr(trim((string) ($_POST['template_key'] ?? '')), 0, 120);
    $statement = $pdo->prepare('SELECT template_key FROM email_templates WHERE template_key = :template_key AND is_active = 1 LIMIT 1');
    $statement->execute(['template_key' => $templateKey]);
    if ((string) $statement->fetchColumn() === '') {
        throw new InvalidArgumentException('Template email invalide ou inactif.');
    }
    if ($templateKey === 'verify_email') {
        if ($userId === false) {
            throw new InvalidArgumentException('Pour envoyer une confirmation email, choisissez un client existant afin de generer un lien valide.');
        }
        sv_create_email_verification($pdo, (int) $userId, $recipient, $recipientName);
        set_admin_flash('success', 'Email de confirmation envoye avec un lien valide.');
        return;
    }

    $subject = smartvision_text_substr(trim((string) ($_POST['subject'] ?? '')), 0, 255);
    $message = smartvision_text_substr(trim((string) ($_POST['message'] ?? '')), 0, 3000);
    $baseUrl = smartvision_public_base_url();
    $status = sv_send_email($pdo, $templateKey, $recipient, [
        'customer' => ['name' => sv_mail_customer_name($recipientName, $recipient), 'email' => $recipient],
        'user' => ['email' => $recipient],
        'order' => ['reference' => 'MANUAL', 'plan' => 'SmartVision', 'amount' => '-'],
        'payment' => ['txn' => 'MANUAL'],
        'event' => [
            'title' => $subject !== '' ? $subject : 'Message SmartVision',
            'message' => $message !== '' ? $message : 'Message envoye depuis le panneau administrateur.',
        ],
        'admin_event' => [
            'Destinataire' => $recipient,
            'Template' => $templateKey,
            'Operateur' => current_admin_username(),
            'Date' => gmdate('Y-m-d H:i:s') . ' UTC',
        ],
        'admin_url' => $baseUrl . '/admin/?page=emails',
        'account_url' => $baseUrl . '/account',
        'orders_url' => $baseUrl . '/account',
        'verify_url' => $baseUrl . '/verify-email/',
        'site_url' => $baseUrl,
        'site_name' => 'SmartVision',
    ]);

    set_admin_flash(
        $status === 'sent' ? 'success' : 'error',
        $status === 'sent'
            ? 'Email envoye.'
            : 'Email journalise avec le statut ' . $status . '. Verifiez la configuration SMTP.',
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
    $recipients = $pdo->query(
        "SELECT id, email, display_name
         FROM site_users
         WHERE email IS NOT NULL AND email <> ''
         ORDER BY id DESC
         LIMIT 200"
    )->fetchAll();
    return [
        'config' => $config,
        'password_configured' => $config['password'] !== '',
        'templates' => $templates,
        'logs' => $logs,
        'recipients' => $recipients,
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
    $pdo->exec('DELETE FROM device_playlist_profiles');
    $pdo->exec('DELETE FROM device_profile_registry');
    $pdo->exec('DELETE FROM playlist_lookup_attempts');
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
            (title, message, notification_type, target_scope, target_value, priority, status, created_by, expires_at, created_at, updated_at)
         VALUES
            (:title, :message, 'important_info', :target_scope, :target_value, :priority, 'active', :created_by, :expires_at, NOW(), NOW())"
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
    $pdo->beginTransaction();
    try {
        $pdo->prepare('DELETE FROM app_notification_receipts WHERE notification_id = :id')->execute(['id' => $notificationId]);
        $pdo->prepare('DELETE FROM app_notifications WHERE id = :id')->execute(['id' => $notificationId]);
        $pdo->commit();
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $exception;
    }
    audit_admin_action($pdo, 'notification_deleted', 'app_notification', (string) $notificationId);
    set_admin_flash('success', 'Notification supprimee.');
}

function admin_purge_notification_history(PDO $pdo): void
{
    ensure_app_notifications_table($pdo);
    if (strtoupper(trim((string) ($_POST['confirmation'] ?? ''))) !== 'PURGER') {
        throw new InvalidArgumentException('Saisissez PURGER pour confirmer.');
    }
    $statement = $pdo->prepare(
        'UPDATE app_notification_receipts SET purged_at = NOW() WHERE seen_at IS NOT NULL AND purged_at IS NULL'
    );
    $statement->execute();
    $purged = $statement->rowCount();
    audit_admin_action($pdo, 'notification_history_purged', 'app_notification_receipt', 'all', ['purged' => $purged]);
    set_admin_flash('success', $purged . ' historique(s) de notification purge(s).');
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

function admin_load_ads_admin(PDO $pdo, int $periodDays): array
{
    return ads_dashboard($pdo, $periodDays);
}

function admin_load_anomalies_admin(PDO $pdo): array
{
    anomaly_ensure_schema($pdo);
    $ignoredTypesSql = implode(',', array_map([$pdo, 'quote'], anomaly_ignored_types()));
    $visibleWhere = $ignoredTypesSql === '' ? '1=1' : "anomaly_type NOT IN ($ignoredTypesSql)";
    $events = $pdo->query(
        "SELECT id, device_id_hash, public_device_code, app_version, platform, route, anomaly_type, message, stack_trace, context_json, created_at
         FROM app_anomaly_events
         WHERE $visibleWhere
         ORDER BY id DESC
         LIMIT 120"
    )->fetchAll();
    $summaryRows = $pdo->query(
        "SELECT anomaly_type, COUNT(*) AS count, MAX(created_at) AS last_seen
         FROM app_anomaly_events
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
           AND $visibleWhere
         GROUP BY anomaly_type
         ORDER BY count DESC, anomaly_type ASC"
    )->fetchAll();
    $last24h = (int) $pdo->query(
        "SELECT COUNT(*) FROM app_anomaly_events WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY) AND $visibleWhere"
    )->fetchColumn();
    $crashes24h = (int) $pdo->query(
        "SELECT COUNT(*) FROM app_anomaly_events
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
           AND $visibleWhere
           AND anomaly_type IN (
                'UNCAUGHT_EXCEPTION',
                'CRASH',
                'PROCESS_EXIT_CRASH',
                'PROCESS_EXIT_CRASH_NATIVE',
                'PROCESS_EXIT_ANR'
           )"
    )->fetchColumn();
    $processExitCrashes24h = (int) $pdo->query(
        "SELECT COUNT(*) FROM app_anomaly_events
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
           AND $visibleWhere
           AND anomaly_type = 'PROCESS_EXIT_CRASH'"
    )->fetchColumn();

    return [
        'events' => $events,
        'summary' => $summaryRows,
        'last24h' => $last24h,
        'crashes24h' => $crashes24h,
        'processExitCrashes24h' => $processExitCrashes24h,
    ];
}

function admin_save_ads_settings(PDO $pdo): void
{
    $provider = (string) ($_POST['provider'] ?? 'HILLTOPADS_VAST');
    if (!in_array($provider, ['HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST'], true)) {
        throw new InvalidArgumentException('Provider pub invalide.');
    }

    $productionTag = admin_optional_url($_POST['vast_production_tag_url'] ?? '', 2000);
    $testTag = admin_optional_url($_POST['vast_test_tag_url'] ?? '', 2000);
    $adsEnabled = ads_bool($_POST['ads_enabled'] ?? '0');
    $useTestAds = ads_bool($_POST['use_test_ads'] ?? '0');
    if ($adsEnabled === 1 && $productionTag === '' && $useTestAds !== 1) {
        $useTestAds = 1;
    }

    $minMinutes = admin_int_range($_POST['min_minutes_between_ads'] ?? 30, 1, 1440);
    $maxPerDay = admin_int_range($_POST['max_ads_per_day'] ?? 3, 1, 500);
    $estimatedEcpm = admin_decimal_range($_POST['estimated_ecpm_eur'] ?? '5', 0, 1000);
    $hilltopSiteId = smartvision_text_substr(preg_replace('/[^A-Za-z0-9._:-]/', '', trim((string) ($_POST['hilltop_site_id'] ?? ''))) ?: '', 0, 64);
    $hilltopZoneId = smartvision_text_substr(preg_replace('/[^A-Za-z0-9._:-]/', '', trim((string) ($_POST['hilltop_zone_id'] ?? ''))) ?: '', 0, 64);

    $statement = $pdo->prepare(
        "UPDATE ads_settings
         SET ads_enabled = :ads_enabled,
             provider = :provider,
             use_test_ads = :use_test_ads,
             vast_production_tag_url = :vast_production_tag_url,
             vast_test_tag_url = :vast_test_tag_url,
             min_minutes_between_ads = :min_minutes_between_ads,
             max_ads_per_day = :max_ads_per_day,
             show_ad_before_live_stream = :show_ad_before_live_stream,
             show_ad_before_movie = :show_ad_before_movie,
             show_ad_before_series_episode = :show_ad_before_series_episode,
             allow_playback_if_ad_fails = :allow_playback_if_ad_fails,
             ads_only_inside_player = 1,
             estimated_ecpm_eur = :estimated_ecpm_eur,
             hilltop_site_id = :hilltop_site_id,
             hilltop_zone_id = :hilltop_zone_id,
             config_version = config_version + 1,
             updated_by = :updated_by,
             updated_at = NOW()
         WHERE id = 1"
    );
    $statement->execute([
        'ads_enabled' => $adsEnabled,
        'provider' => $provider,
        'use_test_ads' => $useTestAds,
        'vast_production_tag_url' => $productionTag,
        'vast_test_tag_url' => $testTag,
        'min_minutes_between_ads' => $minMinutes,
        'max_ads_per_day' => $maxPerDay,
        'show_ad_before_live_stream' => ads_bool($_POST['show_ad_before_live_stream'] ?? '0'),
        'show_ad_before_movie' => ads_bool($_POST['show_ad_before_movie'] ?? '0'),
        'show_ad_before_series_episode' => ads_bool($_POST['show_ad_before_series_episode'] ?? '0'),
        'allow_playback_if_ad_fails' => ads_bool($_POST['allow_playback_if_ad_fails'] ?? '0'),
        'estimated_ecpm_eur' => number_format($estimatedEcpm, 2, '.', ''),
        'hilltop_site_id' => $hilltopSiteId,
        'hilltop_zone_id' => $hilltopZoneId,
        'updated_by' => current_admin_username(),
    ]);
    audit_admin_action($pdo, 'ads_settings_updated', 'ads_settings', '1', [
        'provider' => $provider,
        'ads_enabled' => $adsEnabled,
        'use_test_ads' => $useTestAds,
    ]);
    set_admin_flash('success', 'Configuration publicitaire enregistree.');
}

function admin_reset_ads_settings(PDO $pdo): void
{
    $current = ads_load_settings($pdo);
    $defaults = ads_default_settings();
    $defaults['config_version'] = (int) ($current['config_version'] ?? 1) + 1;
    $defaults['updated_by'] = current_admin_username();
    $statement = $pdo->prepare(
        "UPDATE ads_settings
         SET ads_enabled = :ads_enabled,
             provider = :provider,
             use_test_ads = :use_test_ads,
             vast_production_tag_url = :vast_production_tag_url,
             vast_test_tag_url = :vast_test_tag_url,
             min_minutes_between_ads = :min_minutes_between_ads,
             max_ads_per_day = :max_ads_per_day,
             show_ad_before_live_stream = :show_ad_before_live_stream,
             show_ad_before_movie = :show_ad_before_movie,
             show_ad_before_series_episode = :show_ad_before_series_episode,
             allow_playback_if_ad_fails = :allow_playback_if_ad_fails,
             ads_only_inside_player = :ads_only_inside_player,
             estimated_ecpm_eur = :estimated_ecpm_eur,
             hilltop_site_id = :hilltop_site_id,
             hilltop_zone_id = :hilltop_zone_id,
             config_version = :config_version,
             updated_by = :updated_by,
             updated_at = NOW()
         WHERE id = 1"
    );
    $statement->execute($defaults);
    audit_admin_action($pdo, 'ads_settings_reset', 'ads_settings', '1');
    set_admin_flash('success', 'Configuration publicitaire reinitialisee.');
}

function admin_test_ads_vast(PDO $pdo): void
{
    $settings = ads_load_settings($pdo);
    $mode = (string) ($_POST['vast_mode'] ?? 'active');
    $url = match ($mode) {
        'production' => (string) ($settings['vast_production_tag_url'] ?? ''),
        'test' => (string) ($settings['vast_test_tag_url'] ?? ''),
        default => ads_selected_vast_tag($settings),
    };
    $result = ads_test_vast_tag($url);
    audit_admin_action($pdo, 'ads_vast_tested', 'ads_settings', '1', [
        'mode' => $mode,
        'ok' => (bool) $result['ok'],
        'http_code' => $result['http_code'],
    ]);
    $code = $result['http_code'] !== null ? ' HTTP ' . (int) $result['http_code'] . '.' : '';
    set_admin_flash($result['ok'] ? 'success' : 'error', $result['message'] . $code);
}

function admin_optional_url(mixed $value, int $maxLength): string
{
    $url = smartvision_text_substr(trim((string) $value), 0, $maxLength);
    if ($url === '') {
        return '';
    }
    if (!filter_var($url, FILTER_VALIDATE_URL)) {
        throw new InvalidArgumentException('URL invalide.');
    }

    return $url;
}

function admin_int_range(mixed $value, int $min, int $max): int
{
    $validated = filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => $min, 'max_range' => $max]]);
    if ($validated === false) {
        throw new InvalidArgumentException('Valeur numerique invalide.');
    }

    return (int) $validated;
}

function admin_decimal_range(mixed $value, float $min, float $max): float
{
    $number = filter_var(str_replace(',', '.', (string) $value), FILTER_VALIDATE_FLOAT);
    if ($number === false || $number < $min || $number > $max) {
        throw new InvalidArgumentException('Montant eCPM invalide.');
    }

    return (float) $number;
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
                   CASE WHEN p.device_id IS NULL THEN 0 ELSE 1 END AS playlist_configured,
                   COALESCE(ad_stats.ad_views, 0) AS ad_views,
                   ad_stats.last_ad_view_at
            FROM devices d
            LEFT JOIN device_activations a ON a.id = (
                SELECT da.id FROM device_activations da WHERE da.device_id = d.device_id ORDER BY da.id DESC LIMIT 1
            )
            LEFT JOIN activation_codes c ON c.id = a.activation_code_id
            LEFT JOIN activation_code_metadata m ON m.code_id = c.id
            LEFT JOIN device_playlist_configs p ON p.device_id = d.device_id
            LEFT JOIN (
                SELECT device_id_hash, COUNT(*) AS ad_views, MAX(created_at) AS last_ad_view_at
                FROM ads_events
                WHERE event_type = 'AD_STARTED'
                GROUP BY device_id_hash
            ) ad_stats ON ad_stats.device_id_hash = SHA2(d.device_id, 256)";
    $sql .= $where . ' ORDER BY ' . admin_sort_sql('activity', [
        'device' => 'd.public_device_code',
        'status' => 'd.status',
        'license' => 'd.license_status',
        'xtream' => 'd.xtream_status',
        'ad_views' => 'COALESCE(ad_stats.ad_views, 0)',
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
        $row['diagnostics'] = admin_load_device_diagnostics(
            $pdo,
            (string) $row['device_id'],
            (string) ($row['public_device_code'] ?? '')
        );
        $row['behavior'] = admin_load_device_behavior($pdo, (string) $row['device_id']);
    }
    unset($row);

    return [
        'rows' => $rows,
        'pagination' => admin_pagination($page, $perPage, $total, 'devices_page'),
    ];
}

function admin_load_device_behavior(PDO $pdo, string $deviceId): array
{
    if (!function_exists('behavior_admin_device_analysis') || $deviceId === '') {
        return [];
    }
    try {
        return behavior_admin_device_analysis($pdo, $deviceId);
    } catch (Throwable $exception) {
        error_log('SmartVision admin behavior device analysis failed.');
        return [];
    }
}

function admin_load_behavior_admin(PDO $pdo): array
{
    if (!function_exists('behavior_admin_dashboard')) {
        return [
            'available' => false,
            'message' => 'Service comportement non deploye.',
            'summary' => [],
            'segments' => [],
            'content' => [],
            'recent_events' => [],
        ];
    }
    try {
        return behavior_admin_dashboard($pdo);
    } catch (Throwable $exception) {
        error_log('SmartVision admin behavior dashboard failed.');
        return [
            'available' => false,
            'message' => 'Analyse comportementale indisponible.',
            'summary' => [],
            'segments' => [],
            'content' => [],
            'recent_events' => [],
        ];
    }
}

function admin_load_device_diagnostics(PDO $pdo, string $deviceId, string $publicDeviceCode = ''): array
{
    if (!function_exists('device_diagnostics_load_by_device')) {
        return [];
    }
    try {
        return device_diagnostics_load_by_device($pdo, $deviceId, $publicDeviceCode);
    } catch (Throwable $exception) {
        error_log('SmartVision admin device diagnostics lookup failed.');
        return [];
    }
}

function admin_load_diagnostics_admin(PDO $pdo): array
{
    if (!function_exists('device_diagnostics_ensure_schema')) {
        return [
            'available' => false,
            'message' => 'Service diagnostics non deploye.',
            'rows' => [],
        ];
    }

    try {
        device_diagnostics_ensure_schema($pdo);
        $statement = $pdo->query(
            "SELECT dd.device_id, dd.public_device_code, dd.diagnostic_type, dd.app_version,
                    dd.android_version, dd.device_model, dd.payload_json, dd.updated_at,
                    d.device_name, d.status AS device_status, d.license_status,
                    d.trial_status, d.free_with_ads_status, d.last_seen_at,
                    a.activation_type, c.license_type AS code_license_type
             FROM app_device_diagnostics dd
             LEFT JOIN devices d
                ON d.device_id = dd.device_id
                OR (dd.public_device_code IS NOT NULL AND dd.public_device_code <> '' AND d.public_device_code = dd.public_device_code)
             LEFT JOIN device_activations a ON a.id = (
                SELECT da.id FROM device_activations da WHERE da.device_id = d.device_id ORDER BY da.id DESC LIMIT 1
             )
             LEFT JOIN activation_codes c ON c.id = a.activation_code_id
             ORDER BY dd.updated_at DESC
             LIMIT 300"
        );
    } catch (Throwable $exception) {
        error_log('SmartVision admin diagnostics page failed.');
        return [
            'available' => false,
            'message' => 'Diagnostics indisponibles pour le moment.',
            'rows' => [],
        ];
    }

    $devices = [];
    foreach ($statement->fetchAll() as $row) {
        $deviceId = (string) ($row['device_id'] ?? '');
        $publicCode = (string) ($row['public_device_code'] ?? '');
        $key = $deviceId !== '' ? 'id:' . $deviceId : 'code:' . $publicCode;
        if ($key === 'code:') {
            continue;
        }
        if (!isset($devices[$key])) {
            $devices[$key] = [
                'device_id' => $deviceId,
                'public_device_code' => $publicCode,
                'device_name' => $row['device_name'] ?: $row['device_model'] ?: 'Android TV',
                'device_status' => $row['device_status'] ?? null,
                'license_status' => $row['license_status'] ?? null,
                'license_type' => admin_normalized_license_type($row),
                'last_seen_at' => $row['last_seen_at'] ?? null,
                'latest_at' => $row['updated_at'] ?? null,
                'diagnostics' => [],
            ];
        }
        $type = (string) ($row['diagnostic_type'] ?? '');
        if ($type === '' || isset($devices[$key]['diagnostics'][$type])) {
            continue;
        }
        $payload = json_decode((string) ($row['payload_json'] ?? '{}'), true);
        $devices[$key]['diagnostics'][$type] = [
            'app_version' => $row['app_version'] ?? null,
            'android_version' => $row['android_version'] ?? null,
            'device_model' => $row['device_model'] ?? null,
            'updated_at' => $row['updated_at'] ?? null,
            'payload' => is_array($payload) ? $payload : [],
        ];
    }

    return [
        'available' => true,
        'message' => '',
        'rows' => array_values($devices),
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
        "SELECT id, title, message, notification_type, target_scope, target_value, priority, status, created_by, expires_at, created_at, updated_at
         FROM app_notifications
         ORDER BY id DESC
         LIMIT 80"
    )->fetchAll();
}

function admin_load_app_config_admin(PDO $pdo): array
{
    $featuresJson = (string) get_setting($pdo, 'app_feature_access', '');
    $features = json_decode($featuresJson, true);
    if (!is_array($features)) {
        $features = admin_default_feature_access();
    }

    $variablesJson = (string) get_setting($pdo, 'app_consent_variables', '{}');
    $variables = json_decode($variablesJson, true);
    if (!is_array($variables)) {
        $variables = admin_default_consent_variables();
    }
    $trendingJson = (string) get_setting($pdo, 'app_trending_config', '');
    $trending = json_decode($trendingJson, true);
    if (!is_array($trending)) {
        $trending = admin_default_trending_config();
    }

    return [
        'consent_version' => (string) get_setting($pdo, 'app_consent_version', '2026-06-28'),
        'consent_title' => (string) get_setting($pdo, 'app_consent_title', 'Privacy Policy and Terms of Use'),
        'consent_body' => (string) get_setting($pdo, 'app_consent_body', admin_default_consent_body()),
        'consent_variables' => array_replace(admin_default_consent_variables(), $variables),
        'features' => admin_normalize_feature_access($features),
        'trending' => admin_normalize_trending_config($trending),
    ];
}

function admin_load_private_media_admin(PDO $pdo): array
{
    if (!function_exists('private_media_config')) {
        return ['available' => false, 'config' => [], 'health' => null, 'message' => 'Service bibliotheque privee indisponible.'];
    }

    $config = private_media_config($pdo);
    return [
        'available' => true,
        'config' => $config,
        'health' => private_media_health_snapshot($pdo),
    ];
}

function private_media_health_snapshot(PDO $pdo): ?array
{
    if (!function_exists('private_media_ensure_schema')) {
        return null;
    }
    private_media_ensure_schema($pdo);
    $statement = $pdo->prepare("SELECT provider, status, latency_ms, last_checked_at, last_error FROM private_media_provider_health WHERE provider = :provider LIMIT 1");
    $statement->execute(['provider' => 'eporner']);
    $row = $statement->fetch();
    return is_array($row) ? $row : null;
}

function admin_save_app_config(PDO $pdo): void
{
    admin_save_feature_access($pdo, false);
    admin_save_app_consent($pdo, false);
    admin_save_trending_config($pdo, false);
    audit_admin_action($pdo, 'app_config_updated', 'settings', 'features');
    set_admin_flash('success', 'Configuration application enregistree.');
}

function admin_save_feature_access(PDO $pdo, bool $withFlash = true): void
{
    $features = [];
    foreach (admin_default_feature_access() as $feature) {
        $key = (string) $feature['key'];
        $features[] = [
            'key' => $key,
            'label' => (string) $feature['label'],
            'premium' => isset($_POST['feature'][$key]['premium']),
            'trial' => isset($_POST['feature'][$key]['trial']),
            'free_ads' => isset($_POST['feature'][$key]['free_ads']),
        ];
    }

    admin_save_app_settings($pdo, [
        'app_feature_access' => json_encode($features, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ]);

    if ($withFlash) {
        audit_admin_action($pdo, 'feature_access_updated', 'settings', 'features');
        set_admin_flash('success', 'Disponibilite des fonctionnalites enregistree.');
    }
}

function admin_save_app_consent(PDO $pdo, bool $withFlash = true): void
{
    $variables = [];
    foreach (admin_default_consent_variables() as $key => $default) {
        $variables[$key] = smartvision_text_substr(trim((string) ($_POST['consent_variables'][$key] ?? $default)), 0, 255);
    }

    $settings = [
        'app_consent_version' => smartvision_text_substr(trim((string) ($_POST['consent_version'] ?? '')), 0, 40) ?: gmdate('Y-m-d'),
        'app_consent_title' => smartvision_text_substr(trim((string) ($_POST['consent_title'] ?? '')), 0, 120) ?: 'Privacy Policy and Terms of Use',
        'app_consent_body' => smartvision_text_substr(trim((string) ($_POST['consent_body'] ?? '')), 0, 30000) ?: admin_default_consent_body(),
        'app_consent_variables' => json_encode($variables, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ];

    admin_save_app_settings($pdo, $settings);

    if ($withFlash) {
        audit_admin_action($pdo, 'app_consent_updated', 'settings', 'features');
        set_admin_flash('success', 'Consentement TV enregistre.');
    }
}

function admin_save_trending_config(PDO $pdo, bool $withFlash = true): void
{
    $config = admin_normalize_trending_config([
        'require_landscape_image' => isset($_POST['trending']['require_landscape_image']),
        'exclude_adult' => isset($_POST['trending']['exclude_adult']),
        'use_rating_filter' => isset($_POST['trending']['use_rating_filter']),
        'minimum_rating' => $_POST['trending']['minimum_rating'] ?? null,
        'candidate_limit' => $_POST['trending']['candidate_limit'] ?? null,
        'section_limit' => $_POST['trending']['section_limit'] ?? null,
    ]);

    admin_save_app_settings($pdo, [
        'app_trending_config' => json_encode($config, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ]);

    if ($withFlash) {
        audit_admin_action($pdo, 'trending_config_updated', 'settings', 'features');
        set_admin_flash('success', 'Parametres des tendances enregistres.');
    }
}

function admin_save_private_media_config(PDO $pdo): void
{
    if (!function_exists('private_media_save_config')) {
        throw new RuntimeException('Service bibliotheque privee indisponible.');
    }

    $sectionTitles = $_POST['private_media']['section_title'] ?? [];
    $sectionQueries = $_POST['private_media']['section_query'] ?? [];
    $sectionOrders = $_POST['private_media']['section_order'] ?? [];
    $sectionIds = $_POST['private_media']['section_id'] ?? [];
    $sectionEnabled = $_POST['private_media']['section_enabled'] ?? [];
    $sectionDelete = $_POST['private_media']['section_delete'] ?? [];
    $sections = [];
    $usedIds = [];
    if (is_array($sectionTitles) && is_array($sectionQueries) && is_array($sectionOrders)) {
        foreach ($sectionTitles as $index => $title) {
            if (isset($sectionDelete[$index])) {
                continue;
            }
            $title = smartvision_text_substr(trim((string) $title), 0, 80);
            $query = smartvision_text_substr(trim((string) ($sectionQueries[$index] ?? '')), 0, 120);
            if ($title === '' || $query === '') {
                continue;
            }
            $id = is_array($sectionIds) ? private_media_slug((string) ($sectionIds[$index] ?? '')) : '';
            if ($id === '') {
                $id = private_media_slug($title);
            }
            if ($id === '') {
                $id = 'section';
            }
            $baseId = $id;
            $suffix = 2;
            while (isset($usedIds[$id])) {
                $id = $baseId . '-' . $suffix;
                $suffix++;
            }
            $usedIds[$id] = true;
            $sections[] = [
                'id' => $id,
                'title' => $title,
                'query' => $query,
                'order' => (string) ($sectionOrders[$index] ?? 'latest'),
                'enabled' => isset($sectionEnabled[$index]),
            ];
        }
    }

    private_media_save_config($pdo, [
        'enabled' => isset($_POST['private_media']['enabled']),
        'provider_eporner_enabled' => isset($_POST['private_media']['provider_eporner_enabled']),
        'show_in_app' => isset($_POST['private_media']['show_in_app']),
        'native_playback_enabled' => isset($_POST['private_media']['native_playback_enabled']),
        'force_native_playback_enabled' => isset($_POST['private_media']['force_native_playback_enabled']),
        'native_test_stream_url' => $_POST['private_media']['native_test_stream_url'] ?? '',
        'per_page' => $_POST['private_media']['per_page'] ?? 24,
        'thumbsize' => (string) ($_POST['private_media']['thumbsize'] ?? 'big'),
        'order' => (string) ($_POST['private_media']['order'] ?? 'latest'),
        'sections' => $sections,
    ]);

    audit_admin_action($pdo, 'private_media_config_updated', 'settings', 'private_media');
    set_admin_flash('success', 'Bibliotheque privee enregistree.');
}

function admin_private_media_sync_removed(PDO $pdo): void
{
    if (!function_exists('private_media_sync_removed')) {
        throw new RuntimeException('Service bibliotheque privee indisponible.');
    }
    try {
        $count = private_media_sync_removed($pdo);
        audit_admin_action($pdo, 'private_media_removed_synced', 'provider', 'eporner');
        set_admin_flash('success', $count . ' identifiant(s) retires synchronises. Synchronisation limitee par lot pour eviter les timeouts admin.');
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('SmartVision private media removed sync failed: ' . $exception->getMessage());
        set_admin_flash('error', 'Synchronisation removed indisponible pour le moment. Reessayez plus tard.');
    }
}

function admin_private_media_clear_cache(PDO $pdo): void
{
    if (!function_exists('private_media_clear_cache')) {
        throw new RuntimeException('Service bibliotheque privee indisponible.');
    }
    private_media_clear_cache($pdo);
    audit_admin_action($pdo, 'private_media_cache_cleared', 'provider', 'eporner');
    set_admin_flash('success', 'Cache bibliotheque privee vide.');
}

function admin_save_app_settings(PDO $pdo, array $settings): void
{
    $statement = $pdo->prepare(
        "INSERT INTO app_settings (setting_key, setting_value)
         VALUES (:setting_key, :setting_value)
         ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
    );
    foreach ($settings as $key => $value) {
        $statement->execute(['setting_key' => $key, 'setting_value' => $value]);
    }
}

function admin_default_trending_config(): array
{
    return [
        'require_landscape_image' => true,
        'exclude_adult' => true,
        'use_rating_filter' => false,
        'minimum_rating' => 9.0,
        'candidate_limit' => 50,
        'section_limit' => 10,
    ];
}

function admin_normalize_trending_config(array $config): array
{
    $defaults = admin_default_trending_config();
    $minimumRating = (float) ($config['minimum_rating'] ?? $defaults['minimum_rating']);
    $candidateLimit = (int) ($config['candidate_limit'] ?? $defaults['candidate_limit']);
    $sectionLimit = (int) ($config['section_limit'] ?? $defaults['section_limit']);

    return [
        'require_landscape_image' => (bool) ($config['require_landscape_image'] ?? $defaults['require_landscape_image']),
        'exclude_adult' => (bool) ($config['exclude_adult'] ?? $defaults['exclude_adult']),
        'use_rating_filter' => (bool) ($config['use_rating_filter'] ?? $defaults['use_rating_filter']),
        'minimum_rating' => max(0.0, min(10.0, $minimumRating)),
        'candidate_limit' => max(10, min(100, $candidateLimit)),
        'section_limit' => max(1, min(20, $sectionLimit)),
    ];
}

function admin_default_consent_variables(): array
{
    return [
        'app_name' => 'SmartVision Player',
        'company' => 'ONETECCOM',
        'site' => 'smartvisions.net',
        'support_email' => 'support@smartvisions.net',
    ];
}

function admin_default_feature_access(): array
{
    return [
        ['key' => 'youtube', 'label' => 'YouTube', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'parental_control', 'label' => 'Controle parental', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'replay', 'label' => 'Replay', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'advanced_favorites', 'label' => 'Favoris avances', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'multi_screen', 'label' => 'Multi-ecran', 'premium' => true, 'trial' => false, 'free_ads' => false],
        ['key' => 'local_cache', 'label' => 'Telechargement ou cache local', 'premium' => true, 'trial' => false, 'free_ads' => false],
        ['key' => 'recorder', 'label' => 'Recorder', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'media_center', 'label' => 'Menu Media Center', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'media_file_management', 'label' => 'Gestion fichiers Media', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'media_phone_transfer', 'label' => 'Transfert telephone TV', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'multi_profile', 'label' => 'Multi-profils', 'premium' => true, 'trial' => true, 'free_ads' => false],
        ['key' => 'private_media', 'label' => 'Bibliotheque privee', 'premium' => true, 'trial' => false, 'free_ads' => false],
        ['key' => 'private_media_eporner', 'label' => 'Provider Eporner', 'premium' => true, 'trial' => false, 'free_ads' => false],
        ['key' => 'private_media_native_playback', 'label' => 'Lecture native media prives', 'premium' => true, 'trial' => false, 'free_ads' => false],
    ];
}

function admin_normalize_feature_access(array $features): array
{
    $defaults = [];
    foreach (admin_default_feature_access() as $feature) {
        $defaults[(string) $feature['key']] = $feature;
    }
    foreach ($features as $feature) {
        if (!is_array($feature)) {
            continue;
        }
        $key = (string) ($feature['key'] ?? '');
        if ($key === '' || !isset($defaults[$key])) {
            continue;
        }
        $defaults[$key] = array_replace($defaults[$key], [
            'premium' => !empty($feature['premium']),
            'trial' => !empty($feature['trial']),
            'free_ads' => !empty($feature['free_ads']),
        ]);
    }

    return array_values($defaults);
}

function admin_default_consent_body(): string
{
    return "**SmartVision Player** is a commercial IPTV media player developed and operated by **ONETECCOM**.\n\n"
        . "**SmartVision Player does not provide IPTV content, TV channels, movies, series, IPTV subscriptions or playlists.**\n\n"
        . "Users are solely responsible for the content, links, playlists and Xtream credentials they add. "
        . "A SmartVision Player licence gives access only to the application or specific playback features. "
        . "For questions about licences, payments or support, contact **support@smartvisions.net**.";
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
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Administration SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=3"><link rel="stylesheet" href="/assets/admin-overrides.css?v=9"></head>
<body class="admin-login-body"><main class="admin-login-panel">
    <a class="admin-brand" href="/"><img class="admin-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player"></a>
    <h1>Administration</h1><p>Commandes, licences et appareils SmartVision.</p>
    <?php if (!$configured): ?><div class="admin-notice error">Administration non configuree sur le serveur.</div><?php else: ?>
    <?php if ($error): ?><div class="admin-notice error"><?= admin_escape($error) ?></div><?php endif; ?>
    <form method="post" action="/admin/" class="admin-stack-form"><input type="hidden" name="action" value="login"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label for="username">Identifiant</label><input id="username" name="username" autocomplete="username" required autofocus><label for="password">Mot de passe</label><input id="password" name="password" type="password" autocomplete="current-password" required><button class="admin-button primary" type="submit">Se connecter</button></form>
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
    array $gammalWebhookConfig,
    array $gammalWebhookEvents,
    array $emailAdmin,
    array $adsAdmin,
    array $anomaliesAdmin,
    array $behaviorAdmin,
    array $appConfigAdmin,
    array $privateMediaAdmin,
    array $diagnosticsAdmin,
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
        'ads' => ['Publicites', 'Gestion des publicites video du player SmartVision.'],
        'segments' => ['Segmentation', 'Analyse comportementale et segments utilisateurs pour ciblage publicitaire.'],
        'features' => ['Fonctionnalites', 'Droits par type de licence et consentement TV.'],
        'private_media' => ['Bibliotheque privee', 'Provider, sections, cache et monitoring des medias prives.'],
        'devices' => ['Appareils', 'TV associees, essais, licences et configuration Xtream.'],
        'diagnostics' => ['Diagnostics', 'Synthese, AutoSync, anomalies app, serveur et journal.'],
        'notifications' => ['Notifications', 'Messages envoyes vers toutes les TV ou vers des cibles precises.'],
        'messages' => ['Messages clients', 'Demandes envoyees depuis le formulaire de contact.'],
        'slides' => ['Slides Home', 'Emplacements publicitaires prives visibles sur la Home.'],
    ];
    $heading = $pages[$page] ?? $pages['overview'];
    ?><!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Administration | SmartVision</title><link rel="stylesheet" href="/assets/admin.css?v=3"><link rel="stylesheet" href="/assets/admin-overrides.css?v=9"></head>
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

        <?php if ($page === 'ads'): admin_render_ads_page($adsAdmin); endif; ?>
        <?php if ($page === 'segments'): admin_render_behavior_segments_page($behaviorAdmin); endif; ?>
        <?php if ($page === 'anomalies'): admin_render_anomalies_page($anomaliesAdmin); endif; ?>
        <?php if ($page === 'features'): admin_render_features_page($appConfigAdmin); endif; ?>
        <?php if ($page === 'private_media'): admin_render_private_media_page($privateMediaAdmin); endif; ?>
        <?php if ($page === 'diagnostics'): admin_render_diagnostics_page($diagnosticsAdmin, $anomaliesAdmin, $serverStats, $auditLogs, $stats); endif; ?>

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
        <section class="admin-panel" id="gammal-webhook-settings">
            <div class="admin-panel-heading">
                <div>
                    <h2>Webhook Gammal Tech</h2>
                    <p>URL a configurer dans Gammal Tech : <strong><?= admin_escape($gammalWebhookConfig['url']) ?></strong></p>
                </div>
            </div>
            <form method="post" class="payment-pack-form">
                <input type="hidden" name="redirect_page" value="payments">
                <input type="hidden" name="action" value="save_gammal_webhook_settings">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <div class="payment-pack-grid">
                    <article class="payment-pack-card">
                        <label>IDs projet Gammal autorises
                            <input name="gammal_webhook_project_ids" maxlength="190" placeholder="5 ou 5,7,12" value="<?= admin_escape($gammalWebhookConfig['project_ids']) ?>">
                        </label>
                        <p>Le webhook est capture meme sans ID configure, mais aucune licence n est creee automatiquement.</p>
                    </article>
                    <article class="payment-pack-card">
                        <label>Cle publique Gammal optionnelle
                            <textarea name="gammal_webhook_public_key_manual" rows="5" placeholder="-----BEGIN PUBLIC KEY-----"><?= admin_escape($gammalWebhookConfig['public_key_manual']) ?></textarea>
                        </label>
                        <p>Utilisee si l endpoint public de cle Gammal est bloque par le serveur.</p>
                    </article>
                    <article class="payment-pack-card">
                        <div class="payment-pack-heading">
                            <div><strong>Auto-validation</strong><span><?= $gammalWebhookConfig['auto_approve'] ? 'active' : 'inactive' ?></span></div>
                            <label class="payment-pack-toggle"><input type="checkbox" name="gammal_webhook_auto_approve" value="1"<?= $gammalWebhookConfig['auto_approve'] ? ' checked' : '' ?>> Actif</label>
                        </div>
                        <p>Une licence est creee automatiquement uniquement si le webhook signe correspond a un retour navigateur deja enregistre avec le meme txn.</p>
                    </article>
                </div>
                <div class="payment-pack-actions">
                    <p>Webhook recommande : <?= admin_escape($gammalWebhookConfig['url']) ?></p>
                    <button class="admin-button primary" type="submit">Enregistrer le webhook</button>
                </div>
            </form>
        </section>
        <section class="admin-panel" id="gammal-payment-reviews">
            <div class="admin-panel-heading">
                <div>
                    <h2>Retours Gammal Tech</h2>
                    <p>Les paiements restent approuvables manuellement; le webhook signe peut aussi les approuver automatiquement quand la correlation est sure.</p>
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
        <section class="admin-panel" id="gammal-webhook-events">
            <div class="admin-panel-heading">
                <div>
                    <h2>Journal webhooks Gammal</h2>
                    <p>Dernieres confirmations serveur signees recues depuis Gammal Tech.</p>
                </div>
            </div>
            <div class="admin-table-wrap"><table>
                <thead><tr><th>Transaction</th><th>Projet</th><th>Montant</th><th>Etat</th><th>Message</th><th>Date</th></tr></thead>
                <tbody>
                <?php if ($gammalWebhookEvents === []): ?><tr><td colspan="6" class="admin-empty">Aucun webhook Gammal capture.</td></tr><?php endif; ?>
                <?php foreach ($gammalWebhookEvents as $event): ?>
                <tr>
                    <td><strong><?= admin_escape($event['txn'] ?: '-') ?></strong><small><?= admin_escape($event['event_type']) ?></small></td>
                    <td><?= admin_escape((string) ($event['project_id'] ?? '-')) ?><small>payment #<?= admin_escape((string) ($event['payment_id'] ?? '-')) ?></small></td>
                    <td><?= $event['amount_cents'] === null ? '-' : commerce_money((int) $event['amount_cents'], (string) ($event['currency'] ?: 'EUR')) ?></td>
                    <td><span class="admin-state <?= admin_escape($event['processing_status']) ?>"><?= admin_escape($event['processing_status']) ?></span><small><?= admin_escape($event['verification_status']) ?></small></td>
                    <td><?= admin_escape($event['message'] ?: '-') ?></td>
                    <td><?= admin_escape($event['created_at']) ?><small>MAJ <?= admin_escape($event['updated_at'] ?: '-') ?></small></td>
                </tr>
                <?php endforeach; ?>
                </tbody>
            </table></div>
        </section>
        <?php endif; ?>

        <?php if ($page === 'emails'):
            $mailConfig = $emailAdmin['config'];
            $emailTab = (string) ($_GET['email_tab'] ?? 'overview');
            if (!in_array($emailTab, ['overview', 'list', 'settings', 'templates'], true)) {
                $emailTab = 'overview';
            }
            $emailTabs = ['overview' => 'Overview', 'list' => 'Liste des emails', 'settings' => 'Parametres', 'templates' => 'Templates'];
            $emailTypes = array_values(array_unique(array_filter(array_map(static fn(array $log): string => (string) $log['email_type'], $emailAdmin['logs']))));
            sort($emailTypes);
            $emailProviders = array_values(array_unique(array_filter(array_map(static fn(array $log): string => (string) $log['provider'], $emailAdmin['logs']))));
            sort($emailProviders);
            $emailStatuses = array_values(array_unique(array_filter(array_map(static fn(array $log): string => (string) $log['status'], $emailAdmin['logs']))));
            sort($emailStatuses);
            $emailCounts = [];
            foreach ($emailAdmin['logs'] as $log) {
                $type = (string) ($log['email_type'] ?: 'manual');
                $emailCounts[$type] ??= ['total' => 0, 'errors' => 0];
                $emailCounts[$type]['total']++;
                if ((string) $log['status'] === 'error') {
                    $emailCounts[$type]['errors']++;
                }
            }
            $recentErrors = array_values(array_filter($emailAdmin['logs'], static fn(array $log): bool => (string) $log['status'] === 'error' || (string) ($log['error_message'] ?? '') !== ''));
            $logSearch = strtolower(trim((string) ($_GET['email_q'] ?? '')));
            $logType = (string) ($_GET['email_type'] ?? '');
            $logStatus = (string) ($_GET['email_status'] ?? '');
            $logProvider = (string) ($_GET['email_provider'] ?? '');
            $logPeriod = (string) ($_GET['email_period'] ?? '');
            $filteredLogs = array_values(array_filter($emailAdmin['logs'], static function (array $log) use ($logSearch, $logType, $logStatus, $logProvider, $logPeriod): bool {
                if ($logType !== '' && (string) $log['email_type'] !== $logType) { return false; }
                if ($logStatus !== '' && (string) $log['status'] !== $logStatus) { return false; }
                if ($logProvider !== '' && (string) $log['provider'] !== $logProvider) { return false; }
                if ($logSearch !== '') {
                    $haystack = strtolower(implode(' ', [(string) $log['recipient_email'], (string) $log['subject'], (string) $log['email_type'], (string) $log['template_key']]));
                    if (!str_contains($haystack, $logSearch)) { return false; }
                }
                if (in_array($logPeriod, ['1', '7', '30'], true)) {
                    $createdAt = strtotime((string) $log['created_at']);
                    if ($createdAt === false || $createdAt < time() - ((int) $logPeriod * 86400)) { return false; }
                }
                return true;
            }));
            $templateSearch = strtolower(trim((string) ($_GET['template_q'] ?? '')));
            $templateCategory = (string) ($_GET['template_category'] ?? '');
            $templateStatus = (string) ($_GET['template_status'] ?? '');
            $templateType = (string) ($_GET['template_type'] ?? '');
            $templateCategories = array_values(array_unique(array_filter(array_map(static fn(array $template): string => (string) $template['category'], $emailAdmin['templates']))));
            sort($templateCategories);
            $templateTypes = array_values(array_unique(array_filter(array_map(static fn(array $template): string => !empty($template['is_system']) ? 'system' : 'custom', $emailAdmin['templates']))));
            sort($templateTypes);
            $visibleTemplates = array_values(array_filter($emailAdmin['templates'], static function (array $template) use ($templateSearch, $templateCategory, $templateStatus, $templateType): bool {
                if ($templateCategory !== '' && (string) $template['category'] !== $templateCategory) { return false; }
                if ($templateType !== '' && (!empty($template['is_system']) ? 'system' : 'custom') !== $templateType) { return false; }
                if ($templateStatus === 'active' && (int) $template['is_active'] !== 1) { return false; }
                if ($templateStatus === 'inactive' && (int) $template['is_active'] === 1) { return false; }
                if ($templateSearch !== '') {
                    $haystack = strtolower(implode(' ', [(string) $template['name'], (string) $template['template_key'], (string) $template['category']]));
                    if (!str_contains($haystack, $templateSearch)) { return false; }
                }
                return true;
            }));
            $selectedTemplateId = filter_var($_GET['template_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
            $selectedTemplate = $visibleTemplates[0] ?? ($emailAdmin['templates'][0] ?? null);
            foreach ($emailAdmin['templates'] as $template) {
                if ($selectedTemplateId !== false && (int) $template['id'] === (int) $selectedTemplateId) {
                    $selectedTemplate = $template;
                    break;
                }
            }
        ?>
        <section class="admin-panel email-admin-panel">
            <nav class="email-tabs" aria-label="Sections email">
                <?php foreach ($emailTabs as $tabKey => $tabLabel): ?>
                    <a class="<?= $emailTab === $tabKey ? 'active' : '' ?>" href="/admin/?page=emails&amp;email_tab=<?= admin_escape($tabKey) ?>"><?= admin_escape($tabLabel) ?></a>
                <?php endforeach; ?>
            </nav>
            <?php if ($emailTab === 'overview'): ?>
            <div class="email-tab-body">
                <div class="email-kpi-grid">
                    <?php foreach ($emailCounts as $type => $count): ?>
                    <article class="email-kpi-card"><span><?= admin_escape($type) ?></span><strong><?= (int) $count['total'] ?></strong><small><?= (int) $count['total'] ?> envoyes / <?= (int) $count['errors'] ?> erreurs</small></article>
                    <?php endforeach; ?>
                    <?php if ($emailCounts === []): ?><p class="admin-empty">Aucun email journalise.</p><?php endif; ?>
                </div>
                <div class="email-overview-grid">
                    <section class="email-card">
                        <h2>Envoyer un email</h2>
                        <form method="post" class="email-form">
                            <input type="hidden" name="redirect_page" value="emails"><input type="hidden" name="email_tab" value="overview"><input type="hidden" name="action" value="send_admin_email"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                            <label>Client existant<select name="recipient_user_id"><option value="">Choisir ou saisir un email ci-dessous</option><?php foreach ($emailAdmin['recipients'] as $recipient): ?><option value="<?= (int) $recipient['id'] ?>"><?= admin_escape(($recipient['display_name'] ?: $recipient['email']) . ' - ' . $recipient['email']) ?></option><?php endforeach; ?></select></label>
                            <label>Ou email destinataire<input name="recipient_email" type="email"></label>
                            <label>Template<select name="template_key" required><?php foreach ($emailAdmin['templates'] as $template): if ((int) $template['is_active'] !== 1) { continue; } ?><option value="<?= admin_escape($template['template_key']) ?>"><?= admin_escape($template['name'] . ' - ' . $template['template_key']) ?></option><?php endforeach; ?></select></label>
                            <label>Sujet optionnel<input name="subject" maxlength="255" placeholder="Utilise par les templates generiques"></label>
                            <label>Message optionnel<textarea name="message" placeholder="Utilise par les templates generiques"></textarea></label>
                            <button class="admin-button primary" type="submit">Envoyer</button>
                        </form>
                    </section>
                    <section class="email-card">
                        <h2>Erreurs recentes</h2>
                        <div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Email</th><th>Erreur</th></tr></thead><tbody>
                        <?php if ($recentErrors === []): ?><tr><td colspan="4" class="admin-empty">Aucune erreur recente.</td></tr><?php endif; ?>
                        <?php foreach (array_slice($recentErrors, 0, 8) as $log): ?><tr><td><?= admin_escape($log['created_at']) ?></td><td><?= admin_escape($log['email_type']) ?></td><td><?= admin_escape($log['recipient_email'] ?: '-') ?></td><td><small><?= admin_escape($log['error_message'] ?: '-') ?></small></td></tr><?php endforeach; ?>
                        </tbody></table></div>
                    </section>
                </div>
            </div>
            <?php endif; ?>
            <?php if ($emailTab === 'list'): ?>
            <div class="email-tab-body">
                <form method="get" class="email-filter-grid"><input type="hidden" name="page" value="emails"><input type="hidden" name="email_tab" value="list">
                    <label>Rechercher un email<input name="email_q" value="<?= admin_escape((string) ($_GET['email_q'] ?? '')) ?>" placeholder="Rechercher un email"></label>
                    <label>Type<select name="email_type"><option value="">Tous</option><?php foreach ($emailTypes as $type): ?><option value="<?= admin_escape($type) ?>"<?= $logType === $type ? ' selected' : '' ?>><?= admin_escape($type) ?></option><?php endforeach; ?></select></label>
                    <label>Statut<select name="email_status"><option value="">Tous</option><?php foreach ($emailStatuses as $status): ?><option value="<?= admin_escape($status) ?>"<?= $logStatus === $status ? ' selected' : '' ?>><?= admin_escape($status) ?></option><?php endforeach; ?></select></label>
                    <label>Provider<select name="email_provider"><option value="">Tous</option><?php foreach ($emailProviders as $provider): ?><option value="<?= admin_escape($provider) ?>"<?= $logProvider === $provider ? ' selected' : '' ?>><?= admin_escape($provider) ?></option><?php endforeach; ?></select></label>
                    <label>Periode<select name="email_period"><option value="">Tous</option><option value="1"<?= $logPeriod === '1' ? ' selected' : '' ?>>24h</option><option value="7"<?= $logPeriod === '7' ? ' selected' : '' ?>>7 jours</option><option value="30"<?= $logPeriod === '30' ? ' selected' : '' ?>>30 jours</option></select></label>
                    <button class="admin-button secondary" type="submit">Filtrer</button>
                </form>
                <div class="admin-table-wrap email-table-card"><table><thead><tr><th>Date</th><th>Type</th><th>Destinataire</th><th>Sujet</th><th>Statut</th><th>Provider</th><th>Erreur</th></tr></thead><tbody>
                <?php if ($filteredLogs === []): ?><tr><td colspan="7" class="admin-empty">Aucun email trouve.</td></tr><?php endif; ?>
                <?php foreach ($filteredLogs as $log): ?><tr><td><?= admin_escape($log['created_at']) ?></td><td><strong><?= admin_escape($log['email_type']) ?></strong><small><?= admin_escape($log['template_key']) ?></small></td><td><?= admin_escape($log['recipient_email'] ?: '-') ?></td><td><?= admin_escape($log['subject'] ?: '-') ?></td><td><span class="admin-state <?= admin_escape($log['status']) ?>"><?= admin_escape($log['status']) ?></span></td><td><?= admin_escape($log['provider'] ?: '-') ?></td><td><small><?= admin_escape($log['error_message'] ?: '-') ?></small></td></tr><?php endforeach; ?>
                </tbody></table></div>
            </div>
            <?php endif; ?>
            <?php if ($emailTab === 'settings'): ?>
            <div class="email-tab-body">
                <div class="email-status-grid"><article><span class="admin-state <?= !empty($mailConfig['smtp_enabled']) ? 'active' : 'disabled' ?>"><?= !empty($mailConfig['smtp_enabled']) ? 'SMTP pret' : 'SMTP off' ?></span><strong>cPanel / SMTP HostCreed</strong></article><article><span class="admin-state <?= !empty($mailConfig['external_enabled']) ? 'active' : 'disabled' ?>"><?= !empty($mailConfig['external_enabled']) ? 'EmailJS pret' : 'Services off' ?></span><strong>Gmail et Microsoft</strong></article><article><span class="admin-state <?= !empty($mailConfig['admin_email']) ? 'active' : 'disabled' ?>"><?= !empty($mailConfig['admin_email']) ? 'Admin configure' : 'Admin absent' ?></span><strong><?= admin_escape($mailConfig['admin_email'] ?: '-') ?></strong></article></div>
                <form method="post" class="email-settings-form"><input type="hidden" name="redirect_page" value="emails"><input type="hidden" name="email_tab" value="settings"><input type="hidden" name="action" value="save_email_settings"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                    <h2>cPanel / SMTP</h2>
                    <label>Activer SMTP<select name="smtp_enabled"><option value="0"<?= empty($mailConfig['smtp_enabled']) ? ' selected' : '' ?>>Non</option><option value="1"<?= !empty($mailConfig['smtp_enabled']) ? ' selected' : '' ?>>Oui</option></select></label>
                    <label>Serveur SMTP<input name="smtp_host" value="<?= admin_escape($mailConfig['host']) ?>" required></label><label>Port<input name="smtp_port" type="number" min="1" max="65535" value="<?= (int) $mailConfig['port'] ?>" required></label>
                    <label>Securite<select name="smtp_secure"><option value="ssl"<?= $mailConfig['secure'] === 'ssl' ? ' selected' : '' ?>>SSL 465</option><option value="tls"<?= $mailConfig['secure'] === 'tls' ? ' selected' : '' ?>>STARTTLS 587</option></select></label>
                    <label>Utilisateur SMTP<input name="smtp_user" value="<?= admin_escape($mailConfig['username']) ?>"></label><label>Mot de passe SMTP<input name="smtp_password" type="password" autocomplete="new-password" placeholder="<?= !empty($emailAdmin['password_configured']) ? 'Laisser vide pour conserver' : 'Mot de passe SMTP' ?>"></label>
                    <label>Email expediteur<input name="smtp_from_email" type="email" value="<?= admin_escape($mailConfig['from_email']) ?>"></label><label>Nom expediteur<input name="smtp_from_name" value="<?= admin_escape($mailConfig['from_name']) ?>"></label><label>Reply-To<input name="smtp_reply_to" type="email" value="<?= admin_escape($mailConfig['reply_to']) ?>"></label>
                    <label class="email-settings-wide">Notifications administrateur<input name="admin_notification_email" type="email" value="<?= admin_escape($mailConfig['admin_email']) ?>"></label>
                    <label>Services externes<select name="external_services_enabled"><option value="0"<?= empty($mailConfig['external_enabled']) ? ' selected' : '' ?>>Desactives</option><option value="1"<?= !empty($mailConfig['external_enabled']) ? ' selected' : '' ?>>Actives</option></select></label>
                    <div class="email-settings-actions"><button class="admin-button primary" type="submit">Enregistrer</button></div>
                </form>
            </div>
            <?php endif; ?>
            <?php if ($emailTab === 'templates'): ?>
            <div class="email-tab-body email-templates-layout">
                <div><form method="get" class="email-template-filters"><input type="hidden" name="page" value="emails"><input type="hidden" name="email_tab" value="templates">
                    <label>Rechercher un template<input name="template_q" value="<?= admin_escape((string) ($_GET['template_q'] ?? '')) ?>" placeholder="Rechercher un template"></label>
                    <label>Categorie<select name="template_category"><option value="">Tous</option><?php foreach ($templateCategories as $category): ?><option value="<?= admin_escape($category) ?>"<?= $templateCategory === $category ? ' selected' : '' ?>><?= admin_escape($category) ?></option><?php endforeach; ?></select></label>
                    <label>Statut<select name="template_status"><option value="">Tous</option><option value="active"<?= $templateStatus === 'active' ? ' selected' : '' ?>>Actif</option><option value="inactive"<?= $templateStatus === 'inactive' ? ' selected' : '' ?>>Inactif</option></select></label>
                    <label>Type<select name="template_type"><option value="">Tous</option><?php foreach ($templateTypes as $type): ?><option value="<?= admin_escape($type) ?>"<?= $templateType === $type ? ' selected' : '' ?>><?= admin_escape($type) ?></option><?php endforeach; ?></select></label><button class="admin-button primary" type="submit">Filtrer</button>
                </form><div class="admin-table-wrap email-table-card"><table><thead><tr><th>Template</th><th>Categorie</th><th>Statut</th><th>Type</th><th>Mise a jour</th><th>Actions</th></tr></thead><tbody>
                    <?php if ($visibleTemplates === []): ?><tr><td colspan="6" class="admin-empty">Aucun template.</td></tr><?php endif; ?>
                    <?php foreach ($visibleTemplates as $template): ?><tr class="<?= $selectedTemplate !== null && (int) $selectedTemplate['id'] === (int) $template['id'] ? 'selected-row' : '' ?>"><td><strong><?= admin_escape($template['name']) ?></strong><small><?= admin_escape($template['template_key']) ?></small></td><td><?= admin_escape($template['category']) ?></td><td><span class="admin-state <?= (int) $template['is_active'] === 1 ? 'active' : 'disabled' ?>"><?= (int) $template['is_active'] === 1 ? 'Actif' : 'Inactif' ?></span></td><td><?= !empty($template['is_system']) ? 'system' : 'custom' ?></td><td><?= admin_escape($template['updated_at'] ?: '-') ?></td><td><a class="admin-button compact secondary" href="/admin/?page=emails&amp;email_tab=templates&amp;template_id=<?= (int) $template['id'] ?>">Modifier</a></td></tr><?php endforeach; ?>
                </tbody></table></div></div>
                <?php if ($selectedTemplate !== null): ?><aside class="email-template-editor"><h2>Template: <?= admin_escape($selectedTemplate['name']) ?></h2><p><?= admin_escape($selectedTemplate['template_key']) ?></p><form method="post" class="email-form"><input type="hidden" name="redirect_page" value="emails"><input type="hidden" name="email_tab" value="templates"><input type="hidden" name="action" value="save_email_template"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="template_id" value="<?= (int) $selectedTemplate['id'] ?>">
                    <fieldset><legend>Identite</legend><label>Cle technique<input value="<?= admin_escape($selectedTemplate['template_key']) ?>" disabled></label><div class="email-three-fields"><label>Nom<input value="<?= admin_escape($selectedTemplate['name']) ?>" disabled></label><label>Categorie<input value="<?= admin_escape($selectedTemplate['category']) ?>" disabled></label><label>Ordre<input value="<?= (int) $selectedTemplate['sort_order'] ?>" disabled></label></div><label class="email-checkbox"><input name="is_active" type="checkbox" value="1"<?= (int) $selectedTemplate['is_active'] === 1 ? ' checked' : '' ?>> Actif</label></fieldset>
                    <fieldset><legend>Contenu</legend><label>Sujet<input name="subject_template" maxlength="255" value="<?= admin_escape($selectedTemplate['subject_template']) ?>" required></label><label>Titre<input name="title_template" maxlength="255" value="<?= admin_escape($selectedTemplate['title_template']) ?>" required></label><label>Introduction HTML<textarea name="intro_html"><?= admin_escape($selectedTemplate['intro_html']) ?></textarea></label><label>Corps HTML<textarea name="body_html"><?= admin_escape($selectedTemplate['body_html']) ?></textarea></label><label>Pied HTML<textarea name="footer_html"><?= admin_escape($selectedTemplate['footer_html']) ?></textarea></label></fieldset>
                    <button class="admin-button primary" type="submit">Enregistrer le template</button></form></aside><?php endif; ?>
            </div>
            <?php endif; ?>
        </section>
        <?php endif; ?>

        <?php if ($page === 'emails' && false):
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
        <section class="admin-panel" id="devices"><div class="admin-panel-heading"><div><h2>Appareils</h2><p><?= (int) $devicesPagination['total'] ?> appareil(s), tries par derniere activite decroissante.</p></div><form class="admin-filter-row" method="get"><input type="hidden" name="page" value="devices"><input type="hidden" name="q" value="<?= admin_escape($query) ?>"><label>Etat<select name="device_status"><option value="">Tous</option><?php foreach (['pending', 'active', 'expired', 'blocked'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['device_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><label>Xtream<select name="xtream_status"><option value="">Tous</option><?php foreach (['missing', 'configured', 'invalid'] as $statusOption): ?><option value="<?= admin_escape($statusOption) ?>"<?= ($_GET['xtream_status'] ?? '') === $statusOption ? ' selected' : '' ?>><?= admin_escape($statusOption) ?></option><?php endforeach; ?></select></label><button class="admin-button compact secondary" type="submit">Filtrer</button></form><form method="post" class="purge-form" data-confirm="Supprimer tous les appareils, sessions, activations et playlists ?"><input type="hidden" name="redirect_page" value="devices"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="purge_devices"><input name="confirmation" placeholder="PURGER" aria-label="Confirmation purge"><button class="admin-button danger" type="submit">Purger appareils</button></form></div><div class="admin-table-wrap"><table><thead><tr><th><?= admin_sort_link('devices', 'Appareil', 'device', $query) ?></th><th><?= admin_sort_link('devices', 'Etat', 'status', $query) ?></th><th><?= admin_sort_link('devices', 'Licence', 'license', $query) ?></th><th><?= admin_sort_link('devices', 'Xtream', 'xtream', $query) ?></th><th><?= admin_sort_link('devices', 'Ad view', 'ad_views', $query) ?></th><th><?= admin_sort_link('devices', 'Installation', 'installed', $query) ?></th><th><?= admin_sort_link('devices', 'Expiration / activite', 'activity', $query) ?></th><th>Actions</th></tr></thead><tbody>
        <?php if ($devices === []): ?><tr><td colspan="8" class="admin-empty">Aucun appareil.</td></tr><?php endif; ?>
        <?php foreach ($devices as $device): admin_render_device_row($device); endforeach; ?>
        </tbody></table></div><?= admin_render_pagination($devicesPagination, $page, $query) ?><?php foreach ($devices as $device): admin_render_device_modal($device); endforeach; ?></section>
        <?php endif; ?>

        <?php if ($page === 'notifications'): ?>
        <section class="admin-panel" id="notifications"><div class="admin-panel-heading"><div><h2>Envoyer une notification</h2><p>Pour les cibles multiples, separez les IDs, codes appareil, emails ou IDs client par virgule, espace ou point-virgule.</p></div></div><form method="post" class="notification-form"><input type="hidden" name="redirect_page" value="notifications"><input type="hidden" name="action" value="send_notification"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><label class="notification-title">Titre<input name="title" maxlength="120" required placeholder="Maintenance, information, promotion..."></label><label>Priorite<select name="priority"><option value="normal">Normale</option><option value="important">Importante</option><option value="urgent">Urgente</option></select></label><label>Ciblage<select name="target_scope" data-notification-scope><option value="all">Tous les utilisateurs</option><option value="devices">Appareil(s)</option><option value="users">Client(s)</option></select></label><label>Expiration<input name="expires_at" type="date"></label><label class="notification-targets">Cibles<input name="target_value" placeholder="device_id, code public, email ou id client"></label><label class="notification-message">Message<textarea name="message" maxlength="1200" required placeholder="Message visible dans l'application TV"></textarea></label><div class="notification-submit"><button class="admin-button primary" type="submit">Envoyer la notification</button></div></form></section>
        <section class="admin-panel" id="notifications-list"><div class="admin-panel-heading"><div><h2>Notifications recentes</h2><p><?= count($notifications) ?> notification(s)</p></div><form method="post" class="purge-form" data-confirm="Purger l historique consulte de toutes les TV ?"><input type="hidden" name="redirect_page" value="notifications"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><input type="hidden" name="action" value="purge_notification_history"><input name="confirmation" placeholder="PURGER" aria-label="Confirmation purge historique"><button class="admin-button danger" type="submit">Purger les historiques</button></form></div><div class="admin-table-wrap"><table><thead><tr><th>Message</th><th>Ciblage</th><th>Priorite</th><th>Statut</th><th>Dates</th><th>Actions</th></tr></thead><tbody>
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
<script src="/assets/admin.js?v=5" defer></script>
</body></html><?php
}

function admin_render_ads_page(array $adsAdmin): void
{
    $settings = $adsAdmin['settings'] ?? ads_default_settings();
    $summary = $adsAdmin['summary'] ?? [];
    $series = $adsAdmin['series'] ?? [];
    $events = $adsAdmin['recent_events'] ?? [];
    $provider = $adsAdmin['provider'] ?? [];
    $diagnostics = $adsAdmin['diagnostics'] ?? [];
    $hilltop = $adsAdmin['hilltop'] ?? [];
    $periodDays = (int) ($adsAdmin['period_days'] ?? 1);
    $estimatedRevenue = ((int) ($summary['shown'] ?? 0) / 1000) * (float) ($settings['estimated_ecpm_eur'] ?? 0);
    ?>
        <nav class="admin-tabs ads-tabs" aria-label="Filtres publicites">
            <?php foreach ([1 => "Aujourd'hui", 7 => '7 derniers jours', 30 => '30 derniers jours'] as $days => $label): ?>
                <a class="<?= $periodDays === $days ? 'active' : '' ?>" href="/admin/?page=ads&amp;ads_period=<?= (int) $days ?>"><?= admin_escape($label) ?></a>
            <?php endforeach; ?>
        </nav>

        <section class="admin-kpis ads-kpis" aria-label="Indicateurs publicites">
            <?= admin_kpi('Pubs demandees', (int) ($summary['requested'] ?? 0), 'blue') ?>
            <?= admin_kpi('Pubs affichees', (int) ($summary['shown'] ?? 0), 'cyan') ?>
            <?= admin_kpi('Pubs terminees', (int) ($summary['completed'] ?? 0), 'green') ?>
            <?= admin_kpi('Pubs echouees', (int) ($summary['failed'] ?? 0), 'orange') ?>
            <?= admin_kpi('Taux echec', number_format((float) ($summary['failure_rate'] ?? 0), 1, ',', ' ') . ' %', 'orange') ?>
            <?= admin_kpi('Taux completion', number_format((float) ($summary['completion_rate'] ?? 0), 1, ',', ' ') . ' %', 'green') ?>
            <?= admin_kpi('FREE_WITH_ADS actifs', (int) ($summary['free_devices'] ?? 0), 'cyan') ?>
            <?= admin_kpi('Config active', !empty($settings['ads_enabled']) ? 'Active' : 'Inactive', !empty($settings['ads_enabled']) ? 'green' : 'orange') ?>
        </section>

        <div class="ads-dashboard-grid">
            <section class="admin-panel ads-chart-panel">
                <div class="admin-panel-heading"><div><h2>Tableau de bord</h2><p>Volumes app Android TV sur la periode filtree.</p></div></div>
                <div class="ads-metric-list">
                    <div><span>Pubs bloquees par frequence</span><strong><?= (int) ($summary['blocked_frequency'] ?? 0) ?></strong></div>
                    <div><span>Pubs bloquees par limite journaliere</span><strong><?= (int) ($summary['blocked_daily_limit'] ?? 0) ?></strong></div>
                    <div><span>Derniere pub affichee</span><strong><?= admin_escape((string) ($summary['last_started_at'] ?? '-')) ?></strong></div>
                    <div><span>Derniere erreur pub</span><strong><?= admin_escape((string) (($summary['last_error']['created_at'] ?? null) ?: '-')) ?></strong><small><?= admin_escape((string) (($summary['last_error']['error_message'] ?? null) ?: '-')) ?></small></div>
                    <div><span>Revenu estime</span><strong><?= admin_escape(number_format($estimatedRevenue, 2, ',', ' ') . ' EUR') ?></strong><small>Indicatif uniquement, verifier HilltopAds.</small></div>
                </div>
                <div class="ads-series-table">
                    <table><thead><tr><th>Jour</th><th>Demandes</th><th>Impressions</th><th>Terminees</th><th>Erreurs</th><th>Completion</th></tr></thead><tbody>
                    <?php foreach ($series as $point): ?><tr><td><?= admin_escape($point['label']) ?></td><td><?= (int) $point['requested'] ?></td><td><?= (int) $point['shown'] ?></td><td><?= (int) $point['completed'] ?></td><td><?= (int) $point['failed'] ?></td><td><?= admin_escape(number_format((float) $point['completion_rate'], 1, ',', ' ')) ?> %</td></tr><?php endforeach; ?>
                    </tbody></table>
                </div>
            </section>

            <section class="admin-panel ads-provider-panel">
                <div class="admin-panel-heading"><div><h2>Provider / VAST</h2><p>Etat du fournisseur et infos HilltopAds.</p></div></div>
                <div class="ads-metric-list">
                    <div><span>Provider actuel</span><strong><?= admin_escape((string) ($provider['provider'] ?? '-')) ?></strong></div>
                    <div><span>Mode</span><strong><?= admin_escape((string) ($provider['mode'] ?? '-')) ?></strong></div>
                    <div><span>Statut</span><strong><?= admin_escape((string) ($provider['status'] ?? '-')) ?></strong></div>
                    <div><span>VAST utilise</span><strong><?= admin_escape(admin_mask_url((string) ($provider['vast_tag_url'] ?? ''))) ?></strong></div>
                    <div><span>Hilltop API</span><strong><?= !empty($hilltop['configured']) ? admin_escape((string) ($hilltop['message'] ?? 'Configuree')) : 'Non configuree' ?></strong><small>Publisher ID <?= !empty($hilltop['publisher_id_present']) ? 'present' : 'absent' ?></small></div>
                    <div><span>Balance Hilltop</span><strong><?= admin_escape(admin_hilltop_balance_label($hilltop['balance'] ?? [])) ?></strong></div>
                    <div><span>Inventaire Hilltop</span><strong><?= (int) (($hilltop['inventory']['site_count'] ?? 0)) ?> site(s), <?= (int) (($hilltop['inventory']['zone_count'] ?? 0)) ?> zone(s)</strong></div>
                    <div><span>Stats Hilltop periode</span><strong><?= admin_escape(number_format((float) (($hilltop['stats']['impressions'] ?? 0)), 0, ',', ' ')) ?> impressions</strong><small><?= admin_escape(number_format((float) (($hilltop['stats']['revenue'] ?? 0)), 4, ',', ' ')) ?> revenu API</small></div>
                </div>
                <form method="post" class="ads-vast-test-form">
                    <input type="hidden" name="redirect_page" value="ads">
                    <input type="hidden" name="action" value="test_ads_vast">
                    <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                    <label>Tag a tester<select name="vast_mode"><option value="active">Actif</option><option value="production">Production</option><option value="test">Test</option></select></label>
                    <button class="admin-button secondary" type="submit">Tester le tag VAST</button>
                </form>
            </section>
        </div>

        <section class="admin-panel ads-config-panel">
            <div class="admin-panel-heading"><div><h2>Configuration des pubs</h2><p>Ces reglages sont lus par l application via /api/app/ads-config.</p></div></div>
            <form method="post" class="ads-settings-form">
                <input type="hidden" name="redirect_page" value="ads">
                <input type="hidden" name="action" value="save_ads_settings">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <label><span>adsEnabled</span><input name="ads_enabled" type="checkbox" value="1"<?= !empty($settings['ads_enabled']) ? ' checked' : '' ?>> Activer les pubs</label>
                <label><span>useTestAds</span><input name="use_test_ads" type="checkbox" value="1"<?= !empty($settings['use_test_ads']) ? ' checked' : '' ?>> Utiliser le tag test</label>
                <label><span>Provider</span><select name="provider"><?php foreach (['HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST'] as $providerOption): ?><option value="<?= admin_escape($providerOption) ?>"<?= ($settings['provider'] ?? '') === $providerOption ? ' selected' : '' ?>><?= admin_escape($providerOption) ?></option><?php endforeach; ?></select></label>
                <label class="ads-wide"><span>vastProductionTagUrl</span><input name="vast_production_tag_url" type="url" maxlength="2000" value="<?= admin_escape((string) ($settings['vast_production_tag_url'] ?? '')) ?>"></label>
                <label class="ads-wide"><span>vastTestTagUrl</span><input name="vast_test_tag_url" type="url" maxlength="2000" value="<?= admin_escape((string) ($settings['vast_test_tag_url'] ?? '')) ?>"></label>
                <label><span>minMinutesBetweenAds</span><input name="min_minutes_between_ads" type="number" min="1" max="1440" value="<?= (int) ($settings['min_minutes_between_ads'] ?? 30) ?>"></label>
                <label><span>maxAdsPerDay</span><input name="max_ads_per_day" type="number" min="1" max="500" value="<?= (int) ($settings['max_ads_per_day'] ?? 3) ?>"></label>
                <label><span>estimatedEcpm EUR</span><input name="estimated_ecpm_eur" type="number" min="0" max="1000" step="0.01" value="<?= admin_escape((string) ($settings['estimated_ecpm_eur'] ?? '5.00')) ?>"></label>
                <label><span>Hilltop siteID</span><input name="hilltop_site_id" maxlength="64" value="<?= admin_escape((string) ($settings['hilltop_site_id'] ?? '')) ?>"></label>
                <label><span>Hilltop zoneID</span><input name="hilltop_zone_id" maxlength="64" value="<?= admin_escape((string) ($settings['hilltop_zone_id'] ?? '')) ?>"></label>
                <label><span>Live TV</span><input name="show_ad_before_live_stream" type="checkbox" value="1"<?= !empty($settings['show_ad_before_live_stream']) ? ' checked' : '' ?>> Pre-roll Live</label>
                <label><span>Films</span><input name="show_ad_before_movie" type="checkbox" value="1"<?= !empty($settings['show_ad_before_movie']) ? ' checked' : '' ?>> Pre-roll film</label>
                <label><span>Series</span><input name="show_ad_before_series_episode" type="checkbox" value="1"<?= !empty($settings['show_ad_before_series_episode']) ? ' checked' : '' ?>> Pre-roll episode</label>
                <label><span>Fallback video</span><input name="allow_playback_if_ad_fails" type="checkbox" value="1"<?= !empty($settings['allow_playback_if_ad_fails']) ? ' checked' : '' ?>> Lire si pub echoue</label>
                <label><span>adsOnlyInsidePlayer</span><input type="checkbox" checked disabled> Force a true</label>
                <div class="ads-config-meta"><strong>Version config <?= (int) ($settings['config_version'] ?? 1) ?></strong><small>MAJ <?= admin_escape((string) ($settings['updated_at'] ?? '-')) ?> par <?= admin_escape((string) ($settings['updated_by'] ?? '-')) ?></small></div>
                <div class="ads-settings-actions"><button class="admin-button primary" type="submit">Enregistrer configuration</button></div>
            </form>
            <form method="post" class="ads-reset-form" data-confirm="Reinitialiser la configuration publicitaire ?">
                <input type="hidden" name="redirect_page" value="ads">
                <input type="hidden" name="action" value="reset_ads_settings">
                <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
                <button class="admin-button secondary" type="submit">Reinitialiser configuration par defaut</button>
            </form>
        </section>

        <div class="ads-dashboard-grid">
            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Diagnostic</h2><p>Alertes simples pour eviter les erreurs de monétisation.</p></div></div>
                <ul class="ads-diagnostics">
                    <?php foreach ($diagnostics as $diagnostic): ?><li class="<?= admin_escape((string) ($diagnostic['level'] ?? 'info')) ?>"><?= admin_escape((string) ($diagnostic['message'] ?? '')) ?></li><?php endforeach; ?>
                </ul>
            </section>
            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Demandes par contexte</h2><p>Types de contenu et plateformes.</p></div></div>
                <div class="ads-breakdown-grid">
                    <table><thead><tr><th>Content type</th><th>Demandes</th></tr></thead><tbody><?php foreach (($summary['content_breakdown'] ?? []) as $row): ?><tr><td><?= admin_escape((string) $row['content_type']) ?></td><td><?= (int) $row['requests'] ?></td></tr><?php endforeach; ?></tbody></table>
                    <table><thead><tr><th>Platform</th><th>Demandes</th></tr></thead><tbody><?php foreach (($summary['platform_breakdown'] ?? []) as $row): ?><tr><td><?= admin_escape((string) $row['platform']) ?></td><td><?= (int) $row['requests'] ?></td></tr><?php endforeach; ?></tbody></table>
                </div>
            </section>
        </div>

        <section class="admin-panel" id="ads-events">
            <div class="admin-panel-heading"><div><h2>Evenements recents</h2><p>Derniers evenements pub remontes par l application.</p></div><a class="admin-button compact secondary" href="/admin/?page=ads&amp;ads_period=<?= (int) $periodDays ?>">Actualiser statistiques</a></div>
            <div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Device hash</th><th>User</th><th>Content</th><th>Platform</th><th>App</th><th>Event</th><th>Provider</th><th>Raison / erreur</th><th>Duree</th></tr></thead><tbody>
                <?php if ($events === []): ?><tr><td colspan="10" class="admin-empty">Aucun evenement pub enregistre.</td></tr><?php endif; ?>
                <?php foreach ($events as $event): ?><tr>
                    <td><?= admin_escape((string) $event['created_at']) ?></td>
                    <td><small><?= admin_escape(ads_mask_hash($event['device_id_hash'] ?? null)) ?></small></td>
                    <td><?= admin_escape((string) $event['user_status']) ?></td>
                    <td><?= admin_escape((string) $event['content_type']) ?></td>
                    <td><?= admin_escape((string) $event['platform']) ?></td>
                    <td><?= admin_escape((string) ($event['app_version'] ?: '-')) ?></td>
                    <td><strong><?= admin_escape((string) $event['event_type']) ?></strong></td>
                    <td><?= admin_escape((string) $event['provider']) ?></td>
                    <td><small><?= admin_escape(trim((string) ($event['reason'] ?: '') . ' ' . (string) ($event['error_code'] ?: '') . ' ' . (string) ($event['error_message'] ?: '')) ?: '-') ?></small></td>
                    <td><?= $event['ad_duration_seconds'] !== null ? (int) $event['ad_duration_seconds'] . 's' : '-' ?></td>
                </tr><?php endforeach; ?>
            </tbody></table></div>
        </section>
    <?php
}

function admin_render_anomalies_page(array $anomaliesAdmin): void
{
    $events = $anomaliesAdmin['events'] ?? [];
    $summary = $anomaliesAdmin['summary'] ?? [];
    ?>
        <section class="admin-kpis" aria-label="Anomalies">
            <?= admin_kpi('Anomalies 24h', (int) ($anomaliesAdmin['last24h'] ?? 0), 'orange') ?>
            <?= admin_kpi('Crashs 24h', (int) ($anomaliesAdmin['crashes24h'] ?? 0), 'red') ?>
            <?= admin_kpi('Types 7 jours', count($summary), 'cyan') ?>
        </section>

        <section class="admin-panel">
            <div class="admin-panel-heading"><div><h2>Synthese 7 jours</h2><p>Regroupement par type d anomalie.</p></div></div>
            <div class="admin-table-wrap"><table><thead><tr><th>Type</th><th>Occurrences</th><th>Dernier signal</th></tr></thead><tbody>
                <?php if ($summary === []): ?><tr><td colspan="3" class="admin-empty">Aucune anomalie recente.</td></tr><?php endif; ?>
                <?php foreach ($summary as $row): ?><tr><td><strong><?= admin_escape((string) $row['anomaly_type']) ?></strong></td><td><?= (int) $row['count'] ?></td><td><?= admin_escape((string) $row['last_seen']) ?></td></tr><?php endforeach; ?>
            </tbody></table></div>
        </section>

        <section class="admin-panel" id="anomaly-events">
            <div class="admin-panel-heading"><div><h2>Evenements recents</h2><p>Dernieres anomalies remontees par l application TV.</p></div><a class="admin-button compact secondary" href="/admin/?page=anomalies">Actualiser</a></div>
            <div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Device hash</th><th>App</th><th>Route</th><th>Type</th><th>Message</th><th>Stack trace</th><th>Contexte</th></tr></thead><tbody>
                <?php if ($events === []): ?><tr><td colspan="8" class="admin-empty">Aucune anomalie enregistree.</td></tr><?php endif; ?>
                <?php foreach ($events as $event): ?><tr>
                    <td><?= admin_escape((string) $event['created_at']) ?></td>
                    <td><small><?= admin_escape(anomaly_mask_hash($event['device_id_hash'] ?? null)) ?></small></td>
                    <td><?= admin_escape((string) ($event['app_version'] ?: '-')) ?><small><?= admin_escape((string) $event['platform']) ?></small></td>
                    <td><?= admin_escape((string) ($event['route'] ?: '-')) ?></td>
                    <td><strong><?= admin_escape((string) $event['anomaly_type']) ?></strong></td>
                    <td><small><?= admin_escape((string) ($event['message'] ?: '-')) ?></small></td>
                    <td><small><?= admin_escape(smartvision_text_substr((string) ($event['stack_trace'] ?: '-'), 0, 220)) ?></small></td>
                    <td><small><?= admin_escape((string) ($event['context_json'] ?: '-')) ?></small></td>
                </tr><?php endforeach; ?>
            </tbody></table></div>
        </section>
    <?php
}

function admin_render_behavior_segments_page(array $behaviorAdmin): void
{
    $available = (bool) ($behaviorAdmin['available'] ?? false);
    $message = (string) ($behaviorAdmin['message'] ?? '');
    $summary = is_array($behaviorAdmin['summary'] ?? null) ? $behaviorAdmin['summary'] : [];
    $segments = is_array($behaviorAdmin['segments'] ?? null) ? $behaviorAdmin['segments'] : [];
    $content = is_array($behaviorAdmin['content'] ?? null) ? $behaviorAdmin['content'] : [];
    $regions = is_array($behaviorAdmin['regions'] ?? null) ? $behaviorAdmin['regions'] : [];
    $countries = is_array($behaviorAdmin['countries'] ?? null) ? $behaviorAdmin['countries'] : [];
    $languages = is_array($behaviorAdmin['languages'] ?? null) ? $behaviorAdmin['languages'] : [];
    $interests = is_array($behaviorAdmin['interests'] ?? null) ? $behaviorAdmin['interests'] : [];
    $recentEvents = is_array($behaviorAdmin['recent_events'] ?? null) ? $behaviorAdmin['recent_events'] : [];
    ?>
    <section class="admin-kpis behavior-kpis" aria-label="Segmentation utilisateurs">
        <?= admin_kpi('Evenements tracking', number_format((int) ($summary['total_events'] ?? 0), 0, ',', ' '), 'blue') ?>
        <?= admin_kpi('Appareils trackes', number_format((int) ($summary['tracked_devices'] ?? 0), 0, ',', ' '), 'cyan') ?>
        <?= admin_kpi('Actifs 7 jours', number_format((int) ($summary['active_7d'] ?? 0), 0, ',', ' '), 'green') ?>
        <?= admin_kpi('Engagement moyen', number_format((float) ($summary['avg_engagement'] ?? 0), 1, ',', ' ') . ' / 100', 'orange') ?>
    </section>
    <?php if (!$available): ?><div class="admin-notice error"><?= admin_escape($message ?: 'Segmentation indisponible.') ?></div><?php endif; ?>
    <div data-tab-scope>
        <nav class="behavior-tabs" aria-label="Segmentation">
            <button type="button" class="active" data-tab-target="behavior-tab-overview">Vue</button>
            <button type="button" data-tab-target="behavior-tab-segments">Segments</button>
            <button type="button" data-tab-target="behavior-tab-insights">Interpretation</button>
            <button type="button" data-tab-target="behavior-tab-events">Evenements</button>
        </nav>
        <section class="admin-tab-panel active" id="behavior-tab-overview">
            <div class="behavior-dashboard-grid">
                <section class="admin-panel behavior-panel">
                    <div class="admin-panel-heading"><div><h2>Types de contenus</h2><p>Repartition des signaux utiles sur les 30 derniers jours.</p></div><a class="admin-button compact secondary" href="/admin/?page=segments">Actualiser</a></div>
                    <ul class="behavior-breakdown-list">
                        <?php if ($content === []): ?><li class="muted">Aucun contenu tracke.</li><?php endif; ?>
                        <?php foreach ($content as $row): ?><li><span><?= admin_escape((string) ($row['content_type'] ?? 'UNKNOWN')) ?></span><strong><?= number_format((int) ($row['events'] ?? 0), 0, ',', ' ') ?></strong><small><?= number_format((int) ($row['devices'] ?? 0), 0, ',', ' ') ?> appareil(s)</small></li><?php endforeach; ?>
                    </ul>
                </section>
                <section class="admin-panel behavior-panel">
                    <div class="admin-panel-heading"><div><h2>Lecture rapide</h2><p>Signaux exploitables pour ciblage.</p></div></div>
                    <ul class="behavior-breakdown-list">
                        <li><span>Dernier signal</span><strong><?= admin_escape((string) ($summary['last_event_at'] ?? '-')) ?></strong></li>
                        <li><span>Centres d'interet detectes</span><strong><?= number_format(count($interests), 0, ',', ' ') ?></strong></li>
                        <li><span>Pays detectes</span><strong><?= number_format(count($countries), 0, ',', ' ') ?></strong></li>
                    </ul>
                </section>
            </div>
        </section>
        <section class="admin-tab-panel" id="behavior-tab-segments" hidden>
            <section class="admin-panel behavior-panel">
                <div class="admin-panel-heading"><div><h2>Segments actifs</h2><p>Segments deduits automatiquement des usages 30 jours.</p></div></div>
                <div class="admin-table-wrap"><table><thead><tr><th>Segment</th><th>Groupe</th><th>Appareils</th><th>Score moyen</th><th>Dernier signal</th></tr></thead><tbody>
                    <?php if ($segments === []): ?><tr><td colspan="5" class="admin-empty">Aucun segment calcule pour le moment.</td></tr><?php endif; ?>
                    <?php foreach ($segments as $segment): ?><tr>
                        <td><strong><?= admin_escape((string) ($segment['segment_label'] ?? '-')) ?></strong><small><?= admin_escape((string) ($segment['segment_key'] ?? '-')) ?></small></td>
                        <td><span class="admin-state <?= admin_escape(strtolower((string) ($segment['segment_group'] ?? 'content'))) ?>"><?= admin_escape((string) ($segment['segment_group'] ?? '-')) ?></span></td>
                        <td><?= number_format((int) ($segment['devices'] ?? 0), 0, ',', ' ') ?></td>
                        <td><?= admin_escape((string) ($segment['avg_score'] ?? '0')) ?></td>
                        <td><?= admin_escape((string) ($segment['last_seen_at'] ?? '-')) ?></td>
                    </tr><?php endforeach; ?>
                </tbody></table></div>
            </section>
        </section>
        <section class="admin-tab-panel" id="behavior-tab-insights" hidden>
            <section class="admin-panel behavior-panel">
                <div class="admin-panel-heading"><div><h2>Interpretation contenu consomme</h2><p>Regions, pays, langues et centres d'interet deduits des categories et medias.</p></div></div>
                <div class="behavior-insight-grid">
                    <section><h3>Regions</h3><?= admin_render_behavior_breakdown($regions) ?></section>
                    <section><h3>Pays</h3><?= admin_render_behavior_breakdown($countries) ?></section>
                    <section><h3>Langues</h3><?= admin_render_behavior_breakdown($languages) ?></section>
                    <section><h3>Centres d'interet</h3><?= admin_render_behavior_breakdown($interests) ?></section>
                </div>
            </section>
        </section>
        <section class="admin-tab-panel" id="behavior-tab-events" hidden>
            <section class="admin-panel behavior-panel">
                <div class="admin-panel-heading"><div><h2>Evenements comportementaux recents</h2><p>Derniers signaux utiles recus depuis les applications TV.</p></div></div>
                <div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Code TV</th><th>Type licence</th><th>Event</th><th>Contenu</th><th>Source</th><th>Categorie</th><th>Media</th><th>Plateforme</th><th>App</th><th>Engagement</th></tr></thead><tbody>
                    <?php if ($recentEvents === []): ?><tr><td colspan="11" class="admin-empty">Aucun evenement recent.</td></tr><?php endif; ?>
                    <?php foreach ($recentEvents as $event): ?><tr>
                        <td><?= admin_escape((string) ($event['created_at'] ?? '-')) ?></td>
                        <td><strong><?= admin_escape((string) ($event['public_device_code'] ?? '------')) ?></strong></td>
                        <td><?= admin_escape((string) ($event['license_type'] ?? '-')) ?></td>
                        <td><strong><?= admin_escape((string) ($event['event_type'] ?? '-')) ?></strong></td>
                        <td><?= admin_escape((string) ($event['content_type'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['source_screen'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['category_label'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['content_title'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['platform'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['app_version'] ?? '-')) ?></td>
                        <td><?= admin_escape((string) ($event['engagement_score'] ?? '-')) ?></td>
                    </tr><?php endforeach; ?>
                </tbody></table></div>
            </section>
        </section>
    </div>
    <?php
}

function admin_render_diagnostics_page(array $diagnosticsAdmin, array $anomaliesAdmin, array $serverStats, array $auditLogs, array $stats): void
{
    $rows = $diagnosticsAdmin['rows'] ?? [];
    $available = (bool) ($diagnosticsAdmin['available'] ?? false);
    $message = (string) ($diagnosticsAdmin['message'] ?? '');
    $withAutostart = 0;
    $withAutoSync = 0;
    $withErrors = 0;
    $autoSyncSuccess = 0;
    $autoSyncFailure = 0;
    $autostartSuccess = 0;
    $autostartFailure = 0;
    foreach ($rows as $row) {
        $autostart = $row['diagnostics']['autostart']['payload'] ?? [];
        $autoSync = $row['diagnostics']['auto_sync']['payload'] ?? [];
        if ($autostart !== []) {
            $withAutostart++;
        }
        if ($autoSync !== []) {
            $withAutoSync++;
        }
        if (!empty($autostart['last_error']) || !empty($autoSync['last_error'])) {
            $withErrors++;
        }
        if (($autoSync['last_result'] ?? '') === 'success') {
            $autoSyncSuccess++;
        } elseif (($autoSync['last_result'] ?? '') === 'error' || !empty($autoSync['last_error'])) {
            $autoSyncFailure++;
        }
        if (!empty($autostart['boot_completed']) && empty($autostart['last_error'])) {
            $autostartSuccess++;
        } elseif (!empty($autostart['last_error'])) {
            $autostartFailure++;
        }
    }
    $events = $anomaliesAdmin['events'] ?? [];
    $summary = $anomaliesAdmin['summary'] ?? [];
    $disk = admin_find_cpanel_metric($serverStats, 'diskusage');
    $globalState = $withErrors > 0 || (int) ($anomaliesAdmin['last24h'] ?? 0) > 0 ? 'A surveiller' : 'OK';
    $backendState = (($serverStats['database']['version'] ?? '') !== 'Non disponible') ? 'OK' : 'A verifier';
    ?>
        <div data-tab-scope>
            <nav class="behavior-tabs" aria-label="Diagnostics">
                <button type="button" class="active" data-tab-target="diagnostics-tab-summary">Synthese</button>
                <button type="button" data-tab-target="diagnostics-tab-autosync">AutoSync</button>
                <button type="button" data-tab-target="diagnostics-tab-anomalies">Anomalies App</button>
                <button type="button" data-tab-target="diagnostics-tab-server">Info Serveur</button>
                <button type="button" data-tab-target="diagnostics-tab-journal">Journal</button>
            </nav>

            <section class="admin-tab-panel active" id="diagnostics-tab-summary">
                <section class="admin-kpis" aria-label="Synthese diagnostics">
                    <?= admin_kpi('Etat application', $globalState, $globalState === 'OK' ? 'green' : 'orange') ?>
                    <?= admin_kpi('Backend', $backendState, $backendState === 'OK' ? 'green' : 'orange') ?>
                    <?= admin_kpi('Taille base', $serverStats['database']['size'] ?? 'N/D', 'green') ?>
                    <?= admin_kpi('Disk usage', $disk ? (string) ($disk['percent'] !== null ? number_format((float) $disk['percent'], 1, ',', ' ') . ' %' : ($disk['value'] ?? 'N/D')) : 'N/D', 'cyan') ?>
                    <?= admin_kpi('Appareils actifs', (int) ($stats['active_devices'] ?? 0), 'green') ?>
                    <?= admin_kpi('Anomalies 24h', (int) ($anomaliesAdmin['last24h'] ?? 0), 'orange') ?>
                    <?= admin_kpi('PROCESS_EXIT_CRASH', (int) ($anomaliesAdmin['processExitCrashes24h'] ?? 0), ((int) ($anomaliesAdmin['processExitCrashes24h'] ?? 0)) > 0 ? 'red' : 'green') ?>
                </section>
                <section class="admin-panel">
                    <div class="admin-panel-heading"><div><h2>Vue globale</h2><p>Indicateurs consolides depuis diagnostics device, anomalies et serveur.</p></div><a class="admin-button compact secondary" href="/admin/?page=diagnostics">Actualiser</a></div>
                    <div class="server-metric-list">
                        <div><span>TV remontees</span><strong><?= count($rows) ?></strong></div>
                        <div><span>Diagnostics avec erreur</span><strong><?= $withErrors ?></strong></div>
                        <div><span>AutoSync suivis</span><strong><?= $withAutoSync ?></strong></div>
                        <div><span>Autostart suivis</span><strong><?= $withAutostart ?></strong></div>
                        <div><span>Disk usage</span><strong><?= $disk ? admin_escape((string) ($disk['value'] ?? 'N/D')) . ' / ' . admin_escape((string) ($disk['max'] ?? 'N/D')) : 'N/D' ?></strong></div>
                        <div><span>Derniere lecture serveur</span><strong><?= admin_escape((string) ($serverStats['generated_at'] ?? '-')) ?> UTC</strong></div>
                    </div>
                </section>
            </section>

            <section class="admin-tab-panel" id="diagnostics-tab-autosync" hidden>
                <section class="admin-kpis" aria-label="AutoSync">
                    <?= admin_kpi('AutoSync OK', $autoSyncSuccess, 'green') ?>
                    <?= admin_kpi('AutoSync echecs', $autoSyncFailure, $autoSyncFailure > 0 ? 'orange' : 'green') ?>
                    <?= admin_kpi('Autostart OK', $autostartSuccess, 'green') ?>
                    <?= admin_kpi('Autostart echecs', $autostartFailure, $autostartFailure > 0 ? 'orange' : 'green') ?>
                </section>
                <section class="admin-panel" id="diagnostics">
                    <div class="admin-panel-heading"><div><h2>AutoSync & AutoStart</h2><p>Derniers etats autostart et synchronisation arriere-plan recus par les TV.</p></div></div>
                    <?php if (!$available): ?><p class="admin-empty"><?= admin_escape($message !== '' ? $message : 'Diagnostics indisponibles.') ?></p><?php endif; ?>
                    <div class="admin-table-wrap"><table><thead><tr><th>Appareil</th><th>Licence</th><th>Autostart</th><th>AutoSync</th><th>Plateforme</th><th>Dernier signal</th><th>Erreurs</th></tr></thead><tbody>
                        <?php if ($rows === []): ?><tr><td colspan="7" class="admin-empty">Aucun diagnostic recu pour le moment.</td></tr><?php endif; ?>
                        <?php foreach ($rows as $row):
                            $autostartDiag = $row['diagnostics']['autostart'] ?? [];
                            $autoSyncDiag = $row['diagnostics']['auto_sync'] ?? [];
                            $autostart = is_array($autostartDiag['payload'] ?? null) ? $autostartDiag['payload'] : [];
                            $autoSync = is_array($autoSyncDiag['payload'] ?? null) ? $autoSyncDiag['payload'] : [];
                            $autoSyncSize = isset($autoSync['last_size_kb']) ? (string) $autoSync['last_size_kb'] . ' KB' : 'N/D';
                            $errors = trim((string) ($autostart['last_error'] ?? '') . ' ' . (string) ($autoSync['last_error'] ?? ''));
                        ?><tr>
                            <td><strong><?= admin_escape((string) ($row['public_device_code'] ?: '------')) ?></strong><small><?= admin_escape((string) ($row['device_name'] ?: 'Android TV')) ?></small><small class="mono"><?= admin_escape((string) ($row['device_id'] ?: '-')) ?></small></td>
                            <td><?= admin_escape((string) ($row['license_type'] ?: $row['license_status'] ?: '-')) ?><small><?= admin_escape((string) ($row['device_status'] ?: '-')) ?></small></td>
                            <td><span class="admin-state <?= !empty($autostart['enabled']) ? 'active' : 'disabled' ?>"><?= !empty($autostart['enabled']) ? 'Active' : 'Off' ?></span><small>Source <?= admin_escape((string) ($autostart['last_source'] ?? '-')) ?> / essais <?= admin_escape((string) ($autostart['attempts_this_boot'] ?? '-')) ?></small><small><?= admin_escape((string) ($autostartDiag['updated_at'] ?? '-')) ?></small></td>
                            <td><span class="admin-state <?= (($autoSync['last_result'] ?? '') === 'success') ? 'active' : 'pending' ?>"><?= admin_escape((string) ($autoSync['last_result'] ?? '-')) ?></span><small>Source <?= admin_escape((string) ($autoSync['last_source'] ?? '-')) ?> / <?= admin_escape((string) ($autoSync['last_duration_ms'] ?? '-')) ?> ms</small><small>Taille <?= admin_escape($autoSyncSize) ?></small></td>
                            <td><?= admin_escape((string) ($autostartDiag['device_model'] ?? $autoSyncDiag['device_model'] ?? '-')) ?><small>Android <?= admin_escape((string) ($autostartDiag['android_version'] ?? $autoSyncDiag['android_version'] ?? '-')) ?></small><small>APK <?= admin_escape((string) ($autostartDiag['app_version'] ?? $autoSyncDiag['app_version'] ?? '-')) ?></small></td>
                            <td><?= admin_escape((string) ($row['latest_at'] ?? '-')) ?><small>Activite <?= admin_escape((string) ($row['last_seen_at'] ?? '-')) ?></small></td>
                            <td><small><?= admin_escape($errors !== '' ? smartvision_text_substr($errors, 0, 220) : '-') ?></small></td>
                        </tr><?php endforeach; ?>
                    </tbody></table></div>
                </section>
            </section>

            <section class="admin-tab-panel" id="diagnostics-tab-anomalies" hidden>
                <section class="admin-kpis" aria-label="Anomalies App">
                    <?= admin_kpi('Anomalies 24h', (int) ($anomaliesAdmin['last24h'] ?? 0), 'orange') ?>
                    <?= admin_kpi('Crashs 24h', (int) ($anomaliesAdmin['crashes24h'] ?? 0), 'red') ?>
                    <?= admin_kpi('Types 7 jours', count($summary), 'cyan') ?>
                </section>
                <section class="admin-panel">
                    <div class="admin-panel-heading"><div><h2>Anomalies App</h2><p>Problemes Xtream, synchronisation, crashes, buffering et autres signaux remontes par les applications.</p></div></div>
                    <div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Code TV</th><th>Type</th><th>Niveau</th><th>Message</th><th>Detail technique</th><th>Version app</th></tr></thead><tbody>
                        <?php if ($events === []): ?><tr><td colspan="7" class="admin-empty">Aucune anomalie enregistree.</td></tr><?php endif; ?>
                        <?php foreach ($events as $event): $level = admin_anomaly_level((string) ($event['anomaly_type'] ?? ''), (string) ($event['message'] ?? '')); ?><tr>
                            <td><?= admin_escape((string) ($event['created_at'] ?? '-')) ?></td>
                            <td><strong><?= admin_escape((string) ($event['public_device_code'] ?: '------')) ?></strong><small><?= admin_escape(anomaly_mask_hash($event['device_id_hash'] ?? null)) ?></small></td>
                            <td><strong><?= admin_escape((string) ($event['anomaly_type'] ?? '-')) ?></strong></td>
                            <td><span class="admin-state <?= admin_escape($level) ?>"><?= admin_escape($level) ?></span></td>
                            <td><small><?= admin_escape((string) ($event['message'] ?: '-')) ?></small></td>
                            <td><small><?= admin_escape(smartvision_text_substr(trim((string) ($event['stack_trace'] ?: '') . ' ' . (string) ($event['context_json'] ?: '')) ?: '-', 0, 260)) ?></small></td>
                            <td><?= admin_escape((string) ($event['app_version'] ?: '-')) ?><small><?= admin_escape((string) ($event['platform'] ?? '-')) ?></small></td>
                        </tr><?php endforeach; ?>
                    </tbody></table></div>
                </section>
            </section>

            <section class="admin-tab-panel" id="diagnostics-tab-server" hidden>
                <section class="admin-kpis server-kpis" aria-label="Info serveur">
                    <?= admin_kpi('PHP', $serverStats['php']['version'] ?? 'N/D', 'blue') ?>
                    <?= admin_kpi('MySQL/MariaDB', $serverStats['database']['version'] ?? 'N/D', 'cyan') ?>
                    <?= admin_kpi('Taille base', $serverStats['database']['size'] ?? 'N/D', 'green') ?>
                    <?= admin_kpi('Appareils actifs', $serverStats['database']['active_devices'] ?? 0, 'green') ?>
                    <?= admin_kpi('cPanel', !empty($serverStats['cpanel']['ok']) ? 'Connecte' : 'A verifier', !empty($serverStats['cpanel']['ok']) ? 'cyan' : 'orange') ?>
                </section>
                <div class="server-dashboard-grid">
                    <section class="admin-panel server-panel"><div class="admin-panel-heading"><div><h2>Runtime backend</h2><p>Etat API, base de donnees et runtime PHP.</p></div></div><div class="server-metric-list">
                        <div><span>Memoire PHP</span><strong><?= admin_escape((string) ($serverStats['php']['memory_limit'] ?? 'N/D')) ?></strong></div>
                        <div><span>Upload max</span><strong><?= admin_escape((string) ($serverStats['php']['upload_max_filesize'] ?? 'N/D')) ?></strong></div>
                        <div><span>Execution max</span><strong><?= admin_escape((string) ($serverStats['php']['max_execution_time'] ?? 'N/D')) ?>s</strong></div>
                        <div><span>OPcache</span><strong><?= admin_escape((string) ($serverStats['php']['opcache'] ?? 'N/D')) ?></strong></div>
                        <div><span>Sessions attente</span><strong><?= (int) ($serverStats['database']['pending_sessions'] ?? 0) ?></strong></div>
                        <div><span>Etat API cPanel</span><strong><?= admin_escape((string) ($serverStats['cpanel']['message'] ?? 'N/D')) ?></strong></div>
                    </div></section>
                    <section class="admin-panel server-panel"><div class="admin-panel-heading"><div><h2>Ressources serveur</h2><p>Utilisation, limite et pourcentage cPanel.</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Ressource</th><th>Utilisation</th><th>Limite</th><th>%</th></tr></thead><tbody>
                        <?php if (($serverStats['cpanel']['metrics'] ?? []) === []): ?><tr><td colspan="4" class="admin-empty"><?= admin_escape((string) ($serverStats['cpanel']['message'] ?? 'Aucune metrique cPanel disponible.')) ?></td></tr><?php endif; ?>
                        <?php foreach (($serverStats['cpanel']['metrics'] ?? []) as $metric): ?><tr><td><strong><?= admin_escape((string) ($metric['label'] ?? 'Metric')) ?></strong><small><?= admin_escape((string) ($metric['id'] ?? '-')) ?></small></td><td><?= admin_escape((string) ($metric['value'] ?? 'N/D')) ?></td><td><?= admin_escape((string) ($metric['max'] ?? 'N/D')) ?></td><td><?= array_key_exists('percent', $metric) && $metric['percent'] !== null ? admin_escape(number_format((float) $metric['percent'], 1, ',', ' ') . ' %') : '-' ?></td></tr><?php endforeach; ?>
                    </tbody></table></div></section>
                </div>
            </section>

            <section class="admin-tab-panel" id="diagnostics-tab-journal" hidden>
                <section class="admin-panel" id="audit"><div class="admin-panel-heading"><div><h2>Journal</h2><p>Dernieres actions admin et evenements backend importants.</p></div></div><div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Action</th><th>Cible</th></tr></thead><tbody>
                    <?php if ($auditLogs === []): ?><tr><td colspan="3" class="admin-empty">Aucune action admin recente.</td></tr><?php endif; ?>
                    <?php foreach ($auditLogs as $log): ?><tr><td><?= admin_escape($log['created_at']) ?></td><td><?= admin_escape($log['action']) ?><small><?= admin_escape($log['admin_username']) ?></small></td><td><?= admin_escape(trim((string) $log['target_type'] . ' ' . (string) $log['target_id'])) ?></td></tr><?php endforeach; ?>
                </tbody></table></div></section>
            </section>
        </div>
    <?php
}

function admin_find_cpanel_metric(array $serverStats, string $needle): ?array
{
    foreach (($serverStats['cpanel']['metrics'] ?? []) as $metric) {
        $id = strtolower((string) ($metric['id'] ?? ''));
        $label = strtolower((string) ($metric['label'] ?? ''));
        if ($id === strtolower($needle) || str_contains($id, strtolower($needle)) || str_contains($label, strtolower($needle))) {
            return is_array($metric) ? $metric : null;
        }
    }

    return null;
}

function admin_anomaly_level(string $type, string $message): string
{
    $value = strtoupper($type . ' ' . $message);
    if (str_contains($value, 'CRASH') || str_contains($value, 'ERROR') || str_contains($value, 'FAILED')) {
        return 'error';
    }
    if (str_contains($value, 'WARNING') || str_contains($value, 'TIMEOUT') || str_contains($value, 'NETWORK')) {
        return 'warning';
    }

    return 'info';
}

function admin_render_features_page(array $appConfigAdmin): void
{
    $features = $appConfigAdmin['features'] ?? admin_default_feature_access();
    $variables = $appConfigAdmin['consent_variables'] ?? admin_default_consent_variables();
    $trending = $appConfigAdmin['trending'] ?? admin_default_trending_config();
    ?>
        <form method="post">
            <input type="hidden" name="redirect_page" value="features">
            <input type="hidden" name="action" value="save_feature_access">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">

            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Gestion des fonctionnalites</h2><p>Disponibilite dynamique selon le type de licence utilisateur.</p></div></div>
                <div class="admin-table-wrap"><table><thead><tr><th>Fonctionnalite</th><th>Premium</th><th>Essai gratuit 7 jours</th><th>Free Ads</th></tr></thead><tbody>
                    <?php foreach ($features as $feature): $key = (string) $feature['key']; ?><tr>
                        <td><strong><?= admin_escape((string) $feature['label']) ?></strong><small><?= admin_escape($key) ?></small></td>
                        <td><label><input type="checkbox" name="feature[<?= admin_escape($key) ?>][premium]" value="1"<?= !empty($feature['premium']) ? ' checked' : '' ?>> Oui</label></td>
                        <td><label><input type="checkbox" name="feature[<?= admin_escape($key) ?>][trial]" value="1"<?= !empty($feature['trial']) ? ' checked' : '' ?>> Oui</label></td>
                        <td><label><input type="checkbox" name="feature[<?= admin_escape($key) ?>][free_ads]" value="1"<?= !empty($feature['free_ads']) ? ' checked' : '' ?>> Oui</label></td>
                    </tr><?php endforeach; ?>
                </tbody></table></div>
                <button class="admin-button primary" type="submit">Enregistrer les fonctionnalites</button>
            </section>
        </form>

        <form method="post">
            <input type="hidden" name="redirect_page" value="features">
            <input type="hidden" name="action" value="save_trending_config">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Tendances Home</h2><p>Filtres et limites utilises par les sections Tendances films et Tendances series.</p></div></div>
                <div class="admin-form-grid">
                    <label><span>Candidats analyses</span><input name="trending[candidate_limit]" type="number" min="10" max="100" value="<?= (int) ($trending['candidate_limit'] ?? 50) ?>"></label>
                    <label><span>Cards par section</span><input name="trending[section_limit]" type="number" min="1" max="20" value="<?= (int) ($trending['section_limit'] ?? 10) ?>"></label>
                    <label><span>Note minimale si filtre actif</span><input name="trending[minimum_rating]" type="number" min="0" max="10" step="0.1" value="<?= admin_escape((string) ($trending['minimum_rating'] ?? '9')) ?>"></label>
                </div>
                <div class="admin-check-grid">
                    <label><input type="checkbox" name="trending[require_landscape_image]" value="1"<?= !empty($trending['require_landscape_image']) ? ' checked' : '' ?>> Exiger un poster paysage</label>
                    <label><input type="hidden" name="trending[exclude_adult]" value="1"><input type="checkbox" value="1" checked disabled> Exclure le contenu adulte</label>
                    <label><input type="checkbox" name="trending[use_rating_filter]" value="1"<?= !empty($trending['use_rating_filter']) ? ' checked' : '' ?>> Activer le filtre note</label>
                </div>
                <p class="muted">Par defaut, le filtre note est desactive. Le filtre poster paysage et le filtre adulte restent actifs.</p>
                <button class="admin-button primary" type="submit">Enregistrer les tendances</button>
            </section>
        </form>

        <form method="post">
            <input type="hidden" name="redirect_page" value="features">
            <input type="hidden" name="action" value="save_app_consent">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
            <section class="admin-panel consent-admin-panel">
                <div class="admin-panel-heading"><div><h2>Consentement TV</h2><p>Texte affiche uniquement au premier lancement ou lorsqu une nouvelle version est publiee.</p></div></div>
                <div class="consent-admin-layout">
                    <div class="consent-admin-main">
                        <div class="consent-meta-row">
                            <label><span>Version</span><input name="consent_version" maxlength="40" value="<?= admin_escape((string) ($appConfigAdmin['consent_version'] ?? '2026-06-28')) ?>"></label>
                            <label><span>Titre popup</span><input name="consent_title" maxlength="120" value="<?= admin_escape((string) ($appConfigAdmin['consent_title'] ?? 'Privacy Policy and Terms of Use')) ?>"></label>
                        </div>
                        <label class="consent-body-field"><span>Texte complet</span><textarea name="consent_body" rows="18" maxlength="30000"><?= admin_escape((string) ($appConfigAdmin['consent_body'] ?? admin_default_consent_body())) ?></textarea></label>
                    </div>
                    <aside class="consent-admin-vars">
                        <h3>Variables</h3>
                        <p>Utilisez <code>?NOM?</code> dans le texte. Les passages entre <code>**</code> sont affiches en gras sur la TV.</p>
                        <?php foreach (admin_default_consent_variables() as $key => $default): ?>
                            <label><span><?= admin_escape($key) ?></span><input name="consent_variables[<?= admin_escape($key) ?>]" maxlength="255" value="<?= admin_escape((string) ($variables[$key] ?? $default)) ?>"></label>
                        <?php endforeach; ?>
                    </aside>
                </div>
                <p class="muted">Les passages encadres par **double astérisque** sont affiches en gras dans l application TV.</p>
                <button class="admin-button primary" type="submit">Enregistrer le consentement TV</button>
            </section>
        </form>
    <?php
}

function admin_render_private_media_page(array $privateMediaAdmin): void
{
    $available = !empty($privateMediaAdmin['available']);
    $config = $privateMediaAdmin['config'] ?? (function_exists('private_media_default_config') ? private_media_default_config() : []);
    $health = $privateMediaAdmin['health'] ?? null;
    $sections = is_array($config['sections'] ?? null) ? $config['sections'] : [];
    if ($sections === [] && function_exists('private_media_default_config')) {
        $sections = private_media_default_config()['sections'];
    }
    $orders = ['latest', 'longest', 'shortest', 'top-rated', 'most-popular', 'top-weekly', 'top-monthly'];
    ?>
        <?php if (!$available): ?>
            <section class="admin-panel"><p class="admin-empty"><?= admin_escape((string) ($privateMediaAdmin['message'] ?? 'Bibliotheque privee indisponible.')) ?></p></section>
        <?php return; endif; ?>

        <form method="post">
            <input type="hidden" name="redirect_page" value="private_media">
            <input type="hidden" name="action" value="save_private_media_config">
            <input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>">
            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Activation</h2><p>L'application Android ne consomme que les endpoints SmartVision internes.</p></div></div>
                <div class="admin-check-grid">
                    <label><input type="checkbox" name="private_media[enabled]" value="1"<?= !empty($config['enabled']) ? ' checked' : '' ?>> Activer la bibliotheque privee</label>
                    <label><input type="checkbox" name="private_media[provider_eporner_enabled]" value="1"<?= !empty($config['provider_eporner_enabled']) ? ' checked' : '' ?>> Activer Eporner via proxy</label>
                    <label><input type="checkbox" name="private_media[show_in_app]" value="1"<?= !empty($config['show_in_app']) ? ' checked' : '' ?>> Afficher Media prives dans l'app</label>
                    <label><input type="checkbox" name="private_media[native_playback_enabled]" value="1"<?= !empty($config['native_playback_enabled']) ? ' checked' : '' ?>> Autoriser lecture native si flux compatible</label>
                    <label><input type="checkbox" name="private_media[force_native_playback_enabled]" value="1"<?= !empty($config['force_native_playback_enabled']) ? ' checked' : '' ?>> Forcer lecture native HLS/MP4</label>
                </div>
                <div class="admin-form-grid">
                    <label><span>Elements par page</span><input name="private_media[per_page]" type="number" min="1" max="50" value="<?= (int) ($config['per_page'] ?? 24) ?>"></label>
                    <label><span>Taille thumbnails</span><select name="private_media[thumbsize]"><?php foreach (['small', 'medium', 'big'] as $size): ?><option value="<?= admin_escape($size) ?>"<?= ($config['thumbsize'] ?? 'big') === $size ? ' selected' : '' ?>><?= admin_escape($size) ?></option><?php endforeach; ?></select></label>
                    <label><span>Ordre par defaut</span><select name="private_media[order]"><?php foreach ($orders as $order): ?><option value="<?= admin_escape($order) ?>"<?= ($config['order'] ?? 'latest') === $order ? ' selected' : '' ?>><?= admin_escape($order) ?></option><?php endforeach; ?></select></label>
                    <label><span>Flux HLS/MP4 de test</span><input name="private_media[native_test_stream_url]" maxlength="1000" placeholder="https://.../stream.m3u8 ou .mp4" value="<?= admin_escape((string) ($config['native_test_stream_url'] ?? '')) ?>"></label>
                </div>
            </section>

            <section class="admin-panel">
                <div class="admin-panel-heading"><div><h2>Sous-dossiers TV</h2><p>Chaque ligne cree un sous-dossier expandable sous Media prives. Le nom est affiche sur TV; la query/theme alimente la recherche backend.</p></div></div>
                <div class="admin-table-wrap"><table><thead><tr><th>Active</th><th>Nom sous-dossier</th><th>Recherche / theme</th><th>Ordre</th><th>Supprimer</th></tr></thead><tbody>
                    <?php foreach ($sections as $index => $section): ?><tr>
                        <td><input type="hidden" name="private_media[section_id][<?= (int) $index ?>]" value="<?= admin_escape((string) ($section['id'] ?? '')) ?>"><input type="checkbox" name="private_media[section_enabled][<?= (int) $index ?>]" value="1"<?= !empty($section['enabled']) ? ' checked' : '' ?>></td>
                        <td><input name="private_media[section_title][<?= (int) $index ?>]" maxlength="80" value="<?= admin_escape((string) ($section['title'] ?? '')) ?>"></td>
                        <td><input name="private_media[section_query][<?= (int) $index ?>]" maxlength="120" value="<?= admin_escape((string) ($section['query'] ?? '')) ?>"></td>
                        <td><select name="private_media[section_order][<?= (int) $index ?>]"><?php foreach ($orders as $order): ?><option value="<?= admin_escape($order) ?>"<?= ($section['order'] ?? 'latest') === $order ? ' selected' : '' ?>><?= admin_escape($order) ?></option><?php endforeach; ?></select></td>
                        <td><label class="inline-check"><input type="checkbox" name="private_media[section_delete][<?= (int) $index ?>]" value="1"> Supprimer</label></td>
                    </tr><?php endforeach; ?>
                    <?php $newIndex = count($sections); ?><tr>
                        <td><input type="hidden" name="private_media[section_id][<?= (int) $newIndex ?>]" value=""><input type="checkbox" name="private_media[section_enabled][<?= (int) $newIndex ?>]" value="1"></td>
                        <td><input name="private_media[section_title][<?= (int) $newIndex ?>]" maxlength="80" placeholder="Nouveau sous-dossier"></td>
                        <td><input name="private_media[section_query][<?= (int) $newIndex ?>]" maxlength="120" placeholder="theme ou recherche"></td>
                        <td><select name="private_media[section_order][<?= (int) $newIndex ?>]"><?php foreach ($orders as $order): ?><option value="<?= admin_escape($order) ?>"><?= admin_escape($order) ?></option><?php endforeach; ?></select></td>
                        <td><span class="muted">Nouvelle ligne</span></td>
                    </tr>
                </tbody></table></div>
                <p class="muted">Premiers sous-dossiers fournis par defaut: Nouveautes, Populaires, Top semaine, Mieux notees, Long format, Amateur, Couples, POV. Aucun scraping: search/id/removed officiels uniquement. Lecture native HLS/MP4 seulement si SmartVision recoit un flux direct compatible; sinon l'app TV utilise le player embed officiel.</p>
                <button class="admin-button primary" type="submit">Enregistrer la bibliotheque privee</button>
            </section>
        </form>

        <section class="admin-panel">
            <div class="admin-panel-heading"><div><h2>Cache et monitoring</h2><p>Etat provider et operations de maintenance.</p></div></div>
            <div class="server-metric-list">
                <div><span>Provider</span><strong>Eporner</strong></div>
                <div><span>Statut</span><strong><?= admin_escape((string) ($health['status'] ?? 'non verifie')) ?></strong></div>
                <div><span>Latence</span><strong><?= isset($health['latency_ms']) ? (int) $health['latency_ms'] . ' ms' : '-' ?></strong></div>
                <div><span>Dernier check</span><strong><?= admin_escape((string) ($health['last_checked_at'] ?? '-')) ?></strong></div>
            </div>
            <?php if (!empty($health['last_error'])): ?><p class="admin-empty"><?= admin_escape((string) $health['last_error']) ?></p><?php endif; ?>
            <div class="admin-actions">
                <form method="post"><input type="hidden" name="redirect_page" value="private_media"><input type="hidden" name="action" value="private_media_sync_removed"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><button class="admin-button secondary" type="submit">Synchroniser removed</button></form>
                <form method="post"><input type="hidden" name="redirect_page" value="private_media"><input type="hidden" name="action" value="private_media_clear_cache"><input type="hidden" name="csrf_token" value="<?= admin_escape(csrf_token()) ?>"><button class="admin-button secondary" type="submit">Vider le cache</button></form>
            </div>
        </section>
    <?php
}

function admin_mask_url(string $url): string
{
    $url = trim($url);
    if ($url === '') {
        return '-';
    }
    $parts = parse_url($url);
    if (!is_array($parts) || empty($parts['host'])) {
        return smartvision_text_substr($url, 0, 32) . '...';
    }

    return (string) ($parts['scheme'] ?? 'https') . '://' . (string) $parts['host'] . '/...';
}

function admin_hilltop_balance_label(array $balance): string
{
    if (empty($balance['ok'])) {
        return 'N/D';
    }

    return trim((string) ($balance['balance'] ?? '0') . ' ' . (string) ($balance['currency'] ?? ''));
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
            <strong><?= number_format((int) ($device['ad_views'] ?? 0), 0, ',', ' ') ?></strong>
            <small>pub(s) vue(s)</small>
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
    $behavior = is_array($device['behavior'] ?? null) ? $device['behavior'] : [];
    $segments = is_array($behavior['segments'] ?? null) ? $behavior['segments'] : [];
    $recentEvents = is_array($behavior['recent_events'] ?? null) ? $behavior['recent_events'] : [];
    $contentBreakdown = is_array($behavior['content_breakdown'] ?? null) ? $behavior['content_breakdown'] : [];
    $categoryBreakdown = is_array($behavior['category_breakdown'] ?? null) ? $behavior['category_breakdown'] : [];
    $languageBreakdown = is_array($behavior['language_breakdown'] ?? null) ? $behavior['language_breakdown'] : [];
    $countryBreakdown = is_array($behavior['country_breakdown'] ?? null) ? $behavior['country_breakdown'] : [];
    $regionBreakdown = is_array($behavior['region_breakdown'] ?? null) ? $behavior['region_breakdown'] : [];
    $interestBreakdown = is_array($behavior['interest_breakdown'] ?? null) ? $behavior['interest_breakdown'] : [];
    ?><div class="admin-modal" id="<?= admin_escape($modalId) ?>" role="dialog" aria-modal="true" aria-labelledby="<?= admin_escape($modalId) ?>-title" hidden>
        <div class="admin-modal-backdrop" data-modal-close></div>
        <section class="admin-modal-card">
            <header class="admin-modal-header">
                <div><h2 id="<?= admin_escape($modalId) ?>-title"><?= admin_escape($device['public_device_code'] ?: '------') ?> - <?= admin_escape($device['device_name'] ?: 'Android TV') ?></h2><p class="mono"><?= admin_escape($deviceId) ?></p></div>
                <button type="button" class="admin-modal-close" data-modal-close>Fermer</button>
            </header>
            <nav class="admin-modal-tabs" aria-label="Details appareil">
                <button type="button" class="active" data-tab-target="<?= admin_escape($modalId) ?>-summary">Synthese</button>
                <button type="button" data-tab-target="<?= admin_escape($modalId) ?>-tracking">Tracking</button>
                <button type="button" data-tab-target="<?= admin_escape($modalId) ?>-analysis">Analyse</button>
                <button type="button" data-tab-target="<?= admin_escape($modalId) ?>-diagnostics">Diagnostics</button>
                <button type="button" data-tab-target="<?= admin_escape($modalId) ?>-history">Historique</button>
            </nav>
            <div class="admin-tab-panel active" id="<?= admin_escape($modalId) ?>-summary">
                <div class="admin-detail-grid">
                    <div><span>Etat</span><strong><span class="admin-state <?= admin_escape($device['status']) ?>"><?= admin_escape($device['status']) ?></span></strong></div>
                    <div><span>Licence</span><strong><?= admin_escape($device['license_status'] ?: '-') ?></strong></div>
                    <div><span>Xtream</span><strong><?= admin_escape($device['xtream_status'] ?: '-') ?></strong></div>
                    <div><span>Ad views</span><strong><?= number_format((int) ($device['ad_views'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Derniere pub vue</span><strong><?= admin_escape($device['last_ad_view_at'] ?: '-') ?></strong></div>
                    <div><span>App version</span><strong><?= admin_escape($device['app_version'] ?: '-') ?></strong></div>
                    <div><span>Premiere vue</span><strong><?= admin_escape($device['first_seen_at'] ?: $device['created_at'] ?: '-') ?></strong></div>
                    <div><span>Derniere activite</span><strong><?= admin_escape($device['last_seen_at'] ?: '-') ?></strong></div>
                    <div><span>Expiration</span><strong><?= admin_escape($device['expires_at'] ?: '-') ?></strong></div>
                    <div><span>Pays / UA</span><strong><?= admin_escape($device['country_code'] ?: 'N/D') ?> / <?= admin_escape($device['last_user_agent'] ? 'present' : '-') ?></strong></div>
                </div>
                <div class="admin-modal-columns single-row">
                    <section><h3>Activation actuelle</h3><ul class="admin-detail-list"><li><strong><?= admin_escape($device['activation_type'] ?: '-') ?></strong><span>Code <?= admin_escape($device['activation_code'] ?: '-') ?> - type <?= admin_escape($device['license_type'] ?: '-') ?></span><small>Duree <?= admin_escape((string) ($device['duration_days'] ?: '-')) ?> jours - playlist <?= (int) $device['playlist_configured'] === 1 ? 'configuree' : 'absente' ?></small></li></ul></section>
                </div>
            </div>
            <div class="admin-tab-panel" id="<?= admin_escape($modalId) ?>-tracking">
                <div class="admin-detail-grid">
                    <div><span>Evenements 30j</span><strong><?= number_format((int) ($behavior['total_events'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Jours actifs</span><strong><?= number_format((int) ($behavior['active_days'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Lectures</span><strong><?= number_format((int) ($behavior['playback_starts'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Termines</span><strong><?= number_format((int) ($behavior['playback_completed'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Favoris</span><strong><?= number_format((int) ($behavior['favorite_events'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Recherches</span><strong><?= number_format((int) ($behavior['searches'] ?? 0), 0, ',', ' ') ?></strong></div>
                    <div><span>Engagement</span><strong><?= number_format((float) ($behavior['avg_engagement'] ?? 0), 1, ',', ' ') ?> / 100</strong></div>
                    <div><span>Dernier event</span><strong><?= admin_escape((string) ($behavior['last_event_at'] ?? '-')) ?></strong></div>
                </div>
                <div class="admin-modal-columns">
                    <section><h3>Contenus consommes</h3><?= admin_render_behavior_breakdown($contentBreakdown) ?></section>
                    <section><h3>Categories detectees</h3><?= admin_render_behavior_breakdown($categoryBreakdown) ?></section>
                </div>
                <div class="behavior-insight-grid device-insight-grid">
                    <section><h3>Regions</h3><?= admin_render_behavior_breakdown($regionBreakdown) ?></section>
                    <section><h3>Pays</h3><?= admin_render_behavior_breakdown($countryBreakdown) ?></section>
                    <section><h3>Langues</h3><?= admin_render_behavior_breakdown($languageBreakdown) ?></section>
                    <section><h3>Interets</h3><?= admin_render_behavior_breakdown($interestBreakdown) ?></section>
                </div>
                <section class="device-tracking-events"><h3>Evenements recents</h3><div class="admin-table-wrap"><table><thead><tr><th>Date</th><th>Event</th><th>Contenu</th><th>Categorie</th><th>Media</th><th>Source</th><th>Score</th></tr></thead><tbody>
                    <?php if ($recentEvents === []): ?><tr><td colspan="7" class="admin-empty">Aucun tracking recu.</td></tr><?php endif; ?>
                    <?php foreach ($recentEvents as $event): ?><tr><td><?= admin_escape((string) ($event['created_at'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['event_type'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['content_type'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['category_label'] ?? $event['category_id'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['content_title'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['source_screen'] ?? '-')) ?></td><td><?= admin_escape((string) ($event['engagement_score'] ?? '-')) ?></td></tr><?php endforeach; ?>
                </tbody></table></div></section>
            </div>
            <div class="admin-tab-panel" id="<?= admin_escape($modalId) ?>-analysis">
                <div class="behavior-segment-grid">
                    <?php if ($segments === []): ?><p class="muted">Aucun segment comportemental calcule pour cet appareil.</p><?php endif; ?>
                    <?php foreach ($segments as $segment): ?><article class="behavior-segment-card">
                        <span><?= admin_escape((string) ($segment['segment_group'] ?? '-')) ?></span>
                        <strong><?= admin_escape((string) ($segment['segment_label'] ?? '-')) ?></strong>
                        <meter min="0" max="100" value="<?= (int) ($segment['score'] ?? 0) ?>"></meter>
                        <small>Score <?= (int) ($segment['score'] ?? 0) ?> - confiance <?= (int) ($segment['confidence'] ?? 0) ?></small>
                        <p><?= admin_escape((string) ($segment['evidence'] ?? '')) ?></p>
                    </article><?php endforeach; ?>
                </div>
            </div>
            <div class="admin-tab-panel" id="<?= admin_escape($modalId) ?>-diagnostics">
                <div class="admin-modal-columns single-row"><?= admin_render_device_diagnostics((array) ($device['diagnostics'] ?? []), $device) ?></div>
            </div>
            <div class="admin-tab-panel" id="<?= admin_escape($modalId) ?>-history">
                <div class="admin-modal-columns single-row">
                    <section><h3>Historique</h3><?php if (($device['history'] ?? []) === []): ?><p class="muted">Aucun historique.</p><?php endif; ?><ul class="admin-detail-list"><?php foreach (($device['history'] ?? []) as $history): ?><li><strong><?= admin_escape($history['activation_type'] ?: '-') ?> - <?= admin_escape($history['status'] ?: '-') ?></strong><span>Code <?= admin_escape($history['code_hint'] ?: '-') ?> / <?= admin_escape($history['license_type'] ?: '-') ?></span><small><?= admin_escape($history['starts_at'] ?: $history['created_at'] ?: '-') ?> -> <?= admin_escape($history['expires_at'] ?: '-') ?></small></li><?php endforeach; ?></ul></section>
                </div>
            </div>
        </section>
    </div><?php
}

function admin_render_behavior_breakdown(array $rows): string
{
    if ($rows === []) {
        return '<p class="muted">Aucune donnee.</p>';
    }
    ob_start();
    ?><ul class="behavior-breakdown-list compact"><?php foreach ($rows as $row): ?><li><span><?= admin_escape((string) ($row['label'] ?? 'UNKNOWN')) ?></span><strong><?= number_format((int) ($row['count'] ?? $row['events'] ?? 0), 0, ',', ' ') ?></strong><?php if (isset($row['devices'])): ?><small><?= number_format((int) $row['devices'], 0, ',', ' ') ?> appareil(s)</small><?php endif; ?></li><?php endforeach; ?></ul><?php
    return (string) ob_get_clean();
}

function admin_render_device_diagnostics(array $diagnostics, array $device): string
{
    $autostart = is_array($diagnostics['autostart']['payload'] ?? null) ? $diagnostics['autostart']['payload'] : [];
    $autoSync = is_array($diagnostics['auto_sync']['payload'] ?? null) ? $diagnostics['auto_sync']['payload'] : [];
    ob_start();
    ?>
    <section><h3>Diagnostics</h3><ul class="admin-detail-list">
        <li><strong>Autostart</strong><span>TV code <?= admin_escape((string) ($device['public_device_code'] ?: '-')) ?> / licence <?= admin_escape((string) ($device['license_type'] ?: $device['license_status'] ?: '-')) ?></span><small>Modele <?= admin_escape((string) ($diagnostics['autostart']['device_model'] ?? $diagnostics['auto_sync']['device_model'] ?? $device['device_name'] ?? 'Android TV')) ?> - Android <?= admin_escape((string) ($diagnostics['autostart']['android_version'] ?? $diagnostics['auto_sync']['android_version'] ?? '-')) ?> - APK <?= admin_escape((string) ($diagnostics['autostart']['app_version'] ?? $diagnostics['auto_sync']['app_version'] ?? $device['app_version'] ?? '-')) ?></small><small>Option <?= !empty($autostart['enabled']) ? 'activee' : 'desactivee' ?> - source <?= admin_escape((string) ($autostart['last_source'] ?? '-')) ?> - tentatives <?= admin_escape((string) ($autostart['attempts_this_boot'] ?? '-')) ?></small><small>Derniere tentative <?= admin_escape((string) ($diagnostics['autostart']['updated_at'] ?? '-')) ?> - boot <?= !empty($autostart['boot_completed']) ? 'termine' : 'en attente' ?> - erreur <?= admin_escape((string) ($autostart['last_error'] ?? '-')) ?></small></li>
        <li><strong>Auto sync</strong><span>Option <?= !empty($autoSync['enabled']) ? 'activee' : 'desactivee' ?> - source <?= admin_escape((string) ($autoSync['last_source'] ?? '-')) ?> - resultat <?= admin_escape((string) ($autoSync['last_result'] ?? '-')) ?></span><small>Derniere sync <?= admin_escape((string) ($diagnostics['auto_sync']['updated_at'] ?? '-')) ?> - duree <?= admin_escape((string) ($autoSync['last_duration_ms'] ?? '-')) ?> ms - taille <?= isset($autoSync['last_size_kb']) ? admin_escape((string) $autoSync['last_size_kb']) . ' KB' : 'N/D' ?></small><small>Erreur <?= admin_escape((string) ($autoSync['last_error'] ?? '-')) ?></small></li>
    </ul></section>
    <?php
    return (string) ob_get_clean();
}
