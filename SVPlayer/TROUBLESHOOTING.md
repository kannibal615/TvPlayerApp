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
