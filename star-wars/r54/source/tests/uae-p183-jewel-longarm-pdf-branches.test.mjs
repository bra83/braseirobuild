import test from 'node:test';
import assert from 'node:assert/strict';
import { JEWEL_OF_YAVIN, startJewelOfYavin, adjudicateJewelOfYavinCheck, advanceJewelOfYavin } from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import { LONG_ARM_OF_THE_HUTT, startLongArmOfTheHutt } from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import { getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {arriveAtNabat,advanceNabatMorning,resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';

const jewel = getPublishedAdventureAdapter('jewel-of-yavin');
const arm = getPublishedAdventureAdapter('long-arm-of-the-hutt');
const go = (a,w,id)=>a===arm&&id==='arrive-nabat'?arriveAtNabat(a,w):a===arm&&id==='travel-lylek-den'?advanceNabatMorning(a,w):a.advance(a===arm&&id==='reach-new-meen'?resolveOldLylekRetreat(a,w):w,id).world;
const chain=(a,w,ids)=>ids.reduce((s,id)=>go(a,s,id),w);
// This P183 regression intentionally exercises the P183 manual-check save format.
const toBank=w=>{
 const old=chain(jewel,w,['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine','bank-operation']);
 delete old.publishedAdventure.bankContractVersion;
 return old;
};
const toVault=w=>go(jewel,toBank(w),'skip-bank-transfer');

// Official Drive source: The Jewel of Yavin, SWE09, introduction and TOC;
// Long Arm of the Hutt, SWE02, adventure summary and Acts 1-3.
test('P183 source: Jewel SWE09 opens with Aris in Port Town, not at the museum',()=>{
  assert.equal(JEWEL_OF_YAVIN.source.productCode,'SWE09');
  assert.equal(JEWEL_OF_YAVIN.startNodeId,'port-town-rendezvous');
  assert.equal(JEWEL_OF_YAVIN.nodes['port-town-rendezvous'].visibleNpcIds.includes('joy.npc.aris_shen'),true);
  assert.ok(Object.keys(JEWEL_OF_YAVIN.nodes).length>=15);
});
test('P183 source: the Grand Prix precedes the auction and central bank slice',()=>{
  const w=toBank(jewel.start({}).world);
  const p=w.publishedAdventure;
  assert.equal(p.currentNodeId,'central-bank-slice');
  assert.ok(p.visitedNodeIds.indexOf('grand-prix-race')<p.visitedNodeIds.indexOf('cloud-nine-party'));
  assert.ok(p.visitedNodeIds.indexOf('cloud-nine-party')<p.visitedNodeIds.indexOf('central-bank-slice'));
});
test('P183: bank transfer cannot be claimed by clicking without a successful roll',()=>{
  const w=toBank(jewel.start({}).world);
  assert.equal(jewel.status(w).node.choices.some(x=>x.id==='divert-winning-bid'),false);
  assert.throws(()=>jewel.advance(w,'divert-winning-bid'),/inválida/);
  assert.equal(w.publishedAdventure.flags.bankDiversionSecured,undefined);
});
test('P183: failed bank slice increments alarm, but does not divert credits',()=>{
  const w=toBank(jewel.start({}).world);
  const f=jewel.adjudicateCheck(w,{checkIndex:0,result:{success:false}}).world;
  assert.equal(f.publishedAdventure.clocks.heistAlarm.value,1);
  assert.equal(jewel.status(f).node.choices.some(x=>x.id==='divert-winning-bid'),false);
  assert.equal(w.publishedAdventure.clocks.heistAlarm.value,0);
});
test('P183: successful bank slice authorizes one explicit diversion flag',()=>{
  const w=toBank(jewel.start({}).world);
  const f=jewel.adjudicateCheck(w,{checkIndex:0,result:{success:true}}).world;
  const next=go(jewel,f,'divert-winning-bid');
  assert.equal(next.publishedAdventure.flags.bankDiversionSecured,true);
  assert.equal(next.publishedAdventure.flags.jewelAcquired,undefined);
});
test('P183: the vault cannot yield the jewel without an adjudicated successful check',()=>{
  const w=toVault(jewel.start({}).world);
  assert.equal(jewel.status(w).node.choices.some(x=>x.id==='take-jewel'),false);
  assert.throws(()=>jewel.advance(w,'take-jewel'),/inválida/);
  const f=jewel.adjudicateCheck(w,{checkIndex:0,result:{success:false}}).world;
  assert.equal(jewel.status(f).node.choices.some(x=>x.id==='take-jewel'),false);
});
test('P183: successful vault check unlocks acquisition; final state survives three reopens',()=>{
  const w=toVault(jewel.start({}).world);
  const f=jewel.adjudicateCheck(w,{checkIndex:0,result:{success:true}}).world;
  let n=go(jewel,f,'take-jewel');
  assert.equal(n.publishedAdventure.flags.jewelAcquired,true);
  assert.equal(jewel.status(n).node.choices.some(x=>x.id==='reach-hangar'),false);
  assert.throws(()=>go(jewel,n,'reach-hangar'),/inválida/);
  assert.equal(n.publishedAdventure.status,'ACTIVE');
  for(let i=0;i<3;i++){
    n=structuredClone(JSON.parse(JSON.stringify(n)));
    const status=jewel.status(n);
    assert.equal(status.active,true);
    assert.equal(status.progress.flags.jewelAcquired,true);
    const m=publishedAdventureRuntimeModule(jewel.meta);
    assert.equal(validatePublishedAdventureRuntimeState(m,n.publishedAdventureRuntime,n.publishedAdventure),true);
    assert.deepEqual(AdventureRuntime.restore(m,JSON.stringify(n.publishedAdventureRuntime)).getState(),n.publishedAdventureRuntime);
  }
});
test('P183: withdrawing from the vault completes without inventing the jewel',()=>{
  let w=toVault(jewel.start({}).world);
  w=go(jewel,w,'retreat-without-jewel');
  assert.equal(jewel.status(w).node.choices.some(x=>x.id==='reach-hangar'),false);
  w=go(jewel,w,'reach-hangar-empty');
  assert.equal(w.publishedAdventure.status,'COMPLETED');
  assert.notEqual(w.publishedAdventure.flags.jewelAcquired,true);
});
test('P183: malformed check result is rejected without mutation',()=>{
  const w=toVault(jewel.start({}).world);
  const before=structuredClone(w);
  assert.throws(()=>jewel.adjudicateCheck(w,{checkIndex:0,result:{success:'false'}}),/verdadeiro ou falso/);
  assert.deepEqual(w,before);
});
test('P183: Long Arm contains source-based New Meen, Ota, Piddock and Dimmock route',()=>{
  const nodes=LONG_ARM_OF_THE_HUTT.nodes;
  for(const id of ['nabat-contact','lylek-den-ambush','new-meen-miners','ota-briefing','piddock-party','dimmock-negotiation']) assert.ok(nodes[id],id);
  assert.ok(Object.keys(nodes).length>=14);
});
test('P183: Long Arm investigation path persists into Geonosis, and retains legacy choices',()=>{
  let w=arm.start({}).world;
  w=chain(arm,w,['arrive-nabat','travel-lylek-den','reach-new-meen','meet-bothan-ota','follow-ota-geonosis','attend-piddock-party','speak-with-dimmock','geonosis-final-evidence']);
  assert.equal(w.publishedAdventure.currentNodeId,'geonosis-surface');
  assert.ok(w.publishedAdventure.visitedNodeIds.includes('new-meen-miners'));
  assert.ok(w.publishedAdventure.visitedNodeIds.includes('dimmock-negotiation'));
  const old=chain(arm,arm.start({}).world,['dock-ryloth','head-to-geonosis']);
  assert.equal(old.publishedAdventure.currentNodeId,'geonosis-approach');
});
test('P183: published reward policies do not fabricate fixed credits or Obligation',()=>{
  for(const a of [arm,jewel]) {
    assert.equal(a.rewardPolicy.credits,0);
    assert.equal(a.rewardPolicy.obligationReduction,0);
  }
});

test('P183: forged Long Arm encounter cannot defeat Teemo or skip the scene',()=>{
  const w=chain(arm,arm.start({}).world,['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return','confront-teemo']);
  assert.equal(w.publishedAdventure.currentNodeId,'teemo-showdown');
  assert.throws(()=>arm.advance(w,'teemo-resolved'),/inválida/);
  assert.throws(()=>arm.markEncounterActive(w,'forged-encounter'),/incompatível/);
  assert.throws(()=>arm.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated'}),/não iniciado/);
  assert.equal(w.publishedAdventure.flags.teemoDefeated,undefined);
});
test('P183: Long Arm can retreat without claiming Teemo defeated',()=>{
  const w=chain(arm,arm.start({}).world,['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return','confront-teemo','withdraw-from-palace']);
  assert.equal(w.publishedAdventure.status,'COMPLETED');
  assert.equal(w.publishedAdventure.currentNodeId,'laoth-retreat');
  assert.notEqual(w.publishedAdventure.flags.teemoDefeated,true);
});
test('P183: Teemo defeat needs active encounter, valid outcome and cannot be duplicated',()=>{
  let w=chain(arm,arm.start({}).world,['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return','confront-teemo']);
  // P236 source-backed upgrade: old bare victory without combat proof is forbidden.
  w=structuredClone(w);
  w.publishedAdventure.flags={...w.publishedAdventure.flags,confirmedPartySize:3,palaceAdditionalHenchmenCount:2,gmHuttProfileConfirmed:true};
  w=arm.markEncounterActive(w,'teemo-palace-guards');
  assert.throws(()=>arm.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'forged'}),/inválido/);
  assert.throws(()=>arm.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated'}),/grupos reais/);
  const adversaries=arm.encounterSpec(w,'teemo-palace-guards').adversaries.map((entry,i)=>({id:`p183-teemo-${i}`,profileKey:entry.profileKey,status:'incapacitated'}));
  const resolved=arm.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated',adversaries}).world;
  assert.equal(resolved.publishedAdventure.flags.teemoDefeated,true);
  assert.throws(()=>arm.recordEncounterOutcome(resolved,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated'}),/incompatível/);
  const completed=go(arm,resolved,'teemo-resolved');
  assert.equal(completed.publishedAdventure.status,'COMPLETED');
  assert.equal(w.publishedAdventure.flags.teemoDefeated,undefined);
});
test('P183: Jewel security encounter requires actual start and cannot be repeated',()=>{
  let w=toVault(jewel.start({}).world);
  w=go(jewel,w,'retreat-without-jewel');
  assert.throws(()=>jewel.recordEncounterOutcome(w,{encounterId:'bespin-security-intercept',outcome:'ended'}),/não iniciado/);
  w=jewel.markEncounterActive(w,'bespin-security-intercept');
  const resolved=jewel.recordEncounterOutcome(w,{encounterId:'bespin-security-intercept',outcome:'ended'}).world;
  assert.throws(()=>jewel.recordEncounterOutcome(resolved,{encounterId:'bespin-security-intercept',outcome:'ended'}),/incompatível/);
  assert.equal(resolved.publishedAdventure.encounterHistory.length,1);
});

test('P183: P182 Jewel save at original Bespin node migrates without losing progress',()=>{
  const old=structuredClone(startJewelOfYavin({}).world);
  old.publishedAdventure.currentNodeId='bespin-arrival';
  old.publishedAdventure.visitedNodeIds=['bespin-arrival'];
  old.publishedAdventure.checkHistory=[];
  old.publishedAdventureRuntime=undefined;
  const status=jewel.status(old);
  assert.equal(status.node.id,'bespin-arrival');
  assert.equal(old.publishedAdventureRuntime,undefined);
  const next=jewel.advance(old,'prepare-heist');
  assert.equal(next.progress.currentNodeId,'heist-homework');
  assert.ok(next.world.publishedAdventureRuntime.canonicalEvents.some(e=>e.type==='LEGACY_SNAPSHOT_MIGRATED'));
  assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(jewel.meta),next.world.publishedAdventureRuntime,next.world.publishedAdventure),true);
});
