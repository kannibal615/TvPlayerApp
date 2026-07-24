<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/device_profile_sync_policy.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['device_id'] ?? null);
$deviceToken = trim((string) ($input['device_token'] ?? ''));
$capabilityVersion = max(1, min(10, (int) ($input['capability_version'] ?? 1)));
$supportsCredentials = $capabilityVersion >= 2;
$profilesInput = is_array($input['profiles'] ?? null) ? array_slice($input['profiles'], 0, 50) : [];
error_log('SmartVision device_profiles request received (capability ' . $capabilityVersion . ').');

if ($deviceId === '' || $deviceToken === '') {
    json_response(['success' => false, 'error' => 'Identite appareil invalide.'], 400);
}

$pdo = db();
try {
    if (!has_valid_device_token($pdo, $deviceId, $deviceToken)) {
        error_log('SmartVision device_profiles token rejected.');
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
        $allowedTypes = device_profile_allowed_types($capabilityVersion);
        if ($profileId === '' || $name === null || !in_array($type, $allowedTypes, true)) {
            continue;
        }
        $source = strtolower(trim((string) ($profileInput['source'] ?? '')));
        $credentialsMode = strtolower(trim((string) ($profileInput['credentials_mode'] ?? '')));
        $credentialsPayload = null;
        if ($supportsCredentials && in_array($source, ['xtream', 'm3u'], true)) {
            if (!in_array($credentialsMode, ['custom', 'shared_with_admin'], true)) {
                $credentialsMode = 'custom';
            }
            $credentials = [
                'source' => $source,
                'host' => smartvision_text_substr(trim((string) ($profileInput['host'] ?? '')), 0, 500),
                'username' => smartvision_text_substr(trim((string) ($profileInput['username'] ?? '')), 0, 255),
                'password' => smartvision_text_substr(trim((string) ($profileInput['password'] ?? '')), 0, 255),
                'epg_url' => smartvision_text_substr(trim((string) ($profileInput['epg_url'] ?? '')), 0, 1000),
                'm3u_url' => smartvision_text_substr(trim((string) ($profileInput['m3u_url'] ?? '')), 0, 2000),
            ];
            $hasXtream = $source === 'xtream'
                && $credentials['host'] !== ''
                && $credentials['username'] !== ''
                && $credentials['password'] !== '';
            $hasM3u = $source === 'm3u' && $credentials['m3u_url'] !== '';
            if ($hasXtream || $hasM3u) {
                $credentialsPayload = encrypt_playlist_config($credentials);
            }
        }
        $profiles[$profileId] = [
            'profile_id' => $profileId,
            'name' => $name,
            'type' => $type,
            'source' => in_array($source, ['xtream', 'm3u'], true) ? $source : null,
            'credentials_mode' => in_array($credentialsMode, ['custom', 'shared_with_admin'], true) ? $credentialsMode : null,
            'credentials_payload' => $credentialsPayload,
        ];
    }

    $pdo->beginTransaction();
    $registry = $pdo->prepare(
        "INSERT INTO device_profile_registry (device_id, capability_version, synced_at, updated_at)
         VALUES (:device_id, :capability_version, NOW(), NOW())
         ON DUPLICATE KEY UPDATE capability_version = VALUES(capability_version), synced_at = NOW(), updated_at = NOW()"
    );
    $registry->execute(['device_id' => $deviceId, 'capability_version' => $capabilityVersion]);
    if ($supportsCredentials) {
        $pdo->prepare('DELETE FROM device_playlist_profiles WHERE device_id = :device_id')
            ->execute(['device_id' => $deviceId]);
        $insert = $pdo->prepare(
            "INSERT INTO device_playlist_profiles
                (device_id, profile_id, profile_name, profile_type, source, credentials_mode, credentials_payload, updated_at)
             VALUES
                (:device_id, :profile_id, :profile_name, :profile_type, :source, :credentials_mode, :credentials_payload, NOW())"
        );
        foreach ($profiles as $profile) {
            $insert->execute([
                'device_id' => $deviceId,
                'profile_id' => $profile['profile_id'],
                'profile_name' => $profile['name'],
                'profile_type' => $profile['type'],
                'source' => $profile['source'],
                'credentials_mode' => $profile['credentials_mode'],
                'credentials_payload' => $profile['credentials_payload'],
            ]);
        }
    } else {
        $insert = $pdo->prepare(
            "INSERT INTO device_playlist_profiles (device_id, profile_id, profile_name, profile_type, updated_at)
             VALUES (:device_id, :profile_id, :profile_name, :profile_type, NOW())
             ON DUPLICATE KEY UPDATE profile_name = VALUES(profile_name), profile_type = VALUES(profile_type), updated_at = NOW()"
        );
        foreach ($profiles as $profile) {
            $insert->execute([
                'device_id' => $deviceId,
                'profile_id' => $profile['profile_id'],
                'profile_name' => $profile['name'],
                'profile_type' => $profile['type'],
            ]);
        }
        if ($profiles === []) {
            $pdo->prepare(
                'DELETE FROM device_playlist_profiles WHERE device_id = :device_id AND credentials_payload IS NULL'
            )->execute(['device_id' => $deviceId]);
        }
    }

    if ($supportsCredentials) {
        $resolvedProfiles = [];
        foreach ($profiles as $profile) {
            if ($profile['credentials_payload'] !== null) {
                $resolved = decrypt_playlist_config($profile['credentials_payload']);
                if (is_array($resolved)) {
                    $resolvedProfiles[] = $resolved;
                }
            }
        }
        $serverConfigQuery = $pdo->prepare(
            'SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1'
        );
        $serverConfigQuery->execute(['device_id' => $deviceId]);
        $serverPayload = $serverConfigQuery->fetchColumn();
        $serverConfig = is_string($serverPayload) && $serverPayload !== ''
            ? (decrypt_playlist_config($serverPayload) ?? [])
            : [];
        $pdo->prepare(
            "UPDATE devices SET xtream_status = :xtream_status, updated_at = NOW() WHERE device_id = :device_id"
        )->execute([
            'xtream_status' => device_profile_resolve_xtream_status($resolvedProfiles, $serverConfig),
            'device_id' => $deviceId,
        ]);
    }
    $pdo->commit();

    json_response(['success' => true, 'profile_count' => count($profiles)]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision device_profiles failed: ' . $exception->getMessage());
    json_response(['success' => false, 'error' => 'Synchronisation des profils indisponible.'], 500);
}
