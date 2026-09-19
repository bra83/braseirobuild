import type { WeatherForecast } from './types';

function hashString(value: string): number {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function randomFromSeed(seed: string): () => number {
  let state = hashString(seed) || 0x9e3779b9;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function clearWeather(seed: string): WeatherForecast {
  const random = randomFromSeed(seed);
  return {
    id: `weather_clear_${Math.round(random() * 9999)}`,
    kind: 'clear',
    intensity: 'light',
    probability: 1,
    reason: 'Estado climático estável.',
    eligible: true,
    stage: 'stable',
    stageStep: 0,
    activeStepsRemaining: 0,
  };
}

export function weatherStillEligible(weather: WeatherForecast, possible: WeatherForecast[]): boolean {
  return possible.some(candidate => candidate.kind === weather.kind && candidate.eligible && candidate.probability > 0);
}

/**
 * Máquina pura de clima. Uma chamada equivale exatamente a um deslocamento
 * confirmado e inédito. Geração de texto e reprodução de áudio nunca a chamam.
 */
export function advanceWeatherState(previous: WeatherForecast, possible: WeatherForecast[], seed: string): WeatherForecast {
  const current: WeatherForecast = {
    ...previous,
    stage: previous.stage || (previous.kind === 'clear' ? 'stable' : 'indications'),
    stageStep: previous.stageStep || 0,
  };
  if (current.kind !== 'clear' && current.kind !== 'cloudy' && !weatherStillEligible(current, possible)) {
    return {
      ...current,
      stage: 'dissipating',
      stageStep: (current.stageStep || 0) + 1,
      activeStepsRemaining: 0,
      reason: `${current.reason} As novas condições não sustentam o fenômeno; ele começa a dissipar.`,
    };
  }
  switch (current.stage) {
    case 'indications':
      return {
        ...current,
        stage: 'forming',
        stageStep: (current.stageStep || 0) + 1,
        reason: `${current.reason} Os indícios se organizam, mas o fenômeno ainda não está plenamente ativo.`,
      };
    case 'forming': {
      const random = randomFromSeed(`${seed}|active-duration`);
      const steps = current.kind === 'blizzard' || current.kind === 'storm'
        ? 2 + Math.floor(random() * 2)
        : 1 + Math.floor(random() * 3);
      return {
        ...current,
        stage: 'active',
        stageStep: (current.stageStep || 0) + 1,
        activeStepsRemaining: steps,
        reason: `${current.reason} A formação completou-se após novo deslocamento confirmado.`,
      };
    }
    case 'active': {
      const remaining = Math.max(0, Number(current.activeStepsRemaining || 1) - 1);
      return remaining > 0
        ? { ...current, stage: 'active', stageStep: (current.stageStep || 0) + 1, activeStepsRemaining: remaining }
        : {
            ...current,
            stage: 'dissipating',
            stageStep: (current.stageStep || 0) + 1,
            activeStepsRemaining: 0,
            reason: `${current.reason} O fenômeno perde força depois de alguns avanços.`,
          };
    }
    case 'dissipating':
      return clearWeather(`${seed}|cleared`);
    case 'stable':
    default:
      return current;
  }
}
