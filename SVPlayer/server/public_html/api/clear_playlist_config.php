<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/device_state.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['device_id'] ?? null);
$deviceToken = trim((string) ($input['device_token'] ?? ''));

if ($deviceId === '' || $deviceToken === '') {
    json_response(['success' => false, 'error' => 'Session appareil invalide.'], 400);
}

$pdo = db();

try {
    if (!has_valid_device_token($pdo, $deviceId, $deviceToken)) {
        json_response(['success' => false, 'error' => 'Appareil non autorise.'], 403);
    }

    $state = smartvision_device_state($pdo, $deviceId);
    if (!($state['found'] ?? false)) {
        json_response(['success' => false, 'error' => 'Appareil introuvable.'], 404);
    }
    if (($state['status'] ?? '') !== 'active') {
        json_response(['success' => false, 'error' => 'Appareil non active.'], 403);
    }

    $playlistQuery = $pdo->prepare(
        'SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1'
    );
    $playlistQuery->execute(['device_id' => $deviceId]);
    $encryptedPlaylist = $playlistQuery->fetchColumn();
    $playlist = is_string($encryptedPlaylist) && $encryptedPlaylist !== ''
        ? decrypt_playlist_config((string) $encryptedPlaylist)
        : null;

    if (!is_array($playlist)) {
        json_response([
            'success' => true,
            'cleared' => false,
            'playlist_configured' => false,
        ]);
    }

    if (!submitted_playlist_matches($playlist, $input)) {
        json_response([
            'success' => true,
            'cleared' => false,
            'playlist_configured' => true,
        ]);
    }

    $pdo->beginTransaction();
    $pdo->prepare('DELETE FROM device_playlist_configs WHERE device_id = :device_id')
        ->execute(['device_id' => $deviceId]);
    $pdo->prepare("UPDATE devices SET xtream_status = 'missing', updated_at = NOW() WHERE device_id = :device_id")
        ->execute(['device_id' => $deviceId]);
    $pdo->commit();

    json_response([
        'success' => true,
        'cleared' => true,
        'playlist_configured' => false,
    ]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision clear_playlist_config failed.');
    json_response(['success' => false, 'error' => 'Impossible de supprimer la configuration playlist.'], 500);
}

function submitted_playlist_matches(array $playlist, array $input): bool
{
    $submittedHost = normalize_xtream_host($input['host'] ?? $input['xtream_host'] ?? null);
    $submittedUsername = trim((string) ($input['username'] ?? $input['xtream_username'] ?? ''));
    $submittedPassword = trim((string) ($input['password'] ?? $input['xtream_password'] ?? ''));
    $submittedM3u = normalize_playlist_url($input['m3u_url'] ?? $input['m3uUrl'] ?? null);

    $hasSubmittedXtream = $submittedHost !== '' && $submittedUsername !== '' && $submittedPassword !== '';
    $hasSubmittedM3u = is_string($submittedM3u) && $submittedM3u !== '';
    if (!$hasSubmittedXtream && !$hasSubmittedM3u) {
        return false;
    }

    if ($hasSubmittedXtream) {
        $storedHost = normalize_xtream_host($playlist['host'] ?? null);
        $storedUsername = trim((string) ($playlist['username'] ?? ''));
        $storedPassword = trim((string) ($playlist['password'] ?? ''));
        if (
            hash_equals($storedHost, $submittedHost)
            && hash_equals($storedUsername, $submittedUsername)
            && hash_equals($storedPassword, $submittedPassword)
        ) {
            return true;
        }
    }

    if ($hasSubmittedM3u) {
        $storedM3u = normalize_playlist_url($playlist['m3u_url'] ?? $playlist['m3uUrl'] ?? null);
        if (is_string($storedM3u) && hash_equals($storedM3u, (string) $submittedM3u)) {
            return true;
        }
    }

    return false;
}
