from pathlib import Path

def replace_once(text, old, new, label):
    if new in text:return text
    if old not in text:raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# Damage-only resolution for automatic grapple damage at end of round.
p=Path('app/src/main/assets/www/core/wwn-combat-core.js');s=p.read_text()
marker="export function resolveAttackHitOnly({ rng, attacker, weapon, target, hitModifier = 0, streamName = 'attack-hit-only' }) {"
fn="""export function resolveAutomaticWeaponHitDamage({rng,attacker,weapon,damageBonus=0,streamName='automatic-hit'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');validateWeapon(weapon);if(!attacker)throw new TypeError('attacker required');requireInt(damageBonus,'damageBonus');const attributeScore=attacker.attributes?.[weapon.attribute],attributeMod=attributeModifier(attributeScore),skill=combatSkillContribution(attacker.skills,weapon.combatSkill),damageRoll=rollDiceExpression({rng,expression:weapon.damage,streamName:`${streamName}/damage`});let damageTotal=damageRoll.total+attributeMod+damageBonus;if(weapon.punchSkillAddsToDamage&&weapon.combatSkill==='Punch'&&skill.skillLevel!==null)damageTotal+=skill.skillLevel;return Object.freeze({rule:'WWN_AUTOMATIC_WEAPON_HIT_DAMAGE_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_COMBAT_SOURCES.damage,WWN_COMBAT_SOURCES.weapons],attribute:weapon.attribute,attributeScore,attributeModifier:attributeMod,combatSkill:weapon.combatSkill,skillLevel:skill.skillLevel,damageRoll,damageBonus,damage:Math.max(0,damageTotal),damageKind:'AUTOMATIC_HIT'});
}

"""
if 'export function resolveAutomaticWeaponHitDamage(' not in s:
    if marker not in s:raise SystemExit('hit-only marker missing')
    s=s.replace(marker,fn+marker,1)
p.write_text(s)
print('WWN grapple core patch applied')
