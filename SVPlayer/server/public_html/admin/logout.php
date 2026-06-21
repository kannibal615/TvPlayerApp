<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST' || !verify_csrf($_POST['csrf_token'] ?? null)) {
    http_response_code(405);
    exit('Methode non autorisee.');
}

destroy_admin_session();
header('Location: /admin/');
exit;
