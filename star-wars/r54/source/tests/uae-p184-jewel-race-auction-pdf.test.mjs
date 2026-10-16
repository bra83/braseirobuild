import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {selectLongArmTrexCarryover,chooseLongArmOldLylekCrew,chooseLongArmOldLylekRoute,proceedLongArmOldLylekBluff,recordLongArmOldLylekRuse,chooseLongArmOldLylekTactics} from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import {validatePublishedAdventureRuntimeState,publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {jewelAuctionStatus,jewelGrandPrixStatus} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
const a=getPublishedAdventureAdapter('jewel-of-yavin');
const arm=getPublishedAdventureAdapter('long-arm-of-the-hutt');
function lylekReadyWorld(size=1){
 let w=arm.start({}).world;for(const choice of ['dock-ryloth','help-new-meen','travel-lylek-den'])w=arm.advance(w,choice).world;
 w=selectLongArmTrexCarryover(w,{history:'confirmed_dead'}).world;
 const crewPcIds=Array.from({length:size},(_,i)=>`p184-pc-${i+1}`),rosterEntries=crewPcIds.map((id,i)=>({id,character:{name:`PC ${i+1}`}}));
 w=chooseLongArmOldLylekCrew(w,{crewPcIds,rosterEntries,playerCount:size}).world;
 w=chooseLongArmOldLylekRoute(w,{slowDown:true}).world;
 w=arm.adjudicateCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:0}}).world;
 w=proceedLongArmOldLylekBluff(w).world;
 w=recordLongArmOldLylekRuse(w,{recognizedByHunters:false,oskaraPresent:false}).world;
 w=chooseLongArmOldLylekTactics(w,{approach:'frontal',frontalOutcome:'fighting_retreat',lylekBranch:'stay_passive'}).world;
 w.publishedAdventure.flags.confirmedPartySize=size;return w;
}
const go=(w,id)=>a.advance(w,id).world;
const raceWorld=()=>['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start'].reduce(go,a.start({}).world);
const auctionWorld=()=>['race-aftermath','go-to-museum','attend-cloud-nine'].reduce(go,raceWorld());
const assertUae=w=>assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(a.meta),w.publishedAdventureRuntime,w.publishedAdventure),true);
test('P184 PDF SWE09: seven ordered Grand Prix legs accumulate speed into Place Value',()=>{
 let w=raceWorld();for(let leg=1;leg<=7;leg++)w=a.adjudicateRaceLeg(w,{actionId:`r${leg}`,leg,success:true,speed:4}).world;
 const r=jewelGrandPrixStatus(w);assert.equal(r.completed,true);assert.equal(r.placeValue,28);assert.equal(r.legs.length,7);assertUae(w);
});
test('P184 race rejects skipped leg, forged actionId and repeat without mutating input',()=>{
 const w=raceWorld(),before=structuredClone(w);
 assert.throws(()=>a.adjudicateRaceLeg(w,{actionId:'r2',leg:2,success:true,speed:4}),/ordem/);
 assert.throws(()=>a.adjudicateRaceLeg(w,{actionId:'bad id',leg:1,success:true,speed:4}),/actionId/);
 assert.deepEqual(w,before);
 const x=a.adjudicateRaceLeg(w,{actionId:'r1',leg:1,success:true,speed:4}).world;
 assert.throws(()=>a.adjudicateRaceLeg(x,{actionId:'r1',leg:2,success:true,speed:4}),/registrada/);
 assert.throws(()=>a.adjudicateRaceLeg(x,{actionId:'r2',leg:2,success:true,speed:10}),/Velocidade/);
});
test('P184 race missed buoy, strain and collision persist, no unearned victory',()=>{
 let w=raceWorld();w=a.adjudicateRaceLeg(w,{actionId:'r1',leg:1,success:false,speed:3,missedBuoy:true,systemStrainDelta:2,criticalCollision:true,placeAdjustment:-3}).world;
 assert.equal(jewelGrandPrixStatus(w).missedBuoys,1);assert.equal(jewelGrandPrixStatus(w).systemStrain,2);
 assert.equal(jewelGrandPrixStatus(w).placeValue,0);assert.equal(w.publishedAdventure.flags.grandPrixWon,undefined);
 assert.throws(()=>a.adjudicateRacePlacement(w,{actionId:'place',place:1,competitorsPlaceValues:[20]}),/sete trechos/);
});
test('P184 race placement must match rival Place Values, with tie GM-adjudicated',()=>{
 let w=raceWorld();for(let i=1;i<=7;i++)w=a.adjudicateRaceLeg(w,{actionId:`r${i}`,leg:i,success:true,speed:4}).world;
 assert.throws(()=>a.adjudicateRacePlacement(w,{actionId:'place',place:1,competitorsPlaceValues:[30,20]}),/incompatível/);
 w=a.adjudicateRacePlacement(w,{actionId:'place',place:2,competitorsPlaceValues:[30,20]}).world;
 assert.equal(w.publishedAdventure.flags.grandPrixWon,false);assert.equal(jewelGrandPrixStatus(w).placement,2);assertUae(w);
});
test('P184 auction official baseline Kaltho 200k after ten rounds without PC interference',()=>{
 let w=auctionWorld();for(let i=1;i<=10;i++)w=a.adjudicateAuctionRound(w,{actionId:`auction-${i}`}).world;
 const s=jewelAuctionStatus(w);assert.equal(s.completed,true);assert.equal(s.round,10);assert.equal(s.highBidCredits,200000);assert.equal(s.highBidderId,'kaltho');
 assert.deepEqual(s.roundHistory.map(x=>x.highBidCredits),Array.from({length:10},(_,i)=>(i+1)*20000));assertUae(w);
});
test('P184 auction successful influence adds 10k per affected round, successes and triumph to Bid Value',()=>{
 let w=auctionWorld();w=a.influenceBidder(w,{actionId:'bid-1',bidderId:'elaiza',success:true,netSuccesses:4,triumphs:1,favorableImpression:true}).world;
 assert.equal(jewelAuctionStatus(w).pendingInfluences[0].delta,7);
 w=a.adjudicateAuctionRound(w,{actionId:'auction-1'}).world;
 assert.equal(jewelAuctionStatus(w).highBidCredits,30000);assert.equal(jewelAuctionStatus(w).pendingInfluences.length,0);assertUae(w);
});
test('P184 auction rejects unknown bidder, duplicate receipt, invalid result and out-of-scene action',()=>{
 const w=auctionWorld(),before=structuredClone(w);
 assert.throws(()=>a.influenceBidder(w,{actionId:'bid',bidderId:'admin',success:true,netSuccesses:2}),/Licitante/);
 assert.throws(()=>a.influenceBidder(w,{actionId:'bid',bidderId:'kaltho',success:'false'}),/inválidos/);
 assert.throws(()=>a.influenceBidder(w,{actionId:'bid',bidderId:'kaltho',success:true,netSuccesses:0}),/sucesso/);
 assert.deepEqual(w,before);
 const x=a.influenceBidder(w,{actionId:'bid',bidderId:'kaltho',success:false}).world;
 assert.throws(()=>a.influenceBidder(x,{actionId:'bid',bidderId:'kaltho',success:false}),/registrada/);
 assert.throws(()=>a.adjudicateAuctionRound(raceWorld(),{actionId:'r1'}),/Cloud Nine/);
});
test('P184 auction fail does not increase bid, no automatic credit payout',()=>{
 let w=auctionWorld();w=a.influenceBidder(w,{actionId:'bad-bid',bidderId:'kaltho',success:false}).world;
 w=a.adjudicateAuctionRound(w,{actionId:'auction-1'}).world;
 assert.equal(jewelAuctionStatus(w).highBidCredits,20000);assert.equal(w.credits,undefined);assert.equal(w.publishedAdventure.flags.bankDiversionSecured,undefined);
});
test('P184 new race/auction receipts survive three reopenings with canonical UAE state',()=>{
 let w=auctionWorld();w=a.influenceBidder(w,{actionId:'bid',bidderId:'kaltho',success:true,netSuccesses:1}).world;
 w=a.adjudicateAuctionRound(w,{actionId:'auction-1'}).world;
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(jewelAuctionStatus(w).round,1);assert.equal(jewelAuctionStatus(w).highBidCredits,30000);assertUae(w);}
});
test('P184 legacy Jewel progress migrates without creating phantom race results or bids',()=>{
 let w=raceWorld();w.publishedAdventureRuntime=undefined;w=go(w,'race-aftermath');
 assert.equal(jewelGrandPrixStatus(w),null);assert.equal(w.publishedAdventure.flags.grandPrixWon,undefined);assertUae(w);
});

test('P184 receipt audit rejects edited race Place Value and preserves source state',()=>{
 let w=raceWorld();w=a.adjudicateRaceLeg(w,{actionId:'r1',leg:1,success:true,speed:4}).world;
 const forged=structuredClone(w);forged.publishedAdventure.operations.race.placeValue=500;
 assert.throws(()=>a.status(forged),/inconsistente/);
 assert.throws(()=>a.adjudicateRaceLeg(forged,{actionId:'r2',leg:2,success:true,speed:4}),/inconsistente/);
 assert.equal(jewelGrandPrixStatus(w).placeValue,4);
});
test('P184 receipt audit rejects inflated auction credits and ghost influence',()=>{
 let w=auctionWorld();w=a.adjudicateAuctionRound(w,{actionId:'a1'}).world;
 const forged=structuredClone(w);forged.publishedAdventure.operations.auction.highBidCredits=10000000;
 assert.throws(()=>a.status(forged),/inconsistente/);
 const forged2=structuredClone(w);forged2.publishedAdventure.operations.auction.roundHistory[0].influenceReceipts=['ghost'];
 assert.throws(()=>a.status(forged2),/adulterad|inconsistente/);
 assert.equal(jewelAuctionStatus(w).highBidCredits,20000);
});
test('P184 reserved receipt identifiers cannot mutate Object prototype',()=>{
 assert.throws(()=>a.adjudicateAuctionRound(auctionWorld(),{actionId:'__proto__'}),/actionId/);
 assert.equal(({}).kind,undefined);
});

test('P184 SWE02: old lylek den creates one source-checked hunter per PC including Gand',async()=>{
 let w=lylekReadyWorld(4);
 const spec=arm.encounterSpec(w,'lylek-den-bounty-hunters');
 assert.equal(spec.partySize,4);assert.equal(spec.requiresPartySizeConfirmation,false);
 assert.deepEqual(spec.adversaries.map(x=>[x.profileKey,x.count]),[['laoth_gand_bounty_hunter',1],['laoth_bounty_hunter',1],['laoth_bounty_hunter',1],['laoth_bounty_hunter',1]]);
 const {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES:ADVERSARY_PROFILES}=await import('../app/src/main/assets/vtt/js/rules/adversaries.mjs');
 assert.equal(ADVERSARY_PROFILES.laoth_bounty_hunter.soak,5);
 assert.equal(ADVERSARY_PROFILES.laoth_gand_bounty_hunter.weapons[0].damage,9);
 assert.equal(ADVERSARY_PROFILES.laoth_bounty_hunter.source.page,16);
});
test('P184 SWE02: hunters cannot be marked defeated without combat, and never defeat Teemo',()=>{
 let w=lylekReadyWorld(1);
 assert.throws(()=>arm.recordEncounterOutcome(w,{encounterId:'lylek-den-bounty-hunters',outcome:'adversaries_defeated'}),/não iniciado/);
 w=arm.markEncounterActive(w,'lylek-den-bounty-hunters');
 const next=arm.recordEncounterOutcome(w,{encounterId:'lylek-den-bounty-hunters',outcome:'adversaries_defeated',adversaries:[{id:'hunter-1',profileKey:'laoth_gand_bounty_hunter',status:'incapacitated'}]}).world;
 assert.equal(next.publishedAdventure.flags.lylekHuntersResolved,true);
 assert.equal(next.publishedAdventure.flags.teemoDefeated,undefined);
 assert.throws(()=>arm.recordEncounterOutcome(next,{encounterId:'lylek-den-bounty-hunters',outcome:'adversaries_defeated',adversaries:[{id:'hunter-1',profileKey:'laoth_gand_bounty_hunter',status:'incapacitated'}]}),/incompatível/);
 assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(arm.meta),next.publishedAdventureRuntime,next.publishedAdventure),true);
});
test('P184 Android UI and controller expose four adjudicated operations',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const {GameController}=await import('../app/src/main/assets/vtt/js/core/controller.mjs');
 assert.equal(typeof BraseiroUiBridge.prototype.adjudicateJewelOperation,'function');
 assert.equal(typeof GameController.prototype.adjudicateJewelOperation,'function');
 const {readFileSync}=await import('node:fs');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.ok(bundle.includes('adjudicateJewelOperation'));
 assert.ok(bundle.includes('jewelOfYavinOperations'));
});
