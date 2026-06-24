<?php
declare(strict_types=1);

require_once __DIR__ . '/_includes/site_layout.php';
require_once __DIR__ . '/api/commerce.php';

$plans = commerce_plans();
sv_send_site_headers();
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="SmartVision IPTV Player est un lecteur Android TV. Téléchargement gratuit, licence premium optionnelle, activation simple et aucune chaîne incluse.">
    <title>SmartVision IPTV Player | Lecteur Android TV</title>
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3376574358352765" crossorigin="anonymous"></script>
    <link rel="stylesheet" href="/assets/site.css?v=4">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=4">
    <link rel="stylesheet" href="/assets/mobile.css?v=3">
</head>
<body>
<?php sv_render_site_header(); ?>

<main id="top">
    <section class="hero band home-hero">
        <div class="hero-copy">
            <h1>SmartVision<br><span>IPTV Player</span></h1>
            <p class="hero-lead">Un lecteur Android TV rapide, propre et lisible au canapé. Téléchargez l’application, activez votre appareil et configurez uniquement vos propres sources autorisées.</p>
            <div class="hero-actions">
                <a class="button button-primary" href="/account/?intent=license&plan=year_1">Acheter une licence</a>
                <a class="button button-outline" href="/download.php">Télécharger l’application</a>
            </div>
            <ul class="hero-facts" aria-label="Points clés">
                <li>Application Android TV gratuite</li>
                <li>Premium sans publicité</li>
                <li>Activation sécurisée</li>
                <li>Aucun contenu inclus</li>
            </ul>
        </div>
        <div class="product-stage" aria-label="Aperçu de l’application SmartVision">
            <div class="tv-frame">
                <img src="/assets/images/app-live-tv.png" alt="Interface Android TV SmartVision">
            </div>
            <div class="stage-line"></div>
        </div>
    </section>

    <section class="download-section band" id="telecharger">
        <div>
            <h2>Télécharger l’application</h2>
            <p>SmartVision pour Android TV se télécharge gratuitement. Installez l’APK officiel, ouvrez l’application sur votre TV, puis activez votre appareil depuis le site.</p>
        </div>
        <a class="button button-primary" href="/download.php">Télécharger Android TV</a>
    </section>

    <section class="premium-section band" id="tarifs">
        <div class="section-heading compact">
            <h2>Obtenir SmartVision Premium</h2>
            <p>La version Premium débloque plus de fonctionnalités, supprime la publicité et garde votre expérience TV plus fluide. La licence concerne uniquement le lecteur SmartVision.</p>
        </div>
        <div class="price-grid">
            <?php foreach ($plans as $key => $plan): ?>
                <article class="price-card<?= !empty($plan['recommended']) ? ' featured' : '' ?>">
                    <?php if (!empty($plan['recommended'])): ?><span class="popular">Recommandé</span><?php endif; ?>
                    <p class="plan-name"><?= sv_h((string) $plan['label']) ?></p>
                    <div class="price"><strong><?= sv_h(number_format(((int) $plan['amount_cents']) / 100, 0, ',', ' ')) ?></strong><span>€</span></div>
                    <p><?= sv_h((string) $plan['description']) ?></p>
                    <ul class="plan-features">
                        <li>1 appareil Android TV</li>
                        <li>Sans publicité pendant la durée</li>
                        <li>Mises à jour SmartVision incluses</li>
                    </ul>
                    <a class="button button-primary" href="/account/?plan=<?= sv_h((string) $key) ?>">Acheter cette licence</a>
                </article>
            <?php endforeach; ?>
        </div>
        <p class="legal-note">SmartVision ne vend aucune chaîne, playlist, film, série ni accès à des contenus tiers.</p>
    </section>

    <section class="activation-flow band" id="activation">
        <div class="section-heading compact">
            <h2>Activer mon appareil</h2>
            <p>Un parcours simple depuis le téléphone : saisissez le code affiché sur la TV, activez votre licence puis configurez votre playlist si vous en avez une.</p>
        </div>
        <ol class="steps">
            <li><span>01</span><div><strong>Ouvrez SmartVision</strong><p>L’application affiche un code TV unique.</p></div></li>
            <li><span>02</span><div><strong>Activez la licence</strong><p>Associez votre compte ou votre code SmartVision à cet appareil.</p></div></li>
            <li><span>03</span><div><strong>Configurez votre source</strong><p>Ajoutez uniquement des identifiants ou playlists pour lesquels vous disposez des droits nécessaires.</p></div></li>
        </ol>
        <div class="activation-actions">
            <a class="button button-primary" href="/activate/">Activer appareil</a>
            <a class="button button-outline" href="/activate/">Configurer playlist</a>
        </div>
    </section>

    <section class="showcase band">
        <div class="showcase-image"><img src="/assets/images/app-movies.png" alt="Catalogue utilisateur dans SmartVision"></div>
        <div class="showcase-copy">
            <h2>Conçu pour la télécommande.</h2>
            <p>Grands focus visibles, navigation au D-pad, favoris, historique, reprise de lecture et lecteur natif Android TV. L’interface reste claire à plusieurs mètres de l’écran.</p>
            <a class="text-link" href="/legal-iptv-player/">Lire la page Lecteur IPTV légal</a>
        </div>
    </section>

    <section class="legal-home band">
        <div>
            <h2>Un lecteur légal, pas un fournisseur de contenus.</h2>
            <p>SmartVision est un logiciel de lecture. Vous êtes responsable des sources que vous ajoutez. Le support aide sur l’installation, l’activation et l’usage de l’application, pas sur l’obtention de contenus tiers.</p>
        </div>
        <ul>
            <li>Aucun flux préchargé</li>
            <li>Aucun catalogue vendu</li>
            <li>Aucune promesse d’accès à des contenus payants</li>
            <li>Lecture de vos sources autorisées uniquement</li>
        </ul>
    </section>

    <section class="faq-strip band" aria-label="Questions fréquentes">
        <article><strong>L’application est-elle gratuite ?</strong><span>Oui, le téléchargement est gratuit. Premium est optionnel.</span></article>
        <article><strong>La licence inclut-elle du contenu ?</strong><span>Non. Elle active des fonctions du lecteur SmartVision.</span></article>
        <article><strong>Comment activer ma TV ?</strong><span>Utilisez le code affiché dans l’application sur la page Activation.</span></article>
    </section>

    <section class="help-section band">
        <div>
            <h2>Besoin d’aide ?</h2>
            <p>Contactez le support pour l’installation, l’activation, la licence ou un problème technique lié au lecteur SmartVision.</p>
        </div>
        <a class="button button-outline" href="/contact/">Contact</a>
    </section>
</main>

<?php sv_render_site_footer(); ?>
</body>
</html>
