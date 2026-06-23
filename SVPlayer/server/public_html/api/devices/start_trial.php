<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/device_state.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['deviceId'] ?? $input['device_id'] ?? null);
$publicCode = clean_public_device_code($input['publicDeviceCode'] ?? null);
$playlistConfigured = filter_var($input['playlistConfigured'] ?? false, FILTER_VALIDATE_BOOLEAN);

if ($deviceId === '' || $publicCode === '') {
    json_response(['success' => false, 'error' => 'Identifiant appareil requis.'], 400);
}

$pdo = db();

try {
    $pdo->beginTransaction();

    $deviceQuery = $pdo->prepare(
        'SELECT public_device_code, trial_status FROM devices WHERE device_id = :device_id LIMIT 1 FOR UPDATE'
    );
    $deviceQuery->execute(['device_id' => $deviceId]);
    $device = $deviceQuery->fetch();
    if (!$device || !hash_equals((string) $device['public_device_code'], $publicCode)) {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Appareil introuvable.'], 404);
    }

    $trialQuery = $pdo->prepare(
        "SELECT id, status, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND activation_type = 'trial_demo'
         ORDER BY id DESC
         LIMIT 1"
    );
    $trialQuery->execute(['device_id' => $deviceId]);
    $trial = $trialQuery->fetch();
    if ($trial) {
        $pdo->rollBack();
        json_response([
            'success' => false,
            'error' => 'L essai gratuit a deja ete utilise sur cet appareil.',
            'status' => 'expired',
            'activated' => false,
            'serverDeviceId' => $deviceId,
            'device_id' => $deviceId,
            'publicDeviceCode' => $publicCode,
            'activation_type' => 'trial_demo',
            'trialStatus' => 'expired',
        ]);
    }

    if ($playlistConfigured && ($device['trial_status'] ?? '') !== 'pending_xtream') {
        $pdo->commit();
        $state = smartvision_device_state($pdo, $deviceId);
        unset($state['found']);
        json_response($state);
    }

    if ($playlistConfigured) {
        create_trial_activation($pdo, $deviceId, $publicCode);
    } else {
        $pdo->prepare(
            "UPDATE devices
             SET status = 'active',
                 trial_status = 'pending_xtream',
                 xtream_status = CASE WHEN xtream_status = 'configured' THEN xtream_status ELSE 'missing' END,
                 last_seen_at = NOW(),
                 updated_at = NOW()
             WHERE device_id = :device_id"
        )->execute(['device_id' => $deviceId]);
    }

    $pdo->commit();

    $state = smartvision_device_state($pdo, $deviceId);
    unset($state['found']);
    json_response($state);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision devices/start_trial failed.');
    json_response(['success' => false, 'error' => 'Impossible de demarrer l essai gratuit.'], 500);
}
