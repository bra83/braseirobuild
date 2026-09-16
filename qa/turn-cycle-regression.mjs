import fs from 'node:fs';
const html=fs.readFileSync('app/src/main/assets/www/jornada.html','utf8');
const js=fs.readFileSync('app/src/main/assets/www/jornada.js','utf8');
const facade=fs.readFileSync('app/src/main/assets/www/core/player-mechanics-facade.js','utf8');
const required=['mechanicsDefenseButton','mechanicsWithdrawButton','mechanicsProneButton','mechanicsStandButton','mechanicsEndSideButton','mechanicsEnemySideButton'];
for(const id of required){if(!html.includes(`id="${id}"`))throw new Error(`missing UI ${id}`);if(!js.includes(`$('${id}')`))throw new Error(`missing binding ${id}`)}
for(const method of ['totalDefense','fightingWithdrawal','goProne','standUp','endPcSide','resolveEnemySide'])if(!facade.includes(`  ${method}(args={})`))throw new Error(`missing facade ${method}`);
console.log('TURN_CYCLE_REGRESSION PASS', required.length, 'new combat controls');
