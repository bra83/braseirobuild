import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createPendingAcquisition, resolvePendingAcquisition, genericEconomyCatalog, grantCatalogAsset, loseOwnedAsset } from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import { effectiveWeaponWithAttachments } from '../app/src/main/assets/vtt/js/rules/attachments.mjs';
import { weaponAttackModifiers, activeWeaponQualities } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { itemHardPoints, itemAvailableHardPoints } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

class MemoryRepository{constructor(state=createCampaign('P99')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function game(){const repo=new MemoryRepository(),g=new GameController(repo);g.loadStarterCharacter('cael');return {g,repo}}
function ownedSetup(){let {g,repo}=game();let s=g.state;s.player.character.credits=10000;s=grantCatalogAsset(s,{kind:'weapon',itemId:'blaster_rifle',method:'reward'}).state;s=grantCatalogAsset(s,{kind:'attachment',itemId:'rapid_recharge_xciter',method:'reward'}).state;s=grantCatalogAsset(s,{kind:'attachment',itemId:'spread_barrel',method:'reward'}).state;repo.save(s);g=new GameController(repo);return {g,repo}}

test('P99: catálogo econômico expõe somente attachments source-bound com preço/raridade/hard points',()=>{
 const c=genericEconomyCatalog(); assert.ok(c.attachments.some(x=>x.id==='rapid_recharge_xciter'));assert.ok(c.attachments.some(x=>x.id==='spread_barrel'));
 const r=c.attachments.find(x=>x.id==='rapid_recharge_xciter');assert.equal(r.price,750);assert.equal(r.rarity,7);
});

test('P99: compra aprovada de attachment desconta créditos e cria propriedade sem instalar magicamente',()=>{
 const {g}=game();g.state.player.character.credits=2000;
 const p=createPendingAcquisition('attachment','rapid_recharge_xciter',g.state.player.character,{requestId:'att_buy'});
 const r=resolvePendingAcquisition(g.state,p,{approved:true,finalPrice:750,sourceRuleIds:['GADGETS_AND_GEAR_P89']});
 assert.equal(r.state.player.character.credits,1250);const a=r.state.group.ownedAssets.find(x=>x.kind==='attachment');assert.ok(a);assert.notEqual(a.state?.installed,true);
});

test('P99: sourceCatalogId preserva hard points reais da arma adquirida',()=>{
 const {g}=ownedSetup();const w=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');assert.equal(itemHardPoints(w),4);assert.equal(itemAvailableHardPoints(w),4);
});

test('P99: Rapid-Recharge e Spread Barrel exigem propriedade, compatibilidade e consomem hard points',()=>{
 const {g}=ownedSetup();const sum0=g.attachmentSummary(),w=sum0.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');const rapid=sum0.owned.find(x=>x.catalogId==='rapid_recharge_xciter'),spread=sum0.owned.find(x=>x.catalogId==='spread_barrel');
 assert.throws(()=>g.installOwnedAttachment('asset_missing',w.id),/adquirido não encontrado/i);
 g.installOwnedAttachment(rapid.assetId,w.id);g.installOwnedAttachment(spread.assetId,w.id);
 const sum=g.attachmentSummary(),after=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');assert.equal(after.hardPoints,4);assert.equal(after.hardPointsAvailable,1);assert.equal(after.attachments.length,2);
});

test('P99: attachments alteram perfil de ataque real e remoção desfaz efeitos sem apagar propriedade',()=>{
 const {g}=ownedSetup();let sum=g.attachmentSummary(),w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');const rapid=sum.owned.find(x=>x.catalogId==='rapid_recharge_xciter'),spread=sum.owned.find(x=>x.catalogId==='spread_barrel');
 g.installOwnedAttachment(rapid.assetId,w.id);g.installOwnedAttachment(spread.assetId,w.id);
 const weapon=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');const eff=effectiveWeaponWithAttachments(weapon),mods=weaponAttackModifiers(eff,{useAutoFire:true,character:g.state.player.character});
 assert.equal(eff.range,'Médio');assert.equal(mods.autoFireAvailable,true);assert.equal(mods.inaccurate,3);assert.ok(activeWeaponQualities(eff,{hit:true,useAutoFire:false}).some(q=>q.key==='blast'&&q.rank===4));
 g.removeOwnedAttachment(rapid.assetId);const w2=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'),eff2=effectiveWeaponWithAttachments(w2),mods2=weaponAttackModifiers(eff2,{character:g.state.player.character});assert.equal(mods2.autoFireAvailable,false);assert.equal(mods2.inaccurate,0);assert.equal(g.attachmentSummary().owned.find(x=>x.assetId===rapid.assetId).state.installed,false);
});

test('P99: attachment incompatível e venda/perda enquanto instalado são bloqueados',()=>{
 let {g,repo}=game();let s=g.state;s=grantCatalogAsset(s,{kind:'weapon',itemId:'blaster_pistol',method:'reward'}).state;s=grantCatalogAsset(s,{kind:'attachment',itemId:'spread_barrel',method:'reward'}).state;repo.save(s);g=new GameController(repo);let sum=g.attachmentSummary(),w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_pistol'),spread=sum.owned[0];assert.throws(()=>g.installOwnedAttachment(spread.assetId,w.id),/não é compatível/i);
 const setup=ownedSetup();sum=setup.g.attachmentSummary();w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');const rapid=sum.owned.find(x=>x.catalogId==='rapid_recharge_xciter');setup.g.installOwnedAttachment(rapid.assetId,w.id);assert.throws(()=>loseOwnedAsset(setup.g.state,rapid.assetId,{reason:'perdido'}),/Remova o attachment/i);
});

test('P99: bridge/contratos/R46 expõem aquisição e instalação seguras, sem mod arbitrário',()=>{
 const bridge=fs.readFileSync('app/src/main/assets/vtt/js/core/uiBridge.mjs','utf8'),runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
 for(const m of ['attachmentSummary','proposeAttachmentAcquisition','installOwnedAttachment','removeOwnedAttachment'])assert.ok(bridge.includes(`${m}(`),m);
 for(const f of ['gameBridgeClient.ts','shell-r46-run87/gameBridgeClient.ts']){const t=fs.readFileSync(f,'utf8');for(const m of ['attachmentSummary','proposeAttachmentAcquisition','installOwnedAttachment','removeOwnedAttachment'])assert.ok(t.includes(m),`${f}:${m}`)}
 assert.ok(runtime.includes('ATTACHMENTS MECÂNICOS · SEM RAG'));assert.ok(runtime.includes('proposeAttachmentAcquisition'));assert.ok(runtime.includes('installOwnedAttachment'));assert.equal(runtime.includes('modifyAttachment'),false);
});

test('P99: attachments chegam ao resolveWeaponAttack real — Imprimisa no pool, Automática na dificuldade e Spread reduz alcance',()=>{
 const {g}=ownedSetup();let sum=g.attachmentSummary(),w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');const rapid=sum.owned.find(x=>x.catalogId==='rapid_recharge_xciter'),spread=sum.owned.find(x=>x.catalogId==='spread_barrel');
 g.installOwnedAttachment(rapid.assetId,w.id);g.installOwnedAttachment(spread.assetId,w.id);
 const wi=g.state.player.character.weapons.findIndex(x=>x.sourceCatalogId==='blaster_rifle');
 const r=g.resolveWeaponAttack({weaponIndex:wi,targetRange:'Médio',useAutoFire:true,rng:()=>0.3});
 assert.equal(r.difficulty,3);assert.ok(Number(r.check.pool.setback)>=3);assert.throws(()=>g.resolveWeaponAttack({weaponIndex:wi,targetRange:'Longo',rng:()=>0.3}),/alcance|range|fora/i);
});
