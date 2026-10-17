import test from 'node:test';
import assert from 'node:assert/strict';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {AOR_IMMEDIATE_TALENT_EFFECTS,AOR_RUNTIME_MECHANICAL_TALENTS,aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {braceEnvironmentalSetbackRelief,purchaseTalent,sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {createAorCoreCharacter} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {readFileSync} from 'node:fs';

const gunner=AOR_TALENT_TREES.ace.gunner;
const braceNodes=['ace:gunner:r2c2','ace:gunner:r4c3'];
const makeGunner=()=>createAorCoreCharacter({name:'Test Gunner',speciesKey:'human',careerKey:'ace',specializationKey:'gunner',partySize:1,
  careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['disciplina','artilleria'],
  speciesChoiceSkills:['atletismo','medicina'],motivation:'Proteger a equipe.',dutyType:'Vitória em combate'});
function character(...nodeIds){
  return {creation:{rulesProfile:'AOR_CORE'},talentPurchases:nodeIds.map(nodeId=>{
    const node=gunner.nodes.find(entry=>entry.id===nodeId);
    return {nodeId,originalName:node.originalName};
  })};
}

test('AoR source FIRMEZA positions are Brace, not immediate Grit',()=>{
  assert.deepEqual(gunner.nodes.filter(node=>node.originalName==='FIRMEZA').map(node=>node.id),braceNodes);
  assert.ok(gunner.nodes.every(node=>node.source.printedPage===68));
  assert.equal(aorCanonicalTalentName('FIRMEZA'),'BRACE');
  assert.ok(!AOR_IMMEDIATE_TALENT_EFFECTS.includes('FIRMEZA'));
  assert.ok(!AOR_RUNTIME_MECHANICAL_TALENTS.includes('FIRMEZA'));
  assert.equal(sourceBackedTalentRank(character(braceNodes[0]),'BRACE'),1);
  assert.equal(sourceBackedTalentRank(character(...braceNodes),'BRACE'),2);
  assert.equal(sourceBackedTalentRank({creation:{rulesProfile:'AOR_CORE'},talents:['Firmeza']},'BRACE'),0);
});

test('buying Gunner Brace preserves p.68 node provenance without granting Grit threshold',()=>{
  let c=makeGunner();c.xp.available=100;
  const strainThreshold=c.derived.strain.threshold;
  c=purchaseTalent(c,'ace:gunner:r1c1').character;
  c=purchaseTalent(c,'ace:gunner:r2c1').character;
  const bought=purchaseTalent(c,braceNodes[0]);
  const record=bought.character.talentPurchases.find(entry=>entry.nodeId===braceNodes[0]);
  assert.equal(record.originalName,'FIRMEZA');
  assert.equal(record.source.printedPage,68);
  assert.equal(bought.character.derived.strain.threshold,strainThreshold);
  assert.equal(sourceBackedTalentRank(bought.character,'BRACE'),1);
});

test('AoR Brace relief is activated, rank-limited, and applies only to environmental setback',()=>{
  const one=character(braceNodes[0]);
  assert.deepEqual(braceEnvironmentalSetbackRelief(one,{environmentalSetback:2}),{
    available:true,rank:1,maneuverPrepared:false,removedSetback:0,remainingEnvironmentalSetback:2
  });
  assert.deepEqual(braceEnvironmentalSetbackRelief(one,{maneuverPrepared:true,environmentalSetback:2}),{
    available:true,rank:1,maneuverPrepared:true,removedSetback:1,remainingEnvironmentalSetback:1
  });
  const two=character(...braceNodes);
  assert.deepEqual(braceEnvironmentalSetbackRelief(two,{maneuverPrepared:true,environmentalSetback:1}),{
    available:true,rank:2,maneuverPrepared:true,removedSetback:1,remainingEnvironmentalSetback:0
  });
  assert.throws(()=>braceEnvironmentalSetbackRelief(one,{maneuverPrepared:true,environmentalSetback:-1}),/Brace inválido/);
});

class Repo{constructor(){this.state=createCampaign('AoR Brace');this.saves=0;}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++;}}
function controllerWithBrace({weapon=false}={}){
 const repo=new Repo(),g=new GameController(repo);let c=makeGunner();c.xp.available=100;
 c=purchaseTalent(c,'ace:gunner:r1c1').character;c=purchaseTalent(c,'ace:gunner:r2c1').character;c=purchaseTalent(c,braceNodes[0]).character;
 c.skills.artilleria=1;if(weapon){c.weapons=[{id:'brace-test-blaster',name:'Brace test blaster',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Curto',encumbrance:0,qualities:[],active:true,equipped:true}];c.skills.armas_distancia_ligeras=1;}
 g.setCharacter(c);g.setCampaignMode('AOR');g.beginCombat();return {repo,g};
}
test('p.68 Gunner Brace maneuver persists, affects the next skill action only, and is wired to bridge/UI',()=>{
 const {repo}=controllerWithBrace();const bridge=new BraseiroUiBridge({repository:repo});
 const bridgeResult=bridge.prepareAoRBrace(),prepared=bridgeResult.data;
 assert.equal(bridgeResult.ok,true);assert.equal(bridge.aorBraceStatus().data.prepared,true);
 assert.equal(prepared.rank,1);assert.equal(prepared.combat.playerTurn.maneuversUsed,1);assert.deepEqual(prepared.source,{productCode:'AOR',printedPage:68,nodeIds:[braceNodes[0]]});
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.state.combat.aorBracePrepared.rank,1);}
 const reopened=new GameController(repo),check=reopened.resolveSkillCheck({skillKey:'artilleria',difficulty:2,setback:3,braceEnvironmentalSetback:2,rng:()=>0});
 assert.equal(check.pool.setback,2);assert.deepEqual(check.aorBrace,{rank:1,environmentalSetback:2,removedSetback:1,source:prepared.source});
 assert.equal(reopened.state.combat.aorBracePrepared,undefined);
 for(let i=0;i<3;i++)assert.equal(new GameController(repo).state.combat.aorBracePrepared,undefined);
 const html=readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const app=readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 assert.match(html,/id="braceButton"/);assert.match(html,/id="braceEnvironmentalSetback"/);assert.match(app,/prepareAoRBrace/);
});
test('p.68 Gunner Brace is consumed by the next weapon attack and cannot remove non-environmental setback',()=>{
 const {g}=controllerWithBrace({weapon:true});g.prepareAoRBrace();
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',setback:3,braceEnvironmentalSetback:2,rng:()=>0});
 assert.equal(attack.check.pool.setback,2);assert.equal(attack.check.aorBrace.removedSetback,1);
 assert.equal(g.state.combat.aorBracePrepared,undefined);
 const noPrep=controllerWithBrace({weapon:true}).g;
 assert.throws(()=>noPrep.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',setback:2,braceEnvironmentalSetback:3,rng:()=>0}),/Brace exige/);
 assert.equal(noPrep.state.combat.playerTurn.actionUsed,false);
});
test('p.68 Gunner Brace expires after the next action when no environmental setback applies',()=>{
 const {g}=controllerWithBrace();g.prepareAoRBrace();
 const check=g.resolveSkillCheck({skillKey:'artilleria',difficulty:2,setback:2,braceEnvironmentalSetback:0,rng:()=>0});
 assert.equal(check.pool.setback,2);assert.equal(check.aorBrace.removedSetback,0);assert.equal(g.state.combat.aorBracePrepared,undefined);
});
