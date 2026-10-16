import fs from 'node:fs';

const file = 'app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json';
const index = JSON.parse(fs.readFileSync(file, 'utf8'));
const locations = index.locations ?? {};
for (const [key, record] of Object.entries(locations)) {
  if (key === 'shantipole') {
    record.path = 'image-library/local/r54/maps/shantipole_base.webp';
    record.strategy = 'local_map';
    record.assetId = 'local.r54.map.shantipole';
    record.mapStatus = 'map_local_integrado';
  } else {
    delete record.path;
    delete record.id;
    delete record.assetId;
    record.strategy = 'no_local_map';
    record.mapStatus = 'nenhum_mapa_local_real_no_projeto';
  }
}
for (const record of index.startLocations ?? []) {
  if (String(record.key ?? '').toLowerCase() === 'shantipole') {
    record.path = 'image-library/local/r54/maps/shantipole_base.webp';
    record.strategy = 'local_map';
    record.mapStatus = 'map_local_integrado';
  } else {
    delete record.path;
    record.strategy = 'no_local_map';
    record.mapStatus = 'nenhum_mapa_local_real_no_projeto';
  }
}
index.locationMapPolicy = {
  rule: 'mapa local somente quando o asset for um mapa real explicitamente integrado',
  available: { shantipole: 'image-library/local/r54/maps/shantipole_base.webp' },
  unavailableReason: 'As demais localidades têm coordenadas e identidade, mas não têm mapa local real neste projeto; nenhuma cena genérica é apresentada como mapa.',
};
fs.writeFileSync(file, JSON.stringify(index, null, 2) + '\n', 'utf8');
