<?php
declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

function commerce_plans(): array
{
    return [
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
    if ($existing !== null) {
        return $existing;
    }

    $pdo->beginTransaction();
    try {
        $orderId = null;
        $orderReference = null;
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
                if ($existing !== null) {
                    $pdo->rollBack();
                    return $existing;
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
            if ($existing !== null) {
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
