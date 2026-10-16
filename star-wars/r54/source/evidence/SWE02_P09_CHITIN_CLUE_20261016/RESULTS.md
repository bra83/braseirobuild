# SWE02 p.9–10 — marked chitin clue

Status: `IMPLEMENTADO_NAO_CERTIFICADO`.

## Source and interpretation

- Adventure: Fantasy Flight Games, *Star Wars: Edge of the Empire — Long Arm of the Hutt*, official PDF page 8 / printed page 9. The optional chitin examination calls for a Hard Knowledge check. Its Success, Advantage and Triumph results reveal separate facts; the clue does not identify the shell's exact origin. [Official adventure PDF](https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf)
- Continuation: same adventure, PDF page 9 / printed page 10. Keeping the marked shell is optional evidence about Sivor; this can support the later Geonosis scene. The chain remains optional and does not infer an exact origin.
- Rules mapping: *Edge of the Empire Core Rulebook*, printed p.122, splits Knowledge into six specialties. Since the adventure presents the check generically as Knowledge, the GM selects the appropriate EotE specialty at the table. The UI does not silently choose Xenology. The six specialties are corroborated by the official EotE character sheet and recorded in `audits/RULES_CORE_SOURCE_INTAKE_20261016.md`.

## Implementation

- Deterministic executor: `app/src/main/assets/vtt/js/adventure/swe02KraytFangP238.mjs` records the real net Success, net Advantage and Triumph and maps them to the four p.9 clues. The check only becomes available after the third hold is discovered. Exact origin remains false.
- Controller: `GameController.resolvePublishedAdventureCheck` requires a valid GM-selected EotE Knowledge specialty, uses that skill for the actual dice pool, and persists the skill and Core p.122 citation with the roll receipt.
- UI: `app/src/main/assets/vtt/index.html` and `app/src/main/assets/vtt/js/ui/app.mjs` expose the specialty selector and the explicit “keep marked shell” action. The p.9 findings appear in the ship status after a successful check.
- The shell retention action is a separate persisted choice. P237 evidence on printed p.30 requires prior discovery, successful p.9 examination and explicit retention before Piddock can identify it. New P237 evidence points to printed p.10; validation continues accepting old p.7 evidence rows so older saves remain readable.
- RAG's role is source retrieval and citation. Gemini/narration may describe the clues or suggest a typed action; the controller and adventure validators remain the only path that applies mechanics or state.

## Verification

- Focused run: `node --test --test-reporter=tap tests/uae-p238-swe02-krayt-fang-ship-source.test.mjs tests/swe02-krayt-fang-pp06-11-family.test.mjs tests/uae-p237-swe02-geonosis-dimmock-gantry.test.mjs tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs` — 59 passed, 0 failed, 0 skipped. Raw TAP: `focused.tap`.
- Controller test rejects a missing specialty and an invalid AoR-only specialty, records Xenology only when the GM selects it, and confirms the failed check receipt remains after three independent controller reloads. The P237 scenario verifies the successful clue/keep chain and three JSON reopen validations.
- Bridge bundle: `node tools/build-ui-bridge-bundle.mjs` — PASS, 135 modules.
- Full per-file regression, including all six stress files: 382 files (376 regular, 6 stress), 3,452 tests, 3,450 passed, 0 failed, 2 skipped. Raw per-file TAP and summary: `../SWE02_P09_CHITIN_FULL_REGRESSION_FINAL2_20261016/`. The two skips are offline Gradle-wrapper packaging checks because the JDK `jar` executable is unavailable.
- Syntax checks passed for the changed JavaScript modules; P237/P238 source-proof JSON parsed successfully.

## Remaining limits

This slice does not certify either page or the adventure. Full p.9–10 page-wide source coverage, visual review of the UI, other ship clues and cargo ownership, and the remaining SWE02 Acts 1–3 gates are still open. The selected EotE/AoR rules corpus and all 360 AoR consumer proofs remain pending.
