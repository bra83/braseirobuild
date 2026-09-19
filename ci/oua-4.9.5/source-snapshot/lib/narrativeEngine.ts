import type { CampaignState, Npc, SceneNode } from './db';
import type { DirectorPlan, MasterCommand } from './master/types';
import type { GeminiStoryResponse } from '../types';
import type { InputChannel } from './autonomy/types';

export type NarrativeIntentClass =
  | 'action-diegetic'
  | 'speech-diegetic'
  | 'test-mechanic'
  | 'rule-query'
  | 'context-query'
  | 'recap'
  | 'spatial-clarification'
  | 'planning';

export interface NarrativeIntentDecision {
  intentClass: NarrativeIntentClass;
  advancesFiction: boolean;
  reason: string;
  questionCount: number;
}

export interface NarrativeCheckpoint {
  sceneId: string | null;
  location: string;
  timeOfDay: string;
  weather: string;
  presentNpcIds: string[];
  presentNpcNames: string[];
  positions: string[];
  visibleObjectsAndClues: string[];
  lastSpeech: string;
  actionInProgress: string;
  previousNarrationTail: string;
  formalSceneTitle: string;
  formalSceneObjective: string;
  formalSceneKind: string;
}

export interface NarrativeQualityAssessment {
  repair: boolean;
  flags: string[];
  signature: string;
}

function normalize(value: unknown): string {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9?!.,:;\-\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function compact(value: unknown, max = 1000): string {
  const text = String(value ?? '').replace(/\s+/g, ' ').trim();
  return text.length <= max ? text : text.slice(-max);
}

function countQuestions(value: string): number {
  const literal = (value.match(/\?/g) || []).length;
  if (literal) return literal;
  const normalized = normalize(value);
  const interrogatives = normalized.match(/\b(quem|qual|quais|quando|onde|como|por que|porque|quanto|quantos|o que|que aconteceu)\b/g) || [];
  return Math.min(4, interrogatives.length);
}

const DIEGETIC_SPEECH = /\b(pergunto|perguntamos|digo|dizemos|falo|falamos|respondo|respondemos|conto|contamos|explico|explicamos|agrade[cç]o|agradecemos|pe[cç]o|pedimos|ofere[cç]o|oferecemos|cumprimento|cumprimentamos|me apresento|nos apresentamos|chamo|chamamos)\b/i;
const RULE_QUERY = /\b(regra|regras|manual|mec[aâ]nica|como funciona|como rola|como funciona o teste|qual teste|qual per[ií]cia|quantos dados|n[uú]mero alvo|dado de proeza|dados de sucesso|tengwar|runa de gandalf|olho de sauron)\b/i;
const RECAP = /\b(recapi|resum|relemb|me lembra|lembre-me|o que aconteceu|o que sabemos at[eé] agora|qual foi o resultado anterior)\b/i;
const SPATIAL = /\b(onde (?:eu|n[oó]s|a companhia) (?:estou|estamos)|onde est[aá] (?:a|o|os|as)|onde fica|onde estava|qual (?:porta|sa[ií]da|caminho|corredor)|de que lado|a que dist[aâ]ncia|quem est[aá] perto|quem est[aá] aqui|como [eé] o lugar|como est[aá] disposto|posi[cç][aã]o)\b/i;
const PLANNING = /\b(talvez|penso em|estou pensando|pretendo|planejo|seria melhor|poder[ií]amos|podemos talvez|quero primeiro pensar|antes de agir|se eu fosse|e se eu)\b/i;
const TEST_MECHANIC = /^\s*\[(?:RESOLVER TESTE PENDENTE|ROLAGEM|TESTE)\]|\b(rolo|rolamos|fiz o teste|resultado do teste|usei esperança|gasto esperança|gastar esperança)\b/i;
const CONTEXT_QUERY = /\b(essa cidade|esse povo|essa regi[aã]o|eu sei|n[oó]s sabemos|meu personagem sabe|a companhia sabe|[eé] permitido|costuma haver|quem [eé]|o que [eé]|qual [eé]|como [eé])\b/i;
const EXPLICIT_ACTION = /\b(vou|vamos|abro|abrimos|entro|entramos|saio|sa[ií]mos|ando|andamos|corro|corremos|ataco|atacamos|examino|examinamos|procuro|procuramos|sigo|seguimos|espero|esperamos|acendo|apagamos|entrego|pegamos|pego|subo|descemos|des[cç]o|atravesso|aproximo|afasto|observo|escuto|toco|empurro|puxo|escondo|ajoelho|monto|cavalgo)\b/i;

export function classifyNarrativeIntent(playerAction: string, inputChannel?: InputChannel): NarrativeIntentDecision {
  const action = String(playerAction || '').replace(/^\[[^\]]+\]\s*/g, '').trim();
  const questionCount = countQuestions(action);
  if (inputChannel === 'GM_HELP') return { intentClass: RULE_QUERY.test(action) ? 'rule-query' : 'context-query', advancesFiction: false, reason: 'Canal GM_HELP vinculante: a caixa Mestre nunca avança a ficção.', questionCount };
  if (inputChannel === 'PLAYER_ACTION') {
    if (TEST_MECHANIC.test(action)) return { intentClass: 'test-mechanic', advancesFiction: true, reason: 'Canal PLAYER_ACTION vinculante com mecânica explícita.', questionCount };
    if (DIEGETIC_SPEECH.test(action)) return { intentClass: 'speech-diegetic', advancesFiction: true, reason: 'Canal PLAYER_ACTION vinculante: fala/ação diegética permanece na ficção.', questionCount };
    return { intentClass: 'action-diegetic', advancesFiction: true, reason: 'Canal PLAYER_ACTION vinculante: heurísticas não podem converter a ação do jogador em consulta congelada.', questionCount };
  }
  if (TEST_MECHANIC.test(action)) return { intentClass: 'test-mechanic', advancesFiction: true, reason: 'A mensagem resolve ou declara uma mecânica já inserida na ficção.', questionCount };
  if (DIEGETIC_SPEECH.test(action)) return { intentClass: 'speech-diegetic', advancesFiction: true, reason: 'O jogador declarou fala dirigida a alguém no mundo.', questionCount };
  if (RULE_QUERY.test(action) && (questionCount > 0 || /como|qual|quanto|regra|manual/i.test(action))) return { intentClass: 'rule-query', advancesFiction: false, reason: 'A mensagem pede uma regra, não uma mudança ficcional.', questionCount };
  if (RECAP.test(action)) return { intentClass: 'recap', advancesFiction: false, reason: 'A mensagem pede recapitulação e deve preservar o instante da cena.', questionCount };
  if (SPATIAL.test(action) && !EXPLICIT_ACTION.test(action)) return { intentClass: 'spatial-clarification', advancesFiction: false, reason: 'A mensagem pede orientação espacial sem deslocar a Companhia.', questionCount };
  if (PLANNING.test(action) && !/\b(fa[cç]o isso|ent[aã]o fa[cç]o|decido|executo|vou mesmo|vamos mesmo)\b/i.test(action)) return { intentClass: 'planning', advancesFiction: false, reason: 'A mensagem expressa intenção ou hipótese, não execução.', questionCount };
  if (questionCount > 0 && CONTEXT_QUERY.test(action) && !EXPLICIT_ACTION.test(action)) return { intentClass: 'context-query', advancesFiction: false, reason: 'A mensagem pede contexto que o personagem pode conhecer sem alterar o mundo.', questionCount };
  if (questionCount > 0 && !EXPLICIT_ACTION.test(action)) return { intentClass: 'context-query', advancesFiction: false, reason: 'A mensagem é uma pergunta sem ação diegética declarada; o mundo permanece no mesmo instante.', questionCount };
  return { intentClass: 'action-diegetic', advancesFiction: true, reason: 'A mensagem declara uma ação no mundo.', questionCount };
}

export function latestFictionScene(campaign: CampaignState): SceneNode | null {
  for (let i = campaign.history.length - 1; i >= 0; i -= 1) {
    const node = campaign.history[i];
    if (node?.narrativeMeta?.advancesFiction !== false) return node;
  }
  return null;
}

function extractLastSpeech(value: string): string {
  const text = String(value || '');
  const quoted = [...text.matchAll(/[“"]([^”"]{2,280})[”"]/g)].map(match => match[1].trim());
  if (quoted.length) return quoted.at(-1) || '';
  const emDash = text.split(/\n/).map(line => line.trim()).filter(line => /^—\s*\S/.test(line));
  if (emDash.length) return compact(emDash.at(-1) || '', 320);
  const speechLike = text.match(/(?:diz|pergunta|responde|murmura|retruca|sussurra)[^.!?]{0,220}[.!?]/gi);
  return compact(speechLike?.at(-1) || '', 320);
}

function visibleClueLines(campaign: CampaignState): string[] {
  const recentFictionFacts = campaign.history
    .filter(node => node.narrativeMeta?.advancesFiction !== false)
    .slice(-4)
    .flatMap(node => node.narrativeMeta?.visibleFacts || []);
  const persisted = `${campaign.clues || ''}\n${campaign.recentEvents || ''}`
    .split(/\n+/)
    .map(item => item.replace(/^\s*[•*\-]\s*/, '').trim())
    .filter(Boolean);
  return Array.from(new Set([...persisted, ...recentFictionFacts]
    .map(item => compact(item, 360))
    .filter(Boolean)))
    .slice(-12);
}

export function buildNarrativeCheckpoint(campaign: CampaignState, npcs: Npc[]): NarrativeCheckpoint {
  const previous = latestFictionScene(campaign);
  const master = campaign.masterState;
  const ids = Array.from(new Set([
    ...(campaign.presentNpcIds || []),
    ...(previous?.presentNpcIds || []),
    ...(master?.scene?.participantNpcIds || []),
  ].filter(Boolean)));
  const names = ids.map(id => npcs.find(npc => npc.id === id)?.name).filter((name): name is string => Boolean(name));
  const combatPositions = (master?.combat?.actors || [])
    .filter(actor => actor.position)
    .slice(0, 12)
    .map(actor => `${actor.actorId}: x=${Math.round(actor.position!.x)}, y=${Math.round(actor.position!.y)}`);
  return {
    sceneId: previous?.id || null,
    location: previous?.location || master?.position?.locationName || campaign.currentLocation,
    timeOfDay: previous?.timeOfDay || '',
    weather: previous?.weather || '',
    presentNpcIds: ids,
    presentNpcNames: names,
    positions: combatPositions,
    visibleObjectsAndClues: visibleClueLines(campaign),
    lastSpeech: extractLastSpeech(previous?.atmospheric_narration || ''),
    actionInProgress: previous?.playerAction || '',
    previousNarrationTail: compact(previous?.atmospheric_narration || '', 1500),
    formalSceneTitle: master?.scene?.title || '',
    formalSceneObjective: master?.scene?.objective || '',
    formalSceneKind: master?.scene?.kind || 'exploration',
  };
}

export function narrativeCheckpointPrompt(checkpoint: NarrativeCheckpoint, decision: NarrativeIntentDecision, action: string): string {
  const lines = [
    '\n--- CHECKPOINT NARRATIVO PROFISSIONAL ---',
    `Classificação interna da mensagem: ${decision.intentClass}.`,
    `Altera a ficção agora: ${decision.advancesFiction ? 'SIM' : 'NÃO'}.`,
    `Motivo: ${decision.reason}`,
    `Local: ${checkpoint.location}.`,
    `Horário conhecido: ${checkpoint.timeOfDay || 'usar relógio canônico abaixo'}.`,
    `Clima conhecido: ${checkpoint.weather || 'usar clima canônico abaixo'}.`,
    `Presentes: ${checkpoint.presentNpcNames.join(', ') || checkpoint.presentNpcIds.join(', ') || 'nenhum NPC formalizado'}.`,
    `Posições táticas conhecidas: ${checkpoint.positions.join('; ') || 'nenhuma posição tática relevante'}.`,
    `Objetos/pistas visíveis já estabelecidos: ${checkpoint.visibleObjectsAndClues.join(' | ') || 'nenhum além do contexto atual'}.`,
    `Última fala direta reconhecível: ${checkpoint.lastSpeech || 'nenhuma'}.`,
    `Ação que levou ao ponto atual: ${checkpoint.actionInProgress || 'nenhuma'}.`,
    `Final da cena ficcional: ${checkpoint.previousNarrationTail || 'primeira cena'}.`,
    `Mensagem do jogador agora: ${action}.`,
  ];
  if (!decision.advancesFiction) {
    lines.push('REGRA DE CONGELAMENTO INTERNO: responda a consulta/planejamento sem avançar relógio, NPCs, ameaças, mapa, cena, clima, acontecimentos ou consequências. Depois de responder, o checkpoint ficcional continua exatamente igual. Não exponha esta regra ao jogador.');
  }
  if (decision.questionCount > 1) lines.push(`A mensagem contém ${decision.questionCount} perguntas/questões. Responda a todas as pertinentes; não deixe perguntas desaparecerem por condensação.`);
  return lines.join('\n') + '\n';
}

const SAFE_NON_ADVANCING_COMMANDS = new Set<MasterCommand['type']>();

export function enforceNarrativeIntent(plan: DirectorPlan, decision: NarrativeIntentDecision): DirectorPlan {
  if (decision.advancesFiction) return { ...plan, narrativeIntentClass: decision.intentClass, advancesFiction: true };
  return {
    ...plan,
    narrativeIntentClass: decision.intentClass,
    advancesFiction: false,
    requiredTest: null,
    commands: (plan.commands || []).filter(command => SAFE_NON_ADVANCING_COMMANDS.has(command.type)),
    npcDecisions: [],
    revelationsAllowed: decision.intentClass === 'recap' || decision.intentClass === 'context-query' || decision.intentClass === 'spatial-clarification'
      ? plan.revelationsAllowed
      : [],
    narrationBrief: `${plan.narrationBrief || ''}\nNÃO AVANCE A FICÇÃO. Responda diretamente ao pedido do jogador usando apenas fatos já perceptíveis/conhecidos. Não crie acontecimento, reação nova, passagem de tempo, movimento ou nova pista.`.trim(),
  };
}

function wordSet(value: string): Set<string> {
  return new Set(normalize(value).split(/\s+/).filter(word => word.length > 3).slice(0, 45));
}

function jaccard(a: Set<string>, b: Set<string>): number {
  if (!a.size || !b.size) return 0;
  let intersection = 0;
  for (const item of a) if (b.has(item)) intersection += 1;
  return intersection / (a.size + b.size - intersection);
}

export function narrativeOpeningSignature(value: string): string {
  const firstParagraph = String(value || '').split(/\n\s*\n/)[0] || '';
  return normalize(firstParagraph).split(/\s+/).slice(0, 28).join(' ');
}

export function assessNarrativeQuality(
  response: Pick<GeminiStoryResponse, 'atmospheric_narration' | 'action_result'>,
  campaign: CampaignState,
  decision: NarrativeIntentDecision,
  isOpening = false,
): NarrativeQualityAssessment {
  const narration = String(response.atmospheric_narration || '').trim();
  const result = String(response.action_result || '').trim();
  const normalized = normalize(`${narration} ${result}`);
  const flags: string[] = [];
  const signature = narrativeOpeningSignature(narration);

  if (!decision.advancesFiction) {
    if (narration.split(/\s+/).filter(Boolean).length > 90) flags.push('consulta sem avanço virou uma nova cena longa');
    if (/\b(ent[aã]o|de repente|nesse momento|logo depois|minutos depois|horas depois|mais tarde)\b/.test(normalized)) flags.push('consulta sem avanço introduziu progressão temporal ou acontecimento');
    return { repair: flags.length > 0, flags, signature };
  }

  const genericAtmosphere = /\b(?:o |a )?ambiente (?:era|fica|parece|est[aá])[^.!?]{0,45}\b(?:sombrio|misterioso|tenso|amea[cç]ador)\b|\b(?:algo|alguma coisa) (?:n[aã]o est[aá] certo|parece errado)\b/;
  const prematureConclusion = /\bvoc[eê] percebe que .{0,70}\bment(?:e|indo)\b|\b(?:claramente|obviamente) (?:ele|ela|eles|elas) (?:quer|pretende|planeja|esconde)\b/;
  const artificialQuestion = /(?:\bo que (?:voc[eê]|voc[eê]s) faz(?:em)?(?: agora)?|\bcomo (?:voc[eê]|voc[eê]s) reage(?:m)?|\bqual (?:e|é) (?:a )?sua pr[oó]xima a[cç][aã]o)\??\s*$/;
  if (genericAtmosphere.test(normalized)) flags.push('atmosfera genérica foi nomeada em vez de construída por sinais concretos');
  if (prematureConclusion.test(normalized)) flags.push('conclusão oculta foi entregue antes da evidência');
  if (artificialQuestion.test(normalized)) flags.push('fechamento artificial devolveu agência por pergunta automática');

  const activeMotion = /\b(anda|caminha|atravessa|ergue|abaixa|fecha|abre|puxa|empurra|carrega|arruma|limpa|fala|responde|olha|vira|corre|chega|sai|entra|passa|mexe|amarra|solta|aponta|recolhe|serve|trabalha|discute|esconde|observa|para|continua|aproxima|afasta|respira|hesita|estende|recolhe)\b/.test(normalized);
  if (!activeMotion && narration.split(/\s+/).filter(Boolean).length > 110) flags.push('cena longa e parada, sem ação observável no mundo');

  if (/\b(?:ele est[aá] nervoso|ela est[aá] nervosa|parece nervos[oa]|parecia nervos[oa]|parece suspeit[oa]|parecia suspeit[oa]|parece assustad[oa]|parecia assustad[oa])\b/.test(normalized)) flags.push('emoção foi nomeada diretamente em vez de aparecer por comportamento');
  if ((narration.match(/\b(?:sombrio|antigo|misterioso|silencioso|amea[cç]ador|melanc[oó]lico)\b/g) || []).length >= 5) flags.push('excesso de adjetivação atmosférica');
  if (/\bporta (?:range|rangendo|rangeu)\b/.test(normalized) && campaign.history.slice(-5).some(node => /porta (?:range|rangendo|rangeu)/i.test(node.atmospheric_narration))) flags.push('repetição de mecanismo de suspense recente');
  if (/\bsil[eê]ncio (?:repentino|cai|caiu|subitamente)\b/.test(normalized) && campaign.history.slice(-5).some(node => /sil[eê]ncio (?:repentino|cai|caiu|subitamente)/i.test(node.atmospheric_narration))) flags.push('repetição de silêncio como atalho de suspense');

  if (isOpening && signature) {
    const current = wordSet(signature);
    const repeated = campaign.history.slice(-8).some(node => jaccard(current, wordSet(narrativeOpeningSignature(node.atmospheric_narration))) >= 0.62);
    if (repeated) flags.push('abertura muito semelhante a uma abertura recente');
  }

  return { repair: flags.length > 0, flags, signature };
}

export function professionalRepairBrief(flags: string[], decision: NarrativeIntentDecision): string {
  const issues = flags.map(flag => `- ${flag}`).join('\n');
  return `REPARO NARRATIVO PROFISSIONAL — preserve todos os fatos, regras, falas válidas e consequências já decididas; reescreva somente a apresentação.\nProblemas detectados:\n${issues || '- qualidade abaixo do padrão'}\nUse: CONTEXTO → ESPAÇO → MOVIMENTO → FOCO → IMPLICAÇÃO → ABERTURA PARA AÇÃO. Mostre emoção e suspeita por gesto, silêncio, objeto, escolha de palavras ou comportamento. Não explique a conclusão que o jogador ainda pode inferir. ${decision.advancesFiction ? 'Pare assim que existir uma decisão real.' : 'Não avance a ficção; responda e preserve o checkpoint.'}`;
}
