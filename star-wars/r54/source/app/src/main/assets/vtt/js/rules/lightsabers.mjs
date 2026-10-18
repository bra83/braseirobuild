// Star Wars: Force and Destiny — Core Rulebook
// Chapter V: Gear and Equipment — Lightsabers & Kyber Crystals (pp. 175-182)
// Canonical implementation of Lightsaber hilts, Kyber Crystals, attachment mods, and ignition under water.

const clone = v => structuredClone(v);
const int = (v, label, { min = 0, max = 9999 } = {}) => {
  const n = Number(v);
  if (!Number.isInteger(n) || n < min || n > max) throw new Error(`${label} inválido.`);
  return n;
};

export const LIGHTSABER_CHASSIS_CATALOG = Object.freeze({
  basic_lightsaber: Object.freeze({
    id: 'basic_lightsaber',
    name: 'Sabre de Luz Básico',
    skill: 'Lightsaber',
    hardPoints: 2,
    encumbrance: 1,
    rarity: 10,
    price: 9000,
    restricted: true,
    hands: 1,
    defaultCrystal: 'ilum_crystal',
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 176 }
  }),
  double_bladed_lightsaber: Object.freeze({
    id: 'double_bladed_lightsaber',
    name: 'Sabre de Luz de Lâmina Dupla',
    skill: 'Lightsaber',
    hardPoints: 4,
    encumbrance: 2,
    rarity: 10,
    price: 18000,
    restricted: true,
    hands: 2,
    defaultCrystal: 'ilum_crystal',
    baseQualities: Object.freeze(['Desajeitada 3', 'Acoplada 1']),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 176 }
  }),
  shoto_lightsaber: Object.freeze({
    id: 'shoto_lightsaber',
    name: 'Sabre Shoto',
    skill: 'Lightsaber',
    hardPoints: 2,
    encumbrance: 1,
    rarity: 10,
    price: 8000,
    restricted: true,
    hands: 1,
    defaultCrystal: 'ilum_crystal',
    baseQualities: Object.freeze(['Precisa 1', 'Defensiva 1']),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 177 }
  }),
  guard_shoto: Object.freeze({
    id: 'guard_shoto',
    name: 'Guarda-Shoto',
    skill: 'Lightsaber',
    hardPoints: 2,
    encumbrance: 1,
    rarity: 10,
    price: 8400,
    restricted: true,
    hands: 1,
    defaultCrystal: 'ilum_crystal',
    baseQualities: Object.freeze(['Defensiva 1', 'Deflexão 2']),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 177 }
  }),
  temple_guard_pike: Object.freeze({
    id: 'temple_guard_pike',
    name: 'Pique de Sabre da Guarda do Templo',
    skill: 'Lightsaber',
    hardPoints: 3,
    encumbrance: 3,
    rarity: 10,
    price: 15000,
    restricted: true,
    hands: 2,
    defaultCrystal: 'ilum_crystal',
    baseQualities: Object.freeze(['Defensiva 1', 'Incômoda 3']),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 177 }
  }),
  training_lightsaber: Object.freeze({
    id: 'training_lightsaber',
    name: 'Sabre de Treino',
    skill: 'Lightsaber',
    hardPoints: 0,
    encumbrance: 1,
    rarity: 4,
    price: 400,
    restricted: false,
    hands: 1,
    damage: 6,
    critical: null,
    qualities: Object.freeze(['Dano Atordoante']),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 176 }
  })
});

export const KYBER_CRYSTAL_CATALOG = Object.freeze({
  ilum_crystal: Object.freeze({
    id: 'ilum_crystal',
    name: 'Cristal de Ilum',
    baseDamage: 6,
    baseCritical: 2,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar']),
    hardPointsRequired: 2,
    rarity: 7,
    price: 9000,
    mods: Object.freeze([
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 4, type: 'damage', delta: 1 }),
      Object.freeze({ id: 'crit_1', name: 'Crítico -1', max: 2, type: 'critical', delta: -1 }),
      Object.freeze({ id: 'vicious_1', name: 'Cruel 1', max: 1, type: 'quality', quality: 'Cruel 1' })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 178 }
  }),
  dantooine_crystal: Object.freeze({
    id: 'dantooine_crystal',
    name: 'Cristal de Dantooine',
    baseDamage: 5,
    baseCritical: 3,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar']),
    hardPointsRequired: 2,
    rarity: 8,
    price: 10500,
    mods: Object.freeze([
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 3, type: 'damage', delta: 1 }),
      Object.freeze({ id: 'crit_1', name: 'Crítico -1', max: 1, type: 'critical', delta: -1 }),
      Object.freeze({ id: 'force_rating_1', name: 'Afinidade com a Força +1', max: 1, type: 'force_rating', delta: 1 })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 179 }
  }),
  lorrdian_gemstone: Object.freeze({
    id: 'lorrdian_gemstone',
    name: 'Gema Lorrdiana',
    baseDamage: 5,
    baseCritical: 2,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar', 'Defensiva 1', 'Deflexão 2']),
    hardPointsRequired: 2,
    rarity: 7,
    price: 8500,
    mods: Object.freeze([
      Object.freeze({ id: 'defensive_1', name: 'Defensiva +1', max: 2, type: 'quality', quality: 'Defensiva +1' }),
      Object.freeze({ id: 'deflection_1', name: 'Deflexão +1', max: 2, type: 'quality', quality: 'Deflexão +1' }),
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 2, type: 'damage', delta: 1 })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 180 }
  }),
  krayt_dragon_pearl: Object.freeze({
    id: 'krayt_dragon_pearl',
    name: 'Pérola de Dragão Krayt',
    baseDamage: 9,
    baseCritical: 1,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar', 'Cruel 1']),
    hardPointsRequired: 2,
    rarity: 12,
    price: 25000,
    mods: Object.freeze([
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 2, type: 'damage', delta: 1 }),
      Object.freeze({ id: 'vicious_1', name: 'Cruel +1', max: 2, type: 'quality', quality: 'Cruel +1' })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 181 }
  }),
  barab_ingot: Object.freeze({
    id: 'barab_ingot',
    name: 'Lingote de Barab',
    baseDamage: 7,
    baseCritical: 2,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar', 'Queimadura 1']),
    hardPointsRequired: 2,
    rarity: 8,
    price: 11000,
    mods: Object.freeze([
      Object.freeze({ id: 'burn_1', name: 'Queimadura +1', max: 2, type: 'quality', quality: 'Queimadura +1' }),
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 2, type: 'damage', delta: 1 })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 182 }
  }),
  solari_crystal: Object.freeze({
    id: 'solari_crystal',
    name: 'Cristal Solari',
    baseDamage: 8,
    baseCritical: 2,
    baseQualities: Object.freeze(['Ruptura 1', 'Destroçar', 'Defensiva 1']),
    requiresLightSide: true,
    hardPointsRequired: 2,
    rarity: 9,
    price: 14000,
    mods: Object.freeze([
      Object.freeze({ id: 'damage_1', name: 'Dano +1', max: 2, type: 'damage', delta: 1 }),
      Object.freeze({ id: 'defensive_1', name: 'Defensiva +1', max: 1, type: 'quality', quality: 'Defensiva +1' })
    ]),
    source: { book: 'Star Wars: Force and Destiny Core Rulebook', printedPage: 182 }
  })
});

export const LIGHTSABER_HILT_ATTACHMENTS = Object.freeze({
  curved_hilt: Object.freeze({
    id: 'curved_hilt',
    name: 'Empunhadura Curva',
    hardPointsRequired: 1,
    price: 1500,
    rarity: 5,
    description: 'Concede Precisa 1 em duelos individuais contra adversários armados com sabre de luz ou corpo-a-corpo.',
    effects: Object.freeze({ grantedQualities: Object.freeze(['Precisa 1']), lightsaberDuelBonus: true })
  }),
  bifurcating_pulse: Object.freeze({
    id: 'bifurcating_pulse',
    name: 'Pulso de Ignição Cíclico Bifurcado',
    hardPointsRequired: 1,
    price: 500,
    rarity: 6,
    description: 'Permite que o sabre de luz seja ativado e opere perfeitamente submerso na água sem curto-circuito.',
    effects: Object.freeze({ worksUnderwater: true })
  }),
  extended_hilt: Object.freeze({
    id: 'extended_hilt',
    name: 'Empunhadura Estendida',
    hardPointsRequired: 1,
    price: 750,
    rarity: 4,
    description: 'Alavancagem extra com duas mãos; concede Incômoda 3 e Cruel 1.',
    effects: Object.freeze({ grantedQualities: Object.freeze(['Incômoda 3', 'Cruel 1']) })
  })
});

export function createLightsaber(chassisId, { crystalId = null, attachments = [], installedMods = [] } = {}) {
  const chassis = LIGHTSABER_CHASSIS_CATALOG[chassisId];
  if (!chassis) throw new Error(`Chassi de sabre de luz desconhecido: ${chassisId}`);

  if (chassisId === 'training_lightsaber') {
    return {
      chassisId,
      name: chassis.name,
      skill: chassis.skill,
      damage: chassis.damage,
      critical: chassis.critical,
      qualities: [...chassis.qualities],
      hardPointsRemaining: 0,
      crystal: null,
      attachments: []
    };
  }

  const selectedCrystalId = crystalId ?? chassis.defaultCrystal;
  const crystal = KYBER_CRYSTAL_CATALOG[selectedCrystalId];
  if (!crystal) throw new Error(`Cristal Kyber desconhecido: ${selectedCrystalId}`);

  let usedHardPoints = crystal.hardPointsRequired;
  const validAttachments = [];
  for (const attId of attachments) {
    const att = LIGHTSABER_HILT_ATTACHMENTS[attId];
    if (att) {
      if (usedHardPoints + att.hardPointsRequired <= chassis.hardPoints) {
        usedHardPoints += att.hardPointsRequired;
        validAttachments.push(att);
      } else {
        throw new Error(`Pontos de customização (Hard Points) insuficientes para ${att.name}.`);
      }
    }
  }

  // Calculate base profile from crystal
  let damage = crystal.baseDamage;
  let critical = crystal.baseCritical;
  const qualitiesSet = new Set([...(chassis.baseQualities ?? []), ...(crystal.baseQualities ?? [])]);

  // Apply installed crystal mods
  for (const modId of installedMods) {
    const modDef = crystal.mods.find(m => m.id === modId);
    if (modDef) {
      if (modDef.type === 'damage') damage += modDef.delta;
      if (modDef.type === 'critical') critical = Math.max(1, critical + modDef.delta);
      if (modDef.type === 'quality') qualitiesSet.add(modDef.quality);
    }
  }

  // Apply attachments
  for (const att of validAttachments) {
    if (att.effects?.grantedQualities) {
      for (const q of att.effects.grantedQualities) qualitiesSet.add(q);
    }
  }

  return {
    chassisId,
    name: chassis.name,
    skill: chassis.skill,
    damage,
    critical,
    qualities: Array.from(qualitiesSet),
    hardPointsTotal: chassis.hardPoints,
    hardPointsRemaining: chassis.hardPoints - usedHardPoints,
    crystal: {
      id: crystal.id,
      name: crystal.name,
      installedMods: [...installedMods]
    },
    attachments: validAttachments.map(a => a.id),
    worksUnderwater: validAttachments.some(a => a.effects?.worksUnderwater)
  };
}
