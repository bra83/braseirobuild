/** P170: ranked Hard Headed and Jury Rigged, real combat consumer and UAE restores. */
import test from 'node:test';
import {readFileSync} from 'node:fs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {hardHeadedOption,bodyGuardOption,juryRiggedPurchaseNodes,juryRiggedWeapon,sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {applyStatusEffect} from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(ss=>Object.values(ss).flatMap(tree=>tree.nodes));
const jury=nodes.filter(n=>n.originalName==='MEJORAS CASERAS');
const hard=nodes.filter(n=>n.originalName==='TENACIDAD');
const improved=nodes.filter(n=>n.originalName==='TENACIDAD MEJORADA');
const bodyguard=nodes.filter(n=>n.originalName==='GUARDAESPALDAS');
class Repo {constructor(){this.saved=createCampaign('P170');this.fail=false;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('disk failure P170');this.saved=copy(s)}}
function fixture(selected=[...jury,...hard,...improved]){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=selected.map(n=>({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source}));
 c.talents=selected.map(n=>n.name);g.setCharacter(c);g.setCampaignMode('AOR');
 return {repo,g};
}
function status(g,type){let c=applyStatusEffect(g.state.player.character,{type,duration:'rounds',rounds:3});g.setCharacter(c)}
const win=()=>.49;
const lose=()=>.9999;
test('P170 AoR coverage: 2 Jury, 2 Hard Headed and 2 Improved slots',()=>{
 assert.equal(jury.length,2);assert.equal(hard.length,2);assert.equal(improved.length,2);
 assert.equal(bodyguard.length,2);assert.ok(aorTalentCoverage().mechanicalPositions>=322);
});
test('P170 source-backed slots: display-only names and forged original do not confer mechanics',()=>{
 const {g}=fixture([]);const c=copy(g.state.player.character);
 c.talents=['Melhorias caseiras','Tenacidade','Tenacidade aprimorada'];g.setCharacter(c);
 assert.equal(juryRiggedPurchaseNodes(g.state.player.character).length,0);
 assert.equal(hardHeadedOption(g.state.player.character).available,false);
 assert.throws(()=>g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0}),/não foram adquiridas/);
 status(g,'staggered');assert.throws(()=>g.useHardHeaded({rng:win}),/não foi adquirida/);
});
test('P170 Hard Headed rank 2: difficulty 3, stagger/disoriented removed, failure spends check',()=>{
 const {g}=fixture([...hard]);status(g,'staggered');status(g,'disoriented');
 assert.equal(hardHeadedOption(g.state.player.character).difficulty,3);
 const miss=g.useHardHeaded({rng:lose});assert.equal(miss.success,false);
 assert.ok(g.state.player.character.statusEffects.some(x=>x.type==='staggered'));
 const result=g.useHardHeaded({rng:win});assert.equal(result.success,true);
 assert.equal(result.difficulty,3);
 assert.ok(!g.state.player.character.statusEffects.some(x=>['staggered','disoriented'].includes(x.type)));
});
test('P170 Hard Headed improved: incapacitation due strain only, threshold-1 on success',()=>{
 const {g}=fixture([...hard,...improved]);const c=copy(g.state.player.character);
 c.derived.strain.current=c.derived.strain.threshold+2;g.setCharacter(c);
 assert.throws(()=>g.useHardHeaded({improved:true,rng:win}),/exige um turno de combate/);
 const combat=copy(g.state.combat);combat.active=true;combat.round=2;g.commit('COMBAT_UPDATED',{combat});
 const result=g.useHardHeaded({improved:true,rng:win});assert.equal(result.success,true);
 assert.equal(result.difficulty,4);
 assert.equal(g.state.player.character.derived.strain.current,g.state.player.character.derived.strain.threshold-1);
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.throws(()=>g.useHardHeaded({improved:true,rng:win}),/exige incapacitação|ação deste turno/);
});
test('P170 Hard Headed: rollback on save failure and forbidden unrelated critical incapacity',()=>{
 const {g,repo}=fixture([...hard]);status(g,'disoriented');const before=copy(g.state);
 repo.fail=true;assert.throws(()=>g.useHardHeaded({rng:win}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P170 Jury Rigged: permanent unique owned weapon assignment, damage and critical',()=>{
 const {g}=fixture(jury);const [first,second]=jury;
 const a=g.assignJuryRigged({nodeId:first.id,weaponIndex:0,effect:'damage'});
 assert.ok(a.instanceId);assert.equal(g.state.player.character.weapons[0].damage,10);
 const modified=juryRiggedWeapon(g.state.player.character,g.state.player.character.weapons[0]);
 assert.equal(modified.weapon.damage,11);assert.equal(modified.applied,'damage');
 assert.throws(()=>g.assignJuryRigged({nodeId:second.id,weaponIndex:0,effect:'crit'}),/já possui/);
 g.assignJuryRigged({nodeId:second.id,weaponIndex:1,effect:'crit'});
 assert.equal(juryRiggedWeapon(g.state.player.character,g.state.player.character.weapons[1]).weapon.crit,1);
 assert.equal(juryRiggedWeapon(g.state.player.character,g.state.player.character.weapons[2]).applied,null);
 assert.equal(g.state.player.character.weapons[1].crit,2);
});
test('P170 Jury Rigged: invalid item/slot, stale purchase and rollback',()=>{
 const {g,repo}=fixture(jury);
 assert.throws(()=>g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:100}),/arma pessoal possuída/);
 assert.throws(()=>g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0,effect:'soak'}),/inválida/);
 const before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);repo.fail=false;
 g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0});
 const forged=copy(g.state.player.character);forged.talentPurchases=[];
 assert.equal(juryRiggedWeapon(forged,forged.weapons[0]).applied,null);
});
test('P170 Jury Rigged is consumed by real weapon attack and cannot clone instance IDs',()=>{
 const {g}=fixture(jury);g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0,effect:'damage'});
 const attacked=g.resolveWeaponAttack({weaponIndex:0,difficulty:1,rng:win});
 assert.equal(attacked.hit,true);assert.equal(attacked.juryRigged,'damage');
 assert.equal(attacked.damage,12); // base 10 + 1 net success + 1 Jury
 const forged=copy(g.state.player.character);
 forged.weapons[1].juryRiggedInstanceId=forged.weapons[0].juryRiggedInstanceId;
 assert.equal(juryRiggedWeapon(forged,forged.weapons[0]).applied,null);
 assert.equal(juryRiggedWeapon(forged,forged.weapons[1]).applied,null);
});
test('P170 Android bridge and UI expose only source-backed owned slots and real actions',()=>{
 const {repo,g}=fixture([...jury,...hard,...improved,...bodyguard]);
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 const status=bridge.aorP170Status();assert.equal(status.ok,true);
 assert.equal(status.data.juryRigged.nodes.length,2);
 assert.equal(status.data.hardHeaded.rank,2);
 assert.equal(status.data.bodyGuard.rank,2);
 const assigned=bridge.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0,effect:'damage'});
 assert.equal(assigned.ok,true);
 assert.equal(bridge.aorP170Status().data.weapons[0].effect,'damage');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const method of ['aorP170Status','assignJuryRigged','useHardHeaded','useBodyGuard','resolveIncomingCompanionAttack'])
  assert.ok(ui.includes(method),`P170 UI missing ${method}`);
 const shell=readFileSync(new URL('../app/src/main/assets/vtt/r46-shell.html',import.meta.url),'utf8');
 assert.ok(Number(shell.match(/data-version="1\.0\.(\d+)"/)?.[1])>=171);
});
test('P170 Body Guard: maneuver, voluntary strain, live companion attack upgrades, expiration',()=>{
 const {g}=fixture(bodyguard);const ally=g.addCompanion({name:'Recruta rebelde'});
 const combat=copy(g.state.combat);combat.active=true;combat.round=1;g.commit('COMBAT_UPDATED',{combat});
 assert.equal(bodyGuardOption(g.state.player.character,2).rank,2);
 assert.throws(()=>g.useBodyGuard({companionId:ally.id,strain:2}),/Confirme/);
 const initial=g.state.player.character.derived.strain.current;
 const protection=g.useBodyGuard({companionId:ally.id,strain:2,confirmedEngaged:true});
 assert.equal(protection.difficultyUpgrades,2);
 assert.equal(g.state.player.character.derived.strain.current,initial+2);
 assert.throws(()=>g.useBodyGuard({companionId:ally.id,strain:1,confirmedEngaged:true}),/já foi usado/);
 const attacked=g.resolveIncomingCompanionAttack({companionId:ally.id,attackerCharacteristic:3,attackerSkill:2,rng:win});
 assert.equal(attacked.bodyGuardUpgrades,2);assert.equal(attacked.check.pool.challenge,2);
 assert.equal(g.state.group.companions[0].character.derived.wounds.current,attacked.character.derived.wounds.current);
 g.advancePlayerTurn({rng:win});
 assert.equal(g.state.combat.bodyGuard,undefined);
 const after=g.resolveIncomingCompanionAttack({companionId:ally.id,attackerCharacteristic:3,attackerSkill:2,rng:win});
 assert.equal(after.bodyGuardUpgrades,0);assert.equal(after.check.pool.challenge,0);
});
test('P170 Body Guard: rejects forged display rank, absent ally and rolls back failed save',()=>{
 const {g,repo}=fixture(bodyguard);const ally=g.addCompanion({name:'Companheiro'});
 const combat=copy(g.state.combat);combat.active=true;combat.round=1;g.commit('COMBAT_UPDATED',{combat});
 assert.throws(()=>g.useBodyGuard({companionId:'fake',strain:1,confirmedEngaged:true}),/não pertence/);
 assert.throws(()=>g.useBodyGuard({companionId:ally.id,strain:3,confirmedEngaged:true}),/excede/);
 const before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.useBodyGuard({companionId:ally.id,strain:2,confirmedEngaged:true}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);repo.fail=false;
 g.useBodyGuard({companionId:ally.id,strain:1,confirmedEngaged:true});
 const beforeAttack=copy(g.state);repo.fail=true;
 assert.throws(()=>g.resolveIncomingCompanionAttack({companionId:ally.id,rng:win}),/disk failure/);
 assert.deepEqual(g.state,beforeAttack);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P170 ${id}: AoR gear + conditions survive published adventure and three restores`,()=>{
  const {g,repo}=fixture([...jury,...hard]);
  g.assignJuryRigged({nodeId:jury[0].id,weaponIndex:0,effect:'damage'});
  status(g,'disoriented');g.useHardHeaded({rng:win});
  g.setCampaignMode('MIXED');g.executePublishedAdventureCommand({actionId:`p170:${id}:start`,kind:'start',adventureId:id});
  const saved=copy(repo.saved);const expected=juryRiggedWeapon(g.state.player.character,g.state.player.character.weapons[0]);
  assert.equal(expected.weapon.damage,11);
  for(let i=0;i<3;i++){
   const opened=new GameController(repo);
   assert.equal(validateState(opened.state),true);
   assert.deepEqual(opened.state,saved);
   assert.equal(juryRiggedWeapon(opened.state.player.character,opened.state.player.character.weapons[0]).weapon.damage,11);
  }
 });
 test(`P170 ${id}: malformed predicates and forged consequence metadata fail closed`,()=>{
  const m=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  const r=new AdventureRuntime(m);
  assert.equal(r.isConditionMet(false),false);assert.equal(r.isConditionMet(0),false);
  assert.equal(r.isConditionMet({flag:'any'}),false);
  assert.equal(r.isConditionMet('unknown:allow'),false);
  const before=r.getState();
  assert.throws(()=>r.recordConsequence({id:'p170-adhoc',authored:true}),/reservados|reservado/);
  assert.deepEqual(r.getState(),before);
  assert.equal(validateAdventureCheckpoint(m,r.getState()),true);
 });
 test(`P170 ${id}: UAE objective, consequence and clock transaction with rollback`,()=>{
  const m=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  m.initialObjectives['p170-reward']={title:'Missão adicional',type:'SECONDARY',target:1};
  m.initialClocks['p170-danger']={value:0,max:3,status:'ACTIVE'};
  m.triggers.push({id:'p170-event',event:'P170_EVENT',maxFires:1,consequence:{id:'p170-reward-c',resolveWhen:'flag:p170-allow',onResolveEffects:[{type:'complete-objective',objectiveId:'p170-reward'},{type:'advance-clock',clockId:'p170-danger',delta:1}]}});
  const r=new AdventureRuntime(m),before=r.getState();
  assert.throws(()=>r.commitAction(`${id}:bad`,[{type:'event',eventType:'P170_EVENT'},{type:'resolve-consequence',consequenceId:'p170-reward-c'}]),/Condição de resolução/);
  assert.deepEqual(r.getState(),before);
  r.commitAction(`${id}:good`,[{type:'event',eventType:'P170_EVENT'},{type:'effects',effects:{'p170-allow':true}},{type:'resolve-consequence',consequenceId:'p170-reward-c'}]);
  assert.equal(r.getState().objectives['p170-reward'].status,'COMPLETED');
  assert.equal(r.getState().clocks['p170-danger'].value,1);
  const stable=r.getState();
  for(let i=0;i<3;i++){
   const opened=AdventureRuntime.restore(m,r.serialize());
   assert.equal(validateAdventureCheckpoint(m,opened.getState()),true);
   assert.deepEqual(opened.getState(),stable);
   assert.equal(opened.commitAction(`${id}:good`,[{type:'event',eventType:'P170_EVENT'},{type:'effects',effects:{'p170-allow':true}},{type:'resolve-consequence',consequenceId:'p170-reward-c'}]).applied,false);
  }
 });
}
