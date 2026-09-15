import fs from 'node:fs';
import assert from 'node:assert/strict';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeWave3CampaignState } from '../app/src/main/assets/www/core/wave3-runtime.js';
import { serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { PlayerActionService } from '../app/src/main/assets/www/core/action-service.js';
import { DeterministicBarbaraTestAdapter } from '../app/src/main/assets/www/core/barbara-test-adapter.js';
const sceneRules=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/scr015.rules.json',import.meta.url)));
const character=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave3.core-character.fixture.json',import.meta.url)));
function setup(id){const persistence=new MemoryPersistenceAdapter();const state=makeWave3CampaignState({campaignId:id,displayName:'QA',masterSeed:'post071-'+id,character});persistence.createCampaign({campaignId:id,displayName:'QA',stateJson:serializeCampaignState(state),masterSeed:'post071-'+id,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'snap:r0'});return {persistence,service:new PlayerActionService({persistence,sceneRules,barbara:new DeterministicBarbaraTestAdapter()}),campaignId:id};}
const phys=(formula,rolls)=>Object.freeze({status:'OK',provider:'ANDROID_NATIVE_PHYSICS',algorithm:'physical-dice-top-face-v1',formula,rolls:Object.freeze(rolls),physicalFaces:Object.freeze([...rolls]),seed:'42',physicsMs:400,forcedSettle:false,visualQuality:'ALTO'});
{
 const {persistence,service,campaignId}=setup('observe');const before=JSON.parse(persistence.loadCampaign(campaignId).stateJson);const r=service.perform({campaignId,screenId:'SCR-015',intent:'observe',physicalDice:phys('2d6',[6,5])});assert.equal(r.status,'OK');assert.equal(r.mechanicalEvent.kind,'PLAYER_ACTION_SKILL_CHECK');assert.deepEqual(JSON.parse(persistence.loadCampaign(campaignId).stateJson).rngState,before.rngState);
}
{
 const {service,campaignId}=setup('approach');const r=service.perform({campaignId,screenId:'SCR-015',intent:'approach',physicalDice:phys('2d6',[4,4])});assert.equal(r.status,'OK');assert.equal(r.projectedState.encounterState.foesAware,true);assert.equal(r.mechanicalEvent.kind,'PLAYER_ACTION_REACTION_ROLL');
}
{
 const {service,campaignId}=setup('ambush');const r=service.perform({campaignId,screenId:'SCR-015',intent:'ambush',physicalDice:phys('4d6',[6,6,1,1])});assert.equal(r.status,'OK');assert.equal(r.projectedState.encounterState.ambushPrepared,true);assert.equal(r.mechanicalEvent.kind,'PLAYER_ACTION_OPPOSED_AMBUSH');
}
{
 const {persistence,service,campaignId}=setup('retreat-safe');const before=JSON.parse(persistence.loadCampaign(campaignId).stateJson);const r=service.perform({campaignId,screenId:'SCR-015',intent:'retreat'});assert.equal(r.status,'OK');assert.equal(r.projectedState.encounterState.retreated,true);assert.equal(r.mechanicalEvent.resolution.rngAlgorithm,'NO_ROLL_REQUIRED');assert.deepEqual(JSON.parse(persistence.loadCampaign(campaignId).stateJson).rngState,before.rngState);
}
{
 const {service,campaignId}=setup('retreat-pursued');const a=service.perform({campaignId,screenId:'SCR-015',intent:'approach',physicalDice:phys('2d6',[4,4])});assert.equal(a.status,'OK');const r=service.perform({campaignId,screenId:'SCR-015',intent:'retreat',physicalDice:phys('4d6',[6,6,1,1])});assert.equal(r.status,'OK');assert.equal(r.projectedState.encounterState.retreated,true);assert.equal(r.mechanicalEvent.kind,'PLAYER_ACTION_RETREAT');
}
{
 const {persistence,service,campaignId}=setup('combat-route');const before=persistence.loadCampaign(campaignId);const r=service.perform({campaignId,screenId:'SCR-015',intent:'start_combat'});const after=persistence.loadCampaign(campaignId);assert.equal(r.status,'ROUTED_TO_COMBAT_SESSION_NO_MUTATION');assert.equal(after.revision,before.revision);assert.equal(after.stateJson,before.stateJson);
}
console.log('POST071_ENCOUNTER_ACTIONS_PASS observe approach ambush retreat-safe retreat-pursued start-combat-route');
