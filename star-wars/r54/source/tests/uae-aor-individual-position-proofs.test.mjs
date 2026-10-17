import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {getHealthStatus} from '../app/src/main/assets/vtt/js/rules/health.mjs';

const POSITION='ace:gunner:r1c2';
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
