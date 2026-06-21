<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

apply_api_headers();

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

$currentVersionCode = max(0, (int) ($_GET['version_code'] ?? 0));
$metadataPath = dirname(__DIR__) . '/downloads/smartvision-tv.version.json';

if (!is_file($metadataPath)) {
    json_response([
        'success' => true,
        'update_available' => false,
        'latest_version_code' => $currentVersionCode,
        'latest_version_name' => (string) ($_GET['version_name'] ?? ''),
    ]);
}

$metadata = json_decode((string) file_get_contents($metadataPath), true);
if (!is_array($metadata) || json_last_error() !== JSON_ERROR_NONE) {
    json_response([
        'success' => false,
        'error' => 'Manifest de mise a jour invalide.',
    ], 500);
}

$latestVersionCode = max(0, (int) ($metadata['version_code'] ?? 0));
$latestVersionName = trim((string) ($metadata['version_name'] ?? $latestVersionCode));
$apkPath = '/downloads/smartvision-tv.apk';
$scheme = 'https';
$host = preg_replace('/[^A-Za-z0-9.-]/', '', (string) ($_SERVER['HTTP_HOST'] ?? 'app.smartvisions.net'));
$apkUrl = $scheme . '://' . $host . $apkPath;

json_response([
    'success' => true,
    'update_available' => $latestVersionCode > $currentVersionCode,
    'latest_version_code' => $latestVersionCode,
    'latest_version_name' => $latestVersionName,
    'apk_url' => $apkUrl,
    'apk_sha256' => (string) ($metadata['apk_sha256'] ?? ''),
    'apk_size' => (int) ($metadata['apk_size'] ?? 0),
    'mandatory' => (bool) ($metadata['mandatory'] ?? false),
    'release_notes' => (string) ($metadata['release_notes'] ?? ''),
]);

