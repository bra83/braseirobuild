from pathlib import Path

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# --- wwn-combat-core.js: source-bound hit-only attack, used by no-damage maneuvers ---
p=Path('app/src/main/assets/www/core/wwn-combat-core.js');s=p.read_text()
marker="export function resolveAttack({ rng, attacker, weapon, target, hitModifier = 0, damageBonus = 0, shockBonus = 0, streamName = 'attack' }) {"
fn="""export function resolveAttackHitOnly({ rng, attacker, weapon, target, hitModifier = 0, streamName = 'attack-hit-only' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  validateWeapon(weapon);
  if (!attacker || !target) throw new TypeError('attacker and target required');
  requireInt(attacker.baseAttackBonus, 'baseAttackBonus');requireInt(hitModifier, 'hitModifier');
  const attributeScore = attacker.attributes?.[weapon.attribute];
  const attributeMod = attributeModifier(attributeScore);
  const skill = combatSkillContribution(attacker.skills, weapon.combatSkill);
  const relevantAc = weapon.melee === false ? target.rangedAC : target.meleeAC;requireInt(relevantAc, 'target AC');
  const hitDie = rng.die(20, `${streamName}/hit`);
  const hitTotal = hitDie.result + attacker.baseAttackBonus + attributeMod + skill.contribution + hitModifier;
  const hit = hitTotal >= relevantAc;
  return Object.freeze({rule:'WWN_ATTACK_HIT_ONLY_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_COMBAT_SOURCES.attacks],hitDie,hitTotal,hit,relevantAc,baseAttackBonus:attacker.baseAttackBonus,attribute:weapon.attribute,attributeScore,attributeModifier:attributeMod,combatSkill:weapon.combatSkill,skillLevel:skill.skillLevel,skillContribution:skill.contribution,untrained:skill.untrained,hitModifier,damage:0,damageKind:'NONE',damageRolled:false});
}

"""
if 'export function resolveAttackHitOnly(' not in s:
    if marker not in s: raise SystemExit('resolveAttack marker missing')
    s=s.replace(marker,fn+marker,1)
p.write_text(s)

# --- wwn-special-combat-core.js: opposed skill rolls with maneuver-size modifiers ---
p=Path('app/src/main/assets/www/core/wwn-special-combat-core.js');s=p.read_text()
marker="export function shoveOutcome({attackHit,attackerOpposedTotal=null,defenderOpposedTotal=null,attackerIsPc=false,defenderIsPc=false}){"
fn="""export function resolvePcManeuverSkillRoll({rng,attributeScore,skillLevel=null,modifier=0,streamName='combat/maneuver/pc'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('rng required');int(attributeScore,'attributeScore',{min:3,max:18});if(!(skillLevel===null||skillLevel===undefined||(Number.isInteger(skillLevel)&&skillLevel>=0)))throw new RangeError('skillLevel must be null/untrained or integer >= 0');int(modifier,'modifier');
  const d1=rng.die(6,streamName),d2=rng.die(6,streamName),attributeMod=attributeModifier(attributeScore),skillContribution=skillLevel??-1,total=d1.result+d2.result+attributeMod+skillContribution+modifier;
  return Object.freeze({rule:'WWN_MANEUVER_PC_OPPOSED_ROLL_V1',dice:Object.freeze([d1,d2]),attributeScore,attributeModifier:attributeMod,skillLevel:skillLevel??null,skillContribution,modifier,total,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function resolveNpcManeuverSkillRoll({rng,skillBonus,modifier=0,streamName='combat/maneuver/npc'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('rng required');int(skillBonus,'skillBonus');int(modifier,'modifier');const d1=rng.die(6,streamName),d2=rng.die(6,streamName),total=d1.result+d2.result+skillBonus+modifier;
  return Object.freeze({rule:'WWN_MANEUVER_NPC_OPPOSED_ROLL_V1',dice:Object.freeze([d1,d2]),skillBonus,modifier,total,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}

"""
if 'export function resolvePcManeuverSkillRoll(' not in s:
    if marker not in s: raise SystemExit('shoveOutcome marker missing')
    s=s.replace(marker,fn+marker,1)
p.write_text(s)

# --- NPC profile: explicit grapple/shove size evidence for starter human ---
p=Path('app/src/main/assets/www/core/wwn-npc-combat.js');s=p.read_text()
s=replace_once(s,
"THUG_OR_MILITIA:Object.freeze({profileId:'THUG_OR_MILITIA',hitDice:1,armorClass:13,attackBonus:1,moveFeet:30,morale:8,instinct:4,skillBonus:1,saveTarget:15,importantNamed:false,sourceRefs:Object.freeze([WWN_NPC_COMBAT_SOURCES.stats])})",
"THUG_OR_MILITIA:Object.freeze({profileId:'THUG_OR_MILITIA',hitDice:1,armorClass:13,attackBonus:1,moveFeet:30,morale:8,instinct:4,skillBonus:1,saveTarget:15,importantNamed:false,grappleSizeClass:'HUMAN_SIZED',sourceRefs:Object.freeze([WWN_NPC_COMBAT_SOURCES.stats])})",
'npc size profile')
s=replace_once(s,
"importantNamed:profile.importantNamed,weaponId,weaponContract:weapon,sourceRefs:Object.freeze([...profile.sourceRefs,...weapon.sourceRefs])",
"importantNamed:profile.importantNamed,grappleSizeClass:profile.grappleSizeClass??null,weaponId,weaponContract:weapon,sourceRefs:Object.freeze([...profile.sourceRefs,...weapon.sourceRefs])",
'npc size instance')
p.write_text(s)

print('WWN shove core patch applied')
