import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelHoloEffects,JEWEL_HOLO_EFFECT_RULES,assertJewelHoloWeaponUsable} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P214');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(landscapeRoll){
 const repo=new Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
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
 g.beginJewelHoloParkWingGuard({actionId:'wing-holo-start',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>landscapeRoll});
 const restored=clone(g.state.player.character);restored.derived.wounds.current=0;restored.derived.strain.current=0;g.commit('CHARACTER_UPDATED',{character:restored});
 return {g,repo};
}
function npcThreat(g){
 const c=g.state.combat,a=c.adversaries.find(x=>x.kind==='minion'&&!x.wounds?.incapacitated)||c.adversaries[0];
 const out=g._resolveNpcTurn(c,g.state.player.character,a.id,()=>.4,g.state.group);
 assert.ok(out.attack.check.netAdvantage<0,'NPC must roll actual Threat');
 const pending=out.combat.holoParkPendingNpcThreat?.at(-1);
 assert.ok(pending,'Threat should be reserved for GM');
 g.commit('COMBAT_ACTION',{combat:out.combat,character:out.character});return pending;
}
function unchanged(g,fn){const state=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,saves);}
for(const [landscape,roll,effect] of [['alderaan',.1,'alderaan_lost_weapon'],['kashyyyk',.8,'kashyyyk_lost_weapon']]){
 test(`P214 ${landscape} NPC Threat drops a real weapon and blocks its attack until actual recovery`,()=>{
  const {g,repo}=game(roll);assert.equal(g.state.world.publishedAdventure.operations.holoPark.landscape,landscape);
  const pending=npcThreat(g),index=0,name=g.state.player.character.weapons[index].name;
  unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'fake',effect,gmSymbolConfirmed:true,npcAttackId:pending.attackId,weaponIndex:index}));
  const applied=g.applyJewelHoloParkEffect({actionId:'drop-'+landscape,effect,gmSymbolConfirmed:true,npcAttackId:pending.attackId,weaponIndex:index,gmWeaponHeldConfirmed:true});
  assert.equal(applied.adjudication.result.weaponName,name);
  assert.equal(g.state.combat.holoParkDroppedWeapon.active,true);
  assert.equal(g.state.player.character.weapons[index].name,name,'item must remain in inventory');
  assert.throws(()=>assertJewelHoloWeaponUsable(g.state.combat,index),/Arma caída/);
  unchanged(g,()=>g.resolveWeaponAttack({weaponIndex:index,targetAdversaryId:g.state.combat.adversaries[0].id}));
  unchanged(g,()=>g.recoverJewelHoloWeapon({method:'strain',gmRecoveryConfirmed:false}));
  const before=g.state.player.character.derived.strain.current;
  g.recoverJewelHoloWeapon({method:'strain',gmRecoveryConfirmed:true});
  assert.equal(g.state.player.character.derived.strain.current,before+1);
  assert.equal(g.state.combat.holoParkDroppedWeapon.active,false);
  assert.doesNotThrow(()=>assertJewelHoloWeaponUsable(g.state.combat,index));
  unchanged(g,()=>g.recoverJewelHoloWeapon({method:'strain',gmRecoveryConfirmed:true}));
  for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
  const tampered=clone(g.state.world.publishedAdventure);tampered.operations.receipts['drop-'+landscape].result.weaponIndex=999;
  assert.throws(()=>validateJewelHoloEffects(tampered),/adulterad/);
 });
}
test('P214 unused NPC Threat falls back to one real strain/wound, cannot be double spent',()=>{
 const {g,repo}=game(.1),pending=npcThreat(g),before=clone(g.state.combat.adversaries.find(a=>a.id===pending.attackerId));
 unchanged(g,()=>g.settleJewelHoloNpcThreat({npcAttackId:pending.attackId}));
 const out=g.settleJewelHoloNpcThreat({npcAttackId:pending.attackId,gmFallbackConfirmed:true});
 const after=g.state.combat.adversaries.find(a=>a.id===pending.attackerId);
 assert.equal(out.adjudication.amount,1);
 assert.equal(after.wounds.current-(before.wounds.current??0),1);
 assert.ok(g.state.combat.log.some(x=>x.type==='JEWEL_HOLO_NPC_THREAT_FALLBACK'&&x.attackId===pending.attackId));
 unchanged(g,()=>g.settleJewelHoloNpcThreat({npcAttackId:pending.attackId,gmFallbackConfirmed:true}));
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'spent',effect:'alderaan_lost_weapon',gmSymbolConfirmed:true,npcAttackId:pending.attackId,weaponIndex:0,gmWeaponHeldConfirmed:true}));
 for(let i=0;i<3;i++)assert.ok(new GameController(repo).state.combat.log.some(x=>x.attackId===pending.attackId&&x.type==='JEWEL_HOLO_NPC_THREAT_FALLBACK'));
});
test('P214 rules, UI, bridge and offline bundle contain physical weapon loss and GM fallback',()=>{
 assert.equal(JEWEL_HOLO_EFFECT_RULES.alderaan_lost_weapon.kind,'npc-drop-weapon');
 assert.equal(JEWEL_HOLO_EFFECT_RULES.kashyyyk_lost_weapon.kind,'npc-drop-weapon');
 const base=new URL('../app/src/main/assets/vtt/',import.meta.url);
 const ui=readFileSync(new URL('r46/final-runtime.js',base),'utf8');
 for(const token of ['alderaan_lost_weapon','kashyyyk_lost_weapon','data-p214-recover','data-p214-fallback','data-p214-weapon-gm'])assert.ok(ui.includes(token),token);
 const bundle=readFileSync(new URL('js/core/uiBridge.bundle.js',base),'utf8');
 for(const token of ['recoverJewelHoloWeapon','settleJewelHoloNpcThreat'])assert.ok(bundle.includes(token),token);
});
