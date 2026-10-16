import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');

test('R54 mantém catálogo visual V3.8 fechado e semanticamente presente', () => {
  const manifest = JSON.parse(read('app/src/main/assets/vtt/image-library/manifest.json'));
  assert.equal(manifest.version, '3.8.0');
  assert.deepEqual(manifest.counts, { total: 369, local: 148, external: 221, remoteReady: 221, tokens: 143, equipment: 4, maps: 18, portraits: 37, droids: 23, creatures: 17, kainrathVisualEntities: 77, vehicles: 127, kainrathVehicles: 127 });
  for (const asset of manifest.assets) {
    if (asset.localPath) assert.ok(fs.existsSync(path.join(root, 'app/src/main/assets/vtt', asset.localPath)), asset.id);
  }
});

test('R54 não fixa Nera Voss e não expõe NPC fora da cena', () => {
  const sandbox = read('app/src/main/assets/vtt/js/world/sandbox.mjs');
  assert.doesNotMatch(sandbox, /name:\s*['"]Nera Voss['"]/);
  const bridge = new BraseiroUiBridge({ repository: { load: () => ({
    world: { locations: { loc: { id: 'loc', name: 'Doca', description: 'Doca' } }, npcs: {
      a: { id: 'a', name: 'A', locationId: 'loc', knownToPlayer: true },
      b: { id: 'b', name: 'B', locationId: 'loc', knownToPlayer: true },
    } },
    scene: { locationId: 'loc', visibleNpcIds: ['b'], narration: 'A porta vibra com tiros.' },
    vehicles: {}, combat: {}, session: { status: 'ativa' },
  }) }
  });
  const result = bridge.scenePlayerView();
  assert.equal(result.ok, true);
  const view = result.data;
  assert.deepEqual(view.participants.map((npc) => npc.name), ['B']);
  assert.equal(view.situation, 'A porta vibra com tiros.');
});

test('P21 reiniciar limpa a sessão/snapshots sem apagar personagem nem foto local', () => {
  const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
  const start = runtime.indexOf('function r53RestartAdventure');
  const end = runtime.indexOf('function r53EnsureRestart', start);
  const restart = runtime.slice(start, end);
  assert.match(restart, /restartSandboxPreservingParty/);
  assert.match(restart, /removeItem\(['"]braseiro\.starwars\.edge\.snapshots\.v1['"]\)/);
  assert.doesNotMatch(restart, /removeItem\(['"]braseiro\.starwars\.edge\.state\.v1['"]\)/);
  assert.doesNotMatch(restart, /removeItem\(['"]braseiro\.starwars\.character\.photo\.v1['"]\)/);
  assert.match(restart, /sessionStorage\.clear\(\)/);
});

test('R54 bloqueia acesso universal a arquivos do WebView', () => {
  assert.match(read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'), /setAllowFileAccessFromFileURLs\(false\)/);
});
