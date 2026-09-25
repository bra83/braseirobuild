from pathlib import Path
import shutil,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

src=here/"templates/components/SkillResolutionPanel.tsx"
dst=root/"components/SkillResolutionPanel.tsx"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

rel="components/MechanicsHub.tsx"
t=read(rel)
if "SkillResolutionPanel" not in t:
    t=req(
        t,
        "import { LoneLandsPanel } from './LoneLandsPanel';",
        "import { LoneLandsPanel } from './LoneLandsPanel';\nimport { SkillResolutionPanel } from './SkillResolutionPanel';",
        "Skill resolution import",
    )
    t=req(
        t,
        "    <LoneLandsPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "    <LoneLandsPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>\n    <SkillResolutionPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "Skill resolution render",
    )
write(rel,t)

print("PATCH_4945_UI_OK")
