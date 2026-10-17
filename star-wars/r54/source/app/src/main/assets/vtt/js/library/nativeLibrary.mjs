import * as sourceAuthority_mjs_1 from '../rag/sourceAuthority.mjs';
import { allowedHybridLibrarySource } from '../adventure/hybridFiveAdventurePolicy.mjs';
function parseJson(value, fallback) {
    try {
        return JSON.parse(String(value ?? ''));
    }
    catch {
        return fallback;
    }
}
export function nativeLibraryAvailable(bridge = globalThis.BraseiroLibrary) {
    return Boolean(bridge && typeof bridge.listDocuments === 'function' && typeof bridge.searchDocuments === 'function');
}
export function listLibraryDocuments(bridge = globalThis.BraseiroLibrary) {
    if (!nativeLibraryAvailable(bridge))
        return [];
    const parsed = parseJson(bridge.listDocuments(), []);
    return Array.isArray(parsed) ? parsed : [];
}
export function searchImportedLibrary(query, { limit = 5, bridge = globalThis.BraseiroLibrary } = {}) {
    const q = String(query ?? '').trim();
    if (!q || !nativeLibraryAvailable(bridge))
        return [];
    const parsed = parseJson(bridge.searchDocuments(q, Math.max(1, Math.min(20, Number(limit) || 5))), { results: [] });
    const rows = Array.isArray(parsed) ? parsed : parsed?.results;
    if (!Array.isArray(rows))
        return [];
    // Disabled sources remain visible in the Library for explicit reactivation,
    // but never participate in the Mestre/RAG path. This keeps future adventure
    // packs out of the default sandbox until the user explicitly enables them.
    return rows.filter(row => allowedHybridLibrarySource(row)).map((row, index) => ({
        rank: index + 1,
        score: Number(row.score ?? 0),
        id: String(row.id ?? `LIB-${row.docId ?? index}`),
        title: String(row.title ?? row.fileName ?? 'PDF importado'),
        status: 'FONTE_IMPORTADA',
        summary: String(row.summary ?? row.excerpt ?? ''),
        inputs: 'Consulta textual em PDF importado',
        source: String(row.source ?? row.fileName ?? 'Biblioteca local'),
        edition: 'Importado pelo usuário',
        // Keep physical PDF page and printed book page separate. Some imported
        // adventure PDFs have covers/front matter that shift those numbers.
        page: Number.isSafeInteger(Number(row.page)) && Number(row.page) > 0 ? Number(row.page) : null,
        printedPage: Number.isSafeInteger(Number(row.printedPage)) && Number(row.printedPage) > 0 ? Number(row.printedPage) : null,
        function: null,
        test: null,
        official: null,
        sourceKind: 'library_pdf',
        documentId: row.docId ?? null,
        authorityType: (0, sourceAuthority_mjs_1.normalizeLibraryAuthority)(row.authorityType),
        authorityWeight: (0, sourceAuthority_mjs_1.libraryAuthorityWeight)(row.authorityType),
        enabled: row.enabled !== false
    }));
}
export function updateLibraryDocumentPolicy(docId, { authorityType = 'SUPLEMENTO', enabled = true } = {}, bridge = globalThis.BraseiroLibrary) {
    if (!bridge || typeof bridge.setDocumentPolicy !== 'function')
        throw new Error('Ajuste de autoridade indisponível neste ambiente.');
    const normalized = (0, sourceAuthority_mjs_1.normalizeLibraryAuthority)(authorityType);
    const parsed = parseJson(bridge.setDocumentPolicy(String(docId ?? ''), normalized, Boolean(enabled)), { ok: false, message: 'Falha ao atualizar a fonte.' });
    if (!parsed?.ok)
        throw new Error(parsed?.message || 'Falha ao atualizar a fonte.');
    return parsed;
}
export function libraryAuthorityTypeLabel(value) { return (0, sourceAuthority_mjs_1.libraryAuthorityLabel)(value); }
export function requestPdfImport(bridge = globalThis.BraseiroLibrary) {
    if (!bridge || typeof bridge.pickPdf !== 'function')
        throw new Error('A importação de PDF está disponível no aplicativo Android.');
    const parsed = parseJson(bridge.pickPdf(), { ok: false, message: 'Não foi possível abrir o seletor de PDF.' });
    if (parsed?.ok === false)
        throw new Error(parsed.message || 'Não foi possível abrir o seletor de PDF.');
    return parsed;
}
export function reindexLibraryDocument(docId, bridge = globalThis.BraseiroLibrary) {
    if (!bridge || typeof bridge.reindexDocument !== 'function')
        throw new Error('Reindexação indisponível neste ambiente.');
    const parsed = parseJson(bridge.reindexDocument(String(docId ?? '')), { ok: false, message: 'Falha ao iniciar a reindexação.' });
    if (!parsed?.ok)
        throw new Error(parsed?.message || 'Falha ao iniciar a reindexação.');
    return parsed;
}
export function removeLibraryDocument(docId, bridge = globalThis.BraseiroLibrary) {
    if (!bridge || typeof bridge.removeDocument !== 'function')
        throw new Error('Remoção indisponível neste ambiente.');
    const parsed = parseJson(bridge.removeDocument(String(docId ?? '')), { ok: false, message: 'Falha ao remover documento.' });
    if (!parsed?.ok)
        throw new Error(parsed?.message || 'Falha ao remover documento.');
    return parsed;
}
export function libraryStatusLabel(status) {
    const labels = {
        INDEXANDO: 'Indexando',
        INDEXADO: 'Pronto para uso offline',
        ANEXADO_SEM_TEXTO: 'Anexado · PDF sem texto extraível',
        ERRO: 'Falha na indexação'
    };
    return labels[status] ?? String(status ?? 'Desconhecido');
}
