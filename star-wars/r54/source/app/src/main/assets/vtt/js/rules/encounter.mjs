import * as initiative_mjs_1 from './initiative.mjs';
import * as adversaries_mjs_1 from './adversaries.mjs';
export function createEncounterState() {
    return {
        entries: [],
        slots: [],
        cursor: 0,
        actedActorIds: [],
        waitingForPlayer: false,
        playerActorId: 'player',
        npcActorIds: [],
        activeActorId: null,
        history: []
    };
}
function clone(value) { return structuredClone(value); }
export function setupEncounter(combat, { entries, adversaries, playerActorId = 'player' } = {}) {
    if (!combat?.active)
        throw new Error('O combate precisa estar ativo para montar a iniciativa.');
    if (!Array.isArray(entries) || entries.length < 2)
        throw new Error('A iniciativa precisa de pelo menos dois participantes.');
    if (!Array.isArray(adversaries) || !adversaries.length)
        throw new Error('Adversários não informados.');
    const next = clone(combat);
    next.adversaries = clone(adversaries);
    next.encounter = {
        ...createEncounterState(),
        entries: clone(entries),
        slots: (0, initiative_mjs_1.buildInitiativeSlots)(entries),
        playerActorId,
        npcActorIds: adversaries.map(a => a.id),
        history: [{ type: 'INICIATIVA_DEFINIDA', round: next.round, slots: (0, initiative_mjs_1.buildInitiativeSlots)(entries).map(s => s.side) }]
    };
    return next;
}
export function currentSlot(combat) {
    if (!combat?.active)
        return null;
    const e = combat.encounter;
    if (!e?.slots?.length)
        return null;
    return e.slots[e.cursor] ?? null;
}
export function livingNpcIds(combat) {
    return (combat?.adversaries ?? [])
        .filter(a => a.status !== 'surrendered' && a.status !== 'retreated' && !(0, adversaries_mjs_1.adversaryHealthStatus)(a).incapacitated)
        .map(a => a.id);
}
export function availableActorsForCurrentSlot(combat) {
    const slot = currentSlot(combat);
    if (!slot)
        return [];
    const acted = new Set(combat.encounter.actedActorIds ?? []);
    if (slot.side === 'player') {
        const id = combat.encounter.playerActorId;
        return acted.has(id) ? [] : [id];
    }
    return livingNpcIds(combat).filter(id => !acted.has(id));
}
export function assignCurrentSlot(combat, actorId) {
    const slot = currentSlot(combat);
    if (!slot)
        throw new Error('Não há posição de iniciativa disponível.');
    const available = availableActorsForCurrentSlot(combat);
    if (!available.includes(actorId))
        throw new Error('Este participante não pode ocupar a posição atual de iniciativa.');
    const next = clone(combat);
    next.encounter.activeActorId = actorId;
    next.encounter.waitingForPlayer = slot.side === 'player';
    next.encounter.history.push({ type: 'POSICAO_ATRIBUIDA', round: next.round, slot: next.encounter.cursor, side: slot.side, actorId });
    return next;
}
export function completeCurrentSlot(combat, actorId, { summary = 'Turno concluído' } = {}) {
    const slot = currentSlot(combat);
    if (!slot)
        throw new Error('Não há posição de iniciativa para concluir.');
    const next = clone(combat);
    if (next.encounter.activeActorId && next.encounter.activeActorId !== actorId)
        throw new Error('Outro participante está ocupando esta posição.');
    if (!next.encounter.actedActorIds.includes(actorId))
        next.encounter.actedActorIds.push(actorId);
    next.encounter.history.push({ type: 'TURNO_CONCLUIDO', round: next.round, slot: next.encounter.cursor, side: slot.side, actorId, summary });
    next.encounter.cursor += 1;
    next.encounter.activeActorId = null;
    next.encounter.waitingForPlayer = false;
    if (next.encounter.cursor >= next.encounter.slots.length) {
        next.round += 1;
        next.encounter.cursor = 0;
        next.encounter.actedActorIds = [];
        next.playerTurn = { actionUsed: false, maneuversUsed: 0 };
        next.pendingAttackBoost = 0;
        next.encounter.history.push({ type: 'NOVO_ASSALTO', round: next.round });
    }
    return next;
}
export function skipCurrentSlot(combat, { summary = 'Posição sem participante disponível' } = {}) {
    const slot = currentSlot(combat);
    if (!slot)
        throw new Error('Não há posição de iniciativa para ignorar.');
    const next = clone(combat);
    next.encounter.history.push({ type: 'POSICAO_IGNORADA', round: next.round, slot: next.encounter.cursor, side: slot.side, summary });
    next.encounter.cursor += 1;
    next.encounter.activeActorId = null;
    next.encounter.waitingForPlayer = false;
    if (next.encounter.cursor >= next.encounter.slots.length) {
        next.round += 1;
        next.encounter.cursor = 0;
        next.encounter.actedActorIds = [];
        next.playerTurn = { actionUsed: false, maneuversUsed: 0 };
        next.pendingAttackBoost = 0;
        next.encounter.history.push({ type: 'NOVO_ASSALTO', round: next.round });
    }
    return next;
}
export function updateAdversaryInCombat(combat, adversary) {
    const next = clone(combat);
    const index = next.adversaries.findIndex(a => a.id === adversary.id);
    if (index < 0)
        throw new Error('Adversário do encontro não encontrado.');
    next.adversaries[index] = clone(adversary);
    return next;
}
export function encounterOver(combat, playerUnconscious = false) {
    if (!combat?.active)
        return true;
    if (playerUnconscious)
        return true;
    return livingNpcIds(combat).length === 0;
}
