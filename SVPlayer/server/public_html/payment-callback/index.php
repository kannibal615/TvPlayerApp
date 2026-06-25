<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/commerce.php';
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

$pdo = db();

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
    try {
        $payload = json_decode((string) file_get_contents('php://input'), true, 32, JSON_THROW_ON_ERROR);
        if (!is_array($payload) || !is_array($payload['payment'] ?? null)) {
            throw new InvalidArgumentException('Retour de paiement invalide.');
        }
        $result = commerce_record_gammal_callback(
            $pdo,
            trim((string) ($payload['intent'] ?? '')),
            $payload['payment'],
        );

        if (empty($result['duplicate'])) {
            sv_send_email($pdo, 'payment_received', (string) $result['email'], [
                'customer' => ['name' => (string) ($result['display_name'] ?: $result['email'])],
                'order' => [
                    'reference' => (string) $result['order_reference'],
                    'plan' => (string) $result['plan_label'],
                ],
                'payment' => ['txn' => (string) $result['txn']],
                'orders_url' => smartvision_public_base_url() . '/account/?section=orders',
            ]);
            sv_send_admin_notification($pdo, 'admin_notification_payment_review', [
                'Commande' => (string) $result['order_reference'],
                'Transaction' => (string) $result['txn'],
                'Email client' => (string) $result['email'],
                'Pack' => (string) $result['plan_label'],
                'Montant client' => commerce_money((int) $result['amount_cents'], (string) $result['currency']),
            ], '/admin/?page=payments');
        }

        echo json_encode([
            'success' => true,
            'duplicate' => !empty($result['duplicate']),
            'status' => (string) $result['verification_status'],
            'order_reference' => (string) ($result['order_reference'] ?? ''),
            'account_url' => '/account/?section=orders',
        ], JSON_UNESCAPED_SLASHES);
    } catch (Throwable $exception) {
        error_log('SmartVision Gammal callback processing failed.');
        http_response_code($exception instanceof InvalidArgumentException ? 422 : 409);
        echo json_encode([
            'success' => false,
            'error' => $exception->getMessage() ?: 'Retour de paiement impossible à enregistrer.',
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }
    exit;
}

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
header('Referrer-Policy: strict-origin-when-cross-origin');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self' https://api.gammal.tech; connect-src 'self' https://api.gammal.tech; img-src 'self' data: https:; frame-ancestors 'self'; base-uri 'self'; form-action 'self'");

$userId = (int) ($_SESSION['site_user_id'] ?? 0);
$isLoggedIn = $userId > 0;
sv_set_customer_header_state($isLoggedIn);
$simulationResult = is_array($_SESSION['payment_simulation_result'] ?? null)
    ? $_SESSION['payment_simulation_result']
    : null;
$simulationError = '';

if (isset($_GET['simulation_token'])) {
    try {
        if ((string) getenv('SMARTVISION_ENV') !== 'development') {
            throw new RuntimeException('Simulation indisponible.');
        }
        if (!$isLoggedIn) {
            throw new RuntimeException('Connectez-vous avant de lancer la simulation.');
        }

        $receivedToken = trim((string) $_GET['simulation_token']);
        $storedToken = (string) ($_SESSION['payment_simulation_token'] ?? '');
        $expiresAt = (int) ($_SESSION['payment_simulation_expires_at'] ?? 0);
        if (
            !preg_match('/^[a-f0-9]{48}$/', $receivedToken)
            || !preg_match('/^[a-f0-9]{48}$/', $storedToken)
            || !hash_equals($storedToken, $receivedToken)
            || $expiresAt < time()
        ) {
            throw new RuntimeException('Lien de simulation expiré ou déjà utilisé.');
        }

        $userStatement = $pdo->prepare('SELECT email, display_name, email_verified_at FROM site_users WHERE id = :id LIMIT 1');
        $userStatement->execute(['id' => $userId]);
        $customer = $userStatement->fetch();
        if (!is_array($customer) || empty($customer['email_verified_at'])) {
            throw new RuntimeException('Confirmez votre adresse email avant de lancer la simulation.');
        }

        unset($_SESSION['payment_simulation_token'], $_SESSION['payment_simulation_expires_at']);
        $order = commerce_create_test_order($pdo, $userId, 'simulation', $receivedToken);
        $_SESSION['payment_simulation_result'] = [
            'order_reference' => (string) $order['order_reference'],
            'activation_code' => (string) ($order['activation_code'] ?? ''),
        ];
        sv_send_email($pdo, 'order_confirmed', (string) $customer['email'], [
            'customer' => ['name' => (string) ($customer['display_name'] ?: $customer['email'])],
            'order' => [
                'reference' => (string) $order['order_reference'],
                'plan' => (string) $order['plan_label'],
                'amount' => commerce_money((int) $order['amount_cents'], (string) $order['currency']),
            ],
            'license' => ['code' => (string) ($order['activation_code'] ?? '')],
            'account_url' => smartvision_public_base_url() . '/account/?section=licenses',
        ]);
        sv_send_admin_notification($pdo, 'admin_notification_order_created', [
            'Commande' => (string) $order['order_reference'],
            'Email client' => (string) $customer['email'],
            'Pack' => (string) $order['plan_label'],
            'Mode' => 'Simulation développement',
        ], '/admin/?page=orders');
        header('Location: /payment-callback/?simulation=complete', true, 303);
        exit;
    } catch (Throwable $exception) {
        error_log('SmartVision payment simulation failed.');
        $simulationError = $exception->getMessage() ?: 'Simulation impossible.';
    }
}

$simulationComplete = ($_GET['simulation'] ?? '') === 'complete' && $simulationResult !== null;
$intentToken = trim((string) ($_GET['intent'] ?? ''));
$intent = $intentToken !== '' ? commerce_load_payment_intent($pdo, $intentToken) : null;
$cancelled = strtolower(trim((string) ($_GET['status'] ?? ''))) === 'cancelled';
if ($cancelled && is_array($intent) && ($intent['status'] ?? '') === 'started') {
    $pdo->prepare("UPDATE commerce_order_intents SET status = 'cancelled', updated_at = NOW() WHERE id = :id AND status = 'started'")
        ->execute(['id' => (int) $intent['id']]);
}
$canVerify = !$simulationComplete && $simulationError === '' && !$cancelled && is_array($intent);
?>
<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Retour de paiement | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=5">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=5">
    <link rel="stylesheet" href="/assets/account.css?v=6">
    <?php if ($canVerify): ?><script src="https://api.gammal.tech/sdk/pay/link/verify.js" defer></script><?php endif; ?>
    <script src="/assets/payment-callback.js?v=1" defer></script>
</head>
<body class="account-page payment-return-page">
<?php sv_render_site_header(); ?>
<main
    class="payment-return-shell"
    data-payment-callback
    data-intent="<?= sv_h($canVerify ? $intentToken : '') ?>"
    data-account-url="<?= $isLoggedIn ? '/account/?section=orders' : '/account/?mode=login' ?>"
>
    <section class="payment-return-card">
        <span class="payment-return-icon" aria-hidden="true"><?= $simulationError === '' && !$cancelled ? '✓' : '!' ?></span>
        <?php if ($simulationComplete): ?>
        <h1>Simulation de paiement réussie</h1>
        <p>Le callback simulé a créé une commande payée et une licence test valable 1 jour.</p>
        <?php if (($simulationResult['activation_code'] ?? '') !== ''): ?>
            <div class="license-code-box"><span>Code licence test</span><strong><?= sv_h((string) $simulationResult['activation_code']) ?></strong></div>
        <?php endif; ?>
        <?php elseif ($simulationError !== ''): ?>
        <h1>Simulation impossible</h1>
        <p><?= sv_h($simulationError) ?></p>
        <?php elseif ($cancelled): ?>
        <h1>Paiement annulé</h1>
        <p>Aucun paiement n’a été validé et aucune licence n’a été créée.</p>
        <?php elseif ($canVerify): ?>
        <h1 data-payment-title>Vérification du paiement</h1>
        <p data-payment-message>Vérification du retour Gammal Tech en cours…</p>
        <?php else: ?>
        <h1>Retour de paiement incomplet</h1>
        <p>Cette page ne contient pas d’intention de commande valide. Contactez le support si votre banque a confirmé le débit.</p>
        <?php endif; ?>
        <div class="payment-return-actions">
            <a class="button button-primary" data-payment-account href="<?= $isLoggedIn ? '/account/?section=orders' : '/account/?mode=login' ?>">
                <?= $isLoggedIn ? 'Voir mon compte' : 'Se connecter' ?>
            </a>
            <a class="button button-outline" href="/contact/">Contacter le support</a>
        </div>
        <small>Ne relancez pas le paiement si votre banque a déjà confirmé le débit.</small>
    </section>
</main>
<?php sv_render_site_footer('account-footer'); ?>
</body>
</html>
