function clone(v){return structuredClone(v)}

export function characterIdOf(character){
  if(!character||typeof character!=='object')return null;
  return String(character.characterId??character.id??'').trim()||null;
}

export function characterById(state,characterId){
  const id=String(characterId??'').trim();
  if(!id)return null;
  const roster=state?.charactersState;
  if(roster&&typeof roster==='object'&&roster[id])return roster[id];
  const active=state?.characterState;
  if(active&&characterIdOf(active)===id)return active;
  return null;
}

export function setCharacterById(state,characterId,character){
  const id=String(characterId??'').trim();
  if(!id||!character||typeof character!=='object')throw new TypeError('characterId and character required');
  state.charactersState={...(state.charactersState??{}),[id]:clone(character)};
  if(characterIdOf(state.characterState)===id)state.characterState=clone(character);
  return state;
}

export function activePartyCombatantIds(state){
  const party=state?.partyState;
  const activeGroup=party?.groups?.[party.activeGroupId];
  const raw=Array.isArray(activeGroup?.memberIds)&&activeGroup.memberIds.length
    ? activeGroup.memberIds
    : [characterIdOf(state?.characterState)].filter(Boolean);
  const ids=[];
  for(const value of raw){
    const id=String(value??'').trim();
    if(id&&!ids.includes(id)&&characterById(state,id))ids.push(id);
  }
  return Object.freeze(ids);
}

export function combatPcIds(state,combat=state?.encounterState?.combat){
  const raw=Array.isArray(combat?.pcIds)&&combat.pcIds.length
    ? combat.pcIds
    : [combat?.pcId??characterIdOf(state?.characterState)].filter(Boolean);
  const ids=[];
  for(const value of raw){const id=String(value??'').trim();if(id&&!ids.includes(id)&&characterById(state,id))ids.push(id)}
  return Object.freeze(ids);
}

export function isConditionActive(character,condition){
  const needle=String(condition??'').toUpperCase();
  const raw=character?.conditions??[];
  const list=Array.isArray(raw)?raw:Object.entries(raw).filter(([,v])=>Boolean(v)).map(([k])=>k);
  return list.some(c=>String(typeof c==='string'?c:(c?.id??c?.type??c?.name??'')).toUpperCase()===needle);
}

export function addCharacterCondition(character,condition){
  const next=clone(character);
  const raw=next.conditions??[];
  const list=Array.isArray(raw)?raw.map(c=>typeof c==='string'?c:(c?.id??c?.type??c?.name)).filter(Boolean):Object.entries(raw).filter(([,v])=>Boolean(v)).map(([k])=>k);
  if(!list.some(c=>String(c).toUpperCase()===String(condition).toUpperCase()))list.push(String(condition));
  next.conditions=list;
  return next;
}

export function removeCharacterCondition(character,condition){
  const next=clone(character);const needle=String(condition).toUpperCase();
  const raw=next.conditions??[];const list=Array.isArray(raw)?raw.map(c=>typeof c==='string'?c:(c?.id??c?.type??c?.name)).filter(Boolean):Object.entries(raw).filter(([,v])=>Boolean(v)).map(([k])=>k);
  next.conditions=list.filter(c=>String(c).toUpperCase()!==needle);return next;
}

export function isCombatCapablePc(character){
  return Boolean(character&&Number.isInteger(character.hp?.current)&&character.hp.current>0&&!isConditionActive(character,'DEAD'));
}

export function hasFocus(character,focusId,minLevel=1){
  const id=String(focusId??'').toUpperCase();
  return (character?.foci??[]).some(f=>{
    if(typeof f==='string')return String(f).toUpperCase()===id;
    return String(f?.id??f?.name??'').toUpperCase()===id && Number(f?.level??1)>=minLevel;
  });
}

export function bestPcDexterityScore(state,pcIds){
  let best=null;
  for(const id of pcIds){const score=characterById(state,id)?.attributes?.Dexterity;if(Number.isInteger(score)&&(best===null||score>best))best=score;}
  return best;
}

export function syncActiveCharacterFromRoster(state){
  const id=characterIdOf(state?.characterState);if(!id)return state;
  const current=state.charactersState?.[id];if(current)state.characterState=clone(current);
  return state;
}
