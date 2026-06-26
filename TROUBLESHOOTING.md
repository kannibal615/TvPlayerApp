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
