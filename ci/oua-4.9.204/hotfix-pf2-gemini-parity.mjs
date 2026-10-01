import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(p(rel),text,'utf8');
const must=(ok,label)=>{if(!ok)throw new Error('PF2_GEMINI_PARITY: '+label);};

// 1) Same resilience policy that is proven in Pathfinder 2e:
// configured model -> last known good -> known compatible fallbacks.
// Do not fabricate speculative model IDs into the gameplay chain.
const modelsText=[
"export const GEMINI_TEXT_MODEL_OPTIONS = [",
"  { id: 'gemini-3.5-flash-lite', label: 'Gemini 3.5 Flash-Lite — recomendado' },",
"  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash' },",
"  { id: 'gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash-Lite — compatibilidade' },",
"] as const;",
"",
"export const GEMINI_TTS_MODEL_OPTIONS = [",
"  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },",
"  { id: 'gemini-2.5-flash-preview-tts', label: 'Gemini 2.5 Flash Preview TTS — compatibilidade' },",
"  { id: 'gemini-2.5-pro-preview-tts', label: 'Gemini 2.5 Pro Preview TTS' },",
"] as const;",
"",
"const TEXT_KEY = 'braseiro.geminiModel.text';",
"const WORKING_TEXT_KEY = 'braseiro.geminiModel.working';",
"const LEGACY_PRINCIPAL_KEY = 'braseiro.geminiModel.cinematic';",
"const LEGACY_ECONOMY_KEY = 'braseiro.geminiModel.economy';",
"const TEXT_DEFAULT = 'gemini-3.5-flash-lite';",
"const TTS_KEY = 'braseiro.geminiModel.tts';",
"const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';",
"",
"function cleanModel(value: unknown): string {",
"  return String(value || '').trim().replace(/^models\\//, '');",
"}",
"",
"export function getGeminiTextModel(): string {",
"  if (typeof window === 'undefined') return TEXT_DEFAULT;",
"  const current = cleanModel(window.localStorage.getItem(TEXT_KEY));",
"  if (current) return current;",
"  const migrated = cleanModel(window.localStorage.getItem(LEGACY_PRINCIPAL_KEY));",
"  if (migrated) { window.localStorage.setItem(TEXT_KEY, migrated); return migrated; }",
"  return TEXT_DEFAULT;",
"}",
"",
"export function getGeminiTextModels(preferredOverride?: string): string[] {",
"  const preferred = cleanModel(preferredOverride || getGeminiTextModel());",
"  const working = typeof window === 'undefined' ? '' : cleanModel(window.localStorage.getItem(WORKING_TEXT_KEY));",
"  return Array.from(new Set([preferred, working, 'gemini-3.5-flash-lite', 'gemini-3.5-flash', 'gemini-2.5-flash-lite'].map(cleanModel).filter(Boolean)));",
"}",
"",
"export function rememberWorkingGeminiTextModel(model: string): void {",
"  if (typeof window === 'undefined') return;",
"  const clean = cleanModel(model);",
"  if (clean) window.localStorage.setItem(WORKING_TEXT_KEY, clean);",
"}",
"",
"export function saveGeminiTextModel(model: string): void {",
"  if (typeof window === 'undefined') return;",
"  window.localStorage.setItem(TEXT_KEY, cleanModel(model) || TEXT_DEFAULT);",
"  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);",
"  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);",
"}",
"",
"export function getGeminiTtsModel(): string {",
"  if (typeof window === 'undefined') return TTS_DEFAULT;",
"  return cleanModel(window.localStorage.getItem(TTS_KEY)) || TTS_DEFAULT;",
"}",
"",
"export function saveGeminiTtsModel(model: string): void {",
"  if (typeof window === 'undefined') return;",
"  window.localStorage.setItem(TTS_KEY, cleanModel(model) || TTS_DEFAULT);",
"}",
"",
"export function resetGeminiModels(): void {",
"  if (typeof window === 'undefined') return;",
"  window.localStorage.removeItem(TEXT_KEY);",
"  window.localStorage.removeItem(WORKING_TEXT_KEY);",
"  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);",
"  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);",
"  window.localStorage.removeItem(TTS_KEY);",
"}",
"",
].join('\n');
write('lib/geminiModels.ts',modelsText);

// 2) Text transport follows Pathfinder behavior: try model candidates directly,
// no models.list, no global 429 circuit that prevents trying the next model.
// Android still owns the secrets; browser uses x-goog-api-key header.
const transportText=[
"import { getBrowserGeminiApiKey, hasGeminiApiKey, GeminiKeyTier } from './apiKey';",
"import { getGeminiTextModels, rememberWorkingGeminiTextModel } from './geminiModels';",
"",
"export interface GeminiPart { text?: string; inlineData?: { mimeType?: string; data?: string } }",
"export interface GeminiRestResponse {",
"  candidates?: Array<{ content?: { parts?: GeminiPart[] }; finishReason?: string }>;",
"  promptFeedback?: unknown;",
"  _braseiroTier?: GeminiKeyTier;",
"  _braseiroModel?: string;",
"}",
"",
"function errorWithStatus(status: number, body: string): Error {",
"  const error = new Error('Gemini HTTP ' + status + ': ' + body.slice(0, 900)) as Error & { status?: number };",
"  error.status = status;",
"  return error;",
"}",
"",
"async function browserRequest(model: string, body: object, tier: GeminiKeyTier): Promise<GeminiRestResponse> {",
"  const key = getBrowserGeminiApiKey(tier);",
"  if (!key) throw errorWithStatus(401, 'chave ' + tier + ' não configurada');",
"  const response = await fetch('https://generativelanguage.googleapis.com/v1beta/models/' + encodeURIComponent(model) + ':generateContent', {",
"    method: 'POST',",
"    headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key },",
"    body: JSON.stringify(body),",
"  });",
"  const text = await response.text();",
"  if (!response.ok) throw errorWithStatus(response.status, text);",
"  return { ...(JSON.parse(text) as GeminiRestResponse), _braseiroTier: tier, _braseiroModel: model };",
"}",
"",
"async function nativeRequest(model: string, body: object, paidOnly: boolean): Promise<GeminiRestResponse> {",
"  if (!window.BraseiroAndroid?.geminiRequest) throw new Error('Ponte Gemini nativa indisponível.');",
"  const raw = await window.BraseiroAndroid.geminiRequest(model, JSON.stringify(body), paidOnly);",
"  const envelope = JSON.parse(raw) as { status: number; body: string; tier: GeminiKeyTier };",
"  if (envelope.status < 200 || envelope.status >= 300) throw errorWithStatus(envelope.status, envelope.body);",
"  return { ...(JSON.parse(envelope.body) as GeminiRestResponse), _braseiroTier: envelope.tier, _braseiroModel: model };",
"}",
"",
"function isTextRequest(model: string, generationConfig: Record<string, unknown>): boolean {",
"  const value = String(model || '').toLowerCase();",
"  const modalities = Array.isArray(generationConfig?.responseModalities) ? generationConfig.responseModalities.map(String).join(',').toLowerCase() : '';",
"  if (modalities.includes('audio') || modalities.includes('image')) return false;",
"  return !/tts|image|imagen|veo/.test(value);",
"}",
"",
"function isAuthenticationFailure(error: unknown): boolean {",
"  const status = (error as Error & { status?: number })?.status;",
"  return status === 401;",
"}",
"",
"async function tryNativeCandidates(candidates: string[], body: object, paidOnly: boolean): Promise<GeminiRestResponse> {",
"  let lastError: unknown;",
"  for (const candidate of candidates) {",
"    try {",
"      const response = await nativeRequest(candidate, body, paidOnly);",
"      return response;",
"    } catch (error) {",
"      lastError = error;",
"      if (isAuthenticationFailure(error)) break;",
"    }",
"  }",
"  throw lastError instanceof Error ? lastError : new Error(String(lastError || 'Nenhum modelo Gemini respondeu.'));",
"}",
"",
"async function tryBrowserCandidates(candidates: string[], body: object, tier: GeminiKeyTier): Promise<GeminiRestResponse> {",
"  let lastError: unknown;",
"  for (const candidate of candidates) {",
"    try {",
"      const response = await browserRequest(candidate, body, tier);",
"      return response;",
"    } catch (error) {",
"      lastError = error;",
"      if (isAuthenticationFailure(error)) break;",
"    }",
"  }",
"  throw lastError instanceof Error ? lastError : new Error(String(lastError || 'Nenhum modelo Gemini respondeu.'));",
"}",
"",
"export async function generateGemini(",
"  model: string,",
"  contents: unknown,",
"  generationConfig: Record<string, unknown>,",
"  systemInstruction?: string,",
"  paidOnly = false,",
"): Promise<GeminiRestResponse> {",
"  const body: Record<string, unknown> = { contents, generationConfig };",
"  if (systemInstruction) body.systemInstruction = { parts: [{ text: systemInstruction }] };",
"  const textRequest = isTextRequest(model, generationConfig);",
"  const candidates = textRequest ? getGeminiTextModels(model) : [String(model || '').trim()].filter(Boolean);",
"  if (!candidates.length) throw new Error('Nenhum modelo Gemini foi configurado.');",
"",
"  if (window.BraseiroAndroid?.geminiRequest) {",
"    let freeError: unknown;",
"    if (!paidOnly) {",
"      try {",
"        const response = await tryNativeCandidates(candidates, body, false);",
"        if (textRequest) rememberWorkingGeminiTextModel(response._braseiroModel || model);",
"        return response;",
"      } catch (error) { freeError = error; }",
"    }",
"    if (hasGeminiApiKey('paid')) {",
"      const response = await tryNativeCandidates(candidates, body, true);",
"      if (textRequest) rememberWorkingGeminiTextModel(response._braseiroModel || model);",
"      return response;",
"    }",
"    throw freeError instanceof Error ? freeError : new Error(paidOnly ? 'Nenhuma chave paga está configurada.' : 'Nenhum modelo Gemini respondeu com a chave configurada.');",
"  }",
"",
"  let freeError: unknown;",
"  if (!paidOnly && hasGeminiApiKey('free')) {",
"    try {",
"      const response = await tryBrowserCandidates(candidates, body, 'free');",
"      if (textRequest) rememberWorkingGeminiTextModel(response._braseiroModel || model);",
"      return response;",
"    } catch (error) { freeError = error; }",
"  }",
"  if (hasGeminiApiKey('paid')) {",
"    const response = await tryBrowserCandidates(candidates, body, 'paid');",
"    if (textRequest) rememberWorkingGeminiTextModel(response._braseiroModel || model);",
"    return response;",
"  }",
"  if (freeError instanceof Error) throw freeError;",
"  throw new Error(paidOnly ? 'Nenhuma chave paga está configurada.' : 'Nenhuma chave Gemini está configurada.');",
"}",
"",
"export function responseText(response: GeminiRestResponse): string {",
"  const candidate = response.candidates?.[0];",
"  const finish = String(candidate?.finishReason || '').toUpperCase();",
"  if (finish === 'MAX_TOKENS') throw new Error('A resposta do Gemini foi truncada por limite de tokens.');",
"  if (finish === 'SAFETY' || finish === 'RECITATION') throw new Error('O Gemini interrompeu a resposta (' + finish + ').');",
"  return candidate?.content?.parts?.map(part => part.text || '').join('').trim() || '';",
"}",
"",
].join('\n');
write('lib/geminiTransport.ts',transportText);

// 3) Gameplay must use the same candidate chain, not only the Settings test.
let service=read('services/geminiService.ts');
service=service.replace(
  "import { getGeminiTextModel, getGeminiTtsModel } from '../lib/geminiModels';",
  "import { getGeminiTextModel, getGeminiTextModels, getGeminiTtsModel, rememberWorkingGeminiTextModel } from '../lib/geminiModels';"
);
must(service.includes("getGeminiTextModels"),'geminiService import');
service=service.replaceAll("const models = [getGeminiTextModel()];","const models = getGeminiTextModels();");
service=service.replace(
  "      (normalized as any)._braseiroModel = model;\n      (normalized as any)._braseiroGenerationSource = 'gemini';\n      return normalized;",
  "      rememberWorkingGeminiTextModel((response as any)._braseiroModel || model);\n      (normalized as any)._braseiroModel = (response as any)._braseiroModel || model;\n      (normalized as any)._braseiroGenerationSource = 'gemini';\n      return normalized;"
);
service=service.replace(
  "      (normalized as any)._braseiroModel = model;\n      (normalized as any)._braseiroGenerationSource = 'gemini';\n      return normalized;",
  "      rememberWorkingGeminiTextModel((response as any)._braseiroModel || model);\n      (normalized as any)._braseiroModel = (response as any)._braseiroModel || model;\n      (normalized as any)._braseiroGenerationSource = 'gemini';\n      return normalized;"
);
write('services/geminiService.ts',service);

// 4) Settings test must remember a proven model without lying about the configured preference.
let settings=read('components/SettingsView.tsx');
settings=settings.replaceAll("saveGeminiTextModel(candidate);\n          setTextModel(candidate);","rememberWorkingGeminiTextModel(candidate);\n          setTextModel(candidate);");
write('components/SettingsView.tsx',settings);

// 5) Native Android: a 429 is model-specific until proven otherwise.
// Do not open a five-minute FREE-key circuit that prevents trying Pathfinder's next model.
let main=read('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
const requestRe=/    private fun performGeminiRequest\(model: String, body: String, paidOnly: Boolean\): JSONObject \{[\s\S]*?\n    \}(?=\n\n    private fun sha256Text)/;
must(requestRe.test(main),'native performGeminiRequest');
const nativeFn=[
"    private fun performGeminiRequest(model: String, body: String, paidOnly: Boolean): JSONObject {",
"        val free = getGeminiSecret(\"free\")",
"        val paid = getGeminiSecret(\"paid\")",
"        if (!paidOnly && free.isNotBlank()) {",
"            val result = callGemini(model, body, free)",
"            if (result.status != 429 || paid.isBlank()) {",
"                return JSONObject().put(\"status\", result.status).put(\"body\", result.body).put(\"tier\", \"free\")",
"            }",
"        }",
"        if (paid.isNotBlank()) {",
"            val result = callGemini(model, body, paid)",
"            return JSONObject().put(\"status\", result.status).put(\"body\", result.body).put(\"tier\", \"paid\")",
"        }",
"        val message = when {",
"            paidOnly -> \"Nenhuma chave paga está configurada neste aparelho.\"",
"            free.isBlank() && paid.isBlank() -> \"Nenhuma chave Gemini está configurada neste aparelho. Abra Configurações > Chaves Gemini.\"",
"            else -> \"Nenhuma chave Gemini utilizável está disponível.\"",
"        }",
"        return JSONObject().put(\"status\", 401).put(\"body\", message).put(\"tier\", if (paidOnly) \"paid\" else \"free\")",
"    }",
].join('\n');
main=main.replace(requestRe,nativeFn);
write('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt',main);


// 6) Pathfinder startup parity: Atlas improves grounding but must not stop the
// first Gemini call. If no hex is fixed yet, start narratively without inventing
// coordinates; cartographic movement remains blocked until the player fixes a hex.
let app=read('App.tsx');
const oldStartBlock=[
"    const discovery = await loadMapDiscovery(activeCampaign.id);",
"    if (!discovery.currentHexId) {",
"      setStoryError('Antes de começar, escolha no Atlas o hex onde a Companhia está. A aventura não será iniciada numa posição inventada.');",
"      document.getElementById('atlas-terra-media')?.scrollIntoView({ behavior: 'smooth', block: 'start' });",
"      return;",
"    }",
"    setIsStoryLoading(true); setStoryError(null);",
].join('\\n');
must(app.includes(oldStartBlock),'OUA opening hard Atlas gate');
const newStartBlock=[
"    const discovery = await loadMapDiscovery(activeCampaign.id);",
"    const persistedMaster = normalizeMasterState(activeCampaign);",
"    const openingHexId = discovery.currentHexId || persistedMaster.position.currentHexId || '';",
"    const openingLocationInstruction = openingHexId",
"      ? \`A Companhia já está no hex \${openingHexId}; use exclusivamente o contexto cartográfico desse hex como ponto inicial.\`",
"      : \`O Atlas ainda não possui um hex canônico para a Companhia. Inicie a primeira cena a partir da localização narrativa já persistida (\${persistedMaster.position.locationName || activeCampaign.currentLocation || 'Eriador'}), sem inventar coordenadas, sem mover a Companhia e sem iniciar Jornada. O jogador fixará o hex no Atlas antes de qualquer deslocamento cartográfico.\`;",
"    setIsStoryLoading(true); setStoryError(null);",
].join('\\n');
app=app.replace(oldStartBlock,newStartBlock);
const oldPrompt="    await processTurn(\`[INICIAR AVENTURA GUIADA] A Companhia já está no hex \${discovery.currentHexId}; use exclusivamente o contexto cartográfico desse hex como ponto inicial.\${companyText} Perfil: \${profile.label}. \${profile.prompt} \${openingInstruction(architecture)} O horário inicial canônico é \${String(openingClock.hour).padStart(2, '0')}:\${String(openingClock.minute).padStart(2, '0')} (\${timePeriodFromHour(openingClock.hour)}). Não substitua esse horário por manhã por hábito.\`, false, sourceCampaign);";
must(app.includes(oldPrompt),'OUA opening prompt');
app=app.replace(oldPrompt,"    await processTurn(\`[INICIAR AVENTURA GUIADA] \${openingLocationInstruction}\${companyText} Perfil: \${profile.label}. \${profile.prompt} \${openingInstruction(architecture)} O horário inicial canônico é \${String(openingClock.hour).padStart(2, '0')}:\${String(openingClock.minute).padStart(2, '0')} (\${timePeriodFromHour(openingClock.hour)}). Não substitua esse horário por manhã por hábito.\`, false, sourceCampaign);");
write('App.tsx',app);

console.log('OUA 4.9.204 PF2 Gemini parity hotfix applied');
