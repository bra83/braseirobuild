# RUN245 — QA / CI evidence

Data: 2026-10-10 UTC. Local historical checks remain recorded below; Android verification is through the exact BRELGA workflow run.

## BRELGA run 38059897445

- Branch `dnd/run245-normal-g05-codex`, commit `bb17769ad210c5e350b4100a87afd4e8a19357cf`, runner BRELGA.
- Exact RUN245 SHA `7f69f02e5b77a100cfd517b80c4de96c10d0b33cf3284d78112ebd3755e0222b`, 140,317,801 bytes, 1,213 ZIP entries; package and version identity matched.
- 27 source-wiring checks PASS. Java compile PASS. `assembleDebug` PASS.
- `testDebugUnitTest`: 76 tests, 15 failures, 0 errors, 0 skipped. Failure breakdown: 12 `org.json.JSONObject.put not mocked` (G07/G10/G11 rule tests), 2 SQLiteCantOpen fixture failures (new G05/G08 cases), 1 G08 opportunity fixture used `slashing` instead of canonical `cortante`.
- Targeted G02/G03 class: 3/3 PASS. Existing RHW on-hit class: 7/7 PASS. Bless: 3/3; spell target component: 3/3; G12 image policy: 3/3; G13 narration chunker: 4/4; existing Backup21 suite: 9/9.
- Follow-up patch SHA-256: `335af63eab942a937cf15103ffbb60f2cb16b1be615aca609b28682ea1778089`. At that point the follow-up had not yet completed; it was later verified by run 38061684317 below.
- The failure stopped the workflow before APK identity and signature verification. No signed/updated APK is claimed.
- Job logs, test XML and debug build outputs are preserved in release evidence: https://github.com/bra83/braseirobuild/releases/tag/run245-normal-brelga-38059897445. ZIP SHA-256: `340c60afbe2d16b1386c40c3864c21b5e6dcc25d0d50e78dd3bcc9fa56e3023c`.

## Historical local checks

- `python3 G05_EVIDENCE/test_run245_wiring.py`: latest local result PASS, 27 checks; source-only check, not Android/Robolectric.
- The earlier checkout's Android SDK was absent and its Gradle test task did not run. That local limitation is superseded by the BRELGA result above, including its 15 actual test failures.

## BRELGA run 38061235785

- `:app:compileDebugJavaWithJavac`: PASS; `:app:assembleDebug`: PASS.
- `testDebugUnitTest`: 76 tests, 1 failure, 0 errors, 0 skipped. The 3 G05 turn-bound rider tests and all other suites passed; the sole failure was G08/G09 replay expecting `position_replayed`, but route preflight returned `destination_is_origin`.
- Source correction: preflight now checks the persisted receipt key with the `ui-grid-` prefix used by commit. Focused result is preserved in `BRELGA_RUN_38061235785/`.
- Confirmation run 38061684317 uses commit `24364e46d74768131ab569989ba64ab402efd6d3`; its JUnit output was not yet available when that note was written; see the completed run below.
- Evidence release: https://github.com/bra83/braseirobuild/releases/tag/run245-normal-brelga-38061235785.


## BRELGA run 38061684317 — latest complete result

- Workflow: https://github.com/bra83/braseirobuild/actions/runs/38061684317; branch `dnd/run245-normal-g05-codex`; commit `24364e46d74768131ab569989ba64ab402efd6d3`; runner BRELGA. All workflow steps succeeded.
- Exact RUN245 SHA/CRC and application identity verification passed. Gate patch, SHA-pinned RUN220 runtime restore and 27 G05 static wiring checks passed.
- `:app:compileDebugJavaWithJavac`: PASS; `:app:testDebugUnitTest`: **76 tests across 28 classes, 0 failures/errors/skips**; `:app:assembleDebug`: PASS. The G08/G09 receipt-key replay fix is confirmed by the green movement class. G05 turn-bound riders, typed-damage rejection, G07, G10/G11, G12/G13 and Backup21 cases are green within their targeted scope.
- APK badging: `com.braseiro.dnd5e`, versionCode 606545, versionName `6.6.14-r245-g05-source-debug`; APK v2 debug signature verified. Release/update signing continuity is not verified.
- Evidence ZIP: https://github.com/bra83/braseirobuild/releases/tag/run245-normal-brelga-38061684317; 382,227,478 bytes, ZIP CRC PASS, SHA-256 `b4dd27e40341b0783cd7c23f7a5ec8c8846056f2bb2ee7b0758930c64e7058fa`. The run's 76 tests do not close functional gaps listed in the gate matrix.

## XMM OCR locator cross-check

Drive text extraction returned 1,066,072 OCR characters for the 277,647,654-byte XMM PDF after raw transfer exceeded HTTP 413. The 131-row CSV combines the row-level paraphrased auxiliary source facts with OCR locator outcomes: 87 creature+action label signals, 20 creature-only signals, and 24 rows without a reliable local creature match. It contains no copied page excerpts. The OCR has recognition noise and approximate page offsets; it is not primary validation of mechanics, DCs, saves, duration or effects.
