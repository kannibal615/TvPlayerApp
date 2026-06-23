<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/device_state.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$fingerprintHash = clean_hash($input['deviceFingerprintHash'] ?? null);
$androidIdHash = clean_hash($input['androidIdHash'] ?? null);
$platform = clean_optional_text($input['platform'] ?? 'android_tv', 50) ?: 'android_tv';
$appPackage = clean_optional_text($input['appPackage'] ?? null, 120);
$appVersion = clean_optional_text($input['appVersion'] ?? null, 50);
$manufacturer = clean_optional_text($input['deviceManufacturer'] ?? null, 50);
$model = clean_optional_text($input['deviceModel'] ?? null, 80);
$deviceName = trim(implode(' ', array_filter([$manufacturer, $model]))) ?: 'Android TV';

if ($fingerprintHash === '') {
    $fingerprintHash = $androidIdHash;
}

if ($fingerprintHash === '' || strlen($fingerprintHash) < 32) {
    json_response(['success' => false, 'error' => 'Identite appareil invalide.'], 400);
}

$pdo = db();

try {
    $pdo->beginTransaction();

    $deviceQuery = $pdo->prepare(
        'SELECT device_id, public_device_code
         FROM devices
         WHERE device_fingerprint_hash = :fingerprint_hash
         LIMIT 1
         FOR UPDATE'
    );
    $deviceQuery->execute(['fingerprint_hash' => $fingerprintHash]);
    $device = $deviceQuery->fetch();

    if (!$device) {
        $deviceId = generate_uuid_v4();
        $publicCode = null;
        for ($attempt = 0; $attempt < 12; $attempt++) {
            $candidate = generate_short_code(6);
            try {
                $insert = $pdo->prepare(
                    "INSERT INTO devices
                        (device_id, device_fingerprint_hash, public_device_code, device_name, platform, app_version,
                         status, license_status, trial_status, free_with_ads_status, xtream_status, first_seen_at, last_seen_at)
                     VALUES
                        (:device_id, :fingerprint_hash, :public_code, :device_name, :platform, :app_version,
                         'pending', 'inactive', 'available', 'inactive', 'missing', NOW(), NOW())"
                );
                $insert->execute([
                    'device_id' => $deviceId,
                    'fingerprint_hash' => $fingerprintHash,
                    'public_code' => $candidate,
                    'device_name' => $deviceName,
                    'platform' => $platform,
                    'app_version' => $appVersion,
                ]);
                $publicCode = $candidate;
                break;
            } catch (Throwable $exception) {
                if (!is_duplicate_key($exception)) {
                    throw $exception;
                }
            }
        }
        if ($publicCode === null) {
            throw new RuntimeException('Unable to generate public device code.');
        }
    } else {
        $deviceId = (string) $device['device_id'];
        $publicCode = clean_public_device_code($device['public_device_code'] ?? null);
        if ($publicCode === '') {
            for ($attempt = 0; $attempt < 12; $attempt++) {
                $candidate = generate_short_code(6);
                try {
                    $updateCode = $pdo->prepare(
                        'UPDATE devices SET public_device_code = :public_code WHERE device_id = :device_id'
                    );
                    $updateCode->execute(['public_code' => $candidate, 'device_id' => $deviceId]);
                    $publicCode = $candidate;
                    break;
                } catch (Throwable $exception) {
                    if (!is_duplicate_key($exception)) {
                        throw $exception;
                    }
                }
            }
        }
        $update = $pdo->prepare(
            "UPDATE devices
             SET device_name = :device_name,
                 platform = :platform,
                 app_version = :app_version,
                 last_seen_at = NOW(),
                 updated_at = NOW()
             WHERE device_id = :device_id"
        );
        $update->execute([
            'device_name' => $deviceName,
            'platform' => $platform,
            'app_version' => $appVersion,
            'device_id' => $deviceId,
        ]);
    }

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
        $candidate = generate_short_code(8);
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
        throw new RuntimeException('Unable to create device token session.');
    }

    $sessionId = (int) $pdo->lastInsertId();
    $deviceToken = generate_device_token();
    $insertToken = $pdo->prepare(
        "INSERT INTO activation_session_tokens (session_id, device_id, token_hash, created_at)
         VALUES (:session_id, :device_id, :token_hash, NOW())"
    );
    $insertToken->execute([
        'session_id' => $sessionId,
        'device_id' => $deviceId,
        'token_hash' => device_token_hash($deviceToken),
    ]);

    $pdo->commit();

    $state = smartvision_device_state($pdo, $deviceId);
    unset($state['found']);
    $state['serverDeviceId'] = $deviceId;
    $state['device_id'] = $deviceId;
    $state['publicDeviceCode'] = $publicCode;
    $state['activationStatus'] = $state['status'] ?? 'pending';
    $state['device_token'] = $deviceToken;
    $state['polling_interval'] = $pollingInterval;

    json_response($state);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision device register failed.');
    json_response(['success' => false, 'error' => 'Impossible d enregistrer cet appareil.'], 500);
}
