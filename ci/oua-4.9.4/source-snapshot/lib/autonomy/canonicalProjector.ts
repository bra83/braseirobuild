import {
  getStoreData,
  saveCanonicalTransaction,
  type CampaignState,
  type CanonicalStateSnapshot,
  type Character,
  type Npc,
} from '../db';

interface CanonicalCheckpointRow {
  id: string;
  campaignId: string;
  eventType: string;
  createdAt: number;
  payload?: {
    revision?: number;
    transactionId?: string;
    campaign?: CampaignState;
    state?: CanonicalStateSnapshot;
  };
}

export interface CanonicalProjectionCheck {
  status: 'ok' | 'missing' | 'stale' | 'diverged';
  detail: string;
  snapshot: CanonicalStateSnapshot | null;
  liveCharacters: Character[];
  liveNpcs: Npc[];
}

function sortedIds(rows: Array<{ id: string }>): string[] {
  return rows.map(row => row.id).sort((a, b) => a.localeCompare(b));
}

function sameIds(left: Array<{ id: string }>, right: Array<{ id: string }>): boolean {
  return JSON.stringify(sortedIds(left)) === JSON.stringify(sortedIds(right));
}

export async function getLatestCanonicalSnapshot(campaignId: string): Promise<CanonicalStateSnapshot | null> {
  const rows = await getStoreData<CanonicalCheckpointRow>('event_journal').catch(() => []);
  const checkpoint = rows
    .filter(row => row.campaignId === campaignId && row.eventType === 'CAMPAIGN_CHECKPOINT' && row.payload?.state?.campaign)
    .sort((a, b) => Number(b.payload?.revision || 0) - Number(a.payload?.revision || 0) || Number(b.createdAt || 0) - Number(a.createdAt || 0))[0];
  return checkpoint?.payload?.state || null;
}

/**
 * Confere se o estado materializado ainda corresponde ao último checkpoint
 * canónico. A comparação é deliberadamente limitada aos dados que o diário
 * consegue reconstruir sem blobs (campanha, elenco relevante, mapa e ambiente).
 */
export async function verifyCanonicalProjection(campaign: CampaignState): Promise<CanonicalProjectionCheck> {
  const snapshot = await getLatestCanonicalSnapshot(campaign.id);
  const [allCharacters, allNpcs, mapRows, environmentRows] = await Promise.all([
    getStoreData<Character>('characters').catch(() => []),
    getStoreData<Npc>('npcs').catch(() => []),
    getStoreData<any>('map_discovery').catch(() => []),
    getStoreData<any>('environment_states').catch(() => []),
  ]);
  const liveCharacters = allCharacters.filter(item => (campaign.partyCharacterIds || []).includes(item.id) || item.id === campaign.activeCharacterId);
  const relevantNpcIds = new Set([...(campaign.presentNpcIds || []), ...(campaign.masterState?.npcActors || []).map(item => item.npcId)]);
  const liveNpcs = allNpcs.filter(item => relevantNpcIds.has(item.id));
  if (!snapshot) return { status: 'missing', detail: 'Ainda não existe checkpoint canónico completo para esta campanha.', snapshot: null, liveCharacters, liveNpcs };
  const liveRevision = Number(campaign.revision || 0);
  const checkpointRevision = Number(snapshot.campaign.revision || 0);
  if (checkpointRevision < liveRevision) {
    return { status: 'stale', detail: `Checkpoint na revisão ${checkpointRevision}; banco na revisão ${liveRevision}.`, snapshot, liveCharacters, liveNpcs };
  }
  const liveMap = mapRows.filter(row => row.campaignId === campaign.id).sort((a, b) => Number(b.updatedAt || 0) - Number(a.updatedAt || 0))[0];
  const liveEnvironment = environmentRows.filter(row => row.campaignId === campaign.id).sort((a, b) => Number(b.generatedAt || 0) - Number(a.generatedAt || 0))[0];
  const diverged = checkpointRevision !== liveRevision
    || snapshot.campaign.lastTransactionId !== campaign.lastTransactionId
    || !sameIds(snapshot.characters || [], liveCharacters)
    || !sameIds(snapshot.npcs || [], liveNpcs)
    || String(snapshot.mapDiscovery?.currentHexId || '') !== String(liveMap?.currentHexId || '')
    || String(snapshot.environmentState?.id || '') !== String(liveEnvironment?.id || '')
    || Number(snapshot.environmentState?.travelStep || 0) !== Number(liveEnvironment?.travelStep || 0);
  if (diverged) {
    return { status: 'diverged', detail: `A projeção materializada divergiu do checkpoint da revisão ${checkpointRevision}.`, snapshot, liveCharacters, liveNpcs };
  }
  return { status: 'ok', detail: `Campanha, elenco, mapa e ambiente correspondem ao checkpoint canónico da revisão ${checkpointRevision}.`, snapshot, liveCharacters, liveNpcs };
}

export async function restoreCanonicalProjection(snapshot: CanonicalStateSnapshot, reason = 'reconstrução a partir do diário canónico'): Promise<CampaignState> {
  return saveCanonicalTransaction({
    campaign: snapshot.campaign,
    characters: snapshot.characters || [],
    npcs: snapshot.npcs || [],
    mapDiscovery: snapshot.mapDiscovery,
    environmentState: snapshot.environmentState,
    environmentMemory: snapshot.environmentMemory || [],
    autonomousEvents: snapshot.autonomousEvents || [],
    reason,
    journalEntries: [{
      id: `canonical-rebuild:${snapshot.campaign.id}:${Date.now()}`,
      campaignId: snapshot.campaign.id,
      eventType: 'CANONICAL_PROJECTION_REBUILT',
      entityType: 'canonical-state',
      entityId: snapshot.campaign.id,
      payload: { fromRevision: snapshot.campaign.revision || 0, sourceTransactionId: snapshot.campaign.lastTransactionId || null },
      createdAt: Date.now(),
      applied: true,
    }],
  });
}
