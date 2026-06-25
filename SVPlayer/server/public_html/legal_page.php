<?php
declare(strict_types=1);

require_once __DIR__ . '/_includes/site_layout.php';

$page = $legalPage ?? null;
if (!is_array($page)) {
    http_response_code(500);
    exit('Page configuration missing.');
}

sv_send_site_headers();
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="<?= sv_h((string) $page['description']) ?>">
    <title><?= sv_h((string) $page['title']) ?></title>
    <link rel="stylesheet" href="/assets/site.css?v=3">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=4">
    <link rel="stylesheet" href="/assets/mobile.css?v=5">
</head>
<body class="legal-page">
<?php sv_render_site_header(); ?>
<main class="legal-main">
    <section class="legal-hero band">
        <p class="eyebrow"><?= sv_h((string) ($page['eyebrow'] ?? 'Informations')) ?></p>
        <h1><?= sv_h((string) $page['heading']) ?></h1>
        <?php if (!empty($page['intro'])): ?><p><?= sv_h((string) $page['intro']) ?></p><?php endif; ?>
    </section>

    <section class="legal-content band">
        <?php foreach ($page['sections'] as $section): ?>
            <article class="legal-section">
                <h2><?= sv_h((string) $section['title']) ?></h2>
                <?php foreach (($section['paragraphs'] ?? []) as $paragraph): ?>
                    <p><?= $paragraph ?></p>
                <?php endforeach; ?>
                <?php if (!empty($section['items'])): ?>
                    <ul>
                        <?php foreach ($section['items'] as $item): ?><li><?= $item ?></li><?php endforeach; ?>
                    </ul>
                <?php endif; ?>
            </article>
        <?php endforeach; ?>
    </section>
</main>
<?php sv_render_site_footer(); ?>
</body>
</html>
