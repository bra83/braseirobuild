import test from 'node:test';
import assert from 'node:assert/strict';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {AOR_IMMEDIATE_TALENT_EFFECTS,AOR_RUNTIME_MECHANICAL_TALENTS,aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {braceEnvironmentalSetbackRelief,sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';

const gunner=AOR_TALENT_TREES.ace.gunner;
const braceNodes=['ace:gunner:r2c2','ace:gunner:r4c3'];
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
