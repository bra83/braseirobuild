import type { CampaignState } from './db';
import type { SceneKind } from './master/types';
import type { GeminiStoryResponse } from '../types';
import type { NarrativeIntentDecision } from './narrativeEngine';

export type NarrativeCadence =
  | 'frozen'
  | 'opening'
  | 'conversation'
  | 'council'
  | 'exploration'
  | 'journey'
  | 'danger'
  | 'combat'
  | 'aftermath';

export interface NarrativeRhythmPlan {
  cadence: NarrativeCadence;
  sceneKind: SceneKind;
  targetParagraphMin: number;
  targetParagraphMax: number;
  targetWordMin: number;
  targetWordMax: number;
  maxWorldBeats: number;
  maxNpcTurns: number;
  firstBeat: string;
  handoffRule: string;
  compressionRule: string;
  observationRule: string;
  profileReason: string;
}

export interface NarrativeRhythmAssessment {
  repair: boolean;
  flags: string[];
  words: number;
  paragraphs: number;
  transitionCount: number;
  longestQuotedWords: number;
}

function clampInt(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, Math.round(value)));
}

function sceneKindOf(campaign: CampaignState, override?: SceneKind): SceneKind {
  return override || campaign.masterState?.scene?.kind || 'exploration';
}

function normalized(value: string): string {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function wordCount(value: string): number {
  return String(value || '').trim().split(/\s+/).filter(Boolean).length;
}

function paragraphCount(value: string): number {
  return String(value || '').trim() ? String(value || '').trim().split(/\n\s*\n/).filter(Boolean).length : 0;
}

function longestQuotedPassageWords(value: string): number {
  const passages = [
    ...String(value || '').matchAll(/[“"]([^”"]{8,1800})[”"]/g),
    ...String(value || '').matchAll(/(?:^|\n)\s*—\s*([^\n]{8,1800})/g),
  ];
  return passages.reduce((max, match) => Math.max(max, wordCount(match[1] || '')), 0);
}

/**
 * Cadência local derivada do objetivo de reproduzir uma mesa de actual play,
 * não um conto contínuo. O sistema mantém fala social curta e interruptível,
 * permite uma janela descritiva maior em exploração e comprime deslocamento
 * sem decisão. Os limites são faixas, nunca metas rígidas de enchimento.
 */
export function buildNarrativeRhythmPlan(
  campaign: CampaignState,
  decision: NarrativeIntentDecision,
  options: { isOpening?: boolean; sceneKind?: SceneKind } = {},
): NarrativeRhythmPlan {
  const kind = sceneKindOf(campaign, options.sceneKind);
  const pacing = campaign.masterState?.sessionDirector?.pacing || 'quiet';

  if (!decision.advancesFiction) {
    return {
      cadence: 'frozen', sceneKind: kind,
      targetParagraphMin: 0, targetParagraphMax: 0,
      targetWordMin: 0, targetWordMax: 120,
      maxWorldBeats: 0, maxNpcTurns: 0,
      firstBeat: 'Responda diretamente ao pedido sem transformar a resposta em nova cena.',
      handoffRule: 'Termine depois da resposta necessária; o instante ficcional permanece congelado.',
      compressionRule: 'Não narre passagem de tempo, reação nova, deslocamento ou acontecimento.',
      observationRule: 'Use apenas fatos já conhecidos ou perceptíveis no checkpoint.',
      profileReason: 'Consulta, recap, clarificação ou planejamento não consome uma batida da ficção.',
    };
  }

  if (options.isOpening) {
    return {
      cadence: 'opening', sceneKind: kind,
      targetParagraphMin: 3, targetParagraphMax: 5,
      targetWordMin: 210, targetWordMax: 430,
      maxWorldBeats: 3, maxNpcTurns: 2,
      firstBeat: 'Abra com uma imagem espacial clara e alguma atividade já acontecendo; não comece por explicação histórica.',
      handoffRule: 'Apresente situação suficiente para uma primeira decisão real e pare antes de resolver a aventura por antecipação.',
      compressionRule: 'Contexto passado entra só se necessário para entender o presente; não faça prólogo enciclopédico.',
      observationRule: 'Mostre apenas o que está ao alcance da Companhia na posição inicial e uma ou duas possibilidades implícitas.',
      profileReason: 'Abertura precisa assentar lugar, movimento e problema, mas ainda funcionar como primeira rodada de mesa.',
    };
  }

  if (decision.intentClass === 'speech-diegetic' || kind === 'dialogue') {
    return {
      cadence: 'conversation', sceneKind: kind,
      targetParagraphMin: 1, targetParagraphMax: 2,
      targetWordMin: 55, targetWordMax: 180,
      maxWorldBeats: 1, maxNpcTurns: 2,
      firstBeat: 'Comece pela reação da pessoa interpelada: gesto, pausa, resposta ou recusa. Não recapitule o cenário antes disso.',
      handoffRule: 'Depois da resposta e de no máximo uma complicação social observável, devolva a palavra ao jogador.',
      compressionRule: 'Corte saudações repetidas, deslocamentos triviais e exposição que o NPC não diria naturalmente.',
      observationRule: 'Emoção e intenção aparecem por comportamento e escolha de palavras; não rotule estados internos.',
      profileReason: 'Diálogo de mesa funciona por turnos curtos e frequentes, não por monólogos do Mestre.',
    };
  }

  if (kind === 'council') {
    return {
      cadence: 'council', sceneKind: kind,
      targetParagraphMin: 1, targetParagraphMax: 3,
      targetWordMin: 90, targetWordMax: 240,
      maxWorldBeats: 2, maxNpcTurns: 3,
      firstBeat: 'Mostre quem reage primeiro ao argumento atual e como a sala muda; não faça todos os participantes discursarem de uma vez.',
      handoffRule: 'Pare quando surgir uma resposta, objeção, concessão ou pergunta que mereça decisão da Companhia.',
      compressionRule: 'Resuma concordâncias menores; expanda somente a fala que muda a direção do Conselho.',
      observationRule: 'Mudança de atitude deve ser perceptível em falas, silêncio, apoio, interrupção ou postura; não mostre placares invisíveis.',
      profileReason: 'Conselho precisa respirar como conversa coletiva, com foco alternado e espaço para réplica.',
    };
  }

  if (kind === 'journey') {
    return {
      cadence: 'journey', sceneKind: kind,
      targetParagraphMin: 1, targetParagraphMax: 3,
      targetWordMin: 90, targetWordMax: 260,
      maxWorldBeats: 2, maxNpcTurns: 1,
      firstBeat: 'Situe a Companhia no trecho atual da viagem e mostre uma mudança concreta de terreno, tempo, esforço ou orientação.',
      handoffRule: 'Se surgir evento, perigo, escolha de rota ou descoberta, pare ali; não narre também sua solução.',
      compressionRule: 'Dias sem decisão são condensados. Não invente encontro só para preencher deslocamento.',
      observationRule: 'Distância e paisagem aparecem por sinais físicos do percurso; não antecipe locais ainda não alcançados.',
      profileReason: 'Viagem alterna compressão de rotina com janelas curtas de presença quando algo realmente importa.',
    };
  }

  if (kind === 'combat') {
    return {
      cadence: 'combat', sceneKind: kind,
      targetParagraphMin: 1, targetParagraphMax: 2,
      targetWordMin: 55, targetWordMax: 175,
      maxWorldBeats: 1, maxNpcTurns: 1,
      firstBeat: 'Resolva somente a ação mecanicamente validada agora: origem, trajetória, contato e resultado.',
      handoffRule: 'Pare assim que a nova posição, ferimento, vantagem ou ameaça do turno estiver clara.',
      compressionRule: 'Não narre uma rodada inteira nem ações de outros combatentes que ainda não foram resolvidas.',
      observationRule: 'Descreva impacto e reação física antes de explicar o significado tático.',
      profileReason: 'Combate ganha energia com cortes curtos e consequência legível por ação, não com resumo cinematográfico do turno inteiro.',
    };
  }

  if (kind === 'rest' || kind === 'fellowship') {
    return {
      cadence: 'aftermath', sceneKind: kind,
      targetParagraphMin: 1, targetParagraphMax: 3,
      targetWordMin: 80, targetWordMax: 230,
      maxWorldBeats: 2, maxNpcTurns: 2,
      firstBeat: 'Mostre a mudança de ritmo depois do esforço: uma tarefa, conversa, refeição, reparo, silêncio ou chegada a abrigo.',
      handoffRule: 'Pare em uma decisão de Sociedade, relação ou próximo rumo; não transforme descanso em resumo de semanas.',
      compressionRule: 'Rotina de recuperação é resumida; detalhes aparecem quando revelam relação, consequência ou oportunidade.',
      observationRule: 'Deixe consequências anteriores reaparecerem organicamente em comportamento, feridas, objetos e relações.',
      profileReason: 'Alívio funciona melhor como contraste e espaço social do que como exposição de campanha.',
    };
  }

  const danger = ['rising', 'tense', 'climax'].includes(pacing);
  if (danger) {
    return {
      cadence: 'danger', sceneKind: kind,
      targetParagraphMin: 2, targetParagraphMax: 4,
      targetWordMin: 130, targetWordMax: 330,
      maxWorldBeats: 2, maxNpcTurns: 2,
      firstBeat: 'Retome a posição atual e faça o perigo aparecer por alteração concreta no espaço, som, comportamento ou movimento.',
      handoffRule: 'Permita uma escalada curta e pare antes da próxima decisão ou teste relevante.',
      compressionRule: 'Não acumule três ameaças novas em um único turno; escolha a dominante.',
      observationRule: 'Confirme somente o que os Heróis têm evidência para reconhecer; preserve orientação incerta enquanto ela for real.',
      profileReason: 'Tensão em mesa cresce por ondas e cortes, não por intensidade máxima contínua.',
    };
  }

  return {
    cadence: 'exploration', sceneKind: kind,
    targetParagraphMin: 2, targetParagraphMax: 4,
    targetWordMin: 120, targetWordMax: 320,
    maxWorldBeats: 2, maxNpcTurns: 2,
    firstBeat: 'Comece da posição atual e revele uma camada espacial por vez: panorama/local, movimento e um detalhe que merece atenção.',
    handoffRule: 'Pare quando houver algo examinável, uma direção possível, uma presença ou uma decisão; não descreva o local inteiro por antecipação.',
    compressionRule: 'Travessias banais são curtas; descoberta recebe espaço. Não enumere móveis, ruínas, portas ou pistas em catálogo.',
    observationRule: 'Dê evidência antes de interpretação. Espaços ainda não examinados permanecem desconhecidos.',
    profileReason: 'Exploração aceita uma janela descritiva maior, mas continua dividida em batidas interruptíveis de mesa.',
  };
}

export function narrativeRhythmPrompt(plan: NarrativeRhythmPlan): string {
  return [
    '\n--- CADÊNCIA DE MESA / ACTUAL PLAY ---',
    `Cadência deste turno: ${plan.cadence}. Cena formal: ${plan.sceneKind}.`,
    `Faixa preferida (não obrigação de preencher): ${plan.targetParagraphMin}-${plan.targetParagraphMax} parágrafos, aproximadamente ${plan.targetWordMin}-${plan.targetWordMax} palavras.`,
    `Máximo de batidas novas do mundo antes de devolver agência: ${plan.maxWorldBeats}. Máximo de intervenções relevantes de NPCs: ${plan.maxNpcTurns}.`,
    `PRIMEIRA BATIDA: ${plan.firstBeat}`,
    `DEVOLUÇÃO: ${plan.handoffRule}`,
    `COMPRESSÃO: ${plan.compressionRule}`,
    `OBSERVAÇÃO: ${plan.observationRule}`,
    `MOTIVO DE DIREÇÃO: ${plan.profileReason}`,
    'A faixa serve para controlar ritmo e impedir monólogo. Se a ação já estiver resolvida em menos texto, pare antes. Se uma revelação realmente exigir mais espaço, expanda somente uma batida e devolva agência.',
  ].join('\n') + '\n';
}

export function assessNarrativeRhythm(
  response: Pick<GeminiStoryResponse, 'atmospheric_narration' | 'action_result'>,
  plan: NarrativeRhythmPlan,
): NarrativeRhythmAssessment {
  const narration = String(response.atmospheric_narration || '').trim();
  const words = wordCount(narration);
  const paragraphs = paragraphCount(narration);
  const text = normalized(narration);
  const flags: string[] = [];

  const transitionMatches = text.match(/\b(?:entao|depois|logo depois|em seguida|pouco depois|minutos depois|horas depois|mais tarde|ao fim de|quando finalmente)\b/g) || [];
  const transitionCount = transitionMatches.length;
  const quoted = longestQuotedPassageWords(narration);

  if (plan.cadence === 'frozen') {
    if (words > 0) flags.push('consulta sem avanço recebeu narração ficcional');
    return { repair: flags.length > 0, flags, words, paragraphs, transitionCount, longestQuotedWords: quoted };
  }

  const hardWordMax = Math.max(plan.targetWordMax + 80, Math.round(plan.targetWordMax * 1.35));
  const hardParagraphMax = plan.targetParagraphMax + 1;
  if (words > hardWordMax) flags.push(`turno virou monólogo (${words} palavras; faixa de cadência termina em ${plan.targetWordMax})`);
  if (paragraphs > hardParagraphMax) flags.push(`turno fragmentou em ${paragraphs} parágrafos e demorou demais para devolver agência`);

  if (transitionCount > plan.maxWorldBeats + 1) flags.push('muitas transições temporais/causais no mesmo turno indicam avanço excessivo da ficção');
  if (quoted > 95 && ['conversation', 'council', 'aftermath'].includes(plan.cadence)) flags.push(`fala de NPC longa demais para uma rodada de mesa (${quoted} palavras sem devolução)`);
  if (plan.cadence === 'conversation' && words > 210) flags.push('interação social simples ficou longa demais antes da réplica do jogador');
  if (plan.cadence === 'combat' && words > 210) flags.push('combate narrou mais do que a ação atual deveria resolver');
  if (plan.cadence === 'journey' && transitionCount >= 4) flags.push('jornada atravessou etapas demais em uma única resposta');

  const explainers = text.match(/\b(?:isso significa que|fica claro que|voce entende que|voce percebe que|em outras palavras|o que quer dizer que)\b/g) || [];
  if (explainers.length >= 2) flags.push('texto explica conclusões repetidamente em vez de deixar sinais e consequências falarem');

  const forcedChoice = /\b(?:voce deve|voces devem|a unica opcao|nao ha outra escolha|a melhor opcao e)\b/.test(text);
  if (forcedChoice) flags.push('narração empurrou uma decisão específica em vez de devolver agência');

  return { repair: flags.length > 0, flags, words, paragraphs, transitionCount, longestQuotedWords: quoted };
}

export function rhythmRepairBrief(plan: NarrativeRhythmPlan, flags: string[]): string {
  return [
    'REPARO DE CADÊNCIA DE MESA — preserve fatos, falas válidas, regras e consequências. Reescreva somente o ritmo e o ponto de corte.',
    `Cadência exigida: ${plan.cadence}.`,
    `Problemas: ${flags.length ? flags.map(flag => `- ${flag}`).join('\n') : '- resposta longa ou pouco interruptível'}`,
    `Primeira batida: ${plan.firstBeat}`,
    `Ponto de corte: ${plan.handoffRule}`,
    `Compressão: ${plan.compressionRule}`,
    `Faixa preferida: ${plan.targetParagraphMin}-${plan.targetParagraphMax} parágrafos e ${plan.targetWordMin}-${plan.targetWordMax} palavras, sem preencher por obrigação.`,
  ].join('\n');
}
