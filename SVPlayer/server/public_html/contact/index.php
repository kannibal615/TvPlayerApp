<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/_includes/site_layout.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';

session_name('smartvision_contact');
session_set_cookie_params([
    'lifetime' => 0,
    'path' => '/',
    'secure' => smartvision_cookie_secure(),
    'httponly' => true,
    'samesite' => 'Lax',
]);
session_start();

sv_send_site_headers();

function contact_token(): string
{
    if (!is_string($_SESSION['contact_csrf'] ?? null)) {
        $_SESSION['contact_csrf'] = bin2hex(random_bytes(32));
    }
    return (string) $_SESSION['contact_csrf'];
}

$startedAt = (int) ($_SESSION['contact_started_at'] ?? time());
$_SESSION['contact_started_at'] = $startedAt;
$csrf = contact_token();
$success = false;
$error = '';
$old = [
    'name' => '',
    'email' => '',
    'subject' => '',
    'message' => '',
];

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    $old = [
        'name' => smartvision_text_substr(trim((string) ($_POST['name'] ?? '')), 0, 120),
        'email' => smartvision_text_substr(strtolower(trim((string) ($_POST['email'] ?? ''))), 0, 190),
        'subject' => smartvision_text_substr(trim((string) ($_POST['subject'] ?? '')), 0, 160),
        'message' => smartvision_text_substr(trim((string) ($_POST['message'] ?? '')), 0, 4000),
    ];
    try {
        $postedToken = (string) ($_POST['csrf_token'] ?? '');
        if ($postedToken === '' || !hash_equals($csrf, $postedToken)) {
            throw new RuntimeException('Session expirée. Rechargez la page.');
        }
        if (trim((string) ($_POST['website'] ?? '')) !== '') {
            throw new RuntimeException('Message refusé.');
        }
        if (time() - (int) ($_POST['started_at'] ?? 0) < 3) {
            throw new RuntimeException('Merci de patienter quelques secondes avant l’envoi.');
        }
        if (!filter_var($old['email'], FILTER_VALIDATE_EMAIL)) {
            throw new RuntimeException('Saisissez une adresse email valide.');
        }
        if ($old['name'] === '' || $old['subject'] === '' || strlen($old['message']) < 12) {
            throw new RuntimeException('Complétez le nom, le sujet et un message suffisamment précis.');
        }
        if ((int) ($_SESSION['contact_last_sent_at'] ?? 0) > time() - 60) {
            throw new RuntimeException('Un message vient déjà d’être envoyé. Réessayez dans une minute.');
        }

        $pdo = db();
        ensure_contact_messages_table($pdo);
        $insert = $pdo->prepare(
            "INSERT INTO contact_messages
                (name, email, subject, message, status, ip_hash, user_agent, created_at, updated_at)
             VALUES
                (:name, :email, :subject, :message, 'new', :ip_hash, :user_agent, NOW(), NOW())"
        );
        $insert->execute([
            'name' => $old['name'],
            'email' => $old['email'],
            'subject' => $old['subject'],
            'message' => $old['message'],
            'ip_hash' => request_ip_hash(),
            'user_agent' => smartvision_text_substr((string) ($_SERVER['HTTP_USER_AGENT'] ?? ''), 0, 255) ?: null,
        ]);
        $_SESSION['contact_last_sent_at'] = time();
        $_SESSION['contact_started_at'] = time();
        $success = true;
        $old = ['name' => '', 'email' => '', 'subject' => '', 'message' => ''];
    } catch (Throwable $exception) {
        $error = $exception->getMessage() ?: 'Envoi impossible. Réessayez.';
    }
}
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Contacter le support SmartVision pour l’installation, l’activation, la licence ou un problème technique.">
    <title>Contact | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=6">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=4">
    <link rel="stylesheet" href="/assets/account.css?v=4">
</head>
<body class="legal-page">
<?php sv_render_site_header(); ?>
<main class="legal-main contact-main">
    <section class="legal-hero band">
        <h1>Contact</h1>
        <p>Écrivez au support SmartVision pour une question liée à l’application, l’activation, la licence ou un problème technique.</p>
    </section>

    <section class="contact-layout band">
        <form method="post" class="contact-form">
            <input type="hidden" name="csrf_token" value="<?= sv_h($csrf) ?>">
            <input type="hidden" name="started_at" value="<?= (int) $startedAt ?>">
            <label class="robot-field">Site web<input name="website" tabindex="-1" autocomplete="off"></label>
            <?php if ($success): ?><div class="form-notice success">Message envoyé. Il apparaît maintenant dans le panel admin.</div><?php endif; ?>
            <?php if ($error !== ''): ?><div class="form-notice error" role="alert"><?= sv_h($error) ?></div><?php endif; ?>
            <div class="field"><label for="contact-name">Nom</label><input id="contact-name" name="name" value="<?= sv_h($old['name']) ?>" required></div>
            <div class="field"><label for="contact-email">Email</label><input id="contact-email" name="email" type="email" value="<?= sv_h($old['email']) ?>" required></div>
            <div class="field"><label for="contact-subject">Sujet</label><input id="contact-subject" name="subject" value="<?= sv_h($old['subject']) ?>" maxlength="160" required></div>
            <div class="field"><label for="contact-message">Message</label><textarea id="contact-message" name="message" rows="7" required><?= sv_h($old['message']) ?></textarea></div>
            <button class="button button-primary" type="submit">Envoyer le message</button>
        </form>
        <aside class="contact-aside">
            <h2>Avant d’écrire</h2>
            <p>SmartVision ne fournit pas de contenus, chaînes, films, séries ou playlists. Le support répond uniquement sur le lecteur, la licence et l’activation.</p>
            <ul>
                <li>Indiquez le code public de votre TV si possible.</li>
                <li>Décrivez l’écran concerné et le message affiché.</li>
                <li>N’envoyez jamais de mot de passe dans ce formulaire.</li>
            </ul>
        </aside>
    </section>
</main>
<?php sv_render_site_footer(); ?>
</body>
</html>
