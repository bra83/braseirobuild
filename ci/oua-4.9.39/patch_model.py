from pathlib import Path
import shutil, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_model.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")

def req(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

templates=Path(__file__).resolve().parent/"templates"
for src_rel,dst_rel in [
    ("lib/supplementRules.ts","lib/supplementRules.ts"),
    ("lib/rules/supplementMechanics.ts","lib/rules/supplementMechanics.ts"),
    ("lib/rules/striderMode.ts","lib/rules/striderMode.ts"),
    ("components/SupplementRulesPanel.tsx","components/SupplementRulesPanel.tsx"),
    ("components/MechanicsHub.tsx","components/MechanicsHub.tsx"),
]:
    src=templates/src_rel; dst=root/dst_rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)

# DB sheet metadata for mode-specific and configurable supplement effects.
db_rel="lib/db.ts"
db=read(db_rel)
db=req(
    db,
    "    creationTrace?: string[];\n",
    """    creationTrace?: string[];
    rulesMode?: 'standard' | 'strider';
    supplementSelections?: {
      beornEnchantedSkills?: string[];
      [key: string]: unknown;
    };
""",
    "sheet supplement metadata",
)
write(db_rel,db)

# Master state + commands.
types_rel="lib/master/types.ts"
types=read(types_rel)
types=req(types,"export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward';","export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward' | 'skirmish';","skirmish stance")
types=req(
    types,
    "  spotlightByHero: Record<string, number>;\n",
    """  spotlightByHero: Record<string, number>;
  rulesMode?: 'standard' | 'strider';
  striderUseMilestones?: boolean;
  striderMilestones?: Array<{
    id: string;
    characterId: string;
    milestone: string;
    adventurePoints: number;
    skillPoints: number;
    createdAt: number;
  }>;
  preStriderFellowshipCurrent?: number;
  preStriderFellowshipMax?: number;
  preStriderFellowshipFocus?: Record<string, string>;
  elrondJourneyFeatBonusActive?: boolean;
  elrondItemsIdentified?: boolean;
""",
    "company strider state",
)
types=req(
    types,
    "    featResults?: number[];\n",
    """    featResults?: number[];
    detail?: string;
    notable?: boolean;
""",
    "strider event metadata",
)
types=req(
    types,
    "  lastEventResult?: { eventType: string; description: string; success: boolean; targetCharacterId: string; createdAt: number } | null;\n}",
    """  lastEventResult?: { eventType: string; description: string; success: boolean; targetCharacterId: string; createdAt: number } | null;
  striderEvent?: boolean;
  striderDetailDie?: number;
}""",
    "strider journey flags",
)
types=req(
    types,
    "  rolls?: CombatRollRecord[];\n",
    """  rolls?: CombatRollRecord[];
  gainGroundDiceByHero?: Record<string, number>;
  firstbornPowerArmedByHero?: Record<string, boolean>;
  escapedHeroIds?: string[];
""",
    "combat supplement state",
)
types=req(
    types,
    "  | { type: 'adjust_eye_awareness'; amount: number; source: 'eye-roll' | 'shadow' | 'magic-minor' | 'magic-major' | 'magic-powerful' | 'dramatic'; reason: string }",
    "  | { type: 'adjust_eye_awareness'; amount: number; source: 'eye-roll' | 'shadow' | 'magic-minor' | 'magic-major' | 'magic-powerful' | 'dramatic'; direction?: 'increase' | 'decrease'; reason: string }",
    "eye direction",
)
anchor="  | { type: 'resolve_eye_revelation'; reason: string }"
extra="""  | { type: 'set_rules_mode'; mode: 'standard' | 'strider'; reason: string }
  | { type: 'record_strider_milestone'; targetCharacterId: string; milestone: string; adventurePoints: number; skillPoints: number; reason: string }
  | { type: 'use_hunters_resolve'; targetCharacterId: string; reason: string }
  | { type: 'arm_firstborn_power'; targetCharacterId: string; reason: string }
  | { type: 'gain_ground'; targetCharacterId: string; skill: 'Atletismo' | 'Busca'; reason: string }
  | { type: 'escape_skirmish'; targetCharacterId: string; weaponIndex: number; reason: string }
  | { type: 'set_beorn_enchanted_skills'; targetCharacterId: string; skills: string[]; reason: string }
  | { type: 'player_generate_strider_journey_event'; region: 'border' | 'wild' | 'dark'; featResults: number[]; detailDie: number; terrain?: string; reason: string }"""
types=req(types,anchor,anchor+"\n"+extra,"supplement commands")
types=types.replace(
    "| { type: 'resolve_hero_attack'; attackerId: string; targetActorId: string; weaponIndex: number; spendHope?: boolean; inspired?: boolean; special?: 'heavy' | 'pierce' | 'defend'; reason: string }",
    "| { type: 'resolve_hero_attack'; attackerId: string; targetActorId: string; weaponIndex: number; spendHope?: boolean; inspired?: boolean; special?: 'heavy' | 'pierce' | 'defend' | 'escape'; reason: string }"
)
types=types.replace(
    "| { type: 'resolve_adversary_attack'; attackerId: string; targetCharacterId: string; successDice: number; damage: number; injury: number; ranged?: boolean; reason: string }",
    "| { type: 'resolve_adversary_attack'; attackerId: string; targetCharacterId: string; successDice: number; damage: number; injury: number; ranged?: boolean; deadlyAbility?: boolean; hateCost?: number; reason: string }"
)
types=types.replace(
    "| { type: 'record_undertaking'; targetCharacterId: string; undertaking: 'strengthen-fellowship' | 'heal-scars' | 'narrative'; reason: string }",
    "| { type: 'record_undertaking'; targetCharacterId: string; undertaking: 'strengthen-fellowship' | 'heal-scars' | 'meet-patron' | 'narrative'; reason: string }"
)
types=types.replace(
    "| { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; reason: string }",
    "| { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; skill?: 'Cura' | 'Música'; reason: string }"
)
write(types_rel,types)

# Defaults + normalization.
state_rel="lib/master/state.ts"
state=read(state_rel)
state=req(
    state,
    "      spotlightByHero: {},\n",
    """      spotlightByHero: {},
      rulesMode: 'standard',
      striderUseMilestones: false,
      striderMilestones: [],
      elrondJourneyFeatBonusActive: false,
      elrondItemsIdentified: false,
""",
    "company mode defaults",
)
state=req(
    state,
    "      rolls: [],\n",
    """      rolls: [],
      gainGroundDiceByHero: {},
      firstbornPowerArmedByHero: {},
      escapedHeroIds: [],
""",
    "combat supplement defaults",
)
# Normalize optional company/combat fields even for old saves.
norm_anchor="      stageRouteHexIds: Array.isArray(incoming.journey?.stageRouteHexIds) ? incoming.journey.stageRouteHexIds : [],"
if norm_anchor in state:
    pass
# Generic normalize merges existing spreads, so add explicit fallback near top-level company/combat spread anchors if found.
state=state.replace(
    "company: { ...base.company, ...(incoming.company || {}) },",
    "company: { ...base.company, ...(incoming.company || {}), rulesMode: incoming.company?.rulesMode === 'strider' ? 'strider' : 'standard', striderUseMilestones: Boolean(incoming.company?.striderUseMilestones), striderMilestones: Array.isArray(incoming.company?.striderMilestones) ? incoming.company.striderMilestones : [] },",
    1,
)
state=state.replace(
    "combat: { ...base.combat, ...(incoming.combat || {}) },",
    "combat: { ...base.combat, ...(incoming.combat || {}), gainGroundDiceByHero: incoming.combat?.gainGroundDiceByHero || {}, firstbornPowerArmedByHero: incoming.combat?.firstbornPowerArmedByHero || {}, escapedHeroIds: Array.isArray(incoming.combat?.escapedHeroIds) ? incoming.combat.escapedHeroIds : [] },",
    1,
)
write(state_rel,state)

# Solo supplement is an authoritative mechanical source.
src_rel="lib/sourceAuthority.ts"
src=read(src_rel)
src=req(
    src,
    "export function mayProvideMechanics(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'; }",
    "export function mayProvideMechanics(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'||a==='solo-2e'; }",
    "solo mechanics authority",
)
src=req(
    src,
    "export function mayProvideNarrativeContext(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'||a==='adventure-2e'||a==='lore-reference'; }",
    "export function mayProvideNarrativeContext(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'||a==='solo-2e'||a==='adventure-2e'||a==='lore-reference'; }",
    "solo narrative authority",
)
write(src_rel,src)

# TN automatically follows the selected campaign mode because set_rules_mode stamps the hero sheet.
rules_rel="lib/characterRules.ts"
rules=read(rules_rel)
rules=req(
    rules,
    "  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0); return Math.max(8, Math.min(19, 20 - attribute + modifier));",
    "  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0); const base = sheet.other.rulesMode === 'strider' ? 18 : 20; return Math.max(6, Math.min(19, base - attribute + modifier));",
    "strider TN",
)
write(rules_rel,rules)

print("PATCH_4939_MODEL_OK")
