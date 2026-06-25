<?php
declare(strict_types=1);

date_default_timezone_set('UTC');

const SMARTVISION_PRIVATE_CONFIG = 'smartvision_private/config.php';

function smartvision_public_base_url(): string
{
    $configured = trim((string) getenv('SMARTVISION_PUBLIC_BASE_URL'));
    if ($configured !== '' && filter_var($configured, FILTER_VALIDATE_URL)) {
        return rtrim($configured, '/');
    }

    return 'https://smartvisions.net';
}

function smartvision_is_https(): bool
{
    $forwardedProto = strtolower(trim((string) ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '')));
    return (!empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off')
        || $forwardedProto === 'https';
}

function smartvision_cookie_secure(): bool
{
    return smartvision_is_https();
}

function config_json_error(string $message, int $statusCode = 500): never
{
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'error' => $message,
    ], JSON_UNESCAPED_SLASHES);
    exit;
}

function load_database_config(): array
{
    $privateConfigCandidates = [];
    $ancestor = dirname(__DIR__);
    while ($ancestor !== dirname($ancestor)) {
        if (basename($ancestor) === 'public_html') {
            $privateConfigCandidates[] = dirname($ancestor)
                . DIRECTORY_SEPARATOR . SMARTVISION_PRIVATE_CONFIG;
            break;
        }
        $ancestor = dirname($ancestor);
    }

    $accountHome = trim((string) getenv('HOME'));
    if ($accountHome !== '') {
        $privateConfigCandidates[] = rtrim($accountHome, DIRECTORY_SEPARATOR)
            . DIRECTORY_SEPARATOR . SMARTVISION_PRIVATE_CONFIG;
    }

    $privateConfigCandidates[] = dirname(__DIR__, 2)
        . DIRECTORY_SEPARATOR . SMARTVISION_PRIVATE_CONFIG;
    $privateConfigCandidates[] = dirname(__DIR__, 3)
        . DIRECTORY_SEPARATOR . SMARTVISION_PRIVATE_CONFIG;

    foreach (array_unique($privateConfigCandidates) as $privateConfig) {
        if (is_file($privateConfig)) {
            $config = require $privateConfig;
            if (is_array($config)) {
                $config['_private_config_path'] = $privateConfig;
                return $config;
            }
        }
    }

    return [
        'mysql_host' => getenv('SMARTVISION_MYSQL_HOST') ?: '',
        'mysql_username' => getenv('SMARTVISION_MYSQL_USERNAME') ?: '',
        'mysql_password' => getenv('SMARTVISION_MYSQL_PASSWORD') ?: '',
        'mysql_database' => getenv('SMARTVISION_MYSQL_DATABASE') ?: '',
        'admin_username' => getenv('SMARTVISION_ADMIN_USERNAME') ?: '',
        'admin_password_hash' => getenv('SMARTVISION_ADMIN_PASSWORD_HASH') ?: '',
        'credentials_encryption_key' => getenv('SMARTVISION_CREDENTIALS_ENCRYPTION_KEY') ?: '',
        'cpanel_host' => getenv('SMARTVISION_CPANEL_HOST') ?: '',
        'cpanel_username' => getenv('SMARTVISION_CPANEL_USERNAME') ?: '',
        'cpanel_token' => getenv('SMARTVISION_CPANEL_TOKEN') ?: '',
        'smtp_password' => getenv('SMARTVISION_SMTP_PASSWORD') ?: '',
    ];
}

function db(): PDO
{
    static $pdo = null;

    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $config = load_database_config();
    $host = trim((string) ($config['mysql_host'] ?? ''));
    $database = trim((string) ($config['mysql_database'] ?? ''));
    $username = trim((string) ($config['mysql_username'] ?? ''));
    $password = (string) ($config['mysql_password'] ?? '');

    if ($host === '' || $database === '' || $username === '') {
        config_json_error('Configuration serveur indisponible.', 500);
    }

    $dsn = sprintf('mysql:host=%s;dbname=%s;charset=utf8mb4', $host, $database);

    try {
        $pdo = new PDO($dsn, $username, $password, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
    } catch (Throwable $exception) {
        error_log('SmartVision database connection failed.');
        config_json_error('Connexion base de donnees indisponible.', 503);
    }

    return $pdo;
}
