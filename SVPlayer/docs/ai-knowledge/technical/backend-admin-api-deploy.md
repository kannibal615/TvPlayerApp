# Backend, Admin, API et Deploiement

Derniere mise a jour: 2026-07-13.

## 1. Objectif

Documenter le backend PHP/MySQL, les endpoints publics, le panel admin, les flux compte/paiement/email et la procedure de deploiement cPanel.

## 2. Fonctionnement actuel

Le backend local est sous `server/public_html/` et cible `https://smartvisions.net`. Les API retournent du JSON, utilisent PDO et des requetes preparees. Le deploiement est pilote par `scripts/deploy_activation_phase1.ps1`.

Le script de deploiement upload les fichiers explicitement. Tout nouveau fichier PHP doit etre ajoute a la liste d'upload.

## 3. Workflow utilisateur

- Site public: decouverte, telechargement, achat/activation.
- Account: achat/prolongation, configuration Xtream, verification email.
- Activate/Xtream: parcours QR depuis TV.
- Playlist: page publique `/playlist/` pour envoyer par code TV des identifiants Xtream, un lien M3U ou une URL EPG vers la TV, sans passage obligatoire par le panel admin. Chaque envoi cree une notification d'information ciblee sur l'appareil, sans contenu sensible.
- Admin: gestion fonctionnalites, consentement, pubs, codes, notifications, diagnostics.
- Admin Diagnostics centralise maintenant Synthese, AutoSync, Anomalies App, Info Serveur et Journal dans `server/public_html/admin/index.php`.
- Admin ajoute `Bibliotheque privee` pour activer/desactiver le proxy provider, gerer les sous-dossiers TV prives (nom, recherche/theme, ordre, actif, suppression), vider le cache, lancer la sync `removed` et consulter le monitoring provider.
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
- La publication `0.1.118 (148)` a bien mis en ligne manifeste/API/APK, mais le test post-upload `create_playlist_setup_session.php` a retourne HTTP 500; ce controle backend reste a diagnostiquer separement.
- le wrapper `scripts/release_prod.ps1` incremente automatiquement `versionCode`, lance le build release Android puis appelle ce script avec `-SkipInstall -SkipTests` par defaut, avant de verifier publiquement le manifeste, `api/app_update.php`, l'APK versionne et l'APK stable.

Regle release:
- apres chaque nouveau build Android release destine a etre livre, executer le deploy backend avec `scripts/deploy_activation_phase1.ps1` pour synchroniser le manifeste update, l'APK versionne, l'APK stable, la notification release et les fichiers serveur;
- ne pas publier un APK seul si des changements PHP/admin/API accompagnent la release.
- pour les nouveaux endpoints imbriques, creer explicitement chaque dossier distant dans le script avant `Upload-File` (`api/media`, `api/media/private`, `api/media/private/providers`), sinon Fileman peut ne pas exposer les nouveaux PHP via HTTP malgre un deploy global OK.

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
- `server/public_html/playlist/index.php`
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
- `api/media/private/libraries.php`
- `api/media/private/categories.php`
- `api/media/private/items.php`
- `api/media/private/item.php`
- `api/media/private/playback.php`
- `api/media/private/providers/health.php`
  - Verifie en prod le 2026-07-07 via routes extensionless avec User-Agent Android-like: `libraries`, `categories`, `items` et `providers/health` retournent 200; si la bibliotheque est desactivee, `items` retourne `error=provider_disabled`.
- `api/notifications.php`
  - Optimisation 2026-07-05: la jointure appareil -> commande -> utilisateur n'est executee que si des notifications ciblees `users` existent dans les candidates actives. Les notifications `all` / `devices` evitent cette jointure pour reduire les risques de timeout socket sur l'app.
- `api/home_slides.php`
- `api/commerce.php`
- `api/gammal-webhook.php`
- `api/app/ads-config.php`
- `api/app/ads-vast.php`
- `api/app/ads-events.php`
- `api/app/anomaly-events.php`
- `api/app/behavior-events.php`
- `api/app/device-diagnostics.php`

Note routes `api/app/*`:
- Android appelle plusieurs routes sans `.php` (`api/app/ads-config`, `api/app/behavior-events`, etc.).
- `.htaccess` reecrit ces routes extensionless vers les fichiers PHP correspondants.
- Exception actuelle: `DeviceDiagnosticsApiService` appelle `api/app/device-diagnostics.php` directement.
- Le script de deploy upload les fichiers `.php`; Retrofit doit rester coherent avec la route declaree.

Tables/settings a surveiller:
- devices/licences/activation sessions selon schema SQL;
- `app_settings`;
- `app_feature_access`;
- defaults `app_feature_access` Recorder/Media ajoutes le 2026-07-05: `recorder`, `media_center` (`Menu Media Center`), `media_file_management`, `media_phone_transfer` avec Premium oui, Trial oui, Free Ads non;
- default `app_feature_access` ajoute le 2026-07-10: `multi_profile` (`Multi-profils`) avec Premium oui, Trial oui, Free Ads non; Android l'utilise pour griser/verrouiller l'ajout et la modification de profils.
- defaults `app_feature_access` ajoutes le 2026-07-07 pour `private_media`, `private_media_eporner`, `private_media_native_playback`; la configuration detaillee est stockee en JSON dans `app_settings.private_media_config`.
- `app_consent_receipts`;
- `app_notifications`;
- `ads_settings`;
- `ads_events`;
- `app_behavior_events`.
- `app_anomaly_events.public_device_code` pour afficher le code TV dans Diagnostics > Anomalies App.
- `device_playlist_configs.encrypted_payload` contient maintenant aussi `epg_url` ou `m3u_url` quand l'utilisateur fournit ces liens; une config M3U seule marque la playlist comme configuree mais ne marque pas Xtream comme configure.

## 8. Dependances

- Activation/licence/Xtream.
- Monetisation/consentement/tracking.
- Build/release/update.
- UI TV si endpoint alimente un ecran.

## 9. Regles a ne pas casser

- Ne jamais afficher les secrets de `local.properties`.
- Tout nouveau PHP sous `api` doit etre ajoute au script de deploy.
- Chaque nouveau build APK release livrable doit etre suivi d'un deploy backend, sauf si l'utilisateur demande explicitement un build local non publie.
- Les services optionnels admin doivent etre fail-safe.
- Les erreurs SQL brutes ne doivent pas etre renvoyees au public.
- Les scripts temporaires cPanel doivent etre self-delete et inclure `config.php` avant `helpers.php` si `db()` est requis.
- Verifier l'API publique apres changement de config prod.
- Si un nouveau champ est ajoute a une table existante, le service correspondant doit assurer une migration additive fail-safe avant lecture admin.

## 10. Problemes connus

- Admin HTTP 500 possible si service PHP requis non uploade.
- `Bibliotheque privee > Synchroniser removed` doit rester borne et transactionnel: le provider peut renvoyer un fichier volumineux, donc ne pas reinserer tous les ids en une seule requete admin non bornee.
- `Bibliotheque privee > Sous-dossiers TV` conserve les IDs des sections existantes; ne pas regenerer les IDs a chaque renommage, sinon Android perd la selection/categorie.
- `Bibliotheque privee > Forcer lecture native HLS/MP4` est un mode de test: il priorise `native_test_stream_url` si cette URL est un vrai `.m3u8` ou `.mp4`. Ne pas le presenter comme une conversion d'embed; sans flux direct, le backend doit renvoyer `UNAVAILABLE` pour la lecture native forcee.
- Le test admin du deploy doit suivre la navigation courante: apres la centralisation de Journal dans Diagnostics, le marqueur attendu au login est `Diagnostics`, pas un menu `Journal` separe.
- Feature flags stockes en prod peuvent rester anciens.
- cPanel peut ne pas offrir `Fileman/delete_files`; preferer self-delete.
- Encodage UTF-8 avec BOM peut casser `declare(strict_types=1);`.
- Sans User-Agent explicite, certains appels manuels PowerShell vers les nouveaux endpoints private media peuvent recevoir une 404 LiteSpeed alors que les appels Android/QA avec User-Agent retournent bien le JSON; verifier avec un User-Agent avant de conclure a un fichier manquant.

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
- `app_trending_config` dans `app_settings`: expose via `api/app_config.php` les parametres Home Trends (`require_landscape_image`, `use_rating_filter`, `minimum_rating`, `candidate_limit`, `section_limit`; `exclude_adult` reste actif dans l'admin).
- paiement;
- email;
- pubs/tracking cote serveur.

Ne pas lire ce fichier si la demande concerne uniquement:
- UI Compose locale sans API;
- player ExoPlayer pur;
- documentation d'une decision sans backend.

## 12. Historique court

- 2026-07-10: release prod Android `0.1.116` / `132` deployee via `scripts/deploy_activation_phase1.ps1 -SkipInstall`; manifeste `smartvision-tv-v132-04eaf015.apk`, SHA256 `04eaf0154a106c91f1d403325480217cdb95eece95bfe94d3accc2f71d3ebe5c`, taille `41116724`. Verification publique OK: manifeste et `api/app_update.php` en `132`, APK versionne et stable HTTP 200, hash/taille telecharges identiques, `app_config.php` expose `multi_profile` Premium=true Trial=true FreeAds=false.
- 2026-07-07: ajout des endpoints `api/media/private/*`, du menu admin `Bibliotheque privee`, des flags `private_media*`, et correction du deploy pour creer explicitement `api/media/private/providers` avant upload.
- 2026-07-07: `items.php` accepte `query` pour la recherche privee; `Synchroniser removed` est limite par lot et rollback en erreur pour eviter un HTTP 500 admin; playback prive renvoie aussi `embedUrl/pageUrl` pour le fallback TV.
- 2026-07-07: `Bibliotheque privee` gere les sous-dossiers TV prives avec suppression explicite et migration de l'ancien dossier unique vers les premiers themes par defaut; le backend ne renvoie des streams natifs que pour URLs directes HLS/MP4.
- 2026-07-07: ajout du mode admin test `Forcer lecture native HLS/MP4` avec champ `Flux HLS/MP4 de test`.
- 2026-06-29: migration vers documentation specialisee.
- 2026-07-02: `Admin > Fonctionnalites` ajoute le bloc Tendances Home et `api/app_config.php` renvoie le bloc `trending` consomme par Android.
- 2026-06-29: ajout de la regle "nouveau PHP = ajout deploy script".
- 2026-06-30: clarification des rewrites `.htaccess` pour `api/app/*` et exception `device-diagnostics.php`.
- 2026-06-30: ajout de la regle "nouveau build release livrable = deploy backend obligatoire".
- 2026-06-30: refonte admin Diagnostics en onglets et ajout migration additive `app_anomaly_events.public_device_code`.
- 2026-06-30: alignement du test admin `deploy_activation_phase1.ps1` sur la navigation Diagnostics apres suppression du menu Journal separe.
- 2026-07-01: ajout `/playlist/`, upload deploy associe, support `epg_url` dans le payload playlist chiffre, puis onglets Xtream/M3U/EPG et notification appareil ciblee apres push playlist.
- 2026-07-01: `device_status.php` et `save_playlist_config.php` traitent `m3u_url` comme configuration playlist valide, tout en gardant `xtreamStatus` lie uniquement aux identifiants Xtream.
- 2026-07-05: `api/notifications.php` evite la jointure utilisateur sauf besoin reel de ciblage `users`, afin de reduire le temps de reponse et les erreurs `SocketTimeoutException` cote Android.
- 2026-07-05: ajout des defaults admin/API `app_feature_access` pour Recorder et Media Center sans nouveau fichier PHP.
- 2026-07-05: `media_center` controle maintenant l'affichage/acces du menu Media Center cote Android.
- 2026-07-10: PlaylistWeb et `save_playlist_config.php` stockent `provided_fields` dans le payload chiffre. Android ne confond plus un ancien `epg_url` conserve avec un EPG fourni lors du dernier envoi; la page `/playlist/` propose aussi une suppression EPG explicite.
