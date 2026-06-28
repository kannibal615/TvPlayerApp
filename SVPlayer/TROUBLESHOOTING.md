# Troubleshooting

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
