<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/config.php';
require_once dirname(__DIR__, 2) . '/helpers.php';
require_once __DIR__ . '/private_media_service.php';

apply_api_headers();

try {
    $pdo = db();
    $id = smartvision_text_substr(trim((string) ($_GET['id'] ?? '')), 0, 120);
    if ($id === '') {
        json_response(['success' => false, 'error' => 'missing_id', 'playbackType' => 'UNAVAILABLE', 'streams' => []], 400);
    }
    json_response(private_media_playback($pdo, $id));
} catch (Throwable $exception) {
    json_response(['success' => false, 'error' => 'private_media_playback_failed', 'playbackType' => 'UNAVAILABLE', 'streams' => []], 502);
}
