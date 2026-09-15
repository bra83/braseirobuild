import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeWave3CampaignState } from '../app/src/main/assets/www/core/wave3-runtime.js';
import { serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { PlayerActionService } from '../app/src/main/assets/www/core/action-service.js';
import { DeterministicBarbaraTestAdapter } from '../app/src/main/assets/www/core/barbara-test-adapter.js';

const iria=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
const sceneRules={screenId:'SCR-015',actions:{observe:{status:'WAVE3_IMPLEMENTED',resolution:'SKILL_CHECK',skill:'Notice',attribute:'Wisdom',difficulty:8,situationalModifier:0,rngStream:'scr015/observe',classification:'CANONICAL_RULE',sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.3.0 Skill Checks'}]}}};
function setup(character=iria){
  const persistence=new MemoryPersistenceAdapter();const campaignId='masterful-wave9';const state=makeWave3CampaignState({campaignId,displayName:'Masterful',masterSeed:'wave9-masterful-seed',character});
  persistence.createCampaign({campaignId,displayName:'Masterful',stateJson:serializeCampaignState(state),masterSeed:'wave9-masterful-seed',rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'snapshot:r0'});
  const service=new PlayerActionService({persistence,sceneRules,barbara:new DeterministicBarbaraTestAdapter()});return{persistence,campaignId,service};
}

test('Full Expert can use Masterful Expertise only after an eligible non-combat skill check',()=>{
  const {service,campaignId}=setup();
  const before=service.rerollWithMasterfulExpertise({campaignId,screenId:'SCR-015'});assert.equal(before.status,'NO_PREVIOUS_SKILL_CHECK_NO_MUTATION');
  const first=service.perform({campaignId,screenId:'SCR-015',intent:'observe'});assert.equal(first.status,'OK');
  const reroll=service.rerollWithMasterfulExpertise({campaignId,screenId:'SCR-015'});assert.equal(reroll.status,'OK');
  assert.equal(reroll.mechanicalEvent.kind,'PLAYER_ACTION_CLASS_ABILITY_REROLL');
  assert.ok(reroll.mechanicalEvent.resolution.total>=first.mechanicalEvent.resolution.total,'better result must be retained');
  assert.equal(reroll.projectedState.classAbilityUsage.MASTERFUL_EXPERTISE.sceneId,'scene:ruinas-da-vigilia:encounter:1');
});

test('Masterful Expertise is a hard zero-mutation gate after use in same scene',()=>{
  const {service,campaignId,persistence}=setup();service.perform({campaignId,screenId:'SCR-015',intent:'observe'});service.rerollWithMasterfulExpertise({campaignId,screenId:'SCR-015'});
  const before=persistence.loadCampaign(campaignId);const blocked=service.rerollWithMasterfulExpertise({campaignId,screenId:'SCR-015'});const after=persistence.loadCampaign(campaignId);
  assert.equal(blocked.status,'MASTERFUL_EXPERTISE_ALREADY_USED_THIS_SCENE_NO_MUTATION');assert.equal(after.revision,before.revision);assert.equal(after.stateJson,before.stateJson);
});

test('Partial Expert does not receive Masterful Expertise even if a bad fixture lists the ability',()=>{
  const partial=structuredClone(iria);partial.classId='PARTIAL_EXPERT';partial.class='PARTIAL_EXPERT';partial.classAbilities=['MASTERFUL_EXPERTISE','QUICK_LEARNER'];
  const {service,campaignId}=setup(partial);service.perform({campaignId,screenId:'SCR-015',intent:'observe'});const blocked=service.rerollWithMasterfulExpertise({campaignId,screenId:'SCR-015'});assert.equal(blocked.status,'MASTERFUL_EXPERTISE_NOT_AVAILABLE_NO_MUTATION');
});
