# Troubleshooting

## 2026-07-18 - Who's Watching bloque sur Preparing Home apres plusieurs changements de profil

Problem:
- Admin -> Kids ou Kids -> Admin pouvait rester plusieurs minutes sur `Preparing Home` alors que la synchronisation, les compteurs et les tendances etaient deja termines.

Context:
- Le signal Home pret attendait aussi `continueWatchingLoading`.
- Une reprise d'episode absente de Room appelait `getOrFetchSeriesEpisodes`; jusqu'a 10 reprises pouvaient donc enchainer des appels Xtream de 40 s.
- Une bascule incrementait deux fois `catalogRevision` et pouvait annuler un nouveau job Home tout juste lance.
- Depuis `Info profil` ou `Gerer les profils`, le picker pouvait attendre le callback d'un Home non compose.
- Une fin de synchronisation pouvait publier `Success` avant que Home consomme la nouvelle revision: l'ancien snapshot devenait alors provisoirement `ready` avec `0/0/0`.
- `refreshCatalogCounts()` marquait auparavant son token charge dans `finally`, y compris apres erreur ou annulation; une tentative remplacee pouvait donc valider les zeros d'initialisation.

Working solution:
- Limiter le verrou de transition aux donnees catalogue du profil capture: synchronisation inactive, revision chargee, compteurs et tendances prets.
- Charger Continue Watching en best effort, uniquement depuis Room pendant l'ouverture; les metadonnees distantes restent hors du chemin critique.
- Passer le `profileId` capture aux lectures compteurs/tendances et refuser les resultats d'un ancien token.
- Utiliser le coordinateur de navigation comme unique increment explicite de revision au moment de l'activation, annuler puis mettre les jobs a `null`, et maintenir le coordinateur Home actif hors de `HomeScreen`.
- Observer ensemble `activeProfileId + catalogRevision`, avec le token initial capture avant le lancement du collector afin de ne manquer aucune bascule precoce.
- Identifier chaque rechargement par une generation et chaque requete compteurs/tendances par un ID monotone; seule la derniere tentative reussie peut publier et marquer la revision chargee.
- Publier atomiquement compteurs, flag de chargement et revision chargee dans le meme `HomeLoadGate`; ne pas recombiner ces trois valeurs depuis des `StateFlow` independants.
- Fermer Who's Watching uniquement avec un jeton `(profileId, catalogRevision)` revalide apres une frame contre `catalogRepository.catalogRevision.value`.
- Verifier les compteurs Room avec le `profileId` cible avant et apres activation: une empreinte a jour ne suffit pas si le catalogue local est vide; forcer alors une synchronisation et rester sur le picker avec erreur si elle ne produit toujours aucun contenu.

Avoid next time:
- Ne jamais mettre un enrichissement reseau ou une rangee facultative dans le verrou de navigation d'un profil.
- Ne jamais incrementer la meme generation catalogue depuis deux coordinateurs concurrents.
- Ne jamais transformer un `finally`, une annulation ou une erreur Room en succes de chargement; `0` peut etre un vrai catalogue vide mais ne prouve pas qu'une requete a abouti.
- Toujours tester dans le meme processus `Admin -> Kids -> Admin`, avec reprise episode absente et resultats retardes.

## 2026-07-18 - Playlist web enregistree sans notification visible et profil Kids stale

Problem:
- Le site pouvait enregistrer une nouvelle configuration de playlist tout en ne garantissant pas la creation de `playlist_added`.
- Un profil Kids partageant la source Admin pouvait ensuite reutiliser un catalogue local devenu obsolete apres la livraison web.

Context:
- La configuration et la notification etaient deux operations independantes, et le resultat notification n'etait pas expose au site.
- Le rafraichissement Android pouvait echouer silencieusement ou ne se produire qu'au polling suivant.
- La fraicheur catalogue ne tenait pas compte de la source resolue partagee, du type de profil et des filtres actifs.

Working solution:
- Executer configuration + creation de notification dans la meme transaction et retourner `notification_created`, `notification_id` et `notification_reason`.
- Cibler et dedupliquer `playlist_added` avec l'identifiant canonique du meme appareil, puis journaliser les erreurs Android sans secrets.
- Rafraichir les notifications apres import, au retour au premier plan et par polling.
- Comparer `CatalogSyncFingerprint` a chaque activation; un Kids partageant la source Admin est resynchronise avant Home si l'empreinte attendue a change.

Avoid next time:
- Ne jamais afficher un succes web complet si la notification associee n'a pas ete creee ou explicitement dedupliquee.
- Ne jamais deduire la fraicheur d'un profil partage uniquement depuis sa propre date de derniere synchronisation.

## 2026-06-30 - Release rebuilt with an already published versionCode

Problem:
- A local `assembleRelease` generated `0.1.50 (53)` while production and the test TV could already have `versionCode 53`.

Context:
- `app/build.gradle.kts` still had `versionCode = 53` and `versionName = "0.1.50"`.
- `downloads/smartvision-tv.version.json` and `api/app_update.php` already advertised `0.1.50 (53)`.
- Gradle does not auto-increment Android versions; rebuilding only regenerates an APK with the same version metadata.

Working solution:
- Before any release build, bump `app/build.gradle.kts` to a strictly higher `versionCode`.
- Run `.\scripts\guard_release_version.ps1` before and after `assembleRelease`.
- After a livrable release build, deploy backend with `scripts/deploy_activation_phase1.ps1` so update manifests and server files are synchronized.

Avoid next time:
- Do not call a rebuilt APK "new release" until `output-metadata.json` shows a versionCode greater than production and test devices.
- Do not deploy or test upgrade flow with a reused `versionCode`.

## 2026-06-29 - Admin HTTP 500 after adding a new PHP service

Problem:
- The production admin panel can return HTTP 500 when `admin/index.php` requires a new PHP service file that was not uploaded by the deployment script.

Context:
- Device diagnostics added `api/device_diagnostics_service.php` and `api/app/device-diagnostics.php`.
- `scripts/deploy_activation_phase1.ps1` uploads PHP files explicitly, so new files must be added to its upload list.
- Admin startup should not depend on optional diagnostics schema creation.

Working solution:
- Make the admin diagnostics service include optional and wrap diagnostics reads in `try/catch`.
- Add new diagnostics files to `scripts/deploy_activation_phase1.ps1`.
- Validate with:
  `php -l server/public_html/admin/index.php`
  `php -l server/public_html/api/device_diagnostics_service.php`
  `php -l server/public_html/api/app/device-diagnostics.php`

Avoid next time:
- Do not assume new PHP files are deployed automatically by the release script.
- Do not let optional admin widgets create schema or throw unhandled errors during admin page boot.

## 2026-06-28 - Release Gradle timeout while build continues

Problem:
- The `assembleRelease` command can reach the terminal timeout while Gradle/Java is still compiling in the background.

Context:
- SmartVision Android TV release build.
- Command shape: `.\gradlew.bat --% assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process`.
- A timeout does not always mean the build failed.

Working solution:
- Check active Java/Gradle processes before doing anything else:
  `Get-Process java,gradle -ErrorAction SilentlyContinue | Select-Object Id,ProcessName,CPU,StartTime,Path`
- If Java is still consuming CPU, wait and poll again.
- Confirm completion by checking:
  `app/build/outputs/apk/release/app-release.apk`
  `app/build/outputs/apk/release/output-metadata.json`

Avoid next time:
- Do not kill Java or relaunch Gradle immediately after a timeout.
- Do not deploy until `output-metadata.json` confirms the expected `versionCode` and `versionName`.

## 2026-06-28 - Existing app config missing new dynamic features

Problem:
- Adding a new default feature in PHP is not enough when `app_feature_access` already exists in production.
- The API may continue returning the old stored feature list, so Android does not receive the new feature key.

Context:
- `parental_control` was added after the initial `youtube`, `replay`, `advanced_favorites`, `multi_screen` and `local_cache` feature list.
- Production still returned the old JSON until the API normalized stored settings against current defaults.

Working solution:
- Normalize the stored feature list against `app_config_default_features()` before returning it from `api/app_config.php`.
- Keep existing admin choices for known keys, but append any missing default keys.
- After deploy, verify with:
  `Invoke-RestMethod https://<domain>/api/app_config.php | ConvertTo-Json -Depth 10`

Avoid next time:
- Do not rely only on changing default arrays when production stores an older JSON setting.
- Verify the public API output after admin/config changes, not only PHP syntax.

## 2026-06-28 - Stored feature flags can override expected free_ads locks

Problem:
- The code defaults may correctly set `free_ads = false`, but production can still return `free_ads = true` when the admin-stored `app_feature_access` setting contains older or manual values.

Context:
- After deploying release `0.1.40 (43)`, `api/app_config.php` returned `free_ads = true` for `youtube` and `parental_control`.
- This would prevent the Android lock/greyed state from applying to `free_ads` users even though the Android and PHP default code were correct.

Working solution:
- Re-save the "Gestion des fonctionnalites" admin form with only Premium and Trial enabled for YouTube and parental control.
- Re-check:
  `Invoke-RestMethod https://<domain>/api/app_config.php | ConvertTo-Json -Depth 10`
- Confirm `youtube.free_ads = false` and `parental_control.free_ads = false`.

Avoid next time:
- Do not assume dynamic feature defaults are active in production after deploy.
- Always verify the live `api/app_config.php` output before closing a release involving feature locks.

## 2026-06-28 - Temporary PHP maintenance scripts on cPanel

Problem:
- A temporary PHP script can fail or be unreachable because of local file encoding or PowerShell string expansion.
- cPanel `Fileman/delete_files` is not available on this host, so cleanup cannot rely on that function.

Context:
- Used for one-off production DB maintenance after deployment, such as correcting `app_feature_access`.
- PowerShell `$Host` is a reserved variable name.
- In a double-quoted PowerShell URL, write `${name}?token=${token}` instead of `$name?token=$token`.

Working solution:
- Write temporary PHP files with UTF-8 without BOM:
  `[IO.File]::WriteAllText($tmp, $php, [Text.UTF8Encoding]::new($false))`
- Upload with cPanel `Fileman/upload_files` to `public_html/api`.
- Make the PHP script self-delete in `finally { @unlink(__FILE__); }`.
- If cleanup is needed and the token is lost, overwrite the same filename with a small self-deleting cleanup script and call it once.

Avoid next time:
- Do not use `Set-Content -Encoding UTF8` for PHP files that start with `declare(strict_types=1);`, because a BOM can break PHP strict types.
- Do not use `$Host` as a local PowerShell variable.
- Do not assume cPanel has `Fileman/delete_files`; prefer self-deleting scripts.

## 2026-06-28 - Temporary API maintenance script missing db()

Problem:
- A temporary PHP maintenance script uploaded under `public_html/api` returned HTTP 500 with `Call to undefined function db()`.

Context:
- The script required only `api/helpers.php`.
- In this backend, `db()` is declared in `api/config.php`, while `helpers.php` only uses it indirectly in normal API entrypoints after config has already been loaded.

Working solution:
- Include config before helpers in temporary API scripts:
  `require_once __DIR__ . '/config.php';`
  `require_once __DIR__ . '/helpers.php';`
- Validate generated PHP locally with `php -l` before upload.
- If PowerShell hides the HTTP 500 body, call the temporary URL with `curl.exe -i` to see the JSON error body.

Avoid next time:
- Do not assume `helpers.php` is enough for standalone maintenance scripts.
- Keep the self-delete shutdown hook, but use `curl.exe -i` during diagnosis so the response body is visible before the file deletes itself.
## 2026-06-30 - Xtream KO non detecte si ancien cache local existe

Problem:
- Sur TV avec code public `LAUU9M`, un compte Xtream serveur timeout, mais l'app affichait encore l'ancien catalogue Room et n'affichait pas d'alerte.
- L'ouverture d'un media restait en loading infini et aucune anomalie Xtream n'arrivait dans l'admin.

Context:
- La TV peut garder un ancien compte Xtream local et un ancien catalogue Room.
- Le compte configure cote portail/backend peut avoir change apres la derniere synchronisation.

Cause probable:
- Le splash testait `XtreamAccountManager.current()` avant de relire `device_status.php`; il pouvait donc tester l'ancien compte local au lieu de la playlist backend la plus recente.
- `AppNavigation` ne lancait pas toujours `verifyQuick("startup")` si la derniere sync etait recente.
- Les routes player/detail pouvaient etre atteintes depuis cache local si l'etat Xtream etait encore `UNKNOWN`.
- Le lecteur traitait un flux bloque en buffering comme un probleme player relancable, pas comme une indisponibilite Xtream globale.

Working solution:
- Dans `SplashActivity`, appeler `activationRepository.checkStatus()` avant `xtreamConnectionManager.verifyQuick("splash")`.
- Dans `AppNavigation`, verifier Xtream au premier affichage actif meme sans synchro due, mais ne synchroniser que si la politique l'exige ou si le compte change.
- Traiter `xtreamConnectionState.checking` comme bloquant pour les catalogues.
- Bloquer les routes profondes `player/*`, `movie_player/*`, `movie_detail/*`, `episode_player/*`, `series_detail/*` si Xtream est indisponible.
- Dans `FullScreenPlayerScreen`, apres buffering persistant, appeler `xtreamConnectionManager.markPlaybackUnavailable(...)`, afficher `Connexion Xtream indisponible` et envoyer une anomalie `XTREAM_FAILED`.

Avoid next time:
- Ne jamais supposer qu'une sync recente dispense du test de connexion Xtream.
- Ne pas valider uniquement Home/Header; tester aussi l'ouverture directe d'un media depuis un cache local ancien.
- Ne pas laisser un spinner player sans limite quand le flux media ne demarre jamais.

## 2026-06-30 - Deploy admin test failed after Diagnostics navigation refactor

Problem:
- `scripts/deploy_activation_phase1.ps1 -SkipInstall` uploaded server files and APK, then stopped during `Tests administration...` with `Connexion admin echouee.`

Context:
- Admin Diagnostics had been refactored to centralize `Synthese`, `AutoSync`, `Anomalies App`, `Info Serveur` and `Journal`.
- The deploy test still expected a separate `Journal` marker on the dashboard after login.

Cause probable:
- The admin login was valid, but the test marker was stale after the sidebar/navigation change.

Working solution:
- Update `Test-AdminPanel` in `scripts/deploy_activation_phase1.ps1` to check for `Diagnostics` instead of the old separate `Journal` marker.
- Re-run server deploy/tests with `.\scripts\deploy_activation_phase1.ps1 -SkipInstall -SkipApkUpload` if the APK and manifest were already published, to avoid creating a duplicate release notification.

Avoid next time:
- When admin navigation changes, update deploy smoke-test markers in the same change.
- After a partial deploy stop, check `downloads/smartvision-tv.version.json` before re-running APK upload.

## 2026-07-03 - Release build JBR incomplet et Kotlin daemon AccessDenied

Problem:
- `assembleRelease` peut echouer avant ou pendant Kotlin/KAPT avec:
  `could not open C:\Program Files\Android\Android Studio\jbr\lib\jvm.cfg`
  ou `AccessDeniedException: C:\Users\ONEDEV\AppData\Local\kotlin\daemon\...tmp`.

Context:
- Le JBR Android Studio local peut etre incomplet: `jbr\lib` ne contient que `modules`.
- Un JDK 21 utilisable existe dans `C:\Users\ONEDEV\.codex\cache\jdk21\extracted\jdk-21.0.11+10`.
- KAPT/Kotlin peut avoir besoin d'ecrire dans `AppData\Local\kotlin\daemon`, ce qui peut echouer dans le sandbox.

Working solution:
- Utiliser le JDK 21 cache:
  `$env:JAVA_HOME='C:\Users\ONEDEV\.codex\cache\jdk21\extracted\jdk-21.0.11+10'`
  `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- Lancer la release:
  `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon '-Dkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.workers.max=1'`
- Si `AccessDeniedException` apparait dans `AppData\Local\kotlin\daemon`, relancer la meme commande hors sandbox avec validation utilisateur.

Avoid next time:
- Ne pas insister avec `C:\Program Files\Android\Android Studio\jbr` si `jvm.cfg` manque.
- Ne pas considerer l'erreur Kotlin daemon comme une erreur applicative sans verifier les permissions `AppData`.

## 2026-07-13 - HTTP 500 create_playlist_setup_session

- Probleme: le test secondaire du deploiement echouait apres une publication APK reussie.
- Cause: reinsertion du meme hash dans `activation_session_tokens.token_hash`, colonne UNIQUE.
- Solution: conserver le token valide existant et creer seulement la nouvelle session/short code Playlist.
- A eviter: dupliquer un token appareil pour chaque short code; le token authentifie l'appareil, le short code autorise l'operation web.
- Fichiers/commande: `server/public_html/api/create_playlist_setup_session.php`, `php -l server/public_html/api/create_playlist_setup_session.php`.
# 2026-07-17 - APK release verrouille par un `adb install` bloque

- Probleme: `:app:packageRelease` echoue avec `Unable to delete ... app-release.apk`.
- Contexte: une commande `adb install -r app-release.apk` a depasse son timeout mais son processus Windows est reste actif et gardait l'APK ouvert.
- Cause: le timeout du shell n'arrete pas toujours le processus enfant `adb.exe`.
- Solution: identifier uniquement le processus `adb.exe` dont la ligne de commande contient `install -r`, l'arreter, relancer `assembleRelease`, puis preferer `adb push` vers `/data/local/tmp` suivi de `shell pm install -r`.
- A eviter: tuer le serveur ADB complet ou supprimer le dossier build avant d'avoir verifie le processus qui tient le fichier.
- Commandes concernees: `Get-CimInstance Win32_Process -Filter "Name = 'adb.exe'"`, `Stop-Process -Id <pid> -Force`, `adb push`, `adb shell pm install -r`.
