# Android Architecture, Build et Release

Derniere mise a jour: 2026-07-21.

## Compatibilite Android 9 - 2026-07-21

- Les regles R8 release conservent le detecteur Android et le cache de constructeurs d'exceptions de `kotlinx.coroutines`. Cela empeche l'optimisation de choisir le chemin JVM `ClassValue` indisponible sur Android 9 et conserve le fallback `WeakMap` prevu par la bibliotheque.
- Le correctif ne change ni le minSdk, ni les versions de dependances, ni le schema Room. Sa validation terrain doit etre reprise sur un appareil Android 9 quand une cible sera disponible.

## Splash de demarrage

- `MainActivity` reste l'unique launcher; `Theme.SVPlayer.Splash` fournit la preview systeme et `StartupGate`/`StartupExperience` assurent la continuite Compose. Ne pas ajouter de `SplashActivity`.
- `splash_background.xml` et `StartupExperience` utilisent le meme fond 1920 x 1080 `startup_cinema_background` et le meme logo officiel `smartvision_logo_wide`, avec taille/position identiques pour eviter flash et double logo.
- La progression reelle est rendue par un cercle bleu/cyan anime, fin et compact, avec halo discret; le statut reste sous le cercle. Il n'existe plus de barre lineaire.
- Le contenu initial est precompose pendant le splash. Pour Who's Watching, le signal `initial surface ready` exige un layout non nul et l'animation des cartes ne demarre qu'au handoff visible; la duree du splash n'est pas raccourcie.

## Persistance profils v15

- Room v14 ajoute `profileId` aux recherches, historique et selection YouTube; la migration 13->14 rattache les lignes existantes au profil actif.
- Room v15 ajoute `addedAt` aux films et series pour le classement deterministe des tendances et le fallback nouveautes.
- Les credentials custom sont chiffres AES/GCM avec une cle Android Keystore et ne sont plus persistes dans le JSON des profils.
- Le PIN parental est derive avec PBKDF2-HMAC-SHA256, sel aleatoire et comparaison constante; l'ancien PIN DataStore est migre vers un marqueur `configured`.
- `videoRatio`, `bufferMode` et `retryEnabled` sont scopes par profil avec fallback sur les anciennes valeurs globales.

## Persistance Kids v16

- Room v16 ajoute les tables partagees `kids_category_decisions` et `kids_item_decisions` ainsi que les index source/type/categorie/decision/version.
- La migration `15 -> 16` ne modifie pas les tables catalogue existantes. Le schema exporte est `app/schemas/com.smartvision.svplayer.data.local.SVDatabase/16.json`.

## Snapshot parental v17

- Room v17 ajoute `parental_filter_snapshots` et `parental_hidden_items` pour conserver les resultats filtres par profil entre les ouvertures d'ecran et les redemarrages de l'application.
- La migration `16 -> 17` ne modifie pas les catalogues. Un snapshot est invalide uniquement si son empreinte de mots-cles ou le `sync_state.lastSync` du profil change. Le schema exporte est `app/schemas/com.smartvision.svplayer.data.local.SVDatabase/17.json`.

## 1. Objectif

Documenter l'architecture Android active, les points d'entree techniques, le build release, la signature et les controles avant publication.

## 2. Fonctionnement actuel

L'application Android est dans `app/`. La navigation active est Compose dans `ui/navigation/AppNavigation.kt`. Les dependances sont creees dans `core/data/AppContainer.kt`. Le projet demande JDK 21.

Observabilite reseau:
- `AppContainer` cree un singleton `NetworkActivityTracker`.
- `NetworkActivityInterceptor` instrumente les clients OkHttp Xtream, SmartVision et TMDB avec des titres sanitises host/chemin uniquement.
- Les travaux metier catalogue, M3U, EPG, Home slides/tendances, update APK et verification Xtream publient aussi des etats applicatifs dans le tracker.
- `SettingsScreen` affiche ces etats dans `Network Activity` / `Activite reseau`: actifs, recents, progression, taille, debit, duree, source/section et erreurs.
- Ne jamais exposer les query params, tokens, identifiants Xtream, mots de passe ou URLs de lecture dans ce tracker.

Gradle local constate le 2026-07-13:
- `versionCode = 151`
- `versionName = "0.1.120"`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 23`
- release standard sans minify/shrink
- `releaseOptimized` avec minify/shrink
- `TMDB_READ_ACCESS_TOKEN` est lu depuis `local.properties` vers `BuildConfig`; garder le vrai token hors Git. `TMDB_API_READ_ACCESS_TOKEN` reste accepte comme alias local.

Demarrage:
- `MainActivity` est l'activite launcher TV/Android et utilise `Theme.SVPlayer.Splash` pour la preview systeme immediate.
- Le theme systeme `Theme.SVPlayer.Splash` et `StartupExperience` reutilisent `startup_neon_background.webp` en plein ecran et `smartvision_logo_wide.png` aux memes dimensions/offsets: la preview Android et la premiere frame Compose gardent donc la meme composition sans second ecran ni `SplashActivity`.
- Le startup applicatif suit les phases visuelles `LogoOnly -> Loading -> TransitionOut`. `LogoOnly` dure au moins `450 ms`; la zone de chargement ne devient visible qu'apres `700 ms`, puis le fondu croise vers `AppNavigation` dure `380 ms`.
- `StartupProgressSnapshot` derive la barre de quatre etapes locales reellement terminees. La quatrieme etape correspond a la premiere frame de la surface applicative initiale; aucun ETA ou compteur artificiel n'est affiche et les statuts passent par `SmartVisionStrings` en anglais/francais.
- `MainActivity` attend la premiere frame Compose avant de lancer les checks startup, puis compose `AppNavigation` sous `StartupExperience` et attend la premiere frame de Who's Watching (ou de la surface initiale sans picker) avant le handoff. Un timeout `2500 ms` evite tout blocage et une courte tenue de la progression terminee rend le passage a 100 % visible.
- `MainActivity` applique le theme normal et remplace le `windowBackground` par un fond opaque neutre juste avant `AppNavigation`, afin que le fond/logo splash ne restent pas visibles derriere Home.
- Depuis le 2026-07-03, `MainActivity` ne synchronise plus le catalogue et ne charge plus les categories/pages catalogue pendant le splash. Il lit seulement l'activation locale, efface toute demande startup residuelle, termine le splash rapidement et rend directement `AppNavigation` / Home. La decision de synchro automatique est deplacee apres le premier rendu Home; seule une vraie `Synchronize` peut bloquer la telecommande.
- `MainActivity` met en cache le snapshot d'activation locale lu pendant le splash dans `AppContainer`; `ActivationViewModel` l'utilise comme etat initial. Le chemin Home actif ne doit donc plus afficher l'ecran tampon `localStateReady=false` entre le splash et Home.
- Le splash systeme affiche le logo wide plus petit et plus haut; l'overlay Compose place la barre de progression centree sous ce logo et affiche une seule ligne `pourcentage + statut`.
- L'initialisation diagnostic `AppContainer` dans `SVPlayerApplication` est differee et lancee hors thread UI, pour ne pas bloquer le rendu initial du splash Compose.
- Depuis le 2026-07-05, `SVPlayerApplication` planifie aussi `EpgSyncScheduler.apply(...)` apres le demarrage differe. Ce Worker rafraichit l'EPG stale-aware avec contrainte reseau et ne lance pas de synchronisation catalogue complete.
- `AppNavigation` ne contient plus de `StartupHandoffScreen`; apres le statut `Demarrage en cours...`, `MainActivity` rend directement la navigation.
- `MainActivity` ne pose plus `@drawable/splash_background` comme fond de fenetre permanent afin d'eviter que le fond splash reapparaisse derriere Home; les logs `SVStartup` suivent les statuts startup.

## 3. Workflow utilisateur

Ce domaine n'est pas directement visible sauf:
- update in-app;
- installation APK;
- lancement TV;
- comportement si build instable ou version update mal publiee.

## 4. Workflow technique

Points d'entree:
- `MainActivity.kt`
- `SVPlayerApplication.kt`
- `AppNavigation.kt`
- `RemoteSettingsNavigation.kt` pour relayer les touches globales Settings/Menu vers la navigation Compose.
- `AppContainer.kt`
- `data/network/NetworkActivityTracker.kt`

Build:
- script complet prod: `.\scripts\release_prod.ps1`
- build manuel: `.\gradlew.bat assembleRelease`
- timeout 20 minutes minimum pour release;
- pas de `compileDebugKotlin` ni `testDebugUnitTest` avant release sauf demande explicite.
- avant le build, verifier que `versionCode` est strictement superieur a la prod et aux appareils ADB connectes avec `.\scripts\guard_release_version.ps1`;
- apres le build, relancer `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` pour verifier `output-metadata.json`.
- Diagnostic performance local: `.\gradlew.bat :app:assembleReleaseDiagnostic --no-daemon --max-workers=1 --console=plain` genere un APK `0.1.81-diag` avec `PERF_DIAGNOSTICS_ENABLED=true` et `profileable` shell. Ce variant est uniquement pour ADB/Firestick et ne doit pas etre deployee en prod. `PerformanceDiagnosticRecorder` ecrit les CSV/JSONL via un writer arriere-plan pour limiter la perturbation des mesures UI.

Deploy:
- le script `scripts/deploy_activation_phase1.ps1` n'assemble pas l'APK;
- il upload l'APK deja genere si `app/build/outputs/apk/release/app-release.apk` existe.
- apres chaque nouveau build APK release destine a etre livre, deployer aussi le backend avec `scripts/deploy_activation_phase1.ps1` afin de publier APK, manifeste, notification et fichiers PHP/CSS/JS associes.
- `scripts/release_prod.ps1` orchestre le chemin complet avec suivi visuel PowerShell: lecture version locale/prod, increment automatique de `versionCode`, garde-fou version, `:app:assembleRelease`, garde-fou metadata, `deploy_activation_phase1.ps1 -SkipInstall -SkipTests`, puis verification publique du manifeste, de `app_update.php`, de l'APK versionne et de l'APK stable.
- Par defaut, `release_prod.ps1` ne lance pas l'installation SQL temporaire et ne lance pas les tests publics du deploy; utiliser `-RunSqlInstall` ou `-RunDeployTests` seulement si ces controles sont explicitement souhaites.

## 5. Ecrans concernes

- Splash
- MainActivity
- Update dialog
- Notifications release

## 6. Fichiers de code concernes

- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/wrapper/*`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/SVPlayerApplication.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
- `app/src/main/java/com/smartvision/svplayer/data/home/HomeContentRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/network/NetworkActivityTracker.kt`
- `app/src/main/java/com/smartvision/svplayer/data/tmdb/*`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/EpgRepository.kt`
- `app/src/main/java/com/smartvision/svplayer/data/playlist/EpgSyncWorker.kt`
- `app/src/main/java/com/smartvision/svplayer/ui/update/*`
- `scripts/deploy_activation_phase1.ps1`
- `scripts/release_prod.ps1`

## 7. Donnees / API / Backend / Admin

Update public:
- `api/app_update.php`
- `downloads/smartvision-tv.version.json`
- `downloads/smartvision-tv-v{versionCode}-{hash}.apk`
- `downloads/smartvision-tv.apk`

Artefacts locaux:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/apk/release/output-metadata.json`
- `app/build/reports/profile/` si profiling Gradle.

Diagnostic Firestick / ADB:
- ADB Windows de reference pour ce workspace: `C:\Users\ONEDEV\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Firestick Wi-Fi connue: `192.168.1.33:5555` (`AFTSSS`, `sheldonp`) apres autorisation RSA cote TV.
- Pour les mesures de synchro Xtream, utiliser `scripts/capture_firestick_xtream_sync.ps1`; le script nettoie `logcat`, capture `SVSyncMemory`, releve `dumpsys meminfo com.smartvision.svplayer` et ecrit les resultats sous `diagnostics/firestick-sync-*`.
- Pour le diagnostic Splash/Home, utiliser `scripts/capture_firestick_splash_home_perf.ps1`; il installe l'APK `releaseDiagnostic`, force un cold start, capture `SVPerf`/`SVStartup`/`SVHomeFocus`, `gfxinfo`, `meminfo`, screenshots, fichiers app CSV/JSONL, genere `perf-diagnostics.xlsx` puis un ZIP sous `diagnostics/firestick-splash-home-perf-*`.
- Capture de reference apres optimisation candidate: `diagnostics/firestick-sync-20260701-123758`, deux synchronisations reussies sans OOM/ANR sur `21769 Live / 104005 Movies / 24325 Series`, pic `SVSyncMemory` environ `59 Mo`.

## 8. Dependances

- Backend/admin/deploy pour publication APK.
- Activation/config si update oriente vers notifications.
- Troubleshooting pour timeout Gradle.

## 9. Regles a ne pas casser

- Toujours incrementer `versionCode` avant une nouvelle release publiee.
- Ne jamais reutiliser un `versionCode` deja publie ou installe sur une TV de test; `guard_release_version.ps1` doit bloquer ce cas.
- Ne pas considerer une release terminee tant que l'APK n'est pas publie et verifie.
- Ne pas separer un build APK livrable du deploy backend: le backend doit etre redeploye apres chaque nouveau build release sauf demande explicite de build local uniquement.
- Ne pas relancer un build apres timeout sans verifier Java/Gradle et artefacts.
- Ne pas exposer les secrets de signature.
- Ne pas exposer les secrets TMDB; `local.properties.example` ne doit contenir qu'un placeholder vide.
- Ne pas activer minify/shrink sur le release urgent sans investigation separee.
- Garder `releaseOptimized` comme chemin distinct si optimisation lourde.
- Le variant `releaseDiagnostic` et les appels `PERF_DIAG` doivent rester faciles a supprimer apres analyse; ils ne doivent pas changer la logique utilisateur et ne doivent pas etre publies via `deploy_activation_phase1.ps1`.

## 10. Problemes connus

- `assembleRelease` peut continuer apres timeout.
- Reprise 2026-07-05: release locale `0.1.85` / `versionCode 89` preparee mais non publiee. Sur ce PC, `local.properties` et la keystore release sont absents; `assembleRelease` compile puis echoue a `:app:packageRelease` avec `SigningConfig "release" is missing required property "storeFile"`. Reprendre sur le PC qui possede `local.properties`, `keystore/release.keystore` et les secrets de signature/deploiement, puis relancer `assembleRelease` et `scripts/deploy_activation_phase1.ps1 -SkipInstall`.
- Le cache CDN peut servir l'APK stable ancien; verifier avec cache-buster ou APK versionne.
- `PROJECT_NOTES.md` peut contenir des versions obsoletes.
- Java par defaut peut ne pas etre JDK 21.

## 11. Quand lire ce fichier ?

Lire ce fichier si la demande concerne:
- build;
- release;
- assembleRelease;
- Gradle;
- versionCode;
- update APK;
- signature;
- AndroidManifest;
- AppContainer;
- architecture Android.

Ne pas lire ce fichier si la demande concerne uniquement:
- contenu PHP admin sans APK;
- UX sans build;
- simulation documentaire non executable.

## 12. Historique court

- 2026-07-09: release publiee `0.1.116` / `versionCode 131` pour corrections Movies/Series 3 colonnes: preview VOD plus tolerant aux seeks fournisseurs, details preview focusables/scrollables, lignes VOD compactes enrichies, maintien du focus ligne au premier OK, fiches detail Films/Series stabilisees au debut et overlay fullscreen VOD reorganise avec progressbar focusable. APK `smartvision-tv-v131-b854eab7.apk`, SHA256 `b854eab7100b4179f4f08ddb9b1511c99e65d9a065cf27e85d53c439fc299103`, taille `41083958`. Manifeste public, `api/app_update.php`, APK stable et APK versionne verifies.
- 2026-07-07: release publiee `0.1.116` / `versionCode 120` pour Media prives style YouTube: categories a gauche, liste compacte au centre, player/miniature dans la section principale, WebView embed focusable, controls Media3 visibles/focusables et overlay fullscreen bas retour/play/pause/-15s/+15s. APK `smartvision-tv-v120-e13560d3.apk`, SHA256 `e13560d3e4dab99ebec2bccdf5d95c7a7d67352c1f497b958849347d423a499c`, taille `40969256`. Manifeste public, `api/app_update.php`, APK stable et APK versionne verifies.
- 2026-07-07: release publiee `0.1.112` / `versionCode 116` pour Media 3 colonnes avec `Media local` expandable et bibliotheque privee proxy SmartVision: endpoints `api/media/private/*`, admin `Bibliotheque privee`, flags `private_media`, `private_media_eporner`, `private_media_native_playback`, et Android sans endpoint provider direct. APK `smartvision-tv-v116-23bd43e4.apk`, SHA256 `23bd43e4d227c029be794a5d128fa88ed3d443aa5b769f937cd9ae22c75f3cdb`, taille `40936499`. Manifeste public, `app_update.php`, `app_config.php` et endpoints private media verifies; provider desactive par defaut en prod.
- 2026-07-07: release publiee `0.1.110` / `versionCode 114` pour TMDB lots 7 et 8: Room schema `12`, TTL/fallback/nettoyage TMDB, details films/series scrollables avec trailers/teasers YouTube, casting/realisateur/createurs avec photos, note utilisateur locale, recommandations, Home Tendances en trailers TMDB sans lecture du flux Xtream d'origine et skeleton cards Continue/Tendances. APK `smartvision-tv-v114-f84c329b.apk`, SHA256 `f84c329bf7fbb5c11215713261a562827b1560bb4b076eb85a6d65b36ef7fdee`, taille `40854578`. Manifeste public, `app_update.php`, APK stable/versionne et `app_config.php` verifies. APK installe/lance sur Firestick `192.168.1.33:5555`; Home rendu, `versionCode=114`, aucun crash/ANR/Room error dans les logs filtres. Navigation Movies capturee, ouverture fiche detail via ADB non confirmee pendant la verification manuelle.
- 2026-07-07: release publiee `0.1.109` / `versionCode 113` pour integration TMDB lots 1 a 6: cache Room local, matching film/serie, details film/serie enrichis, Home tendances enrichies sans splash massif, catalogues cache-only et Settings `TMDB attribution`. APK `smartvision-tv-v113-6b467713.apk`, SHA256 `6b467713c176c876eee2517b36a9592238d54d3c72e19bb57cd9572e879cbab3`, taille `40821819`. Manifeste public, `app_update.php`, APK stable/versionne, `app_config.php`, `ads-config` et VAST verifies. APK installe/lance sur Firestick `192.168.1.33:5555`; Home rendu, `TMDB token` actif dans Settings, appel `TMDB: search tv` HTTP 200 observe dans Network Activity, aucun crash/ANR/Room error dans les logs filtres.
- 2026-07-07: release finale publiee `0.1.107` / `versionCode 111` pour corrections Live TV focus categorie initiale, skeleton interne Chaines, logos chaines, section EPG/Info chaine et bloc Premium minimal luxury. APK final a verifier dans le manifeste public apres build/deploy.
- 2026-07-07: ajout local de la couche TMDB optionnelle: `BuildConfig.TMDB_READ_ACCESS_TOKEN`, client Retrofit TMDB dans `AppContainer`, Network Activity type `Tmdb`, cache Room version 11 et attribution Settings. Validation effectuee par `:app:compileReleaseKotlin`.
- 2026-07-07: releases intermediaires `0.1.104` / `versionCode 108`, `0.1.105` / `versionCode 109` et `0.1.106` / `versionCode 110` publiees puis remplacees par `0.1.107 (111)` afin que le depot local final corresponde a l'APK publie, au correctif focus categorie observe sur Firestick et au bloc Premium non clippe.
- 2026-07-06: release locale `0.1.103` / `versionCode 107` construite avec `:app:assembleRelease` pour raccourcis telecommande Settings/Menu globaux. `MainActivity.dispatchKeyEvent()` intercepte uniquement `KEYCODE_SETTINGS`, `KEYCODE_MENU` et `KEYCODE_MEDIA_TOP_MENU`, consomme `ACTION_DOWN`, ouvre Settings sur `ACTION_UP` via `RemoteSettingsNavigation` et `navigateSingleTop()`, puis laisse toutes les autres touches a `super.dispatchKeyEvent(event)`. APK local `app-release.apk`, SHA256 `B05B9994123874475B9EA0F25D52878425907FE388664E9E5C754567983F20E8`, taille `40759014`. Aucun deploiement prod effectue.
- 2026-07-06: release publiee `0.1.102` / `versionCode 106` pour correction Live TV focus/categorie initiale/EPG: selection auto recalculee apres snapshot partiel et tri `sortedByHistorySignals(...)`, annulations `channelsJob` normales non affichees comme `standaloneCoroutine was cancelled`, restauration focus Lazy via `scrollToItem` + `visibleItemsInfo`, recherche entre chaines et header, icone EPG fournie copiee telle quelle, typographies reduites et panneau sans EPG integre aux lignes. APK `smartvision-tv-v106-73da2e18.apk`, SHA256 `73da2e187ea9d5043599608c8268a17d3c0b1af711bfb79cb43474d3bb1edd12`, taille `40759019`, manifeste public, `app_update.php`, APK stable et APK versionne verifies.
- 2026-07-06: release publiee `0.1.101` / `versionCode 105` pour Live TV premium focus header et panneau sans EPG: skeleton 3 panneaux minimum 1 seconde, selection initiale hors `Historique`, focus header reusable vers onglet courant/categories, padding headers, categories compactes, noms chaines agrandis, icone EPG regeneree en PNG transparent avec halo neon, indicateur EPG non focusable dans le header Apercu, overlay mini-player plus glassmorphism, titres EPG agrandis et panneau `A propos de la chaine` sans EPG. APK `smartvision-tv-v105-34681073.apk`, SHA256 `34681073bcb47ad115d1f61afebf2af678ff359ed2d3bcf5fe7b52645496db7b`, taille `40399158`, manifeste public, `app_update.php`, APK stable et APK versionne verifies.
- 2026-07-05: release publiee `0.1.100` / `versionCode 104` pour Live TV UI/focus/EPG: skeleton 3 panneaux, i18n Live TV, categories/chaines compactes, badge EPG image, logos sans fond, numerotation par dossier, suppression Historique dans le header Apercu, mini-player apercu avec overlay bas, lignes EPG focusables, refresh EPG stale-aware et Worker EPG horaire. Aucun filtre admin/API `###` ajoute. APK `smartvision-tv-v104-b7a7822d.apk`, SHA256 `b7a7822d4fab7dfa29e0b22468383cb23723b8c28be617be83cb932ad886f5df`, taille `40351582`, manifeste public, `app_update.php`, APK stable et APK versionne verifies.
- 2026-07-05: release publiee `0.1.99` / `versionCode 103` pour Media compact aligne Live TV: ratios `0.24 / 0.42 / 0.34`, suppression hero/snapshot/stats, hub `Telephone -> TV` sans bouton cache, tuile `TV -> Phone`, details fichier simplifies, mini-player local video remplissant son cadre comme Live TV et anti-flash couronne Media pendant le chargement app-config. APK `smartvision-tv-v103-b1f73b9d.apk`, SHA256 `b1f73b9df54227edc898c640f84aef49abbcbbab66c607341a32feb3767ee4a6`, taille `40314076`, manifeste public, `app_update.php`, APK stable et APK versionne verifies.
- 2026-07-05: release publiee `0.1.96` / `versionCode 100` pour refonte premium Media Center: hero studio, compteurs, stockage intelligent, hub transfert telephone, stats bibliotheque, lignes fichiers enrichies et preview hero. APK `smartvision-tv-v100-bd7812fb.apk`, SHA256 `bd7812fbabd30519b6810c9b80364097a582b0a43818edf9c29574a4b38a5c8f`, manifeste public, `app_update.php`, APK stable et versionne verifies. Parametres admin non modifies volontairement.
- 2026-07-05: release publiee `0.1.95` / `versionCode 99` pour rendre les actions Media Center plus accessibles: `Supprimer` est aligne avec `Lire` et `Renommer`, les dialogs ciblent le fichier par id explicite et la confirmation de suppression prend le focus TV. APK `smartvision-tv-v99-a8450f83.apk`, SHA256 `a8450f83e6e06c8b4f912094acb2a8a63a7020405a994244c336bec8d4f18536`, manifeste public, `app_update.php`, APK stable et flags Recorder/Media verifies.
- 2026-07-05: release publiee `0.1.94` / `versionCode 98` pour stabilisation Recorder Live: fermeture player/chaine sans arret Recorder, reconnexion progressive prolongee, activite `Recorder` dans Network Activity sans URL Xtream, et polish Media UI/focus/transfert. APK `smartvision-tv-v98-f861e960.apk`, SHA256 `f861e960078050444cf323de1719b0ce1594c92e3f9101b391e6de2d2568b936`, manifeste public, `app_update.php`, APK stable et flags Recorder/Media verifies.
- 2026-07-05: release publiee `0.1.93` / `versionCode 97` pour Lots 12 et 13 Media Transfer: serveur HTTP local temporaire `MediaTransferServer`, QR import telephone vers TV dans `SmartVisionMedia/Transfers`, QR export du fichier Media selectionne, gate `media_phone_transfer`. APK `smartvision-tv-v97-68adc147.apk`, SHA256 `68adc147ebd988359f7402e6b77dc9f4fa7917c92d4f897955ee858bf21c3a9b`, manifeste public, `app_update.php`, APK stable et flags app_config verifies.
- 2026-07-05: release publiee `0.1.92` / `versionCode 96` pour lots 7 et 8 Recorder + Media: lecture locale video/audio/photo, route `media_player/{mediaFileId}`, viewer photo plein ecran, bouton Record Live gate/couronne/dialog MVP et retouches focus/design header/Media. APK `smartvision-tv-v96-c91c7b49.apk`, SHA256 `c91c7b49f87e9a6f41d83c40ed58c3c9ca94e1bdb0bb974c4600875df77655e8`, manifeste public, `app_update.php`, APK stable et taille verifies.
- 2026-07-05: release publiee `0.1.91` / `versionCode 95` pour Lots 5 et 6 Media Center: Room `media_folders`/`media_files`/`recording_jobs`, stockage app-specific `SmartVisionMedia`, scan fichiers/dossiers et actions Renommer/Deplacer/Supprimer. APK `smartvision-tv-v95-f66182d2.apk`, SHA256 `f66182d26909fac5a5011357992fdb9963878aa6ec3fcd354d0866180469a6da`, manifeste public, `app_update.php`, APK stable et taille verifies.
- 2026-07-05: release publiee `0.1.90` / `versionCode 94` pour retouches overlay Live TV: Haut/Bas rend la priorite aux panneaux EPG/Settings ouverts, suppression du flash numero technique 6 chiffres, retrait logo SmartVision haut gauche, titre chaine legerement reduit, boutons plus serres/opaques et focus moins epais. APK `smartvision-tv-v94-f4ad6681.apk`, SHA256 `f4ad66813f308ec109186881b731c82939aec17eec45154da56e27b57c2aa674`, manifeste public, `app_update.php`, APK stable et taille verifies.
- 2026-07-05: release publiee `0.1.89` / `versionCode 93` pour affiner l'overlay plein ecran Live TV: bandeau rectangulaire compact, logo chaine sans cadre, numero reel, EPG local courant, zapping haut/bas prioritaire, panneau Settings focusable/aspect ratio enrichi et retour focus liste sur la chaine ouverte. APK `smartvision-tv-v93-36ec0343.apk`, SHA256 `36ec03430e065b5234252ee2142cf7b057e8429a62894fa15d90e3e737a03bcb`, manifeste public, `app_update.php`, APK stable et taille verifies.
- 2026-07-05: release publiee `0.1.88` / `versionCode 92` pour remplacer l'overlay plein ecran Live TV par le bandeau bas glassmorphism dedie direct, EPG lateral, panneau Settings aspect ratio et Record placeholder. APK `smartvision-tv-v92-4709cf61.apk`, SHA256 `4709cf614b68ae0c489efb7323e9aca9a9f096a7a2979bfc2007b0e28c2e95c7`, manifeste public, `app_update.php`, APK stable et taille verifies.
- 2026-07-03: release publiee `0.1.84` / `versionCode 87` pour corriger les historiques Series: sauvegarde/enrichissement des episodes avec titre de serie, poster, label saison/episode et parent serie. APK `smartvision-tv-v87-60b47236.apk`, SHA256 `60b472366e98c48e33b7c23f0ae495bdd9e7fc07a672aa18c5bffa631d76a109`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-05: release publiee `0.1.86` / `versionCode 90` pour correction audio mini-players Home Continue/Tendances et Settings `Network Activity` / `Activite reseau`; APK `smartvision-tv-v90-80b95e80.apk`, SHA256 `80b95e80e07d5f4786e1fb5247f60d014a66324399db4ed4c9ad10c8a51e5df0`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-05: release `0.1.87` / `versionCode 91` preparee pour rendre Settings > Activite reseau scrollable au D-pad et publier l'optimisation `api/notifications.php` contre les timeouts socket.
- 2026-07-03: release publiee `0.1.83` / `versionCode 86` pour splash sans synchro ni chargement catalogue, Home immediat, synchro post-Home uniquement, categories initiales limitees a `20` par section et tendances `10 + 10` aleatoires Room hors adulte. APK `smartvision-tv-v86-9fcd8f69.apk`, SHA256 `9fcd8f69555e3c7e99b495d8c136134f6d3814ef758afc21e17d97dd26568751`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-05: ajout local de `NetworkActivityTracker` et du menu Settings `Network Activity` / `Activite reseau`; instrumentation OkHttp SmartVision/Xtream sans query params, plus travaux metier catalogue, M3U, EPG, Home et update APK.
- 2026-07-03: release locale `0.1.82` / `versionCode 85` construite avec `:app:assembleRelease`, garde-fou `guard_release_version.ps1 -RequireBuildMetadata` OK, APK `app-release.apk` installe et lance sur Firestick `192.168.1.33:5555`; pas de deploiement backend/prod effectue dans cette intervention.
- 2026-07-02: release publiee `0.1.81` / `versionCode 84` pour splash hybride: fond + logo reduit dans la preview systeme, barre/statuts/diagnostics en Compose, nettoyage du fond avant Home; APK `smartvision-tv-v84-1577e450.apk`, SHA256 `1577e4508feb3ae94d5ba672f67ff851d0a1779b6b94a34dd257044b4a65afb0`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-03: optimisation locale `releaseDiagnostic`: recorder diagnostic asynchrone, splash logo plus petit/remonte, progress bar centree sous le logo et statut simplifie avec pourcentage.
- 2026-07-02: release publiee `0.1.80` / `versionCode 83` pour splash systeme fond + logo immediat, anti-flash activation, cache final tendances startup, mini-player Continue watching immediat, ordre initial des tendances Home, refresh local du menu HOME et ancrage horizontal Home; APK `smartvision-tv-v83-90e2e35d.apk`, SHA256 `90e2e35df36f5b33bc20d6aaa19c366904989784f9c676915368909d9daff28f`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-02: release publiee `0.1.78` / `versionCode 81` pour stabiliser le scroll/focus vertical Home Continue watching et Trending; APK `smartvision-tv-v81-bcf5a8d7.apk`, manifeste public, `app_update.php`, APK stable et hash SHA256 verifies.
- 2026-06-29: migration vers documentation specialisee.
- 2026-07-02: release publiee `0.1.76` / `versionCode 79` pour mini-player Home poster paysage, filtre tendances avec `backdropUrl`, demarrage video apres 4 secondes de focus et fondu plus lent; APK `smartvision-tv-v79-54bfa614.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-02: release publiee `0.1.75` / `versionCode 78` pour supprimer l'ecran noir apres splash, renforcer le focus Home vers Tendances, corriger le clipping gauche des carrousels et ajouter les logs `SVStartup` / `SVHomeFocus`.
- 2026-07-02: release publiee `0.1.74` / `versionCode 77` pour affichage immediat logo/progress splash, prechauffage categories/premieres pages, mini-player Continue watching et focus Home corrige; APK `smartvision-tv-v77-055a7642.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-02: release publiee `0.1.73` / `versionCode 76` pour retour splash image, Home tendances films/series, liste `trending_media` validee et mini-preview tendances; APK `smartvision-tv-v76-6a70e99f.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.71` / `versionCode 74` pour splash sans noir avant premiere frame, EPG streaming borne, filtre categories EPG Live TV, splash categories-only et correction crash focus D-pad; APK `smartvision-tv-v74-d76a5da8.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: diagnostic local Firestick pour synchro Xtream volumineuse; release installee localement par ADB, deux synchros reussies sans OOM apres traitement par sections/batchs.
- 2026-07-01: release publiee `0.1.70` / `versionCode 73` pour source M3U/splash source-aware, badge EPG Live TV, Info compte compact et overlay player slim; APK `smartvision-tv-v73-5ab10617.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.69` / `versionCode 72` pour splash video Compose Media3 au demarrage; APK `smartvision-tv-v72-a30ec7bf.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.68` / `versionCode 71` pour source active Xtream/M3U, parsing M3U Live TV, cache EPG XMLTV et Info compte compact; APK `smartvision-tv-v71-44e92a70.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.67` / `versionCode 70` pour Info compte, URL EPG et page web `/playlist/`; APK `smartvision-tv-v70-50bfb24e.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.66` / `versionCode 69` pour agrandissement du splash et suppression du flash activation apres splash; APK `smartvision-tv-v69-cb3e3030.apk`, manifeste public et hash SHA256 verifies.
- 2026-07-01: release publiee `0.1.65` / `versionCode 68` pour splash unique et prechargement Home/catalogues au demarrage; APK `smartvision-tv-v68-e4732ab9.apk`, manifeste public et hash SHA256 verifies.
- 2026-06-30: release publiee `0.1.62` / `versionCode 65`; APK `smartvision-tv-v65-db9e9d1a.apk`, manifeste public et hash SHA256 verifies.
- 2026-06-30: release `0.1.61` / `versionCode 64` pour performance YouTube: WebView conserve entre videos via `loadVideoById`, qualite cible `medium`, queue autoplay 20, popup parametres YouTube et header mini-player corrige.
- 2026-06-30: release `0.1.60` / `versionCode 63` pour ameliorations YouTube: controles affiches uniquement a la lecture reelle, progress bar, reload, previous via historique, favoris locaux, bouton retour mini/fullscreen et reprise de position.
- 2026-06-30: release `0.1.59` / `versionCode 62` pour correction focus/actions boutons YouTube; le conteneur player ne consomme plus les touches quand un bouton du bandeau est focus.
- 2026-06-29: ajout de la regle release: bypass debug/test et timeout release 15 minutes.
- 2026-06-30: ajout du garde-fou `scripts/guard_release_version.ps1` et de la regle deploy backend apres build release.
- 2026-06-30: release publiee `0.1.51` / `versionCode 54`.
- 2026-06-30: release publiee `0.1.52` / `versionCode 55`.
- 2026-06-30: release publiee `0.1.53` / `versionCode 56`.
- 2026-06-30: release publiee `0.1.54` / `versionCode 57`.
- 2026-06-30: release publiee `0.1.55` / `versionCode 58`.
- 2026-06-30: release publiee `0.1.56` / `versionCode 59`.
- 2026-06-30: release publiee `0.1.57` / `versionCode 60`.
- 2026-06-30: release publiee `0.1.58` / `versionCode 61`.
