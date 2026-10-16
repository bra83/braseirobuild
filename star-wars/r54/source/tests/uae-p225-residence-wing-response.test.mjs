import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {recordResolvedTaskTime} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {validateJewelResidenceWingResponse,pendingJewelResidenceWingChecks} from '../app/src/main/assets/vtt/js/adventure/jewelResidenceWingResponse.mjs';
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

function failedFixture(){
 const {g,repo}=fixture();
 const c=clone(g.state.player.character);
 c.characteristics.astucia=1;c.skills.sigilo=0;
 g.commit('CHARACTER_UPDATED',{character:c});
 g.recordJewelArendPatrol({actionId:'p225-start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});
 advance(g,15,'p225-failure');
 const result=g.resolveJewelResidencePatrolCheck({actionId:'p225-fail-check',skillKey:'sigilo',rng:()=>.999});
 assert.equal(result.check.success,false,'fixture must produce a real failed roll');
 return {g,repo,result};
}
const request=(extra={})=>({actionId:'p225-wing-arrival',checkId:'p225-fail-check',count:3,range:'Médio',gmPhysicalArrivalConfirmed:true,gmDistinctOfficersConfirmed:true,gmWingGuardChosenConfirmed:true,rng:()=>.5,...extra});
test('P225 failed roll generates pending patrol, not invented NPCs or damage',()=>{
 const {g}=failedFixture();
 assert.equal(pendingJewelResidenceWingChecks(g.state.world.publishedAdventure).length,1);
 assert.equal(g.state.combat.active,false);
 assert.equal(g.state.world.publishedAdventure.operations.residenceWingResponse,undefined);
});
test('P225 requires real failure, physical presence, distinct guards, GM faction and real range/count',()=>{
 const {g}=failedFixture();
 for(const key of ['gmPhysicalArrivalConfirmed','gmDistinctOfficersConfirmed','gmWingGuardChosenConfirmed'])
  atomic(g,()=>g.beginJewelResidenceWingResponse(request({[key]:false})));
 for(const bad of [0,9,1.5,-1])atomic(g,()=>g.beginJewelResidenceWingResponse(request({count:bad})));
 atomic(g,()=>g.beginJewelResidenceWingResponse(request({range:'Teleporte'})));
 atomic(g,()=>g.beginJewelResidenceWingResponse(request({checkId:'forged'})));
});
test('P225 GM-confirmed physical Wing Guard uses official minion profile and real initiative',()=>{
 const {g,repo}=failedFixture();const beforeXp=clone(g.state.player.character.xp);
 const out=g.beginJewelResidenceWingResponse(request());
 assert.equal(out.adjudication.sequence,1);
 assert.equal(g.state.combat.active,true);
 assert.equal(g.state.combat.publishedAdventureContext.encounterId,'jewel-residence-wing-1');
 assert.equal(g.state.combat.adversaries.length,1);
 assert.equal(g.state.combat.adversaries[0].profileKey,'bespin_wing_guard_officer');
 assert.equal(g.state.combat.adversaries[0].membersTotal,3);
 assert.equal(g.state.combat.adversaries[0].wounds.threshold,15);
 assert.deepEqual(g.state.player.character.xp,beforeXp);
 for(let i=0;i<3;i++)assert.equal(validateJewelResidenceWingResponse(new GameController(repo).state.world.publishedAdventure).length,1);
 atomic(g,()=>g.beginJewelResidenceWingResponse(request({actionId:'duplicate'})));
});
test('P225 real encounter closes into persistent published encounter history without replay',()=>{
 const {g,repo}=failedFixture();g.beginJewelResidenceWingResponse(request());
 const result=g.finishCombat({rng:()=>.5});
 assert.equal(result.publishedAdventureOutcome.encounterId,'jewel-residence-wing-1');
 assert.equal(g.state.combat.active,false);
 assert.equal(g.state.world.publishedAdventure.activeEncounterId,null);
 assert.equal(g.state.world.publishedAdventure.encounterHistory.filter(x=>x.encounterId==='jewel-residence-wing-1').length,1);
 assert.equal(validateJewelResidenceWingResponse(new GameController(repo).state.world.publishedAdventure).length,1);
 atomic(g,()=>g.beginJewelResidenceWingResponse(request({actionId:'another'})));
});
test('P225 anti-fraud replay detects modified count, source, proof, chronology and derived state',()=>{
 const {g}=failedFixture();g.beginJewelResidenceWingResponse(request());
 for(const edit of [p=>p.operations.receipts['p225-wing-arrival'].count=99,
  p=>p.operations.receipts['p225-wing-arrival'].source='invented',
  p=>p.operations.receipts['p225-wing-arrival'].checkId='false',
  p=>p.operations.receipts['p225-wing-arrival'].atMinute=0,
  p=>p.operations.residenceWingResponse[0].count=1,
  p=>delete p.operations.receipts['p225-wing-arrival']]){
  const w=clone(g.state.world);edit(w.publishedAdventure);
  assert.throws(()=>validateJewelResidenceWingResponse(w.publishedAdventure,w));
 }
});
test('P225 positive check never creates Wing Guard, preserves clock and saves',()=>{
 const {g}=fixture();g.recordJewelArendPatrol({actionId:'start',type:'RESIDENCE_START',gmOutdoorsConfirmed:true});advance(g,15,'p225-positive');
 const check=g.resolveJewelResidencePatrolCheck({actionId:'pass',skillKey:'sigilo',rng:()=>.28});
 if(check.check.success)atomic(g,()=>g.beginJewelResidenceWingResponse(request({checkId:'pass'})));
 else assert.equal(pendingJewelResidenceWingChecks(g.state.world.publishedAdventure).length,1);
 assert.equal(g.state.world.clock.elapsedMinutes,15);
});
test('P225 Android PT-BR UI, bridge, bundle, source and version integrated without Kokoro changes',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p225-wing-start'],
  ['js/core/controller.mjs','beginJewelResidenceWingResponse'],
  ['js/core/uiBridge.mjs','beginJewelResidenceWingResponse'],
  ['js/core/uiBridge.bundle.js','jewelResidenceWingResponse.mjs'],
  ['js/adventure/jewelOfYavin.mjs','residenceWingPending:'],
  ['js/core/state.mjs','validateJewelResidenceWingResponse']])
   assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10225);
});
