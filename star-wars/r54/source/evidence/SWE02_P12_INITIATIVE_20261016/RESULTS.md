# SWE02 p.12 Cool initiative follow-up

This closes the previously documented gap between Cool rolls and actual Thwheek vehicle turns. It does not certify the printed page or the adventure.

## Hybrid authority boundary

- Deterministic code rolls each registered PC's Cool pool and Thwheek's source-backed two Ability dice, orders slots, enforces the current actor, advances rounds, and stores actor/slot action receipts.
- RAG remains the route for retrieving the cited SWE02 printed p.12 rule text; the persisted initiative record carries that page citation.
- Gemini/Barbara may narrate the resulting turn or propose a typed action. The engine validates and applies the action; narration cannot set a hit, skip a slot, advance a round, or change a vehicle state.

## Verification

- Focused command: `node --test --test-reporter=tap tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs`
- Result: 15 passed, 0 failed, 0 skipped. Raw TAP is in `focused.tap`.
- Covered: Cool order for each registered PC plus Thwheek; PC selection at each player slot; vehicle action actor receipts; blocked actions on the wrong side; mandatory Thwheek action before passing its live slot; no manual round bypass; save/reopen; existing conditional encounter, actual vehicle damage, crash and crew regressions.
- Generic vehicle-controller regression: `node --test --test-reporter=tap tests/vehicle-controller.test.mjs` — 4 passed, 0 failed, 0 skipped.
- Source modules, UI module and generated bundle passed `node --check`; `git diff --check` passed.
- Official bundle generator completed with 135 modules. This Windows environment lacked the TypeScript package assumed by the script; TypeScript 5.8.3 was unpacked under intermediate `work/` and supplied through `NODE_PATH` for generation.
- `tools/verify-p239-syntax.py` could not run: it requires `\\mnt\\data\\BRASEIRO_STAR_WARS_EDGE_R54_P238_SOURCE_CHECKPOINT.zip`, absent from this checkout.

## Remaining p.12 / global gates

Printed p.12 stays pending for full source-to-semantic proof of the crash and crew consequences and complete page-wide evidence. SWE02, all five adventures, the selected rules corpus, and all 360 AoR proof rows remain uncertified/pending.
