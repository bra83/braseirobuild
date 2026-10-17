# AoR Ace/Gunner R5C1 source review

- Position: `ace:gunner:r5c1`, MADERA DE HÉROE (localized as Fibra de herói), printed page 68 of *Star Wars: Age of Rebellion Core Rulebook*.
- Source file reviewed: `AoR-Core-Rulebook.pdf`, Google Drive file ID `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq`.
- SHA-256: `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50`.
- Visual review: physical PDF page 69 (one-based; zero-based index 68), printed footer 68. The Ace: Gunner tree places Heroic Fortitude at R5C1 for 25 XP; its card permits spending 1 Destiny Point to ignore Critical Injury effects on Brawn or Agility checks through the encounter.
- Deterministic consumer: `core/controller.mjs:4374-4386` requires the purchased source-backed talent, an active player turn and one light Destiny point, then persists the active encounter effect. `rules/critical.mjs` suppresses Critical Injury check modifiers only for Brawn/Agility and preserves non-check injury effects. `core/uiBridge.mjs:1631-1635` exposes activation; new `index.html#heroicFortitudeControl` and `ui/app.mjs` controls surface the exact ability during the player's turn.
- Exact acquisition and persistence test: `tests/uae-aor-individual-position-proofs.test.mjs`, test `AoR p.68 Ace/Gunner R5C1 Heroic Fortitude buys exact node, shields Brawn check and survives three reloads`. It buys the connected path and exact R5C1 through `BraseiroUiBridge.buyTalent`, verifies 25 XP and source provenance, activates through the bridge, spends 1 light Destiny, confirms Brawn Critical Injury setback and difficulty penalties are absent on a live controller skill check, and verifies the purchase/effect across three fresh controller restores before and after the check.
- Full per-file regression: `evidence/AOR_GUNNER_R5C1_FULL_REGRESSION_20261017/summary.json` and per-file TAP logs, including all six stress files.
- Certification scope: this evidence certifies only R5C1. Other AoR positions retain their individual statuses.
