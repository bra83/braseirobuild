import { migrateState, validateState } from './state.mjs';

export const R21_STATE_CHANGED_EVENT = 'braseiro:r21-state-changed';

function emitStateChanged(eventTarget, key, operation) {
  if (!eventTarget || typeof eventTarget.dispatchEvent !== 'function') return false;
  const EventCtor = eventTarget.CustomEvent ?? globalThis.CustomEvent;
  try {
    if (typeof EventCtor === 'function') {
      eventTarget.dispatchEvent(new EventCtor(R21_STATE_CHANGED_EVENT, { detail: { key, operation } }));
      return true;
    }
    const BaseEvent = eventTarget.Event ?? globalThis.Event;
    if (typeof BaseEvent !== 'function') return false;
    const event = new BaseEvent(R21_STATE_CHANGED_EVENT);
    try { Object.defineProperty(event, 'detail', { value: { key, operation }, enumerable: true }); } catch {}
    eventTarget.dispatchEvent(event);
    return true;
  } catch {
    return false;
  }
}

export class LocalStorageRepository {
  constructor(storage = globalThis.localStorage, key = 'braseiro.starwars.edge.state.v1', eventTarget = globalThis) {
    this.storage = storage;
    this.key = key;
    this.eventTarget = eventTarget;
  }
  save(state) {
    validateState(state);
    this.storage.setItem(this.key, JSON.stringify(state));
    emitStateChanged(this.eventTarget, this.key, 'save');
    return state;
  }
  load() {
    const raw = this.storage.getItem(this.key);
    if (!raw) return null;
    const state = migrateState(JSON.parse(raw));
    validateState(state);
    return state;
  }
  clear() {
    this.storage.removeItem(this.key);
    emitStateChanged(this.eventTarget, this.key, 'clear');
  }
}

export function exportStateJson(state) {
  validateState(state);
  return JSON.stringify(state, null, 2);
}

export function importStateJson(text) {
  const parsed = migrateState(JSON.parse(text));
  validateState(parsed);
  return parsed;
}
