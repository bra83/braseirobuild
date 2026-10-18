/* Terminal Tático Holonet — Motor de Batalha Holográfica Star Wars */

export const HOLO_BIOMES = Object.freeze({
  hangar_station: {
    key: 'hangar_station',
    namePtBr: 'Hangar & Baia de Carga Espacial',
    archetype: 'hangar',
    props: [
      { id: 'dock_ring', type: 'structure', shape: 'circle', cx: 50, cy: 50, r: 24, label: 'Berço de Pouso' },
      { id: 'crate_stack_1', type: 'cover_heavy', shape: 'rect', x: 28, y: 32, w: 10, h: 8, label: 'Caixotes Pressurizados (Cobertura Pesada)', cover: 'heavy' },
      { id: 'crate_stack_2', type: 'cover_light', shape: 'rect', x: 64, y: 58, w: 9, h: 6, label: 'Contêiner de Combustível (Cobertura Leve)', cover: 'light' },
      { id: 'blast_door', type: 'boundary', shape: 'line', x1: 15, y1: 82, x2: 85, y2: 82, label: 'Porta Blindada do Hangar' }
    ]
  },
  cantina_outpost: {
    key: 'cantina_outpost',
    namePtBr: 'Cantina / Entreposto da Orla',
    archetype: 'cantina',
    props: [
      { id: 'bar_counter', type: 'structure', shape: 'arc', cx: 50, cy: 30, r: 18, startAngle: 0, endAngle: 180, label: 'Balcão da Cantina' },
      { id: 'booth_1', type: 'cover_heavy', shape: 'circle', cx: 22, cy: 62, r: 8, label: 'Cabine Privativa A (Cobertura Pesada)', cover: 'heavy' },
      { id: 'booth_2', type: 'cover_heavy', shape: 'circle', cx: 78, cy: 62, r: 8, label: 'Cabine Privativa B (Cobertura Pesada)', cover: 'heavy' },
      { id: 'table_center', type: 'cover_light', shape: 'circle', cx: 50, cy: 68, r: 6, label: 'Mesa Central de Sabacc (Cobertura Leve)', cover: 'light' }
    ]
  },
  urban_sublevel: {
    key: 'urban_sublevel',
    namePtBr: 'Beco & Subnível Urbano',
    archetype: 'urban',
    props: [
      { id: 'walkway_main', type: 'structure', shape: 'rect', x: 38, y: 10, w: 24, h: 80, label: 'Passarela de Durasteel' },
      { id: 'trash_compactor', type: 'cover_heavy', shape: 'rect', x: 20, y: 40, w: 12, h: 14, label: 'Compactador de Sucata (Cobertura Pesada)', cover: 'heavy' },
      { id: 'conduit_vent', type: 'cover_light', shape: 'rect', x: 68, y: 35, w: 10, h: 10, label: 'Dutos de Ventilação Térmica (Cobertura Leve)', cover: 'light' }
    ]
  },
  desert_canyon: {
    key: 'desert_canyon',
    namePtBr: 'Desfiladeiro Rochoso & Dunas',
    archetype: 'wilderness',
    props: [
      { id: 'ridge_left', type: 'cover_heavy', shape: 'polygon', points: [[10,20],[24,40],[18,75],[8,65]], label: 'Paredão de Rocha (Cobertura Pesada)', cover: 'heavy' },
      { id: 'ridge_right', type: 'cover_heavy', shape: 'polygon', points: [[88,25],[74,50],[82,80],[92,70]], label: 'Formação Rochosa (Cobertura Pesada)', cover: 'heavy' },
      { id: 'sand_dune', type: 'cover_light', shape: 'polygon', points: [[40,55],[60,55],[55,68],[45,68]], label: 'Crista da Duna (Cobertura Leve)', cover: 'light' }
    ]
  },
  imperial_base: {
    key: 'imperial_base',
    namePtBr: 'Bunker / Instalação Imperial',
    archetype: 'military',
    props: [
      { id: 'command_pod', type: 'structure', shape: 'rect', x: 42, y: 22, w: 16, h: 14, label: 'Holoprojetor de Comando' },
      { id: 'console_left', type: 'cover_heavy', shape: 'rect', x: 18, y: 48, w: 12, h: 20, label: 'Bancos de Computadores (Cobertura Pesada)', cover: 'heavy' },
      { id: 'console_right', type: 'cover_heavy', shape: 'rect', x: 70, y: 48, w: 12, h: 20, label: 'Painéis de Força (Cobertura Pesada)', cover: 'heavy' },
      { id: 'shield_barrier', type: 'cover_light', shape: 'rect', x: 38, y: 72, w: 24, h: 4, label: 'Barreira de Deflexão Baixa (Cobertura Leve)', cover: 'light' }
    ]
  },
  jungle_wilderness: {
    key: 'jungle_wilderness',
    namePtBr: 'Selva Alienígena & Ruínas',
    archetype: 'jungle',
    props: [
      { id: 'ancient_pillar', type: 'cover_heavy', shape: 'circle', cx: 30, cy: 35, r: 9, label: 'Pilar em Ruínas (Cobertura Pesada)', cover: 'heavy' },
      { id: 'wyyyschokk_tree', type: 'cover_heavy', shape: 'circle', cx: 72, cy: 45, r: 11, label: 'Tronco de Wroshyr (Cobertura Pesada)', cover: 'heavy' },
      { id: 'dense_foliage', type: 'cover_light', shape: 'circle', cx: 50, cy: 70, r: 12, label: 'Emaranhado de Raízes (Cobertura Leve)', cover: 'light' }
    ]
  }
});

export const RANGE_BANDS = Object.freeze([
  { key: 'engaged', labelPtBr: 'Engajado', maxRadius: 20, difficulty: 1, label: '0–2m · Contato Direto' },
  { key: 'short', labelPtBr: 'Curto', maxRadius: 40, difficulty: 1, label: '3–10m · Fácil' },
  { key: 'medium', labelPtBr: 'Médio', maxRadius: 65, difficulty: 2, label: '11–30m · Médio' },
  { key: 'long', labelPtBr: 'Longo', maxRadius: 88, difficulty: 3, label: '31–100m · Difícil' },
  { key: 'extreme', labelPtBr: 'Extremo', maxRadius: 100, difficulty: 4, label: '100m+ · Formidável' }
]);

export function calculateRangeBand(x, y, cx = 50, cy = 50) {
  const dist = Math.hypot(x - cx, y - cy);
  for (const band of RANGE_BANDS) {
    if (dist <= band.maxRadius) return band.key;
  }
  return 'extreme';
}

export function detectBiomeFromContext(locationName = '', description = '') {
  const text = `${locationName} ${description}`.toLowerCase();
  if (/hangar|doca|porto espacial|baia|espaçonave|cargueiro|estação/i.test(text)) return 'hangar_station';
  if (/cantina|bar|taberna|clube|bebida|jogo|sabacc|entreposto/i.test(text)) return 'cantina_outpost';
  if (/beco|subnível|cidade|esgoto|passarela|fábrica|industrial|coruscant/i.test(text)) return 'urban_sublevel';
  if (/deserto|duna|areia|desfiladeiro|cânion|tatooine|rochoso|pedra/i.test(text)) return 'desert_canyon';
  if (/base|bunker|posto|laboratório|guarnição|imperial|instalação/i.test(text)) return 'imperial_base';
  if (/selva|floresta|ruína|pântano|kashyyyk|dathomir|árvore|planta/i.test(text)) return 'jungle_wilderness';
  return 'cantina_outpost';
}

export function createInitialHolotableState({
  biomeKey = 'cantina_outpost',
  character = null,
  adversaries = []
} = {}) {
  const biome = HOLO_BIOMES[biomeKey] || HOLO_BIOMES.cantina_outpost;

  const playerToken = {
    id: 'player_hero',
    name: character?.name || 'Seu Herói',
    type: 'player',
    x: 50,
    y: 75,
    rangeBand: 'short',
    cover: 'none',
    wounds: character?.derived?.wounds?.current || 0,
    woundsThreshold: character?.derived?.wounds?.threshold || 12,
    strain: character?.derived?.strain?.current || 0,
    strainThreshold: character?.derived?.strain?.threshold || 12,
    engagedTargetIds: []
  };

  const defaultAdv = [
    { id: 'adv_1', name: 'Atirador Mercenário', type: 'rival', x: 42, y: 32, rangeBand: 'medium', cover: 'light', wounds: 0, woundsThreshold: 10, isTarget: true },
    { id: 'adv_2', name: 'Capanga Enforcer', type: 'minion', x: 62, y: 38, rangeBand: 'medium', cover: 'none', wounds: 0, woundsThreshold: 8, isTarget: false }
  ];

  const adversaryTokens = adversaries.length
    ? adversaries.map((adv, idx) => ({
        id: adv.id || `adv_${idx + 1}`,
        name: adv.name || `Oponente ${idx + 1}`,
        type: adv.type || 'minion',
        x: adv.x ?? (35 + idx * 20),
        y: adv.y ?? (30 + (idx % 2) * 15),
        rangeBand: calculateRangeBand(adv.x ?? 35, adv.y ?? 30),
        cover: adv.cover || 'none',
        wounds: adv.wounds || 0,
        woundsThreshold: adv.woundsThreshold || 10,
        isTarget: idx === 0
      }))
    : defaultAdv;

  return {
    biomeKey: biome.key,
    biomeName: biome.namePtBr,
    radarAngle: 0,
    selectedTargetId: adversaryTokens[0]?.id || null,
    playerToken,
    adversaryTokens,
    log: []
  };
}

export function moveHolotableToken(state, tokenId, x, y) {
  const next = structuredClone(state);
  const clampedX = Math.max(8, Math.min(92, Number(x) || 50));
  const clampedY = Math.max(8, Math.min(92, Number(y) || 50));

  let targetToken = null;
  if (next.playerToken.id === tokenId) {
    targetToken = next.playerToken;
  } else {
    targetToken = next.adversaryTokens.find(t => t.id === tokenId);
  }

  if (!targetToken) throw new Error('Token tático não encontrado.');

  targetToken.x = clampedX;
  targetToken.y = clampedY;

  // Atualiza faixa de alcance em relação ao centro tático (ou relativo ao player)
  const refX = targetToken.type === 'player' ? 50 : next.playerToken.x;
  const refY = targetToken.type === 'player' ? 50 : next.playerToken.y;
  targetToken.rangeBand = calculateRangeBand(clampedX, clampedY, refX, refY);

  // Detecção de cobertura nos props do bioma
  const biome = HOLO_BIOMES[next.biomeKey] || HOLO_BIOMES.cantina_outpost;
  targetToken.cover = 'none';

  for (const prop of biome.props) {
    if (prop.cover) {
      if (prop.shape === 'rect' && clampedX >= prop.x && clampedX <= prop.x + prop.w && clampedY >= prop.y && clampedY <= prop.y + prop.h) {
        targetToken.cover = prop.cover;
        break;
      }
      if (prop.shape === 'circle' && Math.hypot(clampedX - prop.cx, clampedY - prop.cy) <= prop.r) {
        targetToken.cover = prop.cover;
        break;
      }
    }
  }

  next.log.push({
    action: 'MOVE',
    tokenId,
    name: targetToken.name,
    rangeBand: targetToken.rangeBand,
    cover: targetToken.cover
  });

  return next;
}

export function calculateTacticalCheckModifiers(state, targetId) {
  const target = state.adversaryTokens.find(t => t.id === targetId);
  if (!target) throw new Error('Alvo tático não selecionado.');

  const dist = Math.hypot(state.playerToken.x - target.x, state.playerToken.y - target.y);
  let rangeBand = 'extreme';
  let difficulty = 4;

  if (dist <= 15) { rangeBand = 'engaged'; difficulty = 1; }
  else if (dist <= 35) { rangeBand = 'short'; difficulty = 1; }
  else if (dist <= 60) { rangeBand = 'medium'; difficulty = 2; }
  else if (dist <= 85) { rangeBand = 'long'; difficulty = 3; }

  let setbackDice = 0;
  if (target.cover === 'light') setbackDice += 1;
  if (target.cover === 'heavy') setbackDice += 2;

  return {
    targetId: target.id,
    targetName: target.name,
    distancePx: Math.round(dist),
    rangeBand,
    difficulty,
    cover: target.cover,
    setbackDice
  };
}
