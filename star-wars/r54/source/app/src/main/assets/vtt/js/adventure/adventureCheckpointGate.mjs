/** P136 — reusable integrity gate for imported and committed UAE checkpoints.
 * Source adapters retain their own stricter content-specific validation.
 * This gate rejects journal corruption and impossible cross-field states before
 * the Android WebView persists them, without rewriting legitimate history.
 */
import { ADVENTURE_SCHEMA_VERSION, ADVENTURE_STATUSES, CLOCK_STATUSES, OBJECTIVE_TYPES } from './adventureSchema.mjs';
import { auditUaeJournalCausality } from './uaeJournalCausality.mjs';

const own = (o, k) => Object.prototype.hasOwnProperty.call(o, k);
const record = v => v !== null && typeof v === 'object' && !Array.isArray(v);
const safe = s => typeof s === 'string' && s.trim() && !['__proto__','prototype','constructor'].includes(s);
const finite = n => typeof n === 'number' && Number.isFinite(n);
const validDate = s => typeof s === 'string' && Number.isFinite(Date.parse(s));

export function validateAdventureCheckpoint(module, state) {
  if (!record(state) || state.adventureId !== module?.id || state.schemaVersion !== ADVENTURE_SCHEMA_VERSION)
    throw new Error('Checkpoint UAE incompatível ou sem versão válida.');
  if (!ADVENTURE_STATUSES.includes(state.status) || !own(module.nodes, state.currentNodeId))
    throw new Error('Checkpoint UAE com status ou cena inválida.');
  if (!Array.isArray(state.visitedNodeIds) || !state.visitedNodeIds.includes(state.currentNodeId) ||
      new Set(state.visitedNodeIds).size !== state.visitedNodeIds.length ||
      state.visitedNodeIds.some(id => !safe(id) || !own(module.nodes, id)))
    throw new Error('Histórico de cenas UAE inválido.');
  for (const key of ['flags','counters','triggersFired','objectives','clocks','systems']) {
    if (!record(state[key])) throw new Error(`Dicionário UAE inválido: ${key}`);
    if (Object.keys(state[key]).some(id => !safe(id))) throw new Error(`Chave UAE inválida: ${key}`);
  }
  if (Object.values(state.counters).some(n => !finite(n))) throw new Error('Contadores UAE inválidos.');
  const triggers = new Map((module.triggers || []).map(t => [t.id, t]));
  for (const [id, count] of Object.entries(state.triggersFired)) {
    const def = triggers.get(id);
    if (!def || !Number.isSafeInteger(count) || count < 0 || (def.maxFires && count > def.maxFires))
      throw new Error(`Gatilho UAE inválido: ${id}`);
  }
  for (const key of ['consequences','canonicalEvents','discoveredClues','revealedSecrets','completedObjectives','failedObjectives','checkHistory'])
    if (!Array.isArray(state[key])) throw new Error(`Lista UAE inválida: ${key}`);
  for (const key of ['consequences','discoveredClues','revealedSecrets']) {
    const ids = new Set();
    for (const entry of state[key]) {
      if (!record(entry) || !safe(entry.id) || ids.has(entry.id)) throw new Error(`Registro UAE inválido ou duplicado: ${key}`);
      ids.add(entry.id);
    }
  }
  for (const consequence of state.consequences) {
    if (typeof consequence.resolved !== 'boolean' || !validDate(consequence.recordedAt) ||
        (consequence.resolved && !validDate(consequence.resolvedAt)))
      throw new Error('Consequência UAE sem histórico consistente.');
  }
  const completed = new Set(state.completedObjectives), failed = new Set(state.failedObjectives);
  if (completed.size !== state.completedObjectives.length || failed.size !== state.failedObjectives.length)
    throw new Error('Objetivos UAE duplicados.');
  for (const id of [...completed, ...failed]) if (!safe(id) || !own(state.objectives, id))
    throw new Error('Objetivo UAE desconhecido no histórico.');
  for (const [id, obj] of Object.entries(state.objectives)) {
    const authored = module.initialObjectives?.[id];
    if (!authored || obj.id !== id || obj.title !== (authored.title || id) ||
        obj.type !== (OBJECTIVE_TYPES.includes(authored.type) ? authored.type : 'PRIMARY') ||
        obj.target !== (Number(authored.target) > 0 ? Number(authored.target) : 1))
      throw new Error(`Objetivo UAE adulterado ou não definido: ${id}`);
    // P168: a valid-looking imported objective must still respect the authored
    // dependency graph. Historical journals alone cannot prove prerequisites:
    // pre-audit checkpoints may not contain completion events.
    if (obj.status === 'COMPLETED' && (authored.requires || []).some(
        dependency => state.objectives[dependency]?.status !== 'COMPLETED'))
      throw new Error(`Objetivo UAE concluído sem pré-requisitos: ${id}`);
    if (!record(obj) || !['ACTIVE','COMPLETED','FAILED'].includes(obj.status) ||
        !finite(obj.target) || obj.target <= 0 || !finite(obj.progress) || obj.progress < 0 || obj.progress > obj.target ||
        // Completed objectives cannot be granted with partial progress, and
        // active objectives cannot silently remain active at their target.
        (obj.status === 'COMPLETED' && obj.progress !== obj.target) ||
        (obj.status === 'ACTIVE' && obj.progress >= obj.target) ||
        (obj.status === 'COMPLETED') !== completed.has(id) || (obj.status === 'FAILED') !== failed.has(id))
      throw new Error(`Objetivo UAE inconsistente: ${id}`);
  }
  for (const [id, clock] of Object.entries(state.clocks)) {
    if (!record(clock) || !finite(clock.max) || clock.max <= 0 || !finite(clock.value) ||
        clock.value < 0 || clock.value > clock.max || !CLOCK_STATUSES.includes(clock.status) ||
        (clock.status === 'COMPLETED' && clock.value !== clock.max) ||
        (clock.status === 'ACTIVE' && clock.value === clock.max))
      throw new Error(`Relógio UAE inconsistente: ${id}`);
  }
  if (!Number.isSafeInteger(state.eventSequence) || state.eventSequence < 0)
    throw new Error('Sequência UAE inválida.');
  const prefix = `evt_${module.id}_`;
  let previous = 0;
  const commandIds = new Set();
  for (const event of state.canonicalEvents) {
    if (!record(event) || typeof event.id !== 'string' || !event.id.startsWith(prefix))
      throw new Error('Journal UAE contém evento estrangeiro.');
    const suffix = event.id.slice(prefix.length);
    const sequence = /^\d+$/.test(suffix) ? Number(suffix) : NaN;
    if (!Number.isSafeInteger(sequence) || sequence <= previous || sequence > state.eventSequence ||
        !safe(event.type) || !safe(event.source) || !validDate(event.timestamp) ||
        !Number.isSafeInteger(event.turn) || event.turn < 1 || !record(event.payload) ||
        !Array.isArray(event.consequenceIds) || event.consequenceIds.some(id => !safe(id)))
      throw new Error('Journal UAE corrompido ou fora de ordem.');
    if (own(event, 'idempotencyKey')) {
      if (typeof event.idempotencyKey !== 'string' ||
          !/^[a-zA-Z0-9][a-zA-Z0-9_.:/-]{0,127}$/.test(event.idempotencyKey))
        throw new Error('Chave de idempotência UAE inválida no journal.');
      const commandId = `${event.source}\u0000${event.idempotencyKey}`;
      if (commandIds.has(commandId)) throw new Error('Journal UAE contém comando duplicado.');
      commandIds.add(commandId);
    }
    previous = sequence;
  }
  if (own(state, 'consequenceProvenanceFromSequence') &&
      (!Number.isSafeInteger(state.consequenceProvenanceFromSequence) || state.consequenceProvenanceFromSequence < 1 ||
       state.consequenceProvenanceFromSequence > state.eventSequence + 1))
    throw new Error('Marco de proveniência UAE inválido.');
  if (own(state, 'causalAuditFromSequence') &&
      (!Number.isSafeInteger(state.causalAuditFromSequence) || state.causalAuditFromSequence < 1 ||
       state.causalAuditFromSequence > state.eventSequence + 1))
    throw new Error('Versão causal UAE inválida.');
  auditUaeJournalCausality(module, state);
  return true;
}
