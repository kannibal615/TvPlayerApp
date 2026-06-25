<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/ads_service.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

try {
    $stored = ads_store_event(db(), read_json_input());
    json_response([
        'success' => true,
        'event_id' => $stored['id'],
        'event_type' => $stored['eventType'],
    ]);
} catch (InvalidArgumentException $exception) {
    json_response([
        'success' => false,
        'error' => $exception->getMessage(),
    ], 400);
} catch (RuntimeException $exception) {
    json_response([
        'success' => false,
        'error' => 'Evenement pub refuse temporairement.',
    ], 429);
} catch (Throwable $exception) {
    error_log('SmartVision ads-events failed.');
    json_response([
        'success' => false,
        'error' => 'Evenement pub non enregistre.',
    ], 500);
}
