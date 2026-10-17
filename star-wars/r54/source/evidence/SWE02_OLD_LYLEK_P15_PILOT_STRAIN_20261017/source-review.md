# SWE02 p.15 fast-route Pilot strain proof

- Source: official *The Long Arm of the Hutt* PDF, Drive ID `1Lm26krELYpWU0Rx0WgPj__g72YU_F2lC`; SHA-256 `18FB9880242743D11765EACA442168FC3441A3B0D17AA92BC769057734B45489`; printed p.15 (physical PDF p.15), verified in `audits/SWE02_ACT1_DEEP_AUDIT.md`.
- Source semantics: the fast route calls for two Average Pilot checks; each failed check causes one Strain to every PC aboard. This proof covers that stated consequence only.
- Deterministic path: `app/src/main/assets/vtt/js/core/controller.mjs:3219-3250` resolves the check, binds the selected crew to stable roster IDs, applies one Strain to each onboard PC on each failed check, and saves per-PC applications with the p.15 source receipt. `app/src/main/assets/vtt/js/adventure/swe02OldLylekP15.mjs:108-116` validates and persists one application per crew ID.
- Test: `tests/swe02-old-lylek-encounter.test.mjs` :: `failed fast-route Pilot checks apply party-wide strain by stable PC ID and persist after reopen`.
- Regression run: `node --test --test-reporter=tap tests/swe02-old-lylek-encounter.test.mjs`; 12/12 pass, including two PCs, two failed checks, one Strain per PC per failure, and three fresh reloads.
- Scope: this closes the fast-route party-Strain consumer gap. It does not certify printed p.15, pages 15–18, SWE02 Act 1, or the adventure.
