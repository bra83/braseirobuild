import type { MasterState } from '../master/types';

export function spendFellowship(state: MasterState, amount = 1): { state: MasterState; spent: boolean } {
  if (state.company.fellowshipCurrent < amount) return { state, spent: false };
  return {
    spent: true,
    state: { ...state, company: { ...state.company, fellowshipCurrent: state.company.fellowshipCurrent - amount }, updatedAt: Date.now() },
  };
}

export function startFellowshipPhase(state: MasterState, locationId?: string): MasterState {
  return { ...state, activeMode: 'fellowship', fellowshipPhase: { status: 'active', locationId, startedAt: Date.now(), durationDays: 0, undertakings: {} }, updatedAt: Date.now() };
}

export function endFellowshipPhase(state: MasterState): MasterState {
  return { ...state, activeMode: 'adventure', fellowshipPhase: { ...state.fellowshipPhase, status: 'resolved' }, updatedAt: Date.now() };
}
