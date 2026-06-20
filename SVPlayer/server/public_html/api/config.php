<?php
declare(strict_types=1);

date_default_timezone_set('UTC');

const SMARTVISION_PUBLIC_BASE_URL = 'https://app.smartvisions.net';
const SMARTVISION_PRIVATE_CONFIG = 'smartvision_private/config.php';

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
    $privateConfig = dirname(__DIR__, 2) . DIRECTORY_SEPARATOR . SMARTVISION_PRIVATE_CONFIG;

    if (is_file($privateConfig)) {
        $config = require $privateConfig;
        if (is_array($config)) {
            return $config;
        }
    }

    return [
        'mysql_host' => getenv('SMARTVISION_MYSQL_HOST') ?: '',
        'mysql_username' => getenv('SMARTVISION_MYSQL_USERNAME') ?: '',
        'mysql_password' => getenv('SMARTVISION_MYSQL_PASSWORD') ?: '',
        'mysql_database' => getenv('SMARTVISION_MYSQL_DATABASE') ?: '',
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
