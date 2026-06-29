<?php
declare(strict_types=1);

require_once __DIR__ . '/_includes/site_layout.php';
require_once __DIR__ . '/api/commerce.php';

$plans = commerce_plans();
$manifest = sv_apk_manifest();
$versionName = trim((string) ($manifest['latest_version_name'] ?? $manifest['version_name'] ?? ''));
$updatedAt = '';
if (isset($manifest['updated_at']) && is_string($manifest['updated_at'])) {
    $updatedAt = $manifest['updated_at'];
}

$planUi = [
    'month_1' => [
        'title' => 'Mensuelle',
        'price' => '2',
        'suffix' => '€ / mois',
        'payment' => 'Abonnement mensuel',
        'description' => 'Idéal pour tester SmartVision sans engagement.',
        'badge' => 'Sans engagement',
        'cta' => 'S’abonner pour 2 €/mois',
    ],
    'year_1' => [
        'title' => '12 mois',
        'price' => '15',
        'suffix' => '€',
        'payment' => 'Paiement unique',
        'description' => 'Licence valable 12 mois pour un appareil.',
        'badge' => 'Le plus choisi',
        'cta' => 'Acheter pour 15 €',
        'featured' => true,
    ],
    'lifetime' => [
        'title' => 'À vie',
        'price' => '20',
        'suffix' => '€',
        'payment' => 'Paiement unique',
        'description' => 'Licence permanente pour un appareil.',
        'badge' => 'Meilleure valeur',
        'cta' => 'Acheter pour 20 €',
    ],
];

sv_send_site_headers();
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="SmartVision IPTV Player est un lecteur Android TV légal, rapide et moderne. Téléchargement gratuit, licence premium optionnelle et aucun contenu inclus.">
    <title>SmartVision IPTV Player | Lecteur Android TV légal</title>
    <link rel="stylesheet" href="/assets/site.css?v=5">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=7">
    <link rel="stylesheet" href="/assets/mobile.css?v=5">
</head>
<body class="home-page">
<?php sv_render_site_header(); ?>

<main id="top">
    <section class="hero band home-hero">
        <div class="hero-copy">
            <h1>SmartVision</h1>
            <h1>IPTV Player</h1>

            <p class="hero-lead">Un lecteur IPTV Android TV légal, rapide et moderne. Ajoutez vos propres identifiants ou playlists autorisées.</p>
            <div class="hero-actions">
                <a class="button button-primary" href="/account/?intent=license">Acheter une licence</a>
                <a class="button button-outline" href="/download.php">Télécharger l’application</a>
            </div>
        </div>
        <div class="product-stage" aria-label="Aperçu de l’application SmartVision">
            <div class="ambient-card ambient-one"></div>
            <div class="tv-frame">
                <img src="/assets/images/app-live-tv.png" alt="Interface Android TV SmartVision">
            </div>
            <div class="device-shadow"></div>
        </div>
    </section>

    <section class="download-section band" id="telecharger">
        
        <div>
            <h2>Télécharger l’application Android TV</h2>
            <p>Installez SmartVision sur votre Android TV, box Android ou appareil compatible. Le téléchargement est gratuit, puis vous activez Premium uniquement si vous souhaitez débloquer les fonctions avancées.</p>
            <dl class="download-meta">
                <div><dt>Prix</dt><dd>Gratuit</dd></div>
                <div><dt>Version</dt><dd><?= $versionName !== '' ? sv_h($versionName) : 'Dernière APK' ?></dd></div>
                <div><dt>Compatibilité</dt><dd>Android TV</dd></div>
            </dl>
        </div>
        <a class="button button-primary" href="/download.php">Télécharger Android TV</a>
    </section>

    <section class="premium-section band" >
        <div class="section-heading compact">
            <h2 class="premium-title">
                <img class="premium-title-image" src="/assets/images/smartvision-premium-title.png?v=1" alt="SmartVision Premium">
            </h2>
            <p>Passez à SmartVision Premium et profitez d’une expérience IPTV plus fluide, plus confortable et sans publicité sur votre Android TV. 
                La version Premium débloque davantage de fonctionnalités du lecteur SmartVision, améliore l’utilisation au quotidien et offre une expérience plus agréable. 
                Vous restez libre d’ajouter vos propres playlists M3U, OTT, identifiants Xtream ou sources autorisées.
            </p>
            <br>
            <strong>Obtenez votre licence Premium dès maintenant</strong>
        </div>
        <div class="price-grid" id="tarifs">
            <?php foreach ($planUi as $key => $ui): ?>
                <?php $isAvailable = isset($plans[$key]); ?>
                <article class="price-card<?= !empty($ui['featured']) ? ' featured' : '' ?>">
                    <?php if (!empty($ui['badge'])): ?><span class="popular"><?= sv_h($ui['badge']) ?></span><?php endif; ?>
                    <p class="plan-name"><?= sv_h($ui['title']) ?></p>
                    <div class="price"><strong><?= sv_h($ui['price']) ?></strong><span><?= sv_h($ui['suffix']) ?></span></div>
                    <p class="payment-type"><?= sv_h($ui['payment']) ?></p>
                    <p class="plan-description"><?= sv_h($ui['description']) ?></p>
                    <ul class="plan-features">
                        <li>1 appareil</li>
                        <li>Mises à jour incluses</li>
                        <li>Activation rapide</li>
                        <li>Vos propres sources autorisées</li>
                        <li>Aucun contenu fourni par SmartVision</li>
                    </ul>
                    <a class="button button-primary" href="/account/?intent=license"><?= sv_h($ui['cta']) ?></a>
                    <?php if (!$isAvailable): ?><small>Offre indisponible temporairement.</small><?php endif; ?>
                </article>
            <?php endforeach; ?>
        </div>
    </section>

    <section class="activation-flow band" id="activation">
        <div class="section-heading compact">
            <h2>Activer mon appareil</h2>
            <p>Un parcours simple : choisissez une licence, ouvrez SmartVision sur votre TV, puis associez le code affiché à votre compte.</p>
        </div>
        <ol class="steps">
            <li><span>01</span><div><strong>Ouvrez SmartVision</strong><p>L’application affiche un code TV unique sur l’écran d’activation.</p></div></li>
            <li><span>02</span><div><strong>Associez la licence</strong><p>Saisissez le code TV et votre code SmartVision depuis le site.</p></div></li>
            <li><span>03</span><div><strong>Configurez votre source</strong><p>Ajoutez uniquement des identifiants ou playlists pour lesquels vous disposez des droits nécessaires.</p></div></li>
        </ol>
        <div class="activation-actions">
            <a class="button button-primary" href="/activate/">Activer appareil</a>
            <a class="button button-outline" href="/activate/">Configurer playlist</a>
        </div>
    </section>

    <section class="feature-showcase band">
        <div class="showcase-image"><img src="/assets/images/app-movies.png" alt="Interface catalogue SmartVision"></div>
        <div class="showcase-copy">
            <h2>Une expérience pensée pour Android TV.</h2>
            <p>Navigation télécommande, grands focus visibles, reprise de lecture, favoris, historique et écran adapté au canapé. L’interface reste lisible et fluide sur Android TV.</p>
            
        </div>
    </section>



    <section class="faq-strip band" aria-label="Questions fréquentes">
        <article><strong>L’application est-elle gratuite ?</strong><span>Oui. Le téléchargement Android TV est gratuit. Premium est optionnel.</span></article>
        <article><strong>La licence inclut-elle du contenu ?</strong><span>Non. Elle active des fonctions du lecteur SmartVision uniquement.</span></article>
        <article><strong>Comment activer ma TV ?</strong><span>Utilisez le code affiché par l’application sur la page Activation.</span></article>
    </section>

    <section class="help-section band">
        <div>
            <h2>Besoin d’aide ?</h2>
            <p>Contactez le support pour l’installation, l’activation, une licence ou un problème technique lié au lecteur SmartVision.</p>
        </div>
        <a class="button button-outline" href="/contact/">Contact</a>
    </section>
</main>

<?php sv_render_site_footer(); ?>
</body>
</html>
