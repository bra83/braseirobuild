from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_commands.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

def req(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

rel = "lib/master/commands.ts"
cmd = read(rel)

update_end = """  };

  switch (command.type) {"""
eye_helper = """  };

  const raiseEyeAwareness = (amount: number, reason: string) => {
    if (!nextState.company.eyeAwarenessEnabled || nextState.activeMode === 'combat') return;
    const delta = Math.max(0, Math.trunc(Number(amount) || 0));
    if (!delta) return;
    const threshold = Math.max(1, Number(nextState.company.eyeHuntThreshold) || 16);
    const value = Math.max(0, Number(nextState.company.eyeAwareness) || 0) + delta;
    nextState.company = {
      ...nextState.company,
      eyeAwareness: value,
      eyeRevelationPending: value >= threshold,
      eyeLastChangeReason: reason,
    };
  };

  switch (command.type) {"""
cmd = req(cmd, update_end, eye_helper, "eye helper")

cmd = req(
    cmd,
    "case 'adjust_shadow': updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, safeDelta(command.amount))); break;",
    """case 'adjust_shadow': {
      const delta = safeDelta(command.amount);
      let actualGain = 0;
      updateHero(command.targetCharacterId, sheet => {
        const before = Math.max(0, Number(sheet.attributes.shadow) || 0);
        const next = adjustShadowCanonical(sheet, delta);
        actualGain = Math.max(0, Math.max(0, Number(next.attributes.shadow) || 0) - before);
        return next;
      });
      if (actualGain > 0) raiseEyeAwareness(actualGain, '+' + String(actualGain) + ' por Sombra ganha fora de combate.');
      break;
    }""",
    "adjust shadow eye",
)

cmd = req(
    cmd,
    """    case 'resolve_shadow_gain': {
      updateHero(command.targetCharacterId, sheet => resolveShadowGain(sheet, command.amount, command.source, { success: command.testSuccess, successIcons: command.successIcons }).sheet);
      break;
    }""",
    """    case 'resolve_shadow_gain': {
      let actualGain = 0;
      updateHero(command.targetCharacterId, sheet => {
        const before = Math.max(0, Number(sheet.attributes.shadow) || 0);
        const next = resolveShadowGain(sheet, command.amount, command.source, { success: command.testSuccess, successIcons: command.successIcons }).sheet;
        actualGain = Math.max(0, Math.max(0, Number(next.attributes.shadow) || 0) - before);
        return next;
      });
      if (actualGain > 0) raiseEyeAwareness(actualGain, '+' + String(actualGain) + ' por Sombra ganha fora de combate.');
      break;
    }""",
    "resolve shadow eye",
)

start = cmd.index("    case 'start_journey':")
end = cmd.index("\n    case 'start_council':", start)

journey_cases = """    case 'start_journey':
    case 'resolve_journey_march':
    case 'generate_journey_event':
    case 'resolve_journey_event':
    case 'continue_journey_stage':
    case 'complete_journey':
      throw new Error('Decisão de Jornada bloqueada no fluxo automático. Use a etapa explícita da interface do jogador.');

    case 'set_eye_awareness_rule': {
      const initial = Math.max(0, Math.trunc(Number(command.initial) || 0));
      const threshold = Math.max(1, Math.trunc(Number(command.huntThreshold) || 16));
      nextState.company = {
        ...nextState.company,
        eyeAwarenessEnabled: Boolean(command.enabled),
        eyeAwarenessInitial: initial,
        eyeAwareness: command.enabled ? initial : 0,
        eyeHuntThreshold: threshold,
        eyeRevelationPending: false,
        eyeLastChangeReason: command.enabled ? 'Vigilância do Olho ativada para esta campanha.' : 'Vigilância do Olho desativada para esta campanha.',
      };
      break;
    }

    case 'adjust_eye_awareness': {
      if (!nextState.company.eyeAwarenessEnabled) break;
      const amount = Math.max(0, Math.trunc(Number(command.amount) || 0));
      if (!amount) break;
      const threshold = Math.max(1, Number(nextState.company.eyeHuntThreshold) || 16);
      const value = Math.max(0, Number(nextState.company.eyeAwareness) || 0) + amount;
      nextState.company = {
        ...nextState.company,
        eyeAwareness: value,
        eyeRevelationPending: value >= threshold,
        eyeLastChangeReason: command.reason,
      };
      break;
    }

    case 'resolve_eye_revelation': {
      if (!nextState.company.eyeAwarenessEnabled || !nextState.company.eyeRevelationPending) throw new Error('Nenhum episódio de Revelação está aguardando resolução.');
      nextState.company = {
        ...nextState.company,
        eyeAwareness: Math.max(0, Number(nextState.company.eyeAwarenessInitial) || 0),
        eyeRevelationPending: false,
        eyeLastChangeReason: 'Episódio de Revelação apresentado; Vigilância voltou ao valor inicial.',
      };
      break;
    }

    case 'player_start_journey': {
      const roles = command.roles || {};
      const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
      if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
      const route = (command.routeHexIds || []).filter(Boolean);
      if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
      nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
      nextState.journey = {
        ...nextState.journey,
        pendingRoll: null,
        lastRoll: null,
        endRollsByHero: {},
        encounterPending: false,
        lastEventResult: null,
      };
      nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    }

    case 'player_record_journey_roll': {
      const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!hero || !partyIds.has(hero.id)) throw new Error('A rolagem de Jornada precisa pertencer a um Herói da Companhia.');
      const roll = (command.roll || {}) as any;
      const record = {
        kind: command.kind,
        characterId: hero.id,
        skill: command.skill,
        targetNumber: Math.max(1, Number(command.targetNumber) || 14),
        result: command.roll,
        createdAt: Date.now(),
      };
      if (command.kind === 'march') {
        if (!['planned', 'travelling', 'paused'].includes(nextState.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
        if (nextState.journey.pendingEvent || nextState.journey.awaitingEventGeneration || nextState.journey.awaitingEndRoll || nextState.journey.stageComplete) throw new Error('A sequência atual da Jornada precisa ser resolvida antes da Marcha.');
        nextState.journey = { ...nextState.journey, pendingRoll: record, lastRoll: record };
      } else if (command.kind === 'event') {
        const event = nextState.journey.pendingEvent;
        if (!event) throw new Error('Não existe Evento de Jornada aguardando teste.');
        const expected = nextState.company.journeyRoles[event.targetRole];
        if (expected && expected !== hero.id) throw new Error('O Evento deve ser rolado pelo Herói que ocupa o papel alvo.');
        nextState.journey = { ...nextState.journey, pendingRoll: record, lastRoll: record };
      } else {
        if (!nextState.journey.awaitingEndRoll) throw new Error('As rolagens finais de Viagem só ocorrem ao chegar ao destino.');
        const endRolls = { ...(nextState.journey.endRollsByHero || {}) };
        endRolls[hero.id] = {
          success: Boolean(roll.success),
          successIcons: Math.max(0, Number(roll.tengwarCount) || 0),
          mountVigour: Math.max(0, Number(command.mountVigour) || 0),
          roll: command.roll,
          targetNumber: Math.max(1, Number(command.targetNumber) || 14),
        };
        nextState.journey = { ...nextState.journey, endRollsByHero: endRolls, lastRoll: record };
      }
      break;
    }

    case 'player_confirm_journey_march': {
      const staged = nextState.journey.pendingRoll;
      if (!staged || staged.kind !== 'march') throw new Error('A Jogada de Marcha precisa ser rolada e exibida antes de ser aplicada.');
      const roll = (staged.result || {}) as any;
      nextState = resolveJourneyMarch(nextState, Boolean(roll.success), Math.max(0, Number(roll.tengwarCount) || 0));
      nextState.journey = { ...nextState.journey, pendingRoll: null, lastRoll: staged };
      break;
    }

    case 'player_generate_journey_event': {
      if (!nextState.journey.awaitingEventGeneration) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
      nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
      const threshold = command.region === 'border' ? 18 : command.region === 'dark' ? 14 : 16;
      if (nextState.company.eyeAwarenessEnabled) nextState.company = { ...nextState.company, eyeHuntThreshold: threshold };
      break;
    }

    case 'player_confirm_journey_event': {
      const staged = nextState.journey.pendingRoll;
      const pendingBefore = nextState.journey.pendingEvent;
      if (!staged || staged.kind !== 'event' || !pendingBefore) throw new Error('O teste do Evento precisa ser rolado e exibido antes de ser aplicado.');
      const roll = (staged.result || {}) as any;
      const success = Boolean(roll.success);
      const resolved = resolveJourneyEvent(nextState, success, Math.max(0, Number(roll.tengwarCount) || 0));
      nextState = resolved.state;
      const fx = resolved.effects;
      const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
      let shadowEyeGain = 0;

      for (const characterId of partyIds) {
        const hero = nextCharacters.find(character => character.id === characterId);
        if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
          const cramReduction = /\\bcram\\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
          pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
        }
        if (fx.allShadow) {
          updateHero(characterId, sheet => {
            const before = Math.max(0, Number(sheet.attributes.shadow) || 0);
            const next = adjustShadowCanonical(sheet, fx.allShadow);
            shadowEyeGain += Math.max(0, Math.max(0, Number(next.attributes.shadow) || 0) - before);
            return next;
          });
        }
        if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
      }

      if (fx.targetFatigue) {
        const hero = nextCharacters.find(character => character.id === staged.characterId);
        if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[staged.characterId] = Math.max(0, Number(pendingFatigue[staged.characterId]) || 0) + fx.targetFatigue;
      }
      if (fx.targetShadow) {
        updateHero(staged.characterId, sheet => {
          const before = Math.max(0, Number(sheet.attributes.shadow) || 0);
          const next = adjustShadowCanonical(sheet, fx.targetShadow);
          shadowEyeGain += Math.max(0, Math.max(0, Number(next.attributes.shadow) || 0) - before);
          return next;
        });
      }
      if (fx.woundTarget) updateHero(staged.characterId, sheet => applyHeroWound(sheet).sheet);
      if (fx.dayAdjustment > 0) {
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
        nextState = simulated.master;
        nextNpcs = simulated.npcs;
      }
      if (shadowEyeGain > 0) raiseEyeAwareness(shadowEyeGain, '+' + String(shadowEyeGain) + ' por Sombra ganha no Evento de Jornada.');

      nextState.journey = {
        ...nextState.journey,
        travelFatigueByHero: pendingFatigue,
        pendingRoll: null,
        lastRoll: staged,
        encounterPending: Boolean(nextState.journey.encounterPending || (pendingBefore.eventType === 'chance-meeting' && success)),
        lastEventResult: {
          eventType: String(pendingBefore.eventType || ''),
          description: pendingBefore.description,
          success,
          targetCharacterId: staged.characterId,
          createdAt: Date.now(),
        },
      };
      break;
    }

    case 'player_continue_journey_stage':
      if (nextState.journey.pendingRoll || nextState.journey.pendingEvent || nextState.journey.awaitingEventGeneration) throw new Error('Resolva as pendências da etapa antes de continuar.');
      nextState = continueJourneyStage(nextState);
      break;

    case 'player_complete_journey': {
      if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
      const endRolls = nextState.journey.endRollsByHero || {};
      const missing = Array.from(partyIds).filter(characterId => {
        const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
        return accumulated > 0 && !endRolls[characterId];
      });
      if (missing.length) throw new Error('Faça a rolagem final de Viagem para todos os Heróis com Fadiga de Jornada pendente antes de encerrar.');
      for (const characterId of partyIds) {
        const result = endRolls[characterId];
        const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
        const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
        const remaining = Math.max(0, accumulated - reduction);
        if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
      }
      nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {}, endRollsByHero: {}, pendingRoll: null } });
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;
    }

    case 'player_acknowledge_journey_encounter':
      nextState.journey = { ...nextState.journey, encounterPending: false };
      break;

    case 'set_journey_danger':
      nextState = setDangerousArea(nextState, command.danger);
      break;"""

cmd = cmd[:start] + journey_cases + cmd[end:]
write(rel, cmd)
print("PATCH_4938_COMMANDS_OK")
