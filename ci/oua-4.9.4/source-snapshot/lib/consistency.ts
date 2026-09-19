import type { CampaignState, Character, Npc } from './db';
import type { MapDiscovery } from './map';
import { normalizeMasterState } from './master/state';
import { modeForScene, sceneCompatibleWithMode } from './master/sceneMachine';

export interface ConsistencyCheck {
  id: string;
  label: string;
  status: 'pass' | 'warning' | 'fail';
  detail: string;
}

export interface ConsistencyReport {
  generatedAt: number;
  campaignId: string;
  checks: ConsistencyCheck[];
  pass: number;
  warning: number;
  fail: number;
}

export interface ReconciledState {
  campaign: CampaignState;
  characters: Character[];
  npcs: Npc[];
  discovery: MapDiscovery;
  report: ConsistencyReport;
}

function unique(values: string[]): string[] {
  return Array.from(new Set(values.filter(Boolean)));
}

function compatibleSubsystemStatus(campaign: CampaignState): ConsistencyCheck[] {
  const state = normalizeMasterState(campaign);
  const checks: ConsistencyCheck[] = [];
  const active = state.scene.status === 'active' || state.scene.status === 'resolving';
  checks.push({
    id: 'scene-mode', label: 'Cena e modo',
    status: sceneCompatibleWithMode(state.scene.kind, state.activeMode) ? 'pass' : 'fail',
    detail: `${state.scene.kind} / ${state.activeMode}`,
  });
  const pairs: Array<[string, boolean, string]> = [
    ['journey-state', !active || state.scene.kind !== 'journey' || state.journey.status !== 'idle', `Jornada: ${state.journey.status}`],
    ['council-state', !active || state.scene.kind !== 'council' || state.council.status !== 'idle', `Conselho: ${state.council.status}`],
    ['combat-state', !active || state.scene.kind !== 'combat' || state.combat.status !== 'idle', `Combate: ${state.combat.status}`],
    ['fellowship-state', !active || state.scene.kind !== 'fellowship' || state.fellowshipPhase.status !== 'idle', `Sociedade: ${state.fellowshipPhase.status}`],
  ];
  pairs.forEach(([id, ok, detail]) => checks.push({ id, label: id, status: ok ? 'pass' : 'fail', detail }));
  return checks;
}

export function validateCanonicalState(campaign: CampaignState, characters: Character[], npcs: Npc[], discovery: MapDiscovery): ConsistencyReport {
  const state = normalizeMasterState(campaign);
  const characterIds = new Set(characters.map(character => character.id));
  const npcIds = new Set(npcs.map(npc => npc.id));
  const party = unique(campaign.partyCharacterIds || []);
  const presence = unique(campaign.presentNpcIds || []);
  const checks: ConsistencyCheck[] = [
    {
      id: 'party-valid', label: 'Companhia válida',
      status: party.length > 0 && party.every(id => characterIds.has(id)) ? 'pass' : 'fail',
      detail: `${party.length} Heróis-jogadores em jogo.`,
    },
    {
      id: 'active-hero', label: 'Herói ativo',
      status: Boolean(campaign.activeCharacterId && party.includes(campaign.activeCharacterId) && characterIds.has(campaign.activeCharacterId)) ? 'pass' : 'fail',
      detail: campaign.activeCharacterId || 'ausente',
    },
    {
      id: 'location-single-source', label: 'Localização canônica',
      status: campaign.currentLocation === state.position.locationName ? 'pass' : 'fail',
      detail: `${campaign.currentLocation} / ${state.position.locationName}`,
    },
    {
      id: 'hex-single-source', label: 'Hex canônico',
      status: (discovery.currentHexId || null) === (state.position.currentHexId || null) ? 'pass' : 'fail',
      detail: `${discovery.currentHexId || 'nenhum'} / ${state.position.currentHexId || 'nenhum'}`,
    },
    {
      id: 'map-fatigue-disabled', label: 'Fadiga fora do mapa',
      status: Number(discovery.fatigue || 0) === 0 ? 'pass' : 'fail',
      detail: `Fadiga visual: ${Number(discovery.fatigue || 0)}`,
    },
    {
      id: 'scene-heroes', label: 'Participantes da cena',
      status: state.scene.participantHeroIds.every(id => party.includes(id)) ? 'pass' : 'fail',
      detail: `${state.scene.participantHeroIds.length} Heróis na cena.`,
    },
    {
      id: 'npc-presence', label: 'Presença de NPCs',
      status: presence.every(id => npcIds.has(id))
        && presence.every(id => state.scene.participantNpcIds.includes(id))
        && npcs.every(npc => Boolean(npc.presentInScene) === presence.includes(npc.id)) ? 'pass' : 'fail',
      detail: `${presence.length} NPCs presentes.`,
    },
    {
      id: 'pending-test-actor', label: 'Teste pendente direcionado',
      status: !campaign.pendingTest?.characterId || party.includes(campaign.pendingTest.characterId) ? 'pass' : 'fail',
      detail: campaign.pendingTest?.characterId || 'nenhum teste pendente',
    },
    ...compatibleSubsystemStatus(campaign),
  ];
  return {
    generatedAt: Date.now(), campaignId: campaign.id, checks,
    pass: checks.filter(check => check.status === 'pass').length,
    warning: checks.filter(check => check.status === 'warning').length,
    fail: checks.filter(check => check.status === 'fail').length,
  };
}

/**
 * Consolida localização, presença, Companhia e estados de fase sem apagar
 * descobertas, histórico, fichas ou NPCs. Em migração, o atlas vence quando já
 * possui posição; em jogo normal, o MasterState é a autoridade.
 */
export function reconcileCanonicalState(
  campaignInput: CampaignState,
  characterInput: Character[],
  npcInput: Npc[],
  discoveryInput: MapDiscovery,
  options: { mapWins?: boolean; preservePresenceUnion?: boolean } = {},
): ReconciledState {
  // O histórico pode conter imagens base64 grandes. A reconciliação não altera
  // cenas, portanto preserva essas strings por referência e clona apenas o
  // restante do estado mutável.
  const { history, ...campaignWithoutHistory } = campaignInput;
  const campaign: CampaignState = {
    ...structuredClone(campaignWithoutHistory),
    history: history.map(scene => ({ ...scene })),
  };
  const characters = structuredClone(characterInput);
  let npcs = structuredClone(npcInput);
  const discovery = structuredClone(discoveryInput);
  const characterIds = new Set(characters.map(character => character.id));
  const party = unique(campaign.partyCharacterIds || []).filter(id => characterIds.has(id));
  if (!party.length && characters[0]) party.push(characters[0].id);
  campaign.partyCharacterIds = party;
  campaign.activeCharacterId = campaign.activeCharacterId && party.includes(campaign.activeCharacterId)
    ? campaign.activeCharacterId : party[0] || null;
  const active = characters.find(character => character.id === campaign.activeCharacterId);
  if (active) campaign.characterSheet = structuredClone(active.sheet);

  const state = normalizeMasterState(campaign);
  state.schemaVersion = 3;
  const mapHex = discovery.currentHexId || discovery.routeHexIds?.at(-1) || null;
  const canonicalHex = options.mapWins && mapHex ? mapHex : state.position.currentHexId || mapHex;
  const route = unique([...(state.position.routeHexIds || []), ...(discovery.routeHexIds || discovery.visitedHexIds || []), ...(canonicalHex ? [canonicalHex] : [])]);
  state.position = { ...state.position, currentHexId: canonicalHex, routeHexIds: route };
  campaign.currentLocation = state.position.locationName || campaign.currentLocation || 'Eriador';
  state.position.locationName = campaign.currentLocation;
  state.scene.locationName = campaign.currentLocation;

  discovery.currentHexId = canonicalHex;
  discovery.routeHexIds = route;
  discovery.visitedHexIds = unique([...(discovery.visitedHexIds || []), ...route]);
  discovery.discoveredHexIds = unique([...(discovery.discoveredHexIds || []), ...route]);
  discovery.fatigue = 0;
  discovery.updatedAt = Date.now();

  const npcIds = new Set(npcs.map(npc => npc.id));
  const unionPresence = unique([
    ...(campaign.presentNpcIds || []),
    ...(state.scene.participantNpcIds || []),
    ...npcs.filter(npc => npc.presentInScene).map(npc => npc.id),
  ]).filter(id => npcIds.has(id));
  const scenePresence = state.scene.status === 'active' || state.scene.status === 'resolving'
    ? state.scene.participantNpcIds.filter(id => npcIds.has(id)) : campaign.presentNpcIds.filter(id => npcIds.has(id));
  const presence = options.preservePresenceUnion ? unionPresence : unique(scenePresence);
  campaign.presentNpcIds = presence;
  state.scene.participantNpcIds = presence;
  npcs = npcs.map(npc => ({ ...npc, presentInScene: presence.includes(npc.id) }));

  state.scene.participantHeroIds = unique(state.scene.participantHeroIds).filter(id => party.includes(id));
  if (!state.scene.participantHeroIds.length) state.scene.participantHeroIds = [...party];
  if (!state.scene.spotlightCharacterId || !state.scene.participantHeroIds.includes(state.scene.spotlightCharacterId)) {
    state.scene.spotlightCharacterId = campaign.activeCharacterId || state.scene.participantHeroIds[0] || null;
  }
  state.activeMode = modeForScene(state.scene.kind);
  const activeScene = state.scene.status === 'active' || state.scene.status === 'resolving';
  if (activeScene && state.scene.kind === 'journey' && state.journey.status === 'idle') state.journey.status = 'planned';
  if (activeScene && state.scene.kind === 'council' && state.council.status === 'idle') state.council.status = 'introduction';
  if (activeScene && state.scene.kind === 'combat' && state.combat.status === 'idle') state.combat.status = 'round';
  if (activeScene && state.scene.kind === 'fellowship' && state.fellowshipPhase.status === 'idle') state.fellowshipPhase.status = 'active';
  state.updatedAt = Date.now();
  campaign.masterState = state;
  campaign.schemaVersion = Math.max(300, campaign.schemaVersion || 0);
  campaign.updatedAt = Date.now();

  return { campaign, characters, npcs, discovery, report: validateCanonicalState(campaign, characters, npcs, discovery) };
}
