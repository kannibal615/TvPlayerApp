<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/commerce.php';
require_once __DIR__ . '/mail_service.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_response(['success' => false, 'error' => 'Methode non autorisee.'], 405);
}

try {
    $payload = read_json_input();
    $result = commerce_process_gammal_webhook(db(), $payload);

    if (($result['status'] ?? '') === 'approved' && is_array($result['payment'] ?? null)) {
        $payment = $result['payment'];
        sv_send_email(db(), 'order_confirmed', (string) $payment['email'], [
            'customer' => ['name' => (string) ($payment['display_name'] ?: $payment['email'])],
            'order' => [
                'reference' => (string) $payment['order_reference'],
                'plan' => (string) $payment['plan_label'],
                'amount' => commerce_money((int) $payment['amount_cents'], (string) $payment['currency']),
            ],
            'license' => ['code' => (string) ($payment['activation_code'] ?? '')],
            'account_url' => smartvision_public_base_url() . '/account/?section=licenses',
        ]);
        sv_send_admin_notification(db(), 'admin_notification_order_created', [
            'Commande' => (string) $payment['order_reference'],
            'Transaction' => (string) $payment['txn'],
            'Email client' => (string) $payment['email'],
            'Pack' => (string) $payment['plan_label'],
            'Validation' => 'Webhook Gammal signe',
        ], '/admin/?page=orders');
    }

    json_response([
        'success' => true,
        'status' => (string) ($result['status'] ?? 'captured'),
    ]);
} catch (Throwable $exception) {
    error_log('SmartVision Gammal webhook failed: ' . $exception->getMessage());
    json_response([
        'success' => false,
        'error' => 'Webhook refuse.',
    ], $exception instanceof InvalidArgumentException ? 400 : 500);
}
