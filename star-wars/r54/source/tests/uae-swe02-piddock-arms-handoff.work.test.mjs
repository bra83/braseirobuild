import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {PIDDOCK_WEAPONS,agreeSWE02PiddockArms,deliverSWE02PiddockArms,showSWE02PiddockTradeInterest,validateSWE02Act2Contracts} from '../app/src/main/assets/vtt/js/adventure/swe02Act2ContractsWip.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>a.advance(w,id).world;
const party=()=>['dock-ryloth','head-to-geonosis','attend-piddock-party'].reduce(step,a.start({}).world);
const buyer={id:'buyer',name:'Cael',credits:20000,weapons:[],characteristics:{presencia:4},skills:{negociacion:2}};
class Repo{constructor(){this.s=createCampaign('Piddock');this.saves=0;}load(){return structuredClone(this.s)}save(s){this.s=structuredClone(s);this.saves++}}
function controller(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world:party()});const c=structuredClone(g.state.player.character);c.credits=20000;g.commit('CHARACTER_UPDATED',{character:c});return {g,repo};}
test('p.33: três armas oficiais, preços fixos e críticos reduzidos, sem entregar na festa',()=>{
 assert.deepEqual(PIDDOCK_WEAPONS.map(x=>x.price),[1100,1300,1500]);
 assert.deepEqual(PIDDOCK_WEAPONS.map(x=>x.crit),[2,2,2]);
 let w=showSWE02PiddockTradeInterest(party()).world;
 for(const item of PIDDOCK_WEAPONS){const out=agreeSWE02PiddockArms(w,buyer,{weaponId:item.id,quantity:2});w=out.world;
  assert.equal(out.receipt.creditsDue,item.price*2);assert.equal(out.receipt.paidAtParty,false);assert.equal(out.receipt.weaponsDeliveredAtParty,false);
  assert.equal(validateSWE02Act2Contracts(w),true);assert.equal(a.status(w).node.id,'piddock-party');}
 assert.equal(buyer.credits,20000);assert.equal(buyer.weapons.length,0);
 assert.throws(()=>agreeSWE02PiddockArms(w,buyer,{weaponId:'geo-rifle'}),/já registrado/);
 assert.throws(()=>agreeSWE02PiddockArms(w,buyer,{weaponId:'unknown'}),/inválida/);
 w=step(w,'piddock-agent-visit');assert.equal(a.status(w).node.id,'piddock-freighter-handoff');
 let c=structuredClone(buyer);
 for(const item of PIDDOCK_WEAPONS){const out=deliverSWE02PiddockArms(w,c,{weaponId:item.id});w=out.world;c=out.character;
  assert.equal(out.receipt.creditsSpent,item.price*2);assert.equal(validateSWE02Act2Contracts(w),true);
  assert.throws(()=>deliverSWE02PiddockArms(w,c,{weaponId:item.id}),/duplicada/);}
 assert.equal(c.credits,20000-2*(1100+1300+1500));assert.equal(c.weapons.length,6);
 assert.deepEqual(c.weapons.map(x=>x.damage),[7,7,9,9,9,9]);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(a.status(w).node.id,'piddock-freighter-handoff');}
 const tampered=structuredClone(w);tampered.publishedAdventure.piddockArmsDeliveries[0].creditsSpent=1;
 assert.throws(()=>a.status(tampered),/entrega Piddock adulterada/);
});
test('p.33: rolagem oposta com desafio real concede exatamente 10% somente se houver sucesso',()=>{
 const w=showSWE02PiddockTradeInterest(party()).world;
 let rolls=0;const win=agreeSWE02PiddockArms(w,buyer,{weaponId:'geo-rifle',haggle:true,rng:()=>rolls++<4?.36:0});
 assert.equal(win.receipt.haggleCheck.pool.challenge,1);assert.equal(win.receipt.haggleCheck.pool.difficulty,1);
 assert.equal(win.receipt.discountPercent,10);assert.equal(win.receipt.creditsDue,1350);
 const lose=agreeSWE02PiddockArms(w,buyer,{weaponId:'geo-rifle',haggle:true,rng:()=>0});
 assert.equal(lose.receipt.discountPercent,0);assert.equal(lose.receipt.creditsDue,1500);
 const forged=structuredClone(win.world);forged.publishedAdventure.piddockArmsAgreements[0].discountPercent=80;
 assert.throws(()=>validateSWE02Act2Contracts(forged),/contrato Piddock adulterado/);
});
test('Controller + UI + Bridge: rollback insuficiência, recibos reais, três reaberturas',()=>{
 const {g,repo}=controller();const pre=repo.saves;
 assert.throws(()=>g.agreeSWE02PiddockArms({weaponId:'geo-rifle'}),/conversa comercial/);
 assert.equal(repo.saves,pre);
 g.showSWE02PiddockTradeInterest();const agreement=g.agreeSWE02PiddockArms({weaponId:'geo-rifle',quantity:2});
 assert.equal(agreement.creditsDue,3000);assert.equal(g.state.player.character.credits,20000);
 assert.equal(g.swe02PiddockArmsCatalog().length,3);
 g.advancePublishedAdventure('piddock-agent-visit');
 const poor=structuredClone(g.state.player.character);poor.credits=100;g.commit('CHARACTER_UPDATED',{character:poor});
 const before=repo.saves;
 assert.throws(()=>g.deliverSWE02PiddockArms({weaponId:'geo-rifle'}),/insuficientes/);assert.equal(repo.saves,before);
 const rich=structuredClone(g.state.player.character);rich.credits=20000;g.commit('CHARACTER_UPDATED',{character:rich});
 const out=g.deliverSWE02PiddockArms({weaponId:'geo-rifle'});assert.equal(out.creditsSpent,3000);
 assert.equal(g.state.player.character.credits,17000);
 assert.equal(g.state.player.character.weapons.filter(x=>x.name==='Fuzil blaster geonosiano').length,2);
 for(let i=0;i<3;i++){const re=new GameController(repo);assert.equal(validateSWE02Act2Contracts(re.state.world),true);assert.equal(re.state.player.character.credits,17000);}
 const bridge=new BraseiroUiBridge({repository:repo});for(const name of ['swe02PiddockArmsCatalog','agreeSWE02PiddockArms','deliverSWE02PiddockArms'])assert.equal(typeof bridge[name],'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02PiddockWeapon','swe02PiddockAgree','swe02PiddockDeliver','swe02PiddockHaggle']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
