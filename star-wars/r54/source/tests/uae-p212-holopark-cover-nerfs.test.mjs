import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
import {validateJewelHoloEffects,holoParkThrantaBoost,holoParkShadowDefense,holoParkCoruscantCover,JEWEL_HOLO_EFFECT_RULES} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone, adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo {constructor(){this.state=createCampaign('P212');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
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

function setup(roll){const {g,repo}=game();g.beginJewelHoloParkWingGuard({...args,rng:()=>roll});
 // Restore the fixture PC after the real encounter's initial NPC turns.
 const ch=clone(g.state.player.character);ch.derived.wounds.current=0;ch.derived.strain.current=0;
 g.commit('CHARACTER_UPDATED',{character:ch});return {g,repo};}
function attackEvidence(g,{triumph=0,advantage=0,id='real-attack-a'}={}){
 const c=clone(g.state.combat),target=c.adversaries[0];
 c.pendingPlayerSpend={attackId:id,adversaryId:target.id,remainingTriumph:triumph,remainingAdvantage:advantage,
  attackSnapshot:{hit:true}};
 g.commit('COMBAT_UPDATED',{combat:c});return target.id;
}
function doEffect(g,e,id='effect-1'){
 return g.applyJewelHoloParkEffect({actionId:id,effect:e,gmSymbolConfirmed:true,gmBoardedThrantaConfirmed:e==='alderaan_thranta'});
}
function noWrite(g,fn){const state=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,n);}


test('P212 official results: Coruscant flickering building and Alderaan nerf herd are distinct Triumph outcomes',()=>{
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.coruscant_building,{landscape:'coruscant',symbol:'triumph',cost:1,kind:'player-building-cover'});
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.alderaan_nerfs,{landscape:'alderaan',symbol:'triumph',cost:1,kind:'player-nerf-stealth'});
});
test('P212 Coruscant consumes actual Triumph, applies defense 2 to ranged only, no melee',()=>{
 const {g,repo}=setup(.60);attackEvidence(g,{triumph:1,id:'coruscant-hit'});
 const result=doEffect(g,'coruscant_building');
 assert.equal(result.adjudication.result.rangedDefense,2);
 assert.equal(g.state.combat.pendingPlayerSpend.remainingTriumph,0);
 assert.equal(holoParkCoruscantCover(g.state.combat,{skill:'armas_distancia_ligeras'}),2);
 assert.equal(holoParkCoruscantCover(g.state.combat,{skill:'pelea'}),0);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
test('P212 Coruscant real movement clears cover without erasing the receipt',()=>{
 const {g}=setup(.60);attackEvidence(g,{triumph:1});doEffect(g,'coruscant_building');
 const c=clone(g.state.combat);c.pendingPlayerSpend=null;c.playerTurn={...c.playerTurn,maneuversUsed:0};
 c.encounter.cursor=c.encounter.slots.findIndex(slot=>slot.side==='player');c.encounter.waitingForPlayer=true;c.encounter.outcome=null;
 g.commit('COMBAT_UPDATED',{combat:c});
 const id=g.state.combat.adversaries[0].id;
 g.changeCombatRange({targetAdversaryId:id,direction:'closer'});
 assert.equal(holoParkCoruscantCover(g.state.combat,{skill:'armas_distancia_ligeras'}),0);
 assert.equal(validateJewelHoloEffects(g.state.world.publishedAdventure).history.length,1);
});
test('P212 Coruscant rejects no Triumph, wrong landscape, duplicate receipt and tampering atomically',()=>{
 const {g}=setup(.60);attackEvidence(g,{triumph:0});noWrite(g,()=>doEffect(g,'coruscant_building'));
 const c=clone(g.state.combat);c.pendingPlayerSpend.remainingTriumph=1;g.commit('COMBAT_UPDATED',{combat:c});
 doEffect(g,'coruscant_building');noWrite(g,()=>doEffect(g,'coruscant_building','duplicate'));
 const w=clone(g.state.world);w.publishedAdventure.operations.receipts['effect-1'].result.rangedDefense=8;
 assert.throws(()=>validateJewelHoloEffects(w.publishedAdventure),/adulterad/);
});
test('P212 Alderaan nerfs spend a real maneuver, roll Average Stealth and await GM adjudication',()=>{
 const {g,repo}=setup(.10);attackEvidence(g,{triumph:1,id:'herd-hit'});
 const ch=clone(g.state.player.character);ch.characteristics.agilidad=5;ch.skills.sigilo=4;
 g.commit('CHARACTER_UPDATED',{character:ch});
 const result=g.applyJewelHoloParkEffect({actionId:'nerf-stealth-1',effect:'alderaan_nerfs',gmSymbolConfirmed:true,
  gmHerdConfirmed:true,maneuverMethod:'normal',rng:()=>.22});
 assert.equal(result.adjudication.result.maneuverSpent,true);
 assert.equal(result.adjudication.result.gmEscapePending,true);
 assert.equal(result.adjudication.result.check.pool.difficulty,2);
 assert.equal(g.state.combat.pendingPlayerSpend.remainingTriumph,0);
 assert.ok(g.state.combat.playerTurn.maneuversUsed>=1);
 assert.equal(g.state.combat.holoParkNerfStealth.gmEscapePending,true);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
test('P212 Alderaan refuses unconfirmed herd, missing Triumph, and invented stealth faces with atomic rollback',()=>{
 const {g}=setup(.10);attackEvidence(g,{triumph:1,id:'nerf-real'});
 noWrite(g,()=>g.applyJewelHoloParkEffect({actionId:'nerf-no-herd',effect:'alderaan_nerfs',gmSymbolConfirmed:true,rng:()=>.2}));
 g.applyJewelHoloParkEffect({actionId:'nerf-yes',effect:'alderaan_nerfs',gmSymbolConfirmed:true,gmHerdConfirmed:true,rng:()=>.2});
 const w=clone(g.state.world);w.publishedAdventure.operations.receipts['nerf-yes'].result.check.faces[0].index=999;
 assert.throws(()=>validateJewelHoloEffects(w.publishedAdventure),/adulterad/);
 noWrite(g,()=>g.applyJewelHoloParkEffect({actionId:'nerf-twice',effect:'alderaan_nerfs',gmSymbolConfirmed:true,gmHerdConfirmed:true,rng:()=>.2}));
});
test('P212 Coruscant defense modifies ACTUAL NPC attack pool by two setback dice',()=>{
 const {g}=setup(.60);attackEvidence(g,{triumph:1});doEffect(g,'coruscant_building');
 const c=g.state.combat,id=c.adversaries.find(a=>a.kind==='minion'&&!a.wounds?.incapacitated)?.id??c.adversaries[0].id;
 const covered=g._resolveNpcTurn(c,g.state.player.character,id,()=>.5,g.state.group);
 const without=clone(c);delete without.holoParkCoruscantCover;
 const baseline=g._resolveNpcTurn(without,g.state.player.character,id,()=>.5,g.state.group);
 assert.equal(covered.attack.check.pool.setback,baseline.attack.check.pool.setback+2);
});
test('P212 Android version, PT-BR UI and bundle expose the new effects',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const token of ['coruscant_building','alderaan_nerfs','data-p212-herd','data-p212-maneuver'])
  assert.ok(readFileSync(new URL('r46/final-runtime.js',root),'utf8').includes(token),token);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode = (\d+)/)?.[1])>=10212);
});
