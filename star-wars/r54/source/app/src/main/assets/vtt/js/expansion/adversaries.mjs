import { newId } from '../core/ids.mjs';
import { SKILLS } from './character.mjs';
import { NPC_CARD_CATALOG, npcCardByKey, searchNpcCards } from './npcCardCatalog.mjs';

const aorReference=(source)=>Object.freeze({...source,authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false});

const BASE_SOURCE = aorReference({
  title:'La Era de la Rebelión - Caja de Inicio - Libro de reglas',
  language:'es',
  section:'VII Adversarios',
  pages:[44,45]
});

const HARD_PURCHASE_SOURCE = aorReference({
  title:'La Era de la Rebelión - Una compra difícil',
  language:'es',
  section:'Llegada a Coruscant / Tiroteo',
  pages:[4,6,8]
});


export const ADVERSARY_PROFILES = Object.freeze({
  imperial_soldier: Object.freeze({
    key:'imperial_soldier',
    name:'Soldado do Exército Imperial',
    kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:3,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_distancia_ligeras','armas_distancia_pesadas','disciplina'],
    skills:{},
    talents:[],
    abilities:[],
    weapons:[
      {name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2}
    ],
    source:BASE_SOURCE
  }),
  imperial_officer: Object.freeze({
    key:'imperial_officer',
    name:'Oficial do Exército Imperial',
    kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:4,
    woundsThreshold:15,
    strainThreshold:null,
    groupSkills:[],
    skills:{alerta:2,armas_distancia_ligeras:2,conhecimentos:2,disciplina:3},
    talents:[{key:'adversario',name:'Adversário',rank:1}],
    abilities:[
      {key:'direcao_tatica',name:'Direção tática'},
      {key:'fogo_concentrado',name:'Fogo concentrado'}
    ],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3}],
    source:BASE_SOURCE
  }),
  customs_officer: Object.freeze({
    key:'customs_officer',
    name:'Oficial de Controle de Aduana',
    kind:'rival',
    characteristics:{fortaleza:2,agilidad:2,intelecto:3,astucia:3,voluntad:2,presencia:3},
    soak:2,
    woundsThreshold:12,
    strainThreshold:null,
    groupSkills:[],
    skills:{alerta:3,callejeo:3,coaccion:2,disciplina:2,percepcion:3},
    talents:[], abilities:[],
    weapons:[{name:'Pistola blaster leve',skill:'armas_distancia_ligeras',range:'Médio',damage:5,crit:4}],
    source:HARD_PURCHASE_SOURCE
  }),
  stormtrooper: Object.freeze({
    key:'stormtrooper',
    name:'Soldado de Assalto',
    kind:'minion',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:1},
    soak:5,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_cuerpo_a_cuerpo','armas_distancia_pesadas','atletismo','disciplina'],
    skills:{}, talents:[], abilities:[],
    weapons:[
      {name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:4,crit:2,pierce:2,lethal:1},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2}
    ],
    source:HARD_PURCHASE_SOURCE
  }),
  son_barab: Object.freeze({
    key:'son_barab',
    name:'Capitão Son Barab, ISB',
    kind:'nemesis',
    characteristics:{fortaleza:3,agilidad:3,intelecto:3,astucia:3,voluntad:4,presencia:3},
    soak:4,
    woundsThreshold:13,
    strainThreshold:15,
    groupSkills:[],
    skills:{alerta:2,armas_distancia_ligeras:2,callejeo:2,carisma:2,coaccion:2,disciplina:3,engano:2,frialdad:2,liderazgo:2},
    talents:[{key:'adversario',name:'Adversário',rank:1},{key:'golpes_letais',name:'Golpes letais',rank:2}],
    abilities:[],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3}],
    source:HARD_PURCHASE_SOURCE
  })
});


export const ALLY_NPC_CONTROL_MODES = Object.freeze({
  narrative:Object.freeze({key:'narrative',label:'Enfoque narrativo',summary:'Resolva o efeito do aliado narrativamente, sem rolagem própria.'}),
  abstract_combat:Object.freeze({key:'abstract_combat',label:'Combate abstrato',summary:'Aplique ao fim do assalto um efeito mecânico menor e perceptível sem turno completo.'}),
  delegated_combat:Object.freeze({key:'delegated_combat',label:'Combate encomendado',summary:'Um jogador controla o aliado/grupo durante o combate; o Mestre retoma o controle ao final.'})
});

export const AD_HOC_NPC_SOURCE = aorReference({
  title:'La Era de la Rebelión - Una compra difícil',
  section:'¡Socorro! ¡No tengo sus características!',
  guidance:'Características 2; limiar de Ferimentos 8; grau 1 nas perícias que o senso comum indique como treinadas.'
});

export function createAdHocRival({name='PNPC improvisado',trainedSkills=[]}={}){
  if(!Array.isArray(trainedSkills)) throw new Error('Perícias treinadas do PNPC devem ser uma lista.');
  const skills={};
  for(const skill of trainedSkills){
    if(!(skill in SKILLS)) throw new Error(`Perícia não mapeada para PNPC improvisado: ${skill}.`);
    skills[skill]=1;
  }
  return {
    id:newId('adv'), profileKey:'ad_hoc_rival', name:String(name||'PNPC improvisado').trim()||'PNPC improvisado', kind:'rival',
    range:'Médio',
    characteristics:{fortaleza:2,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:2, wounds:{current:0,threshold:8,perMember:8}, strain:null, membersTotal:1, skills, groupSkills:[], talents:[], abilities:[], weapons:[], criticalInjuries:[], status:'active',
    source:clone(AD_HOC_NPC_SOURCE)
  };
}

function clone(value){ return structuredClone(value); }
function assertProfile(profile){
  if(!profile || typeof profile!=='object') throw new Error('Perfil de adversário inválido.');
  if(!['minion','rival','nemesis'].includes(profile.kind)) throw new Error('Categoria de adversário inválida.');
}

export function createAdversary(profileKey,{count=1,range='Médio',name=null}={}){
  const profile=ADVERSARY_PROFILES[profileKey];
  assertProfile(profile);
  const members=profile.kind==='minion' ? Number(count) : 1;
  if(!Number.isInteger(members)||members<1||members>20) throw new Error('Quantidade de adversários inválida.');
  return {
    id:newId('adv'),
    profileKey,
    name:name?.trim() || (profile.kind==='minion' && members>1 ? `${profile.name} x${members}` : profile.name),
    kind:profile.kind,
    range,
    characteristics:clone(profile.characteristics),
    soak:profile.soak,
    wounds:{current:0,threshold:profile.woundsThreshold * members,perMember:profile.woundsThreshold},
    strain:profile.kind==='nemesis' ? {current:0,threshold:profile.strainThreshold ?? 0} : null,
    membersTotal:members,
    skills:clone(profile.skills),
    groupSkills:clone(profile.groupSkills),
    talents:clone(profile.talents),
    abilities:clone(profile.abilities),
    weapons:clone(profile.weapons),
    criticalInjuries:[],
    status:'active',
    source:clone(profile.source)
  };
}

export function aliveMinions(adversary){
  if(adversary?.kind!=='minion') return adversaryHealthStatus(adversary).incapacitated ? 0 : 1;
  const total=Number(adversary.membersTotal)||0;
  const per=Number(adversary.wounds?.perMember)||0;
  const current=Math.max(0,Number(adversary.wounds?.current)||0);
  if(total<1||per<1) return 0;
  const defeated=Math.min(total, current===0 ? 0 : Math.floor((current-1)/per));
  return Math.max(0,total-defeated);
}

export function adversarySkillRank(adversary,skillKey){
  if(!adversary) throw new Error('Adversário inválido.');
  if(!(skillKey in SKILLS)) throw new Error('Perícia de adversário não mapeada.');
  if(adversary.kind==='minion'){
    if(!adversary.groupSkills?.includes(skillKey)) return 0;
    return Math.max(0,aliveMinions(adversary)-1);
  }
  return Math.max(0,Number(adversary.skills?.[skillKey])||0);
}

export function adversaryCharacteristicForSkill(adversary,skillKey){
  const characteristic=SKILLS[skillKey];
  if(!characteristic) throw new Error('Perícia de adversário não mapeada.');
  return Number(adversary.characteristics?.[characteristic] ?? 0);
}

export function adversaryHealthStatus(adversary){
  if(!adversary?.wounds) throw new Error('Estado de adversário inválido.');
  const minionsAlive=adversary.kind==='minion' ? aliveMinionsRaw(adversary) : null;
  const woundIncapacitated=adversary.kind==='minion'
    ? minionsAlive<=0
    : Number(adversary.wounds.current)>Number(adversary.wounds.threshold);
  const strainIncapacitated=adversary.kind==='nemesis' && adversary.strain
    ? Number(adversary.strain.current)>Number(adversary.strain.threshold)
    : false;
  return {
    incapacitated:woundIncapacitated||strainIncapacitated,
    woundIncapacitated,
    strainIncapacitated,
    minionsAlive,
    label:woundIncapacitated||strainIncapacitated?'Incapacitado':'Ativo'
  };
}

function aliveMinionsRaw(adversary){
  const total=Number(adversary.membersTotal)||0;
  const per=Number(adversary.wounds?.perMember)||0;
  const current=Math.max(0,Number(adversary.wounds?.current)||0);
  if(total<1||per<1) return 0;
  const defeated=Math.min(total,current===0?0:Math.floor((current-1)/per));
  return Math.max(0,total-defeated);
}

export function applyAdversaryDamage(adversary,{damage,pierce=0,damageType='wounds'}={}){
  if(!adversary) throw new Error('Adversário inválido.');
  const raw=Number(damage), p=Number(pierce);
  if(!Number.isInteger(raw)||raw<0) throw new Error('Dano inválido.');
  if(!Number.isInteger(p)||p<0) throw new Error('Perfuração inválida.');
  if(!['wounds','strain'].includes(damageType)) throw new Error('Tipo de dano inválido.');
  const next=clone(adversary);
  const effectiveSoak=Math.max(0,Number(next.soak)-p);
  const suffered=Math.max(0,raw-effectiveSoak);
  const convertedToWounds=damageType==='strain' && next.kind!=='nemesis';
  if(damageType==='strain' && next.kind==='nemesis') next.strain.current+=suffered;
  else next.wounds.current+=suffered;
  const status=adversaryHealthStatus(next);
  next.status=status.incapacitated?'incapacitated':'active';
  return {adversary:next,rawDamage:raw,effectiveSoak,suffered,damageType:convertedToWounds?'wounds':damageType,convertedToWounds,status};
}

export function sufferAdversaryStrain(adversary,amount){
  const n=Number(amount);
  if(!Number.isInteger(n)||n<0) throw new Error('Quantidade de Tensão inválida.');
  const next=clone(adversary);
  if(next.kind==='nemesis' && next.strain){
    next.strain.current+=n;
    const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
    return {adversary:next,amount:n,convertedToWounds:false,status};
  }
  next.wounds.current+=n;
  const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
  return {adversary:next,amount:n,convertedToWounds:true,status};
}

export function applyAdversaryCritical(adversary,reason='Ferimento crítico'){
  const next=clone(adversary);
  if(next.kind==='minion'){
    const alive=aliveMinionsRaw(next);
    if(alive<=0) return {adversary:next,applied:false,woundsAdded:0,status:adversaryHealthStatus(next)};
    const per=next.wounds.perMember;
    const defeated=next.membersTotal-alive;
    const target=per*(defeated+1)+1;
    const add=Math.max(1,target-next.wounds.current);
    next.wounds.current+=add;
    const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
    return {adversary:next,applied:true,woundsAdded:add,status};
  }
  next.criticalInjuries.push({id:newId('advcrit'),reason,at:new Date().toISOString()});
  return {adversary:next,applied:true,woundsAdded:0,status:adversaryHealthStatus(next)};
}

export function upgradeDifficultyForAdversaryTalent(adversary){
  return Math.max(0,Number(adversary?.talents?.find(t=>t.key==='adversario')?.rank)||0);
}


function cardTalent(t){
  const name=String(t?.name??'').trim();
  const key=name.toLocaleLowerCase()==='adversary'?'adversario':name.toLocaleLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');
  return {key,name,rank:Math.max(1,Number(t?.rank)||1)};
}

export { NPC_CARD_CATALOG };
export function npcCardProfiles({kind=null,query=''}={}){
  const rows=query?searchNpcCards(query):NPC_CARD_CATALOG.slice();
  return kind?rows.filter(v=>v.kind===kind):rows;
}
export function createCardAdversary(profileKey,{count=1,range='Médio',name=null}={}){
  const profile=npcCardByKey(profileKey);
  if(!profile) throw new Error(`Ficha de NPC não encontrada: ${profileKey}`);
  if(!profile.characteristics || Object.keys(profile.characteristics).length!==6 || profile.soak===null || profile.woundsThreshold===null){
    throw new Error(`Ficha de NPC incompleta no compêndio: ${profileKey}`);
  }
  const members=profile.kind==='minion'?Number(count):1;
  if(!Number.isInteger(members)||members<1||members>20) throw new Error('Quantidade de adversários inválida.');
  const talents=(profile.talents??[]).map(cardTalent);
  const abilities=profile.abilitiesText?[{key:'card_abilities',name:profile.abilitiesText}]:[];
  const weapons=(profile.weapons??[]).map(w=>({
    name:w.name, skill:w.skill??null, skillName:w.skillName??null, range:w.range??null,
    damage:w.damage??null, crit:w.crit??null, qualitiesText:w.qualitiesText??'', raw:w.raw??'', parsed:Boolean(w.parsed)
  }));
  return {
    id:newId('adv'), profileKey, name:name?.trim() || (profile.kind==='minion'&&members>1?`${profile.name} x${members}`:profile.name),
    kind:profile.kind, range, characteristics:clone(profile.characteristics), soak:Number(profile.soak),
    defense:clone(profile.defense??{melee:0,ranged:0}),
    wounds:{current:0,threshold:Number(profile.woundsThreshold)*members,perMember:Number(profile.woundsThreshold)},
    strain:profile.kind==='nemesis'?{current:0,threshold:Number(profile.strainThreshold??0)}:null,
    membersTotal:members, skills:clone(profile.skills??{}), groupSkills:clone(profile.groupSkills??[]), talents, abilities, weapons,
    criticalInjuries:[], status:'active', gearText:profile.gearText??null, unmappedSkills:clone(profile.unmappedSkills??[]),
    source:{type:'user_owned_npc_card_compendium',title:'Star_Wars_RPG_NPC_Cards_All_v1.1.pdf',cardPage:profile.cardPage,pdfPage:profile.pdfPage,reference:profile.sourceCard,mechanicsStatus:profile.mechanicsStatus}
  };
}


export function adversaryProfileExists(profileKey){
  return Boolean(ADVERSARY_PROFILES[profileKey] || npcCardByKey(profileKey));
}
export function createAnyAdversary(profileKey,options={}){
  if(ADVERSARY_PROFILES[profileKey]) return createAdversary(profileKey,options);
  if(npcCardByKey(profileKey)) return createCardAdversary(profileKey,options);
  throw new Error(`Perfil de adversário não encontrado: ${profileKey}`);
}
export function adversaryCompendiumSearch(query='', {kind=null, combatReadyOnly=false}={}){
  const q=String(query??'').trim().toLocaleLowerCase();
  const curated=Object.entries(ADVERSARY_PROFILES).map(([key,v])=>({key,name:v.name,kind:v.kind,source:'curated_runtime',combatReady:true}));
  const cards=(q?searchNpcCards(query):NPC_CARD_CATALOG).map(v=>({key:v.key,name:v.name,kind:v.kind,source:'user_owned_npc_card_compendium',combatReady:Boolean(v.characteristics&&Object.keys(v.characteristics).length===6&&v.soak!==null&&v.woundsThreshold!==null),sourceCard:v.sourceCard}));
  let rows=[...curated,...cards];
  if(q) rows=rows.filter(v=>[v.key,v.name,v.sourceCard].filter(Boolean).join(' ').toLocaleLowerCase().includes(q));
  if(kind) rows=rows.filter(v=>v.kind===kind);
  if(combatReadyOnly) rows=rows.filter(v=>v.combatReady);
  return rows;
}
