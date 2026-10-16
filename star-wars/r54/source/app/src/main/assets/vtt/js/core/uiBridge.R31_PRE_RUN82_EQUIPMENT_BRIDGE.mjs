import { LocalStorageRepository, exportStateJson, importStateJson } from './storage.mjs';
import { LocalSnapshotRepository } from './snapshots.mjs';
import { importCharacterJson } from '../rules/character.mjs';
import { GameController } from './controller.mjs';

export const UI_BRIDGE_VERSION = 3;

function clone(value) {
  return value === undefined ? undefined : structuredClone(value);
}

function messageOf(error) {
  return error instanceof Error ? error.message : String(error ?? 'Erro desconhecido.');
}

function ok(data) {
  return { ok: true, data };
}

function fail(error, message) {
  return { ok: false, error, message };
}

export class BraseiroUiBridge {
  constructor({ repository = null, storage = globalThis.localStorage, snapshots = null } = {}) {
    this.repository = repository ?? new LocalStorageRepository(storage);
    const snapshotStorage = this.repository?.storage ?? storage;
    this.snapshots = snapshots ?? new LocalSnapshotRepository(snapshotStorage);
  }

  readState() {
    try {
      const state = this.repository.load();
      return ok(state ? clone(state) : null);
    } catch (error) {
      return fail('STATE_READ_FAILED', messageOf(error));
    }
  }

  _controllerForExistingCampaign() {
    let existing;
    try {
      existing = this.repository.load();
    } catch (error) {
      return fail('STATE_READ_FAILED', messageOf(error));
    }
    if (!existing) return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
    try {
      return ok(new GameController(this.repository));
    } catch (error) {
      return fail('STATE_READ_FAILED', messageOf(error));
    }
  }



  exportCharacterJson() {
    const controllerResult = this._controllerForExistingCampaign();
    if (!controllerResult.ok) return controllerResult;
    try {
      const game = controllerResult.data;
      const json = game.exportCharacter();
      const name = String(game.state.player?.character?.name ?? 'personagem').trim() || 'personagem';
      const safeName = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9._-]+/g, '_').replace(/^_+|_+$/g, '') || 'personagem';
      return ok({ fileName: `${safeName}.json`, json });
    } catch (error) {
      return fail('CHARACTER_EXPORT_FAILED', messageOf(error));
    }
  }

  generateRandomCharacter(options = {}, { createSnapshot = true } = {}) {
    const controllerResult = this._controllerForExistingCampaign();
    if (!controllerResult.ok) return controllerResult;
    const game = controllerResult.data;
    let character;
    try {
      character = game.buildRandomCharacter(options);
    } catch (error) {
      return fail('RANDOM_CHARACTER_FAILED', messageOf(error));
    }
    try {
      let snapshotId = null;
      if (createSnapshot && game.state.player?.character) snapshotId = this.snapshots.create(game.state, 'Antes de gerar ficha aleatória').id;
      const created = game.setCharacter(character);
      return ok({ character: clone(created), snapshotId, state: clone(game.state) });
    } catch (error) {
      return fail('RANDOM_CHARACTER_FAILED', messageOf(error));
    }
  }

  importCharacterJson(text, { createSnapshot = true } = {}) {
    const controllerResult = this._controllerForExistingCampaign();
    if (!controllerResult.ok) return controllerResult;
    let character;
    try {
      character = importCharacterJson(String(text ?? ''));
    } catch (error) {
      return fail('CHARACTER_IMPORT_FAILED', messageOf(error));
    }
    const game = controllerResult.data;
    try {
      let snapshotId = null;
      if (createSnapshot) snapshotId = this.snapshots.create(game.state, 'Antes de importar ficha').id;
      const imported = game.setCharacter(character);
      return ok({ character: clone(imported), snapshotId, state: clone(game.state) });
    } catch (error) {
      return fail('CHARACTER_IMPORT_FAILED', messageOf(error));
    }
  }

  exportCampaignBackup() {
    let state;
    try { state = this.repository.load(); }
    catch (error) { return fail('STATE_READ_FAILED', messageOf(error)); }
    if (!state) return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
    try {
      const json = exportStateJson(state);
      const name = String(state.name ?? 'campanha').trim() || 'campanha';
      const safeName = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9._-]+/g, '_').replace(/^_+|_+$/g, '') || 'campanha';
      return ok({ fileName: `${safeName}.backup.json`, json });
    } catch (error) {
      return fail('BACKUP_EXPORT_FAILED', messageOf(error));
    }
  }

  importCampaignBackup(text, { createSnapshot = true } = {}) {
    let imported;
    try { imported = importStateJson(String(text ?? '')); }
    catch (error) { return fail('BACKUP_IMPORT_FAILED', messageOf(error)); }

    let existing = null;
    try { existing = this.repository.load(); }
    catch (error) { return fail('STATE_READ_FAILED', messageOf(error)); }

    try {
      let snapshotId = null;
      if (createSnapshot && existing) snapshotId = this.snapshots.create(existing, 'Antes de restaurar backup').id;
      this.repository.save(imported);
      return ok({ state: clone(imported), snapshotId });
    } catch (error) {
      return fail('BACKUP_IMPORT_FAILED', messageOf(error));
    }
  }

  listSnapshots() {
    try {
      return ok(this.snapshots.list().map(item => ({ id: item.id, label: item.label, createdAt: item.createdAt })));
    } catch (error) {
      return fail('SNAPSHOT_READ_FAILED', messageOf(error));
    }
  }

  createSnapshot(label = 'backup manual') {
    let state;
    try { state = this.repository.load(); }
    catch (error) { return fail('STATE_READ_FAILED', messageOf(error)); }
    if (!state) return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
    try {
      const item = this.snapshots.create(state, String(label ?? 'backup manual').trim() || 'backup manual');
      return ok({ id: item.id, label: item.label, createdAt: item.createdAt });
    } catch (error) {
      return fail('SNAPSHOT_CREATE_FAILED', messageOf(error));
    }
  }

  restoreSnapshot(id) {
    let before = null;
    try { before = this.repository.load(); }
    catch (error) { return fail('STATE_READ_FAILED', messageOf(error)); }
    if (!before) return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
    try {
      const restored = this.snapshots.restore(String(id ?? ''));
      this.repository.save(restored);
      return ok({ state: clone(restored) });
    } catch (error) {
      return fail('SNAPSHOT_RESTORE_FAILED', messageOf(error));
    }
  }

  async submitPlayerIntent(text, { fetchImpl = globalThis.fetch, timeoutMs = 15000, signal = null } = {}) {
    const intentText = String(text ?? '').trim();
    if (!intentText) return fail('INVALID_INPUT', 'Digite uma ação.');

    const controllerResult = this._controllerForExistingCampaign();
    if (!controllerResult.ok) return controllerResult;
    const game = controllerResult.data;

    try {
      if (game.state.settings?.masterProvider === 'http') {
        const result = await game.requestMasterTurn({
          intentText,
          endpoint: game.state.settings.masterEndpoint,
          fetchImpl,
          timeoutMs,
          signal,
        });
        return ok({ mode: 'http', result: clone(result), state: clone(game.state) });
      }

      const result = game.submitIntent(intentText);
      return ok({ mode: 'local', result: clone(result), state: clone(game.state) });
    } catch (error) {
      return fail('ENGINE_REJECTED', messageOf(error));
    }
  }
}

export function createBraseiroUiBridge(options = {}) {
  return new BraseiroUiBridge(options);
}


export function installBraseiroUiBridge(target = globalThis, options = {}) {
  if (!target || (typeof target !== 'object' && typeof target !== 'function')) throw new Error('Alvo global inválido para a ponte de UI.');
  if (target.BraseiroGameBridge) return target.BraseiroGameBridge;
  const bridge = createBraseiroUiBridge(options);
  Object.defineProperty(target, 'BraseiroGameBridge', {
    value: bridge,
    configurable: false,
    enumerable: false,
    writable: false,
  });
  return bridge;
}
