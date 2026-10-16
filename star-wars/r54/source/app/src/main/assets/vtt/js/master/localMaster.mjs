import * as context_mjs_1 from './context.mjs';
export function consultRulesLocal(state, query, { limit = 5 } = {}) {
    if (!String(query ?? '').trim())
        throw new Error('Digite o que deseja consultar nas regras.');
    const context = (0, context_mjs_1.buildMasterContext)(state, { ruleQuery: query, intentText: '', maxRules: limit });
    return {
        query: String(query).trim(),
        grounded: context.retrieval.grounded,
        message: context.retrieval.grounded
            ? 'Encontrei regras auditadas pertinentes.'
            : context.retrieval.reason,
        results: context.rules.map(r => ({
            id: r.id, title: r.title, status: r.status, summary: r.summary, source: r.source, edition: r.edition, page: r.page, printedPage: r.printedPage, official: r.official,
            sourceKind: r.sourceKind ?? null, documentId: r.documentId ?? null, authorityType: r.authorityType ?? null, authorityWeight: r.authorityWeight ?? null
        })),
        retrieval: context.retrieval,
        contextDigest: (0, context_mjs_1.contextDigest)(context)
    };
}
export function prepareLocalMasterTurn(state, intentText) {
    if (!String(intentText ?? '').trim())
        throw new Error('Digite uma ação.');
    const context = (0, context_mjs_1.buildMasterContext)(state, { intentText, ruleQuery: intentText, maxRules: 4 });
    return {
        provider: 'local-context-only',
        canNarrateFreely: false,
        context,
        digest: (0, context_mjs_1.contextDigest)(context),
        playerMessage: context.retrieval.grounded
            ? `Ação registrada. O Mestre local preparou contexto com ${context.rules.length} regra(s) pertinente(s); a narração livre ainda requer um provedor de IA configurado.`
            : 'Ação registrada. O Mestre local preparou o estado da cena, mas não encontrou regra auditada pertinente; não vou inventar uma regra para completar a resolução.'
    };
}
