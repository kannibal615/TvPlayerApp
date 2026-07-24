<?php
declare(strict_types=1);

putenv('SMARTVISION_CREDENTIALS_ENCRYPTION_KEY=' . base64_encode(str_repeat('t', 32)));

require_once dirname(__DIR__) . '/public_html/api/config.php';
require_once dirname(__DIR__) . '/public_html/api/helpers.php';
require_once dirname(__DIR__) . '/public_html/api/device_profile_sync_policy.php';

function expect_device_profile_value(mixed $expected, mixed $actual, string $label): void
{
    if ($expected !== $actual) {
        fwrite(STDERR, "FAILED: {$label}\n");
        exit(1);
    }
}

expect_device_profile_value(
    ['admin', 'normal'],
    device_profile_allowed_types(1),
    'v1 keeps the public targeting inventory without kids'
);
expect_device_profile_value(
    ['admin', 'normal', 'kids'],
    device_profile_allowed_types(2),
    'v2 accepts all local profiles'
);

$localXtream = [[
    'source' => 'xtream',
    'host' => 'https://provider.example',
    'username' => 'demo-user',
    'password' => 'demo-password',
]];
expect_device_profile_value(
    'configured',
    device_profile_resolve_xtream_status($localXtream, []),
    'a local v2 Xtream account configures the device status'
);
expect_device_profile_value(
    'configured',
    device_profile_resolve_xtream_status([], $localXtream[0]),
    'a server-side Xtream account keeps the configured status'
);
expect_device_profile_value(
    'missing',
    device_profile_resolve_xtream_status([['source' => 'm3u']], []),
    'M3U alone does not mark Xtream configured'
);

$encrypted = encrypt_playlist_config($localXtream[0]);
$decrypted = decrypt_playlist_config($encrypted);
expect_device_profile_value($localXtream[0], $decrypted, 'profile credentials are encrypted and recoverable');
expect_device_profile_value(null, decrypt_playlist_config($encrypted . 'tampered'), 'tampered ciphertext is rejected');

echo "device_profile_sync_policy_test: OK\n";
