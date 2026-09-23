import type { WorldClock } from './types';

export const WORLD_DAYS_PER_YEAR = 365;
const MINUTES_PER_DAY = 1440;
const MINUTES_PER_YEAR = WORLD_DAYS_PER_YEAR * MINUTES_PER_DAY;

export interface WorldClockAdvance {
  clock: WorldClock;
  yearsElapsed: number;
  daysElapsed: number;
}

export function seasonForDay(day:number):WorldClock['season']{
  const value=Math.max(1,Math.min(WORLD_DAYS_PER_YEAR,Math.trunc(day)||1));
  if(value<=92)return 'spring';
  if(value<=184)return 'summer';
  if(value<=276)return 'autumn';
  return 'winter';
}

/**
 * Calendário civil determinístico. Aceita avanço e correção negativa de tempo
 * para que Atalhos de Jornada reduzam a duração já registrada sem criar dias
 * fantasmas. Não concede recuperação, Sociedade, XP, cura ou provisões.
 */
export function advanceWorldClock(input:WorldClock,minutes:number):WorldClockAdvance{
  const delta=Math.max(-MINUTES_PER_YEAR,Math.min(MINUTES_PER_YEAR,Math.trunc(Number(minutes)||0)));
  const startDay=Math.max(1,Math.min(WORLD_DAYS_PER_YEAR,Math.trunc(input.day)||1));
  const startMinuteOfYear=(startDay-1)*MINUTES_PER_DAY
    +Math.max(0,Math.min(23,Math.trunc(input.hour)||0))*60
    +Math.max(0,Math.min(59,Math.trunc(input.minute)||0));
  let absolute=startMinuteOfYear+delta;
  let yearShift=Math.floor(absolute/MINUTES_PER_YEAR);
  let minuteOfYear=absolute-yearShift*MINUTES_PER_YEAR;
  if(minuteOfYear<0){minuteOfYear+=MINUTES_PER_YEAR;yearShift-=1;}
  const day=Math.floor(minuteOfYear/MINUTES_PER_DAY)+1;
  const minuteOfDay=minuteOfYear%MINUTES_PER_DAY;
  const elapsed=Math.max(0,(Number(input.elapsedMinutes)||0)+delta);
  return {
    clock:{
      year:input.year+yearShift,season:seasonForDay(day),day,
      hour:Math.floor(minuteOfDay/60),minute:minuteOfDay%60,elapsedMinutes:elapsed,
    },
    yearsElapsed:yearShift,
    daysElapsed:delta>=0?Math.floor(delta/MINUTES_PER_DAY):Math.ceil(delta/MINUTES_PER_DAY),
  };
}
