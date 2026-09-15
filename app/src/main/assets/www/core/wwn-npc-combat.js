import { RNG_ALGORITHM } from './rng-core.js';

export const WWN_NPC_COMBAT_SOURCES=Object.freeze({
  stats:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§5.1.0 Monster and NPC Statistics — Normal Humans'}),
  hitDice:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§5.1.0 Hit Dice: 1d8 HP per HD'}),
  weapons:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.4.2 Weapon Statistics — Spear, Light'}),
  mortalInjury:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.5.1.1 NPCs and Mortal Injury'}),
});

export const SRD_NPC_PROFILES=Object.freeze({
  THUG_OR_MILITIA:Object.freeze({profileId:'THUG_OR_MILITIA',hitDice:1,armorClass:13,attackBonus:1,moveFeet:30,morale:8,instinct:4,skillBonus:1,saveTarget:15,importantNamed:false,sourceRefs:Object.freeze([WWN_NPC_COMBAT_SOURCES.stats])}),
});

export const SRD_WEAPONS=Object.freeze({
  LIGHT_SPEAR_MELEE:Object.freeze({weaponId:'LIGHT_SPEAR_MELEE',name:'Lança leve',combatSkill:'Stab',attribute:'Strength',damage:'1d6',melee:true,shock:Object.freeze({damage:2,ac:13}),traits:Object.freeze(['T']),sourceRefs:Object.freeze([WWN_NPC_COMBAT_SOURCES.weapons])}),
  LIGHT_SPEAR_THROWN:Object.freeze({weaponId:'LIGHT_SPEAR_THROWN',name:'Lança leve (arremesso)',combatSkill:'Stab',attribute:'Dexterity',damage:'1d6',melee:false,shock:null,rangeFeet:Object.freeze({normal:30,long:60}),traits:Object.freeze(['T']),sourceRefs:Object.freeze([WWN_NPC_COMBAT_SOURCES.weapons])}),
});

export function rollNpcHitPoints({rng,profile,streamName='npc/hp'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  if(!profile||!Number.isInteger(profile.hitDice)||profile.hitDice<1)throw new RangeError('profile hitDice required');
  const dice=[];let hp=0;for(let i=0;i<profile.hitDice;i+=1){const d=rng.die(8,streamName);dice.push(d);hp+=d.result;}
  return Object.freeze({rule:'WWN_NPC_HP_BY_HD_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_NPC_COMBAT_SOURCES.hitDice],hitDice:profile.hitDice,dice:Object.freeze(dice),hp});
}

export function instantiateNpcCombatant({npcId,name,profileId='THUG_OR_MILITIA',weaponId='LIGHT_SPEAR_MELEE',hp}){
  const profile=SRD_NPC_PROFILES[profileId],weapon=SRD_WEAPONS[weaponId];if(!profile)throw new RangeError('unknown NPC profile');if(!weapon)throw new RangeError('unknown weapon profile');if(!Number.isInteger(hp)||hp<1)throw new RangeError('hp required');
  return Object.freeze({npcId,name,profileId,hitDice:profile.hitDice,hp,maxHp:hp,meleeAC:profile.armorClass,rangedAC:profile.armorClass,armorClass:profile.armorClass,attackBonus:profile.attackBonus,moveFeet:profile.moveFeet,morale:profile.morale,instinct:profile.instinct,skillBonus:profile.skillBonus,saveTarget:profile.saveTarget,importantNamed:profile.importantNamed,weaponId,weaponContract:weapon,sourceRefs:Object.freeze([...profile.sourceRefs,...weapon.sourceRefs])});
}
