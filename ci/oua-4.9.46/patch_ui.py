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

src=here/"templates/components/HeirPanel.tsx"
dst=root/"components/HeirPanel.tsx"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

rel="components/MechanicsHub.tsx"
t=read(rel)
if "HeirPanel" not in t:
    t=req(
        t,
        "import { SkillResolutionPanel } from './SkillResolutionPanel';",
        "import { SkillResolutionPanel } from './SkillResolutionPanel';\nimport { HeirPanel } from './HeirPanel';",
        "Heir panel import",
    )
    t=req(
        t,
        "    <SkillResolutionPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "    <SkillResolutionPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>\n    <HeirPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "Heir panel render",
    )
write(rel,t)

print("PATCH_4946_UI_OK")
