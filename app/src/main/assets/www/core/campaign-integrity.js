const clone=v=>structuredClone(v);
const isObj=v=>Boolean(v)&&typeof v==='object'&&!Array.isArray(v);
export const CAMPAIGN_SCHEMA_VERSION=1;
export function validateCampaignState(state,{campaignId=null,revision=null}={}){
  const errors=[],warnings=[];
  if(!isObj(state))return Object.freeze({ok:false,errors:Object.freeze(['STATE_NOT_OBJECT']),warnings:Object.freeze([])});
  if(state.schemaVersion!==CAMPAIGN_SCHEMA_VERSION)errors.push(`UNSUPPORTED_SCHEMA_VERSION:${state.schemaVersion}`);
  if(state.authorityBindings?.system!=='WWN')errors.push('SYSTEM_NOT_WWN');
  if(!String(state.campaignId??'').trim())errors.push('CAMPAIGN_ID_MISSING');
  if(campaignId!==null&&String(state.campaignId)!==String(campaignId))errors.push('CAMPAIGN_ID_MISMATCH');
  if(!Number.isInteger(state.revision)||state.revision<0)errors.push('REVISION_INVALID');
  if(revision!==null&&state.revision!==revision)errors.push('REVISION_MISMATCH');
  if(!isObj(state.rngState)||!String(state.rngState.masterSeed??'').trim()||!isObj(state.rngState.counters))errors.push('RNG_STATE_INVALID');
  const roster=isObj(state.charactersState)?state.charactersState:{};
  for(const [id,c] of Object.entries(roster)){if(!isObj(c))errors.push(`ROSTER_CHARACTER_INVALID:${id}`);else if(String(c.characterId??'')!==id)errors.push(`ROSTER_CHARACTER_ID_MISMATCH:${id}`);}
  if(state.characterState?.characterId&&!isObj(state.characterState))errors.push('ACTIVE_CHARACTER_INVALID');
  const active=state.characterState?.characterId??null;
  if(active&&roster[active]){
    const a=JSON.stringify(state.characterState),b=JSON.stringify(roster[active]);
    if(a!==b)warnings.push('ACTIVE_CHARACTER_ROSTER_DRIFT');
  }else if(active)warnings.push('ACTIVE_CHARACTER_NOT_IN_ROSTER');
  const groups=state.partyState?.groups;
  if(groups&&isObj(groups)){
    const seen=new Set();
    for(const [gid,g] of Object.entries(groups))for(const id of g?.memberIds??[]){
      if(!roster[id]&&id!==active)warnings.push(`PARTY_MEMBER_NOT_IN_ROSTER:${gid}:${id}`);
      if(seen.has(id))errors.push(`PARTY_MEMBER_DUPLICATED:${id}`);seen.add(id);
    }
    if(state.partyState.activeGroupId&&!groups[state.partyState.activeGroupId])errors.push('ACTIVE_PARTY_GROUP_MISSING');
  }
  if(state.worldState!=null&&!isObj(state.worldState))errors.push('WORLD_STATE_INVALID');
  if(state.factionState!=null&&!isObj(state.factionState))errors.push('FACTION_STATE_INVALID');
  const npcs=state.npcState??state.worldState?.npcs;if(npcs!=null&&!isObj(npcs))errors.push('NPC_REGISTRY_INVALID');else if(isObj(npcs))for(const [id,n] of Object.entries(npcs)){if(!isObj(n))errors.push(`NPC_INVALID:${id}`);else if(String(n.npcId??'')!==id)errors.push(`NPC_ID_MISMATCH:${id}`);if(Array.isArray(n?.memories)&&n.memories.some(m=>!isObj(m)||!String(m.summary??'').trim()))errors.push(`NPC_MEMORY_INVALID:${id}`);}
  const projects=state.worldState?.majorProjects;if(projects!=null&&!isObj(projects))errors.push('MAJOR_PROJECTS_INVALID');else if(isObj(projects))for(const [id,p] of Object.entries(projects)){if(!isObj(p))errors.push(`MAJOR_PROJECT_INVALID:${id}`);else{if(String(p.projectId??'')!==id)errors.push(`MAJOR_PROJECT_ID_MISMATCH:${id}`);if(!['ACTIVE','COMPLETED','FAILED','ABANDONED'].includes(String(p.status??'')))warnings.push(`MAJOR_PROJECT_STATUS_UNKNOWN:${id}`);}}
  const memory=state.memoryState;if(memory!=null&&!isObj(memory))errors.push('MEMORY_STATE_INVALID');else if(isObj(memory)){if(memory.facts!=null&&!Array.isArray(memory.facts))errors.push('MEMORY_FACTS_INVALID');if(memory.threads!=null&&!isObj(memory.threads))errors.push('MEMORY_THREADS_INVALID');if(memory.revealedSecrets!=null&&!Array.isArray(memory.revealedSecrets))errors.push('MEMORY_REVEALED_INVALID');const factIds=[];for(const f of memory.facts??[]){if(!isObj(f)||!String(f.factId??'').trim()||!String(f.summary??'').trim()||!['GM_ONLY','PLAYER_KNOWN'].includes(String(f.visibility??'')))errors.push('MEMORY_FACT_INVALID');else factIds.push(String(f.factId));}if(new Set(factIds).size!==factIds.length)errors.push('MEMORY_FACT_ID_DUPLICATED');const known=new Set(factIds);for(const [id,t] of Object.entries(memory.threads??{})){if(!isObj(t)||String(t.threadId??'')!==id||!Array.isArray(t.factIds))errors.push(`MEMORY_THREAD_INVALID:${id}`);else for(const fid of t.factIds)if(!known.has(String(fid)))warnings.push(`MEMORY_THREAD_DANGLING_FACT:${id}:${fid}`);}for(const fid of memory.revealedSecrets??[])if(!known.has(String(fid)))warnings.push(`MEMORY_REVEALED_DANGLING_FACT:${fid}`);}
  const schedule=state.scheduleState;if(schedule!=null&&!Array.isArray(schedule))errors.push('SCHEDULE_STATE_INVALID');else if(Array.isArray(schedule)){const ids=[];for(const e of schedule){if(!isObj(e)||!String(e.eventId??'').trim()||!Number.isInteger(e.dueMinute)||e.dueMinute<0||!String(e.summary??'').trim()||!['GM_ONLY','PLAYER_KNOWN'].includes(String(e.visibility??'GM_ONLY'))||typeof e.completed!=='boolean')errors.push('SCHEDULE_EVENT_INVALID');else ids.push(String(e.eventId));}if(new Set(ids).size!==ids.length)errors.push('SCHEDULE_EVENT_ID_DUPLICATED');}
  for(const [id,c] of Object.entries(roster)){const pa=c?.pendingAdvancement;if(pa!=null&&!isObj(pa))errors.push(`PENDING_ADVANCEMENT_INVALID:${id}`);else if(isObj(pa)){for(const k of ['focusPick','newSpells','artPicks'])if(pa[k]!=null&&(!Number.isInteger(pa[k])||pa[k]<0))errors.push(`PENDING_ADVANCEMENT_COUNTER_INVALID:${id}:${k}`);}if(c?.inventory!=null&&!Array.isArray(c.inventory))errors.push(`INVENTORY_INVALID:${id}`);const am=c?.advancedMagic;if(am!=null&&!isObj(am))errors.push(`ADVANCED_MAGIC_INVALID:${id}`);else if(isObj(am)){if(am.labs!=null&&!Array.isArray(am.labs))errors.push(`ADVANCED_MAGIC_LABS_INVALID:${id}`);if(am.workings!=null&&!Array.isArray(am.workings))errors.push(`ADVANCED_MAGIC_WORKINGS_INVALID:${id}`);if(am.itemProjects!=null&&!Array.isArray(am.itemProjects))errors.push(`ADVANCED_MAGIC_ITEMS_INVALID:${id}`);if(am.spellResearch!=null&&!isObj(am.spellResearch))errors.push(`ADVANCED_MAGIC_RESEARCH_INVALID:${id}`);}}
  const combat=state.encounterState?.combat;
  if(combat?.active===true){if(!Number.isInteger(combat.round)||combat.round<1)errors.push('COMBAT_ROUND_INVALID');if(!combat.pcId&&!Array.isArray(combat.pcIds))warnings.push('COMBAT_PC_BINDING_MISSING');}
  const special=combat?.specialState;if(special!=null&&!isObj(special))errors.push('SPECIAL_COMBAT_STATE_INVALID');else if(isObj(special)){for(const k of ['proneByCombatant','holds','screens','grapples'])if(special[k]!=null&&!isObj(special[k]))errors.push(`SPECIAL_COMBAT_MAP_INVALID:${k}`);if(special.maneuverHistory!=null&&!Array.isArray(special.maneuverHistory))errors.push('SPECIAL_COMBAT_HISTORY_INVALID');}
  return Object.freeze({ok:errors.length===0,errors:Object.freeze(errors),warnings:Object.freeze(warnings)});
}
export function repairCampaignState(state){
  const next=clone(state);const repairs=[];
  next.charactersState=isObj(next.charactersState)?next.charactersState:{};
  if(isObj(next.worldState?.npcs)){next.npcState={...next.worldState.npcs,...(isObj(next.npcState)?next.npcState:{})};delete next.worldState.npcs;repairs.push('LEGACY_WORLD_NPCS_MIGRATED_TO_CANONICAL_REGISTRY');}
  else if(!isObj(next.npcState))next.npcState={};
  if(next.characterState?.characterId){const id=next.characterState.characterId;if(!next.charactersState[id]){next.charactersState[id]=clone(next.characterState);repairs.push('ACTIVE_CHARACTER_ADDED_TO_ROSTER');}else if(JSON.stringify(next.charactersState[id])!==JSON.stringify(next.characterState)){next.charactersState[id]=clone(next.characterState);repairs.push('ACTIVE_CHARACTER_ROSTER_SYNCED');}}
  if(next.partyState?.groups){const known=new Set(Object.keys(next.charactersState));const seen=new Set();for(const g of Object.values(next.partyState.groups)){g.memberIds=(g.memberIds??[]).filter(id=>known.has(id)&&!seen.has(id)&&seen.add(id));}if(!next.partyState.groups[next.partyState.activeGroupId]){next.partyState.activeGroupId=Object.keys(next.partyState.groups)[0]??null;repairs.push('ACTIVE_PARTY_GROUP_REBOUND');}}
  if(isObj(next.memoryState)){next.memoryState.facts=Array.isArray(next.memoryState.facts)?next.memoryState.facts:[];next.memoryState.threads=isObj(next.memoryState.threads)?next.memoryState.threads:{};next.memoryState.revealedSecrets=Array.isArray(next.memoryState.revealedSecrets)?next.memoryState.revealedSecrets:[];const known=new Set(next.memoryState.facts.filter(isObj).map(f=>String(f.factId??'')).filter(Boolean));for(const [id,t] of Object.entries(next.memoryState.threads)){if(isObj(t)&&Array.isArray(t.factIds)){const clean=[...new Set(t.factIds.map(String).filter(fid=>known.has(fid)))];if(JSON.stringify(clean)!==JSON.stringify(t.factIds)){t.factIds=clean;repairs.push(`MEMORY_THREAD_DANGLING_FACTS_REMOVED:${id}`);}}}const revealed=[...new Set(next.memoryState.revealedSecrets.map(String).filter(fid=>known.has(fid)))];if(JSON.stringify(revealed)!==JSON.stringify(next.memoryState.revealedSecrets)){next.memoryState.revealedSecrets=revealed;repairs.push('MEMORY_REVEALED_DANGLING_FACTS_REMOVED');}}
  if(Array.isArray(next.scheduleState)){const seenSchedule=new Set();const clean=[];for(const e of next.scheduleState){const id=String(e?.eventId??'');if(!id||seenSchedule.has(id))continue;seenSchedule.add(id);clean.push(e);}if(clean.length!==next.scheduleState.length){next.scheduleState=clean;repairs.push('DUPLICATE_SCHEDULE_EVENTS_REMOVED');}}
  return Object.freeze({state:next,repairs:Object.freeze(repairs),validation:validateCampaignState(next)});
}
