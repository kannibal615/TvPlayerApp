<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/commerce.php';
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
$userId = (int) ($_SESSION['site_user_id'] ?? 0);
if ($userId <= 0) {
    header('Location: /account/?mode=login', true, 303);
    exit;
}

$orderReference = preg_replace('/[^A-Z0-9-]/', '', strtoupper((string) ($_GET['order'] ?? '')));
$order = null;
$user = null;

try {
    $userQuery = $pdo->prepare('SELECT id, email, display_name FROM site_users WHERE id = :id LIMIT 1');
    $userQuery->execute(['id' => $userId]);
    $foundUser = $userQuery->fetch();
    if (is_array($foundUser)) {
        $user = $foundUser;
    }

    if ($orderReference !== '') {
        $statement = $pdo->prepare(
            "SELECT id, order_reference, plan_label, amount_cents, currency, status,
                    activation_code_ciphertext, created_at, paid_at
             FROM activation_orders
             WHERE user_id = :user_id AND order_reference = :order_reference
             LIMIT 1"
        );
        $statement->execute([
            'user_id' => $userId,
            'order_reference' => $orderReference,
        ]);
        $foundOrder = $statement->fetch();
        if (is_array($foundOrder)) {
            $foundOrder['activation_code'] = decrypt_private_value($foundOrder['activation_code_ciphertext'] ?? null);
            $order = $foundOrder;
        }
    }
} catch (Throwable $exception) {
    error_log('SmartVision success page failed.');
}

$email = is_array($user) ? (string) $user['email'] : '';
$code = is_array($order) ? (string) ($order['activation_code'] ?? '') : '';
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Confirmation de paiement SmartVision et code licence.">
    <title>Paiement confirmé | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=5">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=5">
    <link rel="stylesheet" href="/assets/account.css?v=5">
</head>
<body class="success-page">
<?php sv_render_site_header(); ?>
<main class="success-shell">
    <section class="success-card">
        <?php if (!is_array($order) || ($order['status'] ?? '') !== 'paid'): ?>
            <h1>Confirmation introuvable</h1>
            <p class="lead">Impossible de retrouver cette commande sur votre compte. Retournez dans votre espace client pour consulter vos licences.</p>
            <div class="success-actions">
                <a class="button button-primary" href="/account/">Accéder à mon espace client</a>
                <a class="button button-outline" href="/contact/">Contacter le support</a>
            </div>
        <?php else: ?>
            <h1>Paiement confirmé</h1>
            <p class="lead">Votre licence SmartVision a été créée avec succès.</p>
            <p class="success-test-note">Paiement test accepté.</p>

            <dl class="success-summary">
                <div><dt>Offre achetée</dt><dd><?= sv_h((string) $order['plan_label']) ?></dd></div>
                <div><dt>Email client</dt><dd><?= sv_h($email) ?></dd></div>
                <div><dt>Commande</dt><dd><?= sv_h((string) $order['order_reference']) ?></dd></div>
                <div><dt>Montant</dt><dd><?= sv_h(commerce_money((int) $order['amount_cents'], (string) $order['currency'])) ?></dd></div>
            </dl>

            <div class="license-code-box">
                <span>Conservez votre code licence. Il permet d’activer votre appareil Android TV.</span>
                <strong><?= $code !== '' ? sv_h($code) : 'Code indisponible' ?></strong>
            </div>

            <div class="success-actions">
                <a class="button button-primary" href="/account/?section=licenses&amp;order=<?= sv_h((string) $order['order_reference']) ?>">Accéder à mon espace client</a>
                <a class="button button-outline" href="/activate/">Activer une TV</a>
            </div>
        <?php endif; ?>
    </section>
</main>
<?php sv_render_site_footer(); ?>
</body>
</html>
