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

## Block 2 - Header and access-control texts

Status: done locally.

Completed:
- Externalized and translated the main header tabs: Home, Live TV, Movies, Series and YouTube.
- Connected header tab labels to the active app language.
- Added English/French strings for locked premium features.
- Added English/French strings for parental-control PIN creation, unlock, change PIN, errors and apply action.
- Reused the i18n layer for the new parental-control dialogs and locked-feature messages.
- Kept the default language as English and the available Settings languages as English/French only.
- Validated the block with the release build that produced `0.1.39 (42)`.

Remaining after block 2:
- Externalize and translate Activation screens.
- Externalize and translate Home body sections, collections and empty/loading states.
- Externalize and translate Live TV, Movies, Series, details and player overlays.
- Externalize and translate Profile, notifications, update dialogs and premium purchase popups.
- Externalize and translate the full YouTube screen: search, categories, suggestions, player messages and errors.
- Replace remaining user-visible hard-coded French text in Android Compose screens.
- Add a repeatable i18n audit command before each release.
- Add backend email flow for forgotten parental PIN once the account email source is confirmed in the Android activation/session model.

## Block 3 - Personalization and notifications

Status: done locally.

Completed:
- Added English/French strings for Settings > Personalization.
- Added English/French labels for the three focus styles: Default, Soft and Compact.
- Added English/French strings for the Notifications screen header, refresh states, empty state, load error and update notification template.
- Kept the default language as English and the visible Settings languages as English/French only.
- Validated the block with the release build that produced `0.1.40 (43)`.

Remaining after block 3:
- Externalize and translate Activation screens.
- Externalize and translate Home body sections, collections and empty/loading states.
- Externalize and translate Live TV, Movies, Series, details and player overlays.
- Externalize and translate Profile, update dialogs and premium purchase popups.
- Externalize and translate the full YouTube screen: search, categories, suggestions, player messages and errors.
- Replace remaining user-visible hard-coded French text in Android Compose screens.
- Add a repeatable i18n audit command before each release.
- Add backend email flow for forgotten parental PIN once the account email source is confirmed in the Android activation/session model.
