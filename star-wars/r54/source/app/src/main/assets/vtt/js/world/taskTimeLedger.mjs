/** P140 — durable, audited accounting for confirmed EotE/AoR task time.
 * Event turns and elapsed task minutes remain distinct. The audit rejects
 * corrupted saves rather than silently resetting or charging time twice.
 */
import { normalizeWorld } from './world.mjs';

const MAX_MINUTES = 1_000_000_000;
const validCheckId = value => typeof value === 'string' && /^task_[a-zA-Z0-9_-]{1,120}$/.test(value);
const validSkill = value => typeof value === 'string' && /^[a-z_]{1,80}$/.test(value);
const own = (value, key) => Object.prototype.hasOwnProperty.call(value, key);
const record = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const validMinutes = n => typeof n === 'number' && Number.isFinite(n) && n >= 0 && n <= MAX_MINUTES;
const sameAmount = (a, b) => Math.abs(a - b) <= Math.max(1, Math.abs(a), Math.abs(b)) * Number.EPSILON * 8;

/** Verify ALL records, their sum, and any persisted UAE world-time receipts.
 * Old saves with no task-time fields remain valid. Historical records need not
 * all have UAE receipts: time can precede the start of a published adventure.
 */
export function validateTaskTimeLedger(world, { verifyAdventureEvents = true } = {}) {
  if (!record(world) || !record(world.clock)) throw new Error('Relógio de tarefas corrompido.');
  const ledger = world.taskTimeApplications ?? {};
  if (!record(ledger)) throw new Error('Registro de tarefas corrompido.');
  const elapsed = world.clock.elapsedMinutes ?? 0;
  if (typeof elapsed !== 'number' || !Number.isFinite(elapsed) || elapsed < 0 ||
      elapsed * 1000 > Number.MAX_SAFE_INTEGER) throw new Error('Relógio de tarefas corrompido.');
  let total = 0;
  for (const [key, entry] of Object.entries(ledger)) {
    if (!validCheckId(key) || !record(entry) || entry.checkId !== key ||
        !validSkill(entry.skillKey) || !validMinutes(entry.minutes) ||
        !validMinutes(entry.baseMinutes) || entry.minutes > entry.baseMinutes ||
        Object.keys(entry).some(field => !['checkId', 'skillKey', 'minutes', 'baseMinutes'].includes(field)))
      throw new Error(`Registro de tarefa inválido: ${key}.`);
    total += entry.minutes;
    if (total * 1000 > Number.MAX_SAFE_INTEGER) throw new Error('Relógio de tarefas excedeu o limite de precisão.');
  }
  if (!sameAmount(total, elapsed)) throw new Error('Relógio de tarefas diverge do histórico confirmado.');
  if (verifyAdventureEvents && world.publishedAdventureRuntime) {
    const journal = world.publishedAdventureRuntime.canonicalEvents;
    if (!Array.isArray(journal)) throw new Error('Journal UAE de tarefas inválido.');
    const seen = new Set();
    for (const event of journal) {
      if (event?.type !== 'TASK_TIME_COMMITTED' || event?.source !== 'world-time') continue;
      const key = event.idempotencyKey;
      const entry = own(ledger, key) ? ledger[key] : null;
      if (!validCheckId(key) || seen.has(key) || !entry ||
          event.payload?.checkId !== key || event.payload?.skillKey !== entry.skillKey ||
          event.payload?.elapsedMinutes !== entry.minutes ||
          event.payload?.baseMinutes !== entry.baseMinutes)
        throw new Error('Journal UAE de tarefas diverge do registro confirmado.');
      seen.add(key);
    }
  }
  return { elapsedMinutes: elapsed, confirmedTasks: Object.keys(ledger).length };
}

export function recordResolvedTaskTime(world, { checkId, skillKey, minutes, baseMinutes } = {}) {
  if (!validCheckId(checkId)) throw new Error('Identificador de tarefa inválido.');
  if (!validSkill(skillKey)) throw new Error('Perícia da tarefa inválida.');
  if (!validMinutes(minutes) || !validMinutes(baseMinutes) || minutes > baseMinutes)
    throw new Error('Duração de tarefa inválida.');
  // Validate the ORIGINAL save, before normalizeWorld can coerce invalid data.
  validateTaskTimeLedger(world);
  const next = normalizeWorld(world);
  const ledger = next.taskTimeApplications ?? {};
  const elapsed = next.clock.elapsedMinutes ?? 0;
  const previous = own(ledger, checkId) ? ledger[checkId] : null;
  if (previous !== null) {
    if (previous.skillKey !== skillKey || previous.minutes !== minutes || previous.baseMinutes !== baseMinutes)
      throw new Error('Identificador de tarefa reutilizado com valores divergentes.');
    return { world: next, applied: false, elapsedMinutes: elapsed, record: structuredClone(previous) };
  }
  const total = elapsed + minutes;
  if (total * 1000 > Number.MAX_SAFE_INTEGER || !Number.isFinite(total))
    throw new Error('Relógio de tarefas excedeu o limite de precisão.');
  const receipt = { checkId, skillKey, minutes, baseMinutes };
  next.clock.elapsedMinutes = total;
  next.taskTimeApplications = { ...ledger, [checkId]: receipt };
  // During the UAE journal phase the new receipt is not mirrored yet; the
  // pre-existing journal still has to be internally consistent.
  validateTaskTimeLedger(next);
  return { world: next, applied: true, elapsedMinutes: total, record: structuredClone(receipt) };
}
