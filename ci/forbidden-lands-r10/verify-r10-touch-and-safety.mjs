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
Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true });

if (!globalThis.localStorage) globalThis.localStorage = dom.window.localStorage;
if (!globalThis.crypto?.randomUUID) {
  globalThis.crypto = { ...(globalThis.crypto || {}), randomUUID: () => '00000000-0000-4000-8000-000000000001' };
}

const React = await import('react');
const { createRoot } = await import('react-dom/client');
const { SuggestedChoiceButton } = await import('../../src/components/SuggestedChoiceButton.tsx');

const host = document.getElementById('root');
const root = createRoot(host);
let calls = 0;
await React.act(async () => {
  root.render(React.createElement(SuggestedChoiceButton, {
    action: 'Seguir a trilha',
    index: 0,
    busy: false,
    active: false,
    onChoose: () => { calls += 1; },
  }));
});
const button = document.querySelector('[data-suggested-choice="true"]');
assert.ok(button, 'botão sugerido deve existir');

function pointer(type, x, y) {
  const event = new dom.window.Event(type, { bubbles: true, cancelable: true });
  Object.defineProperties(event, {
    pointerId: { value: 7 },
    clientX: { value: x },
    clientY: { value: y },
    pointerType: { value: 'touch' },
  });
  button.dispatchEvent(event);
}

await React.act(async () => {
  pointer('pointerdown', 30, 30);
  pointer('pointerup', 35, 34);
  button.dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }));
});
assert.equal(calls, 1, 'tap touch + click sintetizado deve executar exatamente uma vez');

await React.act(async () => {
  pointer('pointerdown', 30, 30);
  pointer('pointercancel', 30, 70);
});
assert.equal(calls, 1, 'gesto cancelado por rolagem não deve executar a escolha');

await React.act(async () => {
  button.dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }));
});
assert.equal(calls, 2, 'click acessível/programático deve continuar funcionando');

const { DataSafetyService } = await import('../../src/services/DataSafetyService.ts');
let checkpoints = 0;
const originalCheckpoint = DataSafetyService.checkpoint;
DataSafetyService.checkpoint = async () => { checkpoints += 1; return true; };

DataSafetyService.scheduleCheckpoint('foreground-test', 1200);
await new Promise(resolve => setTimeout(resolve, 1300));
assert.equal(checkpoints, 0, 'snapshot automático pesado não pode iniciar com o app visível');
DataSafetyService.cancelScheduledCheckpoint();

Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true });
DataSafetyService.scheduleCheckpoint('background-test', 1200);
await new Promise(resolve => setTimeout(resolve, 1300));
assert.equal(checkpoints, 1, 'snapshot deve continuar funcionando fora do gameplay visível');
DataSafetyService.cancelScheduledCheckpoint();
DataSafetyService.checkpoint = originalCheckpoint;

console.log('PASS R10 touch/safety: toque Samsung confiável e snapshot automático fora do gameplay visível.');
