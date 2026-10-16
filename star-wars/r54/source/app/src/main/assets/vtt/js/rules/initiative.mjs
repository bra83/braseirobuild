import * as talents_mjs_1 from './talents.mjs';
import * as health_mjs_1 from './health.mjs';
export function initiativeEntry({ id, side, check, label = '' }) {
    if (!['player', 'npc'].includes(side))
        throw new Error('Lado de iniciativa inválido.');
    if (!check)
        throw new Error('Tirada de iniciativa inválida.');
    return {
        id,
        side,
        label,
        successes: Math.max(0, Number(check.netSuccess) || 0),
        advantages: Math.max(0, Number(check.netAdvantage) || 0),
        triumphs: Math.max(0, Number(check.triumph) || 0)
    };
}
export function sortInitiative(entries) {
    return [...entries].sort((a, b) => b.successes - a.successes ||
        b.advantages - a.advantages ||
        b.triumphs - a.triumphs ||
        (a.side === 'player' && b.side === 'npc' ? -1 : a.side === 'npc' && b.side === 'player' ? 1 : 0));
}
export function buildInitiativeSlots(entries) {
    return sortInitiative(entries).map((entry, index) => ({
        index,
        side: entry.side,
        sourceId: entry.id,
        successes: entry.successes,
        advantages: entry.advantages,
        triumphs: entry.triumphs
    }));
}
export function preparePlayerInitiative(character, { quickReactionStrain = 0 } = {}) {
    const option = (0, talents_mjs_1.quickReactionOption)(character, quickReactionStrain);
    if (option.spend === 0)
        return { character: structuredClone(character), boost: 0, quickReaction: option };
    const result = (0, health_mjs_1.sufferStrain)(character, option.spend);
    return { character: result.character, boost: option.boost, quickReaction: option, status: result.status };
}
