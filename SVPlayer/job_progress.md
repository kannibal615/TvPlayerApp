# Job progress - 2026-06-28

Goal:
- Finish the requested Android TV/admin/deploy block, then build and deploy a new production release.

## Done
- Started the work tracker.
- Read `TRANSLATION_PROGRESS.md` and `TROUBLESHOOTING.md`.
- Added persistent reusable focus styles: `Default`, `Soft`, `Compact`.
- Added Settings > Personalization to choose the global focus style.
- Connected the focus style to core reusable focus modifiers, buttons and notification rows.
- Improved locked YouTube header state in Home and catalog headers: stronger transparency and crown after the YouTube label.
- Improved parental-control locked menu transparency/crown placement and visible text-field focus.
- Added parental keyword filtering to Live/Movie/Series category folders.
- Adjusted parental enable/disable button colors according to active/inactive state.
- Adjusted consent popup size and added D-pad scrolling inside the text area.
- Replaced the synthetic splash sound with the provided `startup_chime.mp3`.
- Kept YouTube suggestions visible during additional loading and added left navigation from player to suggestions.
- Added periodic update checks while the app is activated.
- Added deployment-script release notification insertion into standard `app_notifications`.
- Improved admin Consent TV organization.
- Continued translation block with personalization and notifications strings.
- Built release `0.1.40 (43)` successfully.
- Deployed release `0.1.40 (43)` to production.
- Verified prod update manifest, versioned APK hash, stable APK hash and release notification.
- Corrected stored production feature flags so YouTube and parental control are locked for `free_ads`.
- Started the 2026-06-28 correction block.
- Extended focus personalization to combine style, color and effect.
- Added focus colors: White, Cyan neon, Electric blue.
- Added focus effects: Frame, Neon glow and animated gold reflection.
- Connected the global focus style to reusable focus modifiers and TV buttons.
- Added server-side TV consent receipts in `app_consent_receipts`.
- Android now sends consent acceptance to the backend and uses the server accepted version when available.
- Added consent popup scrollbar and adjusted popup dimensions.
- Added watchdog anomaly types for blocked buffering detection and automatic buffering relaunch.
- Suppressed noisy player exit/progress/release anomaly types in Android and backend filtering.
- Prevented SQL seed deployment from overwriting existing admin-managed `app_settings`.
- Improved update notifications: download icon, update click opens update dialog, installed update notifications hidden in app.
- Applied locked YouTube state to the Live TV header.
- Moved startup sound from Splash to first Home display.
- Stabilized YouTube suggestion incremental loading and added DPAD up exit from player to search.
- Continued translation block for Home/update/premium visible strings.
- Incremented local release to `0.1.41 (44)`.
- Built release `0.1.41 (44)` successfully.
- PHP lint passed for `app_config.php`, `anomaly_service.php` and `admin/index.php`.
- Deployed release `0.1.41 (44)` to production.
- Verified prod update manifest: versionCode `44`, versionName `0.1.41`, APK `smartvision-tv-v44-ad821399.apk`.
- Verified versioned and stable APK SHA256: `ad821399d5e089fd93fd76367ad018df6322e5ca8d350a5251a88dbceb4a162f`.
- Verified release notification id `6` in the standard notifications API.
- Corrected production feature flags so YouTube and parental control are locked for `free_ads`.

## In progress
- None.

## Remaining
- Manual TV validation: focus style selection, consent D-pad scroll, YouTube player left/right focus, parental PIN popups, splash sound.
- Manual TV validation: new focus color/effect combinations, update notification click, consent server reacceptance after version change, buffering watchdog anomaly logs.
