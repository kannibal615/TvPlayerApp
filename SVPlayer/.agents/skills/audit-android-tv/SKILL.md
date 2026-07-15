---
name: audit-android-tv
description: Audit an Android TV or Fire TV application on a local adb device to detect, reproduce, document, and prioritize functional defects, remote-control and focus-navigation problems, visual or UX inconsistencies, rendering jank, slow screen loading, CPU or memory issues, and stability failures. Use when Codex must explore a TV application, capture screenshots and logs, assess UI consistency, profile focused user flows, compare builds, produce an evidence-backed issue backlog, or prepare correction prompts without implementing fixes yet.
---

# Audit Android TV

Run a repeatable, evidence-backed audit on a real Android TV or Fire TV device. Separate observation from correction: inspect source code to understand and localize issues, but do not edit application code unless the user explicitly asks for fixes after the audit.

## Operating principles

- Treat the audit as read-only and non-destructive by default.
- Prefer a real TV device for timing and jank measurements.
- Use adb key events instead of screen coordinates for TV navigation.
- Derive expectations from product behavior, source code, established design patterns, and comparable screens.
- Record an issue only when evidence supports it. Mark uncertain diagnoses as hypotheses.
- Never place credentials, PIN values, tokens, personal data, or playlist URLs in screenshots, filenames, reports, commands shown to the user, or committed files.
- Do not clear app data, uninstall the app, delete profiles, purchase content, log out, or change persistent settings without explicit authorization.
- Keep raw artifacts outside this skill directory.

## Compose supporting skills

Load the installed `android-emulator-qa` skill for adb device control, screenshots, UI trees, key events, and logcat. Load the installed `android-performance` skill for Perfetto, Simpleperf, `gfxinfo`, startup, CPU, and memory evidence. Although the first skill is named for emulators, its adb workflow also applies to physical Fire TV devices.

## Phase 1: Establish scope and preflight

1. Read repository instructions and identify the Android application module, package ID, build variants, launcher activity, navigation routes, and existing tests.
2. Reuse the user's supplied adb serial. Otherwise use `ANDROID_TV_SERIAL`; if neither exists, ask for the serial. For this user's current setup, `192.168.1.33:5555` is the fallback serial, but verify it before every audit.
   On Windows, the capture helper automatically checks `%LOCALAPPDATA%\\Android\\Sdk\\platform-tools\\adb.exe`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT`. Use `ADB_PATH` only when adb is installed elsewhere.
3. Run the bundled preflight command:

   ```bash
   python3 "$SKILL_DIR/scripts/android_tv_evidence.py" preflight \
     --serial "$SERIAL" \
     --output "$RUN_DIR" \
     --package "$PACKAGE"
   ```

4. Stop and ask the user to accept the TV authorization dialog when adb reports `unauthorized`. Do not repeatedly reconnect while the dialog is waiting.
5. Prefer an already installed matching build. Install a debug or profileable build only when needed for source alignment or CPU profiling. Do not uninstall first.
6. Keep an existing authenticated/test session. If a PIN is required, accept it only from the current user request or `ANDROID_TV_AUDIT_PIN`, use it transiently, and never echo or persist it.
7. Create a run folder such as `artifacts/android-tv-audit/<timestamp>/`. Record device model, Fire OS/Android version, display size, density, package, version, commit, variant, date, and audit scope.

If the device is unreachable, distinguish network failure, adb authorization failure, sleeping device, missing adb, and wrong serial. Report the exact blocker instead of continuing with synthetic results.

## Phase 2: Build the screen and flow inventory

Inspect the source before driving the device:

- enumerate routes, drawers, tabs, dialogs, sheets, menus, players, onboarding, empty states, and error states;
- locate focus requesters, `focusProperties`, `focusRestorer`, key handlers, scroll containers, loading state, image loading, and navigation state;
- identify shared colors, typography, spacing, cards, toggles, buttons, and dialog components;
- note screens gated by profile type, PIN, content availability, or network state.

Read [references/smartvision-scenarios.md](references/smartvision-scenarios.md) and adapt it to the routes actually present. Produce a screen inventory before claiming full coverage. Mark each item `covered`, `partially covered`, `blocked`, `not applicable`, or `not reached`.

## Phase 3: Explore functional and TV-navigation behavior

Start from a known state and audit one focused scenario at a time.

For each reachable screen:

1. Capture the initial state before input.
2. Move focus in all four directions from important elements.
3. Verify focus visibility, deterministic order, restoration after Back, and the absence of loops, dead ends, skipped controls, or off-screen focus.
4. Press OK only on safe, understood controls. Confirm whether one press applies the action.
5. Verify Back closes the nearest transient surface before leaving the screen.
6. Exercise vertical and horizontal scrolling with the remote and verify that focused content remains visible.
7. Test loading, loaded, empty, error, disabled, selected, and modal states when safely reachable.
8. Compare displayed data with the selected category, profile, filter, or setting.
9. Save a checkpoint for every anomaly and important state:

   ```bash
   python3 "$SKILL_DIR/scripts/android_tv_evidence.py" capture \
     --serial "$SERIAL" \
     --package "$PACKAGE" \
     --output "$RUN_DIR" \
     --label "home-focus-trending" \
     --key DPAD_DOWN
   ```

The capture helper stores the screenshot, UI hierarchy when available, current activity/focus, recent package logs, `gfxinfo`, and metadata. UI hierarchy can be incomplete for Compose, SurfaceView, video, or custom canvas content; combine it with screenshots, focus behavior, logs, and source inspection.

Do not perform blind breadth-first OK-clicking. Use route knowledge and visible labels to avoid destructive or external actions. If a bug blocks deeper exploration, document the block and continue through another safe entry path when possible.

## Phase 4: Audit visual and UX consistency

Read [references/audit-rubric.md](references/audit-rubric.md). Compare related screens side by side and inspect at least:

- TV safe area, clipping, overscan, alignment, spacing, density, and balance;
- card size, aspect ratio, image crop, placeholder behavior, and late image replacement;
- typography hierarchy, truncation, wrapping, contrast, and localization stress;
- normal, focused, selected, disabled, loading, and error states;
- focus border, scale, glow, elevation, animation, and consistency;
- dialogs, numeric keyboard, toggles, buttons, dividers, and section containers;
- visual stability while focus moves or asynchronous data loads;
- scroll affordance and whether all content is remote-accessible.

Do not report taste alone as a defect. Explain the violated pattern, user impact, and recommended adjustment. Use `amélioration` severity for optional refinements.

## Phase 5: Measure performance on focused flows

Do not profile the entire application in one trace. Select flows that felt slow or are product-critical, then:

1. Measure cold and warm startup separately with `am start -W` where supported.
2. Reset and collect `gfxinfo framestats` around one exact flow.
3. Repeat important flows at least three times when practical.
4. Capture Perfetto for frame misses, main-thread stalls, scheduling, Binder work, locks, and Compose traces when the Fire OS build exposes them.
5. Use Simpleperf only when the package is debuggable/profileable and CPU hotspots are the question.
6. Capture `meminfo` before and after a repeated navigation loop when investigating growth or retention.
7. Correlate device-side traces with screenshots and logs. Do not use adb command round-trip time as precise UI latency.

Use fallbacks in this order when Fire OS restricts profilers: `gfxinfo` and `am start -W`, Perfetto system trace, app trace markers/logs, then repeated observational evidence. State every limitation in the report.

## Phase 6: Classify and document issues

Use these severities:

- `bloquant`: prevents a core flow or makes the application unusable;
- `critique`: severe malfunction, crash, data/security risk, or major inaccessible area;
- `majeur`: reproducible functional, focus, performance, or visual problem with significant impact;
- `mineur`: limited inconsistency or defect with a workaround;
- `amélioration`: non-defective UX or visual refinement.

Each issue must contain:

- stable ID;
- severity and category;
- screen and short title;
- exact remote-control reproduction steps;
- actual and expected result;
- user impact and reproducibility;
- screenshot, log, trace, or code evidence paths;
- suspected code area only when supported;
- recommended correction;
- measurable acceptance criteria;
- confidence level and profiling caveats where relevant.

Write findings to `issues.json` using the format documented in [references/audit-rubric.md](references/audit-rubric.md), then generate the deliverables:

```bash
python3 "$SKILL_DIR/scripts/render_audit.py" \
  --input "$RUN_DIR/issues.json" \
  --output "$RUN_DIR"
```

The command creates:

- `audit-report.md`: coverage, prioritized summary, and detailed evidence;
- `issues.csv`: sortable backlog;
- `codex-fix-prompts.md`: correction batches ready for a later Codex implementation pass.

## Phase 7: Validate the handoff

Before presenting the audit:

- open every referenced screenshot;
- verify that every artifact path exists;
- remove duplicates and separate root causes from symptoms;
- distinguish observed facts from suspected causes;
- confirm that blocked or untested screens are visible in the coverage table;
- ensure prompts request implementation and focused regression tests, not a new diagnosis;
- keep the audit separate from application code changes.

Lead the handoff with the highest-impact findings, coverage limitations, and the next smallest correction batch. After fixes are requested, rerun only the affected scenarios plus adjacent navigation regression paths, then compare before/after performance evidence.
