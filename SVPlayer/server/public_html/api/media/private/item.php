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
        json_response(['success' => false, 'error' => 'missing_id'], 400);
    }
    $result = private_media_item($pdo, $id);
    json_response($result, empty($result['success']) ? 404 : 200);
} catch (Throwable $exception) {
    json_response(['success' => false, 'error' => 'private_media_item_failed'], 502);
}
