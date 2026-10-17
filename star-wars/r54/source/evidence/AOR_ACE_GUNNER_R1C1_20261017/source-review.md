# AoR Ace/Gunner R1C1 source review

- Position: `ace:gunner:r1c1`, ROBUSTO (localized in the VTT as Robusto), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner talent tree places Durable/ROBUSTO in R1C1, sets a 5 XP cost, and describes a reduction of 10 per rank to the Critical Injury roll with a minimum result of 1.
- Deterministic consumer: `rules/talents.mjs` `durableCriticalModifier` counts source-backed DURA ranks and returns -10 per rank. `rules/critical.mjs` applies that modifier to the rolled result and clamps the final result to at least 1. `core/controller.mjs` `applyPlayerCritical` commits the resulting character state.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R1C1 Durable buys exact node, reduces critical result and survives three reloads`. It buys the exact node through `BraseiroUiBridge.buyTalent`, validates its source receipt and XP cost, applies a critical through `GameController`, verifies 50 becomes 40 and 1 remains 1, and reopens the persisted character three times to verify the exact receipt and -10 critical modifier remain effective.
- Full per-file regression: `evidence/AOR_GUNNER_R1C1_FULL_REGRESSION_20261017/summary.json` and the per-test TAP log for the individual-position proof test. Result: 383 files, 3,470 tests, 3,468 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R1C1. All other AoR positions retain their existing individual statuses.
