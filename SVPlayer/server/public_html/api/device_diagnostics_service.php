<?php
declare(strict_types=1);

function device_diagnostics_ensure_schema(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS app_device_diagnostics (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id VARCHAR(100) NULL,
            public_device_code VARCHAR(40) NULL,
            diagnostic_type VARCHAR(40) NOT NULL,
            app_version VARCHAR(50) NULL,
            android_version VARCHAR(40) NULL,
            device_model VARCHAR(120) NULL,
            payload_json TEXT NOT NULL,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_device_diag (device_id, diagnostic_type),
            INDEX idx_public_diag (public_device_code, diagnostic_type),
            INDEX idx_diag_type_time (diagnostic_type, updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
}

function device_diagnostics_upsert(PDO $pdo, array $payload): void
{
    device_diagnostics_ensure_schema($pdo);

    $deviceId = clean_device_id($payload['device_id'] ?? null);
    $publicDeviceCode = clean_public_device_code($payload['public_device_code'] ?? null);
    $diagnosticType = smartvision_text_substr(trim((string) ($payload['diagnostic_type'] ?? '')), 0, 40);
    $data = $payload['payload'] ?? null;
    if (($deviceId === '' && $publicDeviceCode === '') || $diagnosticType === '' || !is_array($data)) {
        throw new InvalidArgumentException('Diagnostic incomplet.');
    }

    $statement = $pdo->prepare(
        "INSERT INTO app_device_diagnostics
            (device_id, public_device_code, diagnostic_type, app_version, android_version, device_model, payload_json, updated_at, created_at)
         VALUES
            (:device_id, :public_device_code, :diagnostic_type, :app_version, :android_version, :device_model, :payload_json, NOW(), NOW())
         ON DUPLICATE KEY UPDATE
            public_device_code = COALESCE(VALUES(public_device_code), public_device_code),
            app_version = VALUES(app_version),
            android_version = VALUES(android_version),
            device_model = VALUES(device_model),
            payload_json = VALUES(payload_json),
            updated_at = NOW()"
    );
    $statement->execute([
        'device_id' => $deviceId !== '' ? $deviceId : null,
        'public_device_code' => $publicDeviceCode !== '' ? $publicDeviceCode : null,
        'diagnostic_type' => $diagnosticType,
        'app_version' => clean_optional_text($payload['app_version'] ?? null, 50),
        'android_version' => clean_optional_text($payload['android_version'] ?? null, 40),
        'device_model' => clean_optional_text($payload['device_model'] ?? null, 120),
        'payload_json' => json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ]);
}

function device_diagnostics_load_by_device(PDO $pdo, string $deviceId, string $publicDeviceCode = ''): array
{
    device_diagnostics_ensure_schema($pdo);
    if ($deviceId === '' && $publicDeviceCode === '') {
        return [];
    }
    $where = [];
    $params = [];
    if ($deviceId !== '') {
        $where[] = 'device_id = :device_id';
        $params['device_id'] = $deviceId;
    }
    if ($publicDeviceCode !== '') {
        $where[] = 'public_device_code = :public_device_code';
        $params['public_device_code'] = $publicDeviceCode;
    }
    $statement = $pdo->prepare(
        "SELECT diagnostic_type, app_version, android_version, device_model, payload_json, updated_at
         FROM app_device_diagnostics
         WHERE " . implode(' OR ', $where) . "
         ORDER BY updated_at DESC"
    );
    $statement->execute($params);
    $rows = [];
    foreach ($statement->fetchAll() as $row) {
        $type = (string) ($row['diagnostic_type'] ?? '');
        if ($type === '' || isset($rows[$type])) {
            continue;
        }
        $payload = json_decode((string) ($row['payload_json'] ?? '{}'), true);
        $rows[$type] = [
            'app_version' => $row['app_version'] ?? null,
            'android_version' => $row['android_version'] ?? null,
            'device_model' => $row['device_model'] ?? null,
            'updated_at' => $row['updated_at'] ?? null,
            'payload' => is_array($payload) ? $payload : [],
        ];
    }
    return $rows;
}
