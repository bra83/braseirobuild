// Star Wars: Force and Destiny — Core Rulebook
// Chapter VIII: The Force — Canonical Force Powers (pp. 280-315)
// Enhances the core power registry with the six classic F&D powers:
// Enhance, Heal/Harm, Bind, Misdirect, Protect/Unleash, and Seek.

const source = (printedPage, section) => Object.freeze({
  book: 'Star Wars: Force and Destiny Core Rulebook',
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
    source: source(null, 'Force Power Upgrade')
  });
}

export const FND_FORCE_POWERS = Object.freeze({
  enhance: Object.freeze({
    key: 'enhance',
    label: 'Aprimorar (Enhance)',
    baseCost: 10,
    source: source(286, 'Force Power: Enhance'),
    description: 'Canaliza a Força no próprio corpo para ampliar capacidades atléticas, agilidade e força física.',
    upgrades: Object.freeze([
      upgrade('control_jump_horizontal', 5, 'control', 'Realiza saltos horizontais como parte do movimento gastando pontos da Força.', []),
      upgrade('control_jump_vertical', 5, 'control', 'Realiza saltos verticais extraordinários gastando pontos da Força.', ['control_jump_horizontal']),
      upgrade('range_jump', 10, 'range', 'Aumenta a distância máxima alcançada pelo Salto da Força em uma faixa de alcance.', ['control_jump_vertical']),
      upgrade('control_brawn', 20, 'control', 'Comprometer 1 dado da Força: aumenta o atributo Fortaleza (Brawn) em +1 enquanto mantido.', ['range_jump']),
      upgrade('control_agility', 20, 'control', 'Comprometer 1 dado da Força: aumenta o atributo Agilidade (Agility) em +1 enquanto mantido.', ['control_brawn'])
    ])
  }),

  heal_harm: Object.freeze({
    key: 'heal_harm',
    label: 'Curar / Ferir (Heal / Harm)',
    baseCost: 10,
    source: source(288, 'Force Power: Heal / Harm'),
    description: 'Manipula a energia vital para regenerar tecidos celulares feridos ou drenar a vida de seres vivos.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Aumenta o alcance de engajado para curto.', []),
      upgrade('strength_1', 10, 'strength', 'Aumenta em +1 a quantidade de Ferimentos curados ou infligidos.', ['range_1']),
      upgrade('magnitude_1', 10, 'magnitude', 'Afeta um alvo adicional por ponto gasto.', ['strength_1']),
      upgrade('control_strain', 10, 'control', 'Permite curar ou infligir Tensão em vez de Ferimentos.', ['magnitude_1']),
      upgrade('control_revive', 15, 'control', 'Estabiliza um alvo mortalmente ferido ou restaura a consciência de um aliado incapacitado.', ['control_strain'])
    ])
  }),

  bind: Object.freeze({
    key: 'bind',
    label: 'Conter (Bind)',
    baseCost: 10,
    source: source(292, 'Force Power: Bind'),
    description: 'Imobiliza e aprisiona oponentes suspendendo-os ou constringindo seus movimentos com a Força.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Aumenta o alcance para Médio.', []),
      upgrade('duration_1', 5, 'duration', 'Mantém o efeito por rodadas adicionais sem nova ativação.', ['range_1']),
      upgrade('magnitude_1', 10, 'magnitude', 'Prende alvos adicionais simultaneamente.', ['duration_1']),
      upgrade('control_suffocate', 15, 'control', 'Sufocamento da Força: inflige Ferimentos diretos contínuos que ignoram Soak a cada rodada mantida.', ['magnitude_1']),
      upgrade('control_crush', 20, 'control', 'Esmagamento: causa dano massivo a estruturas ou alvos imobilizados.', ['control_suffocate'])
    ])
  }),

  misdirect: Object.freeze({
    key: 'misdirect',
    label: 'Enganar (Misdirect)',
    baseCost: 10,
    source: source(296, 'Force Power: Misdirect'),
    description: 'Altera os sentidos de observadores criando ilusões auditivas e visuais ou ocultando presenças.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Aumenta o alcance da ilusão para Curto.', []),
      upgrade('magnitude_1', 5, 'magnitude', 'Engana mais observadores simultaneamente.', ['range_1']),
      upgrade('duration_1', 10, 'duration', 'A ilusão persiste mesmo após o usuário desviar a atenção.', ['magnitude_1']),
      upgrade('control_invisibility', 15, 'control', 'Invisibilidade: torna o usuário e aliados completamente invisíveis à visão comum.', ['duration_1'])
    ])
  }),

  protect_unleash: Object.freeze({
    key: 'protect_unleash',
    label: 'Proteger / Libertar (Protect / Unleash)',
    baseCost: 15,
    source: source(300, 'Force Power: Protect / Unleash'),
    description: 'Ergue uma barreira de força pura contra ataques destrutivos ou libera relâmpagos devastadores da Força.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Aumenta o alcance do campo ou rajada para Médio.', []),
      upgrade('strength_1', 10, 'strength', 'Aumenta a absorção de dano em +2 ou dano do raio em +2.', ['range_1']),
      upgrade('magnitude_1', 10, 'magnitude', 'Protege múltiplos aliados ou eletrocuta múltiplos oponentes em arco.', ['strength_1']),
      upgrade('control_storm', 20, 'control', 'Tempestade da Força: descarrega energia pura com qualidade Crítico 2 e Desorientar.', ['magnitude_1'])
    ])
  }),

  seek: Object.freeze({
    key: 'seek',
    label: 'Buscar (Seek)',
    baseCost: 10,
    source: source(304, 'Force Power: Seek'),
    description: 'Expande a percepção para rastrear alvos distantes, vislumbrar a verdade e encontrar pontos fracos.',
    upgrades: Object.freeze([
      upgrade('range_1', 5, 'range', 'Aumenta o alcance de detecção para faixas planetárias e orbitais.', []),
      upgrade('control_sight', 10, 'control', 'Visão na Força: ignora completamente penalidades por escuridão total, fumaça ou camuflagem.', ['range_1']),
      upgrade('control_weakness', 15, 'control', 'Identifica vulnerabilidades críticas na armadura ou mente do oponente, adicionando sucesso aos ataques.', ['control_sight'])
    ])
  })
});

export function allCanonicalForcePowers() {
  return FND_FORCE_POWERS;
}
