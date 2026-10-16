import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {recordResolvedTaskTime} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {validateJewelArendPatrols,jewelArendPatrolStatus} from '../app/src/main/assets/vtt/js/adventure/jewelArendPatrols.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P221');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(holdingId='plaza-residence'){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p221-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.mecanica=7;ch.characteristics.voluntad=7;ch.skills.frialdad=7;ch.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'p221-bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'p221-bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const w=clone(g.state.world);w.publishedAdventure.encounterHistory=[{encounterId:'arend-shen-four-goons',outcome:'escaped'}];g.commit('WORLD_UPDATED',{world:w});
 g.investigateJewelArendHoldings({actionId:'p221-lead',skill:'computadoras',gmMainframeAccessConfirmed:true,rng:()=>.28});
 const all=['plaza-residence','figg-hall-office','port-town-building','plaza-yacht'];
 for(let i=0;i<all.length;i++){const h=all[i];const result=g.visitJewelArendHolding({actionId:'p221-visit-'+i,holdingId:h,gmSearchConfirmed:true,rng:()=>h===holdingId?0:0.999});if(result.investigation.foundAt)break;}
 assert.equal(g.state.world.publishedAdventure.operations.arendRevenge.foundAt,holdingId);
 return {g,repo};
}
function advance(g,minutes,id){const w=recordResolvedTaskTime(g.state.world,{checkId:'task_p221_'+id,skillKey:'callejeo',minutes,baseMinutes:minutes}).world;g.commit('WORLD_UPDATED',{world:w});}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P221 residence cannot start without physical presence, and never invents time',()=>{
 const {g}=fixture();atomic(g,()=>g.recordJewelArendPatrol({actionId:'x',type:'RESIDENCE_START'}));
 g.recordJewelArendPatrol({actionId:'residence-start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});
 assert.equal(jewelArendPatrolStatus(g.state.world).residence.nextDueMinute,15);
 assert.equal(g.state.world.clock.elapsedMinutes??0,0);
 atomic(g,()=>g.resolveJewelResidencePatrolCheck({actionId:'early',skillKey:'sigilo'}));
});
test('P221 fifteen real minutes trigger one Hard roll; no replay or double charging',()=>{
 const {g,repo}=fixture();g.recordJewelArendPatrol({actionId:'residence-start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});
 advance(g,14,'first');assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,false);
 atomic(g,()=>g.resolveJewelResidencePatrolCheck({actionId:'early',skillKey:'sigilo'}));
 advance(g,1,'second');assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,true);
 const r=g.resolveJewelResidencePatrolCheck({actionId:'patrol-1',skillKey:'sigilo',rng:()=>.28});
 assert.equal(r.check.pool.difficulty,3);assert.equal(r.adjudication.dueMinute,15);
 assert.equal(r.adjudication.check.faces.filter(x=>x.type==='difficulty').length,3);
 assert.equal(g.state.world.clock.elapsedMinutes,15);assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,false);
 atomic(g,()=>g.resolveJewelResidencePatrolCheck({actionId:'patrol-1',skillKey:'sigilo'}));
 for(let i=0;i<3;i++)assert.equal(validateJewelArendPatrols(new GameController(repo).state.world.publishedAdventure).residence.checks.length,1);
});
test('P221 multiple intervals remain due sequentially; leaving cannot erase pending checks',()=>{
 const {g}=fixture();g.recordJewelArendPatrol({actionId:'start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});
 advance(g,31,'long');assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,true);
 atomic(g,()=>g.recordJewelArendPatrol({actionId:'end',type:'RESIDENCE_END',gmLeftOutdoorsConfirmed:true}));
 g.resolveJewelResidencePatrolCheck({actionId:'check1',skillKey:'engano',rng:()=>.28});
 assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,true);
 g.resolveJewelResidencePatrolCheck({actionId:'check2',skillKey:'callejeo',rng:()=>.28});
 assert.equal(jewelArendPatrolStatus(g.state.world).residence.due,false);
 g.recordJewelArendPatrol({actionId:'end',type:'RESIDENCE_END',gmLeftOutdoorsConfirmed:true});
 assert.equal(jewelArendPatrolStatus(g.state.world).residence.endedAt,31);
});
test('P221 Venture alert requires real crew encounter, physical transponder and four distinct cloud cars',()=>{
 const {g,repo}=fixture('plaza-yacht');atomic(g,()=>g.recordJewelArendPatrol({actionId:'alert',type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:true,gmTransponderActivatedConfirmed:true}));
 g.beginJewelArendPropertyEncounter({actionId:'security',holdingId:'plaza-yacht',stage:'security',range:'Médio',gmPhysicalPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmAccessConfirmed:true,rng:()=>.5});
 atomic(g,()=>g.recordJewelArendPatrol({actionId:'alert',type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:true}));
 g.recordJewelArendPatrol({actionId:'alert',type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:true,gmTransponderActivatedConfirmed:true});
 assert.equal(jewelArendPatrolStatus(g.state.world).venture.deadlineMinute,10);
 atomic(g,()=>g.recordJewelArendPatrol({actionId:'arrival',type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:true}));
 advance(g,9,'nine');g.recordJewelArendPatrol({actionId:'arrival',type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:true,gmFourDistinctVehiclesConfirmed:true});
 assert.equal(jewelArendPatrolStatus(g.state.world).venture.status,'ARRIVED');
 assert.equal(jewelArendPatrolStatus(g.state.world).venture.arrival.flights,2);
 for(let i=0;i<3;i++)assert.equal(validateJewelArendPatrols(new GameController(repo).state.world.publishedAdventure).venture.arrival.vehiclesPerFlight,2);
});
test('P221 no phantom arrival at ten minutes; overdue requires GM, no invented combat',()=>{
 const {g}=fixture('plaza-yacht');g.beginJewelArendPropertyEncounter({actionId:'security',holdingId:'plaza-yacht',stage:'security',range:'Médio',gmPhysicalPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmAccessConfirmed:true,rng:()=>.5});
 g.recordJewelArendPatrol({actionId:'alert',type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:true,gmTransponderActivatedConfirmed:true});
 advance(g,11,'late');assert.equal(jewelArendPatrolStatus(g.state.world).venture.status,'OVERDUE_GM_CONFIRMATION');
 atomic(g,()=>g.recordJewelArendPatrol({actionId:'late',type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:true,gmFourDistinctVehiclesConfirmed:true}));
 assert.equal(g.state.combat.adversaries.every(x=>x.profileKey==='yacht_crew'),true);
});
test('P221 forged faces, derived state, future timestamp and replay are rejected',()=>{
 const {g}=fixture();g.recordJewelArendPatrol({actionId:'start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});advance(g,15,'fifteen');g.resolveJewelResidencePatrolCheck({actionId:'check',skillKey:'sigilo',rng:()=>.28});
 for(const edit of [x=>x.operations.receipts.check.check.faces[0].index=999,x=>x.operations.receipts.check.atMinute=99,
  x=>x.operations.arendPatrols.residence.checks[0].skillKey='computadoras',x=>delete x.operations.receipts.start]){
  const w=clone(g.state.world);edit(w.publishedAdventure);assert.throws(()=>validateJewelArendPatrols(w.publishedAdventure,w));
 }
});
test('P221 controller, bridge, UI, bundle, version and Kokoro-preserving integration',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p221-venture-alert'],['r46/final-runtime.js','data-p221-patrol-roll'],
  ['js/core/controller.mjs','resolveJewelResidencePatrolCheck'],['js/core/uiBridge.mjs','recordJewelArendPatrol'],
  ['js/core/uiBridge.bundle.js','jewelArendPatrols.mjs'],['js/adventure/jewelOfYavin.mjs','arendPatrols: clone(jewelArendPatrolStatus(world))'],
  ['js/core/state.mjs','validateJewelArendPatrols']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10221);
});
