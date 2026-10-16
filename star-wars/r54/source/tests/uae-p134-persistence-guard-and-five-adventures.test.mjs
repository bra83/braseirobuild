import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot,
  validatePublishedAdventureRuntimeState, publishedAdventureUaePublicStatus } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { markBeyondTheRimEncounterActive, recordBeyondTheRimEncounterOutcome } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

const clone = structuredClone;
const adapter = getPublishedAdventureAdapter('under-black-sun');
const make = () => new AdventureRuntime(publishedAdventureRuntimeModule(adapter.meta));

for (const [name, mutation] of [
  ['NaN em evento', r => r.dispatchEvent('TEST', { damage: NaN })],
  ['Infinity em evento', r => r.dispatchEvent('TEST', { damage: Infinity })],
  ['BigInt em evento', r => r.dispatchEvent('TEST', { damage: 1n })],
  ['Map em evento', r => r.dispatchEvent('TEST', { damage: new Map() })],
  ['undefined em evento', r => r.dispatchEvent('TEST', { damage: undefined })],
  ['chave perigosa em evento', r => r.dispatchEvent('TEST', JSON.parse('{"__proto__":{ "isAdmin":true }}'))],
  ['ciclo em evento', r => { const x = {}; x.self = x; r.dispatchEvent('TEST', x); }],
  ['flag não persistível', r => r.applyEffects([{ type: 'set-flag', flag: 'state', value: new Set([1]) }])],
  ['efeito desconhecido', r => r.applyEffects([{ type: 'magic-reward', flag: 'freeCredits', value: 999 }])],
  ['segredo sem ID', r => r.revealSecret('', 'Segredo')],
  ['pista sem ID', r => r.addClue('__proto__', 'Pista')]
]) {
  test(`P134 rollback de ${name}`, () => {
    const runtime = make(), before = runtime.getState();
    assert.throws(() => mutation(runtime));
    assert.deepEqual(runtime.getState(), before);
    assert.equal({}.isAdmin, undefined);
  });
}

test('P134 condição nunca lê propriedades herdadas de flags, contadores, objetivos ou payload', () => {
  const runtime = make();
  assert.equal(runtime.isConditionMet('flag:toString'), false);
  assert.equal(runtime.isConditionMet('flag:constructor'), false);
  assert.equal(runtime.isConditionMet('!flag:constructor'), true);
  assert.equal(runtime.isConditionMet('objective:constructor:ACTIVE'), false);
  assert.equal(runtime.isConditionMet('payload:constructor:function'), false);
  runtime.applyEffects({ isReady: true });
  assert.equal(runtime.isConditionMet('flag:isReady'), true);
});

test('P134 pistas e segredos opcionais geram snapshots JSON recuperáveis', () => {
  const runtime = make();
  runtime.addClue('clue-no-title');
  runtime.revealSecret('secret-no-description');
  const restored = AdventureRuntime.restore(runtime.module, runtime.serialize());
  assert.deepEqual(restored.getState(), runtime.getState());
});

for (const {id} of publishedAdventureCatalog()) {
  test(`P134 ${id}: save, projeção e restauração sem vazamento`, () => {
    const a = getPublishedAdventureAdapter(id);
    const world = a.start({}).world;
    const module = publishedAdventureRuntimeModule(a.meta);
    const runtime = AdventureRuntime.restore(module, world.publishedAdventureRuntime);
    runtime.transaction(r => {
      r.dispatchEvent('TURN_VALIDATED', { episode: 1, choices: [] });
      r.recordConsequence('p134-durable-consequence');
      r.updateObjectiveProgress('episode-1', 0.25);
    });
    const updated = clone(world);
    updated.publishedAdventureRuntime = JSON.parse(runtime.serialize());
    assert.equal(validatePublishedAdventureRuntimeState(module, updated.publishedAdventureRuntime, updated.publishedAdventure), true);
    assert.deepEqual(publishedAdventureUaeSnapshot(JSON.parse(JSON.stringify(updated)), a), updated.publishedAdventureRuntime);
    const player = publishedAdventureUaePublicStatus(updated, a);
    assert.ok(!('canonicalEvents' in player));
    assert.ok(!('flags' in player));
    assert.ok(!('revealedSecrets' in player));
    assert.equal(player.objectives.find(x => x.id === 'episode-1')?.progress, 0.25);
  });
  test(`P134 ${id}: recusa snapshot corrompido antes da migração`, () => {
    const a = getPublishedAdventureAdapter(id);
    const world = a.start({}).world;
    const invalid = [
      snapshot => { snapshot.flags = JSON.parse('{"__proto__":{"elevated":true}}'); },
      snapshot => { snapshot.discoveredClues = [{id:'same'}, {id:'same'}]; },
      snapshot => { snapshot.revealedSecrets = [{id:''}]; },
      snapshot => { snapshot.checkHistory = {}; },
      snapshot => { snapshot.systems = new Map(); },
      snapshot => { snapshot.flags.bad = Infinity; }
    ];
    for (const corrupt of invalid) {
      const damaged = clone(world);
      corrupt(damaged.publishedAdventureRuntime);
      assert.throws(() => publishedAdventureUaeSnapshot(damaged, a));
      assert.equal({}.elevated, undefined);
    }
  });
}

test('P134 Além da Orla não confirma combate que não existe', () => {
  const world = getPublishedAdventureAdapter('beyond-the-rim').start({}).world;
  assert.throws(() => markBeyondTheRimEncounterActive(world, 'invented'), /não disponível/);
  assert.throws(() => recordBeyondTheRimEncounterOutcome(world, { encounterId: 'invented', outcome: 'won' }), /não disponível/);
  assert.equal(getPublishedAdventureAdapter('beyond-the-rim').capabilities.encounters, true);
});
