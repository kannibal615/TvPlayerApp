<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

if (!in_array(($_SERVER['REQUEST_METHOD'] ?? ''), ['GET', 'POST'], true)) {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

$input = ($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' ? read_json_input() : [];
$deviceId = clean_device_id($_GET['device_id'] ?? ($input['device_id'] ?? null));
$publicDeviceCode = clean_public_device_code($_GET['public_device_code'] ?? ($input['public_device_code'] ?? null));

if ($deviceId === '' && $publicDeviceCode === '') {
    json_response([
        'success' => false,
        'error' => 'Identifiant appareil requis.',
    ], 400);
}

try {
    $pdo = db();
    ensure_app_notifications_table($pdo);
    $deviceTargets = array_filter([$deviceId, $publicDeviceCode], static fn(string $value): bool => $value !== '');
    $candidateRows = notification_candidate_rows($pdo);
    $associatedTargets = notification_rows_need_user_targets($candidateRows)
        ? notification_associated_user_targets($pdo, $deviceId)
        : [];
    $visibleRows = notification_visible_rows($candidateRows, $deviceTargets, $associatedTargets);

    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        $action = (string) ($input['action'] ?? 'mark_seen');
        if ($action !== 'mark_seen') {
            json_response(['success' => false, 'error' => 'Action inconnue.'], 400);
        }
        $ids = notification_requested_ids($input['notification_ids'] ?? null);
        $visibleIds = array_map(static fn(array $row): int => (int) $row['id'], $visibleRows);
        $idsToMark = $ids === [] ? $visibleIds : array_values(array_intersect($visibleIds, $ids));
        notification_mark_seen($pdo, $deviceId, $idsToMark);
        json_response([
            'success' => true,
            'marked_seen' => count($idsToMark),
            'unread_count' => 0,
        ]);
    }

    $seenLookup = notification_seen_lookup($pdo, $deviceId, array_map(static fn(array $row): int => (int) $row['id'], $visibleRows));
    $notifications = [];
    $unreadCount = 0;

    foreach ($visibleRows as $row) {
        $seen = isset($seenLookup[(int) $row['id']]);
        if (!$seen) {
            $unreadCount++;
        }
        $notifications[] = [
            'id' => (int) $row['id'],
            'title' => (string) $row['title'],
            'message' => (string) $row['message'],
            'priority' => (string) $row['priority'],
            'created_at' => (string) $row['created_at'],
            'expires_at' => $row['expires_at'] === null ? null : (string) $row['expires_at'],
            'seen' => $seen,
        ];
    }

    json_response([
        'success' => true,
        'unread_count' => $unreadCount,
        'notifications' => $notifications,
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision notifications failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de charger les notifications.',
    ], 500);
}

function notification_candidate_rows(PDO $pdo): array
{
    $statement = $pdo->query(
        "SELECT id, title, message, target_scope, target_value, priority, expires_at, created_at
         FROM app_notifications
         WHERE status = 'active'
           AND (expires_at IS NULL OR expires_at > NOW())
         ORDER BY FIELD(priority, 'urgent', 'important', 'normal'), id DESC
         LIMIT 80"
    );
    return $statement->fetchAll();
}

function notification_rows_need_user_targets(array $rows): bool
{
    foreach ($rows as $row) {
        if ((string) ($row['target_scope'] ?? '') === 'users') {
            return true;
        }
    }

    return false;
}

function notification_visible_rows(array $rows, array $deviceTargets, array $associatedTargets): array
{
    $visible = [];

    foreach ($rows as $row) {
        if (notification_is_release_update($row)) {
            continue;
        }
        if (!notification_matches_target($row, $deviceTargets, $associatedTargets)) {
            continue;
        }
        $visible[] = $row;
    }

    return $visible;
}

function notification_is_release_update(array $row): bool
{
    $title = strtolower((string) ($row['title'] ?? ''));
    $message = strtolower((string) ($row['message'] ?? ''));

    return str_contains($title, 'update available')
        && str_contains($message, 'smartvision')
        && str_contains($message, 'install the update');
}

function notification_seen_lookup(PDO $pdo, string $deviceId, array $notificationIds): array
{
    if ($deviceId === '' || $notificationIds === []) {
        return [];
    }
    $placeholders = implode(',', array_fill(0, count($notificationIds), '?'));
    $statement = $pdo->prepare(
        "SELECT notification_id FROM app_notification_receipts
         WHERE device_id = ? AND notification_id IN ($placeholders)"
    );
    $statement->execute(array_merge([$deviceId], $notificationIds));
    $seen = [];
    foreach ($statement->fetchAll(PDO::FETCH_COLUMN) as $id) {
        $seen[(int) $id] = true;
    }

    return $seen;
}

function notification_mark_seen(PDO $pdo, string $deviceId, array $notificationIds): void
{
    if ($deviceId === '' || $notificationIds === []) {
        return;
    }
    $statement = $pdo->prepare(
        "INSERT INTO app_notification_receipts (notification_id, device_id, seen_at)
         VALUES (:notification_id, :device_id, NOW())
         ON DUPLICATE KEY UPDATE seen_at = VALUES(seen_at)"
    );
    foreach (array_values(array_unique(array_map('intval', $notificationIds))) as $id) {
        if ($id > 0) {
            $statement->execute(['notification_id' => $id, 'device_id' => $deviceId]);
        }
    }
}

function notification_requested_ids(mixed $value): array
{
    if (!is_array($value)) {
        return [];
    }

    return array_values(array_filter(array_map('intval', $value), static fn(int $id): bool => $id > 0));
}

function notification_associated_user_targets(PDO $pdo, string $deviceId): array
{
    if ($deviceId === '') {
        return [];
    }
    $statement = $pdo->prepare(
        "SELECT u.id, u.email
         FROM device_activations da
         INNER JOIN activation_orders o ON o.activation_code_id = da.activation_code_id
         INNER JOIN site_users u ON u.id = o.user_id
         WHERE da.device_id = :device_id
           AND da.activation_code_id IS NOT NULL
           AND da.status = 'active'
         ORDER BY da.id DESC
         LIMIT 20"
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
