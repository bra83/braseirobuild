import {
  getStoreData, readEmergencyCampaignMirror, recoverLatestCampaignSnapshot, saveCanonicalTransaction, saveEmergencyCampaignMirror, type CampaignState, type CanonicalStateSnapshot,
} from '../db';
import { getLatestCanonicalSnapshot } from './canonicalProjector';

export interface RecoveryResult {
  campaign: CampaignState;
  recovered: boolean;
  source: 'database' | 'emergency-mirror' | 'snapshot' | 'event-checkpoint' | 'android-recovery';
  detail: string;
}

function valid(campaign: CampaignState | null | undefined): campaign is CampaignState {
  return Boolean(campaign?.id && campaign.characterSheet?.identity && Array.isArray(campaign.history) && campaign.worldFlags && Array.isArray(campaign.presentNpcIds));
}

function digest(campaign: CampaignState): string {
  const source = JSON.stringify({
    id: campaign.id, revision: campaign.revision || 0, updatedAt: campaign.updatedAt || 0,
    history: campaign.history.map(scene => scene.id), events: (campaign.events || []).map(event => event.id),
    sheet: campaign.characterSheet, party: campaign.partyCharacterIds, npcs: campaign.presentNpcIds,
    location: campaign.currentLocation, hex: campaign.masterState?.position.currentHexId || null,
  });
  let hash = 2166136261;
  for (let index = 0; index < source.length; index += 1) { hash ^= source.charCodeAt(index); hash = Math.imul(hash, 16777619); }
  return (hash >>> 0).toString(16);
}

function newer(candidate: CampaignState, current: CampaignState, source: Exclude<RecoveryResult['source'], 'database'>): boolean {
  if (candidate.id !== current.id) return false;
  const candidateRevision = Number(candidate.revision || 0);
  const currentRevision = Number(current.revision || 0);
  if (candidateRevision !== currentRevision) return candidateRevision > currentRevision;
  const candidateTransaction = String(candidate.lastTransactionId || '');
  const currentTransaction = String(current.lastTransactionId || '');
  // Na mesma revisão, uma fonte auxiliar só pode vencer se comprovar a mesma
  // transação canónica (ou se o banco antigo ainda não possuía identificador).
  if (candidateTransaction && currentTransaction && candidateTransaction !== currentTransaction) return false;
  if (!candidateTransaction && currentTransaction && source !== 'event-checkpoint') return false;
  const sameCanonicalCommit = Boolean(candidateTransaction) && (!currentTransaction || candidateTransaction === currentTransaction);
  if (!sameCanonicalCommit && source !== 'event-checkpoint') return false;
  if (candidate.history.length !== current.history.length) return candidate.history.length > current.history.length;
  if ((candidate.events?.length || 0) !== (current.events?.length || 0)) return (candidate.events?.length || 0) > (current.events?.length || 0);
  if (Number(candidate.updatedAt || 0) !== Number(current.updatedAt || 0)) {
    return Number(candidate.updatedAt || 0) > Number(current.updatedAt || 0) && (sameCanonicalCommit || !currentTransaction);
  }
  return digest(candidate) !== digest(current) && sameCanonicalCommit && source === 'event-checkpoint';
}

async function latestCheckpoint(campaignId: string): Promise<{ campaign: CampaignState; state: CanonicalStateSnapshot | null } | null> {
  const state = await getLatestCanonicalSnapshot(campaignId);
  if (state && valid(state.campaign)) return { campaign: state.campaign, state };
  // Compatibilidade com checkpoints 3.4, que continham apenas a campanha.
  const rows = await getStoreData<any>('event_journal').catch(() => []);
  const row = rows.filter(item => item.campaignId === campaignId && item.eventType === 'CAMPAIGN_CHECKPOINT' && item.payload?.campaign)
    .sort((a, b) => Number(b.payload?.revision || 0) - Number(a.payload?.revision || 0) || Number(b.createdAt || 0) - Number(a.createdAt || 0))[0];
  return valid(row?.payload?.campaign) ? { campaign: row.payload.campaign, state: null } : null;
}

function nativeRecoveryState(campaignId: string): CanonicalStateSnapshot | null {
  try {
    const raw = window.BraseiroAndroid?.getLatestRecoveryBackup?.(campaignId);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CanonicalStateSnapshot;
    return valid(parsed?.campaign) && parsed.campaign.id === campaignId ? parsed : null;
  } catch (error) {
    console.warn('[BRASEIRO_RECOVERY] backup nativo inválido:', error);
    return null;
  }
}

export async function recoverCampaignIfNeeded(campaign: CampaignState): Promise<RecoveryResult> {
  const mirror = readEmergencyCampaignMirror();
  const snapshot = await recoverLatestCampaignSnapshot(campaign.id).catch(() => null);
  const checkpoint = await latestCheckpoint(campaign.id);
  const nativeState = nativeRecoveryState(campaign.id);
  let selected = campaign;
  let source: RecoveryResult['source'] = 'database';
  const candidates: Array<{ campaign: CampaignState; source: Exclude<RecoveryResult['source'], 'database'> }> = [];
  if (valid(mirror) && mirror.id === campaign.id) candidates.push({ campaign: mirror, source: 'emergency-mirror' });
  if (valid(snapshot) && snapshot.id === campaign.id) candidates.push({ campaign: snapshot, source: 'snapshot' });
  if (checkpoint && checkpoint.campaign.id === campaign.id) candidates.push({ campaign: checkpoint.campaign, source: 'event-checkpoint' });
  if (nativeState && nativeState.campaign.id === campaign.id) candidates.push({ campaign: nativeState.campaign, source: 'android-recovery' });
  if (!valid(selected)) {
    const first = candidates.sort((a, b) => Number(b.campaign.revision || 0) - Number(a.campaign.revision || 0) || Number(b.campaign.updatedAt || 0) - Number(a.campaign.updatedAt || 0))[0];
    if (first) { selected = first.campaign; source = first.source; }
  } else {
    for (const candidate of candidates) if (newer(candidate.campaign, selected, candidate.source)) { selected = candidate.campaign; source = candidate.source; }
  }
  if (source !== 'database') {
    const checkpointState = source === 'event-checkpoint' ? checkpoint?.state : source === 'android-recovery' ? nativeState : null;
    const restored = await saveCanonicalTransaction({
      campaign: { ...selected, updatedAt: Math.max(Date.now(), selected.updatedAt || 0) },
      characters: checkpointState?.characters,
      npcs: checkpointState?.npcs,
      mapDiscovery: checkpointState?.mapDiscovery,
      environmentState: checkpointState?.environmentState,
      environmentMemory: checkpointState?.environmentMemory,
      autonomousEvents: checkpointState?.autonomousEvents,
      reason: `recuperação automática por ${source}`,
      journalEntries: [{ id: `recovery:${campaign.id}:${Date.now()}`, campaignId: campaign.id, eventType: 'CAMPAIGN_RECOVERED', entityType: 'system', entityId: campaign.id,
        payload: { source, previousRevision: campaign.revision || 0, recoveredRevision: selected.revision || 0, previousDigest: digest(campaign), recoveredDigest: digest(selected), restoredCanonicalState: Boolean(checkpointState) }, createdAt: Date.now(), applied: true }],
    });
    saveEmergencyCampaignMirror(restored);
    return { campaign: restored, recovered: true, source, detail: `Estado recuperado de ${source} na revisão ${restored.revision || 0}.` };
  }
  await getStoreData<CampaignState>('campaigns');
  return { campaign, recovered: false, source, detail: `Banco principal é a cópia canónica mais completa (revisão ${campaign.revision || 0}).` };
}
