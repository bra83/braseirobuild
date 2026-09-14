# STAR WARS — EDGE OF THE GALAXY — BLOCK04 REVIEW

## Scope

Large post-fix audit of the SHA-pinned R38 source with the approved R46 shell preserved. No image generation and no R46 redesign.

## Priority fixes carried into this gate

- External Gemini handoff for `ILUSTRAÇÃO`, using the current scene prompt, plus manual `IMPORTAR` flow.
- Six recovered pre-existing scene visuals used as semantic fallback only until a scene-specific imported image exists.
- Android system-inset values bridged to CSS safe-area variables.
- Existing approved bottom-nav PNG artwork rendered without relying on CSS mask support.

## Browser/runtime result

The corrected broad runtime gate passed execution for 415×915 and 360×800 with:

- no global horizontal document overflow;
- bottom navigation inside the viewport;
- all five main tabs navigable and exactly one active tab at a time;
- no JavaScript page errors;
- no local HTTP asset failures;
- no missing local CSS/HTML references.

The automated gate initially emitted two `OFFSCREEN_CONTROL` findings in `REGISTROS` for `Contatos conhecidos` and `Histórico`.

## REGISTROS finding review

Focused DOM inspection proved both controls are children of:

```html
<nav aria-label="Estados de Registros" style="display: flex; gap: 7px; overflow-x: auto; padding-bottom: 2px;">
```

The buttons intentionally use `flex: 0 0 auto`. Therefore their initial x-coordinate beyond the viewport is the expected state of the existing horizontal scroller, not evidence that the controls are clipped by the document or inaccessible. The document itself remains width-locked to the viewport and the parent navigation owns horizontal scrolling.

**Classification:** auditor false-positive / intentional horizontally scrollable sub-navigation. No R46 layout mutation justified.

## Android gate

- `:app:compileDebugJavaWithJavac`: PASS
- `:app:assembleDebug`: PASS
- APK contains patched `assets/vtt/r46/final-runtime.js`: PASS
- APK contains recovered scene visual assets: PASS
- Validation APK SHA256: `c30262a664c90969ef7c2a214102f7f6a781993c6f2ab0dc5eaa3bc472e4ca3a`

## Residual validation boundary

The safe-area correction is compiled and the CSS/runtime behavior is validated structurally, but actual OEM navigation-bar/status-bar behavior still requires a physical Android device or equivalent system-bar instrumentation. No additional app mutation is justified by the current automated evidence.
