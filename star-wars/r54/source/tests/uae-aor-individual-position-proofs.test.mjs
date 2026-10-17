import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {getHealthStatus} from '../app/src/main/assets/vtt/js/rules/health.mjs';
import {vehicleStatus} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {weaponCatalogEntry} from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';
import {weaponQualityRank} from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import {juryRiggedWeapon} from '../app/src/main/assets/vtt/js/rules/talents.mjs';

const POSITION='ace:gunner:r1c2';
const DURABLE_POSITION='ace:gunner:r1c1';
const TOUGHENED_POSITION='ace:gunner:r2c1';
const ENDURING_POSITION='ace:gunner:r3c2';
const DURABLE_RANK2_POSITION='ace:gunner:r3c1';
const TOUGHENED_RANK2_POSITION='ace:gunner:r4c1';
const OVERWHELM_POSITION='ace:gunner:r1c3';
const DEBILITATING_POSITION='ace:gunner:r1c4';
const SPARE_CLIP_POSITION='ace:gunner:r2c3';
const ENDURING_RANK2_POSITION='ace:gunner:r4c2';
const EXHAUST_PORT_POSITION='ace:gunner:r4c4';
const HEROIC_FORTITUDE_POSITION='ace:gunner:r5c1';
const JURY_RIGGED_POSITION='ace:gunner:r3c3';
const JURY_RIGGED_RANK2_POSITION='ace:gunner:r5c2';
const TRUE_AIM_POSITION='ace:gunner:r2c4';
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

test('AoR p.68 Ace/Gunner R3C1 Durable rank 2 and R4C1 Toughened rank 2 preserve exact IDs and cumulative effects',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Rell Tannis'});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',ENDURING_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const rank2Durable=new GameController(repo).eligibleTalents().find(n=>n.id===DURABLE_RANK2_POSITION);
 assert.ok(rank2Durable);assert.equal(rank2Durable.available,true);assert.equal(rank2Durable.originalName,'ROBUSTO');
 assert.equal(rank2Durable.cost,15);assert.equal(rank2Durable.source.printedPage,68);
 const durableTreeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===DURABLE_RANK2_POSITION);
 assert.ok(durableTreeNode);assert.equal(durableTreeNode.originalName,'ROBUSTO');
 const durableResult=bridge.buyTalent(DURABLE_RANK2_POSITION);
 assert.equal(durableResult.ok,true);assert.equal(durableResult.data.talent.id,DURABLE_RANK2_POSITION);
 assert.deepEqual(durableResult.data.effect,{applied:false,label:null});
 assert.equal(durableResult.data.character.xp.available,before.xp.available-55);
 const durableReceipt=durableResult.data.character.talentPurchases.find(p=>p.nodeId===DURABLE_RANK2_POSITION);
 assert.equal(durableReceipt.source.printedPage,68);assert.equal(durableReceipt.specializationKey,'gunner');

 const criticalGame=new GameController(repo),critical=criticalGame.applyPlayerCritical('prova rank 2',{roll:50});
 assert.equal(critical.injury.durableModifier,-20);assert.equal(critical.total,30);
 const floorGame=new GameController(new RepoWithState(createCampaignWithCharacter(repo.state)));
 const floor=floorGame.applyPlayerCritical('piso rank 2',{roll:1});
 assert.equal(floor.total,1,'two Durable ranks still cannot reduce the critical result below 1');

 const toughenedCandidate=new GameController(repo).eligibleTalents().find(n=>n.id===TOUGHENED_RANK2_POSITION);
 assert.ok(toughenedCandidate);assert.equal(toughenedCandidate.available,true);
 assert.equal(toughenedCandidate.originalName,'ENDURECIDO');assert.equal(toughenedCandidate.cost,20);
 assert.equal(toughenedCandidate.source.printedPage,68);
 const toughenedTreeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===TOUGHENED_RANK2_POSITION);
 assert.ok(toughenedTreeNode);assert.equal(toughenedTreeNode.originalName,'ENDURECIDO');
 const toughenedResult=bridge.buyTalent(TOUGHENED_RANK2_POSITION);
 assert.equal(toughenedResult.ok,true);assert.equal(toughenedResult.data.talent.id,TOUGHENED_RANK2_POSITION);
 assert.deepEqual(toughenedResult.data.effect,{applied:true,label:'+2 no limiar de Ferimentos'});
 const toughenedReceipt=toughenedResult.data.character.talentPurchases.find(p=>p.nodeId===TOUGHENED_RANK2_POSITION);
 assert.equal(toughenedReceipt.source.printedPage,68);assert.equal(toughenedReceipt.specializationKey,'gunner');
 assert.equal(toughenedReceipt.cost,20);
 const threshold=before.derived.wounds.threshold+4;
 assert.equal(toughenedResult.data.character.derived.wounds.threshold,threshold);
 assert.equal(toughenedResult.data.character.xp.available,before.xp.available-75);
 const atBoundary=clone(toughenedResult.data.character);atBoundary.derived.wounds.current=threshold;
 assert.equal(getHealthStatus(atBoundary).woundIncapacitated,false);
 const pastBoundary=clone(toughenedResult.data.character);pastBoundary.derived.wounds.current=threshold+1;
 assert.equal(getHealthStatus(pastBoundary).woundIncapacitated,true);

 const savedState=clone(repo.state);
 for(let reopen=1;reopen<=3;reopen++){
  const reopened=new GameController(new RepoWithState(savedState)),character=reopened.state.player.character;
  for(const id of [DURABLE_RANK2_POSITION,TOUGHENED_RANK2_POSITION]){
   const receipt=character.talentPurchases.find(p=>p.nodeId===id);
   assert.ok(receipt,`exact node ${id} survives reopen ${reopen}`);
   assert.equal(receipt.source.printedPage,68);assert.equal(receipt.specializationKey,'gunner');
  }
  assert.equal(character.derived.wounds.threshold,threshold);
  assert.equal(reopened.healthStatus().effectiveWoundThreshold,threshold);
  const reopenedCritical=reopened.applyPlayerCritical(`reopen rank 2 ${reopen}`,{roll:50});
  assert.equal(reopenedCritical.injury.durableModifier,-20);
  assert.equal(reopenedCritical.total,40,'the saved prior injury modifier is combined with both Durable ranks');
 }
 assert.equal(bridge.buyTalent(DURABLE_RANK2_POSITION).ok,false);
 assert.equal(bridge.buyTalent(TOUGHENED_RANK2_POSITION).ok,false);
});

function createCampaignWithCharacter(state){return clone(state);}

test('AoR p.68 Ace/Gunner R1C3 Overwhelm buys exact node, spends Advantage on a failed vehicle attack and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');
 setup.createAorCharacter({...characterOptions,name:'Sera Tonn',specializationFreeSkills:['artilleria','disciplina']});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 const candidate=setup.eligibleTalents().find(n=>n.id===OVERWHELM_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'ABRUMAR DEFENSAS');
 assert.equal(candidate.name,'Sobrecarregar defesas');assert.equal(candidate.cost,5);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===OVERWHELM_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'ABRUMAR DEFENSAS');assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 const purchased=bridge.buyTalent(OVERWHELM_POSITION);
 assert.equal(purchased.ok,true);assert.equal(purchased.data.talent.id,OVERWHELM_POSITION);
 assert.deepEqual(purchased.data.effect,{applied:false,label:null});
 assert.equal(purchased.data.character.xp.available,before.xp.available-5);
 const receipt=purchased.data.character.talentPurchases.find(p=>p.nodeId===OVERWHELM_POSITION);
 assert.equal(receipt.originalName,'ABRUMAR DEFENSAS');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,5);

 const saved=clone(repo.state);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new RepoWithState(saved),game=new GameController(restored);
  const character=game.state.player.character,owned=character.talentPurchases.find(p=>p.nodeId===OVERWHELM_POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  assert.equal(game.eligibleTalents().find(n=>n.id===OVERWHELM_POSITION).purchased,true);
  game.addVehicle('xwing_t65b');
  game.beginVehicleCombat({targetProfileKey:'vt49_decimator',range:'Bocajarro'});
  let i=0;const rng=()=>[.75,.75,.125,.125,0,0][i++%6];
  const attack=game.resolveVehicleAttack({rng});
  assert.equal(attack.hit,false);assert.ok(attack.check.netAdvantage>=2);
  assert.ok(attack.pendingSpend.qualities.some(q=>q.key==='aor_overwhelm'&&q.rank===1));
  const targetId=game.state.vehicles.combat.targetVehicleId;
  const beforeZone=vehicleStatus(game.state.vehicles.vehicles[targetId],{currentRound:1}).defense.proa;
  const activation=game.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'});
  assert.equal(activation.effect.zone,'proa');assert.equal(activation.effect.defenseAfter,beforeZone-1);
  assert.equal(activation.remainingAdvantage,attack.check.netAdvantage-2);
  assert.equal(vehicleStatus(game.state.vehicles.vehicles[targetId],{currentRound:1}).defense.proa,beforeZone-1);
  assert.equal(new GameController(restored).state.vehicles.vehicles[targetId].roundEffects.aorOverwhelmedDefense.proa,beforeZone-1);
 }
 assert.equal(bridge.buyTalent(OVERWHELM_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R1C4 Debilitating Shot buys exact node, spends Advantage on a hit and persists speed reduction',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');
 setup.createAorCharacter({...characterOptions,name:'Vela Marr',specializationFreeSkills:['artilleria','disciplina']});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 const candidate=setup.eligibleTalents().find(n=>n.id===DEBILITATING_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'DISPARO DEBILITANTE');
 assert.equal(candidate.name,'Disparo debilitante');assert.equal(candidate.cost,5);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===DEBILITATING_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'DISPARO DEBILITANTE');assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 const purchased=bridge.buyTalent(DEBILITATING_POSITION);
 assert.equal(purchased.ok,true);assert.equal(purchased.data.talent.id,DEBILITATING_POSITION);
 assert.deepEqual(purchased.data.effect,{applied:false,label:null});
 assert.equal(purchased.data.character.xp.available,before.xp.available-5);
 const receipt=purchased.data.character.talentPurchases.find(p=>p.nodeId===DEBILITATING_POSITION);
 assert.equal(receipt.originalName,'DISPARO DEBILITANTE');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,5);
 const saved=clone(repo.state);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new RepoWithState(saved),game=new GameController(restored);
  const character=game.state.player.character,owned=character.talentPurchases.find(p=>p.nodeId===DEBILITATING_POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  assert.equal(game.eligibleTalents().find(n=>n.id===DEBILITATING_POSITION).purchased,true);
  game.addVehicle('xwing_t65b');
  game.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Bocajarro'});
  const vehicles=clone(game.state.vehicles),targetId=vehicles.combat.targetVehicleId;
  vehicles.vehicles[targetId].speed=5;game.commit('VEHICLES_UPDATED',{vehicles});
  const beforeStatus=vehicleStatus(game.state.vehicles.vehicles[targetId],{currentRound:1});
  assert.equal(beforeStatus.effectiveSpeedMax,5);
  let i=0;const rng=()=>[.999,.999,0,0,0][i++%5];
  const attack=game.resolveVehicleAttack({rng});
  assert.equal(attack.hit,true);assert.ok(attack.check.netAdvantage>=2);
  assert.ok(attack.pendingSpend.qualities.some(q=>q.key==='aor_debilitating'&&q.rank===1));
  const activation=game.activatePendingVehicleQuality({qualityKey:'aor_debilitating'});
  assert.equal(activation.effect.untilRound,2);assert.equal(activation.remainingAdvantage,attack.check.netAdvantage-2);
  assert.equal(vehicleStatus(game.state.vehicles.vehicles[targetId],{currentRound:1}).effectiveSpeedMax,4);
  const restoredStatus=new GameController(restored).state.vehicles.vehicles[targetId];
  assert.equal(vehicleStatus(restoredStatus,{currentRound:1}).effectiveSpeedMax,4);
  assert.equal(vehicleStatus(restoredStatus,{currentRound:2}).effectiveSpeedMax,4);
  assert.equal(vehicleStatus(restoredStatus,{currentRound:3}).effectiveSpeedMax,5);
 }
 assert.equal(bridge.buyTalent(DEBILITATING_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R2C3 Spare Clip buys exact node, prevents Despair ammo loss, preserves Limited Ammo exception and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');
 setup.createAorCharacter({...characterOptions,name:'Mira Sol',specializationFreeSkills:['artilleria','disciplina']});
 const before=clone(setup.state.player.character),weapon=weaponCatalogEntry('blaster_pistol');
 assert.ok(weapon);assert.equal(weaponQualityRank(weapon,'limitedAmmo'),0);
 const equipped={...clone(weapon),sourceCatalogId:weapon.id,ownedAssetId:'uae-r2c3-blaster',active:true,equipped:true,runtimeState:{}};
 const armed=clone(setup.state.player.character);armed.weapons=[equipped];setup.setCharacter(armed);
 const bridge=new BraseiroUiBridge({repository:repo});
 const prerequisite=bridge.buyTalent(OVERWHELM_POSITION);
 assert.equal(prerequisite.ok,true,'the adjacent exact R1C3 prerequisite is purchased first');
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===SPARE_CLIP_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'MUNICIÓN DE RESERVA');
 assert.equal(candidate.name,'Munição reserva');assert.equal(candidate.cost,10);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===SPARE_CLIP_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'MUNICIÓN DE RESERVA');assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 assert.ok(runtime.includes('data-p160-despair'));
 assert.match(runtime,/call\('adjudicateAttackDespairAmmoLoss',\{weaponIndex:weapon\(\)\}\)/);
 const purchased=bridge.buyTalent(SPARE_CLIP_POSITION);
 assert.equal(purchased.ok,true);assert.equal(purchased.data.talent.id,SPARE_CLIP_POSITION);
 assert.deepEqual(purchased.data.effect,{applied:false,label:null});
 assert.equal(purchased.data.character.xp.available,before.xp.available-15);
 const receipt=purchased.data.character.talentPurchases.find(p=>p.nodeId===SPARE_CLIP_POSITION);
 assert.equal(receipt.originalName,'MUNICIÓN DE RESERVA');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,10);
 const purchasedState=clone(repo.state);

 const ordinaryRepo=new RepoWithState(purchasedState),ordinary=new GameController(ordinaryRepo);
 ordinary.beginCombat();
 const ordinaryAttack=ordinary.resolveWeaponAttack({weaponIndex:0,challenge:1,difficulty:0,rng:()=>0.999});
 assert.ok(ordinaryAttack.check.despair>=1);assert.equal(ordinaryAttack.attackId,ordinary.state.combat.lastAttack.attackId);
 const ammoBridge=new BraseiroUiBridge({repository:ordinaryRepo});
 const adjudication=ammoBridge.adjudicateAttackDespairAmmoLoss({weaponIndex:0});
 assert.equal(adjudication.ok,true);
 const prevention=adjudication.data;
 assert.equal(prevention.prevented,true);assert.equal(prevention.ammoLost,false);
 const adjudicatedState=new GameController(ordinaryRepo).state;
 assert.equal(adjudicatedState.player.character.weapons[0].runtimeState?.outOfAmmo,undefined);
 assert.equal(adjudicatedState.combat.ammoDespairAppliedAttackId,ordinaryAttack.attackId);
 const postAdjudication=clone(ordinaryRepo.state);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(postAdjudication));
  assert.equal(validateState(restored.state),true);
  const owned=restored.state.player.character.talentPurchases.find(p=>p.nodeId===SPARE_CLIP_POSITION);
  assert.ok(owned,`exact source node survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);
  assert.equal(restored.state.player.character.weapons[0].runtimeState?.outOfAmmo,undefined);
  assert.throws(()=>restored.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/já foi adjudicado/);
 }

 const limitedRepo=new RepoWithState(purchasedState),limited=new GameController(limitedRepo);
 const limitedCharacter=clone(limited.state.player.character);
 limitedCharacter.weapons[0].qualities=['Munição limitada 2'];limited.setCharacter(limitedCharacter);
 limited.beginCombat();
 const limitedAttack=limited.resolveWeaponAttack({weaponIndex:0,challenge:1,difficulty:0,rng:()=>0.999});
 assert.ok(limitedAttack.check.despair>=1);
 assert.equal(limited.state.player.character.weapons[0].runtimeState.limitedAmmoRemaining,1);
 assert.throws(()=>limited.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/Munição Limitada/);
 assert.equal(limited.state.player.character.weapons[0].runtimeState.outOfAmmo,undefined);
 const limitedReload=new GameController(limitedRepo);
 assert.equal(validateState(limitedReload.state),true);
 assert.equal(limitedReload.state.player.character.weapons[0].runtimeState.limitedAmmoRemaining,1);
 assert.equal(limitedReload.state.player.character.weapons[0].runtimeState.outOfAmmo,undefined);
 assert.equal(bridge.buyTalent(SPARE_CLIP_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R4C2 Enduring rank 2 buys exact node, stacks soak and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Talla Rinn'});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',ENDURING_POSITION,DURABLE_RANK2_POSITION,TOUGHENED_RANK2_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===ENDURING_RANK2_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'RESISTENTE');
 assert.equal(candidate.name,'Resistente');assert.equal(candidate.cost,20);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===ENDURING_RANK2_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'RESISTENTE');assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));
 assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 const beforeRank2=new GameController(repo).state.player.character.derived.soak;
 const purchase=bridge.buyTalent(ENDURING_RANK2_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,ENDURING_RANK2_POSITION);
 assert.deepEqual(purchase.data.effect,{applied:true,label:'+1 de Proteção'});
 assert.equal(purchase.data.character.derived.soak,beforeRank2+1);
 assert.equal(purchase.data.character.derived.soak,before.derived.soak+2);
 assert.equal(purchase.data.character.xp.available,before.xp.available-95);
 const receipt=purchase.data.character.talentPurchases.find(p=>p.nodeId===ENDURING_RANK2_POSITION);
 assert.equal(receipt.originalName,'RESISTENTE');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,20);

 const damageGame=new GameController(repo),damage=damageGame.applyPlayerDamage({damage:5,reason:'prova rank 2 de Resistente'});
 assert.equal(damage.effectiveSoak,before.derived.soak+2);
 assert.equal(damage.suffered,Math.max(0,5-(before.derived.soak+2)));
 const saved=clone(repo.state);
 for(let reopen=1;reopen<=3;reopen++){
  const restoredRepo=new RepoWithState(saved),restored=new GameController(restoredRepo);
  const character=restored.state.player.character;
  for(const id of [ENDURING_POSITION,ENDURING_RANK2_POSITION]){
   const owned=character.talentPurchases.find(p=>p.nodeId===id);
   assert.ok(owned,`exact rank node ${id} survives reopen ${reopen}`);
   assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  }
  assert.equal(character.derived.soak,before.derived.soak+2);
  const reopenedDamage=restored.applyPlayerDamage({damage:5,reason:`reopen rank 2 ${reopen}`});
  assert.equal(reopenedDamage.effectiveSoak,before.derived.soak+2);
  assert.equal(reopenedDamage.suffered,Math.max(0,5-(before.derived.soak+2)));
 }
 assert.equal(bridge.buyTalent(ENDURING_RANK2_POSITION).ok,false,'the exact rank node cannot be purchased twice');
});


test('AoR p.68 Ace/Gunner R4C3 Brace rank 2 buys exact node, removes two environmental setbacks and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Veya Sol'});
 let character=clone(setup.state.player.character);character.skills.artilleria=1;setup.setCharacter(character);
 const before=clone(new GameController(repo).state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',ENDURING_POSITION,DURABLE_RANK2_POSITION,TOUGHENED_RANK2_POSITION,ENDURING_RANK2_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const accrued=new GameController(repo).grantXp(50,{reason:'Prova R4C3: XP adicional para completar a árvore conectada'});
 assert.equal(accrued.character.xp.available,before.xp.available-45);
 for(const id of [OVERWHELM_POSITION,'ace:gunner:r2c3','ace:gunner:r3c3']){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact Brace path prerequisite ${id} is acquired`);
 }
 const targetId='ace:gunner:r4c3',candidate=new GameController(repo).eligibleTalents().find(n=>n.id===targetId);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'FIRMEZA');
 assert.equal(candidate.name,'Firmeza');assert.equal(candidate.cost,20);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===targetId);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'FIRMEZA');assert.equal(treeNode.cost,20);assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);assert.ok(html.includes('id="braceButton"'));assert.match(ui,/prepareAoRBrace/);
 const xpBeforeRank2=new GameController(repo).state.player.character.xp.available;
 const purchase=bridge.buyTalent(targetId);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,targetId);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===targetId);
 assert.ok(receipt);assert.equal(receipt.originalName,'FIRMEZA');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.source.book,'Star Wars - Age of Rebellion - Core Rulebook');assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,20);
 assert.equal(purchase.data.character.xp.available,xpBeforeRank2-20);
 assert.equal(new GameController(repo).aorBraceStatus().rank,2,'both exact p.68 source-backed Gunner Brace ranks are recognized');
 const combat=new GameController(repo);combat.beginCombat();
 const braceBridge=new BraseiroUiBridge({repository:repo}),prepared=braceBridge.prepareAoRBrace();
 assert.equal(prepared.ok,true);assert.equal(prepared.data.rank,2);assert.equal(prepared.data.combat.playerTurn.maneuversUsed,1);
 assert.deepEqual(prepared.data.source,{productCode:'AOR',printedPage:68,nodeIds:['ace:gunner:r2c2',targetId]});
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(repo.state));
  assert.equal(restored.state.combat.aorBracePrepared.rank,2,`rank-2 preparation survives reopen ${reopen}`);
  assert.deepEqual(restored.state.combat.aorBracePrepared.source,prepared.data.source);
  assert.ok(restored.state.player.character.talentPurchases.some(entry=>entry.nodeId===targetId));
 }
 const action=new GameController(repo),check=action.resolveSkillCheck({skillKey:'artilleria',difficulty:2,setback:3,braceEnvironmentalSetback:2,rng:()=>0});
 assert.equal(check.pool.setback,1,'rank 2 removes both declared environmental setbacks and leaves the other setback');
 assert.deepEqual(check.aorBrace,{rank:2,environmentalSetback:2,removedSetback:2,source:prepared.data.source});
 assert.equal(action.state.combat.aorBracePrepared,undefined,'Brace is consumed by that next action');
 for(let reopen=1;reopen<=3;reopen++)assert.equal(new GameController(new RepoWithState(repo.state)).state.combat.aorBracePrepared,undefined,`consumed preparation remains cleared after reopen ${reopen}`);
 assert.equal(bridge.buyTalent(targetId).ok,false,'the exact rank-2 node cannot be purchased twice');
});


test('AoR p.68 Ace/Gunner R4C4 Exhaust Port buys exact node, ignores Massive on vehicle attack and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Kira Neth'});
 const before=clone(setup.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 for(const id of ['ace:gunner:r1c4','ace:gunner:r2c4','ace:gunner:r3c4']){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===EXHAUST_PORT_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'SALIDA TÉRMICA');
 assert.equal(candidate.name,'Saída térmica');assert.equal(candidate.cost,20);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===EXHAUST_PORT_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'SALIDA TÉRMICA');assert.equal(treeNode.cost,20);assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.ok(html.includes('id="talentOptions"'));assert.match(ui,/data-talent-id="\$\{escapeHtml\(t\.id\)\}"/);
 assert.match(ui,/game\.buyTalent\(button\.dataset\.talentId/);
 assert.match(html,/id="exhaustPortLabel"[^>]*hidden><input id="useExhaustPort" type="checkbox"/);
 assert.match(ui,/resolveVehicleAttack\(\{weaponIndex:Number\(\$\('vehicleWeaponSelect'\)\.value\),useExhaustPort:\$\('useExhaustPort'\)\.checked\}/);
 assert.match(ui,/game\.talentRank\('EXHAUST_PORT'\)/);
 const purchase=bridge.buyTalent(EXHAUST_PORT_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,EXHAUST_PORT_POSITION);
 assert.equal(purchase.data.character.xp.available,before.xp.available-50);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===EXHAUST_PORT_POSITION);
 assert.ok(receipt);assert.equal(receipt.originalName,'SALIDA TÉRMICA');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.source.book,'Star Wars - Age of Rebellion - Core Rulebook');assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,20);
 const setupAfterPurchase=new GameController(repo);setupAfterPurchase.startSession({rng:()=>0.4});
 setupAfterPurchase.addVehicle('xwing_t65b');setupAfterPurchase.setGroupDestiny({light:3,dark:0});
 setupAfterPurchase.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Bocajarro'});
 const vehicles=clone(setupAfterPurchase.state.vehicles),targetId=vehicles.combat.targetVehicleId;
 vehicles.vehicles[targetId].massive=2;setupAfterPurchase.commit('VEHICLES_UPDATED',{vehicles});
 const ready=clone(new GameController(repo).state);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(ready));
  assert.equal(validateState(restored.state),true);assert.ok(restored.state.player.character.talentPurchases.some(entry=>entry.nodeId===EXHAUST_PORT_POSITION));
  assert.equal(restored.state.vehicles.combat.active,true);assert.equal(restored.state.vehicles.vehicles[targetId].massive,2);
  assert.equal(restored.state.group.destiny.light,3);
 }
 const attackBridge=new BraseiroUiBridge({repository:repo}),resolved=attackBridge.resolveVehicleAttack({weaponIndex:0,useExhaustPort:true,rng:()=>0.4});
 assert.equal(resolved.ok,true,JSON.stringify(resolved));const attack=resolved.data;
 assert.equal(attack.massiveRating,2);assert.equal(attack.massiveIgnored,2);assert.equal(attack.criticalCost,Number(attack.weapon.crit));
 assert.equal(attack.exhaustPortUsed,true);assert.equal(new GameController(repo).state.group.destiny.light,2);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(repo.state));assert.equal(validateState(restored.state),true);
  assert.ok(restored.state.player.character.talentPurchases.some(entry=>entry.nodeId===EXHAUST_PORT_POSITION));
  assert.equal(restored.state.group.destiny.light,2);assert.equal(restored.state.vehicles.combat.lastAttack.massiveIgnored,2);
 }
 assert.equal(bridge.buyTalent(EXHAUST_PORT_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R5C1 Heroic Fortitude buys exact node, shields Brawn check and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Rina Vale'});
 const character=clone(setup.state.player.character);
 character.criticalInjuries=[
  {id:'r5c1-setback',active:true,effect:{setback:1,duration:'until_healed'}},
  {id:'r5c1-brawn-difficulty',active:true,effect:{difficultyIncrease:1,difficultyCharacteristics:['fortaleza','agilidad'],duration:'until_healed'}}
 ];setup.setCharacter(character);
 const before=clone(new GameController(repo).state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',ENDURING_POSITION,DURABLE_RANK2_POSITION,TOUGHENED_RANK2_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`exact path prerequisite ${id} is acquired`);
 }
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===HEROIC_FORTITUDE_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'MADERA DE HÉROE');
 assert.equal(candidate.name,'Fibra de herói');assert.equal(candidate.cost,25);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===HEROIC_FORTITUDE_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'MADERA DE HÉROE');assert.equal(treeNode.cost,25);assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.match(html,/id="heroicFortitudeControl" hidden/);assert.match(html,/id="heroicFortitudeButton"/);
 assert.match(ui,/game\.talentRank\('HERFORT'\)/);assert.match(ui,/game\.activateHeroicFortitude\(\)/);
 const purchase=bridge.buyTalent(HEROIC_FORTITUDE_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,HEROIC_FORTITUDE_POSITION);
 assert.equal(purchase.data.character.xp.available,before.xp.available-100);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===HEROIC_FORTITUDE_POSITION);
 assert.ok(receipt);assert.equal(receipt.originalName,'MADERA DE HÉROE');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.source.book,'Star Wars - Age of Rebellion - Core Rulebook');assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,25);
 const encounter=new GameController(repo),group=clone(encounter.state.group);group.destiny={light:2,dark:1};encounter.commit('GROUP_UPDATED',{group});encounter.beginCombat();
 const activationBridge=new BraseiroUiBridge({repository:repo}),activation=activationBridge.activateHeroicFortitude();
 assert.equal(activation.ok,true);assert.equal(activation.data.active,true);assert.equal(activation.data.costDestiny,1);
 assert.equal(activation.data.group.destiny.light,1);
 assert.equal(activation.data.character.combatEffects.heroicFortitude,true);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(repo.state));assert.equal(validateState(restored.state),true);
  assert.ok(restored.state.player.character.talentPurchases.some(entry=>entry.nodeId===HEROIC_FORTITUDE_POSITION));
  assert.equal(restored.state.player.character.combatEffects.heroicFortitude,true,`active effect survives reopen ${reopen}`);
  assert.equal(restored.state.group.destiny.light,1);
  assert.throws(()=>restored.activateHeroicFortitude(),/já está ativa/);
 }
 const protectedCheck=new GameController(repo).resolveSkillCheck({skillKey:'atletismo',difficulty:2,rng:()=>0});
 assert.equal(protectedCheck.pool.setback,0,'the exact R5C1 effect removes a live Brawn Critical Injury setback');
 assert.equal(protectedCheck.pool.difficulty,2,'the live Brawn check keeps its base difficulty after R5C1 suppresses the Critical Injury increase');
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(repo.state));assert.equal(validateState(restored.state),true);
  assert.ok(restored.state.player.character.talentPurchases.some(entry=>entry.nodeId===HEROIC_FORTITUDE_POSITION));
  assert.equal(restored.state.player.character.combatEffects.heroicFortitude,true);
 }
 assert.equal(bridge.buyTalent(HEROIC_FORTITUDE_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R5C2 Jury Rigged rank 2 buys exact node, improves a weapon and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Daro Venn'});
 const weapon=weaponCatalogEntry('blaster_pistol');assert.ok(weapon);
 const armed=clone(setup.state.player.character);
 armed.weapons=[{...clone(weapon),sourceCatalogId:weapon.id,ownedAssetId:'uae-r5c2-blaster',active:true,equipped:true,runtimeState:{}}];
 setup.setCharacter(armed);
 const bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',ENDURING_POSITION,DURABLE_RANK2_POSITION,TOUGHENED_RANK2_POSITION,HEROIC_FORTITUDE_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`connected prerequisite ${id} is purchased`);
 }
 new GameController(repo).grantXp(25,{reason:'Prova R5C2: XP acumulado para a segunda melhoria de arma'});
 const before=clone(repo.state.player.character);
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===JURY_RIGGED_RANK2_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'MEJORAS CASERAS');
 assert.equal(candidate.name,'Melhorias caseiras');assert.equal(candidate.cost,25);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===JURY_RIGGED_RANK2_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'MEJORAS CASERAS');assert.equal(treeNode.source.printedPage,68);

 const purchase=bridge.buyTalent(JURY_RIGGED_RANK2_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,JURY_RIGGED_RANK2_POSITION);
 assert.deepEqual(purchase.data.effect,{applied:false,label:null});
 assert.equal(purchase.data.character.xp.available,before.xp.available-25);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===JURY_RIGGED_RANK2_POSITION);
 assert.equal(receipt.originalName,'MEJORAS CASERAS');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,25);

 const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(runtime.includes('Melhorias caseiras ${i+1}'),'Android UI exposes the exact acquired Jury Rigged rank in its selector');
 assert.ok(runtime.includes("call('assignJuryRigged'"),'Android UI routes the assignment through the deterministic bridge');
 const shell=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46-shell.html',import.meta.url),'utf8');
 assert.ok(Number(shell.match(/data-version="1\.0\.(\d+)"/)?.[1])>=171);

 const assignment=bridge.assignJuryRigged({nodeId:JURY_RIGGED_RANK2_POSITION,weaponIndex:0,effect:'damage'});
 assert.equal(assignment.ok,true,JSON.stringify(assignment));assert.equal(assignment.data.nodeId,JURY_RIGGED_RANK2_POSITION);
 const status=bridge.aorP170Status();assert.equal(status.ok,true);
 assert.equal(status.data.juryRigged.nodes.includes(JURY_RIGGED_RANK2_POSITION),true);
 assert.equal(status.data.weapons[0].effect,'damage');
 const assignedState=clone(repo.state),weaponBaseDamage=assignedState.player.character.weapons[0].damage;
 const unmodifiedState=clone(assignedState);
 delete unmodifiedState.player.character.talentAssignments.juryRigged;
 delete unmodifiedState.player.character.weapons[0].juryRiggedInstanceId;
 const unmodifiedAttack=new GameController(new RepoWithState(unmodifiedState)).resolveWeaponAttack({weaponIndex:0,difficulty:1,rng:()=>.49});
 assert.equal(juryRiggedWeapon(assignedState.player.character,assignedState.player.character.weapons[0]).weapon.damage,weaponBaseDamage+1);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(repo),character=restored.state.player.character;
  const owned=character.talentPurchases.find(entry=>entry.nodeId===JURY_RIGGED_RANK2_POSITION);
  assert.ok(owned,`exact R5C2 source purchase survives reload ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  assert.equal(character.talentAssignments.juryRigged.some(entry=>entry.nodeId===JURY_RIGGED_RANK2_POSITION),true);
  assert.equal(restored.state.player.character.weapons[0].juryRiggedInstanceId,assignedState.player.character.weapons[0].juryRiggedInstanceId);
  const modified=new GameController(repo);
  const attack=modified.resolveWeaponAttack({weaponIndex:0,difficulty:1,rng:()=>.49});
  assert.equal(attack.hit,true);assert.equal(attack.juryRigged,'damage');
  assert.equal(attack.damage,unmodifiedAttack.damage+1,'the real attack includes exactly 1 extra R5C2 Jury Rigged damage');
 }
 assert.equal(bridge.buyTalent(JURY_RIGGED_RANK2_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R3C3 Jury Rigged buys exact node, improves an owned weapon and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Siv Arlo'});
 const weapon=weaponCatalogEntry('blaster_pistol');assert.ok(weapon);
 const armed=clone(setup.state.player.character);
 armed.weapons=[{...clone(weapon),sourceCatalogId:weapon.id,ownedAssetId:'uae-r3c3-blaster',active:true,equipped:true,runtimeState:{}}];
 setup.setCharacter(armed);
 const bridge=new BraseiroUiBridge({repository:repo});
 for(const id of [DURABLE_POSITION,TOUGHENED_POSITION,'ace:gunner:r2c2',OVERWHELM_POSITION,SPARE_CLIP_POSITION]){
  const prerequisite=bridge.buyTalent(id);assert.equal(prerequisite.ok,true,`connected prerequisite ${id} is purchased`);
 }
 const before=clone(repo.state.player.character);
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===JURY_RIGGED_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'MEJORAS CASERAS');
 assert.equal(candidate.name,'Melhorias caseiras');assert.equal(candidate.cost,15);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===JURY_RIGGED_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'MEJORAS CASERAS');assert.equal(treeNode.cost,15);assert.equal(treeNode.source.printedPage,68);
 const purchase=bridge.buyTalent(JURY_RIGGED_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,JURY_RIGGED_POSITION);
 assert.deepEqual(purchase.data.effect,{applied:false,label:null});
 assert.equal(purchase.data.character.xp.available,before.xp.available-15);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===JURY_RIGGED_POSITION);
 assert.equal(receipt.originalName,'MEJORAS CASERAS');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,15);

 const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(runtime.includes('Melhorias caseiras ${i+1}'),'Android UI exposes the acquired Jury Rigged degree');
 assert.ok(runtime.includes("call('assignJuryRigged'"),'Android UI routes the assignment through the deterministic bridge');
 const assignment=bridge.assignJuryRigged({nodeId:JURY_RIGGED_POSITION,weaponIndex:0,effect:'crit'});
 assert.equal(assignment.ok,true);assert.equal(assignment.data.nodeId,JURY_RIGGED_POSITION);
 const status=bridge.aorP170Status();assert.equal(status.ok,true);
 assert.equal(status.data.juryRigged.nodes.includes(JURY_RIGGED_POSITION),true);
 assert.equal(status.data.weapons[0].effect,'crit');
 const assignedState=clone(repo.state),weaponBaseCrit=assignedState.player.character.weapons[0].crit;
 const unmodifiedState=clone(assignedState);
 delete unmodifiedState.player.character.talentAssignments.juryRigged;
 delete unmodifiedState.player.character.weapons[0].juryRiggedInstanceId;
 const unmodifiedAttack=new GameController(new RepoWithState(unmodifiedState)).resolveWeaponAttack({weaponIndex:0,difficulty:1,rng:()=>.49});
 assert.equal(juryRiggedWeapon(assignedState.player.character,assignedState.player.character.weapons[0]).weapon.crit,weaponBaseCrit-1);
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(repo),character=restored.state.player.character;
  const owned=character.talentPurchases.find(entry=>entry.nodeId===JURY_RIGGED_POSITION);
  assert.ok(owned,`exact R3C3 source purchase survives reload ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  assert.equal(character.talentAssignments.juryRigged.some(entry=>entry.nodeId===JURY_RIGGED_POSITION),true);
  assert.equal(character.weapons[0].juryRiggedInstanceId,assignedState.player.character.weapons[0].juryRiggedInstanceId);
  const attack=new GameController(repo).resolveWeaponAttack({weaponIndex:0,difficulty:1,rng:()=>.49});
  assert.equal(attack.hit,true);assert.equal(attack.juryRigged,'crit');
  assert.equal(attack.weapon.crit,weaponBaseCrit-1);
  assert.equal(attack.criticalCost,unmodifiedAttack.criticalCost-1,'the real attack reduces the R3C3 critical threshold by one');
 }
 assert.equal(bridge.buyTalent(JURY_RIGGED_POSITION).ok,false,'the exact node cannot be purchased twice');
});

test('AoR p.68 Ace/Gunner R2C4 True Aim buys exact node, upgrades a ranged attack and survives three reloads',()=>{
 const repo=new Repo(),setup=new GameController(repo);
 setup.setCampaignMode('AOR');setup.createAorCharacter({...characterOptions,name:'Tessa Vorn'});
 const weapon=weaponCatalogEntry('blaster_pistol');assert.ok(weapon);
 const armed=clone(setup.state.player.character);
 armed.weapons=[{...clone(weapon),sourceCatalogId:weapon.id,ownedAssetId:'uae-r2c4-blaster',active:true,equipped:true,runtimeState:{}}];
 armed.skills.armas_distancia_ligeras=2;setup.setCharacter(armed);
 const before=clone(repo.state.player.character),bridge=new BraseiroUiBridge({repository:repo});
 const prerequisite=bridge.buyTalent(DEBILITATING_POSITION);
 assert.equal(prerequisite.ok,true,'the exact adjacent R1C4 prerequisite is purchased');
 const candidate=new GameController(repo).eligibleTalents().find(n=>n.id===TRUE_AIM_POSITION);
 assert.ok(candidate);assert.equal(candidate.available,true);assert.equal(candidate.originalName,'DISPARO CERTERO');
 assert.equal(candidate.name,'Disparo certeiro');assert.equal(candidate.cost,10);
 assert.equal(candidate.source.printedPage,68);assert.equal(candidate.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 const treeNode=AOR_TALENT_TREES.ace.gunner.nodes.find(n=>n.id===TRUE_AIM_POSITION);
 assert.ok(treeNode);assert.equal(treeNode.originalName,'DISPARO CERTERO');assert.equal(treeNode.cost,10);assert.equal(treeNode.source.printedPage,68);
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.match(html,/id="trueAimControl" hidden/);assert.match(html,/id="trueAimButton"/);assert.match(html,/id="aimManeuverMethod"/);
 assert.match(ui,/game\.talentRank\('TRUEAIM'\)/);
 assert.match(ui,/game\.aim\(\{mode:'true_aim',maneuverMethod:\$\('aimManeuverMethod'\)\.value\}\)/);
 const purchase=bridge.buyTalent(TRUE_AIM_POSITION);
 assert.equal(purchase.ok,true);assert.equal(purchase.data.talent.id,TRUE_AIM_POSITION);
 assert.equal(purchase.data.character.xp.available,before.xp.available-15);
 const receipt=purchase.data.character.talentPurchases.find(entry=>entry.nodeId===TRUE_AIM_POSITION);
 assert.ok(receipt);assert.equal(receipt.originalName,'DISPARO CERTERO');assert.equal(receipt.source.printedPage,68);
 assert.equal(receipt.source.book,'Star Wars - Age of Rebellion - Core Rulebook');
 assert.equal(receipt.specializationKey,'gunner');assert.equal(receipt.cost,10);

 new GameController(repo).beginCombat();
 const activation=bridge.aim({mode:'true_aim',maneuverMethod:'normal'});
 assert.equal(activation.ok,true);assert.equal(activation.data.combat.pendingAim.mode,'true_aim');
 assert.equal(activation.data.combat.playerTurn.maneuversUsed,1);assert.equal(activation.data.combat.playerTurn.trueAimUsed,true);
 const readyState=clone(repo.state);
 assert.equal(bridge.aim({mode:'true_aim',maneuverMethod:'normal'}).ok,false,'the exact talent can only be used once per round');
 for(let reopen=1;reopen<=3;reopen++){
  const restored=new GameController(new RepoWithState(readyState));assert.equal(validateState(restored.state),true);
  const character=restored.state.player.character,owned=character.talentPurchases.find(entry=>entry.nodeId===TRUE_AIM_POSITION);
  assert.ok(owned,`exact R2C4 source receipt survives reopen ${reopen}`);
  assert.equal(owned.source.printedPage,68);assert.equal(owned.specializationKey,'gunner');
  assert.equal(restored.state.combat.pendingAim.mode,'true_aim');assert.equal(restored.state.combat.playerTurn.trueAimUsed,true);
  const attack=restored.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',rng:()=>.4});
  assert.equal(attack.hit,true);assert.equal(restored.state.lastCheck.trueAimUpgrades,1);
  assert.equal(restored.state.lastCheck.pool.proficiency,2);assert.equal(restored.state.lastCheck.pool.ability,1);
  assert.equal(restored.state.lastCheck.pool.boost,1);
  assert.equal(restored.state.combat.pendingAim,undefined);assert.equal(restored.state.combat.playerTurn.trueAimUsed,true);
 }
 assert.equal(bridge.buyTalent(TRUE_AIM_POSITION).ok,false,'the exact node cannot be purchased twice');
});
