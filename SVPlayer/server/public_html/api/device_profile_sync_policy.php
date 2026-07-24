<?php
declare(strict_types=1);

function device_profile_allowed_types(int $capabilityVersion): array
{
    return $capabilityVersion >= 2 ? ['admin', 'normal', 'kids'] : ['admin', 'normal'];
}

function device_profile_has_xtream_credentials(array $profile): bool
{
    return ($profile['source'] ?? null) === 'xtream'
        && trim((string) ($profile['host'] ?? '')) !== ''
        && trim((string) ($profile['username'] ?? '')) !== ''
        && trim((string) ($profile['password'] ?? '')) !== '';
}

function device_profile_resolve_xtream_status(array $profiles, array $serverConfig): string
{
    foreach ($profiles as $profile) {
        if (is_array($profile) && device_profile_has_xtream_credentials($profile)) {
            return 'configured';
        }
    }

    return device_profile_has_xtream_credentials([
        'source' => 'xtream',
        'host' => $serverConfig['host'] ?? '',
        'username' => $serverConfig['username'] ?? '',
        'password' => $serverConfig['password'] ?? '',
    ]) ? 'configured' : 'missing';
}

function device_has_synced_xtream_profiles(PDO $pdo, string $deviceId): bool
{
    $statement = $pdo->prepare(
        "SELECT 1 FROM device_playlist_profiles
         WHERE device_id = :device_id AND source = 'xtream' AND credentials_payload IS NOT NULL
         LIMIT 1"
    );
    $statement->execute(['device_id' => $deviceId]);
    return $statement->fetchColumn() !== false;
}
