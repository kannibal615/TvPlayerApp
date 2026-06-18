# SVPlayer Project Notes

## Project

- App name shown to users: `SmartVision`
- Gradle root project: `SVPlayer`
- Android application id: `com.smartvision.svplayer`
- Main activity: `com.smartvision.svplayer/.MainActivity`
- Minimum SDK: `23`
- Compile/target SDK: `36`
- UI stack: native Kotlin + Jetpack Compose for TV

## Local SDK And Emulator

This machine has two Android SDK locations. For this project, use the StreamVault SDK.

- Preferred SDK: `C:\Users\ONEDEV\.streamvault-tools\android-sdk`
- Incomplete default SDK image: `C:\Users\ONEDEV\AppData\Local\Android\Sdk\system-images\android-36\android-tv\x86_64`
- Recommended lightweight AVD: `SVPlayer_TV_Lite_API30`
- Lightweight AVD config: `C:\Users\ONEDEV\.android\avd\SVPlayer_TV_Lite_API30.avd\config.ini`
- Lightweight Android TV system image: `system-images\android-30\android-tv\x86`
- Lightweight ABI: `x86`
- Legacy/heavier AVD: `SmartVision_TV_API36`
- Legacy Android TV system image: `system-images\android-36\android-tv\x86_64`

Recommended lightweight AVD-specific settings:

```properties
hw.device.name=tv_1080p
hw.lcd.width=1920
hw.lcd.height=1080
hw.lcd.density=320
hw.ramSize=2048
hw.cpu.ncore=2
disk.dataPartition.size=6G
hw.audioInput=no
hw.audioOutput=no
hw.camera.back=none
hw.camera.front=none
hw.gps=no
hw.gsmModem=no
showDeviceFrame=no
```

The old API 36 AVD was kept for reference. A backup of its old 2 GB config was created at:

```text
C:\Users\ONEDEV\.android\avd\SmartVision_TV_API36.avd\config.ini.bak-before-4gb
```

## Environment Setup

Use these variables in PowerShell before emulator or Gradle commands:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_SDK_ROOT='C:\Users\ONEDEV\.streamvault-tools\android-sdk'
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
$env:ANDROID_AVD_HOME='C:\Users\ONEDEV\.android\avd'
```

The global Java on this PC may be Java 8. Gradle for this project needs the Android Studio JBR.

## Build

From the project root:

```powershell
cd 'C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer'
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_SDK_ROOT='C:\Users\ONEDEV\.streamvault-tools\android-sdk'
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
.\gradlew.bat :app:assembleDebug --offline --console=plain --max-workers=2
```

Debug APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Start Emulator

Recommended visible lightweight emulator:

```powershell
$env:ANDROID_SDK_ROOT='C:\Users\ONEDEV\.streamvault-tools\android-sdk'
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
$env:ANDROID_AVD_HOME='C:\Users\ONEDEV\.android\avd'

& "$env:ANDROID_SDK_ROOT\emulator\emulator.exe" `
  -avd SVPlayer_TV_Lite_API30 `
  -no-snapshot-load `
  -gpu swiftshader_indirect `
  -no-boot-anim `
  -no-audio `
  -no-metrics `
  -camera-back none `
  -camera-front none `
  -cores 2 `
  -memory 2048 `
  -netfast
```

Notes:

- Prefer `SVPlayer_TV_Lite_API30` for daily work. It uses the Android TV x86 API 30 image and boots as `AOSP_TV_on_x86`.
- The API 30 image has fewer Android 16/Google TV background services and avoids the `No response to onStartJob` / GPU-heavy ANR behavior seen on the API 36 AVD.
- `-gpu host` starts faster on some machines, but on this PC it was unstable with the AMD R6 driver and the emulator exited after boot.
- `-gpu swiftshader_indirect` is slower but more stable on this PC.
- First boot after `-wipe-data` can take several minutes.
- Do not install the APK until Android framework services are available.

## Wait For Boot

```powershell
$adb='C:\Users\ONEDEV\.streamvault-tools\android-sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb -s emulator-5554 shell getprop sys.boot_completed
& $adb -s emulator-5554 shell service check package
& $adb -s emulator-5554 shell service check settings
& $adb -s emulator-5554 shell service check activity
```

Proceed only when:

```text
sys.boot_completed = 1
Service package: found
Service settings: found
Service activity: found
```

## Disable Android Animations

```powershell
$adb='C:\Users\ONEDEV\.streamvault-tools\android-sdk\platform-tools\adb.exe'
& $adb -s emulator-5554 shell settings put global window_animation_scale 0
& $adb -s emulator-5554 shell settings put global transition_animation_scale 0
& $adb -s emulator-5554 shell settings put global animator_duration_scale 0
& $adb -s emulator-5554 shell settings put system screen_off_timeout 2147483647
```

## Install And Launch

```powershell
$adb='C:\Users\ONEDEV\.streamvault-tools\android-sdk\platform-tools\adb.exe'
$apk='C:\Users\ONEDEV\Desktop\IPTV APP NATIVE ANDROID\TvPlayerApp\SVPlayer\app\build\outputs\apk\debug\app-debug.apk'

& $adb -s emulator-5554 install -r $apk
& $adb -s emulator-5554 shell am start -n com.smartvision.svplayer/.MainActivity
```

Verify:

```powershell
& $adb -s emulator-5554 shell pm list packages | Select-String 'com.smartvision.svplayer'
& $adb -s emulator-5554 shell pidof -s com.smartvision.svplayer
& $adb -s emulator-5554 shell dumpsys activity activities | Select-String -Pattern 'topResumedActivity|ResumedActivity|com.smartvision.svplayer'
```

## Current Verified Emulator State

On 2026-06-18:

- Installed lightweight image `system-images;android-30;android-tv;x86`.
- Created AVD `SVPlayer_TV_Lite_API30`.
- Configured the lightweight AVD for 1080p, 2 GB RAM, 2 CPU cores, no audio, no cameras, no GPS/modem, no device frame.
- Emulator started in visible mode with `-gpu swiftshader_indirect`.
- Android animations disabled after boot.
- `app-debug.apk` installed successfully.
- `com.smartvision.svplayer/.MainActivity` launched and stayed focused in 1920x1080.
- No `ANR in com.smartvision.svplayer`, `No response to onStartJob`, `FATAL EXCEPTION`, or `AndroidRuntime` crash was found in the filtered logcat after relaunch.
- First/second launches can still take around 28-38 seconds on this PC because Compose classes are being verified/compiled, but the app remains alive.
- Avoid `adb shell cmd package compile -m speed -f com.smartvision.svplayer` on this PC for now; full precompilation was too slow for interactive work. Use the lightweight AVD first, then optimize app startup/rendering in code.

## Project Structure

```text
app/src/main/java/com/smartvision/svplayer
  core/config              Xtream credentials provider
  core/data                AppContainer and local DI
  core/designsystem        TV scaffold, side nav, focusable controls, theme
  core/navigation          Route definitions
  core/ui                  ViewModel factory
  data/local               Room database, DAOs, entities
  data/remote              Xtream API, DTOs, stream URL factory
  data/repository          Repository implementations, mappers, mock fallback data
  domain/model             Domain models
  domain/repository        Repository contracts
  domain/usecase           Use cases
  feature/home             Home screen
  feature/live             Live TV screen and ViewModel
  feature/movies           Movies screen and ViewModel
  feature/series           Series screen and ViewModel
  feature/player           Media3 player screen and ViewModel
  feature/account          Account/profile screen
  feature/settings         Settings screen and ViewModel
  sync                     WorkManager sync worker, delayed at startup
```

## Important Implementation Notes

- `local.properties` contains Xtream test credentials and must stay ignored by git.
- `local.properties.example` documents the expected keys without secrets.
- Retrofit uses the Xtream Player API.
- Room stores profiles, categories, live streams, movies, series, episodes, favorites, progress, and sync state.
- The repository falls back to `MockCatalogData` when the local database is empty.
- Media playback uses Media3 ExoPlayer.
- Compose for TV focus behavior is centralized in `core/designsystem`.
- The manifest uses `LEANBACK_LAUNCHER`, requires `android.software.leanback`, and does not require touch.
- Automatic catalog sync is intentionally delayed by 30 minutes after app startup. Manual sync remains available from the header. This prevents WorkManager from starting a heavy Xtream sync during the first Compose render, which can trigger `No response to onStartJob` ANRs on a slow Android TV emulator.

## Troubleshooting

If `adb devices` shows `offline` for a long time:

```powershell
$adb='C:\Users\ONEDEV\.streamvault-tools\android-sdk\platform-tools\adb.exe'
& $adb kill-server
& $adb start-server
& $adb devices -l
```

If `sys.boot_completed=1` but install fails with `Can't find service: package`, wait for the framework services:

```powershell
& $adb -s emulator-5554 shell service check package
& $adb -s emulator-5554 shell service check settings
& $adb -s emulator-5554 shell service check activity
```

If the emulator exits after boot with `-gpu host`, restart with:

```powershell
-gpu swiftshader_indirect
```

If the AVD state becomes corrupted, wipe data once:

```powershell
& "$env:ANDROID_SDK_ROOT\emulator\emulator.exe" -avd SmartVision_TV_API36 -wipe-data -no-snapshot-load -gpu swiftshader_indirect
```

Then reinstall SVPlayer.
