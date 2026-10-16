import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {SWE02_PALACE_OPERATIONS,executeSWE02PalaceOperation,listSWE02PalaceOperations,validateSWE02PalaceOperations} from '../app/src/main/assets/vtt/js/adventure/swe02PalaceOperations.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>adapter.advance(w,id).world;
function arrive(){return ['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return'].reduce(step,adapter.start({}).world)}
function reception(){let w=step(arrive(),'approach-fortified-gate');w=adapter.adjudicateCheck(w,{checkIndex:0,result:{success:true,netAdvantage:0}}).world;return step(w,'palace-gate-debt-bluff')}
function at(choice){return step(reception(),choice)}
const c={id:'test-player',name:'Teste',credits:0,gear:[]};
class Repo{constructor(){this.s=createCampaign('WIP4 palace batch')} load(){return structuredClone(this.s)}save(s){this.s=structuredClone(s)}}
function withGame(w){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world:w});return {g,repo}}
const reopening=repo=>{for(let i=0;i<3;i++){const g=new GameController(repo);assert.equal(validateSWE02PalaceOperations(g.state.world),true)}};
test('DATA official pp.43–44: bulk 11 authored operations; no arbitrary op or NPC-specific executor',()=>{
 assert.equal(SWE02_PALACE_OPERATIONS.length,11);
 assert.equal(new Set(SWE02_PALACE_OPERATIONS.map(x=>x.id)).size,11);
 for(const op of SWE02_PALACE_OPERATIONS){assert.ok([43,44].includes(op.page));assert.ok(['GEAR','SEALED','EVIDENCE','OBSERVATION'].includes(op.mode))}
 assert.equal(listSWE02PalaceOperations(reception()).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,0);
 assert.throws(()=>executeSWE02PalaceOperation(reception(),c,{operationId:'workshop-toolkit'}),/indisponível/);
});
test('Workshop: both certified EotE gear allocations and six source-authored locations/actions through shared executor',()=>{
 let w=at('palace-enter-workshop'),character=structuredClone(c);
 assert.equal(listSWE02PalaceOperations(w).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,7);
 for(const op of ['workshop-toolkit','workshop-lights','workshop-repair','workshop-stimpacks','workshop-carbines','workshop-vibroaxes','workshop-b1-worklog']){
  const r=executeSWE02PalaceOperation(w,character,{operationId:op});w=r.world;character=r.character;
  assert.equal(r.receipt.source.productCode,'SWE02');assert.equal(r.receipt.source.printedPage,43);
  assert.equal(validateSWE02PalaceOperations(w),true);
  assert.throws(()=>executeSWE02PalaceOperation(w,character,{operationId:op}),/indisponível/);
 }
 assert.equal(character.gear.length,2);assert.equal(character.gear[0].id,'tool_kit');assert.equal(character.gear[1].quantity,2);
 assert.equal(w.publishedAdventure.palaceOperationReceipts.filter(x=>x.sealedAsset).length,4);
 assert.equal(character.weapons?.length||0,0);
 assert.equal(w.publishedAdventure.flags.b1ReverseEngineeringEvidence,true);
 const forged=structuredClone(w);forged.publishedAdventure.palaceOperationReceipts[0].quantity=200;
 assert.throws(()=>validateSWE02PalaceOperations(forged),/adulterada/);
});
test('Kitchen: cannot grant ryll evidence without adjudicated successful source check',()=>{
 let w=at('palace-enter-kitchen');
 assert.equal(listSWE02PalaceOperations(w).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,0);
 assert.throws(()=>executeSWE02PalaceOperation(w,c,{operationId:'kitchen-ryll'}),/não comprovada/);
 w=adapter.adjudicateCheck(w,{checkIndex:0,result:{success:true,netAdvantage:0}}).world;
 assert.equal(listSWE02PalaceOperations(w).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,1);
 const out=executeSWE02PalaceOperation(w,c,{operationId:'kitchen-ryll'});
 assert.equal(out.world.publishedAdventure.flags.ryllKitchenEvidence,true);
 const forged=structuredClone(out.world);forged.publishedAdventure.checkHistory=[];
 assert.throws(()=>validateSWE02PalaceOperations(forged),/teste real/);
});
test('Bar & landing pad: new official locations reachable without inventing combat/effects',()=>{
 let w=at('palace-enter-bar');assert.equal(adapter.status(w).node.id,'palace-cantina-bar');
 assert.equal(listSWE02PalaceOperations(w).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,2);
 for(const id of ['bar-cook','bar-cover'])w=executeSWE02PalaceOperation(w,c,{operationId:id}).world;
 assert.equal(w.publishedAdventure.flags.palaceCookHarmless,true);
 assert.equal(validateSWE02PalaceOperations(w),true);
 const pad=at('palace-enter-pad');assert.equal(adapter.status(pad).node.id,'palace-landing-pad');
 assert.match(adapter.status(pad).node.summary,/silhueta 3/);
 assert.equal(listSWE02PalaceOperations(pad).filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,0);
});
test('Bith: interview is gated by real location, source-authored witness and idempotency',()=>{
 const w=at('palace-enter-spy'),r=executeSWE02PalaceOperation(w,c,{operationId:'spy-bith-testimony'});
 assert.equal(r.world.publishedAdventure.flags.palaceBithWitnessEvidence,true);
 assert.equal(validateSWE02PalaceOperations(r.world),true);
 const forged=structuredClone(r.world);forged.publishedAdventure.visitedNodeIds=forged.publishedAdventure.visitedNodeIds.filter(x=>x!=='palace-spy-interview');
 assert.throws(()=>validateSWE02PalaceOperations(forged),/testemunha não visitada/);
});
test('Live Controller/Bridge/UI: one generic call handles all operations and 3 reopens, rollback on failure',()=>{
 const {g,repo}=withGame(at('palace-enter-workshop'));
 const pre=JSON.stringify(g.state);
 assert.throws(()=>g.executeSWE02PalaceOperation({operationId:'kitchen-ryll'}),/não comprovada/);
 assert.equal(JSON.stringify(g.state),pre);
 assert.equal(g.swe02PalaceOperationCatalog().filter(o=>SWE02_PALACE_OPERATIONS.some(p=>p.id===o.id)).length,7);
 const result=g.executeSWE02PalaceOperation({operationId:'workshop-toolkit'});
 assert.equal(result.usable,true);
 assert.equal(g.state.player.character.gear.some(x=>x.id==='tool_kit'&&x.ownedAssetId?.startsWith('swe02-palace:')),true);
 reopening(repo);
 const bridge=new BraseiroUiBridge({repository:repo});
 assert.equal(typeof bridge.swe02PalaceOperationCatalog,'function');assert.equal(typeof bridge.executeSWE02PalaceOperation,'function');
 const base=new URL('../app/src/main/assets/vtt/',import.meta.url);
 const html=fs.readFileSync(new URL('index.html',base),'utf8'),ui=fs.readFileSync(new URL('js/ui/app.mjs',base),'utf8');
 for(const id of ['swe02PalaceDeclarative','swe02PalaceOperationSelect','swe02PalaceOperationCommit']){
  assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));
 }
});
test('Two independently witnessed palace proofs feed existing universal ending executor without Computers flag',async()=>{
 const {act3VerifiedEvidence,prepareSWE02AnattaDossier}=await import('../app/src/main/assets/vtt/js/adventure/swe02Act3Resolution.mjs');
 let w=at('palace-enter-workshop');w=executeSWE02PalaceOperation(w,c,{operationId:'workshop-b1-worklog'}).world;
 w=step(w,'palace-workshop-return');
 w=step(w,'palace-enter-spy');w=executeSWE02PalaceOperation(w,c,{operationId:'spy-bith-testimony'}).world;
 assert.deepEqual(act3VerifiedEvidence(w.publishedAdventure),['B1_DROIDS','JABBA_SPY']);
 const d=prepareSWE02AnattaDossier(w);assert.deepEqual(d.evidence,['B1_DROIDS','JABBA_SPY']);
 assert.equal(adapter.status(d.world).active,true);
});
