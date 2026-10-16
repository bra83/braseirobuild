import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelBankEvidence} from '../app/src/main/assets/vtt/js/adventure/jewelBankingOperations.mjs';
import {validatePublishedAdventureRuntimeState,publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
const adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P187');this.saves=0;}load(){return copy(this.state);}save(state){this.state=copy(state);this.saves++;}}
function game(){
 const repo=new Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.state.player.character.gear.push({id:'restraining_bolt',name:'Parafuso de restrição (droide)',quantity:1,encumbrance:0});g.state.player.character.gear.push({id:'tool_kit',name:'Kit de ferramentas',quantity:1,encumbrance:4});g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){
   if(g.state.world.publishedAdventure.operations?.auction?.completed)break;
   g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});
 }
 g.advancePublishedAdventure('bank-operation');
 return {g,repo};
}
function op(g,step,extra={}){return g.resolveJewelBankStep({actionId:step+'-'+(g.state.world.publishedAdventure.operations?.bank?.history.length||0),step,rng:()=>.49,...extra});}
function validate(g){assert.equal(adapter.status(g.state.world).active,true);
 assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(adapter.meta),g.state.world.publishedAdventureRuntime,g.state.world.publishedAdventure),true);}
test('P187 new sessions reject fabricated manual bank success',()=>{
 const {g}=game();assert.equal(g.state.world.publishedAdventure.bankContractVersion,1);
 assert.throws(()=>g.resolvePublishedAdventureCheck(0,{success:true}),/droid|G-100|bancária|adjudicação manual/);
 assert.throws(()=>g.advancePublishedAdventure('divert-winning-bid'),/inválida/);
});
test('P187 G-100 must be captured, disabled and then sliced; all stages use real dice',()=>{
 const {g}=game();assert.throws(()=>op(g,'slice'),/Incapacite/);
 const acquire=op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});
 assert.equal(acquire.adjudication.kind,'BANK_STEP');assert.equal(acquire.adjudication.evidence.pool.difficulty,1);
 assert.equal(validateJewelBankEvidence(acquire.adjudication.evidence,'acquire',{},{target:'industrial_docks',restrainingBolt:true}),true);
 assert.equal(acquire.bank.stage,'SECURED');
 const disabled=op(g,'disable');assert.equal(disabled.adjudication.evidence.pool.challenge,0);assert.equal(disabled.bank.stage,'READY');
 const sliced=op(g,'slice');assert.equal(sliced.adjudication.evidence.pool.challenge,2);assert.equal(sliced.bank.diverted,true);
 assert.equal(g.state.world.publishedAdventure.flags.bankDiversionSecured,true);validate(g);
 g.advancePublishedAdventure('divert-winning-bid');assert.equal(g.state.world.publishedAdventure.currentNodeId,'vault-infiltration');
});
test('P187 droid location and restraint change effective dice difficulty',()=>{
 const {g}=game();const r=op(g,'acquire',{target:'tourist_district',restrainingBolt:false,rng:()=>.1});
 assert.equal(r.adjudication.evidence.pool.difficulty,2);assert.equal(r.adjudication.evidence.pool.challenge,1);
});
test('P187 failed acquisition does not invent droid or bank access',()=>{
 const {g}=game();const r=op(g,'acquire',{target:'tourist_district',restrainingBolt:false,rng:()=>.99});
 assert.equal(r.bank.stage,'SEARCH');assert.equal(r.bank.target,null);assert.equal(r.bank.diverted,false);
});
test('P187 failed disabling activates alarm and upgrades next slicing check',()=>{
 const {g}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:false});
 const failed=op(g,'disable',{rng:()=>.99});assert.equal(failed.bank.alarm,true);
 op(g,'disable',{rng:()=>.49});
 const sliced=op(g,'slice');assert.equal(sliced.adjudication.evidence.pool.challenge,3);
});
test('P187 failed slicing retries upgrade pool, consume 30min and lock after four failures',()=>{
 const {g}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});op(g,'disable');
 for(let i=0;i<4;i++){
  const r=op(g,'slice',{rng:()=>.99});
  assert.equal(r.bank.attempts,i+1);assert.equal(r.bank.minutesSpent,Math.max(0,i)*30);
  assert.equal(r.adjudication.evidence.pool.challenge,2+i);
 }
 assert.equal(g.state.world.publishedAdventure.operations.bank.locked,true);
 assert.throws(()=>op(g,'slice'),/bloqueado/);
 assert.throws(()=>g.advancePublishedAdventure('divert-winning-bid'),/inválida/);
});
test('P187 G-100 black box requires a real Daunting Mechanics check',()=>{
 const {g}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});op(g,'disable');op(g,'slice');
 const cleanup=op(g,'clean');assert.equal(cleanup.adjudication.evidence.pool.difficulty,4);
 assert.equal(cleanup.bank.evidenceCleared,true);validate(g);
});
test('P187 forged faces, totals, phases and flags rejected during save validation',()=>{
 const {g}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});
 for(const mutate of [
  p=>p.operations.receipts['acquire-0'].evidence.roll.faces[0].index=99,
  p=>p.operations.bank.stage='FINISHED',
  p=>p.flags.bankDiversionSecured=true,
  p=>p.operations.receipts['acquire-0'].success=false,
 ]){
  const forged=copy(g.state.world);mutate(forged.publishedAdventure);
  assert.throws(()=>adapter.status(forged),/adulterad|inconsistente|sem transferência|não corresponde/);
 }
});
test('P187 duplicate action and malformed stage are atomic, no saves or partial state',()=>{
 const {g,repo}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});
 const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>g.resolveJewelBankStep({actionId:'acquire-0',step:'disable'}),/registrada/);
 assert.throws(()=>g.resolveJewelBankStep({actionId:'unknown',step:'bank_bypass'}),/desconhecida/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P187 bank receipts and UAE persist over three controller reopenings',()=>{
 const {g,repo}=game();op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});op(g,'disable');op(g,'slice');
 const before=copy(g.state.world.publishedAdventure.operations.bank);
 for(let i=0;i<3;i++){const restored=new GameController(repo);
  assert.deepEqual(adapter.status(restored.state.world).bankStatus,before);validate(restored);
 }
});
test('P187 legacy saves without bankContractVersion preserve manual check path',()=>{
 const {g}=game();const old=copy(g.state.world);
 delete old.publishedAdventure.bankContractVersion;
 const out=adapter.adjudicateCheck(old,{checkIndex:0,result:{success:true}});
 const advanced=adapter.advance(out.world,'divert-winning-bid');
 assert.equal(advanced.progress.flags.bankDiversionSecured,true);
});
test('P187 bank flag without receipts and auction winner mismatch are rejected',()=>{
 const {g}=game();
 const forged=copy(g.state.world);forged.publishedAdventure.flags.bankDiversionSecured=true;
 assert.throws(()=>adapter.status(forged),/sem recibo/);
 op(g,'acquire',{target:'industrial_docks',restrainingBolt:true});op(g,'disable');op(g,'slice');
 const changed=copy(g.state.world);changed.publishedAdventure.operations.auction.highBidderId='elaiza';
 assert.throws(()=>adapter.status(changed),/leilão|inconsistente/);
});
test('P187 Android bridge exposes banking and bundled code',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 assert.equal(typeof BraseiroUiBridge.prototype.resolveJewelBankStep,'function');
 assert.equal(typeof GameController.prototype.adjudicateJewelOperation,'function');
 const {readFileSync}=await import('node:fs');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.ok(bundle.includes('jewelBankingOperations.mjs'));
 assert.ok(bundle.includes('resolveJewelBankStep'));
 const app=readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8');
 const version=app.match(/versionName\s*=\s*"([^"]+)"/)?.[1];
 assert.ok(version&&bundle.includes(`Braseiro FINAL ${version} offline engine bundle`));
});
