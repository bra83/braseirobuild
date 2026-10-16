/**
 * Star Wars VTT — Sistema Mecânico Canônico de Implantes Cibernéticos
 * Referências Oficiais: EotE Core Rulebook p. 173-174; AoR Core Rulebook p. 187-188; Special Modifications p. 48-52.
 */

export const CYBERNETICS_CATALOG = Object.freeze({
  prosthetic_arm_basic: Object.freeze({
    id: 'prosthetic_arm_basic',
    name: 'Braço Protético Padrão',
    type: 'prosthesis',
    cost: 1000,
    encumbrance: 0,
    rarity: 2,
    description: 'Substitui um membro perdido sem conceder bônus adicionais de característica.',
    brawnCost: 1,
    modifiers: {}
  }),
  prosthetic_leg_basic: Object.freeze({
    id: 'prosthetic_leg_basic',
    name: 'Perna Protética Padrão',
    type: 'prosthesis',
    cost: 1000,
    encumbrance: 0,
    rarity: 2,
    description: 'Substitui um membro inferior perdido sem conceder bônus adicionais de característica.',
    brawnCost: 1,
    modifiers: {}
  }),
  cybernetic_arm_enhanced: Object.freeze({
    id: 'cybernetic_arm_enhanced',
    name: 'Braço Cibernético Aprimorado (+1 Fortaleza)',
    type: 'enhancement',
    cost: 10000,
    encumbrance: 0,
    rarity: 6,
    restricted: true,
    description: 'Aumenta permanentemente o valor de Fortaleza (Brawn) em +1, até o limite racial de 6.',
    brawnCost: 1,
    modifiers: { characteristicBonus: { fortaleza: 1 } }
  }),
  cybernetic_leg_enhanced: Object.freeze({
    id: 'cybernetic_leg_enhanced',
    name: 'Perna Cibernética Aprimorada (+1 Agilidade)',
    type: 'enhancement',
    cost: 10000,
    encumbrance: 0,
    rarity: 6,
    restricted: true,
    description: 'Aumenta permanentemente o valor de Agilidade em +1, até o limite racial de 6.',
    brawnCost: 1,
    modifiers: { characteristicBonus: { agilidad: 1 } }
  }),
  cybernetic_brain_implant: Object.freeze({
    id: 'cybernetic_brain_implant',
    name: 'Implante Cerebral Biocibernético (+1 Intelecto)',
    type: 'enhancement',
    cost: 10000,
    encumbrance: 0,
    rarity: 7,
    restricted: true,
    description: 'Co-processador neural que eleva o Intelecto em +1.',
    brawnCost: 1,
    modifiers: { characteristicBonus: { intelecto: 1 } }
  }),
  cybernetic_subdermal_armor: Object.freeze({
    id: 'cybernetic_subdermal_armor',
    name: 'Armadura Subdérmica Cibernética (+1 Absorção)',
    type: 'enhancement',
    cost: 3000,
    encumbrance: 0,
    rarity: 4,
    description: 'Placas sintéticas implantadas sob a derme conferem +1 ao Soak (Absorção).',
    brawnCost: 1,
    modifiers: { soakBonus: 1 }
  }),
  cybernetic_eyes: Object.freeze({
    id: 'cybernetic_eyes',
    name: 'Olhos Cibernéticos com Visão Térmica/Noturna',
    type: 'enhancement',
    cost: 1500,
    encumbrance: 0,
    rarity: 3,
    description: 'Elimina até 2 dados de Revés causados por escuridão ou fumaça.',
    brawnCost: 1,
    modifiers: { removeDarknessSetback: 2, skillBonus: { percepcion: 1 } }
  }),
  cybernetic_respirator: Object.freeze({
    id: 'cybernetic_respirator',
    name: 'Respirador Pulmonar Sintético',
    type: 'enhancement',
    cost: 2000,
    encumbrance: 0,
    rarity: 4,
    description: 'Imunidade total a toxinas gasosas e permite operar no vácuo por 10 minutos.',
    brawnCost: 1,
    modifiers: { toxinImmunity: true, vacuumBreathMinutes: 10 }
  })
});

/**
 * Calcula o limite máximo de implantes cibernéticos suportados pelo personagem.
 * Regra: Igual ao valor atual de Fortaleza (Brawn).
 */
export function cyberneticsCap(character) {
  if (!character) return 0;
  const brawn = Number(character.characteristics?.fortaleza || 1);
  const extraCavities = Number(character.talents?.filter(t => t.id === 'cybernetic_cavity')?.length || 0);
  return Math.max(1, brawn + extraCavities);
}

/**
 * Retorna os implantes atuais do personagem.
 */
export function installedCybernetics(character) {
  return Array.isArray(character?.cybernetics) ? character.cybernetics : [];
}

/**
 * Quantidade de implantes que consom o limite de Brawn instalados no personagem.
 */
export function totalCyberneticsCost(character) {
  const list = installedCybernetics(character);
  return list.reduce((acc, item) => acc + (Number(item.brawnCost) || 1), 0);
}

/**
 * Instala um implante cibernético no personagem respeitando o limite mecânico.
 */
export function installCybernetic(character, cyberneticId, { ownedAssetId = null } = {}) {
  if (!character) throw new Error('Personagem inválido.');
  const item = CYBERNETICS_CATALOG[cyberneticId];
  if (!item) throw new Error(`Implante cibernético desconhecido: ${cyberneticId}`);

  const current = installedCybernetics(character);
  const cap = cyberneticsCap(character);
  const currentCost = totalCyberneticsCost(character);
  const nextCost = currentCost + (item.brawnCost || 1);

  if (nextCost > cap) {
    throw new Error(`Limite de implantes cibernéticos excedido (Máx: ${cap}, Atual: ${currentCost}, Requer: ${item.brawnCost || 1}).`);
  }

  const next = structuredClone(character);
  next.cybernetics = [...current, { ...item, ownedAssetId: ownedAssetId ? String(ownedAssetId) : null, installedAt: new Date().toISOString() }];

  // Aplicar modificadores de características ou absorção
  if (item.modifiers.characteristicBonus) {
    for (const [charKey, bonus] of Object.entries(item.modifiers.characteristicBonus)) {
      if (next.characteristics[charKey] !== undefined) {
        next.characteristics[charKey] = Math.min(6, next.characteristics[charKey] + bonus);
      }
    }
  }

  if (item.modifiers.soakBonus) {
    next.derived = next.derived || {};
    next.derived.soak = (Number(next.derived.soak) || Number(next.characteristics.fortaleza)) + item.modifiers.soakBonus;
  }

  return next;
}

/**
 * Remove um implante cibernético do personagem.
 */
export function removeCybernetic(character, cyberneticId) {
  if (!character) throw new Error('Personagem inválido.');
  const list = installedCybernetics(character);
  const idx = list.findIndex(c => c.id === cyberneticId);
  if (idx === -1) throw new Error('Implante não encontrado no personagem.');

  const item = list[idx];
  const next = structuredClone(character);
  next.cybernetics = list.filter((_, i) => i !== idx);

  // Reverter bônus
  if (item.modifiers?.characteristicBonus) {
    for (const [charKey, bonus] of Object.entries(item.modifiers.characteristicBonus)) {
      if (next.characteristics[charKey] !== undefined) {
        next.characteristics[charKey] = Math.max(1, next.characteristics[charKey] - bonus);
      }
    }
  }

  if (item.modifiers?.soakBonus) {
    next.derived = next.derived || {};
    next.derived.soak = Math.max(0, (Number(next.derived.soak) || 0) - item.modifiers.soakBonus);
  }

  return next;
}
