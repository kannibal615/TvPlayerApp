<?php
declare(strict_types=1);

function apply_api_headers(): void
{
    $allowedOrigins = [
        'https://smartvisions.net',
    ];

    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    if (in_array($origin, $allowedOrigins, true)) {
        header('Access-Control-Allow-Origin: ' . $origin);
        header('Vary: Origin');
    }

    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization');
    header('Content-Type: application/json; charset=utf-8');

    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
        http_response_code(204);
        exit;
    }
}

function json_response(array $data, int $statusCode = 200): never
{
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function read_json_input(): array
{
    $rawBody = file_get_contents('php://input');

    if ($rawBody === false || trim($rawBody) === '') {
        return [];
    }

    $data = json_decode($rawBody, true);
    if (!is_array($data) || json_last_error() !== JSON_ERROR_NONE) {
        json_response([
            'success' => false,
            'error' => 'JSON invalide.',
        ], 400);
    }

    return $data;
}

function generate_short_code(int $length = 6): string
{
    $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    $max = strlen($alphabet) - 1;
    $code = '';

    for ($index = 0; $index < $length; $index++) {
        $code .= $alphabet[random_int(0, $max)];
    }

    return $code;
}

function generate_public_activation_code(): string
{
    return generate_short_code(10);
}

function generate_uuid_v4(): string
{
    $bytes = random_bytes(16);
    $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
    $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);

    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($bytes), 4));
}

function clean_public_device_code(?string $code): string
{
    $normalized = preg_replace('/[^A-Z0-9]/', '', strtoupper(trim((string) $code)));

    return substr((string) $normalized, 0, 6);
}

function clean_hash(?string $value): string
{
    $hash = preg_replace('/[^a-fA-F0-9]/', '', trim((string) $value));

    return substr(strtolower((string) $hash), 0, 128);
}

function get_setting(PDO $pdo, string $key, mixed $default = null): mixed
{
    $statement = $pdo->prepare('SELECT setting_value FROM app_settings WHERE setting_key = :setting_key LIMIT 1');
    $statement->execute(['setting_key' => $key]);
    $value = $statement->fetchColumn();

    return $value === false ? $default : $value;
}

function clean_device_id(?string $deviceId): string
{
    $clean = preg_replace('/[^A-Za-z0-9._:-]/', '', trim((string) $deviceId));

    return substr((string) $clean, 0, 100);
}

function normalize_activation_code(?string $code): string
{
    $normalized = preg_replace('/[^A-Z0-9]/', '', strtoupper(trim((string) $code)));

    return substr((string) $normalized, 0, 64);
}

function activation_code_hash(string $normalizedCode): string
{
    return hash('sha256', $normalizedCode);
}

function request_country_code(): ?string
{
    $country = strtoupper(preg_replace('/[^A-Z]/', '', (string) ($_SERVER['HTTP_CF_IPCOUNTRY'] ?? '')));

    return $country === '' ? null : substr($country, 0, 8);
}

function request_ip_hash(): ?string
{
    $ip = trim((string) ($_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'] ?? ''));
    if (str_contains($ip, ',')) {
        $ip = trim(explode(',', $ip)[0]);
    }

    return $ip === '' ? null : hash('sha256', $ip);
}

function clean_optional_text(mixed $value, int $maxLength): ?string
{
    $text = trim((string) $value);

    if ($text === '') {
        return null;
    }

    return smartvision_text_substr($text, 0, $maxLength);
}

function smartvision_text_substr(string $text, int $start, int $length): string
{
    if (function_exists('mb_substr')) {
        return mb_substr($text, $start, $length, 'UTF-8');
    }

    return substr($text, $start, $length);
}

function is_duplicate_key(Throwable $exception): bool
{
    return $exception instanceof PDOException && $exception->getCode() === '23000';
}

function generate_device_token(): string
{
    return rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
}

function device_token_hash(string $token): string
{
    return hash('sha256', trim($token));
}

function has_valid_device_token(PDO $pdo, string $deviceId, string $token): bool
{
    if ($token === '' || strlen($token) > 128) {
        return false;
    }

    $statement = $pdo->prepare(
        "SELECT 1
         FROM activation_session_tokens tokens
         INNER JOIN activation_sessions sessions ON sessions.id = tokens.session_id
         WHERE tokens.device_id = :device_id
           AND tokens.token_hash = :token_hash
           AND sessions.status IN ('pending', 'validated')
         LIMIT 1"
    );
    $statement->execute([
        'device_id' => $deviceId,
        'token_hash' => device_token_hash($token),
    ]);

    return $statement->fetchColumn() !== false;
}

function normalize_xtream_host(mixed $value): string
{
    $host = rtrim(trim((string) $value), '/');
    if ($host === '' || strlen($host) > 255) {
        return '';
    }
    if (!preg_match('#^https?://#i', $host)) {
        $host = 'https://' . $host;
    }
    $parts = parse_url($host);
    if (!is_array($parts) || empty($parts['host']) || !in_array(strtolower((string) ($parts['scheme'] ?? '')), ['http', 'https'], true)) {
        return '';
    }

    return $host;
}

function credentials_key(): string
{
    $config = load_database_config();
    $encoded = trim((string) ($config['credentials_encryption_key'] ?? ''));
    $decoded = base64_decode($encoded, true);
    if ($decoded === false || strlen($decoded) < 32) {
        throw new RuntimeException('Credentials encryption key unavailable.');
    }

    return substr($decoded, 0, 32);
}

function encrypt_playlist_config(array $config): string
{
    $iv = random_bytes(12);
    $tag = '';
    $plainText = json_encode($config, JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
    $cipherText = openssl_encrypt($plainText, 'aes-256-gcm', credentials_key(), OPENSSL_RAW_DATA, $iv, $tag);
    if ($cipherText === false) {
        throw new RuntimeException('Unable to encrypt playlist configuration.');
    }

    return base64_encode($iv . $tag . $cipherText);
}

function encrypt_private_value(string $value): string
{
    $iv = random_bytes(12);
    $tag = '';
    $cipherText = openssl_encrypt($value, 'aes-256-gcm', credentials_key(), OPENSSL_RAW_DATA, $iv, $tag);
    if ($cipherText === false) {
        throw new RuntimeException('Unable to encrypt private value.');
    }

    return base64_encode($iv . $tag . $cipherText);
}

function decrypt_private_value(?string $payload): ?string
{
    if ($payload === null || trim($payload) === '') {
        return null;
    }

    $raw = base64_decode($payload, true);
    if ($raw === false || strlen($raw) <= 28) {
        return null;
    }

    $plainText = openssl_decrypt(
        substr($raw, 28),
        'aes-256-gcm',
        credentials_key(),
        OPENSSL_RAW_DATA,
        substr($raw, 0, 12),
        substr($raw, 12, 16),
    );

    return $plainText === false ? null : $plainText;
}

function create_activation_code_record(
    PDO $pdo,
    string $label,
    int $durationDays,
    int $maxDevices,
    string $licenseType,
    ?string $validUntil,
    string $createdBy,
    ?string $assignedDeviceId = null,
    ?string $assignedPublicDeviceCode = null,
): array {
    $licenseType = in_array($licenseType, ['paid', 'trial', 'free', 'manual', 'promo'], true)
        ? $licenseType
        : 'manual';
    $durationDays = max(1, min(36500, $durationDays));
    $maxDevices = max(1, min(1000, $maxDevices));
    $label = smartvision_text_substr(trim($label), 0, 100);
    $assignedDeviceId = $assignedDeviceId === null ? null : clean_device_id($assignedDeviceId);
    $assignedPublicDeviceCode = $assignedPublicDeviceCode === null ? null : clean_public_device_code($assignedPublicDeviceCode);

    for ($attempt = 0; $attempt < 16; $attempt++) {
        $plainCode = generate_public_activation_code();
        try {
            $statement = $pdo->prepare(
                "INSERT INTO activation_codes
                    (code_hash, label, duration_days, max_devices, used_devices, license_type, status, valid_until, created_at, updated_at)
                 VALUES
                    (:code_hash, :label, :duration_days, :max_devices, 0, :license_type, 'active', :valid_until, NOW(), NOW())"
            );
            $statement->execute([
                'code_hash' => activation_code_hash(normalize_activation_code($plainCode)),
                'label' => $label === '' ? null : $label,
                'duration_days' => $durationDays,
                'max_devices' => $maxDevices,
                'license_type' => $licenseType,
                'valid_until' => $validUntil,
            ]);
            $codeId = (int) $pdo->lastInsertId();
            $metadata = $pdo->prepare(
                "INSERT INTO activation_code_metadata
                    (code_id, code_hint, created_by, last_used_at, code_ciphertext, assigned_device_id, assigned_public_device_code)
                 VALUES
                    (:code_id, :code_hint, :created_by, NULL, :code_ciphertext, :assigned_device_id, :assigned_public_device_code)"
            );
            $metadata->execute([
                'code_id' => $codeId,
                'code_hint' => $plainCode,
                'created_by' => smartvision_text_substr($createdBy, 0, 100),
                'code_ciphertext' => encrypt_private_value($plainCode),
                'assigned_device_id' => $assignedDeviceId ?: null,
                'assigned_public_device_code' => $assignedPublicDeviceCode ?: null,
            ]);

            return ['id' => $codeId, 'code' => $plainCode];
        } catch (Throwable $exception) {
            if (!is_duplicate_key($exception)) {
                throw $exception;
            }
        }
    }

    throw new RuntimeException('Unable to create activation code.');
}

function create_trial_activation(PDO $pdo, string $deviceId, string $publicDeviceCode = ''): array
{
    $deviceId = clean_device_id($deviceId);
    $publicDeviceCode = clean_public_device_code($publicDeviceCode);
    if ($deviceId === '') {
        throw new InvalidArgumentException('Device id missing.');
    }

    $existing = $pdo->prepare(
        "SELECT id, status, expires_at
         FROM device_activations
         WHERE device_id = :device_id AND activation_type = 'trial_demo'
         ORDER BY id DESC LIMIT 1"
    );
    $existing->execute(['device_id' => $deviceId]);
    $trial = $existing->fetch();
    if (is_array($trial)) {
        if (($trial['status'] ?? '') === 'active' && strtotime((string) $trial['expires_at']) > time()) {
            return [
                'code_id' => null,
                'code' => null,
                'expires_at' => (string) $trial['expires_at'],
                'already_active' => true,
            ];
        }
        throw new RuntimeException('Trial already used.');
    }

    $durationDays = max(1, (int) get_setting($pdo, 'trial_duration_days', '7'));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $durationDays . ' days')
        ->format('Y-m-d H:i:s');
    $created = create_activation_code_record(
        $pdo,
        'Essai gratuit ' . ($publicDeviceCode !== '' ? $publicDeviceCode : $deviceId),
        $durationDays,
        1,
        'trial',
        null,
        'system:trial',
        $deviceId,
        $publicDeviceCode,
    );

    $pdo->prepare("UPDATE device_activations SET status = 'expired' WHERE device_id = :device_id AND status = 'active'")
        ->execute(['device_id' => $deviceId]);

    $insert = $pdo->prepare(
        "INSERT INTO device_activations
            (device_id, activation_code_id, activation_type, status, starts_at, expires_at, created_at)
         VALUES
            (:device_id, :activation_code_id, 'trial_demo', 'active', NOW(), :expires_at, NOW())"
    );
    $insert->execute([
        'device_id' => $deviceId,
        'activation_code_id' => $created['id'],
        'expires_at' => $expiresAt,
    ]);

    $pdo->prepare(
        "UPDATE activation_codes SET used_devices = used_devices + 1, updated_at = NOW() WHERE id = :id"
    )->execute(['id' => $created['id']]);
    $pdo->prepare(
        "UPDATE activation_code_metadata
         SET last_used_at = NOW(), assigned_device_id = :device_id, assigned_public_device_code = :public_code
         WHERE code_id = :id"
    )->execute([
        'id' => $created['id'],
        'device_id' => $deviceId,
        'public_code' => $publicDeviceCode ?: null,
    ]);
    $pdo->prepare(
        "UPDATE devices
         SET status = 'active',
             license_status = 'active',
             trial_status = 'active',
             activated_at = COALESCE(activated_at, NOW()),
             expires_at = :expires_at,
             updated_at = NOW()
         WHERE device_id = :device_id"
    )->execute([
        'device_id' => $deviceId,
        'expires_at' => $expiresAt,
    ]);

    return [
        'code_id' => $created['id'],
        'code' => $created['code'],
        'expires_at' => $expiresAt,
        'already_active' => false,
    ];
}

function decrypt_playlist_config(string $payload): ?array
{
    $raw = base64_decode($payload, true);
    if ($raw === false || strlen($raw) <= 28) {
        return null;
    }
    $iv = substr($raw, 0, 12);
    $tag = substr($raw, 12, 16);
    $cipherText = substr($raw, 28);
    $plainText = openssl_decrypt($cipherText, 'aes-256-gcm', credentials_key(), OPENSSL_RAW_DATA, $iv, $tag);
    if ($plainText === false) {
        return null;
    }
    $decoded = json_decode($plainText, true);

    return is_array($decoded) ? $decoded : null;
}

function ensure_app_notifications_table(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS app_notifications (
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
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS app_notification_receipts (
            notification_id BIGINT NOT NULL,
            device_id VARCHAR(100) NOT NULL,
            seen_at DATETIME NOT NULL,
            PRIMARY KEY (notification_id, device_id),
            INDEX (device_id),
            INDEX (seen_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
}
