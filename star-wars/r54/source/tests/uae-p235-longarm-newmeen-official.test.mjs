import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {advanceNabatMorning,advanceNabatMorningController,resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
const getAdversaryProfile = key => PUBLISHED_ADVENTURE_ADVERSARY_PROFILES[key];
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const clone=x=>structuredClone(x);
const start=()=>a.start({}).world;
const step=(w,id)=>a.advance(w,id).world;
const arrive=()=>advanceNabatMorning(a,step(step(start(),'dock-ryloth'),'help-new-meen'));
const meen=()=>step(resolveOldLylekRetreat(a,arrive()),'reach-new-meen');
const withParty=(world,n)=>{const w=clone(world);w.publishedAdventure.flags.confirmedPartySize=n;return w;};
const enterCantina=w=>{w=step(a.recordNewMeenLifecycle(w,'compound_approach',{precautions:true}).world,'new-meen-enter-cantina');return a.recordNewMeenLifecycle(w,'cantina_start').world;};
const mark=(w,id)=>{const stage=w.publishedAdventure?.newMeenLifecycle?.stage;if(id==='laoth-new-meen-night-raid'&&stage==='NIGHT_STANDOFF')w=a.recordNewMeenLifecycle(w,'night_interaction',{response:'aggressive_move'}).world;if(id==='laoth-drombb-cantina'&&stage==='CANTINA_READY')w=a.recordNewMeenLifecycle(w,'cantina_start').world;return a.markEncounterActive(w,id);};
const members=(spec,dead=true)=>spec.adversaries.map((e,i)=>({id:`enemy-${i}`,profileKey:e.profileKey,status:dead?'incapacitated':'active',wounds:dead?500:0}));
const win=(world,id)=>{
 const spec=a.encounterSpec(world,id);
 const active=mark(world,id);
 return a.recordEncounterOutcome(active,{encounterId:id,outcome:'adversaries_defeated',adversaries:members(spec)}).world;
};

test('SWE02 print pp21-22: Drombb thug official minion stats, group skills and weapons',()=>{
 const p=getAdversaryProfile('laoth_drombb_thug');
 assert.equal(p.kind,'minion');assert.deepEqual(p.characteristics,{fortaleza:3,agilidad:2,intelecto:2,astucia:2,voluntad:1,presencia:1});
 assert.equal(p.soak,3);assert.equal(p.woundsThreshold,6);
 assert.deepEqual(p.groupSkills,['pelea','coaccion','armas_cuerpo_a_cuerpo','armas_distancia_ligeras']);
 assert.deepEqual(p.weapons.map(w=>[w.damage,w.crit]),[[6,3],[4,4]]);
 assert.equal(p.source.authority,'OFFICIAL_PDF_TEXT_VERIFIED');
});
test('SWE02 p22: Angu Drombb official rival stats; no improvised nemesis',()=>{
 const p=getAdversaryProfile('laoth_angu_drombb');
 assert.equal(p.kind,'rival');assert.equal(p.woundsThreshold,14);assert.equal(p.soak,3);
 assert.equal(p.characteristics.presencia,3);assert.equal(p.skills.coaccion,1);
});
test('SWE02 New Meen remains player choice; skipping never means community saved',()=>{
 const w=meen();const choices=a.status(w).node.choices.map(c=>c.id);
 assert.ok(choices.includes('meet-bothan-ota'));
 for(const id of ['confront-drombb-directly','stay-new-meen-night','escort-new-meen-evacuees'])assert.ok(choices.includes(id));
 const skipped=step(w,'meet-bothan-ota');
 assert.equal(skipped.publishedAdventure.flags.newMeenOutcome,'UNRESOLVED');
 assert.equal(skipped.publishedAdventure.flags.drombbThreatNeutralized,undefined);
});
test('SWE02 p19: party size 2, 3 and 4+ creates three groups of 2,3,4; no invented solo difficulty',()=>{
 for(const [size,groupCount] of [[2,2],[3,3],[4,4],[6,4]]){
  const w=withParty(step(meen(),'confront-drombb-directly'),size);
  const spec=a.encounterSpec(w,'laoth-drombb-cantina');
  assert.equal(spec.requiresPartySizeConfirmation,false);
  assert.deepEqual(spec.adversaries.slice(0,3).map(x=>x.count),[groupCount,groupCount,groupCount]);
  assert.equal(spec.adversaries.at(-1).profileKey,'laoth_angu_drombb');
 }
 for(const size of [undefined,1]){
  const w=step(meen(),'confront-drombb-directly');
  if(size!==undefined)w.publishedAdventure.flags.confirmedPartySize=size;
  assert.equal(a.encounterSpec(w,'laoth-drombb-cantina').requiresPartySizeConfirmation,true);
 }
});
test('SWE02 p20: night raid has one group and officially requires one setback for each action',()=>{
 const w=withParty(step(meen(),'stay-new-meen-night'),3);
 const spec=a.encounterSpec(w,'laoth-new-meen-night-raid');
 assert.deepEqual(spec.adversaries.map(x=>x.count),[3]);
 assert.equal(spec.environment.darknessSetbackDice,1);
 assert.equal(spec.environment.requiresDiceIntegration,true);
});
test('SWE02 cannot leave an active New Meen combat by clicking another node',()=>{
 const w=withParty(step(meen(),'stay-new-meen-night'),3);
 const active=mark(w,'laoth-new-meen-night-raid');
 assert.throws(()=>a.advance(active,'leave-new-meen-after-raid'),/Encerre o combate/);
 assert.ok(w.publishedAdventure.activeEncounterId==null);
});
test('SWE02 New Meen combat rejects forged groups, ids, and victory without enemy defeat; atomic rollback',()=>{
 const w=withParty(step(meen(),'stay-new-meen-night'),3);
 const active=mark(w,'laoth-new-meen-night-raid'),before=clone(active);
 for(const adversaries of [[],[{id:'enemy-1',profileKey:'other',status:'incapacitated'}],members(a.encounterSpec(active,'laoth-new-meen-night-raid'),false),[{id:'',profileKey:'laoth_drombb_thug',status:'incapacitated'}]]) {
  assert.throws(()=>a.recordEncounterOutcome(active,{encounterId:'laoth-new-meen-night-raid',outcome:'adversaries_defeated',adversaries}),/grupos reais|incapacitados|identidades únicas/);
  assert.deepEqual(active,before);
 }
});
test('SWE02 p21: resolved night group not recreated in Drombb cantina; sequential combat only',()=>{
 const night=withParty(step(meen(),'stay-new-meen-night'),3);
 const stopped=win(night,'laoth-new-meen-night-raid');
 assert.equal(stopped.publishedAdventure.flags.newMeenNightRaidStopped,true);
 assert.equal(a.status(stopped).node.choices.some(x=>x.id==='follow-drombb-raiders'),true);
 const camp=enterCantina(step(stopped,'follow-drombb-raiders'));
 const spec=a.encounterSpec(camp,'laoth-drombb-cantina');
 assert.deepEqual(spec.adversaries.map(x=>x.count),[3,3,1]);
 assert.deepEqual(spec.adversaries.map(x=>x.profileKey),['laoth_drombb_thug','laoth_drombb_thug','laoth_angu_drombb']);
});
test('SWE02 New Meen full sequence: victory requires actual Drombb defeat with proof and 3 save reopens',()=>{
 let w=enterCantina(withParty(step(meen(),'confront-drombb-directly'),4));
 assert.equal(a.status(w).node.choices.some(x=>x.id==='new-meen-saved-confirmed'),false);
 w=win(w,'laoth-drombb-cantina');
 assert.equal(w.publishedAdventure.flags.drombbThreatNeutralized,true);
 w=step(w,'new-meen-saved-confirmed');
 assert.equal(w.publishedAdventure.flags.newMeenOutcome,'SECURED');
 for(let i=0;i<3;i++){
  w=JSON.parse(JSON.stringify(w));
  assert.equal(a.status(w).node.id,'new-meen-secured');
  assert.equal(w.publishedAdventure.encounterHistory.at(-1).combatProof.groupCount,4);
 }
 w=step(w,'ota-after-new-meen-defense');
 assert.equal(w.publishedAdventure.currentNodeId,'ota-briefing');
 assert.equal(w.publishedAdventure.flags.newMeenOutcome,'SECURED');
});
test('SWE02 New Meen escape and evacuation persist without victory or experience',()=>{
 let w=step(meen(),'escort-new-meen-evacuees');
 assert.equal(w.publishedAdventure.flags.newMeenOutcome,'EVACUATING');
 w=JSON.parse(JSON.stringify(step(w,'ota-after-new-meen-withdrawal')));
 assert.equal(w.publishedAdventure.flags.drombbThreatNeutralized,undefined);
 assert.equal(w.publishedAdventure.rewardsApplied,false);
});
test('SWE02 tampered victory flag without corresponding combat receipt fails closed on restore/status/advance',()=>{
 const w=enterCantina(withParty(step(meen(),'confront-drombb-directly'),4));
 const corrupted=clone(w);corrupted.publishedAdventure.flags.drombbThreatNeutralized=true;
 assert.throws(()=>a.status(corrupted),/registro de combate verificável/);
 assert.throws(()=>a.advance(corrupted,'new-meen-saved-confirmed'),/registro de combate verificável/);
 assert.deepEqual(w.publishedAdventure.flags.confirmedPartySize,4);
});
test('SWE02 cannot invent combat by calling encounter without scene context',()=>{
 const w=withParty(meen(),4);
 assert.throws(()=>a.markEncounterActive(w,'laoth-drombb-cantina'),/incompatível/);
 assert.throws(()=>a.recordEncounterOutcome(w,{encounterId:'laoth-drombb-cantina',outcome:'adversaries_defeated',adversaries:[]}),/incompatível/);
});

import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {environmentSkillModifiers} from '../app/src/main/assets/vtt/js/rules/environment.mjs';
class SnapshotRepo {
 constructor(){this.state=createCampaign('P235 New Meen');this.saves=0;}
 load(){return clone(this.state);}
 save(state){this.state=clone(state);this.saves++;}
}
function controllerNightRaid(){
 const repo=new SnapshotRepo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');
 g.startSession({playerCount:3,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});
 for(const choice of ['dock-ryloth','help-new-meen']) g.advancePublishedAdventure(choice);
 advanceNabatMorningController(g);
 g.state.world=resolveOldLylekRetreat(a,g.state.world,Number(g.state.session?.playerCount)||1);
 for(const choice of ['reach-new-meen','stay-new-meen-night']) g.advancePublishedAdventure(choice);
 return {g,repo};
}
test('P235 actual Android controller starts New Meen raid with EotE session playerCount, official profile, persisted encounter',()=>{
 const {g,repo}=controllerNightRaid(),priorSave=repo.saves;
 assert.equal(g.state.session.campaignRules.destiny.playerCount,3);
 assert.equal(g.publishedAdventureStatus('long-arm-of-the-hutt').encounter.id,'laoth-new-meen-night-raid');
 const r=g.beginPublishedAdventureEncounter('laoth-new-meen-night-raid',{rng:()=>.9});
 assert.equal(r.encounter.confirmedPartySize,3);
 assert.equal(g.state.combat.adversaries.length,1);
 assert.equal(g.state.combat.adversaries[0].membersTotal,3);
 assert.equal(g.state.combat.adversaries[0].profileKey,'laoth_drombb_thug');
 assert.equal(g.state.world.publishedAdventure.flags.confirmedPartySize,3);
 assert.equal(g.state.environment.scriptedSetbackDice,1);
 for(const skill of ['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','atletismo','percepcion'])
  assert.equal(environmentSkillModifiers(g.state.environment,skill).setback,1,skill);
 for(let i=0;i<3;i++) {
   const reopened=new GameController(repo);
   assert.equal(reopened.state.combat.publishedAdventureContext.encounterId,'laoth-new-meen-night-raid');
   assert.equal(reopened.state.environment.scriptedSetbackDice,1);
 }
 assert.ok(repo.saves>priorSave);
});
test('P235 closing official night combat restores prior environment and does not automatically grant credits, PE or save New Meen',()=>{
 const {g,repo}=controllerNightRaid();
 g.beginPublishedAdventureEncounter('laoth-new-meen-night-raid',{rng:()=>.9});
 const origXP=clone(g.state.player.character.xp),origCredits=g.state.player.character.credits;
 const cb=clone(g.state.combat);cb.adversaries.forEach(a=>{a.status='incapacitated';a.wounds.current=a.wounds.threshold});cb.encounter.outcome='adversaries_defeated';
 g.commit('COMBAT_ACTION',{combat:cb});
 const done=g.finishCombat({rng:()=>.9});
 assert.equal(done.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(g.state.environment.scriptedSetbackDice,0);
 assert.equal(g.state.world.publishedAdventure.flags.newMeenNightRaidStopped,true);
 assert.equal(g.state.world.publishedAdventure.flags.newMeenOutcome,undefined);
 assert.deepEqual(g.state.player.character.xp,origXP);
 assert.equal(g.state.player.character.credits,origCredits);
 for(let i=0;i<3;i++)assert.equal(new GameController(repo).state.world.publishedAdventure.flags.newMeenNightRaidStopped,true);
});
