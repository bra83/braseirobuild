import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
import {validateJewelHoloEffects,holoParkSpotlightSetback,holoParkTransportSetback,JEWEL_HOLO_EFFECT_RULES} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone, adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo {constructor(){this.state=createCampaign('P213');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
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


function realNpcThreat(g,roll=.4){
 const c=g.state.combat,actor=c.adversaries.find(x=>x.kind==='minion'&&!x.wounds?.incapacitated)||c.adversaries[0];
 const out=g._resolveNpcTurn(c,g.state.player.character,actor.id,()=>roll,g.state.group);
 assert.ok(out.attack.check.netAdvantage<0,'real NPC attack must roll actual Threat');
 const receipt=out.combat.holoParkPendingNpcThreat?.at(-1);
 assert.ok(receipt&&receipt.attackerId===actor.id);
 const entry=out.combat.log.find(x=>x.type==='TURNO_NPC'&&x.attackId===receipt.attackId);
 assert.deepEqual(receipt.check.faces,entry.check.faces);
 g.commit('COMBAT_ACTION',{combat:out.combat,character:out.character});
 return receipt;
}
function spendNpc(g,e,pending,extras={}){
 return g.applyJewelHoloParkEffect({actionId:'p213-'+e,effect:e,gmSymbolConfirmed:true,
  npcAttackId:pending.attackId,...extras});
}
test('P213 official PDF NPC Threat effects have separate landscapes and do not fake PC Advantage',()=>{
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.christophsis_spotlight,{landscape:'christophsis',symbol:'threat',cost:1,kind:'npc-spotlight'});
 assert.deepEqual(JEWEL_HOLO_EFFECT_RULES.coruscant_transport,{landscape:'coruscant',symbol:'threat',cost:1,kind:'npc-transport'});
});
test('P213 actual NPC Threat is RESERVED and cannot also cause generic NPC strain',()=>{
 const {g}=setup(.40),c=g.state.combat,a=c.adversaries[0];
 const out=g._resolveNpcTurn(c,g.state.player.character,a.id,()=>.4,g.state.group);
 assert.equal(out.attack.check.netAdvantage,-2);
 assert.equal(out.combat.holoParkPendingNpcThreat.length,1);
 // The old engine would apply 2 strain for two Threat. One is reserved for landscape.
 const old=a.strain?.current??0,newValue=out.combat.adversaries.find(x=>x.id===a.id).strain?.current??0;
 assert.ok(newValue-old<=1,'one threat must not be double-spent');
});
test('P213 Christophsis spotlight applies REAL setback to NPC attacks against PC, then expires',()=>{
 const {g,repo}=setup(.40),pending=realNpcThreat(g);
 const applied=spendNpc(g,'christophsis_spotlight',pending);
 assert.equal(applied.adjudication.npcCheck.netAdvantage,pending.check.netAdvantage);
 assert.equal(holoParkSpotlightSetback(g.state.combat),1);
 const c=g.state.combat,actor=c.adversaries[0].id;
 const withEffect=g._resolveNpcTurn(c,g.state.player.character,actor,()=>.5,g.state.group);
 const noEffect=clone(c);delete noEffect.holoParkSpotlight;
 const baseline=g._resolveNpcTurn(noEffect,g.state.player.character,actor,()=>.5,g.state.group);
 assert.equal(withEffect.attack.check.pool.setback,baseline.attack.check.pool.setback+1);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
 // A real player initiative turn ends the illumination; the receipt remains.
 const next=clone(g.state.combat);next.encounter.cursor=next.encounter.slots.findIndex(x=>x.side==='player');
 next.encounter.waitingForPlayer=true;next.encounter.outcome=null;g.commit('COMBAT_UPDATED',{combat:next});
 g.finishPlayerInitiativeTurn({rng:()=>.5});
 assert.equal(holoParkSpotlightSetback(g.state.combat),0);
});
test('P213 Coruscant transport adds one setback to actual player ranged combat pool, clears on real movement',()=>{
 const {g,repo}=setup(.60),pending=realNpcThreat(g);
 spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[]});
 assert.equal(holoParkTransportSetback(g.state.combat),1);
 const c=clone(g.state.combat);c.pendingPlayerSpend=null;c.playerTurn={...c.playerTurn,maneuversUsed:0};
 c.encounter.cursor=c.encounter.slots.findIndex(x=>x.side==='player');c.encounter.waitingForPlayer=true;c.encounter.outcome=null;
 g.commit('COMBAT_UPDATED',{combat:c});
 const adversary=g.state.combat.adversaries[0];
 // The actual player combat roll must include the transport setback.
 const baseline=clone(g.state.combat);delete baseline.holoParkTransport;
 assert.equal(holoParkTransportSetback(baseline),0);
 const id=adversary.id;
 g.changeCombatRange({targetAdversaryId:id,direction:'closer'});
 assert.equal(holoParkTransportSetback(g.state.combat),0);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
test('P213 NPC Threat cannot be forged, reused, or transferred between landscapes',()=>{
 const {g}=setup(.40),pending=realNpcThreat(g);
 noWrite(g,()=>spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[]}));
 noWrite(g,()=>g.applyJewelHoloParkEffect({actionId:'no-npc',effect:'christophsis_spotlight',gmSymbolConfirmed:true,npcAttackId:'fake'}));
 spendNpc(g,'christophsis_spotlight',pending);
 noWrite(g,()=>spendNpc(g,'christophsis_spotlight',pending));
 const w=clone(g.state.world);w.publishedAdventure.operations.receipts['p213-christophsis_spotlight'].npcCheck.faces[0].index=999;
 assert.throws(()=>validateJewelHoloEffects(w.publishedAdventure),/adulterad/);
});
test('P213 Coruscant companion targets must be real group members, and only their actual combat skill gets setback',()=>{
 const {g}=setup(.60),pending=realNpcThreat(g);
 noWrite(g,()=>spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:['invented-companion']}));
 spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[]});
 assert.equal(holoParkTransportSetback(g.state.combat,'nonexistent'),0);
});
test('P213 Android UI, controller, bridge and offline build contain both NPC Threat effects',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const token of ['christophsis_spotlight','coruscant_transport','data-p213-npc-attack','data-p213-companion-gm'])
  assert.ok(readFileSync(new URL('r46/final-runtime.js',root),'utf8').includes(token),token);
 assert.ok(readFileSync(new URL('js/core/uiBridge.mjs',root),'utf8').includes('confirmJewelHoloCompanionMovement'));
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode = (\d+)/)?.[1])>=10213);
});
test('P213 actual player attack consumes Coruscant transport setback die, not just a UI flag',()=>{
 const {g,repo}=setup(.60),pending=realNpcThreat(g);
 spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[]});
 const c=clone(g.state.combat);c.pendingPlayerSpend=null;c.playerTurn={...c.playerTurn,actionUsed:false,maneuversUsed:0};
 c.encounter.cursor=c.encounter.slots.findIndex(x=>x.side==='player');c.encounter.waitingForPlayer=true;c.encounter.outcome=null;
 g.commit('COMBAT_UPDATED',{combat:c});
 const otherRepo=new Repo();otherRepo.state=clone(repo.state);delete otherRepo.state.combat.holoParkTransport;
 const other=new GameController(otherRepo);
 const options={weaponIndex:0,targetAdversaryId:c.adversaries[0].id,rng:()=>.5};
 const actual=g.resolveWeaponAttack(options),baseline=other.resolveWeaponAttack(options);
 assert.equal(actual.check.pool.setback,baseline.check.pool.setback+1);
});
test('P213 companion transport is individual: real combat skill setback and GM movement clears only selected companion',()=>{
 const {g,repo}=setup(.60);
 const first=g.addCompanion({name:'Aliado de teste A'}),second=g.addCompanion({name:'Aliado de teste B'});
 const pending=realNpcThreat(g);
 spendNpc(g,'coruscant_transport',pending,{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[first.id,second.id]});
 assert.equal(holoParkTransportSetback(g.state.combat,first.id),1);
 assert.equal(holoParkTransportSetback(g.state.combat,second.id),1);
 const attack=g.resolveCompanionSkillCheck({companionId:first.id,skillKey:'armas_distancia_ligeras',rng:()=>.5});
 const nonCombat=g.resolveCompanionSkillCheck({companionId:first.id,skillKey:'percepcion',rng:()=>.5});
 assert.equal(attack.pool.setback,1);
 assert.equal(nonCombat.pool.setback,0);
 noWrite(g,()=>g.confirmJewelHoloCompanionMovement({companionId:first.id,gmMovementConfirmed:false}));
 g.confirmJewelHoloCompanionMovement({companionId:first.id,gmMovementConfirmed:true});
 assert.equal(holoParkTransportSetback(g.state.combat,first.id),0);
 assert.equal(holoParkTransportSetback(g.state.combat,second.id),1);
 noWrite(g,()=>g.confirmJewelHoloCompanionMovement({companionId:first.id,gmMovementConfirmed:true}));
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
});
