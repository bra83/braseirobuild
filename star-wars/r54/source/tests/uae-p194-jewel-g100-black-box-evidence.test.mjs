import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelBankState} from '../app/src/main/assets/vtt/js/adventure/jewelBankingOperations.mjs';
const clone=structuredClone;
const adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P194');this.saves=0;}load(){return clone(this.state)}save(s){this.state=clone(s);this.saves++}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.state.player.character.gear.push({id:'restraining_bolt',name:'Parafuso de restrição (droide)',quantity:1,encumbrance:0});g.state.player.character.gear.push({id:'tool_kit',name:'Kit de ferramentas',quantity:1,encumbrance:4});g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:`round${i}`});}
 g.advancePublishedAdventure('bank-operation');return {g,repo};}
function ready(g){g.resolveJewelBankStep({actionId:'acquire',step:'acquire',target:'industrial_docks',restrainingBolt:true,rng:()=>.49});
 g.resolveJewelBankStep({actionId:'disable',step:'disable',rng:()=>.49});g.resolveJewelBankStep({actionId:'slice',step:'slice',rng:()=>.49});}
function clean(g,rng=()=>.49){return g.resolveJewelBankStep({actionId:'clean',step:'clean',rng});}
const ev=(g,action,other={})=>g.resolveJewelBankEvidence({actionId:`ev_${action}_${Object.keys(g.state.world.publishedAdventure.operations.receipts).length}`,action,...other});
test('P194 official Leave No Trace: disassembly costs two real hours, including on failure',()=>{
 const {g}=game();ready(g);const before=g.state.world.clock.elapsedMinutes??0;
 const r=clean(g);assert.equal(r.adjudication.evidence.pool.difficulty,4);
 assert.equal(g.state.world.clock.elapsedMinutes,before+120);
 const ledger=g.state.world.taskTimeApplications.task_jewel_cleanup_clean;
 assert.deepEqual(ledger,{checkId:'task_jewel_cleanup_clean',skillKey:'mecanica',minutes:120,baseMinutes:120});
 assert.equal(validateJewelBankState(g.state.world.publishedAdventure,g.state.world),true);
});
test('P194 failed disassembly still spends 120 minutes without extracting evidence',()=>{
 const {g}=game();ready(g);const before=g.state.world.clock.elapsedMinutes??0;
 const r=clean(g,()=>.99);assert.equal(r.adjudication.success,false);
 assert.equal(g.state.world.clock.elapsedMinutes,before+120);
 assert.equal(r.bank.evidenceCleared,false);
 assert.throws(()=>ev(g,'take_box'),/Caixa-preta|antes da desmontagem/);
});
test('P194 cannot dispose of the black box before successful extraction',()=>{
 const {g,repo}=game();ready(g);const state=clone(g.state),saves=repo.saves;
 assert.throws(()=>ev(g,'destroy_box'),/Caixa-preta|antes da desmontagem/);
 assert.deepEqual(g.state,state);assert.equal(repo.saves,saves);
});
test('P194 stolen black box remains incriminating; parts can be dumped only after confirmed travel',()=>{
 const {g}=game();ready(g);clean(g);
 const taken=ev(g,'take_box');assert.equal(taken.bankEvidence.blackBox,'taken');assert.equal(taken.bankEvidence.blackBoxRisk,true);
 assert.throws(()=>ev(g,'destroy_box'),/já decidida/);
 assert.throws(()=>ev(g,'dump_parts'),/Confirme chegada/);
 const disposed=ev(g,'dump_parts',{reachedCityEdge:true});assert.equal(disposed.bankEvidence.parts,'dumped');
 assert.equal(g.state.player.character.credits,game().g.state.player.character.credits);
 assert.throws(()=>ev(g,'fence_parts'),/duplicado|fora de ordem|antes de descartar/);
});
test('P194 Hard Streetwise uses actual dice and does not fabricate payment',()=>{
 const {g}=game();ready(g);clean(g);ev(g,'destroy_box');
 const before=g.state.player.character.credits;
 const fenced=ev(g,'fence_parts',{rng:()=>.49});
 assert.equal(fenced.adjudication.evidence.skill,'callejeo');
 assert.equal(fenced.adjudication.evidence.pool.difficulty,3);
 assert.equal(fenced.adjudication.success,fenced.adjudication.evidence.roll.success);
 assert.equal(g.state.player.character.credits,before);
 if(fenced.adjudication.success)assert.equal(fenced.bankEvidence.parts,'fenced');
 else assert.equal(fenced.bankEvidence.parts,null);
});
test('P194 forged bank evidence receipt and cleanup clock are rejected',()=>{
 const {g}=game();ready(g);clean(g);ev(g,'destroy_box');ev(g,'fence_parts',{rng:()=>.49});
 const orig=clone(g.state.world);
 for(const mutate of [w=>{w.publishedAdventure.operations.bankEvidence.blackBox='taken';},
  w=>{const r=Object.values(w.publishedAdventure.operations.receipts).find(x=>x.kind==='BANK_EVIDENCE'&&x.action==='fence_parts');r.evidence.roll.faces[0].index=99;},
  w=>{w.taskTimeApplications.task_jewel_cleanup_clean.minutes=10;},
  w=>{w.publishedAdventure.operations.receipts.clean.timeCheckId='task_forged';}]){
  const w=clone(orig);mutate(w);assert.throws(()=>adapter.status(w));
 }
});
test('P194 three reopenings preserve evidence decisions and ledger',()=>{
 const {g,repo}=game();ready(g);clean(g);ev(g,'destroy_box');ev(g,'dump_parts',{reachedCityEdge:true});
 for(let i=0;i<3;i++){const reopened=new GameController(repo),s=adapter.status(reopened.state.world);
  assert.equal(s.bankEvidence.blackBox,'destroyed');assert.equal(s.bankEvidence.parts,'dumped');
  assert.equal(reopened.state.world.taskTimeApplications.task_jewel_cleanup_clean.minutes,120);}
});
test('P194 legacy P193 cleanup receipt without time remains valid without retroactive charge',()=>{
 const {g}=game();ready(g);clean(g);const world=clone(g.state.world);
 delete world.publishedAdventure.operations.receipts.clean.timeCheckId;
 delete world.publishedAdventure.operations.receipts.clean.cleanupVersion;
 delete world.taskTimeApplications.task_jewel_cleanup_clean;
 world.clock.elapsedMinutes-=120;
 assert.equal(validateJewelBankState(world.publishedAdventure,world),true);
 assert.equal(adapter.status(world).bankEvidence,null);
});
test('P194 Android bridge and PT-BR source options are bundled',()=>{
 const base=new URL('../app/src/main/assets/vtt/',import.meta.url);
 const ui=readFileSync(new URL('r46/final-runtime.js',base),'utf8');
 const bundle=readFileSync(new URL('js/core/uiBridge.bundle.js',base),'utf8');
 for(const phrase of ['DESMONTAR G-100 (2H)','DESTRUIR CAIXA-PRETA','DISTRIBUIR PEÇAS','resolveJewelBankEvidence'])assert.ok(ui.includes(phrase),phrase);
 assert.ok(bundle.includes('resolveJewelBankEvidence'));
});
