# SmartVision / SVPlayer - Project Notes

Derniere mise a jour: 2026-06-25.

Ce document decrit l'etat fonctionnel et technique actuel du projet pour permettre a un agent IA de reprendre rapidement sans casser le fonctionnement existant.

## Resume executif

SmartVision est une application Android TV native qui lit les contenus Xtream configures par l'utilisateur. Le projet comprend:

- une application Android TV Kotlin/Compose;
- un backend PHP/MySQL deploye sur `https://smartvisions.net`;
- un site public de vente/activation;
- un espace compte client;
- un panel admin;
- un systeme de mise a jour APK in-app.

Le rendu visuel valide suit une direction premium Android TV: fond sombre bleu/noir, cartes larges, focus cyan/blanc tres visible, navigation D-pad, lecteur plein ecran natif.

## Identite projet

```text
Nom produit: SmartVision
Projet Gradle: SVPlayer
Application ID: com.smartvision.svplayer
Activite launcher TV: com.smartvision.svplayer/.SplashActivity
Activite principale: com.smartvision.svplayer/.MainActivity
Min SDK: 23
Compile SDK: 36
Target SDK: 36
Version actuelle: 0.1.10
VersionCode actuel: 11
Backend: https://smartvisions.net
```

## Etat release connu

Release compilee, signee, deployee et verifiee le 2026-06-25:

```text
version_code: 13
version_name: 0.1.10
affichage: 0.1.10 (13)
apk_file: smartvision-tv-v13-490f9767.apk
apk_url: https://smartvisions.net/downloads/smartvision-tv-v13-490f9767.apk
stable_url: https://smartvisions.net/downloads/smartvision-tv.apk
sha256: 490f9767c2056965f5efadbb05ba92ab947807da13fd39323ca9230d8cbd59e8
size: 38504849
```

Le fichier versionne et l'URL stable ont ete telecharges avec succes apres deploiement.
Le SHA-256 distant correspond au SHA-256 de l'APK release local.

Endpoint update verifie:

```text
GET https://smartvisions.net/api/app_update.php?version_code=0&version_name=qa
```

## Etat Git actuel

Le depot Git est a la racine `TvPlayerApp/`, pas dans `SVPlayer/`.

Etat local attendu apres le nettoyage/documentation:

```text
M SVPlayer/AGENTS.md
M SVPlayer/PROJECT_NOTES.md
D SVPlayer/.kotlin/errors/errors-1781749264597.log
D SVPlayer/home-implemented-qa.png
```

Les deux suppressions sont des artefacts generes qui ne devraient pas etre suivis. Ne pas les restaurer sauf demande explicite.

## Secrets et fichiers locaux

`SVPlayer/local.properties` contient des valeurs sensibles ou locales:

- `sdk.dir`
- identifiants Xtream eventuels;
- `CPANEL_API_KEY`;
- `CPANEL_HOST`;
- `DOMAINE_SERVER`;
- identifiants MySQL;
- configuration release signing.

Regles:

- ne jamais afficher le contenu de `local.properties`;
- ne jamais commiter `local.properties`;
- ne jamais ecrire les secrets dans Kotlin, PHP, JS ou Markdown;
- utiliser `local.properties.example` pour documenter les cles attendues sans valeurs reelles.

## Tooling Android

Le projet utilise Gradle wrapper.

Versions principales:

```text
Gradle wrapper: 8.14.3
Android Gradle Plugin: 8.13.0
Kotlin: 2.1.21
Compose BOM: 2025.05.01
Room: 2.7.1
Media3: 1.6.1
Retrofit: 2.11.0
OkHttp: 4.12.0
WorkManager: 2.10.1
ZXing: 3.5.3
Coil: 2.7.0
```

Le build demande Java 21 via:

```kotlin
kotlin {
    jvmToolchain(21)
}
```

Commandes usuelles:

```powershell
cd 'C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer'
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Si Java 8 est dans le PATH ou si le JBR Android Studio local est casse, definir un JDK 21:

```powershell
$env:JAVA_HOME='C:\Users\ONEDEV\.codex\cache\jdk21\extracted\jdk-21.0.11+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

## Build Gradle

Fichier:

```text
app/build.gradle.kts
```

Points importants:

- `applicationId = "com.smartvision.svplayer"`
- `versionCode = 6`
- `versionName = "0.1.3"`
- `BuildConfig.ACTIVATION_BASE_URL` vient de `DOMAINE_SERVER` dans `local.properties`, fallback `https://smartvisions.net/`.
- La signature release lit `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` depuis `local.properties`.
- `buildConfig = true`
- `compose = true`
- `isMinifyEnabled = false` en release.

## Manifest Android

Fichier:

```text
app/src/main/AndroidManifest.xml
```

Permissions:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `REQUEST_INSTALL_PACKAGES`

Features:

- `android.software.leanback` requis;
- touchscreen non requis.

Application:

- `android:name=".SVPlayerApplication"`
- `android:banner="@drawable/tv_banner"`
- `android:icon="@mipmap/ic_launcher"`
- `android:roundIcon="@mipmap/ic_launcher_round"`
- `networkSecurityConfig="@xml/network_security_config"`
- `usesCleartextTraffic="true"` pour compatibilite flux Xtream HTTP.

Activities:

- `SplashActivity`, exported, `LEANBACK_LAUNCHER`, paysage.
- `MainActivity`, non exported, paysage.

FileProvider:

- utilise pour installer les APK telecharges par la mise a jour in-app.

## Architecture Android active

Package actif principal:

```text
app/src/main/java/com/smartvision/svplayer/ui/
```

Navigation active:

```text
ui/navigation/AppNavigation.kt
```

Ancienne architecture encore presente:

```text
feature/home
feature/live
feature/movies
feature/series
feature/player
feature/settings
feature/account
```

Ces fichiers `feature/*` ne semblent pas etre utilises par `MainActivity`. Avant d'en modifier un, verifier qu'il est importe par une route active. Les ecrans en production sont dans `ui/*`.

## Dependency container

Fichier:

```text
core/data/AppContainer.kt
```

Responsabilites:

- cree `XtreamAccountManager`;
- cree Room `SVDatabase`;
- configure Retrofit Xtream avec base URL dummy `http://127.0.0.1/` et interceptor qui redirige vers l'hote du compte Xtream actif;
- configure Retrofit activation/update avec `BuildConfig.ACTIVATION_BASE_URL`;
- expose repositories et use cases:
  - `activationRepository`;
  - `appUpdateRepository`;
  - `xtreamRepository`;
  - `userContentRepository`;
  - `catalogRepository`;
  - `settingsRepository`;
  - `synchronizeCatalog`;
  - `toggleFavorite`;
  - `buildPlaybackRequest`;
  - `savePlaybackProgress`.

`MainActivity` fournit `AppContainer` via `LocalAppContainer`.

## Navigation Android

Routes dans `AppNavigation.kt`:

```text
home
profile
continue_watching
trending
live_tv
movies
series
settings
sync/settings
player/{channelId}
movie_player/{movieId}
movie_detail/{movieId}
episode_player/{episodeId}
series_detail/{seriesId}
```

Header tabs:

```text
Accueil -> home
Live TV -> live_tv
Films -> movies
Series -> series
```

Back behavior:

- sur Home, Back ouvre une confirmation quitter;
- sur lecteur, Back masque menu/overlay ou revient a l'ecran precedent selon l'etat;
- sur detail/profil/settings, Back revient en arriere.

## Activation Android

Fichiers:

```text
data/activation/ActivationApiService.kt
data/activation/ActivationRepository.kt
ui/activation/ActivationViewModel.kt
ui/activation/ActivationScreen.kt
ui/activation/XtreamQrSetupPanel.kt
```

Flux:

1. `ActivationViewModel` lit ou cree `device_id` local.
2. Il appelle `device_status.php`.
3. Si non active, il cree une session via `create_activation_session.php`.
4. L'UI affiche `short_code` et `qr_url`.
5. Le polling appelle `device_status.php` toutes les `polling_interval` secondes.
6. Si le statut devient actif, Home est affiche.
7. Si `playlist_config` est renvoye avec un token valide, les identifiants Xtream sont stockes dans `XtreamAccountManager`.

`ActivationUiState.shouldShowLicenseKey` est vrai pour:

```text
activationType == "trial_demo" || activationType == "free_ads"
```

## Xtream et catalogue

Fichiers:

```text
core/config/XtreamAccountManager.kt
data/remote/XtreamApiService.kt
data/remote/XtreamApiClient.kt
data/remote/XtreamUrlFactory.kt
data/repository/XtreamRepository.kt
data/repository/DefaultCatalogRepository.kt
```

Endpoints Xtream:

```text
player_api.php?action=get_live_categories
player_api.php?action=get_live_streams
player_api.php?action=get_vod_categories
player_api.php?action=get_vod_streams
player_api.php?action=get_vod_info
player_api.php?action=get_series_categories
player_api.php?action=get_series
player_api.php?action=get_series_info
```

URL streaming:

```text
Live TS:     {host}/live/{username}/{password}/{stream_id}.ts
Live HLS:    {host}/live/{username}/{password}/{stream_id}.m3u8
Movie:       {host}/movie/{username}/{password}/{stream_id}.{extension}
Episode:     {host}/series/{username}/{password}/{episode_id}.{extension}
```

`XtreamRepository` garde un cache memoire pour:

- categories live/movies/series;
- streams live par categorie et par ID;
- films par categorie et par ID;
- series par categorie et par ID;
- details film/serie;
- episodes par serie et par ID;
- elements precedents/suivants pour lecteur.

`DefaultCatalogRepository` synchronise dans Room:

- compte Xtream;
- categories live;
- streams live;
- categories VOD;
- films;
- categories series;
- series.

Les episodes sont charges a la demande par `get_series_info`.

## Room database

Fichier:

```text
data/local/SVDatabase.kt
```

Nom DB:

```text
svplayer.db
```

Version:

```text
2
```

Tables Android:

```text
profiles
categories
live_streams
movies
series
episodes
favorites
playback_progress
sync_state
```

Migration 1 -> 2:

- ajoute `title`, `subtitle`, `imageUrl`, `parentContentId` a `playback_progress`.

## Favoris et historique

Fichier:

```text
data/repository/UserContentRepository.kt
```

Types:

```text
live
movie
series
episode
```

Fonctions:

- observer les favoris par type;
- observer l'historique recent;
- enrichir une progression avec titre/image depuis le catalogue local;
- toggle favorite;
- sauvegarder la progression.

Les categories speciales `Favoris` et `Historiques` sont ajoutees en tete des categories Live, Films et Series.

## Home

Fichiers:

```text
ui/home/HomeScreen.kt
ui/home/HomeViewModel.kt
ui/home/HomeHeroBanner.kt
ui/home/HomeCategoryCard.kt
ui/home/ContinueWatchingRow.kt
ui/home/HomeCollectionsScreen.kt
```

Etat:

- header horizontal avec tabs;
- boutons icones a droite: notifications, profil, parametres;
- bouton cle licence visible selon mode d'utilisation;
- hero publicitaire en slider avec CTA;
- trois cartes principales Live TV, Films, Series;
- section Reprendre la lecture;
- section Tendances;
- cartes avec images de fond locales.

Assets principaux:

```text
drawable-nodpi/live_tv_bg.png
drawable-nodpi/films_bg.png
drawable-nodpi/series_bg.png
drawable-nodpi/home_hero_slide_*.png
```

## Live TV

Fichiers:

```text
ui/live/LiveTvScreen.kt
ui/live/LiveTvViewModel.kt
```

Comportement:

- charge categories live Xtream;
- selectionne automatiquement la premiere categorie, sauf si favoris existent;
- charge les chaines de la categorie;
- focus D-pad entre categories, chaines et apercu;
- OK sur chaine lance le mini lecteur;
- OK sur meme chaine ouvre le lecteur plein ecran;
- bouton Regarder ouvre le plein ecran;
- bouton Favori toggle favori;
- Mini preview ExoPlayer muet, sans controle, avec fallback HLS;
- si aucun compte Xtream, affiche QR de configuration Xtream.

## Films

Fichiers:

```text
ui/movies/MoviesScreen.kt
ui/movies/MoviesViewModel.kt
ui/detail/MovieDetailScreen.kt
```

Comportement:

- categories a gauche;
- zone principale en grille de cards;
- recherche dans le header catalogue;
- 5 cards par ligne via `MediaCatalogDimens.MediaGridColumns`;
- OK sur film ouvre detail film;
- detail film affiche metadata et CTA lecture;
- lecture via `movie_player/{movieId}`;
- favoris et historiques integres.

## Series

Fichiers:

```text
ui/series/SeriesScreen.kt
ui/series/SeriesViewModel.kt
ui/detail/SeriesDetailScreen.kt
```

Comportement:

- categories a gauche;
- zone principale en grille de cards;
- recherche dans le header catalogue;
- OK sur serie ouvre detail serie;
- detail serie charge les episodes par saison;
- lecture via `episode_player/{episodeId}`;
- metadata visible chargee progressivement pour limiter le cout;
- favoris et historiques integres.

## Lecteur plein ecran

Fichier:

```text
ui/player/FullScreenPlayerScreen.kt
```

Kinds:

```text
FullScreenContentKind.Live
FullScreenContentKind.Movie
FullScreenContentKind.Episode
```

Fonctions:

- ExoPlayer plein ecran;
- fallback sur URL alternative si possible;
- overlay sombre;
- barre progression avec position/duree si disponible;
- `Direct` pour Live sans duree;
- boutons controle TV;
- pause/play;
- seek -10/+10;
- sous-titres et settings overlay;
- changement chaine Live avec Haut/Bas si overlay visible;
- precedent/suivant pour films et episodes;
- sauvegarde progression pour films/episodes;
- proposition episode suivant.

## Profil

Fichier:

```text
ui/profile/ProfileScreen.kt
```

Fonctions:

- consulte licence SmartVision;
- affiche identifiant appareil;
- affiche statut, type activation et expiration;
- affiche compte Xtream, host, user masque, expiration si connue;
- affiche compteurs Live/Films/Series;
- QR pour acheter/prolonger licence;
- QR pour configurer Xtream;
- QR pour achat Xtream futur;
- CTA synchroniser et parametres.

Modeles d'utilisation:

```text
Trial: essai gratuit 7 jours sans pub
Premium: licence payante 1 mois, 12 mois ou a vie, sans pub
FreeAds: gratuit avec pubs apres essai
```

## Parametres

Fichier:

```text
ui/settings/SettingsScreen.kt
```

Fonctions:

- ratio video: Fit, Fill, Zoom;
- animations;
- reconnexion automatique;
- version app;
- recherche mise a jour;
- synchronisation catalogue;
- vider donnees locales;
- ajouter/modifier/supprimer/connecter comptes Xtream;
- navigation Haut/Bas dans le formulaire compte Xtream.

## Mise a jour in-app

Fichiers Android:

```text
data/update/AppUpdateApiService.kt
data/update/AppUpdateRepository.kt
ui/update/AppUpdateViewModel.kt
ui/update/AppUpdateDialog.kt
```

Fichiers serveur:

```text
server/public_html/api/app_update.php
server/public_html/download.php
server/public_html/downloads/smartvision-tv.version.json
server/public_html/downloads/*.apk
```

Flux:

1. App appelle `api/app_update.php?version_code={current}&version_name={current}`.
2. Serveur lit `downloads/smartvision-tv.version.json`.
3. Si `latest_version_code` est superieur, l'app affiche `AppUpdateDialog`.
4. L'app telecharge l'APK HTTPS.
5. Hash SHA-256 verifie si fourni.
6. L'app ouvre l'installeur Android via FileProvider.

## Backend PHP

Racine locale:

```text
server/public_html/
```

### Demarrage local du site

Le site peut etre lance localement tout en utilisant la base MySQL de production:

```powershell
cd 'C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer'
.\scripts\start_web_dev.ps1
```

URL par defaut:

```text
http://127.0.0.1:8080/
```

Le script lit les secrets depuis `local.properties`, les injecte uniquement dans
le processus PHP et utilise l'hote cPanel lorsque `MYSQL_HOST=localhost`.
Il ne deploie aucun fichier. Les actions effectuees dans le site local modifient
la base de production; utiliser des donnees de test identifiables.

Structure:

```text
api/
  config.php
  helpers.php
  create_activation_session.php
  device_status.php
  validate_activation.php
  start_trial.php
  create_playlist_setup_session.php
  save_playlist_config.php
  app_update.php
  commerce.php
activate/
  index.php
account/
  index.php
admin/
  bootstrap.php
  index.php
  logout.php
assets/
  site.css
  account.css
  admin.css
  mobile.css
  activation.js
  account.js
  admin.js
sql/
  init_activation_tables.sql
download.php
index.php
```

Regles backend:

- PHP 8.x;
- PDO;
- JSON pour API;
- CORS gere par `apply_api_headers`;
- pas d'erreur SQL exposee;
- codes SmartVision hashes;
- identifiants Xtream chiffres;
- secrets serveur via config privee generee au deploiement;
- admin avec session, CSRF et audit logs.

## Tables MySQL backend

Fichier:

```text
server/public_html/sql/init_activation_tables.sql
```

Tables:

```text
devices
activation_sessions
activation_session_tokens
activation_codes
activation_code_metadata
admin_audit_logs
site_users
device_activations
activation_orders
device_playlist_configs
app_settings
```

Reglages par defaut:

```text
trial_duration_days = 7
activation_session_minutes = 15
polling_interval_seconds = 5
activation_duration_days = 365
payment_mode = test
support_email = support@smartvisions.net
```

## API activation

### POST `api/create_activation_session.php`

Input:

```json
{
  "device_id": "uuid",
  "device_name": "Android TV",
  "app_version": "0.1.3"
}
```

Actions:

- upsert `devices`;
- expire les anciennes sessions;
- cree `activation_sessions`;
- genere `short_code`;
- genere et stocke `device_token` hashe;
- retourne `qr_url`, expiration et polling interval.

### GET `api/device_status.php`

Query:

```text
device_id
device_token optional
```

Retourne:

- `pending`;
- `active`;
- `expired`;
- `blocked`.

Si actif et playlist configuree, renvoie `playlist_config` uniquement si `device_token` est valide.

### POST `api/validate_activation.php`

Valide un code SmartVision pour une session TV.

### POST `api/start_trial.php`

Active l'essai gratuit 7 jours si l'appareil ne l'a jamais utilise.

### POST `api/create_playlist_setup_session.php`

Cree un lien QR de configuration Xtream pour un appareil deja active.

### POST `api/save_playlist_config.php`

Enregistre host/username/password Xtream chiffres pour l'appareil.

## Site public et parcours client

`server/public_html/index.php`:

- page d'accueil marketing;
- vente app;
- prix: 1 mois 2 EUR, 12 mois 15 EUR, a vie 20 EUR;
- liens telechargement, activation, compte, revendeur futur.

`server/public_html/account/index.php`:

- inscription;
- connexion;
- redirection automatique vers `?section=buy-license` apres connexion lorsqu'un achat est demande;
- navigation multi-page avec une seule section rendue par URL:
  - `?section=licenses`;
  - `?section=buy-license`;
  - `?section=activate`;
  - `?section=orders`;
  - `?section=download`;
  - `?section=profile`;
- cartes de licences sans offre preselectionnee;
- liens de paiement Gammal Tech configurables dans le panel admin;
- liste commandes/licences;
- telechargement APK.

Le menu admin `Packs paiement` stocke les URLs dans `app_settings`:

```text
gammal_payment_month_1_url
gammal_payment_month_1_enabled
gammal_payment_year_1_url
gammal_payment_year_1_enabled
gammal_payment_lifetime_url
gammal_payment_lifetime_enabled
```

Le callback public est `/payment-callback/`. Il affiche un retour utilisateur
sans accorder de licence sur la seule base de parametres URL non verifies.

En environnement `development`, un quatrieme pack temporaire `Simulation DEV`
est affiche dans `?section=buy-license`. Il simule un callback Gammal Tech avec
un jeton de session a usage unique, puis cree une commande test et une licence
valable 1 jour. Ce pack est absent lorsque `SMARTVISION_ENV` n'est pas
`development`.

`server/public_html/activate/index.php`:

- saisie code court TV;
- saisie code SmartVision;
- lancement essai gratuit;
- apres activation, saisie identifiants Xtream.

`server/public_html/admin/index.php`:

- login admin;
- stats;
- commandes;
- utilisateurs;
- appareils;
- codes activation;
- generation, activation/desactivation, revocation, suppression;
- extension activation;
- audit logs.

## Deploiement cPanel

Script:

```text
scripts/deploy_activation_phase1.ps1
```

Options:

```powershell
-SkipInstall
-SkipTests
```

Commande recommandee apres build release:

```powershell
cd 'C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer'
.\scripts\deploy_activation_phase1.ps1 -SkipInstall
```

Regle de publication obligatoire:

- chaque nouvel APK release doit avoir un `versionCode` strictement superieur a celui de la release precedente;
- la construction locale seule ne suffit pas;
- apres `assembleRelease`, executer le script de deploiement pour uploader l'APK versionne, l'URL stable et `smartvision-tv.version.json`;
- verifier ensuite `api/app_update.php?version_code={ancienne_version}` et l'accessibilite de l'APK retourne;
- ne jamais annoncer une release terminee si le nouveau manifeste n'est pas visible sur le serveur.

Le script:

- lit `CPANEL_API_KEY`, `CPANEL_HOST`, `DOMAINE_SERVER`, `MYSQL_*` depuis `local.properties`;
- detecte le document root;
- cree dossiers distants;
- upload PHP/CSS/JS/assets/SQL;
- genere/upload config privee;
- si APK release present, upload APK versionne et manifest update;
- peut executer install SQL si `-SkipInstall` absent;
- execute tests publics si `-SkipTests` absent.

Tests du script:

- site public;
- portail activation;
- commerce test;
- validation activation;
- configuration Xtream chiffree;
- admin login/actions/logout;
- endpoint update.

## Assets importants

Application:

```text
app/src/main/res/drawable-nodpi/live_tv_bg.png
app/src/main/res/drawable-nodpi/films_bg.png
app/src/main/res/drawable-nodpi/series_bg.png
app/src/main/res/drawable-nodpi/smartvision_splash_full.png
app/src/main/res/drawable-nodpi/smartvision_mark.png
app/src/main/res/drawable-nodpi/smartvision_wordmark.png
app/src/main/res/drawable-nodpi/tv_banner.png
```

Site:

```text
server/public_html/assets/images/app-live-tv.png
server/public_html/assets/images/app-movies.png
server/public_html/assets/images/smartvision-mark.png
server/public_html/assets/images/smartvision-wordmark.png
```

## Emulateur leger et validation manuelle

Le PC de developpement est limite en ressources. L'ancien emulateur Android TV lourd `Television_1080p` a ete supprime et ne doit plus etre utilise.

AVD obligatoire pour toutes les installations Android TV:

```text
SmartVision_TV_720p_Light
```

Contraintes:

- ne pas utiliser Android Studio;
- ne pas recreer d'emulateur;
- ne pas utiliser `Television_1080p`;
- ne pas lancer de tests UI lourds;
- ne pas supprimer de fichiers SDK, AVD, Gradle ou systeme;
- ne pas faire de nettoyage automatique sans accord explicite;
- ne pas modifier la configuration de l'emulateur sans autorisation.

Role attendu d'un agent pour une verification Android TV:

1. compiler l'application;
2. installer ou uploader l'APK sur `SmartVision_TV_720p_Light`;
3. verifier que l'installation s'est bien terminee;
4. indiquer que l'application est prete pour tests manuels par le user.

Ne pas executer de scenario fonctionnel complet: les tests fonctionnels et visuels sont faits manuellement par le user.

Commande de lancement si l'emulateur n'est pas deja ouvert:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd SmartVision_TV_720p_Light -no-audio -no-boot-anim -gpu auto -memory 1536
```

Attendre que l'emulateur soit pret:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed
```

Le resultat attendu est:

```text
1
```

Compiler:

```powershell
.\gradlew.bat assembleDebug
```

Installer l'APK debug:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r -d .\app\build\outputs\apk\debug\app-debug.apk
```

Si le chemin APK est different, trouver automatiquement le bon fichier dans:

```text
app/build/outputs/apk
```

Resume final attendu:

- build reussi ou echoue;
- APK installe ou non;
- emulateur utilise: `SmartVision_TV_720p_Light`;
- erreurs eventuelles;
- confirmation que le user peut tester manuellement.

Captures QA historiques:

```text
screenshots/codex/
```

Ne pas supprimer les captures QA sans demande explicite; elles servent souvent de reference visuelle.

## Points de vigilance

- `feature/*` et `ui/*` coexistent. Modifier l'ecran vraiment branche dans `AppNavigation.kt`.
- `local.properties` est necessaire pour build release et deploiement, mais ne doit jamais etre affiche.
- La mise a jour in-app requiert `REQUEST_INSTALL_PACKAGES` et l'autorisation utilisateur d'installer depuis SmartVision.
- Les flux Xtream peuvent etre HTTP; `usesCleartextTraffic=true` est intentionnel.
- Les listes Live/Films/Series dependent d'un compte Xtream configure. Sans compte, afficher QR et ne pas crasher.
- Le polling activation peut recevoir playlist_config; cette mise a jour doit rester robuste et ne pas fermer l'app.
- La progression ne doit pas etre sauvegardee pour Live.
- Les favoris et historiques doivent rester des categories speciales en tete.
- Les ecrans TV doivent garder grandes dimensions, focus visible et navigation D-pad.
- Eviter les refactors larges: les ecrans ont ete ajustes visuellement par iterations et captures.

## Commandes utiles

Verifier etat Git:

```powershell
git status --short
```

Compiler debug:

```powershell
.\gradlew.bat assembleDebug
```

Compiler release:

```powershell
.\gradlew.bat assembleRelease
```

Deployer site/backend/APK:

```powershell
.\scripts\deploy_activation_phase1.ps1 -SkipInstall
```

Tester update:

```powershell
Invoke-RestMethod 'https://smartvisions.net/api/app_update.php?version_code=0&version_name=qa'
```

Verifier APK serveur:

```powershell
Invoke-WebRequest 'https://smartvisions.net/downloads/smartvision-tv.apk' -Method Head -UseBasicParsing
```

## Definition de termine pour une tache

Une tache Android est terminee si:

- le bon fichier actif a ete modifie;
- l'UI reste conforme au design Android TV;
- la navigation D-pad est testee ou raisonnee;
- le build pertinent passe, ou l'impossibilite est expliquee;
- aucune donnee sensible n'a ete exposee.

Une tache backend est terminee si:

- les API renvoient JSON propre;
- les erreurs internes restent masquees;
- les requetes SQL sont preparees;
- le deploiement est effectue via script si demande;
- les endpoints publics critiques sont testes.
