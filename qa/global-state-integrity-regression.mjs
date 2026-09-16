import assert from 'node:assert/strict';
import {makeEmptyCampaignState} from '../app/src/main/assets/www/core/campaign-state.js';
import {validateCampaignState,repairCampaignState} from '../app/src/main/assets/www/core/campaign-integrity.js';
const s=makeEmptyCampaignState({campaignId:'qa-global',displayName:'QA',masterSeed:'seed'});
s.memoryState={facts:[{factId:'known',summary:'Fato',visibility:'PLAYER_KNOWN'}],threads:{t:{threadId:'t',status:'OPEN',factIds:['known','ghost']}},revealedSecrets:['known','ghost']};
s.scheduleState=[{eventId:'clock',dueMinute:10,summary:'Evento',visibility:'GM_ONLY',completed:false},{eventId:'clock',dueMinute:20,summary:'Duplicado',visibility:'GM_ONLY',completed:false}];
let v=validateCampaignState(s);assert.equal(v.ok,false);assert(v.errors.includes('SCHEDULE_EVENT_ID_DUPLICATED'));assert(v.warnings.some(x=>x.includes('MEMORY_THREAD_DANGLING_FACT')));
const r=repairCampaignState(s);assert.equal(r.validation.ok,true);assert.deepEqual(r.state.memoryState.threads.t.factIds,['known']);assert.deepEqual(r.state.memoryState.revealedSecrets,['known']);assert.equal(r.state.scheduleState.length,1);assert(r.repairs.includes('DUPLICATE_SCHEDULE_EVENTS_REMOVED'));
console.log('global-state-integrity-regression PASS');
