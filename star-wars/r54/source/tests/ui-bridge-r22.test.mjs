import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { BraseiroUiBridge, UI_BRIDGE_VERSION, installBraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

function memoryStorage(initial = {}) {
  const map = new Map(Object.entries(initial));
  return {
    map,
    getItem(key) { return map.has(key) ? map.get(key) : null; },
    setItem(key, value) { map.set(key, String(value)); },
    removeItem(key) { map.delete(key); },
  };
}

const KEY = 'braseiro.starwars.edge.state.v1';

test('bridge construction is zero-mutation when no campaign exists', () => {
  const storage = memoryStorage();
  const bridge = new BraseiroUiBridge({ storage });
  assert.equal(UI_BRIDGE_VERSION, 3);
  assert.equal(storage.map.size, 0);
  const read = bridge.readState();
  assert.equal(read.ok, true);
  assert.equal(read.data, null);
  assert.equal(storage.map.size, 0);
});

test('player intent never creates a default campaign when storage is empty', async () => {
  const storage = memoryStorage();
  const bridge = new BraseiroUiBridge({ storage });
  const result = await bridge.submitPlayerIntent('Observo a porta.');
  assert.equal(result.ok, false);
  assert.equal(result.error, 'NO_CAMPAIGN');
  assert.equal(storage.map.size, 0);
});

test('corrupt canonical state is reported and never overwritten', async () => {
  const storage = memoryStorage({ [KEY]: '{corrompido' });
  const before = storage.getItem(KEY);
  const bridge = new BraseiroUiBridge({ storage });
  const read = bridge.readState();
  assert.equal(read.ok, false);
  assert.equal(read.error, 'STATE_READ_FAILED');
  const action = await bridge.submitPlayerIntent('Espero.');
  assert.equal(action.ok, false);
  assert.equal(action.error, 'STATE_READ_FAILED');
  assert.equal(storage.getItem(KEY), before);
});

test('local player intent delegates to the canonical GameController and persists it', async () => {
  const storage = memoryStorage();
  const repo = new LocalStorageRepository(storage);
  repo.save(createCampaign('Ponte R22'));
  const bridge = new BraseiroUiBridge({ storage });
  const result = await bridge.submitPlayerIntent('Examino o corredor.');
  assert.equal(result.ok, true);
  assert.equal(result.data.mode, 'local');
  const persisted = repo.load();
  assert.equal(persisted.name, 'Ponte R22');
  assert.equal(persisted.events.some(e => e.type === 'PLAYER_INTENT' && e.payload?.text === 'Examino o corredor.'), true);
  assert.equal(result.data.state.events.some(e => e.type === 'PLAYER_INTENT'), true);
});



test('configured HTTP provider follows the canonical requestMasterTurn path', async () => {
  const storage = memoryStorage();
  const repo = new LocalStorageRepository(storage);
  const state = createCampaign('Ponte HTTP');
  state.settings.masterProvider = 'http';
  state.settings.masterEndpoint = 'https://mestre.example/api';
  repo.save(state);
  let requestedUrl = null;
  const fetchImpl = async (url) => {
    requestedUrl = url;
    return {
      ok: true,
      status: 200,
      json: async () => ({ proposal: {
        sceneTitle: 'Corredor de serviço',
        narration: 'Luzes vermelhas piscam no corredor.',
        askPlayer: 'Você avança ou se esconde?',
        sourceRuleIds: [],
      } }),
    };
  };
  const bridge = new BraseiroUiBridge({ storage });
  const result = await bridge.submitPlayerIntent('Olho o corredor.', { fetchImpl, timeoutMs: 100 });
  assert.equal(result.ok, true);
  assert.equal(result.data.mode, 'http');
  assert.equal(requestedUrl, 'https://mestre.example/api');
  assert.equal(repo.load().scene.narration, 'Luzes vermelhas piscam no corredor.');
});


test('global bridge installation is idempotent and does not create campaign state', () => {
  const storage = memoryStorage();
  const target = {};
  const first = installBraseiroUiBridge(target, { storage });
  const second = installBraseiroUiBridge(target, { storage });
  assert.equal(first, second);
  assert.equal(target.BraseiroGameBridge, first);
  assert.equal(storage.map.size, 0);
});
test('blank player intent is rejected before controller construction or mutation', async () => {
  const storage = memoryStorage();
  const repo = new LocalStorageRepository(storage);
  repo.save(createCampaign('Sem mutação'));
  const before = storage.getItem(KEY);
  const bridge = new BraseiroUiBridge({ storage });
  const result = await bridge.submitPlayerIntent('   ');
  assert.equal(result.ok, false);
  assert.equal(result.error, 'INVALID_INPUT');
  assert.equal(storage.getItem(KEY), before);
});
