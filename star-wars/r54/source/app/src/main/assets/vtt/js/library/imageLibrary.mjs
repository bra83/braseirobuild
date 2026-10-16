const ALLOWED_CATEGORIES = new Set(['token', 'equipment', 'map', 'scene', 'portrait', 'vehicle', 'creature', 'droid', 'template']);
const ALLOWED_SOURCE_TYPES = new Set(['user_generated', 'user_provided', 'project_packaged', 'generated_runtime', 'external_reference']);
function text(value) { return typeof value === 'string' ? value.trim() : ''; }
function norm(value) { return text(value).toLocaleLowerCase('pt-BR'); }
function asArray(value) { return Array.isArray(value) ? value : []; }
export function isAllowedExternalImageUrl(value, policy = {}) {
    const raw = text(value);
    if (!raw)
        return false;
    try {
        const url = new URL(raw);
        if (url.protocol !== 'https:')
            return false;
        const hosts = asArray(policy.allowedRemoteHosts).map(norm).filter(Boolean);
        const prefixes = asArray(policy.allowedRemotePathPrefixes).map(text).filter(Boolean);
        if (hosts.length && !hosts.includes(norm(url.hostname)))
            return false;
        if (prefixes.length && !prefixes.some(prefix => url.pathname.startsWith(prefix)))
            return false;
        return true;
    }
    catch {
        return false;
    }
}
function validateVariant(variant, index, assetIndex, policy, errors) {
    if (!text(variant?.variantId))
        errors.push(`asset-${assetIndex}-variant-${index}-id-required`);
    if (!text(variant?.remoteUrl))
        errors.push(`asset-${assetIndex}-variant-${index}-remote-url-required`);
    else if (!isAllowedExternalImageUrl(variant.remoteUrl, policy))
        errors.push(`asset-${assetIndex}-variant-${index}-remote-url-not-allowed`);
}
export function validateImageManifest(manifest) {
    const errors = [];
    if (!manifest || typeof manifest !== 'object')
        return { ok: false, errors: ['manifest-object-required'] };
    if (![1, 2].includes(manifest.schemaVersion))
        errors.push('schema-version-unsupported');
    if (!text(manifest.libraryId))
        errors.push('library-id-required');
    if (!Array.isArray(manifest.assets))
        errors.push('assets-array-required');
    const ids = new Set();
    const policy = manifest.policy || {};
    for (const [index, asset] of (manifest.assets || []).entries()) {
        if (!text(asset?.assetId))
            errors.push(`asset-${index}-id-required`);
        else if (ids.has(asset.assetId))
            errors.push(`asset-${index}-duplicate-id`);
        else
            ids.add(asset.assetId);
        if (!text(asset?.title))
            errors.push(`asset-${index}-title-required`);
        if (!ALLOWED_CATEGORIES.has(asset?.category))
            errors.push(`asset-${index}-category-invalid`);
        if (asset?.sourceType && !ALLOWED_SOURCE_TYPES.has(asset.sourceType))
            errors.push(`asset-${index}-source-type-invalid`);
        if (asset?.sourceType === 'external_reference') {
            if (!text(asset?.sourcePageUrl))
                errors.push(`asset-${index}-source-page-required`);
            if (asset?.remoteReady === true) {
                if (!text(asset?.remoteUrl))
                    errors.push(`asset-${index}-remote-url-required`);
                else if (!isAllowedExternalImageUrl(asset.remoteUrl, policy))
                    errors.push(`asset-${index}-remote-url-not-allowed`);
            }
            asArray(asset?.variants).forEach((variant, variantIndex) => validateVariant(variant, variantIndex, index, policy, errors));
        }
        else if (!text(asset?.localPath)) {
            errors.push(`asset-${index}-path-required`);
        }
        if (asset?.mechanicalBinding !== null && asset?.mechanicalBinding !== undefined && typeof asset.mechanicalBinding !== 'object')
            errors.push(`asset-${index}-mechanical-binding-invalid`);
        if (asset?.category === 'map' && asset?.mapKind !== undefined && !text(asset.mapKind))
            errors.push(`asset-${index}-map-kind-invalid`);
    }
    return { ok: errors.length === 0, errors, count: manifest.assets?.length || 0 };
}
export function getImageAsset(manifest, assetId) {
    return (manifest?.assets || []).find(asset => asset.assetId === assetId) || null;
}
export function searchImageAssets(manifest, { query = '', category = null, subtype = null, mapKind = null, tags = [], usage = null, sourceType = null, remoteReady = null, offlineReady = null, limit = 100 } = {}) {
    const q = norm(query);
    const wantedTags = asArray(tags).map(norm).filter(Boolean);
    return (manifest?.assets || [])
        .filter(asset => !category || asset.category === category)
        .filter(asset => !subtype || asset.subtype === subtype)
        .filter(asset => !mapKind || asset.mapKind === mapKind)
        .filter(asset => !usage || asset.usage === usage)
        .filter(asset => !sourceType || asset.sourceType === sourceType)
        .filter(asset => remoteReady === null || remoteReady === undefined || Boolean(asset.remoteReady) === Boolean(remoteReady))
        .filter(asset => offlineReady === null || offlineReady === undefined || Boolean(asset.offlineReady ?? asset.localPath) === Boolean(offlineReady))
        .filter(asset => wantedTags.every(tag => asArray(asset.tags).map(norm).includes(tag)))
        .filter(asset => {
        if (!q)
            return true;
        const hay = [asset.assetId, asset.title, asset.category, asset.subtype, asset.mapKind, asset.sourceName, asset.sourceRef, ...asArray(asset.tags)].map(norm).join(' ');
        return hay.includes(q);
    })
        .slice(0, Math.max(0, Math.min(Number(limit) || 100, 500)));
}
export function chooseImageAsset(manifest, criteria = {}) {
    const candidates = searchImageAssets(manifest, { ...criteria, limit: 500 });
    if (!candidates.length)
        return null;
    // seleção determinística; não inferir identidade, regra, facção ou espécie.
    return [...candidates].sort((a, b) => a.assetId.localeCompare(b.assetId))[0];
}
export function imageAssetRuntimeDescriptor(manifest, asset) {
    if (!asset)
        return null;
    if (asset.sourceType === 'external_reference') {
        return {
            assetId: asset.assetId,
            mode: 'external_cache_required',
            remoteReady: Boolean(asset.remoteReady && isAllowedExternalImageUrl(asset.remoteUrl, manifest?.policy || {})),
            remoteUrl: asset.remoteUrl || null,
            sourcePageUrl: asset.sourcePageUrl || null,
            cachePolicy: asset.cachePolicy || 'explicit_on_demand',
            offlineReady: false,
        };
    }
    return {
        assetId: asset.assetId,
        mode: 'packaged',
        localPath: asset.localPath,
        offlineReady: true,
    };
}
