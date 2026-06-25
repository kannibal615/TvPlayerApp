# Guide de reproduction — paiements Gammal Tech et emails SMTP HostCreed

## Objectif du document

Ce document est destiné à un agent IA chargé de reproduire, dans un autre
projet, les fonctionnalités de paiement Gammal Tech et d'envoi d'emails
transactionnels utilisées par SmartVision.

Il décrit :

- le parcours de commande et la création du lien de paiement Gammal Tech ;
- le retour navigateur et la vérification du paiement ;
- l'idempotence et l'enregistrement en base ;
- la création ou le renouvellement du service acheté ;
- l'envoi de l'email client et de la notification administrateur ;
- le transport SMTP HostCreed/cPanel ;
- les templates, les journaux et l'administration des emails ;
- les points à renforcer lors de la réimplémentation.

Le nom observé dans le code et les URLs officielles est **Gammal Tech**
(`gammal.tech`). Certaines demandes peuvent l'écrire « Gammel Tech ».

> Règle de sécurité : ne jamais copier dans ce fichier les mots de passe,
> tokens cPanel, clés API, identifiants MySQL ou identifiants SMTP réels.
> Toutes les valeurs sensibles doivent venir d'un gestionnaire de secrets,
> d'un fichier d'environnement non versionné ou d'un fichier de configuration
> privé hors du paquet de déploiement.

## 1. Architecture générale

L'intégration SmartVision est composée de cinq blocs :

1. Le frontend collecte le contexte de commande.
2. Le frontend crée une intention de commande en base.
3. Le navigateur est redirigé vers un lien de paiement Gammal Tech.
4. Gammal redirige le client vers une page callback qui vérifie le paiement.
5. Le backend enregistre le paiement, active le service et envoie les emails.

Flux simplifié :

```text
Client
  -> choix du pack et du mode de commande
  -> POST /api/order-intents
  -> redirection https://api.gammal.tech/sdk/pay/link/
  -> paiement chez Gammal
  -> retour GET /payment-callback
  -> verify.js appelle GammalVerify(...)
  -> POST interne /payment-callback
  -> verrou + contrôle anti-doublon
  -> création/renouvellement du service
  -> insertion subscription + payment
  -> email client SMTP
  -> notification admin SMTP
  -> réponse JSON et affichage du récapitulatif
```

Dans SmartVision, les principales sources sont :

- `src/browser/site-admin-sync/site-admin-sync.js` : checkout public ;
- `public/assets/client-account.js` : renouvellement depuis le compte client ;
- `src/hostcreed/public-api.php` : intentions de commande ;
- `src/hostcreed/payment-callback.php` : callback et traitement du paiement ;
- `src/hostcreed/mail-service.php` : templates, SMTP, EmailJS et logs ;
- `src/hostcreed/app/migrations/` : schéma MySQL ;
- `src/hostcreed/admin.php` : configuration et supervision des emails.

## 2. Configuration à prévoir

Utiliser des noms adaptés au nouveau projet. Exemple :

```dotenv
# Gammal Tech
GAMMAL_PROJECT_ID=xxx
GAMMAL_PAYMENT_LINK_URL=https://api.gammal.tech/sdk/pay/link/
GAMMAL_SERVICE_FEE_RATE=0.08
APP_PUBLIC_URL=https://example.com

# SMTP HostCreed/cPanel
SMTP_ENABLED=true
SMTP_HOST=nls.hostcreed.com
SMTP_PORT=465
SMTP_SECURE=ssl
SMTP_USER=contact@example.com
SMTP_PASSWORD=secret_non_versionne
SMTP_FROM_EMAIL=contact@example.com
SMTP_FROM_NAME=NomDuSite
SMTP_REPLY_TO=contact@example.com
ADMIN_NOTIFICATION_EMAIL=admin@example.com

# Comportement applicatif
EXTERNAL_SERVICES_ENABLED=true
APP_ENV=production
```

Configuration HostCreed utilisée par SmartVision :

- serveur : `nls.hostcreed.com` ;
- port : `465` ;
- sécurité : SSL implicite ;
- authentification : `AUTH LOGIN` ;
- expéditeur : une boîte email cPanel du domaine ;
- SPF, DKIM et DMARC configurés sur le domaine.

Le mot de passe SMTP est celui de la boîte email cPanel, pas le token API
cPanel et pas le mot de passe MySQL.

## 3. Modèle de données minimal

### 3.1 Table `order_intents`

Elle conserve le panier avant redirection vers Gammal et permet les relances.

Champs importants :

```sql
create table order_intents (
    id bigint unsigned auto_increment primary key,
    session_key varchar(191),
    customer_id bigint unsigned null,
    customer_email varchar(255) null,
    customer_phone varchar(255) null,
    device_type varchar(64) null,
    plan_slug varchar(191) null,
    plan_title varchar(255) null,
    plan_duration varchar(255) null,
    amount_cents int not null default 0,
    currency varchar(16) not null default 'EUR',
    status varchar(64) not null default 'started',
    source varchar(64) not null default 'checkout',
    reminder_sent_at timestamp null,
    raw_payload json null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
        on update current_timestamp,
    index order_intents_email_idx (customer_email),
    index order_intents_status_idx (status, reminder_sent_at)
);
```

États utilisés :

- `started` : checkout commencé ;
- `paid` : paiement enregistré ;
- `reminded` : rappel panier envoyé ;
- `reminder_pending` ou `reminder_error` : tentative de rappel non aboutie.

### 3.2 Table `payments`

Elle conserve le paiement et un instantané du service livré.

Champs importants :

```sql
create table payments (
    id bigint unsigned auto_increment primary key,
    customer_id bigint unsigned null,
    subscription_id bigint unsigned null,
    plan_slug varchar(191) null,
    plan_title varchar(255) null,
    plan_duration varchar(64) null,
    amount_cents int not null,
    currency varchar(16) not null default 'EUR',
    gateway varchar(64) not null default 'gammal',
    method varchar(64) not null default 'payment_link',
    txn varchar(255) null,
    customer_name varchar(255) null,
    customer_email varchar(255) null,
    customer_phone varchar(255) null,
    device_type varchar(64) null,
    email_status varchar(64) not null default 'pending',
    raw_payload json not null,
    created_at timestamp not null default current_timestamp,
    unique index payments_txn_unique_idx (txn)
);
```

SmartVision ajoute aussi des colonnes `subscription_*` pour garder un
instantané du service fourni : statut, durée, identifiant externe, URL,
serveur, username, password, expiration, message et payload externe.

La contrainte unique sur `txn` est essentielle. Dans la migration SmartVision,
elle n'est créée que si aucune transaction non vide n'est déjà dupliquée.

### 3.3 Table `subscriptions`

Elle représente le produit ou service activé après paiement.

```sql
create table subscriptions (
    id bigint unsigned auto_increment primary key,
    customer_id bigint unsigned null,
    source varchar(64) not null default 'payment',
    plan_slug varchar(191) null,
    plan_title varchar(255) null,
    device_type varchar(64) null,
    external_user_id varchar(255) null,
    access_url text null,
    host varchar(255) null,
    username varchar(255) null,
    password varchar(255) null,
    status varchar(64) null,
    duration_units int null,
    expires_at timestamp null,
    raw_payload json null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
        on update current_timestamp
);
```

Dans un autre métier, remplacer les données IPTV par les informations du
produit livré : licence, réservation, téléchargement, abonnement SaaS, etc.

## 4. Checkout côté navigateur

### 4.1 Charger le pack depuis une source fiable

Le frontend reçoit pour chaque pack :

```js
{
  slug: "premium",
  title: "Pack Premium",
  duration: "6 mois",
  amountCents: 4000,
  currency: "EUR"
}
```

SmartVision alimente ces valeurs depuis le catalogue MySQL exposé par
`/api/public-config`. Le frontend ne doit pas constituer lui-même le prix à
partir de texte HTML.

### 4.2 Choisir le contexte client

Trois modes sont proposés :

- compte existant ;
- création de compte avant paiement ;
- commande invitée avec email uniquement.

Pour un compte existant, le client peut :

- créer un nouveau service ;
- renouveler un service existant en conservant ses identifiants.

Contexte stocké temporairement :

```js
{
  mode: "account",             // account ou guest
  preserve_existing: true,
  subscription_id: 123,
  device_type: "smart_tv_android",
  email: "client@example.com",
  phone: "+33..."
}
```

SmartVision place ce contexte dans `sessionStorage` :

```js
sessionStorage.setItem(
  "smartvision_order_context_latest",
  JSON.stringify(orderContext)
);

sessionStorage.setItem(
  "smartvision_payment_customer_latest",
  JSON.stringify({
    email: orderContext.email,
    phone: orderContext.phone,
    device_type: orderContext.device_type
  })
);
```

Ce stockage sert uniquement à restaurer le contexte après la redirection.
Il ne doit jamais être considéré comme une preuve de paiement ou une source
fiable pour le prix.

### 4.3 Enregistrer l'intention de commande

Avant la redirection :

```http
POST /api/order-intents
Content-Type: application/json
```

Payload :

```json
{
  "session_key": "identifiant-analytics",
  "plan_slug": "premium",
  "plan_title": "Pack Premium",
  "plan_duration": "6 mois",
  "amount_cents": 4000,
  "currency": "EUR",
  "customer_email": "client@example.com",
  "customer_phone": "+33...",
  "device_type": "smart_tv_android",
  "mode": "account",
  "preserve_existing": true
}
```

SmartVision limite cette route à 30 requêtes par minute et par IP. L'email est
normalisé en minuscules et validé avant insertion.

Pour une nouvelle implémentation, retourner l'identifiant de l'intention et
l'inclure dans le callback. Cela permet de charger côté serveur le pack, le
prix et le client sans faire confiance aux query parameters.

### 4.4 Calcul du montant envoyé à Gammal

Dans SmartVision, le prix affiché au client inclut une majoration de 8 %. Le
montant transmis à Gammal correspond au montant marchand avant cette
majoration :

```js
const feeRate = 0.08;
const merchantAmount =
  Math.round((customerAmount / (1 + feeRate)) * 100) / 100;
```

Exemple :

```text
prix client : 40,00 EUR
montant envoyé à Gammal : 37,04 EUR
```

Cette formule doit être confirmée avec le contrat Gammal du nouveau projet.
Ne pas recopier automatiquement le taux de 8 % si les conditions commerciales
sont différentes.

### 4.5 Construire le callback

Implémentation actuelle :

```js
function buildPaymentCallbackUrl(plan) {
  const callback = new URL("/payment-callback", window.location.origin);
  callback.searchParams.set("plan_slug", plan.slug);
  callback.searchParams.set("plan_title", plan.title);
  callback.searchParams.set("plan_duration", plan.duration);
  callback.searchParams.set("amount_cents", String(plan.amountCents));
  callback.searchParams.set("currency", plan.currency);
  callback.searchParams.set("method", "payment_link");
  return callback.toString();
}
```

Implémentation recommandée :

```text
https://example.com/payment-callback?intent=UUID_ALEATOIRE
```

L'UUID doit référencer une intention enregistrée côté serveur. Le serveur
retrouve ensuite le prix et le pack en base.

### 4.6 Construire le lien Gammal

```js
const url = new URL("https://api.gammal.tech/sdk/pay/link/");
url.searchParams.set("pid", process.env.GAMMAL_PROJECT_ID);
url.searchParams.set("amount", merchantAmount.toFixed(2));
url.searchParams.set("desc", description.slice(0, 200));
url.searchParams.set("currency", plan.currency);
url.searchParams.set("callback", callbackUrl);
window.location.href = url.toString();
```

Paramètres observés :

- `pid` : identifiant du projet Gammal ;
- `amount` : montant décimal envoyé à la passerelle ;
- `desc` : description limitée à 200 caractères ;
- `currency` : devise, actuellement `EUR` ;
- `callback` : URL absolue de retour.

L'identifiant de projet est public par nature dans cette intégration frontend.
Les clés privées éventuelles ne doivent jamais être placées dans JavaScript.

## 5. Callback Gammal Tech

### 5.1 Route GET : page de vérification

Gammal redirige le navigateur vers :

```text
GET /payment-callback?...paramètres...
```

La page charge :

```html
<script src="https://api.gammal.tech/sdk/pay/link/verify.js"></script>
```

Puis appelle :

```js
window.GammalVerify(function (payment) {
  if (payment.status === 1) {
    // paiement validé
  } else if (payment.status === 2) {
    // transaction déjà vérifiée
  } else {
    // paiement non vérifié
  }
});
```

États gérés dans SmartVision :

- `status === 1` : paiement valide ;
- `status === 2` : paiement déjà traité/vérifié ;
- autre valeur : paiement non vérifié ;
- query parameter `status=cancelled` : paiement annulé.

Après un statut `1`, le navigateur envoie le résultat au backend :

```http
POST /payment-callback
Content-Type: application/json
```

```json
{
  "plan_slug": "premium",
  "plan_title": "Pack Premium",
  "plan_duration": "6 mois",
  "amount_cents": 4000,
  "currency": "EUR",
  "method": "payment_link",
  "customer_email": "client@example.com",
  "customer_phone": "+33...",
  "device_type": "smart_tv_android",
  "order_context": {},
  "payment": {
    "status": 1,
    "txn": "transaction-gammal",
    "amount": 37.04,
    "currency": "EUR",
    "description": "Pack Premium - 6 mois"
  }
}
```

### 5.2 Avertissement de sécurité majeur

Dans l'état actuel de SmartVision, la vérification Gammal a lieu dans le
navigateur et le backend reçoit ensuite un objet `payment` fourni par ce
navigateur. Cette donnée est falsifiable si aucun contrôle serveur
supplémentaire n'est effectué.

Dans le nouveau projet, l'agent doit rechercher et utiliser en priorité :

- une API Gammal de vérification serveur à serveur ;
- une signature cryptographique du callback ;
- un webhook signé ;
- ou un endpoint de consultation d'une transaction par `txn`.

Le backend doit refuser l'activation tant qu'il n'a pas vérifié côté serveur :

- l'existence de la transaction ;
- son statut payé ;
- son projet marchand ;
- son montant ;
- sa devise ;
- son absence de remboursement/annulation ;
- sa correspondance avec l'intention locale.

Si Gammal ne fournit réellement aucun mécanisme serveur, documenter cette
limitation explicitement et isoler l'activation dans un état `pending_review`
au lieu de faire confiance au navigateur.

### 5.3 Idempotence

SmartVision applique deux protections :

1. un verrou MySQL nommé basé sur le hash de `txn` ;
2. une contrainte unique en base sur `payments.txn`.

Pseudo-code :

```php
$lockName = 'payment_' . hash('sha256', $txn);
SELECT GET_LOCK($lockName, 10);

$existing = SELECT * FROM payments WHERE txn = ? LIMIT 1;
if ($existing) {
    return existingPaymentResponse($existing);
}

// traitement du paiement

SELECT RELEASE_LOCK($lockName);
```

Le verrou empêche deux requêtes concurrentes de provisionner deux services
avant que la contrainte unique ne soit atteinte.

Règles :

- rejeter un paiement sans `txn` stable ;
- garder la contrainte unique ;
- libérer le verrou dans un `finally` ;
- retourner le résultat existant en cas de répétition ;
- ne jamais recréer un abonnement pour une transaction déjà enregistrée.

### 5.4 Résolution du client

SmartVision lie le paiement au compte connecté uniquement si :

```text
session.customer_id existe
ET order_context.mode === "account"
```

Pour un compte, l'email et le téléphone de la base sont prioritaires sur les
données du navigateur.

Ordre de priorité actuel :

```text
compte en base
-> order_context
-> payload callback
-> payload Gammal
```

Le type d'appareil est normalisé avec une liste de caractères autorisés et une
longueur maximale de 64 caractères.

## 6. Création ou renouvellement du service

Après confirmation du paiement, SmartVision appelle TVPlusPanel. Dans un autre
projet, remplacer cette étape par la livraison métier nécessaire.

### Création

```text
action=new
type=m3u
sub=<1|3|6|12>
pack=all
note=<email | téléphone | appareil>
api_key=<secret>
```

Le résultat est normalisé en :

```php
[
    'status' => 'active',
    'userId' => '...',
    'url' => '...',
    'host' => '...',
    'username' => '...',
    'password' => '...',
    'expiresAt' => 'YYYY-MM-DD HH:MM:SS',
    'message' => '...',
    'payload' => $rawResponse,
]
```

### Renouvellement

Si `preserve_existing=true`, que l'abonnement appartient au client connecté et
qu'il contient un username/password :

```text
action=renew
type=m3u
username=<existant>
password=<existant>
sub=<durée achetée>
note=<contexte client>
api_key=<secret>
```

L'enregistrement `subscriptions` existant est mis à jour. Sinon, un nouvel
abonnement est créé.

Pour le nouveau projet :

- vérifier que la ressource à renouveler appartient bien au client ;
- ne jamais accepter directement un `subscription_id` du navigateur ;
- conserver la réponse brute du fournisseur externe pour le support ;
- définir clairement ce qui se passe si le paiement réussit mais que la
  livraison externe échoue ;
- préférer une file de jobs avec reprises automatiques pour le provisioning.

## 7. Finalisation du paiement

Ordre actuel SmartVision :

1. créer ou renouveler l'abonnement ;
2. envoyer l'email de commande au client ;
3. insérer le paiement avec `email_status` ;
4. passer les intentions récentes du même email à `paid` ;
5. attribuer les éventuels points de parrainage ;
6. envoyer la notification administrateur ;
7. retourner le récapitulatif au navigateur.

Pour une nouvelle implémentation plus robuste :

1. vérifier le paiement côté serveur ;
2. ouvrir une transaction SQL ;
3. réserver/créer les enregistrements `payment` et `subscription` ;
4. valider la transaction SQL ;
5. provisionner le service via un job idempotent ;
6. envoyer les emails via un job idempotent ;
7. enregistrer chaque tentative.

Les appels réseau et SMTP ne devraient pas garder une transaction SQL ouverte.

## 8. Service email complet

## 8.1 Interface métier

Tous les emails passent par une fonction unique :

```php
sv_send_email(
    ?PDO $pdo,
    string $type,
    string $recipient,
    array $payload = []
): string;
```

Statuts retournés :

- `sent` : transport accepté ;
- `pending` : transport désactivé ou configuration absente ;
- `error` : erreur de transport/configuration ;
- `skipped` : template marketing désactivé.

Exemples :

```php
sv_send_email($pdo, 'verify_email', $email, [
    'verify_url' => $verifyUrl,
]);

sv_send_email($pdo, 'order', $email, [
    'plan_title' => $planTitle,
    'subscription' => $subscription,
]);

sv_send_admin_notification($pdo, 'order_created', [
    'Commande' => '#123',
    'Transaction' => $txn,
    'Email client' => $email,
    'Pack' => $planTitle,
], '/admin/payments');
```

## 8.2 Types d'emails

Templates système SmartVision :

- `verify_email` ;
- `registration_thanks` ;
- `password_recovery` ;
- `trial_active` ;
- `trial_expiring_soon` ;
- `trial_expired` ;
- `order_confirmed` ;
- `cart_reminder` ;
- `inactive_customer_reminder` ;
- `admin_notification_account_created` ;
- `admin_notification_trial_created` ;
- `admin_notification_order_created` ;
- `admin_notification_generic`.

Alias internes :

```text
reset_password -> password_recovery
trial -> trial_active
trial_reminder -> trial_expiring_soon
order -> order_confirmed
```

## 8.3 Table `email_templates`

```sql
create table email_templates (
    id bigint unsigned auto_increment primary key,
    template_key varchar(96) not null,
    locale varchar(32) not null default 'fr-FR',
    name varchar(255) not null,
    category varchar(64) not null,
    subject_template varchar(255) not null,
    title_template varchar(255) not null,
    intro_html text null,
    body_html mediumtext null,
    button_label_template varchar(255) null,
    button_url_variable varchar(96) null,
    footer_html text null,
    variables_json json null,
    is_system tinyint(1) not null default 1,
    is_active tinyint(1) not null default 1,
    sort_order int not null default 100,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
        on update current_timestamp,
    unique key email_templates_key_locale (template_key, locale)
);
```

Les templates par défaut sont insérés avec `insert ignore` afin de ne pas
écraser les modifications effectuées depuis l'administration.

## 8.4 Variables et échappement

Syntaxe :

```text
{{variable}}       valeur échappée HTML
{{customer.email}} valeur imbriquée échappée
{{{safe_block}}}   bloc HTML généré par le serveur
```

Blocs sûrs générés par SmartVision :

- `action_fallback` : lien texte de secours ;
- `subscription_block` : tableau des accès livrés ;
- `plan_cards` : cartes des packs actifs ;
- `admin_event_table` : détails d'une notification admin.

Ne jamais autoriser un administrateur ou un payload utilisateur à injecter
arbitrairement du HTML via la syntaxe triple accolade. Seuls des blocs produits
par le code serveur doivent être considérés sûrs.

Chaque email est construit en :

- version HTML ;
- version texte dérivée du HTML ;
- sujet UTF-8 ;
- enveloppe HTML responsive ;
- bouton d'action facultatif ;
- lien de secours pour confirmation/réinitialisation.

## 8.5 Routage du fournisseur

SmartVision sait router vers SMTP ou EmailJS.

Logique actuelle :

```text
si template marketing désactivé
  -> skipped
sinon si destinataire Gmail/Microsoft et EmailJS activé
  -> EmailJS
sinon si SMTP correctement configuré
  -> SMTP HostCreed
sinon
  -> pending
```

EmailJS est optionnel. Le besoin principal demandé ici est SMTP HostCreed.
Il est possible de supprimer EmailJS du nouveau projet et d'utiliser SMTP pour
tous les domaines.

## 9. Transport SMTP HostCreed en détail

SmartVision n'utilise pas PHPMailer. Il implémente directement le protocole
SMTP avec `fsockopen`.

### 9.1 Connexion SSL port 465

```php
$socket = fsockopen(
    'ssl://' . $smtpHost,
    465,
    $errno,
    $errstr,
    20
);
stream_set_timeout($socket, 20);
```

Dialogue :

```text
S: 220 ...
C: EHLO nls.hostcreed.com
S: 250 ...
C: AUTH LOGIN
S: 334 ...
C: base64(SMTP_USER)
S: 334 ...
C: base64(SMTP_PASSWORD)
S: 235 Authentication successful
C: MAIL FROM:<contact@example.com>
S: 250 ...
C: RCPT TO:<client@example.net>
S: 250 ou 251
C: DATA
S: 354 ...
C: headers + corps MIME + "\r\n.\r\n"
S: 250 Accepted
C: QUIT
S: 221 ...
```

### 9.2 Variante STARTTLS port 587

Si `SMTP_SECURE=tls` :

```text
connexion TCP normale
EHLO
STARTTLS
stream_socket_enable_crypto(...)
EHLO à nouveau
AUTH LOGIN
...
```

Pour HostCreed SmartVision, le profil validé est SSL implicite sur le port 465.

### 9.3 Réponses attendues

- `EHLO` : `250` ;
- `STARTTLS` : `220` ;
- `AUTH LOGIN` : `334` ;
- username encodé : `334` ;
- password encodé : `235` ;
- `MAIL FROM` : `250` ;
- `RCPT TO` : `250` ou `251` ;
- `DATA` : `354` ;
- fin du message : `250` ;
- `QUIT` : `221`.

Toute autre réponse lève une erreur et produit un log `error`.

### 9.4 Headers envoyés

```text
From: NomDuSite <contact@example.com>
Reply-To: contact@example.com
To: <client@example.net>
Subject: sujet encodé MIME si nécessaire
Date: RFC 2822
Message-ID: <valeur-aléatoire@domaine-expediteur>
MIME-Version: 1.0
Content-Type: multipart/alternative; boundary="..."
X-Mailer: Application Mail Service
```

Le domaine du `Message-ID` est dérivé de l'adresse expéditrice.

### 9.5 Corps multipart

Le corps contient :

1. une partie `text/plain; charset=UTF-8` ;
2. une partie `text/html; charset=UTF-8`.

Les deux sont encodées en `quoted-printable`.

Il faut appliquer le « dot-stuffing » SMTP :

```php
$message = preg_replace('/^\./m', '..', $message);
```

Sans cette opération, une ligne commençant par un point peut être interprétée
comme la fin du message.

### 9.6 Conditions d'activation

Le SMTP n'est utilisé que si toutes ces conditions sont vraies :

```text
EXTERNAL_SERVICES_ENABLED=true
SMTP_ENABLED=true
SMTP_HOST non vide
SMTP_USER non vide
SMTP_PASSWORD non vide
SMTP_FROM_EMAIL non vide
destinataire valide/non vide
```

Sinon, l'email est journalisé `pending` sans tentative réseau.

### 9.7 Recommandation pour le nouveau projet

Sauf contrainte particulière, utiliser une bibliothèque maintenue comme
PHPMailer, Symfony Mailer ou Nodemailer. Elle gère mieux :

- les certificats TLS ;
- les pièces jointes ;
- les adresses et noms internationaux ;
- les erreurs SMTP ;
- les encodages MIME ;
- les connexions persistantes ;
- les tests.

Conserver cependant la même interface métier, les mêmes statuts et les mêmes
logs afin de ne pas coupler le projet à une bibliothèque.

## 10. Journalisation des emails

Table :

```sql
create table email_logs (
    id bigint unsigned auto_increment primary key,
    email_type varchar(64) not null,
    recipient_email varchar(255) not null,
    subject varchar(255) null,
    status varchar(64) not null default 'pending',
    error_message text null,
    provider varchar(64) null,
    template_key varchar(96) null,
    html_snapshot mediumtext null,
    text_snapshot mediumtext null,
    payload_json json null,
    from_email varchar(255) null,
    reply_to varchar(255) null,
    created_at timestamp not null default current_timestamp,
    index email_logs_type_status_idx (email_type, status),
    index email_logs_provider_idx (provider),
    index email_logs_recipient_idx (recipient_email)
);
```

Avant stockage, SmartVision masque récursivement les valeurs dont la clé
contient :

```text
pass, password, token, secret, private, api_key
```

Exemple :

```json
{
  "subscription": {
    "username": "visible",
    "password": "[masked]"
  }
}
```

Pour un nouveau projet, éviter de conserver durablement les accès sensibles
dans `html_snapshot` et `text_snapshot`. Appliquer une politique de rétention,
du chiffrement au repos ou une suppression automatique selon le métier.

## 11. Administration email

Le panel SmartVision permet :

- d'activer/désactiver SMTP ;
- de modifier host, port et sécurité ;
- de modifier utilisateur, expéditeur, nom et Reply-To ;
- de changer le mot de passe uniquement si le champ est renseigné ;
- de choisir l'adresse de notification administrateur ;
- de modifier les templates en base ;
- de prévisualiser le HTML ;
- d'envoyer un email de test ;
- de consulter les 200 derniers logs ;
- de filtrer par type, statut, fournisseur et période ;
- de consulter le détail HTML/texte/payload d'un envoi.

Les écritures sont interdites en mode base de données `read_only`.

Important : le fichier de configuration contenant le mot de passe SMTP doit
rester hors du dépôt et hors du build. Si l'administration l'écrit, protéger
la route par :

- authentification forte ;
- permission dédiée ;
- CSRF ;
- HTTPS ;
- journal d'audit ;
- permissions système de fichiers restrictives.

## 12. Délivrabilité HostCreed

La réussite SMTP signifie que le serveur destinataire a accepté le message.
Elle ne garantit pas son arrivée dans la boîte principale.

Validation réalisée sur SmartVision :

- connexion TCP au port 465 ;
- plusieurs authentifications SMTP réussies ;
- envoi direct vers Gmail ;
- envoi direct vers Microsoft ;
- test via le pipeline applicatif réel ;
- contrôle dans cPanel « Track Delivery » ;
- SPF, DKIM et DMARC publiés.

Checklist à reproduire :

1. créer la boîte email dans cPanel ;
2. utiliser cette adresse comme `SMTP_USER` et `SMTP_FROM_EMAIL` ;
3. vérifier SPF dans « Email Deliverability » ;
4. vérifier DKIM ;
5. publier une politique DMARC adaptée ;
6. tester Gmail et Outlook/Hotmail ;
7. contrôler « Track Delivery » ;
8. vérifier manuellement boîte principale et spam ;
9. tester un email HTML et sa version texte ;
10. surveiller les erreurs et rebonds.

Éviter un `From` différent du compte authentifié si la configuration cPanel
n'autorise pas cet alias.

## 13. Gestion des erreurs

Cas à traiter explicitement :

### Paiement valide, enregistrement local échoué

Afficher au client la transaction Gammal et lui demander de contacter le
support. En parallèle, créer une alerte administrateur et prévoir une
réconciliation automatique.

### Paiement dupliqué

Retourner les données du paiement et de l'abonnement déjà existants. Ne pas
renvoyer un second produit.

### Paiement valide, provisioning externe échoué

Enregistrer le paiement avec un état de livraison `pending` ou `failed`,
réessayer automatiquement et notifier l'administrateur.

### Email échoué

Le paiement ne doit pas être annulé. Conserver `email_status=error`, afficher
les informations dans l'espace client et permettre une relance email.

### SMTP désactivé

Journaliser `pending`. En environnement local, ne jamais envoyer par accident
des emails réels.

### Callback sans contexte navigateur

Ne pas dépendre de `sessionStorage`. Retrouver le contexte via l'identifiant
d'intention stocké côté serveur.

## 14. Sécurité obligatoire dans le nouveau projet

L'agent IA doit appliquer au minimum :

- HTTPS obligatoire ;
- validation serveur du paiement auprès de Gammal ;
- validation serveur du pack depuis la base ;
- comparaison exacte montant/devise/intention/transaction ;
- identifiant d'intention aléatoire et non prédictible ;
- contrainte unique sur la transaction ;
- verrou ou traitement atomique ;
- contrôle de propriété lors d'un renouvellement ;
- aucune clé privée dans le frontend ;
- aucune confiance dans `sessionStorage`, query string ou payload navigateur ;
- limitation de débit sur les routes publiques ;
- logs sans secrets ;
- secrets hors Git ;
- protection CSRF sur l'administration ;
- requêtes SQL préparées ;
- timeouts sur tous les appels réseau ;
- jobs idempotents pour provisioning et email ;
- politique de rétention des données et payloads.

## 15. Plan d'implémentation conseillé à l'agent

1. Créer les tables `order_intents`, `payments`, `subscriptions`,
   `email_templates` et `email_logs`.
2. Ajouter la configuration secrète et les validations au démarrage.
3. Exposer le catalogue serveur des packs.
4. Créer une route serveur qui génère l'intention de commande.
5. Construire le lien Gammal à partir des données serveur.
6. Implémenter le callback GET.
7. Implémenter la vérification Gammal serveur à serveur.
8. Implémenter l'idempotence par transaction.
9. Créer le service de provisioning métier.
10. Insérer le paiement et la livraison avec des états explicites.
11. Créer le moteur de templates email.
12. Brancher SMTP HostCreed.
13. Ajouter les logs et les relances.
14. Ajouter les notifications administrateur.
15. Construire une page d'administration et de diagnostic.
16. Tester tous les scénarios ci-dessous.

## 16. Scénarios de test

### Paiement

- paiement normal avec compte ;
- paiement invité ;
- création de compte avant paiement ;
- renouvellement d'un service existant ;
- création d'un nouveau service pour un client existant ;
- paiement annulé ;
- statut Gammal non vérifié ;
- callback appelé deux fois ;
- deux callbacks concurrents ;
- transaction sans identifiant ;
- montant différent de l'intention ;
- devise différente ;
- pack falsifié dans l'URL ;
- transaction appartenant à un autre projet marchand ;
- provisioning externe indisponible ;
- base indisponible après paiement ;
- contexte `sessionStorage` absent.

### SMTP

- SMTP 465 SSL ;
- SMTP 587 STARTTLS si nécessaire ;
- mauvais mot de passe ;
- certificat TLS invalide ;
- destinataire invalide ;
- Gmail ;
- Outlook/Hotmail ;
- caractères accentués dans le sujet ;
- HTML et texte alternatif ;
- template désactivé ;
- SMTP désactivé ;
- timeout réseau ;
- log avec secrets masqués ;
- email client après commande ;
- notification administrateur ;
- relance d'un email en erreur.

## 17. Critères d'acceptation

L'intégration est terminée uniquement si :

- aucun paiement falsifié côté navigateur ne peut activer un produit ;
- une transaction ne peut produire qu'une seule livraison ;
- le prix est repris depuis le serveur ;
- un callback répété retourne le résultat existant ;
- un échec de provisioning peut être repris sans nouveau paiement ;
- un échec email ne perd pas la commande ;
- les emails apparaissent dans `email_logs` avec leur fournisseur et statut ;
- les secrets n'apparaissent ni dans Git, ni dans les logs, ni dans le
  navigateur ;
- Gmail et Microsoft acceptent les emails de test ;
- SPF, DKIM et DMARC sont valides ;
- l'environnement local peut fonctionner avec tous les services externes
  désactivés.

## 18. Résumé opérationnel

La reproduction fidèle du comportement SmartVision consiste à :

- rediriger vers le Payment Link Gammal avec `pid`, `amount`, `desc`,
  `currency` et `callback` ;
- utiliser `verify.js` sur la page de retour ;
- transmettre le résultat au backend ;
- rendre le traitement idempotent avec `txn` ;
- créer ou renouveler le service acheté ;
- enregistrer abonnement et paiement ;
- envoyer un email transactionnel multipart via SMTP HostCreed SSL 465 ;
- notifier l'administrateur ;
- conserver templates et logs en MySQL.

La reproduction sécurisée doit aller plus loin que l'état historique du
projet : la décision d'activer le produit doit être fondée sur une vérification
Gammal effectuée par le serveur et sur les données de l'intention enregistrée
en base, jamais sur les seules données renvoyées par le navigateur.
