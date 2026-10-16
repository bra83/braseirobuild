import fs from 'node:fs';

const bindingsFile = 'app/src/main/assets/vtt/image-library/local/r54/bindings.json';
const indexFile = 'app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json';
const bindings = JSON.parse(fs.readFileSync(bindingsFile, 'utf8'));
const index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
const addAliases = (group, key, aliases) => { bindings[group][key].aliases = [...new Set([...(bindings[group][key].aliases ?? []), ...aliases])]; };
// Only exact lexical variants of the same object are added.
addAliases('equipment', 'traje_ambiente_hostil', ['Equipamento para ambientes adversos']);
addAliases('equipment', 'armadura_pesada', ['Armadura pesada de combate']);
addAliases('equipment', 'armadura_leve', ['Armadura acolchoada']);
addAliases('equipment', 'eletroculos', ['Eletrobinóculos']);
addAliases('equipment', 'scanner_portatil', ['Scanner manual']);
addAliases('equipment', 'medpac', ['Medpac de emergência']);
addAliases('equipment', 'injetor_medico', ['Injetor de estimulantes']);
bindings.equipment.power_pack_or_ammo_reload = { path: 'image-library/local/final/equipment/power_pack_reload.webp', aliases: ['Recarga adicional', 'Power pack or ammo reload'] };
bindings.equipment.glow_rod.aliases = bindings.equipment.glow_rod.aliases.filter((alias) => alias !== 'Lanterna');
bindings.vehicles = {
  digger_crawler: { path: 'image-library/local/final/vehicles/digger_crawler.png', aliases: ['Digger Crawler da Corellian Mining', 'Digger Crawler'] },
  at_est: { path: 'image-library/local/final/vehicles/at_est.png', aliases: ['AT-EST Exploration and Survey Walker', 'AT-EST'] },
  cloakshape: { path: 'image-library/local/final/vehicles/cloakshape.png', aliases: ['Caça estelar CloakShape', 'CloakShape'] },
  jumpmaster_jm5000: { path: 'image-library/local/final/vehicles/jumpmaster_jm5000.png', aliases: ['Batedor de longo alcance JM-5000 JumpMaster', 'JumpMaster JM-5000'] },
  firespray: { path: 'image-library/local/final/vehicles/firespray.png', aliases: ['Nave de patrulha Firespray', 'Firespray'] },
  skipray_gat12h: { path: 'image-library/local/final/vehicles/skipray_gat12h.png', aliases: ['GAT-12h Skipray Blastboat', 'Skipray GAT-12h'] },
  alpha_xg1_star_wing: { path: 'image-library/local/final/vehicles/alpha_xg1_star_wing.png', aliases: ['Canhoneira de assalto Alpha-class XG-1 Star Wing', 'Alpha-class XG-1 Star Wing'] },
  vt49_decimator: { path: 'image-library/local/final/vehicles/vt49_decimator.png', aliases: ['Nave de assalto VT-49 Decimator', 'VT-49 Decimator'] },
  wud500: { path: 'image-library/local/final/vehicles/wud500.png', aliases: ['Iate estelar CEC WUD-500', 'CEC WUD-500'] },
};
const norm = (value) => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
index.exactByName ??= {};
for (const [group, entries] of Object.entries(bindings)) {
  if (group === 'startCount' || !entries || Array.isArray(entries)) continue;
  for (const entry of Object.values(entries)) {
    const path = entry.path ?? (entry.file ? `image-library/local/r54/${entry.file}` : null);
    if (!path) continue;
    for (const alias of entry.aliases ?? []) index.exactByName[norm(alias)] = { path, group, name: alias };
  }
}
index.visualBindingPolicy = { exactOnly: true, noGenericFallbackForEntities: true, localVehiclesIntegrated: Object.keys(bindings.vehicles), unsupportedCatalogEntitiesRemainUnbound: true };
fs.writeFileSync(bindingsFile, JSON.stringify(bindings, null, 2) + '\n', 'utf8');
fs.writeFileSync(indexFile, JSON.stringify(index, null, 2) + '\n', 'utf8');
