<?php
declare(strict_types=1);

const SMARTVISION_FREE_ADS_DURATION_SPEC = '+1 day';

function smartvision_expiration_status(string $licenseStatus, string $trialStatus): string
{
    return ($licenseStatus === 'expired' || $trialStatus === 'expired') ? 'expired' : 'pending';
}

function smartvision_free_ads_action(?string $activeType, bool $hasExpiredAccess): string
{
    if (is_string($activeType) && $activeType !== '') {
        return 'keep_active';
    }

    return $hasExpiredAccess ? 'enable' : 'forbidden';
}

function smartvision_free_ads_expires_at(?DateTimeImmutable $now = null): string
{
    $base = $now ?? new DateTimeImmutable('now', new DateTimeZone('UTC'));

    return $base
        ->setTimezone(new DateTimeZone('UTC'))
        ->modify(SMARTVISION_FREE_ADS_DURATION_SPEC)
        ->format('Y-m-d H:i:s');
}
