import test from 'node:test';import assert from 'node:assert/strict';import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,utilityBeltTalentStatus} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const clone=structuredClone,nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(y=>y.nodes));
class Repo{constructor(){this.saved=createCampaign('p155-utility');this.writes=0;this.fail=false}load(){return clone(this.saved)}save(s){if(this.fail)throw Error('P155 storage failed');this.saved=clone(s);this.writes++}}
function game({rank=1,light=2}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=clone(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];
 const bought=nodes.filter(n=>n.originalName==='CINTURÓN DE ACCESORIOS').slice(0,rank);
 assert.equal(bought.length,rank);
 for(const n of bought){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});}
 g.setCharacter(c);const group=clone(g.state.group);group.destiny={light,dark:3};g.commit('GROUP_UPDATED',{group});return {g,repo};
}
test('P155: both source-backed AoR Utility Belt slots have actual controller and UI consumer',()=>{
 assert.equal(aorCanonicalTalentName('CINTURÓN DE ACCESORIOS'),'UTILITYBELT');
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='CINTURÓN DE ACCESORIOS'&&n.mechanicallyActive).length,2);
 assert.ok(aorTalentCoverage().mechanicalPositions>=254);
 assert.equal(talentRankByOriginal(game({rank:2}).g.state.player.character,'UTILITYBELT'),2);
 assert.equal(utilityBeltTalentStatus(game({rank:0}).g.state.player.character).available,false);
 const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
 for(const s of [runtime,bridge])assert.ok(s.includes('useUtilityBelt'));
 assert.ok(runtime.includes('data-p155-utility-use'));
});
test('P155: catalog filters rarity, encumbrance, restricted items and non-single-use weapons',()=>{
 const {g}=game(),s=g.utilityBeltStatus();assert.equal(s.talent.available,true);
 assert.ok(s.items.some(i=>i.id==='glow_rod'));
 assert.ok(s.items.some(i=>i.id==='stun_grenade'));
 for(const id of ['heavy_blaster_pistol','medpac','tool_kit','blaster_pistol','appraisers_eye'])
  assert.equal(s.items.some(i=>i.id===id),false,id);
 assert.ok(s.items.every(i=>i.rarity<=4&&i.encumbrance<=1));
});
for(const [itemId,kind] of [['glow_rod','gear'],['stun_grenade','weapon']]){
 test(`P155: ${itemId} consumes exactly one light Destiny, adds ${kind}, reopens and replays without writes`,()=>{
  const {g,repo}=game(),before=clone(g.state.player.character);
  const result=g.useUtilityBelt({itemId,actionId:`p155:${itemId}:once`});
  assert.equal(result.kind,kind);assert.equal(result.duplicate,false);
  assert.equal(g.state.group.destiny.light,1);assert.equal(g.state.group.destiny.dark,4);
  assert.equal(g.state.player.character.credits,before.credits);
  assert.equal(g.state.player.character[kind==='gear'?'gear':'weapons'].length,before[kind==='gear'?'gear':'weapons'].length+1);
  assert.equal(validateState(repo.saved),true);
  const writes=repo.writes,reopened=new GameController(repo);
  const again=reopened.useUtilityBelt({itemId,actionId:`p155:${itemId}:once`});
  assert.equal(again.duplicate,true);assert.equal(repo.writes,writes);
  assert.deepEqual(repo.saved,reopened.state);
  assert.throws(()=>reopened.useUtilityBelt({itemId:'comlink',actionId:`p155:${itemId}:once`}),/divergente/);
  assert.equal(repo.writes,writes);
 });
}
for(const [itemId,light,rank] of [['heavy_blaster_pistol',2,1],['medpac',2,1],['tool_kit',2,1],['blaster_pistol',2,1],['unknown',2,1],['glow_rod',0,1],['glow_rod',2,0]]){
 test(`P155: rejects unauthorized ${itemId} with light=${light}, rank=${rank} without mutations`,()=>{
  const {g,repo}=game({rank,light}),before=clone(g.state),saved=clone(repo.saved),writes=repo.writes;
  assert.throws(()=>g.useUtilityBelt({itemId,actionId:'p155:denied'}));
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
 });
}
test('P155: invalid and missing action IDs fail closed',()=>{
 const {g,repo}=game();const before=clone(repo.saved);
 for(const actionId of [null,'','bad id','x'.repeat(121),'__proto__/pollute'])assert.throws(()=>g.useUtilityBelt({itemId:'glow_rod',actionId}));
 assert.deepEqual(repo.saved,before);
});
test('P155: two legitimate activations spend two points, produce two independently owned assets',()=>{
 const {g}=game();const a=g.useUtilityBelt({itemId:'comlink',actionId:'p155:1'});
 const b=g.useUtilityBelt({itemId:'comlink',actionId:'p155:2'});
 assert.equal(a.duplicate,false);assert.equal(b.duplicate,false);
 assert.equal(g.state.group.destiny.light,0);
 const owned=g.state.player.character.gear.filter(i=>i.utilityBeltActionId);
 assert.equal(owned.length,2);assert.notEqual(owned[0].ownedAssetId,owned[1].ownedAssetId);
});
test('P155: failing storage rolls back both inventory and Destiny',()=>{
 const {g,repo}=game();const before=clone(g.state),saved=clone(repo.saved),writes=repo.writes;
 repo.fail=true;assert.throws(()=>g.useUtilityBelt({itemId:'glow_rod',actionId:'p155:fail'}),/storage failed/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P155: unsupported foreign talent label cannot authorize Utility Belt',()=>{
 const {g,repo}=game({rank:0});const c=clone(g.state.player.character);c.talents=['Cinto de acessórios'];g.setCharacter(c);
 assert.equal(g.utilityBeltStatus().talent.available,false);
 assert.throws(()=>g.useUtilityBelt({itemId:'glow_rod',actionId:'p155:forged'}),/não adquirido/);
 assert.equal(validateState(repo.saved),true);
});
test('P155: tampered duplicate or malformed Utility Belt receipts cannot authorize a replay',()=>{
 const {g,repo}=game();g.useUtilityBelt({itemId:'comlink',actionId:'p155:valid'});
 for(const tamper of [receipts=>receipts.push(clone(receipts[0])),receipts=>receipts[0].kind='planet']){
  const c=clone(g.state.player.character);tamper(c.utilityBeltReceipts);g.setCharacter(c);
  const saved=clone(repo.saved),writes=repo.writes;
  assert.throws(()=>g.useUtilityBelt({itemId:'comlink',actionId:'p155:valid'}),/inválido ou duplicado/);
  assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
  // Restore valid fixture before the next independent tampering scenario.
  const repaired=clone(c);repaired.utilityBeltReceipts=[{actionId:'p155:valid',itemId:'comlink',kind:'gear'}];g.setCharacter(repaired);
 }
});
