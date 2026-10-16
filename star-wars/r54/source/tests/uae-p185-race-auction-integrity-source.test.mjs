import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {jewelGrandPrixLegRequirements,jewelGrandPrixStatus,jewelAuctionStatus} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {validatePublishedAdventureRuntimeState,publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const a=getPublishedAdventureAdapter('jewel-of-yavin');
const go=(w,id)=>a.advance(w,id).world;
const race=()=>['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start'].reduce(go,a.start({}).world);
const auction=()=>['race-aftermath','go-to-museum','attend-cloud-nine'].reduce(go,race());
const leg=(w,i,more={})=>a.adjudicateRaceLeg(w,{actionId:`leg-${i}`,leg:i,success:true,speed:4,...more}).world;
const completeRace=(w=race())=>{for(let i=1;i<=7;i++)w=leg(w,i);return w;};
const edit=(w,fn)=>{const copy=structuredClone(w);fn(copy.publishedAdventure);return copy;};
const bad=(w,pattern=/adulterad|inconsistente|incompatível|inválid|ausente|não corresponde/)=>assert.throws(()=>a.status(w),pattern);
const validateUae=w=>assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(a.meta),w.publishedAdventureRuntime,w.publishedAdventure),true);
for(const [name,field,value] of [
 ['auction bidValue','bidValue',999],['auction active','active',false],['auction base','base',99],['auction name','name','FORGED']]){
 test(`P185 replay rejects forged ${name}`,()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;bad(edit(w,p=>p.operations.auction.bidders.kaltho[field]=value));});
}
test('P185 replay rejects auction winner changed without changing credit total',()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;bad(edit(w,p=>p.operations.auction.highBidderId='elaiza'));});
test('P185 replay rejects auction round order and remaining bidders forged',()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;bad(edit(w,p=>p.operations.auction.roundHistory[0].remaining=['elaiza']));});
test('P185 replay rejects auction completed flag forged',()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;bad(edit(w,p=>p.operations.auction.completed=true));});
test('P185 replay rejects auction receipt amount forged in both receipt and history',()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;bad(edit(w,p=>{p.operations.receipts['round-1'].highBidCredits=999999;p.operations.auction.roundHistory[0].highBidCredits=999999;p.operations.auction.highBidCredits=999999;}));});
test('P185 replay rejects bidder influence moved to wrong round',()=>{let w=auction();w=a.influenceBidder(w,{actionId:'i1',bidderId:'kaltho',success:true,netSuccesses:1}).world;bad(edit(w,p=>p.operations.receipts.i1.round=2));});
test('P185 replay rejects influence pending without receipt',()=>{let w=auction();w=a.influenceBidder(w,{actionId:'i1',bidderId:'kaltho',success:true,netSuccesses:1}).world;bad(edit(w,p=>p.operations.auction.pendingInfluences.push({id:'ghost'})));});
test('P185 failed influence does not auto-award triumph bonus',()=>{const w=a.influenceBidder(auction(),{actionId:'bad',bidderId:'kaltho',success:false,triumphs:1}).world;assert.equal(w.publishedAdventure.operations.receipts.bad.delta,0);});
test('P185 started auction cannot be skipped by advancing to bank',()=>{let w=auction();w=a.adjudicateAuctionRound(w,{actionId:'round-1'}).world;assert.throws(()=>go(w,'bank-operation'),/Conclua o leilão/);});
test('P185 legacy auction scene can advance without creating phantom bids',()=>{const w=go(auction(),'bank-operation');assert.equal(w.publishedAdventure.operations,undefined);});
test('P185 started Grand Prix cannot skip seven legs',()=>{let w=race();w=leg(w,1);assert.throws(()=>go(w,'race-aftermath'),/Conclua os sete trechos/);});
test('P185 completed Grand Prix requires classified place to advance',()=>{let w=completeRace();assert.throws(()=>go(w,'race-aftermath'),/classificação/);w=a.adjudicateRacePlacement(w,{actionId:'placement',place:1,competitorsPlaceValues:[15,20]}).world;assert.equal(go(w,'race-aftermath').publishedAdventure.currentNodeId,'grand-prix-aftermath');});
test('P185 legacy Grand Prix without operation remains playable',()=>assert.equal(go(race(),'race-aftermath').publishedAdventure.currentNodeId,'grand-prix-aftermath'));
test('P185 source hazard leg 4 has two setbacks and tibannuck',()=>assert.deepEqual(jewelGrandPrixLegRequirements(4),{leg:4,name:'tibannuck-feeding-ground',terrainSetbacks:2,missedBuoySetbacks:0,source:'SWE09 The Jewel of Yavin, Grand Prix pp.50-53',special:'tibannuck'}));
test('P185 source hazard leg 5 has one setback and beldon',()=>{const h=jewelGrandPrixLegRequirements(5);assert.equal(h.terrainSetbacks,1);assert.equal(h.special,'beldon');});
test('P185 missed buoy requires next-check setback and survives three reopens',()=>{let w=race();w=leg(w,1,{success:false,missedBuoy:true});for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(jewelGrandPrixLegRequirements(2,jewelGrandPrixStatus(w)).missedBuoySetbacks,1);validateUae(w);}w=leg(w,2);assert.equal(jewelGrandPrixStatus(w).legs[1].appliedSetbacks,1);assert.equal(jewelGrandPrixLegRequirements(3,jewelGrandPrixStatus(w)).missedBuoySetbacks,0);});
test('P185 tibannuck threat causes one system strain',()=>{let w=race();for(let i=1;i<=3;i++)w=leg(w,i);w=leg(w,4,{hazard:{threats:2,despairs:0,criticalHit:false}});assert.equal(jewelGrandPrixStatus(w).systemStrain,1);assert.equal(jewelGrandPrixStatus(w).legs[3].terrainSetbacks,2);validateUae(w);});
test('P185 tibannuck despair records critical modifier minus twenty',()=>{let w=race();for(let i=1;i<=3;i++)w=leg(w,i);w=leg(w,4,{criticalCollision:true,hazard:{threats:0,despairs:1,criticalHit:true}});assert.equal(jewelGrandPrixStatus(w).legs[3].criticalModifier,-20);assert.equal(jewelGrandPrixStatus(w).criticalCollisions,1);});
test('P185 beldon three threats records critical without modifier',()=>{let w=race();for(let i=1;i<=4;i++)w=leg(w,i);w=leg(w,5,{criticalCollision:true,hazard:{threats:3,despairs:0,criticalHit:true}});assert.equal(jewelGrandPrixStatus(w).legs[4].criticalModifier,0);assert.equal(jewelGrandPrixStatus(w).legs[4].terrainSetbacks,1);});
test('P185 critical hazard without critical collision receipt is rejected',()=>{let w=race();for(let i=1;i<=3;i++)w=leg(w,i);assert.throws(()=>leg(w,4,{hazard:{threats:0,despairs:1,criticalHit:true}}),/registro de colisão/);});
test('P185 hazard cannot be forged for safe leg',()=>assert.throws(()=>leg(race(),1,{hazard:{threats:1}}),/não prevista/));
test('P185 forged hazard strain fails replay even if total changed',()=>{let w=race();for(let i=1;i<=3;i++)w=leg(w,i);w=leg(w,4,{hazard:{threats:2,despairs:0,criticalHit:false}});bad(edit(w,p=>{p.operations.receipts['leg-4'].hazardStrain=8;p.operations.race.legs[3].hazardStrain=8;}));});
test('P185 forged missed buoy next setback fails replay',()=>{let w=race();w=leg(w,1,{success:false,missedBuoy:true});w=leg(w,2);bad(edit(w,p=>{p.operations.receipts['leg-2'].appliedSetbacks=0;p.operations.race.legs[1].appliedSetbacks=0;}));});
test('P185 forged race victory flag fails replay',()=>{let w=completeRace();w=a.adjudicateRacePlacement(w,{actionId:'placement',place:2,competitorsPlaceValues:[30,20]}).world;bad(edit(w,p=>p.flags.grandPrixWon=true));});
test('P185 corrupted save rejects scene advance without mutating original',()=>{let w=race();w=leg(w,1);const forged=edit(w,p=>p.operations.race.legs[0].speed=6);const snapshot=structuredClone(forged);assert.throws(()=>go(forged,'race-aftermath'));assert.deepEqual(forged,snapshot);});
test('P185 original P184 auction and race saves without hazard fields remain valid',()=>{let w=auction();w=a.influenceBidder(w,{actionId:'i1',bidderId:'kaltho',success:true,netSuccesses:2}).world;w=a.adjudicateAuctionRound(w,{actionId:'r1'}).world;assert.equal(a.status(w).active,true);validateUae(w);});

test('P185 new bidder receipt stores verifiable successes and triumphs',()=>{let w=auction();w=a.influenceBidder(w,{actionId:'i1',bidderId:'elaiza',success:true,netSuccesses:2,triumphs:1}).world;const r=w.publishedAdventure.operations.receipts.i1;assert.equal(r.delta,5);assert.equal(r.netSuccesses,2);assert.equal(r.triumphs,1);bad(edit(w,p=>{p.operations.receipts.i1.delta=9;p.operations.auction.pendingInfluences[0].delta=9;}));});
test('P185 Android published status exposes terrain and missed buoy setback guidance',()=>{let w=race();assert.equal(a.status(w).raceNextLegRequirements.terrainSetbacks,0);w=leg(w,1,{success:false,missedBuoy:true});assert.equal(a.status(w).raceNextLegRequirements.missedBuoySetbacks,1);});
