# AoR Ace/Gunner R4C1 source review

- Position: `ace:gunner:r4c1`, ENDURECIDO rank 2 (localized as Endurecido), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68), printed footer 68. The Gunner tree places Toughened/ENDURECIDO in R4C1, costs 20 XP, and grants +2 wound threshold per rank.
- Deterministic consumer: `rules/talents.mjs` `applyImmediateEffect` adds 2 to `derived.wounds.threshold` per source-backed ENDURECIDO purchase; `rules/health.mjs` uses the threshold to decide incapacitation.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R3C1 Durable rank 2 and R4C1 Toughened rank 2 preserve exact IDs and cumulative effects`. It buys the exact source path through R3C1, buys exact R4C1 through `BraseiroUiBridge.buyTalent`, checks the 20 XP slot cost, cumulative +4 threshold from two ranks, health boundary, source receipt, UI route, and three reopens.
- Full per-file regression: `evidence/AOR_GUNNER_R3C1_R4C1_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,473 tests, 3,471 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R4C1. R3C1 has its own source-review note and separate ledger proof.
