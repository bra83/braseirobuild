/** P172 — Age of Rebellion Master Merchant: purchased source, real asset/credit/strain transaction. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {masterMerchantOption} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {grantCatalogAsset,sellOwnedAsset,createPendingAcquisition,resolvePendingAcquisition} from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(ss=>Object.values(ss).flatMap(t=>t.nodes));
const merchantNode=nodes.find(n=>n.originalName==='MERCADER EXPERTO');
class Repo{constructor(){this.saved=createCampaign('p172');this.fail=false;this.writes=0;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('disk failure P172');this.saved=copy(s);this.writes++;}}
function game({bought=true,withWheel=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=bought?[{nodeId:merchantNode.id,name:merchantNode.name,originalName:merchantNode.originalName}]:[];
 if(withWheel){const n=nodes.find(x=>x.originalName==='CHANCHULLERO');c.talentPurchases.push({nodeId:n.id,name:n.name,originalName:n.originalName});}
 c.talents=c.talentPurchases.map(x=>x.name);c.derived.strain.current=0;c.derived.strain.threshold=Math.max(10,c.derived.strain.threshold);
 g.setCharacter(c);g.setCampaignMode('AOR');return {g,repo};
}
function owned(state,id='p172-item'){
 return grantCatalogAsset(state,{kind:'gear',itemId:'glow_rod',ownedAssetId:id}).state;
}
function pending(c){return createPendingAcquisition('gear','glow_rod',c,{requestId:'p172-request'});}
test('P172: source-backed merchant slot becomes mechanically active, not a label alias',()=>{
 assert.ok(merchantNode);assert.equal(aorTalentCatalog().filter(n=>n.originalName==='MERCADER EXPERTO'&&n.mechanicallyActive).length,1);
 assert.ok(aorTalentCoverage().mechanicalPositions>=325); // P174 adds Redundant Systems + Bad Motivator, preserving Merchant
 const {g}=game({bought:false});const c=copy(g.state.player.character);c.talents.push('Mercador especialista');
 assert.throws(()=>masterMerchantOption(c,{activated:true,checkSucceeded:true,basePrice:100}),/não adquirido/);
 const fake=copy(c);fake.talentPurchases=[{nodeId:'forged',originalName:'MERCADER EXPERTO'}];
 assert.throws(()=>masterMerchantOption(fake,{activated:true,checkSucceeded:true,basePrice:100}),/não adquirido/);
});
test('P172: successful sale adds 25% base price plus independent Wheel and Deal 10%; consumes exactly 2 strain',()=>{
 const {g,repo}=game({withWheel:true});const s=owned(g.state);const before=s.player.character;
 const r=sellOwnedAsset(s,'p172-item',{price:101,legalSale:true,masterMerchant:true,negotiationSucceeded:true,sourceRuleIds:['AoR:GM:Negotiation']});
 assert.equal(r.record.basePrice,101);assert.equal(r.record.talentBonus,10);assert.equal(r.record.masterMerchant.bonus,25);
 assert.equal(r.record.price,136);assert.equal(r.state.player.character.credits,before.credits+136);
 assert.equal(r.state.player.character.derived.strain.current,2);assert.equal(r.state.group.ownedAssets.length,0);
 assert.equal(r.state.player.character.gear.some(x=>x.ownedAssetId==='p172-item'),false);
 repo.save(r.state);assert.equal(validateState(new GameController(repo).state),true);
 assert.equal(new GameController(repo).state.player.character.derived.strain.current,2);
});
test('P172: buy with a successful Negotiation check deducts 25% and spends 2 strain atomically',()=>{
 const {g,repo}=game();const s=copy(g.state);s.player.character.credits=1000;
 const q=pending(s.player.character);
 const r=resolvePendingAcquisition(s,q,{approved:true,finalPrice:101,masterMerchant:true,negotiationSucceeded:true,sourceRuleIds:['AoR:GM:Negotiation']});
 assert.equal(r.record.finalPrice,76);assert.equal(r.record.masterMerchant.bonus,25);
 assert.equal(r.state.player.character.credits,924);assert.equal(r.state.player.character.derived.strain.current,2);
 assert.equal(r.state.group.ownedAssets.length,1);repo.save(r.state);
 for(let i=0;i<3;i++)assert.equal(new GameController(repo).state.player.character.credits,924);
});
test('P172: failed negotiation spends the declared strain without granting a discounted item',()=>{
 const {g}=game();const s=copy(g.state);const q=pending(s.player.character);
 const r=resolvePendingAcquisition(s,q,{approved:false,masterMerchant:true,negotiationSucceeded:false,sourceRuleIds:['GM']});
 assert.equal(r.record.status,'rejeitada');assert.equal(r.state.player.character.derived.strain.current,2);
 assert.equal(r.state.group.ownedAssets.length,0);
});
test('P172: no activation preserves old sales and acquisitions byte-for-byte in player resources',()=>{
 const {g}=game();const s=owned(g.state);const sale=sellOwnedAsset(s,'p172-item',{price:100,legalSale:false,sourceRuleIds:['GM']});
 assert.equal(sale.record.price,100);assert.equal(sale.state.player.character.derived.strain.current,0);
 const q=pending(g.state.player.character);
 const r=resolvePendingAcquisition(g.state,q,{approved:false,sourceRuleIds:['GM']});
 assert.deepEqual(r.state,g.state);
});
for(const options of [
 {masterMerchant:true,negotiationSucceeded:false},
 {masterMerchant:'true',negotiationSucceeded:true},
 {masterMerchant:true,negotiationSucceeded:'true'}
])test(`P172: invalid activation ${JSON.stringify(options)} never grants credits`,()=>{
 const {g}=game();const s=owned(g.state);assert.throws(()=>sellOwnedAsset(s,'p172-item',{price:100,legalSale:true,sourceRuleIds:['GM'],...options}));
 assert.equal(s.player.character.derived.strain.current,0);assert.equal(s.group.ownedAssets.length,1);
});
test('P172: over-threshold voluntary strain rejected without losing the item',()=>{
 const {g}=game();const s=owned(g.state);s.player.character.derived.strain.current=s.player.character.derived.strain.threshold-1;
 assert.throws(()=>sellOwnedAsset(s,'p172-item',{price:100,legalSale:true,masterMerchant:true,negotiationSucceeded:true,sourceRuleIds:['GM']}),/Tensão insuficiente/);
 assert.equal(s.group.ownedAssets.length,1);
});
test('P172: bridge quotes authorized sale and reports master merchant without modifying save',()=>{
 const {g,repo}=game({withWheel:true});repo.save(owned(g.state));const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 const saved=copy(repo.saved),writes=repo.writes;
 const q=bridge.legalSaleQuote({assetId:'p172-item',basePrice:100,masterMerchant:true});
 assert.equal(q.ok,true);assert.equal(q.data.finalPrice,135);assert.equal(q.data.masterMerchant.strainCost,2);
 assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);
});
