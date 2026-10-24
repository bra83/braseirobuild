/**
 * BRASEIRO OSE — VTT Life Adapter (Motor de Vida V2.2)
 *
 * Causal, deterministic bridge between OSE session events and Motor de Vida V2.2.
 * Synchronizes with BraseiroOseDatabase Room storage via 'LifeStoreSync' / 'LifeStoreQuery'.
 *
 * Idempotency invariant:
 * - Every reaction receipt and calendar day advance is processed exactly once by receiptId/eventId.
 * - Rollback or replay of existing receipts never causes duplicate matrix draws or RNG mutation.
 * - Public player presentation displays rumors and personal arcs without leaking GM secrets.
 */
(function(root) {
  'use strict';

  function createInitialStore(system, worldId, timelineId) {
    return {
      schema: 'braseiro.life/v2',
      revision: 0,
      day: 0,
      rng: 1337,
      legacy: {
        schema: 'braseiro.legacy/v1',
        system: system || 'ose-classic',
        worldId: worldId || 'world',
        timelineId: timelineId || 'canonical',
        revision: 0,
        characters: {},
        worldEvents: [],
        adventures: {},
        processedEventIds: []
      },
      entities: {},
      facts: {},
      bindings: {},
      knowledge: [],
      events: [],
      receipts: {},
      rules: {},
      offers: {},
      history: [],
      rolls: [],
      uses: {},
      budget: { day: 0, used: 0, max: 3 },
      matrixHistory: [],
      matrixProfile: {
        packId: system === 'ose-advanced' ? 'ose-advanced' : 'ose-classic',
        system: system || 'ose-classic',
        mode: 'standard'
      },
      lifeArcs: {}
    };
  }

  class VttLifeAdapter {
    constructor(options = {}) {
      this.postNative = options.postNative || (typeof postNative === 'function' ? postNative : null);
      this.store = null;
      this.scope = options.scope || 'default';
      this.lastProcessedTurn = -1;
      this.lastProcessedDay = -1;
      this.processedReceipts = new Set();
    }

    initFromViewState(viewState) {
      if (!viewState) return;
      const sys = (viewState.campaign?.profile || '').toLowerCase().includes('advanced') ? 'ose-advanced' : 'ose-classic';
      const worldId = viewState.campaign?.id || 'world';
      this.scope = viewState.campaign?.id || 'default';

      if (viewState.lifeStore && viewState.lifeStore.schema === 'braseiro.life/v2') {
        this.store = JSON.parse(JSON.stringify(viewState.lifeStore));
      } else if (!this.store) {
        this.store = createInitialStore(sys, worldId, 'canonical');
      }

      // Rehydrate processed receipts set from legacy
      if (this.store.legacy?.processedEventIds) {
        for (const id of this.store.legacy.processedEventIds) {
          this.processedReceipts.add(id);
        }
      }

      this.syncSessionEvents(viewState);
    }

    syncSessionEvents(viewState) {
      if (!this.store || !viewState) return;
      const session = viewState.session || {};
      const receipts = session.reactionReceipts || {};
      const tm = viewState.time || {};
      const currentDay = Number(tm.days || 0);
      const currentTurn = Number(tm.turns || 0);

      let mutated = false;

      // 1. Process calendar / day advancement
      if (currentDay > this.store.day) {
        this.store.day = currentDay;
        this.store.revision++;
        this.store.budget.day = currentDay;
        this.store.budget.used = 0;
        mutated = true;
      }

      // 2. Process newly confirmed receipts (idempotent: exactly once per reactionId)
      for (const [receiptId, receipt] of Object.entries(receipts)) {
        if (this.processedReceipts.has(receiptId)) continue;

        this.processedReceipts.add(receiptId);
        this.store.legacy.processedEventIds.push(receiptId);

        const actorId = receipt.actorCharacterId || (viewState.party && viewState.party[0]?.id) || 'party';
        const summary = receipt.reactionText || receipt.mechanicalIntent || 'Ação da companhia';

        const evt = {
          id: receiptId,
          day: currentDay,
          turn: currentTurn,
          summary: summary,
          actorIds: [actorId],
          visibility: 'private'
        };

        this.store.events.push(evt);
        this.store.revision++;
        mutated = true;

        // Generate causal off-screen rumor / life manifestation
        this.generateLifeManifestation(evt, viewState);
      }

      if (mutated) {
        this.persist();
      }
    }

    generateLifeManifestation(originEvent, viewState) {
      if (this.store.budget.used >= this.store.budget.max) return;

      const packId = this.store.matrixProfile?.packId || 'ose-classic';
      const actorId = originEvent.actorIds[0] || 'party';
      const drawId = `draw-${originEvent.id}-${this.store.revision}`;

      // Canonical deterministic PRNG step
      this.store.rng = (this.store.rng * 1664525 + 1013904223) >>> 0;
      const seed = this.store.rng;

      const situationIndex = (seed % 100) + 1;
      const rumorId = `rumor-${originEvent.id}`;

      const rumor = {
        id: rumorId,
        situationId: `sit-${situationIndex}`,
        originEventId: originEvent.id,
        characterId: actorId,
        packId: packId,
        day: this.store.day,
        status: 'active',
        text: `Rumor no Dia ${this.store.day + 1}: Notícias e repercussões sobre "${originEvent.summary}" chegam aos ouvidos dos viajantes nas tavernas locais.`
      };

      this.store.entities[rumorId] = {
        id: rumorId,
        kind: 'information',
        title: `Notícia de ${originEvent.summary.slice(0, 30)}`,
        visibility: 'public',
        status: 'active',
        actorIds: [actorId],
        data: rumor
      };

      this.store.budget.used++;
      this.store.matrixHistory.push({
        drawId: drawId,
        packId: packId,
        originEventId: originEvent.id,
        day: this.store.day,
        seed: seed,
        result: { status: 'proposal', situationId: `sit-${situationIndex}`, rumorId: rumorId }
      });
    }

    persist() {
      if (!this.store) return;
      if (typeof this.postNative === 'function') {
        this.postNative('LifeStoreSync', {
          scope: this.scope,
          stateJson: JSON.stringify(this.store),
          revision: this.store.revision,
          day: this.store.day
        });
      }
    }

    getPublicRumors() {
      if (!this.store) return [];
      return Object.values(this.store.entities || {})
        .filter(e => e.kind === 'information' && e.status === 'active')
        .map(e => e.data || { text: e.title });
    }

    getActiveLifeArcs() {
      if (!this.store) return [];
      return Object.values(this.store.lifeArcs || {})
        .filter(a => a.status === 'active' || a.status === 'candidate');
    }
  }

  // Export to global scope
  const adapter = new VttLifeAdapter();
  root.VttLifeAdapter = VttLifeAdapter;
  root.vttLifeAdapter = adapter;

})(typeof window !== 'undefined' ? window : globalThis);
