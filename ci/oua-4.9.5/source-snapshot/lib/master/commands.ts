import type { CampaignState, Character, CharacterSheet, Npc } from '../db';
import { adjustEndurance, adjustFatigue, adjustHope, adjustShadow, recomputeConditions, spendHope } from '../rules/hopeShadow';
import { completeJourney, recordJourneyMove, startJourney } from '../rules/journey';
import { recordCouncilResult, resolveCouncil, startCouncil } from '../rules/council';
import { endCombat, moveCombatActor, nextRound, recordCombatNarrative, recordCombatRoll, setCombatBackground, setStance, startCombat, upsertCombatActor } from '../rules/combat';
import { endFellowshipPhase, spendFellowship, startFellowshipPhase } from '../rules/fellowship';
import { normalizeMasterState } from './state';
import { assertModeTransitionAllowed, assertSceneTransitionAllowed, sceneCompatibleWithMode } from './sceneMachine';
import { advanceWorldClock } from './worldClock';
import { advanceOrganizationProject, normalizeOrganization, normalizeStronghold } from '../organizations';
import { simulateLivingWorldDetailed } from '../livingWorldAutomation';
import type { CommandAuditEntry, MasterCommand, MasterState, NpcDraft, SceneKind } from './types';

export interface CommandApplicationResult {
  campaign: CampaignState;
  characters: Character[];
  npcs: Npc[];
  audits: CommandAuditEntry[];
  rejected: Array<{ command: MasterCommand; reason: string }>;
}

export function applyMasterCommands(
  campaign: CampaignState,
  characters: Character[],
  npcs: Npc[],
  commands: MasterCommand[],
): CommandApplicationResult {
  let nextCampaign = structuredClone(campaign);
  let nextCharacters = structuredClone(characters);
  let nextNpcs = structuredClone(npcs);
  let masterState = normalizeMasterState(nextCampaign);
  const audits: CommandAuditEntry[] = [];
  const rejected: Array<{ command: MasterCommand; reason: string }> = [];
  const partyIds = new Set(nextCampaign.partyCharacterIds || []);

  for (const command of commands.slice(0, 50)) {
    const id = `cmd_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    try {
      const before = commandTargetSnapshot(command, nextCampaign, nextCharacters, nextNpcs, masterState);
      ({ campaign: nextCampaign, characters: nextCharacters, npcs: nextNpcs, masterState } = applyOne(command, nextCampaign, nextCharacters, nextNpcs, masterState, partyIds));
      const after = commandTargetSnapshot(command, nextCampaign, nextCharacters, nextNpcs, masterState);
      audits.push({ id, campaignId: campaign.id, timestamp: Date.now(), command, status: 'applied', reason: command.reason, before, after });
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      rejected.push({ command, reason });
      audits.push({ id, campaignId: campaign.id, timestamp: Date.now(), command, status: 'rejected', reason });
    }
  }

  nextCampaign.masterState = { ...masterState, updatedAt: Date.now() };
  nextCampaign.currentLocation = masterState.position.locationName || nextCampaign.currentLocation;
  const validNpcIds = new Set(nextNpcs.map(npc => npc.id));
  nextCampaign.presentNpcIds = Array.from(new Set((nextCampaign.presentNpcIds || []).filter(id => validNpcIds.has(id))));
  nextNpcs = nextNpcs.map(npc => ({ ...npc, presentInScene: nextCampaign.presentNpcIds.includes(npc.id) }));
  nextCampaign.updatedAt = Date.now();
  return { campaign: nextCampaign, characters: nextCharacters, npcs: nextNpcs, audits, rejected };
}

function applyOne(
  command: MasterCommand,
  campaign: CampaignState,
  characters: Character[],
  npcs: Npc[],
  state: MasterState,
  partyIds: Set<string>,
): { campaign: CampaignState; characters: Character[]; npcs: Npc[]; masterState: MasterState } {
  const nextCampaign = structuredClone(campaign);
  let nextCharacters = structuredClone(characters);
  let nextNpcs = structuredClone(npcs);
  let nextState = structuredClone(state);

  const appendCampaignEvent = (type: 'combat_started' | 'combat_ended' | 'custom', data: Record<string, string | number | boolean | null>) => {
    const event = { id: `event_${type}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, type, timestamp: Date.now(), sceneId: nextState.scene.id || undefined, data };
    nextCampaign.events = [...(nextCampaign.events || []), event].slice(-1000);
  };

  const appendLivingActivity = (kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'world', title: string, detail: string, sourceId?: string) => {
    nextState.livingWorld = {
      ...nextState.livingWorld,
      activityLog: [...(nextState.livingWorld.activityLog || []), { id: `activity_${kind}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, kind, title, detail, day: nextState.worldClock.day, createdAt: Date.now(), sourceId }].slice(-300),
      updatedAt: Date.now(),
    };
  };

  const updateHero = (characterId: string, updater: (sheet: CharacterSheet) => CharacterSheet) => {
    if (!partyIds.has(characterId)) throw new Error('Comando rejeitado: o alvo não pertence à Companhia em jogo.');
    let found = false;
    nextCharacters = nextCharacters.map(character => {
      if (character.id !== characterId) return character;
      found = true;
      const sheet = updater(character.sheet);
      return { ...character, name: sheet.identity.name || character.name, culture: sheet.identity.culture || character.culture, vocation: sheet.identity.vocation || character.vocation, sheet, updatedAt: Date.now() };
    });
    if (!found) throw new Error('Comando rejeitado: personagem não encontrado.');
    const updatedHero = nextCharacters.find(character => character.id === characterId);
    if (updatedHero && nextState.combat.actors.some(actor => actor.actorId === characterId)) {
      const attributes = updatedHero.sheet.attributes;
      nextState.combat = {
        ...nextState.combat,
        actors: nextState.combat.actors.map(actor => actor.actorId === characterId ? {
          ...actor,
          endurance: attributes.enduranceCurrent,
          enduranceMax: attributes.enduranceMax,
          wounded: attributes.wounded,
          defeated: attributes.enduranceCurrent <= 0,
          hope: attributes.hopeCurrent,
          shadow: attributes.shadow,
          fatigue: attributes.fatigue,
          stateNotes: [attributes.weary ? 'Cansado' : '', attributes.miserable ? 'Arrasado' : '', attributes.wounded ? 'Ferido' : ''].filter(Boolean),
        } : actor),
        updatedAt: Date.now(),
      };
    }
  };

  switch (command.type) {
    case 'adjust_endurance': updateHero(command.targetCharacterId, sheet => adjustEndurance(sheet, safeDelta(command.amount))); break;
    case 'adjust_hope': updateHero(command.targetCharacterId, sheet => adjustHope(sheet, safeDelta(command.amount))); break;
    case 'spend_hope':
      updateHero(command.targetCharacterId, sheet => {
        const result = spendHope(sheet, command.attribute);
        if (!result.allowed) throw new Error('Comando rejeitado: o Herói não possui Esperança para gastar.');
        return result.sheet;
      });
      break;
    case 'spend_fellowship_for_hope': {
      const amount = Math.max(1, Math.min(5, Math.trunc(Number(command.amount) || 1)));
      const spent = spendFellowship(nextState, amount);
      if (!spent.spent) throw new Error('Comando rejeitado: Sociedade insuficiente.');
      nextState = spent.state;
      updateHero(command.targetCharacterId, sheet => adjustHope(sheet, amount));
      break;
    }
    case 'adjust_shadow': updateHero(command.targetCharacterId, sheet => adjustShadow(sheet, safeDelta(command.amount))); break;
    case 'adjust_fatigue': updateHero(command.targetCharacterId, sheet => adjustFatigue(sheet, safeDelta(command.amount))); break;
    case 'adjust_supplies':
      updateHero(command.targetCharacterId, sheet => {
        const next = structuredClone(sheet);
        next.attributes.supplies = Math.max(0, Math.min(100, next.attributes.supplies + safeDelta(command.amount)));
        return recomputeConditions(next);
      });
      break;
    case 'set_condition':
      updateHero(command.targetCharacterId, sheet => {
        const next = structuredClone(sheet);
        next.attributes[command.condition] = Boolean(command.value);
        return next;
      });
      break;
    case 'move_party':
      if (!command.toHexId) throw new Error('Comando rejeitado: hex de destino ausente.');
      nextState = recordJourneyMove(nextState, command.toHexId, command.locationName || nextState.position.locationName, command.distanceKm, command.terrain);
      nextCampaign.currentLocation = nextState.position.locationName;
      nextState.scene = { ...nextState.scene, locationName: nextState.position.locationName, updatedAt: Date.now() };
      break;
    case 'set_present_npcs': {
      const valid = new Set(nextNpcs.map(npc => npc.id));
      nextCampaign.presentNpcIds = Array.from(new Set(command.npcIds.filter(id => valid.has(id))));
      nextNpcs = nextNpcs.map(npc => ({ ...npc, presentInScene: nextCampaign.presentNpcIds.includes(npc.id), updatedAt: Date.now() }));
      nextState.scene = { ...nextState.scene, participantNpcIds: nextCampaign.presentNpcIds, updatedAt: Date.now() };
      break;
    }
    case 'upsert_npc': {
      const draft = command.npc;
      if (!draft?.name?.trim()) throw new Error('Comando rejeitado: NPC sem nome.');
      const id = resolveNpcId(draft, nextNpcs);
      const index = nextNpcs.findIndex(npc => npc.id === id);
      const previous = index >= 0 ? nextNpcs[index] : null;
      const value = normalizeNpcDraft(draft, previous, nextState.position.locationName, id);
      if (index >= 0) nextNpcs[index] = value; else nextNpcs.push(value);
      if (value.presentInScene) {
        nextCampaign.presentNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), id]));
        nextState.scene = { ...nextState.scene, participantNpcIds: Array.from(new Set([...nextState.scene.participantNpcIds, id])), updatedAt: Date.now() };
      }
      const actorIndex = nextState.npcActors.findIndex(actor => actor.npcId === id);
      const actor = {
        npcId: id,
        emotionalState: value.emotionalState || value.state || 'neutro',
        nextAction: value.nextAction || '',
        location: value.location,
        faction: value.faction,
        beliefs: actorIndex >= 0 ? nextState.npcActors[actorIndex].beliefs : [],
        suspicions: actorIndex >= 0 ? nextState.npcActors[actorIndex].suspicions : [],
        allies: actorIndex >= 0 ? nextState.npcActors[actorIndex].allies : [],
        enemies: actorIndex >= 0 ? nextState.npcActors[actorIndex].enemies : [],
        relationshipByHero: value.relationshipByHero || (actorIndex >= 0 ? nextState.npcActors[actorIndex].relationshipByHero : {}),
        lastUpdatedAt: Date.now(),
      };
      if (actorIndex >= 0) nextState.npcActors[actorIndex] = actor; else nextState.npcActors.push(actor);
      break;
    }
    case 'update_npc_actor': {
      const npcIndex = nextNpcs.findIndex(npc => npc.id === command.npcId);
      if (npcIndex < 0) throw new Error('Comando rejeitado: NPC não encontrado.');
      const actorIndex = nextState.npcActors.findIndex(actor => actor.npcId === command.npcId);
      const currentActor = actorIndex >= 0 ? nextState.npcActors[actorIndex] : {
        npcId: command.npcId, emotionalState: nextNpcs[npcIndex].emotionalState || 'neutro', nextAction: '', location: nextNpcs[npcIndex].location,
        faction: nextNpcs[npcIndex].faction, beliefs: [], suspicions: [], allies: [], enemies: [], relationshipByHero: {}, lastUpdatedAt: Date.now(),
      };
      const actor = {
        ...currentActor,
        emotionalState: command.emotionalState ?? currentActor.emotionalState,
        nextAction: command.nextAction ?? currentActor.nextAction,
        beliefs: command.beliefs ?? currentActor.beliefs,
        suspicions: command.suspicions ?? currentActor.suspicions,
        location: command.location ?? currentActor.location,
        lastUpdatedAt: Date.now(),
      };
      if (actorIndex >= 0) nextState.npcActors[actorIndex] = actor; else nextState.npcActors.push(actor);
      nextNpcs[npcIndex] = { ...nextNpcs[npcIndex], emotionalState: actor.emotionalState, nextAction: actor.nextAction, location: actor.location, updatedAt: Date.now() };
      break;
    }
    case 'open_scene': {
      assertSceneTransitionAllowed(nextState.scene, command.kind);
      if (!sceneCompatibleWithMode(command.kind, nextState.activeMode)) {
        throw new Error(`Comando rejeitado: a cena ${command.kind} não pertence ao modo ${nextState.activeMode}.`);
      }
      const heroIds = (command.participantHeroIds?.length ? command.participantHeroIds : Array.from(partyIds)).filter(id => partyIds.has(id));
      if (!heroIds.length) throw new Error('Comando rejeitado: a cena não possui Heróis-jogadores válidos.');
      const validNpcs = new Set(nextNpcs.map(npc => npc.id));
      const npcIds = (command.participantNpcIds?.length ? command.participantNpcIds : nextCampaign.presentNpcIds || []).filter(id => validNpcs.has(id));
      const spotlight = heroIds.includes(nextState.scene.spotlightCharacterId || '') ? nextState.scene.spotlightCharacterId : heroIds[0] || null;
      const updatingCurrent = nextState.scene.status === 'active' && nextState.scene.kind === command.kind;
      nextState.scene = {
        id: updatingCurrent ? nextState.scene.id : `scene_state_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
        kind: command.kind,
        status: 'active',
        title: command.title || (updatingCurrent ? nextState.scene.title : sceneLabel(command.kind)),
        objective: command.objective || (updatingCurrent ? nextState.scene.objective : ''),
        locationName: nextState.position.locationName || nextCampaign.currentLocation,
        participantHeroIds: heroIds,
        participantNpcIds: npcIds,
        spotlightCharacterId: spotlight,
        spotlightQueue: spotlight ? [spotlight, ...heroIds.filter(id => id !== spotlight)] : heroIds,
        pendingActions: updatingCurrent ? nextState.scene.pendingActions : {},
        tension: Math.max(0, Math.min(6, Number(command.tension ?? nextState.scene.tension) || 0)),
        endCondition: command.endCondition || (updatingCurrent ? nextState.scene.endCondition : ''),
        startedAt: updatingCurrent ? nextState.scene.startedAt : Date.now(),
        updatedAt: Date.now(),
      };
      nextCampaign.presentNpcIds = npcIds;
      nextNpcs = nextNpcs.map(npc => ({ ...npc, presentInScene: npcIds.includes(npc.id), updatedAt: Date.now() }));
      break;
    }
    case 'close_scene':
      nextState.scene = { ...nextState.scene, status: 'closed', pendingActions: {}, updatedAt: Date.now() };
      break;
    case 'set_spotlight':
      if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: foco fora da Companhia.');
      nextState.scene = {
        ...nextState.scene,
        spotlightCharacterId: command.targetCharacterId,
        spotlightQueue: [command.targetCharacterId, ...nextState.scene.participantHeroIds.filter(id => id !== command.targetCharacterId)],
        updatedAt: Date.now(),
      };
      nextState.sessionDirector = { ...nextState.sessionDirector, nextSuggestedHeroId: command.targetCharacterId, updatedAt: Date.now() };
      break;
    case 'upsert_threat': {
      const index = nextState.threatClocks.findIndex(clock => clock.id === command.clock.id);
      const baseClock = index >= 0 ? nextState.threatClocks[index] : { id: command.clock.id, name: command.clock.name, current: 0, max: 6, consequence: '', hidden: true, updatedAt: Date.now() };
      const updatedClock = { ...baseClock, ...command.clock, id: baseClock.id, name: command.clock.name || baseClock.name, current: Math.max(0, Number(command.clock.current ?? baseClock.current) || 0), max: Math.max(1, Number(command.clock.max ?? baseClock.max) || 6), updatedAt: Date.now() };
      updatedClock.current = Math.min(updatedClock.current, updatedClock.max);
      if (index >= 0) nextState.threatClocks[index] = updatedClock; else nextState.threatClocks.push(updatedClock);
      break;
    }
    case 'advance_threat': {
      const amount = safeDelta(command.amount);
      const index = nextState.threatClocks.findIndex(clock => clock.id === command.clockId);
      if (index < 0) throw new Error('Comando rejeitado: relógio de ameaça não encontrado.');
      const clock = nextState.threatClocks[index];
      nextState.threatClocks[index] = { ...clock, current: Math.max(0, Math.min(clock.max, clock.current + amount)), updatedAt: Date.now() };
      break;
    }
    case 'upsert_quest': {
      const index = nextState.quests.findIndex(quest => quest.id === command.quest.id);
      const base = index >= 0 ? nextState.quests[index] : {
        id: command.quest.id, title: command.quest.title, status: 'active' as const, currentStep: '', steps: [], consequences: '', rewards: '', updatedAt: Date.now(),
      };
      const updated = { ...base, ...command.quest, id: base.id, title: command.quest.title || base.title, updatedAt: Date.now() };
      if (index >= 0) nextState.quests[index] = updated; else nextState.quests.push(updated);
      break;
    }
    case 'add_knowledge': {
      if (command.entry.visibility === 'hero' && (!command.entry.heroId || !partyIds.has(command.entry.heroId))) throw new Error('Comando rejeitado: conhecimento individual sem Herói-jogador válido.');
      const now = Date.now();
      const existing = nextState.knowledge.findIndex(entry => entry.id === command.entry.id);
      const value = { ...command.entry, createdAt: existing >= 0 ? nextState.knowledge[existing].createdAt : now, updatedAt: now };
      if (existing >= 0) nextState.knowledge[existing] = value; else nextState.knowledge.push(value);
      break;
    }
    case 'set_living_company':
      nextState.livingWorld = {
        ...nextState.livingWorld,
        companyStyle: command.companyStyle,
        troupeName: safeText(command.troupeName ?? nextState.livingWorld.troupeName, 80),
        baseLocation: safeText(command.baseLocation ?? nextState.livingWorld.baseLocation, 120),
        updatedAt: Date.now(),
      };
      break;
    case 'upsert_hero_life': {
      if (!partyIds.has(command.characterId)) throw new Error('Comando rejeitado: vida cotidiana atribuída a personagem fora da Companhia.');
      const previous = nextState.livingWorld.heroLives[command.characterId];
      const incomingMembers = command.life.householdMembers ?? previous?.householdMembers ?? [];
      const householdMembers = incomingMembers.slice(0, 20).map(member => ({
        id: safeId(member.id || `${command.characterId}_${member.name}_${member.relation}`),
        name: safeText(member.name, 80),
        relation: safeText(member.relation, 80),
        age: Math.max(0, Math.min(150, Math.trunc(Number(member.age) || 0))),
        notes: safeText(member.notes, 300),
      })).filter(member => member.name);
      nextState.livingWorld = {
        ...nextState.livingWorld,
        heroLives: {
          ...nextState.livingWorld.heroLives,
          [command.characterId]: {
            characterId: command.characterId,
            lifestyle: command.life.lifestyle || previous?.lifestyle || 'adventurer',
            occupation: safeText(command.life.occupation ?? previous?.occupation ?? '', 120),
            home: safeText(command.life.home ?? previous?.home ?? '', 160),
            troupeRole: safeText(command.life.troupeRole ?? previous?.troupeRole ?? '', 120),
            householdName: safeText(command.life.householdName ?? previous?.householdName ?? '', 120),
            householdMembers,
            notes: safeText(command.life.notes ?? previous?.notes ?? '', 500),
            updatedAt: Date.now(),
          },
        },
        updatedAt: Date.now(),
      };
      break;
    }
    case 'upsert_life_project': {
      if (!command.project.title.trim()) throw new Error('Comando rejeitado: projeto de vida sem título.');
      const index = nextState.livingWorld.projects.findIndex(project => project.id === command.project.id);
      const previous = index >= 0 ? nextState.livingWorld.projects[index] : null;
      const ownerCharacterIds = Array.from(new Set((command.project.ownerCharacterIds || previous?.ownerCharacterIds || Array.from(partyIds)).filter(id => partyIds.has(id))));
      if (!ownerCharacterIds.length) throw new Error('Comando rejeitado: projeto sem Herói responsável na Companhia.');
      const target = Math.max(1, Math.min(12, Math.trunc(Number(command.project.target ?? previous?.target ?? 4) || 4)));
      const project = {
        id: safeId(command.project.id),
        kind: command.project.kind,
        title: safeText(command.project.title, 120),
        ownerCharacterIds,
        location: safeText(command.project.location ?? previous?.location ?? nextState.position.locationName, 120),
        progress: Math.max(0, Math.min(target, Math.trunc(Number(command.project.progress ?? previous?.progress ?? 0) || 0))),
        target,
        status: command.project.status || previous?.status || 'active' as const,
        lastResolution: safeText(command.project.lastResolution ?? previous?.lastResolution ?? '', 300),
        notes: safeText(command.project.notes ?? previous?.notes ?? '', 500),
        startedAtYear: previous?.startedAtYear || nextState.worldClock.year,
        startedAtDay: previous?.startedAtDay || nextState.worldClock.day,
        updatedAt: Date.now(),
      };
      if (project.progress >= project.target) project.status = 'completed';
      if (index >= 0) nextState.livingWorld.projects[index] = project; else nextState.livingWorld.projects.push(project);
      nextState.livingWorld.updatedAt = Date.now();
      break;
    }
    case 'advance_life_project': {
      const index = nextState.livingWorld.projects.findIndex(project => project.id === command.projectId);
      if (index < 0) throw new Error('Comando rejeitado: projeto de vida não encontrado.');
      if (command.resolution.trim().length < 3) throw new Error('Comando rejeitado: registre qual cena, teste ou empreendimento resolveu esta etapa.');
      const previous = nextState.livingWorld.projects[index];
      if (previous.status !== 'active') throw new Error('Comando rejeitado: somente projetos ativos podem avançar.');
      const amount = Math.max(1, Math.min(3, Math.trunc(Number(command.amount) || 1)));
      const progress = Math.min(previous.target, previous.progress + amount);
      nextState.livingWorld.projects[index] = {
        ...previous,
        progress,
        status: progress >= previous.target ? 'completed' : 'active',
        lastResolution: safeText(command.resolution, 300),
        updatedAt: Date.now(),
      };
      nextState.livingWorld.updatedAt = Date.now();
      break;
    }
    case 'advance_time': {
      const advanced = advanceWorldClock(nextState.worldClock, command.minutes);
      nextState.worldClock = advanced.clock;
      { const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, command.minutes); nextState = simulated.master; nextNpcs = simulated.npcs; }
      if (advanced.yearsElapsed > 0) {
        nextCharacters = nextCharacters.map(character => ({
          ...character,
          sheet: {
            ...character.sheet,
            identity: {
              ...character.sheet.identity,
              age: Math.max(0, Number(character.sheet.identity.age) || 0) + advanced.yearsElapsed,
            },
          },
          updatedAt: Date.now(),
        }));
        nextState.livingWorld = {
          ...nextState.livingWorld,
          heroLives: Object.fromEntries(Object.entries(nextState.livingWorld.heroLives).map(([characterId, life]) => [
            characterId,
            {
              ...life,
              householdMembers: life.householdMembers.map(member => ({ ...member, age: Math.min(150, member.age + advanced.yearsElapsed) })),
              updatedAt: Date.now(),
            },
          ])),
          updatedAt: Date.now(),
        };
      }
      break;
    }
    case 'set_mode':
      assertModeTransitionAllowed(nextState, command.mode);
      nextState = { ...nextState, activeMode: command.mode, updatedAt: Date.now() };
      if (nextState.scene.status === 'idle') nextState.scene = { ...nextState.scene, kind: modeToScene(command.mode), updatedAt: Date.now() };
      break;
    case 'start_journey':
      nextState = startJourney(nextState, command.destinationHexId, command.roles || nextState.company.journeyRoles);
      nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    case 'complete_journey':
      nextState = completeJourney(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;
    case 'start_council':
      nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
      nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
      break;
    case 'record_council_result': nextState = recordCouncilResult(nextState, command.success); break;
    case 'resolve_council':
      nextState = resolveCouncil(nextState, command.outcome);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;
    case 'start_combat': {
      const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
      nextState = startCombat(nextState, nextCharacters, heroActorIds);
      const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
      const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
      sceneNpcIds.forEach((npcId, index) => {
        const npc = nextNpcs.find(item => item.id === npcId);
        if (!npc) return;
        const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
        nextState = upsertCombatActor(nextState, {
          actorId: npc.id,
          actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
          engagedWith: [],
          endurance: 10,
          enduranceMax: 10,
          hate: 2,
          wounded: false,
          defeated: false,
          initiative: Math.max(1, 10 - index),
          tokenId: npc.tokenId || null,
          position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
          quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
        });
      });
      nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
      appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
      appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
      break;
    }
    case 'set_combat_stance':
      if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: postura para personagem fora da Companhia.');
      nextState = setStance(nextState, command.targetCharacterId, command.stance);
      break;
    case 'next_combat_round': nextState = nextRound(nextState); break;
    case 'end_combat': {
      const defeated = nextState.combat.actors.filter(actor => actor.defeated).length;
      const finalRound = Math.max(1, nextState.combat.round || 1);
      nextState = endCombat(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      appendCampaignEvent('combat_ended', { round: finalRound, defeated, location: nextState.position.locationName || nextCampaign.currentLocation });
      appendLivingActivity('world', 'Combate encerrado', `Confronto encerrado na rodada ${finalRound}; ${defeated} participante(s) derrotado(s).`, nextState.combat.id);
      break;
    }
    case 'upsert_combat_actor':
      nextState = upsertCombatActor(nextState, command.actor);
      break;
    case 'move_combat_actor':
      nextState = moveCombatActor(nextState, command.actorId, command.x, command.y);
      break;
    case 'record_combat_narrative': {
      const text = safeText(command.text, 3000);
      if (!text) throw new Error('Comando rejeitado: narrativa de combate vazia.');
      nextState = recordCombatNarrative(nextState, text);
      appendCampaignEvent('custom', { category: 'combat_narrative', round: Math.max(1, nextState.combat.round || 1), text });
      appendLivingActivity('world', `Combate — rodada ${Math.max(1, nextState.combat.round || 1)}`, text, nextState.combat.id);
      break;
    }
    case 'record_combat_roll':
      nextState = recordCombatRoll(nextState, command.roll);
      break;
    case 'set_combat_background':
      nextState = setCombatBackground(nextState, command.imageData);
      break;
    case 'upsert_organization': {
      const previous = (nextState.livingWorld.organizations || []).find(item => item.id === command.organization.id);
      const organization = normalizeOrganization(command.organization, previous);
      nextState.livingWorld = {
        ...nextState.livingWorld,
        organizations: [...(nextState.livingWorld.organizations || []).filter(item => item.id !== organization.id), organization],
        activityLog: [...(nextState.livingWorld.activityLog || []), { id: `activity_org_${Date.now()}`, kind: 'organization' as const, title: previous ? `Organização atualizada: ${organization.name}` : `Organização criada: ${organization.name}`, detail: command.reason, day: nextState.worldClock.day, createdAt: Date.now(), sourceId: organization.id }].slice(-300),
        updatedAt: Date.now(),
      };
      break;
    }
    case 'archive_organization': {
      const organizations = nextState.livingWorld.organizations || [];
      const existing = organizations.find(item => item.id === command.organizationId);
      if (!existing) throw new Error('Poder não encontrado.');
      const archived = { ...existing, archived: command.archived, active: !command.archived, updatedAt: Date.now() };
      nextState.livingWorld = { ...nextState.livingWorld, organizations: organizations.map(item => item.id === existing.id ? archived : item), updatedAt: Date.now() };
      appendLivingActivity('organization', command.archived ? `Poder arquivado: ${existing.name}` : `Poder reativado: ${existing.name}`, command.reason, existing.id);
      break;
    }
    case 'delete_organization': {
      const organizations = nextState.livingWorld.organizations || [];
      const existing = organizations.find(item => item.id === command.organizationId);
      if (!existing) throw new Error('Poder não encontrado.');
      const strongholds = (nextState.livingWorld.strongholds || []).map(stronghold => stronghold.ownerOrganizationId === existing.id ? { ...stronghold, previousOwnerOrganizationId: existing.id, ownerOrganizationId: undefined, ownerKind: 'unclaimed' as const, status: stronghold.status === 'controlled' ? 'abandoned' as const : stronghold.status, updatedAt: Date.now() } : stronghold);
      nextState.livingWorld = { ...nextState.livingWorld, organizations: organizations.filter(item => item.id !== existing.id), strongholds, updatedAt: Date.now() };
      appendLivingActivity('organization', `Poder removido: ${existing.name}`, command.reason, existing.id);
      break;
    }
    case 'advance_organization_project':
      nextState.livingWorld = advanceOrganizationProject(nextState.livingWorld, command.organizationId, command.projectId, command.amount, command.reason);
      break;
    case 'upsert_stronghold': {
      const previous = (nextState.livingWorld.strongholds || []).find(item => item.id === command.stronghold.id);
      const stronghold = normalizeStronghold({ ...command.stronghold, discoveredDay: command.stronghold.discoveredDay ?? previous?.discoveredDay ?? nextState.worldClock.day }, previous);
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: [...(nextState.livingWorld.strongholds || []).filter(item => item.id !== stronghold.id), stronghold], updatedAt: Date.now() };
      appendLivingActivity('world', previous ? `Local persistente atualizado: ${stronghold.name}` : `Local persistente descoberto: ${stronghold.name}`, command.reason, stronghold.id);
      break;
    }
    case 'claim_stronghold': {
      const strongholds = nextState.livingWorld.strongholds || [];
      const existing = strongholds.find(item => item.id === command.strongholdId);
      if (!existing) throw new Error('Refúgio ou fortaleza não encontrado.');
      if (nextState.combat.status !== 'idle' && nextState.combat.status !== 'resolved') throw new Error('A posse não pode mudar enquanto o combate ainda está ativo.');
      const previousOwner = existing.ownerOrganizationId || existing.previousOwnerOrganizationId;
      const captured = normalizeStronghold({
        ...existing,
        ownerKind: 'company',
        ownerOrganizationId: undefined,
        previousOwnerOrganizationId: previousOwner,
        status: 'controlled',
        knownToCompany: true,
        defenders: 0,
        capturedDay: nextState.worldClock.day,
        ownershipHistory: [...(existing.ownershipHistory || []), { ownerKind: 'company', label: 'Companhia', day: nextState.worldClock.day, reason: command.reason, createdAt: Date.now() }],
      }, existing);
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: strongholds.map(item => item.id === captured.id ? captured : item), updatedAt: Date.now() };
      appendLivingActivity('world', `Fortaleza conquistada: ${captured.name}`, `${command.reason} O estoque existente permanece no local até ser distribuído pela Companhia.`, captured.id);
      appendCampaignEvent('custom', { category: 'stronghold_claimed', strongholdId: captured.id, name: captured.name });
      break;
    }
    case 'abandon_stronghold': {
      const strongholds = nextState.livingWorld.strongholds || [];
      const existing = strongholds.find(item => item.id === command.strongholdId);
      if (!existing) throw new Error('Refúgio ou fortaleza não encontrado.');
      const abandoned = normalizeStronghold({ ...existing, ownerKind: 'unclaimed', ownerOrganizationId: undefined, status: 'abandoned', ownershipHistory: [...(existing.ownershipHistory || []), { ownerKind: 'unclaimed', label: 'Sem ocupante estável', day: nextState.worldClock.day, reason: command.reason, createdAt: Date.now() }] }, existing);
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: strongholds.map(item => item.id === abandoned.id ? abandoned : item), updatedAt: Date.now() };
      appendLivingActivity('world', `Local abandonado: ${abandoned.name}`, command.reason, abandoned.id);
      break;
    }
    case 'delete_stronghold': {
      const strongholds = nextState.livingWorld.strongholds || [];
      const existing = strongholds.find(item => item.id === command.strongholdId);
      if (!existing) throw new Error('Refúgio ou fortaleza não encontrado.');
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: strongholds.filter(item => item.id !== existing.id), updatedAt: Date.now() };
      appendLivingActivity('world', `Local removido: ${existing.name}`, command.reason, existing.id);
      break;
    }
    case 'transfer_stronghold_loot': {
      const strongholds = nextState.livingWorld.strongholds || [];
      const existing = strongholds.find(item => item.id === command.strongholdId);
      if (!existing) throw new Error('Refúgio ou fortaleza não encontrado.');
      if (existing.ownerKind !== 'company' || existing.status !== 'controlled') throw new Error('A Companhia precisa controlar o local antes de retirar seu estoque.');
      const loot = existing.loot.find(item => item.id === command.lootItemId && item.state === 'stored');
      if (!loot) throw new Error('Item de estoque não encontrado ou já retirado.');
      const quantity = Math.max(1, Math.min(Math.trunc(command.quantity || 1), loot.quantity));
      updateHero(command.targetCharacterId, sheet => {
        const inventory = [...(sheet.inventory || [])];
        const index = inventory.findIndex(item => item.name.toLowerCase() === loot.name.toLowerCase() && item.category === loot.category);
        if (index >= 0) inventory[index] = { ...inventory[index], quantity: inventory[index].quantity + quantity };
        else inventory.push({ id: `loot_${loot.id}_${Date.now()}`, name: loot.name, quantity, weight: loot.load, load: loot.load, category: loot.category, description: loot.description, equipped: false, value: loot.value, tags: [...loot.tags] });
        const load = inventory.reduce((sum, item) => sum + Math.max(0, Number(item.load ?? item.weight ?? 0)) * Math.max(0, Number(item.quantity || 0)), 0);
        return { ...sheet, inventory, attributes: { ...sheet.attributes, load } };
      });
      const remaining = loot.quantity - quantity;
      const nextLoot = existing.loot.map(item => item.id !== loot.id ? item : remaining > 0 ? { ...item, quantity: remaining } : { ...item, quantity: 0, state: 'claimed' as const, assignedCharacterId: command.targetCharacterId });
      const strategicSupplyCost = loot.category === 'Provisões' ? quantity : 0;
      const updated = normalizeStronghold({ ...existing, loot: nextLoot, supplies: Math.max(0, existing.supplies - strategicSupplyCost) }, existing);
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: strongholds.map(item => item.id === updated.id ? updated : item), updatedAt: Date.now() };
      appendLivingActivity('world', `Estoque retirado de ${existing.name}`, `${quantity}× ${loot.name} foi entregue a um Herói da Companhia.`, existing.id);
      break;
    }
    case 'transfer_stronghold_treasure': {
      const strongholds = nextState.livingWorld.strongholds || [];
      const existing = strongholds.find(item => item.id === command.strongholdId);
      if (!existing) throw new Error('Refúgio ou fortaleza não encontrado.');
      if (existing.ownerKind !== 'company' || existing.status !== 'controlled') throw new Error('A Companhia precisa controlar o local antes de retirar o tesouro.');
      const amount = Math.max(1, Math.min(Math.trunc(command.amount || 1), existing.treasure));
      if (amount <= 0 || existing.treasure <= 0) throw new Error('Não há tesouro disponível nesse local.');
      updateHero(command.targetCharacterId, sheet => ({ ...sheet, resources: { ...sheet.resources, treasure: Math.max(0, Number(sheet.resources.treasure || 0)) + amount } }));
      const updated = normalizeStronghold({ ...existing, treasure: existing.treasure - amount }, existing);
      nextState.livingWorld = { ...nextState.livingWorld, strongholds: strongholds.map(item => item.id === updated.id ? updated : item), updatedAt: Date.now() };
      appendLivingActivity('world', `Tesouro retirado de ${existing.name}`, `${amount} ponto(s) de Tesouro foram atribuídos a um Herói da Companhia por decisão explícita do jogador.`, existing.id);
      break;
    }
    case 'record_living_activity':
      nextState.livingWorld = { ...nextState.livingWorld, activityLog: [...(nextState.livingWorld.activityLog || []), { ...command.activity, createdAt: command.activity.createdAt || Date.now() }].slice(-300), updatedAt: Date.now() };
      break;
    case 'start_fellowship_phase':
      nextState = startFellowshipPhase(nextState, command.locationId);
      nextState.scene = modeScene(nextState, 'fellowship', 'Fase de Sociedade', 'Recuperar-se e preparar o futuro', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    case 'end_fellowship_phase':
      nextState = endFellowshipPhase(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;
    default: assertNever(command);
  }

  const active = nextCharacters.find(character => character.id === nextCampaign.activeCharacterId);
  if (active) nextCampaign.characterSheet = structuredClone(active.sheet);
  return { campaign: nextCampaign, characters: nextCharacters, npcs: nextNpcs, masterState: nextState };
}

function normalizeNpcDraft(draft: NpcDraft, previous: Npc | null, fallbackLocation: string, id: string): Npc {
  const now = Date.now();
  return {
    id,
    name: draft.name.trim(),
    culture: draft.culture ?? previous?.culture ?? '',
    peoples: draft.peoples ?? previous?.peoples ?? '',
    role: draft.role ?? previous?.role ?? '',
    faction: draft.faction ?? previous?.faction ?? '',
    appearance: draft.appearance ?? previous?.appearance ?? '',
    personality: draft.personality ?? previous?.personality ?? '',
    attitude: draft.attitude ?? previous?.attitude ?? 'Neutra',
    objectives: draft.objectives ?? previous?.objectives ?? '',
    fears: draft.fears ?? previous?.fears ?? '',
    secrets: draft.secrets ?? previous?.secrets ?? '',
    notes: draft.notes ?? previous?.notes ?? '',
    trust: Math.max(0, Math.min(100, Number(draft.trust ?? previous?.trust ?? 50) || 50)),
    firstMeeting: previous?.firstMeeting || new Date(now).toISOString(),
    lastMeeting: new Date(now).toISOString(),
    location: draft.location ?? previous?.location ?? fallbackLocation,
    state: draft.state ?? previous?.state ?? 'Normal',
    tokenId: previous?.tokenId || null,
    presentInScene: Boolean(draft.presentInScene ?? previous?.presentInScene ?? true),
    voiceProfile: (draft.voiceProfile as Npc['voiceProfile']) || previous?.voiceProfile || 'Kore',
    speechStyle: draft.speechStyle ?? previous?.speechStyle ?? '',
    emotionalState: draft.emotionalState ?? previous?.emotionalState ?? draft.state ?? 'Neutro',
    nextAction: draft.nextAction ?? previous?.nextAction ?? '',
    memorySummary: previous?.memorySummary || '',
    lifeStatus: draft.lifeStatus ?? previous?.lifeStatus ?? 'alive',
    wealthIndex: Math.max(0, Math.min(100, Number(draft.wealthIndex ?? previous?.wealthIndex ?? 50) || 0)),
    wealthTier: previous?.wealthTier ?? 'modest',
    memoryEvents: previous?.memoryEvents || [],
    commitments: draft.commitments ?? previous?.commitments ?? [],
    lastWorldTickDay: previous?.lastWorldTickDay,
    relationshipByHero: previous?.relationshipByHero || {},
    tokenAssignmentMode: previous?.tokenAssignmentMode,
    tokenMatchScore: previous?.tokenMatchScore,
    tokenMatchReason: previous?.tokenMatchReason,
    tokenDescriptorId: previous?.tokenDescriptorId,
    createdAt: previous?.createdAt || now,
    updatedAt: now,
  };
}

function resolveNpcId(draft: NpcDraft, npcs: Npc[]): string {
  if (draft.id?.trim()) return safeId(draft.id);
  const base = `npc_${safeId(`${draft.name}_${draft.location || ''}_${draft.role || ''}`)}`.slice(0, 80);
  const exact = npcs.find(npc => safeId(`${npc.name}_${npc.location || ''}_${npc.role || ''}`) === base.replace(/^npc_/, ''));
  if (exact) return exact.id;
  let id = base || `npc_${Date.now()}`;
  let suffix = 2;
  while (npcs.some(npc => npc.id === id)) id = `${base}_${suffix++}`;
  return id;
}

function safeId(value: string): string {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || `npc_${Date.now()}`;
}

function modeToScene(mode: MasterState['activeMode']): SceneKind {
  return mode === 'adventure' ? 'exploration' : mode;
}

function sceneLabel(kind: SceneKind): string {
  return { exploration: 'Exploração', dialogue: 'Diálogo', journey: 'Jornada', council: 'Conselho', combat: 'Combate', rest: 'Descanso', fellowship: 'Fase de Sociedade' }[kind];
}

function modeScene(state: MasterState, kind: SceneKind, title: string, objective: string, heroes: string[], npcs: string[]): MasterState['scene'] {
  const now = Date.now();
  const spotlight = heroes.includes(state.scene.spotlightCharacterId || '') ? state.scene.spotlightCharacterId : heroes[0] || null;
  return {
    id: `scene_state_${now}_${Math.random().toString(36).slice(2, 7)}`,
    kind,
    status: 'active',
    title,
    objective,
    locationName: state.position.locationName,
    participantHeroIds: Array.from(new Set(heroes)),
    participantNpcIds: Array.from(new Set(npcs)),
    spotlightCharacterId: spotlight,
    spotlightQueue: spotlight ? [spotlight, ...heroes.filter(id => id !== spotlight)] : heroes,
    pendingActions: {},
    tension: kind === 'combat' ? 5 : kind === 'council' ? 3 : 1,
    endCondition: '',
    startedAt: now,
    updatedAt: now,
  };
}

function safeDelta(value: number): number {
  const numeric = Math.trunc(Number(value) || 0);
  if (Math.abs(numeric) > 100) throw new Error('Comando rejeitado: alteração fora do limite seguro.');
  return numeric;
}

function safeText(value: unknown, maxLength: number): string {
  return String(value ?? '').trim().slice(0, maxLength);
}

function commandTargetSnapshot(command: MasterCommand, campaign: CampaignState, characters: Character[], npcs: Npc[], state: MasterState): unknown {
  if ('targetCharacterId' in command) return characters.find(character => character.id === command.targetCharacterId)?.sheet.attributes;
  if (command.type === 'upsert_npc') return npcs.find(npc => npc.id === command.npc.id || npc.name === command.npc.name);
  if (command.type === 'update_npc_actor') return { npc: npcs.find(npc => npc.id === command.npcId), actor: state.npcActors.find(actor => actor.npcId === command.npcId) };
  if (command.type === 'open_scene' || command.type === 'close_scene') return state.scene;
  if (command.type === 'move_party') return state.position;
  if (command.type.includes('journey')) return state.journey;
  if (command.type.includes('council')) return state.council;
  if (command.type.includes('combat')) return state.combat;
  if (command.type.includes('organization') || command.type.includes('stronghold') || command.type === 'record_living_activity') return state.livingWorld;
  if (command.type.includes('fellowship')) return state.fellowshipPhase;
  if (command.type.includes('life') || command.type === 'set_living_company') return state.livingWorld;
  if (command.type === 'set_present_npcs') return campaign.presentNpcIds;
  return state;
}

function assertNever(value: never): never {
  throw new Error(`Comando não suportado: ${JSON.stringify(value)}`);
}
