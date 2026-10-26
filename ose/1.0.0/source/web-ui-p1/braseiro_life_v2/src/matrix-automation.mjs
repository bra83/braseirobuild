import {stableToken,deriveCharacterProfile} from './life-arcs.mjs';
const allowed=new Set(['event','advance','rest','travel','visit','scene_tick','evaluate']);
export function validateAutomation(options){
 if(!options||typeof options.enabled!=='boolean')throw Error('Automação exige enabled booleano');
 const o={enabled:options.enabled,chancePercent:35,cooldownDays:7,maxAttemptsPerTrigger:1,autoPresent:true,characterIds:[],...structuredClone(options)};
 for(const [k,lo,hi] of [['chancePercent',0,100],['cooldownDays',1,3650],['maxAttemptsPerTrigger',1,8]])if(!Number.isInteger(o[k])||o[k]<lo||o[k]>hi)throw Error(`Automação: ${k} inválido`);
 if(typeof o.autoPresent!=='boolean'||!Array.isArray(o.characterIds)||!o.characterIds.every(x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)))throw Error('Automação: configuração inválida');
 return o;
}
/** Returns a bounded plan, never executes IO or changes state. Called after normal gameplay. */
export function planAutomaticMatrices(s,trigger){
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
