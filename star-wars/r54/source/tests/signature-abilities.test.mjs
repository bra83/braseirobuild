import test from 'node:test';
import assert from 'node:assert/strict';
import {
  SIGNATURE_ABILITIES_CATALOG,
  canPurchaseSignatureAbility,
  purchaseSignatureAbility,
  activateSignatureAbility
} from '../app/src/main/assets/vtt/js/rules/signatureAbilities.mjs';

function makeTestChar(careerKey = 'bounty_hunter', xpAvailable = 50, hasRow5 = true) {
  return {
    id: 'char-test-01',
    name: 'Boba Test',
    creation: { careerKey },
    xp: { available: xpAvailable, total: 100 },
    talentPurchases: hasRow5 ? [{ talentKey: 't1', row: 5 }] : [{ talentKey: 't1', row: 1 }],
    signatureAbilities: []
  };
}

test('SIGNATURE_ABILITIES_CATALOG contains canonical abilities for core careers', () => {
  const keys = Object.keys(SIGNATURE_ABILITIES_CATALOG);
  assert.ok(keys.includes('always_get_my_mark'));
  assert.ok(keys.includes('narrow_escape'));
  assert.ok(keys.includes('the_miracle_worker'));
  assert.ok(keys.includes('unmatched_carnage'));
  assert.ok(keys.includes('roused_to_action'));
  assert.ok(keys.includes('the_bigger_they_are'));
});

test('canPurchaseSignatureAbility requires matching career, row 5 talent, and sufficient XP', () => {
  const char = makeTestChar('bounty_hunter', 50, true);
  const eligible = canPurchaseSignatureAbility(char, 'always_get_my_mark');
  assert.equal(eligible.can, true);

  // Mismatched career
  const wrongCareer = canPurchaseSignatureAbility(char, 'narrow_escape');
  assert.equal(wrongCareer.can, false);
  assert.match(wrongCareer.reason, /carreira/);

  // Insufficient XP
  const brokeChar = makeTestChar('bounty_hunter', 20, true);
  const broke = canPurchaseSignatureAbility(brokeChar, 'always_get_my_mark');
  assert.equal(broke.can, false);
  assert.match(broke.reason, /PE insuficientes/);

  // Missing row 5 talent
  const noviceChar = makeTestChar('bounty_hunter', 50, false);
  const novice = canPurchaseSignatureAbility(noviceChar, 'always_get_my_mark');
  assert.equal(novice.can, false);
  assert.match(novice.reason, /linha 5/);
});

test('purchaseSignatureAbility deducts 30 XP and adds signature ability to character', () => {
  const char = makeTestChar('bounty_hunter', 40, true);
  const result = purchaseSignatureAbility(char, 'always_get_my_mark');

  assert.equal(result.cost, 30);
  assert.equal(result.character.xp.available, 10);
  assert.equal(result.character.signatureAbilities.length, 1);
  assert.equal(result.character.signatureAbilities[0].id, 'always_get_my_mark');
  assert.equal(result.character.signatureAbilities[0].usedThisSession, false);
});

test('activateSignatureAbility flips Light Destiny point to Dark and sets usedThisSession flag', () => {
  const char = makeTestChar('bounty_hunter', 40, true);
  const purchased = purchaseSignatureAbility(char, 'always_get_my_mark').character;

  const group = {
    destiny: { light: 2, dark: 1 }
  };

  const activation = activateSignatureAbility(purchased, 'always_get_my_mark', group);
  assert.equal(activation.activated, true);
  assert.equal(activation.character.signatureAbilities[0].usedThisSession, true);
  assert.equal(activation.group.destiny.light, 1);
  assert.equal(activation.group.destiny.dark, 2);

  // Second activation in same session without upgrade fails
  assert.throws(() => {
    activateSignatureAbility(activation.character, 'always_get_my_mark', activation.group);
  }, /já foi utilizada nesta sessão/);
});

test('activateSignatureAbility fails if no light Destiny points available', () => {
  const char = makeTestChar('bounty_hunter', 40, true);
  const purchased = purchaseSignatureAbility(char, 'always_get_my_mark').character;

  const group = {
    destiny: { light: 0, dark: 3 }
  };

  assert.throws(() => {
    activateSignatureAbility(purchased, 'always_get_my_mark', group);
  }, /Pontos de Destino luminosos insuficientes/);
});
