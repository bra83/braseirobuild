import { projectNpcForPlayer } from './npc-registry.js';
import { projectPartyForPlayer } from './party-state.js';

export function playerWorldProjection(state){
  if(!state||typeof state!=='object')throw new TypeError('state required');
  const content=state.contentState?.public??null;
  return Object.freeze({
    campaignId:state.campaignId,
    revision:state.revision,
    worldState:structuredClone(state.worldState??{}),
    encounterState:structuredClone(state.encounterState??null),
    party:projectPartyForPlayer(state),
    contentState:structuredClone(content),
    npcs:Object.fromEntries(Object.entries(state.npcState??{}).map(([id,npc])=>[id,projectNpcForPlayer(npc)])),
  });
}
