<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data: https:; frame-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");

function xtream_h(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

$publicDeviceCode = clean_public_device_code($_GET['device'] ?? $_POST['device'] ?? null);
$shortCode = normalize_activation_code($_GET['session'] ?? $_POST['session'] ?? null);
$message = '';
$success = false;

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    $host = normalize_xtream_host($_POST['host'] ?? null);
    $username = clean_optional_text($_POST['username'] ?? null, 180);
    $password = clean_optional_text($_POST['password'] ?? null, 255);

    if ($publicDeviceCode === '' || $shortCode === '' || $host === '' || $username === null || $password === null) {
        $message = 'Informations incompletes ou invalides.';
    } else {
        try {
            $pdo = db();
            $deviceLookup = $pdo->prepare('SELECT device_id, trial_status FROM devices WHERE public_device_code = :public_code LIMIT 1');
            $deviceLookup->execute(['public_code' => $publicDeviceCode]);
            $device = $deviceLookup->fetch();
            $deviceId = clean_device_id(is_array($device) ? ($device['device_id'] ?? null) : null);
            if ($deviceId === '') {
                throw new RuntimeException('Device not found.');
            }

            $sessionQuery = $pdo->prepare(
                "SELECT id FROM activation_sessions
                 WHERE device_id = :device_id
                   AND short_code = :short_code
                   AND status = 'validated'
                   AND expires_at > NOW()
                 ORDER BY id DESC
                 LIMIT 1"
            );
            $sessionQuery->execute(['device_id' => $deviceId, 'short_code' => $shortCode]);
            if ($sessionQuery->fetchColumn() === false) {
                throw new RuntimeException('Session expired.');
            }

            $activationQuery = $pdo->prepare(
                "SELECT id FROM device_activations
                 WHERE device_id = :device_id
                   AND status = 'active'
                   AND expires_at > NOW()
                 LIMIT 1"
            );
            $activationQuery->execute(['device_id' => $deviceId]);
            $hasActivation = $activationQuery->fetchColumn() !== false;
            $hasPendingTrial = is_array($device) && ($device['trial_status'] ?? '') === 'pending_xtream';
            if (!$hasActivation && !$hasPendingTrial) {
                throw new RuntimeException('Device inactive.');
            }

            $encrypted = encrypt_playlist_config([
                'host' => $host,
                'username' => $username,
                'password' => $password,
            ]);
            $upsert = $pdo->prepare(
                "INSERT INTO device_playlist_configs (device_id, encrypted_payload, delivered_at, created_at, updated_at)
                 VALUES (:device_id, :payload, NULL, NOW(), NOW())
                 ON DUPLICATE KEY UPDATE encrypted_payload = VALUES(encrypted_payload), delivered_at = NULL, updated_at = NOW()"
            );
            $upsert->execute(['device_id' => $deviceId, 'payload' => $encrypted]);
            if (!$hasActivation && $hasPendingTrial) {
                create_trial_activation($pdo, $deviceId, $publicDeviceCode);
            }
            $pdo->prepare("UPDATE devices SET xtream_status = 'configured', updated_at = NOW() WHERE device_id = :device_id")
                ->execute(['device_id' => $deviceId]);
            $success = true;
            $message = 'Identifiants enregistres. Revenez sur votre TV.';
        } catch (Throwable $exception) {
            error_log('SmartVision xtream page failed.');
            $message = 'Impossible d enregistrer ces identifiants. Verifiez le QR code et recommencez.';
        }
    }
}
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Configuration Xtream SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=3">
</head>
<body class="activation-page">
<main class="activation-shell">
    <a class="brand activation-brand" href="/">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
    </a>

    <section class="activation-card">
        <p class="eyebrow">Configuration Xtream</p>
        <h1>Ajoutez vos identifiants IPTV</h1>
        <p class="lead">SmartVision ne fournit aucun contenu. Saisissez les identifiants Xtream de votre propre abonnement pour synchroniser votre TV.</p>
        <?php if ($publicDeviceCode !== ''): ?><div class="session-code"><span>Appareil</span><strong><?= xtream_h($publicDeviceCode) ?></strong></div><?php endif; ?>

        <?php if ($success): ?>
            <div class="success-panel"><h2>Configuration envoyee</h2><p><?= xtream_h($message) ?></p></div>
        <?php else: ?>
            <form method="post" action="/xtream/">
                <input type="hidden" name="device" value="<?= xtream_h($publicDeviceCode) ?>">
                <input type="hidden" name="session" value="<?= xtream_h($shortCode) ?>">
                <div class="field"><label for="xtream-host">Host / URL serveur</label><input id="xtream-host" name="host" type="url" placeholder="https://serveur.example" autocomplete="url" required></div>
                <div class="field"><label for="xtream-username">Nom d'utilisateur</label><input id="xtream-username" name="username" type="text" autocomplete="username" required></div>
                <div class="field"><label for="xtream-password">Mot de passe</label><input id="xtream-password" name="password" type="text" autocomplete="off" required></div>
                <button class="button button-primary" type="submit">Synchroniser ma TV</button>
                <?php if ($message !== ''): ?><p class="message" role="alert"><?= xtream_h($message) ?></p><?php endif; ?>
            </form>
        <?php endif; ?>
    </section>
</main>
<footer>
    <a class="brand footer-brand" href="/">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
    </a>
    <p>&copy; <?= date('Y') ?> SmartVision. Lecteur IPTV pour Android TV sans contenu inclus.</p>
    <div>
        <a href="/">Accueil</a>
        <a href="/account/">Mon compte</a>
        <a href="/privacy-policy/">Politique de confidentialité</a>
        <a href="/terms-of-use/">Conditions d’utilisation</a>
        <a href="/contact/">Contact</a>
        <a href="/legal-notice/">Mentions légales</a>
        <a href="/legal-iptv-player/">Lecteur IPTV légal</a>
    </div>
</footer>
</body>
</html>
