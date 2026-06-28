CREATE TABLE IF NOT EXISTS devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    device_fingerprint_hash CHAR(64) NULL UNIQUE,
    public_device_code VARCHAR(6) NULL UNIQUE,
    device_name VARCHAR(100) NULL,
    platform VARCHAR(50) DEFAULT 'android_tv',
    app_version VARCHAR(50) NULL,
    status ENUM('pending', 'active', 'expired', 'blocked') DEFAULT 'pending',
    license_status ENUM('inactive', 'active', 'expired', 'blocked') DEFAULT 'inactive',
    trial_status ENUM('available', 'pending_xtream', 'active', 'expired', 'used') DEFAULT 'available',
    free_with_ads_status ENUM('inactive', 'active') DEFAULT 'inactive',
    xtream_status ENUM('missing', 'configured', 'invalid') DEFAULT 'missing',
    country_code VARCHAR(8) NULL,
    install_ip_hash CHAR(64) NULL,
    last_ip_hash CHAR(64) NULL,
    last_user_agent VARCHAR(255) NULL,
    first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NULL,
    activated_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint_hash CHAR(64) NULL UNIQUE AFTER device_id;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS public_device_code VARCHAR(6) NULL UNIQUE AFTER device_fingerprint_hash;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS license_status ENUM('inactive', 'active', 'expired', 'blocked') DEFAULT 'inactive' AFTER status;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS trial_status ENUM('available', 'pending_xtream', 'active', 'expired', 'used') DEFAULT 'available' AFTER license_status;
ALTER TABLE devices MODIFY trial_status ENUM('available', 'pending_xtream', 'active', 'expired', 'used') DEFAULT 'available';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS free_with_ads_status ENUM('inactive', 'active') DEFAULT 'inactive' AFTER trial_status;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS xtream_status ENUM('missing', 'configured', 'invalid') DEFAULT 'missing' AFTER free_with_ads_status;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS country_code VARCHAR(8) NULL AFTER xtream_status;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS install_ip_hash CHAR(64) NULL AFTER country_code;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_ip_hash CHAR(64) NULL AFTER install_ip_hash;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_user_agent VARCHAR(255) NULL AFTER last_ip_hash;

CREATE TABLE IF NOT EXISTS activation_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    short_code VARCHAR(20) NOT NULL UNIQUE,
    status ENUM('pending', 'validated', 'expired', 'cancelled') DEFAULT 'pending',
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    validated_at DATETIME NULL,
    INDEX (device_id),
    INDEX (short_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activation_session_tokens (
    session_id INT NOT NULL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX (device_id),
    CONSTRAINT fk_activation_session_tokens_session
        FOREIGN KEY (session_id) REFERENCES activation_sessions(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activation_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code_hash CHAR(64) NOT NULL UNIQUE,
    label VARCHAR(100) NULL,
    duration_days INT UNSIGNED NOT NULL DEFAULT 365,
    max_devices INT UNSIGNED NOT NULL DEFAULT 1,
    used_devices INT UNSIGNED NOT NULL DEFAULT 0,
    license_type ENUM('paid', 'trial', 'free', 'manual', 'promo') NOT NULL DEFAULT 'manual',
    status ENUM('active', 'disabled', 'expired') DEFAULT 'active',
    valid_until DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activation_code_metadata (
    code_id INT NOT NULL PRIMARY KEY,
    code_hint VARCHAR(32) NULL,
    created_by VARCHAR(100) NULL,
    last_used_at DATETIME NULL,
    code_ciphertext LONGTEXT NULL,
    assigned_device_id VARCHAR(100) NULL,
    assigned_public_device_code VARCHAR(6) NULL,
    CONSTRAINT fk_activation_code_metadata_code
        FOREIGN KEY (code_id) REFERENCES activation_codes(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE activation_codes ADD COLUMN IF NOT EXISTS license_type ENUM('paid', 'trial', 'free', 'manual', 'promo') NOT NULL DEFAULT 'manual' AFTER used_devices;
ALTER TABLE activation_code_metadata ADD COLUMN IF NOT EXISTS code_ciphertext LONGTEXT NULL AFTER last_used_at;
ALTER TABLE activation_code_metadata ADD COLUMN IF NOT EXISTS assigned_device_id VARCHAR(100) NULL AFTER code_ciphertext;
ALTER TABLE activation_code_metadata ADD COLUMN IF NOT EXISTS assigned_public_device_code VARCHAR(6) NULL AFTER assigned_device_id;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_username VARCHAR(100) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id VARCHAR(100) NULL,
    details_json TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX (created_at),
    INDEX (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_anomaly_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id_hash CHAR(64) NULL,
    app_version VARCHAR(50) NULL,
    platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    route VARCHAR(120) NULL,
    anomaly_type VARCHAR(60) NOT NULL,
    message VARCHAR(255) NULL,
    stack_trace TEXT NULL,
    context_json TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_anomaly_created (created_at),
    INDEX idx_anomaly_type (anomaly_type),
    INDEX idx_anomaly_route (route),
    INDEX idx_anomaly_device_time (device_id_hash, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('super_admin', 'admin', 'support') NOT NULL DEFAULT 'admin',
    status ENUM('active', 'blocked') NOT NULL DEFAULT 'active',
    failed_login_attempts INT UNSIGNED NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (status),
    INDEX (last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    display_name VARCHAR(120) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status ENUM('active', 'blocked') DEFAULT 'active',
    email_verified_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS device_activations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    activation_code_id INT NULL,
    activation_type ENUM('smartvision_code', 'own_xtream', 'trial_demo', 'free_ads') NOT NULL,
    status ENUM('active', 'expired', 'blocked') DEFAULT 'active',
    starts_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE device_activations
    MODIFY activation_type ENUM('smartvision_code', 'own_xtream', 'trial_demo', 'free_ads') NOT NULL;

CREATE TABLE IF NOT EXISTS activation_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_reference VARCHAR(32) NULL,
    plan_key VARCHAR(40) NOT NULL,
    plan_label VARCHAR(80) NOT NULL,
    amount_cents INT UNSIGNED NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    status ENUM('pending', 'paid', 'cancelled') DEFAULT 'pending',
    payment_provider VARCHAR(40) NULL,
    payment_reference VARCHAR(100) NULL,
    checkout_token_hash CHAR(64) NULL,
    activation_code_id INT NULL,
    activation_code_ciphertext LONGTEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (user_id),
    INDEX (status),
    UNIQUE KEY uq_activation_orders_reference (order_reference),
    UNIQUE KEY uq_activation_orders_checkout_token (checkout_token_hash),
    CONSTRAINT fk_activation_orders_user
        FOREIGN KEY (user_id) REFERENCES site_users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_activation_orders_code
        FOREIGN KEY (activation_code_id) REFERENCES activation_codes(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE site_users ADD COLUMN IF NOT EXISTS status ENUM('active', 'blocked') DEFAULT 'active' AFTER password_hash;
ALTER TABLE site_users ADD COLUMN IF NOT EXISTS email_verified_at DATETIME NULL AFTER status;
ALTER TABLE site_users ADD COLUMN IF NOT EXISTS updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER last_login_at;

ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS order_reference VARCHAR(32) NULL AFTER user_id;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS currency CHAR(3) NOT NULL DEFAULT 'EUR' AFTER amount_cents;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(40) NULL AFTER status;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100) NULL AFTER payment_provider;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS checkout_token_hash CHAR(64) NULL AFTER payment_reference;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS activation_code_ciphertext LONGTEXT NULL AFTER activation_code_id;
ALTER TABLE activation_orders ADD COLUMN IF NOT EXISTS updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER paid_at;

UPDATE activation_orders
SET order_reference = CONCAT('SV-LEGACY-', LPAD(id, 6, '0'))
WHERE order_reference IS NULL OR order_reference = '';

ALTER TABLE activation_orders ADD UNIQUE INDEX IF NOT EXISTS uq_activation_orders_reference (order_reference);
ALTER TABLE activation_orders ADD UNIQUE INDEX IF NOT EXISTS uq_activation_orders_checkout_token (checkout_token_hash);

CREATE TABLE IF NOT EXISTS device_playlist_configs (
    device_id VARCHAR(100) NOT NULL PRIMARY KEY,
    encrypted_payload LONGTEXT NOT NULL,
    delivered_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ads_settings (
    id TINYINT UNSIGNED NOT NULL PRIMARY KEY DEFAULT 1,
    ads_enabled TINYINT(1) NOT NULL DEFAULT 1,
    provider ENUM('HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST') NOT NULL DEFAULT 'HILLTOPADS_VAST',
    use_test_ads TINYINT(1) NOT NULL DEFAULT 1,
    vast_production_tag_url TEXT NULL,
    vast_test_tag_url TEXT NULL,
    min_minutes_between_ads INT UNSIGNED NOT NULL DEFAULT 30,
    max_ads_per_day INT UNSIGNED NOT NULL DEFAULT 3,
    show_ad_before_live_stream TINYINT(1) NOT NULL DEFAULT 1,
    show_ad_before_movie TINYINT(1) NOT NULL DEFAULT 1,
    show_ad_before_series_episode TINYINT(1) NOT NULL DEFAULT 1,
    allow_playback_if_ad_fails TINYINT(1) NOT NULL DEFAULT 1,
    ads_only_inside_player TINYINT(1) NOT NULL DEFAULT 1,
    estimated_ecpm_eur DECIMAL(8,2) NOT NULL DEFAULT 5.00,
    hilltop_site_id VARCHAR(64) NULL,
    hilltop_zone_id VARCHAR(64) NULL,
    config_version INT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS estimated_ecpm_eur DECIMAL(8,2) NOT NULL DEFAULT 5.00 AFTER ads_only_inside_player;
ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS hilltop_site_id VARCHAR(64) NULL AFTER estimated_ecpm_eur;
ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS hilltop_zone_id VARCHAR(64) NULL AFTER hilltop_site_id;

CREATE TABLE IF NOT EXISTS ads_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id_hash CHAR(64) NULL,
    app_version VARCHAR(50) NULL,
    platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    user_status ENUM('PREMIUM_ACTIVE', 'TRIAL_ACTIVE', 'TRIAL_EXPIRED', 'LICENSE_EXPIRED', 'FREE_WITH_ADS', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    content_type ENUM('LIVE_TV', 'MOVIE', 'SERIES', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    provider ENUM('HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    event_type VARCHAR(40) NOT NULL,
    reason VARCHAR(60) NULL,
    error_code VARCHAR(60) NULL,
    error_message VARCHAR(255) NULL,
    ad_duration_seconds INT UNSIGNED NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ads_events_created (created_at),
    INDEX idx_ads_events_event_type (event_type),
    INDEX idx_ads_events_provider (provider),
    INDEX idx_ads_events_content_type (content_type),
    INDEX idx_ads_events_platform (platform),
    INDEX idx_ads_events_user_status (user_status),
    INDEX idx_ads_events_device_time (device_id_hash, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_behavior_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id_hash CHAR(64) NULL,
    app_version VARCHAR(50) NULL,
    platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    event_type VARCHAR(40) NOT NULL,
    video_id_hash CHAR(64) NULL,
    channel_id VARCHAR(80) NULL,
    category_id VARCHAR(40) NULL,
    tags VARCHAR(500) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_behavior_created (created_at),
    INDEX idx_behavior_event_type (event_type),
    INDEX idx_behavior_channel (channel_id),
    INDEX idx_behavior_category (category_id),
    INDEX idx_behavior_device_time (device_id_hash, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ads_settings
    (id, ads_enabled, provider, use_test_ads, vast_production_tag_url, vast_test_tag_url,
     min_minutes_between_ads, max_ads_per_day, show_ad_before_live_stream, show_ad_before_movie,
     show_ad_before_series_episode, allow_playback_if_ad_fails, ads_only_inside_player,
     estimated_ecpm_eur, hilltop_site_id, hilltop_zone_id, config_version, updated_by)
VALUES
    (1, 1, 'HILLTOPADS_VAST', 1, '', 'https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&correlator=',
     30, 3, 1, 1, 1, 1, 1, 5.00, '', '', 1, 'system')
ON DUPLICATE KEY UPDATE id = VALUES(id);

CREATE TABLE IF NOT EXISTS commerce_order_intents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    plan_key VARCHAR(40) NOT NULL,
    plan_label VARCHAR(80) NOT NULL,
    amount_cents INT UNSIGNED NOT NULL,
    merchant_amount_cents INT UNSIGNED NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    status ENUM('started', 'callback_received', 'approved', 'rejected', 'cancelled', 'expired') NOT NULL DEFAULT 'started',
    checkout_url TEXT NULL,
    raw_payload JSON NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_commerce_order_intents_token (token_hash),
    INDEX idx_commerce_order_intents_user (user_id, status),
    INDEX idx_commerce_order_intents_expiry (expires_at),
    CONSTRAINT fk_commerce_order_intents_user
        FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS commerce_payments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    intent_id BIGINT UNSIGNED NOT NULL,
    user_id INT NOT NULL,
    activation_order_id INT NULL,
    txn VARCHAR(190) NOT NULL,
    gateway VARCHAR(40) NOT NULL DEFAULT 'gammal',
    gateway_status INT NOT NULL,
    verification_status ENUM('pending_review', 'approved', 'rejected') NOT NULL DEFAULT 'pending_review',
    reported_amount_cents INT UNSIGNED NOT NULL,
    expected_amount_cents INT UNSIGNED NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    raw_payload JSON NOT NULL,
    reviewed_by VARCHAR(100) NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_commerce_payments_txn (txn),
    UNIQUE KEY uq_commerce_payments_intent (intent_id),
    INDEX idx_commerce_payments_review (verification_status, created_at),
    CONSTRAINT fk_commerce_payments_intent
        FOREIGN KEY (intent_id) REFERENCES commerce_order_intents(id) ON DELETE RESTRICT,
    CONSTRAINT fk_commerce_payments_user
        FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_commerce_payments_order
        FOREIGN KEY (activation_order_id) REFERENCES activation_orders(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS commerce_webhook_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    txn VARCHAR(190) NULL,
    project_id BIGINT NULL,
    amount_cents INT UNSIGNED NULL,
    currency CHAR(3) NULL,
    verification_status ENUM('valid', 'invalid') NOT NULL DEFAULT 'valid',
    processing_status ENUM('captured', 'approved', 'duplicate', 'pending_callback', 'configuration_required', 'rejected') NOT NULL DEFAULT 'captured',
    payment_id BIGINT UNSIGNED NULL,
    message VARCHAR(255) NULL,
    claims_json JSON NULL,
    raw_payload JSON NULL,
    user_agent VARCHAR(190) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_commerce_webhook_events_token (token_hash),
    UNIQUE KEY uq_commerce_webhook_events_event_txn (event_type, txn),
    INDEX idx_commerce_webhook_events_status (processing_status, created_at),
    INDEX idx_commerce_webhook_events_payment (payment_id),
    CONSTRAINT fk_commerce_webhook_events_payment
        FOREIGN KEY (payment_id) REFERENCES commerce_payments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_templates (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_logs (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX email_verification_user_idx (user_id, used_at, expires_at),
    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('external_services_enabled', '0'),
    ('smtp_enabled', '0'),
    ('smtp_host', 'nls.hostcreed.com'),
    ('smtp_port', '465'),
    ('smtp_secure', 'ssl'),
    ('smtp_user', ''),
    ('smtp_from_email', ''),
    ('smtp_from_name', 'SmartVision'),
    ('smtp_reply_to', ''),
    ('admin_notification_email', '')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);

CREATE TABLE IF NOT EXISTS home_slider_ads (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sort_order INT NOT NULL DEFAULT 0,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(255) NULL,
    button_label VARCHAR(60) NULL,
    button_route VARCHAR(120) NULL,
    image_url VARCHAR(500) NULL,
    status ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
    starts_at DATETIME NULL,
    ends_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (status),
    INDEX (sort_order),
    UNIQUE KEY uq_home_slider_ads_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE home_slider_ads ADD UNIQUE INDEX IF NOT EXISTS uq_home_slider_ads_sort (sort_order);

CREATE TABLE IF NOT EXISTS app_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    message TEXT NOT NULL,
    target_scope ENUM('all', 'devices', 'users') NOT NULL DEFAULT 'all',
    target_value TEXT NULL,
    priority ENUM('normal', 'important', 'urgent') NOT NULL DEFAULT 'normal',
    status ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
    created_by VARCHAR(100) NULL,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (status),
    INDEX (target_scope),
    INDEX (expires_at),
    INDEX (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_notification_receipts (
    notification_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    seen_at DATETIME NOT NULL,
    PRIMARY KEY (notification_id, device_id),
    INDEX (device_id),
    INDEX (seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    status ENUM('new', 'read', 'handled', 'archived') NOT NULL DEFAULT 'new',
    ip_hash CHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (status),
    INDEX (email),
    INDEX (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_settings (setting_key, setting_value) VALUES
('trial_duration_days', '7'),
('activation_session_minutes', '15'),
('polling_interval_seconds', '5'),
('activation_duration_days', '365'),
('payment_mode', 'test'),
('support_email', 'support@smartvisions.net')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

INSERT INTO app_settings (setting_key, setting_value) VALUES
('gammal_payment_month_1_url', ''),
('gammal_payment_month_1_enabled', '0'),
('gammal_payment_year_1_url', ''),
('gammal_payment_year_1_enabled', '0'),
('gammal_payment_lifetime_url', ''),
('gammal_payment_lifetime_enabled', '0'),
('gammal_webhook_project_ids', ''),
('gammal_webhook_auto_approve', '0'),
('gammal_webhook_public_key_manual', ''),
('gammal_webhook_public_key_cache', ''),
('gammal_webhook_public_key_cached_at', '0')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);

INSERT INTO home_slider_ads (sort_order, title, subtitle, button_label, button_route, image_url, status) VALUES
(10, 'Bienvenue sur SmartVision', 'Une experience IPTV fluide, premium et pensee pour Android TV.', 'En savoir plus', 'live_tv', '/assets/images/app-live-tv.png', 'active'),
(20, 'Live TV instantanee', 'Retrouvez vos chaines en direct avec une navigation simple a la telecommande.', 'Voir Live TV', 'live_tv', '/assets/images/app-live-tv.png', 'active'),
(30, 'Films et series', 'Explorez vos catalogues Xtream avec affiches, details et reprise de lecture.', 'Explorer', 'movies', '/assets/images/app-movies.png', 'active')
ON DUPLICATE KEY UPDATE title = VALUES(title);
