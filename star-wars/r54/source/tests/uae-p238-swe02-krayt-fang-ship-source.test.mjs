import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {longArmShipSmellSetback} from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const start=()=>a.start({}).world,step=(w,id)=>a.advance(w,id).world;
const ship=()=>step(start(),'inspect-krayt-fang');
const roll=(w,index,success)=>a.adjudicateCheck(w,{checkIndex:index,result:{success,netAdvantage:0}}).world;
const reopen=w=>JSON.parse(JSON.stringify(w));
const has=(w,index)=>a.currentChecks(w).some(x=>x.index===index);
test('P238 SWE02 p7-8: navigation cannot be claimed without actual check and Bura discovery',()=>{
 let w=ship();assert.equal(a.currentChecks(w).find(x=>x.index===0).pool.challenge,1);
 assert.throws(()=>step(w,'ship-depart-verified'),/inválida/);
 w=roll(w,0,true);assert.equal(w.publishedAdventure.flags.shipCoursePlotted,true);
 assert.throws(()=>step(w,'ship-depart-verified'),/inválida/);
 w=step(w,'ship-find-bura');assert.equal(w.publishedAdventure.flags.shipBuraFound,true);
 w=step(w,'ship-depart-verified');assert.equal(w.publishedAdventure.flags.shipThwheekInterceptionRequired,true);
 for(let i=0;i<3;i++){w=reopen(w);a.status(w)}
});
test('P238 p8: failed Astrogation still plots slower route and forces Thwheek even if tracker slowed',()=>{
 let w=ship();w=roll(w,3,true);w=roll(w,5,true);w=roll(w,0,false);w=step(w,'ship-find-bura');w=step(w,'ship-depart-verified');
 assert.equal(w.publishedAdventure.flags.shipCourseNonOptimal,true);
 assert.equal(w.publishedAdventure.flags.shipThwheekInterceptionRequired,true);
});
test('P238 p8: optimal course and slowed tracker suppress pursuit, not future encounters',()=>{
 let w=ship();w=roll(w,3,true);w=roll(w,5,true);w=roll(w,0,true);w=step(w,'ship-find-bura');w=step(w,'ship-depart-verified');
 assert.equal(w.publishedAdventure.flags.shipThwheekInterceptionRequired,false);
});
test('P238 p7-8: both noises upgrade one difficulty; one disabled restores pool',()=>{
 let w=ship();assert.deepEqual(a.currentChecks(w).find(x=>x.index===2).pool,{difficulty:1,challenge:1,setback:0,boost:0});
 w=roll(w,1,true);assert.deepEqual(a.currentChecks(w).find(x=>x.index===2).pool,{difficulty:2,challenge:0,setback:0,boost:0});
 assert.equal(has(w,1),false);assert.throws(()=>roll(w,1,true),/inválido/);
});
test('P238 p8: transponder cannot be switched off in flight or removed before landing',()=>{
 let w=ship();assert.equal(has(w,8),false);assert.equal(has(w,5),false);
 w=roll(w,3,true);assert.equal(has(w,5),true);w=roll(w,5,true);
 assert.equal(w.publishedAdventure.flags.shipTrackerRemoved,undefined);
 w=step(w,'finish-ship-inspection');assert.equal(w.publishedAdventure.flags.shipSourceUnverified,true);
 w=step(w,'ship-inspect-hull');assert.equal(has(w,8),true);
 w=roll(w,8,true);assert.equal(w.publishedAdventure.flags.shipTrackerRemoved,true);
 for(let i=0;i<3;i++){w=reopen(w);a.status(w)}
});
test('P238 p9: smell penalty for named organic heroes, Lowhhrick and droid exception',()=>{
 let w=ship();assert.equal(longArmShipSmellSetback(w,'Oskara',1),1);
 assert.equal(longArmShipSmellSetback(w,'Lowhhrick',1),2);
 assert.equal(longArmShipSmellSetback(w,'41-VEX',1),0);
 assert.equal(longArmShipSmellSetback(w,'Oskara',6),0);
 assert.equal(longArmShipSmellSetback(w,'Unknown PC',1),0);
 w=roll(w,6,true);w=roll(w,7,true);assert.equal(longArmShipSmellSetback(w,'Lowhhrick',1),0);
});
test('P238 p9: jettison requires actual successful Perception and Computers',()=>{
 let w=ship();assert.equal(has(w,7),false);w=roll(w,6,false);assert.equal(has(w,7),false);
 w=roll(w,6,true);assert.equal(has(w,7),true);w=roll(w,7,false);assert.equal(w.publishedAdventure.flags.shipPeltsJettisoned,undefined);
 w=roll(w,7,true);assert.equal(w.publishedAdventure.flags.shipPeltsJettisoned,true);
});
test('P238 p9-11: knowledge and droid part checks have source and cannot silently roll unknown skill',()=>{
 let w=ship();assert.equal(a.currentChecks(w).some(x=>x.index===9),false);w=a.discoverShipRoom(w,'number-three-hold').world;
 const knowledge=a.currentChecks(w).find(x=>x.index===9),parts=a.currentChecks(w).find(x=>x.index===10);
 assert.equal(knowledge.pool.difficulty,2);assert.equal(knowledge.pool.challenge,1);
 assert.equal(knowledge.skillKey,null);assert.equal(knowledge.requiresGMSourceVerification,true);
 assert.equal(parts.skillKey,'mecanica');assert.equal(parts.source.printedPage,11);
});
test('P238: forged ship flags, forged Bura and forged intercept outcome fail closed',()=>{
 let w=ship();let f=reopen(w);f.publishedAdventure.flags.shipTrackerRemoved=true;assert.throws(()=>a.status(f),/sem recibo/);
 f=reopen(w);f.publishedAdventure.flags.shipBuraFound=true;assert.throws(()=>a.status(f),/descoberta documentada/);
 w=roll(w,0,true);w=step(w,'ship-find-bura');w=step(w,'ship-depart-verified');
 f=reopen(w);f.publishedAdventure.flags.shipThwheekInterceptionRequired=false;assert.throws(()=>a.status(f),/interceptação adulterada/);
});
test('P238: legacy route remains loadable but is explicitly uncertified',()=>{
 const w=step(start(),'dock-ryloth');assert.equal(w.publishedAdventure.flags.shipSourceUnverified,true);a.status(reopen(w));
});
class Repo{constructor(){this.state=createCampaign('P238');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
test('P238 real controller: skill pool, roll receipt and three saved reopens',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 const specs=a.currentChecks(g.state.world);assert.equal(specs.find(x=>x.index===0).pool.challenge,1);
 const r=g.resolvePublishedAdventureCheck(0,{rng:()=>.9});assert.equal(r.adjudication.source.printedPage,8);
 for(let i=0;i<3;i++){const h=new GameController(repo);assert.equal(h.state.world.publishedAdventure.shipChecks.at(-1).key,'course');a.status(h.state.world)}
});
test('P238 real controller: GM selects an EotE Knowledge specialty and the roll receipt survives reopen',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:1,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 g.discoverSWE02ShipRoom('number-three-hold');
 assert.throws(()=>g.resolvePublishedAdventureCheck(9,{rng:()=>.9}),/Mestre deve escolher/);
 assert.throws(()=>g.resolvePublishedAdventureCheck(9,{knowledgeSkillKey:'estrategia_militar',rng:()=>.9}),/Mestre deve escolher/);
 const result=g.resolvePublishedAdventureCheck(9,{knowledgeSkillKey:'xenologia',rng:()=>.9});
 assert.equal(result.adjudication.source.printedPage,9);
 const receipt=repo.state.world.publishedAdventure.shipChecks.at(-1);
 assert.equal(receipt.key,'identifyChitin');assert.equal(receipt.skillKey,'xenologia');
 assert.deepEqual(receipt.rulesSource,{book:'Edge of the Empire Core Rulebook',printedPage:122});
 assert.equal(receipt.success,false);
 for(let i=0;i<3;i++){
  const h=new GameController(repo),p=h.state.world.publishedAdventure;
  assert.equal(p.shipChecks.at(-1).skillKey,'xenologia');assert.equal(p.flags.shipChitinIdentified,undefined);a.status(h.state.world);
 }
});
test('P238 real controller: observations, cargo, Bura conversation and six-hour transit survive three reopens',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 g.recordSWE02ShipObservation('observe-alert-icons');g.recordSWE02ShipObservation('press-cage-icon',{oskaraPresent:true});
 g.discoverSWE02ShipRoom('number-two-hold');g.advancePublishedAdventure('ship-find-bura');
 g.recordSWE02BuraConversation({topics:['shipHelp','mineReward'],oskaraPresent:true,oskaraSharedHistory:true});
 g.resolvePublishedAdventureCheck(0,{rng:()=>.9});g.advancePublishedAdventure('ship-depart-verified');
 for(let i=0;i<3;i++){
   const h=new GameController(repo),p=h.state.world.publishedAdventure;
   assert.equal(p.flags.shipAlertIconsObserved,true);assert.equal(p.flags.shipBuraFreed,true);
   assert.equal(p.shipSequence.discoveredRooms.includes('number-two-hold'),true);
   assert.equal(p.shipSequence.cargoReceipts[0].ownership,'DISCOVERED_NOT_TRANSFERRED');
   assert.equal(p.shipSequence.buraConversation.facts.some(x=>x.id==='mineReward'),true);
   assert.deepEqual(p.shipSequence.transit,{elapsedHours:6,elapsedHoursIsMinimum:true,durationStatus:'LONGER_THAN_OPTIMAL_UNQUANTIFIED',extraDurationStatus:'GM_TIME_ADJUDICATION_REQUIRED',uneventful:true,restAndRecuperationAvailable:true,completed:true,source:{productCode:'SWE02',printedPage:11}});
   a.status(h.state.world);
 }
});

test('P238 controller: cargo transfers enter the selected roster PC inventory and survive reopen exactly once',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:1,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 g.discoverSWE02ShipRoom('number-two-hold');g.discoverSWE02ShipRoom('galley');
 const receiver=structuredClone(g.state.player.character);receiver.name=g.state.player.character.name;
 g.registerPartyCharacter({character:receiver,id:'pc:receiver'});
 let options=g.swe02ShipCargoTransferOptions();assert.equal(options.items.find(x=>x.itemId==='medpac').availableQuantity,1);
 g.transferSWE02ShipCargo({roomId:'number-two-hold',itemId:'medpac',quantity:1,characterId:'pc:receiver'});
 g.transferSWE02ShipCargo({roomId:'galley',itemId:'ration-packs',quantity:3,characterId:'pc:receiver'});
 const owned=g.state.player.roster.entries.find(x=>x.id==='pc:receiver').character.gear;
 assert.equal(owned.find(x=>x.id==='medpac').quantity,1);
 assert.equal(owned.find(x=>x.id==='rations').quantity,3);
 assert.equal(owned.find(x=>x.id==='medpac').source.printedPage,177);
 assert.equal(g.state.player.character.gear.some(x=>x.id==='medpac'),false);
 options=g.swe02ShipCargoTransferOptions();assert.equal(options.items.some(x=>x.itemId==='medpac'),false);
 assert.equal(options.items.find(x=>x.itemId==='ration-packs').availableQuantity,3);
 const before=repo.saves;
 assert.throws(()=>g.transferSWE02ShipCargo({roomId:'number-two-hold',itemId:'medpac',quantity:1,characterId:'pc:receiver'}),/excede o item/);
 assert.equal(repo.saves,before);
 for(let i=0;i<3;i++){
  const h=new GameController(repo),p=h.state.world.publishedAdventure,pc=h.state.player.roster.entries.find(x=>x.id==='pc:receiver');
  assert.equal(p.shipSequence.cargoTransfers.length,2);assert.equal(pc.character.gear.find(x=>x.id==='medpac').quantity,1);
  assert.equal(pc.character.gear.find(x=>x.id==='rations').quantity,3);assert.equal(h.swe02ShipCargoTransferOptions().items.some(x=>x.itemId==='medpac'),false);
  a.status(h.state.world);
 }
});
test('P238: changing a successful slow tracker to false cannot conceal skipped interception',()=>{
 let w=ship();w=roll(w,3,true);w=roll(w,5,true);w=roll(w,0,true);w=step(w,'ship-find-bura');w=step(w,'ship-depart-verified');
 let f=reopen(w);f.publishedAdventure.flags.shipTrackerSlowed=false;assert.throws(()=>a.status(f),/recibo omitido/);
 f=reopen(w);f.publishedAdventure.flags.shipCourseNonOptimal=true;assert.throws(()=>a.status(f),/falha documentada/);
});
