# Android TV audit rubric

Use this rubric to turn observations into reproducible, actionable findings.

## Functional checks

- The selected profile, category, folder, filter, and media item match the displayed content.
- One OK press triggers one intended action; no second click is required.
- Loading, retry, empty, offline, expired-session, and backend-error states are understandable and recoverable.
- Back closes the current dialog/menu before navigating away and preserves prior state where expected.
- Async updates do not replace stable content, reset selection, duplicate synchronization, or move focus unexpectedly.
- Destructive and sensitive actions require appropriate confirmation.

## Remote and focus checks

- Every actionable control is reachable with DPAD only.
- Initial focus is intentional and visible.
- Up, down, left, and right movement follows the visual layout.
- Focus never disappears, becomes trapped, jumps to an unrelated zone, or lands behind a popup.
- Returning from a child screen restores a sensible prior focus.
- Focused items stay inside the viewport; scrolling reveals all content.
- OK, Back, long labels, disabled controls, dialogs, keyboards, and player overlays behave consistently.

## Visual and UX checks

- Maintain TV-safe margins and avoid clipped text, cards, focus glow, or popups.
- Compare shared headers, side menus, cards, section containers, toggles, dividers, buttons, dialogs, and keyboards.
- Check typography scale, weight, line height, wrapping, truncation, and contrast.
- Check spacing rhythm, alignment, card ratios, image crop, placeholder quality, and density.
- Compare normal, focused, selected, pressed, disabled, loading, error, and empty states.
- Look for layout shifts, image swapping, flicker, stale content, and focus-induced data changes.
- Verify that visual emphasis matches action importance.

## Performance and stability checks

- Separate cold startup, warm startup, screen transition, data loading, image loading, and interaction response.
- Record frame/jank evidence for the exact flow, not general impressions.
- Inspect main-thread work, render work, recomposition, parsing, sorting, filtering, database access, image decoding, and network waits only when traces support the attribution.
- Compare memory before and after repeatable loops; one high sample alone is not a leak.
- Record crashes, ANRs, strict-mode failures, repeated exceptions, and timeout behavior from Logcat.
- State device, build, run count, trace type, and profiler limitations.

## Severity decision

| Severity | Decision rule |
|---|---|
| `bloquant` | Core journey cannot be completed or the app becomes unusable. |
| `critique` | Crash, severe data/security risk, or major inaccessible functionality. |
| `majeur` | Reproducible issue with significant functional, navigation, performance, or comprehension impact. |
| `mineur` | Localized defect with limited impact or a clear workaround. |
| `amélioration` | Optional refinement that improves clarity, coherence, or polish. |

## `issues.json` format

Use one top-level object. Keep evidence paths relative to the audit run directory when possible.

```json
{
  "title": "Android TV audit",
  "summary": "Short evidence-based summary.",
  "run": {
    "date": "2026-07-14",
    "device": "Fire TV",
    "serial": "redacted-or-alias",
    "android_version": "value",
    "package": "com.example.app",
    "app_version": "value",
    "build_variant": "debug",
    "commit": "abcdef1",
    "run_count": 3
  },
  "coverage": [
    {
      "screen": "Home",
      "status": "covered",
      "notes": "Initial, loaded and focus states tested."
    }
  ],
  "issues": [
    {
      "id": "TV-NAV-001",
      "severity": "majeur",
      "category": "navigation-focus",
      "screen": "Settings",
      "title": "Focus cannot reach the header",
      "repro_steps": [
        "Open Settings",
        "Focus the first side-menu item",
        "Press DPAD_UP"
      ],
      "actual": "Focus remains on the side-menu item.",
      "expected": "Focus moves to the main header.",
      "impact": "The header is inaccessible with the remote.",
      "reproducibility": "3/3",
      "evidence": [
        "checkpoints/settings-up/screenshot.png",
        "checkpoints/settings-up/window.txt"
      ],
      "suspected_area": [
        "SettingsScreen focusProperties"
      ],
      "recommendation": "Connect the first side-menu item's up target to the header focus requester.",
      "acceptance_criteria": [
        "DPAD_UP reaches the header in one press",
        "DPAD_DOWN returns to the prior menu item",
        "No focus loop is introduced"
      ],
      "confidence": "high",
      "notes": ""
    }
  ]
}
```

Allowed category examples: `fonctionnel`, `navigation-focus`, `performance`, `visuel`, `ux`, `accessibilité`, and `stabilité`.

Allowed coverage status values: `covered`, `partially covered`, `blocked`, `not applicable`, and `not reached`.

## Evidence quality

- Prefer a before/after pair when state transition is the defect.
- Cite the smallest relevant Logcat excerpt or trace report rather than an entire unfiltered log.
- Do not infer a source file solely from a screenshot.
- Use `confidence: low` when the observation is real but the cause is uncertain.
- Consolidate multiple visible symptoms into one issue when they share one demonstrated root cause.
