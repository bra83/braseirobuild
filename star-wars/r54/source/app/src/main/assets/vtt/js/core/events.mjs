import { appendMerchantReceipt, consumeMerchantReceipt } from '../rules/merchantNegotiationReceipts.mjs';
export function compactStoredEvent(event) {
  if (!event || typeof event !== 'object') return event;
  if (event.type !== 'WORLD_UPDATED' || !event.payload?.world) return structuredClone(event);
  const world = event.payload.world;
  if (world.counts && !('npcs' in world) && !('locations' in world) && !('factions' in world) && !('history' in world)) return structuredClone(event);
  return {
    id: event.id,
    type: event.type,
    at: event.at,
    payload: {
      ...event.payload,
      world: {
        schemaVersion: world.schemaVersion ?? null,
        clock: { tick: Number(world.clock?.tick) || 0 },
        counts: {
          npcs: Object.keys(world.npcs ?? {}).length,
          locations: Object.keys(world.locations ?? {}).length,
          factions: Object.keys(world.factions ?? {}).length,
          events: Array.isArray(world.events) ? world.events.length : 0,
          hooks: Array.isArray(world.hooks) ? world.hooks.length : 0,
          aftermaths: Array.isArray(world.aftermaths) ? world.aftermaths.length : 0,
          encounters: Array.isArray(world.encounterHistory) ? world.encounterHistory.length : 0,
          history: Array.isArray(world.history) ? world.history.length : 0
        }
      }
    }
  };
}

export function applyEvent(state, event) {
  if (!event?.id) throw new Error('Evento sem identificador.');
  if (state.appliedActionIds.includes(event.id)) return state;

  const next = structuredClone(state);
  next.appliedActionIds.push(event.id);
  next.events.push(compactStoredEvent(event));
  next.updatedAt = event.at ?? new Date().toISOString();

  if (event.type === 'AOR_P177_ACTION') {
    next.master.aorP177Actions=[...(next.master.aorP177Actions??[]),structuredClone(event.payload.receipt)];
  }
  if (event.type === 'AOR_INTEL_ACTION') {
    next.master.aorIntelActions=[...(next.master.aorIntelActions??[]),structuredClone(event.payload.receipt)];
  }
  if (event.type === 'AOR_MECHANIC_ACTION') {
    next.master.mechanicFieldActions=[...(next.master.mechanicFieldActions??[]),structuredClone(event.payload.receipt)];
  }
  if (event.type === 'MERCHANT_NEGOTIATION_RECORDED') {
    next.master.merchantNegotiationReceipts=appendMerchantReceipt(next,event.payload.receipt);
  }
  if (event.type === 'MERCHANT_NEGOTIATION_CONSUMED') {
    next.master.merchantNegotiationReceipts=consumeMerchantReceipt(next,event.payload.receiptId,event.at);
  }
  if (event.type === 'CHECK_RESOLVED') next.lastCheck = event.payload;
  if (event.type === 'SOCIAL_CHECK_RESOLVED') {
    next.social = next.social ?? { history: [] };
    next.social.lastInfluence = structuredClone(event.payload);
    next.social.history = [...(next.social.history ?? []), structuredClone(event.payload)].slice(-50);
  }
  if (event.type === 'ADVANCED_CHECK_RESOLVED') {
    next.advancedChecks = next.advancedChecks ?? { history: [] };
    next.advancedChecks.last = structuredClone(event.payload);
    next.advancedChecks.history = [...(next.advancedChecks.history ?? []), structuredClone(event.payload)].slice(-50);
  }
  if (event.type === 'PLAYER_INTENT') next.scene.lastPlayerIntent = event.payload.text;
  if (event.type === 'SCENE_UPDATED' && event.payload.scene) next.scene = {...next.scene,...structuredClone(event.payload.scene)};
  if (event.type === 'CHARACTER_UPDATED') next.player.character = structuredClone(event.payload.character);
  if (event.type === 'MASTER_RULE_QUERY') {
    next.master.lastRuleQuery = event.payload.query;
    next.master.lastRuleResults = structuredClone(event.payload.results ?? []);
    next.master.lastRuleAuthority = structuredClone(event.payload.authority ?? null);
  }
  if (event.type === 'MASTER_CONTEXT_PREPARED') next.master.lastContext = structuredClone(event.payload);
  if (event.type === 'MASTER_OPENING_PREPARED') next.master = {...next.master, suggestedActions: structuredClone(event.payload.suggestedActions ?? []), openingRequest: structuredClone(event.payload.request ?? null)};
  if (event.type === 'EQUIPMENT_ACQUISITION_PENDING') {
    next.master.pendingAcquisition = structuredClone(event.payload.pending);
    if(event.payload.character)next.player.character=structuredClone(event.payload.character);
  }
  if (event.type === 'EQUIPMENT_ACQUISITION_RESOLVED') {
    next.master.pendingAcquisition = null;
    next.master.acquisitionHistory = [...(next.master.acquisitionHistory ?? []), structuredClone(event.payload.record)];
    if (event.payload.character) next.player.character = structuredClone(event.payload.character);
  }
  if (event.type === 'COMBAT_UPDATED') next.combat = structuredClone(event.payload.combat);
  if (event.type === 'SESSION_STARTED') {
    next.session = structuredClone(event.payload.session);
    if (event.payload.sessionId) next.sessionId = event.payload.sessionId;
    if (event.payload.scene) next.scene = {...next.scene,...structuredClone(event.payload.scene)};
    if (event.payload.character) next.player.character = structuredClone(event.payload.character);
    if (event.payload.group) next.group = structuredClone(event.payload.group);
    if (event.payload.environment) next.environment = structuredClone(event.payload.environment);
  }
  if (event.type === 'SESSION_PHASE') next.session = structuredClone(event.payload.session);
  if (event.type === 'SESSION_CHECK_RESOLVED') next.session = structuredClone(event.payload.session);
  if (event.type === 'SESSION_COMBAT_STARTED') next.session = structuredClone(event.payload.session);
  if (event.type === 'SESSION_COMBAT_ENDED') next.session = structuredClone(event.payload.session);
  if (event.type === 'SESSION_CLOSED') {
    next.session = structuredClone(event.payload.session);
    next.sessionHistory = [...(next.sessionHistory ?? []), structuredClone(event.payload.historyEntry)];
    if (event.payload.character) next.player.character = structuredClone(event.payload.character);
  }
  if (event.type === 'XP_GRANTED' && event.payload.character) next.player.character = structuredClone(event.payload.character);
  if (event.type === 'VEHICLES_UPDATED' && event.payload.vehicles) next.vehicles = structuredClone(event.payload.vehicles);
  if (event.type === 'GROUP_UPDATED' && event.payload.group) next.group = structuredClone(event.payload.group);
  if (event.type === 'CAMPAIGN_MODE_UPDATED' && event.payload.campaignMode) next.campaignMode = String(event.payload.campaignMode);
  if (event.type === 'REBEL_GROUP_UPDATED' && event.payload.rebelGroup) next.rebelGroup = structuredClone(event.payload.rebelGroup); // legacy replay only
  if (event.type === 'WORLD_UPDATED' && event.payload.world) next.world = structuredClone(event.payload.world);
  if (event.type === 'PLAYER_ROSTER_UPDATED' && event.payload.roster) {
    next.player.roster=structuredClone(event.payload.roster);
    const active=next.player.roster.entries?.find(x=>x?.id===next.player.roster.activeCharacterId);
    if(active?.character)next.player.character=structuredClone(active.character);
  }
  if (event.type === 'ENVIRONMENT_UPDATED' && event.payload.environment) next.environment = structuredClone(event.payload.environment);
  if (event.type === 'COMBAT_ACTION') {
    if (event.payload.combat) next.combat = structuredClone(event.payload.combat);
    if (event.payload.character) next.player.character = structuredClone(event.payload.character);
    if (event.payload.check) next.lastCheck = structuredClone(event.payload.check);
    if (event.payload.attack) next.combat.lastAttack = structuredClone(event.payload.attack);
    if (event.payload.group) next.group = structuredClone(event.payload.group);
    if (event.payload.environment) next.environment = structuredClone(event.payload.environment);
  }
  if (next.player?.character) {
    next.player.roster = next.player.roster && Array.isArray(next.player.roster.entries)
      ? next.player.roster : {schema:1,activeCharacterId:null,entries:[]};
    let active=next.player.roster.entries.find(x=>x?.id===next.player.roster.activeCharacterId);
    if(!active){
      const id=`pc:${String(next.player.character.name??'character').normalize('NFKD').replace(/[^A-Za-z0-9_-]/g,'-').replace(/-+/g,'-').slice(0,64)}`;
      active={id,character:structuredClone(next.player.character)};
      next.player.roster.entries.push(active);
      next.player.roster.activeCharacterId=id;
    }else active.character=structuredClone(next.player.character);
    next.player.roster.schema=1;
  }
  return next;
}
