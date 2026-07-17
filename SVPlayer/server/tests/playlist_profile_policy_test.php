<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/public_html/api/helpers.php';
require_once dirname(__DIR__) . '/public_html/api/playlist_profile_policy.php';

function expect_same(mixed $expected, mixed $actual, string $label): void
{
    if ($expected !== $actual) {
        fwrite(STDERR, "FAILED: {$label}\n");
        exit(1);
    }
}

expect_same(['admin', 'normal'], playlist_clean_target_ids(['admin', 'normal', 'admin', '***']), 'target ids are sanitized and deduplicated');
expect_same(['kids', 'missing'], playlist_invalid_target_ids(['admin', 'kids', 'missing'], ['admin' => 'Admin']), 'unknown and kids ids are rejected');
expect_same(true, playlist_profile_name_collision('playlistweb', ['Admin', 'PlaylistWeb']), 'name collision is case insensitive');
expect_same(false, playlist_profile_name_collision('Salon', ['Admin', 'PlaylistWeb']), 'unique name is accepted');
expect_same(true, playlist_can_create_profile_for_type('xtream'), 'Xtream can create');
expect_same(true, playlist_can_create_profile_for_type('m3u'), 'M3U can create');
expect_same(false, playlist_can_create_profile_for_type('epg'), 'EPG cannot create');

echo "playlist_profile_policy_test: OK\n";
