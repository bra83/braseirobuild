from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_rules.py <project-root>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

src=here/"templates/lib/rules/core47.ts"
dst=root/"lib/rules/core47.ts"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

# Balin's Patron benefit makes one combat roll Favoured.
rel="lib/rules/combat.ts"
t=read(rel)
t=req(
    t,
    "  context:{terrain?:string;isNight?:boolean;escaping?:boolean;complicationDice?:number}={},",
    "  context:{terrain?:string;isNight?:boolean;escaping?:boolean;complicationDice?:number;forcedMode?:'normal'|'favoured'|'ill-favoured'}={},",
    "combat forced mode context",
)
t=req(
    t,
    "  const mode=supplementRollMode(sheet,supplementContext,baseMode);",
    "  const mode=context.forcedMode||supplementRollMode(sheet,supplementContext,baseMode);",
    "combat forced mode",
)
write(rel,t)

print("PATCH_4947_RULES_OK")
