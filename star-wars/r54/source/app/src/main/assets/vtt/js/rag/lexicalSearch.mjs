import * as rulesCorpus_mjs_1 from './rulesCorpus.mjs';
export const RULE_CORPUS_VERSION = rulesCorpus_mjs_1.RULE_CORPUS_VERSION;
const STOP = new Set(['a', 'o', 'as', 'os', 'de', 'da', 'do', 'das', 'dos', 'e', 'em', 'um', 'uma', 'para', 'por', 'com', 'como', 'que', 'qual', 'quais', 'no', 'na', 'nos', 'nas', 'me', 'eu', 'meu', 'minha', 'funciona', 'funcionar', 'regra', 'regras']);
export function normalizeSearchText(value = '') {
    return String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}
export function tokenize(value = '') {
    return normalizeSearchText(value).split(/\s+/).filter(t => t.length > 1 && !STOP.has(t));
}
export const VERIFIED_CORPUS_GAPS = Object.freeze([]);
export function verifiedCorpusGapForQuery(_query = '') {
    return null;
}
function searchable(doc) {
    return normalizeSearchText([doc.id, doc.title, doc.summary, doc.inputs, doc.source, doc.function].filter(Boolean).join(' '));
}
function idf(token, corpus) {
    let n = 0;
    for (const d of corpus)
        if (searchable(d).includes(token))
            n++;
    return Math.log(1 + corpus.length / (1 + n));
}
export function searchVerifiedRules(query, { limit = 5, includePending = true, corpus = rulesCorpus_mjs_1.VERIFIED_RULE_CORPUS } = {}) {
    const q = normalizeSearchText(query);
    if (!q)
        return [];
    if (verifiedCorpusGapForQuery(query))
        return [];
    const tokens = tokenize(query);
    const scored = [];
    for (const doc of corpus) {
        if (!includePending && !['TESTADO', 'IMPLEMENTADO'].includes(doc.status))
            continue;
        const hay = searchable(doc);
        let score = 0;
        if (hay.includes(q))
            score += 8;
        for (const token of tokens) {
            if (!hay.includes(token))
                continue;
            score += idf(token, corpus);
            if (normalizeSearchText(doc.title).includes(token))
                score += 1.8;
            if (normalizeSearchText(doc.summary).includes(token))
                score += 1.2;
            if (normalizeSearchText(doc.id).includes(token))
                score += 0.8;
        }
        if (score > 0)
            scored.push({ doc, score });
    }
    return scored.sort((a, b) => b.score - a.score || a.doc.id.localeCompare(b.doc.id)).slice(0, limit).map((x, i) => ({ rank: i + 1, score: Number(x.score.toFixed(4)), ...structuredClone(x.doc) }));
}
export function ruleRetrievalStatus(results = []) {
    if (!results.length)
        return { grounded: false, reason: 'Nenhuma regra pertinente foi encontrada no corpus auditado.' };
    const grounded = results.some(r => ['TESTADO', 'IMPLEMENTADO', 'FONTE_IMPORTADA'].includes(r.status) && r.summary && r.summary !== 'A_AUDITAR');
    return grounded
        ? { grounded: true, reason: 'Há evidência local pertinente no índice offline.' }
        : { grounded: false, reason: 'O índice encontrou uma lacuna ou regra ainda não auditada; o Mestre não deve preencher por memória.' };
}
