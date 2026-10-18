import test from 'node:test';
import assert from 'node:assert/strict';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

function createMockRepository() {
  let saved = null;
  return {
    load() { return saved; },
    save(state) { saved = structuredClone(state); return saved; }
  };
}

test('Controller & Bridge: Crafting mechanics integration', () => {
  const repo = createMockRepository();
  const bridge = new BraseiroUiBridge({ repository: repo });
  bridge.newCampaign('Campanha Forja');

  // Verify catalog
  const catalogRes = bridge.craftingCatalog();
  assert.equal(catalogRes.ok, true);
  assert.ok(catalogRes.data.templates.weapons.blaster_pistol);
  assert.ok(catalogRes.data.templates.armor.heavy_armor);
  assert.ok(catalogRes.data.templates.droids.combat_droid);

  // Set character with credits and mechanics
  const charRes = bridge.createFndCharacter({ name: 'Tech Crafter', careerKey: 'sentinel', specializationKey: 'artisan' });
  assert.equal(charRes.ok, true);

  // Craft weapon via bridge
  const craftRes = bridge.craftItem({
    templateType: 'weapon',
    templateId: 'blaster_pistol',
    rollResult: { successes: 2, advantage: 3, triumph: 0, threat: 0, despair: 0 },
    spentAdvantage: ['damage_1']
  });
  assert.equal(craftRes.ok, true);
  assert.equal(craftRes.data.success, true);
  assert.equal(craftRes.data.item.damage, 7); // 6 + 1
});

test('Controller & Bridge: Squads and Starfighter Squadrons integration', () => {
  const repo = createMockRepository();
  const bridge = new BraseiroUiBridge({ repository: repo });
  bridge.newCampaign('Campanha Tática');
  bridge.createFndCharacter({ name: 'General Kenobi', careerKey: 'guardian', specializationKey: 'peacekeeper' });

  // Create squad
  const squadRes = bridge.createSquad({ minionProfileName: 'Clone Trooper', minionCount: 4 });
  assert.equal(squadRes.ok, true);
  assert.equal(squadRes.data.minionCount, 4);

  // Redirect damage
  const redirectRes = bridge.redirectSquadDamage(squadRes.data, 8);
  assert.equal(redirectRes.ok, true);
  assert.equal(redirectRes.data.absorbed, true);
  assert.equal(redirectRes.data.minionsLost, 1);
  assert.equal(redirectRes.data.remainingMinions, 3);

  // Starfighter squadron
  const squadronRes = bridge.createStarfighterSquadron({
    squadronName: 'Esquadrão Vermelho',
    leaderVehicleId: 'veh-01',
    wingmenCount: 3,
    wingmanType: 'X-Wing Minion'
  });
  assert.equal(squadronRes.ok, true);
  assert.equal(squadronRes.data.wingmenCount, 3);

  const squadronDamageRes = bridge.redirectSquadronDamage(squadronRes.data);
  assert.equal(squadronDamageRes.ok, true);
  assert.equal(squadronDamageRes.data.absorbed, true);
  assert.equal(squadronDamageRes.data.wingmenLost, 1);
  assert.equal(squadronDamageRes.data.remainingWingmen, 2);
});

test('Controller & Bridge: Mass Combat round resolution', () => {
  const repo = createMockRepository();
  const bridge = new BraseiroUiBridge({ repository: repo });
  bridge.newCampaign('Campanha Batalha');

  const roundRes = bridge.resolveMassCombatRound({
    friendlyForceTier: 2,
    enemyForceTier: 2,
    commanderLeadershipRank: 2,
    rollResult: { successes: 3, advantage: 2, triumph: 1, threat: 0, despair: 0 }
  });
  assert.equal(roundRes.ok, true);
  assert.equal(roundRes.data.victory, true);
  assert.ok(roundRes.data.enemyCasualtiesPercent > 0);
});

test('Controller & Bridge: Signature Abilities integration', () => {
  const repo = createMockRepository();
  const bridge = new BraseiroUiBridge({ repository: repo });
  bridge.newCampaign('Campanha Assinatura');

  // Verify catalog
  const catRes = bridge.signatureAbilitiesCatalog();
  assert.equal(catRes.ok, true);
  assert.ok(catRes.data.always_get_my_mark);

  // Setup valid character with Bounty Hunter career and row 5 talent
  const charBaseRes = bridge.createFndCharacter({ name: 'Boba Fett', careerKey: 'guardian', specializationKey: 'peacekeeper' });
  assert.equal(charBaseRes.ok, true);
  const char = charBaseRes.data;
  char.creation.careerKey = 'bounty_hunter';
  char.xp.available = 50;
  char.talentPurchases = [{ talentKey: 't1', row: 5 }];

  const setRes = bridge.setCharacter(char);
  assert.equal(setRes.ok, true);

  // Can purchase check
  const checkRes = bridge.canPurchaseSignatureAbility('always_get_my_mark');
  assert.equal(checkRes.ok, true);
  assert.equal(checkRes.data.can, true);

  // Purchase signature ability
  const purchaseRes = bridge.purchaseSignatureAbility('always_get_my_mark');
  assert.equal(purchaseRes.ok, true);
  assert.equal(purchaseRes.data.cost, 30);

  // Set destiny pool
  const destinyRes = bridge.setGroupDestiny({ light: 2, dark: 1 });
  assert.equal(destinyRes.ok, true);

  // Activate signature ability
  const actRes = bridge.activateSignatureAbility('always_get_my_mark');
  assert.equal(actRes.ok, true);
  assert.equal(actRes.data.activated, true);
  assert.equal(actRes.data.character.signatureAbilities[0].usedThisSession, true);
});
