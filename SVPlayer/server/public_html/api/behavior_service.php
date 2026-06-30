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
            content_type ENUM('LIVE_TV', 'MOVIE', 'SERIES', 'EPISODE', 'YOUTUBE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            content_id_hash CHAR(64) NULL,
            content_title_hash CHAR(64) NULL,
            content_title VARCHAR(180) NULL,
            category_label VARCHAR(120) NULL,
            content_country VARCHAR(16) NULL,
            content_region VARCHAR(32) NULL,
            content_language VARCHAR(16) NULL,
            interest_tags VARCHAR(255) NULL,
            duration_seconds INT UNSIGNED NULL,
            position_seconds INT UNSIGNED NULL,
            engagement_score TINYINT UNSIGNED NULL,
            source_screen VARCHAR(40) NULL,
            context_json TEXT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_behavior_created (created_at),
            INDEX idx_behavior_event_type (event_type),
            INDEX idx_behavior_channel (channel_id),
            INDEX idx_behavior_category (category_id),
            INDEX idx_behavior_content_type (content_type),
            INDEX idx_behavior_source (source_screen),
            INDEX idx_behavior_device_time (device_id_hash, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    foreach ([
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_type ENUM('LIVE_TV', 'MOVIE', 'SERIES', 'EPISODE', 'YOUTUBE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN' AFTER tags",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_id_hash CHAR(64) NULL AFTER content_type",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_title_hash CHAR(64) NULL AFTER content_id_hash",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_title VARCHAR(180) NULL AFTER content_title_hash",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS category_label VARCHAR(120) NULL AFTER category_id",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_country VARCHAR(16) NULL AFTER category_label",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_region VARCHAR(32) NULL AFTER content_country",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS content_language VARCHAR(16) NULL AFTER content_region",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS interest_tags VARCHAR(255) NULL AFTER content_language",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS duration_seconds INT UNSIGNED NULL AFTER interest_tags",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS position_seconds INT UNSIGNED NULL AFTER duration_seconds",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS engagement_score TINYINT UNSIGNED NULL AFTER position_seconds",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS source_screen VARCHAR(40) NULL AFTER engagement_score",
        "ALTER TABLE app_behavior_events ADD COLUMN IF NOT EXISTS context_json TEXT NULL AFTER source_screen",
    ] as $statement) {
        try {
            $pdo->exec($statement);
        } catch (Throwable $exception) {
            error_log('SmartVision behavior schema alter skipped.');
        }
    }

    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS user_behavior_daily (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id_hash CHAR(64) NOT NULL,
            activity_date DATE NOT NULL,
            content_type ENUM('LIVE_TV', 'MOVIE', 'SERIES', 'EPISODE', 'YOUTUBE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            events_count INT UNSIGNED NOT NULL DEFAULT 0,
            playback_count INT UNSIGNED NOT NULL DEFAULT 0,
            completed_count INT UNSIGNED NOT NULL DEFAULT 0,
            favorite_count INT UNSIGNED NOT NULL DEFAULT 0,
            total_duration_seconds BIGINT UNSIGNED NOT NULL DEFAULT 0,
            avg_engagement_score DECIMAL(5,2) NOT NULL DEFAULT 0,
            top_category_id VARCHAR(40) NULL,
            top_category_label VARCHAR(120) NULL,
            top_tags VARCHAR(500) NULL,
            top_interests VARCHAR(255) NULL,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_behavior_daily_device_type_day (device_id_hash, activity_date, content_type),
            INDEX idx_behavior_daily_date (activity_date),
            INDEX idx_behavior_daily_type (content_type)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS user_segments (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id_hash CHAR(64) NOT NULL,
            segment_key VARCHAR(80) NOT NULL,
            segment_label VARCHAR(120) NOT NULL,
            segment_group ENUM('CONTENT', 'ENGAGEMENT', 'LANGUAGE', 'COUNTRY', 'REGION', 'INTEREST', 'ADS', 'RISK') NOT NULL DEFAULT 'CONTENT',
            score TINYINT UNSIGNED NOT NULL DEFAULT 0,
            confidence TINYINT UNSIGNED NOT NULL DEFAULT 0,
            evidence VARCHAR(500) NULL,
            first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            expires_at DATETIME NULL,
            UNIQUE KEY uq_user_segments_device_key (device_id_hash, segment_key),
            INDEX idx_user_segments_key (segment_key),
            INDEX idx_user_segments_group (segment_group),
            INDEX idx_user_segments_score (score),
            INDEX idx_user_segments_last_seen (last_seen_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    foreach ([
        "ALTER TABLE user_behavior_daily ADD COLUMN IF NOT EXISTS top_interests VARCHAR(255) NULL AFTER top_tags",
        "ALTER TABLE user_segments MODIFY segment_group ENUM('CONTENT', 'ENGAGEMENT', 'LANGUAGE', 'COUNTRY', 'REGION', 'INTEREST', 'ADS', 'RISK') NOT NULL DEFAULT 'CONTENT'",
    ] as $statement) {
        try {
            $pdo->exec($statement);
        } catch (Throwable $exception) {
            error_log('SmartVision behavior schema post alter skipped.');
        }
    }

    try {
        $pdo->exec("DELETE FROM app_behavior_events WHERE content_type = 'UNKNOWN'");
        $pdo->exec("DELETE FROM user_behavior_daily WHERE content_type = 'UNKNOWN'");
    } catch (Throwable $exception) {
        error_log('SmartVision behavior unknown purge skipped.');
    }
}

function behavior_allowed_event_types(): array
{
    return [
        'VIDEO_OPENED',
        'PLAYER_READY',
        'PLAY_PAUSE',
        'VIDEO_COMPLETED',
        'SUGGESTION_OPENED',
        'CONTENT_OPENED',
        'PLAYBACK_PROGRESS',
        'PLAYBACK_COMPLETED',
        'FAVORITE_ADDED',
        'FAVORITE_REMOVED',
        'SEARCH_PERFORMED',
        'PLAYER_ERROR',
    ];
}

function behavior_tracked_events_sql(?string $alias = null): string
{
    $prefix = $alias !== null && $alias !== '' ? $alias . '.' : '';
    return $prefix . "event_type NOT IN ('CATEGORY_OPENED', 'PLAYBACK_STARTED')";
}

function behavior_allowed_content_types(): array
{
    return ['LIVE_TV', 'MOVIE', 'SERIES', 'EPISODE', 'YOUTUBE', 'UNKNOWN'];
}

function behavior_store_events(PDO $pdo, array $payload): array
{
    $events = isset($payload['events']) && is_array($payload['events']) ? $payload['events'] : [$payload];
    $events = array_slice($events, 0, 40);
    $stored = [];
    foreach ($events as $event) {
        if (!is_array($event)) {
            continue;
        }
        $stored[] = behavior_store_event($pdo, $event);
    }

    return [
        'count' => count($stored),
        'events' => $stored,
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
    $contentType = behavior_clean_enum($payload['contentType'] ?? behavior_legacy_content_type($payload), behavior_allowed_content_types(), 'UNKNOWN');
    if ($contentType === 'UNKNOWN') {
        return [
            'id' => 0,
            'eventType' => $eventType,
            'ignored' => true,
        ];
    }
    $tags = behavior_clean_tags($payload['tags'] ?? null);
    $contentTitle = clean_optional_text($payload['contentTitle'] ?? $payload['title'] ?? behavior_context_value($payload['context'] ?? null, ['title', 'media', 'channel']), 180);
    $categoryLabel = clean_optional_text($payload['categoryLabel'] ?? null, 120);
    $inference = behavior_infer_content_context($categoryLabel, $contentTitle, $tags);
    $contentCountry = behavior_clean_id($payload['country'] ?? null, 16) ?: $inference['country'];
    $contentRegion = $inference['region'];
    $contentLanguage = behavior_clean_id($payload['language'] ?? null, 16) ?: $inference['language'];
    $interestTags = $inference['interests'];
    $durationSeconds = behavior_clean_int($payload['durationSeconds'] ?? null, 0, 86400);
    $positionSeconds = behavior_clean_int($payload['positionSeconds'] ?? null, 0, 86400);
    $engagementScore = behavior_clean_int($payload['engagementScore'] ?? null, 0, 100);

    $statement = $pdo->prepare(
        "INSERT INTO app_behavior_events
            (device_id_hash, app_version, platform, event_type, video_id_hash, channel_id, category_id, tags,
             content_type, content_id_hash, content_title_hash, content_title, category_label, content_country,
             content_region, content_language, interest_tags,
             duration_seconds, position_seconds, engagement_score, source_screen, context_json, created_at)
         VALUES
            (:device_id_hash, :app_version, :platform, :event_type, :video_id_hash, :channel_id, :category_id, :tags,
             :content_type, :content_id_hash, :content_title_hash, :content_title, :category_label, :content_country,
             :content_region, :content_language, :interest_tags,
             :duration_seconds, :position_seconds, :engagement_score, :source_screen, :context_json, NOW())"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'app_version' => clean_optional_text($payload['appVersion'] ?? null, 50),
        'platform' => behavior_clean_enum($payload['platform'] ?? 'UNKNOWN', ['ANDROID_TV', 'FIRE_TV', 'UNKNOWN'], 'UNKNOWN'),
        'event_type' => $eventType,
        'video_id_hash' => behavior_clean_hash64($payload['videoIdHash'] ?? null),
        'channel_id' => behavior_clean_id($payload['channelId'] ?? null, 80),
        'category_id' => behavior_clean_id($payload['categoryId'] ?? null, 40),
        'tags' => $tags,
        'content_type' => $contentType,
        'content_id_hash' => behavior_clean_hash64($payload['contentIdHash'] ?? null),
        'content_title_hash' => behavior_clean_hash64($payload['contentTitleHash'] ?? null),
        'content_title' => $contentTitle,
        'category_label' => $categoryLabel,
        'content_country' => $contentCountry,
        'content_region' => $contentRegion,
        'content_language' => $contentLanguage,
        'interest_tags' => $interestTags,
        'duration_seconds' => $durationSeconds,
        'position_seconds' => $positionSeconds,
        'engagement_score' => $engagementScore,
        'source_screen' => behavior_clean_enum($payload['sourceScreen'] ?? 'UNKNOWN', ['HOME', 'LIVE', 'MOVIES', 'SERIES', 'DETAIL', 'PLAYER', 'YOUTUBE', 'SEARCH', 'UNKNOWN'], 'UNKNOWN'),
        'context_json' => behavior_clean_context($payload['context'] ?? null),
    ]);

    behavior_update_daily($pdo, $deviceHash, $contentType);
    behavior_recompute_device_segments($pdo, $deviceHash);

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

function behavior_legacy_content_type(array $payload): string
{
    return isset($payload['videoIdHash']) ? 'YOUTUBE' : 'UNKNOWN';
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

function behavior_clean_int(mixed $value, int $min, int $max): ?int
{
    $clean = filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => $min, 'max_range' => $max]]);

    return $clean === false ? null : (int) $clean;
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

function behavior_clean_context(mixed $value): ?string
{
    $text = trim((string) $value);
    if ($text === '') {
        return null;
    }
    $parts = explode(';', $text);
    $cleanParts = [];
    foreach ($parts as $part) {
        [$key, $partValue] = array_pad(explode('=', $part, 2), 2, '');
        $cleanKey = preg_replace('/[^a-z0-9_]/', '', strtolower(trim($key))) ?: '';
        $cleanValue = trim(preg_replace('/\s+/', ' ', $partValue));
        if ($cleanKey !== '' && $cleanValue !== '') {
            $cleanParts[] = $cleanKey . '=' . smartvision_text_substr($cleanValue, 0, 80);
        }
        if (count($cleanParts) >= 12) {
            break;
        }
    }

    return $cleanParts === [] ? null : smartvision_text_substr(implode(';', $cleanParts), 0, 1000);
}

function behavior_context_value(mixed $value, array $keys): ?string
{
    $text = trim((string) $value);
    if ($text === '') {
        return null;
    }
    $wanted = array_fill_keys(array_map('strtolower', $keys), true);
    foreach (explode(';', $text) as $part) {
        [$key, $partValue] = array_pad(explode('=', $part, 2), 2, '');
        $cleanKey = strtolower(trim($key));
        $cleanValue = trim($partValue);
        if (isset($wanted[$cleanKey]) && $cleanValue !== '') {
            return $cleanValue;
        }
    }

    return null;
}

function behavior_infer_content_context(?string $categoryLabel, ?string $contentTitle, ?string $tags): array
{
    $haystack = behavior_match_text(implode(' ', array_filter([$categoryLabel, $contentTitle, $tags])));
    $language = null;
    $country = null;
    $region = null;
    $interests = [];

    foreach (behavior_language_rules() as $code => $rule) {
        if (preg_match($rule, $haystack)) {
            $language = $code;
            break;
        }
    }

    foreach (behavior_country_rules() as $code => $rule) {
        if (preg_match($rule['pattern'], $haystack)) {
            $country = $code;
            $region = $rule['region'];
            if ($language === null && isset($rule['language'])) {
                $language = $rule['language'];
            }
            break;
        }
    }

    if ($region === null) {
        foreach (behavior_region_rules() as $label => $rule) {
            if (preg_match($rule, $haystack)) {
                $region = $label;
                break;
            }
        }
    }

    foreach (behavior_interest_rules() as $tag => $rule) {
        if (preg_match($rule, $haystack)) {
            $interests[$tag] = true;
        }
    }

    return [
        'language' => $language,
        'country' => $country,
        'region' => $region,
        'interests' => $interests === [] ? null : implode(',', array_keys($interests)),
    ];
}

function behavior_match_text(string $text): string
{
    $normalized = @iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $text);
    if ($normalized === false) {
        $normalized = $text;
    }
    $normalized = strtoupper($normalized);
    $normalized = preg_replace('/[^A-Z0-9]+/', ' ', $normalized) ?: '';

    return ' ' . trim($normalized) . ' ';
}

function behavior_language_rules(): array
{
    return [
        'ar' => '/(\|AR\||\bAR\b|\bARABE\b|\bARABIC\b|\bMBC\b|\bROTANA\b|\bSHAHID\b)/',
        'fr' => '/(\|FR\||\bFRANCE\b|\bFRANCAIS\b|\bFRENCH\b|\bTF1\b|\bM6\b|\bCANAL\b)/',
        'en' => '/(\|EN\||\bENGLISH\b|\bUK\b|\bUSA\b|\bBBC\b|\bCNN\b)/',
        'es' => '/(\|ES\||\bESPANA\b|\bSPAIN\b|\bSPANISH\b|\bLATINO\b)/',
        'pt' => '/(\|PT\||\bPORTUGAL\b|\bBRAZIL\b|\bPORTUGUESE\b)/',
        'it' => '/(\|IT\||\bITALIA\b|\bITALY\b|\bITALIAN\b)/',
        'de' => '/(\|DE\||\bDEUTSCH\b|\bGERMANY\b|\bALLEMAND\b)/',
        'tr' => '/(\|TR\||\bTURKEY\b|\bTURKISH\b|\bTURK\b)/',
    ];
}

function behavior_region_rules(): array
{
    return [
        'Europe' => '/(\|EU\||\bEU\b|\bEUROPE\b|\bFRANCE\b|\bITALY\b|\bSPAIN\b|\bGERMANY\b|\bUK\b)/',
        'MENA' => '/(\|AR\||\bAR\b|\bMAGHREB\b|\bMENA\b|\bTUNISIA\b|\bMAROC\b|\bALGERIA\b|\bEGYPT\b|\bSAUDI\b)/',
        'North America' => '/(\bUSA\b|\bUNITED STATES\b|\bCANADA\b|\bUS\b)/',
        'Latin America' => '/(\bLATAM\b|\bLATINO\b|\bMEXICO\b|\bBRAZIL\b|\bARGENTINA\b)/',
        'Africa' => '/(\bAFRICA\b|\bSENEGAL\b|\bCAMEROON\b|\bNIGERIA\b|\bIVORY COAST\b)/',
    ];
}

function behavior_country_rules(): array
{
    return [
        'FR' => ['region' => 'Europe', 'language' => 'fr', 'pattern' => '/(\bFRANCE\b|\bFR\b|\bTF1\b|\bM6\b)/'],
        'TN' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bTUNISIA\b|\bTUNISIE\b|\bTN\b|\bDIWAN\b|\bNESSMA\b)/'],
        'MA' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bMOROCCO\b|\bMAROC\b|\bMA\b|\b2M\b)/'],
        'DZ' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bALGERIA\b|\bALGERIE\b|\bDZ\b|\bENTV\b)/'],
        'EG' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bEGYPT\b|\bEGYPTE\b|\bEG\b|\bCBC\b)/'],
        'SA' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bSAUDI\b|\bKSA\b|\bSA\b)/'],
        'AE' => ['region' => 'MENA', 'language' => 'ar', 'pattern' => '/(\bUAE\b|\bDUBAI\b|\bABU DHABI\b|\bAE\b)/'],
        'GB' => ['region' => 'Europe', 'language' => 'en', 'pattern' => '/(\bUK\b|\bUNITED KINGDOM\b|\bBRITAIN\b|\bBBC\b)/'],
        'US' => ['region' => 'North America', 'language' => 'en', 'pattern' => '/(\bUSA\b|\bUNITED STATES\b|\bUS\b|\bFOX\b|\bNBC\b)/'],
        'CA' => ['region' => 'North America', 'pattern' => '/(\bCANADA\b|\bCA\b)/'],
        'ES' => ['region' => 'Europe', 'language' => 'es', 'pattern' => '/(\bSPAIN\b|\bESPANA\b|\bES\b)/'],
        'IT' => ['region' => 'Europe', 'language' => 'it', 'pattern' => '/(\bITALY\b|\bITALIA\b|\bIT\b|\bRAI\b)/'],
        'DE' => ['region' => 'Europe', 'language' => 'de', 'pattern' => '/(\bGERMANY\b|\bDEUTSCHLAND\b|\bDE\b)/'],
        'TR' => ['region' => 'Europe', 'language' => 'tr', 'pattern' => '/(\bTURKEY\b|\bTURKIYE\b|\bTR\b)/'],
    ];
}

function behavior_interest_rules(): array
{
    return [
        'sports' => '/(\bSPORT\b|\bSPORTS\b|\bBEIN\b|\bESPN\b|\bEUROSPORT\b|\bFOOT\b|\bFIFA\b|\bNBA\b|\bTENNIS\b|\bDIWAN SPORT\b)/',
        'news' => '/(\bNEWS\b|\bINFO\b|\bACTU\b|\bACTUALITE\b|\bJOURNAL\b|\bFRANCE 24\b|\bCNN\b|\bBBC\b|\bALJAZEERA\b|\bSKY NEWS\b)/',
        'kids' => '/(\bKIDS\b|\bENFANT\b|\bJEUNESSE\b|\bCARTOON\b|\bDISNEY\b|\bNICKELODEON\b|\bTOON\b|\bANIME\b)/',
        'cinema' => '/(\bCINEMA\b|\bMOVIE\b|\bMOVIES\b|\bFILM\b|\bFILMS\b|\bVOD\b|\bBOX OFFICE\b|\bACTION\b|\bCOMEDY\b)/',
        'music' => '/(\bMUSIC\b|\bMUSIQUE\b|\bMTV\b|\bMCM\b|\bCLIP\b|\bHITS\b|\bRADIO\b)/',
        'documentaries' => '/(\bDOC\b|\bDOCUMENTARY\b|\bDOCUMENTAIRE\b|\bNAT GEO\b|\bHISTORY\b|\bDISCOVERY\b|\bNATURE\b)/',
        'religion' => '/(\bQURAN\b|\bCORAN\b|\bISLAM\b|\bRELIGION\b|\bCHRISTIAN\b)/',
        'entertainment' => '/(\bENTERTAINMENT\b|\bVARIETY\b|\bSHOW\b|\bMBC\b|\bROTANA\b|\bREALITY\b)/',
    ];
}

function behavior_update_daily(PDO $pdo, string $deviceHash, string $contentType): void
{
    $statement = $pdo->prepare(
        "REPLACE INTO user_behavior_daily
            (device_id_hash, activity_date, content_type, events_count, playback_count, completed_count,
             favorite_count, total_duration_seconds, avg_engagement_score, top_category_id, top_category_label, top_tags, top_interests)
         SELECT
            device_id_hash,
            UTC_DATE(),
            content_type,
            COUNT(*),
            SUM(event_type IN ('PLAYBACK_PROGRESS', 'PLAYER_READY')),
            SUM(event_type IN ('PLAYBACK_COMPLETED', 'VIDEO_COMPLETED')),
            SUM(event_type IN ('FAVORITE_ADDED', 'FAVORITE_REMOVED')),
            COALESCE(SUM(duration_seconds), 0),
            COALESCE(AVG(engagement_score), 0),
            MAX(category_id),
            MAX(category_label),
            MAX(tags),
            MAX(interest_tags)
         FROM app_behavior_events
         WHERE device_id_hash = :device_id_hash
           AND content_type = :content_type
           AND " . behavior_tracked_events_sql() . "
           AND created_at >= UTC_DATE()
         GROUP BY device_id_hash, content_type"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'content_type' => $contentType,
    ]);
}

function behavior_recompute_device_segments(PDO $pdo, string $deviceHash): void
{
    $rows = behavior_device_summary($pdo, $deviceHash, 30);
    $totalEvents = (int) ($rows['total_events'] ?? 0);
    if ($totalEvents <= 0) {
        return;
    }

    $segments = behavior_interpret_segments($rows);
    foreach ($segments as $segment) {
        behavior_upsert_segment($pdo, $deviceHash, $segment);
    }
}

function behavior_device_summary(PDO $pdo, string $deviceHash, int $days = 30): array
{
    $days = max(1, min(90, $days));
    $statement = $pdo->prepare(
        "SELECT
            COUNT(*) AS total_events,
            COUNT(DISTINCT DATE(created_at)) AS active_days,
            MAX(created_at) AS last_event_at,
            SUM(content_type = 'LIVE_TV') AS live_events,
            SUM(content_type = 'MOVIE') AS movie_events,
            SUM(content_type IN ('SERIES', 'EPISODE')) AS series_events,
            SUM(content_type = 'YOUTUBE') AS youtube_events,
            SUM(event_type IN ('PLAYBACK_PROGRESS', 'PLAYER_READY')) AS playback_starts,
            SUM(event_type IN ('PLAYBACK_COMPLETED', 'VIDEO_COMPLETED')) AS playback_completed,
            SUM(event_type = 'SEARCH_PERFORMED') AS searches,
            SUM(event_type IN ('FAVORITE_ADDED', 'FAVORITE_REMOVED')) AS favorite_events,
            COALESCE(AVG(engagement_score), 0) AS avg_engagement
         FROM app_behavior_events
         WHERE device_id_hash = :device_id_hash
           AND " . behavior_tracked_events_sql() . "
           AND created_at >= DATE_SUB(NOW(), INTERVAL {$days} DAY)"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);
    $summary = $statement->fetch() ?: [];
    $summary['content_breakdown'] = behavior_group_counts($pdo, $deviceHash, $days, 'content_type');
    $summary['category_breakdown'] = behavior_group_counts($pdo, $deviceHash, $days, 'category_label');
    $summary['language_breakdown'] = behavior_group_counts($pdo, $deviceHash, $days, 'content_language');
    $summary['country_breakdown'] = behavior_group_counts($pdo, $deviceHash, $days, 'content_country');
    $summary['region_breakdown'] = behavior_group_counts($pdo, $deviceHash, $days, 'content_region');
    $summary['interest_breakdown'] = behavior_interest_counts($pdo, $days, $deviceHash);
    $summary['segments'] = behavior_load_device_segments($pdo, $deviceHash);
    $summary['recent_events'] = behavior_recent_device_events($pdo, $deviceHash);

    return $summary;
}

function behavior_group_counts(PDO $pdo, string $deviceHash, int $days, string $column): array
{
    $allowed = ['content_type', 'category_label', 'content_language', 'content_country', 'content_region'];
    if (!in_array($column, $allowed, true)) {
        return [];
    }
    $statement = $pdo->prepare(
        "SELECT COALESCE({$column}, 'UNKNOWN') AS label, COUNT(*) AS count
         FROM app_behavior_events
         WHERE device_id_hash = :device_id_hash
           AND " . behavior_tracked_events_sql() . "
           AND created_at >= DATE_SUB(NOW(), INTERVAL {$days} DAY)
         GROUP BY COALESCE({$column}, 'UNKNOWN')
         ORDER BY count DESC
         LIMIT 8"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return $statement->fetchAll();
}

function behavior_interest_counts(PDO $pdo, int $days, ?string $deviceHash = null): array
{
    $where = behavior_tracked_events_sql() . " AND created_at >= DATE_SUB(NOW(), INTERVAL {$days} DAY) AND interest_tags IS NOT NULL AND interest_tags <> ''";
    $params = [];
    if ($deviceHash !== null) {
        $where .= ' AND device_id_hash = :device_id_hash';
        $params['device_id_hash'] = $deviceHash;
    }
    $statement = $pdo->prepare(
        "SELECT interest_tags
         FROM app_behavior_events
         WHERE {$where}
         ORDER BY id DESC
         LIMIT 1000"
    );
    $statement->execute($params);
    $counts = [];
    foreach ($statement->fetchAll() as $row) {
        foreach (explode(',', (string) ($row['interest_tags'] ?? '')) as $tag) {
            $tag = trim($tag);
            if ($tag !== '') {
                $counts[$tag] = ($counts[$tag] ?? 0) + 1;
            }
        }
    }
    arsort($counts);
    $rows = [];
    foreach (array_slice($counts, 0, 10, true) as $label => $count) {
        $rows[] = ['label' => $label, 'count' => $count];
    }

    return $rows;
}

function behavior_interpret_segments(array $summary): array
{
    $total = max(1, (int) ($summary['total_events'] ?? 0));
    $segments = [];
    $contentMap = [
        'live_heavy' => ['Live TV dominant', 'CONTENT', (int) ($summary['live_events'] ?? 0)],
        'movies_heavy' => ['Films dominant', 'CONTENT', (int) ($summary['movie_events'] ?? 0)],
        'series_heavy' => ['Series dominant', 'CONTENT', (int) ($summary['series_events'] ?? 0)],
        'youtube_heavy' => ['YouTube dominant', 'CONTENT', (int) ($summary['youtube_events'] ?? 0)],
    ];
    foreach ($contentMap as $key => [$label, $group, $count]) {
        $score = (int) min(100, round(($count / $total) * 140));
        if ($score >= 35) {
            $segments[] = behavior_segment($key, $label, $group, $score, "part={$count}/{$total}");
        }
    }

    foreach (($summary['category_breakdown'] ?? []) as $category) {
        $label = strtolower((string) ($category['label'] ?? ''));
        $count = (int) ($category['count'] ?? 0);
        $score = (int) min(100, round(($count / $total) * 160));
        foreach ([
            'sports_interest' => ['sport', 'Sport'],
            'news_interest' => ['news|info|actual|journal', 'Actualites'],
            'kids_interest' => ['kids|enfant|cartoon|jeunesse', 'Kids'],
            'music_interest' => ['music|musique|clip', 'Musique'],
            'documentary_interest' => ['docu|nature|history|histoire', 'Documentaire'],
            'cinema_interest' => ['cinema|movie|film|vod', 'Cinema'],
        ] as $key => [$pattern, $segmentLabel]) {
            if ($score >= 25 && preg_match('/' . $pattern . '/i', $label)) {
                $segments[] = behavior_segment($key, $segmentLabel, 'CONTENT', $score, "categorie={$category['label']}");
            }
        }
    }

    foreach (($summary['interest_breakdown'] ?? []) as $interest) {
        $value = strtolower((string) ($interest['label'] ?? ''));
        $count = (int) ($interest['count'] ?? 0);
        if ($value !== '' && $count >= 2) {
            $score = min(100, (int) round(($count / $total) * 150));
            $segments[] = behavior_segment(
                'interest_' . preg_replace('/[^a-z0-9_]/', '', $value),
                behavior_interest_label($value),
                'INTEREST',
                max(35, $score),
                "signals={$count}"
            );
        }
    }

    $activeDays = (int) ($summary['active_days'] ?? 0);
    $playbackStarts = (int) ($summary['playback_starts'] ?? 0);
    $completed = (int) ($summary['playback_completed'] ?? 0);
    $avgEngagement = (float) ($summary['avg_engagement'] ?? 0);
    if ($activeDays >= 10 || $playbackStarts >= 35) {
        $segments[] = behavior_segment('high_engagement', 'Engagement eleve', 'ENGAGEMENT', min(100, $activeDays * 8 + $playbackStarts), "jours={$activeDays};lectures={$playbackStarts}");
    } elseif ($activeDays >= 3 || $playbackStarts >= 8) {
        $segments[] = behavior_segment('medium_engagement', 'Engagement moyen', 'ENGAGEMENT', min(85, 35 + $activeDays * 8 + $playbackStarts), "jours={$activeDays};lectures={$playbackStarts}");
    } else {
        $segments[] = behavior_segment('low_engagement', 'Engagement faible', 'ENGAGEMENT', 45, "jours={$activeDays};lectures={$playbackStarts}");
    }
    if ($playbackStarts > 0 && ($completed / max(1, $playbackStarts)) >= 0.45) {
        $segments[] = behavior_segment('completion_friendly', 'Termine souvent les contenus', 'ENGAGEMENT', min(100, (int) round(($completed / max(1, $playbackStarts)) * 100)), "completes={$completed}/{$playbackStarts}");
    }
    if ($avgEngagement >= 70) {
        $segments[] = behavior_segment('deep_viewer', 'Visionnage profond', 'ENGAGEMENT', (int) min(100, $avgEngagement), "score_moyen={$avgEngagement}");
    }

    foreach ([['language_breakdown', 'LANGUAGE', 'language_'], ['country_breakdown', 'COUNTRY', 'country_'], ['region_breakdown', 'REGION', 'region_']] as [$source, $group, $prefix]) {
        foreach (($summary[$source] ?? []) as $row) {
            $value = strtolower((string) ($row['label'] ?? ''));
            $count = (int) ($row['count'] ?? 0);
            if ($value !== '' && $value !== 'unknown' && $count >= 3) {
                $segments[] = behavior_segment($prefix . preg_replace('/[^a-z0-9_]/', '', $value), behavior_geo_label($group, $value), $group, min(100, (int) round(($count / $total) * 130)), "events={$count}");
            }
        }
    }

    return $segments;
}

function behavior_interest_label(string $value): string
{
    return [
        'sports' => 'Interet Sport',
        'news' => 'Interet News',
        'kids' => 'Interet Enfants',
        'cinema' => 'Interet Cinema',
        'music' => 'Interet Music',
        'documentaries' => 'Interet Documentaires',
        'religion' => 'Interet Religion',
        'entertainment' => 'Interet Divertissement',
    ][$value] ?? ('Interet ' . ucfirst($value));
}

function behavior_geo_label(string $group, string $value): string
{
    $upper = strtoupper($value);
    if ($group === 'LANGUAGE') {
        return 'Langue ' . ([
            'AR' => 'Arabe',
            'FR' => 'Francais',
            'EN' => 'Anglais',
            'ES' => 'Espagnol',
            'PT' => 'Portugais',
            'IT' => 'Italien',
            'DE' => 'Allemand',
            'TR' => 'Turc',
        ][$upper] ?? $upper);
    }
    if ($group === 'COUNTRY') {
        return 'Pays ' . ([
            'FR' => 'France',
            'TN' => 'Tunisie',
            'MA' => 'Maroc',
            'DZ' => 'Algerie',
            'EG' => 'Egypte',
            'SA' => 'Arabie Saoudite',
            'AE' => 'Emirats',
            'GB' => 'Royaume-Uni',
            'US' => 'Etats-Unis',
            'CA' => 'Canada',
            'ES' => 'Espagne',
            'IT' => 'Italie',
            'DE' => 'Allemagne',
            'TR' => 'Turquie',
        ][$upper] ?? $upper);
    }

    return 'Region ' . ucwords($value);
}

function behavior_segment(string $key, string $label, string $group, int $score, string $evidence): array
{
    return [
        'key' => $key,
        'label' => $label,
        'group' => $group,
        'score' => max(0, min(100, $score)),
        'confidence' => max(20, min(100, $score + 10)),
        'evidence' => smartvision_text_substr($evidence, 0, 500),
    ];
}

function behavior_upsert_segment(PDO $pdo, string $deviceHash, array $segment): void
{
    $statement = $pdo->prepare(
        "INSERT INTO user_segments
            (device_id_hash, segment_key, segment_label, segment_group, score, confidence, evidence, first_seen_at, last_seen_at, expires_at)
         VALUES
            (:device_id_hash, :segment_key, :segment_label, :segment_group, :score, :confidence, :evidence, NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY))
         ON DUPLICATE KEY UPDATE
            segment_label = VALUES(segment_label),
            segment_group = VALUES(segment_group),
            score = VALUES(score),
            confidence = VALUES(confidence),
            evidence = VALUES(evidence),
            last_seen_at = NOW(),
            expires_at = DATE_ADD(NOW(), INTERVAL 45 DAY)"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'segment_key' => $segment['key'],
        'segment_label' => $segment['label'],
        'segment_group' => $segment['group'],
        'score' => $segment['score'],
        'confidence' => $segment['confidence'],
        'evidence' => $segment['evidence'],
    ]);
}

function behavior_load_device_segments(PDO $pdo, string $deviceHash): array
{
    $statement = $pdo->prepare(
        "SELECT segment_key, segment_label, segment_group, score, confidence, evidence, last_seen_at, expires_at
         FROM user_segments
         WHERE device_id_hash = :device_id_hash
           AND (expires_at IS NULL OR expires_at > NOW())
         ORDER BY score DESC, last_seen_at DESC
         LIMIT 20"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return $statement->fetchAll();
}

function behavior_recent_device_events(PDO $pdo, string $deviceHash): array
{
    $statement = $pdo->prepare(
        "SELECT event_type, content_type, category_id, category_label, content_title, content_language, content_country,
                content_region, interest_tags, duration_seconds, position_seconds, engagement_score, source_screen, tags, created_at
         FROM app_behavior_events
         WHERE device_id_hash = :device_id_hash
           AND " . behavior_tracked_events_sql() . "
         ORDER BY id DESC
         LIMIT 30"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return $statement->fetchAll();
}

function behavior_admin_dashboard(PDO $pdo): array
{
    behavior_ensure_schema($pdo);
    $summary = $pdo->query(
        "SELECT
            COUNT(*) AS total_events,
            COUNT(DISTINCT device_id_hash) AS tracked_devices,
            COUNT(DISTINCT CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN device_id_hash END) AS active_7d,
            MAX(created_at) AS last_event_at,
            COALESCE(AVG(engagement_score), 0) AS avg_engagement
         FROM app_behavior_events
         WHERE " . behavior_tracked_events_sql()
    )->fetch() ?: [];
    $segments = $pdo->query(
        "SELECT segment_key, segment_label, segment_group, COUNT(*) AS devices, ROUND(AVG(score), 1) AS avg_score, MAX(last_seen_at) AS last_seen_at
         FROM user_segments
         WHERE expires_at IS NULL OR expires_at > NOW()
         GROUP BY segment_key, segment_label, segment_group
         ORDER BY devices DESC, avg_score DESC
         LIMIT 80"
    )->fetchAll();
    $content = $pdo->query(
        "SELECT content_type, COUNT(*) AS events, COUNT(DISTINCT device_id_hash) AS devices
         FROM app_behavior_events
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
           AND " . behavior_tracked_events_sql() . "
         GROUP BY content_type
         ORDER BY events DESC"
    )->fetchAll();
    $regions = behavior_admin_group_counts($pdo, 'content_region');
    $countries = behavior_admin_group_counts($pdo, 'content_country');
    $languages = behavior_admin_group_counts($pdo, 'content_language');
    $interests = behavior_interest_counts($pdo, 30);
    $recent = $pdo->query(
        "SELECT e.created_at, d.public_device_code,
                CASE
                    WHEN a.activation_type = 'free_ads' OR d.free_with_ads_status = 'active' OR c.license_type = 'free' THEN 'free_ads'
                    WHEN a.activation_type IN ('trial_demo', 'trial_pending_xtream') OR d.trial_status IN ('active', 'pending_xtream') OR c.license_type = 'trial' THEN 'trial_demo'
                    ELSE 'premium'
                END AS license_type,
                e.event_type, e.content_type, e.source_screen, e.category_label, e.content_title,
                e.platform, e.app_version, e.engagement_score
         FROM app_behavior_events e
         LEFT JOIN devices d ON e.device_id_hash = SHA2(d.device_id, 256)
         LEFT JOIN device_activations a ON a.id = (
             SELECT da.id FROM device_activations da WHERE da.device_id = d.device_id ORDER BY da.id DESC LIMIT 1
         )
         LEFT JOIN activation_codes c ON c.id = a.activation_code_id
         WHERE " . behavior_tracked_events_sql('e') . "
         ORDER BY e.id DESC
         LIMIT 80"
    )->fetchAll();

    return [
        'available' => true,
        'summary' => $summary,
        'segments' => $segments,
        'content' => $content,
        'regions' => $regions,
        'countries' => $countries,
        'languages' => $languages,
        'interests' => $interests,
        'recent_events' => $recent,
    ];
}

function behavior_admin_group_counts(PDO $pdo, string $column): array
{
    if (!in_array($column, ['content_region', 'content_country', 'content_language'], true)) {
        return [];
    }
    $rows = $pdo->query(
        "SELECT {$column} AS label, COUNT(*) AS count, COUNT(DISTINCT device_id_hash) AS devices
         FROM app_behavior_events
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
           AND " . behavior_tracked_events_sql() . "
           AND {$column} IS NOT NULL
           AND {$column} <> ''
         GROUP BY {$column}
         ORDER BY count DESC
         LIMIT 12"
    );

    return $rows ? $rows->fetchAll() : [];
}

function behavior_admin_device_analysis(PDO $pdo, string $deviceId): array
{
    behavior_ensure_schema($pdo);
    $deviceHash = hash('sha256', $deviceId);

    return behavior_device_summary($pdo, $deviceHash, 30);
}
