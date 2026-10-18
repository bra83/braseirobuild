// Star Wars: Force and Destiny — Core Rulebook
// Chapter IX: The Force & Morality (pp. 317-325)
// Audited implementation of Morality, Emotional Strengths & Weaknesses, Conflict, and Dark/Light Paragon status.

const clone = v => structuredClone(v);
const clamp = (val, min, max) => Math.max(min, Math.min(max, val));
const int = (v, label, { min = 0, max = 9999 } = {}) => {
  const n = Number(v);
  if (!Number.isInteger(n) || n < min || n > max) throw new Error(`${label} inválido.`);
  return n;
};

export const EMOTIONAL_DISPOSITIONS = Object.freeze([
  Object.freeze({ min: 1, max: 10, strength: 'Bravura', weakness: 'Ira', description: 'Coragem inabalável diante do perigo / Tendência a explodir em fúria destrutiva.' }),
  Object.freeze({ min: 11, max: 20, strength: 'Compaixão', weakness: 'Ódio', description: 'Empatia profunda com o sofrimento alheio / Ressentimento amargo e desejo de destruição.' }),
  Object.freeze({ min: 21, max: 30, strength: 'Cautela', weakness: 'Medo', description: 'Prudência e planejamento tático / Paralisia ou ações precipitadas nascidas do pavor.' }),
  Object.freeze({ min: 31, max: 40, strength: 'Entusiasmo', weakness: 'Imprudência', description: 'Otimismo contagiante e determinação / Descuido com os riscos e consequências.' }),
  Object.freeze({ min: 41, max: 50, strength: 'Justiça', weakness: 'Crueldade', description: 'Compromisso com a retidão e igualdade / Punição sádica disfarçada de lei.' }),
  Object.freeze({ min: 51, max: 60, strength: 'Independência', weakness: 'Frieza', description: 'Autossuficiência e resiliência / Desdém pelas emoções e bem-estar dos outros.' }),
  Object.freeze({ min: 61, max: 70, strength: 'Curiosidade', weakness: 'Obsessão', description: 'Busca incessante por sabedoria e mistérios / Fixação perigosa que ignora limites morais.' }),
  Object.freeze({ min: 71, max: 80, strength: 'Orgulho', weakness: 'Arrogância', description: 'Autoestima sólida e honra pessoal / Soberba que subestima adversários e aliados.' }),
  Object.freeze({ min: 81, max: 90, strength: 'Amor', weakness: 'Ciúme', description: 'Vínculos leais e devoção protetora / Apego possessivo e medo desmedido da perda.' }),
  Object.freeze({ min: 91, max: 100, strength: 'Disciplina', weakness: 'Obstinação', description: 'Autocontrole e respeito ao treinamento / Rigidez cega incapaz de se adaptar.' })
]);

export const MORALITY_THRESHOLDS = Object.freeze({
  PARAGON_MASTER: 80,
  PARAGON: 70,
  NEUTRAL_HIGH: 69,
  NEUTRAL_LOW: 31,
  DARKSIDE: 30,
  DARKSIDE_MASTER: 20
});

export function findEmotionalDisposition(roll) {
  const r = clamp(int(roll, 'Rolagem emocional', { min: 1, max: 100 }), 1, 100);
  return EMOTIONAL_DISPOSITIONS.find(d => r >= d.min && r <= d.max) ?? EMOTIONAL_DISPOSITIONS[0];
}

export function normalizeMoralityState(character) {
  const raw = character?.morality && typeof character.morality === 'object' ? character.morality : {};
  const score = clamp(Number.isInteger(raw.score) ? raw.score : (Number.isInteger(character?.moralityScore) ? character.moralityScore : 50), 0, 100);
  const conflict = Math.max(0, Number.isInteger(raw.conflict) ? raw.conflict : 0);
  const strength = String(raw.strength ?? 'Compaixão');
  const weakness = String(raw.weakness ?? 'Ódio');
  const triggeredThisSession = Boolean(raw.triggeredThisSession);
  const history = Array.isArray(raw.history) ? clone(raw.history) : [];

  let side = 'neutral';
  let strainModifier = 0;
  let woundModifier = 0;

  if (score >= MORALITY_THRESHOLDS.PARAGON_MASTER) {
    side = 'light_master';
    strainModifier = +2;
  } else if (score >= MORALITY_THRESHOLDS.PARAGON) {
    side = 'light_paragon';
    strainModifier = +1;
  } else if (score <= MORALITY_THRESHOLDS.DARKSIDE_MASTER) {
    side = 'dark_master';
    woundModifier = +2;
    strainModifier = -2;
  } else if (score <= MORALITY_THRESHOLDS.DARKSIDE) {
    side = 'dark_user';
    woundModifier = +1;
    strainModifier = -1;
  }

  // Session trigger effect:
  let sessionStrainBonus = 0;
  if (triggeredThisSession) {
    if (score >= MORALITY_THRESHOLDS.PARAGON) {
      sessionStrainBonus = +2;
    } else if (score <= MORALITY_THRESHOLDS.DARKSIDE) {
      sessionStrainBonus = -2;
    }
  }

  return {
    score,
    conflict,
    strength,
    weakness,
    triggeredThisSession,
    side,
    isLightParagon: score >= MORALITY_THRESHOLDS.PARAGON,
    isDarkSide: score <= MORALITY_THRESHOLDS.DARKSIDE,
    strainModifier: strainModifier + sessionStrainBonus,
    woundModifier,
    history
  };
}

export function setMoralityScore(character, score) {
  const next = clone(character);
  const m = normalizeMoralityState(next);
  m.score = clamp(int(score, 'Pontuação de moralidade', { min: 0, max: 100 }), 0, 100);
  next.morality = m;
  return next;
}

export function addConflict(character, amount, reason = 'Ação de conflito moral') {
  const next = clone(character);
  const m = normalizeMoralityState(next);
  const pts = int(amount, 'Pontos de conflito', { min: 1, max: 100 });
  m.conflict += pts;
  m.history.push({
    tick: Date.now(),
    type: 'ADD_CONFLICT',
    amount: pts,
    reason: String(reason),
    resultingConflict: m.conflict
  });
  next.morality = m;
  return next;
}

export function setSessionMoralityTrigger(character, triggered = true) {
  const next = clone(character);
  const m = normalizeMoralityState(next);
  m.triggeredThisSession = Boolean(triggered);
  next.morality = m;
  return next;
}

export function triggerGroupMorality(group, { rng = Math.random } = {}) {
  const nextGroup = clone(group);
  const members = Array.isArray(nextGroup?.characters) ? nextGroup.characters : [];
  if (members.length === 0) return { group: nextGroup, triggeredCharacterId: null, roll: 0 };

  const roll = Math.min(100, Math.max(1, Math.floor(Number(rng()) * 100) + 1));
  
  // Distribute brackets across members based on their morality score
  let currentCap = 0;
  let selected = null;
  for (const c of members) {
    const m = normalizeMoralityState(c);
    currentCap += m.score;
    if (roll <= currentCap && !selected) {
      selected = c;
    }
  }
  // Fallback to first if overflow
  if (!selected) selected = members[0];

  const updatedMembers = members.map(c => {
    const isTarget = (c.id === selected.id);
    return setSessionMoralityTrigger(c, isTarget);
  });
  nextGroup.characters = updatedMembers;

  return {
    group: nextGroup,
    triggeredCharacterId: selected.id,
    roll
  };
}

export function resolveEndOfSessionMorality(character, { roll, rng = Math.random } = {}) {
  const next = clone(character);
  const m = normalizeMoralityState(next);

  const diceRoll = roll !== undefined
    ? clamp(int(roll, 'Rolagem d10 de moralidade', { min: 1, max: 10 }), 1, 10)
    : Math.min(10, Math.max(1, Math.floor(Number(rng()) * 10) + 1));

  let delta = diceRoll - m.conflict;
  if (m.triggeredThisSession) {
    delta *= 2;
  }

  const previousScore = m.score;
  const newScore = clamp(previousScore + delta, 0, 100);

  m.score = newScore;
  const oldConflict = m.conflict;
  m.conflict = 0;
  m.triggeredThisSession = false;

  m.history.push({
    tick: Date.now(),
    type: 'SESSION_RESOLUTION',
    roll: diceRoll,
    conflict: oldConflict,
    delta,
    previousScore,
    newScore,
    paragonAchieved: newScore >= MORALITY_THRESHOLDS.PARAGON && previousScore < MORALITY_THRESHOLDS.PARAGON,
    fellToDarkSide: newScore <= MORALITY_THRESHOLDS.DARKSIDE && previousScore > MORALITY_THRESHOLDS.DARKSIDE
  });

  next.morality = m;
  return {
    character: next,
    roll: diceRoll,
    conflict: oldConflict,
    delta,
    previousScore,
    newScore,
    side: normalizeMoralityState(next).side
  };
}
