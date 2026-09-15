import { RNG_ALGORITHM } from './rng-core.js';
import { resolveSkillCheck } from './wwn-rules-core.js';

export const WWN_TRAVEL_SOURCES = Object.freeze({
  overland: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.11.0 Overland Travel' }),
  wilderness: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.12.0–2.12.2 Wilderness Exploration and Expeditions' }),
});

export const TERRAIN_MPH = Object.freeze({
  PLAINS: 3,
  LIGHT_FOREST_OR_DESERT: 2,
  DENSE_FOREST_OR_RUGGED_HILLS: 1.5,
  SWAMP_OR_MARSH: 1,
  MOUNTAINS_OR_DIRE_WASTELANDS: 0.5,
});

export function travelRateMph({ terrain, road = false, foulWeather = false, deepSnow = false }) {
  const base = TERRAIN_MPH[terrain];
  if (base === undefined) throw new RangeError(`unsupported terrain ${terrain}`);
  let rate = base;
  if (road) rate = Math.min(3, rate * 2);
  if (foulWeather) rate *= 0.5;
  if (deepSnow) rate *= 0.1;
  return rate;
}

export function planTravel({ terrain, hours, road = false, foulWeather = false, deepSnow = false }) {
  if (!Number.isFinite(hours) || hours < 0 || hours > 10) throw new RangeError('standard overland travel hours must be 0..10 per day');
  const mph = travelRateMph({ terrain, road, foulWeather, deepSnow });
  return Object.freeze({ sourceRefs: [WWN_TRAVEL_SOURCES.overland], hours, mph, miles: mph * hours });
}

export function resolveWanderingEncounter({ rng, dieSides, streamName }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  if (!Number.isInteger(dieSides) || dieSides < 2) throw new RangeError('GM-provided encounter die must be >= 2');
  const die = rng.die(dieSides, streamName);
  return Object.freeze({ rule: 'WWN_WANDERING_ENCOUNTER_V1', rngAlgorithm: RNG_ALGORITHM, sourceRefs: [WWN_TRAVEL_SOURCES.overland], die, encounter: die.result === 1 });
}

export function exploreHexDays({ concealmentMultiplier = 1 }) {
  if (![1, 2, 3].includes(concealmentMultiplier)) throw new RangeError('concealmentMultiplier must be GM-selected 1, 2, or 3');
  return Object.freeze({ sourceRefs: [WWN_TRAVEL_SOURCES.wilderness], standardHexMiles: 6, days: concealmentMultiplier });
}

export function expeditionSupplyNeeds({ people, days, hotDryWaterMultiplier = 1 }) {
  if (!Number.isInteger(people) || people < 1 || !Number.isInteger(days) || days < 0) throw new RangeError('people>=1 and days>=0 required');
  if (!Number.isInteger(hotDryWaterMultiplier) || hotDryWaterMultiplier < 1) throw new RangeError('water multiplier must be positive integer');
  return Object.freeze({ sourceRefs: [WWN_TRAVEL_SOURCES.wilderness], foodUnits: people * days, waterUnits: people * days * hotDryWaterMultiplier });
}

export function resolveTravelDay({ rng, terrain, hours = 10, road = false, foulWeather = false, deepSnow = false, dayEncounterDie, nightEncounterDie, supplies = null, people = 1, hotDryWaterMultiplier = 1, streamPrefix = 'travel' }) {
  const plan = planTravel({ terrain, hours, road, foulWeather, deepSnow });
  const dayEncounter = resolveWanderingEncounter({ rng, dieSides: dayEncounterDie, streamName: `${streamPrefix}/day-encounter` });
  const nightEncounter = resolveWanderingEncounter({ rng, dieSides: nightEncounterDie, streamName: `${streamPrefix}/night-encounter` });
  let supplyResult = null;
  if (supplies !== null) {
    const needs = expeditionSupplyNeeds({ people, days: 1, hotDryWaterMultiplier });
    const foodBefore = Number(supplies.foodUnits ?? 0); const waterBefore = Number(supplies.waterUnits ?? 0);
    if (!Number.isInteger(foodBefore) || foodBefore < 0 || !Number.isInteger(waterBefore) || waterBefore < 0) throw new RangeError('supply units must be non-negative integers');
    supplyResult = Object.freeze({ needs, before: { foodUnits: foodBefore, waterUnits: waterBefore }, after: { foodUnits: Math.max(0, foodBefore - needs.foodUnits), waterUnits: Math.max(0, waterBefore - needs.waterUnits) }, insufficientFood: foodBefore < needs.foodUnits, insufficientWater: waterBefore < needs.waterUnits });
  }
  return Object.freeze({ rule: 'WWN_TRAVEL_DAY_V1', rngAlgorithm: RNG_ALGORITHM, sourceRefs: [WWN_TRAVEL_SOURCES.overland, WWN_TRAVEL_SOURCES.wilderness], plan, dayEncounter, nightEncounter, supplyResult });
}


export const FORAGE_TERRAIN_DIFFICULTY = Object.freeze({
  WOODLANDS_OR_HEAVY_VEGETATION: 8,
  MOUNTAINS_SCRUBLANDS_OR_SAVANNAS: 9,
  DESERTS_BADLANDS_OR_NORMAL_BARRENS: 12,
  GRIM_WASTES_OR_BARELY_SURVIVABLE: 14,
});

export function forageDifficulty({ terrain, fullDay = false, successiveDaysInSameHex = 0 }) {
  const base = FORAGE_TERRAIN_DIFFICULTY[terrain];
  if (!Number.isInteger(base)) throw new RangeError(`unsupported forage terrain ${terrain}`);
  if (!Number.isInteger(successiveDaysInSameHex) || successiveDaysInSameHex < 0) throw new RangeError('successiveDaysInSameHex must be >=0');
  return Object.freeze({
    sourceRefs:[WWN_TRAVEL_SOURCES.wilderness], terrain, base,
    fullDayModifier:fullDay?-2:0,
    successiveDaysModifier:successiveDaysInSameHex,
    difficulty:base+(fullDay?-2:0)+successiveDaysInSameHex,
  });
}

export function resolveForagingAttempt({
  rng,
  wisdomScore,
  bestSurviveLevel,
  groupSurviveLevels = [],
  terrain,
  fullDay = false,
  successiveDaysInSameHex = 0,
  encounterDie,
  nativeWildsMinimumTwo = false,
  streamPrefix = 'forage',
}) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  if (!(bestSurviveLevel === null || bestSurviveLevel === undefined || (Number.isInteger(bestSurviveLevel) && bestSurviveLevel >= 0))) throw new RangeError('bestSurviveLevel invalid');
  if (!Array.isArray(groupSurviveLevels)) throw new TypeError('groupSurviveLevels must be array');
  const diff=forageDifficulty({terrain,fullDay,successiveDaysInSameHex});
  const check=resolveSkillCheck({rng,skillLevel:bestSurviveLevel,attributeScore:wisdomScore,difficulty:diff.difficulty,streamName:`${streamPrefix}/survive-check`});
  const encounter=resolveWanderingEncounter({rng,dieSides:encounterDie,streamName:`${streamPrefix}/encounter`});
  let forageRoll=null; let unitsFound=0;
  if(check.success){
    forageRoll=rng.die(6,`${streamPrefix}/units`);
    let groupContribution=0;
    for(const level of groupSurviveLevels){
      if(level===null||level===undefined)groupContribution-=1;
      else if(Number.isInteger(level)&&level>=0)groupContribution+=level;
      else throw new RangeError('group Survive level invalid');
    }
    unitsFound=Math.min(10,Math.max(nativeWildsMinimumTwo?2:1,forageRoll.result+groupContribution));
  }
  return Object.freeze({
    rule:'WWN_FORAGING_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_TRAVEL_SOURCES.wilderness],
    attempt:fullDay?'FULL_DAY':'HALF_DAY',difficulty:diff,check,encounter,forageRoll,unitsFound,
    allocationRequired:check.success&&unitsFound>0,
  });
}
