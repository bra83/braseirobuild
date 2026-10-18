import {confirmJewelStarportRoute,registerJewelStarportDestruction} from '../adventure/jewelStarportRoutes.mjs';
import {attemptJewelStarportShipAccess,jewelUnpreparedSpaceUpgrade} from '../adventure/jewelStarportSecurity.mjs';
import {transferJewelCustody} from '../adventure/jewelCustodyTransfers.mjs';
import {recordJewelArendOutcome,recordJewelArisAlliance} from '../adventure/jewelShenAftermath.mjs';
import {prepareJewelArendProperty} from '../adventure/jewelArendProperty.mjs';
import {recordJewelArendPatrol,jewelArendPatrolStatus} from '../adventure/jewelArendPatrols.mjs';
import {recordJewelPursuitTravelCheck,prepareJewelPursuitGroup} from '../adventure/jewelPursuitTravel.mjs';
import {prepareJewelResidenceWingResponse} from '../adventure/jewelResidenceWingResponse.mjs';
import {grantJewelEpisode3Reward} from '../adventure/jewelEpisode3Rewards.mjs';
import {registerJewelCustody,registerJewelBoarding,prepareJewelEscape,finalizeJewelEscape} from '../adventure/jewelBespinDeparture.mjs';
import {deployJewelStormIvPatrol,beginJewelStormIvCombat,resolveJewelStormIvCombat,clearJewelStormIvPatrol,BESPIN_PILOT} from '../adventure/jewelStormIvPatrol.mjs';
import {arriveJewelStarportSquadron,beginJewelStarportCloudCombat,resolveJewelStarportCloudCombat,clearJewelStarportSquadron} from '../adventure/jewelStarportCloudCars.mjs';
import {grantJewelLandoPardon} from '../adventure/jewelLandoPardon.mjs';
import {prepareJewelHoloFaction} from '../adventure/jewelHoloFactions.mjs';
import {applyJewelHoloEffect,holoParkRangedCover,holoParkThrantaBoost,holoParkShadowDefense,holoParkCoruscantCover,clearHoloParkCoruscantCover,reserveJewelHoloNpcThreat,holoParkSpotlightSetback,clearHoloParkSpotlightAtTurnEnd,holoParkTransportSetback,clearHoloParkTransportOnMovement,clearHoloParkTransportCompanion,assertJewelHoloWeaponUsable,recoverJewelHoloDroppedWeapon,settleJewelHoloUnusedNpcThreat,settleJewelHoloUnusedNpcDespair,reserveJewelHoloNpcDespair,holoParkFountainDefense,assertJewelHoloTargetVisible,confirmJewelHoloNpcMovement,confirmJewelHoloNpcVisible,consumeJewelHoloVertigo,attachJewelHoloVertigoRoll,augmentJewelKashyyykInsectRoll,recordJewelKashyyykInsect} from '../adventure/jewelHoloParkEffects.mjs';
import {enterJewelHoloNarrativeClock,advanceJewelHoloNarrativeClock,endJewelHoloNarrativeClock} from '../adventure/jewelHoloNarrativeClock.mjs';
import {enterJewelHoloPark,recordJewelHoloParkRound,confirmJewelHoloParkOutcome} from '../adventure/jewelHoloPark.mjs';
import {investigateJewelArendHoldings,visitJewelArendHolding} from '../adventure/jewelArendRevenge.mjs';
import {resolveJewelArrow7Chase,confirmJewelArrow7ChaseOutcome} from '../adventure/jewelArrow7Chase.mjs';
import {queueJewelArrow7Turret,arrow7WeaponForNpcTurn,consumeJewelArrow7Turret} from '../adventure/jewelArrow7Turret.mjs';
import { rollJewelGrandPrixPilotCheck } from '../adventure/jewelGrandPrixDice.mjs';
import { queueWingGuardTactic, applyQueuedWingGuardTactic, wingGuardRangedDefense, consumeWingGuardTacticalBoost } from '../rules/wingGuardTactics.mjs';
import {performJewelCompanionImpact} from '../adventure/jewelCompanionImpact.mjs';
import { safeIntelId, KNOWLEDGE_SKILLS } from '../rules/aorIntelActions.mjs';
import { safeP177Id } from '../rules/aorP177Actions.mjs';
import {npcBlastReceipt,assertBlastReceipt} from '../rules/aorP179Reactions.mjs';
import { mechanicFieldPreflight, mechanicFieldReceipt, assertOwnedEquipmentOperational } from '../rules/aorMechanicFieldActions.mjs';
import { advanceBeyondTheRimImperialClock, beyondTheRimImperialClockStatus } from '../adventure/beyondTheRim.mjs';
import { longArmShipSmellSetback,recordLongArmOldLylekLootGrant,recordLongArmOldLylekPilotStrain,recordLongArmOldLylekRuse,chooseLongArmOldLylekTactics,triggerLongArmOldLylekAdvantage,resolveLongArmOldLylekRetreat,revealLongArmOldLylekDeveloperDetails,createLongArmOldLylekMapState,assignLongArmOldLylekMapLocation,selectLongArmTrexCarryover,chooseLongArmOldLylekCrew,chooseLongArmOldLylekRoute,proceedLongArmOldLylekBluff,approachLongArmOldLylekOnFoot,
    recordLongArmOldLylekCrashCheck,confirmLongArmOldLylekCrashCrew,inspectLongArmOldLylekSpeeder,recordLongArmOldLylekWarning,persistLongArmOldLylek } from '../adventure/longArmOfTheHutt.mjs';
import {beginThwheekInterception,resolveThwheekInterception,thwheekTactics,THWHEEK_ID,THWHEEK_CONTEXT} from '../adventure/swe02ThwheekP239.mjs';
import {rollSWE02VehicleInitiative} from '../adventure/swe02VehicleInitiative.mjs';
const SWE02_INITIATIVE_ADVANCE=Symbol('SWE02 initiative round transition');
import {nabatMarketCatalog,nabatPurchase,requestNabatSpaceportWatch,deliverNabatNightAlert,recordNabatMorningResponse,nabatShipRepair,nabatSafehouseCare} from '../adventure/swe02NabatP240.mjs';
import {nynCriticalCare,nynNegotiate,nynRewardOptions,distributeNynReward,bribeNewMeenThug} from '../adventure/swe02SocialP241.mjs';
import {adjudicateNewMeenDeaths,coerceNewMeenThugs,openDrombbSafe} from '../adventure/swe02NewMeenP242.mjs';
import {prepareNewMeenEscort,departNewMeenEscort,arriveNewMeenEscort,abandonNewMeenEscort,armNewMeenMilitia,applyNewMeenMilitiaAbstractRound,returnNewMeenMilitiaWeapon} from '../adventure/swe02EvacuationP243.mjs';
import {prepareSWE02AnattaDossier,resolveSWE02TeemoWithoutCombat,claimSWE02TeemoStrongbox,shootSWE02Chandelier} from '../adventure/swe02Act3Resolution.mjs';
import {recruitSWE02Gladiators,queueSWE02ThroneAlarm,triggerSWE02ThroneAlarmOnNpcTurn,applySWE02ThroneReinforcementsAtRoundBoundary,validateSWE02ThroneAlarm} from '../adventure/swe02Act3PalaceWip.mjs';
import {buySWE02AnattaInformation,ANATTA_CATALOG} from '../adventure/swe02Act2Anatta.mjs';
import {askSWE02Piddock,showSWE02PiddockTradeInterest,hireSWE02Vio,bribeSWE02Vrixxtt,agreeSWE02PiddockArms,deliverSWE02PiddockArms,piddockArmsCatalog} from '../adventure/swe02Act2ContractsWip.mjs';
import {listSWE02PalaceOperations,executeSWE02PalaceOperation} from '../adventure/swe02PalaceOperations.mjs';
import * as state_mjs_1 from "./state.mjs";
import * as events_mjs_1 from "./events.mjs";
import * as ids_mjs_1 from "./ids.mjs";
import * as narrativeDice_mjs_1 from "../rules/narrativeDice.mjs";
import * as starterPresets_mjs_1 from "../rules/starterPresets.mjs";
import * as character_mjs_1 from "../rules/character.mjs";
import * as combat_mjs_1 from "../rules/combat.mjs";
import * as health_mjs_1 from "../rules/health.mjs";
import * as weapons_mjs_1 from "../rules/weapons.mjs";
import * as attackDifficulty_mjs_1 from "../rules/attackDifficulty.mjs";
import * as recovery_mjs_1 from "../rules/recovery.mjs";
import * as generalGearEffects_mjs_1 from "../rules/generalGearEffects.mjs";
import * as critical_mjs_1 from "../rules/critical.mjs";
import * as initiative_mjs_1 from "../rules/initiative.mjs";
import * as adversaries_mjs_1 from "../rules/adversaries.mjs";
import * as encounter_mjs_1 from "../rules/encounter.mjs";
import * as localMaster_mjs_1 from "../master/localMaster.mjs";
import * as proposal_mjs_1 from "../master/proposal.mjs";
import * as provider_mjs_1 from "../master/provider.mjs";
import * as sessionFlow_mjs_1 from "../master/sessionFlow.mjs";
import * as progression_mjs_1 from "../rules/progression.mjs";
import * as eoteCharacterCreation_mjs_1 from "../rules/eoteCharacterCreation.mjs";
import * as characterCreation_mjs_1 from "../rules/characterCreation.mjs";
import * as vehicles_mjs_1 from "../rules/vehicles.mjs";
import * as fringeGroup_mjs_1 from "../rules/fringeGroup.mjs";
import * as talents_mjs_1 from "../rules/talents.mjs";
import * as force_mjs_1 from "../rules/force.mjs";
import * as acquisition_mjs_1 from "../rules/acquisition.mjs";
import * as rarity_mjs_1 from "../rules/availability.mjs";
import * as economy_mjs_1 from "../rules/economy.mjs";
import { merchantTarget, issueMerchantReceipt, merchantReceiptFor } from '../rules/merchantNegotiationReceipts.mjs';
import * as equipmentCatalog_mjs_1 from "../rules/equipmentCatalog.mjs";
import * as encumbrance_mjs_1 from "../rules/encumbrance.mjs";
import * as statusEffects_mjs_1 from "../rules/statusEffects.mjs";
import * as destiny_mjs_1 from "../rules/destiny.mjs";
import * as aorSocial_mjs_1 from "../rules/aorSocialTalents.mjs";
import * as obligation_mjs_1 from "../rules/obligation.mjs";
import * as duty_mjs_1 from "../rules/duty.mjs";
import * as fear_mjs_1 from "../rules/fear.mjs";
import * as sceneLibrary_mjs_1 from "../library/sceneLibrary.mjs";
import * as world_mjs_1 from "../world/world.mjs";
import { recordResolvedTaskTime, validateTaskTimeLedger } from "../world/taskTimeLedger.mjs";
import { recordPublishedAdventureTaskTime } from "../adventure/publishedAdventureUaeBridge.mjs";
import { publishedCommandRequest, findPublishedActionReceipt, appendPublishedActionReceipt, currentPublishedAdventureRunKey } from "../adventure/publishedActionReceipts.mjs";
import { AdventureRuntime } from "../adventure/adventureRuntime.mjs";
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from "../adventure/publishedAdventureUaeBridge.mjs";
import * as sandbox_mjs_1 from "../world/sandbox.mjs";
import * as publishedAdventureRegistry_mjs_1 from "../adventure/publishedAdventureRegistry.mjs";
import * as missionOpportunityEngine from "../world/missionOpportunityEngine.mjs";
import * as originalAdventureCreator from "../master/originalAdventureCreator.mjs";
import * as cybernetics_mjs_1 from "../rules/cybernetics.mjs";
import * as astrogation_mjs_1 from "../rules/astrogation.mjs";
import * as attachments_mjs_1 from "../rules/attachments.mjs";
import * as environment_mjs_1 from "../rules/environment.mjs";
function requireCharacter(character) {
    if (!character)
        throw new Error('Carregue um personagem antes desta ação.');
    return character;
}
function characterNaturalSoak(character) {
    const c=requireCharacter(character);
    const brawn=Number(c.characteristics?.fortaleza??0);
    const species=String(c.species??c.speciesKey??'').trim().toLocaleLowerCase('pt-BR');
    let speciesBonus=0;
    if(species==='droide'||species==='droid') speciesBonus=1;
    else {
        for(const [key,spec] of Object.entries(eoteCharacterCreation_mjs_1.EOTE_SPECIES??{})){
            if(key.toLocaleLowerCase('pt-BR')===species||String(spec?.label??'').toLocaleLowerCase('pt-BR')===species){speciesBonus=Number(spec?.soakBonus??0);break;}
        }
    }
    const talentBonus=Number(talents_mjs_1.talentRankByOriginal?.(c,'RESISTENTE')??0);
    const cyberBonus=(cybernetics_mjs_1.installedCybernetics(c)??[]).reduce((sum,item)=>sum+Number(item?.modifiers?.soakBonus??0),0);
    return Math.max(0,brawn+speciesBonus+talentBonus+cyberBonus);
}
function assertCanAct(character) {
    requireCharacter(character);
    if (health_mjs_1.getHealthStatus(character).unconscious)
        throw new Error('O personagem está incapacitado e não pode agir agora.');
}
function criticalActionState(character) {
    return critical_mjs_1.criticalCheckModifiers(character, { consume: false });
}
function assertCanPerformAction(character) {
    assertCanAct(character);
    const critical = criticalActionState(character);
    const status = statusEffects_mjs_1.statusModifiers(character);
    if (critical.cannotAct || status.cannotAct)
        throw new Error('O personagem está staggered e não pode realizar ações agora.');
    return critical;
}
function campaignMode(state) { return ['EOTE','AOR','MIXED'].includes(state?.campaignMode) ? state.campaignMode : 'EOTE'; }
function modeAllowsObligation(state) { return campaignMode(state) === 'EOTE' || campaignMode(state) === 'MIXED'; }
function modeAllowsDuty(state) { return campaignMode(state) === 'AOR' || campaignMode(state) === 'MIXED'; }
function assertProgressionAllowed(state) {
    const character = requireCharacter(state.player.character);
    if (modeAllowsObligation(state) && obligation_mjs_1.obligationTotal(state.group, character) > 100)
        throw new Error('Obligation acima de 100 impede gastar PE até o grupo reduzir a pressão.');
    return character;
}
function applyActionCriticalStrain(character, critical) {
    const cost = Math.max(0, Number(critical?.strainPerAction ?? 0));
    return cost > 0 ? health_mjs_1.sufferStrain(character, cost).character : character;
}
function assertCriticalManeuverAllowed(character, combat, method) {
    const critical = criticalActionState(character);
    const used = Number(combat?.playerTurn?.maneuversUsed ?? 0);
    if (critical.maxManeuvers !== null && used >= critical.maxManeuvers)
        throw new Error('O Ferimento Crítico limita o personagem a uma manobra neste turno.');
    if (critical.noFreeManeuver && used === 0 && method === 'normal')
        throw new Error('O Ferimento Crítico remove a manobra gratuita; use a ação ou sofra Tensão quando permitido.');
    if (critical.cannotVoluntarilySufferStrain && method === 'strain')
        throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para obter manobra adicional.');
    return critical;
}
function spendPlayerManeuver(character, combat, { method = 'normal', label = 'Manobra' } = {}) {
    let nextCharacter = requireCharacter(character);
    assertCanAct(nextCharacter);
    assertPlayerInitiativeTurn(combat);
    const status = statusEffects_mjs_1.statusModifiers(nextCharacter);
    if (status.cannotManeuver)
        throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
    const critical = assertCriticalManeuverAllowed(nextCharacter, combat, method);
    const burden = encumbrance_mjs_1.encumbranceStatus(nextCharacter);
    const used = Number(combat?.playerTurn?.maneuversUsed ?? 0);
    const noFreeManeuver = used === 0 && (Boolean(critical.noFreeManeuver) || Boolean(burden.severelyEncumbered));
    if (noFreeManeuver && method === 'normal') {
        if (burden.severelyEncumbered)
            throw new Error('A carga elimina a manobra gratuita; use a ação ou sofra 2 de Tensão.');
        throw new Error('O Ferimento Crítico remove a manobra gratuita; use a ação ou sofra 2 de Tensão quando permitido.');
    }
    let movement;
    let strainCost = 0;
    if (noFreeManeuver) {
        movement = combat_mjs_1.spendManeuver(combat, { method: 'normal', label });
        if (method === 'strain') {
            strainCost = combat_mjs_1.EXTRA_MANEUVER_STRAIN_COST;
        } else if (method === 'action') {
            if (movement.combat.playerTurn.actionUsed)
                throw new Error('A ação já foi usada e não pode pagar esta manobra.');
            movement.combat.playerTurn.actionUsed = true;
        } else {
            throw new Error('Sem manobra gratuita: use a ação ou sofra 2 de Tensão.');
        }
    } else {
        movement = combat_mjs_1.spendManeuver(combat, { method, label });
        strainCost = movement.strainCost;
    }
    if (strainCost > 0)
        nextCharacter = health_mjs_1.sufferStrain(nextCharacter, strainCost,{voluntary:true}).character;
    if (label !== 'Mirar') {
        movement.combat.pendingAttackBoost = 0;
        delete movement.combat.pendingAim;
    }
    return { combat: movement.combat, character: nextCharacter, strainCost, critical, encumbrance: burden };
}
function clearPlayerAim(combat) {
    const next = clone(combat);
    next.pendingAttackBoost = 0;
    delete next.pendingAim;
    return next;
}
function expireStartOfPlayerTurnTalentEffects(character) {
    if (!character) return character;
    const next=clone(character);
    if(next.combatEffects){
        delete next.combatEffects.sideStep;
        delete next.combatEffects.feint;
        delete next.combatEffects.defensiveStanceTalent;
        if(!Object.keys(next.combatEffects).length) delete next.combatEffects;
    }
    return next;
}

function talentSessionUses(character) {
    return character?.sessionEffects?.talentUses && typeof character.sessionEffects.talentUses === 'object' ? character.sessionEffects.talentUses : {};
}
function talentUsedThisSession(character, key) {
    return Boolean(talentSessionUses(character)[String(key)]);
}
function markTalentUsedThisSession(character, key) {
    const next = clone(requireCharacter(character));
    next.sessionEffects = { ...(next.sessionEffects ?? {}), talentUses: { ...talentSessionUses(next), [String(key)]: true } };
    return next;
}
function resetTalentSessionUses(character) {
    const next = clone(requireCharacter(character));
    if (next.sessionEffects?.talentUses) {
        delete next.sessionEffects.talentUses;
        if (!Object.keys(next.sessionEffects).length) delete next.sessionEffects;
    }
    return next;
}

function assertPlayerInitiativeTurn(combat) {
    if (!combat?.active || !combat.encounter?.slots?.length)
        return;
    const slot = encounter_mjs_1.currentSlot(combat);
    if (!slot || slot.side !== 'player' || !combat.encounter.waitingForPlayer || combat.encounter.outcome) {
        throw new Error('Ainda não é a posição de iniciativa do jogador.');
    }
}

function guardedStanceActive(character, round) {
    const effect=character?.combatEffects?.guardedStance;
    return Boolean(effect && Number(round??0) <= Number(effect.untilRound??-1));
}
function isMeleeWeapon(weapon) {
    return ['pelea','armas_cuerpo_a_cuerpo','sabre_de_luz'].includes(String(weapon?.skill??''));
}
function personalRangeTransitionManeuvers(from, to) {
    const pair = new Set([String(from ?? ''), String(to ?? '')]);
    if (pair.has('Médio') && pair.has('Longo')) return 2;
    if (pair.has('Longo') && pair.has('Extremo')) return 2;
    return 1;
}
function personalRangeIndex(range) {
    const value=String(range??'').trim().toLocaleLowerCase('pt-BR');
    const aliases=new Map([['interação',0],['interacao',0],['engaged',0],['curto',1],['short',1],['médio',2],['medio',2],['medium',2],['longo',3],['long',3],['extremo',4],['extreme',4]]);
    return aliases.has(value)?aliases.get(value):null;
}
function applyMasterWorldOperations(world, operations = []) {
    let next = structuredClone(world);
    const applied = [];
    for (const op of operations) {
        if (op.type === 'create_location') {
            const location = world_mjs_1.createLocation(op);
            next = world_mjs_1.upsertLocation(next, location);
            applied.push({ type: op.type, id: location.id });
        }
        else if (op.type === 'create_faction') {
            const faction = world_mjs_1.createFaction(op);
            next = world_mjs_1.upsertFaction(next, faction);
            applied.push({ type: op.type, id: faction.id });
        }
        else if (op.type === 'create_npc') {
            if (op.locationId && !next.locations?.[op.locationId])
                throw new Error(`Local não encontrado para novo PNPC: ${op.locationId}`);
            if (op.factionId && !next.factions?.[op.factionId])
                throw new Error(`Facção não encontrada para novo PNPC: ${op.factionId}`);
            const npc = world_mjs_1.createNpc(op);
            next = world_mjs_1.upsertNpc(next, npc);
            applied.push({ type: op.type, id: npc.id });
        }
        else if (op.type === 'move_npc') {
            if (op.locationId && !next.locations?.[op.locationId])
                throw new Error(`Local não encontrado: ${op.locationId}`);
            next = world_mjs_1.moveNpc(next, op.npcId, op.locationId);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_npc_faction') {
            if (op.factionId && !next.factions?.[op.factionId])
                throw new Error(`Facção não encontrada: ${op.factionId}`);
            next = world_mjs_1.setNpcFaction(next, op.npcId, op.factionId);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_npc_goal') {
            next = world_mjs_1.setNpcGoal(next, op.npcId, op.goal);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_npc_relation') {
            next = world_mjs_1.setNpcRelation(next, op.npcId, op.relation);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_npc_status') {
            next = world_mjs_1.setNpcStatus(next, op.npcId, op.status);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_npc_visibility') {
            next = world_mjs_1.setNpcVisibility(next, op.npcId, op.knownToPlayer);
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'record_npc_fact') {
            next = world_mjs_1.recordNpcFact(next, op.npcId, { text: op.text, knownToPlayer: op.knownToPlayer });
            applied.push({ type: op.type, id: op.npcId });
        }
        else if (op.type === 'set_faction_objective') {
            next = world_mjs_1.setFactionObjective(next, op.factionId, op.objective);
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'set_faction_status') {
            next = world_mjs_1.setFactionStatus(next, op.factionId, op.status);
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'add_faction_resource') {
            next = world_mjs_1.addFactionResource(next, op.factionId, op.resource);
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'remove_faction_resource') {
            next = world_mjs_1.removeFactionResource(next, op.factionId, op.resource);
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'set_faction_relation') {
            next = world_mjs_1.setFactionRelation(next, op.factionId, op.targetFactionId, op.relation);
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'record_faction_fact') {
            next = world_mjs_1.recordFactionFact(next, op.factionId, { text: op.text, knownToPlayer: op.knownToPlayer });
            applied.push({ type: op.type, id: op.factionId });
        }
        else if (op.type === 'create_faction_plan') {
            const result = world_mjs_1.createFactionPlan(next, op.factionId, { title: op.title, objective: op.objective, steps: op.steps, knownToPlayer: op.knownToPlayer });
            next = result.world;
            applied.push({ type: op.type, id: result.plan.id, factionId: op.factionId });
        }
        else if (op.type === 'set_faction_plan_status') {
            next = world_mjs_1.setFactionPlanStatus(next, op.factionId, op.planId, op.status);
            applied.push({ type: op.type, id: op.planId, factionId: op.factionId });
        }
        else if (op.type === 'advance_faction_plan') {
            const result = world_mjs_1.advanceFactionPlan(next, op.factionId, op.planId);
            next = result.world;
            applied.push({ type: op.type, id: op.planId, factionId: op.factionId });
        }
        else if (op.type === 'schedule_event') {
            const scheduled = world_mjs_1.scheduleWorldEvent(next, { type: op.eventType, dueTick: op.dueTick, payload: op.payload, dedupeKey: op.dedupeKey });
            next = scheduled.world;
            applied.push({ type: op.type, id: scheduled.event.id, created: scheduled.created });
        }
        else if (op.type === 'resolve_hook') {
            next = world_mjs_1.resolveWorldHook(next, op.hookId, { resolution: op.resolution, dismiss: op.dismiss });
            applied.push({ type: op.type, id: op.hookId });
        }
        else if (op.type === 'close_aftermath') {
            next = world_mjs_1.closeWorldAftermath(next, op.aftermathId, { resolution: op.resolution });
            applied.push({ type: op.type, id: op.aftermathId });
        }
        else if (op.type === 'create_opportunity') {
            const result = sandbox_mjs_1.createOpportunity(next, op);
            next = result.world;
            applied.push({ type: op.type, id: result.opportunity.id });
        }
        else if (op.type === 'set_opportunity_status') {
            const result = sandbox_mjs_1.setOpportunityStatus(next, op.opportunityId, op.status, { resolution: op.resolution });
            next = result.world;
            applied.push({ type: op.type, id: op.opportunityId, status: result.opportunity.status });
        }
        else if (op.type === 'advance_clock') {
            const result = sandbox_mjs_1.advanceWorldClock(next, op.clockId, { steps: op.steps });
            next = result.world;
            applied.push({ type: op.type, id: op.clockId, progress: result.clock.progress });
        }
        else if (op.type === 'set_market') {
            next = sandbox_mjs_1.setMarketProfile(next, op.locationId, { rarityModifier: op.rarityModifier, restrictedAccess: op.restrictedAccess, tags: op.tags, notes: op.notes });
            applied.push({ type: op.type, id: op.locationId });
        }
        else
            throw new Error(`Operação de mundo não suportada: ${op.type}`);
    }
    return { world: next, applied };
}
function initiativeSkillKey(value) {
    if (!['frialdad', 'alerta'].includes(value))
        throw new Error('A iniciativa deve usar Frieza ou Alerta.');
    return value;
}
function difficultyWithUpgrades(difficulty, challenge, upgrades) {
    let d = Math.max(0, Number(difficulty) || 0);
    let c = Math.max(0, Number(challenge) || 0);
    let n = Math.max(0, Number(upgrades) || 0);
    while (n-- > 0) {
        if (d > 0) {
            d -= 1;
            c += 1;
        }
        else
            d += 1;
    }
    return { difficulty: d, challenge: c };
}
function difficultyWithDowngrades(difficulty, challenge, downgrades) {
    let d = Math.max(0, Number(difficulty) || 0);
    let c = Math.max(0, Number(challenge) || 0);
    let n = Math.max(0, Number(downgrades) || 0);
    while (n-- > 0) {
        if (c > 0) { c -= 1; d += 1; }
        else if (d > 0) d -= 1;
    }
    return { difficulty: d, challenge: c };
}
function consumeVehicleCopilot(vehicle) {
    const next=clone(vehicle), effect=next?.roundEffects?.copilot;
    const downgrades=Math.max(0,Number(effect?.downgrades??0));
    if(next?.roundEffects&&effect) delete next.roundEffects.copilot;
    return {vehicle:next,downgrades};
}
function defenseSetback(character, weapon, combat=null) {
    const melee = weapon?.skill === 'pelea' || weapon?.skill === 'armas_cuerpo_a_cuerpo';
    const base=Number(character?.derived?.defense?.[melee ? 'melee' : 'ranged'] ?? 0);
    const cover=melee?0:Number(character?.combatEffects?.coverRangedDefense??0);
    const active=(character?.weapons??[]).find(w=>w?.active||w?.equipped||String(w?.id??w?.name??'')===String(character?.activeWeaponId??''));
    const weaponDefense=weapons_mjs_1.weaponDefenseModifiers(active);
    return Math.max(0,base+cover+Number(melee?weaponDefense.melee:weaponDefense.ranged)+holoParkRangedCover(combat,weapon)+holoParkCoruscantCover(combat,weapon));
}
function targetDefenseSetback(target, weapon, reduction = 0, round = 0) {
    if (!target) return 0;
    const melee = weapon?.skill === 'pelea' || weapon?.skill === 'armas_cuerpo_a_cuerpo';
    const raw = Number(target?.derived?.defense?.[melee ? 'melee' : 'ranged'] ?? target?.defense?.[melee ? 'melee' : 'ranged'] ?? 0) + (melee ? 0 : wingGuardRangedDefense(target,round) + holoParkShadowDefense(target,weapon,round) + holoParkFountainDefense(target,weapon));
    return Math.max(0, raw - Math.max(0, Number(reduction) || 0));
}
function extendPersonalWeaponRange(weapon, bands = 0) {
    const order=['Interação','Curto','Médio','Longo','Extremo'];
    const current=order.indexOf(String(weapon?.range??''));
    const plus=Math.max(0,Number(bands)||0);
    if(current<0||plus<1) return {...weapon};
    return {...weapon,range:order[Math.min(order.length-1,current+plus)]};
}
function personalSilhouette(entity) {
    const raw=Number(entity?.silhouette ?? entity?.description?.silhouette ?? 1);
    return Number.isFinite(raw) ? Math.max(0, raw) : 1;
}
function proneTargetModifiers(target, weapon) {
    const prone=statusEffects_mjs_1.statusModifiers(target).prone;
    if(!prone) return {boost:0,setback:0};
    const melee=weapon?.skill==='pelea'||weapon?.skill==='armas_cuerpo_a_cuerpo';
    return melee?{boost:1,setback:0}:{boost:0,setback:1};
}
function itemDamageState(value) {
    const state=String(value??'undamaged').toLocaleLowerCase('pt-BR');
    return ['undamaged','minor','moderate','major','destroyed'].includes(state)?state:'undamaged';
}
function nextItemDamageState(value) {
    const order=['undamaged','minor','moderate','major','destroyed'];
    const i=Math.max(0,order.indexOf(itemDamageState(value)));
    return order[Math.min(order.length-1,i+1)];
}
function itemDamageUsePenalty(weapon) {
    const state=itemDamageState(weapon?.damageState);
    return {state,setback:state==='minor'?1:0,difficultyIncrease:state==='moderate'?1:0,unusable:state==='major'||state==='destroyed'};
}
function chooseNpcWeapon(adversary) {
    for (const weapon of adversary.weapons ?? []) {
        if(itemDamageUsePenalty(weapon).unusable) continue;
        try {
            attackDifficulty_mjs_1.attackDifficultyForWeapon(weapon, adversary.range);
            return weapon;
        }
        catch { }
    }
    return null;
}
function clone(value) { return structuredClone(value); }
function applyEconomicProposal(state, proposal) {
    let next = structuredClone(state);
    if (proposal.acquisitionRequest) {
        if (next.master?.pendingAcquisition)
            throw new Error('Já existe uma aquisição aguardando resolução.');
        const market = sandbox_mjs_1.marketProfile(next.world, next.scene?.locationId ?? null);
        const pending = economy_mjs_1.createPendingAcquisition(proposal.acquisitionRequest.kind, proposal.acquisitionRequest.itemId, requireCharacter(next.player?.character), {
            requestId: ids_mjs_1.newId('acq'), locationId: next.scene?.locationId ?? null, market
        });
        next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('acqreq'), type: 'EQUIPMENT_ACQUISITION_PENDING', at: new Date().toISOString(), payload: { pending } });
    }
    if (proposal.acquisitionDecision) {
        const resolved = economy_mjs_1.resolvePendingAcquisition(next, next.master?.pendingAcquisition, { ...proposal.acquisitionDecision, sourceRuleIds: proposal.sourceRuleIds });
        next = resolved.state;
        next.master = { ...(next.master ?? {}), pendingAcquisition: null, acquisitionHistory: [...(next.master?.acquisitionHistory ?? []), structuredClone(resolved.record)] };
        next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('acq'), type: 'ECONOMY_TRANSACTION', at: new Date().toISOString(), payload: { record: resolved.record } });
    }
    for (const op of proposal.economyOperations ?? []) {
        let result;
        if (op.type === 'grant_asset') result = economy_mjs_1.grantCatalogAsset(next, { ...op, locationId: next.scene?.locationId ?? null, sourceRuleIds: proposal.sourceRuleIds });
        else if (op.type === 'lose_asset') result = economy_mjs_1.loseOwnedAsset(next, op.assetId, { ...op, sourceRuleIds: proposal.sourceRuleIds });
        else if (op.type === 'damage_asset') result = economy_mjs_1.damageOwnedAsset(next, op.assetId, { ...op, sourceRuleIds: proposal.sourceRuleIds });
        else if (op.type === 'repair_asset') result = economy_mjs_1.repairOwnedAsset(next, op.assetId, { ...op, sourceRuleIds: proposal.sourceRuleIds });
        else if (op.type === 'sell_asset') result = economy_mjs_1.sellOwnedAsset(next, op.assetId, { ...op, sourceRuleIds: proposal.sourceRuleIds });
        else throw new Error(`Operação econômica não suportada: ${op.type}`);
        next = result.state;
        next.master = { ...(next.master ?? {}), acquisitionHistory: [...(next.master?.acquisitionHistory ?? []), structuredClone(result.record)] };
        next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('econ'), type: 'ECONOMY_TRANSACTION', at: new Date().toISOString(), payload: { record: result.record } });
    }
    return next;
}
function validateEncounterDirectiveAgainstState(state, directive) {
    if (!directive)
        return null;
    if (state.combat?.active)
        throw new Error('Não é possível preparar outro encontro durante um combate ativo.');
    if (directive.sourceHookId) {
        const hook = state.world?.hooks?.find(h => h.id === directive.sourceHookId);
        if (!hook || hook.status !== 'pending')
            throw new Error('A consequência causal indicada não está mais pendente.');
        if (hook.locationId && state.scene?.locationId && hook.locationId !== state.scene.locationId)
            throw new Error('A consequência causal indicada pertence a outro local da cena.');
    }
    const seen = new Set();
    for (const spec of directive.adversaries ?? []) {
        if (spec.factionId && !state.world?.factions?.[spec.factionId])
            throw new Error(`Facção do adversário não encontrada: ${spec.factionId}`);
        if (!spec.worldNpcId)
            continue;
        if (seen.has(spec.worldNpcId))
            throw new Error('O mesmo PNPC persistente não pode ocupar dois grupos no encontro.');
        seen.add(spec.worldNpcId);
        const npc = state.world?.npcs?.[spec.worldNpcId];
        if (!npc)
            throw new Error(`PNPC persistente não encontrado: ${spec.worldNpcId}`);
        if (npc.combatState?.profileKey && npc.combatState.profileKey !== spec.profileKey)
            throw new Error('O perfil de combate do PNPC persistente não pode mudar silenciosamente.');
        if (npc.combatState?.status === 'incapacitated')
            throw new Error(`${npc.name} está incapacitado e não pode iniciar outro encontro.`);
        if (spec.factionId && npc.factionId && spec.factionId !== npc.factionId)
            throw new Error('A facção do PNPC persistente não pode mudar silenciosamente no encontro.');
    }
    return directive;
}
export class GameController {
    constructor(repository) {
        this.repository = repository;
        const loaded = repository.load();
        this.state = loaded ?? state_mjs_1.createCampaign('Rebelião - campanha local');
        // Reopening a persisted campaign is a read, not a write. Re-saving
        // on every WebView bridge call previously caused redundant storage
        // notifications and could mask a failed idempotent retry.
        if (loaded === null || loaded === undefined) repository.save(this.state);
        this._atomicActionDepth = 0;
    }
    /** Stage all game events and persist exactly once after a validated multi-step action.
     * A failed encounter, skill adjudication or adventure transition cannot
     * leave an orphan combat, spent Destiny or half-awarded reward in storage.
     */
    atomicAction(operation) {
        if (typeof operation !== 'function') throw new Error('Ação transacional inválida.');
        if (this._atomicActionDepth > 0) return operation();
        const before = clone(this.state);
        this._atomicActionDepth = 1;
        try {
            const result = operation();
            this.repository.save(this.state);
            return result;
        } catch (error) {
            this.state = before;
            throw error;
        } finally {
            this._atomicActionDepth = 0;
        }
    }
    commit(type, payload) {
        this.state = events_mjs_1.applyEvent(this.state, {
            id: ids_mjs_1.newId('evt'), type, at: new Date().toISOString(), payload
        });
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return this.state;
    }
    newCampaign(name) {
        this.state = state_mjs_1.createCampaign(name?.trim() || 'Nova campanha');
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return this.state;
    }
    restartSandboxPreservingParty() {
        const previous = this.state;
        const next = state_mjs_1.createCampaign(previous?.name?.trim() || 'Nova campanha');
        next.campaignMode = previous?.campaignMode ?? next.campaignMode;
        next.player = structuredClone(previous?.player ?? next.player);
        next.group = structuredClone(previous?.group ?? next.group);
        next.vehicles = structuredClone(previous?.vehicles ?? next.vehicles);
        next.settings = structuredClone(previous?.settings ?? next.settings);
        this.state = next;
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return {
            state: this.state,
            preserved: {
                character: Boolean(this.state.player?.character),
                group: true,
                vehicles: true,
                settings: true,
                campaignMode: this.state.campaignMode
            }
        };
    }
    setCampaignMode(mode = 'EOTE') {
        const value = String(mode ?? '').trim().toUpperCase();
        if (!['EOTE','AOR','MIXED'].includes(value)) throw new Error('Modo de campanha deve ser EOTE, AOR ou MIXED.');
        this.commit('CAMPAIGN_MODE_UPDATED', { campaignMode: value });
        return { campaignMode: this.state.campaignMode, obligationActive: modeAllowsObligation(this.state), dutyActive: modeAllowsDuty(this.state) };
    }
    campaignModeSummary() {
        return { campaignMode: campaignMode(this.state), rulesProfile: this.state.rulesProfile, obligationActive: modeAllowsObligation(this.state), dutyActive: modeAllowsDuty(this.state) };
    }
    startSession({ playerCount = 1, destinyPlayerCount = null, rng = Math.random, obligationRoll = null, dutyRoll = null } = {}) {
        return this.atomicAction(()=>{
        let character = requireCharacter(this.state.player.character);
        if (this.state.combat?.active)
            throw new Error('Encerre o combate antes de iniciar uma nova sessão.');
        const mode = campaignMode(this.state);
        const obligation = modeAllowsObligation(this.state) ? obligation_mjs_1.obligationCheck(this.state.group, character, { rng, roll: obligationRoll }) : { roll:null,total:0,triggered:false,doubles:false,selected:null,groupPenalty:0,characterPenalty:0,progressionBlocked:false,entries:[] };
        const duty = modeAllowsDuty(this.state) ? duty_mjs_1.dutyCheck(this.state.group, character, { rng, roll: dutyRoll }) : { roll:null,total:0,triggered:false,doubles:false,selected:null,groupWoundThresholdBonus:0,characterWoundThresholdBonus:0,entries:[] };
        const destinyPool = destiny_mjs_1.prepareDestinyPool(destinyPlayerCount ?? playerCount, { rng });
        const group = destiny_mjs_1.setPreparedDestiny(this.state.group, destinyPool);
        const obligationApplied = obligation_mjs_1.applyObligationPenalty(character, obligation);
        character = obligationApplied.character;
        const dutyApplied = duty_mjs_1.applyDutyBonus(character, duty);
        character = resetTalentSessionUses(dutyApplied.character);
        const soundInvestmentRank=talents_mjs_1.sourceBackedTalentRank(character,'SOUNDINVEST');
        const sessionIncome=soundInvestmentRank*100;
        if(sessionIncome){
            const currentCredits=Number(character.credits??0);
            if(!Number.isSafeInteger(currentCredits)||currentCredits<0||!Number.isSafeInteger(currentCredits+sessionIncome))throw new Error('Saldo de investimentos fora do limite.');
            character={...character,credits:currentCredits+sessionIncome};
        }
        this.commit('GROUP_UPDATED', { group });
        this.commit('CHARACTER_UPDATED', { character });
        const at = new Date().toISOString();
        let session = sessionFlow_mjs_1.startSessionRuntime(this.state.session, { at });
        const obligationSession = { ...structuredClone(obligation), priorThreshold: obligationApplied.priorThreshold, appliedThreshold: obligationApplied.appliedThreshold };
        const dutySession = { ...structuredClone(duty), baseThreshold: dutyApplied.baseThreshold, effectiveThreshold: dutyApplied.effectiveThreshold };
        session = { ...session, campaignRules: { mode, destiny: structuredClone(destinyPool), obligation: obligationSession, duty: dutySession, soundInvestments: {rank:soundInvestmentRank,creditsGranted:sessionIncome,sessionNumber:session.number} }, eoteRules: { destiny: structuredClone(destinyPool), obligation: obligationSession } };
        const sessionId = this.state.session?.status === 'encerrada' ? ids_mjs_1.newId('sess') : this.state.sessionId;
        this.commit('SESSION_STARTED', { session, sessionId, scene: { title: session.number === 1 ? 'Abertura' : `Sessão ${session.number}`, narration: this.state.settings?.masterProvider === 'http' ? 'O Mestre está pronto para conduzir a sessão.' : 'A campanha começa agora. O mundo está em movimento, e você decide qual fio puxar primeiro.' } });
        return { session: this.state.session, character, destiny: destinyPool, obligation, duty, campaignMode: mode, soundInvestments: {rank:soundInvestmentRank,creditsGranted:sessionIncome} };
        });
    }
    _setSessionPhase(phase) {
        if (this.state.session?.status !== 'ativa')
            return this.state.session;
        const session = sessionFlow_mjs_1.setSessionPhase(this.state.session, phase);
        this.commit('SESSION_PHASE', { session });
        return session;
    }
    closeSession({ summary = null, xpAward = 0 } = {}) {
        if (this.state.combat?.active)
            throw new Error('Encerre o combate antes de encerrar a sessão.');
        const character = requireCharacter(this.state.player.character);
        if (this.state.session?.status !== 'ativa')
            throw new Error('Não há sessão ativa para encerrar.');
        let nextCharacter = obligation_mjs_1.restoreObligationPenalty(character, this.state.session?.campaignRules?.obligation ?? this.state.session?.eoteRules?.obligation);
        nextCharacter = duty_mjs_1.restoreDutyBonus(nextCharacter);
        if (Number(xpAward) > 0)
            nextCharacter = progression_mjs_1.grantExperience(nextCharacter, Number(xpAward), { reason: 'Experiência concedida no encerramento da sessão' }).character;
        const text = String(summary ?? '').trim() || sessionFlow_mjs_1.buildDeterministicSessionSummary({ ...this.state, player: { ...this.state.player, character: nextCharacter } });
        const session = sessionFlow_mjs_1.closeSessionRuntime(this.state.session, { summary: text, xpAwarded: Number(xpAward) || 0 });
        const historyEntry = { sessionId: this.state.sessionId, number: session.number, startedAt: session.startedAt, endedAt: session.endedAt, summary: session.summary, xpAwarded: session.xpAwarded, checksResolved: session.checksResolved, combatEncounters: session.combatEncounters };
        this.commit('SESSION_CLOSED', { session, historyEntry, character: nextCharacter });
        return { session, historyEntry, character: nextCharacter };
    }
    grantXp(amount, { reason = 'Experiência concedida pelo Mestre' } = {}) {
        const result = progression_mjs_1.grantExperience(requireCharacter(this.state.player.character), Number(amount), { reason });
        this.commit('XP_GRANTED', { character: result.character, amount: result.amount, reason: result.reason });
        return result;
    }
    skillProgressionCost(skillKey) {
        return progression_mjs_1.skillRankCostForCharacter(requireCharacter(this.state.player.character), skillKey);
    }
    trainSkill(skillKey) {
        assertProgressionAllowed(this.state);
        const result = progression_mjs_1.spendXpOnSkillForCharacter(requireCharacter(this.state.player.character), skillKey);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    ownedSpecializations() {
        return progression_mjs_1.ownedSpecializationRefs(requireCharacter(this.state.player.character));
    }
    specializationPurchaseCost(careerKey, specializationKey) {
        return progression_mjs_1.specializationPurchaseCost(requireCharacter(this.state.player.character), careerKey, specializationKey);
    }
    buySpecialization(careerKey, specializationKey) {
        assertProgressionAllowed(this.state);
        const result = progression_mjs_1.purchaseSpecialization(requireCharacter(this.state.player.character), careerKey, specializationKey);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    forceStatus() {
        const character = requireCharacter(this.state.player.character);
        return { force: force_mjs_1.normalizeForceState(character), availableRating: force_mjs_1.availableForceRating(character), sensitive: force_mjs_1.isForceSensitive(character), powers: Object.fromEntries(Object.keys(force_mjs_1.FORCE_POWER_TREES).map(key => [key, force_mjs_1.forcePowerCapabilities(character, key)])) };
    }
    forceCatalog() {
        return structuredClone(force_mjs_1.FORCE_POWER_TREES);
    }
    buyForcePower(powerKey) {
        assertProgressionAllowed(this.state);
        const result = force_mjs_1.purchaseForcePower(requireCharacter(this.state.player.character), String(powerKey));
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    buyForceUpgrade(powerKey, upgradeId) {
        assertProgressionAllowed(this.state);
        const result = force_mjs_1.purchaseForceUpgrade(requireCharacter(this.state.player.character), String(powerKey), String(upgradeId));
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    rollForcePowerCheck({ useOppositePoints = 0 } = {}) {
        const character = requireCharacter(this.state.player.character);
        const roll = force_mjs_1.rollForceDice(character);
        const resolved = force_mjs_1.resolveForceResources(character, this.state.group, roll, { useOppositePoints });
        this.commit('CHARACTER_UPDATED', { character: resolved.character });
        this.commit('GROUP_UPDATED', { group: resolved.group });
        return { roll, ...resolved };
    }
    commitForce(effectId, count = 1) {
        const result = force_mjs_1.commitForceDice(requireCharacter(this.state.player.character), { effectId, count });
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    releaseForce(effectId) {
        const result = force_mjs_1.releaseForceDice(requireCharacter(this.state.player.character), effectId);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    activateSenseOngoing({ mode = 'defense' } = {}) {
        let character = requireCharacter(this.state.player.character);
        const critical = assertCanPerformAction(character);
        let combat = clone(this.state.combat);
        if (combat?.active) {
            assertPlayerInitiativeTurn(combat);
            combat = combat_mjs_1.spendAction(combat, `Força: Sentir (${mode})`);
        }
        const result = force_mjs_1.activateSenseOngoingEffect(character, { mode, round: combat?.round ?? null });
        character = applyActionCriticalStrain(result.character, critical);
        this.commit('COMBAT_ACTION', { combat, character });
        return { ...result, character: clone(character), combat: clone(combat) };
    }
    endSenseOngoing({ mode = 'defense' } = {}) {
        const result = force_mjs_1.endSenseOngoingEffect(requireCharacter(this.state.player.character), { mode });
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    activateInfluenceBasic({ targetAdversaryIds = [], activations = 1, magnitudeActivations = 0, rangeActivations = 0, useOppositePoints = 0, rng = Math.random } = {}) {
        let character = requireCharacter(this.state.player.character);
        const critical = assertCanPerformAction(character);
        let combat = clone(this.state.combat);
        if (!combat?.active) throw new Error('Influência básica com efeito mecânico exige um encontro ativo com alvo persistente.');
        assertPlayerInitiativeTurn(combat);
        const spec = force_mjs_1.resolveInfluenceBasic(character, { activations, magnitudeActivations, rangeActivations });
        const ids = [...new Set((Array.isArray(targetAdversaryIds) ? targetAdversaryIds : [targetAdversaryIds]).map(String).filter(Boolean))];
        if (!ids.length) throw new Error('Selecione ao menos um alvo vivo para Influência.');
        if (ids.length > spec.maxTargets) throw new Error(`Influência permite no máximo ${spec.maxTargets} alvo(s) com este gasto de Magnitude.`);
        const targets = ids.map(id => {
            const target = combat.adversaries?.find(a => String(a.id) === id);
            if (!target) throw new Error('Alvo de Influência não encontrado no encontro.');
            if (adversaries_mjs_1.adversaryHealthStatus(target).incapacitated) throw new Error(`${target.name} já está incapacitado.`);
            const rank=personalRangeIndex(target.range);
            if(rank===null) throw new Error(`Alcance de ${target.name} não é reconhecido pelo runtime.`);
            if(rank > spec.rangeBandIncrease) throw new Error(`${target.name} está fora do alcance atual de Influência.`);
            return clone(target);
        });
        combat = combat_mjs_1.spendAction(combat, 'Força: Influenciar');
        const roll = force_mjs_1.rollForceDice(character, { rng });
        const resources = force_mjs_1.resolveForceResources(character, this.state.group, roll, { useOppositePoints });
        character = resources.character;
        const group = resources.group;
        const success = resources.points >= spec.forcePointCost;
        const affected = [];
        if (success) {
            for (const target of targets) {
                const result = adversaries_mjs_1.sufferAdversaryStrain(target, spec.totalStrain);
                combat = encounter_mjs_1.updateAdversaryInCombat(combat, result.adversary);
                affected.push({ id: result.adversary.id, name: result.adversary.name, strain: spec.totalStrain, convertedToWounds: result.convertedToWounds, status: result.status });
            }
        }
        character = applyActionCriticalStrain(character, critical);
        const check = { type:'force', power:'influence', roll:clone(roll), points:resources.points, forcePointCost:spec.forcePointCost, success, useOppositePoints:Number(useOppositePoints)||0, affected:clone(affected) };
        combat.log = [...(combat.log ?? []), { type:'FORCE_POWER', round:combat.round, power:'influence', success, targets:ids, forcePointCost:spec.forcePointCost, points:resources.points }];
        this.commit('COMBAT_ACTION', { combat, character, group, check });
        return { success, roll, points:resources.points, forcePointCost:spec.forcePointCost, effect:spec, affected, character:clone(character), group:clone(group), combat:clone(combat) };
    }
    cyberneticsSummary() {
        const character=requireCharacter(this.state.player.character);
        const cap=cybernetics_mjs_1.cyberneticsCap(character);
        const installed=cybernetics_mjs_1.installedCybernetics(character);
        const totalCost=cybernetics_mjs_1.totalCyberneticsCost(character);
        const owned=(this.state.group?.ownedAssets??[]).filter(a=>a?.kind==='cybernetic').map(a=>({
            assetId:a.assetId,catalogId:a.catalogId,name:a.name,state:clone(a.state??{}),ownership:clone(a.ownership??{}),
            installed:Boolean(installed.some(x=>x.ownedAssetId===a.assetId))
        }));
        return {cap,totalCost,remainingSlots:Math.max(0,cap-totalCost),installed:clone(installed),owned,catalog:clone(cybernetics_mjs_1.CYBERNETICS_CATALOG)};
    }
    installCybernetic(itemId) {
        const nextCharacter=cybernetics_mjs_1.installCybernetic(requireCharacter(this.state.player.character),String(itemId??''));
        this.commit('CHARACTER_UPDATED',{character:nextCharacter});
        return {character:clone(nextCharacter),summary:this.cyberneticsSummary()};
    }
    removeCybernetic(itemId) {
        const nextCharacter=cybernetics_mjs_1.removeCybernetic(requireCharacter(this.state.player.character),String(itemId??''));
        this.commit('CHARACTER_UPDATED',{character:nextCharacter});
        return {character:clone(nextCharacter),summary:this.cyberneticsSummary()};
    }
    proposeCyberneticAcquisition(itemId) { return this.proposeAcquisition('cybernetic', String(itemId??'')); }
    installOwnedCybernetic(assetId) {
        const id=String(assetId??'').trim();
        const asset=(this.state.group?.ownedAssets??[]).find(a=>a?.assetId===id&&a?.kind==='cybernetic');
        if(!asset) throw new Error('Implante cibernético adquirido não encontrado.');
        if(cybernetics_mjs_1.installedCybernetics(this.state.player.character).some(x=>x.ownedAssetId===id)) throw new Error('Este implante já está instalado.');
        const nextCharacter=cybernetics_mjs_1.installCybernetic(requireCharacter(this.state.player.character),String(asset.catalogId??''),{ownedAssetId:id});
        const group=clone(this.state.group);
        const target=group.ownedAssets.find(a=>a.assetId===id);
        target.state={...(target.state??{}),installed:true,installedAt:new Date().toISOString()};
        this.commit('CHARACTER_UPDATED',{character:nextCharacter});
        this.commit('GROUP_UPDATED',{group});
        return {character:clone(nextCharacter),summary:this.cyberneticsSummary()};
    }
    removeOwnedCybernetic(assetId) {
        const id=String(assetId??'').trim();
        const installed=cybernetics_mjs_1.installedCybernetics(this.state.player.character).find(x=>x.ownedAssetId===id);
        if(!installed) throw new Error('Implante adquirido não está instalado.');
        const nextCharacter=cybernetics_mjs_1.removeCybernetic(requireCharacter(this.state.player.character),installed.id);
        const group=clone(this.state.group);
        const target=group.ownedAssets?.find(a=>a.assetId===id);
        if(target) target.state={...(target.state??{}),installed:false,removedAt:new Date().toISOString()};
        this.commit('CHARACTER_UPDATED',{character:nextCharacter});
        this.commit('GROUP_UPDATED',{group});
        return {character:clone(nextCharacter),summary:this.cyberneticsSummary()};
    }
    submitIntent(text) {
        if (!text?.trim())
            throw new Error('Digite uma ação.');
        const intent = text.trim();
        this.state = events_mjs_1.applyEvent(this.state, {
            id: ids_mjs_1.newId('act'), type: 'PLAYER_INTENT', at: new Date().toISOString(), payload: { text: intent }
        });
        const prepared = localMaster_mjs_1.prepareLocalMasterTurn(this.state, intent);
        this.state = events_mjs_1.applyEvent(this.state, {
            id: ids_mjs_1.newId('ctx'), type: 'MASTER_CONTEXT_PREPARED', at: new Date().toISOString(), payload: { ...prepared.digest, provider: prepared.provider }
        });
        if (!this._atomicActionDepth) this.repository.save(this.state);
        this.lastMasterContext = prepared.context;
        if (this.state.session?.status === 'ativa')
            this._setSessionPhase('resolucao');
        return { state: this.state, master: prepared };
    }
    consultRules(query, { limit = 5 } = {}) {
        const result = localMaster_mjs_1.consultRulesLocal(this.state, query, { limit });
        this.state = events_mjs_1.applyEvent(this.state, {
            id: ids_mjs_1.newId('rag'), type: 'MASTER_RULE_QUERY', at: new Date().toISOString(), payload: { query: result.query, authority: result.retrieval?.authority ?? null, results: result.results.map(r => ({ id: r.id, title: r.title, status: r.status, summary: r.summary, source: r.source, page: r.page, printedPage: r.printedPage, official: r.official, sourceKind: r.sourceKind, documentId: r.documentId, authorityType: r.authorityType, authorityWeight: r.authorityWeight })) }
        });
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return result;
    }
    prepareMasterContext(intentText) {
        const prepared = localMaster_mjs_1.prepareLocalMasterTurn(this.state, intentText);
        this.lastMasterContext = prepared.context;
        return prepared;
    }
    applyMasterProposal(rawProposal) {
        const before = structuredClone(this.state);
        try {
            const knownRuleIds = this.lastMasterContext?.rules?.map(r => r.id) ?? this.state.master.lastContext?.ruleIds ?? [];
            const proposal = proposal_mjs_1.validateMasterProposal(rawProposal, { knownRuleIds, pendingAcquisition: this.state.master?.pendingAcquisition });
            let next = proposal_mjs_1.applyValidatedNarrativeProposal(this.state, proposal);
            if (proposal.worldOperations?.length) {
                const worldResult = applyMasterWorldOperations(next.world, proposal.worldOperations);
                next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('worldmaster'), type: 'WORLD_UPDATED', at: new Date().toISOString(), payload: { world: worldResult.world } });
            }
            if (proposal.sceneLocationId) {
                if (!next.world?.locations?.[proposal.sceneLocationId])
                    throw new Error(`Local da cena não encontrado: ${proposal.sceneLocationId}`);
                next.scene.locationId = proposal.sceneLocationId;
            }
            next = sceneLibrary_mjs_1.applyAutomaticSceneVisual(next).state;
            validateEncounterDirectiveAgainstState(next, proposal.encounterDirective);
            next = applyEconomicProposal(next, proposal);
            this.state = next;
            if (!this._atomicActionDepth) this.repository.save(this.state);
            if (this.state.session?.status === 'ativa')
                this._setSessionPhase(sessionFlow_mjs_1.phaseAfterMasterProposal(proposal));
            return proposal;
        }
        catch (error) {
            this.state = before;
            throw error;
        }
    }
    getOfflineMasterStatus(intentText = '') {
        const prepared = localMaster_mjs_1.prepareLocalMasterTurn(this.state, intentText || 'estado atual');
        return provider_mjs_1.offlineMasterStatus(prepared.context);
    }
    configureTts({ enabled = false, rate = 1 } = {}) {
        const n = Number(rate);
        if (!Number.isFinite(n) || n < 0.6 || n > 1.4)
            throw new Error('Velocidade de TTS inválida.');
        const next = structuredClone(this.state);
        next.settings = next.settings ?? {};
        next.settings.ttsEnabled = Boolean(enabled);
        next.settings.ttsRate = n;
        this.state = next;
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return structuredClone(this.state.settings);
    }
    configureMasterProvider({ mode = 'none', endpoint = null } = {}) {
        if (!['none', 'http'].includes(mode))
            throw new Error('Modo do Mestre inválido.');
        const next = structuredClone(this.state);
        next.settings = next.settings ?? {};
        next.settings.masterProvider = mode;
        next.settings.masterEndpoint = mode === 'http' ? String(endpoint ?? '').trim() || null : null;
        if (mode === 'http' && !next.settings.masterEndpoint)
            throw new Error('Informe o endpoint HTTPS do Mestre.');
        this.state = next;
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return structuredClone(this.state.settings);
    }
    async requestMasterTurn({ intentText, endpoint, fetchImpl = globalThis.fetch, timeoutMs = 15000, signal = null } = {}) {
        if (!String(intentText ?? '').trim())
            throw new Error('Digite uma ação.');
        const before = structuredClone(this.state);
        const prepared = localMaster_mjs_1.prepareLocalMasterTurn(before, intentText.trim());
        try {
            const remote = await provider_mjs_1.requestHttpMasterProposal({ endpoint, context: prepared.context, fetchImpl, timeoutMs, signal });
            const knownRuleIds = prepared.context.rules.map(r => r.id);
            const proposal = proposal_mjs_1.validateMasterProposal(remote.proposal, { knownRuleIds, pendingAcquisition: before.master?.pendingAcquisition });
            let next = events_mjs_1.applyEvent(before, { id: ids_mjs_1.newId('act'), type: 'PLAYER_INTENT', at: new Date().toISOString(), payload: { text: intentText.trim() } });
            next = proposal_mjs_1.applyValidatedNarrativeProposal(next, proposal);
            if (proposal.worldOperations?.length) {
                const worldResult = applyMasterWorldOperations(next.world, proposal.worldOperations);
                next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('worldmaster'), type: 'WORLD_UPDATED', at: new Date().toISOString(), payload: { world: worldResult.world } });
            }
            if (proposal.sceneLocationId) {
                if (!next.world?.locations?.[proposal.sceneLocationId])
                    throw new Error(`Local da cena não encontrado: ${proposal.sceneLocationId}`);
                next.scene.locationId = proposal.sceneLocationId;
            }
            next = sceneLibrary_mjs_1.applyAutomaticSceneVisual(next).state;
            validateEncounterDirectiveAgainstState(next, proposal.encounterDirective);
            next = applyEconomicProposal(next, proposal);
            next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('master'), type: 'MASTER_RESPONSE_ACCEPTED', at: new Date().toISOString(), payload: { provider: 'http', sourceRuleIds: proposal.sourceRuleIds } });
            if (next.session?.status === 'ativa')
                next = events_mjs_1.applyEvent(next, { id: ids_mjs_1.newId('phase'), type: 'SESSION_PHASE', at: new Date().toISOString(), payload: { session: sessionFlow_mjs_1.setSessionPhase(next.session, sessionFlow_mjs_1.phaseAfterMasterProposal(proposal)) } });
            this.state = next;
            if (!this._atomicActionDepth) this.repository.save(this.state);
            this.lastMasterContext = prepared.context;
            return { proposal, context: prepared.context, meta: remote.meta };
        }
        catch (error) {
            this.state = before;
            throw error;
        }
    }
    setCharacter(character) {
        character_mjs_1.validateCharacter(character);
        this.state = clone(this.state);
        this.state.player.character = clone(character);
        const roster=this.state.player.roster??{schema:1,activeCharacterId:null,entries:[]};
        let active=roster.entries?.find(x=>x?.id===roster.activeCharacterId);
        if(active)active.character=clone(character);
        else{
            const id=ids_mjs_1.newId('pc');
            roster.entries=[...(roster.entries??[]),{id,character:clone(character)}];
            roster.activeCharacterId=id;
        }
        roster.schema=1;this.state.player.roster=roster;
        this.state.updatedAt = new Date().toISOString();
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return this.state.player.character;
    }
    registerPartyCharacter({character,id=null}={}){
        return this.atomicAction(()=>{
            character_mjs_1.validateCharacter(character);
            const roster=clone(this.state.player.roster??{schema:1,activeCharacterId:null,entries:[]});
            const pcId=String(id??ids_mjs_1.newId('pc')).trim();
            if(!/^[A-Za-z0-9:_-]{1,120}$/.test(pcId)||roster.entries.some(x=>x.id===pcId)||roster.entries.length>=8)
                throw new Error('Identificador de PC duplicado/inválido ou limite de oito PCs atingido.');
            roster.entries.push({id:pcId,character:clone(character),registeredAt:new Date().toISOString()});
            roster.schema=1;
            this.commit('PLAYER_ROSTER_UPDATED',{roster});
            return {id:pcId,character:clone(character),roster:clone(this.state.player.roster)};
        });
    }
    selectPartyCharacter(id){
        return this.atomicAction(()=>{
            const roster=clone(this.state.player.roster??{schema:1,activeCharacterId:null,entries:[]});
            const entry=roster.entries.find(x=>x.id===String(id));
            if(!entry)throw new Error('PC não encontrado no roster; companheiros NPC não são PCs.');
            roster.activeCharacterId=entry.id;
            this.commit('PLAYER_ROSTER_UPDATED',{roster});
            return {activeCharacterId:entry.id,character:clone(this.state.player.character)};
        });
    }
    importCharacter(text) {
        const character = character_mjs_1.importCharacterJson(text);
        return this.setCharacter(character);
    }
    exportCharacter() {
        if (!this.state.player.character)
            throw new Error('Nenhuma ficha carregada.');
        return character_mjs_1.exportCharacterJson(this.state.player.character);
    }
    loadStarterCharacter(name = 'zal') {
        const factory = starterPresets_mjs_1.STARTER_PRESETS[name];
        if (!factory)
            throw new Error('Personagem inicial não disponível.');
        return this.setCharacter(factory());
    }
    createCustomCharacter(options = {}) {
        if (campaignMode(this.state) === 'AOR') throw new Error('Campanha AoR exige criação AoR; altere para MIXED para usar personagem EotE.');
        return this.setCharacter(eoteCharacterCreation_mjs_1.createEoteCoreCharacter(options));
    }
    aorCreationCatalog() {
        return { profile:'AOR_CORE', species:structuredClone(characterCreation_mjs_1.AOR_SPECIES), careers:structuredClone(characterCreation_mjs_1.AOR_CAREERS), dutyTypes:[...characterCreation_mjs_1.DUTY_TYPES_PT_BR] };
    }
    aorCreationPreview(options = {}) { return characterCreation_mjs_1.creationPreview(options); }
    createAorCharacter(options = {}) {
        if (!modeAllowsDuty(this.state)) throw new Error('Ative campanha AOR ou MIXED antes de criar personagem AoR.');
        const character = characterCreation_mjs_1.createAorCoreCharacter(options);
        this.setCharacter(character);
        const first = character.duties?.[0];
        if (first && !(this.state.group.dutyEntries ?? []).some(e => String(e.member ?? '').toLocaleLowerCase('pt-BR') === String(character.name).toLocaleLowerCase('pt-BR') && String(e.type ?? '') === String(first.type ?? ''))) {
            const group = fringeGroup_mjs_1.addDutyEntry(this.state.group, { value:first.value, member:character.name, type:first.type, description:first.description ?? '' });
            this.commit('GROUP_UPDATED', { group });
        }
        return this.state.player.character;
    }
    buildRandomCharacter(options = {}) {
        return eoteCharacterCreation_mjs_1.createRandomEoteCoreCharacter(options);
    }
    createRandomCharacter(options = {}) {
        return this.setCharacter(this.buildRandomCharacter(options));
    }
    /** P159: UI reads the exact purchased AoR/EotE reroll applicable to a
     * skill. The same session ledger is authoritative at execution time. */
    naturalRerollStatus({skillKey}={}) {
        const character=requireCharacter(this.state.player.character);
        if(typeof skillKey!=='string' || !Object.hasOwn(character.skills??{},skillKey))
            throw new Error('Perícia de rerrolagem Nato inválida.');
        const spec=talents_mjs_1.naturalRerollOption(character,skillKey);
        return {...spec,usedThisSession:spec.available&&talentUsedThisSession(character,spec.sessionKey),sessionActive:this.state.session?.status==='ativa'};
    }
    /** P167: Familiar Suns, one maneuver/check per session, irrespective of
     * whether both AoR tree slots are purchased. Never fabricate planetary
     * inhabitants or hazards: the GM reveals source-world information only
     * after the real Hard Knowledge check succeeds. */
    familiarSunsStatus() {
        const character=requireCharacter(this.state.player.character);
        const rank=talents_mjs_1.sourceBackedTalentRank(character,'FAMILIARSUNS');
        return {available:rank>0,rank,usedThisSession:talentUsedThisSession(character,'familiarSuns'),
            sessionActive:this.state.session?.status==='ativa',difficulty:3,
            skills:['borde_exterior','mundos_del_nucleo'],
            revelationCategories:['habitação','ambiente','predadores','riscos','oportunidades']};
    }
    useFamiliarSuns({skillKey='borde_exterior',rng=Math.random}={}) {
        return this.atomicAction(()=>{
            const status=this.familiarSunsStatus();
            if(!status.available)throw new Error('Sóis familiares não foi adquirido na árvore AoR.');
            if(!status.sessionActive)throw new Error('Sóis familiares exige sessão ativa.');
            if(status.usedThisSession)throw new Error('Sóis familiares já foi utilizado nesta sessão.');
            if(!status.skills.includes(skillKey))throw new Error('Sóis familiares exige Conhecimento Borda Exterior ou Mundos do Núcleo.');
            if(this.state.combat?.active)throw new Error('Sóis familiares requer uma manobra fora do combate neste painel.');
            const check=this.resolveSkillCheck({skillKey,difficulty:3,rng});
            const character=markTalentUsedThisSession(this.state.player.character,'familiarSuns');
            this.commit('CHARACTER_UPDATED',{character});
            return {used:true,skillKey,difficulty:3,check,success:Boolean(check.success),
                gmRevelationRequired:Boolean(check.success),
                revelationCategories:check.success?status.revelationCategories:[]};
        });
    }
    aorP176Status(){
      const c=requireCharacter(this.state.player.character),rank=k=>talents_mjs_1.sourceBackedTalentRank(c,k)>0;
      return {sessionActive:this.state.session?.status==='ativa',vehicleCombatActive:Boolean(this.state.vehicles?.combat?.active),
        boughtInfo:rank('BOUGHT_INFO'),knownSchematic:rank('KNOWN_SCHEMATIC'),fireControl:rank('FIRE_CONTROL'),
        exhaustPort:rank('EXHAUST_PORT'),deadToRights:rank('DEAD_TO_RIGHTS'),deadToRightsImproved:rank('DEAD_TO_RIGHTS_IMPROVED'),
        skilledSlicer:rank('SKILSLI'),masterSlicer:rank('MASSLIC'),
        schematicUsed:(this.state.master?.aorIntelActions??[]).some(r=>r.kind==='schematic'&&r.sessionId===this.state.sessionId&&r.characterName===c.name),
        slicerSystemId:this.state.combat?.aorSkilledSlicer?.systemId??null};
    }
    /** P176 — Bought Info: 50 credits per difficulty, exactly one success;
     * never invent the actual answer. Source and access require GM approval. */
    useAoRBoughtInfo({actionId,subjectId,skillKey='educacion',difficulty=2,approved=false}={}){
      return this.atomicAction(()=>{
        if(!safeIntelId(actionId)||!safeIntelId(subjectId)||!KNOWLEDGE_SKILLS.includes(skillKey)||
           !Number.isSafeInteger(difficulty)||difficulty<1||difficulty>5||approved!==true)
          throw Error('Compra de informação exige identificadores, dificuldade e aprovação do Mestre.');
        const c=requireCharacter(this.state.player.character);
        assertCanPerformAction(c);
        if(talents_mjs_1.sourceBackedTalentRank(c,'BOUGHT_INFO')<1)throw Error('Comprar informação AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||this.state.session?.status!=='ativa')
          throw Error('Compra de informação exige sessão AoR ativa.');
        const old=(this.state.master.aorIntelActions??[]).find(r=>r.actionId===actionId);
        if(old){if(old.kind!=='bought'||old.subjectId!==subjectId||old.skillKey!==skillKey||old.difficulty!==difficulty)throw Error('Recibo de informação repetido com parâmetros divergentes.');return {duplicate:true,receipt:clone(old)};}
        if((this.state.master.aorIntelActions??[]).length>=128)throw Error('Histórico de informação cheio; exporte a campanha.');
        const cost=50*difficulty;
        if(!Number.isSafeInteger(c.credits)||c.credits<cost)throw Error('Créditos insuficientes para comprar informação.');
        let combat=clone(this.state.combat);
        if(combat.active){assertCanPerformAction(c);assertPlayerInitiativeTurn(combat);combat=combat_mjs_1.spendAction(combat,'Comprar informação');}
        const character=clone(c);character.credits-=cost;
        const receipt={actionId,kind:'bought',subjectId,skillKey,difficulty,approved:true,
          campaignId:this.state.campaignId,sessionId:this.state.sessionId,characterName:c.name,
          creditsSpent:cost,netSuccess:1,success:true,gmRevelationRequired:true};
        if(combat.active)this.commit('COMBAT_ACTION',{combat,character});
        else this.commit('CHARACTER_UPDATED',{character});
        this.commit('AOR_INTEL_ACTION',{receipt});
        return {receipt,creditsRemaining:character.credits,success:true,netSuccess:1,gmRevelationRequired:true};
      });
    }
    /** Known Schematic: Hard Education check as maneuver, once per session.
     * Only a real Master-confirmed building/ship is targetable. */
    useAoRKnownSchematic({actionId,subjectId,approved=false,maneuverMethod='normal',rng=Math.random}={}){
      return this.atomicAction(()=>{
        if(!safeIntelId(actionId)||!safeIntelId(subjectId)||approved!==true)
          throw Error('Projeto conhecido exige alvo e aprovação do Mestre.');
        let c=requireCharacter(this.state.player.character);
        assertCanPerformAction(c);
        if(talents_mjs_1.sourceBackedTalentRank(c,'KNOWN_SCHEMATIC')<1)throw Error('Projeto conhecido AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||this.state.session?.status!=='ativa')
          throw Error('Projeto conhecido exige sessão AoR ativa.');
        const list=this.state.master.aorIntelActions??[];
        const old=list.find(r=>r.actionId===actionId);
        if(old){if(old.kind!=='schematic'||old.subjectId!==subjectId)throw Error('Recibo de projeto repetido com parâmetros divergentes.');return {duplicate:true,receipt:clone(old)};}
        if(list.length>=128)throw Error('Histórico de informação cheio; exporte a campanha.');
        if(list.some(r=>r.kind==='schematic'&&r.sessionId===this.state.sessionId&&r.characterName===c.name))
          throw Error('Projeto conhecido já utilizado nesta sessão.');
        let combat=clone(this.state.combat);
        if(combat.active){const spent=spendPlayerManeuver(c,combat,{method:maneuverMethod,label:'Projeto conhecido'});c=spent.character;combat=spent.combat;}
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(c.characteristics?.intelecto??0),Number(c.skills?.educacion??0));
        const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:3,challenge:0,boost:0,setback:0},rng);
        const receipt={actionId,kind:'schematic',subjectId,skillKey:'educacion',difficulty:3,approved:true,
          campaignId:this.state.campaignId,sessionId:this.state.sessionId,characterName:c.name,
          creditsSpent:0,netSuccess:Math.max(0,Number(check.netSuccess??0)),success:Boolean(check.success),gmRevelationRequired:Boolean(check.success)};
        if(combat.active)this.commit('COMBAT_ACTION',{combat,character:c,check});
        else this.commit('CHARACTER_UPDATED',{character:c});
        this.commit('AOR_INTEL_ACTION',{receipt});
        this.commit('SESSION_CHECK_RESOLVED',{session:sessionFlow_mjs_1.noteSessionCheck(this.state.session)});
        return {receipt,check,success:check.success,gmRevelationRequired:check.success};
      });
    }
    /** P177: a single GM-approved AoR action, using live skills, equipment,
     * encounter targets and the event-sourced atomic save boundary. */
    useAoRP177Action({kind,actionId,subjectId,approved=false,fact='',toolsId=null,partsId=null,
        targetIds=[],containerId=null,itemId=null,rng=Math.random}={}){
      return this.atomicAction(()=>{
        const c0=requireCharacter(this.state.player.character);
        if(!['AOR','MIXED'].includes(campaignMode(this.state))||this.state.session?.status!=='ativa')
          throw Error('Talento AoR P177 exige sessão ativa.');
        if(approved!==true||!safeP177Id(actionId)||!safeP177Id(subjectId))throw Error('Identificador ou aprovação do Mestre inválidos.');
        const talent={contraption:'CONTRAPTION',careful_planning:'CAREFUL_PLANNING',incite_rebellion:'INCITE_REBELLION',hidden_storage:'HIDDEN_STORAGE'}[kind];
        if(!talent||talents_mjs_1.sourceBackedTalentRank(c0,talent)<1)throw Error('Talento AoR P177 não adquirido.');
        const prior=(this.state.master?.aorP177Actions??[]).find(x=>x.actionId===actionId);
        if(prior){
          const matching=prior.kind===kind&&prior.subjectId===subjectId&&prior.sessionId===this.state.sessionId&&prior.characterName===c0.name&&prior.approved===approved&&
            (kind!=='careful_planning'||prior.fact===String(fact).trim())&&
            (kind!=='contraption'||(prior.toolsId===toolsId&&prior.partsId===partsId))&&
            (kind!=='incite_rebellion'||JSON.stringify(prior.targetIds)===JSON.stringify(targetIds))&&
            (kind!=='hidden_storage'||(prior.containerId===containerId&&prior.itemId===itemId));
          if(matching)return {duplicate:true,receipt:clone(prior)};
          throw Error('ID de ação reutilizado com parâmetros divergentes.');
        }
        if((this.state.master?.aorP177Actions??[]).length>=256)throw Error('Histórico AoR P177 cheio.');
        if(['contraption','careful_planning','incite_rebellion'].includes(kind)&&
          (this.state.master?.aorP177Actions??[]).some(r=>r.kind===kind&&r.sessionId===this.state.sessionId&&r.characterName===c0.name))
          throw Error('Talento AoR P177 já utilizado nesta sessão.');
        let character=clone(c0),combat=clone(this.state.combat),check=null,extra={};
        if(kind==='careful_planning'){
          if(typeof fact!=='string'||fact.trim().length<1||fact.length>300||/[<>\x00-\x1f]/.test(fact))throw Error('Fato narrativo inválido.');
          extra={fact:fact.trim(),noDestinySpent:true,gmMustRespectCanon:true};
        }else if(kind==='contraption'){
          assertCanPerformAction(character);assertPlayerInitiativeTurn(combat);
          if(!combat.active)throw Error('Artilugio exige encontro ativo; dura somente até seu término.');
          if(!safeP177Id(toolsId)||!safeP177Id(partsId)||toolsId===partsId)throw Error('Ferramentas e peças distintas devem existir no inventário.');
          const owned=id=>(character.gear??[]).find(g=>String(g.id??g.sourceCatalogId)===id&&Number(g.quantity??1)>0);
          const tools=owned(toolsId),parts=owned(partsId);
          if(!tools||!parts)throw Error('Ferramentas/peças não possuídas.');
          combat=combat_mjs_1.spendAction(combat,'Artilugio');
          const pool=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.intelecto??0),Number(character.skills?.mecanica??0));
          check=narrativeDice_mjs_1.rollPool({...pool,difficulty:3,challenge:0,boost:0,setback:0},rng);
          if(check.success){
            parts.quantity=Number(parts.quantity??1)-1;
            // On a Triumph the tool may be salvaged; the consumed part is not duplicated.
            if(check.triumph<1)tools.quantity=Number(tools.quantity??1)-1;
            character.gear.push({id:`contraption_${actionId}`,name:'Artilúgio temporário',quantity:1,encumbrance:0,
              source:'AoR Core: Contraption',temporaryEncounter:true,approvedSubjectId:subjectId});
          }
          extra={difficulty:3,toolsId,partsId,deviceItemId:check.success?`contraption_${actionId}`:null,
            partsConsumed:check.success?1:0,toolsConsumed:check.success&&check.triumph<1?1:0,
            gmDeviceRequired:check.success,expiresWithEncounter:true,encounterSerial:Number(combat.socialEncounterSerial??0)};
        }else if(kind==='incite_rebellion'){
          assertCanPerformAction(character);assertPlayerInitiativeTurn(combat);
          if(!combat.active)throw Error('Incitar à rebelião exige encontro ativo.');
          if(!Array.isArray(targetIds)||!targetIds.length||new Set(targetIds).size!==targetIds.length||
             targetIds.length>Number(character.skills?.coaccion??0)||targetIds.some(id=>!safeP177Id(id)))
            throw Error('Número/IDs de alvos excedem a graduação de Coerção.');
          for(const id of targetIds){const npc=combat.adversaries?.find(a=>a.id===id);
            if(!npc||!['minion','rival'].includes(String(npc.kind??'').toLowerCase())||
               adversaries_mjs_1.adversaryHealthStatus(npc).incapacitated)throw Error('Incitar exige lacaios ou rivais reais ativos.');}
          combat=combat_mjs_1.spendAction(combat,'Incitar à rebelião');
          const pool=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.vontade??0),Number(character.skills?.coaccion??0));
          check=narrativeDice_mjs_1.rollPool({...pool,difficulty:3,challenge:0,boost:0,setback:0},rng);
          if(check.success){for(const id of targetIds){const i=combat.adversaries.findIndex(a=>a.id===id);
            combat.adversaries[i]={...combat.adversaries[i],aorIncitedRebellion:{sessionId:this.state.sessionId,encounterSerial:Number(combat.socialEncounterSerial??0),sourceActionId:actionId}};}}
          extra={difficulty:3,targetIds:[...targetIds],untilEncounterEnd:true};
        }else if(kind==='hidden_storage'){
          if(!safeP177Id(containerId)||!safeP177Id(itemId)||containerId===itemId)throw Error('IDs de compartimento inválidos.');
          const item=(character.gear??[]).find(g=>String(g.id??g.sourceCatalogId)===itemId&&Number(g.quantity??1)>0);
          const container=(character.gear??[]).find(g=>String(g.id??g.sourceCatalogId)===containerId&&Number(g.quantity??1)>0);
          const vehicle=this.state.vehicles?.vehicles?.[containerId];
          if(!item||(!container&&!vehicle))throw Error('Item ou compartimento não possuído.');
          if(item.hiddenStorage)throw Error('Item já oculto; não é possível duplicar compartimentos.');
          const enc=Number(item.encumbrance??item.carga??1);
          if(!Number.isSafeInteger(enc)||enc<1||enc>10)throw Error('Carga do item inválida.');
          const used=(character.gear??[]).filter(g=>g.hiddenStorage).reduce((sum,g)=>sum+Number(g.hiddenStorage.encumbrance??0),0);
          if(used+enc>talents_mjs_1.sourceBackedTalentRank(character,'HIDDEN_STORAGE'))throw Error('Capacidade de armazenamento oculto excedida.');
          item.hiddenStorage={containerId,encumbrance:enc,detectionDifficulty:5};
          extra={containerId,itemId,encumbrance:enc,detectionDifficulty:5};
        }
        const receipt={kind,actionId,subjectId,campaignId:this.state.campaignId,sessionId:this.state.sessionId,
          characterName:c0.name,approved:true,costCredits:0,success:check?Boolean(check.success):true,...extra};
        if(combat.active&&['contraption','incite_rebellion'].includes(kind))this.commit('COMBAT_ACTION',{combat,character,check});
        else this.commit('CHARACTER_UPDATED',{character});
        if(kind==='careful_planning'){
          const facts=this.state.scene?.approvedFacts??[];
          if(!Array.isArray(facts)||facts.length>=40)throw Error('Limite de fatos aprovados nesta cena atingido.');
          this.commit('SCENE_UPDATED',{scene:{...this.state.scene,approvedFacts:[...facts,{actionId,fact:receipt.fact,source:'AoR:Careful Planning'}]}});
        }
        this.commit('AOR_P177_ACTION',{receipt});
        if(check&&this.state.session?.status==='ativa')this.commit('SESSION_CHECK_RESOLVED',{session:sessionFlow_mjs_1.noteSessionCheck(this.state.session)});
        return {receipt,check,duplicate:false};
      });
    }
    aorP177Status(){
      const c=this.state.player?.character;
      const rank=key=>c?talents_mjs_1.sourceBackedTalentRank(c,key):0;
      return {sessionActive:this.state.session?.status==='ativa',combatActive:Boolean(this.state.combat?.active),
        contraption:rank('CONTRAPTION'),carefulPlanning:rank('CAREFUL_PLANNING'),inciteRebellion:rank('INCITE_REBELLION'),
        hiddenStorage:rank('HIDDEN_STORAGE'),greasedPalms:rank('GREASED_PALMS'),stunningBlow:rank('STUNBL'),
        stunningBlowImproved:rank('STUNBLIMP'),softSpot:rank('SOFT_SPOT'),uses:clone((this.state.master?.aorP177Actions??[]).filter(r=>r.sessionId===this.state.sessionId))};
    }
    /** P178: source-owned AoR choices for Android; usage read from persisted session. */
    aorP178Status(){
      const c=this.state.player?.character;
      const rank=key=>c?talents_mjs_1.sourceBackedTalentRank(c,key):0;
      return {sessionActive:this.state.session?.status==='ativa',
        combatActive:Boolean(this.state.combat?.active),vehicleCombatActive:Boolean(this.state.vehicles?.combat?.active),
        worksLikeACharm:rank('WORKS_LIKE_A_CHARM')>0&&!talentUsedThisSession(c,'worksLikeACharm'),
        strokeOfGenius:rank('STROKE_OF_GENIUS')>0&&!talentUsedThisSession(c,'strokeOfGenius'),
        quickFix:rank('QUICK_FIX')>0&&!talentUsedThisSession(c,'quickFix'),
        unstoppable:rank('UNSTOPPABLE')>0,cripplingBlow:rank('CRIPPLING_BLOW')>0,
        formOnMe:rank('FORM_ON_ME')>0,leadership:Number(c?.skills?.liderazgo??0),
        vehicles:Object.values(this.state.vehicles?.vehicles??{}).filter(v=>v.alliedToPlayer===true&&v.closeToActive===true)
          .map(v=>({id:v.id,name:v.name})).slice(0,20)};
    }
    /** AoR Fire Control: a maneuver, no stacking, current vehicle round only. */
    useAoRFireControl({method='normal'}={}){
      return this.atomicAction(()=>{
        const c=requireCharacter(this.state.player.character),vehicles=clone(this.state.vehicles);
        assertCanAct(c);
        if(!['normal','strain','action'].includes(method))throw Error('Pagamento de manobra inválido.');
        if(talents_mjs_1.sourceBackedTalentRank(c,'FIRE_CONTROL')<1)throw Error('Fogo controlado AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||!vehicles.combat.active||this.state.session?.status!=='ativa')
          throw Error('Fogo controlado exige combate veicular AoR ativo.');
        const active=vehicles.vehicles?.[vehicles.activeVehicleId];
        if(!active||vehicles_mjs_1.vehicleStatus(active,{currentRound:vehicles.combat.round}).noManeuvers)
          throw Error('Nave ausente ou impossibilitada de realizar manobras.');
        if(vehicles.combat.aorFireControl?.round===vehicles.combat.round)
          throw Error('Fogo controlado não acumula na mesma rodada.');
        const used=Number(vehicles.combat.maneuversUsed??0);
        if(used>=2)throw Error('Limite de duas manobras veiculares atingido.');
        if(used===0&&method!=='normal')throw Error('A primeira manobra veicular deve ser gratuita.');
        if(used===1){
          if(method==='strain'){
            if(!vehicles_mjs_1.canVoluntarilyInflictVehicleStrain(active))throw Error('Não é possível sofrer Tensão de sistemas.');
            active.systemStrain+=2;
          }else if(method==='action'){
            if(vehicles.combat.actionUsed)throw Error('Ação veicular já usada.');
            vehicles.combat.actionUsed=true;
          }else throw Error('Segunda manobra exige ação ou 2 de Tensão de sistemas.');
        }
        vehicles.combat.maneuversUsed=used+1;
        vehicles.vehicles[active.id]=vehicles_mjs_1.setControlledFire(active,true);
        const receipt={vehicleId:active.id,round:vehicles.combat.round,sessionId:this.state.sessionId};
        vehicles.combat.aorFireControl=receipt;
        vehicles.combat.log.push({type:'AOR_FOGO_CONTROLADO',...receipt});
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {receipt,bonusTargetSilhouette:1,expiresAtNextRound:true};
      });
    }
    talentActionStatus() {
        const character=requireCharacter(this.state.player.character);
        return {
            familiarSuns:this.familiarSunsStatus(),
            mentalFortress:{available:talents_mjs_1.sourceBackedTalentRank(character,'MENTALFORT')>0,active:Boolean(character.combatEffects?.mentalFortress)},
            intenseFocus:talents_mjs_1.intenseFocusOption(character),
            senseDanger:{...talents_mjs_1.senseDangerOption(character),usedThisSession:talentUsedThisSession(character,'senseDanger')},
            touchOfFate:{...talents_mjs_1.touchOfFateOption(character),usedThisSession:talentUsedThisSession(character,'touchOfFate')},
            jumpUp:{available:talents_mjs_1.talentRankByOriginal(character,'JUMP')>0,usedThisTurn:Boolean(this.state.combat?.playerTurn?.jumpUpUsed)},
            balance:{available:talents_mjs_1.talentRankByOriginal(character,'BALANCE')>0},
            quickDraw:{...talents_mjs_1.quickDrawOption(character),usedThisTurn:Boolean(this.state.combat?.playerTurn?.quickDrawUsed)},
            secondWind:{...talents_mjs_1.secondWindOption(character),usedThisEncounter:Boolean(this.state.combat?.secondWindUsed)},
            frenziedAttack:{...talents_mjs_1.frenziedAttackOption(character,0),pending:clone(this.state.combat?.playerTurn?.pendingFrenziedAttack??null)},
            sideStep:{...talents_mjs_1.sideStepOption(character,0),active:clone(character?.combatEffects?.sideStep??null)},
            feint:{...talents_mjs_1.feintTalentOption(character,0),active:clone(character?.combatEffects?.feint??null)},
            defensiveStance:{...talents_mjs_1.defensiveStanceOption(character,0),active:clone(character?.combatEffects?.defensiveStanceTalent??null)},
            lethalBlows:talents_mjs_1.lethalBlowsCriticalModifier(character),
            dodge:{...talents_mjs_1.dodgeOption(character,0),prepared:clone(character?.combatEffects?.dodgeNextIncoming??null)},
            preciseAim:{...talents_mjs_1.preciseAimOption(character,0),pending:clone(this.state.combat?.pendingPreciseAim??null)},
            sniperShot:{...talents_mjs_1.sniperShotOption(character,0),pending:clone(this.state.combat?.pendingSniperShot??null)},
            targetedBlow:{...talents_mjs_1.targetedBlowOption(character),pending:this.state.combat?.pendingDestinyDamageTalent==='targetedBlow'},
            anatomyLessons:{...talents_mjs_1.anatomyLessonsOption(character),pending:this.state.combat?.pendingDestinyDamageTalent==='anatomyLessons'},
            deadlyAccuracy:talents_mjs_1.deadlyAccuracySpec(character),
            armorMaster:talents_mjs_1.armorMasterBonuses(character,{armorSoak:0,worn:false}),
            resolve:{rank:talents_mjs_1.talentRankByOriginal(character,'RESOLVE')},
            masterOfShadows:talents_mjs_1.masterSkillDifficultyOption(character,'shadows'),
            masterDoctor:talents_mjs_1.masterSkillDifficultyOption(character,'doctor'),
            masterSlicer:talents_mjs_1.masterSkillDifficultyOption(character,'slicer'),
            intimidating:{...talents_mjs_1.intimidatingOption(character,0)},
            sessionActive:this.state.session?.status==='ativa',
            combatActive:Boolean(this.state.combat?.active)
        };
    }
    talentTree(selector = {}) { return talents_mjs_1.treeForCharacter(this.state.player.character, selector); }
    eligibleTalents(selector = {}) { return talents_mjs_1.eligibleTalents(requireCharacter(this.state.player.character), selector); }
    availableTalents(selector = {}) { return talents_mjs_1.eligibleTalents(this.state.player.character, selector); }
    buyTalent(nodeId, options = {}) {
        assertProgressionAllowed(this.state);
        const result = talents_mjs_1.purchaseTalent(requireCharacter(this.state.player.character), nodeId, options);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    // Compatibilidade com R11.
    availableFirstRowTalents() { return talents_mjs_1.eligibleFirstRowTalents(this.state.player.character); }
    buyFirstRowTalent(nodeId, options = {}) {
        assertProgressionAllowed(this.state);
        const result = talents_mjs_1.purchaseFirstRowTalent(requireCharacter(this.state.player.character), nodeId, options);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    talentRanks() { return talents_mjs_1.talentRanks(this.state.player.character); }
    talentRank(originalName) { return talents_mjs_1.talentRankByOriginal(this.state.player.character, originalName); }
    equipmentCatalog() { return equipmentCatalog_mjs_1.equipmentCatalog(); }
    utilityBeltStatus(){
        const c=requireCharacter(this.state.player.character);
        const talent=talents_mjs_1.utilityBeltTalentStatus(c);
        const allowed=this.equipmentCatalog();
        const safeGear=allowed.generalGear.filter(x=>Number.isInteger(x.rarity)&&x.rarity<=4&&Number(x.encumbrance)<=1&&!x.restricted);
        const safeWeapons=allowed.weapons.filter(x=>Number.isInteger(x.rarity)&&x.rarity<=4&&Number(x.encumbrance)<=1&&!x.restricted&&
            (x.qualities||[]).some(q=>/^(?:Munição limitada|Limited Ammo)\s*1$/i.test(String(q))));
        return {talent,destiny:clone(this.state.group?.destiny??{}),items:[...safeGear.map(x=>({id:x.id,name:x.name,kind:'gear',rarity:x.rarity,encumbrance:x.encumbrance})),...safeWeapons.map(x=>({id:x.id,name:x.name,kind:'weapon',rarity:x.rarity,encumbrance:x.encumbrance}))]};
    }
    /** One receipt per player-selected activation. Every inventory change and
     * light-to-dark Destiny flip commits atomically; replay never grants twice. */
    useUtilityBelt({itemId,actionId}={}){
        const id=String(actionId??'').trim(),assetId=String(itemId??'').trim();
        if(!/^[A-Za-z0-9:_-]{1,120}$/.test(id))throw new Error('Cinto de acessórios exige identificador de ação válido.');
        const prior=requireCharacter(this.state.player.character);
        const ledger=prior.utilityBeltReceipts??[];
        if(!Array.isArray(ledger)||ledger.length>2000||ledger.some(x=>!x||typeof x!=='object'||
            !/^[A-Za-z0-9:_-]{1,120}$/.test(String(x.actionId??''))||
            typeof x.itemId!=='string'||!['gear','weapon'].includes(x.kind))||
            new Set(ledger.map(x=>x.actionId)).size!==ledger.length)
            throw new Error('Histórico de Cinto de acessórios inválido ou duplicado.');
        const existing=ledger.find(x=>x.actionId===id);
        if(existing){
            if(existing.itemId!==assetId)throw new Error('Repetição de ação com item divergente.');
            return {duplicate:true,actionId:id,itemId:assetId,kind:existing.kind};
        }
        return this.atomicAction(()=>{
            const character=clone(requireCharacter(this.state.player.character));
            const receipts=Array.isArray(character.utilityBeltReceipts)?character.utilityBeltReceipts:[];
            if(receipts.length>=2000)throw new Error('Limite de recibos de Cinto de acessórios atingido; preserve o histórico antes de continuar.');
            const status=this.utilityBeltStatus();
            if(!status.talent.available)throw new Error('Cinto de acessórios não adquirido.');
            const allowed=status.items.find(x=>x.id===assetId);
            if(!allowed)throw new Error('Item não permitido pelo talento: exige raridade até 4, volume pequeno e catálogo validado.');
            const source=allowed.kind==='gear'?equipmentCatalog_mjs_1.generalGearCatalogEntry(assetId):equipmentCatalog_mjs_1.weaponCatalogEntry(assetId);
            if(!source)throw new Error('Item do catálogo não encontrado.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            const item={...clone(source),sourceCatalogId:source.id,ownedAssetId:`utility:${id}`,utilityBeltActionId:id};
            if(allowed.kind==='gear')character.gear=[...(character.gear||[]),{...item,quantity:1}];
            else character.weapons=[...(character.weapons||[]),{...item,quantity:1,active:false,equipped:false}];
            character.utilityBeltReceipts=[...receipts,{actionId:id,itemId:assetId,kind:allowed.kind}];
            this.commit('CHARACTER_UPDATED',{character});
            this.commit('GROUP_UPDATED',{group});
            return {duplicate:false,actionId:id,item:clone(item),kind:allowed.kind,destiny:clone(group.destiny)};
        });
    }


/**
 * P173: paga 2 de Tensão antes do teste e emite um recibo vinculado
 * ao ativo/pedido; o Mestre não pode fabricar um sucesso por JSON.
 */
resolveMerchantNegotiation({kind,targetId,difficulty=2,challenge=0,boost=0,setback=0,rng=Math.random}={}){
    return this.atomicAction(()=>{
        if(!['AOR','MIXED'].includes(this.state.campaignMode))throw new Error('Mercador especialista exige campanha AoR.');
        merchantTarget(this.state,kind,targetId);
        const character=requireCharacter(this.state.player?.character);
        const spec=talents_mjs_1.masterMerchantOption(character,{activated:true,checkSucceeded:false,kind,basePrice:0});
        const critical=assertCanPerformAction(character);
        if(critical.cannotVoluntarilySufferStrain)throw new Error('Ferimento Crítico impede Tensão voluntária.');
        if(!Number.isSafeInteger(difficulty)||difficulty<0||difficulty>5||
           !Number.isSafeInteger(challenge)||challenge<0||challenge>5||
           !Number.isSafeInteger(boost)||boost<0||boost>6||
           !Number.isSafeInteger(setback)||setback<0||setback>6)
            throw new Error('Parâmetros do teste de Negociação inválidos.');
        const paid=health_mjs_1.sufferStrain(character,spec.strainCost,{voluntary:true});
        if(paid.amount!==2)throw new Error('Pagamento de Tensão inconsistente.');
        assertCanAct(paid.character);
        this.commit('CHARACTER_UPDATED',{character:paid.character});
        const check=this.resolveSkillCheck({skillKey:'negociacion',difficulty,challenge,boost,setback,rng});
        const receipt=issueMerchantReceipt(this.state,{id:ids_mjs_1.newId('merchant'),kind,targetId,check});
        this.commit('MERCHANT_NEGOTIATION_RECORDED',{receipt});
        return {check,receipt:clone(receipt)};
    });
}
completeMerchantSale({assetId,price,receiptId,legalSale=true,sourceRuleIds=['AoR:Mercador especialista'],reason=''}={}){
    return this.atomicAction(()=>{
        const receipt=merchantReceiptFor(this.state,{receiptId,kind:'sell',targetId:assetId,requireSuccess:true});
        const result=economy_mjs_1.sellOwnedAsset(this.state,assetId,{
            price,legalSale,masterMerchant:true,negotiationSucceeded:true,
            masterMerchantPrepaid:true,sourceRuleIds,reason
        });
        this.state=result.state;
        this.commit('MERCHANT_NEGOTIATION_CONSUMED',{receiptId});
        const record={...result.record,negotiationReceiptId:receipt.id};
        this.state.master.acquisitionHistory=[...(this.state.master.acquisitionHistory??[]),clone(record)];
        this.commit('ECONOMY_TRANSACTION',{record});
        return {record,state:clone(this.state)};
    });
}
completeMerchantAcquisition({receiptId,approved,finalPrice=null,sourceRuleIds=['AoR:Mercador especialista'],reason=''}={}){
    return this.atomicAction(()=>{
        const pending=this.state.master?.pendingAcquisition;
        if(!pending)throw new Error('Aquisição não está pendente.');
        const receipt=merchantReceiptFor(this.state,{receiptId,kind:'buy',targetId:pending.requestId});
        if(approved===true&&!receipt.success)throw new Error('Negociação fracassada não autoriza desconto.');
        const result=economy_mjs_1.resolvePendingAcquisition(this.state,pending,{
            approved,finalPrice,masterMerchant:true,negotiationSucceeded:receipt.success,
            masterMerchantPrepaid:true,sourceRuleIds,reason
        });
        this.state=result.state;
        this.state.master.pendingAcquisition=null;
        this.commit('MERCHANT_NEGOTIATION_CONSUMED',{receiptId});
        const record={...result.record,negotiationReceiptId:receipt.id};
        this.state.master.acquisitionHistory=[...(this.state.master.acquisitionHistory??[]),clone(record)];
        this.commit('ECONOMY_TRANSACTION',{record});
        return {record,state:clone(this.state)};
    });
}
    /** P174: AoR field mechanics, once per active session, with real dice,
     * GM approval and one atomic persistent equipment transition. */
    useAoRMechanicFieldAction({kind,targetId,donorId=null,actionId,approved=false,rng=Math.random}={}){
      return this.atomicAction(()=>{
        const preflight=mechanicFieldPreflight(this.state,{kind,targetId,donorId,actionId,approved});
        if(preflight.duplicate)return {duplicate:true,receipt:clone(preflight.duplicate)};
        const character=requireCharacter(this.state.player.character);
        const canonical=kind==='redundant'?'REDUNDANT_SYS':'BAD_MOTIVATOR';
        if(talents_mjs_1.sourceBackedTalentRank(character,canonical)<1)
          throw new Error('Talento mecânico AoR não adquirido.');
        assertCanPerformAction(character);
        const check=this.resolveSkillCheck({skillKey:'mecanica',difficulty:kind==='redundant'?1:3,rng});
        const applied=Boolean(check.success&&approved);
        if(applied){
          const before=this.state;
          const effect=kind==='redundant'
            ? economy_mjs_1.repairOwnedAsset(before,targetId,{
                amount:Number(preflight.target.state.damage),cost:0,
                sourceRuleIds:['AoR Core: Redundant Systems'],reason:'Componentes de dispositivo idêntico, doador preservado'})
            : economy_mjs_1.damageOwnedAsset(before,targetId,{
                amount:1,sourceRuleIds:['AoR Core: Bad Motivator'],reason:'Falha espontânea autorizada pelo Mestre'});
          this.state=effect.state;
          if(kind==='motivator'){
            const target=this.state.group.ownedAssets.find(a=>a.assetId===targetId);
            target.state.condition='broken';
          }
          this.commit('ECONOMY_TRANSACTION',{record:{...effect.record,mechanicActionId:actionId}});
        }
        const receipt=mechanicFieldReceipt(this.state,{kind,targetId,donorId,actionId,approved,check,applied});
        this.commit('AOR_MECHANIC_ACTION',{receipt});
        return {duplicate:false,check,receipt:clone(receipt),state:clone(this.state)};
      });
    }
    legalSaleQuote({assetId,basePrice,masterMerchant=false}={}){
        const asset=(this.state.group?.ownedAssets??[]).find(x=>x.assetId===String(assetId??''));
        if(!asset)throw new Error('Ativo possuído não encontrado.');
        if(asset.availability?.restricted)throw new Error('Ativo restrito não pode ser vendido legalmente.');
        const character=requireCharacter(this.state.player?.character);
        const sale=talents_mjs_1.wheelAndDealLegalSale(character,{legalSale:true,restricted:false,basePrice});
        const merchant=talents_mjs_1.masterMerchantOption(character,{activated:masterMerchant,checkSucceeded:true,kind:'sell',basePrice});
        return {assetId:asset.assetId,...sale,finalPrice:sale.finalPrice+merchant.bonus,masterMerchant:merchant};
    }
    economyCatalog() { return economy_mjs_1.genericEconomyCatalog(); }
    economySummary() { return economy_mjs_1.economySummary(this.state, { market: sandbox_mjs_1.marketProfile(this.state.world, this.state.scene?.locationId ?? null) }); }
    acquisitionAvailability(kind, itemId, { rarityModifier = undefined } = {}) {
        const market = sandbox_mjs_1.marketProfile(this.state.world, this.state.scene?.locationId ?? null);
        const modifier = rarityModifier === undefined ? market?.rarityModifier ?? null : rarityModifier;
        return economy_mjs_1.assetAvailability(kind, itemId, { rarityModifier: modifier });
    }
    setPendingAcquisitionRarityModifier(rarityModifier) {
        const existing=this.state.master?.pendingAcquisition;
        let pending = economy_mjs_1.setPendingAcquisitionRarityModifier(existing, rarityModifier);
        if(existing?.talentUse?.sessionKey==='knowSomebody'){
            const rank=Number(existing.talentUse.rank??0);
            if(!Number.isSafeInteger(rank)||rank<1||talents_mjs_1.sourceBackedTalentRank(this.state.player.character,'KNOWSOMEBODY')<rank||!talentUsedThisSession(this.state.player.character,'knowSomebody'))throw new Error('Recibo de Provedor habitual inválido.');
            pending.availability={...rarity_mjs_1.availabilityProfile({rarity:Math.max(0,existing.rarity-rank),restricted:false,rarityModifier}),originalRarity:existing.rarity,knowSomebodyRanks:rank};
        }
        this.commit('EQUIPMENT_ACQUISITION_PENDING', { pending });
        return structuredClone(this.state.master.pendingAcquisition);
    }
    proposeAcquisition(kind, itemId, {useKnowSomebody=false}={}) {
        return this.atomicAction(()=>{
        if(typeof useKnowSomebody!=='boolean')throw new Error('Uso de Provedor habitual inválido.');
        const character = requireCharacter(this.state.player.character);
        if (this.state.master?.pendingAcquisition) throw new Error('Já existe uma aquisição aguardando decisão do Mestre.');
        const market = sandbox_mjs_1.marketProfile(this.state.world, this.state.scene?.locationId ?? null);
        const pending = economy_mjs_1.createPendingAcquisition(kind, itemId, character, { requestId: ids_mjs_1.newId('acq'), locationId: this.state.scene?.locationId ?? null, market });
        if(useKnowSomebody){
            const rank=talents_mjs_1.sourceBackedTalentRank(character,'KNOWSOMEBODY');
            if(rank<1)throw new Error('Provedor habitual não foi adquirido.');
            if(pending.restricted)throw new Error('Provedor habitual não funciona em itens restritos.');
            if(talentUsedThisSession(character,'knowSomebody'))throw new Error('Provedor habitual já foi usado nesta sessão.');
            const reduction=Math.min(rank,pending.rarity);
            const mod=pending.availability.rarityModifier;
            const rar=rarity_mjs_1.availabilityProfile({rarity:pending.rarity-reduction,restricted:false,rarityModifier:mod});
            pending.availability={...rar,originalRarity:pending.rarity,knowSomebodyRanks:rank};
            pending.talentUse={name:'Provedor habitual',rank,sessionKey:'knowSomebody'};
            this.commit('EQUIPMENT_ACQUISITION_PENDING',{pending,character:markTalentUsedThisSession(character,'knowSomebody')});
        } else this.commit('EQUIPMENT_ACQUISITION_PENDING', { pending });
        const skillLabel = pending.availability.skillKey === 'callejeo' ? 'Vida nas ruas' : 'Negociação';
        const result = this.submitIntent(`Quero adquirir ${pending.itemName} por ${pending.listedPrice} créditos. O mercado local aplica modificador de raridade ${pending.availability.rarityModifier ?? 'ainda não definido'}; use ${skillLabel} e as regras de disponibilidade recuperadas. Não altere créditos nem propriedade sem decisão estruturada.`);
        return { ...result, pendingAcquisition: structuredClone(this.state.master.pendingAcquisition) };
        });
    }
    weaponAcquisitionAvailability(itemId, { rarityModifier = null } = {}) {
        return this.acquisitionAvailability('weapon', itemId, { rarityModifier });
    }
    setPendingWeaponAcquisitionRarityModifier(rarityModifier) {
        return this.setPendingAcquisitionRarityModifier(rarityModifier);
    }
    proposeWeaponAcquisition(itemId) { return this.proposeAcquisition('weapon', itemId); }
    proposeAttachmentAcquisition(itemId) { return this.proposeAcquisition('attachment', String(itemId??'')); }
    tinkererStatus() {
        return talents_mjs_1.tinkererStatus(requireCharacter(this.state.player.character));
    }
    applyTinkererHardPoint({kind='weapons',itemIndex}={}) {
        const result=talents_mjs_1.applyTinkererHardPoint(requireCharacter(this.state.player.character),{kind,itemIndex});
        this.commit('CHARACTER_UPDATED',{character:result.character});
        return {...result,status:this.tinkererStatus(),equipment:this.equipmentOwnedSummary()};
    }
    attachmentSummary() {
        const character=requireCharacter(this.state.player.character);
        const gearhead=talents_mjs_1.gearheadModPricing(character);
        const weapons=(character.weapons??[]).map((w,i)=>({weaponIndex:i,id:w.id??w.sourceCatalogId??w.name,name:w.name,sourceCatalogId:w.sourceCatalogId??null,hardPoints:equipmentCatalog_mjs_1.itemHardPoints(w),hardPointsAvailable:equipmentCatalog_mjs_1.itemAvailableHardPoints(w),attachments:(w.attachments??[]).map(a=>({...clone(a),modStatus:attachments_mjs_1.attachmentModStatus(a)}))}));
        const owned=(this.state.group?.ownedAssets??[]).filter(a=>a?.kind==='attachment').map(a=>{
            const installed=weapons.flatMap(w=>w.attachments.map(x=>({weapon:w,attachment:x}))).find(x=>x.attachment?.ownedAssetId===a.assetId);
            const retained=installed?.attachment??{catalogId:a.catalogId,mods:clone(a.state?.mods??[]),modAttempts:clone(a.state?.modAttempts??[]),disabled:Boolean(a.state?.useless)};
            return {assetId:a.assetId,catalogId:a.catalogId,name:a.name,state:clone(a.state??{}),ownership:clone(a.ownership??{}),installedAttachment:installed?clone(installed.attachment):null,installedWeaponId:installed?.weapon?.id??null,modStatus:attachments_mjs_1.attachmentModStatus(retained).map(m=>({...m,baseCost:m.cost,cost:talents_mjs_1.gearheadModPricing(character,m.cost).cost}))};
        });
        return {catalog:attachments_mjs_1.attachmentCatalog(),owned,weapons,gearhead};
    }
    installOwnedAttachment(assetId, weaponId) {
        return this.atomicAction(() => {
        const id=String(assetId??'').trim(); const character=clone(requireCharacter(this.state.player.character));
        const group=clone(this.state.group??{}); group.ownedAssets=Array.isArray(group.ownedAssets)?group.ownedAssets:[];
        const asset=group.ownedAssets.find(a=>a?.assetId===id&&a?.kind==='attachment'); if(!asset)throw new Error('Attachment adquirido não encontrado.');
        if(asset.state?.installed)throw new Error('Este attachment já está instalado.');
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        const weapon=(character.weapons??[]).find(w=>[w.ownedAssetId,w.id,w.sourceCatalogId,w.name].some(v=>String(v??'').trim().toLocaleLowerCase('pt-BR')===target));
        if(!weapon)throw new Error('Arma possuída não encontrada para instalação.');
        const attachment=attachments_mjs_1.attachmentCatalogEntry(asset.catalogId); if(!attachment)throw new Error('Perfil mecânico do attachment não está autorizado.');
        const compatibility=attachments_mjs_1.attachmentCompatibility(attachment,weapon); if(!compatibility.compatible)throw new Error(compatibility.reason);
        if(!equipmentCatalog_mjs_1.canInstallAttachment(weapon,attachment))throw new Error('Hard points insuficientes para instalar este attachment.');
        weapon.attachments=Array.isArray(weapon.attachments)?weapon.attachments:[];
        const payload=attachments_mjs_1.attachmentInstallPayload(attachment,id);
        payload.mods=clone(asset.state?.mods??[]);
        payload.modAttempts=clone(asset.state?.modAttempts??[]);
        payload.disabled=Boolean(asset.state?.useless);
        weapon.attachments.push(payload);
        asset.state={...(asset.state??{}),installed:true,targetWeaponId:weapon.ownedAssetId??weapon.id??weapon.sourceCatalogId??weapon.name,installedAt:new Date().toISOString(),mods:clone(payload.mods),modAttempts:clone(payload.modAttempts)};
        this.commit('CHARACTER_UPDATED',{character}); this.commit('GROUP_UPDATED',{group});
        return this.attachmentSummary();
        });
    }
    removeOwnedAttachment(assetId) {
        return this.atomicAction(() => {
        const id=String(assetId??'').trim(); const character=clone(requireCharacter(this.state.player.character));
        const group=clone(this.state.group??{}); group.ownedAssets=Array.isArray(group.ownedAssets)?group.ownedAssets:[];
        const asset=group.ownedAssets.find(a=>a?.assetId===id&&a?.kind==='attachment'); if(!asset)throw new Error('Attachment adquirido não encontrado.');
        let removed=false, retained=null; for(const weapon of character.weapons??[]){const found=(weapon.attachments??[]).find(a=>a?.ownedAssetId===id);if(found)retained=clone(found);const before=(weapon.attachments??[]).length;weapon.attachments=(weapon.attachments??[]).filter(a=>a?.ownedAssetId!==id);if(weapon.attachments.length<before)removed=true;}
        if(!removed)throw new Error('Este attachment adquirido não está instalado.');
        asset.state={...(asset.state??{}),installed:false,targetWeaponId:null,removedAt:new Date().toISOString(),mods:clone(retained?.mods??asset.state?.mods??[]),modAttempts:clone(retained?.modAttempts??asset.state?.modAttempts??[]),useless:Boolean(retained?.disabled||asset.state?.useless)};
        this.commit('CHARACTER_UPDATED',{character}); this.commit('GROUP_UPDATED',{group});
        return this.attachmentSummary();
        });
    }
    attemptAttachmentMod(assetId, modId, { rng = Math.random } = {}) {
        return this.atomicAction(() => {
        if(this.state.combat?.active) throw new Error('Mods de attachment exigem tempo de oficina; encerre o combate primeiro.');
        const character=clone(requireCharacter(this.state.player.character));
        const group=clone(this.state.group??{}); group.ownedAssets=Array.isArray(group.ownedAssets)?group.ownedAssets:[];
        const id=String(assetId??'').trim(); const key=String(modId??'').trim();
        const asset=group.ownedAssets.find(a=>a?.assetId===id&&a?.kind==='attachment');
        if(!asset) throw new Error('Attachment adquirido não encontrado.');
        if(!asset.state?.installed) throw new Error('Instale o attachment em uma arma antes de modificar.');
        if(asset.state?.useless) throw new Error('Este attachment foi inutilizado por uma tentativa de modificação malsucedida.');
        const hasToolkit=(character.gear??[]).some(g=>String(g?.id??g?.sourceCatalogId??'')==='tool_kit'||/kit de ferramentas|tool kit/i.test(String(g?.name??'')));
        if(!hasToolkit) throw new Error('É necessário possuir um Kit de ferramentas para instalar Mods.');
        const pricing=talents_mjs_1.gearheadModPricing(character);
        if(Number(character.credits??0)<pricing.cost) throw new Error(`Créditos insuficientes: esta tentativa de Mod custa ${pricing.cost} créditos.`);
        let weapon=null, installed=null;
        for(const w of character.weapons??[]){
            const found=(w.attachments??[]).find(a=>a?.ownedAssetId===id);
            if(found){weapon=w;installed=found;break;}
        }
        if(!weapon||!installed) throw new Error('Estado instalado do attachment não foi encontrado na arma.');
        const option=attachments_mjs_1.attachmentModStatus(installed).find(m=>m.id===key);
        if(!option) throw new Error('Opção de Mod não existe neste attachment.');
        if(Number(option.remaining??0)<=0) throw new Error('Esta opção de Mod não possui tentativas disponíveis.');
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.intelecto??0),Number(character.skills?.mecanica??0));
        const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:Number(option.difficulty),challenge:0,boost:0,setback:0},rng);
        character.credits=Math.max(0,Number(character.credits??0)-pricing.cost);
        installed.modAttempts=Array.isArray(installed.modAttempts)?installed.modAttempts:[];
        installed.mods=Array.isArray(installed.mods)?installed.mods:[];
        const at=new Date().toISOString();
        const attempt={modId:key,name:option.name,status:check.success?'installed':'failed',cost:pricing.cost,baseCost:pricing.baseCost,gearheadRank:pricing.rank,difficulty:Number(option.difficulty),at,check:{success:check.success,netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair},effect:clone(option.effect)};
        installed.modAttempts.push(clone(attempt));
        if(check.success) installed.mods.push(clone(attempt));
        else if(Number(check.despair??0)>0){installed.disabled=true;asset.state={...(asset.state??{}),useless:true,disabledAt:at};}
        asset.state={...(asset.state??{}),mods:clone(installed.mods),modAttempts:clone(installed.modAttempts),useless:Boolean(installed.disabled||asset.state?.useless),lastModAttemptAt:at};
        this.commit('CHARACTER_UPDATED',{character}); this.commit('GROUP_UPDATED',{group}); this.commit('CHECK_RESOLVED',check);
        return {success:check.success,check,attempt,attachment:clone(installed),weapon:attachments_mjs_1.effectiveWeaponWithAttachments(weapon),creditsRemaining:character.credits,summary:this.attachmentSummary()};
        });
    }
    aorP170Status(){
        const character=requireCharacter(this.state.player.character);
        const health=health_mjs_1.getHealthStatus(character);
        const status=statusEffects_mjs_1.statusModifiers(character);
        return {
            hardHeaded:talents_mjs_1.hardHeadedOption(character),
            improvedHardHeaded:talents_mjs_1.hardHeadedOption(character,{improved:true}),
            bodyGuard:talents_mjs_1.bodyGuardOption(character),
            juryRigged:{nodes:talents_mjs_1.juryRiggedPurchaseNodes(character),
                assignments:clone(character.talentAssignments?.juryRigged??[])},
            weapons:(character.weapons??[]).map((w,i)=>({index:i,name:w.name,instanceId:w.juryRiggedInstanceId??null,
                effect:talents_mjs_1.juryRiggedWeapon(character,w).applied})),
            companions:(this.state.group?.companions??[]).map(m=>({id:m.id,name:m.character?.name??'Companheiro'})),
            health,status:{staggered:status.cannotAct,disoriented:character.statusEffects?.some(e=>e.type==='disoriented'&&e.active!==false)??false},
            combatActive:Boolean(this.state.combat?.active),bodyGuardActive:clone(this.state.combat?.bodyGuard??null),
            bodyGuardUsed:Boolean(this.state.combat?.playerTurn?.bodyGuardUsed),
            npcCompanionTargetId:this.state.combat?.npcCompanionTargetId??null,
            hardHeadedRecovery:clone(this.state.combat?.hardHeadedRecovery??null)
        };
    }

    /** P171 — GM-selected companion target is resolved by the actual NPC
     * initiative pipeline, not a free attack button. Clearing uses null. */
    /** P179 — AoR Medic: prepare a once/session, out-of-turn Hard Medicine
     * reaction for an ACTUAL critical injury to an allied companion. */
    aorP179Status(){
      const c=requireCharacter(this.state.player.character),combat=this.state.combat;
      const rank=k=>talents_mjs_1.sourceBackedTalentRank(c,k);
      return {sessionActive:this.state.session?.status==='ativa',
        naturalOutdoorsman:rank('NATOUT')>0,
        timeToGo:rank('TIME_TO_GO')>0,improvedTimeToGo:rank('TIME_TO_GO_IMPROVED')>0,
        itsNotThatBad:rank('ITS_NOT_THAT_BAD')>0,
        medicUsed:talentUsedThisSession(c,'itsNotThatBad'),
        medicPrepared:clone(combat.aorCriticalAid??null),
        pendingBlasts:clone(combat.pendingNpcBlasts??[]),
        companions:(this.state.group?.companions??[]).map(x=>({id:x.id,name:x.character?.name??x.name??x.id})),
        destinyLight:Number(this.state.group?.destiny?.light??0)};
    }
    prepareItsNotThatBad({companionId,approved=false}={}){
      return this.atomicAction(()=>{
        const character=requireCharacter(this.state.player.character),combat=clone(this.state.combat);
        if(!['AOR','MIXED'].includes(campaignMode(this.state))||this.state.session?.status!=='ativa'||!combat.active)
          throw Error('Não é tão grave exige sessão AoR e combate ativos.');
        if(talents_mjs_1.sourceBackedTalentRank(character,'ITS_NOT_THAT_BAD')<1)
          throw Error('Não é tão grave não adquirido.');
        if(talentUsedThisSession(character,'itsNotThatBad'))throw Error('Não é tão grave já utilizado nesta sessão.');
        if(health_mjs_1.getHealthStatus(character).unconscious)throw Error('Médico incapacitado.');
        if(approved!==true)throw Error('Mestre deve confirmar o aliado protegido.');
        const member=this._medicalCompanion(companionId);
        if(!member||health_mjs_1.getHealthStatus(member.character).unconscious)throw Error('Aliado indisponível.');
        combat.aorCriticalAid={companionId,sessionId:this.state.sessionId};
        combat.log.push({type:'AOR_CRITICAL_AID_ARMED',companionId,sessionId:this.state.sessionId,round:combat.round});
        this.commit('COMBAT_UPDATED',{combat});
        return clone(combat.aorCriticalAid);
      });
    }
    /** P179: spend the original NPC attack's Blast result once. GM confirms
     * affected targets, but cannot supply invented damage or an attack ID. */
    resolveIncomingBlast({attackId,includePlayer=true,affectedCompanionIds=[],
      useTimeToGo=false,escapeConfirmed=false,improvedCompanionId=null,
      companionEscapeConfirmed=false,engagedConfirmed=false,approved=false,criticalRng=Math.random}={}){
      return this.atomicAction(()=>{
        if(approved!==true)throw Error('Mestre deve confirmar a zona de Explosão.');
        let combat=clone(this.state.combat),character=requireCharacter(this.state.player.character),group=clone(this.state.group);
        const receipt=(combat.pendingNpcBlasts??[]).find(r=>r.attackId===attackId);
        assertBlastReceipt(combat,receipt,this.state);
        if(!Array.isArray(affectedCompanionIds)||new Set(affectedCompanionIds).size!==affectedCompanionIds.length)
          throw Error('Alvos de Explosão inválidos ou duplicados.');
        if(affectedCompanionIds.some(id=>typeof id!=='string'||!group.companions.some(c=>c.id===id)))
          throw Error('Alvo secundário não pertence ao grupo.');
        if(typeof includePlayer!=='boolean'||(!includePlayer&&!affectedCompanionIds.length))
          throw Error('Explosão exige pelo menos um alvo confirmado.');
        if(receipt.hit&&receipt.originalTargetCompanionId&&affectedCompanionIds.includes(receipt.originalTargetCompanionId))
          throw Error('Alvo principal já recebeu o acerto; Explosão atinge apenas outros alvos.');
        if(receipt.hit&&!receipt.originalTargetCompanionId&&includePlayer)
          throw Error('Jogador já recebeu o acerto principal; Explosão atinge apenas outros alvos.');
        if(!useTimeToGo&&(escapeConfirmed||improvedCompanionId!==null||companionEscapeConfirmed||engagedConfirmed))
          throw Error('Deslocamento reativo sem Time to Go.');
        if(useTimeToGo){
          if(!includePlayer||talents_mjs_1.sourceBackedTalentRank(character,'TIME_TO_GO')<1)
            throw Error('Cuerpo a tierra exige personagem atingido com talento adquirido.');
          if(escapeConfirmed!==true)throw Error('Mestre deve confirmar saída real da zona de Explosão.');
          if(improvedCompanionId!==null){
            if(talents_mjs_1.sourceBackedTalentRank(character,'TIME_TO_GO_IMPROVED')<1||
               !affectedCompanionIds.includes(improvedCompanionId)||engagedConfirmed!==true||companionEscapeConfirmed!==true)
              throw Error('Time to Go aprimorado exige aliado Engajado e fuga confirmada.');
          } else if(companionEscapeConfirmed||engagedConfirmed)throw Error('Aliado não identificado para fuga aprimorada.');
          const destiny=destiny_mjs_1.flipDestiny(group,'light');
          group=destiny;
        }
        const results=[];
        if(includePlayer&&!useTimeToGo){
          const d=health_mjs_1.applyDamage(character,{damage:receipt.blastDamage,reason:'Explosão inimiga',criticalRng});
          character=d.character;results.push({id:'player',suffered:d.suffered,criticalAdded:d.criticalAdded});
        }
        if(includePlayer&&useTimeToGo)results.push({id:'player',escaped:true,suffered:0});
        for(const id of affectedCompanionIds){
          if(useTimeToGo&&id===improvedCompanionId){results.push({id,escaped:true,suffered:0});continue;}
          const member=group.companions.find(c=>c.id===id);
          const d=health_mjs_1.applyDamage(member.character,{damage:receipt.blastDamage,reason:'Explosão inimiga',criticalRng});
          member.character=d.character;results.push({id,suffered:d.suffered,criticalAdded:d.criticalAdded});
        }
        if(useTimeToGo){
          const escapes=[{attackId,actorId:'player',sessionId:this.state.sessionId,round:combat.round}];
          if(improvedCompanionId)escapes.push({attackId,actorId:improvedCompanionId,sessionId:this.state.sessionId,round:combat.round});
          combat.aorBlastEscapes=[...(combat.aorBlastEscapes??[]),...escapes].slice(-100);
        }
        combat.pendingNpcBlasts=combat.pendingNpcBlasts.filter(r=>r.attackId!==attackId);
        combat.log.push({type:'AOR_NPC_BLAST_RESOLVED',attackId,sessionId:this.state.sessionId,
          round:combat.round,damage:receipt.blastDamage,timeToGo:Boolean(useTimeToGo),
          improvedCompanionId:improvedCompanionId??null,results:clone(results)});
        this.commit('COMBAT_ACTION',{combat,character,group});
        return {attackId,damage:receipt.blastDamage,results,usedTimeToGo:Boolean(useTimeToGo),
          improvedCompanionId:improvedCompanionId??null};
      });
    }
    setNpcCompanionTarget({companionId=null}={}){
        return this.atomicAction(()=>{
            const combat=clone(this.state.combat);
            if(!combat?.active||!combat.encounter?.slots?.length)
                throw new Error('Seleção de alvo NPC exige encontro com iniciativa ativo.');
            if(companionId!==null){
                if(typeof companionId!=='string'||!companionId.trim())throw new Error('Identificador de companheiro inválido.');
                const member=(this.state.group?.companions??[]).find(m=>m.id===companionId);
                if(!member)throw new Error('Companheiro não pertence ao grupo.');
                if(health_mjs_1.getHealthStatus(member.character).unconscious)
                    throw new Error('Companheiro incapacitado não pode ser selecionado como alvo.');
                combat.npcCompanionTargetId=companionId;
            } else delete combat.npcCompanionTargetId;
            combat.log.push({type:'ALVO_NPC_DEFINIDO',round:combat.round,companionId});
            this.commit('COMBAT_UPDATED',{combat});
            return {companionId:combat.npcCompanionTargetId??null,combat:clone(combat)};
        });
    }

    /** P175 — AoR Field Commander. Actual Leadership action, minion/companion
     * strain cost, and a durable immediate maneuver token. Source: AoR Core
     * pp.146, 74–75. Improved allows doubled Presence and Triumph free action.
     * No spatial claim is invented: GM must confirm Medium range explicitly.
     */
    aorCommandStatus(){
        const character=requireCharacter(this.state.player?.character);
        return {active:Boolean(this.state.combat?.active),sessionActive:this.state.session?.status==='ativa',round:this.state.combat?.round??0,
            fieldCommander:talents_mjs_1.sourceBackedTalentRank(character,'FIELD_COMMANDER'),
            improved:talents_mjs_1.sourceBackedTalentRank(character,'FIELD_COMMANDER_IMPROVED')>0,
            masterLeader:talents_mjs_1.sourceBackedTalentRank(character,'MASTER_LEADER')>0,
            situationalAwareness:talents_mjs_1.sourceBackedTalentRank(character,'SITUATIONAL_AWARENESS')>0,
            cleverSolution:talents_mjs_1.sourceBackedTalentRank(character,'CLEVER_SOLUTION')>0,
            coordinatedAssault:talents_mjs_1.sourceBackedTalentRank(character,'COORDINATED_ASSAULT')>0,
            actionUsed:Boolean(this.state.combat?.playerTurn?.actionUsed),
            masterLeaderUsed:Boolean(this.state.combat?.playerTurn?.masterLeaderUsed),
            fieldCommanderUsed:(this.state.combat?.aorFieldCommandHistory??[]).some(r=>r.round===this.state.combat.round),
            companions:(this.state.group?.companions??[]).map(c=>({id:c.id,name:c.character?.name??'Aliado',
                strain:c.character?.derived?.strain?.current??0})),
            pendingTokens:clone((this.state.combat?.aorCommandTokens??[]).filter(t=>t.round===this.state.combat.round&&!t.used)),
            coordinated:clone(this.state.combat?.aorCoordinatedAssault??null)};
    }
    useAoRFieldCommander({actionId,companionIds=[],confirmedMedium=false,
        useImproved=false,grantFreeAction=false,rng=Math.random}={}){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player?.character);
            if(!['AOR','MIXED'].includes(this.state.campaignMode)||
               character.creation?.rulesProfile!=='AOR_CORE'||
               talents_mjs_1.sourceBackedTalentRank(character,'FIELD_COMMANDER')<1)
                throw new Error('Coordenação tática exige talento AoR comprado.');
            if(!this.state.combat?.active||this.state.session?.status!=='ativa')
                throw new Error('Coordenação tática exige encontro e sessão ativos.');
            if(typeof actionId!=='string'||!/^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,95}$/.test(actionId))
                throw new Error('Identificador de comando inválido.');
            if(typeof useImproved!=='boolean'||typeof grantFreeAction!=='boolean'||
               typeof confirmedMedium!=='boolean'||!Array.isArray(companionIds)||
               companionIds.length===0||companionIds.length>8||
               companionIds.some(id=>typeof id!=='string'||!id.trim())||
               new Set(companionIds).size!==companionIds.length)
                throw new Error('Seleção de aliados para Coordenação tática inválida.');
            const history=this.state.combat.aorFieldCommandHistory??[];
            const previous=history.find(x=>x.actionId===actionId);
            if(previous){
                if(JSON.stringify(previous.companionIds)!==JSON.stringify(companionIds)||
                   previous.useImproved!==useImproved||previous.grantFreeAction!==grantFreeAction||
                   previous.confirmedMedium!==confirmedMedium)
                    throw new Error('Comando repetido com parâmetros divergentes.');
                return {duplicate:true,receipt:clone(previous)};
            }
            if(history.some(x=>x.round===this.state.combat.round))
                throw new Error('Coordenação tática já usada nesta rodada.');
            if(!confirmedMedium)throw new Error('Confirme alcance Médio dos aliados.');
            const improved=talents_mjs_1.sourceBackedTalentRank(character,'FIELD_COMMANDER_IMPROVED')>0;
            if(useImproved&&!improved)throw new Error('Coordenação tática aprimorada não adquirida.');
            if(grantFreeAction&&!useImproved)throw new Error('Ação gratuita exige Coordenação tática aprimorada.');
            const presence=Number(character.characteristics?.presencia??0);
            if(!Number.isSafeInteger(presence)||presence<1||presence>6||
               companionIds.length>presence*(useImproved?2:1))
                throw new Error('Número de aliados excede Presença do comandante.');
            const group=clone(this.state.group);
            for(const id of companionIds){
                const ally=(group.companions??[]).find(m=>m.id===id);
                if(!ally||health_mjs_1.getHealthStatus(ally.character).unconscious)
                    throw new Error('Aliado não encontrado ou incapacitado.');
            }
            // Uses the normal check pipeline, including real dice, critical
            // penalties and the one-action-per-turn combat gate.
            const check=this.resolveSkillCheck({skillKey:'liderazgo',difficulty:2,rng});
            // A failed Triumph spend must not refund the dice/action: grant the
            // normal maneuver instead, preventing infinite free rerolls.
            const freeActionGranted=Boolean(check.success&&grantFreeAction&&Number(check.triumph??0)>0);
            const combat=clone(this.state.combat),tokens=[];
            if(check.success){
                for(const id of companionIds){
                    const ally=group.companions.find(m=>m.id===id);
                    ally.character=health_mjs_1.sufferStrain(ally.character,1,{voluntary:true}).character;
                    tokens.push({companionId:id,round:combat.round,
                        kind:freeActionGranted?'action':'maneuver',used:false});
                }
            }
            const receipt={actionId,round:combat.round,companionIds:[...companionIds],
                useImproved,grantFreeAction,freeActionGranted,confirmedMedium,success:check.success,
                netSuccess:check.netSuccess,triumph:check.triumph??0,
                tokens:clone(tokens),sessionId:this.state.sessionId};
            combat.aorFieldCommandHistory=[...history,receipt].slice(-64);
            combat.aorCommandTokens=[...(combat.aorCommandTokens??[]),...tokens];
            combat.log.push({type:'AOR_FIELD_COMMANDER',round:combat.round,
                actionId,success:check.success,companions:companionIds.length});
            if(check.success)this.commit('GROUP_UPDATED',{group});
            this.commit('COMBAT_UPDATED',{combat});
            return {duplicate:false,check,receipt:clone(receipt),tokens:clone(tokens)};
        });
    }
    /** Consume the free maneuver by a concrete defensive move; cover has a
     * real setback on the next incoming companion attack in this round. */
    useAoRCommandedCover({companionId}={}){
        return this.atomicAction(()=>{
            const combat=clone(this.state.combat);
            if(!combat.active)throw new Error('Cobertura comandada exige combate ativo.');
            const token=(combat.aorCommandTokens??[]).find(t=>t.companionId===companionId&&
                t.round===combat.round&&!t.used&&t.kind==='maneuver');
            if(!token)throw new Error('Nenhuma manobra gratuita disponível para este aliado.');
            if(!(this.state.group.companions??[]).some(m=>m.id===companionId))
                throw new Error('Aliado não pertence ao grupo.');
            token.used=true;
            combat.aorCompanionCover={...(combat.aorCompanionCover??{}),[companionId]:combat.round};
            combat.log.push({type:'AOR_COMMANDED_COVER',round:combat.round,companionId});
            this.commit('COMBAT_UPDATED',{combat});
            return {companionId,round:combat.round,coverSetback:1};
        });
    }
    /** P175 — Coordinated Assault maneuver. The source grants automatic
     * Advantage (not Boost) to engaged allies' next combat check this round. */
    useAoRCoordinatedAssault({companionIds=[],confirmedEngaged=false,maneuverMethod='normal'}={}){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player?.character);
            if(talents_mjs_1.sourceBackedTalentRank(character,'COORDINATED_ASSAULT')<1)
                throw new Error('Ataque coordenado AoR não adquirido.');
            if(!this.state.combat?.active||confirmedEngaged!==true)
                throw new Error('Ataque coordenado exige combate ativo e aliados em Interação.');
            if(!Array.isArray(companionIds)||!companionIds.length||
               new Set(companionIds).size!==companionIds.length||
               companionIds.some(id=>!(this.state.group?.companions??[]).some(c=>c.id===id)))
                throw new Error('Aliados inválidos para Ataque coordenado.');
            const leadership=Number(character.skills?.liderazgo??0);
            if(!Number.isSafeInteger(leadership)||leadership<1||companionIds.length>leadership)
                throw new Error('Ataque coordenado limitado aos graus de Liderança.');
            if(this.state.combat.aorCoordinatedAssault?.round===this.state.combat.round)
                throw new Error('Ataque coordenado já usado nesta rodada.');
            const moved=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Ataque coordenado'});
            const combat=clone(moved.combat);
            combat.aorCoordinatedAssault={round:combat.round,companionIds:[...companionIds],consumed:[]};
            combat.log.push({type:'AOR_COORDINATED_ASSAULT',round:combat.round,companionIds:[...companionIds]});
            this.commit('COMBAT_ACTION',{combat,character:moved.character});
            return clone(combat.aorCoordinatedAssault);
        });
    }
    /** P170 — source-backed Body Guard maneuver for a real companion.
     * The protected target is stored on the combat ledger, never on a UI flag. */
    useBodyGuard({companionId,strain=1,confirmedEngaged=false,maneuverMethod='normal'}={}){
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            assertCanPerformAction(character);assertPlayerInitiativeTurn(this.state.combat);
            if(!this.state.combat?.active)throw new Error('Guarda-costas exige combate ativo.');
            if(this.state.combat.playerTurn?.bodyGuardUsed)throw new Error('Guarda-costas já foi usado nesta rodada.');
            if(confirmedEngaged!==true)throw new Error('Confirme que o companheiro está em alcance de Interação.');
            const member=(this.state.group?.companions??[]).find(c=>c.id===companionId);
            if(!member)throw new Error('Companheiro protegido não pertence ao grupo.');
            const option=talents_mjs_1.bodyGuardOption(character,strain);
            if(!option.available||strain<1)throw new Error('Guarda-costas exige talento adquirido e ao menos 1 de Tensão.');
            if(criticalActionState(character).cannotVoluntarilySufferStrain)
                throw new Error('Ferimento Crítico impede Tensão voluntária para Guarda-costas.');
            const move=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Guarda-costas'});
            character=health_mjs_1.sufferStrain(move.character,option.strainCost,{voluntary:true}).character;
            assertCanAct(character);
            const combat=clone(move.combat);
            combat.playerTurn={...(combat.playerTurn??{}),bodyGuardUsed:true};
            combat.bodyGuard={companionId,difficultyUpgrades:option.difficultyUpgrades,activatedRound:combat.round};
            combat.log.push({type:'GUARDA_COSTAS',round:combat.round,companionId,upgrades:option.difficultyUpgrades});
            this.commit('COMBAT_ACTION',{combat,character});
            return {companionId,...option,combat:clone(combat),character:clone(character)};
        });
    }
    /** GM-initiated incoming attack against an actual owned companion. The
     * dice pool, Body Guard upgrades, damage and roster save are atomic. */
    resolveIncomingCompanionAttack({companionId,attackerCharacteristic=2,attackerSkill=1,
        weaponDamage=5,pierce=0,difficulty=2,damageType='wounds',rng=Math.random}={}){
        return this.atomicAction(()=>{
            if(!this.state.combat?.active)throw new Error('Ataque contra companheiro exige combate ativo.');
            if(!Number.isSafeInteger(attackerCharacteristic)||attackerCharacteristic<0||attackerCharacteristic>6||
               !Number.isSafeInteger(attackerSkill)||attackerSkill<0||attackerSkill>5||
               !Number.isSafeInteger(weaponDamage)||weaponDamage<0||weaponDamage>100||
               !Number.isSafeInteger(pierce)||pierce<0||pierce>20||
               !Number.isSafeInteger(difficulty)||difficulty<1||difficulty>5||
               !['wounds','strain'].includes(damageType))throw new Error('Perfil de ataque inimigo inválido.');
            const group=clone(this.state.group),member=(group.companions??[]).find(m=>m.id===companionId);
            if(!member)throw new Error('Alvo não é um companheiro do grupo.');
            const combat=clone(this.state.combat);
            const guard=combat.bodyGuard?.companionId===companionId?combat.bodyGuard:null;
            const upgrades=guard?Number(guard.difficultyUpgrades??0):0;
            if(!Number.isSafeInteger(upgrades)||upgrades<0||upgrades>5)throw new Error('Proteção de Guarda-costas inválida.');
            const negative=difficultyWithUpgrades(difficulty,0,upgrades);
            const positive=narrativeDice_mjs_1.buildPositivePool(attackerCharacteristic,attackerSkill);
            const coverSetback=combat.aorCompanionCover?.[companionId]===combat.round?1:0;
            const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:negative.difficulty,
                challenge:negative.challenge,setback:coverSetback},rng);
            let damage=null;
            if(check.success){
                damage=health_mjs_1.applyDamage(member.character,{damage:weaponDamage+Math.max(0,check.netSuccess),pierce,
                    damageType,reason:'Ataque inimigo contra companheiro',criticalRng:rng});
                member.character=damage.character;
            }
            combat.log.push({type:'ATAQUE_COMPANHEIRO',round:combat.round,companionId,upgrades,coverSetback,
                success:check.success,damage:damage?.suffered??0});
            this.commit('GROUP_UPDATED',{group});this.commit('COMBAT_UPDATED',{combat});
            return {check,companionId,bodyGuardUpgrades:upgrades,coverSetback,damage,character:clone(member.character)};
        });
    }

    /** P170 — permanent Jury Rigged allocation. No material item mutation:
     * effects are derived only for an owned, matching source purchase. */
    assignJuryRigged({nodeId,weaponIndex=0,effect='damage'}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw new Error('Ajuste de equipamento exige estar fora de combate.');
            const character=clone(requireCharacter(this.state.player.character));
            const eligible=talents_mjs_1.juryRiggedPurchaseNodes(character);
            if(!eligible.includes(nodeId))throw new Error('Melhorias caseiras não foram adquiridas neste nó.');
            if(!['damage','crit'].includes(effect))throw new Error('Melhoria de arma inválida.');
            if(!Number.isSafeInteger(weaponIndex)||weaponIndex<0)throw new Error('Índice de arma inválido.');
            const weapon=character.weapons?.[weaponIndex];
            if(!weapon||!['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','armas_distancia_pesadas','artilleria'].includes(weapon.skill))
                throw new Error('Melhorias caseiras exigem arma pessoal possuída.');
            if(effect==='crit'&&(!Number.isSafeInteger(weapon.crit)||weapon.crit<1))throw new Error('A arma não possui valor Crítico válido.');
            character.talentAssignments=character.talentAssignments??{};
            const assignments=Array.isArray(character.talentAssignments.juryRigged)?character.talentAssignments.juryRigged:[];
            // The same weapon cannot silently stack the two ranked benefits.
            const instanceId=weapon.juryRiggedInstanceId??ids_mjs_1.newId('jury');
            const occupied=assignments.find(a=>a.instanceId===instanceId&&a.nodeId!==nodeId&&eligible.includes(a.nodeId));
            if(occupied)throw new Error('Esta arma já possui Melhorias caseiras de outro grau.');
            weapon.juryRiggedInstanceId=instanceId;
            character.talentAssignments.juryRigged=[...assignments.filter(a=>a.nodeId!==nodeId),{nodeId,instanceId,effect}];
            this.commit('CHARACTER_UPDATED',{character});
            return {nodeId,weaponIndex,instanceId,effect,weapon:clone(weapon)};
        });
    }

    /** P170 — an action that can expressly override staggered/strain
     * incapacitation, but not death, wounds or critical incapacitation. */
    useHardHeaded({improved=false,rng=Math.random}={}){
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            let combat=clone(this.state.combat);
            assertPlayerInitiativeTurn(combat);
            const option=talents_mjs_1.hardHeadedOption(character,{improved});
            if(!option.available)throw new Error('Tenacidade não foi adquirida para esta ação.');
            const health=health_mjs_1.getHealthStatus(character);
            if(health.dead||health.woundIncapacitated||health.criticalIncapacitated)
                throw new Error('Tenacidade não supera morte ou incapacitação por ferimentos/críticos.');
            const statuses=statusEffects_mjs_1.statusModifiers(character);
            if(improved){
                if(!health.strainIncapacitated)throw new Error('Tenacidade aprimorada exige incapacitação por Tensão.');
                if(!combat?.active)throw new Error('Tenacidade aprimorada exige um turno de combate.');
                if(combat.encounter?.slots?.length &&
                   (!combat.hardHeadedRecovery?.ready||combat.hardHeadedRecovery.attempted))
                    throw new Error('Tenacidade aprimorada só pode ser usada no turno seguinte à incapacitação.');
            } else if(!statuses.cannotAct&&!character.statusEffects?.some(e=>e.type==='disoriented'&&e.active!==false))
                throw new Error('Tenacidade exige Atordoado ou Desorientado.');
            if(combat?.active){
                if(combat.playerTurn?.actionUsed)throw new Error('A ação deste turno já foi usada.');
                combat=combat_mjs_1.spendAction(combat,improved?'Tenacidade aprimorada':'Tenacidade');
            }
            const characteristic=Number(character.characteristics?.voluntad??0);
            const rank=Number(character.skills?.disciplina??0);
            const pool=narrativeDice_mjs_1.buildPositivePool(characteristic,rank);
            const check=narrativeDice_mjs_1.rollPool({...pool,difficulty:option.difficulty,
                setback:Number(statuses.setback??0)},rng);
            if(check.success){
                if(improved) character.derived.strain.current=Math.max(0,health.effectiveStrainThreshold-1);
                else{
                    character=statusEffects_mjs_1.removeStatusEffect(character,{type:'staggered'});
                    character=statusEffects_mjs_1.removeStatusEffect(character,{type:'disoriented'});
                }
            }
            if(improved&&combat.encounter?.slots?.length){
                combat.hardHeadedRecovery={...combat.hardHeadedRecovery,attempted:true,ready:false};
                if(!check.success){
                    combat.encounter={...combat.encounter,waitingForPlayer:false,outcome:'player_incapacitated'};
                }else delete combat.hardHeadedRecovery;
            }
            if(combat?.active)combat.log.push({type:'TENACIDADE',round:combat.round,
                improved:Boolean(improved),success:check.success,difficulty:option.difficulty});
            this.commit('COMBAT_ACTION',{combat,character,check});
            return {check,success:check.success,improved:Boolean(improved),difficulty:option.difficulty,
                character:clone(character),combat:clone(combat)};
        });
    }

    equipmentOwnedSummary() {
        const character=requireCharacter(this.state.player.character);
        const decorate=(item,kind)=>({kind,id:item?.id??null,name:item?.name??'Item',equipped:Boolean(item?.equipped||item?.worn||item?.active),worn:Boolean(item?.worn),active:Boolean(item?.active),hardPoints:equipmentCatalog_mjs_1.itemHardPoints(item),hardPointsAvailable:equipmentCatalog_mjs_1.itemAvailableHardPoints(item),qualities:clone(item?.qualities??[]),attachments:clone(item?.attachments??[]),...(kind==='weapon'?{usage:weapons_mjs_1.weaponUsageModifiers(item,character),runtimeState:clone(item?.runtimeState??{})}:{})});
        const legacyArmor=(character.gear??[]).filter(v=>Boolean(equipmentCatalog_mjs_1.armorCatalogEntry(v?.id)||equipmentCatalog_mjs_1.armorCatalogEntry(v?.name)));
        const armor=[...(character.armor??[]),...legacyArmor].map(v=>decorate(v,'armor'));
        const gear=(character.gear??[]).filter(v=>!legacyArmor.includes(v)).map(v=>decorate(v,'gear'));
        return {activeWeaponId:character.activeWeaponId??null,weapons:(character.weapons??[]).map(v=>decorate(v,'weapon')),armor,gear,tinkerer:talents_mjs_1.tinkererStatus(character),encumbrance:this.encumbranceStatus()};
    }
    equipWeapon(weaponId) {
        const character=clone(requireCharacter(this.state.player.character));
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        if(!target) throw new Error('Identificador da arma obrigatório.');
        const weapon=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        if(!weapon) throw new Error(`Arma "${weaponId}" não encontrada no inventário.`);
        assertOwnedEquipmentOperational(this.state,weapon);
        for(const w of character.weapons??[]){w.active=false;w.equipped=false;}
        weapon.active=true; weapon.equipped=true; character.activeWeaponId=weapon.id??weapon.name;
        this.commit('CHARACTER_UPDATED',{character});
        return {activeWeapon:clone(weapon),summary:this.equipmentOwnedSummary()};
    }
    equipArmor(itemId) {
        const character=clone(requireCharacter(this.state.player.character));
        const target=String(itemId??'').trim().toLocaleLowerCase('pt-BR');
        if(!target) throw new Error('Identificador da armadura obrigatório.');
        const all=[...(character.armor??[]),...(character.gear??[])];
        const armor=all.find(item=>{
            const ref=equipmentCatalog_mjs_1.armorCatalogEntry(item?.id)||equipmentCatalog_mjs_1.armorCatalogEntry(item?.name);
            return Boolean(ref)&&(String(item?.id??'').toLocaleLowerCase('pt-BR')===target||String(item?.name??'').toLocaleLowerCase('pt-BR')===target||String(ref?.id??'').toLocaleLowerCase('pt-BR')===target);
        });
        if(!armor) throw new Error(`Armadura "${itemId}" não encontrada no inventário.`);
        assertOwnedEquipmentOperational(this.state,armor);
        for(const item of all){if(equipmentCatalog_mjs_1.armorCatalogEntry(item?.id)||equipmentCatalog_mjs_1.armorCatalogEntry(item?.name)){item.equipped=false;item.worn=false;}}
        armor.equipped=true; armor.worn=true;
        const values=equipmentCatalog_mjs_1.calculateArmorValues(armor,{worn:true});
        const armorTalents=talents_mjs_1.armorMasterBonuses(character,{armorSoak:values.soak,worn:true});
        character.derived=character.derived??{};
        character.derived.soak=characterNaturalSoak(character)+values.soak+armorTalents.soakBonus;
        character.derived.defense={ranged:values.defense.ranged+armorTalents.defenseBonus,melee:values.defense.melee+armorTalents.defenseBonus};
        this.commit('CHARACTER_UPDATED',{character});
        return {armor:clone(armor),values:{...values,talentBonuses:armorTalents},summary:this.equipmentOwnedSummary()};
    }
    unequipArmor(itemId=null) {
        const character=clone(requireCharacter(this.state.player.character));
        const all=[...(character.armor??[]),...(character.gear??[])];
        let changed=false;
        for(const item of all){
            const isArmor=Boolean(equipmentCatalog_mjs_1.armorCatalogEntry(item?.id)||equipmentCatalog_mjs_1.armorCatalogEntry(item?.name));
            if(!isArmor) continue;
            const matches=itemId===null||String(item?.id??'').toLocaleLowerCase('pt-BR')===String(itemId).toLocaleLowerCase('pt-BR')||String(item?.name??'').toLocaleLowerCase('pt-BR')===String(itemId).toLocaleLowerCase('pt-BR');
            if(matches&&(item.equipped||item.worn)){item.equipped=false;item.worn=false;changed=true;}
        }
        if(itemId!==null&&!changed) throw new Error(`Armadura "${itemId}" não está equipada.`);
        character.derived=character.derived??{};
        character.derived.soak=characterNaturalSoak(character);
        character.derived.defense={ranged:0,melee:0};
        this.commit('CHARACTER_UPDATED',{character});
        return {summary:this.equipmentOwnedSummary()};
    }
    groupSummary() { const obligationTotal=obligation_mjs_1.obligationTotal(this.state.group, this.state.player.character); const dutyTotal=duty_mjs_1.dutyTotal(this.state.group, this.state.player.character); return { group: structuredClone(this.state.group), campaignMode:campaignMode(this.state), obligationTotal, dutyTotal, contributionRank:Number(this.state.group?.contributionRank??0), contributionMilestoneReady:duty_mjs_1.contributionMilestoneReady(this.state.group,this.state.player.character), progressionBlocked:modeAllowsObligation(this.state)&&obligationTotal>100 }; }
    destinySummary() { return { pool: structuredClone(this.state.group?.destiny??{light:0,dark:0}), session: structuredClone(this.state.session?.campaignRules?.destiny??null), activeSession:this.state.session?.status==='ativa' }; }
    encumbranceStatus() { return encumbrance_mjs_1.encumbranceStatus(requireCharacter(this.state.player.character)); }
    liftingDifficulty(objectEncumbrance, options = {}) { return encumbrance_mjs_1.liftingDifficulty(requireCharacter(this.state.player.character), objectEncumbrance, options); }
    statusSummary() { return statusEffects_mjs_1.statusModifiers(requireCharacter(this.state.player.character)); }
    applyStatus(payload = {}) { const character = statusEffects_mjs_1.applyStatusEffect(requireCharacter(this.state.player.character), payload); this.commit('CHARACTER_UPDATED', { character }); return character.statusEffects; }
    applyPlayerCondition(type, rounds = 1) { const character = statusEffects_mjs_1.applyStatusEffect(requireCharacter(this.state.player.character), { type, duration: 'rounds', rounds }); this.commit('CHARACTER_UPDATED', { character }); return statusEffects_mjs_1.statusModifiers(character); }
    spendDestiny(side) { const group = destiny_mjs_1.flipDestiny(this.state.group, side); this.commit('GROUP_UPDATED', { group }); return structuredClone(group.destiny); }
    resolveFearCheck(options = {}) { const result = fear_mjs_1.resolveFear(requireCharacter(this.state.player.character), options); this.commit('CHARACTER_UPDATED', { character: result.character }); return result; }
    updateGroupBase(payload = {}) { const group = fringeGroup_mjs_1.updateBase(this.state.group, payload); this.commit('GROUP_UPDATED', { group }); return group; }
    updateGroupShip(payload = {}) { const group = fringeGroup_mjs_1.updateShip(this.state.group, payload); this.commit('GROUP_UPDATED', { group }); return group; }
    setGroupDestiny(payload) { const group = fringeGroup_mjs_1.setDestinyReserve(this.state.group, payload); this.commit('GROUP_UPDATED', { group }); return group; }
    setGroupList(key, items) { const group = fringeGroup_mjs_1.setTextList(this.state.group, key, items); this.commit('GROUP_UPDATED', { group }); return group; }
    addGroupObligation(entry) { if(!modeAllowsObligation(this.state)) throw new Error('Obligation só está ativa em campanha EOTE ou MIXED.'); const group = fringeGroup_mjs_1.addObligationEntry(this.state.group, entry); this.commit('GROUP_UPDATED', { group }); return group; }
    removeGroupObligation(id) { if(!modeAllowsObligation(this.state)) throw new Error('Obligation só está ativa em campanha EOTE ou MIXED.'); const group = fringeGroup_mjs_1.removeObligationEntry(this.state.group, id); this.commit('GROUP_UPDATED', { group }); return group; }
    rebelGroupSummary() { return this.groupSummary(); }
    updateRebelBase(payload = {}) { return this.updateGroupBase(payload); }
    addGroupDuty(entry) { if(!modeAllowsDuty(this.state)) throw new Error('Duty é legado Age of Rebellion no modo EOTE; ative campanha AOR ou MIXED.'); const group=fringeGroup_mjs_1.addDutyEntry(this.state.group,entry); this.commit('GROUP_UPDATED',{group}); return group; }
    removeGroupDuty(id) { if(!modeAllowsDuty(this.state)) throw new Error('Duty é legado Age of Rebellion no modo EOTE; ative campanha AOR ou MIXED.'); const group=fringeGroup_mjs_1.removeDutyEntry(this.state.group,id); this.commit('GROUP_UPDATED',{group}); return group; }
    setGroupContributionRank(rank) { if(!modeAllowsDuty(this.state)) throw new Error('Contribution Rank é legado Age of Rebellion no modo EOTE; ative campanha AOR ou MIXED.'); const group=fringeGroup_mjs_1.setContributionRank(this.state.group,rank); this.commit('GROUP_UPDATED',{group}); return group; }
    addDutyProgress(entryId, amount) { if(!modeAllowsDuty(this.state)) throw new Error('Duty é legado Age of Rebellion no modo EOTE; ative campanha AOR ou MIXED.'); const result=duty_mjs_1.addDutyProgress(this.state.group,entryId,amount); this.commit('GROUP_UPDATED',{group:result.group}); return {...result,milestoneReady:duty_mjs_1.contributionMilestoneReady(result.group)}; }
    resolveContributionMilestone() { if(!modeAllowsDuty(this.state)) throw new Error('Contribution Rank é legado Age of Rebellion no modo EOTE; ative campanha AOR ou MIXED.'); const result=duty_mjs_1.resolveContributionMilestone(this.state.group); this.commit('GROUP_UPDATED',{group:result.group}); return result; }
    allianceRewardOptions() {
        if(!modeAllowsDuty(this.state)) return {pending:null,equipment:[],vehicles:[],strategic:true};
        const pending=this.state.group?.pendingAllianceReward??null;
        if(!pending) return {pending:null,equipment:[],vehicles:[],strategic:true};
        const maxRarity=Number(pending.maxRarity??0),catalog=economy_mjs_1.genericEconomyCatalog();
        const compact=(kind,x)=>({kind,id:x.id,name:x.name,rarity:Number(x.rarity),restricted:Boolean(x.restricted)});
        const equipment=[...catalog.weapons.map(x=>compact('weapon',x)),...catalog.armor.map(x=>compact('armor',x)),...catalog.gear.map(x=>compact('gear',x))].filter(x=>x.rarity<=maxRarity);
        const vehicles=catalog.vehicles.map(x=>compact('vehicle',x)).filter(x=>x.rarity<=maxRarity);
        return {pending:clone(pending),maxRarity,equipment,vehicles,strategic:true};
    }
    claimAllianceReward({kind,itemId=null,name='',note=''}={}) {
        if(!modeAllowsDuty(this.state)) throw new Error('Recompensas da Aliança exigem campanha AOR ou MIXED.');
        const pending=this.state.group?.pendingAllianceReward;
        if(!pending||pending.status!=='pending') throw new Error('Não há recompensa da Aliança pendente.');
        const rewardKind=String(kind??'');
        if(!['equipment','vehicle','strategic'].includes(rewardKind)) throw new Error('Tipo de recompensa da Aliança inválido.');
        let next=clone(this.state), asset=null;
        if(rewardKind==='strategic'){
            const label=String(name??'').trim(); if(!label) throw new Error('Ativo estratégico exige um nome/descrição.');
            next.group=clone(next.group); next.group.resources=Array.isArray(next.group.resources)?next.group.resources:[];
            const resource={id:`strategic_cr${pending.contributionRank}_${Date.now()}`,name:label,note:String(note??''),source:'AoR Alliance Reward',contributionRank:Number(pending.contributionRank)};
            next.group.resources.push(resource); asset=resource;
        } else {
            const economyKind=rewardKind==='vehicle'?'vehicle':String(kind==='equipment'?'':kind);
            let resolvedKind=economyKind;
            if(rewardKind==='equipment'){
                const candidates=['weapon','armor','gear'];
                resolvedKind=candidates.find(k=>{try{return Boolean(economy_mjs_1.catalogAsset(k,itemId));}catch{return false;}});
                if(!resolvedKind) throw new Error('Equipamento não encontrado no catálogo mecânico auditado.');
            }
            const item=economy_mjs_1.catalogAsset(resolvedKind,itemId);
            if(!item) throw new Error('Recompensa não encontrada no catálogo mecânico auditado.');
            if(!Number.isInteger(Number(item.rarity))) throw new Error('Recompensa sem raridade mecânica comprovada.');
            if(Number(item.rarity)>Number(pending.maxRarity)) throw new Error(`Raridade ${item.rarity} excede o limite ${pending.maxRarity} do Contribution Rank atual.`);
            const granted=economy_mjs_1.grantCatalogAsset(next,{kind:resolvedKind,itemId:item.id,method:'alliance_reward',note:String(note??''),sourceRuleIds:['AOR_CORE_ALLIANCE_REWARD']});
            next=granted.state; asset=granted.asset;
        }
        next.group=clone(next.group); next.group.allianceRewardHistory=[...(next.group.allianceRewardHistory??[]),{...clone(pending),status:'claimed',kind:rewardKind,assetId:asset?.assetId??asset?.id??null,name:asset?.name??String(name??''),claimedAt:new Date().toISOString()}];
        delete next.group.pendingAllianceReward;
        this.state=next; if (!this._atomicActionDepth) this.repository.save(this.state);
        return {reward:clone(next.group.allianceRewardHistory.at(-1)),asset:clone(asset),group:clone(next.group)};
    }
    worldSummary() { return world_mjs_1.masterWorldSummary(this.state.world); }
    causalEncounterCandidates(locationId = this.state.scene?.locationId) { return world_mjs_1.causalEncounterCandidates(this.state.world, locationId, { limit: 12 }); }
    activeWorldAftermaths({ limit = 50, knownOnly = false } = {}) { return world_mjs_1.activeWorldAftermaths(this.state.world, { limit, knownOnly }); }
    closeWorldAftermath(aftermathId, { resolution = '' } = {}) { const world = world_mjs_1.closeWorldAftermath(this.state.world, aftermathId, { resolution }); this.commit('WORLD_UPDATED', { world }); return world.aftermaths.find(a => a.id === aftermathId) ?? null; }
    addWorldFaction(payload = {}) { const faction = world_mjs_1.createFaction(payload); const world = world_mjs_1.upsertFaction(this.state.world, faction); this.commit('WORLD_UPDATED', { world }); return faction; }
    setWorldFactionObjective(factionId, objective) { const world = world_mjs_1.setFactionObjective(this.state.world, factionId, objective); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    setWorldFactionStatus(factionId, status) { const world = world_mjs_1.setFactionStatus(this.state.world, factionId, status); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    addWorldFactionResource(factionId, resource) { const world = world_mjs_1.addFactionResource(this.state.world, factionId, resource); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    removeWorldFactionResource(factionId, resource) { const world = world_mjs_1.removeFactionResource(this.state.world, factionId, resource); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    setWorldFactionRelation(factionId, targetFactionId, relation) { const world = world_mjs_1.setFactionRelation(this.state.world, factionId, targetFactionId, relation); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    addWorldFactionFact(factionId, payload = {}) { const world = world_mjs_1.recordFactionFact(this.state.world, factionId, payload); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId]; }
    addWorldFactionPlan(factionId, payload = {}) { const result = world_mjs_1.createFactionPlan(this.state.world, factionId, payload); this.commit('WORLD_UPDATED', { world: result.world }); return result.plan; }
    setWorldFactionPlanStatus(factionId, planId, status) { const world = world_mjs_1.setFactionPlanStatus(this.state.world, factionId, planId, status); this.commit('WORLD_UPDATED', { world }); return world.factions[factionId].plans.find(p => p.id === planId); }
    advanceWorldFactionPlan(factionId, planId) { const result = world_mjs_1.advanceFactionPlan(this.state.world, factionId, planId); this.commit('WORLD_UPDATED', { world: result.world }); return result; }
    addWorldLocation(payload = {}) {
        const location = world_mjs_1.createLocation(payload);
        const world = world_mjs_1.upsertLocation(this.state.world, location);
        this.commit('WORLD_UPDATED', { world });
        return location;
    }
    addWorldNpc(payload = {}) {
        const npc = world_mjs_1.createNpc(payload);
        const world = world_mjs_1.upsertNpc(this.state.world, npc);
        this.commit('WORLD_UPDATED', { world });
        return npc;
    }
    moveWorldNpc(npcId, locationId) { const world = world_mjs_1.moveNpc(this.state.world, npcId, locationId); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    setWorldNpcFaction(npcId, factionId = null) { const world = world_mjs_1.setNpcFaction(this.state.world, npcId, factionId); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    setWorldNpcGoal(npcId, goal) { const world = world_mjs_1.setNpcGoal(this.state.world, npcId, goal); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    setWorldNpcRelation(npcId, relation) { const world = world_mjs_1.setNpcRelation(this.state.world, npcId, relation); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    setWorldNpcStatus(npcId, status) { const world = world_mjs_1.setNpcStatus(this.state.world, npcId, status); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    addWorldNpcFact(npcId, payload = {}) { const world = world_mjs_1.recordNpcFact(this.state.world, npcId, payload); this.commit('WORLD_UPDATED', { world }); return world.npcs[npcId]; }
    scheduleWorldEvent(payload = {}) { const result = world_mjs_1.scheduleWorldEvent(this.state.world, payload); if (result.created)
        this.commit('WORLD_UPDATED', { world: result.world }); return result; }
    cancelWorldEvent(eventId) { const world = world_mjs_1.cancelWorldEvent(this.state.world, eventId); this.commit('WORLD_UPDATED', { world }); return world; }
    resolveWorldHook(hookId, payload = {}) { const world = world_mjs_1.resolveWorldHook(this.state.world, hookId, payload); this.commit('WORLD_UPDATED', { world }); return world.hooks.find(h => h.id === hookId); }
    advanceWorldTime({ ticks = 1, maxEvents = 50 } = {}) { const result = sandbox_mjs_1.advanceLivingWorld(this.state.world, { ticks, maxEvents }); this.commit('WORLD_UPDATED', { world: result.world }); return result; }
    initializeLivingSandbox({ startingPoint = 'Porto de Fronteira', regionLabel = 'Região de Fronteira', startMapPoint = null } = {}) {
        if (startMapPoint !== null && (!Number.isFinite(startMapPoint.x) || !Number.isFinite(startMapPoint.y) || startMapPoint.x < 0 || startMapPoint.x > 100 || startMapPoint.y < 0 || startMapPoint.y > 100)) throw new Error('Ponto do mapa inválido.');
        const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });
        if (result.created) {
            result.world.sandbox.startLocationId = result.startLocationId;
            if (startMapPoint !== null) result.world.sandbox.startMapPoint = { x: startMapPoint.x, y: startMapPoint.y, mapId: 'local.map.galaxy', source: 'player_selected' };
        }
        if (result.created) {
            this.commit('WORLD_UPDATED', { world: result.world });
            const opening = sandbox_mjs_1.sandboxOpening(result.world, { locationId: result.startLocationId, limit: 6 });
            this.commit('SCENE_UPDATED', { scene: { locationId: result.startLocationId, title: String(startingPoint || 'Ponto inicial'), narration: '', visibleNpcIds: [] } });
        }
        return { ...result, opening: sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: result.startLocationId }), summary: sandbox_mjs_1.sandboxSummary(this.state.world) };
    }
    livingSandboxStatus() { return sandbox_mjs_1.sandboxSummary(this.state.world); }
    sandboxOpening({ locationId = null, limit = 6 } = {}) { return sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: locationId ?? this.state.scene?.locationId ?? null, limit }); }
    startSandboxSession({ rng = Math.random } = {}) {
        // R54_BARBARA_OPENING_MASTER: bootstrap sandbox/session only; Barbara owns first narration.
        if (!this.state.player?.character)
            throw new Error('Crie, gere ou importe um personagem antes de iniciar o sandbox.');
        const opening = sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: this.state.scene?.locationId ?? this.state.world?.sandbox?.startLocationId ?? null, limit: 6 });
        if (!opening?.ready || !opening.location)
            throw new Error('Crie o mundo vivo e escolha o ponto inicial antes de iniciar o sandbox.');
        let session = this.state.session;
        if (session?.status !== 'ativa') {
            const started = this.startSession({ playerCount: 1, rng });
            session = started.session;
        }
        this.commit('SCENE_UPDATED', { scene: { visibleNpcIds: [] } });
        const previousOpening = this.state.master?.openingRequest ?? null;
        const pick = (items, { knownFirst = false, previousId = null } = {}) => {
            let pool = (items ?? []).filter(Boolean);
            if (knownFirst) {
                const known = pool.filter(item => item?.knownToPlayer);
                if (known.length) pool = known;
            }
            if (pool.length > 1 && previousId) {
                const alternatives = pool.filter(item => item?.id !== previousId);
                if (alternatives.length) pool = alternatives;
            }
            if (!pool.length) return null;
            const roll = Number(rng());
            const unit = Number.isFinite(roll) ? Math.abs(roll % 1) : 0;
            return pool[Math.min(pool.length - 1, Math.floor(unit * pool.length))] ?? pool[0];
        };
        const knownOpportunity = pick(opening.opportunities, { knownFirst: true, previousId: previousOpening?.opportunityId ?? null });
        const knownNpc = pick(opening.npcs, { knownFirst: true, previousId: previousOpening?.npcId ?? null });
        const knownFaction = pick(opening.factions, { knownFirst: true, previousId: previousOpening?.factionId ?? null });
        const openingAngles = ['movimento imediato no local', 'mudança ambiental que exige atenção', 'interação social já em andamento', 'oportunidade percebida durante a chegada', 'contratempo cotidiano com consequências', 'detalhe sensorial que evolui para uma escolha'];
        let anglePool = openingAngles;
        if (openingAngles.length > 1 && previousOpening?.openingAngle) anglePool = openingAngles.filter(value => value !== previousOpening.openingAngle);
        const angleRoll = Number(rng());
        const angleUnit = Number.isFinite(angleRoll) ? Math.abs(angleRoll % 1) : 0;
        const openingAngle = anglePool[Math.min(anglePool.length - 1, Math.floor(angleUnit * anglePool.length))] ?? openingAngles[0];
        const locationName = String(opening.location.name ?? this.state.scene?.title ?? 'Ponto inicial');
        const suggestions = [];
        if (knownOpportunity?.title) suggestions.push(`Investigar: ${knownOpportunity.title}`);
        if (knownNpc?.name) suggestions.push(`Conversar com ${knownNpc.name}`);
        suggestions.push('Observar o local antes de agir');
        if (opening.market) suggestions.push('Consultar o mercado local');
        suggestions.push('Procurar rumores e oportunidades');
        suggestions.push('Ignorar a situação e seguir adiante');
        this.commit('MASTER_OPENING_PREPARED', { suggestedActions: suggestions.slice(0, 6), request: { locationName, opportunity: knownOpportunity?.title ?? null, opportunityId: knownOpportunity?.id ?? null, npc: knownNpc?.name ?? null, npcId: knownNpc?.id ?? null, faction: knownFaction?.name ?? null, factionId: knownFaction?.id ?? null, openingAngle } });
        return {
            opening,
            proposal: null,
            awaitingMaster: true,
            masterOpeningRequest: {
                kind: 'sandbox_opening',
                locationName,
                sceneLocationId: opening.location.id,
                opportunity: knownOpportunity ? { id: knownOpportunity.id ?? null, title: knownOpportunity.title ?? '', summary: knownOpportunity.summary ?? '' } : null,
                npc: knownNpc ? { id: knownNpc.id ?? null, name: knownNpc.name ?? '', species: knownNpc.species ?? '', sex: knownNpc.sex ?? '', gender: knownNpc.gender ?? '', origin: knownNpc.origin ?? '', career: knownNpc.career ?? '', specialization: knownNpc.specialization ?? '', occupation: knownNpc.occupation ?? '', ageYears: knownNpc.ageYears ?? null, appearance: knownNpc.appearance ?? '' } : null,
                faction: knownFaction ? { id: knownFaction.id ?? null, name: knownFaction.name ?? '' } : null,
                openingAngle,
                suggestedActions: suggestions.slice(0, 6)
            },
            session: structuredClone(session),
            state: structuredClone(this.state)
        };
    }
    /**
     * P145: retry-safe published adventure action entry point. All writes are
     * atomic with the source adapter, UAE journal, world, combat and rewards.
     * The actionId must be generated once by the caller and reused on retry.
     */
    executePublishedAdventureCommand({ actionId, kind, adventureId = null, choiceId = null,
        checkIndex = 0, encounterId = null, minutes = 5, options = {} } = {}) {
        const resolvedId = adventureId || this.state.world?.publishedAdventure?.id || 'under-black-sun';
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(resolvedId);
        if (!options || typeof options !== 'object' || Array.isArray(options))
            throw new Error('Opções de ação publicada inválidas.');
        const { rng, actionId: ignored, ...persistedOptions } = options;
        const args = kind === 'choice' ? { choiceId: String(choiceId ?? ''),
                ...(choiceId==='ship-start-interception'?{unawareCharacterIds:[...(persistedOptions.unawareCharacterIds??[])].sort()}: {}) } :
            kind === 'check' ? { checkIndex: Number(checkIndex),
                useLightDestiny: Boolean(persistedOptions.useLightDestiny),
                useDarkDestiny: Boolean(persistedOptions.useDarkDestiny) } :
            kind === 'encounter' ? { encounterId: encounterId === null ? null : String(encounterId),
                playerQuickReactionStrain: persistedOptions.playerQuickReactionStrain ?? 0,
                ...(encounterId==='teemo-palace-guards'?{palaceAdditionalHenchmenCount:persistedOptions.palaceAdditionalHenchmenCount??null,
                  gmHuttProfileConfirmed:persistedOptions.gmHuttProfileConfirmed===true}:{}) } :
            kind === 'finish-combat' ? { strainSkill: persistedOptions.strainSkill ?? 'frialdad',
                useBalance: Boolean(persistedOptions.useBalance) } :
            kind === 'imperial-clock' ? { minutes: Number(minutes) } : {};
        const request = publishedCommandRequest(kind, adapter.id, args);
        const previous = findPublishedActionReceipt(this.state.world, actionId, request);
        if (previous) {
            // A historical receipt may survive a deliberate adventure restart.
            // It must never replay against a different UAE timeline.
            const runtime = this.state.world?.publishedAdventureRuntime;
            const causallyPresent = runtime?.adventureId === adapter.id &&
                runtime.canonicalEvents?.some(event => event.type === 'PUBLISHED_COMMAND_COMMITTED' &&
                    event.source === 'controller' && event.idempotencyKey === actionId &&
                    event.payload?.actionId === actionId && event.payload?.kind === kind);
            if (!causallyPresent)
                throw new Error('Recibo publicado pertence a outra execução da aventura ou perdeu sua prova causal.');
            return { ...structuredClone(previous.response), applied: false, duplicate: true,
                receiptId: previous.id, state: structuredClone(this.state) };
        }
        return this.atomicAction(() => {
            let result;
            if (kind === 'start') result = this.startPublishedAdventureSession(adapter.id, { rng });
            else if (kind === 'choice') result = this.advancePublishedAdventure(choiceId, { adventureId: adapter.id,
                ...(choiceId==='ship-start-interception'?{unawareCharacterIds:persistedOptions.unawareCharacterIds??[]}: {}) });
            else if (kind === 'check') result = this.resolvePublishedAdventureCheck(checkIndex,
                { adventureId: adapter.id, ...persistedOptions, rng });
            else if (kind === 'encounter') result = this.beginPublishedAdventureEncounter(encounterId,
                { adventureId: adapter.id, ...persistedOptions, rng });
            else if (kind === 'imperial-clock') {
                if(adapter.id!=='beyond-the-rim')throw Error('Relógio imperial pertence apenas a Além da Orla.');
                const tick=this.advancePublishedImperialClock({minutes:Number(minutes),actionId});
                // Do not embed a complete world snapshot inside the durable
                // published-command receipt. The world is saved separately.
                result={clock:tick.clock,eventsAdded:tick.eventsAdded};
            }
            else if (kind === 'finish-combat') {
                if (this.state.combat?.publishedAdventureContext?.adventureId !== adapter.id)
                    throw new Error('Recibo de combate não corresponde a um encontro publicado.');
                result = this.finishCombat({ ...persistedOptions, rng });
            } else throw new Error('Tipo de comando publicado não suportado.');
            // Do not persist a second copy of the entire campaign inside the
            // receipt. Keep only the result required to replay the UI response.
            const response = JSON.parse(JSON.stringify(Object.fromEntries(
                Object.entries(result ?? {}).filter(([key]) => key !== 'state'))));
            // Mirror the durable controller receipt in the UAE causal journal.
            // This is a single repository transaction, not a second save.
            const module = publishedAdventureRuntimeModule(adapter.meta);
            const runtime = AdventureRuntime.restore(module, this.state.world.publishedAdventureRuntime);
            const receiptSequence = (this.state.world.publishedAdventureActionReceipts ?? []).length + 1;
            const runKey = currentPublishedAdventureRunKey(this.state.world);
            runtime.recordPublishedCommandProof({ actionId, kind,
                adventureId: adapter.id, proofVersion: 2,
                request, receiptSequence, runKey });
            const worldWithJournal = { ...structuredClone(this.state.world),
                publishedAdventureRuntime: runtime.getState() };
            validatePublishedAdventureRuntimeState(module, worldWithJournal.publishedAdventureRuntime,
                worldWithJournal.publishedAdventure);
            const recorded = appendPublishedActionReceipt(worldWithJournal,
                { actionId, kind, adventureId: adapter.id, request, response });
            this.commit('WORLD_UPDATED', { world: recorded.world });
            return { ...result, applied: true, duplicate: false,
                receiptId: recorded.receipt.id, state: structuredClone(this.state) };
        });
    }
    publishedAdventureCatalog() {
        return publishedAdventureRegistry_mjs_1.publishedAdventureCatalog();
    }
    publishedAdventureStatus(adventureId = 'under-black-sun') {
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(adventureId);
        return adapter.status(this.state.world);
    }
    certifiedPublishedAdventureCatalog() {
        return publishedAdventureRegistry_mjs_1.certifiedPublishedAdventureCatalog();
    }
    listMissionOpportunities(filters = {}) {
        return missionOpportunityEngine.listMissionOpportunities(this.state.world, filters);
    }
    generateMissionOpportunities(options = {}) {
        return this.atomicAction(() => {
            const res = missionOpportunityEngine.generateMissionOpportunities(this.state.world, options);
            this.commit('WORLD_UPDATED', { world: res.world });
            return { generated: res.generated, missions: missionOpportunityEngine.listMissionOpportunities(this.state.world), state: structuredClone(this.state) };
        });
    }
    acceptMissionOpportunity(missionId) {
        return this.atomicAction(() => {
            const res = missionOpportunityEngine.acceptMissionOpportunity(this.state.world, missionId);
            this.commit('WORLD_UPDATED', { world: res.world });
            return { mission: res.mission, state: structuredClone(this.state) };
        });
    }
    resolveMissionOpportunity(missionId, options = {}) {
        return this.atomicAction(() => {
            const res = missionOpportunityEngine.resolveMissionOpportunity(this.state.world, missionId, options);
            this.commit('WORLD_UPDATED', { world: res.world });
            return { mission: res.mission, success: res.success, state: structuredClone(this.state) };
        });
    }
    rejectMissionOpportunity(missionId, options = {}) {
        return this.atomicAction(() => {
            const res = missionOpportunityEngine.rejectMissionOpportunity(this.state.world, missionId, options);
            this.commit('WORLD_UPDATED', { world: res.world });
            return { mission: res.mission, state: structuredClone(this.state) };
        });
    }
    createOriginalAdventure(options = {}) {
        return originalAdventureCreator.createOriginalAdventure(this.state.world, options);
    }
    registerOriginalAdventure(module) {
        return this.atomicAction(() => {
            const nextWorld = originalAdventureCreator.registerOriginalAdventureInWorld(this.state.world, module);
            this.commit('WORLD_UPDATED', { world: nextWorld });
            return { module, state: structuredClone(this.state) };
        });
    }
    /** P184: source-adjudicated Jewel Grand Prix / auction operations for Android WebView. */
    adjudicateJewelOperation(kind, details = {}) {
        if(kind==='race_roll')return this.resolveJewelGrandPrixLeg(details);
        if(kind==='bank_step')return this.resolveJewelBankStep(details);
        if(kind==='bank_settlement')return this.settleJewelBank(details);
        if(kind==='escape_bomb')return this.resolveJewelEscapeBomb(details);
        if(kind==='bank_replace_tools')return this.replaceJewelSlicerTools(details);
        if(kind==='bank_buy_bolt')return this.buyJewelRestrainingBolt(details);
        if(kind==='bank_evidence')return this.resolveJewelBankEvidence(details);
        if(kind==='bank_droid')return this.resolveJewelLiveDroid(details);
        if(kind==='bank_spend')return this.spendJewelBankSymbols(details);
        if(kind==='bank_prepare')return this.prepareJewelBank(details);
        if(kind==='bank_hire_slicer')return this.hireJewelBankSlicer(details);
        return this.atomicAction(() => {
            if (this.state.combat?.active) throw new Error('Encerre o combate antes de adjudicar a operação.');
            const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            if (this.state.world?.publishedAdventure?.id !== adapter.id) throw new Error('A Joia de Yavin não está ativa.');
            const actions = {
                race_leg: adapter.adjudicateRaceLeg,
                race_placement: adapter.adjudicateRacePlacement,
                bidder_influence: adapter.influenceBidder,
                auction_round: adapter.adjudicateAuctionRound
            };
            if (!Object.prototype.hasOwnProperty.call(actions,kind)) throw new Error('Operação da aventura desconhecida.');
            const result=actions[kind](this.state.world,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P187: source-backed G-100 capture, disable, slice and black-box cleanup. */
    resolveJewelBankStep(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw new Error('Encerre o combate antes da operação bancária.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const p=this.state.world?.publishedAdventure;
            if(p?.id!==adapter.id||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
                throw new Error('A operação do Banco Central não está ativa.');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.performBankStep(this.state.world,character,details);
            if(result.character)this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,bank:result.bank,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P194: black-box custody and source-backed disposal of the G-100 parts. */
    /** P200: in-world bank account ledger; no credits added to the PC wallet. */
    settleJewelBank(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes da liquidação bancária.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const result=adapter.settleBank(this.state.world,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,settlement:result.settlement,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P201: SWE09 source-backed airspeeder betrayal, no invented countdown ticks. */
    resolveJewelEscapeBomb(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de resolver a bomba.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.escapeBomb(this.state.world,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,escapeBomb:result.escapeBomb,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P203: all four official fall bands, including forced critical injuries. */
    resolveJewelEscapeImpact(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de aplicar o impacto.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.escapeImpact(this.state.world,character,details);
            this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,impact:result.impact,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P204: independently adjudicate a confirmed PRESENT companion; never infer position. */
    resolveJewelCompanionImpact(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de aplicar impacto a companheiros.');
            const group=clone(this.state.group),member=(group.companions??[]).find(x=>x.id===details.companionId);
            if(!member)throw Error('Companheiro não pertence ao grupo.');
            const result=performJewelCompanionImpact(this.state.world,member,details);
            member.character=result.character;
            this.commit('GROUP_UPDATED',{group});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,companionImpact:result.companionImpact,
                state:structuredClone(this.state)};
        });
    }
    /** P205: SWE09 p.90, actual NPC initiative slot, personal-scale aft blaster. */
    queueJewelArrow7Turret(details={}) {
        return this.atomicAction(()=>{
            const result=queueJewelArrow7Turret(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            this.commit('COMBAT_ACTION',{combat:result.combat});
            return {adjudication:result.adjudication,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P206: competitive planetary-scale chase at round start; NPC pilot sheet confirmed by GM. */
    resolveJewelArrow7Chase(details={}) {
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            const result=resolveJewelArrow7Chase(this.state.world,this.state.combat,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            this.commit('COMBAT_ACTION',{combat:result.combat});
            return {adjudication:result.adjudication,chase:result.chase,state:clone(this.state)};
        });
    }
    /** P206: final contact or escape is a separate GM decision, never automatic. */
    confirmJewelArrow7ChaseOutcome(details={}) {
        return this.atomicAction(()=>{
            const result=confirmJewelArrow7ChaseOutcome(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            this.commit('COMBAT_ACTION',{combat:result.combat});
            return {adjudication:result.adjudication,chase:result.chase,state:clone(this.state)};
        });
    }
    /** P207: SWE09 pp.90–91, real Computers/Streetwise mainframe search. */
    investigateJewelArendHoldings(details={}) {
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            const result=investigateJewelArendHoldings(this.state.world,this.state.combat,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,investigation:result.investigation,state:clone(this.state)};
        });
    }
    /** P209: SWE09 p.94 real initial Wing Guard force: 2x2 minions + sergeant.
     * Start actual initiative AND register the physical Holo-Park arrival in
     * one transaction. Never fabricate Grayson/Arend profiles or teleport NPCs.
     */
    beginJewelHoloParkWingGuard(details={}) {
        return this.atomicAction(()=>{
            if(details.gmParkArrivalConfirmed!==true||details.gmPursuersConfirmed!==true)
                throw Error('Mestre deve confirmar chegada física e presença da Guarda Alada.');
            if(this.state.combat?.active)throw Error('Encerre o combate anterior antes do Holo-Park.');
            const p=this.state.world?.publishedAdventure;
            if(p?.id!=='jewel-of-yavin'||p.currentNodeId!=='cloud-city-pursuit'||
               p.operations?.holoPark||!p.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome))
                throw Error('Encontro Holo-Park exige a fuga anterior resolvida, sem duplicar combate.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const spec=adapter.encounterSpec(this.state.world,'holo-park-wing-guard');
            if(spec.adversaries.length!==3||spec.adversaries[0].count!==2||spec.adversaries[1].count!==2||
               spec.adversaries[2].profileKey!=='bespin_wing_guard_sergeant')
                throw Error('Formação publicada da Guarda Alada não conferida.');
            const started=this.beginPublishedAdventureEncounter('holo-park-wing-guard',
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random,gmParkArrivalConfirmed:true});
            if(!this.state.combat?.active)throw Error('Encontro terminou antes da chegada ao Holo-Park.');
            const entered=enterJewelHoloPark(this.state.world,this.state.combat,{
                actionId:details.actionId,gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,
                factions:['wing_guard'],rng:details.rng??Math.random,gm47Choice:details.gm47Choice??null});
            this.commit('WORLD_UPDATED',{world:entered.world});
            return {encounter:spec,combat:clone(this.state.combat),adjudication:entered.adjudication,
                holoPark:entered.holoPark,state:clone(this.state)};
        });
    }
    /** P217: only GM-confirmed, distinct Arend/Grayson pursuers. Real combat,
     * never simultaneous with the initial Wing Guard encounter or teleportation. */
    beginJewelHoloParkFaction(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Encerre o combate ativo antes de iniciar outra facção.');
            const prepared=prepareJewelHoloFaction(this.state.world,details);
            this.commit('WORLD_UPDATED',{world:prepared.world});
            const started=this.beginPublishedAdventureEncounter(prepared.encounterId,
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random});
            if(!this.state.combat?.active||this.state.combat.publishedAdventureContext?.encounterId!==prepared.encounterId)
                throw Error('Encontro da facção não foi iniciado corretamente.');
            // Preserve the source Holo-Park clock when a NEW combat resets its
            // local round counter. Never advance time for unrecorded rounds.
            const holo=this.state.world.publishedAdventure.operations.holoPark;
            const offset=holo.started.combatRound+holo.rounds.length-this.state.combat.round;
            if(!Number.isSafeInteger(offset)||offset<0)throw Error('Relógio do Holo-Park incompatível.');
            const synced=clone(this.state.combat);synced.holoParkRoundOffset=offset;
            this.commit('COMBAT_ACTION',{combat:synced});
            return {adjudication:prepared.adjudication,encounter:started,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P208: published Holo-Park landscape changes on real combat rounds. */
    enterJewelHoloPark(details={}) {
        return this.atomicAction(()=>{
            const result=enterJewelHoloPark(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,holoPark:result.holoPark,state:clone(this.state)};
        });
    }
    recordJewelHoloParkRound(details={}) {
        return this.atomicAction(()=>{
            const result=recordJewelHoloParkRound(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,holoPark:result.holoPark,state:clone(this.state)};
        });
    }
    confirmJewelHoloParkOutcome(details={}) {
        return this.atomicAction(()=>{
            const result=confirmJewelHoloParkOutcome(this.state.world,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,holoPark:result.holoPark,state:clone(this.state)};
        });
    }
    /** P232: real-world task ledger drives two-minute narrative landscape changes. */
    enterJewelHoloNarrativeClock(details={}){
        return this.atomicAction(()=>{
            const r=enterJewelHoloNarrativeClock(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:r.world});
            return {...r,state:clone(this.state)};
        });
    }
    advanceJewelHoloNarrativeClock(details={}){
        return this.atomicAction(()=>{
            const r=advanceJewelHoloNarrativeClock(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:r.world});
            return {...r,state:clone(this.state)};
        });
    }
    endJewelHoloNarrativeClock(details={}){
        return this.atomicAction(()=>{
            const r=endJewelHoloNarrativeClock(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:r.world});
            return {...r,state:clone(this.state)};
        });
    }
    /** P210: spend ACTUAL narrative symbols for published Holo-Park effects. */
    applyJewelHoloParkEffect(details={}) {
        return this.atomicAction(()=>{
            let combat=this.state.combat,character=requireCharacter(this.state.player.character);
            if(details.effect==='alderaan_nerfs'){
                assertCanAct(character);assertPlayerInitiativeTurn(combat);
                if(statusEffects_mjs_1.statusModifiers(character).cannotManeuver)
                    throw Error('Personagem imobilizado não pode ocultar-se entre nerfs.');
                const maneuver=spendPlayerManeuver(character,combat,{method:details.maneuverMethod??'normal',
                    label:'Holo-Park Alderaan: ocultar-se entre nerfs'});
                combat=maneuver.combat;character=maneuver.character;
                details={...details,maneuverSpent:true};
            }
            const result=applyJewelHoloEffect(this.state.world,combat,character,details,this.state.group);
            this.commit('WORLD_UPDATED',{world:result.world});
            this.commit('COMBAT_ACTION',{combat:result.combat,character:result.character});
            return {adjudication:result.adjudication,effects:result.effects,state:clone(this.state)};
        });
    }
    /** P215: NPC fountain cover ends only after a GM-confirmed real move. */
    confirmJewelHoloNpcFountainMovement(details={}){
        return this.atomicAction(()=>{
            const combat=confirmJewelHoloNpcMovement(this.state.combat,details);
            this.commit('COMBAT_UPDATED',{combat});
            return {combat:clone(this.state.combat)};
        });
    }
    /** P215: a hidden NPC is targetable only after physical visibility is confirmed. */
    confirmJewelHoloNpcVisibility(details={}){
        return this.atomicAction(()=>{
            const combat=confirmJewelHoloNpcVisible(this.state.combat,details);
            this.commit('COMBAT_UPDATED',{combat});
            return {combat:clone(this.state.combat)};
        });
    }
    /** P215: no reserved NPC Despair is silently discarded. */
    settleJewelHoloNpcDespair(details={}){
        return this.atomicAction(()=>{
            const out=settleJewelHoloUnusedNpcDespair(this.state.combat,
                this.state.world?.publishedAdventure,details);
            this.commit('COMBAT_UPDATED',{combat:out.combat});
            return {adjudication:out.applied,state:clone(this.state)};
        });
    }
    /** P214: recovery of a real weapon lost to an NPC Threat in the Holo-Park. */
    recoverJewelHoloWeapon(details={}){
        return this.atomicAction(()=>{
            let combat=this.state.combat,character=requireCharacter(this.state.player.character);
            assertCanAct(character);
            if(details.method==='maneuver'){
                const moved=spendPlayerManeuver(character,combat,{method:details.maneuverMethod??'normal',
                    label:'Holo-Park: recuperar arma caída'});
                combat=moved.combat;character=moved.character;
            }
            const out=recoverJewelHoloDroppedWeapon(combat,character,{...details,
                maneuverSpent:details.method==='maneuver'});
            this.commit('COMBAT_ACTION',{combat:out.combat,character:out.character});
            return {recovery:out.recovery,state:clone(this.state)};
        });
    }
    /** P214: settle a real reserved NPC Threat through the original strain fallback. */
    settleJewelHoloNpcThreat(details={}){
        return this.atomicAction(()=>{
            const out=settleJewelHoloUnusedNpcThreat(this.state.combat,
                this.state.world?.publishedAdventure,details);
            this.commit('COMBAT_UPDATED',{combat:out.combat});
            return {adjudication:out.applied,state:clone(this.state)};
        });
    }
    /** P213: GM-confirmed companion movement spends that companion's maneuver. */
    confirmJewelHoloCompanionMovement({companionId,gmMovementConfirmed=false}={}){
        return this.atomicAction(()=>{
            if(!this.state.group?.companions?.some(x=>x.id===companionId))
                throw Error('Companheiro não pertence ao grupo atual.');
            const combat=clearHoloParkTransportCompanion(this.state.combat,companionId,gmMovementConfirmed);
            this.commit('COMBAT_UPDATED',{combat});
            return {companionId,combat:clone(this.state.combat)};
        });
    }
    /** P221: 15-minute outdoor patrols and Venture's ten-minute alert use only
     * task-receipt-backed elapsed minutes. Rolls use the real EotE dice engine. */
    recordJewelArendPatrol(details={}){
        return this.atomicAction(()=>{
            const result=recordJewelArendPatrol(this.state.world,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,status:result.status,state:clone(this.state)};
        });
    }
    resolveJewelResidencePatrolCheck(details={}){
        return this.atomicAction(()=>{
            const status=jewelArendPatrolStatus(this.state.world);
            if(!status?.residence?.due)throw Error('Ainda não se passaram quinze minutos reais de exposição.');
            if(!['engano','sigilo','callejeo'].includes(details.skillKey))throw Error('Escolha Engano, Furtividade ou Submundo (Streetwise).');
            if(this.state.combat?.active)throw Error('Resolva o combate atual antes de tentar evitar patrulha na rua.');
            const check=this.resolveSkillCheck({skillKey:details.skillKey,difficulty:3,rng:details.rng??Math.random});
            const result=recordJewelArendPatrol(this.state.world,{actionId:details.actionId,type:'RESIDENCE_CHECK',
                skillKey:details.skillKey,check:{faces:clone(check.faces),pool:clone(check.pool),success:check.success,
                netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair}});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {check,adjudication:result.adjudication,status:result.status,state:clone(this.state)};
        });
    }
    /** P226: actual Hard check on each physical change of location, no NPC spawn. */
    resolveJewelPursuitTravelCheck(details={}){
        return this.atomicAction(()=>{
            if(!['engano','sigilo','callejeo'].includes(details.skillKey))throw Error('Perícia não publicada.');
            if(details.gmNewLocationConfirmed!==true)throw Error('Mestre precisa confirmar chegada ao novo local.');
            const check=this.resolveSkillCheck({skillKey:details.skillKey,difficulty:3,rng:details.rng??Math.random});
            const out=recordJewelPursuitTravelCheck(this.state.world,this.state.combat,this.state.player.character,{...details,
                check:{pool:clone(check.pool),faces:clone(check.faces),success:check.success,netSuccess:check.netSuccess,
                    netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair}});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {check,adjudication:out.adjudication,status:out.status,state:clone(this.state)};
        });
    }
    /** P226: only physically confirmed distinct factions enter real combat. */
    beginJewelPursuitGroup(details={}){
        return this.atomicAction(()=>{
            const prepared=prepareJewelPursuitGroup(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:prepared.world});
            const started=this.beginPublishedAdventureEncounter(prepared.encounterId,
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random});
            if(!this.state.combat?.active||this.state.combat.publishedAdventureContext?.encounterId!==prepared.encounterId)
                throw Error('Grupo perseguindo não iniciou iniciativa real.');
            return {adjudication:prepared.adjudication,encounter:started.encounter,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P229: SWE09 p.94 — actual Hard Computers access to a third-party ship. */
    attemptJewelStarportShipAccess(details={}){
        return this.atomicAction(()=>{
            const out=attemptJewelStarportShipAccess(this.state.world,this.state.combat,this.state.vehicles,
                requireCharacter(this.state.player.character),details);
            this.commit('WORLD_UPDATED',{world:out.world});
            return {adjudication:out.adjudication,accessGranted:out.accessGranted,state:clone(this.state)};
        });
    }
    /** P225: failed patrol -> confirmed physical Wing Guard -> actual initiative. */
    beginJewelResidenceWingResponse(details={}){
        return this.atomicAction(()=>{
            const prepared=prepareJewelResidenceWingResponse(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:prepared.world});
            const started=this.beginPublishedAdventureEncounter(prepared.encounterId,
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random});
            if(!this.state.combat?.active||this.state.combat.publishedAdventureContext?.encounterId!==prepared.encounterId)
                throw Error('Patrulha real não iniciou combate com iniciativa.');
            return {adjudication:prepared.adjudication,encounter:started.encounter,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P223: four physical Storm IV vehicles, each with its own official profile and pilot. */
    deployJewelStormIvPatrol(details={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Encerre o combate pessoal antes de iniciar perseguição veicular.');
            const out=deployJewelStormIvPatrol(this.state.world,this.state.vehicles,details);
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {adjudication:out.adjudication,vehicleIds:out.adjudication.vehicleIds,state:clone(this.state)};
        });
    }
    beginJewelStormIvCombat(details={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Combate pessoal ainda ativo.');
            const out=beginJewelStormIvCombat(this.state.world,this.state.vehicles,details);
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {adjudication:out.adjudication,combat:clone(this.state.vehicles.combat),state:clone(this.state)};
        });
    }
    resolveJewelStormIvCombat(details={}){
        return this.atomicAction(()=>{
            let check=null;
            if(details.outcome==='EVADED'){
                if(details.gmDifficultyConfirmed!==true||!Number.isSafeInteger(details.difficulty)||details.difficulty<1||details.difficulty>5)
                    throw Error('Mestre deve escolher dificuldade real (1–5) para a evasão.');
                check=this.resolveSkillCheck({skillKey:'pilotar_vehiculo_planetario',difficulty:details.difficulty,rng:details.rng??Math.random});
            }
            const out=resolveJewelStormIvCombat(this.state.world,this.state.vehicles,{...details,
                check:check?{pool:clone(check.pool),faces:clone(check.faces),success:check.success,
                 netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair}:null});
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {check,adjudication:out.adjudication,state:clone(this.state)};
        });
    }
    clearJewelStormIvPatrol(details={}){
        return this.atomicAction(()=>{
            const out=clearJewelStormIvPatrol(this.state.world,this.state.vehicles,details);
            this.commit('WORLD_UPDATED',{world:out.world});
            return {adjudication:out.adjudication,state:clone(this.state)};
        });
    }
    /** Storm IV minion pilot has Agility 3; Gunnery is group-only, rank 0 for a lone pilot. */
    resolveJewelStormIvIncomingAttack(details={}){
        const id=this.state.vehicles?.combat?.targetVehicleId;
        if(!id?.startsWith('jewel-venture-storm-iv-'))throw Error('Não há Storm IV oficial em combate.');
        if(details.npcAgility!==undefined||details.npcGunnery!==undefined)
            throw Error('Dados do piloto Storm IV vêm da ficha oficial, não do formulário.');
        return this.resolveIncomingVehicleAttack({...details,npcAgility:BESPIN_PILOT.agility,npcGunnery:0});
    }
    /** P231: three source-backed squadrons of four, after physical takeoff. */
    arriveJewelStarportSquadron(details={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Combate pessoal ativo.');
            const out=arriveJewelStarportSquadron(this.state.world,this.state.vehicles,details);
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {...out,state:clone(this.state)};
        });
    }
    beginJewelStarportCloudCombat(details={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Combate pessoal ativo.');
            const out=beginJewelStarportCloudCombat(this.state.world,this.state.vehicles,details);
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {...out,state:clone(this.state)};
        });
    }
    resolveJewelStarportCloudCombat(details={}){
        return this.atomicAction(()=>{
            let check=null;
            if(details.outcome==='EVADED'){
                if(details.gmDifficultyConfirmed!==true||!Number.isSafeInteger(details.difficulty)||details.difficulty<1||details.difficulty>5)
                    throw Error('Mestre precisa confirmar dificuldade da evasão (1–5).');
                check=this.resolveSkillCheck({skillKey:'pilotar_vehiculo_planetario',difficulty:details.difficulty,rng:details.rng??Math.random});
            }
            const out=resolveJewelStarportCloudCombat(this.state.world,this.state.vehicles,{...details,
                check:check?{pool:clone(check.pool),faces:clone(check.faces),success:check.success,
                    netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair}:null});
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});this.commit('WORLD_UPDATED',{world:out.world});
            return {check,adjudication:out.adjudication,state:clone(this.state)};
        });
    }
    clearJewelStarportSquadron(details={}){
        return this.atomicAction(()=>{
            const out=clearJewelStarportSquadron(this.state.world,this.state.vehicles,details);
            this.commit('WORLD_UPDATED',{world:out.world});return {...out,state:clone(this.state)};
        });
    }
    resolveJewelStarportCloudIncomingAttack(details={}){
        const id=this.state.vehicles?.combat?.targetVehicleId;
        if(!/^jewel-starport-storm-iv-(?:[1-9]|1[0-2])$/.test(id??''))throw Error('Nenhum Storm IV do porto em combate.');
        if(details.npcAgility!==undefined||details.npcGunnery!==undefined)
            throw Error('Ficha oficial do piloto não pode ser sobrescrita.');
        return this.resolveIncomingVehicleAttack({...details,npcAgility:BESPIN_PILOT.agility,npcGunnery:0});
    }
    /** P224: tangible jewel item and source-backed custody; no phantom acquisition. */
    registerJewelCustody(details={}){
        return this.atomicAction(()=>{
            const out=registerJewelCustody(this.state.world,this.state.combat,this.state.vehicles,requireCharacter(this.state.player.character),details);
            this.commit('CHARACTER_UPDATED',{character:out.character});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {...out,state:clone(this.state)};
        });
    }
    /** P227: atomic physical transfer between active PC and existing companion sheets. */
    transferJewelCustody(details={}){
        return this.atomicAction(()=>{
            const out=transferJewelCustody(this.state.world,this.state.combat,this.state.vehicles,
                requireCharacter(this.state.player.character),this.state.group,details);
            this.commit('CHARACTER_UPDATED',{character:out.character});
            this.commit('GROUP_UPDATED',{group:out.group});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {...out,state:clone(this.state)};
        });
    }
    registerJewelBoarding(details={}){
        return this.atomicAction(()=>{
            const out=registerJewelBoarding(this.state.world,this.state.combat,this.state.vehicles,requireCharacter(this.state.player.character),details,this.state.group);
            this.commit('WORLD_UPDATED',{world:out.world});return {...out,state:clone(this.state)};
        });
    }
    /** A failed/off-course jump persists its real roll and damage, but NEVER creates escape proof. */
    executeJewelBespinEscape(details={}){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            prepareJewelEscape(this.state.world,this.state.combat,this.state.vehicles,character,details,this.state.group);
            const jumpResult=this.resolveHyperspaceJump({...details,destinationLocationId:details.destinationLocationId});
            const jump=this.state.vehicles.travel?.lastHyperspaceJump;
            if(!jump?.arrived||!jump.outcome?.jumpSuccessful||jump.outcome.offCourse)
                return {escaped:false,jump:clone(jumpResult),state:clone(this.state)};
            const out=finalizeJewelEscape(this.state.world,this.state.combat,this.state.vehicles,character,details,jump,this.state.group);
            this.commit('WORLD_UPDATED',{world:out.world});
            return {escaped:true,adjudication:out.adjudication,departure:out.departure,jump:clone(jumpResult),state:clone(this.state)};
        });
    }
    /** P230: physical starport escape routes; no invented guards or transit. */
    confirmJewelStarportRoute(details={}){
        return this.atomicAction(()=>{
            const r=confirmJewelStarportRoute(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:r.world});return {...r,state:clone(this.state)};
        });
    }
    /** SWE09 p.94: +10 Criminal only for proven destructive escape. */
    registerJewelStarportDestruction(details={}){
        return this.atomicAction(()=>{
            if(!modeAllowsObligation(this.state))throw Error('Obrigação Criminal exige campanha EotE com Obrigação ativa.');
            const r=registerJewelStarportDestruction(this.state.world,this.state.group,this.state.combat,
                requireCharacter(this.state.player.character),details);
            this.commit('GROUP_UPDATED',{group:r.group});this.commit('WORLD_UPDATED',{world:r.world});
            return {...r,obligationTotal:obligation_mjs_1.obligationTotal(this.state.group,this.state.player.character),state:clone(this.state)};
        });
    }
    /** P222: source-backed Episode III PE, paid to the real PC only after a verified milestone. */
    grantJewelEpisode3Reward(details={}){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            const result=grantJewelEpisode3Reward(this.state.world,this.state.combat,character,details);
            this.commit('XP_GRANTED',{character:result.character,amount:result.adjudication.xp,reason:'SWE09 p.95 — '+details.milestone});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,awards:result.awards,character:clone(result.character),state:clone(this.state)};
        });
    }
    /** P220: stage-by-stage physical holding combat; exact official security forces. */
    beginJewelArendPropertyEncounter(details={}) {
        return this.atomicAction(()=>{
            const prepared=prepareJewelArendProperty(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:prepared.world});
            const started=this.beginPublishedAdventureEncounter(prepared.encounterId,
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random});
            if(!this.state.combat?.active||this.state.combat.publishedAdventureContext?.encounterId!==prepared.encounterId)
                throw Error('Encontro real no imóvel de Arend não iniciado.');
            return {adjudication:prepared.adjudication,encounter:started,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P219: real post-confrontation Arend outcome; no automatic XP or bounty. */
    recordJewelArendOutcome(details={}) {
        return this.atomicAction(()=>{
            const result=recordJewelArendOutcome(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,aftermath:result.aftermath,state:clone(this.state)};
        });
    }
    /** P219: Aris only becomes an ally after mutual GM-confirmed agreement. */
    recordJewelArisAlliance(details={}) {
        return this.atomicAction(()=>{
            const result=recordJewelArisAlliance(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,aftermath:result.aftermath,state:clone(this.state)};
        });
    }
    /** P218: SWE09 p.90, Lando's pardon creates real Favor Obligation for
     * every GM-confirmed PC; never auto-award for companions or NPCs. */
    grantJewelLandoPardon(details={}) {
        return this.atomicAction(()=>{
            if(!modeAllowsObligation(this.state))throw Error('Favor de Lando exige campanha EotE com Obrigação ativa.');
            const character=requireCharacter(this.state.player.character);
            const result=grantJewelLandoPardon(this.state.world,this.state.group,this.state.combat,character,details);
            this.commit('GROUP_UPDATED',{group:result.group});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,obligationTotal:obligation_mjs_1.obligationTotal(this.state.group,character),state:clone(this.state)};
        });
    }
    /** P207: published 25/50/75/automatic d100 at four distinct holdings. */
    visitJewelArendHolding(details={}) {
        return this.atomicAction(()=>{
            const result=visitJewelArendHolding(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,investigation:result.investigation,state:clone(this.state)};
        });
    }
    /** P199: GM-approved, real-price acquisition of a physical restraining bolt. */
    buyJewelRestrainingBolt(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes da compra.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.buyRestrainingBolt(this.state.world,character,details);
            this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P199: replace compromised slicing tools with a real 500-credit EotE kit. */
    replaceJewelSlicerTools(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de substituir ferramentas.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.replaceSlicerTools(this.state.world,character,details);
            this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,bankTools:result.bankTools,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    resolveJewelBankEvidence(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de lidar com as provas.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.performBankEvidence(this.state.world,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,bankEvidence:result.bankEvidence,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P195: keep the G-100 intact with a real Charm roll and confirmed berth. */
    resolveJewelLiveDroid(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw Error('Encerre o combate antes de negociar com o G-100.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.performLiveDroid(this.state.world,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,liveDroid:result.liveDroid,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P191: Table 2-1 symbols are spent AFTER the bank roll, with ledgered time. */
    spendJewelBankSymbols(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw new Error('Encerre o combate antes de gastar símbolos bancários.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            if(this.state.world?.publishedAdventure?.id!==adapter.id)
                throw new Error('A Joia de Yavin não está ativa.');
            const character=requireCharacter(this.state.player.character);
            // Wing Guard strength equals the number of PCs with actual party sheets.
            const partySize=1+(this.state.group?.companions?.length??0);
            const result=adapter.spendBankSymbols(this.state.world,details,character,partySize);
            this.commit('WORLD_UPDATED',{world:result.world});
            if(result.character)this.commit('CHARACTER_UPDATED',{character:result.character});
            return {adjudication:result.adjudication,spending:result.spending,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P188: source-backed bank preparation, with actual spike materials and Computers dice. */
    prepareJewelBank(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw new Error('Encerre o combate antes da preparação bancária.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            if(this.state.world?.publishedAdventure?.id!==adapter.id)
                throw new Error('A Joia de Yavin não está ativa.');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.prepareBank(this.state.world,character,details);
            if(result.character)this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,preparation:result.preparation,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P190: hire source-backed Port Town NPC slicer; real Streetwise and (R3) Negotiation rolls. */
    hireJewelBankSlicer(details={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)
                throw new Error('Encerre o combate antes da contratação.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const character=requireCharacter(this.state.player.character);
            const result=adapter.hireBankSlicer(this.state.world,character,details);
            if(result.character)this.commit('CHARACTER_UPDATED',{character:result.character});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {adjudication:result.adjudication,hiredSlicer:result.hiredSlicer,
                adventure:adapter.status(this.state.world),state:structuredClone(this.state)};
        });
    }
    /** P186: one atomic Grand Prix action: actual piloting dice, hazards, vehicle critical table and durable receipt. */
    resolveJewelGrandPrixLeg({actionId,placeAdjustment=0,difficultyUpgrades=0,rng=Math.random}={}) {
        return this.atomicAction(()=>{
            if(this.state.combat?.active||this.state.vehicles?.combat?.active)throw new Error('Encerre o combate antes da corrida.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('jewel-of-yavin');
            const progress=this.state.world?.publishedAdventure;
            if(progress?.id!==adapter.id||progress.status!=='ACTIVE'||progress.currentNodeId!=='grand-prix-race')throw new Error('O Grande Prêmio não está ativo.');
            if(typeof actionId!=='string'||!actionId.trim())throw new Error('actionId obrigatório.');
            if(progress.operations?.receipts?.[actionId])throw new Error('Ação já registrada: idempotência protegida.');
            const vehicleId=this.state.vehicles?.activeVehicleId;
            const vehicle=this.state.vehicles?.vehicles?.[vehicleId];
            if(!vehicle)throw new Error('Selecione um veículo real para disputar a corrida.');
            const vehicleStatus=vehicles_mjs_1.vehicleStatus(vehicle);
            if(vehicleStatus.destroyed||vehicleStatus.systemsShutdown||vehicleStatus.enginesDown)throw new Error('Veículo incapaz de disputar a corrida.');
            const leg=(progress.operations?.race?.leg??0)+1;
            const requirements=adapter.status(this.state.world).raceNextLegRequirements;
            if(!requirements||requirements.leg!==leg)throw new Error('Trecho da corrida inválido.');
            const pilotEvidence=rollJewelGrandPrixPilotCheck(requireCharacter(this.state.player.character),requirements,rng,difficultyUpgrades);
            const roll=pilotEvidence.roll;
            const threats=Math.max(0,-roll.netAdvantage);
            const despair=roll.despair;
            const criticalHit=leg===4?despair>0:leg===5?(threats>=3||despair>0):false;
            const hazard=(leg===4||leg===5)?{threats,despairs:despair,criticalHit}:null;
            const outcome=adapter.adjudicateRaceLeg(this.state.world,{
                actionId,leg,success:roll.success,speed:vehicle.speed,placeAdjustment,
                missedBuoy:!roll.success,criticalCollision:criticalHit,hazard,pilotEvidence,vehicleId
            });
            let updatedVehicle=vehicle;
            const strain=Number(outcome.adjudication.hazardStrain??0);
            if(strain)updatedVehicle=vehicles_mjs_1.applyVehicleSystemStrain(updatedVehicle,strain).vehicle;
            let critical=null;
            if(criticalHit){
                const applied=vehicles_mjs_1.applyVehicleCritical(updatedVehicle,{
                    rng,modifier:outcome.adjudication.criticalModifier??0,currentRound:leg
                });
                updatedVehicle=applied.vehicle;
                critical=structuredClone(applied.hit);
            }
            const vehicles=clone(this.state.vehicles);
            vehicles.vehicles[vehicleId]=updatedVehicle;
            this.commit('VEHICLES_UPDATED',{vehicles});
            this.commit('WORLD_UPDATED',{world:outcome.world});
            return {adjudication:outcome.adjudication,pilotCheck:pilotEvidence,vehicleCritical:critical,
                vehicle:clone(updatedVehicle),adventure:adapter.status(this.state.world),state:clone(this.state)};
        });
    }
    startPublishedAdventureSession(adventureId = 'under-black-sun', { rng = Math.random, actionId = null } = {}) {
        if (actionId !== null) return this.executePublishedAdventureCommand({ actionId, kind: 'start', adventureId, options: { rng } });
        return this.atomicAction(() => {
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(adventureId);
        const mode = campaignMode(this.state);
        if (!adapter.meta.campaignModes.includes(mode)) throw new Error(adapter.modeError || `A aventura ${adapter.meta.title} não suporta o modo ${mode}.`);
        requireCharacter(this.state.player?.character);
        let session = this.state.session;
        if (session?.status !== 'ativa') session = this.startSession({ playerCount: 1, rng }).session;
        const started = adapter.start(this.state.world);
        this.commit('WORLD_UPDATED', { world: started.world });
        const scene = adapter.currentScene(this.state.world);
        if (scene) this.commit('SCENE_UPDATED', { scene });
        const request = adapter.masterRequest(this.state.world);
        if (request) this.commit('MASTER_OPENING_PREPARED', { suggestedActions: request.suggestedActions, request });
        return { adventure: adapter.status(this.state.world), session: structuredClone(session), state: structuredClone(this.state) };
    
        });
    }
    advancePublishedAdventure(choiceId, { adventureId = null, actionId = null, rng = Math.random, unawareCharacterIds = [] } = {}) {
        if (actionId !== null) return this.executePublishedAdventureCommand({ actionId, kind: 'choice', adventureId, choiceId,
            options:choiceId==='ship-start-interception'?{unawareCharacterIds}:{} });
        return this.atomicAction(() => {
        const resolvedAdventureId = adventureId || this.state.world?.publishedAdventure?.id || 'under-black-sun';
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(resolvedAdventureId);
        const encounterContext = this.state.combat?.publishedAdventureContext ?? null;
        if (this.state.combat?.active)
            throw new Error('Encerre o encontro ou combate ativo antes de avançar para outra cena da aventura publicada.');
        if(adapter.id==='long-arm-of-the-hutt'&&choiceId==='ship-start-interception')
            this.beginSWE02ThwheekCombat({rng,unawareCharacterIds});
        const result = adapter.advance(this.state.world, String(choiceId ?? ''));
        let nextWorld=result.world;
        // SWE02 p.11: the hyperspace trip is six hours. The living-world
        // calendar uses four ticks per day, so one tick advances exactly six
        // hours and is committed with the scene transition.
        if(adapter.id==='long-arm-of-the-hutt'&&choiceId==='ship-depart-verified')
            nextWorld=sandbox_mjs_1.advanceLivingWorld(nextWorld,{ticks:1,maxEvents:50}).world;
        this.commit('WORLD_UPDATED', { world: nextWorld });
        // SWE13 p. 17: 500 credits per controlled PC for accepting the
        // Saleucami meeting. Atomic with the choice and its durable receipt;
        // never re-pay on reopens or actionId replays.
        if (adapter.id === 'mask-of-the-pirate-queen' && String(choiceId) === 'accept-venlana' &&
            this.state.world?.publishedAdventure?.flags?.contractRetainerAccepted &&
            !this.state.world.publishedAdventure.flags.contractRetainerPaid) {
            const character = structuredClone(requireCharacter(this.state.player?.character));
            character.credits = Math.max(0, Number(character.credits) || 0) + 500;
            this.commit('CHARACTER_UPDATED', { character });
            const paidWorld = structuredClone(this.state.world);
            paidWorld.publishedAdventure.flags.contractRetainerPaid = true;
            if (paidWorld.publishedAdventureRuntime?.adventureId === adapter.id) {
                paidWorld.publishedAdventureRuntime.flags = {
                    ...(paidWorld.publishedAdventureRuntime.flags || {}), contractRetainerPaid:true
                };
            }
            this.commit('WORLD_UPDATED', { world: paidWorld });
        }
        const scene = adapter.currentScene(this.state.world);
        if (scene) this.commit('SCENE_UPDATED', { scene });
        const request = adapter.masterRequest(this.state.world);
        if (request) this.commit('MASTER_OPENING_PREPARED', { suggestedActions: request.suggestedActions, request });
        let rewards = null;
        if (result.completed && !this.state.world?.publishedAdventure?.rewardsApplied) {
            const policy = typeof adapter.rewardPolicyForWorld === 'function'
                ? adapter.rewardPolicyForWorld(this.state.world) : (adapter.rewardPolicy ?? {});
            const creditReward = Math.max(0, Number(policy.credits) || 0);
            const character = structuredClone(requireCharacter(this.state.player?.character));
            character.credits = Math.max(0, Number(character.credits) || 0) + creditReward;
            this.commit('CHARACTER_UPDATED', { character });
            let obligationReduced = 0;
            let obligationAdded = 0;
            if (modeAllowsObligation(this.state)) {
                const group = structuredClone(this.state.group);
                const entries = Array.isArray(group?.obligationEntries) ? group.obligationEntries : [];
                const characterName = String(character.name ?? '').trim().toLocaleLowerCase('pt-BR');
                let entry = entries.find(e => characterName && String(e?.member ?? '').trim().toLocaleLowerCase('pt-BR') === characterName && Number(e?.value) > 0);
                if (!entry) entry = entries.filter(e => Number(e?.value) > 0).sort((a,b) => Number(b.value)-Number(a.value))[0] ?? null;
                const pendingFlag = String(policy.pendingObligationFlag ?? 'obligationPendingDelta');
                const pending = Math.max(0, Number(this.state.world?.publishedAdventure?.flags?.[pendingFlag]) || 0);
                if (pending > 0) {
                    if (!entry) { entry = { id: `pubadv_${Date.now()}`, member: character.name ?? '', type: policy.obligationType || 'Dívida', description: policy.obligationDescription || `Consequência de ${adapter.meta.title}.`, value: 0 }; entries.push(entry); }
                    entry.value = Math.max(0, Number(entry.value) || 0) + pending; obligationAdded = pending;
                }
                const reductionCap = Math.max(0, Number(policy.obligationReduction) || 0);
                if (entry && reductionCap > 0) { obligationReduced = Math.min(reductionCap, Math.max(0, Number(entry.value) || 0)); entry.value = Math.max(0, Number(entry.value) - obligationReduced); }
                group.obligationEntries = entries; this.commit('GROUP_UPDATED', { group });
            }
            const reductionCap = Math.max(0, Number(policy.obligationReduction) || 0);
            const note = obligationReduced < reductionCap && modeAllowsObligation(this.state) ? 'A redução restante de Obligation não foi aplicada porque não havia valor suficiente em uma entrada ativa.' : null;
            const world = adapter.markRewardsApplied(this.state.world, { credits: creditReward, obligationAdded, obligationReduced, note });
            this.commit('WORLD_UPDATED', { world });
            rewards = { credits: creditReward, obligationAdded, obligationReduced, note };
        }
        return { adventure: adapter.status(this.state.world), rewards, state: structuredClone(this.state) };
    
        });
    }
    resolvePublishedAdventureCheck(checkIndex = 0, { adventureId = null, useLightDestiny = false, useDarkDestiny = false, repairReductionMinutes=null, aidingLowhhrick=false, knowledgeSkillKey=null, rng = Math.random, actionId = null } = {}) {
        if (actionId !== null) return this.executePublishedAdventureCommand({ actionId, kind: 'check', adventureId, checkIndex, options: { useLightDestiny, useDarkDestiny, repairReductionMinutes, aidingLowhhrick, knowledgeSkillKey, rng } });
        return this.atomicAction(() => {
        const resolvedAdventureId = adventureId || this.state.world?.publishedAdventure?.id || 'under-black-sun';
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(resolvedAdventureId);
        const specs = adapter.currentChecks(this.state.world);
        const requestedIndex = Number(checkIndex);
        const spec = specs.find(x => x.index === requestedIndex) || specs[requestedIndex];
        if (!spec) throw new Error('Teste da aventura indisponível neste ponto.');
        const allowedKnowledgeSkill=spec.knowledgeSkillOptions?.some(x=>x.key===String(knowledgeSkillKey??''))===true;
        if (!spec.skillKey&&!allowedKnowledgeSkill) throw new Error(spec.knowledgeSkillOptions?'O Mestre deve escolher uma especialização de Conhecimento do EotE Core para este teste.':'A perícia deste teste ainda exige adjudicação manual do Mestre.');
        if(spec.skillKey&&knowledgeSkillKey!==null)throw new Error('Especialização de Conhecimento não se aplica a este teste.');
        const resolvedSkillKey=spec.skillKey||String(knowledgeSkillKey);
        const shipOdorSetback=adapter.id==='long-arm-of-the-hutt'&&spec.shipOdorRule
            ?longArmShipSmellSetback(this.state.world,this.state.player?.character?.name,spec.index):0;
        const check = this.resolveSkillCheck({ skillKey: resolvedSkillKey, difficulty: spec.pool.difficulty, challenge: spec.pool.challenge, boost:Number(spec.pool.boost||0), setback: (Number(spec.pool.setback)||0)+shipOdorSetback+(aidingLowhhrick&&adapter.id==='long-arm-of-the-hutt'?2:0), useLightDestiny, useDarkDestiny, cloudCitySlicing: adapter.id === 'jewel-of-yavin' && resolvedSkillKey === 'computadoras' && this.state.world?.publishedAdventure?.currentNodeId === 'gala-reconnaissance', rng });
        let adjudicated = adapter.adjudicateCheck(this.state.world, { checkIndex: spec.index, result: {...check,...(allowedKnowledgeSkill?{skillKey:resolvedSkillKey}:{})}, repairReductionMinutes });
        const characterEffects = { creditsDelta: 0, strainDelta: 0, partyStrain:[] };
        if(adapter.id==='long-arm-of-the-hutt'&&spec.source?.printedPage===15&&check.success===false){
            const old=adjudicated.world.publishedAdventure?.oldLylek,ids=old?.crewPcIds??[],roster=clone(this.state.player?.roster);
            if(!ids.length||ids.length!==Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.eoteRules?.destiny?.playerCount??this.state.session?.playerCount))
                throw Error('SWE02 p.15: falha de Pilotagem exige roster completo dos PCs a bordo.');
            const applications=[];
            for(const id of ids){
                const entry=roster?.entries?.find(x=>x.id===id);if(!entry)throw Error('SWE02 p.15: PC a bordo ausente do roster.');
                const before=Number(entry.character.derived?.strain?.current??0),applied=health_mjs_1.sufferStrain(entry.character,1);
                entry.character=applied.character;applications.push({pcId:id,characterName:entry.character.name,strainBefore:before,strainAfter:Number(entry.character.derived?.strain?.current??before+1),strainApplied:1});
            }
            const checkIndex=old.pilotChecks.length-1;
            adjudicated=recordLongArmOldLylekPilotStrain(adjudicated.world,{checkIndex,applications});
            this.commit('PLAYER_ROSTER_UPDATED',{roster});characterEffects.partyStrain=applications.map(x=>({pcId:x.pcId,strainDelta:1}));
        }
        const creditsDelta = Number(adjudicated.adjudication?.creditDelta ?? 0);
        const strainDelta = Math.max(0, Number(adjudicated.adjudication?.playerStrainDelta ?? 0));
        if (creditsDelta || strainDelta) {
            let character = structuredClone(requireCharacter(this.state.player?.character));
            if (creditsDelta) {
                character.credits = Math.max(0, (Number(character.credits) || 0) + creditsDelta);
                characterEffects.creditsDelta = creditsDelta;
            }
            if (strainDelta) {
                const strained = health_mjs_1.sufferStrain(character, strainDelta);
                character = strained.character;
                characterEffects.strainDelta = strainDelta;
            }
            this.commit('CHARACTER_UPDATED', { character });
        }
        this.commit('WORLD_UPDATED', { world: adjudicated.world });
        const request = adapter.masterRequest(this.state.world);
        if (request) this.commit('MASTER_OPENING_PREPARED', { suggestedActions: request.suggestedActions, request });
        return { check, adjudication: adjudicated.adjudication, characterEffects, adventure: adapter.status(this.state.world), state: structuredClone(this.state) };
    
        });
    }
    beginPublishedAdventureEncounter(encounterId = null, { adventureId = null, rng = Math.random, playerQuickReactionStrain = 0, actionId = null, gmParkArrivalConfirmed = false, palaceAdditionalHenchmenCount = null, gmHuttProfileConfirmed = false } = {}) {
        if (actionId !== null) return this.executePublishedAdventureCommand({ actionId, kind: 'encounter', adventureId, encounterId, options: { rng, playerQuickReactionStrain, palaceAdditionalHenchmenCount, gmHuttProfileConfirmed } });
        return this.atomicAction(() => {
        const resolvedAdventureId = adventureId || this.state.world?.publishedAdventure?.id || 'under-black-sun';
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(resolvedAdventureId);
        if(encounterId==='holo-park-wing-guard' && gmParkArrivalConfirmed!==true)
            throw Error('Holo-Park exige chegada física confirmada pelo Mestre e comando específico.');
        if (this.state.combat?.active) throw new Error('Já existe um combate ativo. Encerre-o antes de iniciar outro encontro.');
        // SWE02 p.16: hunter count equals the number of player characters.
        // Use the actual active session size, never an invented four-PC group.
        let encounterWorld=this.state.world;
        if(adapter.id==='long-arm-of-the-hutt' && ['lylek-den-bounty-hunters','laoth-new-meen-night-raid','laoth-drombb-cantina','teemo-palace-guards'].includes(encounterId)){
            const count=Number(this.state.session?.campaignRules?.destiny?.playerCount ?? this.state.session?.eoteRules?.destiny?.playerCount ?? this.state.session?.playerCount);
            if(Number.isInteger(count)&&count>=1&&count<=8){
                encounterWorld=structuredClone(this.state.world);
                encounterWorld.publishedAdventure.flags={...(encounterWorld.publishedAdventure.flags||{}),confirmedPartySize:count};
            }
        }
        if(adapter.id==='long-arm-of-the-hutt'&&encounterId==='teemo-palace-guards'){
            if(!Number.isInteger(palaceAdditionalHenchmenCount)||palaceAdditionalHenchmenCount<1||palaceAdditionalHenchmenCount>8||gmHuttProfileConfirmed!==true)
                throw Error('Teemo exige confirmação do segundo grupo e conferência do perfil oficial do Hutt.');
            encounterWorld=structuredClone(encounterWorld);
            encounterWorld.publishedAdventure.flags={...(encounterWorld.publishedAdventure.flags||{}),
                palaceAdditionalHenchmenCount,gmHuttProfileConfirmed:true};
        }
        const spec = adapter.encounterSpec(encounterWorld, encounterId);
        if(spec.requiresGMAdditionalGroupReview||spec.requiresOfficialHuttProfileReview)
            throw Error('Encontro de Teemo exige conferência editorial e confirmação do segundo grupo.');
        if(spec.requiresPartySizeConfirmation)throw new Error('Confirme o tamanho do grupo na sessão antes de iniciar a emboscada.');
        if(spec.requiresTrexSlotReview)throw new Error('SWE02 p.15: T’rex está vivo, mas um grupo de um único PC não permite confirmar um slot adicional sem remover o Gand obrigatório. A composição fica PENDENTE.');
        const adversaries = spec.adversaries.map(entry => {
            const npc = entry.worldNpcId ? this.state.world?.npcs?.[entry.worldNpcId] : null;
            const adversary = adversaries_mjs_1.createAdversary(entry.profileKey, { count: entry.count, range: entry.range, name: entry.name || npc?.name || null });
            adversary.factionId = npc?.factionId ?? entry.factionId ?? null;
            adversary.worldNpcId = npc?.id ?? null;
            if (npc) {
                adversary.id = npc.id;
                const persisted = npc.combatState;
                if (persisted) {
                    if (persisted.wounds) adversary.wounds = { ...adversary.wounds, ...clone(persisted.wounds) };
                    if (adversary.strain && persisted.strain) adversary.strain = { ...adversary.strain, ...clone(persisted.strain) };
                    adversary.criticalInjuries = clone(persisted.criticalInjuries ?? []);
                    adversary.status = persisted.status ?? adversary.status;
                } else if (Number(entry.preWounds) > 0 && adversary.wounds) {
                    adversary.wounds.current = Math.min(Number(adversary.wounds.threshold) || 0, Math.max(0, Number(adversary.wounds.current) || 0) + Number(entry.preWounds));
                }
            } else if (Number(entry.preWounds) > 0 && adversary.wounds) {
                // Published encounter source may carry pre-existing damage for
                // a profile-backed adversary without a separate world-NPC row.
                adversary.wounds.current = Math.min(Number(adversary.wounds.threshold) || 0, Math.max(0, Number(adversary.wounds.current) || 0) + Number(entry.preWounds));
            }
            return adversary;
        });
        const priorPublishedEnvironment=Number(spec.environment?.darknessSetbackDice)>0?clone(this.state.environment):null;
        if(priorPublishedEnvironment){
            const environmental=environment_mjs_1.configureEnvironment(this.state.environment,{scriptedSetbackDice:Number(spec.environment.darknessSetbackDice)});
            this.commit('ENVIRONMENT_UPDATED',{environment:environmental});
        }
        let publishedEncounterWorld=encounterWorld;
        if(adapter.id==='long-arm-of-the-hutt'&&publishedEncounterWorld.publishedAdventure?.newMeenLifecycle){
            const stage=publishedEncounterWorld.publishedAdventure.newMeenLifecycle.stage;
            if(encounterId==='laoth-new-meen-night-raid'&&stage==='NIGHT_STANDOFF')
                publishedEncounterWorld=adapter.recordNewMeenLifecycle(publishedEncounterWorld,'night_interaction',{response:'aggressive_move'}).world;
            else if(encounterId==='laoth-new-meen-night-raid'&&stage!=='NIGHT_COMBAT')
                throw Error('SWE02 p.20: registre a resposta à abordagem noturna antes do combate.');
            if(encounterId==='laoth-drombb-cantina'&&['CANTINA_READY','CANTINA_COMBAT'].includes(publishedEncounterWorld.publishedAdventure.newMeenLifecycle.stage))
                publishedEncounterWorld=adapter.recordNewMeenLifecycle(publishedEncounterWorld,'cantina_start').world;
        }
        const started = this.beginEncounter({ adversaries, rng, playerInitiativeSkill: spec.playerInitiativeSkill, npcInitiativeSkill: spec.npcInitiativeSkill, npcInitiativeSetback:spec.npcInitiativeSetback, playerQuickReactionStrain });
        const marked = adapter.markEncounterActive(publishedEncounterWorld, spec.id);
        this.commit('WORLD_UPDATED', { world: marked });
        const next = clone(this.state);
        next.combat.publishedAdventureContext = { adventureId: adapter.id, encounterId: spec.id, label: spec.label, nodeId: next.world?.publishedAdventure?.currentNodeId ?? null, ...(priorPublishedEnvironment?{priorEnvironment:priorPublishedEnvironment}:{}) };
        if(adapter.id==='long-arm-of-the-hutt'&&encounterId==='lylek-den-bounty-hunters'){
            const tactics=clone(next.world?.publishedAdventure?.oldLylek?.tactics??{});
            next.combat.swe02OldLylekTactics={approach:tactics.approach,frontalOutcome:tactics.frontalOutcome,
                startingPosition:tactics.approach==='east_cave_flank'?'partially blocked eastern cave entrance':'frontal approach',
                hunterRetreatPath:tactics.approach==='frontal'&&tactics.frontalOutcome==='fighting_retreat'?'dotted route':'unselected',
                source:{productCode:'SWE02',printedPage:16,relatedPrintedPage:17}};
            next.combat.swe02OldLylekCover={playerBehindRockPile:false,source:{productCode:'SWE02',printedPage:17,coverSetback:2}};
            next.combat.swe02HunterIntent={mode:'CAPTURE',stunArmedResistance:true,lethalAfterHunterFalls:true,
                source:{productCode:'SWE02',printedPage:17}};
            next.combat.swe02OldLylekMap=createLongArmOldLylekMapState(next.world,next.combat,next.player?.roster?.entries??[]);
        }
        next.combat.log = [...(next.combat.log ?? []), { type: 'ENCONTRO_AVENTURA_PUBLICADA', round: next.combat.round, encounterId: spec.id, summary: spec.label }];
        // The published-combat context must be committed through the event
        // reducer, not assigned directly: otherwise causal replay and any
        // future event-sourced restore may lose the encounter ownership.
        this.commit('COMBAT_ACTION', { combat: next.combat });
        return { ...started, encounter: spec, adventure: adapter.status(this.state.world), state: structuredClone(this.state) };
    
        });
    }
    /** P156: transactional, explicit countdown of the Imperial assault at Raxus. */
    advancePublishedImperialClock({minutes=5,actionId}={}){
        // A replayed action is a read: never persist a duplicate receipt.
        const preview=advanceBeyondTheRimImperialClock(this.state.world,{minutes,actionId});
        if(preview.duplicate)return {...preview,world:clone(this.state.world)};
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Encerre o combate ativo antes de avançar o relógio imperial.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('beyond-the-rim');
            const result=adapter.advanceImperialClock(this.state.world,{minutes,actionId});
            this.commit('WORLD_UPDATED',{world:result.world});
            return {...result,world:clone(this.state.world),clock:beyondTheRimImperialClockStatus(this.state.world)};
        });
    }
    restartPublishedAdventure(adventureId = 'under-black-sun', options = {}) {
        return this.startPublishedAdventureSession(adventureId, options);
    }
    continuePublishedAdventureToSandbox({ adventureId = null, rng = Math.random } = {}) {
        const resolvedAdventureId = adventureId || this.state.world?.publishedAdventure?.id || 'under-black-sun';
        const adapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(resolvedAdventureId);
        let status = adapter.status(this.state.world);
        const continuation = adapter.continuation ?? {};
        if (status.archived) {
            if (!this.state.world?.sandbox?.initialized)
                this.initializeLivingSandbox({ startingPoint: continuation.startingPoint || 'Ponto de continuidade', regionLabel: continuation.regionLabel || 'Região atual' });
            return { continued: true, alreadyArchived: true, adventure: adapter.status(this.state.world), sandbox: this.livingSandboxStatus(), state: structuredClone(this.state) };
        }
        if (!status.completed) throw new Error(`Conclua ${adapter.meta.title} antes de continuar no mundo livre.`);
        if (this.state.combat?.active) throw new Error('Encerre o combate antes de continuar no mundo livre.');
        if (!this.state.world?.sandbox?.initialized)
            this.initializeLivingSandbox({ startingPoint: continuation.startingPoint || 'Ponto de continuidade', regionLabel: continuation.regionLabel || 'Região atual' });
        const startLocationId = this.state.world?.sandbox?.startLocationId ?? null;
        const location = startLocationId ? this.state.world.locations?.[startLocationId] : null;
        if (!startLocationId || !location) throw new Error('O ponto de continuidade do mundo livre não está disponível.');
        this.commit('SCENE_UPDATED', { scene: { locationId: startLocationId, title: location?.name ?? continuation.regionLabel ?? 'Mundo livre', narration: '', visibleNpcIds: [] } });
        const sandbox = this.startSandboxSession({ rng });
        const archived = adapter.archive(this.state.world);
        this.commit('WORLD_UPDATED', { world: archived });
        status = adapter.status(this.state.world);
        return { ...sandbox, continued: true, adventure: status, state: structuredClone(this.state) };
    }
    // Compatibilidade para bridges antigas; o fluxo canônico do mundo livre continua startSandboxSession.
    startLivingAdventure(options = {}) { return this.startSandboxSession(options); }
    activeWorldOpportunities({ locationId = null, knownOnly = false, limit = 50 } = {}) { return sandbox_mjs_1.activeOpportunities(this.state.world, { locationId, knownOnly, limit }); }
    createWorldOpportunity(payload = {}) { const result = sandbox_mjs_1.createOpportunity(this.state.world, payload); this.commit('WORLD_UPDATED', { world: result.world }); return result.opportunity; }
    setWorldOpportunityStatus(opportunityId, status, payload = {}) { const result = sandbox_mjs_1.setOpportunityStatus(this.state.world, opportunityId, status, payload); this.commit('WORLD_UPDATED', { world: result.world }); return result.opportunity; }
    advanceWorldClock(clockId, payload = {}) { const result = sandbox_mjs_1.advanceWorldClock(this.state.world, clockId, payload); this.commit('WORLD_UPDATED', { world: result.world }); return result.clock; }
    currentMarketProfile(locationId = null) { return sandbox_mjs_1.marketProfile(this.state.world, locationId ?? this.state.scene?.locationId ?? null); }
    travelToLocation(toLocationId, { maxEvents = 50 } = {}) {
        const fromLocationId = this.state.scene?.locationId ?? this.state.world?.sandbox?.startLocationId ?? null;
        if (!fromLocationId) throw new Error('Defina um local inicial antes de viajar.');
        if (fromLocationId === toLocationId) return { fromLocationId, toLocationId, ticks: 0, world: this.state.world, scene: this.state.scene };
        const route = sandbox_mjs_1.routeBetween(this.state.world, fromLocationId, toLocationId);
        if (!route) throw new Error('Não há rota conhecida entre os locais selecionados.');
        const result = sandbox_mjs_1.advanceLivingWorld(this.state.world, { ticks: route.ticks, maxEvents });
        this.commit('WORLD_UPDATED', { world: result.world });
        const location = this.state.world.locations?.[toLocationId];
        const destinationOpening = sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: toLocationId, limit: 6 });
        this.commit('SCENE_UPDATED', { scene: { locationId: toLocationId, title: location?.name ?? this.state.scene?.title ?? '', narration: '', visibleNpcIds: [] } });
        return { ...result, route, fromLocationId, toLocationId, scene: this.state.scene };
    }
    beginPendingMasterEncounter({ rng = Math.random, playerQuickReactionStrain = 0 } = {}) {
        const directive = this.state.master?.pendingEncounter;
        if (!directive)
            throw new Error('O Mestre não preparou nenhum encontro.');
        validateEncounterDirectiveAgainstState(this.state, directive);
        const sourceHook = directive.sourceHookId ? this.state.world.hooks.find(h => h.id === directive.sourceHookId) : null;
        const adversaries = directive.adversaries.map(spec => {
            const npc = spec.worldNpcId ? this.state.world.npcs[spec.worldNpcId] : null;
            const adversary = adversaries_mjs_1.createAdversary(spec.profileKey, { count: spec.count, range: spec.range, name: spec.name || npc?.name || null });
            adversary.factionId = npc?.factionId ?? spec.factionId ?? sourceHook?.factionId ?? null;
            adversary.worldNpcId = npc?.id ?? null;
            if (npc) {
                adversary.id = npc.id;
                const persisted = npc.combatState;
                if (persisted) {
                    if (persisted.wounds)
                        adversary.wounds = { ...adversary.wounds, ...clone(persisted.wounds) };
                    if (adversary.strain && persisted.strain)
                        adversary.strain = { ...adversary.strain, ...clone(persisted.strain) };
                    adversary.criticalInjuries = clone(persisted.criticalInjuries ?? []);
                    adversary.status = persisted.status ?? adversary.status;
                }
            }
            return adversary;
        });
        const started = this.beginEncounter({ adversaries, rng, playerInitiativeSkill: directive.playerInitiativeSkill, npcInitiativeSkill: directive.npcInitiativeSkill, playerQuickReactionStrain });
        const next = clone(this.state);
        next.master.pendingEncounter = null;
        if (sourceHook) {
            next.combat.causalHookId = sourceHook.id;
            next.combat.causalHookSummary = sourceHook.summary;
            next.combat.log = [...(next.combat.log ?? []), { type: 'ENCONTRO_CAUSAL_INICIADO', round: next.combat.round, hookId: sourceHook.id, summary: sourceHook.summary }];
        }
        this.state = next;
        if (!this._atomicActionDepth) this.repository.save(this.state);
        return { ...started, sourceHookId: sourceHook?.id ?? null };
    }
    environmentStatus() {
        const environment=environment_mjs_1.normalizeEnvironmentRuntime(this.state.environment);
        const character=this.state.player?.character;
        const swift=Boolean(character&&(talents_mjs_1.talentRankByOriginal(character,'SWIFT')>0||talents_mjs_1.talentRankByOriginal(character,'VELOZ')>0));
        return {environment,movement:environment_mjs_1.movementEnvironmentStatus(environment,{swift}),health:character?health_mjs_1.getHealthStatus(character):null};
    }
    configureEnvironment(options={}) {
        const environment=environment_mjs_1.configureEnvironment(this.state.environment,options);
        this.commit('ENVIRONMENT_UPDATED',{environment});
        return this.environmentStatus();
    }
    clearEnvironment() {
        const environment=environment_mjs_1.clearEnvironment();
        this.commit('ENVIRONMENT_UPDATED',{environment});
        return this.environmentStatus();
    }
    applyEnvironmentTurnStart({rng=Math.random,force=false}={}) {
        return this.atomicAction(() => {
        const character=requireCharacter(this.state.player?.character);
        if(this.state.combat?.active&&!force) throw new Error('Em combate, os perigos ambientais são aplicados automaticamente no início do turno do jogador.');
        const tick=environment_mjs_1.applyEnvironmentTurnStart(this.state.environment,character,{rng});
        this.commit('COMBAT_ACTION',{character:tick.character,environment:tick.environment});
        return tick;
        });
    }
    resolveImpassableTerrain({skillKey='atletismo',difficulty=2,rng=Math.random}={}) {
        return this.atomicAction(() => {
        const environment=environment_mjs_1.normalizeEnvironmentRuntime(this.state.environment);
        if(environment.terrain!=='impassable') throw new Error('O ambiente atual não está marcado como terreno intransponível.');
        if(!['atletismo','coordinacion'].includes(skillKey)) throw new Error('Terreno intransponível exige Atletismo ou Coordenação.');
        const check=this.resolveSkillCheck({skillKey,difficulty,rng});
        let nextEnvironment=environment_mjs_1.normalizeEnvironmentRuntime(this.state.environment);
        if(Number(check.netSuccess??0)>0){nextEnvironment=environment_mjs_1.grantImpassableClearance(nextEnvironment);this.commit('ENVIRONMENT_UPDATED',{environment:nextEnvironment});}
        return {check,success:Number(check.netSuccess??0)>0,environment:nextEnvironment};
        });
    }
    resolveFall({range='Curto',skillKey='atletismo',useTriumphRangeReduction=false,rng=Math.random}={}) {
        return this.atomicAction(() => {
        if(!['atletismo','coordinacion'].includes(skillKey)) throw new Error('Uma queda só pode ser mitigada com Atletismo ou Coordenação.');
        const check=this.resolveSkillCheck({skillKey,difficulty:2,rng});
        let effectiveRange=String(range);
        const triumphReduction=Boolean(useTriumphRangeReduction&&Number(check.triumph??0)>0);
        if(triumphReduction) effectiveRange=environment_mjs_1.reduceFallRange(effectiveRange);
        const current=requireCharacter(this.state.player.character);
        const profile=environment_mjs_1.fallingProfile(effectiveRange);
        const resolved=profile.forcedIncapacitation
            ? environment_mjs_1.applyForcedFall(current,{range:effectiveRange,rng})
            : environment_mjs_1.applyNumericFall(current,{range:effectiveRange,netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,rng});
        this.commit('CHARACTER_UPDATED',{character:resolved.character});
        return {...resolved,check,requestedRange:String(range),effectiveRange,triumphReduction};
        });
    }
    _environmentTurnStart(character,rng=Math.random) {
        return environment_mjs_1.applyEnvironmentTurnStart(this.state.environment,character,{rng});
    }
    beginCombat() {
        return this.atomicAction(() => {
        const combat = combat_mjs_1.startCombat(this.state.combat);
        let character=this.state.player.character;
        if(character?.combatEffects?.heroicFortitude||character?.combatEffects?.steelyNerves||character?.combatEffects?.mentalFortress){character=clone(character);delete character.combatEffects.heroicFortitude;delete character.combatEffects.steelyNerves;delete character.combatEffects.mentalFortress;}
        let environment=this.state.environment;
        if(character){const tick=this._environmentTurnStart(character);character=tick.character;environment=tick.environment;}
        this.commit('COMBAT_ACTION', { combat, character, environment });
        if (this.state.session?.status === 'ativa')
            this.commit('SESSION_COMBAT_STARTED', { session: sessionFlow_mjs_1.noteSessionCombatStarted(this.state.session) });
        return combat;
        });
    }
    beginPresetEncounter({ rng = Math.random, playerInitiativeSkill = 'alerta', npcInitiativeSkill = 'alerta', playerQuickReactionStrain = 0 } = {}) {
        const adversaries = [
            adversaries_mjs_1.createAdversary('imperial_soldier', { count: 3, range: 'Médio' }),
            adversaries_mjs_1.createAdversary('imperial_officer', { range: 'Médio' })
        ];
        return this.beginEncounter({ adversaries, rng, playerInitiativeSkill, npcInitiativeSkill, playerQuickReactionStrain });
    }
    beginEncounter({ adversaries, rng = Math.random, playerInitiativeSkill = 'alerta', npcInitiativeSkill = 'alerta', npcInitiativeSetback = 0, playerQuickReactionStrain = 0 } = {}) {
        return this.atomicAction(() => {
        let character = this.state.player.character;
        assertCanAct(character);
        if(character?.combatEffects?.heroicFortitude||character?.combatEffects?.steelyNerves||character?.combatEffects?.mentalFortress){character=clone(character);delete character.combatEffects.heroicFortitude;delete character.combatEffects.steelyNerves;delete character.combatEffects.mentalFortress;}
        if (!Array.isArray(adversaries) || !adversaries.length)
            throw new Error('O encontro precisa de adversários.');
        playerInitiativeSkill = initiativeSkillKey(playerInitiativeSkill);
        npcInitiativeSkill = initiativeSkillKey(npcInitiativeSkill);
        let combat = combat_mjs_1.startCombat(this.state.combat);
        const quickReaction = initiative_mjs_1.preparePlayerInitiative(character, { quickReactionStrain: playerQuickReactionStrain });
        character = quickReaction.character;
        assertCanAct(character);
        const playerCharacteristic = character_mjs_1.SKILLS[playerInitiativeSkill];
        const playerPool = narrativeDice_mjs_1.buildPositivePool(character.characteristics[playerCharacteristic], character.skills[playerInitiativeSkill] ?? 0);
        const playerCheck = narrativeDice_mjs_1.rollPool({ ...playerPool, boost: quickReaction.boost }, rng);
        const entries = [initiative_mjs_1.initiativeEntry({ id: 'player', side: 'player', check: playerCheck, label: character.name })];
        for (const adversary of adversaries) {
            const pool = narrativeDice_mjs_1.buildPositivePool(adversaries_mjs_1.adversaryCharacteristicForSkill(adversary, npcInitiativeSkill), adversaries_mjs_1.adversarySkillRank(adversary, npcInitiativeSkill));
            const setback=Number(adversary.initiativeSetback??npcInitiativeSetback)||0;
            const check = narrativeDice_mjs_1.rollPool({ ...pool, setback }, rng);
            entries.push(initiative_mjs_1.initiativeEntry({ id: adversary.id, side: 'npc', check, label: adversary.name }));
        }
        combat = encounter_mjs_1.setupEncounter(combat, { entries, adversaries, playerActorId: 'player' });
        const progressed = this._processNpcTurnsUntilPlayer(combat, character, rng, this.state.group);
        combat = progressed.combat;
        character = progressed.character;
        const group=progressed.group;
        let environment=this.state.environment;
        if(combat?.encounter?.waitingForPlayer){const tick=this._environmentTurnStart(character,rng);character=tick.character;environment=tick.environment;}
        this.commit('COMBAT_ACTION', { combat, character, group, environment, check: playerCheck });
        if (this.state.session?.status === 'ativa')
            this.commit('SESSION_COMBAT_STARTED', { session: sessionFlow_mjs_1.noteSessionCombatStarted(this.state.session) });
        return { combat, character, playerCheck, entries, npcTurns: progressed.npcTurns, quickReaction: quickReaction.quickReaction };
        });
    }
    _processNpcTurnsUntilPlayer(combat, character, rng = Math.random, group = this.state.group) {
        let nextCombat = clone(combat);
        let nextCharacter = clone(character);
        let nextGroup = clone(group);
        const npcTurns = [];
        let guard = 0;
        while (nextCombat.active && guard++ < 30) {
            const health=health_mjs_1.getHealthStatus(nextCharacter);
            const canRecover=health.strainIncapacitated&&!health.woundIncapacitated&&!health.criticalIncapacitated&&!health.dead&&
                talents_mjs_1.hardHeadedOption(nextCharacter,{improved:true}).available&&
                nextCombat.hardHeadedRecovery?.attempted!==true;
            if (encounter_mjs_1.encounterOver(nextCombat,health.unconscious) &&
                (!canRecover || encounter_mjs_1.livingNpcIds(nextCombat).length===0)) {
                nextCombat.encounter = nextCombat.encounter ?? {};
                nextCombat.encounter.waitingForPlayer = false;
                nextCombat.encounter.outcome = health.unconscious ? 'player_incapacitated' : 'adversaries_defeated';
                break;
            }
            if(health.unconscious&&canRecover&&!nextCombat.hardHeadedRecovery)
                nextCombat.hardHeadedRecovery={ready:false,incapacitatedRound:nextCombat.round,attempted:false};
            const slot = encounter_mjs_1.currentSlot(nextCombat);
            if (!slot)
                break;
            if (slot.side === 'player') {
                nextCombat = encounter_mjs_1.assignCurrentSlot(nextCombat, 'player');
                if(health.unconscious&&canRecover&&nextCombat.hardHeadedRecovery)
                    nextCombat.hardHeadedRecovery.ready=true;
                break;
            }
            if(health.unconscious&&canRecover){
                nextCombat=encounter_mjs_1.skipCurrentSlot(nextCombat,{summary:'Jogador incapacitado; aguarda oportunidade de Tenacidade aprimorada.'});
                nextCombat=applySWE02ThroneReinforcementsAtRoundBoundary(nextCombat,{rng});
                continue;
            }
            const available = encounter_mjs_1.availableActorsForCurrentSlot(nextCombat);
            if (!available.length) {
                nextCombat = encounter_mjs_1.skipCurrentSlot(nextCombat, { summary: 'Nenhum adversário ativo disponível para esta posição.' });
                nextCombat=applySWE02ThroneReinforcementsAtRoundBoundary(nextCombat,{rng});
                continue;
            }
            const actorId = available[0];
            nextCombat = encounter_mjs_1.assignCurrentSlot(nextCombat, actorId);
            const resolved = this._resolveNpcTurn(nextCombat, nextCharacter, actorId, rng, nextGroup);
            nextCombat = resolved.combat;
            nextCharacter = resolved.character;
            nextGroup = resolved.group??nextGroup;
            npcTurns.push(resolved.summary);
            nextCombat = encounter_mjs_1.completeCurrentSlot(nextCombat, actorId, { summary: resolved.summary.text });
            nextCombat=applySWE02ThroneReinforcementsAtRoundBoundary(nextCombat,{rng});
        }
        if (guard >= 30)
            throw new Error('Proteção de loop da iniciativa acionada.');
        return { combat: nextCombat, character: nextCharacter, group: nextGroup, npcTurns };
    }
    queueWingGuardTactic(options={}) {
        return this.atomicAction(()=>{
            const combat=queueWingGuardTactic(this.state.combat,options);
            this.commit('COMBAT_ACTION',{combat});
            return {combat,order:combat.wingGuardOrders.at(-1)};
        });
    }
    _resolveNpcTurn(combat, character, adversaryId, rng = Math.random, group = this.state.group) {
        const original = combat.adversaries.find(a => a.id === adversaryId);
        if (!original)
            throw new Error('Adversário não encontrado no encontro.');
        let nextCombat=clone(combat);
        let adversary=clone(original);
        const burnNotes=[];
        if(Array.isArray(adversary.ongoingEffects)&&adversary.ongoingEffects.length){
            const remaining=[];
            for(const effect of adversary.ongoingEffects){
                if(effect?.type==='burn'&&Number(effect.roundsRemaining)>0){
                    const burned=adversaries_mjs_1.applyAdversaryDamage(adversary,{damage:Math.max(0,Number(effect.damage??0)),pierce:0,damageType:'wounds'});
                    adversary=burned.adversary;
                    burnNotes.push(`Incendiária causou ${burned.suffered} Ferimento(s)`);
                    const left=Math.max(0,Number(effect.roundsRemaining)-1);
                    if(left>0) remaining.push({...effect,roundsRemaining:left});
                } else remaining.push(effect);
            }
            adversary.ongoingEffects=remaining;
        }
        if (adversaries_mjs_1.adversaryHealthStatus(adversary).incapacitated) {
            nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
            nextCombat.wingGuardOrders=(nextCombat.wingGuardOrders??[]).filter(o=>o.actorId!==adversaryId);
            return { combat:nextCombat, character, summary: { actorId: adversaryId, text: `${adversary.name} foi incapacitado antes de agir${burnNotes.length?` (${burnNotes.join('; ')})`:''}.` } };
        }
        let stoodFromProne=false;
        if(statusEffects_mjs_1.statusModifiers(adversary).prone){
            adversary=statusEffects_mjs_1.removeStatusEffect(adversary,{type:'prone'});
            stoodFromProne=true;
            if(adversary.aorCripplingBlow?.strainPerMove===1){
              adversary=adversaries_mjs_1.sufferAdversaryStrain(adversary,1).adversary;
              if(adversaries_mjs_1.adversaryHealthStatus(adversary).incapacitated){
                nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
                nextCombat.wingGuardOrders=(nextCombat.wingGuardOrders??[]).filter(o=>o.actorId!==adversaryId);
                return {combat:nextCombat,character,summary:{actorId:adversaryId,text:`${adversary.name} sofreu Tensão ao se mover e foi incapacitado.`}};
              }
            }
        }
        const status=statusEffects_mjs_1.statusModifiers(adversary,{consumeNextCheck:true});
        adversary=status.character;
        if(status.cannotAct){
            adversary=statusEffects_mjs_1.expireStatusTurn(adversary);
            nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
            nextCombat.wingGuardOrders=(nextCombat.wingGuardOrders??[]).filter(o=>o.actorId!==adversaryId);
            const text=`${adversary.name} está staggered e perde a ação${burnNotes.length?`; ${burnNotes.join('; ')}`:''}`;
            nextCombat.log.push({type:'TURNO_NPC_SEM_ACAO',round:nextCombat.round,actorId:adversary.id,summary:text});
            return {combat:nextCombat,character,summary:{actorId:adversary.id,text:`${text}.`}};
        }
        // Execute the GM-queued maneuver on the actor's actual initiative slot,
        // after incapacity/stagger checks and before its normal action.
        nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
        nextCombat=triggerSWE02ThroneAlarmOnNpcTurn(nextCombat,adversaryId);
        const wing=applyQueuedWingGuardTactic(nextCombat,adversaryId);
        nextCombat=wing.combat;
        adversary=clone(nextCombat.adversaries.find(a=>a.id===adversaryId));
        const arrow7Weapon=arrow7WeaponForNpcTurn(nextCombat,adversary);
        let weapon = arrow7Weapon??chooseNpcWeapon(adversary);
        if (!weapon) {
            adversary=statusEffects_mjs_1.expireStatusTurn(adversary);
            nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
            return { combat:nextCombat, character, summary: { actorId: adversaryId, text: `${adversary.name} não possui ataque utilizável nesta distância.` } };
        }
        const tactical=consumeWingGuardTacticalBoost(adversary);
        adversary=tactical.adversary;
        const wingTacticalBoost=tactical.boost;
        const oldLylekHunterFight=nextCombat.publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&nextCombat.publishedAdventureContext?.encounterId==='lylek-den-bounty-hunters';
        const priorHunterDown=oldLylekHunterFight&&nextCombat.adversaries.some(a=>['laoth_gand_bounty_hunter','laoth_bounty_hunter','trex'].includes(a.profileKey)&&adversaries_mjs_1.adversaryHealthStatus(a).incapacitated);
        const playerArmed=Boolean((character.weapons??[]).some(w=>w&&typeof w==='object'));
        const oldLylekStunIntent=Boolean(oldLylekHunterFight&&!priorHunterDown&&playerArmed&&weapons_mjs_1.weaponHasQuality(weapon,'stunSetting'));
        if(oldLylekHunterFight){
            weapon={...clone(weapon),...(oldLylekStunIntent?{damageType:'strain',stunSettingActive:true}:{stunSettingActive:false})};
            nextCombat.swe02HunterIntent={...(nextCombat.swe02HunterIntent??{}),mode:priorHunterDown?'LETHAL_AFTER_HUNTER_FALLEN':oldLylekStunIntent?'STUN_ARMED_RESISTANCE':'CAPTURE_PREFERENCE',armedResistance:Boolean(playerArmed),hunterAlreadyDown:Boolean(priorHunterDown),lastActorId:adversary.id,source:{productCode:'SWE02',printedPage:17}};
        }
        const damagePenalty=itemDamageUsePenalty(weapon);
        const characteristic = adversaries_mjs_1.adversaryCharacteristicForSkill(adversary, weapon.skill);
        const skill = adversaries_mjs_1.adversarySkillRank(adversary, weapon.skill);
        const positive = narrativeDice_mjs_1.buildPositivePool(characteristic, skill);
        const baseDifficulty = attackDifficulty_mjs_1.attackDifficultyForWeapon(weapon, adversary.range)+damagePenalty.difficultyIncrease+Number(status.difficultyIncrease??0);
        // P171: GM-directed companion targeting consumes the real NPC slot.
        // Defense belongs to the companion, not the player's Sense/Dodge.
        const chosenId=nextCombat.npcCompanionTargetId;
        const chosen=(group?.companions??[]).find(m=>m.id===chosenId);
        if(chosenId&&(!chosen||health_mjs_1.getHealthStatus(chosen.character).unconscious))
            delete nextCombat.npcCompanionTargetId;
        else if(chosen){
            const nextGroup=clone(group);
            const member=nextGroup.companions.find(m=>m.id===chosenId);
            const guard=nextCombat.bodyGuard?.companionId===chosenId?nextCombat.bodyGuard:null;
            const upgrades=guard?Number(guard.difficultyUpgrades??0):0;
            if(!Number.isSafeInteger(upgrades)||upgrades<0||upgrades>5)
                throw new Error('Proteção de Guarda-costas inválida.');
            const negative=difficultyWithUpgrades(baseDifficulty,0,upgrades);
            const environmentModifiers=environment_mjs_1.environmentSkillModifiers(this.state.environment,weapon.skill,character_mjs_1.SKILLS[weapon.skill]);
            const defense=Number(member.character?.derived?.defense?.[isMeleeWeapon(weapon)?'melee':'ranged']??0);
            if(!Number.isSafeInteger(defense)||defense<0||defense>4)
                throw new Error('Defesa do companheiro inválida.');
            const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:negative.difficulty,challenge:negative.challenge,
                boost:Number(nextCombat.pendingNpcBoost??0)+wingTacticalBoost+Number(status.boost??0)+Number(environmentModifiers.boost??0),
                setback:defense+damagePenalty.setback+Number(status.setback??0)+Number(environmentModifiers.setback??0)},rng);
            const attack=weapons_mjs_1.resolveWeaponHit(weapon,check);
            let damageResult=null,criticalResult=null,medicCharacter=character;
            const onBeforeCritical=()=>{
                const aid=nextCombat.aorCriticalAid;
                if(!aid||aid.companionId!==chosenId||aid.sessionId!==this.state.sessionId)return null;
                if(talentUsedThisSession(medicCharacter,'itsNotThatBad'))throw Error('Uso duplicado de Não é tão grave.');
                const medPool=narrativeDice_mjs_1.buildPositivePool(
                    Number(medicCharacter.characteristics?.intelecto??0),Number(medicCharacter.skills?.medicina??0));
                const medicineCheck=narrativeDice_mjs_1.rollPool({...medPool,difficulty:3},rng);
                medicCharacter=markTalentUsedThisSession(medicCharacter,'itsNotThatBad');
                delete nextCombat.aorCriticalAid;
                nextCombat.log.push({type:'AOR_ITS_NOT_THAT_BAD',round:nextCombat.round,
                    sessionId:this.state.sessionId,companionId:chosenId,success:medicineCheck.netSuccess>0,
                    check:clone(medicineCheck)});
                return {prevented:medicineCheck.netSuccess>0,talent:'ITS_NOT_THAT_BAD',check:medicineCheck};
            };
            if(attack.hit){
                damageResult=health_mjs_1.applyDamage(member.character,{damage:attack.damage,pierce:attack.pierce,
                    damageType:attack.damageType,reason:`Ataque de ${adversary.name} contra ${member.character.name}`,criticalRng:rng,onBeforeCritical});
                member.character=damageResult.character;
                if(damageResult.suffered>0&&attack.criticalAvailable&&!damageResult.criticalAdded&&attack.damageType!=='strain'){
                    criticalResult=critical_mjs_1.addStarterCritical(member.character,`Crítico de ${adversary.name}`,{rng,onBeforeCritical});
                    member.character=criticalResult.character;
                }
            }
            character=medicCharacter;
            nextCombat.pendingNpcBoost=0;
            const threatCost=Math.max(0,-Number(check.netAdvantage||0))+Math.max(0,Number(check.despair||0));
            if(threatCost>0)adversary=adversaries_mjs_1.sufferAdversaryStrain(adversary,threatCost).adversary;
            else if(Number(check.netAdvantage||0)>0)nextCombat.pendingNpcBoost=1;
            adversary=statusEffects_mjs_1.expireStatusTurn(adversary);
            nextCombat=encounter_mjs_1.updateAdversaryInCombat(nextCombat,adversary);
            if(health_mjs_1.getHealthStatus(member.character).unconscious)delete nextCombat.npcCompanionTargetId;
            const summary=`${adversary.name}: ${attack.hit?'atingiu':'errou'} ${member.character.name} (${damageResult?.suffered??0} dano, ${upgrades} melhorias de dificuldade por Guarda-costas).`;
            const blast=npcBlastReceipt({attackId:ids_mjs_1.newId('npcblast'),attacker:adversary,weapon,check,
                hit:Boolean(attack.hit),sessionId:this.state.sessionId,round:nextCombat.round,originalTargetCompanionId:chosenId});
            if(blast){if((nextCombat.pendingNpcBlasts??[]).length>=16)throw Error('Fila de explosões NPC cheia; resolva recibos pendentes antes de avançar.');
                nextCombat.pendingNpcBlasts=[...(nextCombat.pendingNpcBlasts??[]),blast];
                nextCombat.log.push({type:'AOR_NPC_BLAST_READY',attackId:blast.attackId,sourceAdversaryId:adversary.id,
                    sessionId:this.state.sessionId,blastDamage:blast.blastDamage,round:nextCombat.round,
                    hit:blast.hit,originalTargetCompanionId:blast.originalTargetCompanionId});}
            nextCombat.lastNpcAttack={...clone(attack),attackerId:adversary.id,targetCompanionId:chosenId,bodyGuardUpgrades:upgrades};
            nextCombat.log.push({type:'TURNO_NPC_COMPANHEIRO',round:nextCombat.round,actorId:adversary.id,
                companionId:chosenId,weapon:weapon.name,bodyGuardUpgrades:upgrades,check:clone(check),damage:damageResult?.suffered??0,
                criticalAdded:Boolean(damageResult?.criticalAdded||criticalResult?.added)});
            return {combat:nextCombat,character,group:nextGroup,attack,damageResult,criticalResult,
                summary:{actorId:adversary.id,text:summary}};
        }
        const senseDefense = force_mjs_1.consumeSenseOngoingTrigger(character, { mode:'defense', round:nextCombat?.round ?? 0 });
        character = senseDefense.character;
        const incomingMelee=isMeleeWeapon(weapon);
        const defensiveTalentUpgrades=incomingMelee?Number(character?.combatEffects?.defensiveStanceTalent?.difficultyUpgrades??0):Number(character?.combatEffects?.sideStep?.difficultyUpgrades??0);
        const dodgeUpgrades=Math.max(0,Number(character?.combatEffects?.dodgeNextIncoming?.difficultyUpgrades??0));
        if(dodgeUpgrades>0){character=clone(character);delete character.combatEffects.dodgeNextIncoming;if(!Object.keys(character.combatEffects).length)delete character.combatEffects;}
        const feintUpgrades=incomingMelee?0:Math.max(0,Number(character?.combatEffects?.feint?.difficultyUpgrades??0));
        const senseDifficulty = difficultyWithUpgrades(baseDifficulty, 0, (senseDefense.applied ? senseDefense.upgrades : 0)+defensiveTalentUpgrades+dodgeUpgrades+feintUpgrades);
        const difficulty = senseDifficulty.difficulty;
        const pendingBoost = Number(combat.pendingNpcBoost ?? 0);
        const environmentModifiers=environment_mjs_1.environmentSkillModifiers(this.state.environment,weapon.skill,character_mjs_1.SKILLS[weapon.skill]);
        const check = narrativeDice_mjs_1.rollPool({
            ...positive,
            difficulty,
            challenge: senseDifficulty.challenge,
            boost: pendingBoost+wingTacticalBoost+Number(status.boost??0)+Number(proneTargetModifiers(character,weapon).boost??0)+Number(environmentModifiers.boost??0),
            setback: defenseSetback(character, weapon,nextCombat)+holoParkSpotlightSetback(nextCombat)+(oldLylekHunterFight&&Boolean(nextCombat.swe02OldLylekCover?.playerBehindRockPile)&&!isMeleeWeapon(weapon)?2:0)+damagePenalty.setback+Number(status.setback??0)+aorSocial_mjs_1.activeScathingSetback(adversary,nextCombat.round)+Number(proneTargetModifiers(character,weapon).setback??0)+(guardedStanceActive(character,nextCombat?.round)&&isMeleeWeapon(weapon)?1:0)+Number(environmentModifiers.setback??0)
        }, rng);
        let attack = weapons_mjs_1.resolveWeaponHit(weapon, check);
        attack = { ...attack, ...(oldLylekHunterFight?{oldLylekHunterIntent:{stun:oldLylekStunIntent,mode:nextCombat.swe02HunterIntent.mode,sourcePage:17},coverSetback:oldLylekHunterFight&&Boolean(nextCombat.swe02OldLylekCover?.playerBehindRockPile)&&!isMeleeWeapon(weapon)?2:0}:{}),wingTacticalBoost, targetRange: adversary.range, difficulty, challenge:senseDifficulty.challenge, attackerId: adversary.id, attackerName: adversary.name, weaponDamageState:damagePenalty.state, environmentModifiers, senseDefense:{applied:senseDefense.applied,upgrades:senseDefense.upgrades,triggersUsed:senseDefense.triggersUsed,triggersPerRound:senseDefense.triggersPerRound}, defensiveTalentUpgrades, defensiveTalent: defensiveTalentUpgrades>0?(incomingMelee?'Postura Defensiva':'Passo Lateral'):null, feintUpgrades, dodgeUpgrades, dodgeApplied:dodgeUpgrades>0 };
        let nextCharacter = clone(character);
        nextCombat.pendingNpcBoost = 0;
        let damageResult = null;
        let criticalResult = null;
        let targetText = stoodFromProne ? 'levantou-se e falhou o ataque' : 'falhou o ataque';
        if (attack.hit) {
            const characterId=this.state.player?.roster?.activeCharacterId;
            const defectiveArmor=(nextCharacter.armor??[]).filter(item=>item?.defectiveUntilFirstUse===true&&item?.ownedAssetId);
            if(characterId&&defectiveArmor.length){nextCombat.swe02NynDefectiveUses=Array.isArray(nextCombat.swe02NynDefectiveUses)?nextCombat.swe02NynDefectiveUses:[];
                for(const item of defectiveArmor)if(!nextCombat.swe02NynDefectiveUses.some(x=>x.characterId===characterId&&x.ownedAssetId===item.ownedAssetId))
                    nextCombat.swe02NynDefectiveUses.push({characterId,ownedAssetId:item.ownedAssetId,kind:'armor'});}
            damageResult = health_mjs_1.applyDamage(nextCharacter, { damage: attack.damage, pierce: attack.pierce, damageType: attack.damageType, reason: `Ataque de ${adversary.name}`, criticalRng: rng });
            nextCharacter = damageResult.character;
            if (Number(damageResult.suffered ?? 0) > 0) nextCombat = clearPlayerAim(nextCombat);
            targetText = `${stoodFromProne?'levantou-se e ':''}acertou e causou ${damageResult.suffered} ${damageResult.damageType === 'strain' ? 'de Tensão' : 'Ferimento(s)'}`;
            if (damageResult.suffered > 0 && attack.criticalAvailable) {
                criticalResult = critical_mjs_1.addStarterCritical(nextCharacter, `Crítico de ${adversary.name}`, { rng });
                nextCharacter = criticalResult.character;
                if (criticalResult.added)
                    targetText += ' + 1 Ferimento Crítico';
            }
        }
        let updatedAdversary = adversary;
        // P213: reserve one ACTUAL NPC Threat instead of spending it twice
        // (the older generic NPC-strain fallback may consume only the remainder).
        const npcAttackId=ids_mjs_1.newId('holo-npc');
        const reserved=reserveJewelHoloNpcThreat(nextCombat,this.state.world?.publishedAdventure,
            adversary.id,check,npcAttackId);
        nextCombat=reserved.combat;
        const despairReserved=reserveJewelHoloNpcDespair(nextCombat,this.state.world?.publishedAdventure,
            adversary.id,check,npcAttackId);
        nextCombat=despairReserved.combat;
        const threatCost = Math.max(0, -Number(check.netAdvantage || 0)) + Math.max(0, Number(check.despair || 0)) - reserved.reserved - despairReserved.reserved;
        if (threatCost > 0) {
            const consequence = adversaries_mjs_1.sufferAdversaryStrain(updatedAdversary, threatCost);
            updatedAdversary = consequence.adversary;
            targetText += consequence.convertedToWounds ? `; ${adversary.name} sofreu ${threatCost} Ferimento(s) por Ameaça/Desespero` : `; sofreu ${threatCost} de Tensão por Ameaça/Desespero`;
        }
        else if (Number(check.netAdvantage || 0) > 0 && !criticalResult?.added) {
            nextCombat.pendingNpcBoost = 1;
            targetText += '; 1 Impulso preparado para o próximo aliado';
        }
        if(burnNotes.length) targetText += `; ${burnNotes.join('; ')}`;
        updatedAdversary=statusEffects_mjs_1.expireStatusTurn(updatedAdversary);
        nextCombat = encounter_mjs_1.updateAdversaryInCombat(nextCombat, updatedAdversary);
        const lylekNpcEvent=this._applySWE02OldLylekAdvantage(nextCombat,check,{preferredHunterId:adversaryId});
        nextCombat=lylekNpcEvent.combat;
        if(lylekNpcEvent.event)targetText+=` ${lylekNpcEvent.event.display}`;
        const blast=npcBlastReceipt({attackId:ids_mjs_1.newId('npcblast'),attacker:adversary,weapon,check,
            hit:Boolean(attack.hit),sessionId:this.state.sessionId,round:nextCombat.round});
        if(blast){if((nextCombat.pendingNpcBlasts??[]).length>=16)throw Error('Fila de explosões NPC cheia; resolva recibos pendentes antes de avançar.');
                nextCombat.pendingNpcBlasts=[...(nextCombat.pendingNpcBlasts??[]),blast];
            nextCombat.log.push({type:'AOR_NPC_BLAST_READY',attackId:blast.attackId,sourceAdversaryId:adversary.id,
                sessionId:this.state.sessionId,blastDamage:blast.blastDamage,round:nextCombat.round,
                    hit:blast.hit,originalTargetCompanionId:blast.originalTargetCompanionId});}
        if(arrow7Weapon)nextCombat=consumeJewelArrow7Turret(nextCombat,adversaryId);
        nextCombat.lastNpcAttack = {...clone(attack),attackId:npcAttackId};
        nextCombat.log.push({ type: 'TURNO_NPC', attackId:npcAttackId, round: nextCombat.round, actorId: adversary.id, weapon: weapon.name, check: clone(check), summary: targetText });
        return {
            combat: nextCombat,
            character: nextCharacter,
            attack,
            damageResult,
            criticalResult,
            summary: { actorId: adversary.id, text: `${adversary.name}: ${targetText}.` }
        };
    }
    advancePlayerTurn({ rng = Math.random } = {}) {
        return this.atomicAction(() => {
        if (this.state.combat?.encounter?.slots?.length)
            return this.finishPlayerInitiativeTurn({ rng });
        const combat = combat_mjs_1.nextPlayerTurn(this.state.combat);
        delete combat.bodyGuard;
        let character = this.state.player.character ? critical_mjs_1.expirePlayerTurnCriticalEffects(this.state.player.character) : null;
        if (character) character = statusEffects_mjs_1.expireStatusTurn(character);
        if (character) character = expireStartOfPlayerTurnTalentEffects(character);
        let environment=this.state.environment;
        if(character){const tick=this._environmentTurnStart(character,rng);character=tick.character;environment=tick.environment;}
        this.commit('COMBAT_ACTION', { combat, character, environment });
        return combat;
        });
    }
    finishPlayerInitiativeTurn({ rng = Math.random } = {}) {
        return this.atomicAction(() => {
        let combat = this.state.combat;
        let character = this.state.player.character;
        const slot = encounter_mjs_1.currentSlot(combat);
        if (!slot || slot.side !== 'player' || !combat.encounter?.waitingForPlayer)
            throw new Error('Não há turno do jogador aguardando encerramento.');
        combat=clone(combat); combat.pendingPlayerSpend=null; combat.pendingPlayerCritical=null; delete combat.pendingPreciseAim; delete combat.pendingSniperShot; delete combat.pendingDestinyDamageTalent;
        delete combat.pendingDestinyDamageTalent;
        combat = encounter_mjs_1.completeCurrentSlot(combat, 'player', { summary: 'Turno do jogador concluído.' });
        combat=applySWE02ThroneReinforcementsAtRoundBoundary(combat,{rng});
        combat=clearHoloParkSpotlightAtTurnEnd(combat);
        character = critical_mjs_1.expirePlayerTurnCriticalEffects(character);
        character = statusEffects_mjs_1.expireStatusTurn(character);
        const progressed = this._processNpcTurnsUntilPlayer(combat, character, rng, this.state.group);
        combat = progressed.combat;
        character = progressed.character;
        const group=progressed.group;
        let environment=this.state.environment;
        if(combat?.encounter?.waitingForPlayer){
            delete combat.bodyGuard;
            character=expireStartOfPlayerTurnTalentEffects(character);
            const tick=this._environmentTurnStart(character,rng);character=tick.character;environment=tick.environment;
        }
        this.commit('COMBAT_ACTION', { combat, character, group, environment });
        return { combat, character, group, environment, npcTurns: progressed.npcTurns };
        });
    }
    finishCombat({ rng = Math.random, strainSkill = 'frialdad', useBalance = false, gmConfirmedSubmission = false, actionId = null } = {}) {
        if (actionId !== null) return this.executePublishedAdventureCommand({ actionId, kind: 'finish-combat', adventureId: this.state.world?.publishedAdventure?.id, options: { rng, strainSkill, useBalance, gmConfirmedSubmission } });
        return this.atomicAction(() => {
        const preCombat = clone(this.state.combat);
        validateSWE02ThroneAlarm(preCombat);
        const causalHookId = preCombat?.causalHookId ?? null;
        const causalOutcome = preCombat?.encounter?.outcome ?? null;
        const publishedAdventureContext = preCombat?.publishedAdventureContext ?? null;
        if(publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&publishedAdventureContext?.encounterId==='lylek-den-lylek'&&causalOutcome==='player_incapacitated'&&gmConfirmedSubmission!==true)
            throw Error('SWE02 p.18: confirme submissão conforme adjudicação explícita do Mestre; não há limite numérico publicado.');
        let publishedAdventureAdapter = null;
        if (publishedAdventureContext?.adventureId) {
            publishedAdventureAdapter = publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter(publishedAdventureContext.adventureId);
            const currentAdventure = this.state.world?.publishedAdventure;
            if (!currentAdventure || currentAdventure.id !== publishedAdventureContext.adventureId || !['ACTIVE','active'].includes(currentAdventure.status))
                throw new Error('O encontro publicado não corresponde mais a uma aventura ativa.');
            if (publishedAdventureContext.nodeId && currentAdventure.currentNodeId !== publishedAdventureContext.nodeId)
                throw new Error('A cena da aventura mudou durante o encontro; o combate não será encerrado com estado divergente.');
            publishedAdventureAdapter.encounterSpec(this.state.world, publishedAdventureContext.encounterId);
        }
        if(publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&
           ['laoth-new-meen-night-raid','laoth-drombb-cantina'].includes(publishedAdventureContext.encounterId)&&
           causalOutcome==='player_incapacitated'&&
           !health_mjs_1.getHealthStatus(requireCharacter(this.state.player?.character)).unconscious)
            throw Error('New Meen: derrota sem incapacitação real do jogador.');
        const persistentAdversaries = clone(preCombat?.adversaries ?? []).filter(a => this.state.world?.npcs?.[a.id]).map(a=>{delete a.aorCripplingBlow;return a;});
        const encounterAdversaries = clone(preCombat?.adversaries ?? []).map(a => ({ id: a.id, name: a.name, profileKey: a.profileKey ?? null, worldNpcId: a.worldNpcId ?? null, factionId: a.factionId ?? null, status: a.status ?? 'active', membersTotal:a.membersTotal, perMember:a.wounds?.perMember, wounds: Number(a.wounds?.current ?? 0), strain: Number(a.strain?.current ?? 0), criticalCount: a.criticalInjuries?.length ?? 0 }));
        let combat = combat_mjs_1.endCombat(preCombat);
        let character = this.state.player.character;
        if(character?.combatEffects){character=recovery_mjs_1.expireStimApplication(character);character=clone(character);if(character.combatEffects){delete character.combatEffects.coverRangedDefense;delete character.combatEffects.guardedStance;delete character.combatEffects.sideStep;delete character.combatEffects.feint;delete character.combatEffects.heroicFortitude;delete character.combatEffects.steelyNerves;delete character.combatEffects.mentalFortress;delete character.combatEffects.defensiveStanceTalent;delete character.combatEffects.dodgeNextIncoming;if(!Object.keys(character.combatEffects).length)delete character.combatEffects;}}
        let recovery = null;
        if (character) {
            // AoR Contraption devices are encounter-only: remove the actual
            // temporary gear item on combat closure, not just its narration.
            character=clone(character);
            character.gear=(character.gear??[]).filter(item=>item.temporaryEncounter!==true);
            character = critical_mjs_1.expireEncounterCriticalEffects(character);
            character = statusEffects_mjs_1.expireStatusEncounter(character);
            recovery = recovery_mjs_1.recoverAfterEncounter(character, { skillKey: strainSkill, rng });
            character = recovery.character;
            if (useBalance) {
                const balance=talents_mjs_1.balanceRecovery(character,{rng});
                if(!balance.available) throw new Error('Equilíbrio não foi adquirido.');
                const extra=health_mjs_1.recoverStrain(character,balance.recovered);
                character=extra.character;
                recovery={...recovery,balance:{...balance,recovered:extra.recovered}};
            }
        }
        // Encounter-only stimulants on companion sheets expire with the same
        // atomic combat closure as the player's effects (never on reload).
        const group=clone(this.state.group);
        if(Array.isArray(group.companions)) for(const member of group.companions)
            member.character=recovery_mjs_1.expireStimApplication(member.character);
        const defectiveUses=Array.isArray(preCombat.swe02NynDefectiveUses)?preCombat.swe02NynDefectiveUses:[];
        let updatedRoster=null;
        if(defectiveUses.length){
            updatedRoster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            for(const entry of updatedRoster.entries){
                const usedIds=new Set(defectiveUses.filter(x=>x.characterId===entry.id).map(x=>x.ownedAssetId));
                if(!usedIds.size)continue;
                const pc=entry.id===updatedRoster.activeCharacterId?character:clone(entry.character);
                if(!pc)continue;
                pc.weapons=(pc.weapons??[]).filter(item=>!usedIds.has(item.ownedAssetId));
                pc.armor=(pc.armor??[]).filter(item=>!usedIds.has(item.ownedAssetId));
                entry.character=pc;
                if(entry.id===updatedRoster.activeCharacterId)character=pc;
            }
            delete combat.swe02NynDefectiveUses;
        }
        this.commit('COMBAT_ACTION', { combat, character, group });
        if(updatedRoster)this.commit('PLAYER_ROSTER_UPDATED',{roster:updatedRoster});
        if (persistentAdversaries.length) {
            let world = clone(this.state.world);
            for (const a of persistentAdversaries) {
                world = world_mjs_1.setNpcCombatState(world, a.id, { profileKey: a.profileKey, kind: a.kind, wounds: a.wounds, strain: a.strain, criticalInjuries: a.criticalInjuries, status: a.status });
                if (a.status === 'incapacitated')
                    world = world_mjs_1.setNpcStatus(world, a.id, 'incapacitado');
            }
            this.commit('WORLD_UPDATED', { world });
        }
        let resolvedWorldHook = null;
        let encounterRecord = null;
        let aftermath = null;
        if (causalOutcome) {
            const resolution = causalOutcome === 'adversaries_defeated' ? 'Conflito encerrado: adversários derrotados.' : causalOutcome === 'player_incapacitated' ? 'Conflito encerrado: jogador incapacitado.' : `Conflito encerrado: ${causalOutcome}.`;
            const recorded = world_mjs_1.recordEncounterOutcome(this.state.world, { hookId: causalHookId, outcome: causalOutcome, summary: resolution, rounds: Number(preCombat.round ?? 0), adversaries: encounterAdversaries });
            let world = recorded.world;
            encounterRecord = recorded.encounter;
            aftermath = recorded.aftermath;
            if (causalHookId) {
                world = world_mjs_1.resolveWorldHook(world, causalHookId, { resolution });
                resolvedWorldHook = world.hooks.find(h => h.id === causalHookId) ?? null;
            }
            this.commit('WORLD_UPDATED', { world });
        }
        let publishedAdventureOutcome = null;
        if (publishedAdventureAdapter) {
            const published = publishedAdventureAdapter.recordEncounterOutcome(this.state.world, { encounterId: publishedAdventureContext.encounterId, outcome: causalOutcome ?? 'ended', adversaries: encounterAdversaries, throneAlarm:preCombat.swe02ThroneAlarm??null, gmConfirmedSubmission,
                combatLog:[...(preCombat.log??[]),...(preCombat.swe02OldLylekAdvantageTriggered?[{check:{netAdvantage:1},sourcePage:18}]:[])] });
            this.commit('WORLD_UPDATED', { world: published.world });
            publishedAdventureOutcome = published.record;
        }
        if (publishedAdventureContext?.priorEnvironment){
            this.commit('ENVIRONMENT_UPDATED',{environment:publishedAdventureContext.priorEnvironment});
        }
        if (this.state.session?.status === 'ativa')
            this.commit('SESSION_COMBAT_ENDED', { session: sessionFlow_mjs_1.noteSessionCombatEnded(this.state.session) });
        return { combat, recovery, resolvedWorldHook, causalOutcome, encounterRecord, aftermath, publishedAdventureOutcome };
    
        });
    }
    useAction(label = 'Ação') {
        let character = this.state.player.character;
        const critical = assertCanPerformAction(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const combat = clearPlayerAim(combat_mjs_1.spendAction(this.state.combat, label));
        character = applyActionCriticalStrain(character, critical);
        this.commit('COMBAT_ACTION', { combat, character });
        return combat;
    }
    useManeuver({ method = 'normal', label = 'Manobra' } = {}) {
        const movement = spendPlayerManeuver(this.state.player.character, this.state.combat, { method, label });
        if (/^(mover|movimentar|aproximar|afastar|deslocar)/i.test(String(label).trim())){movement.combat=clearHoloParkCoruscantCover(movement.combat);movement.combat=clearHoloParkTransportOnMovement(movement.combat);}
        this.commit('COMBAT_ACTION', { combat: movement.combat, character: movement.character });
        return movement;
    }
    changeCombatRange({ targetAdversaryId, direction = 'closer', maneuverMethod = 'normal' } = {}) {
        let character = requireCharacter(this.state.player.character);
        assertCanAct(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const combat0 = this.state.combat;
        if (!combat0?.active) throw new Error('O combate ainda não foi iniciado.');
        const status = statusEffects_mjs_1.statusModifiers(character);
        if (status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
        const targetId = String(targetAdversaryId ?? '').trim();
        const target = combat0.adversaries?.find(a => String(a.id) === targetId);
        if (!target) throw new Error('Alvo do encontro não encontrado.');
        const bands = ['Interação','Curto','Médio','Longo','Extremo'];
        const current = bands.indexOf(String(target.range ?? ''));
        if (current < 0) throw new Error('Faixa de alcance atual inválida.');
        const step = direction === 'farther' ? 1 : direction === 'closer' ? -1 : 0;
        if (!step) throw new Error('Direção de movimento inválida.');
        const nextIndex = Math.max(0, Math.min(bands.length - 1, current + step));
        if (nextIndex === current) throw new Error(direction === 'closer' ? 'O alvo já está em Interação.' : 'O alvo já está em alcance Extremo.');
        const from = bands[current];
        const to = bands[nextIndex];
        const prior = combat0.playerRangeMovement?.[targetId] ?? null;
        const baseRequiredManeuvers = personalRangeTransitionManeuvers(from, to);
        const swift=talents_mjs_1.talentRankByOriginal(character,'SWIFT')>0||talents_mjs_1.talentRankByOriginal(character,'VELOZ')>0;
        let environment=environment_mjs_1.normalizeEnvironmentRuntime(this.state.environment);
        const environmentMovement=environment_mjs_1.movementEnvironmentStatus(environment,{swift});
        const continues = prior && prior.from === from && prior.to === to && prior.direction === direction && Number(prior.remainingManeuvers ?? 0) > 0;
        if(environmentMovement.blocked&&!continues) throw new Error('Terreno intransponível: supere-o primeiro com Atletismo ou Coordenação.');
        if(environment.terrain==='impassable'&&!continues) environment=environment_mjs_1.consumeImpassableClearance(environment);
        const requiredManeuvers = baseRequiredManeuvers * environmentMovement.multiplier;
        const movement = spendPlayerManeuver(character, combat0, { method: maneuverMethod, label: direction === 'closer' ? 'Aproximar-se' : 'Afastar-se' });
        character=clone(movement.character); if(character.combatEffects){delete character.combatEffects.coverRangedDefense;if(!Object.keys(character.combatEffects).length)delete character.combatEffects;}
        const strainCost = movement.strainCost;
        const critical = movement.critical;
        const burden = movement.encumbrance;
        let combat = clearHoloParkTransportOnMovement(clearHoloParkCoruscantCover(clone(movement.combat)));
        combat.playerRangeMovement = {...(combat.playerRangeMovement??{})};
        const maneuversRemaining = requiredManeuvers === 1 ? 0 : continues ? Math.max(0, Number(prior.remainingManeuvers) - 1) : requiredManeuvers - 1;
        const movementComplete = maneuversRemaining === 0;
        const updated = clone(target);
        if (movementComplete) {
            updated.range = to;
            combat = encounter_mjs_1.updateAdversaryInCombat(combat, updated);
            delete combat.playerRangeMovement[targetId];
            combat.log.push({ type:'MOVIMENTO_FAIXA', round:combat.round, targetId, from, to, direction, requiredManeuvers, strainCost });
        } else {
            combat.playerRangeMovement[targetId] = { targetId, from, to, direction, requiredManeuvers, remainingManeuvers:maneuversRemaining };
            combat.log.push({ type:'MOVIMENTO_FAIXA_PROGRESSO', round:combat.round, targetId, from, to, direction, requiredManeuvers, remainingManeuvers:maneuversRemaining, strainCost });
        }
        if (!Object.keys(combat.playerRangeMovement).length) delete combat.playerRangeMovement;
        this.commit('COMBAT_ACTION', { combat, character, environment });
        return { combat, character, environment, target:updated, from, to, movementComplete, requiredManeuvers, baseRequiredManeuvers, maneuversRemaining, strainCost, critical, encumbrance:burden, environmentMovement };
    }
    takeCover({ maneuverMethod = 'normal' } = {}) {
        let character = requireCharacter(this.state.player.character);
        assertCanAct(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const status = statusEffects_mjs_1.statusModifiers(character);
        if (status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
        const movement = spendPlayerManeuver(character, this.state.combat, { method:maneuverMethod, label:'Buscar cobertura' });
        character=clone(movement.character); character.combatEffects={...(character.combatEffects??{}),coverRangedDefense:1};
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character,coverRangedDefense:1};
    }
    takeGuardedStance({ maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        const status=statusEffects_mjs_1.statusModifiers(character); if(status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
        if(guardedStanceActive(character,this.state.combat?.round)) throw new Error('Postura defensiva já está ativa.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Postura defensiva'});
        character=clone(movement.character);
        character.combatEffects={...(character.combatEffects??{}),guardedStance:{appliedRound:Number(this.state.combat?.round??0),untilRound:Number(this.state.combat?.round??0)+1}};
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character,guardedStance:clone(character.combatEffects.guardedStance)};
    }
    activateFrenziedAttack({ strain = 1 } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        const combat=clone(this.state.combat);
        if(!combat.active) throw new Error('Ataque Frenético exige um encontro estruturado ativo.');
        if(combat.playerTurn?.pendingFrenziedAttack) throw new Error('Ataque Frenético já está preparado para o próximo ataque deste turno.');
        const critical=criticalActionState(character);
        if(critical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar Ataque Frenético.');
        const option=talents_mjs_1.frenziedAttackOption(character,strain);
        if(!option.available) throw new Error('Ataque Frenético não foi adquirido.');
        if(option.spend<1) throw new Error('Ataque Frenético exige sofrer ao menos 1 de Tensão.');
        character=health_mjs_1.sufferStrain(character,option.spend,{voluntary:true}).character;
        assertCanAct(character);
        combat.playerTurn={...(combat.playerTurn??{}),pendingFrenziedAttack:{strainCost:option.spend,abilityUpgrades:option.abilityUpgrades,rank:option.rank}};
        combat.log.push({type:'TALENTO_INCIDENTAL',round:combat.round,talent:'Ataque Frenético',strain:option.spend});
        this.commit('COMBAT_ACTION',{combat,character});
        return {combat,character,talent:'Ataque Frenético',...option,pending:true};
    }
    useDefensiveStanceTalent({ strain = 1, maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        if(this.state.combat?.playerTurn?.defensiveStanceTalentUsed) throw new Error('Postura Defensiva já foi usada neste turno.');
        const critical=criticalActionState(character);
        if(critical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar Postura Defensiva.');
        const option=talents_mjs_1.defensiveStanceOption(character,strain);
        if(!option.available) throw new Error('Postura Defensiva não foi adquirida.');
        if(option.spend<1) throw new Error('Postura Defensiva exige sofrer ao menos 1 de Tensão.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Postura Defensiva (talento)'});
        character=health_mjs_1.sufferStrain(movement.character,option.spend,{voluntary:true}).character;
        assertCanAct(character);
        const combat=clone(movement.combat);combat.playerTurn={...(combat.playerTurn??{}),defensiveStanceTalentUsed:true};
        character.combatEffects={...(character.combatEffects??{}),defensiveStanceTalent:{difficultyUpgrades:option.difficultyUpgrades,round:Number(combat.round??0)}};
        combat.log.push({type:'TALENTO_MANOBRA',round:combat.round,talent:'Postura Defensiva',strain:option.spend});
        this.commit('COMBAT_ACTION',{combat,character});
        return {...movement,combat,character,talent:'Postura Defensiva',...option};
    }
    useSideStep({ strain = 1, maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        if(this.state.combat?.playerTurn?.sideStepUsed) throw new Error('Passo Lateral já foi usado neste turno.');
        const critical=criticalActionState(character);
        if(critical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar Passo Lateral.');
        const option=talents_mjs_1.sideStepOption(character,strain);
        if(!option.available) throw new Error('Passo Lateral não foi adquirido.');
        if(option.spend<1) throw new Error('Passo Lateral exige sofrer ao menos 1 de Tensão.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Passo Lateral'});
        character=health_mjs_1.sufferStrain(movement.character,option.spend,{voluntary:true}).character;
        assertCanAct(character);
        const combat=clone(movement.combat);combat.playerTurn={...(combat.playerTurn??{}),sideStepUsed:true};
        character.combatEffects={...(character.combatEffects??{}),sideStep:{difficultyUpgrades:option.difficultyUpgrades,round:Number(combat.round??0)}};
        combat.log.push({type:'TALENTO_MANOBRA',round:combat.round,talent:'Passo Lateral',strain:option.spend});
        this.commit('COMBAT_ACTION',{combat,character});
        return {...movement,combat,character,talent:'Passo Lateral',...option};
    }
    useFeint({strain=1,maneuverMethod='normal'}={}){
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            assertCanAct(character);assertPlayerInitiativeTurn(this.state.combat);
            if(!this.state.combat?.active)throw new Error('Finta exige combate ativo.');
            if(this.state.combat.playerTurn?.feintUsed)throw new Error('Finta já foi usada nesta rodada.');
            if(criticalActionState(character).cannotVoluntarilySufferStrain)throw new Error('Ferimento Crítico impede Tensão voluntária para Finta.');
            const option=talents_mjs_1.feintTalentOption(character,strain);
            if(!option.available)throw new Error('Finta não foi adquirida.');
            if(option.spend<1)throw new Error('Finta exige ao menos 1 de Tensão.');
            const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Finta (talento)'});
            character=health_mjs_1.sufferStrain(movement.character,option.spend,{voluntary:true}).character;
            assertCanAct(character);
            const combat=clone(movement.combat);
            combat.playerTurn={...(combat.playerTurn??{}),feintUsed:true};
            character.combatEffects={...(character.combatEffects??{}),feint:{difficultyUpgrades:option.difficultyUpgrades,round:Number(combat.round??0)}};
            combat.log.push({type:'TALENTO_MANOBRA',round:combat.round,talent:'Finta',strain:option.spend});
            this.commit('COMBAT_ACTION',{combat,character});
            return {combat,character,talent:'Finta',...option};
        });
    }
    useSecondWind() {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        const combat=clone(this.state.combat);
        if(!combat.active) throw new Error('Segundo Fôlego exige um encontro ativo.');
        const option=talents_mjs_1.secondWindOption(character);
        if(!option.available) throw new Error('Segundo Fôlego não foi adquirido.');
        if(combat.secondWindUsed) throw new Error('Segundo Fôlego já foi usado neste encontro.');
        const recovered=health_mjs_1.recoverStrain(character,option.recover);
        character=recovered.character;combat.secondWindUsed=true;
        combat.log.push({type:'TALENTO_INCIDENTAL',round:combat.round,talent:'Segundo Fôlego',recovered:recovered.recovered});
        this.commit('COMBAT_ACTION',{combat,character});
        return {combat,character,talent:'Segundo Fôlego',recovered:recovered.recovered,rank:option.rank};
    }
    prepareDodge({ strain = 1 } = {}) {
        let character=requireCharacter(this.state.player.character);
        assertCanAct(character);
        if(!this.state.combat?.active) throw new Error('Esquiva exige combate ativo.');
        const option=talents_mjs_1.dodgeOption(character,strain);
        if(!option.available||option.spend<1) throw new Error('Esquiva não foi adquirida ou exige ao menos 1 de Tensão.');
        character=health_mjs_1.sufferStrain(character,option.spend,{voluntary:true}).character;
        character.combatEffects={...(character.combatEffects??{}),dodgeNextIncoming:{difficultyUpgrades:option.difficultyUpgrades,preparedAtRound:Number(this.state.combat?.round??0)}};
        this.commit('CHARACTER_UPDATED',{character});
        return {character:clone(character),...option};
    }
    usePreciseAim({ strain = 1, maneuverMethod = 'normal' } = {}) {
        let character=requireCharacter(this.state.player.character),combat=this.state.combat;
        const option=talents_mjs_1.preciseAimOption(character,strain);
        if(!option.available||option.spend<1) throw new Error('Mira Precisa não foi adquirida ou exige ao menos 1 de Tensão.');
        if(combat?.pendingPreciseAim) throw new Error('Mira Precisa já está preparada para o próximo ataque.');
        const spent=spendPlayerManeuver(character,combat,{method:maneuverMethod,label:'Mira Precisa'});
        character=health_mjs_1.sufferStrain(spent.character,option.spend,{voluntary:true}).character;
        combat={...spent.combat,pendingPreciseAim:{defenseReduction:option.defenseReduction,strain:option.spend}};
        this.commit('COMBAT_ACTION',{combat,character});
        return {combat:clone(combat),character:clone(character),...option};
    }
    useSniperShot({ bands = 1, maneuverMethod = 'normal' } = {}) {
        let character=requireCharacter(this.state.player.character),combat=this.state.combat;
        const option=talents_mjs_1.sniperShotOption(character,bands);
        if(!option.available||option.bands<1) throw new Error('Tiro de Precisão não foi adquirido ou exige ao menos 1 banda.');
        if(combat?.pendingSniperShot) throw new Error('Tiro de Precisão já está preparado para o próximo ataque.');
        const spent=spendPlayerManeuver(character,combat,{method:maneuverMethod,label:'Tiro de Precisão'});
        combat={...spent.combat,pendingSniperShot:{bands:option.bands,difficultyUpgrades:option.difficultyUpgrades}};
        this.commit('COMBAT_ACTION',{combat,character:spent.character});
        return {combat:clone(combat),character:clone(spent.character),...option};
    }
    prepareTargetedBlow() {
        const character=requireCharacter(this.state.player.character);assertCanAct(character);
        if(!talents_mjs_1.targetedBlowOption(character).available) throw new Error('Golpe Direcionado não foi adquirido.');
        if(!this.state.combat?.active) throw new Error('Golpe Direcionado exige combate ativo.');
        const combat={...clone(this.state.combat),pendingDestinyDamageTalent:'targetedBlow'};
        this.commit('COMBAT_UPDATED',{combat});return clone(combat);
    }
    prepareAnatomyLessons() {
        const character=requireCharacter(this.state.player.character);assertCanAct(character);
        if(!talents_mjs_1.anatomyLessonsOption(character).available) throw new Error('Anatomia não foi adquirida.');
        if(!this.state.combat?.active) throw new Error('Anatomia exige combate ativo.');
        const combat={...clone(this.state.combat),pendingDestinyDamageTalent:'anatomyLessons'};
        this.commit('COMBAT_UPDATED',{combat});return clone(combat);
    }
    dropProne({ maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        if(statusEffects_mjs_1.statusModifiers(character).prone) throw new Error('O personagem já está caído.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Ficar caído'});
        character=statusEffects_mjs_1.applyStatusEffect(movement.character,{type:'prone',duration:'encounter',source:'maneuver',sourceId:'player-prone'});
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character,prone:true};
    }
    standUpFromProne({ maneuverMethod = 'normal', useJumpUp = false } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        if(!statusEffects_mjs_1.statusModifiers(character).prone) throw new Error('O personagem não está caído.');
        if (useJumpUp) {
            if (talents_mjs_1.talentRankByOriginal(character,'JUMP') < 1) throw new Error('Levantar Rápido não foi adquirido.');
            if (!this.state.combat?.active) throw new Error('Levantar Rápido exige um turno estruturado ativo.');
            if (this.state.combat.playerTurn?.jumpUpUsed) throw new Error('Levantar Rápido já foi usado neste turno.');
            const combat=clone(this.state.combat); combat.playerTurn={...(combat.playerTurn??{}),jumpUpUsed:true};
            combat.log.push({type:'TALENTO_INCIDENTAL',round:combat.round,talent:'Levantar Rápido'});
            character=statusEffects_mjs_1.removeStatusEffect(character,{type:'prone'});
            this.commit('COMBAT_ACTION',{combat,character});
            return {combat,character,strainCost:0,incidental:true,talent:'Levantar Rápido'};
        }
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:'Levantar-se'});
        character=statusEffects_mjs_1.removeStatusEffect(movement.character,{type:'prone'});
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character};
    }
    drawWeaponInCombat(weaponId,{ maneuverMethod = 'normal', useQuickDraw = false } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        let combat=clone(this.state.combat), movement=null, incidental=false;
        if(useQuickDraw){
            const option=talents_mjs_1.quickDrawOption(character);
            if(!option.available) throw new Error('Saque Rápido não foi adquirido.');
            if(!combat.active) throw new Error('Saque Rápido exige um turno estruturado ativo.');
            if(combat.playerTurn?.quickDrawUsed) throw new Error('Saque Rápido já foi usado neste turno.');
            combat.playerTurn={...(combat.playerTurn??{}),quickDrawUsed:true};
            incidental=true;
        } else {
            const status=statusEffects_mjs_1.statusModifiers(character); if(status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
            movement=spendPlayerManeuver(character,combat,{method:maneuverMethod,label:'Sacar / guardar equipamento'});
            combat=movement.combat;character=clone(movement.character);
        }
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        const weapon=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        if(!weapon) throw new Error(`Arma "${weaponId}" não encontrada no inventário.`);
        assertOwnedEquipmentOperational(this.state,weapon);
        for(const w of character.weapons??[]){w.active=false;w.equipped=false;} weapon.active=true;weapon.equipped=true;character.activeWeaponId=weapon.id??weapon.name;
        if(incidental) combat.log.push({type:'TALENTO_INCIDENTAL',round:combat.round,talent:'Saque Rápido',weapon:weapon.name});
        this.commit('COMBAT_ACTION',{combat,character});
        return {combat,character,activeWeapon:clone(weapon),incidental,talent:incidental?'Saque Rápido':null,strainCost:movement?.strainCost??0,critical:movement?.critical??null,encumbrance:movement?.encumbrance??encumbrance_mjs_1.encumbranceStatus(character)};
    }
    prepareWeaponForAttack(weaponId,{ maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        const status=statusEffects_mjs_1.statusModifiers(character); if(status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        const weapon=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        if(!weapon) throw new Error(`Arma "${weaponId}" não encontrada no inventário.`);
        assertOwnedEquipmentOperational(this.state,weapon);
        const required=weapons_mjs_1.weaponQualityRank(weapon,'prepare');
        if(required<=0) throw new Error('A arma selecionada não possui Preparação.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:`Preparar ${weapon.name??'arma'}`});
        character=clone(movement.character);
        const updated=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        updated.runtimeState={...(updated.runtimeState??{}),preparedManeuvers:Math.min(required,Math.max(0,Number(updated.runtimeState?.preparedManeuvers??0))+1)};
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character,weapon:clone(updated),preparedManeuvers:updated.runtimeState.preparedManeuvers,required};
    }
    reloadLimitedAmmoWeapon(weaponId,{ maneuverMethod = 'normal' } = {}) {
        let character=clone(requireCharacter(this.state.player.character));
        assertCanAct(character); assertPlayerInitiativeTurn(this.state.combat);
        const status=statusEffects_mjs_1.statusModifiers(character); if(status.cannotManeuver) throw new Error('O personagem está immobilized e não pode realizar manobras agora.');
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        const weapon=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        if(!weapon) throw new Error(`Arma "${weaponId}" não encontrada no inventário.`);
        assertOwnedEquipmentOperational(this.state,weapon);
        const capacity=weapons_mjs_1.weaponQualityRank(weapon,'limitedAmmo');
        if(capacity<=0 && !weapon.runtimeState?.outOfAmmo) throw new Error('A arma não está sem munição e não possui Munição Limitada.');
        const ammoIndex=(character.gear??[]).findIndex(g=>String(g?.id??g?.sourceCatalogId??'')==='power_pack_or_ammo_reload'||/recarga adicional|power pack|ammo reload/i.test(String(g?.name??'')));
        if(ammoIndex<0) throw new Error('É necessária uma Recarga adicional realmente possuída para recarregar esta arma.');
        const movement=spendPlayerManeuver(character,this.state.combat,{method:maneuverMethod,label:`Recarregar ${weapon.name??'arma'}`});
        character=clone(movement.character);
        const updated=(character.weapons??[]).find((w,idx)=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target||String(idx)===target);
        updated.runtimeState={...(updated.runtimeState??{}),limitedAmmoRemaining:capacity,outOfAmmo:false};
        const gear=character.gear[ammoIndex],qty=Math.max(1,Number(gear?.quantity??1));
        if(qty>1) gear.quantity=qty-1; else character.gear.splice(ammoIndex,1);
        this.commit('COMBAT_ACTION',{combat:movement.combat,character});
        return {...movement,character,weapon:clone(updated),limitedAmmoRemaining:capacity,outOfAmmo:false,reloadConsumed:true};
    }
    /** P160: GM explicitly adjudicates a Despair from the last personal attack.
     * Spare Clip prevents depletion from Despair, but never exempts Limited Ammo.
     * The receipt is tied to the persisted attack ID and survives WebView restarts.
     */
    adjudicateAttackDespairAmmoLoss({weaponIndex=0}={}) {
        return this.atomicAction(() => {
            const combat=clone(this.state.combat),character=clone(requireCharacter(this.state.player.character));
            const index=Number(weaponIndex),attack=combat.lastAttack;
            if(!combat.active)throw new Error('O Desespero de munição exige encontro ativo.');
            if(attack?.attackRound!==combat.round)throw new Error('O Desespero deve ser adjudicado na rodada do ataque.');
            if(!Number.isSafeInteger(index)||index<0||!character.weapons?.[index])throw new Error('Índice de arma inválido.');
            if(!attack?.attackId||attack.weaponIndex!==index||!Number.isSafeInteger(attack.check?.despair)||attack.check.despair<1)
                throw new Error('Não há Desespero de ataque válido para esta arma.');
            if(combat.ammoDespairAppliedAttackId===attack.attackId)throw new Error('Desespero de munição já foi adjudicado neste ataque.');
            const weapon=character.weapons[index];
            if(String(attack.weapon?.id??attack.weapon?.name)!==String(weapon.id??weapon.name))
                throw new Error('A arma mudou desde o ataque registrado.');
            const limited=weapons_mjs_1.weaponQualityRank(weapon,'limitedAmmo');
            if(limited>0)throw new Error('Munição Limitada é consumida normalmente; não se aplica perda adicional por Desespero.');
            if(talents_mjs_1.sourceBackedTalentRank(character,'SPARECL')>0){
                combat.ammoDespairAppliedAttackId=attack.attackId;
                combat.log.push({type:'MUNICAO_RESERVA',attackId:attack.attackId,weaponIndex:index,round:combat.round});
                this.commit('COMBAT_ACTION',{combat,character});
                return {prevented:true,talent:'Munição de reserva',attackId:attack.attackId,ammoLost:false};
            }
            if(weapon.runtimeState?.outOfAmmo)throw new Error('A arma já está sem munição.');
            weapon.runtimeState={...(weapon.runtimeState??{}),outOfAmmo:true};
            combat.ammoDespairAppliedAttackId=attack.attackId;
            combat.log.push({type:'DESESPERO_MUNICAO',attackId:attack.attackId,weaponIndex:index,round:combat.round});
            this.commit('COMBAT_ACTION',{combat,character});
            return {prevented:false,attackId:attack.attackId,ammoLost:true,weapon:clone(weapon)};
        });
    }
    /** P160: spend one light Destiny as an incidental to suppress ongoing
     * Critical Injury check penalties on Brawn/Agility for this encounter.
     */
    activateHeroicFortitude() {
        return this.atomicAction(() => {
            let character=clone(requireCharacter(this.state.player.character));
            if(!this.state.combat?.active)throw new Error('Fibra de herói exige encontro ativo.');
            assertCanAct(character);assertPlayerInitiativeTurn(this.state.combat);
            if(talents_mjs_1.sourceBackedTalentRank(character,'HERFORT')<1)
                throw new Error('Fibra de herói não foi adquirida.');
            if(character.combatEffects?.heroicFortitude)throw new Error('Fibra de herói já está ativa neste encontro.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            character.combatEffects={...(character.combatEffects??{}),heroicFortitude:true};
            const combat=clone(this.state.combat);
            combat.log.push({type:'FIBRA_HEROI',round:combat.round,costDestiny:1});
            this.commit('COMBAT_ACTION',{character,group,combat});
            return {active:true,costDestiny:1,character:clone(character),group:clone(group)};
        });
    }
    /** P161: assign an existing AoR Knowledge Specialization to its official skill. */
    assignKnowledgeSpecialization({nodeId,skillKey}={}) {
        return this.atomicAction(()=>{
            const result=talents_mjs_1.bindKnowledgeSpecialization(requireCharacter(this.state.player.character),{nodeId,skillKey});
            this.commit('CHARACTER_UPDATED',{character:result.character});
            return {nodeId:result.nodeId,skillKey:result.skillKey};
        });
    }
    /** P162: bind a purchased legacy Lábia node without spending XP. */
    assignSmoothTalker({nodeId,skillKey}={}){
        return this.atomicAction(()=>{
            const result=talents_mjs_1.bindSmoothTalker(requireCharacter(this.state.player.character),{nodeId,skillKey});
            this.commit('CHARACTER_UPDATED',{character:result.character});
            return {nodeId:result.nodeId,skillKey:result.skillKey};
        });
    }
    /** P167 Mental Fortress: one light Destiny point suppresses ongoing
     * critical-injury penalties only for Intellect/Cunning checks. The injury
     * itself and action restrictions remain; effect ends with the encounter. */
    activateMentalFortress() {
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            if(!this.state.combat?.active)throw new Error('Fortaleza mental exige encontro ativo.');
            assertCanAct(character);assertPlayerInitiativeTurn(this.state.combat);
            if(talents_mjs_1.sourceBackedTalentRank(character,'MENTALFORT')<1)
                throw new Error('Fortaleza mental não foi adquirida.');
            if(character.combatEffects?.mentalFortress)throw new Error('Fortaleza mental já está ativa neste encontro.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            character.combatEffects={...(character.combatEffects??{}),mentalFortress:true};
            const combat=clone(this.state.combat);
            combat.log.push({type:'FORTALEZA_MENTAL',round:combat.round,costDestiny:1});
            this.commit('COMBAT_ACTION',{character,group,combat});
            return {active:true,costDestiny:1};
        });
    }
    /** P161 — Steely Nerves: Destiny flip, only Willpower/Presence CHECK penalties. */
    activateSteelyNerves() {
        return this.atomicAction(() => {
            let character=clone(requireCharacter(this.state.player.character));
            if(!this.state.combat?.active)throw new Error('Nervos de aço exige encontro ativo.');
            assertCanAct(character);assertPlayerInitiativeTurn(this.state.combat);
            if(talents_mjs_1.sourceBackedTalentRank(character,'STEELYNERVES')<1)
                throw new Error('Nervos de aço não foi adquirido.');
            if(character.combatEffects?.steelyNerves)throw new Error('Nervos de aço já está ativo neste encontro.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            character.combatEffects={...(character.combatEffects??{}),steelyNerves:true};
            const combat=clone(this.state.combat);
            combat.log.push({type:'NERVOS_DE_ACO',round:combat.round,costDestiny:1});
            this.commit('COMBAT_ACTION',{character,group,combat});
            return {active:true,costDestiny:1};
        });
    }
    /** P161 — Let's Ride: board/dismount only an actual selected vehicle,
     * once per round as incidental when the source talent is purchased. */
    boardActiveVehicle({station='cockpit',dismount=false,method='normal'}={}) {
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            assertCanAct(character);
            const vehicle=this.activeVehicle();
            if(!vehicle)throw new Error('Selecione um veículo real antes de embarcar.');
            if(!['cockpit','weapon_station','passenger'].includes(station))throw new Error('Estação de embarque inválida.');
            if(typeof dismount!=='boolean')throw new Error('Desembarque inválido.');
            let combat=clone(this.state.combat);
            if(combat?.active)assertPlayerInitiativeTurn(combat);
            const prior=character.vehicleBoarding??null;
            if(dismount){
                if(!prior||prior.vehicleId!==vehicle.id)throw new Error('Personagem não está embarcado neste veículo.');
            } else if(prior)throw new Error('Personagem já está embarcado.');
            const talent=talents_mjs_1.sourceBackedTalentRank(character,'LETSRIDE')>0;
            const round=Number(combat?.round??0);
            const sessionKey=String(this.state.sessionId??'local');
            const encounterNumber=(combat?.log??[]).filter(e=>e?.type==='COMBATE_INICIADO').length;
            const stamp=`${sessionKey}:${encounterNumber}:${round}`;
            if(talent&&combat?.active&&character.lastQuickBoardingStamp===stamp)
                throw new Error('Embarque rápido já foi utilizado nesta rodada.');
            let incidental=false;
            if(talent&&combat?.active){incidental=true;character.lastQuickBoardingStamp=stamp;}
            else if(combat?.active){
                const movement=spendPlayerManeuver(character,combat,{method,label:dismount?'Desembarcar':'Embarcar'});
                character=movement.character;combat=movement.combat;
            }
            character.vehicleBoarding=dismount?null:{vehicleId:vehicle.id,station};
            if(combat?.active)combat.log.push({type:'EMBARQUE',round,vehicleId:vehicle.id,station,dismount,incidental});
            this.commit('COMBAT_ACTION',{character,combat});
            return {vehicleId:vehicle.id,station,dismount,incidental,boarding:clone(character.vehicleBoarding)};
        });
    }
    toggleWeaponStunSetting(weaponId,{ enabled = null } = {}) {
        const character=clone(requireCharacter(this.state.player.character));
        const target=String(weaponId??'').trim().toLocaleLowerCase('pt-BR');
        const index=(character.weapons??[]).findIndex(w=>String(w?.id??'').toLocaleLowerCase('pt-BR')===target||String(w?.name??'').toLocaleLowerCase('pt-BR')===target);
        if(index<0) throw new Error('Arma não encontrada.');
        const weapon=character.weapons[index];
        if(!weapons_mjs_1.weaponHasQuality(weapon,'stunSetting')) throw new Error('Esta arma não possui Modo atordoante.');
        const next=clone(weapon); next.runtimeState={...(next.runtimeState??{})};
        next.runtimeState.stunSettingActive=enabled===null?!Boolean(next.runtimeState.stunSettingActive):Boolean(enabled);
        character.weapons[index]=next; this.commit('CHARACTER_UPDATED',{character});
        return {weapon:clone(next),stunSettingActive:Boolean(next.runtimeState.stunSettingActive)};
    }
    resolveSuggestedMasterCheck({ rng = Math.random } = {}) {
        return this.atomicAction(() => {
        const suggested = this.state.master?.suggestedCheck;
        if (!suggested)
            throw new Error('O Mestre não sugeriu nenhum teste agora.');
        const check = this.resolveSkillCheck({ ...suggested, rng });
        const next = structuredClone(this.state);
        next.master.suggestedCheck = null;
        this.state = next;
        // Outer atomicAction persists the check and clears the suggestion together.
        return check;
        });
    }
    // Explicitly account for an already resolved check; never charge time on
    // dice resolution or translate minutes into unrelated world event ticks.
    commitLastTaskTime({ checkId } = {}) {
        const check = this.state.lastCheck;
        validateTaskTimeLedger(this.state.world);
        const ledger = this.state.world?.taskTimeApplications ?? {};
        if (typeof checkId !== 'string' || !/^task_[a-zA-Z0-9_-]{1,120}$/.test(checkId))
            throw new Error('Identificador de tarefa inválido.');
        // A retry of a committed action remains harmless even after other rolls.
        const prior = Object.prototype.hasOwnProperty.call(ledger, checkId) ? ledger[checkId] : null;
        if (prior) return { applied: false, elapsedMinutes: this.state.world.clock.elapsedMinutes,
            record: structuredClone(prior) };
        if (!check || check.taskCheckId !== checkId || check.taskTime?.durationMinutes === null ||
            check.taskTime?.durationMinutes === undefined)
            throw new Error('Tarefa não corresponde ao último teste resolvido.');
        const result = recordResolvedTaskTime(this.state.world, {
            checkId, skillKey: check.taskSkillKey,
            minutes: check.taskTime.durationMinutes,
            baseMinutes: check.taskBaseMinutes
        });
        // UAE and world time are prepared in memory first. If the UAE rejects
        // the event, neither the world clock nor its ledger is persisted.
        const world = recordPublishedAdventureTaskTime(result.world, result.record);
        this.commit('WORLD_UPDATED', { world });
        return { applied: true, elapsedMinutes: result.elapsedMinutes, record: result.record };
    }

    /** P168 Intense Presence: an actual light Destiny flip, real strain
     * recovery based on Presence, and one atomic persisted state. */
    useIntensePresence(){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            if(talents_mjs_1.sourceBackedTalentRank(character,'INTENSEPRES')<1)
                throw new Error('Presença imponente não foi adquirida.');
            assertCanAct(character);
            const presence=Number(character.characteristics?.presencia??0);
            if(!Number.isSafeInteger(presence)||presence<1||presence>7)throw new Error('Presença inválida.');
            if(Number(character.derived?.strain?.current??0)<1)throw new Error('Não há Tensão a recuperar.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            const recovered=health_mjs_1.recoverStrain(character,presence);
            this.commit('COMBAT_ACTION',{group,character:recovered.character});
            return {recovered:recovered.recovered,costDestiny:1};
        });
    }
    /** P168: real AoR Diplomat action, with source-backed ownership, a single
     * skill roll, real combat action/maneuver cost, and durable target effects.
     * Explicitly selected companions are confirmed short-range by the player;
     * enemies are checked against their current combat range. */
    aorSocialStatus(){
        const c=requireCharacter(this.state.player.character),rank=k=>talents_mjs_1.sourceBackedTalentRank(c,k);
        return {intensePresence:{available:rank('INTENSEPRES')>0,strain:Number(c.derived?.strain?.current??0)},inspiring:aorSocial_mjs_1.aorSocialTalentStatus(c,{base:rank('INSPIRINGRHET'),improved:rank('INSPIRINGRHETIMP'),supreme:rank('INSPIRINGRHETSUP')}),
            scathing:aorSocial_mjs_1.aorSocialTalentStatus(c,{base:rank('SCATHINGTIRADE'),improved:rank('SCATHINGTIRADEIMP'),supreme:rank('SCATHINGTIRADESUP')}),
            companions:(this.state.group?.companions??[]).map(x=>({id:x.id,name:x.character?.name??x.id,strain:Number(x.character?.derived?.strain?.current??0),inspiringBoost:aorSocial_mjs_1.activeInspiringBoost(x.character,this.state.combat?.active?this.state.combat.round:0,this.state.combat?.socialEncounterSerial)})),
            enemies:(this.state.combat?.adversaries??[]).map(x=>({id:x.id,name:x.name,range:x.range,eligible:aorSocial_mjs_1.isShortRange(x.range)&&!adversaries_mjs_1.adversaryHealthStatus(x).incapacitated,scathingSetback:aorSocial_mjs_1.activeScathingSetback(x,this.state.combat?.round??0)})),
            combatActive:Boolean(this.state.combat?.active)};
    }
    performAoRSocialTalent({kind,targetIds=[],advantageTargetId=null,asManeuver=false,maneuverMethod='normal',confirmedShortRange=false,rng=Math.random}={}){
        return this.atomicAction(()=>{
            if(!['inspiring','scathing'].includes(kind))throw new Error('Talento social AoR inválido.');
            const character=requireCharacter(this.state.player.character);
            const base=kind==='inspiring'?'INSPIRINGRHET':'SCATHINGTIRADE';
            const improved=kind==='inspiring'?'INSPIRINGRHETIMP':'SCATHINGTIRADEIMP';
            const supreme=kind==='inspiring'?'INSPIRINGRHETSUP':'SCATHINGTIRADESUP';
            if(talents_mjs_1.sourceBackedTalentRank(character,base)<1)throw new Error('Talento social não adquirido na árvore AoR.');
            if(asManeuver&&talents_mjs_1.sourceBackedTalentRank(character,supreme)<1)throw new Error('Versão suprema não adquirida.');
            if(typeof asManeuver!=='boolean'||typeof confirmedShortRange!=='boolean')throw new Error('Parâmetros sociais inválidos.');
            if(!this.state.combat?.active)throw new Error('Retórica e reprimenda exigem encontro ativo.');
            assertPlayerInitiativeTurn(this.state.combat);
            if(!Array.isArray(targetIds)||!targetIds.length||targetIds.length>8||
               targetIds.some(id=>typeof id!=='string'||!id)||new Set(targetIds).size!==targetIds.length)
                throw new Error('Selecione de um a oito alvos distintos.');
            const candidates=kind==='inspiring'?(this.state.group?.companions??[]):this.state.combat.adversaries;
            for(const id of targetIds){
                const target=candidates.find(x=>x.id===id);
                if(!target)throw new Error('Alvo social inexistente.');
                if(kind==='inspiring'){
                    if(!confirmedShortRange)throw new Error('Confirme que os companheiros estão em alcance Curto.');
                    if(health_mjs_1.getHealthStatus(target.character).unconscious)throw new Error('Companheiro incapacitado não pode ser inspirado.');
                }else if(!aorSocial_mjs_1.isShortRange(target.range)||adversaries_mjs_1.adversaryHealthStatus(target).incapacitated)
                    throw new Error('Adversário fora do alcance Curto ou incapacitado.');
            }
            if(advantageTargetId!==null&&!targetIds.includes(advantageTargetId))throw new Error('Alvo de Vantagens não selecionado.');
            const skillKey=kind==='inspiring'?'liderazgo':'coaccion';
            const check=this.resolveSkillCheck({skillKey,difficulty:2,socialTalentManeuver:asManeuver?kind:null,socialManeuverMethod:maneuverMethod,rng});
            const successes=Math.max(0,Math.trunc(Number(check.netSuccess??0)));
            const affected=targetIds.slice(0,successes);
            const advantage=Math.max(0,Math.trunc(Number(check.netAdvantage??0)));
            const chosen=affected.includes(advantageTargetId)?advantageTargetId:affected[0]??null;
            const duration=talents_mjs_1.sourceBackedTalentRank(character,improved)>0 ?
                Math.max(0,Number(character.skills?.[skillKey]??0)) : 0;
            const untilRound=duration>0?Number(this.state.combat.round)+duration-1:null;
            const results=[];
            if(kind==='inspiring'){
                const group=clone(this.state.group);
                for(const id of affected){
                    const index=group.companions.findIndex(x=>x.id===id);
                    const result=aorSocial_mjs_1.applyInspiringRhetoric(group.companions[index],{strainRecovered:1+(id===chosen?advantage:0),untilRound,encounterSerial:this.state.combat.socialEncounterSerial});
                    group.companions[index]=result.member;
                    results.push({id,recovered:result.recovered,boostUntilRound:untilRound});
                }
                this.commit('GROUP_UPDATED',{group});
            }else{
                const combat=clone(this.state.combat);
                for(const id of affected){
                    const index=combat.adversaries.findIndex(x=>x.id===id);
                    const result=aorSocial_mjs_1.applyScathingTirade(combat.adversaries[index],{strain:1+(id===chosen?advantage:0),untilRound});
                    combat.adversaries[index]=result.adversary;
                    results.push({id,strain:result.amount,convertedToWounds:result.convertedToWounds,setbackUntilRound:untilRound});
                }
                combat.log.push({type:'LÍNGUA_AFILADA',round:combat.round,check:{netSuccess:check.netSuccess,netAdvantage:check.netAdvantage},affected:clone(results)});
                this.commit('COMBAT_UPDATED',{combat});
            }
            return {kind,check,asManeuver,affected:results,advantageSpent:chosen?advantage:0,round:this.state.combat.round};
        });
    }
    /** Companion skill rolls consume an active Inspiring Rhetoric bonus;
     * they do not grant invented characteristics or NPC turn slots. */
    resolveCompanionSkillCheck({companionId,skillKey,difficulty=2,situationalRange=null,confirmedNearVehicle=false,rng=Math.random}={}){
        return this.atomicAction(()=>{
            const member=this._medicalCompanion(companionId);
            if(!member)throw new Error('Companheiro não encontrado.');
            const attribute=character_mjs_1.SKILLS[skillKey];
            if(!attribute||!Number.isSafeInteger(difficulty)||difficulty<0||difficulty>5)
                throw new Error('Teste de companheiro inválido.');
            let situationalBoost=0,sourceVehicleId=null;
            if(situationalRange!==null){
                if(!['short','close'].includes(situationalRange)||confirmedNearVehicle!==true||
                   !['percepcion','alerta'].includes(skillKey)||
                   talents_mjs_1.sourceBackedTalentRank(this.state.player?.character,'SITUATIONAL_AWARENESS')<1)
                    throw new Error('Conhecimento do ambiente exige talento, alcance confirmado e Percepção/Vigilância.');
                sourceVehicleId=this.state.vehicles?.activeVehicleId;
                if(!sourceVehicleId||!this.state.vehicles?.vehicles?.[sourceVehicleId])
                    throw new Error('Conhecimento do ambiente exige veículo aliado registrado.');
                situationalBoost=situationalRange==='close'?2:1;
            }
            const boost=aorSocial_mjs_1.activeInspiringBoost(member.character,this.state.combat?.active?this.state.combat.round:0,this.state.combat?.socialEncounterSerial)+situationalBoost;
            const positive=narrativeDice_mjs_1.buildPositivePool(Number(member.character.characteristics?.[attribute]??0),Number(member.character.skills?.[skillKey]??0));
            const combatSkills=['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','armas_distancia_pesadas','artilleria'];
            const combat=clone(this.state.combat);
            const order=combat.aorCoordinatedAssault;
            const coordinated=Boolean(combat.active&&combatSkills.includes(skillKey)&&
                order?.round===combat.round&&order.companionIds.includes(companionId)&&
                !order.consumed.includes(companionId));
            const check={...narrativeDice_mjs_1.rollPool({...positive,difficulty,challenge:0,boost,
                setback:combatSkills.includes(skillKey)?holoParkTransportSetback(combat,companionId):0},rng),
                kind:'companion-skill',companionId,skillKey,inspiringBoost:boost-situationalBoost,
                situationalAwarenessBoost:situationalBoost,sourceVehicleId,
                coordinatedAssaultAdvantage:coordinated?1:0};
            if(coordinated){
                check.netAdvantage=Number(check.netAdvantage??0)+1;
                order.consumed.push(companionId);
                this.commit('COMBAT_UPDATED',{combat});
            }
            this.commit('ADVANCED_CHECK_RESOLVED',{type:'companion-skill',check,companionId,skillKey});
            return check;
        });
    }
    resolveSkillCheck({ skillKey, difficulty = 2, challenge = 0, boost = 0, setback = 0, braceEnvironmentalSetback=0, gearAssist = null, assistant = null, useExpertTalent = false, useIntenseFocus = false, intenseFocusManeuverMethod = 'normal', useSenseDanger = false, useTouchOfFate = false, useMasterOfShadows = false, useMasterLeader = false, useCleverSolution = false, useWorksLikeACharm = false, useStrokeOfGenius = false, useQuickFix = false, useMasterDoctor = false, useMasterSlicer = false, intimidatingStrain = 0, useNaturalReroll = false, useLightDestiny = false, usePlayerDestiny = false, useDarkDestiny = false, securityBypass = false, tracking = false, research = false, codebreaking = false, cloudCitySlicing = false, outdoors = false, searchingArea = false, chase = false, crafting = false, foraging = false, poisonResistance = false, scholarlyInstitution = false, terrainSetback = 0, taskDurationMinutes = null, spendKnowledgeTriumph = false, spendSmoothTalkerTriumph = false, spendSkilledSlicerTriumph = false, slicingSystemId = null, confirmedSlicingSystem = false, useSkilledSlicerManeuver = false, skilledSlicerManeuverMethod = 'normal', socialTalentManeuver = null, socialManeuverMethod = 'normal', greasedPalmsCredits = 0, greasedPalmsActionId = null, rng = Math.random }) {
        return this.atomicAction(() => {
        let character = this.state.player.character;
        // Validate time before any Destiny flip, combat action or strain cost.
        if(typeof foraging!=='boolean'||typeof poisonResistance!=='boolean')throw new Error('Contexto de Aclimatação/venenos inválido.');
        const taskTime = talents_mjs_1.talentTaskTimeSpec(character, skillKey, { tracking, research, outdoors, searchingArea, taskDurationMinutes });
        useLightDestiny = Boolean(useLightDestiny || usePlayerDestiny);
        let actionCritical = assertCanPerformAction(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const bracePending=clone(this.state.combat?.aorBracePrepared??null);
        let characteristicKey = character_mjs_1.SKILLS[skillKey];
        if (!characteristicKey || !(skillKey in character.skills))
            throw new Error('Perícia não disponível nesta ficha.');
        if(spendSkilledSlicerTriumph&&confirmedSlicingSystem!==true)throw Error('Mestre deve confirmar o sistema informático real.');
        if(spendSkilledSlicerTriumph||useSkilledSlicerManeuver){
            if(skillKey!=='computadoras'||!safeIntelId(slicingSystemId)||
               talents_mjs_1.sourceBackedTalentRank(character,'SKILSLI')<1)
              throw Error('Slicer consumado exige Computadores, sistema válido e talento comprado.');
        }
        if(useSkilledSlicerManeuver&&(!this.state.combat.active||
           this.state.combat.aorSkilledSlicer?.systemId!==slicingSystemId||
           this.state.combat.aorSkilledSlicer?.sessionId!==this.state.sessionId))
          throw Error('Manobra Slicer exige controle previamente obtido por Triunfo no mesmo sistema.');
        // P178: distinct AoR Core one-session characteristic substitutions.
        // They modify the ACTUAL positive dice pool, never a decorative label.
        const substitutions=[
          ['CLEVER_SOLUTION',useCleverSolution,'cleverSolution','astucia'],
          ['WORKS_LIKE_A_CHARM',useWorksLikeACharm,'worksLikeACharm','presencia'],
          ['STROKE_OF_GENIUS',useStrokeOfGenius,'strokeOfGenius','intelecto'],
          ['QUICK_FIX',useQuickFix,'quickFix','agilidad']
        ];
        if(substitutions.some(([,used])=>typeof used!=='boolean'))throw Error('Escolha de talento de atributo inválida.');
        const requestedSubstitutions=substitutions.filter(([,used])=>used);
        if(requestedSubstitutions.length>1)throw Error('Não é possível substituir uma característica duas vezes no mesmo teste.');
        const substitution=requestedSubstitutions[0]??null;
        if(substitution){
          const [canonical,,sessionKey,attribute]=substitution;
          if(talents_mjs_1.sourceBackedTalentRank(character,canonical)<1)
            throw Error('Talento de substituição de atributo não adquirido.');
          if(!['AOR','MIXED'].includes(campaignMode(this.state))||this.state.session?.status!=='ativa'||talentUsedThisSession(character,sessionKey))
            throw Error('Talento de atributo exige sessão AoR ativa e só pode ser usado uma vez.');
          characteristicKey=attribute;
        }
        let combat = this.state.combat;
        let greasedPalms=null;
        if(greasedPalmsCredits!==0){
          const rank=talents_mjs_1.sourceBackedTalentRank(character,'GREASED_PALMS');
          if(rank<1||!['carisma','coaccion','engano','liderazgo','negociacion'].includes(skillKey))
            throw Error('Untar mãos exige talento adquirido e perícia social.');
          if(!Number.isSafeInteger(greasedPalmsCredits)||greasedPalmsCredits<50||greasedPalmsCredits>50*rank||greasedPalmsCredits%50!==0)
            throw Error('Suborno excede 50 créditos por graduação.');
          if(!safeP177Id(greasedPalmsActionId)||(this.state.master?.aorP177Actions??[]).some(r=>r.actionId===greasedPalmsActionId))
            throw Error('Identificador de suborno inválido ou duplicado.');
          if(!['AOR','MIXED'].includes(campaignMode(this.state))||this.state.session?.status!=='ativa')
            throw Error('Suborno AoR exige sessão ativa.');
          if(Number(character.credits??0)<greasedPalmsCredits)throw Error('Créditos insuficientes para Untar mãos.');
          if(combat.active){const spent=spendPlayerManeuver(character,combat,{method:'normal',label:'Untar mãos'});combat=spent.combat;character=spent.character;}
          character=clone(character);character.credits-=greasedPalmsCredits;
          greasedPalms={upgrades:greasedPalmsCredits/50,costCredits:greasedPalmsCredits};
        }
        let intenseFocus = null;
        if (useIntenseFocus) {
            const option=talents_mjs_1.intenseFocusOption(character);
            if(!option.available) throw new Error('Foco Intenso não foi adquirido.');
            if(actionCritical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar Foco Intenso.');
            if(combat.active){const movement=spendPlayerManeuver(character,combat,{method:intenseFocusManeuverMethod,label:'Foco Intenso'});combat=movement.combat;character=movement.character;}
            character=health_mjs_1.sufferStrain(character,option.strainCost,{voluntary:true}).character;
            assertCanAct(character);
            intenseFocus={used:true,strainCost:option.strainCost,abilityUpgrades:option.abilityUpgrades};
            actionCritical=assertCanPerformAction(character);
        }
        const senseDanger=talents_mjs_1.senseDangerOption(character);
        const touchOfFate=talents_mjs_1.touchOfFateOption(character);
        if(useSenseDanger){
            if(!senseDanger.available) throw new Error('Sentir o Perigo não foi adquirido.');
            if(this.state.session?.status!=='ativa') throw new Error('Sentir o Perigo exige uma sessão ativa para controlar o uso único por sessão.');
            if(talentUsedThisSession(character,'senseDanger')) throw new Error('Sentir o Perigo já foi usado nesta sessão.');
        }
        if(useTouchOfFate){
            if(!touchOfFate.available) throw new Error('Toque do Destino não foi adquirido.');
            if(this.state.session?.status!=='ativa') throw new Error('Toque do Destino exige uma sessão ativa para controlar o uso único por sessão.');
            if(talentUsedThisSession(character,'touchOfFate')) throw new Error('Toque do Destino já foi usado nesta sessão.');
        }
        const naturalReroll=talents_mjs_1.naturalRerollOption(character,skillKey);
        if(useNaturalReroll){
            if(!naturalReroll.available) throw new Error('Nenhum talento Nato de rerrolagem adquirido se aplica a esta perícia.');
            if(this.state.session?.status!=='ativa') throw new Error(`${naturalReroll.label} exige uma sessão ativa para controlar o uso único por sessão.`);
            if(talentUsedThisSession(character,naturalReroll.sessionKey)) throw new Error(`${naturalReroll.label} já foi usado nesta sessão.`);
        }
        const activeSkillTalents=[];
        const requestedMasters=[['shadows',Boolean(useMasterOfShadows)],['doctor',Boolean(useMasterDoctor)],['slicer',Boolean(useMasterSlicer)]];
        let masterDifficultyReduction=0;
        for(const [kind,requested] of requestedMasters){
            if(!requested) continue;
            const spec=talents_mjs_1.masterSkillDifficultyOption(character,kind);
            if(!spec.available) throw new Error(`${spec.label??'Talento mestre'} não foi adquirido.`);
            if(!spec.skills.includes(skillKey)) throw new Error(`${spec.label} não se aplica à perícia ${skillKey}.`);
            if(actionCritical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar este talento.');
            if(combat.active&&combat.playerTurn?.skillMasterUses?.[kind]) throw new Error(`${spec.label} já foi usado nesta rodada.`);
            character=health_mjs_1.sufferStrain(character,spec.strainCost,{voluntary:true}).character; assertCanAct(character);
            if(combat.active){combat=clone(combat);combat.playerTurn={...(combat.playerTurn??{}),skillMasterUses:{...(combat.playerTurn?.skillMasterUses??{}),[kind]:true}};}
            masterDifficultyReduction+=spec.difficultyReduction; activeSkillTalents.push({...spec,used:true});
            actionCritical=assertCanPerformAction(character);
        }
        let masterLeader=null;
        if(useMasterLeader){
            if(skillKey!=='liderazgo'||talents_mjs_1.sourceBackedTalentRank(character,'MASTER_LEADER')<1)
                throw new Error('Líder especialista exige talento AoR comprado e teste de Liderança.');
            if(!combat.active)throw new Error('Líder especialista exige rodada de combate ativa.');
            if(combat.playerTurn?.masterLeaderUsed)throw new Error('Líder especialista já usado nesta rodada.');
            if(actionCritical.cannotVoluntarilySufferStrain)
                throw new Error('Ferimento Crítico impede pagar Tensão para Líder especialista.');
            character=health_mjs_1.sufferStrain(character,2,{voluntary:true}).character;
            assertCanAct(character);
            combat=clone(combat);
            combat.playerTurn={...combat.playerTurn,masterLeaderUsed:true};
            masterDifficultyReduction+=1;
            masterLeader={used:true,strainCost:2,difficultyReduction:1,round:combat.round};
            activeSkillTalents.push({label:'Líder especialista',...masterLeader});
            actionCritical=assertCanPerformAction(character);
        }
        const intimidating=talents_mjs_1.intimidatingOption(character,intimidatingStrain);
        if(intimidating.spend>0){
            if(!intimidating.available) throw new Error('Intimidador não foi adquirido.');
            if(skillKey!=='coaccion') throw new Error('Intimidador só pode reduzir a dificuldade de Coerção neste fluxo.');
            if(actionCritical.cannotVoluntarilySufferStrain) throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar Intimidador.');
            character=health_mjs_1.sufferStrain(character,intimidating.spend,{voluntary:true}).character; assertCanAct(character); activeSkillTalents.push({label:'Intimidador',...intimidating,used:true});
            actionCritical=assertCanPerformAction(character);
        }
        const status = statusEffects_mjs_1.statusModifiers(character, { consumeNextCheck: true });
        character = status.character;
        if(socialTalentManeuver!==null){
            const socialTalent=socialTalentManeuver==='inspiring' ? {key:'INSPIRINGRHETSUP',skill:'liderazgo',label:'Retórica inspiradora'} :
                socialTalentManeuver==='scathing' ? {key:'SCATHINGTIRADESUP',skill:'coaccion',label:'Língua afiada'} : null;
            if(!socialTalent||skillKey!==socialTalent.skill||talents_mjs_1.sourceBackedTalentRank(character,socialTalent.key)<1)
                throw new Error('Manobra social suprema não adquirida para esta perícia.');
            if(!combat.active)throw new Error('Manobra social suprema exige combate ativo.');
            if(actionCritical.cannotVoluntarilySufferStrain)throw new Error('Ferimento Crítico impede pagar Tensão voluntária.');
            const moved=spendPlayerManeuver(character,combat,{method:socialManeuverMethod,label:socialTalent.label});
            character=health_mjs_1.sufferStrain(moved.character,1,{voluntary:true}).character;
            assertCanAct(character);
            combat=clearPlayerAim(moved.combat);
        }else if(useSkilledSlicerManeuver){
            const spent=spendPlayerManeuver(character,combat,{method:skilledSlicerManeuverMethod,label:'Slicer consumado'});
            character=spent.character;combat=clearPlayerAim(spent.combat);
        }else if(combat.active) combat = clearPlayerAim(combat_mjs_1.spendAction(combat, `Usar ${skillKey}`));
        const expert = talents_mjs_1.expertSkillTalentSpec(character, skillKey);
        const codebreakerReduction = talents_mjs_1.codebreakerDifficultyReduction(character,skillKey,{codebreaking});
        const scholarDowngrades=talents_mjs_1.respectedScholarDowngrades(character,skillKey,{scholarlyInstitution});
        let effectiveDifficulty = Math.max((masterDifficultyReduction+codebreakerReduction)>0?1:0,Number(difficulty)-masterDifficultyReduction-codebreakerReduction);
        let expertTalent = null;
        if (useExpertTalent) {
            if (actionCritical.cannotVoluntarilySufferStrain)
                throw new Error('O Ferimento Crítico impede sofrer Tensão voluntariamente para usar este talento.');
            if (!expert.available)
                throw new Error('Esta perícia não possui um talento especialista disponível na ficha.');
            if (combat.active && combat.playerTurn?.expertSkillTalentUsed)
                throw new Error('O talento especialista desta rodada já foi usado.');
            character = health_mjs_1.sufferStrain(character, expert.strainCost,{voluntary:true}).character;
            assertCanAct(character);
            effectiveDifficulty = Math.max(expert.minDifficulty, effectiveDifficulty - expert.difficultyReduction);
            if (combat.active)
                combat.playerTurn.expertSkillTalentUsed = true;
            expertTalent = { ...expert, used: true, effectiveDifficulty };
        }
        const critical = critical_mjs_1.criticalCheckModifiers(character, { skillKey, characteristicKey, consume: true });
        character = critical.character;
        const effectiveCharacteristic = Math.max(0, Number(character.characteristics[characteristicKey] ?? 0) - Number(critical.characteristicPenalty ?? 0));
        let positive;
        let assistedInfo = null;
        let effectiveBoost = Number(boost) + (useTouchOfFate ? Number(touchOfFate.boost ?? 0) : 0) +
            aorSocial_mjs_1.activeInspiringBoost(character,combat?.active?combat.round:0,combat?.socialEncounterSerial);
        const assistantActor = assistant?.actor || assistant;
        if (assistantActor) {
            const primaryVirtual = { characteristics: { ...character.characteristics, [characteristicKey]: effectiveCharacteristic }, skills: character.skills };
            const assistedPool = narrativeDice_mjs_1.buildAssistedCheckPool({ primaryActor: primaryVirtual, assistantActor, skillKey, characteristicKey, skilled: assistant?.skilled ?? null });
            positive = { ability: assistedPool.ability, proficiency: assistedPool.proficiency };
            effectiveBoost += assistedPool.boost;
            assistedInfo = assistedPool;
        } else positive = narrativeDice_mjs_1.buildPositivePool(effectiveCharacteristic, character.skills[skillKey]);
        if (greasedPalms) positive = destiny_mjs_1.upgradePositivePool(positive,greasedPalms.upgrades);
        if (intenseFocus?.used) positive = destiny_mjs_1.upgradePositivePool(positive, intenseFocus.abilityUpgrades);
        if (useLightDestiny) { positive = destiny_mjs_1.upgradePositivePool(positive, 1); const group = destiny_mjs_1.flipDestiny(this.state.group, 'light'); this.commit('GROUP_UPDATED', { group }); }
        effectiveDifficulty += Number(critical.difficultyIncrease ?? 0) + Number(status.difficultyIncrease ?? 0);
        let criticalDifficulty = difficultyWithUpgrades(effectiveDifficulty, Number(challenge), Number(critical.difficultyUpgrades ?? 0));
        if(intimidating.spend>0) criticalDifficulty=difficultyWithDowngrades(criticalDifficulty.difficulty,criticalDifficulty.challenge,intimidating.downgrades);
        // AoR Respected Scholar downgrades RED challenge dice into PURPLE;
        // it never removes plain difficulty dice when no red dice remain.
        if(scholarDowngrades>0){
            const applied=Math.min(scholarDowngrades,criticalDifficulty.challenge);
            criticalDifficulty={difficulty:criticalDifficulty.difficulty+applied,challenge:criticalDifficulty.challenge-applied};
        }
        // SWE09 printed p.69: a captured electronic signature upgrades ALL
        // later Cloud City slicing checks, not only the remote G-100. The
        // G-100 uses its own bank dice engine and must not be upgraded twice.
        const jewelElectronicSignatureUpgrade = skillKey==='computadoras' &&
            this.state.world?.publishedAdventure?.id==='jewel-of-yavin' &&
            this.state.world?.publishedAdventure?.status==='ACTIVE' &&
            this.state.world?.publishedAdventure?.operations?.bankSpending?.signatureCaptured===true &&
            this.state.world?.publishedAdventure?.operations?.bankTools?.signatureCleared!==true &&
            (codebreaking===true || cloudCitySlicing===true || confirmedSlicingSystem===true || slicingSystemId!==null);
        if(jewelElectronicSignatureUpgrade)
            criticalDifficulty=difficultyWithUpgrades(criticalDifficulty.difficulty,criticalDifficulty.challenge,1);
        if (useDarkDestiny) { criticalDifficulty = destiny_mjs_1.upgradeNegativePool(criticalDifficulty, 1); const group = destiny_mjs_1.flipDestiny(this.state.group, 'dark'); this.commit('GROUP_UPDATED', { group }); }
        const baseSetback = Number(setback);
        const declaredBraceEnvironment=Number(braceEnvironmentalSetback);
        if(!Number.isSafeInteger(declaredBraceEnvironment)||declaredBraceEnvironment<0||declaredBraceEnvironment>baseSetback)
            throw new Error('Brace exige um número válido de Setbacks ambientais incluídos no Setback informado.');
        let gearModifier = null;
        if (gearAssist) {
            const assistItemId=String(gearAssist.itemId??'').trim();
            const ownedAssist=(character.gear??[]).find(item=>{
                const id=String(item?.id??item?.sourceCatalogId??'').trim();
                return id===assistItemId&&Number(item?.quantity??1)>0;
            });
            if(!ownedAssist) throw new Error('O equipamento de assistência precisa estar realmente possuído na mochila.');
            if (assistItemId === 'tool_kit') {
                if (skillKey !== 'mecanica')
                    throw new Error('Kit de ferramentas só pode aliviar a penalização auditada de Mecânica.');
                gearModifier = generalGearEffects_mjs_1.mechanicsToolSetbackRelief(assistItemId, { setback: baseSetback, missingToolsSetback: gearAssist.missingToolsSetback ?? 0 });
            }
            else {
                gearModifier = generalGearEffects_mjs_1.opticalSetbackRelief(assistItemId, { setback: baseSetback, longRangeSetback: gearAssist.longRangeSetback ?? 0, poorLightingSetback: gearAssist.poorLightingSetback ?? 0 });
            }
        }
        const environmentalSetback = gearModifier ? gearModifier.remainingSetback : baseSetback;
        const environmentModifiers=environment_mjs_1.environmentSkillModifiers(this.state.environment,skillKey,characteristicKey);
        effectiveBoost+=Number(environmentModifiers.boost??0);
        const burdenSetback = encumbrance_mjs_1.encumbranceSetback(character, characteristicKey);
        const activePcId=this.state.player?.roster?.activeCharacterId??character.id??null;
        if(bracePending&&bracePending.characterId!==activePcId)throw new Error('Brace foi preparado por outra ficha; encerre ou limpe essa preparação antes de agir.');
        const braceEnvAuto=Math.max(0,Number(environmentModifiers.setback??0));
        const braceEnvTotal=declaredBraceEnvironment+braceEnvAuto;
        const braceResult=bracePending?talents_mjs_1.braceEnvironmentalSetbackRelief(character,{maneuverPrepared:true,environmentalSetback:braceEnvTotal}):null;
        const braceRemoved=Number(braceResult?.removedSetback??0);
        const braceManualRemoved=Math.min(braceRemoved,declaredBraceEnvironment,environmentalSetback);
        const braceAutoRemoved=Math.min(Math.max(0,braceRemoved-braceManualRemoved),braceEnvAuto);
        const talentModifiers = talents_mjs_1.talentSkillPoolModifiers(character, skillKey, { boost: effectiveBoost + Number(status.boost ?? 0), setback: Math.max(0,environmentalSetback-braceManualRemoved) + Math.max(0,braceEnvAuto-braceAutoRemoved) + critical.setback + Number(status.setback ?? 0) + burdenSetback, securityBypass, tracking, codebreaking, outdoors, chase, crafting, foraging, poisonResistance, terrainSetback });
        const senseDangerRemoved=useSenseDanger?Math.min(Number(senseDanger.setbackRemoved??0),Number(talentModifiers.setback??0)):0;
        const baseFinalSetback=Math.max(0,Number(talentModifiers.setback??0)-senseDangerRemoved) +
            (this.state.combat?.active&&['pelea','armas_cuerpo_a_cuerpo','armas_distancia_ligeras','armas_distancia_pesadas','artilleria'].includes(skillKey)?holoParkTransportSetback(this.state.combat):0);
        const finalSetback=baseFinalSetback;
        if(bracePending)delete combat.aorBracePrepared;
        const finalPool={
            ...positive,
            difficulty: criticalDifficulty.difficulty,
            challenge: criticalDifficulty.challenge,
            boost: critical.removeBoost ? 0 : talentModifiers.boost,
            setback: finalSetback
        };
        const vertigoSkill=combat?.playerTurn?.actionUsed?consumeJewelHoloVertigo(combat,finalPool,{actionKind:'PLAYER_SKILL'}):{combat,pool:finalPool};combat=vertigoSkill.combat;Object.assign(finalPool,vertigoSkill.pool);
        if(typeof spendKnowledgeTriumph!=='boolean'||typeof spendSmoothTalkerTriumph!=='boolean')throw new Error('Escolha de gasto de Triunfo inválida.');
        if([spendKnowledgeTriumph,spendSmoothTalkerTriumph,spendSkilledSlicerTriumph].filter(Boolean).length>1)
          throw new Error('Não é possível gastar o mesmo Triunfo duas vezes.');
        const knowledgeRanks=talents_mjs_1.knowledgeSpecializationRank(character,skillKey);
        if(spendKnowledgeTriumph&&!knowledgeRanks)throw new Error('Conhecimento especializado não foi adquirido para esta perícia.');
        const smoothRanks=talents_mjs_1.smoothTalkerRank(character,skillKey);
        if(spendSmoothTalkerTriumph&&!smoothRanks)throw new Error('Lábia não foi adquirida para esta perícia.');
        const originalRoll=narrativeDice_mjs_1.rollPool(finalPool,rng);
        const resolvedRoll=useNaturalReroll?narrativeDice_mjs_1.rollPool(finalPool,rng):originalRoll;
        const knowledgeSpent=spendKnowledgeTriumph&&Number(resolvedRoll.triumph??0)>0;
        const knowledgeBonus=knowledgeSpent?knowledgeRanks:0;
        const smoothSpent=spendSmoothTalkerTriumph&&Number(resolvedRoll.triumph??0)>0;
        const smoothBonus=smoothSpent?smoothRanks:0;
        const slicerSpent=spendSkilledSlicerTriumph&&Number(resolvedRoll.triumph??0)>0;
        if(slicerSpent){
          if(!combat.active)throw Error('Slicer consumado exige encontro ativo para converter testes em manobras.');
          combat=clone(combat);
          combat.aorSkilledSlicer={systemId:slicingSystemId,sessionId:this.state.sessionId,activatedRound:combat.round};
          combat.log=[...(combat.log??[]),{type:'AOR_SLICER_CONTROL',systemId:slicingSystemId,round:combat.round,sessionId:this.state.sessionId,triumphSpent:1}];
        }
        const check = {
            ...resolvedRoll,
            netSuccess:Number(resolvedRoll.netSuccess??0)+knowledgeBonus+smoothBonus,
            success:Number(resolvedRoll.netSuccess??0)+knowledgeBonus+smoothBonus>0,
            remainingTriumph:Math.max(0,Number(resolvedRoll.triumph??0)-(knowledgeSpent?1:0)-(smoothSpent?1:0)-(slicerSpent?1:0)),
            skilledSlicer:spendSkilledSlicerTriumph?{systemId:slicingSystemId,triumphSpent:slicerSpent?1:0,controlActive:slicerSpent}:null,
            skilledSlicerManeuver:useSkilledSlicerManeuver,
            smoothTalker:spendSmoothTalkerTriumph?{skillKey,rank:smoothRanks,triumphSpent:smoothSpent?1:0,bonusSuccesses:smoothBonus}:null,
            knowledgeSpecialization:spendKnowledgeTriumph?{skillKey,rank:knowledgeRanks,triumphSpent:knowledgeSpent?1:0,bonusSuccesses:knowledgeBonus}:null,
            naturalReroll:useNaturalReroll?{...naturalReroll,used:true,originalRoll}:null,
            talentModifiers:{...talentModifiers,finalSetback},
            taskTime,
            scholarDowngrades,
            jewelElectronicSignatureUpgrade,
            ...(taskDurationMinutes !== null ? {
                taskCheckId: ids_mjs_1.newId('task'), taskSkillKey: skillKey,
                taskBaseMinutes: taskDurationMinutes
            } : {}),
            gearModifier,
            expertTalent,
            assistedInfo,
            intenseFocus,
            senseDanger:useSenseDanger?{used:true,setbackRemoved:senseDangerRemoved}:null,
            touchOfFate:useTouchOfFate?{used:true,boost:Number(touchOfFate.boost??0)}:null,
            activeSkillTalents, masterLeader, cleverSolution:useCleverSolution?{used:true,characteristicKey:'astucia'}:null,
            aorAttributeSubstitution:substitution?{talent:substitution[0],characteristicKey:substitution[3],sessionKey:substitution[2],used:true}:null,
            aorBrace:bracePending?{rank:braceResult.rank,environmentalSetback:braceEnvTotal,removedSetback:braceRemoved,source:clone(bracePending.source)}:null,
            intimidating:intimidating.spend>0?intimidating:null, environmentModifiers
        };
        if(substitution)character=markTalentUsedThisSession(character,substitution[2]);
        if(useSenseDanger) character=markTalentUsedThisSession(character,'senseDanger');
        if(useTouchOfFate) character=markTalentUsedThisSession(character,'touchOfFate');
        if(useNaturalReroll) character=markTalentUsedThisSession(character,naturalReroll.sessionKey);
        character = applyActionCriticalStrain(character, critical);
        const lylekSkillEvent=this._applySWE02OldLylekAdvantage(combat,check);
        combat=lylekSkillEvent.combat;
        if(lylekSkillEvent.event)check.lylekEvent=clone(lylekSkillEvent.event);
        this.commit('COMBAT_ACTION', { combat, character, check });
        if(greasedPalms){const receipt={kind:'greased_palms',actionId:greasedPalmsActionId,subjectId:'social_'+skillKey,
          campaignId:this.state.campaignId,sessionId:this.state.sessionId,characterName:character.name,approved:true,
          success:Boolean(check.success),skillKey,upgrades:greasedPalms.upgrades,costCredits:greasedPalms.costCredits};
          this.commit('AOR_P177_ACTION',{receipt});check.greasedPalms=clone(receipt);}
        if (this.state.session?.status === 'ativa')
            this.commit('SESSION_CHECK_RESOLVED', { session: sessionFlow_mjs_1.noteSessionCheck(this.state.session) });
        return check;
        });
    }

    resolveIncomingIdentificationCheck({actorName='NPC',actorCharacteristic=2,actorSkill=1,difficulty=2,challenge=0,rng=Math.random}={}){
        return this.atomicAction(()=>{
            const valid=n=>Number.isSafeInteger(n)&&n>=0&&n<=6;
            if(!valid(actorCharacteristic)||!valid(actorSkill)||!valid(difficulty)||!valid(challenge))
                throw new Error('Dados da identificação NPC inválidos.');
            if(actorSkill>5)throw new Error('Graduação de perícia NPC inválida.');
            const character=requireCharacter(this.state.player?.character);
            const talent=talents_mjs_1.indistinguishableIdentificationDefense(character,{identifyingCharacter:true});
            const positive=narrativeDice_mjs_1.buildPositivePool(actorCharacteristic,actorSkill);
            const negative=difficultyWithUpgrades(difficulty,challenge,talent.difficultyUpgrades);
            const check=narrativeDice_mjs_1.rollPool({...positive,...negative,boost:0,setback:0},rng);
            const result={type:'incoming-identification',actorName:String(actorName).slice(0,100),
                check,talent,difficulty:negative.difficulty,challenge:negative.challenge,resolvedAt:new Date().toISOString()};
            this.commit('ADVANCED_CHECK_RESOLVED',result);
            return result;
        });
    }
    resolveOpposedSkillCheck({ skillKey, opponentSkillKey = null, opponentCharacteristic = null, opponentSkill = null, targetAdversaryId = null, boost = 0, setback = 0, useLightDestiny = false, useDarkDestiny = false, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        let characteristic = opponentCharacteristic, skill = opponentSkill, label = 'Oposição';
        if (targetAdversaryId) {
            const adversary = (this.state.combat?.adversaries ?? []).find(a => a.id === targetAdversaryId);
            if (!adversary) throw new Error('Oponente não encontrado no encontro atual.');
            const opposeKey = opponentSkillKey || skillKey;
            characteristic = adversaries_mjs_1.adversaryCharacteristicForSkill(adversary, opposeKey);
            skill = adversaries_mjs_1.adversarySkillRank(adversary, opposeKey);
            label = adversary.name ?? 'Oponente';
        }
        const negative = narrativeDice_mjs_1.buildOpposedNegativePool(characteristic, skill);
        const check = this.resolveSkillCheck({ skillKey, difficulty: negative.difficulty, challenge: negative.challenge, boost, setback, useLightDestiny, useDarkDestiny, rng });
        const result = { type:'opposed', label, opposition:negative.opposition, check, skillKey, resolvedAt:new Date().toISOString() };
        this.commit('ADVANCED_CHECK_RESOLVED', result);
        return result;
        });
    }
    resolveSocialInfluenceCheck({ skillKey, opponentSkillKey = null, opponentCharacteristic = null, opponentSkill = null, targetAdversaryId = null, boost = 0, setback = 0, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        const mapping = { coaccion:'disciplina', engano:'disciplina', liderazgo:'disciplina', carisma:'frialdad', negociacion:'negociacion' };
        if (!(skillKey in mapping)) throw new Error('Perícia social não suportada por este fluxo.');
        const allowed = skillKey === 'negociacion' ? ['negociacion','frialdad'] : [mapping[skillKey]];
        const opposedBy = opponentSkillKey || mapping[skillKey];
        if (!allowed.includes(opposedBy)) throw new Error('Perícia de oposição social inválida para a abordagem escolhida.');
        const result = this.resolveOpposedSkillCheck({ skillKey, opponentSkillKey:opposedBy, opponentCharacteristic, opponentSkill, targetAdversaryId, boost, setback, rng });
        const social = { ...result, type:'social', actingSkill:skillKey, opposingSkill:opposedBy, resolvedAt:new Date().toISOString() };
        this.commit('SOCIAL_CHECK_RESOLVED', social);
        return social;
        });
    }
    /** P149: NPC-initiated social influence against the player. Unlike
     * resolveSocialInfluenceCheck this roll uses the NPC's positive pool and
     * the player's opposed pool; Nobody's Fool upgrades ONLY this negative
     * pool. Does not mutate the player or consume any voluntary resources. */
    resolveIncomingSocialInfluenceCheck({ skillKey, actorAdversaryId = null,
        actorCharacteristic = null, actorSkill = null, opponentSkillKey = null,
        boost = 0, setback = 0, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        const mapping = { carisma:'frialdad', coaccion:'disciplina', engano:'disciplina' };
        if (!Object.hasOwn(mapping,skillKey))
            throw new Error('Influência social recebida exige Charme, Coerção ou Enganação.');
        const defenseKey=opponentSkillKey ?? mapping[skillKey];
        if (defenseKey !== mapping[skillKey])
            throw new Error('Perícia defensiva incompatível com influência recebida.');
        const player=requireCharacter(this.state.player?.character);
        let actorName='NPC', characteristic=actorCharacteristic, skill=actorSkill, socialSetback=0;
        if (actorAdversaryId !== null) {
            const adversary=(this.state.combat?.adversaries??[]).find(a=>a.id===actorAdversaryId);
            if (!adversary) throw new Error('NPC de influência não encontrado no encontro.');
            actorName=adversary.name??'NPC';
            characteristic=adversaries_mjs_1.adversaryCharacteristicForSkill(adversary,skillKey);
            skill=adversaries_mjs_1.adversarySkillRank(adversary,skillKey);
            socialSetback=aorSocial_mjs_1.activeScathingSetback(adversary,this.state.combat?.round??0);
        }
        const positive=narrativeDice_mjs_1.buildPositivePool(characteristic,skill);
        const defense=narrativeDice_mjs_1.buildOpposedNegativePool(
            player.characteristics[character_mjs_1.SKILLS[defenseKey]],player.skills[defenseKey]);
        const talent=talents_mjs_1.nobodyFoolDefense(player,skillKey);
        const negative=difficultyWithUpgrades(defense.difficulty,defense.challenge,talent.difficultyUpgrades);
        if (![boost,setback].every(n=>Number.isSafeInteger(n)&&n>=0&&n<=100))
            throw new Error('Dados de influência social inválidos.');
        const check=narrativeDice_mjs_1.rollPool({...positive,...negative,boost,setback:setback+socialSetback},rng);
        const result={type:'incoming-social',actorName,actingSkill:skillKey,opposingSkill:defenseKey,
            opposition:defense.opposition,talent,check,resolvedAt:new Date().toISOString()};
        this.commit('SOCIAL_CHECK_RESOLVED',result);
        return result;
        });
    }
    /** P150: enemy Computers intrusion against a system protected by the
     * player. Defensive Slicing is contextual, with improved replacing (not
     * adding to) the ranked setbacks. One persisted check, one atomic write. */
    resolveIncomingComputerIntrusionCheck({ actorAdversaryId = null,
        actorCharacteristic = null, actorSkill = null, baseDifficulty = 2,
        baseChallenge = 0, defendingOwnedSystem = false, boost = 0,
        setback = 0, rng = Math.random } = {}) {
        return this.atomicAction(() => {
            const player = requireCharacter(this.state.player?.character);
            if (defendingOwnedSystem !== true)
                throw new Error('Defesa informática exige sistema possuído ou programado pelo personagem.');
            const validDice = n => Number.isSafeInteger(n) && n >= 0 && n <= 10;
            if (![baseDifficulty, baseChallenge, boost, setback].every(validDice) ||
                baseDifficulty + baseChallenge > 10)
                throw new Error('Pool de intrusão informática inválido.');
            let actorName = 'Invasor', characteristic = actorCharacteristic, skill = actorSkill, socialSetback=0;
            if (actorAdversaryId !== null) {
                const adversary = (this.state.combat?.adversaries ?? []).find(a => a.id === actorAdversaryId);
                if (!adversary) throw new Error('Invasor não encontrado no encontro.');
                actorName = adversary.name ?? 'Invasor';
                characteristic = adversaries_mjs_1.adversaryCharacteristicForSkill(adversary, 'computadoras');
                skill = adversaries_mjs_1.adversarySkillRank(adversary, 'computadoras');
                socialSetback=aorSocial_mjs_1.activeScathingSetback(adversary,this.state.combat?.round??0);
            }
            if (![characteristic, skill].every(validDice) || characteristic > 6 || skill > 5)
                throw new Error('Dados do invasor inválidos.');
            const positive = narrativeDice_mjs_1.buildPositivePool(characteristic, skill);
            const talent = talents_mjs_1.defensiveSlicingDefense(player, { defendingOwnedSystem });
            const negative = difficultyWithUpgrades(baseDifficulty, baseChallenge, talent.difficultyUpgrades);
            const check = narrativeDice_mjs_1.rollPool({ ...positive, ...negative,
                boost, setback: setback + talent.setback + socialSetback }, rng);
            const result = { type: 'incoming-computer-intrusion', actorName,
                actingSkill: 'computadoras', protectedSystem: true,
                talent, check, resolvedAt: new Date().toISOString() };
            this.commit('ADVANCED_CHECK_RESOLVED', result);
            return result;
        });
    }
    /** P151 — Directed NPC droid roll. The GM supplies real NPC statistics and
     * explicitly confirms the actor is a droid. The character's own skills are
     * never substituted; one validated roll and one atomic persistence write. */
    resolveDirectedDroidSkillCheck({skillKey, droidCharacteristic, droidSkill,
        difficulty=2, challenge=0, setback=0, boost=0, confirmedDroid=false,
        rng=Math.random}={}) {
        return this.atomicAction(() => {
            const leader=requireCharacter(this.state.player.character);
            if(confirmedDroid!==true) throw new Error('Confirme que o NPC dirigido é um droide.');
            if(!character_mjs_1.SKILLS[skillKey]) throw new Error('Perícia do droide inválida.');
            for(const [name,value,max] of [['Característica',droidCharacteristic,6],['Graduação',droidSkill,5],
                ['Dificuldade',difficulty,5],['Desafio',challenge,5],['Contratempo',setback,20],['Bônus',boost,20]])
                if(!Number.isSafeInteger(value)||value<0||value>max) throw new Error(`${name} do droide inválido.`);
            if(challenge>difficulty) throw new Error('Desafios não podem exceder a dificuldade.');
            const binaryBoost=talents_mjs_1.speaksBinaryBoost(leader,{directingDroid:true});
            const positive=narrativeDice_mjs_1.buildPositivePool(droidCharacteristic,droidSkill);
            const check={...narrativeDice_mjs_1.rollPool({...positive,difficulty:difficulty-challenge,
                challenge,boost:boost+binaryBoost,setback},rng),
                kind:'directed_npc_droid',skillKey,binaryBoost,confirmedDroid:true};
            this.commit('CHECK_RESOLVED',check);
            return check;
        });
    }

    resolveAssistedSkillCheck({ skillKey, assistantCharacteristic = 0, assistantSkill = 0, skilled = null, difficulty = 2, challenge = 0, boost = 0, setback = 0, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        const characteristicKey = character_mjs_1.SKILLS[skillKey];
        if (!characteristicKey) throw new Error('Perícia inválida para assistência.');
        const actor = { characteristics:{[characteristicKey]:Number(assistantCharacteristic)}, skills:{[skillKey]:Number(assistantSkill)} };
        const check = this.resolveSkillCheck({ skillKey, difficulty, challenge, boost, setback, assistant:{actor,skilled}, rng });
        const result = { type:'assisted', skillKey, check, assistance:check.assistedInfo ?? null, resolvedAt:new Date().toISOString() };
        this.commit('ADVANCED_CHECK_RESOLVED', result);
        return result;
        });
    }
    resolveCompetitiveSkillCheck({ skillKey, difficulty = 2, opponentCharacteristic = 0, opponentSkill = 0, opponentLabel = 'Oponente', boost = 0, setback = 0, opponentBoost = 0, opponentSetback = 0, rng = Math.random, opponentRng = null } = {}) {
        return this.atomicAction(() => {
        const player = this.resolveSkillCheck({ skillKey, difficulty, boost, setback, rng });
        const opponentPositive = narrativeDice_mjs_1.buildPositivePool(Number(opponentCharacteristic), Number(opponentSkill));
        const opponent = narrativeDice_mjs_1.rollPool({ ...opponentPositive, difficulty:Number(difficulty), challenge:0, boost:Number(opponentBoost), setback:Number(opponentSetback) }, opponentRng ?? rng);
        const comparison = narrativeDice_mjs_1.compareCompetitiveResults([{id:'player',label:this.state.player?.character?.name ?? 'Jogador',check:player},{id:'opponent',label:String(opponentLabel||'Oponente'),check:opponent}]);
        const result = { type:'competitive', skillKey, player, opponent, comparison, resolvedAt:new Date().toISOString() };
        this.commit('ADVANCED_CHECK_RESOLVED', result);
        return result;
        });
    }
    resolveCheck({ characteristic, skill, difficulty = 2, challenge = 0, boost = 0, setback = 0, rng = Math.random }) {
        return this.atomicAction(() => {
        const positive = narrativeDice_mjs_1.buildPositivePool(characteristic, skill);
        let result = narrativeDice_mjs_1.rollPool({ ...positive, difficulty: Number(difficulty), challenge: Number(challenge), boost: Number(boost), setback: Number(setback) }, rng);
        const lylekResult=this._applySWE02OldLylekAdvantage(clone(this.state.combat),result);
        if(lylekResult.event){result={...result,lylekEvent:clone(lylekResult.event)};this.commit('COMBAT_ACTION',{combat:lylekResult.combat,check:result});}
        this.commit('CHECK_RESOLVED', result);
        if (this.state.session?.status === 'ativa')
            this.commit('SESSION_CHECK_RESOLVED', { session: sessionFlow_mjs_1.noteSessionCheck(this.state.session) });
        return result;
        });
    }
    resolveWeaponAttack({ weaponIndex = 0, targetAdversaryId = null, targetRange = null, targetBehindRockPile=false, difficulty = 2, challenge = 0, boost = 0, setback = 0, braceEnvironmentalSetback=0, useLightDestiny = false, useDarkDestiny = false, useAutoFire = false, useTargetedBlow = false, useAnatomyLessons = false, useStunningBlow = false, useSoftSpot = false, useCripplingBlow = false, useKashyyykInsect = false, gmInsectConfirmed = false, rng = Math.random }) {
        assertJewelHoloWeaponUsable(this.state.combat,Number(weaponIndex));
        return this.atomicAction(() => {
        let character = this.state.player.character;
        const bracePending=clone(this.state.combat?.aorBracePrepared??null);
        assertCanPerformAction(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const weapon = character.weapons?.[Number(weaponIndex)];
        if (!weapon)
            throw new Error('Arma inválida.');
        assertOwnedEquipmentOperational(this.state,weapon);
        if(weapon.runtimeState?.outOfAmmo)
            throw new Error('Arma sem munição: use uma Recarga adicional antes de atacar.');
        if((useTargetedBlow||useAnatomyLessons||useSoftSpot)&&useLightDestiny) throw new Error('Somente um Ponto de Destino do jogador pode ser investido em um mesmo teste.');
        if(useSoftSpot&&talents_mjs_1.sourceBackedTalentRank(character,'SOFT_SPOT')<1)throw Error('Ponto débil AoR não adquirido.');
        if(useCripplingBlow&&talents_mjs_1.sourceBackedTalentRank(character,'CRIPPLING_BLOW')<1)throw Error('Golpe incapacitante AoR não adquirido.');
        if(useCripplingBlow&&!targetAdversaryId)throw Error('Golpe incapacitante exige alvo real de encontro.');
        if(useTargetedBlow&&useAnatomyLessons) throw new Error('Golpe Direcionado e Anatomia não podem gastar dois Pontos de Destino no mesmo ataque.');
        const characteristicKey = character_mjs_1.SKILLS[weapon.skill];
        if (!characteristicKey)
            throw new Error('A perícia desta arma ainda não está mapeada.');
        const status = statusEffects_mjs_1.statusModifiers(character, { consumeNextCheck: true });
        character = status.character;
        let combat = this.state.combat;
        const pendingAim = clone(combat?.pendingAim ?? null);
        const trueAim=talents_mjs_1.trueAimOption(character);
        const pendingPreciseAim=clone(combat?.pendingPreciseAim??null);
        const pendingSniperShot=clone(combat?.pendingSniperShot??null);
        const pendingDestinyTalent=combat?.pendingDestinyDamageTalent??null;
        useTargetedBlow=Boolean(useTargetedBlow||pendingDestinyTalent==='targetedBlow');
        useAnatomyLessons=Boolean(useAnatomyLessons||pendingDestinyTalent==='anatomyLessons');
        if(useSoftSpot&&(useTargetedBlow||useAnatomyLessons))throw Error('Não é permitido gastar múltiplos Pontos de Destino no mesmo ataque.');
        if (combat.active)
            combat = combat_mjs_1.spendAction(combat, `Ataque: ${weapon.name}`);
        const critical = critical_mjs_1.criticalCheckModifiers(character, { skillKey: weapon.skill, characteristicKey, consume: true });
        character = critical.character;
        const effectiveCharacteristic = Math.max(0, Number(character.characteristics[characteristicKey] ?? 0) - Number(critical.characteristicPenalty ?? 0));
        let positive = narrativeDice_mjs_1.buildPositivePool(effectiveCharacteristic, character.skills[weapon.skill] ?? 0);
        const trueAimUpgrades=pendingAim?.mode==='true_aim' ? trueAim.abilityUpgrades : 0;
        if(pendingAim?.mode==='true_aim'){
            if(!trueAim.available) throw new Error('Mira Verdadeira não foi adquirida.');
            if(['pelea','armas_cuerpo_a_cuerpo'].includes(weapon.skill)) throw new Error('Mira Verdadeira exige arma de ataque à distância.');
            positive=destiny_mjs_1.upgradePositivePool(positive,trueAimUpgrades);
        }
        const frenziedAttack=clone(combat?.playerTurn?.pendingFrenziedAttack??null);
        if(frenziedAttack){
            if(!['pelea','armas_cuerpo_a_cuerpo'].includes(weapon.skill)) throw new Error('Ataque Frenético só pode aprimorar testes de Briga ou Corpo a Corpo.');
            positive=destiny_mjs_1.upgradePositivePool(positive,Math.max(0,Number(frenziedAttack.abilityUpgrades??0)));
        }
        const senseOffense = force_mjs_1.consumeSenseOngoingTrigger(character, { mode:'offense', round:this.state.combat?.round ?? 0 });
        character = senseOffense.character;
        if (senseOffense.applied) positive = destiny_mjs_1.upgradePositivePool(positive, senseOffense.upgrades);
        if (useLightDestiny) { positive = destiny_mjs_1.upgradePositivePool(positive, 1); const group = destiny_mjs_1.flipDestiny(this.state.group, 'light'); this.commit('GROUP_UPDATED', { group }); }
        const vertigoAction=consumeJewelHoloVertigo(combat,positive,{actionKind:'PLAYER_WEAPON'});combat=vertigoAction.combat;positive=vertigoAction.pool;
        const targetAdversary = targetAdversaryId ? combat.adversaries?.find(a => a.id === targetAdversaryId) : null;
        if (targetAdversaryId && !targetAdversary)
            throw new Error('Alvo do encontro não encontrado.');
        if (targetAdversary && adversaries_mjs_1.adversaryHealthStatus(targetAdversary).incapacitated)
            throw new Error('Esse adversário já está incapacitado.');
        if(targetAdversary)assertJewelHoloTargetVisible(targetAdversary);
        const resolvedRange = targetAdversary ? targetAdversary.range : targetRange;
        const attackWeapon=character.weapons?.[Number(weaponIndex)]??weapon;
        const effectiveAttachmentWeapon=attachments_mjs_1.effectiveWeaponWithAttachments(attackWeapon);
        const weaponQualityModifiers=weapons_mjs_1.weaponAttackModifiers(effectiveAttachmentWeapon,{useAutoFire,character});
        attackWeapon.runtimeState={...(attackWeapon.runtimeState??{})};
        if(weaponQualityModifiers.prepare>0&&Number(attackWeapon.runtimeState.preparedManeuvers??0)<weaponQualityModifiers.prepare) throw new Error(`A arma exige ${weaponQualityModifiers.prepare} manobra(s) de Preparação antes do ataque.`);
        if(attackWeapon.runtimeState.outOfAmmo) throw new Error('Arma sem munição: use uma Recarga adicional antes de atacar.');
        if(weaponQualityModifiers.limitedAmmo>0){if(attackWeapon.runtimeState.limitedAmmoRemaining===undefined)attackWeapon.runtimeState.limitedAmmoRemaining=weaponQualityModifiers.limitedAmmo;if(Number(attackWeapon.runtimeState.limitedAmmoRemaining)<=0)throw new Error('A arma está sem munição limitada; use uma Recarga adicional antes de atacar.');}
        if(weaponQualityModifiers.slowFiring>0&&combat.active&&Number.isFinite(Number(attackWeapon.runtimeState.lastFiredRound))&&Number(combat.round)<=Number(attackWeapon.runtimeState.lastFiredRound)+weaponQualityModifiers.slowFiring) throw new Error(`Cadência Lenta ${weaponQualityModifiers.slowFiring}: esta arma ainda não pode disparar novamente.`);
        const stunSettingActive=Boolean(attackWeapon.runtimeState?.stunSettingActive&&weapons_mjs_1.weaponHasQuality(attackWeapon,'stunSetting'));
        if(stunSettingActive&&!['Interação','Curto','Engaged','Short'].includes(String(resolvedRange??''))) throw new Error('Modo atordoante limita o alcance a Curto.');
        if(useStunningBlow&&(weapon.skill!=='armas_cuerpo_a_cuerpo'||talents_mjs_1.sourceBackedTalentRank(character,'STUNBL')<1))
          throw Error('Golpe comocionador exige arma corpo a corpo e talento adquirido.');
        let effectiveAttackWeapon=stunSettingActive?{...effectiveAttachmentWeapon,range:'Curto',damageType:'strain'}:effectiveAttachmentWeapon;
        if(useStunningBlow)effectiveAttackWeapon={...effectiveAttackWeapon,damageType:'strain'};
        const strongArmBands=talents_mjs_1.strongArmRangeBonus(character,effectiveAttackWeapon);
        if(strongArmBands>0) effectiveAttackWeapon=extendPersonalWeaponRange(effectiveAttackWeapon,strongArmBands);
        if(pendingSniperShot){
            if(['pelea','armas_cuerpo_a_cuerpo'].includes(weapon.skill)) throw new Error('Tiro de Precisão exige ataque à distância não arremessado.');
            effectiveAttackWeapon=extendPersonalWeaponRange(effectiveAttackWeapon,pendingSniperShot.bands);
        }
        const automaticDifficulty = resolvedRange ? attackDifficulty_mjs_1.attackDifficultyForWeapon(effectiveAttackWeapon, resolvedRange) : Number(difficulty);
        const upgrades = targetAdversary ? adversaries_mjs_1.upgradeDifficultyForAdversaryTalent(targetAdversary) : 0;
        let upgraded = difficultyWithUpgrades(automaticDifficulty + (useCripplingBlow?1:0) + Number(weaponQualityModifiers.difficultyIncrease??0) + Number(critical.difficultyIncrease ?? 0) + Number(status.difficultyIncrease ?? 0), Number(challenge), upgrades + Number(critical.difficultyUpgrades ?? 0) + Number(pendingSniperShot?.difficultyUpgrades??0));
        if (useDarkDestiny) { upgraded = destiny_mjs_1.upgradeNegativePool(upgraded, 1); const group = destiny_mjs_1.flipDestiny(this.state.group, 'dark'); this.commit('GROUP_UPDATED', { group }); }
        const quickStrike = targetAdversary ? talents_mjs_1.quickStrikeBoost(character, { targetActorId: targetAdversary.id, actedActorIds: combat.encounter?.actedActorIds ?? [] }) : { rank: 0, boost: 0, applies: false };
        const proneMods=targetAdversary?proneTargetModifiers(targetAdversary,effectiveAttackWeapon):{boost:0,setback:0};
        const environmentModifiers=environment_mjs_1.environmentSkillModifiers(this.state.environment,weapon.skill,characteristicKey);
        const declaredBraceEnvironment=Number(braceEnvironmentalSetback);
        if(!Number.isSafeInteger(declaredBraceEnvironment)||declaredBraceEnvironment<0||declaredBraceEnvironment>Number(setback))
            throw new Error('Brace exige um número válido de Setbacks ambientais incluídos no Setback informado.');
        const activePcId=this.state.player?.roster?.activeCharacterId??character.id??null;
        if(bracePending&&bracePending.characterId!==activePcId)throw new Error('Brace foi preparado por outra ficha; encerre ou limpe essa preparação antes de agir.');
        const oldLylekTargetCover=Boolean(targetBehindRockPile&&targetAdversary&&
            combat.publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&
            combat.publishedAdventureContext?.encounterId==='lylek-den-bounty-hunters'&&!isMeleeWeapon(effectiveAttackWeapon));
        if(typeof targetBehindRockPile!=='boolean')throw Error('SWE02 p.17: estado de cobertura deve ser explícito.');
        const braceEnvAuto=Math.max(0,Number(environmentModifiers.setback??0))+(oldLylekTargetCover?2:0);
        const braceEnvTotal=declaredBraceEnvironment+braceEnvAuto;
        const braceResult=bracePending?talents_mjs_1.braceEnvironmentalSetbackRelief(character,{maneuverPrepared:true,environmentalSetback:braceEnvTotal}):null;
        const braceRemoved=Number(braceResult?.removedSetback??0);
        const braceManualRemoved=Math.min(braceRemoved,declaredBraceEnvironment);
        const braceAutoRemoved=Math.min(Math.max(0,braceRemoved-braceManualRemoved),braceEnvAuto);
        const braceAdjustedSetback=Math.max(0,Number(setback)-braceManualRemoved);
        const totalBoost = Number(boost) + Number(status.boost ?? 0) + Number(combat.pendingAttackBoost ?? 0) + Number(quickStrike.boost ?? 0) + Number(weaponQualityModifiers.accurate??0) + Number(proneMods.boost??0) + Number(environmentModifiers.boost??0) + holoParkThrantaBoost(combat,weapon);
        const qualitySetback=Number(weaponQualityModifiers.setback??0);
        const targetedAimSetback = pendingAim?.mode === 'targeted' ? (Number(pendingAim.count ?? 1) >= 2 ? 1 : 2) : 0;
        const targetDefense=targetAdversary?targetDefenseSetback(targetAdversary,effectiveAttackWeapon,Number(pendingPreciseAim?.defenseReduction??0),combat.round):0;
        let check = { ...narrativeDice_mjs_1.rollPool({
                ...positive,
                difficulty: upgraded.difficulty,
                challenge: upgraded.challenge,
                boost: critical.removeBoost ? 0 : totalBoost,
                setback: Math.max(0,braceAdjustedSetback + (oldLylekTargetCover?2:0) + critical.setback + Number(status.setback ?? 0) + qualitySetback + Number(proneMods.setback??0) + targetedAimSetback + targetDefense + encumbrance_mjs_1.encumbranceSetback(character, characteristicKey) + (guardedStanceActive(character,combat?.round)?1:0) + Number(environmentModifiers.setback??0) + holoParkTransportSetback(combat)-braceAutoRemoved)
            }, rng), environmentModifiers, quickStrike, aim: pendingAim, trueAimUpgrades, targetedAimSetback, targetDefense, preciseAim:pendingPreciseAim, sniperShot:pendingSniperShot, strongArmBands, frenziedAttack, senseOffense: { applied:senseOffense.applied, upgrades:senseOffense.upgrades, triggersUsed:senseOffense.triggersUsed, triggersPerRound:senseOffense.triggersPerRound } };
        check.aorBrace=bracePending?{rank:braceResult.rank,environmentalSetback:braceEnvTotal,removedSetback:braceRemoved,source:clone(bracePending.source)}:null;
        check.swe02OldLylekCover=oldLylekTargetCover?{behindRockPile:true,setback:2,sourcePage:17}:null;
        let kashyyykInsectProof=null;
        if(useKashyyykInsect){
            const park=this.state.world?.publishedAdventure?.operations?.holoPark;
            if(gmInsectConfirmed!==true||!combat.active||park?.status!=='ACTIVE'||park.landscape!=='kashyyyk'||
               !targetAdversary||!this.state.world?.publishedAdventure||
               combat.round!==park.started.combatRound+park.rounds.length)
                throw Error('Inseto de Kashyyyk exige alvo real, paisagem ativa e confirmação do Mestre antes da rolagem.');
            const augmented=augmentJewelKashyyykInsectRoll(check,rng);
            check=augmented.check;kashyyykInsectProof=augmented.proof;
        }
        if(weaponQualityModifiers.limitedAmmo>0) attackWeapon.runtimeState.limitedAmmoRemaining=Math.max(0,Number(attackWeapon.runtimeState.limitedAmmoRemaining)-1);
        if(bracePending)delete combat.aorBracePrepared;
        if(weaponQualityModifiers.prepare>0) attackWeapon.runtimeState.preparedManeuvers=0;
        if(weaponQualityModifiers.slowFiring>0&&combat.active) attackWeapon.runtimeState.lastFiredRound=Number(combat.round);
        effectiveAttackWeapon.runtimeState=clone(attackWeapon.runtimeState);
        const juryRigged=talents_mjs_1.juryRiggedWeapon(character,effectiveAttackWeapon);
        effectiveAttackWeapon=juryRigged.weapon;
        let attack = { ...weapons_mjs_1.resolveWeaponHit(effectiveAttackWeapon, check,{useAutoFire}),juryRigged:juryRigged.applied, targetRange: resolvedRange, difficulty: upgraded.difficulty, challenge: upgraded.challenge, quickStrikeBoost: quickStrike.boost, stunSettingActive };
        const talentDamage = attack.hit ? talents_mjs_1.personalCombatTalentDamage(character, { weapon, targetRange: resolvedRange }) : {total:0,details:[]};
        const damageDetails=clone(talentDamage.details??[]);
        let talentDamageBonus=Number(talentDamage.total??0);
        const deadly=talents_mjs_1.deadlyAccuracySpec(character);
        if(attack.hit&&deadly.available&&deadly.skillKey===weapon.skill&&deadly.bonus>0){talentDamageBonus+=deadly.bonus;damageDetails.push({talent:'Precisão letal',amount:deadly.bonus,skillKey:deadly.skillKey});}
        let destinyTalent=null;
        if(attack.hit&&useTargetedBlow){const opt=talents_mjs_1.targetedBlowOption(character);if(!opt.available)throw new Error('Golpe Direcionado não foi adquirido.');const bonus=Math.max(0,Number(character.characteristics?.agilidad??0));const group=destiny_mjs_1.flipDestiny(this.state.group,'light');this.commit('GROUP_UPDATED',{group});talentDamageBonus+=bonus;destinyTalent={talent:'Golpe direcionado',amount:bonus};damageDetails.push(destinyTalent);}
        if(attack.hit&&useAnatomyLessons){const opt=talents_mjs_1.anatomyLessonsOption(character);if(!opt.available)throw new Error('Anatomia não foi adquirida.');const bonus=Math.max(0,Number(character.characteristics?.intelecto??0));const group=destiny_mjs_1.flipDestiny(this.state.group,'light');this.commit('GROUP_UPDATED',{group});talentDamageBonus+=bonus;destinyTalent={talent:'Anatomia',amount:bonus};damageDetails.push(destinyTalent);}
        if(attack.hit&&useSoftSpot){const bonus=Math.max(0,Number(character.characteristics?.astucia??0));const group=destiny_mjs_1.flipDestiny(this.state.group,'light');this.commit('GROUP_UPDATED',{group});talentDamageBonus+=bonus;destinyTalent={talent:'Ponto débil',amount:bonus};damageDetails.push(destinyTalent);}
        if (talentDamageBonus > 0) attack = { ...attack, damage: attack.damage + talentDamageBonus, talentDamageBonus, talentDamageDetails:damageDetails, destinyTalent };
        else attack = { ...attack, talentDamageBonus: 0, talentDamageDetails:[], destinyTalent:null };
        attack={...attack,attackId:ids_mjs_1.newId('atk'),attackRound:combat.round,weaponIndex:Number(weaponIndex)};
        combat = clone(combat);
        if(combat.playerTurn?.pendingFrenziedAttack){combat.playerTurn={...(combat.playerTurn??{})};delete combat.playerTurn.pendingFrenziedAttack;}
        combat.pendingAttackBoost = 0;
        delete combat.pendingAim;
        delete combat.pendingPreciseAim;
        delete combat.pendingSniperShot;
        combat.pendingPlayerCritical = null;
        combat.pendingPlayerSpend = null;
        let targetDamage = null;
        if (targetAdversary) {
            const effectivePierce=Math.max(0,Number(attack.pierce??0)+(Math.max(0,Number(attack.breach??0))*10));
            if(attack.hit){
                targetDamage = adversaries_mjs_1.applyAdversaryDamage(targetAdversary, { damage: attack.damage, pierce: effectivePierce, damageType: attack.damageType });
                if(useCripplingBlow&&targetDamage.suffered>0&&attack.damageType==='wounds'){
                  targetDamage.adversary={...targetDamage.adversary,aorCripplingBlow:{sourceAttackId:attack.attackId,
                    encounterRound:combat.round,strainPerMove:1}};
                }
                combat = encounter_mjs_1.updateAdversaryInCombat(combat, targetDamage.adversary);
            }
            const supported=new Set(['autofire','blast','disorient','stun','burn','concussive','ensnare','sunder','linked','knockdown']);
            const qualities=weapons_mjs_1.activeWeaponQualities(effectiveAttackWeapon,{hit:Boolean(attack.hit),useAutoFire:Boolean(useAutoFire)}).filter(q=>supported.has(q.key)).map(q=>q.key==='blast'?{...q,cost:talents_mjs_1.masterGrenadierBlastCost(character,Number(q.cost)).cost}:q);
            combat.pendingPlayerSpend={
                attackId:attack.attackId,adversaryId:targetAdversary.id,weaponIndex:Number(weaponIndex),weapon:weapon.name,
                remainingAdvantage:Math.max(0,Number(check.netAdvantage??0)),remainingTriumph:Math.max(0,Number(check.triumph??0)),
                qualities:clone(qualities),activations:{},attackSnapshot:{hit:Boolean(attack.hit),cripplingBlow:Boolean(useCripplingBlow),stunningBlow:Boolean(useStunningBlow),skillKey:weapon.skill,damage:Number(attack.damage??0),pierce:effectivePierce,damageType:attack.damageType,vicious:Number(attack.vicious??0),baseDamage:Number(effectiveAttackWeapon.damage??0),netSuccess:Math.max(0,Number(check.netSuccess??0))}
            };
            const finalDamage=targetDamage;
            if (attack.hit && finalDamage?.suffered > 0 && attack.criticalAvailable && !finalDamage.status.incapacitated) {
                combat.pendingPlayerCritical = { adversaryId: targetAdversary.id, weapon: weapon.name, cost: attack.criticalCost, byTriumph: Boolean(attack.criticalByTriumph), vicious:Number(attack.vicious??0) };
            }
            attack = { ...attack, targetAdversaryId: targetAdversary.id, targetName: targetAdversary.name, targetDamage, autoFireHits:[], effectivePierce, pendingSpend:clone(combat.pendingPlayerSpend) };
        }
        if(kashyyykInsectProof){
            const recorded=recordJewelKashyyykInsect(this.state.world,combat,attack,kashyyykInsectProof,{gmInsectConfirmed});
            combat=recorded.combat;
            attack={...attack,kashyyykInsectReceipt:recorded.receipt,
                pendingSpend:combat.pendingPlayerSpend?clone(combat.pendingPlayerSpend):null};
            this.commit('WORLD_UPDATED',{world:recorded.world});
        }
        character = applyActionCriticalStrain(character, critical);
        if(this.state.combat?.publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&
           this.state.combat?.publishedAdventureContext?.encounterId==='lylek-den-bounty-hunters'){
            combat.swe02OldLylekFightChecks=[...(combat.swe02OldLylekFightChecks||[]),
                {round:combat.round,netAdvantage:Number(check.netAdvantage)||0,sourcePage:18}];
            if(Number(check.netAdvantage)>0)combat.swe02OldLylekAdvantageTriggered=true;
        }
        if(combat.active&&attackWeapon.defectiveUntilFirstUse===true&&attackWeapon.ownedAssetId){
            const characterId=this.state.player?.roster?.activeCharacterId;
            if(characterId){combat.swe02NynDefectiveUses=Array.isArray(combat.swe02NynDefectiveUses)?combat.swe02NynDefectiveUses:[];
                if(!combat.swe02NynDefectiveUses.some(x=>x.characterId===characterId&&x.ownedAssetId===attackWeapon.ownedAssetId))
                    combat.swe02NynDefectiveUses.push({characterId,ownedAssetId:attackWeapon.ownedAssetId,kind:'weapon'});}
        }
        const lylekEventResult=this._applySWE02OldLylekAdvantage(combat,check,{preferredHunterId:targetAdversaryId});
        combat=lylekEventResult.combat;
        if(lylekEventResult.event)attack={...attack,lylekEvent:clone(lylekEventResult.event)};
        this.commit('COMBAT_ACTION', { combat, character, check, attack });
        return attack;
        });
    }
    activatePendingPlayerCritical({ rng = Math.random, useTriumph = false } = {}) {
        let combat = clone(this.state.combat);
        const pending = combat.pendingPlayerCritical;
        if (!pending)
            throw new Error('Não há Ferimento Crítico disponível para ativar.');
        const target = combat.adversaries.find(a => a.id === pending.adversaryId);
        if (!target)
            throw new Error('O alvo do crítico não está mais no encontro.');
        const spend=combat.pendingPlayerSpend;
        let paidWith='legacy';
        if(spend&&String(spend.adversaryId)===String(pending.adversaryId)){
            const cost=Math.max(0,Number(pending.cost??0));
            if(useTriumph&&pending.byTriumph){
                if(Number(spend.remainingTriumph??0)<1) throw new Error('Não há Triunfo disponível para ativar este crítico.');
                spend.remainingTriumph-=1; paidWith='triumph';
            } else if(Number(spend.remainingAdvantage??0)>=cost){
                spend.remainingAdvantage-=cost; paidWith='advantage';
            } else if(pending.byTriumph&&Number(spend.remainingTriumph??0)>=1){
                spend.remainingTriumph-=1; paidWith='triumph';
            } else throw new Error('Resultados insuficientes para ativar o Ferimento Crítico.');
        }
        const lethalBlows=talents_mjs_1.lethalBlowsCriticalModifier(this.state.player.character);
        const criticalModifier=Math.max(0,Number(pending.vicious??0))*10+Number(lethalBlows.modifier??0);
        const result = adversaries_mjs_1.applyAdversaryCritical(target, `Crítico ativado por ${this.state.player.character?.name ?? 'PJ'}`, { modifier: criticalModifier, rng });
        result.lethalBlows=lethalBlows;
        result.criticalModifier=criticalModifier;
        combat = encounter_mjs_1.updateAdversaryInCombat(combat, result.adversary);
        combat.pendingPlayerCritical = null;
        if(spend) combat.pendingPlayerSpend=spend;
        combat.log.push({ type: 'CRITICO_ATIVADO', round: combat.round, targetId: target.id, woundsAdded: result.woundsAdded, paidWith });
        this.commit('COMBAT_UPDATED', { combat });
        return {...result,paidWith,remainingAdvantage:spend?.remainingAdvantage??null,remainingTriumph:spend?.remainingTriumph??null};
    }
    /** P177: Improved Stunning Blow spends the original attack Triumph, never a
     * fabricated roll, and applies a real staggered status to the hit NPC. */
    activatePendingImprovedStunningBlow({triumphs=1}={}){
      return this.atomicAction(()=>{
        const c=requireCharacter(this.state.player.character);
        if(talents_mjs_1.sourceBackedTalentRank(c,'STUNBLIMP')<1)throw Error('Golpe comocionador aprimorado não adquirido.');
        if(!Number.isSafeInteger(triumphs)||triumphs<1||triumphs>5)throw Error('Quantidade de Triunfos inválida.');
        let combat=clone(this.state.combat),spend=combat?.pendingPlayerSpend;
        if(!combat.active||!spend||spend.activations?.improvedStunningBlow)throw Error('Ataque com Triunfos pendentes indisponível.');
        if(!spend.attackSnapshot?.hit||spend.attackSnapshot.damageType!=='strain'||
          !['pelea','armas_cuerpo_a_cuerpo'].includes(spend.attackSnapshot.skillKey)||
          spend.remainingTriumph<triumphs)throw Error('Golpe exige acerto de Tensão corpo a corpo e Triunfos suficientes.');
        const target=combat.adversaries?.find(a=>a.id===spend.adversaryId);
        if(!target||adversaries_mjs_1.adversaryHealthStatus(target).incapacitated)throw Error('Alvo indisponível.');
        const affected=statusEffects_mjs_1.applyStatusEffect(target,{type:'staggered',duration:'rounds',rounds:triumphs,
          source:'talent',sourceId:`${spend.attackId}:stunning`});
        combat=encounter_mjs_1.updateAdversaryInCombat(combat,affected);
        spend.remainingTriumph-=triumphs;spend.activations={...(spend.activations??{}),improvedStunningBlow:true};
        combat.pendingPlayerSpend=spend;
        combat.log.push({type:'STUNNING_BLOW_IMPROVED',round:combat.round,attackId:spend.attackId,targetId:target.id,triumphs});
        this.commit('COMBAT_UPDATED',{combat});return {target:affected,triumphs,remainingTriumph:spend.remainingTriumph};
      });
    }
    /** P149: Disorient talent spends the SAME attack Advantage ledger used by
     * criticals and weapon qualities. Persist target status and remaining
     * Advantage in one combat commit, with one activation per attack. */
    activatePendingDisorientTalent() {
        return this.atomicAction(() => {
        let combat=clone(this.state.combat);
        const spend=combat?.pendingPlayerSpend;
        if (!combat?.active || !spend) throw new Error('Não há ataque ativo com Vantagens pendentes.');
        if (spend.activations?.talentDisorient) throw new Error('Desorientar já foi ativado neste ataque.');
        const option=talents_mjs_1.disorientTalentOption(this.state.player?.character,{
            hit:spend.attackSnapshot?.hit,advantage:spend.remainingAdvantage});
        if (option.rank<1) throw new Error('Talento Desorientar não foi adquirido.');
        if (!option.hit) throw new Error('Desorientar exige ataque bem-sucedido.');
        if (!option.available) throw new Error('Vantagens insuficientes para Desorientar.');
        const target=combat.adversaries?.find(a=>String(a.id)===String(spend.adversaryId));
        if (!target || adversaries_mjs_1.adversaryHealthStatus(target).incapacitated)
            throw new Error('Alvo de Desorientar indisponível.');
        const affected=statusEffects_mjs_1.applyStatusEffect(target,{
            type:'disoriented',duration:'rounds',rounds:option.rounds,
            source:'talent',sourceId:`${spend.attackId}:talent-disorient`});
        combat=encounter_mjs_1.updateAdversaryInCombat(combat,affected);
        spend.remainingAdvantage-=option.cost;
        spend.activations={...(spend.activations??{}),talentDisorient:1};
        combat.pendingPlayerSpend=spend;
        combat.log.push({type:'TALENTO_DESORIENTAR',round:combat.round,
            attackId:spend.attackId,targetId:target.id,rounds:option.rounds,cost:option.cost});
        this.commit('COMBAT_UPDATED',{combat});
        return {target:clone(affected),rounds:option.rounds,cost:option.cost,
            remainingAdvantage:spend.remainingAdvantage};
        });
    }
    /** P154: Knockdown talent uses one Triumph on a successful melee hit.
     * Weapon Knockdown (Advantage) remains independent and cannot be
     * substituted by this talent. Reopen/retry cannot spend twice.
     */
    activatePendingKnockdownTalent() {
        return this.atomicAction(() => {
        let combat=clone(this.state.combat);
        const spend=combat?.pendingPlayerSpend;
        if(!combat?.active || !spend) throw new Error('Não há ataque ativo com resultados pendentes.');
        if(spend.activations?.talentKnockdown) throw new Error('Derrubada já foi ativada neste ataque.');
        const option=talents_mjs_1.knockdownTalentOption(this.state.player?.character,{
            hit:spend.attackSnapshot?.hit,skillKey:spend.attackSnapshot?.skillKey,
            triumph:spend.remainingTriumph});
        if(option.rank<1) throw new Error('Talento Derrubada não foi adquirido.');
        if(!option.hit || !option.melee) throw new Error('Derrubada exige acerto corpo a corpo.');
        if(!option.available) throw new Error('Triunfo insuficiente para Derrubada.');
        const target=combat.adversaries?.find(a=>String(a.id)===String(spend.adversaryId));
        if(!target || adversaries_mjs_1.adversaryHealthStatus(target).incapacitated)
            throw new Error('Alvo de Derrubada indisponível.');
        const affected=statusEffects_mjs_1.applyStatusEffect(target,{
            type:'prone',duration:'encounter',rounds:1,
            source:'talent',sourceId:`${spend.attackId}:talent-knockdown`});
        combat=encounter_mjs_1.updateAdversaryInCombat(combat,affected);
        spend.remainingTriumph-=option.costTriumph;
        spend.activations={...(spend.activations??{}),talentKnockdown:1};
        combat.pendingPlayerSpend=spend;
        combat.log.push({type:'TALENTO_DERRUBADA',round:combat.round,
            attackId:spend.attackId,targetId:target.id,costTriumph:option.costTriumph});
        this.commit('COMBAT_UPDATED',{combat});
        return {target:clone(affected),costTriumph:option.costTriumph,
            remainingTriumph:spend.remainingTriumph};
        });
    }
    activatePendingWeaponQuality({ qualityKey, targetWeaponIndex = 0, secondaryTargetIds = [], excludedTargetIds = [], useSelectiveTriumph = false } = {}) {
        return this.atomicAction(() => {
        let combat=clone(this.state.combat);
        const spend=combat.pendingPlayerSpend;
        if(!spend) throw new Error('Não há ataque com resultados pendentes para gastar.');
        const quality=(spend.qualities??[]).find(q=>q.key===qualityKey);
        if(!quality) throw new Error('Qualidade ativa indisponível para este ataque.');
        const activations=Number(spend.activations?.[quality.key]??0);
        if(!quality.repeatable&&activations>0) throw new Error('Esta qualidade já foi ativada neste ataque.');
        if(quality.key==='linked'&&activations>=Number(quality.rank??0)) throw new Error('Todos os acertos Vinculados disponíveis já foram ativados.');
        const targetIndex=combat.adversaries.findIndex(a=>String(a.id)===String(spend.adversaryId));
        if(targetIndex<0) throw new Error('Alvo do ataque não encontrado.');
        let target=clone(combat.adversaries[targetIndex]);
        let cost=Math.max(0,Number(quality.cost??0));
        // The attack receipt, not a caller-supplied discount, is authoritative.
        // Blast activation cost is already snapshotted after Master Grenadier in the attack receipt.
        if(quality.key!=='blast'&&((Array.isArray(excludedTargetIds)&&excludedTargetIds.length>0)||useSelectiveTriumph))
            throw new Error('Detonação seletiva exige Explosão.');
        if(quality.key==='knockdown'){
            const attackerSilhouette=personalSilhouette(this.state.player?.character);
            const targetSilhouette=personalSilhouette(target);
            cost=2+Math.max(0,targetSilhouette-attackerSilhouette);
        }
        if(Number(spend.remainingAdvantage??0)<cost) throw new Error('Vantagens insuficientes para ativar esta qualidade.');
        if(adversaries_mjs_1.adversaryHealthStatus(target).incapacitated) throw new Error('O alvo já está incapacitado.');
        const snap=spend.attackSnapshot??{};
        let effect={key:quality.key,label:quality.label,rank:Number(quality.rank??0)};
        if(quality.key==='blast'){
            const ids=[...new Set((Array.isArray(secondaryTargetIds)?secondaryTargetIds:[]).map(String))].filter(id=>id!==String(target.id));
            if(snap.hit&&ids.length===0) throw new Error('Explosão em ataque bem-sucedido exige indicar ao menos um personagem Engajado com o alvo original.');
            if(!Array.isArray(secondaryTargetIds)||!Array.isArray(excludedTargetIds))throw new Error('Listas de alvos de Explosão inválidas.');
            if(new Set(secondaryTargetIds.map(String)).size!==secondaryTargetIds.length)throw new Error('Alvos de Explosão duplicados.');
            if(new Set(excludedTargetIds.map(String)).size!==excludedTargetIds.length)throw new Error('Exclusões de Explosão duplicadas.');
            const blastTargets=[];
            if(!snap.hit) blastTargets.push(String(target.id));
            blastTargets.push(...ids);
            const excluded=excludedTargetIds.map(String);
            if(excluded.some(id=>!blastTargets.includes(id)))throw new Error('Exclusão fora dos alvos afetados pela Explosão.');
            const selective=talents_mjs_1.selectiveDetonationOption(this.state.player?.character,{excluded:excluded.length,useTriumph:useSelectiveTriumph});
            if(Number(spend.remainingAdvantage??0)<cost+selective.advantageCost)throw new Error('Vantagens insuficientes para Explosão e exclusões.');
            if(Number(spend.remainingTriumph??0)<selective.triumphCost)throw new Error('Triunfo insuficiente para Detonação seletiva.');
            const talentBlastBonus=talents_mjs_1.powerfulBlastDamageBonus(this.state.player?.character,{weapon:spend.weapon});
            const blastDamage=Math.max(0,Number(quality.rank??0)+talentBlastBonus+(snap.hit?Math.max(0,Number(snap.netSuccess??0)):0));
            const results=[];
            for(const id of blastTargets.filter(id=>!excluded.includes(id))){
                const idx=combat.adversaries.findIndex(a=>String(a.id)===String(id));
                if(idx<0) throw new Error('Alvo secundário de Explosão não pertence ao encontro.');
                const victim=clone(combat.adversaries[idx]);
                const hit=adversaries_mjs_1.applyAdversaryDamage(victim,{damage:blastDamage,pierce:0,damageType:snap.damageType??'wounds'});
                combat=encounter_mjs_1.updateAdversaryInCombat(combat,hit.adversary);
                results.push({id:hit.adversary.id,name:hit.adversary.name,suffered:hit.suffered,status:hit.status});
            }
            target=clone(combat.adversaries.find(a=>String(a.id)===String(spend.adversaryId))??target);
            spend.remainingAdvantage-=selective.advantageCost;
            spend.remainingTriumph-=selective.triumphCost;
            effect={...effect,damage:blastDamage,talentBlastBonus,targets:results,excludedTargetIds:excluded,selectiveDetonation:selective,missBlast:!snap.hit};
        } else if(quality.key==='autofire'||quality.key==='linked'){
            const extra=adversaries_mjs_1.applyAdversaryDamage(target,{damage:Number(snap.damage??0),pierce:Number(snap.pierce??0),damageType:snap.damageType??'wounds'});
            target=extra.adversary; effect={...effect,damage:extra.suffered,status:extra.status};
        } else if(quality.key==='stun'){
            const strained=adversaries_mjs_1.sufferAdversaryStrain(target,Math.max(0,Number(quality.rank??0)));
            target=strained.adversary; effect={...effect,strain:Number(quality.rank??0),convertedToWounds:Boolean(strained.convertedToWounds)};
        } else if(quality.key==='disorient'||quality.key==='concussive'||quality.key==='ensnare'){
            const type=quality.key==='disorient'?'disoriented':quality.key==='concussive'?'staggered':'immobilized';
            target=statusEffects_mjs_1.applyStatusEffect(target,{type,duration:'rounds',rounds:Math.max(1,Number(quality.rank??1)),source:'weapon-quality',sourceId:`${spend.attackId}:${quality.key}`});
            effect={...effect,statusType:type,rounds:Math.max(1,Number(quality.rank??1))};
        } else if(quality.key==='knockdown'){
            const attackerSilhouette=personalSilhouette(this.state.player?.character);
            const targetSilhouette=personalSilhouette(target);
            target=statusEffects_mjs_1.applyStatusEffect(target,{type:'prone',duration:'encounter',rounds:1,source:'weapon-quality',sourceId:`${spend.attackId}:knockdown`});
            effect={...effect,statusType:'prone',cost,targetSilhouette,attackerSilhouette};
        } else if(quality.key==='burn'){
            const ongoing=Array.isArray(target.ongoingEffects)?target.ongoingEffects:[];
            ongoing.push({type:'burn',roundsRemaining:Math.max(1,Number(quality.rank??1)),damage:Math.max(0,Number(snap.baseDamage??snap.damage??0)),sourceId:`${spend.attackId}:burn`});
            target.ongoingEffects=ongoing; effect={...effect,rounds:Math.max(1,Number(quality.rank??1)),damage:Math.max(0,Number(snap.baseDamage??snap.damage??0))};
        } else if(quality.key==='sunder'){
            const idx=Number(targetWeaponIndex);
            const targetWeapon=target.weapons?.[idx];
            if(!targetWeapon) throw new Error('Item empunhado do alvo não encontrado para Hendedura.');
            const before=itemDamageState(targetWeapon.damageState);
            const after=nextItemDamageState(before);
            target.weapons[idx]={...targetWeapon,damageState:after};
            effect={...effect,itemIndex:idx,itemName:targetWeapon.name,before,after};
        } else throw new Error('Qualidade ativa ainda não suportada pelo runtime.');
        spend.remainingAdvantage-=cost;
        spend.activations={...(spend.activations??{}),[quality.key]:activations+1};
        combat.pendingPlayerSpend=spend;
        combat=encounter_mjs_1.updateAdversaryInCombat(combat,target);
        combat.log.push({type:'QUALIDADE_ATIVADA',round:combat.round,targetId:target.id,quality:quality.key,cost,effect:clone(effect)});
        this.commit('COMBAT_UPDATED',{combat});
        return {effect,target:clone(target),remainingAdvantage:spend.remainingAdvantage,remainingTriumph:spend.remainingTriumph,activations:spend.activations[quality.key]};
        });
    }
    applyPlayerDamage({ damage, pierce = 0, damageType = 'wounds', reason = 'Dano recebido', criticalRng = Math.random }) {
        const character = this.state.player.character;
        const result = health_mjs_1.applyDamage(character, { damage: Number(damage), pierce: Number(pierce), damageType, reason, criticalRng });
        if (this.state.combat?.active && Number(result.suffered ?? 0) > 0) {
            const combat = clearPlayerAim(this.state.combat);
            this.commit('COMBAT_ACTION', { combat, character: result.character });
        } else {
            this.commit('CHARACTER_UPDATED', { character: result.character });
        }
        return result;
    }
    sufferPlayerStrain(amount) {
        const result = health_mjs_1.sufferStrain(this.state.player.character, Number(amount));
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    usePlayerStimpack({ maneuverMethod = 'normal' } = {}) {
        let character = this.state.player.character;
        let combat = this.state.combat;
        assertCanAct(character);
        assertPlayerInitiativeTurn(combat);
        let maneuver = null;
        if (combat.active) {
            maneuver = spendPlayerManeuver(character, combat, { method: maneuverMethod, label: 'Usar injetor de estimulantes' });
            combat = maneuver.combat;
            character = maneuver.character;
        }
        const result = health_mjs_1.useStimpack(character);
        character = result.character;
        this.commit('COMBAT_ACTION', { combat, character });
        return { ...result, combat, strainCost: maneuver?.strainCost ?? 0, critical: maneuver?.critical ?? null, encumbrance: maneuver?.encumbrance ?? encumbrance_mjs_1.encumbranceStatus(character) };
    }
    useEmergencyRepairPatch() {
        let character = requireCharacter(this.state.player.character);
        assertCanPerformAction(character);
        assertPlayerInitiativeTurn(this.state.combat);
        let combat = this.state.combat.active ? clearPlayerAim(combat_mjs_1.spendAction(this.state.combat, 'Usar remendo de reparo de emergência')) : clone(this.state.combat);
        const result = health_mjs_1.useEmergencyRepairPatch(character);
        character = result.character;
        this.commit('COMBAT_ACTION', { combat, character });
        return { ...result, combat };
    }
    aorBraceStatus(){
        const character=this.state.player?.character,rank=character?talents_mjs_1.sourceBackedTalentRank(character,'BRACE'):0;
        const vehicleCombat=this.state.vehicles?.combat;
        const pending=this.state.combat?.aorBracePrepared??vehicleCombat?.aorBracePrepared??null;
        return {rank,combatActive:Boolean(this.state.combat?.active||vehicleCombat?.active),vehicleCombatActive:Boolean(vehicleCombat?.active),prepared:Boolean(pending),pending:clone(pending),source:{productCode:'AOR',printedPage:68}};
    }
    prepareAoRBrace({maneuverMethod='normal'}={}){
        let character=requireCharacter(this.state.player?.character),combat=this.state.combat;
        const option=talents_mjs_1.braceEnvironmentalSetbackRelief(character,{maneuverPrepared:true,environmentalSetback:0});
        if(!option.available||option.rank<1)throw new Error('Brace (Firmeza) não foi adquirido com fonte AoR ativa.');
        if(!combat?.active&&!this.state.vehicles?.combat?.active)throw new Error('Brace exige encontro ativo.');
        if(combat?.aorBracePrepared||this.state.vehicles?.combat?.aorBracePrepared)throw new Error('Brace já está preparado para a próxima ação.');
        const characterId=this.state.player?.roster?.activeCharacterId??character.id;
        if(!characterId)throw new Error('Brace exige a identidade persistente da ficha ativa.');
        const source={productCode:'AOR',printedPage:68,nodeIds:(character.talentPurchases??[]).filter(x=>String(x.nodeId??'').startsWith('ace:gunner:')&&String(x.source?.book??'').includes('Age of Rebellion')&&x?.source?.printedPage===68&&String(x.originalName??x.name??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toUpperCase()==='FIRMEZA').map(x=>x.nodeId).filter(Boolean)};
        if(combat?.active){
          const spent=spendPlayerManeuver(character,combat,{method:maneuverMethod,label:'Brace'});
          combat={...spent.combat,aorBracePrepared:{characterId,rank:option.rank,preparedAtRound:Number(spent.combat.round??0),source}};
          this.commit('COMBAT_ACTION',{combat,character:spent.character});
          return {combat:clone(combat),character:clone(spent.character),...option,source:clone(combat.aorBracePrepared.source)};
        }
        this._assertSWE02VehicleTurn('player');
        const vehicles=clone(this.state.vehicles),vehicleCombat=vehicles.combat,active=vehicles.vehicles?.[vehicles.activeVehicleId];
        if(!vehicleCombat?.active||!active)throw new Error('Brace exige combate veicular e nave ativa.');
        const vehicleStatus=vehicles_mjs_1.vehicleStatus(active,{currentRound:vehicleCombat.round});
        if(vehicleStatus.destroyed||vehicleStatus.noManeuvers)throw new Error('O estado do veículo impede gastar uma manobra de Brace.');
        const used=Number(vehicleCombat.maneuversUsed??0);
        if(used>=2)throw new Error('A nave/veículo já recebeu 2 manobras neste assalto.');
        if(used===1){
          if(maneuverMethod==='strain'){
            if(!vehicles_mjs_1.canVoluntarilyInflictVehicleStrain(active))throw new Error('Não é possível sofrer Tensão de sistemas voluntariamente.');
            active.systemStrain+=2;
          }else if(maneuverMethod==='action'){
            if(vehicleCombat.actionUsed)throw new Error('A ação deste assalto já foi usada.');
            vehicleCombat.actionUsed=true;
          }else throw new Error('A segunda manobra veicular exige 2 de Tensão de sistemas ou trocar a ação.');
        }
        vehicleCombat.maneuversUsed=used+1;
        vehicleCombat.aorBracePrepared={characterId,rank:option.rank,preparedAtRound:Number(vehicleCombat.round??0),source};
        vehicles.vehicles[active.id]=active;
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {combat:clone(vehicleCombat),vehicle:clone(active),character:clone(character),...option,source:clone(source)};
    }
    aim({ maneuverMethod = 'normal', mode = 'boost', targetDetail = '' } = {}) {
        let character = this.state.player.character;
        assertCanAct(character);
        assertPlayerInitiativeTurn(this.state.combat);
        const requestedMode=String(mode??'boost');
        const normalizedMode = requestedMode === 'targeted' ? 'targeted' : requestedMode === 'true_aim' ? 'true_aim' : 'boost';
        if(normalizedMode==='true_aim'){
            if(!this.state.combat?.active) throw new Error('Mira Verdadeira exige combate ativo.');
            if(!talents_mjs_1.trueAimOption(character).available) throw new Error('Mira Verdadeira não foi adquirida.');
            if(this.state.combat.playerTurn?.trueAimUsed) throw new Error('Mira Verdadeira só pode ser usada uma vez por rodada.');
        }
        const detail = String(targetDetail ?? '').trim();
        if (normalizedMode === 'targeted' && !detail) throw new Error('Informe o item ou parte específica que está sendo visada.');
        const movement = spendPlayerManeuver(character, this.state.combat, { method: maneuverMethod, label: 'Mirar' });
        let combat = movement.combat;
        character = movement.character;
        const previous = combat.pendingAim ?? null;
        const sameTarget = normalizedMode === 'boost' || String(previous?.targetDetail ?? '') === detail;
        const count = previous?.mode === normalizedMode && sameTarget ? Math.min(2, Number(previous?.count ?? 0) + 1) : 1;
        combat.pendingAim = { mode: normalizedMode, count, targetDetail: normalizedMode === 'targeted' ? detail : null };
        combat.pendingAttackBoost = normalizedMode === 'boost' ? count : normalizedMode === 'true_aim' ? 1 : 0;
        if(normalizedMode==='true_aim') combat.playerTurn={...(combat.playerTurn??{}),trueAimUsed:true};
        this.commit('COMBAT_ACTION', { combat, character });
        return { ...movement, combat, character, aim: clone(combat.pendingAim) };
    }
    /** P164 — persistent, explicitly custom companions. No sourcebook NPC is
     * fabricated: a player-created sheet is a local character, not lore. */
    companionRoster(){
        return clone(this.state.group?.companions??[]);
    }
    addCompanion({name,woundsThreshold=12,strainThreshold=12}={}){
        return this.atomicAction(()=>{
            const title=typeof name==='string'?name.trim():'';
            if(!title||title.length>120)throw new Error('Informe um nome de companheiro válido.');
            if(!Number.isSafeInteger(woundsThreshold)||woundsThreshold<1||woundsThreshold>100||
               !Number.isSafeInteger(strainThreshold)||strainThreshold<1||strainThreshold>100)
                throw new Error('Limiares de companheiro inválidos.');
            const group=clone(this.state.group),members=group.companions??[];
            if(members.length>=8)throw new Error('Limite de oito companheiros atingido.');
            const character=character_mjs_1.createBlankCharacter(title);
            character.source={type:'user_custom',official:false,note:'Ficha simplificada criada pelo jogador, sem atributos oficiais de NPC.'};
            character.derived.wounds.threshold=woundsThreshold;
            character.derived.strain.threshold=strainThreshold;
            const member={id:ids_mjs_1.newId('companion'),character};
            members.push(member);group.companions=members;
            fringeGroup_mjs_1.validateCompanions(members);
            this.commit('GROUP_UPDATED',{group});
            return clone(member);
        });
    }
    removeCompanion(companionId){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw new Error('Encerre o combate antes de remover companheiros.');
            const group=clone(this.state.group),before=(group.companions??[]).length;
            if((group.companions??[]).some(x=>x.id===companionId&&(x.character?.gear??[]).some(item=>item?.id==='swe09-jewel-of-yavin-gem')))
                throw new Error('Não é possível remover companheiro que carrega a Joia de Yavin; transfira-a primeiro.');
            group.companions=(group.companions??[]).filter(x=>x.id!==companionId);
            if(group.companions.length===before)throw new Error('Companheiro não encontrado.');
            this.commit('GROUP_UPDATED',{group});
            return {removed:companionId};
        });
    }
    _medicalCompanion(companionId){
        if(companionId===null||companionId===undefined||companionId==='')return null;
        if(typeof companionId!=='string')throw new Error('Identificador de companheiro inválido.');
        const member=(this.state.group?.companions??[]).find(x=>x.id===companionId);
        if(!member)throw new Error('Companheiro não encontrado.');
        return member;
    }
    /** A real wound event for a roster member; no direct arbitrary healing. */
    applyCompanionDamage({companionId,damage,reason='Dano na cena',criticalRng=Math.random,useItsNotThatBad=false,approved=false,medicineRng=Math.random}={}){
        return this.atomicAction(()=>{
            const member=this._medicalCompanion(companionId);
            if(!member)throw new Error('Selecione um companheiro.');
            if(!Number.isSafeInteger(damage)||damage<1||damage>100)throw new Error('Dano de companheiro inválido.');
            let medicCharacter=requireCharacter(this.state.player.character);
            if(useItsNotThatBad){
              if(approved!==true||!['AOR','MIXED'].includes(campaignMode(this.state))||this.state.session?.status!=='ativa'||
                 talents_mjs_1.sourceBackedTalentRank(medicCharacter,'ITS_NOT_THAT_BAD')<1||
                 talentUsedThisSession(medicCharacter,'itsNotThatBad')||
                 health_mjs_1.getHealthStatus(medicCharacter).unconscious)
                throw Error('Não é tão grave exige médico apto, talento e confirmação do Mestre.');
            }
            let medicineCheck=null;
            const onBeforeCritical=useItsNotThatBad?()=>{
              const medPool=narrativeDice_mjs_1.buildPositivePool(Number(medicCharacter.characteristics?.intelecto??0),Number(medicCharacter.skills?.medicina??0));
              const check=narrativeDice_mjs_1.rollPool({...medPool,difficulty:3},medicineRng);medicineCheck=check;
              medicCharacter=markTalentUsedThisSession(medicCharacter,'itsNotThatBad');
              return {prevented:check.netSuccess>0,talent:'ITS_NOT_THAT_BAD',check};
            }:null;
            const result=health_mjs_1.applyDamage(member.character,{damage,reason,criticalRng,onBeforeCritical});
            const group=clone(this.state.group);
            group.companions.find(x=>x.id===companionId).character=result.character;
            const attempted=useItsNotThatBad&&talentUsedThisSession(medicCharacter,'itsNotThatBad');
            if(attempted){
              const combat=clone(this.state.combat);
              combat.log.push({type:'AOR_ITS_NOT_THAT_BAD',round:combat.round,sessionId:this.state.sessionId,
                companionId,success:Boolean(result.criticalPrevention),
                check:clone(medicineCheck)});
              this.commit('COMBAT_ACTION',{combat,character:medicCharacter,group});
            }else this.commit('GROUP_UPDATED',{group});
            return {...result,companionId};
        });
    }
    /** P163 — one combat action, real gear, actual Medicine dice and
     * encounter-only characteristic buffs; the same commit stores all costs. */
    applyStimApplication({characteristicKey='agilidad',additionalCharacteristics=[],improved=false,targetCompanionId=null,rng=Math.random}={}){
        return this.atomicAction(() => {
        const character=requireCharacter(this.state.player.character);
        assertCanPerformAction(character);
        if(!this.state.combat?.active)throw new Error('Aplicação de estimulantes exige encontro ativo.');
        assertPlayerInitiativeTurn(this.state.combat);
        const target=this._medicalCompanion(targetCompanionId);
        const patient=target?.character??character;
        let combat=clearPlayerAim(combat_mjs_1.spendAction(this.state.combat,'Aplicação de estimulantes'));
        // P233: the pending Kashyyyk vertigo applies to this player's actual
        // Medicine dice, not to the companion receiving treatment or an NPC.
        const basePositive=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.intelecto??0),Number(character.skills?.medicina??0));
        const vertigo=consumeJewelHoloVertigo(combat,basePositive,{actionKind:'PLAYER_STIM_APPLICATION'});
        combat=vertigo.combat;
        const result=recovery_mjs_1.resolveStimApplication(character,patient,{characteristicKey,additionalCharacteristics,improved,vertigoDowngrade:vertigo.downgraded??false,rng});
        combat=attachJewelHoloVertigoRoll(combat,result.check,'PLAYER_STIM_APPLICATION');
        if(target){
            const group=clone(this.state.group);
            group.companions.find(x=>x.id===target.id).character=result.character;
            this.commit('COMBAT_ACTION',{combat,character,group,check:result.check});
        }else this.commit('COMBAT_ACTION',{combat,character:result.character,check:result.check});
        return {...result,combat,targetCompanionId:target?.id??null};
        });
    }
    /** Facility is explicit: no automatic free healing from the menu. */
    recoverInBacta({hours,facilityConfirmed=false,targetCompanionId=null}={}){
        return this.atomicAction(() => {
        if(this.state.combat?.active)throw new Error('Encerre o combate antes da recuperação em bacta.');
        if(facilityConfirmed!==true)throw new Error('Confirme a presença de um tanque de bacta na cena.');
        const character=requireCharacter(this.state.player.character);
        const target=this._medicalCompanion(targetCompanionId);
        const result=recovery_mjs_1.recoverInBactaTank(target?.character??character,hours,{supervisor:character});
        if(target){
            const group=clone(this.state.group);
            group.companions.find(x=>x.id===target.id).character=result.character;
            this.commit('GROUP_UPDATED',{group});
        }else this.commit('CHARACTER_UPDATED',{character:result.character});
        return {...result,targetCompanionId:target?.id??null};
        });
    }
    useMedicine({ medicalSupplies = true, medicalItemId = null, useMasterDoctor=false,
        targetCompanionId=null, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        let character = requireCharacter(this.state.player.character);
        const target=this._medicalCompanion(targetCompanionId);
        const patient=target?.character??character;
        assertCanPerformAction(character);
        assertPlayerInitiativeTurn(this.state.combat);
        if (this.state.combat.active && this.state.combat.medicineUsed)
            throw new Error('A tentativa de Medicina deste encontro já foi usada.');
        if(typeof useMasterDoctor!=='boolean')throw new Error('Opção de Médico especialista inválida.');
        const master=talents_mjs_1.masterSkillDifficultyOption(character,'doctor');
        if(useMasterDoctor&&!master.available)throw new Error('Médico especialista não foi adquirido.');
        if(useMasterDoctor&&this.state.combat?.active&&this.state.combat?.playerTurn?.skillMasterUses?.doctor)
            throw new Error('Médico especialista já foi usado nesta rodada.');
        let combat = this.state.combat.active ? clearPlayerAim(combat_mjs_1.spendAction(this.state.combat, 'Medicina')) : clone(this.state.combat);
        // Companion treatment cannot claim medical supplies that the healer
        // does not own. Legacy self-treatment remains backward compatible.
        if(target && medicalSupplies){
            const supplies=(character.gear??[]).some(item=>Number(item?.quantity??1)>0 &&
                ['medpac','emergency_medpac','medical_kit'].includes(item?.id??item?.sourceCatalogId));
            if(!supplies)throw new Error('Tratamento de companheiro exige suprimentos médicos possuídos.');
        }
        if(target && medicalItemId && !(character.gear??[]).some(item=>
            (item?.id??item?.sourceCatalogId)===medicalItemId && Number(item?.quantity??1)>0))
            throw new Error('Equipamento médico selecionado não está no inventário.');
        const medSkillKey = patient?.creation?.speciesKey === 'droid' ? 'mecanica' : 'medicina';
        const medCharacteristic = character_mjs_1.SKILLS[medSkillKey];
        const critical = critical_mjs_1.criticalCheckModifiers(character, { skillKey: medSkillKey, characteristicKey: medCharacteristic, consume: true });
        character = critical.character;
        if(useMasterDoctor){
            if(critical.cannotVoluntarilySufferStrain)throw new Error('Ferimento Crítico impede gastar Tensão voluntária.');
            character=health_mjs_1.sufferStrain(character,2,{voluntary:true}).character;
            assertCanAct(character);
            if(combat.active)combat.playerTurn={...(combat.playerTurn??{}),skillMasterUses:{...(combat.playerTurn?.skillMasterUses??{}),doctor:true}};
        }
        // P233: Medicine is a player ACTION even when healing a companion.
        // Use the exact characteristic after critical-wound penalties, then
        // consume once in the same atomic transaction as the real dice roll.
        const medPositive=narrativeDice_mjs_1.buildPositivePool(
            Math.max(0,Number(character.characteristics?.[medCharacteristic]??0)-Math.max(0,Number(critical.characteristicPenalty??0))),
            Number(character.skills?.[medSkillKey]??0));
        const vertigo=consumeJewelHoloVertigo(combat,medPositive,{actionKind:'PLAYER_MEDICINE'});
        combat=vertigo.combat;
        const result = recovery_mjs_1.resolveMedicineCheck(character, target?.character??character, {
            vertigoDowngrade:vertigo.downgraded??false,
            medicalSupplies, medicalItemId, difficultyDecrease:useMasterDoctor?1:0,
            setback:critical.setback, difficultyIncrease:critical.difficultyIncrease,
            difficultyUpgrades:critical.difficultyUpgrades, characteristicPenalty:critical.characteristicPenalty,
            removeBoost:critical.removeBoost, rng
        });
        combat=attachJewelHoloVertigoRoll(combat,result.check,'PLAYER_MEDICINE');
        if(this.state.combat.active)combat.medicineUsed=true;
        if(target){
            character=applyActionCriticalStrain(character,critical);
            const group=clone(this.state.group);
            group.companions.find(x=>x.id===target.id).character=result.character;
            this.commit('COMBAT_ACTION',{combat,character,group,check:result.check});
        }else{
            result.character=applyActionCriticalStrain(result.character,critical);
            this.commit('COMBAT_ACTION',{combat,character:result.character,check:result.check});
        }
        return {...result,masterDoctorUsed:useMasterDoctor,targetCompanionId:target?.id??null};
        });
    }
    applyPlayerCritical(reason = 'Ferimento crítico', options = {}) {
        const character = requireCharacter(this.state.player.character);
        const result = critical_mjs_1.addStarterCritical(character, reason, options);
        if (result.added)
            this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    restFullNight() {
        if (this.state.combat.active)
            throw new Error('Encerre o encontro antes de descansar uma noite completa.');
        const result = recovery_mjs_1.restFullNight(this.state.player.character);
        this.commit('CHARACTER_UPDATED', { character: result.character });
        return result;
    }
    healthStatus() {
        if (!this.state.player.character)
            return null;
        return health_mjs_1.getHealthStatus(this.state.player.character);
    }
    astrogationPlan(options = {}) {
        const character=requireCharacter(this.state.player.character);
        const vehicle=this.activeVehicle();
        if(!vehicle) throw new Error('Selecione uma nave antes de calcular o salto.');
        if(!vehicle.hyperdrive) throw new Error('A nave ativa não possui hiperpropulsor.');
        const status=vehicles_mjs_1.vehicleStatus(vehicle);
        if(status.hyperspaceDisabled) throw new Error('O hiperpropulsor está desativado por dano crítico.');
        const difficulty=astrogation_mjs_1.calculateAstrogationDifficulty({
            routeType:options.routeType??'MAJOR',
            hasNavicomputer:options.hasNavicomputer??Boolean(vehicle.navComputer),
            hasAstromech:Boolean(options.hasAstromech),
            starCharts:options.starCharts??'CURRENT',
            rushedJump:Boolean(options.rushedJump),
            damagedNavicomputer:Boolean(options.damagedNavicomputer??status.navigationDisabled),
            damagedHyperdrive:Boolean(options.damagedHyperdrive??status.hyperspaceDisabled),
            stellarHazard:Boolean(options.stellarHazard),
            customDifficulty:options.customDifficulty??null,
            customSetback:Number(options.customSetback??0),
            customBoost:Number(options.customBoost??0)
        });
        const skill='astronavegacion';
        const characteristicKey=character_mjs_1.SKILLS[skill]??'intelecto';
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.[characteristicKey]??0),Number(character.skills?.[skill]??0));
        return {vehicleId:vehicle.id,vehicleName:vehicle.name,hyperdriveClass:Number(vehicle.hyperdriveClass??1),skill,characteristicKey,positive,difficulty};
    }
    resolveHyperspaceJump(options = {}) {
        let character=requireCharacter(this.state.player.character);
        const plan=this.astrogationPlan(options);
        const pool={...plan.positive,...plan.difficulty.pool,boost:Number(plan.difficulty.pool.boost??0)+Number(options.boost??0),setback:Number(plan.difficulty.pool.setback??0)+Number(options.setback??0)};
        const check=narrativeDice_mjs_1.rollPool(pool,typeof options.rng==='function'?options.rng:Math.random);
        const outcome=astrogation_mjs_1.resolveAstrogationNarrativeOutcome({
            successes:Number(check.raw?.success??0)+Number(check.triumph??0),
            failures:Number(check.raw?.failure??0)+Number(check.despair??0),
            advantages:Number(check.raw?.advantage??0),threats:Number(check.raw?.threat??0),
            triumphs:Number(check.triumph??0),despairs:Number(check.despair??0),routeType:plan.difficulty.routeType,hyperdriveClass:plan.hyperdriveClass,
            baseHours:options.baseHours??null,mishapRoll:options.mishapRoll??null,rng:typeof options.rng==='function'?options.rng:Math.random
        });
        const vehicles=clone(this.state.vehicles);
        const vehicle=vehicles.vehicles?.[plan.vehicleId];
        if(!vehicle) throw new Error('A nave ativa não está mais disponível.');
        vehicle.systemStrain=Math.max(0,Number(vehicle.systemStrain??0)+Number(outcome.systemStrainSuffered??0));
        vehicle.hullDamage=Math.max(0,Number(vehicle.hullDamage??0)+Number(outcome.hullTraumaSuffered??0));
        if(outcome.hyperdriveDisabled&&!vehicle.disabledComponents.includes('hyperdrive')) vehicle.disabledComponents.push('hyperdrive');
        vehicles.travel=vehicles.travel??{};
        const destinationLocationId=String(options.destinationLocationId??'').trim()||null;
        if(destinationLocationId&&!this.state.world?.locations?.[destinationLocationId]) throw new Error('Destino hiperespacial não existe no Mundo Vivo.');
        const originLocationId=this.state.scene?.locationId??null;
        const destinationName=destinationLocationId?String(this.state.world.locations[destinationLocationId]?.name??destinationLocationId):String(options.destinationLabel??'').trim()||null;
        let timeAdvancedTicks=0;
        let arrivedLocationId=null;
        let worldResult=null;
        if(outcome.jumpSuccessful){
            timeAdvancedTicks=Math.max(1,Math.ceil(Number(outcome.travelTime?.effectiveHours??0)/6));
            worldResult=sandbox_mjs_1.advanceLivingWorld(this.state.world,{ticks:timeAdvancedTicks,maxEvents:Number(options.maxEvents??50)});
        }
        vehicles.travel.lastHyperspaceJump={
            at:new Date().toISOString(),routeType:plan.difficulty.routeType,vehicleId:plan.vehicleId,check:clone(check),outcome:clone(outcome),
            originLocationId,destinationLocationId,destinationLabel:destinationName,timeAdvancedTicks,
            arrived: Boolean(outcome.jumpSuccessful&&!outcome.offCourse&&destinationLocationId)
        };
        this.commit('VEHICLES_UPDATED',{vehicles});
        if(worldResult){
            this.commit('WORLD_UPDATED',{world:worldResult.world});
            if(destinationLocationId&&!outcome.offCourse){
                arrivedLocationId=destinationLocationId;
                const location=this.state.world.locations?.[destinationLocationId];
                this.commit('SCENE_UPDATED',{scene:{locationId:destinationLocationId,title:location?.name??destinationName??this.state.scene?.title??'',narration:'',visibleNpcIds:[]}});
            }
        }
        if(this.state.session?.status==='ativa') this.commit('SESSION_CHECK_RESOLVED',{session:sessionFlow_mjs_1.noteSessionCheck(this.state.session)});
        return {plan,check,outcome,timeAdvancedTicks,arrivedLocationId,scene:clone(this.state.scene),world:worldResult?clone(this.state.world):null,vehicle:clone(this.state.vehicles.vehicles[plan.vehicleId])};
    }
    vehicleProfiles() {
        return vehicles_mjs_1.STARTER_VEHICLE_PROFILES;
    }
    activeVehicle() {
        const id = this.state.vehicles?.activeVehicleId;
        return id ? this.state.vehicles.vehicles?.[id] ?? null : null;
    }
    addVehicle(profileKey, { setActive = true, id = null, name = null } = {}) {
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles_mjs_1.createVehicleFromProfile(profileKey, { id: id ?? `veh_${profileKey}`, name });
        vehicles.vehicles[vehicle.id] = vehicle;
        if (setActive)
            vehicles.activeVehicleId = vehicle.id;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return vehicle;
    }
    selectVehicle(vehicleId) {
        const vehicles = clone(this.state.vehicles);
        if (!vehicles.vehicles?.[vehicleId])
            throw new Error('Nave/veículo não encontrado.');
        vehicles.activeVehicleId = vehicleId;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return vehicles.vehicles[vehicleId];
    }
    /** SWE02 pp.7–11: persist shipboard discoveries and source-backed decisions. */
    setSWE02PeltSearchAnnounced(announced){
        return this.atomicAction(()=>{
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.setPeltSearchAnnounced(this.state.world,Boolean(announced));
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.result);
        });
    }
    recordSWE02ShipObservation(id,details={}){
        return this.atomicAction(()=>{
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            let character=null,normalized={...(details||{})};
            if(id==='lowhhrick-pelt-strain'&&details?.applyToActiveLowhhrick===true){
                character=structuredClone(requireCharacter(this.state.player?.character));
                if(!String(character.name||'').toLowerCase().includes('lowhhrick'))throw Error('SWE02 p.9: aplique os 2 pontos de Tensão apenas à ficha de Lowhhrick selecionada.');
                character=health_mjs_1.sufferStrain(character,2).character;
                normalized={...normalized,applied:true,characterId:character.id||character.name};
            }else if(id==='lowhhrick-pelt-strain')normalized={...normalized,applied:false,characterId:null};
            const out=adapter.recordShipObservation(this.state.world,id,normalized);
            if(character)this.commit('CHARACTER_UPDATED',{character});
            this.commit('WORLD_UPDATED',{world:out.world});return {receipt:out.result,issueState:out.issueState,character:character?clone(character):null};
        });
    }
    adjudicateSWE02ShipTransitExtraDuration({ticks}={}){
        return this.atomicAction(()=>{
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.adjudicateShipTransitExtraDuration(this.state.world,Number(ticks));
            const world=sandbox_mjs_1.advanceLivingWorld(out.world,{ticks:Number(ticks),maxEvents:50}).world;
            this.commit('WORLD_UPDATED',{world});
            return {transit:clone(out.result),clockTick:world.clock.tick};
        });
    }
    discoverSWE02ShipRoom(roomId){
        return this.atomicAction(()=>{
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.discoverShipRoom(this.state.world,roomId);
            this.commit('WORLD_UPDATED',{world:out.world});return {items:out.result,receipt:clone(out.progress.shipSequence.cargoReceipts.at(-1))};
        });
    }
    swe02ShipCargoTransferOptions(){
        const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
        return {items:adapter.shipCargoTransferOptions(this.state.world),pcs:(this.state.player?.roster?.entries??[]).map(x=>({id:x.id,name:x.character?.name??x.id}))};
    }
    transferSWE02ShipCargo({roomId,itemId,quantity,characterId}={}){
        return this.atomicAction(()=>{
            const roster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            const entry=roster.entries.find(x=>x.id===String(characterId??''));
            if(!entry||!entry.character)throw Error('SWE02 p.11: PC destinatário não existe no roster.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.transferShipCargo(this.state.world,{roomId,itemId,quantity:Number(quantity),characterId:entry.id});
            const transfer=out.result,gearSource=equipmentCatalog_mjs_1.generalGearCatalogEntry(transfer.catalogId);
            if(!gearSource||gearSource.source?.authority!=='PRIMARY_RULE_SOURCE'||gearSource.source?.rulesProfile!=='EOTE_CORE'||
                gearSource.source?.printedPage!==transfer.rulesSource.printedPage)
                throw Error('SWE02 p.11: mapeamento do equipamento não corresponde à fonte EotE Core registrada.');
            const character=clone(entry.character);character.gear=Array.isArray(character.gear)?character.gear:[];
            const existing=character.gear.find(x=>String(x?.id??x?.sourceCatalogId??'')===transfer.catalogId);
            if(existing){
                const prior=Number(existing.quantity??1);
                if(!Number.isSafeInteger(prior)||prior<1||prior+transfer.quantity>999)throw Error('SWE02 p.11: quantidade de equipamento na ficha inválida.');
                existing.quantity=prior+transfer.quantity;
            }else character.gear.push({...clone(gearSource),quantity:transfer.quantity});
            character_mjs_1.validateCharacter(character);
            entry.character=character;
            this.commit('PLAYER_ROSTER_UPDATED',{roster});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {transfer:clone(transfer),character:clone(character),remaining:this.swe02ShipCargoTransferOptions().items};
        });
    }
    recordSWE02BuraConversation(details={}){
        return this.atomicAction(()=>{
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.recordBuraConversation(this.state.world,details);
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.result);
        });
    }
    recordSWE02NewMeenLifecycle(operation,details={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02 New Meen: encerre o combate antes de registrar uma escolha de cena.');
            const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            const out=adapter.recordNewMeenLifecycle(this.state.world,String(operation),details??{});
            this.commit('WORLD_UPDATED',{world:out.world});return {receipt:out.receipt,lifecycle:out.lifecycle};
        });
    }
    /** SWE02 pp.13–14: source-backed Nabat operations; all commits atomic. */
    swe02NabatMarket(){return nabatMarketCatalog();}
    purchaseSWE02NabatItem({itemId,quantity=1}={}){
        return this.atomicAction(()=>{
            const out=nabatPurchase(this.state.world,requireCharacter(this.state.player?.character),{itemId,quantity});
            this.commit('CHARACTER_UPDATED',{character:out.character});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {receipt:clone(out.receipt),item:clone(out.item),creditsRemaining:out.character.credits};
        });
    }
    requestSWE02SpaceportWatch(){
        return this.atomicAction(()=>{
            const out=requestNabatSpaceportWatch(this.state.world);
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
        });
    }
    receiveSWE02SpaceportAlert(){
        return this.atomicAction(()=>{
            const out=deliverNabatNightAlert(this.state.world);
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
        });
    }
    respondSWE02NabatMorning({response}={}){
        return this.atomicAction(()=>{
            const out=recordNabatMorningResponse(this.state.world,requireCharacter(this.state.player?.character),{response});
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
        });
    }
    repairSWE02KraytFangAtNabat(){
        return this.atomicAction(()=>{
            const out=nabatShipRepair(this.state.world,this.state.vehicles);
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
        });
    }
    treatSWE02NabatWounds(){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes dos cuidados no abrigo.');
            const roster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            const entry=roster.entries.find(x=>x.id===roster.activeCharacterId);
            const character=requireCharacter(this.state.player?.character);
            if(!entry)throw Error('SWE02: PC ativo não existe no roster.');
            const out=nabatSafehouseCare(this.state.world,character,{characterKey:entry.id});
            entry.character=out.character;
            this.commit('PLAYER_ROSTER_UPDATED',{roster});
            this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
        });
    }
    treatSWE02NabatPartyCare(){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes dos cuidados no abrigo.');
            const roster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            const expected=Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.eoteRules?.destiny?.playerCount??this.state.session?.playerCount??1);
            if(!Number.isSafeInteger(expected)||expected<1||expected>8||roster.entries.length!==expected)
                throw Error('SWE02 p.13: cadastre uma ficha individual para cada PC antes dos cuidados do grupo.');
            let world=clone(this.state.world);const receipts=[];
            const already=new Set((world.publishedAdventure?.nabatReceipts??[]).filter(x=>x.type==='SAFEHOUSE_CARE').map(x=>String(x.characterKey)));
            for(const entry of roster.entries){
                if(already.has(entry.id))continue;
                if(!entry.character)throw Error('SWE02 p.13: ficha de PC ausente no roster.');
                const out=nabatSafehouseCare(world,entry.character,{characterKey:entry.id});
                world=out.world;entry.character=out.character;receipts.push(out.receipt);
            }
            if(!receipts.length)throw Error('SWE02: todos os PCs já receberam os cuidados do abrigo.');
            this.commit('PLAYER_ROSTER_UPDATED',{roster});this.commit('WORLD_UPDATED',{world});
            return {type:'SAFEHOUSE_CARE_PARTY',patients:receipts.length,receipts:clone(receipts),woundsHealed:receipts.reduce((n,x)=>n+Number(x.woundsHealed||0),0),strainRecovered:receipts.reduce((n,x)=>n+Number(x.strainRecovered||0),0)};
        });
    }
    /** SWE02 pp.13–14: one real physician dice roll per critical injury. */
    treatSWE02NynCritical({characterId=null,injuryId,rng=Math.random}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes do tratamento médico.');
            const roster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            const pcId=String(characterId??roster.activeCharacterId??'');
            const entry=roster.entries.find(x=>x.id===pcId);
            if(!entry?.character)throw Error('SWE02: selecione uma ficha de PC registrada para o tratamento.');
            const out=nynCriticalCare(this.state.world,entry.character,{injuryId,characterKey:entry.id,rng});
            entry.character=out.character;this.commit('PLAYER_ROSTER_UPDATED',{roster});this.commit('WORLD_UPDATED',{world:out.world});
            return clone(out.receipt);
        });
    }
    negotiateSWE02Nyn({method='charm',rng=Math.random}={}){
        return this.atomicAction(()=>{
            const out=nynNegotiate(this.state.world,requireCharacter(this.state.player?.character),{method,rng});
            this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
            return clone(out.receipt);
        });
    }
    swe02NynRewardOptions(){return nynRewardOptions(this.state.world,this.state.player?.roster);}
    distributeSWE02NynReward({characterId,itemId}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes de distribuir equipamento.');
            const roster=clone(this.state.player?.roster??{schema:1,activeCharacterId:null,entries:[]});
            const entry=roster.entries.find(x=>x.id===String(characterId??''));
            if(!entry?.character)throw Error('SWE02: PC destinatário não existe no roster.');
            const out=distributeNynReward(this.state.world,entry.character,{characterId:entry.id,itemId});
            character_mjs_1.validateCharacter(out.character);entry.character=clone(out.character);
            this.commit('PLAYER_ROSTER_UPDATED',{roster});this.commit('WORLD_UPDATED',{world:out.world});
            return clone(out.receipt);
        });
    }
    bribeSWE02NewMeenThug({credits=500,rng=Math.random}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes de negociar.');
            let out=bribeNewMeenThug(this.state.world,requireCharacter(this.state.player?.character),{credits,rng});
            const p=out.world?.publishedAdventure;
            if(p?.newMeenLifecycle){const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
                out={...out,world:adapter.recordNewMeenLifecycle(out.world,'bribe_receipt',{receipt:out.receipt}).world};}
            this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
            return {receipt:clone(out.receipt),remainingThugs:out.remainingThugs};
        });
    }
    /** SWE02 pp.20-21: escorted civilians are NOT a liberated New Meen. */
    prepareSWE02NewMeenEscort(){return this.atomicAction(()=>{if(this.state.combat?.active)throw Error('SWE02: encerre combate antes da escolta.');
        const out=prepareNewMeenEscort(this.state.world);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);});}
    departSWE02NewMeenEscort({travelTicks}={}){return this.atomicAction(()=>{if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=departNewMeenEscort(this.state.world,{travelTicks});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);});}
    arriveSWE02NewMeenEscort(){return this.atomicAction(()=>{if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        let out=arriveNewMeenEscort(this.state.world);const p=out.world?.publishedAdventure;
        if(p?.newMeenLifecycle){const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            out={...out,world:adapter.recordNewMeenLifecycle(out.world,'escort_receipt',{receipt:out.receipt}).world};}
        this.commit('WORLD_UPDATED',{world:out.world});
        return {receipt:clone(out.receipt),timeAdvancedTicks:out.timeAdvancedTicks};});}
    abandonSWE02NewMeenEscort(){return this.atomicAction(()=>{if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        let out=abandonNewMeenEscort(this.state.world);const p=out.world?.publishedAdventure;
        if(p?.newMeenLifecycle){const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
            out={...out,world:adapter.recordNewMeenLifecycle(out.world,'escort_receipt',{receipt:out.receipt}).world};}
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);});}
    armSWE02NewMeenMilitia({weaponIndex}={}){return this.atomicAction(()=>{if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=armNewMeenMilitia(this.state.world,requireCharacter(this.state.player?.character),{weaponIndex});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);});}
    returnSWE02NewMeenMilitiaWeapon({loanId}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: devolução apenas após encerrar o combate.');
        const out=returnNewMeenMilitiaWeapon(this.state.world,requireCharacter(this.state.player?.character),{loanId});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
        return clone(out.receipt);
    });}
    applySWE02NewMeenMilitiaRound({targetGroupId}={}){return this.atomicAction(()=>{
        const out=applyNewMeenMilitiaAbstractRound(this.state.world,this.state.combat,{targetGroupId});
        this.commit('WORLD_UPDATED',{world:out.world});this.commit('COMBAT_ACTION',{combat:out.combat});return clone(out.effect);});}
    /** SWE02 p.23: explicit GM death adjudication bounded by actual minion losses. */
    adjudicateSWE02NewMeenDeaths({groupId,confirmedDeaths}={}){
        return this.atomicAction(()=>{
            const out=adjudicateNewMeenDeaths(this.state.world,this.state.combat,{groupId,confirmedDeaths});
            this.commit('WORLD_UPDATED',{world:out.world});this.commit('COMBAT_ACTION',{combat:out.combat});
            return clone(out.receipt);
        });
    }
    /** SWE02 p.23: Coerce uses real player turn/action, dice, and verified fatality count. */
    coerceSWE02NewMeenThugs({rng=Math.random}={}){
        return this.atomicAction(()=>{
            const out=coerceNewMeenThugs(this.state.world,this.state.combat,requireCharacter(this.state.player?.character),{rng});
            this.commit('WORLD_UPDATED',{world:out.world});this.commit('COMBAT_ACTION',{combat:out.combat});
            return clone(out.receipt);
        });
    }
    openSWE02DrombbSafe({method='key',gmConfirmedQuick,rng=Math.random}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: encerre o combate antes de abrir o cofre.');
            let out=openDrombbSafe(this.state.world,requireCharacter(this.state.player?.character),{method,gmConfirmedQuick,rng});
            const p=out.world?.publishedAdventure;
            if(p?.newMeenLifecycle){const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');
                out={...out,world:adapter.recordNewMeenLifecycle(out.world,'safe_search').world};}
            this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
            return clone(out.receipt);
        });
    }
    /** SWE02 p.33: Piddock questions and genuine Vio contract. */
    askSWE02Piddock({topic='GERAL',expressTradeInterest=false}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=askSWE02Piddock(this.state.world,{topic,expressTradeInterest});
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    showSWE02PiddockTradeInterest(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=showSWE02PiddockTradeInterest(this.state.world);
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    hireSWE02Vio({offer='WEX',partySize=null,rng=Math.random}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const actual=partySize??this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.eoteRules?.destiny?.playerCount??this.state.session?.playerCount;
        const out=hireSWE02Vio(this.state.world,requireCharacter(this.state.player?.character),{offer,partySize:actual,rng});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
        return clone(out.receipt);
    });}
    swe02PiddockArmsCatalog(){return piddockArmsCatalog(this.state.world);}
    agreeSWE02PiddockArms(options={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: não negociar armas durante combate.');
        const out=agreeSWE02PiddockArms(this.state.world,requireCharacter(this.state.player?.character),options);
        out.world.publishedAdventure.flags={...(out.world.publishedAdventure.flags||{}),piddockTradeAgreed:true};
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    deliverSWE02PiddockArms(options={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: não entregar armas durante combate.');
        const out=deliverSWE02PiddockArms(this.state.world,requireCharacter(this.state.player?.character),options);
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    bribeSWE02Vrixxtt({credits=500,targetId=null,rng=Math.random}={}){return this.atomicAction(()=>{
        const out=bribeSWE02Vrixxtt(this.state.world,this.state.combat,requireCharacter(this.state.player?.character),{credits,targetId,rng});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
        this.commit('COMBAT_ACTION',{combat:out.combat});return clone(out.receipt);
    });}
    /** SWE02 pp.43–44: one reusable, source-backed palace transaction family. */
    swe02PalaceOperationCatalog(){return listSWE02PalaceOperations(this.state.world);}
    executeSWE02PalaceOperation(options={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('SWE02: operações do palácio não são gratuitas durante combate.');
            const out=executeSWE02PalaceOperation(this.state.world,requireCharacter(this.state.player?.character),options);
            this.commit('CHARACTER_UPDATED',{character:out.character});
            this.commit('WORLD_UPDATED',{world:out.world});
            return clone(out.receipt);
        });
    }
    swe02AnattaCatalog(){return clone(ANATTA_CATALOG);}
    buySWE02AnattaInformation({topicId}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=buySWE02AnattaInformation(this.state.world,requireCharacter(this.state.player?.character),{topicId});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});
        return clone(out.receipt);
    });}
    /** SWE02 p.43: all four gladiators, bribery or opposed Coercion. */
    recruitSWE02Gladiators({method='BRIBE',rng=Math.random}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: negocie antes de iniciar combate.');
        const out=recruitSWE02Gladiators(this.state.world,requireCharacter(this.state.player?.character),{method,rng});
        this.commit('CHARACTER_UPDATED',{character:out.character});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    /** SWE02 p.44: Teemo presses alarm as his NPC maneuver, arrivals after four rounds. */
    queueSWE02ThroneAlarm({confirmedBarracksCount}={}){return this.atomicAction(()=>{
        if(this.state.world?.publishedAdventure?.flags?.electrogateBarracksDiverted)throw Error('SWE02 p.40: quatro guardas do quartel já estão no eletroportão; não podem reforçar o trono.');
        const combat=queueSWE02ThroneAlarm(this.state.combat,{confirmedBarracksCount});
        this.commit('COMBAT_ACTION',{combat});return clone(combat.swe02ThroneAlarmQueued);
    });}
    /** SWE02 Act 3 pp.34,44-46: concrete proof and nonviolent endings. */
    prepareSWE02AnattaDossier(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=prepareSWE02AnattaDossier(this.state.world);
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    resolveSWE02TeemoWithoutCombat({method='BLACKMAIL'}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=resolveSWE02TeemoWithoutCombat(this.state.world,{method});
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    claimSWE02TeemoStrongbox(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02: combate ativo.');
        const out=claimSWE02TeemoStrongbox(this.state.world,requireCharacter(this.state.player?.character));
        this.commit('CHARACTER_UPDATED',{character:out.character});
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    shootSWE02TeemoChandelier({weaponIndex,rng=Math.random}={}){return this.atomicAction(()=>{
        const out=shootSWE02Chandelier(this.state.world,this.state.combat,requireCharacter(this.state.player?.character),{weaponIndex,rng});
        this.commit('WORLD_UPDATED',{world:out.world});this.commit('COMBAT_ACTION',{combat:out.combat});
        return clone(out.receipt);
    });}
    /** SWE02 p.18: an Advantage during the hunters' fight can trigger this source event. */
    _applySWE02OldLylekAdvantage(combat,check,{preferredHunterId=null}={}){
        if(!combat?.active||combat.publishedAdventureContext?.adventureId!=='long-arm-of-the-hutt'||combat.publishedAdventureContext?.encounterId!=='lylek-den-bounty-hunters'||Number(check?.netAdvantage)<=0)return {combat,event:null};
        const old=this.state.world?.publishedAdventure?.oldLylek;
        if(old?.tactics?.lylekBranch!=='disturb_on_advantage'||old.lylekTriggered)return {combat,event:null};
        const hunters=combat.adversaries.filter(a=>['laoth_gand_bounty_hunter','laoth_bounty_hunter','trex'].includes(a.profileKey)&&!a.swe02KilledByLylek);
        const victim=hunters.find(a=>a.id===preferredHunterId&&!adversaries_mjs_1.adversaryHealthStatus(a).incapacitated)||hunters.find(a=>!adversaries_mjs_1.adversaryHealthStatus(a).incapacitated)||hunters[0];
        if(!victim)return {combat,event:null};
        const triggerId=ids_mjs_1.newId('lylek_event'),out=triggerLongArmOldLylekAdvantage(this.state.world,{hunterId:victim.id,triggerId});
        const killed=clone(victim);killed.wounds={...killed.wounds,current:Number(killed.wounds.threshold)+1};killed.status='incapacitated';killed.swe02KilledByLylek=true;
        killed.swe02Fate={kind:'KILLED_BY_LYLEK',preyReturnedToNest:true,source:{productCode:'SWE02',printedPage:18}};
        combat=encounter_mjs_1.updateAdversaryInCombat(combat,killed);
        const event={...clone(out.receipt),display:'The southern lylek kills one bounty hunter and retreats to its nest with the prey.'};
        combat.swe02OldLylekAdvantageEvent=event;
        combat.log=[...(combat.log??[]),{type:'SWE02_LYLEK_ADVANTAGE_EVENT',round:combat.round,event:clone(event)}];
        this.commit('WORLD_UPDATED',{world:out.world});
        return {combat,event};
    }
    setSWE02OldLylekPlayerCover({behindRockPile=false}={}){return this.atomicAction(()=>{
        const combat=clone(this.state.combat);
        if(!combat?.active||combat.publishedAdventureContext?.adventureId!=='long-arm-of-the-hutt'||combat.publishedAdventureContext?.encounterId!=='lylek-den-bounty-hunters'||typeof behindRockPile!=='boolean')
            throw Error('SWE02 p.17: marque posição atrás das rochas somente durante o combate dos caçadores.');
        combat.swe02OldLylekCover={...(combat.swe02OldLylekCover??{}),playerBehindRockPile:behindRockPile,
            source:{productCode:'SWE02',printedPage:17,coverSetback:2}};
        combat.log=[...(combat.log??[]),{type:'SWE02_LYLEK_COVER_POSITION',behindRockPile,round:combat.round,sourcePage:17}];
        this.commit('COMBAT_ACTION',{combat});return clone(combat.swe02OldLylekCover);
    });}
    setSWE02OldLylekMapLocation({actorKey,locationId}={}){return this.atomicAction(()=>{
        let combat=clone(this.state.combat);
        combat=assignLongArmOldLylekMapLocation(combat,{actorKey,locationId});
        combat.log=[...(combat.log??[]),{type:'SWE02_LYLEK_MAP_LOCATION',actorKey,locationId,round:combat.round,sourcePage:16}];
        this.commit('COMBAT_ACTION',{combat});return clone(combat.swe02OldLylekMap);
    });}
    resolveSWE02OldLylekHunterRetreat(){return this.atomicAction(()=>{
        const combat=clone(this.state.combat);
        if(!combat?.active||combat.publishedAdventureContext?.adventureId!=='long-arm-of-the-hutt'||combat.publishedAdventureContext?.encounterId!=='lylek-den-bounty-hunters')
            throw Error('SWE02 p.17: a retirada só pode ser resolvida durante o combate dos caçadores.');
        const active=combat.adversaries.filter(a=>!adversaries_mjs_1.adversaryHealthStatus(a).incapacitated&&a.status!=='retreated');
        const out=resolveLongArmOldLylekRetreat(this.state.world,{enemyIds:active.map(a=>a.id)});
        for(const enemy of active){enemy.status='retreated';enemy.swe02RetreatRoute='dotted map path';}
        combat.encounter={...(combat.encounter??{}),outcome:'escaped'};
        combat.swe02OldLylekTactics={...(combat.swe02OldLylekTactics??{}),retreat:{...out.receipt}};
        combat.log=[...(combat.log??[]),{type:'SWE02_LYLEK_FIGHTING_RETREAT',round:combat.round,enemyIds:active.map(a=>a.id),sourcePage:17}];
        this.commit('WORLD_UPDATED',{world:out.world});this.commit('COMBAT_ACTION',{combat});return clone(out.receipt);
    });}
    revealSWE02OldLylekDeveloperDetails({asked=false}={}){return this.atomicAction(()=>{
        const out=revealLongArmOldLylekDeveloperDetails(this.state.world,{asked});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    /** SWE02 p.15: crew IDs are explicit and exclude companions/NPCs. */
    chooseSWE02OldLylekCrew({crewPcIds}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.15: registre os PCs a bordo antes do combate.');
        const roster=this.state.player?.roster,entries=roster?.entries??[];
        const count=Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.eoteRules?.destiny?.playerCount??this.state.session?.playerCount);
        if(!Number.isInteger(count)||count<1||count>8||entries.length!==count)throw Error('SWE02 p.15: cadastre uma ficha para cada PC; playerCount não substitui o roster.');
        const out=chooseLongArmOldLylekCrew(this.state.world,{crewPcIds,rosterEntries:entries,playerCount:count});
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.progress.oldLylek);
    });}
    chooseSWE02OldLylekRoute({slowDown}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.15: escolha a rota antes do combate.');
        const out=chooseLongArmOldLylekRoute(this.state.world,{slowDown});
        this.commit('WORLD_UPDATED',{world:out.world});return clone(out.progress.oldLylek);
    });}
    selectSWE02TrexCarryover({history}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.15: o histórico de T’rex deve ser selecionado antes do combate.');
        const out=selectLongArmTrexCarryover(this.state.world,{history});this.commit('WORLD_UPDATED',{world:out.world});return clone(out.carryover);
    });}
    proceedSWE02OldLylekBluff(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.17: não é possível avançar durante combate.');
        const out=proceedLongArmOldLylekBluff(this.state.world);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.progress.oldLylek);
    });}
    recordSWE02OldLylekRuse(options={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.17: registre o ruse antes do combate.');
        const out=recordLongArmOldLylekRuse(this.state.world,options);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    chooseSWE02OldLylekTactics(options={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 pp.16–18: escolha tática antes do combate.');
        const out=chooseLongArmOldLylekTactics(this.state.world,options);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.receipt);
    });}
    approachSWE02OldLylekOnFoot(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.17: não é possível mudar a aproximação durante combate.');
        const out=approachLongArmOldLylekOnFoot(this.state.world);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.progress.oldLylek);
    });}
    resolveSWE02OldLylekCrashCheck({skillKey='aguante',rng=Math.random}={}){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.17: o speeder já parou; teste indisponível durante combate.');
        if(!['aguante','coordinacion'].includes(skillKey))throw Error('SWE02 p.17: escolha Aguante ou Coordenação.');
        const old=this.state.world?.publishedAdventure?.oldLylek,crew=old?.crewPcIds??[],roster=clone(this.state.player?.roster);
        const sessionCount=Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.eoteRules?.destiny?.playerCount??this.state.session?.playerCount);
        if(!Number.isInteger(sessionCount)||sessionCount<1||sessionCount>8||crew.length!==sessionCount)throw Error('SWE02 p.17: roster e IDs de PCs a bordo não confirmados.');
        const prior=old?.crashChecks??[];
        const checked=new Set(prior.map(x=>x.characterId));
        const characterId=crew.find(id=>!checked.has(id));
        if(!characterId)throw Error('SWE02 p.17: todos os testes da tripulação já foram registrados.');
        const rosterEntry=roster.entries.find(x=>x.id===characterId);
        if(!rosterEntry)throw Error('SWE02 p.17: PC a bordo ausente do roster.');
        const character=clone(rosterEntry.character);
        character.skills={...(character.skills??{}),[skillKey]:Number(character.skills?.[skillKey]??0)};
        roster.activeCharacterId=characterId;
        roster.entries.find(x=>x.id===characterId).character=character;
        this.commit('PLAYER_ROSTER_UPDATED',{roster});
        const check=this.resolveSkillCheck({skillKey,difficulty:2,rng});
        let updated=character,damage=null;
        if(!check.success){damage=health_mjs_1.applyDamage(character,{damage:6,damageType:'wounds',reason:'Lançado do speeder — SWE02 p.17',criticalRng:rng});updated=damage.character;}
        const out=recordLongArmOldLylekCrashCheck(this.state.world,{characterId,characterName:character.name,skillKey,check});
        let world=out.world;
        const checks=world.publishedAdventure.oldLylek.crashChecks;
        if(checks.length===sessionCount)world=confirmLongArmOldLylekCrashCrew(world,{expectedPcCount:sessionCount}).world;
        const updatedRoster=clone(this.state.player.roster);
        updatedRoster.entries.find(x=>x.id===characterId).character=updated;
        if(roster.activeCharacterId===characterId)this.commit('CHARACTER_UPDATED',{character:updated});
        this.commit('PLAYER_ROSTER_UPDATED',{roster:updatedRoster});this.commit('WORLD_UPDATED',{world});
        return {check,damage:damage?{rawDamage:damage.rawDamage,suffered:damage.suffered,effectiveSoak:damage.effectiveSoak}:null,
            characterId,checked:checks.length,expected:sessionCount,phase:world.publishedAdventure.oldLylek.phase};
    });}
    inspectSWE02OldLylekSpeeder(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.18: encerre o combate antes de inspecionar o speeder.');
        const out=inspectLongArmOldLylekSpeeder(this.state.world),group=clone(this.state.group),loot=out.progress.oldLylek.hunterLoot;
        const inventory=[
          {id:'swe02-old-lylek-stimpack-1',name:'Stimpack',quantity:1},
          {id:'swe02-old-lylek-stimpack-2',name:'Stimpack',quantity:1},
          {id:'swe02-old-lylek-medpac',name:'Medpac',quantity:1},
          {id:'swe02-old-lylek-emergency-repair-kit',name:'Emergency Repair Kit',quantity:1}
        ];
        group.belongings=Array.isArray(group.belongings)?group.belongings:[];
        if(inventory.some(x=>group.belongings.some(y=>y.id===x.id)))throw Error('SWE02 p.18: item do speeder já consta no inventário do grupo.');
        for(const item of inventory)group.belongings.push({...item,source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:18,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}});
        const granted=recordLongArmOldLylekLootGrant(out.world,{inventoryIds:inventory.map(x=>x.id)});
        this.commit('GROUP_UPDATED',{group});this.commit('WORLD_UPDATED',{world:granted.world});return clone(granted.progress.oldLylek.hunterLoot);
    });}
    insistSWE02OldLylek(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.18: encerre o combate dos caçadores primeiro.');
        const out=recordLongArmOldLylekWarning(this.state.world);this.commit('WORLD_UPDATED',{world:out.world});const adapter=publishedAdventureRegistry_mjs_1.getPublishedAdventureAdapter('long-arm-of-the-hutt');const scene=adapter.currentScene(this.state.world);if(scene)this.commit('SCENE_UPDATED',{scene});return clone(out.progress.oldLylek.lylekWarning);
    });}
    persistSWE02OldLylek(){return this.atomicAction(()=>{
        if(this.state.combat?.active)throw Error('SWE02 p.18: encerre o combate ativo primeiro.');
        const out=persistLongArmOldLylek(this.state.world);this.commit('WORLD_UPDATED',{world:out.world});return clone(out.progress.oldLylek.lylekEncounter);
    });}
    /** SWE02 p.18: adventure action, not a decorative 'triggered' flag. */
    beginSWE02ThwheekCombat({rng=Math.random,unawareCharacterIds=[]}={}){
        return this.atomicAction(()=>{
            if(this.state.combat?.active)throw Error('Encerre o combate pessoal antes da interceptação.');
            const roster=this.state.player?.roster;
            const count=Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.playerCount??1);
            if(!Number.isSafeInteger(count)||count<1||count>8||roster?.entries?.length!==count)
                throw Error('SWE02 p.12: cadastre no roster uma ficha individual para cada PC confirmado na sessão antes da iniciativa.');
            const initiative=rollSWE02VehicleInitiative(roster,{rng,unawareCharacterIds});
            const out=beginThwheekInterception(this.state.world,this.state.vehicles,{initiative});
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});
            this.commit('WORLD_UPDATED',{world:out.world});
            const first=out.vehicles.combat.swe02Initiative.slots[0];
            if(first.side==='player')this.selectPartyCharacter(first.actorId);
            return {combat:clone(this.state.vehicles.combat),adventure:clone(this.state.world.publishedAdventure)};
        });
    }
    /** End the current Cool slot; only the last slot advances the vehicle round. */
    passSWE02VehicleTurn({rng=Math.random}={}){
        return this.atomicAction(()=>{
            const vehicles=clone(this.state.vehicles),combat=vehicles.combat,initiative=combat?.swe02Initiative;
            if(!combat?.active||combat.publishedVehicleContext?.id!==THWHEEK_CONTEXT||!initiative)
                throw Error('SWE02 p.12: não há turno de iniciativa ativo.');
            const previous=initiative.slots[initiative.index];
            const enemy=this.state.vehicles.vehicles?.[THWHEEK_ID];
            if(previous.side==='npc'&&enemy&&enemy.hullDamage<5&&combat.npcAttackResolvedRound!==combat.round)
                throw Error('SWE02 p.12: Thwheek precisa resolver seu ataque antes de passar o turno.');
            const nextIndex=initiative.index+1;
            if(nextIndex<initiative.slots.length){
                initiative.index=nextIndex;
                const next=initiative.slots[nextIndex];
                if(next.side==='player'){
                    this.selectPartyCharacter(next.actorId);
                    combat.actionUsed=false;combat.maneuversUsed=0;
                }
            }else{
                // nextVehicleRound owns all round-end effects and is nested in
                // this transaction, so round effects and initiative persist atomically.
                this.nextVehicleRound({rng,initiativeAdvanceToken:SWE02_INITIATIVE_ADVANCE});
                const updated=clone(this.state.vehicles),order=updated.combat.swe02Initiative;
                const next=order.slots[0];
                if(next.side==='player')this.selectPartyCharacter(next.actorId);
                this.commit('VEHICLES_UPDATED',{vehicles:updated});
            }
            if(nextIndex<initiative.slots.length)this.commit('VEHICLES_UPDATED',{vehicles});
            return {previous:clone(previous),current:clone(this.state.vehicles.combat.swe02Initiative.slots[this.state.vehicles.combat.swe02Initiative.index]),
                combat:clone(this.state.vehicles.combat)};
        });
    }
    _assertSWE02VehicleTurn(side){
        const combat=this.state.vehicles?.combat;
        if(combat?.publishedVehicleContext?.id!==THWHEEK_CONTEXT||!combat.swe02Initiative)return;
        const initiative=combat.swe02Initiative,slot=initiative?.slots?.[initiative.index];
        if(!slot||slot.side!==side)throw Error('SWE02 p.12: a ação não pertence ao lado cujo turno está ativo na iniciativa.');
        if(side==='player'&&slot.actorId!==this.state.player?.roster?.activeCharacterId)
            throw Error('SWE02 p.12: selecione o PC do turno ativo antes de agir.');
    }
    _recordSWE02VehicleAction(vehicles,kind){
        const combat=vehicles?.combat,initiative=combat?.swe02Initiative;
        if(combat?.publishedVehicleContext?.id!==THWHEEK_CONTEXT||!initiative)return;
        const slot=initiative.slots[initiative.index];
        initiative.actionReceipts.push({round:combat.round,slotIndex:initiative.index,actorId:slot.actorId,kind});
    }
    /** Combat resolution requires actual logged dice damage; no GM-only victory toggle. */
    resolveSWE02ThwheekCombat({pursue=false}={}){
        return this.atomicAction(()=>{
            const out=resolveThwheekInterception(this.state.world,this.state.vehicles,{pursue});
            if(out.outcome!=='PURSUIT_CONTINUES'){
                out.world.publishedAdventure.flags.shipThwheekLandedSafely=out.outcome!=='KRAYT_FANG_SHOT_DOWN';
            }
            this.commit('VEHICLES_UPDATED',{vehicles:out.vehicles});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {outcome:out.outcome,receipt:clone(this.state.world.publishedAdventure.shipThwheekReceipt??null),
                combat:clone(this.state.vehicles.combat)};
        });
    }
    /** Linked 1 is activated with two real Advantage, never on a missed shot. */
    activateSWE02ThwheekLinked(){
        return this.atomicAction(()=>{
            const v=clone(this.state.vehicles),c=v.combat;
            if(!c?.active||c.publishedVehicleContext?.id!==THWHEEK_CONTEXT||c.targetVehicleId!==THWHEEK_ID)
                throw Error('SWE02: nenhum ataque de Thwheek ativo.');
            const a=c.lastIncomingAttack;
            if(!a||a.round!==c.round||a.attackerId!==THWHEEK_ID||!a.hit||Number(a.check?.netAdvantage)<2||
               c.publishedVehicleContext.linkedActivatedRound===c.round||
               (c.log||[]).some(x=>x.type==='CRITICO_VEICULAR_INIMIGO'&&x.round===c.round))
                throw Error('SWE02: Vinculada exige acerto e duas Vantagens não gastas neste assalto.');
            const target=v.vehicles?.[v.activeVehicleId];
            if(!target||a.targetId!==target.id)throw Error('SWE02: alvo do ataque Vinculado mudou.');
            const damage=6+Math.max(0,Number(a.check.netSuccess));
            const hit=vehicles_mjs_1.applyVehicleScaleDamage(target,{damage,breach:0});
            v.vehicles[target.id]=hit.vehicle;
            c.publishedVehicleContext.linkedActivatedRound=c.round;
            c.log=[...(c.log||[]),{type:'SWE02_THWHEEK_LINKED',round:c.round,attackerId:THWHEEK_ID,
                targetId:target.id,damage:hit.suffered,advantageSpent:2}];
            this.commit('VEHICLES_UPDATED',{vehicles:v});
            return {damage:hit.suffered,vehicle:clone(hit.vehicle)};
        });
    }
    /** Crash requires official Hard pilot check, then a Hard Resilience roll per actual PC. */
    resolveSWE02CrashPilot({rng=Math.random}={}){
        return this.atomicAction(()=>{
            const p=this.state.world?.publishedAdventure;
            if(p?.id!=='long-arm-of-the-hutt'||p.currentNodeId!=='thwheek-crash'||
               p.flags?.shipCrashCrewPending!==true||p.shipCrashPilotCheck)
                throw Error('SWE02: pouso de emergência indisponível ou já resolvido.');
            const count=Number(this.state.session?.campaignRules?.destiny?.playerCount??this.state.session?.playerCount);
            const crewEntries=this.state.player?.roster?.entries??[];
            const crewIds=crewEntries.map(x=>x?.id),initiativeIds=(this.state.vehicles?.combat?.swe02Initiative?.slots??[])
                .filter(x=>x.side==='player').map(x=>x.actorId);
            const pilotCharacterId=this.state.player?.roster?.activeCharacterId;
            if(!Number.isInteger(count)||count<1||count>8||crewEntries.length!==count||
                crewEntries.some(x=>typeof x?.id!=='string'||!x.id)||new Set(crewIds).size!==count||
                initiativeIds.length!==count||initiativeIds.some(id=>!crewIds.includes(id))||!crewIds.includes(pilotCharacterId))
                throw Error('SWE02 p.12: roster de tripulação não corresponde ao número de PCs confirmado.');
            const check=this.resolveSkillCheck({skillKey:'pilotar_vehiculo_espacial',difficulty:3,rng});
            const world=clone(this.state.world),q=world.publishedAdventure;
            q.shipCrashPilotCheck={success:check.success,netSuccess:check.netSuccess,
                source:{productCode:'SWE02',printedPage:12},skill:'pilotar_vehiculo_espacial',difficulty:3,pilotCharacterId};
            q.shipCrashCrewChecks=[];
            q.shipCrashCrewIdentitySchema=1;
            q.shipCrashExpectedCrewIds=crewEntries.map(x=>x.id);
            q.shipCrashExpectedCrewCount=count;
            this.commit('WORLD_UPDATED',{world});
            return {check,softLanding:check.success,expectedCrew:q.shipCrashExpectedCrewCount};
        });
    }
    resolveSWE02CrashResilience({rng=Math.random}={}){
        return this.atomicAction(()=>{
            const p=this.state.world?.publishedAdventure,character=requireCharacter(this.state.player?.character);
            if(p?.id!=='long-arm-of-the-hutt'||p.currentNodeId!=='thwheek-crash'||!p.shipCrashPilotCheck||
               p.flags?.shipCrashCrewPending!==true||!p.shipCrashExpectedCrewCount)
                throw Error('SWE02: falta Pilotagem ou número de tripulantes confirmado.');
            const crewId=this.state.player?.roster?.activeCharacterId;
            const expectedIds=p.shipCrashExpectedCrewIds??[];
            if(p.shipCrashCrewIdentitySchema===1&&!expectedIds.includes(crewId))throw Error('SWE02: PC não pertence à tripulação registrada para esta queda.');
            if((p.shipCrashCrewChecks||[]).some(x=>p.shipCrashCrewIdentitySchema===1?x.characterId===crewId:x.characterName===character.name))throw Error('SWE02: este tripulante já realizou Aguante.');
            const positive=narrativeDice_mjs_1.buildPositivePool(character.characteristics.fortaleza,character.skills.aguante??0);
            if(p.shipCrashPilotCheck.success){if(positive.ability>0){positive.ability--;positive.proficiency++;}else positive.ability++;}
            const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:3,challenge:0,boost:0,setback:0},rng);
            this.commit('CHECK_RESOLVED',check);
            let next=clone(character);
            const wounds=Math.max(0,-Number(check.netAdvantage||0));
            next.derived.wounds.current=Math.min(Math.max(next.derived.wounds.threshold*2,0),next.derived.wounds.current+wounds);
            let critical=null;
            if(!check.success){const applied=critical_mjs_1.addStarterCritical(next,'Queda da Krayt Fang — SWE02 p.12',{rng});
                next=applied.character;critical=applied.injury??null;}
            this.commit('CHARACTER_UPDATED',{character:next});
            const world=clone(this.state.world),q=world.publishedAdventure;
            q.shipCrashCrewChecks=[...(q.shipCrashCrewChecks||[]),{characterId:crewId??null,characterName:character.name,success:check.success,
                netAdvantage:check.netAdvantage,wounds,criticalInjuryId:critical?.id??null,sourcePage:12}];
            if(q.shipCrashCrewChecks.length===q.shipCrashExpectedCrewCount){
                q.flags.shipCrashCrewPending=false;q.flags.shipCrashCrewResolved=true;
            }
            this.commit('WORLD_UPDATED',{world});
            return {check,wounds,critical,completed:q.flags.shipCrashCrewResolved===true,
                checked:q.shipCrashCrewChecks.length,expected:q.shipCrashExpectedCrewCount};
        });
    }
    beginVehicleCombat({ targetProfileKey = 'tie_ln', range = 'Curto' } = {}) {
        if(this.state.world?.publishedAdventure?.currentNodeId==='thwheek-orbit'||
           this.state.vehicles?.combat?.publishedVehicleContext?.id===THWHEEK_CONTEXT&&this.state.vehicles.combat.active)
            throw Error('SWE02: use o encontro oficial de Thwheek, não um caça genérico.');
        const active = this.activeVehicle();
        if (!active)
            throw new Error('Selecione uma nave/veículo antes de iniciar o encontro.');
        const vehicles = clone(this.state.vehicles);
        const target = vehicles_mjs_1.createVehicleFromProfile(targetProfileKey, { id: `target_${targetProfileKey}` });
        vehicles.vehicles[target.id] = target;
        vehicles.combat = { ...vehicles.combat, aorFireControl:null, brilliantEvasionAttempted:false, brilliantEvasion:null, active: true, round: 1, targetVehicleId: target.id, range, actionUsed: false, maneuversUsed: 0, lastAttack: null, pendingTargetCritical:null, pendingIncomingCritical:null, npcAttackResolvedRound:null, lastIncomingAttack:null, hullDamageControlAttempts:{}, log: [...(vehicles.combat.log ?? []), { type: 'INICIO_COMBATE_VEICULO', round: 1, targetId: target.id }] };
        this.commit('VEHICLES_UPDATED', { vehicles });
        return vehicles.combat;
    }
    endVehicleCombat() {
        if(this.state.vehicles?.combat?.active&&this.state.vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT)
            throw Error('SWE02: resolva a interceptação pelo resultado real do combate.');
        const vehicles = clone(this.state.vehicles);
        vehicles.combat = { ...vehicles.combat, aorFireControl:null, brilliantEvasionAttempted:false, brilliantEvasion:null, active: false, round: 0, targetVehicleId: null, actionUsed: false, maneuversUsed: 0, lastAttack: null, pendingTargetCritical:null, pendingIncomingCritical:null, npcAttackResolvedRound:null, lastIncomingAttack:null, hullDamageControlAttempts:{} };
        for (const v of Object.values(vehicles.vehicles ?? {}))
            if (v?.roundEffects) {
                const base = vehicles_mjs_1.STARTER_VEHICLE_PROFILES[v.key]?.speedMax;
                if (Number.isFinite(base)) {
                    v.speedMax = base;
                    v.speed = Math.min(v.speed, v.speedMax);
                }
                v.roundEffects = { evasive: false, stayOnTarget: false, enfiladeTargetId: null, controlledFire: false };
            }
        this.commit('VEHICLES_UPDATED', { vehicles });
        return vehicles.combat;
    }
    nextVehicleRound({ rng = Math.random, initiativeAdvanceToken = null } = {}) {
        const vehicles = clone(this.state.vehicles);
        if (!vehicles.combat.active)
            throw new Error('Não há combate de naves/veículos ativo.');
        if(vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT&&vehicles.combat.swe02Initiative&&initiativeAdvanceToken!==SWE02_INITIATIVE_ADVANCE)
            throw Error('SWE02 p.12: avance a iniciativa slot por slot para iniciar o próximo assalto.');
        const endingRound=Math.max(1,Number(vehicles.combat.round||1));
        const due=(vehicles.combat.guidedProjectiles??[]).filter(p=>Number(p.resolveAtRound??endingRound)<=endingRound);
        const future=(vehicles.combat.guidedProjectiles??[]).filter(p=>Number(p.resolveAtRound??endingRound)>endingRound);
        const reactivations=[...(vehicles.combat.guidedReactivations??[])];
        for(const projectile of due){
            const target=vehicles.vehicles?.[projectile.targetVehicleId];
            if(!target) continue;
            const guided=vehicles_mjs_1.resolveGuidedAttack({target,weapon:projectile.weapon,guidedRating:projectile.guidedRating,facing:projectile.facing??'proa',rng,currentRound:endingRound});
            if(guided.targetDamage) vehicles.vehicles[target.id]=guided.targetDamage.vehicle;
            vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'GUIADA_RESOLVIDA',round:endingRound,targetId:target.id,projectileId:projectile.id,hit:guided.hit,damage:guided.targetDamage?.suffered??0,damageType:guided.ion?'systemStrain':'hull'}];
            if(guided.reactivationAvailable) reactivations.push({id:projectile.id,remainingAdvantage:Number(guided.check?.netAdvantage??0),projectile:clone(projectile),check:clone(guided.check)});
        }
        vehicles.combat.guidedProjectiles=future;
        vehicles.combat.guidedReactivations=reactivations;
        vehicles.combat.round = endingRound + 1;
        if(vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT&&vehicles.combat.swe02Initiative){
            vehicles.combat.swe02Initiative.round=vehicles.combat.round;
            vehicles.combat.swe02Initiative.index=0;
            vehicles.combat.swe02Initiative.completedRounds+=1;
        }
        vehicles.combat.aorFireControl = null;
        for(const ship of Object.values(vehicles.vehicles??{})){if(ship?.roundEffects)ship.roundEffects.controlledFire=false;}
        vehicles.combat.actionUsed = false;
        vehicles.combat.maneuversUsed = 0;
        vehicles.combat.pendingTargetCritical = null;
        vehicles.combat.pendingVehicleSpend = null;
        let active = vehicles.vehicles[vehicles.activeVehicleId];
        if (active) {
            const expired = vehicles_mjs_1.expireFullThrottle(active, vehicles.combat.round);
            active = vehicles_mjs_1.expireVehicleCriticalRound(expired.vehicle, vehicles.combat.round);
            const carry={...(active.roundEffects??{})};
            delete carry.evasive; delete carry.stayOnTarget; delete carry.enfiladeTargetId; delete carry.controlledFire;
            active.roundEffects = { evasive: false, stayOnTarget: false, enfiladeTargetId: null, controlledFire: false, ...carry };
            vehicles.vehicles[active.id] = active;
        }
        let target = vehicles.vehicles[vehicles.combat.targetVehicleId];
        if (target) {
            const expired = vehicles_mjs_1.expireFullThrottle(target, vehicles.combat.round);
            target = vehicles_mjs_1.expireVehicleCriticalRound(expired.vehicle, vehicles.combat.round);
            const carry={...(target.roundEffects??{})};
            delete carry.evasive; delete carry.stayOnTarget; delete carry.enfiladeTargetId; delete carry.controlledFire;
            target.roundEffects = { evasive: false, stayOnTarget: false, enfiladeTargetId: null, controlledFire: false, ...carry };
            vehicles.vehicles[target.id] = target;
        }
        if(vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT)
            Object.assign(vehicles,thwheekTactics(vehicles));
        this.commit('VEHICLES_UPDATED', { vehicles });
        return vehicles.combat;
    }
    useVehiclePilotManeuver(kind, { method = 'normal', direction = 'closer' } = {}) {
        this._assertSWE02VehicleTurn('player');
        const vehicles = clone(this.state.vehicles);
        const active = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!active)
            throw new Error('Nenhuma nave/veículo ativo.');
        if(kind==='stop_short' && campaignMode(this.state)==='AOR' &&
           talents_mjs_1.sourceBackedTalentRank(this.state.player?.character,'FULLSTOP')<1)
            throw new Error('Parada em seco exige talento AoR adquirido na árvore.');
        const criticalStatus = vehicles_mjs_1.vehicleStatus(active, { currentRound: vehicles.combat.round });
        if (criticalStatus.destroyed)
            throw new Error('A nave/veículo foi destruída.');
        if (criticalStatus.noManeuvers)
            throw new Error('Um Critical Hit impede manobras da nave/veículo até ser resolvido ou reparado.');
        if (criticalStatus.tractorLocked && ['accelerate','brake','advance','punch_it','stop_short'].includes(kind))
            throw new Error(`Raio trator ativo: faça um teste de Pilotagem dificuldade ${criticalStatus.tractorDifficulty} antes de mover a nave/veículo.`);
        if (vehicles.combat.active) {
            const used = Number(vehicles.combat.maneuversUsed ?? 0);
            if (used >= 2)
                throw new Error('A nave/veículo já recebeu 2 manobras exclusivas de piloto neste assalto.');
            if (used === 1) {
                if (method === 'strain') {
                    if (!vehicles_mjs_1.canVoluntarilyInflictVehicleStrain(active))
                        throw new Error('Power Fluctuations impede sofrer Tensão de sistemas voluntariamente.');
                    active.systemStrain += 2;
                }
                else if (method === 'action') {
                    if (vehicles.combat.actionUsed)
                        throw new Error('A ação deste assalto já foi usada.');
                    vehicles.combat.actionUsed = true;
                }
                else
                    throw new Error('A segunda manobra exige 2 de Tensão de sistemas ou trocar a ação.');
            }
            vehicles.combat.maneuversUsed = used + 1;
        }
        let result = null;
        if (kind === 'accelerate')
            result = vehicles_mjs_1.accelerateOrBrake(active, 1);
        else if (kind === 'brake')
            result = vehicles_mjs_1.accelerateOrBrake(active, -1);
        else if (kind === 'advance') {
            vehicles.combat.range = vehicles_mjs_1.movePlanetaryRange(vehicles.combat.range, { direction, speed: active.speed });
            result = active;
        }
        else if (kind === 'evasive')
            result = vehicles_mjs_1.setEvasiveManeuvers(active, true);
        else if (kind === 'stay_on_target')
            result = vehicles_mjs_1.setStayOnTarget(active, true);
        else if (kind === 'punch_it') {
            const r = vehicles_mjs_1.punchIt(active);
            result = r.vehicle;
        }
        else if (kind === 'stop_short') {
            const r = vehicles_mjs_1.stopShort(active);
            result = r.vehicle;
        }
        else
            throw new Error('Manobra de nave/veículo desconhecida.');
        vehicles.vehicles[active.id] = result;
        vehicles.combat.log = [...(vehicles.combat.log ?? []), { type: 'MANOBRA_VEICULO', round: vehicles.combat.round, kind, vehicleId: active.id }];
        this._recordSWE02VehicleAction(vehicles,'maneuver');
        this.commit('VEHICLES_UPDATED', { vehicles });
        return { vehicle: result, combat: vehicles.combat, status: vehicles_mjs_1.vehicleStatus(result) };
    }
    useFullThrottle({ asManeuver = false, secondManeuverMethod = 'normal', skillKey = 'pilotar_vehiculo_espacial', rng = Math.random } = {}) {
        this._assertSWE02VehicleTurn('player');
        let character = requireCharacter(this.state.player.character);
        const vehicles = clone(this.state.vehicles);
        const active = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!active)
            throw new Error('Nenhuma nave/veículo ativo.');
        const criticalStatus = vehicles_mjs_1.vehicleStatus(active, { currentRound: vehicles.combat.round });
        if (criticalStatus.destroyed || criticalStatus.noManeuvers)
            throw new Error('Um Critical Hit impede esta manobra/ação de pilotagem.');
        if (!vehicles.combat.active)
            throw new Error('Aceleração máxima usa assaltos e exige um encontro de nave/veículo ativo.');
        if (asManeuver) {
            const used = Number(vehicles.combat.maneuversUsed ?? 0);
            if (used >= 2)
                throw new Error('A nave/veículo já recebeu 2 manobras exclusivas de piloto neste assalto.');
            if (used === 1) {
                if (secondManeuverMethod === 'strain') {
                    if (!vehicles_mjs_1.canVoluntarilyInflictVehicleStrain(active))
                        throw new Error('Power Fluctuations impede sofrer Tensão de sistemas voluntariamente.');
                    active.systemStrain += 2;
                }
                else if (secondManeuverMethod === 'action') {
                    if (vehicles.combat.actionUsed)
                        throw new Error('A ação deste assalto já foi usada.');
                    vehicles.combat.actionUsed = true;
                }
                else
                    throw new Error('A segunda manobra exige 2 de Tensão de sistemas ou trocar a ação.');
            }
            vehicles.combat.maneuversUsed = used + 1;
        }
        else {
            if (vehicles.combat.actionUsed)
                throw new Error('A ação deste assalto já foi usada.');
            vehicles.combat.actionUsed = true;
        }
        const copilot=consumeVehicleCopilot(active);
        const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,skillKey,active.id);
        const result = vehicles_mjs_1.attemptFullThrottle(copilot.vehicle, character, { skillKey, asManeuver, currentRound: vehicles.combat.round, rng, difficultyDowngrades:copilot.downgrades,difficultyUpgrades:officialUnpreparedUpgrade });
        if (result.personalStrainCost > 0)
            character = health_mjs_1.sufferStrain(character, result.personalStrainCost,{voluntary:true}).character;
        vehicles.vehicles[active.id] = result.vehicle;
        vehicles.combat.log = [...(vehicles.combat.log ?? []), { type: 'ACELERACAO_MAXIMA', round: vehicles.combat.round, vehicleId: active.id, success: result.check.success, bonus: result.bonus, asManeuver }];
        this._recordSWE02VehicleAction(vehicles,asManeuver?'maneuver':'action');
        this.commit('VEHICLES_UPDATED', { vehicles });
        if (result.personalStrainCost > 0)
            this.commit('CHARACTER_UPDATED', { character });
        return { ...result, character, combat: vehicles.combat };
    }
    /** GM-authoritative formation membership: never fabricate an ally or distance. */
    confirmAoRFormationAlly({vehicleId,approved=false,closeToActive=false}={}){
      return this.atomicAction(()=>{
        const vehicles=clone(this.state.vehicles),v=vehicles.vehicles?.[vehicleId];
        if(!['AOR','MIXED'].includes(campaignMode(this.state))||!vehicles.combat?.active||
          !v||vehicleId===vehicles.activeVehicleId||vehicleId===vehicles.combat.targetVehicleId)
          throw Error('Formação exige veículo registrado e combate AoR ativo.');
        if(approved!==true||closeToActive!==true)throw Error('Mestre deve confirmar aliança e alcance Curto.');
        v.alliedToPlayer=true;v.closeToActive=true;
        this.commit('VEHICLES_UPDATED',{vehicles});return {vehicleId,alliedToPlayer:true,closeToActive:true};
      });
    }
    gainVehicleAdvantage({ zone = 'proa', skillKey = 'pilotar_vehiculo_espacial', alliedVehicleIds = [], rng = Math.random } = {}) {
        this._assertSWE02VehicleTurn('player');
        const character=requireCharacter(this.state.player.character), vehicles=clone(this.state.vehicles);
        if(!vehicles.combat?.active) throw new Error('Ganhar a Vantagem exige combate veicular ativo.');
        if(vehicles.combat.actionUsed) throw new Error('A ação deste assalto já foi usada.');
        const active=vehicles.vehicles?.[vehicles.activeVehicleId], target=vehicles.vehicles?.[vehicles.combat.targetVehicleId];
        if(!active||!target) throw new Error('Atacante ou alvo de veículo ausente.');
        if(!vehicles_mjs_1.canGainAdvantage(active)) throw new Error('Ganhar a Vantagem exige Silhueta 1–4 e Velocidade 4+.');
        if(!['pilotar_vehiculo_espacial','pilotar_vehiculo_planetario'].includes(skillKey)) throw new Error('Ganhar a Vantagem exige Pilotagem.');
        // AoR Form on Me: GM must mark real registered vehicles as allies
        // and confirm they remain at Close range. No synthetic squadrons.
        if(!Array.isArray(alliedVehicleIds)||new Set(alliedVehicleIds).size!==alliedVehicleIds.length)
            throw Error('Formação de combate exige IDs aliados distintos.');
        const formationRank=talents_mjs_1.sourceBackedTalentRank(character,'FORM_ON_ME');
        const leadership=Number(character.skills?.liderazgo??0);
        if(alliedVehicleIds.length&&(!formationRank||alliedVehicleIds.length>leadership))
            throw Error('Formação de combate não adquirida ou excede Liderança.');
        for(const id of alliedVehicleIds){
          const ally=vehicles.vehicles?.[id];
          if(typeof id!=='string'||!ally||id===active.id||id===target.id||
             ally.alliedToPlayer!==true||ally.closeToActive!==true)
            throw Error('Formação exige nave aliada real em alcance Curto confirmado pelo Mestre.');
        }
        const pair=[String(active.id),String(target.id)].sort().join('::');
        const repeatCount=Math.max(0,Number(vehicles.combat.gainAdvantageCounts?.[pair]??0));
        const baseDifficulty=vehicles_mjs_1.gainAdvantageDifficulty(active.speed,target.speed,repeatCount);
        const copilot=consumeVehicleCopilot(active); vehicles.vehicles[active.id]=copilot.vehicle;
        const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,skillKey,active.id);
        const upgraded=difficultyWithUpgrades(baseDifficulty,0,officialUnpreparedUpgrade);
        const negative=difficultyWithDowngrades(upgraded.difficulty,upgraded.challenge,copilot.downgrades);
        const characteristicKey=character_mjs_1.SKILLS[skillKey]??'agilidad';
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.[characteristicKey]??0),Number(character.skills?.[skillKey]??0));
        const handling=vehicles_mjs_1.handlingPoolModifier(copilot.vehicle);
        const talentModifiers=talents_mjs_1.talentSkillPoolModifiers(character,skillKey,{boost:handling.boost,setback:handling.setback});
        const check={...narrativeDice_mjs_1.rollPool({...positive,...negative,boost:talentModifiers.boost,setback:talentModifiers.setback},rng),talentModifiers};
        if(check.success){
            vehicles.vehicles[active.id]=vehicles_mjs_1.applyGainAdvantage(vehicles.vehicles[active.id],{targetVehicleId:target.id,currentRound:vehicles.combat.round,zone});
            vehicles.combat.gainAdvantageCounts={...(vehicles.combat.gainAdvantageCounts??{}),[pair]:repeatCount+1};
            for(const id of alliedVehicleIds){
              const ally=vehicles_mjs_1.applyGainAdvantage(vehicles.vehicles[id],{targetVehicleId:target.id,currentRound:vehicles.combat.round,zone});
              ally.roundEffects.gainAdvantage.formationLeaderVehicleId=active.id;
              vehicles.vehicles[id]=ally;
            }
        }
        vehicles.combat.actionUsed=true;
        vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'GANHAR_VANTAGEM',round:vehicles.combat.round,vehicleId:active.id,targetId:target.id,success:Boolean(check.success),zone,formationAllies:check.success?[...alliedVehicleIds]:[],difficulty:negative.difficulty,baseDifficulty}];
        this._recordSWE02VehicleAction(vehicles,'action');
        this.commit('VEHICLES_UPDATED',{vehicles}); this.commit('CHECK_RESOLVED',check);
        return {check,success:Boolean(check.success),difficulty:negative.difficulty,baseDifficulty,difficultyDowngrades:copilot.downgrades,zone,formationAllies:check.success?[...alliedVehicleIds]:[],vehicle:clone(vehicles.vehicles[active.id]),combat:clone(vehicles.combat)};
    }
    useVehicleCrewAction(kind,{ skillKey=null, zone='proa', rng=Math.random, spendAdvantage=false, spendTriumph=false, weaponIndex=0, gmAllowed=false }={}) {
        this._assertSWE02VehicleTurn('player');
        const character=requireCharacter(this.state.player.character), vehicles=clone(this.state.vehicles);
        if(!vehicles.combat?.active) throw new Error('A ação de tripulação exige combate veicular ativo.');
        if(vehicles.combat.actionUsed) throw new Error('A ação deste assalto já foi usada.');
        const active=vehicles.vehicles?.[vehicles.activeVehicleId], target=vehicles.vehicles?.[vehicles.combat.targetVehicleId];
        if(!active) throw new Error('Nenhuma nave/veículo ativo.');
        const defs={
            plot_course:{skills:{astronavegacion:2,percepcion:3},defaultSkill:'astronavegacion'},
            copilot:{skills:{pilotar_vehiculo_espacial:2,pilotar_vehiculo_planetario:2},defaultSkill:'pilotar_vehiculo_espacial'},
            boost_shields:{skills:{mecanica:3},defaultSkill:'mecanica'},
            fire_discipline:{skills:{disciplina:3,liderazgo:3},defaultSkill:'disciplina'},
            scan_enemy:{skills:{percepcion:3},defaultSkill:'percepcion'},
            slice_enemy:{skills:{computadoras:3},defaultSkill:'computadoras'},
            spoof_missiles:{skills:{computadoras:2,alerta:3},defaultSkill:'computadoras'},
            manual_repairs:{skills:{atletismo:3},defaultSkill:'atletismo'}
        };
        const def=defs[String(kind??'')]; if(!def) throw new Error('Ação de tripulação veicular desconhecida.');
        const chosen=String(skillKey||def.defaultSkill); if(!(chosen in def.skills)) throw new Error('Perícia inválida para esta ação veicular.');
        if(['scan_enemy','slice_enemy'].includes(kind)&&!target) throw new Error('Esta ação exige um veículo inimigo como alvo.');
        if(kind==='manual_repairs'){
            if(!gmAllowed) throw new Error('Reparo Manual exige confirmação de que o contexto permite acessar e reparar o casco.');
            const hasToolkit=(character.gear??[]).some(g=>String(g?.id??g?.sourceCatalogId??'')==='tool_kit'||/kit de ferramentas|tool kit/i.test(String(g?.name??'')));
            if(!hasToolkit) throw new Error('Reparo Manual exige Kit de ferramentas possuído.');
            const attempts=vehicles.combat.hullDamageControlAttempts??{};
            if(attempts[active.id]) throw new Error('O reparo de Trauma de Casco por Controle de Danos/Reparo Manual só pode ser tentado uma vez por encontro.');
            vehicles.combat.hullDamageControlAttempts={...attempts,[active.id]:true};
        }
        const characteristicKey=character_mjs_1.SKILLS[chosen]; if(!characteristicKey) throw new Error('Perícia sem característica associada.');
        const difficulty=Number(def.skills[chosen]);
        const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,chosen,active.id);
        const negative=difficultyWithUpgrades(difficulty,0,officialUnpreparedUpgrade);
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(character.characteristics?.[characteristicKey]??0),Number(character.skills?.[chosen]??0));
        const talentModifiers=talents_mjs_1.talentSkillPoolModifiers(character,chosen,{boost:0,setback:0});
        const check={...narrativeDice_mjs_1.rollPool({...positive,...negative,boost:talentModifiers.boost,setback:talentModifiers.setback},rng),talentModifiers};
        let effect=null;
        if(check.success){
            const successes=Math.max(1,Number(check.netSuccess??1));
            if(kind==='plot_course'){ active.roundEffects={...(active.roundEffects??{}),plotCourse:{setbackReduction:successes}}; effect={setbackReduction:successes}; }
            else if(kind==='copilot'){ active.roundEffects={...(active.roundEffects??{}),copilot:{downgrades:successes}}; effect={difficultyDowngrades:successes}; }
            else if(kind==='boost_shields'){ vehicles.vehicles[active.id]=vehicles_mjs_1.applyBoostShields(active,{zone,currentRound:vehicles.combat.round,durationRounds:successes}); effect={zone,durationRounds:successes,systemStrain:1}; }
            else if(kind==='fire_discipline'){
                const boostedAttacks=1+Math.floor(Math.max(0,successes-1)/2);
                const advantageSpent=spendAdvantage&&Number(check.netAdvantage??0)>=3?3:0;
                active.roundEffects={...(active.roundEffects??{}),fireDiscipline:{remainingAttacks:boostedAttacks,systemStrainOnHit:advantageSpent===3}};
                effect={boostedAttacks,advantageSpent,systemStrainOnHit:advantageSpent===3,remainingAdvantage:Number(check.netAdvantage??0)-advantageSpent};
            }
            else if(kind==='scan_enemy'){
                const advantageSpent=spendAdvantage&&Number(check.netAdvantage??0)>=2?2:0;
                effect={targetVehicleId:target.id,weapons:(target.weapons??[]).map(w=>({name:w.name,range:w.range,qualities:clone(w.qualities??{})})),modifications:clone(target.attachments??target.modifications??[]),hullThreshold:Number(target.hullThreshold??0),systemStrainThreshold:Number(target.systemStrainThreshold??0),advantageSpent,remainingAdvantage:Number(check.netAdvantage??0)-advantageSpent};
                if(advantageSpent===2){effect.hullDamage=Number(target.hullDamage??0);effect.systemStrain=Number(target.systemStrain??0);}
                vehicles.combat.lastScan=clone(effect);
            }
            else if(kind==='slice_enemy'){
                let sliced=vehicles_mjs_1.applySlicedDefense(target,{zone,currentRound:vehicles.combat.round,durationRounds:successes});
                const advantageSpent=spendAdvantage&&Number(check.netAdvantage??0)>=2?2:0;
                const triumphSpent=spendTriumph&&Number(check.triumph??0)>=1?1:0;
                if(advantageSpent===2) sliced=vehicles_mjs_1.applyVehicleSystemStrain(sliced,1).vehicle;
                let disabledWeaponIndex=null;
                if(triumphSpent===1){
                    const wi=Math.max(0,Math.min((sliced.weapons?.length??1)-1,Number(weaponIndex)||0));
                    if(sliced.weapons?.[wi]){sliced.weapons[wi].runtimeState={...(sliced.weapons[wi].runtimeState??{}),disabledUntilRound:Number(vehicles.combat.round)};disabledWeaponIndex=wi;}
                }
                vehicles.vehicles[target.id]=sliced;
                effect={targetVehicleId:target.id,zone,durationRounds:successes,advantageSpent,triumphSpent,systemStrainInflicted:advantageSpent===2?1:0,disabledWeaponIndex,remainingAdvantage:Number(check.netAdvantage??0)-advantageSpent,remainingTriumph:Number(check.triumph??0)-triumphSpent};
            }
            else if(kind==='spoof_missiles'){
                const upgrades=1+Math.floor(Math.max(0,successes-1)/2);
                vehicles.vehicles[active.id]=vehicles_mjs_1.applySpoofingMissiles(active,{upgrades,currentRound:vehicles.combat.round}); effect={difficultyUpgrades:upgrades};
            }
            else if(kind==='manual_repairs'){
                const repaired=vehicles_mjs_1.applyManualRepairs(active,{successes,operator:character}); vehicles.vehicles[active.id]=repaired.vehicle; effect={hullRecovered:repaired.hullRecovered};
            }
            if(['plot_course','copilot','fire_discipline'].includes(kind)) vehicles.vehicles[active.id]=active;
        }
        vehicles.combat.actionUsed=true;
        vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'ACAO_TRIPULACAO_VEICULO',kind,round:vehicles.combat.round,vehicleId:active.id,targetId:target?.id??null,success:Boolean(check.success),skillKey:chosen,difficulty,effect:clone(effect)}];
        this._recordSWE02VehicleAction(vehicles,'crew-action');
        this.commit('VEHICLES_UPDATED',{vehicles}); this.commit('CHECK_RESOLVED',check);
        return {kind,skillKey:chosen,difficulty,check,success:Boolean(check.success),effect,vehicle:clone(vehicles.vehicles[active.id]),target:target?clone(vehicles.vehicles[target.id]):null,combat:clone(vehicles.combat)};
    }
    useControlledFire() {
        throw new Error('Fogo controlado pertence ao legado Age of Rebellion e não faz parte das 18 especializações EotE Core ativas.');
    }
    /** P175 — Brilliant Evasion, AoR Core p.144: once per vehicle encounter,
     * one opposed Piloting action can bar the selected enemy from attacking
     * the player's craft for Agility rounds. NPC skill is GM-adjudicated. */
    useAoRBrilliantEvasion({npcAgility=3,npcPiloting=2,
        skillKey='pilotar_vehiculo_espacial',rng=Math.random}={}){
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player?.character);
            if(character.creation?.rulesProfile!=='AOR_CORE'||
               !['AOR','MIXED'].includes(this.state.campaignMode)||
               talents_mjs_1.sourceBackedTalentRank(character,'BRILLIANT_EVASION')<1)
                throw new Error('Evasão impecável AoR não adquirida.');
            if(!['pilotar_vehiculo_espacial','pilotar_vehiculo_planetario'].includes(skillKey)||
               !Number.isSafeInteger(npcAgility)||npcAgility<0||npcAgility>6||
               !Number.isSafeInteger(npcPiloting)||npcPiloting<0||npcPiloting>5)
                throw new Error('Oposição de pilotagem NPC inválida.');
            const vehicles=clone(this.state.vehicles),combat=vehicles.combat;
            if(!combat.active||!vehicles.activeVehicleId||!combat.targetVehicleId)
                throw new Error('Evasão impecável exige combate veicular ativo.');
            if(combat.actionUsed||combat.brilliantEvasionAttempted)
                throw new Error('Evasão impecável já tentada ou ação do assalto utilizada.');
            const ownVehicle=vehicles.vehicles[vehicles.activeVehicleId],enemy=vehicles.vehicles[combat.targetVehicleId];
            if(!ownVehicle||!enemy||ownVehicle.id===enemy.id)
                throw new Error('Naves válidas são necessárias para Evasão impecável.');
            assertCanPerformAction(character);
            const negative=narrativeDice_mjs_1.buildOpposedNegativePool(npcAgility,npcPiloting);
            const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,skillKey,ownVehicle.id);
            const upgraded=difficultyWithUpgrades(negative.difficulty,negative.challenge,officialUnpreparedUpgrade);
            const positive=narrativeDice_mjs_1.buildPositivePool(character.characteristics.agilidad,character.skills[skillKey]);
            const check=narrativeDice_mjs_1.rollPool({...positive,...upgraded,boost:0,setback:0},rng);
            combat.actionUsed=true;combat.brilliantEvasionAttempted=true;
            if(check.success)combat.brilliantEvasion={targetVehicleId:enemy.id,
                startsAtRound:combat.round,untilRound:combat.round+Math.max(1,Number(character.characteristics.agilidad))-1};
            combat.log.push({type:'AOR_BRILLIANT_EVASION',round:combat.round,targetVehicleId:enemy.id,
                success:check.success,untilRound:combat.brilliantEvasion?.untilRound??null});
            this.commit('VEHICLES_UPDATED',{vehicles});this.commit('CHECK_RESOLVED',check);
            return {check,success:check.success,untilRound:combat.brilliantEvasion?.untilRound??null,
                targetVehicleId:enemy.id};
        });
    }
    /** P150 — the enemy vehicle now has a real attack against the player's
     * selected ship. Its pool uses explicitly adjudicated NPC Gunnery values;
     * Tricky Target applies only if the player is the pilot. Damage, ammo,
     * once-per-round marker and log are persisted in one repository write. */
    resolveIncomingVehicleAttack({ npcAgility = 3, npcGunnery = 2,
        weaponIndex = 0, facing = 'proa', playerPiloting = true,
        rng = Math.random } = {}) {
        return this.atomicAction(() => {
            this._assertSWE02VehicleTurn('npc');
            const player = requireCharacter(this.state.player?.character);
            const vehicles = clone(this.state.vehicles);
            const combat = vehicles.combat;
            if (!combat?.active) throw new Error('Ataque inimigo exige combate veicular ativo.');
            if (combat.npcAttackResolvedRound === combat.round)
                throw new Error('O veículo inimigo já atacou neste assalto.');
            if (combat.pendingIncomingCritical)
                throw new Error('Resolva ou dispense o crítico inimigo pendente antes de novo ataque.');
            // Brilliant Evasion bars this enemy's attack before ammo, weapon
            // cooldown, critical or damage are touched. A blocked NPC turn is
            // still spent once, so repeated calls cannot bypass suppression.
            if(combat.brilliantEvasion?.targetVehicleId===combat.targetVehicleId&&
               combat.round>=combat.brilliantEvasion.startsAtRound&&
               combat.round<=combat.brilliantEvasion.untilRound){
                combat.npcAttackResolvedRound=combat.round;
                combat.lastIncomingAttack={round:combat.round,attackerId:combat.targetVehicleId,
                    targetId:vehicles.activeVehicleId,blockedBy:'BRILLIANT_EVASION',hit:false,damage:0};
                combat.log.push({type:'AOR_BRILLIANT_EVASION_BLOCK',round:combat.round,
                    attackerId:combat.targetVehicleId});
                this.commit('VEHICLES_UPDATED',{vehicles});
                return {blocked:true,reason:'BRILLIANT_EVASION',attack:null,
                    vehicle:clone(vehicles.vehicles[vehicles.activeVehicleId]),pendingCritical:null};
            }
            if (![npcAgility, npcGunnery, weaponIndex].every(Number.isSafeInteger) ||
                npcAgility < 0 || npcAgility > 6 || npcGunnery < 0 || npcGunnery > 5 ||
                weaponIndex < 0 || !['proa','popa','babor','estribor'].includes(facing) ||
                typeof playerPiloting !== 'boolean')
                throw new Error('Parâmetros de ataque veicular inimigo inválidos.');
            const target = vehicles.vehicles?.[vehicles.activeVehicleId];
            const attacker = vehicles.vehicles?.[combat.targetVehicleId];
            if (!attacker || !target || attacker.id === target.id)
                throw new Error('Nave inimiga ou nave do jogador indisponível.');
            if(attacker.key==='storm_iv_cloud_car'&&attacker.wingGuardPatrolId){
                npcAgility=BESPIN_PILOT.agility;
                npcGunnery=BESPIN_PILOT.soloSkillRank;
            }
            if (vehicles_mjs_1.vehicleStatus(attacker,{currentRound:combat.round}).destroyed ||
                vehicles_mjs_1.vehicleStatus(target,{currentRound:combat.round}).destroyed)
                throw new Error('Uma nave destruída não pode atacar ou receber ataque.');
            const weapon = attacker.weapons?.[weaponIndex];
            if (!weapon) throw new Error('Arma do veículo inimigo inválida.');
            weapon.runtimeState = {...(weapon.runtimeState??{})};
            const ammo = Math.max(0,Number(weapon.qualities?.limitedAmmo??0));
            if (ammo > 0) {
                if (weapon.runtimeState.limitedAmmoRemaining === undefined)
                    weapon.runtimeState.limitedAmmoRemaining = ammo;
                if (Number(weapon.runtimeState.limitedAmmoRemaining) <= 0)
                    throw new Error('Veículo inimigo sem munição limitada.');
            }
            const slow = Math.max(0,Number(weapon.qualities?.slowFiring??0));
            if (slow > 0 && Number.isFinite(Number(weapon.runtimeState.lastFiredRound)) &&
                Number(combat.round) <= Number(weapon.runtimeState.lastFiredRound) + slow)
                throw new Error('Arma inimiga ainda em cadência lenta.');
            if(combat.publishedVehicleContext?.id===THWHEEK_CONTEXT){
                if(attacker.id!==THWHEEK_ID||weaponIndex!==0)throw Error('SWE02: arma de Thwheek inválida.');
                if(attacker.hullDamage>=5)throw Error('SWE02: Thwheek está fugindo para a atmosfera e não ataca.');
                npcAgility=3;npcGunnery=0; // SWE02 p.12: Agilidade 3; Gunnery sem graduação listada.
                const wasHit=Boolean(combat.publishedVehicleContext.firstHitRecorded)||
                    (combat.log||[]).some(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===THWHEEK_ID&&x.hit);
                attacker.roundEffects.stayOnTarget=!wasHit;
                attacker.roundEffects.evasive=wasHit;
                combat.publishedVehicleContext.firstHitRecorded=wasHit;
            }
            const npc = {characteristics:{agilidad:npcAgility},skills:{[weapon.skill]:npcGunnery}};
            const attack = vehicles_mjs_1.resolveVehicleWeaponAttack({
                character:npc, attacker, target, targetOperator:playerPiloting?player:null,
                facing, weaponIndex, targetRange:combat.range, rng, currentRound:combat.round
            });
            if (ammo > 0) weapon.runtimeState.limitedAmmoRemaining -= 1;
            if (slow > 0) weapon.runtimeState.lastFiredRound = combat.round;
            vehicles.vehicles[attacker.id] = attacker;
            if (attack.targetDamage) vehicles.vehicles[target.id] = attack.targetDamage.vehicle;
            else if (attack.targetEffect?.vehicle) vehicles.vehicles[target.id] = attack.targetEffect.vehicle;
            combat.npcAttackResolvedRound = combat.round;
            combat.lastIncomingAttack = { round:combat.round, attackerId:attacker.id,
                targetId:target.id, hit:attack.hit, damage:attack.targetDamage?.suffered??0,
                targetSilhouette:attack.targetSilhouette, baseDifficulty:attack.baseDifficulty,
                difficulty:attack.difficulty, challenge:attack.challenge,
                check:clone(attack.check), criticalAvailable:attack.criticalAvailable };
            if (attack.criticalAvailable) {
                combat.pendingIncomingCritical = { targetVehicleId:target.id,
                    attackRound:combat.round, weapon:weapon.name,
                    defenseZone:facing, cost:attack.criticalCost,
                    remainingAdvantage:Math.max(0,Number(attack.check.netAdvantage??0)),
                    remainingTriumph:Math.max(0,Number(attack.check.triumph??0)), rawRoll:null };
            }
            combat.log = [...(combat.log??[]),{type:'ATAQUE_VEICULAR_INIMIGO',
                round:combat.round, attackerId:attacker.id,targetId:target.id,
                hit:attack.hit,damage:attack.targetDamage?.suffered??0,
                targetSilhouette:attack.targetSilhouette,playerPiloting,
                criticalAvailable:attack.criticalAvailable}];
            this._recordSWE02VehicleAction(vehicles,'attack');
            this.commit('VEHICLES_UPDATED',{vehicles});
            return {attack, vehicle:clone(vehicles.vehicles[target.id]),
                pendingCritical:clone(combat.pendingIncomingCritical??null)};
        });
    }
    /** A critical is not automatic: spend the enemy's actual Advantage or
     * Triumph once, with the same component-choice flow as player attacks. */
    activateIncomingVehicleCritical({component=null,rng=Math.random}={}) {
        return this.atomicAction(() => {
            const vehicles=clone(this.state.vehicles), combat=vehicles.combat;
            const pending=combat?.pendingIncomingCritical;
            if (!combat?.active || !pending) throw new Error('Nenhum crítico veicular inimigo pendente.');
            const target=vehicles.vehicles?.[pending.targetVehicleId];
            if (!target || target.id!==vehicles.activeVehicleId)
                throw new Error('Alvo do crítico inimigo não está mais disponível.');
            if (pending.remainingAdvantage < pending.cost && pending.remainingTriumph < 1)
                throw new Error('Resultados insuficientes para crítico veicular inimigo.');
            const rawRoll=pending.rawRoll ?? Math.floor(Math.max(0,Math.min(.999999999999,Number(rng())))*100)+1;
            if (!Number.isSafeInteger(rawRoll) || rawRoll<1 || rawRoll>100)
                throw new Error('Rolagem crítica inimiga inválida.');
            const preview=vehicles_mjs_1.vehicleCriticalResult(rawRoll+
                vehicles_mjs_1.unrepairedVehicleCriticalCount(target)*10);
            const needsComponent=['component_hit','major_system_failure'].includes(preview.key);
            const choices=target.silhouette<=4?vehicles_mjs_1.SMALL_VEHICLE_COMPONENTS:vehicles_mjs_1.LARGE_VEHICLE_COMPONENTS;
            if (needsComponent && !component) {
                combat.pendingIncomingCritical={...pending,rawRoll};
                this.commit('VEHICLES_UPDATED',{vehicles});
                return {activated:false,componentRequired:true,preview,componentChoices:[...choices]};
            }
            if (component!==null && !choices.includes(component))
                throw new Error('Componente do crítico inimigo inválido.');
            const applied=vehicles_mjs_1.applyVehicleCritical(target,{roll:rawRoll,
                component,defenseZone:pending.defenseZone,currentRound:combat.round});
            vehicles.vehicles[target.id]=applied.vehicle;
            combat.pendingIncomingCritical=null;
            combat.log=[...(combat.log??[]),{type:'CRITICO_VEICULAR_INIMIGO',
                round:combat.round,targetId:target.id,criticalKey:applied.hit.key,
                paidWith:pending.remainingAdvantage>=pending.cost?'advantage':'triumph'}];
            this.commit('VEHICLES_UPDATED',{vehicles});
            return {activated:true,...applied,targetId:target.id};
        });
    }
    declineIncomingVehicleCritical() {
        return this.atomicAction(() => {
            const vehicles=clone(this.state.vehicles);
            if (!vehicles.combat?.pendingIncomingCritical)
                throw new Error('Nenhum crítico inimigo pendente para dispensar.');
            vehicles.combat.pendingIncomingCritical=null;
            this.commit('VEHICLES_UPDATED',{vehicles});
            return {declined:true};
        });
    }
    resolveVehicleAttack({ weaponIndex = 0, facing = 'proa', useExhaustPort=false, useDeadToRights=false, braceEnvironmentalSetback=0, rng = Math.random } = {}) {
        return this.atomicAction(() => {
            this._assertSWE02VehicleTurn('player');
        const character = requireCharacter(this.state.player.character);
        const vehicles = clone(this.state.vehicles);
        const declaredBraceEnvironment=Number(braceEnvironmentalSetback);
        if(!Number.isSafeInteger(declaredBraceEnvironment)||declaredBraceEnvironment<0||declaredBraceEnvironment>20)
            throw new Error('Brace exige um número válido de Setbacks ambientais veiculares.');
        if (!vehicles.combat.active)
            throw new Error('Inicie um combate de naves/veículos.');
        if (vehicles.combat.actionUsed)
            throw new Error('A ação deste assalto já foi usada.');
        const attacker = vehicles.vehicles?.[vehicles.activeVehicleId], target = vehicles.vehicles?.[vehicles.combat.targetVehicleId];
        if (!attacker || !target)
            throw new Error('Atacante ou alvo de veículo ausente.');
        const weapon=attacker.weapons?.[Number(weaponIndex)];
        if(!weapon) throw new Error('Arma veicular inválida.');
        weapon.runtimeState={...(weapon.runtimeState??{})};
        if(Number.isFinite(Number(weapon.runtimeState.disabledUntilRound))&&Number(vehicles.combat.round)<=Number(weapon.runtimeState.disabledUntilRound)) throw new Error('Este sistema de armas está desativado neste assalto.');
        const limitedAmmo=Math.max(0,Number(weapon.qualities?.limitedAmmo??0));
        if(limitedAmmo>0){
            if(weapon.runtimeState.limitedAmmoRemaining===undefined) weapon.runtimeState.limitedAmmoRemaining=limitedAmmo;
            if(Number(weapon.runtimeState.limitedAmmoRemaining)<=0) throw new Error('A arma veicular está sem Munição Limitada.');
        }
        const slowFiring=Math.max(0,Number(weapon.qualities?.slowFiring??0));
        const lastFired=Number(weapon.runtimeState.lastFiredRound);
        if(slowFiring>0&&Number.isFinite(lastFired)&&Number(vehicles.combat.round)<=lastFired+slowFiring)
            throw new Error(`Cadência Lenta ${slowFiring}: esta arma ainda não pode disparar neste assalto.`);
        if(typeof useExhaustPort!=='boolean'||typeof useDeadToRights!=='boolean')throw Error('Opções de talentos veiculares inválidas.');
        if(useExhaustPort&&talents_mjs_1.sourceBackedTalentRank(character,'EXHAUST_PORT')<1)
          throw Error('Saída térmica AoR não adquirida.');
        if(useDeadToRights&&talents_mjs_1.sourceBackedTalentRank(character,'DEAD_TO_RIGHTS')<1)
          throw Error('Impacto direto AoR não adquirido.');
        const massiveRating=Number(target.massive??0);
        if(useExhaustPort&&massiveRating<1)throw Error('Saída térmica só tem efeito contra alvo com regra Massive.');
        const requiredDestiny=Number(useExhaustPort)+Number(useDeadToRights);
        if(Number(this.state.group?.destiny?.light??0)<requiredDestiny)throw Error('Pontos de Destino luminosos insuficientes.');
        const improved=talents_mjs_1.sourceBackedTalentRank(character,'DEAD_TO_RIGHTS_IMPROVED')>0;
        const deadToRightsBonus=useDeadToRights?(improved?Number(character.characteristics?.agilidad??0):Math.ceil(Number(character.characteristics?.agilidad??0)/2)):0;
        const temp=vehicles_mjs_1.vehicleTemporaryEffects(attacker,{currentRound:vehicles.combat.round});
        const formationLeaderId=temp.gainAdvantage?.formationLeaderVehicleId;
        const formationStillValid=!formationLeaderId||Boolean(vehicles.vehicles?.[formationLeaderId]&&attacker.alliedToPlayer===true&&attacker.closeToActive===true&&
          String(vehicles_mjs_1.vehicleTemporaryEffects(vehicles.vehicles[formationLeaderId],{currentRound:vehicles.combat.round}).gainAdvantage?.targetVehicleId??'')===String(target.id));
        const effectiveFacing=formationStillValid&&String(temp.gainAdvantage?.targetVehicleId??'')===String(target.id)?String(temp.gainAdvantage?.zone??facing):facing;
        const fireDiscipline=Math.max(0,Number(attacker.roundEffects?.fireDiscipline?.remainingAttacks??0));
        const fireDisciplineStrain=Boolean(attacker.roundEffects?.fireDiscipline?.systemStrainOnHit);
        const vehicleBracePending=clone(vehicles.combat.aorBracePrepared??null);
        const personalBracePending=vehicleBracePending?null:clone(this.state.combat?.aorBracePrepared??null);
        const bracePending=vehicleBracePending??personalBracePending;
        const activeCharacterId=this.state.player?.roster?.activeCharacterId??character.id??null;
        if(bracePending&&bracePending.characterId!==activeCharacterId)throw new Error('Brace foi preparado por outra ficha; encerre ou limpe essa preparação antes de agir.');
        const braceResult=bracePending?talents_mjs_1.braceEnvironmentalSetbackRelief(character,{maneuverPrepared:true,environmentalSetback:declaredBraceEnvironment}):null;
        const braceRemoved=Number(braceResult?.removedSetback??0);
        const attack = vehicles_mjs_1.resolveVehicleWeaponAttack({ character, attacker, target, facing:effectiveFacing, weaponIndex, targetRange: vehicles.combat.range, rng, currentRound:vehicles.combat.round, extraBoost:fireDiscipline>0?1:0,extraSetback:Math.max(0,declaredBraceEnvironment-braceRemoved),ignoreMassive:useExhaustPort,deadToRightsBonus,allVehicles:vehicles.vehicles });
        attack.aorBrace=bracePending?{rank:braceResult.rank,environmentalSetback:declaredBraceEnvironment,removedSetback:braceRemoved,source:clone(bracePending.source)}:null;
        if(useExhaustPort||(useDeadToRights&&attack.hit)){
            let group=clone(this.state.group);
            if(useExhaustPort)group=destiny_mjs_1.flipDestiny(group,'light');
            if(useDeadToRights&&attack.hit)group=destiny_mjs_1.flipDestiny(group,'light');
            this.commit('GROUP_UPDATED',{group});
        }
        attack.exhaustPortUsed=useExhaustPort;
        attack.deadToRightsUsed=useDeadToRights&&attack.hit;
        if(fireDiscipline>0){ attacker.roundEffects.fireDiscipline.remainingAttacks=fireDiscipline-1; if(attacker.roundEffects.fireDiscipline.remainingAttacks<=0) delete attacker.roundEffects.fireDiscipline; }
        if(limitedAmmo>0) weapon.runtimeState.limitedAmmoRemaining=Math.max(0,Number(weapon.runtimeState.limitedAmmoRemaining)-1);
        if(slowFiring>0) weapon.runtimeState.lastFiredRound=Number(vehicles.combat.round);
        vehicles.vehicles[attacker.id]=attacker;
        vehicles.combat.actionUsed = true;
        vehicles.combat.lastAttack = attack;
        vehicles.combat.pendingTargetCritical = null;
        vehicles.combat.pendingVehicleSpend = null;
        if (attack.targetDamage)
            vehicles.vehicles[target.id] = attack.targetDamage.vehicle;
        else if (attack.targetEffect?.vehicle)
            vehicles.vehicles[target.id] = attack.targetEffect.vehicle;
        if(vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT && target.id===THWHEEK_ID && attack.hit){
            vehicles.combat.publishedVehicleContext.firstHitRecorded=true;
            vehicles.vehicles[target.id].roundEffects.stayOnTarget=false;
            vehicles.vehicles[target.id].roundEffects.evasive=true;
        }
        if(attack.hit&&fireDisciplineStrain){
            const strained=vehicles_mjs_1.applyVehicleSystemStrain(vehicles.vehicles[target.id],1);
            vehicles.vehicles[target.id]=strained.vehicle;
            attack.fireDisciplineSystemStrain=1;
        }
        const qualities=[];
        // P165: ranks must originate from purchased AoR tree nodes, never display labels.
        const overwhelmRanks=talents_mjs_1.sourceBackedTalentRank(character,'OVERWHELMDEF');
        const debilShotRanks=talents_mjs_1.sourceBackedTalentRank(character,'DEBILSHOT');
        if(!attack.hit&&overwhelmRanks>0) qualities.push({key:'aor_overwhelm',label:'Sobrecarregar defesas',rank:overwhelmRanks,cost:2,repeatable:true});
        if(attack.hit&&debilShotRanks>0) qualities.push({key:'aor_debilitating',label:'Disparo debilitante',rank:1,cost:2,repeatable:false});
        if(!attack.hit&&Number(attack.guided??0)>0) qualities.push({key:'guided',label:`Guiada ${Number(attack.guided)}`,rank:Number(attack.guided),cost:3,repeatable:false});
        if(attack.hit&&Number(attack.linked??0)>0) qualities.push({key:'linked',label:`Vinculada ${Number(attack.linked)}`,rank:Number(attack.linked),cost:2,repeatable:true});
        vehicles.combat.pendingVehicleSpend={
            attackId:ids_mjs_1.newId('vatk'),targetVehicleId:target.id,weaponIndex:Number(weaponIndex),weapon:clone(weapon),facing:effectiveFacing,
            remainingAdvantage:Math.max(0,Number(attack.check?.netAdvantage??0)),remainingTriumph:Math.max(0,Number(attack.check?.triumph??0)),qualities,activations:{},
            attackSnapshot:{hit:Boolean(attack.hit),damage:Number(attack.damage??0),breach:Number(attack.breach??0),ion:Boolean(attack.ion),guided:Number(attack.guided??0),linked:Number(attack.linked??0)}
        };
        if(attack.criticalAvailable&&Number(attack.targetDamage?.suffered??0)>0){
            vehicles.combat.pendingTargetCritical={targetVehicleId:target.id,weapon:attack.weapon?.name??'Arma veicular',cost:Number(attack.criticalCost??0),byTriumph:Number(attack.check?.triumph??0)>0,defenseZone:effectiveFacing,rawRoll:null,componentRequired:false,paidWith:null};
        }
        vehicles.combat.log = [...(vehicles.combat.log ?? []), { type: 'ATAQUE_VEICULO', round: vehicles.combat.round, attackerId: attacker.id, targetId: target.id, hit: attack.hit, damage: attack.targetDamage?.suffered ?? 0, damageType:attack.ion?'systemStrain':'hull', specialEffect: attack.targetEffect?.type ?? null }];
        this._recordSWE02VehicleAction(vehicles,'attack');
        if(vehicleBracePending)delete vehicles.combat.aorBracePrepared;
        this.commit('VEHICLES_UPDATED', { vehicles });
        if(personalBracePending){const combat=clone(this.state.combat);delete combat.aorBracePrepared;this.commit('COMBAT_UPDATED',{combat});}
        return {...attack,pendingSpend:clone(vehicles.combat.pendingVehicleSpend)};
        });
    }
    activatePendingVehicleCritical({ component = null, rng = Math.random, useTriumph = false } = {}) {
        const vehicles=clone(this.state.vehicles),pending=vehicles.combat?.pendingTargetCritical;
        if(!pending) throw new Error('Não há Critical Hit veicular disponível para ativar.');
        const spend=vehicles.combat?.pendingVehicleSpend;
        if(!pending.paidWith&&spend&&String(spend.targetVehicleId)===String(pending.targetVehicleId)){
            const cost=Math.max(0,Number(pending.cost??0));
            if(useTriumph&&pending.byTriumph){
                if(Number(spend.remainingTriumph??0)<1) throw new Error('Não há Triunfo disponível para ativar este Critical Hit.');
                spend.remainingTriumph-=1; pending.paidWith='triumph';
            } else if(Number(spend.remainingAdvantage??0)>=cost){
                spend.remainingAdvantage-=cost; pending.paidWith='advantage';
            } else if(pending.byTriumph&&Number(spend.remainingTriumph??0)>=1){
                spend.remainingTriumph-=1; pending.paidWith='triumph';
            } else throw new Error('Resultados insuficientes para ativar o Critical Hit veicular.');
            vehicles.combat.pendingVehicleSpend=spend;
        } else if(!pending.paidWith&&!spend) pending.paidWith='legacy';
        const target=vehicles.vehicles?.[pending.targetVehicleId];
        if(!target) throw new Error('O alvo do Critical Hit não está mais no combate.');
        const hasStoredRawRoll=pending.rawRoll!==null&&pending.rawRoll!==undefined&&Number.isInteger(Number(pending.rawRoll))&&Number(pending.rawRoll)>=1&&Number(pending.rawRoll)<=100;
        const rawRoll=hasStoredRawRoll?Number(pending.rawRoll):(Math.floor(Math.max(0,Math.min(0.999999999999,Number(rng())))*100)+1);
        const total=rawRoll+(vehicles_mjs_1.unrepairedVehicleCriticalCount(target)*10);
        const preview=vehicles_mjs_1.vehicleCriticalResult(total);
        const needsComponent=['component_hit','major_system_failure'].includes(preview.key);
        const choices=target.silhouette<=4?[...vehicles_mjs_1.SMALL_VEHICLE_COMPONENTS]:[...vehicles_mjs_1.LARGE_VEHICLE_COMPONENTS];
        if(needsComponent&&!component){
            vehicles.combat.pendingTargetCritical={...pending,rawRoll,componentRequired:true,preview:{key:preview.key,title:preview.title,total},componentChoices:choices};
            this.commit('VEHICLES_UPDATED',{vehicles});
            return {activated:false,componentRequired:true,preview:{...preview,total,rawRoll},componentChoices:choices,pending:clone(vehicles.combat.pendingTargetCritical)};
        }
        if(component&&!choices.includes(component)) throw new Error('Componente veicular inválido para este alvo.');
        const result=vehicles_mjs_1.applyVehicleCritical(target,{roll:rawRoll,component:component??null,defenseZone:pending.defenseZone??'proa',currentRound:vehicles.combat.round});
        vehicles.vehicles[target.id]=result.vehicle;
        vehicles.combat.pendingTargetCritical=null;
        vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'CRITICO_VEICULAR_ATIVADO',round:vehicles.combat.round,targetId:target.id,weapon:pending.weapon,criticalKey:result.hit?.key??null,total:result.hit?.total??null}];
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {activated:true,...result,targetId:target.id};
    }
    activatePendingVehicleQuality({ qualityKey } = {}) {
        return this.atomicAction(() => {
        const vehicles=clone(this.state.vehicles),spend=vehicles.combat?.pendingVehicleSpend;
        if(!spend) throw new Error('Não há ataque veicular com resultados pendentes para gastar.');
        const quality=(spend.qualities??[]).find(q=>q.key===qualityKey);
        if(!quality) throw new Error('Qualidade veicular ativa indisponível para este ataque.');
        const activations=Number(spend.activations?.[quality.key]??0);
        if(!quality.repeatable&&activations>0) throw new Error('Esta qualidade já foi ativada neste ataque.');
        if(quality.key==='linked'&&activations>=Number(quality.rank??0)) throw new Error('Todos os acertos Vinculados disponíveis já foram ativados.');
        if(quality.key==='aor_overwhelm'&&activations>=Number(quality.rank??0)) throw new Error('Todas as graduações de Sobrecarregar defesas já foram usadas.');
        if(quality.key==='aor_overwhelm'||quality.key==='aor_debilitating'){
            const canonical=quality.key==='aor_overwhelm'?'OVERWHELMDEF':'DEBILSHOT';
            const purchased=talents_mjs_1.sourceBackedTalentRank(requireCharacter(this.state.player.character),canonical);
            if(purchased<1||Number(quality.rank??0)>purchased) throw new Error('Talento AoR não autenticado pela árvore adquirida.');
        }
        const cost=Math.max(0,Number(quality.cost??0));
        if(Number(spend.remainingAdvantage??0)<cost) throw new Error('Vantagens insuficientes para ativar esta qualidade veicular.');
        const target=vehicles.vehicles?.[spend.targetVehicleId];
        if(!target) throw new Error('O alvo do ataque veicular não está mais disponível.');
        let effect={key:quality.key,label:quality.label,rank:Number(quality.rank??0)};
        if(quality.key==='linked'){
            const snap=spend.attackSnapshot??{};
            const hit=Boolean(snap.ion)?vehicles_mjs_1.applyVehicleIonDamage(target,{damage:Number(snap.damage??0),breach:Number(snap.breach??0)}):vehicles_mjs_1.applyVehicleScaleDamage(target,{damage:Number(snap.damage??0),breach:Number(snap.breach??0)});
            vehicles.vehicles[target.id]=hit.vehicle;
            effect={...effect,damage:hit.suffered,damageType:Boolean(snap.ion)?'systemStrain':'hull'};
        } else if(quality.key==='aor_overwhelm'){
            if(Boolean(spend.attackSnapshot?.hit)) throw new Error('Sobrecarregar defesas exige ataque que não acertou.');
            const facing=spend.facing??'proa';
            if(!['proa','babor','estribor','popa'].includes(facing)) throw new Error('Zona de defesa inválida.');
            const current=vehicles_mjs_1.vehicleStatus(target,{currentRound:vehicles.combat.round}).defense[facing];
            if(current<=0) throw new Error('A defesa desta zona já está zerada.');
            target.roundEffects={...(target.roundEffects??{}),aorOverwhelmedDefense:{...(target.roundEffects?.aorOverwhelmedDefense??{}),[facing]:Number(target.roundEffects?.aorOverwhelmedDefense?.[facing]??0)+1}};
            vehicles.vehicles[target.id]=target;
            effect={...effect,zone:facing,defenseAfter:vehicles_mjs_1.vehicleStatus(target,{currentRound:vehicles.combat.round}).defense[facing]};
        } else if(quality.key==='aor_debilitating'){
            if(!Boolean(spend.attackSnapshot?.hit)) throw new Error('Disparo debilitante exige acerto.');
            target.roundEffects={...(target.roundEffects??{}),aorDebilitatedSpeed:{untilRound:Number(vehicles.combat.round)+1}};
            const effectiveMax=vehicles_mjs_1.vehicleStatus(target,{currentRound:vehicles.combat.round}).effectiveSpeedMax;
            target.speed=Math.min(target.speed,effectiveMax);
            vehicles.vehicles[target.id]=target;
            effect={...effect,effectiveSpeedMax:effectiveMax,untilRound:Number(vehicles.combat.round)+1};
        } else if(quality.key==='guided'){
            const projectile={id:ids_mjs_1.newId('guided'),targetVehicleId:target.id,weapon:clone(spend.weapon),guidedRating:Number(quality.rank??0),facing:spend.facing??'proa',resolveAtRound:Number(vehicles.combat.round),sourceAttackId:spend.attackId};
            vehicles.combat.guidedProjectiles=[...(vehicles.combat.guidedProjectiles??[]),projectile];
            effect={...effect,scheduled:true,projectileId:projectile.id,resolveAtRound:projectile.resolveAtRound};
        } else throw new Error('Qualidade veicular ainda não suportada pelo runtime.');
        spend.remainingAdvantage-=cost;
        spend.activations={...(spend.activations??{}),[quality.key]:activations+1};
        vehicles.combat.pendingVehicleSpend=spend;
        vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'QUALIDADE_VEICULAR_ATIVADA',round:vehicles.combat.round,targetId:target.id,quality:quality.key,cost,effect:clone(effect)}];
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {effect,target:clone(vehicles.vehicles[target.id]),remainingAdvantage:spend.remainingAdvantage,remainingTriumph:spend.remainingTriumph,activations:spend.activations[quality.key]};
        });
    }
    reactivateGuidedProjectile(projectileId) {
        const vehicles=clone(this.state.vehicles),list=vehicles.combat?.guidedReactivations??[];
        const idx=list.findIndex(x=>String(x.id)===String(projectileId));
        if(idx<0) throw new Error('Não há projétil Guiado elegível para nova ativação.');
        const pending=list[idx];
        if(Number(pending.remainingAdvantage??0)<3) throw new Error('São necessárias 3 Vantagens para reativar Guiada.');
        const projectile={...clone(pending.projectile),id:ids_mjs_1.newId('guided'),resolveAtRound:Number(vehicles.combat.round)};
        vehicles.combat.guidedProjectiles=[...(vehicles.combat.guidedProjectiles??[]),projectile];
        list.splice(idx,1); vehicles.combat.guidedReactivations=list;
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {scheduled:true,projectile:clone(projectile)};
    }
    applyVehicleDamage({ damage = 0, breach = 0, target = 'active' } = {}) {
        if(this.state.vehicles?.combat?.active&&this.state.vehicles.combat.publishedVehicleContext?.id===THWHEEK_CONTEXT)
            throw Error('SWE02: dano do duelo espacial somente por rolagem veicular real.');
        const vehicles = clone(this.state.vehicles);
        const id = target === 'target' ? vehicles.combat.targetVehicleId : vehicles.activeVehicleId;
        const vehicle = vehicles.vehicles?.[id];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const result = vehicles_mjs_1.applyVehicleScaleDamage(vehicle, { damage, breach });
        vehicles.vehicles[id] = result.vehicle;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return result;
    }
    applyVehicleStrain(amount) {
        const vehicles = clone(this.state.vehicles);
        const id = vehicles.activeVehicleId;
        const vehicle = vehicles.vehicles?.[id];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const result = vehicles_mjs_1.applyVehicleSystemStrain(vehicle, amount);
        vehicles.vehicles[id] = result.vehicle;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return result;
    }
    vehicleDamageControl({ boost = 0, setback = 0, rng = Math.random, mode='system', gmAllowed=false } = {}) {
        const character = requireCharacter(this.state.player.character);
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle) throw new Error('Nave/veículo não encontrado.');
        if (!['system','hull'].includes(mode)) throw new Error('Modo de Controle de Danos inválido.');
        if (vehicles.combat.active && vehicles.combat.actionUsed) throw new Error('A ação deste assalto já foi usada.');
        if(mode==='hull'){
            if(!gmAllowed) throw new Error('Reparo de Trauma de Casco exige confirmação de que o contexto permite o reparo durante o encontro.');
            const attempts=vehicles.combat.hullDamageControlAttempts??{};
            if(vehicles.combat.active&&attempts[vehicle.id]) throw new Error('Só é permitido tentar reduzir Trauma de Casco uma vez por encontro.');
            if(vehicles.combat.active) vehicles.combat.hullDamageControlAttempts={...attempts,[vehicle.id]:true};
        }
        const diff = vehicles_mjs_1.damageControlDifficulty(vehicle,{mode});
        const positive = narrativeDice_mjs_1.buildPositivePool(character.characteristics.intelecto, character.skills.mecanica ?? 0);
        const talentModifiers = talents_mjs_1.talentSkillPoolModifiers(character, 'mecanica', { boost, setback });
        const check = { ...narrativeDice_mjs_1.rollPool({ ...positive, difficulty: diff, challenge: 0, boost: talentModifiers.boost, setback: talentModifiers.setback }, rng), talentModifiers };
        let repaired = { vehicle, mode, systemStrainRecovered: 0, hullRecovered: 0 };
        if (check.netSuccess > 0) repaired = vehicles_mjs_1.applyDamageControl(vehicle, { successes: check.netSuccess, operator: character, mode });
        vehicles.vehicles[vehicle.id] = repaired.vehicle;
        if (vehicles.combat.active) vehicles.combat.actionUsed = true;
        vehicles.combat.log=[...(vehicles.combat.log??[]),{type:'CONTROLE_DANOS',round:vehicles.combat.round,vehicleId:vehicle.id,mode,success:Boolean(check.success),difficulty:diff,recovered:mode==='system'?repaired.systemStrainRecovered:repaired.hullRecovered}];
        this.commit('VEHICLES_UPDATED', { vehicles }); this.commit('CHECK_RESOLVED',check);
        return { check, difficulty: diff, ...repaired };
    }
    applyActiveVehicleCritical({ roll = null, rng = Math.random, modifier = 0, component = null, defenseZone = 'proa', inAtmosphere = false } = {}) {
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const result = vehicles_mjs_1.applyVehicleCritical(vehicle, { roll, rng, modifier, component, defenseZone, inAtmosphere, currentRound: vehicles.combat.round });
        vehicles.vehicles[vehicle.id] = result.vehicle;
        let character = this.state.player.character;
        if (character && Number(result.hit?.crewStrain ?? 0) > 0)
            character = health_mjs_1.sufferStrain(character, Number(result.hit.crewStrain)).character;
        if (character && Number(result.hit?.crewDisorientedRounds ?? 0) > 0)
            character = statusEffects_mjs_1.applyStatusEffect(character, { type: 'disoriented', duration: 'rounds', rounds: Number(result.hit.crewDisorientedRounds), sourceId: `vehicle-critical:${result.hit.number}` });
        this.commit('VEHICLES_UPDATED', { vehicles });
        if (character && character !== this.state.player.character)
            this.commit('CHARACTER_UPDATED', { character });
        return { ...result, character };
    }
    repairActiveVehicleCritical(index, { encounterEnded = !this.state.vehicles?.combat?.active } = {}) {
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const repaired = vehicles_mjs_1.repairVehicleCritical(vehicle, index, { encounterEnded });
        vehicles.vehicles[vehicle.id] = repaired;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return { vehicle: repaired, status: vehicles_mjs_1.vehicleStatus(repaired, { currentRound: vehicles.combat.round }) };
    }
    attemptRepairActiveVehicleCritical(index, { boost = 0, setback = 0, rng = Math.random, encounterEnded = !this.state.vehicles?.combat?.active } = {}) {
        const character = requireCharacter(this.state.player.character);
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const i = Number(index);
        if (!Number.isInteger(i) || i < 0 || i >= (vehicle.criticalHits?.length ?? 0))
            throw new Error('Critical Hit inexistente.');
        const hit = vehicle.criticalHits[i];
        if (hit?.repaired)
            return { repaired: true, alreadyRepaired: true, vehicle, hit, difficulty: hit.repairDifficulty, check: null, status: vehicles_mjs_1.vehicleStatus(vehicle, { currentRound: vehicles.combat.round }) };
        if (hit?.destroyed || hit?.key === 'vaporized')
            throw new Error('Uma nave vaporizada/destruída não pode ser reparada.');
        if (hit?.repairAfterEncounter && !encounterEnded)
            throw new Error('Este Critical Hit só pode ser reparado após o fim do encontro.');
        const difficulty = Number(hit?.repairDifficulty);
        if (!Number.isInteger(difficulty) || difficulty < 1)
            throw new Error('Este Critical Hit não possui dificuldade de reparo válida.');
        if (vehicles.combat?.active && vehicles.combat.actionUsed)
            throw new Error('A ação deste assalto já foi usada.');
        const positive = narrativeDice_mjs_1.buildPositivePool(character.characteristics.intelecto, character.skills.mecanica ?? 0);
        const talentModifiers = talents_mjs_1.talentSkillPoolModifiers(character, 'mecanica', { boost, setback });
        const check = { ...narrativeDice_mjs_1.rollPool({ ...positive, difficulty, challenge: 0, boost: talentModifiers.boost, setback: talentModifiers.setback }, rng), talentModifiers };
        let repairedVehicle = vehicle;
        const repaired = check.netSuccess > 0;
        if (repaired)
            repairedVehicle = vehicles_mjs_1.repairVehicleCritical(vehicle, i, { encounterEnded });
        vehicles.vehicles[vehicle.id] = repairedVehicle;
        if (vehicles.combat?.active)
            vehicles.combat.actionUsed = true;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return { repaired, vehicle: repairedVehicle, hit: repairedVehicle.criticalHits?.[i] ?? hit, difficulty, check, status: vehicles_mjs_1.vehicleStatus(repairedVehicle, { currentRound: vehicles.combat.round }) };
    }
    restVehicleFullDay({ sufferedNewStrain = false } = {}) {
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const result = vehicles_mjs_1.recoverVehicleSystemsAfterFullDay(vehicle, { sufferedNewStrain });
        vehicles.vehicles[vehicle.id] = result.vehicle;
        this.commit('VEHICLES_UPDATED', { vehicles });
        return result;
    }
    vehiclePilotingHazardCheck({ boost = 0, setback = 0, rng = Math.random } = {}) {
        const character = requireCharacter(this.state.player.character);
        const vehicles=clone(this.state.vehicles); let vehicle=vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle) throw new Error('Nave/veículo não encontrado.');
        const copilot=consumeVehicleCopilot(vehicle); vehicle=copilot.vehicle;
        const plotReduction=Math.max(0,Number(vehicle.roundEffects?.plotCourse?.setbackReduction??0)); if(vehicle.roundEffects?.plotCourse) delete vehicle.roundEffects.plotCourse;
        vehicles.vehicles[vehicle.id]=vehicle;
        const { difficulty, upgrades } = vehicles_mjs_1.pilotingHazardDifficulty(vehicle);
        const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,'pilotar_vehiculo_espacial',vehicle.id);
        const totalUpgrades=upgrades+officialUnpreparedUpgrade;
        const pool = vehicles_mjs_1.handlingPoolModifier(vehicle);
        const upgraded = difficultyWithUpgrades(difficulty, 0, totalUpgrades);
        const positive = narrativeDice_mjs_1.buildPositivePool(character.characteristics.agilidad, character.skills.pilotar_vehiculo_espacial ?? 0);
        const externalSetback=Math.max(0,Number(setback)-plotReduction);
        const talentModifiers = talents_mjs_1.talentSkillPoolModifiers(character, 'pilotar_vehiculo_espacial', { boost: Number(boost) + pool.boost, setback: externalSetback + pool.setback });
        const criticalStatus = vehicles_mjs_1.vehicleStatus(vehicle, { currentRound: vehicles.combat?.round ?? 0 });
        const recoveryDifficulty = criticalStatus.noManeuvers && Number(vehicles_mjs_1.vehicleCriticalModifiers(vehicle, { currentRound: this.state.vehicles?.combat?.round ?? 0 }).pilotRecoveryDifficulty ?? 0) > 0
            ? Number(vehicles_mjs_1.vehicleCriticalModifiers(vehicle, { currentRound: this.state.vehicles?.combat?.round ?? 0 }).pilotRecoveryDifficulty)
            : difficulty;
        const recoveryUpgraded = difficultyWithUpgrades(recoveryDifficulty, 0, totalUpgrades);
        const downgraded=difficultyWithDowngrades(recoveryUpgraded.difficulty,recoveryUpgraded.challenge,copilot.downgrades);
        const check = { ...narrativeDice_mjs_1.rollPool({ ...positive, ...downgraded, boost: talentModifiers.boost, setback: talentModifiers.setback }, rng), talentModifiers };
        this.commit('CHECK_RESOLVED', check);
        if (check.success && criticalStatus.noManeuvers) vehicles.vehicles[vehicle.id]=vehicles_mjs_1.clearKnockedOffCourse(vehicles.vehicles[vehicle.id]);
        this.commit('VEHICLES_UPDATED',{vehicles});
        return { check, difficulty: downgraded.difficulty, baseDifficulty:recoveryDifficulty, upgrades:totalUpgrades, officialUnpreparedUpgrade, difficultyDowngrades:copilot.downgrades, plotCourseReduction:plotReduction, talentModifiers };
    }
    attemptTractorEscape({ boost = 0, setback = 0, rng = Math.random } = {}) {
        const character = requireCharacter(this.state.player.character);
        const vehicles = clone(this.state.vehicles);
        const vehicle = vehicles.vehicles?.[vehicles.activeVehicleId];
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        const copilot=consumeVehicleCopilot(vehicle);
        const officialUnpreparedUpgrade=jewelUnpreparedSpaceUpgrade(this.state.world,vehicles,'pilotar_vehiculo_espacial',vehicle.id);
        const result = vehicles_mjs_1.tractorEscapeCheck(copilot.vehicle, character, { rng, boost, setback, difficultyDowngrades:copilot.downgrades,difficultyUpgrades:officialUnpreparedUpgrade });
        if (!result.skipped) {
            vehicles.vehicles[vehicle.id] = result.vehicle;
            this.commit('VEHICLES_UPDATED', { vehicles });
            this.commit('CHECK_RESOLVED', result.check);
        }
        return result;
    }
    plotHyperspaceCourse({ difficulty = 1, boost = 0, setback = 0, useExpertTalent = false, rng = Math.random } = {}) {
        return this.atomicAction(() => {
        const character = requireCharacter(this.state.player.character);
        const vehicle = this.activeVehicle();
        if (!vehicle)
            throw new Error('Nave/veículo não encontrado.');
        if (!vehicle.hyperdrive)
            throw new Error('Esta nave/veículo não possui hiperimpulsor.');
        const criticalStatus = vehicles_mjs_1.vehicleStatus(vehicle, { currentRound: this.state.vehicles?.combat?.round ?? 0 });
        if (criticalStatus.hyperspaceDisabled)
            throw new Error('Critical Hit: hiperimpulsor/computador de navegação inoperante até reparo.');
        if (!vehicle.navComputer && !vehicle.astromechPort)
            throw new Error('A nave não possui computador de navegação nem suporte astromecânico para calcular a rota.');
        if(!Number.isSafeInteger(difficulty)||difficulty<1||difficulty>5 ||
           !Number.isSafeInteger(boost)||boost<0||boost>20 || !Number.isSafeInteger(setback)||setback<0||setback>20)
            throw new Error('Parâmetros de navegação inválidos.');
        const expert=talents_mjs_1.expertSkillTalentSpec(character,'astronavegacion');
        if(useExpertTalent&&!expert.available) throw new Error('Astronavegante especialista não adquirido.');
        const navigator=useExpertTalent?health_mjs_1.sufferStrain(character,expert.strainCost,{voluntary:true}).character:character;
        const effectiveDifficulty=useExpertTalent?Math.max(1,difficulty-expert.difficultyReduction):difficulty;
        const positive = narrativeDice_mjs_1.buildPositivePool(navigator.characteristics.intelecto, navigator.skills.astronavegacion ?? 0);
        const talentModifiers = talents_mjs_1.talentSkillPoolModifiers(character, 'astronavegacion', { boost, setback });
        const check = { ...narrativeDice_mjs_1.rollPool({ ...positive, difficulty: effectiveDifficulty, challenge: 0, boost: talentModifiers.boost, setback: talentModifiers.setback }, rng), talentModifiers, expertTalent:useExpertTalent?{...expert,used:true}:null };
        if(useExpertTalent) this.commit('CHARACTER_UPDATED',{character:navigator});
        this.commit('CHECK_RESOLVED', check);
        const timeMultiplier = talents_mjs_1.galacticCartographerTimeMultiplier(character);
        return { check, difficulty: effectiveDifficulty, vehicleId: vehicle.id, talentModifiers, timeMultiplier };
        });
    }
    adversaryProfiles() {
        return adversaries_mjs_1.ADVERSARY_PROFILES;
    }
    adHocNpc(payload = {}) { return adversaries_mjs_1.createAdHocRival(payload); }
    allyNpcControlModes() { return structuredClone(adversaries_mjs_1.ALLY_NPC_CONTROL_MODES); }
}
