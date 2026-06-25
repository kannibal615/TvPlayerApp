# AGENTS.md - SmartVision / SVPlayer

Ce fichier sert de briefing rapide pour tout agent IA qui reprend le projet. Lire aussi `PROJECT_NOTES.md` pour le detail complet.

## Objectif produit

SmartVision est une application Android TV native de type lecteur IPTV. Elle ne fournit aucun contenu par defaut. L'utilisateur doit activer son appareil puis configurer ses propres identifiants Xtream pour charger Live TV, Films et Series.

Le style recherche est une interface Android TV premium, sombre bleu/noir, lisible a 2-3 metres, pilotee au D-pad, avec focus visible et grands composants.

## Regles non negociables

- Ne jamais hardcoder de secrets, identifiants Xtream, cPanel, MySQL ou keystore dans le code source.
- Ne jamais afficher les secrets de `local.properties` dans les logs ou reponses.
- Ne pas versionner `local.properties`, `keystore/`, `.gradle/`, `.kotlin/`, `build/`, `app/build/`.
- Ne pas utiliser WebView pour les ecrans natifs ou la lecture video.
- La lecture video doit rester native avec AndroidX Media3 ExoPlayer.
- L'application ne doit pas proposer de chaines, films, series ou contenus de test comme service final.
- Toute UI TV actionnable doit etre focusable, pas seulement clickable.
- Ne pas casser les ecrans valides existants quand une demande concerne une fonctionnalite ciblee.

## Racines importantes

```text
TvPlayerApp/
  .git/                         Depot Git racine
  .idea/                        Projet IDE
  SVPlayer/                     Projet Android + backend local

SVPlayer/
  app/                          Application Android TV
  server/public_html/           Backend PHP/MySQL et site smartvisions.net
  scripts/deploy_activation_phase1.ps1
  design/                       Concepts et assets brand
  screenshots/codex/            Captures QA et arbres UI
```

## Stack technique

- Kotlin, Jetpack Compose, Compose Material3 et composants TV custom.
- Navigation Compose.
- Room pour cache local, favoris, historique et progression.
- DataStore pour activation, comptes Xtream et reglages.
- Retrofit + OkHttp pour Xtream, activation et update.
- AndroidX Media3 ExoPlayer pour mini lecteur et lecteur plein ecran.
- Backend PHP 8.x + PDO + MySQL/MariaDB sur cPanel.
- Deploiement serveur via API cPanel depuis PowerShell.

## Entree application active

L'application lance `SplashActivity`, puis `MainActivity`.

`MainActivity` charge:

```text
com.smartvision.svplayer.ui.navigation.AppNavigation
```

La navigation active est dans `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`.

Attention: il existe aussi un ancien package `feature/*`. Il semble etre une generation precedente. Les ecrans actifs pour l'app actuelle sont majoritairement dans `ui/*`. Avant modification, verifier les imports de `AppNavigation.kt` pour savoir quel ecran est vraiment utilise.

## Routes actives

```text
home
profile
continue_watching
trending
live_tv
movies
series
settings
player/{channelId}
movie_player/{movieId}
movie_detail/{movieId}
episode_player/{episodeId}
series_detail/{seriesId}
```

## Flux produit actuel

1. `SplashActivity` affiche le splash natif et ouvre `MainActivity`.
2. `AppNavigation` cree `ActivationViewModel`.
3. Si l'appareil n'est pas active, `ActivationScreen` affiche QR code et code court.
4. L'utilisateur active sur `https://smartvisions.net/activate/`.
5. L'app poll `device_status.php`.
6. Quand l'appareil est actif, Home devient accessible.
7. Si aucun compte Xtream n'est configure, les sections Live TV, Films et Series affichent un QR de configuration Xtream.
8. Le portail enregistre les identifiants Xtream chiffrees.
9. L'app recupere la configuration via `device_status.php` avec `device_token`, cree le compte local et synchronise le catalogue.
10. Les sections affichent les donnees Xtream reelles.
11. Les lecteurs utilisent les URL Xtream natives avec Media3.

## Ecrans Android actifs

### Home

Chemins principaux:

```text
ui/home/HomeScreen.kt
ui/home/HomeHeroBanner.kt
ui/home/HomeCategoryCard.kt
ui/home/ContinueWatchingRow.kt
ui/home/HomeCollectionsScreen.kt
```

Etat:

- Header horizontal: Accueil, Live TV, Films, Series.
- Boutons header Home: notification, profil, parametres.
- Hero publicitaire en slider.
- Cartes principales avec assets `live_tv_bg.png`, `films_bg.png`, `series_bg.png`.
- Reprendre la lecture et tendances depuis historique/progression.
- Bouton cle licence visible pour essai gratuit ou gratuit avec pubs.

### Live TV

Chemins:

```text
ui/live/LiveTvScreen.kt
ui/live/LiveTvViewModel.kt
```

Etat:

- Layout valide en 3 colonnes: categories, chaines, apercu.
- Donnees Xtream reelles.
- Categories speciales ajoutees en tete: `Favoris`, `Historiques`.
- OK sur chaine lance le mini lecteur dans Apercu.
- OK une deuxieme fois sur la meme chaine ouvre le plein ecran.
- Mini lecteur Media3 muet par defaut, avec fallback `.m3u8` si `.ts` echoue.
- Si aucun compte Xtream n'est configure, afficher `XtreamQrSetupPanel`.

### Films

Chemins:

```text
ui/movies/MoviesScreen.kt
ui/movies/MoviesViewModel.kt
ui/detail/MovieDetailScreen.kt
```

Etat:

- Colonne categories conservee.
- Liste + apercu fusionnes en grille de cards.
- Grille 5 elements par ligne.
- Categories speciales: `Favoris`, `Historiques`.
- OK sur card film ouvre l'ecran detail film.
- Detail film permet de lancer `movie_player/{movieId}`.
- Si aucun compte Xtream n'est configure, afficher QR Xtream.

### Series

Chemins:

```text
ui/series/SeriesScreen.kt
ui/series/SeriesViewModel.kt
ui/detail/SeriesDetailScreen.kt
```

Etat:

- Colonne categories conservee.
- Liste + apercu fusionnes en grille de cards.
- Grille 5 elements par ligne.
- Categories speciales: `Favoris`, `Historiques`.
- OK sur card serie ouvre l'ecran detail serie.
- Detail serie charge saisons et episodes via `get_series_info`.
- OK sur episode ouvre `episode_player/{episodeId}`.
- Si aucun compte Xtream n'est configure, afficher QR Xtream.

### Lecteur plein ecran

Chemin:

```text
ui/player/FullScreenPlayerScreen.kt
```

Etat:

- ExoPlayer plein ecran.
- Overlay sombre premium avec info programme/contenu.
- Barre de progression corrigee pour VOD/series; Live affiche Direct si pas de duree.
- Boutons: sous-titres, -10, precedent, play/pause, suivant, +10, parametres.
- Pour Live TV, Haut/Bas changent de chaine seulement quand overlay controle est visible.
- Pour films/series, precedent/suivant jouent le film adjacent ou episode adjacent.
- Progression de lecture sauvegardee pour films et episodes, pas pour Live.

### Profil

Chemin:

```text
ui/profile/ProfileScreen.kt
```

Etat:

- Ecran profil client.
- Affiche licence, device_id, mode d'utilisation, expiration, compte Xtream, expiration Xtream si connue, compte actif, compteurs catalogue.
- Actions par QR pour achat/prolongation licence et configuration/achat Xtream.
- Mode produit prevu: essai 7 jours sans pub, premium sans pub, gratuit avec pubs apres essai.

### Parametres

Chemin:

```text
ui/settings/SettingsScreen.kt
```

Etat:

- Reglages experience: ratio video, animations, reconnexion.
- Gestion locale des comptes Xtream.
- Recherche de mise a jour in-app.
- Synchronisation catalogue.
- Nettoyage donnees locales.
- Le formulaire Xtream gere Haut/Bas entre champs pour telecommande.

## Backend actif

Backend local sous:

```text
server/public_html/
```

Domaine cible:

```text
https://smartvisions.net
```

Endpoints importants:

```text
api/create_activation_session.php
api/device_status.php
api/validate_activation.php
api/start_trial.php
api/create_playlist_setup_session.php
api/save_playlist_config.php
api/app_update.php
api/commerce.php
activate/index.php
account/index.php
admin/index.php
download.php
```

Le backend:

- retourne du JSON pour les API;
- utilise PDO et requetes preparees;
- chiffre les identifiants Xtream avant stockage;
- hash les codes SmartVision;
- genere des codes activation apres paiement test;
- gere compte client, commandes, admin, activation, essai gratuit, playlist Xtream et update APK.

## Donnees et persistance

Room database:

```text
data/local/SVDatabase.kt
```

Tables Android:

- `profiles`
- `categories`
- `live_streams`
- `movies`
- `series`
- `episodes`
- `favorites`
- `playback_progress`
- `sync_state`

Les favoris et historiques sont geres par:

```text
data/repository/UserContentRepository.kt
```

## Xtream

API principale:

```text
data/remote/XtreamApiService.kt
data/remote/XtreamUrlFactory.kt
data/repository/XtreamRepository.kt
data/repository/DefaultCatalogRepository.kt
```

Endpoints Xtream utilises:

- `get_live_categories`
- `get_live_streams`
- `get_vod_categories`
- `get_vod_streams`
- `get_vod_info`
- `get_series_categories`
- `get_series`
- `get_series_info`

URL de lecture:

- Live: `/live/{username}/{password}/{stream_id}.ts`
- Fallback Live: `/live/{username}/{password}/{stream_id}.m3u8`
- Film: `/movie/{username}/{password}/{stream_id}.{extension}`
- Episode: `/series/{username}/{password}/{episode_id}.{extension}`

## Build et deploiement

Le projet demande Java 21:

```powershell
cd 'C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer'
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

### Regle obligatoire pour chaque nouvelle release APK

Avant toute generation d'un nouvel APK release:

1. incrementer `versionCode` dans `app/build.gradle.kts`, meme si `versionName` reste identique;
2. construire l'APK release signe;
3. publier immediatement cet APK avec `scripts/deploy_activation_phase1.ps1 -SkipInstall`;
4. verifier que `api/app_update.php` retourne le nouveau `latest_version_code`, le bon `latest_version_name` et une URL APK accessible.

Une release locale non publiee ne doit pas etre consideree comme terminee. Le manifeste
`downloads/smartvision-tv.version.json` doit etre regenere et envoye avec chaque APK afin
que la mise a jour in-app soit detectee.

Version release publiee le 2026-06-25:

```text
versionName: 0.1.14
versionCode: 17
affichage: 0.1.14 (17)
apk: https://smartvisions.net/downloads/smartvision-tv-v17-0e4ec2e9.apk
```

Si le JBR Android Studio local echoue, utiliser un JDK 21 externe ou portable et definir `JAVA_HOME`.

Deploiement serveur:

```powershell
.\scripts\deploy_activation_phase1.ps1 -SkipInstall
```

Le script:

- lit `local.properties`;
- detecte le document root cPanel;
- upload PHP/CSS/JS/assets;
- upload APK release si `app/build/outputs/apk/release/app-release.apk` existe;
- genere `downloads/smartvision-tv.version.json`;
- teste site public, compte/commande, activation, Xtream chiffre et admin.

## Tests Android TV sur PC limite

Regle prioritaire: ne plus utiliser l'ancien AVD lourd `Television_1080p`. Ne pas recreer d'emulateur, ne pas modifier la configuration AVD, ne pas utiliser Android Studio, ne pas lancer de tests UI lourds, ne pas faire de nettoyage automatique sans accord explicite et ne pas supprimer de fichiers SDK, AVD, Gradle ou systeme sans autorisation.

Pour les tests Android TV, utiliser uniquement:

```text
SmartVision_TV_720p_Light
```

Role attendu de l'agent:

1. compiler l'application;
2. installer ou uploader l'APK sur `SmartVision_TV_720p_Light`;
3. verifier que l'installation est terminee;
4. dire au user que l'application est prete pour tests manuels.

Ne pas executer de scenario fonctionnel complet: le user teste manuellement.

Commande de lancement si l'emulateur n'est pas deja ouvert:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd SmartVision_TV_720p_Light -no-audio -no-boot-anim -gpu auto -memory 1536
```

Attendre le boot:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed
```

Le resultat attendu est `1`.

Compiler puis installer:

```powershell
.\gradlew.bat assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r -d .\app\build\outputs\apk\debug\app-debug.apk
```

Si l'APK n'est pas a ce chemin, trouver automatiquement le dernier APK genere dans `app/build/outputs/apk`.

Resume final attendu apres une installation:

- build reussi ou echoue;
- APK installe ou non;
- emulateur utilise: `SmartVision_TV_720p_Light`;
- erreurs eventuelles;
- confirmation que le user peut tester manuellement.

## Validation attendue

Pour une modification UI:

1. compiler;
2. installer sur `SmartVision_TV_720p_Light`;
3. verifier seulement que l'installation s'est bien terminee;
4. laisser le user faire les tests fonctionnels et visuels manuellement.

Pour une modification backend:

1. tester localement si possible;
2. deployer via script;
3. tester URL publique;
4. verifier que les API ne renvoient jamais d'erreur SQL brute;
5. verifier qu'aucun secret n'apparait dans les logs.

## Etat Git actuel a surveiller

Deux fichiers generes ont ete supprimes localement parce qu'ils ne devraient pas etre suivis:

```text
SVPlayer/.kotlin/errors/errors-1781749264597.log
SVPlayer/home-implemented-qa.png
```

Ils apparaitront en `D` tant qu'un commit ne retire pas ces artefacts du depot. Ne pas restaurer ces fichiers sauf demande explicite.

`AGENTS.md` et `PROJECT_NOTES.md` sont les fichiers de documentation de reference.
