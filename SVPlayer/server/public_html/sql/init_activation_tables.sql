CREATE TABLE IF NOT EXISTS devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    device_name VARCHAR(100) NULL,
    platform VARCHAR(50) DEFAULT 'android_tv',
    app_version VARCHAR(50) NULL,
    status ENUM('pending', 'active', 'expired', 'blocked') DEFAULT 'pending',
    first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NULL,
    activated_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    CONSTRAINT fk_activation_code_metadata_code
        FOREIGN KEY (code_id) REFERENCES activation_codes(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS site_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    display_name VARCHAR(120) NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS device_activations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    activation_code_id INT NULL,
    activation_type ENUM('smartvision_code', 'own_xtream', 'trial_demo') NOT NULL,
    status ENUM('active', 'expired', 'blocked') DEFAULT 'active',
    starts_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activation_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    plan_key VARCHAR(40) NOT NULL,
    plan_label VARCHAR(80) NOT NULL,
    amount_cents INT UNSIGNED NOT NULL,
    status ENUM('pending', 'paid', 'cancelled') DEFAULT 'pending',
    activation_code_id INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    INDEX (user_id),
    INDEX (status),
    CONSTRAINT fk_activation_orders_user
        FOREIGN KEY (user_id) REFERENCES site_users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_activation_orders_code
        FOREIGN KEY (activation_code_id) REFERENCES activation_codes(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

INSERT INTO app_settings (setting_key, setting_value) VALUES
('trial_duration_days', '7'),
('activation_session_minutes', '15'),
('polling_interval_seconds', '5'),
('activation_duration_days', '365')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
