// astrogation.mjs - Star Wars FFG Astrogation and Hyperspace Travel Rules

export const ROUTE_TYPES = Object.freeze({
  MAJOR: {
    id: 'MAJOR',
    labelPtBr: 'Rota Principal (Hipervia Primária)',
    labelEn: 'Major Hyperlane',
    baseDifficulty: 1,
    baseHours: 12,
    description: 'Rotas hiperespaciais amplamente mapeadas, balizadas e desobstruídas de sombras gravitacionais.',
    examples: ['Perlemian Trade Route', 'Corellian Run', 'Corellian Trade Spine', 'Rimma Trade Route', 'Hydian Way']
  },
  MINOR: {
    id: 'MINOR',
    labelPtBr: 'Rota Secundária (Hipervia Menor)',
    labelEn: 'Minor Hyperlane',
    baseDifficulty: 2,
    baseHours: 24,
    description: 'Rotas comerciais menores ou hipervias regionais com tráfego moderado e balizas ocasionais.',
    examples: ['Celanon Spur', 'Triellus Trade Route', 'Entralla Route', 'Five Veils Route', 'Great Gran Run']
  },
  SECONDARY: {
    id: 'SECONDARY',
    labelPtBr: 'Rota Pouco Frequentada / Desvio',
    labelEn: 'Secondary / Unfrequented Route',
    baseDifficulty: 3,
    baseHours: 48,
    description: 'Caminhos secundários, atalhos de contrabandistas ou rotas desatualizadas fora do mapa principal.',
    examples: ['Shapani Bypass', 'Carbonite Run', 'Minntooine Spur', 'Bakura Trace']
  },
  UNCHARTED: {
    id: 'UNCHARTED',
    labelPtBr: 'Espaço Inexplorado / Salto Cego',
    labelEn: 'Uncharted / Deep Space Jump',
    baseDifficulty: 4,
    baseHours: 96,
    description: 'Salto através de espaço não mapeado ou regiões sem coordenadas estelares verificadas.',
    examples: ['Unknown Regions', 'Wild Space', 'Fringes do Espaço Hutt']
  },
  HAZARDOUS: {
    id: 'HAZARDOUS',
    labelPtBr: 'Região Perigosa / Anomalia Estelar',
    labelEn: 'Hazardous / Dangerous Anomaly',
    baseDifficulty: 5,
    baseHours: 120,
    description: 'Espaço com alta densidade de sombras de massa, campos de asteroides densos ou anomalias.',
    examples: ['Deep Core', 'The Maw Cluster', 'Koornacht Cluster']
  }
});

export const HYPERDRIVE_CLASSES = Object.freeze({
  0.5: { multiplier: 0.5, rating: 'Classe 0.5', note: 'Altamente modificado / Protótipo militar (ex: Millennium Falcon)' },
  1: { multiplier: 1.0, rating: 'Classe 1', note: 'Padrão militar da Marinha Imperial / Caça estelar de alto desempenho' },
  2: { multiplier: 2.0, rating: 'Classe 2', note: 'Padrão comercial civil / Cargueiro leve (ex: YT-1300 standard)' },
  3: { multiplier: 3.0, rating: 'Classe 3', note: 'Cargueiro médio / Transporte pesado civil' },
  4: { multiplier: 4.0, rating: 'Classe 4', note: 'Cargueiro pesado de grande porte / Nave capital padrão' },
  10: { multiplier: 10.0, rating: 'Classe 10', note: 'Hiperpropulsor de reserva / emergência militar' },
  15: { multiplier: 15.0, rating: 'Classe 15', note: 'Hiperpropulsor de reserva básico de emergência civil' }
});

export const HYPERSPACE_MISHAP_TABLE = Object.freeze([
  {
    id: 'system_strain_spike',
    namePtBr: 'Pico de Tensão no Sistema',
    nameEn: 'System Strain Spike',
    minRoll: 1,
    maxRoll: 25,
    systemStrain: 4,
    hullTrauma: 0,
    hyperdriveDisabled: false,
    offCourse: false,
    collisionRisk: false,
    description: 'O hiperpropulsor sobrecarrega ao tentar estabilizar o túnel de hiperespaço. A nave sofre 4 de Tensão de Sistema.',
    narrative: 'Um estampido elétrico ecoa pelo convés de força enquanto os relés do hiperpropulsor descarregam nos capacitores principais.'
  },
  {
    id: 'off_course',
    namePtBr: 'Desvio de Rota em Hiperespaço',
    nameEn: 'Off-Course Arrival',
    minRoll: 26,
    maxRoll: 50,
    systemStrain: 2,
    hullTrauma: 0,
    hyperdriveDisabled: false,
    offCourse: true,
    delayMultiplier: 2.0,
    description: 'Flutuação de hiperespaço desvia as coordenadas do salto. A nave emerge em um setor ou sistema vizinho incorreto.',
    narrative: 'Ao retornar ao espaço real, a constelação projetada não coincide com o destino: a nave está a parsecs de distância do alvo.'
  },
  {
    id: 'collision_hazard',
    namePtBr: 'Risco de Colisão / Sombra de Gravidade',
    nameEn: 'Collision Hazard',
    minRoll: 51,
    maxRoll: 75,
    systemStrain: 3,
    hullTrauma: 2,
    hyperdriveDisabled: false,
    offCourse: true,
    collisionRisk: true,
    pilotCheckDifficulty: 3,
    description: 'Os sensores de corte de massa desativam o hiperpropulsor bruscamente na beira de um campo de detritos ou sombra gravitacional. Exige teste de Pilotagem (Espacial) Dificuldade Média (3) para evitar colisão catastrófica; a nave sofre 3 de Tensão e 2 de Dano no Casco.',
    narrative: 'As sirenes de colisão gritam no cockpit: rochas espaciais e destroços giram perigosamente perto da proa da nave!'
  },
  {
    id: 'hyperdrive_burnout',
    namePtBr: 'Queima do Motivador do Hiperpropulsor',
    nameEn: 'Hyperdrive Motivator Burnout',
    minRoll: 76,
    maxRoll: 90,
    systemStrain: 5,
    hullTrauma: 1,
    hyperdriveDisabled: true,
    offCourse: false,
    collisionRisk: false,
    repairDifficulty: 3,
    description: 'O motivador principal do hiperpropulsor derrete com o retorno de energia. O salto é abortado, a nave sofre 5 de Tensão de Sistema e o hiperpropulsor fica inoperante até reparos (Mecânica Dificuldade 3).',
    narrative: 'Fumaça acre sobe da câmara dos motores de hiperespaço. O computador acusa falha crítica de propulsão interestelar.'
  },
  {
    id: 'hostile_ambush',
    namePtBr: 'Surgimento em Vetor Hostil / Patrulha Imperial',
    nameEn: 'Hostile Vector Emergence',
    minRoll: 91,
    maxRoll: 100,
    systemStrain: 1,
    hullTrauma: 0,
    hyperdriveDisabled: false,
    offCourse: false,
    hostileEncounter: true,
    description: 'A nave emerge exatamente dentro da grade de varredura ativa de uma patrulha imperial ou emboscada pirata com escudos e armas erguidos.',
    narrative: 'O alerta de travamento de alvos hostis pisca nos consoles logo após a transição do hiperespaço.'
  }
]);

export function calculateAstrogationDifficulty({
  routeType = 'MAJOR',
  hasNavicomputer = true,
  hasAstromech = false,
  starCharts = 'CURRENT',
  rushedJump = false,
  damagedNavicomputer = false,
  damagedHyperdrive = false,
  stellarHazard = false,
  customDifficulty = null,
  customSetback = 0,
  customBoost = 0
} = {}) {
  const normRoute = String(routeType).toUpperCase();
  const route = ROUTE_TYPES[normRoute] ?? ROUTE_TYPES.MAJOR;

  let baseDiff = customDifficulty !== null ? Math.max(1, Math.min(5, Number(customDifficulty) || 1)) : route.baseDifficulty;
  let difficulty = baseDiff;
  let upgrades = 0;
  let setbackDice = Math.max(0, Number(customSetback) || 0);
  let boostDice = Math.max(0, Number(customBoost) || 0);
  const modifiers = [];

  // Sem computador de navegação ou astromete (salto às cegas)
  if (!hasNavicomputer && !hasAstromech) {
    const blindJumpIncrease = (normRoute === 'UNCHARTED' || normRoute === 'HAZARDOUS') ? 3 : 2;
    difficulty += blindJumpIncrease;
    modifiers.push(`Sem computador de navegação ou astromete (+${blindJumpIncrease} Dificuldade)`);
  } else if (hasAstromech && hasNavicomputer) {
    boostDice += 1;
    modifiers.push('Assistência de droide astromete (+1 Boost)');
  }

  // Cartas estelares
  const chartStatus = String(starCharts).toUpperCase();
  if (chartStatus === 'OUTDATED') {
    setbackDice += 1;
    modifiers.push('Cartas estelares desatualizadas (+1 Setback)');
  } else if (chartStatus === 'NONE' || chartStatus === 'MISSING') {
    setbackDice += 2;
    modifiers.push('Ausência de cartas estelares da região (+2 Setback)');
  }

  // Computador avariado
  if (damagedNavicomputer) {
    setbackDice += 2;
    upgrades += 1;
    modifiers.push('Computador de navegação danificado (+2 Setback, +1 Upgrade de Dificuldade)');
  }

  // Hiperpropulsor danificado
  if (damagedHyperdrive) {
    setbackDice += 1;
    modifiers.push('Hiperpropulsor danificado (+1 Setback)');
  }

  // Salto às pressas / sob fogo
  if (rushedJump) {
    setbackDice += 1;
    upgrades += 1;
    modifiers.push('Cálculo às pressas sob fogo inimigo (+1 Setback, +1 Upgrade de Dificuldade)');
  }

  // Anomalia estelar / gravidade
  if (stellarHazard) {
    setbackDice += 2;
    upgrades += 1;
    modifiers.push('Anomalia estelar / Poço gravitacional regional (+2 Setback, +1 Upgrade de Dificuldade)');
  }

  // Conversão de dados roxos (Difficulty) em vermelhos (Challenge) devido a upgrades
  const rawDifficulty = difficulty;
  const challengeDice = Math.min(difficulty, upgrades);
  const remainingDifficulty = Math.max(0, difficulty - challengeDice);

  return {
    routeType: route.id,
    routeLabelPtBr: route.labelPtBr,
    baseDifficulty: baseDiff,
    effectiveDifficultyRating: rawDifficulty,
    upgradesApplied: upgrades,
    setbackDice,
    boostDice,
    pool: {
      difficulty: remainingDifficulty,
      challenge: challengeDice,
      setback: setbackDice,
      boost: boostDice
    },
    modifiers
  };
}

export function formatTravelHours(hours) {
  const h = Number(hours) || 0;
  if (h < 1) return `${Math.round(h * 60)} minutos`;
  if (h < 24) return `${Number(h.toFixed(1))} horas`;
  const days = Math.floor(h / 24);
  const remHours = Math.round(h % 24);
  if (remHours === 0) return `${days} dia${days > 1 ? 's' : ''}`;
  return `${days} dia${days > 1 ? 's' : ''} e ${remHours} hora${remHours > 1 ? 's' : ''}`;
}

export function calculateHyperspaceTravelTime({
  baseHours = null,
  routeType = 'MAJOR',
  hyperdriveClass = 1,
  advantageCount = 0,
  threatCount = 0,
  triumphCount = 0,
  despairCount = 0
} = {}) {
  const route = ROUTE_TYPES[String(routeType).toUpperCase()] ?? ROUTE_TYPES.MAJOR;
  const base = baseHours !== null ? Math.max(0.5, Number(baseHours) || 12) : route.baseHours;
  const hClass = Math.max(0.1, Number(hyperdriveClass) || 1.0);

  const rawHours = base * hClass;

  // Redução por Vantagem: 10% por Vantagem (máximo 50% de redução)
  const advReduction = Math.min(0.50, Math.max(0, Number(advantageCount) || 0) * 0.10);

  // Redução por Triunfo: 25% por Triunfo (máximo 50% de redução)
  const triumphReduction = Math.min(0.50, Math.max(0, Number(triumphCount) || 0) * 0.25);

  const totalDiscount = Math.min(0.75, advReduction + triumphReduction);

  // Atraso por Ameaça: 10% por Ameaça (máximo +100%)
  const threatDelay = Math.min(1.00, Math.max(0, Number(threatCount) || 0) * 0.10);

  // Atraso por Desespero: 25% por Desespero
  const despairDelay = Math.max(0, Number(despairCount) || 0) * 0.25;

  const totalDelay = threatDelay + despairDelay;

  const effectiveHours = Math.max(0.5, Number((rawHours * (1 - totalDiscount) * (1 + totalDelay)).toFixed(2)));

  return {
    baseHours: base,
    hyperdriveClass: hClass,
    rawHours: Number(rawHours.toFixed(2)),
    effectiveHours,
    formattedTime: formatTravelHours(effectiveHours),
    totalDiscountPercentage: Math.round(totalDiscount * 100),
    totalDelayPercentage: Math.round(totalDelay * 100)
  };
}

export function getHyperspaceMishap(roll = null, { rng = Math.random } = {}) {
  const r = roll !== null ? Math.max(1, Math.min(100, Math.floor(Number(roll)))) : (Math.floor(rng() * 100) + 1);
  const mishap = HYPERSPACE_MISHAP_TABLE.find(m => r >= m.minRoll && r <= m.maxRoll) ?? HYPERSPACE_MISHAP_TABLE[0];
  return {
    roll: r,
    ...structuredClone(mishap)
  };
}

export function resolveAstrogationNarrativeOutcome({
  successes = 0,
  failures = 0,
  advantages = 0,
  threats = 0,
  triumphs = 0,
  despairs = 0,
  routeType = 'MAJOR',
  hyperdriveClass = 1,
  baseHours = null,
  mishapRoll = null,
  rng = Math.random
} = {}) {
  const netSuccesses = Number(successes) - Number(failures);
  const netAdvantages = Number(advantages) - Number(threats);
  const numTriumphs = Math.max(0, Number(triumphs) || 0);
  const numDespairs = Math.max(0, Number(despairs) || 0);

  const jumpSuccessful = netSuccesses > 0;

  let systemStrain = 0;
  let hullTrauma = 0;
  let hyperdriveDisabled = false;
  let offCourse = false;
  let collisionRisk = false;
  let mishap = null;

  // Ameaças geram estresse nos sistemas da nave
  if (threats >= 2) {
    systemStrain += Math.floor(threats / 2);
  }

  // Falha no salto
  if (!jumpSuccessful) {
    systemStrain += 1;
  }

  // Desespero aciona a tabela de acidentes de hiperespaço
  if (numDespairs > 0) {
    mishap = getHyperspaceMishap(mishapRoll, { rng });
    systemStrain += mishap.systemStrain;
    hullTrauma += mishap.hullTrauma;
    if (mishap.hyperdriveDisabled) hyperdriveDisabled = true;
    if (mishap.offCourse) offCourse = true;
    if (mishap.collisionRisk) collisionRisk = true;
  }

  const travelTime = calculateHyperspaceTravelTime({
    baseHours,
    routeType,
    hyperdriveClass,
    advantageCount: Math.max(0, netAdvantages),
    threatCount: Math.max(0, -netAdvantages),
    triumphCount: numTriumphs,
    despairCount: numDespairs
  });

  let narrativeSummary = '';
  if (jumpSuccessful) {
    narrativeSummary = `Salto hiperespacial bem-sucedido (${netSuccesses} Sucesso${netSuccesses > 1 ? 's' : ''}). Tempo de viagem estimado: ${travelTime.formattedTime}.`;
    if (numTriumphs > 0) {
      narrativeSummary += ` Triunfo na navegação abriu vetor ideal de aproximação com economia de tempo.`;
    }
    if (threats > 0) {
      narrativeSummary += ` Turbulência de hiperespaço causou ${systemStrain} ponto(s) de Tensão no Sistema.`;
    }
  } else {
    narrativeSummary = `Falha no salto hiperespacial. O computador não conseguiu travar as coordenadas em segurança e o salto foi abortado.`;
  }

  if (mishap) {
    narrativeSummary += ` [ACIDENTE DE HIPERESPAÇO]: ${mishap.namePtBr} - ${mishap.description}`;
  }

  return {
    jumpSuccessful,
    netSuccesses,
    netAdvantages,
    triumphs: numTriumphs,
    despairs: numDespairs,
    travelTime,
    systemStrainSuffered: systemStrain,
    hullTraumaSuffered: hullTrauma,
    hyperdriveDisabled,
    offCourse,
    collisionRisk,
    mishap,
    narrativeSummary
  };
}
