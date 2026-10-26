/** Braseiro Adventures V1 — portable, deterministic core. No IO or AI dependencies. */
export const SCHEMA = 'braseiro.adventure/v1';
const clone = x => structuredClone(x);
const plain = x => !!x && typeof x === 'object' && !Array.isArray(x);
const isStr = x => typeof x === 'string' && x.trim().length > 0;
const assert = (p, msg) => { if (!p) throw new Error(`BRASEIRO_ADVENTURE: ${msg}`); };
const uniq = xs => new Set(xs).size === xs.length;
const arr = x => Array.isArray(x) ? x : [];
const validId = x => isStr(x) && x.length <= 160 && /^[a-zA-Z0-9][a-zA-Z0-9._:-]*$/.test(x);
const stringList = x => Array.isArray(x) && x.every(isStr);
const validRefs = (list, label) => {
  assert(Array.isArray(list), `${label} deve ser uma lista`);
  assert(list.every(x => plain(x) && validId(x.id)), `${label} exige ids válidos`);
  assert(uniq(list.map(x => x.id)), `${label} contém id duplicado`);
};

export function validateAdventure(adventure) {
  assert(plain(adventure), 'JSON deve ser um objeto');
  assert(adventure.schema === SCHEMA, `schema deve ser ${SCHEMA}`);
  assert(validId(adventure.id), 'id inválido');
  assert(validId(adventure.system), 'system inválido');
  assert(validId(adventure.worldId), 'worldId inválido');
  assert(isStr(adventure.title), 'title obrigatório');
  assert(plain(adventure.story) && isStr(adventure.story.premise), 'story.premise obrigatório');
  assert(isStr(adventure.story.background), 'story.background obrigatório');
  assert(isStr(adventure.start?.locationId), 'start.locationId obrigatório');
  assert(plain(adventure.start), 'start obrigatório');
  for (const k of ['locations', 'npcs', 'enemies', 'objectives', 'secrets', 'encounters']) validRefs(adventure[k] ?? [], k);
  assert(arr(adventure.locations).some(l => l.id === adventure.start.locationId), 'local inicial inexistente');
  const refs = new Set(arr(adventure.locations).map(x => x.id));
  for (const en of arr(adventure.encounters)) {
    if (en.locationId) assert(refs.has(en.locationId), `encontro ${en.id} aponta para local inexistente`);
    assert(arr(en.enemies).every(x => arr(adventure.enemies).some(e => e.id === x)), `encontro ${en.id}: inimigo inexistente`);
  }
  for (const enemy of arr(adventure.enemies)) {
    assert(plain(enemy.rulesRef) && isStr(enemy.rulesRef.source), `inimigo ${enemy.id}: rulesRef.source obrigatório`);
  }
  for (const secret of arr(adventure.secrets)) {
    assert(secret.visibility === 'gm_only', `segredo ${secret.id} deve ser gm_only`);
    assert(isStr(secret.truth), `segredo ${secret.id}: truth obrigatório`);
  }
  assert(JSON.stringify(adventure).length <= 2_000_000, 'aventura excede tamanho permitido (2 MB)');
  return clone(adventure);
}

/** A character's legacy survives adventure switches. Storage is provided by the host. */
export function createLegacyStore({system, worldId, timelineId='principal'}) {
  for (const [k, v] of Object.entries({system, worldId, timelineId})) assert(validId(v), `${k} inválido`);
  return {schema: 'braseiro.legacy/v1',system,worldId,timelineId,revision:0,characters:{},worldEvents:[],adventures:{},processedEventIds:[]};
}

export function beginAdventure(store, input, partyCharacterIds) {
  const adv = validateAdventure(input);
  assert(store?.schema === 'braseiro.legacy/v1', 'legado inválido');
  assert(store.system === adv.system && store.worldId === adv.worldId, 'universo incompatível');
  assert(Array.isArray(partyCharacterIds) && partyCharacterIds.length > 0 && partyCharacterIds.every(validId) && uniq(partyCharacterIds), 'partyCharacterIds inválidos');
  assert(!store.adventures[adv.id], 'aventura já importada; retome-a em vez de duplicar');
  const out = clone(store);
  for (const characterId of partyCharacterIds) {
    out.characters[characterId] ??= {characterId,events:[]};
  }
  out.adventures[adv.id] = {id:adv.id,title:adv.title,status:'active',participantIds:[...partyCharacterIds],currentLocationId:adv.start.locationId,
    revealedSecretIds:[],completedObjectiveIds:[],failedObjectiveIds:[],eventIds:[],startedAt:new Date().toISOString(),endedAt:null};
  out.revision++;
  return out;
}

/** The host must verify outcomes via deterministic gameplay/state, not through AI text or imported JSON. */
export function applyVerifiedEvent(store, event, {verifiedByHost=false, expectedRevision}={}) {
  assert(verifiedByHost === true, 'evento sem verificação do motor de regras');
  assert(expectedRevision === undefined || expectedRevision === store.revision, 'conflito de revisão; recarregue o estado');
  assert(plain(event) && validId(event.id) && validId(event.adventureId), 'evento/id inválido');
  const active = store.adventures?.[event.adventureId];
  assert(active && active.status === 'active', 'aventura inativa ou inexistente');
  assert(event.system === store.system && event.worldId === store.worldId && event.timelineId === store.timelineId, 'evento pertence a outro mundo/sistema/linha temporal');
  assert(['creature_defeated','life_saved','place_saved','promise_made','promise_broken','faction_changed','title_gained','reputation_changed','item_acquired','character_died','other'].includes(event.type), 'tipo de evento não suportado');
  assert(isStr(event.summary) && isStr(event.occurredAt) && isStr(event.locationId), 'evento sem resumo, data ou local');
  assert(Array.isArray(event.actors) && event.actors.length > 0, 'evento sem participantes');
  assert(event.actors.every(a => plain(a) && validId(a.characterId) && isStr(a.role)), 'ator inválido');
  assert(event.actors.every(a => active.participantIds.includes(a.characterId)), 'personagem não participou da aventura');
  assert(stringList(event.witnessIds ?? []), 'testemunhas inválidas');
  assert(stringList(event.knownRegionIds ?? []), 'regiões inválidas');
  assert(plain(event.subject) && validId(event.subject.id) && isStr(event.subject.name), 'alvo inválido');
  if (store.processedEventIds.includes(event.id)) {
    const existing = store.worldEvents.find(x => x.id === event.id);
    assert(JSON.stringify(existing) === JSON.stringify(event), 'id de evento reutilizado com conteúdo diferente');
    return clone(store); // idempotent replay
  }
  const out=clone(store), frozenEvent=clone(event);
  out.worldEvents.push(frozenEvent);
  out.processedEventIds.push(event.id);
  out.adventures[event.adventureId].eventIds.push(event.id);
  for (const actor of event.actors) out.characters[actor.characterId].events.push(event.id);
  out.revision++;
  return out;
}

export function markAdventureProgress(store, adventureId, {completedObjectiveIds=[],failedObjectiveIds=[],revealedSecretIds=[],locationId}={}) {
  const existing = store.adventures?.[adventureId];
  assert(existing?.status === 'active', 'aventura não ativa');
  const out=clone(store), state=out.adventures[adventureId];
  const appendUnique = (dst, items) => {assert(Array.isArray(items) && items.every(validId), 'ids de progresso inválidos'); for (const x of items) if(!dst.includes(x)) dst.push(x);};
  appendUnique(state.completedObjectiveIds,completedObjectiveIds);
  appendUnique(state.failedObjectiveIds,failedObjectiveIds);
  appendUnique(state.revealedSecretIds,revealedSecretIds);
  assert(!state.completedObjectiveIds.some(x => state.failedObjectiveIds.includes(x)), 'objetivo não pode estar concluído e falhado');
  if (locationId) {assert(validId(locationId),'local inválido'); state.currentLocationId=locationId;}
  out.revision++;
  return out;
}

export function finishAdventure(store, adventureId, {status='completed',endingSummary}={}) {
  assert(['completed','failed','abandoned'].includes(status), 'status de encerramento inválido');
  assert(isStr(endingSummary), 'resumo do encerramento obrigatório');
  const prev=store.adventures?.[adventureId];
  assert(prev?.status === 'active', 'aventura não está ativa');
  const out=clone(store);
  out.adventures[adventureId] = {...prev,status,endingSummary,endedAt:new Date().toISOString()};
  out.revision++;
  return out;
}

/** Facts are true; rumor/reach is separately scoped. Output does not expose unrevealed secrets. */
export function buildCharacterLegacyContext(store, characterId, {regionId, maxEvents=24}={}) {
  const char = store.characters?.[characterId];
  if (!char) return {characterId, canonicalFacts:[],publiclyKnown:[],unknownLocally:[]};
  const evs=char.events.map(id=>store.worldEvents.find(e=>e.id===id)).filter(Boolean).slice(-maxEvents);
  return {characterId,
    canonicalFacts:evs.map(e=>({id:e.id,type:e.type,summary:e.summary,role:e.actors.find(a=>a.characterId===characterId)?.role,occurredAt:e.occurredAt})),
    publiclyKnown:evs.filter(e=>!regionId || arr(e.knownRegionIds).includes(regionId)).map(e=>({id:e.id,summary:e.publicStory ?? e.summary,verifiedTruth:e.summary})),
    unknownLocally:regionId ? evs.filter(e=>!arr(e.knownRegionIds).includes(regionId)).map(e=>e.id):[]};
}

/** Separate GM-only facts from player-visible starting context. */
export function buildAdventureNarrationContext(input, store, characterIds, regionId) {
  const adv=validateAdventure(input);
  assert(store.system === adv.system && store.worldId === adv.worldId,'mundo incompatível');
  const state=store.adventures[adv.id];
  assert(state,'aventura não carregada');
  const publicStory={id:adv.id,title:adv.title,premise:adv.story.premise,publicBackground:adv.story.playerIntroduction ?? '',locationId:state.currentLocationId,
    completedObjectiveIds:state.completedObjectiveIds,participantLegacy:characterIds.map(id=>buildCharacterLegacyContext(store,id,{regionId}))};
  const gmOnly={background:adv.story.background,secrets:arr(adv.secrets),npcs:arr(adv.npcs),enemies:arr(adv.enemies),objectives:arr(adv.objectives),encounters:arr(adv.encounters)};
  return {playerContext:publicStory,masterContext:gmOnly};
}

/** Persistence adapter: caller must write the returned store atomically (SQLite/IndexedDB). */
export function assertLegacyStore(store) {
  assert(plain(store) && store.schema === 'braseiro.legacy/v1', 'arquivo de legado incompatível');
  assert(validId(store.system) && validId(store.worldId) && validId(store.timelineId), 'universo do legado inválido');
  assert(Number.isInteger(store.revision) && store.revision >= 0, 'revisão inválida');
  assert(plain(store.characters) && plain(store.adventures) && Array.isArray(store.worldEvents) && Array.isArray(store.processedEventIds),'estrutura inválida');
  assert(uniq(store.worldEvents.map(e=>e.id)), 'eventos duplicados');
  assert(uniq(store.processedEventIds),'ids de eventos duplicados');
  return clone(store);
}
