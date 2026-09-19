import type { MasterState, JourneyRole } from '../master/types';

export const JOURNEY_ROLE_SKILLS: Record<JourneyRole, string> = {
  guide: 'Viagem',
  scout: 'Exploração',
  hunter: 'Caçada',
  lookout: 'Vigilância',
};

export function validateJourneyRoles(roles: Partial<Record<JourneyRole, string>>, partyIds: string[]): string[] {
  const errors: string[] = [];
  const party = new Set(partyIds);
  for (const [role, characterId] of Object.entries(roles)) {
    if (characterId && !party.has(characterId)) errors.push(`${role}: personagem fora da Companhia`);
  }
  if (!roles.guide) errors.push('A Companhia precisa de um Guia.');
  return errors;
}

export function startJourney(state: MasterState, destinationHexId: string | null, roles: Partial<Record<JourneyRole, string>>): MasterState {
  return {
    ...state,
    activeMode: 'journey',
    company: { ...state.company, journeyRoles: { ...roles } },
    journey: {
      status: 'planned',
      originHexId: state.position.currentHexId,
      destinationHexId,
      startedAt: Date.now(),
      day: 0,
      segments: [],
      pendingEvent: null,
    },
    updatedAt: Date.now(),
  };
}

export function recordJourneyMove(state: MasterState, toHexId: string, locationName: string, distanceKm?: number, terrain?: string): MasterState {
  const fromHexId = state.position.currentHexId;
  const now = Date.now();
  return {
    ...state,
    activeMode: state.journey.status === 'idle' ? 'adventure' : 'journey',
    position: {
      currentHexId: toHexId,
      locationName,
      routeHexIds: [...state.position.routeHexIds, toHexId].filter((id, index, all) => index === 0 || id !== all[index - 1]),
      lastMovedAt: now,
    },
    journey: state.journey.status === 'idle' ? state.journey : {
      ...state.journey,
      status: 'travelling',
      segments: [...state.journey.segments, {
        id: `segment_${now}_${Math.random().toString(36).slice(2, 6)}`,
        fromHexId,
        toHexId,
        distanceKm,
        terrain,
        startedAt: now,
        completedAt: now,
      }],
    },
    updatedAt: now,
  };
}

export function completeJourney(state: MasterState): MasterState {
  return {
    ...state,
    activeMode: 'adventure',
    journey: { ...state.journey, status: 'completed', destinationHexId: state.position.currentHexId, pendingEvent: null },
    updatedAt: Date.now(),
  };
}
