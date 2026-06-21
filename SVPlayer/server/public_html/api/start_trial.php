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
$shortCode = normalize_activation_code($input['short_code'] ?? null);
if ($deviceId === '' || $shortCode === '') {
    json_response(['success' => false, 'error' => 'Informations de session incompletes.'], 400);
}

$pdo = db();
try {
    $pdo->beginTransaction();
    $sessionQuery = $pdo->prepare(
        "SELECT id, status, expires_at FROM activation_sessions
         WHERE device_id = :device_id AND short_code = :short_code
         ORDER BY id DESC LIMIT 1 FOR UPDATE"
    );
    $sessionQuery->execute(['device_id' => $deviceId, 'short_code' => $shortCode]);
    $session = $sessionQuery->fetch();
    if (!$session || ($session['status'] ?? '') !== 'pending' || strtotime((string) $session['expires_at']) <= time()) {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'Session expiree. Generez un nouveau code sur la TV.'], 410);
    }

    $trialQuery = $pdo->prepare(
        "SELECT id FROM device_activations WHERE device_id = :device_id AND activation_type = 'trial_demo' LIMIT 1"
    );
    $trialQuery->execute(['device_id' => $deviceId]);
    if ($trialQuery->fetchColumn() !== false) {
        $pdo->rollBack();
        json_response(['success' => false, 'error' => 'L essai gratuit a deja ete utilise sur cet appareil.'], 409);
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
    $pdo->prepare("UPDATE activation_sessions SET status = 'validated', validated_at = NOW() WHERE id = :id")
        ->execute(['id' => $session['id']]);
    $deviceUpdate = $pdo->prepare(
        "UPDATE devices SET status = 'active', activated_at = COALESCE(activated_at, NOW()),
         expires_at = :expires_at, last_seen_at = NOW(), updated_at = NOW() WHERE device_id = :device_id"
    );
    $deviceUpdate->execute(['device_id' => $deviceId, 'expires_at' => $expiresAt]);
    $pdo->commit();

    json_response([
        'success' => true,
        'status' => 'active',
        'activated' => true,
        'activation_type' => 'trial_demo',
        'expires_at' => $expiresAt,
        'trial_days' => $durationDays,
    ]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision start_trial failed.');
    json_response(['success' => false, 'error' => 'Impossible de demarrer l essai gratuit.'], 500);
}
