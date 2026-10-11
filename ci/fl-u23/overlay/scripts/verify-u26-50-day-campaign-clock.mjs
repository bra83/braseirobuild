/**
 * U26 G05 -- protect period clock from malformed saves and run a REAL
 * fifty-day IndexedDB campaign update (not a UI or physical Android test).
 */
import assert from 'node:assert/strict';
import 'fake-indexeddb/auto';
import {getDb,invalidateDbConnection} from '../src/db/db.ts';
import {CampaignService} from '../src/services/CampaignService.ts';
import {WorldStateService} from '../src/services/WorldStateService.ts';

const sample={...CampaignService.create(),id:'u26-world-50-day',
  currentDay:1,currentPeriod:'Morning',currentHexId:'test-hex',
  partyCharacterIds:[],updatedAt:Date.now()};
let pass=0;
const check=(actual,expected,label)=>{assert.deepEqual(actual,expected,label);pass++};
const end=CampaignService.advanceState(sample,200);
check(end.currentDay,51,'fifty days = 200 quarter day ticks');
check(end.currentPeriod,'Morning','full days preserve morning');
check(CampaignService.advanceState(sample,1).currentPeriod,'Day','first quarter');
check(CampaignService.advanceState(sample,4).currentDay,2,'day rollover');
check(CampaignService.advanceState(sample,1.2).currentPeriod,'Evening','fractional input original behavior matches ceil');
check(CampaignService.advanceState(sample,-1).currentDay,1,'negative periods remain no-op');
for (const invalid of [NaN,Infinity,-Infinity,Number.MAX_VALUE]) {
  assert.throws(()=>CampaignService.advanceState(sample,invalid),/Período inválido/,String(invalid));pass++;
  await assert.rejects(()=>CampaignService.advancePeriods(invalid),/Avanço de tempo inválido/,String(invalid));pass++;
}
await assert.rejects(()=>CampaignService.advancePeriods(20001),/Avanço de tempo inválido/);pass++;
const db=await getDb();
await db.put('campaign',sample);
await db.put('app_meta',{id:'activeCampaignId',value:sample.id,updatedAt:Date.now()});
await db.put('world_state',WorldStateService.empty(sample.id));
let result;
try {
  result=await CampaignService.advancePeriods(200);
} catch (error) {
  throw new Error('The REAL fifty-day campaign advancement failed: '+String(error));
}
check(result?.currentDay,51,'real 50-day campaign advancement persisted day');
check(result?.currentPeriod,'Morning','real 50-day campaign advancement persisted phase');
const beforeWorld=await db.get('world_state',sample.id);
assert.ok(beforeWorld?.id===sample.id&&beforeWorld.environment&&beforeWorld.livingWorld,
  'living world and environment were persisted on 50-day advancement');pass++;
db.close();invalidateDbConnection();
const reopened=await getDb();
const stored=await reopened.get('campaign',sample.id);
check(stored.currentDay,51,'camp day durable after connection reopen');
check(stored.currentPeriod,'Morning','clock phase durable after reopen');
const afterWorld=await reopened.get('world_state',sample.id);
check(afterWorld.id,sample.id,'world identity durable after reopen');
assert.ok(Array.isArray(afterWorld.appliedOperationIds),'world operation deduplication retained');pass++;
reopened.close();invalidateDbConnection();
console.log('U26_G05_50_DAY_CAMPAIGN_CLOCK PASS '+pass+'/'+pass+
  ' (actual campaign transaction, world/environment reconcile and IndexedDB reopen; Android not tested)');
