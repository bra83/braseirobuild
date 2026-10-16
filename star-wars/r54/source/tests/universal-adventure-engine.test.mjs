import test from 'node:test';
import assert from 'node:assert/strict';
import { validateAdventureModule, createInitialAdventureState, createCanonicalEvent } from '../app/src/main/assets/vtt/js/adventure/adventureSchema.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';

const SAMPLE_MODULE = Object.freeze({
  id: 'test-mission',
  title: 'Missão de Teste',
  campaignModes: ['EOTE', 'MIXED'],
  startNodeId: 'start',
  initialClocks: {
    patrol_alert: { value: 0, max: 4, status: 'ACTIVE' }
  },
  nodes: {
    'start': {
      episode: 1,
      title: 'Início',
      locationId: 'loc_hangar',
      summary: 'Chegada ao local.',
      narration: 'A porta se abre.',
      checks: [{ skill: 'Percepção', difficulty: 'Média' }],
      choices: [
        { id: 'go_stealth', label: 'Seguir furtivamente', to: 'shadows', effects: { stealthy: true } },
        { id: 'go_guns', label: 'Entrar atirando', to: 'firefight', condition: 'flag:armed' }
      ]
    },
    'shadows': {
      episode: 1,
      title: 'Sombras',
      locationId: 'loc_corridor',
      summary: 'Avanço discreto.',
      narration: 'Nenhum alarme soa.',
      choices: [
        { id: 'reach_exit', label: 'Alcançar a saída', to: 'exit' }
      ]
    },
    'firefight': {
      episode: 1,
      title: 'Tiroteio',
      locationId: 'loc_hangar',
      summary: 'Combate.',
      choices: []
    },
    'exit': {
      episode: 1,
      title: 'Fuga',
      locationId: 'loc_surface',
      summary: 'Missão concluída.',
      choices: []
    }
  }
});

test('Universal Adventure Engine: validação de módulo e estado inicial', () => {
  assert.equal(validateAdventureModule(SAMPLE_MODULE), true);
  const state = createInitialAdventureState(SAMPLE_MODULE);
  assert.equal(state.adventureId, 'test-mission');
  assert.equal(state.status, 'ACTIVE');
  assert.equal(state.currentNodeId, 'start');
  assert.equal(state.visitedNodeIds.length, 1);
  assert.equal(state.canonicalEvents.length, 1);
});

test('Universal Adventure Engine: transição de nós e avaliação de condições', () => {
  const runtime = new AdventureRuntime(SAMPLE_MODULE);
  const choices = runtime.getAvailableChoices();
  assert.equal(choices.length, 1, 'Apenas go_stealth deve estar disponível inicialmente (go_guns exige flag:armed)');
  assert.equal(choices[0].id, 'go_stealth');

  runtime.transition('go_stealth');
  const s1 = runtime.getState();
  assert.equal(s1.currentNodeId, 'shadows');
  assert.equal(s1.flags.stealthy, true);
  assert.equal(s1.visitedNodeIds.includes('shadows'), true);

  runtime.transition('reach_exit');
  const s2 = runtime.getState();
  assert.equal(s2.currentNodeId, 'exit');
  assert.equal(s2.status, 'COMPLETED');
});

test('Universal Adventure Engine: gestão de clocks persistentes e pistas', () => {
  const runtime = new AdventureRuntime(SAMPLE_MODULE);
  const clock = runtime.advanceClock('patrol_alert', 2);
  assert.equal(clock.value, 2);
  assert.equal(clock.status, 'ACTIVE');

  runtime.advanceClock('patrol_alert', 2);
  const c2 = runtime.getState().clocks.patrol_alert;
  assert.equal(c2.value, 4);
  assert.equal(c2.status, 'COMPLETED');

  runtime.addClue('clue_plan', 'Plano da Base', 'Esquema de patrulha recuperado.');
  assert.equal(runtime.getState().discoveredClues.length, 1);
  assert.equal(runtime.getState().discoveredClues[0].id, 'clue_plan');
});

test('Universal Adventure Engine: integridade de contexto para AI GM', () => {
  const runtime = new AdventureRuntime(SAMPLE_MODULE);
  runtime.addClue('c1', 'Pista 1', 'Resumo 1');
  runtime.revealSecret('s1', 'Segredo de teste');
  const ctx = runtime.buildAiContext();

  assert.equal(ctx.adventure.title, 'Missão de Teste');
  assert.equal(ctx.playerKnowledge.discoveredClues.length, 1);
  assert.equal(ctx.gmKnowledge.revealedSecrets.length, 1);
  assert.equal(ctx.scene.title, 'Início');
});
