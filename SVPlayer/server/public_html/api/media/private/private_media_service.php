<?php
declare(strict_types=1);

const PRIVATE_MEDIA_PROVIDER = 'eporner';
const PRIVATE_MEDIA_CONFIG_KEY = 'private_media_config';
const PRIVATE_MEDIA_CACHE_TTL_SECONDS = 1800;
const PRIVATE_MEDIA_DETAILS_TTL_SECONDS = 21600;
const PRIVATE_MEDIA_RATE_LIMIT_SECONDS = 2;
const PRIVATE_MEDIA_REMOVED_SYNC_LIMIT = 5000;
const EPORNER_API_BASE = 'https://www.eporner.com/api/v2/video/';

function private_media_default_config(): array
{
    return [
        'enabled' => false,
        'provider_eporner_enabled' => false,
        'show_in_app' => false,
        'native_playback_enabled' => false,
        'per_page' => 24,
        'thumbsize' => 'big',
        'order' => 'latest',
        'sections' => [
            [
                'id' => 'all',
                'title' => 'Nouveautes',
                'query' => 'all',
                'order' => 'latest',
                'enabled' => true,
            ],
            [
                'id' => 'popular',
                'title' => 'Populaires',
                'query' => 'all',
                'order' => 'most-popular',
                'enabled' => true,
            ],
            [
                'id' => 'top-weekly',
                'title' => 'Top semaine',
                'query' => 'all',
                'order' => 'top-weekly',
                'enabled' => true,
            ],
            [
                'id' => 'top-rated',
                'title' => 'Mieux notees',
                'query' => 'all',
                'order' => 'top-rated',
                'enabled' => true,
            ],
            [
                'id' => 'long-format',
                'title' => 'Long format',
                'query' => 'all',
                'order' => 'longest',
                'enabled' => true,
            ],
            [
                'id' => 'amateur',
                'title' => 'Amateur',
                'query' => 'amateur',
                'order' => 'top-monthly',
                'enabled' => true,
            ],
            [
                'id' => 'couples',
                'title' => 'Couples',
                'query' => 'couple',
                'order' => 'top-monthly',
                'enabled' => true,
            ],
            [
                'id' => 'pov',
                'title' => 'POV',
                'query' => 'pov',
                'order' => 'top-monthly',
                'enabled' => true,
            ],
        ],
    ];
}

function private_media_normalize_config(array $config): array
{
    $defaults = private_media_default_config();
    $perPage = (int) ($config['per_page'] ?? $defaults['per_page']);
    $thumbsize = (string) ($config['thumbsize'] ?? $defaults['thumbsize']);
    $order = (string) ($config['order'] ?? $defaults['order']);
    $sections = [];
    $rawSections = $config['sections'] ?? $defaults['sections'];
    if (private_media_is_legacy_single_section($rawSections)) {
        $rawSections = $defaults['sections'];
    }
    $usedIds = [];

    foreach ($rawSections as $section) {
        if (!is_array($section)) {
            continue;
        }
        $id = private_media_slug((string) ($section['id'] ?? $section['title'] ?? 'section'));
        $title = smartvision_text_substr(trim((string) ($section['title'] ?? 'Section')), 0, 80);
        $query = smartvision_text_substr(trim((string) ($section['query'] ?? 'all')), 0, 120);
        if ($id === '' || $title === '' || $query === '') {
            continue;
        }
        $baseId = $id;
        $suffix = 2;
        while (isset($usedIds[$id])) {
            $id = $baseId . '-' . $suffix;
            $suffix++;
        }
        $usedIds[$id] = true;
        $sections[] = [
            'id' => $id,
            'title' => $title,
            'query' => $query,
            'order' => private_media_allowed_order((string) ($section['order'] ?? $order)),
            'enabled' => (bool) ($section['enabled'] ?? true),
        ];
    }

    if ($sections === []) {
        $sections = $defaults['sections'];
    }

    return [
        'enabled' => (bool) ($config['enabled'] ?? $defaults['enabled']),
        'provider_eporner_enabled' => (bool) ($config['provider_eporner_enabled'] ?? $defaults['provider_eporner_enabled']),
        'show_in_app' => (bool) ($config['show_in_app'] ?? $defaults['show_in_app']),
        'native_playback_enabled' => (bool) ($config['native_playback_enabled'] ?? $defaults['native_playback_enabled']),
        'per_page' => max(1, min(50, $perPage)),
        'thumbsize' => in_array($thumbsize, ['small', 'medium', 'big'], true) ? $thumbsize : $defaults['thumbsize'],
        'order' => private_media_allowed_order($order),
        'sections' => $sections,
    ];
}

function private_media_is_legacy_single_section(mixed $sections): bool
{
    if (!is_array($sections) || count($sections) !== 1) {
        return false;
    }
    $section = reset($sections);
    if (!is_array($section)) {
        return false;
    }
    $title = strtolower(trim((string) ($section['title'] ?? '')));
    $query = strtolower(trim((string) ($section['query'] ?? '')));
    return $query === 'all' && str_contains($title, 'tous les medias');
}

function private_media_config(PDO $pdo): array
{
    $json = (string) get_setting($pdo, PRIVATE_MEDIA_CONFIG_KEY, '');
    $decoded = json_decode($json, true);
    return private_media_normalize_config(is_array($decoded) ? $decoded : []);
}

function private_media_save_config(PDO $pdo, array $config): void
{
    $normalized = private_media_normalize_config($config);
    $statement = $pdo->prepare(
        "INSERT INTO app_settings (setting_key, setting_value)
         VALUES (:setting_key, :setting_value)
         ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)"
    );
    $statement->execute([
        'setting_key' => PRIVATE_MEDIA_CONFIG_KEY,
        'setting_value' => json_encode($normalized, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
    ]);
}

function private_media_ensure_schema(PDO $pdo): void
{
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS private_media_provider_cache (
            cache_key VARCHAR(180) PRIMARY KEY,
            payload MEDIUMTEXT NOT NULL,
            fetched_at DATETIME NOT NULL,
            expires_at DATETIME NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS private_media_removed_ids (
            provider VARCHAR(32) NOT NULL,
            provider_video_id VARCHAR(80) NOT NULL,
            removed_at DATETIME NOT NULL,
            PRIMARY KEY (provider, provider_video_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS private_media_provider_health (
            provider VARCHAR(32) PRIMARY KEY,
            status VARCHAR(32) NOT NULL,
            latency_ms INT NULL,
            last_checked_at DATETIME NOT NULL,
            last_error VARCHAR(255) NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
}

function private_media_libraries(PDO $pdo): array
{
    $config = private_media_config($pdo);
    return [[
        'id' => 'private_media',
        'title' => 'Media prives',
        'type' => 'remote',
        'provider' => PRIVATE_MEDIA_PROVIDER,
        'isEnabled' => private_media_provider_enabled($config),
    ]];
}

function private_media_categories(PDO $pdo): array
{
    $config = private_media_config($pdo);
    if (!private_media_provider_enabled($config)) {
        return [];
    }

    return array_values(array_map(static fn(array $section): array => [
        'id' => $section['id'],
        'title' => $section['title'],
        'parentLibraryId' => 'private_media',
        'query' => $section['query'],
        'order' => $section['order'],
        'provider' => PRIVATE_MEDIA_PROVIDER,
    ], array_filter($config['sections'], static fn(array $section): bool => !empty($section['enabled']))));
}

function private_media_items(PDO $pdo, string $categoryId, int $page, int $perPage, string $searchQuery = ''): array
{
    private_media_ensure_schema($pdo);
    $config = private_media_config($pdo);
    if (!private_media_provider_enabled($config)) {
        return private_media_empty_page($page, $perPage, 'provider_disabled');
    }

    $section = private_media_find_section($config, $categoryId);
    if ($section === null) {
        return private_media_empty_page($page, $perPage, 'category_not_found');
    }

    $safePage = max(1, min(1000000, $page));
    $safePerPage = max(1, min(50, $perPage));
    $query = trim($searchQuery) !== ''
        ? smartvision_text_substr(trim($searchQuery), 0, 120)
        : (string) $section['query'];
    $cacheKey = 'search:' . sha1(json_encode([$section['id'], $query, $section['order'], $safePage, $safePerPage, $config['thumbsize']], JSON_UNESCAPED_SLASHES));
    $payload = private_media_cache_get($pdo, $cacheKey);
    if ($payload === null) {
        $payload = eporner_search($query, $safePage, $safePerPage, $config['thumbsize'], $section['order']);
        private_media_cache_set($pdo, $cacheKey, $payload, PRIVATE_MEDIA_CACHE_TTL_SECONDS);
    }

    $items = [];
    foreach (($payload['videos'] ?? []) as $video) {
        if (!is_array($video)) {
            continue;
        }
        $item = eporner_normalize_item($video, false);
        if (!private_media_is_removed($pdo, $item['providerVideoId'])) {
            $items[] = $item;
        }
    }

    return [
        'count' => count($items),
        'page' => (int) ($payload['page'] ?? $safePage),
        'perPage' => (int) ($payload['per_page'] ?? $safePerPage),
        'totalCount' => (int) ($payload['total_count'] ?? count($items)),
        'totalPages' => (int) ($payload['total_pages'] ?? 1),
        'categoryId' => $section['id'],
        'query' => $query,
        'items' => $items,
        'error' => null,
    ];
}

function private_media_item(PDO $pdo, string $id): array
{
    private_media_ensure_schema($pdo);
    $config = private_media_config($pdo);
    if (!private_media_provider_enabled($config)) {
        return ['success' => false, 'error' => 'provider_disabled', 'item' => null];
    }

    $providerVideoId = private_media_provider_id_from_internal_id($id);
    if ($providerVideoId === '' || private_media_is_removed($pdo, $providerVideoId)) {
        return ['success' => false, 'error' => 'item_removed', 'item' => null];
    }

    $cacheKey = 'id:' . sha1($providerVideoId);
    $payload = private_media_cache_get($pdo, $cacheKey);
    if ($payload === null) {
        $payload = eporner_video_id($providerVideoId, $config['thumbsize']);
        private_media_cache_set($pdo, $cacheKey, $payload, PRIVATE_MEDIA_DETAILS_TTL_SECONDS);
    }

    $video = private_media_extract_video($payload);
    if ($video === null) {
        private_media_mark_removed($pdo, $providerVideoId);
        return ['success' => false, 'error' => 'item_removed', 'item' => null];
    }

    return ['success' => true, 'error' => null, 'item' => eporner_normalize_details($video, $config)];
}

function private_media_playback(PDO $pdo, string $id): array
{
    $details = private_media_item($pdo, $id);
    if (empty($details['success']) || !is_array($details['item'])) {
        return [
            'success' => false,
            'error' => $details['error'] ?? 'playback_unavailable',
            'playbackType' => 'UNAVAILABLE',
            'streams' => [],
        ];
    }

    $item = $details['item'];
    $playbackType = (string) ($item['playbackType'] ?? 'UNAVAILABLE');
    $embedUrl = (string) ($item['embedUrl'] ?? '');
    $pageUrl = (string) ($item['sourceUrl'] ?? '');
    $streams = is_array($item['streams'] ?? null) ? $item['streams'] : [];
    $hasNativeStream = $streams !== [] && in_array($playbackType, ['HLS', 'MP4'], true);
    return [
        'success' => $hasNativeStream || $playbackType === 'EMBED' || $playbackType === 'PAGE_ONLY',
        'error' => $hasNativeStream || $playbackType === 'EMBED' ? null : 'native_stream_unavailable',
        'id' => (string) ($item['id'] ?? $id),
        'title' => (string) ($item['title'] ?? ''),
        'thumbnailUrl' => $item['thumbnailUrl'] ?? null,
        'playbackType' => $playbackType,
        'embedUrl' => $embedUrl !== '' ? $embedUrl : null,
        'pageUrl' => $pageUrl !== '' ? $pageUrl : null,
        'streams' => $streams,
    ];
}

function private_media_health(PDO $pdo): array
{
    private_media_ensure_schema($pdo);
    $config = private_media_config($pdo);
    if (!private_media_provider_enabled($config)) {
        return [
            'provider' => PRIVATE_MEDIA_PROVIDER,
            'enabled' => false,
            'status' => 'disabled',
            'latencyMs' => null,
            'lastError' => null,
        ];
    }

    $start = microtime(true);
    try {
        eporner_search('all', 1, 1, 'small', 'latest');
        $latency = (int) round((microtime(true) - $start) * 1000);
        private_media_save_health($pdo, 'available', $latency, null);
        return [
            'provider' => PRIVATE_MEDIA_PROVIDER,
            'enabled' => true,
            'status' => 'available',
            'latencyMs' => $latency,
            'lastError' => null,
        ];
    } catch (Throwable $exception) {
        $latency = (int) round((microtime(true) - $start) * 1000);
        private_media_save_health($pdo, 'unavailable', $latency, smartvision_text_substr($exception->getMessage(), 0, 255));
        return [
            'provider' => PRIVATE_MEDIA_PROVIDER,
            'enabled' => true,
            'status' => 'unavailable',
            'latencyMs' => $latency,
            'lastError' => $exception->getMessage(),
        ];
    }
}

function private_media_sync_removed(PDO $pdo): int
{
    @set_time_limit(45);
    private_media_ensure_schema($pdo);
    $ids = eporner_removed_ids(PRIVATE_MEDIA_REMOVED_SYNC_LIMIT);
    $statement = $pdo->prepare(
        "INSERT INTO private_media_removed_ids (provider, provider_video_id, removed_at)
         VALUES (:provider, :provider_video_id, UTC_TIMESTAMP())
         ON DUPLICATE KEY UPDATE removed_at = VALUES(removed_at)"
    );
    $count = 0;
    $pdo->beginTransaction();
    try {
        foreach ($ids as $id) {
            if ($id === '') {
                continue;
            }
            if ($count >= PRIVATE_MEDIA_REMOVED_SYNC_LIMIT) {
                break;
            }
            $statement->execute(['provider' => PRIVATE_MEDIA_PROVIDER, 'provider_video_id' => $id]);
            $count++;
        }
        $pdo->commit();
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $exception;
    }
    return $count;
}

function private_media_clear_cache(PDO $pdo): void
{
    private_media_ensure_schema($pdo);
    $pdo->exec("DELETE FROM private_media_provider_cache");
}

function private_media_provider_enabled(array $config): bool
{
    return !empty($config['enabled']) && !empty($config['provider_eporner_enabled']) && !empty($config['show_in_app']);
}

function private_media_find_section(array $config, string $categoryId): ?array
{
    foreach ($config['sections'] as $section) {
        if (!empty($section['enabled']) && (string) $section['id'] === $categoryId) {
            return $section;
        }
    }
    return null;
}

function private_media_empty_page(int $page, int $perPage, string $error): array
{
    return [
        'count' => 0,
        'page' => max(1, $page),
        'perPage' => max(1, $perPage),
        'totalCount' => 0,
        'totalPages' => 0,
        'items' => [],
        'error' => $error,
    ];
}

function eporner_search(string $query, int $page, int $perPage, string $thumbsize, string $order): array
{
    return eporner_request_json('search/', [
        'query' => $query,
        'page' => (string) $page,
        'per_page' => (string) $perPage,
        'thumbsize' => $thumbsize,
        'order' => $order,
        'format' => 'json',
    ]);
}

function eporner_video_id(string $providerVideoId, string $thumbsize): array
{
    return eporner_request_json('id/', [
        'id' => $providerVideoId,
        'thumbsize' => $thumbsize,
        'format' => 'json',
    ]);
}

function eporner_removed_txt(): string
{
    return eporner_request_raw('removed/', ['format' => 'txt'], 25);
}

function eporner_removed_ids(int $limit): array
{
    private_media_rate_limit(PRIVATE_MEDIA_PROVIDER);
    $url = EPORNER_API_BASE . 'removed/?' . http_build_query(['format' => 'txt']);
    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 25,
            'ignore_errors' => true,
            'header' => "User-Agent: SmartVisionPrivateMedia/1.0\r\nAccept: text/plain,*/*;q=0.8\r\n",
        ],
    ]);
    $handle = @fopen($url, 'rb', false, $context);
    if ($handle === false) {
        throw new RuntimeException('Provider removed indisponible.');
    }

    $ids = [];
    try {
        while (!feof($handle) && count($ids) < $limit) {
            $line = fgets($handle, 256);
            if ($line === false) {
                break;
            }
            $id = smartvision_text_substr(trim($line), 0, 80);
            if ($id !== '') {
                $ids[$id] = $id;
            }
        }
    } finally {
        fclose($handle);
    }
    return array_values($ids);
}

function eporner_request_json(string $method, array $query): array
{
    $raw = eporner_request_raw($method, $query);
    $decoded = json_decode($raw, true);
    if (!is_array($decoded) || json_last_error() !== JSON_ERROR_NONE) {
        throw new RuntimeException('Provider JSON invalide.');
    }
    return $decoded;
}

function eporner_request_raw(string $method, array $query, int $timeoutSeconds = 8): string
{
    private_media_rate_limit(PRIVATE_MEDIA_PROVIDER);
    $url = EPORNER_API_BASE . ltrim($method, '/') . '?' . http_build_query($query);
    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => max(3, min(30, $timeoutSeconds)),
            'ignore_errors' => true,
            'header' => "User-Agent: SmartVisionPrivateMedia/1.0\r\nAccept: application/json,text/plain;q=0.9,*/*;q=0.8\r\n",
        ],
    ]);
    $raw = @file_get_contents($url, false, $context);
    if ($raw === false) {
        throw new RuntimeException('Provider indisponible.');
    }
    return $raw;
}

function eporner_normalize_item(array $video, bool $details): array
{
    $providerVideoId = trim((string) ($video['id'] ?? ''));
    $thumb = private_media_thumb_src($video['default_thumb'] ?? null);
    $keywords = private_media_keywords((string) ($video['keywords'] ?? ''));
    return [
        'id' => private_media_internal_id($providerVideoId),
        'provider' => PRIVATE_MEDIA_PROVIDER,
        'providerVideoId' => $providerVideoId,
        'title' => trim((string) ($video['title'] ?? 'Untitled')),
        'description' => null,
        'keywords' => $keywords,
        'tags' => $keywords,
        'thumbnailUrl' => $thumb,
        'thumbnails' => private_media_thumbs($video['thumbs'] ?? []),
        'durationSeconds' => isset($video['length_sec']) ? (int) $video['length_sec'] : null,
        'durationLabel' => (string) ($video['length_min'] ?? ''),
        'views' => isset($video['views']) ? (int) $video['views'] : null,
        'rating' => isset($video['rate']) ? (float) $video['rate'] : null,
        'addedAt' => (string) ($video['added'] ?? ''),
        'sourceUrl' => $details ? (string) ($video['url'] ?? '') : null,
        'embedUrl' => $details ? (string) ($video['embed'] ?? '') : (string) ($video['embed'] ?? ''),
        'isPlayable' => false,
        'playbackType' => 'EMBED',
    ];
}

function private_media_extract_native_streams(array $video): array
{
    $streams = [];
    $candidateKeys = ['hls', 'hls_url', 'mp4', 'mp4_url', 'stream', 'stream_url', 'video_url', 'download_url'];
    foreach ($candidateKeys as $key) {
        if (isset($video[$key])) {
            private_media_collect_native_stream($streams, $video[$key], $key);
        }
    }
    foreach (['streams', 'sources', 'videos', 'files'] as $key) {
        if (isset($video[$key]) && is_array($video[$key])) {
            private_media_collect_native_stream($streams, $video[$key], $key);
        }
    }
    return array_values($streams);
}

function private_media_collect_native_stream(array &$streams, mixed $candidate, string $label = 'auto'): void
{
    if (is_string($candidate)) {
        $stream = private_media_native_stream_from_url($candidate, $label);
        if ($stream !== null) {
            $streams[$stream['url']] = $stream;
        }
        return;
    }
    if (!is_array($candidate)) {
        return;
    }
    $url = null;
    foreach (['url', 'src', 'file', 'link'] as $urlKey) {
        if (isset($candidate[$urlKey]) && is_string($candidate[$urlKey])) {
            $url = $candidate[$urlKey];
            break;
        }
    }
    if ($url !== null) {
        $quality = (string) ($candidate['quality'] ?? $candidate['label'] ?? $candidate['name'] ?? $label);
        $stream = private_media_native_stream_from_url($url, $quality);
        if ($stream !== null) {
            $streams[$stream['url']] = $stream;
        }
    }
    foreach ($candidate as $key => $value) {
        if (is_array($value)) {
            private_media_collect_native_stream($streams, $value, is_string($key) ? $key : $label);
        }
    }
}

function private_media_native_stream_from_url(string $url, string $quality): ?array
{
    $url = trim($url);
    if (!preg_match('#^https?://#i', $url)) {
        return null;
    }
    $path = strtolower((string) (parse_url($url, PHP_URL_PATH) ?? ''));
    $type = '';
    if (str_ends_with($path, '.m3u8')) {
        $type = 'HLS';
    } elseif (str_ends_with($path, '.mp4')) {
        $type = 'MP4';
    }
    if ($type === '') {
        return null;
    }
    return [
        'type' => $type,
        'url' => $url,
        'quality' => smartvision_text_substr(trim($quality) !== '' ? trim($quality) : 'auto', 0, 40),
    ];
}

function eporner_normalize_details(array $video, array $config): array
{
    $item = eporner_normalize_item($video, true);
    $streams = !empty($config['native_playback_enabled']) ? private_media_extract_native_streams($video) : [];
    $item['streams'] = $streams;
    if ($streams !== []) {
        $item['isPlayable'] = true;
        $item['playbackType'] = $streams[0]['type'];
    } else {
        $item['isPlayable'] = !empty($item['embedUrl']);
        $item['playbackType'] = !empty($item['embedUrl']) ? 'EMBED' : 'PAGE_ONLY';
    }
    return $item;
}

function private_media_extract_video(array $payload): ?array
{
    if (isset($payload['id'])) {
        return $payload;
    }
    if (isset($payload['video']) && is_array($payload['video'])) {
        return $payload['video'];
    }
    if (isset($payload['videos'][0]) && is_array($payload['videos'][0])) {
        return $payload['videos'][0];
    }
    return null;
}

function private_media_cache_get(PDO $pdo, string $cacheKey): ?array
{
    $statement = $pdo->prepare("SELECT payload FROM private_media_provider_cache WHERE cache_key = :cache_key AND expires_at > UTC_TIMESTAMP()");
    $statement->execute(['cache_key' => $cacheKey]);
    $payload = $statement->fetchColumn();
    if (!is_string($payload) || $payload === '') {
        return null;
    }
    $decoded = json_decode($payload, true);
    return is_array($decoded) ? $decoded : null;
}

function private_media_cache_set(PDO $pdo, string $cacheKey, array $payload, int $ttlSeconds): void
{
    $safeTtl = max(60, min(86400, $ttlSeconds));
    $statement = $pdo->prepare(
        "INSERT INTO private_media_provider_cache (cache_key, payload, fetched_at, expires_at)
         VALUES (:cache_key, :payload, UTC_TIMESTAMP(), DATE_ADD(UTC_TIMESTAMP(), INTERVAL {$safeTtl} SECOND))
         ON DUPLICATE KEY UPDATE payload = VALUES(payload), fetched_at = VALUES(fetched_at), expires_at = VALUES(expires_at)"
    );
    $statement->bindValue(':cache_key', $cacheKey);
    $statement->bindValue(':payload', json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES));
    $statement->execute();
}

function private_media_is_removed(PDO $pdo, string $providerVideoId): bool
{
    if ($providerVideoId === '') {
        return true;
    }
    $statement = $pdo->prepare("SELECT 1 FROM private_media_removed_ids WHERE provider = :provider AND provider_video_id = :id LIMIT 1");
    $statement->execute(['provider' => PRIVATE_MEDIA_PROVIDER, 'id' => $providerVideoId]);
    return (bool) $statement->fetchColumn();
}

function private_media_mark_removed(PDO $pdo, string $providerVideoId): void
{
    if ($providerVideoId === '') {
        return;
    }
    $statement = $pdo->prepare(
        "INSERT INTO private_media_removed_ids (provider, provider_video_id, removed_at)
         VALUES (:provider, :id, UTC_TIMESTAMP())
         ON DUPLICATE KEY UPDATE removed_at = VALUES(removed_at)"
    );
    $statement->execute(['provider' => PRIVATE_MEDIA_PROVIDER, 'id' => $providerVideoId]);
}

function private_media_save_health(PDO $pdo, string $status, ?int $latencyMs, ?string $error): void
{
    $statement = $pdo->prepare(
        "INSERT INTO private_media_provider_health (provider, status, latency_ms, last_checked_at, last_error)
         VALUES (:provider, :status, :latency_ms, UTC_TIMESTAMP(), :last_error)
         ON DUPLICATE KEY UPDATE status = VALUES(status), latency_ms = VALUES(latency_ms), last_checked_at = VALUES(last_checked_at), last_error = VALUES(last_error)"
    );
    $statement->execute([
        'provider' => PRIVATE_MEDIA_PROVIDER,
        'status' => $status,
        'latency_ms' => $latencyMs,
        'last_error' => $error,
    ]);
}

function private_media_rate_limit(string $provider): void
{
    $file = sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'smartvision_private_media_' . preg_replace('/[^a-z0-9_\\-]/i', '_', $provider) . '.lock';
    $last = is_file($file) ? (float) @file_get_contents($file) : 0.0;
    $elapsed = microtime(true) - $last;
    if ($elapsed > 0 && $elapsed < PRIVATE_MEDIA_RATE_LIMIT_SECONDS) {
        usleep((int) ((PRIVATE_MEDIA_RATE_LIMIT_SECONDS - $elapsed) * 1000000));
    }
    @file_put_contents($file, (string) microtime(true), LOCK_EX);
}

function private_media_internal_id(string $providerVideoId): string
{
    return PRIVATE_MEDIA_PROVIDER . ':' . $providerVideoId;
}

function private_media_provider_id_from_internal_id(string $id): string
{
    return str_starts_with($id, PRIVATE_MEDIA_PROVIDER . ':') ? substr($id, strlen(PRIVATE_MEDIA_PROVIDER) + 1) : $id;
}

function private_media_slug(string $value): string
{
    $slug = strtolower(trim(preg_replace('/[^a-zA-Z0-9]+/', '-', $value) ?? ''));
    return trim($slug, '-');
}

function private_media_allowed_order(string $order): string
{
    return in_array($order, ['latest', 'longest', 'shortest', 'top-rated', 'most-popular', 'top-weekly', 'top-monthly'], true)
        ? $order
        : 'latest';
}

function private_media_keywords(string $keywords): array
{
    return array_values(array_filter(array_map(static fn(string $item): string => trim($item), explode(',', $keywords))));
}

function private_media_thumb_src(mixed $thumb): ?string
{
    return is_array($thumb) && isset($thumb['src']) ? (string) $thumb['src'] : null;
}

function private_media_thumbs(mixed $thumbs): array
{
    if (!is_array($thumbs)) {
        return [];
    }
    $result = [];
    foreach ($thumbs as $thumb) {
        if (!is_array($thumb)) {
            continue;
        }
        $src = private_media_thumb_src($thumb);
        if ($src !== null && $src !== '') {
            $result[] = [
                'url' => $src,
                'width' => isset($thumb['width']) ? (int) $thumb['width'] : null,
                'height' => isset($thumb['height']) ? (int) $thumb['height'] : null,
                'size' => (string) ($thumb['size'] ?? ''),
            ];
        }
    }
    return $result;
}
