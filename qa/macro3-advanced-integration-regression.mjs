import fs from 'node:fs';
const html=fs.readFileSync('app/src/main/assets/www/gestao.html','utf8');
const js=fs.readFileSync('app/src/main/assets/www/gestao.js','utf8');
for(const id of ['advancedMagicState','establishLab','startResearch','resolveResearch','startWorking','completeWorking','startMagicItem','resolveMagicItem','specialCombatState','holdAction','screenAlly','shoveTarget','grappleTarget','grappleMove','dualWield','snapAttack','swarmAttack']){if(!html.includes(`id="${id}"`))throw new Error(`missing UI ${id}`);if(!js.includes(`$('${id}')`))throw new Error(`missing binding ${id}`)}
for(const svc of ['AdvancedMagicService','AdvancedMagicResolutionService','SpecialCombatService'])if(!js.includes(`new ${svc}`))throw new Error(`service not instantiated ${svc}`);
for(const method of ['establishResearchLab','startSpellResearch','resolveSpellResearch','startWorking','completeWorking','startMagicItemProject','resolveMagicItemProject','hold','screen','shove','grappleStart','grappleMove','dualWield','snapAttack','swarmAttack'])if(!js.includes(`.${method}(`))throw new Error(`method not wired ${method}`);
console.log('MACRO3_ADVANCED_INTEGRATION PASS');
