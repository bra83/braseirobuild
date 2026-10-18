import { newId } from '../core/ids.mjs';
import { validateAdventureModule } from '../adventure/adventureSchema.mjs';
import { validateAdventureSemantics } from '../adventure/adventureSemanticGate.mjs';
import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';

const clone = v => structuredClone(v);
const now = () => new Date().toISOString();

function hash(s) {
  let h = 2166136261;
  for (const c of String(s)) {
    h ^= c.charCodeAt(0);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export function validateOriginalAdventure(module) {
  validateAdventureModule(module);
  validateAdventureSemantics(module);
  return true;
}

export function compileSceneGraphToUaeModule(spec, world) {
  const adventureId = String(spec.id || newId('adv_orig')).trim();
  const title = String(spec.title || 'Operação nas Sombras da Galáxia').trim();
  const campaignModes = Array.isArray(spec.campaignModes) && spec.campaignModes.length ? spec.campaignModes : ['EOTE', 'AOR', 'MIXED'];
  const rulesProfile = spec.rulesProfile || 'MIXED';

  const nodes = {};
  for (const [nodeId, n] of Object.entries(spec.nodes || {})) {
    nodes[nodeId] = {
      id: nodeId,
      episode: Number(n.episode) || 1,
      title: String(n.title || nodeId),
      locationId: n.locationId || null,
      summary: String(n.summary || ''),
      narration: String(n.narration || ''),
      visibleNpcIds: Array.isArray(n.visibleNpcIds) ? [...n.visibleNpcIds] : [],
      checks: Array.isArray(n.checks) ? clone(n.checks) : [],
      choices: (Array.isArray(n.choices) ? n.choices : []).map(c => ({
        id: String(c.id),
        label: String(c.label),
        to: String(c.to),
        ...(c.condition ? { condition: String(c.condition) } : {}),
        ...(c.effects ? { effects: clone(c.effects) } : {})
      }))
    };
  }

  const module = {
    id: adventureId,
    title,
    campaignModes: Object.freeze(campaignModes),
    rulesProfile,
    startNodeId: spec.startNodeId || Object.keys(nodes)[0] || 'ep1-briefing',
    initialClocks: Object.freeze({
      alertClock: { value: 0, max: 6, status: 'ACTIVE', visibleToPlayer: true },
      momentum: { value: 0, max: 6, status: 'ACTIVE', visibleToPlayer: true },
      ...(spec.initialClocks || {})
    }),
    initialObjectives: Object.freeze({
      'episode-1': { title: 'Concluir o contato inicial e reunir inteligência', type: 'PRIMARY', target: 1 },
      'episode-2': { title: 'Penetrar o perímetro do alvo', type: 'PRIMARY', target: 1 },
      'episode-3': { title: 'Decidir o destino da operação e escapar', type: 'PRIMARY', target: 1 },
      'adventure-finale': { title: 'Concluir a aventura original', type: 'PRIMARY', target: 1 },
      ...(spec.initialObjectives || {})
    }),
    triggers: [
      {
        id: 'trg-ep1-enter-ep2',
        event: 'NODE_ENTER',
        condition: 'payload:to:ep2-stealth-approach',
        maxFires: 1,
        effects: [{ type: 'complete-objective', objectiveId: 'episode-1' }]
      },
      {
        id: 'trg-ep1-enter-ep2-direct',
        event: 'NODE_ENTER',
        condition: 'payload:to:ep2-direct-approach',
        maxFires: 1,
        effects: [{ type: 'complete-objective', objectiveId: 'episode-1' }]
      },
      {
        id: 'trg-ep2-enter-ep3-sabotage',
        event: 'NODE_ENTER',
        condition: 'payload:to:ep3-climax-sabotage',
        maxFires: 1,
        effects: [{ type: 'complete-objective', objectiveId: 'episode-2' }]
      },
      {
        id: 'trg-ep2-enter-ep3-confront',
        event: 'NODE_ENTER',
        condition: 'payload:to:ep3-climax-confrontation',
        maxFires: 1,
        effects: [{ type: 'complete-objective', objectiveId: 'episode-2' }]
      },
      {
        id: 'trg-finale-victory',
        event: 'NODE_ENTER',
        condition: 'payload:to:epilogue-victory',
        maxFires: 1,
        effects: [
          { type: 'complete-objective', objectiveId: 'episode-3' },
          { type: 'complete-objective', objectiveId: 'adventure-finale' }
        ]
      },
      {
        id: 'trg-finale-compromise',
        event: 'NODE_ENTER',
        condition: 'payload:to:epilogue-compromise',
        maxFires: 1,
        effects: [
          { type: 'complete-objective', objectiveId: 'episode-3' },
          { type: 'complete-objective', objectiveId: 'adventure-finale' }
        ]
      },
      ...(spec.triggers || [])
    ],
    nodes: Object.freeze(nodes)
  };

  validateOriginalAdventure(module);
  return module;
}

export function createOriginalAdventure(world, {
  prompt = '',
  campaignMode = 'MIXED',
  preferredPatronNpcId = null,
  preferredLocationId = null,
  adventureId = null
} = {}) {
  const w = ensureLivingWorldDepth(world);
  const npcs = Object.values(w.npcs || {}).filter(n => {
    const s = w.livingDepth?.npcs?.[n.id];
    return !s || s.lifeState === 'alive';
  });
  const locations = Object.values(w.locations || {});
  const factions = Object.values(w.factions || {});

  // Grounding entities
  const patron = (preferredPatronNpcId && w.npcs?.[preferredPatronNpcId]) ||
    npcs.find(n => n.relation === 'ally' || n.relation === 'friendly') ||
    npcs[0] ||
    { id: 'npc_patron_orig', name: 'Capitão Jaro Vex', occupation: 'navegador veterano' };

  const startLoc = (preferredLocationId && w.locations?.[preferredLocationId]) ||
    (patron.locationId && w.locations?.[patron.locationId]) ||
    locations[0] ||
    { id: 'loc_start_orig', name: 'Entreposto da Orla Exterior' };

  const targetLoc = locations.find(l => l.id !== startLoc.id) ||
    { id: 'loc_target_orig', name: 'Instalação Abandonada no Hiperespaço' };

  const faction = (patron.factionId && w.factions?.[patron.factionId]) ||
    factions[0] ||
    { id: 'fac_orig', name: 'Sindicato Independente' };

  const seed = hash(prompt || adventureId || now());
  const themes = [
    { title: 'O Código Fantasma de Kuat', prize: 'esquemas técnicos confidenciais', antagonist: 'agente de segurança corporativa' },
    { title: 'Conspiração nas Docas Negras', prize: 'carregamento de coaxium refinado', antagonist: 'sindicalista corrupto' },
    { title: 'Resgate no Setor Trax', prize: 'diplomata da Aliança mantido refém', antagonist: 'caçador de recompensas Trandoshano' },
    { title: 'A Carga de Kessel', prize: 'coordenadas de uma rota hiperespacial oculta', antagonist: 'patrulha imperial insubordinada' }
  ];
  const theme = themes[seed % themes.length];

  const spec = {
    id: adventureId || `orig_${seed.toString(36)}`,
    title: theme.title,
    campaignModes: ['EOTE', 'AOR', 'MIXED'],
    rulesProfile: campaignMode === 'AOR' ? 'AOR_CORE' : campaignMode === 'EOTE' ? 'EOTE_CORE' : 'MIXED',
    startNodeId: 'ep1-briefing',
    nodes: {
      'ep1-briefing': {
        episode: 1,
        title: `Episódio 1: Encontro em ${startLoc.name}`,
        locationId: startLoc.id,
        summary: `${patron.name} apresenta a oportunidade de recuperar ${theme.prize} antes que caia em mãos erradas.`,
        narration: `A fumaça e as conversas abafadas de ${startLoc.name} oferecem cobertura suficiente para a reunião. ${patron.name} desliza um datapad criptografado sobre a mesa. "A oportunidade é única", afirma com voz cautelosa. "Temos registros confirmando a localização de ${theme.prize}. Mas o tempo é curto e já há patrulhas vasculhando o setor."`,
        visibleNpcIds: [patron.id],
        checks: [
          { skill: 'Negociação', difficulty: 'Média', purpose: 'exigir adiantamento ou garantia adicional de transporte' },
          { skill: 'Frieza', difficulty: 'Fácil', purpose: 'perceber espiões nas mesas vizinhas' }
        ],
        choices: [
          {
            id: 'ep1-choice-stealth',
            label: 'Aceitar o contrato e preparar aproximação furtiva pelos dutos de manutenção',
            to: 'ep2-stealth-approach',
            effects: { approach: 'stealth', preparedIntel: true }
          },
          {
            id: 'ep1-choice-direct',
            label: 'Aceitar o contrato e mover-se velozmente antes que a concorrência se organize',
            to: 'ep2-direct-approach',
            effects: { approach: 'direct', speedAdvantage: true }
          }
        ]
      },
      'ep2-stealth-approach': {
        episode: 2,
        title: `Episódio 2: Infiltração Silenciosa em ${targetLoc.name}`,
        locationId: targetLoc.id,
        summary: 'A equipe contorna as defesas externas através dos conduítes industriais e hackeia o sistema central.',
        narration: `O cheiro de ozônio e ferrugem domina os corredores silenciosos de ${targetLoc.name}. Câmeras de vigilância giram em ciclos automatizados. Nos terminais à frente, os registros protegidos de ${theme.prize} aguardam decodificação, mas os alarmes estão engatilhados.`,
        visibleNpcIds: [],
        checks: [
          { skill: 'Sigilo', difficulty: 'Média', purpose: 'mover-se pelos pontos cegos dos sensores' },
          { skill: 'Computadores', difficulty: 'Média', purpose: 'abrir a câmara do objetivo sem disparar alarme geral' }
        ],
        choices: [
          {
            id: 'ep2-stealth-to-sabotage',
            label: 'Sobrecaregar os geradores para apagar os rastros eletrônicos da extração',
            to: 'ep3-climax-sabotage',
            effects: { stealthExtraction: true }
          },
          {
            id: 'ep2-stealth-to-confront',
            label: 'Descer à câmara principal onde o opositor comanda a transferência da carga',
            to: 'ep3-climax-confrontation',
            effects: { ambushPosition: true }
          }
        ]
      },
      'ep2-direct-approach': {
        episode: 2,
        title: `Episódio 2: Incursão Frontal em ${targetLoc.name}`,
        locationId: targetLoc.id,
        summary: 'Avanço audacioso através do hangar de carga; o grupo precisa forçar a passagem sob fogo cruzado.',
        narration: `O rangido de metal e o clarão de disparos de blaster iluminam o perímetro de ${targetLoc.name}. O grupo rompe a barricada externa antes que as portas de isolamento possam ser completamente trancadas.`,
        visibleNpcIds: [],
        checks: [
          { skill: 'Vigilância', difficulty: 'Média', purpose: 'identificar a cobertura mais vantajosa sob fogo' },
          { skill: 'Atletismo', difficulty: 'Média', purpose: 'saltar sobre a esteira de triagem industrial' }
        ],
        choices: [
          {
            id: 'ep2-direct-to-confront',
            label: 'Encurralar o líder dos adversários antes que a nave de fuga decole',
            to: 'ep3-climax-confrontation',
            effects: { enemyTrapped: true }
          },
          {
            id: 'ep2-direct-to-sabotage',
            label: 'Cortar a linha de energia da plataforma de pouso',
            to: 'ep3-climax-sabotage',
            effects: { powerCut: true }
          }
        ]
      },
      'ep3-climax-sabotage': {
        episode: 3,
        title: 'Episódio 3: Sabotagem e Fuga no Clímax',
        locationId: targetLoc.id,
        summary: 'Desafio técnico e contra-relógio: estabilizar a fuga enquanto a instalação sofre colapso controlado.',
        narration: `Alarmes estridentes ecoam pelo complexo enquanto luzes vermelhas de emergência piscam sem parar. O feixe de contenção que prende ${theme.prize} começa a sobreaquecer. O grupo precisa agir em perfeita sincronia para selar a retirada.`,
        visibleNpcIds: [],
        checks: [
          { skill: 'Mecânica', difficulty: 'Média', purpose: 'destravar a cápsula da carga sem danificar o conteúdo' },
          { skill: 'Disciplina', difficulty: 'Média', purpose: 'manter o foco sob o ruído ensurdecedor do colapso' }
        ],
        choices: [
          {
            id: 'ep3-sabotage-victory',
            label: 'Garantir a carga intacta e saltar para o hiperespaço em segurança',
            to: 'epilogue-victory',
            effects: { outcome: 'victory', prizeIntact: true }
          },
          {
            id: 'ep3-sabotage-compromise',
            label: 'Abandonar metade dos componentes auxiliares para garantir a sobrevivência de todos',
            to: 'epilogue-compromise',
            effects: { outcome: 'compromise', prizePartial: true }
          }
        ]
      },
      'ep3-climax-confrontation': {
        episode: 3,
        title: 'Episódio 3: Duelo Decisivo no Hangar',
        locationId: targetLoc.id,
        summary: 'O confronto frente a frente contra o opositor e seus guardas pelo controle definitivo do setor.',
        narration: `No hangar cavernoso, o opositor recarrega seu rifle pesado atrás de um caixote blindado. "Vocês vieram de muito longe para morrer aqui!", grita pelo alto-falante. A rampa da nave de transporte começa a fechar.`,
        visibleNpcIds: [],
        checks: [
          { skill: 'Armas de Fogo (Leves)', difficulty: 'Média', purpose: 'suprimir o ninho de atiradores' },
          { skill: 'Liderança', difficulty: 'Média', purpose: 'coordenar o ataque em pinça da equipe' }
        ],
        choices: [
          {
            id: 'ep3-confront-victory',
            label: 'Desarmar os opositores, tomar a rampa e render os sobreviventes',
            to: 'epilogue-victory',
            effects: { outcome: 'victory', enemyNeutralized: true }
          },
          {
            id: 'ep3-confront-compromise',
            label: 'Forçar um cessar-fogo tenso e negociar a retirada com o prêmio principal',
            to: 'epilogue-compromise',
            effects: { outcome: 'compromise', truceBrokered: true }
          }
        ]
      },
      'epilogue-victory': {
        episode: 3,
        title: 'Epílogo: Triunfo e Aliança Consolidada',
        locationId: startLoc.id,
        summary: 'A missão é coroada de êxito total; o patrono cumpre as promessas e a reputação do grupo cresce.',
        narration: `De volta à segurança de ${startLoc.name}, ${patron.name} inspeciona ${theme.prize} com expressão de alívio e admiração. Os créditos acordados são transferidos pontualmente, e o suporte de ${faction.name} à tripulação está assegurado para os próximos desafios da galáxia.`,
        visibleNpcIds: [patron.id],
        checks: [],
        choices: []
      },
      'epilogue-compromise': {
        episode: 3,
        title: 'Epílogo: Retirada Custosa e Sobrevivência',
        locationId: startLoc.id,
        summary: 'O grupo sobrevive e preserva a inteligência crítica, embora cicatrizes e tensões permaneçam.',
        narration: `Com a nave danificada e os motores fumegantes, a equipe retorna a ${startLoc.name}. O objetivo principal foi conquistado e vidas foram salvas, mas o custo foi alto. ${patron.name} reconhece a coragem da equipe, embora alerte que novos inimigos foram feitos no processo.`,
        visibleNpcIds: [patron.id],
        checks: [],
        choices: []
      }
    }
  };

  return compileSceneGraphToUaeModule(spec, w);
}

export function registerOriginalAdventureInWorld(world, module) {
  validateOriginalAdventure(module);
  const next = clone(world);
  next.customAdventures = next.customAdventures || {};
  next.customAdventures[module.id] = clone(module);
  return next;
}
