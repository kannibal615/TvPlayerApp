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

INSERT INTO app_settings (setting_key, setting_value) VALUES
('trial_duration_days', '7'),
('activation_session_minutes', '15'),
('polling_interval_seconds', '5'),
('activation_duration_days', '365'),
('payment_mode', 'test'),
('support_email', 'support@smartvisions.net')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

INSERT INTO home_slider_ads (sort_order, title, subtitle, button_label, button_route, image_url, status) VALUES
(10, 'Bienvenue sur SmartVision', 'Une experience IPTV fluide, premium et pensee pour Android TV.', 'En savoir plus', 'live_tv', '/assets/images/app-live-tv.png', 'active'),
(20, 'Live TV instantanee', 'Retrouvez vos chaines en direct avec une navigation simple a la telecommande.', 'Voir Live TV', 'live_tv', '/assets/images/app-live-tv.png', 'active'),
(30, 'Films et series', 'Explorez vos catalogues Xtream avec affiches, details et reprise de lecture.', 'Explorer', 'movies', '/assets/images/app-movies.png', 'active')
ON DUPLICATE KEY UPDATE title = VALUES(title);
