# SmartVision scenario inventory

Adapt this inventory to the routes and features present in the checked-out source. Do not mark an absent or inaccessible screen as covered.

## Startup and profiles

- Cold launch, warm launch, loading transition, and failure recovery.
- Profile picker initial focus, profile selection, add classic profile, and add kids profile.
- Admin, standard, and kids profile differences.
- Back behavior and focus restoration after returning to profile selection.

## Home

- Header and side-menu entry/exit in every direction.
- Hero loading and locally cached image behavior.
- Continue Watching, trending movies, and trending series.
- Stable poster images before and after focus enters a section.
- Horizontal scrolling, five-card visibility target where applicable, and focus retention.
- Mini-player background, playback state, and navigation away/back.
- Synchronization progress, completion, failure, and duplicate-launch prevention.

## Live TV, Movies, and Series

- Correct initial folder/category selection and matching content.
- Filter application on the first OK press.
- Selected filter placement and focus destination after application.
- Folder list, item list, sorting controls, poster ratios, row heights, and text widths.
- Consistent dimensions and spacing across all three screens.
- Empty category, long labels, missing artwork, loading, and error states.
- Player launch, overlay controls, Back behavior, and return focus.

## Settings and profile information

- Side-menu to header navigation, including DPAD_UP from the first item.
- General settings, toggles, dialogs, persistence, disabled states, and descriptions.
- Account/profile information, license state, and status indicators.
- Synchronization screen section focus, inner-control focus, scrolling, progress, and per-profile catalogue status.
- Ensure every visible section and inner control is reachable with the remote.

## Parental control

- Entry from account information and enabled/disabled status indicator.
- Section-level focus before inner-control focus.
- Text field focus before keyboard display.
- Numeric PIN keyboard proportions, centered digits, PIN dots, Apply focus after the fourth digit, incorrect-PIN shake, and Back behavior.
- Filtering keywords, four-keyword row behavior where applicable, folders and actual hidden content.
- Long result lists, scrolling, loading/progress, empty state, and persistence.
- Use a session-only test PIN; never write it to artifacts or reports.

## Cross-cutting states

- Network loss and recovery.
- API/server error and retry.
- Missing or slow images.
- Very long localized strings.
- Rapid repeated DPAD input.
- App background/foreground and process restart.
- Fire TV sleep/wake when relevant.
- Crash, ANR, repeated exceptions, memory growth, and frame jank.

## Suggested coverage table

For every screen, track initial state, focused state, selected state, loaded state, error/empty state, DPAD directions, OK, Back, scroll, screenshot evidence, logs, and performance evidence. Use `blocked` with the blocker when a prerequisite or defect prevents coverage.
