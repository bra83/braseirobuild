import fs from 'node:fs';
const html=fs.readFileSync('app/src/main/assets/www/operacoes.html','utf8'),js=fs.readFileSync('app/src/main/assets/www/operacoes.js','utf8'),home=fs.readFileSync('app/src/main/assets/www/index.html','utf8');
for(const id of ['livingRefresh','livingPulse','livingNpc','livingNpcLocation','livingNpcMove','livingNpcMemory','livingNpcRemember','livingNpcPlayerView'])if(!html.includes(`id=\"${id}\"`)||!js.includes(`$('${id}')`))throw new Error(`living world operation unreachable ${id}`);
for(const token of ['NpcWorldService','projectLivingWorldForMaster','npcWorld.move','npcWorld.remember',"visibility:'PLAYER'"])if(!js.includes(token))throw new Error(`missing living integration ${token}`);
if(home.includes('WAVE 1')||home.includes('Runtime incremental WWN'))throw new Error('obsolete development shell still exposed');
console.log('LIVING_WORLD_OPERATIONS_GLOBAL PASS');
