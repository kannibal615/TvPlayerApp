<?php
declare(strict_types=1);

require_once dirname(__DIR__, 3) . '/config.php';
require_once dirname(__DIR__, 3) . '/helpers.php';
require_once dirname(__DIR__) . '/private_media_service.php';

apply_api_headers();

try {
    $pdo = db();
    json_response(['success' => true, 'health' => private_media_health($pdo)]);
} catch (Throwable $exception) {
    json_response(['success' => false, 'error' => 'private_media_health_failed'], 502);
}
