import test from 'node:test';
import assert from 'node:assert/strict';
import {
  SHIP_ALERTS, SHIP_MANIFEST,
  SHIP_CARGO_GEAR_MAPPINGS, shipCargoTransferOptions, recordShipCargoTransfer,
  shipIssueState, shipSmellSetback, shipTaskPool, shipCheckPool,
  recordShipObservation, recordShipRoomDiscovery, recordShipCheck,
  recordShipChoice, recordBuraConversation, validateShip
} from '../app/src/main/assets/vtt/js/adventure/swe02KraytFangP238.mjs';

const ship=()=>({id:'long-arm-of-the-hutt',currentNodeId:'krayt-fang-logs',flags:{},shipChecks:[],shipChoices:[]});
const reopen=x=>JSON.parse(JSON.stringify(x));

test('SWE02 p7: four alerts and unresolved issue state have exact ordered source cues',()=>{
  const p=ship();
  assert.deepEqual(SHIP_ALERTS.map(x=>x.id),['fuel-low','tracker','trandoshan-popup','captive']);
  let issues=shipIssueState(p);
  assert.ok(issues.alerts.every(x=>x.status==='unresolved'));
  assert.equal(issues.fuel.quantity,null);
  assert.equal(issues.smell.unresolved,true);
  assert.equal(issues.noise.concentrationPenalty,true);
  recordShipObservation(p,'observe-alert-icons');
  assert.ok(shipIssueState(reopen(p)).alerts.every(x=>x.status==='observed'));
  assert.throws(()=>recordShipObservation(p,'observe-alert-icons'),/já registrada/);
  const forged=reopen(p);forged.flags.shipCageReminderRead=true;
  assert.throws(()=>validateShip(forged),/alerta da cela sem recibo/);
});

test('SWE02 p7: odor and noise penalties compose for checks and medical aid',()=>{
  const p=ship();
  assert.equal(shipSmellSetback(p,'Mathus','general'),1);
  assert.equal(shipSmellSetback(p,'Lowhhrick','general'),2);
  assert.equal(shipSmellSetback(p,'41-VEX','general'),0);
  assert.equal(shipSmellSetback(p,'Oskara','locateSmell'),0);
  assert.deepEqual(shipTaskPool(p,2,{characterName:'Oskara',aidingLowhhrick:true}),{difficulty:1,challenge:1,setback:3,boost:0});
  assert.deepEqual(shipTaskPool(p,2,{characterName:'41-VEX',aidingLowhhrick:true}),{difficulty:1,challenge:1,setback:2,boost:0});
  assert.deepEqual(shipTaskPool(p,2,{characterName:'Oskara',locatingSmell:true}),{difficulty:1,challenge:1,setback:0,boost:0});
  assert.deepEqual(shipCheckPool(p,6,{characterName:'Pash',announcedSearch:true}),{difficulty:1,challenge:1,setback:0,boost:1});
  assert.deepEqual(shipCheckPool(p,8,{characterName:'Pash'}),{difficulty:1,challenge:1,setback:1,boost:0});
  p.flags.shipKlaxonOff=true;
  assert.deepEqual(shipTaskPool(p,2,{characterName:'Mathus'}),{difficulty:2,challenge:0,setback:1,boost:0});
  p.flags.shipPeltsJettisoned=true;
  assert.deepEqual(shipTaskPool(p,2,{characterName:'Lowhhrick'}),{difficulty:2,challenge:0,setback:0,boost:0});
});

test('SWE02 p7-8: route and slowed-tracker combination is deterministic and survives JSON reopen',()=>{
  for(const routeSuccess of [false,true])for(const trackerSlowed of [false,true]){
    const p=ship();
    recordShipCheck(p,3,true); // translation prerequisite for tracker settings
    recordShipCheck(p,5,trackerSlowed);
    recordShipCheck(p,0,routeSuccess);
    recordShipChoice(p,'ship-find-bura');
    recordShipChoice(p,'ship-depart-verified');
    assert.equal(p.flags.shipThwheekInterceptionRequired,!routeSuccess||!trackerSlowed);
    assert.equal(p.flags.shipCourseNonOptimal,!routeSuccess);
    const loaded=reopen(p);assert.equal(validateShip(loaded),true);
    assert.equal(loaded.flags.shipThwheekInterceptionRequired,!routeSuccess||!trackerSlowed);
  }
});

test('SWE02 pp9-11: cargo discovery records source manifest without transferring or selling items',()=>{
  const p=ship();
  const contents=recordShipRoomDiscovery(p,'number-two-hold');
  assert.deepEqual(contents.map(x=>x.id),['broken-astromech-parts','empty-crates-number-two','poorly-maintained-tools','medpac','general-purpose-scanner','electrobinoculars','trandoshan-respirator','glow-rod-number-two','tool-kit']);
  assert.equal(contents.find(x=>x.id==='broken-astromech-parts').note.includes('2,000'),true);
  assert.equal(p.shipSequence.cargoReceipts[0].ownership,'DISCOVERED_NOT_TRANSFERRED');
  assert.equal(p.credits,undefined);
  assert.throws(()=>recordShipRoomDiscovery(p,'number-two-hold'),/já registrado/);
  assert.equal(validateShip(reopen(p)),true);
  assert.equal(SHIP_MANIFEST.galley.find(x=>x.id==='ration-packs').quantity,6);
  assert.equal(SHIP_MANIFEST['crew-quarters'].find(x=>x.id==='escape-from-mos-shuuta-credits').note.includes('may place'),true);
});

test('SWE02 p11 + EotE Core: exact cataloged cargo transfers by count to a stable PC owner',()=>{
  const p=ship();p.status='ACTIVE';recordShipRoomDiscovery(p,'number-two-hold');recordShipRoomDiscovery(p,'galley');
  let options=shipCargoTransferOptions(p);
  assert.ok(options.some(x=>x.itemId==='medpac'&&x.catalogId==='medpac'&&x.availableQuantity===1&&x.rulePage===177));
  assert.ok(options.some(x=>x.itemId==='general-purpose-scanner'&&x.catalogId==='multi_scanner'&&x.rulePage===175));
  assert.ok(options.some(x=>x.itemId==='ration-packs'&&x.catalogId==='rations'&&x.availableQuantity===6&&x.rulePage===179));
  assert.equal(options.some(x=>x.itemId==='broken-astromech-parts'),false);
  assert.equal(options.some(x=>x.itemId==='empty-crates-number-two'),false);
  recordShipCargoTransfer(p,{roomId:'number-two-hold',itemId:'medpac',quantity:1,characterId:'pc:receiver'});
  recordShipCargoTransfer(p,{roomId:'galley',itemId:'ration-packs',quantity:2,characterId:'pc:receiver'});
  assert.equal(p.shipSequence.cargoTransfers.length,2);
  assert.equal(p.shipSequence.cargoTransfers[0].source.printedPage,11);
  assert.equal(p.shipSequence.cargoTransfers[0].rulesSource.printedPage,177);
  assert.equal(shipCargoTransferOptions(reopen(p)).find(x=>x.itemId==='ration-packs').availableQuantity,4);
  assert.throws(()=>recordShipCargoTransfer(p,{roomId:'number-two-hold',itemId:'medpac',quantity:1,characterId:'pc:receiver'}),/excede o item/);
  assert.throws(()=>recordShipCargoTransfer(p,{roomId:'number-two-hold',itemId:'broken-astromech-parts',quantity:1,characterId:'pc:receiver'}),/mapeamento/);
  assert.throws(()=>recordShipCargoTransfer(p,{roomId:'galley',itemId:'ration-packs',quantity:5,characterId:'pc:receiver'}),/excede/);
  assert.equal(validateShip(reopen(p)),true);
  const forged=reopen(p);forged.shipSequence.cargoTransfers[0].rulesSource.printedPage=181;
  assert.throws(()=>validateShip(forged),/transferência de carga inválido/);
});

test('SWE02 p9: pelt find creates six discovered items; odor-source search remains exempt',()=>{
  const p=ship();
  recordShipCheck(p,6,true);
  const cargo=p.shipSequence.cargoReceipts.find(x=>x.items.some(i=>i.id==='wookiee-pelts'));
  assert.equal(cargo.items[0].quantity,6);
  assert.equal(cargo.ownership,'DISCOVERED_NOT_TRANSFERRED');
  assert.equal(shipSmellSetback(p,'Sasha','ordinary'),1);
  assert.equal(shipSmellSetback(p,'Sasha','investigateSmellSource'),0);
  recordShipObservation(p,'lowhhrick-pelt-strain',{applied:true,characterId:'lowhhrick-pc'});
  assert.equal(p.shipSequence.lowhhrickStrainReceipt.amount,2);
  assert.equal(p.shipSequence.lowhhrickStrainReceipt.applied,true);
  assert.equal(validateShip(reopen(p)),true);
});

test('SWE02 p9-10: cage identifies B’ura; freeing is free; knowledge symbols retain graded shell facts',()=>{
  const p=ship();
  const cage=recordShipObservation(p,'press-cage-icon',{oskaraPresent:true});
  assert.equal(cage.details.buraLocated,true);
  assert.equal(cage.details.oskaraRecognition,true);
  recordShipChoice(p,'ship-find-bura');
  assert.equal(p.flags.shipBuraFreed,true);
  assert.equal(validateShip(reopen(p)),true);
  const conversation=recordBuraConversation(p,{topics:['injury','captureStory','shipHelp','mineReward','drombbDetails'],oskaraPresent:true,oskaraSharedHistory:true,oskaraRaisedCorruption:true});
  assert.ok(conversation.facts.some(x=>x.id==='oskaraConnection'&&x.printedPage===10));
  assert.ok(conversation.facts.some(x=>x.id==='drombbDetails'&&x.referencedPrintedPage===18));
  recordShipCheck(p,9,{success:true,netSuccess:2,netAdvantage:1,triumph:true});
  assert.deepEqual(p.shipChitinKnowledge,{intelligentInsectileSpecies:true,mayHaveShedShell:true,redDustProbablyGeonosis:true,clanOrOwnershipMarkings:true,exactOriginKnown:false,source:{productCode:'SWE02',printedPage:9}});
  recordShipObservation(p,'keep-marked-chitin');
  assert.equal(validateShip(reopen(p)),true);
});

test('SWE02 p8: tracker-removal threats preserve a hull-damage obligation without inventing trauma',()=>{
  const p=ship();p.currentNodeId='krayt-fang-ground-inspection';
  recordShipCheck(p,8,{success:false,netFailure:1,netThreat:2});
  assert.equal(p.flags.shipTrackerRemovalFailedToLocate,true);
  assert.deepEqual(p.shipTrackerRemovalHullDamage,{status:'GM_HULL_DAMAGE_ADJUDICATION_REQUIRED',threats:2,hullTraumaAmount:null,source:{productCode:'SWE02',printedPage:8}});
  assert.equal(validateShip(reopen(p)),true);
  const forged=reopen(p);forged.shipTrackerRemovalHullDamage.hullTraumaAmount=2;
  assert.throws(()=>validateShip(forged),/efeito de Ameaça no casco inválido/);
});

test('SWE02 p11: verified route records six uneventful hours and available rest without inventing recovery',()=>{
  const p=ship();
  recordShipCheck(p,0,true);recordShipChoice(p,'ship-find-bura');recordShipChoice(p,'ship-depart-verified');
  recordShipObservation(p,'complete-hyperspace-transit');
  assert.deepEqual(p.shipSequence.transit,{elapsedHours:6,elapsedHoursIsMinimum:true,durationStatus:'A_LITTLE_MORE_THAN_SIX_HOURS',extraDurationStatus:'NOT_REQUIRED',uneventful:true,restAndRecuperationAvailable:true,completed:true,source:{productCode:'SWE02',printedPage:11}});
  assert.equal(p.shipSequence.transit.strainRecovered,undefined);
  assert.equal(validateShip(reopen(p)),true);
});
