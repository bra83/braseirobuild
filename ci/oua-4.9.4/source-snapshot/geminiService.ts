import { hasGeminiApiKey } from './lib/apiKey';
import { generateGemini, responseText } from './lib/geminiTransport';

const Type = { OBJECT: 'OBJECT', STRING: 'STRING', ARRAY: 'ARRAY', NUMBER: 'NUMBER' } as const;

export interface HexIdentification {
  terrain: string;
  name: string | null;
  poi: string | null;
  region?: string | null;
  placeType?: string | null;
  tags?: string[];
  confidence?: number;
}

const TERRAIN_VALUES = [
  'unknown', 'plains', 'hills', 'forest', 'dense_forest', 'mountains',
  'swamp', 'water', 'settlement', 'road', 'ruins', 'impassable',
] as const;

function errorText(error: unknown): string {
  if (error instanceof Error) return error.message;
  return String(error || 'erro desconhecido');
}

function readableAiError(error: unknown): string {
  const text = errorText(error);
  if (/429|quota|resource_exhausted|rate.?limit/i.test(text)) {
    return 'A cota do Gemini está esgotada para a análise do mapa. Aguarde a renovação da cota ou use outra chave em Configurações.';
  }
  if (/401|403|api.?key|permission|unauth/i.test(text)) {
    return 'A chave Gemini não foi aceita para analisar o mapa. Confira a chave em Configurações.';
  }
  if (/network|fetch|internet|offline|failed to fetch/i.test(text)) {
    return 'Não foi possível acessar o Gemini. Confira a conexão com a internet e tente novamente.';
  }
  if (/404|not found|model.*(?:unavailable|available)/i.test(text)) {
    return 'O modelo de visão não está disponível para esta chave. O aplicativo tentou os modelos alternativos antes de parar.';
  }
  return text;
}

function extractJsonObject(raw: string): string {
  const cleaned = raw
    .trim()
    .replace(/^```(?:json)?\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim();
  if (!cleaned) throw new Error('A IA respondeu sem conteúdo.');

  const start = cleaned.indexOf('{');
  if (start < 0) return cleaned;
  let depth = 0;
  let quoted = false;
  let escaped = false;
  for (let index = start; index < cleaned.length; index += 1) {
    const char = cleaned[index];
    if (escaped) { escaped = false; continue; }
    if (char === '\\' && quoted) { escaped = true; continue; }
    if (char === '"') { quoted = !quoted; continue; }
    if (quoted) continue;
    if (char === '{') depth += 1;
    if (char === '}') {
      depth -= 1;
      if (depth === 0) return cleaned.slice(start, index + 1);
    }
  }
  throw new Error('A resposta estruturada da IA veio incompleta.');
}

function normalizeIdentification(value: Partial<HexIdentification>): HexIdentification {
  const terrain = TERRAIN_VALUES.includes(value.terrain as (typeof TERRAIN_VALUES)[number])
    ? String(value.terrain)
    : 'unknown';
  const confidence = Number(value.confidence);
  return {
    terrain,
    name: typeof value.name === 'string' && value.name.trim() ? value.name.trim() : null,
    poi: typeof value.poi === 'string' && value.poi.trim() ? value.poi.trim() : null,
    region: typeof value.region === 'string' && value.region.trim() ? value.region.trim() : null,
    placeType: typeof value.placeType === 'string' && value.placeType.trim() ? value.placeType.trim() : null,
    tags: Array.isArray(value.tags)
      ? value.tags.filter((tag): tag is string => typeof tag === 'string' && Boolean(tag.trim())).map(tag => tag.trim()).slice(0, 8)
      : [],
    confidence: Number.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0,
  };
}

function parseIdentification(raw: string): HexIdentification {
  const json = extractJsonObject(raw);
  try {
    return normalizeIdentification(JSON.parse(json) as Partial<HexIdentification>);
  } catch (error) {
    throw new Error(`A IA respondeu em formato inválido: ${errorText(error)}`);
  }
}

export async function identifyHex(imageBase64: string, contextBase64?: string): Promise<HexIdentification> {
  if (!hasGeminiApiKey()) throw new Error('Configure uma chave Gemini em Configurações antes de identificar o terreno.');
  if (!imageBase64) throw new Error('O recorte do hexágono ficou vazio.');

  const parts: any[] = [{ inlineData: { mimeType: 'image/png', data: imageBase64 } }];
  if (contextBase64) parts.push({ inlineData: { mimeType: 'image/png', data: contextBase64 } });
  parts.push({
    text: `A primeira imagem é o hexágono selecionado; a segunda, quando presente, mostra apenas o contexto vizinho do mesmo mapa de Eriador.
Classifique somente o que está realmente visível. Não invente nomes apagados ou ilegíveis.
O campo terrain DEVE ser exatamente um destes identificadores em inglês: ${TERRAIN_VALUES.join(', ')}.
Interpretação: plains=planícies, hills=colinas, forest=floresta, dense_forest=floresta densa, mountains=montanhas, swamp=pântano, water=água, settlement=povoado/cidade, road=estrada, ruins=ruínas, impassable=intransponível.
name: nome próprio legível de cidade, vila, ruína ou local; caso contrário null.
poi: ponto de interesse visível; caso contrário null.
region: região legível; caso contrário null.
placeType: tipo curto em português, como cidade, vila, ruína, estrada, floresta ou montanha.
tags: no máximo 8 palavras curtas em português.
confidence: número entre 0 e 1.`,
  });

  const schema = {
    type: Type.OBJECT,
    properties: {
      terrain: { type: Type.STRING, enum: [...TERRAIN_VALUES] },
      name: { type: Type.STRING, nullable: true },
      poi: { type: Type.STRING, nullable: true },
      region: { type: Type.STRING, nullable: true },
      placeType: { type: Type.STRING, nullable: true },
      tags: { type: Type.ARRAY, items: { type: Type.STRING } },
      confidence: { type: Type.NUMBER },
    },
    required: ['terrain', 'name', 'poi', 'region', 'placeType', 'tags', 'confidence'],
  };

  // Comece pelo modelo econômico. Cotas são independentes; uma resposta 429
  // de um modelo não deve impedir a tentativa do seguinte.
  const models = ['gemini-3.1-flash-lite', 'gemini-3.5-flash'];
  let lastError: unknown = null;

  for (const model of models) {
    const configs: any[] = [
      { temperature: 0.05, maxOutputTokens: 900, responseMimeType: 'application/json', responseSchema: schema },
      // Alguns projetos/modelos rejeitam nullable ou responseSchema mesmo
      // aceitando visão e JSON. Nesse caso, preserve JSON mode e valide aqui.
      { temperature: 0.05, maxOutputTokens: 900, responseMimeType: 'application/json' },
    ];
    for (const config of configs) {
      try {
        const response = await generateGemini(model, [{ parts }], config);
        const text = responseText(response);
        if (!text) {
          const finishReason = response.candidates?.[0]?.finishReason;
          throw new Error(`A IA não devolveu JSON${finishReason ? ` (${finishReason})` : ''}.`);
        }
        const parsed = parseIdentification(text);
        console.info(`[BRASEIRO_MAP_AI] modelo=${model} schema=${Boolean(config.responseSchema)} terreno=${parsed.terrain}`);
        return parsed;
      } catch (error) {
        lastError = error;
        console.warn(`[BRASEIRO_MAP_AI] falha em ${model} (schema=${Boolean(config.responseSchema)}):`, error);
        const detail = errorText(error);
        if (/401|403|api.?key|permission/i.test(detail)) break;
        if (/429|quota|resource_exhausted|404|not found|model.*unavailable/i.test(detail)) break;
      }
    }
    if (/401|403|api.?key|permission/i.test(errorText(lastError))) break;
  }

  throw new Error(readableAiError(lastError));
}
