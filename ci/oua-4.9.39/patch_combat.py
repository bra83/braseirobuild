from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_combat.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Extend combat command special with Strider Escape.
types_rel="lib/master/types.ts"
types=read(types_rel)
types=types.replace(
"special?: 'heavy' | 'pierce' | 'defend';",
"special?: 'heavy' | 'pierce' | 'defend' | 'escape';"
)
write(types_rel,types)

rel="lib/rules/combat.ts"
t=read(rel)
t=req(t,
"import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollMode, weaponPiercingThreshold } from './equipment';",
"""import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollMode, weaponPiercingThreshold } from './equipment';
import { canUseSkirmishWeapon, skirmishHeroAttackDiceModifier, supplementBonusDice, supplementHeavyBlowStrengthBonus, supplementInspired, supplementRollMode } from './supplementMechanics';""",
"supplement combat import")

# Skirmish is a ranged stance and may not engage.
t=t.replace(
"if(actor.stance==='rearward') throw new Error('Um Herói na Retaguarda não pode se engajar em combate corpo a corpo.');",
"if(actor.stance==='rearward'||actor.stance==='skirmish') throw new Error('Um Herói na Retaguarda/Escaramuça não pode se engajar em combate corpo a corpo.');"
)

# Strider uses base TN 18 also in combat.
t=req(t,
"""export function heroAttackTargetNumber(sheet:CharacterSheet,target:CombatActorState):number{
  const modifier=Number(sheet.attributes.tnModifiers?.strength||0);
  return clamp(20-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);
}""",
"""export function heroAttackTargetNumber(sheet:CharacterSheet,target:CombatActorState):number{
  const modifier=Number(sheet.attributes.tnModifiers?.strength||0);
  const base=sheet.other.rulesMode==='strider'?18:20;
  return clamp(base-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);
}""","Strider combat TN")

# Replace hero attack resolver with supplement-aware variant.
start=t.index("export function rollHeroAttack(")
end=t.index("\nexport function attackIsPiercing",start)
new_attack="""export function rollHeroAttack(
  sheet:CharacterSheet,
  actor:CombatActorState,
  target:CombatActorState,
  weaponIndex:number,
  hopeDice=0,
  inspired=false,
  context:{terrain?:string;isNight?:boolean;escaping?:boolean}={},
):{roll:RollResult;weapon:NonNullable<CharacterSheet['combat']['weapons'][number]>;ranged:boolean}{
  const weapon=sheet.combat.weapons[Math.max(0,Math.min(sheet.combat.weapons.length-1,Math.trunc(weaponIndex)))]||sheet.combat.weapons[0];
  if(!weapon) throw new Error('O Herói não possui arma configurada.');
  const ranged=isRangedWeapon(weapon.name,weapon.group);
  if(actor.stance==='rearward'&&!ranged) throw new Error('Na Retaguarda só é permitido atacar com arma à distância.');
  if(!canUseSkirmishWeapon(String(actor.stance||''),ranged)) throw new Error('Na postura Escaramuça só é permitido usar armas à distância.');
  if(actor.stance!=='rearward'&&actor.stance!=='skirmish'&&ranged&&actor.engagedWith.length>0) throw new Error('Um combatente engajado não pode usar livremente uma arma à distância.');
  const rank=Math.max(0,Number(sheet.combat.proficiencies?.[weapon.group]||0));
  const supplementIsInspired=supplementInspired(sheet,{kind:'attack',terrain:context.terrain,isNight:context.isNight,stance:actor.stance,weaponName:weapon.name,weaponGroup:weapon.group});
  const inspiredDice=inspired||automaticInspired(sheet)||supplementIsInspired?Math.max(hopeDice,2):hopeDice;
  const gainNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainNote?.split(':')[1])||0);
  const skirmish=skirmishHeroAttackDiceModifier(String(actor.stance||''),ranged,Boolean(context.escaping));
  const count=Math.max(0,rank+heroAttackDiceModifier(actor)+inspiredDice+gainGround+skirmish);
  const baseMode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal';
  const mode=supplementRollMode(sheet,{kind:'attack',terrain:context.terrain,isNight:context.isNight,combatRound:0,stance:actor.stance,weaponName:weapon.name,weaponGroup:weapon.group},baseMode);
  const roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,target),weapon.group,mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
  return {roll,weapon,ranged};
}"""
t=t[:start]+new_attack+t[end:]

# Great Strength adds +2 Strength-equivalent to Heavy Blow.
t=req(t,
"""  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name);""",
"""  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name)+supplementHeavyBlowStrengthBonus(sheet);""",
"Great Strength heavy blow")

# Protection: Beorning Furious and Hide Coat.
start=t.index("export function rollHeroProtection(")
end=t.index("\nexport function rollActorProtection",start)
new_protection="""export function rollHeroProtection(sheet:CharacterSheet,injury:number,rangedPiercing=false):RollResult{
  const extra=supplementBonusDice(sheet,{kind:'protection'});
  const dice=Math.max(0,Number(sheet.attributes.protection)||0)+extra;
  const equipmentMode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':protectionRollMode(sheet,rangedPiercing);
  const mode=supplementRollMode(sheet,{kind:'protection'},equipmentMode);
  return rollOneRing(dice,protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
}"""
t=t[:start]+new_protection+t[end:]

# Actor protection can be made Ill-favoured by Breaking Blow.
t=t.replace(
"export function rollActorProtection(actor:CombatActorState,injury:number):RollResult{\n  return rollOneRing(Math.max(0,Number(actor.protection)||0),0,Math.max(2,Number(injury)||2),'Proteção','normal',false,false);\n}",
"export function rollActorProtection(actor:CombatActorState,injury:number,mode:'favoured'|'normal'|'ill-favoured'='normal'):RollResult{\n  return rollOneRing(Math.max(0,Number(actor.protection)||0),0,Math.max(2,Number(injury)||2),'Proteção',mode,false,false);\n}"
)
write(rel,t)

print("PATCH_4939_COMBAT_OK")
