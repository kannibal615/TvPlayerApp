<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/device_diagnostics_service.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

try {
    device_diagnostics_upsert(db(), read_json_input());
    json_response(['success' => true]);
} catch (InvalidArgumentException $exception) {
    json_response([
        'success' => false,
        'error' => $exception->getMessage(),
    ], 400);
} catch (Throwable $exception) {
    error_log('SmartVision device diagnostics failed.');
    json_response([
        'success' => false,
        'error' => 'Diagnostic non enregistre.',
    ], 500);
}
