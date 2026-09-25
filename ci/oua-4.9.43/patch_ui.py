from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

src=here/"templates/components/LoneLandsPanel.tsx"
dst=root/"components/LoneLandsPanel.tsx"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

rel="components/MechanicsHub.tsx"
t=read(rel)
if "LoneLandsPanel" not in t:
    t=req(
        t,
        "import { TreasureIndexPanel } from './TreasureIndexPanel';",
        "import { TreasureIndexPanel } from './TreasureIndexPanel';\nimport { LoneLandsPanel } from './LoneLandsPanel';",
        "Lone Lands panel import",
    )
    t=req(
        t,
        "    <TreasureIndexPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "    <TreasureIndexPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>\n    <LoneLandsPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
        "Lone Lands panel render",
    )
write(rel,t)

print("PATCH_4943_UI_OK")
