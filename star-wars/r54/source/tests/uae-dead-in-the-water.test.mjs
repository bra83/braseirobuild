import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import {
  DEAD_IN_THE_WATER,
  startDeadInTheWater,
  deadInTheWaterStatus,
  advanceDeadInTheWater,
  currentDeadInTheWaterScene,
  currentDeadInTheWaterChecks,
  adjudicateDeadInTheWaterCheck,
  currentDeadInTheWaterEncounter,
  deadInTheWaterEncounterSpec,
  markDeadInTheWaterEncounterActive,
  recordDeadInTheWaterEncounterOutcome,
  markDeadInTheWaterRewardsApplied,
  archiveDeadInTheWater,
  currentDeadInTheWaterMasterRequest
} from '../app/src/main/assets/vtt/js/adventure/deadInTheWater.mjs';
import {
  getPublishedAdventureAdapter,
  certifiedPublishedAdventureCatalog,
  RELEASE_CERTIFIED_ADVENTURES
} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

function step(world, choiceId) {
  return advanceDeadInTheWater(world, choiceId).world;
}

function roundTrip(world) {
  return JSON.parse(JSON.stringify(world));
}

test('Dead in the Water: Catálogo oficial e metadados AoR', () => {
  assert.equal(DEAD_IN_THE_WATER.id, 'dead-in-the-water');
  assert.equal(DEAD_IN_THE_WATER.rulesProfile, 'AOR_CORE');
  assert.deepEqual(DEAD_IN_THE_WATER.campaignModes, ['AOR', 'MIXED']);
  assert.equal(DEAD_IN_THE_WATER.source.official, true);
  assert.equal(DEAD_IN_THE_WATER.source.productCode, 'SWA03');
  assert.match(DEAD_IN_THE_WATER.title, /Morte nas Profundezas/i);

  assert.ok(RELEASE_CERTIFIED_ADVENTURES.includes('dead-in-the-water'));
  const catalog = certifiedPublishedAdventureCatalog();
  const entry = catalog.find(x => x.id === 'dead-in-the-water');
  assert.ok(entry, 'dead-in-the-water deve constar no catálogo de aventuras certificadas');
  assert.deepEqual(entry.campaignModes, ['AOR', 'MIXED']);
});

test('Dead in the Water: Início PT-BR, anti-spoiler e isolamento de nós futuros', () => {
  const initialWorld = createWorld();
  const started = startDeadInTheWater(initialWorld);
  const status = deadInTheWaterStatus(started.world);

  assert.equal(status.active, true);
  assert.equal(status.completed, false);
  assert.equal(status.progress.currentNodeId, 'arrival-surface');
  assert.match(status.node.title, /Chegada a Morjanssik/i);
  assert.match(status.node.narration, /Morjanssik/i);

  // Anti-spoiler: nós futuros não devem ser expostos na lista de escolhas para o jogador
  assert.ok(status.node.choices.length >= 2);
  for (const choice of status.node.choices) {
    assert.equal('to' in choice, false, 'Destino "to" não deve vazar para a visão pública do jogador');
    assert.equal('effects' in choice, false, 'Efeitos mecânicos não devem vazar para a visão pública');
  }

  const masterReq = currentDeadInTheWaterMasterRequest(started.world);
  assert.ok(masterReq);
  assert.equal(masterReq.antiSpoiler, true);
  assert.equal(masterReq.nodeId, 'arrival-surface');
  assert.equal(masterReq.episode, 1);
  assert.ok(masterReq.suggestedActions.length >= 2);
});

test('Dead in the Water: Checks de aventura com pool mecânico sem revelar escolhas', () => {
  const started = startDeadInTheWater(createWorld());
  const checks = currentDeadInTheWaterChecks(started.world);

  assert.ok(Array.isArray(checks));
  assert.equal(checks.length, 2);
  assert.equal(checks[0].skill, 'Frieza');
  assert.equal(checks[0].difficulty, 'Fácil');
  assert.equal(checks[1].skill, 'Percepção');
  assert.equal(checks[1].difficulty, 'Média');
  assert.equal('to' in checks[0], false);
});

test('Dead in the Water: Adjudicação de checks atualiza clocks e histórico de consequências', () => {
  let w = startDeadInTheWater(createWorld()).world;

  // Falha com desespero (triumph=0, despair=1) acelera breach
  let adjResult = adjudicateDeadInTheWaterCheck(w, {
    checkIndex: 1,
    result: { success: false, netSuccess: 0, netAdvantage: -1, triumph: 0, despair: 1 }
  });
  w = adjResult.world;
  assert.equal(adjResult.progress.flags.mineBreachClock, 2);
  assert.equal(adjResult.progress.checkHistory.length, 1);

  // Sucesso com vantagens (netSuccess=2, netAdvantage=2, triumph=1) dá momentum
  adjResult = adjudicateDeadInTheWaterCheck(w, {
    checkIndex: 0,
    result: { success: true, netSuccess: 2, netAdvantage: 2, triumph: 1, despair: 0 }
  });
  w = adjResult.world;
  assert.equal(adjResult.progress.flags.allianceMomentum, 2);
  assert.equal(adjResult.progress.checkHistory.length, 2);
});

test('Dead in the Water: Trilha da Vitória (Vitória épica na plataforma laser, resgate da mina e lealdade Quarren)', () => {
  let w = startDeadInTheWater(createWorld()).world;

  // Episódio 1: Superfície e Reunião
  w = step(w, 'dock-cooperate');
  let s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'management-meeting');
  assert.equal(s.progress.flags.arrivalApproach, 'cooperative');

  w = step(w, 'investigate-underworld');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'rusty-harpoon-tavern');

  // Episódio 2: O Arpão Enferrujado (Negociação) e Perseguição
  w = step(w, 'tavern-negotiate');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'catwalk-confrontation');
  assert.equal(s.progress.flags.mervBribed, true);

  w = step(w, 'catwalk-capture');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'bathysphere-descent');
  assert.equal(s.progress.flags.mervInterrogated, true);
  assert.equal(s.progress.flags.wolgImplicated, true);

  // Episódio 3: Descida submarina Kulv-Bockar
  w = step(w, 'bathysphere-dock-clean');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'staging-trap');
  assert.equal(s.progress.flags.stealthDescent, true);

  // Episódio 4: Emboscada, Fuga e Provas com Wolg e Q-4
  w = step(w, 'staging-breakout');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'staging-investigation');
  assert.equal(s.progress.flags.escapedDetention, true);

  w = step(w, 'rush-level-42');
  s = deadInTheWaterStatus(w);
  assert.equal(s.progress.currentNodeId, 'level-42-showdown');
  assert.equal(s.progress.flags.evidenceSecured, true);
  assert.equal(s.progress.flags.q4Allied, true);

  // Episódio 5: Confronto com Capitão Generet e Plataforma Laser
  const enc = currentDeadInTheWaterEncounter(w);
  assert.ok(enc);
  assert.equal(enc.id, 'mining-laser-climax');
  assert.equal(enc.available, true);

  const spec = deadInTheWaterEncounterSpec(w);
  assert.equal(spec.id, 'mining-laser-climax');
  assert.ok(spec.adversaries.some(a => a.profileKey === 'capitan_generet'));

  w = markDeadInTheWaterEncounterActive(w, 'mining-laser-climax');
  const encResult = recordDeadInTheWaterEncounterOutcome(w, {
    encounterId: 'mining-laser-climax',
    outcome: 'adversaries_defeated',
    adversaries: [
      { profileKey: 'capitan_generet', worldNpcId: 'ditw.npc.capitan_generet', status: 'defeated' },
      { profileKey: 'teniente_huv', worldNpcId: 'ditw.npc.teniente_huv', status: 'defeated' }
    ]
  });
  w = encResult.world;
  assert.equal(w.publishedAdventure.flags.laserPlatformDisabled, true);

  // Agora a escolha de vitória está disponível
  w = step(w, 'resolve-victory');
  s = deadInTheWaterStatus(w);
  assert.equal(s.completed, true);
  assert.equal(s.progress.currentNodeId, 'epilogue-victory');
  assert.equal(s.progress.flags.mineSaved, true);
  assert.equal(s.progress.flags.resolutionOutcome, 'victory');

  // Consequências no Mundo Vivo (Living World)
  assert.ok(w.publishedAdventureConsequences['dead-in-the-water']);
  assert.equal(w.publishedAdventureConsequences['dead-in-the-water'].mineSaved, true);
  assert.equal(w.publishedAdventureConsequences['dead-in-the-water'].q4Joined, true);

  // Verificação de NPCs e Memória Causal sem telepatia
  assert.equal(w.npcs['ditw.npc.kelmut_wolg'].status, 'reabilitado');
  assert.equal(w.npcs['ditw.npc.capitan_generet'].status, 'capturado');
  const kalbracMem = w.livingDepth.npcs['ditw.npc.kalbrac'].memory;
  assert.ok(kalbracMem.some(m => /salvou as minas de Morjanssik/i.test(m.summary)));
  const chonkMem = w.livingDepth.npcs['ditw.npc.moren_chonk'].memory;
  assert.ok(chonkMem.some(m => /protegeram os operários Quarren/i.test(m.summary)));

  // Relações de facções atualizadas
  assert.ok(w.livingDepth.factions['ditw.fac.rebel_alliance'].relations['player-group'] >= 20);
  assert.ok(w.livingDepth.factions['ditw.fac.quarren_miners'].relations['player-group'] >= 25);
  assert.ok(w.livingDepth.factions['ditw.fac.galactic_empire'].relations['player-group'] <= -30);

  // Planos de facção pós-epílogo registrados no Mundo Vivo
  const plans = w.publishedAdventureWorldPlans['dead-in-the-water'];
  assert.ok(Array.isArray(plans));
  assert.ok(plans.some(p => p.id === 'quarren-alliance-fleet-reinforcement'));
});

test('Dead in the Water: Trilha de Evacuação (Briga na taverna, rompimento da mina e retirada heróica com as provas)', () => {
  let w = startDeadInTheWater(createWorld()).world;

  w = step(w, 'dock-observe');
  assert.equal(w.publishedAdventure.flags.cargoNoticed, true);

  w = step(w, 'investigate-underworld');
  w = step(w, 'tavern-fight');
  assert.equal(w.publishedAdventure.flags.barFight, true);

  // Encontro no Arpão Enferrujado
  const brawl = currentDeadInTheWaterEncounter(w);
  assert.equal(brawl.id, 'rusty-harpoon-brawl');
  w = markDeadInTheWaterEncounterActive(w, 'rusty-harpoon-brawl');
  w = recordDeadInTheWaterEncounterOutcome(w, {
    encounterId: 'rusty-harpoon-brawl',
    outcome: 'adversaries_defeated',
    adversaries: [{ profileKey: 'walif_merv', status: 'defeated' }]
  }).world;

  w = step(w, 'catwalk-capture');
  w = step(w, 'bathysphere-dock-emergency');
  assert.equal(w.publishedAdventure.flags.roughDescent, true);

  w = step(w, 'staging-breakout');
  w = step(w, 'rush-level-42');

  // Clímax: A contenção falha / grupo é forçado a recuar
  w = markDeadInTheWaterEncounterActive(w, 'mining-laser-climax');
  w = recordDeadInTheWaterEncounterOutcome(w, {
    encounterId: 'mining-laser-climax',
    outcome: 'evacuated'
  }).world;
  assert.equal(w.publishedAdventure.flags.mineBreached, true);

  // Resolução de evacuação
  w = step(w, 'resolve-evacuation');
  const s = deadInTheWaterStatus(w);
  assert.equal(s.completed, true);
  assert.equal(s.progress.currentNodeId, 'epilogue-evacuation');
  assert.equal(s.progress.flags.mineSaved, false);
  assert.equal(s.progress.flags.resolutionOutcome, 'evacuation');

  assert.equal(w.npcs['ditw.npc.capitan_generet'].status, 'evadido');
  assert.equal(w.npcs['ditw.npc.kelmut_wolg'].status, 'sob_custodia');
  assert.ok(w.livingDepth.npcs['ditw.npc.kalbrac'].memory.some(m => /danos severos/i.test(m.summary)));
});

test('Dead in the Water: Recompensas oficiais e persistência contra 3x recarga de estado', () => {
  let w = startDeadInTheWater(createWorld()).world;
  w = step(w, 'dock-cooperate');
  w = step(w, 'investigate-underworld');
  w = step(w, 'tavern-negotiate');
  w = step(w, 'catwalk-capture');
  w = step(w, 'bathysphere-dock-clean');
  w = step(w, 'staging-breakout');
  w = step(w, 'rush-level-42');

  w = recordDeadInTheWaterEncounterOutcome(w, {
    encounterId: 'mining-laser-climax',
    outcome: 'adversaries_defeated'
  }).world;
  w = step(w, 'resolve-victory');

  // Aplicação de recompensa oficial (Duty + XP)
  w = markDeadInTheWaterRewardsApplied(w, { duty: 10, xp: 25 });
  assert.equal(w.publishedAdventureRewards['dead-in-the-water'].applied, true);
  assert.equal(w.publishedAdventureRewards['dead-in-the-water'].duty, 10);
  assert.equal(w.publishedAdventureRewards['dead-in-the-water'].xp, 25);

  // Arquivamento
  w = archiveDeadInTheWater(w);
  let status = deadInTheWaterStatus(w);
  assert.equal(status.archived, true);

  // 3x Round-trip reload (simulando salvamento e carregamento no banco/disco)
  for (let cycle = 1; cycle <= 3; cycle++) {
    w = roundTrip(w);
    status = deadInTheWaterStatus(w);
    assert.equal(status.archived, true, `Ciclo de recarga ${cycle}: deve manter arquivado`);
    assert.equal(status.progress.currentNodeId, 'epilogue-victory');
    assert.equal(status.progress.flags.mineSaved, true);
    assert.equal(status.progress.flags.resolutionOutcome, 'victory');
    assert.equal(status.progress.sceneJournal.length, 8);
    assert.equal(w.publishedAdventureRewards['dead-in-the-water'].applied, true);
    assert.equal(w.publishedAdventureRewards['dead-in-the-water'].duty, 10);
    assert.equal(w.publishedAdventureRewards['dead-in-the-water'].xp, 25);
  }
});

test('Dead in the Water: Integração através do Adaptador Universal UAE e reconciliação', () => {
  const adapter = getPublishedAdventureAdapter('dead-in-the-water');
  let w = createWorld();

  const started = adapter.start(w);
  w = started.world;
  let status = adapter.status(w);
  assert.equal(status.active, true);
  assert.ok(status.uae, 'UAE snapshot deve existir');
  assert.equal(status.uae.currentNodeId, 'arrival-surface');

  // Avanço via adapter.advance
  const advanced = adapter.advance(w, 'dock-cooperate');
  w = advanced.world;
  status = adapter.status(w);
  assert.equal(status.uae.currentNodeId, 'management-meeting');
  assert.ok(status.uae.objectives.length > 0);
  const uaeState = adapter.uaeSnapshot(w);
  assert.ok(uaeState.visitedNodeIds.includes('arrival-surface'));
  assert.ok(uaeState.visitedNodeIds.includes('management-meeting'));
});
