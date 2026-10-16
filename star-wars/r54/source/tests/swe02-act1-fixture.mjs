import {selectLongArmTrexCarryover,chooseLongArmOldLylekCrew,chooseLongArmOldLylekRoute,approachLongArmOldLylekOnFoot,recordLongArmOldLylekRuse,chooseLongArmOldLylekTactics} from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import {recordNabatMorningResponse} from '../app/src/main/assets/vtt/js/adventure/swe02NabatP240.mjs';

/** Complete verified ship checks, locate B’ura and use a source-backed safe landing. */
export function finishNabatArrival(adapter,world){
 let w=world;
 for(const checkIndex of [3,5,0])w=adapter.adjudicateCheck(w,{checkIndex,result:{success:true,netSuccess:1,netAdvantage:0}}).world;
 for(const choice of ['ship-find-bura','ship-depart-verified','ship-safe-landing','nabat-meet-nyn'])w=adapter.advance(w,choice).world;
 return w;
}
/** Bring an adapter fixture through the verified Krayt Fang inspection and landing route. */
export function arriveAtNabat(adapter,world){
 return finishNabatArrival(adapter,adapter.advance(world,'inspect-krayt-fang').world);
}
/** Controller path companion to arriveAtNabat, with a real Krayt Fang and persisted arrival. */
export function arriveAtNabatController(game){
 game.advancePublishedAdventure('inspect-krayt-fang');
 for(const checkIndex of [3,5,0])game.resolvePublishedAdventureCheck(checkIndex,{rng:()=>.2});
 for(const choice of ['ship-find-bura','ship-depart-verified','ship-safe-landing','nabat-meet-nyn'])game.advancePublishedAdventure(choice);
 return game.state.world;
}
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
