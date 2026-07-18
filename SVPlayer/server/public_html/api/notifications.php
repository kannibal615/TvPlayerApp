<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

if (!in_array(($_SERVER['REQUEST_METHOD'] ?? ''), ['GET', 'POST'], true)) {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = ($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' ? read_json_input() : [];
$deviceId = clean_device_id($_GET['device_id'] ?? ($input['device_id'] ?? null));
$publicDeviceCode = clean_public_device_code($_GET['public_device_code'] ?? ($input['public_device_code'] ?? null));
$deviceToken = trim((string) ($_GET['device_token'] ?? ($input['device_token'] ?? '')));
$appVersionCode = max(0, (int) ($_GET['app_version_code'] ?? ($input['app_version_code'] ?? 0)));

if ($deviceId === '' && $publicDeviceCode === '') {
    json_response(['success' => false, 'error' => 'Identifiant appareil requis.'], 400);
}

try {
    $pdo = db();
    ensure_app_notifications_table($pdo);
    $detailsAuthorized = $deviceId !== '' && has_validated_device_token($pdo, $deviceId, $deviceToken);
    $deviceTargets = array_filter([$deviceId, $publicDeviceCode], static fn(string $value): bool => $value !== '');
    $candidateRows = notification_candidate_rows($pdo);
    $associatedTargets = notification_rows_need_user_targets($candidateRows)
        ? notification_associated_user_targets($pdo, $deviceId)
        : [];
    $currentRows = notification_visible_rows($candidateRows, $deviceTargets, $associatedTargets, $appVersionCode);
    $currentIds = array_map(static fn(array $row): int => (int) $row['id'], $currentRows);
    $receiptLookup = notification_receipt_lookup($pdo, $deviceId, $currentIds);
    $currentRows = array_values(array_filter($currentRows, static function (array $row) use ($receiptLookup): bool {
        $receipt = $receiptLookup[(int) $row['id']] ?? null;
        return $receipt === null;
    }));
    $historyRows = notification_history_rows($pdo, $deviceId, $appVersionCode);
    $visibleRows = array_merge($currentRows, $historyRows);

    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        $action = (string) ($input['action'] ?? 'mark_seen');
        if ($action !== 'mark_seen') {
            json_response(['success' => false, 'error' => 'Action inconnue.'], 400);
        }
        $requestedIds = notification_requested_ids($input['notification_ids'] ?? null);
        $visibleIds = array_map(static fn(array $row): int => (int) $row['id'], $visibleRows);
        $idsToMark = $requestedIds === []
            ? array_map(static fn(array $row): int => (int) $row['id'], $currentRows)
            : array_values(array_intersect($visibleIds, $requestedIds));
        notification_mark_seen($pdo, $deviceId, $idsToMark);
        json_response([
            'success' => true,
            'marked_seen' => count($idsToMark),
            'unread_count' => max(0, count($currentRows) - count(array_intersect($currentIds, $idsToMark))),
        ]);
    }

    $notifications = [];
    foreach ($visibleRows as $row) {
        $type = notification_resolved_type($row);
        $seenAt = $row['seen_at'] ?? null;
        $notifications[] = [
            'id' => (int) $row['id'],
            'type' => $type,
            'title' => (string) $row['title'],
            'message' => (string) $row['message'],
            'priority' => (string) $row['priority'],
            'created_at' => (string) $row['created_at'],
            'expires_at' => $row['expires_at'] === null ? null : (string) $row['expires_at'],
            'seen' => $seenAt !== null,
            'seen_at' => $seenAt === null ? null : (string) $seenAt,
            'source_version_code' => $row['source_version_code'] === null
                ? notification_version_code($row)
                : (int) $row['source_version_code'],
            'details' => $detailsAuthorized ? notification_details($row) : null,
        ];
    }

    json_response([
        'success' => true,
        'unread_count' => count($currentRows),
        'notifications' => $notifications,
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision notifications failed.');
    json_response(['success' => false, 'error' => 'Impossible de charger les notifications.'], 500);
}

function notification_candidate_rows(PDO $pdo): array
{
    return $pdo->query(
        "SELECT id, title, message, notification_type, source_version_code, payload_ciphertext, created_by,
                target_scope, target_value, priority, status, expires_at, created_at, NULL AS seen_at
         FROM app_notifications
         WHERE status = 'active'
           AND (expires_at IS NULL OR expires_at > NOW())
         ORDER BY id DESC
         LIMIT 240"
    )->fetchAll();
}

function notification_history_rows(PDO $pdo, string $deviceId, int $appVersionCode): array
{
    if ($deviceId === '') {
        return [];
    }
    $statement = $pdo->prepare(
        "SELECT n.id, n.title, n.message, n.notification_type, n.source_version_code, n.payload_ciphertext, n.created_by,
                n.target_scope, n.target_value, n.priority, n.status, n.expires_at, n.created_at, r.seen_at
         FROM app_notification_receipts r
         INNER JOIN app_notifications n ON n.id = r.notification_id
         WHERE r.device_id = :device_id
           AND r.purged_at IS NULL
         ORDER BY r.seen_at DESC, n.id DESC
         LIMIT 80"
    );
    $statement->execute(['device_id' => $deviceId]);
    return array_values(array_filter($statement->fetchAll(), static function (array $row) use ($appVersionCode): bool {
        return !notification_is_installed_update($row, $appVersionCode);
    }));
}

function notification_rows_need_user_targets(array $rows): bool
{
    foreach ($rows as $row) {
        if ((string) ($row['target_scope'] ?? '') === 'users') return true;
    }
    return false;
}

function notification_visible_rows(array $rows, array $deviceTargets, array $associatedTargets, int $appVersionCode): array
{
    $visible = [];
    foreach ($rows as $row) {
        if (!notification_matches_target($row, $deviceTargets, $associatedTargets)) continue;
        if (notification_is_installed_update($row, $appVersionCode)) continue;
        $visible[] = $row;
    }
    $priorityOrder = ['urgent' => 0, 'important' => 1, 'normal' => 2];
    usort($visible, static function (array $left, array $right) use ($priorityOrder): int {
        $leftPriority = $priorityOrder[(string) ($left['priority'] ?? 'normal')] ?? 2;
        $rightPriority = $priorityOrder[(string) ($right['priority'] ?? 'normal')] ?? 2;
        return $leftPriority === $rightPriority
            ? ((int) $right['id'] <=> (int) $left['id'])
            : ($leftPriority <=> $rightPriority);
    });
    return array_slice($visible, 0, 80);
}

function notification_resolved_type(array $row): string
{
    $stored = (string) ($row['notification_type'] ?? '');
    if (in_array($stored, ['app_update', 'playlist_added'], true)) return $stored;
    $createdBy = strtolower((string) ($row['created_by'] ?? ''));
    $title = strtolower((string) ($row['title'] ?? ''));
    $message = strtolower((string) ($row['message'] ?? ''));
    if ($createdBy === 'deploy_script' || (str_contains($title, 'update') && str_contains($message, 'smartvision'))) {
        return 'app_update';
    }
    if (str_starts_with($createdBy, 'system_playlist') || str_contains($title, 'playlist')) {
        return 'playlist_added';
    }
    return 'important_info';
}

function notification_version_code(array $row): ?int
{
    $stored = (int) ($row['source_version_code'] ?? 0);
    if ($stored > 0) return $stored;
    if (preg_match('/\((\d+)\)/', (string) ($row['message'] ?? ''), $matches) === 1) {
        return (int) $matches[1];
    }
    return null;
}

function notification_is_installed_update(array $row, int $appVersionCode): bool
{
    if (notification_resolved_type($row) !== 'app_update' || $appVersionCode <= 0) return false;
    $versionCode = notification_version_code($row);
    return $versionCode !== null && $versionCode <= $appVersionCode;
}

function notification_details(array $row): ?array
{
    $ciphertext = (string) ($row['payload_ciphertext'] ?? '');
    if ($ciphertext === '') return null;
    return decrypt_playlist_config($ciphertext);
}

function notification_receipt_lookup(PDO $pdo, string $deviceId, array $notificationIds): array
{
    if ($deviceId === '' || $notificationIds === []) return [];
    $placeholders = implode(',', array_fill(0, count($notificationIds), '?'));
    $statement = $pdo->prepare(
        "SELECT notification_id, seen_at, purged_at FROM app_notification_receipts
         WHERE device_id = ? AND notification_id IN ($placeholders)"
    );
    $statement->execute(array_merge([$deviceId], $notificationIds));
    $receipts = [];
    foreach ($statement->fetchAll() as $row) {
        $receipts[(int) $row['notification_id']] = $row;
    }
    return $receipts;
}

function notification_mark_seen(PDO $pdo, string $deviceId, array $notificationIds): void
{
    if ($deviceId === '' || $notificationIds === []) return;
    $statement = $pdo->prepare(
        "INSERT INTO app_notification_receipts (notification_id, device_id, seen_at, purged_at)
         VALUES (:notification_id, :device_id, NOW(), NULL)
         ON DUPLICATE KEY UPDATE seen_at = VALUES(seen_at)"
    );
    foreach (array_values(array_unique(array_map('intval', $notificationIds))) as $id) {
        if ($id > 0) $statement->execute(['notification_id' => $id, 'device_id' => $deviceId]);
    }
}

function notification_requested_ids(mixed $value): array
{
    if (!is_array($value)) return [];
    return array_values(array_filter(array_map('intval', $value), static fn(int $id): bool => $id > 0));
}

function notification_associated_user_targets(PDO $pdo, string $deviceId): array
{
    if ($deviceId === '') return [];
    $statement = $pdo->prepare(
        "SELECT u.id, u.email
         FROM device_activations da
         INNER JOIN activation_orders o ON o.activation_code_id = da.activation_code_id
         INNER JOIN site_users u ON u.id = o.user_id
         WHERE da.device_id = :device_id
           AND da.activation_code_id IS NOT NULL
           AND da.status = 'active'
         ORDER BY da.id DESC LIMIT 20"
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
    if ($scope === 'all') return true;
    $targets = notification_target_list((string) ($row['target_value'] ?? ''));
    if ($targets === []) return false;
    $lookup = array_flip($targets);
    $candidates = $scope === 'devices' ? $deviceTargets : ($scope === 'users' ? $userTargets : []);
    foreach ($candidates as $target) {
        if (isset($lookup[strtolower($target)])) return true;
    }
    return false;
}

function notification_target_list(string $value): array
{
    $parts = preg_split('/[\s,;]+/', strtolower($value));
    if (!is_array($parts)) return [];
    return array_values(array_unique(array_filter(array_map('trim', $parts))));
}
