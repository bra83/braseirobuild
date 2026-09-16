export const WWN_PARTY_SOURCES = Object.freeze({
  splitForaging: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.12.5–2.12.6 Foraging / Wandering Encounters' }),
  dungeonParallelActions: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.13.2 Timekeeping in the Dungeon' }),
  systemStrain: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.5.2 System Strain' }),
});

function clone(v){ return structuredClone(v); }
function nonEmpty(value,label){ if(typeof value!=='string'||!value.trim())throw new Error(`${label} required`); return value.trim(); }
function ensureUnique(items,label){ const s=new Set(items); if(s.size!==items.length)throw new Error(`${label} contains duplicates`); }

export function characterRosterFromState(state){
  const out={};
  if(state?.charactersState && typeof state.charactersState==='object'){
    for(const [id,c] of Object.entries(state.charactersState)) if(c && typeof c==='object') out[id]=clone(c);
  }
  const c=state?.characterState;
  if(c && typeof c==='object'){
    const id=String(c.characterId??c.id??'active-character');
    if(!out[id])out[id]=clone(c);
  }
  return Object.freeze(out);
}

export function createPartyState({memberIds,position=null,groupId='party-main'}={}){
  if(!Array.isArray(memberIds)||memberIds.length<1)throw new Error('memberIds required');
  const ids=memberIds.map(v=>nonEmpty(String(v),'memberId'));ensureUnique(ids,'memberIds');
  return Object.freeze({
    schemaVersion:1,
    activeGroupId:groupId,
    groups:Object.freeze({
      [groupId]:Object.freeze({groupId,label:'GRUPO',memberIds:Object.freeze([...ids]),position:clone(position),encounterRiskUnit:true})
    }),
    sourceRefs:Object.freeze([WWN_PARTY_SOURCES.splitForaging,WWN_PARTY_SOURCES.dungeonParallelActions]),
  });
}

export function validatePartyState(partyState,{knownMemberIds=null}={}){
  const errors=[];
  if(!partyState||typeof partyState!=='object')return Object.freeze({ok:false,errors:Object.freeze(['partyState required'])});
  const groups=partyState.groups;
  if(!groups||typeof groups!=='object'||Object.keys(groups).length<1)errors.push('groups required');
  const all=[];
  for(const [id,g] of Object.entries(groups??{})){
    if(!g||typeof g!=='object') {errors.push(`invalid group ${id}`);continue;}
    if((g.groupId??id)!==id)errors.push(`groupId mismatch ${id}`);
    if(!Array.isArray(g.memberIds)||g.memberIds.length<1)errors.push(`memberIds required ${id}`);
    else all.push(...g.memberIds);
  }
  if(new Set(all).size!==all.length)errors.push('member cannot exist in more than one group');
  if(Array.isArray(knownMemberIds)){
    const known=new Set(knownMemberIds);
    for(const id of all)if(!known.has(id))errors.push(`unknown member ${id}`);
  }
  if(!groups?.[partyState.activeGroupId])errors.push('activeGroupId must identify a group');
  return Object.freeze({ok:errors.length===0,errors:Object.freeze(errors)});
}

export function splitPartyGroup(partyState,{sourceGroupId,newGroups}){
  const valid=validatePartyState(partyState);if(!valid.ok)throw new Error(valid.errors.join('; '));
  const source=partyState.groups[sourceGroupId];if(!source)return Object.freeze({status:'SOURCE_GROUP_NOT_FOUND_NO_MUTATION'});
  if(!Array.isArray(newGroups)||newGroups.length<2)return Object.freeze({status:'SPLIT_REQUIRES_AT_LEAST_TWO_GROUPS_NO_MUTATION'});
  const sourceMembers=[...source.memberIds].sort();
  const incoming=[];const nextGroups={...clone(partyState.groups)};delete nextGroups[sourceGroupId];
  for(const raw of newGroups){
    const groupId=nonEmpty(raw?.groupId,'groupId');if(nextGroups[groupId])return Object.freeze({status:'DUPLICATE_GROUP_ID_NO_MUTATION',groupId});
    const memberIds=(raw?.memberIds??[]).map(v=>nonEmpty(String(v),'memberId'));if(memberIds.length<1)return Object.freeze({status:'EMPTY_SPLIT_GROUP_NO_MUTATION',groupId});
    incoming.push(...memberIds);
    nextGroups[groupId]={groupId,label:raw.label??groupId,memberIds:[...memberIds],position:clone(raw.position??source.position??null),encounterRiskUnit:true};
  }
  if(new Set(incoming).size!==incoming.length)return Object.freeze({status:'DUPLICATE_MEMBER_IN_SPLIT_NO_MUTATION'});
  if(JSON.stringify([...incoming].sort())!==JSON.stringify(sourceMembers))return Object.freeze({status:'SPLIT_MEMBERS_MUST_MATCH_SOURCE_NO_MUTATION'});
  const next={...clone(partyState),groups:nextGroups,activeGroupId:newGroups[0].groupId,lastSplit:{sourceGroupId,resultGroupIds:newGroups.map(g=>g.groupId)}};
  const check=validatePartyState(next);if(!check.ok)throw new Error(check.errors.join('; '));
  return Object.freeze({status:'OK',partyState:Object.freeze(next),sourceRefs:Object.freeze([WWN_PARTY_SOURCES.splitForaging])});
}

export function mergePartyGroups(partyState,{groupIds,targetGroupId='party-main',label='GRUPO',position=null}){
  const valid=validatePartyState(partyState);if(!valid.ok)throw new Error(valid.errors.join('; '));
  if(!Array.isArray(groupIds)||groupIds.length<2)return Object.freeze({status:'MERGE_REQUIRES_AT_LEAST_TWO_GROUPS_NO_MUTATION'});
  const selected=groupIds.map(id=>partyState.groups[id]);if(selected.some(v=>!v))return Object.freeze({status:'GROUP_NOT_FOUND_NO_MUTATION'});
  const members=selected.flatMap(g=>g.memberIds);ensureUnique(members,'merged memberIds');
  const nextGroups={...clone(partyState.groups)};for(const id of groupIds)delete nextGroups[id];
  if(nextGroups[targetGroupId])return Object.freeze({status:'TARGET_GROUP_ID_EXISTS_NO_MUTATION'});
  nextGroups[targetGroupId]={groupId:targetGroupId,label,memberIds:[...members],position:clone(position??selected[0].position??null),encounterRiskUnit:true};
  const next={...clone(partyState),groups:nextGroups,activeGroupId:targetGroupId,lastMerge:{sourceGroupIds:[...groupIds],targetGroupId}};
  return Object.freeze({status:'OK',partyState:Object.freeze(next),sourceRefs:Object.freeze([WWN_PARTY_SOURCES.splitForaging])});
}

export function characterStatusProjection(character){
  if(!character||typeof character!=='object')return Object.freeze({status:'MISSING_CHARACTER'});
  const hp=character.hp;
  if(!hp||!Number.isInteger(hp.current)||!Number.isInteger(hp.max)||hp.max<1||hp.current<0||hp.current>hp.max)return Object.freeze({status:'MISSING_HP_EVIDENCE'});
  const maxStrain=character.systemStrain?.max??character.attributes?.Constitution;
  const currentStrain=character.systemStrain?.current??0;
  if(!Number.isInteger(maxStrain)||maxStrain<0||!Number.isInteger(currentStrain)||currentStrain<0||currentStrain>maxStrain)return Object.freeze({status:'MISSING_SYSTEM_STRAIN_EVIDENCE'});
  return Object.freeze({
    status:'OK',
    hp:Object.freeze({current:hp.current,max:hp.max,fill:hp.current/hp.max}),
    systemStrain:Object.freeze({current:currentStrain,max:maxStrain,fill:maxStrain===0?0:currentStrain/maxStrain}),
    sourceRefs:Object.freeze([WWN_PARTY_SOURCES.systemStrain]),
  });
}

export function projectPartyForPlayer(state){
  const roster=characterRosterFromState(state);
  let party=state?.partyState;
  if(!party){
    const ids=Object.keys(roster);if(ids.length<1)return Object.freeze({status:'NO_PARTY'});
    party=createPartyState({memberIds:ids,position:state?.worldState?.position??null});
  }
  const valid=validatePartyState(party,{knownMemberIds:Object.keys(roster)});if(!valid.ok)return Object.freeze({status:'INVALID_PARTY',errors:valid.errors});
  const groups={};
  for(const [groupId,g] of Object.entries(party.groups)){
    groups[groupId]={groupId,label:g.label??'GRUPO',position:clone(g.position),members:g.memberIds.map(id=>{
      const c=roster[id];const bars=characterStatusProjection(c);
      return {characterId:id,name:c?.name??id,level:c?.level??null,classId:c?.classId??c?.class??null,statusBars:bars.status==='OK'?bars:null};
    })};
  }
  return Object.freeze({status:'OK',activeGroupId:party.activeGroupId,groups:Object.freeze(groups)});
}
