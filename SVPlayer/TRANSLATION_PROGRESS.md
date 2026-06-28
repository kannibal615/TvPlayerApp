# Translation progress

Date: 2026-06-28

Goal:
- Main application language: English.
- Language available from settings: French.
- Keep the translation work incremental and documented after each block.

## Block 1 - Settings foundation

Status: done locally.

Completed:
- Added a lightweight app string layer in `ui/i18n/SmartVisionStrings.kt`.
- Added English as the default text set and French as the secondary text set.
- Switched `PlayerSettings.language` default from `Francais` to `English`.
- Connected the active Settings screen to the selected app language.
- Kept internal saved values stable for settings such as sync frequency, animation mode and retry mode.
- Restricted the visible language choices to `English` and `Francais`.
- Removed active Spanish/Arabic language choices from the Settings screen path.
- Localized the Settings menu sections, update labels, data labels and parental-control labels.

Remaining after block 1:
- Externalize and translate Activation screens.
- Externalize and translate Home, header tabs and collections.
- Externalize and translate Live TV, Movies, Series, details and player overlays.
- Externalize and translate Profile, notifications, update dialogs and premium popups.
- Externalize and translate YouTube screens and locked-feature states.
- Decide whether older persisted French values such as `A chaque demarrage`, `Manuelle` and `Jamais` should be migrated to stable enum-like keys.
- Add a simple i18n audit command or checklist before each release.
- Run a release build after the next translation block to validate Kotlin/Compose changes.
