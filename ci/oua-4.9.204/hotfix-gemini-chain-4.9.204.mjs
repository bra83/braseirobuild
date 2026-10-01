import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(p(rel),text,'utf8');

const models = `export const GEMINI_TEXT_MODEL_OPTIONS = [
  { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash' },
  { id: 'gemini-3.7-flash', label: 'Gemini 3.7 Flash' },
  { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash' },
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash — Mestre' },
  { id: 'gemini-flash-latest', label: 'Gemini Flash Latest — alias' },
  { id: 'gemini-3.1-flash-lite', label: 'Gemini 3.1 Flash-Lite' },
  { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash — compatibilidade' },
] as const;

export const GEMINI_TTS_MODEL_OPTIONS = [
  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },
  { id: 'gemini-2.5-flash-preview-tts', label: 'Gemini 2.5 Flash Preview TTS — compatibilidade' },
  { id: 'gemini-2.5-pro-preview-tts', label: 'Gemini 2.5 Pro Preview TTS' },
] as const;

const TEXT_KEY = 'braseiro.geminiModel.text';
const WORKING_TEXT_KEY = 'braseiro.geminiModel.working';
const LEGACY_PRINCIPAL_KEY = 'braseiro.geminiModel.cinematic';
const LEGACY_ECONOMY_KEY = 'braseiro.geminiModel.economy';
const TEXT_DEFAULT = 'gemini-3.5-flash';
const TTS_KEY = 'braseiro.geminiModel.tts';
const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';

function cleanModel(value: unknown): string {
  return String(value || '').trim().replace(/^models\\//, '');
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

export function getGeminiTextModels(): string[] {
  const preferred = cleanModel(getGeminiTextModel());
  const working = typeof window === 'undefined' ? '' : cleanModel(window.localStorage.getItem(WORKING_TEXT_KEY));
  return Array.from(new Set([
    preferred,
    working,
    ...GEMINI_TEXT_MODEL_OPTIONS.map(option => option.id),
  ].map(cleanModel).filter(Boolean)));
}

export function rememberWorkingGeminiTextModel(model: string): void {
  if (typeof window === 'undefined') return;
  const clean = cleanModel(model);
  if (clean) window.localStorage.setItem(WORKING_TEXT_KEY, clean);
}

export function saveGeminiTextModel(model: string): void {
  if (typeof window === 'undefined') return;
  const clean = cleanModel(model) || TEXT_DEFAULT;
  window.localStorage.setItem(TEXT_KEY, clean);
  window.localStorage.setItem(WORKING_TEXT_KEY, clean);
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
  window.localStorage.removeItem(WORKING_TEXT_KEY);
  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);
  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);
  window.localStorage.removeItem(TTS_KEY);
}
`;
write('lib/geminiModels.ts',models);

let settings=read('components/SettingsView.tsx');
const importRe=/import\s*\{([\s\S]*?)\}\s*from\s*['"]\.\.\/lib\/geminiModels['"];?/;
const im=settings.match(importRe);
if(!im) throw new Error('Settings geminiModels import not found');
const names=im[1].split(',').map(x=>x.trim()).filter(Boolean);
if(!names.includes('getGeminiTextModels')) names.push('getGeminiTextModels');
settings=settings.replace(importRe,`import {
  ${names.join(',\n  ')}
} from '../lib/geminiModels';`);

const testRe=/  const testTextModel = async \(\) => \{[\s\S]*?\n  \};\n\n  const testTtsModel/;
if(!testRe.test(settings)) throw new Error('Settings text model test block not found');
settings=settings.replace(testRe,`  const testTextModel = async () => {
    if (!hasGeminiApiKey()) { setStatus('Salve ao menos uma chave Gemini antes do teste.'); return; }
    setTesting('text');
    const candidates = Array.from(new Set([textModel.trim(), ...getGeminiTextModels()].filter(Boolean)));
    setStatus('Testando a mesma cadeia de modelos usada pela aventura…');
    let lastError: unknown;
    try {
      for (const candidate of candidates) {
        try {
          const response = await generateGemini(
            candidate,
            [{ parts: [{ text: 'Responda somente JSON válido: {"ok":true,"pipeline":"adventure"}' }] }],
            { responseMimeType: 'application/json', temperature: 0, maxOutputTokens: 80 },
          );
          const text = responseText(response).trim();
          if (!text) throw new Error('Resposta vazia.');
          saveGeminiTextModel(candidate);
          setTextModel(candidate);
          setStatus('Gemini OK — ' + candidate + ' respondeu pela mesma cadeia de modelos usada pela aventura.');
          return;
        } catch (error) {
          lastError = error;
        }
      }
      throw lastError || new Error('Nenhum modelo respondeu.');
    } catch (error) {
      setStatus('Falha na cadeia Gemini da aventura: ' + (error instanceof Error ? error.message : String(error)));
    } finally {
      setTesting(null);
    }
  };

  const testTtsModel`);
write('components/SettingsView.tsx',settings);

console.log('OUA 4.9.204 Gemini chain hotfix applied');
