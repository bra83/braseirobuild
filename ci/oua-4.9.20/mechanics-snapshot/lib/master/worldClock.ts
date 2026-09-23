import type { WorldClock } from './types';

export const WORLD_DAYS_PER_YEAR = 365;

export interface WorldClockAdvance {
  clock: WorldClock;
  yearsElapsed: number;
  daysElapsed: number;
}

export function seasonForDay(day: number): WorldClock['season'] {
  const value = Math.max(1, Math.min(WORLD_DAYS_PER_YEAR, Math.trunc(day) || 1));
  if (value <= 92) return 'spring';
  if (value <= 184) return 'summer';
  if (value <= 276) return 'autumn';
  return 'winter';
}

/**
 * Calendário civil determinístico. Ele movimenta somente data e idade: não
 * concede recuperação, Sociedade, XP, cura, provisões ou benefícios mecânicos.
 */
export function advanceWorldClock(input: WorldClock, minutes: number): WorldClockAdvance {
  const safeMinutes = Math.max(0, Math.min(60 * 24 * WORLD_DAYS_PER_YEAR, Math.trunc(Number(minutes) || 0)));
  const startDay = Math.max(1, Math.min(WORLD_DAYS_PER_YEAR, Math.trunc(input.day) || 1));
  const startMinuteOfYear = (startDay - 1) * 1440
    + Math.max(0, Math.min(23, Math.trunc(input.hour) || 0)) * 60
    + Math.max(0, Math.min(59, Math.trunc(input.minute) || 0));
  const absolute = startMinuteOfYear + safeMinutes;
  const yearsElapsed = Math.floor(absolute / (WORLD_DAYS_PER_YEAR * 1440));
  const minuteOfYear = absolute % (WORLD_DAYS_PER_YEAR * 1440);
  const day = Math.floor(minuteOfYear / 1440) + 1;
  const minuteOfDay = minuteOfYear % 1440;
  return {
    clock: {
      year: input.year + yearsElapsed,
      season: seasonForDay(day),
      day,
      hour: Math.floor(minuteOfDay / 60),
      minute: minuteOfDay % 60,
      elapsedMinutes: Math.max(0, Number(input.elapsedMinutes) || 0) + safeMinutes,
    },
    yearsElapsed,
    daysElapsed: Math.floor(safeMinutes / 1440),
  };
}
