import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter, publishedAdventureAdapterSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { startLongArmOfTheHutt, advanceLongArmOfTheHutt } from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';

const clone = structuredClone;
const moduleFor = adapter => publishedAdventureRuntimeModule(adapter.meta);

for (const { id } of publishedAdventureCatalog()) {
  test(`P129 ${id}: partida e primeira escolha auditadas, restauráveis, sem alterar mundo original`, () => {
    const adapter = getPublishedAdventureAdapter(id);
    const initialWorld = { external: { preserved: true } };
    const before = clone(initialWorld);
    const start = adapter.start(initialWorld);
    const module = moduleFor(adapter);
    const first = publishedAdventureUaeSnapshot(start.world, adapter);
    assert.deepEqual(initialWorld, before);
    assert.equal(first.canonicalEvents.filter(e => e.type === 'NODE_ENTER').length, 1);
    assert.equal(first.canonicalEvents.filter(e => e.type === 'ADAPTER_STARTED').length, 1);
    assert.equal(validatePublishedAdventureRuntimeState(module, first, start.world.publishedAdventure), true);
    const choice = start.node.choices[0].id;
    const next = adapter.advance(start.world, choice);
    const state = publishedAdventureUaeSnapshot(next.world, adapter);
    assert.equal(validatePublishedAdventureRuntimeState(module, state, next.world.publishedAdventure), true);
    assert.equal(state.canonicalEvents.filter(e => e.type === 'NODE_TRANSITION').length, 1);
    assert.deepEqual(AdventureRuntime.restore(module, JSON.stringify(state)).getState(), state);
    assert.equal(first.canonicalEvents.filter(e => e.type === 'NODE_TRANSITION').length, 0);
  });
}

test('P129: save legado avançado recebe projeção de objetivos sem mutação nem novo turno', () => {
  const adapter = getPublishedAdventureAdapter('long-arm-of-the-hutt');
  const raw = startLongArmOfTheHutt({}).world;
  const legacy = advanceLongArmOfTheHutt(raw, 'dock-ryloth').world;
  const before = clone(legacy);
  const publicStatus = adapter.status(legacy).uae;
  assert.equal(publicStatus.currentNodeId, legacy.publishedAdventure.currentNodeId);
  assert.equal(publicStatus.status, 'ACTIVE');
  assert.equal(publicStatus.objectives.find(x => x.id === 'episode-1').status, 'ACTIVE');
  assert.equal('flags' in publicStatus, false);
  assert.deepEqual(legacy.publishedAdventure, before.publishedAdventure);
  assert.equal(legacy.publishedAdventureRuntime, undefined);
  const integrated = adapter.advance(legacy, 'head-to-geonosis');
  const journal = integrated.world.publishedAdventureRuntime.canonicalEvents;
  assert.equal(journal.filter(e => e.type === 'LEGACY_SNAPSHOT_MIGRATED').length, 1);
  assert.equal(journal.filter(e => e.type === 'NODE_TRANSITION').length, 1);
});

test('P129: migração recupera episódios percorridos, sem inventar cenas ou recompensas', () => {
  const adapter = getPublishedAdventureAdapter('beyond-the-rim');
  const legacy = adapter.start({}).world;
  delete legacy.publishedAdventureRuntime;
  const episodeThree = Object.entries(adapter.meta.nodes).find(([, n]) => n.episode === 3)[0];
  legacy.publishedAdventure.currentNodeId = episodeThree;
  legacy.publishedAdventure.visitedNodeIds.push(episodeThree);
  const original = clone(legacy);
  const state = publishedAdventureUaeSnapshot(legacy, adapter);
  assert.equal(state.objectives['episode-1'].status, 'COMPLETED');
  assert.equal(state.objectives['episode-2'].status, 'COMPLETED');
  assert.equal(state.objectives['episode-3'].status, 'ACTIVE');
  assert.equal(state.objectives['adventure-finale'].status, 'ACTIVE');
  assert.equal(state.canonicalEvents.filter(e => e.type === 'EPISODE_PROGRESS_RECONCILED').length, 2);
  assert.equal(state.canonicalEvents.filter(e => e.type === 'NODE_TRANSITION').length, 0);
  assert.deepEqual(legacy, original);
});

test('P129: save legado encerrado e arquivado recupera final e histórico uma vez', () => {
  const adapter = getPublishedAdventureAdapter('mask-of-the-pirate-queen');
  const legacy = adapter.start({}).world;
  delete legacy.publishedAdventureRuntime;
  legacy.publishedAdventure.status = 'archived';
  const first = publishedAdventureUaeSnapshot(legacy, adapter);
  const second = publishedAdventureUaeSnapshot(legacy, adapter);
  assert.deepEqual(first.canonicalEvents.map(e => [e.id, e.type]), second.canonicalEvents.map(e => [e.id, e.type]));
  assert.deepEqual(Object.values(first.objectives).map(o => [o.id, o.status]), Object.values(second.objectives).map(o => [o.id, o.status]));
  assert.equal(first.status, 'ARCHIVED');
  assert.equal(first.objectives['adventure-finale'].status, 'COMPLETED');
  assert.equal(first.canonicalEvents.filter(e => e.type === 'ADVENTURE_COMPLETED').length, 1);
  assert.equal(first.canonicalEvents.filter(e => e.type === 'ADVENTURE_ARCHIVED').length, 1);
  assert.equal(validatePublishedAdventureRuntimeState(moduleFor(adapter), first, legacy.publishedAdventure), true);
});

test('P129: alteração externa de cena recupera marcos sem repetir início nem prêmio', () => {
  const adapter = getPublishedAdventureAdapter('jewel-of-yavin');
  const world = adapter.start({}).world;
  const episodeTwo = Object.entries(adapter.meta.nodes).find(([, n]) => n.episode === 2)[0];
  world.publishedAdventure.currentNodeId = episodeTwo;
  world.publishedAdventure.visitedNodeIds.push(episodeTwo);
  const repaired = adapter.markRewardsApplied(world, { credits: 0 });
  const state = repaired.publishedAdventureRuntime;
  assert.equal(state.currentNodeId, episodeTwo);
  assert.equal(state.objectives['episode-1'].status, 'COMPLETED');
  assert.equal(state.canonicalEvents.filter(e => e.type === 'EXTERNAL_PROGRESS_RECONCILED').length, 1);
  assert.equal(state.canonicalEvents.filter(e => e.type === 'ADAPTER_STARTED').length, 1);
  assert.equal(state.canonicalEvents.filter(e => e.type === 'ADVENTURE_COMPLETED').length, 0);
  const again = adapter.markRewardsApplied(repaired, { credits: 0 });
  assert.equal(again.publishedAdventureRuntime.canonicalEvents.filter(e => e.type === 'EXTERNAL_PROGRESS_RECONCILED').length, 1);
  assert.equal(again.publishedAdventureRuntime.canonicalEvents.filter(e => e.type === 'EPISODE_PROGRESS_RECONCILED').length, 1);
});

test('P129: flags apagadas no legado não reaparecem após restauração UAE', () => {
  const adapter = getPublishedAdventureAdapter('long-arm-of-the-hutt');
  const world = adapter.start({}).world;
  world.publishedAdventureRuntime.flags.stale = true;
  world.publishedAdventure.flags.stale = true;
  delete world.publishedAdventure.flags.stale;
  const synced = adapter.markRewardsApplied(world, { credits: 0 });
  assert.equal(Object.hasOwn(synced.publishedAdventureRuntime.flags, 'stale'), false);
  assert.equal(Object.hasOwn(world.publishedAdventureRuntime.flags, 'stale'), true);
});

test('P129: journal duplicado é rejeitado sem mutar save ou conceder recompensa', () => {
  const adapter = getPublishedAdventureAdapter('long-arm-of-the-hutt');
  const world = adapter.start({}).world;
  world.publishedAdventureRuntime.canonicalEvents.push(clone(world.publishedAdventureRuntime.canonicalEvents[0]));
  const before = clone(world);
  assert.throws(() => adapter.markRewardsApplied(world, { credits: 1000 }), /duplicados/);
  assert.deepEqual(world, before);
});

test('P129: validador rejeita divergência de cena, sequência e objetivo inválido', () => {
  const adapter = getPublishedAdventureAdapter('under-black-sun');
  const world = adapter.start({}).world;
  const module = moduleFor(adapter);
  const valid = world.publishedAdventureRuntime;
  assert.equal(validatePublishedAdventureRuntimeState(module, valid, world.publishedAdventure), true);
  assert.throws(() => validatePublishedAdventureRuntimeState(module, { ...valid, eventSequence: 0 }), /Sequência/);
  assert.throws(() => validatePublishedAdventureRuntimeState(module, { ...valid, currentNodeId: 'absent' }), /cena inexistente/);
  assert.throws(() => validatePublishedAdventureRuntimeState(module, { ...valid, objectives: { 'episode-1': { status: 'ACTIVE', progress: -1, target: 1 } } }), /Objetivo/);
  assert.throws(() => validatePublishedAdventureRuntimeState(module, valid, { ...world.publishedAdventure, currentNodeId: 'absent' }), /diverge/);
});

test('P129: catálogo não anuncia combate para Além da Orla enquanto não houver encontro implementado', () => {
  const unsupported = publishedAdventureAdapterSnapshot('beyond-the-rim');
  assert.equal(unsupported.capabilities.encounters, true);
  assert.equal(unsupported.capabilities.checks, true);
  assert.equal(unsupported.capabilities.archive, true);
  for (const id of ['under-black-sun', 'long-arm-of-the-hutt', 'jewel-of-yavin', 'mask-of-the-pirate-queen'])
    assert.equal(publishedAdventureAdapterSnapshot(id).capabilities.encounters, true);
});

import { attachmentInstallPayload, effectiveWeaponWithAttachments } from '../app/src/main/assets/vtt/js/rules/attachments.mjs';
import { weaponAttackModifiers } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';

const blaster = (attachments, hardPoints = 4) => ({ id: 'blaster_rifle', name: 'Blaster rifle', damage: 9, range: 'Longo', hardPoints, qualities: [], attachments });

test('P129: attachment incompatível injetado no save não altera arma nem mods', () => {
  const fake = attachmentInstallPayload('bowcaster_accelerator', 'forged-1');
  fake.mods = [{ modId: 'grant_autofire', status: 'installed' }];
  const original = blaster([fake]);
  const before = clone(original);
  const effective = effectiveWeaponWithAttachments(original);
  assert.equal(effective.damage, 9);
  assert.equal(weaponAttackModifiers(effective).autoFireAvailable, false);
  assert.equal(effective.attachmentEffects.ignoredAttachments, 1);
  assert.deepEqual(original, before);
});

test('P129: hard points vêm da arma e catálogo, nunca do custo forjado no save', () => {
  const rapid = attachmentInstallPayload('rapid_recharge_xciter', 'asset-a');
  const spread = attachmentInstallPayload('spread_barrel', 'asset-b');
  spread.hardPointsRequired = 0;
  spread.mods = [{ modId: 'blast_plus_1', status: 'installed' }];
  const effective = effectiveWeaponWithAttachments(blaster([rapid, spread], 2));
  assert.equal(effective.attachmentEffects.hardPointsUsed, 1);
  assert.equal(effective.attachmentEffects.ignoredAttachments, 1);
  assert.equal(effective.range, 'Longo');
  assert.equal(weaponAttackModifiers(effective).blast, 0);
  assert.equal(weaponAttackModifiers(effective).inaccurate, 3);
});

test('P129: duplicar ID de ativo não duplica attachment nem efeitos', () => {
  const rapid = attachmentInstallPayload('rapid_recharge_xciter', 'asset-duplicate');
  const effective = effectiveWeaponWithAttachments(blaster([rapid, clone(rapid)]));
  assert.equal(effective.attachmentEffects.attachments.length, 1);
  assert.equal(effective.attachmentEffects.ignoredAttachments, 1);
  assert.equal(weaponAttackModifiers(effective).inaccurate, 3);
});

test('P129: armas legadas sem capacidade declarada preservam attachments válidos', () => {
  const rapid = attachmentInstallPayload('rapid_recharge_xciter', 'legacy-a');
  const spread = attachmentInstallPayload('spread_barrel', 'legacy-b');
  const weapon = blaster([rapid, spread]);
  delete weapon.hardPoints;
  const effective = effectiveWeaponWithAttachments(weapon);
  assert.equal(effective.attachmentEffects.ignoredAttachments, 0);
  assert.equal(effective.attachmentEffects.hardPointsUsed, 3);
  assert.equal(effective.range, 'Médio');
  assert.equal(weaponAttackModifiers(effective).blast, 4);
});

test('P129: mods de attachment desativado não entram na resolução de ataque', () => {
  const rapid = attachmentInstallPayload('rapid_recharge_xciter', 'disabled-a');
  rapid.disabled = true;
  rapid.mods = [{ modId: 'reduce_inaccurate', status: 'installed' }];
  const effective = effectiveWeaponWithAttachments(blaster([rapid]));
  assert.equal(effective.attachmentEffects.attachments.length, 0);
  assert.equal(weaponAttackModifiers(effective).inaccurate, 0);
});
