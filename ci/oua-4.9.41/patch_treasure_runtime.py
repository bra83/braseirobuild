from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_treasure_runtime.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Generic skill dice UI.
rel="components/DiceView.tsx"
t=read(rel)
t=req(
    t,
    "import { canSpendHopeForSupplementMagicalSuccess, gandalfIsAutomaticMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';",
    "import { canSpendHopeForSupplementMagicalSuccess, gandalfIsAutomaticMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';\nimport { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, illOmenCouncilPenalty, magicalBlessingDice } from '../lib/rules/magicalTreasure';",
    "Treasure Dice imports",
)
t=req(
    t,
    "    const skill = findSkill(character.sheet, skillName);\n    setOneRingSuccessCount(Math.max(0, skill?.rank || 0));",
    """    const skill = findSkill(character.sheet, skillName);
    const blessingDice=magicalBlessingDice(character.sheet,skillName);
    const councilPenalty=masterState.activeMode==='council'?illOmenCouncilPenalty(character.sheet):0;
    setOneRingSuccessCount(Math.max(0,(skill?.rank||0)+blessingDice+councilPenalty));""",
    "Blessing +2d generic",
)
t=req(
    t,
    "    if (!selectedCharacter || hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;",
    "    if (!selectedCharacter || hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;\n    if (curseBlocksHopeForSkill(selectedCharacter.sheet,oneRingSkillName)) { setDiceError('Malícia: este item impede gastar Esperança nesta rolagem.'); return; }",
    "Malice Hope block",
)
t=req(
    t,
    """    const source = magicalSuccessSource(selectedCharacter.sheet, context);
    if (!source || !canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, context)) {
      setDiceError('Sucesso Mágico indisponível para esta Perícia e situação.');
      return;
    }""",
    """    const supplementSource = magicalSuccessSource(selectedCharacter.sheet, context);
    const treasureSource = blessingSource(selectedCharacter.sheet, oneRingSkillName);
    const source = supplementSource || treasureSource;
    const allowed = canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, context) || identifiedBlessingCanMagicalSuccess(selectedCharacter.sheet,oneRingSkillName);
    if (!source || !allowed) {
      setDiceError('Sucesso Mágico indisponível para esta Perícia e situação.');
      return;
    }
    if (curseBlocksHopeForSkill(selectedCharacter.sheet,oneRingSkillName)) {
      setDiceError('Malícia: este item impede gastar Esperança nesta rolagem.');
      return;
    }""",
    "Blessing magical success handler",
)
t=t.replace(
    "{canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, { kind:'skill', skill:oneRingSkillName, terrain, isNight }) && (",
    "{(canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, { kind:'skill', skill:oneRingSkillName, terrain, isNight }) || identifiedBlessingCanMagicalSuccess(selectedCharacter.sheet,oneRingSkillName)) && (",
    1,
)
t=t.replace(
    "<p className=\"mt-1 text-[10px] text-cyan-500\">Puxado de {currentSkill?.name || oneRingSkillName}: {currentSkill?.rank || 0} dados.</p>",
    "<p className=\"mt-1 text-[10px] text-cyan-500\">Perícia {currentSkill?.rank || 0}d6{magicalBlessingDice(selectedCharacter.sheet,oneRingSkillName)>0?' · Bênção +2d':''}{masterState.activeMode==='council'&&illOmenCouncilPenalty(selectedCharacter.sheet)<0?' · Mau Agouro −1d':''}.</p>",
    1,
)
write(rel,t)

# Journey path: Blessings affect every matching Journey skill roll.
rel="App.tsx"
t=read(rel)
if "magicalBlessingDice" not in t:
    t=t.replace(
        "import { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource, supplementBonusDice, supplementInspired, supplementRollMode } from './lib/rules/supplementMechanics';",
        "import { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource, supplementBonusDice, supplementInspired, supplementRollMode } from './lib/rules/supplementMechanics';\nimport { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice } from './lib/rules/magicalTreasure';",
        1,
    )
t=req(
    t,
    """    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode: hero.sheet.other.rulesMode === 'strider' ? 'strider' as const : 'standard' as const };
    const source = useMagicalSuccess ? magicalSuccessSource(hero.sheet, magicalContext) : null;
    if (useMagicalSuccess && !canSpendHopeForSupplementMagicalSuccess(hero.sheet, magicalContext)) throw new Error('Sucesso Mágico indisponível para esta Perícia/situação.');
    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + Math.trunc(extraDice), skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);""",
    """    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode: hero.sheet.other.rulesMode === 'strider' ? 'strider' as const : 'standard' as const };
    const supplementSource = magicalSuccessSource(hero.sheet, magicalContext);
    const treasureSource = blessingSource(hero.sheet,skill);
    const source = useMagicalSuccess ? (supplementSource||treasureSource) : null;
    const canMagic = canSpendHopeForSupplementMagicalSuccess(hero.sheet, magicalContext) || identifiedBlessingCanMagicalSuccess(hero.sheet,skill);
    if (useMagicalSuccess && !canMagic) throw new Error('Sucesso Mágico indisponível para esta Perícia/situação.');
    if ((spendHope||useMagicalSuccess) && curseBlocksHopeForSkill(hero.sheet,skill)) throw new Error('Malícia: o item impede gastar Esperança nesta rolagem.');
    const treasureDice=magicalBlessingDice(hero.sheet,skill);
    const effectiveExtra=Math.trunc(extraDice)+treasureDice;
    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + effectiveExtra, skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, effectiveExtra, forcedMode, spendHope ? 1 : 0);""",
    "Journey Blessing runtime",
)
write(rel,t)

# Journey UI also knows whether an identified Blessing provides Magical Success.
rel="components/JourneyResolutionPanel.tsx"
t=read(rel)
t=req(
    t,
    "import { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';",
    "import { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';\nimport { blessingSource, identifiedBlessingCanMagicalSuccess, magicalBlessingDice } from '../lib/rules/magicalTreasure';",
    "Journey Blessing imports",
)
t=req(
    t,
    """    const source=hero?magicalSuccessSource(hero.sheet,context):null;
    const canMagic=Boolean(hero&&canSpendHopeForSupplementMagicalSuccess(hero.sheet,context));""",
    """    const source=hero?(magicalSuccessSource(hero.sheet,context)||blessingSource(hero.sheet,skill)):null;
    const canMagic=Boolean(hero&&(canSpendHopeForSupplementMagicalSuccess(hero.sheet,context)||identifiedBlessingCanMagicalSuccess(hero.sheet,skill)));""",
    "Journey Blessing magic option",
)
t=t.replace(
    "Gastar 1 Esperança {strider?'· Peregrino: Inspirado (+2d)':'· bônus normal'}",
    "Gastar 1 Esperança {strider?'· Peregrino: Inspirado (+2d)':'· bônus normal'}{hero&&magicalBlessingDice(hero.sheet,skill)>0?' · Bênção +2d automática':''}",
    1,
)
write(rel,t)

# Direct rules commands for skill-based supplement rolls also gain Blessing dice.
rel="lib/master/commands.ts"
t=read(rel)
t=req(
    t,
    "import { allowedBanes, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "import { allowedBanes, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "Blessing command import",
)
# Known direct Hero skill rolls.
t=t.replace(
    "Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca')",
    "Math.max(0,Number(skill?.rank)||0)+magicalBlessingDice(hero.sheet,'Busca'),0,targetNumberForSkill(hero.sheet,'Busca')",
)
t=t.replace(
    "Math.max(0,Number(skill?.rank)||0)+extra,0,targetNumberForSkill(hero.sheet,'Cura')",
    "Math.max(0,Number(skill?.rank)||0)+extra+magicalBlessingDice(hero.sheet,'Cura'),0,targetNumberForSkill(hero.sheet,'Cura')",
)
write(rel,t)

print("PATCH_4941_TREASURE_RUNTIME_OK")
