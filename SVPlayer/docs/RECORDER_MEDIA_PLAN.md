# Recorder + Media Center - Plan technique

Derniere mise a jour: 2026-07-07.

Statut: Lot 17 - Media Center garde l'agencement Live TV en 3 colonnes `0.24 / 0.42 / 0.34`, mais la colonne gauche devient hierarchique: parent expandable `Media local` avec `All files`, `Recordings`, `Imports`, `Transfers`, puis categorie principale `Media prives`. Les contenus prives passent uniquement par le backend/proxy SmartVision `api/media/private/*`; Android ne connait pas les endpoints Eporner, ne scrape rien et n'essaie pas de lecture native sans flux HLS/MP4 fourni par SmartVision.

## 1. Objectif

Ajouter progressivement un systeme Recorder + Media Center reutilisable pour SmartVision, sans le limiter au Live TV et sans casser les ecrans existants.

Objectifs produit a respecter:
- Recorder reutilisable pour Live TV maintenant, puis films, series ou autres sources compatibles plus tard.
- Enregistrement reserve aux utilisateurs Premium ou essai gratuit actif.
- Free avec pubs: boutons visibles, grises, couronne Premium, popup upgrade au clic.
- Licence expiree: fichiers conserves, acces bloque, aucune suppression automatique.
- YouTube: pas d'enregistrement; bouton grise et message source non disponible.
- Aucun enregistrement infini: fin EPG si disponible, sinon duree manuelle obligatoire avec duree maximale de securite.
- Nouvel ecran Media dans le header pour enregistrer, importer, organiser, lire et transferer des fichiers locaux.
- Panel admin coherent avec les feature flags existants.

## 2. Sources analysees

Demande:
- Mission Recorder + Media Center fournie en piece jointe.

Documentation IA:
- `docs/ai-knowledge/ROOT.md`
- `docs/ai-knowledge/features/catalog-playback.md`
- `docs/ai-knowledge/ui-ux/tv-navigation-focus.md`
- `docs/ai-knowledge/features/activation-license-trial-xtream.md`
- `docs/ai-knowledge/features/monetization-consent-tracking.md`
- `docs/ai-knowledge/technical/android-architecture-build-release.md`
- `docs/ai-knowledge/technical/backend-admin-api-deploy.md`

Code Android / backend verifie:
- `app/src/main/java/com/smartvision/svplayer/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/home/TvHeader.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/catalog/MediaCatalogComponents.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/detail/DetailCommon.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/live/LiveTvViewModel.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/player/FullScreenPlayerScreen.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/SVDatabase.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/entity/MediaEntities.kt`
- `app/src/main/java/com/smartvision/svplayer/data/local/dao/MediaDao.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/EpgRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/appconfig/AppConfigRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/monetization/MonetizationModels.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`

References Android officielles consultees:
- Foreground services types: `https://developer.android.com/develop/background-work/services/fgs/service-types`
- Foreground service timeouts: `https://developer.android.com/develop/background-work/services/fgs/timeout`
- App-specific storage: `https://developer.android.com/training/data-storage/app-specific`

## 3. Constat existant

### Navigation et header

Etat actuel:
- Les routes actives sont centralisees dans `AppNavigation.kt`.
- `AppRoute` contient `home`, `live_tv`, `movies`, `series`, `youtube`, `settings`, `profile`, `notifications`, `continue_watching`, `trending`.
- `headerTabs()` expose actuellement: Home, Live TV, Movies, Series, YouTube.
- Le header Home (`ui/home/TvHeader.kt`) et le header catalogue (`ui/catalog/MediaCatalogComponents.kt`) savent afficher un etat `locked` avec couronne Premium et alpha reduite.
- Le header detail (`ui/detail/DetailCommon.kt`) affiche les tabs mais ne reprend pas encore `locked`, `warning`, icone YouTube ou couronne. Si Media devient un onglet global, cette difference devra etre corrigee pour garder une UX coherente sur les details films/series.

Implication:
- Ajouter `Media` demande une nouvelle route `media` dans `AppNavigation.kt`, une nouvelle entree dans `headerTabs()`, et une propagation propre du verrou premium sur tous les headers actifs.

### Live TV et mini-player

Etat actuel:
- `LiveTvScreen.kt` affiche trois zones: categories, chaines, apercu.
- Le mini-player Live utilise `ExoPlayer` via `MiniPreviewPlayer`, avec fallback vers l'URL fallback si le flux principal echoue.
- Le panneau apercu affiche EPG/programme, bouton `Regarder`, bouton `Favori`.
- Le retour depuis le fullscreen restaure le focus sur la chaine ouverte via `liveReturnFocusChannelId`.
- Le mode Free avec pubs affiche deja un apercu pub + QR premium quand aucune chaine n'est selectionnee.

Implication:
- Le bouton Recorder principal doit plutot etre branche dans le fullscreen Live, mais l'etat global d'enregistrement devra aussi etre visible depuis Media et possiblement depuis Live TV.
- Les changements de focus doivent rester localises; ne pas casser `returnFocusChannelId`, `firstChannelFocusRequester`, `epgDetailsFocusRequester` et le routage droite vers apercu/EPG.

### Fullscreen player et overlay Live

Etat actuel:
- `FullScreenPlayerScreen.kt` gere Live, Movie et Episode avec Media3 ExoPlayer.
- Pour Live, `resolveLive()` construit l'URL depuis `XtreamRepository`, recupere la liste EPG locale et les chaines precedente/suivante.
- L'overlay Live plein ecran contient `EPG`, `Settings`, `Record`, `Back to List`.
- `Record` est actuellement un placeholder avec TODO et toast `Recording coming soon`.
- Haut/Bas zappe Live uniquement quand aucun panneau EPG/Settings n'est ouvert.
- Back ferme d'abord EPG/Settings, puis sort du player avec sauvegarde historique/focus.

Implication:
- Le point de branchement UI naturel du Recorder MVP est `onRecord` dans `LiveTvFullscreenOverlay`.
- Il faudra ajouter un etat UI `REC` sans perturber zapping, panneaux EPG/Settings, pub preroll et sortie player.
- Le Recorder ne doit pas dependre du cycle de vie du composable player: le travail long doit etre porte par un service ou un worker dedie.

### EPG

Etat actuel:
- `EpgRepository` garde les programmes avec `startMillis` et `stopMillis`.
- `LiveChannel.withEpg()` utilise deja ces horodatages pour trouver le programme courant.
- `FullScreenEpgProgram` ne conserve aujourd'hui que `title`, `description`, `timeRange` et `isCurrent`.

Implication:
- Pour enregistrer jusqu'a la fin du programme, il faut propager `startMillis` et `stopMillis` jusqu'au modele fullscreen ou interroger `EpgRepository` au moment de construire `RecordingRequest`.
- Ne pas se baser uniquement sur `timeRange`, qui est une chaine d'affichage.

### Licence, premium et feature flags

Etat actuel:
- `MonetizationModels.kt` centralise les statuts: `PREMIUM_ACTIVE`, `TRIAL_ACTIVE`, `FREE_WITH_ADS`, `TRIAL_EXPIRED`, `LICENSE_EXPIRED`.
- `ActivationViewModel` considere `FREE_WITH_ADS` comme un acces runtime a l'app, mais pas comme un acces aux features premium.
- `AppConfigRepository.isFeatureAllowed()` croise `FeatureAccess` avec le statut monetisation.
- Le panel admin `admin/index.php` gere `app_feature_access` avec colonnes Premium / Trial / Free Ads.
- `api/app_config.php` expose les features a Android.
- Les features existantes incluent `youtube`, `parental_control`, `replay`, `advanced_favorites`, `multi_screen`, `local_cache`.

Implication:
- Creer un `PremiumFeatureGate` doit reutiliser `resolveMonetizationStatus()` et `AppConfigRepository`, pas dupliquer la logique licence.
- Cles feature a ajouter cote admin/API/Android: au minimum `recorder`, `media_center`, `media_file_management`, `media_phone_transfer`.
- Pour Free Ads, le gate doit retourner un etat bloque mais affichable: `visibleLocked`, avec couronne et popup.

### Room et stockage local

Etat actuel:
- `SVDatabase` est en version 10.
- `MediaEntities.kt` reste reserve au catalogue Xtream/M3U, favoris, progression, tendances et historique YouTube.
- `MediaCenterEntities.kt` ajoute `media_folders`, `media_files` et `recording_jobs`.
- `MediaCenterDao.kt` isole les requetes Media Center pour ne pas surcharger `MediaDao`.
- `MediaStorageManager` cree et scanne `context.getExternalFilesDir(null)/SmartVisionMedia`, avec fallback `filesDir/SmartVisionMedia`.
- Les sous-dossiers par defaut sont `Recordings`, `Imports` et `Transfers`.
- `MediaRepository` synchronise le scan disque vers Room, marque les fichiers absents comme supprimes logiquement, et execute les operations physiques renommer/deplacer/supprimer.

Implication:
- Les futurs lots Recorder doivent ecrire dans `SmartVisionMedia/Recordings` puis indexer via `MediaRepository`.
- Les futurs lots import/transfert doivent ecrire dans `SmartVisionMedia/Imports` ou `SmartVisionMedia/Transfers`.
- La licence expiree bloque l'acces UI mais ne supprime pas les fichiers locaux.

### Services, notifications et contraintes Android

Etat actuel:
- `AndroidManifest.xml` declare `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, update/install, boot receivers, mais pas de `Service` pour recording.
- `WorkManager` existe deja pour la synchro, mais le recording est une action utilisateur longue et visible.
- Les docs Android actuelles imposent un type de foreground service pour les apps ciblant Android 14+; SmartVision cible actuellement SDK 36.
- Les foreground services `dataSync` / `mediaProcessing` ont des limites de duree systeme sur Android recent; la demande produit impose de toute facon une duree maximale.

Implication:
- `RecordingService` devra etre un foreground service avec notification visible et type/permissions manifest a confirmer au moment du lot service.
- La duree maximale MVP doit rester inferieure aux limites systeme et explicitement bornee.
- Ne pas lancer un enregistrement depuis boot/background; il doit partir d'une action utilisateur.

### Transfert telephone <-> TV

Etat actuel:
- ZXing core est deja present pour generer des QR codes.
- `INTERNET` est deja declare.
- Aucun serveur local temporaire n'existe.

Implication:
- Le transfert phone/TV doit rester un lot separe, apres Media/stockage/lecture locale.
- Il faudra ajouter une dependance serveur HTTP locale legere uniquement si necessaire, avec arret strict quand l'ecran/popup est ferme.

## 4. Plan d'architecture propose

### Package recorder

Creer un package dedie:

```text
app/src/main/java/com/smartvision/svplayer/recording/
  RecorderController.kt
  RecordingRequest.kt
  RecordingModels.kt
  RecordingService.kt
  RecordingEngine.kt
  HlsRecordingEngine.kt
  ProgressiveRecordingEngine.kt
  RecordingRepository.kt
  RecordingStorageManager.kt
  RecordingScheduler.kt
```

Regle:
- Le player Compose demande une action.
- `RecorderController` valide l'acces, la source, l'espace et la duree.
- `RecordingService` execute le travail long.
- `RecordingRepository` persiste statut/job.
- `RecordingStorageManager` gere chemins, temp, noms, espace disponible.
- `RecordingEngine` ecrit les bytes dans un fichier temp puis finalise.

MVP recommande:
- Live TV HLS/progressive uniquement.
- Pas YouTube.
- Pas de recorder VOD/series tant que le pipeline Live n'est pas stable.

### Package Media Center

Creer un package dedie:

```text
app/src/main/java/com/smartvision/svplayer/media/
  MediaScreen.kt
  MediaViewModel.kt
  MediaRepository.kt
  MediaStorageManager.kt
  MediaPlaybackModels.kt
```

Donnees:

```text
data/local/entity/MediaCenterEntities.kt
data/local/dao/MediaCenterDao.kt
```

Tables Room proposees:
- `media_files`
- `media_folders`
- `recording_jobs`

Ne pas melanger ces tables avec le catalogue Xtream/M3U.

### PremiumFeatureGate

Creer un composant central:

```text
app/src/main/java/com/smartvision/svplayer/domain/access/PremiumFeatureGate.kt
```

Responsabilites:
- prendre `MonetizationStatus`;
- lire les flags `AppRuntimeConfig.features`;
- retourner un etat reutilisable:

```text
Allowed
LockedPremiumVisible
BlockedExpired
SourceUnsupported
ConfigDisabled
```

Usage prevu:
- header Media;
- bouton Record;
- ecran Media;
- actions fichiers;
- transfert telephone;
- lecture fichiers enregistres;
- futur reuse films/series.

Implementation Lot 2:
- `PremiumFeatureGate` existe sous `domain/access`.
- Les features connues sont centralisees dans `PremiumFeature`.
- Les etats retournes distinguent acces autorise, verrou Premium visible, licence/essai expire, source non supportee et desactivation par config.
- Le statut doit etre resolu avant appel via la logique monetisation existante; le gate ne recalcule pas activation/licence.
- Le gate lit `AppRuntimeConfig.features` via `featureAccessFor()` et conserve des defaults locaux si la config distante ne contient pas encore une cle.
- Le gate ne lance aucune action UI ou navigation directement; les lots suivants doivent brancher les headers, popups et boutons en consommant `PremiumFeatureGateResult`.

### Admin/API

Ajouter les features admin/API avant ou pendant le lot `PremiumFeatureGate`:
- `recorder`
- `media_center`
- `media_file_management`
- `media_phone_transfer`

Valeurs par defaut souhaitees:
- Premium: oui
- Trial: oui
- Free Ads: non

Impact:
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`
- `AppConfigRepository.defaultFeatureAccess()`
- Documentation backend/admin.

Implementation Lot 2:
- Les quatre flags existent dans les defaults Android, API et admin.
- Valeurs par defaut: Premium oui, Trial oui, Free Ads non.
- Aucun nouveau fichier PHP n'a ete cree; le script de deploy n'a donc pas besoin d'ajout de fichier pour ce lot.

## 5. Ordre d'implementation propose

### Lot 2 - PremiumFeatureGate + flags admin

Objectif:
- centraliser l'acces Premium/Trial/Free Ads/Expired.

Fichiers probables:
- `data/monetization/MonetizationModels.kt`
- `data/appconfig/AppConfigRepository.kt`
- nouveau `domain/access/PremiumFeatureGate.kt`
- `server/public_html/admin/index.php`
- `server/public_html/api/app_config.php`
- `ui/i18n/SmartVisionStrings.kt`

Validation:
- compilation Android;
- verifier que YouTube garde son comportement actuel;
- verifier `Admin > Fonctionnalites` et `api/app_config.php` en local si backend execute.

### Lot 3 - Route Media + header locked

Objectif:
- ajouter `Media` dans le header sans ecran complexe.

Fichiers probables:
- `ui/navigation/AppNavigation.kt`
- `ui/home/TvHeader.kt`
- `ui/catalog/MediaCatalogComponents.kt`
- `ui/detail/DetailCommon.kt`
- nouveau `ui/media/MediaScreen.kt` placeholder
- `ui/i18n/SmartVisionStrings.kt`

Validation:
- build Android;
- verifier header Home/Catalogue/Details;
- en Free Ads: Media grise + couronne + popup;
- en Premium/Trial: route Media accessible.

Implementation Lot 3:
- `AppRoute.Media` et la route `media` existent dans `AppNavigation.kt`.
- Le header global inclut `Media` entre Series et YouTube.
- `media_center` pilote le menu via `PremiumFeatureGate`.
- Si Free Ads ou licence/essai expire: bouton visible, grise, couronne, popup upgrade au clic.
- Si `media_center` est desactive par config admin pour le statut courant: l'entree de menu est retiree et la route directe affiche un placeholder.
- Le header Detail affiche maintenant warning/couronne comme les headers Home/Catalogue.
- Le libelle admin/API de `media_center` est explicite: menu Media Center.

### Lot 4 - MediaScreen MVP visuel

Objectif:
- ecran 3 zones avec etat vide, focus TV, sans vraie gestion fichiers.

Fichiers probables:
- `ui/media/MediaScreen.kt`
- `ui/media/MediaViewModel.kt`
- composants UI locaux ou reutilisation `ui/catalog`.

Validation:
- navigation D-pad gauche/droite/haut/bas;
- Back revient proprement;
- aucun overlap desktop/TV.

Implementation Lot 4:
- `ui/media/MediaScreen.kt` cree un ecran TV MVP visuel.
- Zones affichees: bibliotheque, medias recents, apercu.
- Les zones Recordings, Imports, Folders, Transfers sont selectionnables au D-pad.
- L'etat vide precise qu'aucun fichier local n'est encore indexe.
- Les actions Play/Rename/Move/Delete sont visibles mais desactivees tant que le lot stockage/gestion fichiers n'existe pas.
- L'ecran locked reutilise `PremiumFeatureGateResult` et le popup Premium existant.

### Lot 5 - Room Media + stockage local

Objectif:
- creer tables, DAO, repository, structure `SmartVisionMedia`.

Fichiers probables:
- `data/local/SVDatabase.kt`
- `data/local/entity/MediaCenterEntities.kt`
- `data/local/dao/MediaCenterDao.kt`
- `media/MediaRepository.kt`
- `media/MediaStorageManager.kt`
- `core/data/AppContainer.kt`

Validation:
- migration Room compilee;
- tests simples repository si existants ou compile gate;
- verifier creation dossiers et espace disponible.

Implementation Lot 5:
- `SVDatabase` passe en version 10 avec migration additive `9 -> 10`.
- Ajout des tables `media_folders`, `media_files` et `recording_jobs`.
- Ajout de `MediaCenterDao.kt` et `MediaCenterEntities.kt`.
- Ajout de `MediaStorageManager`, `MediaRepository` et raccord `AppContainer.mediaRepository`.
- Le scan local cree `SmartVisionMedia/Recordings`, `SmartVisionMedia/Imports` et `SmartVisionMedia/Transfers`, puis indexe les fichiers existants sans demander de permission stockage large.

### Lot 6 - Gestion fichiers Media

Objectif:
- afficher vrais fichiers, dossiers, renommer, supprimer, deplacer.

Validation:
- operations sur fichiers app-specific;
- suppression logique + suppression physique controlee;
- pas de suppression automatique a cause licence expiree.

Implementation Lot 6:
- `MediaScreen` consomme `MediaViewModel` et affiche les vrais fichiers/dossiers indexes.
- Les zones `Tous les fichiers`, `Enregistrements`, `Imports`, `Dossiers` et `Transferts` filtrent les donnees Room.
- Le panneau apercu affiche type, source, taille, date, dossier et chemin relatif.
- `Actualiser` rescane le stockage local.
- `Renommer`, `Deplacer` et `Supprimer` agissent sur les fichiers app-specific et mettent Room a jour.
- `Lire` ouvre les fichiers video/audio/photo locaux depuis le Lot 7.

### Lot 7 - Lecture video/photo Media

Objectif:
- mini-player local, full screen local, retour focus.

Attention:
- le player fullscreen actuel attend des IDs Xtream. Prevoir une route separee `media_player/{mediaFileId}` ou un `PlaybackKind.Local` sans casser Live/Movie/Episode.

Implementation Lot 7:
- Route `media_player/{mediaFileId}` ajoutee dans `AppNavigation.kt`.
- `MediaRepository.getPlayback()` expose un `MediaCenterPlayback` avec URI app-specific valide.
- Les fichiers video/audio utilisent le player fullscreen existant avec `UserContentType.LocalMedia`, sans preroll pub ni verification Xtream.
- Les photos utilisent un viewer plein ecran local avec retour D-pad.
- Le bouton `Lire` est actif pour Video/Audio/Photo et reste inactif pour `Other`.
- Navigation focus Media amelioree: passage DPAD droite liste -> actions apercu, dossiers focusables, panneau apercu plus compact.

### Lot 8 - Bouton Recorder UI Live

Objectif:
- brancher le bouton Record au gate et aux popups duree/EPG, sans enregistrement reel si necessaire.

Points critiques:
- Free Ads: bouton grise + couronne + popup.
- YouTube: source non disponible.
- EPG: utiliser start/end millis, pas `timeRange`.

Implementation Lot 8:
- `AppNavigation.kt` evalue `PremiumFeature.RECORDER` et le transmet au player Live.
- Le bouton `Record` du fullscreen Live affiche un etat lock/couronne quand l'acces est bloque mais visible.
- Au clic avec acces autorise, une popup Recorder affiche le programme courant, l'option `Jusqu'a la fin du programme` si EPG start/stop disponible, et des durees manuelles 30/60/120 minutes.
- Le bouton de demarrage lance un enregistrement reel depuis le Lot 9/10/11.
- Les bornes `startMillis`/`stopMillis` sont conservees dans `FullScreenEpgProgram`.

### Lot 9 - RecordingService + controller + jobs

Objectif:
- service foreground, notification/etat, start/stop/cancel, statut Room.

Validation:
- start/stop manuel;
- duree maximale obligatoire;
- temp cleanup en erreur.

Implementation Lot 9:
- Ajout du package `recorder` avec `RecorderController`, `RecordingService`, `RecordingEngine`, `RecordingRepository` et modeles de requete/job.
- `RecordingService` est declare en foreground service `dataSync` avec notification visible et action `Stop`.
- Les jobs sont persistés dans `recording_jobs` avec statuts `queued`, `running`, `completed`, `failed`, `cancelled`.
- Le stockage utilise un fichier temporaire `.part`, puis finalise seulement si des donnees media ont ete ecrites.

### Lot 10 - Enregistrement Live TV manuel

Objectif:
- enregistrer un flux compatible pendant une duree choisie, finaliser fichier, indexer dans Media.

Implementation Lot 10:
- Le bouton `Record` Live demarre le Recorder si le gate Premium/admin autorise l'acces.
- Les durees manuelles 30/60/120 minutes lancent un job unique; un second job simultane est bloque.
- Les fichiers finalises sont places dans `SmartVisionMedia/Recordings` puis indexes via `MediaRepository`, donc visibles dans Media apres refresh/observation Room.
- Le moteur supporte les flux progressifs et HLS simple non chiffre; les HLS chiffres ou playlists exotiques echouent proprement dans le job.

### Lot 11 - Integration EPG

Objectif:
- enregistrer jusqu'a fin programme + marge configurable.

Implementation Lot 11:
- `FullScreenEpgProgram` conserve `startMillis` et `stopMillis`.
- La popup Recorder propose `Jusqu'a la fin du programme` seulement si `stopMillis` est futur.
- Le job Recorder recoit le titre et les bornes EPG pour nommer le fichier et borner la duree.
- La marge configurable n'est pas encore exposee; le MVP s'arrete a la fin programme connue.

### Lot 12 - Import telephone vers TV

Objectif:
- serveur local temporaire + QR upload.

Implementation Lot 12:
- Ajout de `media/transfer/MediaTransferServer`, serveur HTTP local temporaire lance uniquement depuis l'ecran Media.
- Le bouton `Importer tel.` demarre une session QR `/u/{token}`; la page mobile envoie un fichier par `PUT /upload/{token}/{filename}`.
- Les fichiers recus sont ecrits en streaming dans `SmartVisionMedia/Transfers` via fichier `.part`, finalises seulement si non vides, puis indexes par `MediaRepository.refreshStorage()`.
- La session utilise une URL/token aleatoire par lancement et reste limitee au reseau local tant que le dialog QR est ouvert.
- Le bouton respecte `PremiumFeature.MEDIA_PHONE_TRANSFER`: visible locked pour Premium/Trial requis, masque si desactive par config admin.

### Lot 13 - Export TV vers telephone

Objectif:
- lien temporaire + QR download.

Implementation Lot 13:
- Le bouton `Exporter tel.` dans l'apercu Media demarre une session QR `/d/{token}` pour le fichier selectionne.
- La page mobile expose un lien `/download/{token}` servant uniquement ce fichier app-specific avec `Content-Disposition` et `Cache-Control: no-store`.
- Aucun listing global ni chemin arbitraire n'est expose; le download passe par l'id Media selectionne puis `MediaRepository.getDownload()`.
- La session est arretee a la fermeture du dialog ou a la destruction du ViewModel Media.

### Lot 14 - Stabilisation finale

Objectif:
- erreurs, logs, cleanup temp, polish UI/focus, tests Firestick/Android TV, doc finale.

Implementation Lot 14:
- Le Recorder Live ne depend plus de l'ecran player apres le demarrage: `RecordingService` utilise `START_REDELIVER_INTENT` pour reduire les pertes de job si Android recrée le service.
- Les flux Live progressifs ne sont plus finalises au premier EOF avant la duree demandee: `RecordingEngine` reconnecte le flux et continue d'ecrire dans le meme fichier temporaire, sauf action Stop explicite, duree atteinte, echecs initiaux sans donnees ou grace de reconnexion expiree apres coupure.
- Network Activity affiche une activite `Recorder` sanitisee pendant l'enregistrement Live: titre chaine, section Live TV, source Recorder, progression temporelle, octets ecrits, debit et statut final, sans URL Xtream ni credentials.
- Media Center corrige le routage DPAD droite liste -> apercu: le focus vise le premier bouton reellement actif (`Lire`, `Renommer` ou `Exporter tel.`) et ne demande plus un `FocusRequester` absent quand aucun fichier n'est selectionne.
- Media Center affiche un etat `Preparation du transfert telephone...` pendant l'ouverture import/export, ferme automatiquement la session QR apres upload reussi, et remet l'etat session a null en cas d'echec de demarrage.
- Media Center remplace l'ancien bouton lateral `Importer tel.` par un panneau `Telephone -> TV`: si l'acces est autorise, le bouton `Recevoir fichier` explique le QR et le Wi-Fi; si l'acces est verrouille, le bouton reste focusable sous `Debloquer` et affiche la raison Premium/essai.
- Les libelles techniques `Source`, `Type`, `Video/Photo/Audio/File` passent par i18n EN/FR.

### Lot 16 - Refonte premium Media Center

Objectif:
- Donner a la fonctionnalite premium un rendu TV premium sans changer la logique stockage/transfert.

Implementation Lot 16:
- Colonne bibliotheque: hero `Studio media premium`, compteurs par zone, carte `Stockage intelligent` et module `Hub transfert`.
- Zone centrale: header contextualise, bouton Actualiser plus discret, bandeau de statistiques `Fichiers total` / `Recordings` / `Hub transfert`.
- Lignes fichiers: icone badgee par type, fond gradient actif, pills source/taille/type, date conservee.
- Apercu: hero media avec etat `Pret pour lecture locale`, details regroupes et actions `Lire`, `Renommer`, `Supprimer`, `Deplacer`, `Exporter tel.` conservees.
- La logique de suppression, renommage, deplacement, lecture locale, import/export telephone et gates Premium/admin n'est pas modifiee.

## 6. Risques et points de vigilance

- HLS vs progressive: ne pas supposer que tous les flux Xtream sont enregistrables proprement par copie HTTP simple.
- Identifiants Xtream: ne jamais stocker ni logguer d'URL de lecture brute dans Room ou logs persistants; les chemins fichiers locaux peuvent etre stockes, pas les credentials.
- EPG: le modele fullscreen doit conserver les timestamps ou relire `EpgRepository`.
- Android foreground service: type `dataSync` et permissions manifest ajoutes; verifier le comportement reel sur Firestick/Android TV pendant un enregistrement long.
- Stockage: rester en app-specific storage pour eviter permissions larges et suppression hors perimetre.
- Room: migration additive uniquement, pas de refonte des tables catalogue.
- Focus TV: pas de `FocusRequester` vers items lazy non composes; suivre les regles existantes.
- Admin: tout nouveau flag PHP doit etre ajoute aussi a `api/app_config.php` et au script de deploy si nouveaux fichiers PHP.
- Detail headers: aligner les badges lock/warning avec Home/Catalogue avant d'afficher Media partout.
- YouTube: bloquer explicitement le recorder, ne pas contourner les limitations.

## 7. Definitions de termine

### Lot 1
Termine:
- Existant analyse.
- Points d'integration identifies.
- Plan technique documente.
- Ordre d'implementation propose.

Non fait dans ce lot:
- aucun code Android/PHP modifie;
- aucun build lance;
- aucune migration Room creee;
- aucun service recorder implemente.

Prochaine etape recommandee:
- Lot 2 - `PremiumFeatureGate + flags admin`.

### Lot 2

Termine:
- `PremiumFeatureGate` ajoute et reutilisable par les prochains lots.
- Defaults Android ajoutes pour `recorder`, `media_center`, `media_file_management`, `media_phone_transfer`.
- Defaults admin/API ajoutes dans `app_feature_access`.
- Strings i18n ajoutees pour Media, verrou premium generique et source recorder non supportee.
- Documentation IA mise a jour.

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK, avertissements deprecation preexistants.

Non fait dans ce lot:
- aucun onglet/header Media branche;
- aucun ecran Media Center cree;
- aucun bouton Record connecte;
- aucune migration Room Media Center;
- aucun service Recorder;
- aucun flux de transfert telephone/TV.

Prochaine etape recommandee:
- Lot 3 - Route Media + header locked.

### Lot 3

Termine:
- Route `media` ajoutee.
- Onglet `Media` ajoute au header principal.
- Verrou/couronne branche via `PremiumFeatureGate` et flag admin/API `media_center`.
- Header Detail aligne avec Home/Catalogue pour les badges warning/locked.
- Controle admin/API clarifie par le libelle `Menu Media Center`.

Non fait dans ce lot:
- aucun vrai listing de fichiers locaux;
- aucune action fichier;
- aucun recorder.

### Lot 4

Termine:
- Ecran `MediaScreen` MVP visuel ajoute.
- Layout TV 3 zones ajoute: bibliotheque, liste vide, apercu.
- Focus initial et selection D-pad sur les zones Media.
- Etat locked Premium/expire affiche si la route est ouverte directement sans acces.

Validation:
- `git diff --check`: OK, avertissements CRLF uniquement.
- `php -l server/public_html/admin/index.php`: OK.
- `php -l server/public_html/api/app_config.php`: OK.
- `.\gradlew.bat :app:compileDebugKotlin`: OK en 1m49, avertissements deprecation preexistants.

Non fait dans ce lot:
- aucune migration Room Media Center;
- aucune creation de dossier `SmartVisionMedia`;
- aucune lecture locale;
- aucun import/export telephone;
- aucune suppression, renommage ou deplacement reel.

Prochaine etape recommandee:
- Lot 5 - Room Media + stockage local.

### Extension Media prives 2026-07-07

Termine:
- `Media prives` devient un dossier expandable avec sous-dossiers/categories issus du backend SmartVision.
- Les premiers sous-dossiers prives par defaut sont `Nouveautes`, `Populaires`, `Top semaine`, `Mieux notees`, `Long format`, `Amateur`, `Couples`, `POV`; ils sont gerables dans Admin > Bibliotheque privee et leur nom/query servent aux recherches backend.
- `Importer tel.` et `Exporter tel.` sont deplaces sous le dossier expandable `Media local`, sous forme de lignes compactes avec icone QR, pour ne plus polluer les categories principales.
- Le bouton refresh de la liste Media est remplace par un champ de recherche focusable TV; en prive, la recherche repart via `/api/media/private/items.php?query=...`.
- Premier OK sur une video privee charge la preview et lance le mini-player; second OK sur le meme item ouvre `private_media_player/{id}`.
- Lecture privee: ExoPlayer uniquement pour flux directs HLS/MP4 fournis par SmartVision/provider et valides par le backend; sinon WebView embed officiel. Aucun endpoint provider n'est construit dans l'APK et aucune extraction HTML n'est faite.
- `Synchroniser removed` cote admin est borne par lot et transactionnel pour eviter les HTTP 500.

Validation:
- `php -l` sur `private_media_service.php`, `items.php`, `admin/index.php`: OK.
- `.\gradlew.bat :app:compileReleaseKotlin --no-daemon --max-workers=1 --console=plain`: OK.
