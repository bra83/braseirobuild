import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
const clone=structuredClone, adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo {constructor(){this.state=createCampaign('P209');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(){
 const repo=new Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const w=clone(g.state.world),p=w.publishedAdventure;
 // Generate real P201 bomb receipts instead of a fabricated status.
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=6;ch.skills.mecanica=5;ch.characteristics.voluntad=6;ch.skills.frialdad=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const valid=clone(g.state.world);p.operations=valid.publishedAdventure.operations;
 p.encounterHistory=[...(p.encounterHistory??[]),{encounterId:'arend-shen-four-goons',outcome:'adversaries_defeated',nodeId:p.currentNodeId}];
 p.activeEncounterId=null;
 g.commit('WORLD_UPDATED',{world:w});return {g,repo};
}
const args={actionId:'wing-holo-start',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>.5};
function atomic(g,fn){const s=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,s);assert.equal(g.repository.saves,n);}
test('P209 published SWE09 p94 Wing Guard force is two separate two-officer groups plus sergeant',()=>{
 const {g}=game(),spec=adapter.encounterSpec(g.state.world,'holo-park-wing-guard');
 assert.deepEqual(spec.adversaries.map(x=>[x.profileKey,x.count,x.range]),[
  ['bespin_wing_guard_officer',2,'Médio'],['bespin_wing_guard_officer',2,'Médio'],['bespin_wing_guard_sergeant',1,'Médio']]);
});
test('P209 explicit physical arrival is required and errors roll back all state',()=>{
 const {g}=game();atomic(g,()=>g.beginJewelHoloParkWingGuard({...args,gmParkArrivalConfirmed:false}));
 atomic(g,()=>g.beginJewelHoloParkWingGuard({...args,gmPursuersConfirmed:false}));
 atomic(g,()=>g.beginPublishedAdventureEncounter('holo-park-wing-guard',{adventureId:'jewel-of-yavin',rng:()=>.5}));
});
test('P209 real initiative, distinct minion groups, published context and receipt in ONE save',()=>{
 const {g,repo}=game(),n=repo.saves;const out=g.beginJewelHoloParkWingGuard(args);
 assert.equal(repo.saves,n+1);assert.equal(out.encounter.id,'holo-park-wing-guard');
 assert.equal(out.combat.active,true);assert.equal(out.combat.publishedAdventureContext.encounterId,'holo-park-wing-guard');
 assert.deepEqual(out.combat.adversaries.map(x=>[x.profileKey,x.membersTotal]),[
  ['bespin_wing_guard_officer',2],['bespin_wing_guard_officer',2],['bespin_wing_guard_sergeant',1]]);
 assert.equal(out.holoPark.started.factions.length,1);assert.equal(out.holoPark.started.factions[0],'wing_guard');
 assert.equal(validateJewelHoloPark(g.state.world.publishedAdventure).roundCount,0);
 atomic(g,()=>g.beginJewelHoloParkWingGuard({...args,actionId:'wing-repeat'}));
});
test('P209 three reopenings retain combat groups and landscape without duplicating receipts',()=>{
 const {g,repo}=game();g.beginJewelHoloParkWingGuard(args);
 for(let i=0;i<3;i++){
  const reopened=new GameController(repo);
  assert.equal(reopened.state.combat.adversaries.length,3);
  assert.equal(reopened.state.combat.publishedAdventureContext.encounterId,'holo-park-wing-guard');
  assert.equal(validateJewelHoloPark(reopened.state.world.publishedAdventure).started.id,'wing-holo-start');
 }
});
test('P209 no Wing Guard encounter before goons resolved or during active combat',()=>{
 const {g}=game();const w=clone(g.state.world);w.publishedAdventure.encounterHistory=[];g.commit('WORLD_UPDATED',{world:w});
 atomic(g,()=>g.beginJewelHoloParkWingGuard(args));
});
test('P209 Android bridge, UI and offline bundle source wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [path,token] of [['r46/final-runtime.js','data-p209-holo-start'],['js/core/uiBridge.mjs','beginJewelHoloParkWingGuard'],['js/core/controller.mjs','beginJewelHoloParkWingGuard'],['js/adventure/jewelOfYavin.mjs','holo-park-wing-guard']])
  assert.ok(readFileSync(new URL(path,root),'utf8').includes(token),path);
});
test('P209 finishing published Holo-Park combat records the real encounter outcome once',()=>{
 const {g,repo}=game();g.beginJewelHoloParkWingGuard(args);
 const before=repo.saves;
 const ended=g.finishCombat({rng:()=>.5});
 assert.equal(ended.publishedAdventureOutcome.encounterId,'holo-park-wing-guard');
 assert.ok(['ended','player_incapacitated','adversaries_defeated'].includes(ended.publishedAdventureOutcome.outcome));
 assert.equal(g.state.world.publishedAdventure.activeEncounterId,null);
 assert.equal(g.state.world.publishedAdventure.encounterHistory.filter(x=>x.encounterId==='holo-park-wing-guard').length,1);
 assert.equal(repo.saves,before+1);
});
