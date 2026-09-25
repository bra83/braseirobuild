from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_model.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

def req(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

types_rel = "lib/master/types.ts"
types = read(types_rel)

types = req(
    types,
    "  eyeAwareness: number;\n",
    """  eyeAwareness: number;
  eyeAwarenessEnabled?: boolean;
  eyeAwarenessInitial?: number;
  eyeHuntThreshold?: number;
  eyeRevelationPending?: boolean;
  eyeLastChangeReason?: string;
""",
    "company eye fields",
)

types = req(
    types,
    "    successDiceModifier?: number;\n",
    """    successDiceModifier?: number;
    targetDie?: number;
    featResults?: number[];
""",
    "journey event dice fields",
)

types = req(
    types,
    "  travelFatigueByHero?: Record<string, number>;\n}",
    """  travelFatigueByHero?: Record<string, number>;
  pendingRoll?: {
    kind: 'march' | 'event';
    characterId: string;
    skill: string;
    targetNumber: number;
    result: unknown;
    createdAt: number;
  } | null;
  lastRoll?: {
    kind: 'march' | 'event' | 'end';
    characterId: string;
    skill: string;
    targetNumber: number;
    result: unknown;
    createdAt: number;
  } | null;
  endRollsByHero?: Record<string, { success: boolean; successIcons: number; mountVigour?: number; roll?: unknown; targetNumber?: number }>;
  encounterPending?: boolean;
  lastEventResult?: { eventType: string; description: string; success: boolean; targetCharacterId: string; createdAt: number } | null;
}""",
    "journey manual state",
)

master_anchor = "export interface MasterState {\n"
summary_type = """export interface ActionResolutionSummary {
  id: string;
  action: string;
  timeBefore: string;
  timeAfter: string;
  elapsedMinutes: number;
  changes: string[];
  consequences: string[];
  nextSteps: string[];
  createdAt: number;
}

"""
types = req(types, master_anchor, summary_type + master_anchor, "summary type")
types = req(
    types,
    "  currentSessionId: string;\n  updatedAt: number;",
    """  currentSessionId: string;
  lastResolutionSummary?: ActionResolutionSummary | null;
  updatedAt: number;""",
    "master summary field",
)

insert_after = "  | { type: 'continue_journey_stage'; reason: string }"
extra_commands = """
  | { type: 'player_start_journey'; destinationHexId: string | null; roles: Partial<Record<JourneyRole, string>>; routeHexIds: string[]; mounted?: boolean; forcedMarch?: boolean; reason: string }
  | { type: 'player_record_journey_roll'; kind: 'march' | 'event' | 'end'; targetCharacterId: string; skill: string; targetNumber: number; roll: unknown; mountVigour?: number; reason: string }
  | { type: 'player_confirm_journey_march'; reason: string }
  | { type: 'player_generate_journey_event'; region: 'border' | 'wild' | 'dark'; targetDie: number; featResults: number[]; terrain?: string; reason: string }
  | { type: 'player_confirm_journey_event'; reason: string }
  | { type: 'player_continue_journey_stage'; reason: string }
  | { type: 'player_complete_journey'; reason: string }
  | { type: 'player_acknowledge_journey_encounter'; reason: string }
  | { type: 'set_eye_awareness_rule'; enabled: boolean; initial: number; huntThreshold?: number; reason: string }
  | { type: 'adjust_eye_awareness'; amount: number; source: 'eye-roll' | 'shadow' | 'magic-minor' | 'magic-major' | 'magic-powerful' | 'dramatic'; reason: string }
  | { type: 'resolve_eye_revelation'; reason: string }"""
types = req(types, insert_after, insert_after + extra_commands, "manual journey commands")
write(types_rel, types)

state_rel = "lib/master/state.ts"
state = read(state_rel)
state = req(
    state,
    "      eyeAwareness: 0,\n",
    """      eyeAwareness: 0,
      eyeAwarenessEnabled: undefined,
      eyeAwarenessInitial: 0,
      eyeHuntThreshold: 16,
      eyeRevelationPending: false,
      eyeLastChangeReason: '',
""",
    "default eye state",
)
state = req(
    state,
    "travelFatigueByHero: {} },",
    "travelFatigueByHero: {}, pendingRoll: null, lastRoll: null, endRollsByHero: {}, encounterPending: false, lastEventResult: null },",
    "default journey manual state",
)
state = req(
    state,
    "      stageRouteHexIds: Array.isArray(incoming.journey?.stageRouteHexIds) ? incoming.journey.stageRouteHexIds : [],\n",
    """      stageRouteHexIds: Array.isArray(incoming.journey?.stageRouteHexIds) ? incoming.journey.stageRouteHexIds : [],
      pendingRoll: incoming.journey?.pendingRoll || null,
      lastRoll: incoming.journey?.lastRoll || null,
      endRollsByHero: incoming.journey?.endRollsByHero || {},
      encounterPending: Boolean(incoming.journey?.encounterPending),
      lastEventResult: incoming.journey?.lastEventResult || null,
""",
    "normalize journey manual state",
)
write(state_rel, state)

journey_rel = "lib/rules/journey.ts"
journey = read(journey_rel)
journey = req(
    journey,
    "        description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,\n",
    "        description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,targetDie:targetRoll,featResults:[...featResults],\n",
    "persist visible event dice",
)
write(journey_rel, journey)

print("PATCH_4938_MODEL_OK")
