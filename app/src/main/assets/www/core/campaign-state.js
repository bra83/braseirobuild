import { canonicalJson } from './canonical-json.js';
import { RNG_ALGORITHM } from './rng-core.js';

export function makeEmptyCampaignState({ campaignId, displayName, masterSeed }) {
  return {
    schemaVersion: 1,
    appDataVersion: 1,
    revision: 0,
    campaignId,
    displayName,
    authorityBindings: { system: 'WWN', rulesProfile: 'STANDARD' },
    rngState: { algorithm: RNG_ALGORITHM, masterSeed, counters: {} },
    characterState: null,
    charactersState: {},
    partyState: null,
    contentState: null,
    worldState: { time: null, position: null, weather: null },
    encounterState: null,
    npcState: {},
    factionState: {},
    journalState: { lastMechanicalEventId: null },
  };
}

export function serializeCampaignState(state) {
  return canonicalJson(state);
}
