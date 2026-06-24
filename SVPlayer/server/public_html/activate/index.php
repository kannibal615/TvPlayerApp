<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/_includes/site_layout.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';

sv_send_site_headers("'none'");

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
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activation SmartVision</title>
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3376574358352765" crossorigin="anonymous"></script>
    <link rel="stylesheet" href="/assets/site.css?v=4">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=4">
</head>
<body class="activation-page">
<?php sv_render_site_header(); ?>
<main class="activation-shell">
    <section class="activation-card">
        <?php if (!$hasSession): ?>
            <h1>Saisissez le code de votre TV</h1>
            <p class="lead">Ouvrez SmartVision sur votre téléviseur et recopiez ici le code affiché dans l’application.</p>
            <form method="get" action="/activate/" class="activation-code-form">
                <div class="field">
                    <label for="tv-code">Code TV</label>
                    <input id="tv-code" name="code" type="text" minlength="6" maxlength="20" autocomplete="one-time-code" placeholder="A7K9PQ" required autofocus>
                </div>
                <button class="button button-primary" type="submit">Continuer</button>
                <?php if ($sessionError !== ''): ?><p class="message" role="alert"><?= sv_h($sessionError) ?></p><?php endif; ?>
            </form>
            <p class="manual-help">Vous n’avez pas encore l’application ? <a class="text-link" href="/download.php">Téléchargez SmartVision pour Android TV.</a></p>
            <p class="manual-help">Vous avez besoin d’une licence ? <a class="text-link" href="/account/">Acheter ou retrouver un code SmartVision.</a></p>
        <?php else: ?>
            <div id="activation-step"<?= $sessionStatus === 'validated' ? ' hidden' : '' ?>>
                <h1>Activez votre TV</h1>
                <p class="lead">Utilisez votre code SmartVision ou démarrez l’essai gratuit unique de 7 jours.</p>
                <div class="session-code"><span>Code affiché sur la TV</span><strong><?= sv_h($shortCode) ?></strong></div>
                <form id="license-form" novalidate>
                    <input type="hidden" id="device-id" value="<?= sv_h($deviceId) ?>">
                    <input type="hidden" id="short-code" value="<?= sv_h($shortCode) ?>">
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
                <h1>Configurer votre playlist</h1>
                <p class="lead">Saisissez vos identifiants ou votre portail autorisé. Les données sont chiffrées puis transmises uniquement à cet appareil.</p>
                <p class="xtream-note">SmartVision ne fournit aucune source. Vous pouvez ignorer cette étape et la faire plus tard depuis l’application.</p>
                <form id="playlist-form" novalidate>
                    <div class="field"><label for="xtream-host">Adresse du serveur</label><input id="xtream-host" type="url" placeholder="https://serveur.example" autocomplete="url"></div>
                    <div class="field"><label for="xtream-username">Nom d’utilisateur</label><input id="xtream-username" type="text" autocomplete="username"></div>
                    <div class="field"><label for="xtream-password">Mot de passe</label><input id="xtream-password" type="text" autocomplete="off"></div>
                    <div class="form-actions">
                        <button class="button button-primary" id="playlist-button" type="submit">Configurer ma TV</button>
                        <button class="button button-outline" id="skip-button" type="button">Configurer plus tard</button>
                    </div>
                    <p class="message" id="playlist-message" role="alert" aria-live="polite"></p>
                </form>
            </div>

            <div class="success-panel" id="success-step" hidden>
                <h2>Votre TV est prête</h2>
                <p>Revenez à SmartVision sur votre téléviseur. L’application va récupérer le statut automatiquement.</p>
            </div>
        <?php endif; ?>
    </section>
</main>
<?php sv_render_site_footer(); ?>
<?php if ($hasSession): ?><script src="/assets/activation.js?v=3" defer></script><?php endif; ?>
</body>
</html>
