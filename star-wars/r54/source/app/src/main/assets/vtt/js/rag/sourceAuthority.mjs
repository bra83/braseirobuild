export const LIBRARY_AUTHORITY_TYPES = Object.freeze({
    ERRATA: { weight: 500, label: 'Errata / correção' },
    BASE: { weight: 400, label: 'Livro básico' },
    SUPLEMENTO: { weight: 300, label: 'Complemento de regras' },
    AVENTURA: { weight: 200, label: 'Aventura / cenário' },
    REFERENCIA: { weight: 100, label: 'Referência' }
});
export function normalizeLibraryAuthority(value = 'SUPLEMENTO') {
    const key = String(value ?? '').trim().toUpperCase();
    return Object.prototype.hasOwnProperty.call(LIBRARY_AUTHORITY_TYPES, key) ? key : 'SUPLEMENTO';
}
export function libraryAuthorityWeight(value) {
    return LIBRARY_AUTHORITY_TYPES[normalizeLibraryAuthority(value)].weight;
}
export function libraryAuthorityLabel(value) {
    return LIBRARY_AUTHORITY_TYPES[normalizeLibraryAuthority(value)].label;
}
export function sourceAuthorityBand(row = {}) {
    if (row.sourceKind === 'rule_matrix') {
        if (['TESTADO', 'IMPLEMENTADO'].includes(row.status))
            return 10000;
        return 2000;
    }
    if (row.sourceKind === 'library_pdf')
        return 3000 + libraryAuthorityWeight(row.authorityType);
    return 1000;
}
export function authoritySortScore(row = {}) {
    return sourceAuthorityBand(row) * 1000 + Math.min(999, Math.max(0, Number(row.score ?? 0)) * 10);
}
export function buildAuthorityDecision(results = []) {
    if (!results.length)
        return { policyVersion: 1, primary: null, precedenceApplied: false, competingSources: 0 };
    const primary = results[0];
    const sourceIds = new Set(results.map(r => r.documentId || `${r.sourceKind}:${r.source ?? r.id}`));
    const importedTypes = new Set(results.filter(r => r.sourceKind === 'library_pdf').map(r => normalizeLibraryAuthority(r.authorityType)));
    return {
        policyVersion: 1,
        primary: { id: primary.id, sourceKind: primary.sourceKind, status: primary.status, authorityType: primary.authorityType ?? null, documentId: primary.documentId ?? null },
        precedenceApplied: sourceIds.size > 1 && (importedTypes.size > 1 || results.some(r => r.sourceKind === 'rule_matrix')),
        competingSources: Math.max(0, sourceIds.size - 1),
        rule: 'Mecânica TESTADA/IMPLEMENTADA do motor prevalece. Entre PDFs importados: Errata > Livro básico > Complemento > Aventura/Cenário > Referência.'
    };
}
