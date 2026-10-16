import {AOR_TALENT_TREES} from './aorTalentTreesData.mjs';
import {AOR_IMMEDIATE_TALENT_EFFECTS, AOR_RUNTIME_MECHANICAL_TALENTS} from './aorTalentRuntimeAliases.mjs';
export const AOR_CORE_CAREER_KEYS=Object.freeze(Object.keys(AOR_TALENT_TREES));
// Purchase/path enforcement is available for every source-backed slot.
// Immediate effects are applied on purchase. Other advertised mechanical
// talents are explicitly mapped to existing combat, skill, vehicle or recovery
// consumers; unimplemented AoR actions remain mechanicallyActive=false.
export const AOR_IMMEDIATE_MECHANICAL_TALENTS=AOR_IMMEDIATE_TALENT_EFFECTS;
export function aorTree(career,specialization){return AOR_TALENT_TREES?.[career]?.[specialization]||null;}
export function aorTalentCatalog({career=null,specialization=null,query=''}={}){
 const q=String(query).trim().toLowerCase(),rows=[];
 for(const [c,ss] of Object.entries(AOR_TALENT_TREES)) for(const [sp,t] of Object.entries(ss)){
  if(career&&c!==career)continue;if(specialization&&sp!==specialization)continue;
  for(const n of t.nodes){const r={campaignMode:'AOR',career:c,specialization:sp,...n,purchaseActive:true,mechanicallyActive:AOR_RUNTIME_MECHANICAL_TALENTS.includes(n.originalName)};if(!q||`${c} ${sp} ${n.name} ${n.originalName}`.toLowerCase().includes(q))rows.push(r);}
 } return rows;
}
export function aorProgressionTreeSeed(career,specialization){const t=aorTree(career,specialization);if(!t)throw new Error('Árvore AoR não encontrada');return {campaignMode:'AOR',career,specialization,tree:t,sourcePolicy:'AUDITED_PRIMARY_SOURCE'};}
export function aorTalentCoverage(){return {careers:AOR_CORE_CAREER_KEYS.length,specializations:Object.values(AOR_TALENT_TREES).reduce((n,x)=>n+Object.keys(x).length,0),positions:aorTalentCatalog().length,mechanicalPositions:aorTalentCatalog().filter(n=>n.mechanicallyActive).length,purchasePositions:aorTalentCatalog().length};}
