# BRASEIRO WWN — WAVE 3 / M6 CORE VERTICAL SLICE

STATUS: PASS_NON_ANDROID_INTEGRATION
DATE: 2026-09-05

## Implemented

- WWN attribute modifier core with explicit SRD source references.
- WWN deterministic skill check core: `2d6 + skill + attribute modifier + situational modifier >= difficulty`.
- Untrained pertinent-skill contribution `-1` when the check is attemptable.
- `SCR-015 / OBSERVAR` as the first real PLAYER_ACTION mechanical path.
- Difficulty `8` is explicitly classified as scene/GM design for this encounter, not as a universal WWN observation rule.
- Deterministic RNG counter state committed with the mechanical action.
- Atomic mechanical commit before narrative generation.
- Deterministic Barbara Wave 3 test adapter operating only on already-resolved mechanics.
- Narrative event append tied to the same mechanical revision, with no campaign-state mutation.
- Recovery after narrative-provider failure without reroll and without revision increment.
- GM_HELP read-only contract.
- Memory persistence parity for browser/integration tests.
- Room repository/DAO/JS bridge surfaces for narrative append and event listing.
- SCR-015 UI hook for real `observe` execution in bundled Chromium runtime.
- Other SCR-015 choices remain `DEFERRED_NO_MUTATION` rather than fake implementations.

## Tests / evidence

- Node: 20/20 PASS.
- Static guard: PASS.
- SQLite M5 contract: PASS.
- Wave 3 SQLite narrative append contract: PASS.
- Wave 3 UI bundled Chromium integration: PASS.
- GM_HELP non-advance: PASS.
- Mechanical commit survives injected narrative failure: PASS.
- Narrative recovery performs no reroll: PASS.
- Deferred actions zero-mutation: PASS.
- SCR-015 touch/notes/scroll/UTF-8/geometry regression: PASS.
- Runtime asset validation: PASS.
- Visual regression against the approved menu-corrected Wave 2 capture: **0 changed pixels at tolerance 0**.
- Kotlin native contract static check: PASS; Android compilation not executed because Android SDK/Gradle wrapper are unavailable in this environment.

## Visual preservation

Initial Wave 3 SCR-015 rendering is pixel-identical to the menu-corrected Wave 2 baseline before any player action:

- viewport: 415×915
- changed pixels: 0 / 379,725
- mean absolute error: 0
- tolerance: 0

The visual contract therefore did not regress while wiring the mechanical vertical slice.

## Boundaries / not claimed

- Full Character Creator legality is Wave 4. The Wave 3 fixture is only validated for the core-check subset (level 1, legal attribute range, level-0/1 skills).
- Full Atlas/travel remains Wave 5.
- Full NPC/social behavior remains Wave 6.
- Ambush and combat/VTT remain Wave 7.
- Production Barbara/RAG remains Wave 8.
- Android BUILD/WEBVIEW/DEVICE evidence is not claimed.
- Revalidation against the external original Forge source package was skipped because `/mnt/data/wwn_m3b/unpacked` is unavailable in this runtime; runtime assets and unchanged visual output were independently verified.
