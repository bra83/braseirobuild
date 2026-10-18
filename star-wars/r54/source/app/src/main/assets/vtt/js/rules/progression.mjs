import { EOTE_CAREERS, EOTE_CORE } from './eoteCharacterCreation.mjs';
import { EOTE_UNIVERSAL_SPECIALIZATIONS } from './forceData.mjs';
import { AOR_CAREERS } from './characterCreation.mjs';
import { AOR_TALENT_TREES } from '../expansion/aorTalentTreesData.mjs';
import { FND_CAREERS, FND_CORE } from './fndCharacterCreation.mjs';
import { FND_TALENT_TREES } from '../expansion/fndTalentTreesData.mjs';
import { grantMinimumForceRating } from './force.mjs';

export const MAX_SKILL_RANK = 5;

export function skillRankCost(currentRank, isCareer) {
  if (!Number.isInteger(currentRank) || currentRank < 0 || currentRank >= MAX_SKILL_RANK) {
    throw new Error(`O grau atual deve estar entre 0 e ${MAX_SKILL_RANK - 1}.`);
  }
  if (typeof isCareer !== 'boolean') throw new Error('É preciso saber se a perícia é profissional.');
  const newRank = currentRank + 1;
  return newRank * 5 + (isCareer ? 0 : 5);
}

export function spendXpOnSkill(character, skillKey, { isCareer } = {}) {
  if (!character?.skills || !(skillKey in character.skills)) throw new Error('Perícia inexistente na ficha.');
  const currentRank = character.skills[skillKey];
  const cost = skillRankCost(currentRank, isCareer);
  if (!Number.isInteger(character.xp?.available) || character.xp.available < cost) {
    throw new Error(`PE insuficientes: são necessários ${cost}.`);
  }
  const next = structuredClone(character);
  next.skills[skillKey] = currentRank + 1;
  next.xp.available -= cost;
  return { character: next, cost, newRank: currentRank + 1 };
}


export function isCareerSkill(character, skillKey) {
  if (!character || typeof character!=='object') throw new Error('Ficha ausente.');
  return Array.isArray(character.careerSkills) && character.careerSkills.includes(skillKey);
}

export function skillRankCostForCharacter(character, skillKey) {
  if (!character?.skills || !(skillKey in character.skills)) throw new Error('Perícia inexistente na ficha.');
  return skillRankCost(character.skills[skillKey], isCareerSkill(character,skillKey));
}

export function spendXpOnSkillForCharacter(character, skillKey) {
  return spendXpOnSkill(character,skillKey,{isCareer:isCareerSkill(character,skillKey)});
}

export function grantExperience(character, amount, {reason='Experiência concedida pelo Mestre'}={}) {
  if (!character?.xp) throw new Error('Ficha sem controle de Experiência.');
  const xp=Number(amount);
  if (!Number.isInteger(xp) || xp<0 || xp>1000) throw new Error('Quantidade de PE inválida.');
  const next=structuredClone(character);
  next.xp.total+=xp;
  next.xp.available+=xp;
  return {character:next,amount:xp,reason:String(reason ?? '').trim() || 'Experiência concedida pelo Mestre'};
}


function specializationEntry(careerKey, specializationKey, profile = EOTE_CORE) {
  if (profile === 'FND_CORE') {
    const career = FND_CAREERS?.[careerKey];
    const specialization = career?.specializations?.[specializationKey];
    return career && specialization && FND_TALENT_TREES?.[careerKey]?.[specializationKey]
      ? { careerKey, specializationKey, career, specialization, universal: false } : null;
  }
  if (profile === 'AOR_CORE') {
    const career = AOR_CAREERS?.[careerKey];
    const specialization = career?.specializations?.[specializationKey];
    // Only source-backed AoR core trees can be acquired.
    return career && specialization && AOR_TALENT_TREES?.[careerKey]?.[specializationKey]
      ? { careerKey, specializationKey, career, specialization, universal: false } : null;
  }
  if (careerKey === 'universal') {
    const specialization = EOTE_UNIVERSAL_SPECIALIZATIONS?.[specializationKey] ?? null;
    return specialization ? { careerKey, specializationKey, career: { label: 'Universal' }, specialization, universal: true } : null;
  }
  const career = EOTE_CAREERS?.[careerKey] ?? null;
  const specialization = career?.specializations?.[specializationKey] ?? null;
  return career && specialization ? { careerKey, specializationKey, career, specialization, universal: false } : null;
}

export function ownedSpecializationRefs(character) {
  if (!character || typeof character !== 'object') throw new Error('Ficha ausente.');
  const refs = [];
  const seen = new Set();
  const push = (careerKey, specializationKey, source) => {
    const ref = specializationEntry(careerKey, specializationKey, character.creation?.rulesProfile);
    if (!ref) return;
    const id = `${careerKey}:${specializationKey}`;
    if (seen.has(id)) return;
    seen.add(id);
    refs.push({
      careerKey,
      specializationKey,
      careerLabel: ref.career.label,
      specializationLabel: ref.specialization.label,
      source
    });
  };

  const initialCareer = String(character.creation?.careerKey ?? '').trim();
  const initialSpec = String(character.creation?.specializationKey ?? '').trim();
  if (initialCareer && initialSpec) push(initialCareer, initialSpec, 'initial');

  for (const purchase of (Array.isArray(character.specializationPurchases) ? character.specializationPurchases : [])) {
    push(String(purchase?.careerKey ?? '').trim(), String(purchase?.specializationKey ?? '').trim(), 'purchased');
  }

  return refs;
}

export function ownsSpecialization(character, careerKey, specializationKey) {
  const id = `${String(careerKey)}:${String(specializationKey)}`;
  return ownedSpecializationRefs(character).some(ref => `${ref.careerKey}:${ref.specializationKey}` === id);
}

export function specializationPurchaseCost(character, careerKey, specializationKey) {
  const profile = character?.creation?.rulesProfile ?? EOTE_CORE;
  if (![EOTE_CORE, 'AOR_CORE', FND_CORE].includes(profile)) throw new Error('Perfil de progressão não suportado.');
  const ref = specializationEntry(String(careerKey), String(specializationKey), profile);
  if (!ref) throw new Error('Especialização não encontrada no catálogo auditado para este perfil.');
  if (ownsSpecialization(character, ref.careerKey, ref.specializationKey)) {
    throw new Error('O personagem já possui essa especialização.');
  }
  const currentCount = ownedSpecializationRefs(character).length;
  if (currentCount < 1) throw new Error('A ficha não possui especialização inicial auditável.');
  const totalAfterPurchase = currentCount + 1;
  const originalCareerKey = String(character?.creation?.careerKey ?? '');
  const universal = Boolean(ref.universal);
  const inCareer = ref.careerKey === originalCareerKey;
  const cost = totalAfterPurchase * 10 + (inCareer || universal ? 0 : 10);
  if (profile === EOTE_CORE && universal && ref.specializationKey === 'force_sensitive_exile' && character?.creation?.speciesKey === 'droid') {
    throw new Error('Droides não podem adquirir a especialização Exilado Sensível à Força.');
  }
  return {
    careerKey: ref.careerKey,
    specializationKey: ref.specializationKey,
    rulesProfile: profile,
    careerLabel: ref.career.label,
    specializationLabel: ref.specialization.label,
    inCareer,
    universal,
    currentCount,
    totalAfterPurchase,
    cost,
    careerSkillsGranted: [...ref.specialization.skills]
  };
}

export function purchaseSpecialization(character, careerKey, specializationKey) {
  if (!character?.xp || !Number.isInteger(character.xp.available)) throw new Error('Ficha sem controle válido de Experiência.');
  const quote = specializationPurchaseCost(character, careerKey, specializationKey);
  if (character.xp.available < quote.cost) throw new Error(`PE insuficientes: são necessários ${quote.cost} PE.`);

  const next = structuredClone(character);
  next.xp.available -= quote.cost;
  next.specializations = Array.isArray(next.specializations) ? next.specializations : [];
  if (!next.specializations.includes(quote.specializationLabel)) next.specializations.push(quote.specializationLabel);
  next.careerSkills = Array.isArray(next.careerSkills) ? next.careerSkills : [];
  next.careerSkills = [...new Set([...next.careerSkills, ...quote.careerSkillsGranted])];
  next.specializationPurchases = Array.isArray(next.specializationPurchases) ? next.specializationPurchases : [];
  next.specializationPurchases.push({
    careerKey: quote.careerKey,
    specializationKey: quote.specializationKey,
    rulesProfile: quote.rulesProfile,
    careerLabel: quote.careerLabel,
    specializationLabel: quote.specializationLabel,
    inCareer: quote.inCareer,
    universal: quote.universal,
    cost: quote.cost
  });
  let finalCharacter = next;
  if (quote.universal && quote.specializationKey === 'force_sensitive_exile') finalCharacter = grantMinimumForceRating(next, 1);
  return { character: finalCharacter, ...quote };
}
