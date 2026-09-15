from pathlib import Path

p = Path('app/src/main/assets/www/core/combat-service.js')
s = p.read_text()

imp = "import { materializeWeaponContract } from './wwn-equipment-catalog.js';"
mod_imp = "import { weaponMechanicalBonuses } from './wwn-item-mechanics-core.js';"
if mod_imp not in s:
    s = s.replace(imp, imp + "\n" + mod_imp, 1)

old = "  if(!weapon)return{status:'MISSING_EVIDENCE_WEAPON'};\n  const trait=reloadTrait(item.traits?.length?item:weapon);\n  if(tactical&&trait&&item.loaded!==true)return{status:'WEAPON_NOT_LOADED'};\n  return{status:'OK',weapon,builtIn:false,item,reloadTrait:trait};"
new = "  if(!weapon)return{status:'MISSING_EVIDENCE_WEAPON'};\n  const mechanicalBonuses=weaponMechanicalBonuses(pc,item);\n  if(mechanicalBonuses.rangeMultiplier!==1&&weapon.rangeFeet){weapon=Object.freeze({...weapon,rangeFeet:Object.freeze({normal:weapon.rangeFeet.normal*mechanicalBonuses.rangeMultiplier,long:weapon.rangeFeet.long*mechanicalBonuses.rangeMultiplier})});}\n  const trait=reloadTrait(item.traits?.length?item:weapon);\n  if(tactical&&trait&&item.loaded!==true)return{status:'WEAPON_NOT_LOADED'};\n  return{status:'OK',weapon,builtIn:false,item,reloadTrait:trait,mechanicalBonuses};"
if old in s:
    s = s.replace(old, new, 1)

marker = "const weaponResult=playerWeapon(state,actorId,weaponId,{tactical:Boolean(combat),modeId,combatSkill});if(weaponResult.status!=='OK')return Object.freeze({status:weaponResult.status,revision:loaded.revision});const weapon=weaponResult.weapon;"
bonus = "const weaponBonuses=weaponResult.mechanicalBonuses??{hitBonus:0,damageBonus:0,shockBonus:0};"
attack_start = s.find('  attackNpc({')
if attack_start < 0:
    raise SystemExit('attackNpc not found')
attack_end_candidates = [s.find(x, attack_start + 20) for x in ['\n  endTurn({','\n  enemyAttack({','\n  attackPc({','\n  reloadWeapon({','\n  moveActor({']]
attack_end_candidates = [x for x in attack_end_candidates if x >= 0]
attack_end = min(attack_end_candidates) if attack_end_candidates else len(s)
block = s[attack_start:attack_end]
if bonus not in block:
    if marker not in block:
        raise SystemExit('attackNpc weapon marker not found')
    block = block.replace(marker, marker + bonus, 1)
block = block.replace('hitModifier:hitModifier+tacticalHitModifier,damageBonus,shockBonus','hitModifier:hitModifier+tacticalHitModifier+weaponBonuses.hitBonus,damageBonus:damageBonus+weaponBonuses.damageBonus,shockBonus:shockBonus+weaponBonuses.shockBonus')
block = block.replace('hitModifier:hitModifier+tacticalHitModifier, damageBonus, shockBonus','hitModifier:hitModifier+tacticalHitModifier+weaponBonuses.hitBonus, damageBonus:damageBonus+weaponBonuses.damageBonus, shockBonus:shockBonus+weaponBonuses.shockBonus')
s = s[:attack_start] + block + s[attack_end:]

p.write_text(s)
