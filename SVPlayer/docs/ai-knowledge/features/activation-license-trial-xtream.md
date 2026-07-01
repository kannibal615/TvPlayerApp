# Activation, Licence, Essai et Xtream

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Documenter le flux d'acces SmartVision: enregistrement device, activation, licence premium, essai gratuit, mode gratuit avec pubs et configuration Xtream.

## 2. Fonctionnement actuel

L'application demarre sur `SplashActivity`, puis `MainActivity`. `AppNavigation.kt` cree `ActivationViewModel`. Si l'appareil n'est pas active, `ActivationScreen` affiche les actions d'activation. Une fois actif, l'app peut afficher Home, mais les contenus Xtream demandent un compte playlist configure.

Flux observe:
- device register via `api/devices/register.php`;
- session d'activation via `api/create_activation_session.php`;
- polling statut via `api/device_status.php`;
- activation licence via `api/licenses/activate.php`;
- essai via `api/devices/start_trial.php`;
- mode gratuit avec pubs via `api/devices/enable_free_with_ads.php`;
- configuration Xtream via `api/create_playlist_setup_session.php` puis portail `/xtream/`;
- recuperation de `playlist_config` dans `device_status.php`;
- creation locale du compte Xtream et synchro catalogue.
- verification rapide Xtream au demarrage via `XtreamConnectionManager` avant synchro globale; en cas d'erreur, Home reste accessible mais les sections catalogue sont bloquees.
- au splash, `device_status.php` doit etre relu avant `XtreamConnectionManager.verifyQuick()` afin d'importer la derniere playlist configuree cote serveur avant le test, meme si un ancien compte local et un ancien catalogue Room existent deja.
- le splash natif porte tout le statut de demarrage avec un seul visuel et une seule progress bar; le panneau `StartupVerificationPanel` Compose n'est plus utilise au demarrage.
- le splash enchaine les statuts licence, activation, serveur Xtream, fraicheur de la derniere synchronisation, synchro si necessaire, prechargement Home / Live TV / Films / Series, puis demarrage.
- si le cache local indique deja un acces actif, `ActivationViewModel` garde Home accessible pendant le refresh serveur au lieu d'afficher un second loader Compose.
- apres `MainActivity`, `AppNavigation` attend que `ActivationViewModel` ait lu l'etat local avant de rendre `ActivationScreen`, afin d'eviter le flash "Activer SmartVision" sur un appareil deja actif.
- si le splash vient de valider Xtream pour le compte courant, `AppNavigation` ne relance pas immediatement la meme verification startup.

## 3. Workflow utilisateur

1. L'utilisateur ouvre l'app sur TV.
2. Si l'appareil n'est pas actif, il voit un code/QR d'activation ou les options d'essai/licence.
3. Il active sur le portail web ou saisit un code licence.
4. Si aucun compte Xtream n'est configure, les sections contenus affichent un QR de configuration.
5. Il configure ses identifiants Xtream sur le portail.
6. L'app recupere la playlist et synchronise les contenus.
7. L'essai peut demarrer apres validation Xtream si le type est `trial_pending_xtream`.

## 4. Workflow technique

Android:
- `ui/activation/ActivationScreen.kt`
- `ui/activation/ActivationViewModel.kt`
- `ui/activation/XtreamQrSetupPanel.kt`
- `data/activation/ActivationRepository.kt`
- `data/activation/ActivationApiService.kt`
- `core/config/XtreamAccountManager.kt`
- `data/xtream/XtreamConnectionManager.kt`
- `data/xtream/XtreamConnectionNotifier.kt`
- `ui/navigation/AppNavigation.kt`

Backend:
- `server/public_html/api/devices/register.php`
- `server/public_html/api/create_activation_session.php`
- `server/public_html/api/device_status.php`
- `server/public_html/api/licenses/activate.php`
- `server/public_html/api/devices/start_trial.php`
- `server/public_html/api/devices/enable_free_with_ads.php`
- `server/public_html/api/create_playlist_setup_session.php`
- `server/public_html/api/save_playlist_config.php`
- `server/public_html/activate/index.php`
- `server/public_html/xtream/index.php`
- `server/public_html/account/index.php`

## 5. Ecrans concernes

- Activation TV
- Home si licence ou essai donne acces
- Live TV / Movies / Series en cas de QR Xtream
- Home + popup alerte Xtream si compte configure mais indisponible
- Profil pour statut licence, device, compte Xtream
- Portail web activation / account / xtream

## 6. Fichiers de code concernes

- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/ActivationRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/ActivationApiService.kt`
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionNotifier.kt`
- `server/public_html/api/*activation*.php`
- `server/public_html/api/devices/*.php`
- `server/public_html/api/licenses/activate.php`
- `server/public_html/xtream/index.php`

## 7. Donnees / API / Backend / Admin

API Android connues:
- `POST api/devices/register.php`
- `POST api/create_activation_session.php`
- `GET api/device_status.php`
- `POST api/create_playlist_setup_session.php`
- `POST api/licenses/activate.php`
- `POST api/devices/start_trial.php`
- `POST api/devices/enable_free_with_ads.php`

Champs importants:
- `device_id`
- `device_token`
- `publicDeviceCode`
- `activation_type`
- `licenseStatus`
- `trialStatus`
- `freeWithAdsStatus`
- `xtreamStatus`
- `playlist_config`

Admin:
- generation et gestion des codes/licences dans `server/public_html/admin/index.php`;
- configuration des fonctionnalites via `app_config.php` et `app_feature_access`.

## 8. Dependances

- Backend/admin/API pour statut device et playlist.
- Catalogue/playback pour synchronisation apres configuration.
- Monetisation/tracking pour `free_with_ads` et gating.
- UI TV/focus pour activation, QR et formulaires.

## 9. Regles a ne pas casser

- Ne jamais stocker de secrets Xtream en clair dans les docs ou logs.
- Ne pas demarrer l'essai trop tot si le flux exige validation Xtream.
- Preserver l'ecran essai expire, achat licence et gratuit avec pubs.
- Ne pas changer le sens de `activation_type` sans mettre a jour Android et backend.
- Toujours verifier les retours publics si changement backend.
- L'alerte Xtream doit masquer les mots de passe et ne remonter au backend que serveur, type d'erreur, message court, detail technique non secret, code TV et version app.
- Ne pas tester seulement l'ancien compte local au demarrage: rafraichir le statut backend avant le controle Xtream pour detecter une playlist remplacee depuis le portail web.

## 10. Problemes connus

- Les infos de release dans legacy peuvent etre obsoletes.
- Les feature flags stockes en prod peuvent contredire les defaults PHP.
- Les nouveaux fichiers PHP doivent etre ajoutes au script de deploiement.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- activation;
- licence;
- essai gratuit;
- trial pending;
- configuration Xtream;
- QR playlist;
- statut device;
- code SmartVision;
- acces premium ou gratuit avec pubs.

Ne pas lire ce fichier si la demande concerne uniquement:
- focus visuel d'un ecran deja actif;
- build release sans changement produit;
- player ExoPlayer sans logique d'acces.

## 12. Historique court

- 2026-06-29: migration vers documentation specialisee.
- 2026-06-29: version Gradle locale constatee `0.1.50` / `53`; verifier avant release.
- 2026-06-30: flux `trial_pending_xtream` et endpoints activation reverifies contre le code.
- 2026-06-30: ajout etat Xtream central et popup/notification locale quand le serveur utilisateur est indisponible.
- 2026-06-30: correction du controle de demarrage Xtream: rafraichissement `device_status.php` avant verification et controle obligatoire au premier affichage actif, meme si la derniere synchro est recente.
- 2026-06-30: unification visuelle du splash et conservation de l'etat local actif pendant le refresh activation pour eviter un second loader apres le splash.
- 2026-07-01: suppression du panneau de verification Compose au demarrage; `SplashActivity` devient l'unique splash avec les statuts startup complets.
- 2026-07-01: ajout d'un garde `localStateReady` pour empecher le rendu transitoire de l'ecran activation apres le splash sur appareil deja actif.
