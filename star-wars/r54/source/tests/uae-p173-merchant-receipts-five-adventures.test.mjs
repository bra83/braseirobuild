/** P173 — receipt-bound AoR merchant negotiation, persistence and five adventures. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {grantCatalogAsset,createPendingAcquisition} from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {readFileSync} from 'node:fs';

const copy=structuredClone;
const merchant=Object.values(AOR_TALENT_TREES).flatMap(ss=>Object.values(ss).flatMap(t=>t.nodes)).find(n=>n.originalName==='MERCADER EXPERTO');
class Repo {
 constructor(){this.saved=createCampaign('P173');this.fail=false;this.writes=0;}
 load(){return copy(this.saved);}
 save(s){if(this.fail)throw Error('P173 falha de disco');this.saved=copy(s);this.writes++;}
}
function game({bought=true,withAsset=true}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=bought?[{nodeId:merchant.id,name:merchant.name,originalName:merchant.originalName}]:[];
 c.talents=c.talentPurchases.map(x=>x.name);c.derived.strain.current=0;c.derived.strain.threshold=15;
 g.setCharacter(c);g.setCampaignMode('AOR');
 if(withAsset)g.state=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p173-item'}).state;
 repo.save(g.state);return {g,repo};
}
const rollSuccess=g=>g.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',rng:()=>0.2});
function pending(g,repo){
 const q=createPendingAcquisition('gear','glow_rod',g.state.player.character,{requestId:'p173-request'});
 g.state.master.pendingAcquisition=q;repo.save(g.state);return q;
}
test('P173: source-backed Negotiation roll pays strain before receipt, not at sale',()=>{
 const {g}=game();const before=g.state.player.character.derived.strain.current;
 const {check,receipt}=rollSuccess(g);
 assert.equal(check.success,true);assert.equal(receipt.success,true);assert.equal(receipt.strainPaid,2);
 assert.equal(g.state.player.character.derived.strain.current,before+2);
 assert.equal(g.state.master.merchantNegotiationReceipts.length,1);
 assert.equal(validateState(g.state),true);
 const result=g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 assert.equal(result.record.price,125);assert.equal(result.record.masterMerchant.prepaid,true);
 assert.equal(result.record.negotiationReceiptId,receipt.id);
 assert.equal(g.state.player.character.derived.strain.current,before+2);
 assert.equal(g.state.master.merchantNegotiationReceipts[0].consumed,true);
 assert.equal(g.state.group.ownedAssets.length,0);
});
test('P173: replayed receipt cannot sell another asset',()=>{
 const {g}=game();const receipt=rollSuccess(g).receipt;
 g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 g.state=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p173-next'}).state;
 assert.throws(()=>g.completeMerchantSale({assetId:'p173-next',price:100,receiptId:receipt.id}),/Recibo/);
 assert.equal(g.state.group.ownedAssets.length,1);
});
test('P173: receipt cannot be reused after reopening or rolled back to earlier object',()=>{
 const {g,repo}=game();const receipt=rollSuccess(g).receipt;
 const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
 reopened.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 for(let i=0;i<3;i++){
  const open=new GameController(repo);assert.equal(validateState(open.state),true);
  assert.throws(()=>open.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id}),/Recibo/);
 }
});
test('P173: failed Negotiation charges 2 strain, grants no sale bonus',()=>{
 const {g}=game();const {check,receipt}=g.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',rng:()=>0});
 assert.equal(check.success,false);assert.equal(g.state.player.character.derived.strain.current,2);
 assert.throws(()=>g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id}),/bem-sucedida/);
 assert.equal(g.state.group.ownedAssets.length,1);assert.equal(g.state.player.character.credits>=0,true);
});
test('P173: successful purchase consumes prepaid receipt without second strain charge',()=>{
 const {g,repo}=game({withAsset:false});pending(g,repo);
 const {receipt,check}=g.resolveMerchantNegotiation({kind:'buy',targetId:'p173-request',rng:()=>0.2});
 assert.equal(check.success,true);const before=g.state.player.character.credits;
 const result=g.completeMerchantAcquisition({receiptId:receipt.id,approved:true,finalPrice:100});
 assert.equal(result.record.finalPrice,75);
 assert.equal(g.state.player.character.credits,before-75);
 assert.equal(g.state.player.character.derived.strain.current,2);
 assert.equal(g.state.master.pendingAcquisition,null);
 assert.equal(g.state.group.ownedAssets.length,1);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P173: failed purchase records rejection, consumes receipt, keeps paid strain',()=>{
 const {g,repo}=game({withAsset:false});pending(g,repo);
 const {receipt,check}=g.resolveMerchantNegotiation({kind:'buy',targetId:'p173-request',rng:()=>0});
 assert.equal(check.success,false);
 const before=g.state.player.character.credits;
 assert.throws(()=>g.completeMerchantAcquisition({receiptId:receipt.id,approved:true,finalPrice:100}),/fracassada/);
 assert.equal(g.state.master.pendingAcquisition.requestId,'p173-request');
 const result=g.completeMerchantAcquisition({receiptId:receipt.id,approved:false});
 assert.equal(result.record.status,'rejeitada');assert.equal(g.state.player.character.credits,before);
 assert.equal(g.state.player.character.derived.strain.current,2);
 assert.equal(g.state.master.merchantNegotiationReceipts[0].consumed,true);
});
test('P173: receipt cannot migrate to another character, session, campaign or item',()=>{
 for(const key of ['character','session','campaign','item']){
  const {g}=game();const {receipt}=rollSuccess(g);
  if(key==='character'){const c=copy(g.state.player.character);c.name='Outro personagem';g.setCharacter(c);}
  if(key==='session')g.state.sessionId='sess_diferente';
  if(key==='campaign')g.state.campaignId='camp_diferente';
  if(key==='item'){g.state=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'outro'}).state;}
  assert.throws(()=>g.completeMerchantSale({assetId:key==='item'?'outro':'p173-item',price:100,receiptId:receipt.id}),/Recibo/);
 }
});
test('P173: no purchased talent means no receipt, no strain or changes',()=>{
 const {g,repo}=game({bought:false});const before=copy(g.state);
 assert.throws(()=>rollSuccess(g),/não adquirido/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P173: over-threshold strain rejects before rolling or modifying save',()=>{
 const {g,repo}=game();const c=copy(g.state.player.character);
 c.derived.strain.current=c.derived.strain.threshold-1;g.setCharacter(c);
 const before=copy(repo.saved);
 assert.throws(()=>rollSuccess(g),/Tensão insuficiente/);
 assert.deepEqual(g.state,before);
});
test('P173: save failure on negotiation rolls back paid strain and receipt',()=>{
 const {g,repo}=game();const before=copy(g.state);
 repo.fail=true;assert.throws(()=>rollSuccess(g),/disco/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P173: save failure on sale rolls back assets, credits and receipt consumption',()=>{
 const {g,repo}=game();const {receipt}=rollSuccess(g);const before=copy(g.state);
 repo.fail=true;assert.throws(()=>g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id}),/disco/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P173: corrupted receipt or missing event fails validation and reopening',()=>{
 for(const corruption of [
  s=>s.master.merchantNegotiationReceipts[0].strainPaid=0,
  s=>s.master.merchantNegotiationReceipts[0].success=false,
  s=>s.events=s.events.filter(e=>e.type!=='MERCHANT_NEGOTIATION_RECORDED'),
  s=>s.master.merchantNegotiationReceipts[0].consumed=true
 ]){
  const {g,repo}=game();rollSuccess(g);const bad=copy(g.state);corruption(bad);
  assert.throws(()=>validateState(bad),/Recibo|Consumo|evento/);
  repo.saved=bad;assert.throws(()=>migrateState(repo.saved),/Recibo|Consumo|evento/);
 }
});
test('P173: bridge exposes receipt-based operation and returns errors instead of side effects',()=>{
 const {g,repo}=game();const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 const out=bridge.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',rng:()=>0.2});
 assert.equal(out.ok,true);const receipt=out.data.receipt;
 const result=bridge.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 assert.equal(result.ok,true);
 const retry=bridge.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 assert.equal(retry.ok,false);assert.equal(validateState(new GameController(repo).state),true);
});
test('P173: invalid check parameters cannot create receipts or spend strain',()=>{
 const {g}=game();const before=copy(g.state);
 for(const params of [{difficulty:8},{challenge:-1},{boost:99},{setback:1.5}]){
  assert.throws(()=>g.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',...params}),/inválidos/);
  assert.deepEqual(g.state,before);
 }
});
test('P173: legacy P172 save without ledger migrates with no synthetic receipts',()=>{
 const {g}=game();const legacy=copy(g.state);delete legacy.master.merchantNegotiationReceipts;
 const reopened=migrateState(legacy);assert.deepEqual(reopened.master.merchantNegotiationReceipts,[]);
 assert.equal(validateState(reopened),true);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P173 ${id}: real published adventure and Merchant receipt survive three reopens`,()=>{
  const {g,repo}=game();
  g.setCampaignMode('MIXED');
  g.startPublishedAdventureSession(id,{rng:()=>0.5});
  const adventureBefore=copy(g.state.world.publishedAdventureRuntime);
  const {receipt}=rollSuccess(g);
  g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
  assert.deepEqual(g.state.world.publishedAdventureRuntime,adventureBefore);
  for(let n=0;n<3;n++){
   const open=new GameController(repo);
   assert.equal(validateState(open.state),true);
   assert.deepEqual(open.state.world.publishedAdventureRuntime,adventureBefore);
   assert.equal(open.state.master.merchantNegotiationReceipts[0].consumed,true);
   assert.equal(open.state.group.ownedAssets.length,0);
  }
 });
}

test('P173: receipt ledger refuses to discard 32 unconsumed rolls and rolls back the 33rd strain',()=>{
 const {g,repo}=game();const c=copy(g.state.player.character);c.derived.strain.threshold=100;g.setCharacter(c);
 for(let i=0;i<32;i++)g.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',rng:()=>0.2});
 const before=copy(g.state);assert.equal(before.master.merchantNegotiationReceipts.length,32);
 assert.throws(()=>g.resolveMerchantNegotiation({kind:'sell',targetId:'p173-item',rng:()=>0.2}),/Limite de recibos/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P173: event and consumption timestamp tampering are rejected',()=>{
 const {g}=game();const receipt=rollSuccess(g).receipt;
 g.completeMerchantSale({assetId:'p173-item',price:100,receiptId:receipt.id});
 const bad=copy(g.state);bad.master.merchantNegotiationReceipts[0].consumedAt='2099-01-01T00:00:00Z';
 assert.throws(()=>validateState(bad),/Consumo/);
 const bad2=copy(g.state);
 bad2.events.find(e=>e.type==='MERCHANT_NEGOTIATION_RECORDED').payload.receipt.strainPaid=0;
 assert.throws(()=>validateState(bad2),/Recibo/);
});

test('P173: Android UI binds roll, receipt, GM authorization, sale and purchase; offline bundle 81 modules',()=>{
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const marker of ['data-p173-roll','data-p173-receipt','data-p173-authorized','data-p173-sell',
   'data-p173-buy-roll','data-p173-buy-receipt','data-p173-buy-approved','data-p173-buy'])
  assert.ok(ui.includes(marker),marker);
 for(const method of ['resolveMerchantNegotiation','completeMerchantSale','completeMerchantAcquisition'])
  assert.ok(bundle.includes(method),method);
 assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=81);
 assert.ok(/1\.0\.(17[4-9]|1[89][0-9]|[2-9][0-9]{2})/.test(ui)); // Version must not regress below P174
});
