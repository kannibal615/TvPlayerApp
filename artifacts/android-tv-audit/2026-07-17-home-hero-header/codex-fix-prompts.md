# Lots de correction pour Codex

Traiter un lot à la fois. Inspecter les preuves et le code avant toute modification, préserver les changements existants, puis exécuter les tests ciblés et le scénario de validation Android TV.

## Lot 1 — priorité-haute / navigation-focus

```text
Corrige les anomalies Android TV suivantes : TV-HOME-001.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-HOME-001 — Home — Implicit focus relocation clips the hero and upper content below the fixed header
  Actuel : The hero loses its top 22 px after category focus. Continue watching focus moves the parent scroll from the explicit 288 px target to 436 px, hiding the hero and clipping the upper part of the category cards under the header. Trending focus repeats a 148 px post-focus drift. Returning to the header still leaves the hero top border and rounded corners clipped.
  Attendu : The explicit Home scroll position remains authoritative after focus is requested. At the top, the complete 132 dp hero, including its upper border and corners, starts below the header clearance. On lower rows, no text, badge, card or focus glow is clipped beneath the fixed header.
  Correction proposée : Make the explicit Home D-pad scroll job the only owner of parent vertical positioning. Neutralize the parent scroll container's automatic focused-child relocation for these routed targets, then verify the settled scroll value after focus. Do not compensate again with arbitrary header/hero spacers or clipping-only changes.
  Preuves : checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png; checkpoints/20260717-034304-511847-home-continue-repro-after-5s/screenshot.png; checkpoints/20260717-034347-098297-home-trending-movies-after-5s/screenshot.png; checkpoints/20260717-034730-635187-home-header-after-full-return-5s/screenshot.png; focus-scroll-evidence.txt
  Validation : Five seconds after Home entry, the hero top edge, border and both upper rounded corners are fully visible below the header.; After category focus settles, scrollState remains 0 rather than 22 px.; After Continue watching focus settles, scrollState remains at the chosen target with no +148 px drift.; No category badge, title, card edge or focus glow is clipped beneath the header in any vertical focus state.; DPAD_UP and DPAD_DOWN still target the first item of each available row and header focus remains reachable.
```

## Lot 2 — finition / visuel

```text
Corrige les anomalies Android TV suivantes : TV-HOME-003, TV-HOME-002.

Contraintes :
- Analyse d'abord les captures, logs, traces et zones de code indiquées dans le rapport d'audit.
- Corrige les causes racines sans refonte hors périmètre.
- Préserve la navigation complète à la télécommande et la cohérence visuelle existante.
- Ajoute ou adapte les tests ciblés lorsque cela est pertinent.
- Rejoue les critères d'acceptation sur l'appareil Android TV et rapporte les résultats.

Anomalies :
- TV-HOME-003 — Home category cards — Live TV and Series focus halos are clipped at the horizontal viewport edges
  Actuel : The Live TV halo is cut flush at the left edge and the Series halo is cut flush at the right edge. The centered Movies halo has room to render normally.
  Attendu : The complete focus halo remains visible around all three category cards, including the two cards touching the horizontal content edges.
  Correction proposée : Remove the unnecessary all-axis parent clip once vertical scroll ownership is corrected, relying on direction-aware scroll clipping, or provide a measured horizontal halo-safe inset for the category row without changing focus geometry. Validate both edge cards on the Fire Stick.
  Preuves : checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png; checkpoints/20260717-034109-591542-home-return-category-after-5s/screenshot.png; checkpoints/20260717-035030-114923-home-settings-down-after-5s/screenshot.png
  Validation : The Live TV focus halo is fully visible to the left of its border.; The Series focus halo is fully visible to the right of its border.; Movies keeps the same focus rendering and all three cards retain equal width and spacing.; No Home content is allowed to draw vertically over the fixed header after changing the clipping strategy.; The 25 dp horizontal TV-safe margin remains respected.
- TV-HOME-002 — Home hero — Hero copy is French while the surrounding Home interface is English
  Actuel : The hero displays French copy while Home, Live TV, Movies, Series, category actions, Continue watching and Trending titles are English.
  Attendu : All visible Home copy follows the active application locale, including remotely configured hero slides.
  Correction proposée : Serve locale-keyed hero text or fall back to SmartVisionStrings when the remote slide locale does not match the active UI locale.
  Preuves : checkpoints/20260717-033822-748416-home-initial-after-5s/screenshot.png; checkpoints/20260717-034954-790595-home-header-settings-focus-after-5s/screenshot.png
  Validation : Under English UI, hero title and subtitle are English.; Under French UI, hero title and subtitle are French.; Changing slides does not mix locales on the same Home screen.
```
