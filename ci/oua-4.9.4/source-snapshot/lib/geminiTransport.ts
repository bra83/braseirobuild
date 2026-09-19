import { getBrowserGeminiApiKey, hasGeminiApiKey, GeminiKeyTier } from './apiKey';

export interface GeminiPart { text?: string; inlineData?: { mimeType?: string; data?: string } }
export interface GeminiRestResponse {
  candidates?: Array<{ content?: { parts?: GeminiPart[] }; finishReason?: string }>;
  promptFeedback?: unknown;
  _braseiroTier?: GeminiKeyTier;
}

let freeCircuitOpenUntil = 0;

function errorWithStatus(status: number, body: string): Error {
  const error = new Error(`Gemini HTTP ${status}: ${body.slice(0, 900)}`) as Error & { status?: number };
  error.status = status;
  return error;
}

async function browserRequest(model: string, body: object, tier: GeminiKeyTier): Promise<GeminiRestResponse> {
  const key = getBrowserGeminiApiKey(tier);
  if (!key) throw errorWithStatus(401, `chave ${tier} não configurada`);
  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(key)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  });
  const text = await response.text();
  if (!response.ok) throw errorWithStatus(response.status, text);
  return { ...(JSON.parse(text) as GeminiRestResponse), _braseiroTier: tier };
}

async function nativeRequest(model: string, body: object, paidOnly: boolean): Promise<GeminiRestResponse> {
  if (!window.BraseiroAndroid?.geminiRequest) throw new Error('Ponte Gemini nativa indisponível.');
  const raw = await window.BraseiroAndroid.geminiRequest(model, JSON.stringify(body), paidOnly);
  const envelope = JSON.parse(raw) as { status: number; body: string; tier: GeminiKeyTier };
  if (envelope.status < 200 || envelope.status >= 300) throw errorWithStatus(envelope.status, envelope.body);
  return { ...(JSON.parse(envelope.body) as GeminiRestResponse), _braseiroTier: envelope.tier };
}

/**
 * Transporte único. Android guarda e usa as chaves fora da WebView. No PWA,
 * tenta a gratuita, repete uma vez o 429 e só então usa a paga.
 */
export async function generateGemini(
  model: string,
  contents: unknown,
  generationConfig: Record<string, unknown>,
  systemInstruction?: string,
  paidOnly = false,
): Promise<GeminiRestResponse> {
  const body: Record<string, unknown> = { contents, generationConfig };
  if (systemInstruction) body.systemInstruction = { parts: [{ text: systemInstruction }] };
  if (window.BraseiroAndroid?.geminiRequest) return nativeRequest(model, body, paidOnly);

  const canTryFree = !paidOnly && hasGeminiApiKey('free') && Date.now() >= freeCircuitOpenUntil;
  if (canTryFree) {
    for (let attempt = 0; attempt < 2; attempt += 1) {
      try { return await browserRequest(model, body, 'free'); }
      catch (error) {
        const status = (error as Error & { status?: number }).status;
        if (status !== 429) throw error;
        if (attempt === 0) await new Promise(resolve => window.setTimeout(resolve, 900));
      }
    }
    freeCircuitOpenUntil = Date.now() + 5 * 60_000;
  }
  if (hasGeminiApiKey('paid')) return browserRequest(model, body, 'paid');
  throw new Error(canTryFree ? 'A cota gratuita está esgotada e não há chave paga configurada.' : 'Nenhuma chave Gemini está configurada.');
}

export function responseText(response: GeminiRestResponse): string {
  const candidate = response.candidates?.[0];
  const finish = String(candidate?.finishReason || '').toUpperCase();
  if (finish === 'MAX_TOKENS') throw new Error('A resposta do Gemini foi truncada por limite de tokens.');
  if (finish === 'SAFETY' || finish === 'RECITATION') throw new Error(`O Gemini interrompeu a resposta (${finish}).`);
  return candidate?.content?.parts?.map(part => part.text || '').join('').trim() || '';
}
