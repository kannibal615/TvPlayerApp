<?php
declare(strict_types=1);

function apply_api_headers(): void
{
    $allowedOrigins = [
        'https://app.smartvisions.net',
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
    return 'SV-' . generate_short_code(4) . '-' . generate_short_code(4) . '-' . generate_short_code(4);
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

function clean_optional_text(mixed $value, int $maxLength): ?string
{
    $text = trim((string) $value);

    if ($text === '') {
        return null;
    }

    return mb_substr($text, 0, $maxLength, 'UTF-8');
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
