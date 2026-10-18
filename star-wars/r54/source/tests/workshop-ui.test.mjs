import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { initWorkshopUi } from '../app/src/main/assets/vtt/js/ui/workshopUi.mjs';

function createMockRepository() {
  let saved = null;
  return {
    load() { return saved; },
    save(state) { saved = structuredClone(state); return saved; }
  };
}

class MockElement {
  constructor(id, tag = 'div') {
    this.id = id;
    this.tagName = tag.toUpperCase();
    this.children = [];
    this.value = '';
    this.textContent = '';
    this.className = '';
    this.checked = false;
    this.listeners = {};
  }
  append(child) {
    this.children.push(child);
  }
  replaceChildren(...newChildren) {
    this.children = [...newChildren];
  }
  addEventListener(event, fn) {
    this.listeners[event] = this.listeners[event] || [];
    this.listeners[event].push(fn);
  }
  click() {
    for (const fn of this.listeners['click'] || []) fn();
  }
  trigger(event) {
    for (const fn of this.listeners[event] || []) fn();
  }
}

test('Workshop UI: Initializes templates, lightsabers, beasts and signature abilities', () => {
  const elements = new Map();
  const getOrCreate = (id, tag = 'div') => {
    if (!elements.has(id)) elements.set(id, new MockElement(id, tag));
    return elements.get(id);
  };

  const ids = [
    ['craftCategory', 'select'],
    ['craftTemplate', 'select'],
    ['craftTemplateDetails', 'div'],
    ['craftAdvantageOption', 'input'],
    ['craftTriumphOption', 'input'],
    ['craftExecuteBtn', 'button'],
    ['craftFeedback', 'div'],
    ['saberChassis', 'select'],
    ['saberCrystal', 'select'],
    ['saberAssembleBtn', 'button'],
    ['saberFeedback', 'div'],
    ['beastSelect', 'select'],
    ['beastTameBtn', 'button'],
    ['beastMountBtn', 'button'],
    ['beastChargeBtn', 'button'],
    ['beastFeedback', 'div'],
    ['squadCreateBtn', 'button'],
    ['squadAbsorbBtn', 'button'],
    ['squadFeedback', 'div'],
    ['massFriendlyTier', 'input'],
    ['massEnemyTier', 'input'],
    ['massCombatBtn', 'button'],
    ['massCombatFeedback', 'div'],
    ['sigSelect', 'select'],
    ['sigBuyBtn', 'button'],
    ['sigActivateBtn', 'button'],
    ['sigFeedback', 'div']
  ];

  for (const [id, tag] of ids) getOrCreate(id, tag);

  getOrCreate('craftCategory').value = 'weapon';
  getOrCreate('craftAdvantageOption').checked = true;
  getOrCreate('craftTriumphOption').checked = true;
  getOrCreate('massFriendlyTier').value = '2';
  getOrCreate('massEnemyTier').value = '2';

  // Setup DOM mock document.createElement
  globalThis.document = {
    createElement(tag) {
      return new MockElement(null, tag);
    }
  };

  const repo = createMockRepository();
  const game = new GameController(repo);
  game.newCampaign('Campanha Workshop');
  game.createFndCharacter({ name: 'Crafter Luke', careerKey: 'sentinel', specializationKey: 'artisan' });

  const $ = id => elements.get(id) || null;
  initWorkshopUi(game, $, () => {});

  // 1. Verify craft template options were populated
  const craftTpl = $('craftTemplate');
  assert.ok(craftTpl.children.length > 0);
  craftTpl.value = craftTpl.children[0].value;

  // 2. Click Craft
  $('craftExecuteBtn').click();
  const craftFb = $('craftFeedback');
  assert.match(craftFb.textContent, /sucesso/i);

  // 3. Verify lightsaber options
  const saberChassis = $('saberChassis');
  const saberCrystal = $('saberCrystal');
  assert.ok(saberChassis.children.length > 0);
  assert.ok(saberCrystal.children.length > 0);
  saberChassis.value = saberChassis.children[0].value;
  saberCrystal.value = saberCrystal.children[0].value;

  // 4. Click Assemble Lightsaber
  $('saberAssembleBtn').click();
  const saberFb = $('saberFeedback');
  assert.match(saberFb.textContent, /forjado com sucesso/i);

  // 5. Verify beast options
  const beastSel = $('beastSelect');
  assert.ok(beastSel.children.length > 0);
  beastSel.value = beastSel.children[0].value;

  // 6. Tame beast
  $('beastTameBtn').click();
  const beastFb = $('beastFeedback');
  assert.match(beastFb.textContent, /domado/i);

  // 7. Mount beast
  $('beastMountBtn').click();
  assert.match($('beastFeedback').textContent, /Montado em/i);

  // 8. Mounted charge
  $('beastChargeBtn').click();
  assert.match($('beastFeedback').textContent, /Investida montada/i);

  // 9. Squads & Mass Combat
  $('squadCreateBtn').click();
  assert.match($('squadFeedback').textContent, /Esquadrão ativado/i);

  $('squadAbsorbBtn').click();
  assert.match($('squadFeedback').textContent, /absorveu o impacto/i);

  $('massCombatBtn').click();
  assert.match($('massCombatFeedback').textContent, /Baixas/i);

  // 10. Signature Abilities
  const sigSel = $('sigSelect');
  assert.ok(sigSel.children.length >= 12);
  sigSel.value = sigSel.children[0].value;
});
