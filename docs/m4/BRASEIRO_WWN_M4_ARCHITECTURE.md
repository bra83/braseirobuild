# BRASEIRO WWN — M4 EXECUTABLE ARCHITECTURE

STATUS: COMPLETE_PENDING_GUIDE_REAUDIT  
DATE: 2026-09-05  
PRECONDITION: M3B_FULL_CONCEPT_ASSET_COMPOSITION = PASS_BY_USER_CONTINUE  
IMPLEMENTATION: NOT_STARTED  
ANDROID_RUNTIME: NOT_STARTED

## 1. Objective

Define the executable architecture before implementation. This architecture preserves the project authority chain:

`WWN SOURCE EVIDENCE -> RULES ENGINE WWN -> CANONICAL CAMPAIGN STATE -> ACTION PIPELINE -> PROJECTIONS (UI/ATLAS/VTT/AUDIO) -> PERSISTENCE/JOURNAL`

The UI presents state and collects intent. It never becomes a second Rules Engine. Barbara/GM orchestrates but does not own rules or a second database. VTT is a projection of real encounter/campaign state. Atlas is the authority for geographic position. NPCs are persistent entities with bounded knowledge.

## 2. Product runtime shape

### Android shell
- Kotlin Android application.
- One primary Activity hosting a WebView runtime.
- `WebViewAssetLoader`-style local asset serving; no `file://` dependency for production UI.
- Native bridge only for platform capabilities and canonical persistence services.
- Insets, IME, lifecycle, file picker/share, audio/TTS, secure provider keys and backup handled natively.
- No RPG rule values duplicated in Kotlin.

### Web runtime
- ES2022 modules, HTML and CSS; no framework is required for the first implementation waves.
- Pure domain modules remain testable outside the DOM.
- UI reads immutable view models and emits typed intents.
- Dynamic text/state is native DOM data; no whole-screen screenshot is permitted as UI.
- Golden viewport contract: 415×915 portrait.

### Persistence runtime
- Room/SQLite behind a narrow native bridge.
- Canonical state is not duplicated in WebView storage.
- Browser-only test adapters are fixtures, never production authorities.
- Reads return explicit `OK | NOT_FOUND | CORRUPT | IO_ERROR`; a read error can never be interpreted as an empty campaign.

### Optional model provider
- Barbara natural-language generation uses a provider adapter.
- Provider credentials stay outside campaign files and are stored using Android secure storage.
- Rules, RNG, state mutation, time advancement and legality never depend on unstructured model output.
- Test harness uses a deterministic fake provider.

## 3. Authority boundaries

| Authority | Sole owner | Consumers | Forbidden duplication |
|---|---|---|---|
| WWN mechanical rule | `rules-wwn` | action pipeline, character, combat, travel, factions | UI, Barbara prompt, VTT |
| Campaign canonical state | `campaign-state` + persistence adapter | all runtime modules | localStorage, ad-hoc DOM state |
| Geographic position | `atlas-domain` in campaign state | travel, narration, world, VTT transition | scene-only coordinates |
| Encounter/VTT state | `encounter-domain` in campaign state | VTT, combat, GM narration | token widget state |
| Time/calendar | `world-clock` in campaign state | travel, dungeon, NPCs, factions, TTS triggers | component timers |
| NPC identity/memory/knowledge | `npc-world` | Barbara, settlement, combat | prompt-only NPC memory |
| Source evidence | `source-index` | Rules evidence, GM_HELP, Barbara | fabricated citations |
| RNG | `rng-core` | dice, procedural generators | Math.random in mechanics |
| Visual geometry | M3B geometry/Composer contracts | render shell | implementation-time beautify/reinterpretation |
| Audio/TTS state | `audio-session` | native audio bridge | background autonomous state mutation |

## 4. Action pipeline

All state-changing player actions use one pipeline:

1. `PLAYER_ACTION` emits a typed `IntentEnvelope`.
2. Load canonical state at revision `R`.
3. Resolve source/rule requirements.
4. Resolve deterministic mechanical outcome through `rules-wwn` and `rng-core`.
5. Validate invariants and derive a `StateDelta`.
6. Atomically commit `ActionEvent + new state revision + mechanical journal entry`.
7. Build Barbara context from the committed revision.
8. Generate narrative response through the model adapter.
9. Persist narrative as a separate append-only narrative event tied to the mechanical event.
10. Project the new revision to Scene, Atlas, VTT, Journal and Audio/TTS.

If narrative generation fails, the mechanical commit remains valid and recoverable. Reopen shows the committed result and can retry only the narrative step without rerolling or repeating the action.

`GM_HELP` is a separate query pipeline with no mutation capability. It receives a read-only snapshot and source retrieval port. It cannot spend resources, move the party, advance time, open combat or alter NPC/faction/world state.

## 5. Hex Geometry V1 hard contract

- Canonical coordinates: axial `(q,r)` with derived cube `(x,y,z)` for distance and adjacency.
- All rendered hexes are mathematically regular: equal side length, one global scale per map view.
- Orientation is versioned map metadata (`pointy` or `flat`); it is not inferred per cell.
- No per-cell pixel nudges are allowed.
- Pan/zoom are camera transforms only and never change canonical coordinates.
- Selecting a hex may add a ring/overlay but cannot deform or enlarge the active cell relative to the grid.
- World travel uses adjacency/path records, never destination teleport shortcuts.
- Standard wilderness reference remains 6 miles per hex where the selected WWN rule profile requires that reference.
- Fog/discovery is state keyed by hex coordinate, not a painted bitmap authority.

### Geographic coherence validator
Before a procedural map commit:
- every route is a chain of adjacent hexes;
- party position references an existing hex;
- rivers/roads cannot jump non-adjacent hexes;
- POIs reference valid coordinates;
- discovered/fog states cannot refer to nonexistent coordinates;
- terrain transitions may be scored/warned but must never silently rewrite authored user data;
- generators must be deterministic from stored seed and namespace.

## 6. Procedural determinism

`rng-core` owns all mechanical/procedural random numbers.

- Campaign stores a numeric master seed.
- Each subsystem uses a namespaced stream: `dice`, `encounter`, `hex`, `dungeon`, `weather`, `npc`, `faction`, etc.
- Stream derivation is stable and versioned; algorithm/version are stored in campaign metadata.
- Each committed random event records stream name, counter/ordinal and raw results.
- Replays never call `Math.random()`.
- A generator upgrade requires a new generator version; old campaigns keep their previous version for replay consistency.

## 7. Screen/runtime mapping

The 34 M3B screens are projections over modules, not 34 separate mini-apps. The critical mapping is:

- `SCR-011` -> character read model.
- `SCR-012` -> scene projection + PLAYER_ACTION.
- `SCR-013` -> read-only GM_HELP.
- `SCR-014` -> Atlas camera/projection.
- `SCR-015` -> Wilderness/Hex; user-approved Forge geometry is exact composition authority.
- `SCR-016` -> Dungeon/Site exploration.
- `SCR-017/018` -> settlement/NPC projections.
- `SCR-019` -> inventory/crafting.
- `SCR-020` -> magic/Arts/Effort.
- `SCR-021` -> authoritative dice result projection.
- `SCR-022` -> encounter/VTT projection.
- `SCR-023/024/025` -> factions/projects/living world.
- `SCR-026` -> append-only journal/history.
- `SCR-027` -> source evidence.
- `SCR-028` -> audio/TTS settings/session.
- `SCR-029` -> campaign option binding.
- `SCR-030..032` -> backup/import/recovery safe flows.
- `SCR-033/034` -> development-only Composer/geometry tools.

## 8. Visual implementation authority

For `SCR-015 ENCONTRO NO HEX / RUÍNAS DA VIGÍLIA`:
- viewport authority = 415×915 Forge geometry JSON;
- 27 in-bounds layers and z-order are preserved;
- only `EXPLORAR` is active in the approved bottom navigation state;
- packaged 697×1536 bitmap is reference-only and is never stretched into runtime geometry;
- runtime mutable text/state stays DOM/native;
- `VISUAL_LOCK` is impossible until a real runtime capture passes geometry + overlay + diff.

## 9. Android lifecycle contract

Required before release evidence can advance:
- cold start and first frame must not decode huge map assets synchronously;
- foreground/background recreation;
- WebView reload without losing committed campaign state;
- process death and reopen;
- IME open/close on 415×915 without trapping the primary action;
- clean install;
- app upgrade with migration;
- file import/export through Android document APIs;
- TTS STOP on navigation/session close;
- no campaign overwrite after bridge/read failure.

## 10. Architecture gate result

This document defines executable boundaries, but creates no runtime code. Wave 1 is not authorized by this document alone. See terminal gate.
