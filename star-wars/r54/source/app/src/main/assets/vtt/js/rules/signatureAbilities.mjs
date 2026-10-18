// Star Wars: Edge Studio / Fantasy Flight Games
// Canonical Career Signature Abilities & 8-Node Upgrade Trees
// Source: Core Rulebooks and Career Sourcebooks.

const clone = v => structuredClone(v);

export const SIGNATURE_ABILITIES_CATALOG = Object.freeze({
  always_get_my_mark: Object.freeze({
    id: 'always_get_my_mark',
    careerKey: 'bounty_hunter',
    name: 'Sempre Pego Minha Presa (Always Get My Mark)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para identificar a localização exata, pistas recentes e vulnerabilidade direta do alvo contratado.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'duration_1', name: 'Aumento de Duração', cost: 10, type: 'duration' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' },
      { id: 'range_1', name: 'Aumento de Alcance', cost: 10, type: 'range' }
    ])
  }),

  narrow_escape: Object.freeze({
    id: 'narrow_escape',
    careerKey: 'smuggler',
    name: 'Fuga por um Fio (Narrow Escape)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para desvencilhar a si e aos aliados de um cerco ou perseguição sem saída, abrindo uma rota de fuga imediata.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'allies_1', name: 'Escolta Ampla', cost: 15, type: 'allies' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  the_miracle_worker: Object.freeze({
    id: 'the_miracle_worker',
    careerKey: 'technician',
    name: 'Operador de Milagres (The Miracle Worker)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para reativar um sistema veicular ou dispositivo completamente destruído pelo restante do encontro.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'duration_1', name: 'Reparo Duradouro', cost: 10, type: 'duration' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  unmatched_carnage: Object.freeze({
    id: 'unmatched_carnage',
    careerKey: 'hired_gun',
    name: 'Chacina Sem Igual (Unmatched Carnage)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para realizar um ataque adicional gratuito contra cada oponente engajado na mesma rodada.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'damage_1', name: 'Dano Adicional', cost: 10, type: 'damage' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  roused_to_action: Object.freeze({
    id: 'roused_to_action',
    careerKey: 'commander',
    name: 'Despertar para a Ação (Roused to Action)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para conceder a todos os aliados no encontro 1 manobra gratuita adicional por turno durante o encontro.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'duration_1', name: 'Extensão de Comando', cost: 10, type: 'duration' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  the_bigger_they_are: Object.freeze({
    id: 'the_bigger_they_are',
    careerKey: 'soldier',
    name: 'Quanto Maior o Tamanho (The Bigger They Are)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para infligir acertos críticos automáticos e ignorar metade da proteção de alvos de silhueta superior.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'crit_1', name: 'Crítico Devastador', cost: 10, type: 'critical' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  })
});

export function canPurchaseSignatureAbility(character, signatureKey) {
  const ability = SIGNATURE_ABILITIES_CATALOG[signatureKey];
  if (!ability) return { can: false, reason: 'Habilidade de Assinatura desconhecida.' };

  const careerKey = character?.creation?.careerKey;
  if (careerKey !== ability.careerKey) {
    return { can: false, reason: `Esta habilidade pertence à carreira ${ability.careerKey}, não a ${careerKey}.` };
  }

  // Check if character already owns it
  const owned = character?.signatureAbilities || [];
  if (owned.some(a => a.id === signatureKey)) {
    return { can: false, reason: 'O personagem já possui esta Habilidade de Assinatura.' };
  }

  // Check if character has unlocked row 5 of a talent tree
  const purchases = character?.talentPurchases || [];
  const hasRow5 = purchases.some(p => p.row === 5);
  if (!hasRow5) {
    return { can: false, reason: 'É necessário ter adquirido pelo menos um talento de linha 5 na árvore da carreira.' };
  }

  const availableXp = Number(character?.xp?.available ?? 0);
  if (availableXp < ability.baseCost) {
    return { can: false, reason: `PE insuficientes: necessário ${ability.baseCost}, disponível ${availableXp}.` };
  }

  return { can: true, reason: null };
}

export function purchaseSignatureAbility(character, signatureKey) {
  const check = canPurchaseSignatureAbility(character, signatureKey);
  if (!check.can) throw new Error(check.reason);

  const ability = SIGNATURE_ABILITIES_CATALOG[signatureKey];
  const next = clone(character);
  next.xp.available -= ability.baseCost;
  next.signatureAbilities = Array.isArray(next.signatureAbilities) ? next.signatureAbilities : [];
  next.signatureAbilities.push({
    id: ability.id,
    name: ability.name,
    purchasedAt: new Date().toISOString(),
    upgrades: [],
    usedThisSession: false
  });

  return {
    character: next,
    ability: clone(ability),
    cost: ability.baseCost
  };
}

export function activateSignatureAbility(character, signatureKey, group) {
  const nextChar = clone(character);
  const nextGroup = clone(group);

  const entry = (nextChar.signatureAbilities || []).find(a => a.id === signatureKey);
  if (!entry) throw new Error('Personagem não possui esta Habilidade de Assinatura.');

  const ability = SIGNATURE_ABILITIES_CATALOG[signatureKey];
  if (entry.usedThisSession && !entry.upgrades.includes('frequency_1')) {
    throw new Error('Esta habilidade já foi utilizada nesta sessão.');
  }

  // Destiny cost check:
  const destiny = nextGroup?.destiny;
  if (destiny && destiny.light < ability.destinyCost) {
    throw new Error('Pontos de Destino luminosos insuficientes para ativar a Habilidade de Assinatura.');
  }

  if (destiny) {
    destiny.light -= ability.destinyCost;
    destiny.dark += ability.destinyCost;
  }

  entry.usedThisSession = true;

  return {
    character: nextChar,
    group: nextGroup,
    activated: true,
    abilityName: ability.name,
    description: ability.description
  };
}
