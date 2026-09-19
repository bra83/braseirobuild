import {
  getStoreData,
  listSoundMetadata,
  saveStoreData,
  type CampaignState,
  type Manual,
  type Npc,
  type SoundItem,
  type TokenItem,
} from '../db';
import { APP_VERSION } from '../version';
import { ERIADOR_MAP_ID, loadRuntimeMapManifest } from '../map';
import type { AssetRegistryEntry } from './types';

function entry(
  assetType: AssetRegistryEntry['assetType'],
  sourceId: string,
  name: string,
  available: boolean,
  origin: AssetRegistryEntry['origin'],
  metadata: Record<string, unknown> = {},
  size?: number,
): AssetRegistryEntry {
  return {
    id: `${assetType}:${sourceId}`,
    assetType,
    sourceId,
    name,
    size,
    available,
    origin,
    version: APP_VERSION,
    lastVerifiedAt: Date.now(),
    metadata,
  };
}

export async function rebuildAssetRegistry(campaign?: CampaignState | null): Promise<AssetRegistryEntry[]> {
  const [manifest, manuals, tokens, npcs, sounds, snapshots] = await Promise.all([
    loadRuntimeMapManifest().catch(() => null),
    getStoreData<Manual>('manuals').catch(() => []),
    getStoreData<TokenItem>('tokens').catch(() => []),
    getStoreData<Npc>('npcs').catch(() => []),
    listSoundMetadata().catch(() => [] as SoundItem[]),
    getStoreData<{ id: string; campaignId: string; createdAt: number; reason: string }>('snapshots').catch(() => []),
  ]);
  const rows: AssetRegistryEntry[] = [];
  rows.push(entry('map', ERIADOR_MAP_ID, 'Mapa da Terra-média 10.630 × 7.678', Boolean(manifest), 'bundled', {
    hexCount: manifest?.hexes.length || 0,
    revision: manifest?.revision || 0,
    imageWidth: manifest?.image.width || 0,
    imageHeight: manifest?.image.height || 0,
  }));
  for (const manual of manuals) {
    rows.push(entry('manual', manual.id, manual.name, Boolean(manual.indexed && manual.chunkCount > 0), 'indexeddb', {
      indexed: manual.indexed,
      chunkCount: manual.chunkCount,
      pageCount: manual.pageCount || 0,
      system: manual.system,
    }, manual.size));
  }
  for (const token of tokens) {
    rows.push(entry('token', token.id, token.name, Boolean(token.blob?.size), 'indexeddb', {
      category: token.category,
      assignedNpcCount: npcs.filter(npc => npc.tokenId === token.id).length,
    }, token.blob?.size));
  }
  for (const sound of sounds) {
    rows.push(entry('audio', sound.id, sound.name, sound.available !== false, sound.source === 'native' ? 'native' : 'indexeddb', {
      channel: sound.catalogChannel || sound.channel,
      familyKey: sound.familyKey || '',
      failCount: sound.failCount || 0,
      playCount: sound.playCount || 0,
    }, sound.size));
  }
  if (campaign) rows.push(entry('campaign', campaign.id, campaign.name, true, 'indexeddb', {
    historyLength: campaign.history.length,
    eventCount: campaign.events?.length || 0,
    currentLocation: campaign.currentLocation,
    currentHexId: campaign.masterState?.position.currentHexId || null,
  }));
  for (const snapshot of snapshots.filter(item => !campaign || item.campaignId === campaign.id).slice(-12)) {
    rows.push(entry('snapshot', snapshot.id, `Snapshot: ${snapshot.reason}`, true, 'generated', { campaignId: snapshot.campaignId, createdAt: snapshot.createdAt }));
  }
  for (const row of rows) await saveStoreData('asset_registry', row, 'registro central de recursos');
  return rows;
}

export async function getAssetRegistry(): Promise<AssetRegistryEntry[]> {
  return getStoreData<AssetRegistryEntry>('asset_registry');
}
