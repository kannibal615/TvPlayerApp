<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/device_state.php';

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
    $state = smartvision_device_state($pdo, $deviceId);
    if (!($state['found'] ?? false)) {
        json_response([
            'success' => false,
            'error' => 'Appareil introuvable.',
        ], 404);
    }

    if (($state['status'] ?? '') !== 'blocked') {
        $playlistQuery = $pdo->prepare(
            'SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1'
        );
        $playlistQuery->execute(['device_id' => $deviceId]);
        $encryptedPlaylist = $playlistQuery->fetchColumn();
        $playlist = is_string($encryptedPlaylist) && $encryptedPlaylist !== ''
            ? decrypt_playlist_config((string) $encryptedPlaylist)
            : null;
        $playlistConfigured = is_array($playlist)
            && trim((string) ($playlist['host'] ?? '')) !== ''
            && trim((string) ($playlist['username'] ?? '')) !== ''
            && trim((string) ($playlist['password'] ?? '')) !== '';
        $state['playlist_configured'] = $playlistConfigured;
        $state['xtreamStatus'] = $playlistConfigured ? 'configured' : 'missing';

        if (is_array($playlist) && has_valid_device_token($pdo, $deviceId, $deviceToken)) {
            $state['playlist_config'] = $playlist;
            $delivered = $pdo->prepare(
                'UPDATE device_playlist_configs SET delivered_at = NOW() WHERE device_id = :device_id'
            );
            $delivered->execute(['device_id' => $deviceId]);
        }
    }

    unset($state['found']);
    json_response($state);
} catch (Throwable $exception) {
    error_log('SmartVision device_status failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de lire le statut appareil.',
    ], 500);
}
