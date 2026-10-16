# SWE02 p.12 crash crew identity follow-up

This slice makes the printed crash rule resolve once for every actual PC aboard, using stable roster identity. It does not certify the printed page or SWE02.

## Source and mechanics

- Source: *The Long Arm of the Hutt*, printed p.12, “If the Krayt Fang Is Shot Down.” After Hard Piloting, each crew member makes a Hard Resilience check; success on Pilot's check allows one positive-die upgrade; failure causes a Critical Injury; each Threat causes one wound.
- The pilot action snapshots the Cool initiative participants as expected crew IDs and records the active pilot ID. State validation ties those IDs to the persisted party roster.
- Resilience is keyed by stable PC ID, so duplicate character names cannot skip a crew check or count as two crew members. UI lets the user select which roster PC makes the next check.
- Existing deterministic effects remain in code; RAG carries the page citation, and Gemini/Barbara only narrate or propose typed actions.

## Verification

- `node --test --test-reporter=tap tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs` — 18 passed, 0 failed, 0 skipped. See `focused.tap`.
- Covered duplicate PC names, once-per-ID enforcement, exact roster set validation on migration/reopen, all expected IDs, three-person incomplete crew, save/reopen, soft-landing positive-die upgrade, failed check Critical Injury, Threat-to-wound handling, and a pre-feature active Thwheek save that continues its historical round flow.
- `node --test --test-reporter=tap tests/vehicle-controller.test.mjs` — 4 passed, 0 failed, 0 skipped.
- The official UI bridge bundle rebuilt with 135 modules. All changed `.mjs` files, the UI module and bundle passed `node --check`; `git diff --check` passed.

## Remaining page gate

Printed p.12 stays pending for source-complete coverage, including the situational exception for a PC who did not see Thwheek emerge and turn for the attack, plus page-wide persistence and regression evidence.
