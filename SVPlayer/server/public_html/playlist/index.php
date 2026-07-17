<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/playlist_profile_policy.php';
require_once dirname(__DIR__) . '/_includes/site_layout.php';

sv_send_site_headers("'self'");
header('Cache-Control: no-store');

$message = '';
$messageType = '';
$postedDevice = '';
$postedHost = '';
$postedUsername = '';
$postedEpg = '';
$postedM3u = '';
$postedConfigType = 'xtream';
$postedTargetIds = [];
$postedCreateNew = false;
$postedNewProfileName = 'PlaylistWeb';

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    $publicDeviceCode = clean_public_device_code($_POST['device'] ?? null);
    $postedConfigType = clean_optional_text($_POST['config_type'] ?? 'xtream', 20) ?? 'xtream';
    if (!in_array($postedConfigType, ['xtream', 'm3u', 'epg'], true)) {
        $postedConfigType = 'xtream';
    }
    $postedTargetIds = playlist_clean_target_ids($_POST['target_profile_ids'] ?? null);
    $postedCreateNew = isset($_POST['create_new_profile']);
    $postedNewProfileName = clean_optional_text($_POST['new_profile_name'] ?? 'PlaylistWeb', 60) ?? '';
    $hostInput = trim((string) ($_POST['host'] ?? ''));
    $usernameInput = trim((string) ($_POST['username'] ?? ''));
    $passwordInput = trim((string) ($_POST['password'] ?? ''));
    $clearEpg = $postedConfigType === 'epg' && isset($_POST['clear_epg']);
    $hasXtreamInput = $postedConfigType === 'xtream';
    $host = $hasXtreamInput ? normalize_xtream_host($hostInput) : null;
    $username = $hasXtreamInput ? clean_optional_text($usernameInput, 180) : null;
    $password = $hasXtreamInput ? clean_optional_text($passwordInput, 255) : null;
    $epgUrl = $postedConfigType === 'epg' && !$clearEpg ? normalize_epg_url($_POST['epg_url'] ?? null) : null;
    $m3uUrl = $postedConfigType === 'm3u' ? normalize_playlist_url($_POST['m3u_url'] ?? null) : null;

    $postedDevice = $publicDeviceCode;
    $postedHost = $hostInput;
    $postedUsername = $usernameInput;
    $postedEpg = trim((string) ($_POST['epg_url'] ?? ''));
    $postedM3u = trim((string) ($_POST['m3u_url'] ?? ''));

    if (strlen($publicDeviceCode) !== 6) {
        $message = 'Code TV invalide.';
        $messageType = 'error';
    } elseif ($hasXtreamInput && ($host === null || $host === '' || $username === null || $password === null)) {
        $message = 'Renseignez host, utilisateur et mot de passe Xtream.';
        $messageType = 'error';
    } elseif ($epgUrl === '') {
        $message = 'URL EPG invalide.';
        $messageType = 'error';
    } elseif ($m3uUrl === '') {
        $message = 'URL M3U invalide.';
        $messageType = 'error';
    } elseif ($postedConfigType === 'epg' && $epgUrl === null && !$clearEpg) {
        $message = 'Renseignez une URL EPG.';
        $messageType = 'error';
    } elseif ($postedConfigType === 'm3u' && $m3uUrl === null) {
        $message = 'Renseignez un lien M3U.';
        $messageType = 'error';
    } elseif (!$postedCreateNew && $postedTargetIds === []) {
        $message = 'Selectionnez au moins un profil destinataire.';
        $messageType = 'error';
    } elseif ($postedCreateNew && !playlist_can_create_profile_for_type($postedConfigType)) {
        $message = 'Un nouveau profil doit d abord recevoir une configuration Xtream ou M3U.';
        $messageType = 'error';
    } elseif ($postedCreateNew && $postedNewProfileName === '') {
        $message = 'Renseignez le nom du nouveau profil.';
        $messageType = 'error';
    } else {
        try {
            $pdo = db();
            $deviceQuery = $pdo->prepare(
                "SELECT d.device_id, d.trial_status, COALESCE(r.capability_version, 0) AS capability_version,
                    EXISTS (
                        SELECT 1 FROM device_activations
                        WHERE device_id = d.device_id AND status = 'active' AND expires_at > NOW()
                        LIMIT 1
                    ) AS has_activation
                 FROM devices d
                 LEFT JOIN device_profile_registry r ON r.device_id = d.device_id
                 WHERE d.public_device_code = :public_code AND d.status <> 'blocked'
                 LIMIT 1"
            );
            $deviceQuery->execute(['public_code' => $publicDeviceCode]);
            $device = $deviceQuery->fetch();
            $deviceId = clean_device_id(is_array($device) ? ($device['device_id'] ?? null) : null);
            if ($deviceId === '') {
                throw new RuntimeException('Device not found.');
            }
            $hasActivation = (int) ($device['has_activation'] ?? 0) === 1;
            $hasPendingTrial = ($device['trial_status'] ?? '') === 'pending_xtream';
            if (!$hasActivation && !$hasPendingTrial) {
                $message = 'Cette TV doit etre activee avant de recevoir une playlist.';
                $messageType = 'error';
            } elseif ((int) ($device['capability_version'] ?? 0) < 1) {
                $message = 'Ouvrez ou mettez a jour SmartVision sur la TV pour charger les profils.';
                $messageType = 'error';
            } else {
                $eligibleProfilesQuery = $pdo->prepare(
                    "SELECT profile_id, profile_name FROM device_playlist_profiles
                     WHERE device_id = :device_id AND profile_type IN ('admin', 'normal')"
                );
                $eligibleProfilesQuery->execute(['device_id' => $deviceId]);
                $eligibleProfiles = $eligibleProfilesQuery->fetchAll();
                $eligibleById = [];
                foreach ($eligibleProfiles as $eligibleProfile) {
                    $eligibleById[(string) $eligibleProfile['profile_id']] = (string) $eligibleProfile['profile_name'];
                }
                $invalidTarget = playlist_invalid_target_ids($postedTargetIds, $eligibleById);
                $nameCollision = $postedCreateNew && playlist_profile_name_collision($postedNewProfileName, array_values($eligibleById));
                if ($invalidTarget !== []) {
                    $message = 'Un profil selectionne n est plus disponible. Rechargez la liste.';
                    $messageType = 'error';
                } elseif ($nameCollision) {
                    $message = 'Un profil porte deja ce nom.';
                    $messageType = 'error';
                } else {
                    $existingQuery = $pdo->prepare('SELECT encrypted_payload FROM device_playlist_configs WHERE device_id = :device_id LIMIT 1');
                    $existingQuery->execute(['device_id' => $deviceId]);
                    $existingPayload = $existingQuery->fetchColumn();
                    $config = is_string($existingPayload) && $existingPayload !== ''
                        ? (decrypt_playlist_config($existingPayload) ?? [])
                        : [];

                    if ($hasXtreamInput) {
                        $config['host'] = $host;
                        $config['username'] = $username;
                        $config['password'] = $password;
                        $config['source'] = 'xtream';
                    }
                    if ($clearEpg) {
                        unset($config['epg_url']);
                    } elseif ($epgUrl !== null) {
                        $config['epg_url'] = $epgUrl;
                    }
                    if ($m3uUrl !== null) {
                        $config['m3u_url'] = $m3uUrl;
                        $config['source'] = 'm3u';
                    }
                    $config['provided_fields'] = [$postedConfigType];
                    $config['config_id'] = generate_uuid_v4();
                    $config['target_profile_ids'] = $postedTargetIds;
                    $config['new_profile_name'] = $postedCreateNew ? $postedNewProfileName : null;

                    $hasXtreamConfig = trim((string) ($config['host'] ?? '')) !== ''
                        && trim((string) ($config['username'] ?? '')) !== ''
                        && trim((string) ($config['password'] ?? '')) !== '';
                    $upsert = $pdo->prepare(
                        "INSERT INTO device_playlist_configs (device_id, encrypted_payload, delivered_at, created_at, updated_at)
                         VALUES (:device_id, :payload, NULL, NOW(), NOW())
                         ON DUPLICATE KEY UPDATE encrypted_payload = VALUES(encrypted_payload), delivered_at = NULL, updated_at = NOW()"
                    );
                    $upsert->execute(['device_id' => $deviceId, 'payload' => encrypt_playlist_config($config)]);

                    if ($hasXtreamConfig && !$hasActivation && $hasPendingTrial) {
                        create_trial_activation($pdo, $deviceId, $publicDeviceCode);
                    }
                    mark_latest_pending_device_session_validated($pdo, $deviceId);
                    $pdo->prepare("UPDATE devices SET xtream_status = :xtream_status, updated_at = NOW() WHERE device_id = :device_id")
                        ->execute(['xtream_status' => $hasXtreamConfig ? 'configured' : 'missing', 'device_id' => $deviceId]);
                    create_playlist_push_notification(
                        $pdo,
                        $deviceId,
                        $publicDeviceCode,
                        $hasXtreamInput,
                        $m3uUrl !== null,
                        $postedConfigType === 'epg',
                        'playlist_page',
                        [
                            'xtream_host' => $hasXtreamInput ? $host : null,
                            'xtream_username' => $hasXtreamInput ? $username : null,
                            'xtream_password' => $hasXtreamInput ? $password : null,
                            'm3u_url' => $m3uUrl,
                            'epg_url' => $postedConfigType === 'epg' ? $epgUrl : null,
                        ]
                    );

                    $message = 'Configuration mise en attente. La TV la recevra normalement sous une minute lorsqu elle est ouverte.';
                    $messageType = 'success';
                    $postedHost = '';
                    $postedUsername = '';
                    $postedEpg = '';
                    $postedM3u = '';
                    $postedTargetIds = [];
                    $postedCreateNew = false;
                    $postedNewProfileName = 'PlaylistWeb';
                }
            }
        } catch (Throwable $exception) {
            error_log('SmartVision playlist page failed.');
            $message = 'Impossible d envoyer cette configuration. Verifiez le code TV et recommencez.';
            $messageType = 'error';
        }
    }
}
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Playlist TV | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=7">
</head>
<body class="activation-page">
<?php sv_render_site_header(); ?>
<main class="activation-shell playlist-shell">
    <section class="activation-card playlist-card">
        <p class="eyebrow">Playlist</p>
        <h1>Envoyer une configuration a la TV</h1>
        <p class="lead">Saisissez le code TV, choisissez un ou plusieurs profils, puis envoyez votre configuration chiffree.</p>

        <form id="playlist-form" method="post" action="/playlist/"
              data-selected-targets="<?= sv_h(json_encode($postedTargetIds, JSON_UNESCAPED_SLASHES) ?: '[]') ?>"
              data-create-new="<?= $postedCreateNew ? '1' : '0' ?>">
            <input id="playlist-config-type" type="hidden" name="config_type" value="<?= sv_h($postedConfigType) ?>">

            <section class="playlist-target-card" aria-labelledby="playlist-target-title">
                <div class="field playlist-code-field">
                    <div class="field-label-row">
                        <label id="playlist-target-title" for="playlist-device">Code TV</label>
                        <span id="playlist-code-status" class="field-status" role="status" aria-live="polite"></span>
                    </div>
                    <input id="playlist-device" name="device" type="text" inputmode="text" maxlength="6"
                           value="<?= sv_h($postedDevice) ?>" placeholder="ABC123" autocomplete="off" required>
                </div>
                <div id="playlist-targets" class="playlist-targets" hidden>
                    <div class="playlist-target-heading">
                        <div><strong>Profils destinataires</strong><span>Vous pouvez choisir plusieurs profils.</span></div>
                    </div>
                    <div id="playlist-profile-options" class="playlist-profile-options"></div>
                    <label id="playlist-new-profile-option" class="playlist-profile-option playlist-new-profile-option">
                        <input id="playlist-create-new" type="checkbox" name="create_new_profile" value="1">
                        <span class="playlist-profile-avatar">+</span>
                        <span><strong>Nouveau profil</strong><small>Créer un profil Normal autonome</small></span>
                    </label>
                    <p id="playlist-new-profile-help" class="playlist-inline-help" hidden>Un nouveau profil doit d’abord recevoir une source Xtream ou M3U.</p>
                    <div id="playlist-new-profile-name-wrap" class="field" hidden>
                        <label for="playlist-new-profile-name">Nom du nouveau profil</label>
                        <input id="playlist-new-profile-name" name="new_profile_name" type="text" maxlength="60"
                               value="<?= sv_h($postedNewProfileName) ?>" placeholder="PlaylistWeb">
                    </div>
                    <p id="playlist-target-error" class="playlist-inline-error" role="alert" aria-live="polite"></p>
                </div>
            </section>

            <div class="playlist-tabs">
                <input id="playlist-tab-xtream" name="playlist-tabs" type="radio" value="xtream" data-config-tab <?= $postedConfigType === 'xtream' ? 'checked' : '' ?>>
                <input id="playlist-tab-m3u" name="playlist-tabs" type="radio" value="m3u" data-config-tab <?= $postedConfigType === 'm3u' ? 'checked' : '' ?>>
                <input id="playlist-tab-epg" name="playlist-tabs" type="radio" value="epg" data-config-tab <?= $postedConfigType === 'epg' ? 'checked' : '' ?>>
                <div class="playlist-tab-list" role="tablist" aria-label="Type de configuration">
                    <label for="playlist-tab-xtream">Code Xtream</label>
                    <label for="playlist-tab-m3u">Lien M3U</label>
                    <label for="playlist-tab-epg">Lien EPG</label>
                </div>
                <section class="playlist-tab-panel playlist-panel-xtream" data-config-panel="xtream">
                    <div class="field"><label for="playlist-host">Host / URL serveur Xtream</label><input id="playlist-host" name="host" type="url" value="<?= sv_h($postedHost) ?>" placeholder="https://serveur.example" autocomplete="url"></div>
                    <div class="playlist-field-row">
                        <div class="field"><label for="playlist-username">Nom d'utilisateur Xtream</label><input id="playlist-username" name="username" type="text" value="<?= sv_h($postedUsername) ?>" autocomplete="username"></div>
                        <div class="field"><label for="playlist-password">Mot de passe Xtream</label><input id="playlist-password" name="password" type="password" autocomplete="off"></div>
                    </div>
                </section>
                <section class="playlist-tab-panel playlist-panel-m3u" data-config-panel="m3u">
                    <div class="field"><label for="playlist-m3u">Lien M3U</label><input id="playlist-m3u" name="m3u_url" type="url" value="<?= sv_h($postedM3u) ?>" placeholder="https://serveur.example/playlist.m3u" autocomplete="url"></div>
                </section>
                <section class="playlist-tab-panel playlist-panel-epg" data-config-panel="epg">
                    <div class="field"><label for="playlist-epg">Lien EPG</label><input id="playlist-epg" name="epg_url" type="url" value="<?= sv_h($postedEpg) ?>" placeholder="https://serveur.example/epg.xml" autocomplete="url"></div>
                    <label class="playlist-clear-option"><input type="checkbox" name="clear_epg" value="1"> Supprimer le lien EPG des profils sélectionnés</label>
                </section>
            </div>
            <button id="playlist-submit" class="button button-primary playlist-submit" type="submit" disabled>Envoyer la configuration</button>
        </form>
        <?php if ($message !== ''): ?><p class="message <?= $messageType === 'success' ? 'success-message' : '' ?>" role="alert"><?= sv_h($message) ?></p><?php endif; ?>
    </section>
</main>
<?php sv_render_site_footer(); ?>
<script src="/assets/playlist.js?v=1" defer></script>
</body>
</html>
