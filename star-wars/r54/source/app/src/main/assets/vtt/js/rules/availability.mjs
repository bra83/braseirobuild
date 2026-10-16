function integer(value, label, { min = -100, max = 100 } = {}) {
    const n = Number(value);
    if (!Number.isInteger(n) || n < min || n > max)
        throw new Error(`${label} inválido.`);
    return n;
}
export const RARITY_MODIFIER_REFERENCE = Object.freeze([
    Object.freeze({ id: 'primary_core_world', modifier: -2, label: 'Mundo primário do Núcleo (como Coruscant, Duro ou Corellia)' }),
    Object.freeze({ id: 'other_core_world', modifier: -1, label: 'Outro mundo do Núcleo' }),
    Object.freeze({ id: 'primary_trade_lane', modifier: -1, label: 'Mundo em rota comercial primária' }),
    Object.freeze({ id: 'colony_or_inner_rim', modifier: 0, label: 'Colônia ou mundo da Orla Interior' }),
    Object.freeze({ id: 'civilized_world', modifier: 0, label: 'Mundo civilizado' }),
    Object.freeze({ id: 'mid_rim_world', modifier: 1, label: 'Mundo da Orla Média' }),
    Object.freeze({ id: 'recently_settled_or_out_of_way', modifier: 1, label: 'Mundo colonizado recentemente ou fora de mão' }),
    Object.freeze({ id: 'outer_rim_world', modifier: 2, label: 'Mundo da Orla Exterior' }),
    Object.freeze({ id: 'frontier_world', modifier: 2, label: 'Mundo de fronteira' }),
    Object.freeze({ id: 'wild_space_world', modifier: 3, label: 'Mundo do Espaço Selvagem' }),
    Object.freeze({ id: 'uncivilized_world', modifier: 4, label: 'Mundo não civilizado' })
]);
export function applyDifficultyUpgrades(difficulty, upgrades) {
    let d = integer(difficulty, 'Dificuldade', { min: 0, max: 100 });
    let c = 0;
    let n = integer(upgrades, 'Melhorias de dificuldade', { min: 0, max: 100 });
    while (n-- > 0) {
        if (d > 0) {
            d -= 1;
            c += 1;
        }
        else
            d += 1;
    }
    return { difficulty: d, challenge: c };
}
export function rarityDifficulty(rarity) {
    const r = integer(rarity, 'Raridade', { min: 0, max: 100 });
    const baseDifficulty = r <= 1 ? 0 : r <= 3 ? 1 : r <= 5 ? 2 : r <= 7 ? 3 : r <= 9 ? 4 : 5;
    const upgrades = Math.max(0, r - 10);
    const pool = applyDifficultyUpgrades(baseDifficulty, upgrades);
    const label = baseDifficulty === 0 ? 'Simples' : baseDifficulty === 1 ? 'Fácil' : baseDifficulty === 2 ? 'Média' : baseDifficulty === 3 ? 'Difícil' : baseDifficulty === 4 ? 'Desalentadora' : 'Formidável';
    return { rarity: r, baseDifficulty, upgrades, ...pool, label, requiresRoll: baseDifficulty > 0 };
}
export function effectiveRarity(baseRarity, rarityModifier) {
    const base = integer(baseRarity, 'Raridade base', { min: 0, max: 100 });
    const modifier = integer(rarityModifier, 'Modificador de raridade', { min: -100, max: 100 });
    return Math.max(0, base + modifier);
}
export function availabilityProfile({ rarity, restricted = false, rarityModifier = null } = {}) {
    const baseRarity = integer(rarity, 'Raridade base', { min: 0, max: 100 });
    const isRestricted = Boolean(restricted);
    const skillKey = isRestricted ? 'callejeo' : 'negociacion';
    const base = rarityDifficulty(baseRarity);
    if (rarityModifier === null || rarityModifier === undefined) {
        return {
            baseRarity,
            rarityModifier: null,
            modifierKnown: false,
            effectiveRarity: null,
            skillKey,
            restricted: isRestricted,
            baseDifficulty: base.difficulty,
            baseChallenge: base.challenge,
            baseUpgrades: base.upgrades,
            difficulty: null,
            challenge: null,
            upgrades: null,
            label: null,
            requiresRoll: null,
            knowledgeAlternativeAllowed: !isRestricted,
            lawEnforcementMayIncreaseDifficulty: isRestricted
        };
    }
    const modifier = integer(rarityModifier, 'Modificador de raridade', { min: -100, max: 100 });
    const effective = effectiveRarity(baseRarity, modifier);
    const check = rarityDifficulty(effective);
    return {
        baseRarity,
        rarityModifier: modifier,
        modifierKnown: true,
        effectiveRarity: effective,
        skillKey,
        restricted: isRestricted,
        baseDifficulty: base.difficulty,
        baseChallenge: base.challenge,
        baseUpgrades: base.upgrades,
        difficulty: check.difficulty,
        challenge: check.challenge,
        upgrades: check.upgrades,
        label: check.label,
        requiresRoll: check.requiresRoll,
        knowledgeAlternativeAllowed: !isRestricted,
        lawEnforcementMayIncreaseDifficulty: isRestricted
    };
}
export function rarityModifierReferenceById(id) {
    return RARITY_MODIFIER_REFERENCE.find(entry => entry.id === String(id)) ?? null;
}
