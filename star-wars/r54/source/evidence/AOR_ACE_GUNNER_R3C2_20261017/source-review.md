# AoR Ace/Gunner R3C2 source review

- Position: `ace:gunner:r3c2`, RESISTENTE (localized in the VTT as Resistente), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner tree places Enduring/RESISTENTE in R3C2; it is linked below R2C2 Brace and costs 15 XP. Its source card grants +1 soak.
- Deterministic consumer: `rules/talents.mjs` applies +1 to `derived.soak` for the source-backed RESISTENTE node. `rules/health.mjs` consumes that soak in `applyDamage`, subtracting it from incoming damage after Pierce. `core/controller.mjs` `applyPlayerDamage` commits the resolved result.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R3C2 Enduring buys exact node, raises live soak and survives three reloads`. It purchases exact path prerequisites R1C1, R2C1, and R2C2, then purchases R3C2 through `BraseiroUiBridge.buyTalent` by exact ID, checks source receipt and +1 soak, resolves incoming damage through the controller, and performs three fresh-controller reload checks against restored save state.
- Full per-file regression: `evidence/AOR_GUNNER_R3C2_FULL_REGRESSION_20261017/summary.json` and the per-test TAP log for the individual-position proof test. Result: 383 files, 3,472 tests, 3,470 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R3C2. All other AoR positions retain their existing individual statuses.
