/** P155: real playable encounters in Beyond the Rim, journal/flags and rollback. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {BEYOND_THE_RIM, startBeyondTheRim, advanceBeyondTheRim, beyondTheRimStatus,
 currentBeyondTheRimEncounter,beyondTheRimEncounterSpec,markBeyondTheRimEncounterActive,
 recordBeyondTheRimEncounterOutcome} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
import {getPublishedAdventureAdapter,publishedAdventureAdapterSnapshot,publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const clone=structuredClone;
const paths={
 'droid-delivery':['meet-reom','accept-job','investigate-rumors','return-from-rumors','wait-for-droid'],
 'rival-convergence':null,
 'imperial-interruption':null
};
function toHangar(){let w=startBeyondTheRim({}).world;for(const choice of paths['droid-delivery'])w=advanceBeyondTheRim(w,choice).world;return w;}
function toScene(node){let w=toHangar();if(node==='droid-delivery')return w;
 // Tests on later episodes use a scene projection rather than inventing
 // an entire playthrough. The production controller still enforces scene + UAE.
 w=clone(w);w.publishedAdventure.currentNodeId=node;w.publishedAdventure.visitedNodeIds.push(node);return w;
}
class Repo{constructor(id='p155'){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return clone(this.saved);}save(x){if(this.fail)throw Error('P155 storage failed');this.saved=clone(x);this.writes++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');g.startPublishedAdventureSession('beyond-the-rim',{rng:()=>0.5});for(const choice of paths['droid-delivery'])g.advancePublishedAdventure(choice,{adventureId:'beyond-the-rim'});return {g,repo};}
const expected={
 'droid-delivery':['btr-yiyar-hangar','yiyar_salvager',2],
 'rival-convergence':['btr-yiyar-jungle','yiyar_salvager',3],
 'imperial-interruption':['btr-imperial-interception','stormtrooper',3]
};
test('P155: registry advertises actual combat for all five, without claiming PDF certification',()=>{
 assert.equal(publishedAdventureCatalog().length,5);
 for(const {id} of publishedAdventureCatalog())assert.equal(publishedAdventureAdapterSnapshot(id).capabilities.encounters,true);
 assert.equal(BEYOND_THE_RIM.implementation.episodesTotal,3);
 const module=publishedAdventureRuntimeModule(BEYOND_THE_RIM);assert.ok(validatePublishedAdventureGraph(module));
});
for(const [node,[id,profile,count]] of Object.entries(expected)){
 test(`P155: ${node} exposes scene-locked, runnable ${id}`,()=>{
  const w=toScene(node),spec=beyondTheRimEncounterSpec(w);
  assert.equal(currentBeyondTheRimEncounter(w).id,id);
  assert.equal(spec.id,id);assert.equal(spec.adversaries[0].profileKey,profile);
  assert.equal(spec.adversaries[0].count,count);
  assert.equal(spec.implementationAuthority,'BRASEIRO_PROVISIONAL_NOT_PDF_CERTIFIED');
  assert.equal(createAdversary(profile,{count}).profileKey,profile);
  assert.throws(()=>beyondTheRimEncounterSpec(w,'invented'),/não disponível/);
  const active=markBeyondTheRimEncounterActive(w,id);
  assert.equal(currentBeyondTheRimEncounter(active).status,'active');
  assert.throws(()=>markBeyondTheRimEncounterActive(active,id),/já está ativo/);
  assert.throws(()=>advanceBeyondTheRim(active,BEYOND_THE_RIM.nodes[node].choices[0].id),/Encerre o encontro/);
 });
 test(`P155: ${node} persists victory, blocks replay and allows same scene to be inspected`,()=>{
  const w=markBeyondTheRimEncounterActive(toScene(node),id);
  const r=recordBeyondTheRimEncounterOutcome(w,{encounterId:id,outcome:'adversaries_defeated',adversaries:[{id:'one',profileKey:profile,status:'incapacitated',wounds:5}]});
  assert.equal(r.record.encounterId,id);assert.equal(r.world.publishedAdventure.systems.encounters.activeId,null);
  assert.equal(r.world.publishedAdventure.systems.encounters.history.length,1);
  assert.equal(currentBeyondTheRimEncounter(r.world).status,'resolved');
  assert.throws(()=>recordBeyondTheRimEncounterOutcome(r.world,{encounterId:id,outcome:'adversaries_defeated'}),/não disponível/);
  assert.equal(beyondTheRimStatus(r.world).encounter.available,false);
  const restored=clone(r.world);assert.deepEqual(beyondTheRimStatus(restored).encounter,beyondTheRimStatus(r.world).encounter);
 });
 test(`P155: ${node} rejects invalid outcome and premature settlement without mutating source`,()=>{
  const w=toScene(node),before=clone(w);
  assert.throws(()=>recordBeyondTheRimEncounterOutcome(w,{encounterId:id,outcome:'adversaries_defeated'}),/não está ativo/);
  assert.throws(()=>recordBeyondTheRimEncounterOutcome(w,{encounterId:'invented',outcome:'ended'}),/não disponível/);
  assert.deepEqual(w,before);
  const active=markBeyondTheRimEncounterActive(w,id);
  assert.throws(()=>recordBeyondTheRimEncounterOutcome(active,{encounterId:id,outcome:'arbitrary'}),/inválido/);
  assert.deepEqual(active.publishedAdventure.systems.encounters.history,[]);
 });
}
for(const outcome of ['adversaries_defeated','player_incapacitated','retreat','ended']){
 test(`P155: hangar ${outcome} maps only real combat outcomes to droid custody`,()=>{
  const w=markBeyondTheRimEncounterActive(toHangar(),'btr-yiyar-hangar');
  const p=recordBeyondTheRimEncounterOutcome(w,{encounterId:'btr-yiyar-hangar',outcome}).world.publishedAdventure;
  assert.equal(Boolean(p.flags.droidRecovered),outcome==='adversaries_defeated');
  assert.equal(Boolean(p.flags.droidLost),['player_incapacitated','retreat'].includes(outcome));
  assert.equal(Boolean(p.flags.yiyarEscalated),['player_incapacitated','retreat'].includes(outcome));
 });
}
test('P155: imperial victory reduces exposure and defeat raises it, neutral ending preserves it',()=>{
 for(const [outcome,expectedExposure] of [['adversaries_defeated',3],['player_incapacitated',5],['ended',4]]){
  const w=toScene('imperial-interruption');w.publishedAdventure.systems.imperialExposure=4;
  const active=markBeyondTheRimEncounterActive(w,'btr-imperial-interception');
  const next=recordBeyondTheRimEncounterOutcome(active,{encounterId:'btr-imperial-interception',outcome}).world;
  assert.equal(next.publishedAdventure.systems.imperialExposure,expectedExposure);
 }
});
test('P155: real GameController Android combat starts, finishes and persists Yiyar outcome',()=>{
 const {g,repo}=game();assert.equal(validateState(repo.saved),true);
 const started=g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5});
 assert.equal(started.encounter.id,'btr-yiyar-hangar');assert.equal(g.state.combat.active,true);
 assert.equal(g.state.combat.adversaries[0].profileKey,'yiyar_salvager');
 assert.throws(()=>g.advancePublishedAdventure('pursue-yiyar',{adventureId:'beyond-the-rim'}),/Encerre o encontro/);
 g.state.combat.encounter.outcome='adversaries_defeated';
 const done=g.finishCombat({rng:()=>0.5});
 assert.equal(done.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(repo.saved.world.publishedAdventure.flags.droidRecovered,true);
 assert.equal(validateState(repo.saved),true);
 const writes=repo.writes,reopened=new GameController(repo);
 assert.equal(reopened.publishedAdventureStatus('beyond-the-rim').encounter.status,'resolved');
 assert.throws(()=>reopened.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5}),/não disponível/);
 assert.equal(repo.writes,writes);
});
test('P155: repository failure rolls back combat start and UAE encounter journal',()=>{
 const {g,repo}=game(),before=clone(g.state),saved=clone(repo.saved),writes=repo.writes;
 repo.fail=true;
 assert.throws(()=>g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5}),/storage failed/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P155: repository failure rolls back combat finish, history, NPCs and flags',()=>{
 const {g,repo}=game();g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5});
 g.state.combat.encounter.outcome='adversaries_defeated';const before=clone(g.state),saved=clone(repo.saved),writes=repo.writes;
 repo.fail=true;
 assert.throws(()=>g.finishCombat({rng:()=>0.5}),/storage failed/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
for(const {id} of publishedAdventureCatalog())test(`P155: ${id} retains UAE start, choice and 3 reloads`,()=>{
 const repo=new Repo(`p155:${id}`),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 g.executePublishedAdventureCommand({actionId:`p155:${id}:start`,kind:'start',adventureId:id});
 const first=getPublishedAdventureAdapter(id).status(g.state.world).node.choices[0].id;
 g.executePublishedAdventureCommand({actionId:`p155:${id}:choice`,kind:'choice',adventureId:id,choiceId:first});
 const saved=clone(repo.saved),writes=repo.writes;
 for(let i=0;i<3;i++){
  const reopened=new GameController(repo);
  const again=reopened.executePublishedAdventureCommand({actionId:`p155:${id}:start`,kind:'start',adventureId:id});
  assert.equal(again.duplicate,true);assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);
  assert.equal(validateState(repo.saved),true);
 }
});
