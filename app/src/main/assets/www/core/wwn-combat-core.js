import { RNG_ALGORITHM } from './rng-core.js';
import { attributeModifier } from './wwn-rules-core.js';

export const WWN_COMBAT_SOURCES = Object.freeze({
  saves: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.2.0 Saving Throws' }),
  initiative: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.4.1–2.4.2 Combat Sequence / Initiative' }),
  attacks: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.4.5 Combat Attack Rolls' }),
  damage: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.4.6 Damage and Shock' }),
  npcStats: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§5.1.0 Monster and NPC Statistics' }),
  weapons: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§3.4.2–3.4.3 Weapon Statistics / Traits' }),
});

const SAVE_TYPES = Object.freeze(['PHYSICAL', 'EVASION', 'MENTAL', 'LUCK']);


export function unarmedWeaponContract({attribute='Dexterity'}={}) {
  if (!['Strength','Dexterity'].includes(attribute)) throw new RangeError('unarmed attribute must be Strength or Dexterity');
  return Object.freeze({
    weaponId:'UNARMED', name:'Ataque desarmado', combatSkill:'Punch', attribute,
    damage:'1d2', melee:true, shock:null, punchSkillAddsToDamage:true, lessLethal:true,
    sourceRefs:Object.freeze([WWN_COMBAT_SOURCES.weapons]),
  });
}


function requireInt(value, label) {
  if (!Number.isInteger(value)) throw new TypeError(`${label} must be an integer`);
  return value;
}

export function computeSaveTargets(character) {
  if (!character || typeof character !== 'object') throw new TypeError('character required');
  const level = requireInt(character.level, 'level');
  if (level < 1) throw new RangeError('level must be >= 1');
  const a = character.attributes ?? {};
  const str = attributeModifier(a.Strength);
  const dex = attributeModifier(a.Dexterity);
  const con = attributeModifier(a.Constitution);
  const int = attributeModifier(a.Intelligence);
  const wis = attributeModifier(a.Wisdom);
  const cha = attributeModifier(a.Charisma);
  return Object.freeze({
    Physical: 16 - level - Math.max(str, con),
    Evasion: 16 - level - Math.max(dex, int),
    Mental: 16 - level - Math.max(wis, cha),
    Luck: 16 - level,
  });
}

export function resolveSavingThrow({ rng, saveType, target, modifier = 0, streamName = 'save' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  const normalized = String(saveType ?? '').toUpperCase();
  if (!SAVE_TYPES.includes(normalized)) throw new RangeError(`unsupported saveType ${saveType}`);
  requireInt(target, 'target');
  requireInt(modifier, 'modifier');
  const die = rng.die(20, streamName);
  const total = die.result + modifier;
  const success = die.result === 20 ? true : die.result === 1 ? false : total >= target;
  return Object.freeze({
    rule: 'WWN_SAVE_V1', rngAlgorithm: RNG_ALGORITHM, sourceRefs: [WWN_COMBAT_SOURCES.saves],
    saveType: normalized, target, modifier, die, total, naturalOverride: die.result === 1 ? 'AUTO_FAIL' : die.result === 20 ? 'AUTO_SUCCESS' : null,
    success,
  });
}

export function resolveSideInitiative({ rng, bestDexterityScore = null, dexterityModifier = null, streamName = 'initiative' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  let mod = dexterityModifier;
  if (mod === null || mod === undefined) {
    if (bestDexterityScore === null || bestDexterityScore === undefined) mod = 0;
    else mod = attributeModifier(bestDexterityScore);
  }
  requireInt(mod, 'dexterityModifier');
  const die = rng.die(8, streamName);
  return Object.freeze({ rule: 'WWN_SIDE_INITIATIVE_V1', sourceRefs: [WWN_COMBAT_SOURCES.initiative], die, modifier: mod, total: die.result + mod });
}

export function orderInitiativeSides(sides) {
  if (!Array.isArray(sides) || sides.length < 2) throw new TypeError('at least two sides required');
  return [...sides].sort((a, b) => {
    if (b.total !== a.total) return b.total - a.total;
    if (Boolean(a.isPcSide) !== Boolean(b.isPcSide)) return a.isPcSide ? -1 : 1;
    return String(a.sideId).localeCompare(String(b.sideId));
  });
}

export function parseDiceExpression(expression) {
  const m = /^\s*(\d+)d(\d+)(?:\s*([+-])\s*(\d+))?\s*$/i.exec(String(expression ?? ''));
  if (!m) throw new RangeError(`invalid dice expression ${expression}`);
  const count = Number(m[1]); const sides = Number(m[2]);
  const bonus = m[3] ? (m[3] === '-' ? -1 : 1) * Number(m[4]) : 0;
  if (!Number.isInteger(count) || count < 1 || count > 50 || !Number.isInteger(sides) || sides < 2) throw new RangeError('invalid dice expression range');
  return Object.freeze({ count, sides, bonus });
}

export function rollDiceExpression({ rng, expression, streamName = 'damage' }) {
  const spec = parseDiceExpression(expression);
  const dice = [];
  let rawTotal = 0;
  for (let i = 0; i < spec.count; i += 1) {
    const die = rng.die(spec.sides, streamName);
    dice.push(die); rawTotal += die.result;
  }
  return Object.freeze({ expression, dice, rawTotal, expressionBonus: spec.bonus, total: rawTotal + spec.bonus });
}

function combatSkillContribution(skills, skillName) {
  const level = skills?.[skillName];
  if (level === null || level === undefined) return { skillLevel: null, contribution: -2, untrained: true };
  requireInt(level, 'combat skill level');
  if (level < 0) throw new RangeError('combat skill level must be >= 0');
  return { skillLevel: level, contribution: level, untrained: false };
}

function validateWeapon(weapon) {
  if (!weapon || typeof weapon !== 'object') throw new TypeError('weapon contract required');
  if (!['Stab', 'Shoot', 'Punch', 'Exert'].includes(weapon.combatSkill)) throw new RangeError('weapon combatSkill must be Stab, Shoot, Punch, or Exert');
  if (!['Strength', 'Dexterity', 'Constitution', 'Intelligence', 'Wisdom', 'Charisma'].includes(weapon.attribute)) throw new RangeError('weapon attribute invalid');
  parseDiceExpression(weapon.damage);
  if (weapon.shock) {
    requireInt(weapon.shock.damage, 'shock.damage');
    if (!(weapon.shock.ac === null || weapon.shock.ac === undefined || Number.isInteger(weapon.shock.ac))) throw new TypeError('shock.ac must be integer or null');
  }
}

export function resolveAttackFromPhysicalDice({ hitFace, damageFaces = [], attacker, weapon, target, hitModifier = 0, damageBonus = 0, shockBonus = 0, physicalMeta = null }) {
  validateWeapon(weapon);
  if (!attacker || !target) throw new TypeError('attacker and target required');
  requireInt(attacker.baseAttackBonus, 'baseAttackBonus');
  requireInt(hitModifier, 'hitModifier'); requireInt(damageBonus, 'damageBonus'); requireInt(shockBonus, 'shockBonus');
  if (!Number.isInteger(hitFace) || hitFace < 1 || hitFace > 20) throw new RangeError('physical hit face must be 1..20');
  if (physicalMeta?.forcedSettle === true) throw new Error('forced physical settle is not authoritative');
  const attributeScore = attacker.attributes?.[weapon.attribute];
  const attributeMod = attributeModifier(attributeScore);
  const skill = combatSkillContribution(attacker.skills, weapon.combatSkill);
  const relevantAc = weapon.melee === false ? target.rangedAC : target.meleeAC;
  requireInt(relevantAc, 'target AC');
  const hitDie = Object.freeze({result:hitFace,sides:20,source:'PHYSICAL_3D_TOP_FACE'});
  const hitTotal = hitFace + attacker.baseAttackBonus + attributeMod + skill.contribution + hitModifier;
  const hit = hitTotal >= relevantAc;
  const shockEligible = Boolean(weapon.shock) && weapon.melee !== false && !target.shockImmune && (weapon.shock.ac === null || weapon.shock.ac === undefined || target.meleeAC <= weapon.shock.ac);
  const shockDamage = shockEligible ? Math.max(0, weapon.shock.damage + attributeMod + shockBonus) : 0;
  const spec = parseDiceExpression(weapon.damage);
  if (!Array.isArray(damageFaces) || damageFaces.length !== spec.count || damageFaces.some(v=>!Number.isInteger(v)||v<1||v>spec.sides)) {
    throw new RangeError(`physical damage requires ${spec.count} settled d${spec.sides} face(s)`);
  }
  const damageDice = damageFaces.map((result,index)=>Object.freeze({result,sides:spec.sides,index,source:'PHYSICAL_3D_TOP_FACE'}));
  const rawTotal = damageFaces.reduce((a,b)=>a+b,0);
  const physicalDamageRoll = Object.freeze({expression:weapon.damage,dice:Object.freeze(damageDice),rawTotal,expressionBonus:spec.bonus,total:rawTotal+spec.bonus});
  let damageRoll = null, preShockDamage = 0, damage = 0, damageKind = 'NONE';
  if (hit) {
    damageRoll = physicalDamageRoll;
    let damageTotal = damageRoll.total + attributeMod + damageBonus;
    if (weapon.punchSkillAddsToDamage && weapon.combatSkill === 'Punch' && skill.skillLevel !== null) damageTotal += skill.skillLevel;
    preShockDamage = Math.max(0, damageTotal);
    damage = shockEligible ? Math.max(preShockDamage, shockDamage) : preShockDamage;
    damageKind = 'HIT';
  } else if (shockEligible) { damage = shockDamage; damageKind = 'SHOCK'; }
  return Object.freeze({
    rule:'WWN_ATTACK_V1',rngAlgorithm:'PHYSICAL_3D_V1',diceAuthority:'PHYSICAL_TOP_FACE_AFTER_SETTLE',sourceRefs:[WWN_COMBAT_SOURCES.attacks,WWN_COMBAT_SOURCES.damage],
    hitDie,hitTotal,hit,relevantAc,baseAttackBonus:attacker.baseAttackBonus,attribute:weapon.attribute,attributeScore,attributeModifier:attributeMod,
    combatSkill:weapon.combatSkill,skillLevel:skill.skillLevel,skillContribution:skill.contribution,untrained:skill.untrained,hitModifier,damageRoll,damageBonus,shockBonus,
    shockEligible,shockDamage,preShockDamage,damage,damageKind,
    physicalDice:physicalMeta?Object.freeze({seed:String(physicalMeta.seed??''),physicsMs:Number(physicalMeta.physicsMs??0),visualQuality:String(physicalMeta.visualQuality??''),forcedSettle:false,allFaces:Object.freeze([hitFace,...damageFaces])}):null,
  });
}

export function resolveAutomaticWeaponHitDamage({rng,attacker,weapon,damageBonus=0,streamName='automatic-hit'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');validateWeapon(weapon);if(!attacker)throw new TypeError('attacker required');requireInt(damageBonus,'damageBonus');const attributeScore=attacker.attributes?.[weapon.attribute],attributeMod=attributeModifier(attributeScore),skill=combatSkillContribution(attacker.skills,weapon.combatSkill),damageRoll=rollDiceExpression({rng,expression:weapon.damage,streamName:`${streamName}/damage`});let damageTotal=damageRoll.total+attributeMod+damageBonus;if(weapon.punchSkillAddsToDamage&&weapon.combatSkill==='Punch'&&skill.skillLevel!==null)damageTotal+=skill.skillLevel;return Object.freeze({rule:'WWN_AUTOMATIC_WEAPON_HIT_DAMAGE_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_COMBAT_SOURCES.damage,WWN_COMBAT_SOURCES.weapons],attribute:weapon.attribute,attributeScore,attributeModifier:attributeMod,combatSkill:weapon.combatSkill,skillLevel:skill.skillLevel,damageRoll,damageBonus,damage:Math.max(0,damageTotal),damageKind:'AUTOMATIC_HIT'});
}

export function resolveAttackHitOnly({ rng, attacker, weapon, target, hitModifier = 0, streamName = 'attack-hit-only' }) {
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

export function resolveAttack({ rng, attacker, weapon, target, hitModifier = 0, damageBonus = 0, shockBonus = 0, streamName = 'attack' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  validateWeapon(weapon);
  if (!attacker || !target) throw new TypeError('attacker and target required');
  requireInt(attacker.baseAttackBonus, 'baseAttackBonus');
  requireInt(hitModifier, 'hitModifier'); requireInt(damageBonus, 'damageBonus'); requireInt(shockBonus, 'shockBonus');
  const attributeScore = attacker.attributes?.[weapon.attribute];
  const attributeMod = attributeModifier(attributeScore);
  const skill = combatSkillContribution(attacker.skills, weapon.combatSkill);
  const relevantAc = weapon.melee === false ? target.rangedAC : target.meleeAC;
  requireInt(relevantAc, 'target AC');

  const hitDie = rng.die(20, `${streamName}/hit`);
  const hitTotal = hitDie.result + attacker.baseAttackBonus + attributeMod + skill.contribution + hitModifier;
  const hit = hitTotal >= relevantAc;

  const shockEligible = Boolean(weapon.shock) && weapon.melee !== false && !target.shockImmune && (weapon.shock.ac === null || weapon.shock.ac === undefined || target.meleeAC <= weapon.shock.ac);
  const shockDamage = shockEligible ? Math.max(0, weapon.shock.damage + attributeMod + shockBonus) : 0;

  let damageRoll = null;
  let preShockDamage = 0;
  let damage = 0;
  let damageKind = 'NONE';
  if (hit) {
    damageRoll = rollDiceExpression({ rng, expression: weapon.damage, streamName: `${streamName}/damage` });
    let damageTotal = damageRoll.total + attributeMod + damageBonus;
    if (weapon.punchSkillAddsToDamage && weapon.combatSkill === 'Punch' && skill.skillLevel !== null) damageTotal += skill.skillLevel;
    preShockDamage = Math.max(0, damageTotal);
    damage = shockEligible ? Math.max(preShockDamage, shockDamage) : preShockDamage;
    damageKind = 'HIT';
  } else if (shockEligible) {
    damage = shockDamage;
    damageKind = 'SHOCK';
  }

  return Object.freeze({
    rule: 'WWN_ATTACK_V1', rngAlgorithm: RNG_ALGORITHM, sourceRefs: [WWN_COMBAT_SOURCES.attacks, WWN_COMBAT_SOURCES.damage],
    hitDie, hitTotal, hit, relevantAc, baseAttackBonus: attacker.baseAttackBonus,
    attribute: weapon.attribute, attributeScore, attributeModifier: attributeMod,
    combatSkill: weapon.combatSkill, skillLevel: skill.skillLevel, skillContribution: skill.contribution, untrained: skill.untrained,
    hitModifier, damageRoll, damageBonus, shockBonus, shockEligible, shockDamage, preShockDamage, damage, damageKind,
  });
}

export function resolveNpcAttack({ rng, npc, weapon, target, hitModifier = 0, damageBonus = 0, shockBonus = 0, streamName = 'npc-attack' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  validateWeapon(weapon);
  if (!npc || !target) throw new TypeError('npc and target required');
  requireInt(npc.attackBonus, 'npc.attackBonus');
  requireInt(hitModifier, 'hitModifier'); requireInt(damageBonus, 'damageBonus'); requireInt(shockBonus, 'shockBonus');
  const relevantAc = weapon.melee === false ? (target.rangedAC ?? target.armorClass) : (target.meleeAC ?? target.armorClass);
  requireInt(relevantAc, 'target AC');
  const hitDie = rng.die(20, `${streamName}/hit`);
  const hitTotal = hitDie.result + npc.attackBonus + hitModifier;
  const hit = hitTotal >= relevantAc;
  const shockEligible = Boolean(weapon.shock) && weapon.melee !== false && !target.shockImmune && (weapon.shock.ac === null || weapon.shock.ac === undefined || relevantAc <= weapon.shock.ac);
  const shockDamage = shockEligible ? Math.max(0, weapon.shock.damage + shockBonus) : 0;
  let damageRoll = null, preShockDamage = 0, damage = 0, damageKind = 'NONE';
  if (hit) {
    damageRoll = rollDiceExpression({ rng, expression: weapon.damage, streamName: `${streamName}/damage` });
    preShockDamage = Math.max(0, damageRoll.total + damageBonus);
    damage = shockEligible ? Math.max(preShockDamage, shockDamage) : preShockDamage;
    damageKind = 'HIT';
  } else if (shockEligible) {
    damage = shockDamage; damageKind = 'SHOCK';
  }
  return Object.freeze({
    rule:'WWN_NPC_ATTACK_V1', rngAlgorithm:RNG_ALGORITHM,
    sourceRefs:[WWN_COMBAT_SOURCES.attacks, WWN_COMBAT_SOURCES.damage, WWN_COMBAT_SOURCES.npcStats],
    hitDie, hitTotal, hit, relevantAc, attackBonus:npc.attackBonus, hitModifier,
    damageRoll, damageBonus, shockBonus, shockEligible, shockDamage, preShockDamage, damage, damageKind,
  });
}

