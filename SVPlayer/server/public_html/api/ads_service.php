<?php
declare(strict_types=1);

const SMARTVISION_ADS_DEFAULT_TEST_TAG = 'https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&correlator=';

function ads_bool(mixed $value): int
{
    return in_array((string) $value, ['1', 'true', 'on', 'yes'], true) ? 1 : 0;
}

function ads_private_config_value(string $key): string
{
    $config = load_database_config();
    $value = $config[$key] ?? getenv('SMARTVISION_' . strtoupper($key));

    return trim((string) $value);
}

function ads_default_settings(): array
{
    $productionTag = ads_private_config_value('hilltopads_vast_tag_url');

    return [
        'ads_enabled' => 1,
        'provider' => 'HILLTOPADS_VAST',
        'use_test_ads' => $productionTag === '' ? 1 : 0,
        'vast_production_tag_url' => $productionTag,
        'vast_test_tag_url' => SMARTVISION_ADS_DEFAULT_TEST_TAG,
        'min_minutes_between_ads' => 30,
        'max_ads_per_day' => 3,
        'show_ad_before_live_stream' => 1,
        'show_ad_before_movie' => 1,
        'show_ad_before_series_episode' => 1,
        'allow_playback_if_ad_fails' => 1,
        'ads_only_inside_player' => 1,
        'estimated_ecpm_eur' => '5.00',
        'hilltop_site_id' => '',
        'hilltop_zone_id' => '',
        'config_version' => 1,
        'updated_by' => 'system',
    ];
}

function ads_ensure_schema(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS ads_settings (
            id TINYINT UNSIGNED NOT NULL PRIMARY KEY DEFAULT 1,
            ads_enabled TINYINT(1) NOT NULL DEFAULT 1,
            provider ENUM('HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST') NOT NULL DEFAULT 'HILLTOPADS_VAST',
            use_test_ads TINYINT(1) NOT NULL DEFAULT 1,
            vast_production_tag_url TEXT NULL,
            vast_test_tag_url TEXT NULL,
            min_minutes_between_ads INT UNSIGNED NOT NULL DEFAULT 30,
            max_ads_per_day INT UNSIGNED NOT NULL DEFAULT 3,
            show_ad_before_live_stream TINYINT(1) NOT NULL DEFAULT 1,
            show_ad_before_movie TINYINT(1) NOT NULL DEFAULT 1,
            show_ad_before_series_episode TINYINT(1) NOT NULL DEFAULT 1,
            allow_playback_if_ad_fails TINYINT(1) NOT NULL DEFAULT 1,
            ads_only_inside_player TINYINT(1) NOT NULL DEFAULT 1,
            estimated_ecpm_eur DECIMAL(8,2) NOT NULL DEFAULT 5.00,
            hilltop_site_id VARCHAR(64) NULL,
            hilltop_zone_id VARCHAR(64) NULL,
            config_version INT UNSIGNED NOT NULL DEFAULT 1,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            updated_by VARCHAR(100) NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS ads_events (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            device_id_hash CHAR(64) NULL,
            app_version VARCHAR(50) NULL,
            platform ENUM('ANDROID_TV', 'FIRE_TV', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            user_status ENUM('PREMIUM_ACTIVE', 'TRIAL_ACTIVE', 'TRIAL_EXPIRED', 'LICENSE_EXPIRED', 'FREE_WITH_ADS', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            content_type ENUM('LIVE_TV', 'MOVIE', 'SERIES', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            provider ENUM('HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
            event_type VARCHAR(40) NOT NULL,
            reason VARCHAR(60) NULL,
            error_code VARCHAR(60) NULL,
            error_message VARCHAR(255) NULL,
            ad_duration_seconds INT UNSIGNED NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_ads_events_created (created_at),
            INDEX idx_ads_events_event_type (event_type),
            INDEX idx_ads_events_provider (provider),
            INDEX idx_ads_events_content_type (content_type),
            INDEX idx_ads_events_platform (platform),
            INDEX idx_ads_events_user_status (user_status),
            INDEX idx_ads_events_device_time (device_id_hash, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    foreach ([
        "ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS estimated_ecpm_eur DECIMAL(8,2) NOT NULL DEFAULT 5.00 AFTER ads_only_inside_player",
        "ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS hilltop_site_id VARCHAR(64) NULL AFTER estimated_ecpm_eur",
        "ALTER TABLE ads_settings ADD COLUMN IF NOT EXISTS hilltop_zone_id VARCHAR(64) NULL AFTER hilltop_site_id",
    ] as $statement) {
        try {
            $pdo->exec($statement);
        } catch (Throwable $exception) {
            error_log('SmartVision ads schema alter skipped.');
        }
    }

    $defaults = ads_default_settings();
    $statement = $pdo->prepare(
        "INSERT INTO ads_settings
            (id, ads_enabled, provider, use_test_ads, vast_production_tag_url, vast_test_tag_url,
             min_minutes_between_ads, max_ads_per_day, show_ad_before_live_stream, show_ad_before_movie,
             show_ad_before_series_episode, allow_playback_if_ad_fails, ads_only_inside_player,
             estimated_ecpm_eur, hilltop_site_id, hilltop_zone_id, config_version, updated_by)
         VALUES
            (1, :ads_enabled, :provider, :use_test_ads, :vast_production_tag_url, :vast_test_tag_url,
             :min_minutes_between_ads, :max_ads_per_day, :show_ad_before_live_stream, :show_ad_before_movie,
             :show_ad_before_series_episode, :allow_playback_if_ad_fails, :ads_only_inside_player,
             :estimated_ecpm_eur, :hilltop_site_id, :hilltop_zone_id, :config_version, :updated_by)
         ON DUPLICATE KEY UPDATE id = id"
    );
    $statement->execute($defaults);

    $privateProductionTag = ads_private_config_value('hilltopads_vast_tag_url');
    if ($privateProductionTag !== '') {
        $syncTag = $pdo->prepare(
            "UPDATE ads_settings
             SET vast_production_tag_url = :vast_production_tag_url,
                 use_test_ads = 0,
                 updated_at = NOW()
             WHERE id = 1
               AND (vast_production_tag_url IS NULL OR vast_production_tag_url = '')
               AND (updated_by IS NULL OR updated_by = 'system')"
        );
        $syncTag->execute(['vast_production_tag_url' => $privateProductionTag]);
    }
}

function ads_load_settings(PDO $pdo): array
{
    ads_ensure_schema($pdo);
    $row = $pdo->query('SELECT * FROM ads_settings WHERE id = 1 LIMIT 1')->fetch();
    if (!is_array($row)) {
        return ads_default_settings();
    }

    $row['ads_enabled'] = (int) $row['ads_enabled'];
    $row['use_test_ads'] = (int) $row['use_test_ads'];
    $row['min_minutes_between_ads'] = (int) $row['min_minutes_between_ads'];
    $row['max_ads_per_day'] = (int) $row['max_ads_per_day'];
    $row['show_ad_before_live_stream'] = (int) $row['show_ad_before_live_stream'];
    $row['show_ad_before_movie'] = (int) $row['show_ad_before_movie'];
    $row['show_ad_before_series_episode'] = (int) $row['show_ad_before_series_episode'];
    $row['allow_playback_if_ad_fails'] = (int) $row['allow_playback_if_ad_fails'];
    $row['ads_only_inside_player'] = 1;
    $row['config_version'] = (int) $row['config_version'];
    $row['estimated_ecpm_eur'] = (string) ($row['estimated_ecpm_eur'] ?? '5.00');
    $row['hilltop_site_id'] = (string) ($row['hilltop_site_id'] ?? '');
    $row['hilltop_zone_id'] = (string) ($row['hilltop_zone_id'] ?? '');

    return $row;
}

function ads_selected_vast_tag(array $settings): string
{
    $tag = (int) ($settings['use_test_ads'] ?? 1) === 1
        ? (string) ($settings['vast_test_tag_url'] ?? '')
        : (string) ($settings['vast_production_tag_url'] ?? '');

    return trim($tag);
}

function ads_public_config(PDO $pdo): array
{
    $settings = ads_load_settings($pdo);

    return [
        'adsEnabled' => (bool) $settings['ads_enabled'],
        'provider' => (string) $settings['provider'],
        'useTestAds' => (bool) $settings['use_test_ads'],
        'vastTagUrl' => ads_selected_vast_tag($settings),
        'minMinutesBetweenAds' => (int) $settings['min_minutes_between_ads'],
        'maxAdsPerDay' => (int) $settings['max_ads_per_day'],
        'showAdBeforeLiveStream' => (bool) $settings['show_ad_before_live_stream'],
        'showAdBeforeMovie' => (bool) $settings['show_ad_before_movie'],
        'showAdBeforeSeriesEpisode' => (bool) $settings['show_ad_before_series_episode'],
        'allowPlaybackIfAdFails' => (bool) $settings['allow_playback_if_ad_fails'],
        'adsOnlyInsidePlayer' => true,
        'configVersion' => (int) $settings['config_version'],
    ];
}

function ads_allowed_event_types(): array
{
    return [
        'AD_CONFIG_FETCHED',
        'AD_CONFIG_FAILED',
        'AD_REQUESTED',
        'AD_ALLOWED',
        'AD_BLOCKED',
        'AD_LOADED',
        'AD_STARTED',
        'AD_IMPRESSION',
        'AD_COMPLETED',
        'AD_SKIPPED',
        'AD_FAILED',
        'AD_FALLBACK_TO_CONTENT',
        'CONTENT_STARTED_AFTER_AD',
    ];
}

function ads_allowed_reasons(): array
{
    return [
        'USER_PREMIUM',
        'TRIAL_ACTIVE',
        'ADS_DISABLED',
        'NOT_FREE_WITH_ADS',
        'MIN_INTERVAL_NOT_REACHED',
        'DAILY_LIMIT_REACHED',
        'CONTENT_TYPE_DISABLED',
        'INVALID_CONTEXT',
        'MISSING_VAST_TAG',
    ];
}

function ads_clean_enum(mixed $value, array $allowed, string $default): string
{
    $clean = strtoupper(preg_replace('/[^A-Z0-9_]/', '', trim((string) $value)) ?: '');

    return in_array($clean, $allowed, true) ? $clean : $default;
}

function ads_store_event(PDO $pdo, array $payload): array
{
    ads_ensure_schema($pdo);
    $eventType = ads_clean_enum($payload['eventType'] ?? '', ads_allowed_event_types(), '');
    if ($eventType === '') {
        throw new InvalidArgumentException('eventType invalide.');
    }

    $deviceId = clean_device_id($payload['deviceId'] ?? null);
    if ($deviceId === '') {
        throw new InvalidArgumentException('deviceId requis.');
    }
    $deviceHash = hash('sha256', $deviceId);
    if (ads_device_rate_limited($pdo, $deviceHash)) {
        throw new RuntimeException('Trop d evenements pub pour cet appareil.');
    }

    $reason = $payload['reason'] ?? null;
    $reason = $reason === null ? null : ads_clean_enum($reason, ads_allowed_reasons(), '');
    if ($reason === '') {
        $reason = null;
    }

    $duration = filter_var($payload['adDurationSeconds'] ?? null, FILTER_VALIDATE_INT, [
        'options' => ['min_range' => 0, 'max_range' => 86400],
    ]);

    $statement = $pdo->prepare(
        "INSERT INTO ads_events
            (device_id_hash, app_version, platform, user_status, content_type, provider, event_type,
             reason, error_code, error_message, ad_duration_seconds, created_at)
         VALUES
            (:device_id_hash, :app_version, :platform, :user_status, :content_type, :provider, :event_type,
             :reason, :error_code, :error_message, :ad_duration_seconds, NOW())"
    );
    $statement->execute([
        'device_id_hash' => $deviceHash,
        'app_version' => clean_optional_text($payload['appVersion'] ?? null, 50),
        'platform' => ads_clean_enum($payload['platform'] ?? 'UNKNOWN', ['ANDROID_TV', 'FIRE_TV', 'UNKNOWN'], 'UNKNOWN'),
        'user_status' => ads_clean_enum($payload['userStatus'] ?? 'UNKNOWN', ['PREMIUM_ACTIVE', 'TRIAL_ACTIVE', 'TRIAL_EXPIRED', 'LICENSE_EXPIRED', 'FREE_WITH_ADS', 'UNKNOWN'], 'UNKNOWN'),
        'content_type' => ads_clean_enum($payload['contentType'] ?? 'UNKNOWN', ['LIVE_TV', 'MOVIE', 'SERIES', 'UNKNOWN'], 'UNKNOWN'),
        'provider' => ads_clean_enum($payload['provider'] ?? 'UNKNOWN', ['HILLTOPADS_VAST', 'GOOGLE_IMA_TEST', 'CUSTOM_VAST', 'UNKNOWN'], 'UNKNOWN'),
        'event_type' => $eventType,
        'reason' => $reason,
        'error_code' => clean_optional_text($payload['errorCode'] ?? null, 60),
        'error_message' => clean_optional_text($payload['errorMessage'] ?? null, 255),
        'ad_duration_seconds' => $duration === false ? null : $duration,
    ]);

    return [
        'id' => (int) $pdo->lastInsertId(),
        'eventType' => $eventType,
    ];
}

function ads_device_rate_limited(PDO $pdo, string $deviceHash): bool
{
    $statement = $pdo->prepare(
        "SELECT COUNT(*) FROM ads_events
         WHERE device_id_hash = :device_id_hash
           AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)"
    );
    $statement->execute(['device_id_hash' => $deviceHash]);

    return (int) $statement->fetchColumn() > 300;
}

function ads_period_days(mixed $value): int
{
    $days = filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 30]]);

    return in_array((int) $days, [1, 7, 30], true) ? (int) $days : 1;
}

function ads_period_start_sql(int $days): string
{
    return $days === 1
        ? 'UTC_DATE()'
        : 'DATE_SUB(UTC_DATE(), INTERVAL ' . ($days - 1) . ' DAY)';
}

function ads_dashboard(PDO $pdo, int $days): array
{
    ads_ensure_schema($pdo);
    $settings = ads_load_settings($pdo);
    $summary = ads_event_summary($pdo, $days);
    $series = ads_daily_series($pdo, $days);
    $recentEvents = ads_recent_events($pdo);
    $provider = ads_provider_status($settings, $summary);
    $diagnostics = ads_diagnostics($settings, $summary);
    $hilltop = ads_hilltop_summary($settings, $days);

    return [
        'period_days' => $days,
        'settings' => $settings,
        'summary' => $summary,
        'series' => $series,
        'recent_events' => $recentEvents,
        'provider' => $provider,
        'diagnostics' => $diagnostics,
        'hilltop' => $hilltop,
    ];
}

function ads_event_summary(PDO $pdo, int $days): array
{
    $startSql = ads_period_start_sql($days);
    $row = $pdo->query(
        "SELECT
            COALESCE(SUM(event_type = 'AD_REQUESTED'), 0) AS requested,
            COALESCE(SUM(event_type = 'AD_STARTED'), 0) AS shown,
            COALESCE(SUM(event_type = 'AD_COMPLETED'), 0) AS completed,
            COALESCE(SUM(event_type = 'AD_FAILED'), 0) AS failed,
            COALESCE(SUM(event_type = 'AD_BLOCKED'), 0) AS blocked,
            COALESCE(SUM(event_type = 'AD_BLOCKED' AND reason = 'MIN_INTERVAL_NOT_REACHED'), 0) AS blocked_frequency,
            COALESCE(SUM(event_type = 'AD_BLOCKED' AND reason = 'DAILY_LIMIT_REACHED'), 0) AS blocked_daily_limit,
            COUNT(DISTINCT CASE WHEN user_status = 'FREE_WITH_ADS' THEN device_id_hash END) AS free_devices,
            MAX(CASE WHEN event_type = 'AD_STARTED' THEN created_at END) AS last_started_at,
            MAX(CASE WHEN event_type = 'AD_FAILED' THEN created_at END) AS last_failed_at,
            MAX(created_at) AS last_event_at
         FROM ads_events
         WHERE created_at >= {$startSql}"
    )->fetch() ?: [];

    $lastError = $pdo->query(
        "SELECT error_code, error_message, created_at
         FROM ads_events
         WHERE event_type = 'AD_FAILED'
         ORDER BY id DESC
         LIMIT 1"
    )->fetch() ?: null;

    $contentRows = $pdo->query(
        "SELECT content_type, COUNT(*) AS requests
         FROM ads_events
         WHERE created_at >= {$startSql}
           AND event_type IN ('AD_REQUESTED', 'AD_ALLOWED')
         GROUP BY content_type"
    )->fetchAll();
    $platformRows = $pdo->query(
        "SELECT platform, COUNT(*) AS requests
         FROM ads_events
         WHERE created_at >= {$startSql}
           AND event_type IN ('AD_REQUESTED', 'AD_ALLOWED')
         GROUP BY platform"
    )->fetchAll();

    $requested = (int) ($row['requested'] ?? 0);
    $shown = (int) ($row['shown'] ?? 0);
    $completed = (int) ($row['completed'] ?? 0);
    $failed = (int) ($row['failed'] ?? 0);

    return [
        'requested' => $requested,
        'shown' => $shown,
        'completed' => $completed,
        'failed' => $failed,
        'blocked' => (int) ($row['blocked'] ?? 0),
        'blocked_frequency' => (int) ($row['blocked_frequency'] ?? 0),
        'blocked_daily_limit' => (int) ($row['blocked_daily_limit'] ?? 0),
        'free_devices' => (int) ($row['free_devices'] ?? 0),
        'failure_rate' => $requested > 0 ? round(($failed / $requested) * 100, 1) : 0.0,
        'completion_rate' => $shown > 0 ? round(($completed / $shown) * 100, 1) : 0.0,
        'last_started_at' => $row['last_started_at'] ?? null,
        'last_failed_at' => $row['last_failed_at'] ?? null,
        'last_event_at' => $row['last_event_at'] ?? null,
        'last_error' => is_array($lastError) ? $lastError : null,
        'content_breakdown' => $contentRows,
        'platform_breakdown' => $platformRows,
    ];
}

function ads_daily_series(PDO $pdo, int $days): array
{
    $startSql = ads_period_start_sql($days);
    $rows = $pdo->query(
        "SELECT DATE(created_at) AS day,
                SUM(event_type = 'AD_REQUESTED') AS requested,
                SUM(event_type = 'AD_STARTED') AS shown,
                SUM(event_type = 'AD_COMPLETED') AS completed,
                SUM(event_type = 'AD_FAILED') AS failed
         FROM ads_events
         WHERE created_at >= {$startSql}
         GROUP BY DATE(created_at)
         ORDER BY day"
    )->fetchAll();
    $byDay = [];
    foreach ($rows as $row) {
        $byDay[(string) $row['day']] = $row;
    }

    $series = [];
    for ($offset = $days - 1; $offset >= 0; $offset--) {
        $day = gmdate('Y-m-d', strtotime('-' . $offset . ' days'));
        $row = $byDay[$day] ?? [];
        $shown = (int) ($row['shown'] ?? 0);
        $completed = (int) ($row['completed'] ?? 0);
        $series[] = [
            'day' => $day,
            'label' => gmdate('d/m', strtotime($day)),
            'requested' => (int) ($row['requested'] ?? 0),
            'shown' => $shown,
            'completed' => $completed,
            'failed' => (int) ($row['failed'] ?? 0),
            'completion_rate' => $shown > 0 ? round(($completed / $shown) * 100, 1) : 0.0,
        ];
    }

    return $series;
}

function ads_recent_events(PDO $pdo): array
{
    return $pdo->query(
        "SELECT id, device_id_hash, app_version, platform, user_status, content_type, provider,
                event_type, reason, error_code, error_message, ad_duration_seconds, created_at
         FROM ads_events
         ORDER BY id DESC
         LIMIT 80"
    )->fetchAll();
}

function ads_mask_hash(?string $hash): string
{
    $hash = trim((string) $hash);
    if ($hash === '') {
        return '-';
    }

    return substr($hash, 0, 10) . '...' . substr($hash, -6);
}

function ads_provider_status(array $settings, array $summary): array
{
    $selectedTag = ads_selected_vast_tag($settings);
    $issues = [];
    if ((int) $settings['ads_enabled'] !== 1) {
        $issues[] = 'pubs desactivees';
    }
    if ($selectedTag === '') {
        $issues[] = 'URL VAST manquante';
    }
    if ((int) $settings['use_test_ads'] === 1) {
        $issues[] = 'mode test actif';
    }
    if ((float) ($summary['failure_rate'] ?? 0) > 50.0) {
        $issues[] = 'erreur recente elevee';
    }

    return [
        'provider' => (string) $settings['provider'],
        'mode' => (int) $settings['use_test_ads'] === 1 ? 'test' : 'production',
        'vast_tag_url' => $selectedTag,
        'status' => $issues === [] ? 'OK' : implode(', ', $issues),
    ];
}

function ads_diagnostics(array $settings, array $summary): array
{
    $messages = [];
    if ((int) $settings['ads_enabled'] === 1 && trim((string) $settings['vast_production_tag_url']) === '' && (int) $settings['use_test_ads'] !== 1) {
        $messages[] = ['level' => 'danger', 'message' => 'Les pubs sont activees mais aucune URL VAST production n est configuree.'];
    }
    if ((int) $settings['use_test_ads'] === 1) {
        $messages[] = ['level' => 'warning', 'message' => 'Le mode test est actif.'];
    }
    if ((float) ($summary['failure_rate'] ?? 0) > 50.0) {
        $messages[] = ['level' => 'danger', 'message' => 'Taux d echec eleve: verifier le tag HilltopAds.'];
    }
    if ((int) ($summary['requested'] ?? 0) > 0 && empty($summary['last_started_at'])) {
        $messages[] = ['level' => 'warning', 'message' => 'Aucune pub affichee alors que des demandes existent sur la periode.'];
    }
    if (empty($summary['last_event_at'])) {
        $messages[] = ['level' => 'warning', 'message' => 'Aucun evenement pub recu recemment.'];
    } elseif (strtotime((string) $summary['last_event_at']) < time() - 86400) {
        $messages[] = ['level' => 'warning', 'message' => 'Aucun evenement pub recu depuis plus de 24h.'];
    }
    if ((int) $settings['max_ads_per_day'] > 5) {
        $messages[] = ['level' => 'warning', 'message' => 'maxAdsPerDay est superieur a 5.'];
    }
    if ((int) $settings['min_minutes_between_ads'] < 10) {
        $messages[] = ['level' => 'warning', 'message' => 'minMinutesBetweenAds est inferieur a 10.'];
    }
    if ((int) $settings['allow_playback_if_ad_fails'] !== 1) {
        $messages[] = ['level' => 'danger', 'message' => 'allowPlaybackIfAdFails est desactive: risque de bloquer la video.'];
    }

    if ($messages === []) {
        $messages[] = ['level' => 'success', 'message' => 'Configuration correcte.'];
    }

    return $messages;
}

function ads_test_vast_tag(string $url): array
{
    $url = trim($url);
    if ($url === '' || !filter_var($url, FILTER_VALIDATE_URL)) {
        return ['ok' => false, 'message' => 'URL VAST vide ou invalide.', 'http_code' => null];
    }

    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 8,
            'ignore_errors' => true,
            'header' => "User-Agent: SmartVisionAdsDiagnostics/1.0\r\nAccept: application/xml,text/xml,*/*\r\n",
        ],
    ]);
    $body = @file_get_contents($url, false, $context);
    $statusLine = $http_response_header[0] ?? '';
    preg_match('/\s(\d{3})\s/', (string) $statusLine, $matches);
    $code = isset($matches[1]) ? (int) $matches[1] : null;

    if ($body === false || $code === null || $code >= 400) {
        return ['ok' => false, 'message' => 'Tag VAST injoignable ou refuse.', 'http_code' => $code];
    }

    return [
        'ok' => true,
        'message' => str_contains(strtolower(substr($body, 0, 5000)), '<vast') ? 'Tag VAST joignable.' : 'URL joignable, structure VAST non confirmee.',
        'http_code' => $code,
    ];
}

function ads_hilltop_summary(array $settings, int $days): array
{
    $key = ads_private_config_value('hilltopads_api_key');
    if ($key === '') {
        return ['configured' => false, 'ok' => false, 'message' => 'Cle API HilltopAds absente de la configuration privee.'];
    }

    $today = gmdate('Y-m-d');
    $from = gmdate('Y-m-d', strtotime('-' . max(0, $days - 1) . ' days'));
    $balance = ads_hilltop_request('/publisher/balance', []);
    $inventory = ads_hilltop_request('/publisher/inventory', []);
    $statsParams = [
        'date' => $from,
        'date2' => $today,
        'group' => 'date',
    ];
    if (trim((string) ($settings['hilltop_zone_id'] ?? '')) !== '') {
        $statsParams['zoneID'] = trim((string) $settings['hilltop_zone_id']);
    }
    $stats = ads_hilltop_request('/publisher/listStats', $statsParams);

    return [
        'configured' => true,
        'ok' => $balance['ok'] || $inventory['ok'] || $stats['ok'],
        'message' => ($balance['ok'] || $inventory['ok'] || $stats['ok']) ? 'API HilltopAds joignable.' : 'API HilltopAds indisponible.',
        'publisher_id_present' => ads_private_config_value('hilltopads_publisher_id') !== '',
        'balance' => ads_hilltop_balance($balance),
        'inventory' => ads_hilltop_inventory($inventory),
        'stats' => ads_hilltop_stats($stats),
    ];
}

function ads_hilltop_request(string $path, array $params): array
{
    $key = ads_private_config_value('hilltopads_api_key');
    if ($key === '') {
        return ['ok' => false, 'error' => 'missing_key', 'data' => null];
    }
    $params = ['key' => $key] + $params;
    $url = 'https://api.hilltopads.com' . $path . '?' . http_build_query($params);
    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 8,
            'ignore_errors' => true,
            'header' => "Accept: application/json\r\n",
        ],
    ]);
    $response = @file_get_contents($url, false, $context);
    if ($response === false) {
        return ['ok' => false, 'error' => 'request_failed', 'data' => null];
    }
    $json = json_decode($response, true);
    if (!is_array($json)) {
        return ['ok' => false, 'error' => 'invalid_json', 'data' => null];
    }

    return [
        'ok' => strtolower((string) ($json['status'] ?? '')) === 'success',
        'error' => strtolower((string) ($json['status'] ?? '')) === 'success' ? null : (string) ($json['message'] ?? 'hilltop_error'),
        'data' => $json,
    ];
}

function ads_hilltop_balance(array $response): array
{
    $result = is_array($response['data']['result'] ?? null) ? $response['data']['result'] : [];

    return [
        'ok' => (bool) ($response['ok'] ?? false),
        'balance' => array_key_exists('balance', $result) ? (string) $result['balance'] : null,
        'currency' => (string) ($result['currency'] ?? ''),
    ];
}

function ads_hilltop_inventory(array $response): array
{
    $result = is_array($response['data']['result'] ?? null) ? $response['data']['result'] : [];
    $sites = ads_normalize_collection($result['sites'] ?? []);
    $zones = ads_normalize_collection($result['zones'] ?? []);

    return [
        'ok' => (bool) ($response['ok'] ?? false),
        'site_count' => count($sites),
        'zone_count' => count($zones),
        'sites' => array_slice($sites, 0, 8),
        'zones' => array_slice($zones, 0, 8),
    ];
}

function ads_hilltop_stats(array $response): array
{
    $rows = ads_normalize_collection($response['data']['result'] ?? []);
    $totals = ['impressions' => 0.0, 'clicks' => 0.0, 'revenue' => 0.0];
    foreach ($rows as $row) {
        $totals['impressions'] += ads_pick_number($row, ['impressions', 'impression', 'imps', 'views']);
        $totals['clicks'] += ads_pick_number($row, ['clicks', 'click']);
        $totals['revenue'] += ads_pick_number($row, ['revenue', 'profit', 'income', 'earnings', 'money']);
    }

    return [
        'ok' => (bool) ($response['ok'] ?? false),
        'row_count' => count($rows),
        'impressions' => $totals['impressions'],
        'clicks' => $totals['clicks'],
        'revenue' => $totals['revenue'],
        'rows' => array_slice($rows, 0, 10),
    ];
}

function ads_normalize_collection(mixed $value): array
{
    if (!is_array($value)) {
        return [];
    }
    $rows = [];
    foreach ($value as $item) {
        if (is_array($item)) {
            $rows[] = $item;
        }
    }

    return $rows;
}

function ads_pick_number(array $row, array $keys): float
{
    foreach ($keys as $key) {
        if (array_key_exists($key, $row) && is_numeric($row[$key])) {
            return (float) $row[$key];
        }
    }

    return 0.0;
}

function ads_short_value(array $row, array $keys): string
{
    foreach ($keys as $key) {
        if (isset($row[$key]) && trim((string) $row[$key]) !== '') {
            return smartvision_text_substr((string) $row[$key], 0, 120);
        }
    }

    return '-';
}
