import test from 'node:test';
import assert from 'node:assert/strict';
import { attackDifficultyForWeapon, availableTargetRanges } from '../app/src/main/assets/vtt/js/rules/attackDifficulty.mjs';

const light={skill:'armas_distancia_ligeras',range:'Médio'};
const heavy={skill:'armas_distancia_pesadas',range:'Longo'};
const melee={skill:'armas_cuerpo_a_cuerpo',range:'Interação'};
const artillery={skill:'artilleria',range:'Extremo'};

test('ataque à distância calcula dificuldade pela faixa',()=>{
  assert.equal(attackDifficultyForWeapon(light,'Curto'),1);
  assert.equal(attackDifficultyForWeapon(light,'Médio'),2);
  assert.equal(attackDifficultyForWeapon(heavy,'Longo'),3);
  assert.deepEqual(availableTargetRanges(light),['Interação','Curto','Médio']);
});

test('Interação modifica armas à distância e corpo a corpo usa dificuldade 2',()=>{
  assert.equal(attackDifficultyForWeapon(light,'Interação'),2);
  assert.equal(attackDifficultyForWeapon(heavy,'Interação'),3);
  assert.equal(attackDifficultyForWeapon(melee,'Interação'),2);
  assert.throws(()=>attackDifficultyForWeapon(artillery,'Interação'),/Artilharia/);
});

test('não permite atacar além do alcance da arma',()=>{
  assert.throws(()=>attackDifficultyForWeapon(light,'Longo'),/além do alcance/);
  assert.throws(()=>attackDifficultyForWeapon(melee,'Curto'),/além do alcance|Interação/);
});
