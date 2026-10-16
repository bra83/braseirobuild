/**
 * R54 P129 — Durable Universal Adventure Engine integration for the five
 * published adventure adapters. The legacy adapters remain responsible for
 * their source-specific checks/encounters and world consequences. The UAE
 * owns a persisted, replay-safe event/objective/clock ledger in the same world
 * snapshot. A validated adapter transition is mirrored, never silently lost.
 */
import { AdventureRuntime, assertPersistable } from './adventureRuntime.mjs';
import { validateAdventureModule } from './adventureSchema.mjs';
import { validateAdventureCheckpoint } from './adventureCheckpointGate.mjs';
import { validatePublishedAdventureGraph } from './publishedAdventureGraphGate.mjs';
import { validateTaskTimeLedger } from '../world/taskTimeLedger.mjs';
import { validatePublishedActionReceipts, validatePublishedActionCausality } from './publishedActionReceipts.mjs';
import { validateAdventureSemantics } from './adventureSemanticGate.mjs';

const clone = value => structuredClone(value);
const own = (value, key) => Object.prototype.hasOwnProperty.call(value, key);
const normalizeStatus = value => {
  const status = String(value ?? 'ACTIVE').toUpperCase();
  return ['ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED', 'ARCHIVED'].includes(status) ? status : 'ACTIVE';
};
const asRecord = value => value && typeof value === 'object' && !Array.isArray(value) ? value : {};

// Milestones are generated from existing authored episode markers. They do
// not add or invent encounters, rewards or book mechanics.
export function publishedAdventureRuntimeModule(sourceModule) {
  validateAdventureModule(sourceModule);
  validatePublishedAdventureGraph(sourceModule);
  const module = clone(sourceModule);
  const episodes = [...new Set(Object.values(module.nodes).map(node => Number(node.episode)).filter(n => Number.isInteger(n) && n > 0))].sort((a, b) => a - b);
  module.initialObjectives = { ...asRecord(module.initialObjectives) };
  module.triggers = [...(module.triggers ?? [])];
  for (const episode of episodes) {
    const objectiveId = `episode-${episode}`;
    if (!own(module.initialObjectives, objectiveId)) module.initialObjectives[objectiveId] = { title: `Concluir episódio ${episode}`, type: 'PRIMARY', target: 1 };
    const firstNext = Object.entries(module.nodes).filter(([, node]) => Number(node.episode) > episode).map(([id]) => id);
    if (firstNext.length) {
      // A single objective may be reached by several branches; completion is
      // idempotent, and maxFires=1 prevents repeated side effects.
      for (const nodeId of firstNext) {
        const triggerId = `p128-episode-${episode}-enter-${nodeId}`;
        module.triggers.push({ id: triggerId, event: 'NODE_ENTER', condition: `payload:to:${nodeId}`, maxFires: 1, effects: [{ type: 'complete-objective', objectiveId }] });
      }
    }
  }
  module.initialObjectives['adventure-finale'] ??= { title: 'Concluir a aventura', type: 'PRIMARY', target: 1 };
  module.triggers.push({ id: 'p128-adventure-finale', event: 'ADVENTURE_COMPLETED', maxFires: 1,
    effects: [...episodes.map(episode => ({ type: 'complete-objective', objectiveId: `episode-${episode}` })),
      { type: 'complete-objective', objectiveId: 'adventure-finale' }] });
  validateAdventureModule(module);
  validateAdventureSemantics(module);
  return module;
}

function hydrateLegacy(runtime, legacy, { copyNode = false } = {}) {
  const state = runtime.state;
  if (copyNode) {
    if (!own(runtime.module.nodes, legacy.currentNodeId)) throw new Error(`Nó publicado inexistente: ${legacy.currentNodeId}`);
    state.currentNodeId = legacy.currentNodeId;
  }
  // Published adapters are authoritative for source-specific mechanics. A
  // deleted legacy flag must not survive as a stale UAE flag after reopening.
  // UAE-only state (objectives, trigger counters, events) remains separate.
  state.flags = clone(asRecord(legacy.flags));
  state.counters = clone(asRecord(legacy.counters));
  state.systems = clone(asRecord(legacy.systems));
  state.checkHistory = clone(Array.isArray(legacy.checkHistory) ? legacy.checkHistory : []);
  const visited = Array.isArray(legacy.visitedNodeIds) ? legacy.visitedNodeIds : [];
  state.visitedNodeIds = [...new Set([...state.visitedNodeIds, ...visited, state.currentNodeId])];
  state.currentLocationId = runtime.module.nodes[state.currentNodeId]?.locationId ?? state.currentLocationId;
  const clocks = asRecord(legacy.clocks);
  for (const [id, clock] of Object.entries(clocks)) {
    if (!clock || typeof clock !== 'object') continue;
    const max = Number(clock.max) > 0 ? Number(clock.max) : Number(state.clocks[id]?.max ?? 6);
    const value = Math.max(0, Math.min(max, Number(clock.value) || 0));
    state.clocks[id] = { ...state.clocks[id], ...clone(clock), max, value };
  }
  if (own(state.clocks, 'imperialExposure') && Number.isFinite(Number(legacy.systems?.imperialExposure))) {
    const clock = state.clocks.imperialExposure;
    clock.value = Math.max(0, Math.min(clock.max, Number(legacy.systems.imperialExposure)));
    clock.status = clock.value >= clock.max ? 'COMPLETED' : 'ACTIVE';
  }
  state.status = normalizeStatus(legacy.status);
  if (legacy.completedAt) state.completedAt = legacy.completedAt;
  if (legacy.archivedAt) state.archivedAt = legacy.archivedAt;
}

/**
 * Reconcile imported saves and out-of-band adapter progress without inventing
 * scene visits, choices, check results or rewards. The legacy node and visited
 * set are authoritative; only the objective milestones implied by their
 * authored episode markers may be recovered. Repeated reconciliation is safe.
 */
function reconcileLegacyMilestones(runtime, legacy, origin = 'legacy') {
  const module = runtime.module;
  const status = normalizeStatus(legacy.status);
  const finished = status === 'COMPLETED' || status === 'ARCHIVED';
  const visited = new Set([...(Array.isArray(legacy.visitedNodeIds) ? legacy.visitedNodeIds : []), legacy.currentNodeId]);
  const reached = [...visited].reduce((highest, id) => {
    const episode = Number(module.nodes[id]?.episode);
    return Number.isInteger(episode) && episode > 0 ? Math.max(highest, episode) : highest;
  }, 0);
  for (const [objectiveId, objective] of Object.entries(runtime.state.objectives)) {
    const match = /^episode-(\d+)$/.exec(objectiveId);
    if (!match || objective.status !== 'ACTIVE') continue;
    const episode = Number(match[1]);
    if (!finished && episode >= reached) continue;
    runtime.dispatchEvent('EPISODE_PROGRESS_RECONCILED', { objectiveId, reachedEpisode: reached, origin }, 'migration');
    runtime.completeObjective(objectiveId);
  }
  if (finished && !runtime.state.canonicalEvents.some(e => e.type === 'ADVENTURE_COMPLETED')) {
    runtime.dispatchEvent('ADVENTURE_COMPLETED', { nodeId: legacy.currentNodeId, origin }, 'migration');
  }
  if (status === 'ARCHIVED' && !runtime.state.canonicalEvents.some(e => e.type === 'ADVENTURE_ARCHIVED')) {
    runtime.dispatchEvent('ADVENTURE_ARCHIVED', { adventureId: module.id, origin }, 'migration');
  }
  runtime.state.status = status;
}

function loadRuntime(module, previousWorld) {
  const previous = previousWorld?.publishedAdventure;
  const saved = previousWorld?.publishedAdventureRuntime;
  if (saved?.adventureId === module.id) {
    // Validate v2 snapshots BEFORE migration, which otherwise repairs missing
    // arrays, sequence numbers and objective progress and can hide corruption.
    if (Number(saved.schemaVersion) >= 2) validatePublishedAdventureRuntimeState(module, saved);
    const runtime = AdventureRuntime.restore(module, saved);
    if (previous?.currentNodeId && runtime.state.currentNodeId !== previous.currentNodeId) {
      if (!own(module.nodes, previous.currentNodeId)) throw new Error('Checkpoint de aventura aponta para cena inexistente.');
      // Earlier builds and imported campaigns may update the legacy progress
      // without a UAE event. Preserve the journal and audit the discrepancy.
      const from = runtime.state.currentNodeId;
      hydrateLegacy(runtime, previous, { copyNode: true });
      runtime.dispatchEvent('EXTERNAL_PROGRESS_RECONCILED', { from, to: previous.currentNodeId }, 'migration');
    }
    if (previous?.id === module.id) {
      hydrateLegacy(runtime, previous, { copyNode: true });
      reconcileLegacyMilestones(runtime, previous, 'restored');
    }
    return runtime;
  }
  if (saved && saved.adventureId !== module.id && previous?.id === module.id) {
    // A new campaign explicitly replaces the previous adventure. Old saves
    // without UAE are migrated, but a mismatched active UAE cannot be reused.
    throw new Error('Checkpoint UAE pertence a outra aventura.');
  }
  const runtime = new AdventureRuntime(module);
  if (previous?.id === module.id) {
    hydrateLegacy(runtime, previous, { copyNode: true });
    runtime.dispatchEvent('LEGACY_SNAPSHOT_MIGRATED', { nodeId: previous.currentNodeId }, 'migration');
    reconcileLegacyMilestones(runtime, previous, 'import');
  }
  return runtime;
}

/** Strict checkpoint gate. Invalid snapshots are never promoted as a
 * successfully synchronized published adventure. No player-facing data is
 * returned by this validator. */
export function validatePublishedAdventureRuntimeState(module, state, progress = null) {
  if (!state || state.adventureId !== module.id) throw new Error('Checkpoint UAE de aventura incompatível.');
  // A snapshot may be valid as a JS object but lose flags, trigger payloads or
  // secrets when Android saves it as JSON. Reject such snapshots before they
  // are promoted or exposed to the player bridge.
  assertPersistable(state, 'Checkpoint UAE');
  for (const field of ['discoveredClues', 'revealedSecrets']) {
    if (!Array.isArray(state[field])) throw new Error(`Coleção UAE ausente: ${field}`);
    const ids = new Set();
    for (const entry of state[field]) {
      if (!entry || typeof entry.id !== 'string' || !entry.id.trim() || ids.has(entry.id))
        throw new Error(`Coleção UAE inválida ou duplicada: ${field}`);
      ids.add(entry.id);
    }
  }
  if (!Array.isArray(state.checkHistory) || !state.flags || typeof state.flags !== 'object' ||
      Array.isArray(state.flags) || !state.systems || typeof state.systems !== 'object' ||
      Array.isArray(state.systems)) throw new Error('Dados persistidos UAE inválidos.');
  if (!Object.hasOwn(module.nodes, state.currentNodeId)) throw new Error('Checkpoint UAE contém cena inexistente.');
  if (!Array.isArray(state.visitedNodeIds) || !state.visitedNodeIds.includes(state.currentNodeId))
    throw new Error('Checkpoint UAE perdeu o histórico da cena atual.');
  if (state.visitedNodeIds.some(id => typeof id !== 'string' || !Object.hasOwn(module.nodes, id)) ||
      new Set(state.visitedNodeIds).size !== state.visitedNodeIds.length)
    throw new Error('Histórico UAE contém cenas desconhecidas ou duplicadas.');
  if (!['ACTIVE','PAUSED','COMPLETED','FAILED','ARCHIVED'].includes(state.status))
    throw new Error('Status UAE inválido.');
  if (!state.triggersFired || typeof state.triggersFired !== 'object' || Array.isArray(state.triggersFired) ||
      Object.values(state.triggersFired).some(n => !Number.isSafeInteger(n) || n < 0))
    throw new Error('Contadores de gatilhos UAE inválidos.');
  const triggerCatalog = new Map((module.triggers || []).map(t => [t.id, t]));
  for (const [id, fires] of Object.entries(state.triggersFired)) {
    const trigger = triggerCatalog.get(id);
    if (!trigger || (trigger.maxFires && fires > trigger.maxFires))
      throw new Error(`Contador de gatilho UAE desconhecido ou acima do limite: ${id}`);
  }
  if (!state.counters || typeof state.counters !== 'object' || Array.isArray(state.counters) ||
      Object.values(state.counters).some(value => typeof value !== 'number' || !Number.isFinite(value)))
    throw new Error('Contadores UAE malformados.');
  if (!Array.isArray(state.canonicalEvents)) throw new Error('Journal UAE inválido.');
  const eventIds = state.canonicalEvents.map(event => event?.id);
  if (eventIds.some(id => typeof id !== 'string' || !id) || new Set(eventIds).size !== eventIds.length)
    throw new Error('Journal UAE contém eventos duplicados ou inválidos.');
  const prefix = `evt_${module.id}_`;
  let maxId = 0;
  for (const event of state.canonicalEvents) {
    const id = event?.id;
    const suffix = id.startsWith(prefix) ? id.slice(prefix.length) : '';
    const sequence = /^\d+$/.test(suffix) ? Number(suffix) : NaN;
    if (!Number.isSafeInteger(sequence) || sequence <= maxId)
      throw new Error('Sequência regressiva ou ID estrangeiro no journal UAE.');
    if (typeof event.type !== 'string' || !event.type ||
        typeof event.source !== 'string' || !event.source ||
        !event.payload || typeof event.payload !== 'object' || Array.isArray(event.payload) ||
        !Number.isSafeInteger(event.turn) || event.turn < 1 ||
        !Array.isArray(event.consequenceIds) || event.consequenceIds.some(id => typeof id !== 'string') ||
        !Number.isFinite(Date.parse(event.timestamp)))
      throw new Error('Evento canônico UAE malformado.');
    maxId = sequence;
  }
  if (!Number.isSafeInteger(state.eventSequence) || state.eventSequence < maxId)
    throw new Error('Sequência do journal UAE inválida.');
  if (!Array.isArray(state.consequences) || !Array.isArray(state.completedObjectives) ||
      !Array.isArray(state.failedObjectives) || !state.objectives || typeof state.objectives !== 'object' || Array.isArray(state.objectives))
    throw new Error('Estrutura persistida de objetivos/consequências UAE inválida.');
  for (const id of [...state.completedObjectives, ...state.failedObjectives]) {
    if (typeof id !== 'string' || !Object.hasOwn(state.objectives, id))
      throw new Error('Histórico UAE referencia objetivo inexistente.');
  }
  for (const id of Object.keys(state.objectives)) {
    if (!Object.hasOwn(module.initialObjectives || {}, id))
      throw new Error(`Objetivo UAE não definido pela aventura: ${id}`);
  }
  const completed = new Set(state.completedObjectives);
  const failed = new Set(state.failedObjectives);
  if (completed.size !== state.completedObjectives.length || failed.size !== state.failedObjectives.length)
    throw new Error('Objetivos UAE duplicados no histórico.');
  for (const id of completed) if (state.objectives[id]?.status !== 'COMPLETED' || failed.has(id))
    throw new Error(`Objetivo UAE concluído inconsistente: ${id}`);
  for (const id of failed) if (state.objectives[id]?.status !== 'FAILED')
    throw new Error(`Objetivo UAE falhado inconsistente: ${id}`);
  for (const [id, objective] of Object.entries(state.objectives)) {
    if (objective?.status === 'COMPLETED' && !completed.has(id))
      throw new Error(`Objetivo UAE concluído ausente do histórico: ${id}`);
    if (objective?.status === 'FAILED' && !failed.has(id))
      throw new Error(`Objetivo UAE falhado ausente do histórico: ${id}`);
  }
  const consequenceIds = state.consequences.map(c => c?.id);
  if (consequenceIds.some(id => typeof id !== 'string' || !id) ||
      new Set(consequenceIds).size !== consequenceIds.length)
    throw new Error('Consequências UAE inválidas ou duplicadas.');
  for (const consequence of state.consequences) {
    if (!consequence || typeof consequence !== 'object' || Array.isArray(consequence) ||
        typeof consequence.id !== 'string' || !consequence.id ||
        (consequence.resolved !== undefined && typeof consequence.resolved !== 'boolean') ||
        (consequence.resolved === true && !Number.isFinite(Date.parse(consequence.resolvedAt))))
      throw new Error('Registro de consequência UAE malformado.');
  }
  for (const [id, objective] of Object.entries(state.objectives || {})) {
    if (!objective || !['ACTIVE', 'COMPLETED', 'FAILED'].includes(objective.status) ||
        !Number.isFinite(objective.progress) || !Number.isFinite(objective.target) ||
        objective.target <= 0 || objective.progress < 0 || objective.progress > objective.target)
      throw new Error(`Objetivo UAE inválido: ${id}`);
  }
  if (!state.clocks || typeof state.clocks !== 'object' || Array.isArray(state.clocks))
    throw new Error('Estrutura de relógios UAE inválida.');
  for (const [id, clock] of Object.entries(state.clocks)) {
    if (!clock || !Number.isFinite(clock.value) || !Number.isFinite(clock.max) ||
        !['ACTIVE','COMPLETED','EXPIRED','PAUSED'].includes(clock.status) ||
        clock.max <= 0 || clock.value < 0 || clock.value > clock.max)
      throw new Error(`Relógio UAE inválido: ${id}`);
  }
  if (progress) {
    if (progress.id !== module.id || progress.currentNodeId !== state.currentNodeId ||
        normalizeStatus(progress.status) !== state.status)
      throw new Error('Checkpoint UAE diverge do progresso publicado.');
    if (['COMPLETED', 'ARCHIVED'].includes(state.status) &&
        state.objectives?.['adventure-finale']?.status !== 'COMPLETED')
      throw new Error('Aventura encerrada com objetivo final pendente.');
  }
  validateAdventureCheckpoint(module, state);
  return true;
}

/**
 * Called only after the source-specific adapter has accepted the action.
 * Returns a NEW world; errors do not mutate the caller's world or checkpoint.
 */
export function reconcilePublishedAdventureWorld(previousWorld, nextWorld, adapter, { kind = 'SYNC', choiceId = null, payload = {} } = {}) {
  validatePublishedActionCausality(previousWorld);
  const next = clone(nextWorld);
  // Every START, including an immediate restart within the same millisecond,
  // advances a durable identity independent of the WebView clock. Rollback
  // of the containing transaction restores the previous serial atomically.
  if (kind === 'START') {
    const prior = previousWorld?.publishedAdventureRunSerial;
    if (prior !== undefined && (!Number.isSafeInteger(prior) || prior < 1))
      throw new Error('Contador de execução publicada anterior inválido.');
    const nextSerial = (prior ?? 0) + 1;
    if (!Number.isSafeInteger(nextSerial)) throw new Error('Contador de execução publicada esgotado.');
    next.publishedAdventureRunSerial = nextSerial;
  }
  // START deliberately replaces the run while the old UAE journal is still
  // present in the adapter's intermediate world; reconcile it below first.
  if (kind === 'START') validatePublishedActionReceipts(next);
  else validatePublishedActionCausality(next);
  const progress = next.publishedAdventure;
  if (!progress || progress.id !== adapter.id) throw new Error('Progresso de aventura incompatível com o adaptador.');
  const module = publishedAdventureRuntimeModule(adapter.meta);
  const runtime = kind === 'START' ? new AdventureRuntime(module) : loadRuntime(module, previousWorld);
  const previousNodeId = runtime.state.currentNodeId;
  const newNodeId = progress.currentNodeId;
  if (!own(module.nodes, newNodeId)) throw new Error(`Cena publicada inexistente: ${newNodeId}`);

  // One accepted adapter action (choice, clock changes, episode milestones,
  // and journal) must either produce one valid checkpoint or no checkpoint.
  const checkpoint = runtime.transaction(() => {
    if (kind === 'CHOICE') {
      const choice = module.nodes[previousNodeId]?.choices?.find(c => c.id === String(choiceId));
      if (!choice || choice.to !== newNodeId) throw new Error(`Divergência entre escolha publicada e grafo UAE: ${choiceId}`);
      let mirrored = false;
      try {
        // Legacy condition evaluation can include source-specific checks not
        // implemented by UAE. In that case preserve the legal legacy decision
        // with a visible, auditable reconciliation event.
        hydrateLegacy(runtime, previousWorld?.publishedAdventure ?? {}, { copyNode: false });
        runtime.state.status = 'ACTIVE';
        runtime.transition(String(choiceId));
        mirrored = true;
      } catch (error) {
        if (!/Condição para escolha/.test(String(error.message))) throw error;
      }
      if (!mirrored) {
        runtime.state.currentNodeId = newNodeId;
        runtime.state.currentLocationId = module.nodes[newNodeId]?.locationId ?? runtime.state.currentLocationId;
        if (!runtime.state.visitedNodeIds.includes(newNodeId)) runtime.state.visitedNodeIds.push(newNodeId);
        runtime.dispatchEvent('NODE_ENTER', { from: previousNodeId, to: newNodeId, choiceId, legacyCondition: true }, 'adapter');
        runtime.dispatchEvent('NODE_TRANSITION', { from: previousNodeId, to: newNodeId, choiceId, legacyCondition: true }, 'adapter');
        runtime.dispatchEvent('ADAPTER_CONDITION_RECONCILED', { choiceId, from: previousNodeId, to: newNodeId }, 'adapter');
      }
    } else if (kind !== 'START') {
      runtime.dispatchEvent(`ADAPTER_${kind}`, { nodeId: newNodeId, ...clone(asRecord(payload)) }, 'adapter');
    }

    const beforeClocks = clone(runtime.state.clocks);
    hydrateLegacy(runtime, progress, { copyNode: true });
    for (const [id, clock] of Object.entries(runtime.state.clocks)) {
      const before = beforeClocks[id];
      if (!before || clock.value === before.value) continue;
      runtime.dispatchEvent('CLOCK_ADVANCED', { clockId: id, value: clock.value, max: clock.max, status: clock.status }, 'adapter');
      if (Number(before.value) < Number(clock.max) && Number(clock.value) >= Number(clock.max))
        runtime.dispatchEvent('CLOCK_COMPLETED', { clockId: id, value: clock.value }, 'adapter');
    }
    if (kind === 'START') {
      runtime.dispatchEvent('NODE_ENTER', { from: null, to: newNodeId }, 'adapter');
      runtime.dispatchEvent('ADAPTER_STARTED', { nodeId: newNodeId }, 'adapter');
    }
    const completed = normalizeStatus(progress.status) === 'COMPLETED';
    if (completed && !runtime.state.canonicalEvents.some(e => e.type === 'ADVENTURE_COMPLETED')) {
      runtime.dispatchEvent('ADVENTURE_COMPLETED', { nodeId: newNodeId }, 'adapter');
    }
    if (normalizeStatus(progress.status) === 'ARCHIVED' && !runtime.state.canonicalEvents.some(e => e.type === 'ADVENTURE_ARCHIVED')) {
      runtime.dispatchEvent('ADVENTURE_ARCHIVED', { adventureId: module.id }, 'adapter');
    }
    reconcileLegacyMilestones(runtime, progress, 'adapter');
    const checkpoint = runtime.getState();
    validatePublishedAdventureRuntimeState(module, checkpoint, progress);
    return checkpoint;
  });
  next.publishedAdventureRuntime = checkpoint;
  return next;
}

export function publishedAdventureUaeSnapshot(world, adapter) {
  const module = publishedAdventureRuntimeModule(adapter.meta);
  const saved = world?.publishedAdventureRuntime;
  if (saved && saved.adventureId !== adapter.id) return null;
  if (!saved && world?.publishedAdventure?.id !== adapter.id) return null;
  // A legacy save must have a safe, useful player projection even before the
  // next player action persists the migrated UAE journal.
  const progress = world?.publishedAdventure;
  const drift = saved && progress?.id === adapter.id &&
    (saved.currentNodeId !== progress.currentNodeId || saved.status !== normalizeStatus(progress.status));
  if (saved && Number(saved.schemaVersion) >= 2) validatePublishedAdventureRuntimeState(module, saved);
  const runtime = !saved || drift ? loadRuntime(module, world) : AdventureRuntime.restore(module, saved);
  const snapshot = runtime.getState();
  validatePublishedAdventureRuntimeState(module, snapshot, progress?.id === adapter.id ? progress : null);
  return snapshot;
}

/** Player-safe projection: never expose flags, trigger payloads, GM secrets or
 * unvisited episode objectives through the Android scene bridge. */
export function publishedAdventureUaePublicStatus(world, adapter) {
  const state = publishedAdventureUaeSnapshot(world, adapter);
  if (!state) return null;
  const episode = Number(adapter.meta.nodes[state.currentNodeId]?.episode) || 1;
  const objectives = Object.values(state.objectives).filter(objective => {
    // The authored module is the visibility authority, never imported save data.
    if (adapter.meta.initialObjectives?.[objective.id]?.type === 'HIDDEN' || objective.type === 'HIDDEN') return false;
    const match = String(objective.id).match(/^episode-(\d+)$/);
    return !match || Number(match[1]) <= episode || objective.status === 'COMPLETED';
  }).map(objective => ({ id: objective.id, title: objective.title, status: objective.status,
    progress: objective.progress, target: objective.target }));
  const clocks = Object.entries(state.clocks).filter(([id]) =>
    adapter.meta.initialClocks?.[id]?.visibleToPlayer === true
  ).map(([id, clock]) => ({ id, value: clock.value, max: clock.max, status: clock.status }));
  return { schemaVersion: state.schemaVersion, status: state.status,
    currentNodeId: state.currentNodeId, objectives, clocks };
}

/** Mirror a confirmed task into the UAE journal of the active published
 * adventure. A rejected UAE commit never changes the supplied world object.
 * This is NOT an automatic world tick or a new encounter/reward rule. */
let taskTimeModuleResolver = null;
export function installPublishedTaskTimeModuleResolver(resolver) {
  if (typeof resolver !== 'function') throw new Error('Resolver UAE inválido.');
  taskTimeModuleResolver = resolver;
}
export function recordPublishedAdventureTaskTime(world, record) {
  validateTaskTimeLedger(world);
  if (!record || world.taskTimeApplications?.[record.checkId]?.checkId !== record.checkId ||
      world.taskTimeApplications[record.checkId].skillKey !== record.skillKey ||
      world.taskTimeApplications[record.checkId].minutes !== record.minutes ||
      world.taskTimeApplications[record.checkId].baseMinutes !== record.baseMinutes)
    throw new Error('Tarefa sem recibo confirmado no mundo.');
  const progress = world?.publishedAdventure;
  if (!progress || !['active', 'paused'].includes(String(progress.status).toLowerCase())) return world;
  if (!world?.publishedAdventureRuntime) return world; // legacy snapshot: no fabricated UAE history
  const saved = world.publishedAdventureRuntime;
  if (saved.adventureId !== progress.id) throw new Error('Tarefa: checkpoint de aventura incompatível.');
  // Use the same official adapter metadata as the five-adventure registry.
  // Imported lazily through a callback parameter would obscure failures;
  // the module is resolved from the source-backed catalog here.
  if (!taskTimeModuleResolver) throw new Error('Catálogo de aventuras indisponível.');
  const module = publishedAdventureRuntimeModule(taskTimeModuleResolver(progress.id));
  validatePublishedAdventureRuntimeState(module, saved, progress);
  const runtime = AdventureRuntime.restore(module, saved);
  runtime.dispatchEvent('TASK_TIME_COMMITTED', {
    checkId: record.checkId, skillKey: record.skillKey,
    elapsedMinutes: record.minutes, baseMinutes: record.baseMinutes
  }, 'world-time', { idempotencyKey: record.checkId });
  const next = clone(world);
  next.publishedAdventureRuntime = runtime.getState();
  validatePublishedAdventureRuntimeState(module, next.publishedAdventureRuntime, progress);
  validateTaskTimeLedger(next);
  return next;
}
