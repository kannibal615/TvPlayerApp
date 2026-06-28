# Terminal commands for TvPlayerApp / SVPlayer

Ce fichier regroupe les commandes PowerShell les plus utiles pour travailler dans ce projet.

Base de travail:

```powershell
Set-Location "C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer"
```

## Android

Build debug:

```powershell
.\gradlew.bat assembleDebug
```

Build release:

```powershell
.\gradlew.bat assembleRelease
```

Build debug puis installation sur l'emulateur ou l'appareil connecté:

```powershell
.\gradlew.bat assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r -d .\app\build\outputs\apk\debug\app-debug.apk
```

Attendre le boot de l'emulateur Android TV:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed
```

Lancer l'emulateur recommande pour les tests TV:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd SmartVision_TV_720p_Light -no-audio -no-boot-anim -gpu auto -memory 1536
```

Lister les APK generes:

```powershell
Get-ChildItem -Recurse .\app\build\outputs\apk -Filter *.apk
```

## Serveur local

Lancer le serveur web local PHP:

```powershell
.\scripts\start_web_dev.ps1
```

Lancer le serveur web local PHP sans ouvrir le navigateur:

```powershell
.\scripts\start_web_dev.ps1 -NoBrowser
```

Lancer un autre port:

```powershell
.\scripts\start_web_dev.ps1 -Port 8081
```

## Déploiement

Déploiement complet en prod:

```powershell
cd SVPlayer
.\scripts\deploy_activation_phase1.ps1
```

Déploiement serveur uniquement:

```powershell
cd SVPlayer
.\scripts\deploy_activation_phase1.ps1 -SkipInstall -SkipApkUpload
```

Déploiement APK seulement:

```powershell
.\scripts\deploy_activation_phase1.ps1 -SkipInstall
```

Déploiement avec seulement les tests actifs, sans installer ni uploader l'APK:

```powershell
.\scripts\deploy_activation_phase1.ps1 -SkipInstall -SkipApkUpload -SkipTests
```

Déploiement orienté QA commerce uniquement:

```powershell
.\scripts\deploy_activation_phase1.ps1 -CommerceTestOnly
```

Déploiement avec device QA:

```powershell
.\scripts\deploy_activation_phase1.ps1 -QaDeviceId "DEVICE_ID"
```

Déploiement avec device QA et code court:

```powershell
.\scripts\deploy_activation_phase1.ps1 -QaDeviceId "DEVICE_ID" -QaShortCode "1234"
```

Déploiement avec maintien actif QA:

```powershell
.\scripts\deploy_activation_phase1.ps1 -QaDeviceId "DEVICE_ID" -QaShortCode "1234" -QaActiveHoldSeconds 60
```

## Utilitaires

Voir la configuration locale:

```powershell
Get-Content .\local.properties
```

Comparer les versions APK et release:

```powershell
Get-Item .\app\build\outputs\apk\release\app-release.apk | Format-List *
```

Calculer le hash SHA-256 d'un APK release:

```powershell
Get-FileHash .\app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

Consulter les logs de build les plus recents:

```powershell
Get-Content .\build-qa.log -Tail 200
Get-Content .\build-qa.err.log -Tail 200
```

## Notes

- Les commandes de déploiement lisent `local.properties`.
- Pour une release Android, il faut incrémenter `versionCode` avant `assembleRelease`.
- L'APK release attendue par le script est `app/build/outputs/apk/release/app-release.apk`.
- Le déploiement public génère aussi `downloads/smartvision-tv.version.json`.
