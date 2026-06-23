<?php
declare(strict_types=1);

function smartvision_device_state(PDO $pdo, string $deviceId): array
{
    $deviceQuery = $pdo->prepare(
        'SELECT device_id, public_device_code, status
         FROM devices
         WHERE device_id = :device_id
         LIMIT 1'
    );
    $deviceQuery->execute(['device_id' => $deviceId]);
    $device = $deviceQuery->fetch();

    if (!$device) {
        return [
            'found' => false,
            'success' => false,
            'error' => 'Appareil introuvable.',
        ];
    }

    if (($device['status'] ?? '') === 'blocked') {
        return [
            'found' => true,
            'success' => true,
            'status' => 'blocked',
            'activated' => false,
            'serverDeviceId' => $deviceId,
            'device_id' => $deviceId,
            'publicDeviceCode' => (string) ($device['public_device_code'] ?? ''),
            'licenseStatus' => 'blocked',
            'trialStatus' => 'used',
            'freeWithAdsStatus' => 'inactive',
            'xtreamStatus' => 'missing',
            'playlist_configured' => false,
        ];
    }

    $pdo->prepare(
        "UPDATE device_activations
         SET status = 'expired'
         WHERE device_id = :device_id
           AND status = 'active'
           AND expires_at <= NOW()"
    )->execute(['device_id' => $deviceId]);

    $activationQuery = $pdo->prepare(
        "SELECT activation_type, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND status = 'active'
           AND expires_at > NOW()
         ORDER BY
           CASE activation_type
             WHEN 'smartvision_code' THEN 1
             WHEN 'own_xtream' THEN 2
             WHEN 'trial_demo' THEN 3
             WHEN 'free_ads' THEN 4
             ELSE 9
           END,
           expires_at DESC
         LIMIT 1"
    );
    $activationQuery->execute(['device_id' => $deviceId]);
    $activation = $activationQuery->fetch();

    $trialQuery = $pdo->prepare(
        "SELECT status, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND activation_type = 'trial_demo'
         ORDER BY id DESC
         LIMIT 1"
    );
    $trialQuery->execute(['device_id' => $deviceId]);
    $trial = $trialQuery->fetch();

    $playlistQuery = $pdo->prepare(
        'SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1'
    );
    $playlistQuery->execute(['device_id' => $deviceId]);
    $encryptedPlaylist = $playlistQuery->fetchColumn();
    $playlistConfigured = is_string($encryptedPlaylist) && $encryptedPlaylist !== '';
    $xtreamStatus = $playlistConfigured ? 'configured' : 'missing';

    $status = 'pending';
    $activated = false;
    $activationType = null;
    $expiresAt = null;
    $licenseStatus = 'inactive';
    $trialStatus = 'available';
    $freeWithAdsStatus = 'inactive';

    if (is_array($trial)) {
        $trialStatus = (($trial['status'] ?? '') === 'active' && strtotime((string) $trial['expires_at']) > time())
            ? 'active'
            : 'expired';
    }

    if (is_array($activation)) {
        $status = 'active';
        $activated = true;
        $activationType = (string) $activation['activation_type'];
        $expiresAt = (string) $activation['expires_at'];

        if ($activationType === 'smartvision_code' || $activationType === 'own_xtream') {
            $licenseStatus = 'active';
            $trialStatus = $trialStatus === 'active' ? 'used' : $trialStatus;
            $freeWithAdsStatus = 'inactive';
        } elseif ($activationType === 'trial_demo') {
            $trialStatus = 'active';
        } elseif ($activationType === 'free_ads') {
            $freeWithAdsStatus = 'active';
        }
    } elseif ($trialStatus === 'expired') {
        $status = 'expired';
    }

    $update = $pdo->prepare(
        "UPDATE devices
         SET status = :status,
             license_status = :license_status,
             trial_status = :trial_status,
             free_with_ads_status = :free_with_ads_status,
             xtream_status = :xtream_status,
             activated_at = CASE WHEN :activated = 1 THEN COALESCE(activated_at, NOW()) ELSE activated_at END,
             expires_at = :expires_at,
             last_seen_at = NOW(),
             updated_at = NOW()
         WHERE device_id = :device_id
           AND status <> 'blocked'"
    );
    $update->execute([
        'status' => $status,
        'license_status' => $licenseStatus,
        'trial_status' => $trialStatus,
        'free_with_ads_status' => $freeWithAdsStatus,
        'xtream_status' => $xtreamStatus,
        'activated' => $activated ? 1 : 0,
        'expires_at' => $expiresAt,
        'device_id' => $deviceId,
    ]);

    return [
        'found' => true,
        'success' => true,
        'status' => $status,
        'activated' => $activated,
        'serverDeviceId' => $deviceId,
        'device_id' => $deviceId,
        'publicDeviceCode' => (string) ($device['public_device_code'] ?? ''),
        'expires_at' => $expiresAt,
        'activation_type' => $activationType,
        'licenseStatus' => $licenseStatus,
        'trialStatus' => $trialStatus,
        'freeWithAdsStatus' => $freeWithAdsStatus,
        'xtreamStatus' => $xtreamStatus,
        'playlist_configured' => $playlistConfigured,
    ];
}
