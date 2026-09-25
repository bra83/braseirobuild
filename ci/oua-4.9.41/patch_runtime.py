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

# Character Paths sheet helper.
rel="lib/characterPaths.ts"
t=read(rel)
t += """
export function sheetFeatOneBecomesEye(sheet:{other?:{characterPath?:{importantEventFeat?:number}}}|null|undefined):boolean {
  return Number(sheet?.other?.characterPath?.importantEventFeat)===12;
}
"""
write(rel,t)

# Dice re-interpreter. Remaps every raw Feat 1 to Eye before Favoured /
# Ill-favoured selection, then reuses the canonical interpreter.
rel="lib/dice.ts"
t=read(rel)
helper="""
export function applyFeatOneAsEye(result:RollResult,enabled:boolean):RollResult {
  if(!enabled||result.type!=='one-ring'||!result.featResults?.some(value=>value===1)) return result;
  const featResults=result.featResults.map(value=>value===1?11:value);
  const rebuilt=rollOneRingFromObservation(
    featResults,
    result.successResults||[],
    Number(result.modifier)||0,
    Number(result.targetNumber)||14,
    result.skillName||'',
    result.rollMode||'normal',
    Boolean(result.weary),
    result.physics,
    Boolean(result.miserable),
  );
  return {
    ...rebuilt,
    id:result.id,
    timestamp:result.timestamp,
    characterId:result.characterId,
    characterName:result.characterName,
    magicalSuccess:result.magicalSuccess,
    magicalSource:result.magicalSource,
  };
}

"""
t=req(t,"// Helper to format roll description",helper+"// Helper to format roll description","Feat 1 as Eye helper")
write(rel,t)

# Generic Dice UI.
rel="components/DiceView.tsx"
t=read(rel)
t=t.replace(
"import { rollMagicalSuccess, rollOneRing, rollOneRingFromObservation, rollStandard, rollStandardFromObservation, type RollResult } from '../lib/dice';",
"import { applyFeatOneAsEye, rollMagicalSuccess, rollOneRing, rollOneRingFromObservation, rollStandard, rollStandardFromObservation, type RollResult } from '../lib/dice';",
1)
if "sheetFeatOneBecomesEye" not in t:
    t=t.replace(
        "import type { MasterState } from '../lib/master/types';",
        "import type { MasterState } from '../lib/master/types';\nimport { sheetFeatOneBecomesEye } from '../lib/characterPaths';",
        1)
t=req(t,
"""      if (selectedCharacter && gandalfIsAutomaticMagicalSuccess(selectedCharacter.sheet, Number(result.featResult) || 0)) {""",
"""      if (selectedCharacter) result=applyFeatOneAsEye(result,sheetFeatOneBecomesEye(selectedCharacter.sheet));
      if (selectedCharacter && gandalfIsAutomaticMagicalSuccess(selectedCharacter.sheet, Number(result.featResult) || 0)) {""",
"generic Dice Grey Wizard")
write(rel,t)

# Combat: all Hero attack/protection rolls.
rel="lib/rules/combat.ts"
t=read(rel)
t=t.replace(
"import { rollOneRing, type RollResult } from '../dice';",
"import { applyFeatOneAsEye, rollOneRing, type RollResult } from '../dice';",
1)
if "sheetFeatOneBecomesEye" not in t:
    t=t.replace(
        "import type { Character, CharacterSheet } from '../db';",
        "import type { Character, CharacterSheet } from '../db';\nimport { sheetFeatOneBecomesEye } from '../characterPaths';",
        1)
# Hero attack return roll.
t=req(t,
"""  const roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,actor,target),weapon.group,mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
  return {roll,weapon,ranged};""",
"""  let roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,actor,target),weapon.group,mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
  roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(sheet));
  return {roll,weapon,ranged};""",
"combat attack Grey Wizard")
# Hero protection.
t=req(t,
"""  return rollOneRing(Math.max(0,dice),protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));""",
"""  return applyFeatOneAsEye(
    rollOneRing(Math.max(0,dice),protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable)),
    sheetFeatOneBecomesEye(sheet),
  );""",
"combat protection Grey Wizard")
write(rel,t)

# App Journey helper: remap after Journey roll generation.
rel="App.tsx"
t=read(rel)
if "applyFeatOneAsEye" not in t:
    t=t.replace("import { rollMagicalSuccess, rollOneRing", "import { applyFeatOneAsEye, rollMagicalSuccess, rollOneRing",1)
if "sheetFeatOneBecomesEye" not in t:
    t=t.replace(
        "import { createRandomCharacter } from './lib/characterFactory';",
        "import { createRandomCharacter } from './lib/characterFactory';\nimport { sheetFeatOneBecomesEye } from './lib/characterPaths';",
        1)
t=req(t,
"""    const roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + Math.trunc(extraDice), skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);""",
"""    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + Math.trunc(extraDice), skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);
    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));""",
"Journey Grey Wizard")
write(rel,t)

# Direct command rolls: add one local helper and use it on rolls made on behalf
# of a Hero outside the central combat resolver.
rel="lib/master/commands.ts"
t=read(rel)
if "applyFeatOneAsEye" not in t:
    t=t.replace("import { rollOneRing } from '../dice';","import { applyFeatOneAsEye, rollOneRing } from '../dice';",1)
if "sheetFeatOneBecomesEye" not in t:
    t=t.replace(
        "import { allowedBanes, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
        "import { allowedBanes, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';\nimport { sheetFeatOneBecomesEye } from '../characterPaths';",
        1)
# Targeted replacements for named Hero rolls introduced by mechanics patches.
repls=[
("const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca'),'Busca',baseMode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));",
 "let roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca'),'Busca',baseMode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable)); roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));"),
("const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0)+extra,0,targetNumberForSkill(hero.sheet,'Cura'),'Cura',mode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));",
 "let roll=rollOneRing(Math.max(0,Number(skill?.rank)||0)+extra,0,targetNumberForSkill(hero.sheet,'Cura'),'Cura',mode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable)); roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(hero.sheet));"),
("const shadowRoll=rollOneRing(rank,0,targetNumberForSkill(next,'Sabedoria'),'Sabedoria',baseMode,Boolean(next.attributes.weary),Boolean(next.attributes.miserable));",
 "let shadowRoll=rollOneRing(rank,0,targetNumberForSkill(next,'Sabedoria'),'Sabedoria',baseMode,Boolean(next.attributes.weary),Boolean(next.attributes.miserable)); shadowRoll=applyFeatOneAsEye(shadowRoll,sheetFeatOneBecomesEye(next));"),
]
for old,new in repls:
    t=t.replace(old,new)
write(rel,t)

print("PATCH_4941_RUNTIME_OK")
