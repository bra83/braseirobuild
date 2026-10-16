import { reconcilePublishedAdventureWorld, publishedAdventureUaeSnapshot, publishedAdventureUaePublicStatus, installPublishedTaskTimeModuleResolver, publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from './publishedAdventureUaeBridge.mjs';
import { assertFivePublishedAdventures } from './hybridFiveAdventurePolicy.mjs';
import * as underBlackSun from './underBlackSun.mjs';
import * as beyondTheRim from './beyondTheRim.mjs';
import * as longArmOfTheHutt from './longArmOfTheHutt.mjs';
import * as jewelOfYavin from './jewelOfYavin.mjs';
import * as maskOfThePirateQueen from './maskOfThePirateQueen.mjs';

function clone(value) { return structuredClone(value); }

const UNDER_BLACK_SUN_ADAPTER = Object.freeze({
  id: underBlackSun.UNDER_BLACK_SUN.id,
  meta: underBlackSun.UNDER_BLACK_SUN,
  modeError: 'Sob um Sol Negro exige campanha EotE ou MISTA porque a aventura usa Obligation como regra própria.',
  rewardPolicy: Object.freeze({
    credits: 1000,
    obligationReduction: 5,
    pendingObligationFlag: 'obligationPendingDelta',
    obligationType: 'Dívida',
    obligationDescription: 'Consequência da operação na Zelcomm.'
  }),
  continuation: Object.freeze({ startingPoint: 'Coruscant — Setor 943', regionLabel: 'Coruscant' }),
  status: underBlackSun.underBlackSunStatus,
  start: underBlackSun.startUnderBlackSun,
  advance: underBlackSun.advanceUnderBlackSun,
  currentScene: underBlackSun.currentUnderBlackSunScene,
  masterRequest: underBlackSun.currentUnderBlackSunMasterRequest,
  currentChecks: underBlackSun.currentUnderBlackSunChecks,
  adjudicateCheck: underBlackSun.adjudicateUnderBlackSunCheck,
  encounterSpec: underBlackSun.underBlackSunEncounterSpec,
  markEncounterActive: underBlackSun.markUnderBlackSunEncounterActive,
  recordEncounterOutcome: underBlackSun.recordUnderBlackSunEncounterOutcome,
  markRewardsApplied: underBlackSun.markUnderBlackSunRewardsApplied,
  archive: underBlackSun.archiveUnderBlackSun
});


const BEYOND_THE_RIM_ADAPTER = Object.freeze({
  id: beyondTheRim.BEYOND_THE_RIM.id,
  meta: beyondTheRim.BEYOND_THE_RIM,
  capabilities: Object.freeze({ encounters: true }),
  modeError: 'Além da Orla exige campanha EotE ou MISTA porque esta implementação segue as regras e o enquadramento de Edge of the Empire.',
  rewardPolicy: Object.freeze({ credits: 0, obligationReduction: 0 }),
  continuation: Object.freeze({ startingPoint: 'Raxus Prime', regionLabel: 'Outer Rim' }),
  status: beyondTheRim.beyondTheRimStatus,
  start: beyondTheRim.startBeyondTheRim,
  advance: beyondTheRim.advanceBeyondTheRim,
  currentScene: beyondTheRim.currentBeyondTheRimScene,
  masterRequest: beyondTheRim.currentBeyondTheRimMasterRequest,
  currentChecks: beyondTheRim.currentBeyondTheRimChecks,
  adjudicateCheck: beyondTheRim.adjudicateBeyondTheRimCheck,
  encounterSpec: beyondTheRim.beyondTheRimEncounterSpec,
  markEncounterActive: beyondTheRim.markBeyondTheRimEncounterActive,
  recordEncounterOutcome: beyondTheRim.recordBeyondTheRimEncounterOutcome,
  advanceImperialClock: beyondTheRim.advanceBeyondTheRimImperialClock,
  markRewardsApplied: beyondTheRim.markBeyondTheRimRewardsApplied,
  archive: beyondTheRim.archiveBeyondTheRim
});

const LONG_ARM_OF_THE_HUTT_ADAPTER = Object.freeze({
  id: longArmOfTheHutt.LONG_ARM_OF_THE_HUTT.id,
  meta: longArmOfTheHutt.LONG_ARM_OF_THE_HUTT,
  modeError: 'O Longo Braço do Hutt exige campanha EotE ou MISTA.',
  rewardPolicy: Object.freeze({ credits: 0, obligationReduction: 0 }),
  continuation: Object.freeze({ startingPoint: 'Tatooine — Mos Shuuta', regionLabel: 'Outer Rim' }),
  status: longArmOfTheHutt.longArmOfTheHuttStatus,
  start: longArmOfTheHutt.startLongArmOfTheHutt,
  advance: longArmOfTheHutt.advanceLongArmOfTheHutt,
  currentScene: longArmOfTheHutt.currentLongArmOfTheHuttScene,
  masterRequest: longArmOfTheHutt.currentLongArmOfTheHuttMasterRequest,
  currentChecks: longArmOfTheHutt.currentLongArmOfTheHuttChecks,
  adjudicateCheck: longArmOfTheHutt.adjudicateLongArmOfTheHuttCheck,
  recordShipObservation: longArmOfTheHutt.recordLongArmShipObservation,
  adjudicateShipTransitExtraDuration: longArmOfTheHutt.adjudicateLongArmShipTransitExtraDuration,
  discoverShipRoom: longArmOfTheHutt.discoverLongArmShipRoom,
  shipCargoTransferOptions: longArmOfTheHutt.longArmShipCargoTransferOptions,
  transferShipCargo: longArmOfTheHutt.transferLongArmShipCargo,
  recordBuraConversation: longArmOfTheHutt.recordLongArmBuraConversation,
  setPeltSearchAnnounced: longArmOfTheHutt.setLongArmPeltSearchAnnounced,
  recordNewMeenLifecycle: longArmOfTheHutt.recordLongArmNewMeenLifecycle,
  encounterSpec: longArmOfTheHutt.longArmOfTheHuttEncounterSpec,
  markEncounterActive: longArmOfTheHutt.markLongArmOfTheHuttEncounterActive,
  recordEncounterOutcome: longArmOfTheHutt.recordLongArmOfTheHuttEncounterOutcome,
  markRewardsApplied: longArmOfTheHutt.markLongArmOfTheHuttRewardsApplied,
  archive: longArmOfTheHutt.archiveLongArmOfTheHutt
});

const JEWEL_OF_YAVIN_ADAPTER = Object.freeze({
  id: jewelOfYavin.JEWEL_OF_YAVIN.id,
  meta: jewelOfYavin.JEWEL_OF_YAVIN,
  modeError: 'A Joia de Yavin exige campanha EotE ou MISTA.',
  rewardPolicy: Object.freeze({ credits: 0, obligationReduction: 0 }),
  continuation: Object.freeze({ startingPoint: 'Bespin — Cloud City', regionLabel: 'Outer Rim' }),
  status: jewelOfYavin.jewelOfYavinStatus,
  start: jewelOfYavin.startJewelOfYavin,
  advance: jewelOfYavin.advanceJewelOfYavin,
  currentScene: jewelOfYavin.currentJewelOfYavinScene,
  masterRequest: jewelOfYavin.currentJewelOfYavinMasterRequest,
  currentChecks: jewelOfYavin.currentJewelOfYavinChecks,
  adjudicateCheck: jewelOfYavin.adjudicateJewelOfYavinCheck,
  influenceBidder: jewelOfYavin.influenceJewelOfYavinBidder,
  adjudicateAuctionRound: jewelOfYavin.adjudicateJewelOfYavinAuctionRound,
  adjudicateRaceLeg: jewelOfYavin.adjudicateJewelOfYavinRaceLeg,
  adjudicateRacePlacement: jewelOfYavin.adjudicateJewelOfYavinRacePlacement,
  performBankStep: jewelOfYavin.performJewelOfYavinBankStep,
  settleBank: jewelOfYavin.settleJewelOfYavinBank,
  escapeBomb: jewelOfYavin.performJewelOfYavinEscapeBomb,
  escapeImpact: jewelOfYavin.performJewelOfYavinEscapeImpact,
  replaceSlicerTools: jewelOfYavin.replaceJewelOfYavinSlicerTools,
  buyRestrainingBolt: jewelOfYavin.buyJewelOfYavinRestrainingBolt,
  performBankEvidence: jewelOfYavin.performJewelOfYavinBankEvidence,
  performLiveDroid: jewelOfYavin.performJewelOfYavinLiveDroid,
  spendBankSymbols: jewelOfYavin.performJewelOfYavinBankSpending,
  prepareBank: jewelOfYavin.performJewelOfYavinBankPreparation,
  hireBankSlicer: jewelOfYavin.hireJewelOfYavinSlicer,
  encounterSpec: jewelOfYavin.jewelOfYavinEncounterSpec,
  markEncounterActive: jewelOfYavin.markJewelOfYavinEncounterActive,
  recordEncounterOutcome: jewelOfYavin.recordJewelOfYavinEncounterOutcome,
  markRewardsApplied: jewelOfYavin.markJewelOfYavinRewardsApplied,
  archive: jewelOfYavin.archiveJewelOfYavin
});

const MASK_OF_THE_PIRATE_QUEEN_ADAPTER = Object.freeze({
  id: maskOfThePirateQueen.MASK_OF_THE_PIRATE_QUEEN.id,
  meta: maskOfThePirateQueen.MASK_OF_THE_PIRATE_QUEEN,
  modeError: 'A Máscara da Rainha Pirata exige campanha EotE ou MISTA.',
  // SWE13 pp. 18-19, 94-95: 10,000 credits per PC, less a 500-credit
  // retainer if taken; Porel awards 1,000 extra, Venlana promises double.
  // Obligation reduction was an unsourced legacy placeholder; do not apply it.
  rewardPolicy: Object.freeze({ credits: 10000, obligationReduction: 0 }),
  rewardPolicyForWorld: world => {
    const flags = world?.publishedAdventure?.flags || {};
    const mode = flags.resolutionMode;
    const retainer = flags.contractRetainerPaid ? 500 : 0;
    if (['alliance','independent'].includes(mode)) return {credits:0,obligationReduction:0};
    if (mode === 'porel') return {credits:11000-retainer,obligationReduction:0};
    if (mode === 'venlana') return {credits:20000-retainer,obligationReduction:0};
    // Old completed P181 saves keep their previously recorded receipts.
    return {credits:10000-retainer,obligationReduction:0};
  },
  continuation: Object.freeze({ startingPoint: 'Saleucami — Taleucema', regionLabel: 'Outer Rim' }),
  status: maskOfThePirateQueen.maskOfThePirateQueenStatus,
  start: maskOfThePirateQueen.startMaskOfThePirateQueen,
  advance: maskOfThePirateQueen.advanceMaskOfThePirateQueen,
  currentScene: maskOfThePirateQueen.currentMaskOfThePirateQueenScene,
  masterRequest: maskOfThePirateQueen.currentMaskOfThePirateQueenMasterRequest,
  currentChecks: maskOfThePirateQueen.currentMaskOfThePirateQueenChecks,
  adjudicateCheck: maskOfThePirateQueen.adjudicateMaskOfThePirateQueenCheck,
  encounterSpec: maskOfThePirateQueen.maskOfThePirateQueenEncounterSpec,
  markEncounterActive: maskOfThePirateQueen.markMaskOfThePirateQueenEncounterActive,
  recordEncounterOutcome: maskOfThePirateQueen.recordMaskOfThePirateQueenEncounterOutcome,
  markRewardsApplied: maskOfThePirateQueen.markMaskOfThePirateQueenRewardsApplied,
  archive: maskOfThePirateQueen.archiveMaskOfThePirateQueen
});

const REGISTRY = new Map([
  [UNDER_BLACK_SUN_ADAPTER.id, UNDER_BLACK_SUN_ADAPTER],
  [BEYOND_THE_RIM_ADAPTER.id, BEYOND_THE_RIM_ADAPTER],
  [LONG_ARM_OF_THE_HUTT_ADAPTER.id, LONG_ARM_OF_THE_HUTT_ADAPTER],
  [JEWEL_OF_YAVIN_ADAPTER.id, JEWEL_OF_YAVIN_ADAPTER],
  [MASK_OF_THE_PIRATE_QUEEN_ADAPTER.id, MASK_OF_THE_PIRATE_QUEEN_ADAPTER]
]);

assertFivePublishedAdventures([...REGISTRY.keys()]);

export function publishedAdventureCatalog() {
  return [...REGISTRY.values()].map(adapter => ({
    id: adapter.meta.id,
    title: adapter.meta.title,
    originalTitle: adapter.meta.originalTitle,
    rulesProfile: adapter.meta.rulesProfile,
    campaignModes: [...adapter.meta.campaignModes]
  }));
}

installPublishedTaskTimeModuleResolver(id => getPublishedAdventureAdapter(id).meta);

const WRAPPED = new Map();
function integratedAdapter(adapter) {
  const normalize = world => adapter.id === 'mask-of-the-pirate-queen' ? maskOfThePirateQueen.migrateMaskOfThePirateQueenWorld(world) :
    adapter.id === 'jewel-of-yavin' ? jewelOfYavin.migrateJewelOfYavinWorld(world) : world;
  const mapWorld = (previous, world, kind, options = {}) => reconcilePublishedAdventureWorld(previous, world, adapter, { kind, ...options });
  const wrapResult = (method, kind, options) => (world, ...args) => {
    const baseline = normalize(world);
    const result = method(baseline, ...args);
    if (!result || !result.world) throw new Error(`Adaptador ${adapter.id}: retorno de ${kind} inválido.`);
    return { ...result, world: mapWorld(baseline, result.world, kind, options(...args)) };
  };
  return Object.freeze({ ...adapter,
    currentScene: world => adapter.currentScene(normalize(world)),
    currentChecks: world => adapter.currentChecks(normalize(world)),
    masterRequest: world => adapter.masterRequest(normalize(world)),
    encounterSpec: (world, encounterId) => adapter.encounterSpec(normalize(world), encounterId),
    status: world => { const w=normalize(world); return { ...adapter.status(w), uae: publishedAdventureUaePublicStatus(w, adapter) }; },
    start: (world) => {
      const result = adapter.start(normalize(world));
      return { ...result, world: mapWorld(world, result.world, 'START') };
    },
    advance: wrapResult(adapter.advance, 'CHOICE', choiceId => ({ choiceId })),
    adjudicateCheck: wrapResult(adapter.adjudicateCheck, 'CHECK_RESOLVED', details => ({ payload: { checkIndex: details?.checkIndex ?? 0 } })),
    recordEncounterOutcome: wrapResult(adapter.recordEncounterOutcome, 'ENCOUNTER_RESOLVED', details => ({ payload: { encounterId: details?.encounterId ?? null, outcome: details?.outcome ?? null } })),
    ...(adapter.advanceImperialClock ? {advanceImperialClock:wrapResult(adapter.advanceImperialClock,'IMPERIAL_CLOCK',details=>({payload:{minutes:details?.minutes??5,actionId:details?.actionId??null}}))} : {}),
    ...(adapter.influenceBidder ? { influenceBidder:wrapResult(adapter.influenceBidder,'CHECK_RESOLVED',details=>({payload:{kind:'AUCTION_INFLUENCE',actionId:details?.actionId??null}})) } : {}),
    ...(adapter.adjudicateAuctionRound ? { adjudicateAuctionRound:wrapResult(adapter.adjudicateAuctionRound,'CHECK_RESOLVED',details=>({payload:{kind:'AUCTION_ROUND',actionId:details?.actionId??null}})) } : {}),
    ...(adapter.adjudicateRaceLeg ? { adjudicateRaceLeg:wrapResult(adapter.adjudicateRaceLeg,'CHECK_RESOLVED',details=>({payload:{kind:'GRAND_PRIX_LEG',actionId:details?.actionId??null}})) } : {}),
    ...(adapter.adjudicateRacePlacement ? { adjudicateRacePlacement:wrapResult(adapter.adjudicateRacePlacement,'CHECK_RESOLVED',details=>({payload:{kind:'GRAND_PRIX_PLACEMENT',actionId:details?.actionId??null}})) } : {}),
    ...(adapter.performBankStep ? { performBankStep:wrapResult(adapter.performBankStep,'CHECK_RESOLVED',(character,details)=>({payload:{kind:'BANK_STEP',actionId:details?.actionId??null,step:details?.step??null}})) } : {}),
    ...(adapter.spendBankSymbols ? { spendBankSymbols:wrapResult(adapter.spendBankSymbols,'CHECK_RESOLVED',details=>({payload:{kind:'BANK_SLICE_SPEND',actionId:details?.actionId??null}})) } : {}),

    markEncounterActive: (world, encounterId) => { const w=normalize(world); return mapWorld(w, adapter.markEncounterActive(w, encounterId), 'ENCOUNTER_STARTED', { payload: { encounterId } }); },
    markRewardsApplied: (world, details) => {
      const w=normalize(world);
      // Never let an early reward guard mask a corrupted canonical journal.
      if (Number(w?.publishedAdventureRuntime?.schemaVersion) >= 2)
        validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(adapter.meta),w.publishedAdventureRuntime);
      return mapWorld(w, adapter.markRewardsApplied(w, details), 'REWARDS_APPLIED', { payload: { credits: Number(details?.credits) || 0 } });
    },
    archive: world => { const w=normalize(world); return mapWorld(w, adapter.archive(w), 'ARCHIVED'); },
    uaeSnapshot: world => publishedAdventureUaeSnapshot(normalize(world), adapter)
  });
}

export function getPublishedAdventureAdapter(adventureId = 'under-black-sun') {
  const id = String(adventureId ?? '').trim();
  const adapter = REGISTRY.get(id);
  if (!adapter) throw new Error('Aventura publicada desconhecida.');
  if (!WRAPPED.has(id)) WRAPPED.set(id, integratedAdapter(adapter));
  return WRAPPED.get(id);
}

export function publishedAdventureAdapterSnapshot(adventureId = 'under-black-sun') {
  const adapter = getPublishedAdventureAdapter(adventureId);
  return {
    id: adapter.id,
    meta: clone(adapter.meta),
    rewardPolicy: clone(adapter.rewardPolicy),
    continuation: clone(adapter.continuation),
    capabilities: {
      checks: typeof adapter.currentChecks === 'function' && typeof adapter.adjudicateCheck === 'function',
      encounters: adapter.capabilities?.encounters !== false && typeof adapter.encounterSpec === 'function' && typeof adapter.recordEncounterOutcome === 'function',
      rewards: typeof adapter.markRewardsApplied === 'function',
      archive: typeof adapter.archive === 'function'
    }
  };
}
