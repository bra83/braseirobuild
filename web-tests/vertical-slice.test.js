import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeWave3CampaignState } from '../app/src/main/assets/www/core/wave3-runtime.js';
import { serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { PlayerActionService } from '../app/src/main/assets/www/core/action-service.js';
import { DeterministicBarbaraTestAdapter, FailingBarbaraTestAdapter } from '../app/src/main/assets/www/core/barbara-test-adapter.js';
import { gmHelpReadOnly } from '../app/src/main/assets/www/core/gm-help-service.js';

const sceneRules = JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/scr015.rules.json', import.meta.url)));
const character = JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave3.core-character.fixture.json', import.meta.url)));

function setup(seed='vertical-seed') {
  const persistence = new MemoryPersistenceAdapter();
  const campaignId = 'campaign-wave3';
  const state = makeWave3CampaignState({ campaignId, displayName:'Wave 3 Test', masterSeed:seed, character });
  persistence.createCampaign({ campaignId, displayName:'Wave 3 Test', stateJson:serializeCampaignState(state), masterSeed:seed, rngAlgorithm:state.rngState.algorithm, initialSnapshotId:'snap:r0' });
  return { persistence, campaignId };
}

test('full Wave3 observe flow commits mechanics, appends narrative, and reopens without reroll', () => {
  const { persistence, campaignId } = setup();
  const service = new PlayerActionService({ persistence, sceneRules, barbara:new DeterministicBarbaraTestAdapter() });
  const result = service.perform({ campaignId, screenId:'SCR-015', intent:'observe' });
  assert.equal(result.status, 'OK');
  assert.equal(result.receipt.newRevision, 1);
  assert.equal(result.narrativeReceipt.revision, 1);
  const reopened = persistence.loadCampaign(campaignId);
  assert.equal(reopened.status, 'OK');
  assert.equal(reopened.revision, 1);
  const state = JSON.parse(reopened.stateJson);
  assert.equal(state.revision, 1);
  assert.equal(state.rngState.counters['SCR-015/observe/skill-check'], 2);
  assert.equal(state.journalState.lastMechanicalEventId, result.mechanicalEvent.eventId);
  const events = persistence.listEvents(campaignId).events;
  assert.equal(events.length, 2);
  assert.equal(events.filter(e => e.eventKind === 'NARRATIVE').length, 1);
});

test('GM_HELP is read-only and cannot advance revision or RNG', () => {
  const { persistence, campaignId } = setup();
  const before = persistence.loadCampaign(campaignId);
  const beforeState = JSON.parse(before.stateJson);
  const help = gmHelpReadOnly({ state:beforeState, question:'O que esta cena significa?' });
  const after = persistence.loadCampaign(campaignId);
  assert.equal(help.mutatesState, false);
  assert.equal(help.advancesTime, false);
  assert.equal(after.revision, before.revision);
  assert.equal(after.stateJson, before.stateJson);
  assert.equal(persistence.listEvents(campaignId).events.length, 0);
});

test('narrative provider failure happens after mechanical commit and recovery does not reroll', () => {
  const { persistence, campaignId } = setup('crash-seed');
  const failing = new PlayerActionService({ persistence, sceneRules, barbara:new FailingBarbaraTestAdapter() });
  const first = failing.perform({ campaignId, screenId:'SCR-015', intent:'observe' });
  assert.equal(first.status, 'MECHANICAL_COMMITTED_NARRATIVE_PENDING');
  const afterCommit = persistence.loadCampaign(campaignId);
  assert.equal(afterCommit.revision, 1);
  const stateAfterCommit = JSON.parse(afterCommit.stateJson);
  const rngAfterCommit = structuredClone(stateAfterCommit.rngState);
  assert.equal(persistence.listEvents(campaignId).events.length, 1);

  const recovering = new PlayerActionService({ persistence, sceneRules, barbara:new DeterministicBarbaraTestAdapter() });
  const recovered = recovering.recoverPendingNarrative({ campaignId });
  assert.equal(recovered.status, 'RECOVERED');
  const afterRecovery = persistence.loadCampaign(campaignId);
  assert.equal(afterRecovery.revision, 1);
  assert.deepEqual(JSON.parse(afterRecovery.stateJson).rngState, rngAfterCommit);
  assert.equal(afterRecovery.stateJson, afterCommit.stateJson);
  assert.equal(persistence.listEvents(campaignId).events.length, 2);
  assert.equal(recovering.recoverPendingNarrative({ campaignId }).status, 'ALREADY_NARRATED');
});

test('deferred SCR015 actions do not mutate campaign state', () => {
  const { persistence, campaignId } = setup();
  const service = new PlayerActionService({ persistence, sceneRules, barbara:new DeterministicBarbaraTestAdapter() });
  const before = persistence.loadCampaign(campaignId);
  for (const intent of ['approach','ambush','retreat','start_combat']) {
    const result = service.perform({ campaignId, screenId:'SCR-015', intent });
    assert.equal(result.status, 'DEFERRED_NO_MUTATION');
  }
  const after = persistence.loadCampaign(campaignId);
  assert.equal(after.revision, before.revision);
  assert.equal(after.stateJson, before.stateJson);
  assert.equal(persistence.listEvents(campaignId).events.length, 0);
});
