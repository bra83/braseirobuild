import 'fake-indexeddb/auto';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body><div id="root"></div></body></html>', { url: 'https://braseiro.local' });
globalThis.window = dom.window;
globalThis.document = dom.window.document;
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true });
globalThis.HTMLElement = dom.window.HTMLElement;
globalThis.Event = dom.window.Event;
globalThis.MouseEvent = dom.window.MouseEvent;
globalThis.CustomEvent = dom.window.CustomEvent;
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
globalThis.localStorage = dom.window.localStorage;
Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true });
if (!globalThis.crypto?.randomUUID) {
  globalThis.crypto = { ...(globalThis.crypto || {}), randomUUID: () => Math.random().toString(36).slice(2).padEnd(36,'0').slice(0,36) };
}

const React = await import('react');
const { createRoot } = await import('react-dom/client');
const { SuggestedChoiceButton } = await import('../../src/components/SuggestedChoiceButton.tsx');
const { GeminiClientService } = await import('../../src/services/GeminiClientService.ts');
const { MasterOrchestratorService } = await import('../../src/services/MasterOrchestratorService.ts');
const { NarrativeIntentService } = await import('../../src/services/NarrativeIntentService.ts');

const settings = {
  narrativeProfile: 'Expert',
  narrativeStyle: 'Dinâmico',
  generateImages: false,
  ttsAutoplay: false,
};

const remoteScene = action => ({
  sceneTitle: 'Consequência',
  narration: 'A ação "' + action + '" produz uma consequência observável e imediata. O terreno, os sons e as pessoas presentes reagem sem decidir pelo personagem.\n\nA cena avança de modo concreto e deixa outra decisão disponível para o jogador.',
  npcDialogue: [],
  suggestedActions: ['Observar a consequência', 'Interagir com o que mudou', 'Mudar de abordagem'],
  checks: [],
  statePatches: [],
  worldOperations: [],
  audioCues: [],
  imagePrompt: '',
  timelineSummary: 'A ação foi processada.',
  requiresPlayerDecision: true,
  advancesFiction: true,
  presentationKind: 'scene',
});

const originalCall = GeminiClientService.call;
let remoteCalls = 0;
GeminiClientService.call = async (_model, payload) => {
  remoteCalls += 1;
  return remoteScene(String(payload?.userInput || 'ação'));
};

function Harness() {
  const [busy, setBusy] = React.useState(false);
  const [action, setAction] = React.useState('Observar os rastros');
  const [turns, setTurns] = React.useState(0);
  const [source, setSource] = React.useState('');

  const choose = async value => {
    if (busy) return;
    setBusy(true);
    try {
      const intent = NarrativeIntentService.classify(value, { trustedFictionAction: true });
      const result = await MasterOrchestratorService.run({
        payload: {
          userInput: value,
          systemInstruction: 'Teste executável de turno sugerido.',
          history: '',
          rulesPreloaded: true,
          ruleAuthority: { mechanical: false, authorized: true, reason: 'ação ficcional', domain: 'none' },
          ruleCitations: [],
        },
        settings,
        characters: [],
        actingCharacter: null,
        initial: false,
        firstVisit: false,
        rollResolution: false,
        cinematic: false,
        intent,
      });
      setSource(result.masterDecision?.generationSource || '');
      setTurns(v => v + 1);
      setAction(result.suggestedActions?.[0] || 'Continuar');
    } finally {
      setBusy(false);
    }
  };

  return React.createElement('div', null,
    React.createElement('output', { id: 'turns' }, String(turns)),
    React.createElement('output', { id: 'source' }, source),
    React.createElement(SuggestedChoiceButton, {
      action,
      index: 0,
      busy,
      active: busy,
      onChoose: choose,
    }),
  );
}

const root = createRoot(document.getElementById('root'));
await React.act(async () => { root.render(React.createElement(Harness)); });

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function waitFor(predicate, timeout = 2500) {
  const start = Date.now();
  while (!predicate()) {
    if (Date.now() - start > timeout) throw new Error('timeout esperando UI liberar');
    await React.act(async () => { await delay(10); });
  }
}
async function tapButton() {
  const button = document.querySelector('[data-suggested-choice="true"]');
  assert.ok(button, 'botão deve estar renderizado');
  assert.equal(button.disabled, false, 'botão deve estar livre antes do toque');
  const fire = (type, x, y) => {
    const event = new dom.window.Event(type, { bubbles: true, cancelable: true });
    Object.defineProperties(event, {
      pointerId: { value: 5 },
      clientX: { value: x },
      clientY: { value: y },
      pointerType: { value: 'touch' },
    });
    button.dispatchEvent(event);
  };
  await React.act(async () => {
    fire('pointerdown', 24, 24);
    fire('pointerup', 27, 26);
    button.dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }));
  });
}

for (let expected = 1; expected <= 12; expected += 1) {
  await tapButton();
  await waitFor(() => document.getElementById('turns')?.textContent === String(expected));
  await waitFor(() => document.querySelector('[data-suggested-choice="true"]')?.disabled === false);
}
assert.equal(remoteCalls, 12, '12 toques devem gerar 12 turnos, sem clique perdido ou duplicado');
assert.equal(document.getElementById('source')?.textContent, 'gemini');

// 503 then successful second model: UI must recover and become clickable.
let retryCalls = 0;
GeminiClientService.call = async (_model, payload) => {
  retryCalls += 1;
  if (retryCalls === 1) throw new Error('HTTP 503: indisponível');
  return remoteScene(String(payload?.userInput || 'ação'));
};
await tapButton();
await waitFor(() => document.getElementById('turns')?.textContent === '13');
await waitFor(() => document.querySelector('[data-suggested-choice="true"]')?.disabled === false);
assert.equal(retryCalls, 2);

// All remote models fail: local fallback must still release the button.
let failureCalls = 0;
GeminiClientService.call = async () => {
  failureCalls += 1;
  throw new Error('TIMEOUT: simulado');
};
await tapButton();
await waitFor(() => document.getElementById('turns')?.textContent === '14');
await waitFor(() => document.querySelector('[data-suggested-choice="true"]')?.disabled === false);
assert.equal(document.getElementById('source')?.textContent, 'local-fallback');
assert.ok(failureCalls <= 2, 'não pode percorrer mais de dois modelos');

GeminiClientService.call = originalCall;
console.log('PASS R10 end-to-end choice UI: 14 turnos via toque, retry 503, fallback e botão sempre liberado.');
