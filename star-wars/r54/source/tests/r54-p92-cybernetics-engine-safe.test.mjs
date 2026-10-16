import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';

class MemoryRepository { constructor(state=createCampaign('P92 Cyber')){this.state=structuredClone(state)} load(){return structuredClone(this.state)} save(s){this.state=structuredClone(s)} }

test('P92: cibernético altera ficha, respeita persistência e remoção reverte efeito',()=>{
  const repo=new MemoryRepository(),g=new GameController(repo);g.loadStarterCharacter('cael');
  const soak=Number(g.state.player.character.derived.soak||0);
  const installed=g.installCybernetic('cybernetic_subdermal_armor');
  assert.equal(installed.summary.installed.length,1);
  assert.equal(g.state.player.character.derived.soak,soak+1);
  const reopened=new GameController(repo);
  assert.equal(reopened.cyberneticsSummary().installed.length,1);
  reopened.removeCybernetic('cybernetic_subdermal_armor');
  assert.equal(reopened.cyberneticsSummary().installed.length,0);
  assert.equal(reopened.state.player.character.derived.soak,soak);
});

test('P92: limite de cibernéticos por Fortaleza é realmente bloqueante',()=>{
  const g=new GameController(new MemoryRepository());g.loadStarterCharacter('cael');
  assert.equal(g.cyberneticsSummary().cap,3);
  g.installCybernetic('prosthetic_arm_basic');
  g.installCybernetic('prosthetic_arm_basic');
  g.installCybernetic('prosthetic_arm_basic');
  assert.throws(()=>g.installCybernetic('prosthetic_leg_basic'),/Limite de implantes/);
});

test('P92: bridge público não concede veículo nem instala cibernético sem aquisição',()=>{
  const bridge=fs.readFileSync('app/src/main/assets/vtt/js/core/uiBridge.mjs','utf8');
  assert.equal(/\n\s*installCybernetic\(itemId\)\s*\{/.test(bridge),false);
  assert.equal(/\n\s*removeCybernetic\(itemId\)\s*\{/.test(bridge),false);
  assert.equal(/\n\s*addVehicle\(profileKey/.test(bridge),false);
  assert.ok(bridge.includes('cyberneticsSummary()'));
});
