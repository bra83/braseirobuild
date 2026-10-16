/**
 * P141 — Causal audit for the durable UAE journal.
 * No new game rules or narrative are inferred from the audit. Its purpose is
 * to reject forged internal events and inconsistent trigger/consequence/objective
 * references before Android persists or reopens a checkpoint.
 */
const own = (value, key) => Object.prototype.hasOwnProperty.call(value, key);
const record = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const validId = value => typeof value === 'string' && value.trim() !== '' &&
  !['__proto__', 'constructor', 'prototype'].includes(value);

export function auditUaeJournalCausality(module, state) {
  const triggers = new Map((module.triggers || []).map(t => [t.id, t]));
  const fired = new Map();
  const precedingTypes = new Set();
  const precedingEvents = new Map();
  const triggerCausePairs = new Set();
  const clockAdvances = new Map();
  const internalTypes = new Set(['TRIGGER_FIRED', 'TRIGGER_CASCADE_LIMIT',
    'CONSEQUENCE_RECORDED', 'CONSEQUENCE_RESOLVED', 'OBJECTIVE_COMPLETED', 'OBJECTIVE_FAILED',
    'ADVENTURE_ACTION_COMMITTED']);
  const consequences = new Map((state.consequences || []).map(c => [c.id, c]));
  const objectives = state.objectives || {};
  const clocks = state.clocks || {};
  const recorded = new Set();
  const resolved = new Set();
  const completed = new Set();
  const failed = new Set();
  let auditEvents = 0;
  const committedActions = new Set();
  let lastReceiptSequence = 0;

  for (const event of state.canonicalEvents || []) {
    const payload = event.payload;
    if (!record(payload)) throw new Error('Journal UAE: payload inválido.');
    if (internalTypes.has(event.type) && event.source !== 'engine')
      throw new Error('Journal UAE: evento interno com origem falsificada.');
    if (event.type === 'TRIGGER_FIRED' && state.causalAuditFromSequence !== undefined &&
        Number(event.id.slice(`evt_${module.id}_`.length)) >= state.causalAuditFromSequence &&
        !Object.hasOwn(payload, 'causeEventId'))
      throw new Error('Journal UAE: gatilho sem vínculo causal obrigatório.');
    switch (event.type) {
      case 'ADVENTURE_ACTION_COMMITTED': {
        const prefix = `evt_${module.id}_`;
        const seq = Number(event.id.slice(prefix.length));
        const { actionId, commands, firstSequence, lastSequence, appliedEventIds } = payload;
        if (event.source !== 'engine' || !validId(actionId) ||
            !/^[a-zA-Z0-9][a-zA-Z0-9_.:/-]{0,127}$/.test(actionId) ||
            event.idempotencyKey !== actionId ||
            !Array.isArray(commands) || commands.length < 1 || commands.length > 32 ||
            commands.some(command => !record(command) ||
              !['event', 'effects', 'advance-clock', 'progress-objective',
                'complete-objective', 'fail-objective', 'record-consequence',
                'resolve-consequence', 'transition'].includes(command.type) ||
              (command.type === 'event' && (command.source === 'engine' ||
                internalTypes.has(command.eventType)))) ||
            !Number.isSafeInteger(firstSequence) || !Number.isSafeInteger(lastSequence) ||
            firstSequence < 1 || lastSequence !== seq - 1 || firstSequence > seq ||
            !Array.isArray(appliedEventIds))
          throw new Error('Journal UAE: recibo de ação inválido.');
        if (committedActions.has(actionId))
          throw new Error('Journal UAE: identificador de ação duplicado.');
        // Nested or forged receipts must not claim events already covered by
        // a previous committed action. Legitimate actions are sequential.
        if (firstSequence <= lastReceiptSequence)
          throw new Error('Journal UAE: recibos de ação com janelas sobrepostas.');
        committedActions.add(actionId);
        lastReceiptSequence = seq;
        // The receipt must enumerate EXACTLY the real preceding events from
        // this transaction, including those emitted by cascading triggers.
        if (lastSequence - firstSequence > 4096)
          throw new Error('Journal UAE: recibo de ação excede o limite causal.');
        const expected = [];
        for (let sequence = firstSequence; sequence <= lastSequence; sequence++) {
          const id = `${prefix}${sequence}`;
          if (!precedingEvents.has(id))
            throw new Error('Journal UAE: recibo de ação contém lacuna causal.');
          expected.push(id);
        }
        if (expected.length !== appliedEventIds.length ||
            expected.some((id, index) => id !== appliedEventIds[index]))
          throw new Error('Journal UAE: recibo de ação diverge dos eventos aplicados.');
        auditEvents++;
        break;
      }
      case 'TRIGGER_FIRED': {
        const def = triggers.get(payload.triggerId);
        if (event.source !== 'engine' || !def || def.event !== payload.eventType ||
            !precedingTypes.has(payload.eventType))
          throw new Error('Journal UAE: gatilho sem evento causal válido.');
        if (Object.hasOwn(payload, 'causeEventId')) {
          const cause = precedingEvents.get(payload.causeEventId);
          if (!cause || cause.type !== def.event || cause.type !== payload.eventType ||
              cause.type === 'TRIGGER_FIRED')
            throw new Error('Journal UAE: vínculo causal de gatilho inválido.');
          const pair = `${def.id}\u0000${payload.causeEventId}`;
          if (triggerCausePairs.has(pair))
            throw new Error('Journal UAE: gatilho disparado duas vezes pelo mesmo evento.');
          triggerCausePairs.add(pair);
        }
        fired.set(def.id, (fired.get(def.id) || 0) + 1);
        if (def.maxFires && fired.get(def.id) > def.maxFires)
          throw new Error('Journal UAE: gatilho excedeu máximo de disparos.');
        auditEvents++;
        break;
      }
      case 'CONSEQUENCE_RECORDED': {
        if (!validId(payload.consequenceId) || !consequences.has(payload.consequenceId) ||
            recorded.has(payload.consequenceId))
          throw new Error('Journal UAE: consequência registrada inválida ou duplicada.');
        // P169: imported P168 journals are grandfathered until the first new
        // transaction; every later authored consequence must point to its
        // actual prior source event and an exact authored trigger definition.
        const seq = Number(event.id.slice(`evt_${module.id}_`.length));
        if (state.consequenceProvenanceFromSequence !== undefined &&
            seq >= state.consequenceProvenanceFromSequence) {
          const authored = (module.triggers || []).filter(t =>
            (typeof t.consequence === 'string' ? t.consequence : t.consequence?.id) === payload.consequenceId);
          if (payload.authored !== Boolean(authored.length))
            throw new Error('Journal UAE: origem da consequência adulterada.');
          if (authored.length) {
            const trigger = authored.find(t => t.id === payload.triggerId);
            const cause = precedingEvents.get(payload.causeEventId);
            if (!trigger || !cause || cause.type !== trigger.event ||
                !validId(payload.causeEventId))
              throw new Error('Journal UAE: consequência sem gatilho causal comprovado.');
          } else if (Object.hasOwn(payload,'triggerId') || Object.hasOwn(payload,'causeEventId'))
            throw new Error('Journal UAE: consequência avulsa falsificou gatilho.');
        }
        recorded.add(payload.consequenceId);
        auditEvents++;
        break;
      }
      case 'CONSEQUENCE_RESOLVED': {
        if (!validId(payload.consequenceId) || !consequences.get(payload.consequenceId)?.resolved ||
            resolved.has(payload.consequenceId))
          throw new Error('Journal UAE: resolução de consequência inválida.');
        resolved.add(payload.consequenceId);
        auditEvents++;
        break;
      }
      case 'OBJECTIVE_COMPLETED': {
        if (!validId(payload.objectiveId) || !own(objectives, payload.objectiveId) ||
            completed.has(payload.objectiveId))
          throw new Error('Journal UAE: conclusão de objetivo inválida.');
        completed.add(payload.objectiveId);
        auditEvents++;
        break;
      }
      case 'OBJECTIVE_FAILED': {
        if (!validId(payload.objectiveId) || !own(objectives, payload.objectiveId) ||
            failed.has(payload.objectiveId))
          throw new Error('Journal UAE: falha de objetivo inválida.');
        failed.add(payload.objectiveId);
        auditEvents++;
        break;
      }
      case 'CLOCK_ADVANCED': {
        if (state.causalAuditFromSequence !== undefined &&
            Number(event.id.slice(`evt_${module.id}_`.length)) >= state.causalAuditFromSequence) {
          if (!validId(payload.clockId) || !own(clocks, payload.clockId) ||
              typeof payload.max !== 'number' || !Number.isFinite(payload.max) || payload.max <= 0 ||
              typeof payload.value !== 'number' || !Number.isFinite(payload.value) ||
              payload.value < 0 || payload.value > payload.max ||
              !['ACTIVE','COMPLETED','EXPIRED','PAUSED'].includes(payload.status))
            throw new Error('Journal UAE: avanço de relógio inválido.');
          clockAdvances.set(payload.clockId, payload);
        }
        break;
      }
      case 'CLOCK_COMPLETED': {
        if (!validId(payload.clockId) || !own(clocks, payload.clockId) ||
            typeof payload.value !== 'number' || !Number.isFinite(payload.value))
          throw new Error('Journal UAE: conclusão de relógio inválida.');
        if (state.causalAuditFromSequence !== undefined &&
            Number(event.id.slice(`evt_${module.id}_`.length)) >= state.causalAuditFromSequence) {
          const advance = clockAdvances.get(payload.clockId);
          if (!advance || advance.status !== 'COMPLETED' || advance.value !== payload.value ||
              advance.value !== advance.max)
            throw new Error('Journal UAE: conclusão de relógio sem avanço causal.');
        }
        auditEvents++;
        break;
      }
      default:
        break;
    }
    precedingTypes.add(event.type);
    precedingEvents.set(event.id, event);
  }
  // Older snapshots may contain counters without a full causal journal. If
  // trigger audit events exist, however, they must account for EVERY counter:
  // mixing audited and unaudited counters cannot be produced by this runtime.
  // Fully audited runs must also reject a forged trigger counter when the
  // attacker removes ALL TRIGGER_FIRED events. Previously fired.size=0
  // skipped the correspondence check, allowing a zero-journal counter grant.
  if (fired.size || state.causalAuditFromSequence === 1) {
    for (const [id, count] of Object.entries(state.triggersFired || {})) {
      if (count !== (fired.get(id) || 0))
        throw new Error(`Journal UAE: contador de gatilho sem histórico: ${id}`);
    }
    for (const [id, count] of fired) {
      if ((state.triggersFired || {})[id] !== count)
        throw new Error(`Journal UAE: gatilho registrado sem contador: ${id}`);
    }
  }
  for (const id of resolved) if (!recorded.has(id)) {
    // Pre-P141 snapshots can legitimately have a consequence that predates the
    // available journal; do not fabricate a CON​SEQUENCE_RECORDED event.
    if (state.canonicalEvents?.some(e => e.type === 'CONSEQUENCE_RECORDED'))
      throw new Error(`Journal UAE: resolução sem registro: ${id}`);
  }
  for (const id of completed) if (failed.has(id))
    throw new Error(`Journal UAE: objetivo concluído e falhado: ${id}`);
  // A checkpoint born under the fully audited schema (sequence 1) has no
  // historical unjournaled mutations. Requiring the reverse correspondence
  // prevents an imported save from granting completed objectives or silently
  // resolving consequences by editing only the snapshot arrays. Legacy saves
  // use a later causalAuditFromSequence (or none), so their prior history is
  // intentionally not reconstructed or falsely rejected here.
  if (state.causalAuditFromSequence === 1) {
    for (const consequence of state.consequences || []) {
      if (!recorded.has(consequence.id))
        throw new Error(`Journal UAE: consequência sem evento de registro: ${consequence.id}`);
      if (consequence.resolved !== resolved.has(consequence.id))
        throw new Error(`Journal UAE: resolução de consequência sem evento: ${consequence.id}`);
    }
    for (const id of state.completedObjectives || []) if (!completed.has(id))
      throw new Error(`Journal UAE: objetivo concluído sem evento: ${id}`);
    for (const id of state.failedObjectives || []) if (!failed.has(id))
      throw new Error(`Journal UAE: objetivo falhado sem evento: ${id}`);
  }
  return { events: state.canonicalEvents?.length || 0, audited: auditEvents,
    triggers: [...fired.values()].reduce((n, count) => n + count, 0),
    consequences: recorded.size, resolutions: resolved.size };
}
