import {chooseLongArmOldLylekRoute,approachLongArmOldLylekOnFoot} from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';

/** A downstream test fixture records a valid, source-shaped retreat from the prior hunter scene. */
export function resolveOldLylekRetreat(adapter,world,partySize=null){
 const next=structuredClone(world),p=next.publishedAdventure;
 const configured=Number(p?.flags?.confirmedPartySize);
 const size=Number.isInteger(partySize)&&partySize>=1&&partySize<=8?partySize:
   Number.isInteger(configured)&&configured>=1&&configured<=8?configured:1;
 p.flags={...(p.flags||{}),confirmedPartySize:size};
 let w=chooseLongArmOldLylekRoute(next,{slowDown:true}).world;
 w=approachLongArmOldLylekOnFoot(w).world;
 const encounterId='lylek-den-bounty-hunters';w=adapter.markEncounterActive(w,encounterId);
 const spec=adapter.encounterSpec(w,encounterId),adversaries=[];
 for(const entry of spec.adversaries)for(let i=0;i<entry.count;i++)adversaries.push({
   id:`fixture-hunter-${adversaries.length+1}`,profileKey:entry.profileKey,status:'active'
 });
 return adapter.recordEncounterOutcome(w,{encounterId,outcome:'escaped',adversaries,combatLog:[]}).world;
}
