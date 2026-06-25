<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/helpers.php';
require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/ads_service.php';

apply_api_headers();
header('Cache-Control: no-store, max-age=0');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

try {
    $config = ads_public_config(db());
    $config['success'] = true;
    json_response($config);
} catch (Throwable $exception) {
    error_log('SmartVision ads-config failed.');
    json_response([
        'success' => false,
        'error' => 'Configuration publicitaire indisponible.',
    ], 500);
}
