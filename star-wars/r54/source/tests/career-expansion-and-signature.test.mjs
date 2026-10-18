import test from 'node:test';
import assert from 'node:assert/strict';
import { EXPANDED_CAREER_SPECIALIZATIONS } from '../app/src/main/assets/vtt/js/rules/careerExpansionData.mjs';
import {
  SIGNATURE_ABILITIES_CATALOG,
  canPurchaseSignatureAbility,
  purchaseSignatureAbility,
  activateSignatureAbility
} from '../app/src/main/assets/vtt/js/rules/signatureAbilities.mjs';

test('Expanded Career Specializations: Catalog integrity across 12 careers (24 specs)', () => {
  const careers = Object.keys(EXPANDED_CAREER_SPECIALIZATIONS);
  assert.equal(careers.length, 12);

  let totalSpecs = 0;
  for (const [careerKey, specs] of Object.entries(EXPANDED_CAREER_SPECIALIZATIONS)) {
    const specKeys = Object.keys(specs);
    assert.equal(specKeys.length, 2, `Carreira ${careerKey} deve ter exatamente 2 especializações adicionais`);
    for (const [key, spec] of Object.entries(specs)) {
      totalSpecs++;
      assert.equal(spec.key, key);
      assert.equal(spec.careerKey, careerKey);
      assert.ok(spec.source.book, `Fonte de ${key} deve ser documentada`);
      assert.equal(spec.careerSkills.length, 4, `Especialização ${key} deve ter 4 perícias de carreira`);
    }
  }
  assert.equal(totalSpecs, 24);
});

test('Expanded Signature Abilities: Full catalog with 18 canonical signature abilities', () => {
  const abilities = Object.keys(SIGNATURE_ABILITIES_CATALOG);
  // Original 6 + 6 second abilities + 6 F&D careers = 18 signature abilities
  assert.equal(abilities.length, 18);

  // Second abilities for core careers
  assert.ok(SIGNATURE_ABILITIES_CATALOG.hard_boiled);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.unmatched_fortune);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.inventive_creation);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.last_one_standing);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.master_commander);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.overwhelming_firepower);

  // F&D career abilities
  assert.ok(SIGNATURE_ABILITIES_CATALOG.prophecy);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.unstoppable_force);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.peacekeeper_mandate);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.my_target_awaits);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.unseen_infiltrator);
  assert.ok(SIGNATURE_ABILITIES_CATALOG.deadly_reputation);
});

test('Purchase and activation of second signature ability', () => {
  const char = {
    id: 'char-smuggler-01',
    name: 'Han Lucky',
    creation: { careerKey: 'smuggler' },
    xp: { available: 50, total: 100 },
    talentPurchases: [{ talentKey: 't1', row: 5 }],
    signatureAbilities: []
  };

  assert.equal(canPurchaseSignatureAbility(char, 'unmatched_fortune').can, true);
  const purchase = purchaseSignatureAbility(char, 'unmatched_fortune');
  assert.equal(purchase.character.xp.available, 20);
  assert.equal(purchase.character.signatureAbilities[0].id, 'unmatched_fortune');

  const group = { destiny: { light: 1, dark: 1 } };
  const activation = activateSignatureAbility(purchase.character, 'unmatched_fortune', group);
  assert.equal(activation.activated, true);
  assert.equal(activation.group.destiny.light, 0);
  assert.equal(activation.group.destiny.dark, 2);
});
