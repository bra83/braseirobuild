# AoR Ace/Gunner R1C4 source review

- Position: `ace:gunner:r1c4`, DISPARO DEBILITANTE (localized in the VTT as Disparo debilitante), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner tree places Debilitating Shot in R1C4 at a cost of 5 XP. Its source effect applies after a successful starship/vehicle weapon attack: spend 2 Advantage to reduce the target's maximum speed by 1 through the end of the next round.
- Deterministic consumer: `core/controller.mjs` queues the quality only for a hit, checks the source-backed rank and Advantage spend, reduces target maximum speed by 1, and expires the effect at the stated round boundary. `rules/vehicles.mjs` returns the effective maximum speed for a given round.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R1C4 Debilitating Shot buys exact node, spends Advantage on a hit and persists speed reduction`. It purchases the exact node through `BraseiroUiBridge.buyTalent`, checks its source receipt and XP, restores three controllers, resolves a successful vehicle attack with actual Advantage, spends 2, and verifies speed reduction for rounds 1–2 and expiration by round 3.
- Full per-file regression: `evidence/AOR_GUNNER_R1C4_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,475 tests, 3,473 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R1C4. All other AoR positions retain their existing individual statuses.
