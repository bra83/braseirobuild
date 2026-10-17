# AoR Ace/Gunner R2C3 source review

- Position: `ace:gunner:r2c3`, MUNICIÓN DE RESERVA (localized in the VTT as Munição reserva), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner tree places Spare Clip/MUNICIÓN DE RESERVA in R2C3 at a cost of 10 XP. Its card prevents ordinary ammunition loss caused by a Despair on an attack, while Limited Ammo weapons still deplete normally.
- Deterministic consumer: `core/controller.mjs` `adjudicateAttackDespairAmmoLoss` validates the current attack ID, weapon and Despair. It consults the source-backed SPARECL rank to prevent ordinary ammo loss and issue a persisted audit receipt; it rejects Limited Ammo weapons before that prevention branch.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R2C3 Spare Clip buys exact node, prevents Despair ammo loss, preserves Limited Ammo exception and survives three reloads`. It buys the adjacent R1C3 prerequisite and exact R2C3 through the bridge, equips a catalog weapon, verifies the ordinary Despair-prevention receipt and no-ammo-loss behavior after three restores, then proves a Limited Ammo weapon still consumes one round and is not treated as protected by Spare Clip.
- Full per-file regression: `evidence/AOR_GUNNER_R2C3_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,476 tests, 3,474 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R2C3. All other AoR positions retain their existing individual statuses.
