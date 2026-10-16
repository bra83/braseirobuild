import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {arriveAtNabat,advanceNabatMorning,resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {applyAdversaryDamage} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {applyDamage} from '../app/src/main/assets/vtt/js/rules/health.mjs';
import {livingNpcIds} from '../app/src/main/assets/vtt/js/rules/encounter.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo{constructor(){this.state=createCampaign('P242');this.saves=0;}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++;}}
function fixture({night=false,partySize=3}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:partySize,rng:()=>.9});
 let w=adapter.start(g.state.world).world;
 w=arriveAtNabat(adapter,w);w=advanceNabatMorning(adapter,w);w=resolveOldLylekRetreat(adapter,w,partySize);
 for(const id of ['reach-new-meen',night?'stay-new-meen-night':'confront-drombb-directly'])w=adapter.advance(w,id).world;
 if(!night){w=adapter.recordNewMeenLifecycle(w,'compound_approach',{precautions:true}).world;w=adapter.advance(w,'new-meen-enter-cantina').world;}
 w.publishedAdventure.flags.confirmedPartySize=partySize;g.state.world=w;repo.save(g.state);
 g.beginPublishedAdventureEncounter(night?'laoth-new-meen-night-raid':'laoth-drombb-cantina',{rng:()=>.9});
 return {g,repo};
}
function woundMinions(g,index=0,count=2){
 const c=structuredClone(g.state.combat),a=c.adversaries.filter(x=>x.profileKey==='laoth_drombb_thug')[index];
 const result=applyAdversaryDamage(a,{damage:a.soak+a.wounds.perMember*count+1});
 Object.assign(a,result.adversary);g.commit('COMBAT_ACTION',{combat:c});return a.id;
}
function reopen(repo){for(let i=0;i<3;i++){const g=new GameController(repo);assert.ok(adapter.status(g.state.world).active);}}
const rngSuccess=()=>.4,rngFail=()=>0;
test('P242 GM cannot declare deaths without live combat damage',()=>{
 const {g,repo}=fixture();const id=g.state.combat.adversaries[0].id,saves=repo.saves;
 assert.throws(()=>g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:1}),/excedem baixas/);
 assert.equal(repo.saves,saves);assert.equal(g.state.world.publishedAdventure.newMeenFatalityReceipts,undefined);
});
test('P242 GM confirmation bounded by actual incapacitated minions and never automatic',()=>{
 const {g,repo}=fixture();const id=woundMinions(g,0,2);
 assert.equal(g.state.world.publishedAdventure.newMeenFatalityReceipts,undefined);
 assert.throws(()=>g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:3}),/excedem baixas/);
 const r=g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:1});
 assert.equal(r.confirmedDeaths,1);assert.equal(r.verifiedCasualties,2);
 assert.throws(()=>g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:2}),/excedem baixas/);
 assert.equal(g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:1}).confirmedTotal,2);
 reopen(repo);
});
test('P242 1-4 confirmed deaths use Hard Coerce; success surrenders only thugs',()=>{
 const {g,repo}=fixture();const id=woundMinions(g,0,2);
 g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:2});
 // The controller may have an NPC initiative first; it must be the player's actual turn.
 const c=structuredClone(g.state.combat);c.encounter.waitingForPlayer=true;c.encounter.activeActorId='player';c.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c});
 const r=g.coerceSWE02NewMeenThugs({rng:rngSuccess});
 assert.equal(r.difficulty,3);assert.equal(r.success,true);assert.ok(r.surrenderedGroupIds.length>=1);
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.equal(g.state.combat.adversaries.at(-1).profileKey,'laoth_angu_drombb');
 assert.equal(g.state.combat.adversaries.at(-1).status,'active');
 assert.ok(livingNpcIds(g.state.combat).includes(g.state.combat.adversaries.at(-1).id));
 for(const id of r.surrenderedGroupIds)assert.ok(!livingNpcIds(g.state.combat).includes(id));
 reopen(repo);
});
test('P242 failed Coerce consumes player action, never surrenders enemies',()=>{
 const {g,repo}=fixture();const id=woundMinions(g,0,1);g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:1});
 const c=structuredClone(g.state.combat);c.encounter.waitingForPlayer=true;c.encounter.activeActorId='player';c.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c});
 const r=g.coerceSWE02NewMeenThugs({rng:rngFail});assert.equal(r.success,false);
 assert.equal(r.surrenderedGroupIds.length,0);assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.throws(()=>g.coerceSWE02NewMeenThugs({rng:rngSuccess}),/ação do turno/);reopen(repo);
});
test('P242 5-8 / 9+ difficulty thresholds are calculated from actual casualty-confirmed receipts',()=>{
 for(const [size,dead,expected] of [[3,5,2],[4,9,1]]){
  const {g}=fixture({partySize:size});let left=dead;
  for(let i=0;i<3&&left>0;i++){const n=Math.min(size,left),id=woundMinions(g,i,n);g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:n});left-=n;}
  const c=structuredClone(g.state.combat);c.encounter.waitingForPlayer=true;c.encounter.activeActorId='player';c.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c});
  const r=g.coerceSWE02NewMeenThugs({rng:rngSuccess});assert.equal(r.difficulty,expected);
 }
});
test('P242 night-raid verified fatalities carry into cantina only after combat proof',()=>{
 const {g,repo}=fixture({night:true});const id=woundMinions(g,0,3);
 const r=g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:3});assert.equal(r.confirmedTotal,3);
 const c=structuredClone(g.state.combat);c.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat:c});
 g.finishCombat({rng:()=>.9});assert.equal(g.state.world.publishedAdventure.flags.newMeenNightRaidStopped,true);
 g.advancePublishedAdventure('follow-drombb-raiders');g.recordSWE02NewMeenLifecycle('compound_approach',{precautions:true});g.advancePublishedAdventure('new-meen-enter-cantina');g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});
 const c2=structuredClone(g.state.combat);c2.encounter.waitingForPlayer=true;c2.encounter.activeActorId='player';c2.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c2});
 const coerced=g.coerceSWE02NewMeenThugs({rng:rngSuccess});assert.equal(coerced.confirmedDeaths,3);assert.equal(coerced.difficulty,3);
 reopen(repo);
});
test('P242 fake receipt and fake dice rejected by adapter status',()=>{
 const {g}=fixture();const id=woundMinions(g,0,2);g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:1});
 let w=structuredClone(g.state.world);w.publishedAdventure.newMeenFatalityReceipts[0].confirmedDeaths=9;
 assert.throws(()=>adapter.status(w),/adulterada|duplicadas/);
 const c=structuredClone(g.state.combat);c.encounter.waitingForPlayer=true;c.encounter.activeActorId='player';c.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c});g.coerceSWE02NewMeenThugs({rng:rngSuccess});
 w=structuredClone(g.state.world);w.publishedAdventure.newMeenCoercionReceipts[0].check.netSuccess=99;
 assert.throws(()=>adapter.status(w),/dados adulterados/);
});
test('P242 encounter closure accepts verified surrender only with Drombb incapacitated',()=>{
 const {g,repo}=fixture();const id=woundMinions(g,0,2);g.adjudicateSWE02NewMeenDeaths({groupId:id,confirmedDeaths:2});
 let c=structuredClone(g.state.combat);c.encounter.waitingForPlayer=true;c.encounter.activeActorId='player';c.playerTurn.actionUsed=false;g.commit('COMBAT_ACTION',{combat:c});g.coerceSWE02NewMeenThugs({rng:rngSuccess});
 c=structuredClone(g.state.combat);const boss=c.adversaries.at(-1);boss.wounds.current=boss.wounds.threshold+1;boss.status='incapacitated';
 c.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat:c});
 const done=g.finishCombat({rng:()=>.9});assert.equal(done.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(g.state.world.publishedAdventure.flags.drombbThreatNeutralized,true);
 assert.ok(g.state.world.publishedAdventure.encounterHistory.at(-1).combatProof.surrenderedGroupIds.length>0);
 reopen(repo);
});
test('P242 player incapacitated by Drombb opens Ota rescue without claiming liberation',()=>{
 const {g,repo}=fixture();const c=structuredClone(g.state.combat);c.encounter.outcome='player_incapacitated';g.commit('COMBAT_ACTION',{combat:c});
 assert.throws(()=>g.finishCombat({rng:()=>.9}),/sem incapacitação real/);
 const damaged=applyDamage(g.state.player.character,{damage:100,criticalRng:()=>0});g.commit('CHARACTER_UPDATED',{character:damaged.character});
 assert.equal(damaged.status.unconscious,true);
 const out=g.finishCombat({rng:()=>.9});assert.equal(out.publishedAdventureOutcome.outcome,'player_incapacitated');
 assert.equal(g.state.world.publishedAdventure.flags.drombbCaptured,true);
 assert.equal(g.state.world.publishedAdventure.flags.newMeenOutcome,'LOST');
 assert.equal(adapter.status(g.state.world).node.choices.some(x=>x.id==='drombb-ota-rescue'),true);
 g.advancePublishedAdventure('drombb-ota-rescue');
 assert.equal(g.state.world.publishedAdventure.currentNodeId,'drombb-ota-rescue');
 assert.notEqual(g.state.world.publishedAdventure.flags.newMeenOutcome,'SECURED');
 g.advancePublishedAdventure('ota-after-drombb-rescue');assert.equal(g.state.world.publishedAdventure.currentNodeId,'ota-briefing');
 reopen(repo);
});
test('P242 p23 Drombb safe grants exactly 2000 credits once after actual victory',()=>{
 const {g,repo}=fixture();const before=g.state.player.character.credits;
 assert.throws(()=>g.openSWE02DrombbSafe(),/encerre o combate/);
 const c=structuredClone(g.state.combat);
 for(const a of c.adversaries){a.wounds.current=a.wounds.threshold+1;a.status='incapacitated';}
 c.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat:c});g.finishCombat({rng:()=>.9});
 const receipt=g.openSWE02DrombbSafe({method:'key',gmConfirmedQuick:true});
 assert.equal(receipt.creditsGranted,2000);assert.equal(g.state.player.character.credits,before+2000);
 assert.throws(()=>g.openSWE02DrombbSafe({method:'key'}),/já investigado/);
 reopen(repo);
});
test('P242 safe Skulduggery Hard failed roll does not mint credits',()=>{
 const {g,repo}=fixture();const c=structuredClone(g.state.combat);
 for(const a of c.adversaries){a.wounds.current=a.wounds.threshold+1;a.status='incapacitated';}
 c.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat:c});g.finishCombat({rng:()=>.9});
 const before=g.state.player.character.credits;
 const receipt=g.openSWE02DrombbSafe({method:'skulduggery',gmConfirmedQuick:true,rng:()=>0});
 assert.equal(receipt.check.pool.difficulty,3);assert.equal(receipt.success,false);
 assert.equal(g.state.player.character.credits,before);reopen(repo);
});
test('P242 safe cannot be claimed after retreat or via forged credit receipt',()=>{
 const {g}=fixture();const c=structuredClone(g.state.combat);c.encounter.outcome='escaped';g.commit('COMBAT_ACTION',{combat:c});g.finishCombat({rng:()=>.9});
 assert.throws(()=>g.openSWE02DrombbSafe(),/somente após neutralizar/);
 const w=structuredClone(g.state.world);w.publishedAdventure.newMeenSafeReceipts=[{type:'NEW_MEEN_SAFE',source:{productCode:'SWE02',printedPage:23},nodeId:'drombb-cantina',method:'key',check:null,success:true,characterKey:'cael',gmConfirmedQuick:true,creditsGranted:2000,creditsRemaining:2000,combatEncounterId:'laoth-drombb-cantina'}];
 assert.throws(()=>adapter.status(w),/sem vitória real/);
});
test('P242 p23 safe accepts explosives and gates the 2000 credits on explicit GM race confirmation',()=>{
 const {g,repo}=fixture();const c=structuredClone(g.state.combat);
 for(const a of c.adversaries){a.wounds.current=a.wounds.threshold+1;a.status='incapacitated';}
 c.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat:c});g.finishCombat({rng:()=>.9});
 const before=g.state.player.character.credits;
 assert.throws(()=>g.openSWE02DrombbSafe({method:'blast'}),/Mestre deve confirmar/);
 const receipt=g.openSWE02DrombbSafe({method:'blast',gmConfirmedQuick:false});
 assert.equal(receipt.success,true);assert.equal(receipt.creditsGranted,0);assert.equal(g.state.player.character.credits,before);
 assert.equal(g.state.world.publishedAdventure.newMeenSafeReceipts[0].gmConfirmedQuick,false);reopen(repo);
});
test('P242 bridge and offline UI contain live confirmation and Coerce operations',()=>{
 const b=new BraseiroUiBridge({repository:new Repo()});
 for(const fn of ['adjudicateSWE02NewMeenDeaths','coerceSWE02NewMeenThugs','openSWE02DrombbSafe'])assert.equal(typeof b[fn],'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02NewMeenConfirmDeaths','swe02NewMeenCoerce','swe02NewMeenGroup','swe02DrombbSafeOpen','swe02NewMeenLifecycle','swe02NewMeenApproach','swe02DrombbSafeQuick','swe02NewMeenNightResolve','swe02NewMeenNightInteraction','swe02NewMeenNightResult','swe02NewMeenEscortRefuse','swe02NewMeenCoercionResolve','swe02NewMeenRoadHarassment','swe02NewMeenRetaliationResolve','swe02NewMeenDetectedResolve','swe02NewMeenAllyResolve','swe02NewMeenCharm','swe02NewMeenDefeatResolve','swe02NewMeenOfficeSearch','swe02NewMeenGmNote']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
 assert.doesNotMatch(ui,/window\.prompt\(/);
});
