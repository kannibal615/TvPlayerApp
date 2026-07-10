# Activation, Licence, Essai et Xtream

Derniere mise a jour: 2026-07-10.

## 1. Objectif

Documenter le flux d'acces SmartVision: enregistrement device, activation, licence premium, essai gratuit, mode gratuit avec pubs et configuration Xtream.

## 2. Fonctionnement actuel

L'application demarre sur `MainActivity`, activite launcher avec le theme systeme `Theme.SVPlayer.Splash`. `MainActivity` affiche le startup Compose, puis `AppNavigation.kt` cree `ActivationViewModel`. Si l'appareil n'est pas active, `ActivationScreen` affiche les actions d'activation. Une fois actif, l'app peut afficher Home, mais les contenus Xtream demandent un compte playlist configure.

Flux observe:
- device register via `api/devices/register.php`;
- session d'activation via `api/create_activation_session.php`;
- polling statut via `api/device_status.php`;
- activation licence via `api/licenses/activate.php`;
- essai via `api/devices/start_trial.php`;
- mode gratuit avec pubs via `api/devices/enable_free_with_ads.php`;
- configuration Xtream via `api/create_playlist_setup_session.php` puis portail `/xtream/`;
- envoi web direct via `/playlist/` avec code TV pour pousser des identifiants Xtream et/ou une URL EPG;
- recuperation de `playlist_config` dans `device_status.php`, incluant `epg_url` si fourni;
- suppression distante de la config playlist via `api/clear_playlist_config.php` quand un profil local correspondant est supprime;
- import Android dans un profil dedie `PlaylistWeb`, mis a jour par nom si l'utilisateur ne l'a pas renomme, puis synchro catalogue.
- verification rapide Xtream au demarrage via `XtreamConnectionManager` avant synchro globale; elle teste principalement `player_api.php` sans `action` (`user_info.status == active`) et confirme une panne sur 3 essais discrets avant popup, notification ou blocage catalogue.
- au splash, `device_status.php` doit etre relu avant `XtreamConnectionManager.verifyQuick()` afin d'importer la derniere playlist configuree cote serveur avant le test, meme si un ancien compte local et un ancien catalogue Room existent deja.
- `MainActivity` porte tout le statut de demarrage avec un seul visuel et une seule progress bar; `SplashActivity`, `StartupVerificationPanel` et `StartupHandoffScreen` ne sont plus utilises au demarrage.
- depuis le 2026-07-02, ce startup applicatif est hybride: le theme systeme `Theme.SVPlayer.Splash` affiche le fond + logo reduit, et Compose affiche uniquement progress bar, statuts et diagnostics au-dessus.
- `MainActivity` garde le theme splash systeme pendant le startup pour ne pas remplacer le fond immediat par un ecran noir; juste avant Home, il applique le theme normal et remplace le `windowBackground` par un fond opaque neutre.
- le splash enchaine les statuts licence, activation, serveur Xtream, fraicheur de la derniere synchronisation, synchro si necessaire, prechargement Home / Live TV / Films / Series, puis demarrage. Il affiche aussi pourcentage, etape courante, elements traites/restants, temps ecoule, ETA et details Live/Films/Series quand la synchro fournit ces compteurs.
- quand la source active est M3U, le splash verifie le lien M3U et ne precharge que Home / Live TV pour eviter de presenter Films / Series comme disponibles.
- si le cache local indique deja un acces actif, `ActivationViewModel` garde Home accessible pendant le refresh serveur au lieu d'afficher un second loader Compose.
- `AppNavigation` ne contient plus d'ecran intermediaire `StartupHandoffScreen`; apres `Demarrage en cours...`, la navigation est rendue directement.
- `MainActivity` ne garde plus `splash_background` comme fond de fenetre permanent apres `setContent`, afin d'eviter un retour visuel du splash derriere Home.
- si le splash vient de valider Xtream pour le compte courant, `AppNavigation` ne relance pas immediatement la meme verification startup.
- `AppNavigation` attend `ActivationViewModel.localStateReady` avant de choisir entre `ActivationScreen`, QR playlist et Home; cela evite le flash transitoire de l'ecran activation apres le splash sur les appareils deja actifs.

## 3. Workflow utilisateur

1. L'utilisateur ouvre l'app sur TV.
2. Si l'appareil n'est pas actif, il voit un code/QR d'activation ou les options d'essai/licence.
3. Il active sur le portail web ou saisit un code licence.
4. Si aucun compte Xtream n'est configure, les sections contenus affichent un QR de configuration.
5. Il configure ses identifiants Xtream sur le portail ou envoie un lien M3U / une URL EPG depuis `/playlist/`.
6. L'app recupere la playlist, conserve l'URL EPG, conserve le lien M3U, puis synchronise la source active choisie localement: Xtream ou M3U.
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
- `server/public_html/api/clear_playlist_config.php`
- `server/public_html/activate/index.php`
- `server/public_html/xtream/index.php`
- `server/public_html/playlist/index.php`
- `server/public_html/account/index.php`

## 5. Ecrans concernes

- Activation TV
- Home si licence ou essai donne acces
- Live TV / Movies / Series en cas de QR Xtream
- Home + popup alerte Xtream si compte configure mais indisponible
- Info compte pour statut licence, device, compte Xtream et URL EPG
- Portail web activation / account / xtream / playlist

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
- `POST api/clear_playlist_config.php`
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
- `epg_url`
- `m3u_url`
- `activePlaylistSource` local Android: `xtream` ou `m3u`, exclusif.
- `playlist_profiles_json` et `active_playlist_profile_id` locaux Android: profils utilisateur multiples geres par `XtreamAccountManager`. Au premier demarrage apres mise a jour, les anciens `accounts_json`, `m3u_url`, `epg_url` et `active_playlist_source` sont copies dans `Profil principal` si une source existe.
- `PlaylistWeb`: profil Android reserve aux playlists envoyees depuis `/playlist/` ou le QR web. `XtreamAccountManager` met a jour ce profil si son nom est toujours `PlaylistWeb`; si l'utilisateur le renomme, le prochain push web cree un nouveau profil `PlaylistWeb`.
- `device_status.php` ne doit renvoyer `playlist_config` que pour un appareil actif avec `device_token` rattache a une session d'activation `validated`; un jeton `pending` cree par `register.php` ne doit jamais suffire a renvoyer les identifiants playlist.
- Apres un enregistrement playlist web valide, `save_playlist_config.php` et `/playlist/` marquent les sessions pending de l'appareil comme `validated` afin que le token de polling courant puisse recevoir cette playlist meme si l'app a conserve un token plus ancien qu'une session recente.
- `create_playlist_setup_session.php` cree une session deja `validated` et rattache le `device_token` courant a cette session dans `activation_session_tokens`; sinon `device_status.php` refuserait de livrer `playlist_config` malgre `playlist_configured=true`.
- `ActivationRepository.registerDevice()` ne doit pas remplacer un `device_token` local deja present par le nouveau jeton `pending` du register, afin de conserver la session validee et d'eviter une restauration non maitrisee apres redemarrage.
- `multi_profile` dans `app_feature_access`: controle la creation/modification multi-profils. Les defaults autorisent Premium et essai 7 jours, et verrouillent Free Ads avec couronne cote TV.

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
- Ne jamais livrer `playlist_config` avec un token de session `pending`; il peut servir au polling/setup, pas a retourner des secrets playlist.
- Ne pas demarrer l'essai trop tot si le flux exige validation Xtream.
- Preserver l'ecran essai expire, achat licence et gratuit avec pubs.
- Ne pas changer le sens de `activation_type` sans mettre a jour Android et backend.
- Toujours verifier les retours publics si changement backend.
- L'alerte Xtream doit masquer les mots de passe et ne remonter au backend que serveur, type d'erreur, message court, detail technique non secret, code TV et version app.
- Ne pas tester seulement l'ancien compte local au demarrage: rafraichir le statut backend avant le controle Xtream pour detecter une playlist remplacee depuis le portail web.
- Si Xtream et M3U sont tous les deux renseignes, ne pas choisir automatiquement une source distante a chaque refresh: conserver la source active locale choisie par l'utilisateur.
- Les popups et messages de synchronisation doivent afficher les donnees de la source active: identifiants Xtream si Xtream est actif, lien M3U si M3U est actif.
- L'activation/licence reste globale appareil; les profils ne changent que la source playlist active. La selection d'un profil reprojette ses champs vers les anciens flows pour preserver les repositories existants.
- Le multi-profil doit rester derriere `PremiumFeature.MULTI_PROFILE` et `app_feature_access.multi_profile`; ne pas autoriser l'ajout/modification de profils en Free Ads sans changement admin explicite.

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
- 2026-07-01: remplacement du fond image du splash par une video Compose Media3 muette et bouclee, avec logo/progress/status conserves au-dessus.
- 2026-07-01: ajout d'un garde `localStateReady` pour empecher le rendu transitoire de l'ecran activation apres le splash sur appareil deja actif.
- 2026-07-01: ajout de l'URL EPG dans le payload playlist chiffre, affichage/edition dans Info compte, et page web `/playlist/` pour pousser Xtream/EPG vers une TV par code.
- 2026-07-01: `/playlist/` passe en onglets `Code Xtream`, `Lien M3U`, `Lien EPG`; le payload chiffre peut aussi porter `m3u_url`, et un push cree une notification d'information ciblee sur la TV.
- 2026-07-01: Info compte ajoute la source active exclusive Xtream/M3U; `device_status.php` considere aussi `m3u_url` comme playlist configuree sans marquer Xtream configure.
- 2026-07-01: correction source active M3U dans le splash et le popup manuel de synchronisation; suppression du flash de l'ancien visuel splash dans `AppNavigation`.
- 2026-07-02: `MainActivity` devient l'activite launcher avec theme splash systeme, porte les checks/preloads startup, et `StartupHandoffScreen` est supprime.
- 2026-07-02: `MainActivity` conserve la preview splash jusqu'a la premiere frame Compose et `AppNavigation` bloque le rendu activation tant que l'etat local n'est pas pret pour eviter le flash activation avant Home.
