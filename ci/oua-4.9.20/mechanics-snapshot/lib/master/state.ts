import type { CampaignState } from '../db';
import type { MasterState, SceneKind, SceneState, SessionDirectorState } from './types';
import { normalizeOrganization, normalizeStronghold } from '../organizations';

function defaultScene(campaign: Pick<CampaignState, 'id' | 'currentLocation' | 'partyCharacterIds' | 'activeCharacterId'>, now: number): SceneState {
  const heroes = Array.from(new Set(campaign.partyCharacterIds || []));
  const active = campaign.activeCharacterId && heroes.includes(campaign.activeCharacterId) ? campaign.activeCharacterId : heroes[0] || null;
  return {
    id: `scene_state_${campaign.id}_${now}`,
    kind: 'exploration',
    status: 'idle',
    title: 'Cena ainda não iniciada',
    objective: '',
    locationName: campaign.currentLocation || 'Eriador',
    participantHeroIds: heroes,
    participantNpcIds: [],
    spotlightCharacterId: active,
    spotlightQueue: active ? [active, ...heroes.filter(id => id !== active)] : heroes,
    pendingActions: {},
    tension: 0,
    endCondition: '',
    startedAt: now,
    updatedAt: now,
  };
}

function defaultSessionDirector(now: number): SessionDirectorState {
  return {
    sessionObjective: '',
    pacing: 'quiet',
    spotlightDebtByHero: {},
    sceneCountByHero: {},
    lastActedAtByHero: {},
    nextSuggestedHeroId: null,
    openBeats: [],
    updatedAt: now,
  };
}

export function createDefaultMasterState(campaign: Pick<CampaignState, 'id' | 'currentLocation' | 'campaignSummary' | 'partyCharacterIds' | 'activeCharacterId'>): MasterState {
  const now = Date.now();
  return {
    schemaVersion: 3,
    activeMode: 'adventure',
    position: { currentHexId: null, locationName: campaign.currentLocation || 'Eriador', routeHexIds: [] },
    worldClock: { year: 2965, season: 'spring', day: 1, hour: 8, minute: 0, elapsedMinutes: 0 },
    company: {
      fellowshipCurrent: 0,
      fellowshipMax: 0,
      patron: '',
      sanctuaryIds: [],
      eyeAwareness: 0,
      journeyRoles: {},
      fellowshipFocus: {},
      spotlightByHero: {},
    },
    journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null },
    council: { status: 'idle', tolerance: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },
    combat: { status: 'idle', round: 0, actors: [], advantageDice: 0, complicationDice: 0, turnIndex: 0, backgroundImage: null, narrativeBlocks: [], rolls: [], updatedAt: now },
    fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {} },
    quests: [],
    threatClocks: [],
    knowledge: [],
    npcActors: [],
    livingWorld: {
      companyStyle: 'fellowship',
      troupeName: '',
      baseLocation: campaign.currentLocation || 'Eriador',
      heroLives: {},
      projects: [],
      organizations: [],
      strongholds: [],
      activityLog: [],
      pendingReview: [],
      automationEnabled: true,
      lastSimulatedAt: now,
      updatedAt: now,
    },
    scene: defaultScene(campaign, now),
    sessionDirector: defaultSessionDirector(now),
    memory: {
      campaignSummary: campaign.campaignSummary || '',
      sessionSummaries: [],
      canonicalFacts: [],
      promises: [],
      openThreads: [],
    },
    currentSessionId: `session_${campaign.id}_${now}`,
    updatedAt: now,
  };
}

function inferSceneKind(mode: MasterState['activeMode']): SceneKind {
  if (mode === 'adventure') return 'exploration';
  return mode;
}

function normalizeStringArray(value: unknown): string[] {
  return Array.isArray(value) ? Array.from(new Set(value.map(String).filter(Boolean))) : [];
}

export function normalizeMasterState(campaign: CampaignState): MasterState {
  const base = createDefaultMasterState(campaign);
  const incoming = campaign.masterState as MasterState | undefined;
  if (!incoming) return base;
  const partyIds = new Set(campaign.partyCharacterIds || []);
  const incomingScene = incoming.scene || base.scene;
  const sceneHeroes = normalizeStringArray(incomingScene.participantHeroIds).filter(id => partyIds.has(id));
  const allHeroes = normalizeStringArray(campaign.partyCharacterIds);
  const participants = sceneHeroes.length ? sceneHeroes : allHeroes;
  const spotlight = incomingScene.spotlightCharacterId && partyIds.has(incomingScene.spotlightCharacterId)
    ? incomingScene.spotlightCharacterId
    : campaign.activeCharacterId && partyIds.has(campaign.activeCharacterId)
      ? campaign.activeCharacterId
      : participants[0] || null;
  const scene: SceneState = {
    ...base.scene,
    ...incomingScene,
    id: incomingScene.id || base.scene.id,
    kind: incomingScene.kind || inferSceneKind(incoming.activeMode || base.activeMode),
    locationName: incomingScene.locationName || incoming.position?.locationName || campaign.currentLocation || base.scene.locationName,
    participantHeroIds: participants,
    participantNpcIds: normalizeStringArray(incomingScene.participantNpcIds),
    spotlightCharacterId: spotlight,
    spotlightQueue: normalizeStringArray(incomingScene.spotlightQueue).filter(id => partyIds.has(id)),
    pendingActions: incomingScene.pendingActions || {},
    tension: Math.max(0, Math.min(6, Number(incomingScene.tension) || 0)),
    startedAt: Number(incomingScene.startedAt) || Date.now(),
    updatedAt: Number(incomingScene.updatedAt) || Date.now(),
  };
  if (!scene.spotlightQueue.length) scene.spotlightQueue = spotlight ? [spotlight, ...participants.filter(id => id !== spotlight)] : participants;
  const director: SessionDirectorState = {
    ...base.sessionDirector,
    ...(incoming.sessionDirector || {}),
    spotlightDebtByHero: incoming.sessionDirector?.spotlightDebtByHero || {},
    sceneCountByHero: incoming.sessionDirector?.sceneCountByHero || {},
    lastActedAtByHero: incoming.sessionDirector?.lastActedAtByHero || {},
    nextSuggestedHeroId: incoming.sessionDirector?.nextSuggestedHeroId && partyIds.has(incoming.sessionDirector.nextSuggestedHeroId)
      ? incoming.sessionDirector.nextSuggestedHeroId
      : null,
    openBeats: Array.isArray(incoming.sessionDirector?.openBeats) ? incoming.sessionDirector.openBeats : [],
    updatedAt: incoming.sessionDirector?.updatedAt || Date.now(),
  };
  return {
    ...base,
    ...incoming,
    position: {
      ...base.position,
      ...(incoming.position || {}),
      locationName: incoming.position?.locationName || campaign.currentLocation || base.position.locationName,
      routeHexIds: Array.isArray(incoming.position?.routeHexIds) ? incoming.position.routeHexIds : [],
    },
    worldClock: { ...base.worldClock, ...(incoming.worldClock || {}) },
    company: {
      ...base.company,
      ...(incoming.company || {}),
      sanctuaryIds: Array.isArray(incoming.company?.sanctuaryIds) ? incoming.company.sanctuaryIds : [],
      journeyRoles: incoming.company?.journeyRoles || {},
      fellowshipFocus: incoming.company?.fellowshipFocus || {},
      spotlightByHero: incoming.company?.spotlightByHero || {},
    },
    journey: { ...base.journey, ...(incoming.journey || {}), segments: Array.isArray(incoming.journey?.segments) ? incoming.journey.segments : [] },
    council: { ...base.council, ...(incoming.council || {}), participants: incoming.council?.participants || [], interlocutorNpcIds: incoming.council?.interlocutorNpcIds || [] },
    combat: { ...base.combat, ...(incoming.combat || {}), actors: incoming.combat?.actors || [], narrativeBlocks: incoming.combat?.narrativeBlocks || [], rolls: incoming.combat?.rolls || [], turnIndex: Math.max(0, Number(incoming.combat?.turnIndex || 0)), updatedAt: incoming.combat?.updatedAt || Date.now() },
    fellowshipPhase: { ...base.fellowshipPhase, ...(incoming.fellowshipPhase || {}), undertakings: incoming.fellowshipPhase?.undertakings || {} },
    quests: Array.isArray(incoming.quests) ? incoming.quests : [],
    threatClocks: Array.isArray(incoming.threatClocks) ? incoming.threatClocks : [],
    knowledge: Array.isArray(incoming.knowledge)
      ? incoming.knowledge
          .filter(entry => entry && typeof entry === 'object')
          .map(entry => ({ ...entry, statement: String(entry.statement || '').trim() }))
          .filter(entry => entry.statement.length > 0)
      : [],
    npcActors: Array.isArray(incoming.npcActors) ? incoming.npcActors : [],
    livingWorld: {
      ...base.livingWorld,
      ...(incoming.livingWorld || {}),
      heroLives: incoming.livingWorld?.heroLives || {},
      projects: Array.isArray(incoming.livingWorld?.projects) ? incoming.livingWorld.projects : [],
      organizations: Array.isArray(incoming.livingWorld?.organizations) ? incoming.livingWorld.organizations.map(item => normalizeOrganization(item, item)) : [],
      strongholds: Array.isArray(incoming.livingWorld?.strongholds) ? incoming.livingWorld.strongholds.map(item => normalizeStronghold(item, item)) : [],
      activityLog: Array.isArray(incoming.livingWorld?.activityLog) ? incoming.livingWorld.activityLog : [],
      pendingReview: Array.isArray(incoming.livingWorld?.pendingReview) ? incoming.livingWorld.pendingReview : [],
      automationEnabled: incoming.livingWorld?.automationEnabled !== false,
      lastSimulatedAt: incoming.livingWorld?.lastSimulatedAt || Date.now(),
      updatedAt: incoming.livingWorld?.updatedAt || Date.now(),
    },
    scene,
    sessionDirector: director,
    memory: {
      ...base.memory,
      ...(incoming.memory || {}),
      sessionSummaries: incoming.memory?.sessionSummaries || [],
      canonicalFacts: Array.isArray(incoming.memory?.canonicalFacts)
        ? incoming.memory.canonicalFacts
            .filter(entry => entry && typeof entry === 'object')
            .map(entry => ({ ...entry, fact: String(entry.fact || '').trim(), tags: normalizeStringArray(entry.tags) }))
            .filter(entry => entry.fact.length > 0)
        : [],
      promises: incoming.memory?.promises || [],
      openThreads: incoming.memory?.openThreads || [],
    },
    schemaVersion: 3,
    updatedAt: incoming.updatedAt || Date.now(),
  };
}

export function publicKnowledge(masterState: MasterState, partyCharacterIds: string[]): MasterState['knowledge'] {
  const party = new Set(partyCharacterIds);
  return masterState.knowledge.filter(entry => entry.visibility === 'company' || (entry.visibility === 'hero' && entry.heroId && party.has(entry.heroId)));
}

export function recordHeroSpotlight(masterState: MasterState, heroId: string, partyCharacterIds: string[]): MasterState {
  const party = Array.from(new Set(partyCharacterIds));
  if (!party.includes(heroId)) return masterState;
  const now = Date.now();
  const counts = { ...masterState.sessionDirector.sceneCountByHero, [heroId]: (masterState.sessionDirector.sceneCountByHero[heroId] || 0) + 1 };
  const lastActed = { ...masterState.sessionDirector.lastActedAtByHero, [heroId]: now };
  const minimum = Math.min(...party.map(id => counts[id] || 0));
  const debt = Object.fromEntries(party.map(id => [id, Math.max(0, (counts[id] || 0) - minimum)]));
  const next = party
    .filter(id => id !== heroId)
    .sort((a, b) => (counts[a] || 0) - (counts[b] || 0) || (lastActed[a] || 0) - (lastActed[b] || 0))[0] || heroId;
  return {
    ...masterState,
    company: {
      ...masterState.company,
      spotlightByHero: { ...masterState.company.spotlightByHero, [heroId]: (masterState.company.spotlightByHero[heroId] || 0) + 1 },
    },
    scene: {
      ...masterState.scene,
      spotlightCharacterId: heroId,
      spotlightQueue: [next, ...party.filter(id => id !== next)],
      pendingActions: { ...masterState.scene.pendingActions, [heroId]: '' },
      updatedAt: now,
    },
    sessionDirector: {
      ...masterState.sessionDirector,
      sceneCountByHero: counts,
      lastActedAtByHero: lastActed,
      spotlightDebtByHero: debt,
      nextSuggestedHeroId: next,
      updatedAt: now,
    },
    updatedAt: now,
  };
}
