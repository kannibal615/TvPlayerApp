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

if ($deviceId === '' || $publicCode === '') {
    json_response(['success' => false, 'error' => 'Identifiant appareil requis.'], 400);
}

$pdo = db();

try {
    $pdo->beginTransaction();

    $deviceQuery = $pdo->prepare(
        'SELECT public_device_code FROM devices WHERE device_id = :device_id LIMIT 1 FOR UPDATE'
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
            'trialStatus' => 'expired',
        ], 409);
    }

    $durationDays = max(1, (int) get_setting($pdo, 'trial_duration_days', '7'));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $durationDays . ' days')
        ->format('Y-m-d H:i:s');

    $pdo->prepare("UPDATE device_activations SET status = 'expired' WHERE device_id = :device_id AND status = 'active'")
        ->execute(['device_id' => $deviceId]);

    $insert = $pdo->prepare(
        "INSERT INTO device_activations
            (device_id, activation_code_id, activation_type, status, starts_at, expires_at, created_at)
         VALUES (:device_id, NULL, 'trial_demo', 'active', NOW(), :expires_at, NOW())"
    );
    $insert->execute(['device_id' => $deviceId, 'expires_at' => $expiresAt]);

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
