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

## In progress
- None.

## Remaining
- Manual TV validation: focus style selection, consent D-pad scroll, YouTube player left/right focus, parental PIN popups, splash sound.
