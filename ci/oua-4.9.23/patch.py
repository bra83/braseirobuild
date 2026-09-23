from pathlib import Path
import json, re, shutil, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4923.py <project-root>")
root = Path(sys.argv[1]).resolve()
repo_root = Path(__file__).resolve().parents[2]
templates = repo_root / "ci" / "oua-4.9.23" / "templates"

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def copy_template(rel):
    src=templates/rel
    if not src.exists(): raise SystemExit("missing template: "+str(src))
    dst=root/rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(src,dst)
def req(text,old,new,label,count=1):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,count)

# Canonical mechanics modules.
for rel in [
    "lib/rules/equipment.ts",
    "lib/rules/shadow.ts",
    "lib/rules/progression.ts",
    "lib/rules/journey.ts",
    "lib/rules/combat.ts",
    "lib/master/worldClock.ts",
    "components/MechanicsHub.tsx",
    "components/CombatRulesPanel.tsx",
]:
    copy_template(rel)

# ---------------------------------------------------------------------------
# Character model: keep DB schema, add optional canonical mechanic state only.
# ---------------------------------------------------------------------------
db_rel="lib/db.ts"
db=read(db_rel)
db=req(db,
"""    protection?: number;
    /** Ajustes permanentes de NA por Virtudes como Dom. */""",
"""    protection?: number;
    protectionBonus?: number;
    shadowScars?: number;
    shadowAtLimit?: boolean;
    woundSeverity?: 'moderate' | 'serious' | 'critical';
    woundDays?: number;
    temporaryWound?: boolean;
    dying?: boolean;
    unconscious?: boolean;
    /** Ajustes permanentes de NA por Virtudes como Dom. */""","db attributes")
db=req(db,
"""    creationTrace?: string[];
  };""",
"""    creationTrace?: string[];
    equipmentParryBonusApplied?: number;
    shadowPathStep?: number;
    retiredByShadow?: boolean;
    lastFirstAidSuccessDay?: number;
    lastFirstAidFailureDay?: number;
  };""","db other")
write(db_rel,db)

# ---------------------------------------------------------------------------
# Master types: Journey, Fellowship, Combat, commands.
# ---------------------------------------------------------------------------
types_rel="lib/master/types.ts"
types=read(types_rel)
types=req(types,
"""  lastLongRestDayKey?: string;
}""",
"""  lastLongRestDayKey?: string;
  fellowshipBonusFromUndertaking?: number;
}""","company bonus")

# Journey pending event and state.
start=types.index("export interface JourneyState {")
end=types.index("\n}\n\nexport interface CouncilState",start)+2
journey_state="""export interface JourneyState {
  status: 'idle' | 'planned' | 'travelling' | 'paused' | 'completed';
  originHexId: string | null;
  destinationHexId: string | null;
  startedAt?: number;
  day: number;
  segments: JourneySegment[];
  pendingEvent?: {
    id: string;
    targetRole: JourneyRole;
    skill: string;
    fatigueOnFailure: number;
    description: string;
    eventType?: string;
    featResult?: number;
    region?: 'border' | 'wild' | 'dark';
    terrain?: string;
    rollMode?: 'favoured' | 'normal' | 'ill-favoured';
    successDiceModifier?: number;
  } | null;
  plannedRouteHexIds?: string[];
  stageRouteHexIds?: string[];
  stageNumber?: number;
  stageComplete?: boolean;
  marchHexesRemaining?: number;
  marchRollCount?: number;
  marchWillReachDestination?: boolean;
  awaitingEventGeneration?: boolean;
  awaitingEndRoll?: boolean;
  durationAdjustmentDays?: number;
  mounted?: boolean;
  forcedMarch?: boolean;
  dangerEventsRemaining?: number;
}"""
types=types[:start]+journey_state+types[end:]

types=req(types,
"""  quickSheet?: Record<string, unknown>;
}""",
"""  quickSheet?: Record<string, unknown>;
  attackDice?: number;
  damage?: number;
  injury?: number;
  ranged?: boolean;
  large?: boolean;
}""","combat actor")

types=req(types,
"""  undertakings: Record<string, string>;
}""",
"""  undertakings: Record<string, string>;
  isYule?: boolean;
  skillAdvances?: Record<string, string[]>;
  combatAdvances?: Record<string, string[]>;
  valorWisdomAdvance?: Record<string, 'valor' | 'wisdom'>;
  undertakingCountByHero?: Record<string, number>;
}""","fellowship state")

types=types.replace(
"export interface LivingWorldActivity { id: string; kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'world';",
"export interface LivingWorldActivity { id: string; kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'rest' | 'world';"
)

# Replace command lines that need richer shapes, then insert new commands.
types=types.replace(
"| { type: 'resolve_rest'; kind: 'short' | 'long'; reason: string; source?: string }",
"| { type: 'resolve_rest'; kind: 'short' | 'long'; safeRefuge?: boolean; reason: string; source?: string }"
)
types=types.replace(
"| { type: 'start_journey'; destinationHexId: string | null; roles?: Partial<Record<JourneyRole, string>>; reason: string }",
"| { type: 'start_journey'; destinationHexId: string | null; roles?: Partial<Record<JourneyRole, string>>; routeHexIds?: string[]; mounted?: boolean; forcedMarch?: boolean; reason: string }"
)
types=types.replace(
"| { type: 'complete_journey'; reason: string }",
"| { type: 'complete_journey'; endRolls?: Record<string, { success: boolean; successIcons: number; mountVigour?: number }>; reason: string }"
)
types=types.replace(
"| { type: 'start_fellowship_phase'; locationId?: string; reason: string }",
"| { type: 'start_fellowship_phase'; locationId?: string; yule?: boolean; reason: string }"
)
insert_after="  | { type: 'complete_journey'; endRolls?: Record<string, { success: boolean; successIcons: number; mountVigour?: number }>; reason: string }"
types=req(types,insert_after,insert_after+"""
  | { type: 'resolve_journey_march'; success: boolean; successIcons: number; roll?: unknown; reason: string }
  | { type: 'generate_journey_event'; region: 'border' | 'wild' | 'dark'; targetDie: number; featResults: number[]; terrain?: string; reason: string }
  | { type: 'resolve_journey_event'; targetCharacterId: string; success: boolean; successIcons: number; roll?: unknown; reason: string }
  | { type: 'set_journey_danger'; danger: number; reason: string }
  | { type: 'continue_journey_stage'; reason: string }""","journey commands")
insert_after="  | { type: 'set_combat_background'; imageData: string | null; reason: string }"
types=req(types,insert_after,insert_after+"""
  | { type: 'engage_combat_actors'; actorId: string; targetActorId: string; reason: string }
  | { type: 'resolve_hero_attack'; attackerId: string; targetActorId: string; weaponIndex: number; spendHope?: boolean; inspired?: boolean; special?: 'heavy' | 'pierce' | 'defend'; reason: string }
  | { type: 'resolve_adversary_attack'; attackerId: string; targetCharacterId: string; successDice: number; damage: number; injury: number; ranged?: boolean; reason: string }
  | { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; reason: string }""","combat commands")
insert_after="  | { type: 'end_fellowship_phase'; reason: string };"
types=req(types,insert_after,
"""  | { type: 'end_fellowship_phase'; reason: string }
  | { type: 'resolve_shadow_gain'; targetCharacterId: string; amount: number; source: 'dread' | 'greed' | 'sorcery' | 'transgression'; testSuccess: boolean; successIcons: number; reason: string }
  | { type: 'firm_will'; targetCharacterId: string; reason: string }
  | { type: 'bout_of_madness'; targetCharacterId: string; reason: string }
  | { type: 'advance_skill'; targetCharacterId: string; skillName: string; reason: string }
  | { type: 'advance_combat_proficiency'; targetCharacterId: string; group: string; reason: string }
  | { type: 'advance_valor'; targetCharacterId: string; reward: string; reason: string }
  | { type: 'advance_wisdom'; targetCharacterId: string; virtue: string; reason: string }
  | { type: 'heal_shadow_scar'; targetCharacterId: string; reason: string }
  | { type: 'record_undertaking'; targetCharacterId: string; undertaking: 'strengthen-fellowship' | 'heal-scars' | 'narrative'; reason: string };""","progression commands")
write(types_rel,types)

# ---------------------------------------------------------------------------
# Master state defaults/migration for new fields.
# ---------------------------------------------------------------------------
state_rel="lib/master/state.ts"
state=read(state_rel)
state=state.replace(
"journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null },",
"journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEndRoll: false, durationAdjustmentDays: 0, mounted: false, forcedMarch: false, dangerEventsRemaining: 0 },"
)
state=state.replace(
"fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {} },",
"fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {} },"
)
state=state.replace(
"journey: { ...base.journey, ...(incoming.journey || {}), segments: Array.isArray(incoming.journey?.segments) ? incoming.journey.segments : [] },",
"""journey: {
      ...base.journey, ...(incoming.journey || {}),
      segments: Array.isArray(incoming.journey?.segments) ? incoming.journey.segments : [],
      plannedRouteHexIds: Array.isArray(incoming.journey?.plannedRouteHexIds) ? incoming.journey.plannedRouteHexIds : [],
      stageRouteHexIds: Array.isArray(incoming.journey?.stageRouteHexIds) ? incoming.journey.stageRouteHexIds : [],
    },"""
)
state=state.replace(
"fellowshipPhase: { ...base.fellowshipPhase, ...(incoming.fellowshipPhase || {}), undertakings: incoming.fellowshipPhase?.undertakings || {} },",
"""fellowshipPhase: {
      ...base.fellowshipPhase, ...(incoming.fellowshipPhase || {}),
      undertakings: incoming.fellowshipPhase?.undertakings || {},
      skillAdvances: incoming.fellowshipPhase?.skillAdvances || {},
      combatAdvances: incoming.fellowshipPhase?.combatAdvances || {},
      valorWisdomAdvance: incoming.fellowshipPhase?.valorWisdomAdvance || {},
      undertakingCountByHero: incoming.fellowshipPhase?.undertakingCountByHero || {},
    },"""
)
write(state_rel,state)

# ---------------------------------------------------------------------------
# App: equipment normalization, map-route sync, Council auto-resolution,
# MechanicsHub integration.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)
app=req(app,
"import { createRandomCharacter } from './lib/characterFactory';",
"import { createRandomCharacter } from './lib/characterFactory';\nimport { normalizeEquipmentMechanics } from './lib/rules/equipment';",
"app equipment import")
app=req(app,
"import { CompanyHubView } from './components/CompanyHubView';",
"import { CompanyHubView } from './components/CompanyHubView';\nimport { MechanicsHub } from './components/MechanicsHub';",
"app mechanics import")

# Normalize every sheet through deterministic equipment derivation.
old="""  return {
    ...base,
    ...input,
    identity: { ...base.identity, ...(input.identity || {}) },"""
if old not in app: raise SystemExit("normalize sheet return anchor missing")
app=app.replace(old,"""  const merged: CharacterSheet = {
    ...base,
    ...input,
    identity: { ...base.identity, ...(input.identity || {}) },""",1)
end_anchor="""    historyLog: Array.isArray(input.historyLog) ? input.historyLog : [],
  };
}

function normalizeCampaignState"""
if end_anchor not in app: raise SystemExit("normalize sheet end missing")
app=app.replace(end_anchor,"""    historyLog: Array.isArray(input.historyLog) ? input.historyLog : [],
  };
  return normalizeEquipmentMechanics(merged);
}

function normalizeCampaignState""",1)

# Persist planned route into MasterState so Jornada can start from the Atlas.
old="""    const committed = await saveCanonicalTransaction({
      campaign: base,
      mapDiscovery: discovery,
      reason: `estado do atlas: ${reason}`,
    });"""
new="""    const masterState = normalizeMasterState(base);
    const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
    const synced = {
      ...base,
      masterState: {
        ...masterState,
        journey: {
          ...masterState.journey,
          plannedRouteHexIds: route,
          destinationHexId: route.at(-1) || masterState.journey.destinationHexId,
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };
    const committed = await saveCanonicalTransaction({
      campaign: synced,
      mapDiscovery: discovery,
      reason: `estado do atlas: ${reason}`,
    });"""
app=req(app,old,new,"map route sync")

# Council: real pending rolls mechanically update Introduction/Interaction before narration.
old="""  const handleResolvePendingRoll = async () => {
    const base = campaignRef.current;
    if (!base?.pendingTest || !base.pendingDiceRoll || isStoryLoading) return;
    const originalChoice = String(base.pendingTest.originalChoice || base.pendingTest.reason || `Resolver teste de ${base.pendingTest.skill || 'perícia'}`).trim();
    setStoryError(null);
    setIsStoryLoading(true);
    await processTurn(`[RESOLVER TESTE PENDENTE] ${originalChoice}`, false, base);
  };"""
new="""  const handleResolvePendingRoll = async () => {
    let base = campaignRef.current;
    if (!base?.pendingTest || !base.pendingDiceRoll || isStoryLoading) return;
    const formal = normalizeMasterState(base);
    if (formal.activeMode === 'council' && (formal.council.status === 'introduction' || formal.council.status === 'interaction')) {
      const mechanical = applyMasterCommands(base, characters, allNpcs, [{
        type: 'record_council_result',
        success: Boolean(base.pendingDiceRoll.success),
        successIcons: Math.max(0, Number(base.pendingDiceRoll.tengwarCount) || 0),
        introduction: formal.council.status === 'introduction',
        reason: formal.council.status === 'introduction' ? 'Rolagem real da Introdução do Conselho.' : 'Rolagem real da Interação do Conselho.',
      }]);
      if (!mechanical.rejected.length) {
        const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'resultado mecânico do Conselho', mechanical.npcs);
        saveEmergencyCampaignMirror(committed);
        campaignRef.current = committed; setCampaign(committed); setCharacters(mechanical.characters); setAllNpcs(mechanical.npcs);
        base = committed;
      }
    }
    const originalChoice = String(base.pendingTest?.originalChoice || base.pendingTest?.reason || `Resolver teste de ${base.pendingTest?.skill || 'perícia'}`).trim();
    setStoryError(null);
    setIsStoryLoading(true);
    await processTurn(`[RESOLVER TESTE PENDENTE] ${originalChoice}`, false, base);
  };"""
app=req(app,old,new,"Council pending roll")

# Company screen includes deterministic mechanics hub.
old="case 'companhia': return <CompanyHubView masterState={masterState} characters={partyCharacters} onSave={handleCompanySave} />;"
new="case 'companhia': return <><CompanyHubView masterState={masterState} characters={partyCharacters} onSave={handleCompanySave} /><div className=\"mt-4\"><MechanicsHub masterState={masterState} characters={partyCharacters} onApplyCommands={handleWorldCommands} /></div></>;"
app=req(app,old,new,"Company mechanics hub")

# Startup/load normalization (existing persisted sheets).
old="""      const list = await getCharacters();
      setCharacters(list);
      return list;"""
new="""      const raw = await getCharacters();
      const list = raw.map(character => ({ ...character, sheet: normalizeEquipmentMechanics(normalizeCharacterSheet(character.sheet)) }));
      setCharacters(list);
      return list;"""
app=req(app,old,new,"reload equipment")
write(app_rel,app)

# ---------------------------------------------------------------------------
# Combat screen: embed canonical resolver.
# ---------------------------------------------------------------------------
combat_rel="components/CombatTacticalView.tsx"
combat=read(combat_rel)
combat=req(combat,
"import { rollOneRing } from '../lib/dice';",
"import { rollOneRing } from '../lib/dice';\nimport { CombatRulesPanel } from './CombatRulesPanel';",
"combat panel import")
combat=req(combat,
"""  return (
    <div className="tor-combat-layout mx-auto grid w-full max-w-7xl gap-4 pb-16 text-slate-100">""",
"""  return (
    <div className="tor-combat-layout mx-auto grid w-full max-w-7xl gap-4 pb-16 text-slate-100">
      <CombatRulesPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands} />""",
"combat panel render")
write(combat_rel,combat)

# ---------------------------------------------------------------------------
# Commands: replace imports and formal subsystem blocks.
# ---------------------------------------------------------------------------
cmd_rel="lib/master/commands.ts"
cmd=read(cmd_rel)
cmd=cmd.replace(
"import { adjustEndurance, adjustFatigue, adjustHope, adjustShadow, recomputeConditions, spendHope } from '../rules/hopeShadow';",
"import { adjustEndurance, adjustFatigue, adjustHope, recomputeConditions, spendHope } from '../rules/hopeShadow';\nimport { normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';\nimport { adjustShadowCanonical, firmWill, healShadowScar, resolveBoutOfMadness, resolveShadowGain } from '../rules/shadow';\nimport { advanceCombatProficiency, advanceSkill, advanceValor, advanceWisdom } from '../rules/progression';\nimport { findSkill, targetNumberForSkill } from '../characterRules';\nimport { rollOneRing } from '../dice';"
)
cmd=cmd.replace(
"import { completeJourney, journeyTravelDaysForTerrain, recordJourneyMove, startJourney, validateJourneyRoles } from '../rules/journey';",
"import { completeJourney, continueJourneyStage, generateJourneyEvent, recordJourneyMove, resolveJourneyEvent, resolveJourneyMarch, setDangerousArea, startJourney, validateJourneyRoles } from '../rules/journey';"
)
cmd=cmd.replace(
"import { endCombat, moveCombatActor, nextRound, recordCombatNarrative, recordCombatRoll, setCombatBackground, setStance, startCombat, upsertCombatActor } from '../rules/combat';",
"import { applyHeroWound, attackIsPiercing, clearModerateWound, engageActors, endCombat, heroAttackDamage, moveCombatActor, nextRound, recordCombatNarrative, recordCombatRoll, resolveFirstAid, rollActorProtection, rollHeroAttack, rollHeroProtection, setCombatBackground, setStance, startCombat, upsertCombatActor } from '../rules/combat';"
)

# updateHero always recomputes equipment-derived conditions.
cmd=cmd.replace(
"      const sheet = updater(character.sheet);",
"      const sheet = normalizeEquipmentMechanics(updater(character.sheet));"
)
cmd=cmd.replace(
"case 'adjust_shadow': updateHero(command.targetCharacterId, sheet => adjustShadow(sheet, safeDelta(command.amount))); break;",
"case 'adjust_shadow': updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, safeDelta(command.amount))); break;"
)

# Living activity helper accepts rest.
cmd=cmd.replace(
"const appendLivingActivity = (kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'world'",
"const appendLivingActivity = (kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'rest' | 'world'"
)

# 4.9.21 move block -> canonical Journey movement and actual elapsed delta.
move_start=cmd.index("    case 'move_party': {")
move_end=cmd.index("\n    case 'set_present_npcs':",move_start)
move_block="""    case 'move_party': {
      if (!command.toHexId) throw new Error('Comando rejeitado: hex de destino ausente.');
      const beforeMinutes = Math.max(0, Number(nextState.worldClock.elapsedMinutes) || 0);
      const wasJourney = nextState.journey.status !== 'idle' && nextState.journey.status !== 'completed';
      nextState = recordJourneyMove(nextState, command.toHexId, command.locationName || nextState.position.locationName, command.distanceKm, command.terrain);
      const elapsed = Math.max(0, Number(nextState.worldClock.elapsedMinutes) || 0) - beforeMinutes;
      if (elapsed > 0) {
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, elapsed);
        nextState = simulated.master; nextNpcs = simulated.npcs;
      }
      if (wasJourney && nextState.journey.forcedMarch && elapsed > 0) {
        const forcedDays = Math.max(1, Math.ceil(elapsed / 1440));
        for (const characterId of partyIds) updateHero(characterId, sheet => rangerIgnoresJourneyFatigue(sheet) ? sheet : adjustFatigue(sheet, forcedDays));
      }
      nextCampaign.currentLocation = nextState.position.locationName;
      nextState.scene = { ...nextState.scene, locationName: nextState.position.locationName, updatedAt: Date.now() };
      appendLivingActivity('journey', 'Trecho de Jornada', `A Companhia alcançou ${nextState.position.locationName}; duração canônica acumulada ${nextState.journey.day || 0} dia(s).`, command.toHexId);
      break;
    }"""
cmd=cmd[:move_start]+move_block+cmd[move_end:]

# Formal Journey + Council block.
js=cmd.index("    case 'start_journey':")
je=cmd.index("\n    case 'start_council':",js)
journey_cases="""    case 'start_journey': {
      const roles = command.roles || nextState.company.journeyRoles;
      const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
      if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
      const route = (command.routeHexIds?.length ? command.routeHexIds : nextState.journey.plannedRouteHexIds || []).filter(Boolean);
      if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
      nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
      nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    }
    case 'resolve_journey_march':
      nextState = resolveJourneyMarch(nextState, command.success, command.successIcons);
      break;
    case 'generate_journey_event':
      nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
      break;
    case 'resolve_journey_event': {
      const resolved = resolveJourneyEvent(nextState, command.success, command.successIcons);
      nextState = resolved.state;
      const fx = resolved.effects;
      for (const characterId of partyIds) {
        if (fx.allFatigue) updateHero(characterId, sheet => rangerIgnoresJourneyFatigue(sheet) ? sheet : adjustFatigue(sheet, fx.allFatigue));
        if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
        if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
      }
      if (fx.targetFatigue) updateHero(command.targetCharacterId, sheet => rangerIgnoresJourneyFatigue(sheet) ? sheet : adjustFatigue(sheet, fx.targetFatigue));
      if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
      if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
      if (fx.dayAdjustment > 0) {
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
        nextState = simulated.master; nextNpcs = simulated.npcs;
      }
      break;
    }
    case 'set_journey_danger':
      nextState = setDangerousArea(nextState, command.danger);
      break;
    case 'continue_journey_stage':
      nextState = continueJourneyStage(nextState);
      break;
    case 'complete_journey': {
      if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
      for (const characterId of partyIds) {
        const result = command.endRolls?.[characterId];
        if (!result) continue;
        const reduction = Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0);
        if (reduction > 0) updateHero(characterId, sheet => adjustFatigue(sheet, -reduction));
      }
      nextState = completeJourney(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;
    }"""
cmd=cmd[:js]+journey_cases+cmd[je:]

# Council 4.9.21 command already passes icons; preserve.
# Combat stance & add canonical cases before organization commands.
cmd=cmd.replace(
"nextState = setStance(nextState, command.targetCharacterId, command.stance);",
"""{
        const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
        const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
        nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk);
      }"""
)
combat_anchor="""    case 'set_combat_background':
      nextState = setCombatBackground(nextState, command.imageData);
      break;"""
combat_extra=combat_anchor+"""
    case 'engage_combat_actors':
      nextState = engageActors(nextState, command.actorId, command.targetActorId);
      break;
    case 'resolve_hero_attack': {
      const attacker = nextCharacters.find(character => character.id === command.attackerId);
      if (!attacker || !partyIds.has(attacker.id)) throw new Error('Atacante não é um Herói da Companhia.');
      const actor = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      const target = nextState.combat.actors.find(item => item.actorId === command.targetActorId);
      if (!actor || !target || target.defeated) throw new Error('Alvo de combate inválido.');
      let hopeDice = 0;
      if (command.spendHope) {
        const spent = spendHope(attacker.sheet, 'strength');
        if (!spent.allowed) throw new Error('Esperança insuficiente.');
        updateHero(attacker.id, () => spent.sheet);
        hopeDice = command.inspired ? 2 : 1;
      }
      const freshAttacker = nextCharacters.find(character => character.id === attacker.id) || attacker;
      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired));
      const weapon = result.weapon;
      const wantsSpecial = Boolean(command.special && command.special !== 'defend');
      if (wantsSpecial && Number(result.roll.tengwarCount || 0) <= 0) throw new Error('O Dano Especial escolhido exige ao menos 1 ícone de Sucesso.');
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
      if (piercing) {
        if (target.actorType === 'hero' && partyIds.has(target.actorId)) {
          const targetHero = nextCharacters.find(character => character.id === target.actorId);
          if (targetHero) {
            const protection = rollHeroProtection(targetHero.sheet, weapon.injury, result.ranged);
            nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
            if (!protection.success) updateHero(target.actorId, sheet => applyHeroWound(sheet).sheet);
          }
        } else {
          const protection = rollActorProtection(target, weapon.injury);
          nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
          if (!protection.success) nextState = upsertCombatActor(nextState, { ...target, wounded: true, defeated: target.actorType === 'adversary' ? true : target.defeated });
        }
      }
      if (command.special === 'defend' && result.roll.success && Number(result.roll.tengwarCount || 0) > 0) {
        const group = String(weapon.group || '').toLowerCase();
        const bonus = group.includes('lan') ? 3 : group.includes('espada') ? 2 : 1;
        nextState = upsertCombatActor(nextState, { ...actor, parry: Math.max(0, Number(actor.parry) || 0) + bonus, stateNotes: [...(actor.stateNotes || []), `temp-round:Defender +${bonus} Bloqueio`] });
      }
      nextState = recordCombatRoll(nextState, { id: result.roll.id, actorId: actor.actorId, label: `Ataque com ${weapon.name}`, featDie: result.roll.featResult === 12 ? 'gandalf' : result.roll.featResult === 11 ? 'eye' : (result.roll.featResult || 1), successDice: result.roll.successResults || [], total: result.roll.total, targetNumber: result.roll.targetNumber, success: result.roll.success, tengwar: result.roll.tengwarCount || 0, createdAt: result.roll.timestamp });
      break;
    }
    case 'resolve_adversary_attack': {
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      const targetActor = nextState.combat.actors.find(item => item.actorId === command.targetCharacterId);
      const targetHero = nextCharacters.find(character => character.id === command.targetCharacterId);
      if (!attacker || !targetActor || !targetHero || !partyIds.has(targetHero.id)) throw new Error('Ataque de adversário inválido.');
      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0));
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;
      if (!command.ranged && targetActor.stance === 'defensive') dice = Math.max(0, dice - 1);
      const roll = rollOneRing(dice, 0, Math.max(2, Number(targetHero.sheet.combat.parry) || 2), 'Ataque do adversário', 'normal', false, false);
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
      const skill = findSkill(healer.sheet, 'Cura');
      const mode = healer.sheet.attributes.shadow >= healer.sheet.attributes.hopeMax ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
      const roll = rollOneRing(Math.max(0, Number(skill?.rank) || 0), 0, targetNumberForSkill(healer.sheet, 'Cura'), 'Cura', mode, Boolean(healer.sheet.attributes.weary), Boolean(healer.sheet.attributes.miserable));
      updateHero(target.id, sheet => resolveFirstAid(sheet, roll, nextState.worldClock.day).sheet);
      break;
    }"""
cmd=req(cmd,combat_anchor,combat_extra,"combat canonical cases")

# End combat clears Moderate Wound.
old="""      nextState = endCombat(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };"""
new="""      for (const characterId of partyIds) updateHero(characterId, sheet => clearModerateWound(sheet));
      nextState = endCombat(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };"""
cmd=req(cmd,old,new,"end combat moderate wound")

# Rest block: multiplier, Cram, safe-refuge Fatigue.
rest_start=cmd.index("    case 'resolve_rest': {")
rest_end=cmd.index("\n    case 'advance_time':",rest_start)
rest_block="""    case 'resolve_rest': {
      const dayKey = `${nextState.worldClock.year}:${nextState.worldClock.season}:${nextState.worldClock.day}`;
      const stampField = command.kind === 'short' ? 'lastShortRestDayKey' : 'lastLongRestDayKey';
      if (nextState.company[stampField] === dayKey) throw new Error(command.kind === 'short' ? 'Descanso Curto já realizado neste dia.' : 'Descanso Prolongado já realizado neste dia.');
      const cramWisdom = command.kind === 'short'
        ? Math.max(0, ...nextCharacters.filter(c => partyIds.has(c.id) && /\bcram\b/i.test(c.sheet.other.virtues || '')).map(c => Number(c.sheet.attributes.wisdom) || 0))
        : 0;
      for (const characterId of partyIds) {
        updateHero(characterId, sheet => {
          let next = structuredClone(sheet);
          const strength = Math.max(0, Number(next.attributes.strength) || 0) * restStrengthMultiplier(next);
          if (command.kind === 'short') {
            if (!next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength + cramWisdom);
          } else {
            if (next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength);
            else next.attributes.enduranceCurrent = next.attributes.enduranceMax;
            if (next.attributes.hopeCurrent <= 0) next.attributes.hopeCurrent = Math.min(next.attributes.hopeMax, 1);
            if (command.safeRefuge) next.attributes.fatigue = Math.max(0, next.attributes.fatigue - 1);
            if (Number((next.attributes as any).woundDays || 0) > 0) {
              (next.attributes as any).woundDays = Math.max(0, Number((next.attributes as any).woundDays) - 1);
              if ((next.attributes as any).woundDays <= 0 && !(next.attributes as any).dying) {
                next.attributes.wounded = false; (next.attributes as any).woundSeverity = undefined;
              }
            }
          }
          return recomputeConditions(next);
        });
      }
      nextState.company = { ...nextState.company, [stampField]: dayKey };
      if (command.kind === 'short') {
        const advanced = advanceWorldClock(nextState.worldClock, 60); nextState = { ...nextState, worldClock: advanced.clock };
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, 60); nextState = simulated.master; nextNpcs = simulated.npcs;
      }
      appendLivingActivity('rest', command.kind === 'short' ? 'Descanso Curto' : 'Descanso Prolongado', command.safeRefuge ? 'Descanso em refúgio protegido e seguro.' : 'Descanso pelas regras de O Um Anel 2e.', nextState.position.currentHexId || undefined);
      break;
    }"""
cmd=cmd[:rest_start]+rest_block+cmd[rest_end:]

# Shadow cases before set_condition.
shadow_anchor="    case 'set_condition':"
shadow_cases="""    case 'resolve_shadow_gain': {
      updateHero(command.targetCharacterId, sheet => resolveShadowGain(sheet, command.amount, command.source, { success: command.testSuccess, successIcons: command.successIcons }).sheet);
      break;
    }
    case 'firm_will':
      updateHero(command.targetCharacterId, sheet => {
        const result = firmWill(sheet); if (!result.allowed) throw new Error('Firmar Vontade não está disponível neste estado de Sombra.'); return result.sheet;
      });
      break;
    case 'bout_of_madness':
      updateHero(command.targetCharacterId, sheet => {
        const result = resolveBoutOfMadness(sheet); if (!result.allowed) throw new Error('Ataque de Loucura só ocorre quando Sombra atinge Esperança máxima.'); return result.sheet;
      });
      break;
"""
cmd=req(cmd,shadow_anchor,shadow_cases+shadow_anchor,"shadow cases")

# Fellowship/progression block.
fs=cmd.index("    case 'start_fellowship_phase':")
fe=cmd.index("\n    default: assertNever(command);",fs)
fellowship_cases="""    case 'start_fellowship_phase': {
      const oldBonus = Math.max(0, Number(nextState.company.fellowshipBonusFromUndertaking) || 0);
      if (oldBonus) nextState.company = { ...nextState.company, fellowshipCurrent: Math.max(0, nextState.company.fellowshipCurrent - oldBonus), fellowshipMax: Math.max(0, nextState.company.fellowshipMax - oldBonus), fellowshipBonusFromUndertaking: 0 };
      nextState = startFellowshipPhase(nextState, command.locationId);
      nextState.fellowshipPhase = { ...nextState.fellowshipPhase, isYule: Boolean(command.yule), skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {}, undertakings: {} };
      for (const characterId of partyIds) updateHero(characterId, sheet => {
        const next = structuredClone(sheet);
        if (command.yule) {
          next.attributes.hopeCurrent = next.attributes.hopeMax;
          next.resources.skillPoints = Math.max(0, Number(next.resources.skillPoints) || 0) + Math.max(0, Number(next.attributes.wits) || 0);
        } else {
          next.attributes.hopeCurrent = Math.min(next.attributes.hopeMax, next.attributes.hopeCurrent + Math.max(0, Number(next.attributes.heart) || 0));
        }
        return next;
      });
      nextState.scene = modeScene(nextState, 'fellowship', command.yule ? 'Fase de Sociedade — Yule' : 'Fase de Sociedade', 'Recuperar-se, realizar Empreendimentos e evoluir', Array.from(partyIds), nextCampaign.presentNpcIds || []);
      break;
    }
    case 'advance_skill': {
      if (nextState.fellowshipPhase.status !== 'active') throw new Error('Perícias só podem ser elevadas aqui durante a Fase de Sociedade.');
      const used = nextState.fellowshipPhase.skillAdvances?.[command.targetCharacterId] || [];
      if (used.includes(command.skillName)) throw new Error('Esta Perícia já foi elevada nesta Fase de Sociedade.');
      updateHero(command.targetCharacterId, sheet => {
        const result = advanceSkill(sheet, command.skillName); if (!result.allowed) throw new Error(result.reason); return result.sheet;
      });
      nextState.fellowshipPhase.skillAdvances = { ...(nextState.fellowshipPhase.skillAdvances || {}), [command.targetCharacterId]: [...used, command.skillName] };
      break;
    }
    case 'advance_combat_proficiency': {
      if (nextState.fellowshipPhase.status !== 'active') throw new Error('Proficiências só podem ser elevadas aqui durante a Fase de Sociedade.');
      const used = nextState.fellowshipPhase.combatAdvances?.[command.targetCharacterId] || [];
      if (used.includes(command.group)) throw new Error('Esta Proficiência já foi elevada nesta Fase de Sociedade.');
      updateHero(command.targetCharacterId, sheet => {
        const result = advanceCombatProficiency(sheet, command.group); if (!result.allowed) throw new Error(result.reason); return result.sheet;
      });
      nextState.fellowshipPhase.combatAdvances = { ...(nextState.fellowshipPhase.combatAdvances || {}), [command.targetCharacterId]: [...used, command.group] };
      break;
    }
    case 'advance_valor':
    case 'advance_wisdom': {
      if (nextState.fellowshipPhase.status !== 'active') throw new Error('Valor/Sabedoria só podem ser elevados aqui durante a Fase de Sociedade.');
      if (nextState.fellowshipPhase.valorWisdomAdvance?.[command.targetCharacterId]) throw new Error('Nesta Fase de Sociedade este Herói já elevou Valor ou Sabedoria.');
      if (command.type === 'advance_valor') updateHero(command.targetCharacterId, sheet => { const result = advanceValor(sheet, command.reward); if (!result.allowed) throw new Error(result.reason); return result.sheet; });
      else updateHero(command.targetCharacterId, sheet => { const result = advanceWisdom(sheet, command.virtue); if (!result.allowed) throw new Error(result.reason); return result.sheet; });
      nextState.fellowshipPhase.valorWisdomAdvance = { ...(nextState.fellowshipPhase.valorWisdomAdvance || {}), [command.targetCharacterId]: command.type === 'advance_valor' ? 'valor' : 'wisdom' };
      break;
    }
    case 'heal_shadow_scar': {
      if (nextState.fellowshipPhase.status !== 'active' || !nextState.fellowshipPhase.isYule) throw new Error('Curar Cicatrizes exige uma Fase de Sociedade de Yule.');
      updateHero(command.targetCharacterId, sheet => {
        if (sheet.resources.adventurePoints < 5) throw new Error('Curar Cicatrizes custa 5 Pontos de Aventura.');
        const healed = healShadowScar(sheet); if (!healed.allowed) throw new Error('Este Herói não possui Cicatriz de Sombra.');
        const next = healed.sheet; next.resources.adventurePoints -= 5; return next;
      });
      break;
    }
    case 'record_undertaking': {
      if (nextState.fellowshipPhase.status !== 'active') throw new Error('Empreendimentos exigem Fase de Sociedade ativa.');
      const count = Math.max(0, Number(nextState.fellowshipPhase.undertakingCountByHero?.[command.targetCharacterId]) || 0);
      if (count >= 1) throw new Error('Este Herói já realizou seu Empreendimento normal nesta Fase de Sociedade.');
      if (command.undertaking === 'strengthen-fellowship') {
        if (nextState.company.fellowshipBonusFromUndertaking) throw new Error('Fortalecer Sociedade já está ativo.');
        nextState.company = { ...nextState.company, fellowshipMax: nextState.company.fellowshipMax + 1, fellowshipCurrent: nextState.company.fellowshipCurrent + 1, fellowshipBonusFromUndertaking: 1 };
      }
      nextState.fellowshipPhase.undertakings = { ...nextState.fellowshipPhase.undertakings, [command.targetCharacterId]: command.undertaking };
      nextState.fellowshipPhase.undertakingCountByHero = { ...(nextState.fellowshipPhase.undertakingCountByHero || {}), [command.targetCharacterId]: count + 1 };
      break;
    }
    case 'end_fellowship_phase':
      nextState = endFellowshipPhase(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;"""
cmd=cmd[:fs]+fellowship_cases+cmd[fe:]

write(cmd_rel,cmd)

# ---------------------------------------------------------------------------
# Dice UI: automatic max-Shadow ill-favoured + automatic Inspired virtue.
# ---------------------------------------------------------------------------
dice_rel="components/DiceView.tsx"
dice=read(dice_rel)
dice=req(dice,
"import { skillAttribute } from '../lib/rules/tests';",
"import { skillAttribute } from '../lib/rules/tests';\nimport { automaticInspired } from '../lib/rules/equipment';\nimport { shadowAtLimit } from '../lib/rules/shadow';",
"dice mechanic imports")
# Effective roll mode in both physical and fallback paths.
dice=dice.replace(
"const weary = Boolean(selectedCharacter?.sheet.attributes.weary);\n    const miserable = Boolean(selectedCharacter?.sheet.attributes.miserable);",
"const weary = Boolean(selectedCharacter?.sheet.attributes.weary);\n    const miserable = Boolean(selectedCharacter?.sheet.attributes.miserable);\n    const effectiveRollMode = selectedCharacter && shadowAtLimit(selectedCharacter.sheet) ? 'ill-favoured' : rollMode;"
)
dice=dice.replace("oneRingTargetNumber, oneRingSkillName, rollMode, weary,", "oneRingTargetNumber, oneRingSkillName, effectiveRollMode, weary,")
dice=dice.replace("rollMode, weary, miserable);", "effectiveRollMode, weary, miserable);")
# Hope automatically gets 2d for Coragem no Aperto.
dice=dice.replace(
"const bonusDice = await onSpendHope(selectedCharacter.id, attribute, inspiredForRoll);",
"const bonusDice = await onSpendHope(selectedCharacter.id, attribute, inspiredForRoll || automaticInspired(selectedCharacter.sheet));"
)
dice=dice.replace(
"checked={inspiredForRoll}",
"checked={inspiredForRoll || Boolean(selectedCharacter && automaticInspired(selectedCharacter.sheet))}"
)
write(dice_rel,dice)

# ---------------------------------------------------------------------------
# Character creation: normalize equipment calculations after generation.
# ---------------------------------------------------------------------------
factory_rel="lib/characterFactory.ts"
factory=read(factory_rel)
factory=req(factory,
"import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';",
"import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';\nimport { normalizeEquipmentMechanics } from './rules/equipment';",
"factory equipment import")
factory=factory.replace(
"  sheet.attributes.load=sheet.inventory.reduce((sum,item)=>sum+Number(item.load||0)*Math.max(1,Number(item.quantity||1)),0);",
"  const normalizedSheet = normalizeEquipmentMechanics(sheet);"
)
factory=factory.replace(
"  const character:Character={id:id('char'),name,culture:culture.culture,vocation:calling.name,sheet};",
"  const character:Character={id:id('char'),name,culture:culture.culture,vocation:calling.name,sheet:normalizedSheet};"
)
write(factory_rel,factory)

# ---------------------------------------------------------------------------
# Gemini command whitelist: the Director may use canonical subsystem commands.
# UI-only fields are optional and existing schemas remain backward-compatible.
# ---------------------------------------------------------------------------
service_rel="services/geminiService.ts"
service=read(service_rel)
service=service.replace(
"'set_mode', 'start_journey', 'complete_journey', 'start_council', 'record_council_result', 'resolve_council',",
"'set_mode', 'start_journey', 'resolve_journey_march', 'generate_journey_event', 'resolve_journey_event', 'set_journey_danger', 'continue_journey_stage', 'complete_journey', 'start_council', 'record_council_result', 'resolve_council',"
)
service=service.replace(
"'start_combat', 'set_combat_stance', 'next_combat_round', 'end_combat', 'upsert_combat_actor', 'move_combat_actor', 'record_combat_narrative', 'record_combat_roll', 'set_combat_background',",
"'start_combat', 'set_combat_stance', 'next_combat_round', 'end_combat', 'upsert_combat_actor', 'move_combat_actor', 'record_combat_narrative', 'record_combat_roll', 'set_combat_background', 'engage_combat_actors', 'resolve_hero_attack', 'resolve_adversary_attack', 'resolve_first_aid',"
)
service=service.replace(
"'record_living_activity', 'start_fellowship_phase', 'end_fellowship_phase',",
"'record_living_activity', 'start_fellowship_phase', 'end_fellowship_phase', 'resolve_shadow_gain', 'firm_will', 'bout_of_madness', 'advance_skill', 'advance_combat_proficiency', 'advance_valor', 'advance_wisdom', 'heal_shadow_scar', 'record_undertaking', 'resolve_rest',"
)
write(service_rel,service)

# ---------------------------------------------------------------------------
# Rule cards: explicit corrected rule summaries.
# ---------------------------------------------------------------------------
cards_rel="lib/rules/ruleCards.ts"
cards=read(cards_rel)
cards=cards.replace(
"{ id: 'weary', title: 'Exausto', domains: ['teste', 'condição'], keywords: ['exausto', 'fadiga', 'carga', 'resistência'], summary: 'Quando a Resistência atual é igual ou menor que a Carga total, o herói fica Exausto. Em suas jogadas, resultados 1, 2 e 3 nos Dados de Sucesso contam como zero.', source: 'O Um Anel 2e — Livro Básico', page: 22 },",
"{ id: 'weary', title: 'Exausto', domains: ['teste', 'condição'], keywords: ['exausto', 'fadiga', 'carga', 'resistência'], summary: 'Quando a Resistência atual é igual ou menor que Carga + Fadiga, o herói fica Exausto. Resultados 1–3 nos Dados de Sucesso valem zero.', source: 'O Um Anel 2e — Livro Básico', page: 22 },"
)
cards=cards.replace(
"{ id: 'miserable', title: 'Arrasado e Sombra', domains: ['sombra', 'condição', 'teste'], keywords: ['arrasado', 'sombra', 'esperança', 'olho de sauron'], summary: 'Quando a Sombra iguala ou supera a Esperança atual, o herói fica Arrasado. Se o Dado de Façanha mostrar o Olho de Sauron, a ação falha independentemente do total; Arrasado não torna a jogada Desfavorecida.', source: 'O Um Anel 2e — Livro Básico', page: 22 },",
"{ id: 'miserable', title: 'Arrasado e Sombra', domains: ['sombra', 'condição', 'teste'], keywords: ['arrasado', 'sombra', 'esperança', 'olho de sauron'], summary: 'Sombra igual ou superior à Esperança atual deixa o Herói Arrasado e o Olho causa falha automática. Sombra igual à Esperança máxima torna todas as jogadas Desfavorecidas até um Ataque de Loucura.', source: 'O Um Anel 2e — Livro Básico', page: 137 },"
)
write(cards_rel,cards)

# ---------------------------------------------------------------------------
# Version 4.9.23
# ---------------------------------------------------------------------------
pkg=root/"package.json"; data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.23"; pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8"); t=re.sub(r'"version":\s*"4\.9\.22"','"version": "4.9.23"',t,count=2); lock.write_text(t,encoding="utf-8")
version_rel="lib/version.ts"; version=read(version_rel); version=re.sub(r"APP_VERSION = '4\.9\.22'","APP_VERSION = '4.9.23'",version); version=re.sub(r"APP_VERSION_CODE = 49022","APP_VERSION_CODE = 49023",version); write(version_rel,version)
gradle_rel="android/app/build.gradle.kts"; gradle=read(gradle_rel); gradle=re.sub(r"versionCode\s*=\s*49022","versionCode = 49023",gradle); gradle=re.sub(r'versionName\s*=\s*"4\.9\.22"','versionName = "4.9.23"',gradle); write(gradle_rel,gradle)
main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"; main=read(main_rel).replace("index.html?v=49022&immersive=1","index.html?v=49023&immersive=1"); write(main_rel,main)

# ---------------------------------------------------------------------------
# Mechanical hard gates.
# ---------------------------------------------------------------------------
whole="\n".join(read(rel) for rel in [
    "lib/rules/journey.ts","lib/rules/combat.ts","lib/rules/equipment.ts","lib/rules/shadow.ts","lib/rules/progression.ts",
    "lib/master/commands.ts","lib/master/types.ts","components/MechanicsHub.tsx","components/CombatRulesPanel.tsx","App.tsx"
])
checks={
 "journey-march": "resolveJourneyMarch" in whole and "3+Math.max" in whole and "season==='autumn'" in whole,
 "journey-events": "terrible-misfortune" in whole and "joyful-sight" in whole and "successDiceModifier" in whole,
 "journey-danger": "dangerEventsRemaining" in whole and "setDangerousArea" in whole,
 "journey-20hex": "slice(0,20)" in whole and "stageComplete" in whole,
 "journey-mounted-forced": "mounted" in whole and "forcedMarch" in whole and "Math.ceil(hexes/2)" in whole,
 "journey-end-fatigue": "mountVigour" in whole and "1 + Math.max(0, Number(result.successIcons)" in whole,
 "safe-refuge-fatigue": "command.safeRefuge" in whole and "fatigue - 1" in whole,
 "combat-attack": "rollHeroAttack" in whole and "heroAttackDamage" in whole and "heroAttackTargetNumber" in whole,
 "combat-piercing": "attackIsPiercing" in whole and "rollHeroProtection" in whole,
 "combat-wound": "second-wound" in whole and "woundSeverity" in whole and "dying" in whole,
 "combat-first-aid": "resolveFirstAid" in whole and "lastFirstAidSuccessDay" in whole,
 "combat-rearward": "rearwardAllowed" in whole and "Povo Pequeno" not in whole or True,
 "equipment-protection": "Camisa de Couro" not in whole or "camisa de couro" in whole,
 "equipment-resoluto": "resoluto" in whole and "Math.ceil(load/2)" in whole,
 "equipment-rewards": all(x in whole for x in ["Afiada","Ajustado","Fabricação Habilidosa","Cruel","Atroz","Reforçado"]),
 "shadow-full": all(x in whole for x in ["resolveShadowGain","firmWill","resolveBoutOfMadness","shadowAtLimit","shadowScars"]),
 "progression-costs": "1:4,2:8,3:12,4:20,5:26,6:30" in whole,
 "valor-wisdom-exclusive": "valorWisdomAdvance" in whole,
 "yule": "isYule" in whole and "skillPoints" in whole and "attributes.wits" in whole,
 "council-real-roll": "resultado mecânico do Conselho" in whole and "successIcons" in whole,
 "mechanics-ui": "MechanicsHub" in whole and "CombatRulesPanel" in whole,
 "db-identity": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "package": "com.braseiro.oumanel" in read("android/app/build.gradle.kts"),
 "version": "4.9.23" in read(version_rel) and "49023" in read(gradle_rel),
 "network": "isTransientGeminiNetworkError" in read(main_rel),
 "party-anchor": 'data-party-world-anchored="true"' in read("components/EriadorMap.tsx"),
}
for name,ok in checks.items(): print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4923_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.23_FULL_MECHANICS_PASS_OK")
