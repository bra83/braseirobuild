import assert from 'node:assert/strict';
import { oncePerPendingAction } from './lib/rules/oncePerPendingAction';

let rolls = 0;
const rollOnce = oncePerPendingAction(async () => {
  rolls++;
  await Promise.resolve();
  return 'dice-outcome';
});
const results=await Promise.all([rollOnce(),rollOnce(),rollOnce()]);
assert.deepEqual(results,['dice-outcome','dice-outcome','dice-outcome']);
assert.equal(rolls,1,'one pending roll dispatches once even on repeated taps');
assert.equal(await rollOnce(),'dice-outcome');
assert.equal(rolls,1,'a previously dispatched roll never silently rerolls');
console.log('PASS base-manual-dice-once');

let events=0;
const eventOnce=oncePerPendingAction(async () => {events++;return 'event-dice';});
const outputs=await Promise.all([eventOnce(),eventOnce()]);
assert.deepEqual(outputs,['event-dice','event-dice']);
assert.equal(events,1);
console.log('PASS base-manual-event-once');

let failedRuns=0;
const failOnce=oncePerPendingAction(async () => {failedRuns++;throw new Error('mock dispatcher failure');});
for(let i=0;i<2;i++) {
  await assert.rejects(failOnce(),/mock dispatcher failure/);
}
assert.equal(failedRuns,1,'failed submission is not repeated silently');
console.log('PASS no-silent-redraw-on-failure');

let newActions=0;
await oncePerPendingAction(async()=>{newActions++;})();
await oncePerPendingAction(async()=>{newActions++;})();
assert.equal(newActions,2,'separately selected player actions remain independent');
console.log('PASS separate-choices-still-allowed');
console.log('OUA_BASE_MANUAL_ROLL_TEST_PASS count=4');
