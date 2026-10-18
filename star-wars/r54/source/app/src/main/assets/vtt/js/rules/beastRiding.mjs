// Star Wars: Age of Rebellion & Force and Destiny
// Stay on Target (pp. 74-85) & Savage Spirits (pp. 68-79)
// Canonical Beast Riding, Mount Profiles, Taming, Mounted Combat and Panic Mechanics.

const clone = v => structuredClone(v);
const clamp = (val, min, max) => Math.max(min, Math.min(max, val));

export const CANONICAL_MOUNTS = Object.freeze({
  dewback: Object.freeze({
    id: 'dewback',
    name: 'Dewback de Tatooine',
    silhouette: 3,
    woundThreshold: 22,
    soak: 5,
    speed: 2,
    defense: 0,
    baseBiteDamage: 7,
    tamingDifficulty: 2,
    temperament: 'Dócil quando adestrado, implacável no deserto',
    traits: Object.freeze(['Adaptado ao Calor Extremo', 'Mordida Trituradora', 'Resistência a Tempestades de Areia'])
  }),

  tauntaun: Object.freeze({
    id: 'tauntaun',
    name: 'Tauntaun das Estepes de Hoth',
    silhouette: 2,
    woundThreshold: 14,
    soak: 3,
    speed: 4,
    defense: 1,
    baseBiteDamage: 5,
    tamingDifficulty: 2,
    temperament: 'Nervoso e ágil, leal ao cavaleiro experiente',
    traits: Object.freeze(['Pelagem Térmica Ártica', 'Galope das Neves', 'Faro Aguçado'])
  }),

  bantha: Object.freeze({
    id: 'bantha',
    name: 'Bantha Gigante',
    silhouette: 4,
    woundThreshold: 35,
    soak: 7,
    speed: 1,
    defense: 0,
    baseBiteDamage: 10,
    tamingDifficulty: 3,
    temperament: 'Plácido, mas uma muralha viva imparável quando enfurecido',
    traits: Object.freeze(['Besta de Carga Titânica', 'Chifres Espirais de Choque', 'Pisoteio Esmagador'])
  }),

  varactyl: Object.freeze({
    id: 'varactyl',
    name: 'Varactyl de Utapau',
    silhouette: 3,
    woundThreshold: 18,
    soak: 4,
    speed: 5,
    defense: 1,
    baseBiteDamage: 6,
    tamingDifficulty: 3,
    temperament: 'Orgulhoso, leal e veloz escalador de despenhadeiros',
    traits: Object.freeze(['Escalada Vertical em Penhascos', 'Salto Amortecido', 'Agilidade Réptil'])
  }),

  blurrg: Object.freeze({
    id: 'blurrg',
    name: 'Blurrg das Planícies',
    silhouette: 2,
    woundThreshold: 16,
    soak: 4,
    speed: 3,
    defense: 0,
    baseBiteDamage: 6,
    tamingDifficulty: 2,
    temperament: 'Teimoso, agressivo no combate e de mastigação voraz',
    traits: Object.freeze(['Mandíbula Implacável', 'Firmeza de Tranco', 'Investida Pesada'])
  })
});

export function tameBeast(character, mountKey, checkResult = {}) {
  const template = CANONICAL_MOUNTS[mountKey];
  if (!template) throw new Error(`Montaria desconhecida: ${mountKey}`);

  const successes = Number(checkResult.successes ?? checkResult.success ?? 0);
  const advantages = Number(checkResult.advantages ?? checkResult.advantage ?? 0);
  const triumphs = Number(checkResult.triumphs ?? checkResult.triumph ?? 0);
  const threats = Number(checkResult.threats ?? checkResult.threat ?? 0);
  const despairs = Number(checkResult.despairs ?? checkResult.despair ?? 0);

  if (successes < 1) {
    const retaliation = despairs > 0 || threats >= 3;
    return {
      tamed: false,
      retaliation,
      damageSuffered: retaliation ? template.baseBiteDamage : 0,
      message: retaliation
        ? `Falha na doma! O ${template.name} reagiu violentamente e desferiu um ataque de retaliação (${template.baseBiteDamage} de dano).`
        : `O ${template.name} recusou o adestramento e se manteve arisco.`
    };
  }

  const hasBond = triumphs > 0 || advantages >= 3;
  const mountInstance = {
    id: `mount_${template.id}_${Date.now()}`,
    templateId: template.id,
    name: `${template.name} de ${character.name}`,
    silhouette: template.silhouette,
    woundThreshold: template.woundThreshold,
    currentWounds: 0,
    soak: template.soak,
    speed: template.speed,
    defense: template.defense,
    hasBond,
    tamedAt: new Date().toISOString(),
    mounted: false
  };

  const nextChar = clone(character);
  nextChar.mounts = Array.isArray(nextChar.mounts) ? nextChar.mounts : [];
  nextChar.mounts.push(mountInstance);

  return {
    tamed: true,
    character: nextChar,
    mount: mountInstance,
    hasBond,
    message: hasBond
      ? `Sucesso extraordinário! O ${template.name} foi domado e desenvolveu um Elo Empático especial (+1 Dado de Vantagem contínuo).`
      : `O ${template.name} foi domado com sucesso e agora aceita arreios e comandos.`
  };
}

export function mountBeast(mount) {
  const next = clone(mount);
  if (next.mounted) throw new Error('O personagem já está montado nesta criatura.');
  next.mounted = true;
  return next;
}

export function dismountBeast(mount) {
  const next = clone(mount);
  if (!next.mounted) throw new Error('O personagem não está montado.');
  next.mounted = false;
  return next;
}

export function executeMountedCharge(mount, weaponBaseDamage = 4) {
  if (!mount.mounted) throw new Error('É necessário estar montado para executar uma Investida de Montaria.');
  // Canonical rule: Mounted charge adds the beast silhouette to the damage
  const chargeBonus = Number(mount.silhouette) || 1;
  const totalDamage = weaponBaseDamage + chargeBonus;
  return {
    chargeBonus,
    totalDamage,
    speed: mount.speed,
    description: `Investida montada com ${mount.name}: +${chargeBonus} de dano por impacto de Silhueta ${mount.silhouette}. Dano total: ${totalDamage}.`
  };
}

export function checkBeastPanic(mount, damageTaken, checkResult = {}) {
  const next = clone(mount);
  next.currentWounds = clamp(Number(next.currentWounds || 0) + Number(damageTaken || 0), 0, next.woundThreshold * 2);

  const halfThreshold = Math.floor(next.woundThreshold / 2);
  const needsPanicCheck = next.currentWounds >= halfThreshold;

  if (!needsPanicCheck) {
    return {
      mount: next,
      panicked: false,
      thrownRider: false,
      message: `${next.name} suportou os ferimentos sem hesitar.`
    };
  }

  const successes = Number(checkResult.successes ?? checkResult.success ?? 0);
  const despairs = Number(checkResult.despairs ?? checkResult.despair ?? 0);

  if (successes >= 1 && despairs === 0) {
    return {
      mount: next,
      panicked: false,
      thrownRider: false,
      message: `Comando firme! O cavaleiro controlou o estresse de ${next.name}.`
    };
  }

  next.mounted = false;
  return {
    mount: next,
    panicked: true,
    thrownRider: true,
    strainSufferedByRider: 3,
    message: `Pânico! Ferido criticamente, ${next.name} empinou em desespero, arremessou o cavaleiro ao solo (3 de Tensão) e disparou em fuga!`
  };
}
