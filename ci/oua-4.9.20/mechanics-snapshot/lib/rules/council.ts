import type { MasterState } from '../master/types';

export function startCouncil(state: MasterState, objective: string, tolerance: number, participants: string[], npcIds: string[]): MasterState {
  return {
    ...state,
    activeMode: 'council',
    council: {
      status: 'introduction',
      id: `council_${Date.now()}`,
      objective,
      tolerance: Math.max(1, tolerance),
      successes: 0,
      failures: 0,
      participants: Array.from(new Set(participants)),
      interlocutorNpcIds: Array.from(new Set(npcIds)),
    },
    updatedAt: Date.now(),
  };
}

export function recordCouncilResult(state: MasterState, success: boolean): MasterState {
  const council = { ...state.council };
  if (success) council.successes += 1;
  else council.failures += 1;
  council.status = 'interaction';
  if (council.failures >= council.tolerance) {
    council.status = 'resolved';
    council.outcome = council.successes > 0 ? 'partial' : 'failure';
  }
  return { ...state, council, updatedAt: Date.now() };
}

export function resolveCouncil(state: MasterState, outcome: 'success' | 'partial' | 'failure'): MasterState {
  return { ...state, activeMode: 'adventure', council: { ...state.council, status: 'resolved', outcome }, updatedAt: Date.now() };
}
