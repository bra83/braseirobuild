const RANGE_ORDER = Object.freeze({ 'Interação': 0, 'Curto': 1, 'Médio': 2, 'Longo': 3, 'Extremo': 4 });
const RANGED_BASE_DIFFICULTY = Object.freeze({ 'Interação': 1, 'Curto': 1, 'Médio': 2, 'Longo': 3, 'Extremo': 4 });
export function attackDifficultyForWeapon(weapon, targetRange) {
    if (!weapon)
        throw new Error('Arma inválida.');
    if (!(targetRange in RANGE_ORDER))
        throw new Error('Intervalo de alcance inválido.');
    if (!(weapon.range in RANGE_ORDER))
        throw new Error('Alcance da arma inválido.');
    if (RANGE_ORDER[targetRange] > RANGE_ORDER[weapon.range])
        throw new Error('O alvo está além do alcance desta arma.');
    if (weapon.skill === 'pelea' || weapon.skill === 'armas_cuerpo_a_cuerpo') {
        if (targetRange !== 'Interação')
            throw new Error('Ataques corpo a corpo exigem distância de Interação.');
        return 2;
    }
    let difficulty = RANGED_BASE_DIFFICULTY[targetRange];
    if (targetRange === 'Interação') {
        if (weapon.skill === 'armas_distancia_ligeras')
            difficulty += 1;
        else if (weapon.skill === 'armas_distancia_pesadas')
            difficulty += 2;
        else if (weapon.skill === 'artilleria')
            throw new Error('Não é possível usar Artilharia em Interação com um inimigo.');
    }
    return difficulty;
}
export function availableTargetRanges(weapon) {
    if (!weapon || !(weapon.range in RANGE_ORDER))
        return [];
    if (weapon.skill === 'pelea' || weapon.skill === 'armas_cuerpo_a_cuerpo')
        return ['Interação'];
    return Object.keys(RANGE_ORDER).filter(r => RANGE_ORDER[r] <= RANGE_ORDER[weapon.range]);
}
