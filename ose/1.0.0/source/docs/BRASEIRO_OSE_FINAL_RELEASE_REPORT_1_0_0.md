# BRASEIRO OSE — FINAL RELEASE REPORT 1.0.0

Date: 2026-09-14  
Status: `FINAL_LOCAL_RELEASE_PASS_EXTERNAL_ANDROID_GATES_EXCLUDED`

## Release scope

This is the single final Android Studio source-project release for the locally executable closure gates. No APK or AAB is included or generated.

The approved 415×915 parchment UI, dark/gold primary navigation, MAP visual lock and Hex Geometry V1 were preserved. Mechanical authority remains in the OSE Rules Engine; Classic and Advanced stay isolated; Barbara remains narrative/orchestration only; `PLAYER_REACTION` remains the intentional mutation channel; GM Help remains zero-delta.

## Final executed gates

- Native Classic/Advanced character creation and loadout to `ACTIVE`: PASS.
- World bootstrap Classic/Advanced: PASS.
- Classic wilderness encounter corpus: 960/960 PASS.
- Advanced wilderness encounter corpus: 1,600/1,600 PASS.
- Advanced dungeon encounter corpus: 360/360 PASS.
- Advanced monster registry: 327/327 IDs available with source provenance policy enforced.
- Wilderness fatigue combat effect: 200 paired attack trials PASS (`-1 attack`, `-1 damage`, minimum damage 1).
- 33 Rules/Referee sessions: PASS, 528 actions, 52 encounters.
- 33 SessionEngine sessions: PASS, 33 idempotent retries, 33 narrations, 66 commits.
- Dungeon deterministic corpus: 1,024/1,024 generated worlds deterministic and reachable.
- Settlement discovery/transition gate: PASS.
- Causal world stress: 300 NPCs + 300 world events/missions PASS; plausible knowledge bounded; typed spatial location preserved.
- Persistence closure contract: migration, checksum, checkpoint, portable character validation, narration-only crash recovery and transition idempotency PASS.
- Persistence Room source contract: 12/12 static checks PASS; transactional commit/checkpoint and no destructive migration fallback present.
- Handoff additions contracts: 17/17 PASS.
- Final visual regression: 7/7 captures at exactly 415×915 PASS; no page/image errors; MAP Geometry V1 preserved.
- JS syntax / JSON / Python QA / local web build: PASS.
- Final secret scan: 0 literal API credentials.
- Source TODO/FIXME scan: 0.
- Forbidden binary scan: 0 APK, 0 AAB, 0 DEX.

## External gates not promoted

The following are not reported as PASS because this environment cannot execute them and the user explicitly removed them from the local release gate:

- full Gradle/Android compile;
- actual Room/Robolectric execution;
- Android emulator/device smoke;
- APK/AAB generation (explicitly forbidden for this delivery);
- real Gemini network smoke, which requires user credentials configured in-app.

The project includes the Gradle wrapper/configuration and Android source required for external Android Studio acceptance.

## Canonical evidence gaps intentionally preserved

The release does not invent missing rules:

- `XP_SPLIT_REMAINDER = MISSING_EVIDENCE`;
- Classic potion table 83–84 remains `MISSING_EVIDENCE` because the supplied table skips those results;
- unsupported Advanced post-level-14 progression remains not authorized where evidence is absent;
- referee-judgment situations remain `REFEREE_JUDGMENT`.

Advanced Referee content follows the frozen priority: v1.2 where present, pre-release v0.3 only as explicitly tagged supplemental gap source for genuinely missing v1.2 sections.

## Final project version

- Android `versionName`: `1.0.0`
- Web UI package version: `1.0.0`
- Gradle wrapper distribution configured: `8.10.2`

## Recovery

`RETOMADA_ATUAL.md` in the project root is the compact recovery checkpoint for any future maintenance. This release is not a request to reopen passed visual/source audits.
