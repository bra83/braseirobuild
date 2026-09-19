export interface SfxPlaybackOptions {
  /** Impede que um efeito preparado por uma cena antiga toque depois de PARAR/trocar de cena. */
  expectedSessionId?: string;
  semanticId?: string;
  eventId?: string;
  priority?: number;
  phrase?: string;
  searchTerms?: string[];
  scheduledAtMs?: number;
  plannedAtMs?: number;
}

const normalize = (value = '') => value.trim().toLowerCase().replace(/_/g, '-');

export function sfxCooldownMs(semanticId = ''): number {
  const id = normalize(semanticId);
  if (id === 'horse') return 12_000;
  if (/^footsteps-/.test(id)) return 7_000;
  if (id === 'growl' || id === 'roar') return 8_000;
  if (id === 'thunder') return 10_000;
  if (/^door-/.test(id)) return 2_500;
  if (id === 'sword' || id === 'bow') return 1_500;
  if (id === 'armor' || id === 'stone-scrape') return 3_000;
  return 2_000;
}

/** Tempo para a parte reconhecível do efeito aparecer antes do próximo evento. */
export function sfxHoldMs(semanticId = ''): number {
  const id = normalize(semanticId);
  if (id === 'horse') return 1_600;
  if (/^footsteps-/.test(id)) return 1_100;
  if (id === 'thunder' || id === 'roar') return 1_200;
  if (/^door-/.test(id)) return 700;
  if (id === 'sword' || id === 'bow') return 500;
  return 650;
}

export const SFX_GLOBAL_DEDUP_MS = 350;
export const SFX_FILE_DEDUP_MS = 2_500;
export const SFX_MAX_QUEUE = 2;
