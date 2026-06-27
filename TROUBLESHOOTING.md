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

## 2026-06-27 - Crash sortie grand player Live apres Back

Probleme rencontre :
`PROCESS_EXIT_CRASH` remontait au demarrage suivant apres sortie du grand player Live avec Back, souvent apres moins de 5 secondes de lecture.

Contexte :
Les logs anomalies pointaient vers `after_save_progress_deferred` sur `contentType=live`, sans stack Java/Kotlin. Le crash etait donc probablement une course entre navigation retour, sauvegarde historique Live courte et release Media3/Surface.

Solution qui fonctionne :
- ne pas enregistrer l historique Live si `positionMs < 5000` ;
- sauvegarder le progres avant `onBack`, pas apres navigation ;
- supprimer la sauvegarde doublon dans `onDispose` ;
- detacher `PlayerView`, puis differer `player.release()` et proteger stop/release contre les doubles appels ;
- compter `PROCESS_EXIT_CRASH`, `PROCESS_EXIT_CRASH_NATIVE` et `PROCESS_EXIT_ANR` dans le KPI admin.

Erreurs a eviter :
- ne pas forcer l historique Live a `5001 ms`, car cela cree des entrees apres une lecture trop courte ;
- ne pas appeler `onProgressSnapshot()` apres `onBack()` ;
- ne pas conclure qu il n y a pas crash si le compteur admin ignore `PROCESS_EXIT_*`.
