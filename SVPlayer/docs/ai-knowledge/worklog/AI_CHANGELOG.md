# AI Changelog

## 2026-07-05 - Release prod 0.1.94 (98) stabilisation Recorder

Type:
- android
- release
- deploy
- recorder
- documentation

Resume:
- Incrementation Android en `0.1.94` / `versionCode 98`.
- Build release signe incluant la stabilisation Recorder: quitter la chaine/player ne doit pas stopper l'enregistrement, reconnexion progressive prolongee, activite `Recorder` dans Network Activity sans URL Xtream.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall`; premier passage interrompu par reset cPanel sur upload image, deuxieme passage OK.
- Correction post-deploy de `app_feature_access`: `recorder`, `media_center`, `media_file_management` et `media_phone_transfer` restent `premium=true`, `trial=true`, `free_ads=false`.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.94 (98)` > prod `0.1.93 (97)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 12m20.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.94 (98)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.94` / `98`; APK versionne `smartvision-tv-v98-f861e960.apk` et APK stable ont taille `40314086` et SHA256 `f861e960078050444cf323de1719b0ce1594c92e3f9101b391e6de2d2568b936`.
- `api/app_config.php`: flags Recorder/Media/gestion fichiers/transfert `premium=true`, `trial=true`, `free_ads=false`.
- `api/app/ads-config`: OK, provider `HILLTOPADS_VAST`; `api/app/ads-vast.php`: HTTP 200.

## 2026-07-05 - Lot 14 stabilisation Recorder + Media

Type:
- android
- recorder
- media-center
- ui-tv
- documentation

Resume:
- Correction Recorder: quitter la chaine/player ne doit plus arreter l'enregistrement Live; le service utilise `START_REDELIVER_INTENT`.
- `RecordingEngine` reconnecte les flux progressifs si la socket se ferme avant la duree demandee, sauf Stop explicite, echecs initiaux sans donnees ou grace de reconnexion expiree.
- Ajout d'une activite `Recorder` dans Settings > Network Activity: titre sanitise, section Live TV, progression, octets, debit et statut final sans URL Xtream.
- Media Center: correction focus DPAD droite liste -> apercu vers le premier bouton actif, message visible pendant preparation import/export telephone, fermeture de session QR apres upload reussi.
- Ajout i18n EN/FR pour l'etat transfert et les libelles de type/source Media.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 3m15 apres ajout Network Activity Recorder, warnings deprecation Android/Room kapt uniquement.

## 2026-07-05 - Release prod 0.1.93 (97) pour Lots 12 et 13 Media Transfer

Type:
- android
- release
- deploy
- media-center
- documentation

Resume:
- Incrementation Android en `0.1.93` / `versionCode 97`.
- Build release signe incluant les lots 12 et 13: import telephone vers TV et export TV vers telephone par QR local.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall`.
- Correction de la configuration prod `app_feature_access`: `recorder`, `media_center`, `media_file_management` et `media_phone_transfer` restent Premium/Trial et sont bloques en Free Ads.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.93 (97)` > prod `0.1.92 (96)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 14m15.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.93 (97)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.93` / `97`; APK versionne `smartvision-tv-v97-68adc147.apk` et APK stable ont taille `40314069` et SHA256 `68adc147ebd988359f7402e6b77dc9f4fa7917c92d4f897955ee858bf21c3a9b`.
- `api/app_config.php`: flags Recorder/Media/gestion fichiers/transfert `premium=true`, `trial=true`, `free_ads=false`.

## 2026-07-05 - Lots 12 et 13 transfert telephone/TV Media Center

Type:
- android
- media-center
- ui-tv
- documentation

Resume:
- Ajout de `MediaTransferServer`, serveur HTTP local temporaire pour sessions QR Media.
- Import telephone vers TV: QR upload, page mobile, envoi `PUT`, ecriture streaming dans `SmartVisionMedia/Transfers` avec fichier `.part`, puis indexation Media.
- Export TV vers telephone: QR download du fichier Media selectionne, sans exposer de listing global ni chemin arbitraire.
- Branchement des boutons `Importer tel.` et `Exporter tel.` dans `MediaScreen`, controles par `PremiumFeature.MEDIA_PHONE_TRANSFER`.
- Ajout des strings EN/FR et documentation Recorder/Media, catalog/playback, focus/navigation.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 2m31 apres correction suspend upload/export, warning Room kapt uniquement.

## 2026-07-05 - Lots 9, 10 et 11 Recorder Live MVP reel

Type:
- android
- recorder
- media-center
- documentation

Resume:
- Ajout du package `recorder`: `RecorderController`, `RecordingService`, `RecordingEngine`, `RecordingRepository` et modeles Recorder.
- Ajout du foreground service `dataSync` avec notification Recorder et action `Stop`.
- Persistance des jobs dans `recording_jobs` avec statuts `queued`, `running`, `completed`, `failed`, `cancelled`.
- Branchement du bouton `Record` Live: durees 30/60/120 min et option EPG jusqu'a fin programme demarrent un vrai enregistrement.
- Ecriture dans `SmartVisionMedia/Recordings` via fichier temporaire `.part`, finalisation atomique puis indexation Media Center.
- Support MVP des flux progressifs et HLS simple non chiffre; echec propre pour HLS chiffre/non supporte.

Validation:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK en 5m26, warnings deprecation uniquement.

## 2026-07-05 - Release prod 0.1.92 (96) pour Lots 7 et 8 Recorder + Media

Type:
- android
- release
- deploy
- documentation

Resume:
- Incrementation Android en `0.1.92` / `versionCode 96` pour depasser la prod deja en `0.1.91 (95)`.
- Build release signe de l'etat courant incluant les lots 7 et 8 Recorder + Media et les retouches header/focus/Media demandees.
- Deploiement prod via `deploy_activation_phase1.ps1 -SkipInstall` avec validations publiques et admin OK.

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.92 (96)` > prod `0.1.91 (95)`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --max-workers=1 --console=plain`: OK en 17m32.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.92 (96)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests site/activation/admin OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.92` / `96`; APK versionne `smartvision-tv-v96-c91c7b49.apk` et APK stable `smartvision-tv.apk?v=96` repondent en HTTP 200 avec taille `40264839` et SHA256 `c91c7b49f87e9a6f41d83c40ed58c3c9ca94e1bdb0bb974c4600875df77655e8`.

## 2026-07-05 - Lots 7 et 8 Media lecture locale + Recorder UI Live

Type:
- android
- ui-tv
- media-center
- documentation

Resume:
- Ajout de la route `media_player/{mediaFileId}` et du bouton `Lire` actif pour les fichiers Media video/audio/photo.
- Reutilisation du player fullscreen pour video/audio locaux via `UserContentType.LocalMedia`, sans preroll pub ni verification Xtream.
- Ajout d'un viewer photo local plein ecran.
- Branchement du bouton `Record` Live au gate `PremiumFeature.RECORDER` avec lock/couronne et popup MVP EPG/duree sans lancement de service DVR reel.
- Amelioration du focus et du design Media: DPAD droite liste -> actions apercu, dossiers focusables, panneau apercu compact, boutons actions en grille.
- Compactage des headers Home/Catalogue/Details, libelle YouTube reduit a `YT`, et couronne locked positionnee au-dessus du label.

Validation:
- `.\gradlew.bat :app:compileDebugKotlin`: OK en 2m10 apres correction d'un import `padding` manquant.

Fichiers principaux:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TvHeader.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaRepository.kt`
- `docs/RECORDER_MEDIA_PLAN.md`

## 2026-07-05 - Lots 5 et 6 Media Center stockage local + gestion fichiers

Type:
- android
- ui-tv
- room
- documentation
- release

Resume:
- Ajout du schema Room Media Center en version 10: `media_folders`, `media_files`, `recording_jobs`.
- Ajout du stockage app-specific `SmartVisionMedia` avec dossiers `Recordings`, `Imports`, `Transfers`.
- Ajout de `MediaRepository` / `MediaStorageManager` et raccord `AppContainer.mediaRepository`.
- Remplacement du MVP visuel par un ecran Media connecte aux vrais fichiers/dossiers locaux.
- Actions branchees: Actualiser, Renommer, Deplacer, Supprimer.
- Version Android preparee pour release: `0.1.91` / `versionCode 95`.

Fichiers code/version:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaCenterDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaCenterEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaCenterModels.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/media/MediaStorageManager.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.91 (95)` > prod `0.1.90 (94)`.
- `.\gradlew.bat assembleRelease`: OK en 12m29, APK `app/build/outputs/apk/release/app-release.apk`.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.91 (95)`.
- `apksigner verify --verbose --print-certs`: OK, v1=true, v2=true, certificat `CN=SmartVision, OU=Android TV, O=SmartVision, C=FR`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK, tests site/activation/admin OK.
- Production verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.91` / `95`; APK versionne `smartvision-tv-v95-f66182d2.apk` et APK stable `smartvision-tv.apk` repondent avec taille `40232068` et SHA256 `f66182d26909fac5a5011357992fdb9963878aa6ec3fcd354d0866180469a6da`.

## 2026-07-05 - Lots 3 et 4 Media Center route/header + MVP visuel

Type:
- android
- ui-tv
- admin
- documentation

Resume:
- Ajout de la route `media` et de l'onglet `Media` dans les headers.
- Branchement du menu Media sur `PremiumFeatureGate` et le flag admin/API `media_center`.
- Affichage verrouille avec couronne et popup Premium pour les statuts sans acces.
- Alignement du header Detail avec les badges locked/warning deja presents sur Home/Catalogue.
- Ajout d'un ecran `MediaScreen` MVP visuel avec bibliotheque, zone vide, apercu et actions fichiers desactivees.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/DetailCommon.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/media/MediaScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/AppConfigRepository.kt`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK en 1m49, avertissements deprecation preexistants.

## 2026-07-05 - Lot 2 PremiumFeatureGate Recorder + Media Center

Type:
- android
- backend
- admin
- documentation

Resume:
- Ajout d'un gate central `PremiumFeatureGate` pour les futures surfaces Recorder et Media Center.
- Ajout des defaults Android/admin/API pour `recorder`, `media_center`, `media_file_management`, `media_phone_transfer`.
- Ajout des strings i18n necessaires aux prochains branchements UI Media/Recorder.
- Documentation du perimetre Lot 2 et de la decision structurante.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/domain/access/PremiumFeatureGate.kt`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/AppConfigRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/decisions/2026-07-05-premium-feature-gate-recorder-media.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK, avertissements deprecation preexistants.

## 2026-07-05 - Lot 1 Recorder + Media Center

Type:
- documentation
- analyse technique
- roadmap

Resume:
- Analyse de la demande Recorder + Media Center avant code.
- Creation du document de suivi `docs/RECORDER_MEDIA_PLAN.md`.
- Indexation du domaine Recorder/Media Center dans le routeur `docs/ai-knowledge/ROOT.md`.
- Aucun changement Android/PHP/Gradle.

Fichiers MD mis a jour:
- `docs/RECORDER_MEDIA_PLAN.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code:
- aucun

## 2026-07-05 - Retouches overlay Live TV et release 0.1.90

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Haut/Bas ne zappe plus quand un panneau Live EPG/Settings est ouvert; le panneau garde la priorite de navigation.
- Le numero haut droite n'affiche plus l'id technique temporaire avant enrichissement Room, ce qui supprime le flash a 6 chiffres.
- Le logo SmartVision haut gauche est retire de l'overlay Live.
- Le titre chaine est legerement reduit, les boutons sont plus rapproches/serres a droite, plus opaques et avec un focus moins epais.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.90 (94)` > prod `0.1.89 (93)`.
- `.\gradlew.bat assembleRelease`: OK en environ 15m25.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.90 (94)`.
- `git diff --check`: OK, avertissements CRLF uniquement.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: `app_update.php` sert `latest_version_code=94`, manifeste public `0.1.90`, APK versionne `smartvision-tv-v94-f4ad6681.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133769`, SHA256 `f4ad66813f308ec109186881b731c82939aec17eec45154da56e27b57c2aa674`.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`; avertissements META-INF non bloquants.

## 2026-07-05 - Affinage overlay Live TV et release 0.1.89

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Le bandeau Live plein ecran devient rectangulaire, plus compact et plus sobre visuellement.
- Le logo de chaine est affiche sans cadre et les infos chaine sont decalees vers la gauche.
- Le numero affiche en haut a droite reprend le numero reel/pad de la liste Live.
- L'EPG sous le nom de chaine utilise le programme local courant quand il existe.
- Haut/Bas zappe en priorite sur Live TV, meme si un panneau EPG/Settings est ouvert.
- Le panneau Settings Live recupere le focus, retire la ligne vitesse indisponible et expose plus de choix d'aspect ratio.
- A la sortie du fullscreen Live, la liste restaure le focus sur la derniere chaine ouverte, y compris apres zapping.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.89 (93)` > prod `0.1.88 (92)`.
- `.\gradlew.bat assembleRelease`: premier essai KO sur `firstLiveActionFocusRequester` manquant, corrige; second essai OK en 9m.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.89 (93)`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`.
- Production verifiee: `api/app_update.php` sert `latest_version_code=93`, APK versionne `smartvision-tv-v93-36ec0343.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133779`, SHA256 `36ec03430e065b5234252ee2142cf7b057e8429a62894fa15d90e3e737a03bcb`.

## 2026-07-05 - Header date et heure

Type:
- android
- ui-tv
- documentation

Resume:
- Le header principal affiche maintenant l'heure `HH:mm:ss` et la date `dd/MM/yyyy` sur deux lignes tout a droite.
- Le logo, les espacements d'onglets et les boutons icones du header sont legerement compactes pour conserver le rendu sur une seule ligne.
- Le bloc date/heure reste non focusable et ne change pas le routage D-pad ni les callbacks de navigation existants.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/home/TvHeader.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\gradlew.bat :app:compileReleaseKotlin`: OK en 23s apres ajustement final.

## 2026-07-05 - Overlay player Live TV et release 0.1.88

Type:
- android
- ui-tv
- playback
- release
- documentation

Resume:
- Le player plein ecran Live TV utilise maintenant un overlay dedie au direct: logo SmartVision en haut gauche, numero de chaine en haut droite et bandeau bas full-width colle au bas de l'ecran.
- Le bandeau Live affiche logo chaine, nom, ligne EPG locale si disponible, puis les boutons compacts `EPG`, `Settings`, `Record`, `Back to List`.
- Les controles VOD ont ete retires du Live TV plein ecran: plus de pause, -10s, +10s, fullscreen ni barre de progression.
- Le panneau EPG lateral affiche les programmes locaux avec horaire, titre, description et mise en evidence du programme courant.
- Le panneau Settings Live expose le mode d'affichage `Fit`, `Fill`, `Zoom`, `16:9`, `Auto`; la vitesse est affichee comme indisponible pour Live TV.
- `Record` reste un placeholder avec TODO DVR et message court, sans developper l'enregistrement.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Validation:
- `.\scripts\guard_release_version.ps1`: OK, local `0.1.88 (92)` > prod `0.1.87 (91)`.
- `.\gradlew.bat assembleRelease`: OK en 10m20s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata`: OK, metadata `0.1.88 (92)`.
- `apksigner verify --verbose --print-certs`: OK, signatures v1/v2 valides, certificat `CN=SmartVision`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall`: OK.
- Production verifiee: `api/app_update.php` sert `latest_version_code=92`, APK versionne `smartvision-tv-v92-4709cf61.apk` et APK stable `smartvision-tv.apk` repondent `200`, taille `40133757`, SHA256 `4709cf614b68ae0c489efb7323e9aca9a9f096a7a2979bfc2007b0e28c2e95c7`.

## 2026-07-05 - Scroll Activite reseau et timeout notifications

### Changements
- Settings > Activite reseau: le panneau droit devient focusable et scrollable au D-pad pour parcourir toute la liste active/recente.
- `api/notifications.php`: la jointure utilisateur n'est plus executee pour les notifications `all` / `devices`; elle reste reservee aux candidates `target_scope = users`, ce qui reduit les risques de `SocketTimeoutException`.
- Version Android preparee pour release: `0.1.87` / `versionCode 91`.

### Validation
- `php -l server/public_html/api/notifications.php`: OK.
- `./gradlew :app:compileReleaseKotlin`: OK apres correction de l'import `focusable`.

## 2026-07-05 - Activite reseau Settings et correction audio Home

Type:
- android
- ui-tv
- playback
- observability
- release
- documentation

Resume:
- Home garde maintenant le fade-in audio des mini-players jusqu'a disparition/changement du composable pour `Continue watching` LiveImmediate et Tendances; le volume n'est plus remis a `0f` juste apres la premiere frame.
- Ajout de `NetworkActivityTracker`, centralise dans `AppContainer`, avec travaux actifs/recents, progression, taille, debit, duree, source/section et erreurs.
- Instrumentation OkHttp SmartVision/Xtream via titres sanitises host/chemin uniquement; les query params, tokens, identifiants Xtream, mots de passe et URLs de lecture ne sont pas affiches.
- Settings ajoute le menu `Network Activity` / `Activite reseau`, compact et extensible, pour visualiser catalogue Live/Films/Series, M3U, EPG, Home slides/tendances, verification Xtream, update APK et requetes HTTP.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/network/NetworkActivityTracker.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin` OK.
- Version bump release `0.1.86` / `versionCode 90` car la prod servait deja `0.1.85 (89)`.
- `.\scripts\guard_release_version.ps1 -SkipAdb` OK: local `90` > prod `89`.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK.
- `.\scripts\guard_release_version.ps1 -SkipAdb -RequireBuildMetadata` OK: metadata `0.1.86 (90)`.
- `apksigner verify --verbose --print-certs` OK: signatures v1/v2 valides, certificat `CN=SmartVision`.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK.
- Production verifiee: `smartvision-tv.version.json`, `api/app_update.php`, APK versionne `smartvision-tv-v90-80b95e80.apk` et APK stable `smartvision-tv.apk` servent SHA256 `80b95e80e07d5f4786e1fb5247f60d014a66324399db4ed4c9ad10c8a51e5df0`.

## 2026-07-05 - Mini-players audio Home/Live et release 0.1.85 non publiee

Type:
- android
- ui-tv
- playback
- release-blocked
- documentation

Resume:
- Home `Continue watching` active maintenant le son du mini-player apres 1 seconde de lecture, avec montee progressive du volume player de `0f` a `1f` sur 1 seconde.
- Home `Trending movies` / `Trending series` alignent la transition visuelle de focus sur `Continue watching`: scale focus `1.0f`, animation largeur via `SmartVisionDimensions.FocusAnimationMillis`, stabilisation/transformation a `1_000 ms`.
- Les mini-players tendances et l'apercu Live TV appliquent le meme fade-in audio local, annule au changement d'URL/focus, a l'erreur ou a la disparition du composable.
- `versionCode` a ete incremente de `88` a `89` pour la prochaine release, avec `versionName` conserve a `0.1.85`.
- Release non publiee: `assembleRelease` compile le code mais echoue a `:app:packageRelease` car `local.properties` est absent et la signature release n'a pas `RELEASE_STORE_FILE`. Le vrai `local.properties`, la keystore et les secrets de signature/deploiement doivent etre repris sur l'autre PC.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `job_progress.md`

Verification:
- `.\gradlew.bat compileDebugKotlin` OK avec `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` et `ANDROID_HOME=C:\Users\bbess\AppData\Local\Android\Sdk`.
- `guard_release_version.ps1 -SkipAdb` OK avant build: local `0.1.85 (89)`, prod `0.1.85 (88)`.
- `assembleRelease` lance et compile, puis echoue au packaging: `SigningConfig "release" is missing required property "storeFile"`.
- Aucune mise en prod effectuee depuis ce PC.

## 2026-07-04 - Verification Xtream confirmee avant popup

Type:
- android
- xtream
- playback
- documentation

Resume:
- `XtreamConnectionManager.verifyQuick()` confirme maintenant les echecs Xtream sur 3 essais silencieux avant notification, popup ou blocage catalogue.
- La verification principale s'appuie sur `player_api.php` sans `action` et `user_info.status == active`; les categories ne sont plus utilisees pour decider si les identifiants sont valides.
- Un buffering lecteur signale d'abord un incident de flux puis lance une confirmation Xtream, sans transformer immediatement un flux bloque en panne globale.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamApiClient.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.smartvision.svplayer.data.xtream.*" --no-daemon --max-workers=1 --console=plain` OK.
- `assembleDebug` lance puis stoppe a la demande utilisateur avant completion.

## 2026-07-03 - Robustesse historique Series

Type:
- android
- catalogue
- playback
- documentation

Resume:
- Le lecteur episode utilise les details Xtream deja charges pour sauvegarder l'historique avec le vrai titre de serie et le poster, meme si la liste serie Xtream en memoire ne contient pas la serie.
- L'enrichissement historique repare aussi les anciennes lignes episode qui ont `parentContentId` en retrouvant titre/image depuis la serie Room, meme sans entite episode locale.
- Les titres generiques `Series` / `Serie` sont traites comme des fallbacks episode.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/XtreamRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.

## 2026-07-03 - Normalisation images catalogue et release prod 0.1.85

Type:
- android
- catalogue
- ui-tv
- release-prod
- documentation

Resume:
- Ajout d'une normalisation commune des URL d'images catalogue pour Xtream: URL absolues, `//`, chemins `/...`, chemins relatifs, espaces et slashes echappes.
- Application a l'ingestion Room et a la lecture Room pour Live TV, Movies, Series, historiques et tendances Home.
- Application aux details Xtream utilises par les backdrops/posters Home premium.
- Les logos M3U `tvg-logo` relatifs sont resolus contre l'URL de la playlist.
- Bump release `0.1.85` / `versionCode 88`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/ImageUrlNormalizer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/Mappers.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/XtreamRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/remote/XtreamUrlFactory.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/M3uPlaylistClient.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\scripts\guard_release_version.ps1` OK avant build: local `0.1.85` / `88`, prod `0.1.84` / `87`.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK en 13m53s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build: metadata `0.1.85` / `88`.
- `.\scripts\deploy_activation_phase1.ps1` OK, tests publics et admin OK.
- Prod verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.85` / `88`, APK `smartvision-tv-v88-57ca0938.apk`, SHA256 `57ca0938ddb1b75cf22ff4afddc4c894e059cf5d9e14bb0cf88bc7a9511aa2cb`.
- Hash de `https://smartvisions.net/downloads/smartvision-tv.apk` verifie identique au build local.

## 2026-07-03 - Synchro Films Info compte et corrections EPG Live TV

Type:
- android
- catalogue
- ui-tv
- focus
- documentation

Resume:
- La synchro catalogue Films garde l'appel global `get_vod_streams`, puis bascule sur un fallback par `category_id` si le provider renvoie `0` film alors que des categories VOD existent.
- Les films recuperes par categorie sont rattaches a leur dossier quand la reponse ne porte pas `category_id`, puis dedupliques par `stream_id`.
- Live TV > Historique rehydrate les lignes depuis Room via les ids regardes, ce qui restaure `epgChannelId`, programmes EPG, badge `E` et details EPG sous mini-player.
- Les dossiers Live TV avec EPG affichent leur compteur dans un cadre bleu au lieu d'un badge `E` supplementaire.
- Le panneau details EPG sous le mini-player est focusable: D-pad haut/bas scrolle les programmes, puis D-pad bas va vers `Regarder` quand le bas est atteint.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.

## 2026-07-03 - Correction historique Series et release prod 0.1.84

Type:
- android
- catalogue
- playback
- release-prod
- documentation

Resume:
- Correction de la sauvegarde `playback_progress` pour les episodes Series.
- Les sauvegardes player conservent les metadonnees d'historique existantes au lieu de les remplacer par des champs vides.
- Les entrees `episode` sont enrichies depuis Room quand possible avec titre de serie, label saison/episode, poster de serie et `parentContentId`.
- Les titres generiques `Episode <id>` / `Series` / `Serie` et les sous-titres generiques sont remplaces par les metadonnees serie/episode disponibles.
- Publication prod `0.1.84` / `versionCode 87`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\scripts\guard_release_version.ps1` OK avant build: local `0.1.84` / `87`, prod `0.1.83` / `86`.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK en 12m13s.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build.
- `.\scripts\deploy_activation_phase1.ps1` OK, tests publics et admin OK.
- Prod verifiee: `smartvision-tv.version.json` et `api/app_update.php` annoncent `0.1.84` / `87`, APK `smartvision-tv-v87-60b47236.apk`, SHA256 `60b472366e98c48e33b7c23f0ae495bdd9e7fc07a672aa18c5bffa631d76a109`.
- Hash de `https://smartvisions.net/downloads/smartvision-tv.apk` verifie identique au build local.

## 2026-07-03 - HOME Tendances premium

Type:
- android
- ui-tv
- catalogue
- documentation

Resume:
- `Trending movies` et `Trending series` utilisent maintenant `TrendingContentRow`, separe de `ContinueWatchingRow`.
- Les cards Tendances restent portrait au focus court, attendent `1,3s`, s'ancrent a gauche, passent en 16:9, affichent backdrop/fallback poster floute, puis lancent un mini-preview Media3 muet.
- Les previews demarrent a 15% de la duree connue avec tentative fallback 30% si la premiere frame n'arrive pas.
- Les details premium sont prepares seulement pour les items visibles/proches ou focussees, avec concurrence bornee a `2`.
- Ajout du cache Room `home_trending_preview_cache` metadata-only; aucune URL de lecture brute n'est stockee en base.
- Ajout de la feuille de route `ui-ux/home-trending-premium-roadmap.md` et d'une decision cache metadata-only.

Fichiers code/schema:
- `app/src/main/java/com/smartvision/svplayer/ui/home/TrendingContentRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaEntities.kt`
- `app/schemas/com.smartvision.svplayer.data.local.SVDatabase/9.json`

Verification:
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain` OK.
- `.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- TV verifiee: `versionName=0.1.83`, `versionCode=86`.

## 2026-07-03 - Correction warnings Home transitoires et retour tendances

Type:
- android
- ui-tv
- catalogue

Resume:
- `AppNavigation` separe le blocage navigation Xtream du blocage visuel Home/Header.
- Les warnings jaunes et overlays "Connexion indisponible" ne sont plus affiches pendant le simple etat `checking`; ils restent visibles apres echec Xtream confirme.
- `HomeScreen` relance les tendances en cache-first apres la premiere frame Home, sans repasser par le splash ni forcer de synchronisation catalogue.
- `HomeCollectionsScreen` relance aussi ce chargement cache-first sur la route `Trending`.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`

Verification:
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- Firestick `0.1.83` / `versionCode 86`: captures sous `diagnostics/home-badges-trending-fix-20260703-055415`, Home sans badge jaune transitoire et lignes `Trending movies` / `Trending series` visibles apres D-pad bas.

## 2026-07-03 - Suppression du tampon bleu fonce entre splash et Home

Type:
- android
- startup
- ui-tv

Resume:
- `MainActivity` conserve le snapshot d'activation locale lu pendant le splash dans `AppContainer`.
- `ActivationViewModel` demarre avec ce snapshot quand il est disponible, avec `localStateReady=true` des sa creation.
- `AppNavigation` ne passe plus par l'etat tampon `localReady=false` sur le chemin Home deja active, ce qui supprime l'ecran bleu fonce intermediaire apres le splash.

Fichiers code:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`

Verification:
- `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- Logcat Firestick: apres `startup complete: rendering AppNavigation`, premier etat observe `state localReady=true checking=false activated=true playableSource=true route=home`.

## 2026-07-03 - Splash sans synchro, Home immediat et release prod 0.1.83

Type:
- android
- performance
- ui-tv
- catalogue
- release-prod

Resume:
- `MainActivity` ne lance plus aucune synchro, aucun `LoadLocal`, aucun prechargement Home et aucun chargement catalogue pendant le splash.
- Le splash garde seulement les controles legers indispensables, lit l'activation locale, efface les demandes startup residuelles et rend directement `AppNavigation` / Home.
- Home decide la synchronisation automatique apres premier rendu et bloque la telecommande uniquement pendant une vraie `Synchronize`.
- Live TV / Movies / Series chargent d'abord `20` categories Room maximum, puis completent discretement la liste complete dans chaque ViewModel.
- Les tendances Home utilisent `10` films + `10` series aleatoires depuis Room, hors adulte, sans recalcul `trending_media`, sans validation URL et sans appels details Xtream.
- Version release incrementee a `0.1.83` / `versionCode 86`.

Fichiers code/version:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/CategoryDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1` OK.
- `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon '-Dkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.workers.max=1'` OK en 13m30.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK.
- Prod verifiee: `downloads/smartvision-tv.version.json`, `api/app_update.php`, APK versionne `smartvision-tv-v86-9fcd8f69.apk` et APK stable cache-buste publient `0.1.83` / `versionCode 86`.
- SHA256 local/versionne/stable: `9fcd8f69555e3c7e99b495d8c136134f6d3814ef758afc21e17d97dd26568751`.

## 2026-07-03 - Deplacement synchro/catalogue Splash vers Home et release locale 0.1.82

Type:
- android
- ui-tv
- catalogue
- release

Resume:
- `MainActivity` ne lance plus la synchronisation catalogue complete et ne charge plus categories/pages catalogue pendant le splash.
- Ajout de `StartupCatalogWorkRequest` dans `AppContainer` pour transmettre a Home `Synchronize`, `LoadLocal` ou aucun travail.
- Home orchestre les traitements section par section avec overlays sombres progressifs sur Live TV / Films / Series, blocage telecommande pendant traitement et deblocage en succes ou erreur.
- `SyncStatus` expose maintenant les phases `WAITING`, `RUNNING`, `IMPORTING`, `LOADING_TRENDS`, `COMPLETED`, `ERROR` et un pourcentage par section.
- Les tendances sauvegardees sont chargees explicitement par section; le recalcul `trending_media` reste limite a la synchronisation catalogue.
- Frequence de synchronisation par defaut ramenee a `24h`.
- Release locale `0.1.82` / `versionCode 85` construite et installee sur Firestick `192.168.1.33:5555`.

Fichiers code/scripts:
- `app/build.gradle.kts`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/startup/StartupCatalogWork.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCategoryCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

Verification:
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1` OK.
- `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon '-Dkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.workers.max=1'` OK.
- `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -ExecutionPolicy Bypass -File .\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\release\app-release.apk` OK.
- `adb -s 192.168.1.33:5555 shell monkey -p com.smartvision.svplayer 1` OK.

## 2026-07-03 - Corrections performance Splash/Home diagnostic

Type:
- android
- performance
- ui-tv
- diagnostics

Resume:
- Le splash ne bloque plus sur `HomeContentRepository.preloadTrending()` ni sur les details Xtream/backdrops des tendances; Home rafraichit les tendances apres le premier rendu si le cache est absent.
- `HomeViewModel` initialise `HomeUiState` avec les caches disponibles pour eviter un premier rendu Home vide quand l'historique/slides/tendances sont deja en memoire.
- Le mini-player Home attend un focus stable court avant de creer ExoPlayer, garde le poster/thumbnail jusqu'a `onRenderedFirstFrame` et ignore en poster-only les URLs Media3 non supportees pendant la session.
- Les carrousels Home gardent des slots `LazyRow` stables quand les previews sont activees et limitent le scroll automatique aux cards non entierement visibles.
- `PerformanceDiagnosticRecorder` ecrit les fichiers via un writer arriere-plan pour moins perturber les mesures de jank.
- Le splash systeme a un logo plus petit/remonte, une progress bar recentree sous le logo et un status simplifie `pourcentage + statut`.
- Build local `:app:assembleReleaseDiagnostic` OK et APK `0.1.81-diag` installe sur la Firestick `192.168.1.33:5555`.

Fichiers code/scripts:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/diagnostics/PerformanceDiagnosticRecorder.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/res/drawable/splash_background.xml`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat :app:assembleReleaseDiagnostic --no-daemon --max-workers=1 --console=plain` OK.
- `adb -s 192.168.1.33:5555 install -r app\build\outputs\apk\releaseDiagnostic\app-releaseDiagnostic.apk` OK.

## 2026-07-03 - Diagnostic performance local Splash/Home Firestick

Type:
- android
- diagnostics
- perf
- adb

Resume:
- Ajout d'un build local `releaseDiagnostic` (`0.1.81-diag`, meme `applicationId`) qui active uniquement `BuildConfig.PERF_DIAGNOSTICS_ENABLED` et `profileable` shell.
- Ajout de `PerformanceDiagnosticRecorder`, balise `PERF_DIAG`, qui ecrit CSV/JSONL locaux sous `/sdcard/Android/data/com.smartvision.svplayer/files/diagnostics/splash-home-*`.
- Instrumentation diagnostic non fonctionnelle sur `MainActivity`, `HomeViewModel`, `HomeContentRepository`, `HomeScreen`, `ContinueWatchingRow` et `ContentProgressCard`: statuts splash, donnees chargees, cache Home, focus/scroll LazyRow, mini-player Media3 et premiere frame.
- Ajout du script `scripts/capture_firestick_splash_home_perf.ps1` pour installer l'APK diagnostic, capturer logcat/gfxinfo/meminfo/screenshots, pull les fichiers app, generer `perf-diagnostics.xlsx` et creer un ZIP.
- Capture Firestick produite sous `diagnostics/firestick-splash-home-perf-20260703-001037.zip`; Perfetto indisponible sur cette Firestick via CLI ancienne, erreurs conservees dans les logs bruts.

Fichiers code/scripts:
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/smartvision/svplayer/data/diagnostics/PerformanceDiagnosticRecorder.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/*`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `scripts/capture_firestick_splash_home_perf.ps1`

Fichiers MD mis a jour:
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`
- `TROUBLESHOOTING.md`

## 2026-07-02 - Splash hybride et diagnostic startup

Type:
- android
- startup
- ui-tv
- release-prod

Resume:
- `Theme.SVPlayer.Splash` affiche le fond + logo reduit dans la preview systeme pendant tout le startup.
- `MainActivity` ne redessine plus fond/logo en Compose; il affiche uniquement progress bar, statut et diagnostics au-dessus de la preview systeme.
- Le fond de fenetre splash est remplace par un fond opaque neutre juste avant `AppNavigation`, pour eviter que fond/logo splash restent visibles derriere Home.
- Les diagnostics startup affichent pourcentage, etape courante/total, elements traites/restants, temps ecoule, ETA estimee et details Live/Films/Series pendant une synchro catalogue.
- Version locale incrementee a `0.1.81` / `versionCode 84` car `0.1.80` / `83` est deja publie.
- `AppNavigation` attend `ActivationViewModel.localStateReady` avant de rendre `ActivationScreen`, pour eviter le flash activation apres splash sur appareil deja actif.
- Les tendances Home sont preparees pendant le startup dans `HomeContentRepository`: config tendances, details `backdropUrl`, mapping final `ContinueItem`, puis cache consomme par `HomeViewModel`.
- Le mini-player Continue watching rend le `PlayerView` visible et lance `play()` immediatement au focus, sans attendre la duree ou la premiere frame derriere un alpha `0`.
- Les carrousels Continue watching / Trending ancrent horizontalement la card focussee via `LazyListState.animateScrollToItem`, avec borne de fin de liste.
- Les lignes tendances Home ne restaurent plus un ancien scroll sauvegarde: chaque jeu de donnees cree un `LazyListState` neuf et se remet a l'index `0`, ce qui evite une ouverture directement sur un item au milieu de liste.
- Le clic sur HOME dans le header quand l'utilisateur est deja sur Home fait un refresh local Home et remet les lignes a gauche au lieu de renaviguer vers la meme route.
- Les sections Home vides ne sont plus affichees et les routes D-pad ignorent les lignes absentes.
- Release prod publiee en `0.1.81` / `versionCode 84` avec APK `smartvision-tv-v84-1577e450.apk`, SHA256 `1577e4508feb3ae94d5ba672f67ff851d0a1779b6b94a34dd257044b4a65afb0`; `downloads/smartvision-tv.version.json`, `api/app_update.php`, APK versionne et APK stable verifies.

Fichiers code/version:
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCategoryCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeHeroBanner.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeCollectionsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContentProgressCard.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/i18n/SmartVisionStrings.kt`
- `app/src/main/res/drawable/splash_background.xml`

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Verification:
- `.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK.
- `.\scripts\guard_release_version.ps1` OK avant build.
- `.\gradlew.bat --% :app:assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process` OK en 8m27.
- `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` OK apres build.
- `apksigner verify --verbose --print-certs app-release.apk` OK.
- `.\scripts\deploy_activation_phase1.ps1 -SkipInstall` OK; verification publique du manifeste, `app_update.php`, telechargement APK versionne/stable et hash OK.

## 2026-07-02 - Stabilisation verticale Home

Type:
- android
- ui-tv
- release
- deploy

Resume:
- Home ne fait plus de `bringIntoView()` repete apres `requestFocus()` pour les transitions Continue watching / Trending.
- Le routage Up/Down annule le job precedent, remet la ligne cible au premier item, lance un seul scroll vertical `ScrollState.animateScrollTo()`, puis demande le focus apres la fin du scroll.
- Les lignes Continue watching / Trending gardent une hauteur fixe pour isoler l'animation de largeur de la card focussee des calculs de scroll vertical.
- Les logs `SVHomeFocus` tracent `scroll start`, `scroll end` et `focus requested`.
- Release prod publiee en `0.1.78` / `versionCode 81` avec APK `smartvision-tv-v81-bcf5a8d7.apk`, hash SHA256 `bcf5a8d7e1201e28fdd78f44cfe26c349f61f1896aa3e92d52b8731a56cedd5c`.

Fichiers code/version:
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/ContinueWatchingRow.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/theme/Dimensions.kt`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash systeme MainActivity sans handoff

Type:
- android
- startup
- release-prep

Resume:
- `MainActivity` devient l'unique activite launcher TV/Android avec `Theme.SVPlayer.Splash` pour afficher immediatement le fond splash systeme.
- La logique startup/preload anciennement portee par `SplashActivity.runStartupChecks()` est deplacee dans un etat Compose de `MainActivity`.
- `SplashActivity` et `StartupHandoffScreen` sont supprimes; apres le statut `Demarrage en cours...`, `MainActivity` rend directement `AppNavigation`.
- `MainActivity` ne pose plus `window.setBackgroundDrawableResource(R.drawable.splash_background)` afin d'eviter le retour du fond splash derriere Home.
- Version locale incrementee a `0.1.78` / `versionCode 81` car `0.1.77` / `80` etait deja publie.

Fichiers code/version:
- `app/build.gradle.kts`

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Mini-player Home et config tendances admin

Type:
- android
- backend
- admin
- release-prep

Resume:
- Continue watching n'affiche plus de poster dans le mini-player: le focus lance la video sur fond noir avec fondu au demarrage.
- Tendances films/series affichent uniquement le poster paysage pendant 4 secondes, puis fondent vers la video; la lecture mini-player est bornee a 40 secondes avant retour au poster.
- Navigation Home: `LEFT` est bloque sur la premiere card des carrousels et le scroll horizontal automatique a chaque focus est retire pour reduire la latence/vibration.
- Selection tendances: filtre note retire par defaut, filtre adulte conserve, filtre poster paysage conserve au chargement Home.
- `Admin > Fonctionnalites` ajoute les parametres Tendances Home, exposes par `api/app_config.php` et consommes par Android.
- Handoff splash affiche aussi logo, barre pleine et statut `Demarrage en cours...` pour eviter le fond seul avant Home.
- Release prod publiee en `0.1.77` / `versionCode 80` avec APK `smartvision-tv-v80-8c4d880b.apk`, hash SHA256 `8c4d880b1b686aee1c101b668f0ff018181ee2c2fd1600a8ee14c42cc44bd1f0`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Mini-player Home poster paysage

Type:
- android
- ui-tv
- release-prep

Resume:
- Home filtre les tendances films/series au chargement de section pour ne garder que les medias avec une image paysage `backdropUrl` issue des details Xtream.
- Les cards Home portent maintenant une image `previewImageUrl` dediee au mini-player, distincte du poster portrait.
- Le mini-player garde le poster paysage affiche pendant la preparation Media3, attend 4 secondes de focus pour films/series, puis affiche la video en crossfade plus lent afin d'eviter l'impression de coupure/ecran noir.
- Le routage focus Home demande le focus avant le recentrage vertical pour reduire le decalage entre mouvement d'ecran et focus visible.
- Release prod publiee en `0.1.76` / `versionCode 79` avec APK `smartvision-tv-v79-54bfa614.apk`, hash SHA256 `54bfa6145236657f92be05e012fa76ca4416ccddb34b764a8ff71188418e937d`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Handoff splash et focus Home durable

Type:
- android
- ui-tv
- release-prep

Resume:
- `MainActivity` applique le fond `splash_background` avant `setContent` et `AppNavigation` affiche `smartvision_splash_bg` pendant la rehydratation locale pour eviter l'ecran noir entre splash et Home.
- Home renforce le routage D-pad vertical: chaque descente vers une ligne cible remet la `LazyRow` a l'index `0`, attend les frames Compose, scrolle la ligne visible, puis demande le focus sur le premier item.
- Les carrousels Continue watching / Trending movies / Trending series recoivent un padding horizontal interne pour que la premiere card, le focus et le mini-player ne soient pas tronques.
- Ajout de logs diagnostics `SVStartup` et `SVHomeFocus` pour verifier le handoff splash et les problemes de focus sur Firestick.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash immediat, focus Home et preview Continue watching

Type:
- android
- ui-tv
- catalogue
- release-prep

Resume:
- `SplashActivity` attend la premiere frame Compose avant les checks startup et `SVPlayerApplication` differe l'initialisation diagnostic hors thread UI pour afficher logo/progress bar sans retard perceptible.
- Le splash prechauffe apres synchro ou sans synchro les categories et premieres pages locales bornees Live/Films/Series, reutilisees par les ViewModels catalogue pour limiter les loaders d'ouverture.
- Home corrige le D-pad bas vers Continue watching / Trending movies / Trending series avec bring-into-view avant `requestFocus`.
- Continue watching utilise le mini-player Media3 muet au focus: Live immediat, Films/Episodes depuis la position de reprise, boucle de 20 secondes et overlay i18n `Resume playback` / `Reprendre la lecture`.
- Release prod publiee en `0.1.74` / `versionCode 77` avec APK `smartvision-tv-v77-055a7642.apk`, hash SHA256 `055a764235f6036795617e82fdd47ae71a68c3c448cc861beb4a44a5033205ab`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-02 - Splash image et tendances Home separees

Type:
- android
- ui-tv
- catalogue
- release-prep

Resume:
- Retour du splash sur le fond image `smartvision_splash_bg`, sans video Compose et sans second splash applicatif.
- Home separe les tendances en `Trending movies` et `Trending series`, limite l'historique a `10` items et precharge au splash uniquement les jeux Home bornes `10/10/10`.
- Ajout de `trending_media` en Room pour stocker maximum `50` films et `50` series valides pendant la synchro Xtream, avec selection note `10/10` puis `9/10`, exclusion adulte et controle court d'URL de lecture.
- Les cards tendances passent en 16:9 au focus, gardent le poster immediat puis lancent un mini-player Media3 muet par segments avec fondu.
- D-pad bas sur Home cible le premier item de la ligne suivante: categories -> premiere ligne disponible, Continue watching -> Trending movies, Trending movies -> Trending series.
- Release prod publiee en `0.1.73` / `versionCode 76` avec APK `smartvision-tv-v76-6a70e99f.apk`, hash SHA256 `6a70e99fda1ee40aaf14d45791cd8fde93ef9b8c6833e121e99e1c2a4df686e9`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

## 2026-07-01 - Pagination Room des catalogues

Type:
- android
- performance
- documentation

Resume:
- Abandon de l'idee de mini catalogue backend pour cette optimisation: Room reste le stockage persistant complet sur l'appareil.
- Ajout de requetes catalogue paginees locales `LIMIT/OFFSET` et de methodes `CatalogRepository` pour Live TV, Movies et Series.
- Live TV / Movies / Series ne chargent plus les snapshots complets pour ouvrir les ecrans; ils affichent les categories puis chargent les contenus au scroll.
- Home ne seme plus ses tendances depuis les snapshots complets Movies/Series; il reste sur des requetes limitees et l'historique recent.
- Ajout d'une decision durable: `docs/ai-knowledge/decisions/2026-07-01-room-paged-catalog-ui.md`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/decisions/2026-07-01-room-paged-catalog-ui.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

## 2026-07-01 - Splash leger et filtre EPG Live TV

Type:
- android
- ui
- performance
- documentation

Resume:
- `SplashActivity` ne precharge plus Home ni EPG; le demarrage lit seulement les categories/counts apres les verifications et l'eventuelle synchro catalogue.
- Le splash garde la video, mais ajoute `@drawable/splash_background` en preview systeme et un poster `smartvision_splash_bg` jusqu'a la premiere frame Media3 pour reduire l'ecran noir avant la video.
- Live TV remplace la recherche du header Categories par un bouton `EPG`; actif, il filtre les dossiers pour n'afficher que ceux qui ont au moins une chaine avec EPG local disponible.
- Le cache EPG passe par un fichier local borne et un parsing streaming, et la synchro EPG manuelle ne rend plus la synchro catalogue rouge si elle echoue seule.
- Les demandes de focus du profil/popup sont differees et protegees; le test Firestick 10 minutes a revele des crashes `FocusRequester is not initialized` sur DPAD droite, corriges par suppression des liens `focusProperties` vers items lazy et filet de securite dans `MainActivity`.
- Release prod publiee en `0.1.71` / `versionCode 74` avec APK `smartvision-tv-v74-d76a5da8.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/EpgRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/catalog/MediaCatalogComponents.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/youtube/YoutubeScreen.kt`
- `app/src/main/res/values/styles.xml`

## 2026-07-01 - Diagnostic memoire synchro Xtream Firestick

Type:
- android
- tooling
- documentation

Resume:
- Ajout de logs `SVSyncMemory` dans la synchronisation Xtream pour mesurer les pics memoire autour des appels reseau, de l'ecriture Room et de l'invalidation du cache local.
- Ajout du script `scripts/capture_firestick_xtream_sync.ps1` pour capturer `logcat` et `dumpsys meminfo` sur la Firestick `192.168.1.33:5555`.
- Documentation du chemin ADB Windows de reference: `C:\Users\ONEDEV\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Premiere capture Firestick: un tres gros catalogue a atteint `usedMb=118` juste avant l'ecriture Room puis a fini en ANR; au redemarrage, la reconstruction locale a lance deux OOM de 62 Mo avant qu'une synchro plus reduite reussisse.
- Optimisation candidate: synchro Xtream traitee par sections et insertions Room en batchs dans une transaction unique, counts/categories via SQL, tendances Home limitees, splash sans prechargement complet Movies/Series.
- Capture Firestick apres optimisation: `diagnostics/firestick-sync-20260701-123758`, deux synchros reussies sans OOM/ANR sur `21769 Live / 104005 Movies / 24325 Series`; pic `SVSyncMemory` observe a environ `59 Mo`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code/outillage concernes:
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `scripts/capture_firestick_xtream_sync.ps1`

## 2026-07-01 - M3U source-aware, EPG badge et UI player slim

Type:
- android
- ui
- documentation

Resume:
- Le splash et le popup manuel de synchronisation tiennent compte de la source active: M3U affiche le lien M3U et ne presente plus Films/Series comme sections chargees.
- Live TV accepte un lien M3U comme source jouable et Movies/Series affichent un etat vide explicite quand M3U est actif.
- Ajout d'un badge bleu `E` sur les lignes Live TV qui ont des programmes EPG locaux.
- Info compte est compacte: badge usage dans Licence, expiration Xtream dans Info compte, identifiants Xtream sur une ligne, boutons d'actions en icones et bascules plus petites.
- L'overlay player Live/Films/Series devient plus slim sans toucher YouTube.
- L'initialisation lourde de l'application est differee pour reduire l'ecran noir avant le splash, et les etats transitoires n'affichent plus l'ancien visuel splash.
- Release prod publiee en `0.1.70` / `versionCode 73` avec APK `smartvision-tv-v73-5ab10617.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SVPlayerApplication.kt`
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/build.gradle.kts`

## 2026-07-01 - Splash video Compose Media3

Type:
- android
- ui
- documentation

Resume:
- `SplashActivity` remplace l'ancien fond image par `splash_wave_animation.mp4` lu avec Media3 `ExoPlayer` dans un ecran Compose plein ecran.
- La video est muette, bouclee, sans controles et non focusable; le fond noir sert de securite.
- Le logo, la progress bar, les statuts startup et les prechargements Home / Live TV / Films / Series restent dans le meme splash unique.
- Le theme `Theme.SVPlayer.Splash` utilise maintenant un `windowBackground` noir pour eviter l'ancien visuel avant l'ecran video.
- Release prod publiee en `0.1.69` / `versionCode 72` avec APK `smartvision-tv-v72-a30ec7bf.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/res/values/styles.xml`
- `app/build.gradle.kts`

## 2026-07-01 - Source M3U active et EPG Live TV

Type:
- android
- ui
- backend
- documentation

Resume:
- Info compte compacte les cartes source et ajoute le lien M3U avec bascules exclusives Xtream/M3U ON vert / OFF rouge.
- Le catalogue supporte une source active unique: Xtream conserve le comportement existant, M3U alimente Live TV via URL directe et vide Movies/Series.
- Ajout du parser M3U, du cache EPG XMLTV et de l'affichage EPG scrollable dans l'apercu Live TV.
- Le Splash affiche les statuts de synchronisation/chargement M3U et EPG; le backend considere `m3u_url` comme playlist configuree sans marquer Xtream configure.
- Release prod publiee en `0.1.68` / `versionCode 71` avec APK `smartvision-tv-v71-44e92a70.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/decisions/2026-07-01-exclusive-playlist-source.md`
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/*`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/*`
- `server/public_html/api/device_status.php`
- `server/public_html/api/save_playlist_config.php`

## 2026-07-01 - Correctifs Home YouTube, Playlist et Info compte pilote

Type:
- android
- ui
- backend
- siteweb
- documentation

Resume:
- Correction du flash Home ou YouTube apparaissait verrouille avec couronne pendant le chargement initial de la config distante.
- Les pushs playlist depuis `/playlist/` ou le QR Xtream creent maintenant une notification d'information ciblee sur la TV, non cliquable dans l'app.
- `/playlist/` passe en interface mobile-first a onglets `Code Xtream`, `Lien M3U`, `Lien EPG`; le header web est stabilise et le payload chiffre accepte aussi `m3u_url`.
- Info compte reutilise le header principal, reduit le menu gauche, agrandit/compacte le panneau droit, retire les cadres des icones de lignes/actions EPG et donne le focus initial a `Licence SmartVision`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/notifications/NotificationsScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/ActivationApiService.kt`
- `server/public_html/api/helpers.php`
- `server/public_html/api/save_playlist_config.php`
- `server/public_html/playlist/index.php`
- `server/public_html/assets/site.css`

## 2026-07-01 - Info compte, URL EPG et page Playlist

Type:
- android
- ui
- backend
- siteweb
- documentation

Resume:
- `Profil client` est renomme `Info compte`; la section Xtream est reorganisee en cartes selon le modele fourni, avec actions Modifier par QR / Modifier / Supprimer et icones blanches.
- Ajout d'une section URL EPG avec affichage, popup d'edition locale et bouton QR.
- Le payload playlist chiffre accepte `epg_url`; `device_status.php` renvoie l'URL EPG a la TV sans marquer Xtream comme configure si les identifiants sont absents.
- Ajout de la page publique `/playlist/` dans le header/footer pour envoyer par code TV des identifiants Xtream et/ou une URL EPG vers la TV.
- Release prod publiee en `0.1.67` / `versionCode 70` avec APK `smartvision-tv-v70-50bfb24e.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/core/config/XtreamAccountManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/activation/*`
- `server/public_html/api/device_status.php`
- `server/public_html/api/save_playlist_config.php`
- `server/public_html/playlist/index.php`
- `server/public_html/xtream/index.php`
- `scripts/deploy_activation_phase1.ps1`

## 2026-07-01 - Ajustement splash et suppression flash activation

Type:
- android
- ui
- documentation

Resume:
- `SplashActivity` garde le splash natif unique et augmente le ratio du logo a `0.54f` et de la progress bar a `0.36f`.
- `ActivationViewModel` expose `localStateReady` apres lecture du cache activation local.
- `AppNavigation` affiche seulement le fond plein ecran tant que l'etat local activation n'est pas pret, au lieu de rendre temporairement `ActivationScreen`.
- Release prod publiee en `0.1.66` / `versionCode 69` avec APK `smartvision-tv-v69-cb3e3030.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/build.gradle.kts`

## 2026-07-01 - Splash unique et prechargement Home/catalogues au demarrage

Type:
- android
- ui
- performance
- documentation

Resume:
- Suppression du panneau `StartupVerificationPanel` Compose pour eliminer le second splash et sa seconde progress bar.
- `SplashActivity` redevient l'unique splash visuel, au format du premier modele, avec progression par statuts startup.
- Le splash verifie activation/Xtream, decide si la derniere synchronisation est OK ou KO, synchronise si necessaire, puis precharge Home / Live TV / Films / Series.
- Home utilise les caches memoire de slides, progression recente enrichie et snapshots Movies / Series pour eviter le premier chargement visible.
- Release prod publiee en `0.1.65` / `versionCode 68` avec APK `smartvision-tv-v68-e4732ab9.apk`.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/decisions/2026-06-30-xtream-startup-gating-local-catalog.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeSlidesRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/UserContentRepository.kt`
- `app/build.gradle.kts`

## 2026-06-30 - Popup manuel de synchronisation Xtream dans Profil

Type:
- android
- ui
- catalogue
- documentation

Resume:
- Profil > Identifiants Xtream affiche maintenant la derniere synchronisation sous le bouton Synchroniser.
- Le bouton ouvre un popup dedie avec cards Live TV / Films / Series, infos compte masquees et focus initial sur Lancer la synchronisation.
- Pendant `SyncStatus.Running`, le popup bloque Back/D-pad, desactive les boutons, affiche un loader et met a jour compteurs/progress bars par section.
- Apres succes ou erreur, le focus passe sur Retour; la fermeture ouvre Appareil et catalogue et y remet le focus.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/domain/model/Models.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/profile/ProfileScreen.kt`

## 2026-06-30 - Splash unique, gating Xtream sans flicker et cache catalogue local

Type:
- android
- ui
- performance
- documentation

Resume:
- Splash natif agrandi pour garder un seul visuel de chargement pendant les statuts startup.
- `ActivationViewModel` conserve l'acces local actif pendant le refresh serveur afin d'eviter un second loader Compose apres le splash.
- `CONNECTED + checking` ne bloque plus Home/Header; les overlays Xtream restent reserves aux etats inconnus ou en erreur.
- `XtreamConnectionManager` expose une validation connectee recente pour eviter une verification startup immediate en double apres le splash.
- Ajout d'un cache memoire de snapshots catalogue locaux reutilise par Live TV, Movies et Series pour reduire les loaders lors des retours d'ecran.
- Ajout de tests unitaires pour le gating Xtream et l'invalidation du cache catalogue.

Fichiers MD mis a jour:
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/screens-home-profile-settings.md`
- `docs/ai-knowledge/worklog/AI_CHANGELOG.md`

Fichiers code concernes:
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/data/xtream/XtreamConnectionManager.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/DefaultCatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/repository/LocalCatalogSnapshotCache.kt`
- `app/src/main/java/com/smartvision/svplayer/domain/repository/CatalogRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/activation/ActivationViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/movies/MoviesViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/series/SeriesViewModel.kt`

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
