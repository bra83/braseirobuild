import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const vtt = path.join(root, 'app/src/main/assets/vtt');
const readJson = (file) => JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
const norm = (value) => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();

test('auditoria visual independente: locais distinguem mapa dedicado de esquema gerado', () => {
  const index = readJson('app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json');
  assert.equal(index.locations.shantipole.strategy, 'local_map');
  assert.equal(index.locations.shantipole.path, 'image-library/local/r54/maps/shantipole_base.webp');
  for (const [key, location] of Object.entries(index.locations)) {
    assert.ok(location.mapStatus, key);
    assert.ok(location.path, `${key} sem mapa`);
    assert.ok(fs.existsSync(path.join(vtt, location.path)), `${key}: ${location.path}`);
    assert.ok(['local_map','generated_play_map'].includes(location.strategy), key);
    if (location.provenance === 'generated_from_location_profile') assert.equal(location.strategy, 'generated_play_map', key);
  }
  assert.equal((index.startLocations ?? []).filter((location) => location.path).length, 82);
  assert.equal(index.locationMapPolicy.localMaps, 82);
  assert.equal(index.locationMapPolicy.galacticLocatorMaps, 0);
  assert.match(index.locationMapPolicy.labelPolicy, /(não|nunca).*cartografia oficial/i);
});

test('auditoria visual independente: bindings de itens usam o objeto certo e todos os paths existem', () => {
  const bindings = readJson('app/src/main/assets/vtt/image-library/local/r54/bindings.json');
  const paths = [];
  for (const group of ['weapons', 'equipment', 'vehicles']) {
    for (const [key, item] of Object.entries(bindings[group] ?? {})) {
      const assetPath = item.path ?? (item.file ? `image-library/local/r54/${item.file}` : null);
      assert.ok(assetPath, `${group}.${key} sem path`);
      paths.push(assetPath);
      assert.ok(fs.existsSync(path.join(vtt, assetPath)), `${group}.${key}: ${assetPath}`);
    }
  }
  assert.equal(bindings.weapons.pistola_blaster.file, 'weapons/pistola_blaster.webp');
  assert.equal(bindings.weapons.pistola_blaster_pesada.file, 'weapons/pistola_blaster_pesada.webp');
  assert.notEqual(bindings.weapons.pistola_blaster.file, bindings.weapons.pistola_blaster_pesada.file);
  assert.ok(bindings.equipment.power_pack_or_ammo_reload.path.endsWith('/power_pack_reload.webp'));
  assert.equal(bindings.equipment.glow_rod.aliases.includes('Lanterna'), false);
  assert.equal(new Set(paths).size, paths.length, 'dois bindings diferentes reutilizam o mesmo asset local');
});

test('auditoria visual independente: índice P6 e UI não contêm fallback genérico para entidades', () => {
  const index = readJson('app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json');
  const p6 = fs.readFileSync(path.join(vtt, 'r54-device-p6.js'), 'utf8');
  assert.equal(index.visualBindingPolicy.exactOnly, true);
  assert.equal(index.visualBindingPolicy.noGenericFallbackForEntities, true);
  assert.match(p6, /repairVehicleImage/);
  assert.match(p6, /repairParticipantImages/);
  assert.match(p6, /Nenhum asset visual local específico está integrado/);
  assert.match(p6, /local_map/);
  assert.doesNotMatch(p6, /fallback.*local.*map/i);
  const runtime = fs.readFileSync(path.join(vtt, 'r46/final-runtime.js'), 'utf8');
  assert.doesNotMatch(runtime, /mos-eisley-city|tatooine-map/);
  assert.match(runtime, /usa esquema local de jogo claramente identificado/);
  assert.doesNotMatch(runtime, /Cada local possui mapa local jogável/);
});

test('auditoria visual independente: todos os caminhos do índice P6 que representam imagens são resolvíveis', () => {
  const index = readJson('app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json');
  for (const record of index.records) {
    if (!record.path || /^https?:/i.test(record.path)) continue;
    assert.ok(fs.existsSync(path.join(vtt, record.path)), `${record.assetId ?? 'sem-id'}: ${record.path}`);
  }
  for (const [name, visual] of Object.entries(index.exactByName)) {
    assert.ok(visual.path, name);
    if (!/^https?:/i.test(visual.path)) assert.ok(fs.existsSync(path.join(vtt, visual.path)), `${name}: ${visual.path}`);
  }
});
