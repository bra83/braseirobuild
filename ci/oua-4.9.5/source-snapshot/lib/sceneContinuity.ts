import type { CampaignState, Npc, SceneNode } from './db';
import type { DirectorPlan, MasterCommand } from './master/types';
import type { GeminiStoryResponse } from '../types';
import type { EnvironmentContext, WeatherForecast } from './autonomy/types';
import { latestFictionScene } from './narrativeEngine';

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function tail(value: string, max = 1200): string {
  const text = String(value || '').trim();
  return text.length <= max ? text : text.slice(-max);
}

const SOCIAL_ACTION = /\b(apresent|cumpriment|saud|respon|digo|dizer|falo|falar|convers|pergunt|question|explic|conto|contar|agradec|pe[cç]o|pedir|ofere[cç]|mostr|entreg|aceit|recus|negoci|barganh|escut|ou[cç]o|ouvir)\b/i;
const EXPLICIT_EXIT = /\b(vou embora|vamos embora|parto|partimos|deixo o local|deixamos o local|saio|sa[ií]mos|fujo|fugimos|sigo viagem|seguimos viagem|retomo a jornada|retomamos a jornada|afasto-me|nos afastamos|abandono o lugar|abandonamos o lugar)\b/i;

export interface SceneContinuityAnchor {
  hasPreviousScene: boolean;
  preserveScene: boolean;
  socialContinuation: boolean;
  previousNode: SceneNode | null;
  previousLocation: string;
  previousAction: string;
  previousResult: string;
  previousNarrationTail: string;
  presentNpcIds: string[];
  presentNpcNames: string[];
  currentSceneTitle: string;
  currentSceneObjective: string;
  currentSceneKind: string;
  playerAction: string;
}

export function buildSceneContinuityAnchor(campaign: CampaignState, playerAction: string, npcs: Npc[]): SceneContinuityAnchor {
  const previousNode = latestFictionScene(campaign);
  const master = campaign.masterState;
  const ids = Array.from(new Set([
    ...(campaign.presentNpcIds || []),
    ...(previousNode?.presentNpcIds || []),
    ...(master?.scene?.participantNpcIds || []),
  ].filter(Boolean)));
  const names = ids.map(id => npcs.find(npc => npc.id === id)?.name).filter((name): name is string => Boolean(name));
  // A continuidade social não depende de o Diretor já ter formalizado o NPC.
  // Na primeira cena é comum a prosa apresentar "uma guarda" ou "um viajante"
  // antes de existir um id persistente; a ação do jogador ainda precisa receber
  // resposta dessa mesma pessoa, e o Diretor pode formalizá-la neste turno.
  const socialContinuation = Boolean(previousNode) && SOCIAL_ACTION.test(playerAction);
  const explicitExit = EXPLICIT_EXIT.test(playerAction);
  return {
    hasPreviousScene: Boolean(previousNode),
    preserveScene: Boolean(previousNode) && socialContinuation && !explicitExit,
    socialContinuation,
    previousNode,
    previousLocation: previousNode?.location || campaign.currentLocation,
    previousAction: previousNode?.playerAction || '',
    previousResult: previousNode?.action_result || '',
    previousNarrationTail: tail(previousNode?.atmospheric_narration || ''),
    presentNpcIds: ids,
    presentNpcNames: names,
    currentSceneTitle: master?.scene?.title || '',
    currentSceneObjective: master?.scene?.objective || '',
    currentSceneKind: master?.scene?.kind || 'exploration',
    playerAction,
  };
}

export function continuityPrompt(anchor: SceneContinuityAnchor): string {
  if (!anchor.hasPreviousScene) return '';
  const npcLine = anchor.presentNpcNames.length ? anchor.presentNpcNames.join(', ') : 'nenhum ID persistente ainda; preserve o interlocutor descrito no final da narração anterior e formalize-o com upsert_npc/set_present_npcs se ele continuar relevante';
  return `\n--- CONTINUIDADE IMEDIATA OBRIGATÓRIA ---\nA cena anterior NÃO é um resumo distante: é o instante imediatamente anterior a esta ação.\nLocal anterior: ${anchor.previousLocation}.\nCena formal: ${anchor.currentSceneTitle || anchor.currentSceneKind}; objetivo=${anchor.currentSceneObjective || 'não informado'}.\nNPCs que continuam fisicamente presentes: ${npcLine}.\nAção anterior do jogador: ${anchor.previousAction || 'nenhuma'}.\nResultado anterior: ${anchor.previousResult || 'nenhum'}.\nFinal da narração anterior: ${anchor.previousNarrationTail || 'não disponível'}.\nAÇÃO AGORA: ${anchor.playerAction}.\n${anchor.preserveScene ? 'REGRA DURA: esta é continuação da mesma interação. Não feche, troque ou reinicie a cena; não faça o interlocutor nem os NPCs presentes desaparecerem. Se o interlocutor ainda não tiver ID persistente, formalize a MESMA pessoa agora em vez de inventar outra. A primeira batida da próxima narração deve ser a reação imediata à ação do jogador.' : 'Mantenha causalidade direta. Só mude de cena, local ou participantes quando a ação ou um fato validado realmente produzir essa transição.'}\n`;
}

export function enforceDirectorContinuity(plan: DirectorPlan, anchor: SceneContinuityAnchor): DirectorPlan {
  if (!anchor.preserveScene) return plan;
  const filtered: MasterCommand[] = [];
  for (const command of plan.commands || []) {
    if (command.type === 'close_scene' || command.type === 'open_scene') continue;
    if (command.type === 'set_present_npcs') {
      filtered.push({ ...command, npcIds: Array.from(new Set([...anchor.presentNpcIds, ...(command.npcIds || [])])), reason: `${command.reason} Continuidade: NPCs já presentes foram preservados.` });
      continue;
    }
    filtered.push(command);
  }
  if (anchor.presentNpcIds.length && !filtered.some(command => command.type === 'set_present_npcs')) {
    filtered.push({ type: 'set_present_npcs', npcIds: anchor.presentNpcIds, reason: 'Continuidade direta da interação da cena anterior.' });
  }
  const names = anchor.presentNpcNames.length ? anchor.presentNpcNames.join(', ') : 'os interlocutores já presentes';
  return {
    ...plan,
    mode: plan.mode === 'adventure' ? 'adventure' : plan.mode,
    sceneKind: anchor.currentSceneKind as DirectorPlan['sceneKind'],
    commands: filtered,
    narrationBrief: `${plan.narrationBrief || ''}\nCONTINUIDADE OBRIGATÓRIA: responda primeiro à ação atual dentro da mesma interação com ${names}. Preserve local, participantes, assunto e causalidade. Não introduza um novo gancho antes de mostrar a reação imediata.`.trim(),
  };
}

export function narrationNeedsContinuityRepair(response: GeminiStoryResponse, anchor: SceneContinuityAnchor): boolean {
  if (!anchor.preserveScene) return false;
  const text = normalize(`${response.atmospheric_narration || ''} ${response.action_result || ''}`);
  const mentionsNpc = anchor.presentNpcNames.some(name => {
    const normalized = normalize(name);
    const first = normalized.split(' ')[0];
    return normalized.length > 2 && (text.includes(normalized) || (first.length > 2 && text.includes(first)));
  });
  const hasInteractionReaction = /\b(responde|respondeu|ouve|escuta|encara|assente|reage|recebe|considera|retruca|pergunta|diz|fala|voz|olhar|gesto|guarda|interlocutor)\b/.test(text);
  // Com NPC formal, nome OU reação explícita bastam. Sem ID/nome formal,
  // exigimos ao menos uma reação interpessoal clara para impedir que a cena
  // simplesmente salte para outro assunto.
  return anchor.presentNpcNames.length ? (!mentionsNpc && !hasInteractionReaction) : !hasInteractionReaction;
}

export function timePeriodFromHour(hour: number): string {
  const value = ((Number(hour) % 24) + 24) % 24;
  if (value < 5) return 'Madrugada';
  if (value < 7) return 'Amanhecer';
  if (value < 12) return 'Manhã';
  if (value < 14) return 'Meio-dia';
  if (value < 17) return 'Tarde';
  if (value < 20) return 'Entardecer';
  return 'Noite';
}

export interface SceneEnvironmentBinding {
  weatherLabel: string;
  weatherKind: WeatherForecast['kind'];
  weatherStage: string;
  hour: number;
  minute: number;
  period: string;
  season: string;
  region: string;
  terrain: string;
}

export function buildSceneEnvironmentBinding(environment: EnvironmentContext, hour: number, minute = 0): SceneEnvironmentBinding {
  const kindLabels: Record<WeatherForecast['kind'], string> = {
    clear: 'Tempo aberto', cloudy: 'Céu encoberto', rain: 'Chuva', storm: 'Tempestade', snow: 'Neve', blizzard: 'Nevasca', fog: 'Névoa', wind: 'Vento',
  };
  const gender = new Set<WeatherForecast['kind']>(['rain','storm','snow','blizzard','fog']);
  const intensity = environment.currentWeather.intensity === 'heavy' ? 'forte' : environment.currentWeather.intensity === 'medium' ? (gender.has(environment.currentWeather.kind) ? 'moderada' : 'moderado') : (gender.has(environment.currentWeather.kind) ? 'leve' : 'leve');
  const kind = environment.currentWeather.kind;
  const stage = environment.currentWeather.stage || 'stable';
  const base = kindLabels[kind];
  const weatherLabel = stage === 'indications' ? `Indícios de ${base.toLowerCase()}`
    : stage === 'forming' ? `${base} em formação`
      : stage === 'dissipating' ? `${base} diminuindo`
        : kind === 'clear' || kind === 'cloudy' ? base
          : `${base} ${intensity}`;
  return {
    weatherLabel,
    weatherKind: environment.currentWeather.kind,
    weatherStage: environment.currentWeather.stage || 'stable',
    hour,
    minute,
    period: timePeriodFromHour(hour),
    season: environment.season,
    region: environment.region,
    terrain: environment.terrainClass,
  };
}

export function environmentBindingPrompt(binding: SceneEnvironmentBinding): string {
  return `\n--- ESTADO SENSORIAL CANÔNICO DA CENA ---\nRelógio do mundo: ${String(binding.hour).padStart(2, '0')}:${String(binding.minute).padStart(2, '0')} — ${binding.period}.\nClima externo persistente: ${binding.weatherLabel}.\nRegião/terreno: ${binding.region}; ${binding.terrain}.\nREGRA DURA: o cabeçalho, a narração, a imagem e o áudio descrevem este MESMO estado. Não escreva manhã se for noite; não escreva sol forte se o clima estiver encoberto, chuvoso, nevando ou com névoa. TEMPO ABERTO não significa calor: não invente sol quente, abafamento, frio extremo ou temperatura incomum sem evidência ambiental. Se a cena for interna, indique o horário/clima por luz, temperatura, telhado, janelas, roupas molhadas ou outro indício natural quando isso couber, sem forçar exposição.\n`;
}

function narrationEnvironmentCue(binding: SceneEnvironmentBinding): string {
  const p = binding.period.toLowerCase();
  const when = binding.period === 'Madrugada' ? 'Na madrugada'
    : binding.period === 'Amanhecer' ? 'Ao amanhecer'
      : binding.period === 'Manhã' ? 'Pela manhã'
        : binding.period === 'Meio-dia' ? 'Ao meio-dia'
          : binding.period === 'Tarde' ? 'Durante a tarde'
            : binding.period === 'Entardecer' ? 'No entardecer' : 'À noite';
  switch (binding.weatherKind) {
    case 'rain': return `${when}, a chuva marca o ambiente e altera sons, luz e superfícies ao redor.`;
    case 'storm': return `${when}, a tempestade pesa sobre a região e domina o céu.`;
    case 'snow': return `${when}, a neve modifica a paisagem e abafa parte dos sons.`;
    case 'blizzard': return `${when}, vento e neve fecham a paisagem e reduzem os contornos.`;
    case 'fog': return `${when}, a névoa encurta a visão e filtra a luz.`;
    case 'cloudy': return `${when}, o céu permanece encoberto e a luz é difusa.`;
    case 'wind': return `${when}, o vento atravessa a região e move o ambiente ao redor.`;
    default: return p === 'noite' || p === 'madrugada'
      ? `${when}, o céu está aberto, sem que isso implique calor ou luz diurna.`
      : `${when}, o tempo está aberto e a luz corresponde naturalmente ao horário.`;
  }
}

export function ensureEnvironmentAnchoredResponse(response: GeminiStoryResponse, binding: SceneEnvironmentBinding): GeminiStoryResponse {
  const normalized = normalize(response.atmospheric_narration || '');
  const periodTerms: Record<string, RegExp> = {
    'Madrugada': /madrugada/, 'Amanhecer': /amanhecer|aurora/, 'Manhã': /manha/, 'Meio-dia': /meio dia|meio-dia/, 'Tarde': /tarde/, 'Entardecer': /entardecer|crepusculo|poente/, 'Noite': /noite|luar|estrelas|escuridao/,
  };
  const weatherTerms: Record<WeatherForecast['kind'], RegExp> = {
    clear: /tempo aberto|ceu limpo|céu limpo|sol|ensolar|claridade/, cloudy: /encoberto|nublado|nuvens/, rain: /chuva|garoa|gotas/, storm: /tempestade|trovao|relampago/, snow: /neve|flocos/, blizzard: /nevasca/, fog: /nevoa|neblina|bruma/, wind: /vento|ventania/,
  };
  const periodPresent = periodTerms[binding.period]?.test(normalized) ?? false;
  const weatherPresent = weatherTerms[binding.weatherKind].test(normalized);
  if (periodPresent && weatherPresent) return { ...response, weather: binding.weatherLabel };
  const cue = narrationEnvironmentCue(binding);
  const atmospheric = `${cue}\n\n${String(response.atmospheric_narration || '').trim()}`.trim();
  const voiceScript = [{ speakerType: 'narrator' as const, speakerName: 'Narrador', text: cue, emotion: 'contido' }, ...(response.voiceScript || [])];
  return { ...response, atmospheric_narration: atmospheric, weather: binding.weatherLabel, voiceScript };
}
