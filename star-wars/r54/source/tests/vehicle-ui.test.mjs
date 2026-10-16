import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('interface possui aba Nave em PT-BR com combate, dano e viagem',()=>{
  for(const text of ['data-view="vehicleView"','Nave','Acelerar +1','Manobras evasivas','Atacar nave inimiga','Controle de danos','Calcular rota hiperespacial']) assert.match(html,new RegExp(text.replace(/[+]/g,'\\+'),'i'));
});

test('interface de nave usa o motor persistente e não duplica regra em HTML estático',()=>{
  for(const call of ['game.addVehicle','game.beginVehicleCombat','game.useVehiclePilotManeuver','game.resolveVehicleAttack','game.vehicleDamageControl','game.plotHyperspaceCourse']) assert.match(js,new RegExp(call.replace('.','\\.')));
});
