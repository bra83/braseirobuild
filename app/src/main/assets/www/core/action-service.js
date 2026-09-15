import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import { resolveSkillCheck, resolveSkillCheckFromPhysicalDice, validateMinimalCoreCharacter } from './wwn-rules-core.js';
import { WWN_CLASS_SOURCES } from './wwn-class-core.js';
import { SRD_NPC_PROFILES } from './wwn-npc-combat.js';
import { resolveReactionRoll, resolveReactionRollFromPhysicalDice, resolveOpposedSkillCheck, resolveOpposedSkillCheckFromPhysicalDice, resolvePursuitEscape, resolvePursuitEscapeFromPhysicalDice, resolveSafeWithdrawalNoRoll } from './wwn-encounter-core.js';
import { advanceLivingWorldPresentation } from './living-world-core.js';

function parseState(loadResult) {
  if (loadResult.status !== 'OK') throw new Error(`Campaign load failed: ${loadResult.status}`);
  const state = JSON.parse(loadResult.stateJson);
  if (state.revision !== loadResult.revision) throw new Error('State/load revision mismatch');
  return state;
}

function findNarrativeFor(events, mechanicalEventId) {
  return events.find((event) => {
    if (event.eventKind !== 'NARRATIVE') return false;
    try { return JSON.parse(event.payloadJson).parentMechanicalEventId === mechanicalEventId; }
    catch { return false; }
  }) ?? null;
}

export class PlayerActionService {
  constructor({ persistence, sceneRules, barbara }) {
    this.persistence = persistence;
    this.sceneRules = sceneRules;
    this.barbara = barbara;
  }

  perform({ campaignId, screenId, intent, physicalDice = null }) {
    const contract = this.sceneRules.actions?.[intent];
    const implemented = contract && ['WAVE3_IMPLEMENTED','IMPLEMENTED'].includes(contract.status);
    if (!implemented) {
      const loaded = this.persistence.loadCampaign(campaignId);
      return Object.freeze({
        status: contract?.status === 'IMPLEMENTED_BY_COMBAT_SESSION' ? 'ROUTED_TO_COMBAT_SESSION_NO_MUTATION' : 'DEFERRED_NO_MUTATION', intent, screenId,
        revision: loaded.status === 'OK' ? loaded.revision : null,
        targetWave: contract?.targetWave ?? null,
      });
    }
    if (screenId !== this.sceneRules.screenId) throw new Error(`Scene rule mismatch ${screenId}`);

    const loaded = this.persistence.loadCampaign(campaignId);
    const state = parseState(loaded);
    const legality = validateMinimalCoreCharacter(state.characterState);
    if (!legality.ok) throw new Error(`Character fixture invalid: ${legality.errors.join('; ')}`);

    const rng = DeterministicRng.fromState(state.rngState);
    const character = state.characterState;
    const hasPhysical = physicalDice && Array.isArray(physicalDice.rolls);
    const profile = contract.targetProfileId ? SRD_NPC_PROFILES[contract.targetProfileId] : null;
    let resolution;
    if (contract.resolution === 'SKILL_CHECK') {
      const skillLevel = character.skills?.[contract.skill] ?? null;
      const attributeScore = character.attributes?.[contract.attribute];
      resolution = hasPhysical ? resolveSkillCheckFromPhysicalDice({
        faces: physicalDice.rolls, skillLevel, attributeScore, difficulty: contract.difficulty,
        situationalModifier: contract.situationalModifier ?? 0, physicalMeta: physicalDice,
      }) : resolveSkillCheck({
        rng, skillLevel, attributeScore, difficulty: contract.difficulty,
        situationalModifier: contract.situationalModifier ?? 0, streamName: contract.rngStream,
      });
    } else if (contract.resolution === 'REACTION_ROLL') {
      const charismaScore = character.attributes?.[contract.attribute ?? 'Charisma'];
      resolution = hasPhysical ? resolveReactionRollFromPhysicalDice({physicalDice,charismaScore}) : resolveReactionRoll({rng,charismaScore,streamName:contract.rngStream ?? 'SCR-015/approach/reaction'});
    } else if (contract.resolution === 'OPPOSED_SKILL_CHECK') {
      if (!profile || !Number.isInteger(profile.skillBonus)) return Object.freeze({status:'MISSING_EVIDENCE_TARGET_SKILL_BONUS_NO_MUTATION',revision:loaded.revision});
      const actorSkillLevel = character.skills?.[contract.skill] ?? null;
      const actorAttributeScore = character.attributes?.[contract.attribute];
      const args={actorSkillLevel,actorAttributeScore,defenderSkillBonus:profile.skillBonus,actorModifier:contract.actorModifier??0};
      resolution = hasPhysical ? resolveOpposedSkillCheckFromPhysicalDice({physicalDice,...args}) : resolveOpposedSkillCheck({rng,...args,streamName:contract.rngStream ?? `SCR-015/${intent}/opposed`});
    } else if (contract.resolution === 'SAFE_WITHDRAWAL') {
      const foesAware = state.encounterState?.foesAware ?? contract.initialFoesAware ?? false;
      if (!foesAware) {
        resolution = resolveSafeWithdrawalNoRoll({reason:'FOES_UNAWARE'});
      } else {
        if (!profile || !Number.isInteger(profile.skillBonus)) return Object.freeze({status:'MISSING_EVIDENCE_PURSUER_SKILL_BONUS_NO_MUTATION',revision:loaded.revision});
        const actorSkillLevel = character.skills?.[contract.skill] ?? null;
        const actorAttributeScore = character.attributes?.[contract.attribute];
        const actorModifier = Number.isInteger(contract.actorModifier) ? contract.actorModifier : 0;
        const args={actorSkillLevel,actorAttributeScore,defenderSkillBonus:profile.skillBonus,actorModifier};
        resolution = hasPhysical ? resolvePursuitEscapeFromPhysicalDice({physicalDice,...args}) : resolvePursuitEscape({rng,...args,streamName:contract.rngStream ?? 'SCR-015/retreat/escape'});
      }
    } else {
      throw new Error(`Unsupported action resolution: ${contract.resolution}`);
    }

    const newRevision = loaded.revision + 1;
    const eventId = `mech:${campaignId}:r${newRevision}:${screenId}:${intent}`;
    const newState = structuredClone(state);
    newState.revision = newRevision;
    newState.rngState = hasPhysical || resolution.rngAlgorithm === 'NO_ROLL_REQUIRED' ? structuredClone(state.rngState) : rng.exportState();
    const encounter = {...(newState.encounterState ?? {}),screenId};
    if (encounter.foesAware === undefined) encounter.foesAware = contract.initialFoesAware ?? false;
    const dice = Array.isArray(resolution.dice) ? resolution.dice.map((die)=>typeof die==='number'?die:die.result) : [];
    if (intent === 'observe') {
      encounter.contactState = resolution.success ? 'CONTATO OBSERVADO' : 'CONTATO INESPERADO';
      encounter.observationCount = (encounter.observationCount ?? 0) + 1;
    } else if (intent === 'approach') {
      encounter.foesAware = true;
      encounter.contactState = resolution.disposition?.label ?? 'REAÇÃO DEFINIDA';
      encounter.reaction = {total:resolution.total,disposition:resolution.disposition};
      encounter.ambushPrepared = false;
    } else if (intent === 'ambush') {
      encounter.ambushPrepared = resolution.success;
      encounter.foesAware = !resolution.success;
      encounter.contactState = resolution.success ? 'EMBOSCADA PREPARADA' : 'GRUPO DESCOBERTO';
    } else if (intent === 'retreat') {
      encounter.scenePhase = resolution.success ? 'WITHDRAWN' : 'ACTIVE';
      encounter.retreated = resolution.success;
      encounter.contactState = resolution.success ? (resolution.rngAlgorithm==='NO_ROLL_REQUIRED'?'RETIRADA SEGURA':'FUGA BEM-SUCEDIDA') : 'FUGA INTERROMPIDA';
      if (resolution.success) encounter.ambushPrepared = false;
    }
    encounter.lastResolution = {
      intent,
      success: resolution.success,
      total: resolution.total ?? null,
      opposedTotal: resolution.opposedTotal ?? null,
      difficulty: resolution.difficulty ?? null,
      dice,
      rule: resolution.rule,
      disposition: resolution.disposition ?? null,
    };
    newState.encounterState = encounter;
    newState.worldState = { ...(newState.worldState ?? {}), livingWorld: advanceLivingWorldPresentation(newState,{trigger:`PLAYER_ACTION:${intent}`,revision:newRevision}) };
    newState.journalState = { ...(newState.journalState ?? {}), lastMechanicalEventId: eventId };

    const kindByIntent={observe:'PLAYER_ACTION_SKILL_CHECK',approach:'PLAYER_ACTION_REACTION_ROLL',ambush:'PLAYER_ACTION_OPPOSED_AMBUSH',retreat:'PLAYER_ACTION_RETREAT'};
    const mechanicalEvent = Object.freeze({
      eventId,
      revision: newRevision,
      kind: kindByIntent[intent] ?? 'PLAYER_ACTION_RESOLUTION',
      screenId,
      intent,
      contractClassification: contract.classification,
      sourceRefs: Object.freeze([...(contract.sourceRefs ?? []),...(resolution.sourceRefs ?? [])]),
      diceAuthority: hasPhysical ? Object.freeze({
        provider: physicalDice.provider,
        algorithm: physicalDice.algorithm,
        formula: physicalDice.formula,
        physicalFaces: Object.freeze([...(physicalDice.physicalFaces ?? physicalDice.rolls ?? [])]),
        seed: physicalDice.seed ?? null,
        physicsMs: physicalDice.physicsMs ?? null,
        forcedSettle: Boolean(physicalDice.forcedSettle),
      }) : Object.freeze({provider:'DETERMINISTIC_INTERNAL',algorithm:state.rngState.algorithm}),
      resolution,
    });
    const receipt = this.persistence.commit({
      campaignId,
      expectedRevision: loaded.revision,
      eventId,
      eventKind: mechanicalEvent.kind,
      eventPayloadJson: canonicalJson(mechanicalEvent),
      newStateJson: canonicalJson(newState),
      rngCountersJson: canonicalJson(newState.rngState.counters),
    });
    if (receipt.status !== 'OK') return Object.freeze({ status: receipt.status, receipt });

    try {
      const narrative = this.barbara.generate({ mechanicalEvent, state: newState });
      if (narrative?.geminiDeferred === true && typeof this.barbara.generateAsync === 'function') {
        return Object.freeze({
          status: 'MECHANICAL_COMMITTED_NARRATIVE_PENDING', receipt, mechanicalEvent, narrative,
          projectedState: newState,
        });
      }
      const narrativeEventId = `narrative:${eventId}`;
      const narrativeReceipt = this.persistence.appendNarrative({
        campaignId,
        eventId: narrativeEventId,
        mechanicalEventId: eventId,
        mechanicalRevision: newRevision,
        eventPayloadJson: canonicalJson(narrative),
      });
      return Object.freeze({
        status: 'OK', receipt, narrativeReceipt, mechanicalEvent, narrative,
        projectedState: newState,
      });
    } catch (error) {
      return Object.freeze({
        status: 'MECHANICAL_COMMITTED_NARRATIVE_PENDING', receipt, mechanicalEvent,
        projectedState: newState, narrativeError: String(error?.message ?? error),
      });
    }
  }

  rerollWithMasterfulExpertise({ campaignId, screenId }) {
    const loaded = this.persistence.loadCampaign(campaignId);
    const state = parseState(loaded);
    const character = state.characterState ?? {};
    const classId = String(character.classId ?? character.class ?? '').toUpperCase().replace(/\s+/g, '_');
    const abilities = new Set((character.classAbilities ?? []).map((value) => String(value).toUpperCase()));
    if (classId !== 'FULL_EXPERT' || !abilities.has('MASTERFUL_EXPERTISE')) {
      return Object.freeze({ status: 'MASTERFUL_EXPERTISE_NOT_AVAILABLE_NO_MUTATION', revision: loaded.revision });
    }
    const sceneId = state.encounterState?.sceneId;
    if (!sceneId) return Object.freeze({ status: 'MISSING_SCENE_ID_NO_MUTATION', revision: loaded.revision });
    const priorUsage = state.classAbilityUsage?.MASTERFUL_EXPERTISE;
    if (priorUsage?.sceneId === sceneId) {
      return Object.freeze({ status: 'MASTERFUL_EXPERTISE_ALREADY_USED_THIS_SCENE_NO_MUTATION', revision: loaded.revision, sceneId });
    }
    const lastId = state.journalState?.lastMechanicalEventId;
    const previousRecord = lastId && typeof this.persistence.getEvent === 'function' ? this.persistence.getEvent(lastId)?.event : null;
    if (!previousRecord) return Object.freeze({ status: 'NO_PREVIOUS_SKILL_CHECK_NO_MUTATION', revision: loaded.revision, sceneId });
    let previousEvent;
    try { previousEvent = JSON.parse(previousRecord.payloadJson); } catch { return Object.freeze({ status: 'PREVIOUS_SKILL_CHECK_CORRUPT_NO_MUTATION', revision: loaded.revision, sceneId }); }
    if (previousEvent.kind !== 'PLAYER_ACTION_SKILL_CHECK' || previousEvent.screenId !== screenId || !previousEvent.resolution) {
      return Object.freeze({ status: 'PREVIOUS_EVENT_NOT_ELIGIBLE_SKILL_CHECK_NO_MUTATION', revision: loaded.revision, sceneId });
    }
    const prior = previousEvent.resolution;
    const rng = DeterministicRng.fromState(state.rngState);
    const reroll = resolveSkillCheck({
      rng,
      skillLevel: prior.skillLevel,
      attributeScore: prior.attributeScore,
      difficulty: prior.difficulty,
      situationalModifier: prior.situationalModifier ?? 0,
      streamName: `${previousEvent.intent ?? 'skill'}/masterful-expertise/${sceneId}`,
    });
    const useReroll = reroll.total > prior.total;
    const chosen = useReroll ? reroll : prior;
    const resolution = Object.freeze({
      ...chosen,
      rule: 'WWN_MASTERFUL_EXPERTISE_REROLL_V1',
      sourceRefs: Object.freeze([WWN_CLASS_SOURCES.expert, ...(chosen.sourceRefs ?? [])]),
      masterfulExpertise: Object.freeze({ sceneId, previousTotal: prior.total, rerollTotal: reroll.total, usedReroll: useReroll }),
    });
    const newRevision = loaded.revision + 1;
    const eventId = `mech:${campaignId}:r${newRevision}:${screenId}:masterful-expertise`;
    const newState = structuredClone(state);
    newState.revision = newRevision;
    newState.rngState = rng.exportState();
    newState.classAbilityUsage = { ...(newState.classAbilityUsage ?? {}), MASTERFUL_EXPERTISE: { sceneId, revision: newRevision } };
    newState.encounterState = {
      ...(newState.encounterState ?? {}),
      contactState: resolution.success ? 'CONTATO OBSERVADO' : 'CONTATO INESPERADO',
      lastResolution: {
        ...(newState.encounterState?.lastResolution ?? {}),
        intent: previousEvent.intent,
        success: resolution.success,
        total: resolution.total,
        difficulty: resolution.difficulty,
        dice: resolution.dice.map((die) => die.result),
        classAbility: 'MASTERFUL_EXPERTISE',
        previousTotal: prior.total,
        rerollTotal: reroll.total,
      },
    };
    newState.journalState = { ...(newState.journalState ?? {}), lastMechanicalEventId: eventId };
    const mechanicalEvent = Object.freeze({
      eventId, revision: newRevision, kind: 'PLAYER_ACTION_CLASS_ABILITY_REROLL', screenId, intent: previousEvent.intent,
      sourceRefs: [WWN_CLASS_SOURCES.expert], resolution, parentMechanicalEventId: previousEvent.eventId, sceneId,
    });
    const receipt = this.persistence.commit({
      campaignId, expectedRevision: loaded.revision, eventId, eventKind: mechanicalEvent.kind,
      eventPayloadJson: canonicalJson(mechanicalEvent), newStateJson: canonicalJson(newState), rngCountersJson: canonicalJson(newState.rngState.counters),
    });
    if (receipt.status !== 'OK') return Object.freeze({ status: receipt.status, receipt });
    try {
      const narrative = this.barbara.generate({ mechanicalEvent, state: newState });
      if (narrative?.geminiDeferred === true && typeof this.barbara.generateAsync === 'function') {
        return Object.freeze({ status: 'MECHANICAL_COMMITTED_NARRATIVE_PENDING', receipt, mechanicalEvent, narrative, projectedState: newState });
      }
      const narrativeReceipt = this.persistence.appendNarrative({
        campaignId, eventId: `narrative:${eventId}`, mechanicalEventId: eventId, mechanicalRevision: newRevision, eventPayloadJson: canonicalJson(narrative),
      });
      return Object.freeze({ status: 'OK', receipt, narrativeReceipt, mechanicalEvent, narrative, projectedState: newState });
    } catch (error) {
      return Object.freeze({ status: 'MECHANICAL_COMMITTED_NARRATIVE_PENDING', receipt, mechanicalEvent, projectedState: newState, narrativeError: String(error?.message ?? error) });
    }
  }

  async recoverPendingNarrativeAsync({ campaignId }) {
    const loaded = this.persistence.loadCampaign(campaignId);
    const state = parseState(loaded);
    const mechanicalEventId = state.journalState?.lastMechanicalEventId;
    if (!mechanicalEventId) return Object.freeze({ status: 'NO_PENDING_MECHANICAL_EVENT', revision: loaded.revision });
    const existingRecord = typeof this.persistence.latestNarrative === 'function' ? this.persistence.latestNarrative(campaignId, mechanicalEventId)?.event : null;
    let existing = null; try { existing = existingRecord?.payloadJson ? JSON.parse(existingRecord.payloadJson) : null; } catch {}
    if (existing) return Object.freeze({ status: 'ALREADY_NARRATED', revision: loaded.revision, narrativeEvent: existing });
    const mechanicalRecord = typeof this.persistence.getEvent === 'function' ? this.persistence.getEvent(mechanicalEventId)?.event : null;
    if (!mechanicalRecord) throw new Error('Mechanical event missing during narrative recovery');
    const mechanicalEvent = JSON.parse(mechanicalRecord.payloadJson);
    const narrative = this.barbara.generateAsync
      ? await this.barbara.generateAsync({ mechanicalEvent, state })
      : await Promise.resolve(this.barbara.generate({ mechanicalEvent, state }));
    const narrativeReceipt = this.persistence.appendNarrative({
      campaignId,
      eventId: `narrative:${mechanicalEventId}`,
      mechanicalEventId,
      mechanicalRevision: mechanicalEvent.revision,
      eventPayloadJson: canonicalJson(narrative),
    });
    return Object.freeze({ status: 'RECOVERED', revision: loaded.revision, narrative, narrativeReceipt, mechanicalEvent, projectedState: state });
  }

  recoverPendingNarrative({ campaignId }) {
    const loaded = this.persistence.loadCampaign(campaignId);
    const state = parseState(loaded);
    const mechanicalEventId = state.journalState?.lastMechanicalEventId;
    if (!mechanicalEventId) return Object.freeze({ status: 'NO_PENDING_MECHANICAL_EVENT', revision: loaded.revision });
    const existingRecord = typeof this.persistence.latestNarrative === 'function' ? this.persistence.latestNarrative(campaignId, mechanicalEventId)?.event : null;
    let existing = null; try { existing = existingRecord?.payloadJson ? JSON.parse(existingRecord.payloadJson) : null; } catch {}
    if (existing) return Object.freeze({ status: 'ALREADY_NARRATED', revision: loaded.revision, narrativeEvent: existing });
    const mechanicalRecord = typeof this.persistence.getEvent === 'function' ? this.persistence.getEvent(mechanicalEventId)?.event : null;
    if (!mechanicalRecord) throw new Error('Mechanical event missing during narrative recovery');
    const mechanicalEvent = JSON.parse(mechanicalRecord.payloadJson);
    const narrative = this.barbara.generate({ mechanicalEvent, state });
    const narrativeReceipt = this.persistence.appendNarrative({
      campaignId,
      eventId: `narrative:${mechanicalEventId}`,
      mechanicalEventId,
      mechanicalRevision: mechanicalEvent.revision,
      eventPayloadJson: canonicalJson(narrative),
    });
    return Object.freeze({ status: 'RECOVERED', revision: loaded.revision, narrative, narrativeReceipt });
  }
}
