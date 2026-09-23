import type { CharacterSheet } from '../db';
import { normalizeEquipmentMechanics } from './equipment';

export const ADVANCEMENT_COST: Record<number,number> = {1:4,2:8,3:12,4:20,5:26,6:30};

function list(value:string):string[]{ return String(value||'').split(/[;\n,]+/).map(x=>x.trim()).filter(Boolean); }
function costFor(target:number):number { return ADVANCEMENT_COST[Math.max(1,Math.min(6,Math.trunc(target)))]||999; }

export function advanceSkill(input:CharacterSheet,skillName:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input);
  const skill=next.skills.find(x=>x.name===skillName);
  if(!skill) return {sheet:input,cost:0,allowed:false,reason:'Perícia não encontrada.'};
  const target=Math.max(1,Number(skill.rank||0)+1);
  if(target>6) return {sheet:input,cost:0,allowed:false,reason:'A Perícia já está no nível máximo 6.'};
  const cost=costFor(target);
  if(next.resources.skillPoints<cost) return {sheet:input,cost,allowed:false,reason:'Pontos de Perícia insuficientes.'};
  next.resources.skillPoints-=cost; skill.rank=target;
  return {sheet:next,cost,allowed:true,reason:'Perícia elevada para '+target+'.'};
}
export function advanceCombatProficiency(input:CharacterSheet,group:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input);
  const prof={...(next.combat.proficiencies||{})};
  const target=Math.max(1,Number(prof[group]||0)+1);
  if(target>6) return {sheet:input,cost:0,allowed:false,reason:'Proficiência já está no nível máximo 6.'};
  const cost=costFor(target);
  if(next.resources.adventurePoints<cost) return {sheet:input,cost,allowed:false,reason:'Pontos de Aventura insuficientes.'};
  next.resources.adventurePoints-=cost; prof[group]=target; next.combat.proficiencies=prof;
  return {sheet:next,cost,allowed:true,reason:'Proficiência elevada para '+target+'.'};
}
export function advanceValor(input:CharacterSheet,reward:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input); const target=Math.max(2,Number(next.attributes.valor||1)+1);
  if(target>6) return {sheet:input,cost:0,allowed:false,reason:'Valor já está no nível máximo 6.'};
  const cost=costFor(target);
  if(next.resources.adventurePoints<cost) return {sheet:input,cost,allowed:false,reason:'Pontos de Aventura insuficientes.'};
  if(!reward.trim()) return {sheet:input,cost,allowed:false,reason:'A elevação de Valor exige escolher uma Recompensa.'};
  next.resources.adventurePoints-=cost; next.attributes.valor=target;
  const rewards=list(next.other.rewards); rewards.push(reward.trim()); next.other.rewards=Array.from(new Set(rewards)).join('; ');
  return {sheet:normalizeEquipmentMechanics(next),cost,allowed:true,reason:'Valor elevado para '+target+' e Recompensa adquirida.'};
}
export function advanceWisdom(input:CharacterSheet,virtue:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input); const target=Math.max(2,Number(next.attributes.wisdom||1)+1);
  if(target>6) return {sheet:input,cost:0,allowed:false,reason:'Sabedoria já está no nível máximo 6.'};
  const cost=costFor(target);
  if(next.resources.adventurePoints<cost) return {sheet:input,cost,allowed:false,reason:'Pontos de Aventura insuficientes.'};
  if(!virtue.trim()) return {sheet:input,cost,allowed:false,reason:'A elevação de Sabedoria exige escolher uma Virtude.'};
  next.resources.adventurePoints-=cost; next.attributes.wisdom=target;
  const virtues=list(next.other.virtues); virtues.push(virtue.trim()); next.other.virtues=Array.from(new Set(virtues)).join('; ');
  const v=virtue.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  if(v==='confianca'){ next.attributes.hopeMax+=2; next.attributes.hopeCurrent=Math.min(next.attributes.hopeMax,next.attributes.hopeCurrent+2); }
  if(v==='resiliencia'){ next.attributes.enduranceMax+=2; next.attributes.enduranceCurrent=Math.min(next.attributes.enduranceMax,next.attributes.enduranceCurrent+2); }
  if(v==='destreza') next.combat.parry+=1;
  return {sheet:normalizeEquipmentMechanics(next),cost,allowed:true,reason:'Sabedoria elevada para '+target+' e Virtude adquirida.'};
}
