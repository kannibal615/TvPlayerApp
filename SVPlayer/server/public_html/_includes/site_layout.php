<?php
declare(strict_types=1);

function sv_h(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function sv_send_site_headers(string $frameAncestors = "'self'"): void
{
    header('Content-Type: text/html; charset=utf-8');
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: SAMEORIGIN');
    header('Referrer-Policy: strict-origin-when-cross-origin');
    header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self' https://pagead2.googlesyndication.com; connect-src 'self' https://pagead2.googlesyndication.com https://googleads.g.doubleclick.net https://tpc.googlesyndication.com; img-src 'self' data: https:; frame-src https://googleads.g.doubleclick.net https://tpc.googlesyndication.com; frame-ancestors {$frameAncestors}; base-uri 'self'; form-action 'self'");
}

function sv_render_site_header(): void
{
    ?>
<header class="site-header">
    <a class="brand" href="/" aria-label="SmartVision, accueil">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
    </a>
    <nav aria-label="Navigation principale">
        <a href="/#fonctionnalites">Fonctionnalités</a>
        <a href="/#legal">Lecteur légal</a>
        <a href="/#activation">Activation</a>
        <a href="/account/">Mon compte</a>
        <a href="/legal-iptv-player/">Lecteur IPTV légal</a>
    </nav>
    <a class="button button-primary header-cta" href="/download.php">Télécharger</a>
</header>
    <?php
}

function sv_render_site_footer(string $class = ''): void
{
    $classAttribute = $class !== '' ? ' class="' . sv_h($class) . '"' : '';
    ?>
<footer<?= $classAttribute ?>>
    <a class="brand footer-brand" href="/">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
    </a>
    <p>&copy; <?= date('Y') ?> SmartVision. Lecteur IPTV pour Android TV sans contenu inclus.</p>
    <div>
        <a href="/account/">Mon compte</a>
        <a href="/activate/">Activation</a>
        <a href="/privacy-policy/">Politique de confidentialité</a>
        <a href="/terms-of-use/">Conditions d’utilisation</a>
        <a href="/contact/">Contact</a>
        <a href="/legal-notice/">Mentions légales</a>
        <a href="/legal-iptv-player/">Lecteur IPTV légal</a>
    </div>
</footer>
    <?php
}
