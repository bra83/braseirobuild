const clone=v=>structuredClone(v);
const TYPES=new Set(['staggered','immobilized','disoriented','prone','fear_setback','fear_difficulty','fear_boost','fear_immunity','fear_checked']);
function list(character){return Array.isArray(character?.statusEffects)?character.statusEffects:[];}
function uid(type){return `${type}_${Date.now()}_${Math.random().toString(36).slice(2,8)}`;}

export function normalizeStatusEffects(character){const next=clone(character);next.statusEffects=list(next).filter(e=>e&&TYPES.has(e.type)&&e.active!==false).map(e=>({...e,active:e.active!==false}));return next;}
export function applyStatusEffect(character,{type,source='rule',sourceId=null,duration='next_turn',rounds=1,effect={}}={}){
  if(!TYPES.has(type)) throw new Error('Efeito de estado desconhecido.');
  const next=normalizeStatusEffects(character); const existing=next.statusEffects.find(e=>e.type===type&&e.sourceId===sourceId&&e.active!==false);
  const roundsValue=Math.max(1,Number.isInteger(Number(rounds))?Number(rounds):1);
  if(existing){
    if(['next_turn','rounds'].includes(duration)) existing.roundsRemaining=Math.max(1,Number(existing.roundsRemaining??0))+roundsValue;
    existing.duration=duration==='encounter'?'encounter':existing.duration; existing.effect={...existing.effect,...clone(effect)}; return next;
  }
  next.statusEffects.push({id:uid(type),type,source:String(source),sourceId:sourceId?String(sourceId):null,duration,roundsRemaining:['next_turn','rounds'].includes(duration)?roundsValue:null,effect:clone(effect),active:true});
  return next;
}
export function removeStatusEffect(character,{type=null,sourceId=null}={}){const next=normalizeStatusEffects(character);next.statusEffects=next.statusEffects.filter(e=>!((!type||e.type===type)&&(!sourceId||e.sourceId===sourceId)));return next;}
export function statusModifiers(character,{consumeNextCheck=false}={}){
  const next=normalizeStatusEffects(character); const out={character:next,cannotAct:false,cannotManeuver:false,prone:false,setback:0,difficultyIncrease:0,boost:0,fearImmuneSources:[]};
  for(const e of next.statusEffects){
    if(e.type==='staggered') out.cannotAct=true;
    if(e.type==='immobilized') out.cannotManeuver=true;
    if(e.type==='disoriented') out.setback+=1;
    if(e.type==='prone') out.prone=true;
    if(e.type==='fear_setback') out.setback+=Number(e.effect?.setback??1);
    if(e.type==='fear_difficulty') out.difficultyIncrease+=Number(e.effect?.difficultyIncrease??1);
    if(e.type==='fear_boost') {out.boost+=Number(e.effect?.boost??1); if(consumeNextCheck) e.active=false;}
    if(e.type==='fear_immunity'&&e.sourceId) out.fearImmuneSources.push(e.sourceId);
  }
  next.statusEffects=next.statusEffects.filter(e=>e.active!==false);
  return out;
}
export function expireStatusTurn(character){const next=normalizeStatusEffects(character);for(const e of next.statusEffects){if(['next_turn','rounds'].includes(e.duration)){e.roundsRemaining=Math.max(0,Number(e.roundsRemaining??1)-1);if(e.roundsRemaining===0)e.active=false;}}next.statusEffects=next.statusEffects.filter(e=>e.active!==false);return next;}
export function expireStatusEncounter(character){const next=normalizeStatusEffects(character);next.statusEffects=next.statusEffects.filter(e=>e.duration!=='encounter'&&e.active!==false);return next;}
export function hasFearImmunity(character,sourceId){return Boolean(sourceId&&statusModifiers(character).fearImmuneSources.includes(String(sourceId)));}

export function hasFearChecked(character,sourceId){if(!sourceId)return false;return normalizeStatusEffects(character).statusEffects.some(e=>e.type==='fear_checked'&&e.sourceId===String(sourceId)&&e.active!==false);}
