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
        "SELECT id FROM device_activations
         WHERE device_id = :device_id
           AND activation_type = 'trial_demo'
         LIMIT 1"
    );
    $trialQuery->execute(['device_id' => $deviceId]);
    if ($trialQuery->fetchColumn() === false) {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Le mode gratuit sera disponible apres l essai.'], 403);
    }

    $pdo->prepare(
        "UPDATE device_activations
         SET status = 'expired'
         WHERE device_id = :device_id
           AND status = 'active'
           AND activation_type <> 'free_ads'"
    )->execute(['device_id' => $deviceId]);

    $existingFree = $pdo->prepare(
        "SELECT id FROM device_activations
         WHERE device_id = :device_id
           AND activation_type = 'free_ads'
           AND status = 'active'
           AND expires_at > NOW()
         LIMIT 1"
    );
    $existingFree->execute(['device_id' => $deviceId]);
    if ($existingFree->fetchColumn() === false) {
        $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
            ->modify('+36500 days')
            ->format('Y-m-d H:i:s');
        $insert = $pdo->prepare(
            "INSERT INTO device_activations
                (device_id, activation_code_id, activation_type, status, starts_at, expires_at, created_at)
             VALUES (:device_id, NULL, 'free_ads', 'active', NOW(), :expires_at, NOW())"
        );
        $insert->execute(['device_id' => $deviceId, 'expires_at' => $expiresAt]);
    }

    $pdo->commit();

    $state = smartvision_device_state($pdo, $deviceId);
    unset($state['found']);
    json_response($state);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision devices/enable_free_with_ads failed.');
    json_response(['success' => false, 'error' => 'Impossible d activer le mode gratuit.'], 500);
}
