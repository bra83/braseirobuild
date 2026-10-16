/* Braseiro R54 P146 — crash-safe Android WebView command outbox.
 * Persist BEFORE dispatch. Retry the same ID after reload, never roll again.
 * Do not use this outbox for sandbox actions or TTS.
 */
(function (root) {
  'use strict';
  const KEY = 'braseiro.starwars.published.pending.v1';
  const TYPES = new Set(['start', 'choice', 'check', 'encounter', 'finish-combat', 'imperial-clock']);
  const ID = /^[A-Za-z0-9][A-Za-z0-9_.:/-]{0,127}$/;
  const resultError = (error, message) => ({ ok: false, error, message });
  const own = (object, key) => Object.prototype.hasOwnProperty.call(object, key);
  const storage = () => root.localStorage;
  const bridge = () => root.BraseiroGameBridge;

  function state() {
    const game = bridge();
    if (!game || typeof game.readState !== 'function')
      throw new Error('Motor de persistência da campanha indisponível.');
    const result = game.readState();
    if (!result?.ok || !result.data || typeof result.data.campaignId !== 'string')
      throw new Error(result?.message || 'Campanha persistida indisponível.');
    return result.data;
  }
  function generateId() {
    const crypto = root.crypto;
    if (crypto && typeof crypto.randomUUID === 'function')
      return `pub:${crypto.randomUUID()}`;
    if (crypto && typeof crypto.getRandomValues === 'function') {
      const bytes = new Uint8Array(16);
      crypto.getRandomValues(bytes);
      return `pub:${Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')}`;
    }
    return `pub:${Date.now().toString(36)}:${Math.random().toString(36).slice(2, 12)}`;
  }
  function command(kind, adventureId, args = {}) {
    if (!TYPES.has(kind) || typeof adventureId !== 'string' || !ID.test(adventureId))
      throw new Error('Ação de aventura inválida.');
    if (!args || typeof args !== 'object' || Array.isArray(args))
      throw new Error('Argumentos de ação inválidos.');
    const allowed = kind === 'choice' ? ['choiceId'] :
      kind === 'check' ? ['checkIndex', 'options'] :
      kind === 'encounter' ? ['encounterId', 'options'] :
      kind === 'finish-combat' ? ['options'] :
      kind === 'imperial-clock' ? ['minutes'] : [];
    if (Object.keys(args).some(key => !allowed.includes(key)))
      throw new Error('Argumentos não suportados para a ação publicada.');
    const normalized = { kind, adventureId };
    if (kind === 'choice') normalized.choiceId = String(args.choiceId ?? '');
    if (kind === 'check') normalized.checkIndex = Number(args.checkIndex ?? 0);
    if (kind === 'encounter') normalized.encounterId = args.encounterId == null ? null : String(args.encounterId);
    if (kind === 'imperial-clock') normalized.minutes = Number(args.minutes ?? 5);
    if (['check', 'encounter', 'finish-combat'].includes(kind)) {
      const options = args.options ?? {};
      if (!options || typeof options !== 'object' || Array.isArray(options))
        throw new Error('Opções de ação inválidas.');
      normalized.options = JSON.parse(JSON.stringify(options));
    }
    const encoded = JSON.stringify(normalized);
    if (encoded.length > 16384 || !Number.isFinite(normalized.checkIndex ?? 0) || !Number.isSafeInteger(normalized.minutes ?? 5) || (normalized.minutes ?? 5)<1 || (normalized.minutes ?? 5)>30)
      throw new Error('Ação publicada excede os limites permitidos.');
    return normalized;
  }
  function pending() {
    const raw = storage().getItem(KEY);
    if (!raw) return null;
    let entry;
    try { entry = JSON.parse(raw); } catch { throw new Error('Comando pendente corrompido; não será descartado automaticamente.'); }
    if (!entry || entry.version !== 1 || !ID.test(entry.actionId) ||
        typeof entry.campaignId !== 'string' || !ID.test(entry.campaignId) ||
        !entry.command || typeof entry.command !== 'object' ||
        entry.fingerprint !== JSON.stringify(entry.command) ||
        JSON.stringify(command(entry.command.kind, entry.command.adventureId,
          Object.fromEntries(Object.entries(entry.command).filter(([key]) => key !== 'kind' && key !== 'adventureId')))) !== entry.fingerprint)
      throw new Error('Comando pendente inválido; recuperação bloqueada sem apagar dados.');
    return entry;
  }
  function dispatch(entry) {
    const game = bridge();
    if (!game || typeof game.executePublishedAdventureCommand !== 'function')
      return resultError('BRIDGE_UNAVAILABLE', 'Motor de comandos publicados indisponível.');
    try {
      const result = game.executePublishedAdventureCommand({ ...entry.command, actionId: entry.actionId });
      return result && typeof result === 'object' ? result :
        resultError('INVALID_RESPONSE', 'Resposta inválida do motor publicado.');
    } catch (error) {
      return resultError('ENGINE_EXCEPTION', String(error?.message || error));
    }
  }
  function releaseIfRejected(entry, result) {
    // A deterministic engine rejection with an authoritative persisted state
    // and NO receipt is not a committed action. Ambiguous bridge failures
    // remain in the outbox and must be retried with the SAME ID.
    if (!result || result.ok || ['BRIDGE_UNAVAILABLE', 'ENGINE_EXCEPTION', 'INVALID_RESPONSE'].includes(result.error)) return;
    try {
      const saved = state();
      if (saved.campaignId !== entry.campaignId) return;
      const receipts = saved.world?.publishedAdventureActionReceipts ?? [];
      if (!Array.isArray(receipts) || receipts.some(r => r.id === entry.actionId)) return;
      storage().removeItem(KEY);
    } catch { /* retain the outbox on uncertain storage state */ }
  }
  function replay(entry) {
    if (state().campaignId !== entry.campaignId)
      return resultError('CAMPAIGN_CHANGED', 'Existe ação pendente de outra campanha. Não foi executada.');
    const result = dispatch(entry);
    if (result.ok) {
      try { storage().removeItem(KEY); }
      catch { /* replay remains safe because the ID is durable */ }
    } else releaseIfRejected(entry, result);
    return result;
  }
  function execute(kind, adventureId, args = {}) {
    try {
      const current = state();
      const next = command(kind, adventureId, args);
      const fingerprint = JSON.stringify(next);
      const outstanding = pending();
      if (outstanding) {
        if (outstanding.campaignId !== current.campaignId)
          return resultError('CAMPAIGN_CHANGED', 'Há uma ação pendente de outra campanha. Recupere-a antes de prosseguir.');
        if (outstanding.fingerprint !== fingerprint) {
          const recovered = replay(outstanding);
          return recovered.ok ? resultError('PREVIOUS_ACTION_RECOVERED', 'Ação anterior recuperada. Atualize a cena antes de escolher outra ação.') : recovered;
        }
        return replay(outstanding);
      }
      const entry = { version: 1, campaignId: current.campaignId, actionId: generateId(), command: next, fingerprint };
      // No state-changing command is sent if the outbox cannot be written.
      storage().setItem(KEY, JSON.stringify(entry));
      return replay(entry);
    } catch (error) {
      return resultError('OUTBOX_FAILED', String(error?.message || error));
    }
  }
  function recover() {
    try {
      const entry = pending();
      return entry ? replay(entry) : { ok: true, recovered: false };
    } catch (error) {
      return resultError('RECOVERY_FAILED', String(error?.message || error));
    }
  }
  function finishCombat(options = {}) {
    try {
      // A published combat may already have committed when its ACK was lost.
      // Its combat context is then gone; never fall through to sandbox combat
      // while the durable published outbox still has an unresolved command.
      const outstanding = pending();
      if (outstanding) {
        const recovered = replay(outstanding);
        return recovered.ok ? resultError('PREVIOUS_ACTION_RECOVERED',
          'Ação publicada anterior recuperada. Atualize o combate antes de continuar.') : recovered;
      }
      const saved = state();
      const adventureId = saved.combat?.publishedAdventureContext?.adventureId;
      if (adventureId) return execute('finish-combat', adventureId, { options });
      const game = bridge();
      return game?.finishCombat?.(options) ?? resultError('BRIDGE_UNAVAILABLE', 'Motor de combate indisponível.');
    } catch (error) {
      return resultError('COMBAT_COMMAND_FAILED', String(error?.message || error));
    }
  }
  const client = Object.freeze({ execute, finishCombat, recover, pending });
  root.BraseiroPublishedActionClient = client;
  // Recover after a WebView/process restart, before any new UI interaction.
  const recovery = recover();
  if (!recovery.ok || recovery.recovered !== false)
    root.__BRASEIRO_PUBLISHED_RECOVERY__ = recovery;
})(window);
