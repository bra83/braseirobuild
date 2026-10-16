import test from 'node:test';
import assert from 'node:assert/strict';
import { createBlankCharacter, validateCharacter, exportCharacterJson, importCharacterJson } from '../app/src/main/assets/vtt/js/rules/character.mjs';

test('ficha estrutural vazia respeita seis características e habilidades conhecidas',()=>{
  const c=createBlankCharacter('Rin'); assert.equal(validateCharacter(c),true); assert.equal(Object.keys(c.characteristics).length,6); assert.ok(Object.keys(c.skills).length>=30);
});

test('importação rejeita característica fora da faixa 1-6 sem substituir ficha válida',()=>{
  const good=createBlankCharacter('Rin'); const bad=JSON.parse(exportCharacterJson(good)); bad.characteristics.agilidad=7;
  assert.throws(()=>importCharacterJson(JSON.stringify(bad)),/entre 1 e 6/); assert.equal(good.characteristics.agilidad,2);
});

test('ficha JSON faz ida e volta',()=>{
  const c=createBlankCharacter('Rin'); c.skills.pilotar_vehiculo_espacial=3; const r=importCharacterJson(exportCharacterJson(c)); assert.equal(r.skills.pilotar_vehiculo_espacial,3);
});
