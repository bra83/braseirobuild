from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4921.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

def replace_required(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# HOPE / CORE DICE
# Official TOR 2e: spend 1 Hope -> gain 1 Success Die; Inspired -> 2 Success Dice.
# It is NOT a flat Attribute bonus.
# ---------------------------------------------------------------------------
hope_rel = "lib/rules/hopeShadow.ts"
hope = read(hope_rel)
old = """export function spendHope(sheet: CharacterSheet, attribute: 'strength' | 'heart' | 'wits'): { sheet: CharacterSheet; bonus: number; allowed: boolean } {
  if (sheet.attributes.hopeCurrent <= 0) return { sheet, bonus: 0, allowed: false };
  const next = adjustHope(sheet, -1);
  return { sheet: next, bonus: Math.max(0, Number(sheet.attributes[attribute]) || 0), allowed: true };
}"""
new = """export function spendHope(sheet: CharacterSheet, _attribute: 'strength' | 'heart' | 'wits'): { sheet: CharacterSheet; bonusSuccessDice: number; allowed: boolean } {
  if (sheet.attributes.hopeCurrent <= 0) return { sheet, bonusSuccessDice: 0, allowed: false };
  const next = adjustHope(sheet, -1);
  return { sheet: next, bonusSuccessDice: 1, allowed: true };
}"""
hope = replace_required(hope, old, new, "spendHope attribute bonus")
write(hope_rel, hope)

app_rel = "App.tsx"
app = read(app_rel)
old = """  const handleSpendHope = async (characterId: string, attribute: 'strength' | 'heart' | 'wits'): Promise<number> => {
    const base = campaignRef.current;
    const character = characters.find(item => item.id === characterId);
    if (!base || !character || !(base.partyCharacterIds || []).includes(characterId)) return 0;
    const bonus = Math.max(0, Number(character.sheet.attributes[attribute]) || 0);
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'spend_hope', targetCharacterId: characterId, attribute,
      reason: `Esperança gasta antes de uma jogada de ${attribute}.`, source: 'O Um Anel 2e — gasto de Esperança',
    }]);
    if (mechanical.rejected.length) return 0;
    const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'gasto de Esperança');
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    return bonus;
  };"""
new = """  const handleSpendHope = async (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired = false): Promise<number> => {
    const base = campaignRef.current;
    const character = characters.find(item => item.id === characterId);
    if (!base || !character || !(base.partyCharacterIds || []).includes(characterId)) return 0;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'spend_hope', targetCharacterId: characterId, attribute,
      reason: `Esperança gasta antes de uma jogada de ${attribute}.`, source: 'O Um Anel 2e — gasto de Esperança',
    }]);
    if (mechanical.rejected.length) return 0;
    const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'gasto de Esperança');
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    return inspired ? 2 : 1;
  };"""
app = replace_required(app, old, new, "App Hope handler")
write(app_rel, app)

dice_rel = "components/DiceView.tsx"
dice = read(dice_rel)
dice = replace_required(
    dice,
    "  onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits') => Promise<number>;",
    "  onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired?: boolean) => Promise<number>;",
    "DiceView Hope prop",
)
dice = replace_required(
    dice,
    "  const [hopeBonus, setHopeBonus] = useState(0);",
    "  const [hopeBonus, setHopeBonus] = useState(0);\n  const [inspiredForRoll, setInspiredForRoll] = useState(false);",
    "DiceView inspired state",
)
dice = replace_required(
    dice,
    """    setHopeSpentForRoll(false);
    setHopeBonus(0);
    setOneRingModifier(0);""",
    """    setHopeSpentForRoll(false);
    setHopeBonus(0);
    setInspiredForRoll(false);
    setOneRingModifier(0);""",
    "DiceView reset Hope",
)
old = """  const handleSpendHope = async () => {
    if (!selectedCharacter || hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;
    const attribute = skillAttribute(selectedCharacter.sheet, oneRingSkillName);
    const bonus = await onSpendHope(selectedCharacter.id, attribute);
    if (bonus > 0) {
      setOneRingModifier(current => current + bonus);
      setHopeBonus(bonus);
      setHopeSpentForRoll(true);
    }
  };"""
new = """  const handleSpendHope = async () => {
    if (!selectedCharacter || hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;
    const attribute = skillAttribute(selectedCharacter.sheet, oneRingSkillName);
    const bonusDice = await onSpendHope(selectedCharacter.id, attribute, inspiredForRoll);
    if (bonusDice > 0) {
      setOneRingSuccessCount(current => current + bonusDice);
      setHopeBonus(bonusDice);
      setHopeSpentForRoll(true);
    }
  };"""
dice = replace_required(dice, old, new, "DiceView spend Hope")

# Arrasado was displayed but not passed into the actual roll interpreter.
dice = replace_required(
    dice,
    "    const weary = Boolean(selectedCharacter?.sheet.attributes.weary);",
    "    const weary = Boolean(selectedCharacter?.sheet.attributes.weary);\n    const miserable = Boolean(selectedCharacter?.sheet.attributes.miserable);",
    "DiceView miserable state",
)
dice = replace_required(
    dice,
    """          {
            engine: observed.engine || 'braseiro-physical-dice', mode: 'physical_3d',
            seedFeat: observed.physics_seed_feat, seedSuccess: observed.physics_seed_success,
            stepsFeat: observed.physics_steps_feat, stepsSuccess: observed.physics_steps_success,
            forcedSettle: Boolean(observed.forced_settle_feat || observed.forced_settle_success),
          },
        );""",
    """          {
            engine: observed.engine || 'braseiro-physical-dice', mode: 'physical_3d',
            seedFeat: observed.physics_seed_feat, seedSuccess: observed.physics_seed_success,
            stepsFeat: observed.physics_steps_feat, stepsSuccess: observed.physics_steps_success,
            forcedSettle: Boolean(observed.forced_settle_feat || observed.forced_settle_success),
          },
          miserable,
        );""",
    "physical Arrasado",
)
dice = replace_required(
    dice,
    "        result = rollOneRing(oneRingSuccessCount, oneRingModifier, oneRingTargetNumber, oneRingSkillName, rollMode, weary);",
    "        result = rollOneRing(oneRingSuccessCount, oneRingModifier, oneRingTargetNumber, oneRingSkillName, rollMode, weary, miserable);",
    "fallback Arrasado",
)
dice = replace_required(
    dice,
    """      if (hopeBonus) setOneRingModifier(current => current - hopeBonus);
      setHopeBonus(0);
      setHopeSpentForRoll(false);""",
    """      if (hopeBonus) setOneRingSuccessCount(current => Math.max(0, current - hopeBonus));
      setHopeBonus(0);
      setHopeSpentForRoll(false);
      setInspiredForRoll(false);""",
    "remove Hope dice after roll",
)

old_button = """            <button type="button" onClick={() => void handleSpendHope()} disabled={hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0} className="rounded border border-amber-700 bg-amber-950/25 px-3 py-3 text-sm font-bold text-amber-200 disabled:opacity-40">
              {hopeSpentForRoll ? 'Esperança aplicada' : `Gastar 1 Esperança (+${selectedCharacter.sheet.attributes[skillAttribute(selectedCharacter.sheet, oneRingSkillName)]})`}
            </button>"""
new_button = """            <div className="rounded border border-amber-900/70 bg-amber-950/15 p-2">
              <label className="mb-2 flex items-center gap-2 text-xs text-amber-100">
                <input type="checkbox" checked={inspiredForRoll} disabled={hopeSpentForRoll} onChange={event => setInspiredForRoll(event.target.checked)} />
                Inspirado nesta jogada
              </label>
              <button type="button" onClick={() => void handleSpendHope()} disabled={hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0} className="w-full rounded border border-amber-700 bg-amber-950/25 px-3 py-3 text-sm font-bold text-amber-200 disabled:opacity-40">
                {hopeSpentForRoll ? `Esperança aplicada (+${hopeBonus}d)` : `Gastar 1 Esperança (+${inspiredForRoll ? 2 : 1}d)`}
              </button>
            </div>"""
dice = replace_required(dice, old_button, new_button, "Hope button")
write(dice_rel, dice)

# ---------------------------------------------------------------------------
# JOURNEY: all 4 roles, terrain duration, world clock integration.
# Official TOR 2e base duration: 1 day/hex +1 day for difficult terrain hexes.
# Initial placement consumes no travel time.
# ---------------------------------------------------------------------------
journey_rel = "lib/rules/journey.ts"
journey = """import type { MasterState, JourneyRole } from '../master/types';
import { advanceWorldClock } from '../master/worldClock';

export const JOURNEY_ROLE_SKILLS: Record<JourneyRole, string> = {
  guide: 'Viagem',
  scout: 'Exploração',
  hunter: 'Caçada',
  lookout: 'Vigilância',
};

export const JOURNEY_ROLE_LABELS: Record<JourneyRole, string> = {
  guide: 'Guia',
  scout: 'Batedor',
  hunter: 'Caçador',
  lookout: 'Vigia',
};

const DIFFICULT_TERRAINS = new Set([
  'hills', 'hill', 'forest', 'dense_forest', 'woods', 'wood',
  'marsh', 'swamp', 'waste', 'ruins', 'mountains', 'mountain',
  'water',
]);

function terrainKey(value?: string): string {
  return String(value || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase().trim().replace(/[ -]+/g, '_');
}

export function journeyTravelDaysForTerrain(terrain?: string): number {
  return DIFFICULT_TERRAINS.has(terrainKey(terrain)) ? 2 : 1;
}

export function validateJourneyRoles(roles: Partial<Record<JourneyRole, string>>, partyIds: string[]): string[] {
  const errors: string[] = [];
  const party = new Set(partyIds);
  const required: JourneyRole[] = ['guide', 'scout', 'hunter', 'lookout'];
  for (const role of required) {
    const characterId = roles[role];
    if (!characterId) errors.push(`A Companhia precisa definir ${JOURNEY_ROLE_LABELS[role]} (${JOURNEY_ROLE_SKILLS[role]}).`);
    else if (!party.has(characterId)) errors.push(`${JOURNEY_ROLE_LABELS[role]}: personagem fora da Companhia.`);
  }
  return errors;
}

export function startJourney(state: MasterState, destinationHexId: string | null, roles: Partial<Record<JourneyRole, string>>): MasterState {
  return {
    ...state,
    activeMode: 'journey',
    company: { ...state.company, journeyRoles: { ...roles } },
    journey: {
      status: 'planned',
      originHexId: state.position.currentHexId,
      destinationHexId,
      startedAt: Date.now(),
      day: 0,
      segments: [],
      pendingEvent: null,
    },
    updatedAt: Date.now(),
  };
}

export function recordJourneyMove(state: MasterState, toHexId: string, locationName: string, distanceKm?: number, terrain?: string): MasterState {
  const fromHexId = state.position.currentHexId;
  const now = Date.now();
  const isTravel = Boolean(fromHexId && fromHexId !== toHexId);
  const travelDays = isTravel ? journeyTravelDaysForTerrain(terrain) : 0;
  const startWorldMinute = Math.max(0, Number(state.worldClock.elapsedMinutes) || 0);
  const advanced = travelDays > 0 ? advanceWorldClock(state.worldClock, travelDays * 1440) : { clock: state.worldClock };
  const completedWorldMinute = Math.max(0, Number(advanced.clock.elapsedMinutes) || startWorldMinute);

  return {
    ...state,
    activeMode: state.journey.status === 'idle' ? 'adventure' : 'journey',
    worldClock: advanced.clock,
    position: {
      currentHexId: toHexId,
      locationName,
      routeHexIds: [...state.position.routeHexIds, toHexId].filter((id, index, all) => index === 0 || id !== all[index - 1]),
      lastMovedAt: now,
    },
    journey: state.journey.status === 'idle' ? state.journey : {
      ...state.journey,
      status: 'travelling',
      day: Math.max(0, Number(state.journey.day) || 0) + travelDays,
      segments: [...state.journey.segments, {
        id: `segment_${now}_${Math.random().toString(36).slice(2, 6)}`,
        fromHexId,
        toHexId,
        distanceKm,
        terrain,
        travelDays,
        startedAtWorldMinute: startWorldMinute,
        completedAtWorldMinute,
        startedAt: now,
        completedAt: now,
      }],
    },
    updatedAt: now,
  };
}

export function completeJourney(state: MasterState): MasterState {
  return {
    ...state,
    activeMode: 'adventure',
    journey: { ...state.journey, status: 'completed', destinationHexId: state.position.currentHexId, pendingEvent: null },
    updatedAt: Date.now(),
  };
}
"""
write(journey_rel, journey)

# Types: backwards-compatible fields.
types_rel = "lib/master/types.ts"
types = read(types_rel)
types = replace_required(
    types,
    """  terrain?: string;
  startedAt: number;
  completedAt: number;""",
    """  terrain?: string;
  travelDays?: number;
  startedAtWorldMinute?: number;
  completedAtWorldMinute?: number;
  startedAt: number;
  completedAt: number;""",
    "JourneySegment time fields",
)
types = replace_required(
    types,
    """export interface CouncilState {
  status: 'idle' | 'introduction' | 'interaction' | 'resolved';
  id?: string;
  objective?: string;
  tolerance: number;
  successes: number;
  failures: number;
  participants: string[];
  interlocutorNpcIds: string[];
  outcome?: 'success' | 'partial' | 'failure';
}""",
    """export interface CouncilState {
  status: 'idle' | 'introduction' | 'interaction' | 'resolved';
  id?: string;
  objective?: string;
  /** Legacy field kept for stored campaigns. In 4.9.21 it mirrors Resistance. */
  tolerance: number;
  resistance?: number;
  timeLimit?: number;
  testsUsed?: number;
  introductionSucceeded?: boolean;
  introductionSuccessIcons?: number;
  disasterRisk?: boolean;
  successes: number;
  failures: number;
  participants: string[];
  interlocutorNpcIds: string[];
  outcome?: 'success' | 'partial' | 'failure';
}""",
    "CouncilState",
)
types = replace_required(
    types,
    "| { type: 'record_council_result'; success: boolean; reason: string }",
    "| { type: 'record_council_result'; success: boolean; successIcons?: number; introduction?: boolean; reason: string }",
    "Council command",
)
write(types_rel, types)

# State defaults/migration for Council.
state_rel = "lib/master/state.ts"
state = read(state_rel)
state = replace_required(
    state,
    "    council: { status: 'idle', tolerance: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },",
    "    council: { status: 'idle', tolerance: 0, resistance: 0, timeLimit: 0, testsUsed: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },",
    "Council default",
)
old = "    council: { ...base.council, ...(incoming.council || {}), participants: incoming.council?.participants || [], interlocutorNpcIds: incoming.council?.interlocutorNpcIds || [] },"
new = """    council: {
      ...base.council,
      ...(incoming.council || {}),
      resistance: Math.max(0, Number(incoming.council?.resistance ?? incoming.council?.tolerance ?? 0) || 0),
      timeLimit: Math.max(0, Number(incoming.council?.timeLimit ?? incoming.council?.tolerance ?? 0) || 0),
      testsUsed: Math.max(0, Number(incoming.council?.testsUsed || 0) || 0),
      participants: incoming.council?.participants || [],
      interlocutorNpcIds: incoming.council?.interlocutorNpcIds || [],
    },"""
state = replace_required(state, old, new, "Council migration")
write(state_rel, state)

# Council: Resistance + Introduction-derived time limit, no failure-count-as-tolerance.
council_rel = "lib/rules/council.ts"
council = """import type { MasterState } from '../master/types';

export function startCouncil(state: MasterState, objective: string, resistance: number, participants: string[], npcIds: string[]): MasterState {
  const safeResistance = Math.max(1, Math.trunc(Number(resistance) || 1));
  return {
    ...state,
    activeMode: 'council',
    council: {
      status: 'introduction',
      id: `council_${Date.now()}`,
      objective,
      tolerance: safeResistance,
      resistance: safeResistance,
      timeLimit: safeResistance,
      testsUsed: 0,
      introductionSuccessIcons: 0,
      disasterRisk: false,
      successes: 0,
      failures: 0,
      participants: Array.from(new Set(participants)),
      interlocutorNpcIds: Array.from(new Set(npcIds)),
    },
    updatedAt: Date.now(),
  };
}

export function recordCouncilResult(state: MasterState, success: boolean, successIcons = 0, introduction = false): MasterState {
  const council = { ...state.council };
  const resistance = Math.max(1, Number(council.resistance ?? council.tolerance) || 1);
  const icons = Math.max(0, Math.trunc(Number(successIcons) || 0));
  const isIntroduction = introduction || council.status === 'introduction';

  if (isIntroduction) {
    council.introductionSucceeded = success;
    council.introductionSuccessIcons = success ? icons : 0;
    council.disasterRisk = !success;
    council.timeLimit = resistance + (success ? icons : 0);
    council.status = 'interaction';
    return { ...state, council, updatedAt: Date.now() };
  }

  if (council.status !== 'interaction') return state;
  council.testsUsed = Math.max(0, Number(council.testsUsed || 0)) + 1;
  if (success) council.successes += 1 + icons;
  else council.failures += 1;

  if (council.successes >= resistance) {
    council.status = 'resolved';
    council.outcome = 'success';
  } else if (council.testsUsed >= Math.max(1, Number(council.timeLimit || resistance))) {
    council.status = 'resolved';
    council.outcome = council.successes > 0 ? 'partial' : 'failure';
  }
  return { ...state, council, updatedAt: Date.now() };
}

export function resolveCouncil(state: MasterState, outcome: 'success' | 'partial' | 'failure'): MasterState {
  return { ...state, activeMode: 'adventure', council: { ...state.council, status: 'resolved', outcome }, updatedAt: Date.now() };
}
"""
write(council_rel, council)

# Master commands: validate all Journey roles; simulate living world for travel time;
# pass Council success icons; Fellowship phase recovers Hope equal to Heart.
commands_rel = "lib/master/commands.ts"
commands = read(commands_rel)
commands = replace_required(
    commands,
    "import { completeJourney, recordJourneyMove, startJourney } from '../rules/journey';",
    "import { completeJourney, journeyTravelDaysForTerrain, recordJourneyMove, startJourney, validateJourneyRoles } from '../rules/journey';",
    "journey imports",
)
old = """    case 'move_party':
      if (!command.toHexId) throw new Error('Comando rejeitado: hex de destino ausente.');
      nextState = recordJourneyMove(nextState, command.toHexId, command.locationName || nextState.position.locationName, command.distanceKm, command.terrain);
      nextCampaign.currentLocation = nextState.position.locationName;
      nextState.scene = { ...nextState.scene, locationName: nextState.position.locationName, updatedAt: Date.now() };
      break;"""
new = """    case 'move_party': {
      if (!command.toHexId) throw new Error('Comando rejeitado: hex de destino ausente.');
      const fromHexId = nextState.position.currentHexId;
      const moved = Boolean(fromHexId && fromHexId !== command.toHexId);
      const travelDays = moved ? journeyTravelDaysForTerrain(command.terrain) : 0;
      nextState = recordJourneyMove(nextState, command.toHexId, command.locationName || nextState.position.locationName, command.distanceKm, command.terrain);
      if (travelDays > 0) {
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, travelDays * 1440);
        nextState = simulated.master;
        nextNpcs = simulated.npcs;
        appendLivingActivity('journey', 'Trecho de jornada', `${travelDays} dia(s) para atravessar ${command.terrain || 'terreno comum'} até ${nextState.position.locationName}.`, command.toHexId);
      }
      nextCampaign.currentLocation = nextState.position.locationName;
      nextState.scene = { ...nextState.scene, locationName: nextState.position.locationName, updatedAt: Date.now() };
      break;
    }"""
commands = replace_required(commands, old, new, "move_party travel time")

old = """    case 'start_journey':
      nextState = startJourney(nextState, command.destinationHexId, command.roles || nextState.company.journeyRoles);
      nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;"""
new = """    case 'start_journey': {
      const roles = command.roles || nextState.company.journeyRoles;
      const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
      if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
      nextState = startJourney(nextState, command.destinationHexId, roles);
      nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    }"""
commands = replace_required(commands, old, new, "start_journey roles")

commands = replace_required(
    commands,
    "    case 'record_council_result': nextState = recordCouncilResult(nextState, command.success); break;",
    "    case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;",
    "Council result command",
)

old = """    case 'start_fellowship_phase':
      nextState = startFellowshipPhase(nextState, command.locationId);
      nextState.scene = modeScene(nextState, 'fellowship', 'Fase de Sociedade', 'Recuperar-se e preparar o futuro', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;"""
new = """    case 'start_fellowship_phase':
      nextState = startFellowshipPhase(nextState, command.locationId);
      for (const characterId of partyIds) {
        updateHero(characterId, sheet => adjustHope(sheet, Math.max(0, Number(sheet.attributes.heart) || 0)));
      }
      nextState.scene = modeScene(nextState, 'fellowship', 'Fase de Sociedade', 'Recuperar-se e preparar o futuro', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;"""
commands = replace_required(commands, old, new, "Fellowship Hope recovery")
write(commands_rel, commands)

# ---------------------------------------------------------------------------
# MAP UI: route estimate uses the same canonical terrain/day function.
# ---------------------------------------------------------------------------
map_rel = "components/EriadorMap.tsx"
mp = read(map_rel)
import_anchor = "import type { Character } from '../lib/db';"
mp = replace_required(
    mp,
    import_anchor,
    import_anchor + "\nimport { journeyTravelDaysForTerrain } from '../lib/rules/journey';",
    "map journey import",
)

# Replace planned-route message with real day estimate.
old = """    setMessage(`Rota planejada com ${route.length} etapa(s). Use “Avançar 1 hex pela rota” para viajar etapa por etapa.`);"""
new = """    const estimatedDays = route.reduce((sum, hexId) => sum + journeyTravelDaysForTerrain(byId.get(hexId)?.terrain), 0);
    setMessage(`Rota planejada com ${route.length} etapa(s), estimativa de ${estimatedDays} dia(s) pelas regras de terreno. Use “Avançar 1 hex pela rota” para viajar etapa por etapa.`);"""
if old in mp:
    mp = mp.replace(old, new, 1)
else:
    # 4.9.20 wording variant.
    old2 = """    setMessage(`Rota planejada com ${route.length} etapa(s). Toque em “Avançar 1 hex pela rota” para viajar etapa por etapa.`);"""
    mp = replace_required(mp, old2, new, "route estimate")
write(map_rel, mp)

# ---------------------------------------------------------------------------
# VERSION 4.9.21
# ---------------------------------------------------------------------------
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.21"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.20"', '"version": "4.9.21"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.20'", "APP_VERSION = '4.9.21'", version)
version = re.sub(r"APP_VERSION_CODE = 49020", "APP_VERSION_CODE = 49021", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49020", "versionCode = 49021", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.20"', 'versionName = "4.9.21"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49020&immersive=1", "index.html?v=49021&immersive=1")
write(main_rel, main)

# ---------------------------------------------------------------------------
# HARD GATES
# ---------------------------------------------------------------------------
hope = read(hope_rel)
dice = read(dice_rel)
journey = read(journey_rel)
types = read(types_rel)
state = read(state_rel)
council = read(council_rel)
commands = read(commands_rel)
mp = read(map_rel)
db = read("lib/db.ts")
models = read("lib/geminiModels.ts")
main = read(main_rel)

checks = {
    "hope is success die not attribute": "bonusSuccessDice: 1" in hope and "Number(sheet.attributes[attribute])" not in hope,
    "dice Hope adds d6": "setOneRingSuccessCount(current => current + bonusDice)" in dice and "Gastar 1 Esperança" in dice,
    "inspired Hope doubles": "inspiredForRoll ? 2 : 1" in dice,
    "miserable reaches roll engine": "miserable," in dice and "rollMode, weary, miserable" in dice,
    "journey all roles": "const required: JourneyRole[] = ['guide', 'scout', 'hunter', 'lookout']" in journey,
    "journey terrain time": "journeyTravelDaysForTerrain" in journey and "DIFFICULT_TERRAINS" in journey,
    "journey clock advances": "advanceWorldClock(state.worldClock, travelDays * 1440)" in journey,
    "journey segment records days": "travelDays," in journey and "completedAtWorldMinute" in journey,
    "living world advances with travel": "simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, travelDays * 1440)" in commands,
    "map route estimates days": "estimativa de" in mp and "journeyTravelDaysForTerrain" in mp,
    "council resistance": "resistance" in council and "timeLimit" in council,
    "council intro drives time limit": "council.timeLimit = resistance + (success ? icons : 0)" in council,
    "council no fail-count tolerance": "council.failures >= council.tolerance" not in council,
    "fellowship recovers heart Hope": "adjustHope(sheet, Math.max(0, Number(sheet.attributes.heart)" in commands,
    "one Master model preserved": "const TEXT_KEY = 'braseiro.geminiModel.text'" in models,
    "network retry preserved": "isTransientGeminiNetworkError" in main,
    "party anchor preserved": 'data-party-world-anchored="true"' in mp,
    "db identity preserved": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
    "version": "4.9.21" in read(version_rel) and "49021" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4921_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.21_MECHANICS_PASS_OK")
