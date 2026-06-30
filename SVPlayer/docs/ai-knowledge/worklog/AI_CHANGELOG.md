# AI Changelog

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
