import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {nabatMarketCatalog} from '../app/src/main/assets/vtt/js/adventure/swe02NabatP240.mjs';
import {applyVehicleCritical} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
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
test('P240 p.13: Nabat operations require source-backed safe docking arranged by B’ura',()=>{
 const {g,repo}=setup({land:false}),p=g.state.world.publishedAdventure,saves=repo.saves;
 p.currentNodeId='nabat-arrival';p.visitedNodeIds.push('nabat-arrival');
 assert.throws(()=>adapter.status(g.state.world),/sem recibo de atracamento seguro/);
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/atracamento seguro/);assert.equal(repo.saves,saves);
});
test('SWE02 p.13 arrival narration stays source-backed and does not promise fuel',()=>{
 const {g}=setup();
 const scene=adapter.currentScene(g.state.world);
 assert.match(scene.narration,/plataformas e a cantina/i);
 assert.match(scene.narration,/clima de Ryloth/);
 assert.match(scene.narration,/lyleks errantes/);
 assert.match(scene.narration,/reparos pagos por seus contatos/);
 assert.match(scene.narration,/retirar o transponder.*em solo/);
 assert.doesNotMatch(scene.narration,/reabastec|abastecimento/i);
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
test('P240 p.13: Bura contacts repair the real ship hull, systems and repairable criticals without creating a ship',()=>{
 const {g,repo}=setup();const v=g.state.vehicles,ship=v.vehicles[v.activeVehicleId];ship.hullDamage=7;ship.systemStrain=4;
 const damaged=applyVehicleCritical(ship,{roll:95}).vehicle;damaged.disabledComponents=['hyperdrive'];v.vehicles[v.activeVehicleId]=damaged;
 const before=g.state.player.character.credits;
 const r=g.repairSWE02KraytFangAtNabat();assert.equal(r.hullBefore,7);assert.equal(r.hullAfter,0);
 assert.equal(r.systemStrainBefore,4);assert.equal(r.systemStrainAfter,0);assert.equal(r.criticalHitsRepaired[0].key,'engine_damaged');
 assert.equal(g.activeVehicle().hullDamage,0);assert.equal(g.activeVehicle().systemStrain,0);
 assert.equal(g.activeVehicle().criticalHits[0].repaired,true);assert.deepEqual(g.activeVehicle().disabledComponents,[]);
 assert.equal(g.state.player.character.credits,before);assert.equal(r.creditsSpent,0);
 assert.equal(Object.keys(g.state.vehicles.vehicles).length,1);
 const forged=structuredClone(g.state.world);forged.publishedAdventure.nabatReceipts.at(-1).systemStrainAfter=1;
 assert.throws(()=>adapter.status(forged),/reparos dos contatos inválido/);
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/já foram pagos/);reopen(repo);
});
test('P240 p.13: contacts cannot claim to repair a destroyed ship; failure rolls back all state',()=>{
 const {g,repo}=setup();const v=g.state.vehicles,ship=v.vehicles[v.activeVehicleId];ship.hullDamage=3;ship.systemStrain=2;
 v.vehicles[v.activeVehicleId]=applyVehicleCritical(ship,{roll:100,modifier:54}).vehicle;const before=structuredClone(g.state),saves=repo.saves;
 assert.throws(()=>g.repairSWE02KraytFangAtNabat(),/nave destruída/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
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
 g.advancePublishedAdventure('nabat-meet-nyn');assert.throws(()=>g.receiveSWE02SpaceportAlert(),/depois da conversa/);
 g.negotiateSWE02Nyn({method:'coerce',rng:()=>0});const r=g.receiveSWE02SpaceportAlert();
 assert.deepEqual(r.visitors,['human','gand']);assert.equal(r.arrivalLeadHours,2);
 assert.throws(()=>g.receiveSWE02SpaceportAlert(),/já entregue/);reopen(repo);
});
test('P240 p.14: overnight choice closes Nyn scene, delivers requested alert and records the morning briefing',()=>{
 const {g,repo}=setup();g.requestSWE02SpaceportWatch();g.advancePublishedAdventure('nabat-meet-nyn');
 g.advancePublishedAdventure('travel-lylek-den');const p=g.state.world.publishedAdventure;
 assert.equal(p.currentNodeId,'nabat-morning-departure');assert.equal(p.flags.nabatNynConversationClosed,true);
 assert.equal(p.flags.nabatSpaceportAlertDelivered,true);
 assert.deepEqual(p.nabatReceipts.find(x=>x.type==='TATOOINE_VISITORS_ALERT').visitors,['human','gand']);
 const briefing=p.nabatReceipts.find(x=>x.type==='NABAT_MORNING_BRIEFING');
 assert.equal(briefing.serviceTunnel,true);assert.equal(briefing.speederProgrammedForRyllMines,true);
 assert.equal(briefing.buraTravelsWithParty,true);assert.equal(briefing.visitorReport,'HUMAN_AND_GAND_REPORTED');
 assert.throws(()=>g.advancePublishedAdventure('nabat-depart-for-mines'),/Escolha inválida/);reopen(repo);
});
test('P240 p.14: morning reply is typed, one-time, and required before mine departure',()=>{
 const {g,repo}=setup();g.advancePublishedAdventure('nabat-meet-nyn');g.advancePublishedAdventure('travel-lylek-den');
 assert.throws(()=>g.respondSWE02NabatMorning({response:'promise_everything'}),/inválida/);
 const r=g.respondSWE02NabatMorning({response:'same_hutt'});assert.equal(r.response,'same_hutt');
 assert.throws(()=>g.respondSWE02NabatMorning({response:'uncertain'}),/já registrada/);
 g.advancePublishedAdventure('nabat-depart-for-mines');const p=g.state.world.publishedAdventure;
 assert.equal(p.currentNodeId,'lylek-den-ambush');assert.ok(p.nabatReceipts.some(x=>x.type==='NABAT_MORNING_DEPARTURE'));
 assert.throws(()=>adapter.status({...g.state.world,publishedAdventure:{...p,nabatReceipts:p.nabatReceipts.filter(x=>x.type!=='NABAT_MORNING_RESPONSE')}}),/saída matinal inválida/);
 reopen(repo);
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
 const w=structuredClone(g.state.world),purchase=w.publishedAdventure.nabatReceipts.find(x=>x.type==='MARKET_PURCHASE');purchase.unitPrice=1;
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
