# WWN — Large-scale continuation — 2026-09-16

Implemented without Gradle/APK work:
- campaign integrity validator and conservative roster/party repair layer;
- portable-backup validation now rejects structurally invalid WWN campaign state;
- fixed HealthService persistence drift by synchronizing active-character mutations back into charactersState;
- party member move/remove operations with active-group recovery;
- searchable/filterable journal service and in-app Diário search/filter controls;
- PlayerMechanicsFacade now exposes health/recovery and party movement operations;
- added source-level large-scale regression harness.

Validation:
- all 137 JavaScript modules/scripts: syntax PASS;
- large-scale regression harness: PASS;
- source-only forbidden-artifact scan: PASS at packaging time.

Gradle, APK/AAB and Android-device gates intentionally deferred by user instruction.
