# BRASEIRO WWN — M4 CANONICAL STATE MODEL

STATUS: SCHEMA_ARCHITECTURE_DEFINED / PHYSICAL_SCHEMA_NOT_IMPLEMENTED  
DB_SCHEMA_VERSION: RESERVED_FOR_M5

## 1. Canonical aggregate

`CampaignAggregate`

- `campaignId`
- `schemaVersion`
- `appDataVersion`
- `revision` monotonic integer
- `profile` (STANDARD + explicitly enabled options/modules)
- `authorityBindings` (source/rule profile versions)
- `rngState` (algorithm version, master seed, stream counters)
- `clock`
- `party`
- `characters{}`
- `inventory{}`
- `magicState{}`
- `atlas`
- `sites{}`
- `encounters{}`
- `npcs{}`
- `factions{}`
- `majorProjects{}`
- `missions{}`
- `worldEvents{}`
- `audioPrefsRef`
- `sourceBindings`
- `lastCommittedEventId`
- `createdAt/updatedAt` (audit metadata only)

## 2. State ownership and invariants

### Campaign revision
Every state mutation increments revision exactly once per atomic mechanical commit. Narrative-only append events may reference the same mechanical revision.

### Characters
Persistent entity IDs are stable across scenes, Atlas and VTT. HP, System Strain, Effort commitment, conditions, equipment and progression never live only in screen state.

### Position
`party.position` references one canonical Atlas/site/encounter coordinate context. Transitions are explicit events:
- world hex -> site entrance;
- site -> encounter;
- encounter -> site/world return.
No screen may maintain an uncommitted alternate position as authoritative state.

### Time
`clock` stores canonical campaign time plus calendar/profile metadata. UI clocks are projections. Real wall-clock time never advances the campaign by itself.

### NPC knowledge
Each NPC has:
- `knownFacts[]`
- `memoryRefs[]`
- `relationshipRefs[]`
- `locationRef`
- `currentActivity`
- `goalRefs[]`
- `lastWorldTick`
Barbara receives a filtered NPC context built from these fields plus current observation, never full campaign truth.

### Discovery/fog
Atlas stores discovery by coordinate/POI IDs. Player-known map state is explicit and separate from canonical hidden world facts.

## 3. Event model

`CampaignEvent`
- `eventId`
- `campaignId`
- `revision`
- `eventType`
- `actorId?`
- `intentId?`
- `ruleIds[]`
- `sourceEvidenceIds[]`
- `rngRecords[]`
- `deltaSummary`
- `canonicalTimeBefore/After`
- `positionBefore/After?`
- `checksumAfter`
- `createdAtAudit`

Event categories:
- mechanical action
- time advance
- travel/exploration
- encounter/combat
- inventory/crafting
- magic/Effort
- character progression
- NPC/world/faction tick
- option/profile change
- import/migration/recovery
- narrative append (non-mechanical)

## 4. Snapshot policy

Snapshots are accelerators and recovery anchors, not separate truth.

Create a snapshot:
- at campaign creation;
- at clean close when state changed;
- after a configurable event interval (initial design target: 25 commits);
- before and after migration;
- before risky import/restore;
- at major scene/session boundaries when cheap.

Each snapshot records:
- revision;
- state JSON/blob;
- checksum;
- schema version;
- RNG version/counters;
- last event ID.

On load:
1. read metadata;
2. verify latest snapshot checksum;
3. replay later events if needed;
4. verify final checksum/revision;
5. only then expose state to UI.

If any read/verification step fails, return a recovery state. Never replace with a newly-created empty campaign.

## 5. Proposed physical persistence (M5 target)

Room/SQLite tables, subject to M5 implementation audit:
- `campaign_meta`
- `campaign_state`
- `campaign_event`
- `campaign_snapshot`
- `source_catalog`
- `source_chunk`
- `source_fts`
- `asset_catalog`
- `backup_record`
- `app_meta`

`campaign_state` may store the aggregate as versioned JSON while indexed tables contain search/source metadata. This keeps Kotlin free of duplicated RPG business rules while retaining transactional safety.

## 6. Atomic commit transaction

Inside one SQLite transaction:
1. verify `expectedRevision`;
2. insert mechanical event;
3. write new aggregate state at `revision+1`;
4. optionally write snapshot;
5. update campaign metadata/checksum;
6. commit.

If any write fails, rollback all five.

Narrative generation happens after this commit. A later narrative-event append cannot change mechanical state.

## 7. Backup format architecture

Portable extension: `.braseiro-wwn` ZIP.

Logical contents:
- `manifest.json`
- `campaign.json`
- `events.ndjson`
- `snapshots/index.json` plus selected snapshot payloads
- `source_bindings.json` (references/metadata, not automatically full protected books)
- `asset_manifest.json` (references + provenance)
- `checksums.sha256`

Import flow:
1. unpack to temporary area;
2. verify checksums/schema/version;
3. import into clean temporary database;
4. load and compare canonical aggregate/checksum;
5. run migration if required;
6. only after verification allow user-confirmed replace/clone;
7. keep pre-import recovery snapshot until next clean close.

## 8. Migration contract

- Every schema change has `from -> to` migration code and fixture.
- No destructive migration in release builds without explicit user export/confirmation.
- Generator/RNG algorithm versions migrate independently from DB schema.
- Rule-profile changes are campaign option migrations, not silent application defaults.
- A failed migration leaves the original database untouched and enters Recovery UI.

## 9. Save/close/reopen invariants

- committed action survives app kill immediately after receipt;
- uncommitted UI edits do not masquerade as committed game state;
- reopen resumes the latest verified revision;
- campaign list shows corruption/recovery status instead of hiding or replacing a bad save;
- backup and restore compare canonical checksums, not only file existence.
