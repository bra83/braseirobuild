# SWE02 p.12 focused source-to-code evidence

Source: *The Long Arm of the Hutt*, printed p.12, “Thwheek Attacks!” and “If the Krayt Fang Is Shot Down.” The persisted source proof is `validation/P239_SWE02_SOURCE_PROOFS.json`; implementation paths are linked there to `swe02ThwheekP239.mjs`, `swe02VehicleInitiative.mjs`, vehicle rules, controller methods, UI controls and focused tests.

## Page claims and deterministic ownership

| Printed claim | Deterministic implementation/evidence |
|---|---|
| Interception is conditional on route/tracker state. | Adventure progression computes the interception predicate from Astrogation route and tracker outcome. Tests cover optimal course + slowed tracker bypass, non-optimal course despite slowed tracker, and route receipts. |
| Thwheek’s G1-M4-C profile, linked medium lasers, and initial Short range. | `rules/vehicles.mjs` defines source-backed profile; the vehicle engine owns range, attacks and hull trauma. Tests inspect profile, require actual Krayt Fang, and verify range-band maneuvering. |
| Thwheek and PCs roll Cool, except a PC who did not see the fighter emerge/turn rolls Vigilance. | `swe02VehicleInitiative.mjs` rolls and sorts initiative; the GM UI records unaware PCs by stable ID. The resolved skill, slots and actor-bound actions persist and validate against roster IDs. |
| Thwheek starts with Stay on Target and switches to Evasive Maneuvers after its first hit. | `swe02ThwheekP239.mjs` derives the maneuver from actual attack receipts and persists the round transition; test asserts the change after a hit. |
| Five hull trauma makes Thwheek break off; the party may pursue or continue to Nabat. | Combat resolution requires actual incoming attack receipts and hull damage; pursuit keeps combat open, withdrawal closes it and unlocks Nabat. Test covers the full branch. |
| If the Krayt Fang is shot down, make a Hard Pilot check, then Hard Resilience for each actual crew PC; success on the Pilot check upgrades one positive die, failure causes a Critical Injury and each Threat causes one wound. | Crash flow snapshots the actual initiative roster by stable PC ID, resolves Pilot before crew, grants one positive-die upgrade on soft landing and applies Critical Injury/wounds through deterministic rules. Tests cover duplicate names, incomplete crew, one check per ID, soft-landing upgrade, failure injury, Threat wounds, persistence and tamper rejection. |

RAG retrieves the cited p.12 text and preserves the page reference. Gemini/Barbara can narrate or propose typed awareness/action choices; GM adjudication supplies awareness and pursuit choices, while code validates rolls, turns, outcomes, injuries and state transitions. LLM output does not directly change mechanics or save state.

## Verification

- `node --test --test-reporter=tap tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs` — 23 passed, 0 failed, 0 skipped. Raw output: `focused.tap`.
- `node --test --test-reporter=tap tests/vehicle-controller.test.mjs` — 4 passed, 0 failed, 0 skipped. Raw output: `vehicle-controller.tap`.
- The P239 suite includes adjacent p.8/p.11 route/transit assertions in addition to p.12, as shown by test names in the TAP output.
- Related initiative and crash evidence remains in `evidence/SWE02_P12_INITIATIVE_EXCEPTION_20261016/` and `evidence/SWE02_P12_CRASH_CREW_IDS_20261016/`.

This is focused evidence, not editorial certification. The page remains pending for end-to-end UI inspection, a clean full-project regression, and complete source audit across the adventure. The adventure, other four authorized adventures, selected rules corpus and all 360 AoR consumer rows remain pending.
