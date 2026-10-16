import test from 'node:test';
import assert from 'node:assert/strict';
import { createAdversary, adversarySkillRank, upgradeDifficultyForAdversaryTalent } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

test('soldados de assalto da aventura usam grupo de lacaios com Proteção 5 e perícia por tamanho',()=>{
  const s=createAdversary('stormtrooper',{count:3}); assert.equal(s.kind,'minion'); assert.equal(s.soak,5); assert.equal(s.wounds.threshold,15); assert.equal(adversarySkillRank(s,'armas_distancia_pesadas'),2); assert.equal(s.weapons[0].damage,9);
});

test('oficial de aduana da aventura é rival com Percepção 3 e limiar 12',()=>{
  const o=createAdversary('customs_officer'); assert.equal(o.kind,'rival'); assert.equal(o.wounds.threshold,12); assert.equal(adversarySkillRank(o,'percepcion'),3); assert.equal(o.weapons[0].damage,5);
});

test('Son Barab é antagonista com Tensão, Adversário 1 e Golpes letais 2 registrados',()=>{
  const b=createAdversary('son_barab'); assert.equal(b.kind,'nemesis'); assert.equal(b.strain.threshold,15); assert.equal(upgradeDifficultyForAdversaryTalent(b),1); assert.equal(b.talents.find(t=>t.key==='golpes_letais').rank,2);
});
