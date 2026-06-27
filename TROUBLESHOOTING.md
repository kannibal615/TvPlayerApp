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
