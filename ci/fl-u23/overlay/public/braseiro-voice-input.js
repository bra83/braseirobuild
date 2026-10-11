(() => {
  if (window.__braseiroVoiceInputInstalled) return;
  window.__braseiroVoiceInputInstalled = true;

  const state = {
    active: null,
    base: '',
    listening: false,
    pluginBound: false,
    webRecognition: null,
    buttons: new Set(),
  };

  const normalize = value => String(value || '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().trim();

  const isActionTarget = el => {
    if (!(el instanceof HTMLTextAreaElement)) return false;
    if (el.dataset.braseiroVoiceAction === 'true') return true;
    const p = normalize(el.getAttribute('placeholder'));
    return (
      p.includes('descreva a acao, pergunta, plano de viagem ou dialogo') ||
      p.includes('descreva detalhadamente o que deseja fazer ou falar') ||
      /^o que .+ faz\??$/.test(p)
    );
  };

  const toast = message => {
    let box = document.getElementById('braseiro-voice-toast');
    if (!box) {
      box = document.createElement('div');
      box.id = 'braseiro-voice-toast';
      Object.assign(box.style, {
        position: 'fixed', left: '50%', bottom: '88px', transform: 'translateX(-50%)',
        zIndex: '2147483647', maxWidth: '88vw', padding: '10px 14px',
        borderRadius: '10px', background: 'rgba(0,0,0,.88)', color: '#fff',
        font: '600 13px system-ui,sans-serif', boxShadow: '0 8px 30px rgba(0,0,0,.35)'
      });
      document.body.appendChild(box);
    }
    box.textContent = String(message || '');
    box.style.display = 'block';
    clearTimeout(box.__hideTimer);
    box.__hideTimer = setTimeout(() => { box.style.display = 'none'; }, 2600);
  };

  // Escrever btn.disabled = true quando já está true ainda chama setAttribute e
  // gera um registro de mutação 'disabled'. Com o observer abaixo isso virava um
  // loop infinito que congelava o renderer sempre que busy desabilitava o textarea.
  const syncDisabled = (btn, value) => {
    if (btn.disabled !== value) btn.disabled = value;
  };

  const setButtonState = mode => {
    state.buttons.forEach(btn => {
      if (!btn.isConnected) { state.buttons.delete(btn); return; }
      const target = btn.__voiceTarget;
      syncDisabled(btn, Boolean(target && target.disabled));
      const label = mode === 'listening' ? '● OUVINDO' : mode === 'processing' ? '… PROCESSANDO' : '🎙 FALAR';
      if (btn.textContent !== label) btn.textContent = label;
      const pressed = mode === 'listening' ? 'true' : 'false';
      if (btn.getAttribute('aria-pressed') !== pressed) btn.setAttribute('aria-pressed', pressed);
    });
  };

  const setReactValue = (el, value) => {
    if (!el) return;
    const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set;
    if (setter) setter.call(el, value);
    else el.value = value;
    el.dispatchEvent(new Event('input', { bubbles: true }));
  };

  const combine = (base, spoken) => {
    const a = String(base || '').trim();
    const b = String(spoken || '').trim();
    if (!a) return b;
    if (!b) return a;
    return /[\s]$/.test(base) || /^[,.;:!?)}\]]/.test(b) ? a + b : a + ' ' + b;
  };

  const applySpeech = text => {
    if (!state.active || !state.active.isConnected) return;
    setReactValue(state.active, combine(state.base, text));
    try { state.active.setSelectionRange(state.active.value.length, state.active.value.length); } catch (_) {}
  };

  const finish = (message) => {
    state.listening = false;
    setButtonState('idle');
    if (state.active?.isConnected) state.active.focus();
    state.active = null;
    state.base = '';
    if (message) toast(message);
  };

  const pauseVoiceOutput = async () => {
    try { window.speechSynthesis?.cancel?.(); } catch (_) {}
    try { await window.Capacitor?.Plugins?.AndroidTts?.stop?.(); } catch (_) {}
    try { window.BraseiroAndroid?.stopSpeech?.(); } catch (_) {}
    try { window.dispatchEvent(new CustomEvent('braseiro-voice-input-start')); } catch (_) {}
  };

  const bindCapacitorPlugin = async plugin => {
    if (state.pluginBound || !plugin?.addListener) return;
    state.pluginBound = true;
    await plugin.addListener('partialResult', event => {
      if (state.listening) applySpeech(event?.text || '');
    });
    await plugin.addListener('finalResult', event => {
      if (state.listening) applySpeech(event?.text || '');
      finish(event?.text ? 'Ditado concluído. Revise e envie quando quiser.' : 'Não consegui entender a fala.');
    });
    await plugin.addListener('voiceState', event => {
      if (!state.listening && event?.state !== 'idle') return;
      if (event?.state === 'processing') setButtonState('processing');
      else if (event?.state === 'listening' || event?.state === 'speech') setButtonState('listening');
    });
    await plugin.addListener('voiceError', event => {
      finish(event?.message || 'O reconhecimento de voz foi interrompido.');
    });
  };

  const bindCustomNativeEvents = () => {
    if (window.__braseiroVoiceCustomEventsBound) return;
    window.__braseiroVoiceCustomEventsBound = true;
    window.addEventListener('braseiro-voice-partial', event => {
      if (state.listening) applySpeech(event.detail?.text || '');
    });
    window.addEventListener('braseiro-voice-final', event => {
      if (state.listening) applySpeech(event.detail?.text || '');
      finish(event.detail?.text ? 'Ditado concluído. Revise e envie quando quiser.' : 'Não consegui entender a fala.');
    });
    window.addEventListener('braseiro-voice-state', event => {
      if (event.detail?.state === 'processing') setButtonState('processing');
      else if (event.detail?.state === 'listening' || event.detail?.state === 'speech') setButtonState('listening');
      else if (event.detail?.state === 'idle' && state.listening) finish();
    });
    window.addEventListener('braseiro-voice-error', event => {
      finish(event.detail?.message || 'O reconhecimento de voz foi interrompido.');
    });
  };

  const startWebFallback = () => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) throw new Error('Reconhecimento de voz não disponível neste aparelho.');
    const rec = new Recognition();
    state.webRecognition = rec;
    rec.lang = 'pt-BR';
    rec.interimResults = true;
    rec.continuous = false;
    rec.onresult = event => {
      let interim = '';
      let finalText = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const value = event.results[i]?.[0]?.transcript || '';
        if (event.results[i].isFinal) finalText += value;
        else interim += value;
      }
      applySpeech(finalText || interim);
      if (finalText) finish('Ditado concluído. Revise e envie quando quiser.');
    };
    rec.onerror = event => finish(event?.error === 'not-allowed' ? 'Permissão de microfone negada.' : 'O reconhecimento de voz foi interrompido.');
    rec.onend = () => { if (state.listening) finish(); };
    rec.start();
  };

  const start = async target => {
    if (!target || target.disabled) return;
    if (state.listening) {
      try {
        if (window.Capacitor?.Plugins?.VoiceInput) await window.Capacitor.Plugins.VoiceInput.stop();
        else if (window.BraseiroAndroid?.stopVoiceInput) window.BraseiroAndroid.stopVoiceInput();
        else state.webRecognition?.stop?.();
      } catch (_) {}
      setButtonState('processing');
      return;
    }

    await pauseVoiceOutput();
    state.active = target;
    state.base = target.value || '';
    state.listening = true;
    setButtonState('listening');

    try {
      const plugin = window.Capacitor?.Plugins?.VoiceInput;
      if (plugin) {
        await bindCapacitorPlugin(plugin);
        const availability = await plugin.isAvailable?.().catch?.(() => null);
        if (availability && availability.available === false) throw new Error('Reconhecimento de voz não disponível neste aparelho.');
        await plugin.start({ language: 'pt-BR', preferOffline: true });
        return;
      }
      bindCustomNativeEvents();
      if (window.BraseiroAndroid?.startVoiceInput) {
        window.BraseiroAndroid.startVoiceInput();
        return;
      }
      startWebFallback();
    } catch (error) {
      finish(error?.message || String(error));
    }
  };

  const ensureButton = target => {
    if (!isActionTarget(target)) return;
    if (target.dataset.braseiroVoiceBound === 'true') return;
    target.dataset.braseiroVoiceBound = 'true';

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'braseiro-voice-input-button';
    btn.textContent = '🎙 FALAR';
    btn.setAttribute('aria-label', 'Ditar ação por voz');
    btn.__voiceTarget = target;
    Object.assign(btn.style, {
      marginTop: '8px', minHeight: '38px', padding: '8px 12px',
      borderRadius: '8px', border: '1px solid currentColor',
      background: 'transparent', color: 'inherit', font: '700 12px system-ui,sans-serif',
      letterSpacing: '.03em', cursor: 'pointer', opacity: '.9'
    });
    btn.addEventListener('click', () => void start(target));
    target.insertAdjacentElement('afterend', btn);
    state.buttons.add(btn);
    syncDisabled(btn, Boolean(target.disabled));
  };

  const scan = () => {
    document.querySelectorAll('textarea').forEach(ensureButton);
    state.buttons.forEach(btn => {
      if (!btn.isConnected) state.buttons.delete(btn);
      else if (btn.__voiceTarget) syncDisabled(btn, Boolean(btn.__voiceTarget.disabled));
    });
  };

  // Debounce: o observer nunca executa scan em cascata síncrona de microtasks.
  let scanTimer = 0;
  const scheduleScan = () => {
    if (scanTimer) return;
    scanTimer = setTimeout(() => { scanTimer = 0; scan(); }, 120);
  };

  bindCustomNativeEvents();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', scan, { once: true });
  else scan();
  new MutationObserver(scheduleScan).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['disabled', 'placeholder', 'data-braseiro-voice-action'] });
  window.addEventListener('pagehide', () => {
    try { window.Capacitor?.Plugins?.VoiceInput?.cancel?.(); } catch (_) {}
    try { window.BraseiroAndroid?.cancelVoiceInput?.(); } catch (_) {}
    try { state.webRecognition?.abort?.(); } catch (_) {}
  });
})();
