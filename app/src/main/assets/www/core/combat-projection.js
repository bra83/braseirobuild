import { characterStatusProjection } from './party-state.js';
import { distanceFeet, freshActionEconomy } from './wwn-tactical-combat.js';
import { characterById, combatPcIds, isConditionActive } from './combat-roster.js';
import { deriveArmorClass, deriveCombatMoveFeet } from './wwn-armor-core.js';
import { inspectWeaponReadiness, reloadTrait, singleShotReloadProgress } from './wwn-reload-core.js';
import { materializeWeaponContract, WWN_EQUIPMENT_SOURCES } from './wwn-equipment-catalog.js';
import { totalDefenseActive } from './wwn-defense-core.js';

function clone(v){return structuredClone(v)}
function enemyVisibleStatus(npc){if(!npc||!Number.isInteger(npc.hp)||!Number.isInteger(npc.maxHp)||npc.maxHp<1)return'ESTADO DESCONHECIDO';if(npc.hp<=0)return'FORA DE COMBATE';const ratio=npc.hp/npc.maxHp;if(ratio<=.25)return'MUITO FERIDO';if(ratio<=.6)return'FERIDO';return'EM COMBATE';}
function activeCharacterId(state){return String(state?.characterState?.characterId??state?.characterState?.id??'')}

function skillPt(skill){return({Shoot:'Atirar',Stab:'Apunhalar',Exert:'Esforço',Punch:'Briga'})[skill]??skill;}
function weaponLabel(item,mode,skill){const modeName=mode==='MELEE'?'corpo a corpo':mode==='THROWN'?'arremesso':'distância';return `${item.name} • ${modeName}${mode==='THROWN'?` • ${skillPt(skill)}`:''}`;}
function weaponSummary(weapon){const parts=[weapon.damage];if(weapon.shock)parts.push(`Dano de choque ${weapon.shock.damage}/${weapon.shock.ac??'-'}`);if(weapon.melee===false&&weapon.rangeFeet)parts.push(`${weapon.rangeFeet.normal}/${weapon.rangeFeet.long} pés`);return parts.join(' • ');}
function projectWeapons(pc){
  const choices=[Object.freeze({choiceId:'UNARMED',weaponId:'UNARMED',modeId:null,combatSkill:'Punch',name:'Ataque desarmado',label:'Ataque desarmado',summary:'1d2 + Briga • corpo a corpo',damageFormula:'1d2',carryState:'BUILT_IN',loaded:true,reloadTrait:null,readiness:'READY',canAttack:true,reloadRequired:false,sourceRefs:[WWN_EQUIPMENT_SOURCES.weapons]})];
  if(!Array.isArray(pc?.inventory))return Object.freeze({inventoryKnown:false,choices:Object.freeze(choices)});
  for(const item of pc.inventory){if(item?.kind!=='WEAPON')continue;const modes=item.weaponModes??item.modes??[];for(const mode of modes){const skills=mode.modeId==='THROWN'?['Shoot','Stab','Exert']:[mode.combatSkill];for(const combatSkill of skills){const m=materializeWeaponContract(pc,item,{modeId:mode.modeId,combatSkill});if(m.status!=='OK')continue;const readiness=inspectWeaponReadiness({item,inventory:pc.inventory,shootSkillLevel:pc.skills?.Shoot??null});choices.push(Object.freeze({choiceId:`${item.itemId??item.id}:${mode.modeId}:${combatSkill}`,weaponId:item.itemId??item.id,modeId:mode.modeId,combatSkill,name:item.name,label:weaponLabel(item,mode.modeId,combatSkill),summary:weaponSummary(m.weapon),damageFormula:m.weapon.damage,carryState:String(item.carryState??'').toUpperCase(),loaded:item.loaded===true||reloadTrait(item)===null,reloadTrait:reloadTrait(item),readiness:readiness.status,canAttack:readiness.status==='READY',reloadRequired:readiness.status==='RELOAD_REQUIRED',missingAmmo:readiness.status==='MISSING_READIED_AMMO',ammoType:item.ammoType??null,actionCost:readiness.reload?.cost??null,reloadProgress:singleShotReloadProgress(item),sourceRefs:clone(m.weapon.sourceRefs??[])}));}}}
  return Object.freeze({inventoryKnown:true,choices:Object.freeze(choices)});
}

export function projectCombatForPlayer(state){
  const combat=state?.encounterState?.combat;if(!combat)return Object.freeze({status:'NO_COMBAT'});
  const pcIds=[...combatPcIds(state,combat)];
  const pcs=pcIds.map(id=>{
    const pc=characterById(state,id),position=combat.tactical?.actors?.[id],injury=pc?.mortalInjury??null;const armor=deriveArmorClass(pc);const movement=deriveCombatMoveFeet(pc);
    return Object.freeze({
      actorId:id,name:pc?.name??id,position:position?clone(position):null,statusBars:characterStatusProjection(pc),armorClass:armor.status==='OK'?armor.armorClass+(totalDefenseActive(combat,id)?2:0):null,baseArmorClass:armor.status==='OK'?armor.armorClass:null,armor:clone(armor),moveFeet:movement.status==='OK'?movement.moveFeet:null,
      economy:clone(combat.economy?.[id]??freshActionEconomy()),totalDefense:totalDefenseActive(combat,id),mortallyInjured:isConditionActive(pc,'MORTALLY_INJURED'),stabilized:injury?.stabilized===true,dead:isConditionActive(pc,'DEAD'),
      roundsSinceDowned:Number.isInteger(injury?.roundsSinceDowned)?injury.roundsSinceDowned:null,roundsUntilDeath:injury&&!injury.stabilized&&!injury.dead?Math.max(0,6-Number(injury.roundsSinceDowned??0)):null,
      classId:pc?.classId??pc?.class??null,level:pc?.level??null,weapons:projectWeapons(pc),
    });
  });
  const activeId=activeCharacterId(state);const legacyPc=pcs.find(p=>p.actorId===activeId)??pcs[0]??null;
  const enemies=(combat.enemyIds??[]).map(id=>{
    const npc=state.npcState?.[id],pos=combat.tactical?.actors?.[id],distances={};
    for(const pc of pcs)if(pc.position&&pos)distances[pc.actorId]=distanceFeet(pc.position,pos);
    return Object.freeze({actorId:id,name:npc?.name??id,status:enemyVisibleStatus(npc),position:pos?clone(pos):null,distancesByPcId:Object.freeze(distances),distanceFeet:legacyPc?.actorId&&distances[legacyPc.actorId]!==undefined?distances[legacyPc.actorId]:null,defeated:(npc?.hp??0)<=0});
  });
  return Object.freeze({
    status:'OK',active:combat.active===true,encounterId:combat.encounterId??null,round:combat.round??null,order:Object.freeze([...(combat.order??[])]),activeSideId:combat.activeSideId??null,outcome:combat.outcome??null,
    pc:legacyPc,pcs:Object.freeze(pcs),enemies:Object.freeze(enemies),units:combat.tactical?.units??'feet',sourceRefs:clone(combat.sourceRefs??[]),
  });
}
