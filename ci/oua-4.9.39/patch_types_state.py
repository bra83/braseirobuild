from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_types_state.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Types: rules mode, Skirmish stance, Strider event metadata, supplement commands.
# ---------------------------------------------------------------------------
rel="lib/master/types.ts"
t=read(rel)
t=t.replace(
"export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward';",
"export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward' | 'skirmish';"
)

t=req(t,
"  eyeLastChangeReason?: string;\n",
"  eyeLastChangeReason?: string;\n  rulesMode?: 'standard' | 'strider';\n",
"company rules mode")

t=req(t,
"""  lastEventResult?: { eventType: string; description: string; success: boolean; targetCharacterId: string; createdAt: number } | null;
}""",
"""  lastEventResult?: { eventType: string; description: string; success: boolean; targetCharacterId: string; createdAt: number } | null;
  striderEvent?: boolean;
  striderDetailDie?: number;
}""","Strider journey metadata")

t=t.replace(
"| { type: 'adjust_eye_awareness'; amount: number; source: 'eye-roll' | 'shadow' | 'magic-minor' | 'magic-major' | 'magic-powerful' | 'dramatic'; reason: string }",
"| { type: 'adjust_eye_awareness'; amount: number; source: 'eye-roll' | 'shadow' | 'magic-minor' | 'magic-major' | 'magic-powerful' | 'dramatic'; direction?: 'increase' | 'decrease'; reason: string }"
)

# First Aid may use Song via Staunching Song.
t=t.replace(
"| { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; reason: string }",
"| { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; skill?: 'Cura' | 'Música'; reason: string }"
)

anchor="  | { type: 'resolve_eye_revelation'; reason: string }"
extra="""  | { type: 'set_rules_mode'; mode: 'standard' | 'strider'; reason: string }
  | { type: 'record_strider_milestone'; targetCharacterId: string; milestone: string; adventurePoints: number; skillPoints: number; reason: string }
  | { type: 'use_hunters_resolve'; targetCharacterId: string; reason: string }
  | { type: 'set_beorn_enchanted_skills'; targetCharacterId: string; skills: string[]; reason: string }
  | { type: 'gain_ground'; targetCharacterId: string; success: boolean; successIcons: number; reason: string }
  | { type: 'arm_firstborn_power'; targetCharacterId: string; reason: string }"""
t=req(t,anchor,anchor+"\n"+extra,"supplement commands")
write(rel,t)

# ---------------------------------------------------------------------------
# State defaults/normalization.
# ---------------------------------------------------------------------------
rel="lib/master/state.ts"
t=read(rel)
t=req(t,
"      eyeLastChangeReason: '',\n",
"      eyeLastChangeReason: '',\n      rulesMode: 'standard',\n",
"default rules mode")

# Normalize explicit mode even when reading an older save.
company_anchor="company: { ...base.company, ...(incoming.company || {})"
if company_anchor in t and "rulesMode:" not in t[t.index(company_anchor):t.index(company_anchor)+500]:
    t=t.replace(
        company_anchor,
        "company: { ...base.company, ...(incoming.company || {}), rulesMode: incoming.company?.rulesMode === 'strider' ? 'strider' : 'standard'"
    )
else:
    # Spread-based normalization is already backward safe because default carries standard.
    pass

# Preserve Strider Journey metadata.
t=req(t,
"""      lastEventResult: incoming.journey?.lastEventResult || null,
""",
"""      lastEventResult: incoming.journey?.lastEventResult || null,
      striderEvent: Boolean(incoming.journey?.striderEvent),
      striderDetailDie: incoming.journey?.striderDetailDie == null ? undefined : Math.max(1, Math.min(6, Number(incoming.journey.striderDetailDie) || 1)),
""","normalize Strider journey metadata")
write(rel,t)

print("PATCH_4939_TYPES_STATE_OK")
