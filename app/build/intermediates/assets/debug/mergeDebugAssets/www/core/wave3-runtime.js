import { MemoryPersistenceAdapter, NativePersistencePort } from './persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from './campaign-state.js';
import { DeterministicBarbaraTestAdapter } from './barbara-test-adapter.js';
import { createRuntimeNarrativeProvider } from './runtime-narrative-provider.js';
import { PlayerActionService } from './action-service.js';
import { resolveActiveCampaignId } from './runtime-campaign.js';

export function makeWave3CampaignState({ campaignId, displayName, masterSeed, character }) {
  const state = makeEmptyCampaignState({ campaignId, displayName, masterSeed });
  state.characterState = structuredClone(character);
  state.encounterState = { screenId: 'SCR-015', sceneId: 'scene:ruinas-da-vigilia:encounter:1', contactState: 'CONTATO INESPERADO', observationCount: 0, lastResolution: null };
  state.worldState = { ...state.worldState, position: { q: 17, r: 22, label: 'Ruínas da Vigília' } };
  return state;
}

export function createWave3Runtime({ sceneRules, character, nativeObject = globalThis.BraseiroNative } = {}) {
  const persistence = nativeObject ? new NativePersistencePort(nativeObject) : new MemoryPersistenceAdapter();
  const campaignId = nativeObject ? resolveActiveCampaignId(persistence) : 'wave3-browser-scr015';
  if (!campaignId) return { status: 'NO_ACTIVE_CAMPAIGN', persistence, actionService: null, campaignId: null };
  const existing = persistence.loadCampaign(campaignId);
  if (existing.status === 'NOT_FOUND' && !nativeObject) {
    const masterSeed = 'wave3-browser-seed-v1';
    const state = makeWave3CampaignState({ campaignId, displayName: 'Wave 3 Browser Fixture', masterSeed, character });
    persistence.createCampaign({
      campaignId,
      displayName: state.displayName,
      stateJson: serializeCampaignState(state),
      masterSeed,
      rngAlgorithm: state.rngState.algorithm,
      initialSnapshotId: 'snapshot:wave3:r0',
    });
  }
  return {
    status: 'OK', campaignId, persistence,
    actionService: new PlayerActionService({ persistence, sceneRules, barbara: nativeObject ? createRuntimeNarrativeProvider({native:nativeObject}) : new DeterministicBarbaraTestAdapter() }),
  };
}
