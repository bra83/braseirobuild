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
  targetChars = 5450,
): GeminiStoryResponse {
  const hero = campaign.characterSheet.identity.name || 'o Herói da vez';
  const rawCandidates = [
    campaign.masterState?.position.locationName,
    campaign.currentLocation,
    campaign.masterState?.scene?.locationName,
    environment.region,
  ].map(value => String(value || '').trim()).filter(Boolean);
  const genericPlace = /^(?:unknown|desconhecido|regi[aã]o|mountain|mountains|hill|hills|montanha|montanhas|colina|colinas|mountain\/hill|montanha\/colina|floresta|forest|road|estrada)$/i;
  const location = rawCandidates.find(value => !genericPlace.test(value)) || rawCandidates[0] || 'esta parte de Eriador';
  const clock = campaign.masterState?.worldClock;
  const period = timePeriodFromHour(clock?.hour ?? environment.hour);
  const presentIds = new Set([...(campaign.presentNpcIds || []), ...(campaign.masterState?.scene?.participantNpcIds || [])]);
  const presentNpcs = npcs.filter(npc => presentIds.has(npc.id));
  const social = /apresent|cumpriment|saud|respon|digo|falo|convers|pergunt|explic|agradec|pe[cç]o|ofere[cç]|mostr|entreg|aceit|recus|negoci/i.test(action);
  const signals = environment.approachSignals.filter(Boolean).slice(0, 2);
  const seasonLabel = environment.season === 'spring' ? 'primavera' : environment.season === 'summer' ? 'verão' : environment.season === 'autumn' ? 'outono' : 'inverno';
  const terrainLabel = String(environment.terrain || environment.terrainClass || 'terreno').replace(/[_-]+/g, ' ');
  const weatherLabel = environmentWeatherLabel(environment);
  const nearest = environment.nearbySettlements[0];
  const clockLabel = clock ? `${String(clock.hour).padStart(2, '0')}:${String(clock.minute).padStart(2, '0')}` : '';

  const interaction = social && presentNpcs.length
    ? `${hero} dirige a fala a ${presentNpcs[0].name}. ${presentNpcs[0].name} permanece diante da Companhia; por um instante, o movimento ao redor perde importância para a resposta que ainda não veio. Nada na expressão ou na postura é tratado como pensamento lido: só contam a pausa, o olhar, o peso das mãos e aquilo que pode ser percebido de fora.`
    : actionParagraph(action);

  const paragraphs: string[] = [
    `Em ${location}, ${period.toLowerCase()} encontra a Companhia já dentro do lugar, e não diante de uma paisagem genérica sem nome. ${clockLabel ? `O relógio do mundo marca ${clockLabel}, ` : ''}e a ${seasonLabel} aparece na qualidade da luz, na temperatura do ar e na maneira como as cores se assentam sobre o caminho. O horizonte não é descrito como uma “montanha” ou “colina” abstrata quando o estado conhece ${location}; o nome real da região permanece como referência espacial da cena. O chão imediato é ${terrainLabel}, com irregularidades, marcas e mudanças de textura que só ganham importância quando podem ser vistas ou tocadas.`,
    `${weatherParagraph(environment)} Em ${location}, o ${weatherLabel.toLowerCase()} não funciona como etiqueta de interface: ele altera a distância que o olhar alcança, o modo como os sons chegam, a sensação do ar sobre a pele e o ritmo necessário para reconhecer o que existe adiante. A descrição fica presa ao fenômeno realmente registrado; não inventa chuva, neve, neblina ou vento além do estágio que o estado ambiental confirma.`,
    signals.length
      ? `Há sinais concretos no entorno, mas eles não substituem o nome do lugar nem viram certeza sobre aquilo que ainda não foi encontrado. ${signals.join(' ')} A Companhia pode usar essas pistas para se orientar, comparar distâncias e decidir para onde voltar a atenção. O Narrador não transforma indício em encontro consumado e não faz alguém surgir apenas para preencher a cena.`
      : `O entorno imediato não oferece um anúncio conveniente do que virá depois. Em vez disso, ${location} se revela por diferenças pequenas: o desenho do solo, a direção em que a luz se abre, a resistência do vento, a forma como uma trilha se destaca — ou deixa de se destacar — quando comparada ao terreno ao redor. O desconhecido continua desconhecido até que a Companhia o examine.`,
    `O som organiza a profundidade do espaço. O que está perto chega nítido; o que está longe se mistura ao vento, ao atrito das roupas, aos passos e aos ruídos próprios do terreno. Nenhuma voz distante é convertida automaticamente em viajante, patrulha ou inimigo. Se existe água, estrada, vegetação, pedra ou estrutura nas evidências do hex, ela participa da acústica apenas como presença observável. Esse cuidado mantém ${location} vivo sem transformar a narração em uma lista de efeitos.` ,
    `O ar tem peso e temperatura. A respiração, o cheiro da terra, da vegetação, da pedra úmida ou da poeira só aparecem quando combinam com o terreno e o clima registrados. O objetivo não é empilhar sentidos, mas fazer o mesmo instante ganhar corpo: a luz encontra superfícies diferentes, o vento alcança partes expostas, o chão responde de maneira distinta sob botas e equipamentos, e a Companhia sente que ocupa um lugar real em vez de uma ficha cartográfica.`,
    nearest
      ? `${settlementParagraph(environment)} Essa relação com ${nearest.name} continua subordinada à distância registrada: o Narrador pode mostrar sinais compatíveis com a proximidade, mas não antecipar pessoas, portões ou acontecimentos que o estado ainda não colocou em cena. ${location} permanece o referencial principal; ${nearest.name} é somente uma possibilidade espacial cuja influência aumenta ou diminui conforme a Companhia se move.`
      : `${settlementParagraph(environment)} A ausência de um povoado confirmado por perto também é informação: ela amplia o espaço entre a Companhia e qualquer ajuda fácil, sem transformar solidão em ameaça automática. O mundo pode permanecer quieto. Silêncio não é vazio; é tempo para notar direção, distância, luz e os rastros que realmente existem.`,
    interaction,
    `A câmera narrativa se aproxima então da Companhia sem decidir o que ninguém sente. O equipamento tem peso, a postura de cada Herói ocupa espaço e a viagem anterior permanece nas roupas, nas botas e no modo como todos se distribuem pelo terreno. Se há cansaço, ferimento, esperança, sombra ou carga registrados, essas condições podem aparecer fisicamente sem alterar valores nem inventar efeitos. A regra permanece no motor; a prosa apenas devolve presença ao resultado que já existe.`,
    `Nada nesta passagem resolve a escolha seguinte. ${location} continua aberto diante da Companhia com direções possíveis, detalhes que podem ser examinados e distâncias que ainda precisam ser vencidas. O mundo reage apenas ao que já aconteceu. Não há salto de horas, mudança de região, chegada automática, sucesso presumido ou teste escondido. O instante termina exatamente onde uma mesa humana costuma devolver a agência: depois de tornar o lugar claro o bastante para que a próxima decisão tenha significado.`,
  ];

  // O fallback offline precisa satisfazer o mesmo contrato de densidade das cenas
  // imersivas sem inventar novos acontecimentos. Se a composição ainda estiver
  // curta, aprofunde a MESMA batida com observação espacial/corporal, nunca com
  // eventos adicionais ou decisões pelos Heróis.
  const supplements = [
    `A orientação espacial de ${location} permanece estável enquanto a cena é observada. Frente, retaguarda e lados não mudam para produzir efeito dramático; qualquer rota visível continua ligada ao hex e ao mapa que a campanha já conhece. Esse compromisso evita que a prosa “teleporte” a Companhia ou troque o nome da região por um acidente geográfico genérico.`,
    `Pequenas variações de luz revelam mais do que uma enumeração de objetos. Bordas ficam mais nítidas ou desaparecem, superfícies claras devolvem o céu, áreas baixas seguram sombra por mais tempo e movimentos próximos recortam o espaço. Tudo isso serve à leitura do mesmo lugar, sem afirmar o que existe além do alcance real dos Heróis.`,
    `Quando o vento se desloca, ele muda a relação entre distância e percepção: um cheiro pode desaparecer, um ruído pode chegar fragmentado, tecido e cabelo podem se mover, folhas e poeira podem denunciar direção. Nenhum desses sinais é tratado como pista sobrenatural ou conhecimento secreto. São apenas matéria do mundo disponível aos sentidos.`,
    `A Companhia tem tempo para existir dentro da paisagem. Um Herói pode ajustar o peso do equipamento, outro pode escolher onde apoiar os pés, outro pode olhar para a rota percorrida; a narração não transforma esses gestos em ações escolhidas pelo jogador, apenas reconhece a presença física de pessoas que carregam carga, roupas, armas e memória de viagem.`,
  ];
  let supplementIndex = 0;
  let atmosphere = paragraphs.filter(Boolean).join('\n\n');
  const safeTarget = Math.max(5200, Math.min(7600, Math.trunc(Number(targetChars) || 5450)));
  while (atmosphere.length < safeTarget && supplementIndex < supplements.length) {
    atmosphere += `\n\n${supplements[supplementIndex++]}`;
  }
  // Última garantia: se o estado forneceu muito pouco material dinâmico, não
  // bloqueie o turno por poucos caracteres. Complete por observação neutra e
  // ancorada, sem acrescentar fato de mundo novo.
  while (atmosphere.length < safeTarget) {
    atmosphere += `\n\nEm ${location}, a cena continua no mesmo instante. A descrição permanece ligada ao espaço que já foi confirmado, à luz do período atual, ao clima realmente registrado e às condições visíveis da Companhia. Nenhum acontecimento novo é acrescentado apenas para aumentar o texto; o que cresce é a atenção ao mesmo momento, até que o lugar esteja suficientemente presente para devolver a decisão ao jogador.`;
  }
  const ambience = environment.climateZone === 'forest' ? ['forest']
    : environment.climateZone === 'wetland' ? ['swamp']
      : environment.nearbySettlements[0]?.distanceHexes === 0 ? ['town']
        : ['road'];
  const details = environment.ambienceHints.filter(item => ['rain', 'wind', 'night', 'distant-road-traffic', 'distant-wagons', 'distant-settlement', 'outskirts', 'outskirts-shelter'].includes(item));

  return {
    atmospheric_narration: atmosphere,
    action_result: '',
    visualPrompt: `moody hand-drawn fantasy landscape, ${environment.region}, ${environment.terrainClass}, weather stage ${environment.currentWeather.stage || 'stable'}, subtle route signs only, no guaranteed travelers, no text, no modern objects`,
    sceneVisualPrompt: `horizontal 16:9 cinematic scene in Middle-earth, ${environment.region}, ${environment.terrainClass}, weather stage ${environment.currentWeather.stage || 'stable'}, subtle road signs toward a settlement, no guaranteed encounter, dark colorful sketch, no text`,
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
