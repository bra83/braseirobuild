/** Portable Braseiro V2. All changes are pure transactions; time is fictional integer days. */
import {assertLegacyStore} from './legacy-engine.mjs';
import {sampleCrossedMatrix,matrixContextFromStore,selectMatrixProfile,revalidateMatrixProposal} from './matrix-engine.mjs';
import {validateCharacterProfile,characterCapabilities,registerLifeArc,presentLifeArc,expireArcOffer,chooseArcOpportunity,applyArcSignal,tickLifeArcs,validateLifeArcs,deriveCharacterProfile} from './life-arcs.mjs';
import {validateAutomation} from './matrix-automation.mjs';
const cp = x => structuredClone(x);
const serial = x => JSON.parse(JSON.stringify(x));
const ok = (x,m) => {if(!x) throw Error(`BRASEIRO_LIFE: ${m}`);};
const id = x => typeof x === 'string' && /^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x) && !['__proto__','constructor','prototype'].includes(x);
const number = x => Number.isFinite(x);
const kinds = ['home','promise','npc_project','relationship','tradition','heirloom','reputation','opportunity','world_change','recognition','personal_project','succession','foreshadowing','resource','problem','adaptation','information','causal_chain','routine','adversary','life_arc'];
const statuses = ['active','dormant','resolved','expired','retired','dead','blocked'];
const get = (s,ref) => {ok(typeof ref==='string','referência inválida'); const [kind,key,field] = ref.split(':'); return kind==='fact' ? s.facts[key]?.value : kind==='entity' ? s.entities[key]?.[field] : undefined;};
export function createLifeStore(legacy,{day=0,seed=1}={}) {
  ok(Number.isSafeInteger(day)&&day>=0 && Number.isInteger(seed),'tempo/seed inválido');
  return {schema:'braseiro.life/v2',revision:0,legacy:assertLegacyStore(legacy),day,rng:seed>>>0,entities:{},facts:{},bindings:{},knowledge:[],events:[],receipts:{},rules:{},offers:{},history:[],rolls:[],uses:{},budget:{day,used:0,max:3}};
}
export function assertLifeStore(s) {
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
export function transact(store,command,{verifiedByHost=false,expectedRevision=store.revision}={}) {
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
export function buildSceneContext(s,{locationId,actorIds=[],audience='player',limit=6}={}) {
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
export function buildNpcContext(s,actorId) {
  return {actorId,knowledge:s.knowledge.filter(k=>k.actorId===actorId).map(k=>{const target=k.entityId?s.entities[k.entityId]:s.events.find(x=>x.id===k.eventId);return {referenceId:k.entityId??k.eventId,certainty:k.certainty,claim:k.claim??(k.certainty==='confirmed'?(target?.publicDescription??target?.publicStory??target?.summary??target?.title):undefined),source:k.source,learnedDay:k.day};}),relationships:Object.values(s.entities).filter(e=>e.kind==='relationship'&&e.actorIds?.includes(actorId)).map(e=>({id:e.id,title:e.title,actorIds:e.actorIds,dimensions:cp(e.dimensions??{})}))};
}
export function synchronizeLegacy(s,legacy) {
  const l=assertLegacyStore(legacy);ok(['system','worldId','timelineId'].every(k=>l[k]===s.legacy[k]),'universo incompatível');
  ok(l.revision>=s.legacy.revision,'legado antigo');const out=cp(s);out.legacy=l;out.revision++;return out;
}
export const ENTITY_KINDS=Object.freeze(kinds);

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
