# BRASEIRO WWN — M4 IMPLEMENTATION WAVES

STATUS: PLAN_COMPLETE_PENDING_GUIDE_REAUDIT  
PRINCIPLE: smallest safe vertical slices, early visible proof, no backend-only marathon.

## Wave 1 — Foundations + M5 Persistence Skeleton
**Objective:** executable project skeleton, canonical state contract, deterministic RNG, Room bridge, transaction/revision primitives.  
**Modules:** android-shell, web-runtime shell, campaign-state, persistence-port, rng-core, event/journal skeleton.  
**Tests:** RNG vectors, read-failure semantics, transaction rollback, revision conflict, clean create/save/load.  
**Capture:** boot/resume shell only; no visual-lock claim.  
**Rollback:** tag/archive Wave 1 baseline; DB schema version 1 only after migrations test.  
**PASS:** unit + persistence integration + Android build/WebView smoke where environment exists.

## Wave 2 — Render Shell + Asset Resolver + Capture Harness
**Objective:** real 415×915 render path before broad feature implementation.  
**Modules:** ui-shell, asset-resolver, visual-geometry, capture-harness.  
**Screens:** boot/resume shell and SCR-015 composition skeleton using approved geometry with native dynamic placeholders.  
**Tests:** geometry schema, 27-layer bounds/z-order, asset hash lookup, proportional scaling, deterministic capture, overlay/diff generation.  
**PASS:** actual runtime capture pipeline works; no screenshot-as-UI; SCR-015 geometry structurally matches Forge.

## Wave 3 — M6 Core Vertical Slice
**Objective:** first end-to-end playable state path.  
**Flow:** create/load minimal legal character fixture -> campaign -> Scene -> PLAYER_ACTION -> rules -> dice -> commit -> deterministic Barbara test response -> journal -> save -> close/reopen.  
**Modules:** rules core subset, action-service, character subset, dice, Barbara interface, journal.  
**Tests:** full flow, GM_HELP non-advance, crash between commit/narrative, reopen continuity.  
**PASS:** Integration Gate E subset passes with no fake state.

## Wave 4 — Full Character / Sheet / Inventory / Magic
**Objective:** complete WWN character creation and live character subsystems.  
**Screens:** SCR-006..011, SCR-019, SCR-020.  
**Modules:** full character legality, classes/partials, Foci, equipment, encumbrance, spells/Arts/Effort, advancement.  
**Tests:** legal/illegal fixtures, option isolation, derived stats, save/reopen.  
**PASS:** release-blocking character Feature IDs reach required unit/integration evidence.

## Wave 5 — Atlas + Wilderness / Exact SCR-015 Visual Lock Candidate
**Objective:** real pan/zoom, canonical position, routes, 6-mile reference behavior, travel/time/resources, fog/discovery, encounter hooks.  
**Screens:** SCR-014, SCR-015.  
**Modules:** atlas-domain, HexGeometryV1, wilderness-domain, world-clock, weather hooks.  
**Tests:** adjacency, no teleport-only model, route interruption, seed replay, process reopen mid-travel.  
**Visual:** implement SCR-015 against exact Forge geometry and run full overlay/pixel/binary diff loop.  
**PASS:** Atlas functional integration + SCR-015 visual gate; Visual Lock only if real diff gate passes.

## Wave 6 — Dungeon / Settlement / NPC Social
**Objective:** dungeon-turn procedure, site position, light/resources, transition to/from settlement and social NPC state.  
**Screens:** SCR-016..018.  
**Modules:** dungeon-domain, settlement read models, npc-world base.  
**Tests:** time/resource clocks, discovered site state, NPC memory/knowledge limits, save/reopen.  
**PASS:** dungeon and social integration gates.

## Wave 7 — Combat / VTT / Dice
**Objective:** real encounter state with entity-linked tokens and WWN combat resolution.  
**Screens:** SCR-021, SCR-022.  
**Modules:** encounter-domain, VTT projection, combat rules, Shock, maneuvers, injury/death/recovery hooks.  
**Tests:** token/entity binding, initiative/turns, HP/conditions, Shock, combat end state sync, dice visual/mechanical identity.  
**PASS:** no VTT parallel authority; deterministic combat fixtures pass.

## Wave 8 — Barbara + RAG + GM_HELP + Sources
**Objective:** production GM orchestration grounded in local sources while preserving rule/state authority.  
**Screens:** SCR-012, SCR-013, SCR-027.  
**Modules:** source-index, Barbara orchestrator, model adapter, GM_HELP.  
**Tests:** known rule retrieval, missing evidence, fabricated-locator rejection, excluded-system firewall, GM_HELP zero mutation, provider failure after commit.  
**PASS:** grounded GM pipeline with state-safe failure behavior.

## Wave 9 — Living World + Factions + Major Projects
**Objective:** persistent off-screen world simulation tied to canonical time.  
**Screens:** SCR-023..025.  
**Modules:** living-world scheduler, faction-domain, project-domain, NPC activities/missions.  
**Tests:** hundreds of NPC fixtures, mission timers, faction turns, optional module OFF, deterministic tick boundaries.  
**PASS:** state survives long campaigns and replay boundaries are documented/tested.

## Wave 10 — Journal / Options / Backup / Audio-TTS
**Objective:** operational completeness and safety utilities.  
**Screens:** SCR-026, SCR-028..032.  
**Modules:** journal search/read models, campaign options, backup-recovery, audio-session, native TTS/file flows.  
**Tests:** long-form TTS + STOP, option migrations, logical backup -> temp DB -> compare, failed import recovery.  
**PASS:** no unsafe overwrite; backup equivalence proven.

## Wave 11 — Remaining Screen Completion + Functional/Visual Regression
**Objective:** implement remaining M3B screen contracts, typography/asset consistency, no dead controls.  
**Screens:** all 34 including development-only Composer surfaces where required.  
**Tests:** feature-to-screen coverage, all interactive controls, 415×915 regression, accessibility/IME, source/asset provenance.  
**PASS:** M15 Functional QA + M16 Visual QA candidate.

## Wave 12 — Android Hardening + Stress + RC
**Objective:** M17–M20 readiness.  
**Tests:** clean install, upgrade/migration, process death, background/foreground, WebView, S23-class device, 33-session stress, multi-thousand-turn replay, hundreds NPC/missions, backup cycles, ZIP integrity.  
**PASS:** required A–E evidence before device acceptance; then F on real device. No timeout-as-pass.

## Wave ordering rationale

Dependency-critical order is:
`persistence/RNG -> render/capture -> end-to-end action -> character -> position/time -> site/NPC -> combat -> grounded GM -> living world -> operational utilities -> full regression -> Android hardening`.

This deliberately makes a real screen and a real vertical slice appear early while keeping state authority safe.
