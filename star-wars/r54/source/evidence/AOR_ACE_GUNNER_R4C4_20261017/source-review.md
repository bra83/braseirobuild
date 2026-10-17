# AoR Ace/Gunner R4C4 source review

- Position: `ace:gunner:r4c4`, SALIDA TÉRMICA (localized as Saída térmica), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (one-based; zero-based index 68), printed footer 68. The Gunner talent tree places Exhaust Port at R4C4 for 20 XP. The card says that before a starship or vehicle attack the character may spend 1 Destiny Point to ignore Massive for that attack.
- Deterministic consumer: `core/controller.mjs:7137-7189` verifies the source-backed EXHAUST_PORT purchase, a Massive target, and one light Destiny Point; it runs the attack with Massive ignored and flips one Destiny Point. `core/uiBridge.mjs:1961-1965` passes the attack option to the controller. The new UI checkbox in `index.html#exhaustPortLabel` is surfaced only when the exact talent is present, disables when no Massive target or light Destiny Point is available, and `ui/app.mjs` forwards the selection into `game.resolveVehicleAttack`.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R4C4 Exhaust Port buys exact node, ignores Massive on vehicle attack and survives three reloads`. It buys R1C4 through R3C4 and exact R4C4 by `BraseiroUiBridge.buyTalent`, verifies 20 XP and source provenance, resolves a vehicle attack through the bridge against Massive 2, confirms the attack ignores Massive and spends exactly 1 Destiny Point, and verifies the purchase and attack state across three controller restores before and after use.
- Focused UI regression: `tests/vehicle-ui.test.mjs`.
- Full per-file regression: `evidence/AOR_GUNNER_R4C4_FULL_REGRESSION_20261017/summary.json` and per-file TAP logs, including all six stress files.
- Certification scope: this evidence certifies only R4C4; other AoR positions retain their individual statuses.
