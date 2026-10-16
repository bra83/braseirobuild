/** P169: cross-adventure provenance, recovery, replay and AoR stealth consumer. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank,masterSkillDifficultyOption} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const copy=structuredClone;
function fixture(id){
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 module.initialObjectives['p169-proof']={title:'Recompensa autorizada',type:'SECONDARY',target:1};
 module.initialClocks['p169-suspicion']={max:3,value:0,status:'ACTIVE'};
 const consequence={id:'p169-payoff',description:'Somente depois de evento de origem',resolveWhen:'flag:p169-authorized',
  onResolveEffects:[{type:'complete-objective',objectiveId:'p169-proof'},
   {type:'advance-clock',clockId:'p169-suspicion',delta:1},
   {type:'set-flag',flag:'p169-reward',value:true}]};
 for(const [i,event] of ['P169_CONTACT','P169_SECOND_CONTACT'].entries())module.triggers.push({id:`p169-origin-${i}`,event,maxFires:1,consequence:copy(consequence)});
 assert.ok(validateAdventureSemantics(module).triggers>0);
 return module;
}
for(const {id} of publishedAdventureCatalog()){
 test(`P169 ${id}: forged source consequence, direct API and command rollback`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m),before=r.getState();
  assert.throws(()=>r.recordConsequence({id:'p169-payoff'}),/gatilho de origem/);
  assert.throws(()=>r.commitAction(`${id}:fake`,[{type:'record-consequence',consequence:{id:'p169-payoff'}}]),/gatilho de origem/);
  assert.deepEqual(r.getState(),before);
  assert.equal(validateAdventureCheckpoint(m,before),true);
 });
 test(`P169 ${id}: two authored triggers, replay receipt, reward once, three reopens`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  const commands=[{type:'event',eventType:'P169_SECOND_CONTACT',payload:{case:id}}];
  assert.equal(r.commitAction(`${id}:contact`,commands).applied,true);
  assert.equal(r.commitAction(`${id}:contact`,commands).applied,false);
  const events=r.getState().canonicalEvents;
  const recorded=events.find(e=>e.type==='CONSEQUENCE_RECORDED'&&e.payload.consequenceId==='p169-payoff');
  assert.equal(recorded.payload.authored,true);
  assert.equal(recorded.payload.triggerId,'p169-origin-1');
  assert.ok(events.some(e=>e.id===recorded.payload.causeEventId&&e.type==='P169_SECOND_CONTACT'));
  r.commitAction(`${id}:contact2`,[{type:'event',eventType:'P169_CONTACT'}]);
  assert.equal(r.getState().consequences.length,1);
  r.commitAction(`${id}:auth`,[{type:'effects',effects:{'p169-authorized':true}}]);
  r.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p169-payoff'}]);
  assert.equal(r.getState().flags['p169-reward'],true);
  assert.equal(r.getState().objectives['p169-proof'].status,'COMPLETED');
  assert.equal(r.getState().clocks['p169-suspicion'].value,1);
  const stable=r.getState();
  for(let i=0;i<3;i++){
   const restored=AdventureRuntime.restore(m,r.serialize());
   assert.equal(restored.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p169-payoff'}]).applied,false);
   assert.deepEqual(restored.getState(),stable);
   assert.equal(validateAdventureCheckpoint(m,restored.getState()),true);
  }
 });
 test(`P169 ${id}: tampered journal provenance cannot restore`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  r.dispatchEvent('P169_CONTACT');
  const original=r.getState(),index=original.canonicalEvents.findIndex(e=>e.type==='CONSEQUENCE_RECORDED');
  assert.ok(index>0);
  for(const bad of [
   e=>{e.payload.triggerId='p169-origin-1';},
   e=>{e.payload.causeEventId='evt_fake_99';},
   e=>{e.payload.authored=false;},
   e=>{delete e.payload.causeEventId;}
  ]){
   const forged=copy(original);bad(forged.canonicalEvents[index]);
   assert.throws(()=>AdventureRuntime.restore(m,forged),/Journal UAE|proveniência|gatilho causal|origem/);
  }
 });
 test(`P169 ${id}: ad-hoc consequence remains allowed but has no authored rewards`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  r.commitAction(`${id}:adhoc`,[{type:'record-consequence',consequence:{id:'p169-free',description:'Evento improvisado'}}]);
  r.commitAction(`${id}:finish`,[{type:'resolve-consequence',consequenceId:'p169-free'}]);
  assert.equal(r.getState().consequences.find(c=>c.id==='p169-free').resolved,true);
  assert.equal(r.getState().flags['p169-reward'],undefined);
  assert.equal(r.getState().objectives['p169-proof'].status,'ACTIVE');
  assert.equal(validateAdventureCheckpoint(m,r.getState()),true);
 });
 test(`P169 ${id}: legacy checkpoint receives provenance boundary without rewriting old events`,()=>{
  const m=fixture(id),old=new AdventureRuntime(m).getState();
  delete old.consequenceProvenanceFromSequence;
  const restored=AdventureRuntime.restore(m,old);
  assert.equal(restored.getState().consequenceProvenanceFromSequence,undefined);
  restored.dispatchEvent('P169_CONTACT');
  const current=restored.getState();
  assert.equal(current.consequenceProvenanceFromSequence,2);
  assert.equal(current.canonicalEvents[0].type,'ADVENTURE_STARTED');
  assert.equal(validateAdventureCheckpoint(m,current),true);
 });
}
const all=Object.values(AOR_TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes));
class Repo{constructor(){this.saved=createCampaign('P169');this.fail=false;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P169 disk fail');this.saved=copy(s);}}
function hero(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talentPurchases=[];c.talents=[];
 const n=all.find(n=>n.originalName==='SOMBRA FURTIVA');assert.ok(n);
 c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});c.talents.push(n.name);
 g.setCharacter(c);g.setCampaignMode('AOR');return {g,repo,n};
}
test('P169 AoR: one source-backed Master of Shadows position is a real skill consumer',()=>{
 const active=aorTalentCatalog().filter(n=>n.originalName==='SOMBRA FURTIVA'&&n.mechanicallyActive);
 assert.equal(active.length,1);assert.ok(aorTalentCoverage().mechanicalPositions>=314);
 const {g,n}=hero();assert.equal(sourceBackedTalentRank(g.state.player.character,'MASSHAD'),1);
 assert.equal(masterSkillDifficultyOption(g.state.player.character,'shadows').available,true);
 const before=g.state.player.character.derived.strain.current;
 const check=g.resolveSkillCheck({skillKey:'sigilo',difficulty:3,useMasterOfShadows:true,rng:()=>.25});
 assert.equal(check.pool.difficulty,2);assert.equal(g.state.player.character.derived.strain.current,before+2);
 const forged=copy(g.state.player.character);forged.talentPurchases=[];forged.talents=[n.name];
 g.setCharacter(forged);assert.equal(sourceBackedTalentRank(g.state.player.character,'MASSHAD'),0);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'sigilo',useMasterOfShadows:true}),/não foi adquirido/);
});
test('P169 AoR: stealth skill fails closed on wrong skill and storage rollback',()=>{
 const {g,repo}=hero(),before=copy(g.state);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'computadoras',useMasterOfShadows:true}),/não se aplica/);
 assert.deepEqual(g.state,before);
 repo.fail=true;
 assert.throws(()=>g.resolveSkillCheck({skillKey:'sigilo',difficulty:3,useMasterOfShadows:true,rng:()=>.25}),/disk fail/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P169 ${id}: AoR stealth check persists in published campaign with three reopens`,()=>{
  const {g,repo}=hero();g.setCampaignMode('MIXED');g.executePublishedAdventureCommand({actionId:`p169:${id}:start`,kind:'start',adventureId:id});
  const check=g.resolveSkillCheck({skillKey:'sigilo',difficulty:3,useMasterOfShadows:true,rng:()=>.25});
  assert.equal(check.pool.difficulty,2);const saved=copy(repo.saved);
  for(let i=0;i<3;i++){
   const opened=new GameController(repo);
   assert.equal(validateState(opened.state),true);
   assert.equal(opened.state.player.character.derived.strain.current,saved.player.character.derived.strain.current);
   assert.deepEqual(repo.saved,saved);
  }
 });
}
