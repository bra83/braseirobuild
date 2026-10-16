/** Universal Adventure Engine — validated, versioned state contract (v2). */
export const ADVENTURE_SCHEMA_VERSION = 2;
export const CAMPAIGN_MODES = Object.freeze(['EOTE', 'AOR', 'MIXED']);
export const OBJECTIVE_TYPES = Object.freeze(['PRIMARY', 'SECONDARY', 'OPTIONAL', 'HIDDEN']);
export const CLOCK_STATUSES = Object.freeze(['ACTIVE', 'COMPLETED', 'EXPIRED', 'PAUSED']);
export const ADVENTURE_STATUSES = Object.freeze(['NOT_STARTED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED', 'ARCHIVED']);

const isRecord = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const own = (object, key) => Object.prototype.hasOwnProperty.call(object, key);
const safeId = id => typeof id === 'string' && Boolean(id.trim()) && !['__proto__', 'constructor', 'prototype'].includes(id);
const positive = (value, fallback = 1) => Number.isFinite(Number(value)) && Number(value) > 0 ? Number(value) : fallback;

export function createCanonicalEvent({ id, type, turn = 1, source = 'system', payload = {}, consequences = [], idempotencyKey = null } = {}) {
  return {
    id: id || `evt_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    type: String(type || 'GENERIC_EVENT'),
    timestamp: new Date().toISOString(),
    turn: Number(turn) || 1,
    source: String(source || 'system'),
    payload: structuredClone(payload),
    consequenceIds: Array.isArray(consequences) ? [...consequences] : [],
    ...(idempotencyKey === null ? {} : { idempotencyKey })
  };
}

export function validateAdventureModule(module) {
  if (!isRecord(module)) throw new Error('Módulo de aventura inválido.');
  if (!safeId(module.id)) throw new Error('Módulo de aventura deve possuir um "id" string.');
  if (typeof module.title !== 'string' || !module.title.trim()) throw new Error(`Aventura "${module.id}" deve possuir um "title".`);
  if (!Array.isArray(module.campaignModes) || !module.campaignModes.length) throw new Error(`Aventura "${module.id}" deve declarar "campaignModes" suportados.`);
  for (const mode of module.campaignModes) if (!CAMPAIGN_MODES.includes(mode)) throw new Error(`Modo de campanha inválido "${mode}" na aventura "${module.id}".`);
  if (!safeId(module.startNodeId)) throw new Error(`Aventura "${module.id}" deve definir "startNodeId".`);
  if (!isRecord(module.nodes) || !own(module.nodes, module.startNodeId)) throw new Error(`"startNodeId" (${module.startNodeId}) não existe nos nós da aventura "${module.id}".`);

  for (const [nodeId, node] of Object.entries(module.nodes)) {
    if (!safeId(nodeId)) throw new Error(`Identificador de nó inválido: ${nodeId}`);
    if (!isRecord(node)) throw new Error(`Nó "${nodeId}" inválido.`);
    if (node.choices !== undefined && !Array.isArray(node.choices)) throw new Error(`Escolhas do nó "${nodeId}" devem ser lista.`);
    const ids = new Set();
    for (const choice of node.choices || []) {
      if (!isRecord(choice) || !safeId(choice.id) || ids.has(choice.id)) throw new Error(`Escolha inválida ou duplicada no nó "${nodeId}".`);
      ids.add(choice.id);
      if (typeof choice.to !== 'string' || !own(module.nodes, choice.to)) throw new Error(`Nó "${nodeId}" aponta para destino inexistente "${choice.to}" na aventura "${module.id}".`);
      if (choice.condition !== undefined && typeof choice.condition !== 'string' && !Array.isArray(choice.condition)) throw new Error(`Condição inválida na escolha "${choice.id}".`);
    }
  }
  if (module.initialClocks !== undefined && !isRecord(module.initialClocks)) throw new Error('Relógios iniciais inválidos.');
  for (const [id, clock] of Object.entries(module.initialClocks || {})) {
    if (!safeId(id) || !isRecord(clock) || !Number.isFinite(Number(clock.max)) || Number(clock.max) <= 0) throw new Error(`Relógio "${id}" deve possuir max positivo.`);
  }
  if (module.initialObjectives !== undefined && !isRecord(module.initialObjectives)) throw new Error('Objetivos iniciais inválidos.');
  for (const [id, objective] of Object.entries(module.initialObjectives || {})) {
    if (!safeId(id) || !isRecord(objective) || (objective.target !== undefined && !(Number(objective.target) > 0))) throw new Error(`Objetivo "${id}" inválido.`);
  }
  if (module.triggers !== undefined && !Array.isArray(module.triggers)) throw new Error('Gatilhos devem ser lista.');
  const triggerIds = new Set();
  for (const trigger of module.triggers || []) {
    if (!isRecord(trigger) || !safeId(trigger.id) || triggerIds.has(trigger.id) || !safeId(trigger.event)) throw new Error('Gatilho inválido ou duplicado.');
    triggerIds.add(trigger.id);
    if (trigger.maxFires !== undefined && (!Number.isInteger(trigger.maxFires) || trigger.maxFires < 1)) throw new Error(`maxFires inválido em "${trigger.id}".`);
  }
  return true;
}

function objectiveFromDefinition(id, def) {
  return { id, title: def.title || id, type: OBJECTIVE_TYPES.includes(def.type) ? def.type : 'PRIMARY', status: 'ACTIVE', progress: 0, target: positive(def.target) };
}

export function migrateAdventureState(state, module = null) {
  if (!isRecord(state)) throw new Error('Snapshot de aventura inválido.');
  const migrated = structuredClone(state);
  if (module && migrated.adventureId && migrated.adventureId !== module.id) throw new Error('Snapshot pertence a outra aventura.');
  if (Number(migrated.schemaVersion || 1) > ADVENTURE_SCHEMA_VERSION) throw new Error('Snapshot de versão futura não suportada.');
  if (!migrated.schemaVersion || migrated.schemaVersion < ADVENTURE_SCHEMA_VERSION) {
    migrated.schemaVersion = ADVENTURE_SCHEMA_VERSION;
    if (!migrated.recoveredAt) migrated.recoveredAt = new Date().toISOString();
  }
  migrated.adventureId ||= module?.id;
  migrated.flags = isRecord(migrated.flags) ? migrated.flags : {};
  migrated.counters = isRecord(migrated.counters) ? migrated.counters : {};
  migrated.triggersFired = isRecord(migrated.triggersFired) ? migrated.triggersFired : {};
  migrated.objectives = isRecord(migrated.objectives) ? migrated.objectives : {};
  migrated.clocks = isRecord(migrated.clocks) ? migrated.clocks : {};
  migrated.systems = isRecord(migrated.systems) ? migrated.systems : {};
  for (const field of ['consequences','visitedNodeIds','canonicalEvents','discoveredClues','revealedSecrets','completedObjectives','failedObjectives','checkHistory']) {
    if (!Array.isArray(migrated[field])) migrated[field] = [];
  }
  if (module) {
    if (!own(module.nodes, migrated.currentNodeId)) throw new Error(`Snapshot aponta para nó inexistente: ${migrated.currentNodeId}`);
    for (const [id, def] of Object.entries(module.initialObjectives || {})) if (!own(migrated.objectives, id)) migrated.objectives[id] = objectiveFromDefinition(id, def);
    for (const [id, clock] of Object.entries(module.initialClocks || {})) if (!own(migrated.clocks, id)) migrated.clocks[id] = structuredClone(clock);
  }
  for (const objective of Object.values(migrated.objectives)) {
    objective.target = positive(objective.target);
    objective.progress = Math.max(0, Math.min(objective.target, Number(objective.progress) || 0));
  }
  for (const clock of Object.values(migrated.clocks)) {
    clock.max = positive(clock.max, 6);
    clock.value = Math.max(0, Math.min(clock.max, Number(clock.value) || 0));
    clock.status ||= clock.value >= clock.max ? 'COMPLETED' : 'ACTIVE';
  }
  // Imported journals can be truncated or have gaps. Use the highest actual
  // canonical ID rather than array length to avoid ID reuse after restore.
  const prefix = `evt_${migrated.adventureId}_`;
  const lastId = migrated.canonicalEvents.reduce((highest, event) => {
    const id = String(event?.id ?? '');
    const suffix = id.startsWith(prefix) ? id.slice(prefix.length) : '';
    return /^\d+$/.test(suffix) ? Math.max(highest, Number(suffix)) : highest;
  }, 0);
  migrated.eventSequence = Math.max(0, Math.floor(Number(migrated.eventSequence) || 0), migrated.canonicalEvents.length, lastId);
  if (!migrated.visitedNodeIds.includes(migrated.currentNodeId)) migrated.visitedNodeIds.push(migrated.currentNodeId);
  return migrated;
}

export function createInitialAdventureState(module) {
  validateAdventureModule(module);
  const objectives = Object.fromEntries(Object.entries(module.initialObjectives || {}).map(([id, def]) => [id, objectiveFromDefinition(id, def)]));
  return {
    adventureId: module.id, schemaVersion: ADVENTURE_SCHEMA_VERSION, moduleVersion: module.contentVersion || 1,
    status: 'ACTIVE', currentNodeId: module.startNodeId,
    currentLocationId: module.nodes[module.startNodeId]?.locationId || null,
    visitedNodeIds: [module.startNodeId], flags: {}, counters: {}, triggersFired: {}, consequences: [],
    clocks: structuredClone(module.initialClocks || {}), objectives,
    discoveredClues: [], revealedSecrets: [], completedObjectives: [], failedObjectives: [],
    eventSequence: 1, causalAuditFromSequence: 1, consequenceProvenanceFromSequence: 1,
    canonicalEvents: [createCanonicalEvent({ id: `evt_${module.id}_1`, type: 'ADVENTURE_STARTED', source: 'engine', payload: { adventureId: module.id, startNodeId: module.startNodeId } })],
    checkHistory: [], systems: structuredClone(module.defaultSystems || {}),
    startedAt: new Date().toISOString(), updatedAt: new Date().toISOString(), completedAt: null
  };
}
