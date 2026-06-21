<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/api/helpers.php';
require_once dirname(__DIR__) . '/api/config.php';

session_start([
    'cookie_httponly' => true,
    'cookie_secure' => true,
    'cookie_samesite' => 'Lax',
]);

header('Content-Type: text/html; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
header('Referrer-Policy: strict-origin-when-cross-origin');
header("Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; frame-ancestors 'self'; base-uri 'self'; form-action 'self'");

$plans = [
    'month_1' => ['label' => 'Activation 1 mois', 'amount_cents' => 200, 'duration_days' => 30],
    'year_1' => ['label' => 'Activation 12 mois', 'amount_cents' => 1500, 'duration_days' => 365],
    'lifetime' => ['label' => 'Activation a vie', 'amount_cents' => 2000, 'duration_days' => 36500],
];

function h(string $value): string { return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'); }

function selected_plan(array $plans): string
{
    $plan = preg_replace('/[^a-z0-9_]/', '', (string) ($_REQUEST['plan'] ?? 'year_1'));
    return array_key_exists($plan, $plans) ? $plan : 'year_1';
}

function current_user(PDO $pdo): ?array
{
    $userId = (int) ($_SESSION['site_user_id'] ?? 0);
    if ($userId <= 0) return null;
    $statement = $pdo->prepare('SELECT id, email, display_name FROM site_users WHERE id = :id LIMIT 1');
    $statement->execute(['id' => $userId]);
    $user = $statement->fetch();
    return is_array($user) ? $user : null;
}

function csrf_token(): string
{
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(24));
    }
    return (string) $_SESSION['csrf_token'];
}

function assert_csrf(): void
{
    if (!hash_equals((string) ($_SESSION['csrf_token'] ?? ''), (string) ($_POST['csrf_token'] ?? ''))) {
        throw new RuntimeException('Session expiree. Rechargez la page.');
    }
}

function create_order_and_code(PDO $pdo, array $user, string $planKey, array $plan): string
{
    $pdo->beginTransaction();
    try {
        $insertOrder = $pdo->prepare(
            "INSERT INTO activation_orders (user_id, plan_key, plan_label, amount_cents, status, created_at)
             VALUES (:user_id, :plan_key, :plan_label, :amount_cents, 'pending', NOW())"
        );
        $insertOrder->execute([
            'user_id' => $user['id'],
            'plan_key' => $planKey,
            'plan_label' => $plan['label'],
            'amount_cents' => $plan['amount_cents'],
        ]);
        $orderId = (int) $pdo->lastInsertId();

        $activationCode = null;
        $codeId = null;
        $insertCode = $pdo->prepare(
            "INSERT INTO activation_codes (code_hash, label, duration_days, max_devices, used_devices, status, valid_until)
             VALUES (:code_hash, :label, :duration_days, 1, 0, 'active', NULL)"
        );
        for ($attempt = 0; $attempt < 12; $attempt++) {
            $candidate = generate_public_activation_code();
            try {
                $insertCode->execute([
                    'code_hash' => activation_code_hash($candidate),
                    'label' => 'Commande web #' . $orderId . ' - ' . $plan['label'],
                    'duration_days' => $plan['duration_days'],
                ]);
                $activationCode = $candidate;
                $codeId = (int) $pdo->lastInsertId();
                break;
            } catch (Throwable $exception) {
                if (!is_duplicate_key($exception)) {
                    throw $exception;
                }
            }
        }
        if ($activationCode === null || $codeId === null) {
            throw new RuntimeException('Generation du code impossible.');
        }

        $hint = substr($activationCode, 0, 8) . '...';
        $meta = $pdo->prepare(
            "INSERT INTO activation_code_metadata (code_id, code_hint, created_by, last_used_at)
             VALUES (:code_id, :hint, :created_by, NULL)"
        );
        $meta->execute([
            'code_id' => $codeId,
            'hint' => $hint,
            'created_by' => (string) $user['email'],
        ]);

        $updateOrder = $pdo->prepare(
            "UPDATE activation_orders
             SET status = 'paid', activation_code_id = :code_id, paid_at = NOW()
             WHERE id = :order_id"
        );
        $updateOrder->execute(['code_id' => $codeId, 'order_id' => $orderId]);
        $pdo->commit();
        $_SESSION['last_activation_code'] = $activationCode;
        return $activationCode;
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $exception;
    }
}

$pdo = db();
$message = '';
$error = '';
$planKey = selected_plan($plans);
$plan = $plans[$planKey];

try {
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        assert_csrf();
        $action = (string) ($_POST['action'] ?? '');
        $planKey = selected_plan($plans);
        $plan = $plans[$planKey];

        if ($action === 'logout') {
            $_SESSION = [];
            session_destroy();
            header('Location: /account/');
            exit;
        }

        if ($action === 'login' || $action === 'register') {
            $email = strtolower(trim((string) ($_POST['email'] ?? '')));
            $password = (string) ($_POST['password'] ?? '');
            if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($password) < 6) {
                throw new RuntimeException('Email invalide ou mot de passe trop court.');
            }

            if ($action === 'register') {
                $displayName = clean_optional_text($_POST['display_name'] ?? null, 120) ?: $email;
                $insert = $pdo->prepare(
                    "INSERT INTO site_users (email, display_name, password_hash, created_at)
                     VALUES (:email, :display_name, :password_hash, NOW())"
                );
                $insert->execute([
                    'email' => $email,
                    'display_name' => $displayName,
                    'password_hash' => password_hash($password, PASSWORD_DEFAULT),
                ]);
                $_SESSION['site_user_id'] = (int) $pdo->lastInsertId();
            } else {
                $query = $pdo->prepare('SELECT id, password_hash FROM site_users WHERE email = :email LIMIT 1');
                $query->execute(['email' => $email]);
                $found = $query->fetch();
                if (!$found || !password_verify($password, (string) $found['password_hash'])) {
                    throw new RuntimeException('Identifiants incorrects.');
                }
                $_SESSION['site_user_id'] = (int) $found['id'];
                $pdo->prepare('UPDATE site_users SET last_login_at = NOW() WHERE id = :id')
                    ->execute(['id' => $_SESSION['site_user_id']]);
            }
            header('Location: /account/?plan=' . rawurlencode($planKey));
            exit;
        }

        if ($action === 'fake_payment') {
            $user = current_user($pdo);
            if (!$user) throw new RuntimeException('Connectez-vous avant de commander.');
            $code = create_order_and_code($pdo, $user, $planKey, $plan);
            $message = 'Paiement fictif valide. Votre code SmartVision est pret.';
        }
    }
} catch (Throwable $exception) {
    $error = $exception->getMessage() ?: 'Operation impossible.';
}

$user = current_user($pdo);
$orders = [];
if ($user) {
    $query = $pdo->prepare(
        "SELECT orders.id, orders.plan_label, orders.amount_cents, orders.status, orders.created_at, metadata.code_hint
         FROM activation_orders orders
         LEFT JOIN activation_code_metadata metadata ON metadata.code_id = orders.activation_code_id
         WHERE orders.user_id = :user_id
         ORDER BY orders.id DESC
         LIMIT 20"
    );
    $query->execute(['user_id' => $user['id']]);
    $orders = $query->fetchAll();
}
$csrf = csrf_token();
$lastCode = (string) ($_SESSION['last_activation_code'] ?? '');
unset($_SESSION['last_activation_code']);
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Compte SmartVision</title>
    <link rel="stylesheet" href="/assets/site.css?v=1">
    <link rel="stylesheet" href="/assets/mobile.css?v=1">
</head>
<body>
<header class="site-header">
    <a class="brand" href="/"><img class="brand-mark" src="/assets/images/smartvision-mark.png" alt=""><img class="brand-wordmark" src="/assets/images/smartvision-wordmark.png" alt="SmartVision"></a>
    <nav><a href="/">Accueil</a><a href="/activate/">Activation</a><a href="/admin/">Admin</a></nav>
    <?php if ($user): ?><form method="post"><input type="hidden" name="csrf_token" value="<?= h($csrf) ?>"><button class="button button-outline header-cta" name="action" value="logout">Déconnexion</button></form><?php endif; ?>
</header>
<main class="activation-shell">
    <section class="activation-card" style="width:min(860px,100%)">
        <p class="eyebrow">Compte SmartVision</p>
        <h1>Commander une activation</h1>
        <?php if ($error !== ''): ?><p class="message" role="alert"><?= h($error) ?></p><?php endif; ?>
        <?php if ($message !== ''): ?><p class="message success-message"><?= h($message) ?></p><?php endif; ?>
        <?php if ($lastCode !== ''): ?>
            <div class="success-panel"><h2>Code généré</h2><p style="font-size:28px;letter-spacing:2px;font-weight:800"><?= h($lastCode) ?></p><p>Conservez ce code. Il active un appareil SmartVision.</p></div>
        <?php endif; ?>

        <?php if (!$user): ?>
            <div class="form-actions" style="align-items:start">
                <form method="post">
                    <input type="hidden" name="csrf_token" value="<?= h($csrf) ?>"><input type="hidden" name="plan" value="<?= h($planKey) ?>">
                    <h2>Créer un compte</h2>
                    <div class="field"><label>Nom</label><input name="display_name" autocomplete="name"></div>
                    <div class="field"><label>Email</label><input name="email" type="email" required autocomplete="email"></div>
                    <div class="field"><label>Mot de passe</label><input name="password" type="password" minlength="6" required autocomplete="new-password"></div>
                    <button class="button button-primary" name="action" value="register">Créer et continuer</button>
                </form>
                <form method="post">
                    <input type="hidden" name="csrf_token" value="<?= h($csrf) ?>"><input type="hidden" name="plan" value="<?= h($planKey) ?>">
                    <h2>Connexion</h2>
                    <div class="field"><label>Email</label><input name="email" type="email" required autocomplete="email"></div>
                    <div class="field"><label>Mot de passe</label><input name="password" type="password" minlength="6" required autocomplete="current-password"></div>
                    <button class="button button-outline" name="action" value="login">Se connecter</button>
                </form>
            </div>
        <?php else: ?>
            <p class="lead">Connecté avec <?= h((string) $user['email']) ?>. Paiement réel à connecter plus tard ; le bouton ci-dessous simule un paiement réussi et génère un code réel.</p>
            <form method="post">
                <input type="hidden" name="csrf_token" value="<?= h($csrf) ?>">
                <div class="price-grid" style="grid-template-columns:repeat(3,1fr);margin-top:22px">
                    <?php foreach ($plans as $key => $item): ?>
                        <label class="price-card<?= $key === $planKey ? ' featured' : '' ?>" style="min-height:190px">
                            <input type="radio" name="plan" value="<?= h($key) ?>"<?= $key === $planKey ? ' checked' : '' ?>>
                            <p class="plan-name"><?= h($item['label']) ?></p>
                            <p class="price"><strong><?= (int) ($item['amount_cents'] / 100) ?></strong><span>€</span></p>
                        </label>
                    <?php endforeach; ?>
                </div>
                <div class="field"><label>Carte test</label><input value="4242 4242 4242 4242" readonly></div>
                <button class="button button-primary" name="action" value="fake_payment">Valider le paiement fictif et générer le code</button>
            </form>
            <?php if ($orders): ?>
                <h2>Mes commandes</h2>
                <?php foreach ($orders as $order): ?>
                    <p class="xtream-note"><?= h((string) $order['plan_label']) ?> - <?= h((string) $order['status']) ?> - <?= h((string) ($order['code_hint'] ?? 'code masqué')) ?></p>
                <?php endforeach; ?>
            <?php endif; ?>
        <?php endif; ?>
    </section>
</main>
</body>
</html>
