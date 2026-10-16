# SWE02 p.12 initiative awareness exception

This focused slice implements the printed conditional initiative rule from *The Long Arm of the Hutt*, p.12: Thwheek rolls Cool; PCs likely use Cool unless a PC did not see the fighter emerge from hyperspace and turn to attack, in which case that PC rolls Vigilance.

- The GM-facing selector records unaware PCs by stable roster ID before combat starts. Selection defaults to no PCs, matching the encounter narration where the PCs see the fighter emerge.
- The deterministic initiative resolver uses each selected PC's Vigilance (Willpower characteristic) pool and Cool (Presence characteristic) for the others. Thwheek's Cool pool is unchanged.
- Persisted slots record the chosen skill and the unaware roster IDs. State validation rejects unknown/duplicate IDs or a skill inconsistent with that record; older initiative saves without the new fields retain their prior Cool behavior.
- Gemini/Barbara may narrate the appearance or suggest a typed awareness proposal. The GM selection and code own the mechanical choice and roll.

## Verification

- `node --test --test-reporter=tap tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs` — 20 passed, 0 failed, 0 skipped. TAP output: `focused.tap`.
- Tests cover mixed awareness, recorded skill choice, state tamper rejection, unknown/duplicate PC IDs, and the existing crash/vehicle initiative behavior.
- `node tools/build-ui-bridge-bundle.mjs` — PASS, 135 modules.
- `node --test --test-reporter=tap tests/vehicle-controller.test.mjs` — 4 passed, 0 failed, 0 skipped.
- Changed JS syntax checks, JSON parsing, and `git diff --check` passed.

Printed p.12 and SWE02 remain pending for full page-wide source mapping, persistence coverage and regression.
