#!/usr/bin/env node
// Executed against the extracted CANONICAL 4.9.211 Android Studio source.
// This is a read-only runtime inventory; it never changes the game or its saves.
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const file = rel => fs.readFileSync(path.join(root,rel),'utf8');
const required = ['App.tsx','package.json','components/JourneyResolutionPanel.tsx','lib/master/commands.ts','lib/rules/journey.ts','lib/db.ts','lib/version.ts'];
let fail = 0;
for (const rel of required) {
  const ok = fs.existsSync(path.join(root,rel));
  console.log((ok?'PASS':'FAIL')+' CORE_FILE '+rel);
  if (!ok) fail++;
}
if (fail) process.exit(2);
const panel = file('components/JourneyResolutionPanel.tsx');
const commands = file('lib/master/commands.ts');
const db = file('lib/db.ts');
for (const [name,ok] of [
  ['manual-stage-exists',panel.includes('pendingManualRoll')],
  ['player-click-roll',/onClick\s*=\s*\{\(\)\s*=>\s*\{[\s\S]{0,250}(staged\.run|executeRollJourney)/.test(panel)],
  ['roll-command-implemented',commands.includes('player_record_journey_roll')],
  ['finish-journey-command-implemented',commands.includes('player_complete_journey')],
  ['transaction-persistence-available',db.includes('braseiro-db')],
]) {console.log((ok?'PASS':'FAIL')+' CORE_GATE '+name);if(!ok)fail++;}
const snippets = [
  ['JOURNEY_PANEL_ROLL',panel,['pendingManualRoll','const requestRollJourney','requestGenerateEvent','staged.run()']],
  ['JOURNEY_COMMANDS',commands,['player_record_journey_roll','player_confirm_journey_march','player_complete_journey']],
];
for(const [title,src,needles] of snippets){
  const lines = src.split('\n');
  console.log('=== '+title+' ===');
  for(const needle of needles) {
    const idx=lines.findIndex(line=>line.includes(needle));
    console.log('ANCHOR '+JSON.stringify(needle)+' line='+ (idx+1));
    if(idx>=0)console.log(lines.slice(Math.max(0,idx-4),Math.min(lines.length,idx+12)).map((line,i)=>String(Math.max(0,idx-4)+i+1).padStart(4)+' '+line).join('\n'));
  }
}
console.log('BASE_CORE_INVENTORY '+(fail?'FAIL':'PASS')+' errors='+fail);
if(fail)process.exitCode=2;
