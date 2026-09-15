import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeWave3CampaignState } from '../app/src/main/assets/www/core/wave3-runtime.js';
import { serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { PlayerActionService } from '../app/src/main/assets/www/core/action-service.js';
import { DeterministicBarbaraTestAdapter } from '../app/src/main/assets/www/core/barbara-test-adapter.js';

const sceneRules=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/scr015.rules.json',import.meta.url)));
const character=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave3.core-character.fixture.json',import.meta.url)));
function setup(seed='physical-dice-test'){
  const persistence=new MemoryPersistenceAdapter(); const campaignId='physical-dice-campaign';
  const state=makeWave3CampaignState({campaignId,displayName:'Physical Dice',masterSeed:seed,character});
  persistence.createCampaign({campaignId,displayName:'Physical Dice',stateJson:serializeCampaignState(state),masterSeed:seed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'snap:r0'});
  return {persistence,campaignId,service:new PlayerActionService({persistence,sceneRules,barbara:new DeterministicBarbaraTestAdapter()})};
}
function proof(rolls=[6,5]){return Object.freeze({status:'OK',provider:'ANDROID_NATIVE_PHYSICS',algorithm:'physical-dice-top-face-v1',formula:'2d6',rolls,physicalFaces:[...rolls],seed:'123456',physicsMs:4120,forcedSettle:false,visualQuality:'ALTO'});}

test('physical 2d6 faces become the canonical WWN skill-check dice without consuming deterministic RNG',()=>{
  const {persistence,campaignId,service}=setup(); const before=JSON.parse(persistence.loadCampaign(campaignId).stateJson);
  const result=service.perform({campaignId,screenId:'SCR-015',intent:'observe',physicalDice:proof([6,5])});
  assert.equal(result.status,'OK');
  assert.deepEqual(result.mechanicalEvent.resolution.dice.map(d=>d.result),[6,5]);
  assert.equal(result.mechanicalEvent.resolution.rngAlgorithm,'physical-dice-top-face-v1');
  assert.equal(result.mechanicalEvent.diceAuthority.provider,'ANDROID_NATIVE_PHYSICS');
  assert.deepEqual(result.mechanicalEvent.diceAuthority.physicalFaces,[6,5]);
  const after=JSON.parse(persistence.loadCampaign(campaignId).stateJson);
  assert.deepEqual(after.rngState,before.rngState);
});

test('invalid physical face is rejected before campaign mutation',()=>{
  const {persistence,campaignId,service}=setup(); const before=persistence.loadCampaign(campaignId);
  assert.throws(()=>service.perform({campaignId,screenId:'SCR-015',intent:'observe',physicalDice:proof([7,1])}),/Invalid physical 2d6 faces/);
  const after=persistence.loadCampaign(campaignId);
  assert.equal(after.revision,before.revision); assert.equal(after.stateJson,before.stateJson);
});

test('non-physical harness path remains deterministic for regression/replay tooling',()=>{
  const {persistence,campaignId,service}=setup('deterministic-stays');
  const result=service.perform({campaignId,screenId:'SCR-015',intent:'observe'});
  assert.equal(result.status,'OK');
  assert.equal(result.mechanicalEvent.resolution.rngAlgorithm,'splitmix64-counter-v1');
  const after=JSON.parse(persistence.loadCampaign(campaignId).stateJson);
  assert.equal(after.rngState.counters['SCR-015/observe/skill-check'],2);
});
