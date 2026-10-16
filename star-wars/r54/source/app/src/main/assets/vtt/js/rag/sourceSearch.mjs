import * as lexicalSearch_mjs_1 from './lexicalSearch.mjs';
import * as nativeLibrary_mjs_1 from '../library/nativeLibrary.mjs';
import * as sourceAuthority_mjs_1 from './sourceAuthority.mjs';
import { allowedHybridLibrarySource } from '../adventure/hybridFiveAdventurePolicy.mjs';
export function mergeGroundedSources(verified = [], imported = [], { limit = 5 } = {}) {
    const taggedVerified = verified.map(r => ({ ...r, sourceKind: r.sourceKind ?? 'rule_matrix' }));
    const taggedImported = imported.filter(r => allowedHybridLibrarySource(r)).map(r => ({ ...r, sourceKind: r.sourceKind ?? 'library_pdf' }));
    const dedupe = new Map();
    for (const row of [...taggedVerified, ...taggedImported]) {
        const key = `${row.sourceKind}:${row.id}:${row.page ?? ''}`;
        const current = dedupe.get(key);
        if (!current || Number(row.score ?? 0) > Number(current.score ?? 0))
            dedupe.set(key, row);
    }
    return [...dedupe.values()]
        .sort((a, b) => (0, sourceAuthority_mjs_1.authoritySortScore)(b) - (0, sourceAuthority_mjs_1.authoritySortScore)(a) || Number(b.score ?? 0) - Number(a.score ?? 0) || String(a.id).localeCompare(String(b.id)))
        .slice(0, Math.max(1, Number(limit) || 5))
        .map((r, index) => ({ ...structuredClone(r), rank: index + 1 }));
}
export function searchGroundedSources(query, { limit = 5, includePending = true, librarySearch = nativeLibrary_mjs_1.searchImportedLibrary } = {}) {
    const expanded = Math.max(limit, Math.min(12, limit * 2));
    const verified = (0, lexicalSearch_mjs_1.searchVerifiedRules)(query, { limit: expanded, includePending });
    let imported = [];
    try {
        imported = librarySearch(query, { limit: expanded }) ?? [];
    }
    catch {
        imported = [];
    }
    const results = mergeGroundedSources(verified, imported, { limit });
    const retrieval = { ...(0, lexicalSearch_mjs_1.ruleRetrievalStatus)(results), authority: (0, sourceAuthority_mjs_1.buildAuthorityDecision)(results) };
    return { results, retrieval };
}
