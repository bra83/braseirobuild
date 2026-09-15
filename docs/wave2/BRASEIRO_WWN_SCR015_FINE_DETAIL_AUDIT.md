# BRASEIRO WWN — SCR-015 FINE DETAIL AUDIT

STATUS: FINE_DETAIL_PASS_COMPLETE_WITH_ENVIRONMENT_LIMITS
ENVIRONMENT: Chromium 144 headless, viewport 415×915, DPR 1
REFERENCE: approved concept normalized to 415×915
BINARY_DIFF_TOLERANCE: 16/255 fixed

## Corrections applied
- brand title weight reduced and letter spacing tuned to match approved reference;
- location title size tuned without changing approved geometry;
- scene asset horizontal offset corrected and brightness override removed;
- narrative line-height tuned while preserving internal scrolling;
- GM notes typography tuned while preserving long-text scrolling and collapsed/open state;
- mutable-text chrome rebuilt from approved component assets with restricted glyph inpainting so runtime DOM text no longer sits on a rectangular transparent patch;
- contact badge cleanup expanded to remove residual source glyphs;
- derivation tool and provenance updated so the fine-detail chrome is reproducible;
- no concept alteration, no non-proportional scaling, no whole-screen static UI substitution.

## Metrics
Previous full diff: 81,495 changed pixels / 21.4616% / MAE 13.4681.
Fine final full diff: 78,208 changed pixels / 20.5960% / MAE 12.7162.
Fine final app-owned diff with OS-chrome mask: 77,220 changed pixels / 20.3358% / MAE 12.3126.
Geometry: 27/27 canonical layers unchanged; max geometry deviation remains 0 px.

## Regression
- Runtime touch/route/open-collapse/long-text QA: PASS.
- Runtime asset/provenance QA: PASS.
- Source asset validation: PASS.
- SQLite contract: PASS.
- Static guard: PASS.
- Node geometry/RNG/persistence suite: PASS.

## Remaining legitimate differences
1. Browser capture has no real Android status bar/WebView rasterization; device/WebView check remains pending.
2. Static component resampling differs slightly from the normalized concept because Chromium and the reference normalization use different image sampling paths.
3. Native serif rasterization cannot be proven identical to Android WebView until device capture.
4. Minor texture/raster differences remain in reconstructed text-free chrome regions.
