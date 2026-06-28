<?php
declare(strict_types=1);

function anomaly_ensure_schema(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS app_anomaly_events (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id_hash CHAR(64) NULL,
            app_version VARCHAR(50) NULL,
            platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            route VARCHAR(120) NULL,
            anomaly_type VARCHAR(60) NOT NULL,
            message VARCHAR(255) NULL,
            stack_trace TEXT NULL,
            context_json TEXT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_anomaly_created (created_at),
            INDEX idx_anomaly_type (anomaly_type),
            INDEX idx_anomaly_route (route),
            INDEX idx_anomaly_device_time (device_id_hash, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
}

function anomaly_store_event(PDO $pdo, array $payload): array
{
    anomaly_ensure_schema($pdo);

    $deviceId = clean_device_id($payload['deviceId'] ?? null);
    if ($deviceId === '') {
        throw new InvalidArgumentException('deviceId requis.');
    }
    $deviceHash = hash('sha256', $deviceId);
    if (anomaly_device_rate_limited($pdo, $deviceHash)) {
        throw new RuntimeException('Trop d anomalies pour cet appareil.');
    }

    $anomalyType = anomaly_clean_type($payload['anomalyType'] ?? 'UNKNOWN');
    if (in_array($anomalyType, anomaly_ignored_types(), true)) {
        return [
            'id' => 0,
            'anomalyType' => $anomalyType,
        ];
    }
    $context = clean_optional_text($payload['context'] ?? null, 500);

    $statement = $pdo->prepare(
        "INSERT INTO app_anomaly_events
            (device_id_hash, app_version, platform, route, anomaly_type, message, stack_trace, context_json, created_at)
         VALUES
            (:device_id_hash, :app_version, :platform, :route, :anomaly_type, :message, :stack_trace, :context_json, NOW())"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'app_version' => clean_optional_text($payload['appVersion'] ?? null, 50),
        'platform' => anomaly_clean_platform($payload['platform'] ?? 'UNKNOWN'),
        'route' => clean_optional_text($payload['route'] ?? null, 120),
        'anomaly_type' => $anomalyType,
        'message' => clean_optional_text($payload['message'] ?? null, 255),
        'stack_trace' => clean_optional_text($payload['stackTrace'] ?? null, 4000),
        'context_json' => $context,
    ]);

    return [
        'id' => (int) $pdo->lastInsertId(),
        'anomalyType' => $anomalyType,
    ];
}

function anomaly_device_rate_limited(PDO $pdo, string $deviceHash): bool
{
    $statement = $pdo->prepare(
        "SELECT COUNT(*) FROM app_anomaly_events
         WHERE device_id_hash = :device_id_hash
           AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return (int) $statement->fetchColumn() > 120;
}

function anomaly_clean_type(mixed $value): string
{
    $raw = trim((string) $value);
    $clean = preg_replace('/[^\p{L}\p{N}_ -]/u', '', $raw);
    if (!is_string($clean)) {
        $clean = '';
    }
    return $clean !== '' ? smartvision_text_substr($clean, 0, 60) : 'UNKNOWN';
}

function anomaly_ignored_types(): array
{
    return [
        'PLAYER_EXIT_STEP',
        'PLAYER_EXIT',
        'PLAYER_EXIT_BEGIN',
        'PLAYER_RELEASE_DONE',
        'PLAYER_PROGRESS_SAVE_DONE',
    ];
}

function anomaly_clean_platform(mixed $value): string
{
    $clean = strtoupper(preg_replace('/[^A-Z0-9_]/', '', trim((string) $value)) ?: '');
    return in_array($clean, ['ANDROID_TV', 'FIRE_TV', 'UNKNOWN'], true) ? $clean : 'UNKNOWN';
}

function anomaly_mask_hash(?string $hash): string
{
    $hash = (string) $hash;
    if (strlen($hash) < 16) {
        return '-';
    }
    return substr($hash, 0, 8) . '...' . substr($hash, -6);
}
