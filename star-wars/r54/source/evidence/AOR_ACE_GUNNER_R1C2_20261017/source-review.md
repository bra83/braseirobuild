# AoR Ace/Gunner R1C2 source review

- Position: `ace:gunner:r1c2`, APLOMO (localized in the VTT as Compostura), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (zero-based index 68); the printed footer reads 68 and the Ace: Gunner tree places Grit/APLOMO in R1C2. The source states the talent increases the character's strain threshold by 1. The local character sheet translates strain as Tensão and the talent label as Compostura.
- Deterministic consumer: `rules/talents.mjs` `applyImmediateEffect`, APLOMO/GRIT cases, adds 1 to `derived.strain.threshold` and returns the effect receipt. `rules/health.mjs` uses that effective threshold for incapacitation.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R1C2 Grit buys exact node, adds strain threshold and survives three reloads`. It purchases through `BraseiroUiBridge.buyTalent` using the exact node ID, checks XP and effect receipt, validates the strain boundary, verifies the UI's exact ID route, and reopens the controller three times with the receipt and threshold intact.
- Full per-file regression: `evidence/AOR_GUNNER_R1C2_FULL_REGRESSION_20261017/summary.json` and the per-test TAP log for the individual-position proof test. Result: 383 files, 3,469 tests, 3,467 passed, 0 failed, 2 skipped (JDK jar executable unavailable for offline Gradle-wrapper packaging checks), including all 6 stress files.
- Certification scope: this evidence certifies only R1C2. Other AoR positions remain individually unproven and pending.
