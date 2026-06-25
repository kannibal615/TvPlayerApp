<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';

header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');

$configuredManifestPath = trim((string) getenv('SMARTVISION_APK_MANIFEST_PATH'));
$manifestPath = $configuredManifestPath !== ''
    ? $configuredManifestPath
    : __DIR__ . '/downloads/smartvision-tv.version.json';
if (!is_file($manifestPath)) {
    http_response_code(503);
    exit('Le telechargement SmartVision est temporairement indisponible.');
}

$rawManifest = file_get_contents($manifestPath);
$rawManifest = preg_replace('/^\xEF\xBB\xBF/', '', (string) $rawManifest);
$manifest = json_decode((string) $rawManifest, true);
$fileName = is_array($manifest) ? basename((string) ($manifest['apk_file'] ?? '')) : '';

if ($fileName === '' || !preg_match('/^smartvision-tv-v\d+-[a-f0-9]{8}\.apk$/', $fileName)) {
    http_response_code(503);
    exit('Le telechargement SmartVision est temporairement indisponible.');
}

$localApkPath = __DIR__ . '/downloads/' . $fileName;
$downloadBaseUrl = trim((string) getenv('SMARTVISION_DOWNLOAD_BASE_URL'));
$downloadUrl = $downloadBaseUrl !== '' && !is_file($localApkPath)
    ? rtrim($downloadBaseUrl, '/') . '/downloads/' . rawurlencode($fileName)
    : '/downloads/' . rawurlencode($fileName);

header('Location: ' . $downloadUrl, true, 302);
exit;
