import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {recordResolvedTaskTime} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {validateJewelResidenceWingResponse,pendingJewelResidenceWingChecks} from '../app/src/main/assets/vtt/js/adventure/jewelResidenceWingResponse.mjs';
import {recordJewelPursuitTravelCheck,validateJewelPursuitTravel,jewelPursuitTravelStatus,PURSUER_FACTIONS} from '../app/src/main/assets/vtt/js/adventure/jewelPursuitTravel.mjs';
import {buildPositivePool,rollPool} from '../app/src/main/assets/vtt/js/rules/narrativeDice.mjs';
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

function trip(){const {g,repo}=fixture('plaza-yacht');return {g,repo,visit:g.state.world.publishedAdventure.operations.arendRevenge.visits.at(-1)};}
function lowStats(g){const c=clone(g.state.player.character);c.characteristics.astucia=1;c.skills.engano=0;g.commit('CHARACTER_UPDATED',{character:c});}
function failCheck(g,visit,actionId='p226-travel-fail'){
 lowStats(g);
 const result=g.resolveJewelPursuitTravelCheck({actionId,visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:true,rng:()=>.999});
 assert.equal(result.check.success,false);return result;
}
const arrive=(check,extra={})=>({actionId:'p226-arrival-1',checkId:check.id,slot:1,faction:'wing_guard',count:2,range:'Médio',gmPhysicalArrivalConfirmed:true,gmDistinctPersonnelConfirmed:true,gmFactionChosenConfirmed:true,rng:()=>.5,...extra});
test('P226 visit must be real and confirmed; no check before a visit, no double roll',()=>{
 const {g,visit}=trip();lowStats(g);
 atomic(g,()=>g.resolveJewelPursuitTravelCheck({actionId:'no',visitId:'forged',skillKey:'engano',gmNewLocationConfirmed:true,rng:()=>.999}));
 atomic(g,()=>g.resolveJewelPursuitTravelCheck({actionId:'no',visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:false,rng:()=>.999}));
 failCheck(g,visit);atomic(g,()=>g.resolveJewelPursuitTravelCheck({actionId:'twice',visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:true,rng:()=>.999}));
});
test('P226 real failed Hard dice produce one pending group, never teleport NPC',()=>{
 const {g}=trip(),visit=g.state.world.publishedAdventure.operations.arendRevenge.visits.at(-1);
 const {adjudication}=failCheck(g,visit);
 assert.equal(adjudication.slots,1);assert.equal(g.state.combat.active,false);
 assert.equal(g.state.world.publishedAdventure.operations.pursuitTravel.arrivals.length,0);
 assert.equal(jewelPursuitTravelStatus(g.state.world).nextSlot,1);
});
test('P226 successful Hard roll records zero pursuers and cannot deploy invented NPCs',()=>{
 const {g,visit}=trip();const c=clone(g.state.player.character);
 c.characteristics.astucia=7;c.skills.engano=7;g.commit('CHARACTER_UPDATED',{character:c});
 const out=g.resolveJewelPursuitTravelCheck({actionId:'p226-success',visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:true,rng:()=>.28});
 assert.equal(out.check.success,true);assert.equal(out.adjudication.slots,0);
 assert.equal(jewelPursuitTravelStatus(g.state.world).nextSlot,null);
 atomic(g,()=>g.beginJewelPursuitGroup(arrive(out.adjudication)));
 assert.equal(g.state.combat.active,false);
});
test('P226 two Threat on a failed real roll permit two distinct sequential groups',()=>{
 const {g,visit}=trip();lowStats(g);
 let i=0;const roll=g.resolveJewelPursuitTravelCheck({actionId:'threat2',visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:true,rng:()=>i++===0?.01:.78});
 assert.equal(roll.check.success,false);assert.ok(roll.check.netAdvantage<=-2);assert.equal(roll.adjudication.slots,2);
 g.beginJewelPursuitGroup(arrive(roll.adjudication,{actionId:'first',faction:'wing_guard'}));
 atomic(g,()=>g.beginJewelPursuitGroup(arrive(roll.adjudication,{actionId:'second',slot:2,faction:'grayson_security'})));
 g.finishCombat({rng:()=>.5});
 const next=g.beginJewelPursuitGroup(arrive(roll.adjudication,{actionId:'second',slot:2,faction:'grayson_security',count:3}));
 assert.equal(next.combat.adversaries[0].profileKey,PURSUER_FACTIONS.grayson_security.profileKey);
 assert.equal(next.combat.adversaries[0].membersTotal,3);
 assert.equal(validateJewelPursuitTravel(g.state.world.publishedAdventure).arrivals.length,2);
});
test('P226 one Despair on a real upgraded Hard pool permits exactly three groups',()=>{
 const {g,visit}=trip();lowStats(g);
 const c=g.state.player.character,pool={...buildPositivePool(c.characteristics.astucia,c.skills.engano),difficulty:2,challenge:1,boost:0,setback:0};
 let i=0;const check=rollPool(pool,()=>{const j=i++;return j===0?.01:j===3?.999:.2});
 assert.equal(check.success,false);assert.ok(check.despair>0);
 const out=recordJewelPursuitTravelCheck(g.state.world,g.state.combat,c,{actionId:'despair',visitId:visit.id,skillKey:'engano',gmNewLocationConfirmed:true,check});
 assert.equal(out.adjudication.slots,3);
 assert.equal(out.world.publishedAdventure.operations.pursuitTravel.arrivals.length,0);
});
test('P226 GM physical presence, faction, distinct personnel, count and range are mandatory',()=>{
 const {g,visit}=trip();const {adjudication}=failCheck(g,visit);
 for(const key of ['gmPhysicalArrivalConfirmed','gmDistinctPersonnelConfirmed','gmFactionChosenConfirmed'])atomic(g,()=>g.beginJewelPursuitGroup(arrive(adjudication,{[key]:false})));
 for(const extra of [{count:0},{count:9},{range:'Teleportado'},{faction:'aris_auto'},{checkId:'forged'},{slot:2}])atomic(g,()=>g.beginJewelPursuitGroup(arrive(adjudication,extra)));
});
test('P226 physical Wing Guard encounter uses actual profile, initiative and history',()=>{
 const {g,repo,visit}=trip();const {adjudication}=failCheck(g,visit),xp=clone(g.state.player.character.xp);
 const out=g.beginJewelPursuitGroup(arrive(adjudication));
 assert.equal(out.combat.active,true);assert.equal(out.combat.publishedAdventureContext.encounterId,'jewel-pursuit-travel-4-1');
 assert.equal(out.combat.adversaries[0].profileKey,'bespin_wing_guard_officer');assert.deepEqual(g.state.player.character.xp,xp);
 for(let i=0;i<3;i++)assert.equal(validateJewelPursuitTravel(new GameController(repo).state.world.publishedAdventure).arrivals.length,1);
 g.finishCombat({rng:()=>.5});
 assert.equal(g.state.world.publishedAdventure.encounterHistory.filter(x=>x.encounterId==='jewel-pursuit-travel-4-1').length,1);
 atomic(g,()=>g.beginJewelPursuitGroup(arrive(adjudication,{actionId:'duplicate'})));
});
test('P226 replay rejects falsified roll, arrival, chronology, derived state and receipt deletion',()=>{
 const {g,visit}=trip();const {adjudication}=failCheck(g,visit);g.beginJewelPursuitGroup(arrive(adjudication));
 for(const edit of [p=>p.operations.receipts[adjudication.id].slots=3,
  p=>p.operations.receipts[adjudication.id].faces[0].index=0,
  p=>p.operations.receipts['p226-arrival-1'].count=10,
  p=>p.operations.receipts['p226-arrival-1'].atMinute=-1,
  p=>p.operations.pursuitTravel.arrivals[0].faction='fake',
  p=>delete p.operations.receipts['p226-arrival-1']]){
  const w=clone(g.state.world);edit(w.publishedAdventure);assert.throws(()=>validateJewelPursuitTravel(w.publishedAdventure,w));
 }
});
test('P226 bridge, PT-BR UI, offline bundle and state validation integrated',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p226-roll'],['r46/final-runtime.js','data-p226-start'],
  ['js/core/controller.mjs','beginJewelPursuitGroup'],['js/core/uiBridge.mjs','resolveJewelPursuitTravelCheck'],
  ['js/core/uiBridge.bundle.js','jewelPursuitTravel.mjs'],['js/core/state.mjs','validateJewelPursuitTravel'],
  ['js/adventure/jewelOfYavin.mjs','pursuitTravel:']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10226);
});
