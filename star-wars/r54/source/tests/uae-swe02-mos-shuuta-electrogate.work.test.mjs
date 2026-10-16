import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {SWE02_SITE_OPERATIONS,listSWE02PalaceOperations,executeSWE02PalaceOperation,validateSWE02PalaceOperations} from '../app/src/main/assets/vtt/js/adventure/swe02PalaceOperations.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>a.advance(w,id).world;
const gate=()=>['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return','mos-gate-investigate'].reduce(step,a.start({}).world);
const c={id:'cael',name:'Cael',credits:0,characteristics:{intelecto:5},skills:{computadoras:3}};
class Repo{constructor(){this.s=createCampaign('Electrogate');this.saves=0}load(){return structuredClone(this.s)}save(s){this.s=structuredClone(s);this.saves++}}
function game(w){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world:w});return {g,repo};}
test('p.40: eletroportão energizado; teste Difícil real, quatro guardas, sem teletransporte para palácio',()=>{
 let w=gate();assert.ok(SWE02_SITE_OPERATIONS.some(op=>op.id==='mos-electrogate-disable'));
 assert.equal(a.status(w).node.id,'mos-shuuta-electrogate');
 assert.equal(listSWE02PalaceOperations(w).length,1);
 assert.throws(()=>step(w,'mos-gate-guards'),/inválida/);
 assert.throws(()=>executeSWE02PalaceOperation(w,{id:'c',characteristics:{intelecto:0}}, {operationId:'mos-electrogate-disable'}),/ficha inválida/);
 let n=0;const r=executeSWE02PalaceOperation(w,c,{operationId:'mos-electrogate-disable',rng:()=>n++<8?.36:0});w=r.world;
 assert.equal(r.receipt.check.pool.difficulty,3);assert.equal(r.receipt.barracksCount,4);
 assert.equal(r.receipt.success,true);assert.equal(w.publishedAdventure.flags.electrogateBarracksDiverted,true);
 assert.equal(validateSWE02PalaceOperations(w),true);
 assert.equal(listSWE02PalaceOperations(w).length,0);
 assert.throws(()=>executeSWE02PalaceOperation(w,c,{operationId:'mos-electrogate-disable'}),/indisponível/);
 w=step(w,'mos-gate-guards');assert.equal(a.status(w).node.id,'palace-front-gate');
 assert.equal(w.publishedAdventure.flags.teemoDefeated,undefined);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(validateSWE02PalaceOperations(w),true);}
 const forged=structuredClone(w);forged.publishedAdventure.flags.electrogateBarracksDiverted=false;
 assert.throws(()=>a.status(forged),/eletroportão comprovado/);
});
test('p.40: falha não afasta guardas, tentativas persistem e não geram reforço inexistente',()=>{
 let w=gate(),failed=executeSWE02PalaceOperation(w,c,{operationId:'mos-electrogate-disable',rng:()=>0});
 assert.equal(failed.receipt.success,false);assert.equal(failed.world.publishedAdventure.flags.electrogateBarracksDiverted,undefined);
 assert.throws(()=>step(failed.world,'mos-gate-guards'),/inválida/);
 assert.equal(listSWE02PalaceOperations(failed.world).length,1);
 const forged=structuredClone(failed.world);forged.publishedAdventure.palaceOperationReceipts[0].barracksCount=99;
 assert.throws(()=>validateSWE02PalaceOperations(forged),/adulterado/);
});
test('Controller: barracks diversion blocks Teemo alarm; three persistent reopens and rollback',()=>{
 const {g,repo}=game(gate());const ch=structuredClone(g.state.player.character);ch.characteristics.intelecto=5;ch.skills.computadoras=3;g.commit('CHARACTER_UPDATED',{character:ch});
 const before=repo.saves;
 assert.throws(()=>g.executeSWE02PalaceOperation({operationId:'workshop-toolkit'}),/indisponível/);assert.equal(repo.saves,before);
 let n=0;const r=g.executeSWE02PalaceOperation({operationId:'mos-electrogate-disable',rng:()=>n++<8?.36:0});
 assert.equal(r.success,true);assert.equal(g.state.world.publishedAdventure.flags.electrogateBarracksDiverted,true);
 g.advancePublishedAdventure('mos-gate-guards');g.commit('WORLD_UPDATED',{world:a.adjudicateCheck(g.state.world,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:0}}).world});
 g.advancePublishedAdventure('palace-gate-debt-bluff');g.advancePublishedAdventure('palace-enter-throne');
 g.beginPublishedAdventureEncounter('teemo-palace-guards',{rng:()=>.999,palaceAdditionalHenchmenCount:2,gmHuttProfileConfirmed:true});
 const combatBefore=JSON.stringify(g.state.combat),saves=repo.saves;
 assert.throws(()=>g.queueSWE02ThroneAlarm({confirmedBarracksCount:4}),/já estão no eletroportão/);
 assert.equal(JSON.stringify(g.state.combat),combatBefore);assert.equal(repo.saves,saves);
 for(let i=0;i<3;i++){const r=new GameController(repo);assert.equal(validateSWE02PalaceOperations(r.state.world),true);assert.equal(r.state.world.publishedAdventure.flags.electrogateBarracksDiverted,true);}
});
