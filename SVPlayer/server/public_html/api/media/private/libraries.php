<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/config.php';
require_once dirname(__DIR__, 2) . '/helpers.php';
require_once __DIR__ . '/private_media_service.php';

apply_api_headers();

try {
    $pdo = db();
    private_media_ensure_schema($pdo);
    json_response(['success' => true, 'libraries' => private_media_libraries($pdo)]);
} catch (Throwable $exception) {
    json_response(['success' => false, 'error' => 'private_media_libraries_failed'], 500);
}
