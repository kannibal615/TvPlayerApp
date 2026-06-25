<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

function commerce_plans(): array
{
    $plans = [
        'month_1' => [
            'label' => '1 mois',
            'full_label' => 'Activation 1 mois',
            'amount_cents' => 200,
            'duration_days' => 30,
            'description' => 'Pour tester SmartVision sans engagement.',
        ],
        'year_1' => [
            'label' => '12 mois',
            'full_label' => 'Activation 12 mois',
            'amount_cents' => 1500,
            'duration_days' => 365,
            'description' => 'Le meilleur rapport qualite/prix.',
            'recommended' => true,
        ],
        'lifetime' => [
            'label' => 'A vie',
            'full_label' => 'Activation a vie',
            'amount_cents' => 2000,
            'duration_days' => 36500,
            'description' => 'Un paiement unique pour cet appareil.',
        ],
    ];

    if ((string) getenv('SMARTVISION_ENV') === 'development') {
        $plans['simulation'] = [
            'label' => 'Simulation DEV',
            'full_label' => 'TEST DEV - Abonnement simulation',
            'amount_cents' => 0,
            'duration_days' => 1,
            'description' => 'Simule un paiement Gammal Tech et son callback.',
            'simulation' => true,
        ];
    }

    return $plans;
}

function commerce_payment_plans(): array
{
    return array_filter(
        commerce_plans(),
        static fn(array $plan): bool => empty($plan['simulation']),
    );
}

function commerce_plan(string $planKey): array
{
    $plans = commerce_plans();
    return $plans[$planKey] ?? $plans['year_1'];
}

function commerce_plan_key(mixed $value): string
{
    $planKey = preg_replace('/[^a-z0-9_]/', '', strtolower(trim((string) $value)));
    return array_key_exists((string) $planKey, commerce_plans()) ? (string) $planKey : 'year_1';
}

function commerce_payment_setting_key(string $planKey, string $suffix): string
{
    return 'gammal_payment_' . commerce_plan_key($planKey) . '_' . $suffix;
}

function commerce_payment_url(PDO $pdo, string $planKey): string
{
    $url = trim((string) get_setting(
        $pdo,
        commerce_payment_setting_key($planKey, 'url'),
        '',
    ));
    $enabled = (string) get_setting(
        $pdo,
        commerce_payment_setting_key($planKey, 'enabled'),
        '0',
    ) === '1';

    return $enabled && commerce_is_valid_gammal_payment_url($url) ? $url : '';
}

function commerce_ensure_payment_schema(PDO $pdo): void
{
    static $ensured = [];
    $connectionId = spl_object_id($pdo);
    if (isset($ensured[$connectionId])) {
        return;
    }
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS commerce_order_intents (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            token_hash CHAR(64) NOT NULL,
            plan_key VARCHAR(40) NOT NULL,
            plan_label VARCHAR(80) NOT NULL,
            amount_cents INT UNSIGNED NOT NULL,
            merchant_amount_cents INT UNSIGNED NOT NULL,
            currency CHAR(3) NOT NULL DEFAULT 'EUR',
            status ENUM('started', 'callback_received', 'approved', 'rejected', 'cancelled', 'expired') NOT NULL DEFAULT 'started',
            checkout_url TEXT NULL,
            raw_payload JSON NULL,
            expires_at DATETIME NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_commerce_order_intents_token (token_hash),
            INDEX idx_commerce_order_intents_user (user_id, status),
            INDEX idx_commerce_order_intents_expiry (expires_at),
            CONSTRAINT fk_commerce_order_intents_user FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS commerce_payments (
            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            intent_id BIGINT UNSIGNED NOT NULL,
            user_id INT NOT NULL,
            activation_order_id INT NULL,
            txn VARCHAR(190) NOT NULL,
            gateway VARCHAR(40) NOT NULL DEFAULT 'gammal',
            gateway_status INT NOT NULL,
            verification_status ENUM('pending_review', 'approved', 'rejected') NOT NULL DEFAULT 'pending_review',
            reported_amount_cents INT UNSIGNED NOT NULL,
            expected_amount_cents INT UNSIGNED NOT NULL,
            currency CHAR(3) NOT NULL DEFAULT 'EUR',
            raw_payload JSON NOT NULL,
            reviewed_by VARCHAR(100) NULL,
            reviewed_at DATETIME NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_commerce_payments_txn (txn),
            UNIQUE KEY uq_commerce_payments_intent (intent_id),
            INDEX idx_commerce_payments_review (verification_status, created_at),
            CONSTRAINT fk_commerce_payments_intent FOREIGN KEY (intent_id) REFERENCES commerce_order_intents(id) ON DELETE RESTRICT,
            CONSTRAINT fk_commerce_payments_user FOREIGN KEY (user_id) REFERENCES site_users(id) ON DELETE CASCADE,
            CONSTRAINT fk_commerce_payments_order FOREIGN KEY (activation_order_id) REFERENCES activation_orders(id) ON DELETE SET NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $ensured[$connectionId] = true;
}

function commerce_create_payment_intent(PDO $pdo, int $userId, string $planKey): array
{
    commerce_ensure_payment_schema($pdo);
    $planKey = commerce_plan_key($planKey);
    $plan = commerce_plan($planKey);
    if (!empty($plan['simulation'])) {
        throw new InvalidArgumentException('Utilisez le parcours de simulation pour ce pack.');
    }
    $configuredUrl = commerce_payment_url($pdo, $planKey);
    if ($configuredUrl === '') {
        throw new RuntimeException('Le paiement de cette offre est temporairement indisponible.');
    }

    $parts = parse_url($configuredUrl);
    parse_str((string) ($parts['query'] ?? ''), $query);
    $merchantAmount = filter_var($query['amount'] ?? null, FILTER_VALIDATE_FLOAT);
    $currency = strtoupper(trim((string) ($query['currency'] ?? 'EUR')));
    if ($merchantAmount === false || $merchantAmount <= 0 || !preg_match('/^[A-Z]{3}$/', $currency)) {
        throw new RuntimeException('Le lien de paiement configure contient un montant ou une devise invalide.');
    }

    $token = bin2hex(random_bytes(32));
    $tokenHash = hash('sha256', $token);
    $callbackUrl = smartvision_public_base_url() . '/payment-callback/?intent=' . rawurlencode($token);
    $query['callback'] = $callbackUrl;
    $checkoutUrl = ($parts['scheme'] ?? 'https') . '://' . ($parts['host'] ?? 'api.gammal.tech')
        . (isset($parts['port']) ? ':' . (int) $parts['port'] : '')
        . ($parts['path'] ?? '/sdk/pay/link/') . '?' . http_build_query($query, '', '&', PHP_QUERY_RFC3986);

    $statement = $pdo->prepare(
        "INSERT INTO commerce_order_intents
            (user_id, token_hash, plan_key, plan_label, amount_cents, merchant_amount_cents,
             currency, status, checkout_url, raw_payload, expires_at)
         VALUES
            (:user_id, :token_hash, :plan_key, :plan_label, :amount_cents, :merchant_amount_cents,
             :currency, 'started', :checkout_url, :raw_payload, DATE_ADD(NOW(), INTERVAL 2 HOUR))"
    );
    $statement->execute([
        'user_id' => $userId,
        'token_hash' => $tokenHash,
        'plan_key' => $planKey,
        'plan_label' => (string) $plan['full_label'],
        'amount_cents' => (int) $plan['amount_cents'],
        'merchant_amount_cents' => (int) round((float) $merchantAmount * 100),
        'currency' => $currency,
        'checkout_url' => $checkoutUrl,
        'raw_payload' => json_encode(['configured_url' => $configuredUrl], JSON_UNESCAPED_SLASHES),
    ]);

    return [
        'id' => (int) $pdo->lastInsertId(),
        'token' => $token,
        'checkout_url' => $checkoutUrl,
    ];
}

function commerce_load_payment_intent(PDO $pdo, string $token, bool $forUpdate = false): ?array
{
    if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
        return null;
    }
    commerce_ensure_payment_schema($pdo);
    $statement = $pdo->prepare(
        "SELECT i.*, u.email, u.display_name
         FROM commerce_order_intents i
         JOIN site_users u ON u.id = i.user_id
         WHERE i.token_hash = :token_hash
         LIMIT 1" . ($forUpdate ? ' FOR UPDATE' : '')
    );
    $statement->execute(['token_hash' => hash('sha256', $token)]);
    $intent = $statement->fetch();
    return is_array($intent) ? $intent : null;
}

function commerce_record_gammal_callback(PDO $pdo, string $token, array $payment): array
{
    commerce_ensure_payment_schema($pdo);
    $txn = smartvision_text_substr(trim((string) ($payment['txn'] ?? '')), 0, 190);
    $gatewayStatus = (int) ($payment['status'] ?? 0);
    $amount = filter_var($payment['amount'] ?? null, FILTER_VALIDATE_FLOAT);
    $currency = strtoupper(trim((string) ($payment['currency'] ?? '')));
    if ($txn === '' || !in_array($gatewayStatus, [1, 2], true) || $amount === false || $amount < 0) {
        throw new InvalidArgumentException('Retour de paiement Gammal incomplet.');
    }

    $reportedAmountCents = (int) round((float) $amount * 100);
    $lockName = 'sv_payment_' . substr(hash('sha256', $txn), 0, 48);
    $lockStatement = $pdo->prepare('SELECT GET_LOCK(:lock_name, 10)');
    $lockStatement->execute(['lock_name' => $lockName]);
    if ((int) $lockStatement->fetchColumn() !== 1) {
        throw new RuntimeException('La transaction est deja en cours de traitement.');
    }

    try {
        $existing = $pdo->prepare(
            "SELECT p.*, i.plan_key, i.plan_label, i.amount_cents, u.email, u.display_name,
                    o.order_reference
             FROM commerce_payments p
             JOIN commerce_order_intents i ON i.id = p.intent_id
             JOIN site_users u ON u.id = p.user_id
             LEFT JOIN activation_orders o ON o.id = p.activation_order_id
             WHERE p.txn = :txn LIMIT 1"
        );
        $existing->execute(['txn' => $txn]);
        $existingPayment = $existing->fetch();
        if (is_array($existingPayment)) {
            $existingPayment['duplicate'] = true;
            return $existingPayment;
        }

        $pdo->beginTransaction();
        $intent = commerce_load_payment_intent($pdo, $token, true);
        if ($intent === null || strtotime((string) $intent['expires_at']) < time()) {
            throw new RuntimeException('Intention de commande absente ou expiree.');
        }
        if (!in_array((string) $intent['status'], ['started', 'callback_received'], true)) {
            throw new RuntimeException('Cette intention de commande est deja finalisee.');
        }
        if ($reportedAmountCents !== (int) $intent['merchant_amount_cents']) {
            throw new RuntimeException('Le montant retourne par Gammal ne correspond pas a la commande.');
        }
        if ($currency !== strtoupper((string) $intent['currency'])) {
            throw new RuntimeException('La devise retournee par Gammal ne correspond pas a la commande.');
        }

        $orderReference = commerce_order_reference();
        $insertOrder = $pdo->prepare(
            "INSERT INTO activation_orders
                (user_id, order_reference, plan_key, plan_label, amount_cents, currency,
                 status, payment_provider, payment_reference, checkout_token_hash, created_at, updated_at)
             VALUES
                (:user_id, :order_reference, :plan_key, :plan_label, :amount_cents, :currency,
                 'pending', 'gammal', :payment_reference, :checkout_token_hash, NOW(), NOW())"
        );
        $insertOrder->execute([
            'user_id' => (int) $intent['user_id'],
            'order_reference' => $orderReference,
            'plan_key' => (string) $intent['plan_key'],
            'plan_label' => (string) $intent['plan_label'],
            'amount_cents' => (int) $intent['amount_cents'],
            'currency' => (string) $intent['currency'],
            'payment_reference' => $txn,
            'checkout_token_hash' => (string) $intent['token_hash'],
        ]);
        $orderId = (int) $pdo->lastInsertId();

        $insertPayment = $pdo->prepare(
            "INSERT INTO commerce_payments
                (intent_id, user_id, activation_order_id, txn, gateway_status,
                 verification_status, reported_amount_cents, expected_amount_cents, currency, raw_payload)
             VALUES
                (:intent_id, :user_id, :activation_order_id, :txn, :gateway_status,
                 'pending_review', :reported_amount_cents, :expected_amount_cents, :currency, :raw_payload)"
        );
        $insertPayment->execute([
            'intent_id' => (int) $intent['id'],
            'user_id' => (int) $intent['user_id'],
            'activation_order_id' => $orderId,
            'txn' => $txn,
            'gateway_status' => $gatewayStatus,
            'reported_amount_cents' => $reportedAmountCents,
            'expected_amount_cents' => (int) $intent['merchant_amount_cents'],
            'currency' => $currency,
            'raw_payload' => json_encode($payment, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        ]);
        $paymentId = (int) $pdo->lastInsertId();
        $pdo->prepare(
            "UPDATE commerce_order_intents SET status = 'callback_received', raw_payload = :raw_payload, updated_at = NOW()
             WHERE id = :id"
        )->execute([
            'raw_payload' => json_encode($payment, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            'id' => (int) $intent['id'],
        ]);
        $pdo->commit();

        return [
            'id' => $paymentId,
            'intent_id' => (int) $intent['id'],
            'user_id' => (int) $intent['user_id'],
            'activation_order_id' => $orderId,
            'txn' => $txn,
            'verification_status' => 'pending_review',
            'plan_key' => (string) $intent['plan_key'],
            'plan_label' => (string) $intent['plan_label'],
            'amount_cents' => (int) $intent['amount_cents'],
            'currency' => $currency,
            'email' => (string) $intent['email'],
            'display_name' => (string) ($intent['display_name'] ?? ''),
            'order_reference' => $orderReference,
            'duplicate' => false,
        ];
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $exception;
    } finally {
        $release = $pdo->prepare('SELECT RELEASE_LOCK(:lock_name)');
        $release->execute(['lock_name' => $lockName]);
    }
}

function commerce_approve_gammal_payment(PDO $pdo, int $paymentId, string $reviewedBy): array
{
    commerce_ensure_payment_schema($pdo);
    $pdo->beginTransaction();
    try {
        $statement = $pdo->prepare(
            "SELECT p.*, i.plan_key, i.plan_label, i.amount_cents, i.currency,
                    u.email, u.display_name, o.order_reference, o.status AS order_status
             FROM commerce_payments p
             JOIN commerce_order_intents i ON i.id = p.intent_id
             JOIN site_users u ON u.id = p.user_id
             JOIN activation_orders o ON o.id = p.activation_order_id
             WHERE p.id = :id LIMIT 1 FOR UPDATE"
        );
        $statement->execute(['id' => $paymentId]);
        $payment = $statement->fetch();
        if (!is_array($payment)) {
            throw new InvalidArgumentException('Paiement introuvable.');
        }
        if ($payment['verification_status'] === 'approved' && $payment['order_status'] === 'paid') {
            $pdo->commit();
            return $payment;
        }
        if ($payment['verification_status'] !== 'pending_review' || $payment['order_status'] !== 'pending') {
            throw new InvalidArgumentException('Ce paiement ne peut pas etre approuve.');
        }

        $plan = commerce_plan((string) $payment['plan_key']);
        $createdCode = create_activation_code_record(
            $pdo,
            (string) $payment['order_reference'] . ' - ' . (string) $payment['plan_label'],
            (int) $plan['duration_days'],
            1,
            'paid',
            null,
            'customer:' . (int) $payment['user_id'],
        );
        $pdo->prepare(
            "UPDATE activation_orders
             SET status = 'paid', activation_code_id = :code_id,
                 activation_code_ciphertext = :code_ciphertext, paid_at = NOW(), updated_at = NOW()
             WHERE id = :order_id AND status = 'pending'"
        )->execute([
            'code_id' => (int) $createdCode['id'],
            'code_ciphertext' => encrypt_private_value((string) $createdCode['code']),
            'order_id' => (int) $payment['activation_order_id'],
        ]);
        $pdo->prepare(
            "UPDATE commerce_payments
             SET verification_status = 'approved', reviewed_by = :reviewed_by, reviewed_at = NOW(), updated_at = NOW()
             WHERE id = :id"
        )->execute(['reviewed_by' => $reviewedBy, 'id' => $paymentId]);
        $pdo->prepare(
            "UPDATE commerce_order_intents SET status = 'approved', updated_at = NOW() WHERE id = :id"
        )->execute(['id' => (int) $payment['intent_id']]);
        $pdo->commit();
        $payment['verification_status'] = 'approved';
        $payment['order_status'] = 'paid';
        $payment['activation_code'] = (string) $createdCode['code'];
        return $payment;
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $exception;
    }
}

function commerce_reject_gammal_payment(PDO $pdo, int $paymentId, string $reviewedBy): void
{
    commerce_ensure_payment_schema($pdo);
    $pdo->beginTransaction();
    try {
        $statement = $pdo->prepare(
            "SELECT intent_id, activation_order_id, verification_status
             FROM commerce_payments WHERE id = :id LIMIT 1 FOR UPDATE"
        );
        $statement->execute(['id' => $paymentId]);
        $payment = $statement->fetch();
        if (!is_array($payment) || $payment['verification_status'] !== 'pending_review') {
            throw new InvalidArgumentException('Ce paiement ne peut pas etre rejete.');
        }
        $pdo->prepare(
            "UPDATE commerce_payments
             SET verification_status = 'rejected', reviewed_by = :reviewed_by, reviewed_at = NOW(), updated_at = NOW()
             WHERE id = :id"
        )->execute(['reviewed_by' => $reviewedBy, 'id' => $paymentId]);
        $pdo->prepare(
            "UPDATE commerce_order_intents SET status = 'rejected', updated_at = NOW() WHERE id = :id"
        )->execute(['id' => (int) $payment['intent_id']]);
        $pdo->prepare(
            "UPDATE activation_orders SET status = 'cancelled', updated_at = NOW()
             WHERE id = :id AND status = 'pending'"
        )->execute(['id' => (int) $payment['activation_order_id']]);
        $pdo->commit();
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $exception;
    }
}

function commerce_load_gammal_payments(PDO $pdo, int $limit = 80): array
{
    commerce_ensure_payment_schema($pdo);
    $limit = max(1, min(200, $limit));
    return $pdo->query(
        "SELECT p.id, p.txn, p.gateway_status, p.verification_status,
                p.reported_amount_cents, p.expected_amount_cents, p.currency,
                p.reviewed_by, p.reviewed_at, p.created_at,
                i.plan_label, i.amount_cents, o.order_reference, o.status AS order_status,
                u.email, u.display_name
         FROM commerce_payments p
         JOIN commerce_order_intents i ON i.id = p.intent_id
         JOIN site_users u ON u.id = p.user_id
         LEFT JOIN activation_orders o ON o.id = p.activation_order_id
         ORDER BY CASE WHEN p.verification_status = 'pending_review' THEN 0 ELSE 1 END, p.id DESC
         LIMIT {$limit}"
    )->fetchAll();
}

function commerce_is_valid_gammal_payment_url(string $url): bool
{
    if (!filter_var($url, FILTER_VALIDATE_URL)) {
        return false;
    }

    $parts = parse_url($url);
    $scheme = strtolower((string) ($parts['scheme'] ?? ''));
    $host = strtolower(rtrim((string) ($parts['host'] ?? ''), '.'));

    return $scheme === 'https'
        && ($host === 'gammal.tech' || str_ends_with($host, '.gammal.tech'));
}

function commerce_money(int $amountCents, string $currency = 'EUR'): string
{
    return number_format($amountCents / 100, 2, ',', ' ') . ' ' . strtoupper($currency);
}

function commerce_checkout_token(): string
{
    return bin2hex(random_bytes(24));
}

function commerce_order_reference(): string
{
    return 'SV-' . gmdate('ymd') . '-' . generate_short_code(6);
}

function commerce_create_test_order(
    PDO $pdo,
    int $userId,
    string $planKey,
    string $checkoutToken,
): array {
    if ($userId <= 0 || !preg_match('/^[a-f0-9]{48}$/', $checkoutToken)) {
        throw new InvalidArgumentException('Commande invalide. Rechargez la page.');
    }

    $planKey = commerce_plan_key($planKey);
    $plan = commerce_plan($planKey);
    $tokenHash = hash('sha256', $checkoutToken);

    $existing = commerce_find_order_by_token($pdo, $userId, $tokenHash);
    if ($existing !== null && ($existing['status'] ?? '') === 'paid' && !empty($existing['activation_code'])) {
        return $existing;
    }

    $pdo->beginTransaction();
    try {
        $orderId = null;
        $orderReference = null;
        if ($existing !== null && ($existing['status'] ?? '') === 'pending') {
            $orderId = (int) $existing['id'];
            $orderReference = (string) $existing['order_reference'];
            $pdo->prepare(
                "UPDATE activation_orders
                 SET plan_key = :plan_key, plan_label = :plan_label, amount_cents = :amount_cents, updated_at = NOW()
                 WHERE id = :id AND user_id = :user_id AND status = 'pending'"
            )->execute([
                'plan_key' => $planKey,
                'plan_label' => $plan['full_label'],
                'amount_cents' => $plan['amount_cents'],
                'id' => $orderId,
                'user_id' => $userId,
            ]);
        } else {
            for ($attempt = 0; $attempt < 10; $attempt++) {
                $candidateReference = commerce_order_reference();
                try {
                    $insertOrder = $pdo->prepare(
                        "INSERT INTO activation_orders
                            (user_id, order_reference, plan_key, plan_label, amount_cents, currency,
                             status, payment_provider, checkout_token_hash, created_at, updated_at)
                         VALUES
                            (:user_id, :order_reference, :plan_key, :plan_label, :amount_cents, 'EUR',
                             'pending', 'test', :checkout_token_hash, NOW(), NOW())"
                    );
                    $insertOrder->execute([
                        'user_id' => $userId,
                        'order_reference' => $candidateReference,
                        'plan_key' => $planKey,
                        'plan_label' => $plan['full_label'],
                        'amount_cents' => $plan['amount_cents'],
                        'checkout_token_hash' => $tokenHash,
                    ]);
                    $orderId = (int) $pdo->lastInsertId();
                    $orderReference = $candidateReference;
                    break;
                } catch (Throwable $exception) {
                    if (!is_duplicate_key($exception)) {
                        throw $exception;
                    }
                    $existing = commerce_find_order_by_token($pdo, $userId, $tokenHash);
                    if ($existing !== null && ($existing['status'] ?? '') === 'paid' && !empty($existing['activation_code'])) {
                        $pdo->rollBack();
                        return $existing;
                    }
                    if ($existing !== null && ($existing['status'] ?? '') === 'pending') {
                        $orderId = (int) $existing['id'];
                        $orderReference = (string) $existing['order_reference'];
                        break;
                    }
                }
            }
        }

        if ($orderId === null || $orderReference === null) {
            throw new RuntimeException('Creation de commande impossible.');
        }

        $createdCode = create_activation_code_record(
            $pdo,
            $orderReference . ' - ' . $plan['full_label'],
            (int) $plan['duration_days'],
            1,
            'paid',
            null,
            'customer:' . $userId,
        );
        $plainCode = (string) $createdCode['code'];
        $codeId = (int) $createdCode['id'];

        $paymentReference = 'TEST-' . strtoupper(generate_short_code(10));
        $updateOrder = $pdo->prepare(
            "UPDATE activation_orders
             SET status = 'paid', activation_code_id = :code_id,
                 activation_code_ciphertext = :code_ciphertext,
                 payment_reference = :payment_reference,
                 paid_at = NOW(), updated_at = NOW()
             WHERE id = :order_id AND status = 'pending'"
        );
        $updateOrder->execute([
            'code_id' => $codeId,
            'code_ciphertext' => encrypt_private_value($plainCode),
            'payment_reference' => $paymentReference,
            'order_id' => $orderId,
        ]);

        if ($updateOrder->rowCount() !== 1) {
            throw new RuntimeException('Validation de paiement impossible.');
        }

        $pdo->commit();

        return [
            'id' => $orderId,
            'order_reference' => $orderReference,
            'plan_key' => $planKey,
            'plan_label' => $plan['full_label'],
            'amount_cents' => $plan['amount_cents'],
            'currency' => 'EUR',
            'status' => 'paid',
            'activation_code' => $plainCode,
        ];
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }

        if (is_duplicate_key($exception)) {
            $existing = commerce_find_order_by_token($pdo, $userId, $tokenHash);
            if ($existing !== null && ($existing['status'] ?? '') === 'paid' && !empty($existing['activation_code'])) {
                return $existing;
            }
        }
        throw $exception;
    }
}

function commerce_find_order_by_token(PDO $pdo, int $userId, string $tokenHash): ?array
{
    $statement = $pdo->prepare(
        "SELECT id, order_reference, plan_key, plan_label, amount_cents, currency, status,
                activation_code_ciphertext
         FROM activation_orders
         WHERE user_id = :user_id AND checkout_token_hash = :checkout_token_hash
         LIMIT 1"
    );
    $statement->execute([
        'user_id' => $userId,
        'checkout_token_hash' => $tokenHash,
    ]);
    $order = $statement->fetch();
    if (!is_array($order)) {
        return null;
    }
    $order['activation_code'] = decrypt_private_value($order['activation_code_ciphertext'] ?? null);
    return $order;
}

function commerce_load_customer_orders(PDO $pdo, int $userId): array
{
    $statement = $pdo->prepare(
        "SELECT orders.id, orders.order_reference, orders.plan_key, orders.plan_label,
                orders.amount_cents, orders.currency, orders.status, orders.payment_provider,
                orders.payment_reference, orders.activation_code_ciphertext,
                orders.created_at, orders.paid_at,
                codes.status AS code_status, codes.max_devices, codes.used_devices,
                metadata.code_hint, metadata.last_used_at,
                MAX(activations.expires_at) AS activation_expires_at,
                SUM(CASE WHEN activations.status = 'active' AND activations.expires_at > NOW() THEN 1 ELSE 0 END) AS active_devices
         FROM activation_orders orders
         LEFT JOIN activation_codes codes ON codes.id = orders.activation_code_id
         LEFT JOIN activation_code_metadata metadata ON metadata.code_id = codes.id
         LEFT JOIN device_activations activations ON activations.activation_code_id = codes.id
         WHERE orders.user_id = :user_id
         GROUP BY orders.id, codes.id, metadata.code_id
         ORDER BY orders.id DESC
         LIMIT 50"
    );
    $statement->execute(['user_id' => $userId]);
    $orders = $statement->fetchAll();

    return array_map(static function (array $order): array {
        $order['activation_code'] = decrypt_private_value($order['activation_code_ciphertext'] ?? null);
        unset($order['activation_code_ciphertext']);
        return $order;
    }, $orders);
}

function commerce_customer_summary(array $orders): array
{
    $paidOrders = array_filter($orders, static fn(array $order): bool => $order['status'] === 'paid');
    $activeLicenses = array_filter($paidOrders, static fn(array $order): bool =>
        ($order['code_status'] ?? '') === 'active'
        && (int) ($order['active_devices'] ?? 0) > 0
    );

    return [
        'orders' => count($orders),
        'licenses' => count($paidOrders),
        'active_licenses' => count($activeLicenses),
        'devices' => array_sum(array_map(static fn(array $order): int => (int) ($order['active_devices'] ?? 0), $paidOrders)),
    ];
}
