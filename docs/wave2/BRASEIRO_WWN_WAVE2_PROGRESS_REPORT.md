# BRASEIRO WWN — WAVE 2 PROGRESS

DATE: 2026-09-05  
STATUS: STRUCTURAL_IMPLEMENTATION_COMPLETE / RUNTIME_CAPTURE_BLOCKED_BY_CONTAINER

## Why this is the right visual point
Wave 2 is the first real render/capture wave. The user previously requested that, when the project reached the appropriate point, a screen concept be brought forward **guided by the rules**. That point is now reached.

Rule-guided reference brought forward:
- `SCR015_RULE_GUIDED_CONCEPT_REFERENCE.png`
- screen: `SCR-015 — Wilderness / Hex Exploration contextual encounter`
- exact geometry authority: 415×915 Forge geometry, 27 layers
- mechanical boundary: choice buttons create typed PLAYER_ACTION intents; they never hard-code WWN outcomes. `INICIAR COMBATE` requests canonical combat; Rules Engine decides legality/consequence. GM notes remain read-only/help.

## Implemented this round
- ingested exact 27-layer SCR-015 geometry into runtime data;
- verified 27/27 source asset SHA-256 values;
- created provenance-aware derived icon assets from user-approved Forge assets, with native text kept out of the images;
- implemented `visual-geometry` validator for schema, viewport, layer IDs, bounds, z-order and aspect lock;
- implemented `asset-resolver` with provenance blocking;
- implemented real DOM/CSS `scr015.html` structural runtime at exact 415×915 geometry;
- replaced dynamic/baked title, location, coordinates, contact, stats, narrative, choices, GM notes and navigation labels with native DOM text;
- kept scene art, compass/location icon and GM-note icon as independent assets;
- wired screen buttons to typed UI/PLAYER_ACTION/route events; no button is decorative in the DOM implementation;
- preserved `EXPLORAR` as the sole active bottom-nav state;
- created proportional normalized 415×915 reference for visual-diff comparison (height fit + subpixel-equivalent center crop; no non-proportional stretch);
- implemented 50% overlay, pixel diff, binary black/white diff and JSON metrics with explicit tolerance;
- added a Chromium capture script for environments where headless rendering is functional.

## Executed evidence
- Node/domain/geometry tests: **12/12 PASS**.
- Geometry: **27 layers / 415×915 / bounds/z-order/aspect = PASS**.
- Source asset integrity: **27/27 hashes PASS**.
- Visual diff harness: **PASS**; identical images return zero changed pixels, controlled mutation is detected.
- Self-audit reference at tolerance 16: **0 changed pixels** (harness sanity only; not runtime evidence).

## Current blocker
The system Chromium binary in this execution container hangs before producing any screenshot, even with headless/no-sandbox/virtual-time variants. Android SDK/WebView/emulator are also unavailable.

Therefore:
- `RUNTIME_CAPTURE = BLOCKED_ENVIRONMENT`
- `OVERLAY_AGAINST_REAL_RUNTIME = NOT_RUN`
- `VISUAL_LOCK = NOT_ESTABLISHED`

This blocker is environmental only. The real runtime screen, geometry validator, asset resolver and visual-diff harness are implemented and ready to execute in Android/WebView or a functional Chromium environment.

## Next exact action
As soon as a functional runtime capture environment is available:
1. capture `scr015.html` at exact 415×915;
2. run overlay 50%;
3. run pixel diff;
4. run binary diff at tolerance 16, then tune tolerance only if anti-aliasing evidence warrants it;
5. measure geometry/text/assets separately;
6. correct implementation without redesign;
7. establish SCR-015 Visual Lock only after actual capture passes;
8. then continue the broader Wave 2 render shell and Wave 3 end-to-end playable slice.
