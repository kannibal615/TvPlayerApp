<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/config.php';
require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/commerce.php';

session_name('smartvision_customer');
session_set_cookie_params([
    'lifetime' => 0,
    'path' => '/',
    'secure' => true,
    'httponly' => true,
    'samesite' => 'Lax',
]);
session_start();

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
header('Referrer-Policy: strict-origin-when-cross-origin');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; frame-ancestors 'self'; base-uri 'self'; form-action 'self'");

function account_escape(mixed $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function account_csrf_token(): string
{
    if (!is_string($_SESSION['customer_csrf'] ?? null)) {
        $_SESSION['customer_csrf'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['customer_csrf'];
}

function account_assert_csrf(): void
{
    $stored = $_SESSION['customer_csrf'] ?? '';
    $received = $_POST['csrf_token'] ?? '';
    if (!is_string($stored) || !is_string($received) || $stored === '' || !hash_equals($stored, $received)) {
        throw new RuntimeException('Session expiree. Rechargez la page.');
    }
}

function account_current_user(PDO $pdo): ?array
{
    $userId = (int) ($_SESSION['site_user_id'] ?? 0);
    if ($userId <= 0) {
        return null;
    }

    $statement = $pdo->prepare(
        "SELECT id, email, display_name, status, created_at, last_login_at
         FROM site_users WHERE id = :id LIMIT 1"
    );
    $statement->execute(['id' => $userId]);
    $user = $statement->fetch();
    if (!is_array($user) || ($user['status'] ?? 'active') !== 'active') {
        unset($_SESSION['site_user_id']);
        return null;
    }
    return $user;
}

function account_flash(string $type, string $message): void
{
    $_SESSION['customer_flash'] = ['type' => $type, 'message' => $message];
}

function account_consume_flash(): ?array
{
    $flash = $_SESSION['customer_flash'] ?? null;
    unset($_SESSION['customer_flash']);
    return is_array($flash) ? $flash : null;
}

function account_redirect(string $planKey = 'year_1', ?string $highlight = null): never
{
    $url = '/account/?plan=' . rawurlencode($planKey);
    if ($highlight !== null && $highlight !== '') {
        $url .= '&order=' . rawurlencode($highlight);
    }
    header('Location: ' . $url, true, 303);
    exit;
}

$pdo = db();
$plans = commerce_plans();
$planKey = commerce_plan_key($_REQUEST['plan'] ?? $_SESSION['selected_plan'] ?? 'year_1');
$_SESSION['selected_plan'] = $planKey;
$error = null;

try {
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        account_assert_csrf();
        $action = (string) ($_POST['action'] ?? '');
        $planKey = commerce_plan_key($_POST['plan'] ?? $planKey);
        $_SESSION['selected_plan'] = $planKey;

        if ($action === 'logout') {
            $_SESSION = [];
            session_destroy();
            header('Location: /account/', true, 303);
            exit;
        }

        if ($action === 'register') {
            $email = strtolower(trim((string) ($_POST['email'] ?? '')));
            $displayName = clean_optional_text($_POST['display_name'] ?? null, 120);
            $password = (string) ($_POST['password'] ?? '');
            if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
                throw new RuntimeException('Saisissez une adresse email valide.');
            }
            if (strlen($password) < 8) {
                throw new RuntimeException('Le mot de passe doit contenir au moins 8 caracteres.');
            }

            try {
                $insert = $pdo->prepare(
                    "INSERT INTO site_users (email, display_name, password_hash, status, created_at, updated_at)
                     VALUES (:email, :display_name, :password_hash, 'active', NOW(), NOW())"
                );
                $insert->execute([
                    'email' => $email,
                    'display_name' => $displayName ?: strstr($email, '@', true),
                    'password_hash' => password_hash($password, PASSWORD_DEFAULT),
                ]);
            } catch (Throwable $exception) {
                if (is_duplicate_key($exception)) {
                    throw new RuntimeException('Un compte existe deja avec cette adresse. Connectez-vous.');
                }
                throw $exception;
            }

            session_regenerate_id(true);
            $_SESSION['site_user_id'] = (int) $pdo->lastInsertId();
            account_flash('success', 'Compte cree. Choisissez votre licence puis confirmez la commande.');
            account_redirect($planKey);
        }

        if ($action === 'login') {
            $email = strtolower(trim((string) ($_POST['email'] ?? '')));
            $password = (string) ($_POST['password'] ?? '');
            $statement = $pdo->prepare(
                "SELECT id, password_hash, status FROM site_users WHERE email = :email LIMIT 1"
            );
            $statement->execute(['email' => $email]);
            $found = $statement->fetch();
            if (!is_array($found) || !password_verify($password, (string) $found['password_hash'])) {
                usleep(300000);
                throw new RuntimeException('Email ou mot de passe incorrect.');
            }
            if (($found['status'] ?? 'active') !== 'active') {
                throw new RuntimeException('Ce compte est temporairement bloque. Contactez le support.');
            }

            session_regenerate_id(true);
            $_SESSION['site_user_id'] = (int) $found['id'];
            $pdo->prepare('UPDATE site_users SET last_login_at = NOW(), updated_at = NOW() WHERE id = :id')
                ->execute(['id' => $_SESSION['site_user_id']]);
            account_flash('success', 'Connexion reussie.');
            account_redirect($planKey);
        }

        if ($action === 'test_payment') {
            $user = account_current_user($pdo);
            if ($user === null) {
                throw new RuntimeException('Connectez-vous pour commander.');
            }
            if (($_POST['accept_terms'] ?? '') !== '1') {
                throw new RuntimeException('Confirmez que la licence ne contient aucun abonnement IPTV.');
            }
            $checkoutToken = (string) ($_POST['checkout_token'] ?? '');
            if (!hash_equals((string) ($_SESSION['checkout_token'] ?? ''), $checkoutToken)) {
                throw new RuntimeException('Cette commande a expire. Rechargez la page.');
            }

            $order = commerce_create_test_order($pdo, (int) $user['id'], $planKey, $checkoutToken);
            $_SESSION['checkout_token'] = commerce_checkout_token();
            account_flash('success', 'Paiement test accepte. Votre code SmartVision est disponible ci-dessous.');
            account_redirect($planKey, (string) $order['order_reference']);
        }
    }
} catch (Throwable $exception) {
    error_log('SmartVision customer action failed.');
    $error = $exception->getMessage() ?: 'Operation impossible. Reessayez.';
}

$user = account_current_user($pdo);
$orders = $user ? commerce_load_customer_orders($pdo, (int) $user['id']) : [];
$summary = commerce_customer_summary($orders);
$flash = account_consume_flash();
$highlightOrder = preg_replace('/[^A-Z0-9-]/', '', strtoupper((string) ($_GET['order'] ?? '')));
$checkoutToken = (string) ($_SESSION['checkout_token'] ?? '');
if (!preg_match('/^[a-f0-9]{48}$/', $checkoutToken)) {
    $checkoutToken = commerce_checkout_token();
    $_SESSION['checkout_token'] = $checkoutToken;
}
$csrf = account_csrf_token();
$selectedPlan = commerce_plan($planKey);
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Gerez vos licences, commandes et activations SmartVision.">
    <title><?= $user ? 'Mon compte' : 'Creer mon compte' ?> | SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=2">
    <link rel="stylesheet" href="/assets/account.css?v=2">
    <link rel="stylesheet" href="/assets/mobile.css?v=2">
</head>
<body class="account-page">
<header class="site-header account-header">
    <a class="brand" href="/" aria-label="SmartVision, accueil">
        <img class="brand-mark" src="/assets/images/smartvision-mark.png" alt="">
        <img class="brand-wordmark" src="/assets/images/smartvision-wordmark.png" alt="SmartVision IPTV Player">
    </a>
    <nav aria-label="Navigation compte">
        <a href="/">Accueil</a>
        <a href="/download.php">Telecharger</a>
        <a href="/activate/">Activer une TV</a>
    </nav>
    <?php if ($user): ?>
        <div class="account-user-menu">
            <span><?= account_escape($user['display_name'] ?: $user['email']) ?></span>
            <form method="post">
                <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
                <button class="button button-outline header-cta" name="action" value="logout">Deconnexion</button>
            </form>
        </div>
    <?php else: ?>
        <a class="button button-outline header-cta" href="/activate/">J'ai deja un code</a>
    <?php endif; ?>
</header>

<?php if (!$user): ?>
<main class="auth-layout">
    <section class="auth-intro">
        <p class="section-label">Compte SmartVision</p>
        <h1>Votre activation en quelques minutes.</h1>
        <p>Un seul compte pour retrouver vos commandes, vos codes et l'etat de vos appareils.</p>
        <ol class="compact-steps">
            <li><span>1</span><div><strong>Creez votre compte</strong><small>Une adresse email et un mot de passe.</small></div></li>
            <li><span>2</span><div><strong>Choisissez votre licence</strong><small>Une activation correspond a un appareil.</small></div></li>
            <li><span>3</span><div><strong>Activez votre TV</strong><small>Saisissez le code depuis votre telephone.</small></div></li>
        </ol>
        <div class="auth-plan-summary">
            <span>Licence selectionnee</span>
            <strong><?= account_escape($selectedPlan['label']) ?> - <?= commerce_money((int) $selectedPlan['amount_cents']) ?></strong>
        </div>
    </section>
    <section class="auth-form-panel">
        <h2>Creer mon compte</h2>
        <p>Votre licence restera accessible ici apres la commande.</p>
        <?php if ($error): ?><div class="form-notice error" role="alert"><?= account_escape($error) ?></div><?php endif; ?>
        <form method="post" class="stack-form">
            <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
            <input type="hidden" name="plan" value="<?= account_escape($planKey) ?>">
            <div class="field"><label for="register-name">Nom</label><input id="register-name" name="display_name" autocomplete="name" required></div>
            <div class="field"><label for="register-email">Email</label><input id="register-email" name="email" type="email" autocomplete="email" required></div>
            <div class="field"><label for="register-password">Mot de passe</label><input id="register-password" name="password" type="password" minlength="8" autocomplete="new-password" required><small>8 caracteres minimum.</small></div>
            <button class="button button-primary" name="action" value="register">Creer mon compte et continuer</button>
        </form>
        <details class="login-disclosure">
            <summary>J'ai deja un compte</summary>
            <form method="post" class="stack-form compact-form">
                <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
                <input type="hidden" name="plan" value="<?= account_escape($planKey) ?>">
                <div class="field"><label for="login-email">Email</label><input id="login-email" name="email" type="email" autocomplete="email" required></div>
                <div class="field"><label for="login-password">Mot de passe</label><input id="login-password" name="password" type="password" autocomplete="current-password" required></div>
                <button class="button button-outline" name="action" value="login">Se connecter</button>
            </form>
        </details>
    </section>
</main>
<?php else: ?>
<main class="customer-dashboard">
    <section class="customer-heading">
        <div><p class="section-label">Espace client</p><h1>Bonjour <?= account_escape($user['display_name'] ?: $user['email']) ?></h1><p>Commandez une licence, retrouvez vos codes et activez vos appareils.</p></div>
        <a class="button button-outline" href="/activate/">Activer une TV</a>
    </section>

    <?php if ($flash): ?><div class="form-notice <?= account_escape($flash['type'] ?? '') ?>"><?= account_escape($flash['message'] ?? '') ?></div><?php endif; ?>
    <?php if ($error): ?><div class="form-notice error" role="alert"><?= account_escape($error) ?></div><?php endif; ?>

    <section class="customer-stats" aria-label="Resume du compte">
        <div><span>Commandes</span><strong><?= (int) $summary['orders'] ?></strong></div>
        <div><span>Licences</span><strong><?= (int) $summary['licenses'] ?></strong></div>
        <div><span>Licences actives</span><strong><?= (int) $summary['active_licenses'] ?></strong></div>
        <div><span>Appareils actifs</span><strong><?= (int) $summary['devices'] ?></strong></div>
    </section>

    <section class="purchase-progress" aria-label="Etapes de commande">
        <div class="active"><span>1</span><strong>Choisir</strong><small>Selectionnez la licence</small></div>
        <div><span>2</span><strong>Paiement</strong><small>Mode test securise</small></div>
        <div><span>3</span><strong>Code</strong><small>Disponible immediatement</small></div>
    </section>

    <form method="post" class="checkout-layout" id="checkout-form">
        <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
        <input type="hidden" name="checkout_token" value="<?= account_escape($checkoutToken) ?>">
        <section class="plan-selector">
            <div class="panel-heading"><div><h2>Choisissez votre licence</h2><p>Une activation correspond a un seul appareil.</p></div></div>
            <div class="account-plan-grid">
                <?php foreach ($plans as $key => $plan): ?>
                    <label class="account-plan<?= $key === $planKey ? ' selected' : '' ?>">
                        <input type="radio" name="plan" value="<?= account_escape($key) ?>" data-plan-label="<?= account_escape($plan['label']) ?>" data-plan-price="<?= account_escape(commerce_money((int) $plan['amount_cents'])) ?>"<?= $key === $planKey ? ' checked' : '' ?>>
                        <?php if (!empty($plan['recommended'])): ?><span class="plan-recommended">Le plus choisi</span><?php endif; ?>
                        <strong><?= account_escape($plan['label']) ?></strong>
                        <div class="account-plan-price"><?= commerce_money((int) $plan['amount_cents']) ?></div>
                        <p><?= account_escape($plan['description']) ?></p>
                        <ul><li>1 appareil</li><li>Mises a jour incluses</li><li>Votre propre abonnement Xtream</li></ul>
                    </label>
                <?php endforeach; ?>
            </div>
            <div class="license-rule"><strong>Important</strong><span>SmartVision est un lecteur. La licence ne fournit aucune chaine, film ou abonnement IPTV.</span></div>
        </section>

        <aside class="order-summary">
            <h2>Votre commande</h2>
            <dl><div><dt>Licence</dt><dd id="summary-plan"><?= account_escape($selectedPlan['label']) ?></dd></div><div><dt>Appareils</dt><dd>1</dd></div><div><dt>Frais</dt><dd>0,00 EUR</dd></div></dl>
            <div class="order-total"><span>Total</span><strong id="summary-price"><?= commerce_money((int) $selectedPlan['amount_cents']) ?></strong></div>
            <div class="test-payment-note"><strong>Paiement de test</strong><span>Aucun montant reel ne sera debite. Le code genere est utilisable.</span></div>
            <label class="terms-check"><input type="checkbox" name="accept_terms" value="1" required><span>Je confirme acheter le lecteur SmartVision, sans contenu IPTV inclus.</span></label>
            <button class="button button-primary" name="action" value="test_payment">Valider le paiement test</button>
            <small class="secure-note">Prix et duree verifies cote serveur. Une seule licence sera generee.</small>
        </aside>
    </form>

    <section class="licenses-section" id="licenses">
        <div class="panel-heading"><div><h2>Mes licences</h2><p>Vos codes restent disponibles dans votre compte.</p></div><a class="text-link" href="/download.php">Telecharger la derniere APK</a></div>
        <?php if ($orders === []): ?>
            <div class="empty-state"><strong>Aucune licence pour le moment.</strong><span>Choisissez une offre ci-dessus pour recevoir votre premier code.</span></div>
        <?php else: ?>
            <div class="customer-table-wrap">
                <table class="customer-table">
                    <thead><tr><th>Commande</th><th>Licence</th><th>Montant</th><th>Paiement</th><th>Code SmartVision</th><th>Appareil</th><th>Expiration</th><th>Actions</th></tr></thead>
                    <tbody>
                    <?php foreach ($orders as $order):
                        $isHighlighted = $highlightOrder !== '' && hash_equals((string) $order['order_reference'], $highlightOrder);
                        $plainCode = (string) ($order['activation_code'] ?? '');
                        $activeDevices = (int) ($order['active_devices'] ?? 0);
                    ?>
                        <tr<?= $isHighlighted ? ' class="highlight-order"' : '' ?>>
                            <td><strong><?= account_escape($order['order_reference'] ?: 'SV-' . $order['id']) ?></strong><small><?= account_escape((string) $order['created_at']) ?></small></td>
                            <td><?= account_escape($order['plan_label']) ?></td>
                            <td><?= account_escape(commerce_money((int) $order['amount_cents'], (string) $order['currency'])) ?></td>
                            <td><span class="state <?= account_escape((string) $order['status']) ?>"><?= $order['status'] === 'paid' ? 'Paye' : account_escape($order['status']) ?></span></td>
                            <td><?php if ($plainCode !== ''): ?><code><?= account_escape($plainCode) ?></code><?php else: ?><?= account_escape($order['code_hint'] ?: 'Code indisponible') ?><?php endif; ?></td>
                            <td><?= $activeDevices ?> / <?= (int) ($order['max_devices'] ?? 1) ?><small><?= $activeDevices > 0 ? 'Active' : 'Non active' ?></small></td>
                            <td><?= account_escape($order['activation_expires_at'] ?: 'Debute a activation') ?></td>
                            <td class="table-actions"><?php if ($plainCode !== ''): ?><button class="mini-button" type="button" data-copy="<?= account_escape($plainCode) ?>">Copier</button><?php endif; ?><a class="mini-button" href="/activate/">Activer</a></td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php endif; ?>
    </section>
</main>
<?php endif; ?>

<footer class="account-footer"><p>&copy; <?= date('Y') ?> SmartVision. Lecteur IPTV sans contenu inclus.</p><div><a href="/">Accueil</a><a href="/activate/">Activation</a><a href="/download.php">Telecharger</a></div></footer>
<script src="/assets/account.js?v=2" defer></script>
</body>
</html>
