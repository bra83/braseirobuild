import { newId } from '../core/ids.mjs';
import {
  normalizeWorld, createLocation, upsertLocation, createFaction, upsertFaction, setFactionRelation,
  createFactionPlan, createNpc, upsertNpc, recordNpcFact, scheduleWorldEvent, advanceWorld
} from './world.mjs';
import { ensureLivingWorldDepth, advanceLivingWorldDepth, livingWorldSnapshot, livingWorldSummary } from './livingWorldDepth.mjs';

const clone=v=>structuredClone(v);
const SANDBOX_SOURCE=Object.freeze({kind:'sandbox_seed',label:'Seed local de mundo vivo',note:'Estrutura inicial sem roteiro obrigatório; fatos canônicos adicionais devem vir do RAG/fontes.'});
function text(v,max=500){return String(v??'').trim().slice(0,max);}
function int(v,{min=0,max=1_000_000}={}){const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error('Valor inteiro inválido.');return n;}
function ensure(world){
  const w=normalizeWorld(world);
  w.sandbox={initialized:false,seedVersion:1,regionLabel:'',startLocationId:null,createdAtTick:w.clock.tick,...(w.sandbox&&typeof w.sandbox==='object'?w.sandbox:{})};
  w.clocks=w.clocks&&typeof w.clocks==='object'&&!Array.isArray(w.clocks)?w.clocks:{};
  w.opportunities=Array.isArray(w.opportunities)?w.opportunities:[];
  w.markets=w.markets&&typeof w.markets==='object'&&!Array.isArray(w.markets)?w.markets:{};
  w.routes=Array.isArray(w.routes)?w.routes:[];
  return w;
}

export function normalizeSandboxWorld(world){return ensureLivingWorldDepth(ensure(world));}

export function createWorldClock(world,{id=null,label,segments=4,progress=0,factionId=null,planId=null,locationId=null,knownToPlayer=false,stakes=''}={}){
  const w=ensure(world); const clean=text(label,160); if(!clean) throw new Error('Relógio exige nome.');
  const max=int(segments,{min:2,max:12}), value=int(progress,{min:0,max});
  if(factionId&&!w.factions[factionId]) throw new Error('Facção do relógio não encontrada.');
  if(locationId&&!w.locations[locationId]) throw new Error('Local do relógio não encontrado.');
  const clock={id:id||newId('clock'),label:clean,segments:max,progress:value,status:value>=max?'completed':'active',factionId:factionId||null,planId:planId||null,locationId:locationId||null,knownToPlayer:Boolean(knownToPlayer),stakes:text(stakes,500),source:clone(SANDBOX_SOURCE)};
  w.clocks[clock.id]=clock; return {world:w,clock:clone(clock)};
}
export function advanceWorldClock(world,clockId,{steps=1}={}){
  const w=ensure(world), c=w.clocks[clockId]; if(!c) throw new Error('Relógio não encontrado.');
  if(c.status==='completed') return {world:w,clock:clone(c),advanced:0};
  const n=int(steps,{min:0,max:12}); const before=c.progress; c.progress=Math.min(c.segments,c.progress+n); c.status=c.progress>=c.segments?'completed':'active';
  w.history.push({id:newId('whist'),type:'advance_clock',tick:w.clock.tick,clockId:c.id,before,after:c.progress,status:c.status});
  return {world:w,clock:clone(c),advanced:c.progress-before};
}

const OPPORTUNITY_STATUSES=new Set(['available','accepted','resolved','expired','dismissed']);
export function createOpportunity(world,{id=null,kind='rumor',title,summary='',locationId=null,factionId=null,npcId=null,reward=null,expiresTick=null,knownToPlayer=true,tags=[],sourceHookId=null}={}){
  const w=ensure(world); const clean=text(title,180); if(!clean) throw new Error('Oportunidade exige título.');
  if(locationId&&!w.locations[locationId]) throw new Error('Local da oportunidade não encontrado.');
  if(factionId&&!w.factions[factionId]) throw new Error('Facção da oportunidade não encontrada.');
  if(npcId&&!w.npcs[npcId]) throw new Error('PNPC da oportunidade não encontrado.');
  const item={id:id||newId('opp'),kind:text(kind,60)||'rumor',title:clean,summary:text(summary,1000),status:'available',locationId:locationId||null,factionId:factionId||null,npcId:npcId||null,reward:reward===null?null:clone(reward),expiresTick:expiresTick===null||expiresTick===undefined?null:int(expiresTick,{min:w.clock.tick,max:1_000_000_000}),knownToPlayer:Boolean(knownToPlayer),tags:(Array.isArray(tags)?tags:[]).map(x=>text(x,80)).filter(Boolean).slice(0,20),sourceHookId:sourceHookId||null,createdAtTick:w.clock.tick,resolvedAtTick:null,resolution:null,source:clone(SANDBOX_SOURCE)};
  w.opportunities.push(item); return {world:w,opportunity:clone(item)};
}
export function setOpportunityStatus(world,opportunityId,status,{resolution=''}={}){
  const w=ensure(world), item=w.opportunities.find(o=>o.id===opportunityId); if(!item) throw new Error('Oportunidade não encontrada.');
  const next=String(status); if(!OPPORTUNITY_STATUSES.has(next)) throw new Error('Estado de oportunidade inválido.');
  if(['resolved','expired','dismissed'].includes(item.status)) throw new Error('Oportunidade já encerrada.');
  item.status=next; if(['resolved','expired','dismissed'].includes(next)){item.resolvedAtTick=w.clock.tick;item.resolution=text(resolution,1000)||null;}
  w.history.push({id:newId('whist'),type:'opportunity_status',tick:w.clock.tick,opportunityId:item.id,status:item.status,resolution:item.resolution});
  return {world:w,opportunity:clone(item)};
}
export function activeOpportunities(world,{locationId=null,knownOnly=false,limit=50}={}){
  const w=ensure(world), max=int(limit,{min:1,max:200});
  return w.opportunities.filter(o=>['available','accepted'].includes(o.status)&&(!locationId||o.locationId===locationId)&&(!knownOnly||o.knownToPlayer)).slice(0,max).map(clone);
}

export function marketProfile(world,locationId){
  const w=ensure(world); if(!locationId||!w.locations[locationId]) return null; const m=w.markets[locationId]; return m?clone(m):null;
}
export function setMarketProfile(world,locationId,{rarityModifier=0,restrictedAccess='conditional',tags=[],notes=''}={}){
  const w=ensure(world); if(!w.locations[locationId]) throw new Error('Local do mercado não encontrado.');
  const mod=Number(rarityModifier); if(!Number.isInteger(mod)||mod<-5||mod>5) throw new Error('Modificador de raridade do mercado inválido.');
  w.markets[locationId]={locationId,rarityModifier:mod,restrictedAccess:text(restrictedAccess,80)||'conditional',tags:(Array.isArray(tags)?tags:[]).map(x=>text(x,80)).filter(Boolean),notes:text(notes,600),updatedAtTick:w.clock.tick,source:clone(SANDBOX_SOURCE)}; return w;
}
export function connectLocations(world,{fromLocationId,toLocationId,ticks=1,risk='normal',bidirectional=true}={}){
  const w=ensure(world); if(!w.locations[fromLocationId]||!w.locations[toLocationId]) throw new Error('Rota exige locais existentes.'); if(fromLocationId===toLocationId) throw new Error('Rota exige dois locais distintos.');
  const t=int(ticks,{min:1,max:1000});
  const add=(a,b)=>{if(!w.routes.some(r=>r.fromLocationId===a&&r.toLocationId===b))w.routes.push({id:newId('route'),fromLocationId:a,toLocationId:b,ticks:t,risk:text(risk,80)||'normal',source:clone(SANDBOX_SOURCE)});}; add(fromLocationId,toLocationId); if(bidirectional)add(toLocationId,fromLocationId); return w;
}
export function routeBetween(world,fromLocationId,toLocationId){const w=ensure(world);return clone(w.routes.find(r=>r.fromLocationId===fromLocationId&&r.toLocationId===toLocationId)??null);}

const NPC_NAMES=['Rian Taal','Kell Ardan','Siva Renn','Brel Karo','Tavi Mer','Juno Sarr','Vek Tal','Rin Varo','Cassa Nym','Daro Fen','Mira Kess','Orin Vale','Tess Ralo','Kira Denn','Brax Morn','Lio Tav','Vana Korr','Pell Naro','Senn Vek','Aris Daal','Nova Rinn','Kato Ver','Ress Mako','Dena Sol','Varo Kain','Lysa Tev','Mek Ronn','Jori Pell','Tala Venn','Kess Ordo','Riva Sar','Nilo Bren','Asha Korr','Dax Rell','Mara Vex','Teren Vos'];
const LOCATION_TEMPLATES=[
  ['Porto principal','porto','mercado'],['Cantina e bazar','social','mercado'],['Doca de reparos','oficina','naves'],['Posto alfandegário','autoridade','segurança'],
  ['Distrito industrial','indústria','oficina'],['Centro médico','medicina','civil'],['Entreposto de carga','carga','contrabando'],['Assentamento periférico','civil','fronteira'],
  ['Estação de retransmissão','comunicações','isolado'],['Complexo de mineração','mineração','trabalho'],['Campo de destroços','espaço','perigo'],['Refúgio remoto','oculto','fronteira']
];
function rotated(list,offset){const n=list.length||1,o=((offset%n)+n)%n;return list.slice(o).concat(list.slice(0,o));}
function seedEntropy(world){const base=String(world?.id||world?.campaignId||'')+':'+Date.now()+':'+Math.random();let h=2166136261;for(let i=0;i<base.length;i++){h^=base.charCodeAt(i);h=Math.imul(h,16777619);}return h>>>0;}

const FACTION_TEMPLATES=[
  ['Guarnição Imperial','Consolidar controle, inteligência e fluxo de cargas',['patrulhas','credenciais','apoio logístico'],'imperial'],
  ['Célula Rebelde','Proteger aliados e ampliar capacidade contra o Império',['rede clandestina','informantes'],'rebelde'],
  ['Sindicato do Submundo','Expandir lucro e influência sobre rotas ilegais',['contrabandistas','cobradores'],'submundo'],
  ['Liga de Comerciantes','Manter comércio funcionando e proteger margens',['crédito','armazéns'],'comércio'],
  ['Cooperativa Local','Defender população, trabalho e suprimentos',['trabalhadores','abrigo'],'civil'],
  ['Caçadores e Autônomos','Obter contratos, recompensas e acesso a alvos',['rastreadores','naves leves'],'autônomos']
];

export function seedLivingRegion(world,{startingPoint='Porto de Fronteira',regionLabel='Região de Fronteira'}={}){
  let w=ensure(world); if(w.sandbox.initialized) return {world:w,created:false,startLocationId:w.sandbox.startLocationId};
  const entropy=seedEntropy(w);
  const npcNames=rotated(NPC_NAMES,entropy%NPC_NAMES.length);
  const factionTemplates=rotated(FACTION_TEMPLATES,(entropy>>>8)%FACTION_TEMPLATES.length);
  const locs=[];
  for(let i=0;i<LOCATION_TEMPLATES.length;i++){
    const [base,...tags]=LOCATION_TEMPLATES[i]; const name=i===0?text(startingPoint,120)||base:`${base} ${i+1}`;
    const loc=createLocation({name,description:`${base} em ${regionLabel}. Área associada a ${tags.join(' e ')}, com tráfego, interesses locais e condições que evoluem conforme as ações do grupo.`,knownToPlayer:i<4,tags}); w=upsertLocation(w,loc); locs.push(loc);
  }
  const factions=[];
  for(const [name,objective,resources,tag] of factionTemplates){const f=createFaction({name,objective,resources,knownToPlayer:['imperial','rebelde','comércio'].includes(tag),tags:[tag,'sandbox']}); w=upsertFaction(w,f); factions.push(f);}
  // Relações iniciais estruturam tensão, sem determinar a história.
  w=setFactionRelation(w,factions[0].id,factions[1].id,'hostil'); w=setFactionRelation(w,factions[1].id,factions[0].id,'hostil');
  w=setFactionRelation(w,factions[2].id,factions[3].id,'competitiva'); w=setFactionRelation(w,factions[3].id,factions[2].id,'desconfiada');
  w=setFactionRelation(w,factions[4].id,factions[0].id,'pressionada'); w=setFactionRelation(w,factions[5].id,factions[2].id,'transacional');
  const npcs=[];
  let ni=0;
  for(let fi=0;fi<factions.length;fi++){
    for(let j=0;j<6;j++){
      const loc=locs[(fi*2+j)%locs.length]; const f=factions[fi];
      const npc=createNpc({name:npcNames[ni++ % npcNames.length],locationId:loc.id,factionId:f.id,goal:`Avançar interesses de ${f.name} sem expor todos os recursos disponíveis.`,relation:j===0?'cautelosa':'neutral',status:'active',knownToPlayer:j<2,tags:['sandbox',f.tags[0]??'local']});
      w=upsertNpc(w,npc); w=recordNpcFact(w,npc.id,{text:`Agenda privada ligada ao objetivo atual de ${f.name}.`,knownToPlayer:false}); w=recordNpcFact(w,npc.id,{text:`Tem conexão operacional com ${loc.name}.`,knownToPlayer:j===0}); npcs.push(npc);
    }
  }
  const clocks=[];
  for(let i=0;i<factions.length;i++){
    const f=factions[i]; const steps=[0,1,2,3].map(j=>({text:`Etapa ${j+1}: pressionar a situação em ${locs[(i+j)%locs.length].name}`,locationId:locs[(i+j)%locs.length].id,knownToPlayer:false}));
    const created=createFactionPlan(w,f.id,{title:`Plano ativo de ${f.name}`,objective:f.objective,steps,knownToPlayer:false}); w=created.world;
    const c=createWorldClock(w,{label:`Movimento de ${f.name}`,segments:4,factionId:f.id,planId:created.plan.id,locationId:locs[i].id,knownToPlayer:false,stakes:f.objective}); w=c.world; clocks.push(c.clock);
    for(let step=0;step<5;step++){const due=2+i+step*4; w=scheduleWorldEvent(w,{type:'advance_faction_plan',dueTick:due,payload:{factionId:f.id,planId:created.plan.id,locationId:locs[(i+step)%locs.length].id},dedupeKey:`sandbox-plan:${f.id}:${step}`}).world;}
  }
  // Mercados e rotas: dados locais explícitos, usados pela raridade em vez de inferência pelo nome do planeta.
  for(let i=0;i<locs.length;i++){
    w=setMarketProfile(w,locs[i].id,{rarityModifier:[-1,0,0,1,1,0,0,2,2,1,3,2][i],restrictedAccess:i<4?'controlled':'conditional',tags:locs[i].tags,notes:'Perfil local de disponibilidade do sandbox.'});
    if(i>0) w=connectLocations(w,{fromLocationId:locs[0].id,toLocationId:locs[i].id,ticks:Math.max(1,Math.ceil(i/3)),risk:i>8?'alto':i>4?'moderado':'normal'});
  }
  const kinds=['rumor','contract','market','distress'];
  for(let i=0;i<12;i++){
    const loc=locs[i], f=factions[i%factions.length], npc=npcs[i*3];
    const created=createOpportunity(w,{kind:kinds[i%kinds.length],title:`Sinal em ${loc.name}`,summary:`Há uma situação aberta em ${loc.name} ligada a ${f.name}. O jogador pode investigar, negociar, ignorar ou partir; não existe solução obrigatória.`,locationId:loc.id,factionId:f.id,npcId:npc?.id??null,knownToPlayer:i<4,tags:['sandbox',kinds[i%kinds.length]],expiresTick:i<4?null:12+i}); w=created.world;
  }
  w.sandbox={initialized:true,seedVersion:2,seed:entropy,regionLabel:text(regionLabel,160)||'Região de Fronteira',startLocationId:locs[0].id,createdAtTick:w.clock.tick,source:clone(SANDBOX_SOURCE)};
  w.history.push({id:newId('whist'),type:'sandbox_seeded',tick:w.clock.tick,startLocationId:locs[0].id,factions:factions.length,npcs:npcs.length,locations:locs.length,opportunities:w.opportunities.length});
  w=ensureLivingWorldDepth(w);
  return {world:w,created:true,startLocationId:locs[0].id,counts:{factions:factions.length,npcs:npcs.length,locations:locs.length,opportunities:w.opportunities.length,clocks:clocks.length}};
}

function clockForPlan(w,planId){return Object.values(w.clocks).find(c=>c.planId===planId&&c.status!=='completed')??null;}
export function advanceLivingWorld(world,{ticks=1,maxEvents=50}={}){
  const before=ensure(world); const base=advanceWorld(before,{ticks,maxEvents}); let w=ensure(base.world); const clockAdvances=[]; const opportunitiesCreated=[];
  for(const p of base.processed){
    if(p.status==='done'&&p.type==='advance_faction_plan'){
      const event=w.events.find(e=>e.id===p.eventId), planId=event?.payload?.planId; const c=planId?clockForPlan(w,planId):null;
      if(c){const r=advanceWorldClock(w,c.id,{steps:1});w=r.world;clockAdvances.push(r.clock);}
    }
  }
  for(const hook of base.hooksCreated){
    if(w.opportunities.some(o=>o.sourceHookId===hook.id)) continue;
    const created=createOpportunity(w,{kind:'rumor',title:'Consequência em movimento',summary:hook.summary,locationId:hook.locationId,factionId:hook.factionId,npcId:hook.npcId,knownToPlayer:true,tags:['consequence'],sourceHookId:hook.id}); w=created.world; opportunitiesCreated.push(created.opportunity);
  }
  for(const o of w.opportunities){if(o.status==='available'&&o.expiresTick!==null&&o.expiresTick<=w.clock.tick){o.status='expired';o.resolvedAtTick=w.clock.tick;o.resolution='A oportunidade expirou com a passagem do tempo.';}}
  w=advanceLivingWorldDepth(w,{ticks});
  return {...base,world:w,clockAdvances,opportunitiesCreated,living:livingWorldSummary(w)};
}

export function sandboxOpening(world,{locationId=null,limit=6}={}){
  const w=ensure(world); const locId=locationId||w.sandbox.startLocationId; const loc=w.locations[locId]??null; const max=int(limit,{min:1,max:20});
  const localNpcs=Object.values(w.npcs).filter(n=>n.locationId===locId&&n.status==='active').slice(0,max).map(n=>({id:n.id,name:n.name,species:n.species,sex:n.sex,gender:n.gender,origin:n.origin,career:n.career,specialization:n.specialization,occupation:n.occupation,ageYears:n.ageYears,appearance:n.appearance,factionId:n.factionId,goal:n.goal,relation:n.relation,knownToPlayer:n.knownToPlayer,facts:(n.facts??[]).filter(f=>f.knownToPlayer)}));
  const opportunities=activeOpportunities(w,{locationId:locId,knownOnly:false,limit:max});
  const factionIds=new Set(localNpcs.map(n=>n.factionId).filter(Boolean)); for(const o of opportunities) if(o.factionId) factionIds.add(o.factionId);
  const factions=[...factionIds].map(id=>w.factions[id]).filter(Boolean).map(f=>({id:f.id,name:f.name,objective:f.objective,status:f.status,knownToPlayer:f.knownToPlayer}));
  const clocks=Object.values(w.clocks).filter(c=>c.status==='active'&&(c.locationId===locId||factionIds.has(c.factionId))).slice(0,max).map(clone);
  const hooks=(w.hooks??[]).filter(h=>h.status==='pending'&&h.locationId===locId).slice(0,max).map(clone);
  const living=livingWorldSnapshot(w,{locationId:locId,knownOnly:false,limit:max});
  return {ready:Boolean(w.sandbox.initialized&&loc),region:w.sandbox.regionLabel,startLocationId:w.sandbox.startLocationId,location:loc?clone(loc):null,market:marketProfile(w,locId),npcs:localNpcs,factions,opportunities,clocks,hooks,living,principle:'Apresente tensão e escolhas; não force missão, solução, facção ou aventura publicada.'};
}

export function sandboxSummary(world){const w=ensureLivingWorldDepth(ensure(world));return {sandbox:clone(w.sandbox),clockCount:Object.keys(w.clocks).length,activeClockCount:Object.values(w.clocks).filter(c=>c.status==='active').length,opportunityCount:w.opportunities.length,activeOpportunityCount:w.opportunities.filter(o=>['available','accepted'].includes(o.status)).length,marketCount:Object.keys(w.markets).length,routeCount:w.routes.length,living:livingWorldSummary(w)};}
