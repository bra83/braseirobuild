import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelHoloEffects,consumeJewelHoloVertigo,augmentJewelKashyyykInsectRoll} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P216');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
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

test('P216 Kashyyyk: actual NPC Despair triggers Difficult Discipline, strain and next action downgrade',()=>{
 const {g,repo}=game(.86),pending=npcDespair(g);
 assert.equal(g.state.world.publishedAdventure.operations.holoPark.landscape,'kashyyyk');
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'no-gm',effect:'kashyyyk_vertigo',npcAttackId:pending.attackId}));
 const r=g.applyJewelHoloParkEffect({actionId:'vertigo',effect:'kashyyyk_vertigo',npcAttackId:pending.attackId,gmSymbolConfirmed:true,rng:()=>.02});
 assert.equal(r.adjudication.result.check.pool.difficulty,3);
 assert.equal(r.adjudication.result.check.success,false);
 assert.equal(r.adjudication.result.strainApplied,2);
 assert.equal(g.state.combat.holoParkVertigo.active,true);
 const consumed=consumeJewelHoloVertigo(g.state.combat,{ability:1,proficiency:2,difficulty:2});
 assert.equal(consumed.pool.proficiency,1);assert.equal(consumed.pool.ability,2);
 assert.equal(consumed.combat.holoParkVertigo.active,false);
 const beforeAction=clone(g.state.combat);
 const check=g.resolveSkillCheck({skillKey:'disciplina',difficulty:1,rng:()=>.45});
 assert.equal(g.state.combat.holoParkVertigo.active,false,'next real action must consume vertigo');
 assert.ok(g.state.combat.log.some(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE'));
 assert.equal(check.pool.proficiency,Math.max(0,Math.min(g.state.player.character.characteristics.voluntad,g.state.player.character.skills.disciplina)-1));
 assert.equal(consumeJewelHoloVertigo(consumed.combat,consumed.pool).downgraded,false);
 unchanged(g,()=>g.applyJewelHoloParkEffect({actionId:'duplicate',effect:'kashyyyk_vertigo',npcAttackId:pending.attackId,gmSymbolConfirmed:true}));
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
 const fake=clone(g.state.world.publishedAdventure);fake.operations.receipts.vertigo.result.check.faces[0].index=99;
 assert.throws(()=>validateJewelHoloEffects(fake),/adulterad/);
});

test('P216 insect: same attack roll gains exactly two real Boost dice only with original Triumph',()=>{
 const {g,repo}=game(.86),target=g.state.combat.adversaries[0];
 const original=clone(g.state);
 unchanged(g,()=>g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useKashyyykInsect:true}));
 const base={pool:{ability:1,proficiency:1,difficulty:1,boost:0,setback:1},faces:[
  {type:'ability',index:1,face:{success:1}},{type:'proficiency',index:11,face:{triumph:1}},
  {type:'difficulty',index:0,face:{}},{type:'setback',index:0,face:{}}],
  success:true,netSuccess:2,netAdvantage:0,triumph:1,despair:0};
 const rolled=augmentJewelKashyyykInsectRoll(base,()=>.5);
 assert.equal(rolled.check.pool.boost,2);assert.equal(rolled.proof.base.triumph,1);
 assert.equal(rolled.proof.extraBoostFaces.length,2);assert.equal(rolled.check.faces.length,6);
 assert.equal(g.state.world.publishedAdventure.operations.holoPark.landscape,'kashyyyk');
});
test('P216 insect integrated in actual weapon attack, atomic receipt and three reopens',()=>{
 const {g,repo}=game(.86);
 const ch=clone(g.state.player.character),target=g.state.combat.adversaries[0];
 const skill=ch.weapons[0].skill;
 ch.characteristics.agilidad=6;ch.skills[skill]=5;g.commit('CHARACTER_UPDATED',{character:ch});
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,
  useKashyyykInsect:true,gmInsectConfirmed:true,rng:()=>.99});
 assert.ok(attack.kashyyykInsectReceipt,'actual Triumph should produce same-roll receipt');
 assert.equal(attack.check.pool.boost,2);
 assert.equal(attack.kashyyykInsectReceipt.result.base.pool.boost,0);
 assert.equal(attack.kashyyykInsectReceipt.result.final.pool.boost,2);
 assert.equal(attack.pendingSpend.remainingTriumph,Math.max(0,attack.check.triumph-1));
 assert.equal(g.state.combat.lastAttack?.attackId,attack.attackId);
 for(let i=0;i<3;i++)assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure).history.length,1);
 const fake=clone(g.state.world.publishedAdventure);fake.operations.receipts[attack.kashyyykInsectReceipt.id].result.extraBoostFaces[0].index=100;
 assert.throws(()=>validateJewelHoloEffects(fake),/adulterad/);
});
test('P216 insect without original Triumph does not fabricate bonus or receipt',()=>{
 const {g,repo}=game(.86),target=g.state.combat.adversaries[0];
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,
  useKashyyykInsect:true,gmInsectConfirmed:true,rng:()=>.25});
 assert.equal(attack.kashyyykInsectReceipt,undefined);
 assert.equal(attack.check.pool.boost,0);
 assert.equal(validateJewelHoloEffects(new GameController(repo).state.world.publishedAdventure),null);
});
test('P216 vertigo downgrades actual next weapon attack and cannot affect a second roll',()=>{
 const {g}=game(.86),pending=npcDespair(g);
 const ch=clone(g.state.player.character);ch.characteristics.agilidad=6;ch.skills[ch.weapons[0].skill]=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.applyJewelHoloParkEffect({actionId:'vertigo-attack',effect:'kashyyyk_vertigo',npcAttackId:pending.attackId,gmSymbolConfirmed:true,rng:()=>.02});
 const target=g.state.combat.adversaries[0];
 const a=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>.5});
 assert.equal(a.check.pool.proficiency,4,'one proficiency downgraded into ability');
 assert.equal(a.check.pool.ability,2);
 assert.equal(g.state.combat.holoParkVertigo.active,false);
 assert.equal(g.state.combat.log.filter(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE').length,1);
});
