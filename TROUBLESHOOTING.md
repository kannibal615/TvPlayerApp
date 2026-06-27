# Troubleshooting

## 2026-06-26 - Build Gradle release bloque avec Kotlin daemon

Probleme rencontre :
`gradlew :app:assembleRelease` peut depasser plusieurs minutes et laisser un arbre Java actif, souvent autour du Kotlin compile daemon, sans rendre la main a la commande.

Contexte :
Build release Android SVPlayer sous Windows/PowerShell apres modifications Compose/Kotlin.

Solution qui fonctionne :
- arreter le build suspendu uniquement s'il ne rend pas la main ;
- relancer les commandes Gradle avec `--%` pour proteger les arguments `-D...` de PowerShell ;
- utiliser `-Dkotlin.compiler.execution.strategy=in-process` pour eviter le daemon Kotlin si besoin.

Commandes utiles :
```powershell
Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java|java.exe' } | Format-List ProcessId,ParentProcessId,CommandLine
.\gradlew.bat --% :app:compileReleaseKotlin --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process
.\gradlew.bat --% :app:assembleRelease --no-daemon --console=plain -Dkotlin.compiler.execution.strategy=in-process
```

Erreurs a eviter :
- ne pas relancer plusieurs builds Gradle concurrents ;
- ne pas passer `-Dkotlin.compiler.execution.strategy=in-process` sans `--%` dans PowerShell, sinon Gradle peut recevoir une fausse tache `.compiler.execution.strategy=in-process`.

## 2026-06-27 - Admin 500 apres ajout d un nouveau service PHP backend

Probleme rencontre :
Apres deploy prod, `/admin/` retournait `500 Internal Server Error` pendant les tests admin.

Contexte :
Ajout d un nouveau service PHP requis par `server/public_html/admin/index.php`. Le fichier local passait `php -l`, mais le script `scripts/deploy_activation_phase1.ps1` n uploadait pas encore le nouveau fichier requis.

Solution qui fonctionne :
- verifier les nouveaux `require_once` ajoutes dans `admin/index.php` ou les endpoints API ;
- ajouter chaque nouveau fichier PHP au bloc `Upload des fichiers PHP/SQL` dans `deploy_activation_phase1.ps1` ;
- relancer le deploy complet puis valider `/admin/` publiquement.

Erreurs a eviter :
- ne pas supposer qu un fichier PHP nouveau est deploye automatiquement ;
- ne pas cloturer un deploy si le test admin echoue, meme si les pages publiques et l APK sont deja uploades.

## 2026-06-27 - Validation curl anomaly-events retourne JSON invalide

Probleme rencontre :
`POST /api/app/anomaly-events` peut repondre `400 JSON invalide` pendant une validation manuelle PowerShell alors que l endpoint fonctionne.

Contexte :
Test curl depuis PowerShell avec un fichier JSON cree par `Set-Content -Encoding UTF8`. Windows PowerShell ajoute un BOM UTF-8, et `read_json_input()` le refuse.

Solution qui fonctionne :
- creer le fichier de test sans BOM, par exemple avec `[System.IO.File]::WriteAllText($path, $json, [System.Text.Encoding]::ASCII)` ;
- ou envoyer le JSON depuis un client qui n ajoute pas de BOM, comme Retrofit Android.

Erreurs a eviter :
- ne pas diagnostiquer l endpoint comme casse avant de verifier l encodage exact du corps POST ;
- ne pas utiliser `Set-Content -Encoding UTF8` de Windows PowerShell pour ce test JSON.

## 2026-06-27 - Lecteur YouTube WebView bloque par la page complete

Probleme rencontre :
Le lecteur YouTube integre affichait une bande grise "Lire la suite" au milieu de l ecran et la telecommande ne controlait pas la video.

Contexte :
L ecran player SmartVision chargeait `youtube.com/watch` dans une WebView pour rester dans l application. Cette URL affiche la page YouTube complete, avec overlays, recommandations et elements non adaptes au focus Android TV.

Solution qui fonctionne :
- ne pas charger `youtube.com/watch` dans la WebView ;
- charger une page HTML locale avec l IFrame Player API officielle via `loadDataWithBaseURL(...)` ;
- piloter la lecture par `evaluateJavascript` depuis les touches TV : OK play/pause, gauche/droite seek ;
- afficher une erreur SmartVision propre pour les codes YouTube 101/150/152.

Erreurs a eviter :
- ne pas auto-cliquer ou masquer une bannière/condition YouTube ;
- ne pas rouvrir l app YouTube si le besoin est de rester dans SmartVision ;
- ne pas remettre un fallback vers `youtube.com/watch`, car il ramene les overlays et le blocage de focus.

## 2026-06-27 - Lecteur YouTube doit rester dans la section miniatures

Probleme rencontre :
La selection d une video YouTube ouvrait une route/ecran `youtube_player/{videoId}`, ce qui cassait la continuite de l ecran YouTube et rendait le retour moins naturel.

Contexte :
L ecran YouTube Compose contient deja le header de section avec la recherche et la grille de miniatures. Le besoin TV est de remplacer uniquement la zone miniatures par le lecteur, puis de revenir a la grille avec Back.

Solution qui fonctionne :
- garder l etat de lecture dans `YoutubeScreen` au lieu de naviguer vers une route player separee ;
- afficher `YoutubeWebPlayer` dans le panel des miniatures, sous le meme header de recherche ;
- intercepter Back avec `BackHandler(enabled = playingVideo != null)` pour fermer le lecteur et restaurer le focus sur la grille ;
- masquer les controles iframe YouTube (`controls=0`, `disablekb=1`) et piloter OK/gauche/droite depuis le listener Android TV ;
- stopper/detruire le player via `smartVisionStop` avant de detruire le WebView.

Erreurs a eviter :
- ne pas refaire une navigation `navController.navigate("youtube_player/$videoId")` depuis la grille ;
- ne pas laisser le chargement infini de la grille actif pendant que le lecteur remplace les miniatures ;
- ne pas remettre les controles iframe natifs, car ils recuperent mal le focus D-pad sur Android TV.

## 2026-06-27 - Crash sortie grand player Live apres Back

Probleme rencontre :
`PROCESS_EXIT_CRASH` remontait au demarrage suivant apres sortie du grand player Live avec Back. Les reproductions fiables arrivaient surtout apres plus de 5 secondes, quand la chaine ou le film appartenait a une categorie pas encore presente dans l historique et le tri des dossiers.

Contexte :
Les logs anomalies pointaient vers `contentType=live`, avec des positions superieures a 5 secondes et les dernieres etapes `before_onBack`, `release_begin` ou `release_done`, sans stack Java/Kotlin. Le signal `PLAYER_PROGRESS_SAVE_DONE` ne prouvait pas que Room avait fini, car `saveProgress()` lancait un `viewModelScope.launch`. Le crash etait donc probablement une course entre sauvegarde historique Live asynchrone, refresh des categories/historique et release Media3/Surface.

Solution qui fonctionne :
- garder l historique Live actif apres 5 secondes ;
- ne pas lancer `saveProgress()` en fire-and-forget avec `viewModelScope.launch` pendant la sortie ;
- serialiser les sauvegardes de progression avec un `Mutex` ;
- attendre la fin reelle de la sauvegarde Room avant `onBack()` pour eviter la course avec le refresh historique/categories et le release player ;
- tracer `PLAYER_PROGRESS_SAVE_DONE`, `PLAYER_PROGRESS_SAVE_FAILED` ou `PLAYER_PROGRESS_SAVE_SKIPPED_SHORT_LIVE` avec `streamId`, `categoryId`, position et duree ;
- sauvegarder le progres Live/Films/Episodes avant `onBack`, pas apres navigation ;
- supprimer la sauvegarde doublon dans `onDispose` ;
- detacher `PlayerView`, puis differer `player.release()` et proteger stop/release contre les doubles appels ;
- compter `PROCESS_EXIT_CRASH`, `PROCESS_EXIT_CRASH_NATIVE` et `PROCESS_EXIT_ANR` dans le KPI admin.

Erreurs a eviter :
- ne pas supprimer l historique Live pour masquer le crash ;
- ne pas marquer `PLAYER_PROGRESS_SAVE_DONE` avant que Room ait vraiment termine ;
- ne pas appeler `onProgressSnapshot()` apres `onBack()` ;
- ne pas conclure qu il n y a pas crash si le compteur admin ignore `PROCESS_EXIT_*`.
