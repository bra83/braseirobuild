# BRASEIRO WWN — M4 MODULE CONTRACTS

STATUS: DEFINED_PENDING_GUIDE_REAUDIT

## Module map

### `ui-shell`
**Owns:** DOM composition, navigation, 415×915 responsive shell, input collection, immutable view models.  
**May call:** application services only.  
**Must not:** calculate WWN legality, roll mechanics, mutate persistence directly.

### `asset-resolver`
**Owns:** asset catalog lookup, provenance class, crop/clip metadata, placeholder policy, cache-safe URLs.  
**Input:** stable asset/provenance IDs.  
**Output:** render-safe asset handle.  
**Must not:** silently substitute protected/source-only assets for reusable assets.

### `visual-geometry`
**Owns:** M3B geometry schema, anchors, layers, proportional scaling, interaction regions, Composer import/export validation.  
**Hard rule:** approved exact geometry is data, not a suggestion.

### `action-service`
**Owns:** `IntentEnvelope -> ResolutionEnvelope -> commit` orchestration.  
**Must not:** contain WWN rule constants.  
**Failure policy:** no partial state mutation.

### `rules-wwn`
**Owns:** normalized WWN mechanics and mechanical invariants traced to RULE_ID/source evidence.  
**Input:** typed state + intent + rule profile.  
**Output:** legal result, required rolls, derived values, state delta proposal.  
**Must not:** import OSE/D&D/Pathfinder/SWN/Godbound fallback values.

### `rng-core`
**Owns:** seeded streams, dice primitives, raw roll records, deterministic vectors.  
**Must not:** expose untracked random draws to mechanics.

### `campaign-state`
**Owns:** canonical in-memory aggregate and revision semantics.  
**Mutation:** only through validated commits.  
**Read failure:** distinct from empty/not-found.

### `persistence-port`
**Owns contract:** load, atomic commit, snapshot, export, import-to-temp, migrate, verify.  
**Production adapter:** Android Room/SQLite bridge.  
**Browser adapter:** test-only.

### `journal`
**Owns:** append-only mechanical/narrative history linked to event IDs and state revisions.  
**Must not:** become a second source of mutable campaign truth.

### `character-domain`
**Owns:** character legality, derived sheet read model, advancement hooks.  
**Delegates mechanics:** `rules-wwn`.

### `inventory-domain`
**Owns:** item instances, Readied/Stowed, provenance, currency, crafting transaction requests.  
**Delegates encumbrance/crafting legality:** `rules-wwn`.

### `magic-domain`
**Owns:** spellbook/prepared/casting/Art/Effort state projection.  
**Delegates restrictions and costs:** `rules-wwn`.

### `world-clock`
**Owns:** canonical campaign time, elapsed-time commits, day/night and scheduled event boundaries.  
**Time advances only:** through a committed action/procedure.

### `atlas-domain`
**Owns:** party position, hex graph, POIs, discovery/fog, route state, map annotations.  
**Must not:** resolve travel mechanics itself; it requests travel resolution from `rules-wwn`.

### `wilderness-domain`
**Owns:** exploration procedure state, route interruption, camp/forage request context, discovery records.  
**Uses:** atlas, world-clock, rules, rng.

### `dungeon-domain`
**Owns:** site position, dungeon-turn state, discovered map, light/resource clocks, encounter procedure context.  
**Uses:** world-clock, rules, rng.

### `encounter-domain`
**Owns:** encounter ID, participants, real entity references, initiative, positions/ranges, round/turn state.  
**VTT:** projection only.  
**Combat calculations:** `rules-wwn`.

### `npc-world`
**Owns per NPC:** identity, stats ref, location, goals, disposition, memories, knowledge boundaries, current activity, status.  
**Hard rule:** prompt context may only contain knowledge the NPC can know.

### `living-world`
**Owns:** deterministic/off-screen scheduling policy for NPC/faction/event updates triggered by canonical time changes.  
**Must not:** run uncontrolled real-time mutation while the campaign is closed.

### `faction-domain`
**Owns:** faction aggregate, assets, goals, turn scheduling and state.  
**Mechanical action resolution:** `rules-wwn`.

### `project-domain`
**Owns:** Renown/Major Project aggregate when enabled by campaign options.  
**Hard rule:** optional module OFF means no hidden progression.

### `source-index`
**Owns:** source catalog, namespaces, locators, chunks/FTS metadata, authority ranking, missing-evidence results.  
**Must not:** invent page/locator values.

### `barbara-orchestrator`
**Owns:** context assembly, tool/service routing, narrative prompt construction, response parsing.  
**Does not own:** rules, persistence, time, position, NPC memory or RNG.

### `gm-help`
**Owns:** query-only assistant workflow.  
**Capability token:** read-only; no mutation services are injected.

### `model-adapter`
**Owns:** provider request/response transport and configured model metadata.  
**Secrets:** native secure store; not exported with campaign.

### `audio-session`
**Owns:** TTS queue, stop/pause state, optional ambience hooks.  
**Narration authority:** only committed/visible narrative text may enter TTS by default.

### `backup-recovery`
**Owns:** logical export manifest, checksum, import-to-temp, canonical comparison, final atomic replace/merge.  
**Hard rule:** never overwrite current campaign before verification.

### `android-bridge`
**Owns:** persistence RPC, file APIs, secure settings, TTS/audio, lifecycle notifications.  
**Must not:** implement RPG rules.

### `capture-harness`
**Owns:** deterministic 415×915 fixtures, screenshots, dynamic masks, overlay/diff reports and golden registry.

## Typed envelopes

### IntentEnvelope
- `intentId`
- `campaignId`
- `expectedRevision`
- `actorEntityId`
- `intentType`
- `payload`
- `uiSource`
- `clientTimestamp` (audit only, never mechanical time)

### ResolutionEnvelope
- `intentId`
- `ruleIds[]`
- `sourceEvidenceIds[]`
- `rngRecords[]`
- `mechanicalOutcome`
- `stateDelta`
- `timeDelta`
- `positionDelta`
- `encounterDelta`
- `journalMechanicalEntry`

### CommitReceipt
- `campaignId`
- `previousRevision`
- `newRevision`
- `eventId`
- `stateChecksum`
- `snapshotId?`

## Forbidden dependency edges

- UI -> SQLite direct: forbidden.
- UI -> model provider direct: forbidden.
- Barbara -> persistence mutation direct: forbidden.
- Barbara -> RNG direct: forbidden.
- VTT token widget -> character HP direct mutation: forbidden.
- Audio callback -> campaign time mutation: forbidden.
- GM_HELP -> action-service commit: forbidden.
- Any module -> `Math.random()` for mechanical/procedural state: forbidden.
