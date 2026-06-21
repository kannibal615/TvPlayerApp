<?php
declare(strict_types=1);

header('Content-Type: text/html; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
header("Referrer-Policy: strict-origin-when-cross-origin");
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; frame-ancestors 'self'; base-uri 'self'; form-action 'self' https://smartvisions.net");
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="SmartVision est un lecteur IPTV premium pour Android TV. Activez votre appareil, ajoutez vos identifiants Xtream et profitez de votre propre abonnement.">
    <title>SmartVision IPTV Player</title>
    <link rel="stylesheet" href="/assets/site.css?v=1">
</head>
<body>
<header class="site-header">
    <a class="brand" href="#top" aria-label="SmartVision, accueil">
        <img class="brand-mark" src="/assets/images/smartvision-mark.png" alt="">
        <img class="brand-wordmark" src="/assets/images/smartvision-wordmark.png" alt="SmartVision IPTV Player">
    </a>
    <nav aria-label="Navigation principale">
        <a href="#fonctionnalites">Fonctionnalités</a>
        <a href="#tarifs">Tarifs</a>
        <a href="#activation">Activation</a>
        <a href="#revendeurs">Revendeurs</a>
    </nav>
    <a class="button button-outline header-cta" href="/activate/">Activer ma TV</a>
</header>

<main id="top">
    <section class="hero band">
        <div class="hero-copy">
            <p class="eyebrow">Lecteur IPTV natif pour Android TV</p>
            <h1>SmartVision<br><span>IPTV Player</span></h1>
            <p class="hero-lead">Votre lecteur IPTV, simplement. Une interface pensée pour la télévision, rapide à utiliser et lisible à distance.</p>
            <div class="hero-actions">
                <a class="button button-primary" href="/downloads/smartvision-tv.apk">Télécharger l’application</a>
                <a class="button button-outline" href="/activate/">Activer ma TV</a>
            </div>
            <ul class="hero-facts" aria-label="Points clés">
                <li>Android TV</li>
                <li>Lecture native</li>
                <li>Essai 7 jours</li>
                <li>Mises à jour incluses</li>
            </ul>
        </div>
        <div class="product-stage" aria-label="Aperçu de l’application SmartVision">
            <div class="tv-frame">
                <img src="/assets/images/app-live-tv.png" alt="Écran Live TV de SmartVision">
            </div>
            <div class="stage-line"></div>
        </div>
    </section>

    <section class="feature-strip" id="fonctionnalites">
        <article><strong>Navigation TV</strong><span>Focus D-pad clair et commandes adaptées à la télécommande.</span></article>
        <article><strong>Lecture native</strong><span>Media3 ExoPlayer, sans WebView et sans lecteur externe.</span></article>
        <article><strong>Votre contenu</strong><span>Ajoutez votre propre abonnement Xtream après l’activation.</span></article>
        <article><strong>Bibliothèque personnelle</strong><span>Favoris, historique et reprise de lecture sur l’appareil.</span></article>
    </section>

    <section class="pricing band" id="tarifs">
        <div class="section-heading">
            <p class="eyebrow">Une licence par appareil</p>
            <h2>Choisissez votre activation</h2>
            <p>La licence SmartVision donne accès à l’application. Elle n’inclut aucune chaîne, aucun film et aucun abonnement IPTV.</p>
        </div>
        <div class="price-grid">
            <article class="price-card">
                <p class="plan-name">1 mois</p>
                <p class="price"><strong>2</strong><span>€</span></p>
                <p>Pour découvrir SmartVision sans engagement.</p>
                <a class="button button-outline" href="https://smartvisions.net/?smartvision_plan=1-month">Choisir 1 mois</a>
            </article>
            <article class="price-card featured">
                <span class="popular">Le plus choisi</span>
                <p class="plan-name">12 mois</p>
                <p class="price"><strong>15</strong><span>€</span></p>
                <p>Le meilleur équilibre pour un usage quotidien.</p>
                <a class="button button-primary" href="https://smartvisions.net/?smartvision_plan=12-months">Choisir 12 mois</a>
            </article>
            <article class="price-card">
                <p class="plan-name">À vie</p>
                <p class="price"><strong>20</strong><span>€</span></p>
                <p>Une activation définitive pour cet appareil.</p>
                <a class="button button-outline" href="https://smartvisions.net/?smartvision_plan=lifetime">Choisir à vie</a>
            </article>
        </div>
        <div class="trial-callout">
            <div><strong>Pas encore décidé ?</strong><span>Activez un essai gratuit unique de 7 jours depuis la page affichée par votre TV.</span></div>
            <a class="text-link" href="/activate/">Commencer l’activation</a>
        </div>
    </section>

    <section class="activation-flow band" id="activation">
        <div class="section-heading compact">
            <p class="eyebrow">Prêt en quelques minutes</p>
            <h2>Activation en 3 étapes</h2>
        </div>
        <ol class="steps">
            <li><span>01</span><div><strong>Installez l’application</strong><p>Téléchargez l’APK SmartVision sur votre appareil Android TV.</p></div></li>
            <li><span>02</span><div><strong>Associez votre TV</strong><p>Saisissez le code affiché sur la TV, puis votre code SmartVision ou démarrez l’essai.</p></div></li>
            <li><span>03</span><div><strong>Ajoutez votre accès Xtream</strong><p>Configurez-le sur le site ou plus tard, directement avec la télécommande.</p></div></li>
        </ol>
        <div class="activation-actions">
            <a class="button button-primary" href="/activate/">Ouvrir la page d’activation</a>
            <a class="button button-outline" href="https://smartvisions.net">Obtenir un abonnement IPTV</a>
        </div>
        <p class="legal-note">SmartVision est uniquement un lecteur multimédia. Nous ne fournissons, n’hébergeons et ne distribuons aucun contenu.</p>
    </section>

    <section class="showcase band">
        <div class="showcase-image"><img src="/assets/images/app-movies.png" alt="Catalogue de films dans SmartVision"></div>
        <div class="showcase-copy">
            <p class="eyebrow">Pensé pour le grand écran</p>
            <h2>Une interface nette, même à trois mètres.</h2>
            <p>Live TV, films et séries restent rapides à parcourir grâce à une grille stable, des focus visibles et des lecteurs vidéo natifs.</p>
            <a class="text-link" href="/downloads/smartvision-tv.apk">Télécharger SmartVision pour Android TV</a>
        </div>
    </section>

    <section class="reseller band" id="revendeurs">
        <div>
            <p class="eyebrow">Professionnels</p>
            <h2>Espace revendeurs</h2>
            <p>La gestion des lots de licences et des comptes revendeurs sera disponible dans une prochaine version.</p>
        </div>
        <span class="soon">Bientôt disponible</span>
    </section>
</main>

<footer>
    <a class="brand footer-brand" href="#top">
        <img class="brand-mark" src="/assets/images/smartvision-mark.png" alt="">
        <img class="brand-wordmark" src="/assets/images/smartvision-wordmark.png" alt="SmartVision IPTV Player">
    </a>
    <p>© <?= date('Y') ?> SmartVision. Lecteur IPTV pour Android TV.</p>
    <div><a href="/activate/">Activation</a><a href="https://smartvisions.net">Support</a><a href="/admin/">Administration</a></div>
</footer>
</body>
</html>
