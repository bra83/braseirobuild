import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {completeCurrentSlot} from '../app/src/main/assets/vtt/js/rules/encounter.mjs';
import {recruitSWE02Gladiators,validateSWE02Palace,queueSWE02ThroneAlarm,triggerSWE02ThroneAlarmOnNpcTurn,applySWE02ThroneReinforcementsAtRoundBoundary,validateSWE02ThroneAlarm} from '../app/src/main/assets/vtt/js/adventure/swe02Act3PalaceWip.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>adapter.advance(w,id).world;
const arrive=()=>['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return'].reduce(step,adapter.start({}).world);
function gladiatorWorld(){let w=step(arrive(),'approach-fortified-gate');
 w=adapter.adjudicateCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:0}}).world;
 return step(step(w,'palace-gate-debt-bluff'),'palace-enter-gladiators');}
class Repo{constructor(){this.state=createCampaign('palace WIP3');}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function game(w){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world:w});return {g,repo};}
function reopen(repo){for(let i=0;i<3;i++){const g=new GameController(repo);assert.ok(adapter.status(g.state.world).active);}}
test('p.43: four gladiators, real bribe, once, no charm, no fake alliance',()=>{
 const w=gladiatorWorld(),c={id:'cael',credits:1600};
 assert.equal(adapter.status(w).node.id,'palace-gladiator-cells');
 assert.throws(()=>recruitSWE02Gladiators(w,c,{method:'CHARM'}),/Charme/);
 assert.throws(()=>recruitSWE02Gladiators(w,{id:'cael',credits:999}),/1.000/);
 const r=recruitSWE02Gladiators(w,c);assert.equal(r.character.credits,600);
 assert.equal(r.receipt.gladiators,4);assert.equal(r.receipt.creditsSpent,1000);
 assert.equal(r.world.publishedAdventure.flags.palaceGladiatorsAllied,true);
 assert.equal(validateSWE02Palace(r.world),true);
 assert.throws(()=>recruitSWE02Gladiators(r.world,r.character),/já negociados/);
 const f=structuredClone(r.world);f.publishedAdventure.palaceGladiatorReceipt.creditsSpent=0;
 assert.throws(()=>adapter.status(f),/suborno de gladiadores adulterado/);
});
test('p.43: opposed Coercion dice, failed check does not mint allies; receipt anti-fraud',()=>{
 const w=gladiatorWorld(),c={id:'cael',credits:0,characteristics:{voluntad:2},skills:{coaccion:0}};
 const r=recruitSWE02Gladiators(w,c,{method:'COERCE',rng:()=>0});
 assert.equal(r.receipt.method,'COERCE');assert.equal(r.receipt.creditsSpent,0);
 assert.equal(r.receipt.check.pool.difficulty,1);assert.equal(r.receipt.success,r.receipt.check.success);
 assert.equal(validateSWE02Palace(r.world),true);
 const f=structuredClone(r.world);f.publishedAdventure.palaceGladiatorReceipt.check.netSuccess=999;
 assert.throws(()=>adapter.status(f),/dados adulterados/);
});
test('p.43: gladiator combat is real and separate from Teemo; cannot fake victory',()=>{
 const w=gladiatorWorld(),spec=adapter.encounterSpec(w,'laoth-palace-gladiators');
 assert.equal(spec.adversaries.length,1);assert.equal(spec.adversaries[0].count,4);
 const active=adapter.markEncounterActive(w,'laoth-palace-gladiators');
 assert.throws(()=>adapter.recordEncounterOutcome(active,{encounterId:'laoth-palace-gladiators',outcome:'adversaries_defeated',adversaries:[{id:'g1',profileKey:'laoth_drombb_thug',status:'active'}]}),/não derrotados/);
 const r=adapter.recordEncounterOutcome(active,{encounterId:'laoth-palace-gladiators',outcome:'adversaries_defeated',adversaries:[{id:'g1',profileKey:'laoth_drombb_thug',status:'incapacitated'}]});
 assert.equal(r.record.combatProof.eliminated,true);
});
test('p.43: controller/bridge/UI: credits atomic, rollback and 3 reopenings',()=>{
 const {g,repo}=game(gladiatorWorld());g.state.player.character.credits=1200;g.commit('CHARACTER_UPDATED',{character:g.state.player.character});
 const r=g.recruitSWE02Gladiators({method:'BRIBE'});assert.equal(r.creditsSpent,1000);
 assert.equal(g.state.player.character.credits,200);assert.equal(g.state.world.publishedAdventure.flags.palaceGladiatorsAllied,true);
 assert.throws(()=>g.recruitSWE02Gladiators({method:'BRIBE'}),/já negociados/);
 assert.equal(g.state.player.character.credits,200);reopen(repo);
 const b=new BraseiroUiBridge({repository:repo});assert.equal(typeof b.recruitSWE02Gladiators,'function');assert.equal(typeof b.queueSWE02ThroneAlarm,'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02GladiatorRecruit','swe02GladiatorMethod','swe02ThroneAlarmQueue','swe02ThroneReinforcementCount']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
function throneCombat(){const teemo=createAdversary('teemo_the_hutt',{range:'Médio'});
 const guard=createAdversary('laoth_elite_gamorrean_guard',{count:2,range:'Curto'});
 return {active:true,round:1,publishedAdventureContext:{adventureId:'long-arm-of-the-hutt',encounterId:'teemo-palace-guards'},
  adversaries:[guard,teemo],encounter:{entries:[{id:'player',side:'player',successes:2,advantages:0,triumphs:0},{id:teemo.id,side:'npc',successes:1,advantages:0,triumphs:0}],
   slots:[{side:'player',sourceId:'player'},{side:'npc',sourceId:teemo.id}],cursor:0,actedActorIds:[],activeActorId:null,waitingForPlayer:false,npcActorIds:[teemo.id],playerActorId:'player',history:[]},
  playerTurn:{actionUsed:false,maneuversUsed:0},log:[]};}
test('p.44: GM confirms count; Teemo must spend maneuver on actual NPC turn; no premature arrival',()=>{
 const c=throneCombat(),teemo=c.adversaries.at(-1);
 assert.throws(()=>queueSWE02ThroneAlarm(c,{confirmedBarracksCount:0}),/confirmar/);
 const queued=queueSWE02ThroneAlarm(c,{confirmedBarracksCount:3});
 assert.throws(()=>queueSWE02ThroneAlarm(queued,{confirmedBarracksCount:3}),/já solicitado/);
 assert.equal(triggerSWE02ThroneAlarmOnNpcTurn(queued,c.adversaries[0].id).swe02ThroneAlarm,undefined);
 let triggered=triggerSWE02ThroneAlarmOnNpcTurn(queued,teemo.id);
 assert.equal(triggered.swe02ThroneAlarm.pressedRound,1);assert.equal(triggered.swe02ThroneAlarm.dueRound,5);
 assert.equal(validateSWE02ThroneAlarm(triggered),true);
 for(let round=2;round<=5;round++){
  triggered.round=round;
  triggered=applySWE02ThroneReinforcementsAtRoundBoundary(triggered,{rng:()=>.8});
  assert.equal(triggered.swe02ThroneAlarm.arrived,false);
 }
 triggered.round=6;
 triggered=applySWE02ThroneReinforcementsAtRoundBoundary(triggered,{rng:()=>.8});
 assert.equal(triggered.swe02ThroneAlarm.arrived,true);assert.equal(triggered.adversaries.length,3);
 assert.equal(triggered.adversaries.at(-1).membersTotal,3);
 assert.equal(triggered.encounter.entries.length,3);assert.equal(triggered.encounter.slots.length,3);
 assert.equal(validateSWE02ThroneAlarm(triggered),true);
 const repeat=applySWE02ThroneReinforcementsAtRoundBoundary(triggered,{rng:()=>.1});assert.equal(repeat.adversaries.length,3);
 for(let i=0;i<3;i++){triggered=JSON.parse(JSON.stringify(triggered));assert.equal(validateSWE02ThroneAlarm(triggered),true);}
 const forged=structuredClone(triggered);forged.swe02ThroneAlarm.dueRound=2;
 assert.throws(()=>validateSWE02ThroneAlarm(forged),/adulterado/);
});
test('p.44: no alarm from incapacitated Teemo; no reinforcements without alarm',()=>{
 const c=throneCombat();c.adversaries.at(-1).status='incapacitated';
 assert.throws(()=>queueSWE02ThroneAlarm(c,{confirmedBarracksCount:2}),/incapacitado/);
 const noAlarm=throneCombat();noAlarm.round=10;
 assert.equal(applySWE02ThroneReinforcementsAtRoundBoundary(noAlarm).adversaries.length,2);
});
test('p.44: controller commits real Teemo NPC maneuver, initiative countdown and three persisted reopens',()=>{
 const w=step(arrive(),'confront-teemo');const {g,repo}=game(w);
 const started=g.beginPublishedAdventureEncounter('teemo-palace-guards',{rng:()=>.999,palaceAdditionalHenchmenCount:2,gmHuttProfileConfirmed:true});
 assert.equal(started.encounter.id,'teemo-palace-guards');
 const before=JSON.stringify(g.state.combat);
 assert.throws(()=>g.queueSWE02ThroneAlarm({confirmedBarracksCount:0}),/confirmar/);
 assert.equal(JSON.stringify(g.state.combat),before);
 g.queueSWE02ThroneAlarm({confirmedBarracksCount:2});
 let attempts=0;
 while(!g.state.combat.swe02ThroneAlarm && attempts++<8){
  if(!g.state.combat.encounter.waitingForPlayer)break;
  g.finishPlayerInitiativeTurn({rng:()=>.999});
 }
 assert.ok(g.state.combat.swe02ThroneAlarm,'Teemo must act on NPC slot');
 assert.ok(g.state.combat.log.some(x=>x.type==='SWE02_THRONE_ALARM_PRESSED'&&x.maneuverUsed));
 assert.equal(validateSWE02ThroneAlarm(g.state.combat),true);
 for(let i=0;i<3;i++){
  const r=new GameController(repo);
  assert.equal(validateSWE02ThroneAlarm(r.state.combat),true);
  assert.equal(r.state.combat.swe02ThroneAlarm.dueRound,r.state.combat.swe02ThroneAlarm.pressedRound+4);
 }
});
