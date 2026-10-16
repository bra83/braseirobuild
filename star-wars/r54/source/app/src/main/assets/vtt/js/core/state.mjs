import {validateJewelHoloVertigoConsumption} from '../adventure/jewelHoloParkEffects.mjs';
import {validateJewelStarportRoutes} from '../adventure/jewelStarportRoutes.mjs';
import {validateJewelStarportSecurity} from '../adventure/jewelStarportSecurity.mjs';
import {validateJewelEpisode3Rewards} from '../adventure/jewelEpisode3Rewards.mjs';
import {validateJewelBespinDeparture} from '../adventure/jewelBespinDeparture.mjs';
import {validateJewelStormIvPatrol} from '../adventure/jewelStormIvPatrol.mjs';
import {validateJewelHoloNarrativeClock} from '../adventure/jewelHoloNarrativeClock.mjs';
import {validateJewelStarportCloudCars} from '../adventure/jewelStarportCloudCars.mjs';
import {validateJewelShenAftermath} from '../adventure/jewelShenAftermath.mjs';
import {validateJewelArendProperty} from '../adventure/jewelArendProperty.mjs';
import {validateJewelArendPatrols} from '../adventure/jewelArendPatrols.mjs';
import {validateJewelPursuitTravel} from '../adventure/jewelPursuitTravel.mjs';
import {validateJewelResidenceWingResponse} from '../adventure/jewelResidenceWingResponse.mjs';
import {validateJewelLandoPardon} from '../adventure/jewelLandoPardon.mjs';
import {validateSWE02VehicleInitiative} from '../adventure/swe02VehicleInitiative.mjs';
migrateState = migrateState;
import * as ids_mjs_1 from "./ids.mjs";
import * as combat_mjs_1 from "../rules/combat.mjs";
import * as sessionFlow_mjs_1 from "../master/sessionFlow.mjs";
import * as vehicles_mjs_1 from "../rules/vehicles.mjs";
import * as fringeGroup_mjs_1 from "../rules/fringeGroup.mjs";
import * as world_mjs_1 from "../world/world.mjs";
import { validateTaskTimeLedger } from "../world/taskTimeLedger.mjs";
import { validatePublishedActionReceipts, validatePublishedActionCausality } from "../adventure/publishedActionReceipts.mjs";
import * as environment_mjs_1 from "../rules/environment.mjs";
import { validateAoRSocialState } from "../rules/aorSocialTalents.mjs";
import { compactStoredEvent } from "./events.mjs";
import { validateAoRIntelLedger } from '../rules/aorIntelActions.mjs';
import { validateP177Ledger } from '../rules/aorP177Actions.mjs';
import { validateMechanicFieldLedger } from '../rules/aorMechanicFieldActions.mjs';
import { validateMerchantReceiptLedger } from "../rules/merchantNegotiationReceipts.mjs";
import {validateP179CombatState} from '../rules/aorP179Reactions.mjs';
const safeSlicerId=v=>typeof v==='string'&&/^[A-Za-z0-9:_-]{1,120}$/.test(v);
function validateP178CombatState(state){
  for(const npc of state.combat?.adversaries??[]){
    const effect=npc?.aorCripplingBlow;
    if(effect===undefined)continue;
    if(!effect||typeof effect!=='object'||Array.isArray(effect)||
      typeof effect.sourceAttackId!=='string'||!/^[A-Za-z0-9:_-]{1,120}$/.test(effect.sourceAttackId)||
      !Number.isSafeInteger(effect.encounterRound)||effect.encounterRound<1||effect.strainPerMove!==1)
      throw Error('Golpe incapacitante AoR inválido no checkpoint.');
  }
  const registry=state.vehicles?.vehicles??{};
  for(const [id,vehicle] of Object.entries(registry)){
    for(const key of ['alliedToPlayer','closeToActive'])
      if(vehicle[key]!==undefined&&typeof vehicle[key]!=='boolean')throw Error('Confirmação de formação AoR inválida.');
    const effect=vehicle.roundEffects?.gainAdvantage;
    if(effect?.formationLeaderVehicleId!==undefined){
      const leader=effect.formationLeaderVehicleId;
      if(typeof leader!=='string'||leader===id||!registry[leader]||
         vehicle.alliedToPlayer!==true||vehicle.closeToActive!==true||
         typeof effect.targetVehicleId!=='string')throw Error('Formação AoR inválida no checkpoint.');
    }
  }
}
export const STATE_VERSION = 15;
export function createCampaign(name = 'Nova campanha') {
    const now = new Date().toISOString();
    return {
        schemaVersion: STATE_VERSION,
        campaignId: ids_mjs_1.newId('camp'),
        sessionId: ids_mjs_1.newId('sess'),
        name,
        createdAt: now,
        updatedAt: now,
        rulesProfile: 'EOTE_CORE',
        campaignMode: 'EOTE',
        player: { character: null, roster:{schema:1,activeCharacterId:null,entries:[]} },
        world: world_mjs_1.createWorld(),
        scene: { title: 'Abertura', narration: 'Mestre de IA ainda não conectado.', locationId: null, imageAssetId: null, imageSource: null, imageLocked: false, imagePrompt: null },
        combat: combat_mjs_1.createCombatState(),
        events: [],
        appliedActionIds: [],
        lastCheck: null,
        master: { corpusVersion: 1, lastRuleQuery: null, lastRuleResults: [], lastContext: null, pendingQuestion: null, suggestedActions: [], suggestedCheck: null, lastProposalRuleIds: [], lastRuleAuthority: null, pendingAcquisition: null, acquisitionHistory: [], merchantNegotiationReceipts: [], mechanicFieldActions: [], aorIntelActions: [], pendingEncounter: null },
        settings: { ttsEnabled: false, ttsRate: 1, masterProvider: 'none', masterEndpoint: null },
        session: sessionFlow_mjs_1.createSessionRuntime(1),
        sessionHistory: [],
        vehicles: vehicles_mjs_1.createVehicleRuntime(),
        group: fringeGroup_mjs_1.createFringeGroup(),
        environment: environment_mjs_1.createEnvironmentRuntime()
    };
}
export function migrateState(value) {
    if (!value || typeof value !== 'object')
        throw new Error('Estado inválido.');
    const version = Number(value.schemaVersion ?? 1);
    if (version > STATE_VERSION)
        throw new Error(`Versão de estado não suportada: ${value.schemaVersion}`);
    const next = structuredClone(value);
    if (version === 1) {
        next.combat = combat_mjs_1.createCombatState();
        next.schemaVersion = 2;
    }
    if (Number(next.schemaVersion) === 2) {
        next.schemaVersion = 3;
    }
    if (Number(next.schemaVersion) === 3) {
        next.schemaVersion = 4;
    }
    if (Number(next.schemaVersion) === 4) {
        next.schemaVersion = 5;
    }
    if (Number(next.schemaVersion) === 5) {
        next.schemaVersion = 6;
    }
    if (Number(next.schemaVersion) === 6) {
        next.schemaVersion = 7;
    }
    if (Number(next.schemaVersion) === 7) {
        next.schemaVersion = 8;
    }
    if (Number(next.schemaVersion) === 8) {
        next.schemaVersion = 9;
    }
    if (Number(next.schemaVersion) === 9) {
        next.schemaVersion = 10;
    }
    if (Number(next.schemaVersion) === 10) {
        next.schemaVersion = 11;
    }
    if (Number(next.schemaVersion) === 11) {
        next.schemaVersion = 12;
    }
    if (Number(next.schemaVersion) === 12) {
        next.schemaVersion = 13;
    }
    if (Number(next.schemaVersion) === 13) {
        next.schemaVersion = 14;
    }
    if (Number(next.schemaVersion) === 14) {
        next.schemaVersion = 15;
    }
    const combatDefaults = combat_mjs_1.createCombatState();
    next.combat = { ...combatDefaults, ...(next.combat ?? {}), playerTurn: { ...combatDefaults.playerTurn, ...(next.combat?.playerTurn ?? {}) }, adversaries: Array.isArray(next.combat?.adversaries) ? next.combat.adversaries : [], encounter: next.combat?.encounter ?? null, log: Array.isArray(next.combat?.log) ? next.combat.log : [] };
    next.master = { corpusVersion: 1, lastRuleQuery: null, lastRuleResults: [], lastContext: null, pendingQuestion: null, suggestedActions: [], suggestedCheck: null, lastProposalRuleIds: [], lastRuleAuthority: null, pendingAcquisition: null, acquisitionHistory: [], merchantNegotiationReceipts: [], mechanicFieldActions: [], aorIntelActions: [], pendingEncounter: null, ...(next.master ?? {}) };
    next.master.suggestedActions = Array.isArray(next.master.suggestedActions) ? next.master.suggestedActions.map(String).filter(Boolean).slice(0, 6) : [];
    next.master.lastRuleResults = Array.isArray(next.master.lastRuleResults) ? next.master.lastRuleResults : [];
    next.master.lastProposalRuleIds = Array.isArray(next.master.lastProposalRuleIds) ? next.master.lastProposalRuleIds : [];
    next.master.acquisitionHistory = Array.isArray(next.master.acquisitionHistory) ? next.master.acquisitionHistory : [];
    next.master.merchantNegotiationReceipts=next.master.merchantNegotiationReceipts??[];
    next.master.mechanicFieldActions=next.master.mechanicFieldActions??[];
    next.master.aorIntelActions=next.master.aorIntelActions??[];
    next.master.aorP177Actions=next.master.aorP177Actions??[];
    next.settings = { ttsEnabled: false, ttsRate: 1, masterProvider: 'none', masterEndpoint: null, ...(next.settings ?? {}) };
    next.settings.ttsRate = Math.max(0.6, Math.min(1.4, Number(next.settings.ttsRate) || 1));
    next.session = sessionFlow_mjs_1.normalizeSessionRuntime(next.session, 1);
    next.sessionHistory = Array.isArray(next.sessionHistory) ? next.sessionHistory : [];
    next.vehicles = vehicles_mjs_1.normalizeVehicleRuntime(next.vehicles);
    const inferredCampaignMode = next.campaignMode ?? (next.rulesProfile === 'AOR_CORE' || next.player?.character?.creation?.rulesProfile === 'AOR_CORE' ? 'AOR' : 'EOTE');
    next.campaignMode = ['EOTE','AOR','MIXED'].includes(String(inferredCampaignMode).toUpperCase()) ? String(inferredCampaignMode).toUpperCase() : 'EOTE';
    if (next.rulesProfile !== 'EOTE_CORE') {
        next.legacyRulesProfile = String(next.rulesProfile ?? 'unknown');
        next.rulesProfile = 'EOTE_CORE';
    }
    if (next.player?.character) {
        next.player.character.statusEffects = Array.isArray(next.player.character.statusEffects) ? next.player.character.statusEffects : [];
    }
    next.player = next.player ?? { character:null };
    next.player.roster = next.player.roster && Array.isArray(next.player.roster.entries)
        ? next.player.roster
        : { schema:1, activeCharacterId:null, entries:[] };
    if (next.player.character) {
        const roster=next.player.roster;
        let active=roster.entries.find(x=>x?.id===roster.activeCharacterId);
        if (!active) {
            const id=ids_mjs_1.newId('pc');
            active={id,character:structuredClone(next.player.character)};
            roster.entries=[...roster.entries,active];
            roster.activeCharacterId=id;
        } else {
            active.character=structuredClone(next.player.character);
        }
        roster.schema=1;
    }
    if (next.player?.character?.creation?.rulesProfile === 'AOR_CORE' && next.campaignMode === 'EOTE') {
        const c = next.player.character;
        c.legacyAoR = {
            ...(c.legacyAoR && typeof c.legacyAoR === 'object' ? c.legacyAoR : {}),
            duties: Array.isArray(c.duties) ? structuredClone(c.duties) : [],
            groupContributionRank: Number(c.groupContributionRank ?? 0),
            originalRulesProfile: 'AOR_CORE',
            note: 'Ficha AoR preservada como legado. Nenhum Duty foi convertido em Obligation.'
        };
        c.obligations = Array.isArray(c.obligations) ? c.obligations : [];
    }
    next.scene = { title: 'Abertura', narration: '', locationId: null, imageAssetId: null, imageSource: null, imageLocked: false, imagePrompt: null, ...(next.scene ?? {}) };
    next.group = next.group ? fringeGroup_mjs_1.normalizeFringeGroup(next.group) : fringeGroup_mjs_1.migrateRebelGroupToFringe(next.rebelGroup);
    if ('rebelGroup' in next)
        delete next.rebelGroup;
    // Validate the raw persisted task ledger BEFORE world normalization.
    // Legacy saves without a clock/ledger still follow the old migration path.
    if (next.world?.clock && (Object.prototype.hasOwnProperty.call(next.world, 'taskTimeApplications') ||
        Object.prototype.hasOwnProperty.call(next.world.clock, 'elapsedMinutes')))
        validateTaskTimeLedger(next.world);
    // Never let world normalization silently discard a damaged command ledger.
    validatePublishedActionReceipts(next.world);
    if(next.world?.publishedAdventure?.id==='jewel-of-yavin'){validateJewelLandoPardon(next.world.publishedAdventure);validateJewelShenAftermath(next.world.publishedAdventure);validateJewelArendProperty(next.world.publishedAdventure);validateJewelArendPatrols(next.world.publishedAdventure,next.world);validateJewelResidenceWingResponse(next.world.publishedAdventure,next.world);validateJewelPursuitTravel(next.world.publishedAdventure,next.world);validateJewelStormIvPatrol(next.world.publishedAdventure,next.vehicles);validateJewelStarportCloudCars(next.world.publishedAdventure,next.world,next.vehicles);validateJewelHoloNarrativeClock(next.world.publishedAdventure,next.world);validateJewelEpisode3Rewards(next.world.publishedAdventure);validateJewelBespinDeparture(next.world.publishedAdventure,next.world,next.vehicles,next.player?.character,next.group);validateJewelStarportSecurity(next.world.publishedAdventure,next.world);validateJewelStarportRoutes(next.world.publishedAdventure,next.world);}
    validatePublishedActionCausality(next.world);
    next.world = world_mjs_1.normalizeWorld(next.world);
    next.environment = environment_mjs_1.normalizeEnvironmentRuntime(next.environment);
    next.events = Array.isArray(next.events) ? next.events.map(compactStoredEvent).filter(Boolean) : [];
    validateMerchantReceiptLedger(next);
    validateP177Ledger(next);
    validateP178CombatState(next);
    validateSWE02VehicleInitiative(next.vehicles?.combat,next.player?.roster);
    return next;
}
export function validateState(value) {
    if (!value || typeof value !== 'object')
        throw new Error('Estado inválido.');
    if (value.schemaVersion !== STATE_VERSION)
        throw new Error(`Versão de estado não suportada: ${value.schemaVersion}`);
    for (const key of ['campaignId', 'sessionId', 'name', 'rulesProfile']) {
        if (typeof value[key] !== 'string' || !value[key])
            throw new Error(`Campo obrigatório inválido: ${key}`);
    }
    if (!Array.isArray(value.events) || !Array.isArray(value.appliedActionIds))
        throw new Error('Coleções de estado inválidas.');
    if (!value.combat || typeof value.combat !== 'object' || typeof value.combat.active !== 'boolean')
        throw new Error('Estado de combate inválido.');
    validateJewelHoloVertigoConsumption(value.combat,value.world?.publishedAdventure);
    if(!Number.isSafeInteger(value.combat.socialEncounterSerial)||value.combat.socialEncounterSerial<0)
        throw new Error('Identificador de encontro social inválido.');
    if(value.combat.npcCompanionTargetId!==undefined &&
       (typeof value.combat.npcCompanionTargetId!=='string'||!value.combat.npcCompanionTargetId.trim()||
        !(value.group?.companions??[]).some(m=>m.id===value.combat.npcCompanionTargetId)))
        throw new Error('Alvo NPC de companheiro inválido no checkpoint.');
    if(value.combat.hardHeadedRecovery!==undefined){
        const recovery=value.combat.hardHeadedRecovery;
        if(!recovery||typeof recovery!=='object'||typeof recovery.ready!=='boolean'||
           typeof recovery.attempted!=='boolean'||!Number.isSafeInteger(recovery.incapacitatedRound)||
           recovery.incapacitatedRound<1||!value.combat.active)
            throw new Error('Janela de Tenacidade aprimorada inválida no checkpoint.');
    }
    const combatOrders=value.combat.aorFieldCommandHistory??[];
    const commandTokens=value.combat.aorCommandTokens??[];
    if(!Array.isArray(combatOrders)||combatOrders.length>64||
       !Array.isArray(commandTokens)||commandTokens.length>128)
        throw new Error('Histórico de comando AoR inválido.');
    const ids=new Set(),rounds=new Set();
    for(const receipt of combatOrders){
        if(!receipt||typeof receipt.actionId!=='string'||!receipt.actionId||
           ids.has(receipt.actionId)||!Number.isSafeInteger(receipt.round)||receipt.round<1||
           rounds.has(receipt.round)||!Array.isArray(receipt.companionIds)||
           receipt.companionIds.length<1||receipt.companionIds.length>8||
           new Set(receipt.companionIds).size!==receipt.companionIds.length||
           typeof receipt.success!=='boolean'||typeof receipt.useImproved!=='boolean'||
           typeof receipt.grantFreeAction!=='boolean'||typeof receipt.freeActionGranted!=='boolean'||
           (receipt.freeActionGranted&&(!receipt.grantFreeAction||!receipt.useImproved||receipt.triumph<1||!receipt.success))||
           receipt.confirmedMedium!==true||
           !Number.isFinite(receipt.netSuccess)||!Number.isSafeInteger(receipt.triumph)||
           receipt.triumph<0||!Array.isArray(receipt.tokens)||
           (receipt.success?receipt.tokens.length!==receipt.companionIds.length:receipt.tokens.length!==0)||
           receipt.sessionId!==value.sessionId||
           receipt.tokens.some(t=>t.kind!==(receipt.freeActionGranted?'action':'maneuver')||
               !receipt.companionIds.includes(t.companionId)))
            throw new Error('Recibo de comando AoR inválido.');
        ids.add(receipt.actionId);rounds.add(receipt.round);
    }
    const tokenKeys=new Set();
    for(const token of commandTokens){
        const key=String(token?.round)+'\u0000'+String(token?.companionId);
        if(tokenKeys.has(key))throw new Error('Manobra comandada AoR duplicada.');
        tokenKeys.add(key);
        if(!token||!Number.isSafeInteger(token.round)||token.round<1||
           !['maneuver','action'].includes(token.kind)||typeof token.used!=='boolean'||
           !(value.group?.companions??[]).some(c=>c.id===token.companionId)||
           !combatOrders.some(r=>r.success&&r.round===token.round&&
               r.companionIds.includes(token.companionId)&&
               r.tokens.some(t=>t.companionId===token.companionId&&t.kind===token.kind)))
            throw new Error('Manobra comandada AoR sem recibo válido.');
    }
    if(value.combat.aorCoordinatedAssault!==undefined){
        const order=value.combat.aorCoordinatedAssault;
        if(!order||!Number.isSafeInteger(order.round)||order.round<1||
           !Array.isArray(order.companionIds)||!Array.isArray(order.consumed)||
           new Set(order.companionIds).size!==order.companionIds.length||
           new Set(order.consumed).size!==order.consumed.length||
           order.consumed.some(id=>!order.companionIds.includes(id)))
            throw new Error('Ataque coordenado AoR inválido.');
    }
    const fire=value.vehicles?.combat?.aorFireControl;
    if(fire!==undefined&&fire!==null){
        const v=value.vehicles?.combat;
        if(!v.active||!Number.isSafeInteger(fire.round)||fire.round<1||
           fire.round!==v.round||fire.sessionId!==value.sessionId||
           value.vehicles?.vehicles?.[fire.vehicleId]?.roundEffects?.controlledFire!==true||
           !(v.log??[]).some(e=>e.type==='AOR_FOGO_CONTROLADO'&&e.vehicleId===fire.vehicleId&&e.round===fire.round&&e.sessionId===fire.sessionId))
          throw Error('Fogo controlado AoR inválido.');
    }
    const slicer=value.combat?.aorSkilledSlicer;
    if(slicer!==undefined&&slicer!==null){
        if(!value.combat.active||!safeSlicerId(slicer.systemId)||
           slicer.sessionId!==value.sessionId||
           !Number.isSafeInteger(slicer.activatedRound)||slicer.activatedRound<1||
           !(value.combat.log??[]).some(e=>e.type==='AOR_SLICER_CONTROL'&&e.systemId===slicer.systemId&&e.round===slicer.activatedRound&&e.sessionId===slicer.sessionId&&e.triumphSpent===1))
          throw Error('Controle de sistema AoR inválido.');
    }
    const evasion=value.vehicles?.combat?.brilliantEvasion;
    if(evasion!==undefined&&evasion!==null){
        const v=value.vehicles?.combat;
        if(v.brilliantEvasionAttempted!==true||!v.active||
           typeof evasion.targetVehicleId!=='string'||
           !value.vehicles.vehicles?.[evasion.targetVehicleId]||
           !Number.isSafeInteger(evasion.startsAtRound)||evasion.startsAtRound<1||
           !Number.isSafeInteger(evasion.untilRound)||
           evasion.untilRound<evasion.startsAtRound||
           evasion.untilRound>evasion.startsAtRound+5)
            throw new Error('Evasão impecável AoR inválida no checkpoint.');
    }
    if(value.vehicles?.combat?.brilliantEvasionAttempted!==undefined&&
       typeof value.vehicles.combat.brilliantEvasionAttempted!=='boolean')
        throw new Error('Tentativa de Evasão impecável inválida.');
    validateAoRSocialState(value);
    if (!value.combat.playerTurn || !Number.isInteger(value.combat.playerTurn.maneuversUsed))
        throw new Error('Turno de combate inválido.');
    if (!value.master || typeof value.master !== 'object' || !Array.isArray(value.master.lastRuleResults))
        throw new Error('Estado do Mestre inválido.');
    validateMerchantReceiptLedger(value);
    validateMechanicFieldLedger(value);
    validateAoRIntelLedger(value);
    validateP177Ledger(value);
    validateP178CombatState(value);
    validateP179CombatState(value);
    if (!value.session || typeof value.session !== 'object' || !['nao_iniciada', 'ativa', 'encerrada'].includes(value.session.status))
        throw new Error('Estado de sessão inválido.');
    if (!Array.isArray(value.sessionHistory))
        throw new Error('Histórico de sessões inválido.');
    if (!['EOTE','AOR','MIXED'].includes(value.campaignMode))
        throw new Error('Modo de campanha inválido.');
    if (!value.vehicles || typeof value.vehicles !== 'object' || !value.vehicles.combat || typeof value.vehicles.combat.active !== 'boolean')
        throw new Error('Estado de naves/veículos inválido.');
    fringeGroup_mjs_1.validateFringeGroup(value.group);
    validatePublishedActionReceipts(value.world);
    if(value.world?.publishedAdventure?.id==='jewel-of-yavin'){validateJewelLandoPardon(value.world.publishedAdventure);validateJewelShenAftermath(value.world.publishedAdventure);validateJewelArendProperty(value.world.publishedAdventure);validateJewelArendPatrols(value.world.publishedAdventure,value.world);validateJewelResidenceWingResponse(value.world.publishedAdventure,value.world);validateJewelPursuitTravel(value.world.publishedAdventure,value.world);validateJewelStarportCloudCars(value.world.publishedAdventure,value.world,value.vehicles);validateJewelHoloNarrativeClock(value.world.publishedAdventure,value.world);validateJewelEpisode3Rewards(value.world.publishedAdventure);validateJewelBespinDeparture(value.world.publishedAdventure,value.world,value.vehicles,value.player?.character,value.group);validateJewelStarportSecurity(value.world.publishedAdventure,value.world);validateJewelStarportRoutes(value.world.publishedAdventure,value.world);}
    validatePublishedActionCausality(value.world);
    const world = world_mjs_1.normalizeWorld(value.world);
    validateTaskTimeLedger(value.world?.clock ? value.world : world);
    if (world.schemaVersion !== 6 || !world.clock || !world.factions || typeof world.factions !== 'object' || !Array.isArray(world.events) || !Array.isArray(world.hooks) || !Array.isArray(world.history))
        throw new Error('Estado de mundo inválido.');
    return true;
}
