/**
 * BRASEIRO OSE — VTT Life Bridge & Adapter (Motor de Vida V2.2)
 *
 * Full integration of Motor de Vida V2.2:
 * - src/legacy-engine.mjs
 * - src/matrix-engine.mjs
 * - src/life-arcs.mjs
 * - src/matrix-automation.mjs
 * - src/life-engine.mjs
 * - src/runtime.mjs
 * - OSE Classic and OSE Advanced Matrix Packs
 */
(function(root) {
  'use strict';

  // --- 1. LEGACY ENGINE ---
  const LegacyEngine = (function() {
/** Braseiro Adventures V1 — portable, deterministic core. No IO or AI dependencies. */
const SCHEMA = 'braseiro.adventure/v1';
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

function validateAdventure(adventure) {
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
function createLegacyStore({system, worldId, timelineId='principal'}) {
  for (const [k, v] of Object.entries({system, worldId, timelineId})) assert(validId(v), `${k} inválido`);
  return {schema: 'braseiro.legacy/v1',system,worldId,timelineId,revision:0,characters:{},worldEvents:[],adventures:{},processedEventIds:[]};
}

function beginAdventure(store, input, partyCharacterIds) {
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
function applyVerifiedEvent(store, event, {verifiedByHost=false, expectedRevision}={}) {
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

function markAdventureProgress(store, adventureId, {completedObjectiveIds=[],failedObjectiveIds=[],revealedSecretIds=[],locationId}={}) {
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

function finishAdventure(store, adventureId, {status='completed',endingSummary}={}) {
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
function buildCharacterLegacyContext(store, characterId, {regionId, maxEvents=24}={}) {
  const char = store.characters?.[characterId];
  if (!char) return {characterId, canonicalFacts:[],publiclyKnown:[],unknownLocally:[]};
  const evs=char.events.map(id=>store.worldEvents.find(e=>e.id===id)).filter(Boolean).slice(-maxEvents);
  return {characterId,
    canonicalFacts:evs.map(e=>({id:e.id,type:e.type,summary:e.summary,role:e.actors.find(a=>a.characterId===characterId)?.role,occurredAt:e.occurredAt})),
    publiclyKnown:evs.filter(e=>!regionId || arr(e.knownRegionIds).includes(regionId)).map(e=>({id:e.id,summary:e.publicStory ?? e.summary,verifiedTruth:e.summary})),
    unknownLocally:regionId ? evs.filter(e=>!arr(e.knownRegionIds).includes(regionId)).map(e=>e.id):[]};
}

/** Separate GM-only facts from player-visible starting context. */
function buildAdventureNarrationContext(input, store, characterIds, regionId) {
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
function assertLegacyStore(store) {
  assert(plain(store) && store.schema === 'braseiro.legacy/v1', 'arquivo de legado incompatível');
  assert(validId(store.system) && validId(store.worldId) && validId(store.timelineId), 'universo do legado inválido');
  assert(Number.isInteger(store.revision) && store.revision >= 0, 'revisão inválida');
  assert(plain(store.characters) && plain(store.adventures) && Array.isArray(store.worldEvents) && Array.isArray(store.processedEventIds),'estrutura inválida');
  assert(uniq(store.worldEvents.map(e=>e.id)), 'eventos duplicados');
  assert(uniq(store.processedEventIds),'ids de eventos duplicados');
  return clone(store);
}
    return { SCHEMA, validateAdventure, createLegacyStore, beginAdventure, applyVerifiedEvent, markAdventureProgress, finishAdventure, buildCharacterLegacyContext, buildAdventureNarrationContext, assertLegacyStore };

  })();

  // --- 2. MATRIX ENGINE ---
  const MatrixEngine = (function() {
/** Hidden, deterministic, five-axis matrix with bounded draws and coherence checks. */
const assert=(x,m)=>{if(!x)throw Error(`BRASEIRO_MATRIX: ${m}`);};
const clone=x=>structuredClone(x);
const key=x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const AXES=['cause','manifestation','domain','timing','response'];
const aliases={'ose':'ose-classic','ose-fantasy':'ose-classic','dnd5e-2024':'dnd5e','oua':'o-um-anel','tor2e':'o-um-anel','pf2e':'pf2e-kingmaker','gurps-investigacao':'gurps','gurps-csi':'gurps','gurps-serial-killer':'gurps','star-wars-aor':'star-wars','star-wars-eote':'star-wars'};
const profiles={
 'gurps-investigacao':['gurps','investigacao'], 'gurps-csi':['gurps','csi'], 'gurps-serial-killer':['gurps','serial-killer'],
 'o-um-anel':['o-um-anel','default'],'dnd5e':['dnd5e','default'],'forbidden-lands':['forbidden-lands','default'],'pf2e-kingmaker':['pf2e-kingmaker','default'],
 'star-wars-aor':['star-wars','aor'],'star-wars-eote':['star-wars','eote'],
 'ose-classic':['ose-classic','classic'],'ose-advanced':['ose-advanced','advanced']
};
function selectMatrixProfile(packId,system){
 const profile=profiles[packId];assert(profile&&(aliases[system]??system)===profile[0],'perfil pertence a outro sistema');
 return {packId,system:profile[0],mode:profile[1]};
}
function validateMatrixBundle(bundle){
 assert(bundle?.schema==='braseiro.matrix-bundle/v1'&&key(bundle.pack?.id)&&typeof bundle.pack.version==='string','pacote inválido');
 assert(key(bundle.pack.system)&&key(bundle.pack.mode),'sistema/modalidade inválidos');
 assert(bundle.tables&&AXES.every(a=>Array.isArray(bundle.tables[a])&&bundle.tables[a].length>0&&bundle.tables[a].length<=10000),'tabelas incompletas');
 for(const axis of AXES){const seen=new Set();for(const row of bundle.tables[axis]){
  assert(key(row.id)&&!seen.has(row.id),'ID de entrada inválido/duplicado');seen.add(row.id);
  assert(typeof row.text==='string'&&row.text.length>0&&row.text.length<=4000,'texto inválido');
  assert(['verified_origin','context_only','narrative_opportunity','schedule_only','player_choice'].includes(row.agency),'agência inválida');
  assert(row.weight===undefined||(Number.isInteger(row.weight)&&row.weight>=1&&row.weight<=20),'peso inválido');
  assert((row.requiredCapabilities??[]).every(key),'capacidade inválida');
  if(row.minElapsedDays!==undefined)assert(Number.isSafeInteger(row.minElapsedDays)&&row.minElapsedDays>=0&&Number.isSafeInteger(row.maxElapsedDays)&&row.maxElapsedDays>=row.minElapsedDays,'janela inválida');
 }}
 return bundle;
}
function stageMatches(row,ctx){const elapsed=ctx.day-ctx.origin.day;return (row.minElapsedDays===undefined||(elapsed>=row.minElapsedDays&&elapsed<=row.maxElapsedDays))&&(!row.allowedStages||row.allowedStages.includes(ctx.stage));}
function reason(row,ctx,selection,axis){
 if(!stageMatches(row,ctx))return 'outside_time_window';
 if((row.requiredCapabilities??[]).some(x=>!ctx.capabilities.includes(x)))return 'missing_capability';
 if(row.setting&&row.setting!==ctx.packId)return 'wrong_setting';
 if(row.agency==='mechanical'||row.diagnosis||row.automaticVoluntaryAction)return 'forbidden_authority';
 if(row.intensity!==undefined&&row.intensity>ctx.maxIntensity)return 'intensity_limit';
 if(row.domain&&ctx.characterProfile?.domainWeights?.[row.domain]===0)return 'personal_domain_disabled';
 if(row.causeTags?.some(tag=>ctx.characterProfile?.causeWeights?.[tag]===0))return 'personal_cause_disabled';
 if(row.domain&&ctx.allowedDomains?.length&&!ctx.allowedDomains.includes(row.domain))return 'domain_limit';
 for(const [k,v] of Object.entries(row.requiresFacts??{}))if(!Object.hasOwn(ctx.facts,k)||!equal(ctx.facts[k],v))return 'required_canon_missing';
 for(const [k,v] of Object.entries(row.forbidsFacts??{}))if(Object.hasOwn(ctx.facts,k)&&equal(ctx.facts[k],v))return 'contradicts_canon';
 // A proposed claim is allowed only if canon already supports it. Nothing rewrites history.
 for(const [k,v] of Object.entries(row.assertedFacts??{}))if(!Object.hasOwn(ctx.facts,k)||!equal(ctx.facts[k],v))return 'unsupported_claim';
 if(axis==='cause'&&row.originCode!==ctx.origin.causeId)return 'different_origin';
 if(row.causeTags&&selection.cause&&!row.causeTags.some(t=>selection.cause.causeTags.includes(t)))return 'incompatible_cause';
 if(row.domain&&selection.manifestation&&row.domain!==selection.manifestation.domain)return 'incompatible_domain';
 if(axis==='manifestation'){
  const domainCaps={family:'family_contact',intimacy:'intimate_contact',home:'home_exists',npc:'related_person'};
  if(domainCaps[row.domain]&&!ctx.capabilities.includes(domainCaps[row.domain]))return 'missing_relationship_or_home';
  const matches=ctx.recent.filter(x=>x.packId===ctx.packId&&x.situationId===(row.baseSituationId??row.baseId??row.id)&&(!x.characterId&&!x.result?.characterId|| (x.characterId??x.result?.characterId)===ctx.characterId));
  const used=matches.reduce((latest,x)=>!latest||x.day>latest.day?x:latest,null);
  const arc=ctx.arcs?.find(a=>a.packId===ctx.packId&&a.characterId===ctx.characterId&&a.originEventId===ctx.origin.id&&a.situationId===(row.baseSituationId??row.baseId??row.id));
  if(arc&&['resolved','rejected'].includes(arc.status))return 'arc_closed';
  if(arc&&!ctx.revalidating&&['active','resurfaced','dormant','resolving'].includes(arc.status)&&arc.nextDueDay>ctx.day)return 'arc_not_due';
  if(used&&ctx.day-used.day<(row.cooldownDays??7))return 'repetition_cooldown';
 }
 return null;
}
function checkMatrixCoherence(bundle,selection,context){
 const ctx=normalizeContext(bundle,context), reasons=[];
 for(const axis of AXES){const row=selection[axis];if(!row){reasons.push({axis,reason:'missing_axis'});continue;}if(!bundle.tables[axis].some(x=>equal(x,row))){reasons.push({axis,reason:'foreign_row'});continue;}const why=reason(row,ctx,selection,axis);if(why)reasons.push({axis,reason:why});}
 return {valid:reasons.length===0,reasons};
}
function normalizeContext(bundle,c){
 assert(Number.isSafeInteger(c?.day)&&Number.isSafeInteger(c.origin?.day)&&c.day>=c.origin.day,'tempo/origem inválidos');
 assert(key(c.origin.id)&&key(c.origin.causeId)&&key(c.characterId),'identidade/origem inválida');
 assert((c.origin.actorIds??[]).includes(c.characterId)||(c.origin.witnessIds??[]).includes(c.characterId),'personagem sem vínculo com origem');
 assert((aliases[c.system]??c.system)===bundle.pack.system&&c.mode===bundle.pack.mode,'sistema ou modalidade incompatível');
 assert(Number.isInteger(c.maxIntensity??2)&&(c.maxIntensity??2)>=0&&(c.maxIntensity??2)<=5,'intensidade inválida');
 const elapsed=c.day-c.origin.day;
 const stage=elapsed<=1?'immediate':elapsed<=7?'settling':elapsed<=30?'established':elapsed<=365?'dormant':'long_term';
 return {...c,stage,packId:bundle.pack.id,facts:c.facts??{},capabilities:c.capabilities??[],recent:c.recent??[],maxIntensity:c.maxIntensity??2};
}
/** Two independent d100, interpreted as a base-100 number, never summed. */
function candidateWeight(row,context){
 const p=context.characterProfile??{};let value=(row.weight??1)*(context.characterProfile?4:1);
 if(row.domain)value*=(p.domainWeights?.[row.domain]??1)*(p.decisionWeights?.[row.domain]??1);
 if(['work','legacy','belief'].includes(row.domain)&&row.causeTags?.length){const experience=Math.max(...row.causeTags.map(t=>p.experience?.[t]??0));value*=1+Math.min(0.5,experience/10);}
 if(row.causeTags?.length)value*=Math.max(...row.causeTags.map(t=>p.causeWeights?.[t]??1));
 const protection=Math.min(5,Object.values(p.protections??{}).reduce((a,b)=>a+b,0));
 if(row.valence==='positive')value*=1+protection/5;
 if(row.valence==='challenging')value*=1-protection/10;
 return value<=0?0:Math.max(1,Math.min(20,Math.round(value)));
}
function drawWeighted(rows,state,axis,audit,context){
 const weights=rows.map(r=>candidateWeight(r,context)),total=weights.reduce((a,b)=>a+b,0);assert(total<=10000,'peso total excede 10.000');
 function d100(){state.value=(Math.imul(1664525,state.value)+1013904223)>>>0;return Math.floor(state.value/4294967296*100)+1;}
 for(let attempt=0;attempt<16;attempt++){
  const dice=[d100(),d100()],raw=(dice[0]-1)*100+dice[1]-1,cutoff=Math.floor(10000/total)*total;
  audit.push({axis,dice,raw,accepted:raw<cutoff,seedState:state.value,totalEffectiveWeight:total});
  if(raw>=cutoff)continue;
  let index=raw%total;for(let i=0;i<rows.length;i++){if(index<weights[i])return rows[i];index-=weights[i];}
 }
 return null;
}
/** All output is a proposal. Caller commits neither new crimes nor diagnoses from this draw. */
function sampleCrossedMatrix(bundle,context,seed=1){
 validateMatrixBundle(bundle);const ctx=normalizeContext(bundle,context),selection={},draws=[],filters=[];const state={value:seed>>>0};
 if(ctx.characterStatus==='dead'||ctx.facts[`dead-${ctx.characterId}`]===true)return {status:'none',reason:'character_dead',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
 const chance=ctx.occurrenceChance??100;
 assert(Number.isInteger(chance)&&chance>=0&&chance<=100,'chance de ocorrência inválida');
 if(chance<100){
  state.value=(Math.imul(1664525,state.value)+1013904223)>>>0;const value=Math.floor(state.value/4294967296*100)+1;
  draws.push({axis:'occurrence',dice:[value],accepted:value<=chance,seedState:state.value,chance});
  if(value>chance)return {status:'none',reason:'ordinary_life',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
 }
 for(const axis of AXES){
  const reasons={},eligible=[];
  for(const row of bundle.tables[axis]){const why=reason(row,ctx,selection,axis);if(why)reasons[why]=(reasons[why]??0)+1;else eligible.push(row);}
  filters.push({axis,total:bundle.tables[axis].length,eligible:eligible.length,rejected:reasons});
  if(!eligible.length)return {status:'none',reason:'no_coherent_crossing',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
  const row=drawWeighted(eligible,state,axis,draws,ctx);if(!row)return {status:'none',reason:'draw_limit',packId:bundle.pack.id,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
  selection[axis]=clone(row);
 }
 const check=checkMatrixCoherence(bundle,selection,ctx);
 if(!check.valid)return {status:'none',reason:'coherence_rejected',check,draws,filters,seedAfter:state.value};
 return {status:'proposal',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,originEventId:ctx.origin.id,characterId:ctx.characterId,day:ctx.day,stage:ctx.stage,situationId:selection.manifestation.baseSituationId??selection.manifestation.baseId??selection.manifestation.id,selection,draws,filters,proposal:{situation:selection.manifestation.text,focus:selection.domain.text,playerOption:selection.response.text,timing:selection.timing.mode,requiresPlayerChoice:true,mechanicalEffect:'none',canonMutation:'none'}};
}
/** Build runtime inputs only from the host's confirmed world state. */
function matrixContextFromStore(store,{originEventId,characterId,mode,capabilities=[],maxIntensity=2,allowedDomains}={}){
 const origin=store.events.find(e=>e.id===originEventId);
 assert(origin&&origin.lifeCauseId,'evento de origem não classificado pelo host');
 if(store.matrixProfile&&mode!==undefined)assert(mode===store.matrixProfile.mode,'modalidade difere do perfil salvo');
 return {system:store.legacy.system,mode:store.matrixProfile?.mode??mode,day:store.day,characterId,origin:{...origin,causeId:origin.lifeCauseId},facts:Object.fromEntries(Object.entries(store.facts).map(([k,v])=>[k,v.value])),capabilities,maxIntensity,allowedDomains,recent:store.matrixHistory??[],characterProfile:store.characterLifeProfiles?.[characterId],characterStatus:store.legacy.characters[characterId]?.status,arcs:Object.values(store.lifeArcs??[])};
}

/** Recheck a frozen proposal against present facts/time/capabilities before exposure. */
function revalidateMatrixProposal(result,context){
 if(result?.version==='2.1.0')return {valid:false,reasons:[{reason:'legacy_proposal_requires_redraw'}]};
 if(result?.status!=='proposal')return {valid:false,reasons:[{reason:'not_a_proposal'}]};
 try{
  const profile=selectMatrixProfile(result.packId,context.system);
  const bundle={schema:'braseiro.matrix-bundle/v1',pack:{id:result.packId,version:result.version,system:profile.system,mode:profile.mode},tables:Object.fromEntries(AXES.map(a=>[a,[result.selection[a]]]))};
  if(context.characterStatus==='dead'||context.facts?.[`dead-${context.characterId}`]===true)return {valid:false,reasons:[{reason:'character_dead'}]};
  return checkMatrixCoherence(bundle,result.selection,{...context,revalidating:true});
 }catch(error){return {valid:false,reasons:[{reason:'context_changed',message:error.message}]};}
}
    return { selectMatrixProfile, validateMatrixBundle, checkMatrixCoherence, candidateWeight, sampleCrossedMatrix, matrixContextFromStore, revalidateMatrixProposal };

  })();

  // --- 3. LIFE ARCS ENGINE ---
  const LifeArcs = (function() {
/** Biographical arcs; no rule-sheet conditions, diagnoses or player actions are granted. */
const assert=(v,m)=>{if(!v)throw Error(`BRASEIRO_ARC: ${m}`);};
const id=x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const ARC_STATES=['candidate','active','dormant','resurfaced','resolving','resolved','rejected','expired'];
function stableToken(value){let a=2166136261,b=2246822519;for(const ch of String(value)){a=Math.imul(a^ch.charCodeAt(0),16777619);b=Math.imul(b^ch.charCodeAt(0),3266489917);}return `${(a>>>0).toString(16)}${(b>>>0).toString(16)}`;}
function validateCharacterProfile(p){
 assert(p&&id(p.characterId),'personagem inválido');
 for(const field of ['domainWeights','causeWeights','protections']){assert(!p[field]||(typeof p[field]==='object'&&!Array.isArray(p[field])),'perfil inválido');for(const [key,n] of Object.entries(p[field]??{}))assert(id(key)&&Number.isFinite(n)&&n>=0&&n<=5,'peso fora de 0 a 5');}
 if(p.maxIntensity!==undefined)assert(Number.isInteger(p.maxIntensity)&&p.maxIntensity>=0&&p.maxIntensity<=5,'intensidade inválida');
 assert((p.capabilities??[]).every(id),'capacidade inválida');
 assert(p.occurrenceChance===undefined||(Number.isInteger(p.occurrenceChance)&&p.occurrenceChance>=0&&p.occurrenceChance<=100),'chance inválida');
 return structuredClone(p);
}
function characterCapabilities(s,characterId,provided){
 const profile=s.characterLifeProfiles?.[characterId];const caps=new Set(provided??profile?.capabilities??[]);
 for(const e of Object.values(s.entities)){
  if(e.status!=='active'||!e.actorIds?.includes(characterId))continue;
  if(e.kind==='home')caps.add('home_exists');
  if(e.kind==='promise')caps.add('promise_exists');
  if(e.kind==='relationship'){
   caps.add('related_person');if(e.relationType==='family')caps.add('family_contact');if(e.relationType==='intimate')caps.add('intimate_contact');
   if((e.dimensions?.trust??0)>=3)caps.add('support_available');
  }
 }
 for(const [fact,cap] of [['reparation-promised','reparation_promised'],['reparation-approved','reparation_approved'],['reward-approved','reward_approved'],['known-limitation','known_limitation']])if(s.facts[`${fact}-${characterId}`]?.value===true)caps.add(cap);
 if(s.events.some(e=>e.lifeCauseId?.startsWith('cause-promise-')&&(e.actorIds??[]).includes(characterId)))caps.add('promise_exists');
 return [...caps];
}
function arcIdentity(result){return `arc-${stableToken([result.packId,result.characterId,result.originEventId,result.situationId].join('|'))}`;}
function move(s,arc,status,reason,eventId){
 if(arc.status===status)return;
 const transition={from:arc.status,to:status,day:s.day,reason,...(eventId?{eventId}:{})};arc.transitions.push(transition);arc.status=status;arc.updatedDay=s.day;
}
function registerLifeArc(s,result,recordIndex){
 s.lifeArcs??={};const key=arcIdentity(result);let arc=s.lifeArcs[key];
 if(!arc){arc=s.lifeArcs[key]={id:key,packId:result.packId,packVersion:result.version,characterId:result.characterId,originEventId:result.originEventId,situationId:result.situationId,domain:result.selection.manifestation.domain,status:'candidate',createdDay:s.day,updatedDay:s.day,nextDueDay:s.day+30,progress:0,intensity:result.selection.manifestation.intensity??1,transitions:[],recordIndices:[]};}
 else {assert(!['resolved','rejected'].includes(arc.status),'arco encerrado exige gatilho confirmado para reabrir');if(['dormant','expired'].includes(arc.status))move(s,arc,'candidate','nova oportunidade contextual');}
 arc.recordIndices.push(recordIndex);for(const offer of Object.values(s.offers))if(offer.arcId===key&&offer.status==='pending')offer.status='expired';return key;
}
function presentLifeArc(s,arcId){const arc=s.lifeArcs?.[arcId];if(!arc)return;move(s,arc,arc.transitions.some(t=>['active','resurfaced'].includes(t.to))?'resurfaced':'active','manifestação apresentada');arc.nextDueDay=s.day+14;}
function expireArcOffer(s,arcId,reason){const arc=s.lifeArcs?.[arcId];if(!arc||['resolved','rejected'].includes(arc.status))return;move(s,arc,'dormant',reason);arc.nextDueDay=s.day+14;}
function chooseArcOpportunity(s,arcId,choice){const arc=s.lifeArcs?.[arcId];if(!arc)return;if(choice==='pass'){move(s,arc,'dormant','jogador não explorou a oportunidade');arc.nextDueDay=s.day+21;}else if(choice==='engage'){move(s,arc,'active','jogador demonstrou interesse; resultado ainda depende do jogo');arc.nextDueDay=s.day+14;}}
function applyArcSignal(s,{arcId,outcome},event){
 const arc=s.lifeArcs?.[arcId];assert(arc,'arco inexistente');
 assert((event.actorIds??[]).includes(arc.characterId)||(event.witnessIds??[]).includes(arc.characterId),'evento não envolve o personagem');
 const signalId=`${event.id}:${outcome}`;arc.processedSignals??=[];if(arc.processedSignals.includes(signalId))return;
 assert(['support_received','practical_improvement','setback','resolved','new_trigger','rejected','no_change'].includes(outcome),'resultado de arco inválido');
 if(['resolved','rejected'].includes(arc.status))assert(outcome==='new_trigger','arco encerrado não pode progredir sem novo gatilho');
 if(outcome==='support_received'||outcome==='practical_improvement'){arc.progress=Math.min(2,arc.progress+1);move(s,arc,'resolving','apoio ou melhora confirmados',event.id);arc.nextDueDay=s.day+30;}
 if(outcome==='setback'){arc.intensity=Math.min(5,arc.intensity+1);move(s,arc,'active','revés confirmado',event.id);arc.nextDueDay=s.day+14;}
 if(outcome==='resolved'||outcome==='rejected'){move(s,arc,outcome,'encerramento confirmado',event.id);arc.nextDueDay=null;for(const o of Object.values(s.offers))if(o.arcId===arcId&&o.status==='pending')o.status='expired';}
 if(outcome==='new_trigger'){move(s,arc,'dormant','novo gatilho confirmado',event.id);arc.reopenedByEventId=event.id;arc.nextDueDay=s.day;}
 arc.processedSignals.push(signalId);arc.lastEvidenceEventId=event.id;arc.updatedDay=s.day;
}
function tickLifeArcs(s){
 for(const arc of Object.values(s.lifeArcs??{})){
  if(arc.nextDueDay===null||s.day<arc.nextDueDay)continue;
  if(arc.status==='candidate'){move(s,arc,'expired','proposta não usada no prazo');for(const o of Object.values(s.offers))if(o.arcId===arc.id&&o.status==='pending')o.status='expired';}
  else if(['active','resurfaced'].includes(arc.status)){move(s,arc,'dormant','intervalo sem novo acontecimento relevante');for(const o of Object.values(s.offers))if(o.arcId===arc.id&&o.status==='pending')o.status='expired';}
  else if(arc.status==='resolving'){move(s,arc,'dormant','evolução aguarda evidência; não há cura automática');}
 }
}
function validateLifeArcs(s){
 for(const [key,arc] of Object.entries(s.lifeArcs??{})){assert(key===arc.id&&id(key)&&id(arc.characterId)&&ARC_STATES.includes(arc.status),'arco inválido');assert(Array.isArray(arc.transitions)&&arc.transitions.every(t=>ARC_STATES.includes(t.from)&&ARC_STATES.includes(t.to)&&Number.isSafeInteger(t.day)),'transições inválidas');}
 for(const p of Object.values(s.characterLifeProfiles??{}))validateCharacterProfile(p);
}
/** Experience and choices influence narrative selection, never grant traits or diagnoses. */
function deriveCharacterProfile(s,characterId){
 const configured=s.characterLifeProfiles?.[characterId]??{};
 const profile={...structuredClone(configured),characterId,domainWeights:{...configured.domainWeights},protections:{...configured.protections},experience:{},decisionWeights:{}};
 for(const event of s.events){if(!(event.actorIds??[]).includes(characterId)&&!(event.witnessIds??[]).includes(characterId))continue;const tag=event.lifeCauseId?.match(/^cause-(.+)-\d+$/)?.[1];if(tag)profile.experience[tag]=(profile.experience[tag]??0)+1;}
 const supporters=Object.values(s.entities).filter(e=>e.kind==='relationship'&&e.status==='active'&&e.actorIds?.includes(characterId)&&(e.dimensions?.trust??0)>=3).length;
 const confirmedSupport=Object.values(s.lifeArcs??{}).filter(a=>a.characterId===characterId&&(a.processedSignals??[]).some(key=>key.endsWith(':support_received'))).length;
 if(configured.protections?.support===undefined)profile.protections.support=Math.min(5,supporters+confirmedSupport);
 for(const offer of Object.values(s.offers).filter(o=>o.arcId&&o.status==='chosen'&&o.chosenBy===characterId&&s.day-(o.chosenDay??o.day)<=180).slice(-32)){
  const domain=s.lifeArcs?.[offer.arcId]?.domain;if(!domain)continue;
  const delta=offer.choice==='pass'?-0.2:offer.choice==='engage'?0.15:0;
  profile.decisionWeights[domain]=Math.max(0.25,Math.min(2,(profile.decisionWeights[domain]??1)+delta));
 }
 return profile;
}
    return { ARC_STATES, stableToken, validateCharacterProfile, characterCapabilities, arcIdentity, registerLifeArc, presentLifeArc, expireArcOffer, chooseArcOpportunity, applyArcSignal, tickLifeArcs, validateLifeArcs, deriveCharacterProfile };

  })();

  // --- 4. MATRIX AUTOMATION ENGINE ---
  const MatrixAutomation = (function(LifeArcs) {
    const { stableToken, deriveCharacterProfile } = LifeArcs;
const allowed=new Set(['event','advance','rest','travel','visit','scene_tick','evaluate']);
function validateAutomation(options){
 if(!options||typeof options.enabled!=='boolean')throw Error('Automação exige enabled booleano');
 const o={enabled:options.enabled,chancePercent:35,cooldownDays:7,maxAttemptsPerTrigger:1,autoPresent:true,characterIds:[],...structuredClone(options)};
 for(const [k,lo,hi] of [['chancePercent',0,100],['cooldownDays',1,3650],['maxAttemptsPerTrigger',1,8]])if(!Number.isInteger(o[k])||o[k]<lo||o[k]>hi)throw Error(`Automação: ${k} inválido`);
 if(typeof o.autoPresent!=='boolean'||!Array.isArray(o.characterIds)||!o.characterIds.every(x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)))throw Error('Automação: configuração inválida');
 return o;
}
/** Returns a bounded plan, never executes IO or changes state. Called after normal gameplay. */
function planAutomaticMatrices(s,trigger){
 const config=s.matrixAutomation;if(!config?.enabled||!allowed.has(trigger.type)||!s.matrixProfile)return [];
 const current=s.sceneContext??{};
 const ids=config.characterIds.length?config.characterIds:(current.actorIds?.length?current.actorIds:Object.keys(s.characterLifeProfiles??{}));
 const result=[];
 for(const characterId of [...new Set(ids)].sort()){
  if(result.length>=config.maxAttemptsPerTrigger||s.budget.used>=s.budget.max)break;
  if(s.facts[`dead-${characterId}`]?.value===true||s.legacy.characters[characterId]?.status==='dead')continue;
  const schedule=s.matrixSchedule?.[characterId];if(schedule&&s.day<schedule.nextDueDay)continue;
  const events=s.events.filter(e=>e.lifeCauseId&&((e.actorIds??[]).includes(characterId)||(e.witnessIds??[]).includes(characterId)));
  const score=e=>{const used=(s.matrixHistory??[]).filter(r=>r.originEventId===e.id&&(r.characterId??r.result?.characterId)===characterId).length;return (used===0?20:0)+(e.locationId&&e.locationId===current.locationId?5:0)+e.day/100000-used*3;};
  events.sort((a,b)=>score(b)-score(a)||a.id.localeCompare(b.id));
  if(!events.length)continue;
  const p=deriveCharacterProfile(s,characterId);
  const protections=Math.min(5,Object.values(p.protections??{}).reduce((a,b)=>a+b,0));
  const occurrenceChance=p.occurrenceChance??Math.max(0,Math.round(config.chancePercent*(1-protections/10)));
  result.push({id:`auto-${stableToken(`${trigger.id}|${characterId}|${s.day}`)}`,characterId,originEventId:events[0].id,mode:s.matrixProfile.mode,occurrenceChance,maxIntensity:p.maxIntensity??2,locationId:current.locationId,downtime:trigger.type==='rest'});
 }
 return result;
}
    return { validateAutomation, planAutomaticMatrices };

  })(LifeArcs);

  // --- 5. LIFE ENGINE ---
  const LifeEngine = (function(LegacyEngine, MatrixEngine, LifeArcs, MatrixAutomation) {
    const { assertLegacyStore } = LegacyEngine;
    const { sampleCrossedMatrix, matrixContextFromStore, selectMatrixProfile, revalidateMatrixProposal } = MatrixEngine;
    const { validateCharacterProfile, characterCapabilities, registerLifeArc, presentLifeArc, expireArcOffer, chooseArcOpportunity, applyArcSignal, tickLifeArcs, validateLifeArcs, deriveCharacterProfile } = LifeArcs;
    const { validateAutomation } = MatrixAutomation;
/** Portable Braseiro V2. All changes are pure transactions; time is fictional integer days. */




const cp = x => structuredClone(x);
const serial = x => JSON.parse(JSON.stringify(x));
const ok = (x,m) => {if(!x) throw Error(`BRASEIRO_LIFE: ${m}`);};
const id = x => typeof x === 'string' && /^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x) && !['__proto__','constructor','prototype'].includes(x);
const number = x => Number.isFinite(x);
const kinds = ['home','promise','npc_project','relationship','tradition','heirloom','reputation','opportunity','world_change','recognition','personal_project','succession','foreshadowing','resource','problem','adaptation','information','causal_chain','routine','adversary','life_arc'];
const statuses = ['active','dormant','resolved','expired','retired','dead','blocked'];
const get = (s,ref) => {ok(typeof ref==='string','referência inválida'); const [kind,key,field] = ref.split(':'); return kind==='fact' ? s.facts[key]?.value : kind==='entity' ? s.entities[key]?.[field] : undefined;};
function createLifeStore(legacy,{day=0,seed=1}={}) {
  ok(Number.isSafeInteger(day)&&day>=0 && Number.isInteger(seed),'tempo/seed inválido');
  return {schema:'braseiro.life/v2',revision:0,legacy:assertLegacyStore(legacy),day,rng:seed>>>0,entities:{},facts:{},bindings:{},knowledge:[],events:[],receipts:{},rules:{},offers:{},history:[],rolls:[],uses:{},budget:{day,used:0,max:3}};
}
function assertLifeStore(s) {
  ok(s?.schema==='braseiro.life/v2','schema inválido'); assertLegacyStore(s.legacy);
  ok(Number.isSafeInteger(s.revision)&&s.revision>=0 && Number.isSafeInteger(s.day)&&s.day>=0,'revisão/tempo inválidos');
  for(const k of ['entities','facts','bindings','receipts','rules','offers','uses']) ok(s[k]&&typeof s[k]==='object'&&!Array.isArray(s[k]),`estrutura ${k}`);
  for(const k of ['knowledge','events','history','rolls']) ok(Array.isArray(s[k]),`estrutura ${k}`);
  ok(Number.isInteger(s.rng)&&s.rng>=0&&s.rng<=4294967295,'rng inválido');
  ok(s.budget&&Number.isInteger(s.budget.max)&&s.budget.max>0,'orçamento inválido');
  for(const [key,e] of Object.entries(s.entities)) {ok(key===e.id && id(key),'entidade inválida');validateEntity(e);}
  for(const [key,r] of Object.entries(s.rules)) {ok(key===r.id,'regra inválida');validateRule(r);}
  if(s.matrixHistory!==undefined)ok(Array.isArray(s.matrixHistory),'histórico de matrizes inválido');
  if(s.matrixProfile)ok(JSON.stringify(selectMatrixProfile(s.matrixProfile.packId,s.legacy.system))===JSON.stringify(s.matrixProfile),'perfil de matriz inválido');
  validateLifeArcs(s);if(s.matrixAutomation)validateAutomation(s.matrixAutomation);
  return cp(s);
}
function validateEntity(e) {
  ok(e&&id(e.id)&&kinds.includes(e.kind)&&typeof e.title==='string'&&e.title.length>0,'entidade/tipo/título inválido');
  ok(['public','private','gm_only'].includes(e.visibility),'visibilidade obrigatória');
  ok(statuses.includes(e.status??'active'),'estado inválido');
  ok(Array.isArray(e.actorIds??[])&&(e.actorIds??[]).every(id),'atores inválidos');
  ok(!e.locationId||id(e.locationId),'local inválido');
  if(e.kind==='resource') ok(number(e.quantity)&&e.quantity>=0&&number(e.dailyDelta??0),'recurso inválido');
  if(e.dueDay!==undefined) ok(Number.isSafeInteger(e.dueDay)&&e.dueDay>=0,'prazo inválido');
  if(e.validUntil!==undefined) ok(Number.isSafeInteger(e.validUntil)&&e.validUntil>=0,'validade inválida');
}
const ops=['eq','ne','gte','lte','exists','includes'];
function validateCondition(c) {ok(c&&typeof c.ref==='string'&&ops.includes(c.op),'condição inválida'); const bits=c.ref.split(':'); ok(['fact','entity'].includes(bits[0])&&id(bits[1]),'referência inválida'); if(bits[0]==='entity')ok(['status','quantity','progress','dueDay','validUntil'].includes(bits[2]),'campo ilegível');}
function condition(s,c) {
  const v=get(s,c.ref); switch(c.op){case 'exists': return (v!==undefined)===(c.value??true);case 'eq':return v===c.value;case 'ne':return v!==undefined&&v!==c.value;case 'gte':return number(v)&&v>=c.value;case 'lte':return number(v)&&v<=c.value;case 'includes':return Array.isArray(v)&&v.includes(c.value);}
}
const effectFields=['status','progress','quantity'];
function validateEffect(e) {
  ok(e&&['set_fact','set_entity','add_resource','offer','reveal'].includes(e.type),'efeito inválido');
  if(e.type==='set_fact')ok(id(e.id)&&['public','private','gm_only'].includes(e.visibility),'fato inválido');
  if(e.type==='set_entity') {ok(id(e.id)&&effectFields.includes(e.field),'campo de efeito proibido');if(e.field==='status')ok(statuses.includes(e.value),'estado inválido');else ok(number(e.value),'valor inválido');}
  if(e.type==='add_resource')ok(id(e.id)&&number(e.amount),'recurso inválido');
  if(e.type==='offer'){ok(id(e.id)&&typeof e.text==='string'&&['public','private','gm_only'].includes(e.visibility),'oferta inválida'); for(const list of Object.values(e.choices??{})){ok(Array.isArray(list),'escolha inválida');for(const fx of list){ok(fx.type!=='offer','ofertas recursivas proibidas');validateEffect(fx);}}}
  if(e.type==='reveal')ok(id(e.entityId)&&id(e.actorId)&&typeof e.source==='string','revelação inválida');
}
function validateRule(r) {
  ok(r&&id(r.id)&&typeof r.version==='string'&&Array.isArray(r.when)&&r.when.length>0,'regra exige condições e versão');r.when.forEach(validateCondition);
  ok(['world','involuntary','player_choice'].includes(r.agency),'agência inválida');
  ok(Array.isArray(r.effects)&&r.effects.length>0,'efeitos obrigatórios');r.effects.forEach(validateEffect);
  if(r.agency==='player_choice')ok(r.effects.every(x=>x.type==='offer'),'ação voluntária exige oferta');
  ok(Number.isSafeInteger(r.cooldownDays??1)&&(r.cooldownDays??1)>=1,'cooldown inválido');
  ok(r.chance===undefined||(Number.isInteger(r.chance)&&r.chance>=0&&r.chance<=100),'chance inválida');
  ok(!r.locationId||id(r.locationId),'local inválido');
  ok(Number.isSafeInteger(r.priority??0),'prioridade inválida');
}
function audit(s,type,payload) {s.history.push({type,day:s.day,...cp(payload)});}
function roll(s,table,version) {s.rng=(Math.imul(1664525,s.rng)+1013904223)>>>0; const value=Math.floor(s.rng/4294967296*100)+1;s.rolls.push({day:s.day,table,version,state:s.rng,value});return value;}
function applyEffect(s,e,source) {
  e={...e};
  const scope=s.rules[source]?.locationId;
  if(scope && e.locationId===undefined)e.locationId=scope;
  if(e.type==='set_fact')s.facts[e.id]={id:e.id,value:cp(e.value),visibility:e.visibility,actorIds:cp(e.actorIds??[]),locationId:e.locationId,day:s.day,source};
  if(e.type==='set_entity'){const target=s.entities[e.id];ok(target,'entidade ausente');if(e.field==='quantity')ok(target.kind==='resource'&&e.value>=0,'quantidade inválida');if(e.field==='progress')ok(e.value>=0,'progresso inválido');target[e.field]=cp(e.value);target.updatedDay=s.day;}
  if(e.type==='add_resource'){const target=s.entities[e.id];ok(target?.kind==='resource','recurso ausente');ok(target.quantity+e.amount>=0,'recursos insuficientes');target.quantity+=e.amount;target.updatedDay=s.day;}
  if(e.type==='offer'){
    const key=`${source}:${e.id}:${s.day}`;
    // A pending choice is not reissued each day.
    if(Object.values(s.offers).some(o=>o.source===source&&o.templateId===e.id&&o.status==='pending'))return;
    s.offers[key]={...cp(e),id:key,templateId:e.id,source,day:s.day,status:'pending'};
  }
  if(e.type==='reveal'){ok(s.entities[e.entityId],'entidade ausente');s.knowledge.push({entityId:e.entityId,actorId:e.actorId,source:e.source,day:s.day,certainty:'confirmed'});}
  audit(s,'effect',{source,effect:e});
}
function evaluate(s,locationId) {
  const rules=Object.values(s.rules).sort((a,b)=>Number(a.narrative!==false)-Number(b.narrative!==false)||(b.priority??0)-(a.priority??0)||a.id.localeCompare(b.id));
  // One bounded pass: chained stages run on the next evaluation, never an unbounded loop.
  const eligible=rules.filter(r=>!r.disabled&&(!r.locationId||r.agency==='world'||r.locationId===locationId)&&r.when.every(c=>condition(s,c)));
  for(const r of eligible){
    if(!r.when.every(c=>condition(s,c)))continue;
    if(r.once!==false&&s.uses[r.id])continue;
    if(s.uses[r.id]&&s.day-s.uses[r.id].day<(r.cooldownDays??1))continue;
    const presents=r.effects.some(e=>e.type==='offer')||r.narrative===true;
    if(presents&&s.budget.used>=s.budget.max)continue;
    if(r.chance!==undefined&&roll(s,r.id,r.version)>r.chance){s.uses[r.id]={day:s.day,result:'miss'};continue;}
    for(const e of r.effects)applyEffect(s,e,r.id);
    s.uses[r.id]={day:s.day,result:'applied'}; if(presents)s.budget.used++;
    audit(s,'rule_applied',{ruleId:r.id,version:r.version});
  }
}
/** The trusted host supplies authorization, never the imported adventure or AI. */
function transact(store,command,{verifiedByHost=false,expectedRevision=store.revision}={}) {
  ok(verifiedByHost===true,'verificação do host obrigatória');ok(store.revision===expectedRevision,'conflito de revisão');
  ok(command&&id(command.id),'comando sem id');
  let fingerprint=JSON.stringify(command);
  // Table assets may be megabytes. Compact only this new command's receipt.
  if(command.type==='matrix_draw'){
    const text=fingerprint;let a=2166136261,b=2246822519,c=3266489917,d=668265263;
    for(let i=0;i<text.length;i++){const x=text.charCodeAt(i);a=Math.imul(a^x,16777619);b=Math.imul(b^x,2246822519);c=Math.imul(c^x,3266489917);d=Math.imul(d^x,668265263);}
    fingerprint=`matrix-v1:${text.length}:${[a,b,c,d].map(v=>(v>>>0).toString(16)).join(':')}`;
  }
  if(store.receipts[command.id]){ok(store.receipts[command.id]===fingerprint,'id reutilizado com conteúdo diferente');return cp(store);}
  const s=cp(store), c=command;
  switch(c.type){
    case 'register':validateEntity(c.entity);ok(!s.entities[c.entity.id],'entidade duplicada');s.entities[c.entity.id]={status:'active',createdDay:s.day,...cp(c.entity)};break;
    case 'rule':validateRule(c.rule);ok(!s.rules[c.rule.id],'regra duplicada');s.rules[c.rule.id]=cp(c.rule);break;
    case 'binding':{
      ok(id(c.binding?.id)&&id(c.binding.tag)&&Array.isArray(c.binding.effects),'binding inválido');
      ok(!s.bindings[c.binding.id],'binding duplicado');
      for(const fx of c.binding.effects){ok(fx.type==='set_fact','binding só produz fatos');validateEffect(fx);}
      s.bindings[c.binding.id]=cp(c.binding);break;
    }
    case 'fact':validateEffect({...c,type:'set_fact',id:c.key});applyEffect(s,{...c,type:'set_fact',id:c.key},c.id);break;
    case 'event':{
      const e=c.event;ok(e&&id(e.id)&&typeof e.summary==='string'&&['public','private','gm_only'].includes(e.visibility),'evento inválido');
      ok(!s.events.some(x=>x.id===e.id),'evento duplicado');
      ok((e.actorIds??[]).every(id)&&(e.witnessIds??[]).every(id),'atores inválidos');
      s.events.push({...cp(e),day:s.day});
      for(const binding of Object.values(s.bindings))if(binding.tag===e.tag){for(const fx of binding.effects){const levels=['public','private','gm_only'];const visibility=levels[Math.max(levels.indexOf(fx.visibility),levels.indexOf(e.visibility))];applyEffect(s,{...fx,visibility,actorIds:visibility==='private'?(e.actorIds??[]):(fx.actorIds??[]),locationId:e.locationId??fx.locationId},e.id);}audit(s,'event_binding',{bindingId:binding.id,eventId:e.id});}
      for(const signal of e.lifeArcSignals??[])applyArcSignal(s,signal,e);
      for(const a of e.witnessIds??[])s.knowledge.push({eventId:e.id,actorId:a,day:s.day,source:`witness:${e.id}`,certainty:'confirmed'});
      // Voluntary habit repetition is counted only from verified host events.
      for(const x of Object.values(s.entities))if(x.kind==='tradition'&&x.eventTag===e.tag&&(x.actorIds??[]).some(a=>(e.actorIds??[]).includes(a))){x.progress=(x.progress??0)+1;if(x.progress>=(x.threshold??3))applyEffect(s,{type:'offer',id:`confirm-${x.id}`,text:`Manter como tradição: ${x.title}?`,visibility:'private',actorIds:x.actorIds,choices:{accept:[{type:'set_fact',id:`tradition-${x.id}`,value:true,visibility:'private',actorIds:x.actorIds}],decline:[]}},x.id);}
      break;
    }
    case 'learn':{
      ok(id(c.actorId)&&typeof c.source==='string'&&['confirmed','rumor','belief'].includes(c.certainty),'conhecimento inválido');
      const target=c.entityId?s.entities[c.entityId]:s.events.find(e=>e.id===c.eventId);ok(target,'origem inexistente');
      ok(Number.isSafeInteger(c.availableDay)&&c.availableDay<=s.day,'informação ainda não chegou');
      if(c.sourceActorId)ok(s.knowledge.some(k=>k.actorId===c.sourceActorId&&(c.entityId?k.entityId===c.entityId:k.eventId===c.eventId)),'transmissor não sabe');
      s.knowledge.push({entityId:c.entityId,eventId:c.eventId,actorId:c.actorId,source:c.source,sourceActorId:c.sourceActorId,certainty:c.certainty,claim:c.claim,day:s.day});break;
    }
    case 'rest':case 'travel':case 'visit':case 'scene_tick':case 'advance':{
      const startDay=s.day;
      const toDay=c.toDay??(c.type==='advance'?NaN:s.day);
      if(c.locationId!==undefined)ok(id(c.locationId),'local inválido');
      if(c.actorIds!==undefined)ok(Array.isArray(c.actorIds)&&c.actorIds.every(id),'personagens da cena inválidos');
      s.sceneContext={...(s.sceneContext??{}),...(c.locationId?{locationId:c.locationId}:{}),...(c.actorIds?{actorIds:cp(c.actorIds)}:{}),downtime:c.type==='rest'};
      ok(Number.isSafeInteger(toDay)&&toDay>=s.day&&toDay-s.day<=36600,'avanço inválido ou maior que 100 anos');
      while(s.day<toDay){s.day++;s.budget={...s.budget,day:s.day,used:0};
        for(const e of Object.values(s.entities)){
          if(e.status!=='active')continue;
          if(e.kind==='resource'){e.quantity=Math.max(0,e.quantity+(e.dailyDelta??0));e.updatedDay=s.day;}
          if(e.kind==='information'&&e.validUntil!==undefined&&s.day>e.validUntil){e.status='expired';audit(s,'information_expired',{entityId:e.id});}
          if(e.kind==='opportunity'&&e.dueDay!==undefined&&s.day>e.dueDay){e.status='expired';audit(s,'opportunity_expired',{entityId:e.id});}
        }
        tickLifeArcs(s);evaluate(s,s.sceneContext.locationId);
      }
      if(c.type!=='advance'&&s.day===startDay)evaluate(s,s.sceneContext.locationId);break;
    }
    case 'evaluate':if(c.locationId)s.sceneContext={...(s.sceneContext??{}),locationId:c.locationId};evaluate(s,c.locationId);break;
    case 'choose':{
      const o=s.offers[c.offerId];ok(o?.status==='pending','escolha indisponível');ok((o.actorIds??[]).includes(c.actorId),'personagem não autorizado');
      if(isMatrixOffer(s,o))ok(matrixOfferCheck(s,o).valid,'situação mudou; reavalie a oferta da matriz');
      const rule=s.rules[o.source];ok(!rule||rule.when.every(x=>condition(s,x)),'situação mudou; reavalie a oferta');
      ok(Object.hasOwn(o.choices??{},c.choice),'opção inválida');for(const e of o.choices[c.choice])applyEffect(s,e,o.id);o.status='chosen';o.choice=c.choice;o.chosenBy=c.actorId;o.chosenDay=s.day;chooseArcOpportunity(s,o.arcId,c.choice);break;
    }
    case 'transfer':{
      const e=s.entities[c.entityId];ok(e?.kind==='heirloom'&&id(c.toActorId),'objeto/destinatário inválido');ok(e.ownerId===c.fromActorId,'proprietário incompatível');
      e.provenance??=[];e.provenance.push({from:c.fromActorId,to:c.toActorId,day:s.day,eventId:c.eventId});e.ownerId=c.toActorId;break;
    }
    case 'retire':{
      ok(s.legacy.characters[c.actorId],'personagem inexistente');ok(s.facts[`dead-${c.actorId}`]?.value!==true,'personagem morto não pode aposentar');
      s.legacy.characters[c.actorId].status='retired';s.legacy.characters[c.actorId].npcRole=c.npcRole??'patron';
      if(c.successorId){ok(id(c.successorId)&&!s.legacy.characters[c.successorId],'sucessor inválido');s.legacy.characters[c.successorId]={characterId:c.successorId,events:[],predecessorId:c.actorId};}break;
    }
    case 'matrix_character_profile':{
      const profile=validateCharacterProfile(c.profile);s.characterLifeProfiles??={};s.characterLifeProfiles[profile.characterId]=profile;break;
    }
    case 'matrix_automation_configure':{const config=validateAutomation(c.options);if(config.enabled)ok(s.matrixProfile,'configure o perfil antes da automação');s.matrixAutomation=config;break;}
    case 'matrix_schedule':{
      ok(id(c.characterId)&&Number.isSafeInteger(c.nextDueDay)&&c.nextDueDay>=s.day,'agendamento ficcional inválido');
      s.matrixSchedule??={};s.matrixSchedule[c.characterId]={lastAttemptDay:s.day,nextDueDay:c.nextDueDay};break;
    }
    case 'arc_progress':{
      const evidence=s.events.find(e=>e.id===c.eventId);ok(evidence,'evidência do arco inexistente');
      applyArcSignal(s,{arcId:c.arcId,outcome:c.outcome},evidence);break;
    }
    case 'matrix_configure':{
      s.matrixProfile=selectMatrixProfile(c.packId,s.legacy.system);break;
    }
    case 'matrix_present':{
      const o=s.offers[c.offerId];ok(o?.status==='pending'&&o.visibility==='gm_only'&&o.type==='offer','proposta reservada indisponível');
      ok(o.templateId.startsWith('matrix-'),'proposta não pertence à matriz');
      const check=matrixOfferCheck(s,o);
      if(!check.valid){o.status='expired';expireArcOffer(s,o.arcId,'condições mudaram antes da apresentação');audit(s,'matrix_proposal_expired',{offerId:o.id,reasons:check.reasons});break;}
      o.status='presented';presentLifeArc(s,o.arcId);
      applyEffect(s,{type:'offer',id:`scene-${c.id}`,text:o.text,visibility:'private',actorIds:o.actorIds,arcId:o.arcId,matrixRecordIndex:o.matrixRecordIndex,locationId:o.locationId,suggestedAction:o.suggestedAction,choices:{engage:[],pass:[]}},c.id);
      break;
    }
    case 'matrix_draw':{
      ok(c.bundle,'tabelas ausentes');
      ok(s.matrixProfile,'configure a matriz do sistema antes do sorteio');
      ok(s.matrixProfile.packId===c.bundle.pack.id,'tabela difere do perfil salvo');
      const context=liveMatrixContext(s,c.context);
      if(c.occurrenceChance!==undefined)context.occurrenceChance=c.occurrenceChance;
      if(s.budget.used>=s.budget.max){audit(s,'matrix_deferred',{reason:'narrative_budget'});break;}
      const result=sampleCrossedMatrix(c.bundle,context,s.rng);s.rng=result.seedAfter;
      s.matrixHistory??=[];
      const recordIndex=s.matrixHistory.length;
      s.matrixHistory.push({commandId:c.id,characterId:c.context.characterId,day:s.day,packId:c.bundle.pack.id,version:c.bundle.pack.version,originEventId:c.context.originEventId,situationId:result.situationId??null,status:result.status,contextAudit:cp({...context,recent:undefined}),result:cp(result)});
      if(result.status==='proposal'){
        const arcId=registerLifeArc(s,result,recordIndex);
        const offerId=`matrix-${c.id}`;ok(id(offerId),'id de sorteio longo demais');
        applyEffect(s,{type:'offer',id:offerId,text:result.proposal.situation,visibility:'gm_only',actorIds:[c.context.characterId],matrixRecordIndex:recordIndex,arcId,locationId:context.locationId,suggestedAction:result.proposal.playerOption,choices:{present:[{type:'set_fact',id:`presented-${c.id}`,value:true,visibility:'gm_only'}],defer:[]}},c.id);
        s.budget.used++;
      }
      audit(s,'matrix_draw',{packId:c.bundle.pack.id,status:result.status});break;
    }
    case 'consume_context':{
      ok(Array.isArray(c.entityIds)&&c.entityIds.every(x=>!!s.entities[x]),'contexto inválido');for(const key of c.entityIds){s.entities[key].lastUsedDay=s.day;s.entities[key].useCount=(s.entities[key].useCount??0)+1;}break;
    }
    default:throw Error('BRASEIRO_LIFE: comando desconhecido');
  }
  s.receipts[c.id]=fingerprint;s.revision++;audit(s,'command',{id:c.id,commandType:c.type});return serial(s);
}
function visible(s,x,viewerIds,gm) {
  if(gm||x.visibility==='public')return true;
  if(x.visibility==='gm_only')return false;
  return (x.actorIds??[]).some(a=>viewerIds.includes(a)) || s.knowledge.some(k=>viewerIds.includes(k.actorId)&&(k.entityId===x.id||k.eventId===x.id));
}
/** Explicit selection + consumption prevents repeating old memories on every reply. */
function buildSceneContext(s,{locationId,actorIds=[],audience='player',limit=6}={}) {
  ok(['player','gm'].includes(audience)&&Number.isInteger(limit)&&limit>=0&&limit<=50,'contexto inválido');
  const gm=audience==='gm';
  const ranked=Object.values(s.entities).filter(e=>visible(s,e,actorIds,gm)&&e.status==='active'&&(!e.locationId||e.locationId===locationId)&&(!e.actorIds?.length||e.actorIds.some(a=>actorIds.includes(a))||e.locationId===locationId)&&
    (e.lastUsedDay===undefined||s.day-e.lastUsedDay>=(e.cooldownDays??7)))
    .map(e=>({e,score:(e.locationId===locationId?5:0)+(e.actorIds?.some(a=>actorIds.includes(a))?4:0)+(e.updatedDay===s.day?3:0)+(e.dueDay!==undefined&&e.dueDay<=s.day?4:0)-(e.useCount??0)}))
    .sort((a,b)=>b.score-a.score||a.e.id.localeCompare(b.e.id)).slice(0,limit);
  // Never serialize arbitrary entity payloads, private truths, choices or engine rules.
  const selected=ranked.map(({e,score})=>({id:e.id,kind:e.kind,title:e.title,description:gm?e.description:(e.publicDescription??(e.visibility==='public'||e.actorIds?.some(a=>actorIds.includes(a))?e.description:undefined)),status:e.status,score,...(e.kind==='resource'?{quantity:e.quantity}:{}),...(e.kind==='information'?{asOfDay:e.createdDay,certainty:'recorded_at_that_time'}:{})}));
  const offers=Object.values(s.offers).filter(o=>o.status==='pending'&&(!isMatrixOffer(s,o)||matrixOfferCheck(s,o).valid)&&visible(s,o,actorIds,gm)&&(!o.locationId||o.locationId===locationId)&&(!s.rules[o.source]||s.rules[o.source].when.every(c=>condition(s,c))))
    .map(o=>({id:o.id,text:o.text,choices:Object.keys(o.choices??{}),...(o.suggestedAction?{suggestedAction:o.suggestedAction}:{})}));
  const facts=Object.values(s.facts).filter(f=>visible(s,f,actorIds,gm)&&(!f.locationId||f.locationId===locationId)).slice(-24).map(f=>({id:f.id,value:cp(f.value),day:f.day}));
  return {day:s.day,selectedEntityIds:selected.map(x=>x.id),elements:selected,offers,facts,instructions:['Narrar somente fatos fornecidos; propostas não são ações consumadas.','O jogador decide ações voluntárias.','Ausência de contexto antigo permite uma cena inteiramente nova.','Informação datada pode estar desatualizada.'],...(gm?{biographicalArcs:Object.values(s.lifeArcs??{}).filter(a=>actorIds.includes(a.characterId)).map(a=>({id:a.id,characterId:a.characterId,domain:a.domain,status:a.status,progress:a.progress,originEventId:a.originEventId,transitions:a.transitions.slice(-2)})),eligibleRuleIds:Object.values(s.rules).filter(r=>r.when.every(c=>condition(s,c))).map(r=>r.id)}:{})};
}
/** NPC perspective never receives canonical truth merely because the GM knows it. */
function buildNpcContext(s,actorId) {
  return {actorId,knowledge:s.knowledge.filter(k=>k.actorId===actorId).map(k=>{const target=k.entityId?s.entities[k.entityId]:s.events.find(x=>x.id===k.eventId);return {referenceId:k.entityId??k.eventId,certainty:k.certainty,claim:k.claim??(k.certainty==='confirmed'?(target?.publicDescription??target?.publicStory??target?.summary??target?.title):undefined),source:k.source,learnedDay:k.day};}),relationships:Object.values(s.entities).filter(e=>e.kind==='relationship'&&e.actorIds?.includes(actorId)).map(e=>({id:e.id,title:e.title,actorIds:e.actorIds,dimensions:cp(e.dimensions??{})}))};
}
function synchronizeLegacy(s,legacy) {
  const l=assertLegacyStore(legacy);ok(['system','worldId','timelineId'].every(k=>l[k]===s.legacy[k]),'universo incompatível');
  ok(l.revision>=s.legacy.revision,'legado antigo');const out=cp(s);out.legacy=l;out.revision++;return out;
}
const ENTITY_KINDS=Object.freeze(kinds);

function liveMatrixContext(s,options){
 const profile=s.characterLifeProfiles?.[options.characterId];
 const ctx=matrixContextFromStore(s,{...options,maxIntensity:profile?.maxIntensity??options.maxIntensity,capabilities:characterCapabilities(s,options.characterId,profile?undefined:options.capabilities)});
 ctx.characterProfile=deriveCharacterProfile(s,options.characterId);
 ctx.locationId=options.locationId??s.sceneContext?.locationId;
 const caps=new Set(ctx.capabilities);
 if(ctx.locationId&&ctx.origin.locationId===ctx.locationId)caps.add('related_location');
 if(s.sceneContext?.downtime)caps.add('downtime');
 ctx.capabilities=[...caps];return ctx;
}
function matrixOfferCheck(s,offer){
 let index=offer.matrixRecordIndex;
 if(index===undefined)index=s.matrixHistory?.findIndex(r=>r.commandId===offer.source);
 const record=s.matrixHistory?.[index];
 if(!record)return {valid:false,reasons:[{reason:'missing_audit_record'}]};
 if(s.matrixProfile?.packId!==record.packId)return {valid:false,reasons:[{reason:'profile_changed'}]};
 try{
  const ctx=liveMatrixContext(s,{...record.contextAudit,characterId:record.result.characterId,originEventId:record.originEventId,mode:s.matrixProfile.mode});
  ctx.recent=(s.matrixHistory??[]).filter((_,i)=>i!==index);
  return revalidateMatrixProposal(record.result,ctx);
 }catch(error){return {valid:false,reasons:[{reason:'live_context_invalid',message:error.message}]};}
}

function isMatrixOffer(s,offer){return offer.matrixRecordIndex!==undefined||offer.templateId?.startsWith('matrix-')||(s.history??[]).some(h=>h.type==='command'&&h.commandType==='matrix_present'&&h.id===offer.source);}
    return { createLifeStore, assertLifeStore, transact, buildSceneContext, buildNpcContext, synchronizeLegacy, ENTITY_KINDS };

  })(LegacyEngine, MatrixEngine, LifeArcs, MatrixAutomation);

  // --- 6. RUNTIME ---
  const RuntimeEngine = (function(LifeEngine, MatrixAutomation) {
    const { transact, buildSceneContext, assertLifeStore } = LifeEngine;
    const { planAutomaticMatrices } = MatrixAutomation;
/** Trusted host facade. All effects, automatic draws and schedules commit atomically. */
class LifeRuntime {
 constructor(repository,key,state,{matrixProvider}={}){this.repository=repository;this.key=key;this.state=assertLifeStore(state);this.queue=Promise.resolve();this.matrixProvider=matrixProvider;}
 static async open(repository,key,initial,options={}){let state=await repository.load(key);if(!state){await repository.save(key,initial,null);state=initial;}return new LifeRuntime(repository,key,state,options);}
 dispatch(command){const operation=this.queue.then(async()=>{
  const before=this.state;let next=transact(before,command,{verifiedByHost:true,expectedRevision:before.revision});
  if(next.revision===before.revision)return structuredClone(next);
  const plan=planAutomaticMatrices(next,command);
  if(plan.length){
   if(!this.matrixProvider)throw Error('Automação habilitada sem provedor de matrizes');
   const bundle=await this.matrixProvider(next.matrixProfile.packId);
   for(const item of plan){
    if(next.budget.used>=next.budget.max)break;
    next=transact(next,{id:item.id,type:'matrix_draw',bundle,context:item,occurrenceChance:item.occurrenceChance},{verifiedByHost:true});
    next=transact(next,{id:`schedule-${item.id}`,type:'matrix_schedule',characterId:item.characterId,nextDueDay:next.day+next.matrixAutomation.cooldownDays},{verifiedByHost:true});
    if(next.matrixAutomation.autoPresent){
     const offer=Object.values(next.offers).find(o=>o.source===item.id&&o.status==='pending');
     if(offer)next=transact(next,{id:`present-${item.id}`,type:'matrix_present',offerId:offer.id},{verifiedByHost:true});
    }
   }
  }
  await this.repository.save(this.key,next,before.revision);this.state=next;return structuredClone(next);
 });this.queue=operation.catch(()=>{});return operation;}
 snapshot(){return structuredClone(this.state);}
 scene(options){return buildSceneContext(this.state,options);}
 async reload(){await this.queue;const s=await this.repository.load(this.key);if(!s)throw Error('Save inexistente');this.state=assertLifeStore(s);return this.snapshot();}
}
    return { LifeRuntime };

  })(LifeEngine, MatrixAutomation);

  // Unpack module exports for adapter and global export
  const { createLegacyStore, assertLegacyStore, validateAdventure, beginAdventure, finishAdventure } = LegacyEngine;
  const { selectMatrixProfile, validateMatrixBundle, checkMatrixCoherence, sampleCrossedMatrix } = MatrixEngine;
  const { createLifeStore, assertLifeStore, transact, buildSceneContext } = LifeEngine;
  const { planAutomaticMatrices } = MatrixAutomation;
  const { LifeRuntime } = RuntimeEngine;

  // --- 7. OSE MATRIX BUNDLE PROVIDER ---
  const _bundleCache = new Map();

  function getOseMatrixBundle(packId) {
    if (_bundleCache.has(packId)) return _bundleCache.get(packId);
    let bundle = null;
    if (typeof process !== 'undefined' && process.versions && process.versions.node) {
      try {
        const fs = process.getBuiltinModule('fs');
        const path = process.getBuiltinModule('path');
        const candidates = [
          path.resolve(__dirname, 'braseiro_life_v2/data/matrices/packs', packId + '.json'),
          path.resolve(process.cwd(), 'ose/1.0.0/source/web-ui-p1/braseiro_life_v2/data/matrices/packs', packId + '.json'),
          path.resolve(process.cwd(), 'data/matrices/packs', packId + '.json')
        ];
        for (const c of candidates) {
          if (fs.existsSync(c)) {
            const raw = JSON.parse(fs.readFileSync(c, 'utf8'));
            bundle = validateMatrixBundle({
              schema: 'braseiro.matrix-bundle/v1',
              pack: raw.pack,
              tables: raw.tables
            });
            break;
          }
        }
      } catch (err) {
        // Fallback below
      }
    }
    if (!bundle) {
      // Fallback base bundle for browser environments
      bundle = validateMatrixBundle({
        schema: 'braseiro.matrix-bundle/v1',
        pack: {
          id: packId,
          title: packId === 'ose-advanced' ? 'Old-School Essentials Advanced Fantasy' : 'Old-School Essentials Classic Fantasy',
          system: packId === 'ose-advanced' ? 'ose-advanced' : 'ose-classic',
          mode: packId === 'ose-advanced' ? 'advanced' : 'classic',
          version: '2.2.0',
          tables: {
            cause: 'causes',
            domain: 'domains',
            manifestation: 'manifestations-' + packId,
            timing: 'timing',
            response: 'responses'
          }
        },
        tables: {
          cause: [
            { id: 'cause-victory-1', originCode: 'cause-victory-1', text: 'ameaça derrotada', causeTags: ['victory', 'reputation'], agency: 'verified_origin', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'cause-care-1', originCode: 'cause-care-1', text: 'repouso e cuidado', causeTags: ['care', 'recovery'], agency: 'verified_origin', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'cause-journey-1', originCode: 'cause-journey-1', text: 'viagem e travessia', causeTags: ['journey', 'discovery'], agency: 'verified_origin', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] }
          ],
          domain: [
            { id: 'dom-reputation', domain: 'reputation', text: 'reputação e notícias', agency: 'context_only', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'dom-social', domain: 'social', text: 'vida social e tavernas', agency: 'context_only', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'dom-world', domain: 'world', text: 'mundo e rotas comerciais', agency: 'context_only', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] }
          ],
          manifestation: [
            { id: packId + '-sit-1-observe', baseSituationId: 'sit-1', text: 'Notícias da expedição circulam entre caravaneiros e viajantes nas estalagens', domain: 'reputation', causeTags: ['victory', 'reputation'], agency: 'narrative_opportunity', sceneMode: 'observe', setting: packId, allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: packId + '-sit-2-observe', baseSituationId: 'sit-2', text: 'Relatos do descanso seguro no vilarejo chegam aos ouvidos dos guardas locais', domain: 'social', causeTags: ['care', 'recovery'], agency: 'narrative_opportunity', sceneMode: 'observe', setting: packId, allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: packId + '-sit-3-observe', baseSituationId: 'sit-3', text: 'Trilhas recentemente percorridas voltam a receber patrulhas e batedores', domain: 'world', causeTags: ['journey', 'discovery'], agency: 'narrative_opportunity', sceneMode: 'observe', setting: packId, allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] }
          ],
          timing: [
            { id: 'timing-immediate-1', text: 'primeiras repercussões', agency: 'schedule_only', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'timing-settling-1', text: 'ajuste nos dias seguintes', agency: 'schedule_only', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] }
          ],
          response: [
            { id: 'resp-public-1', text: 'A repercussão se torna pública na região', agency: 'player_choice', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] },
            { id: 'resp-witness-1', text: 'Testemunhas confirmam os acontecimentos', agency: 'player_choice', allowedStages: ['immediate', 'settling', 'established', 'dormant', 'long_term'] }
          ]
        }
      });
    }
    _bundleCache.set(packId, bundle);
    return bundle;
  }

  // --- 8. MEMORY REPOSITORY ---
  class LifeMemoryRepo {
    constructor() { this.data = new Map(); }
    async load(key) { return this.data.has(key) ? structuredClone(this.data.get(key)) : null; }
    async save(key, state, expectedRevision) {
      if (expectedRevision !== null && expectedRevision !== undefined) {
        const current = this.data.get(key);
        if (current && current.revision !== expectedRevision) throw Error('BRASEIRO_LIFE: conflito de revisão');
      }
      this.data.set(key, structuredClone(state));
    }
  }

  // --- 9. VTT LIFE ADAPTER ---
  class VttLifeAdapter {
    constructor(options = {}) {
      this.postNative = options.postNative || (typeof postNative === 'function' ? postNative : null);
      this.store = null;
      this.runtime = null;
      this.repo = new LifeMemoryRepo();
      this.scope = options.scope || 'default';
      this.processedReceipts = new Set();
      this.matrixProvider = options.matrixProvider || getOseMatrixBundle;
    }

    initFromViewState(viewState) {
      if (!viewState) return;
      const sys = (viewState.campaign?.profile || '').toLowerCase().includes('advanced') ? 'ose-advanced' : 'ose-classic';
      const worldId = viewState.campaign?.id || 'world';
      this.scope = viewState.campaign?.id || 'default';

      if (viewState.lifeStore && viewState.lifeStore.schema === 'braseiro.life/v2') {
        this.store = assertLifeStore(structuredClone(viewState.lifeStore));
        this.processedReceipts.clear();
      } else if (!this.store || this.store.legacy?.worldId !== worldId) {
        this.processedReceipts.clear();
        const legacy = createLegacyStore({ system: sys, worldId: worldId, timelineId: 'canonical' });
        this.store = createLifeStore(legacy);
        this.store = transact(this.store, { id: 'init-cfg', type: 'matrix_configure', packId: sys }, { verifiedByHost: true });
        this.store = transact(this.store, {
          id: 'init-auto',
          type: 'matrix_automation_configure',
          options: { enabled: true, autoPresent: true, cooldownDays: 7 }
        }, { verifiedByHost: true });
      }

      this.repo.save(worldId, this.store, null);
      this.runtime = new LifeRuntime(this.repo, worldId, this.store, { matrixProvider: this.matrixProvider });

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

      // 1. Process calendar advancement via true transact
      if (currentDay > this.store.day) {
        this.store = transact(this.store, {
          id: 'adv-day-' + currentDay,
          type: 'advance',
          toDay: currentDay
        }, { verifiedByHost: true });
        mutated = true;
      }

      // 2. Process newly confirmed receipts (strictly idempotent: exactly once per receiptId)
      for (const [receiptId, receipt] of Object.entries(receipts)) {
        if (this.processedReceipts.has(receiptId)) continue;

        // Causal authority (§11): strictly require confirmed referee mechanicalMutation === true
        if (receipt.mechanicalMutation !== true || receipt.unconfirmed === true || receipt.traceStatus === 'UNCONFIRMED') {
          continue;
        }

        this.processedReceipts.add(receiptId);
        this.store.legacy.processedEventIds.push(receiptId);

        const actorId = receipt.actorCharacterId || (viewState.party && viewState.party[0]?.id) || 'party';
        const summary = receipt.reactionText || receipt.mechanicalIntent || 'Ação confirmada da companhia';

        // A cause must be emitted by the authoritative referee and persisted with the receipt.
        // Neither mechanical feedback nor the player's text is proof of victory, healing or travel.
        const causeId = typeof receipt.lifeCauseId === 'string' ? receipt.lifeCauseId : '';
        if (!['cause-journey-1', 'cause-care-1', 'cause-victory-1', 'cause-loss-1', 'cause-discovery-1', 'cause-promise-1'].includes(causeId)) {
          continue;
        }

        const evt = {
          id: receiptId,
          day: this.store.day,
          turn: currentTurn,
          lifeCauseId: causeId,
          summary: summary,
          actorIds: [actorId],
          visibility: 'private',
          locationId: viewState.position?.primary?.spatialEntityId || 'village'
        };

        this.store = transact(this.store, {
          id: receiptId,
          type: 'event',
          event: evt
        }, { verifiedByHost: true });

        this.executeMatrixAutomation(evt, viewState);
        mutated = true;
      }

      if (mutated) {
        this.repo.save(this.store.legacy.worldId, this.store, null);
        this.persist();
      }
    }

    executeMatrixAutomation(originEvent, viewState) {
      if (this.store.budget.used >= this.store.budget.max) return;

      const packId = this.store.matrixProfile?.packId || 'ose-classic';
      const actorId = originEvent.actorIds[0] || 'party';
      const bundle = this.matrixProvider(packId);

      const caps = ['related_person', 'witness_available', 'related_location', 'downtime', 'record_available'];
      const ctx = {
        originEventId: originEvent.id,
        packId: packId,
        system: bundle.pack.system,
        mode: bundle.pack.mode,
        day: this.store.day,
        stage: this.store.day <= 1 ? 'immediate' : this.store.day <= 7 ? 'settling' : 'established',
        origin: {
          id: originEvent.id,
          day: originEvent.day,
          causeId: originEvent.lifeCauseId,
          actorIds: originEvent.actorIds,
          summary: originEvent.summary
        },
        characterId: actorId,
        maxIntensity: 2,
        capabilities: caps,
        facts: this.store.facts || {},
        recent: (this.store.matrixHistory || []).map(h => ({
          packId: h.packId,
          originEventId: h.originEventId,
          situationId: h.result?.situationId,
          day: h.day,
          characterId: actorId
        })),
        arcs: Object.values(this.store.lifeArcs || {})
      };

      const seed = ((this.store.rng * 1664525 + 1013904223) >>> 0);
      this.store.rng = seed;

      const drawResult = sampleCrossedMatrix(bundle, ctx, seed);
      if (drawResult.status !== 'proposal' || !drawResult.selection) return;

      const coherence = checkMatrixCoherence(bundle, drawResult.selection, ctx);
      if (!coherence.valid) return;

      const drawId = 'draw-' + originEvent.id;
      this.store = transact(this.store, {
        id: drawId,
        type: 'matrix_draw',
        bundle: bundle,
        context: ctx,
        occurrenceChance: 100
      }, { verifiedByHost: true });

      const pendingOffer = Object.values(this.store.offers).find(o => o.source === drawId && o.status === 'pending');
      if (pendingOffer) {
        this.store = transact(this.store, {
          id: 'present-' + drawId,
          type: 'matrix_present',
          offerId: pendingOffer.id
        }, { verifiedByHost: true });
      }

      const sitText = drawResult.selection.manifestation.text;
      const rumorId = 'rumor-' + originEvent.id;
      const rumorData = {
        id: rumorId,
        situationId: drawResult.selection.manifestation.baseSituationId || 'sit-ose',
        originEventId: originEvent.id,
        characterId: actorId,
        packId: packId,
        day: this.store.day,
        status: 'active',
        text: sitText + ' (Repercussão confirmada de: ' + originEvent.summary + ')'
      };

      this.store.entities[rumorId] = {
        id: rumorId,
        kind: 'information',
        title: sitText,
        visibility: 'public',
        status: 'active',
        actorIds: [actorId],
        data: rumorData
      };
    }

    persist() {
      if (!this.store) return;
      if (typeof this.postNative === 'function') {
        this.postNative('LifeStoreSync', {
          scope: this.scope,
          stateJson: JSON.stringify(this.store),
          store: this.store,
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

  // --- 10. GLOBAL EXPORTS ---
  const adapter = new VttLifeAdapter();
  root.VttLifeAdapter = VttLifeAdapter;
  root.vttLifeAdapter = adapter;
  root.LifeRuntime = LifeRuntime;
  root.transact = transact;
  root.buildSceneContext = buildSceneContext;
  root.sampleCrossedMatrix = sampleCrossedMatrix;
  root.checkMatrixCoherence = checkMatrixCoherence;
  root.validateMatrixBundle = validateMatrixBundle;
  root.selectMatrixProfile = selectMatrixProfile;
  root.createLegacyStore = createLegacyStore;
  root.createLifeStore = createLifeStore;

})(typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : this));
