// Star Wars: Edge Studio / Fantasy Flight Games
// Special Modifications (pp. 72-91) & Fully Operational (pp. 78-95)
// Canonical implementation of Weapon, Armor, and Droid Crafting mechanics.

const clone = v => structuredClone(v);
const int = (v, label, { min = 0, max = 999999 } = {}) => {
  const n = Number(v);
  if (!Number.isInteger(n) || n < min || n > max) throw new Error(`${label} inválido.`);
  return n;
};

export const WEAPON_CRAFTING_TEMPLATES = Object.freeze({
  blaster_pistol: Object.freeze({
    id: 'blaster_pistol',
    category: 'weapon',
    name: 'Pistola Blaster Personalizada',
    skill: 'armas_distancia_ligeras',
    difficulty: 2,
    creditCost: 150,
    timeHours: 8,
    baseDamage: 6,
    baseCritical: 3,
    baseEncumbrance: 1,
    baseHardPoints: 2,
    range: 'Médio',
    qualities: Object.freeze(['Dano Atordoante'])
  }),
  blaster_rifle: Object.freeze({
    id: 'blaster_rifle',
    category: 'weapon',
    name: 'Rifle Blaster Personalizado',
    skill: 'armas_distancia_pesadas',
    difficulty: 3,
    creditCost: 400,
    timeHours: 16,
    baseDamage: 9,
    baseCritical: 3,
    baseEncumbrance: 4,
    baseHardPoints: 3,
    range: 'Longo',
    qualities: Object.freeze(['Dano Atordoante'])
  }),
  heavy_blaster: Object.freeze({
    id: 'heavy_blaster',
    category: 'weapon',
    name: 'Arma Blaster Pesada Personalizada',
    skill: 'artilleria',
    difficulty: 4,
    creditCost: 850,
    timeHours: 24,
    baseDamage: 11,
    baseCritical: 2,
    baseEncumbrance: 6,
    baseHardPoints: 4,
    range: 'Longo',
    qualities: Object.freeze(['Incômoda 3'])
  }),
  vibro_blade: Object.freeze({
    id: 'vibro_blade',
    category: 'weapon',
    name: 'Vibrolâmina Forjada',
    skill: 'armas_cuerpo_a_cuerpo',
    difficulty: 2,
    creditCost: 100,
    timeHours: 6,
    baseDamage: 3, // + Brawn
    baseCritical: 2,
    baseEncumbrance: 2,
    baseHardPoints: 2,
    range: 'Interação',
    qualities: Object.freeze(['Perfurante 2', 'Viciosa 1'])
  })
});

export const ARMOR_CRAFTING_TEMPLATES = Object.freeze({
  light_armor: Object.freeze({
    id: 'light_armor',
    category: 'armor',
    name: 'Traje Laminado Leve',
    difficulty: 2,
    creditCost: 200,
    timeHours: 10,
    baseSoak: 1,
    baseDefenseMelee: 0,
    baseDefenseRanged: 0,
    baseEncumbrance: 1,
    baseHardPoints: 1
  }),
  heavy_armor: Object.freeze({
    id: 'heavy_armor',
    category: 'armor',
    name: 'Armadura de Placas Pesada',
    difficulty: 3,
    creditCost: 650,
    timeHours: 20,
    baseSoak: 2,
    baseDefenseMelee: 1,
    baseDefenseRanged: 1,
    baseEncumbrance: 3,
    baseHardPoints: 2
  }),
  powered_armor: Object.freeze({
    id: 'powered_armor',
    category: 'armor',
    name: 'Exoarmadura Energizada',
    difficulty: 4,
    creditCost: 3000,
    timeHours: 40,
    baseSoak: 3,
    baseDefenseMelee: 2,
    baseDefenseRanged: 2,
    baseEncumbrance: 4,
    baseHardPoints: 3
  })
});

export const DROID_CRAFTING_TEMPLATES = Object.freeze({
  combat_droid: Object.freeze({
    id: 'combat_droid',
    category: 'droid',
    name: 'Droide de Combate de Quarta Classe',
    difficulty: 3,
    creditCost: 2500,
    timeHours: 32,
    characteristics: Object.freeze({ fortaleza: 3, agilidad: 3, intelecto: 1, astucia: 2, voluntad: 1, presencia: 1 }),
    woundsThreshold: 14,
    strainThreshold: 10,
    soak: 4,
    skills: Object.freeze({ pelea: 2, armas_distancia_pesadas: 2, vigilancia: 1 })
  }),
  astromech_droid: Object.freeze({
    id: 'astromech_droid',
    category: 'droid',
    name: 'Droide Astromecânico de Segunda Classe',
    difficulty: 2,
    creditCost: 1500,
    timeHours: 20,
    characteristics: Object.freeze({ fortaleza: 1, agilidad: 2, intelecto: 3, astucia: 2, voluntad: 1, presencia: 1 }),
    woundsThreshold: 10,
    strainThreshold: 10,
    soak: 2,
    skills: Object.freeze({ computadoras: 2, mecanica: 2, astronavegacion: 2 })
  }),
  medical_droid: Object.freeze({
    id: 'medical_droid',
    category: 'droid',
    name: 'Droide Médico de Primeira Classe',
    difficulty: 3,
    creditCost: 1800,
    timeHours: 24,
    characteristics: Object.freeze({ fortaleza: 1, agilidad: 2, intelecto: 4, astucia: 2, voluntad: 1, presencia: 1 }),
    woundsThreshold: 10,
    strainThreshold: 12,
    soak: 2,
    skills: Object.freeze({ medicina: 3, educacion: 2 })
  })
});

export function craftItem(character, {
  templateType, // 'weapon' | 'armor' | 'droid'
  templateId,
  rollResult = { successes: 1, advantage: 0, triumph: 0, threat: 0, despair: 0 },
  spentAdvantage = []
} = {}) {
  if (!character) throw new Error('Personagem inválido para fabricação.');
  let template = null;
  if (templateType === 'weapon') template = WEAPON_CRAFTING_TEMPLATES[templateId];
  else if (templateType === 'armor') template = ARMOR_CRAFTING_TEMPLATES[templateId];
  else if (templateType === 'droid') template = DROID_CRAFTING_TEMPLATES[templateId];
  else throw new Error(`Tipo de template inválido: ${templateType}`);

  if (!template) throw new Error(`Template não encontrado: ${templateId}`);

  // Credit cost verification
  const currentCredits = Number(character.credits ?? 0);
  if (currentCredits < template.creditCost) {
    throw new Error(`Créditos insuficientes: necessário ${template.creditCost}, disponível ${currentCredits}.`);
  }

  const successes = Number(rollResult.successes ?? 0);
  if (successes < 1) {
    // Construction failed: 50% material salvage lost
    const nextChar = clone(character);
    nextChar.credits -= Math.floor(template.creditCost / 2);
    return {
      success: false,
      character: nextChar,
      salvagedCreditsLost: Math.floor(template.creditCost / 2),
      item: null,
      message: 'Falha no teste de Mecânica. Metade dos materiais foi perdida.'
    };
  }

  const nextChar = clone(character);
  nextChar.credits -= template.creditCost;

  const advantage = Number(rollResult.advantage ?? rollResult.advantages ?? 0);
  const triumph = Number(rollResult.triumph ?? rollResult.triumphs ?? 0);
  const threat = Number(rollResult.threat ?? rollResult.threats ?? 0);
  const despair = Number(rollResult.despair ?? rollResult.despairs ?? 0);

  let finalTimeHours = template.timeHours;
  let damage = template.baseDamage ?? 0;
  let soak = template.baseSoak ?? (template.soak ?? 0);
  let hardPoints = template.baseHardPoints ?? 0;
  let encumbrance = template.baseEncumbrance ?? 0;
  const qualities = Array.isArray(template.qualities) ? [...template.qualities] : [];

  // Advantage expenditure options
  if (advantage >= 2) {
    finalTimeHours = Math.max(1, Math.floor(finalTimeHours * 0.75)); // -25% tempo
  }
  if (advantage >= 3) {
    if (templateType === 'weapon') damage += 1;
    if (templateType === 'armor') soak += 1;
    if (templateType === 'droid') hardPoints += 1;
  }
  if (advantage >= 4) {
    hardPoints += 1; // +1 Hard Point extra
  }

  // Triumph effects
  if (triumph >= 1) {
    if (templateType === 'weapon') qualities.push('Precisa 1');
    if (templateType === 'armor') qualities.push('Defensiva 1');
    if (templateType === 'droid') {
      // Droid gains a free skill rank
      if (template.skills) {
        const firstSkill = Object.keys(template.skills)[0];
        template = { ...template, skills: { ...template.skills, [firstSkill]: template.skills[firstSkill] + 1 } };
      }
    }
  }

  // Threat & Despair effects
  if (threat >= 2) {
    finalTimeHours = Math.floor(finalTimeHours * 1.5); // +50% tempo
  }
  if (despair >= 1) {
    qualities.push('Imprecisa 1');
  }

  const craftedItem = {
    id: `crafted_${template.id}_${Date.now()}`,
    name: template.name,
    category: templateType,
    damage,
    soak,
    hardPoints,
    encumbrance: Math.max(0, encumbrance),
    timeHours: finalTimeHours,
    qualities,
    craftedBy: character.name,
    craftedAt: new Date().toISOString(),
    details: template
  };

  // Add to character inventory or droids
  if (templateType === 'weapon') {
    nextChar.weapons = Array.isArray(nextChar.weapons) ? nextChar.weapons : [];
    nextChar.weapons.push(craftedItem);
  } else if (templateType === 'armor') {
    nextChar.armor = Array.isArray(nextChar.armor) ? nextChar.armor : [];
    nextChar.armor.push(craftedItem);
  } else if (templateType === 'droid') {
    nextChar.gear = Array.isArray(nextChar.gear) ? nextChar.gear : [];
    nextChar.gear.push(craftedItem);
  }

  return {
    success: true,
    character: nextChar,
    item: craftedItem,
    creditsSpent: template.creditCost,
    timeHours: finalTimeHours,
    message: `Fabricação de ${template.name} concluída com sucesso!`
  };
}
