from pathlib import Path
import sys

if len(sys.argv)!=2: raise SystemExit("usage: patch_rules.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Combat resolver: actual Strider stance and supplement modifiers.
rel="lib/rules/combat.ts"
t=read(rel)
t=req(
    t,
    "import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollMode, weaponPiercingThreshold } from './equipment';",
    "import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollMode, weaponPiercingThreshold } from './equipment';\nimport { supplementHeavyBlowStrengthBonus, supplementProtectionIllFavouredForTarget } from './supplementMechanics';",
    "combat supplement imports",
)
t=req(
    t,
    "function stanceOrder(stance?:CombatStance):number{return stance==='forward'?0:stance==='open'?1:stance==='defensive'?2:stance==='rearward'?3:4;}",
    "function stanceOrder(stance?:CombatStance):number{return stance==='forward'?0:stance==='open'?1:stance==='defensive'?2:stance==='skirmish'?3:stance==='rearward'?4:5;}",
    "skirmish order",
)
old="""export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false):MasterState{
  if(stance==='rearward'){
    const check=rearwardAllowed(state,characterId,smallFolkException);
    if(!check.allowed) throw new Error(check.reason);
  }
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===characterId?{...actor,stance}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}"""
new="""export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false):MasterState{
  if(stance==='rearward'){
    const check=rearwardAllowed(state,characterId,smallFolkException);
    if(!check.allowed) throw new Error(check.reason);
  }
  if(stance==='skirmish'){
    const actors=state.combat.actors.map(actor=>{
      if(actor.actorId===characterId) return {...actor,stance,engagedWith:[]};
      return {...actor,engagedWith:(actor.engagedWith||[]).filter(id=>id!==characterId)};
    });
    return {...state,combat:{...state.combat,actors,updatedAt:Date.now()},updatedAt:Date.now()};
  }
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===characterId?{...actor,stance}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}"""
t=req(t,old,new,"skirmish stance behavior")
t=req(
    t,
    "  if(actor.stance==='rearward') throw new Error('Um Herói na Retaguarda não pode se engajar em combate corpo a corpo.');",
    "  if(actor.stance==='rearward'||actor.stance==='skirmish') throw new Error(actor.stance==='rearward'?'Um Herói na Retaguarda não pode se engajar em combate corpo a corpo.':'Um Herói em Escaramuça luta em movimento e não se engaja em combate corpo a corpo.');\n  if(target.actorType==='hero'&&target.stance==='skirmish') throw new Error('Um Herói em Escaramuça não fica engajado; ataques corpo a corpo contra ele sofrem a penalidade da postura.');",
    "skirmish engagement",
)
t=req(
    t,
    """export function heroAttackDiceModifier(actor:CombatActorState):number{
  if(actor.stance==='forward') return 1;
  if(actor.stance==='defensive') return -Math.max(1,(actor.engagedWith||[]).length);
  return 0;
}""",
    """export function heroAttackDiceModifier(actor:CombatActorState):number{
  if(actor.stance==='forward') return 1;
  if(actor.stance==='defensive') return -Math.max(1,(actor.engagedWith||[]).length);
  if(actor.stance==='skirmish') return -1;
  return 0;
}""",
    "skirmish attack die",
)
t=req(
    t,
    "  return clamp(20-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);",
    "  const base=sheet.other.rulesMode==='strider'?18:20;\n  return clamp(base-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);",
    "strider combat TN",
)
t=req(
    t,
    """  hopeDice=0,
  inspired=false,
):{roll:RollResult;weapon:NonNullable<CharacterSheet['combat']['weapons'][number]>;ranged:boolean}{""",
    """  hopeDice=0,
  inspired=false,
  extraDice=0,
  forcedMode?:'normal'|'favoured'|'ill-favoured',
  ignoreSkirmishPenalty=false,
):{roll:RollResult;weapon:NonNullable<CharacterSheet['combat']['weapons'][number]>;ranged:boolean}{""",
    "attack extra args",
)
t=req(
    t,
    "  if(actor.stance==='rearward'&&!ranged) throw new Error('Na Retaguarda só é permitido atacar com arma à distância.');",
    "  if(actor.stance==='rearward'&&!ranged) throw new Error('Na Retaguarda só é permitido atacar com arma à distância.');\n  if(actor.stance==='skirmish'&&!ranged) throw new Error('Na Postura de Escaramuça só é permitido atacar com arma à distância.');",
    "skirmish ranged only",
)
t=req(
    t,
    "  const inspiredDice=inspired||automaticInspired(sheet)?Math.max(hopeDice,2):hopeDice;\n  const count=Math.max(0,rank+heroAttackDiceModifier(actor)+inspiredDice);\n  const mode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal';",
    """  const inspiredDice=(hopeDice>0&&(inspired||automaticInspired(sheet)))?Math.max(hopeDice,2):hopeDice;
  const stanceModifier=ignoreSkirmishPenalty&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const count=Math.max(0,rank+stanceModifier+inspiredDice+Math.trunc(Number(extraDice)||0));
  const mode=forcedMode||(Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal');""",
    "attack modifiers",
)
t=req(
    t,
    "  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name);",
    "  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name)+supplementHeavyBlowStrengthBonus(sheet);",
    "great strength heavy blow",
)
t=req(
    t,
    "export function rollHeroProtection(sheet:CharacterSheet,injury:number,rangedPiercing=false):RollResult{\n  const dice=Math.max(0,Number(sheet.attributes.protection)||0);\n  const mode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':protectionRollMode(sheet,rangedPiercing);\n  return rollOneRing(dice,protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));\n}",
    """export function rollHeroProtection(sheet:CharacterSheet,injury:number,rangedPiercing=false,extraDice=0,forcedMode?:'normal'|'favoured'|'ill-favoured'):RollResult{
  const dice=Math.max(0,Number(sheet.attributes.protection)||0)+Math.trunc(Number(extraDice)||0);
  const baseMode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':protectionRollMode(sheet,rangedPiercing);
  const mode=forcedMode||baseMode;
  return rollOneRing(Math.max(0,dice),protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
}""",
    "protection modifiers",
)
write(rel,t)

print("PATCH_4939_RULES_OK")
