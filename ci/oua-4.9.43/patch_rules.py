from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_rules.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

src=Path(__file__).resolve().parent/"templates/lib/rules/loneLands.ts"
dst=root/"lib/rules/loneLands.ts"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

# Oath attack bonus is part of the real combat pool.
rel="lib/rules/combat.ts"
t=read(rel)
if "loneLandsOathHeroAttackBonus" not in t:
    t=t.replace(
        "import { applyIllLuckToResult, enchantedProtectionResultBonus, runeScoredArmourIgnoresConditions, runeScoredWeaponIgnoresConditions, straightFlightIgnoresComplications } from './magicalGearMechanics';",
        "import { applyIllLuckToResult, enchantedProtectionResultBonus, runeScoredArmourIgnoresConditions, runeScoredWeaponIgnoresConditions, straightFlightIgnoresComplications } from './magicalGearMechanics';\nimport { loneLandsOathHeroAttackBonus } from './loneLands';",
        1,
    )
t=req(t,
"""  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot-complications);""",
"""  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot+ loneLandsOathHeroAttackBonus(actor)-complications);""",
"oath Hero attack +1d")
write(rel,t)

print("PATCH_4943_RULES_OK")
