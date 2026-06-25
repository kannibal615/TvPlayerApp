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
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self' https://pagead2.googlesyndication.com; connect-src 'self' https://pagead2.googlesyndication.com https://googleads.g.doubleclick.net https://tpc.googlesyndication.com; img-src 'self' data: https:; frame-src https://googleads.g.doubleclick.net https://tpc.googlesyndication.com; frame-ancestors 'self'; base-uri 'self'; form-action 'self'");

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
        throw new RuntimeException('Session expirée. Rechargez la page.');
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

function account_success_redirect(string $orderReference, string $planKey): never
{
    header('Location: /success/?order=' . rawurlencode($orderReference) . '&plan=' . rawurlencode($planKey), true, 303);
    exit;
}

function account_plan_ui(string $key): array
{
    $map = [
        'month_1' => [
            'title' => '1 mois',
            'price' => '2 € / mois',
            'payment' => 'Abonnement mensuel',
            'description' => 'Idéal pour tester SmartVision sans engagement.',
            'badge' => 'Sans engagement',
        ],
        'year_1' => [
            'title' => '12 mois',
            'price' => '15 €',
            'payment' => 'Paiement unique',
            'description' => 'Licence valable 12 mois pour un appareil.',
            'badge' => 'Le plus choisi',
            'featured' => true,
        ],
        'lifetime' => [
            'title' => 'À vie',
            'price' => '20 €',
            'payment' => 'Paiement unique',
            'description' => 'Licence permanente pour un appareil.',
            'badge' => 'Meilleure valeur',
        ],
    ];

    return $map[$key] ?? $map['year_1'];
}

function account_status_label(string $status): string
{
    return match ($status) {
        'paid' => 'Payée',
        'pending' => 'En attente',
        'cancelled' => 'Annulée',
        'active' => 'Active',
        'disabled' => 'Désactivée',
        'expired' => 'Expirée',
        default => $status,
    };
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
                throw new RuntimeException('Le mot de passe doit contenir au moins 8 caractères.');
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
                    throw new RuntimeException('Un compte existe déjà avec cette adresse. Connectez-vous.');
                }
                throw $exception;
            }

            session_regenerate_id(true);
            $_SESSION['site_user_id'] = (int) $pdo->lastInsertId();
            account_flash('success', 'Compte créé. Choisissez votre licence puis confirmez la commande.');
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
                throw new RuntimeException('Ce compte est temporairement bloqué. Contactez le support.');
            }

            session_regenerate_id(true);
            $_SESSION['site_user_id'] = (int) $found['id'];
            $pdo->prepare('UPDATE site_users SET last_login_at = NOW(), updated_at = NOW() WHERE id = :id')
                ->execute(['id' => $_SESSION['site_user_id']]);
            account_flash('success', 'Connexion réussie.');
            account_redirect($planKey);
        }

        if ($action === 'test_payment') {
            $user = account_current_user($pdo);
            if ($user === null) {
                throw new RuntimeException('Connectez-vous pour commander.');
            }
            if (($_POST['accept_terms'] ?? '') !== '1') {
                throw new RuntimeException('Confirmez que la licence concerne uniquement le lecteur SmartVision.');
            }
            $checkoutToken = (string) ($_POST['checkout_token'] ?? '');
            if (!hash_equals((string) ($_SESSION['checkout_token'] ?? ''), $checkoutToken)) {
                throw new RuntimeException('Cette commande a expiré. Rechargez la page.');
            }

            $order = commerce_create_test_order($pdo, (int) $user['id'], $planKey, $checkoutToken);
            $_SESSION['checkout_token'] = commerce_checkout_token();
            account_flash('success', 'Paiement confirmé. Votre code SmartVision est disponible.');
            account_success_redirect((string) $order['order_reference'], $planKey);
        }
    }
} catch (Throwable $exception) {
    error_log('SmartVision customer action failed.');
    $error = $exception->getMessage() ?: 'Opération impossible. Réessayez.';
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
$selectedUi = account_plan_ui($planKey);
$manifest = sv_apk_manifest();
$versionName = trim((string) ($manifest['latest_version_name'] ?? $manifest['version_name'] ?? 'Dernière version'));
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Gérez vos licences, commandes et activations SmartVision.">
    <title><?= $user ? 'Mon compte' : 'Créer mon compte' ?> | SmartVision</title>
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3376574358352765" crossorigin="anonymous"></script>
    <link rel="stylesheet" href="/assets/site.css?v=5">
    <link rel="stylesheet" href="/assets/site-overrides.css?v=5">
    <link rel="stylesheet" href="/assets/account.css?v=5">
    <link rel="stylesheet" href="/assets/mobile.css?v=5">
</head>
<body class="account-page">
<?php sv_render_site_header(); ?>

<?php if (!$user): ?>
<main class="auth-shell">
    <section class="auth-hero-panel">
        <h1>Achetez une licence, puis activez votre TV.</h1>
        <p>Créez votre espace client pour retrouver vos codes SmartVision, gérer vos commandes et activer vos appareils Android TV.</p>
        <div class="auth-legal-box">
            <strong>Licence logiciel uniquement</strong>
            <span>SmartVision ne fournit aucun contenu audiovisuel, chaîne, film, série ou playlist.</span>
        </div>
    </section>

    <section class="auth-form-panel">
        <h2>Créer mon compte</h2>
        <p>Votre licence restera accessible ici après la commande.</p>
        <?php if ($error): ?><div class="form-notice error" role="alert"><?= account_escape($error) ?></div><?php endif; ?>
        <form method="post" class="stack-form">
            <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
            <input type="hidden" name="plan" value="<?= account_escape($planKey) ?>">
            <div class="field"><label for="register-name">Nom</label><input id="register-name" name="display_name" autocomplete="name" required></div>
            <div class="field"><label for="register-email">Email</label><input id="register-email" name="email" type="email" autocomplete="email" required></div>
            <div class="field"><label for="register-password">Mot de passe</label><input id="register-password" name="password" type="password" minlength="8" autocomplete="new-password" required><small>8 caractères minimum.</small></div>
            <button class="button button-primary" name="action" value="register">Créer mon compte et continuer</button>
        </form>
        <details class="login-disclosure"<?= ($_GET['mode'] ?? '') === 'login' ? ' open' : '' ?>>
            <summary>J’ai déjà un compte</summary>
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
    <section class="dashboard-hero">
        <div>
            <h1>Bonjour <?= account_escape($user['display_name'] ?: $user['email']) ?></h1>
            <p>Gérez vos licences, activez vos appareils et téléchargez la dernière version de SmartVision.</p>
        </div>
        <div class="dashboard-quick-actions">
            <a href="#activate-tv"><span class="quick-icon screen"></span>Activer une TV</a>
            <a href="/download.php"><span class="quick-icon download"></span>Télécharger l’APK</a>
            <a href="#buy-license"><span class="quick-icon cart"></span>Acheter une licence</a>
        </div>
    </section>

    <?php if ($flash): ?><div class="form-notice <?= account_escape($flash['type'] ?? '') ?>"><?= account_escape($flash['message'] ?? '') ?></div><?php endif; ?>
    <?php if ($error): ?><div class="form-notice error" role="alert"><?= account_escape($error) ?></div><?php endif; ?>

    <section class="customer-stats" aria-label="Résumé du compte">
        <div><span>Commandes</span><strong><?= (int) $summary['orders'] ?></strong></div>
        <div><span>Licences</span><strong><?= (int) $summary['licenses'] ?></strong></div>
        <div><span>Licences actives</span><strong><?= (int) $summary['active_licenses'] ?></strong></div>
        <div><span>Appareils activés</span><strong><?= (int) $summary['devices'] ?></strong></div>
    </section>

    <section class="dashboard-shell">
        <aside class="dashboard-sidebar" aria-label="Navigation espace client">
            <a href="#licenses" class="active">Mes licences</a>
            <a href="#activate-tv">Activer une TV</a>
            <a href="#orders">Commandes</a>
            <a href="#download">Téléchargement</a>
            <a href="#account">Compte</a>
            <div class="legal-mini">
                <strong>Lecteur légal</strong>
                <span>SmartVision ne vend aucun contenu. Vous utilisez vos propres sources autorisées.</span>
                <a href="/legal-iptv-player/">En savoir plus</a>
            </div>
        </aside>

        <div class="dashboard-content">
            <nav class="dashboard-tabs" aria-label="Sections du compte">
                <a href="#licenses">Mes licences</a>
                <a href="#activate-tv">Activer une TV</a>
                <a href="#orders">Commandes</a>
                <a href="#download">Téléchargement</a>
                <a href="#account">Compte</a>
            </nav>

            <section class="dashboard-panel" id="licenses">
                <div class="panel-heading">
                    <div>
                        <h2>Mes licences</h2>
                        <p>Vos codes restent disponibles dans votre compte.</p>
                    </div>
                    <a class="button button-outline slim-button" href="#buy-license">Acheter une licence</a>
                </div>

                <?php if ($orders === []): ?>
                    <div class="empty-state license-empty">
                        <strong>Aucune licence pour le moment.</strong>
                        <span>Achetez une licence pour activer votre appareil SmartVision.</span>
                        <a class="button button-primary" href="#buy-license">Acheter une licence</a>
                    </div>
                <?php else: ?>
                    <div class="license-card-grid">
                        <?php foreach ($orders as $order):
                            $plainCode = (string) ($order['activation_code'] ?? '');
                            $activeDevices = (int) ($order['active_devices'] ?? 0);
                            $isHighlighted = $highlightOrder !== '' && hash_equals((string) $order['order_reference'], $highlightOrder);
                        ?>
                            <article class="license-card<?= $isHighlighted ? ' highlight-order' : '' ?>">
                                <div class="license-card-head">
                                    <div>
                                        <span><?= account_escape($order['plan_label']) ?></span>
                                        <strong><?= account_escape(account_status_label((string) $order['status'])) ?></strong>
                                    </div>
                                    <span class="state <?= account_escape((string) $order['status']) ?>"><?= account_escape(account_status_label((string) $order['status'])) ?></span>
                                </div>
                                <dl>
                                    <div><dt>Code licence</dt><dd><?php if ($plainCode !== ''): ?><code><?= account_escape($plainCode) ?></code><?php else: ?><?= account_escape($order['code_hint'] ?: 'Indisponible') ?><?php endif; ?></dd></div>
                                    <div><dt>Achat</dt><dd><?= account_escape((string) $order['created_at']) ?></dd></div>
                                    <div><dt>Expiration</dt><dd><?= account_escape($order['activation_expires_at'] ?: 'Débute à l’activation') ?></dd></div>
                                    <div><dt>Appareil lié</dt><dd><?= $activeDevices ?> / <?= (int) ($order['max_devices'] ?? 1) ?></dd></div>
                                </dl>
                                <div class="license-actions">
                                    <?php if ($plainCode !== ''): ?><button class="mini-button" type="button" data-copy="<?= account_escape($plainCode) ?>">Copier code</button><?php endif; ?>
                                    <a class="mini-button" href="/activate/">Activer une TV</a>
                                </div>
                            </article>
                        <?php endforeach; ?>
                    </div>
                <?php endif; ?>
            </section>

            <section class="dashboard-panel" id="buy-license">
                <div class="panel-heading">
                    <div>
                        <h2>Choisissez votre licence</h2>
                        <p>Une activation correspond à un seul appareil. Les paiements uniques ne sont pas des abonnements.</p>
                    </div>
                </div>
                <form method="post" class="checkout-layout" id="checkout-form">
                    <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
                    <input type="hidden" name="checkout_token" value="<?= account_escape($checkoutToken) ?>">
                    <section class="plan-selector">
                        <div class="account-plan-grid">
                            <?php foreach ($plans as $key => $plan):
                                $ui = account_plan_ui((string) $key);
                            ?>
                                <label class="account-plan<?= $key === $planKey ? ' selected' : '' ?><?= !empty($ui['featured']) ? ' featured' : '' ?>">
                                    <input type="radio" name="plan" value="<?= account_escape($key) ?>" data-plan-label="<?= account_escape($ui['title']) ?>" data-plan-price="<?= account_escape($ui['price']) ?>"<?= $key === $planKey ? ' checked' : '' ?>>
                                    <?php if (!empty($ui['badge'])): ?><span class="plan-recommended"><?= account_escape($ui['badge']) ?></span><?php endif; ?>
                                    <strong><?= account_escape($ui['title']) ?></strong>
                                    <div class="account-plan-price"><?= account_escape($ui['price']) ?></div>
                                    <p><b><?= account_escape($ui['payment']) ?></b><br><?= account_escape($ui['description']) ?></p>
                                    <ul><li>1 appareil</li><li>Mises à jour incluses</li><li>Vos propres sources autorisées</li><li>Aucun contenu fourni</li></ul>
                                </label>
                            <?php endforeach; ?>
                        </div>
                        <div class="license-rule"><strong>Important</strong><span>SmartVision est un lecteur. La licence ne fournit aucune chaîne, film, série, playlist ou contenu tiers.</span></div>
                    </section>

                    <aside class="order-summary">
                        <h2>Votre commande</h2>
                        <dl>
                            <div><dt>Licence</dt><dd id="summary-plan"><?= account_escape($selectedUi['title']) ?></dd></div>
                            <div><dt>Paiement</dt><dd><?= account_escape($selectedUi['payment']) ?></dd></div>
                            <div><dt>Appareils</dt><dd>1</dd></div>
                        </dl>
                        <div class="order-total"><span>Total</span><strong id="summary-price"><?= account_escape($selectedUi['price']) ?></strong></div>
                        <div class="test-payment-note"><strong>Paiement de test</strong><span>Aucun montant réel ne sera débité. Le code généré est utilisable.</span></div>
                        <label class="terms-check"><input type="checkbox" name="accept_terms" value="1" required><span>Je confirme acheter une licence du lecteur SmartVision, sans contenu inclus.</span></label>
                        <button class="button button-primary" name="action" value="test_payment">Valider le paiement test</button>
                        <small class="secure-note">Prix et durée vérifiés côté serveur. Une seule licence sera générée.</small>
                    </aside>
                </form>
            </section>

            <section class="dashboard-panel split-panel" id="activate-tv">
                <div>
                    <h2>Activer mon appareil</h2>
                    <ol class="activation-mini-steps">
                        <li>Ouvrez SmartVision sur votre Android TV.</li>
                        <li>Notez le code affiché à l’écran.</li>
                        <li>Saisissez ce code ici.</li>
                        <li>Validez l’activation avec votre licence.</li>
                    </ol>
                </div>
                <form action="/activate/" method="get" class="activation-mini-form">
                    <div class="field">
                        <label for="dashboard-tv-code">Code appareil</label>
                        <input id="dashboard-tv-code" name="code" type="text" minlength="6" maxlength="20" autocomplete="one-time-code" placeholder="ABCD-EFGH">
                        <small>Le code appareil est visible sur l’écran d’activation de l’application.</small>
                    </div>
                    <button class="button button-primary" type="submit">Activer cet appareil</button>
                </form>
            </section>

            <section class="dashboard-panel" id="orders">
                <div class="panel-heading">
                    <div><h2>Commandes</h2><p>Historique de vos achats SmartVision.</p></div>
                </div>
                <?php if ($orders === []): ?>
                    <div class="empty-state"><strong>Aucune commande pour le moment.</strong><span>Votre première commande apparaîtra ici après paiement.</span></div>
                <?php else: ?>
                    <div class="customer-table-wrap">
                        <table class="customer-table">
                            <thead><tr><th>Commande</th><th>Licence</th><th>Montant</th><th>Paiement</th><th>Code SmartVision</th><th>Appareil</th><th>Expiration</th><th>Actions</th></tr></thead>
                            <tbody>
                            <?php foreach ($orders as $order):
                                $plainCode = (string) ($order['activation_code'] ?? '');
                                $activeDevices = (int) ($order['active_devices'] ?? 0);
                            ?>
                                <tr>
                                    <td><strong><?= account_escape($order['order_reference'] ?: 'SV-' . $order['id']) ?></strong><small><?= account_escape((string) $order['created_at']) ?></small></td>
                                    <td><?= account_escape($order['plan_label']) ?></td>
                                    <td><?= account_escape(commerce_money((int) $order['amount_cents'], (string) $order['currency'])) ?></td>
                                    <td><span class="state <?= account_escape((string) $order['status']) ?>"><?= account_escape(account_status_label((string) $order['status'])) ?></span></td>
                                    <td><?php if ($plainCode !== ''): ?><code><?= account_escape($plainCode) ?></code><?php else: ?><?= account_escape($order['code_hint'] ?: 'Code indisponible') ?><?php endif; ?></td>
                                    <td><?= $activeDevices ?> / <?= (int) ($order['max_devices'] ?? 1) ?><small><?= $activeDevices > 0 ? 'Activé' : 'Non activé' ?></small></td>
                                    <td><?= account_escape($order['activation_expires_at'] ?: 'Débute à l’activation') ?></td>
                                    <td class="table-actions"><?php if ($plainCode !== ''): ?><button class="mini-button" type="button" data-copy="<?= account_escape($plainCode) ?>">Copier</button><?php endif; ?><a class="mini-button" href="/activate/">Activer</a></td>
                                </tr>
                            <?php endforeach; ?>
                            </tbody>
                        </table>
                    </div>
                <?php endif; ?>
            </section>

            <section class="dashboard-panel split-panel" id="download">
                <div>
                    <h2>Téléchargement</h2>
                    <p>Installez la dernière version officielle de SmartVision sur Android TV, box Android ou appareil compatible.</p>
                    <div class="download-chips"><span>Android TV</span><span>APK officiel</span><span>Installation manuelle</span></div>
                </div>
                <div class="download-card">
                    <strong>SmartVision Android TV</strong>
                    <span>Version actuelle : <?= account_escape($versionName) ?></span>
                    <a class="button button-primary" href="/download.php">Télécharger l’APK</a>
                    <a class="button button-outline" href="/contact/">Guide d’installation</a>
                </div>
            </section>

            <section class="dashboard-panel split-panel" id="account">
                <div>
                    <h2>Compte</h2>
                    <dl class="account-details">
                        <div><dt>Email</dt><dd><?= account_escape($user['email']) ?></dd></div>
                        <div><dt>Nom</dt><dd><?= account_escape($user['display_name'] ?: '-') ?></dd></div>
                        <div><dt>Statut</dt><dd><?= account_escape(account_status_label((string) $user['status'])) ?></dd></div>
                        <div><dt>Créé le</dt><dd><?= account_escape((string) $user['created_at']) ?></dd></div>
                    </dl>
                </div>
                <form method="post" class="logout-card">
                    <input type="hidden" name="csrf_token" value="<?= account_escape($csrf) ?>">
                    <strong>Session</strong>
                    <span>Déconnectez-vous de cet espace client sur cet appareil.</span>
                    <button class="button button-outline" name="action" value="logout">Déconnexion</button>
                </form>
            </section>
        </div>
    </section>
</main>
<?php endif; ?>

<?php sv_render_site_footer('account-footer'); ?>
<script src="/assets/account.js?v=5" defer></script>
</body>
</html>
