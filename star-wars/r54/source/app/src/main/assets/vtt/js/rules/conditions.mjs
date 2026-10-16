export const CONDITION_SOURCE=Object.freeze({book:'Star Wars: Edge of the Empire — Core Rulebook',printedPage:218,section:'Status Effects'});
export const CONDITION_TYPES=Object.freeze(['staggered','immobilized','disoriented','on_fire','ensnared']);
export function normalizePlayerConditions(value){const v=value&&typeof value==='object'?value:{};const out={};for(const k of CONDITION_TYPES)out[k]=Math.max(0,Number.isInteger(Number(v[k]))?Number(v[k]):0);return out}
export function applyPlayerCondition(combat,type,{rounds=1}={}){if(!CONDITION_TYPES.includes(type))throw new Error('Condição de combate inválida.');const n=Number(rounds);if(!Number.isInteger(n)||n<1)throw new Error('Duração da condição inválida.');const next=structuredClone(combat);next.playerConditions=normalizePlayerConditions(next.playerConditions);next.playerConditions[type]+=n;return next}
export function playerConditionModifiers(combat){const c=normalizePlayerConditions(combat?.playerConditions);return {conditions:c,cannotAct:c.staggered>0,cannotManeuver:(c.immobilized>0||c.ensnared>0),setback:c.disoriented>0?1:0,onFire:c.on_fire>0,ensnared:c.ensnared>0}}
export function expirePlayerConditionsEndTurn(combat){const next=structuredClone(combat);next.playerConditions=normalizePlayerConditions(next.playerConditions);for(const k of CONDITION_TYPES)next.playerConditions[k]=Math.max(0,next.playerConditions[k]-1);return next}
export function clearPlayerConditions(combat){const next=structuredClone(combat);next.playerConditions=normalizePlayerConditions(null);return next}
export function extinguishFireCheck({surface='hard',hasWater=false}={}){
  if(hasWater) return {difficulty:0,success:true,autoExtinguish:true};
  if(surface==='soft'||surface==='grass') return {difficulty:1,skill:'coordinacion',name:'Fácil (1 Roxo)'};
  return {difficulty:2,skill:'coordinacion',name:'Médio (2 Roxos)'};
}
export function escapeEnsnareCheck(){
  return {difficulty:3,skill:'atletismo',name:'Difícil (3 Roxos)'};
}
