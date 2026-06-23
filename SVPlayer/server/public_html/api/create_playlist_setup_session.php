<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

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

    $accessQuery = $pdo->prepare(
        "SELECT
            EXISTS (
                SELECT 1 FROM device_activations
                WHERE device_id = :device_id_active AND status = 'active' AND expires_at > NOW()
                LIMIT 1
            ) AS has_activation,
            trial_status
         FROM devices
         WHERE device_id = :device_id_device
         LIMIT 1"
    );
    $accessQuery->execute([
        'device_id_active' => $deviceId,
        'device_id_device' => $deviceId,
    ]);
    $access = $accessQuery->fetch();
    $hasActivation = is_array($access) && (int) ($access['has_activation'] ?? 0) === 1;
    $hasPendingTrial = is_array($access) && ($access['trial_status'] ?? '') === 'pending_xtream';
    if (!$hasActivation && !$hasPendingTrial) {
        json_response(['success' => false, 'error' => 'Appareil non active.'], 403);
    }

    $deviceQuery = $pdo->prepare('SELECT public_device_code FROM devices WHERE device_id = :device_id LIMIT 1');
    $deviceQuery->execute(['device_id' => $deviceId]);
    $publicDeviceCode = clean_public_device_code($deviceQuery->fetchColumn() ?: null);
    if ($publicDeviceCode === '') {
        $publicDeviceCode = $deviceId;
    }

    $sessionMinutes = max(1, (int) get_setting($pdo, 'activation_session_minutes', '15'));
    $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
        ->modify('+' . $sessionMinutes . ' minutes')
        ->format('Y-m-d H:i:s');

    $insertSession = $pdo->prepare(
        "INSERT INTO activation_sessions (device_id, short_code, status, expires_at, created_at, validated_at)
         VALUES (:device_id, :short_code, 'validated', :expires_at, NOW(), NOW())"
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
        throw new RuntimeException('Unable to generate a unique setup code.');
    }

    $qrUrl = SMARTVISION_PUBLIC_BASE_URL
        . '/xtream/?device=' . rawurlencode($publicDeviceCode)
        . '&session=' . rawurlencode($shortCode);

    json_response([
        'success' => true,
        'device_id' => $deviceId,
        'short_code' => $shortCode,
        'qr_url' => $qrUrl,
        'expires_at' => $expiresAt,
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision create_playlist_setup_session failed.');
    json_response(['success' => false, 'error' => 'Impossible de creer le lien de configuration.'], 500);
}
