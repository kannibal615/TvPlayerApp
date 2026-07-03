# Android Architecture, Build et Release

Derniere mise a jour: 2026-07-03.

## 1. Objectif

Documenter l'architecture Android active, les points d'entree techniques, le build release, la signature et les controles avant publication.

## 2. Fonctionnement actuel

L'application Android est dans `app/`. La navigation active est Compose dans `ui/navigation/AppNavigation.kt`. Les dependances sont creees dans `core/data/AppContainer.kt`. Le projet demande JDK 21.

Gradle local constate le 2026-07-03:
- `versionCode = 88`
- `versionName = "0.1.85"`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 23`
- release standard sans minify/shrink
- `releaseOptimized` avec minify/shrink

Demarrage:
- `MainActivity` est l'activite launcher TV/Android et utilise `Theme.SVPlayer.Splash` pour la preview systeme immediate.
- Le theme systeme `Theme.SVPlayer.Splash` utilise `@drawable/splash_background` comme preview immediate; cette preview contient le fond splash et le logo wide reduit/centre.
- Le startup Compose de `MainActivity` ne redessine plus le fond ni le logo: il garde la preview systeme visible et affiche seulement la progress bar, le statut et les diagnostics.
- Les diagnostics startup visibles incluent pourcentage, etape courante/total, elements traites/restants quand connus, temps ecoule, ETA estimee et details Live/Films/Series pendant une synchronisation catalogue.
- `MainActivity` attend la premiere frame Compose avant de lancer les checks startup, pour afficher immediatement la barre et les statuts au-dessus de la preview systeme.
- `MainActivity` applique le theme normal et remplace le `windowBackground` par un fond opaque neutre juste avant `AppNavigation`, afin que le fond/logo splash ne restent pas visibles derriere Home.
- Depuis le 2026-07-03, `MainActivity` ne synchronise plus le catalogue et ne charge plus les categories/pages catalogue pendant le splash. Il lit seulement l'activation locale, efface toute demande startup residuelle, termine le splash rapidement et rend directement `AppNavigation` / Home. La decision de synchro automatique est deplacee apres le premier rendu Home; seule une vraie `Synchronize` peut bloquer la telecommande.
- `MainActivity` met en cache le snapshot d'activation locale lu pendant le splash dans `AppContainer`; `ActivationViewModel` l'utilise comme etat initial. Le chemin Home actif ne doit donc plus afficher l'ecran tampon `localStateReady=false` entre le splash et Home.
- Le splash systeme affiche le logo wide plus petit et plus haut; l'overlay Compose place la barre de progression centree sous ce logo et affiche une seule ligne `pourcentage + statut`.
- L'initialisation diagnostic `AppContainer` dans `SVPlayerApplication` est differee et lancee hors thread UI, pour ne pas bloquer le rendu initial du splash Compose.
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
- `AppContainer.kt`

Build:
- `.\gradlew.bat assembleRelease`
- timeout 20 minutes minimum pour release;
- pas de `compileDebugKotlin` ni `testDebugUnitTest` avant release sauf demande explicite.
- avant le build, verifier que `versionCode` est strictement superieur a la prod et aux appareils ADB connectes avec `.\scripts\guard_release_version.ps1`;
- apres le build, relancer `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` pour verifier `output-metadata.json`.
- Diagnostic performance local: `.\gradlew.bat :app:assembleReleaseDiagnostic --no-daemon --max-workers=1 --console=plain` genere un APK `0.1.81-diag` avec `PERF_DIAGNOSTICS_ENABLED=true` et `profileable` shell. Ce variant est uniquement pour ADB/Firestick et ne doit pas etre deployee en prod. `PerformanceDiagnosticRecorder` ecrit les CSV/JSONL via un writer arriere-plan pour limiter la perturbation des mesures UI.

Deploy:
- le script `scripts/deploy_activation_phase1.ps1` n'assemble pas l'APK;
- il upload l'APK deja genere si `app/build/outputs/apk/release/app-release.apk` existe.
- apres chaque nouveau build APK release destine a etre livre, deployer aussi le backend avec `scripts/deploy_activation_phase1.ps1` afin de publier APK, manifeste, notification et fichiers PHP/CSS/JS associes.

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
- `app/src/main/java/com/smartvision/svplayer/ui/update/*`
- `scripts/deploy_activation_phase1.ps1`

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
- Ne pas activer minify/shrink sur le release urgent sans investigation separee.
- Garder `releaseOptimized` comme chemin distinct si optimisation lourde.
- Le variant `releaseDiagnostic` et les appels `PERF_DIAG` doivent rester faciles a supprimer apres analyse; ils ne doivent pas changer la logique utilisateur et ne doivent pas etre publies via `deploy_activation_phase1.ps1`.

## 10. Problemes connus

- `assembleRelease` peut continuer apres timeout.
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

- 2026-07-03: release publiee `0.1.84` / `versionCode 87` pour corriger les historiques Series: sauvegarde/enrichissement des episodes avec titre de serie, poster, label saison/episode et parent serie. APK `smartvision-tv-v87-60b47236.apk`, SHA256 `60b472366e98c48e33b7c23f0ae495bdd9e7fc07a672aa18c5bffa631d76a109`, manifeste public, `app_update.php`, APK stable et hash verifies.
- 2026-07-03: release publiee `0.1.83` / `versionCode 86` pour splash sans synchro ni chargement catalogue, Home immediat, synchro post-Home uniquement, categories initiales limitees a `20` par section et tendances `10 + 10` aleatoires Room hors adulte. APK `smartvision-tv-v86-9fcd8f69.apk`, SHA256 `9fcd8f69555e3c7e99b495d8c136134f6d3814ef758afc21e17d97dd26568751`, manifeste public, `app_update.php`, APK stable et hash verifies.
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
