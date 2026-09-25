from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_runtime.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Generic Dice.
rel="components/DiceView.tsx"
t=read(rel)
if "applyIllLuckToResult" not in t:
    t=t.replace(
        "import { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, illOmenCouncilPenalty, magicalBlessingDice } from '../lib/rules/magicalTreasure';",
        "import { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, illOmenCouncilPenalty, magicalBlessingDice } from '../lib/rules/magicalTreasure';\nimport { applyIllLuckToResult } from '../lib/rules/magicalGearMechanics';",
        1)
t=req(t,
"""      if (selectedCharacter) result=applyFeatOneAsEye(result,sheetFeatOneBecomesEye(selectedCharacter.sheet));
      if (selectedCharacter && gandalfIsAutomaticMagicalSuccess""",
"""      if (selectedCharacter) {
        result=applyFeatOneAsEye(result,sheetFeatOneBecomesEye(selectedCharacter.sheet));
        result=applyIllLuckToResult(selectedCharacter.sheet,result);
      }
      if (selectedCharacter && gandalfIsAutomaticMagicalSuccess""",
"Dice Ill Luck")
write(rel,t)

# Journey: Darkening penalty and Ill Luck.
rel="App.tsx"
t=read(rel)
if "curseDarkeningDicePenalty" not in t:
    t=t.replace(
        "import { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice } from './lib/rules/magicalTreasure';",
        "import { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice } from './lib/rules/magicalTreasure';\nimport { applyIllLuckToResult, curseDarkeningDicePenalty } from './lib/rules/magicalGearMechanics';",
        1)
t=req(t,
"""    const treasureDice=magicalBlessingDice(hero.sheet,skill);
    const effectiveExtra=Math.trunc(extraDice)+treasureDice;""",
"""    const treasureDice=magicalBlessingDice(hero.sheet,skill);
    const darknessPenalty=curseDarkeningDicePenalty(hero.sheet);
    const effectiveExtra=Math.trunc(extraDice)+treasureDice+darknessPenalty;""",
"Journey Darkening")
t=req(t,
"""    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));""",
"""    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));
    roll=applyIllLuckToResult(hero.sheet,roll);""",
"Journey Ill Luck")
write(rel,t)

# MechanicsHub Shadow test.
rel="components/MechanicsHub.tsx"
t=read(rel)
if "applyIllLuckToResult" not in t:
    t=t.replace(
        "import { sheetFeatOneBecomesEye } from '../lib/characterPaths';",
        "import { sheetFeatOneBecomesEye } from '../lib/characterPaths';\nimport { applyIllLuckToResult, curseDarkeningDicePenalty } from '../lib/rules/magicalGearMechanics';",
        1)
t=req(t,
"""    const extra=supplementBonusDice(selectedShadow.sheet,{kind:'shadow',skill:skillName,dreadSource});
    const roll=shadowSource==='transgression'?null:rollOneRing(Math.max(0,rating+extra),0,targetNumberForSkill(selectedShadow.sheet,skillName),skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));""",
"""    const extra=supplementBonusDice(selectedShadow.sheet,{kind:'shadow',skill:skillName,dreadSource})+curseDarkeningDicePenalty(selectedShadow.sheet);
    let roll=shadowSource==='transgression'?null:rollOneRing(Math.max(0,rating+extra),0,targetNumberForSkill(selectedShadow.sheet,skillName),skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));""",
"Shadow Darkening")
t=req(t,
"""    if(roll) roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(selectedShadow.sheet));
    void run""",
"""    if(roll) {
      roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(selectedShadow.sheet));
      roll=applyIllLuckToResult(selectedShadow.sheet,roll);
    }
    void run""",
"Shadow Ill Luck")
write(rel,t)

# Direct Hero skill rolls in command engine: apply Darkening/Ill Luck where applicable.
rel="lib/master/commands.ts"
t=read(rel)
if "curseDarkeningDicePenalty" not in t:
    t=t.replace(
        "import { activeQualitiesForItem, actorIsBane, applyIllLuckToResult, cleavingEnabled, curseBlocksHopeForWeapon, curseDarkeningDicePenalty, curseHuntedBy,",
        "import { activeQualitiesForItem, actorIsBane, applyIllLuckToResult, cleavingEnabled, curseBlocksHopeForWeapon, curseDarkeningDicePenalty, curseHuntedBy,",
        1)
# Prepare Shot: Search is vision-dependent.
t=t.replace(
"Math.max(0,Number(skill?.rank)||0)+magicalBlessingDice(hero.sheet,'Busca')",
"Math.max(0,Number(skill?.rank)||0)+magicalBlessingDice(hero.sheet,'Busca')+curseDarkeningDicePenalty(hero.sheet)",
)
# Cure is not vision-dependent enough to force Darkening; do not modify.
# Gain Ground / other combat tasks use darkness penalty where present.
t=t.replace(
"Math.max(0, Number(skill?.rank) || 0), 0, targetNumberForSkill(hero.sheet, command.skill)",
"Math.max(0, (Number(skill?.rank) || 0)+curseDarkeningDicePenalty(hero.sheet)), 0, targetNumberForSkill(hero.sheet, command.skill)",
)
# Convert direct hero rolls after 4.9.41 feat remap to also apply Ill Luck.
t=t.replace(
"roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));",
"roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet)); roll=applyIllLuckToResult(hero.sheet,roll);"
)
t=t.replace(
"roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(healer.sheet));",
"roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(healer.sheet)); roll=applyIllLuckToResult(healer.sheet,roll);"
)
t=t.replace(
"shadowRoll=applyFeatOneAsEye(shadowRoll,sheetFeatOneBecomesEye(next));",
"shadowRoll=applyFeatOneAsEye(shadowRoll,sheetFeatOneBecomesEye(next)); shadowRoll=applyIllLuckToResult(next,shadowRoll);"
)
write(rel,t)

print("PATCH_4942_RUNTIME_OK")
