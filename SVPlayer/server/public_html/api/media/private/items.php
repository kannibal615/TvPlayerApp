<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/config.php';
require_once dirname(__DIR__, 2) . '/helpers.php';
require_once __DIR__ . '/private_media_service.php';

apply_api_headers();

try {
    $pdo = db();
    $categoryId = smartvision_text_substr(trim((string) ($_GET['category_id'] ?? 'all')), 0, 80);
    $page = filter_var($_GET['page'] ?? 1, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 1000000]]) ?: 1;
    $perPage = filter_var($_GET['per_page'] ?? 24, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 50]]) ?: 24;
    json_response(['success' => true, 'page' => private_media_items($pdo, $categoryId, (int) $page, (int) $perPage)]);
} catch (Throwable $exception) {
    json_response(['success' => false, 'error' => 'private_media_items_failed'], 502);
}
