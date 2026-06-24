<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

$deviceId = clean_device_id($_GET['device_id'] ?? null);
$publicDeviceCode = clean_public_device_code($_GET['public_device_code'] ?? null);

if ($deviceId === '' && $publicDeviceCode === '') {
    json_response([
        'success' => false,
        'error' => 'Identifiant appareil requis.',
    ], 400);
}

try {
    $pdo = db();
    ensure_app_notifications_table($pdo);
    $associatedTargets = notification_associated_user_targets($pdo, $deviceId);
    $deviceTargets = array_filter([$deviceId, $publicDeviceCode], static fn(string $value): bool => $value !== '');
    $statement = $pdo->query(
        "SELECT id, title, message, target_scope, target_value, priority, expires_at, created_at
         FROM app_notifications
         WHERE status = 'active'
           AND (expires_at IS NULL OR expires_at > NOW())
         ORDER BY FIELD(priority, 'urgent', 'important', 'normal'), id DESC
         LIMIT 80"
    );
    $rows = $statement->fetchAll();
    $notifications = [];

    foreach ($rows as $row) {
        if (!notification_matches_target($row, $deviceTargets, $associatedTargets)) {
            continue;
        }
        $notifications[] = [
            'id' => (int) $row['id'],
            'title' => (string) $row['title'],
            'message' => (string) $row['message'],
            'priority' => (string) $row['priority'],
            'created_at' => (string) $row['created_at'],
            'expires_at' => $row['expires_at'] === null ? null : (string) $row['expires_at'],
        ];
    }

    json_response([
        'success' => true,
        'notifications' => $notifications,
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision notifications failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de charger les notifications.',
    ], 500);
}

function notification_associated_user_targets(PDO $pdo, string $deviceId): array
{
    if ($deviceId === '') {
        return [];
    }
    $statement = $pdo->prepare(
        "SELECT DISTINCT u.id, u.email
         FROM device_activations da
         INNER JOIN activation_orders o ON o.activation_code_id = da.activation_code_id
         INNER JOIN site_users u ON u.id = o.user_id
         WHERE da.device_id = :device_id"
    );
    $statement->execute(['device_id' => $deviceId]);
    $targets = [];
    foreach ($statement->fetchAll() as $row) {
        $targets[] = (string) $row['id'];
        $targets[] = strtolower((string) $row['email']);
    }

    return array_values(array_unique(array_filter($targets)));
}

function notification_matches_target(array $row, array $deviceTargets, array $userTargets): bool
{
    $scope = (string) ($row['target_scope'] ?? 'all');
    if ($scope === 'all') {
        return true;
    }
    $targets = notification_target_list((string) ($row['target_value'] ?? ''));
    if ($targets === []) {
        return false;
    }
    $lookup = array_flip($targets);
    if ($scope === 'devices') {
        foreach ($deviceTargets as $target) {
            if (isset($lookup[strtolower($target)])) {
                return true;
            }
        }
    }
    if ($scope === 'users') {
        foreach ($userTargets as $target) {
            if (isset($lookup[strtolower($target)])) {
                return true;
            }
        }
    }

    return false;
}

function notification_target_list(string $value): array
{
    $parts = preg_split('/[\s,;]+/', strtolower($value));
    if (!is_array($parts)) {
        return [];
    }

    return array_values(array_unique(array_filter(array_map('trim', $parts))));
}
