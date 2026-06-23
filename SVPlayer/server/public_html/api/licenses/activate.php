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
$licenseCode = normalize_activation_code($input['licenseCode'] ?? $input['activation_code'] ?? null);

if ($deviceId === '' || $publicCode === '' || $licenseCode === '') {
    json_response(['success' => false, 'error' => 'Informations licence incompletes.'], 400);
}

if (!preg_match('/^[A-Z0-9]{10}$/', $licenseCode)) {
    json_response(['success' => false, 'error' => 'Le code licence doit contenir exactement 10 caracteres.'], 422);
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

    $codeQuery = $pdo->prepare(
        "SELECT id, duration_days, max_devices, used_devices, status, valid_until
         FROM activation_codes
         WHERE code_hash = :code_hash
         LIMIT 1
         FOR UPDATE"
    );
    $codeQuery->execute(['code_hash' => activation_code_hash($licenseCode)]);
    $code = $codeQuery->fetch();

    if (!$code || ($code['status'] ?? '') !== 'active') {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Code licence invalide.'], 422);
    }

    if (!empty($code['valid_until']) && strtotime((string) $code['valid_until']) <= time()) {
        $pdo->prepare("UPDATE activation_codes SET status = 'expired', updated_at = NOW() WHERE id = :id")
            ->execute(['id' => $code['id']]);
        $pdo->commit();
        json_response(['success' => false, 'error' => 'Code licence expire.'], 422);
    }

    $existingQuery = $pdo->prepare(
        "SELECT id, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND activation_code_id = :code_id
           AND status = 'active'
           AND expires_at > NOW()
         LIMIT 1"
    );
    $existingQuery->execute(['device_id' => $deviceId, 'code_id' => $code['id']]);
    $existing = $existingQuery->fetch();
    if ($existing) {
        $pdo->commit();
        $state = smartvision_device_state($pdo, $deviceId);
        unset($state['found']);
        json_response($state);
    }

    if ((int) $code['used_devices'] >= (int) $code['max_devices']) {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Ce code licence a deja ete utilise.'], 409);
    }

    $defaultDuration = max(1, (int) get_setting($pdo, 'activation_duration_days', '365'));
    $durationDays = max(1, (int) ($code['duration_days'] ?: $defaultDuration));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $durationDays . ' days')
        ->format('Y-m-d H:i:s');

    $pdo->prepare("UPDATE device_activations SET status = 'expired' WHERE device_id = :device_id AND status = 'active'")
        ->execute(['device_id' => $deviceId]);

    $insertActivation = $pdo->prepare(
        "INSERT INTO device_activations
            (device_id, activation_code_id, activation_type, status, starts_at, expires_at, created_at)
         VALUES
            (:device_id, :activation_code_id, 'smartvision_code', 'active', NOW(), :expires_at, NOW())"
    );
    $insertActivation->execute([
        'device_id' => $deviceId,
        'activation_code_id' => $code['id'],
        'expires_at' => $expiresAt,
    ]);

    $pdo->prepare(
        "UPDATE activation_codes
         SET used_devices = used_devices + 1,
             updated_at = NOW()
         WHERE id = :id"
    )->execute(['id' => $code['id']]);

    $pdo->prepare(
        "UPDATE activation_code_metadata
         SET last_used_at = NOW(),
             assigned_device_id = :device_id,
             assigned_public_device_code = :public_code
         WHERE code_id = :id"
    )->execute([
        'id' => $code['id'],
        'device_id' => $deviceId,
        'public_code' => $publicCode,
    ]);

    $pdo->commit();

    $state = smartvision_device_state($pdo, $deviceId);
    unset($state['found']);
    json_response($state);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision licenses/activate failed.');
    json_response(['success' => false, 'error' => 'Impossible d activer la licence.'], 500);
}
