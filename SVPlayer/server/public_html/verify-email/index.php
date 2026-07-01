<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/mail_service.php';
require_once dirname(__DIR__) . '/_includes/site_layout.php';

session_name('smartvision_customer');
session_set_cookie_params([
    'lifetime' => 0,
    'path' => '/',
    'secure' => smartvision_cookie_secure(),
    'httponly' => true,
    'samesite' => 'Lax',
]);
session_start();
sv_send_site_headers();

$pdo = db();
$token = trim((string) ($_GET['token'] ?? ''));
$success = false;
$message = 'Le lien de vérification est invalide ou a expiré.';

if (preg_match('/^[a-f0-9]{64}$/', $token)) {
    try {
        sv_mail_ensure_schema($pdo);
        $pdo->beginTransaction();
        $statement = $pdo->prepare(
            "SELECT t.id, t.user_id, u.email, u.display_name, u.email_verified_at
             FROM email_verification_tokens t
             JOIN site_users u ON u.id = t.user_id
             WHERE t.token_hash = :token_hash
               AND t.used_at IS NULL
               AND t.expires_at >= NOW()
             LIMIT 1
             FOR UPDATE"
        );
        $statement->execute(['token_hash' => hash('sha256', $token)]);
        $verification = $statement->fetch();
        if (!is_array($verification)) {
            throw new RuntimeException($message);
        }

        $pdo->prepare('UPDATE site_users SET email_verified_at = COALESCE(email_verified_at, NOW()), updated_at = NOW() WHERE id = :id')
            ->execute(['id' => (int) $verification['user_id']]);
        $pdo->prepare('UPDATE email_verification_tokens SET used_at = NOW() WHERE id = :id AND used_at IS NULL')
            ->execute(['id' => (int) $verification['id']]);
        $pdo->commit();

        session_regenerate_id(true);
        $_SESSION['site_user_id'] = (int) $verification['user_id'];
        unset($_SESSION['customer_intent']);
        $pdo->prepare('UPDATE site_users SET last_login_at = NOW(), updated_at = NOW() WHERE id = :id')
            ->execute(['id' => (int) $verification['user_id']]);
        sv_set_customer_header_state(true);

        $success = true;
        $message = 'Votre adresse email est confirmée. Vous pouvez maintenant commander une licence.';
        sv_send_email($pdo, 'registration_thanks', (string) $verification['email'], [
            'customer' => [
                'name' => sv_mail_customer_name((string) ($verification['display_name'] ?? ''), (string) $verification['email']),
                'email' => (string) $verification['email'],
            ],
            'account_url' => smartvision_public_base_url() . '/account/?section=buy-license',
        ]);
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('SmartVision email verification failed.');
        $message = $exception instanceof RuntimeException
            ? $exception->getMessage()
            : 'La vérification est temporairement indisponible.';
    }
}
?>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Vérification email | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=6">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=5">
    <link rel="stylesheet" href="/assets/account.css?v=6">
</head>
<body class="account-page payment-return-page">
<?php sv_render_site_header(); ?>
<main class="payment-return-shell">
    <section class="payment-return-card">
        <span class="payment-return-icon" aria-hidden="true"><?= $success ? '✓' : '!' ?></span>
        <h1><?= $success ? 'Adresse email confirmée' : 'Vérification impossible' ?></h1>
        <p><?= sv_h($message) ?></p>
        <div class="payment-return-actions">
            <a class="button button-primary" href="/account/<?= $success ? '?section=buy-license' : '?section=profile' ?>">Accéder à mon compte</a>
            <a class="button button-outline" href="/contact/">Contacter le support</a>
        </div>
    </section>
</main>
<?php sv_render_site_footer('account-footer'); ?>
</body>
</html>
