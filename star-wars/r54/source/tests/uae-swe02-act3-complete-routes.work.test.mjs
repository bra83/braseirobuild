import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {act3VerifiedEvidence,prepareSWE02AnattaDossier,resolveSWE02TeemoWithoutCombat,claimSWE02TeemoStrongbox,validateSWE02Act3,swe02Act3Consequences} from '../app/src/main/assets/vtt/js/adventure/swe02Act3Resolution.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>a.advance(w,id).world;
const arrive=()=>['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return'].reduce(step,a.start({}).world);
const check=(w,index,success=true)=>a.adjudicateCheck(w,{checkIndex:index,result:{success,netSuccess:success?1:0,netAdvantage:0}}).world;
function evidenceWorld(){let w=step(arrive(),'approach-fortified-gate');w=check(w,0);w=step(w,'palace-gate-debt-bluff');w=step(w,'palace-enter-workshop');w=step(w,'palace-record-b1-worklog');w=step(w,'palace-worklog-to-comms');w=check(w,2);return w;}
function deathWorld(){let w=step(arrive(),'confront-teemo');w=structuredClone(w);
 w.publishedAdventure.flags.confirmedPartySize=3;w.publishedAdventure.flags.palaceAdditionalHenchmenCount=2;w.publishedAdventure.flags.gmHuttProfileConfirmed=true;
 w=a.markEncounterActive(w,'teemo-palace-guards');
 const enemies=a.encounterSpec(w,'teemo-palace-guards').adversaries.map((x,i)=>({id:`enemy-${i}`,profileKey:x.profileKey,status:'incapacitated',wounds:999}));
 return a.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated',adversaries:enemies}).world;
}
class Repo{constructor(){this.state=createCampaign('SWE02 act3');this.saves=0;}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
function controller(world){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.commit('WORLD_UPDATED',{world});return {g,repo};}
const reopen=(repo)=>{for(let i=0;i<3;i++){const r=new GameController(repo);assert.equal(a.status(r.state.world).node.id,repo.state.world.publishedAdventure.currentNodeId);} };
test('Ato3 dossiê exige duas provas distintas concretas, não meras flags',()=>{
 let w=arrive();assert.throws(()=>prepareSWE02AnattaDossier(w),/duas provas/);
 w=step(w,'approach-fortified-gate');w=check(w,0);w=step(w,'palace-gate-debt-bluff');w=step(w,'palace-enter-workshop');w=step(w,'palace-record-b1-worklog');
 assert.deepEqual(act3VerifiedEvidence(w.publishedAdventure),['B1_DROIDS']);assert.throws(()=>prepareSWE02AnattaDossier(w),/duas provas/);
 const forged=structuredClone(w);forged.publishedAdventure.flags.bithSpyEvidence=true;
 assert.deepEqual(act3VerifiedEvidence(forged.publishedAdventure),['B1_DROIDS']);
});
test('Ato3 Anatta: provas verificadas, recibo durável, sem créditos e sem morte',()=>{
 const w=evidenceWorld();assert.deepEqual(act3VerifiedEvidence(w.publishedAdventure),['B1_DROIDS','RYLL_WITHHELD']);
 const prepared=prepareSWE02AnattaDossier(w);assert.equal(prepared.receipt.type,'ANATTA_DOSSIER_PREPARED');
 assert.equal(validateSWE02Act3(prepared.world),true);
 assert.throws(()=>prepareSWE02AnattaDossier(prepared.world),/já registrado/);
 assert.equal(prepared.world.publishedAdventure.flags.teemoDefeated,undefined);
});
test('Ato3 chantagem só diante de Teemo, sem combate e com dossiê',()=>{
 let w=prepareSWE02AnattaDossier(evidenceWorld()).world;
 assert.throws(()=>resolveSWE02TeemoWithoutCombat(w,{method:'BLACKMAIL'}),/confronto/);
 w=step(w,'palace-comms-to-throne');const out=resolveSWE02TeemoWithoutCombat(w,{method:'BLACKMAIL'});
 assert.equal(out.resolution.teemoStatus,'ALIVE_DEAL');assert.equal(out.resolution.teemoKilled,false);
 assert.equal(swe02Act3Consequences(out.world.publishedAdventure).teemoAliveDeal,true);
 w=step(out.world,'teemo-peaceful-ending');assert.equal(w.publishedAdventure.status,'COMPLETED');
 assert.match(a.status(w).node.summary,/continua vivo/);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(a.status(w).node.id,'laoth-epilogue');}
});
test('Ato3 intervenção Jabba via Anatta encerra sem fabricar morte ou combate',()=>{
 let w=prepareSWE02AnattaDossier(evidenceWorld()).world;
 w=resolveSWE02TeemoWithoutCombat(w,{method:'JABBA'}).world;
 assert.equal(w.publishedAdventure.flags.jabbaInformed,true);
 assert.equal(swe02Act3Consequences(w.publishedAdventure).teemoRemovedByJabba,true);
 w=step(w,'palace-comms-peaceful-ending');assert.equal(w.publishedAdventure.status,'COMPLETED');
 assert.match(a.status(w).node.summary,/Anatta apresentou/);
});
test('Ato3 dossiê adulterado, falso fim e falso crédito são rejeitados',()=>{
 const w=prepareSWE02AnattaDossier(evidenceWorld()).world;
 const forged=structuredClone(w);forged.publishedAdventure.act3Dossier.evidence=['B1_DROIDS','JABBA_SPY'];
 assert.throws(()=>a.status(forged),/dossiê sem provas/);
 const forgedEnding=structuredClone(w);forgedEnding.publishedAdventure.flags.teemoPeacefullyNeutralized=true;
 assert.throws(()=>a.status(forgedEnding),/estado sem recibo|neutralização/);
 const death=deathWorld();const f=structuredClone(death);f.publishedAdventure.act3Strongbox={credits:10000,recipientKey:'cael',creditsAfter:10000};
 assert.throws(()=>a.status(f),/estado sem recibo/);
});
test('Ato3 cofre oficial: 10000 créditos somente depois de vitória real, sem duplicação',()=>{
 const death=deathWorld(),character={id:'cael',credits:150};
 const out=claimSWE02TeemoStrongbox(death,character);assert.equal(out.character.credits,10150);
 assert.equal(out.receipt.credits,10000);assert.equal(validateSWE02Act3(out.world),true);
 assert.throws(()=>claimSWE02TeemoStrongbox(out.world,out.character),/já recolhido/);
 const fake=structuredClone(out.world);fake.publishedAdventure.encounterHistory=[];
 assert.throws(()=>validateSWE02Act3(fake),/sem vitória/);
});
test('Ato3 controller/bridge/UI: persistência e três reaberturas; rollback em falha',()=>{
 const {g,repo}=controller(evidenceWorld());const receipt=g.prepareSWE02AnattaDossier();assert.equal(receipt.evidence.length,2);
 const before=repo.saves;assert.throws(()=>g.resolveSWE02TeemoWithoutCombat({method:'BLACKMAIL'}),/confronto/);assert.equal(repo.saves,before);
 const outcome=g.resolveSWE02TeemoWithoutCombat({method:'JABBA'});assert.equal(outcome.teemoKilled,false);
 g.advancePublishedAdventure('palace-comms-peaceful-ending');reopen(repo);
 const b=new BraseiroUiBridge({repository:repo});for(const name of ['prepareSWE02AnattaDossier','resolveSWE02TeemoWithoutCombat','claimSWE02TeemoStrongbox'])assert.equal(typeof b[name],'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02Act3Actions','swe02Act3Dossier','swe02Act3Resolve','swe02Act3Strongbox','swe02Act3Feedback']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
test('Ato3 controlador: cofre realmente credita carteira e persiste três vezes',()=>{
 const {g,repo}=controller(deathWorld());const before=g.state.player.character.credits;
 const r=g.claimSWE02TeemoStrongbox();assert.equal(r.credits,10000);assert.equal(g.state.player.character.credits,before+10000);
 assert.throws(()=>g.claimSWE02TeemoStrongbox(),/já recolhido/);reopen(repo);
});
function chandelierFixture(){
 let w=step(arrive(),'confront-teemo');w=check(w,0,true);
 const {g,repo}=controller(w);
 g.beginPublishedAdventureEncounter('teemo-palace-guards',{rng:()=>.9,palaceAdditionalHenchmenCount:2,gmHuttProfileConfirmed:true});
 const combat=structuredClone(g.state.combat);combat.encounter.waitingForPlayer=true;combat.encounter.activeActorId='player';combat.playerTurn.actionUsed=false;
 g.commit('COMBAT_ACTION',{combat});return {g,repo};
}
test('Ato3 lustre exige combate, percepção e blaster; falha consome ação sem vitória',()=>{
 const {g,repo}=chandelierFixture();const before=repo.saves;
 assert.throws(()=>g.shootSWE02TeemoChandelier({weaponIndex:1,rng:()=>.4}),/blaster/);assert.equal(repo.saves,before);
 const r=g.shootSWE02TeemoChandelier({weaponIndex:0,rng:()=>0});assert.equal(r.success,false);
 assert.equal(g.state.combat.playerTurn.actionUsed,true);assert.equal(g.state.world.publishedAdventure.flags.teemoDefeated,undefined);
 assert.ok(g.state.combat.adversaries.some(x=>x.profileKey==='teemo_the_hutt'&&x.status==='active'));
 assert.throws(()=>g.shootSWE02TeemoChandelier({weaponIndex:0,rng:()=>.4}),/ação real/);
 reopen(repo);
});
test('Ato3 lustre acerta por dados reais: Hutt cai, minions recuam, combate fecha, cofre credita',()=>{
 const {g,repo}=chandelierFixture();const char=g.state.player.character;
 const skill=char.skills.armas_distancia_pesadas||0;const positive=Math.max(char.characteristics.agilidad,skill);
 let i=0;const rng=()=>i++<positive?.35:0;
 const shot=g.shootSWE02TeemoChandelier({weaponIndex:0,rng});
 assert.equal(shot.success,true);assert.equal(shot.pool.challenge,2);assert.equal(shot.pool.difficulty,0);
 assert.ok(g.state.combat.adversaries.some(x=>x.profileKey==='teemo_the_hutt'&&x.status==='incapacitated'));
 assert.ok(g.state.combat.adversaries.filter(x=>x.profileKey!=='teemo_the_hutt').every(x=>x.status==='retreated'));
 assert.equal(g.state.world.publishedAdventure.flags.teemoDefeated,undefined);
 const end=g.finishCombat({rng:()=>.9});assert.equal(end.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(g.state.world.publishedAdventure.flags.teemoDefeated,true);
 const credits=g.state.player.character.credits;g.claimSWE02TeemoStrongbox();assert.equal(g.state.player.character.credits,credits+10000);
 g.advancePublishedAdventure('teemo-resolved');assert.equal(g.state.world.publishedAdventure.status,'COMPLETED');
 reopen(repo);
});
test('Ato3 não aceita recibo adulterado de tiro ou sucesso sem teste de Percepção',()=>{
 const {g}=chandelierFixture();const char=g.state.player.character;const positive=Math.max(char.characteristics.agilidad,char.skills.armas_distancia_pesadas||0);
 let i=0;g.shootSWE02TeemoChandelier({weaponIndex:0,rng:()=>i++<positive?.35:0});
 let w=structuredClone(g.state.world);w.publishedAdventure.act3ChandelierReceipts[0].check.netSuccess=100;
 assert.throws(()=>a.status(w),/dados adulterados/);
 w=structuredClone(g.state.world);w.publishedAdventure.checkHistory=[];
 assert.throws(()=>a.status(w),/lustre sem Percepção/);
});
import {ANATTA_CATALOG,buySWE02AnattaInformation,validateSWE02Anatta} from '../app/src/main/assets/vtt/js/adventure/swe02Act2Anatta.mjs';
test('Ato2 Anatta: 11 assuntos e preços oficiais, sem barganha nem compras duplicadas',()=>{
 assert.equal(Object.keys(ANATTA_CATALOG).length,11);
 assert.equal(ANATTA_CATALOG.sivorMurder.credits,100);assert.equal(ANATTA_CATALOG.kubazSpy.credits,40);
 let w=arrive();
 assert.throws(()=>buySWE02AnattaInformation(w,{id:'cael',credits:100},{topicId:'kubazSpy'}),/recepção/);
 w=['dock-ryloth','head-to-geonosis','attend-piddock-party'].reduce(step,a.start({}).world);
 assert.equal(a.status(w).node.id,'piddock-party');
 const before={id:'cael',credits:600};const purchased=buySWE02AnattaInformation(w,before,{topicId:'kubazSpy'});
 assert.equal(purchased.character.credits,560);assert.equal(purchased.receipt.creditsSpent,40);
 assert.throws(()=>buySWE02AnattaInformation(purchased.world,purchased.character,{topicId:'kubazSpy'}),/já foi comprada/);
 const next=buySWE02AnattaInformation(purchased.world,purchased.character,{topicId:'jabbaRelationship'});
 assert.equal(next.character.credits,500);assert.equal(next.world.publishedAdventure.flags.anattaJabbaAngleKnown,true);
 assert.equal(validateSWE02Anatta(next.world),true);
 const forged=structuredClone(next.world);forged.publishedAdventure.anattaTradeReceipts[0].creditsSpent=0;
 assert.throws(()=>a.status(forged),/Anatta adulterado/);
});
