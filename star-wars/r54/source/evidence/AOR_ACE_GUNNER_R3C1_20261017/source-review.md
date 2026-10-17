# AoR Ace/Gunner R3C1 source review

- Position: `ace:gunner:r3c1`, ROBUSTO rank 2 (localized as Robusto), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68), printed footer 68. The Gunner tree places Durable/ROBUSTO in R3C1, costs 15 XP, and repeats its per-rank reduction to Critical Injury results with a minimum of 1.
- Deterministic consumer: `rules/talents.mjs` `durableCriticalModifier` returns -10 per source-backed DURA rank; `rules/critical.mjs` applies the modifier and clamps the final result to 1; `core/controller.mjs` `applyPlayerCritical` commits the result.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R3C1 Durable rank 2 and R4C1 Toughened rank 2 preserve exact IDs and cumulative effects`. It buys the exact prerequisite path including R1C1, R2C1, R2C2 and R3C2, then exact R3C1; verifies its source receipt, 15 XP cost, rank-2 critical modifier (-20), floor 1, and the rank-2 effect after three reopens.
- Full per-file regression: `evidence/AOR_GUNNER_R3C1_R4C1_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,473 tests, 3,471 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R3C1. R4C1 has its own source-review note and separate ledger proof.
