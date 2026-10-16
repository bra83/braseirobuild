import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { startLongArmOfTheHutt } from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import { attachmentInstallPayload, effectiveWeaponWithAttachments } from '../app/src/main/assets/vtt/js/rules/attachments.mjs';
import { weaponAttackModifiers } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';

class MemoryRepository {
  constructor(state = createCampaign('P128')) { this.state = structuredClone(state); }
  load() { return structuredClone(this.state); }
  save(state) { this.state = structuredClone(state); }
}
const normalized = status => String(status).toUpperCase();

// These tests exercise the REAL published adapters and controller. They do not
// replace source-specific mechanics with synthetic stub encounters.
test('P128: cinco adaptadores geram UAE persistido, objetivos por episódio e transição real', () => {
  for (const { id } of publishedAdventureCatalog()) {
    const adapter = getPublishedAdventureAdapter(id);
    const started = adapter.start({});
    const initial = publishedAdventureUaeSnapshot(started.world, adapter);
    assert.ok(initial, id);
    assert.equal(initial.adventureId, id);
    assert.equal(initial.currentNodeId, adapter.meta.startNodeId);
    assert.equal(initial.objectives['adventure-finale'].status, 'ACTIVE');
    assert.ok(initial.objectives['episode-1'], `${id}: episódio 1`);
    assert.ok(initial.canonicalEvents.some(event => event.type === 'ADAPTER_STARTED'));
    const choice = started.node.choices[0];
    assert.ok(choice, `${id}: escolha inicial`);
    const advanced = adapter.advance(started.world, choice.id);
    const after = publishedAdventureUaeSnapshot(advanced.world, adapter);
    assert.equal(after.currentNodeId, advanced.world.publishedAdventure.currentNodeId, id);
    assert.ok(after.canonicalEvents.some(event => event.type === 'NODE_TRANSITION'), id);
    assert.deepEqual(AdventureRuntime.restore(publishedAdventureRuntimeModule(adapter.meta), JSON.stringify(after)).getState(), after);
    assert.equal(normalized(after.status), normalized(advanced.world.publishedAdventure.status));
    assert.ok(new Set(after.canonicalEvents.map(event => event.id)).size === after.canonicalEvents.length, id);
  }
});

test('P128: reinício e reabertura no controlador mantêm journal UAE e estado de personagem', () => {
  const repo = new MemoryRepository();
  let game = new GameController(repo);
  game.loadStarterCharacter('cael');
  game.startPublishedAdventureSession('long-arm-of-the-hutt', { rng: () => .5 });
  const first = game.state.world.publishedAdventureRuntime;
  assert.equal(first.currentNodeId, 'rylos-escape-aftermath');
  game.advancePublishedAdventure('dock-ryloth');
  const after = game.state.world.publishedAdventureRuntime;
  assert.equal(after.currentNodeId, 'ryloth-safehouse');
  assert.ok(after.eventSequence > first.eventSequence);
  game = new GameController(repo);
  assert.deepEqual(game.state.world.publishedAdventureRuntime, after);
  game.advancePublishedAdventure('head-to-geonosis');
  assert.equal(game.state.world.publishedAdventureRuntime.currentNodeId, 'geonosis-approach');
  assert.equal(game.state.world.publishedAdventureRuntime.canonicalEvents.filter(event => event.type === 'ADAPTER_STARTED').length, 1);
});

test('P128: migração de save legado sem UAE preserva cena e audita origem', () => {
  const legacy = startLongArmOfTheHutt({}).world;
  const adapter = getPublishedAdventureAdapter('long-arm-of-the-hutt');
  const advanced = adapter.advance(legacy, 'dock-ryloth');
  const state = advanced.world.publishedAdventureRuntime;
  assert.equal(state.currentNodeId, 'ryloth-safehouse');
  assert.ok(state.canonicalEvents.some(event => event.type === 'LEGACY_SNAPSHOT_MIGRATED'));
  assert.ok(state.canonicalEvents.some(event => event.type === 'NODE_TRANSITION'));
});

test('P128: três aventuras por episódios concluem objetivos e arquivam sem duplicar eventos', () => {
  for (const id of ['long-arm-of-the-hutt', 'jewel-of-yavin', 'mask-of-the-pirate-queen']) {
    const adapter = getPublishedAdventureAdapter(id);
    let current = adapter.start({});
    for (let n = 0; n < 25 && !current.completed; n++) {
      if(id==='mask-of-the-pirate-queen' && current.node.id==='flagship-showdown' && !current.node.choices?.length) {
        const resolved=adapter.recordEncounterOutcome(current.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'});
        current={...current,world:resolved.world,node:adapter.status(resolved.world).node};
      }
      assert.ok(current.node.choices?.length, `${id}: nó sem saída`);
      current = adapter.advance(current.world, current.node.choices[0].id);
    }
    assert.equal(current.completed, true, id);
    const completed = publishedAdventureUaeSnapshot(current.world, adapter);
    for (const objective of Object.values(completed.objectives)) assert.equal(objective.status, 'COMPLETED', `${id}: ${objective.id}`);
    assert.equal(completed.canonicalEvents.filter(event => event.type === 'ADVENTURE_COMPLETED').length, 1, id);
    const archived = adapter.archive(current.world);
    const state = publishedAdventureUaeSnapshot(archived, adapter);
    assert.equal(state.status, 'ARCHIVED');
    assert.equal(state.canonicalEvents.filter(event => event.type === 'ADVENTURE_ARCHIVED').length, 1);
    assert.equal(new Set(state.canonicalEvents.map(event => event.id)).size, state.canonicalEvents.length);
  }
});

test('P128: teste da aventura gera evento canônico e preserva progressão de check', () => {
  const adapter = getPublishedAdventureAdapter('long-arm-of-the-hutt');
  const started = adapter.start({});
  const adjudicated = adapter.adjudicateCheck(started.world, { checkIndex: 0, result: { success: true, netSuccess: 1, netAdvantage: 1 } });
  const state = publishedAdventureUaeSnapshot(adjudicated.world, adapter);
  assert.ok(state.canonicalEvents.some(event => event.type === 'ADAPTER_CHECK_RESOLVED'));
  assert.deepEqual(state.checkHistory, adjudicated.world.publishedAdventure.checkHistory);
});

test('P128: reimportação de snapshot com sequência lacunar não duplica IDs de eventos', () => {
  const adapter = getPublishedAdventureAdapter('jewel-of-yavin');
  const module = publishedAdventureRuntimeModule(adapter.meta);
  const runtime = new AdventureRuntime(module);
  const state = runtime.getState();
  state.canonicalEvents.push({ ...state.canonicalEvents[0], id: `evt_${module.id}_812` });
  state.eventSequence = 2;
  const recovered = AdventureRuntime.restore(module, state);
  recovered.dispatchEvent('RECOVERY_TEST');
  assert.equal(recovered.getState().canonicalEvents.at(-1).id, `evt_${module.id}_813`);
});

test('P128: falha ao resolver consequência não deixa marca de resolução nem evento órfão', () => {
  const adapter = getPublishedAdventureAdapter('mask-of-the-pirate-queen');
  const runtime = new AdventureRuntime(publishedAdventureRuntimeModule(adapter.meta));
  runtime.recordConsequence('evidence');
  const before = runtime.getState();
  assert.throws(() => runtime.resolveConsequence('evidence', [{ type: 'advance-clock', clockId: 'pirateAlert', delta: 'NaN' }]), /Variação de relógio inválida/);
  assert.deepEqual(runtime.getState(), before);
  assert.equal(runtime.resolveConsequence('evidence', { evidenceSecured: true }).resolved, true);
  assert.equal(runtime.getState().flags.evidenceSecured, true);
});

test('P128: attachment ignora efeito e modificação forjados em snapshot persistido', () => {
  const installed = attachmentInstallPayload('rapid_recharge_xciter', 'asset-1');
  installed.effects = { damageBonus: 999, rangeDelta: 3, grantedQualities: ['Precisa 999'] };
  installed.mods = [
    { modId: 'reduce_inaccurate', status: 'installed', effect: { damageBonus: 999 } },
    { modId: 'reduce_inaccurate', status: 'installed', effect: { damageBonus: 999 } },
    { modId: 'reduce_inaccurate', status: 'installed', effect: { damageBonus: 999 } },
    { modId: 'illegal-mod', status: 'installed', effect: { damageBonus: 999 } }
  ];
  const weapon = { id: 'blaster_rifle', name: 'Blaster rifle', damage: 9, range: 'Longo', qualities: ['Explosão 2'], attachments: [installed] };
  const effective = effectiveWeaponWithAttachments(weapon);
  assert.equal(effective.damage, 9);
  assert.equal(effective.range, 'Longo');
  assert.equal(weaponAttackModifiers(effective).inaccurate, 1);
  assert.equal(weaponAttackModifiers(effective).accurate, 0);
  assert.equal(effective.attachmentEffects.ignoredMods, 2);
  assert.equal(weapon.attachments[0].effects.damageBonus, 999, 'função pura não altera o save original');
});

test('P128: qualidades graduadas combinam corretamente base + attachment + mods', () => {
  const spread = attachmentInstallPayload('spread_barrel', 'asset-2');
  spread.mods = [{ modId: 'blast_plus_1', status: 'installed', effect: { qualityDelta: { blast: 100 } } }];
  const weapon = { id: 'blaster_rifle', name: 'Blaster rifle', damage: 9, range: 'Longo', qualities: ['Explosão 2', 'Explosão 3'], attachments: [spread] };
  const effective = effectiveWeaponWithAttachments(weapon);
  assert.equal(weaponAttackModifiers(effective).blast, 5);
  assert.equal(effective.qualities.filter(value => value.startsWith('Explosão')).length, 1);
  assert.equal(effective.range, 'Médio');
});

test('P128: UI recebe objetivos visíveis sem flags, pistas secretas nem episódios futuros', () => {
  const adapter = getPublishedAdventureAdapter('under-black-sun');
  const started = adapter.start({});
  const view = adapter.status(started.world).uae;
  assert.equal(view.status, 'ACTIVE');
  assert.ok(view.objectives.some(objective => objective.id === 'episode-1'));
  assert.ok(!view.objectives.some(objective => objective.id === 'episode-3'));
  assert.equal('flags' in view, false);
  assert.equal('gmKnowledge' in view, false);
  assert.deepEqual(view.clocks, []);
});

test('P128: painel Android exibe objetivos da UAE sem exibir estados internos', async () => {
  const { readFile } = await import('node:fs/promises');
  const runtime = await readFile(new URL('../app/src/main/assets/vtt/r46/final-runtime.js', import.meta.url), 'utf8');
  assert.match(runtime, /OBJETIVOS DA AVENTURA/);
  assert.match(runtime, /status\?\.uae\?\.objectives/);
  assert.match(runtime, /uaeObjectivesUi/);
});

test('P128: Sob um Sol Negro mantém UAE até desfecho e arquivamento com consequência', async () => {
  const { adjudicateUnderBlackSunCheck } = await import('../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs');
  const adapter = getPublishedAdventureAdapter('under-black-sun');
  let run = adapter.start({});
  for (const choiceId of ['escape-clean', 'begin-investigation', 'go-spyder', 'spyder-return', 'go-zelcomm', 'zelcomm-clean', 'move-endgame', 'plan-direct', 'through-refinery', 'enter-abandoned', 'parley-kaato'])
    run = adapter.advance(run.world, choiceId);
  const check = adjudicateUnderBlackSunCheck(run.world, { checkIndex: 0, result: { success: true, netSuccess: 1, netAdvantage: 1 } });
  run = adapter.advance(check.world, 'kaato-surrenders');
  assert.equal(run.completed, true);
  const state = publishedAdventureUaeSnapshot(run.world, adapter);
  assert.equal(state.objectives['adventure-finale'].status, 'COMPLETED');
  assert.equal(state.objectives['adventure-finale'].progress, 1);
  assert.equal(state.canonicalEvents.filter(e => e.type === 'ADVENTURE_COMPLETED').length, 1);
  assert.equal(adapter.status(adapter.archive(run.world)).uae.status, 'ARCHIVED');
});

test('P128: Além da Orla preserva UAE e três episódios até arquivamento real', async () => {
  const { adjudicateBeyondTheRimCheck } = await import('../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs');
  const adapter = getPublishedAdventureAdapter('beyond-the-rim');
  let world = adapter.start({}).world;
  const step = id => { world = adapter.advance(world, id).world; };
  const pass = (index = 0, advantage = 1) => {
    world = adapter.adjudicateCheck(world, { checkIndex: index, result: { success: true, netSuccess: 1, netAdvantage: advantage, triumph: 0, despair: 0 } }).world;
  };
  step('meet-reom'); step('accept-job'); step('investigate-rumors'); pass(); step('return-from-rumors');
  step('wait-for-droid'); step('pursue-yiyar'); pass(0, 2); step('secure-it3po');
  step('depart-wheel-ahead'); step('plot-cholganna-route'); pass(); step('arrive-cholganna'); pass(0, 2);
  step('follow-debris'); pass(); step('reach-main-wreck'); pass(); step('wait-after-wreck'); pass();
  step('follow-beacon-contact'); pass(); step('go-to-retreat'); pass(); step('face-imperial-pressure'); pass();
  step('resolve-episode2'); step('depart-for-raxus'); pass(); step('reach-scrapheap'); pass();
  step('prepare-dropoff'); pass(); step('secure-isotech'); pass(); step('make-final-escape'); pass();
  step('complete-beyond-rim');
  const state = publishedAdventureUaeSnapshot(world, adapter);
  assert.equal(state.status, 'COMPLETED');
  assert.ok(state.canonicalEvents.filter(e => e.type === 'ADAPTER_CHECK_RESOLVED').length >= 10);
  assert.equal(state.canonicalEvents.filter(e => e.type === 'ADVENTURE_COMPLETED').length, 1);
  for (const objective of Object.values(state.objectives)) assert.equal(objective.status, 'COMPLETED');
  assert.equal(adapter.status(adapter.archive(world)).uae.status, 'ARCHIVED');
});
