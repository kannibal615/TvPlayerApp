<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

function sv_mail_ensure_schema(PDO $pdo): void
{
    static $ensured = [];
    $connectionId = spl_object_id($pdo);
    if (isset($ensured[$connectionId])) {
        return;
    }

    $column = $pdo->query("SHOW COLUMNS FROM site_users LIKE 'email_verified_at'")->fetch();
    if (!is_array($column)) {
        $pdo->exec('ALTER TABLE site_users ADD COLUMN email_verified_at DATETIME NULL AFTER status');
        $pdo->exec('UPDATE site_users SET email_verified_at = COALESCE(created_at, NOW()) WHERE email_verified_at IS NULL');
    }

    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS email_templates (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            template_key VARCHAR(96) NOT NULL,
            locale VARCHAR(32) NOT NULL DEFAULT 'fr-FR',
            name VARCHAR(255) NOT NULL,
            category VARCHAR(64) NOT NULL,
            subject_template VARCHAR(255) NOT NULL,
            title_template VARCHAR(255) NOT NULL,
            intro_html TEXT NULL,
            body_html MEDIUMTEXT NULL,
            button_label_template VARCHAR(255) NULL,
            button_url_variable VARCHAR(96) NULL,
            footer_html TEXT NULL,
            variables_json JSON NULL,
            is_system TINYINT(1) NOT NULL DEFAULT 1,
            is_active TINYINT(1) NOT NULL DEFAULT 1,
            sort_order INT NOT NULL DEFAULT 100,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY email_templates_key_locale (template_key, locale)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS email_logs (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            email_type VARCHAR(64) NOT NULL,
            recipient_email VARCHAR(255) NOT NULL,
            subject VARCHAR(255) NULL,
            status VARCHAR(64) NOT NULL DEFAULT 'pending',
            error_message TEXT NULL,
            provider VARCHAR(64) NULL,
            template_key VARCHAR(96) NULL,
            html_snapshot MEDIUMTEXT NULL,
            text_snapshot MEDIUMTEXT NULL,
            payload_json JSON NULL,
            from_email VARCHAR(255) NULL,
            reply_to VARCHAR(255) NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX email_logs_type_status_idx (email_type, status),
            INDEX email_logs_provider_idx (provider),
            INDEX email_logs_recipient_idx (recipient_email),
            INDEX email_logs_created_idx (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS email_verification_tokens (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            token_hash CHAR(64) NOT NULL UNIQUE,
            expires_at DATETIME NOT NULL,
            used_at DATETIME NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX email_verification_user_idx (user_id, used_at, expires_at),
            CONSTRAINT fk_email_verification_user
                FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    foreach (sv_mail_default_templates() as $template) {
        $statement = $pdo->prepare(
            "INSERT IGNORE INTO email_templates
                (template_key, locale, name, category, subject_template, title_template,
                 intro_html, body_html, button_label_template, button_url_variable,
                 footer_html, variables_json, is_system, is_active, sort_order)
             VALUES
                (:template_key, 'fr-FR', :name, :category, :subject_template, :title_template,
                 :intro_html, :body_html, :button_label_template, :button_url_variable,
                 :footer_html, :variables_json, 1, 1, :sort_order)"
        );
        $statement->execute($template);
    }
    $ensured[$connectionId] = true;
}

function sv_mail_default_templates(): array
{
    return [
        [
            'template_key' => 'verify_email',
            'name' => 'Vérification du compte',
            'category' => 'system',
            'subject_template' => 'Confirmez votre adresse email SmartVision',
            'title_template' => 'Bienvenue {{customer.name}}',
            'intro_html' => '<p>Confirmez votre adresse email pour sécuriser votre compte SmartVision.</p>',
            'body_html' => '<p>Ce lien est valable pendant 24 heures.</p>',
            'button_label_template' => 'Confirmer mon email',
            'button_url_variable' => 'verify_url',
            'footer_html' => '<p>Si vous n’êtes pas à l’origine de cette inscription, ignorez cet email.</p>',
            'variables_json' => json_encode(['customer.name', 'customer.email', 'verify_url']),
            'sort_order' => 10,
        ],
        [
            'template_key' => 'registration_thanks',
            'name' => 'Compte vérifié',
            'category' => 'system',
            'subject_template' => 'Votre compte SmartVision est confirmé',
            'title_template' => 'Compte confirmé',
            'intro_html' => '<p>Votre adresse email est maintenant vérifiée.</p>',
            'body_html' => '<p>Vous pouvez acheter une licence et gérer vos appareils depuis votre espace client.</p>',
            'button_label_template' => 'Accéder à mon compte',
            'button_url_variable' => 'account_url',
            'footer_html' => '<p>SmartVision est un lecteur Android TV sans contenu inclus.</p>',
            'variables_json' => json_encode(['customer.name', 'account_url']),
            'sort_order' => 20,
        ],
        [
            'template_key' => 'order_confirmed',
            'name' => 'Commande confirmée',
            'category' => 'transactional',
            'subject_template' => 'Commande {{order.reference}} confirmée',
            'title_template' => 'Merci pour votre commande',
            'intro_html' => '<p>Votre commande <strong>{{order.reference}}</strong> est confirmée.</p>',
            'body_html' => '<p>Offre : {{order.plan}}<br>Montant : {{order.amount}}</p>{{{license_block}}}',
            'button_label_template' => 'Voir mes licences',
            'button_url_variable' => 'account_url',
            'footer_html' => '<p>Conservez votre code licence et ne le partagez pas publiquement.</p>',
            'variables_json' => json_encode(['order.reference', 'order.plan', 'order.amount', 'license_block', 'account_url']),
            'sort_order' => 30,
        ],
        [
            'template_key' => 'payment_received',
            'name' => 'Paiement reçu en vérification',
            'category' => 'transactional',
            'subject_template' => 'Paiement SmartVision reçu',
            'title_template' => 'Paiement reçu',
            'intro_html' => '<p>Nous avons reçu le retour de paiement pour {{order.plan}}.</p>',
            'body_html' => '<p>Transaction : {{payment.txn}}<br>Le paiement est en cours de vérification sécurisée.</p>',
            'button_label_template' => 'Voir mes commandes',
            'button_url_variable' => 'orders_url',
            'footer_html' => '<p>Ne relancez pas le paiement si votre banque a déjà confirmé le débit.</p>',
            'variables_json' => json_encode(['order.plan', 'payment.txn', 'orders_url']),
            'sort_order' => 40,
        ],
        [
            'template_key' => 'admin_notification_account_created',
            'name' => 'Admin — nouveau compte',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Nouveau compte client',
            'title_template' => 'Nouveau compte client',
            'intro_html' => '<p>Un nouveau compte a été créé.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les clients',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['admin_event_table', 'admin_url']),
            'sort_order' => 100,
        ],
        [
            'template_key' => 'admin_notification_order_created',
            'name' => 'Admin — nouvelle commande',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Nouvelle commande {{order.reference}}',
            'title_template' => 'Nouvelle commande',
            'intro_html' => '<p>Une commande vient d’être créée.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les commandes',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['order.reference', 'admin_event_table', 'admin_url']),
            'sort_order' => 110,
        ],
        [
            'template_key' => 'admin_notification_payment_review',
            'name' => 'Admin — paiement à vérifier',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Paiement Gammal à vérifier',
            'title_template' => 'Paiement en attente de validation',
            'intro_html' => '<p>Un retour Gammal a été reçu et doit être vérifié avant livraison.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les paiements',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['admin_event_table', 'admin_url']),
            'sort_order' => 120,
        ],
        [
            'template_key' => 'admin_notification_generic',
            'name' => 'Admin — notification générique',
            'category' => 'admin',
            'subject_template' => '[SmartVision] {{event.title}}',
            'title_template' => '{{event.title}}',
            'intro_html' => '<p>{{event.message}}</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir l’administration',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['event.title', 'event.message', 'admin_event_table', 'admin_url']),
            'sort_order' => 130,
        ],
    ];
}

function sv_mail_config(PDO $pdo): array
{
    $private = load_database_config();
    return [
        'external_enabled' => (string) get_setting($pdo, 'external_services_enabled', '0') === '1',
        'smtp_enabled' => (string) get_setting($pdo, 'smtp_enabled', '0') === '1',
        'host' => trim((string) get_setting($pdo, 'smtp_host', 'nls.hostcreed.com')),
        'port' => max(1, min(65535, (int) get_setting($pdo, 'smtp_port', '465'))),
        'secure' => strtolower(trim((string) get_setting($pdo, 'smtp_secure', 'ssl'))),
        'username' => trim((string) get_setting($pdo, 'smtp_user', '')),
        'password' => (string) ($private['smtp_password'] ?? ''),
        'from_email' => trim((string) get_setting($pdo, 'smtp_from_email', '')),
        'from_name' => trim((string) get_setting($pdo, 'smtp_from_name', 'SmartVision')),
        'reply_to' => trim((string) get_setting($pdo, 'smtp_reply_to', '')),
        'admin_email' => trim((string) get_setting($pdo, 'admin_notification_email', '')),
    ];
}

function sv_mail_payload_value(array $payload, string $path): mixed
{
    $value = $payload;
    foreach (explode('.', $path) as $part) {
        if (!is_array($value) || !array_key_exists($part, $value)) {
            return '';
        }
        $value = $value[$part];
    }
    return is_scalar($value) ? $value : '';
}

function sv_mail_render_string(string $template, array $payload, array $safeBlocks = []): string
{
    $rendered = preg_replace_callback(
        '/\{\{\{([a-zA-Z0-9_.-]+)\}\}\}/',
        static fn(array $match): string => (string) ($safeBlocks[$match[1]] ?? ''),
        $template,
    );
    return preg_replace_callback(
        '/\{\{([a-zA-Z0-9_.-]+)\}\}/',
        static fn(array $match): string => htmlspecialchars(
            (string) sv_mail_payload_value($payload, $match[1]),
            ENT_QUOTES | ENT_SUBSTITUTE,
            'UTF-8',
        ),
        (string) $rendered,
    );
}

function sv_mail_safe_blocks(array $payload): array
{
    $blocks = [];
    if (is_array($payload['license'] ?? null)) {
        $license = $payload['license'];
        $blocks['license_block'] = '<div style="margin:20px 0;padding:18px;border:1px solid #2d6ca8;border-radius:10px;background:#071b31">'
            . '<strong style="display:block;color:#ffffff">Code licence</strong>'
            . '<div style="margin-top:8px;color:#8ceeff;font-size:22px;font-weight:800;letter-spacing:1px">'
            . htmlspecialchars((string) ($license['code'] ?? ''), ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
            . '</div></div>';
    }
    if (is_array($payload['admin_event'] ?? null)) {
        $rows = '';
        foreach ($payload['admin_event'] as $label => $value) {
            $rows .= '<tr><td style="padding:8px;border-bottom:1px solid #263a53;color:#94a6bd">'
                . htmlspecialchars((string) $label, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
                . '</td><td style="padding:8px;border-bottom:1px solid #263a53;color:#ffffff">'
                . htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
                . '</td></tr>';
        }
        $blocks['admin_event_table'] = '<table style="width:100%;border-collapse:collapse">' . $rows . '</table>';
    }
    return $blocks;
}

function sv_mail_build(PDO $pdo, string $type, array $payload): array
{
    $aliases = [
        'order' => 'order_confirmed',
        'trial' => 'trial_active',
        'reset_password' => 'password_recovery',
    ];
    $templateKey = $aliases[$type] ?? $type;
    $statement = $pdo->prepare(
        "SELECT * FROM email_templates
         WHERE template_key = :template_key AND locale = 'fr-FR' LIMIT 1"
    );
    $statement->execute(['template_key' => $templateKey]);
    $template = $statement->fetch();
    if (!is_array($template)) {
        throw new RuntimeException('Template email indisponible: ' . $templateKey);
    }
    if ((int) $template['is_active'] !== 1) {
        return ['status' => 'skipped', 'template_key' => $templateKey];
    }

    $safeBlocks = sv_mail_safe_blocks($payload);
    $subject = html_entity_decode(strip_tags(sv_mail_render_string((string) $template['subject_template'], $payload)), ENT_QUOTES, 'UTF-8');
    $title = sv_mail_render_string((string) $template['title_template'], $payload, $safeBlocks);
    $intro = sv_mail_render_string((string) ($template['intro_html'] ?? ''), $payload, $safeBlocks);
    $body = sv_mail_render_string((string) ($template['body_html'] ?? ''), $payload, $safeBlocks);
    $footer = sv_mail_render_string((string) ($template['footer_html'] ?? ''), $payload, $safeBlocks);
    $buttonUrl = '';
    $buttonVariable = trim((string) ($template['button_url_variable'] ?? ''));
    if ($buttonVariable !== '') {
        $buttonUrl = trim((string) sv_mail_payload_value($payload, $buttonVariable));
    }
    $buttonLabel = sv_mail_render_string((string) ($template['button_label_template'] ?? ''), $payload, $safeBlocks);
    $button = '';
    $fallback = '';
    if ($buttonUrl !== '' && filter_var($buttonUrl, FILTER_VALIDATE_URL)) {
        $escapedUrl = htmlspecialchars($buttonUrl, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $button = '<p style="margin:28px 0"><a href="' . $escapedUrl . '" style="display:inline-block;padding:14px 22px;border-radius:8px;background:#1677ff;color:#fff;text-decoration:none;font-weight:800">'
            . $buttonLabel . '</a></p>';
        $fallback = '<p style="font-size:12px;color:#7f90a8">Lien de secours : <a href="' . $escapedUrl . '" style="color:#55b8ff">' . $escapedUrl . '</a></p>';
    }
    $html = '<!doctype html><html><body style="margin:0;background:#020611;color:#f7f9fd;font-family:Arial,sans-serif">'
        . '<div style="max-width:640px;margin:0 auto;padding:32px 18px"><div style="padding:28px;border:1px solid #223651;border-radius:12px;background:#081423">'
        . '<h1 style="margin:0 0 18px;font-size:28px;color:#ffffff">' . $title . '</h1>'
        . '<div style="color:#b6c3d5;line-height:1.65">' . $intro . $body . $button . $fallback . $footer . '</div>'
        . '</div><p style="color:#65758a;font-size:12px;text-align:center">SmartVision — lecteur Android TV sans contenu inclus.</p></div>'
        . '</body></html>';
    $text = trim(html_entity_decode(strip_tags(preg_replace('/<br\s*\/?>/i', "\n", $html)), ENT_QUOTES, 'UTF-8'));

    return [
        'status' => 'ready',
        'template_key' => $templateKey,
        'subject' => $subject,
        'html' => $html,
        'text' => $text,
    ];
}

function sv_mail_mask_payload(mixed $value, string $key = ''): mixed
{
    if (preg_match('/pass|password|token|secret|private|api_key|activation_code|license_code|^code$/i', $key)) {
        return '[masked]';
    }
    if (!is_array($value)) {
        return $value;
    }
    $masked = [];
    foreach ($value as $childKey => $childValue) {
        $masked[$childKey] = sv_mail_mask_payload($childValue, (string) $childKey);
    }
    return $masked;
}

function sv_send_email(?PDO $pdo, string $type, string $recipient, array $payload = []): string
{
    $pdo ??= db();
    $recipient = strtolower(trim($recipient));
    $config = ['from_email' => '', 'reply_to' => ''];
    $built = ['template_key' => $type, 'subject' => '', 'html' => '', 'text' => ''];
    try {
        sv_mail_ensure_schema($pdo);
        $config = sv_mail_config($pdo);
        $built = sv_mail_build($pdo, $type, $payload);
        if (($built['status'] ?? '') === 'skipped') {
            sv_mail_log($pdo, $type, $recipient, $built, 'skipped', null, $config, $payload);
            return 'skipped';
        }

        $status = 'pending';
        $error = null;
        if (!filter_var($recipient, FILTER_VALIDATE_EMAIL)) {
            $status = 'error';
            $error = 'Destinataire invalide.';
        } elseif (
            !$config['external_enabled']
            || !$config['smtp_enabled']
            || $config['host'] === ''
            || $config['username'] === ''
            || $config['password'] === ''
            || !filter_var($config['from_email'], FILTER_VALIDATE_EMAIL)
        ) {
            $status = 'pending';
            $error = 'Transport SMTP désactivé ou incomplet.';
        } else {
            sv_smtp_send($config, $recipient, (string) $built['subject'], (string) $built['html'], (string) $built['text']);
            $status = 'sent';
        }

        sv_mail_log($pdo, $type, $recipient, $built, $status, $error, $config, $payload);
        return $status;
    } catch (Throwable $exception) {
        error_log('SmartVision email delivery failed: ' . smartvision_text_substr($exception->getMessage(), 0, 300));
        try {
            sv_mail_log(
                $pdo,
                $type,
                $recipient,
                $built,
                'error',
                smartvision_text_substr($exception->getMessage(), 0, 1000),
                $config,
                $payload,
            );
        } catch (Throwable) {
            // Le journal email ne doit jamais interrompre le parcours métier.
        }
        return 'error';
    }
}

function sv_send_admin_notification(PDO $pdo, string $templateKey, array $details, string $adminPath = '/admin/'): string
{
    try {
        $config = sv_mail_config($pdo);
    } catch (Throwable) {
        error_log('SmartVision admin email configuration failed.');
        return 'error';
    }
    if (!filter_var($config['admin_email'], FILTER_VALIDATE_EMAIL)) {
        return sv_send_email($pdo, $templateKey, '', [
            'event' => ['title' => 'Notification administrateur', 'message' => ''],
            'admin_event' => $details,
            'admin_url' => smartvision_public_base_url() . $adminPath,
        ]);
    }
    return sv_send_email($pdo, $templateKey, $config['admin_email'], [
        'event' => ['title' => 'Notification administrateur', 'message' => ''],
        'order' => ['reference' => (string) ($details['Commande'] ?? '')],
        'admin_event' => $details,
        'admin_url' => smartvision_public_base_url() . $adminPath,
    ]);
}

function sv_mail_log(
    PDO $pdo,
    string $type,
    string $recipient,
    array $built,
    string $status,
    ?string $error,
    array $config,
    array $payload,
): void {
    $htmlSnapshot = (string) ($built['html'] ?? '');
    $textSnapshot = (string) ($built['text'] ?? '');
    $licenseCode = is_array($payload['license'] ?? null)
        ? trim((string) ($payload['license']['code'] ?? ''))
        : '';
    if ($licenseCode !== '') {
        $htmlSnapshot = str_replace(
            [htmlspecialchars($licenseCode, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'), $licenseCode],
            '[masked]',
            $htmlSnapshot,
        );
        $textSnapshot = str_replace($licenseCode, '[masked]', $textSnapshot);
    }
    $htmlSnapshot = preg_replace('/([?&](?:token|intent)=)[a-f0-9]{32,128}/i', '$1[masked]', $htmlSnapshot);
    $textSnapshot = preg_replace('/([?&](?:token|intent)=)[a-f0-9]{32,128}/i', '$1[masked]', $textSnapshot);
    $statement = $pdo->prepare(
        "INSERT INTO email_logs
            (email_type, recipient_email, subject, status, error_message, provider,
             template_key, html_snapshot, text_snapshot, payload_json, from_email, reply_to)
         VALUES
            (:email_type, :recipient_email, :subject, :status, :error_message, 'smtp',
             :template_key, :html_snapshot, :text_snapshot, :payload_json, :from_email, :reply_to)"
    );
    $statement->execute([
        'email_type' => $type,
        'recipient_email' => $recipient,
        'subject' => (string) ($built['subject'] ?? ''),
        'status' => $status,
        'error_message' => $error,
        'template_key' => (string) ($built['template_key'] ?? $type),
        'html_snapshot' => $htmlSnapshot,
        'text_snapshot' => $textSnapshot,
        'payload_json' => json_encode(sv_mail_mask_payload($payload), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        'from_email' => (string) ($config['from_email'] ?? ''),
        'reply_to' => (string) ($config['reply_to'] ?? ''),
    ]);
}

function sv_smtp_send(array $config, string $recipient, string $subject, string $html, string $text): void
{
    $secure = $config['secure'] === 'tls' ? 'tls' : 'ssl';
    $target = ($secure === 'ssl' ? 'ssl://' : '') . $config['host'] . ':' . $config['port'];
    $socket = @stream_socket_client($target, $errno, $errstr, 20, STREAM_CLIENT_CONNECT);
    if (!is_resource($socket)) {
        throw new RuntimeException('Connexion SMTP impossible: ' . $errstr);
    }
    stream_set_timeout($socket, 20);
    try {
        sv_smtp_expect($socket, [220]);
        sv_smtp_command($socket, 'EHLO ' . $config['host'], [250]);
        if ($secure === 'tls') {
            sv_smtp_command($socket, 'STARTTLS', [220]);
            if (!stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT)) {
                throw new RuntimeException('Activation STARTTLS impossible.');
            }
            sv_smtp_command($socket, 'EHLO ' . $config['host'], [250]);
        }
        sv_smtp_command($socket, 'AUTH LOGIN', [334]);
        sv_smtp_command($socket, base64_encode($config['username']), [334]);
        sv_smtp_command($socket, base64_encode($config['password']), [235]);
        sv_smtp_command($socket, 'MAIL FROM:<' . $config['from_email'] . '>', [250]);
        sv_smtp_command($socket, 'RCPT TO:<' . $recipient . '>', [250, 251]);
        sv_smtp_command($socket, 'DATA', [354]);

        $boundary = 'sv_' . bin2hex(random_bytes(12));
        $fromName = mb_encode_mimeheader($config['from_name'], 'UTF-8', 'B');
        $messageIdDomain = substr(strrchr($config['from_email'], '@') ?: '@smartvisions.net', 1);
        $headers = [
            'From: ' . $fromName . ' <' . $config['from_email'] . '>',
            'Reply-To: ' . ($config['reply_to'] ?: $config['from_email']),
            'To: <' . $recipient . '>',
            'Subject: ' . mb_encode_mimeheader($subject, 'UTF-8', 'B'),
            'Date: ' . date(DATE_RFC2822),
            'Message-ID: <' . bin2hex(random_bytes(16)) . '@' . $messageIdDomain . '>',
            'MIME-Version: 1.0',
            'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
            'X-Mailer: SmartVision Mail Service',
        ];
        $body = implode("\r\n", $headers) . "\r\n\r\n"
            . '--' . $boundary . "\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n"
            . quoted_printable_encode($text) . "\r\n"
            . '--' . $boundary . "\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n"
            . quoted_printable_encode($html) . "\r\n"
            . '--' . $boundary . "--\r\n";
        $body = preg_replace('/^\./m', '..', $body);
        fwrite($socket, $body . "\r\n.\r\n");
        sv_smtp_expect($socket, [250]);
        sv_smtp_command($socket, 'QUIT', [221]);
    } finally {
        fclose($socket);
    }
}

function sv_smtp_command($socket, string $command, array $expectedCodes): string
{
    fwrite($socket, $command . "\r\n");
    return sv_smtp_expect($socket, $expectedCodes);
}

function sv_smtp_expect($socket, array $expectedCodes): string
{
    $response = '';
    while (($line = fgets($socket, 2048)) !== false) {
        $response .= $line;
        if (strlen($line) < 4 || $line[3] === ' ') {
            break;
        }
    }
    $code = (int) substr($response, 0, 3);
    if (!in_array($code, $expectedCodes, true)) {
        throw new RuntimeException('Réponse SMTP inattendue: ' . trim($response));
    }
    return $response;
}

function sv_create_email_verification(PDO $pdo, int $userId, string $email, string $displayName): string
{
    sv_mail_ensure_schema($pdo);
    $pdo->prepare('UPDATE email_verification_tokens SET used_at = NOW() WHERE user_id = :user_id AND used_at IS NULL')
        ->execute(['user_id' => $userId]);
    $token = bin2hex(random_bytes(32));
    $pdo->prepare(
        "INSERT INTO email_verification_tokens (user_id, token_hash, expires_at)
         VALUES (:user_id, :token_hash, DATE_ADD(NOW(), INTERVAL 24 HOUR))"
    )->execute([
        'user_id' => $userId,
        'token_hash' => hash('sha256', $token),
    ]);
    $verifyUrl = smartvision_public_base_url() . '/verify-email/?token=' . rawurlencode($token);
    sv_send_email($pdo, 'verify_email', $email, [
        'customer' => ['name' => $displayName !== '' ? $displayName : $email, 'email' => $email],
        'verify_url' => $verifyUrl,
    ]);
    return $token;
}
