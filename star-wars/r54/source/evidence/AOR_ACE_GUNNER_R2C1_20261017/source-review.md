# AoR Ace/Gunner R2C1 source review

- Position: `ace:gunner:r2c1`, ENDURECIDO (localized in the VTT as Endurecido), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner talent tree places Toughened/ENDURECIDO in R2C1, connected below R1C1, and the card grants +2 wound threshold for 10 XP.
- Deterministic consumer: `rules/talents.mjs` `applyImmediateEffect` adds 2 to `derived.wounds.threshold`. `rules/health.mjs` compares current wounds with `effectiveWoundThreshold` and marks incapacitation only when wounds exceed the effective threshold.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R2C1 Toughened buys exact node, raises wound boundary and survives three reloads`. It buys the adjacent R1C1 prerequisite, buys R2C1 through `BraseiroUiBridge.buyTalent` by exact ID, checks its source receipt, 10 XP slot cost and +2 effect, tests both sides of the wound boundary, and reopens the saved controller three times.
- Full per-file regression: `evidence/AOR_GUNNER_R2C1_FULL_REGRESSION_20261017/summary.json` and the per-test TAP log for the individual-position proof test. Result: 383 files, 3,471 tests, 3,469 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R2C1. All other AoR positions retain their existing individual statuses.
