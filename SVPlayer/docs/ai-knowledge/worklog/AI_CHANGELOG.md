# AI Changelog

## 2026-06-30 - Correction detection Xtream avec ancien cache local

Type:
- android
- diagnostic
- documentation

Resume:
- Diagnostic du cas TV `LAUU9M`: le serveur Xtream de test timeout sur `player_api.php`, donc l'app devait detecter une erreur reseau.
- Correction du splash: lecture de `device_status.php` avant verification Xtream pour importer la playlist serveur la plus recente avant de tester.
- Correction navigation: verification Xtream obligatoire au premier affichage actif, sans forcer une resynchronisation catalogue si elle n'est pas due.
- Correction UI: etat `checking` considere bloquant et routes detail/player bloquees si Xtream est indisponible.
- Correction lecteur: un buffering persistant ne reste plus en spinner infini; il met l'etat Xtream en erreur, affiche un message et envoie `XTREAM_FAILED`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

## 2026-06-30 - Diagnostics admin et gating Xtream startup

Type:
- fonctionnalite
- backend
- android
- documentation

Resume:
- Refonte de `Diagnostics` admin en onglets Synthese, AutoSync, Anomalies App, Info Serveur et Journal.
- Ajout d'un etat Xtream central avec verification rapide au splash, classification des erreurs, popup TV, notification locale et anti-doublon d'anomalies.
- Blocage Home/Header des entrees Live TV / Movies / Series et des reprises de lecture quand Xtream est indisponible.
- Navigation catalogue basculee vers les donnees locales Room via `CatalogRepository`.
- Synchronisation complete durcie: verification Xtream avant sync, progression `SyncStatus.Running`, remplacement local apres recuperation complete des reponses principales.
- AutoSync retente automatiquement seulement les erreurs reseau.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-06-30-xtream-startup-gating-local-catalog.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/*`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamApiClient.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/*`
- `app/src/main/java/com/smartvision/svplayer/ui/series/*`
- `server/public_html/admin/index.php`
- `server/public_html/api/anomaly_service.php`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Release Android 0.1.61 versionCode 64

Type:
- android
- youtube
- release

Resume:
- Bump Android de `0.1.60 (63)` vers `0.1.61 (64)`.
- YouTube WebView: changement video via `loadVideoById` sans recharger toute la page HTML, bridge Compose actualise et qualite de lecture ciblee `medium`.
- YouTube autoplay: la liste de suggestions est consommee progressivement et maintenue autour de 20 videos pour eviter les arrets apres quelques lectures.
- YouTube UI: header mini-player corrige avec favoris visible et bouton parametres persistant; popup parametres sans arreter la lecture; nettoyage recherche, historique et favoris.
- YouTube overlay: progress bar avec temps consomme/duree totale, bouton retour fullscreen masque avec le bandeau ou affichable seul via DPAD Haut.
- Recommandations YouTube: algorithme de dossiers enrichi avec mix historique, memes chaines, meme style et decouverte.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `../TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/YoutubeDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/FavoriteDao.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

## 2026-06-30 - Release Android 0.1.60 versionCode 63

Type:
- android
- ui
- youtube
- release
- deploy

Resume:
- Bump Android de `0.1.59 (62)` vers `0.1.60 (63)`.
- YouTube mini/fullscreen: le bandeau ne s'affiche plus a l'ouverture, seulement quand la lecture demarre reellement, puis auto-hide apres 7 secondes d'inactivite.
- Ajout progress bar non focusable, bouton reload, previous base sur l'historique, bouton retour plein ecran vers le mini lecteur et reprise de position lors des transitions mini/fullscreen.
- Ecran YouTube: favoris locaux, dossier Favoris entre History et Trending, bouton coeur dans le header, suggestions recherche limitees a 3, miniatures allegees hors focus et animation "now playing" sur la video en cours.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/FavoriteDao.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

## 2026-06-30 - Release Android 0.1.59 versionCode 62

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.58 (61)` vers `0.1.59 (62)`.
- Correction definitive des boutons du bandeau YouTube: le conteneur player ne consomme plus DPAD/OK quand un bouton du bandeau est focus.
- Le focus gauche/droite reste borne dans le bandeau et OK/Enter atteint maintenant le handler du bouton focus.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.58 versionCode 61

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.57 (60)` vers `0.1.58 (61)`.
- Correction critique des controles YouTube: le bandeau parent ne consomme plus OK/Enter avant les boutons.
- Correction focus YouTube: le WebView ne reprend plus automatiquement le focus quand l'overlay SmartVision Compose controle la lecture.
- Conservation du routage DPAD gauche/droite dans les boutons et des commandes JS play/pause.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.57 versionCode 60

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.56 (59)` vers `0.1.57 (60)`.
- Correction du bandeau de controle YouTube: DPAD gauche/droite route manuellement dans les boutons, OK/Enter declenche l'action focus, Haut/Back masque le bandeau et rend le focus au player.
- YouTube: suppression du bandeau haut sur le mini-lecteur, retrait des libelles sous les icones, bandeau bas plus compact, fond bleu tres transparent, bordures/glow neon renforces.
- Live TV / Films / Series: bandeau haut sans nom de dossier/categorie, bandeau bas plus compact, boutons mieux distingues, bordures et glow neon renforces.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.56 versionCode 59

Type:
- android
- ui
- release
- deploy

Resume:
- Bump Android de `0.1.55 (58)` vers `0.1.56 (59)`.
- Refonte overlay plein ecran Live TV / Films / Series en style glassmorphism: bandeau haut avec logo, badge, titre, categorie et meta droite.
- Remplacement du bloc de controles par un bandeau bas transparent avec boutons circulaires; barre de progression conservee uniquement pour Films/Series.
- Application du meme langage visuel au player YouTube avec bandeau haut/bas, boutons ronds, glow bleu et focus Play/Pause.
- Correction de la navigation telecommande du bandeau YouTube: Bas/OK affiche les controles et focus Play/Pause; gauche/droite restent dans le bandeau; Haut/Back masque le bandeau et rend le focus au player.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`

## 2026-06-30 - Release Android 0.1.55 versionCode 58

Type:
- android
- backend
- admin
- release
- deploy

Resume:
- Bump Android de `0.1.54 (57)` vers `0.1.55 (58)`.
- Enrichissement du tracking YouTube: titre video dans `Media`, source de lancement, categorie interpretee depuis titre/description/tags/categoryId, duree et score engagement.
- Suppression de `PLAYER_READY` du tracking accepte cote app et API; conservation de `VIDEO_OPENED` et deduplication `VIDEO_COMPLETED`.
- Amelioration autoplay YouTube: si les suggestions arrivent apres la fin de lecture, la video suivante est lancee des disponibilite.
- Ajout d'un player YouTube plein ecran applicatif avec bandeau de controles SmartVision et controles iframe YouTube masques.
- Traduction incrementale YouTube: categories visibles anglais/francais, anglais par defaut.
- Correction affichage categorie admin pour eviter les codes numeriques YouTube et afficher une categorie exploitable.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/YoutubeEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeBehaviorReporter.kt`
- `app/src/main/java/com/smartvision/svplayer/data/youtube/YoutubeRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeWebPlayer.kt`
- `server/public_html/api/behavior_service.php`

## 2026-06-30 - Release Android 0.1.54 versionCode 57

Type:
- android
- backend
- admin
- release
- deploy

Resume:
- Bump Android de `0.1.52 (55)` vers `0.1.54 (57)`.
- Correction du fallback de titre Live player/tracking via le catalogue local quand le cache Xtream memoire est absent.
- Suppression du tracking `CATEGORY_OPENED` et `PLAYBACK_STARTED` cote app et rejet serveur pour anciennes APK.
- Normalisation admin des types licence comportementaux en `free_ads`, `trial_demo`, `premium`.
- Reorganisation de l'ecran Segmentation en onglets et enrichissement de la lecture rapide.
- Correction du tableau Diagnostics admin: suppression de la reference invalide a `devices.license_type`, jointure activation/code et schema SQL `app_device_diagnostics`.
- Correction Back player Live: Back quitte le player hors pub meme si l'overlay est visible, puis le focus revient sur la chaine selectionnee/focalisee.
- Build release direct via `:app:assembleRelease`, deploiement complet backend/APK et verification publique de `0.1.54 (57)`.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Release Android 0.1.52 versionCode 55

Type:
- release
- deploy

Resume:
- Bump Android de `0.1.51 (54)` vers `0.1.52 (55)`.
- Build release direct via `:app:assembleRelease`.
- Deploiement complet backend/APK via `scripts/deploy_activation_phase1.ps1 -SkipInstall`.
- Verification publique de `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne, APK stable et assets admin `admin-overrides.css?v=8` / `admin.js?v=4`.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/build/outputs/apk/release/output-metadata.json`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`

## 2026-06-30 - Update settings, segmentation enrichie et onglets appareil

Type:
- android
- backend
- admin
- documentation

Resume:
- Ajout de `Last update` / `Derniere mise a jour` dans Settings > Updates.
- Suppression de l'ouverture automatique du popup update apres check silencieux.
- Ajout du `contentTitle` dans le tracking Android et stockage backend `content_title`.
- Inference backend region, pays, langue et centres d'interet depuis categories, medias et tags.
- Enrichissement admin Segmentation avec regions, pays, langues, interets et colonnes recentes demandees.
- Correction JS/CSS des onglets du popup detail appareil et cache-busting assets.
- Documentation de la regle langue: UI Android en anglais par defaut avec traduction francaise.

Fichiers concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/update/AppUpdateViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`

## 2026-06-30 - Release Android 0.1.51 versionCode 54

Type:
- release
- deploy

Resume:
- Bump Android de `0.1.50 (53)` vers `0.1.51 (54)`.
- Build release direct via `:app:assembleRelease`.
- Validation du garde-fou version avant et apres build.
- Deploiement backend/APK via `scripts/deploy_activation_phase1.ps1 -SkipInstall`.
- Verification publique de `api/app_update.php`, `downloads/smartvision-tv.version.json`, APK versionne et APK stable.

Fichiers concernes:
- `app/build.gradle.kts`
- `app/build/outputs/apk/release/output-metadata.json`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`

## 2026-06-30 - Garde-fou version release et regle deploy backend

Type:
- tooling
- documentation

Resume:
- Ajout de `scripts/guard_release_version.ps1` pour comparer `versionCode` local, metadata APK, manifests production et appareils ADB connectes.
- Documentation de la cause du conflit `0.1.50 (53)`: numero deja publie et regenere localement sans increment.
- Ajout de la regle Knowledge System: apres chaque nouveau build release livrable, deployer le backend.

Fichiers concernes:
- `scripts/guard_release_version.ps1`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/ROOT.md`
- `TROUBLESHOOTING.md`

## 2026-06-30 - Implementation tracking comportemental et segmentation admin

Type:
- code
- backend
- android
- admin

Resume:
- Extension de `app_behavior_events` pour contenus Live TV, Films, Series, Episodes et YouTube.
- Ajout des agrégats `user_behavior_daily` et des segments `user_segments`.
- Ajout du reporter Android generique `BehaviorReporter` et instrumentation non bloquante des ecrans Live, Films, Series, details et player.
- Ajout du menu admin `Segmentation`.
- Refonte du popup detail appareil en onglets avec Tracking et Analyse comportementale.

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/behavior/BehaviorReporter.kt`
- `server/public_html/api/behavior_service.php`
- `server/public_html/admin/index.php`
- `server/public_html/assets/admin.js`
- `server/public_html/assets/admin-overrides.css`
- `server/public_html/sql/init_activation_tables.sql`

## 2026-06-30 - Verification code et amelioration continue du Knowledge System

Type:
- documentation
- verification

Resume:
- Reverification du Knowledge System contre le code Android/PHP/script reel.
- Ajout d'un protocole d'auto-amelioration continue.
- Clarification des routes `continue_watching` et `trending`.
- Clarification des routes Android `api/app/*` extensionless, des rewrites `.htaccess` et de l'exception `device-diagnostics.php`.
- Clarification de la spec segmentation comme cible future, distincte du tracking YouTube deja implemente.
- Aucun changement code applicatif.

Fichiers MD mis a jour:
- `docs/ai-knowledge/CONTINUOUS_IMPROVEMENT.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/PROJECT_OVERVIEW.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/KNOWLEDGE_SYSTEM_TEST_REPORT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- aucun

## 2026-06-30 - Spec V1 tracking comportemental et segmentation publicitaire

Type:
- documentation
- specification

Resume:
- Ajout d'une spec V1 detaillee pour tracking comportemental, segmentation utilisateur et ciblage publicitaire.
- Reference de la spec dans le domaine monetisation/tracking et dans le root ai-knowledge.
- Aucun changement code.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/user-behavior-segmentation-ads-v1.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- aucun

## 2026-06-29 - Creation du systeme ai-knowledge

Type:
- documentation
- decision

Resume:
- Creation du routeur `ROOT.md`.
- Creation de fichiers specialises par domaine fonctionnel, UI/UX et technique.
- Conservation des MD legacy comme sources historiques.
- Ajout de decisions structurantes.
- Ajout du rapport de migration et du rapport de test par simulations.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/PROJECT_OVERVIEW.md`
- `docs/ai-knowledge/AGENT_WORKFLOW.md`
- `docs/ai-knowledge/LEGACY_SOURCES.md`
- `docs/ai-knowledge/MIGRATION_REPORT.md`
- `docs/ai-knowledge/KNOWLEDGE_SYSTEM_TEST_REPORT.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-06-29-ai-knowledge-router.md`
- `docs/ai-knowledge/decisions/2026-06-29-native-tv-player.md`
- `docs/ai-knowledge/decisions/2026-06-29-documentation-update-policy.md`

Fichiers code concernes:
- aucun
# 2026-06-30 - Release prod 0.1.62 / versionCode 65

Type:
- release
- deploy
- documentation

Resume:
- Incrementation Android de `0.1.61` / `64` vers `0.1.62` / `65`.
- Build `:app:assembleRelease` reussi et publication prod app + serveur.
- Alignement du test admin du script de deploy sur le menu `Diagnostics` apres centralisation de Journal dans Diagnostics.
- Verification publique du manifeste, du version gate `app_update.php`, du hash APK versionne, de `app_config.php` et de la route `api/app/behavior-events`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Fichiers code concernes:
- `app/build.gradle.kts`
- `scripts/deploy_activation_phase1.ps1`
