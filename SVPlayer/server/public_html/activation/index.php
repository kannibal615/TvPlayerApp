<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';

$publicDeviceCode = clean_public_device_code($_GET['device'] ?? null);
$target = '/account/?source=tv&intent=license';
if ($publicDeviceCode !== '') {
    $target .= '&device=' . rawurlencode($publicDeviceCode);
}

header('Location: ' . $target, true, 302);
exit;
