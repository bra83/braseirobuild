import * as imageLibraryData_mjs_1 from './imageLibraryData.mjs';
import * as imageLibrary_mjs_1 from './imageLibrary.mjs';
function normalize(value) { return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR'); }
function words(value) { return new Set(normalize(value).split(/[^a-z0-9'-]+/).filter(Boolean)); }
function sceneAssets(manifest = imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST) { return (manifest.assets ?? []).filter(a => a.category === 'scene' && a.localPath); }
const CUES = Object.freeze([
    { assetId: 'local.scene.outer-rim.space-combat', weight: 18, terms: ['combate espacial', 'combate veicular', 'cacas tie', 'tie', 'orbita', 'batalha espacial', 'nave de patrulha', 'artilharia dorsal'] },
    { assetId: 'local.scene.imperial.corridor-combat', weight: 16, terms: ['combate', 'stormtrooper', 'corredor', 'blaster', 'tiroteio', 'imperial', 'cobertura'] },
    { assetId: 'local.scene.hyperspace.cockpit', weight: 17, terms: ['hiperespaco', 'salto', 'rota', 'viagem espacial', 'trajeto', 'rumo a', 'navegacao'] },
    { assetId: 'local.scene.tatooine.cantina-night', weight: 17, terms: ['cantina', 'taverna', 'bar', 'mos eisley', 'contrabandista', 'taberneiro', 'balcao', 'social'] },
    { assetId: 'local.scene.jedha.desert-city', weight: 15, terms: ['jedha', 'mercado', 'cidade desertica', 'cidade', 'mercado de rua', 'ruinas urbanas'] },
    { assetId: 'local.scene.outer-rim.spaceport', weight: 14, terms: ['espacoporto', 'hangar', 'cais', 'doca', 'cargueiro', 'pista de pouso', 'porto espacial'] },
    { assetId: 'local.scene.tatooine.jundland-day', weight: 15, terms: ['jundland', 'tatooine', 'deserto', 'planicies', 'dois sois', 'homestead', 'ermo'] },
]);
export function buildSceneVisualContext(state = {}) {
    const world = state.world ?? {};
    const locationId = state.scene?.locationId ?? null;
    const location = locationId ? world.locations?.[locationId] ?? null : null;
    const parts = [state.scene?.title, state.scene?.narration, location?.name, location?.description, state.session?.phase, state.combat?.active ? 'combate' : '', state.vehicles?.combat?.active ? 'combate veicular combate espacial' : ''];
    return parts.filter(Boolean).join(' | ');
}
export function scoreSceneAsset(asset, context) {
    const text = normalize(context);
    const tokenSet = words(context);
    let score = 0;
    const hits = [];
    const cue = CUES.find(c => c.assetId === asset.assetId);
    if (cue) {
        for (const raw of cue.terms) {
            const term = normalize(raw);
            const matched = term.includes(' ') ? text.includes(term) : tokenSet.has(term);
            if (matched) {
                score += cue.weight + (term.includes(' ') ? 4 : 0);
                hits.push(raw);
            }
        }
    }
    for (const tag of (asset.tags ?? [])) {
        const t = normalize(tag);
        if (t && tokenSet.has(t)) {
            score += 3;
            hits.push(`tag:${tag}`);
        }
    }
    if (asset.sceneMode === 'vehicle_combat' && /combate veicular|combate espacial|orbita|tie/.test(text))
        score += 20;
    if (asset.sceneMode === 'combat' && /combate|tiroteio|blaster|stormtrooper/.test(text))
        score += 10;
    if (asset.sceneMode === 'travel' && /viagem|hiperespaco|rota|salto|espacoporto|hangar/.test(text))
        score += 8;
    if (asset.sceneMode === 'social' && /social|cantina|bar|negocia|conversa/.test(text))
        score += 8;
    if (asset.sceneMode === 'exploration' && /explora|deserto|cidade|mercado|jundland|jedha/.test(text))
        score += 7;
    return { score, hits: [...new Set(hits)] };
}
export function selectLocalSceneAsset(context, { manifest = imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, minScore = 10 } = {}) {
    const ranked = sceneAssets(manifest).map(asset => ({ asset, ...scoreSceneAsset(asset, context) })).sort((a, b) => b.score - a.score || a.asset.assetId.localeCompare(b.asset.assetId));
    const best = ranked[0] ?? null;
    if (!best || best.score < minScore)
        return { asset: null, score: best?.score ?? 0, hits: best?.hits ?? [], fallback: 'gemini' };
    return { asset: best.asset, score: best.score, hits: best.hits, fallback: null };
}
export function resolveSceneVisualForState(state, { manifest = imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, minScore = 10 } = {}) {
    const existingId = String(state?.scene?.imageAssetId ?? '').trim();
    if (existingId) {
        const existing = (manifest.assets ?? []).find(a => a.assetId === existingId && a.category === 'scene') ?? null;
        if (existing)
            return { asset: existing, descriptor: imageLibrary_mjs_1.imageAssetRuntimeDescriptor(manifest, existing), source: state.scene?.imageSource ?? 'library', locked: Boolean(state.scene?.imageLocked), score: null, hits: [] };
    }
    const context = buildSceneVisualContext(state);
    const selected = selectLocalSceneAsset(context, { manifest, minScore });
    return { asset: selected.asset, descriptor: selected.asset ? imageLibrary_mjs_1.imageAssetRuntimeDescriptor(manifest, selected.asset) : null, source: selected.asset ? 'library' : 'gemini_fallback', locked: false, score: selected.score, hits: selected.hits, context };
}
export function applyAutomaticSceneVisual(state, { manifest = imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, minScore = 10 } = {}) {
    const next = structuredClone(state);
    next.scene = { title: 'Abertura', narration: '', locationId: null, imageAssetId: null, imageSource: null, imageLocked: false, imagePrompt: null, ...(next.scene ?? {}) };
    if (next.scene.imageLocked && next.scene.imageAssetId)
        return { state: next, changed: false, reason: 'locked' };
    const resolved = resolveSceneVisualForState({ ...next, scene: { ...next.scene, imageAssetId: null } }, { manifest, minScore });
    const assetId = resolved.asset?.assetId ?? null;
    const changed = next.scene.imageAssetId !== assetId || next.scene.imageSource !== (assetId ? 'library' : 'gemini_fallback');
    next.scene.imageAssetId = assetId;
    next.scene.imageSource = assetId ? 'library' : 'gemini_fallback';
    if (!assetId)
        next.scene.imagePrompt = buildSceneVisualContext(next);
    return { state: next, changed, reason: assetId ? 'local_match' : 'gemini_fallback', asset: resolved.asset, score: resolved.score, hits: resolved.hits };
}
