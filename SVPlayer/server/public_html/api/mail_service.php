<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

const SV_MAIL_TEMPLATE_DESIGN_VERSION = '2026-06-email-brand-v2';

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

    sv_mail_seed_templates($pdo);
    $ensured[$connectionId] = true;
}

function sv_mail_seed_templates(PDO $pdo): void
{
    $currentVersion = '';
    try {
        $currentVersion = (string) get_setting($pdo, 'email_template_design_version', '');
    } catch (Throwable) {
        $currentVersion = '';
    }
    $shouldRefreshSystemTemplates = $currentVersion !== SV_MAIL_TEMPLATE_DESIGN_VERSION;

    foreach (sv_mail_brand_templates() as $template) {
        $sql = $shouldRefreshSystemTemplates
            ? "INSERT INTO email_templates
                (template_key, locale, name, category, subject_template, title_template,
                 intro_html, body_html, button_label_template, button_url_variable,
                 footer_html, variables_json, is_system, is_active, sort_order)
             VALUES
                (:template_key, 'fr-FR', :name, :category, :subject_template, :title_template,
                 :intro_html, :body_html, :button_label_template, :button_url_variable,
                 :footer_html, :variables_json, 1, 1, :sort_order)
             ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                category = VALUES(category),
                subject_template = VALUES(subject_template),
                title_template = VALUES(title_template),
                intro_html = VALUES(intro_html),
                body_html = VALUES(body_html),
                button_label_template = VALUES(button_label_template),
                button_url_variable = VALUES(button_url_variable),
                footer_html = VALUES(footer_html),
                variables_json = VALUES(variables_json),
                is_system = 1,
                is_active = 1,
                sort_order = VALUES(sort_order)"
            : "INSERT IGNORE INTO email_templates
                (template_key, locale, name, category, subject_template, title_template,
                 intro_html, body_html, button_label_template, button_url_variable,
                 footer_html, variables_json, is_system, is_active, sort_order)
             VALUES
                (:template_key, 'fr-FR', :name, :category, :subject_template, :title_template,
                 :intro_html, :body_html, :button_label_template, :button_url_variable,
                 :footer_html, :variables_json, 1, 1, :sort_order)";
        $statement = $pdo->prepare(
            $sql
        );
        $statement->execute($template);
    }

    if ($shouldRefreshSystemTemplates) {
        $statement = $pdo->prepare(
            "INSERT INTO app_settings (setting_key, setting_value)
             VALUES ('email_template_design_version', :version)
             ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
        );
        $statement->execute(['version' => SV_MAIL_TEMPLATE_DESIGN_VERSION]);
    }
}

function sv_mail_default_templates(): array
{
    return [
        [
            'template_key' => 'verify_email',
            'name' => 'Vérification du compte',
            'category' => 'system',
            'subject_template' => 'Confirmez votre adresse email SmartVision',
            'title_template' => 'Confirmez votre adresse email',
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

function sv_mail_brand_templates(): array
{
    return [
        [
            'template_key' => 'verify_email',
            'name' => 'Compte client',
            'category' => 'account',
            'subject_template' => 'Confirmez votre adresse email SmartVision',
            'title_template' => 'Confirmez votre adresse email',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Merci pour votre inscription sur <strong>SmartVision</strong>.</p><p>Cliquez sur le bouton ci-dessous pour confirmer votre adresse email et activer votre espace client.</p>',
            'button_label_template' => 'Confirmer mon email',
            'button_url_variable' => 'verify_url',
            'footer_html' => '<p>Ce lien est valable pendant 24 heures. Si vous n’êtes pas à l’origine de cette inscription, vous pouvez ignorer cet email.</p>',
            'variables_json' => json_encode(['customer.name', 'customer.email', 'verify_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 10,
        ],
        [
            'template_key' => 'registration_thanks',
            'name' => 'Remerciement inscription',
            'category' => 'account',
            'subject_template' => 'Bienvenue chez SmartVision',
            'title_template' => 'Bienvenue chez SmartVision',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Votre compte client est maintenant prêt.</p><p>Depuis votre espace client, vous pouvez suivre vos commandes, récupérer vos licences et gérer vos appareils Android TV.</p>',
            'button_label_template' => 'Accéder à mon espace client',
            'button_url_variable' => 'account_url',
            'footer_html' => '<p>SmartVision est un lecteur IPTV Android TV. Aucun contenu TV, playlist ou abonnement tiers n’est inclus.</p>',
            'variables_json' => json_encode(['customer.name', 'account_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 20,
        ],
        [
            'template_key' => 'password_recovery',
            'name' => 'Recuperation mot de passe',
            'category' => 'account',
            'subject_template' => 'Réinitialisation de votre mot de passe SmartVision',
            'title_template' => 'Réinitialisez votre mot de passe',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Nous avons reçu une demande de réinitialisation du mot de passe de votre compte SmartVision.</p><p>Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe sécurisé.</p>',
            'button_label_template' => 'Réinitialiser mon mot de passe',
            'button_url_variable' => 'reset_url',
            'footer_html' => '<p>Si vous n’avez pas demandé cette action, ignorez cet email. Votre mot de passe actuel reste inchangé.</p>',
            'variables_json' => json_encode(['customer.name', 'customer.email', 'reset_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 30,
        ],
        [
            'template_key' => 'trial_active',
            'name' => 'Test gratuit actif',
            'category' => 'trial',
            'subject_template' => 'Votre test gratuit SmartVision 24h est prêt',
            'title_template' => 'Votre test gratuit est actif',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Votre test gratuit SmartVision est maintenant actif sur votre appareil.</p><p>Vous pouvez lancer l’application Android TV, configurer votre playlist Xtream et tester l’expérience SmartVision pendant la période d’essai.</p><p><strong>Expiration :</strong> {{trial.expires_at}}</p>',
            'button_label_template' => 'Ouvrir les instructions',
            'button_url_variable' => 'setup_url',
            'footer_html' => '<p>Après l’essai, vous pourrez activer une licence complète depuis votre espace client.</p>',
            'variables_json' => json_encode(['customer.name', 'trial.expires_at', 'device.public_code', 'setup_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 40,
        ],
        [
            'template_key' => 'trial_expiring_soon',
            'name' => 'Test gratuit bientot expire',
            'category' => 'trial',
            'subject_template' => 'Votre test gratuit SmartVision expire bientôt',
            'title_template' => 'Votre test gratuit expire bientôt',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Votre période d’essai SmartVision arrive bientôt à expiration.</p><p>Pour continuer à utiliser l’application sans interruption, choisissez une formule et activez votre licence depuis votre espace client.</p><p><strong>Fin de l’essai :</strong> {{trial.expires_at}}</p>',
            'button_label_template' => 'Choisir ma licence',
            'button_url_variable' => 'plans_url',
            'footer_html' => '<p>Votre playlist et vos réglages restent liés à votre appareil.</p>',
            'variables_json' => json_encode(['customer.name', 'trial.expires_at', 'plans_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 50,
        ],
        [
            'template_key' => 'trial_expired',
            'name' => 'Test gratuit expire',
            'category' => 'trial',
            'subject_template' => 'Votre test gratuit SmartVision est terminé',
            'title_template' => 'Votre test gratuit est terminé',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Votre test gratuit SmartVision est arrivé à expiration.</p><p>Vous pouvez continuer à utiliser l’application en activant une licence depuis votre espace client.</p>',
            'button_label_template' => 'Activer une licence',
            'button_url_variable' => 'plans_url',
            'footer_html' => '<p>Besoin d’aide ? Répondez à cet email ou contactez le support SmartVision.</p>',
            'variables_json' => json_encode(['customer.name', 'plans_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 60,
        ],
        [
            'template_key' => 'order_confirmed',
            'name' => 'Achat licence confirme',
            'category' => 'order',
            'subject_template' => 'Votre licence SmartVision IPTV Player est confirmée',
            'title_template' => 'Votre licence IPTV Player est confirmée',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Merci pour votre achat. Votre commande <strong>{{order.reference}}</strong> est confirmée.</p><p><strong>Offre :</strong> {{order.plan}}<br><strong>Montant :</strong> {{order.amount}}</p>{{{license_block}}}<p>Conservez votre code licence. Il permet d’activer SmartVision sur votre appareil.</p>',
            'button_label_template' => 'Voir ma licence',
            'button_url_variable' => 'account_url',
            'footer_html' => '<p>Ne partagez jamais votre code licence publiquement.</p>',
            'variables_json' => json_encode(['customer.name', 'order.reference', 'order.plan', 'order.amount', 'license_block', 'account_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 70,
        ],
        [
            'template_key' => 'cart_reminder',
            'name' => 'Rappel panier',
            'category' => 'marketing',
            'subject_template' => 'Votre licence SmartVision IPTV Player vous attend',
            'title_template' => 'Votre panier SmartVision est toujours disponible',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Vous avez commencé une commande SmartVision mais elle n’est pas encore finalisée.</p><p>Reprenez votre achat quand vous êtes prêt pour activer votre licence Android TV.</p><p><strong>Offre sélectionnée :</strong> {{order.plan}}</p>',
            'button_label_template' => 'Finaliser ma commande',
            'button_url_variable' => 'cart_url',
            'footer_html' => '<p>Si vous avez déjà finalisé votre achat, vous pouvez ignorer ce message.</p>',
            'variables_json' => json_encode(['customer.name', 'order.plan', 'cart_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 80,
        ],
        [
            'template_key' => 'inactive_customer_reminder',
            'name' => 'Rappel client inactif',
            'category' => 'marketing',
            'subject_template' => 'Votre compte SmartVision est toujours disponible',
            'title_template' => 'Reprenez SmartVision sur votre Android TV',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Nous avons remarqué que votre compte SmartVision n’a pas été utilisé récemment.</p><p>Votre espace client reste disponible pour retrouver vos informations, gérer vos appareils ou choisir une nouvelle licence.</p>',
            'button_label_template' => 'Ouvrir mon espace client',
            'button_url_variable' => 'account_url',
            'footer_html' => '<p>SmartVision reste un lecteur Android TV indépendant, sans contenu TV inclus.</p>',
            'variables_json' => json_encode(['customer.name', 'account_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 90,
        ],
        [
            'template_key' => 'payment_received',
            'name' => 'Paiement recu en verification',
            'category' => 'order',
            'subject_template' => 'Paiement SmartVision reçu',
            'title_template' => 'Paiement reçu',
            'intro_html' => '<p>Bonjour {{customer.name}},</p>',
            'body_html' => '<p>Nous avons reçu le retour de paiement pour {{order.plan}}.</p><p><strong>Transaction :</strong> {{payment.txn}}<br>Le paiement est en cours de vérification sécurisée.</p>',
            'button_label_template' => 'Voir mes commandes',
            'button_url_variable' => 'orders_url',
            'footer_html' => '<p>Ne relancez pas le paiement si votre banque a déjà confirmé le débit.</p>',
            'variables_json' => json_encode(['customer.name', 'order.plan', 'payment.txn', 'orders_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 100,
        ],
        [
            'template_key' => 'admin_notification_account_created',
            'name' => 'Admin - nouveau compte',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Nouveau compte client',
            'title_template' => 'Nouveau compte client',
            'intro_html' => '<p>Un nouveau compte a été créé.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les clients',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['admin_event_table', 'admin_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 110,
        ],
        [
            'template_key' => 'admin_notification_order_created',
            'name' => 'Admin - nouvelle commande',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Nouvelle commande {{order.reference}}',
            'title_template' => 'Nouvelle commande',
            'intro_html' => '<p>Une commande vient d’être créée.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les commandes',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['order.reference', 'admin_event_table', 'admin_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 120,
        ],
        [
            'template_key' => 'admin_notification_payment_review',
            'name' => 'Admin - paiement a verifier',
            'category' => 'admin',
            'subject_template' => '[SmartVision] Paiement Gammal à vérifier',
            'title_template' => 'Paiement en attente de validation',
            'intro_html' => '<p>Un retour Gammal a été reçu et doit être vérifié avant livraison.</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir les paiements',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['admin_event_table', 'admin_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 130,
        ],
        [
            'template_key' => 'admin_notification_generic',
            'name' => 'Admin - notification generique',
            'category' => 'admin',
            'subject_template' => '[SmartVision] {{event.title}}',
            'title_template' => '{{event.title}}',
            'intro_html' => '<p>{{event.message}}</p>',
            'body_html' => '{{{admin_event_table}}}',
            'button_label_template' => 'Ouvrir l’administration',
            'button_url_variable' => 'admin_url',
            'footer_html' => '',
            'variables_json' => json_encode(['event.title', 'event.message', 'admin_event_table', 'admin_url'], JSON_UNESCAPED_UNICODE),
            'sort_order' => 140,
        ],
    ];
}

function sv_mail_config(PDO $pdo): array
{
    $private = load_database_config();
    $storedPassword = null;
    try {
        $storedPassword = decrypt_private_value((string) get_setting($pdo, 'smtp_password_ciphertext', ''));
    } catch (Throwable) {
        $storedPassword = null;
    }
    return [
        'external_enabled' => (string) get_setting($pdo, 'external_services_enabled', '0') === '1',
        'smtp_enabled' => (string) get_setting($pdo, 'smtp_enabled', '0') === '1',
        'host' => trim((string) get_setting($pdo, 'smtp_host', 'nls.hostcreed.com')),
        'port' => max(1, min(65535, (int) get_setting($pdo, 'smtp_port', '465'))),
        'secure' => strtolower(trim((string) get_setting($pdo, 'smtp_secure', 'ssl'))),
        'username' => trim((string) get_setting($pdo, 'smtp_user', '')),
        'password' => (string) ($storedPassword ?: ($private['smtp_password'] ?? '')),
        'from_email' => trim((string) get_setting($pdo, 'smtp_from_email', '')),
        'from_name' => trim((string) get_setting($pdo, 'smtp_from_name', 'SmartVision')),
        'reply_to' => trim((string) get_setting($pdo, 'smtp_reply_to', '')),
        'admin_email' => trim((string) get_setting($pdo, 'admin_notification_email', '')),
    ];
}

function sv_mail_customer_name(string $displayName, string $email): string
{
    $displayName = trim($displayName);
    if ($displayName !== '' && !filter_var($displayName, FILTER_VALIDATE_EMAIL)) {
        return $displayName;
    }

    $localPart = strstr($email, '@', true);
    $fallback = is_string($localPart) && trim($localPart) !== '' ? trim($localPart) : trim($email);
    return $fallback !== '' ? $fallback : 'client';
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
        $blocks['license_block'] = '<div style="margin:22px 0;padding:18px;border:1px solid #c9d8f1;border-radius:12px;background:#f3f7ff">'
            . '<strong style="display:block;color:#0f2242;font-size:14px">Code licence</strong>'
            . '<div style="margin-top:8px;color:#1261e6;font-size:24px;font-weight:800;letter-spacing:1px">'
            . htmlspecialchars((string) ($license['code'] ?? ''), ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
            . '</div><p style="margin:10px 0 0;color:#536985;font-size:13px">Saisissez ce code dans l’application SmartVision pour activer votre appareil.</p></div>';
    }
    if (is_array($payload['admin_event'] ?? null)) {
        $rows = '';
        foreach ($payload['admin_event'] as $label => $value) {
            $rows .= '<tr><td style="padding:9px 8px;border-bottom:1px solid #dbe5f3;color:#536985">'
                . htmlspecialchars((string) $label, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
                . '</td><td style="padding:9px 8px;border-bottom:1px solid #dbe5f3;color:#0f2242;font-weight:700">'
                . htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
                . '</td></tr>';
        }
        $blocks['admin_event_table'] = '<table style="width:100%;border-collapse:collapse;margin:12px 0 4px">' . $rows . '</table>';
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

    $baseUrl = smartvision_public_base_url();
    $payload = array_replace_recursive([
        'app' => [
            'name' => 'SmartVision',
            'tagline' => 'IPTV PLAYER',
            'logo_url' => $baseUrl . '/assets/images/smartvision-logo-wide.png?v=3',
        ],
        'customer' => ['name' => 'client', 'email' => ''],
        'order' => ['reference' => '-', 'plan' => 'SmartVision', 'amount' => '-'],
        'payment' => ['txn' => '-'],
        'trial' => ['expires_at' => '-'],
        'device' => ['public_code' => '-'],
        'verify_url' => $baseUrl . '/verify-email/',
        'reset_url' => $baseUrl . '/reset-password/',
        'account_url' => $baseUrl . '/account/',
        'orders_url' => $baseUrl . '/account/',
        'plans_url' => $baseUrl . '/#plans',
        'cart_url' => $baseUrl . '/#plans',
        'setup_url' => $baseUrl . '/activate/',
        'admin_url' => $baseUrl . '/admin/',
        'site_url' => $baseUrl,
        'site_name' => 'SmartVision',
    ], $payload);

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
        $button = '<p style="margin:28px 0 18px"><a href="' . $escapedUrl . '" style="display:inline-block;padding:15px 24px;border-radius:9px;background:#1463ff;color:#ffffff;text-decoration:none;font-weight:800">'
            . $buttonLabel . '</a></p>';
        $fallback = '<p style="margin:0 0 24px;font-size:12px;line-height:1.6;color:#566982">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br><a href="' . $escapedUrl . '" style="color:#1261e6;text-decoration:underline">' . $escapedUrl . '</a></p>';
    }
    $html = '<!doctype html><html><body style="margin:0;background:#020611;color:#f7f9fd;font-family:Arial,sans-serif">'
        . '<div style="max-width:640px;margin:0 auto;padding:32px 18px"><div style="padding:28px;border:1px solid #223651;border-radius:12px;background:#081423">'
        . '<h1 style="margin:0 0 18px;font-size:28px;color:#ffffff">' . $title . '</h1>'
        . '<div style="color:#b6c3d5;line-height:1.65">' . $intro . $body . $button . $fallback . $footer . '</div>'
        . '</div><p style="color:#65758a;font-size:12px;text-align:center">SmartVision — lecteur Android TV sans contenu inclus.</p></div>'
        . '</body></html>';
    $logoUrl = htmlspecialchars((string) sv_mail_payload_value($payload, 'app.logo_url'), ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    $appTagline = htmlspecialchars((string) sv_mail_payload_value($payload, 'app.tagline'), ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    $html = '<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>'
        . '<body style="margin:0;padding:0;background:#eaf2ff;color:#0f172a;font-family:Arial,Helvetica,sans-serif">'
        . '<div style="width:100%;background:#eaf2ff;padding:32px 8px">'
        . '<div style="max-width:640px;margin:0 auto;border:1px solid #d3dfef;border-radius:8px;overflow:hidden;background:#ffffff">'
        . '<div style="padding:24px 32px;background:#123a7a">'
        . '<img src="' . $logoUrl . '" width="190" alt="SmartVision" style="display:block;max-width:190px;height:auto;border:0;outline:none;text-decoration:none">'
        . '<p style="margin:8px 0 0;color:#dce8ff;font-size:13px">' . $appTagline . '</p>'
        . '</div>'
        . '<div style="padding:32px 32px 28px;background:#ffffff">'
        . '<h1 style="margin:0 0 18px;color:#101828;font-size:26px;line-height:1.25;font-weight:800">' . $title . '</h1>'
        . '<div style="color:#1f2937;font-size:15px;line-height:1.65">' . $intro . $body . $button . $fallback . $footer . '</div>'
        . '</div>'
        . '</div>'
        . '<p style="max-width:640px;margin:14px auto 0;color:#6b7b90;font-size:12px;line-height:1.5;text-align:center">SmartVision - lecteur Android TV sans contenu inclus. Vous recevez cet email car une action a ete effectuee sur votre compte SmartVision.</p>'
        . '</div>'
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
        'customer' => ['name' => sv_mail_customer_name($displayName, $email), 'email' => $email],
        'verify_url' => $verifyUrl,
    ]);
    return $token;
}
