<?php
declare(strict_types=1);

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header("Referrer-Policy: no-referrer");
header("Content-Security-Policy: default-src 'self'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");

$deviceId = preg_replace('/[^A-Za-z0-9._:-]/', '', trim((string) ($_GET['device_id'] ?? '')));
$deviceId = substr((string) $deviceId, 0, 100);
$shortCode = preg_replace('/[^A-Z0-9]/', '', strtoupper(trim((string) ($_GET['code'] ?? ''))));
$shortCode = substr((string) $shortCode, 0, 20);
$hasValidSessionParameters = $deviceId !== '' && $shortCode !== '';

function escape_html(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}
?><!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activation SmartVision</title>
    <style>
        :root {
            color-scheme: dark;
            font-family: Inter, Arial, Helvetica, sans-serif;
            background: #020713;
            color: #fff;
        }

        * {
            box-sizing: border-box;
        }

        body {
            min-height: 100vh;
            margin: 0;
            display: grid;
            place-items: center;
            padding: 24px;
            background:
                radial-gradient(circle at 50% 20%, rgba(25, 118, 255, .25), transparent 42%),
                linear-gradient(145deg, #020713, #07111f 55%, #02050c);
        }

        main {
            width: min(100%, 560px);
            padding: 34px;
            border: 1px solid #263650;
            border-radius: 16px;
            background: rgba(11, 21, 38, .96);
            box-shadow: 0 28px 80px rgba(0, 0, 0, .44);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 30px;
            font-size: 23px;
            font-weight: 750;
        }

        .brand-mark {
            width: 36px;
            height: 36px;
            display: grid;
            place-items: center;
            border-radius: 10px;
            background: linear-gradient(145deg, #16d9ff, #176dff 55%, #0c2f8f);
            color: #fff;
            font-size: 20px;
        }

        .brand strong {
            color: #1c91ff;
        }

        h1 {
            margin: 0;
            font-size: clamp(30px, 7vw, 42px);
            line-height: 1.08;
        }

        .lead {
            margin: 14px 0 26px;
            color: #b5c1d4;
            font-size: 17px;
            line-height: 1.55;
        }

        .session {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 18px;
            margin-bottom: 24px;
            padding: 16px 18px;
            border: 1px solid #244d8e;
            border-radius: 10px;
            background: #08182d;
        }

        .session span {
            color: #9eacc1;
            font-size: 14px;
        }

        .session strong {
            font-size: 24px;
            letter-spacing: 2px;
        }

        label {
            display: block;
            margin-bottom: 9px;
            color: #dce6f5;
            font-size: 15px;
            font-weight: 650;
        }

        input {
            width: 100%;
            height: 58px;
            padding: 0 17px;
            border: 1px solid #334563;
            border-radius: 9px;
            outline: none;
            background: #07101e;
            color: #fff;
            font: inherit;
            font-size: 19px;
            letter-spacing: 1px;
            text-transform: uppercase;
        }

        input:focus {
            border-color: #2e87ff;
            box-shadow: 0 0 0 3px rgba(46, 135, 255, .2);
        }

        button {
            width: 100%;
            height: 56px;
            margin-top: 18px;
            border: 1px solid #3c9cff;
            border-radius: 9px;
            background: #1976ff;
            color: #fff;
            cursor: pointer;
            font: inherit;
            font-size: 17px;
            font-weight: 750;
        }

        button:hover,
        button:focus-visible {
            background: #2b86ff;
            box-shadow: 0 0 0 3px rgba(46, 135, 255, .24);
        }

        button:disabled {
            cursor: wait;
            opacity: .58;
        }

        .message {
            min-height: 24px;
            margin: 16px 0 0;
            color: #ff7373;
            line-height: 1.45;
        }

        .success {
            padding: 24px;
            border: 1px solid rgba(0, 230, 118, .5);
            border-radius: 10px;
            background: rgba(0, 230, 118, .08);
        }

        .success h2 {
            margin: 0 0 9px;
            color: #39eb94;
        }

        .success p {
            margin: 0;
            color: #c3d0df;
            line-height: 1.55;
        }

        .invalid {
            color: #ff7d7d;
            line-height: 1.55;
        }

        [hidden] {
            display: none !important;
        }

        @media (max-width: 520px) {
            body {
                padding: 14px;
            }

            main {
                padding: 25px 20px;
            }
        }
    </style>
</head>
<body>
<main>
    <div class="brand">
        <span class="brand-mark">&#9654;</span>
        <span>Smart<strong>Vision</strong></span>
    </div>

    <?php if (!$hasValidSessionParameters): ?>
        <h1>Lien invalide</h1>
        <p class="invalid">Scannez de nouveau le QR code affiche sur votre televiseur.</p>
    <?php else: ?>
        <section id="activation-content">
            <h1>Activer votre TV</h1>
            <p class="lead">Saisissez votre code SmartVision pour associer cet appareil a votre activation.</p>

            <div class="session">
                <span>Code affiche sur la TV</span>
                <strong><?= escape_html($shortCode) ?></strong>
            </div>

            <form id="activation-form" novalidate>
                <input type="hidden" id="device-id" value="<?= escape_html($deviceId) ?>">
                <input type="hidden" id="short-code" value="<?= escape_html($shortCode) ?>">

                <label for="activation-code">Code SmartVision</label>
                <input
                    id="activation-code"
                    name="activation_code"
                    type="text"
                    inputmode="text"
                    autocomplete="one-time-code"
                    minlength="6"
                    maxlength="64"
                    pattern="[A-Za-z0-9 -]+"
                    placeholder="SV-XXXX-XXXX"
                    required
                    autofocus
                >

                <button type="submit" id="submit-button">Activer cet appareil</button>
                <p class="message" id="message" role="alert" aria-live="polite"></p>
            </form>
        </section>

        <section class="success" id="success-content" hidden>
            <h2>Appareil active</h2>
            <p>SmartVision est maintenant actif sur votre televiseur. Vous pouvez revenir a l application.</p>
        </section>
    <?php endif; ?>
</main>

<?php if ($hasValidSessionParameters): ?>
<script>
    const form = document.getElementById('activation-form');
    const button = document.getElementById('submit-button');
    const message = document.getElementById('message');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        message.textContent = '';

        const activationCode = document.getElementById('activation-code').value.trim();
        if (activationCode.length < 6) {
            message.textContent = 'Saisissez un code SmartVision valide.';
            return;
        }

        button.disabled = true;
        button.textContent = 'Activation en cours...';

        try {
            const response = await fetch('/api/validate_activation.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    device_id: document.getElementById('device-id').value,
                    short_code: document.getElementById('short-code').value,
                    activation_code: activationCode,
                }),
            });
            const data = await response.json();

            if (!response.ok || data.success !== true) {
                throw new Error(data.error || 'Activation impossible.');
            }

            document.getElementById('activation-content').hidden = true;
            document.getElementById('success-content').hidden = false;
        } catch (error) {
            message.textContent = error.message || 'Activation impossible.';
            button.disabled = false;
            button.textContent = 'Activer cet appareil';
        }
    });
</script>
<?php endif; ?>
</body>
</html>
