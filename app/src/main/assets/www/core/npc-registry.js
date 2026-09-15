export function validateNpcRecord(npc){
  const errors=[]; if(!npc||typeof npc!=='object')return{ok:false,errors:['npc required']};
  if(typeof npc.npcId!=='string'||!npc.npcId)errors.push('npcId required');
  if(typeof npc.name!=='string'||!npc.name)errors.push('name required');
  if(npc.publicFacts!==undefined&&!Array.isArray(npc.publicFacts))errors.push('publicFacts must be array');
  if(npc.gmSecrets!==undefined&&!Array.isArray(npc.gmSecrets))errors.push('gmSecrets must be array');
  if(npc.memories!==undefined&&!Array.isArray(npc.memories))errors.push('memories must be array');
  return{ok:errors.length===0,errors};
}

export function projectNpcForPlayer(npc){
  const v=validateNpcRecord(npc); if(!v.ok)throw new Error(v.errors.join('; '));
  return Object.freeze({npcId:npc.npcId,name:npc.name,location:npc.location??null,publicFacts:structuredClone(npc.publicFacts??[]),visibleStatus:npc.visibleStatus??null,memories:structuredClone((npc.memories??[]).filter(m=>m?.visibility==='PLAYER_KNOWN'))});
}

export function projectNpcForGm(npc){const v=validateNpcRecord(npc);if(!v.ok)throw new Error(v.errors.join('; '));return structuredClone(npc);}

export function appendNpcMemory(npc,{eventId,summary,visibility='GM_ONLY'}){
  const v=validateNpcRecord(npc);if(!v.ok)throw new Error(v.errors.join('; '));
  if(typeof eventId!=='string'||!eventId||typeof summary!=='string'||!summary)throw new Error('eventId and summary required');
  if(!['GM_ONLY','PLAYER_KNOWN'].includes(visibility))throw new Error('invalid memory visibility');
  const next=structuredClone(npc); next.memories=[...(next.memories??[]),{eventId,summary,visibility}]; return next;
}
