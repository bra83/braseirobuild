import {
  ERIADOR_MAP_ID,
  loadMapDiscovery,
  loadRuntimeMapManifest,
  loadRuntimeTerraMediaLocations,
  resolveHexNarrativeLocation,
  type EriadorMapManifest,
  type MapDiscovery,
  type TerraMediaLocation,
} from './map';

export type CampaignStartMode = 'STAR_OF_MIST' | 'TALES_LONE_LANDS' | 'STRIDER' | 'SANDBOX' | 'TAVERN';
export type StriderPatronId = 'balin' | 'bilbo' | 'cirdan' | 'gandalf' | 'gilraen' | 'tom_bombadil';
export type StriderMissionChoice = 'random' | 1 | 2 | 3 | 4 | 5 | 6;

export interface CampaignStartSelection {
  mode: CampaignStartMode | null;
  striderPatronId?: StriderPatronId;
  striderMissionChoice?: StriderMissionChoice;
}

export interface CampaignStartDefinition {
  mode: CampaignStartMode;
  sourceBook?: string;
  sourcePages?: string;
  adventureId?: string;
  canonicalRegionId?: string;
  canonicalLocationId?: string;
  mapId: string;
  hexId: string;
  patronId?: StriderPatronId;
  questSeedId?: string;
  questRoll?: number;
  playerMustChooseLocation?: boolean;
  randomLocationPolicy?: 'TAVERN';
  openingSituation: string;
  canonicalLocationName: string;
  scenePlaceName: string;
  resolvedAt: number;
}

export interface ResolvedCampaignStart {
  definition: CampaignStartDefinition;
  discovery: MapDiscovery;
}

export interface CampaignStartOption {
  id: CampaignStartMode;
  label: string;
  description: string;
  sourceLabel: string;
}

export const CAMPAIGN_START_OPTIONS: CampaignStartOption[] = [
  {
    id: 'STAR_OF_MIST',
    label: 'A Estrela da Névoa',
    description: 'Aventura de Marco do Livro Básico. A Companhia começa na aproximação oficial pelo Vau Sarn.',
    sourceLabel: 'Livro Básico 2e · A Estrela da Névoa',
  },
  {
    id: 'TALES_LONE_LANDS',
    label: 'Contos das Terras Solitárias',
    description: 'Começa pela primeira aventura da sequência: Toca de Troll, Se é Que Houve Uma…, sob a placa do Pônei Saltitante.',
    sourceLabel: 'Contos das Terras Solitárias · cap. 1',
  },
  {
    id: 'STRIDER',
    label: 'Modo Peregrino',
    description: 'Escolha um Patrono e uma semente de missão oficial, ou deixe o sistema lançar o d6 por você.',
    sourceLabel: 'Modo Peregrino · pp. 5–7',
  },
  {
    id: 'SANDBOX',
    label: 'Sandbox',
    description: 'Campanha livre. Você escolhe manualmente no Atlas onde a Companhia começa.',
    sourceLabel: 'Início livre do Braseiro',
  },
  {
    id: 'TAVERN',
    label: 'Modo Taverna',
    description: 'O host sorteia uma estalagem estruturada que possui âncora real no Atlas e posiciona a Companhia automaticamente.',
    sourceLabel: 'Catálogo estruturado do Atlas',
  },
];

export const STRIDER_PATRONS: Array<{ id: StriderPatronId; label: string; meetingLabel: string; anchorLocationId: string }> = [
  { id: 'balin', label: 'Balin, Filho de Fundin', meetingLabel: 'Erebor', anchorLocationId: 'erebor' },
  { id: 'bilbo', label: 'Bilbo Bolseiro', meetingLabel: 'Vila dos Hobbits', anchorLocationId: 'hobbiton' },
  { id: 'cirdan', label: 'Círdan, o Marinheiro', meetingLabel: 'Mithlond', anchorLocationId: 'mithlond' },
  { id: 'gandalf', label: 'Gandalf, o Cinzento', meetingLabel: 'uma pousada em Bri', anchorLocationId: 'bree' },
  { id: 'gilraen', label: 'Gilraen, a Bela', meetingLabel: 'Valfenda', anchorLocationId: 'rivendell' },
  { id: 'tom_bombadil', label: "Tom Bombadil e Senhora Fruta D'Ouro", meetingLabel: 'Floresta Velha, perto da casa de Tom', anchorLocationId: 'old_forest' },
];

export const STRIDER_MISSIONS: Record<StriderPatronId, string[]> = {
  balin: [
    'Investigue sussurros sombrios que perturbam terras anãs e descubra o que se move nos ermos.',
    'Descubra a origem de um artefato quebrado e encontre quem possa restaurá-lo contra o Inimigo.',
    'Explore ruínas anãs em busca de segredos antigos e recursos úteis contra a Sombra.',
    'Encontre um camarada caído e recupere algo vital que carregava.',
    'Descubra quem tomou uma fortaleza anã e como o lugar foi profanado.',
    'Investigue um tenente do Inimigo que está reunindo seguidores e ampliando seu poder.',
  ],
  bilbo: [
    'Encontre uma iguaria rara de grande valor para Bilbo e descubra onde ela pode ser obtida.',
    'Explore uma área e registre marcos úteis para atualizar mapas e perceber como a terra mudou.',
    'Leve uma mensagem importante por estradas perigosas e descubra a quem ela deve chegar.',
    'Investigue um texto antigo indecifrável e encontre alguém capaz de traduzi-lo.',
    'Recupere um mapa importante que caiu nas mãos do Inimigo e descubra os segredos que contém.',
    'Localize um artefato ressurgido antes que os servos do Inimigo o alcancem.',
  ],
  cirdan: [
    'Entregue um recurso importante para a construção de navios enquanto a ameaça da Sombra cresce.',
    'Investigue o retorno de um antigo inimigo dos Elfos e o rancor que o move.',
    'Viaje até um farol remoto, reacenda sua chama e enfrente o que tenta impedi-lo.',
    'Descubra o destino de mensageiros e cartas que caíram nas mãos do Inimigo.',
    'Recupere um artefato élfico carregado por um servo do Inimigo e descubra por que ele o cobiça.',
    'Investigue a doença que está atingindo árvores sagradas usadas nos navios dos Portos Cinzentos.',
  ],
  gandalf: [
    'Leve esperança e boas notícias a um assentamento remoto em tempos sombrios.',
    'Leve informações sobre os movimentos do Inimigo a um aliado de confiança.',
    'Procure um possível aliado e descubra o que é necessário para conquistar sua confiança.',
    'Capture um servo do Inimigo para descobrir o que ele sabe.',
    'Investigue rumores sobre uma arma terrível que o Inimigo pretende usar.',
    'Encontre e resgate um aliado capturado pela Sombra.',
  ],
  gilraen: [
    'Descubra o que aconteceu com um Patrulheiro desaparecido.',
    'Investigue um poderoso servo do Inimigo que está caçando Patrulheiros.',
    'Descubra quem comanda patrulhas inimigas que atacam comerciantes e suprimentos nas estradas.',
    'Investigue por que um assentamento específico passou a atrair a atenção da Sombra.',
    'Descubra que ameaça e que armas são trazidas por servos monstruosos do Inimigo.',
    'Vá até um refúgio remoto dos Patrulheiros que deixou de enviar notícias e descubra o que ocorreu.',
  ],
  tom_bombadil: [
    'Investigue uma aflição sombria entre criaturas do ermo e procure um ingrediente raro para a cura.',
    'Descubra o que contamina uma fonte sagrada e enfrente a criatura que a guarda.',
    'Escolte viajantes através do ermo e descubra a verdadeira importância de sua missão.',
    'Investigue a corrupção de um local sagrado e a presença sombria que tomou o lugar.',
    'Encontre uma flor rara que voltou a desabrochar e descubra por que ela é importante.',
    'Investigue a presença hostil que se instalou em uma árvore antiga e descubra se pode ser expulsa.',
  ],
};

const TAVERN_STARTS: Array<{ id: string; label: string; anchorLocationId: string; source: string }> = [
  { id: 'prancing-pony', label: 'O Pônei Saltitante', anchorLocationId: 'bree', source: 'Bri' },
  { id: 'ivy-bush', label: 'A Moita de Hera', anchorLocationId: 'hobbiton', source: 'Vila dos Hobbits' },
];

function randomInt(maxExclusive: number): number {
  if (maxExclusive <= 1) return 0;
  const cryptoApi = globalThis.crypto;
  if (cryptoApi?.getRandomValues) {
    const max = 0x100000000;
    const limit = max - (max % maxExclusive);
    const buf = new Uint32Array(1);
    do cryptoApi.getRandomValues(buf); while (buf[0] >= limit);
    return buf[0] % maxExclusive;
  }
  return Math.floor(Math.random() * maxExclusive);
}

function appendUnique(values: string[], value: string): string[] {
  return values.includes(value) ? values : [...values, value];
}

function locationById(locations: TerraMediaLocation[], id: string): TerraMediaLocation {
  const found = locations.find(location => location.id === id);
  if (!found?.hexId) throw new Error(`O Atlas não possui uma âncora utilizável para ${id}.`);
  return found;
}

function updatedDiscovery(discovery: MapDiscovery, hexId: string): MapDiscovery {
  return {
    ...discovery,
    mapId: ERIADOR_MAP_ID,
    currentHexId: hexId,
    discoveredHexIds: appendUnique(discovery.discoveredHexIds || [], hexId),
    visitedHexIds: appendUnique(discovery.visitedHexIds || [], hexId),
    routeHexIds: appendUnique(discovery.routeHexIds || [], hexId),
    plannedRouteHexIds: [],
    updatedAt: Date.now(),
  };
}

function canonicalName(manifest: EriadorMapManifest, locations: TerraMediaLocation[], location: TerraMediaLocation): string {
  const resolved = resolveHexNarrativeLocation(manifest, locations, location.hexId!);
  return resolved.source === 'unresolved' ? location.name : resolved.name;
}

function definitionForAnchor(
  mode: CampaignStartMode,
  manifest: EriadorMapManifest,
  locations: TerraMediaLocation[],
  anchorLocationId: string,
  extras: Omit<CampaignStartDefinition, 'mode' | 'mapId' | 'hexId' | 'canonicalLocationId' | 'canonicalLocationName' | 'resolvedAt'>,
): CampaignStartDefinition {
  const anchor = locationById(locations, anchorLocationId);
  return {
    mode,
    mapId: ERIADOR_MAP_ID,
    hexId: anchor.hexId!,
    canonicalLocationId: anchor.id,
    canonicalLocationName: canonicalName(manifest, locations, anchor),
    resolvedAt: Date.now(),
    ...extras,
  };
}

export function isCampaignStartReady(selection: CampaignStartSelection, hasAtlasPosition: boolean): boolean {
  if (!selection.mode) return false;
  if (selection.mode === 'SANDBOX') return hasAtlasPosition;
  if (selection.mode === 'STRIDER') return Boolean(selection.striderPatronId);
  return true;
}

export async function resolveCampaignStart(
  campaignId: string,
  selection: CampaignStartSelection,
): Promise<ResolvedCampaignStart> {
  if (!selection.mode) throw new Error('Escolha como a campanha vai começar.');
  const [manifest, locations, discovery] = await Promise.all([
    loadRuntimeMapManifest(),
    loadRuntimeTerraMediaLocations(),
    loadMapDiscovery(campaignId),
  ]);

  let definition: CampaignStartDefinition;

  if (selection.mode === 'SANDBOX') {
    if (!discovery.currentHexId) throw new Error('No modo Sandbox, escolha no Atlas o hex onde a Companhia começa.');
    const resolved = resolveHexNarrativeLocation(manifest, locations, discovery.currentHexId);
    definition = {
      mode: 'SANDBOX',
      sourceBook: 'Braseiro O Um Anel 2e',
      sourcePages: 'Início livre',
      mapId: ERIADOR_MAP_ID,
      hexId: discovery.currentHexId,
      canonicalLocationName: resolved.name,
      scenePlaceName: resolved.name,
      playerMustChooseLocation: true,
      openingSituation: 'A campanha é livre. A Companhia começa exatamente no local escolhido pelo jogador no Atlas; não substitua esse ponto por outro.',
      resolvedAt: Date.now(),
    };
    return { definition, discovery: updatedDiscovery(discovery, definition.hexId) };
  }

  if (selection.mode === 'STAR_OF_MIST') {
    definition = definitionForAnchor('STAR_OF_MIST', manifest, locations, 'sarn_ford', {
      sourceBook: 'O Um Anel 2e — Livro Básico',
      sourcePages: 'A Estrela da Névoa · pp. 223–230',
      adventureId: 'star-of-the-mist',
      canonicalRegionId: 'eriador',
      scenePlaceName: 'Vau Sarn',
      openingSituation: 'A Companhia chega ao Vau Sarn ao pôr do sol e percebe uma luz distante a oeste que conduz à antiga torre no sul de Ered Luin. Inicie a aventura a partir dessa aproximação, sem antecipar segredos ainda não descobertos.',
    });
  } else if (selection.mode === 'TALES_LONE_LANDS') {
    definition = definitionForAnchor('TALES_LONE_LANDS', manifest, locations, 'bree', {
      sourceBook: 'Contos das Terras Solitárias',
      sourcePages: 'Cap. 1 · Toca de Troll, Se é Que Houve Uma… · Parte 1',
      adventureId: 'troll-hole-if-ever-there-was-one',
      canonicalRegionId: 'bree-land',
      scenePlaceName: 'O Pônei Saltitante, em Bri',
      openingSituation: 'Comece pela abertura da primeira aventura, no salão comum do Pônei Saltitante. Jari e Diarmoc devem entrar conforme a sequência da aventura; não revele antecipadamente os segredos do vale e dos Trolls.',
    });
  } else if (selection.mode === 'STRIDER') {
    const patronId = selection.striderPatronId;
    if (!patronId) throw new Error('Escolha um Patrono para o Modo Peregrino.');
    const patron = STRIDER_PATRONS.find(item => item.id === patronId)!;
    const choice = selection.striderMissionChoice ?? 'random';
    const roll = choice === 'random' ? randomInt(6) + 1 : choice;
    const mission = STRIDER_MISSIONS[patronId][roll - 1];
    definition = definitionForAnchor('STRIDER', manifest, locations, patron.anchorLocationId, {
      sourceBook: 'O Um Anel 2e — Modo Peregrino',
      sourcePages: 'pp. 5–7 · Missões do Patrono',
      adventureId: 'strider-patron-mission',
      patronId,
      questSeedId: `${patronId}-${roll}`,
      questRoll: roll,
      scenePlaceName: patron.meetingLabel,
      openingSituation: `${patron.label} apresenta a missão sorteada/selecionada pelo host (resultado ${roll} no d6): ${mission} A primeira cena deve apresentar a incumbência e deixar as perguntas em aberto para serem descobertas em jogo.`,
    });
  } else {
    const tavern = TAVERN_STARTS[randomInt(TAVERN_STARTS.length)];
    definition = definitionForAnchor('TAVERN', manifest, locations, tavern.anchorLocationId, {
      sourceBook: 'Catálogo estruturado do Braseiro / O Um Anel 2e',
      sourcePages: 'Locais com âncora cartográfica validada',
      adventureId: 'random-tavern-sandbox',
      randomLocationPolicy: 'TAVERN',
      scenePlaceName: `${tavern.label}, ${tavern.source}`,
      openingSituation: `A Companhia começa em ${tavern.label}, em ${tavern.source}. O host já sorteou esta estalagem; não troque por outra nem invente um local fora do Atlas. Abra uma situação jogável e livre a partir do que está acontecendo nesta taverna.`,
    });
  }

  return { definition, discovery: updatedDiscovery(discovery, definition.hexId) };
}

export function campaignStartPrompt(definition: CampaignStartDefinition): string {
  const mission = definition.patronId && definition.questRoll
    ? ` Patrono: ${STRIDER_PATRONS.find(item => item.id === definition.patronId)?.label || definition.patronId}. Semente de missão d6: ${definition.questRoll}.`
    : '';
  return `[INÍCIO DE CAMPANHA RESOLVIDO PELO HOST] Modo: ${definition.mode}. Fonte: ${definition.sourceBook || 'Braseiro'}. Posição canônica do Atlas: hex ${definition.hexId}, ${definition.canonicalLocationName}. Local específico da cena: ${definition.scenePlaceName}.${mission} Situação inicial: ${definition.openingSituation} REGRA DURA: esta posição, esta aventura e esta semente são fatos do estado; não escolha outro início.`;
}
