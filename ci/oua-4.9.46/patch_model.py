from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_model.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/db.ts"
t=read(rel)
t=req(t,
"""    characterPath?: {
      backgroundId: string;""",
"""    heirState?: {
      name?: string;
      previousExperiencePool: number;
      inheritedFavouredSkill?: string;
      inheritanceItemIds?: string[];
      ready?: boolean;
      createdCharacterId?: string;
      retiredOrDead?: boolean;
    };
    characterPath?: {
      backgroundId: string;""",
"heir state")
write(rel,t)

rel="lib/master/types.ts"
t=read(rel)
anchor="  | { type: 'end_skill_test_session'; reason: string }"
extra="""
  | { type: 'educate_heir'; targetCharacterId: string; heirName?: string; spend: number; reason: string }
  | { type: 'set_heir_family_skill'; targetCharacterId: string; skill: string; reason: string }
  | { type: 'create_heir_character'; targetCharacterId: string; inheritedItemIds?: string[]; reason: string }"""
t=req(t,anchor,anchor+extra,"heir commands")
write(rel,t)

print("PATCH_4946_MODEL_OK")
