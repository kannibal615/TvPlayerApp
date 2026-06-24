<?php
declare(strict_types=1);

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
