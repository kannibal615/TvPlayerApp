<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';
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

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    $publicDeviceCode = clean_public_device_code($_POST['device'] ?? null);
    $postedConfigType = clean_optional_text($_POST['config_type'] ?? 'xtream', 20) ?? 'xtream';
    if (!in_array($postedConfigType, ['xtream', 'm3u', 'epg'], true)) {
        $postedConfigType = 'xtream';
    }
    $hostInput = trim((string) ($_POST['host'] ?? ''));
    $usernameInput = trim((string) ($_POST['username'] ?? ''));
    $passwordInput = trim((string) ($_POST['password'] ?? ''));
    $hasXtreamInput = $postedConfigType === 'xtream';
    $host = $hasXtreamInput ? normalize_xtream_host($hostInput) : null;
    $username = $hasXtreamInput ? clean_optional_text($usernameInput, 180) : null;
    $password = $hasXtreamInput ? clean_optional_text($passwordInput, 255) : null;
    $epgUrl = $postedConfigType === 'epg' ? normalize_epg_url($_POST['epg_url'] ?? null) : null;
    $m3uUrl = $postedConfigType === 'm3u' ? normalize_playlist_url($_POST['m3u_url'] ?? null) : null;

    $postedDevice = $publicDeviceCode;
    $postedHost = $hostInput;
    $postedUsername = $usernameInput;
    $postedEpg = trim((string) ($_POST['epg_url'] ?? ''));
    $postedM3u = trim((string) ($_POST['m3u_url'] ?? ''));

    if ($publicDeviceCode === '') {
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
    } elseif ($postedConfigType === 'epg' && $epgUrl === null) {
        $message = 'Renseignez une URL EPG.';
        $messageType = 'error';
    } elseif ($postedConfigType === 'm3u' && $m3uUrl === null) {
        $message = 'Renseignez un lien M3U.';
        $messageType = 'error';
    } else {
        try {
            $pdo = db();
            $deviceQuery = $pdo->prepare(
                "SELECT device_id, trial_status,
                    EXISTS (
                        SELECT 1 FROM device_activations
                        WHERE device_id = devices.device_id AND status = 'active' AND expires_at > NOW()
                        LIMIT 1
                    ) AS has_activation
                 FROM devices
                 WHERE public_device_code = :public_code
                 LIMIT 1"
            );
            $deviceQuery->execute(['public_code' => $publicDeviceCode]);
            $device = $deviceQuery->fetch();
            $deviceId = clean_device_id(is_array($device) ? ($device['device_id'] ?? null) : null);
            if ($deviceId === '') {
                throw new RuntimeException('Device not found.');
            }

            $hasActivation = is_array($device) && (int) ($device['has_activation'] ?? 0) === 1;
            $hasPendingTrial = is_array($device) && ($device['trial_status'] ?? '') === 'pending_xtream';
            if (!$hasActivation && !$hasPendingTrial) {
                $message = 'Cette TV doit etre activee avant de recevoir une playlist.';
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
                }
                if ($epgUrl !== null) {
                    $config['epg_url'] = $epgUrl;
                }
                if ($m3uUrl !== null) {
                    $config['m3u_url'] = $m3uUrl;
                }

                $hasXtreamConfig = trim((string) ($config['host'] ?? '')) !== ''
                    && trim((string) ($config['username'] ?? '')) !== ''
                    && trim((string) ($config['password'] ?? '')) !== '';

                $upsert = $pdo->prepare(
                    "INSERT INTO device_playlist_configs (device_id, encrypted_payload, delivered_at, created_at, updated_at)
                     VALUES (:device_id, :payload, NULL, NOW(), NOW())
                     ON DUPLICATE KEY UPDATE encrypted_payload = VALUES(encrypted_payload), delivered_at = NULL, updated_at = NOW()"
                );
                $upsert->execute([
                    'device_id' => $deviceId,
                    'payload' => encrypt_playlist_config($config),
                ]);

                if ($hasXtreamConfig && !$hasActivation && $hasPendingTrial) {
                    create_trial_activation($pdo, $deviceId, $publicDeviceCode);
                }

                $pdo->prepare("UPDATE devices SET xtream_status = :xtream_status, updated_at = NOW() WHERE device_id = :device_id")
                    ->execute([
                        'xtream_status' => $hasXtreamConfig ? 'configured' : 'missing',
                        'device_id' => $deviceId,
                    ]);

                create_playlist_push_notification(
                    $pdo,
                    $deviceId,
                    $publicDeviceCode,
                    $hasXtreamInput,
                    $m3uUrl !== null,
                    $epgUrl !== null,
                    'playlist_page'
                );

                $message = 'Configuration envoyee. Ouvrez ou actualisez SmartVision sur la TV.';
                $messageType = 'success';
                $postedHost = '';
                $postedUsername = '';
                $postedEpg = '';
                $postedM3u = '';
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
    <link rel="stylesheet" href="/assets/site.css?v=6">
</head>
<body class="activation-page">
<?php sv_render_site_header(); ?>
<main class="activation-shell playlist-shell">
    <section class="activation-card playlist-card">
        <p class="eyebrow">Playlist</p>
        <h1>Envoyer une configuration a la TV</h1>
        <p class="lead">Saisissez le code affiche sur la TV, puis vos identifiants Xtream et/ou une URL EPG. SmartVision chiffre les donnees et les transmet uniquement a l'appareil concerne.</p>

        <div class="playlist-tabs">
            <input id="playlist-tab-xtream" name="playlist-tabs" type="radio" <?= $postedConfigType === 'xtream' ? 'checked' : '' ?>>
            <input id="playlist-tab-m3u" name="playlist-tabs" type="radio" <?= $postedConfigType === 'm3u' ? 'checked' : '' ?>>
            <input id="playlist-tab-epg" name="playlist-tabs" type="radio" <?= $postedConfigType === 'epg' ? 'checked' : '' ?>>
            <div class="playlist-tab-list" role="tablist" aria-label="Type de configuration">
                <label for="playlist-tab-xtream">Code Xtream</label>
                <label for="playlist-tab-m3u">Lien M3U</label>
                <label for="playlist-tab-epg">Lien EPG</label>
            </div>
            <section class="playlist-tab-panel playlist-panel-xtream">
                <form method="post" action="/playlist/">
                    <input type="hidden" name="config_type" value="xtream">
                    <div class="field"><label for="playlist-device-xtream">Code TV</label><input id="playlist-device-xtream" name="device" type="text" inputmode="text" maxlength="6" value="<?= sv_h($postedDevice) ?>" placeholder="ABC123" required></div>
                    <div class="field"><label for="playlist-host">Host / URL serveur Xtream</label><input id="playlist-host" name="host" type="url" value="<?= sv_h($postedHost) ?>" placeholder="https://serveur.example" autocomplete="url" required></div>
                    <div class="field"><label for="playlist-username">Nom d'utilisateur Xtream</label><input id="playlist-username" name="username" type="text" value="<?= sv_h($postedUsername) ?>" autocomplete="username" required></div>
                    <div class="field"><label for="playlist-password">Mot de passe Xtream</label><input id="playlist-password" name="password" type="text" autocomplete="off" required></div>
                    <button class="button button-primary" type="submit">Envoyer a la TV</button>
                </form>
            </section>
            <section class="playlist-tab-panel playlist-panel-m3u">
                <form method="post" action="/playlist/">
                    <input type="hidden" name="config_type" value="m3u">
                    <div class="field"><label for="playlist-device-m3u">Code TV</label><input id="playlist-device-m3u" name="device" type="text" inputmode="text" maxlength="6" value="<?= sv_h($postedDevice) ?>" placeholder="ABC123" required></div>
                    <div class="field"><label for="playlist-m3u">Lien M3U</label><input id="playlist-m3u" name="m3u_url" type="url" value="<?= sv_h($postedM3u) ?>" placeholder="https://serveur.example/playlist.m3u" autocomplete="url" required></div>
                    <button class="button button-primary" type="submit">Envoyer le lien M3U</button>
                </form>
            </section>
            <section class="playlist-tab-panel playlist-panel-epg">
                <form method="post" action="/playlist/">
                    <input type="hidden" name="config_type" value="epg">
                    <div class="field"><label for="playlist-device-epg">Code TV</label><input id="playlist-device-epg" name="device" type="text" inputmode="text" maxlength="6" value="<?= sv_h($postedDevice) ?>" placeholder="ABC123" required></div>
                    <div class="field"><label for="playlist-epg">Lien EPG</label><input id="playlist-epg" name="epg_url" type="url" value="<?= sv_h($postedEpg) ?>" placeholder="https://serveur.example/epg.xml" autocomplete="url" required></div>
                    <button class="button button-primary" type="submit">Envoyer le lien EPG</button>
                </form>
            </section>
        </div>
        <?php if ($message !== ''): ?><p class="message <?= $messageType === 'success' ? 'success-message' : '' ?>" role="alert"><?= sv_h($message) ?></p><?php endif; ?>
    </section>
</main>
<?php sv_render_site_footer(); ?>
</body>
</html>
