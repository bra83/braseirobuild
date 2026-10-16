import test from 'node:test';
import assert from 'node:assert/strict';
import { validateCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';

const expected={
  zal:{name:'Zal',soak:4,wounds:12,strain:12,credits:300},
  vendri:{name:'Vendri',soak:4,wounds:13,strain:12,credits:75},
  tendaar:{name:'Tendaar',soak:4,wounds:12,strain:13,credits:100},
  'jin-rio':{name:'Jin-Rio',soak:3,wounds:12,strain:12,credits:750},
  cael:{name:'Cael',soak:5,wounds:13,strain:13,credits:50},
  arkhan:{name:'Arkhan',soak:3,wounds:11,strain:14,credits:175}
};

for(const [id,factory] of Object.entries(STARTER_PRESETS)){
  test(`${expected[id].name} gera ficha inicial válida com recursos verificados`,()=>{
    const c=factory(); const e=expected[id];
    assert.equal(validateCharacter(c),true);
    assert.equal(c.name,e.name); assert.equal(c.derived.soak,e.soak);
    assert.equal(c.derived.wounds.threshold,e.wounds); assert.equal(c.derived.strain.threshold,e.strain);
    assert.equal(c.credits,e.credits);
  });
}

test('valores centrais dos perfis iniciais permanecem estáveis',()=>{
  const zal=STARTER_PRESETS.zal(); assert.equal(zal.characteristics.agilidad,4); assert.equal(zal.skills.pilotar,2);
  const cael=STARTER_PRESETS.cael(); assert.equal(cael.characteristics.fortaleza,3); assert.equal(cael.skills.armas_distancia_pesadas,2);
  const arkhan=STARTER_PRESETS.arkhan(); assert.equal(arkhan.characteristics.presencia,3); assert.equal(arkhan.skills.liderazgo,2);
});

test('fixtures locais de regressão são EotE, não oficiais e sem autoridade AoR',()=>{
  for(const [id,factory] of Object.entries(STARTER_PRESETS)){
    const c=factory();
    assert.equal(c.source?.type,'eote_test_fixture',id);
    assert.equal(c.source?.official,false,id);
    assert.equal(c.source?.authority,'NONE',id);
    assert.equal(c.creation?.rulesProfile,'EOTE_CORE',id);
    assert.equal(c.creation?.fixture,true,id);
    assert.equal(c.creation?.official,false,id);
    assert.equal(c.creation?.fixtureId,id,id);
    assert.equal(c.duties,undefined,id);
    assert.equal(c.groupContributionRank,undefined,id);
    assert.equal(c.obligations.length,0,id);
    assert.doesNotMatch(JSON.stringify(c),/Age of Rebellion|La Era de la Rebelión|Duty|Contribution Rank/i,id);
  }
});
