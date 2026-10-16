// SWE02 printed pp.19–23: source-bounded New Meen encounter lifecycle.
// This module is deliberately pure. The host application owns persistence,
// dice execution, inventory/credit transactions, and combat proof.
const clone = value => structuredClone(value);
const SOURCE = Object.freeze({
  title: 'The Long Arm of the Hutt',
  productCode: 'SWE02',
  authority: 'OFFICIAL_PDF_TEXT_VERIFIED'
});

export const SWE02_NEW_MEEN_LIFECYCLE_VERSION = 1;
export const SWE02_NEW_MEEN_SOURCE = SOURCE;

const PAGE = Object.freeze({
  arrival: 19,
  refusal: 20,
  night: 20,
  militia: 21,
  compound: 22,
  resolution: 23
});
const MESSAGE_CLAIMS = Object.freeze([
  {
    id: 'THWHEEK_SIVOR_AND_GEOSIS_DEALS',
    summary: 'Thwheek describes Teemo-linked dealings with Geonosians and his role in Sivor’s death.'
  },
  {
    id: 'THWHEEK_DUKE_DIMMOCK_OPPORTUNITY',
    summary: 'The messages mention losing Duke Piddock’s support and considering Dimmock as a possible partner.'
  },
  {
    id: 'SIVOR_DECAPITATION_COVERUP',
    summary: 'A later message refers to Sivor’s decapitation and keeping it from the Geonosian contact.'
  }
]);

const STAGES = new Set([
  'ARRIVAL', 'AWAITING_ESCORT', 'THUGS_TARGET_PCS', 'NIGHT_ELIGIBLE',
  'NIGHT_STANDOFF', 'NIGHT_COMBAT', 'DROMBB_RETALIATION', 'CANTINA_APPROACH',
  'DETECTED_CONTACT', 'CANTINA_READY', 'CANTINA_COMBAT',
  'VICTORY', 'DEFEAT_RESCUED', 'ESCORT_COMPLETED', 'UNRESOLVED'
]);

function assertState(state) {
  if (!state || state.schemaVersion !== SWE02_NEW_MEEN_LIFECYCLE_VERSION ||
      !STAGES.has(state.stage) || !Array.isArray(state.events) ||
      !Number.isSafeInteger(state.revision) || state.revision < 0) {
    throw new Error('SWE02 New Meen lifecycle: invalid state.');
  }
  if (state.partySize !== null && (!Number.isInteger(state.partySize) || state.partySize < 2 || state.partySize > 8)) {
    throw new Error('SWE02 New Meen lifecycle: party size must be source-confirmed (2–8) or unresolved.');
  }
  return state;
}

function append(state, type, printedPage, fields = {}) {
  const next = clone(assertState(state));
  const event = {
    id: `new-meen-${next.revision + 1}`,
    type,
    revision: next.revision + 1,
    source: { ...SOURCE, printedPage },
    ...clone(fields)
  };
  next.revision++;
  next.events.push(event);
  return { state: next, event };
}

function requireStage(state, allowed) {
  assertState(state);
  if (!allowed.includes(state.stage)) throw new Error(`SWE02 New Meen lifecycle: expected ${allowed.join(' or ')}, got ${state.stage}.`);
}

function gmNote(value) {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error('SWE02 New Meen lifecycle: this source branch needs an explicit GM disposition.');
  }
  return value.trim();
}

export function createSWE02NewMeenLifecycle({ partySize = null } = {}) {
  if (partySize !== null && (!Number.isInteger(partySize) || partySize < 2 || partySize > 8)) {
    throw new Error('SWE02 New Meen lifecycle: source gives no force size for solo or unconfirmed parties.');
  }
  return {
    schemaVersion: SWE02_NEW_MEEN_LIFECYCLE_VERSION,
    revision: 0,
    stage: 'ARRIVAL',
    partySize,
    response: null,
    escort: { offered: false, assembled: false, departed: false, arrived: false, abandoned: false, significantFavor: false },
    nightRaid: { eligible: false, skipped: false, interaction: null, combatTriggered: false, outcome: null, groupStopped: false },
    approach: { precautions: null, detected: false, gunfire: false, remainingThugGroups: null },
    cantina: { started: false, groupSize: null, groups: null, thugCount: null, bribedThugs: 0, bribeReceipts: [], charmAttempted: false, outcome: null },
    safe: { searched: false, method: null, creditsGranted: 0 },
    messages: { checked: false, found: false, claimIds: [] },
    unresolved: [],
    events: []
  };
}

// The printed scenario supports these response branches, but does not dictate
// how an individual table's players choose to help.
export function chooseSWE02NewMeenResponse(state, { action } = {}) {
  requireStage(state, ['ARRIVAL']);
  if (!['confront_drombb', 'avoid_and_stay', 'do_nothing', 'other'].includes(action)) {
    throw new Error('SWE02 New Meen lifecycle: unknown arrival response.');
  }
  const next = clone(state);
  next.response = action;
  if (action === 'confront_drombb') next.stage = 'CANTINA_APPROACH';
  else if (action === 'avoid_and_stay') {
    next.stage = 'NIGHT_ELIGIBLE';
    next.nightRaid.eligible = true;
  } else if (action === 'do_nothing') {
    next.stage = 'AWAITING_ESCORT';
    next.escort.offered = true;
  } else next.stage = 'UNRESOLVED';
  const out = append(next, 'ARRIVAL_RESPONSE_RECORDED', PAGE.arrival, { action });
  out.state.stage = next.stage;
  return out;
}

export function refuseSWE02NewMeenEscort(state) {
  requireStage(state, ['AWAITING_ESCORT']);
  const next = clone(state);
  next.escort.abandoned = true;
  next.stage = 'THUGS_TARGET_PCS';
  return append(next, 'ESCORT_REFUSED_THUGS_REDIRECT_TO_PCS', PAGE.refusal, {
    sequence: ['coerce_pcs_to_return_to_cantina', 'violence_if_coercion_fails'],
    nextResolution: 'GM_ADJUDICATION'
  });
}

export function resolveSWE02NewMeenCoercionOfPcs(state, { coerced, gmRuling } = {}) {
  requireStage(state, ['THUGS_TARGET_PCS']);
  if (typeof coerced !== 'boolean') throw new Error('SWE02 New Meen lifecycle: record the GM-adjudicated coercion result.');
  const next = clone(state);
  if (coerced) next.stage = 'CANTINA_APPROACH';
  else {
    // The source says the thugs resort to violence; it does not give a check,
    // initiative detail, or a scripted combat profile for this branch.
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P20_PC_REFUSAL_VIOLENCE', gmRuling: gmNote(gmRuling) });
  }
  return append(next, 'PC_COERCION_RESOLVED', PAGE.refusal, {
    coerced, consequence: coerced ? 'RETURN_TO_CANTINA' : 'VIOLENCE',
    gmRuling: coerced ? null : gmNote(gmRuling)
  });
}

export function recordSWE02NewMeenRoadHarassment(state, { gmDisposition } = {}) {
  requireStage(state, ['AWAITING_ESCORT']);
  const note = gmNote(gmDisposition);
  const next = clone(state);
  next.unresolved.push({ claim: 'P20_OPTIONAL_ROAD_HARASSMENT', gmRuling: note });
  return append(next, 'OPTIONAL_ROAD_HARASSMENT_GM_DISPOSITION', PAGE.refusal, { gmRuling: note });
}

export function chooseSWE02NewMeenAllyResolution(state, { method } = {}) {
  requireStage(state, ['CANTINA_APPROACH', 'CANTINA_READY']);
  if (!['narrative', 'abstracted', 'delegated'].includes(method)) {
    throw new Error('SWE02 New Meen lifecycle: choose one of the three source-listed allied NPC methods.');
  }
  const next = clone(state);
  next.allyResolutionMethod = method;
  return append(next, 'TWILEK_ALLY_RESOLUTION_SELECTED', PAGE.militia, {
    method,
    sourceDefined: true,
    mechanicalImplementation: method === 'abstracted' ? 'EXISTING_P243_API' : 'GM_RUNS_SOURCE_METHOD'
  });
}

export function assembleSWE02NewMeenEscort(state) {
  requireStage(state, ['AWAITING_ESCORT']);
  if (state.escort.assembled) throw new Error('SWE02 New Meen lifecycle: escort already assembled.');
  const next = clone(state);
  next.escort.assembled = true;
  return append(next, 'ESCORT_ASSEMBLED', PAGE.refusal, {
    adults: 20, childrenCount: null, destination: 'Nabat', threatMayHarassOnRoad: true
  });
}

// Import, rather than duplicate, the existing P243 travel and world-clock
// transaction. This only aligns the page-19–23 lifecycle with that receipt.
export function recordSWE02NewMeenEscortReceipt(state, { receipt } = {}) {
  requireStage(state, ['AWAITING_ESCORT']);
  if (!receipt || !['NEW_MEEN_ESCORT_ARRIVED', 'NEW_MEEN_ESCORT_ABANDONED'].includes(receipt.type) ||
      receipt.source?.productCode !== 'SWE02' || !Array.isArray(receipt.source.printedPages) ||
      !receipt.source.printedPages.includes(20) || !receipt.source.printedPages.includes(21)) {
    throw new Error('SWE02 New Meen lifecycle: import a verified P243 escort receipt.');
  }
  const next = clone(state);
  if (receipt.type === 'NEW_MEEN_ESCORT_ARRIVED') {
    if (receipt.favor !== 'BURA_BAN_SIGNIFICANT_FAVOR_NOT_NEW_MEEN_LIBERATION') {
      throw new Error('SWE02 New Meen lifecycle: escort arrival must preserve the source distinction from liberation.');
    }
    next.escort.assembled = true;
    next.escort.departed = true;
    next.escort.arrived = true;
    next.escort.significantFavor = true;
    next.stage = 'ESCORT_COMPLETED';
  } else {
    next.escort.abandoned = true;
    next.escort.significantFavor = false;
  }
  const out = append(next, 'EXISTING_P243_ESCORT_RECEIPT_IMPORTED', PAGE.refusal, {
    importedType: receipt.type,
    adults: receipt.adults ?? 20,
    significantFavor: receipt.type === 'NEW_MEEN_ESCORT_ARRIVED',
    newMeenLiberated: false
  });
  out.state.stage = next.stage;
  return out;
}

export function resolveSWE02NewMeenNightStay(state, { choice, gmDisposition } = {}) {
  requireStage(state, ['NIGHT_ELIGIBLE']);
  if (!['stay', 'visit_drombb', 'other'].includes(choice)) throw new Error('SWE02 New Meen lifecycle: unknown night choice.');
  const next = clone(state);
  if (choice === 'visit_drombb') {
    next.nightRaid.skipped = true;
    next.nightRaid.eligible = false;
    next.stage = 'CANTINA_APPROACH';
  } else if (choice === 'stay') next.stage = 'NIGHT_STANDOFF';
  else {
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P20_NIGHT_CHOICE', gmRuling: gmNote(gmDisposition) });
  }
  const out = append(next, 'NIGHT_SCENE_CHOICE', PAGE.night, {
    choice, consequence: choice === 'visit_drombb' ? 'SKIP_DRUNK_AND_DISORDERLY' : choice === 'stay' ? 'AWAKENED_BY_CRASH' : 'GM_DISPOSITION_REQUIRED',
    gmRuling: choice === 'other' ? gmNote(gmDisposition) : null
  });
  out.state.stage = next.stage;
  return out;
}

export function resolveSWE02NewMeenNightInteraction(state, { response, gmDisposition } = {}) {
  requireStage(state, ['NIGHT_STANDOFF']);
  if (!['hand_over_speeder', 'refuse_speeder', 'speak_out_of_turn', 'aggressive_move', 'other'].includes(response)) {
    throw new Error('SWE02 New Meen lifecycle: unknown night-raid response.');
  }
  const next = clone(state);
  next.nightRaid.interaction = response;
  if (response === 'hand_over_speeder') {
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P20_SPEeder_HANDOVER', gmRuling: gmNote(gmDisposition) });
  } else if (response === 'speak_out_of_turn' || response === 'aggressive_move') {
    next.nightRaid.combatTriggered = true;
    next.stage = 'NIGHT_COMBAT';
  } else if (response === 'refuse_speeder') {
    next.stage = 'NIGHT_STANDOFF';
  } else {
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P20_NIGHT_INTERACTION', gmRuling: gmNote(gmDisposition) });
  }
  const out = append(next, 'NIGHT_RAID_INTERACTION', PAGE.night, {
    response, combatTriggered: next.nightRaid.combatTriggered,
    handoverOutcomeRequiresGM: response === 'hand_over_speeder'
  });
  out.state.stage = next.stage;
  return out;
}

export function resolveSWE02NewMeenNightCombat(state, { outcome, gmDisposition } = {}) {
  requireStage(state, ['NIGHT_COMBAT']);
  if (!['thugs_driven_off', 'pcs_defeated', 'other'].includes(outcome)) throw new Error('SWE02 New Meen lifecycle: unknown night combat outcome.');
  const next = clone(state);
  next.nightRaid.outcome = outcome;
  if (outcome === 'thugs_driven_off') {
    next.nightRaid.groupStopped = true;
    next.approach.remainingThugGroups = 2;
    next.stage = 'DROMBB_RETALIATION';
  } else {
    // The printed pages do not specify a night-raid defeat transition.
    const note = gmNote(gmDisposition);
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P20_NIGHT_COMBAT_OUTCOME', outcome, gmRuling: note });
  }
  const out = append(next, 'NIGHT_RAID_COMBAT_RESOLVED', PAGE.night, {
    outcome, remainingCantinaGroups: next.approach.remainingThugGroups,
    gmRuling: outcome === 'thugs_driven_off' ? null : gmNote(gmDisposition)
  });
  out.state.stage = next.stage;
  return out;
}

export function resolveSWE02NewMeenRetaliationChoice(state, { choice, gmDisposition } = {}) {
  requireStage(state, ['DROMBB_RETALIATION']);
  if (!['go_to_compound', 'defend_new_meen', 'other'].includes(choice)) {
    throw new Error('SWE02 New Meen lifecycle: choose a response to Drombb’s source-stated retaliation.');
  }
  const next = clone(state);
  if (choice === 'go_to_compound') next.stage = 'CANTINA_APPROACH';
  else {
    next.stage = 'UNRESOLVED';
    next.unresolved.push({
      claim: choice === 'defend_new_meen' ? 'P19_DROMBB_RETALIATES_AT_NEW_MEEN' : 'P19_ALT_RESPONSE',
      gmRuling: gmNote(gmDisposition)
    });
  }
  const out = append(next, 'DROMBB_REMAINING_FORCE_RESPONSE', PAGE.arrival, {
    choice,
    sourceSaysRemainingMenAttackNewMeen: choice === 'defend_new_meen',
    remainingThugGroups: next.approach.remainingThugGroups,
    sourceDoesNotSpecifyTheDetailedRetaliationCombat: choice === 'defend_new_meen',
    gmRuling: choice === 'go_to_compound' ? null : gmNote(gmDisposition)
  });
  out.state.stage = next.stage;
  return out;
}

export function approachSWE02DrombbCompound(state, { precautions } = {}) {
  requireStage(state, ['CANTINA_APPROACH']);
  if (typeof precautions !== 'boolean') throw new Error('SWE02 New Meen lifecycle: explicitly record whether precautions were taken.');
  const next = clone(state);
  next.approach.precautions = precautions;
  next.approach.detected = !precautions;
  if (precautions) {
    next.stage = 'CANTINA_READY';
    next.approach.remainingThugGroups ??= 3;
  } else next.stage = 'DETECTED_CONTACT';
  const out = append(next, 'COMPOUND_APPROACH', PAGE.compound, {
    precautions, detected: !precautions,
    skillChecksRequired: precautions ? false : null,
    detectedContactSent: !precautions
  });
  out.state.stage = next.stage;
  return out;
}

export function resolveSWE02DrombbDetectedContact(state, { response, gmDisposition } = {}) {
  requireStage(state, ['DETECTED_CONTACT']);
  if (!['negotiate_politely', 'turn_back', 'other'].includes(response)) throw new Error('SWE02 New Meen lifecycle: unknown detected-contact response.');
  const next = clone(state);
  if (response === 'negotiate_politely') {
    // The source explicitly says an Aqualish fires even during polite negotiation.
    next.approach.gunfire = true;
    next.stage = 'CANTINA_READY';
    next.approach.remainingThugGroups ??= 3;
  } else {
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P22_DETECTED_APPROACH', response, gmRuling: gmNote(gmDisposition) });
  }
  const out = append(next, 'DETECTED_CONTACT_RESOLVED', PAGE.compound, {
    response, aqualishFired: response === 'negotiate_politely',
    allDefendersAlerted: response === 'negotiate_politely',
    gmRuling: response === 'negotiate_politely' ? null : gmNote(gmDisposition)
  });
  out.state.approach.gunfire = next.approach.gunfire;
  out.state.stage = next.stage;
  return out;
}

export function recordSWE02NewMeenGunfire(state, { cause } = {}) {
  requireStage(state, ['CANTINA_READY', 'CANTINA_COMBAT']);
  if (typeof cause !== 'string' || !cause.trim()) throw new Error('SWE02 New Meen lifecycle: record what caused gunfire.');
  const next = clone(state);
  next.approach.gunfire = true;
  if (next.stage === 'CANTINA_READY') next.stage = 'CANTINA_COMBAT';
  return append(next, 'GUNFIRE_ALERT_PROPAGATED', PAGE.compound, {
    cause: cause.trim(), defendersAlerted: true
  });
}

export function startSWE02DrombbCantina(state) {
  requireStage(state, ['CANTINA_READY', 'CANTINA_COMBAT']);
  if (state.cantina.started) throw new Error('SWE02 New Meen lifecycle: cantina encounter already started.');
  if (state.partySize === null) throw new Error('SWE02 New Meen lifecycle: confirm party size; the source does not define a solo force.');
  const groupSize = Math.min(4, state.partySize);
  const groups = state.approach.remainingThugGroups ?? 3;
  const baseThugs = groupSize * groups;
  const bribedThugs = state.cantina.bribeReceipts.filter(receipt => receipt.success).length;
  if (bribedThugs > baseThugs) throw new Error('SWE02 New Meen lifecycle: bribes exceed the remaining thug group.');
  const next = clone(state);
  next.cantina.started = true;
  next.cantina.groupSize = groupSize;
  next.cantina.groups = groups;
  next.cantina.thugCount = baseThugs - bribedThugs;
  next.stage = 'CANTINA_COMBAT';
  const out = append(next, 'CANTINA_ENCOUNTER_STARTED', PAGE.compound, {
    groupSize, groups, thugCount: next.cantina.thugCount, bribedThugs,
    drombbPresent: true, priorNightGroupExcluded: state.nightRaid.groupStopped
  });
  out.state.stage = 'CANTINA_COMBAT';
  return out;
}

// Wrap the existing P241 transaction receipt so this lifecycle can show the
// correct number of remaining minions without rolling or charging credits a
// second time.
export function recordSWE02NewMeenBribe(state, { receipt } = {}) {
  requireStage(state, ['CANTINA_READY']);
  if (!receipt || receipt.type !== 'NEW_MEEN_THUG_BRIBE' || receipt.source?.productCode !== 'SWE02' ||
      receipt.source?.printedPage !== 23 || !Number.isInteger(receipt.creditsOffered) ||
      receipt.creditsOffered < 400 || receipt.creditsOffered > 500 ||
      receipt.success !== receipt.check?.success || receipt.creditsSpent !== (receipt.success ? receipt.creditsOffered : 0) ||
      receipt.skillKey !== 'negociacion' || receipt.difficulty !== 1 || receipt.targetProfileKey === 'laoth_angu_drombb') {
    throw new Error('SWE02 New Meen lifecycle: import a verified P241 per-thug bribe receipt.');
  }
  const next = clone(state);
  const max = Math.min(4, next.partySize) * (next.approach.remainingThugGroups ?? 3);
  if (next.cantina.bribeReceipts.filter(x => x.success).length >= max) throw new Error('SWE02 New Meen lifecycle: no eligible thug remains; Drombb cannot be bribed.');
  next.cantina.bribeReceipts.push(clone(receipt));
  next.cantina.bribedThugs = next.cantina.bribeReceipts.filter(x => x.success).length;
  return append(next, 'THUG_BRIBE_IMPORTED', PAGE.resolution, {
    creditsOffered: receipt.creditsOffered,
    creditsSpent: receipt.creditsSpent,
    success: receipt.success,
    remainingThugsAfterBribe: max - next.cantina.bribedThugs,
    drombbBribed: false
  });
}

export function recordSWE02NewMeenCharmAttempt(state) {
  requireStage(state, ['CANTINA_READY', 'CANTINA_COMBAT']);
  if (state.cantina.charmAttempted) throw new Error('SWE02 New Meen lifecycle: Charm already attempted.');
  const next = clone(state);
  next.cantina.charmAttempted = true;
  // Source says Charm never settles matters: thugs feign a truce, reposition,
  // and resume fighting. It does not give this branch a Charm check.
  return append(next, 'CHARM_ATTEMPT_FAILED_SOURCE', PAGE.resolution, {
    check: 'NONE', thugsFeignTruce: true, thugsReposition: true, fightResumes: true
  });
}

export function resolveSWE02NewMeenCantinaOutcome(state, { outcome, drombbDefeated = false, defeatResolution, gmDisposition } = {}) {
  requireStage(state, ['CANTINA_COMBAT']);
  if (!state.cantina.started) throw new Error('SWE02 New Meen lifecycle: start cantina encounter first.');
  if (!['victory', 'pcs_defeated', 'other'].includes(outcome)) throw new Error('SWE02 New Meen lifecycle: unknown cantina outcome.');
  const next = clone(state);
  next.cantina.outcome = outcome;
  if (outcome === 'victory') {
    if (drombbDefeated !== true) throw new Error('SWE02 New Meen lifecycle: page-23 victory requires Drombb defeated.');
    next.stage = 'VICTORY';
  } else if (outcome === 'pcs_defeated') {
    if (!['p21_residents_rescue', 'p23_capture_rescue'].includes(defeatResolution)) {
      throw new Error('SWE02 New Meen lifecycle: select the printed p.21 or p.23 defeat resolution explicitly.');
    }
    if (defeatResolution === 'p21_residents_rescue') {
      next.stage = 'AWAITING_ESCORT';
      next.escort.offered = true;
    } else next.stage = 'DEFEAT_RESCUED';
  }
  else {
    const note = gmNote(gmDisposition);
    next.stage = 'UNRESOLVED';
    next.unresolved.push({ claim: 'P23_CANTINA_OTHER_OUTCOME', gmRuling: note });
  }
  const sourcePage = outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue' ? PAGE.militia : PAGE.resolution;
  const out = append(next, 'CANTINA_OUTCOME', sourcePage, {
    outcome, drombbDefeated: outcome === 'victory',
    celebrationAvailable: outcome === 'victory',
    communityResult: outcome === 'victory' ? 'THREATS_DEFEATED' : outcome === 'pcs_defeated' ? 'NOT_LIBERATED' : 'GM_DISPOSITION_REQUIRED',
    defeatResolution: outcome === 'pcs_defeated' ? defeatResolution : null,
    residentsBeatenAndAbandonBySunrise: outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue',
    thugsLeaveAfterBeatingResidents: outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue',
    eldersDiscussSellingSettlement: outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue',
    buraDisappointed: outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue',
    significantEscortFavorOffered: outcome === 'pcs_defeated' && defeatResolution === 'p21_residents_rescue',
    capturedAndBoundFor: outcome === 'pcs_defeated' && defeatResolution === 'p23_capture_rescue' ? 'Tatooine' : null,
    rescueBy: outcome === 'pcs_defeated' && defeatResolution === 'p23_capture_rescue' ? ['B’ura B’an', 'Ota'] : [],
    rescueMethod: outcome === 'pcs_defeated' && defeatResolution === 'p23_capture_rescue' ? 'pick_storage_closet_lock_then_pass_sleeping_guard' : null,
    proceedToAct2: outcome === 'pcs_defeated' && defeatResolution === 'p23_capture_rescue',
    gmRuling: outcome === 'other' ? gmNote(gmDisposition) : null
  });
  out.state.stage = next.stage;
  return out;
}

export function recordSWE02DrombbOfficeSearch(state, { computersHardCheckPassed } = {}) {
  requireStage(state, ['VICTORY']);
  if (typeof computersHardCheckPassed !== 'boolean') throw new Error('SWE02 New Meen lifecycle: resolve the printed Hard Computers check first.');
  if (state.messages.checked) throw new Error('SWE02 New Meen lifecycle: office search already recorded.');
  const next = clone(state);
  next.messages.checked = true;
  next.messages.found = computersHardCheckPassed;
  next.messages.claimIds = computersHardCheckPassed ? [
    'THWHEEK_SIVOR_AND_GEOSIS_DEALS',
    'THWHEEK_DUKE_DIMMOCK_OPPORTUNITY',
    'SIVOR_DECAPITATION_COVERUP'
  ] : [];
  return append(next, 'DROMBB_OFFICE_MESSAGES_SEARCHED', PAGE.resolution, {
    check: { skill: 'Computers', difficulty: 'Hard', passed: computersHardCheckPassed },
    foundMessageCount: computersHardCheckPassed ? 2 : 0,
    disclosedClaimIds: [...next.messages.claimIds],
    disclosedClaims: computersHardCheckPassed ? MESSAGE_CLAIMS.map(claim => ({ ...claim })) : [],
    informationOptional: true
  });
}

export function recordSWE02DrombbSafeSearch(state, { method, successful = true, gmConfirmedQuick = true } = {}) {
  requireStage(state, ['VICTORY']);
  if (!['key', 'blast', 'skulduggery'].includes(method)) throw new Error('SWE02 New Meen lifecycle: source-safe method must be key, blast, or Skulduggery.');
  if (method === 'skulduggery' && typeof successful !== 'boolean') throw new Error('SWE02 New Meen lifecycle: Hard Skulduggery result required.');
  if (typeof gmConfirmedQuick !== 'boolean') throw new Error('SWE02 New Meen lifecycle: GM must adjudicate whether PCs reached loot before the Twi’leks.');
  if (state.safe.searched) throw new Error('SWE02 New Meen lifecycle: safe already searched.');
  const methodSuccess = method !== 'skulduggery' || successful;
  const granted = methodSuccess && gmConfirmedQuick ? 2000 : 0;
  const next = clone(state);
  next.safe.searched = true;
  next.safe.method = method;
  next.safe.creditsGranted = granted;
  if (!gmConfirmedQuick) next.unresolved.push({ claim: 'P23_LOOT_RACE', gmRuling: 'PCs arrived after Twi’lek ransacking began.' });
  return append(next, 'DROMBB_SAFE_SEARCHED', PAGE.resolution, {
    method, skulduggeryDifficulty: method === 'skulduggery' ? 'Hard' : null,
    successful: methodSuccess, gmConfirmedQuick,
    creditsGranted: granted, exactSafeValue: 2000,
    inventoryContents: { drinks: 'present', rationPackets: 'present' }
  });
}

export function validateSWE02NewMeenLifecycle(state) {
  assertState(state);
  if (state.revision !== state.events.length) throw new Error('SWE02 New Meen lifecycle: revision/event count mismatch.');
  for (let i = 0; i < state.events.length; i++) {
    const event = state.events[i];
    if (event.id !== `new-meen-${i + 1}` || event.revision !== i + 1 || event.source?.productCode !== 'SWE02' ||
        !Object.values(PAGE).includes(event.source.printedPage)) {
      throw new Error('SWE02 New Meen lifecycle: source event is invalid.');
    }
  }
  if (state.nightRaid.groupStopped && state.approach.remainingThugGroups !== 2) throw new Error('SWE02 New Meen lifecycle: remaining thug groups mismatch.');
  if (state.cantina.bribedThugs !== state.cantina.bribeReceipts.filter(receipt => receipt.success).length ||
      state.cantina.bribeReceipts.some(receipt => receipt.source?.productCode !== 'SWE02' || receipt.source?.printedPage !== 23 ||
        receipt.type !== 'NEW_MEEN_THUG_BRIBE' || !Number.isInteger(receipt.creditsOffered) || receipt.creditsOffered < 400 || receipt.creditsOffered > 500 ||
        receipt.success !== receipt.check?.success || receipt.creditsSpent !== (receipt.success ? receipt.creditsOffered : 0) ||
        receipt.targetProfileKey === 'laoth_angu_drombb')) {
    throw new Error('SWE02 New Meen lifecycle: bribe receipt invalid.');
  }
  if (state.cantina.started && (state.cantina.groups !== state.approach.remainingThugGroups || state.cantina.groupSize !== Math.min(4, state.partySize) ||
      state.cantina.thugCount !== state.cantina.groupSize * state.cantina.groups - state.cantina.bribedThugs)) {
    throw new Error('SWE02 New Meen lifecycle: cantina force does not match source party scaling.');
  }
  if (state.cantina.outcome === 'victory' && (state.stage !== 'VICTORY' || !state.events.some(e => e.type === 'CANTINA_OUTCOME' && e.drombbDefeated))) {
    throw new Error('SWE02 New Meen lifecycle: victory without Drombb defeat.');
  }
  if (state.safe.creditsGranted !== 0 && (state.safe.creditsGranted !== 2000 || !state.safe.searched || state.stage !== 'VICTORY')) {
    throw new Error('SWE02 New Meen lifecycle: invalid safe reward.');
  }
  if (state.messages.found !== (state.messages.claimIds.length > 0) || state.messages.claimIds.some(id =>
    !MESSAGE_CLAIMS.some(claim => claim.id === id))) throw new Error('SWE02 New Meen lifecycle: message claims are invalid.');
  return true;
}
