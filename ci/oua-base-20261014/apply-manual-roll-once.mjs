#!/usr/bin/env node
// Applies one small behavioral correction to the EXACT OUA 4.9.211 source tree.
// This prevents rapid/double taps from invoking one selected Journey roll/event
// more than once. No books, supplements, saves or DB schemas are touched.
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const panelPath = path.join(root,'components','JourneyResolutionPanel.tsx');
if (!fs.existsSync(panelPath)) throw new Error('JOURNEY_PANEL_NOT_FOUND');
let panel=fs.readFileSync(panelPath,'utf8');
const imported = "import { oncePerPendingAction } from '../lib/rules/oncePerPendingAction';";
if (!panel.includes(imported)) panel = imported + '\n' + panel;

const patches=[
  ['run: () => executeRollJourney(...args),','run: oncePerPendingAction(() => executeRollJourney(...args)),'],
  ['run: () => executeGenerateEvent(...args),','run: oncePerPendingAction(() => executeGenerateEvent(...args)),'],
];
for (const [before,after] of patches) {
  if (panel.includes(after)) continue;
  if (!panel.includes(before)) throw new Error('BASE_ROLL_GATE_ANCHOR_MISSING: '+before);
  panel=panel.replace(before,after);
}
if ((panel.match(/oncePerPendingAction\(\(\) => execute/g)||[]).length!==2) throw new Error('BASE_ROLL_GATE_WRAPPERS_INVALID');
fs.writeFileSync(panelPath,panel,'utf8');

const helperPath=path.join(root,'lib','rules','oncePerPendingAction.ts');
fs.writeFileSync(helperPath,`/**
 * Guard for an explicitly selected/manual dice action.
 * Once the player presses Rolar, all subsequent activations of that same
 * pending button share a single promise. There is never a second dice draw
 * or duplicate dispatch due to impatient/double tapping.
 * A NEW user-selected action gets a NEW wrapper.
 */
export function oncePerPendingAction<T>(action: () => Promise<T>): () => Promise<T> {
  let inFlightOrCompleted: Promise<T> | undefined;
  return () => {
    if (!inFlightOrCompleted) {
      inFlightOrCompleted = Promise.resolve().then(action);
    }
    return inFlightOrCompleted;
  };
}
`,'utf8');
console.log('OUA_BASE_MANUAL_ROLL_SINGLE_FLIGHT_APPLIED');
