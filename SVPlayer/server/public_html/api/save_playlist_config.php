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
$publicDeviceCode = clean_public_device_code($input['publicDeviceCode'] ?? $input['device'] ?? null);
$shortCode = normalize_activation_code($input['short_code'] ?? null);
$hostInput = trim((string) ($input['host'] ?? ''));
$usernameInput = trim((string) ($input['username'] ?? ''));
$passwordInput = trim((string) ($input['password'] ?? ''));
$clearEpg = filter_var($input['clear_epg'] ?? false, FILTER_VALIDATE_BOOLEAN);
$hasXtreamInput = $hostInput !== '' || $usernameInput !== '' || $passwordInput !== '';
$host = $hasXtreamInput ? normalize_xtream_host($hostInput) : null;
$username = $hasXtreamInput ? clean_optional_text($usernameInput, 180) : null;
$password = $hasXtreamInput ? clean_optional_text($passwordInput, 255) : null;
$epgProvided = array_key_exists('epg_url', $input) || array_key_exists('epgUrl', $input) || $clearEpg;
$epgUrl = $clearEpg ? null : normalize_epg_url($input['epg_url'] ?? $input['epgUrl'] ?? null);
$m3uUrl = normalize_playlist_url($input['m3u_url'] ?? $input['m3uUrl'] ?? null);

if (($deviceId === '' && $publicDeviceCode === '') || $shortCode === '') {
    json_response(['success' => false, 'error' => 'Identifiants Xtream incomplets ou invalides.'], 400);
}
if ($hasXtreamInput && ($host === null || $host === '' || $username === null || $password === null)) {
    json_response(['success' => false, 'error' => 'Identifiants Xtream incomplets ou invalides.'], 400);
}
if ($epgUrl === '') {
    json_response(['success' => false, 'error' => 'URL EPG invalide.'], 400);
}
if ($m3uUrl === '') {
    json_response(['success' => false, 'error' => 'URL M3U invalide.'], 400);
}
if (!$hasXtreamInput && !$epgProvided && $m3uUrl === null) {
    json_response(['success' => false, 'error' => 'Configuration playlist vide.'], 400);
}

$pdo = db();
try {
    if ($deviceId === '' && $publicDeviceCode !== '') {
        $deviceLookup = $pdo->prepare('SELECT device_id FROM devices WHERE public_device_code = :public_code LIMIT 1');
        $deviceLookup->execute(['public_code' => $publicDeviceCode]);
        $deviceId = clean_device_id($deviceLookup->fetchColumn() ?: null);
    }

    if ($deviceId === '') {
        json_response(['success' => false, 'error' => 'Appareil introuvable.'], 404);
    }

    $sessionQuery = $pdo->prepare(
        "SELECT id FROM activation_sessions
         WHERE device_id = :device_id AND short_code = :short_code AND status = 'validated'
         ORDER BY id DESC LIMIT 1"
    );
    $sessionQuery->execute(['device_id' => $deviceId, 'short_code' => $shortCode]);
    if ($sessionQuery->fetchColumn() === false) {
        json_response(['success' => false, 'error' => 'Activez d abord cet appareil.'], 403);
    }

    $accessQuery = $pdo->prepare(
        "SELECT public_device_code, trial_status,
            EXISTS (
                SELECT 1 FROM device_activations
                WHERE device_id = :active_device_id AND status = 'active' AND expires_at > NOW()
                LIMIT 1
            ) AS has_activation
         FROM devices
         WHERE device_id = :device_id
         LIMIT 1"
    );
    $accessQuery->execute([
        'active_device_id' => $deviceId,
        'device_id' => $deviceId,
    ]);
    $access = $accessQuery->fetch();
    $hasActivation = is_array($access) && (int) ($access['has_activation'] ?? 0) === 1;
    $hasPendingTrial = is_array($access) && ($access['trial_status'] ?? '') === 'pending_xtream';
    if (!$hasActivation && !$hasPendingTrial) {
        json_response(['success' => false, 'error' => 'Activation appareil invalide.'], 403);
    }

    // DDL in ensure_app_notifications_table() may implicitly commit on MySQL,
    // so prepare the schema before opening the atomic config+notification transaction.
    ensure_app_notifications_table($pdo);
    $pdo->beginTransaction();
    $existingQuery = $pdo->prepare('SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1');
    $existingQuery->execute(['device_id' => $deviceId]);
    $existingPayload = $existingQuery->fetchColumn();
    $config = is_string($existingPayload) && $existingPayload !== ''
        ? (decrypt_playlist_config($existingPayload) ?? [])
        : [];
    unset($config['target_profile_ids'], $config['new_profile_name']);
    $config['config_id'] = generate_uuid_v4();
    if ($hasXtreamInput) {
        $config['host'] = $host;
        $config['username'] = $username;
        $config['password'] = $password;
        $config['source'] = 'xtream';
    }
    if ($clearEpg) {
        unset($config['epg_url']);
    } elseif ($epgUrl !== null) {
        $config['epg_url'] = $epgUrl;
    }
    if ($m3uUrl !== null) {
        $config['m3u_url'] = $m3uUrl;
        if (!$hasXtreamInput) {
            $config['source'] = 'm3u';
        }
    }
    $config['provided_fields'] = array_values(array_filter([
        $hasXtreamInput ? 'xtream' : null,
        $epgProvided ? 'epg' : null,
        $m3uUrl !== null ? 'm3u' : null,
    ]));
    $hasXtreamConfig = trim((string) ($config['host'] ?? '')) !== ''
        && trim((string) ($config['username'] ?? '')) !== ''
        && trim((string) ($config['password'] ?? '')) !== '';
    $hasM3uConfig = trim((string) ($config['m3u_url'] ?? '')) !== '';
    $playlistConfigured = $hasXtreamConfig || $hasM3uConfig;
    $encrypted = encrypt_playlist_config($config);
    $upsert = $pdo->prepare(
        "INSERT INTO device_playlist_configs (device_id, encrypted_payload, delivered_at, created_at, updated_at)
         VALUES (:device_id, :payload, NULL, NOW(), NOW())
         ON DUPLICATE KEY UPDATE encrypted_payload = VALUES(encrypted_payload), delivered_at = NULL, updated_at = NOW()"
    );
    $upsert->execute(['device_id' => $deviceId, 'payload' => $encrypted]);

    $trialActivation = null;
    if ($hasXtreamConfig && !$hasActivation && $hasPendingTrial) {
        $trialActivation = create_trial_activation(
            $pdo,
            $deviceId,
            clean_public_device_code($access['public_device_code'] ?? null),
        );
    }
    mark_latest_pending_device_session_validated($pdo, $deviceId);

    $pdo->prepare("UPDATE devices SET xtream_status = :xtream_status, updated_at = NOW() WHERE device_id = :device_id")
        ->execute([
            'xtream_status' => $hasXtreamConfig ? 'configured' : 'missing',
            'device_id' => $deviceId,
        ]);

    $notification = create_playlist_push_notification(
        $pdo,
        $deviceId,
        clean_public_device_code($access['public_device_code'] ?? null),
        $hasXtreamInput,
        $m3uUrl !== null,
        $epgProvided,
        'xtream_qr',
        [
            'xtream_host' => $hasXtreamInput ? $host : null,
            'xtream_username' => $hasXtreamInput ? $username : null,
            'xtream_password' => $hasXtreamInput ? $password : null,
            'm3u_url' => $m3uUrl,
            'epg_url' => $epgProvided ? $epgUrl : null,
        ]
    );
    $pdo->commit();

    json_response([
        'success' => true,
        'playlist_configured' => $playlistConfigured,
        'trial_started' => $trialActivation !== null,
        'expires_at' => is_array($trialActivation) ? ($trialActivation['expires_at'] ?? null) : null,
        'notification_created' => (bool) ($notification['created'] ?? false),
        'notification_id' => $notification['notification_id'] ?? null,
        'notification_reason' => $notification['reason'] ?? null,
    ]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision save_playlist_config failed.');
    json_response(['success' => false, 'error' => 'Impossible d enregistrer la configuration Xtream.'], 500);
}
