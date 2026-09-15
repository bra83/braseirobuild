export const LoadStatus = Object.freeze({
  OK: 'OK',
  NOT_FOUND: 'NOT_FOUND',
  CORRUPT: 'CORRUPT',
  IO_ERROR: 'IO_ERROR',
});

// Native bridge calls are synchronous from JavaScript's point of view. Keep a
// verified per-page state cache so ordinary rendering does not repeatedly hit
// Room, recompute SHA-256 and serialize the same campaign JSON.
export class NativePersistencePort {
  constructor(nativeObject = globalThis.BraseiroNative) {
    if (!nativeObject) throw new Error('Native persistence bridge unavailable');
    this.native = nativeObject;
    this.campaignCache = new Map();
  }

  capabilities() { return JSON.parse(this.native.capabilities()); }
  listCampaigns() { return JSON.parse(this.native.listCampaigns()); }

  invalidateCampaign(id) { if (id != null) this.campaignCache.delete(String(id)); }

  loadCampaign(id, { includeSnapshots = false, fresh = false } = {}) {
    const key = String(id);
    let loaded = !fresh ? this.campaignCache.get(key) : null;
    if (loaded) loaded = { ...loaded };
    else {
      loaded = JSON.parse(this.native.loadCampaign(key));
      if (loaded.status === 'OK') {
        const cacheable = { ...loaded };
        delete cacheable.snapshots;
        this.campaignCache.set(key, cacheable);
      }
    }
    // Snapshots are large and almost no player-facing screen needs them. Load
    // them only for backup/export callers that explicitly request them.
    if (includeSnapshots && loaded.status === 'OK' && typeof this.native.listSnapshots === 'function') {
      const snapshots = JSON.parse(this.native.listSnapshots(key));
      if (snapshots.status === 'OK') loaded.snapshots = snapshots.snapshots ?? [];
    }
    return loaded;
  }

  createCampaign(request) {
    const receipt = JSON.parse(this.native.createCampaign(JSON.stringify(request)));
    if (receipt.status === 'OK') this.invalidateCampaign(request.campaignId);
    return receipt;
  }

  commit(request) {
    const receipt = JSON.parse(this.native.commit(JSON.stringify(request)));
    if (receipt.status === 'OK') {
      const key = String(request.campaignId);
      const cached = this.campaignCache.get(key);
      if (cached && Number(cached.revision) === Number(request.expectedRevision)) {
        this.campaignCache.set(key, {
          ...cached,
          revision: receipt.newRevision,
          stateJson: request.newStateJson,
          checksum: receipt.stateChecksum ?? cached.checksum,
        });
      } else this.invalidateCampaign(key);
    }
    return receipt;
  }

  appendNarrative(request) { return JSON.parse(this.native.appendNarrative(JSON.stringify(request))); }
  getEvent(eventId) {
    if (typeof this.native.getEvent === 'function') return JSON.parse(this.native.getEvent(String(eventId)));
    const events = this.listEvents('');
    return { status: 'UNAVAILABLE', event: events.events?.find((e) => e.eventId === eventId) ?? null };
  }
  latestNarrative(campaignId, mechanicalEventId) {
    if (typeof this.native.latestNarrative === 'function') return JSON.parse(this.native.latestNarrative(String(campaignId), String(mechanicalEventId)));
    const events = this.listEvents(campaignId);
    const prefix = `narrative:${mechanicalEventId}`;
    const event = [...(events.events ?? [])].reverse().find((e) => e.eventKind === 'NARRATIVE' && String(e.eventId).startsWith(prefix)) ?? null;
    return event ? { status: 'OK', event } : { status: 'NOT_FOUND' };
  }
  listEvents(id) { return JSON.parse(this.native.listEvents(id)); }
  listSnapshots(id) { return typeof this.native.listSnapshots === 'function' ? JSON.parse(this.native.listSnapshots(id)) : { status: 'UNAVAILABLE', snapshots: [] }; }
}

export class MemoryPersistenceAdapter {
  constructor() { this.rows = new Map(); this.failStage = null; }

  createCampaign({ campaignId, displayName, stateJson, masterSeed, rngAlgorithm, initialSnapshotId }) {
    if (this.rows.has(campaignId)) throw new Error('Campaign already exists');
    const row = { campaignId, displayName, revision: 0, stateJson, masterSeed, rngAlgorithm, events: [], snapshots: [initialSnapshotId] };
    this.rows.set(campaignId, structuredClone(row));
    return { status: 'OK', campaignId, previousRevision: -1, newRevision: 0, eventId: 'campaign-created', snapshotId: initialSnapshotId };
  }

  loadCampaign(campaignId, { includeSnapshots = true } = {}) {
    const row = this.rows.get(campaignId);
    if (!row) return { status: LoadStatus.NOT_FOUND };
    if (row.__corrupt) return { status: LoadStatus.CORRUPT, reason: row.__corrupt };
    const out = { status: LoadStatus.OK, ...structuredClone(row) };
    if (!includeSnapshots) delete out.snapshots;
    return out;
  }

  commit(request) {
    const current = this.rows.get(request.campaignId);
    if (!current) throw new Error('Campaign not found');
    if (current.revision !== request.expectedRevision) {
      return { status: 'REVISION_CONFLICT', expected: request.expectedRevision, actual: current.revision };
    }
    const staged = structuredClone(current);
    const newRevision = current.revision + 1;
    staged.events.push({ eventId: request.eventId, revision: newRevision, eventKind: request.eventKind, payloadJson: request.eventPayloadJson });
    if (this.failStage === 'after-event') throw new Error('Injected transaction failure');
    staged.stateJson = request.newStateJson;
    staged.revision = newRevision;
    if (request.snapshotId) staged.snapshots.push(request.snapshotId);
    if (this.failStage === 'before-commit') throw new Error('Injected transaction failure');
    this.rows.set(request.campaignId, staged);
    return { status: 'OK', campaignId: request.campaignId, previousRevision: request.expectedRevision, newRevision, eventId: request.eventId, snapshotId: request.snapshotId ?? null };
  }

  appendNarrative(request) {
    const current = this.rows.get(request.campaignId);
    if (!current) throw new Error('Campaign not found');
    const existing = current.events.find((event) => event.eventId === request.eventId);
    if (existing) return { status: 'OK', campaignId: request.campaignId, revision: existing.revision, eventId: existing.eventId, idempotent: true };
    const parent = current.events.find((event) => event.eventId === request.mechanicalEventId);
    if (!parent) return { status: 'PARENT_MECHANICAL_EVENT_NOT_FOUND' };
    if (parent.revision !== request.mechanicalRevision) return { status: 'PARENT_REVISION_MISMATCH', actual: parent.revision };
    const staged = structuredClone(current);
    staged.events.push({
      eventId: request.eventId,
      revision: request.mechanicalRevision,
      eventKind: 'NARRATIVE',
      payloadJson: request.eventPayloadJson,
      parentMechanicalEventId: request.mechanicalEventId,
    });
    this.rows.set(request.campaignId, staged);
    return { status: 'OK', campaignId: request.campaignId, revision: request.mechanicalRevision, eventId: request.eventId, idempotent: false };
  }

  getEvent(eventId) {
    for (const current of this.rows.values()) { const event = current.events.find((e) => e.eventId === eventId); if (event) return { status: 'OK', event: structuredClone(event) }; }
    return { status: 'NOT_FOUND' };
  }

  latestNarrative(campaignId, mechanicalEventId) {
    const current = this.rows.get(campaignId);
    if (!current) return { status: LoadStatus.NOT_FOUND };
    const prefix = `narrative:${mechanicalEventId}`;
    const event = [...current.events].reverse().find((e) => e.eventKind === 'NARRATIVE' && String(e.eventId).startsWith(prefix));
    return event ? { status: 'OK', event: structuredClone(event) } : { status: 'NOT_FOUND' };
  }

  listEvents(campaignId) {
    const current = this.rows.get(campaignId);
    if (!current) return { status: LoadStatus.NOT_FOUND, events: [] };
    return { status: 'OK', events: structuredClone(current.events) };
  }

  listSnapshots(campaignId) {
    const current = this.rows.get(campaignId);
    if (!current) return { status: LoadStatus.NOT_FOUND, snapshots: [] };
    return { status: 'OK', snapshots: structuredClone(current.snapshots ?? []) };
  }

  corruptCampaignForTest(campaignId, reason = 'checksum mismatch') {
    const row = this.rows.get(campaignId);
    if (!row) throw new Error('Campaign not found');
    row.__corrupt = reason;
  }
}
