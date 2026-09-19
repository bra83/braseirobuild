import type { CampaignState, Character, Npc } from './db';
import type { InputChannel } from './autonomy/types';
import type { MapDiscovery } from './map';

export interface BarbaraBridgeResult { available?: boolean; valid?: boolean; bypassed?: boolean; [key: string]: any }

function bridge() { return window.BraseiroAndroid; }

export async function getBarbaraHealth(): Promise<BarbaraBridgeResult> {
  try { return await bridge()?.barbaraHealth?.() || { available: false, bypassed: true }; }
  catch (error) { return { available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export function projectCampaignForBarbara(campaign: CampaignState, characters: Character[] = [], npcs: Npc[] = [], mapDiscovery?: MapDiscovery | null, hostStoryOccasion: 'campaign_opening' | 'first_arrival' | 'changed_return' | 'continuation' = 'continuation'): Record<string, unknown> {
  const master: any = campaign.masterState || {};
  const position: any = master.position || {};
  const clock: any = master.worldClock || {};
  const active = characters.find(item => item.id === campaign.activeCharacterId);
  const publicNpcs = Object.fromEntries(
    npcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id)).map((npc: any) => [npc.id, {
      id: npc.id, name: npc.name, role: npc.role, attitude: npc.attitude, emotional_state: npc.emotionalState,
      location: npc.location || position.locationName || campaign.currentLocation,
    }]),
  );
  const revision = Number((campaign as any).revision ?? 0);
  const currentHexId = position.currentHexId || mapDiscovery?.currentHexId || (campaign as any).mapDiscovery?.currentHexId || null;
  const fictionHistory = (campaign.history || []).filter((node: any) => node?.narrativeMeta?.advancesFiction !== false);
  const introducedLocations: Record<string, any> = {};
  fictionHistory.forEach((node: any, index: number) => {
    const location = String(node?.location || '').trim();
    if (!location) return;
    const entry = introducedLocations[location] || { visit_count: 0 };
    entry.visit_count += 1;
    entry.introduction_delivered = true;
    entry.introduction_occasion = index === 0 ? 'campaign_opening' : 'first_arrival';
    entry.introduction_tick = index;
    entry.last_seen_tick = index;
    entry.imprint = String(node?.hexId || node?.location || '');
    introducedLocations[location] = entry;
  });
  return {
    campaign_id: String(campaign.id || 'braseiro-o-um-anel'),
    system_id: 'the_one_ring',
    state_version: Math.max(0, Number.isFinite(revision) ? revision : 0),
    tick: Math.max(0, Number(clock.totalMinutes ?? clock.tick ?? 0)),
    location: String(position.locationName || campaign.currentLocation || ''),
    game_time: { day: clock.day ?? 1, hour: clock.hour ?? 0, minute: clock.minute ?? 0, season: clock.season || '' },
    discovery: { namespace: 'the_one_ring:terra_media', campaign_started: fictionHistory.length > 0, campaign_introduction_delivered: fictionHistory.length > 0, last_location_id: fictionHistory.at(-1)?.location || null, locations: introducedLocations, current_hex_id: currentHexId, map_discovery_current_hex_id: mapDiscovery?.currentHexId || null, q: position.q ?? null, r: position.r ?? null },
    weather: { kind: (campaign as any).environmentState?.weather?.kind || (campaign as any).environmentState?.currentWeather?.kind || '', intensity: (campaign as any).environmentState?.weather?.intensity || '' },
    player_state: active ? {
      character_id: active.id, name: active.sheet.identity.name || active.name,
      hope: active.sheet.attributes.hopeCurrent, shadow: active.sheet.attributes.shadow,
      weary: active.sheet.attributes.weary, miserable: active.sheet.attributes.miserable,
      load: active.sheet.attributes.load,
    } : {},
    npcs: publicNpcs,
    facts: { current_location: campaign.currentLocation, present_npc_ids: [...(campaign.presentNpcIds || [])], supervisor_story_occasion: hostStoryOccasion },
    public_ledger: [],
    secret_ledger: [],
    memory: [], factions: {}, rumors: [], events: [], clocks: {}, economy: {}, scene: master.scene || {}, notes: {}, sites: {}, world_flags: {},
  };
}

export async function validateBarbaraState(campaign: CampaignState, characters: Character[], npcs: Npc[], mapDiscovery?: MapDiscovery | null): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraValidateState;
  if (!fn) return { valid: true, available: false, bypassed: true };
  try { return await fn(projectCampaignForBarbara(campaign, characters, npcs, mapDiscovery)); }
  catch (error) { return { valid: true, available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export async function getBarbaraPlan(input: {
  action: string; inputChannel: InputChannel; location?: string; imprint?: string; narrative?: boolean; nonAdvancing?: boolean; importance?: string; storyOccasion?: 'campaign_opening' | 'first_arrival' | 'changed_return' | 'continuation';
}): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraPlan;
  if (!fn) return { available: false, bypassed: true };
  try { return await fn(input.action, input.importance || 'normal', input.narrative !== false, Boolean(input.nonAdvancing), input.location || '', input.imprint || '', input.inputChannel, input.storyOccasion || 'continuation'); }
  catch (error) { return { available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export async function validateBarbaraNarration(input: {
  response: Record<string, unknown>; action: string; inputChannel: InputChannel; occasion?: string; importance?: string;
}): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraValidate;
  if (!fn) return { valid: true, available: false, bypassed: true };
  try { return await fn(input.response, input.action, input.importance || 'normal', input.occasion || 'continuation', input.inputChannel); }
  catch (error) { return { valid: true, available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export async function getBarbaraDeviceProfile(): Promise<BarbaraBridgeResult> {
  try {
    const profile = await bridge()?.barbaraDeviceProfile?.() || { available: false, render: { max_dpr: 2, render_scale: 1 } };
    if (typeof window !== 'undefined') window.__BRASEIRO_DEVICE_PROFILE__ = profile;
    return profile;
  } catch (error) {
    return { available: false, render: { max_dpr: 2, render_scale: 1 }, error: error instanceof Error ? error.message : String(error) };
  }
}

export function currentBarbaraRenderPolicy(): { maxDpr: number; renderScale: number; qualityTier: string } {
  const profile: any = typeof window !== 'undefined' ? window.__BRASEIRO_DEVICE_PROFILE__ : null;
  const render = profile?.render || {};
  const maxDpr = Number.isFinite(Number(render.max_dpr)) ? Math.max(1, Number(render.max_dpr)) : 2;
  const renderScale = Number.isFinite(Number(render.render_scale)) ? Math.min(1, Math.max(.65, Number(render.render_scale))) : 1;
  return { maxDpr, renderScale, qualityTier: String(render.quality_tier || 'balanced') };
}

export async function inspectBarbaraTurn(hostState: Record<string, unknown>, text: string, importance = 'normal', inputChannel: InputChannel = 'PLAYER_ACTION'): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraSupervisorInspect;
  if (!fn) return { available: false, bypassed: true };
  try { return await fn(hostState, text, importance, inputChannel); }
  catch (error) { return { available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export async function finalizeBarbaraNarrative(input: {
  hostState: Record<string, unknown>; text: string; requestId: string; narration: string; importance?: string; inputChannel: InputChannel;
  resolution?: Record<string, unknown> | null; presentation?: Record<string, unknown> | null;
}): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraSupervisorFinalize;
  if (!fn) return { valid: true, available: false, bypassed: true };
  try { return await fn(input.hostState, input.text, input.requestId, input.narration, input.importance || 'normal', input.inputChannel, input.resolution || null, input.presentation || null); }
  catch (error) { return { valid: false, available: true, reason: 'supervisor_finalize_exception', error: error instanceof Error ? error.message : String(error) }; }
}

export async function getBarbaraAudioScenePlan(events: Record<string, unknown>[], context: Record<string, unknown>, narrationActive = false): Promise<BarbaraBridgeResult> {
  const fn = bridge()?.barbaraAudioScenePlan;
  if (!fn) return { available: false, bypassed: true };
  try { return await fn(events, context, {}, {}, narrationActive); }
  catch (error) { return { available: false, bypassed: true, error: error instanceof Error ? error.message : String(error) }; }
}

export async function getBarbaraVisiblePois(canonical: Record<string, unknown>[], campaign: Record<string, unknown>[], procedural: Record<string, unknown>[], gm = false): Promise<Record<string, unknown>[]> {
  const fn = bridge()?.barbaraVisiblePois;
  if (!fn) return [...canonical, ...campaign, ...procedural].filter((item: any) => gm || item?.hidden !== true || item?.discovered === true);
  try { return await fn(canonical, campaign, procedural, gm); }
  catch { return [...canonical, ...campaign, ...procedural].filter((item: any) => gm || item?.hidden !== true || item?.discovered === true); }
}
