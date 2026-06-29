# SmartVision Project Overview

Derniere mise a jour: 2026-06-29.

## 1. Objectif

Donner aux agents IA une vue courte et fiable du projet avant de lire un domaine specialise.

## 2. Produit

SmartVision est une application Android TV native. Elle ne fournit pas de contenu IPTV par defaut. L'utilisateur active son appareil, configure ses identifiants Xtream, puis l'application charge Live TV, Films et Series depuis son propre fournisseur.

Le projet inclut:
- application Android TV Kotlin/Compose;
- backend PHP/MySQL sous `server/public_html/`;
- site public `https://smartvisions.net`;
- espace compte client;
- panel admin;
- API d'activation, configuration, publicite, tracking, notifications et update APK.

## 3. Stack principale

- Android: Kotlin, Jetpack Compose, Navigation Compose, Room, DataStore.
- Video: AndroidX Media3 ExoPlayer.
- Reseau: Retrofit et OkHttp.
- Backend: PHP 8.x, PDO, MySQL/MariaDB, cPanel.
- Build: Gradle wrapper, JDK 21.

## 4. Points d'entree Android

- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`

Routes actives principales:
- `home`
- `live_tv`
- `movies`
- `series`
- `youtube`
- `settings`
- `profile`
- `notifications`
- `player/{channelId}`
- `movie_player/{movieId}`
- `movie_detail/{movieId}`
- `episode_player/{episodeId}`
- `series_detail/{seriesId}`

## 5. Etat local constate

Lu dans `app/build.gradle.kts` le 2026-06-29:
- `compileSdk = 36`
- `minSdk = 23`
- `targetSdk = 36`
- `versionCode = 53`
- `versionName = "0.1.50"`
- release standard sans minify/shrink
- variante `releaseOptimized` avec minify/shrink

Attention: `PROJECT_NOTES.md` contient une release plus ancienne. Pour toute release, verifier `app/build.gradle.kts`, `output-metadata.json` et les endpoints publics.

## 6. Backend et deploiement

Backend local:
- `server/public_html/`

Domain cible:
- `https://smartvisions.net`

Script principal:
- `scripts/deploy_activation_phase1.ps1`

Le script upload les fichiers PHP/JS/CSS/images explicitement, upload l'APK release s'il existe, genere `downloads/smartvision-tv.version.json`, cree une notification de release et teste les endpoints si `-SkipTests` n'est pas utilise.

## 7. Sources de verite rapides

- Routage IA: `docs/ai-knowledge/ROOT.md`
- Architecture courte: ce fichier
- Workflow agent: `docs/ai-knowledge/AGENT_WORKFLOW.md`
- Build/release: `docs/ai-knowledge/technical/android-architecture-build-release.md`
- Backend/admin: `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- Erreurs reutilisables: `TROUBLESHOOTING.md`

## 8. Regles non negociables

- Ne jamais afficher ni hardcoder de secrets.
- Ne jamais modifier `local.properties` pour documenter des valeurs reelles.
- Ne pas restaurer des artefacts generes supprimes sauf demande explicite.
- Ne pas utiliser WebView pour les ecrans natifs ou la lecture video applicative.
- Garder la lecture video native avec Media3 ExoPlayer.
- Toute UI TV actionnable doit etre focusable.
- Ne pas casser les flux valides quand la demande cible un domaine precis.
