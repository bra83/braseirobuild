/**
 * Universal Adventure Engine — Runtime Execution & State Graph (v2)
 * Braseiro Star Wars: Edge of the Galaxy VTT
 */

import {
  validateAdventureModule,
  createInitialAdventureState,
  createCanonicalEvent,
  migrateAdventureState,
  ADVENTURE_SCHEMA_VERSION
} from './adventureSchema.mjs';
import { validateAdventureCheckpoint } from './adventureCheckpointGate.mjs';
import { validateObjectiveDependencies } from './adventureSemanticGate.mjs';

function clone(v) {
  return structuredClone(v);
}

// Only UAE methods may originate reserved engine journal events. External
// adapters must use their own source and cannot impersonate objective or
// consequence state transitions by calling dispatchEvent directly.
const ENGINE_EVENT_TOKEN = Symbol('uae-engine-event');
const CONTROLLER_PROOF_TOKEN = Symbol('uae-published-controller-proof');
// P169: authored consequence rewards require an actual fired source trigger.
const AUTHORED_CONSEQUENCE_TOKEN = Symbol('uae-authored-consequence');
const ENGINE_EVENT_TYPES = new Set([
  'TRIGGER_FIRED', 'TRIGGER_CASCADE_LIMIT', 'CONSEQUENCE_RECORDED',
  'CONSEQUENCE_RESOLVED', 'OBJECTIVE_COMPLETED', 'OBJECTIVE_FAILED',
  'ADVENTURE_ACTION_COMMITTED'
]);

// Imported adventures and save files are untrusted input. Never allow a key
// to mutate the prototype of the runtime's ordinary JS dictionaries.
function safeStateKey(key, label = 'Identificador') {
  if (typeof key !== 'string' || !key.trim() ||
      key === '__proto__' || key === 'constructor' || key === 'prototype') {
    throw new Error(`${label} ${label.startsWith('Variação') ? 'inválida' : 'inválido'}.`);
  }
  return key;
}

// Snapshots are serialized as JSON. Reject values that structuredClone accepts
// but JSON would silently erase (NaN, Infinity, Map, cycles, etc.). The same
// guard protects imported trigger payloads and mutable flag values.
export function assertPersistable(value, label = 'Estado', ancestors = new Set(), depth = 0) {
  if (depth > 40) throw new Error(`${label} excede a profundidade permitida.`);
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return;
  if (typeof value === 'number' && Number.isFinite(value)) return;
  if (typeof value !== 'object' || (Object.getPrototypeOf(value) !== Object.prototype &&
      Object.getPrototypeOf(value) !== null && !Array.isArray(value)))
    throw new Error(`${label} contém valor não persistível.`);
  if (ancestors.has(value)) throw new Error(`${label} contém referência circular.`);
  // JSON serialization ignores symbols, non-enumerable properties and array
  // custom keys; getters/toJSON can alter a snapshot during serialization.
  // Reject all of these before any state mutation is committed.
  const isArray = Array.isArray(value);
  for (const key of Reflect.ownKeys(value)) {
    if (isArray && key === 'length') continue;
    if (typeof key !== 'string' || (isArray && !/^(0|[1-9]\d*)$/.test(key)))
      throw new Error(`${label} contém propriedade não persistível.`);
    if (!isArray) safeStateKey(key, 'Chave persistida');
    const descriptor = Object.getOwnPropertyDescriptor(value, key);
    if (!descriptor?.enumerable || !Object.hasOwn(descriptor, 'value'))
      throw new Error(`${label} contém propriedade não persistível.`);
  }
  ancestors.add(value);
  if (isArray) {
    for (let i = 0; i < value.length; i++) {
      if (!Object.hasOwn(value, i)) throw new Error(`${label} contém lacuna em lista.`);
      assertPersistable(value[i], label, ancestors, depth + 1);
    }
  } else {
    for (const [key, entry] of Object.entries(value)) {
      assertPersistable(entry, label, ancestors, depth + 1);
    }
  }
  ancestors.delete(value);
}

// Stable JSON fingerprint: a retried Android command must be identical even
// when the JS object key insertion order differs after save/reopen.
function canonicalJson(value) {
  if (Array.isArray(value)) return `[${value.map(canonicalJson).join(',')}]`;
  if (value && typeof value === 'object') return `{${Object.keys(value).sort().map(k => `${JSON.stringify(k)}:${canonicalJson(value[k])}`).join(',')}}`;
  return JSON.stringify(value);
}

function checkedIdempotencyKey(value) {
  if (value === undefined || value === null) return null;
  if (typeof value !== 'string' || !/^[a-zA-Z0-9][a-zA-Z0-9_.:/-]{0,127}$/.test(value))
    throw new Error('Chave de idempotência UAE inválida.');
  return value;
}

function finiteNumber(value, label) {
  // Numeric strings from authored JSON are accepted for compatibility, but
  // booleans, arrays, objects, whitespace and hex notation are not numbers.
  const numericString = typeof value === 'string' &&
    /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(value.trim());
  if (!(typeof value === 'number' || numericString) || !Number.isFinite(Number(value)))
    throw new Error(`${label} ${label.startsWith('Variação') ? 'inválida' : 'inválido'}.`);
  return Number(value);
}

function finiteResult(value, label) {
  if (!Number.isFinite(value)) throw new Error(`${label} fora dos limites numéricos.`);
  return value;
}

export class AdventureRuntime {
  constructor(module, state = null) {
    validateAdventureModule(module);
    validateObjectiveDependencies(module);
    this.module = clone(module);
    // Do not let the migration layer normalize a non-JSON input into a
    // seemingly valid checkpoint. Validate the raw import first.
    if (state !== null) {
      assertPersistable(state, 'Checkpoint recebido');
      // v2 is already canonical: never let migration silently repair corrupt
      // counters, clocks, journal sequences or objective progress.
      if (state.schemaVersion === ADVENTURE_SCHEMA_VERSION) validateAdventureCheckpoint(this.module, state);
    }
    this.state = state ? migrateAdventureState(clone(state), this.module) : createInitialAdventureState(this.module);
    this._ensureIntegrity();
    assertPersistable(this.state, 'Checkpoint UAE');
    validateAdventureCheckpoint(this.module, this.state);
    this._processingEvents = false;
    this._pendingEvents = [];
    this._atomicDepth = 0;
    this._strictCascadeDepth = 0;
  }

  _ensureIntegrity() {
    if (!this.state.counters) this.state.counters = {};
    if (!this.state.triggersFired) this.state.triggersFired = {};
    if (!this.state.consequences) this.state.consequences = [];
    if (!this.state.objectives) this.state.objectives = {};
    if (!this.state.flags) this.state.flags = {};
    if (!this.state.clocks) this.state.clocks = {};
    if (!this.state.visitedNodeIds) this.state.visitedNodeIds = [this.state.currentNodeId || this.module.startNodeId];
    if (!this.state.canonicalEvents) this.state.canonicalEvents = [];
    if (!this.state.discoveredClues) this.state.discoveredClues = [];
    if (!this.state.revealedSecrets) this.state.revealedSecrets = [];
    if (!Array.isArray(this.state.completedObjectives)) this.state.completedObjectives = [];
    if (!Array.isArray(this.state.failedObjectives)) this.state.failedObjectives = [];
    if (!Array.isArray(this.state.canonicalEvents)) this.state.canonicalEvents = [];
    const prefix = `evt_${this.module.id}_`;
    const maxId = this.state.canonicalEvents.reduce((highest, event) => {
      const id = String(event?.id ?? '');
      const suffix = id.startsWith(prefix) ? id.slice(prefix.length) : '';
      return /^\d+$/.test(suffix) ? Math.max(highest, Number(suffix)) : highest;
    }, 0);
    this.state.eventSequence = Math.max(0, Math.floor(Number(this.state.eventSequence) || 0), this.state.canonicalEvents.length, maxId);
  }

  _emitEvent(type, payload = {}, source = 'runtime', idempotencyKey = null) {
    if (!Number.isSafeInteger(this.state.eventSequence) || this.state.eventSequence >= Number.MAX_SAFE_INTEGER)
      throw new Error('Sequência de eventos esgotada.');
    const sequence = ++this.state.eventSequence;
    const event = createCanonicalEvent({
      id: `evt_${this.module.id}_${sequence}`, type, source, payload, idempotencyKey
    });
    this.state.canonicalEvents.push(event);
    this.state.updatedAt = event.timestamp;
    return event;
  }

  _atomic(operation) {
    const before = clone(this.state);
    const pending = [...this._pendingEvents];
    const processing = this._processingEvents;
    const outermost = this._atomicDepth === 0;
    this._atomicDepth++;
    try {
      // Upgrade P141 saves without rewriting historical journal entries. Every
      // event emitted from this transaction onward must carry an exact cause.
      if (outermost && !Object.hasOwn(this.state, 'causalAuditFromSequence'))
        this.state.causalAuditFromSequence = this.state.eventSequence + 1;
      // P168 checkpoints lack consequence provenance. Keep their old journal,
      // but every new consequence must be causally proven from this commit.
      if (outermost && !Object.hasOwn(this.state, 'consequenceProvenanceFromSequence'))
        this.state.consequenceProvenanceFromSequence = this.state.eventSequence + 1;
      const result = operation();
      // The engine is synchronous. A Promise here would commit a checkpoint
      // before later mutations, outside rollback and causality validation.
      if (result && typeof result.then === 'function')
        throw new Error('Transação UAE assíncrona não é suportada.');
      // Validate only the outermost commit: inner calls can temporarily hold
      // intermediate state while a transition updates several ledgers.
      if (outermost) {
        assertPersistable(this.state, 'Commit UAE');
        validateAdventureCheckpoint(this.module, this.state);
      }
      return result;
    } catch (error) {
      this.state = before;
      this._pendingEvents = pending;
      this._processingEvents = processing;
      throw error;
    } finally {
      this._atomicDepth--;
    }
  }

  // Compose a multi-step adapter action as one all-or-nothing checkpoint.
  // This API is also used by integrations that advance multiple clocks and
  // objectives in the same narrative turn.
  transaction(operation) {
    if (typeof operation !== 'function') throw new Error('Transação UAE inválida.');
    if (operation.constructor?.name === 'AsyncFunction')
      throw new Error('Transação UAE assíncrona não é suportada.');
    this._strictCascadeDepth++;
    try { return this._atomic(() => operation(this)); }
    finally { this._strictCascadeDepth--; }
  }

  #dispatchEngineEvent(eventType, payload, options = {}) {
    return this.dispatchEvent(eventType, payload, 'engine', { ...options, engineToken: ENGINE_EVENT_TOKEN });
  }

  /**
   * P143 — one durable, replay-safe adventure action. The command receipt is
   * written in the SAME atomic transaction as all trigger cascades, clocks,
   * objectives and consequences. Retrying after an Android WebView restart
   * returns the prior receipt without applying any command twice.
   *
   * A receipt is not an authorization to bypass source-specific encounter
   * rules: the published adventure adapter still validates player choices.
   */
  commitAction(actionId, commands) {
    const key = checkedIdempotencyKey(actionId);
    if (key === null) throw new Error('Ação UAE exige identificador de idempotência.');
    if (!Array.isArray(commands) || commands.length < 1 || commands.length > 32)
      throw new Error('Ação UAE exige entre 1 e 32 comandos.');
    assertPersistable(commands, 'Comandos de aventura');
    const encoded = canonicalJson(commands);
    if (encoded.length > 32768) throw new Error('Ação UAE excede o limite de tamanho.');
    if (this._processingEvents) throw new Error('Ação UAE não pode iniciar dentro de cascata de gatilhos.');
    // Search only engine-authenticated action receipts; an unrelated external
    // event with the same key is never sufficient to skip an action.
    const previous = this.state.canonicalEvents.find(e => e.type === 'ADVENTURE_ACTION_COMMITTED' &&
      e.source === 'engine' && e.idempotencyKey === key);
    if (previous) {
      if (previous.payload.actionId !== key || canonicalJson(previous.payload.commands) !== encoded)
        throw new Error('Identificador de ação reutilizado com comandos diferentes.');
      return { applied: false, receiptId: previous.id, state: this.getState() };
    }
    return this.transaction(() => {
      const firstSequence = this.state.eventSequence + 1;
      for (const command of commands) {
        if (!command || typeof command !== 'object' || Array.isArray(command))
          throw new Error('Comando de aventura inválido.');
        switch (command.type) {
          case 'event':
            if (command.source === 'engine') throw new Error('Origem interna UAE reservada ao motor.');
            this.dispatchEvent(command.eventType, command.payload ?? {}, command.source ?? 'adapter-action');
            break;
          case 'effects':
            if (!command.effects || typeof command.effects !== 'object')
              throw new Error('Efeitos de ação UAE inválidos.');
            this.applyEffects(command.effects);
            break;
          case 'advance-clock':
            this.advanceClock(command.clockId, command.delta ?? 1);
            break;
          case 'progress-objective':
            if (!Object.hasOwn(this.state.objectives, command.objectiveId))
              throw new Error('Objetivo UAE desconhecido no comando.');
            this.updateObjectiveProgress(command.objectiveId, command.delta ?? 1);
            break;
          case 'complete-objective':
          case 'fail-objective':
            if (!Object.hasOwn(this.state.objectives, command.objectiveId))
              throw new Error('Objetivo UAE desconhecido no comando.');
            if (command.type === 'complete-objective') this.completeObjective(command.objectiveId);
            else this.failObjective(command.objectiveId);
            break;
          case 'record-consequence':
            this.recordConsequence(command.consequence);
            break;
          case 'resolve-consequence':
            if (!this.state.consequences.some(c => c.id === command.consequenceId))
              throw new Error('Consequência UAE desconhecida no comando.');
            this.resolveConsequence(command.consequenceId);
            break;
          case 'transition':
            this.transition(command.choiceId);
            break;
          default:
            throw new Error(`Comando de aventura UAE não suportado: ${String(command.type)}`);
        }
      }
      const lastSequence = this.state.eventSequence;
      const appliedEventIds = this.state.canonicalEvents
        .filter(e => Number(e.id.slice(`evt_${this.module.id}_`.length)) >= firstSequence)
        .map(e => e.id);
      this.#dispatchEngineEvent('ADVENTURE_ACTION_COMMITTED', {
        actionId: key, commands: clone(commands), firstSequence, lastSequence, appliedEventIds
      }, { idempotencyKey: key });
      const receipt = this.state.canonicalEvents.find(e =>
        e.type === 'ADVENTURE_ACTION_COMMITTED' && e.idempotencyKey === key && e.source === 'engine');
      return { applied: true, receiptId: receipt.id, state: this.getState() };
    });
  }

  /** P150 — only the published controller can originate a durable proof.
   * Generic adapter commands may not impersonate the controller's receipt
   * source, even if they happen to know an actionId from a save file. */
  recordPublishedCommandProof({ actionId, kind, adventureId, proofVersion,
      request, receiptSequence, runKey } = {}) {
    const key = checkedIdempotencyKey(actionId);
    if (!key || !['start','choice','check','encounter','finish-combat','imperial-clock'].includes(kind) ||
        adventureId !== this.module.id || proofVersion !== 2 ||
        typeof request !== 'string' || !request || request.length > 32768 ||
        !Number.isSafeInteger(receiptSequence) || receiptSequence < 1 ||
        (runKey !== null && (typeof runKey !== 'string' || !runKey || runKey.length > 256)))
      throw new Error('Prova causal de comando publicado inválida.');
    let decoded;
    try { decoded = JSON.parse(request); } catch { throw new Error('Requisição publicada inválida.'); }
    if (!decoded || decoded.kind !== kind || decoded.adventureId !== adventureId ||
        canonicalJson(decoded) !== request)
      throw new Error('Requisição publicada divergente da prova causal.');
    return this.dispatchEvent('PUBLISHED_COMMAND_COMMITTED',
      { actionId: key, kind, adventureId, proofVersion, request, receiptSequence, runKey },
      'controller', { idempotencyKey: key, controllerToken: CONTROLLER_PROOF_TOKEN });
  }

  // Every external event is persisted before triggers execute. Nested events are
  // queued, never recursively evaluated. The cap applies to the entire cascade.
  dispatchEvent(eventType, payload = {}, source = 'runtime', options = {}) {
    safeStateKey(eventType, 'Tipo de evento');
    safeStateKey(source, 'Origem de evento');
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) throw new Error('Payload de evento inválido.');
    assertPersistable(payload, 'Payload de evento');
    if (!options || typeof options !== 'object' || Array.isArray(options)) throw new Error('Opções de evento inválidas.');
    if (source === 'engine' && options.engineToken !== ENGINE_EVENT_TOKEN)
      throw new Error('Origem interna UAE reservada ao motor.');
    if ((source === 'controller' || eventType === 'PUBLISHED_COMMAND_COMMITTED') &&
        options.controllerToken !== CONTROLLER_PROOF_TOKEN)
      throw new Error('Prova de comando publicado reservada ao controlador.');
    if (ENGINE_EVENT_TYPES.has(eventType) && source !== 'engine')
      throw new Error('Evento interno UAE não pode ser injetado externamente.');
    const idempotencyKey = checkedIdempotencyKey(options.idempotencyKey);
    // The source+key pair is a durable command identity. A retry after a
    // WebView crash must not reapply triggers, counters or consequences.
    if (idempotencyKey !== null) {
      const matches = [...this.state.canonicalEvents, ...this._pendingEvents]
        .filter(e => e.idempotencyKey === idempotencyKey && e.source === source);
      if (matches.length) {
        if (matches.some(e => e.type !== eventType && e.eventType !== eventType || canonicalJson(e.payload) !== canonicalJson(payload)))
          throw new Error('Chave de idempotência reutilizada com evento diferente.');
        return [];
      }
    }
    if (this._processingEvents) {
      this._pendingEvents.push({ eventType, payload: clone(payload), source, idempotencyKey });
      return [];
    }
    return this._atomic(() => {
      this._pendingEvents.push({ eventType, payload: clone(payload), source, idempotencyKey });
      const firedIds = [];
      let triggerExecutions = 0;
      this._processingEvents = true;
      try {
        while (this._pendingEvents.length) {
          const next = this._pendingEvents.shift();
          const causeEvent = this._emitEvent(next.eventType, next.payload, next.source, next.idempotencyKey);
          const eligible = (this.module.triggers || [])
            .map((trigger, index) => ({ trigger, index }))
            .filter(({ trigger }) => trigger.event === next.eventType)
            .sort((a, b) => (Number(b.trigger.priority) || 0) - (Number(a.trigger.priority) || 0) || a.index - b.index);
          for (const { trigger } of eligible) {
            const fires = Number(this.state.triggersFired[trigger.id] || 0);
            if (trigger.maxFires && fires >= trigger.maxFires) continue;
            if (!this.isConditionMet(trigger.condition, next.payload)) continue;
            if (triggerExecutions >= 10) {
              // Published actions and explicit UAE transactions cannot commit
              // a partial cascade with some rewards/flags already applied.
              // Keep the historical auditable cap for standalone dispatches.
              if (this._strictCascadeDepth > 0)
                throw new Error('Limite de cascata UAE: transação revertida integralmente.');
              this._pendingEvents.length = 0;
              this._emitEvent('TRIGGER_CASCADE_LIMIT', { maxCascades: 10, lastEvent: next.eventType }, 'engine');
              return firedIds;
            }
            triggerExecutions++;
            this.state.triggersFired[trigger.id] = fires + 1;
            firedIds.push(trigger.id);
            // Conditions inside a trigger's individual effects are evaluated against
            // the triggering event, not against an empty/default payload.
            // Each queued event has its own immutable payload context.
            if (trigger.effects) this.applyEffects(trigger.effects, next.payload);
            if (trigger.consequence) this.recordConsequence(trigger.consequence, {
              token: AUTHORED_CONSEQUENCE_TOKEN, triggerId: trigger.id, causeEventId: causeEvent.id
            });
            this._emitEvent('TRIGGER_FIRED', { triggerId: trigger.id, eventType: next.eventType, causeEventId: causeEvent.id }, 'engine');
          }
        }
      } finally {
        this._processingEvents = false;
        this._pendingEvents.length = 0;
      }
      return firedIds;
    });
  }

  getState() {
    return clone(this.state);
  }

  serialize() {
    assertPersistable(this.state, 'Snapshot de aventura');
    validateAdventureCheckpoint(this.module, this.state);
    return JSON.stringify(this.state);
  }

  static restore(module, serializedState) {
    const raw = typeof serializedState === 'string' ? JSON.parse(serializedState) : serializedState;
    // Compatibility with P128's documented journal-gap recovery: the saved
    // counter may lag behind a valid journal (e.g. interrupted save). Never
    // rewrite event IDs or repair any other v2 corruption. Published adapters
    // perform their strict raw-snapshot gate before calling this method.
    if (raw && raw.schemaVersion === ADVENTURE_SCHEMA_VERSION &&
        Array.isArray(raw.canonicalEvents) && Number.isSafeInteger(raw.eventSequence) && raw.eventSequence >= 0) {
      const prefix = `evt_${module.id}_`;
      let previous = 0;
      let ordered = true;
      for (const event of raw.canonicalEvents) {
        const id = event?.id;
        const suffix = typeof id === 'string' && id.startsWith(prefix) ? id.slice(prefix.length) : '';
        const sequence = /^\d+$/.test(suffix) ? Number(suffix) : NaN;
        if (!Number.isSafeInteger(sequence) || sequence <= previous) { ordered = false; break; }
        previous = sequence;
      }
      if (ordered && previous > raw.eventSequence) {
        const recovered = clone(raw);
        recovered.eventSequence = previous;
        return new AdventureRuntime(module, recovered);
      }
    }
    return new AdventureRuntime(module, raw);
  }

  getCurrentNode() {
    return clone(this.module.nodes[this.state.currentNodeId] || null);
  }

  isConditionMet(condition, payload = {}) {
    return this._isConditionMet(condition, payload, 0);
  }

  /** P164: bounded, fail-closed conditions prevent a malformed imported
   * adventure from recursively exhausting the Android WebView stack. */
  _isConditionMet(condition, payload, depth) {
    if (depth > 16) return false;
    if (condition === undefined || condition === null || condition === '') return true;
    // False, zero and other malformed authored predicates must never unlock
    // choices or trigger rewards through JS truthiness.
    if (Array.isArray(condition)) return condition.length > 0 && condition.length <= 64 &&
      condition.every(item => item !== '' && item !== null && item !== undefined &&
        this._isConditionMet(item, payload, depth + 1));
    if (typeof condition !== 'string' || condition.length > 2048) return false;
    if (condition.startsWith('all:')) {
      const parts=condition.slice(4).split('|');
      return parts.length > 0 && parts.length <= 32 && parts.every(item =>
        item.length > 0 && this._isConditionMet(item, payload, depth + 1));
    }
    if (condition.startsWith('any:')) {
      const parts=condition.slice(4).split('|');
      return parts.length > 0 && parts.length <= 32 && parts.every(item => item.length > 0) &&
        parts.some(item => this._isConditionMet(item, payload, depth + 1));
    }
    const s = this.state;

    if (condition.startsWith('unvisited:')) {
      const target = condition.slice(10);
      return Object.hasOwn(this.module.nodes, target) && !s.visitedNodeIds.includes(target);
    }
    if (condition.startsWith('visited:')) {
      const target = condition.slice(8);
      return s.visitedNodeIds.includes(target);
    }
    if (condition.startsWith('flag:')) {
      const flagName = condition.slice(5);
      return Object.hasOwn(s.flags, flagName) && Boolean(s.flags[flagName]);
    }
    if (condition.startsWith('!flag:')) {
      const flagName = condition.slice(6);
      return !Object.hasOwn(s.flags, flagName) || !s.flags[flagName];
    }
    if (condition.startsWith('investigated:')) {
      const n = Number(condition.slice(13)) || 0;
      return (s.investigationLocations?.length || 0) >= n;
    }
    if (condition.startsWith('check-success:')) {
      const nodeId = condition.slice(14);
      if (nodeId === 'kaato-parley' && s.flags?.parleyBroken) return false;
      return (s.checkHistory || []).some(x => x.nodeId === nodeId && x.success === true);
    }
    if (condition.startsWith('encounter-resolved:')) {
      const encId = condition.slice(19);
      return (s.systems?.encounters?.history || []).some(x => x.encounterId === encId && x.outcome);
    }
    if (condition === 'prep-ready') {
      return (s.visitedNodeIds || []).includes('hangar-investigation') || (s.visitedNodeIds || []).includes('ship-security');
    }
    if (condition === 'kaato-outcome:neutralized') {
      return Boolean(s.flags?.kaatoNeutralized) && (s.systems?.encounters?.history || []).some(x => x.encounterId === 'kaato-showdown' && x.outcome);
    }
    if (condition === 'kaato-outcome:not-neutralized') {
      return !s.flags?.kaatoNeutralized && (s.systems?.encounters?.history || []).some(x => x.encounterId === 'kaato-showdown' && x.outcome);
    }
    if (condition.startsWith('clock:')) {
      const [clockId, op, val] = condition.slice(6).split(':');
      // An absent clock is not an implicit zero-valued clock. Otherwise
      // comparisons such as <=1 unlock content before its clock exists.
      if (!clockId || !Object.hasOwn(s.clocks ?? {}, clockId)) return false;
      const cur = s.clocks[clockId]?.value;
      const target = val !== undefined && /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(val.trim()) ? Number(val) : NaN;
      if (!Number.isFinite(cur) || !Number.isFinite(target)) return false;
      if (op === '>=') return cur >= target;
      if (op === '<=') return cur <= target;
      if (op === '>') return cur > target;
      if (op === '<') return cur < target;
      if (op === '=') return cur === target;
      return false;
    }
    if (condition.startsWith('counter:')) {
      const [counterId, op, val] = condition.slice(8).split(':');
      if (!counterId || !Object.hasOwn(s.counters ?? {}, counterId)) return false;
      const cur = s.counters[counterId];
      const target = val !== undefined && /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(val.trim()) ? Number(val) : NaN;
      if (!Number.isFinite(cur) || !Number.isFinite(target)) return false;
      if (op === '>=') return cur >= target;
      if (op === '<=') return cur <= target;
      if (op === '>') return cur > target;
      if (op === '<') return cur < target;
      if (op === '=') return cur === target;
      return false;
    }
    if (condition.startsWith('objective:')) {
      const [objId, status] = condition.slice(10).split(':');
      return Object.hasOwn(s.objectives ?? {}, objId) && s.objectives[objId].status === status;
    }
    if (condition.startsWith('payload:')) {
      // Split at the FIRST separator only: event values (routes, qualified
      // identifiers) may themselves contain ':' and must compare exactly.
      const comparison = condition.slice(8);
      const separator = comparison.indexOf(':');
      if (separator < 1) return false;
      const field = comparison.slice(0, separator);
      const expected = comparison.slice(separator + 1);
      // Objects and arrays must never match the implicit JS strings
      // '[object Object]' or 'a,b' in authored trigger predicates.
      if (!payload || typeof payload !== 'object' || !Object.hasOwn(payload, field)) return false;
      const actual=payload[field];
      return (typeof actual === 'string' || typeof actual === 'number' || typeof actual === 'boolean') &&
        (typeof actual !== 'number' || Number.isFinite(actual)) && String(actual) === expected;
    }
    return false; // fail closed: desconhecido nunca libera escolha ou gatilho
  }

  getAvailableChoices() {
    const node = this.getCurrentNode();
    if (!node || !Array.isArray(node.choices)) return [];

    return node.choices
      .filter(choice => this.isConditionMet(choice.condition))
      .map(c => ({
        id: c.id,
        label: c.label,
        to: c.to
      }));
  }

  applyEffects(effects, eventPayload = {}) {
    // The triggering event is scoped to this effects batch only. Never cache
    // it on the runtime: a later choice, checkpoint or unrelated event must
    // not inherit stale trigger data.
    if (!eventPayload || typeof eventPayload !== 'object' || Array.isArray(eventPayload))
      throw new Error('Contexto de evento UAE inválido.');
    assertPersistable(eventPayload, 'Contexto de evento UAE');
    // Direct API calls must be as atomic as transitions and trigger cascades.
    return this._atomic(() => {
    if (!effects || typeof effects !== 'object')
      throw new Error('Efeitos UAE inválidos.');

    if (Array.isArray(effects)) {
      for (const eff of effects) {
        this._applySingleEffect(eff, eventPayload);
      }
    } else {
      for (const [k, v] of Object.entries(effects)) {
        if (k === 'clear-flag') {
          delete this.state.flags[safeStateKey(v, 'Flag')];
        } else if (k === 'set-counter') {
          const { counter, value } = v;
          this.state.counters[safeStateKey(counter, 'Contador')] = finiteNumber(value, 'Valor de contador');
        } else if (k === 'emit-event') {
          this.dispatchEvent(v.type || 'CUSTOM_EVENT', v.payload || {}, v.source || 'effect');
        } else if (k === 'objective-progress') {
          this.updateObjectiveProgress(v.objectiveId, v.delta ?? 1);
        } else if (k === 'complete-objective') {
          this.completeObjective(typeof v === 'string' ? v : v?.objectiveId);
        } else if (k === 'fail-objective') {
          this.failObjective(typeof v === 'string' ? v : v?.objectiveId);
        } else {
          assertPersistable(v, 'Valor de flag');
          this.state.flags[safeStateKey(k, 'Flag')] = clone(v);
        }
      }
    }
    });
  }

  _applySingleEffect(eff, eventPayload = {}) {
    if (!eff || typeof eff !== 'object' || Array.isArray(eff))
      throw new Error('Efeito UAE inválido.');
    // `condition: false` must not bypass the guard through JS truthiness.
    // Missing conditions are unconditional; explicit malformed predicates
    // fail closed and cannot grant rewards in imported Android commands.
    if (Object.hasOwn(eff, 'condition') && !this.isConditionMet(eff.condition, eventPayload)) return;
    if (Object.hasOwn(eff, 'type') && (typeof eff.type !== 'string' || !eff.type))
      throw new Error('Tipo de efeito UAE inválido.');
    const type = eff.type || 'set-flag';
    switch (type) {
      case 'set-flag':
        assertPersistable(eff.value === undefined ? true : eff.value, 'Valor de flag');
        this.state.flags[safeStateKey(eff.flag, 'Flag')] = clone(eff.value === undefined ? true : eff.value);
        break;
      case 'clear-flag':
        delete this.state.flags[safeStateKey(eff.flag, 'Flag')];
        break;
      case 'set-counter':
        this.state.counters[safeStateKey(eff.counter, 'Contador')] = finiteNumber(eff.value, 'Valor de contador');
        break;
      case 'adjust-counter':
        const counterId = safeStateKey(eff.counter, 'Contador');
        const delta = finiteNumber(eff.delta ?? 1, 'Variação de contador');
        this.state.counters[counterId] = finiteResult((Number(this.state.counters[counterId]) || 0) + delta, 'Contador');
        break;
      case 'advance-clock':
        this.advanceClock(eff.clockId, eff.delta ?? 1);
        break;
      case 'emit-event':
        this.dispatchEvent(eff.eventType || 'CUSTOM_EVENT', eff.payload || {}, 'effect');
        break;
      case 'complete-objective':
        this.completeObjective(eff.objectiveId);
        break;
      case 'fail-objective':
        this.failObjective(eff.objectiveId);
        break;
      case 'progress-objective':
        this.updateObjectiveProgress(eff.objectiveId, eff.delta ?? 1);
        break;
      case 'add-clue':
        this.addClue(eff.clueId, eff.title, eff.summary);
        break;
      case 'reveal-secret':
        this.revealSecret(eff.secretId, eff.description);
        break;
      default:
        throw new Error(`Efeito UAE não suportado: ${type}`);
    }
  }

  transition(choiceId) {
    return this._atomic(() => {
      if (this.state.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
      const fromNodeId = this.state.currentNodeId;
      const node = this.getCurrentNode();
      if (!node) throw new Error('Cena atual inválida.');
      const choice = (node.choices || []).find(c => c.id === choiceId);
      if (!choice) throw new Error(`Escolha "${choiceId}" não encontrada no nó atual.`);
      if (!this.isConditionMet(choice.condition)) throw new Error(`Condição para escolha "${choiceId}" não foi satisfeita.`);
      const nextNodeId = choice.to;
      const nextNode = this.module.nodes[nextNodeId];
      if (!nextNode) throw new Error(`Nó de destino "${nextNodeId}" inexistente.`);

      if (choice.effects) this.applyEffects(choice.effects);
      this.state.currentNodeId = nextNodeId;
      this.state.currentLocationId = nextNode.locationId || this.state.currentLocationId;
      if (!this.state.visitedNodeIds.includes(nextNodeId)) this.state.visitedNodeIds.push(nextNodeId);
      if (nextNode.onEnter) this.applyEffects(nextNode.onEnter);
      this.dispatchEvent('NODE_ENTER', { from: fromNodeId, to: nextNodeId, choiceId });
      this.dispatchEvent('NODE_TRANSITION', { from: fromNodeId, to: nextNodeId, choiceId });

      if (nextNode.terminal || (!nextNode.choices || nextNode.choices.length === 0)) {
        this.state.status = 'COMPLETED';
        this.state.completedAt = new Date().toISOString();
        this.dispatchEvent('ADVENTURE_COMPLETED', { nodeId: nextNodeId });
      }
      this.state.updatedAt = new Date().toISOString();
      return this.getState();
    });
  }

  evaluateTriggers(eventType, payload = {}) {
    return this.dispatchEvent(eventType, payload, 'runtime');
  }

  recordConsequence(consequence, provenance = null) {
    return this._atomic(() => {
      if (!consequence) return;
      const cId = typeof consequence === 'string' ? consequence : consequence?.id;
      safeStateKey(cId, 'Consequência');
      assertPersistable(consequence, 'Consequência');
      // A direct call or an untrusted commitAction cannot mint a published
      // consequence and later claim its onResolveEffects rewards. The token is
      // module-private; only the trigger evaluator can supply it.
      const authors = (this.module.triggers || []).filter(trigger =>
        (typeof trigger.consequence === 'string' ? trigger.consequence : trigger.consequence?.id) === cId);
      const author = authors.find(trigger => trigger.id === provenance?.triggerId);
      const authenticated = provenance?.token === AUTHORED_CONSEQUENCE_TOKEN;
      if (authors.length && (!authenticated || !author ||
          typeof provenance.causeEventId !== 'string' ||
          !this.state.canonicalEvents.some(event => event.id === provenance.causeEventId && event.type === author.event)))
        throw new Error('Consequência publicada exige gatilho de origem comprovada.');
      if (!authors.length && authenticated)
        throw new Error('Consequência de gatilho não consta da fonte publicada.');
      if (this.state.consequences.some(c => c.id === cId)) return;
      const metadata = typeof consequence === 'string' ? {id:cId} : {...consequence};
      delete metadata.onResolveEffects;
      delete metadata.resolveWhen;
      // Only plain metadata is durable; no imported scripts, nested objects,
      // internal fields or prototype keys can ride along in the save.
      for (const [key, value] of Object.entries(metadata)) {
        safeStateKey(key, 'Campo de consequência');
        if (key !== 'id' && (value !== null && !['string','number','boolean'].includes(typeof value)))
          throw new Error('Metadados de consequência não persistíveis.');
        if (['resolved','recordedAt','resolvedAt','authored','triggerId','causeEventId'].includes(key))
          throw new Error('Metadados de consequência reservados ao motor.');
      }
      this.state.consequences.push({...metadata, resolved:false, recordedAt:new Date().toISOString()});
      this.#dispatchEngineEvent('CONSEQUENCE_RECORDED', {
        consequenceId: cId, authored: Boolean(authors.length),
        ...(author ? {triggerId:provenance.triggerId, causeEventId:provenance.causeEventId} : {})
      });
    });
  }

  resolveConsequence(consequenceId, onResolveEffects = null) {
    return this._atomic(() => {
      const c = this.state.consequences.find(item => item.id === consequenceId);
      if (c && !c.resolved) {
        // Resolve only the original, validated source-authored script. A
        // tampered checkpoint cannot supply a replacement reward payload.
        const authoredSources = (this.module.triggers || [])
          .map(trigger => trigger.consequence)
          .filter(value => (typeof value === 'string' ? value : value?.id) === consequenceId);
        const authored = authoredSources.find(value => value && typeof value === 'object');
        if (authored?.resolveWhen && !this.isConditionMet(authored.resolveWhen))
          throw new Error('Condição de resolução da consequência não satisfeita.');
        c.resolved = true;
        c.resolvedAt = new Date().toISOString();
        // A published consequence has one authoritative effect script. Caller-
        // supplied scripts would allow a forged reward even with a clean save.
        // Legacy ad-hoc consequences retain the older explicit-effects API.
        if (authoredSources.length && onResolveEffects !== null)
          throw new Error('Consequência publicada não aceita efeitos de resolução externos.');
        if (authored?.onResolveEffects) this.applyEffects(authored.onResolveEffects);
        if (!authored && onResolveEffects) this.applyEffects(onResolveEffects);
        this.#dispatchEngineEvent('CONSEQUENCE_RESOLVED', { consequenceId });
      }
      return c ? clone(c) : null;
    });
  }

  updateObjectiveProgress(objectiveId, delta = 1) {
    return this._atomic(() => {
    if (!this.state.objectives[objectiveId]) return;
    const obj = this.state.objectives[objectiveId];
    if (obj.status !== 'ACTIVE') return;

    const progressDelta = finiteNumber(delta, 'Progresso');
    const nextProgress = finiteResult((Number(obj.progress) || 0) + progressDelta, 'Progresso');
    const next = Math.max(0, Math.min(obj.target, nextProgress));
    const requires = this.module.initialObjectives?.[objectiveId]?.requires || [];
    if (next >= obj.target && requires.some(id => this.state.objectives[id]?.status !== 'COMPLETED'))
      throw new Error(`Pré-requisitos do objetivo ${objectiveId} não concluídos.`);
    obj.progress = next;
    this.dispatchEvent('OBJECTIVE_PROGRESS', { objectiveId, progress: obj.progress, target: obj.target });
    if (obj.progress >= obj.target) {
      this.completeObjective(objectiveId);
    }
    });
  }

  completeObjective(objectiveId) {
    return this._atomic(() => {
    if (!this.state.objectives[objectiveId]) return;
    const obj = this.state.objectives[objectiveId];
    if (obj.status !== 'ACTIVE') return;
    const requires = this.module.initialObjectives?.[objectiveId]?.requires || [];
    if (requires.some(id => this.state.objectives[id]?.status !== 'COMPLETED'))
      throw new Error(`Pré-requisitos do objetivo ${objectiveId} não concluídos.`);

    obj.status = 'COMPLETED';
    obj.progress = obj.target;
    obj.completedAt = new Date().toISOString();
    if (!this.state.completedObjectives.includes(objectiveId)) {
      this.state.completedObjectives.push(objectiveId);
    }

    this.#dispatchEngineEvent('OBJECTIVE_COMPLETED', { objectiveId });
    });
  }

  failObjective(objectiveId) {
    return this._atomic(() => {
    if (!this.state.objectives[objectiveId]) return;
    const obj = this.state.objectives[objectiveId];
    if (obj.status !== 'ACTIVE') return;

    obj.status = 'FAILED';
    obj.failedAt = new Date().toISOString();
    if (!this.state.failedObjectives.includes(objectiveId)) {
      this.state.failedObjectives.push(objectiveId);
    }

    this.#dispatchEngineEvent('OBJECTIVE_FAILED', { objectiveId });
    });
  }

  advanceClock(clockId, delta = 1) {
    return this._atomic(() => {
      safeStateKey(clockId, 'Identificador de relógio');
      const clockDelta = finiteNumber(delta, 'Variação de relógio');
      if (!this.state.clocks[clockId]) this.state.clocks[clockId] = { value: 0, max: 6, status: 'ACTIVE' };
      const clock = this.state.clocks[clockId];
      if (clock.status === 'PAUSED' || clock.status === 'EXPIRED') return clone(clock);
      const wasComplete = clock.status === 'COMPLETED';
      clock.max = Number(clock.max) > 0 ? Number(clock.max) : 6;
      clock.value = Math.min(clock.max, Math.max(0, finiteResult((Number(clock.value) || 0) + clockDelta, 'Relógio')));
      clock.status = clock.value >= clock.max ? 'COMPLETED' : 'ACTIVE';
      this.dispatchEvent('CLOCK_ADVANCED', { clockId, value: clock.value, max: clock.max, status: clock.status });
      if (!wasComplete && clock.status === 'COMPLETED') this.dispatchEvent('CLOCK_COMPLETED', { clockId, value: clock.value });
      return clone(clock);
    });
  }

  addClue(clueId, title, summary) {
    return this._atomic(() => {
    safeStateKey(clueId, 'Pista');
    if (title !== undefined) assertPersistable(title, 'Título de pista');
    if (summary !== undefined) assertPersistable(summary, 'Resumo de pista');
    if (!this.state.discoveredClues.some(c => c.id === clueId)) {
      this.state.discoveredClues.push({
        id: clueId,
        title: title ?? null,
        summary: summary ?? null,
        discoveredAt: new Date().toISOString()
      });
      this.dispatchEvent('CLUE_DISCOVERED', { clueId, title: title ?? null });
    }
    });
  }

  revealSecret(secretId, description) {
    return this._atomic(() => {
    safeStateKey(secretId, 'Segredo');
    if (description !== undefined) assertPersistable(description, 'Descrição de segredo');
    if (!this.state.revealedSecrets.some(s => s.id === secretId)) {
      this.state.revealedSecrets.push({
        id: secretId,
        description: description ?? null,
        revealedAt: new Date().toISOString()
      });
      this.dispatchEvent('SECRET_REVEALED', { secretId });
    }
    });
  }

  archive() {
    return this._atomic(() => {
      if (this.state.status !== 'COMPLETED' && this.state.status !== 'FAILED')
        throw new Error('Aventura precisa estar encerrada antes do arquivamento.');
      this.state.status = 'ARCHIVED';
      this.state.archivedAt = new Date().toISOString();
      this.state.updatedAt = this.state.archivedAt;
      this.dispatchEvent('ADVENTURE_ARCHIVED', { adventureId: this.module.id });
      return this.getState();
    });
  }

  buildAiContext(world = {}, character = {}) {
    const node = this.getCurrentNode();
    return {
      adventure: {
        id: this.module.id,
        title: this.module.title,
        status: this.state.status,
        currentNodeId: this.state.currentNodeId,
        currentLocationId: this.state.currentLocationId,
        clocks: clone(this.state.clocks),
        objectives: clone(this.state.objectives)
      },
      playerKnowledge: {
        discoveredClues: clone(this.state.discoveredClues),
        visitedNodeIds: clone(this.state.visitedNodeIds)
      },
      gmKnowledge: {
        revealedSecrets: clone(this.state.revealedSecrets),
        flags: clone(this.state.flags),
        consequences: clone(this.state.consequences)
      },
      scene: {
        title: node?.title || '',
        summary: node?.summary || '',
        narration: node?.narration || '',
        availableChecks: clone(node?.checks || [])
      }
    };
  }
}
