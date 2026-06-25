<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/config.php';
require_once dirname(__DIR__) . '/ads_service.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
    http_response_code(405);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'Methode non autorisee.';
    exit;
}

header('Content-Type: application/xml; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('X-Content-Type-Options: nosniff');

try {
    echo ads_fetch_vast_xml(db());
} catch (Throwable $exception) {
    error_log('SmartVision ads-vast proxy failed.');
    http_response_code(502);
    echo '<?xml version="1.0" encoding="UTF-8"?><VAST version="3.0"></VAST>';
}
