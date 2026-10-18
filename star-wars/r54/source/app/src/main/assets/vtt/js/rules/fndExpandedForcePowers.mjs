// Star Wars: Force and Destiny & Career Sourcebooks
// Canonical Expanded Force Powers:
// Battle Meditation, Foresee, Farsight, Alter, Conjure, Suppress, Manipulate, Imbue
// Sources: F&D Core, Chronicles of the Gatekeeper, Savage Spirits, Unlimited Power, Endless Vigil, Disciples of Harmony.

import { FND_FORCE_POWERS } from './fndForcePowers.mjs';

const source = (book, printedPage, section) => Object.freeze({
  book,
  printedPage,
  section
});

function upgrade(id, cost, type, label, parents = []) {
  return Object.freeze({
    id,
    cost,
    type,
    label,
    parents: Object.freeze(parents),
    source: source('Sourcebook Expansion', null, 'Force Power Upgrade')
  });
}

export const SUPPLEMENTAL_FORCE_POWERS = Object.freeze({
  battle_meditation: Object.freeze({
    key: 'battle_meditation',
    label: 'Meditação de Batalha (Battle Meditation)',
    baseCost: 15,
    source: source('Chronicles of the Gatekeeper', 30, 'Force Power: Battle Meditation'),
    description: 'Sincroniza as mentes dos aliados no campo de batalha, harmonizando suas ações e amplificando a eficácia do grupo inteiro.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Estende o alcance da sincronização de Curto para Médio.', []),
      upgrade('magnitude_1', 10, 'magnitude', 'Aumenta em +1 o número de aliados adicionais beneficiados pela meditação.', ['range_1']),
      upgrade('control_success', 15, 'control', 'Aliados sob o efeito adicionam 1 Sucesso automático a todos os testes de combate.', ['magnitude_1']),
      upgrade('control_advantage', 15, 'control', 'Aliados adicionam 1 Vantagem automática a todos os testes de combate e manobras.', ['control_success']),
      upgrade('mastery_fleet', 20, 'mastery', 'A meditação atinge escala planetária ou de frota naval estelar inteira.', ['control_advantage'])
    ])
  }),

  foresee: Object.freeze({
    key: 'foresee',
    label: 'Previsão (Foresee)',
    baseCost: 10,
    source: source('Force and Destiny Core Rulebook', 288, 'Force Power: Foresee'),
    description: 'Sonda o fluxo temporal imediato e futuro distante para antecipar perigos, emboscadas e desdobramentos de eventos.',
    upgrades: Object.freeze([
      upgrade('duration_1', 5, 'duration', 'Estende a janela de visão do futuro imediato para horas ou dias.', []),
      upgrade('range_1', 10, 'range', 'Detecta eventos e ameaças a distâncias planetárias ou do setor.', ['duration_1']),
      upgrade('control_initiative', 15, 'control', 'Adiciona pontos de Força gerados como Sucessos diretos no teste de Iniciativa do personagem e aliados.', ['range_1']),
      upgrade('control_ambush', 15, 'control', 'O grupo não pode ser surpreendido em emboscadas; reage imediatamente na primeira rodada.', ['control_initiative']),
      upgrade('mastery_destiny', 20, 'mastery', 'Permite prever uma reviravolta crítica da trama, concedendo 1 Ponto de Destino Luminoso adicional à reserva.', ['control_ambush'])
    ])
  }),

  farsight: Object.freeze({
    key: 'farsight',
    label: 'Clarividência (Farsight)',
    baseCost: 10,
    source: source('Savage Spirits', 36, 'Force Power: Farsight'),
    description: 'Projeta os sentidos visuais além de barreiras físicas e distâncias espaciais, enxergando no escuro absoluto e através de blindagens.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Estende a visão mística para alcance Médio e Longo.', []),
      upgrade('control_see_unseen', 10, 'control', 'Ignora camuflagem óptica, escuridão total, fumaça e invisibilidade de alvos ocultos.', ['range_1']),
      upgrade('control_scan_life', 15, 'control', 'Detecta presenças biológicas, sentimentos vitais e silhuetas através de estruturas sólidas.', ['control_see_unseen']),
      upgrade('mastery_astral', 20, 'mastery', 'Permite projetar a visão para outros planetas com os quais o usuário tenha um elo prévio.', ['control_scan_life'])
    ])
  }),

  alter: Object.freeze({
    key: 'alter',
    label: 'Alterar Ambiente (Alter)',
    baseCost: 10,
    source: source('Unlimited Power', 34, 'Force Power: Alter'),
    description: 'Transmuta o terreno natural e o ambiente circundante, criando trincheiras, tempestades de poeira ou moldando barreiras de rocha e gelo.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Permite moldar terreno até alcance Médio.', []),
      upgrade('magnitude_1', 10, 'magnitude', 'Aumenta a área de terreno alterada (raio de 10 metros adicionais por magnitude).', ['range_1']),
      upgrade('control_difficult_terrain', 10, 'control', 'Converte terreno normal em terreno difícil intransponível para oponentes.', ['magnitude_1']),
      upgrade('control_cover', 15, 'control', 'Ergue coberturas rochosas sólidas instantâneas (+2 de Defesa) para o usuário e aliados.', ['control_difficult_terrain']),
      upgrade('mastery_cataclysm', 25, 'mastery', 'Desencadeia terremotos locais, fendas no solo ou avalanches contra forças inimigas.', ['control_cover'])
    ])
  }),

  conjure: Object.freeze({
    key: 'conjure',
    label: 'Conjurar (Conjure)',
    baseCost: 15,
    source: source('Unlimited Power', 36, 'Force Power: Conjure'),
    description: 'Moldagem tangível de energia da Força para forjar armas etéreas de pura luz/sombra ou invocar espectros de proteção.',
    upgrades: Object.freeze([
      upgrade('strength_1', 5, 'strength', 'Aumenta o dano da arma conjurada em +1.', []),
      upgrade('duration_1', 10, 'duration', 'Mantém a criação mística ativa por todo o encontro sem custo adicional de concentração.', ['strength_1']),
      upgrade('control_weapon_qualities', 15, 'control', 'Concede qualidades de combate à arma conjurada (ex: Perfurante 2, Atordoamento, Defensiva 1).', ['duration_1']),
      upgrade('mastery_spectral_guardian', 25, 'mastery', 'Invoca um guardião espectral autônomo da Força que combate e defende os aliados.', ['control_weapon_qualities'])
    ])
  }),

  suppress: Object.freeze({
    key: 'suppress',
    label: 'Suprimir a Força (Suppress)',
    baseCost: 10,
    source: source('Disciples of Harmony', 34, 'Force Power: Suppress'),
    description: 'Cria um vórtice de contenção que dissipa, enfraquece e anula poderes da Força invocados por adversários sombrios.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Estende a bolha de supressão para alcance Médio.', []),
      upgrade('magnitude_1', 10, 'magnitude', 'Protege múltiplos aliados simultaneamente sob o manto de amortecimento da Força.', ['range_1']),
      upgrade('control_drain_force', 15, 'control', 'O oponente atingido deve despender 1 ponto adicional de Força para conseguir manifestar qualquer poder.', ['magnitude_1']),
      upgrade('mastery_severance', 25, 'mastery', 'Corte da Força temporário: impede totalmente o oponente de ativar poderes da Força na rodada.', ['control_drain_force'])
    ])
  }),

  manipulate: Object.freeze({
    key: 'manipulate',
    label: 'Manipulação de Máquinas (Manipulate)',
    baseCost: 10,
    source: source('Endless Vigil', 32, 'Force Power: Manipulate'),
    description: 'Interage telepaticamente com circuitos, computadores, armas de energia e motores mecânicos através da Força viva presente na matéria.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Alcança sistemas eletrônicos e veículos a alcance Médio.', []),
      upgrade('control_instant_repair', 10, 'control', 'Repara instantaneamente trauma de casco veicular ou dano mecânico sem ferramentas.', ['range_1']),
      upgrade('control_overload', 15, 'control', 'Sobrecarga circuitos de armas ou escudos inimigos, desativando-os temporariamente.', ['control_instant_repair']),
      upgrade('mastery_ghost_in_the_machine', 20, 'mastery', 'Assume controle absoluto de um droide ou sistema veicular automatizado com a mente.', ['control_overload'])
    ])
  }),

  imbue: Object.freeze({
    key: 'imbue',
    label: 'Imbuir (Imbue)',
    baseCost: 10,
    source: source('Unlimited Power', 38, 'Force Power: Imbue'),
    description: 'Transfere energia viva da Força para o corpo de um aliado, aprimorando drasticamente seus atributos biológicos ou resistências.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Estende o alcance para Curto e Médio.', []),
      upgrade('duration_1', 10, 'duration', 'Mantém o efeito no aliado durante várias rodadas consecutivas.', ['range_1']),
      upgrade('control_boost_characteristic', 15, 'control', 'Aumenta um atributo de escolha do aliado (ex: Fortaleza, Agilidade ou Presença) em +1.', ['duration_1']),
      upgrade('mastery_champion', 20, 'mastery', 'Concede ao aliado resistência massiva (+2 Soak) e imunidade a efeitos de medo e atordoamento.', ['control_boost_characteristic'])
    ])
  })
});

export const ALL_FORCE_POWERS = Object.freeze({
  ...FND_FORCE_POWERS,
  ...SUPPLEMENTAL_FORCE_POWERS
});
