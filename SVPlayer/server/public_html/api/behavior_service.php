<?php
declare(strict_types=1);

function behavior_ensure_schema(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS app_behavior_events (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id_hash CHAR(64) NULL,
            app_version VARCHAR(50) NULL,
            platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            event_type VARCHAR(40) NOT NULL,
            video_id_hash CHAR(64) NULL,
            channel_id VARCHAR(80) NULL,
            category_id VARCHAR(40) NULL,
            tags VARCHAR(500) NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_behavior_created (created_at),
            INDEX idx_behavior_event_type (event_type),
            INDEX idx_behavior_channel (channel_id),
            INDEX idx_behavior_category (category_id),
            INDEX idx_behavior_device_time (device_id_hash, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
}

function behavior_allowed_event_types(): array
{
    return [
        'VIDEO_OPENED',
        'PLAYER_READY',
        'PLAY_PAUSE',
        'VIDEO_COMPLETED',
        'SUGGESTION_OPENED',
    ];
}

function behavior_store_event(PDO $pdo, array $payload): array
{
    behavior_ensure_schema($pdo);

    $deviceId = clean_device_id($payload['deviceId'] ?? null);
    if ($deviceId === '') {
        throw new InvalidArgumentException('deviceId requis.');
    }
    $deviceHash = hash('sha256', $deviceId);
    if (behavior_device_rate_limited($pdo, $deviceHash)) {
        throw new RuntimeException('Trop d evenements comportementaux pour cet appareil.');
    }

    $eventType = behavior_clean_enum($payload['eventType'] ?? '', behavior_allowed_event_types(), '');
    if ($eventType === '') {
        throw new InvalidArgumentException('eventType invalide.');
    }

    $statement = $pdo->prepare(
        "INSERT INTO app_behavior_events
            (device_id_hash, app_version, platform, event_type, video_id_hash, channel_id, category_id, tags, created_at)
         VALUES
            (:device_id_hash, :app_version, :platform, :event_type, :video_id_hash, :channel_id, :category_id, :tags, NOW())"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'app_version' => clean_optional_text($payload['appVersion'] ?? null, 50),
        'platform' => behavior_clean_enum($payload['platform'] ?? 'UNKNOWN', ['ANDROID_TV', 'FIRE_TV', 'UNKNOWN'], 'UNKNOWN'),
        'event_type' => $eventType,
        'video_id_hash' => behavior_clean_hash64($payload['videoIdHash'] ?? null),
        'channel_id' => behavior_clean_id($payload['channelId'] ?? null, 80),
        'category_id' => behavior_clean_id($payload['categoryId'] ?? null, 40),
        'tags' => behavior_clean_tags($payload['tags'] ?? null),
    ]);

    return [
        'id' => (int) $pdo->lastInsertId(),
        'eventType' => $eventType,
    ];
}

function behavior_device_rate_limited(PDO $pdo, string $deviceHash): bool
{
    $statement = $pdo->prepare(
        "SELECT COUNT(*) FROM app_behavior_events
         WHERE device_id_hash = :device_id_hash
           AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return (int) $statement->fetchColumn() > 240;
}

function behavior_clean_enum(mixed $value, array $allowed, string $default): string
{
    $clean = strtoupper(preg_replace('/[^A-Z0-9_]/', '', trim((string) $value)) ?: '');

    return in_array($clean, $allowed, true) ? $clean : $default;
}

function behavior_clean_id(mixed $value, int $maxLength): ?string
{
    $clean = preg_replace('/[^A-Za-z0-9._:-]/', '', trim((string) $value)) ?: '';

    return $clean === '' ? null : substr($clean, 0, $maxLength);
}

function behavior_clean_hash64(mixed $value): ?string
{
    $clean = preg_replace('/[^a-fA-F0-9]/', '', trim((string) $value)) ?: '';
    $clean = strtolower($clean);

    return strlen($clean) === 64 ? $clean : null;
}

function behavior_clean_tags(mixed $value): ?string
{
    $tags = explode(',', strtolower(trim((string) $value)));
    $cleanTags = [];
    foreach ($tags as $tag) {
        $clean = preg_replace('/[^a-z0-9_-]/', '', trim($tag)) ?: '';
        if ($clean !== '' && strlen($clean) <= 32) {
            $cleanTags[$clean] = true;
        }
        if (count($cleanTags) >= 12) {
            break;
        }
    }

    return empty($cleanTags) ? null : substr(implode(',', array_keys($cleanTags)), 0, 500);
}
