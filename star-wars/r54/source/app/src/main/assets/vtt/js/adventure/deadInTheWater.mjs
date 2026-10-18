import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';

const ID = 'dead-in-the-water';
const SOURCE = Object.freeze({
  title: 'Dead in the Water',
  originalTitle: 'Muerte en las profundidades',
  system: 'Star Wars: Age of Rebellion',
  productCode: 'SWA03',
  sourceBook: "Age of Rebellion Game Master's Kit",
  official: true,
  userMaterial: false
});

const LOC = Object.freeze({
  docks: 'ditw.loc.morjanssik_surface',
  traffic: 'ditw.loc.traffic_control',
  management: 'ditw.loc.management_offices',
  rustyHarpoon: 'ditw.loc.rusty_harpoon',
  catwalks: 'ditw.loc.catwalks',
  bathysphere: 'ditw.loc.bathysphere_dock',
  oceanTrench: 'ditw.loc.ocean_trench',
  stagingArea: 'ditw.loc.mine_staging_area',
  mineLevel42: 'ditw.loc.mine_level_42'
});

const NPC = Object.freeze({
  kalbrac: 'ditw.npc.kalbrac',
  chonk: 'ditw.npc.moren_chonk',
  merv: 'ditw.npc.walif_merv',
  wolg: 'ditw.npc.kelmut_wolg',
  generet: 'ditw.npc.capitan_generet',
  huv: 'ditw.npc.teniente_huv',
  limeren: 'ditw.npc.agente_limeren',
  q4: 'ditw.npc.q4_droid'
});

const FACTION = Object.freeze({
  alliance: 'ditw.fac.rebel_alliance',
  miners: 'ditw.fac.quarren_miners',
  empire: 'ditw.fac.galactic_empire'
});

function clone(v) { return structuredClone(v); }
function now() { return new Date().toISOString(); }
function clamp(val, min, max) { return Math.max(min, Math.min(max, Number(val) || 0)); }

function loc(id, name, description, tags = []) {
  return { id, name, description, knownToPlayer: true, tags: ['published-adventure', ID, ...tags] };
}

function npc(id, name, locationId, factionId, profile = {}) {
  return {
    id, name, locationId, factionId,
    goal: profile.goal || '',
    relation: profile.relation || 'neutral',
    status: 'active',
    knownToPlayer: Boolean(profile.knownToPlayer),
    facts: [],
    tags: ['published-adventure', ID, ...(profile.tags || [])],
    combatState: null,
    updatedAt: now(),
    species: profile.species || '',
    sex: profile.sex || '',
    gender: profile.gender || '',
    origin: profile.origin || 'Mon Cala',
    career: profile.career || '',
    specialization: profile.specialization || '',
    occupation: profile.occupation || '',
    ageYears: profile.ageYears ?? null,
    appearance: profile.appearance || ''
  };
}

function faction(id, name, objective, knownToPlayer = false) {
  return {
    id, name, objective, status: 'active',
    resources: [], relations: {}, facts: [],
    knownToPlayer, tags: ['published-adventure', ID],
    plans: [], updatedAt: now()
  };
}

export const DEAD_IN_THE_WATER = Object.freeze({
  id: ID,
  title: 'Morte nas Profundezas',
  originalTitle: 'Dead in the Water (Muerte en las profundidades)',
  rulesProfile: 'AOR_CORE',
  campaignModes: Object.freeze(['AOR', 'MIXED']),
  source: SOURCE,
  startNodeId: 'arrival-surface',
  schemaVersion: 1,
  initialClocks: Object.freeze({
    mineBreachClock: { value: 0, max: 6, status: 'ACTIVE' },
    allianceMomentum: { value: 0, max: 6, status: 'ACTIVE' }
  }),
  nodes: Object.freeze({
    // EPISÓDIO 1: LA LLEGADA
    'arrival-surface': {
      episode: 1,
      title: 'Chegada a Morjanssik',
      locationId: LOC.docks,
      summary: 'A equipe da Aliança chega à cidade flutuante de Morjanssik em Mon Cala para investigar a paralisia na extração de minério dos Cruzadores Calamari.',
      narration: 'Morjanssik flutua sobre as águas escuras de Mon Cala como uma enorme carcaça de metal e coral, cercada por névoa marítima e torres de controle. A produção de minério das profundezas do leito oceânico, crucial para a construção dos Cruzadores da Frota Rebelde, foi paralisada por acidentes misteriosos.',
      visibleNpcIds: [NPC.kalbrac],
      checks: [
        { skill: 'Frieza', difficulty: 'Fácil', purpose: 'manter postura diplomática perante os operadores do porto' },
        { skill: 'Percepção', difficulty: 'Média', purpose: 'identificar a presença atípica de patrulhas de segurança Quarren' }
      ],
      choices: [
        { id: 'dock-cooperate', label: 'Cooperar com a segurança e seguir ao encontro de Kalbrac', to: 'management-meeting', effects: { arrivalApproach: 'cooperative' } },
        { id: 'dock-observe', label: 'Observar o tráfego de carga antes de entrar no escritório', to: 'management-meeting', effects: { arrivalApproach: 'cautious', cargoNoticed: true } }
      ]
    },
    'management-meeting': {
      episode: 1,
      title: 'Reunião de Gerência em Morjanssik',
      locationId: LOC.management,
      summary: 'O diplomata Mon Calamari Kalbrac apresenta a situação ao lado do Gerente Principal Moren Chonk; tensões raciais e suspeitas de sabotagem interna emergem.',
      narration: 'No escritório central com vista para o oceano, Kalbrac e Moren Chonk discutem rispidamente. Chonk acusa o Conselho Calamari de negligência, enquanto Kalbrac aponta que remessas de droides perfuradores desapareceram e que o sabotador possui acesso interno.',
      visibleNpcIds: [NPC.kalbrac, NPC.chonk],
      checks: [
        { skill: 'Negociação', difficulty: 'Média', purpose: 'apaziguar a tensão entre Chonk e Kalbrac' },
        { skill: 'Carisma', difficulty: 'Média', purpose: 'conquistar a confiança de Moren Chonk' },
        { skill: 'Perspicácia', difficulty: 'Média', purpose: 'detectar evasivas sobre os registros de carga' }
      ],
      choices: [
        { id: 'investigate-underworld', label: 'Seguir a pista do mercado clandestino até o Arpão Enferrujado', to: 'rusty-harpoon-tavern', effects: { investigationTarget: 'merv' } }
      ]
    },

    // EPISÓDIO 2: UNA NOCHE EN LA CIUDAD
    'rusty-harpoon-tavern': {
      episode: 2,
      title: 'O Arpão Enferrujado',
      locationId: LOC.rustyHarpoon,
      summary: 'Uma taverna úmida no setor Quarren onde o contrabandista Walif Merv negocia peças roubadas e componentes de droides.',
      narration: 'A taverna é escura, barulhenta e cheira a salmoura e óleo de motor. No fundo do salão, Walif Merv divide uma mesa com valentões Quarren, contando créditos e oferecendo peças que deveriam estar nas minas submarinas.',
      visibleNpcIds: [NPC.merv],
      checks: [
        { skill: 'Negociação', difficulty: '1 Dificuldade + 1 Desafio', purpose: 'comprar a cooperação de Merv sem violência' },
        { skill: 'Coerção', difficulty: 'Média', purpose: 'intimidar Merv a revelar o comprador do minério' },
        { skill: 'Manha', difficulty: 'Média', purpose: 'descobrir quem forneceu o código das batisferas' }
      ],
      choices: [
        { id: 'tavern-negotiate', label: 'Fechar acordo com Walif Merv e obter o código de acesso', to: 'catwalk-confrontation', effects: { mervBribed: true } },
        { id: 'tavern-fight', label: 'Enfrentar os capangas de Merv quando a conversa azedar', to: 'catwalk-confrontation', effects: { barFight: true } }
      ]
    },
    'catwalk-confrontation': {
      episode: 2,
      title: 'Perseguição nas Passarelas',
      locationId: LOC.catwalks,
      summary: 'Merv tenta fugir pelas passarelas externas sobre o oceano tempestuoso; a captura rende evidências sobre o subgerente Kelmut Wolg.',
      narration: 'Chuva e ondas violentas fustigam as passarelas de serviço do anel inferior da cidade. Merv salta entre tubulações de vapor tentando alcançar um bote submarino. Ao ser cercado, ele confessa em pânico que vendeu os droides e explosivos para indivíduos com identidades Mon Calamari suspeitas que desceram ao leito marinho.',
      visibleNpcIds: [NPC.merv],
      checks: [
        { skill: 'Atletismo', difficulty: 'Média', purpose: 'interceptar a rota de fuga nas passarelas molhadas' },
        { skill: 'Percepção', difficulty: 'Média', purpose: 'localizar o datapad de transações deixado por Merv' }
      ],
      choices: [
        { id: 'catwalk-capture', label: 'Interrogar Merv, recolher as provas e descer à batisfera', to: 'bathysphere-descent', effects: { mervInterrogated: true, wolgImplicated: true } }
      ]
    },

    // EPISÓDIO 3: HUNDIRSE
    'bathysphere-descent': {
      episode: 3,
      title: 'Descida na Batisfera Kulv-Bockar',
      locationId: LOC.oceanTrench,
      summary: 'A viagem na batisfera de transporte Kulv-Bockar desce quase um quilômetro na fossa oceânica até o leito marinho de Morjanssik.',
      narration: 'A batisfera de transporte Kulv-Bockar range sob a imensa pressão hidrostática enquanto desce pelas profundezas azuis-escuras de Mon Cala. Abaixo, a grande trincheira marinha revela os mastros iluminados e os domos da instalação mineradora cravada na rocha oceânica.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Mecânica', difficulty: 'Média', purpose: 'estabilizar os sistemas de pressão da batisfera' },
        { skill: 'Vigilância', difficulty: 'Média', purpose: 'detectar anomalias elétricas nas eclusas de atracação' }
      ],
      choices: [
        { id: 'bathysphere-dock-clean', label: 'Atracar discretamente na eclusa da mina', to: 'staging-trap', effects: { stealthDescent: true } },
        { id: 'bathysphere-dock-emergency', label: 'Atracar às pressas após oscilação nos controles', to: 'staging-trap', effects: { roughDescent: true } }
      ]
    },

    // EPISÓDIO 4: LOS ACUSADOS
    'staging-trap': {
      episode: 4,
      title: 'Emboscada na Área de Preparação',
      locationId: LOC.stagingArea,
      summary: 'Ao desembarcarem nas profundezas, os agentes são emboscados e acusados de sabotagem pelos conspiradores imperiais disfarçados.',
      narration: 'A eclusa se abre para o ar úmido e pressurizado da área de preparação da mina. Antes que o grupo possa se orientar, guardas de segurança armados e dois oficiais com fisionomia Mon Calamari — Capitão Generet e Tenente Huv — fecham as saídas. Generet, com uma voz estranhamente rouca para um Mon Cal, acusa o grupo de espionagem imperial e ordena sua detenção.',
      visibleNpcIds: [NPC.generet, NPC.huv],
      checks: [
        { skill: 'Percepção', difficulty: 'Difícil', purpose: 'notar anomalias cirúrgicas e voz artificial no disfarce de Generet' },
        { skill: 'Disciplina', difficulty: 'Média', purpose: 'resistir ao interrogatório na cela de retenção' }
      ],
      choices: [
        { id: 'staging-breakout', label: 'Efetuar a fuga da detenção com ajuda de Kelmut Wolg e do droide Q-4', to: 'staging-investigation', effects: { escapedDetention: true } }
      ]
    },
    'staging-investigation': {
      episode: 4,
      title: 'A Verdade nas Profundezas',
      locationId: LOC.stagingArea,
      summary: 'Kelmut Wolg, arrependido, confessa que foi coagido pelos imperiais disfarçados que pretendem inundar toda a mina nível 42.',
      narration: 'Nos corredores de serviço da mina submarina, o subgerente Quarren Kelmut Wolg surge desolado ao lado de um droide barrenador Q-4 modificado. Wolg revela que os supostos inspetores Mon Calamari são agentes da Divisão de Desestabilização Imperial. Eles tomaram a plataforma laser de mineração e estão descendo ao Nível 42 para fundir os pilares de sustentação e afundar a cidade!',
      visibleNpcIds: [NPC.wolg, NPC.q4],
      checks: [
        { skill: 'Liderança', difficulty: 'Fácil', purpose: 'reunir os droides barrenadores Q-4 para apoiar a ofensiva' },
        { skill: 'Computadores', difficulty: 'Média', purpose: 'baixar o manifesto da sabotagem e provas incontestáveis do complô imperial' }
      ],
      choices: [
        { id: 'rush-level-42', label: 'Descer imediatamente ao Nível 42 para impedir a quebra da contenção marinha', to: 'level-42-showdown', effects: { evidenceSecured: true, q4Allied: true } }
      ]
    },

    // EPISÓDIO 5: FUSIÓN
    'level-42-showdown': {
      episode: 5,
      title: 'Batalha no Nível 42 — Plataforma Laser',
      locationId: LOC.mineLevel42,
      summary: 'O confronto decisivo no Nível 42 contra o Capitão Generet e a plataforma laser de mineração antes que a pressão do oceano rompa a mina.',
      narration: 'O Nível 42 é uma gigantesca caverna submarina onde o leito rochoso sustenta milhões de toneladas de oceano. A plataforma laser de mineração avança roncando como um tanque blindado; seu feixe perfurante já superaquece a rocha do pilar central. Generet e seus agentes imperiais atiram da carcaça do veículo, determinados a quebrar a contenção e afogar todos na mina.',
      visibleNpcIds: [NPC.generet, NPC.huv, NPC.limeren],
      checks: [
        { skill: 'Artilharia', difficulty: 'Difícil', purpose: 'alvejar os controles externos ou a escotilha da plataforma laser' },
        { skill: 'Armas Pesadas', difficulty: 'Média', purpose: 'abrir fogo concentrado contra a blindagem traseira da plataforma' },
        { skill: 'Liderança', difficulty: 'Fácil', purpose: 'comandar os droides Q-4 para perfurar a blindagem do veículo inimigo' },
        { skill: 'Atletismo', difficulty: 'Difícil', purpose: 'escalar a plataforma em movimento sob fogo inimigo' }
      ],
      choices: [
        {
          id: 'resolve-victory',
          label: 'Neutralizar a plataforma laser, capturar os sabotadores e salvar a mina de Morjanssik',
          to: 'epilogue-victory',
          condition: 'flag:laserPlatformDisabled',
          effects: { mineSaved: true, imperialPlotExposed: true, resolutionOutcome: 'victory' }
        },
        {
          id: 'resolve-evacuation',
          label: 'A contenção cedeu — evacuar a equipe e os sobreviventes com as provas do complô',
          to: 'epilogue-evacuation',
          condition: 'flag:mineBreached',
          effects: { mineSaved: false, imperialPlotExposed: true, resolutionOutcome: 'evacuation' }
        }
      ]
    },

    // EPÍLOGOS
    'epilogue-victory': {
      episode: 5,
      title: 'Epílogo — Vitória e Lealdade Quarren',
      locationId: LOC.management,
      summary: 'A mina é salva intacta, o complô da Desestabilização Imperial é exposto e o povo Quarren declara apoio integral à Aliança Rebelde.',
      narration: 'Com a queima da escotilha e a rendição dos agentes imperiais desmascarados, os alarmes da mina são silenciados. Moren Chonk e o Conselho Calamari recebem as provas gravadas da infiltração imperial. Envergonhado e grato, o conselho Quarren firma aliança incondicional com a Rebelião, garantindo o fluxo contínuo de minério para a construção dos Cruzadores da Frota.',
      visibleNpcIds: [NPC.kalbrac, NPC.chonk, NPC.wolg, NPC.q4],
      checks: [],
      choices: []
    },
    'epilogue-evacuation': {
      episode: 5,
      title: 'Epílogo — Evacuação e Resistência',
      locationId: LOC.docks,
      summary: 'A mina sofre inundação severa paralisando as obras de cruzadores por um ano, mas a trama imperial foi desmascarada e a Aliança sobrevive.',
      narration: 'A água do oceano rompe as galerias inferiores, forçando uma evacuação desesperada na batisfera. Morjanssik perde suas minas por pelo menos um ano, adiando dois novos Cruzadores Calamari para a Frota Rebelde. No entanto, as provas do complô imperial preservadas pelo grupo uniram Mon Calamari e Quarren contra o verdadeiro inimigo, frustrando o objetivo político do Império.',
      visibleNpcIds: [NPC.kalbrac, NPC.chonk],
      checks: [],
      choices: []
    }
  })
});

function conditionOk(choice, progress) {
  const c = choice.condition;
  if (!c) return true;
  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  return true;
}

function playerNode(node, progress) {
  if (!node) return null;
  return {
    id: progress.currentNodeId,
    episode: node.episode,
    title: node.title,
    locationId: node.locationId,
    summary: node.summary,
    narration: node.narration,
    visibleNpcIds: [...(node.visibleNpcIds || [])],
    checks: node.checks || [],
    choices: (node.choices || []).filter(c => conditionOk(c, progress)).map(c => ({ id: c.id, label: c.label }))
  };
}

function baseProgress({ rewardsApplied = false } = {}) {
  return {
    id: ID,
    status: 'active',
    currentNodeId: DEAD_IN_THE_WATER.startNodeId,
    visitedNodeIds: [DEAD_IN_THE_WATER.startNodeId],
    checkHistory: [],
    flags: {
      mineBreachClock: 0,
      allianceMomentum: 0,
      laserPlatformDisabled: false,
      mineBreached: false,
      logorCaptured: false,
      evidenceSecured: false
    },
    systems: {
      encounters: { activeId: null, history: [] },
      clocks: {
        mineBreach: { value: 0, max: 6, status: 'active' },
        momentum: { value: 0, max: 6, status: 'active' }
      }
    },
    sceneJournal: [],
    rewardsApplied: Boolean(rewardsApplied),
    startedAt: now(),
    updatedAt: now()
  };
}

function ensureSystems(progress) {
  progress.systems = progress.systems || {};
  progress.systems.encounters = progress.systems.encounters || { activeId: null, history: [] };
  progress.systems.clocks = progress.systems.clocks || {
    mineBreach: { value: 0, max: 6, status: 'active' },
    momentum: { value: 0, max: 6, status: 'active' }
  };
  return progress.systems;
}

export function seedDeadInTheWaterWorld(world) {
  const next = clone(world || {});
  next.locations = next.locations || {};
  next.npcs = next.npcs || {};
  next.factions = next.factions || {};

  const locations = [
    loc(LOC.docks, 'Docas de Morjanssik', 'Doca de superfície da cidade flutuante mineradora de Mon Cala.', ['mon-cala', 'docks']),
    loc(LOC.traffic, 'Torre de Controle de Tráfego', 'Centro de controle de tráfego aéreo e marítimo de Morjanssik.', ['mon-cala', 'traffic']),
    loc(LOC.management, 'Escritórios de Gerência', 'Gabinete executivo de Moren Chonk com vista panorâmica do oceano.', ['mon-cala', 'management']),
    loc(LOC.rustyHarpoon, 'O Arpão Enferrujado', 'Taverna decadente no setor operário Quarren de Morjanssik.', ['mon-cala', 'underworld', 'tavern']),
    loc(LOC.catwalks, 'Passarelas Externas', 'Passarelas industriais expostas às ondas violentas de Mon Cala.', ['mon-cala', 'catwalks']),
    loc(LOC.bathysphere, 'Doca da Batisfera', 'Estação de lançamento dos submarinos de transporte profundo.', ['mon-cala', 'submersible']),
    loc(LOC.oceanTrench, 'Fossa Oceânica Submarina', 'Trincheira abissal a quase 1 km abaixo da superfície.', ['mon-cala', 'deep-ocean']),
    loc(LOC.stagingArea, 'Área de Preparação da Mina', 'Complexo de docas e celas no fundo do mar.', ['mon-cala', 'mine-staging']),
    loc(LOC.mineLevel42, 'Mina Morjanssik — Nível 42', 'Nível mais profundo da mina onde os pilares sustentam o oceano.', ['mon-cala', 'mine-level-42'])
  ];
  for (const l of locations) {
    next.locations[l.id] = next.locations[l.id] ? { ...l, ...next.locations[l.id] } : l;
  }

  const factions = [
    faction(FACTION.alliance, 'Aliança Rebelde', 'Garantir suprimentos de minério para a construção de cruzadores capitais.', true),
    faction(FACTION.miners, 'Trabalhadores Quarren de Morjanssik', 'Preservar suas minas, autonomia e integridade dos trabalhadores.', true),
    faction(FACTION.empire, 'Ramo de Desestabilização Imperial', 'Destruir a capacidade naval de Mon Cala e incitar guerra civil entre Quarren e Calamari.', false)
  ];
  for (const f of factions) {
    next.factions[f.id] = next.factions[f.id] ? { ...f, ...next.factions[f.id] } : f;
  }

  const npcs = [
    npc(NPC.kalbrac, 'Kalbrac', LOC.management, FACTION.alliance, {
      species: 'Mon Calamari', sex: 'masculino', occupation: 'diplomata e oficial de ligação',
      knownToPlayer: true, relation: 'ally', goal: 'Manter a aliança entre Mon Calamari e Quarren.'
    }),
    npc(NPC.chonk, 'Moren Chonk', LOC.management, FACTION.miners, {
      species: 'Quarren', sex: 'masculino', occupation: 'gerente principal de Morjanssik',
      knownToPlayer: true, relation: 'neutral', goal: 'Proteger os interesses comerciais e a segurança de Morjanssik.'
    }),
    npc(NPC.merv, 'Walif Merv', LOC.rustyHarpoon, null, {
      species: 'Quarren', sex: 'masculino', occupation: 'contrabandista e comerciante do submundo',
      knownToPlayer: false, relation: 'hostile', goal: 'Lucrar com peças e escapar sem ser preso.'
    }),
    npc(NPC.wolg, 'Kelmut Wolg', LOC.stagingArea, FACTION.miners, {
      species: 'Quarren', sex: 'masculino', occupation: 'subgerente de operações mineradoras',
      knownToPlayer: false, relation: 'neutral', goal: 'Livrar-se da chantagem imperial e salvar sua consciência.'
    }),
    npc(NPC.generet, 'Capitão Generet', LOC.mineLevel42, FACTION.empire, {
      species: 'Humano (Disfarçado cirurgicamente de Mon Calamari)', sex: 'masculino', occupation: 'agente imperial de desestabilização',
      knownToPlayer: false, relation: 'hostile', goal: 'Inundar a mina de Morjanssik e destruir o apoio naval da Rebelião.'
    }),
    npc(NPC.huv, 'Tenente Huv', LOC.mineLevel42, FACTION.empire, {
      species: 'Humano (Disfarçado de Mon Calamari)', sex: 'masculino', occupation: 'agente imperial de desestabilização',
      knownToPlayer: false, relation: 'hostile', goal: 'Destruir as defesas da mina e assegurar promoção no Império.'
    }),
    npc(NPC.limeren, 'Agente Limeren', LOC.mineLevel42, FACTION.empire, {
      species: 'Humano (Disfarçado de Mon Calamari)', sex: 'masculino', occupation: 'agente imperial',
      knownToPlayer: false, relation: 'hostile', goal: 'Eliminar qualquer intruso antes do colapso da mina.'
    }),
    npc(NPC.q4, 'Droide Q-4', LOC.stagingArea, FACTION.alliance, {
      species: 'Droide de Mineração Q-4', sex: 'não aplicável', occupation: 'droide perfurador',
      knownToPlayer: false, relation: 'friendly', goal: 'Auxiliar os mineiros e combater sabotadores.'
    })
  ];
  for (const n of npcs) {
    next.npcs[n.id] = next.npcs[n.id] ? { ...n, ...next.npcs[n.id] } : n;
  }

  next.publishedAdventureRewards = next.publishedAdventureRewards || {};
  if (!next.publishedAdventure || next.publishedAdventure.id !== ID) {
    next.publishedAdventure = baseProgress({ rewardsApplied: Boolean(next.publishedAdventureRewards[ID]) });
  }
  ensureSystems(next.publishedAdventure);
  return next;
}

export function startDeadInTheWater(world) {
  const next = seedDeadInTheWaterWorld(world);
  next.publishedAdventure = baseProgress({ rewardsApplied: Boolean(next.publishedAdventureRewards?.[ID]) });
  const p = next.publishedAdventure;
  p.visitedNodeIds = [p.currentNodeId];
  p.updatedAt = now();
  return {
    world: next,
    progress: clone(p),
    node: playerNode(DEAD_IN_THE_WATER.nodes[p.currentNodeId], p)
  };
}

export function currentDeadInTheWaterScene(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) return null;
  const node = DEAD_IN_THE_WATER.nodes[p.currentNodeId];
  if (!node) return null;
  return {
    locationId: node.locationId,
    title: node.title,
    narration: node.narration,
    visibleNpcIds: [...(node.visibleNpcIds || [])]
  };
}

export function currentDeadInTheWaterChecks(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) return [];
  const node = DEAD_IN_THE_WATER.nodes[p.currentNodeId];
  if (!node || !Array.isArray(node.checks)) return [];
  return node.checks.map((c, index) => ({
    index,
    id: `${p.currentNodeId}-check-${index}`,
    skill: c.skill,
    difficulty: c.difficulty,
    purpose: c.purpose
  }));
}

export function adjudicateDeadInTheWaterCheck(world, { checkIndex = 0, result = {} } = {}) {
  const next = seedDeadInTheWaterWorld(world);
  const p = next.publishedAdventure;
  if (p.status !== 'active') throw new Error('A aventura Morte nas Profundezas não está ativa.');
  ensureSystems(p);

  const checks = currentDeadInTheWaterChecks(next);
  const reqIdx = Number(checkIndex);
  const checkSpec = checks.find(x => x.index === reqIdx) || checks[reqIdx];
  if (!checkSpec) throw new Error('Teste de aventura indisponível neste ponto.');

  const success = Boolean(result.success);
  const netAdvantage = Number(result.netAdvantage) || 0;
  const netSuccess = Number(result.netSuccess) || 0;
  const triumph = Math.max(0, Number(result.triumph) || 0);
  const despair = Math.max(0, Number(result.despair) || 0);

  // Mechanical consequences on clocks
  const breachDelta = Math.max(0, -netAdvantage > 1 ? 1 : 0) + (despair * 2);
  const momentumDelta = (success ? 1 : 0) + triumph;
  p.flags.mineBreachClock = clamp((p.flags.mineBreachClock || 0) + breachDelta, 0, 6);
  p.flags.allianceMomentum = clamp((p.flags.allianceMomentum || 0) + momentumDelta, 0, 6);

  if (p.currentNodeId === 'level-42-showdown') {
    if (p.flags.allianceMomentum >= 4) {
      p.flags.laserPlatformDisabled = true;
    }
    if (p.flags.mineBreachClock >= 5) {
      p.flags.mineBreached = true;
    }
  }

  const entry = {
    at: now(),
    nodeId: p.currentNodeId,
    checkId: checkSpec.id,
    skill: checkSpec.skill,
    success,
    netSuccess,
    netAdvantage,
    triumph,
    despair
  };
  p.checkHistory.push(entry);
  p.updatedAt = now();
  next.publishedAdventure = p;

  return {
    world: next,
    progress: clone(p),
    adjudication: entry
  };
}

export function currentDeadInTheWaterEncounter(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'active') return null;
  ensureSystems(p);
  const history = p.systems.encounters.history || [];

  let id = null, label = null;
  if (p.currentNodeId === 'rusty-harpoon-tavern' || (p.currentNodeId === 'catwalk-confrontation' && p.flags?.barFight)) {
    id = 'rusty-harpoon-brawl';
    label = 'Briga no Arpão Enferrujado';
  } else if (p.currentNodeId === 'level-42-showdown') {
    id = 'mining-laser-climax';
    label = 'Confronto da Plataforma Laser — Nível 42';
  }

  if (!id) return null;
  const resolved = history.some(x => x.encounterId === id && x.outcome);
  return {
    id, label, available: !resolved,
    status: resolved ? 'resolved' : (p.systems.encounters.activeId === id ? 'active' : 'ready')
  };
}

export function deadInTheWaterEncounterSpec(world, encounterId = null) {
  const next = seedDeadInTheWaterWorld(world);
  const p = next.publishedAdventure;
  const current = currentDeadInTheWaterEncounter(next);
  const id = String(encounterId || current?.id || '');
  if (!current || id !== current.id || !current.available) {
    throw new Error('Encontro publicado indisponível neste ponto.');
  }

  if (id === 'rusty-harpoon-brawl') {
    return {
      id,
      label: 'Briga no Arpão Enferrujado',
      playerInitiativeSkill: 'frialdad',
      npcInitiativeSkill: 'alerta',
      adversaries: [
        { profileKey: 'walif_merv', worldNpcId: NPC.merv, range: 'Médio' },
        { profileKey: 'quarren_thug', count: 3, range: 'Curto' }
      ]
    };
  }

  if (id === 'mining-laser-climax') {
    return {
      id,
      label: 'Confronto da Plataforma Laser — Nível 42',
      playerInitiativeSkill: 'disciplina',
      npcInitiativeSkill: 'frialdad',
      adversaries: [
        { profileKey: 'capitan_generet', worldNpcId: NPC.generet, range: 'Longo' },
        { profileKey: 'teniente_huv', worldNpcId: NPC.huv, range: 'Médio' },
        { profileKey: 'agente_limeren', worldNpcId: NPC.limeren, range: 'Médio' },
        { profileKey: 'imperial_saboteur', count: 4, range: 'Longo' }
      ]
    };
  }

  throw new Error('Encontro publicado desconhecido.');
}

export function markDeadInTheWaterEncounterActive(world, encounterId) {
  const next = seedDeadInTheWaterWorld(world);
  const p = next.publishedAdventure;
  ensureSystems(p);
  const current = currentDeadInTheWaterEncounter(next);
  const id = String(encounterId || current?.id || '');
  if (!current || !current.available || id !== current.id) {
    throw new Error('Encontro publicado indisponível neste ponto.');
  }
  p.systems.encounters.activeId = id;
  p.updatedAt = now();
  return next;
}

export function recordDeadInTheWaterEncounterOutcome(world, { encounterId, outcome, adversaries = [] } = {}) {
  const next = seedDeadInTheWaterWorld(world);
  const p = next.publishedAdventure;
  ensureSystems(p);
  const current = currentDeadInTheWaterEncounter(next);
  const id = String(encounterId || p.systems.encounters.activeId || current?.id || '');
  if (!id || !current || id !== current.id) throw new Error('Resultado de encontro incompatível com a cena atual.');

  const record = {
    at: now(),
    nodeId: p.currentNodeId,
    encounterId: id,
    outcome: outcome || 'ended',
    adversaries: (adversaries || []).map(a => ({
      id: a.id ?? null,
      worldNpcId: a.worldNpcId ?? null,
      profileKey: a.profileKey ?? null,
      status: a.status ?? 'active'
    }))
  };

  p.systems.encounters.history.push(record);
  p.systems.encounters.activeId = null;

  if (id === 'mining-laser-climax') {
    if (outcome === 'adversaries_defeated') {
      p.flags.laserPlatformDisabled = true;
    } else {
      p.flags.mineBreached = true;
    }
  }

  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, record: clone(record) };
}

function applyDeadInTheWaterEpilogueConsequences(world, progress) {
  if (progress.consequencesApplied) return;
  const outcome = progress.flags?.resolutionOutcome;
  const isVictory = outcome === 'victory';

  // Update NPCs
  const wolg = world.npcs?.[NPC.wolg];
  if (wolg) {
    wolg.status = isVictory ? 'reabilitado' : 'sob_custodia';
    wolg.relation = 'friendly';
  }
  const generet = world.npcs?.[NPC.generet];
  if (generet) {
    generet.status = isVictory ? 'capturado' : 'evadido';
  }

  // Living World depth integration
  const deep = ensureLivingWorldDepth(world);
  world.livingDepth = deep.livingDepth;
  const tick = Number(world.clock?.tick) || 0;

  const memory = (npcId, summary, importance = 75) => {
    const s = world.livingDepth?.npcs?.[npcId];
    if (!s) return;
    s.memory = Array.isArray(s.memory) ? s.memory : [];
    if (!s.memory.some(m => m.summary === summary)) {
      s.memory.push({ tick, kind: 'published-adventure', summary, importance, actor: ID });
    }
  };

  const rel = (factionId, delta) => {
    const s = world.livingDepth?.factions?.[factionId];
    if (!s) return;
    s.relations = s.relations && typeof s.relations === 'object' ? s.relations : {};
    s.relations['player-group'] = clamp((Number(s.relations['player-group']) || 0) + delta, -100, 100);
  };

  memory(NPC.kalbrac, isVictory
    ? 'A equipe da Aliança salvou as minas de Morjanssik e selou o apoio total dos Quarren.'
    : 'A equipe da Aliança expôs a trama imperial, embora as minas de Morjanssik tenham sofrido danos severos.', 90);

  memory(NPC.chonk, isVictory
    ? 'Os Rebeldes protegeram os operários Quarren da sabotagem imperial com honra.'
    : 'Os Rebeldes revelaram o plano imperial e salvaram as vidas dos mineiros durante a evacuação.', 85);

  rel(FACTION.alliance, isVictory ? 20 : 10);
  rel(FACTION.miners, isVictory ? 25 : 10);
  rel(FACTION.empire, -30);

  // Register aftermath plan in the Living World
  const plans = world.publishedAdventureWorldPlans = { ...(world.publishedAdventureWorldPlans || {}) };
  const createdDay = Math.floor(tick / 4);
  plans[ID] = [
    {
      id: 'quarren-alliance-fleet-reinforcement',
      factionId: FACTION.miners,
      status: 'active',
      visibility: 'post-epilogue',
      createdDay,
      goal: isVictory
        ? 'Fornecer minério ininterrupto para a conclusão de dois novos Cruzadores Calamari para a Frota Rebelde.'
        : 'Reconstruir os níveis inundados de Morjanssik com auxílio de engenheiros da Aliança.'
    },
    {
      id: 'imperial-destabilization-retaliation',
      factionId: FACTION.empire,
      status: 'latent',
      visibility: 'secret-until-resolved',
      createdDay,
      goal: 'Investigar o fracasso do Capitão Generet em Morjanssik e preparar contra-ataque da Divisão de Desestabilização.'
    }
  ];

  world.publishedAdventureConsequences = {
    ...(world.publishedAdventureConsequences || {}),
    [ID]: {
      appliedAt: now(),
      resolutionOutcome: outcome,
      mineSaved: Boolean(progress.flags?.mineSaved),
      q4Joined: true,
      wolgReconciled: true,
      livingWorldApplied: true
    }
  };

  progress.consequencesApplied = true;
}

export function advanceDeadInTheWater(world, choiceId) {
  const next = seedDeadInTheWaterWorld(world);
  const p = next.publishedAdventure;
  if (p.status !== 'active') throw new Error('A aventura Morte nas Profundezas não está ativa.');
  ensureSystems(p);

  const node = DEAD_IN_THE_WATER.nodes[p.currentNodeId];
  if (!node) throw new Error('Cena atual não existe.');
  const choice = (node.choices || []).find(c => c.id === String(choiceId));
  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha indisponível neste ponto da aventura.');

  const fromNodeId = p.currentNodeId;
  const targetNodeId = choice.to;
  const effects = { ...(choice.effects || {}) };

  p.flags = { ...(p.flags || {}), ...effects };
  p.currentNodeId = targetNodeId;
  if (!p.visitedNodeIds.includes(targetNodeId)) {
    p.visitedNodeIds.push(targetNodeId);
  }

  p.sceneJournal.push({
    fromNodeId,
    toNodeId: targetNodeId,
    choiceId: choice.id,
    choiceLabel: choice.label,
    at: now()
  });

  if (targetNodeId.startsWith('epilogue-')) {
    p.status = 'completed';
    p.completedAt = now();
    applyDeadInTheWaterEpilogueConsequences(next, p);
  }

  p.updatedAt = now();
  next.publishedAdventure = p;

  return {
    world: next,
    progress: clone(p),
    node: playerNode(DEAD_IN_THE_WATER.nodes[targetNodeId], p),
    completed: p.status === 'completed'
  };
}

export function deadInTheWaterStatus(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) {
    return {
      available: true,
      active: false,
      catalog: {
        id: ID,
        title: DEAD_IN_THE_WATER.title,
        campaignModes: [...DEAD_IN_THE_WATER.campaignModes],
        rulesProfile: DEAD_IN_THE_WATER.rulesProfile
      }
    };
  }
  ensureSystems(p);
  const node = DEAD_IN_THE_WATER.nodes[p.currentNodeId];
  const reward = world?.publishedAdventureRewards?.[ID] || null;

  return {
    available: true,
    active: p.status === 'active',
    completed: p.status === 'completed',
    archived: p.status === 'archived',
    progress: clone(p),
    node: node ? playerNode(node, p) : null,
    encounter: currentDeadInTheWaterEncounter(world),
    reward: reward?.applied ? clone(reward) : null
  };
}

export function markDeadInTheWaterRewardsApplied(world, details = {}) {
  const next = clone(world);
  next.publishedAdventureRewards = next.publishedAdventureRewards || {};
  next.publishedAdventureRewards[ID] = {
    applied: true,
    appliedAt: now(),
    xp: Number(details.xp) || 20,
    duty: Number(details.duty) || 10,
    note: details.note || 'Morte nas Profundezas — recompensa oficial de Deber e Pontos de Experiência'
  };
  if (next?.publishedAdventure?.id === ID) {
    next.publishedAdventure.rewardsApplied = true;
    next.publishedAdventure.updatedAt = now();
  }
  return next;
}

export function archiveDeadInTheWater(world) {
  const next = clone(world);
  const p = next?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'completed') {
    throw new Error('Morte nas Profundezas precisa estar concluída antes de arquivar.');
  }
  p.status = 'archived';
  p.archivedAt = now();
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}

export function currentDeadInTheWaterMasterRequest(world) {
  const s = deadInTheWaterStatus(world);
  if (!s.active && !s.completed) return null;
  const node = s.node;
  if (!node) return null;
  return {
    kind: 'published_adventure',
    adventureId: ID,
    adventureTitle: DEAD_IN_THE_WATER.title,
    nodeId: node.id,
    episode: node.episode,
    locationId: node.locationId,
    summary: node.summary,
    checks: currentDeadInTheWaterChecks(world),
    suggestedActions: (node.choices || []).filter(c => conditionOk(c, world.publishedAdventure)).map(c => c.label).slice(0, 6),
    antiSpoiler: true,
    source: SOURCE
  };
}
