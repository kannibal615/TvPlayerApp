<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/config.php';

apply_api_headers();

if (!in_array(($_SERVER['REQUEST_METHOD'] ?? ''), ['GET', 'POST'], true)) {
    json_response([
        'success' => false,
        'error' => 'Methode non autorisee.',
    ], 405);
}

try {
    $pdo = db();
    ensure_app_consent_receipts_table($pdo);
    $input = ($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' ? read_json_input() : [];
    $deviceId = clean_device_id($_GET['device_id'] ?? ($input['device_id'] ?? null));
    $publicDeviceCode = clean_public_device_code($_GET['public_device_code'] ?? ($input['public_device_code'] ?? null));

    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        $action = (string) ($input['action'] ?? '');
        if ($action !== 'accept_consent') {
            json_response(['success' => false, 'error' => 'Action inconnue.'], 400);
        }
        $version = smartvision_text_substr(trim((string) ($input['consent_version'] ?? '')), 0, 40);
        if ($version === '' || ($deviceId === '' && $publicDeviceCode === '')) {
            json_response(['success' => false, 'error' => 'Consentement ou appareil manquant.'], 400);
        }
        app_config_store_consent_receipt(
            $pdo,
            $deviceId,
            $publicDeviceCode,
            $version,
            smartvision_text_substr(trim((string) ($input['app_version'] ?? '')), 0, 50)
        );
        json_response([
            'success' => true,
            'accepted_consent_version' => $version,
        ]);
    }

    json_response([
        'success' => true,
        'consent' => app_config_consent($pdo),
        'accepted_consent_version' => app_config_accepted_consent_version($pdo, $deviceId, $publicDeviceCode),
        'features' => app_config_features($pdo),
        'trending' => app_config_trending($pdo),
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision app config failed.');
    json_response([
        'success' => false,
        'error' => 'Configuration application indisponible.',
    ], 500);
}

function app_config_accepted_consent_version(PDO $pdo, string $deviceId, string $publicDeviceCode): ?string
{
    if ($deviceId === '' && $publicDeviceCode === '') {
        return null;
    }
    $conditions = [];
    $params = [];
    if ($deviceId !== '') {
        $conditions[] = 'device_id = :device_id';
        $params['device_id'] = $deviceId;
    }
    if ($publicDeviceCode !== '') {
        $conditions[] = 'public_device_code = :public_device_code';
        $params['public_device_code'] = $publicDeviceCode;
    }
    $statement = $pdo->prepare(
        'SELECT consent_version FROM app_consent_receipts WHERE ' . implode(' OR ', $conditions) . ' ORDER BY updated_at DESC LIMIT 1'
    );
    $statement->execute($params);
    $version = $statement->fetchColumn();

    return is_string($version) && $version !== '' ? $version : null;
}

function app_config_store_consent_receipt(PDO $pdo, string $deviceId, string $publicDeviceCode, string $version, string $appVersion): void
{
    $statement = $pdo->prepare(
        "INSERT INTO app_consent_receipts
            (device_id, public_device_code, consent_version, app_version, accepted_at, updated_at)
         VALUES
            (:device_id, :public_device_code, :consent_version, :app_version, NOW(), NOW())
         ON DUPLICATE KEY UPDATE
            device_id = COALESCE(VALUES(device_id), device_id),
            public_device_code = COALESCE(VALUES(public_device_code), public_device_code),
            consent_version = VALUES(consent_version),
            app_version = VALUES(app_version),
            accepted_at = NOW(),
            updated_at = NOW()"
    );
    $statement->execute([
        'device_id' => $deviceId !== '' ? $deviceId : null,
        'public_device_code' => $publicDeviceCode !== '' ? $publicDeviceCode : null,
        'consent_version' => $version,
        'app_version' => $appVersion !== '' ? $appVersion : null,
    ]);
}

function app_config_consent(PDO $pdo): array
{
    $version = (string) get_setting($pdo, 'app_consent_version', '2026-06-28');
    $title = (string) get_setting($pdo, 'app_consent_title', 'Privacy Policy and Terms of Use');
    $body = (string) get_setting($pdo, 'app_consent_body', app_config_default_consent_body());
    $variablesJson = (string) get_setting($pdo, 'app_consent_variables', '{}');
    $variables = json_decode($variablesJson, true);
    if (!is_array($variables)) {
        $variables = [];
    }

    return [
        'version' => $version,
        'title' => $title,
        'body' => $body,
        'variables' => $variables,
    ];
}

function app_config_features(PDO $pdo): array
{
    $json = (string) get_setting($pdo, 'app_feature_access', '');
    $decoded = json_decode($json, true);
    if (!is_array($decoded)) {
        $decoded = app_config_default_features();
    }

    return app_config_normalize_features($decoded);
}

function app_config_trending(PDO $pdo): array
{
    $json = (string) get_setting($pdo, 'app_trending_config', '');
    $decoded = json_decode($json, true);
    if (!is_array($decoded)) {
        $decoded = app_config_default_trending();
    }

    return app_config_normalize_trending($decoded);
}

function app_config_normalize_trending(array $config): array
{
    $defaults = app_config_default_trending();
    $minimumRating = (float) ($config['minimum_rating'] ?? $defaults['minimum_rating']);
    $candidateLimit = (int) ($config['candidate_limit'] ?? $defaults['candidate_limit']);
    $sectionLimit = (int) ($config['section_limit'] ?? $defaults['section_limit']);

    return [
        'require_landscape_image' => (bool) ($config['require_landscape_image'] ?? $defaults['require_landscape_image']),
        'exclude_adult' => (bool) ($config['exclude_adult'] ?? $defaults['exclude_adult']),
        'use_rating_filter' => (bool) ($config['use_rating_filter'] ?? $defaults['use_rating_filter']),
        'minimum_rating' => max(0.0, min(10.0, $minimumRating)),
        'candidate_limit' => max(10, min(100, $candidateLimit)),
        'section_limit' => max(1, min(20, $sectionLimit)),
    ];
}

function app_config_default_trending(): array
{
    return [
        'require_landscape_image' => true,
        'exclude_adult' => true,
        'use_rating_filter' => false,
        'minimum_rating' => 9.0,
        'candidate_limit' => 50,
        'section_limit' => 10,
    ];
}

function app_config_normalize_features(array $features): array
{
    $defaults = [];
    foreach (app_config_default_features() as $feature) {
        $defaults[(string) $feature['key']] = $feature;
    }
    foreach ($features as $feature) {
        if (!is_array($feature)) {
            continue;
        }
        $key = (string) ($feature['key'] ?? '');
        if ($key === '' || !isset($defaults[$key])) {
            continue;
        }
        $defaults[$key] = array_replace($defaults[$key], [
            'premium' => (bool) ($feature['premium'] ?? false),
            'trial' => (bool) ($feature['trial'] ?? false),
            'free_ads' => (bool) ($feature['free_ads'] ?? false),
        ]);
    }

    return array_values($defaults);
}

function app_config_default_features(): array
{
    return [
        [
            'key' => 'youtube',
            'label' => 'YouTube',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'parental_control',
            'label' => 'Controle parental',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'replay',
            'label' => 'Replay',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'advanced_favorites',
            'label' => 'Favoris avances',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'multi_screen',
            'label' => 'Multi-ecran',
            'premium' => true,
            'trial' => false,
            'free_ads' => false,
        ],
        [
            'key' => 'local_cache',
            'label' => 'Telechargement ou cache local',
            'premium' => true,
            'trial' => false,
            'free_ads' => false,
        ],
        [
            'key' => 'recorder',
            'label' => 'Recorder',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'media_center',
            'label' => 'Menu Media Center',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'media_file_management',
            'label' => 'Gestion fichiers Media',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'media_phone_transfer',
            'label' => 'Transfert telephone TV',
            'premium' => true,
            'trial' => true,
            'free_ads' => false,
        ],
        [
            'key' => 'private_media',
            'label' => 'Bibliotheque privee',
            'premium' => true,
            'trial' => false,
            'free_ads' => false,
        ],
        [
            'key' => 'private_media_eporner',
            'label' => 'Provider Eporner',
            'premium' => true,
            'trial' => false,
            'free_ads' => false,
        ],
        [
            'key' => 'private_media_native_playback',
            'label' => 'Lecture native media prives',
            'premium' => true,
            'trial' => false,
            'free_ads' => false,
        ],
    ];
}

function app_config_default_consent_body(): string
{
    return <<<'TEXT'
**SmartVision Player** is a commercial IPTV media player developed and operated by **ONETECCOM**.

SmartVision Player lets users play their own compatible content, streams or playlists.

**SmartVision Player does not provide IPTV content, TV channels, movies, series, IPTV subscriptions or playlists.**

By using SmartVision Player, activating a licence, creating an account or using **smartvisions.net**, you accept these terms.

1. User consent
You acknowledge that SmartVision Player is only a media player, that you are solely responsible for the content, links, playlists and Xtream credentials you add, and that you must use only content for which you have the required rights.

2. Data collected
We may collect technical data required to operate the service, including device identifier, generated device code, device model, operating system version, application version, IP address, licence status, activation dates, trial status, account email when applicable, support information and technical error data.

3. Use of data
Data is used to provide SmartVision Player, activate the application, verify licences, manage trials and accounts, process payments, provide support, improve stability, prevent fraud, display ads in the free mode and comply with legal obligations.

4. Ads and free mode
SmartVision Player may offer an ad-supported free mode. Ads may be shown in the application and may be provided by third-party partners under their own privacy policies.

5. Purchases, licences and activation
A SmartVision Player licence gives access only to the application or to specific playback features. It never gives access to IPTV content, TV channels, movies, series, TV packages, IPTV subscriptions or playlists.

6. User responsibility
You are solely responsible for playlists, links, Xtream credentials, content watched, legality of sources and compliance with copyright and applicable laws.

7. Prohibited use
It is forbidden to use SmartVision Player for illegal content, resell the application without authorization, resell IPTV subscriptions under the SmartVision Player name, bypass activation, use fraudulent payment methods or disrupt associated services.

8. Suspension
ONETECCOM may suspend or remove access in case of violation of these terms, fraud, disputed payment, illegal use, abuse or serious reports involving illegal or unauthorized content.

9. Third-party content and services
SmartVision Player does not control third-party content, servers, streams, playlists or subscriptions. SmartVision Player only provides a playback tool.

10. Updates
SmartVision Player may publish updates to fix bugs, improve security, add features or adapt the application to technical or legal requirements.

11. Intellectual property
SmartVision Player, its name, logo, interface, code, visuals and graphic elements are protected. Unauthorized reproduction, modification, distribution or use is prohibited.

12. Contact
For questions about SmartVision Player, personal data, licences, payments or support, contact **support@smartvisions.net**.
TEXT;
}
