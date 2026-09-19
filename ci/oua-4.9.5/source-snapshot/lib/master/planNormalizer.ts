import type { AdventureMode, DirectorPlan, MasterCommand, SceneKind } from './types';

const FORMAL_START_BY_MODE: Partial<Record<AdventureMode, MasterCommand['type']>> = {
  journey: 'start_journey',
  council: 'start_council',
  combat: 'start_combat',
  fellowship: 'start_fellowship_phase',
};

const FORMAL_COMMANDS_BY_MODE: Partial<Record<AdventureMode, Set<MasterCommand['type']>>> = {
  journey: new Set(['start_journey', 'complete_journey']),
  council: new Set(['start_council', 'record_council_result', 'resolve_council']),
  combat: new Set(['start_combat', 'set_combat_stance', 'next_combat_round', 'end_combat']),
  fellowship: new Set(['start_fellowship_phase', 'end_fellowship_phase']),
};

function narrativeFallbackKind(mode: AdventureMode): SceneKind {
  if (mode === 'council') return 'dialogue';
  if (mode === 'fellowship') return 'rest';
  return 'exploration';
}

export interface FormalPhaseNormalizationResult {
  plan: DirectorPlan;
  warning?: string;
}

/**
 * Fases formais só existem quando o Diretor envia o comando de abertura previsto
 * pelas regras. Sem ele, a intenção continua como cena comum e nunca derruba o
 * turno depois de uma rolagem.
 */
export function normalizeUnstartedFormalPhase(plan: DirectorPlan, currentMode: AdventureMode): FormalPhaseNormalizationResult {
  const commands = (plan.commands || []).filter(command => command.type !== 'move_party');
  const startCommand = FORMAL_START_BY_MODE[plan.mode];
  if (!startCommand || currentMode === plan.mode || commands.some(command => command.type === startCommand)) {
    return { plan: { ...plan, commands } };
  }

  const fallbackKind = narrativeFallbackKind(plan.mode);
  const forbidden = FORMAL_COMMANDS_BY_MODE[plan.mode] || new Set<MasterCommand['type']>();
  return {
    plan: {
      ...plan,
      mode: 'adventure',
      sceneKind: fallbackKind,
      commands: commands.filter(command => !forbidden.has(command.type)),
    },
    warning: `${plan.mode} foi tratado como ${fallbackKind}: o Diretor não abriu a fase formal com ${startCommand}.`,
  };
}
