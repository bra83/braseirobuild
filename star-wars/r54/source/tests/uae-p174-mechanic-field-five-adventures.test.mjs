import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {grantCatalogAsset,damageOwnedAsset,repairOwnedAsset} from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
const copy=structuredClone;
const tree=aorTalentCatalog();
const byName=name=>tree.find(n=>n.originalName===name);
class Repo{constructor(){this.saved=createCampaign('P174');this.fail=false;}load(){return copy(this.saved);}save(s){if(this.fail)throw Error('P174 falha de disco');this.saved=copy(s);}}
function fixture(kind='redundant'){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 const talent=byName(kind==='redundant'?'SISTEMAS REDUNDANTES':'PIEZA DEFECTUOSA');
 assert.ok(talent);c.talentPurchases=[{nodeId:talent.id,originalName:talent.originalName,name:talent.name}];c.talents=[talent.name];
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.2});
 for(const id of ['target','donor'])g.state=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:id}).state;
 if(kind==='redundant')g.state=damageOwnedAsset(g.state,'target',{amount:3,sourceRuleIds:['fixture']}).state;
 repo.save(g.state);
 return {repo,g};
}
function action(g,kind='redundant',options={}){return g.useAoRMechanicFieldAction({kind,targetId:'target',donorId:kind==='redundant'?'donor':null,actionId:`P174_${kind}`,approved:true,rng:()=>0.2,...options});}
const target=g=>g.state.group.ownedAssets.find(a=>a.assetId==='target');
const donor=g=>g.state.group.ownedAssets.find(a=>a.assetId==='donor');
test('P174: source tree activates two source-backed AoR positions',()=>{
 for(const name of ['SISTEMAS REDUNDANTES','PIEZA DEFECTUOSA'])assert.equal(byName(name).mechanicallyActive,true);
 assert.ok(aorTalentCoverage().mechanicalPositions>=325);
});
test('P174: Redundant Systems repairs broken gear, donor intact, with real Easy Mechanics check',()=>{
 const {g,repo}=fixture();const old=copy(donor(g));const result=action(g);
 assert.equal(result.check.success,true);assert.equal(result.receipt.applied,true);
 assert.equal(result.receipt.kind,'redundant');assert.equal(target(g).state.condition,'operational');
 assert.equal(target(g).state.damage,0);assert.deepEqual(donor(g),old);
 assert.equal(validateState(g.state),true);assert.deepEqual(repo.saved,g.state);
});
test('P174: Bad Motivator fails a GM-approved device after real Hard Mechanics check',()=>{
 const {g}=fixture('motivator');const result=action(g,'motivator');
 assert.equal(result.receipt.applied,true);assert.equal(result.receipt.kind,'motivator');
 assert.equal(target(g).state.condition,'broken');assert.equal(target(g).state.damage,1);
 assert.equal(validateState(g.state),true);
});
test('P174: failed check and refused GM authorization never damage or repair',()=>{
 for(const [kind,opts] of [['redundant',{rng:()=>0}],['motivator',{approved:false}]]){
  const {g}=fixture(kind);const before=copy(target(g));const result=action(g,kind,opts);
  assert.equal(result.receipt.applied,false);assert.deepEqual(target(g),before);
  assert.equal(validateState(g.state),true);
 }
});
test('P174: duplicate receipt replay is idempotent, other ID cannot spend same session talent',()=>{
 const {g}=fixture();const first=action(g);const stable=copy(g.state);
 const again=action(g);assert.equal(again.duplicate,true);assert.equal(again.receipt.actionId,first.receipt.actionId);
 assert.deepEqual(g.state,stable);
 assert.throws(()=>action(g,'redundant',{actionId:'P174_second'}),/já usado/);
 assert.throws(()=>action(g,'redundant',{approved:false}),/divergentes/);
});
test('P174: rejects missing talent, donor mismatch, damaged donor, invalid target, no session',()=>{
 for(const variant of ['talent','donor','target','session']){
  const {g,repo}=fixture();
  if(variant==='talent'){const c=copy(g.state.player.character);c.talentPurchases=[];c.talents=[];g.setCharacter(c);}
  if(variant==='donor')g.state=damageOwnedAsset(g.state,'donor',{amount:1,sourceRuleIds:['fixture']}).state;
  if(variant==='target')g.state.group.ownedAssets=g.state.group.ownedAssets.filter(a=>a.assetId!=='target');
  if(variant==='session')g.state.session.status='encerrada';
  repo.save(g.state);const before=copy(g.state);
  assert.throws(()=>action(g),/adquirido|doador|reparo|Reparo|Dispositivo|sessão/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 }
});
test('P174: save failure rolls back check, equipment, receipt and event atomically',()=>{
 const {g,repo}=fixture();const before=copy(g.state);repo.fail=true;
 assert.throws(()=>action(g),/disco/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P174: forged or missing causal receipt rejected by state validation',()=>{
 for(const change of [
  s=>s.master.mechanicFieldActions[0].applied=false,
  s=>s.master.mechanicFieldActions[0].success=false,
  s=>s.events=s.events.filter(e=>e.type!=='AOR_MECHANIC_ACTION'),
  s=>s.master.mechanicFieldActions.push(copy(s.master.mechanicFieldActions[0]))
 ]){
  const {g}=fixture();action(g);const bad=copy(g.state);change(bad);
  assert.throws(()=>validateState(bad),/recibo|Recibo|adulterado|duplicado|mais de uma vez/);
 }
});
test('P174: bridge exposes durable source-backed action',()=>{
 const {g,repo}=fixture();const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 const r=bridge.useAoRMechanicFieldAction({kind:'redundant',targetId:'target',donorId:'donor',actionId:'bridge174',approved:true,rng:()=>0.2});
 assert.equal(r.ok,true);assert.equal(r.data.receipt.applied,true);
 assert.equal(bridge.useAoRMechanicFieldAction({kind:'redundant',targetId:'target',donorId:'donor',actionId:'bridge174',approved:true,rng:()=>0.2}).data.duplicate,true);
 assert.equal(validateState(new GameController(repo).state),true);
});
test('P174: legacy P173 save migrates with empty mechanic ledger',()=>{
 const {g}=fixture();const old=copy(g.state);delete old.master.mechanicFieldActions;
 const migrated=migrateState(old);assert.deepEqual(migrated.master.mechanicFieldActions,[]);assert.equal(validateState(migrated),true);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P174 ${id}: AoR mechanic equipment action does not alter five adventure UAE checkpoints across three restores`,()=>{
  const {g,repo}=fixture();g.setCampaignMode('MIXED');
  g.startPublishedAdventureSession(id,{rng:()=>0.5});
  const before=copy(g.state.world.publishedAdventureRuntime);
  action(g);
  assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
  for(let i=0;i<3;i++){
   const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
   assert.equal(reopened.state.master.mechanicFieldActions.length,1);
   assert.deepEqual(reopened.state.world.publishedAdventureRuntime,before);
   assert.equal(reopened.state.group.ownedAssets.find(a=>a.assetId==='target').state.damage,0);
  }
 });
}
test('P174: Bad Motivator disables actual catalog weapon actions until repaired',()=>{
 const {g,repo}=fixture('motivator');
 g.state=grantCatalogAsset(g.state,{kind:'weapon',itemId:'blaster_pistol',ownedAssetId:'p174-blaster'}).state;
 repo.save(g.state);
 const result=g.useAoRMechanicFieldAction({kind:'motivator',targetId:'p174-blaster',actionId:'weapon-break',approved:true,rng:()=>0.2});
 assert.equal(result.receipt.applied,true);
 const weapon=g.state.player.character.weapons.find(w=>w.ownedAssetId==='p174-blaster');
 assert.ok(weapon);
 const idx=g.state.player.character.weapons.findIndex(w=>w.ownedAssetId==='p174-blaster');
 assert.throws(()=>g.equipWeapon(String(idx)),/Dispositivo avariado/);
 assert.throws(()=>g.resolveWeaponAttack({weaponIndex:idx,rng:()=>0.2}),/Dispositivo avariado/);
 const repaired=repairOwnedAsset(g.state,'p174-blaster',{amount:1,cost:0,sourceRuleIds:['test-repair']});
 g.state=repaired.state;repo.save(g.state);
 assert.equal(g.equipWeapon(String(idx)).activeWeapon.ownedAssetId,'p174-blaster');
 assert.equal(validateState(new GameController(repo).state),true);
});
test('P174: Unicode character names survive mechanic receipt and three reopenings',()=>{
 const {g,repo}=fixture();const c=copy(g.state.player.character);c.name='Capitão Téndaar';g.setCharacter(c);
 const result=action(g);assert.equal(result.receipt.characterName,'Capitão Téndaar');
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
