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

# Inventory curse runtime state.
rel="lib/db.ts"
t=read(rel)
t=req(t,
"""    magicalCurseActive?: boolean;
    magicalCurseTrigger?: string;""",
"""    magicalCurseActive?: boolean;
    magicalCurseRevealed?: boolean;
    magicalCurseOwnerPresent?: boolean;
    magicalCurseHunterTag?: string;
    magicalCurseWeaknessFlaw?: string;
    magicalCurseTrigger?: string;""",
"curse runtime state")
write(rel,t)

# Combat actor + combat pending states and commands.
rel="lib/master/types.ts"
t=read(rel)
t=req(t,
"""  power?: number;
}""",
"""  power?: number;
  attributeLevel?: number;
  baneTags?: Array<'Orcs'|'Trolls'|'Wolves'|'Evil Men'|'Undead'|'Spiders'>;
}""",
"combat actor bane metadata")
t=req(t,
"""  elvenLight?: {
    ownerCharacterId: string;
    targetActorIds: string[];
    active: boolean;
    triggered: boolean;
    createdAt: number;
  } | null;""",
"""  elvenLight?: {
    ownerCharacterId: string;
    targetActorIds: string[];
    active: boolean;
    triggered: boolean;
    createdAt: number;
  } | null;
  cleavingReadyByHero?: Record<string, {
    weaponIndex: number;
    sourceTargetId: string;
    createdAt: number;
  }>;
""",
"cleaving pending state")
anchor="  | { type: 'remove_magical_curse'; targetCharacterId: string; inventoryItemId: string; reason: string }"
extra="""
  | { type: 'set_combat_actor_banes'; actorId: string; baneTags: Array<'Orcs'|'Trolls'|'Wolves'|'Evil Men'|'Undead'|'Spiders'>; reason: string }
  | { type: 'set_combat_actor_attribute_level'; actorId: string; attributeLevel: number; reason: string }
  | { type: 'resolve_cleaving_attack'; attackerId: string; targetActorId: string; reason: string }
  | { type: 'stand_up_combat_actor'; actorId: string; reason: string }
  | { type: 'set_magical_curse_state'; targetCharacterId: string; inventoryItemId: string; active: boolean; revealed?: boolean; ownerPresent?: boolean; weakeningAttribute?: 'strength'|'heart'|'wits'; hunterTag?: string; reason: string }"""
t=req(t,anchor,anchor+extra,"4.9.42 commands")
write(rel,t)

# Combat default + normalization.
rel="lib/master/state.ts"
t=read(rel)
t=t.replace(
"pendingHoundPiercing: null,\n      elvenLight: null,",
"pendingHoundPiercing: null,\n      elvenLight: null,\n      cleavingReadyByHero: {},",
1)
t=t.replace(
"pendingHoundPiercing: incoming.combat?.pendingHoundPiercing || null, elvenLight: incoming.combat?.elvenLight || null",
"pendingHoundPiercing: incoming.combat?.pendingHoundPiercing || null, elvenLight: incoming.combat?.elvenLight || null, cleavingReadyByHero: incoming.combat?.cleavingReadyByHero || {}",
1)
write(rel,t)

print("PATCH_4942_MODEL_OK")
