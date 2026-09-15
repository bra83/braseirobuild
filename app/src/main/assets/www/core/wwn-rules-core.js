import { RNG_ALGORITHM } from './rng-core.js';

export const WWN_RULE_SOURCES = Object.freeze({
  attributeModifiers: Object.freeze({
    sourceId: 'SRC-WWN-SRD-1',
    locator: '§1.1.2 Attribute Modifiers',
  }),
  skillChecks: Object.freeze({
    sourceId: 'SRC-WWN-SRD-1',
    locator: '§2.3.0 Skill Checks',
  }),
  skillDifficulties: Object.freeze({
    sourceId: 'SRC-WWN-SRD-1',
    locator: '§2.3.1 Skill Check Difficulties',
  }),
  aidingSkillChecks: Object.freeze({
    sourceId: 'SRC-WWN-SRD-1',
    locator: '§2.3.3 Aiding a Skill Check',
  }),
});

export function attributeModifier(score) {
  if (!Number.isInteger(score) || score < 3 || score > 18) {
    throw new RangeError('WWN attribute score must be an integer from 3 to 18');
  }
  if (score === 3) return -2;
  if (score <= 7) return -1;
  if (score <= 13) return 0;
  if (score <= 17) return 1;
  return 2;
}

export function validateMinimalCoreCharacter(character) {
  const errors = [];
  const required = ['Strength', 'Dexterity', 'Constitution', 'Intelligence', 'Wisdom', 'Charisma'];
  if (!character || typeof character !== 'object') return { ok: false, errors: ['character required'] };
  if (character.level !== 1) errors.push('Wave 3 core fixture must be level 1');
  for (const name of required) {
    const score = character.attributes?.[name];
    if (!Number.isInteger(score) || score < 3 || score > 18) errors.push(`${name} must be 3..18`);
  }
  for (const [skill, level] of Object.entries(character.skills ?? {})) {
    if (!Number.isInteger(level) || level < 0 || level > 1) errors.push(`${skill} level must be 0..1 at level 1 in Wave 3 subset`);
  }
  return { ok: errors.length === 0, errors };
}

export function resolveSkillCheck({
  rng,
  skillLevel,
  attributeScore,
  difficulty,
  situationalModifier = 0,
  streamName = 'skill-check',
}) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  if (!(skillLevel === null || skillLevel === undefined || (Number.isInteger(skillLevel) && skillLevel >= 0))) {
    throw new RangeError('skillLevel must be null/untrained or integer >= 0');
  }
  if (!Number.isInteger(difficulty) || difficulty < 2) throw new RangeError('difficulty must be integer >= 2');
  if (!Number.isInteger(situationalModifier) || situationalModifier < -2 || situationalModifier > 2) {
    throw new RangeError('situationalModifier must be -2..+2 for ordinary circumstances');
  }

  const attributeMod = attributeModifier(attributeScore);
  const skillContribution = skillLevel ?? -1;
  const d1 = rng.die(6, streamName);
  const d2 = rng.die(6, streamName);
  const diceTotal = d1.result + d2.result;
  const total = diceTotal + skillContribution + attributeMod + situationalModifier;
  return Object.freeze({
    rule: 'WWN_SKILL_CHECK_CORE_V1',
    rngAlgorithm: rng.algorithm ?? RNG_ALGORITHM,
    sourceRefs: [WWN_RULE_SOURCES.skillChecks, WWN_RULE_SOURCES.attributeModifiers, WWN_RULE_SOURCES.skillDifficulties],
    dice: [d1, d2],
    diceTotal,
    skillLevel: skillLevel ?? null,
    skillContribution,
    attributeScore,
    attributeModifier: attributeMod,
    situationalModifier,
    difficulty,
    total,
    success: total >= difficulty,
  });
}


export function resolveSkillCheckFromPhysicalDice({
  faces,
  skillLevel,
  attributeScore,
  difficulty,
  situationalModifier = 0,
  physicalMeta = null,
}) {
  if (physicalMeta?.forcedSettle === true) throw new Error('forced physical settle is not authoritative');
  if (physicalMeta) {
    if (physicalMeta.provider !== 'ANDROID_NATIVE_PHYSICS' || physicalMeta.algorithm !== 'physical-dice-top-face-v1') {
      throw new Error('Physical dice proof required');
    }
    const formula = String(physicalMeta.formula ?? '').toLowerCase().replace(/\s+/g, '');
    if (formula !== '2d6') throw new Error(`Unexpected physical formula ${formula}`);
  }
  if (!Array.isArray(faces) || faces.length !== 2 || faces.some(v => !Number.isInteger(Number(v)) || Number(v) < 1 || Number(v) > 6)) {
    throw new RangeError('Invalid physical 2d6 faces');
  }
  const settled = faces.map(Number);
  let at = 0;
  const rng = {
    algorithm: 'physical-dice-top-face-v1',
    die(sides, streamName = 'physical-dice') {
      if (sides !== 6 || at >= settled.length) throw new Error('Physical dice consumption mismatch');
      const result = settled[at++];
      return Object.freeze({algorithm:'physical-dice-top-face-v1',stream:streamName,counterBefore:null,rawHex:null,sides,result,physical:true,source:'PHYSICAL_3D_TOP_FACE'});
    },
  };
  return resolveSkillCheck({rng,skillLevel,attributeScore,difficulty,situationalModifier,streamName:'physical-dice/skill-check'});
}


export function resolveAidingSkillCheck({
  rng,
  skillLevel,
  attributeScore,
  difficulty,
  situationalModifier = 0,
  streamName = 'skill-check/aid',
}) {
  const check = resolveSkillCheck({rng, skillLevel, attributeScore, difficulty, situationalModifier, streamName});
  return Object.freeze({
    rule: 'WWN_AID_SKILL_CHECK_V1',
    sourceRefs: [WWN_RULE_SOURCES.aidingSkillChecks, ...check.sourceRefs],
    check,
    success: check.success,
    allyBonus: check.success ? 1 : 0,
    maximumCombinedAidBonus: 1,
  });
}

export function combineAidBonuses(aidResults = []) {
  if (!Array.isArray(aidResults)) throw new TypeError('aidResults must be an array');
  const successful = aidResults.filter(v => v?.success === true).length;
  return Object.freeze({
    rule: 'WWN_AID_BONUS_CAP_V1',
    sourceRefs: [WWN_RULE_SOURCES.aidingSkillChecks],
    successfulHelpers: successful,
    bonus: successful > 0 ? 1 : 0,
    cap: 1,
  });
}
