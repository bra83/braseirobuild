import test from 'node:test';
import assert from 'node:assert/strict';
import { publishedAdventureCatalog, getPublishedAdventureAdapter, publishedAdventureAdapterSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { validateAdventureModule } from '../app/src/main/assets/vtt/js/adventure/adventureSchema.mjs';
import { LONG_ARM_OF_THE_HUTT } from '../app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';
import { JEWEL_OF_YAVIN } from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import { MASK_OF_THE_PIRATE_QUEEN } from '../app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs';
import { adjudicateUnderBlackSunCheck } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import { adjudicateBeyondTheRimCheck } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

test('Wave 4: catálogo contém 5 aventuras oficiais publicadas', () => {
  const catalog = publishedAdventureCatalog();
  assert.equal(catalog.length, 5);
  const ids = catalog.map(c => c.id);
  assert.ok(ids.includes('under-black-sun'));
  assert.ok(ids.includes('beyond-the-rim'));
  assert.ok(ids.includes('long-arm-of-the-hutt'));
  assert.ok(ids.includes('jewel-of-yavin'));
  assert.ok(ids.includes('mask-of-the-pirate-queen'));
});

test('Wave 4: todas as novas aventuras passam na validação do Universal Adventure Engine', () => {
  assert.equal(validateAdventureModule(LONG_ARM_OF_THE_HUTT), true);
  assert.equal(validateAdventureModule(JEWEL_OF_YAVIN), true);
  assert.equal(validateAdventureModule(MASK_OF_THE_PIRATE_QUEEN), true);
});

test('Wave 4: adapters das novas aventuras executam início e avanço de cenas sem erro', () => {
  for (const id of ['long-arm-of-the-hutt', 'jewel-of-yavin', 'mask-of-the-pirate-queen']) {
    const adapter = getPublishedAdventureAdapter(id);
    assert.equal(typeof adapter.start, 'function');
    assert.equal(typeof adapter.advance, 'function');
    assert.equal(typeof adapter.status, 'function');
    assert.equal(typeof adapter.currentScene, 'function');

    const started = adapter.start({});
    assert.equal(started.progress.id, id);
    assert.equal(started.progress.status, 'ACTIVE');
    assert.ok(started.node.choices.length > 0);

    const firstChoice = started.node.choices[0].id;
    const next = adapter.advance(started.world, firstChoice);
    assert.ok(next.progress.visitedNodeIds.length >= 2);
  }
});

test('Universal Adventure Engine: capacidades refletem implementação real, sem anunciar combate inexistente', () => {
  for (const id of ['under-black-sun', 'beyond-the-rim', 'long-arm-of-the-hutt', 'jewel-of-yavin', 'mask-of-the-pirate-queen']) {
    const snap = publishedAdventureAdapterSnapshot(id);
    assert.equal(snap.capabilities.checks, true, `${id} deve ter checks`);
    assert.equal(snap.capabilities.encounters, true, `${id}: capacidade real de combate`);
    assert.equal(snap.capabilities.rewards, true, `${id} deve ter rewards`);
    assert.equal(snap.capabilities.archive, true, `${id} deve ter archive`);
  }
});

test('Universal Adventure Engine: fluxo completo de jogo até conclusão e arquivamento nas 5 aventuras', () => {
  // Test 1: Under a Black Sun (negotiation path)
  {
    const adapter = getPublishedAdventureAdapter('under-black-sun');
    let session = adapter.start({});
    for (const choiceId of ['escape-clean', 'begin-investigation', 'go-spyder', 'spyder-return', 'go-zelcomm', 'zelcomm-clean', 'move-endgame', 'plan-direct', 'through-refinery', 'enter-abandoned', 'parley-kaato']) {
      session = adapter.advance(session.world, choiceId);
    }
    const res = adjudicateUnderBlackSunCheck(session.world, { checkIndex: 0, result: { success: true, netSuccess: 1, netAdvantage: 1 } });
    session = adapter.advance(res.world, 'kaato-surrenders');
    assert.ok(session.completed, 'under-black-sun deve concluir via rendição');
    const archived = adapter.archive(session.world);
    assert.equal(adapter.status(archived).archived, true);
  }

  // Test 2: Beyond the Rim (episodes 1 to 3 with check adjudication)
  {
    const adapter = getPublishedAdventureAdapter('beyond-the-rim');
    const step = (w, id) => adapter.advance(w, id).world;
    const pass = (w, i = 0, adv = 1) => adjudicateBeyondTheRimCheck(w, { checkIndex: i, result: { success: true, netSuccess: 1, netAdvantage: adv, triumph: 0, despair: 0 } }).world;

    let w = adapter.start({}).world;
    w = step(w, 'meet-reom');
    w = step(w, 'accept-job');
    w = step(w, 'investigate-rumors');
    w = pass(w);
    w = step(w, 'return-from-rumors');
    w = step(w, 'wait-for-droid');
    w = step(w, 'pursue-yiyar');
    w = pass(w, 0, 2);
    w = step(w, 'secure-it3po');
    w = step(w, 'depart-wheel-ahead');
    w = step(w, 'plot-cholganna-route');
    w = pass(w);
    w = step(w, 'arrive-cholganna');
    w = pass(w, 0, 2);
    w = step(w, 'follow-debris');
    w = pass(w);
    w = step(w, 'reach-main-wreck');
    w = pass(w);
    w = step(w, 'wait-after-wreck');
    w = pass(w);
    w = step(w, 'follow-beacon-contact');
    w = pass(w);
    w = step(w, 'go-to-retreat');
    w = pass(w);
    w = step(w, 'face-imperial-pressure');
    w = pass(w);
    w = step(w, 'resolve-episode2');
    w = step(w, 'depart-for-raxus');
    w = pass(w);
    w = step(w, 'reach-scrapheap');
    w = pass(w);
    w = step(w, 'prepare-dropoff');
    w = pass(w);
    w = step(w, 'secure-isotech');
    w = pass(w);
    w = step(w, 'make-final-escape');
    w = pass(w);
    const finalSession = adapter.advance(w, 'complete-beyond-rim');
    assert.ok(finalSession.completed, 'beyond-the-rim deve concluir com êxito');
    const archived = adapter.archive(finalSession.world);
    assert.equal(adapter.status(archived).archived, true);
  }

  // Test 3, 4, 5: Long Arm of the Hutt, Jewel of Yavin, Mask of the Pirate Queen
  for (const id of ['long-arm-of-the-hutt', 'jewel-of-yavin', 'mask-of-the-pirate-queen']) {
    const adapter = getPublishedAdventureAdapter(id);
    let session = adapter.start({});
    let safety = 0;
    while (!session.completed && safety < 30) {
      safety++;
      if(id==='mask-of-the-pirate-queen' && session.node.id==='flagship-showdown' && !session.node.choices?.length){
        const resolved=adapter.recordEncounterOutcome(session.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'});
        session={...session,world:resolved.world,node:adapter.status(resolved.world).node};
      }
      const choices = session.node.choices;
      if (!choices || choices.length === 0) break;
      session = adapter.advance(session.world, choices[0].id);
    }
    assert.ok(session.completed, `Aventura ${id} deve alcançar a conclusão.`);
    const archived = adapter.archive(session.world);
    assert.equal(adapter.status(archived).archived, true);
  }
});
