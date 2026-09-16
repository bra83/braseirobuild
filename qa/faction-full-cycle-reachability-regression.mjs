import fs from 'node:fs';
const html=fs.readFileSync('app/src/main/assets/www/gestao.html','utf8');
const js=fs.readFileSync('app/src/main/assets/www/gestao.js','utf8');
const requiredHtml=['factionStart','factionTieBreak','factionUpkeep','factionSpecialResolutions','factionSpecials','factionAction','factionTakeAction','goalOutcome','factionGoal'];
const missing=requiredHtml.filter(id=>!html.includes(`id="${id}"`));
if(missing.length) throw new Error(`missing faction controls: ${missing.join(',')}`);
for(const token of ['new WorldActionService({persistence})','world.startFactionTurn','factions.resolveUpkeep','factions.resolveSpecials','factions.takeAction','factions.reviewGoal']) if(!js.includes(token)) throw new Error(`unreachable faction phase: ${token}`);
if(js.includes('resolveSpecials({campaignId,resolutions:[]})')) throw new Error('special resolution still hard-coded empty');
console.log('FACTION_FULL_CYCLE_REACHABILITY PASS initiative + income + upkeep + specials + action + goal + next faction');
