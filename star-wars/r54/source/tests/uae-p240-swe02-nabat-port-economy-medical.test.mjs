import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {nabatMarketCatalog} from '../app/src/main/assets/vtt/js/adventure/swe02NabatP240.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import fs from 'node:fs';
class Repo{constructor(){this.state=createCampaign('P240');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
function setup({land=true}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:2,rng:()=>.9});
 g.addVehicle('yt1300',{name:'Krayt Fang'});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});
 g.advancePublishedAdventure('inspect-krayt-fang');g.resolvePublishedAdventureCheck(3,{rng:()=>.2});g.resolvePublishedAdventureCheck(5,{rng:()=>.2});
 g.resolvePublishedAdventureCheck(0,{rng:()=>.2});g.advancePublishedAdventure('ship-find-bura');g.advancePublishedAdventure('ship-depart-verified');
 if(land)g.advancePublishedAdventure('ship-safe-landing');
 return {repo,g};
}
function reopen(repo){for(let i=0;i<3;i++){const h=new GameController(repo);assert.ok(adapter.status(h.state.world).active);}}
test('P240 p.13: market uses only primary EotE Core entries priced <= 500',()=>{
 const c=nabatMarketCatalog();assert.ok(c.length>8);assert.ok(c.some(x=>x.id==='glow_rod'));
 assert.ok(c.every(x=>x.price>0&&x.price<=500&&x.source.activeRulesAuthority===true));
 assert.ok(!c.some(x=>x.id==='electronic_lockpick'||x.id==='blaster_rifle'));
});
test('P240 p.13: real purchase debits wallet and adds catalog gear atomically',()=>{
 const {g,repo}=setup();const before=g.state.player.character.credits;
 const r=g.purchaseSWE02NabatItem({itemId:'glow_rod',quantity:2});
 assert.equal(r.receipt.cost,20);assert.equal(g.state.player.character.credits,before-20);
 assert.equal(g.state.player.character.gear.at(-1).quantity,2);
 assert.equal(g.state.player.character.gear.at(-1).sourceCatalogId,'glow_rod');
 assert.equal(g.state.world.publishedAdventure.nabatReceipts.at(-1).source.printedPages[0],13);reopen(repo);
});
test('P240 market rejects forged price, non-EotE source, 501+ item and bad quantities',()=>{
 const {g,repo}=setup();const n=repo.saves;
 for(const options of [{itemId:'electronic_lockpick'},{itemId:'blaster_rifle'},{itemId:'glow_rod',quantity:0},
  {itemId:'glow_rod',quantity:1.5},{itemId:'glow_rod',quantity:11},{itemId:'unknown'}])
  assert.throws(()=>g.purchaseSWE02NabatItem(options),/indisponível|quantidade/);
 assert.equal(repo.saves,n);
});
test('P240 purchase insufficient credits: rollback of character, receipts and save',()=>{
 const {g,repo}=setup();const before=structuredClone(g.state),n=repo.saves;
 assert.throws(()=>g.purchaseSWE02NabatItem({itemId:'multi_scanner'}),/insuficientes/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,n);
});
test('P240 p.13: Bura contact pays actual hull repairs, never creates a new ship',()=>{
 const {g,repo}=setup();const v=g.state.vehicles;v.vehicles[v.activeVehicleId].hullDamage=7;
 const before=g.state.player.character.credits;
 const r=g.repairSWE02KraytFangAtNabat();assert.equal(r.hullBefore,7);assert.equal(r.hullAfter,0);
 assert.equal(g.activeVehicle().hullDamage,0);assert.equal(g.state.player.character.credits,before);
 assert.equal(Object.keys(g.state.vehicles.vehicles).length,1);
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/já pago/);reopen(repo);
});
test('P240 p.13: no ship repair if Bura not found, or ship name forged',()=>{
 const {g,repo}=setup();const p=g.state.world.publishedAdventure;
 p.flags.shipBuraFound=false;const n=repo.saves;
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/B’ura/);assert.equal(repo.saves,n);
 g.state.world.publishedAdventure.flags.shipBuraFound=true;g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId].name='Other Ship';
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/Krayt Fang/);assert.equal(repo.saves,n);
});
test('P240 p.13–14: port watch must be requested, alert only after Nyn meeting',()=>{
 const {g,repo}=setup();assert.throws(()=>g.receiveSWE02SpaceportAlert(),/fora da cena/);
 g.advancePublishedAdventure('nabat-meet-nyn');assert.throws(()=>g.receiveSWE02SpaceportAlert(),/nenhum pedido/);
 const r=g.requestSWE02SpaceportWatch();assert.equal(r.type,'SPACEPORT_WATCH_REQUESTED');
});
test('P240 p.13–14: watch at arrival, alert in Nyn safehouse, no automatic knowledge',()=>{
 const {g,repo}=setup();g.requestSWE02SpaceportWatch();assert.throws(()=>g.requestSWE02SpaceportWatch(),/já solicitada/);
 assert.equal(g.state.world.publishedAdventure.flags.nabatSpaceportAlertDelivered,undefined);
 g.advancePublishedAdventure('nabat-meet-nyn');const r=g.receiveSWE02SpaceportAlert();
 assert.deepEqual(r.visitors,['human','gand']);assert.equal(r.arrivalLeadHours,2);
 assert.throws(()=>g.receiveSWE02SpaceportAlert(),/já entregue/);reopen(repo);
});
test('P240 p.13: Nyn shelter heals real wounds and strain, never erases critical injuries',()=>{
 const {g,repo}=setup();g.advancePublishedAdventure('nabat-meet-nyn');
 g.state.player.character.derived.wounds.current=5;g.state.player.character.derived.strain.current=3;
 g.state.player.character.criticalInjuries=[{id:'critical_test',code:'stunned',active:true}];
 const r=g.treatSWE02NabatWounds();assert.equal(r.woundsHealed,5);assert.equal(r.strainRecovered,3);
 assert.equal(g.state.player.character.derived.wounds.current,0);assert.equal(g.state.player.character.derived.strain.current,0);
 assert.equal(g.state.player.character.criticalInjuries.length,1);assert.equal(r.criticalTreatmentPending,true);
 assert.throws(()=>g.treatSWE02NabatWounds(),/já realizado/);reopen(repo);
});
test('P240 anti-forgery: missing or modified receipt fails adapter status',()=>{
 const {g}=setup();g.purchaseSWE02NabatItem({itemId:'glow_rod'});
 const w=structuredClone(g.state.world);w.publishedAdventure.nabatReceipts[0].unitPrice=1;
 assert.throws(()=>adapter.status(w),/preço ou compra adulterados/);
 const x=structuredClone(g.state.world);x.publishedAdventure.flags.nabatHullRepaired=true;
 assert.throws(()=>adapter.status(x),/sem recibo/);
});
test('P240 no mutation outside Nabat; failed actions do not save',()=>{
 const {g,repo}=setup({land:false});const n=repo.saves;
 assert.throws(()=>g.purchaseSWE02NabatItem({itemId:'glow_rod'}),/fora da cena/);
 assert.throws(()=>g.requestSWE02SpaceportWatch(),/fora da cena/);
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/fora da cena/);
 assert.equal(repo.saves,n);
});
test('P240 Android bridge and UI source expose port operations',()=>{
 const b=new BraseiroUiBridge({repository:new Repo()});
 for(const method of ['swe02NabatMarket','purchaseSWE02NabatItem','requestSWE02SpaceportWatch','receiveSWE02SpaceportAlert','repairSWE02KraytFangAtNabat','treatSWE02NabatWounds'])
  assert.equal(typeof b[method],'function',method);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02NabatWatch','swe02NabatAlert','swe02NabatRepair','swe02NabatCare','swe02NabatBuy'])
  {assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
