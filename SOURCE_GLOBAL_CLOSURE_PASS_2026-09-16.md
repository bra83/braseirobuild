# WWN — Global source closure pass

This pass continued source-only closure; Gradle/APK/device are not gates here.

## Newly closed as reachable product flows
- Campaign memory: persistent GM/player facts, explicit secret reveal, narrative threads, player-safe projection.
- Scheduled world events: persistent due-minute schedule, deterministic due selection, completion marking, conversion into campaign memory without leaking GM-only facts.
- Expedition support surfaced to GM operations: sea travel, sea encounter checks, pack-load capacity, privation, hireling costs, service costs and lifestyle effects.
- New CampaignMemoryService persists these operations through the canonical campaign revision/event log.

## Validation
- 142 WebView/core JS modules pass syntax validation.
- 16 QA suites pass.
- Existing 66 repeated simulated sessions remain green (33 standard + 33 deep).
- New macro-source-zero regression validates memory secrecy/reveal, schedule processing, sea travel, pack overload, privation, hirelings and lifestyle.
