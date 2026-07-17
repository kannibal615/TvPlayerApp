<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'valid' => false, 'error' => 'Methode non autorisee.'], 405);
}

$input = read_json_input();
$publicCode = clean_public_device_code($input['device'] ?? null);
$pdo = db();

try {
    $ipHash = request_ip_hash() ?: hash('sha256', (string) ($_SERVER['HTTP_USER_AGENT'] ?? 'unknown'));
    $rateQuery = $pdo->prepare(
        "SELECT COUNT(*) FROM playlist_lookup_attempts
         WHERE ip_hash = :ip_hash AND attempted_at >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)"
    );
    $rateQuery->execute(['ip_hash' => $ipHash]);
    if ((int) $rateQuery->fetchColumn() >= 30) {
        json_response(['success' => false, 'valid' => false, 'error' => 'Trop de tentatives. Reessayez plus tard.'], 429);
    }
    $pdo->prepare('INSERT INTO playlist_lookup_attempts (ip_hash, attempted_at) VALUES (:ip_hash, NOW())')
        ->execute(['ip_hash' => $ipHash]);
    if (random_int(1, 50) === 1) {
        $pdo->exec('DELETE FROM playlist_lookup_attempts WHERE attempted_at < DATE_SUB(NOW(), INTERVAL 7 DAY)');
    }

    if (strlen($publicCode) !== 6) {
        json_response(['success' => true, 'valid' => false, 'error' => 'Code TV invalide.']);
    }

    $deviceQuery = $pdo->prepare(
        "SELECT d.device_id, d.trial_status, d.status,
                COALESCE(r.capability_version, 0) AS capability_version,
                EXISTS (
                    SELECT 1 FROM device_activations a
                    WHERE a.device_id = d.device_id AND a.status = 'active' AND a.expires_at > NOW()
                    LIMIT 1
                ) AS has_activation
         FROM devices d
         LEFT JOIN device_profile_registry r ON r.device_id = d.device_id
         WHERE d.public_device_code = :public_code
         LIMIT 1"
    );
    $deviceQuery->execute(['public_code' => $publicCode]);
    $device = $deviceQuery->fetch();
    $eligible = is_array($device)
        && ($device['status'] ?? '') !== 'blocked'
        && ((int) ($device['has_activation'] ?? 0) === 1 || ($device['trial_status'] ?? '') === 'pending_xtream');
    if (!$eligible) {
        json_response(['success' => true, 'valid' => false, 'error' => 'Code TV invalide ou non active.']);
    }
    if ((int) ($device['capability_version'] ?? 0) < 1) {
        json_response([
            'success' => true,
            'valid' => true,
            'ready' => false,
            'profiles' => [],
            'message' => 'Ouvrez ou mettez a jour SmartVision sur la TV pour charger les profils.',
        ]);
    }

    $profileQuery = $pdo->prepare(
        "SELECT profile_id, profile_name, profile_type
         FROM device_playlist_profiles
         WHERE device_id = :device_id AND profile_type IN ('admin', 'normal')
         ORDER BY CASE profile_type WHEN 'admin' THEN 0 ELSE 1 END, profile_name"
    );
    $profileQuery->execute(['device_id' => $device['device_id']]);
    $profiles = array_map(static fn(array $profile): array => [
        'id' => (string) $profile['profile_id'],
        'name' => (string) $profile['profile_name'],
        'type' => (string) $profile['profile_type'],
    ], $profileQuery->fetchAll());

    json_response(['success' => true, 'valid' => true, 'ready' => true, 'profiles' => $profiles]);
} catch (Throwable $exception) {
    error_log('SmartVision playlist_targets failed.');
    json_response(['success' => false, 'valid' => false, 'error' => 'Verification temporairement indisponible.'], 500);
}

