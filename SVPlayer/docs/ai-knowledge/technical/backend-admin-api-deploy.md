# Backend, Admin, API et Deploiement

Derniere mise a jour: 2026-06-29.

## 1. Objectif

Documenter le backend PHP/MySQL, les endpoints publics, le panel admin, les flux compte/paiement/email et la procedure de deploiement cPanel.

## 2. Fonctionnement actuel

Le backend local est sous `server/public_html/` et cible `https://smartvisions.net`. Les API retournent du JSON, utilisent PDO et des requetes preparees. Le deploiement est pilote par `scripts/deploy_activation_phase1.ps1`.

Le script de deploiement upload les fichiers explicitement. Tout nouveau fichier PHP doit etre ajoute a la liste d'upload.

## 3. Workflow utilisateur

- Site public: decouverte, telechargement, achat/activation.
- Account: achat/prolongation, configuration Xtream, verification email.
- Activate/Xtream: parcours QR depuis TV.
- Admin: gestion fonctionnalites, consentement, pubs, codes, notifications, diagnostics.
- App: consomme les endpoints activation, config, update, ads, tracking.

## 4. Workflow technique

Fichiers de base:
- `server/public_html/api/config.php`
- `server/public_html/api/helpers.php`
- `server/public_html/admin/bootstrap.php`
- `server/public_html/admin/index.php`

Deploy:
- le script lit `local.properties` pour cPanel et secrets locaux;
- cree les repertoires distants;
- upload PHP/CSS/JS/assets;
- upload APK release si present;
- genere le manifeste de version;
- cree une notification de release;
- execute les tests publics sauf `-SkipTests`.

## 5. Ecrans concernes

- Site public
- Account
- Activation portal
- Xtream portal
- Admin panel
- App Android via API

## 6. Fichiers de code concernes

- `server/public_html/index.php`
- `server/public_html/download.php`
- `server/public_html/account/index.php`
- `server/public_html/activate/index.php`
- `server/public_html/activation/index.php`
- `server/public_html/xtream/index.php`
- `server/public_html/payment-callback/index.php`
- `server/public_html/verify-email/index.php`
- `server/public_html/admin/*`
- `server/public_html/api/*`
- `server/public_html/api/app/*`
- `server/public_html/assets/*`
- `server/public_html/sql/init_activation_tables.sql`
- `scripts/deploy_activation_phase1.ps1`

## 7. Donnees / API / Backend / Admin

Endpoints importants:
- `api/devices/register.php`
- `api/create_activation_session.php`
- `api/device_status.php`
- `api/validate_activation.php`
- `api/licenses/activate.php`
- `api/devices/start_trial.php`
- `api/devices/enable_free_with_ads.php`
- `api/create_playlist_setup_session.php`
- `api/save_playlist_config.php`
- `api/app_update.php`
- `api/app_config.php`
- `api/notifications.php`
- `api/home_slides.php`
- `api/commerce.php`
- `api/gammal-webhook.php`
- `api/app/ads-config.php`
- `api/app/ads-vast.php`
- `api/app/ads-events.php`
- `api/app/anomaly-events.php`
- `api/app/behavior-events.php`
- `api/app/device-diagnostics.php`

Tables/settings a surveiller:
- devices/licences/activation sessions selon schema SQL;
- `app_settings`;
- `app_feature_access`;
- `app_consent_receipts`;
- `app_notifications`;
- `ads_settings`;
- `ads_events`;
- `app_behavior_events`.

## 8. Dependances

- Activation/licence/Xtream.
- Monetisation/consentement/tracking.
- Build/release/update.
- UI TV si endpoint alimente un ecran.

## 9. Regles a ne pas casser

- Ne jamais afficher les secrets de `local.properties`.
- Tout nouveau PHP sous `api` doit etre ajoute au script de deploy.
- Les services optionnels admin doivent etre fail-safe.
- Les erreurs SQL brutes ne doivent pas etre renvoyees au public.
- Les scripts temporaires cPanel doivent etre self-delete et inclure `config.php` avant `helpers.php` si `db()` est requis.
- Verifier l'API publique apres changement de config prod.

## 10. Problemes connus

- Admin HTTP 500 possible si service PHP requis non uploade.
- Feature flags stockes en prod peuvent rester anciens.
- cPanel peut ne pas offrir `Fileman/delete_files`; preferer self-delete.
- Encodage UTF-8 avec BOM peut casser `declare(strict_types=1);`.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- PHP;
- admin;
- API;
- MySQL;
- deploy;
- cPanel;
- notifications;
- app_update;
- app_config;
- paiement;
- email;
- pubs/tracking cote serveur.

Ne pas lire ce fichier si la demande concerne uniquement:
- UI Compose locale sans API;
- player ExoPlayer pur;
- documentation d'une decision sans backend.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: ajout de la regle "nouveau PHP = ajout deploy script".
