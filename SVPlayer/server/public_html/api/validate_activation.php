<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['device_id'] ?? null);
$shortCode = normalize_activation_code($input['short_code'] ?? null);
$activationCode = normalize_activation_code($input['activation_code'] ?? null);

if ($deviceId === '' || $shortCode === '' || $activationCode === '') {
    json_response([
        'success' => false,
        'error' => 'Informations d activation incompletes.',
    ], 400);
}

$pdo = db();

try {
    $pdo->beginTransaction();

    $sessionQuery = $pdo->prepare(
        "SELECT id, status, expires_at
         FROM activation_sessions
         WHERE device_id = :device_id
           AND short_code = :short_code
         ORDER BY id DESC
         LIMIT 1
         FOR UPDATE"
    );
    $sessionQuery->execute([
        'device_id' => $deviceId,
        'short_code' => $shortCode,
    ]);
    $session = $sessionQuery->fetch();

    if (!$session) {
        $pdo->rollBack();
        json_response([
            'success' => false,
            'error' => 'Session d activation introuvable.',
        ], 404);
    }

    if (($session['status'] ?? '') === 'cancelled') {
        $pdo->rollBack();
        json_response([
            'success' => false,
            'error' => 'Session d activation annulee.',
        ], 410);
    }

    if (($session['status'] ?? '') === 'expired' || strtotime((string) $session['expires_at']) <= time()) {
        $expireSession = $pdo->prepare(
            "UPDATE activation_sessions SET status = 'expired' WHERE id = :session_id"
        );
        $expireSession->execute(['session_id' => $session['id']]);
        $pdo->commit();

        json_response([
            'success' => false,
            'error' => 'Session d activation expiree. Generez un nouveau code sur la TV.',
        ], 410);
    }

    $codeQuery = $pdo->prepare(
        "SELECT id, duration_days, max_devices, used_devices, status, valid_until
         FROM activation_codes
         WHERE code_hash = :code_hash
         LIMIT 1
         FOR UPDATE"
    );
    $codeQuery->execute([
        'code_hash' => activation_code_hash($activationCode),
    ]);
    $code = $codeQuery->fetch();

    if (!$code || ($code['status'] ?? '') !== 'active') {
        $pdo->rollBack();
        json_response([
            'success' => false,
            'error' => 'Code SmartVision invalide.',
        ], 422);
    }

    if (!empty($code['valid_until']) && strtotime((string) $code['valid_until']) <= time()) {
        $expireCode = $pdo->prepare(
            "UPDATE activation_codes SET status = 'expired' WHERE id = :code_id"
        );
        $expireCode->execute(['code_id' => $code['id']]);
        $pdo->commit();

        json_response([
            'success' => false,
            'error' => 'Code SmartVision invalide.',
        ], 422);
    }

    $existingQuery = $pdo->prepare(
        "SELECT id, expires_at
         FROM device_activations
         WHERE device_id = :device_id
           AND activation_code_id = :activation_code_id
           AND status = 'active'
           AND expires_at > NOW()
         ORDER BY id DESC
         LIMIT 1"
    );
    $existingQuery->execute([
        'device_id' => $deviceId,
        'activation_code_id' => $code['id'],
    ]);
    $existing = $existingQuery->fetch();

    if ($existing) {
        mark_activation_validated($pdo, (int) $session['id'], $deviceId, (string) $existing['expires_at']);
        $pdo->commit();

        json_response([
            'success' => true,
            'status' => 'active',
            'activated' => true,
            'expires_at' => $existing['expires_at'],
            'activation_type' => 'smartvision_code',
        ]);
    }

    if ((int) $code['used_devices'] >= (int) $code['max_devices']) {
        $pdo->rollBack();
        json_response([
            'success' => false,
            'error' => 'Ce code SmartVision a deja atteint sa limite d appareils.',
        ], 409);
    }

    $defaultDuration = max(1, (int) get_setting($pdo, 'activation_duration_days', '365'));
    $durationDays = max(1, (int) ($code['duration_days'] ?: $defaultDuration));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $durationDays . ' days')
        ->format('Y-m-d H:i:s');

    $expirePrevious = $pdo->prepare(
        "UPDATE device_activations
         SET status = 'expired'
         WHERE device_id = :device_id
           AND status = 'active'"
    );
    $expirePrevious->execute(['device_id' => $deviceId]);

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

    $consumeCode = $pdo->prepare(
        "UPDATE activation_codes
         SET used_devices = used_devices + 1,
             updated_at = NOW()
         WHERE id = :code_id"
    );
    $consumeCode->execute(['code_id' => $code['id']]);

    $markCodeUsed = $pdo->prepare(
        "UPDATE activation_code_metadata
         SET last_used_at = NOW()
         WHERE code_id = :code_id"
    );
    $markCodeUsed->execute(['code_id' => $code['id']]);

    mark_activation_validated($pdo, (int) $session['id'], $deviceId, $expiresAt);
    $pdo->commit();

    json_response([
        'success' => true,
        'status' => 'active',
        'activated' => true,
        'expires_at' => $expiresAt,
        'activation_type' => 'smartvision_code',
    ]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }

    error_log('SmartVision validate_activation failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de valider l activation.',
    ], 500);
}

function mark_activation_validated(PDO $pdo, int $sessionId, string $deviceId, string $expiresAt): void
{
    $validateSession = $pdo->prepare(
        "UPDATE activation_sessions
         SET status = 'validated', validated_at = NOW()
         WHERE id = :session_id"
    );
    $validateSession->execute(['session_id' => $sessionId]);

    $activateDevice = $pdo->prepare(
        "UPDATE devices
         SET status = 'active',
             activated_at = COALESCE(activated_at, NOW()),
             expires_at = :expires_at,
             last_seen_at = NOW(),
             updated_at = NOW()
         WHERE device_id = :device_id"
    );
    $activateDevice->execute([
        'device_id' => $deviceId,
        'expires_at' => $expiresAt,
    ]);
}
