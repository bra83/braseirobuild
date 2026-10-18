import test from 'node:test';
import assert from 'node:assert/strict';
import {
  SQUAD_FORMATIONS,
  SQUADRON_FORMATIONS,
  createSquad,
  setSquadFormation,
  redirectDamageToSquad,
  createStarfighterSquadron,
  setSquadronFormation,
  redirectVehicleDamageToWingman,
  resolveMassCombatRound
} from '../app/src/main/assets/vtt/js/rules/squadsAndMassCombat.mjs';

test('Squads: Formações e inicialização de Esquadrão', () => {
  let squad = createSquad({ leaderName: 'Capitão Rex', minionCount: 4 });
  assert.equal(squad.minionCount, 4);
  assert.equal(squad.formation, 'skirmish');
  assert.equal(squad.active, true);

  squad = setSquadFormation(squad, 'defensive');
  assert.equal(squad.formation, 'defensive');
  assert.equal(SQUAD_FORMATIONS.defensive.bonusDefense, 1);
});

test('Squads: Redirecionamento de dano ao minion do esquadrão', () => {
  let squad = createSquad({ leaderName: 'Sargento Rebelde', minionCount: 2 });
  const hit1 = redirectDamageToSquad(squad);
  assert.equal(hit1.absorbed, true);
  assert.equal(hit1.remainingMinions, 1);
  assert.equal(hit1.squad.active, true);

  const hit2 = redirectDamageToSquad(hit1.squad);
  assert.equal(hit2.absorbed, true);
  assert.equal(hit2.remainingMinions, 0);
  assert.equal(hit2.squad.active, false);

  const hit3 = redirectDamageToSquad(hit2.squad);
  assert.equal(hit3.absorbed, false);
});

test('Squadrons: Formações e sacrifício de caça de escolta (Wingman)', () => {
  let squadron = createStarfighterSquadron({
    leaderPilotName: 'Wedge Antilles',
    leaderVehicleId: 'rogue_leader',
    wingmenCount: 3
  });
  assert.equal(squadron.wingmenCount, 3);
  assert.equal(squadron.formation, 'vic');

  squadron = setSquadronFormation(squadron, 'spread');
  assert.equal(squadron.formation, 'spread');

  const hit = redirectVehicleDamageToWingman(squadron);
  assert.equal(hit.absorbed, true);
  assert.equal(hit.remainingMinions, undefined);
  assert.equal(hit.remainingWingmen, 2);
});

test('Mass Combat: Resolução de rodada com vitória e triunfo', () => {
  const round = resolveMassCombatRound({
    friendlyForceTier: 3,
    enemyForceTier: 3,
    rollResult: { successes: 2, advantage: 2, triumph: 1, threat: 0, despair: 0 }
  });

  assert.equal(round.baseDifficulty, 2);
  assert.equal(round.victory, true);
  assert.ok(round.enemyCasualtiesPercent >= 50);
  assert.equal(round.commanderRisk, false);
  assert.ok(round.tacticalOutcome.includes('Vitória esmagadora'));
});

test('Mass Combat: Derrota com desespero impõe risco crítico ao comandante', () => {
  const round = resolveMassCombatRound({
    friendlyForceTier: 2,
    enemyForceTier: 3, // Inimigo maior -> Dificuldade 3
    rollResult: { successes: 0, advantage: 0, triumph: 0, threat: 2, despair: 1 }
  });

  assert.equal(round.baseDifficulty, 3);
  assert.equal(round.victory, false);
  assert.equal(round.commanderRisk, true);
  assert.ok(round.tacticalOutcome.includes('Derrota crítica'));
});
