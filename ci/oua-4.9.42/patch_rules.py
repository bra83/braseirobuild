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

src=Path(__file__).resolve().parent/"templates/lib/rules/magicalGearMechanics.ts"
dst=root/"lib/rules/magicalGearMechanics.ts"
dst.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(src,dst)

# Magical treasure helpers respect Ownership suppression.
rel="lib/rules/magicalTreasure.ts"
t=read(rel)
t=req(t,
"""export function magicalBlessingDice(sheet:CharacterSheet,skill:string):number {
  const key=norm(skill);
  return (sheet.inventory||[]).some(item=>
    ['magnificent-artefact','wondrous-item'].includes(String(item.magicalKind||''))
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)
  ) ? 2 : 0;
}""",
"""export function magicalBlessingDice(sheet:CharacterSheet,skill:string):number {
  const key=norm(skill);
  return (sheet.inventory||[]).some(item=>
    ['magnificent-artefact','wondrous-item'].includes(String(item.magicalKind||''))
    && !(item.magicalCurse==='ownership'&&item.magicalCurseActive&&item.magicalCurseOwnerPresent)
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)
  ) ? 2 : 0;
}""","Ownership blessing dice")
t=req(t,
"""    && Boolean(item.magicalIdentified)
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)""",
"""    && Boolean(item.magicalIdentified)
    && !(item.magicalCurse==='ownership'&&item.magicalCurseActive&&item.magicalCurseOwnerPresent)
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)""",
"Ownership identified blessing")
t=req(t,
"""    && Boolean(entry.magicalIdentified)
    && (entry.magicalBlessings||[]).some(blessing=>norm(blessing)===key)""",
"""    && Boolean(entry.magicalIdentified)
    && !(entry.magicalCurse==='ownership'&&entry.magicalCurseActive&&entry.magicalCurseOwnerPresent)
    && (entry.magicalBlessings||[]).some(blessing=>norm(blessing)===key)""",
"Ownership blessing source")
t=req(t,
"""export function activeFamousQualities(item:CharacterSheet['inventory'][number]):string[] {
  if(item.magicalKind!=='famous-war-gear') return [];
  return activeQualityIds(item.magicalQualities,item.magicalQualitiesUnlocked);
}""",
"""export function activeFamousQualities(item:CharacterSheet['inventory'][number]):string[] {
  if(item.magicalKind!=='famous-war-gear') return [];
  if(item.magicalCurse==='ownership'&&item.magicalCurseActive&&item.magicalCurseOwnerPresent) return [];
  return activeQualityIds(item.magicalQualities,item.magicalQualitiesUnlocked);
}""",
"Ownership qualities")
write(rel,t)

# Shadow Taint is a real floor while active.
rel="lib/rules/shadow.ts"
t=read(rel)
if "curseShadowTaintAmount" not in t:
    t=t.replace(
        "import type { CharacterSheet } from '../db';",
        "import type { CharacterSheet } from '../db';\nimport { curseShadowTaintAmount } from './magicalGearMechanics';",
        1)
t=req(t,
"""  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  next.attributes.shadow=clamp(next.attributes.shadow,scars,max);""",
"""  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  const curseFloor=curseShadowTaintAmount(next);
  const floor=Math.max(scars,curseFloor);
  next.attributes.shadow=clamp(next.attributes.shadow,floor,max);""",
"Shadow Taint floor")
t=req(t,
"""  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  next.attributes.shadow=clamp(Number(next.attributes.shadow)+amount,scars,Math.max(scars,Number(next.attributes.hopeMax)||0));""",
"""  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  const floor=Math.max(scars,curseShadowTaintAmount(next));
  next.attributes.shadow=clamp(Number(next.attributes.shadow)+amount,floor,Math.max(floor,Number(next.attributes.hopeMax)||0));""",
"Shadow Taint adjust")
write(rel,t)

# Equipment load/parry qualities.
rel="lib/rules/equipment.ts"
t=read(rel)
if "enchantedLoad" not in t:
    t=t.replace(
        "import type { CharacterSheet } from '../db';",
        "import type { CharacterSheet } from '../db';\nimport { enchantedLoad, famousShieldStaticParryBonus } from './magicalGearMechanics';",
        1)
t=req(t,
"""  if(armorOrHelm && /resoluto/.test(norm(sheet.other.culturalBlessing))) load=Math.ceil(load/2);
  return load;""",
"""  if(armorOrHelm && /resoluto/.test(norm(sheet.other.culturalBlessing))) load=Math.ceil(load/2);
  load=enchantedLoad(sheet,item,load);
  return load;""",
"enchanted load")
t=req(t,
"""  const base=SHIELDS[norm(item.magicalBaseItemName||item.name)]?.parry ?? 0;
  return base+(itemReward(sheet,item,'Reforçado')?1:0);""",
"""  const base=SHIELDS[norm(item.magicalBaseItemName||item.name)]?.parry ?? 0;
  return base+(itemReward(sheet,item,'Reforçado')?1:0)+famousShieldStaticParryBonus(item);""",
"superior reinforced static parry")
write(rel,t)

# Combat rolls: rune-scored gear, protection result bonus, Ill Luck, complications.
rel="lib/rules/combat.ts"
t=read(rel)
if "magicalGearMechanics" not in t:
    t=t.replace(
        "import { supplementHeavyBlowStrengthBonus, supplementInspired, supplementRollMode } from './supplementMechanics';",
        "import { supplementHeavyBlowStrengthBonus, supplementInspired, supplementRollMode } from './supplementMechanics';\nimport { applyIllLuckToResult, enchantedProtectionResultBonus, runeScoredArmourIgnoresConditions, runeScoredWeaponIgnoresConditions, straightFlightIgnoresComplications } from './magicalGearMechanics';",
        1)
t=t.replace(
"  context:{terrain?:string;isNight?:boolean;escaping?:boolean}={},",
"  context:{terrain?:string;isNight?:boolean;escaping?:boolean;complicationDice?:number}={},",
1)
t=req(t,
"""  const gainGroundNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainGroundNote?.split(':')[1])||0);""",
"""  const gainGroundNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainGroundNote?.split(':')[1])||0);""",
"combat modifier anchor")
t=req(t,
"""  const stanceModifier=context.escaping&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot);
  const baseMode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal';""",
"""  const stanceModifier=context.escaping&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const complications=straightFlightIgnoresComplications(sheet,weapon)?0:Math.max(0,Math.trunc(Number(context.complicationDice)||0));
  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot-complications);
  const baseMode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal';""",
"straight flight complications")
t=req(t,
"""  const roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,actor,target),weapon.group,mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
  return {roll,weapon,ranged};""",
"""  const ignoreConditions=runeScoredWeaponIgnoresConditions(sheet,weapon);
  let roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,actor,target),weapon.group,mode,ignoreConditions?false:Boolean(sheet.attributes.weary),ignoreConditions?false:Boolean(sheet.attributes.miserable));
  roll=applyIllLuckToResult(sheet,roll);
  return {roll,weapon,ranged};""",
"rune weapon and ill luck")
t=req(t,
"""  const mode=forcedMode||baseMode;
  return rollOneRing(Math.max(0,dice),protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));""",
"""  const mode=forcedMode||baseMode;
  const ignoreConditions=runeScoredArmourIgnoresConditions(sheet);
  let roll=rollOneRing(Math.max(0,dice),protectionRollBonus(sheet)+enchantedProtectionResultBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,ignoreConditions?false:Boolean(sheet.attributes.weary),ignoreConditions?false:Boolean(sheet.attributes.miserable));
  roll=applyIllLuckToResult(sheet,roll);
  return roll;""",
"enchanted protection")
write(rel,t)

print("PATCH_4942_RULES_OK")
