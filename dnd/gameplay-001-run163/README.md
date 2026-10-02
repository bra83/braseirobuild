# GAMEPLAY-001 — RUN163 reconciliation packet

## Status
READY_FOR_MASTER_HOOK / PARTIAL_INTEGRATION_PASS

## Canonical base
BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip
SHA-256: a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813

The current repository main branch does not contain the expanded D&D RUN163 tree. For that reason this branch deliberately does **not** overlay partial RUN163 Android sources onto main. It records the reconciliable GAMEPLAY-001 checkpoint stored in the canonical Drive workspace.

## Checkpoint
BRASEIRO_DND_6.6.2_GAMEPLAY001_FAERUN_EBERRON_SANDBOX_RUNTIME_PTBR_PATCH.zip
SHA-256: a4669e90434ccb0b1e769ed650084dcbbd8d46b0935de57c736d239d06678832
Drive: https://drive.google.com/file/d/1xBAl8F926owmHHnDVokIrR-vWpNyGDfJ/view?usp=drivesdk

The ZIP contains the full RUN163-based working tree plus:
- GAMEPLAY001_REPORT.md
- PATCH_MANIFEST_GAMEPLAY001.md
- GAMEPLAY001_VALIDATION.txt
- GAMEPLAY001_AUDIT.txt
- GAMEPLAY001_MASTER_HOOK_CONTRACT.md
- GAMEPLAY-001.patch

Patch SHA-256: 4df3626f8990ff08936fcaf7ef31a3193e08fa7f645e54871df72eba126a82fb

## Runtime delta
Changed:
- android/app/src/main/java/com/braseiro/dnd5e/WorldStateEngine.java
- android/app/src/main/java/com/braseiro/dnd5e/GameplaySandboxRuntimeEngine.java (new)

Added tests:
- tools/test_gameplay_001_sandbox_runtime.sh
- tools/test_gameplay_001_gateway_contracts.sh
- tools/test_gameplay_001_worldstate_compile.sh

Shared critical files intentionally unchanged:
- MasterOrchestrator.java
- AppDb.java
- MainActivity.java

## Implemented
- explicit question != action separation;
- manual-roll/request flow without fabricated rolls;
- sandbox state persisted through existing party session-preferences JSON;
- Faerun regional/actionable inventory, faction/deity/travel gateway wrappers;
- Eberron Sharn Inquisitives, Dragonmarked Intrigue and Morgrave Expeditions sandbox contexts;
- travel destination changes only after a resolved gateway and externally confirmed resource commit;
- Gemini proposal gate blocks direct HP, slots, item, condition, renown, roll/save, resource/cost, location, damage/healing and AC mutations;
- scene time/weather continuity retained from persisted world state;
- no scripted adventure introduced.

## Validation
16/16 scripts PASS:
- 13 inherited RUN163 suites;
- GAMEPLAY_001_SANDBOX_RUNTIME_PASS;
- GAMEPLAY_001_GATEWAY_CONTRACTS_PASS;
- GAMEPLAY_001_WORLDSTATE_COMPILE_PASS.

Invariants:
- PT-BR preserved
- auto_rolls=false
- APK_BUILT=false
- BOOK_COMPLETE=false
- Mystara excluded

## MASTER hook required
MasterOrchestrator.java is shared/critical and was not edited by GAMEPLAY. The MASTER should wire the new WorldStateEngine entrypoints:
- gameplaySandboxContext()
- previewGameplayInput(...)
- commitGameplayInput(...)
- resolveGameplayEberronFramework(...)
- resolveGameplayFaerunFaction(...)
- resolveGameplayFaerunDeityRenown(...)
- beginGameplayTravel(...)
- resolveGameplayFaerunTravelService(...)
- gateGeminiGameplayProposal(...)

After that hook, rerun all 16 suites.

## ENGINE dependency
Any monetary/resource cost for travel remains ENGINE-owned. GAMEPLAY requires engine_resource_commit_confirmed=true before committing destination/world transition and does not calculate/debit costs itself.
