# SmartVision AI Knowledge Root

Derniere mise a jour: 2026-06-30.

Ce fichier est le point d'entree court pour les agents IA. Lire ce fichier avant `AGENTS.md`, `PROJECT_NOTES.md` ou tout gros fichier legacy. Les anciens MD restent des sources historiques et ne doivent plus etre lus par defaut.

## Resume global

SmartVision / SVPlayer est une application Android TV native Kotlin/Compose pour lire les contenus Xtream configures par l'utilisateur. Le projet contient aussi un backend PHP/MySQL sur `smartvisions.net`, un site public, un espace compte, un panel admin, des API d'activation, des API de configuration, de tracking et de mise a jour APK.

Etat technique constate le 2026-06-30:
- applicationId: `com.smartvision.svplayer`
- version locale Gradle: `0.1.51` / `versionCode 54`
- entree TV: `SplashActivity -> MainActivity -> ui/navigation/AppNavigation.kt`
- lecture video: AndroidX Media3 ExoPlayer natif
- backend local: `server/public_html/`
- deploiement: `scripts/deploy_activation_phase1.ps1`

## Regles de lecture

1. Lire ce fichier.
2. Identifier le domaine de la demande avec les mots-cles ci-dessous.
3. Lire uniquement les fichiers specialises utiles.
4. Lire ensuite uniquement les fichiers de code concernes.
5. Ne lire les fichiers legacy que si le nouveau systeme manque d'information.
6. Apres modification future, mettre a jour le ou les fichiers MD concernes et `worklog/AI_CHANGELOG.md`.

## Domaines

## Vue projet et workflow agent

Resume:
Contexte global, workflow Codex, regles de mise a jour documentaire et definition de termine.

Lire les fichiers suivants si la demande concerne:
- reprise de projet
- methode agent IA
- documentation
- auto-amelioration
- drift documentaire
- definition de termine
- sources a lire avant intervention

Fichiers detailles:
- `docs/ai-knowledge/PROJECT_OVERVIEW.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md`
- `docs/ai-knowledge/LEGACY_SOURCES.md`

Dependances:
- tous les domaines

Ne pas lire par defaut si la demande concerne uniquement:
- un bug localise deja route par un domaine plus precis

Statut rapide:
- systeme documentaire actif cree le 2026-06-29.

## Activation, licence, essai et Xtream

Resume:
Activation device, licence premium, essai gratuit, mode gratuit avec pubs et configuration Xtream par QR.

Lire les fichiers suivants si la demande concerne:
- activation
- licence
- code SmartVision
- essai gratuit
- trial_pending_xtream
- free_with_ads
- QR Xtream
- device_status
- playlist_config

Fichiers detailles:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`

Dependances:
- Backend/admin/API
- Monetisation, consentement et tracking
- Catalogue et lecture

Ne pas lire par defaut si la demande concerne uniquement:
- focus visuel sans logique d'acces
- build Gradle sans changement produit

Statut rapide:
- flux actif: appareil non active -> activation ou essai -> configuration Xtream -> catalogue.

## Catalogue, synchro, playlist et lecteur

Resume:
Chargement Xtream Live TV, Films, Series, Room cache, favoris, historique, progression et ExoPlayer plein ecran.

Lire les fichiers suivants si la demande concerne:
- Live TV
- films
- series
- playlist
- ExoPlayer
- plein ecran
- progression
- favoris
- historique
- synchronisation catalogue
- get_live_streams
- get_vod_streams
- get_series_info

Fichiers detailles:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`

Dependances:
- Activation, licence, essai et Xtream
- UI TV, focus et navigation
- Monetisation, consentement et tracking

Ne pas lire par defaut si la demande concerne uniquement:
- admin PHP sans effet catalogue
- page marketing publique

Statut rapide:
- les ecrans actifs sont sous `ui/*`, pas sous une ancienne architecture.

## UI TV, focus et navigation telecommande

Resume:
Navigation D-pad, focus Compose, header TV, Home, catalogues, profil, parametres, notifications, YouTube et contraintes visuelles TV.

Lire les fichiers suivants si la demande concerne:
- D-pad
- focus
- FocusRequester
- navigation telecommande
- Home
- continue_watching
- trending
- Live TV layout
- Movies grid
- Series grid
- Settings
- Profile
- YouTube
- i18n
- popup
- consentement TV

Fichiers detailles:
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`

Dependances:
- Catalogue, synchro, playlist et lecteur
- Activation, licence, essai et Xtream
- Monetisation, consentement et tracking

Ne pas lire par defaut si la demande concerne uniquement:
- API backend sans changement d'ecran
- release APK uniquement

Statut rapide:
- toute UI TV actionnable doit rester focusable.

## Backend, admin, API et deploiement serveur

Resume:
PHP/MySQL, endpoints publics, admin, site compte, paiement, email, deploiement cPanel et verification publique.

Lire les fichiers suivants si la demande concerne:
- PHP
- MySQL
- admin/index.php
- api/app_config.php
- api/app_update.php
- deploy_activation_phase1.ps1
- cPanel
- Gammal
- email
- app_feature_access
- notifications
- endpoints publics

Fichiers detailles:
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`

Dependances:
- Activation, licence, essai et Xtream
- Monetisation, consentement et tracking
- Build, release et update APK

Ne pas lire par defaut si la demande concerne uniquement:
- Compose UI sans endpoint
- lecteur ExoPlayer local uniquement

Statut rapide:
- le script de deploiement upload les fichiers explicitement; tout nouveau PHP doit y etre ajoute.

## Monetisation, consentement, tracking et publicite

Resume:
Mode gratuit avec pubs, consentement TV, configuration de fonctionnalites, VAST, evenements pub, anomalies, comportement utilisateur et diagnostics device.

Lire les fichiers suivants si la demande concerne:
- publicite
- ads
- VAST
- Hilltop
- Google IMA
- consentement
- app_config
- app_feature_access
- anomaly-events
- behavior-events
- segmentation utilisateur
- ciblage publicitaire
- diagnostics device
- free_ads
- YouTube premium lock

Fichiers detailles:
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

Dependances:
- Activation, licence, essai et Xtream
- Lecteur plein ecran
- Backend/admin/API

Ne pas lire par defaut si la demande concerne uniquement:
- catalogue Xtream sans gating ni tracking
- build local sans changement runtime

Statut rapide:
- les pubs doivent rester player-only et fail-open selon la logique existante.

## Build, release et update APK

Resume:
Build Android release, signature, versionCode, upload APK, manifeste update, notification de release et verification.

Lire les fichiers suivants si la demande concerne:
- assembleRelease
- release APK
- versionCode
- versionName
- app_update.php
- smartvision-tv.version.json
- smartvision-tv.apk
- Gradle timeout
- JDK 21
- deploy APK

Fichiers detailles:
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `TROUBLESHOOTING.md` seulement si erreur deja documentee

Dependances:
- Backend/admin/API
- Vue projet et workflow agent

Ne pas lire par defaut si la demande concerne uniquement:
- simulation documentation sans build
- UX non livree

Statut rapide:
- pour une release, bypass `compileDebugKotlin` et `testDebugUnitTest`; lancer directement `assembleRelease` avec timeout 15 min.
- avant/apres build release, utiliser `scripts/guard_release_version.ps1`; apres un nouveau build release livrable, deployer aussi le backend avec `scripts/deploy_activation_phase1.ps1`.

## Decisions, legacy et rapports

Resume:
Decisions structurantes, sources legacy conservees, rapport de migration et rapport de test du systeme de connaissance.

Lire les fichiers suivants si la demande concerne:
- pourquoi ce systeme existe
- anciennes docs
- decision deja prise
- simulation documentaire
- migration documentation

Fichiers detailles:
- `docs/ai-knowledge/LEGACY_SOURCES.md`
- `docs/ai-knowledge/MIGRATION_REPORT.md`
- `docs/ai-knowledge/KNOWLEDGE_SYSTEM_TEST_REPORT.md`
- `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md`
- `docs/ai-knowledge/decisions/2026-06-29-ai-knowledge-router.md`
- `docs/ai-knowledge/decisions/2026-06-29-native-tv-player.md`
- `docs/ai-knowledge/decisions/2026-06-29-documentation-update-policy.md`

Dependances:
- tous les domaines

Ne pas lire par defaut si la demande concerne uniquement:
- correction locale simple deja routee

Statut rapide:
- anciens MD conserves comme sources legacy, non supprimes.

## Regles de mise a jour apres intervention

Apres toute modification future, verifier:
- fonctionnalite changee
- workflow utilisateur change
- regle metier changee
- dependance changee
- ecran change
- endpoint, table ou parametre admin change
- fichier MD a mettre a jour
- nouvelle decision a documenter

Si oui:
- mettre a jour le fichier specialise concerne;
- ajouter une entree courte dans `docs/ai-knowledge/worklog/AI_CHANGELOG.md`;
- ajouter une decision dans `docs/ai-knowledge/decisions/` si le choix est structurant;
- documenter toute erreur reutilisable dans `TROUBLESHOOTING.md`.
