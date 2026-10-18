// Star Wars: Age of Rebellion / Edge Studio
// Lead by Example (pp. 70-87) & Stay on Target (pp. 70-85)
// Canonical implementation of Squads, Starfighter Squadrons, and Mass Combat Battle Resolution.

const clone = v => structuredClone(v);
const clamp = (val, min, max) => Math.max(min, Math.min(max, val));

export const SQUAD_FORMATIONS = Object.freeze({
  skirmish: Object.freeze({ id: 'skirmish', name: 'Formação de Tiroteio', bonusRangedBoost: 1, bonusDefense: 0 }),
  assault: Object.freeze({ id: 'assault', name: 'Formação de Choque', bonusMeleeBoost: 1, bonusDefense: 0 }),
  defensive: Object.freeze({ id: 'defensive', name: 'Falange Defensiva', bonusRangedBoost: 0, bonusDefense: 1 })
});

export const SQUADRON_FORMATIONS = Object.freeze({
  vic: Object.freeze({ id: 'vic', name: 'Formação em V', handlingBoost: 1, gunneryBoost: 0 }),
  spread: Object.freeze({ id: 'spread', name: 'Formação Espalhada', handlingBoost: 0, gunneryBoost: 1 })
});

export const MASS_COMBAT_FORCE_TIERS = Object.freeze([
  Object.freeze({ tier: 1, label: 'Pequeno (30-50 tropas / 2-4 veículos)' }),
  Object.freeze({ tier: 2, label: 'Médio (100-300 tropas / 10-20 veículos)' }),
  Object.freeze({ tier: 3, label: 'Grande (500-1.000 tropas / batalhão)' }),
  Object.freeze({ tier: 4, label: 'Maciço (2.000-5.000 tropas / regimento)' }),
  Object.freeze({ tier: 5, label: 'Planetário / Frota Titânica (10.000+ tropas)' })
]);

export function createSquad({
  leaderName,
  minionProfileName = 'Soldado Rebelde da Linha de Frente',
  minionCount = 4,
  maxCapacity = 6,
  formation = 'skirmish'
} = {}) {
  return {
    id: `squad_${Date.now()}`,
    leaderName,
    minionProfileName,
    minionCount: clamp(Number(minionCount) || 1, 1, maxCapacity),
    maxCapacity,
    formation: SQUAD_FORMATIONS[formation] ? formation : 'skirmish',
    active: true
  };
}

export function setSquadFormation(squad, formationKey) {
  if (!SQUAD_FORMATIONS[formationKey]) throw new Error(`Formação de esquadrão inválida: ${formationKey}`);
  const next = clone(squad);
  next.formation = formationKey;
  return next;
}

export function redirectDamageToSquad(squad, damageAmount = 1) {
  const next = clone(squad);
  if (next.minionCount <= 0) {
    return { squad: next, absorbed: false, minionsLost: 0, remainingMinions: 0 };
  }
  next.minionCount -= 1;
  if (next.minionCount === 0) {
    next.active = false;
  }
  return {
    squad: next,
    absorbed: true,
    minionsLost: 1,
    remainingMinions: next.minionCount,
    message: '1 subordinado do esquadrão absorveu o impacto fatal e foi eliminado.'
  };
}

export function createStarfighterSquadron({
  leaderPilotName,
  leaderVehicleId,
  wingmanType = 'Caça T-65B X-Wing',
  wingmenCount = 3,
  maxWingmen = 4,
  formation = 'vic'
} = {}) {
  return {
    id: `squadron_${Date.now()}`,
    leaderPilotName,
    leaderVehicleId,
    wingmanType,
    wingmenCount: clamp(Number(wingmenCount) || 1, 1, maxWingmen),
    maxWingmen,
    formation: SQUADRON_FORMATIONS[formation] ? formation : 'vic',
    active: true
  };
}

export function setSquadronFormation(squadron, formationKey) {
  if (!SQUADRON_FORMATIONS[formationKey]) throw new Error(`Formação de esquadrilha inválida: ${formationKey}`);
  const next = clone(squadron);
  next.formation = formationKey;
  return next;
}

export function redirectVehicleDamageToWingman(squadron) {
  const next = clone(squadron);
  if (next.wingmenCount <= 0) {
    return { squadron: next, absorbed: false, wingmenLost: 0, remainingWingmen: 0 };
  }
  next.wingmenCount -= 1;
  if (next.wingmenCount === 0) {
    next.active = false;
  }
  return {
    squadron: next,
    absorbed: true,
    wingmenLost: 1,
    remainingWingmen: next.wingmenCount,
    message: '1 caça de escolta da ala interceptou o ataque e foi destruído.'
  };
}

export function resolveMassCombatRound({
  friendlyForceTier = 2,
  enemyForceTier = 2,
  commanderLeadershipRank = 2,
  rollResult = { successes: 1, advantage: 0, triumph: 0, threat: 0, despair: 0 }
} = {}) {
  const tierDiff = enemyForceTier - friendlyForceTier;
  const baseDifficulty = clamp(2 + tierDiff, 1, 5);

  const successes = Number(rollResult.successes ?? rollResult.success ?? 0);
  const advantage = Number(rollResult.advantage ?? rollResult.advantages ?? 0);
  const triumph = Number(rollResult.triumph ?? rollResult.triumphs ?? 0);
  const threat = Number(rollResult.threat ?? rollResult.threats ?? 0);
  const despair = Number(rollResult.despair ?? rollResult.despairs ?? 0);

  const victory = successes > 0;
  const enemyCasualtiesPercent = clamp(successes * 15 + triumph * 20, 0, 100);
  const friendlyCasualtiesPercent = clamp((successes <= 0 ? 25 : 5) + threat * 5 + despair * 15, 0, 100);

  let tacticalOutcome = '';
  if (victory) {
    tacticalOutcome = triumph > 0
      ? 'Vitória esmagadora! As forças inimigas foram rompidas e bateram em retirada desordenada.'
      : 'As forças aliadas conquistaram o objetivo da fase e empurraram a linha de frente inimiga.';
  } else {
    tacticalOutcome = despair > 0
      ? 'Derrota crítica! A linha defensiva cedeu e os comandantes correm risco de cerco.'
      : 'Avanço repelido. As forças inimigas mantiveram suas posições e impuseram baixas.';
  }

  return {
    baseDifficulty,
    victory,
    enemyCasualtiesPercent,
    friendlyCasualtiesPercent,
    commanderRisk: despair > 0,
    tacticalOutcome,
    moraleShift: advantage - threat
  };
}
