import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES as profiles} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>a.advance(w,id).world,chain=(w,...ids)=>ids.reduce((state,id)=>step(id==='reach-new-meen'?resolveOldLylekRetreat(a,state):state,id),w),start=()=>a.start({}).world;
const party=()=>chain(start(),'dock-ryloth','head-to-geonosis','attend-piddock-party');
const audience=()=>step(party(),'piddock-to-dimmock');
const check=(w,i,success)=>a.adjudicateCheck(w,{checkIndex:i,result:{success,netSuccess:success?1:0,netAdvantage:0}}).world;
const finish=w=>step(w,'dimmock-finish');
const proof=(w,status='incapacitated')=>a.encounterSpec(w,'laoth-gantry-aa7').adversaries.map((x,i)=>({id:`aa7-${i}`,profileKey:x.profileKey,status}));
const reopen=w=>JSON.parse(JSON.stringify(w));
test('P237 p37: official separate soldier and Vrixx’tt profiles',()=>{
 const s=profiles.laoth_geonosian_soldier,v=profiles.laoth_vrixxtt_findsman;
 assert.deepEqual([s.characteristics.agilidad,s.soak,s.woundsThreshold,s.weapons[0].damage,s.weapons[0].crit],[3,5,8,7,2]);
 assert.deepEqual([v.characteristics.agilidad,v.soak,v.woundsThreshold,v.weapons[0].damage,v.weapons[0].crit],[4,5,13,9,2]);
 assert.equal(v.source.printedPage,37);
});
test('P237 p34: Piddock Kubaz clue no roll, B1 clue requires Hard Charm',()=>{
 let w=party();assert.equal(a.currentChecks(w)[0].pool.difficulty,3);
 w=step(w,'piddock-kubaz');assert.equal(w.publishedAdventure.flags.geo_kubaz,true);
 assert.throws(()=>step(w,'piddock-kubaz'),/inválida/);
 assert.throws(()=>step(w,'piddock-b1'),/inválida/);
 w=check(w,0,true);w=step(w,'piddock-b1');assert.equal(w.publishedAdventure.flags.geo_b1,true);
});
test('P237 pp7,30: Sivor shell needs explicit collection and identification',()=>{
 let w=chain(start(),'inspect-krayt-fang','sivor-shell','finish-ship-inspection','head-to-geonosis','attend-piddock-party');
 assert.equal(w.publishedAdventure.flags.geo_shell,true);
 w=step(w,'piddock-identify-shell');assert.equal(w.publishedAdventure.flags.geo_identifiedShell,true);
 for(let i=0;i<3;i++){w=reopen(w);a.status(w)}
});
test('P237 p23: Drombb messages require victory and Hard Computers',()=>{
 let w=chain(start(),'dock-ryloth','help-new-meen','travel-lylek-den','reach-new-meen','confront-drombb-directly');
 assert.equal(a.currentChecks(w).length,0);assert.throws(()=>step(w,'drombb-messages'),/inválida/);
 w=structuredClone(w);w.publishedAdventure.flags.confirmedPartySize=3;w=a.markEncounterActive(w,'laoth-drombb-cantina');
 const enemies=a.encounterSpec(w,'laoth-drombb-cantina').adversaries.map((x,i)=>({id:`drombb-${i}`,profileKey:x.profileKey,status:'incapacitated'}));
 w=a.recordEncounterOutcome(w,{encounterId:'laoth-drombb-cantina',outcome:'adversaries_defeated',adversaries:enemies}).world;
 assert.equal(a.currentChecks(w)[0].pool.difficulty,3);w=check(w,0,true);w=step(w,'drombb-messages');
 assert.equal(w.publishedAdventure.flags.geo_messages,true);
});
test('P237 p36: zero points means five Geonosians and one Gand',()=>{
 let w=finish(audience());assert.equal(w.publishedAdventure.flags.geoScore,0);
 const spec=a.encounterSpec(w,'laoth-gantry-aa7');assert.equal(spec.geonosianGuardCount,5);
 assert.equal(spec.adversaries.length,6);assert.equal(spec.adversaries.at(-1).profileKey,'laoth_vrixxtt_findsman');
 assert.throws(()=>step(w,'board-lucky-guess'),/inválida/);
 for(let i=0;i<3;i++){w=reopen(w);a.status(w)}
});
test('P237 p36: three points means three Geonosians',()=>{
 let w=step(party(),'piddock-kubaz');w=step(w,'piddock-to-dimmock');w=check(w,0,true);
 w=chain(w,'dimmock-anti-empire','dimmock-kubaz');w=finish(w);
 assert.equal(w.publishedAdventure.flags.geoScore,3);assert.equal(a.encounterSpec(w,'laoth-gantry-aa7').geonosianGuardCount,3);
});
test('P237 p36: six-plus removes soldiers, not Vrixx’tt',()=>{
 let w=chain(start(),'inspect-krayt-fang','sivor-shell','finish-ship-inspection','help-new-meen','travel-lylek-den','reach-new-meen','confront-drombb-directly');
 w=structuredClone(w);w.publishedAdventure.flags.confirmedPartySize=3;w=a.markEncounterActive(w,'laoth-drombb-cantina');
 w=a.recordEncounterOutcome(w,{encounterId:'laoth-drombb-cantina',outcome:'adversaries_defeated',adversaries:a.encounterSpec(w,'laoth-drombb-cantina').adversaries.map((x,i)=>({id:`dr-${i}`,profileKey:x.profileKey,status:'incapacitated'}))}).world;
 w=check(w,0,true);w=chain(w,'drombb-messages','new-meen-saved-confirmed','ota-after-new-meen-defense','follow-ota-geonosis','attend-piddock-party');
 w=step(w,'piddock-kubaz');w=check(w,0,true);w=chain(w,'piddock-b1','piddock-identify-shell','piddock-to-dimmock');
 w=check(w,0,true);w=chain(w,'dimmock-anti-empire','dimmock-kubaz','dimmock-b1','dimmock-shell','dimmock-sivor','dimmock-messages');
 w=finish(w);assert.equal(w.publishedAdventure.flags.geoScore,7);
 const spec=a.encounterSpec(w,'laoth-gantry-aa7');assert.equal(spec.geonosianGuardCount,0);assert.equal(spec.adversaries.length,1);
});
test('P237 p36: failed Daunting Coerce, rude and Imperial loyalty apply penalties',()=>{
 let w=audience();assert.deepEqual(a.currentChecks(w).map(x=>x.pool.difficulty),[2,4]);
 w=check(w,1,false);w=chain(w,'dimmock-rude','dimmock-loyal-empire');
 assert.throws(()=>step(w,'dimmock-loyal-empire'),/duplicado/);
 assert.throws(()=>step(w,'dimmock-anti-empire'),/contraditórias/);
 w=finish(w);assert.equal(w.publishedAdventure.flags.geoScore,-4);
});
test('P237: forged negotiation points, facts and statements fail closed',()=>{
 const w=finish(audience());let f=structuredClone(w);f.publishedAdventure.flags.geoScore=9;assert.throws(()=>a.status(f),/adulterada/);
 f=structuredClone(w);f.publishedAdventure.flags.geo_kubaz=true;assert.throws(()=>a.status(f),/sem prova/);
 f=structuredClone(w);f.publishedAdventure.geonosisArguments.push({choiceId:'dimmock-b1',key:'b1',nodeId:'dimmock-official-audience'});
 assert.throws(()=>a.status(f),/sem prova/);
});
test('P237 p37: boarding requires resolved combat, no false victory or unverified bribe',()=>{
 let w=finish(audience());w=a.markEncounterActive(w,'laoth-gantry-aa7');
 assert.throws(()=>step(w,'board-lucky-guess'),/inválida/);
 assert.throws(()=>a.recordEncounterOutcome(w,{encounterId:'laoth-gantry-aa7',outcome:'adversaries_defeated',adversaries:proof(w,'active')}),/sem inimigos incapacitados/);
 assert.throws(()=>a.recordEncounterOutcome(w,{encounterId:'laoth-gantry-aa7',outcome:'negotiated',adversaries:proof(w)}),/pagamento verificado/);
 w=a.recordEncounterOutcome(w,{encounterId:'laoth-gantry-aa7',outcome:'adversaries_defeated',adversaries:proof(w)}).world;
 for(let i=0;i<3;i++){w=reopen(w);a.status(w)}
 w=chain(w,'board-lucky-guess','lucky-guess-to-tatooine');assert.equal(a.status(w).node.id,'tatooine-infiltration');
 const fake=structuredClone(w);fake.publishedAdventure.encounterHistory=[];assert.throws(()=>a.status(fake),/sem recibo/);
});
test('P237 p37: Vio rescue from player incapacitation does not claim enemy defeat',()=>{
 let w=finish(audience());w=a.markEncounterActive(w,'laoth-gantry-aa7');
 w=a.recordEncounterOutcome(w,{encounterId:'laoth-gantry-aa7',outcome:'player_incapacitated',adversaries:proof(w,'active')}).world;
 assert.equal(w.publishedAdventure.flags.geoRescuedByVios,true);
 assert.equal(w.publishedAdventure.encounterHistory.at(-1).outcome,'player_incapacitated');
});
class Repo{constructor(){this.state=createCampaign('SWE02 P237');this.saves=0}load(){return structuredClone(this.state)}save(x){this.state=structuredClone(x);this.saves++}}
test('P237 controller: real dice, persistence and gantry encounter across three reopens',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});
 for(const id of ['dock-ryloth','head-to-geonosis','attend-piddock-party','piddock-to-dimmock'])g.advancePublishedAdventure(id);
 const r=g.resolvePublishedAdventureCheck(0,{rng:()=>.9});assert.equal(r.adjudication.source.printedPage,36);
 for(let i=0;i<3;i++){const h=new GameController(repo);assert.equal(h.state.world.publishedAdventure.checkHistory.at(-1).success,r.check.success)}
 g.advancePublishedAdventure('dimmock-finish');const e=g.beginPublishedAdventureEncounter('laoth-gantry-aa7',{rng:()=>.9});
 assert.equal(e.encounter.adversaries.at(-1).profileKey,'laoth_vrixxtt_findsman');
 for(let i=0;i<3;i++){const h=new GameController(repo);assert.equal(h.state.combat.publishedAdventureContext.encounterId,'laoth-gantry-aa7')}
});
