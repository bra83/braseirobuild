from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_model.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Character persistent supplement + magical-item state.
rel="lib/db.ts"
t=read(rel)
t=req(t,
"    tags?: string[];\n  }>;",
"""    tags?: string[];
    magicalKind?: 'wondrous-artefact' | 'wondrous-item' | 'enchanted-reward';
    magicalQualities?: string[];
    magicalIdentified?: boolean;
    craftsmanship?: string;
  }>;""","inventory magical metadata")
t=req(t,
"""    supplementSelections?: {
      beornEnchantedSkills?: string[];
      [key: string]: unknown;
    };
""",
"""    supplementSelections?: {
      beornEnchantedSkills?: string[];
      [key: string]: unknown;
    };
    supplementState?: {
      houndWounded?: boolean;
      houndRecoveryPending?: boolean;
      houndRecoveryFailed?: boolean;
      artificerYuleReady?: boolean;
      artificerAcquiredPhaseStartedAt?: number;
      artificerUsedYuleYear?: number;
      artificerStudyUsedPhaseStartedAt?: number;
      [key: string]: unknown;
    };
""","persistent supplement state")
write(rel,t)

# Master state.
rel="lib/master/types.ts"
t=read(rel)
t=req(t,
"  interlocutorNpcIds: string[];\n  outcome?: 'success' | 'partial' | 'failure';",
"""  interlocutorNpcIds: string[];
  spokespersonId?: string;
  timeLimitBonus?: number;
  memoryVeil?: boolean;
  outcome?: 'success' | 'partial' | 'failure';""","Council supplement state")
if "  power?: number;" not in t:
    t=req(t,
    "  quickSheet?: Record<string, unknown>;\n}",
    """  quickSheet?: Record<string, unknown>;
  power?: number;
}""","combat actor Power")
t=req(t,
"  escapedHeroIds?: string[];\n",
"""  escapedHeroIds?: string[];
  pendingHoundPiercing?: {
    targetCharacterId: string;
    sourceActorId?: string;
    createdAt: number;
    round: number;
  } | null;
  elvenLight?: {
    ownerCharacterId: string;
    targetActorIds: string[];
    active: boolean;
    triggered: boolean;
    createdAt: number;
  } | null;
""","combat stateful virtues")
t=req(t,
"  striderDetailDie?: number;\n}",
"""  striderDetailDie?: number;
  herbalRecoveryByHero?: Record<string, {
    success: boolean;
    successIcons: number;
    roll: unknown;
    createdAt: number;
  }>;
}""","Journey herbal recovery")
anchor="  | { type: 'player_generate_strider_journey_event'; region: 'border' | 'wild' | 'dark'; featResults: number[]; detailDie: number; terrain?: string; reason: string }"
extra="""
  | { type: 'prepare_shot'; targetCharacterId: string; weaponIndex: number; reason: string }
  | { type: 'set_combat_actor_power'; actorId: string; power: number; reason: string }
  | { type: 'activate_elven_lights'; targetCharacterId: string; targetActorIds: string[]; reason: string }
  | { type: 'extinguish_elven_lights'; targetCharacterId: string; reason: string }
  | { type: 'resolve_hound_piercing_choice'; targetCharacterId: string; choice: 'hound' | 'hero'; reason: string }
  | { type: 'resolve_hound_recovery'; targetCharacterId: string; reason: string }
  | { type: 'record_journey_herbal_roll'; targetCharacterId: string; reason: string }
  | { type: 'set_council_spokesperson'; targetCharacterId: string; reason: string }
  | { type: 'apply_beauty_of_stars'; targetCharacterId: string; choice: 'time-limit' | 'memory-veil'; reason: string }
  | { type: 'artificer_study_item'; targetCharacterId: string; inventoryItemId: string; skill: 'Ofício' | 'História'; reason: string }
  | { type: 'artificer_yule_craft'; targetCharacterId: string; kind: 'enchanted-reward' | 'wondrous-artefact'; inventoryItemId?: string; name?: string; reason: string }"""
t=req(t,anchor,anchor+extra,"stateful supplement commands")
write(rel,t)

# Defaults / old-save normalization.
rel="lib/master/state.ts"
t=read(rel)
t=t.replace(
"      escapedHeroIds: [],\n",
"""      escapedHeroIds: [],
      pendingHoundPiercing: null,
      elvenLight: null,
""",1)
t=t.replace(
"travelFatigueByHero: {}, pendingRoll: null, lastRoll: null, endRollsByHero: {}, encounterPending: false, lastEventResult: null },",
"travelFatigueByHero: {}, pendingRoll: null, lastRoll: null, endRollsByHero: {}, encounterPending: false, lastEventResult: null, herbalRecoveryByHero: {} },",
1)
t=t.replace(
"combat: { ...base.combat, ...(incoming.combat || {}), gainGroundDiceByHero: incoming.combat?.gainGroundDiceByHero || {}, firstbornPowerArmedByHero: incoming.combat?.firstbornPowerArmedByHero || {}, escapedHeroIds: Array.isArray(incoming.combat?.escapedHeroIds) ? incoming.combat.escapedHeroIds : [] },",
"combat: { ...base.combat, ...(incoming.combat || {}), gainGroundDiceByHero: incoming.combat?.gainGroundDiceByHero || {}, firstbornPowerArmedByHero: incoming.combat?.firstbornPowerArmedByHero || {}, escapedHeroIds: Array.isArray(incoming.combat?.escapedHeroIds) ? incoming.combat.escapedHeroIds : [], pendingHoundPiercing: incoming.combat?.pendingHoundPiercing || null, elvenLight: incoming.combat?.elvenLight || null },",
1)
t=t.replace(
"lastEventResult: incoming.journey?.lastEventResult || null,",
"lastEventResult: incoming.journey?.lastEventResult || null,\n      herbalRecoveryByHero: incoming.journey?.herbalRecoveryByHero || {},",
1)
write(rel,t)

print("PATCH_4940_MODEL_OK")
