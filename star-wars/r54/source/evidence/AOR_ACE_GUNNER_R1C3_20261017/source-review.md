# AoR Ace/Gunner R1C3 source review

- Position: `ace:gunner:r1c3`, ABRUMAR DEFENSAS (localized in the VTT as Sobrecarregar defesas), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68. The Ace: Gunner tree places Overwhelm Defenses in R1C3 at a cost of 5 XP. The source limits its use to an unsuccessful starship/vehicle weapon attack: spend 2 Advantage per rank to reduce defense in the attacked zone by 1 per 2 Advantage spent.
- Deterministic consumer: `core/controller.mjs` queues the quality only after the failed vehicle attack and checks source-backed rank and available Advantage before activating it. Activation spends the Advantage and changes the selected zone defense; `rules/vehicles.mjs` exposes the effective zone defense.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R1C3 Overwhelm buys exact node, spends Advantage on a failed vehicle attack and survives three reloads`. It buys the exact node through `BraseiroUiBridge.buyTalent`, checks receipt, source and XP, restores three independent controllers, makes a failed vehicle attack with at least 2 Advantage, activates the exact quality, and checks the persistent zone defense result.
- Full per-file regression: `evidence/AOR_GUNNER_R1C3_FULL_REGRESSION_20261017/summary.json` and the exact-position proof TAP log. Result: 383 files, 3,474 tests, 3,472 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R1C3. All other AoR positions retain their existing individual statuses.
