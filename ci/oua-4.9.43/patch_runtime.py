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

# ---------------------------------------------------------------------------
# Journey runtime: Forodwaith modifiers must affect the real player roll.
# ---------------------------------------------------------------------------
rel="App.tsx"
t=read(rel)
if "forodwaithJourneyDiceModifier" not in t:
    # Anchor is present after 4.9.41 runtime imports.
    anchor="import { characterPathCompanyDeltas, sheetFeatOneBecomesEye } from './lib/characterPaths';"
    t=req(
        t,
        anchor,
        anchor+"\nimport { forodwaithJourneyDiceModifier, forodwaithRollMode } from './lib/rules/loneLands';",
        "Lone Lands App imports",
    )

# 4.9.39/4.9.41 manual Journey handler owns the authoritative roll.
old="""    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode: hero.sheet.other.rulesMode === 'strider' ? 'strider' as const : 'standard' as const };
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
      : rollJourneySkill(hero, skill, effectiveExtra, forcedMode, spendHope ? 1 : 0);"""
new="""    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode: hero.sheet.other.rulesMode === 'strider' ? 'strider' as const : 'standard' as const };
    const supplementSource = magicalSuccessSource(hero.sheet, magicalContext);
    const treasureSource = blessingSource(hero.sheet,skill);
    const source = useMagicalSuccess ? (supplementSource||treasureSource) : null;
    const canMagic = canSpendHopeForSupplementMagicalSuccess(hero.sheet, magicalContext) || identifiedBlessingCanMagicalSuccess(hero.sheet,skill);
    if (useMagicalSuccess && !canMagic) throw new Error('Sucesso Mágico indisponível para esta Perícia/situação.');
    if ((spendHope||useMagicalSuccess) && curseBlocksHopeForSkill(hero.sheet,skill)) throw new Error('Malícia: o item impede gastar Esperança nesta rolagem.');
    const treasureDice=magicalBlessingDice(hero.sheet,skill);
    const loneLandsDice=forodwaithJourneyDiceModifier(master.journey,skill);
    const effectiveExtra=Math.trunc(extraDice)+treasureDice+loneLandsDice;
    const baseMode=forcedMode || (shadowAtLimit(hero.sheet) ? 'ill-favoured' : findSkill(hero.sheet,skill)?.favorite ? 'favoured' : 'normal');
    const loneLandsMode=forodwaithRollMode(master.journey,skill,baseMode);
    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + effectiveExtra, skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, effectiveExtra, loneLandsMode, spendHope ? 1 : 0);"""
t=req(t,old,new,"Forodwaith Journey roll modifiers")
write(rel,t)

# ---------------------------------------------------------------------------
# Shadow runtime: inherited oath grants +1d to Hero Shadow Tests in combat.
# ---------------------------------------------------------------------------
rel="components/MechanicsHub.tsx"
t=read(rel)
if "loneLandsOathShadowBonus" not in t:
    anchor="import { sheetFeatOneBecomesEye } from '../lib/characterPaths';"
    t=req(
        t,
        anchor,
        anchor+"\nimport { loneLandsOathShadowBonus } from '../lib/rules/loneLands';",
        "Lone Lands Shadow import",
    )
old="""    const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
    const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
    const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
    let roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));"""
new="""    const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
    const oathDice=loneLandsOathShadowBonus(masterState,selectedShadow.id);
    const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
    const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
    let roll=shadowSource==='transgression'?null:rollOneRing(Math.max(0,rating+oathDice),0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));"""
t=req(t,old,new,"Oath Shadow bonus")
write(rel,t)

print("PATCH_4943_RUNTIME_OK")
