import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {getHealthStatus} from '../app/src/main/assets/vtt/js/rules/health.mjs';

const POSITION='ace:gunner:r1c2';
const DURABLE_POSITION='ace:gunner:r1c1';
const TOUGHENED_POSITION='ace:gunner:r2c1';
const ENDURING_POSITION='ace:gunner:r3c2';
const clone=structuredClone;
const characterOptions={name:'Nira Valen',speciesKey:'human',careerKey:'ace',specializationKey:'gunner',partySize:1,
 careerFreeSkills:['astronavegacion','frialdad','mecanica','percepcion'],
 specializationFreeSkills:['disciplina','armas_distancia_pesadas'],speciesChoiceSkills:['atletismo','medicina'],
 motivation:'Libertar mundos ocupados.',dutyType:'Superioridade espacial'};
class Repo{
 constructor(){this.state=createCampaign('AoR p.68 R1C2');this.saves=0;}
 load(){return clone(this.state);}
 save(value){this.state=clone(value);this.saves++;}
}

test('AoR p.68 Ace/Gunner R1C2 Grit buys exact node, adds strain threshold and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter(characterOptions);
 const before=clone(setup.state.player.character),candidate=setup.eligibleTalents().find(n=>n.id===POSITION);
 assert.ok(candidate);assert.equal(candidate.originalName,'APLOMO');assert.equal(candidate.name,'Compostura');
 assert.equal(candidate.cost,5);assert.equal(candidate.source.printedPage,68);
 assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'APLOMO');assert.equal(treeNode.source.printedPage,68);

 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);

 const bridge=new BraseiroUiBridge({repository:repo}),result=bridge.buyTalent(POSITION);
 assert.equal(result.ok,true);assert.equal(result.data.talent.id,POSITION);
 assert.deepEqual(result.data.effect,{applied:true,label:'+1 no limiar de Tensão'});
 const purchase=result.data.character.talentPurchases.find(p=>p.nodeId===POSITION);
 assert.equal(purchase.originalName,'APLOMO');assert.equal(purchase.source.printedPage,68);
 assert.equal(purchase.specializationKey,'gunner');
 assert.equal(result.data.character.derived.strain.threshold,before.derived.strain.threshold+1);
 assert.equal(result.data.character.xp.available,before.xp.available-5);

 const postPurchase=clone(result.data.character),priorThreshold=before.derived.strain.threshold;
 postPurchase.derived.strain.current=priorThreshold+1;
 assert.equal(getHealthStatus(postPurchase).strainIncapacitated,false,'Grit raises the live incapacitation boundary by one strain');
 postPurchase.derived.strain.current=priorThreshold+2;
 assert.equal(getHealthStatus(postPurchase).strainIncapacitated,true);

 const saved=repo.saves;
 for(let reopen=1;reopen<=3;reopen++){
  const game=new GameController(repo),character=game.state.player.character;
  const owned=character.talentPurchases.find(p=>p.nodeId===POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
  assert.equal(character.derived.strain.threshold,before.derived.strain.threshold+1);
  assert.equal(game.eligibleTalents().find(n=>n.id===POSITION).purchased,true);
 }
 assert.equal(repo.saves,saved,'reopen is read-only');
 assert.equal(bridge.buyTalent(POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R1C1 Durable buys exact node, reduces critical result and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Tarin Voss'});
 const before=clone(setup.state.player.character),candidate=setup.eligibleTalents().find(n=>n.id===DURABLE_POSITION);
 assert.ok(candidate);assert.equal(candidate.originalName,'ROBUSTO');assert.equal(candidate.name,'Robusto');
 assert.equal(candidate.cost,5);assert.equal(candidate.source.printedPage,68);
 assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===DURABLE_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'ROBUSTO');assert.equal(treeNode.source.printedPage,68);

 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);

 const bridge=new BraseiroUiBridge({repository:repo}),purchaseResult=bridge.buyTalent(DURABLE_POSITION);
 assert.equal(purchaseResult.ok,true);assert.equal(purchaseResult.data.talent.id,DURABLE_POSITION);
 assert.deepEqual(purchaseResult.data.effect,{applied:false,label:null});
 const receipt=purchaseResult.data.character.talentPurchases.find(p=>p.nodeId===DURABLE_POSITION);
 assert.equal(receipt.originalName,'ROBUSTO');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,5);
 assert.equal(purchaseResult.data.character.xp.available,before.xp.available-5);

 const savesBeforeCritical=repo.saves,live=new GameController(repo),critical=live.applyPlayerCritical('prova R1C1',{roll:50});
 assert.equal(critical.total,40);assert.equal(critical.injury.durableModifier,-10);
 assert.equal(repo.saves,savesBeforeCritical+1,'controller critical application commits the persisted state');
 const minimum=new GameController(new RepoWithState(repo.state)).applyPlayerCritical('piso R1C1',{roll:1});
 assert.equal(minimum.total,1,'Durable cannot reduce a critical result below 1');
 for(let reopen=1;reopen<=3;reopen++){
  const reopenedRepo=new RepoWithState(repo.state),game=new GameController(reopenedRepo),character=game.state.player.character;
  const owned=character.talentPurchases.find(p=>p.nodeId===DURABLE_POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
  assert.equal(owned.specializationKey,'gunner');
  const reopenedCritical=game.applyPlayerCritical(`reopen ${reopen}`,{roll:50});
  assert.equal(reopenedCritical.injury.durableModifier,-10);
  assert.equal(reopenedCritical.total,50,'one persisted prior injury adds 10 and Durable removes 10');
 }
 assert.equal(bridge.buyTalent(DURABLE_POSITION).ok,false,'the exact node cannot be purchased twice');
});

class RepoWithState extends Repo{
 constructor(state){super();this.state=clone(state);}
}

test('AoR p.68 Ace/Gunner R2C1 Toughened buys exact node, raises wound boundary and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Kessa Dorn'});
 const before=clone(setup.state.player.character);
 const bridge=new BraseiroUiBridge({repository:repo});
 const prerequisite=bridge.buyTalent(DURABLE_POSITION);
 assert.equal(prerequisite.ok,true,'the exact adjacent R1C1 prerequisite is purchased first');
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===TOUGHENED_POSITION);
 assert.ok(candidate);assert.equal(candidate.originalName,'ENDURECIDO');assert.equal(candidate.name,'Endurecido');
 assert.equal(candidate.available,true);assert.equal(candidate.cost,10);assert.equal(candidate.source.printedPage,68);
 assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===TOUGHENED_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'ENDURECIDO');assert.equal(treeNode.source.printedPage,68);

 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);

 const result=bridge.buyTalent(TOUGHENED_POSITION);
 assert.equal(result.ok,true);assert.equal(result.data.talent.id,TOUGHENED_POSITION);
 assert.deepEqual(result.data.effect,{applied:true,label:'+2 no limiar de Ferimentos'});
 const owned=result.data.character.talentPurchases.find(p=>p.nodeId===TOUGHENED_POSITION);
 assert.equal(owned.originalName,'ENDURECIDO');assert.equal(owned.source.printedPage,68);
 assert.equal(owned.specializationKey,'gunner');assert.equal(owned.cost,10);
 const threshold=before.derived.wounds.threshold+2;
 assert.equal(result.data.character.derived.wounds.threshold,threshold);
 assert.equal(result.data.character.xp.available,before.xp.available-15);
 const atBoundary=clone(result.data.character);atBoundary.derived.wounds.current=threshold;
 assert.equal(getHealthStatus(atBoundary).woundIncapacitated,false);
 const pastBoundary=clone(result.data.character);pastBoundary.derived.wounds.current=threshold+1;
 assert.equal(getHealthStatus(pastBoundary).woundIncapacitated,true);

 const saved=repo.saves;
 for(let reopen=1;reopen<=3;reopen++){
  const game=new GameController(repo),character=game.state.player.character;
  const receipt=character.talentPurchases.find(p=>p.nodeId===TOUGHENED_POSITION);
  assert.ok(receipt,`exact source node survives reopen ${reopen}`);
  assert.equal(receipt.source.printedPage,68);assert.equal(receipt.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
  assert.equal(character.derived.wounds.threshold,threshold);
  assert.equal(game.healthStatus().effectiveWoundThreshold,threshold);
  assert.equal(game.eligibleTalents().find(n=>n.id===TOUGHENED_POSITION).purchased,true);
 }
 assert.equal(repo.saves,saved,'reopens are read-only');
 assert.equal(bridge.buyTalent(TOUGHENED_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R3C2 Enduring buys exact node, raises live soak and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Jora Kinn'});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2']){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===ENDURING_POSITION);
 assert.ok(candidate);assert.equal(candidate.originalName,'RESISTENTE');assert.equal(candidate.name,'Resistente');
 assert.equal(candidate.available,true);assert.equal(candidate.cost,15);assert.equal(candidate.source.printedPage,68);
 assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===ENDURING_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'RESISTENTE');assert.equal(treeNode.source.printedPage,68);

 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);

 const result=bridge.buyTalent(ENDURING_POSITION);
 assert.equal(result.ok,true);assert.equal(result.data.talent.id,ENDURING_POSITION);
 assert.deepEqual(result.data.effect,{applied:true,label:'+1 de Proteção'});
 const receipt=result.data.character.talentPurchases.find(p=>p.nodeId===ENDURING_POSITION);
 assert.equal(receipt.originalName,'RESISTENTE');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,15);
 const soak=result.data.character.derived.soak;
 assert.equal(soak,before.derived.soak+1);
 assert.equal(result.data.character.xp.available,before.xp.available-40);

 const game=new GameController(repo),damage=game.applyPlayerDamage({damage:5,reason:'prova R3C2'});
 assert.equal(damage.effectiveSoak,soak);assert.equal(damage.suffered,Math.max(0,5-soak));
 const savedState=clone(repo.state),savedWrites=repo.saves;
 for(let reopen=1;reopen<=3;reopen++){
  const reopenedRepo=new RepoWithState(savedState),reopened=new GameController(reopenedRepo);
  const character=reopened.state.player.character,owned=character.talentPurchases.find(p=>p.nodeId===ENDURING_POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
  assert.equal(character.derived.soak,soak);
  const reopenedDamage=reopened.applyPlayerDamage({damage:5,reason:`reopen ${reopen}`});
  assert.equal(reopenedDamage.effectiveSoak,soak);assert.equal(reopenedDamage.suffered,Math.max(0,5-soak));
 }
 assert.equal(repo.saves,savedWrites,'reopen evidence uses independent restored controllers');
 assert.equal(bridge.buyTalent(ENDURING_POSITION).ok,false,'the exact node cannot be purchased twice');
});
