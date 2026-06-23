<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();
header('Cache-Control: public, max-age=120');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

try {
    $statement = db()->query(
        "SELECT title, subtitle, button_label, button_route, image_url
         FROM home_slider_ads
         WHERE status = 'active'
           AND (starts_at IS NULL OR starts_at <= NOW())
           AND (ends_at IS NULL OR ends_at >= NOW())
         ORDER BY sort_order ASC, id ASC
         LIMIT 12"
    );
    $slides = array_map(static fn(array $row): array => [
        'title' => (string) ($row['title'] ?? ''),
        'subtitle' => (string) ($row['subtitle'] ?? ''),
        'button_label' => (string) ($row['button_label'] ?? 'En savoir plus'),
        'button_route' => (string) ($row['button_route'] ?? 'home'),
        'image_url' => (string) ($row['image_url'] ?? ''),
    ], $statement->fetchAll());

    json_response([
        'success' => true,
        'slides' => $slides,
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision home_slides failed.');
    json_response(['success' => false, 'error' => 'Slides indisponibles.'], 500);
}
