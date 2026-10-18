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
  }),

  hard_boiled: Object.freeze({
    id: 'hard_boiled',
    careerKey: 'bounty_hunter',
    name: 'Casca Grossa (Hard-Boiled)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para ignorar todos os efeitos incapacitantes e reduzir qualquer dano massivo recebido a zero na rodada.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'duration_1', name: 'Resiliência Duradoura', cost: 10, type: 'duration' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  unmatched_fortune: Object.freeze({
    id: 'unmatched_fortune',
    careerKey: 'smuggler',
    name: 'Sorte Inigualável (Unmatched Fortune)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para converter qualquer resultado de falha em Sucessos e Triunfos automáticos.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'allies_1', name: 'Sorte Compartilhada', cost: 15, type: 'allies' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  inventive_creation: Object.freeze({
    id: 'inventive_creation',
    careerKey: 'technician',
    name: 'Criação Inventiva (Inventive Creation)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para montar instantaneamente um dispositivo ou ferramenta de alta tecnologia funcional com sucata ao redor.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'complexity_1', name: 'Engenharia Complexa', cost: 10, type: 'complexity' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  last_one_standing: Object.freeze({
    id: 'last_one_standing',
    careerKey: 'hired_gun',
    name: 'Último de Pé (Last One Standing)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para desferir um golpe giratório devastador que incapacita instantaneamente todos os minions no alcance curto.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'range_1', name: 'Alcance Médio', cost: 10, type: 'range' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  master_commander: Object.freeze({
    id: 'master_commander',
    careerKey: 'commander',
    name: 'Mestre Estrategista (Master Commander)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para reorganizar completamente as posições de iniciativa de todos os combatentes e conceder 1 ação extra imediata.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'actions_1', name: 'Coordenação Tática', cost: 15, type: 'actions' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  overwhelming_firepower: Object.freeze({
    id: 'overwhelming_firepower',
    careerKey: 'soldier',
    name: 'Poder de Fogo Avassalador (Overwhelming Firepower)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para duplicar o dano base de uma arma pesada ou artilharia contra veículos ou alvos fortificados.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'crit_1', name: 'Destruição de Blindagem', cost: 10, type: 'critical' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  prophecy: Object.freeze({
    id: 'prophecy',
    careerKey: 'mystic',
    name: 'Profecia Viva (Prophecy)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para canalizar uma visão transcendental que reverte qualquer catástrofe narrativa iminente.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'destiny_grant_1', name: 'Revelação Cósmica', cost: 15, type: 'destiny_grant' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  unstoppable_force: Object.freeze({
    id: 'unstoppable_force',
    careerKey: 'guardian',
    name: 'Bastião Inabalável (Unstoppable Force)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para tornar a si e aos aliados adjacentes imunes a qualquer dano ou efeito crítico na rodada.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'duration_1', name: 'Bastião Prolongado', cost: 10, type: 'duration' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  peacekeeper_mandate: Object.freeze({
    id: 'peacekeeper_mandate',
    careerKey: 'consular',
    name: 'Mandato da Pacificação (Peacekeeper Mandate)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para impor uma trégua psíquica compulsória ou resolver um conflito social hostil instantaneamente.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'range_1', name: 'Presença Ampla', cost: 10, type: 'range' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  my_target_awaits: Object.freeze({
    id: 'my_target_awaits',
    careerKey: 'seeker',
    name: 'Meu Alvo me Espera (My Target Awaits)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para se mover instantaneamente para alcance engajado com qualquer presa ou alvo em vista.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'strike_1', name: 'Ataque de Emboscada', cost: 10, type: 'strike' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  unseen_infiltrator: Object.freeze({
    id: 'unseen_infiltrator',
    careerKey: 'sentinel',
    name: 'Infiltrador Invisível (Unseen Infiltrator)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para se tornar imperceptível a sensores eletrônicos, câmeras e visão biológica até o final do encontro.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'allies_1', name: 'Infiltração de Esquadra', cost: 15, type: 'allies' },
      { id: 'frequency_1', name: 'Aumento de Frequência', cost: 15, type: 'frequency' }
    ])
  }),

  deadly_reputation: Object.freeze({
    id: 'deadly_reputation',
    careerKey: 'warrior',
    name: 'Reputação Mortal (Deadly Reputation)',
    baseCost: 30,
    destinyCost: 1,
    description: 'Uma vez por sessão, gasta 1 Ponto de Destino para desmoralizar todos os oponentes na cena, forçando-os a sofrer 4 de Tensão e desvantagem constante.',
    upgrades: Object.freeze([
      { id: 'destiny_1', name: 'Redução de Destino', cost: 10, type: 'destiny' },
      { id: 'strain_1', name: 'Terror Profundo', cost: 10, type: 'strain' },
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
