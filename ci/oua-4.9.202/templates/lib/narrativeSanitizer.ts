import type { GeminiStoryResponse, StoryChoice, VoiceSegment } from '../types';
import type { EnvironmentContext } from './autonomy/types';

const TECHNICAL_HEX_ID = '[A-Z]{1,3}\\d{1,3}';


const OPERATIONAL_PARAGRAPH = /(?:modo local degradado|fallback local|gemini (?:estiver|esta|está|ficou|indisponivel|indisponível)|aplicativo est[aá] em modo|regra recuperada ou uma rolagem|estado, as fichas, o mapa e o diario|estado, as fichas, o mapa e o diário|cota gratuita|chave gemini|indice local|índice local|banco de dados)/i;

/** Remove mensagens de diagnóstico que pertencem à interface, nunca à voz do Mestre. */

/**
 * Última barreira contra a pergunta automática que transforma toda cena em
 * formulário. A agência já deve estar implícita no ponto de parada da prosa.
 */
export function stripArtificialAgencyQuestion(value: string): string {
  return String(value || '')
    .replace(/(?:\s|\n)*(?:O que (?:você|vocês) faz(?:em)?(?: agora)?|Como (?:você|vocês) reage(?:m)?|Qual é (?:a )?sua próxima ação)\?\s*$/i, '')
    .trim();
}

export function stripOperationalNarrative(value: string): string {
  const paragraphs = String(value || '').split(/\n\s*\n/g)
    .map(item => item.trim())
    .filter(item => item && !OPERATIONAL_PARAGRAPH.test(item));
  return paragraphs.join('\n\n').replace(/[ \t]{2,}/g, ' ').replace(/[ \t]+([,.;:!?])/g, '$1').trim();
}

function cleanPlaceName(value: unknown): string {
  const text = String(value || '').replace(/\s+/g, ' ').trim();
  if (!text) return '';
  if (/^(?:regi[aã]o n[aã]o confirmada|terreno n[aã]o confirmado|unknown|sem hex)$/i.test(text)) return '';
  if (new RegExp(`^${TECHNICAL_HEX_ID}$`, 'i').test(text)) return '';
  return text;
}

export function narrativeRegionName(context: Pick<EnvironmentContext, 'region' | 'nearbySettlements'>, currentLocation?: string): string {
  const region = cleanPlaceName(context.region);
  const canonical = cleanPlaceName(currentLocation);
  const nearbyAtCurrentHex = context.nearbySettlements?.find(item => item.distanceHexes === 0);
  return region || cleanPlaceName(nearbyAtCurrentHex?.name) || canonical || 'Eriador';
}

/**
 * Identificadores como BO44 pertencem ao Atlas e ao diagnóstico. Esta função
 * impede que vazem para a prosa do Mestre, sem remover os IDs do estado interno.
 */
export function replaceTechnicalHexReferences(value: string, placeName: string): string {
  let text = String(value || '');
  if (!text) return text;
  const place = cleanPlaceName(placeName) || 'Eriador';
  const region = `região de ${place}`;
  const patterns: Array<[RegExp, string]> = [
    [new RegExp(`\\bsobre\\s+o\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `sobre a ${region}`],
    [new RegExp(`\\bsobre\\s+a\\s+quadrícula\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `sobre a ${region}`],
    [new RegExp(`\\bdo\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `da ${region}`],
    [new RegExp(`\\bno\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `na ${region}`],
    [new RegExp(`\\bpelo\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `pela ${region}`],
    [new RegExp(`\\bpara\\s+o\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `para a ${region}`],
    [new RegExp(`\\bao\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `à ${region}`],
    [new RegExp(`\\bde\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `da ${region}`],
    [new RegExp(`\\bem\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `na ${region}`],
    [new RegExp(`\\bO\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'g'), `A ${region}`],
    [new RegExp(`\\bo\\s+hex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), `a ${region}`],
    [new RegExp(`\\bhex(?:ágono)?\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), region],
    [new RegExp(`\\bquadrícula\\s+${TECHNICAL_HEX_ID}\\b`, 'gi'), region],
  ];
  for (const [pattern, replacement] of patterns) text = text.replace(pattern, replacement);
  // Última barreira: caso o modelo copie apenas o código, sem escrever “hex”.
  // Dois ou três caracteres evitam confundir referências comuns como d20.
  text = text.replace(/\b[A-Z]{2,3}\d{2,3}\b/g, place);
  return text.replace(/[ \t]{2,}/g, ' ').replace(/[ \t]+([,.;:!?])/g, '$1');
}


function normalizedWords(value: string): Set<string> {
  return new Set(String(value || '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().replace(/[^a-z0-9\s]/g, ' ')
    .split(/\s+/).filter(word => word.length > 3));
}

/**
 * O painel de consequência não deve repetir a mesma prosa que acabou de ser
 * narrada. Mantemos resultados mecânicos/estruturados; ecos puramente
 * literários são ocultados quando repetem substancialmente a narração.
 */
export function stripRedundantActionResult(narration: string, result: string): string {
  const clean = String(result || '').replace(/^\s*(?:consequ[eê]ncia|resultado(?: da a[cç][aã]o)?)\s*:\s*/i, '').trim();
  if (!clean) return '';
  const mechanical = /\b(?:resist[eê]ncia|esperan[cç]a|sombra|fadiga|ferimento|ferido|miser[aá]vel|cansado|suprimentos?|invent[aá]rio|item|carga|teste|sucesso|falha|n[uú]mero alvo|dado|tengwar|ódio|odio|postura|bloqueio|parada|prote[cç][aã]o|pontos?)\b/i.test(clean);
  if (mechanical) return clean;
  const a = normalizedWords(narration);
  const b = normalizedWords(clean);
  if (!a.size || !b.size) return clean;
  let shared = 0;
  for (const word of b) if (a.has(word)) shared += 1;
  const containment = shared / b.size;
  return containment >= 0.62 ? '' : clean;
}

function sanitizeChoices(choices: StoryChoice[] | undefined, placeName: string): StoryChoice[] {
  return (choices || []).map(choice => ({
    ...choice,
    text: replaceTechnicalHexReferences(choice.text, placeName),
    test: choice.test ? {
      ...choice.test,
      reason: replaceTechnicalHexReferences(choice.test.reason, placeName),
      consequences: choice.test.consequences ? replaceTechnicalHexReferences(choice.test.consequences, placeName) : choice.test.consequences,
    } : choice.test,
  }));
}

function sanitizeVoiceScript(voiceScript: VoiceSegment[] | undefined, placeName: string): VoiceSegment[] | undefined {
  return voiceScript?.map(segment => ({ ...segment, text: stripArtificialAgencyQuestion(replaceTechnicalHexReferences(segment.text, placeName)) })).filter(segment => segment.text);
}

export function sanitizeNarrativeResponse(
  response: GeminiStoryResponse,
  environment: Pick<EnvironmentContext, 'region' | 'nearbySettlements'>,
  currentLocation?: string,
): GeminiStoryResponse {
  const placeName = narrativeRegionName(environment, currentLocation);
  return {
    ...response,
    atmospheric_narration: stripArtificialAgencyQuestion(stripOperationalNarrative(replaceTechnicalHexReferences(response.atmospheric_narration, placeName))),
    action_result: stripRedundantActionResult(
      stripArtificialAgencyQuestion(stripOperationalNarrative(replaceTechnicalHexReferences(response.atmospheric_narration, placeName))),
      stripOperationalNarrative(replaceTechnicalHexReferences(response.action_result, placeName)),
    ),
    choices: sanitizeChoices(response.choices, placeName),
    voiceScript: sanitizeVoiceScript(response.voiceScript, placeName),
    narrativeLog: response.narrativeLog ? stripOperationalNarrative(replaceTechnicalHexReferences(response.narrativeLog, placeName)) : response.narrativeLog,
    requiredTest: response.requiredTest ? {
      ...response.requiredTest,
      reason: replaceTechnicalHexReferences(response.requiredTest.reason, placeName),
      consequences: response.requiredTest.consequences ? replaceTechnicalHexReferences(response.requiredTest.consequences, placeName) : response.requiredTest.consequences,
    } : response.requiredTest,
  };
}
