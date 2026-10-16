/**
 * P145 — durable exactly-once command receipts for published adventures.
 * A receipt is committed in the same repository write as the character,
 * adventure, world, combat and UAE checkpoint. No UI/WebView memory is trusted.
 */
import { assertPersistable } from './adventureRuntime.mjs';

const own = (o, k) => Object.prototype.hasOwnProperty.call(o, k);
const validId = id => typeof id === 'string' && !['__proto__','prototype','constructor'].includes(id) && /^[A-Za-z0-9][A-Za-z0-9_.:/-]{0,127}$/.test(id);
const kinds = new Set(['start', 'choice', 'check', 'encounter', 'finish-combat', 'imperial-clock']);
const MAX_RECEIPTS = 4096;
const copy = value => structuredClone(value);

function canonical(value) {
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`;
  if (value && typeof value === 'object') return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${canonical(value[key])}`).join(',')}}`;
  return JSON.stringify(value);
}

export function publishedCommandRequest(kind, adventureId, args = {}) {
  if (!kinds.has(kind) || !validId(adventureId)) throw new Error('Comando de aventura publicado inválido.');
  assertPersistable(args, 'Argumentos de comando publicado');
  return canonical({ kind, adventureId, args });
}

/** Stable identity for one run of one published adventure. Historical
 * receipts from earlier runs remain valid but cannot be replayed as current.
 * New P146 receipts carry this fingerprint; P145 receipts are grandfathered.
 */
export function currentPublishedAdventureRunKey(world) {
  // P147: monotonic durable run identity; timestamps are not unique on fast
  // restarts and cannot be trusted as an exactly-once replay boundary.
  const serial = world?.publishedAdventureRunSerial;
  if (serial !== undefined) {
    if (!Number.isSafeInteger(serial) || serial < 1)
      throw new Error('Identificador persistente de execução publicada inválido.');
    const id = world?.publishedAdventureRuntime?.adventureId;
    if (!validId(id) || id !== world?.publishedAdventure?.id)
      throw new Error('Execução publicada sem identidade de aventura consistente.');
    return `run:${id}:${serial}`;
  }
  // Backward compatibility for P146 receipts and older snapshots.

  const runtime = world?.publishedAdventureRuntime;
  const progress = world?.publishedAdventure;
  if (!runtime || !progress || runtime.adventureId !== progress.id) return null;
  if (typeof runtime.startedAt !== 'string' || typeof progress.startedAt !== 'string') return null;
  return JSON.stringify([runtime.adventureId, runtime.startedAt, progress.startedAt]);
}

export function validatePublishedActionReceipts(world) {
  const serial = world?.publishedAdventureRunSerial;
  if (serial !== undefined && (!Number.isSafeInteger(serial) || serial < 1))
    throw new Error('Contador de execuções publicadas inválido.');
  const receipts = world?.publishedAdventureActionReceipts;
  if (receipts === undefined) return true; // P144 and earlier saves
  if (!Array.isArray(receipts) || receipts.length > MAX_RECEIPTS)
    throw new Error('Livro de recibos da aventura inválido.');
  assertPersistable(receipts, 'Recibos de aventura');
  const ids = new Set();
  for (let index = 0; index < receipts.length; index++) {
    const receipt = receipts[index];
    if (!receipt || !validId(receipt.id) || ids.has(receipt.id) ||
        !kinds.has(receipt.kind) || !validId(receipt.adventureId) ||
        typeof receipt.request !== 'string' || !receipt.request ||
        (receipt.runKey !== undefined && (typeof receipt.runKey !== 'string' || receipt.runKey.length > 256 || !receipt.runKey)) ||
        (receipt.proofVersion !== undefined && receipt.proofVersion !== 2) ||
        !Number.isSafeInteger(receipt.sequence) || receipt.sequence !== index + 1 ||
        (typeof receipt.committedAt !== 'string' || !Number.isFinite(Date.parse(receipt.committedAt))) ||
        !receipt.response || typeof receipt.response !== 'object' || Array.isArray(receipt.response))
      throw new Error('Recibo publicado corrompido ou duplicado.');
    ids.add(receipt.id);
    let decoded;
    try { decoded = JSON.parse(receipt.request); } catch { throw new Error('Assinatura de recibo inválida.'); }
    if (!decoded || decoded.kind !== receipt.kind || decoded.adventureId !== receipt.adventureId ||
        canonical(decoded) !== receipt.request)
      throw new Error('Assinatura de recibo divergente.');
  }
  return true;
}

/**
 * P146: cross-check controller receipts against the UAE canonical journal.
 * A receipt alone is not proof that an adventure action committed. Old receipts
 * from a deliberately restarted adventure are retained but are not mistaken
 * for commands in the current run; each event in the CURRENT journal must
 * have exactly one corresponding receipt.
 */
export function validatePublishedActionCausality(world) {
  validatePublishedActionReceipts(world);
  const events = world?.publishedAdventureRuntime?.canonicalEvents;
  if (events === undefined) return true; // pre-UAE and legacy worlds
  if (!Array.isArray(events)) throw new Error('Journal UAE de comandos inválido.');
  const receipts = world?.publishedAdventureActionReceipts ?? [];
  const byId = new Map(receipts.map(receipt => [receipt.id, receipt]));
  const observed = new Set();
  let lastSequence = 0;
  const activeRunKey = currentPublishedAdventureRunKey(world);
  for (const event of events) {
    if (event?.type !== 'PUBLISHED_COMMAND_COMMITTED') continue;
    const id = event?.payload?.actionId;
    const receipt = byId.get(id);
    if (!receipt || event.source !== 'controller' || event.idempotencyKey !== id ||
        event.payload?.kind !== receipt.kind ||
        event.payload?.adventureId !== receipt.adventureId ||
        receipt.adventureId !== world.publishedAdventureRuntime.adventureId ||
        observed.has(id) || receipt.sequence <= lastSequence ||
        (receipt.runKey !== undefined && receipt.runKey !== activeRunKey) ||
        (receipt.proofVersion === 2 &&
          (event.payload?.proofVersion !== 2 || event.payload?.request !== receipt.request ||
           event.payload?.receiptSequence !== receipt.sequence ||
           event.payload?.runKey !== receipt.runKey)) ||
        (event.payload?.proofVersion === 2 && receipt.proofVersion !== 2))
      throw new Error('Prova causal de comando publicado divergente ou ausente.');
    observed.add(id);
    lastSequence = receipt.sequence;
  }
  // P146 receipts can be attributed to this exact run. A missing journal
  // event cannot be concealed by leaving an otherwise valid receipt behind.
  for (const receipt of receipts) {
    if (receipt.runKey === activeRunKey && !observed.has(receipt.id))
      throw new Error('Recibo da execução atual perdeu sua prova causal UAE.');
  }
  return true;
}

export function findPublishedActionReceipt(world, actionId, request) {
  if (!validId(actionId)) throw new Error('Identificador de ação publicada inválido.');
  validatePublishedActionCausality(world);
  const receipt = (world?.publishedAdventureActionReceipts ?? []).find(r => r.id === actionId);
  if (receipt && receipt.request !== request)
    throw new Error('Identificador de ação publicada reutilizado com comando diferente.');
  return receipt ? copy(receipt) : null;
}

export function appendPublishedActionReceipt(world, { actionId, kind, adventureId, request, response }) {
  if (!validId(actionId) || !kinds.has(kind) || !validId(adventureId) || typeof request !== 'string')
    throw new Error('Recibo de ação publicada inválido.');
  validatePublishedActionReceipts(world);
  const prior = world?.publishedAdventureActionReceipts ?? [];
  if (prior.some(r => r.id === actionId)) throw new Error('Recibo publicado já existe.');
  if (prior.length >= MAX_RECEIPTS)
    throw new Error('Limite de recibos atingido; exporte e inicie uma nova campanha antes de novas ações.');
  assertPersistable(response, 'Resultado de comando publicado');
  const runKey = currentPublishedAdventureRunKey(world);
  const receipt = { id: actionId, kind, adventureId, request, response: copy(response),
    sequence: prior.length + 1, committedAt: new Date().toISOString(), proofVersion: 2,
    ...(runKey ? { runKey } : {}) };
  const next = { ...copy(world), publishedAdventureActionReceipts: [...copy(prior), receipt] };
  validatePublishedActionReceipts(next);
  return { world: next, receipt: copy(receipt) };
}
