<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/public_html/api/monetization_rules.php';

function expect_same(string $expected, string $actual, string $label): void
{
    if ($expected !== $actual) {
        fwrite(STDERR, $label . ': attendu ' . $expected . ', obtenu ' . $actual . PHP_EOL);
        exit(1);
    }
}

expect_same('expired', smartvision_expiration_status('inactive', 'expired'), 'Essai expire');
expect_same('expired', smartvision_expiration_status('expired', 'used'), 'Licence expiree');
expect_same('pending', smartvision_expiration_status('inactive', 'available'), 'Nouvel appareil');
expect_same('keep_active', smartvision_free_ads_action('smartvision_code', true), 'Premium non retrograde');
expect_same('keep_active', smartvision_free_ads_action('trial_demo', true), 'Essai non retrograde');
expect_same('keep_active', smartvision_free_ads_action('free_ads', true), 'Activation gratuite idempotente');
expect_same('enable', smartvision_free_ads_action(null, true), 'Gratuit apres expiration');
expect_same('forbidden', smartvision_free_ads_action(null, false), 'Gratuit avant expiration refuse');
expect_same(
    '2026-06-25 12:30:00',
    smartvision_free_ads_expires_at(new DateTimeImmutable('2026-06-24 12:30:00', new DateTimeZone('UTC'))),
    'Gratuit avec pubs expire apres 24h'
);

fwrite(STDOUT, "Backend monetization rules: OK\n");
