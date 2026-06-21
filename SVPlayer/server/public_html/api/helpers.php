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
