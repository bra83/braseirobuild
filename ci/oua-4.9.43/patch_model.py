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

rel="lib/master/types.ts"
t=read(rel)

# Persistent campaign state introduced by Contos das Terras Solitárias.
t=req(t,
"export interface CompanyState {",
"""export interface LoneLandsState {
  heirCharacterId?: string;
  ancestorName?: string;
  estelangClaimedBy?: string;
  heirOathBound?: boolean;
  hillDreamerIds?: string[];
  hillDestroyed?: boolean;
  lossothSafeRefugeNextAdventure?: boolean;
  lossothHuntCompleted?: boolean;
}

export interface CompanyState {""",
"Lone-lands state interface")
t=req(t,
"  characterPathEyeDelta?: number;\n",
"""  characterPathEyeDelta?: number;
  loneLands?: LoneLandsState;
""",
"Lone-lands company state")

# Forodwaith journey preset.
t=req(t,
"""  herbalRecoveryByHero?: Record<string, {
    success: boolean;
    successIcons: number;
    roll: unknown;
    createdAt: number;
  }>;
}""",
"""  herbalRecoveryByHero?: Record<string, {
    success: boolean;
    successIcons: number;
    roll: unknown;
    createdAt: number;
  }>;
  loneLandsForodwaith?: {
    active: boolean;
    lossothGear: boolean;
    lossothGuides: boolean;
    useSpecialTable: boolean;
    stormSpirits: boolean;
    specialEventsResolved: number;
  } | null;
}""",
"Forodwaith journey state")

# Scenario Council order/roll state.
t=req(t,
"""  memoryVeil?: boolean;
  outcome?: 'success' | 'partial' | 'failure';""",
"""  memoryVeil?: boolean;
  sequenceVariant?: 'standard' | 'lone-lands-introduction-first';
  resistancePending?: boolean;
  rollDiceModifier?: number;
  egelCouncil?: boolean;
  outcome?: 'success' | 'partial' | 'failure';""",
"Lone-lands Council state")

# Adventure adversary metadata + combat state.
t=req(t,
"""  baneTags?: Array<'Orcs'|'Trolls'|'Wolves'|'Evil Men'|'Undead'|'Spiders'>;
}""",
"""  baneTags?: Array<'Orcs'|'Trolls'|'Wolves'|'Evil Men'|'Undead'|'Spiders'>;
  deadlyAbilities?: string[];
  traits?: string[];
}""",
"adversary deadly abilities")
t=req(t,
"""  cleavingReadyByHero?: Record<string, {
    weaponIndex: number;
    sourceTargetId: string;
    createdAt: number;
  }>;
""",
"""  cleavingReadyByHero?: Record<string, {
    weaponIndex: number;
    sourceTargetId: string;
    createdAt: number;
  }>;
  loneLandsOathActive?: boolean;
  combatTaskInspiredUsedRound?: number;
""",
"Lone-lands combat state")

anchor="  | { type: 'set_magical_curse_state'; targetCharacterId: string; inventoryItemId: string; active: boolean; revealed?: boolean; ownerPresent?: boolean; weakeningAttribute?: 'strength'|'heart'|'wits'; hunterTag?: string; reason: string }"
extra="""
  | { type: 'set_combat_actor_deadly_abilities'; actorId: string; deadlyAbilities: string[]; traits?: string[]; reason: string }
  | { type: 'set_lone_lands_heir'; targetCharacterId: string; ancestorName?: string; reason: string }
  | { type: 'claim_estelang'; targetCharacterId: string; reason: string }
  | { type: 'invoke_heir_oath'; targetCharacterId: string; reason: string }
  | { type: 'mark_hill_shadow_dreamer'; targetCharacterId: string; reason: string }
  | { type: 'destroy_hill_of_fear'; reason: string }
  | { type: 'undertake_hunt_lossoth'; targetCharacterId: string; reason: string }
  | { type: 'undertake_stone_unhappy'; targetCharacterId: string; reason: string }
  | { type: 'prepare_forodwaith_journey'; lossothGear: boolean; lossothGuides: boolean; useSpecialTable: boolean; reason: string }
  | { type: 'set_forodwaith_storm_spirits'; active: boolean; reason: string }
  | { type: 'start_lone_lands_dwarven_council'; participants: string[]; npcIds: string[]; reason: string }
  | { type: 'set_lone_lands_council_request'; resistance: 3|6|9; objective: string; reason: string }
  | { type: 'start_egel_council'; participants: string[]; npcIds: string[]; request: 'reasonable'|'bold'|'outrageous'; relation: 'reluctant'|'jagat-vouches'|'friendly'; reason: string }
  | { type: 'resolve_combat_task'; actorCharacterId: string; task: 'intimidate'|'inspire'|'protect'; protectedCharacterId?: string; spendHope?: boolean; inspired?: boolean; reason: string }
  | { type: 'resolve_lossoth_safe_refuge_rest'; reason: string }"""
t=req(t,anchor,anchor+extra,"Lone-lands commands")
write(rel,t)

print("PATCH_4943_MODEL_OK")
