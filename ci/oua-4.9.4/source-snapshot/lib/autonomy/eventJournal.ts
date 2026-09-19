import { getStoreData, getStoreItem, saveStoreData } from '../db';
import type { CampaignState } from '../db';
import type { EventJournalEntry, InputChannel, TurnTransaction } from './types';

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function fnv1a(value: string): string {
  let hash = 0x811c9dc5;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
}

export function createTurnIdempotencyKey(campaign: CampaignState, action: string, inputChannel: InputChannel = 'PLAYER_ACTION'): string {
  const lastSceneId = campaign.history.at(-1)?.id || 'no-scene';
  const pendingRoll = campaign.pendingDiceRoll?.timestamp || 0;
  const source = `${String(campaign.id || 'campaign')}|${lastSceneId}|${pendingRoll}|${inputChannel}|${normalize(action)}`;
  return `turn-key-${fnv1a(source)}`;
}

export interface BeginTurnResult {
  transaction: TurnTransaction;
  duplicateCommitted: boolean;
  duplicateInFlight: boolean;
}

export async function beginTurnTransaction(campaign: CampaignState, action: string, inputChannel: InputChannel = 'PLAYER_ACTION'): Promise<BeginTurnResult> {
  const idempotencyKey = createTurnIdempotencyKey(campaign, action, inputChannel);
  const existing = await getStoreData<TurnTransaction>('turn_transactions')
    .then(rows => rows
      .filter(row => row.campaignId === campaign.id && row.idempotencyKey === idempotencyKey)
      .sort((a, b) => b.updatedAt - a.updatedAt)[0])
    .catch(() => undefined);

  if (existing?.status === 'committed') {
    return { transaction: existing, duplicateCommitted: true, duplicateInFlight: false };
  }
  if (existing && ['preparing', 'mechanics-applied'].includes(existing.status) && Date.now() - existing.updatedAt < 90_000) {
    return { transaction: existing, duplicateCommitted: false, duplicateInFlight: true };
  }

  const now = Date.now();
  const transaction: TurnTransaction = {
    id: `turn_${campaign.id}_${now}_${Math.random().toString(36).slice(2, 7)}`,
    idempotencyKey,
    campaignId: campaign.id,
    action,
    inputChannel,
    baseSceneId: campaign.history.at(-1)?.id,
    status: 'preparing',
    createdAt: now,
    updatedAt: now,
  };
  await saveStoreData('turn_transactions', transaction, 'início de turno autônomo');
  await appendJournalEntry({
    id: `${transaction.id}:started`,
    campaignId: campaign.id,
    turnId: transaction.id,
    eventType: 'TURN_STARTED',
    entityType: 'campaign',
    entityId: campaign.id,
    payload: { action, inputChannel, baseSceneId: transaction.baseSceneId || null },
    idempotencyKey,
    createdAt: now,
    applied: true,
  });
  return { transaction, duplicateCommitted: false, duplicateInFlight: false };
}

export async function markTurnMechanicsApplied(turnId: string, details: Record<string, unknown>): Promise<void> {
  const transaction = await getStoreItem<TurnTransaction>('turn_transactions', turnId);
  if (!transaction || transaction.status === 'committed') return;
  const updated = { ...transaction, status: 'mechanics-applied' as const, updatedAt: Date.now() };
  await saveStoreData('turn_transactions', updated, 'mecânica do turno validada');
  await appendJournalEntry({
    id: `${turnId}:mechanics`, campaignId: transaction.campaignId, turnId,
    eventType: 'TURN_MECHANICS_APPLIED', entityType: 'campaign', entityId: transaction.campaignId,
    payload: { inputChannel: transaction.inputChannel, ...details }, idempotencyKey: transaction.idempotencyKey, createdAt: Date.now(), applied: true,
  });
}

export async function commitTurnTransaction(turnId: string, sceneId: string, resultDigest: string, payload: Record<string, unknown> = {}): Promise<void> {
  const transaction = await getStoreItem<TurnTransaction>('turn_transactions', turnId);
  if (!transaction || transaction.status === 'committed') return;
  const updated: TurnTransaction = {
    ...transaction,
    status: 'committed',
    committedSceneId: sceneId,
    resultDigest,
    updatedAt: Date.now(),
  };
  await saveStoreData('turn_transactions', updated, 'commit do turno autônomo');
  await appendJournalEntry({
    id: `${turnId}:committed`, campaignId: transaction.campaignId, turnId,
    eventType: 'TURN_COMMITTED', entityType: 'campaign', entityId: transaction.campaignId,
    payload: { inputChannel: transaction.inputChannel, sceneId, resultDigest, ...payload }, idempotencyKey: transaction.idempotencyKey,
    createdAt: Date.now(), applied: true,
  });
}

export async function abortTurnTransaction(turnId: string, error: unknown): Promise<void> {
  const transaction = await getStoreItem<TurnTransaction>('turn_transactions', turnId).catch(() => undefined);
  if (!transaction || transaction.status === 'committed') return;
  const message = error instanceof Error ? error.message : String(error || 'falha desconhecida');
  await saveStoreData('turn_transactions', {
    ...transaction, status: 'aborted', error: message.slice(0, 1200), updatedAt: Date.now(),
  } satisfies TurnTransaction, 'cancelamento seguro do turno');
  await appendJournalEntry({
    id: `${turnId}:aborted`, campaignId: transaction.campaignId, turnId,
    eventType: 'TURN_ABORTED', entityType: 'system', entityId: transaction.campaignId,
    payload: { error: message.slice(0, 1200) }, idempotencyKey: transaction.idempotencyKey,
    createdAt: Date.now(), applied: true,
  }).catch(() => undefined);
}

export async function appendJournalEntry(entry: EventJournalEntry): Promise<void> {
  const existing = await getStoreItem<EventJournalEntry>('event_journal', entry.id).catch(() => undefined);
  if (existing) return;
  await saveStoreData('event_journal', entry, 'diário canônico de eventos');
}

export async function appendCampaignEventsToJournal(
  campaignId: string,
  turnId: string | undefined,
  events: Array<{ id: string; type: string; timestamp: number; sceneId?: string; data?: Record<string, unknown> }>,
): Promise<void> {
  for (const event of events) {
    await appendJournalEntry({
      id: `campaign-event:${campaignId}:${event.id}`,
      campaignId,
      turnId,
      eventType: event.type.toUpperCase(),
      entityType: event.type === 'location_changed' ? 'map' : event.type === 'npc_encountered' ? 'npc' : 'campaign',
      entityId: event.type === 'npc_encountered' ? String(event.data?.npcId || '') : campaignId,
      payload: { sceneId: event.sceneId || null, ...(event.data || {}) },
      createdAt: event.timestamp || Date.now(),
      applied: true,
    });
  }
}

export function digestTurnResult(campaign: CampaignState, sceneId: string): string {
  return fnv1a(JSON.stringify({
    campaignId: campaign.id,
    sceneId,
    updatedAt: campaign.updatedAt,
    historyLength: campaign.history.length,
    currentLocation: campaign.currentLocation,
    currentHexId: campaign.masterState?.position.currentHexId || null,
    eventCount: campaign.events?.length || 0,
  }));
}
