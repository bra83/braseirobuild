import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
import {validateJewelHoloEffects,holoParkThrantaBoost,holoParkShadowDefense,JEWEL_HOLO_EFFECT_RULES} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
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

test('P211 official source: thranta Advantage and Christophsis shadow Advantage are two distinct published results',()=>{
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.alderaan_thranta,{landscape:'alderaan',symbol:'advantage',cost:1,kind:'player-ranged-boost'});
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.christophsis_shadow,{landscape:'christophsis',symbol:'advantage',cost:1,kind:'npc-shadow-cover'});
});
test('P211 Alderaan consumes REAL Advantage, grants one ranged Boost for two rounds and no melee bonus',()=>{
 const {g,repo}=setup(.10);attackEvidence(g,{advantage:1,id:'thranta-attack'});
 const before=repo.saves;const result=doEffect(g,'alderaan_thranta');
 assert.equal(repo.saves,before+1);assert.equal(result.adjudication.result.rangedBoost,1);
 assert.equal(g.state.combat.pendingPlayerSpend.remainingAdvantage,0);
 assert.equal(holoParkThrantaBoost(g.state.combat,{skill:'armas_distancia_ligeras'}),0);
 const active=clone(g.state.combat);active.round++;
 assert.equal(holoParkThrantaBoost(active,{skill:'armas_distancia_ligeras'}),1);
 assert.equal(holoParkThrantaBoost(active,{skill:'pelea'}),0);
 const late=clone(g.state.combat);late.round+=3;
 assert.equal(holoParkThrantaBoost(late,{skill:'armas_distancia_ligeras'}),0);
 noWrite(g,()=>doEffect(g,'alderaan_thranta','second-thranta'));
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
test('P211 Christophsis consumes REAL Advantage and gives the actual attacked NPC ranged defense 1 until next round',()=>{
 const {g,repo}=setup(.30),id=attackEvidence(g,{advantage:1,id:'shadow-attack'});
 const result=doEffect(g,'christophsis_shadow');
 assert.equal(result.adjudication.targetId,id);
 assert.equal(g.state.combat.pendingPlayerSpend.remainingAdvantage,0);
 const npc=g.state.combat.adversaries.find(a=>a.id===id);
 assert.equal(holoParkShadowDefense(npc,{skill:'armas_distancia_pesadas'},g.state.combat.round),1);
 assert.equal(holoParkShadowDefense(npc,{skill:'pelea'},g.state.combat.round),0);
 assert.equal(holoParkShadowDefense(npc,{skill:'armas_distancia_pesadas'},g.state.combat.round+2),0);
 noWrite(g,()=>doEffect(g,'christophsis_shadow','shadow-duplicate'));
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
test('P211 rejects invented symbols, wrong landscape, duplicate ID, mutated receipt and stacks atomically',()=>{
 const {g}=setup(.10);attackEvidence(g,{advantage:0});noWrite(g,()=>doEffect(g,'alderaan_thranta'));
 const c=clone(g.state.combat);c.pendingPlayerSpend.remainingAdvantage=2;g.commit('COMBAT_UPDATED',{combat:c});
 noWrite(g,()=>g.applyJewelHoloParkEffect({actionId:'unboarded',effect:'alderaan_thranta',gmSymbolConfirmed:true}));
 doEffect(g,'alderaan_thranta');
 const w=clone(g.state.world);w.publishedAdventure.operations.receipts['effect-1'].result.rangedBoost=7;
 assert.throws(()=>validateJewelHoloEffects(w.publishedAdventure),/adulterad/);
 noWrite(g,()=>doEffect(g,'christophsis_shadow','wrong-landscape'));
 noWrite(g,()=>doEffect(g,'alderaan_thranta','new-id'));
});
test('P211 actual attack controller consumes thranta Boost in the real narrative dice pool',()=>{
 const {g}=setup(.10);attackEvidence(g,{advantage:1});doEffect(g,'alderaan_thranta');
 const ch=clone(g.state.player.character);assert.ok(ch.weapons?.length,'starter has real weapons');
 const c=clone(g.state.combat);c.round++;c.pendingPlayerSpend=null;c.playerTurn={...c.playerTurn,actionUsed:false};
 g.commit('COMBAT_UPDATED',{combat:c});
 const idx=ch.weapons.findIndex(w=>!['pelea','armas_cuerpo_a_cuerpo'].includes(w.skill));assert.ok(idx>=0);
 const out=g.resolveWeaponAttack({weaponIndex:idx,targetAdversaryId:g.state.combat.adversaries[0].id,rng:()=>.1});
 assert.ok(out.check.pool.boost>=1,'real attack pool must include the thranta Boost');
});
test('P211 Android source and generated bundle expose both effects without altering Kokoro',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,token] of [['r46/final-runtime.js','alderaan_thranta'],['r46/final-runtime.js','christophsis_shadow'],['js/core/controller.mjs','holoParkShadowDefense'],['js/core/controller.mjs','holoParkThrantaBoost'],['js/core/uiBridge.bundle.js','holoParkThrantaBoost']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(token),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10211);
});
test('P211 Christophsis shadow adds one setback to the ACTUAL player ranged attack pool',()=>{
 const {g}=setup(.30),id=attackEvidence(g,{advantage:1});
 doEffect(g,'christophsis_shadow');
 const before=clone(g.state),index=g.state.player.character.weapons.findIndex(w=>!['pelea','armas_cuerpo_a_cuerpo'].includes(w.skill));
 assert.ok(index>=0);
 const ready=clone(g.state.combat);ready.round++;ready.pendingPlayerSpend=null;ready.playerTurn={...ready.playerTurn,actionUsed:false};
 g.commit('COMBAT_UPDATED',{combat:ready});
 const withShadow=g.resolveWeaponAttack({weaponIndex:index,targetAdversaryId:id,rng:()=>.1});
 const base=clone(before.combat);base.pendingPlayerSpend=null;base.playerTurn={...base.playerTurn,actionUsed:false};
 base.adversaries=base.adversaries.map(a=>a.id===id?{...a,holoParkShadow:undefined}:a);
 g.commit('COMBAT_UPDATED',{combat:base});
 g.commit('CHARACTER_UPDATED',{character:before.player.character});
 const without=g.resolveWeaponAttack({weaponIndex:index,targetAdversaryId:id,rng:()=>.1});
 assert.equal(withShadow.check.pool.setback,without.check.pool.setback+1);
});
