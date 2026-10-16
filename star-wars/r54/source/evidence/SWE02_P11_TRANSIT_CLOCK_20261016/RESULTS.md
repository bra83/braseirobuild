# SWE02 pp.8, 11 hyperspace transit clock

Source checked in the Drive copy of *The Long Arm of the Hutt.pdf* (Drive ID `1Lm26krELYpWU0Rx0WgPj__g72YU_F2lC`) and the official publisher PDF. Printed p.11 describes the normal Tatooine–Ryloth trip as little more than six hours and uneventful, with time to rest, talk and explore. Printed p.8 says failed Astrogation makes the trip longer, without stating by how much.

- `ship-depart-verified` records a minimum six-hour transit and advances one project world-clock tick. The project clock has four six-hour periods per day.
- On a normal route, the receipt stores the approximate duration and no extra delay.
- On a failed route, the receipt stores that the trip is longer and marks the unknown extra duration for GM adjudication. At the Ryloth arrival decision, the GM can enter extra whole six-hour blocks; code applies those ticks once and persists them. Repeated adjudication is rejected.
- Fuel has no numerical amount in the adventure. The text allows rest and recuperation but does not specify exact resource recovery here; this slice leaves those benefits available without automatically changing wounds or strain.

## Verification

- `node --test --test-reporter=tap tests/uae-p238-swe02-krayt-fang-ship-source.test.mjs tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs` — 36 passed, 0 failed, 0 skipped. Output: `focused.tap`.
- Covers normal and failed Astrogation duration, one-time clock advancement, GM-selected delay blocks, success-route rejection, state reload and duplicate-action rejection.
- `node --test --test-reporter=tap tests/vehicle-controller.test.mjs` — 4 passed, 0 failed, 0 skipped.
- Official UI bridge bundle rebuilt with 135 modules; changed JS syntax checks, JSON parsing and `git diff --check` passed.

SWE02 and p.11 remain pending for full cargo transfer/ownership, verified rest recovery, fuel representation and page-wide evidence.
