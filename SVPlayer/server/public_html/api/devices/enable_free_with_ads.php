<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/device_state.php';
require_once dirname(__DIR__) . '/monetization_rules.php';

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

    $pdo->prepare(
        "UPDATE device_activations
         SET status = 'expired'
         WHERE device_id = :device_id
           AND status = 'active'
           AND expires_at <= NOW()"
    )->execute(['device_id' => $deviceId]);

    $activeQuery = $pdo->prepare(
        "SELECT activation_type
         FROM device_activations
         WHERE device_id = :device_id
           AND status = 'active'
           AND expires_at > NOW()
         ORDER BY id DESC
         LIMIT 1"
    );
    $activeQuery->execute(['device_id' => $deviceId]);
    $activeType = $activeQuery->fetchColumn();
    $hasActiveAccess = is_string($activeType) && $activeType !== '';
    if (smartvision_free_ads_action($hasActiveAccess ? $activeType : null, false) === 'keep_active') {
        $pdo->commit();
        $state = smartvision_device_state($pdo, $deviceId);
        unset($state['found']);
        json_response($state);
    }

    $eligibilityQuery = $pdo->prepare(
        "SELECT id FROM device_activations
         WHERE device_id = :device_id
           AND activation_type IN ('trial_demo', 'smartvision_code', 'own_xtream')
           AND (status = 'expired' OR expires_at <= NOW())
         LIMIT 1"
    );
    $eligibilityQuery->execute(['device_id' => $deviceId]);
    $hasExpiredAccess = $eligibilityQuery->fetchColumn() !== false;
    if (smartvision_free_ads_action(null, $hasExpiredAccess) === 'forbidden') {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Le mode gratuit sera disponible apres expiration de l essai ou de la licence.'], 403);
    }

    $expiresAt = smartvision_free_ads_expires_at();
    $insert = $pdo->prepare(
        "INSERT INTO device_activations
            (device_id, activation_code_id, activation_type, status, starts_at, expires_at, created_at)
         VALUES (:device_id, NULL, 'free_ads', 'active', NOW(), :expires_at, NOW())"
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
    error_log('SmartVision devices/enable_free_with_ads failed.');
    json_response(['success' => false, 'error' => 'Impossible d activer le mode gratuit.'], 500);
}
