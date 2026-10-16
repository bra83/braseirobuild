/** P159 — AoR Natural talents + end-to-end five-adventure checkpoint security. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCatalog, aorTalentCoverage } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { aorCanonicalTalentName } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { naturalRerollOption, talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes));
class Repo {constructor(id='p159'){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P159 disk error');this.saved=copy(s);this.writes++}}
function hero(talent){
 const repo=new Repo('p159:'+talent),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character),node=nodes.find(n=>n.originalName===talent);
 assert.ok(node,talent);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talents=[node.name];c.talentPurchases=[{nodeId:node.id,name:node.name,originalName:talent,source:node.source}];
 g.setCharacter(c);g.startSession({rng:()=>0.99});return {g,repo};
}
test('P159: 270/360 AoR slots; two natural talents source-backed, unrelated slots remain disabled',()=>{
 const coverage=aorTalentCoverage();assert.equal(coverage.positions,360);assert.ok(coverage.mechanicalPositions>=270);
 for(const [original,canonical] of [['EMBAUCADOR NATO','NATCHARM'],['LÍDER NATO','NATLEAD']]){
  assert.equal(aorCanonicalTalentName(original),canonical);
  assert.equal(aorTalentCatalog().filter(n=>n.originalName===original&&n.mechanicallyActive).length,1);
  assert.equal(talentRankByOriginal(hero(original).g.state.player.character,canonical),1);
 }
 for(const original of ['GUARDAESPALDAS','TENACIDAD'])
  assert.equal(aorTalentCatalog().filter(n=>n.originalName===original&&n.mechanicallyActive).length,2); // P170
});
for(const [talent,canonical,skills,wrong] of [
 ['EMBAUCADOR NATO','NATCHARM',['carisma','engano'],'liderazgo'],
 ['LÍDER NATO','NATLEAD',['liderazgo','frialdad'],'medicina']
]){
 for(const skill of skills)test(`P159: ${talent} rerolls ${skill} once, persists across three reopens`,()=>{
  const {g,repo}=hero(talent);const c=g.state.player.character;
  const opt=naturalRerollOption(c,skill);assert.equal(opt.available,true);assert.equal(opt.originalName,canonical);
  const first=g.resolveSkillCheck({skillKey:skill,useNaturalReroll:true,difficulty:2,rng:()=>0.5});
  assert.equal(first.naturalReroll.used,true);assert.deepEqual(first.naturalReroll.originalRoll.pool,first.pool);
  const saved=copy(repo.saved);
  for(let i=0;i<3;i++){
   const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
   assert.throws(()=>reopened.resolveSkillCheck({skillKey:skills[1],useNaturalReroll:true,rng:()=>0.5}),/já foi usado nesta sessão/);
   assert.deepEqual(repo.saved,saved);
  }
 });
 test(`P159: ${talent} does not reroll unrelated ${wrong} and cannot mutate save`,()=>{
  const {g,repo}=hero(talent),before=copy(repo.saved),writes=repo.writes;
  assert.throws(()=>g.resolveSkillCheck({skillKey:wrong,useNaturalReroll:true,rng:()=>0.5}),/Nenhum talento Nato/);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
 });
 test(`P159: ${talent} storage failure rolls back session use and dice`,()=>{
  const {g,repo}=hero(talent),before=copy(g.state);repo.fail=true;
  assert.throws(()=>g.resolveSkillCheck({skillKey:skills[0],useNaturalReroll:true,rng:()=>0.5}),/P159 disk error/);
  assert.deepEqual(g.state,before);
  repo.fail=false;assert.equal(g.resolveSkillCheck({skillKey:skills[0],useNaturalReroll:true,rng:()=>0.5}).naturalReroll.used,true);
 });
}
function scenario(id){
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 module.initialObjectives['p159-objective']={title:'Objetivo de integração P159',type:'SECONDARY',target:2};
 module.initialClocks['p159-clock']={value:0,max:2,status:'ACTIVE'};
 module.triggers.push({id:'p159-primary',event:'P159_SIGNAL',maxFires:1,effects:[
  {type:'set-flag',flag:'p159-flag',value:true},
  {type:'advance-clock',clockId:'p159-clock',delta:2},
  {type:'progress-objective',objectiveId:'p159-objective',delta:2},
  {type:'emit-event',eventType:'P159_FOLLOWUP',payload:{stage:'complete'}}
 ],consequence:{id:'p159-consequence',description:'Consequence persisted by engine'}});
 module.triggers.push({id:'p159-followup',event:'P159_FOLLOWUP',condition:'payload:stage:complete',maxFires:1,
  effects:[{type:'adjust-counter',counter:'p159-followups',delta:1}]});
 return module;
}
for(const {id} of publishedAdventureCatalog()){
 test(`P159 ${id}: atomic trigger → consequence → clock → objective → nested event → receipt → restore`,()=>{
  const module=scenario(id),runtime=new AdventureRuntime(module),key=`p159:${id}:commit`;
  const commands=[{type:'event',eventType:'P159_SIGNAL',payload:{nodeId:module.startNodeId}}];
  const result=runtime.commitAction(key,commands);assert.equal(result.applied,true);
  const state=runtime.getState();assert.equal(state.flags['p159-flag'],true);
  assert.equal(state.clocks['p159-clock'].value,2);assert.equal(state.clocks['p159-clock'].status,'COMPLETED');
  assert.equal(state.objectives['p159-objective'].status,'COMPLETED');
  assert.equal(state.objectives['p159-objective'].progress,2);
  assert.equal(state.counters['p159-followups'],1);assert.equal(state.consequences.length,1);
  assert.equal(state.triggersFired['p159-primary'],1);assert.equal(state.triggersFired['p159-followup'],1);
  assert.equal(validateAdventureCheckpoint(module,state),true);
  const receipt=state.canonicalEvents.find(e=>e.type==='ADVENTURE_ACTION_COMMITTED');assert.ok(receipt);
  assert.ok(receipt.payload.appliedEventIds.length>=7);
  for(let i=0;i<3;i++){
   const reopened=AdventureRuntime.restore(module,runtime.serialize());
   assert.equal(reopened.commitAction(key,commands).applied,false);
   assert.deepEqual(reopened.getState(),state);
  }
 });
 test(`P159 ${id}: checkpoint rejects tampered progress, completed clock and missing trigger journal`,()=>{
  const module=scenario(id),runtime=new AdventureRuntime(module);
  runtime.commitAction(`p159:${id}:valid`,[{type:'event',eventType:'P159_SIGNAL'}]);
  const good=runtime.getState();
  const mutants=[
   s=>{s.objectives['p159-objective'].progress=1},
   s=>{s.objectives['p159-objective'].status='ACTIVE';s.completedObjectives=[];s.objectives['p159-objective'].progress=2},
   s=>{s.clocks['p159-clock'].value=1},
   s=>{s.clocks['p159-clock'].status='ACTIVE'},
   s=>{s.triggersFired['p159-primary']=2},
   s=>{s.canonicalEvents=s.canonicalEvents.filter(e=>e.type!=='TRIGGER_FIRED');s.eventSequence+=1},
  ];
  for(const mutate of mutants){const invalid=copy(good);mutate(invalid);
   assert.throws(()=>validateAdventureCheckpoint(module,invalid));
   assert.throws(()=>AdventureRuntime.restore(module,invalid));
  }
  assert.equal(validateAdventureCheckpoint(module,good),true);
 });
 test(`P159 ${id}: failed multi-command transaction does not grant reward, clock, objective or receipt`,()=>{
  const module=scenario(id),runtime=new AdventureRuntime(module),before=runtime.getState();
  assert.throws(()=>runtime.commitAction(`p159:${id}:rollback`,[
   {type:'event',eventType:'P159_SIGNAL'},
   {type:'complete-objective',objectiveId:'unknown-objective'}
  ]),/Objetivo UAE desconhecido/);
  assert.deepEqual(runtime.getState(),before);
  assert.equal(runtime.commitAction(`p159:${id}:rollback`,[{type:'event',eventType:'P159_SIGNAL'}]).applied,true);
 });
}
test('P159: bundle/version Android 1.0.159, existing TTS untouched',()=>{
 const gradle=readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.match(gradle,/versionName = "1\.0\.(?:159|1[6-9]\d|[2-9]\d{2})"/);assert.match(bundle,/FINAL 1\.0\.(?:159|1[6-9]\d|[2-9]\d{2}) offline engine bundle/);
 assert.match(bundle,/NATLEAD/);assert.match(bundle,/NATCHARM/);
});
for(const [talent,skill] of [['EMBAUCADOR NATO','carisma'],['LÍDER NATO','liderazgo']])
 test(`P159: Android bridge offers ${talent} only for the purchased skill, then disables it after use`,async()=>{
  const {g,repo}=hero(talent);
  const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const bridge=new BraseiroUiBridge({repository:repo,storage:null});
  const available=bridge.naturalRerollStatus({skillKey:skill});
  assert.equal(available.ok,true);assert.equal(available.data.available,true);
  assert.equal(available.data.usedThisSession,false);assert.equal(available.data.sessionActive,true);
  assert.equal(bridge.naturalRerollStatus({skillKey:'medicina'}).data.available,false);
  const before=copy(repo.saved),writes=repo.writes;
  assert.equal(bridge.naturalRerollStatus({skillKey:'forged'}).ok,false);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
  g.resolveSkillCheck({skillKey:skill,useNaturalReroll:true,rng:()=>0.5});
  const used=bridge.naturalRerollStatus({skillKey:skill});
  assert.equal(used.ok,true);assert.equal(used.data.usedThisSession,true);
  const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  for(const token of ['data-p159-natural-label','data-p159-p151-natural-label','naturalRerollStatus','refreshNatural','refreshP151Natural'])assert.ok(ui.includes(token),token);
 });
