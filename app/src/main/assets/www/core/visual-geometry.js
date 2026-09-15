export const GEOMETRY_SCHEMA = 'BRASEIRO_WWN_APPROVED_COMPOSITION_GEOMETRY_V1';

export function validateGeometry(doc) {
  const errors = [];
  if (doc?.schema !== GEOMETRY_SCHEMA) errors.push(`schema=${doc?.schema}`);
  if (doc?.viewport?.width !== 415 || doc?.viewport?.height !== 915) errors.push('viewport must be 415x915');
  if (!Array.isArray(doc?.layers) || doc.layers.length === 0) errors.push('layers missing');
  const ids = new Set();
  let lastZ = -Infinity;
  for (const layer of doc?.layers ?? []) {
    if (!layer.stable_id || ids.has(layer.stable_id)) errors.push(`duplicate/missing stable_id ${layer.stable_id}`);
    ids.add(layer.stable_id);
    const g = layer.geometry ?? {};
    for (const key of ['x','y','w','h','z']) if (!Number.isFinite(g[key])) errors.push(`${layer.stable_id}.${key} invalid`);
    if (g.w <= 0 || g.h <= 0) errors.push(`${layer.stable_id} nonpositive size`);
    if (g.x < 0 || g.y < 0 || g.x + g.w > 415 || g.y + g.h > 915) errors.push(`${layer.stable_id} out of viewport`);
    if (g.z < lastZ) errors.push(`${layer.stable_id} z-order not monotonic`);
    lastZ = g.z;
    if (layer.state?.aspect_locked !== true) errors.push(`${layer.stable_id} aspect must stay locked`);
  }
  if (doc?.deterministic_state?.bottom_nav_active !== 'explore') errors.push('only explore may be active in SCR-015 fixture');
  return { ok: errors.length === 0, errors, layerCount: doc?.layers?.length ?? 0 };
}

export function applyGeometry(el, geometry) {
  Object.assign(el.style, {
    position: 'absolute',
    left: `${geometry.x}px`,
    top: `${geometry.y}px`,
    width: `${geometry.w}px`,
    height: `${geometry.h}px`,
    zIndex: String(geometry.z),
    opacity: String(geometry.opacity ?? 1),
  });
}
