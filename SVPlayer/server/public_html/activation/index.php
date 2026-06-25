<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';

$code = normalize_activation_code($_GET['code'] ?? null);
$deviceId = clean_device_id($_GET['device_id'] ?? null);

$target = '/activate/';
$params = [];
if ($code !== '') {
    $params['code'] = $code;
}
if ($deviceId !== '') {
    $params['device_id'] = $deviceId;
}
if ($params !== []) {
    $target .= '?' . http_build_query($params);
}

header('Location: ' . $target, true, 302);
exit;
