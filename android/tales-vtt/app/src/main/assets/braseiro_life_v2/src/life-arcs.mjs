/** Biographical arcs; no rule-sheet conditions, diagnoses or player actions are granted. */
const assert=(v,m)=>{if(!v)throw Error(`BRASEIRO_ARC: ${m}`);};
const id=x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
export const ARC_STATES=['candidate','active','dormant','resurfaced','resolving','resolved','rejected','expired'];
export function stableToken(value){let a=2166136261,b=2246822519;for(const ch of String(value)){a=Math.imul(a^ch.charCodeAt(0),16777619);b=Math.imul(b^ch.charCodeAt(0),3266489917);}return `${(a>>>0).toString(16)}${(b>>>0).toString(16)}`;}
export function validateCharacterProfile(p){
 assert(p&&id(p.characterId),'personagem inválido');
 for(const field of ['domainWeights','causeWeights','protections']){assert(!p[field]||(typeof p[field]==='object'&&!Array.isArray(p[field])),'perfil inválido');for(const [key,n] of Object.entries(p[field]??{}))assert(id(key)&&Number.isFinite(n)&&n>=0&&n<=5,'peso fora de 0 a 5');}
 if(p.maxIntensity!==undefined)assert(Number.isInteger(p.maxIntensity)&&p.maxIntensity>=0&&p.maxIntensity<=5,'intensidade inválida');
 assert((p.capabilities??[]).every(id),'capacidade inválida');
 assert(p.occurrenceChance===undefined||(Number.isInteger(p.occurrenceChance)&&p.occurrenceChance>=0&&p.occurrenceChance<=100),'chance inválida');
 return structuredClone(p);
}
export function characterCapabilities(s,characterId,provided){
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
export function arcIdentity(result){return `arc-${stableToken([result.packId,result.characterId,result.originEventId,result.situationId].join('|'))}`;}
function move(s,arc,status,reason,eventId){
 if(arc.status===status)return;
 const transition={from:arc.status,to:status,day:s.day,reason,...(eventId?{eventId}:{})};arc.transitions.push(transition);arc.status=status;arc.updatedDay=s.day;
}
export function registerLifeArc(s,result,recordIndex){
 s.lifeArcs??={};const key=arcIdentity(result);let arc=s.lifeArcs[key];
 if(!arc){arc=s.lifeArcs[key]={id:key,packId:result.packId,packVersion:result.version,characterId:result.characterId,originEventId:result.originEventId,situationId:result.situationId,domain:result.selection.manifestation.domain,status:'candidate',createdDay:s.day,updatedDay:s.day,nextDueDay:s.day+30,progress:0,intensity:result.selection.manifestation.intensity??1,transitions:[],recordIndices:[]};}
 else {assert(!['resolved','rejected'].includes(arc.status),'arco encerrado exige gatilho confirmado para reabrir');if(['dormant','expired'].includes(arc.status))move(s,arc,'candidate','nova oportunidade contextual');}
 arc.recordIndices.push(recordIndex);for(const offer of Object.values(s.offers))if(offer.arcId===key&&offer.status==='pending')offer.status='expired';return key;
}
export function presentLifeArc(s,arcId){const arc=s.lifeArcs?.[arcId];if(!arc)return;move(s,arc,arc.transitions.some(t=>['active','resurfaced'].includes(t.to))?'resurfaced':'active','manifestação apresentada');arc.nextDueDay=s.day+14;}
export function expireArcOffer(s,arcId,reason){const arc=s.lifeArcs?.[arcId];if(!arc||['resolved','rejected'].includes(arc.status))return;move(s,arc,'dormant',reason);arc.nextDueDay=s.day+14;}
export function chooseArcOpportunity(s,arcId,choice){const arc=s.lifeArcs?.[arcId];if(!arc)return;if(choice==='pass'){move(s,arc,'dormant','jogador não explorou a oportunidade');arc.nextDueDay=s.day+21;}else if(choice==='engage'){move(s,arc,'active','jogador demonstrou interesse; resultado ainda depende do jogo');arc.nextDueDay=s.day+14;}}
export function applyArcSignal(s,{arcId,outcome},event){
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
export function tickLifeArcs(s){
 for(const arc of Object.values(s.lifeArcs??{})){
  if(arc.nextDueDay===null||s.day<arc.nextDueDay)continue;
  if(arc.status==='candidate'){move(s,arc,'expired','proposta não usada no prazo');for(const o of Object.values(s.offers))if(o.arcId===arc.id&&o.status==='pending')o.status='expired';}
  else if(['active','resurfaced'].includes(arc.status)){move(s,arc,'dormant','intervalo sem novo acontecimento relevante');for(const o of Object.values(s.offers))if(o.arcId===arc.id&&o.status==='pending')o.status='expired';}
  else if(arc.status==='resolving'){move(s,arc,'dormant','evolução aguarda evidência; não há cura automática');}
 }
}
export function validateLifeArcs(s){
 for(const [key,arc] of Object.entries(s.lifeArcs??{})){assert(key===arc.id&&id(key)&&id(arc.characterId)&&ARC_STATES.includes(arc.status),'arco inválido');assert(Array.isArray(arc.transitions)&&arc.transitions.every(t=>ARC_STATES.includes(t.from)&&ARC_STATES.includes(t.to)&&Number.isSafeInteger(t.day)),'transições inválidas');}
 for(const p of Object.values(s.characterLifeProfiles??{}))validateCharacterProfile(p);
}
/** Experience and choices influence narrative selection, never grant traits or diagnoses. */
export function deriveCharacterProfile(s,characterId){
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
