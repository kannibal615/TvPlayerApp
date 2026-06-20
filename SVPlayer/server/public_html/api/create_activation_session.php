<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

$input = read_json_input();
$deviceId = clean_device_id($input['device_id'] ?? null);

if ($deviceId === '') {
    json_response([
        'success' => false,
        'error' => 'device_id requis.',
    ], 400);
}

$deviceName = clean_optional_text($input['device_name'] ?? null, 100);
$appVersion = clean_optional_text($input['app_version'] ?? null, 50);
$pdo = db();

try {
    $pdo->beginTransaction();

    $upsertDevice = $pdo->prepare(
        "INSERT INTO devices (device_id, device_name, platform, app_version, status, first_seen_at, last_seen_at)
         VALUES (:device_id, :device_name, 'android_tv', :app_version, 'pending', NOW(), NOW())
         ON DUPLICATE KEY UPDATE
            device_name = VALUES(device_name),
            app_version = VALUES(app_version),
            last_seen_at = NOW(),
            updated_at = NOW()"
    );
    $upsertDevice->execute([
        'device_id' => $deviceId,
        'device_name' => $deviceName,
        'app_version' => $appVersion,
    ]);

    $expireOldSessions = $pdo->prepare(
        "UPDATE activation_sessions
         SET status = 'expired'
         WHERE device_id = :device_id
           AND status = 'pending'
           AND expires_at <= NOW()"
    );
    $expireOldSessions->execute(['device_id' => $deviceId]);

    $sessionMinutes = max(1, (int) get_setting($pdo, 'activation_session_minutes', '15'));
    $pollingInterval = max(1, (int) get_setting($pdo, 'polling_interval_seconds', '5'));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $sessionMinutes . ' minutes')
        ->format('Y-m-d H:i:s');

    $insertSession = $pdo->prepare(
        "INSERT INTO activation_sessions (device_id, short_code, status, expires_at, created_at)
         VALUES (:device_id, :short_code, 'pending', :expires_at, NOW())"
    );

    $shortCode = null;
    for ($attempt = 0; $attempt < 10; $attempt++) {
        $candidate = generate_short_code(6);

        try {
            $insertSession->execute([
                'device_id' => $deviceId,
                'short_code' => $candidate,
                'expires_at' => $expiresAt,
            ]);
            $shortCode = $candidate;
            break;
        } catch (Throwable $exception) {
            if (!is_duplicate_key($exception)) {
                throw $exception;
            }
        }
    }

    if ($shortCode === null) {
        throw new RuntimeException('Unable to generate a unique activation code.');
    }

    $pdo->commit();

    $qrUrl = SMARTVISION_PUBLIC_BASE_URL
        . '/activate/?device_id=' . rawurlencode($deviceId)
        . '&code=' . rawurlencode($shortCode);

    json_response([
        'success' => true,
        'device_id' => $deviceId,
        'short_code' => $shortCode,
        'qr_url' => $qrUrl,
        'expires_at' => $expiresAt,
        'polling_interval' => $pollingInterval,
    ]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }

    error_log('SmartVision create_activation_session failed.');
    json_response([
        'success' => false,
        'error' => 'Impossible de creer la session d activation.',
    ], 500);
}
