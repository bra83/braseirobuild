import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function seq(values, fallback=0){let i=0;return()=>values[i++]??fallback}
const manySuccess=()=>0.4;
const successAdv=()=>{const vals=[.999,.8,.8,.8,.8,0,0,0];return seq(vals,0)};
function game(key='base'){
 const g=new GameController(new LocalStorageRepository(new MemoryStorage(),`p103-${key}`));g.loadStarterCharacter('tendaar');
 const c=structuredClone(g.state.player.character);for(const k of Object.keys(c.characteristics))c.characteristics[k]=5;for(const k of ['atletismo','mecanica','disciplina','liderazgo','percepcion','computadoras','artilleria'])c.skills[k]=5;c.gear=[...(c.gear||[]),{id:'tool_kit',name:'Kit de ferramentas',quantity:1}];g.commit('CHARACTER_UPDATED',{character:c});
 g.addVehicle('skipray_gat12h');g.beginVehicleCombat({targetProfileKey:'firespray',range:'Curto'});return g;
}
test('P103 Controle de Danos separa Tensão e Casco e limita tentativa de Casco por encontro',()=>{
 const g=game('dc');let v=structuredClone(g.state.vehicles),id=v.activeVehicleId;v.vehicles[id].systemStrain=6;v.vehicles[id].hullDamage=6;g.commit('VEHICLES_UPDATED',{vehicles:v});
 let r=g.vehicleDamageControl({mode:'system',rng:manySuccess});assert.ok(r.systemStrainRecovered>0);assert.equal(g.state.vehicles.vehicles[id].hullDamage,6);
 g.nextVehicleRound();r=g.vehicleDamageControl({mode:'hull',gmAllowed:true,rng:manySuccess});assert.ok(r.hullRecovered>0);const after=g.state.vehicles.vehicles[id].hullDamage;
 g.nextVehicleRound();assert.throws(()=>g.vehicleDamageControl({mode:'hull',gmAllowed:true,rng:manySuccess}),/uma vez por encontro/i);assert.equal(g.state.vehicles.vehicles[id].hullDamage,after);
});
test('P103 Reparo Manual exige permissão contextual e Kit, usa Atletismo e compartilha limite do Casco',()=>{
 const g=game('manual');let v=structuredClone(g.state.vehicles),id=v.activeVehicleId;v.vehicles[id].hullDamage=5;g.commit('VEHICLES_UPDATED',{vehicles:v});
 assert.throws(()=>g.useVehicleCrewAction('manual_repairs',{rng:manySuccess}),/confirmação/i);
 const r=g.useVehicleCrewAction('manual_repairs',{gmAllowed:true,rng:manySuccess});assert.equal(r.skillKey,'atletismo');assert.ok(r.effect.hullRecovered>=1);
 g.nextVehicleRound();assert.throws(()=>g.vehicleDamageControl({mode:'hull',gmAllowed:true,rng:manySuccess}),/uma vez por encontro/i);
});
test('P103 Escanear só revela níveis atuais quando 2 Vantagens são escolhidas e disponíveis',()=>{
 const g=game('scan');const r=g.useVehicleCrewAction('scan_enemy',{spendAdvantage:true,rng:successAdv()});assert.equal(r.success,true);assert.equal(r.effect.advantageSpent,2);assert.ok('hullDamage' in r.effect);assert.ok('systemStrain' in r.effect);
});
test('P103 Fatiar pode gastar Vantagens em Tensão e Triunfo para desativar arma por um assalto',()=>{
 const g=game('slice');const tid=g.state.vehicles.combat.targetVehicleId,before=g.state.vehicles.vehicles[tid].systemStrain;const r=g.useVehicleCrewAction('slice_enemy',{spendAdvantage:true,spendTriumph:true,weaponIndex:0,rng:successAdv()});assert.equal(r.success,true);assert.equal(r.effect.advantageSpent,2);assert.equal(r.effect.triumphSpent,1);assert.equal(g.state.vehicles.vehicles[tid].systemStrain,before+1);assert.equal(g.state.vehicles.vehicles[tid].weapons[0].runtimeState.disabledUntilRound,1);
});
test('P103 Disciplina de Fogo usa um Boost base + um por cada dois Êxitos adicionais',()=>{
 const g=game('fire');const r=g.useVehicleCrewAction('fire_discipline',{rng:manySuccess});const successes=Math.max(1,Number(r.check.netSuccess));assert.equal(r.effect.boostedAttacks,1+Math.floor((successes-1)/2));
});
test('P103 Despistar Mísseis ganha upgrade adicional por cada dois Êxitos adicionais',()=>{
 const g=game('spoof');const r=g.useVehicleCrewAction('spoof_missiles',{rng:manySuccess});const successes=Math.max(1,Number(r.check.netSuccess));assert.equal(r.effect.difficultyUpgrades,1+Math.floor((successes-1)/2));
});
test('P103 R46 expõe modos de Controle de Danos, Reparo Manual e gastos opcionais',()=>{
 const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');assert.match(runtime,/RECUPERAR TENSÃO DE SISTEMAS/);assert.match(runtime,/REPARAR TRAUMA DE CASCO/);assert.match(runtime,/data-p102-act="manual_repairs"/);assert.match(runtime,/data-p103-spend-adv/);assert.match(runtime,/data-p103-spend-tri/);
});
