import test from 'node:test';
import assert from 'node:assert/strict';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {AOR_IMMEDIATE_TALENT_EFFECTS,AOR_RUNTIME_MECHANICAL_TALENTS,aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {braceEnvironmentalSetbackRelief,purchaseTalent,sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {createAorCoreCharacter} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';

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
