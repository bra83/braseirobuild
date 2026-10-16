# SWE02 p.13 physician follow-up evidence

- Printed source: *The Long Arm of the Hutt*, p.13 (P241 source proof); official PDF/source identifier is recorded in `validation/P241_SWE02_SOURCE_PROOFS.json`.
- Path: `swe02SocialP241.mjs:nynCriticalCare` → `GameController.treatSWE02NynCritical` → offline bridge → `swe02NynCritical` UI.
- State: one persisted attempt per identified patient and Critical Injury; success removes the actual injury, failure keeps it; ordinary safehouse care must precede the check.
- Persistence proof: test fixture reopens `GameController` and validates adapter state three times for successful and failed attempts.
- Command: `node --test --test-reporter=tap tests/uae-p241-swe02-nyn-newmeen-social.test.mjs`
- Result: exit 0, 14 passed, 0 failed, 0 skipped. Raw TAP: `focused.tap`; exit code: `exit-code.txt`.
- Scope limit: this closes the specific physician-attempt gap identified by the older P240 report; printed p.13 and SWE02 remain pending for fuel, ship system strain/critical repairs, docking/source details, and remaining page/branch gates.
