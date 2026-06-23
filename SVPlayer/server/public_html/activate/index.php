<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");

$deviceId = clean_device_id($_GET['device_id'] ?? null);
$shortCode = normalize_activation_code($_GET['code'] ?? null);
$sessionStatus = '';
$sessionError = '';

if ($shortCode !== '') {
    try {
        $query = db()->prepare(
            "SELECT device_id, status, expires_at FROM activation_sessions
             WHERE short_code = :short_code ORDER BY id DESC LIMIT 1"
        );
        $query->execute(['short_code' => $shortCode]);
        $session = $query->fetch();
        if (!$session || ($deviceId !== '' && !hash_equals((string) $session['device_id'], $deviceId))) {
            $sessionError = 'Code TV introuvable.';
            $deviceId = '';
        } elseif (($session['status'] ?? '') === 'expired' || strtotime((string) $session['expires_at']) <= time()) {
            $sessionError = 'Ce code TV a expiré. Générez un nouveau code depuis l’application.';
            $deviceId = '';
        } else {
            $deviceId = (string) $session['device_id'];
            $sessionStatus = (string) $session['status'];
        }
    } catch (Throwable $exception) {
        $sessionError = 'Le service d’activation est temporairement indisponible.';
    }
}

$hasSession = $deviceId !== '' && $shortCode !== '';
function h(string $value): string { return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'); }
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activation SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=2">
</head>
<body class="activation-page">
<main class="activation-shell">
    <a class="brand activation-brand" href="/">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png" alt="SmartVision IPTV Player">
    </a>

    <section class="activation-card">
        <?php if (!$hasSession): ?>
            <p class="eyebrow">Associer un appareil</p>
            <h1>Saisissez le code de votre TV</h1>
            <p class="lead">Ouvrez SmartVision sur votre téléviseur et recopiez ici le code à 6 caractères affiché à l’écran.</p>
            <form method="get" action="/activate/">
                <div class="field">
                    <label for="tv-code">Code TV</label>
                    <input id="tv-code" name="code" type="text" minlength="6" maxlength="20" autocomplete="one-time-code" placeholder="A7K9PQ" required autofocus>
                </div>
                <button class="button button-primary" type="submit">Continuer</button>
                <?php if ($sessionError !== ''): ?><p class="message" role="alert"><?= h($sessionError) ?></p><?php endif; ?>
            </form>
            <p class="manual-help">Vous n’avez pas encore l’application ? <a class="text-link" href="/download.php">Téléchargez SmartVision pour Android TV.</a></p>
            <p class="manual-help">Vous avez besoin d’une licence ? <a class="text-link" href="/account/">Acheter ou retrouver un code SmartVision.</a></p>
        <?php else: ?>
            <div id="activation-step"<?= $sessionStatus === 'validated' ? ' hidden' : '' ?>>
                <p class="eyebrow">Activation de l’appareil</p>
                <h1>Activez votre TV</h1>
                <p class="lead">Utilisez votre code SmartVision, ou démarrez l’essai gratuit unique de 7 jours.</p>
                <div class="session-code"><span>Code affiché sur la TV</span><strong><?= h($shortCode) ?></strong></div>
                <form id="license-form" novalidate>
                    <input type="hidden" id="device-id" value="<?= h($deviceId) ?>">
                    <input type="hidden" id="short-code" value="<?= h($shortCode) ?>">
                    <div class="field">
                        <label for="activation-code">Code SmartVision</label>
                        <input id="activation-code" type="text" minlength="10" maxlength="10" autocomplete="one-time-code" placeholder="AB12CD34EF">
                    </div>
                    <button class="button button-primary" id="license-button" type="submit">Activer avec mon code</button>
                </form>
                <div class="divider">ou</div>
                <button class="button button-outline" id="trial-button" type="button">Essayer gratuitement pendant 7 jours</button>
                <p class="message" id="activation-message" role="alert" aria-live="polite"></p>
            </div>

            <div id="playlist-step"<?= $sessionStatus === 'validated' ? '' : ' hidden' ?>>
                <p class="eyebrow">Dernière étape</p>
                <h1>Ajoutez votre abonnement IPTV</h1>
                <p class="lead">Saisissez vos identifiants Xtream. Ils seront chiffrés puis transmis uniquement à cet appareil.</p>
                <p class="xtream-note">Vous n’avez pas d’abonnement ? <a class="text-link" href="https://smartvisions.net">Découvrir les offres sur smartvisions.net</a></p>
                <form id="playlist-form" novalidate>
                    <div class="field"><label for="xtream-host">Adresse du serveur</label><input id="xtream-host" type="url" placeholder="https://serveur.example" autocomplete="url"></div>
                    <div class="field"><label for="xtream-username">Nom d’utilisateur</label><input id="xtream-username" type="text" autocomplete="username"></div>
                    <div class="field"><label for="xtream-password">Mot de passe</label><input id="xtream-password" type="password" autocomplete="current-password"></div>
                    <div class="form-actions">
                        <button class="button button-primary" id="playlist-button" type="submit">Configurer ma TV</button>
                        <button class="button button-outline" id="skip-button" type="button">Configurer plus tard</button>
                    </div>
                    <p class="message" id="playlist-message" role="alert" aria-live="polite"></p>
                </form>
            </div>

            <div class="success-panel" id="success-step" hidden>
                <h2>Votre TV est prête</h2>
                <p>Revenez à SmartVision sur votre téléviseur. L’application va s’ouvrir automatiquement.</p>
            </div>
        <?php endif; ?>
    </section>
</main>
<?php if ($hasSession): ?><script src="/assets/activation.js?v=2" defer></script><?php endif; ?>
</body>
</html>
