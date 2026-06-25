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

function sv_apk_manifest(): array
{
    $path = dirname(__DIR__) . '/downloads/smartvision-tv.version.json';
    if (!is_file($path)) {
        return [];
    }

    $raw = preg_replace('/^\xEF\xBB\xBF/', '', (string) file_get_contents($path));
    $manifest = json_decode((string) $raw, true);
    if (!is_array($manifest)) {
        return [];
    }

    return $manifest;
}

function sv_render_site_header(): void
{
    ?>
<header class="site-header">
    <a class="brand" href="/" aria-label="SmartVision, accueil">
        <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
    </a>
    <button class="site-menu-toggle" type="button" data-site-menu-toggle aria-expanded="false" aria-controls="site-nav" aria-label="Ouvrir le menu">
        <span></span><span></span><span></span>
    </button>
    <nav id="site-nav" class="site-nav" aria-label="Navigation principale" data-site-menu>
        <a href="/">Accueil</a>
        <a href="/#tarifs">Tarifs</a>
        <a href="/activate/">Activation</a>
        <a href="/#telecharger">Télécharger l’application</a>
        <a href="/legal-iptv-player/">Lecteur IPTV légal</a>
        <a class="mobile-only" href="/account/?mode=login">Connexion</a>
    </nav>
    <div class="site-actions">
        <a class="button button-outline header-cta" href="/account/?mode=login">Connexion</a>
        <a class="button button-primary header-cta" href="/account/?intent=license&plan=year_1">Acheter une licence</a>
    </div>
</header>
    <?php
}

function sv_render_site_footer(string $class = ''): void
{
    $classAttribute = $class !== '' ? ' class="' . sv_h($class) . '"' : '';
    ?>
<footer<?= $classAttribute ?>>
    <div class="footer-main">
        <a class="brand footer-brand" href="/">
            <img class="brand-logo-wide" src="/assets/images/smartvision-logo-wide.png?v=3" alt="SmartVision IPTV Player">
        </a>
        <p>SmartVision est un lecteur Android TV légal. L’application ne fournit, ne vend et n’héberge aucun contenu audiovisuel, chaîne TV, film, série ou playlist.</p>
    </div>
    <nav class="footer-links" aria-label="Liens de pied de page">
        <a href="/">Accueil</a>
        <a href="/#tarifs">Tarifs</a>
        <a href="/activate/">Activation</a>
        <a href="/account/">Compte client</a>
        <a href="/contact/">Contact</a>
        <a href="/terms-of-use/">Conditions d’utilisation</a>
        <a href="/privacy-policy/">Confidentialité</a>
        <a href="/legal-notice/">Mentions légales</a>
        <a href="/legal-iptv-player/">Lecteur IPTV légal</a>
    </nav>
    <p class="footer-copy">&copy; <?= date('Y') ?> SmartVision. Lecteur Android TV, sans contenu inclus.</p>
</footer>
<script src="/assets/site.js?v=5" defer></script>
    <?php
}
