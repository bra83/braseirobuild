from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_commands.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/master/commands.ts"
cmd=read(rel)

cmd=req(cmd,
"import { rollOneRing } from '../dice';",
"""import { rollOneRing } from '../dice';
import { attributeForSkill } from '../characterRules';
import { supplementFellowshipBonus, supplementPatronFellowshipBonus } from '../rules/supplementMechanics';
import {
  applyHuntersResolve,
  canUseSongForFirstAid,
  effectiveRestKind,
  gainGroundBonusDice,
  hasVirtue,
  journeyFatigueAfterSupplementReduction,
  supplementBonusDice,
  supplementEnemyHateLossOnSuccessfulAttack,
  supplementParryBonus,
  supplementProtectionIllFavouredForTarget,
  skirmishIncomingAttackDiceModifier,
} from '../rules/supplementMechanics';
import { striderJourneyEventDetail, striderMilestoneAward, type StriderMilestone } from '../rules/striderMode';""",
"supplement command imports")

# ---------------------------------------------------------------------------
# Eye Awareness can also decrease from Strider Fortune.
# ---------------------------------------------------------------------------
old="""    case 'adjust_eye_awareness': {
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
    }"""
new="""    case 'adjust_eye_awareness': {
      if (!nextState.company.eyeAwarenessEnabled) break;
      const amount = Math.max(0, Math.trunc(Number(command.amount) || 0));
      if (!amount) break;
      const threshold = Math.max(1, Number(nextState.company.eyeHuntThreshold) || 16);
      const sign = command.direction === 'decrease' ? -1 : 1;
      const value = Math.max(0, Number(nextState.company.eyeAwareness) || 0) + sign * amount;
      nextState.company = {
        ...nextState.company,
        eyeAwareness: Math.max(0, value),
        eyeRevelationPending: Math.max(0, value) >= threshold,
        eyeLastChangeReason: command.reason,
      };
      break;
    }"""
cmd=req(cmd,old,new,"Eye decrement")

# ---------------------------------------------------------------------------
# Permanent supplement commands.
# ---------------------------------------------------------------------------
anchor="    case 'resolve_eye_revelation': {"
insert="""    case 'set_rules_mode': {
      if (command.mode === 'strider' && partyIds.size > 2) throw new Error('A variante de Jornada do Modo Peregrino é indicada para um ou dois Heróis; Companhias com três ou mais usam as regras padrão de Jornada.');
      const oldMode = nextState.company.rulesMode === 'strider' ? 'strider' : 'standard';
      if (command.mode === 'strider' && oldMode !== 'strider') {
        nextState.company = {
          ...nextState.company,
          rulesMode: 'strider',
          preStriderFellowshipCurrent: nextState.company.fellowshipCurrent,
          preStriderFellowshipMax: nextState.company.fellowshipMax,
          preStriderFellowshipFocus: { ...(nextState.company.fellowshipFocus || {}) },
        };
        if (partyIds.size === 1) {
          const heroId = Array.from(partyIds)[0];
          const hero = nextCharacters.find(character => character.id === heroId);
          const heroBonus = hero ? supplementFellowshipBonus(hero.sheet) : 0;
          const patronBonus = supplementPatronFellowshipBonus(nextState.company.patron || '');
          const fellowship = Math.max(0, 3 + heroBonus + patronBonus);
          nextState.company = { ...nextState.company, fellowshipCurrent: fellowship, fellowshipMax: fellowship, fellowshipFocus: {} };
        }
      } else if (command.mode === 'standard' && oldMode === 'strider') {
        nextState.company = {
          ...nextState.company,
          rulesMode: 'standard',
          fellowshipCurrent: Math.max(0, Number(nextState.company.preStriderFellowshipCurrent) || Math.max(1, partyIds.size)),
          fellowshipMax: Math.max(1, Number(nextState.company.preStriderFellowshipMax) || Math.max(1, partyIds.size)),
          fellowshipFocus: { ...(nextState.company.preStriderFellowshipFocus || {}) },
          preStriderFellowshipCurrent: undefined,
          preStriderFellowshipMax: undefined,
          preStriderFellowshipFocus: undefined,
        };
      } else {
        nextState.company = { ...nextState.company, rulesMode: command.mode };
      }
      for (const characterId of partyIds) {
        updateHero(characterId, sheet => {
          const next = structuredClone(sheet);
          next.other.rulesMode = command.mode;
          const distinct = [...(next.other.distinctiveFeatures || [])].filter(item => item !== 'Peregrino');
          if (command.mode === 'strider') distinct.push('Peregrino');
          next.other.distinctiveFeatures = distinct;
          return next;
        });
      }
      break;
    }

    case 'record_strider_milestone': {
      if (nextState.company.rulesMode !== 'strider') throw new Error('Marcos desta tabela pertencem ao Modo Peregrino.');
      const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!hero || !partyIds.has(hero.id)) throw new Error('Herói inválido para o Marco do Modo Peregrino.');
      const award = striderMilestoneAward(command.milestone as StriderMilestone);
      if (!award) throw new Error('Marco do Modo Peregrino desconhecido.');
      updateHero(hero.id, sheet => {
        const next = structuredClone(sheet);
        next.resources.adventurePoints = Math.max(0, Number(next.resources.adventurePoints) || 0) + award.adventurePoints;
        next.resources.skillPoints = Math.max(0, Number(next.resources.skillPoints) || 0) + award.skillPoints;
        return next;
      });
      break;
    }

    case 'use_hunters_resolve': {
      updateHero(command.targetCharacterId, sheet => {
        const next = applyHuntersResolve(sheet);
        if (next === sheet) throw new Error('A Resolução do Caçador não está disponível neste estado.');
        return next;
      });
      break;
    }

    case 'set_beorn_enchanted_skills': {
      updateHero(command.targetCharacterId, sheet => {
        if (!hasVirtue(sheet, 'O Encantamento de Beorn')) throw new Error('O Herói não possui O Encantamento de Beorn.');
        const unique = Array.from(new Set((command.skills || []).map(value => String(value).trim()).filter(Boolean)));
        if (unique.length !== 3) throw new Error('O Encantamento de Beorn exige exatamente três Perícias de Força.');
        if (unique.some(skill => attributeForSkill(skill) !== 'strength')) throw new Error('Todas as Perícias escolhidas para O Encantamento de Beorn devem ser de Força.');
        const next = structuredClone(sheet);
        next.other.supplementSelections = { ...(next.other.supplementSelections || {}), beornEnchantedSkills: unique };
        return next;
      });
      break;
    }

    case 'gain_ground': {
      if (nextState.company.rulesMode !== 'strider' || nextState.activeMode !== 'combat') throw new Error('Ganhar Terreno só está disponível no combate do Modo Peregrino.');
      const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
      const actor = nextState.combat.actors.find(item => item.actorId === command.targetCharacterId);
      if (!hero || !actor || actor.actorType !== 'hero' || actor.stance !== 'skirmish') throw new Error('Ganhar Terreno exige um Herói na Postura de Escaramuça.');
      const skill = findSkill(hero.sheet, command.skill);
      const baseMode = hero.sheet.attributes.shadow >= hero.sheet.attributes.hopeMax ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
      const roll = rollOneRing(Math.max(0, Number(skill?.rank) || 0), 0, targetNumberForSkill(hero.sheet, command.skill), command.skill, baseMode, Boolean(hero.sheet.attributes.weary), Boolean(hero.sheet.attributes.miserable));
      nextState = recordCombatRoll(nextState, { id: roll.id, actorId: hero.id, label: 'Ganhar Terreno · ' + command.skill, featDie: roll.featResult === 12 ? 'gandalf' : roll.featResult === 11 ? 'eye' : (roll.featResult || 1), successDice: roll.successResults || [], total: roll.total, targetNumber: roll.targetNumber, success: roll.success, tengwar: roll.tengwarCount || 0, createdAt: roll.timestamp });
      const notes = (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:'));
      const bonus = gainGroundBonusDice(Boolean(roll.success), Math.max(0, Number(roll.tengwarCount) || 0));
      nextState = upsertCombatActor(nextState, { ...actor, stateNotes: bonus > 0 ? [...notes, 'gain-ground:' + String(bonus)] : notes });
      break;
    }

    case 'arm_firstborn_power': {
      const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!hero || !hasVirtue(hero.sheet, 'Poder do Primogênito')) throw new Error('O Herói não possui Poder do Primogênito.');
      const actor = nextState.combat.actors.find(item => item.actorId === command.targetCharacterId);
      if (!actor) throw new Error('O Herói precisa estar em combate para preparar Poder do Primogênito.');
      const notes = [...(actor.stateNotes || []).filter(note => note !== 'firstborn-armed'), 'firstborn-armed'];
      nextState = upsertCombatActor(nextState, { ...actor, stateNotes: notes });
      break;
    }

"""
cmd=req(cmd,anchor,insert+anchor,"supplement command cases")

# ---------------------------------------------------------------------------
# Strider Journey: no formal role choice, detailed solo event tables.
# ---------------------------------------------------------------------------
old="""    case 'player_start_journey': {
      const roles = command.roles || {};
      const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
      if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
      const route = (command.routeHexIds || []).filter(Boolean);
      if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
      nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));"""
new="""    case 'player_start_journey': {
      let roles = command.roles || {};
      if (nextState.company.rulesMode === 'strider' && partyIds.size <= 2) {
        const heroId = Array.from(partyIds)[0];
        roles = heroId ? { guide: heroId, scout: heroId, hunter: heroId, lookout: heroId } : {};
      } else {
        const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
        if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
      }
      const route = (command.routeHexIds || []).filter(Boolean);
      if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
      nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));"""
cmd=req(cmd,old,new,"Strider journey start")

old="""    case 'player_generate_journey_event': {
      if (!nextState.journey.awaitingEventGeneration) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
      nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
      const threshold = command.region === 'border' ? 18 : command.region === 'dark' ? 14 : 16;
      if (nextState.company.eyeAwarenessEnabled) nextState.company = { ...nextState.company, eyeHuntThreshold: threshold };
      break;
    }"""
new="""    case 'player_generate_journey_event': {
      if (!nextState.journey.awaitingEventGeneration) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
      nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
      if (nextState.company.rulesMode === 'strider' && nextState.journey.pendingEvent) {
        const pending = nextState.journey.pendingEvent;
        const sourceType = String(pending.eventType || '') === 'ill-choices' ? 'poor-choices' : String(pending.eventType || '');
        const detail = striderJourneyEventDetail(sourceType as any, command.targetDie);
        const heroId = Array.from(partyIds)[0] || '';
        nextState.journey = {
          ...nextState.journey,
          striderEvent: true,
          striderDetailDie: command.targetDie,
          pendingEvent: {
            ...pending,
            targetRole: 'guide',
            skill: detail?.skill || 'Vigilância',
            detail: detail?.detail || pending.description,
            notable: Boolean(detail?.notable),
            description: pending.description + ' · ' + String(detail?.detail || ''),
          },
        };
        nextState.company = { ...nextState.company, journeyRoles: { guide: heroId, scout: heroId, hunter: heroId, lookout: heroId } };
      } else {
        nextState.journey = { ...nextState.journey, striderEvent: false, striderDetailDie: undefined };
      }
      const threshold = command.region === 'border' ? 18 : command.region === 'dark' ? 14 : 16;
      if (nextState.company.eyeAwarenessEnabled) nextState.company = { ...nextState.company, eyeHuntThreshold: threshold };
      break;
    }"""
cmd=req(cmd,old,new,"Strider journey event generation")

# Strider Despair is +2 Shadow; Honey Cakes reduces Event Fatigue by 1.
cmd=req(cmd,
"      const fx = resolved.effects;\n",
"""      const fx = resolved.effects;
      if (nextState.company.rulesMode === 'strider' && String(pendingBefore.eventType || '') === 'despair' && !success) fx.allShadow = 2;
""","Strider event effects")

cmd=cmd.replace(
"pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);",
"pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + journeyFatigueAfterSupplementReduction(hero.sheet, Math.max(0, fx.allFatigue - cramReduction));"
)
cmd=cmd.replace(
"if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[staged.characterId] = Math.max(0, Number(pendingFatigue[staged.characterId]) || 0) + fx.targetFatigue;",
"if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[staged.characterId] = Math.max(0, Number(pendingFatigue[staged.characterId]) || 0) + journeyFatigueAfterSupplementReduction(hero.sheet, fx.targetFatigue);"
)

# ---------------------------------------------------------------------------
# Rest: Elven Dreams converts that hero's Short Rest recovery into Long Rest.
# ---------------------------------------------------------------------------
cmd=req(cmd,
"""          const strength = Math.max(0, Number(next.attributes.strength) || 0) * restStrengthMultiplier(next);
          if (command.kind === 'short') {
            if (!next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength + cramWisdom);
          } else {""",
"""          const strength = Math.max(0, Number(next.attributes.strength) || 0) * restStrengthMultiplier(next);
          const heroRestKind = effectiveRestKind(next, command.kind);
          if (heroRestKind === 'short') {
            if (!next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength + cramWisdom);
          } else {""",
"Elven Dreams rest")

# ---------------------------------------------------------------------------
# Replace combat resolution cases with supplement-aware mechanics.
# ---------------------------------------------------------------------------
combat_start=cmd.index("    case 'resolve_hero_attack': {")
combat_end=cmd.index("\n    case 'upsert_organization':",combat_start)
combat_cases="""    case 'resolve_hero_attack': {
      const attacker = nextCharacters.find(character => character.id === command.attackerId);
      if (!attacker || !partyIds.has(attacker.id)) throw new Error('Atacante não é um Herói da Companhia.');
      let actor = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      let target = nextState.combat.actors.find(item => item.actorId === command.targetActorId);
      if (!actor || !target || target.defeated) throw new Error('Alvo de combate inválido.');
      let hopeDice = 0;
      if (command.spendHope) {
        const spent = spendHope(attacker.sheet, 'strength');
        if (!spent.allowed) throw new Error('Esperança insuficiente.');
        updateHero(attacker.id, () => spent.sheet);
        hopeDice = command.inspired ? 2 : 1;
      }
      const freshAttacker = nextCharacters.find(character => character.id === attacker.id) || attacker;
      const terrain = String(nextState.journey.segments?.at(-1)?.terrain || '');
      const isNight = nextState.worldClock.hour < 6 || nextState.worldClock.hour >= 20;
      const escaping = command.special === 'escape';
      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired), { terrain, isNight, escaping });
      const weapon = result.weapon;
      const wantsSpecial = Boolean(command.special && ['heavy','pierce','defend'].includes(command.special));
      if (wantsSpecial && Number(result.roll.tengwarCount || 0) <= 0) throw new Error('O Dano Especial escolhido exige ao menos 1 ícone de Sucesso.');

      if (escaping) {
        const cleanNotes = (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:'));
        if (result.roll.success) {
          const linked = new Set(actor.engagedWith || []);
          const actors = nextState.combat.actors.map(item => item.actorId === actor!.actorId
            ? { ...item, engagedWith: [], stateNotes: [...cleanNotes, 'escaped-skirmish'] }
            : linked.has(item.actorId) ? { ...item, engagedWith: (item.engagedWith || []).filter(id => id !== actor!.actorId) } : item);
          nextState = { ...nextState, combat: { ...nextState.combat, actors, updatedAt: Date.now() }, updatedAt: Date.now() };
        } else {
          nextState = upsertCombatActor(nextState, { ...actor, stateNotes: cleanNotes });
        }
      } else {
        const damage = heroAttackDamage(freshAttacker.sheet, weapon, result.roll, command.special === 'heavy');
        const piercing = result.roll.success && attackIsPiercing(freshAttacker.sheet, weapon, result.roll, command.special === 'pierce');
        if (!result.ranged && !(actor.engagedWith || []).includes(target.actorId)) nextState = engageActors(nextState, actor.actorId, target.actorId);
        if (damage > 0) {
          if (target.actorType === 'hero' && partyIds.has(target.actorId)) {
            updateHero(target.actorId, sheet => adjustEndurance(sheet, -damage));
          } else {
            nextState = upsertCombatActor(nextState, { ...target, endurance: Math.max(0, target.endurance - damage), defeated: target.endurance - damage <= 0 });
          }
        }
        target = nextState.combat.actors.find(item => item.actorId === command.targetActorId) || target;
        if (result.roll.success && target.actorType !== 'hero') {
          const hateLoss = supplementEnemyHateLossOnSuccessfulAttack(freshAttacker.sheet, Number(result.roll.tengwarCount) || 0);
          if (hateLoss > 0 && Number(target.hate || 0) > 0) {
            target = { ...target, hate: Math.max(0, Number(target.hate || 0) - hateLoss) };
            nextState = upsertCombatActor(nextState, target);
          }
        }
        if (piercing) {
          if (target.actorType === 'hero' && partyIds.has(target.actorId)) {
            const targetHero = nextCharacters.find(character => character.id === target.actorId);
            if (targetHero) {
              const protection = rollHeroProtection(targetHero.sheet, weapon.injury, result.ranged);
              nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
              if (!protection.success) updateHero(target.actorId, sheet => applyHeroWound(sheet).sheet);
            }
          } else {
            const ill = supplementProtectionIllFavouredForTarget(freshAttacker.sheet, !result.ranged, piercing);
            const protection = rollActorProtection(target, weapon.injury, ill ? 'ill-favoured' : 'normal');
            nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
            if (!protection.success) nextState = upsertCombatActor(nextState, { ...target, wounded: true, defeated: target.actorType === 'adversary' ? true : target.defeated });
          }
        }
        if (command.special === 'defend' && result.roll.success && Number(result.roll.tengwarCount || 0) > 0) {
          const group = String(weapon.group || '').toLowerCase();
          const bonus = group.includes('lan') ? 3 : group.includes('espada') ? 2 : 1;
          nextState = upsertCombatActor(nextState, { ...actor, parry: Math.max(0, Number(actor.parry) || 0) + bonus, stateNotes: [...(actor.stateNotes || []), 'temp-round:Defender +' + String(bonus) + ' Bloqueio'] });
        }
        actor = nextState.combat.actors.find(item => item.actorId === command.attackerId) || actor;
        nextState = upsertCombatActor(nextState, { ...actor, stateNotes: (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:')) });
      }
      nextState = recordCombatRoll(nextState, { id: result.roll.id, actorId: actor.actorId, label: escaping ? 'Escapar em Escaramuça' : 'Ataque com ' + weapon.name, featDie: result.roll.featResult === 12 ? 'gandalf' : result.roll.featResult === 11 ? 'eye' : (result.roll.featResult || 1), successDice: result.roll.successResults || [], total: result.roll.total, targetNumber: result.roll.targetNumber, success: result.roll.success, tengwar: result.roll.tengwarCount || 0, createdAt: result.roll.timestamp });
      break;
    }

    case 'resolve_adversary_attack': {
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      let targetActor = nextState.combat.actors.find(item => item.actorId === command.targetCharacterId);
      const targetHero = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!attacker || !targetActor || !targetHero || !partyIds.has(targetHero.id)) throw new Error('Ataque de adversário inválido.');
      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0));
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;
      if (!command.ranged && targetActor.stance === 'defensive') dice = Math.max(0, dice - 1);
      dice = Math.max(0, dice + skirmishIncomingAttackDiceModifier(String(targetActor.stance || ''), Boolean(command.ranged)));
      if (!command.ranged && hasVirtue(targetHero.sheet, 'Cão da Floresta das Trevas') && targetHero.sheet.other.supplementSelections?.houndAvailable !== false) {
        const marker = 'hound-defended-round:' + String(Math.max(1, nextState.combat.round || 1));
        if (!(targetActor.stateNotes || []).includes(marker)) {
          dice = Math.max(0, dice - 1);
          targetActor = { ...targetActor, stateNotes: [...(targetActor.stateNotes || []), marker] };
          nextState = upsertCombatActor(nextState, targetActor);
        }
      }
      const terrain = String(nextState.journey.segments?.at(-1)?.terrain || '');
      const parry = Math.max(2, Number(targetHero.sheet.combat.parry) || 2) + supplementParryBonus(targetHero.sheet, { kind:'attack', terrain });
      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', false, false);
      nextState = recordCombatRoll(nextState, { id: roll.id, actorId: attacker.actorId, label: 'Ataque do adversário', featDie: roll.featResult === 12 ? 'gandalf' : roll.featResult === 11 ? 'eye' : (roll.featResult || 1), successDice: roll.successResults || [], total: roll.total, targetNumber: roll.targetNumber, success: roll.success, tengwar: roll.tengwarCount || 0, createdAt: roll.timestamp });
      if (roll.success) updateHero(targetHero.id, sheet => adjustEndurance(sheet, -Math.max(0, Number(command.damage) || 0)));
      const piercing = Boolean(roll.success) && (roll.featResult === 12 || (Number(roll.featResult) >= 10 && Number(roll.featResult) !== 11));
      if (piercing) {
        const protection = rollHeroProtection(targetHero.sheet, Math.max(2, Number(command.injury) || 2), Boolean(command.ranged));
        nextState = recordCombatRoll(nextState, { id: protection.id, actorId: targetHero.id, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
        if (!protection.success) updateHero(targetHero.id, sheet => applyHeroWound(sheet).sheet);
      }
      break;
    }

    case 'resolve_first_aid': {
      const healer = nextCharacters.find(character => character.id === command.healerCharacterId);
      const target = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!healer || !target || !partyIds.has(healer.id) || !partyIds.has(target.id)) throw new Error('Herói inválido para Primeiros Socorros.');
      const requestedSkill = command.skill === 'Música' ? 'Música' : 'Cura';
      if (requestedSkill === 'Música' && !canUseSongForFirstAid(healer.sheet)) throw new Error('Música só pode substituir Cura aqui com Música de Estancamento.');
      const skill = findSkill(healer.sheet, requestedSkill);
      const baseMode = healer.sheet.attributes.shadow >= healer.sheet.attributes.hopeMax ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
      const extra = supplementBonusDice(healer.sheet, { kind:'first-aid', skill: requestedSkill });
      const roll = rollOneRing(Math.max(0, Number(skill?.rank) || 0) + extra, 0, targetNumberForSkill(healer.sheet, requestedSkill), requestedSkill, baseMode, Boolean(healer.sheet.attributes.weary), Boolean(healer.sheet.attributes.miserable));
      updateHero(target.id, sheet => resolveFirstAid(sheet, roll, nextState.worldClock.day).sheet);
      break;
    }"""
cmd=cmd[:combat_start]+combat_cases+cmd[combat_end:]

write(rel,cmd)
print("PATCH_4939_COMMANDS_OK")
