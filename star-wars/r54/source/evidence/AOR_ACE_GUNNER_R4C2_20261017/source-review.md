# AoR Ace/Gunner R4C2 source review

- Position: `ace:gunner:r4c2`, RESISTENTE rank 2 (localized as Resistente), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68), printed footer 68. The Gunner tree places Enduring/RESISTENTE in R4C2 at a cost of 20 XP; its card grants +1 soak per rank.
- Deterministic consumer: `rules/talents.mjs` applies +1 `derived.soak` per source-backed RESISTENTE node. `rules/health.mjs` `applyDamage` subtracts the soak after Pierce, and `core/controller.mjs` `applyPlayerDamage` commits the result.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R4C2 Enduring rank 2 buys exact node, stacks soak and survives three reloads`. It purchases exact path nodes through R4C1, purchases R4C2 by exact ID through the bridge, verifies the 20 XP cost and second +1 soak, resolves incoming damage in the controller, and repeats source/soak/damage checks in three restored controllers.
- Full per-file regression: `evidence/AOR_GUNNER_R4C2_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,477 tests, 3,475 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R4C2. All other AoR positions retain their existing individual statuses.
