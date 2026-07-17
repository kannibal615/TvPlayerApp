<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['device_id'] ?? null);
$deviceToken = trim((string) ($input['device_token'] ?? ''));
$capabilityVersion = max(1, min(10, (int) ($input['capability_version'] ?? 1)));
$profilesInput = is_array($input['profiles'] ?? null) ? array_slice($input['profiles'], 0, 50) : [];

if ($deviceId === '' || $deviceToken === '') {
    json_response(['success' => false, 'error' => 'Identite appareil invalide.'], 400);
}

$pdo = db();
try {
    if (!has_valid_device_token($pdo, $deviceId, $deviceToken)) {
        json_response(['success' => false, 'error' => 'Session appareil invalide.'], 403);
    }

    $profiles = [];
    foreach ($profilesInput as $profileInput) {
        if (!is_array($profileInput)) {
            continue;
        }
        $profileId = clean_device_id($profileInput['profile_id'] ?? null);
        $name = clean_optional_text($profileInput['name'] ?? null, 60);
        $type = strtolower(trim((string) ($profileInput['type'] ?? '')));
        if ($profileId === '' || $name === null || !in_array($type, ['admin', 'normal'], true)) {
            continue;
        }
        $profiles[$profileId] = ['profile_id' => $profileId, 'name' => $name, 'type' => $type];
    }

    $pdo->beginTransaction();
    $registry = $pdo->prepare(
        "INSERT INTO device_profile_registry (device_id, capability_version, synced_at, updated_at)
         VALUES (:device_id, :capability_version, NOW(), NOW())
         ON DUPLICATE KEY UPDATE capability_version = VALUES(capability_version), synced_at = NOW(), updated_at = NOW()"
    );
    $registry->execute(['device_id' => $deviceId, 'capability_version' => $capabilityVersion]);
    $pdo->prepare('DELETE FROM device_playlist_profiles WHERE device_id = :device_id')
        ->execute(['device_id' => $deviceId]);
    $insert = $pdo->prepare(
        "INSERT INTO device_playlist_profiles (device_id, profile_id, profile_name, profile_type, updated_at)
         VALUES (:device_id, :profile_id, :profile_name, :profile_type, NOW())"
    );
    foreach ($profiles as $profile) {
        $insert->execute([
            'device_id' => $deviceId,
            'profile_id' => $profile['profile_id'],
            'profile_name' => $profile['name'],
            'profile_type' => $profile['type'],
        ]);
    }
    $pdo->commit();

    json_response(['success' => true, 'profile_count' => count($profiles)]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision device_profiles failed.');
    json_response(['success' => false, 'error' => 'Synchronisation des profils indisponible.'], 500);
}

