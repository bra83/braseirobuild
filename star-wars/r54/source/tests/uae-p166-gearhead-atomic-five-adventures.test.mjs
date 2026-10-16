/** P166: source-backed AoR Gearhead, transactional attachment mods, and all five UAE adapters. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {grantCatalogAsset} from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import {sourceBackedTalentRank,gearheadModPricing,talentSkillPoolModifiers} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
const gearNodes=AOR_TALENT_TREES.engineer.scientist.nodes.filter(n=>n.originalName==='MANITAS');
class Repo {constructor(){this.saved=createCampaign('P166');this.fail=false;this.writes=0;}load(){return copy(this.saved);}save(s){if(this.fail)throw Error('P166 disk failure');this.saved=copy(s);this.writes++;}}
function setup(ranks=0){
 const repo=new Repo();let g=new GameController(repo);g.loadStarterCharacter('cael');
 let c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];c.credits=10000;c.characteristics.intelecto=5;c.skills.mecanica=5;
 for(const n of gearNodes.slice(0,ranks)){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source});}
 g.setCharacter(c);
 let s=grantCatalogAsset(g.state,{kind:'weapon',itemId:'blaster_rifle',method:'reward'}).state;
 s=grantCatalogAsset(s,{kind:'attachment',itemId:'rapid_recharge_xciter',method:'reward'}).state;
 s=grantCatalogAsset(s,{kind:'gear',itemId:'tool_kit',method:'reward'}).state;
 repo.save(s);g=new GameController(repo);
 const summary=g.attachmentSummary();const weapon=summary.weapons.find(w=>w.sourceCatalogId==='blaster_rifle');const attachment=summary.owned.find(a=>a.catalogId==='rapid_recharge_xciter');
 g.installOwnedAttachment(attachment.assetId,weapon.id);
 return {repo,g,assetId:attachment.assetId,weaponId:weapon.id};
}
test('P166: MANITAS AoR has exactly two purchased, mechanically active positions',()=>{
 assert.equal(gearNodes.length,2);
 assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=300); // P166 baseline; later talents may increase coverage
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='MANITAS'&&n.mechanicallyActive).length,2);
});
test('P166: Mechanics removes one setback per rank, never a different skill',()=>{
 for(let rank=0;rank<=2;rank++){
  const {g}=setup(rank),c=g.state.player.character;
  assert.equal(sourceBackedTalentRank(c,'GEARHD'),rank);
  assert.equal(talentSkillPoolModifiers(c,'mecanica',{setback:3}).setback,3-rank);
  assert.equal(talentSkillPoolModifiers(c,'medicina',{setback:3}).setback,3);
  assert.equal(gearheadModPricing(c).cost,rank?50:100);
 }
});
test('P166: two ranks do not stack the once-only half-price Mod discount',()=>{
 const {g,repo,assetId}=setup(2);const before=g.state.player.character.credits;
 const r=g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});
 assert.equal(r.attempt.cost,50);assert.equal(r.attempt.baseCost,100);assert.equal(r.attempt.gearheadRank,2);
 assert.equal(r.creditsRemaining,before-50);
 for(let i=0;i<3;i++){
  const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
  assert.equal(reopened.attachmentSummary().gearhead.cost,50);
  assert.equal(reopened.attachmentSummary().owned.find(a=>a.assetId===assetId).modStatus[0].cost,50);
  assert.equal(reopened.state.player.character.credits,before-50);
 }
});
test('P166: failed Mod spends discounted credits and one attempt, never grants success',()=>{
 const {g,assetId}=setup(1);const before=g.state.player.character.credits;
 const r=g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0});
 assert.equal(r.success,false);assert.equal(r.attempt.cost,50);assert.equal(r.creditsRemaining,before-50);
 const mod=g.attachmentSummary().owned.find(a=>a.assetId===assetId).modStatus.find(m=>m.id==='reduce_inaccurate');
 assert.equal(mod.failed,1);assert.equal(mod.installed,0);
});
test('P166: display labels, forged node ids and altered original names never grant discount',()=>{
 const {g}=setup(0);const c=copy(g.state.player.character);
 c.talents=['Faz-tudo','Faz-tudo'];c.talentPurchases=[{nodeId:gearNodes[0].id,originalName:'CHAPUCERO NATO'},{nodeId:'engineer:scientist:r1c1',originalName:'MANITAS'}];
 g.setCharacter(c);assert.equal(sourceBackedTalentRank(g.state.player.character,'GEARHD'),0);
 assert.equal(gearheadModPricing(g.state.player.character).cost,100);
 assert.equal(talentSkillPoolModifiers(g.state.player.character,'mecanica',{setback:3}).setback,3);
});
test('P166: mod save failure atomically rolls back credits, attempts, weapon and ledger',()=>{
 const {g,repo,assetId}=setup(2);const before=copy(g.state),saved=copy(repo.saved),writes=repo.writes;
 repo.fail=true;assert.throws(()=>g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
 repo.fail=false;const done=g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});assert.equal(done.attempt.cost,50);
});
test('P166: attachment install/remove each roll back all state on storage failure',()=>{
 const {g,repo,assetId,weaponId}=setup(1);const before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.removeOwnedAttachment(assetId),/disk failure/);assert.deepEqual(g.state,before);
 repo.fail=false;g.removeOwnedAttachment(assetId);const removed=copy(g.state);
 repo.fail=true;assert.throws(()=>g.installOwnedAttachment(assetId,weaponId),/disk failure/);assert.deepEqual(g.state,removed);
});
test('P166: Android bridge exposes the source-backed price and generated UI uses dynamic cost',()=>{
 const {repo,assetId}=setup(1);const bridge=new BraseiroUiBridge({repository:repo,storage:null});const r=bridge.attachmentSummary();
 assert.equal(r.ok,true);assert.equal(r.data.gearhead.cost,50);
 assert.equal(r.data.owned.find(a=>a.assetId===assetId).modStatus[0].cost,50);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(ui.includes('TENTAR MOD · ${Number(m.cost||100)} cr'));
});
for(const {id} of publishedAdventureCatalog())test(`P166: ${id} UAE transactional triggers, objectives, consequences and three restores`,()=>{
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 module.initialObjectives.p166={title:'P166 — objetivo verificado',type:'SECONDARY',target:2};
 module.initialClocks.p166={value:0,max:2,status:'ACTIVE'};
 module.triggers.push({id:'p166-checkpoint-trigger',event:'P166_CHECKPOINT_EVENT',maxFires:2,effects:[{type:'progress-objective',objectiveId:'p166',delta:1},{type:'advance-clock',clockId:'p166',delta:1}],consequence:{id:'p166-checkpoint-consequence',description:'Marco persistido'}});
 const runtime=new AdventureRuntime(module);
 for(let i=0;i<2;i++)runtime.commitAction(`${id}:p166:${i}`,[{type:'event',eventType:'P166_CHECKPOINT_EVENT'}]);
 const good=runtime.getState();assert.equal(good.objectives.p166.status,'COMPLETED');assert.equal(good.clocks.p166.status,'COMPLETED');assert.equal(validateAdventureCheckpoint(module,good),true);
 assert.throws(()=>runtime.commitAction(`${id}:p166:bad`,[{type:'progress-objective',objectiveId:'not-in-book',delta:1}]));assert.deepEqual(runtime.getState(),good);
 for(let i=0;i<3;i++){
  const reopened=AdventureRuntime.restore(module,runtime.serialize());
  assert.equal(reopened.commitAction(`${id}:p166:1`,[{type:'event',eventType:'P166_CHECKPOINT_EVENT'}]).applied,false);
  assert.deepEqual(reopened.getState(),good);
 }
});
