import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {askSWE02Piddock,showSWE02PiddockTradeInterest,hireSWE02Vio,bribeSWE02Vrixxtt,validateSWE02Act2Contracts} from '../app/src/main/assets/vtt/js/adventure/swe02Act2ContractsWip.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>a.advance(w,id).world;
const party=()=>['dock-ryloth','head-to-geonosis','attend-piddock-party'].reduce(step,a.start({}).world);
const gantry=()=>['piddock-to-dimmock','dimmock-finish'].reduce(step,party());
class Repo{constructor(){this.state=createCampaign('SWE02 contracts');this.saves=0;}load(){return structuredClone(this.state);}save(x){this.state=structuredClone(x);this.saves++;}}
function controller(w){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world:w});return {g,repo};}
function reopen(repo){for(let i=0;i<3;i++){const c=new GameController(repo);assert.equal(a.status(c.state.world).node.id,repo.state.world.publishedAdventure.currentNodeId);}}

test('Piddock p.33: quatro perguntas, aviso na quarta, encerramento na quinta, sem inventar respostas',()=>{
 let w=party();for(let i=0;i<5;i++){const r=askSWE02Piddock(w,{topic:`pergunta ${i+1}`});w=r.world;
  assert.equal(r.receipt.ordinal,i+1);assert.equal(r.receipt.warning,i===3);assert.equal(r.receipt.conversationClosed,i===4);}
 assert.throws(()=>askSWE02Piddock(w,{topic:'sexta'}),/encerrou/);
 assert.throws(()=>showSWE02PiddockTradeInterest(w),/terminou/);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(validateSWE02Act2Contracts(w),true);}
});
test('Piddock: manifestar comércio mantém diálogo aberto, mas não inventa armas',()=>{
 let w=party();for(let i=0;i<4;i++)w=askSWE02Piddock(w,{topic:`t${i}`}).world;
 w=showSWE02PiddockTradeInterest(w).world;
 for(let i=0;i<6;i++)w=askSWE02Piddock(w,{topic:`t${i+4}`}).world;
 assert.equal(w.publishedAdventure.piddockConversationClosed,false);
 assert.equal(validateSWE02Act2Contracts(w),true);
 const forged=structuredClone(w);forged.publishedAdventure.piddockQuestionReceipts[2].ordinal=30;
 assert.throws(()=>a.status(forged),/Piddock adulterado/);
});
test('Piddock choices oficializadas incrementam perguntas, sem bloquear escolhas já válidas',()=>{
 let w=party();w=step(w,'piddock-kubaz');
 assert.equal(w.publishedAdventure.piddockQuestionReceipts.length,1);
 assert.equal(w.publishedAdventure.piddockQuestionReceipts[0].topic,'piddock-kubaz');
 assert.equal(a.status(w).node.id,'piddock-party');
});
test('Vio p.33: contratar rota direta com débito exato, sem Dimmock/AA7 e sem trocar dono da nave',()=>{
 let w=party(),c={id:'buyer',credits:3500,characteristics:{presencia:3},skills:{negociacion:1}};
 assert.throws(()=>step(w,'piddock-hire-vios'),/inválida/);
 const out=hireSWE02Vio(w,c,{partySize:3,offer:'WEX'});w=out.world;
 assert.equal(out.receipt.creditsSpent,1500);assert.equal(out.character.credits,2000);
 assert.equal(out.receipt.shipOwnership,'ORPA_AND_WEX_VIO');assert.equal(validateSWE02Act2Contracts(w),true);
 assert.throws(()=>hireSWE02Vio(w,out.character,{partySize:3}),/duplicada/);
 w=step(w,'piddock-hire-vios');assert.equal(a.status(w).node.id,'lucky-guess-boarding');
 w=step(w,'lucky-guess-to-tatooine');assert.equal(a.status(w).node.id,'tatooine-infiltration');
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));a.status(w);}
});
test('Vio: negociação Média com dados reais, 250 por pessoa se passar e 500 se falhar',()=>{
 const c={id:'buyer',credits:4000,characteristics:{presencia:3},skills:{negociacion:1}};
 let rolls=0;const win=hireSWE02Vio(party(),c,{partySize:3,offer:'NEGOTIATE',rng:()=>rolls++<3?.36:0});
 assert.equal(win.receipt.perPerson,250);assert.equal(win.character.credits,3250);
 const lose=hireSWE02Vio(party(),c,{partySize:3,offer:'NEGOTIATE',rng:()=>0});
 assert.equal(lose.receipt.perPerson,500);assert.equal(lose.character.credits,2500);
 const forged=structuredClone(win.world);forged.publishedAdventure.vioHireReceipt.perPerson=1;
 assert.throws(()=>a.status(forged),/Vio adulterado/);
 assert.throws(()=>hireSWE02Vio(party(),{...c,credits:100},{partySize:3}),/insuficientes/);
});
test('Vio controller/bridge/UI: compra atômica, rollback, três reaberturas',()=>{
 const {g,repo}=controller(party());const before=repo.saves;
 assert.throws(()=>g.hireSWE02Vio({partySize:9}),/passageiros/);assert.equal(repo.saves,before);
 const credits=g.state.player.character.credits;
 // Starter Cael has few credits: fund explicitly as a test fixture, not a game grant.
 const funded=structuredClone(g.state.player.character);funded.credits=5000;g.commit('CHARACTER_UPDATED',{character:funded});
 const r=g.hireSWE02Vio({partySize:3,offer:'WEX'});assert.equal(r.creditsSpent,1500);
 assert.equal(g.state.player.character.credits,3500);assert.equal(g.state.world.publishedAdventure.flags.vioHired,true);
 g.advancePublishedAdventure('piddock-hire-vios');reopen(repo);
 const bridge=new BraseiroUiBridge({repository:repo});
 for(const method of ['askSWE02Piddock','showSWE02PiddockTradeInterest','hireSWE02Vio','bribeSWE02Vrixxtt'])assert.equal(typeof bridge[method],'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02Act2Contracts','swe02PiddockAsk','swe02VioHire','swe02Act2Gantry','swe02VrixxttBribe','swe02VrixxttTarget']){
  assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
function combatFixture(){const {g,repo}=controller(gantry());g.beginPublishedAdventureEncounter('laoth-gantry-aa7',{rng:()=>.9});
 const combat=structuredClone(g.state.combat);combat.encounter.waitingForPlayer=true;combat.encounter.activeActorId='player';combat.playerTurn.actionUsed=false;
 g.commit('COMBAT_ACTION',{combat});const ch=structuredClone(g.state.player.character);ch.credits=5000;g.commit('CHARACTER_UPDATED',{character:ch});return {g,repo};}
test('AA7 p.37: 500 créditos retira apenas Vrixx, sem neutralizar soldados nem fabricar vitória',()=>{
 const {g,repo}=combatFixture();const before=g.state.player.character.credits;
 const r=g.bribeSWE02Vrixxtt({credits:500});assert.equal(r.creditsSpent,500);
 assert.equal(g.state.player.character.credits,before-500);
 assert.equal(g.state.combat.adversaries.filter(a=>a.profileKey==='laoth_geonosian_soldier'&&a.status==='active').length,5);
 assert.equal(g.state.combat.adversaries.at(-1).status,'retreated');assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.throws(()=>g.bribeSWE02Vrixxtt({credits:500}),/ação de iniciativa/);
 assert.equal(g.state.world.publishedAdventure.flags.geoGantryCleared,undefined);
 reopen(repo);
});
test('AA7 p.37: 1000 créditos executa tiro de Gand com dados, dano e cobertura reais',()=>{
 const {g,repo}=combatFixture();const target=g.state.combat.adversaries[0];const before=target.wounds.current;
 let i=0;const r=g.bribeSWE02Vrixxtt({credits:1000,targetId:target.id,rng:()=>i++<4?.35:0});
 assert.equal(r.sniperSupport,true);assert.equal(r.shot.pool.setback,1);
 assert.equal(r.shot.check.success,true);assert.ok(r.shot.suffered>0);
 assert.ok(g.state.combat.adversaries[0].wounds.current>before);
 assert.equal(g.state.combat.adversaries.at(-1).status,'retreated');reopen(repo);
 const forged=structuredClone(g.state.world);forged.publishedAdventure.vrixxttBribeReceipt.shot.rawDamage=999;
 assert.throws(()=>a.status(forged),/tiro Vrixx’tt adulterado/);
});
test('AA7: pagamento falho preserva saldo, combate e saves; sem alvo real não há tiro',()=>{
 const {g,repo}=combatFixture();const before=repo.saves,credits=g.state.player.character.credits;
 assert.throws(()=>g.bribeSWE02Vrixxtt({credits:1000,targetId:'fake'}),/soldado real/);
 assert.equal(repo.saves,before);assert.equal(g.state.player.character.credits,credits);
 assert.equal(g.state.combat.adversaries.at(-1).status,'active');
 const wrong=structuredClone(g.state.world);wrong.publishedAdventure.flags.vioHired=true;
 assert.throws(()=>a.status(wrong),/Vio sem recibo/);
});
test('AA7: fechamento só aceita soldados realmente derrotados + Gand pago; embarque persiste',()=>{
 const {g,repo}=combatFixture();const receipt=g.bribeSWE02Vrixxtt({credits:500});
 let combat=structuredClone(g.state.combat);
 for(const a of combat.adversaries)if(a.profileKey==='laoth_geonosian_soldier'){
  a.wounds.current=a.wounds.threshold+1;a.status='incapacitated';
 }
 combat.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat});
 const out=g.finishCombat({rng:()=>.9});
 assert.equal(out.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(out.publishedAdventureOutcome.combatProof.bribedVrixxttId,receipt.vrixxttId);
 assert.equal(g.state.world.publishedAdventure.flags.geoGantryCleared,true);
 g.advancePublishedAdventure('board-lucky-guess');reopen(repo);
});
