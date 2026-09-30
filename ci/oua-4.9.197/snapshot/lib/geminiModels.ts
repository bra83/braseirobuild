export const GEMINI_TEXT_MODEL_OPTIONS = [
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash — Mestre' },
  { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash' },
  { id: 'gemini-3.7-flash', label: 'Gemini 3.7 Flash' },
  { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash' },
  { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash' },
  { id: 'gemini-flash-latest', label: 'Gemini Flash Latest — alias' },
] as const;

export const GEMINI_TTS_MODEL_OPTIONS = [
  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },
  { id: 'gemini-2.5-flash-preview-tts', label: 'Gemini 2.5 Flash Preview TTS — compatibilidade' },
  { id: 'gemini-2.5-pro-preview-tts', label: 'Gemini 2.5 Pro Preview TTS' },
] as const;

const TEXT_KEY = 'braseiro.geminiModel.text';
const LEGACY_PRINCIPAL_KEY = 'braseiro.geminiModel.cinematic';
const LEGACY_ECONOMY_KEY = 'braseiro.geminiModel.economy';
const TEXT_DEFAULT = 'gemini-3.5-flash';
const TTS_KEY = 'braseiro.geminiModel.tts';
const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';

function cleanModel(value: unknown): string {
  return String(value || '').trim().replace(/^models\//, '');
}

export function getGeminiTextModel(): string {
  if (typeof window === 'undefined') return TEXT_DEFAULT;
  const current = cleanModel(window.localStorage.getItem(TEXT_KEY));
  if (current) return current;
  const migrated = cleanModel(window.localStorage.getItem(LEGACY_PRINCIPAL_KEY));
  if (migrated) {
    window.localStorage.setItem(TEXT_KEY, migrated);
    return migrated;
  }
  return TEXT_DEFAULT;
}

export function saveGeminiTextModel(model: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TEXT_KEY, cleanModel(model) || TEXT_DEFAULT);
  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);
  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);
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
  window.localStorage.removeItem(TEXT_KEY);
  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);
  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);
  window.localStorage.removeItem(TTS_KEY);
}
