import test from 'node:test';
import assert from 'node:assert/strict';
import {
  assembleSWE02NewMeenEscort,
  approachSWE02DrombbCompound,
  chooseSWE02NewMeenAllyResolution,
  chooseSWE02NewMeenResponse,
  createSWE02NewMeenLifecycle,
  recordSWE02DrombbOfficeSearch,
  recordSWE02DrombbSafeSearch,
  recordSWE02NewMeenEscortReceipt,
  recordSWE02NewMeenCharmAttempt,
  recordSWE02NewMeenBribe,
  recordSWE02NewMeenGunfire,
  recordSWE02NewMeenRoadHarassment,
  refuseSWE02NewMeenEscort,
  resolveSWE02DrombbDetectedContact,
  resolveSWE02NewMeenCantinaOutcome,
  resolveSWE02NewMeenCoercionOfPcs,
  resolveSWE02NewMeenNightCombat,
  resolveSWE02NewMeenNightInteraction,
  resolveSWE02NewMeenNightStay,
  resolveSWE02NewMeenRetaliationChoice,
  startSWE02DrombbCantina,
  validateSWE02NewMeenLifecycle
} from '../app/src/main/assets/vtt/js/adventure/swe02NewMeenLifecycleP19.mjs';

function move(state, fn, options) {
  return fn(state, options).state;
}
function base(size = 3) { return createSWE02NewMeenLifecycle({ partySize: size }); }
function afterNightWin(size = 3) {
  let s = move(base(size), chooseSWE02NewMeenResponse, { action: 'avoid_and_stay' });
  s = move(s, resolveSWE02NewMeenNightStay, { choice: 'stay' });
  s = move(s, resolveSWE02NewMeenNightInteraction, { response: 'aggressive_move' });
  s = move(s, resolveSWE02NewMeenNightCombat, { outcome: 'thugs_driven_off' });
  return move(s, resolveSWE02NewMeenRetaliationChoice, { choice: 'go_to_compound' });
}
function readyAtCantina(size = 3) {
  let s = move(base(size), chooseSWE02NewMeenResponse, { action: 'confront_drombb' });
  return move(s, approachSWE02DrombbCompound, { precautions: true });
}
function winCantina(state) {
  let s = move(state, startSWE02DrombbCantina);
  return move(s, resolveSWE02NewMeenCantinaOutcome, { outcome: 'victory', drombbDefeated: true });
}

test('p19: party-size state supports 2, 3, 4+ groups and blocks unspecified solo force', () => {
  for (const [size, expected] of [[2, 2], [3, 3], [4, 4], [7, 4]]) {
    let s = readyAtCantina(size);
    s = move(s, startSWE02DrombbCantina);
    assert.equal(s.cantina.groupSize, expected);
    assert.equal(s.cantina.groups, 3);
    assert.equal(s.cantina.groupSize * s.cantina.groups, expected * 3);
  }
  assert.throws(() => startSWE02DrombbCantina(readyAtCantina(null)), /confirm party size/);
  assert.throws(() => createSWE02NewMeenLifecycle({ partySize: 1 }), /source gives no force size/i);
});

test('p19–20: direct visit skips night, while avoid-and-stay makes the raid eligible', () => {
  let direct = move(base(), chooseSWE02NewMeenResponse, { action: 'confront_drombb' });
  direct = move(direct, approachSWE02DrombbCompound, { precautions: true });
  assert.equal(direct.nightRaid.skipped, false);
  assert.equal(direct.stage, 'CANTINA_READY');

  let overnight = move(base(), chooseSWE02NewMeenResponse, { action: 'avoid_and_stay' });
  overnight = move(overnight, resolveSWE02NewMeenNightStay, { choice: 'visit_drombb' });
  assert.equal(overnight.nightRaid.skipped, true);
  assert.equal(overnight.stage, 'CANTINA_APPROACH');
});

test('p20: no-help branch offers escort; refusal redirects thugs to PCs then requires adjudication', () => {
  let s = move(base(), chooseSWE02NewMeenResponse, { action: 'do_nothing' });
  assert.equal(s.stage, 'AWAITING_ESCORT');
  assert.equal(s.escort.offered, true);
  s = move(s, assembleSWE02NewMeenEscort);
  assert.equal(s.events.at(-1).adults, 20);
  assert.equal(s.events.at(-1).childrenCount, null);
  s = move(s, refuseSWE02NewMeenEscort);
  assert.deepEqual(s.events.at(-1).sequence, ['coerce_pcs_to_return_to_cantina', 'violence_if_coercion_fails']);
  assert.throws(() => resolveSWE02NewMeenCoercionOfPcs(s, { coerced: false }), /explicit GM disposition/);
  s = move(s, resolveSWE02NewMeenCoercionOfPcs, { coerced: false, gmRuling: 'Resolve as a separate table encounter.' });
  assert.equal(s.stage, 'UNRESOLVED');
  assert.equal(s.unresolved[0].claim, 'P20_PC_REFUSAL_VIOLENCE');
});

test('p20: optional road harassment requires a GM disposition instead of a fabricated encounter', () => {
  let s = move(base(), chooseSWE02NewMeenResponse, { action: 'do_nothing' });
  assert.throws(() => recordSWE02NewMeenRoadHarassment(s), /explicit GM disposition/);
  s = move(s, recordSWE02NewMeenRoadHarassment, { gmDisposition: 'No harassment encounter selected for this trip.' });
  assert.equal(s.events.at(-1).source.printedPage, 20);
  assert.equal(s.unresolved[0].claim, 'P20_OPTIONAL_ROAD_HARASSMENT');
});

test('p20: existing P243 arrival receipt grants only the source-stated favor and preserves non-liberation', () => {
  let s = move(base(), chooseSWE02NewMeenResponse, { action: 'do_nothing' });
  s = move(s, assembleSWE02NewMeenEscort);
  const receipt = {
    type: 'NEW_MEEN_ESCORT_ARRIVED',
    source: { productCode: 'SWE02', printedPages: [20, 21] },
    adults: 20,
    favor: 'BURA_BAN_SIGNIFICANT_FAVOR_NOT_NEW_MEEN_LIBERATION'
  };
  s = move(s, recordSWE02NewMeenEscortReceipt, { receipt });
  assert.equal(s.stage, 'ESCORT_COMPLETED');
  assert.equal(s.escort.significantFavor, true);
  assert.equal(s.events.at(-1).newMeenLiberated, false);
  assert.throws(() => recordSWE02NewMeenEscortReceipt(s, { receipt }), /expected AWAITING_ESCORT/);
});

test('p20: speeder surrender is recorded without inventing its later disposition; provocation triggers combat', () => {
  let s = move(base(), chooseSWE02NewMeenResponse, { action: 'avoid_and_stay' });
  s = move(s, resolveSWE02NewMeenNightStay, { choice: 'stay' });
  assert.throws(() => resolveSWE02NewMeenNightInteraction(s, { response: 'hand_over_speeder' }), /explicit GM disposition/);
  s = move(s, resolveSWE02NewMeenNightInteraction, { response: 'hand_over_speeder', gmDisposition: 'Resolve who has the speeder after the developers leave.' });
  assert.equal(s.nightRaid.combatTriggered, false);
  assert.equal(s.unresolved[0].claim, 'P20_SPEeder_HANDOVER');

  let combat = move(base(), chooseSWE02NewMeenResponse, { action: 'avoid_and_stay' });
  combat = move(combat, resolveSWE02NewMeenNightStay, { choice: 'stay' });
  combat = move(combat, resolveSWE02NewMeenNightInteraction, { response: 'speak_out_of_turn' });
  assert.equal(combat.stage, 'NIGHT_COMBAT');
  combat = move(combat, resolveSWE02NewMeenNightCombat, { outcome: 'thugs_driven_off' });
  assert.equal(combat.approach.remainingThugGroups, 2);
  assert.equal(combat.nightRaid.groupStopped, true);
  assert.equal(combat.stage, 'DROMBB_RETALIATION');
  assert.throws(() => resolveSWE02NewMeenRetaliationChoice(combat, { choice: 'defend_new_meen' }), /explicit GM disposition/);
  const hold = move(combat, resolveSWE02NewMeenRetaliationChoice, { choice: 'defend_new_meen', gmDisposition: 'Resolve Drombb’s remaining groups at New Meen.' });
  assert.equal(hold.events.at(-1).sourceSaysRemainingMenAttackNewMeen, true);
  assert.equal(hold.events.at(-1).remainingThugGroups, 2);
  assert.equal(hold.stage, 'UNRESOLVED');
});

test('p22: cautious approach avoids checks; detected polite negotiation guarantees gunfire alert', () => {
  const cautious = move(move(base(), chooseSWE02NewMeenResponse, { action: 'confront_drombb' }), approachSWE02DrombbCompound, { precautions: true });
  assert.equal(cautious.approach.detected, false);
  assert.equal(cautious.events.at(-1).skillChecksRequired, false);

  let detected = move(move(base(), chooseSWE02NewMeenResponse, { action: 'confront_drombb' }), approachSWE02DrombbCompound, { precautions: false });
  assert.equal(detected.stage, 'DETECTED_CONTACT');
  detected = move(detected, resolveSWE02DrombbDetectedContact, { response: 'negotiate_politely' });
  assert.equal(detected.approach.gunfire, true);
  assert.equal(detected.events.at(-1).allDefendersAlerted, true);
  detected = move(detected, recordSWE02NewMeenGunfire, { cause: 'Aqualish fires after the warning.' });
  assert.equal(detected.events.at(-1).defendersAlerted, true);
});

test('p21: prior night group is excluded; Charm does not roll and thugs resume fighting', () => {
  let s = afterNightWin(3);
  s = move(s, approachSWE02DrombbCompound, { precautions: true });
  s = move(s, chooseSWE02NewMeenAllyResolution, { method: 'abstracted' });
  assert.equal(s.events.at(-1).mechanicalImplementation, 'EXISTING_P243_API');
  s = move(s, recordSWE02NewMeenCharmAttempt);
  assert.equal(s.events.at(-1).check, 'NONE');
  assert.equal(s.events.at(-1).fightResumes, true);
  s = move(s, startSWE02DrombbCantina);
  assert.equal(s.cantina.groups, 2);
  assert.equal(s.events.at(-1).priorNightGroupExcluded, true);
  assert.equal(s.cantina.groupSize * s.cantina.groups, 6);
});

test('p23: safe methods include key, blast, and Hard Skulduggery; credit grant is one-time and GM-gated', () => {
  let s = winCantina(readyAtCantina());
  s = move(s, recordSWE02DrombbSafeSearch, { method: 'blast' });
  assert.equal(s.safe.creditsGranted, 2000);
  assert.equal(s.events.at(-1).exactSafeValue, 2000);
  assert.throws(() => recordSWE02DrombbSafeSearch(s, { method: 'key' }), /already searched/);

  let late = winCantina(readyAtCantina());
  late = move(late, recordSWE02DrombbSafeSearch, { method: 'skulduggery', successful: true, gmConfirmedQuick: false });
  assert.equal(late.safe.creditsGranted, 0);
  assert.equal(late.unresolved[0].claim, 'P23_LOOT_RACE');
});

test('p23: the office clue appears only after the existing Hard Computers result passes', () => {
  let s = winCantina(readyAtCantina());
  assert.throws(() => recordSWE02DrombbOfficeSearch(s), /resolve the printed Hard Computers/);
  s = move(s, recordSWE02DrombbOfficeSearch, { computersHardCheckPassed: false });
  assert.deepEqual(s.messages.claimIds, []);
  let found = winCantina(readyAtCantina());
  found = move(found, recordSWE02DrombbOfficeSearch, { computersHardCheckPassed: true });
  assert.equal(found.messages.found, true);
  assert.deepEqual(found.messages.claimIds, [
    'THWHEEK_SIVOR_AND_GEOSIS_DEALS', 'THWHEEK_DUKE_DIMMOCK_OPPORTUNITY', 'SIVOR_DECAPITATION_COVERUP'
  ]);
  assert.equal(found.events.at(-1).foundMessageCount, 2);
  assert.match(found.events.at(-1).disclosedClaims[0].summary, /Geonosians/);
});

test('p23: lifecycle imports the existing per-thug bribe receipt without charging twice or bribing Drombb', () => {
  let s = readyAtCantina(2);
  const failed = {
    type: 'NEW_MEEN_THUG_BRIBE', source: { productCode: 'SWE02', printedPage: 23 },
    creditsOffered: 450, creditsSpent: 0, skillKey: 'negociacion', difficulty: 1,
    success: false, check: { success: false }
  };
  s = move(s, recordSWE02NewMeenBribe, { receipt: failed });
  assert.equal(s.cantina.bribedThugs, 0);
  const paid = { ...failed, creditsSpent: 450, success: true, check: { success: true } };
  s = move(s, recordSWE02NewMeenBribe, { receipt: paid });
  assert.equal(s.cantina.bribedThugs, 1);
  assert.throws(() => recordSWE02NewMeenBribe(s, { receipt: { ...paid, targetProfileKey: 'laoth_angu_drombb' } }), /verified P241 per-thug bribe receipt/);
  s = move(s, startSWE02DrombbCantina);
  assert.equal(s.cantina.thugCount, 5);
  assert.equal(s.events.at(-1).drombbPresent, true);
});

test('p23: victory requires Drombb defeated; defeat records B’ura/Ota rescue and no liberation', () => {
  let s = move(readyAtCantina(), startSWE02DrombbCantina);
  assert.throws(() => resolveSWE02NewMeenCantinaOutcome(s, { outcome: 'victory' }), /requires Drombb defeated/);
  assert.throws(() => resolveSWE02NewMeenCantinaOutcome(s, { outcome: 'pcs_defeated' }), /select the printed p.21 or p.23/);
  s = move(s, resolveSWE02NewMeenCantinaOutcome, { outcome: 'pcs_defeated', defeatResolution: 'p23_capture_rescue' });
  assert.equal(s.stage, 'DEFEAT_RESCUED');
  assert.deepEqual(s.events.at(-1).rescueBy, ['B’ura B’an', 'Ota']);
  assert.equal(s.events.at(-1).communityResult, 'NOT_LIBERATED');
  assert.equal(s.events.at(-1).proceedToAct2, true);
  assert.equal(s.events.at(-1).capturedAndBoundFor, 'Tatooine');
  assert.equal(s.events.at(-1).rescueMethod, 'pick_storage_closet_lock_then_pass_sleeping_guard');
});

test('p21 and p23 defeat passages stay distinct rather than auto-merging their outcomes', () => {
  let s = move(readyAtCantina(), startSWE02DrombbCantina);
  s = move(s, resolveSWE02NewMeenCantinaOutcome, { outcome: 'pcs_defeated', defeatResolution: 'p21_residents_rescue' });
  assert.equal(s.stage, 'AWAITING_ESCORT');
  assert.equal(s.escort.offered, true);
  assert.equal(s.events.at(-1).residentsBeatenAndAbandonBySunrise, true);
  assert.equal(s.events.at(-1).significantEscortFavorOffered, true);
  assert.equal(s.events.at(-1).proceedToAct2, false);
});

test('lifecycle event stream is serializable and rejects tampered provenance/revisions', () => {
  const s = afterNightWin();
  const roundTrip = JSON.parse(JSON.stringify(s));
  assert.equal(validateSWE02NewMeenLifecycle(roundTrip), true);
  roundTrip.events[0].source.printedPage = 24;
  assert.throws(() => validateSWE02NewMeenLifecycle(roundTrip), /source event is invalid/);
  const mismatched = afterNightWin();
  mismatched.approach.remainingThugGroups = 3;
  assert.throws(() => validateSWE02NewMeenLifecycle(mismatched), /remaining thug groups mismatch/);
});
