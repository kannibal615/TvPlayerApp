# Android Architecture, Build et Release

Derniere mise a jour: 2026-06-30.

## 1. Objectif

Documenter l'architecture Android active, les points d'entree techniques, le build release, la signature et les controles avant publication.

## 2. Fonctionnement actuel

L'application Android est dans `app/`. La navigation active est Compose dans `ui/navigation/AppNavigation.kt`. Les dependances sont creees dans `core/data/AppContainer.kt`. Le projet demande JDK 21.

Gradle local constate le 2026-06-30:
- `versionCode = 64`
- `versionName = "0.1.61"`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 23`
- release standard sans minify/shrink
- `releaseOptimized` avec minify/shrink

## 3. Workflow utilisateur

Ce domaine n'est pas directement visible sauf:
- update in-app;
- installation APK;
- lancement TV;
- comportement si build instable ou version update mal publiee.

## 4. Workflow technique

Points d'entree:
- `SplashActivity.kt`
- `MainActivity.kt`
- `SVPlayerApplication.kt`
- `AppNavigation.kt`
- `AppContainer.kt`

Build:
- `.\gradlew.bat assembleRelease`
- timeout 15 minutes minimum pour release;
- pas de `compileDebugKotlin` ni `testDebugUnitTest` avant release sauf demande explicite.
- avant le build, verifier que `versionCode` est strictement superieur a la prod et aux appareils ADB connectes avec `.\scripts\guard_release_version.ps1`;
- apres le build, relancer `.\scripts\guard_release_version.ps1 -RequireBuildMetadata` pour verifier `output-metadata.json`.

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
- `app/src/main/java/com/smartvision/svplayer/SplashActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/MainActivity.kt`
- `app/src/main/java/com/smartvision/svplayer/SVPlayerApplication.kt`
- `app/src/main/java/com/smartvision/svplayer/core/data/AppContainer.kt`
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

- 2026-06-29: migration vers documentation specialisee.
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
