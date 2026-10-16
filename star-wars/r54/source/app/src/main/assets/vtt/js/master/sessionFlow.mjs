export const SESSION_PHASE_LABELS_PT_BR = Object.freeze({
    abertura: 'Abertura',
    cena: 'Cena',
    decisao: 'Decisão do jogador',
    resolucao: 'Resolução',
    consequencia: 'Consequência',
    conflito: 'Conflito',
    recuperacao: 'Recuperação',
    encerramento: 'Encerramento',
    encerrada: 'Sessão encerrada'
});
export const SESSION_PHASES = Object.freeze(Object.keys(SESSION_PHASE_LABELS_PT_BR));
export function createSessionRuntime(number = 1) {
    return {
        schemaVersion: 1,
        number: Number.isInteger(number) && number > 0 ? number : 1,
        status: 'nao_iniciada',
        phase: 'abertura',
        startedAt: null,
        endedAt: null,
        checksResolved: 0,
        combatEncounters: 0,
        xpAwarded: 0,
        summary: null
    };
}
export function normalizeSessionRuntime(value, number = 1) {
    const base = createSessionRuntime(number);
    if (!value || typeof value !== 'object')
        return base;
    const next = { ...base, ...structuredClone(value) };
    if (!['nao_iniciada', 'ativa', 'encerrada'].includes(next.status))
        next.status = 'nao_iniciada';
    if (!SESSION_PHASES.includes(next.phase))
        next.phase = next.status === 'encerrada' ? 'encerrada' : 'abertura';
    for (const key of ['checksResolved', 'combatEncounters', 'xpAwarded']) {
        const n = Number(next[key]);
        next[key] = Number.isInteger(n) && n >= 0 ? n : 0;
    }
    next.number = Number.isInteger(Number(next.number)) && Number(next.number) > 0 ? Number(next.number) : number;
    return next;
}
export function startSessionRuntime(value, { at = new Date().toISOString(), number = null } = {}) {
    const current = normalizeSessionRuntime(value);
    if (current.status === 'ativa')
        throw new Error('Já existe uma sessão em andamento.');
    const nextNumber = number ?? (current.status === 'encerrada' ? current.number + 1 : current.number);
    return {
        ...createSessionRuntime(nextNumber),
        status: 'ativa',
        phase: 'abertura',
        startedAt: at
    };
}
export function setSessionPhase(value, phase) {
    const next = normalizeSessionRuntime(value);
    if (next.status !== 'ativa')
        return next;
    if (!SESSION_PHASES.includes(phase) || phase === 'encerrada')
        throw new Error('Fase de sessão inválida.');
    next.phase = phase;
    return next;
}
export function noteSessionCheck(value) {
    const next = normalizeSessionRuntime(value);
    if (next.status !== 'ativa')
        return next;
    next.checksResolved += 1;
    next.phase = 'consequencia';
    return next;
}
export function noteSessionCombatStarted(value) {
    const next = normalizeSessionRuntime(value);
    if (next.status !== 'ativa')
        return next;
    next.combatEncounters += 1;
    next.phase = 'conflito';
    return next;
}
export function noteSessionCombatEnded(value) {
    const next = normalizeSessionRuntime(value);
    if (next.status !== 'ativa')
        return next;
    next.phase = 'recuperacao';
    return next;
}
export function phaseAfterMasterProposal(proposal) {
    if (proposal?.suggestedCheck)
        return 'resolucao';
    if (proposal?.askPlayer)
        return 'decisao';
    return 'cena';
}
export function closeSessionRuntime(value, { at = new Date().toISOString(), summary = '', xpAwarded = 0 } = {}) {
    const current = normalizeSessionRuntime(value);
    if (current.status !== 'ativa')
        throw new Error('Não há sessão ativa para encerrar.');
    const xp = Number(xpAwarded);
    if (!Number.isInteger(xp) || xp < 0 || xp > 1000)
        throw new Error('PE de encerramento inválidos.');
    return {
        ...current,
        status: 'encerrada',
        phase: 'encerrada',
        endedAt: at,
        xpAwarded: xp,
        summary: String(summary ?? '').trim() || 'Sessão encerrada sem resumo narrativo.'
    };
}
export function buildDeterministicSessionSummary(state) {
    const s = normalizeSessionRuntime(state?.session);
    const character = state?.player?.character;
    const eventSlice = (state?.events ?? []).filter(e => {
        if (!s.startedAt)
            return false;
        return String(e.at ?? '') >= s.startedAt;
    });
    const intents = eventSlice.filter(e => e.type === 'PLAYER_INTENT').length;
    const combats = s.combatEncounters;
    const checks = s.checksResolved;
    const status = character
        ? `Ferimentos ${character.derived?.wounds?.current ?? 0}/${character.derived?.wounds?.threshold ?? 0}; Tensão ${character.derived?.strain?.current ?? 0}/${character.derived?.strain?.threshold ?? 0}.`
        : 'Sem personagem ativo.';
    return `Sessão ${s.number}: ${intents} ação(ões) declarada(s), ${checks} teste(s) resolvido(s), ${combats} encontro(s) de combate. ${status}`;
}
