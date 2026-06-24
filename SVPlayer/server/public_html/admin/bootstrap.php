<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/helpers.php';

function admin_security_headers(): void
{
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: DENY');
    header('Referrer-Policy: no-referrer');
    header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
}

function start_admin_session(): void
{
    if (session_status() === PHP_SESSION_ACTIVE) {
        return;
    }

    session_name('smartvision_admin');
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/admin/',
        'secure' => true,
        'httponly' => true,
        'samesite' => 'Strict',
    ]);
    session_start();
}

function admin_credentials(): array
{
    $config = load_database_config();

    return [
        'username' => trim((string) ($config['admin_username'] ?? '')),
        'password_hash' => (string) ($config['admin_password_hash'] ?? ''),
    ];
}

function admin_database_state(string $username = ''): array
{
    try {
        $pdo = db();
        $count = (int) $pdo->query("SELECT COUNT(*) FROM admin_users")->fetchColumn();
        $user = null;
        if ($username !== '') {
            $statement = $pdo->prepare(
                "SELECT id, username, password_hash, role, status, failed_login_attempts, locked_until
                 FROM admin_users
                 WHERE username = :username
                 LIMIT 1"
            );
            $statement->execute(['username' => $username]);
            $found = $statement->fetch();
            $user = is_array($found) ? $found : null;
        }

        return [
            'available' => true,
            'count' => $count,
            'user' => $user,
        ];
    } catch (Throwable $exception) {
        error_log('SmartVision admin database lookup failed.');
        return [
            'available' => false,
            'count' => 0,
            'user' => null,
        ];
    }
}

function admin_auth_is_configured(): bool
{
    $databaseState = admin_database_state();
    if ($databaseState['available'] && $databaseState['count'] > 0) {
        return true;
    }

    $credentials = admin_credentials();
    return $credentials['username'] !== '' && $credentials['password_hash'] !== '';
}

function is_admin_authenticated(): bool
{
    return ($_SESSION['admin_authenticated'] ?? false) === true
        && is_string($_SESSION['admin_username'] ?? null);
}

function current_admin_username(): string
{
    return is_admin_authenticated() ? (string) $_SESSION['admin_username'] : 'unknown';
}

function require_admin(): void
{
    if (!is_admin_authenticated()) {
        header('Location: /admin/');
        exit;
    }
}

function verify_admin_login(string $username, string $password): bool
{
    $lockUntil = (int) ($_SESSION['admin_lock_until'] ?? 0);
    if ($lockUntil > time()) {
        return false;
    }

    $username = trim($username);
    $databaseState = admin_database_state($username);
    $databaseUser = $databaseState['user'];
    $usingDatabase = $databaseState['available'] && $databaseState['count'] > 0;
    $validUsername = false;
    $validPassword = false;

    if ($usingDatabase && is_array($databaseUser)) {
        $lockedUntil = isset($databaseUser['locked_until']) && is_string($databaseUser['locked_until'])
            ? strtotime($databaseUser['locked_until'])
            : false;
        $notLocked = $lockedUntil === false || $lockedUntil <= time();
        $validUsername = ($databaseUser['status'] ?? '') === 'active' && $notLocked;
        $validPassword = $validUsername
            && password_verify($password, (string) ($databaseUser['password_hash'] ?? ''));
    } elseif (!$usingDatabase) {
        // Emergency fallback while the database migration is not yet installed.
        $credentials = admin_credentials();
        $validUsername = $credentials['username'] !== ''
            && hash_equals($credentials['username'], $username);
        $validPassword = $credentials['password_hash'] !== ''
            && password_verify($password, $credentials['password_hash']);
    }

    if ($validUsername && $validPassword) {
        if ($usingDatabase && is_array($databaseUser)) {
            $statement = db()->prepare(
                "UPDATE admin_users
                 SET failed_login_attempts = 0, locked_until = NULL, last_login_at = NOW()
                 WHERE id = :id"
            );
            $statement->execute(['id' => (int) $databaseUser['id']]);
        }
        session_regenerate_id(true);
        $_SESSION['admin_authenticated'] = true;
        $_SESSION['admin_username'] = $usingDatabase && is_array($databaseUser)
            ? (string) $databaseUser['username']
            : $username;
        $_SESSION['admin_login_failures'] = 0;
        unset($_SESSION['admin_lock_until']);
        return true;
    }

    if ($usingDatabase && is_array($databaseUser)) {
        $attempts = (int) ($databaseUser['failed_login_attempts'] ?? 0) + 1;
        $statement = db()->prepare(
            "UPDATE admin_users
             SET failed_login_attempts = :attempts,
                 locked_until = CASE WHEN :lock_threshold >= 5 THEN DATE_ADD(NOW(), INTERVAL 5 MINUTE) ELSE locked_until END
             WHERE id = :id"
        );
        $statement->execute([
            'attempts' => $attempts,
            'lock_threshold' => $attempts,
            'id' => (int) $databaseUser['id'],
        ]);
    }

    usleep(500000);
    $failures = (int) ($_SESSION['admin_login_failures'] ?? 0) + 1;
    $_SESSION['admin_login_failures'] = $failures;
    if ($failures >= 5) {
        $_SESSION['admin_lock_until'] = time() + 300;
        $_SESSION['admin_login_failures'] = 0;
    }

    return false;
}

function destroy_admin_session(): void
{
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $params['path'], '', true, true);
    }
    session_destroy();
}

function csrf_token(): string
{
    if (!is_string($_SESSION['csrf_token'] ?? null)) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }

    return $_SESSION['csrf_token'];
}

function verify_csrf(?string $token): bool
{
    $stored = $_SESSION['csrf_token'] ?? '';

    return is_string($stored) && is_string($token) && $stored !== '' && hash_equals($stored, $token);
}

function set_admin_flash(string $type, string $message): void
{
    $_SESSION['admin_flash'] = [
        'type' => $type,
        'message' => $message,
    ];
}

function consume_admin_flash(): ?array
{
    $flash = $_SESSION['admin_flash'] ?? null;
    unset($_SESSION['admin_flash']);

    return is_array($flash) ? $flash : null;
}

function audit_admin_action(
    PDO $pdo,
    string $action,
    ?string $targetType = null,
    ?string $targetId = null,
    array $details = [],
): void {
    $statement = $pdo->prepare(
        "INSERT INTO admin_audit_logs
            (admin_username, action, target_type, target_id, details_json, created_at)
         VALUES
            (:admin_username, :action, :target_type, :target_id, :details_json, NOW())"
    );
    $statement->execute([
        'admin_username' => current_admin_username(),
        'action' => $action,
        'target_type' => $targetType,
        'target_id' => $targetId,
        'details_json' => $details === [] ? null : json_encode($details, JSON_UNESCAPED_SLASHES),
    ]);
}

function generate_smartvision_code(): string
{
    return generate_public_activation_code();
}

function admin_redirect(): never
{
    header('Location: /admin/');
    exit;
}

function admin_escape(?string $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

admin_security_headers();
start_admin_session();

if (realpath((string) ($_SERVER['SCRIPT_FILENAME'] ?? '')) === __FILE__) {
    http_response_code(404);
    exit;
}
