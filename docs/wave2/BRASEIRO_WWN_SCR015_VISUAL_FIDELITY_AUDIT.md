# BRASEIRO WWN — SCR-015 Visual Fidelity Audit

STATUS: CORRECTED_BROWSER_PASS_WITH_ENVIRONMENT_LIMITATIONS  
SCREEN: SCR-015 — Encontro no Hex / Ruínas da Vigília  
DATE: 2026-09-05  
GOLDEN VIEWPORT: 415×915  
BINARY DIFF TOLERANCE: 16/255 (fixed; not increased during correction)

## 1. Authority and capture method

Approved concept authority remains `SCR015_RULE_GUIDED_CONCEPT_REFERENCE.png` (697×1536) and was not modified.
For pixel comparison only, a 415×915 audit derivative was produced from the same approved reference. The original concept remains untouched.

Real environment used: **Chromium 144.0.7559.96, headless browser, DPR 1, viewport 415×915**.
The browser environment blocks `localhost` and `file://` navigation by administrator policy. Therefore the QA harness executes the **real SCR-015 HTML/CSS/JS sources** with the same JSON geometry/fixture and exact PNG bytes injected into a Playwright page with `set_content`. This is not a whole-screen image substitution; runtime DOM, native text, buttons, event handlers, scrolling, and open/collapsed state execute in Chromium.

Capture waits for `document.fonts.status === "loaded"` and all images to report `complete && naturalWidth > 0` before screenshot.

## 2. Iterative correction result

Initial browser capture:
- changed pixels @ tolerance 16: 96,257 / 379,725
- changed ratio: 25.3491%
- mean absolute channel error: 15.5378

Final browser capture:
- changed pixels @ tolerance 16: 81,495 / 379,725
- changed ratio: 21.4616%
- mean absolute channel error: 13.4681
- changed pixels reduced by 14,762 (15.34% relative reduction)

App-owned comparison with concept OS-status-bar region masked only for diagnostic accounting:
- changed pixels: 80,507 / 379,725
- changed ratio: 21.2014%
- mean absolute channel error: 13.0645

The unmasked full binary diff is the official visual evidence. The masked number is diagnostic only and does not replace/hide the full result.

## 3. Corrections applied

Priority 1 — composition/assets:
- preserved all 27 Forge geometry records as the outer DOM geometry authority;
- reused exact approved top-navigation, section-heading and bottom-navigation component PNGs;
- retained exact compass, scene, location badge and notes-button assets;
- derived text-free runtime chrome from approved component assets for contact badge, stats, narrative panel, four choice buttons, combat button and GM notes panel;
- dynamic narrative, stats, encounter status, choices and GM notes remain native text/state, not baked into the screen;
- added exact component chrome without whole-screen screenshot substitution;
- corrected scene-art internal registration by +1 px X / +2 px Y and brightness 1.016 inside the unchanged 389×244 layer container after image correlation against the approved concept;
- all image scaling uses `contain`; no non-proportional stretch was introduced.

Priority 2 — spacing/typography:
- corrected stat icon/text offsets and separators;
- corrected GM-note feather/header/content offsets and line wrapping;
- corrected narrative line height and scroll behavior;
- corrected action-label positioning, including the primary combat action;
- changed runtime serif fallback to `Georgia, "Times New Roman", serif` for closer platform-safe matching;
- corrected location-title size/weight to 19 px / 400;
- added outer scene-shell frame and bottom-navigation separator without changing interaction geometry.

Priority 3 — details:
- preserved UTF-8 accents (`RUÍNAS`, `DISTÂNCIA`, `REAÇÃO`, etc.);
- focus-visible state retained for interactive controls;
- internal long-text panels hide scrollbars visually while preserving touch scrolling;
- GM notes now support open/collapsed/reopen state.

## 4. Geometry audit

`SCR015_GEOMETRY_AUDIT.json`:
- layers checked: 27/27
- viewport: 415×915
- max DOM bounding-box deviation from approved geometry: **0 px**
- x/y/w/h: PASS for all 27 layers
- no outer document scrolling: PASS

The scene image itself is registered +1/+2 px **inside** its exact outer layer; the geometry container remains unchanged.

## 5. Asset audit

Source/Forge validation:
- approved geometry/source layers: 27/27
- source hashes validated: 27/27

Runtime asset audit:
- runtime exact/derived assets checked: 25
- missing files: 0
- hash mismatches: 0
- empty-alpha assets: 0
- dynamic-text chrome includes transparent text zones: PASS
- exact static component reuse: PASS
- non-proportional-stretch guard: PASS
- dynamic choice/contact/stat/narrative/notes text remains native: PASS

## 6. Interaction / responsive regression

Chromium touch-mode QA at 415×915:
- Observe tap -> `PLAYER_ACTION_INTENT/observe`: PASS
- Approach tap -> `PLAYER_ACTION_INTENT/approach`: PASS
- Ambush tap -> `PLAYER_ACTION_INTENT/ambush`: PASS
- Retreat tap -> `PLAYER_ACTION_INTENT/retreat`: PASS
- Start Combat tap -> `PLAYER_ACTION_INTENT/start_combat`: PASS
- top Search action: PASS
- Explore route: PASS
- only Explore active: PASS
- GM notes collapse: PASS
- GM notes reopen: PASS
- long narrative internal touch-scroll: PASS
- long GM notes internal touch-scroll: PASS
- body/outer layout remains 415×915 without unintended scrolling: PASS
- editable controls on SCR-015: 0
- keyboard/IME: **N/A on this specific screen because there is no input/textarea/contenteditable control**

## 7. Functional regression preserved

- JS/geometry/RNG/persistence Node tests: 12/12 PASS
- static guard: PASS (`Math.random`, localStorage, extra bridge prohibited)
- SQLite contract: PASS (create/load, revision conflict, rollback, corrupt read)
- source asset/geometry validation: PASS 27/27
- runtime asset/provenance validation: PASS

No rule, canonical-save, RNG, navigation-intent or persistence code was removed to improve visual matching.

## 8. Legitimate / environment-dependent differences

1. The approved concept contains a phone OS status bar (`19:42`, signal, battery). The Chromium page does not own or render Android system chrome. This remains visible in the **unmasked** diff and is also measured separately with an OS-only diagnostic mask.
2. Exact concept typeface metadata/font file is not part of the approved reusable runtime asset package. Dynamic text therefore uses a platform-safe serif stack; this is the largest remaining correctable-looking raster difference that cannot be made exact without an authoritative/reusable font.
3. Pixel anti-aliasing/resampling differs between the approved rendered concept and Chromium, especially around small serif text and fine one-pixel borders.

## 9. Verification not executed

Not executed because this environment has **no Android SDK, no `adb`, no emulator, and no Gradle wrapper JAR**:
- Android build validation;
- Android WebView capture;
- Android status/navigation bar fidelity;
- physical S23 Ultra capture/touch/IME test;
- Android WebView font rasterization comparison.

These are not marked PASS.

## 10. Remaining pendências (max 5)

1. Capture the same state in real Android WebView at the golden device viewport and rerun overlay/pixel/binary diff.
2. Resolve/approve an exact reusable runtime typeface if exact glyph fidelity is required; otherwise platform-serif difference remains accepted but visible.
3. Validate Android system status/navigation chrome against the concept on device; browser cannot own this region.
4. Re-run touch/IME acceptance on physical device once Android tooling/device execution is available.

## NEXT_EXACT_ACTION

When an Android-capable executor is available: build the current project unchanged, open SCR-015 with the same deterministic fixture/state, capture at the target S23 Ultra viewport, wait for fonts/assets, then rerun tolerance-16 overlay/pixel/binary diff. Correct only WebView/device-specific deviations discovered there; do not reopen the concept or replace native text with baked imagery.
