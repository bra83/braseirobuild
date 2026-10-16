/** P148 — executable preflight for the authored UAE condition/effect language.
 * A structurally reachable edge is not playable when its condition is a typo.
 * This gate rejects unsupported expressions before an Android campaign starts;
 * it never fabricates a roll, encounter, branch, or book rule.
 */
const own = (o, k) => Object.prototype.hasOwnProperty.call(o, k);
const identifier = x => typeof x === 'string' && /^[A-Za-z0-9][A-Za-z0-9_.:-]*$/.test(x) &&
  !['__proto__', 'prototype', 'constructor'].includes(x);
const numeric = x => typeof x === 'number' && Number.isFinite(x) ||
  typeof x === 'string' && /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(x.trim()) && Number.isFinite(Number(x));
const record = x => x !== null && typeof x === 'object' && !Array.isArray(x);
const requireId = (x, label) => { if (!identifier(x)) throw Error(`UAE semantic: ${label} inválido: ${String(x)}`); };
// Mirror the runtime's bounded condition grammar. A condition that passes the
// authoring gate must never become permanently false only because it is too
// deep/long for the Android evaluator. Limit expansion before recursing.
const RESERVED_EVENTS = new Set(['TRIGGER_FIRED', 'TRIGGER_CASCADE_LIMIT',
  'CONSEQUENCE_RECORDED', 'CONSEQUENCE_RESOLVED', 'OBJECTIVE_COMPLETED',
  'OBJECTIVE_FAILED', 'ADVENTURE_ACTION_COMMITTED', 'PUBLISHED_COMMAND_COMMITTED']);
function assertExternalEvent(type, path) {
  requireId(type, path);
  if (RESERVED_EVENTS.has(type))
    throw Error(`UAE semantic: ${path} tenta emitir evento reservado ao motor.`);
}


export function validateAuthoredCondition(condition, module, path = 'condition', depth = 0, nested = false) {
  if (depth > 16) throw Error(`UAE semantic: ${path} excede profundidade permitida.`);
  if (condition === undefined || condition === null || condition === '') {
    if (nested) throw Error(`UAE semantic: ${path} contém condição vazia.`);
    return 0;
  }
  if (Array.isArray(condition)) {
    if (!condition.length || condition.length > 64) throw Error(`UAE semantic: ${path} lista fora dos limites.`);
    return condition.reduce((n, c, i) => n + validateAuthoredCondition(c, module, `${path}[${i}]`, depth + 1, true), 0);
  }
  if (typeof condition !== 'string' || condition.length > 2048) throw Error(`UAE semantic: ${path} não é condição válida.`);
  if (condition.startsWith('all:') || condition.startsWith('any:')) {
    const parts = condition.slice(4).split('|');
    if (!parts.length || parts.length > 32 || parts.some(x => !x))
      throw Error(`UAE semantic: ${path} contém termos inválidos.`);
    return parts.reduce((n, c, i) => n + validateAuthoredCondition(c, module, `${path}.${i}`, depth + 1, true), 0);
  }
  if (['prep-ready', 'kaato-outcome:neutralized', 'kaato-outcome:not-neutralized'].includes(condition)) {
    const allowed = condition === 'prep-ready' ? module.id === 'beyond-the-rim' : module.id === 'under-black-sun';
    if (!allowed) throw Error(`UAE semantic: ${path} é condição específica de outra aventura.`);
    return 1;
  }
  const colon = condition.indexOf(':');
  if (colon < 1) throw Error(`UAE semantic: ${path} desconhecida: ${condition}`);
  const op = condition.slice(0, colon), tail = condition.slice(colon + 1);
  if (['visited', 'unvisited', 'check-success'].includes(op)) {
    if (!own(module.nodes, tail)) throw Error(`UAE semantic: ${path} referencia cena inexistente ${tail}.`);
  } else if (['flag', '!flag', 'encounter-resolved'].includes(op)) {
    requireId(tail, path);
  } else if (op === 'investigated') {
    if (!/^(0|[1-9]\d*)$/.test(tail)) throw Error(`UAE semantic: ${path} exige inteiro não negativo.`);
  } else if (['clock', 'counter'].includes(op)) {
    const match = /^([^:]+):(>=|<=|>|<|=):(.+)$/.exec(tail);
    if (!match || !identifier(match[1]) || !numeric(match[3]))
      throw Error(`UAE semantic: ${path} tem comparação inválida.`);
    if (op === 'clock' && !own(module.initialClocks || {}, match[1]))
      throw Error(`UAE semantic: ${path} referencia relógio não declarado ${match[1]}.`);
  } else if (op === 'objective') {
    const match = /^([^:]+):(ACTIVE|COMPLETED|FAILED)$/.exec(tail);
    if (!match || !own(module.initialObjectives || {}, match[1]))
      throw Error(`UAE semantic: ${path} referencia objetivo/estado inválido.`);
  } else if (op === 'payload') {
    const match = /^([^:]+):(.+)$/.exec(tail);
    if (!match || !identifier(match[1])) throw Error(`UAE semantic: ${path} compara payload inválido.`);
  } else throw Error(`UAE semantic: ${path} usa operador não suportado ${op}.`);
  return 1;
}

const supportedEffects = new Set(['set-flag', 'clear-flag', 'set-counter', 'adjust-counter',
  'advance-clock', 'emit-event', 'complete-objective', 'fail-objective',
  'progress-objective', 'add-clue', 'reveal-secret']);
export function validateAuthoredEffects(effects, module, path = 'effects') {
  if (effects === undefined || effects === null) return 0;
  if (Array.isArray(effects)) {
    if (effects.length > 128) throw Error(`UAE semantic: ${path} excede limite de efeitos.`);
    return effects.reduce((n, effect, index) => {
    const where = `${path}[${index}]`;
    if (!record(effect)) throw Error(`UAE semantic: ${where} não é efeito válido.`);
    if (Object.hasOwn(effect, 'type') && (typeof effect.type !== 'string' || !effect.type))
      throw Error(`UAE semantic: ${where} tem tipo inválido.`);
    const type = effect.type || 'set-flag';
    if (!supportedEffects.has(type)) throw Error(`UAE semantic: ${where} tem efeito não suportado ${type}.`);
    validateAuthoredCondition(effect.condition, module, `${where}.condition`);
    if (['set-flag','clear-flag'].includes(type)) requireId(effect.flag, where);
    if (['set-counter','adjust-counter'].includes(type)) {
      requireId(effect.counter, where);
      if (!numeric(type === 'set-counter' ? effect.value : effect.delta ?? 1)) throw Error(`UAE semantic: ${where} tem contador inválido.`);
    }
    if (type === 'advance-clock') {
      if (!own(module.initialClocks || {}, effect.clockId) || !numeric(effect.delta ?? 1))
        throw Error(`UAE semantic: ${where} usa relógio/delta inválido.`);
    }
    if (['complete-objective','fail-objective','progress-objective'].includes(type)) {
      if (!own(module.initialObjectives || {}, effect.objectiveId)) throw Error(`UAE semantic: ${where} usa objetivo inexistente.`);
      if (type === 'progress-objective' && !numeric(effect.delta ?? 1)) throw Error(`UAE semantic: ${where} tem progresso inválido.`);
    }
    if (type === 'emit-event') {
      assertExternalEvent(effect.eventType ?? 'CUSTOM_EVENT', where);
      if (Object.hasOwn(effect, 'source') && effect.source !== 'effect')
        throw Error(`UAE semantic: ${where} não pode falsificar origem de evento.`);
    }
    if (type === 'add-clue') {
      requireId(effect.clueId, where);
      if ((effect.title !== undefined && typeof effect.title !== 'string') ||
          (effect.summary !== undefined && typeof effect.summary !== 'string'))
        throw Error(`UAE semantic: ${where} contém pista inválida.`);
    }
    if (type === 'reveal-secret') {
      requireId(effect.secretId, where);
      if (effect.description !== undefined && typeof effect.description !== 'string')
        throw Error(`UAE semantic: ${where} contém segredo inválido.`);
    }
    return n + 1;
  }, 0);
  }
  if (!record(effects)) throw Error(`UAE semantic: ${path} deve ser objeto ou lista.`);
  let count = 0;
  for (const [key, value] of Object.entries(effects)) {
    count++;
    if (key === 'clear-flag') requireId(value, path);
    else if (key === 'set-counter') {
      if (!record(value)) throw Error(`UAE semantic: ${path} define contador inválido.`);
      requireId(value.counter, path);
      if (!numeric(value.value)) throw Error(`UAE semantic: ${path} tem valor inválido.`);
    } else if (key === 'emit-event') {
      if (!record(value)) throw Error(`UAE semantic: ${path} emite evento inválido.`);
      assertExternalEvent(value.type || 'CUSTOM_EVENT', path);
      if (Object.hasOwn(value, 'source') && value.source !== 'effect')
        throw Error(`UAE semantic: ${path} não pode falsificar origem de evento.`);
    } else if (key === 'objective-progress') {
      if (!record(value) || !own(module.initialObjectives || {}, value.objectiveId) || !numeric(value.delta ?? 1))
        throw Error(`UAE semantic: ${path} altera objetivo inválido.`);
    } else if (key === 'complete-objective' || key === 'fail-objective') {
      const id = typeof value === 'string' ? value : value?.objectiveId;
      if (!own(module.initialObjectives || {}, id)) throw Error(`UAE semantic: ${path} usa objetivo inexistente.`);
    } else requireId(key, `${path}.flag`);
  }
  return count;
}

/** Validate source-authored consequences at load time rather than after a
 * player has already advanced into the triggering scene. Runtime-owned fields
 * are never accepted from adventure content. */
export function validateAuthoredConsequence(consequence, path = 'consequence', module = null) {
  if (consequence === undefined || consequence === null) return 0;
  if (typeof consequence === 'string') {
    requireId(consequence, path);
    return 1;
  }
  if (!record(consequence)) throw Error(`UAE semantic: ${path} inválida.`);
  requireId(consequence.id, path);
  for (const key of ['resolved', 'recordedAt', 'resolvedAt', 'authored', 'triggerId', 'causeEventId'])
    if (own(consequence, key)) throw Error(`UAE semantic: ${path} contém campo interno ${key}.`);
  for (const [key, value] of Object.entries(consequence)) {
    if (key === 'onResolveEffects') {
      if (!module) throw Error(`UAE semantic: ${path} sem módulo para efeitos de resolução.`);
      validateAuthoredEffects(value, module, `${path}.onResolveEffects`);
      continue;
    }
    if (key === 'resolveWhen') {
      if (!module) throw Error(`UAE semantic: ${path} sem módulo para condição de resolução.`);
      validateAuthoredCondition(value, module, `${path}.resolveWhen`);
      continue;
    }
    requireId(key, `${path}.${key}`);
    if (value !== null && !['string','boolean','number'].includes(typeof value))
      throw Error(`UAE semantic: ${path}.${key} deve ser valor simples.`);
    if (typeof value === 'number' && !Number.isFinite(value))
      throw Error(`UAE semantic: ${path}.${key} não pode ser infinito.`);
  }
  return 1;
}

export function validateObjectiveDependencies(module) {
  // Optional source-authored objective prerequisites are a DAG. They cannot
  // be bypassed by direct commands, nested triggers or imported checkpoints.
  const objectives = module.initialObjectives || {};
  const visiting = new Set(), visited = new Set();
  const visit = id => {
    if (visiting.has(id)) throw Error(`UAE semantic: dependência circular de objetivo ${id}.`);
    if (visited.has(id)) return;
    visiting.add(id);
    const def = objectives[id];
    if (def.requires !== undefined) {
      if (!Array.isArray(def.requires) || def.requires.length > 32 ||
          new Set(def.requires).size !== def.requires.length)
        throw Error(`UAE semantic: pré-requisitos inválidos em ${id}.`);
      for (const dependency of def.requires) {
        if (!identifier(dependency) || !own(objectives, dependency) || dependency === id)
          throw Error(`UAE semantic: pré-requisito inexistente em ${id}.`);
        visit(dependency);
      }
    }
    visiting.delete(id); visited.add(id);
  };
  for (const id of Object.keys(objectives)) visit(id);
}

export function validateAdventureSemantics(module) {
  validateObjectiveDependencies(module);
  let conditions = 0, effects = 0, checks = 0, choices = 0;
  for (const [nodeId, node] of Object.entries(module.nodes || {})) {
    for (const [index, choice] of (node.choices || []).entries()) {
      choices++;
      conditions += validateAuthoredCondition(choice.condition, module, `${nodeId}.choices[${index}]`);
      effects += validateAuthoredEffects(choice.effects, module, `${nodeId}.choices[${index}].effects`);
    }
    effects += validateAuthoredEffects(node.onEnter, module, `${nodeId}.onEnter`);
    if (node.checks !== undefined && !Array.isArray(node.checks))
      throw Error(`UAE semantic: ${nodeId}.checks deve ser lista.`);
    for (const [index, check] of (node.checks || []).entries()) {
      if (!record(check) || !['skill','difficulty','purpose'].every(key =>
          typeof check[key] === 'string' && check[key].trim()))
        throw Error(`UAE semantic: ${nodeId}.checks[${index}] incompleto ou inválido.`);
      checks++;
    }
  }
  const consequenceSources = new Map();
  for (const trigger of module.triggers || []) {
    // Two triggers may legitimately point to the same consequence, but must
    // never supply different resolution scripts under the same persistent ID.
    const consequence = trigger.consequence;
    const consequenceId = typeof consequence === 'string' ? consequence : consequence?.id;
    if (consequenceId) {
      const script = JSON.stringify(typeof consequence === 'string' ? {} : {
        resolveWhen: consequence.resolveWhen ?? null,
        onResolveEffects: consequence.onResolveEffects ?? null
      });
      if (consequenceSources.has(consequenceId) && consequenceSources.get(consequenceId) !== script)
        throw Error(`UAE semantic: consequência ${consequenceId} tem scripts conflitantes.`);
      consequenceSources.set(consequenceId, script);
    }
    conditions += validateAuthoredCondition(trigger.condition, module, `${trigger.id}.condition`);
    effects += validateAuthoredEffects(trigger.effects, module, `${trigger.id}.effects`);
    validateAuthoredConsequence(trigger.consequence, `${trigger.id}.consequence`, module);
  }
  return { adventureId: module.id, nodes: Object.keys(module.nodes || {}).length,
    choices, checks, conditions, effects, triggers: (module.triggers || []).length };
}
