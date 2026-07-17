# SmartVision Home hero/header and visual consistency audit

The installed build matches the current source. The hero clipping is reproducible and is caused by a second implicit vertical relocation after requestFocus, not by the configured header/hero spacing. Two independent visual inconsistencies are also visible: mixed hero localization and edge-card focus halos clipped by the parent viewport.

## Contexte du test

| Champ | Valeur |
|---|---|
| Date | 2026-07-17 |
| Device | Amazon Fire TV Stick AFTSSS |
| Serial | FireStick-LAN |
| Android Version | Fire OS based on Android 9 / SDK 28 |
| Package | com.smartvision.svplayer |
| App Version | 0.1.120 (179) |
| Build Variant | installed release |
| Commit | 8930e52 |
| Run Count | 10 |

## Synthèse

| Gravité | Nombre |
|---|---:|
| Bloquant | 0 |
| Critique | 0 |
| Majeur | 1 |
| Mineur | 2 |
| Amélioration | 0 |

## Couverture

| Écran/parcours | État | Notes |
|---|---|---|
| Home initial and hero after five seconds | covered | Warm Home entry, initial category focus and header focus captured only after at least five seconds. |
| Home vertical D-pad flow | covered | Header, categories, Continue watching, Trending movies and Trending series tested in both directions with focus/scroll logs. |
| Home horizontal rows and edge focus | covered | Five-card target, focused scaling, rightward reveal and return path inspected. |
| Home loading, empty, error and synchronization overlays | not reached | Not required to reproduce the installed visual defect and not forced to avoid persistent account/catalog changes. |
| Home frame stability | partially covered | A ten-second gfxinfo sample was taken. Fire OS reported contradictory counters (100% janky but zero missed-vsync and zero slow-thread/draw counters), so no performance defect is asserted from that sample. |

## Backlog priorisé

| ID | Gravité | Catégorie | Écran | Problème |
|---|---|---|---|---|
| [TV-HOME-001](#tv-home-001-implicit-focus-relocation-clips-the-hero-and-upper-content-below-the-fixed-header) | majeur | navigation-focus | Home | Implicit focus relocation clips the hero and upper content below the fixed header |
| [TV-HOME-003](#tv-home-003-live-tv-and-series-focus-halos-are-clipped-at-the-horizontal-viewport-edges) | mineur | visuel | Home category cards | Live TV and Series focus halos are clipped at the horizontal viewport edges |
| [TV-HOME-002](#tv-home-002-hero-copy-is-french-while-the-surrounding-home-interface-is-english) | mineur | visuel | Home hero | Hero copy is French while the surrounding Home interface is English |

## Détails

### TV-HOME-001 — Implicit focus relocation clips the hero and upper content below the fixed header

- **Gravité :** majeur
- **Catégorie :** navigation-focus
- **Écran :** Home
- **Reproductibilité :** 5/5 vertical cycles; initial top clipping also reproduced after five-second captures
- **Confiance :** high

**Étapes de reproduction**

1. Open Home and wait five seconds.
2. Observe the hero while the first category has focus.
3. Press DPAD_DOWN to focus Continue watching and wait five seconds.
4. Continue to Trending movies/series, then return upward to categories and the header.

**Résultat actuel :** The hero loses its top 22 px after category focus. Continue watching focus moves the parent scroll from the explicit 288 px target to 436 px, hiding the hero and clipping the upper part of the category cards under the header. Trending focus repeats a 148 px post-focus drift. Returning to the header still leaves the hero top border and rounded corners clipped.

**Résultat attendu :** The explicit Home scroll position remains authoritative after focus is requested. At the top, the complete 132 dp hero, including its upper border and corners, starts below the header clearance. On lower rows, no text, badge, card or focus glow is clipped beneath the fixed header.

**Impact :** The principal Home visual is visibly damaged on every entry and the vertical hierarchy looks broken during normal D-pad navigation. Repeated spacing changes cannot stabilize the screen because they do not remove the second scroll owner.

**Preuves**

- `checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png`
- `checkpoints/20260717-034304-511847-home-continue-repro-after-5s/screenshot.png`
- `checkpoints/20260717-034347-098297-home-trending-movies-after-5s/screenshot.png`
- `checkpoints/20260717-034730-635187-home-header-after-full-return-5s/screenshot.png`
- `focus-scroll-evidence.txt`

**Zone de code suspectée**

- HomeScreen.kt requestVerticalFocus(): animateScrollTo followed by requestFocus
- HomeScreen.kt initial/header focus effects: post-focus scroll reset occurs before relocation has settled
- HomeScreen.kt verticalScroll parent: automatic focused-child relocation remains enabled

**Correction proposée :** Make the explicit Home D-pad scroll job the only owner of parent vertical positioning. Neutralize the parent scroll container's automatic focused-child relocation for these routed targets, then verify the settled scroll value after focus. Do not compensate again with arbitrary header/hero spacers or clipping-only changes.

**Critères d’acceptation**

- Five seconds after Home entry, the hero top edge, border and both upper rounded corners are fully visible below the header.
- After category focus settles, scrollState remains 0 rather than 22 px.
- After Continue watching focus settles, scrollState remains at the chosen target with no +148 px drift.
- No category badge, title, card edge or focus glow is clipped beneath the header in any vertical focus state.
- DPAD_UP and DPAD_DOWN still target the first item of each available row and header focus remains reachable.

**Notes :** The installed versionCode/versionName exactly match the checked-out source. Current source history contains several geometry/reset attempts, but device logs prove the remaining movement happens after requestFocus.

### TV-HOME-003 — Live TV and Series focus halos are clipped at the horizontal viewport edges

- **Gravité :** mineur
- **Catégorie :** visuel
- **Écran :** Home category cards
- **Reproductibilité :** 5/5 category focus checks
- **Confiance :** high

**Étapes de reproduction**

1. Open Home and wait five seconds.
2. Focus the Live TV category card and inspect its left halo.
3. Move focus to the Series category card and inspect its right halo.
4. Compare both with the centered Movies card.

**Résultat actuel :** The Live TV halo is cut flush at the left edge and the Series halo is cut flush at the right edge. The centered Movies halo has room to render normally.

**Résultat attendu :** The complete focus halo remains visible around all three category cards, including the two cards touching the horizontal content edges.

**Impact :** The focus treatment looks asymmetrical and unfinished on two of the three principal Home actions.

**Preuves**

- `checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png`
- `checkpoints/20260717-034109-591542-home-return-category-after-5s/screenshot.png`
- `checkpoints/20260717-035030-114923-home-settings-down-after-5s/screenshot.png`

**Zone de code suspectée**

- HomeScreen.kt scroll viewport: explicit clipToBounds clips both vertical and horizontal drawing
- HomeScreen.kt category Row: edge cards have no horizontal halo-safe inset
- HomeCategoryCard.kt shadow is correctly drawn before the card clip but remains subject to the parent clip

**Correction proposée :** Remove the unnecessary all-axis parent clip once vertical scroll ownership is corrected, relying on direction-aware scroll clipping, or provide a measured horizontal halo-safe inset for the category row without changing focus geometry. Validate both edge cards on the Fire Stick.

**Critères d’acceptation**

- The Live TV focus halo is fully visible to the left of its border.
- The Series focus halo is fully visible to the right of its border.
- Movies keeps the same focus rendering and all three cards retain equal width and spacing.
- No Home content is allowed to draw vertically over the fixed header after changing the clipping strategy.
- The 25 dp horizontal TV-safe margin remains respected.

**Notes :** This clipping is horizontal and distinct from TV-HOME-001, although both involve the same scroll viewport.

### TV-HOME-002 — Hero copy is French while the surrounding Home interface is English

- **Gravité :** mineur
- **Catégorie :** visuel
- **Écran :** Home hero
- **Reproductibilité :** Observed in every Home checkpoint
- **Confiance :** high

**Étapes de reproduction**

1. Open Home with the current profile and wait five seconds.
2. Compare the hero title/subtitle with the header, category cards and section titles.

**Résultat actuel :** The hero displays French copy while Home, Live TV, Movies, Series, category actions, Continue watching and Trending titles are English.

**Résultat attendu :** All visible Home copy follows the active application locale, including remotely configured hero slides.

**Impact :** The first screen looks unfinished and the language hierarchy is inconsistent.

**Preuves**

- `checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png`
- `checkpoints/20260717-034954-790595-home-header-settings-focus-after-5s/screenshot.png`

**Zone de code suspectée**

- HomeHeroBanner.kt HomeSlide.toHeroSlide(): remote title/subtitle are rendered without locale selection
- Remote Home slide configuration/content

**Correction proposée :** Serve locale-keyed hero text or fall back to SmartVisionStrings when the remote slide locale does not match the active UI locale.

**Critères d’acceptation**

- Under English UI, hero title and subtitle are English.
- Under French UI, hero title and subtitle are French.
- Changing slides does not mix locales on the same Home screen.

**Notes :** This issue is independent of the scroll/clipping root cause.
