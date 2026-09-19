import type { CampaignState, ManualChunk, Npc } from '../db';
import type { GeminiStoryResponse, StoryChoice } from '../../types';
import type { DirectorPlan } from '../master/types';
import type { EnvironmentContext } from './types';
import { environmentWeatherLabel } from './environment';
import { timePeriodFromHour } from '../sceneContinuity';

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

export function isRecoverableGeminiFailure(error: unknown): boolean {
  const text = error instanceof Error ? `${error.name} ${error.message}` : String(error || '');
  return /429|quota|cota|resource_exhausted|network|internet|offline|failed to fetch|timeout|timed out|503|502|504|unavailable|api key|chave.*nao|chave.*não|401|403/i.test(text);
}

function deterministicSkill(action: string): string | null {
  const text = normalize(action);
  if (/esgueir|furtiv|silencio|silêncio|ocultar|se esconder/.test(text)) return 'Furtividade';
  if (/observar|vigiar|escutar|perceber|examinar ao longe/.test(text)) return 'Vigilância';
  if (/procurar|investigar|rastrear|examinar|explorar/.test(text)) return 'Exploração';
  if (/viajar|seguir viagem|marchar|orientar|rota/.test(text)) return 'Viagem';
  if (/cacar|caçar|buscar alimento|rastrear animal/.test(text)) return 'Caçada';
  if (/convencer|persuadir|negociar|barganhar/.test(text)) return 'Persuasão';
  if (/inspirar|encorajar|impressionar|intimidar/.test(text)) return 'Fascínio';
  if (/liderar.*(batalha|combate)|comandar.*(batalha|combate)|tática|tatica/.test(text)) return 'Batalha';
  return null;
}

export function createLocalDirectorPlan(campaign: CampaignState, action: string, manualExcerpts: ManualChunk[] = []): DirectorPlan {
  const master = campaign.masterState;
  const skill = deterministicSkill(action);
  const normalizedSkill = normalize(skill || '');
  const groundedIndex = skill ? manualExcerpts.findIndex(item => { const text = normalize(item.text); return text.includes(normalizedSkill) || (text.includes('teste') && (text.includes('numero alvo') || text.includes('dado de proeza') || text.includes('dados de sucesso'))); }) : -1;
  const grounded = groundedIndex >= 0;
  return {
    intent: action,
    actingCharacterId: campaign.activeCharacterId || undefined,
    scope: 'hero',
    mode: master?.activeMode || 'adventure',
    sceneKind: master?.scene.kind || 'exploration',
    spotlightCharacterId: campaign.activeCharacterId || undefined,
    requiredTest: skill && grounded ? {
      id: `local_test_${Date.now()}`,
      skill,
      reason: `A ação envolve ${skill}; o teste foi sustentado pela regra local recuperada antes da resolução.`,
      characterId: campaign.activeCharacterId || undefined,
    } : null,
    commands: [],
    npcDecisions: [],
    revelationsAllowed: [],
    visibleFacts: [],
    hiddenNotes: ['Fallback local ativo. Mensagens operacionais nunca devem aparecer na prosa mostrada ao jogador.'],
    narrationBrief: 'Continue com fatos ambientais e estado já persistido, em prosa natural de O Um Anel. Não revele segredo nem mencione falha de rede, Gemini, fallback, índice ou banco de dados.',
    ruleSourceNumbers: grounded ? [groundedIndex + 1] : [],
    ruleSummary: grounded ? `Regra local consultada em ${manualExcerpts[groundedIndex].manualName}, página ${manualExcerpts[groundedIndex].pageNum}.` : 'Nenhuma consequência mecânica foi autorizada.',
  };
}

function weatherParagraph(environment: EnvironmentContext): string {
  const weather = environment.currentWeather;
  const stage = weather.stage || 'stable';
  if (stage === 'indications') {
    if (weather.kind === 'blizzard' || weather.kind === 'snow') return 'O vento muda e a temperatura cai. Grãos de neve antiga correm rente ao chão, mas ainda não há nevasca ativa nem precipitação suficiente para apagar a rota.';
    if (weather.kind === 'rain' || weather.kind === 'storm') return 'O vento vira e o ar ganha umidade. Nuvens mais pesadas se juntam ao longe, ainda como indício — não como chuva ou tempestade já instalada.';
    if (weather.kind === 'fog') return 'A umidade cresce junto ao solo e os contornos mais distantes perdem nitidez, embora a névoa ainda não tenha fechado o caminho.';
    return 'Uma mudança discreta no vento anuncia que o tempo pode se alterar mais adiante, sem fenômeno ativo por enquanto.';
  }
  if (stage === 'forming') {
    if (weather.kind === 'blizzard') return 'A neve começa a correr rente ao chão e a visibilidade diminui aos poucos. A nevasca está em formação; ela só poderá se tornar ativa depois de outro deslocamento confirmado.';
    if (weather.kind === 'snow') return 'Os primeiros flocos atravessam o ar frio, ainda esparsos. A neve está em formação e não domina o caminho.';
    if (weather.kind === 'rain' || weather.kind === 'storm') return 'Gotas espaçadas começam a marcar o chão enquanto o vento reúne as nuvens. O fenômeno está em formação, não plenamente ativo.';
    if (weather.kind === 'fog') return 'Faixas de névoa começam a ocupar as baixadas, fechando a paisagem de maneira gradual.';
    return `O ${environmentWeatherLabel(environment)} começa a tomar forma sem mudança brusca.`;
  }
  if (stage === 'dissipating') {
    return `O ${environmentWeatherLabel(environment)} perde força. A visibilidade e os sons do caminho retornam gradualmente ao normal.`;
  }
  if (stage === 'active') {
    if (weather.kind === 'blizzard') return 'A nevasca está ativa: neve e vento reduzem a visibilidade, cobrem marcas menores e tornam cada referência do caminho mais valiosa.';
    if (weather.kind === 'snow') return 'A neve cai de forma contínua, alterando as cores do terreno e cobrindo aos poucos as trilhas menos usadas.';
    if (weather.kind === 'rain' || weather.kind === 'storm') return `A ${weather.kind === 'storm' ? 'tempestade' : 'chuva'} está ativa. Água e vento abafam sons distantes e transformam o aspecto da estrada.`;
    if (weather.kind === 'fog') return 'A névoa está ativa e reduz o mundo aos contornos mais próximos, fazendo sons parecerem maiores e mais incertos.';
    if (weather.kind === 'wind') return 'O vento está ativo, varrendo o terreno e levando consigo cheiros, vozes e folhas soltas.';
  }
  return `O ${environmentWeatherLabel(environment)} se mantém sobre a região. Vento, luz e visibilidade seguem como já estavam estabelecidos.`;
}

function settlementParagraph(environment: EnvironmentContext): string {
  const nearest = environment.nearbySettlements[0];
  if (!nearest) return 'Nenhum sinal claro de povoado próximo rompe a solidão. O que existe adiante precisa ser descoberto pelo caminho, não presumido.';
  if (nearest.movementTrend === 'leaving') return `Os sinais de ${nearest.name} ficam para trás e o tráfego ligado à localidade se torna menos provável a cada etapa.`;
  if (nearest.distanceHexes === 0) return `${nearest.name} já determina o ritmo do lugar: portões, guardas, vozes, tarefas e deslocamentos cotidianos pertencem à cena imediata.`;
  if (nearest.distanceHexes === 1) return `Os arredores de ${nearest.name} já podem deixar sinais discretos — cercas, caminhos secundários ou marcas de passagem — sem garantir que alguém esteja por perto.`;
  if (nearest.distanceHexes === 2) return `${nearest.name} ainda está adiante; conforme o vento e o relevo, sinais remotos de ocupação podem surgir sem confirmar presença humana.`;
  if (nearest.distanceHexes === 3) return `A influência de ${nearest.name} é distante, perceptível apenas por rotas convergentes e marcas ocasionais de viagem.`;
  return `${nearest.name} permanece distante e não altera, por si só, o que a Companhia pode confirmar neste ponto do caminho.`;
}

function actionParagraph(action: string): string {
  const text = normalize(action.replace(/^\[[^\]]+\]\s*/g, ''));
  if (/perguntar|questionar|conversar|falar|dialogar|barliman|estalajadeiro/.test(text)) return 'As palavras ficam entre a Companhia e o interlocutor. O que importa agora está na resposta — ou na maneira como ela demora a chegar.';
  if (/examinar|observar|procurar|investigar|rastrear/.test(text)) return 'A atenção se estreita sobre marcas, distâncias e pequenas diferenças no terreno; nada além do que pode ser visto ou comparado é tomado como certeza.';
  if (/viajar|seguir|marchar|caminho|rota/.test(text)) return 'A estrada continua adiante sob o mesmo céu, e os sinais de passagem ganham importância à medida que a Companhia escolhe por onde seguir.';
  return 'A Companhia permanece diante do mesmo lugar, com as marcas, vozes e caminhos já presentes ainda ao alcance dos sentidos.';
}

function choicesFor(environment: EnvironmentContext, action = ''): StoryChoice[] {
  const text = normalize(action);
  if (/perguntar|questionar|conversar|falar|dialogar/.test(text)) return [
    { id: `local_choice_listen_${Date.now()}`, text: 'Observar a reação do interlocutor e pedir que esclareça apenas o ponto que parece mais incerto.', requiresTest: false },
    { id: `local_choice_compare_${Date.now()}`, text: 'Comparar o que foi dito com as pistas e lembranças já registradas pela Companhia.', requiresTest: false },
    { id: `local_choice_leave_${Date.now()}`, text: 'Encerrar a conversa por enquanto e verificar pessoalmente o local relacionado ao relato.', requiresTest: false },
  ];
  if (/examinar|observar|procurar|investigar|rastrear/.test(text)) return [
    { id: `local_choice_detail_${Date.now()}`, text: 'Concentrar a busca no sinal mais recente e distinguir o que é marca natural do que foi deixado por alguém.', requiresTest: false },
    { id: `local_choice_route_${Date.now()}`, text: 'Relacionar os vestígios ao caminho e às rotas possíveis antes de escolher uma direção.', requiresTest: false },
    { id: `local_choice_party_${Date.now()}`, text: 'Ouvir o que outro membro da Companhia percebeu antes de tirar uma conclusão.', requiresTest: false },
  ];
  const nearest = environment.nearbySettlements[0];
  return [
    { id: `local_choice_observe_${Date.now()}`, text: 'Conferir o terreno imediato e escolher um ponto seguro de onde observar o caminho.', requiresTest: false },
    { id: `local_choice_continue_${Date.now()}`, text: 'Seguir pela rota confirmada, mantendo atenção aos sinais que realmente mudarem à frente.', requiresTest: false },
    nearest && nearest.distanceHexes <= 2
      ? { id: `local_choice_signs_${Date.now()}`, text: `Procurar sinais concretos dos arredores de ${nearest.name} antes de decidir se a Companhia se aproxima.`, requiresTest: false }
      : { id: `local_choice_route_${Date.now()}`, text: 'Rever a rota, o abrigo disponível e o tempo restante antes de avançar.', requiresTest: false },
  ];
}

export function createLocalNarration(
  campaign: CampaignState,
  action: string,
  environment: EnvironmentContext,
  manualExcerpts: ManualChunk[] = [],
  npcs: Npc[] = [],
): GeminiStoryResponse {
  const hero = campaign.characterSheet.identity.name || 'o Herói da vez';
  const location = campaign.masterState?.position.locationName || campaign.currentLocation || environment.region;
  const clock = campaign.masterState?.worldClock;
  const period = timePeriodFromHour(clock?.hour ?? environment.hour);
  const presentIds = new Set([...(campaign.presentNpcIds || []), ...(campaign.masterState?.scene?.participantNpcIds || [])]);
  const presentNpcs = npcs.filter(npc => presentIds.has(npc.id));
  const social = /apresent|cumpriment|saud|respon|digo|falo|convers|pergunt|explic|agradec|pe[cç]o|ofere[cç]|mostr|entreg|aceit|recus|negoci/i.test(action);
  const signals = environment.approachSignals[0] || '';

  const openingParts = [`${period} em ${location}.`];
  if (signals) openingParts.push(signals.replace(/\s+/g, ' ').trim());
  const opening = openingParts.join(' ');

  const interaction = social && presentNpcs.length
    ? `${hero} dirige a fala a ${presentNpcs[0].name}. ${presentNpcs[0].name} permanece diante da Companhia; por um instante, o resto do lugar perde importância para a resposta que ainda não veio.`
    : actionParagraph(action);

  const paragraphs = [opening, weatherParagraph(environment), interaction];
  if (!social || !presentNpcs.length) {
    const settlement = settlementParagraph(environment);
    if (settlement) paragraphs.push(settlement);
  }
  const atmosphere = paragraphs.filter(Boolean).slice(0, 4).join('\n\n');

  const ambience = environment.climateZone === 'forest' ? ['forest']
    : environment.climateZone === 'wetland' ? ['swamp']
      : environment.nearbySettlements[0]?.distanceHexes === 0 ? ['town']
        : ['road'];
  const details = environment.ambienceHints.filter(item => ['rain', 'wind', 'night', 'distant-road-traffic', 'distant-wagons', 'distant-settlement', 'outskirts', 'outskirts-shelter'].includes(item));

  return {
    atmospheric_narration: atmosphere,
    action_result: '',
    visualPrompt: `moody hand-drawn fantasy landscape, ${environment.region}, ${environment.terrainClass}, weather stage ${environment.currentWeather.stage || 'stable'}, subtle route signs only, no guaranteed travelers, no text, no modern objects`,
    sceneVisualPrompt: `vertical cinematic scene in Middle-earth, ${environment.region}, ${environment.terrainClass}, weather stage ${environment.currentWeather.stage || 'stable'}, subtle road signs toward a settlement, no guaranteed encounter, dark colorful sketch, no text`,
    location,
    weather: environmentWeatherLabel(environment),
    choices: choicesFor(environment, action),
    requiredTest: null,
    audioPlan: {
      environment: [environment.climateZone, environment.region],
      ambience,
      ambienceDetails: details,
      musicMood: ['calm'],
      sfx: [],
      stopPreviousAmbience: false,
    },
    events: [{
      id: `event_local_fallback_${Date.now()}`,
      type: 'custom',
      timestamp: Date.now(),
      data: { localFallback: true, weather: environment.currentWeather.kind, hexId: environment.currentHexId || '' },
    }],
    hpChange: 0,
    sanityChange: 0,
    suppliesChange: 0,
    inventoryChanges: [],
    narrativeLog: `Fallback narrativo local em ${location}; nenhuma consequência mecânica não sustentada foi aplicada.`,
    imageRecommended: false,
    usedSourceNumbers: manualExcerpts.length ? [1] : [],
    sourceUsageSummary: manualExcerpts.length ? 'A primeira fonte recuperada foi consultada para impedir resolução mecânica sem sustentação.' : '',
    voiceScript: [{ speakerType: 'narrator', speakerName: 'Narrador', text: atmosphere, emotion: 'contido' }],
  };
}
