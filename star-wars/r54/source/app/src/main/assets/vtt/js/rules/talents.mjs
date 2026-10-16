import { CHARACTERISTICS, SKILLS } from './character.mjs';
import { TALENT_TREES } from './talentTreesData.mjs';
import { AOR_TALENT_TREES } from '../expansion/aorTalentTreesData.mjs';
import { AOR_RUNTIME_MECHANICAL_TALENTS, aorCanonicalTalentName } from '../expansion/aorTalentRuntimeAliases.mjs';
import { ownedSpecializationRefs, ownsSpecialization } from './progression.mjs';
import { FORCE_SENSITIVE_EXILE_TREE } from './forceData.mjs';
import { forceRating, isForceSensitive, availableForceRating, rollForceDice } from './force.mjs';

function clone(value){ return structuredClone(value); }
function purchases(c){ return Array.isArray(c?.talentPurchases)?c.talentPurchases:[]; }

const ACTIVE_TALENT_TREES=Object.freeze({...TALENT_TREES,...AOR_TALENT_TREES,universal:Object.freeze({force_sensitive_exile:FORCE_SENSITIVE_EXILE_TREE})});
const allNodes=Object.values(ACTIVE_TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes));
// Prefer the established EotE labels when resolving existing character.talents.
const eoteNodes=Object.values(TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes));
const PT_BY_ORIGINAL=new Map([...allNodes,...eoteNodes].map(n=>[n.originalName,n.name]));

function treeRef(character,{careerKey=null,specializationKey=null,nodeId=null}={}){
  // AoR purchases use the same authoritative specialization ownership ledger
  // as EotE. A node ID can select only a tree the character actually owns.
  // The AoR source catalog is isolated from EotE and universal trees.
  if (character?.creation?.rulesProfile === 'AOR_CORE') {
    const refs=ownedSpecializationRefs(character);
    let career=careerKey, spec=specializationKey;
    if(nodeId && (!career || !spec)){
      const found=refs.find(ref=>AOR_TALENT_TREES?.[ref.careerKey]?.[ref.specializationKey]?.nodes?.some(n=>n.id===nodeId));
      if(found){career=found.careerKey;spec=found.specializationKey;}
    }
    career ||= character.creation.careerKey;
    spec ||= character.creation.specializationKey;
    const owned=refs.some(ref=>ref.careerKey===career && ref.specializationKey===spec);
    const tree=owned ? AOR_TALENT_TREES?.[career]?.[spec] ?? null : null;
    return {career,spec,tree,owned:Boolean(tree)};
  }
  const owned=ownedSpecializationRefs(character);
  let career=careerKey;
  let spec=specializationKey;
  if(nodeId && (!career || !spec)){
    const found=owned.find(ref=>ACTIVE_TALENT_TREES?.[ref.careerKey]?.[ref.specializationKey]?.nodes?.some(node=>node.id===nodeId));
    if(found){ career=found.careerKey; spec=found.specializationKey; }
  }
  if(!career || !spec){
    career=character?.creation?.careerKey;
    spec=character?.creation?.specializationKey;
  }
  if(!ownsSpecialization(character,career,spec)) return {career,spec,tree:null,owned:false};
  return {career,spec,tree:ACTIVE_TALENT_TREES?.[career]?.[spec]??null,owned:true};
}
function ownedIds(character){ return new Set(purchases(character).map(p=>p.nodeId)); }

function neighborMap(tree){
  const map=new Map(tree.nodes.map(n=>[n.id,new Set()]));
  for(const [a,b] of tree.edges){
    map.get(a)?.add(b); map.get(b)?.add(a);
  }
  return map;
}

function unlockedIds(character,tree){
  const owned=ownedIds(character);
  const unlocked=new Set(tree.nodes.filter(n=>n.row===1).map(n=>n.id));
  if(!owned.size) return unlocked;
  const neighbors=neighborMap(tree);
  for(const id of owned) for(const n of (neighbors.get(id)??[])) unlocked.add(n);
  return unlocked;
}

function applyImmediateEffect(character,node,{characteristicKey=null,skillKey=null,skillKeys=null}={}){
  const c=character;
  switch(c.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(node.originalName) : node.originalName){
    case 'APLOMO':
    case 'GRIT':
      c.derived.strain.threshold+=1;
      return {applied:true,label:'+1 no limiar de Tensão'};
    case 'ENDURECIDO':
    case 'TOUGH':
      c.derived.wounds.threshold+=2;
      return {applied:true,label:'+2 no limiar de Ferimentos'};
    case 'RESISTENTE':
    case 'ENDUR':
      c.derived.soak+=1;
      return {applied:true,label:'+1 de Proteção'};
    case 'SEXTO SENTIDO':
    case 'SIXTHSENSE':
      c.derived.defense.ranged+=1;
      return {applied:true,label:'+1 de Defesa contra ataques à distância'};
    case 'SUPREF':
      c.derived.defense.melee+=1;
      return {applied:true,label:'+1 de Defesa corpo a corpo'};
    case 'INSIGHT':
      c.careerSkills=Array.isArray(c.careerSkills)?c.careerSkills:[];
      c.careerSkills=[...new Set([...c.careerSkills,'percepcao','disciplina'])];
      return {applied:true,label:'Percepção e Disciplina tornam-se perícias de carreira'};
    case 'FORCE_RATING':
      c.force={...(c.force??{}),rating:forceRating(c)+1};
      return {applied:true,label:'+1 Force Rating'};
    case 'DEDICACIÓN':
    case 'DEDI': {
      if(!CHARACTERISTICS.includes(characteristicKey)) throw new Error('Dedicação exige escolher uma Característica.');
      if(Number(c.characteristics?.[characteristicKey]??0)>=6) throw new Error('Dedicação não pode elevar uma Característica acima de 6.');
      c.characteristics[characteristicKey]+=1;
      if(characteristicKey==='fortaleza'){
        c.derived.soak+=1;
        c.derived.wounds.threshold+=1;
      }
      if(characteristicKey==='voluntad') c.derived.strain.threshold+=1;
      return {applied:true,label:`+1 em ${characteristicKey}`,characteristicKey};
    }
    case 'WELLROUND': {
      if (!Array.isArray(skillKeys) || skillKeys.length !== 2 ||
          skillKeys.some(key => typeof key !== 'string' || !Object.hasOwn(SKILLS,key)) ||
          skillKeys[0] === skillKeys[1])
        throw new Error('Polifacético exige exatamente duas perícias distintas e válidas.');
      c.careerSkills = [...new Set([...(c.careerSkills ?? []), ...skillKeys])];
      return {applied:true,label:`Polifacético: ${skillKeys.join(', ')}`,skillKeys:[...skillKeys]};
    }
    case 'SMOOTHTALK': {
      if(!SMOOTH_TALKER_SKILLS.includes(skillKey))throw new Error('Lábia exige escolher Carisma, Coação, Enganação ou Negociação.');
      return {applied:true,label:`Lábia: ${skillKey}`,skillKey};
    }
    case 'KNOWSPEC': {
      if(skillKey===null||skillKey===undefined)return {applied:false,label:'Pendente: escolher perícia de Conhecimento'};
      if(!KNOWLEDGE_SKILLS.includes(skillKey)) throw new Error('Conhecimento especializado exige escolher uma perícia de Conhecimento.');
      return {applied:true,label:`Conhecimento especializado: ${skillKey}`,skillKey};
    }
    case 'DEADACC': {
      const combatSkills=new Set(['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','armas_distancia_pesadas','artilleria']);
      if(!combatSkills.has(skillKey)) throw new Error('Precisão letal exige escolher uma perícia de combate.');
      return {applied:true,label:`Precisão letal vinculada a ${skillKey}`,skillKey};
    }
    default:
      return {applied:false,label:null};
  }
}

export { TALENT_TREES };

export const FIRST_ROW_TALENTS=Object.freeze(Object.fromEntries(
  Object.entries(ACTIVE_TALENT_TREES).map(([career,specs])=>[career,Object.fromEntries(
    Object.entries(specs).map(([spec,tree])=>[spec,tree.nodes.filter(n=>n.row===1).map(n=>({...n}))])
  )])
));

export function treeForCharacter(character,selector={}){
  const {career,spec,tree}=treeRef(character,selector);
  if(!tree) return null;
  return {careerKey:career,specializationKey:spec,nodes:tree.nodes.map(n=>clone(n)),edges:tree.edges.map(e=>[...e])};
}

export function eligibleTalents(character,selector={}){
  const {tree}=treeRef(character,selector);
  if(!tree) return [];
  const owned=ownedIds(character);
  const unlocked=unlockedIds(character,tree);
  const xp=Number(character?.xp?.available??0);
  return tree.nodes.map(n=>({
    ...clone(n),
    purchased:owned.has(n.id),
    unlocked:unlocked.has(n.id),
    ...(character?.creation?.rulesProfile === 'AOR_CORE' ? {purchaseActive:true,mechanicallyActive:AOR_RUNTIME_MECHANICAL_TALENTS.includes(n.originalName)} : {}),
    available:!owned.has(n.id)&&unlocked.has(n.id)&&xp>=n.cost,
    needsChoice:['DEDICACIÓN','DEDI','DEADACC','KNOWSPEC','SMOOTHTALK','WELLROUND'].includes(character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName),
    choiceType:['DEDICACIÓN','DEDI'].includes(character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName)?'characteristic':((character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName)==='DEADACC'?'combatSkill':((character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName)==='KNOWSPEC'?'knowledgeSkill':((character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName)==='SMOOTHTALK'?'socialSkill':((character?.creation?.rulesProfile === 'AOR_CORE' ? aorCanonicalTalentName(n.originalName) : n.originalName)==='WELLROUND'?'twoSkills':null))))
  }));
}

export function purchaseTalent(character,nodeId,{characteristicKey=null,skillKey=null,skillKeys=null,careerKey=null,specializationKey=null}={}){
  if(!character||typeof character!=='object') throw new Error('Ficha inválida.');
  const ref=treeRef(character,{careerKey,specializationKey,nodeId});
  if(!ref.tree) throw new Error('Talento não pertence a uma especialização possuída por esta ficha.');
  const node=eligibleTalents(character,{careerKey:ref.career,specializationKey:ref.spec}).find(n=>n.id===nodeId);
  if(!node) throw new Error('Talento não pertence a uma especialização possuída por esta ficha.');
  if(node.purchased) throw new Error('Este espaço da árvore já foi adquirido.');
  if(node.forceOnly&&!isForceSensitive(character)) throw new Error('Este talento exige Force Rating 1 ou maior.');
  if(!node.unlocked) throw new Error('Este talento ainda não está conectado a um talento adquirido.');
  if(Number(character.xp?.available??0)<node.cost) throw new Error(`PE insuficientes: são necessários ${node.cost} PE.`);

  const c=clone(character);
  const effect=applyImmediateEffect(c,node,{characteristicKey,skillKey,skillKeys});
  c.xp.available-=node.cost;
  c.talentPurchases=[...purchases(c),{
    nodeId:node.id,name:node.name,originalName:node.originalName,cost:node.cost,row:node.row,column:node.column,
    source:node.source,careerKey:ref.career,specializationKey:ref.spec,effect:effect.label??null,choice:effect.skillKeys??effect.characteristicKey??effect.skillKey??null
  }];
  c.talents=Array.isArray(c.talents)?c.talents:[];
  c.talents.push(node.name);
  return {character:c,talent:node,effect};
}

// Compatibilidade com R11 e chamadas já existentes.
export function eligibleFirstRowTalents(character){ return eligibleTalents(character).filter(n=>n.row===1); }
export function purchaseFirstRowTalent(character,nodeId,options={}){
  const node=eligibleTalents(character).find(n=>n.id===nodeId);
  if(!node||node.row!==1) throw new Error('Talento não pertence à primeira linha da especialização inicial desta ficha.');
  return purchaseTalent(character,nodeId,options);
}

export function talentRanks(character){
  const out={};
  for(const t of (character?.talents??[])){
    const name=typeof t==='string'?t:(t?.name??'');
    if(name) out[name]=(out[name]??0)+1;
  }
  return out;
}

export function talentRankByOriginal(character,originalName){
  // AoR's translated labels may differ from EotE for the same mechanical
  // talent. Purchased slots retain the original source key and must not be
  // counted twice through the denormalized character.talents display list.
  if (character?.creation?.rulesProfile === 'AOR_CORE') {
    const ownedPurchases=purchases(character);
    const bought=ownedPurchases.filter(p=>p.originalName===originalName || aorCanonicalTalentName(p.originalName)===originalName).length;
    const names=new Set([PT_BY_ORIGINAL.get(originalName)??originalName,
      ...allNodes.filter(n=>n.originalName===originalName || aorCanonicalTalentName(n.originalName)===originalName).map(n=>n.name)]);
    const displayed=(character.talents??[]).filter(t=>typeof t==='string' && names.has(t)).length;
    const purchaseDisplays=ownedPurchases.filter(p=>names.has(p.name)).length;
    // Species-granted talents remain in the display list and can stack with
    // bought slots of the same name. Subtract only the purchase display copies.
    return bought+Math.max(0,displayed-purchaseDisplays);
  }
  const pt=PT_BY_ORIGINAL.get(originalName)??originalName;
  return Number(talentRanks(character)[pt]??0);
}



/** P160: AoR's new effects must come from actual purchased tree nodes.
 * A translated display label cannot activate a Destiny, ammo or piloting talent.
 * EotE characters retain their established legacy talent lookup.
 */
const AOR_SOURCE_NODES_BY_ID=new Map(Object.values(AOR_TALENT_TREES)
  .flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes))
  .map(node=>[node.id,node]));
export function sourceBackedTalentRank(character, canonical) {
  if(character?.creation?.rulesProfile!=='AOR_CORE')return talentRankByOriginal(character,canonical);
  return new Set(purchases(character).filter(p=>{
    const node=AOR_SOURCE_NODES_BY_ID.get(p.nodeId);
    return node && node.originalName===p.originalName && aorCanonicalTalentName(node.originalName)===canonical;
  }).map(p=>p.nodeId)).size;
}


/** P170: the ranked Hard Headed check is an ACTION even while staggered.
 * The improved variant alone may be attempted after strain incapacitation.
 * A single purchased rank uses Daunting (4); extra ranks lower it by one.
 * Improved uses Formidable (5), lowered by additional ranks after first.
 */
export function hardHeadedOption(character,{improved=false}={}) {
  const rank=sourceBackedTalentRank(character,'HARDHD');
  const improvedOwned=sourceBackedTalentRank(character,'HARDHDIMP')>0;
  return {available:rank>0&&(!improved||improvedOwned),rank,improved:Boolean(improved),
    difficulty:Math.max(1,(improved?5:4)-Math.max(0,rank-1))};
}

/** P170: a Body Guard maneuver protects exactly one engaged ally. */
export function bodyGuardOption(character,requestedStrain=0){
  const rank=sourceBackedTalentRank(character,'BOD');
  if(!Number.isSafeInteger(requestedStrain)||requestedStrain<0||requestedStrain>rank)
    throw new Error('Tensão de Guarda-costas excede graduações adquiridas.');
  return {available:rank>0,rank,strainCost:requestedStrain,difficultyUpgrades:requestedStrain};
}

/** P170: Jury Rigged is bound to one owned *instance* per purchased slot.
 * It cannot be granted by translated labels, duplicated slot IDs or by a
 * stale assignment after its source purchase is removed. */
const EOTE_SOURCE_NODES_BY_ID=new Map(eoteNodes.map(node=>[node.id,node]));
export function juryRiggedPurchaseNodes(character) {
  const aor=character?.creation?.rulesProfile==='AOR_CORE';
  const index=aor?AOR_SOURCE_NODES_BY_ID:EOTE_SOURCE_NODES_BY_ID;
  const seen=new Set();
  return purchases(character).filter(p=>{
    const node=index.get(p.nodeId);
    if(!node||seen.has(node.id)||node.originalName!==p.originalName||
       (aor?aorCanonicalTalentName(node.originalName):node.originalName)!=='JURY')return false;
    seen.add(node.id);return true;
  }).map(p=>p.nodeId);
}
export function juryRiggedWeapon(character,weapon) {
  const next=clone(weapon),assigned=character?.talentAssignments?.juryRigged;
  if(!Array.isArray(assigned)||!next?.juryRiggedInstanceId)return {weapon:next,applied:null};
  // An imported save cannot copy an authorized instance ID onto a second
  // weapon and thereby multiply one ranked benefit across the inventory.
  const matches=(character?.weapons??[]).filter(w=>w?.juryRiggedInstanceId===next.juryRiggedInstanceId);
  if(matches.length!==1)return {weapon:next,applied:null};
  const valid=new Set(juryRiggedPurchaseNodes(character));
  const row=assigned.find(a=>a?.instanceId===next.juryRiggedInstanceId&&valid.has(a.nodeId));
  if(!row)return {weapon:next,applied:null};
  if(row.effect==='damage'&&Number.isSafeInteger(next.damage)&&next.damage>=0){next.damage+=1;return {weapon:next,applied:'damage'};}
  if(row.effect==='crit'&&Number.isSafeInteger(next.crit)&&next.crit>0){next.crit=Math.max(1,next.crit-1);return {weapon:next,applied:'crit'};}
  return {weapon:next,applied:null};
}

/** P161 — only purchased, source-backed Knowledge Specialization slots count.
 * Choice is made at purchase and is not inferred from translated labels. */
export function knowledgeSpecializationRank(character, skillKey) {
  if(!KNOWLEDGE_SKILLS.includes(skillKey)) return 0;
  if(character?.creation?.rulesProfile!=='AOR_CORE') return 0;
  const seen=new Set();
  for(const p of purchases(character)) {
    const node=AOR_SOURCE_NODES_BY_ID.get(p.nodeId);
    if(node?.originalName==='CONOCIMIENTO ESPECIALIZADO' && p.originalName===node.originalName && p.choice===skillKey)seen.add(node.id);
  }
  return seen.size;
}

/** Bind a legacy/purchased Knowledge Specialization node once, without XP cost.
 * This preserves old campaigns whose talent paths were bought before P161. */
export function bindKnowledgeSpecialization(character,{nodeId,skillKey}={}) {
  if(!KNOWLEDGE_SKILLS.includes(skillKey))throw new Error('Perícia de Conhecimento inválida.');
  const next=clone(character),purchase=purchases(next).find(p=>p.nodeId===nodeId);
  const node=AOR_SOURCE_NODES_BY_ID.get(nodeId);
  if(!purchase||node?.originalName!=='CONOCIMIENTO ESPECIALIZADO'||purchase.originalName!==node.originalName)
    throw new Error('Nó de Conhecimento especializado não foi adquirido.');
  if(purchase.choice!==null&&purchase.choice!==undefined)throw new Error('Conhecimento especializado já tem perícia vinculada.');
  purchase.choice=skillKey;purchase.effect=`Conhecimento especializado: ${skillKey}`;
  return {character:next,nodeId,skillKey};
}

/** P162: Lábia (Smooth Talker) is selected per purchased tree node.
 * Only source-authentic AoR purchases count, never translated display labels. */
export const SMOOTH_TALKER_SKILLS=Object.freeze(['carisma','coaccion','engano','negociacion']);
export function smoothTalkerRank(character,skillKey){
  if(character?.creation?.rulesProfile!=='AOR_CORE'||!SMOOTH_TALKER_SKILLS.includes(skillKey))return 0;
  const seen=new Set();
  for(const p of purchases(character)){
    const node=AOR_SOURCE_NODES_BY_ID.get(p.nodeId);
    if(node?.originalName==='LABIA'&&p.originalName===node.originalName&&p.choice===skillKey)seen.add(node.id);
  }
  return seen.size;
}
export function bindSmoothTalker(character,{nodeId,skillKey}={}){
  if(!SMOOTH_TALKER_SKILLS.includes(skillKey))throw new Error('Perícia social de Lábia inválida.');
  const next=clone(character),purchase=purchases(next).find(p=>p.nodeId===nodeId);
  const node=AOR_SOURCE_NODES_BY_ID.get(nodeId);
  if(!purchase||node?.originalName!=='LABIA'||purchase.originalName!==node.originalName)
    throw new Error('Nó de Lábia não foi adquirido.');
  if(purchase.choice!==null&&purchase.choice!==undefined)throw new Error('Lábia já tem perícia vinculada.');
  purchase.choice=skillKey;purchase.effect=`Lábia: ${skillKey}`;
  return {character:next,nodeId,skillKey};
}

/** P155 — Utility Belt. This talent grants an activated inventory action,
 * not a passive item or extra equipment at character creation. */
export function utilityBeltTalentStatus(character){
 const rank=character?.creation?.rulesProfile==='AOR_CORE' ? purchases(character).filter(p=>p.originalName==='CINTURÓN DE ACCESORIOS' && allNodes.some(n=>n.id===p.nodeId&&n.originalName===p.originalName)).length : 0;
 return {available:rank>0,rank,destinyCost:1,maxRarity:4,maxEncumbrance:1};
}

/** P141 — Tinkerer / Mañoso. Each rank authorizes one different owned item
 * to receive +1 hard point. The bonus is item-local and survives JSON saves;
 * catalog definitions and unmodified gear are never mutated.
 */
const TINKERER_ITEM_KINDS = Object.freeze(['weapons','armor','gear']);
export function tinkererStatus(character) {
  const rank = talentRankByOriginal(character, 'TINK');
  const items = TINKERER_ITEM_KINDS.flatMap(kind =>
    (Array.isArray(character?.[kind]) ? character[kind] : []).map((item, index) => ({
      kind, index, id: item?.ownedAssetId ?? item?.id ?? item?.sourceCatalogId ?? null,
      name: item?.name ?? null, enhanced: item?.tinkererHardPointBonus === 1
    })));
  const applied = items.filter(item => item.enhanced).length;
  const invalidBonus = TINKERER_ITEM_KINDS.some(kind =>
    (Array.isArray(character?.[kind]) ? character[kind] : []).some(item =>
      item && item.tinkererHardPointBonus !== undefined &&
      item.tinkererHardPointBonus !== 0 && item.tinkererHardPointBonus !== 1));
  if (invalidBonus || applied > rank)
    throw new Error('Inventário de Mañoso/Ajustador incompatível com talentos adquiridos.');
  return {rank, applied, available: rank-applied, items};
}
export function applyTinkererHardPoint(character, {kind='weapons', itemIndex}={}) {
  if (!character || typeof character !== 'object') throw new Error('Ficha inválida.');
  if (!TINKERER_ITEM_KINDS.includes(kind) || !Number.isSafeInteger(itemIndex) || itemIndex < 0)
    throw new Error('Item para Mañoso inválido.');
  const status = tinkererStatus(character);
  if (status.rank < 1) throw new Error('Personagem não possui Mañoso/Ajustador.');
  if (status.available < 1) throw new Error('Todos os usos de Mañoso/Ajustador já foram alocados.');
  const current = character[kind]?.[itemIndex];
  if (!current || typeof current !== 'object' || Array.isArray(current))
    throw new Error('Item para Mañoso inexistente no inventário.');
  if (current.tinkererHardPointBonus !== undefined && current.tinkererHardPointBonus !== 0)
    throw new Error('Este item já recebeu Mañoso/Ajustador.');
  const updated = clone(character);
  updated[kind][itemIndex].tinkererHardPointBonus = 1;
  updated[kind][itemIndex].tinkererAppliedAt = new Date().toISOString();
  return {character:updated, item:clone(updated[kind][itemIndex]), status:tinkererStatus(updated)};
}

export const KNOWLEDGE_SKILLS = Object.freeze([
  'bajos_fondos','borde_exterior','educacion','estrategia_militar',
  'mundos_del_nucleo','saber_arcaico','xenologia'
]);

/** P138: context-gated task duration for Researcher, Expert Tracker and
 * Technical Aptitude.  Duration is returned to the caller, not silently
 * deducted from the world clock; the host must apply the elapsed time once.
 */
export function talentTaskTimeSpec(character, skillKey, { tracking = false, research = false, outdoors = false, searchingArea = false, taskDurationMinutes = null } = {}) {
  if (taskDurationMinutes !== null && (!Number.isFinite(taskDurationMinutes) || taskDurationMinutes < 0))
    throw new Error('Duração de tarefa inválida.');
  const details = [];
  let multiplier = 1;
  if (tracking && skillKey === 'supervivencia') {
    const rank = talentRankByOriginal(character, 'EXPTRACK');
    if (rank > 0) { multiplier *= 0.5; details.push({ originalName: 'EXPTRACK', rank, multiplier: 0.5 }); }
  }
  if (research && KNOWLEDGE_SKILLS.includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'RESEARCHER');
    if (rank > 0) { multiplier *= 0.5; details.push({ originalName: 'RESEARCHER', rank, multiplier: 0.5 }); }
  }
  if (outdoors && ['supervivencia','atletismo','coordinacion'].includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'OUTDOORSMAN');
    if (rank > 0) { multiplier *= 0.5; details.push({ originalName:'OUTDOORSMAN',rank,multiplier:0.5 }); }
  }
  if (searchingArea && ['percepcion','alerta'].includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'KEENEYED');
    if (rank > 0) { multiplier *= 0.5; details.push({ originalName:'KEENEYED',rank,multiplier:0.5 }); }
  }
  if (skillKey === 'computadoras') {
    const rank = talentRankByOriginal(character, 'TECHAPT');
    if (rank > 0) { const factor = Math.max(0, 1 - 0.25 * rank); multiplier *= factor;
      details.push({ originalName: 'TECHAPT', rank, multiplier: factor }); }
  }
  return { multiplier, durationMinutes: taskDurationMinutes === null ? null : taskDurationMinutes * multiplier, details };
}

const SKILL_BOOST_TALENTS=Object.freeze([
  Object.freeze({originalName:'STALK',skills:Object.freeze(['sigilo','coordinacion']),label:'Perseguidor'}),
  // AoR Physical Training: one boost per rank on Athletics and Resilience.
  Object.freeze({originalName:'PHYTRAIN',skills:Object.freeze(['atletismo','aguante']),label:'Treinamento físico'}),
  Object.freeze({originalName:'COM',skills:Object.freeze(['liderazgo']),label:'Comando'}),
  Object.freeze({originalName:'UNCSEN',skills:Object.freeze(['percepcao']),label:'Sentidos sobrenaturais'}),
  Object.freeze({originalName:'UNCREA',skills:Object.freeze(['alerta']),label:'Reações sobrenaturais'}),
  Object.freeze({originalName:'SENSEEMO',skills:Object.freeze(['carisma','coaccion','engano']),label:'Sentir emoções'})
]);

const SKILL_SETBACK_REMOVAL_TALENTS=Object.freeze([
  Object.freeze({originalName:'KILL',skills:Object.freeze(['carisma','liderazgo']),label:'Don de gentes / Matar com gentileza'}),
  Object.freeze({originalName:'PLAUSDEN',skills:Object.freeze(['coaccion','engano']),label:'Negação plausível'}),
  Object.freeze({originalName:'STRSMART',skills:Object.freeze(['callejeo','bajos_fondos']),label:'Instinto das ruas'}),
  Object.freeze({originalName:'CONV',skills:Object.freeze(['engano','actividad_criminal']),label:'Atitude convincente'}),
  Object.freeze({originalName:'GALMAP',skills:Object.freeze(['astronavegacion']),label:'Cartógrafo galáctico'}),
  Object.freeze({originalName:'GEARHD',skills:Object.freeze(['mecanica']),label:'Cabeça de engrenagem'}),
  Object.freeze({originalName:'SKILLJOCK',skills:Object.freeze(['pilotar_vehiculo_planetario','pilotar_vehiculo_espacial']),label:'Piloto consumado'}),
  Object.freeze({originalName:'COMMANDPRES',skills:Object.freeze(['liderazgo','frialdad']),label:'Dotes de comando'}),
  Object.freeze({originalName:'KEENEYED',skills:Object.freeze(['percepcion','alerta']),label:'Observador'})
]);

// AoR/EotE core talents consumed by the existing combat, fear, critical,
// repair and health pipelines. Ranked values are computed from purchased
// source slots, never from UI labels alone.
export function durableCriticalModifier(character){
  return -10 * Math.max(0,talentRankByOriginal(character,'DURA'));
}
export function confidenceFearDifficultyReduction(character,skillKey='disciplina'){
  return skillKey==='disciplina'?Math.max(0,talentRankByOriginal(character,'CONF')):0;
}
export function trueAimOption(character){
  const rank=Math.max(0,talentRankByOriginal(character,'TRUEAIM'));
  return {available:rank>0,rank,abilityUpgrades:rank,boost:1,oncePerRound:true};
}
export function stimpackSpecializationBonus(character){
  return Math.max(0,talentRankByOriginal(character,'STIMSPEC'));
}
/** P166: Gearhead/MANITAS is a ranked Mechanics setback removal.
 * Its attachment modification price reduction is a single half-price benefit,
 * not a multiplicative discount for two purchased slots. */
export function gearheadModPricing(character, baseCost=100) {
  if (!Number.isSafeInteger(baseCost) || baseCost < 0 || baseCost > 100000000)
    throw new Error('Custo de Mod inválido.');
  const rank = Math.max(0, sourceBackedTalentRank(character, 'GEARHD'));
  return { rank, baseCost, cost: rank > 0 ? Math.ceil(baseCost / 2) : baseCost,
    discount: rank > 0 ? baseCost - Math.ceil(baseCost / 2) : 0 };
}

export function talentSkillPoolModifiers(character,skillKey,{boost=0,setback=0,securityBypass=false,tracking=false,codebreaking=false,outdoors=false,chase=false,crafting=false,foraging=false,poisonResistance=false,terrainSetback=0}={}){
  if(typeof foraging!=='boolean'||typeof poisonResistance!=='boolean')throw new Error('Contexto de sobrevivência/toxinas deve ser booleano.');
  const baseBoost=Math.max(0,Number(boost)||0);
  const terrain=Number(terrainSetback);
  if(!Number.isSafeInteger(terrain)||terrain<0||terrain>20) throw new Error('Penalidade de terreno inválida.');
  const planetary=skillKey==='pilotar_vehiculo_planetario';
  const allTerrain=planetary&&terrain>0&&talentRankByOriginal(character,'ALLTERR')>0;
  const baseSetback=Math.max(0,Number(setback)||0)+(allTerrain?0:terrain);
  let talentBoost=0;
  let removalCapacity=0;
  const details=[];
  if(allTerrain) details.push({talent:'Condutor todo-terreno',originalName:'ALLTERR',effect:'ignore_terrain_setback',amount:terrain});
  if(chase&&['pilotar_vehiculo_planetario','pilotar_vehiculo_espacial','atletismo','coordinacion'].includes(skillKey)){
    const rank=talentRankByOriginal(character,'SHORTCUT');
    if(rank>0){talentBoost+=rank;details.push({talent:'Atalho',originalName:'SHORTCUT',effect:'boost',amount:rank});}
  }
  if(foraging&&skillKey==='supervivencia'){
    // The printed talent removes up to two setback dice when looking for
    // food, water or shelter. A second purchased slot does not double the cap.
    const rank=talentRankByOriginal(character,'ACCLIM');
    if(rank>0){removalCapacity+=2;details.push({talent:'Aclimatação',originalName:'ACCLIM',effect:'remove_setback',amount:2});}
  }
  if(poisonResistance&&skillKey==='aguante'){
    const rank=talentRankByOriginal(character,'POISONRES');
    if(rank>0){talentBoost+=rank;details.push({talent:'Resistência a venenos',originalName:'POISONRES',effect:'boost',amount:rank});}
  }
  if(crafting&&skillKey==='mecanica'){
    const rank=talentRankByOriginal(character,'INVENTOR');
    if(rank>0){talentBoost+=rank;details.push({talent:'Inventor',originalName:'INVENTOR',effect:'boost',amount:rank});}
  }
  // Bypass Security applies only to bypassing locks/security systems, not
  // every Computers or Skulduggery roll. The caller must opt in explicitly.
  if(securityBypass && ['computadoras','actividad_criminal'].includes(skillKey)){
    const ranks=Math.max(0,talentRankByOriginal(character,'BYP'));
    removalCapacity+=ranks;
    if(ranks) details.push({talent:'Burlar segurança',originalName:'BYP',effect:'remove_setback',amount:ranks});
  }
  // Researcher always helps knowledge tests. Expert Tracker is restricted
  // to an explicitly declared tracking test; generic Survival is unchanged.
  if (KNOWLEDGE_SKILLS.includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'RESEARCHER');
    if (rank > 0) { removalCapacity += rank; details.push({ talent:'Investigador',originalName:'RESEARCHER',effect:'remove_setback',amount:rank }); }
  }
  if (tracking && skillKey === 'supervivencia') {
    const rank = talentRankByOriginal(character, 'EXPTRACK');
    if (rank > 0) { removalCapacity += rank; details.push({ talent:'Rastreador especialista',originalName:'EXPTRACK',effect:'remove_setback',amount:rank }); }
  }
  if (codebreaking && ['computadoras','educacion'].includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'CODEBREAKER');
    if (rank > 0) { removalCapacity += rank; details.push({talent:'Decifrador',originalName:'CODEBREAKER',effect:'remove_setback',amount:rank}); }
  }
  if (outdoors && ['supervivencia','atletismo','coordinacion','aguante'].includes(skillKey)) {
    const rank = talentRankByOriginal(character, 'OUTDOORSMAN');
    if (rank > 0) { removalCapacity += rank; details.push({talent:'Mateiro',originalName:'OUTDOORSMAN',effect:'remove_setback',amount:rank}); }
  }
  for(const rule of SKILL_BOOST_TALENTS){
    if(!rule.skills.includes(skillKey)) continue;
    const rank=talentRankByOriginal(character,rule.originalName);
    if(rank>0){ talentBoost+=rank; details.push({talent:rule.label,originalName:rule.originalName,effect:'boost',amount:rank}); }
  }
  for(const rule of SKILL_SETBACK_REMOVAL_TALENTS){
    if(!rule.skills.includes(skillKey)) continue;
    const rank=['SKILLJOCK','GEARHD'].includes(rule.originalName) ? sourceBackedTalentRank(character,rule.originalName) : talentRankByOriginal(character,rule.originalName);
    if(rank>0){ removalCapacity+=rank; details.push({talent:rule.label,originalName:rule.originalName,effect:'remove_setback',amount:rank}); }
  }
  const setbackRemoved=Math.min(baseSetback,removalCapacity);
  return {
    boost:baseBoost+talentBoost,
    setback:Math.max(0,baseSetback-removalCapacity),
    baseBoost,baseSetback,talentBoost,setbackRemoved,removalCapacity,details
  };
}

/** Codebreaker reduces the difficulty only for explicitly declared code-breaking
 * or decryption work, never for arbitrary Computers/Education checks. */
/** AoR Nobody's Fool: only incoming Charm, Coercion and Deception rolls
 * targeting the talent owner receive negative-dice upgrades. Not a bonus to
 * outgoing social checks or other social skills. */
export function nobodyFoolDefense(character, incomingSkillKey) {
  const rank = ['carisma', 'coaccion', 'engano'].includes(incomingSkillKey)
    ? Math.max(0, talentRankByOriginal(character, 'NOBODYSFOOL')) : 0;
  return {available:rank>0, rank, difficultyUpgrades:rank, skillKey:incomingSkillKey};
}

/** P157 — AoR Diplomat/Commander contextual defenses and economy. All ranks
 * derive from purchased source slots; none is granted by a display-only label. */
export function indistinguishableIdentificationDefense(character,{identifyingCharacter=false}={}){
  const rank=identifyingCharacter ? Math.max(0,talentRankByOriginal(character,'INDISTINGUISHABLE')) : 0;
  return {rank,available:rank>0,difficultyUpgrades:rank,identifyingCharacter:Boolean(identifyingCharacter)};
}
export function wheelAndDealLegalSale(character,{legalSale=false,restricted=false,basePrice=0}={}){
  if(!Number.isSafeInteger(basePrice)||basePrice<0)throw new Error('Preço-base de venda inválido.');
  const rank=legalSale&&!restricted ? Math.max(0,talentRankByOriginal(character,'WHEELDEAL')) : 0;
  const bonus=Math.floor(basePrice*rank/10);
  if(!Number.isSafeInteger(basePrice+bonus)||basePrice+bonus>100000000)throw new Error('Valor de venda com talento excede o limite econômico.');
  return {rank,legalSale:Boolean(legalSale),restricted:Boolean(restricted),basePrice,bonus,finalPrice:basePrice+bonus};
}
/** P172 — Master Merchant (AoR Quartermaster): optional pre-check 2 strain.
 * This quote is not a roll and grants no bonus unless the GM confirms the
 * Negotiation check succeeded. The caller must commit strain and credits in
 * one transaction. Display names cannot forge a purchased source node. */
export function masterMerchantOption(character,{activated=false,checkSucceeded=false,kind='sell',basePrice=0,strainAlreadyPaid=false}={}){
  if(typeof activated!=='boolean'||typeof checkSucceeded!=='boolean'||typeof strainAlreadyPaid!=='boolean'||(strainAlreadyPaid&&!activated)||!['sell','buy'].includes(kind)||
      !Number.isSafeInteger(basePrice)||basePrice<0||basePrice>100000000)
    throw new Error('Opção de Mercador especialista inválida.');
  const rank=sourceBackedTalentRank(character,'MASTMERCH');
  if(activated&&rank<1)throw new Error('Mercador especialista não adquirido na árvore AoR.');
  const strainCost=activated?2:0;
  const strain=character?.derived?.strain;
  if(activated&&(!Number.isSafeInteger(strain?.current)||!Number.isSafeInteger(strain?.threshold)||
      strain.current+(strainAlreadyPaid?0:strainCost)>strain.threshold))
    throw new Error('Tensão insuficiente para ativar Mercador especialista.');
  const bonus=activated&&checkSucceeded?Math.floor(basePrice/4):0;
  const finalPrice=kind==='sell'?basePrice+bonus:basePrice-bonus;
  if(!Number.isSafeInteger(finalPrice)||finalPrice>100000000)
    throw new Error('Preço de Mercador especialista excede limite econômico.');
  return {available:rank>0,activated,checkSucceeded,kind,strainCost,strainAlreadyPaid,bonus,basePrice,finalPrice};
}

/** P158 — Context-bound AoR explosive talents; purchased slots only.
 * Selective Detonation excludes at most one target per spent Advantage, or
 * up to purchased ranks for one spent Triumph, from the blast affected set.
 */
export function selectiveDetonationOption(character,{excluded=0,useTriumph=false}={}){
  const rank=Math.max(0,talentRankByOriginal(character,'SELECTDET'));
  if(!Number.isSafeInteger(excluded)||excluded<0)throw new Error('Quantidade de alvos excluídos inválida.');
  if(excluded>rank)throw new Error('Detonação seletiva excede as graduações adquiridas.');
  if(useTriumph!==true&&useTriumph!==false)throw new Error('Uso de Triunfo deve ser booleano.');
  return {rank,excluded,advantageCost:useTriumph?0:excluded,triumphCost:useTriumph&&excluded>0?1:0};
}
export function masterGrenadierBlastCost(character,baseCost){
  if(!Number.isSafeInteger(baseCost)||baseCost<1)throw new Error('Custo de Explosão inválido.');
  const available=talentRankByOriginal(character,'MASTERGREN')>0;
  return {available,baseCost,cost:Math.max(1,baseCost-(available?1:0))};
}
export function feintTalentOption(character,requestedStrain=0){
  const rank=Math.max(0,talentRankByOriginal(character,'FEINT'));
  if(!Number.isSafeInteger(requestedStrain)||requestedStrain<0)throw new Error('Tensão de Finta inválida.');
  if(requestedStrain>rank)throw new Error(`Finta permite no máximo ${rank} de Tensão.`);
  return {rank,available:rank>0,spend:requestedStrain,difficultyUpgrades:requestedStrain,onlyRanged:true};
}

/** AoR/EotE Disorient: spend two Advantage on a successful hit to impose
 * Disoriented for one round per purchased rank. The host owns the pending
 * attack receipt and must subtract Advantage exactly once. */
/** P154 — AoR source-backed combat talents. These do not alter printed
 * weapon qualities or grant an effect to characters without purchased slots.
 * The controller consumes the SAME pending attack ledger as weapon qualities.
 */
export function powerfulBlastDamageBonus(character, {weapon=null}={}) {
  if (!weapon) return 0;
  // Caller supplies an actually activated Blast quality; no global damage buff.
  return talentRankByOriginal(character,'POWERBLAST');
}
export function strongArmRangeBonus(character, weapon) {
  const rank=talentRankByOriginal(character,'STRONGARM');
  const id=String(weapon?.sourceCatalogId??weapon?.id??weapon?.key??'').toLowerCase();
  const isGrenade=['stun_grenade','frag_grenade','concussion_grenade','thermal_detonator'].includes(id)
    || /\bgranada\b/i.test(String(weapon?.name??''));
  const thrown=weapon?.thrown===true || weapon?.throwable===true || isGrenade;
  return rank>0 && thrown && weapon?.skill==='armas_distancia_ligeras' ? 1 : 0;
}
export function knockdownTalentOption(character, {hit=false, skillKey=null, triumph=0}={}) {
  const rank=talentRankByOriginal(character,'KNOCKDOWN_TALENT');
  if(!Number.isSafeInteger(triumph)||triumph<0) throw new Error('Triunfos de Derrubada inválidos.');
  const melee=['pelea','armas_cuerpo_a_cuerpo'].includes(skillKey);
  return {rank,available:rank>0&&Boolean(hit)&&melee&&triumph>=1,
    hit:Boolean(hit),melee,costTriumph:1,remainingTriumph:triumph};
}

export function disorientTalentOption(character, {hit=false, advantage=0}={}) {
  const rank = Math.max(0, talentRankByOriginal(character, 'DISOR'));
  const points = Number(advantage);
  if (!Number.isSafeInteger(points) || points < 0)
    throw new Error('Vantagens para Desorientar inválidas.');
  return {available:rank>0 && Boolean(hit) && points>=2, rank, cost:2,
    rounds:rank, hit:Boolean(hit), remainingAdvantage:points};
}

/** Only an explicit interaction with a recognized learning institution qualifies. */
export function respectedScholarDowngrades(character,skillKey,{scholarlyInstitution=false}={}){
  return scholarlyInstitution&&['carisma','negociacion','liderazgo','engano','coaccion'].includes(skillKey)
    ? talentRankByOriginal(character,'RESSCH') : 0;
}

/** AoR Speaks Binary modifies a directed NPC droid's pool, not the player's. */
export function speaksBinaryBoost(character,{directingDroid=false}={}){
  return directingDroid ? talentRankByOriginal(character,'SPEAKBIN') : 0;
}

export function codebreakerDifficultyReduction(character,skillKey,{codebreaking=false}={}) {
  if (!codebreaking || !['computadoras','educacion'].includes(skillKey)) return 0;
  return talentRankByOriginal(character,'CODEBREAKER')>0 ? 1 : 0;
}

export function talentCloseRangeDamageBonus(character,{weapon=null,targetRange=null}={}){
  if(!weapon) return 0;
  const ranged=['armas_distancia_ligeras','armas_distancia_pesadas'];
  if(!ranged.includes(weapon.skill)) return 0;
  if(!['Curto','Interação'].includes(String(targetRange??''))) return 0;
  return talentRankByOriginal(character,'POINTBL');
}

export function talentVehicleDefenseBonus(character){
  return talentRankByOriginal(character,'DEFDRI');
}

export function quickReactionOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'RAPREA');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Reação rápida inválida.');
  if(spend>rank) throw new Error(`Reação rápida permite no máximo ${rank} de Tensão nesta iniciativa.`);
  return {rank,spend,boost:spend};
}

export function talentPilotedVehicleDefenseBonus(character){
  return talentRankByOriginal(character,'DEFDRI');
}

/** P150 — Defensive Slicing is only active when a protected computer is
 * actually being attacked. Improved Defensive Slicing REPLACES setbacks with
 * upgrades; never stack both. Neither bonus applies to an outgoing check. */
export function defensiveSlicingDefense(character, { defendingOwnedSystem = false } = {}) {
  const rank = talentRankByOriginal(character, 'DEFSLI');
  const improved = talentRankByOriginal(character, 'DEFSLIIMP') > 0;
  const applies = Boolean(defendingOwnedSystem) && rank > 0;
  return { rank, improved, applies,
    setback: applies && !improved ? rank : 0,
    difficultyUpgrades: applies && improved ? rank : 0 };
}

export function talentPilotedVehicleSilhouette(character, silhouette){
  const base=Math.max(0,Number(silhouette)||0);
  return talentRankByOriginal(character,'TRICK')>0 ? Math.max(0,base-1) : base;
}

export function fullThrottleTalentSpec(character){
  const base=talentRankByOriginal(character,'FULLTH');
  const improved=talentRankByOriginal(character,'FULLTHIMP')>0;
  const supreme=talentRankByOriginal(character,'FULLTHSUP')>0;
  return {available:base>0,improved,supreme,bonus:supreme?2:base>0?1:0,actionDifficulty:3,maneuverDifficulty:2,improvedStrainCost:improved?1:0};
}

export function galacticCartographerTimeMultiplier(character){
  return talentRankByOriginal(character,'GALMAP')>0 ? 0.5 : 1;
}

export function expertSkillTalentSpec(character,skillKey){
  const configs={
    astronavegacion:{originalName:'MASSTAR',label:'Navegador mestre'}
  };
  const config=configs[skillKey];
  if(!config) return {available:false,skillKey,originalName:null,label:null,strainCost:0,difficultyReduction:0,minDifficulty:1};
  const rank=talentRankByOriginal(character,config.originalName);
  return {available:rank>0,rank,skillKey,originalName:config.originalName,label:config.label,strainCost:2,difficultyReduction:1,minDifficulty:1};
}

export function quickStrikeBoost(character,{targetActorId=null,actedActorIds=[]}={}){
  const rank=talentRankByOriginal(character,'QUICKST');
  if(rank<1 || !targetActorId) return {rank,boost:0,applies:false};
  const acted=new Set(Array.isArray(actedActorIds)?actedActorIds:[]);
  const applies=!acted.has(String(targetActorId));
  return {rank,boost:applies?rank:0,applies,targetActorId:String(targetActorId)};
}


export function forceTalentSummary(character){
  return {
    forceRating:forceRating(character),
    availableForceRating:availableForceRating(character),
    forceSensitive:isForceSensitive(character),
    overwhelmEmotions:talentRankByOriginal(character,'OVEREMO')>0,
    intenseFocus:talentRankByOriginal(character,'INTFOC')>0,
    senseDanger:talentRankByOriginal(character,'SENSEDANGER')>0,
    balance:talentRankByOriginal(character,'BALANCE')>0,
    touchOfFate:talentRankByOriginal(character,'TOUCHFATE')>0,
    quickDraw:talentRankByOriginal(character,'QUICKDR')>0,
    sixthSense:talentRankByOriginal(character,'SIXTHSENSE')>0,
    superiorReflexes:talentRankByOriginal(character,'SUPREF')>0
  };
}

export function overwhelmEmotionsModifiers(character,skillKey,{light=0,dark=0}={}){
  if(talentRankByOriginal(character,'OVEREMO')<1) return {available:false,forceDice:0,success:0,threat:0};
  if(!['carisma','coaccion','engano'].includes(skillKey)) return {available:false,forceDice:0,success:0,threat:0};
  const l=Math.max(0,Number(light)||0),d=Math.max(0,Number(dark)||0);
  if(skillKey==='carisma') return {available:true,forceDice:forceRating(character),success:l,threat:Math.floor(d/2)};
  return {available:true,forceDice:forceRating(character),success:d,threat:Math.floor(l/2)};
}

export function intenseFocusOption(character){return {available:talentRankByOriginal(character,'INTFOC')>0,strainCost:1,abilityUpgrades:1};}
export function senseDangerOption(character){return {available:talentRankByOriginal(character,'SENSEDANGER')>0,setbackRemoved:2,usesPerSession:1};}
export function touchOfFateOption(character){return {available:talentRankByOriginal(character,'TOUCHFATE')>0,boost:2,usesPerSession:1};}
export function balanceRecovery(character,{rng=Math.random}={}){
  if(talentRankByOriginal(character,'BALANCE')<1) return {available:false,recovered:0,roll:null};
  const roll=rollForceDice(character,{rng});
  return {available:true,recovered:roll.light,roll};
}

// R54 P107 — EotE Core combat talents with deterministic runtime effects.
export function frenziedAttackOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'FRENZ');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Ataque Frenético inválida.');
  if(spend>rank) throw new Error(`Ataque Frenético permite no máximo ${rank} de Tensão por ataque.`);
  return {available:rank>0,rank,spend,abilityUpgrades:spend};
}

export function defensiveStanceOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'DEFSTA');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Postura Defensiva inválida.');
  if(spend>rank) throw new Error(`Postura Defensiva permite no máximo ${rank} de Tensão por ativação.`);
  return {available:rank>0,rank,spend,difficultyUpgrades:spend};
}

export function sideStepOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'SIDESTEP');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Passo Lateral inválida.');
  if(spend>rank) throw new Error(`Passo Lateral permite no máximo ${rank} de Tensão por ativação.`);
  return {available:rank>0,rank,spend,difficultyUpgrades:spend};
}

export function secondWindOption(character){
  const rank=talentRankByOriginal(character,'SECWIND');
  return {available:rank>0,rank,recover:rank,usesPerEncounter:1};
}

export function quickDrawOption(character){
  const rank=talentRankByOriginal(character,'QUICKDR');
  return {available:rank>0,rank,usesPerRound:1};
}

export function lethalBlowsCriticalModifier(character){
  const rank=talentRankByOriginal(character,'LETHALBL');
  return {rank,modifier:rank*10};
}

export function personalCombatTalentDamage(character,{weapon=null,targetRange=null}={}){
  if(!weapon) return {total:0,pointBlank:0,feralStrength:0,barrage:0,details:[]};
  const range=String(targetRange??'');
  const details=[];
  const pointBlank=talentCloseRangeDamageBonus(character,{weapon,targetRange});
  if(pointBlank>0) details.push({talent:'À queima-roupa',amount:pointBlank});
  const meleeSkills=new Set(['pelea','armas_cuerpo_a_cuerpo']);
  const feralStrength=meleeSkills.has(weapon.skill)?talentRankByOriginal(character,'FERSTR'):0;
  if(feralStrength>0) details.push({talent:'Força feroz',amount:feralStrength});
  const barrageSkills=new Set(['armas_distancia_pesadas','artilleria']);
  const barrage=(barrageSkills.has(weapon.skill)&&['Longo','Extremo'].includes(range))?talentRankByOriginal(character,'BAR'):0;
  if(barrage>0) details.push({talent:'Rajada',amount:barrage});
  return {total:pointBlank+feralStrength+barrage,pointBlank,feralStrength,barrage,details};
}


// R54 P108 — Assassin / combat talent block.
export function dodgeOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'DODGE');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Esquiva inválida.');
  if(spend>rank) throw new Error(`Esquiva permite no máximo ${rank} de Tensão por ataque recebido.`);
  return {available:rank>0,rank,spend,difficultyUpgrades:spend};
}

export function preciseAimOption(character, requestedStrain=0){
  const rank=talentRankByOriginal(character,'PRECAIM');
  const spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Mira Precisa inválida.');
  if(spend>rank) throw new Error(`Mira Precisa permite no máximo ${rank} de Tensão por ativação.`);
  return {available:rank>0,rank,spend,defenseReduction:spend};
}

export function sniperShotOption(character, requestedBands=0){
  const rank=talentRankByOriginal(character,'SNIPSHOT');
  const bands=Number(requestedBands);
  if(!Number.isInteger(bands)||bands<0) throw new Error('Bandas de Tiro de Precisão inválidas.');
  if(bands>rank) throw new Error(`Tiro de Precisão permite aumentar no máximo ${rank} banda(s) de alcance.`);
  return {available:rank>0,rank,bands,difficultyUpgrades:bands};
}

export function targetedBlowOption(character){
  return {available:talentRankByOriginal(character,'TARGBL')>0,characteristicKey:'agilidad'};
}
export function anatomyLessonsOption(character){
  return {available:talentRankByOriginal(character,'ANAT')>0,characteristicKey:'intelecto'};
}

export function deadlyAccuracySpec(character){
  const purchase=[...purchases(character)].reverse().find(p=>p?.originalName==='DEADACC' || (character?.creation?.rulesProfile==='AOR_CORE' && aorCanonicalTalentName(p?.originalName)==='DEADACC'));
  const skillKey=String(purchase?.choice??'');
  const combatSkills=new Set(['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','armas_distancia_pesadas','artilleria']);
  if(!purchase||!combatSkills.has(skillKey)) return {available:false,skillKey:null,bonus:0};
  const bonus=Math.max(0,Number(character?.skills?.[skillKey]??0));
  return {available:true,skillKey,bonus};
}


// R54 P109 — passive armor/strain talent helpers.
export function armorMasterBonuses(character,{armorSoak=0,worn=false}={}){
  if(!worn) return {armorMaster:false,improved:false,soakBonus:0,defenseBonus:0};
  const armorMaster=talentRankByOriginal(character,'ARM')>0;
  const improved=talentRankByOriginal(character,'ARMIMP')>0&&Number(armorSoak)>=2;
  return {armorMaster,improved,soakBonus:armorMaster?1:0,defenseBonus:improved?1:0};
}
export function resolveStrainReduction(character,amount,{voluntary=false}={}){
  const raw=Math.max(0,Number(amount)||0),rank=talentRankByOriginal(character,'RESOLVE');
  if(voluntary||raw<=0||rank<=0) return {rank,raw,effective:raw,reduced:0,voluntary:Boolean(voluntary)};
  const effective=Math.max(1,raw-rank);
  return {rank,raw,effective,reduced:raw-effective,voluntary:false};
}


// R54 P110 — active skill-check talents.
export function masterSkillDifficultyOption(character,kind){
  const configs={
    shadows:{originalName:'MASSHAD',skills:['sigilo','actividad_criminal'],label:'Mestre das sombras'},
    doctor:{originalName:'MASDOC',skills:['medicina'],label:'Médico mestre'},
    slicer:{originalName:'MASSLIC',skills:['computadoras'],label:'Slicer mestre'}
  };
  const cfg=configs[kind]; if(!cfg) return {available:false,kind,skills:[],label:null,strainCost:0,difficultyReduction:0};
  const available=sourceBackedTalentRank(character,cfg.originalName)>0;
  return {available,kind,originalName:cfg.originalName,skills:[...cfg.skills],label:cfg.label,strainCost:2,difficultyReduction:1,minDifficulty:1};
}
export function intimidatingOption(character,requestedStrain=0){
  const rank=talentRankByOriginal(character,'INTIM'),spend=Number(requestedStrain);
  if(!Number.isInteger(spend)||spend<0) throw new Error('Tensão de Intimidador inválida.');
  if(spend>rank) throw new Error(`Intimidador permite no máximo ${rank} de Tensão por teste.`);
  return {available:rank>0,rank,spend,downgrades:spend};
}

// R54 P111 — Natural reroll talents (EotE Core), once per game session.
const NATURAL_REROLL_RULES=Object.freeze([
  Object.freeze({originalName:'NATBRAW',label:'Brigão nato',skills:Object.freeze(['pelea','armas_cuerpo_a_cuerpo'])}),
  Object.freeze({originalName:'NATCHARM',label:'Encantador nato',skills:Object.freeze(['carisma','engano'])}),
  Object.freeze({originalName:'NATLEAD',label:'Líder nato',skills:Object.freeze(['liderazgo','frialdad'])}),
  Object.freeze({originalName:'NATDOC',label:'Médico nato',skills:Object.freeze(['medicina'])}),
  Object.freeze({originalName:'NATENF',label:'Impositor nato',skills:Object.freeze(['coaccion','callejeo'])}),
  Object.freeze({originalName:'NATHUN',label:'Caçador nato',skills:Object.freeze(['percepcion','alerta'])}),
  Object.freeze({originalName:'NATMAR',label:'Atirador nato',skills:Object.freeze(['armas_distancia_ligeras','armas_distancia_pesadas'])}),
  Object.freeze({originalName:'NATNEG',label:'Negociador nato',skills:Object.freeze(['frialdad','negociacion'])}),
  Object.freeze({originalName:'NATOUT',label:'Sobrevivente nato',skills:Object.freeze(['aguante','supervivencia'])}),
  Object.freeze({originalName:'NATPIL',label:'Piloto nato',skills:Object.freeze(['pilotar_vehiculo_espacial','artilleria'])}),
  Object.freeze({originalName:'NATDRI',label:'Condutor nato',skills:Object.freeze(['pilotar_vehiculo_planetario','artilleria'])}),
  Object.freeze({originalName:'NATPRO',label:'Programador nato',skills:Object.freeze(['computadoras','astronavegacion'])}),
  Object.freeze({originalName:'NATROG',label:'Trapaceiro nato',skills:Object.freeze(['actividad_criminal','sigilo'])}),
  Object.freeze({originalName:'NATSCH',label:'Acadêmico nato',skills:Object.freeze(['bajos_fondos','borde_exterior','educacion','estrategia_militar','mundos_del_nucleo','saber_arcaico','xenologia'])}),
  Object.freeze({originalName:'NATTIN',label:'Funileiro nato',skills:Object.freeze(['mecanica'])})
]);
export function naturalRerollOption(character,skillKey){
  const matches=NATURAL_REROLL_RULES.filter(rule=>rule.skills.includes(skillKey)&&talentRankByOriginal(character,rule.originalName)>0);
  if(!matches.length) return {available:false,skillKey,originalName:null,label:null,sessionKey:null};
  const rule=matches[0];
  return {available:true,skillKey,originalName:rule.originalName,label:rule.label,sessionKey:`natural:${rule.originalName}`};
}
