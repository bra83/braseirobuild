# G05 targeted 5e.tools review log — RUN245

## Result by issue range

| RUN245 rows | Source | Reviewed rows | Evidence |
|---|---|---:|---|
| 1, 3–6, 8–10, 15–18 | RHW (2026) | 12 | Existing individual action notes retained in the source and executor matrices |
| 2, 7, 11–14, 19–50 | RHW (2026) | 38 | `qa-ci/G05_REVIEW_RHW_002_050.csv` |
| 51–100 | RHW (2026) | 50 | `qa-ci/G05_REVIEW_RHW_051_100.csv` |
| 101–149 | RHW (2026) | 49 | `qa-ci/G05_REVIEW_RHW_101_149.csv` |
| 150–193 | XMM (2025) | 44 | `qa-ci/G05_REVIEW_XMM_150_193.csv` |
| 194–237 | XMM (2025) | 44 | `qa-ci/G05_REVIEW_XMM_194_237.csv` |
| 238–280 | XMM (2025) | 43 | `qa-ci/G05_REVIEW_XMM_238_280.csv` |
| **Total** |  | **280/280** | `G05_AUXILIARY_REVIEW_5ETOOLS_280.csv` and `G05_SOURCE_TO_EXECUTOR_MATRIX.csv` |

Each range CSV preserves the issue row number, creature, action, edition/source code, printed page, source URL, action section, mechanical facts checked, RUN245 comparison, confidence, and source/PDF limits. Duplicated issue rows remain separate.

## Targeted findings

- Use RHW source code for *Ravenloft: The Horrors Within* (2026). `books.html#rhw` is distinct from Van Richten (2021); no VRGR entry or PDF was substituted.
- Use XMM source code for the 2025 *Monster Manual*. The direct individual records came from the 5e.tools structured XMM bestiary; browser-style request headers were required for JSON fetches in some passes. The source is auxiliary, and the PDF was not reviewed page by page.
- Saving throws, grapple escape DCs, and non-combat ability checks are distinguished in row notes. Target size, trigger movement, damage types, duration, and recharge are recorded when present.
- Some RHW action entries point to separate named spell text and are not a complete spell implementation. Open details: RHW rows 7, 11, 19, 30, 36–39, 77, 111, 114, 115, 124, 125, 149; row 12 does not restate Siphon Spell’s save DC; row 50’s Wild Magic Surge has multiple outcome-specific saves; row 148’s Mind Swap recharge number is ambiguous.
- In XMM, Bugbear Stalker/Morningstar and Bugbear Warrior/Light Hammer do not impose Grappled; the attack receives advantage against a target already grappled by that bugbear. Quick Grapple is a separate bonus action. Row reports also identify escape DCs that are not saving throws.

## Executor status

Four source-bound action handler entries are now present: Aberrant Death’s Head / Brain-Rending Bite, Elder Thing / Soothing Tentacle, Madam Eva / Radiant Flame and Relentless Juggernaut / Crushing Stone. The prior RHW on-hit class passed in BRELGA; the three new turn-bound riders passed in BRELGA run 38061684317; 76/76 tests are green. The remaining 276 issue rows are source-reviewed but do not have action-specific deterministic executors approved here. No licensed RHW PDF was found in the supplied Drive folder; the XMM raw PDF fetch returned HTTP 413; readable but noisy OCR yielded label-only locator checks for all 131 XMM rows (87 creature+action, 20 creature only, 24 no reliable localizer). It did not validate mechanics. G05 remains open.
