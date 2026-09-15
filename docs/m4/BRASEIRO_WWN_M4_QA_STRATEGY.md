# BRASEIRO WWN — M4 QA + CAPTURE STRATEGY

STATUS: DESIGNED / TESTS_NOT_RUN

## 1. Evidence ladder

No vague DONE state is allowed:

`CODE_COMPLETE -> STATIC_PASS -> UNIT_PASS -> INTEGRATION_PASS -> BUILD_PASS -> EMULATOR_PASS -> WEBVIEW_PASS -> DEVICE_PASS`

A timeout, skipped test, unavailable environment or screenshot existence is never PASS.

## 2. Gate families

### Gate A — Static / Rules
- source/RULE_ID traceability;
- excluded-system firewall;
- module dependency lint;
- no mechanical `Math.random()`;
- no UI-owned rule constants;
- optional-rule namespace isolation;
- no unauthorized protected asset use.

### Gate B — State / Persistence
- atomic commit/rollback;
- revision conflict;
- save/load equivalence;
- process kill/reopen;
- snapshot replay;
- migration forward path;
- failed migration recovery;
- backup round trip into clean temp DB;
- READ FAILURE != EMPTY CAMPAIGN.

### Gate C — Procedural / Determinism
- RNG golden vectors;
- same seed + same version -> same output;
- stream isolation;
- hex adjacency/path invariants;
- dungeon/world generation invariants;
- NPC/faction tick determinism boundaries;
- replay from event history.

### Gate D — Visual 415×915
For every approved or release-blocking screen state:
1. capture real runtime at 415×915;
2. wait for fonts/images and deterministic fixture state;
3. compare against the approved concept/geometry;
4. produce side-by-side;
5. 50% overlay;
6. absolute pixel diff;
7. binary black/white diff;
8. geometry report;
9. asset report;
10. typography/color report;
11. correct implementation and recapture until gate passes.

#### Candidate visual tolerances (to be calibrated in Wave 2, not yet PASS criteria)
- `APPROVED_EXACT` static geometry: target ±1 px on boxes/anchors.
- binary diff default pixel tolerance: 8/255 per RGB channel after dynamic masks.
- static changed-pixel target: <=0.5% for exact-reference regions.
- dynamic text/time/weather/image slots are masked or compared structurally, never used to excuse static geometry drift.
- any wrong asset, non-proportional stretch, missing layer or wrong active nav state is automatic FAIL regardless of pixel ratio.

For SCR-015, Forge 415×915 geometry is the authority; the 697×1536 reference bitmap is visual-only.

### Gate E — Integration / Full Flow
Canonical vertical slice:
`Create/Load Character -> Campaign -> Scene -> PLAYER_ACTION -> Rules -> Dice -> Commit -> Narrative -> NPC/World -> Save -> Close -> Reopen -> Continue`.

Also test:
- scene <-> Atlas position continuity;
- wilderness travel/time/resources;
- site/dungeon -> combat -> return;
- inventory and magic resource changes;
- GM_HELP no-advance;
- NPC knowledge filtering;
- faction/world updates after time advance;
- journal matches committed events.

### Gate F — Physical Device Acceptance
Only after A–E pass for the candidate build.
- S23 Ultra/target Android class: insets, 415×915 logical capture target where possible, IME, gestures, WebView, audio/TTS, lifecycle.
- Physical device acceptance does not substitute for automated development QA.

## 3. Required automated harnesses

### Unit
Pure JS domain/rules/RNG tests with deterministic fixtures.

### Browser integration
Headless Chromium/Playwright-equivalent harness for DOM, interaction, and deterministic visual capture at 415×915.

### Android integration
Instrumentation tests for bridge, persistence, lifecycle, file APIs, TTS and WebView load.

### Persistence fault injection
Simulate:
- transaction failure;
- corrupt snapshot;
- unavailable file;
- migration exception;
- process death between mechanical commit and narrative append.

## 4. Stress strategy

Before RC:
- 33 full synthetic sessions minimum as a repeatable campaign batch;
- multi-thousand-turn campaign replay;
- hundreds of NPC entities and concurrent mission/activity records;
- faction turns and off-screen world ticks;
- repeated save/close/reopen;
- backup/restore cycles;
- Atlas discovery growth;
- combat and noncombat mixed sessions;
- memory and source-index growth;
- leak/heap trend and first-frame timing.

Stress completion requires assertions, not merely “did not crash”.

## 5. SCR-015 first visual golden

Wave 2/5 must use `ENCONTRO NO HEX / RUÍNAS DA VIGÍLIA` as the first exact visual proof because it already has approved Forge geometry.

Required evidence bundle:
- `capture_415x915.png`
- `reference_normalized_415x915.png` generated only for comparison, never runtime asset
- `side_by_side.png`
- `overlay_50.png`
- `pixel_diff.png`
- `binary_diff_tol8.png`
- `geometry_report.json`
- `asset_report.json`
- `visual_gate.md`

Only real runtime capture can establish `VISUAL_LOCK`.
