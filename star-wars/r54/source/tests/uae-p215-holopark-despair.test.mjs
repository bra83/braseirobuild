import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelHoloEffects,holoParkFountainDefense,assertJewelHoloTargetVisible} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P215');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(roll){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=6;ch.skills.mecanica=5;ch.characteristics.voluntad=6;ch.skills.frialdad=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const w=clone(g.state.world),p=w.publishedAdventure;
 p.encounterHistory=[...(p.encounterHistory??[]),{encounterId:'arend-shen-four-goons',outcome:'adversaries_defeated',nodeId:p.currentNodeId}];
 p.activeEncounterId=null;g.commit('WORLD_UPDATED',{world:w});
 g.beginJewelHoloParkWingGuard({actionId:'wing-holo-start',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>roll});
 const restored=clone(g.state.player.character);restored.derived.wounds.current=0;restored.derived.strain.current=0;
 restored.combatEffects={...(restored.combatEffects??{}),dodgeNextIncoming:{difficultyUpgrades:1}};
 g.commit('CHARACTER_UPDATED',{character:restored});
 return {g,repo};
}
function npcDespair(g){
 const c=g.state.combat,a=c.adversaries.find(x=>x.kind==='minion'&&!x.wounds?.incapacitated)||c.adversaries[0];
 const out=g._resolveNpcTurn(c,g.state.player.character,a.id,()=>.999,g.state.group);
 assert.ok(out.attack.check.despair>=1,'actual Challenge die Despair required');
 const pending=out.combat.holoParkPendingNpcDespair?.at(-1);
 assert.ok(pending,'NPC Despair must be reserved from actual roll');
 g.commit('COMBAT_ACTION',{combat:out.combat,character:out.character});return pending;
}
function unchanged(g,fn){const state=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,saves);}
test('P215 Christophsis: actual NPC Despair gives selected real NPC ranged defense 2 until confirmed movement',()=>{
 const {g,repo}=game(.3),pending=npcDespair(g),target=g.state.combat.adversaries[0];
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'no-gm',effect:'christophsis_fountain',gmSymbolConfirmed:true,npcAttackId:pending.attackId,targetId:target.id}));
 const result=g.applyJewelHoloParkEffect({actionId:'fountain',effect:'christophsis_fountain',gmSymbolConfirmed:true,npcAttackId:pending.attackId,targetId:target.id,gmTargetPresentConfirmed:true});
 assert.equal(result.adjudication.result.rangedDefense,2);
 const npc=g.state.combat.adversaries.find(x=>x.id===target.id);
 assert.equal(holoParkFountainDefense(npc,{skill:'armas_distancia_ligeras'}),2);
 assert.equal(holoParkFountainDefense(npc,{skill:'pelea'}),0);
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'duplicate',effect:'christophsis_fountain',gmSymbolConfirmed:true,npcAttackId:pending.attackId,targetId:target.id,gmTargetPresentConfirmed:true}));
 unchanged(g,()=>g.confirmJewelHoloNpcFountainMovement({targetId:target.id}));
 g.confirmJewelHoloNpcFountainMovement({targetId:target.id,gmMovementConfirmed:true});
 assert.equal(holoParkFountainDefense(g.state.combat.adversaries.find(x=>x.id===target.id),{skill:'armas_distancia_ligeras'}),0);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
 const fake=clone(g.state.world.publishedAdventure);fake.operations.receipts.fountain.result.rangedDefense=99;
 assert.throws(()=>validateJewelHoloEffects(fake),/adulterad/);
});
test('P215 Coruscant: real Despair triggers opposed NPC Stealth vs real player Perception and blocks unseen targeting',()=>{
 const {g,repo}=game(.55),pending=npcDespair(g),target=g.state.combat.adversaries[0];
 // The NPC's actual skill is read from its live sheet; a deterministic success roll does not alter that sheet.
 const r=g.applyJewelHoloParkEffect({actionId:'vanish',effect:'coruscant_vanish',gmSymbolConfirmed:true,npcAttackId:pending.attackId,targetId:target.id,gmTargetPresentConfirmed:true,rng:()=>.80});
 assert.ok(r.adjudication.result.check.faces.length>0);
 assert.equal(r.adjudication.result.hidden,true,'opposed Stealth must succeed in this deterministic test');
 assert.equal(r.adjudication.result.hidden,r.adjudication.result.check.success);
 const now=g.state.combat.adversaries.find(x=>x.id===target.id);
 if(r.adjudication.result.hidden){
  assert.throws(()=>assertJewelHoloTargetVisible(now),/oculto/);
  unchanged(g,()=>g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id}));
  unchanged(g,()=>g.confirmJewelHoloNpcVisibility({targetId:target.id}));
  g.confirmJewelHoloNpcVisibility({targetId:target.id,gmVisibleConfirmed:true});
  assert.doesNotThrow(()=>assertJewelHoloTargetVisible(g.state.combat.adversaries.find(x=>x.id===target.id)));
 }
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
 const fake=clone(g.state.world.publishedAdventure);fake.operations.receipts.vanish.result.check.faces[0].index=99;
 assert.throws(()=>validateJewelHoloEffects(fake),/adulterad/);
});
test('P215 reserved Despair not spent on landscape has GM-only fallback and no double spend',()=>{
 const {g,repo}=game(.3),pending=npcDespair(g);
 unchanged(g,()=>g.settleJewelHoloNpcDespair({npcAttackId:pending.attackId}));
 const before=g.state.combat.adversaries.find(a=>a.id===pending.attackerId).wounds.current;
 const r=g.settleJewelHoloNpcDespair({npcAttackId:pending.attackId,gmFallbackConfirmed:true});
 assert.equal(r.adjudication.amount,1);
 const after=g.state.combat.adversaries.find(a=>a.id===pending.attackerId).wounds.current;
 assert.equal(after,before+1);
 unchanged(g,()=>g.settleJewelHoloNpcDespair({npcAttackId:pending.attackId,gmFallbackConfirmed:true}));
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'double',effect:'christophsis_fountain',gmSymbolConfirmed:true,
  npcAttackId:pending.attackId,targetId:g.state.combat.adversaries[0].id,gmTargetPresentConfirmed:true}));
 for(let i=0;i<3;i++)assert.ok(new GameController(repo).state.combat.log.some(x=>x.type==='JEWEL_HOLO_NPC_DESPAIR_FALLBACK'));
});
test('P215 source contracts, Android UI and bridge integration',()=>{
 const base=new URL('../app/src/main/assets/vtt/',import.meta.url);
 const ui=readFileSync(new URL('r46/final-runtime.js',base),'utf8');
 for(const s of ['christophsis_fountain','coruscant_vanish','data-p215-npc-despair','data-p215-end-fountain','data-p215-end-hidden'])assert.ok(ui.includes(s),s);
 const bridge=readFileSync(new URL('js/core/uiBridge.mjs',base),'utf8');
 for(const s of ['confirmJewelHoloNpcFountainMovement','confirmJewelHoloNpcVisibility'])assert.ok(bridge.includes(s),s);
});
