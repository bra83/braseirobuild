import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
import {validateJewelHoloEffects,holoParkRangedCover,JEWEL_HOLO_EFFECT_RULES} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
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
 return g.applyJewelHoloParkEffect({actionId:id,effect:e,gmSymbolConfirmed:true});
}
function noWrite(g,fn){const state=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,n);}
test('P210 source-backed four real landscape symbols and effects are explicitly scoped',()=>{
 const rules=JEWEL_HOLO_EFFECT_RULES;
 assert.deepEqual(['alderaan_upheaval','christophsis_shatter','coruscant_speeder','kashyyyk_wroshyr'].map(id=>rules[id].landscape),['alderaan','christophsis','coruscant','kashyyyk']);
});

test('P210 Christophsis consumes actual Triumph and staggers real NPC one round',()=>{
 const {g,repo}=setup(.30),id=attackEvidence(g,{triumph:1});const before=repo.saves;
 const r=doEffect(g,'christophsis_shatter');assert.equal(repo.saves,before+1);
 assert.equal(r.adjudication.targetId,id);assert.equal(g.state.combat.pendingPlayerSpend.remainingTriumph,0);
 assert.ok(g.state.combat.adversaries.find(x=>x.id===id).statusEffects.some(x=>x.type==='staggered'));
 assert.equal(validateJewelHoloEffects(g.state.world.publishedAdventure).history.length,1);
 noWrite(g,()=>doEffect(g,'christophsis_shatter','effect-2'));
});
test('P210 Coruscant spends one real Advantage and disorients target',()=>{
 const {g}=setup(.60),id=attackEvidence(g,{advantage:2});doEffect(g,'coruscant_speeder');
 assert.equal(g.state.combat.pendingPlayerSpend.remainingAdvantage,1);
 assert.ok(g.state.combat.adversaries.find(x=>x.id===id).statusEffects.some(x=>x.type==='disoriented'));
});
test('P210 Kashyyyk consumes Advantage and adds one REAL ranged setback until next round',()=>{
 const {g}=setup(.80);attackEvidence(g,{advantage:1});doEffect(g,'kashyyyk_wroshyr');
 assert.equal(holoParkRangedCover(g.state.combat,{skill:'armas_distancia_pesadas'}),1);
 assert.equal(holoParkRangedCover(g.state.combat,{skill:'pelea'}),0);
 const c=clone(g.state.combat);c.round+=2;assert.equal(holoParkRangedCover(c,{skill:'armas_distancia_pesadas'}),0);
});
test('P210 Alderaan requires player attack Despair and applies real 2 strain plus lost maneuver',()=>{
 const {g}=setup(.02),c=clone(g.state.combat);c.playerTurn={actionUsed:false,maneuversUsed:0};
 c.playerTurn.actionUsed=true;c.lastAttack={attackId:'real-attack-despair',check:{despair:1}};
 g.commit('COMBAT_UPDATED',{combat:c});const before=g.state.player.character.derived.strain.current;
 doEffect(g,'alderaan_upheaval');assert.equal(g.state.player.character.derived.strain.current,before+2);
 assert.equal(g.state.combat.playerTurn.maneuversUsed,1);
});
test('P210 rejects scene mismatch, fabricated symbols, GM omission and duplicate receipts atomically',()=>{
 const {g}=setup(.30);attackEvidence(g,{triumph:0,advantage:1});
 noWrite(g,()=>doEffect(g,'christophsis_shatter'));
 noWrite(g,()=>doEffect(g,'coruscant_speeder'));
 noWrite(g,()=>g.applyJewelHoloParkEffect({actionId:'bad',effect:'christophsis_shatter',gmSymbolConfirmed:false}));
 const c=clone(g.state.combat);c.pendingPlayerSpend.remainingTriumph=1;g.commit('COMBAT_UPDATED',{combat:c});
 doEffect(g,'christophsis_shatter');noWrite(g,()=>doEffect(g,'christophsis_shatter'));
});
test('P210 receipts reject mutation and preserve legacy P209 saves without effects',()=>{
 const {g,repo}=setup(.60);assert.equal(validateJewelHoloEffects(g.state.world.publishedAdventure),null);
 attackEvidence(g,{advantage:1});doEffect(g,'coruscant_speeder');
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateJewelHoloEffects(reopened.state.world.publishedAdventure).history.length,1);}
 const w=clone(g.state.world);w.publishedAdventure.operations.receipts['effect-1'].result.rounds=4;
 assert.throws(()=>validateJewelHoloEffects(w.publishedAdventure),/adulterad/);
});
test('P210 Android bridge, source, UI, version and generated bundle expose the real effect',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [path,token] of [['r46/final-runtime.js','data-p210-holo-apply'],['js/core/uiBridge.mjs','applyJewelHoloParkEffect'],['js/core/controller.mjs','applyJewelHoloParkEffect'],['js/core/uiBridge.bundle.js','JEWEL_HOLO_EFFECT']])
  assert.ok(readFileSync(new URL(path,root),'utf8').includes(token),path);
 assert.ok(Number((readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode = (\d+)/)||[])[1])>=10210);
});
test('P210 Kashyyyk cover changes the ACTUAL NPC attack dice pool by one setback',()=>{
 const {g}=setup(.80);attackEvidence(g,{advantage:1});doEffect(g,'kashyyyk_wroshyr');
 const c=g.state.combat,id=c.adversaries.find(a=>a.kind==='minion'&&!a.wounds?.incapacitated)?.id??c.adversaries[0].id;
 const withCover=g._resolveNpcTurn(c,g.state.player.character,id,()=>.5,g.state.group);
 const without=clone(c);delete without.holoParkCover;
 const baseline=g._resolveNpcTurn(without,g.state.player.character,id,()=>.5,g.state.group);
 assert.equal(withCover.attack.check.pool.setback,baseline.attack.check.pool.setback+1);
});
