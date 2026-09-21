export type GeminiTextRole = 'economy' | 'cinematic';

export const GEMINI_TEXT_MODEL_OPTIONS = [
  { id: 'gemini-3.5-flash-lite', label: 'Gemini 3.5 Flash-Lite — econômico' },
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash — principal' },
  { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash — mais recente' },
  { id: 'gemini-3.7-flash', label: 'Gemini 3.7 Flash' },
  { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash' },
  { id: 'gemini-3.1-flash-lite', label: 'Gemini 3.1 Flash-Lite' },
  { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash' },
  { id: 'gemini-flash-latest', label: 'Gemini Flash Latest — alias' },
] as const;

export const GEMINI_TTS_MODEL_OPTIONS = [
  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },
  { id: 'gemini-2.5-flash-preview-tts', label: 'Gemini 2.5 Flash Preview TTS — compatibilidade' },
  { id: 'gemini-2.5-pro-preview-tts', label: 'Gemini 2.5 Pro Preview TTS' },
] as const;

const TEXT_KEYS: Record<GeminiTextRole, string> = {
  economy: 'braseiro.geminiModel.economy',
  cinematic: 'braseiro.geminiModel.cinematic',
};

const TEXT_DEFAULTS: Record<GeminiTextRole, string> = {
  economy: 'gemini-3.5-flash-lite',
  cinematic: 'gemini-3.5-flash',
};

const TTS_KEY = 'braseiro.geminiModel.tts';
const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';

function cleanModel(value: unknown): string {
  return String(value || '').trim().replace(/^models\//, '');
}

export function getGeminiTextModel(role: GeminiTextRole): string {
  if (typeof window === 'undefined') return TEXT_DEFAULTS[role];
  return cleanModel(window.localStorage.getItem(TEXT_KEYS[role])) || TEXT_DEFAULTS[role];
}

export function saveGeminiTextModel(role: GeminiTextRole, model: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TEXT_KEYS[role], cleanModel(model) || TEXT_DEFAULTS[role]);
}

export function getGeminiTtsModel(): string {
  if (typeof window === 'undefined') return TTS_DEFAULT;
  return cleanModel(window.localStorage.getItem(TTS_KEY)) || TTS_DEFAULT;
}

export function saveGeminiTtsModel(model: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TTS_KEY, cleanModel(model) || TTS_DEFAULT);
}

export function resetGeminiModels(): void {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(TEXT_KEYS.economy);
  window.localStorage.removeItem(TEXT_KEYS.cinematic);
  window.localStorage.removeItem(TTS_KEY);
}
