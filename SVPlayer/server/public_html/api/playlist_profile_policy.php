<?php
declare(strict_types=1);

function playlist_clean_target_ids(mixed $value): array
{
    if (!is_array($value)) {
        return [];
    }
    return array_values(array_unique(array_filter(array_map(
        static fn(mixed $id): string => clean_device_id(is_string($id) ? $id : null),
        $value
    ))));
}

function playlist_invalid_target_ids(array $requestedIds, array $eligibleById): array
{
    return array_values(array_filter(
        $requestedIds,
        static fn(string $id): bool => !array_key_exists($id, $eligibleById)
    ));
}

function playlist_profile_name_collision(string $requestedName, array $eligibleNames): bool
{
    foreach ($eligibleNames as $name) {
        if (strcasecmp(trim((string) $name), trim($requestedName)) === 0) {
            return true;
        }
    }
    return false;
}

function playlist_can_create_profile_for_type(string $configType): bool
{
    return in_array($configType, ['xtream', 'm3u'], true);
}

