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
$deviceToken = trim((string) ($_GET['device_token'] ?? ''));

if ($deviceId === '') {
    json_response([
        'success' => false,
        'error' => 'device_id requis.',
    ], 400);
}

$pdo = db();

try {
    $deviceQuery = $pdo->prepare('SELECT device_id, status FROM devices WHERE device_id = :device_id LIMIT 1');
    $deviceQuery->execute(['device_id' => $deviceId]);
    $device = $deviceQuery->fetch();

    if (!$device) {
        json_response([
            'success' => false,
            'error' => 'Appareil introuvable.',
        ], 404);
    }

    if (($device['status'] ?? '') === 'blocked') {
        json_response([
            'success' => true,
            'status' => 'blocked',
            'activated' => false,
        ]);
    }

    $activationQuery = $pdo->prepare(
        "SELECT activation_type, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND status = 'active'
         ORDER BY expires_at DESC
         LIMIT 1"
    );
    $activationQuery->execute(['device_id' => $deviceId]);
    $activation = $activationQuery->fetch();

    if (!$activation) {
        $pendingUpdate = $pdo->prepare(
            "UPDATE devices
             SET status = 'pending', last_seen_at = NOW(), updated_at = NOW()
             WHERE device_id = :device_id
               AND status <> 'blocked'"
        );
        $pendingUpdate->execute(['device_id' => $deviceId]);

        json_response([
            'success' => true,
            'status' => 'pending',
            'activated' => false,
        ]);
    }

    $expiresAt = (string) $activation['expires_at'];
    $isExpired = strtotime($expiresAt) !== false && strtotime($expiresAt) <= time();

    if ($isExpired) {
        $pdo->beginTransaction();

        $expireActivation = $pdo->prepare(
            "UPDATE device_activations
             SET status = 'expired'
             WHERE device_id = :device_id
               AND status = 'active'
               AND expires_at <= NOW()"
        );
        $expireActivation->execute(['device_id' => $deviceId]);

        $expireDevice = $pdo->prepare(
            "UPDATE devices
             SET status = 'expired', last_seen_at = NOW(), updated_at = NOW()
             WHERE device_id = :device_id"
        );
        $expireDevice->execute(['device_id' => $deviceId]);

        $pdo->commit();

        json_response([
            'success' => true,
            'status' => 'expired',
            'activated' => false,
        ]);
    }

    $activeUpdate = $pdo->prepare(
        "UPDATE devices
         SET status = 'active',
             activated_at = COALESCE(activated_at, NOW()),
             expires_at = :expires_at,
             last_seen_at = NOW(),
             updated_at = NOW()
         WHERE device_id = :device_id"
    );
    $activeUpdate->execute([
        'device_id' => $deviceId,
        'expires_at' => $expiresAt,
    ]);

    $playlistQuery = $pdo->prepare(
        'SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1'
    );
    $playlistQuery->execute(['device_id' => $deviceId]);
    $encryptedPlaylist = $playlistQuery->fetchColumn();
    $playlistConfigured = is_string($encryptedPlaylist) && $encryptedPlaylist !== '';

    $response = [
        'success' => true,
        'status' => 'active',
        'activated' => true,
        'expires_at' => $expiresAt,
        'activation_type' => $activation['activation_type'],
        'playlist_configured' => $playlistConfigured,
    ];

    if ($playlistConfigured && has_valid_device_token($pdo, $deviceId, $deviceToken)) {
        $playlist = decrypt_playlist_config((string) $encryptedPlaylist);
        if ($playlist !== null) {
            $response['playlist_config'] = $playlist;
            $delivered = $pdo->prepare(
                'UPDATE device_playlist_configs SET delivered_at = NOW() WHERE device_id = :device_id'
            );
            $delivered->execute(['device_id' => $deviceId]);
        }
    }

    json_response($response);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }

    error_log('SmartVision device_status failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de lire le statut appareil.',
    ], 500);
}
