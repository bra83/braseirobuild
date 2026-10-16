import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { resolveWeaponHit } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';

test('ataque bem-sucedido soma Êxitos líquidos ao dano da arma',()=>{
  const weapon=STARTER_PRESETS.arkhan().weapons[0];
  const r=resolveWeaponHit(weapon,{success:true,netSuccess:2,netAdvantage:1});
  assert.equal(r.damage,9);
  assert.equal(r.criticalAvailable,false);
});

test('Vantagens suficientes disponibilizam crítico e Explosão conforme a arma',()=>{
  const frag=STARTER_PRESETS.zal().weapons[1];
  const r=resolveWeaponHit(frag,{success:true,netSuccess:1,netAdvantage:4});
  assert.equal(r.criticalAvailable,true);
  assert.equal(r.blastAvailable,true);
  assert.equal(r.blastDamage,7);
});

test('granada atordoante produz dano de Tensão',()=>{
  const stun=STARTER_PRESETS.vendri().weapons[2];
  const r=resolveWeaponHit(stun,{success:true,netSuccess:1,netAdvantage:0});
  assert.equal(r.damageType,'strain');
  assert.equal(r.damage,9);
});

test('Triunfo pode disponibilizar crítico mesmo sem Vantagens suficientes',()=>{
  const weapon=STARTER_PRESETS.arkhan().weapons[0];
  const r=resolveWeaponHit(weapon,{success:true,netSuccess:1,netAdvantage:0,triumph:1});
  assert.equal(r.criticalAvailable,true);
  assert.equal(r.criticalByTriumph,true);
});
