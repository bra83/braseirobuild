/** P167 Mental Fortress source-bound, critical effects and persistence. */
import test from 'node:test';import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {criticalCheckModifiers} from '../app/src/main/assets/vtt/js/rules/critical.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const clone=structuredClone,nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes)).filter(n=>n.originalName==='FORTALEZA MENTAL');
class Repo{constructor(){this.saved=createCampaign('P167 mental');this.fail=false}load(){return clone(this.saved)}save(s){if(this.fail)throw Error('P167 disk failure');this.saved=clone(s)}}
function setup(ranks=1,light=2){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');const c=clone(g.state.player.character);
 c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];
 for(const n of nodes.slice(0,ranks)){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source});}
 g.setCharacter(c);const group=clone(g.state.group);group.destiny={light,dark:1};g.commit('GROUP_UPDATED',{group});return {repo,g};}
test('P167 two authentic AoR Mental Fortress slots, 304/360 mechanically active',()=>{
 assert.equal(nodes.length,2);assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=304);
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='FORTALEZA MENTAL'&&n.mechanicallyActive).length,2);
 for(let n=0;n<=2;n++){const {g}=setup(n);assert.equal(sourceBackedTalentRank(g.state.player.character,'MENTALFORT'),n);}
});
test('P167 Mental Fortress only suppresses critical check modifiers on Intellect/Cunning, not incapacitation',()=>{
 const {g}=setup(1);const c=clone(g.state.player.character);
 c.criticalInjuries=[{id:'mental',active:true,effect:{setback:1,difficultyIncrease:1,difficultyCharacteristics:['intelecto','astucia'],duration:'until_healed'}},
  {id:'stagger',active:true,effect:{staggered:true,cannotVoluntarilySufferStrain:true,duration:'until_healed'}}];
 assert.equal(criticalCheckModifiers(c,{skillKey:'mecanica',characteristicKey:'intelecto'}).difficultyIncrease,1);
 c.combatEffects={mentalFortress:true};
 for(const characteristicKey of ['intelecto','astucia']){
  const r=criticalCheckModifiers(c,{characteristicKey});assert.equal(r.difficultyIncrease,0);assert.equal(r.setback,0);
  assert.equal(r.cannotAct,true);assert.equal(r.cannotVoluntarilySufferStrain,true);
 }
 assert.equal(criticalCheckModifiers(c,{characteristicKey:'agilidad'}).setback,1);
 const forged=clone(c);forged.talentPurchases=[];assert.equal(criticalCheckModifiers(forged,{characteristicKey:'intelecto'}).setback,1);
});
test('P167 Mental Fortress activation spends exactly one Destiny, persists, repeats blocked',()=>{
 const {g,repo}=setup(2);g.beginCombat();const light=g.state.group.destiny.light;
 assert.equal(g.activateMentalFortress().active,true);assert.equal(g.state.group.destiny.light,light-1);
 assert.equal(g.state.player.character.combatEffects.mentalFortress,true);
 assert.throws(()=>g.activateMentalFortress(),/já está ativa/);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.equal(reopened.state.player.character.combatEffects.mentalFortress,true);}
 g.beginCombat();assert.equal(g.state.player.character.combatEffects?.mentalFortress,undefined);
});
test('P167 Mental Fortress rejects no talent, no combat, no Destiny and rolls back disk failure',()=>{
 const no=setup(0);no.g.beginCombat();assert.throws(()=>no.g.activateMentalFortress(),/não foi adquirida/);
 const zero=setup(1,0);assert.throws(()=>zero.g.activateMentalFortress(),/encontro ativo/);zero.g.beginCombat();const before=clone(zero.g.state);
 assert.throws(()=>zero.g.activateMentalFortress(),/Destino luminoso/);assert.deepEqual(zero.g.state,before);
 const {g,repo}=setup(1);g.beginCombat();const snapshot=clone(g.state);repo.fail=true;
 assert.throws(()=>g.activateMentalFortress(),/disk failure/);assert.deepEqual(g.state,snapshot);assert.deepEqual(repo.saved,snapshot);
});
test('P167 Android bridge exposes mental defense and UI action',()=>{
 const {repo,g}=setup(1);g.beginCombat();const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.talentActionStatus().data.mentalFortress.available,true);
 assert.equal(bridge.activateMentalFortress().ok,true);assert.equal(bridge.talentActionStatus().data.mentalFortress.active,true);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');assert.ok(ui.includes("call('activateMentalFortress'"));
});
