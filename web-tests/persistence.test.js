import test from 'node:test';
import assert from 'node:assert/strict';
import { MemoryPersistenceAdapter, LoadStatus } from '../app/src/main/assets/www/core/persistence-port.js';

function fixture(adapter) {
  adapter.createCampaign({
    campaignId: 'c1', displayName: 'Teste', stateJson: '{"revision":0}', masterSeed: 'seed',
    rngAlgorithm: 'splitmix64-counter-v1', initialSnapshotId: 's0',
  });
}

test('create -> load is explicit OK', () => {
  const p = new MemoryPersistenceAdapter(); fixture(p);
  const load = p.loadCampaign('c1');
  assert.equal(load.status, LoadStatus.OK);
  assert.equal(load.revision, 0);
});

test('missing is NOT_FOUND and not an empty campaign', () => {
  const p = new MemoryPersistenceAdapter();
  assert.deepEqual(p.loadCampaign('missing'), { status: LoadStatus.NOT_FOUND });
});

test('corruption is CORRUPT and never NOT_FOUND', () => {
  const p = new MemoryPersistenceAdapter(); fixture(p);
  p.corruptCampaignForTest('c1');
  const load = p.loadCampaign('c1');
  assert.equal(load.status, LoadStatus.CORRUPT);
});

test('revision conflict does not mutate state', () => {
  const p = new MemoryPersistenceAdapter(); fixture(p);
  const result = p.commit({ campaignId:'c1', expectedRevision:9, eventId:'e1', eventPayloadJson:'{}', newStateJson:'{"revision":1}' });
  assert.equal(result.status, 'REVISION_CONFLICT');
  assert.equal(p.loadCampaign('c1').revision, 0);
});

test('injected failure rolls back staged event/state', () => {
  const p = new MemoryPersistenceAdapter(); fixture(p);
  p.failStage = 'after-event';
  assert.throws(() => p.commit({ campaignId:'c1', expectedRevision:0, eventId:'e1', eventPayloadJson:'{}', newStateJson:'{"revision":1}' }));
  const load = p.loadCampaign('c1');
  assert.equal(load.revision, 0);
  assert.equal(load.events.length, 0);
});
