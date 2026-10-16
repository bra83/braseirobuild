import {selectLongArmTrexCarryover,chooseLongArmOldLylekCrew,chooseLongArmOldLylekRoute,approachLongArmOldLylekOnFoot,recordLongArmOldLylekRuse,chooseLongArmOldLylekTactics} from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import {recordNabatMorningResponse} from '../app/src/main/assets/vtt/js/adventure/swe02NabatP240.mjs';

/** Printed p.14 Nabat night-to-morning sequence, with a fixture PC response before departure. */
export function advanceNabatMorning(adapter,world){
 let w=adapter.advance(world,'travel-lylek-den').world;
 w=recordNabatMorningResponse(w,{id:'fixture-pc',name:'Fixture PC'},{response:'uncertain'}).world;
 return adapter.advance(w,'nabat-depart-for-mines').world;
}
export function advanceNabatMorningController(game){
 game.advancePublishedAdventure('travel-lylek-den');
 game.respondSWE02NabatMorning({response:'uncertain'});
 game.advancePublishedAdventure('nabat-depart-for-mines');
 return game.state.world;
}

/** A downstream test fixture records a valid, source-shaped retreat from the prior hunter scene. */
export function resolveOldLylekRetreat(adapter,world,partySize=null){
 const next=structuredClone(world),p=next.publishedAdventure;
 const configured=Number(p?.flags?.confirmedPartySize);
 const size=Number.isInteger(partySize)&&partySize>=1&&partySize<=8?partySize:
   Number.isInteger(configured)&&configured>=1&&configured<=8?configured:1;
 p.flags={...(p.flags||{}),confirmedPartySize:size};
 let selected=selectLongArmTrexCarryover(next,{history:'confirmed_dead'}).world;
 selected=chooseLongArmOldLylekCrew(selected,{crewPcIds:Array.from({length:size},(_,i)=>`fixture-pc-${i+1}`),rosterEntries:Array.from({length:size},(_,i)=>({id:`fixture-pc-${i+1}`,character:{name:`Fixture PC ${i+1}`}})),playerCount:size}).world;
 let w=chooseLongArmOldLylekRoute(selected,{slowDown:true}).world;
 w=approachLongArmOldLylekOnFoot(w).world;
 w=recordLongArmOldLylekRuse(w,{recognizedByHunters:false,oskaraPresent:false}).world;
 w=chooseLongArmOldLylekTactics(w,{approach:'frontal',frontalOutcome:'fighting_retreat',lylekBranch:'stay_passive'}).world;
 const encounterId='lylek-den-bounty-hunters';w=adapter.markEncounterActive(w,encounterId);
 const spec=adapter.encounterSpec(w,encounterId),adversaries=[];
 for(const entry of spec.adversaries)for(let i=0;i<entry.count;i++)adversaries.push({
   id:`fixture-hunter-${adversaries.length+1}`,profileKey:entry.profileKey,status:'active'
 });
 return adapter.recordEncounterOutcome(w,{encounterId,outcome:'escaped',adversaries,combatLog:[]}).world;
}
