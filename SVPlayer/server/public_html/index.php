<?php
declare(strict_types=1);

require_once __DIR__ . '/_includes/site_layout.php';

sv_send_site_headers();
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="SmartVision est un lecteur IPTV légal pour Android TV. Téléchargez l’application, activez votre TV et ajoutez uniquement vos propres sources autorisées.">
    <title>SmartVision IPTV Player</title>
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3376574358352765" crossorigin="anonymous"></script>
    <link rel="stylesheet" href="/assets/site.css?v=3">
    <link rel="stylesheet" href="/assets/mobile.css?v=3">
</head>
<body>
<?php sv_render_site_header(); ?>

<main id="top">
    <section class="hero band home-hero">
        <div class="hero-copy">
            <h1>SmartVision<br><span>IPTV Player</span></h1>
            <p class="hero-lead">Un lecteur IPTV Android TV propre, rapide et lisible à distance. Installez l’application, activez votre appareil et ajoutez uniquement vos propres identifiants ou playlists autorisés.</p>
            <div class="hero-actions">
                <a class="button button-primary" href="/download.php">Télécharger l’application</a>
                <a class="button button-outline" href="/activate/">Activer ma TV</a>
                <a class="button button-outline" href="/account/">Mon compte</a>
            </div>
            <ul class="hero-facts" aria-label="Points clés">
                <li>Android TV</li>
                <li>Lecture native</li>
                <li>Activation sécurisée</li>
                <li>Sources utilisateur uniquement</li>
            </ul>
        </div>
        <div class="product-stage" aria-label="Aperçu de l’application SmartVision">
            <div class="tv-frame">
                <img src="/assets/images/app-live-tv.png" alt="Interface TV SmartVision">
            </div>
            <div class="stage-line"></div>
        </div>
    </section>

    <section class="feature-strip" id="fonctionnalites">
        <article><strong>Interface TV</strong><span>Navigation au D-pad, focus visible et écrans conçus pour le canapé.</span></article>
        <article><strong>Lecteur natif</strong><span>Lecture Android TV avec Media3 ExoPlayer, sans WebView ni lecteur externe.</span></article>
        <article><strong>Organisation claire</strong><span>Live TV, films, séries, favoris, historique et reprise de lecture.</span></article>
        <article><strong>Activation simple</strong><span>Associez votre TV depuis un téléphone puis configurez vos sources autorisées.</span></article>
    </section>

    <section class="activation-flow band" id="activation">
        <div class="section-heading compact">
            <h2>Prêt en quelques minutes</h2>
            <p>SmartVision sépare clairement le logiciel de lecture et les sources ajoutées par l’utilisateur.</p>
        </div>
        <ol class="steps">
            <li><span>01</span><div><strong>Installez SmartVision</strong><p>Téléchargez l’APK officiel sur votre appareil Android TV.</p></div></li>
            <li><span>02</span><div><strong>Associez votre TV</strong><p>Utilisez le code affiché à l’écran pour activer l’appareil depuis le site.</p></div></li>
            <li><span>03</span><div><strong>Ajoutez vos sources</strong><p>Renseignez uniquement des identifiants, portails ou playlists pour lesquels vous disposez d’une autorisation légale.</p></div></li>
        </ol>
        <div class="activation-actions">
            <a class="button button-primary" href="/activate/">Ouvrir l’activation</a>
            <a class="button button-outline" href="/download.php">Télécharger l’APK</a>
        </div>
    </section>

    <section class="showcase band">
        <div class="showcase-image"><img src="/assets/images/app-movies.png" alt="Catalogue organisé dans SmartVision"></div>
        <div class="showcase-copy">
            <h2>Une expérience stable sur grand écran.</h2>
            <p>SmartVision privilégie la lisibilité, la rapidité et la compatibilité Android TV : menus sobres, navigation prévisible, lecteurs natifs et fonctions utiles pour retrouver rapidement vos contenus autorisés.</p>
            <a class="text-link" href="/legal-iptv-player/">Comprendre le positionnement légal</a>
        </div>
    </section>

    <section class="legal-home band" id="legal">
        <div>
            <h2>Un lecteur, pas un fournisseur de contenus.</h2>
            <p>SmartVision ne contient, ne vend et ne fournit aucune chaîne, aucun film, aucune série, aucune playlist et aucun abonnement IPTV. L’application sert uniquement à lire et organiser les sources que vous ajoutez vous-même, lorsque vous disposez des droits nécessaires.</p>
        </div>
        <ul>
            <li>Aucun flux préchargé</li>
            <li>Aucun catalogue pirate</li>
            <li>Aucune promesse d’accès à des contenus payants</li>
            <li>Support limité au logiciel SmartVision</li>
        </ul>
    </section>

    <section class="reseller band">
        <div>
            <h2>Besoin d’aide ?</h2>
            <p>Le support SmartVision accompagne l’installation, l’activation et le fonctionnement technique de l’application. Il ne fournit pas de fournisseurs, playlists ou contenus tiers.</p>
        </div>
        <a class="button button-outline" href="/contact/">Contacter le support</a>
    </section>
</main>

<?php sv_render_site_footer(); ?>
</body>
</html>
