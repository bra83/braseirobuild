import React, { useEffect, useRef, useState } from 'react';
import { AppHeader } from './components/AppHeader';
import { MainNavigation, AppTab } from './components/MainNavigation';
import { GameView } from './components/GameView';
import type { PartyMapMove } from './components/EriadorMap';
import { CharacterSheetView } from './components/CharacterSheetView';
import { CharacterRegistryView } from './components/CharacterRegistryView';
import { CharacterPathsCreator } from './components/CharacterPathsCreator';
import { DiceView } from './components/DiceView';
import { NpcRegistryView } from './components/NpcRegistryView';
import { SoundLibraryView } from './components/SoundLibraryView';
import { ManualLibraryView } from './components/ManualLibraryView';
import { CampaignView } from './components/CampaignView';
import { SettingsView } from './components/SettingsView';
import { TokensView } from './components/TokensView';
import { WorldAliveView } from './components/WorldAliveView';
import { MasterHubView } from './components/MasterHubView';
import { CompanyHubView } from './components/CompanyHubView';
import { MechanicsHub } from './components/MechanicsHub';
import { JourneyResolutionPanel } from './components/JourneyResolutionPanel';
import { MechanicalStatusStrip } from './components/MechanicalStatusStrip';
import { attachResolutionSummary } from './lib/actionResolution';
import { CombatTacticalView } from './components/CombatTacticalView';
import { OrganizationsView } from './components/OrganizationsView';
import { QuickAudioPanel } from './components/QuickAudioPanel';
import { answerRulesQuestion, generateDirectorPlan, generateNarration, generateNarrationFromPlan, generateSceneImage } from './services/geminiService';
import { composeVisualPrompt } from './utils/visualStyle';
import {
  CampaignState, Character, CharacterSheet, getActiveCampaign, getCharacters, getStoreData, IndividualToken, listSoundMetadata, Npc, PendingTest,
  recoverLatestCampaignSnapshot, createCampaignSnapshot, RollItem, saveEmergencyCampaignMirror, saveStoreData, saveMasterTransaction, saveCanonicalTransaction, SceneNode, SoundItem, SoundKeywordRule, clearStore,
} from './lib/db';
import { buildOrchestratorContext, buildTurnContext } from './lib/contextBuilder';
import { AudioEngine } from './lib/AudioEngine';
import { AudioDiagnostics } from './lib/audioDiagnostics';
import { ambienceSearchAliases, findBestSoundForEvent, findBestSoundForQuery, musicSearchAliases, planSounds, SoundEffectEvent, SoundPlan } from './lib/soundPlanner';
import { selectSceneAudioFromCatalog, SceneAudioContext } from './lib/sceneAudioPlanner';
import { applyRulesToSound, listSoundKeywordRules } from './lib/soundKeywordRules';
import { ensureSoundKeywordLibrary } from './lib/soundLibraryBootstrap';
import { RollResult, applyFeatOneAsEye, rollMagicalSuccess, secureRoll } from './lib/dice';
import { CampaignEvent, GeminiStoryResponse, SceneAudioPlan, StoryChoice, VoiceSegment, TtsVoiceName } from './types';
import type { RuleAnswer, MasterCommand, CompanyState, JourneyRole } from './lib/master/types';
import { applyMasterCommands } from './lib/master/commands';
import { reconcileCompanyDraft } from './lib/master/companySave';
import { patronProfile } from './lib/rules/core47';
import { isAdvisoryMasterRejection, sanitizeDirectorCommands } from './lib/master/commandSanitizer';
import { createDefaultMasterState, normalizeMasterState, recordHeroSpotlight } from './lib/master/state';
import { rememberDirectorPlan } from './lib/master/memory';
import { speakWithAndroid, stopAndroidSpeech } from './lib/narration';
import { createSkillTest, shouldSuppressRoutineTest } from './lib/rules/tests';
import { attributeForSkill, findSkill, targetNumberForSkill } from './lib/characterRules';
import { listNativeSounds } from './lib/native';
import { createCharacterFromPath, createRandomCharacter, type CharacterPathCreationOptions } from './lib/characterFactory';
import { characterPathCompanyDeltas, sheetFeatOneBecomesEye } from './lib/characterPaths';
import { forodwaithJourneyDiceModifier, forodwaithRollMode } from './lib/rules/loneLands';
import { normalizeEquipmentMechanics } from './lib/rules/equipment';
import { shadowAtLimit } from './lib/rules/shadow';
import { applyEldarAutomaticMagicalSuccess, canSpendHopeForSupplementMagicalSuccess, hasVirtue, magicalSuccessSource } from './lib/rules/supplementMechanics';
import { rollJourneySkill } from './lib/rules/journeyRoll';
import { journeySupporterIds } from './lib/rules/journey';
import { calculateInitialStriderEyeAwareness } from './lib/rules/striderMode';
import { coreCultureId } from './lib/coreCulturalVirtues';
import { supportSuccessDice } from './lib/rules/support';
import { spendHope as spendHopeMechanically } from './lib/rules/hopeShadow';
import { blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice } from './lib/rules/magicalTreasure';
import { applyIllLuckToResult, curseDarkeningDicePenalty } from './lib/rules/magicalGearMechanics';
import { importCharacterJson } from './lib/characterPortability';
import { loadMapDiscovery, loadRuntimeMapManifest, loadRuntimeTerraMediaLocations, loadTerraMediaRuntimeContext, MAP_DISCOVERY_CHANGED_EVENT, resolveHexNarrativeLocation } from './lib/map';
import type { MapDiscovery } from './lib/map';
import { reconcileCanonicalState } from './lib/consistency';
import { migrateCampaignTo300, needsMigration300 } from './lib/migration300';
import { normalizeSceneTransitionCommands } from './lib/master/sceneMachine';
import { normalizeUnstartedFormalPhase } from './lib/master/planNormalizer';
import { autoAssignNpcTokens, getNpcTokenCatalog, syncBundledNpcTokens } from './lib/npcTokenLibrary';
import { AdventureProfileId, getAdventureProfile } from './lib/adventureProfile';
import { runStartupSupervisor } from './lib/autonomy/startupSupervisor';
import type { HealthReport } from './lib/autonomy/types';
import { TurnCoordinator } from './lib/autonomy/turnCoordinator';
import { finalizeBarbaraNarrative, getBarbaraDeviceProfile, getBarbaraPlan, inspectBarbaraTurn, projectCampaignForBarbara, validateBarbaraNarration, validateBarbaraState } from './lib/barbaraBridge';
import { createLocalDirectorPlan, createLocalNarration, isRecoverableGeminiFailure } from './lib/autonomy/localFallbackDirector';
import { buildSceneContinuityAnchor, buildSceneEnvironmentBinding, continuityPrompt, enforceDirectorContinuity, ensureEnvironmentAnchoredResponse, environmentBindingPrompt, narrationNeedsContinuityRepair, timePeriodFromHour } from './lib/sceneContinuity';
import { buildAutonomousEnvironmentContext, enrichAudioPlanWithEnvironment, prepareEnvironmentAdvance, prepareEnvironmentNarrativeMemory } from './lib/autonomy/environment';
import { MANUALS_DRIVE_FOLDER_ID } from './lib/driveFolders';
import { sanitizeNarrativeResponse } from './lib/narrativeSanitizer';
import { groundDirectorPlanInManuals } from './lib/ruleEvidence';
import { chooseOpeningArchitecture, chooseOpeningClock, openingInstruction, rememberOpening } from './lib/openingPlanner';
import { diversifySceneChoices, rememberChoices } from './lib/choiceDiversifier';
import { imageTargetFor, sceneImageFingerprint, targetMatchesScene } from './lib/sceneImageBinding';
import { enrichNpcPopulation } from './lib/npcPersonalityCatalog';
import { assessNarrativeQuality, buildNarrativeCheckpoint, classifyNarrativeIntent, enforceNarrativeIntent, narrativeCheckpointPrompt, professionalRepairBrief } from './lib/narrativeEngine';
import { assessNarrativeRhythm, buildNarrativeRhythmPlan, narrativeRhythmPrompt, rhythmRepairBrief } from './lib/narrativeRhythm';
import { reconcileSceneNpcPresence, synchronizeNpcPresenceFlags } from './lib/sceneEntitySync';
import { buildStoredSceneVisualPrompt } from './lib/sceneVisualPrompt';


const JOURNEY_ROLE_SKILL: Record<JourneyRole,string> = {
  guide: 'Viagem', scout: 'Exploração', hunter: 'Caçada', lookout: 'Vigilância',
};

function journeySkillRank(character: Character, skillName: string): number {
  return Math.max(0, Number(findSkill(character.sheet, skillName)?.rank) || 0);
}

function classifyJourneyRegion(region: string, tags: string[] = []): 'border'|'wild'|'dark' {
  const text = (region + ' ' + tags.join(' ')).normalize('NFD').replace(/[̀-ͯ]/g,'').toLowerCase();
  if (/dark|shadow|sombr|mordor|angmar|barrow|tumular/.test(text)) return 'dark';
  if (/border|fronteira|shire|condado|bree|bri|rivendell|valfenda|lindon/.test(text)) return 'border';
  return 'wild';
}

function journeyFeatResults(region: 'border'|'wild'|'dark'): number[] {
  const count = region === 'wild' ? 1 : 2;
  return Array.from({length:count},()=>secureRoll(1,12));
}

function reconcileCharacterPathPartyEffects(campaign:CampaignState,characters:Character[],partyCharacterIds:string[]):CampaignState {
  const master=normalizeMasterState(campaign);
  const next=characterPathCompanyDeltas(characters,partyCharacterIds);
  const oldFellowship=Number(master.company.characterPathFellowshipDelta)||0;
  const oldEye=Number(master.company.characterPathEyeDelta)||0;
  const fellowshipMax=Math.max(0,(Number(master.company.fellowshipMax)||0)-oldFellowship+next.fellowship);
  const fellowshipCurrent=Math.max(0,Math.min(fellowshipMax,(Number(master.company.fellowshipCurrent)||0)-oldFellowship+next.fellowship));
  const eyeAwareness=Math.max(0,(Number(master.company.eyeAwareness)||0)-oldEye+next.eye);
  return {
    ...campaign,
    masterState:{
      ...master,
      company:{
        ...master.company,
        fellowshipMax,
        fellowshipCurrent,
        eyeAwareness,
        characterPathFellowshipDelta:next.fellowship,
        characterPathEyeDelta:next.eye,
      },
      updatedAt:Date.now(),
    },
    updatedAt:Date.now(),
  };
}

function applySoloCharacterRules(characters:Character[],partyCharacterIds:string[],rulesMode:'standard'|'strider'):Character[] {
  const soloStrider=rulesMode==='strider'&&partyCharacterIds.length===1;
  const party=new Set(partyCharacterIds);
  return characters.map(character=>{
    if(!party.has(character.id)) return character;
    const sheet=structuredClone(character.sheet);
    sheet.other.rulesMode=soloStrider?'strider':'standard';
    const features=(sheet.other.distinctiveFeatures||[]).filter(feature=>feature!=='Peregrino');
    if(soloStrider) features.push('Peregrino');
    sheet.other.distinctiveFeatures=features;
    return {...character,sheet,updatedAt:Date.now()};
  });
}

function immersiveJourneyUiEnabled(): boolean {
  try {
    const query = new URLSearchParams(window.location.search);
    if (query.get('classic') === '1' || query.get('immersive') === '0') return false;
    if (query.get('immersive') === '1') return true;
    return localStorage.getItem('braseiro.tor2e.immersiveUi') !== '0';
  } catch {
    return true;
  }
}

async function yieldForFirstPaint(): Promise<void> {
  await new Promise<void>(resolve => {
    if (typeof requestAnimationFrame !== 'function' || document.visibilityState === 'hidden') {
      window.setTimeout(resolve, 0);
      return;
    }
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
  });
}

function scheduleStartupMaintenance(task: () => Promise<void>): void {
  const start = () => window.setTimeout(() => {
    void task().catch(error => console.error('[BRASEIRO_STARTUP_MAINTENANCE]', error));
  }, 250);
  const requestIdle = (window as any).requestIdleCallback as ((callback: () => void, options?: { timeout: number }) => number) | undefined;
  if (requestIdle) requestIdle(start, { timeout: 1_500 });
  else start();
}

function markBootStage(stage: string): void {
  try {
    (window as any).__braseiroBootStage = stage;
    document.documentElement.dataset.braseiroBootStage = stage;
  } catch { /* diagnóstico não pode bloquear o boot */ }
  console.info(`[BRASEIRO_BOOT] ${stage}`);
}

const DEFAULT_CHARACTER_SHEET: CharacterSheet = {
  identity: {
    name: 'Novo Herói', culture: '', vocation: '', lifestyle: '', age: 20,
    description: '', patron: '', company: '',
  },
  attributes: {
    strength: 0, heart: 0, wits: 0, enduranceCurrent: 0, enduranceMax: 0,
    hopeCurrent: 0, hopeMax: 0, shadow: 0, fatigue: 0, load: 0,
    wounded: false, weary: false, miserable: false, supplies: 100,
  },
  skills: [
    'Fascínio', 'Atletismo', 'Vigilância', 'Caçada', 'Música', 'Ofício',
    'Indução', 'Viagem', 'Discernimento', 'Cura', 'Cortesia', 'Batalha',
    'Persuasão', 'Furtividade', 'Busca', 'Exploração', 'Enigma', 'História',
  ].map(name => ({ name, rank: 0, specialization: '', favorite: false })),
  combat: { parry: 0, armor: 'Nenhuma', helmet: 'Nenhum', shield: 'Nenhum', weapons: [] },
  resources: { coins: 0, treasure: 0, experience: 0, adventurePoints: 0, skillPoints: 0 },
  inventory: [],
  other: { virtues: '', rewards: '', flaws: '', notes: '', narrativeConditions: '' },
  tokenId: null,
  historyLog: [],
};

function newCampaign(): CampaignState {
  const now = Date.now();
  const campaign: CampaignState = {
    id: `campaign_${now}`,
    name: 'Nova Crônica de O Um Anel',
    systemProfileId: 'the-one-ring-2e',
    settingProfileId: 'middle-earth-third-age',
    rulesProfileName: 'O Um Anel 2e — Terra-média, Terceira Era',
    rulesEdition: '2e',
    rulesSourceFolderId: MANUALS_DRIVE_FOLDER_ID,
    characterSheet: structuredClone(DEFAULT_CHARACTER_SHEET),
    activeCharacterId: null, partyCharacterIds: [],
    currentLocation: 'Eriador',
    currentChapter: 'Prólogo',
    campaignSummary: 'Uma nova crônica aguarda seu primeiro passo.',
    recentEvents: '', clues: '', mysteries: '', quests: '', worldFlags: {}, presentNpcIds: [],
    history: [], pendingDiceRoll: null, pendingTest: null, events: [], firstImageGenerated: false,
    schemaVersion: 350, revision: 1, createdAt: now, updatedAt: now,
  };
  campaign.masterState = createDefaultMasterState(campaign);
  return campaign;
}

function normalizeCharacterSheet(input?: Partial<CharacterSheet> | null): CharacterSheet {
  const base = structuredClone(DEFAULT_CHARACTER_SHEET);
  if (!input) return base;
  const skills = Array.isArray(input.skills) && input.skills.length ? input.skills : base.skills;
  const merged: CharacterSheet = {
    ...base,
    ...input,
    identity: { ...base.identity, ...(input.identity || {}) },
    attributes: { ...base.attributes, ...(input.attributes || {}) },
    skills: skills.map(skill => ({ name: skill.name || 'Perícia', rank: Number(skill.rank) || 0, specialization: skill.specialization || '', favorite: Boolean(skill.favorite) })),
    combat: { ...base.combat, ...(input.combat || {}), weapons: Array.isArray(input.combat?.weapons) ? input.combat!.weapons : [] },
    resources: { ...base.resources, ...(input.resources || {}) },
    inventory: Array.isArray(input.inventory) ? input.inventory.map(item => ({
      ...item,
      id: item.id || `item_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      name: item.name || 'Item sem nome',
      quantity: Math.max(0, Number(item.quantity) || 0),
      weight: Math.max(0, Number(item.weight) || 0),
      load: Math.max(0, Number(item.load ?? item.weight) || 0),
      category: item.category || 'Geral',
      description: item.description || '',
      equipped: Boolean(item.equipped),
    })) : [],
    other: { ...base.other, ...(input.other || {}) },
    tokenId: input.tokenId || null,
    historyLog: Array.isArray(input.historyLog) ? input.historyLog : [],
  };
  return normalizeEquipmentMechanics(merged);
}

function normalizeCampaignState(input: CampaignState): CampaignState {
  const fallback = newCampaign();
  const history = Array.isArray(input.history) ? input.history.filter(node => node?.id && node.atmospheric_narration != null) : [];
  const pendingTest = input.pendingTest
    ? {
        ...input.pendingTest,
        skill: String(input.pendingTest.skill || 'Perícia apropriada'),
        reason: String(input.pendingTest.reason || 'Teste pendente'),
        originalChoice: String(input.pendingTest.originalChoice || input.pendingTest.reason || `Resolver teste de ${input.pendingTest.skill || 'perícia'}`).trim(),
        createdAt: Number(input.pendingTest.createdAt) || Date.now(),
      }
    : null;
  const normalized: CampaignState = {
    ...fallback,
    ...input,
    id: input.id || fallback.id,
    name: input.name || fallback.name,
    systemProfileId: input.systemProfileId || fallback.systemProfileId,
    settingProfileId: input.settingProfileId || fallback.settingProfileId,
    rulesProfileName: input.rulesProfileName || fallback.rulesProfileName,
    rulesEdition: input.rulesEdition || fallback.rulesEdition,
    rulesSourceFolderId: input.rulesSourceFolderId || fallback.rulesSourceFolderId,
    characterSheet: normalizeCharacterSheet(input.characterSheet),
    activeCharacterId: input.activeCharacterId || null,
    partyCharacterIds: Array.isArray(input.partyCharacterIds) ? Array.from(new Set(input.partyCharacterIds.filter(Boolean))) : [],
    currentLocation: input.currentLocation || history.at(-1)?.location || 'Eriador',
    currentChapter: input.currentChapter || 'Prólogo',
    campaignSummary: input.campaignSummary || fallback.campaignSummary,
    recentEvents: input.recentEvents || '',
    clues: input.clues || '',
    mysteries: input.mysteries || '',
    quests: input.quests || '',
    worldFlags: input.worldFlags || {},
    presentNpcIds: Array.isArray(input.presentNpcIds) ? input.presentNpcIds : [],
    history,
    pendingDiceRoll: input.pendingDiceRoll || null,
    pendingTest,
    events: Array.isArray(input.events) ? input.events : [],
    schemaVersion: Number(input.schemaVersion || 0),
    createdAt: input.createdAt || Date.now(),
    updatedAt: input.updatedAt || Date.now(),
  };
  normalized.masterState = normalizeMasterState(normalized);
  normalized.currentLocation = normalized.masterState.position.locationName || normalized.currentLocation;
  return normalized;
}

function makeEvent(type: CampaignEvent['type'], sceneId?: string, data?: CampaignEvent['data']): CampaignEvent {
  return { id: `event_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, type, timestamp: Date.now(), sceneId, data };
}

async function reconcileCampaignLocationWithAtlas(campaign: CampaignState, providedDiscovery?: MapDiscovery): Promise<CampaignState> {
  try {
    const [manifest, discovery, locations] = await Promise.all([
      loadRuntimeMapManifest(),
      providedDiscovery ? Promise.resolve(providedDiscovery) : loadMapDiscovery(campaign.id),
      loadRuntimeTerraMediaLocations(),
    ]);
    const currentHexId = discovery.currentHexId || campaign.masterState?.position.currentHexId || null;
    if (!manifest || !currentHexId) return campaign;
    const resolved = resolveHexNarrativeLocation(manifest, locations, currentHexId);
    if (!resolved.name || resolved.source === 'unresolved') return campaign;
    const state = normalizeMasterState(campaign);
    const alreadyCanonical = campaign.currentLocation === resolved.name
      && state.position.locationName === resolved.name
      && state.position.currentHexId === currentHexId;
    if (alreadyCanonical) return campaign;
    state.position = { ...state.position, currentHexId, locationName: resolved.name };
    state.scene = { ...state.scene, locationName: resolved.name };
    state.updatedAt = Date.now();
    return { ...campaign, currentLocation: resolved.name, masterState: state, updatedAt: Date.now() };
  } catch (error) {
    console.warn('[BRASEIRO_MAPA] não foi possível reconciliar o nome cartográfico atual:', error);
    return campaign;
  }
}

function normalizeText(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function friendlyTurnError(error: unknown): string {
  const raw = error instanceof Error ? error.message : String(error || '');
  if (/401|403|chave.*nao configurada|chave.*não configurada|unauthoriz|api key/i.test(raw)) {
    return 'O Gemini não está disponível: confira a chave em Configurações. A campanha não foi alterada.';
  }
  if (/429|quota|cota|resource_exhausted|limite temporario|limite temporário/i.test(raw)) {
    return 'A cota do Gemini está temporariamente esgotada. Tente novamente mais tarde; a campanha não foi alterada.';
  }
  if (/unable to resolve host|no address associated with hostname|unknownhost|software caused connection abort|connection abort|connection reset|sockettimeout|timed out|network is unreachable|failed to connect|temporary failure in name resolution|conexão temporariamente indisponível/i.test(raw)) {
    return 'A conexão com o Gemini caiu antes de concluir o turno. A cena anterior foi preservada sem alterações. Verifique a internet e tente a ação novamente.';
  }
  if (/json|unterminated|string in json|truncad|max.tokens/i.test(raw)) {
    return 'O Gemini devolveu uma resposta incompleta. O turno foi cancelado com segurança; tente novamente.';
  }
  if (/reading ['"]?normalize|\.normalize|properties of undefined/i.test(raw)) {
    return 'A cena recebeu um campo de texto incompleto. A cena anterior e o resultado da rolagem foram preservados; toque novamente em “Continuar com este resultado”.';
  }
  return raw || 'O Mestre não conseguiu concluir o turno. Nenhuma mudança foi aplicada.';
}

function explicitParagraphCount(value: string): number {
  const clean = String(value || '').replace(/\r/g, '').trim();
  if (!clean) return 0;
  const explicit = clean.split(/\n\s*\n+/).map(item => item.trim()).filter(Boolean);
  if (explicit.length > 1) return explicit.length;
  const sentences = clean.match(/[^.!?…]+[.!?…]+(?:[”"']+)?|[^.!?…]+$/g)?.filter(Boolean) || [];
  return Math.max(1, Math.ceil(sentences.length / 2));
}

function openingNarrationNeedsRepair(response: { atmospheric_narration?: string }, profileId: AdventureProfileId): boolean {
  const text = String(response.atmospheric_narration || '').trim();
  const words = text.split(/\s+/).filter(Boolean).length;
  const chars = text.length;
  const paragraphs = explicitParagraphCount(text);
  const minimums: Record<AdventureProfileId, { words: number; paragraphs: number; chars: number }> = {
    quick: { words: 950, paragraphs: 8, chars: 6200 },
    balanced: { words: 1000, paragraphs: 9, chars: 6500 },
    cinematic: { words: 1100, paragraphs: 10, chars: 7200 },
    investigation: { words: 1000, paragraphs: 9, chars: 6500 },
    exploration: { words: 1000, paragraphs: 9, chars: 6500 },
  };
  const target = minimums[profileId];
  return chars < target.chars || words < target.words || paragraphs < target.paragraphs;
}

function tailLines(value: string, maxLines: number, maxChars: number): string {
  const lines = value.split('\n').map(line => line.trim()).filter(Boolean).slice(-maxLines);
  const joined = lines.join('\n');
  return joined.length <= maxChars ? joined : joined.slice(-maxChars);
}

function sanitizeModelEvents(response: GeminiStoryResponse, base: CampaignState, sceneId: string): CampaignEvent[] {
  const text = normalizeText(`${response.atmospheric_narration || ''} ${response.action_result || ''}`);
  const locationChanged = normalizeText(response.location || '') !== normalizeText(base.currentLocation || '');
  const actualCombat = /(inicia|comeca|irrompe|entra em|rola iniciativa|trocam golpes|ataca|golpeia|dispara|combate se inicia|batalha comeca)/.test(text)
    && !/(iminente|pode haver|ameaca de|prestes a|evitar o combate|sem combate)/.test(text);
  const completedJourney = /(dia de jornada (termina|terminou|foi concluido)|jornada do dia (termina|terminou)|alcancam o destino|chegam ao destino|montam acampamento ao fim do dia)/.test(text);
  const completedRest = /(descanso (foi )?concluido|repouso completo|dormiram ate|uma noite de sono|recupera apos descansar)/.test(text);
  return (response.events || []).flatMap((event): CampaignEvent[] => {
    const normalized: CampaignEvent = { ...event, id: event.id || `event_${Date.now()}_${Math.random()}`, timestamp: event.timestamp || Date.now(), sceneId };
    if (['scene_started', 'test_requested', 'test_resolved'].includes(normalized.type)) return [];
    if (normalized.type === 'location_changed') return locationChanged ? [normalized] : [];
    if (normalized.type === 'combat_started') return actualCombat ? [normalized] : [];
    if (normalized.type === 'journey_day_completed') return completedJourney || Number(normalized.data?.days || 0) > 0 ? [normalized] : [];
    if (normalized.type === 'rest_completed') return completedRest ? [normalized] : [];
    if (normalized.type === 'npc_encountered') return normalized.data?.npcId || normalized.data?.name ? [normalized] : [];
    if (normalized.type === 'clue_discovered') return normalized.data?.clue || normalized.data?.description ? [normalized] : [];
    return [normalized];
  });
}

function updateCampaignMemory(base: CampaignState, node: SceneNode, events: CampaignEvent[], narrativeLog?: string): Pick<CampaignState, 'campaignSummary' | 'recentEvents' | 'clues' | 'presentNpcIds'> {
  const action = String(node.playerAction || '').replace(/^\[RESOLVER TESTE PENDENTE\]\s*/i, '').trim();
  const result = String(node.action_result || '').trim() || String(node.atmospheric_narration || '').split(/(?<=[.!?])\s+/)[0] || 'A cena avançou.';
  const entry = `• ${node.location}: ${action} → ${result}`.slice(0, 900);
  const recentEvents = tailLines(`${base.recentEvents || ''}\n${entry}`, 14, 8_000);
  const campaignSummary = tailLines(`${base.campaignSummary || ''}\n${narrativeLog?.trim() || entry}`, 22, 12_000);
  const clueLines = events.filter(event => event.type === 'clue_discovered').map(event => String(event.data?.clue || event.data?.description || '')).filter(Boolean);
  const clues = tailLines(`${base.clues || ''}\n${clueLines.map(value => `• ${value}`).join('\n')}`, 40, 12_000);
  const npcIds = events.filter(event => event.type === 'npc_encountered').map(event => String(event.data?.npcId || '')).filter(Boolean);
  return { campaignSummary, recentEvents, clues, presentNpcIds: Array.from(new Set([...(base.presentNpcIds || []), ...npcIds])) };
}

function isImportableSharedImage(value: unknown): value is string {
  const source = String(value || '');
  return source.startsWith('data:image/')
    || source.startsWith('blob:')
    || source.startsWith('https://appassets.androidplatform.net/shared-image/');
}

async function sharedImageToBlob(source: string): Promise<Blob> {
  if (!isImportableSharedImage(source)) throw new Error('A imagem compartilhada não é válida.');
  const response = await fetch(source);
  if (!response.ok) throw new Error('O Android não conseguiu ler a imagem compartilhada.');
  const blob = await response.blob();
  if (!blob.size || (blob.type && !blob.type.startsWith('image/'))) throw new Error('O conteúdo compartilhado não é uma imagem válida.');
  return blob;
}

async function compressSceneImage(source: string): Promise<string> {
  if (!isImportableSharedImage(source)) return source;
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => {
      try {
        const maxWidth = 1080;
        const scale = Math.min(1, maxWidth / Math.max(1, image.naturalWidth || image.width));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round((image.naturalWidth || image.width) * scale));
        canvas.height = Math.max(1, Math.round((image.naturalHeight || image.height) * scale));
        const context = canvas.getContext('2d');
        if (!context) throw new Error('O Android não conseguiu preparar a imagem recebida.');
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', 0.84));
      } catch (error) {
        reject(error);
      }
    };
    image.onerror = () => reject(new Error('A imagem compartilhada não pôde ser aberta pelo aplicativo.'));
    image.src = source;
  });
}

export default function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('jogo');
  const [quickAudioOpen, setQuickAudioOpen] = useState(false);
  const [campaign, setCampaign] = useState<CampaignState | null>(null);
  const campaignRef = useRef<CampaignState | null>(null);
  const previousCombatStatusRef = useRef<string>('idle');
  const [bootError, setBootError] = useState<string | null>(null);
  const [allNpcs, setAllNpcs] = useState<Npc[]>([]);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [presentNpcs, setPresentNpcs] = useState<Npc[]>([]);
  const [isStoryLoading, setIsStoryLoading] = useState(false);
  const [isImageLoading, setIsImageLoading] = useState(false);
  const [isNarrationLoading, setIsNarrationLoading] = useState(false);
  const [storyError, setStoryError] = useState<string | null>(null);
  const [imageError, setImageError] = useState<string | null>(null);
  const [narrationError, setNarrationError] = useState<string | null>(null);
  const [currentImage, setCurrentImage] = useState<string | null>(null);
  const [customAction, setCustomAction] = useState('');
  const [activeSources, setActiveSources] = useState<Array<{ name: string; pageNum: number; physicalPageNum?: number; pageLabel?: string }>>([]);
  const [ruleAnswer, setRuleAnswer] = useState<RuleAnswer | null>(null);
  const [atmosphereActive, setAtmosphereActive] = useState(true);
  const [audioActive, setAudioActive] = useState(false);
  const [imageQuality, setImageQuality] = useState<'fast' | 'quality'>(() => localStorage.getItem('braseiro.imageQuality') === 'quality' ? 'quality' : 'fast');
  const [adventureProfile, setAdventureProfile] = useState<AdventureProfileId>(() => {
    const stored = localStorage.getItem('braseiro.adventureProfile') as AdventureProfileId | null;
    return ['balanced', 'cinematic', 'quick', 'investigation', 'exploration'].includes(stored || '') ? stored! : 'balanced';
  });
  const [imageImportStatus, setImageImportStatus] = useState('');
  const [healthReport, setHealthReport] = useState<HealthReport | null>(null);
  const [isHealthChecking, setIsHealthChecking] = useState(false);
  const narrationRunRef = useRef(0);
  const sfxTimersRef = useRef<number[]>([]);
  const sharedImageImportingRef = useRef(false);
  const audioLibraryCacheRef = useRef<{ loadedAt: number; sounds: SoundItem[]; rules: SoundKeywordRule[] } | null>(null);

  useEffect(() => {
    void getBarbaraDeviceProfile().then(profile => {
      const root = document.documentElement;
      const render: any = (profile as any)?.render || {};
      root.dataset.barbaraQuality = String(render.quality_tier || 'balanced');
      root.style.setProperty('--barbara-render-scale', String(render.render_scale || 1));
      root.style.setProperty('--barbara-max-dpr', String(render.max_dpr || 2));
    });
  }, []);

  useEffect(() => { campaignRef.current = campaign; }, [campaign]);
  useEffect(() => {
    if (!campaign?.history?.length) return;
    void window.BraseiroAndroid?.prewarmTts?.();
  }, [campaign?.history?.length, campaign?.history?.at(-1)?.id]);
  useEffect(() => { localStorage.setItem('braseiro.adventureProfile', adventureProfile); }, [adventureProfile]);
  useEffect(() => {
    if (!campaign) return;
    const status = normalizeMasterState(campaign).combat.status;
    if (status !== previousCombatStatusRef.current && status !== 'idle' && status !== 'resolved') setActiveTab('combate');
    previousCombatStatusRef.current = status;
  }, [campaign?.masterState?.combat?.status]);

  const persistCampaign = async (value: CampaignState, reason = 'autosave') => {
    // Nunca apresenta nem promove uma alteração como salva antes do commit
    // canónico. O espelho continua apontando para a última revisão confirmada.
    const previous = campaignRef.current;
    if (previous) saveEmergencyCampaignMirror(previous);
    try {
      const persisted = await saveCanonicalTransaction({ campaign: value, reason });
      campaignRef.current = persisted;
      setCampaign(persisted);
      return persisted;
    } catch (error) {
      if (previous) {
        saveEmergencyCampaignMirror(previous);
        campaignRef.current = previous;
        setCampaign(previous);
      }
      throw error;
    }
  };

  const reloadNpcs = async (syncPresence = false) => {
    try {
      const stored = await getStoreData<Npc>('npcs');
      const npcs = await enrichNpcPopulation(stored);
      setAllNpcs(npcs);
      const visible = npcs.filter(item => item.presentInScene);
      setPresentNpcs(visible);
      if (syncPresence && campaignRef.current) {
        const base = campaignRef.current;
        const updated: CampaignState = { ...base, presentNpcIds: visible.map(item => item.id), updatedAt: Date.now() };
        await persistCampaign(updated, 'sincronizar presença de NPCs');
      }
    } catch (error) { console.error('[BRASEIRO_DB] NPCs:', error); }
  };

  const reloadCharacters = async (): Promise<Character[]> => {
    try {
      const raw = await getCharacters();
      const list = raw.map(character => ({ ...character, sheet: normalizeEquipmentMechanics(normalizeCharacterSheet(character.sheet)) }));
      setCharacters(list);
      return list;
    } catch (error) {
      console.error('[BRASEIRO_DB] personagens:', error);
      setCharacters([]);
      return [];
    }
  };

  const initialize = async () => {
    setBootError(null);
    markBootStage('initialize:start');
    const bootWatchdog = window.setTimeout(() => console.error(`[BRASEIRO_BOOT_STALL] ${String((window as any).__braseiroBootStage || 'unknown')}`), 10_000);
    try {
      let active = await getActiveCampaign();
      markBootStage('campaign:loaded');
      if (!active) {
        active = newCampaign();
        active = await saveCanonicalTransaction({ campaign: active, reason: 'criação da campanha' });
      }
      const migrationSourceAtBoot = { campaignSchema: Number(active.schemaVersion || 0), masterSchema: Number(active.masterState?.schemaVersion || 0) };
      const migrationRequiredAtBoot = needsMigration300(active);
      active = normalizeCampaignState(active);
      let list = await reloadCharacters();
      if (!list.length) {
        const migrated: Character = {
          id: `char_${Date.now()}_migrated`,
          name: active.characterSheet.identity.name || 'Novo Herói',
          culture: active.characterSheet.identity.culture || '',
          vocation: active.characterSheet.identity.vocation || '',
          sheet: normalizeCharacterSheet(active.characterSheet),
          createdAt: Date.now(), updatedAt: Date.now(),
        };
        list = [migrated];
        setCharacters(list);
      }
      const validCharacterIds = new Set(list.map(character => character.id));
      const selected = list.find(character => character.id === active.activeCharacterId) || list[0];
      const savedParty = (active.partyCharacterIds || []).filter(id => validCharacterIds.has(id));
      const partyCharacterIds = Array.from(new Set(savedParty.length ? savedParty : [selected.id]));
      if (!partyCharacterIds.includes(selected.id)) partyCharacterIds.unshift(selected.id);
      active = {
        ...active,
        activeCharacterId: selected.id,
        partyCharacterIds,
        characterSheet: normalizeCharacterSheet(selected.sheet),
      };
      const discovery = await loadMapDiscovery(active.id);
      markBootStage('map-discovery:loaded');
      const storedNpcs = await getStoreData<Npc>('npcs').catch(() => []);
      markBootStage('npcs:loaded');
      // Publica imediatamente o estado legível. Migração, atlas, snapshots,
      // reconciliação e auditorias são manutenção e jamais podem bloquear a UI.
      setCharacters(list);
      setAllNpcs(storedNpcs);
      setPresentNpcs(storedNpcs.filter(npc => active.presentNpcIds.includes(npc.id) || npc.presentInScene));
      setCampaign(active);
      campaignRef.current = active;
      const firstVisibleNode = active.history.at(-1);
      setCurrentImage(firstVisibleNode?.imageData || null);
      markBootStage('ui:published');
      await yieldForFirstPaint();
      markBootStage('ui:first-paint');
      window.clearTimeout(bootWatchdog);
      const bootCampaign = active;
      const bootList = list;
      scheduleStartupMaintenance(async () => {
        markBootStage('maintenance:start');
        const current = campaignRef.current;
        if (!current || current.id !== bootCampaign.id || current.lastTransactionId !== bootCampaign.lastTransactionId) return;
        let maintainedActive = bootCampaign;
        let maintainedList = bootList;
        let maintainedNpcs = storedNpcs;
        if (migrationRequiredAtBoot) {
          await createCampaignSnapshot(maintainedActive, 'antes da migração 3.0.0');
          const migrated = migrateCampaignTo300(maintainedActive, maintainedList, maintainedNpcs, discovery, migrationSourceAtBoot);
          maintainedActive = await reconcileCampaignLocationWithAtlas(migrated.campaign, migrated.discovery);
          maintainedList = migrated.characters;
          maintainedNpcs = migrated.npcs;
          maintainedActive = await saveMasterTransaction(maintainedActive, maintainedList, [], 'migração 3.0.0', maintainedNpcs, migrated.discovery);
          await saveStoreData('migration_reports', migrated.report, 'relatório de migração 3.0.0');
        } else {
          const reconciled = reconcileCanonicalState(maintainedActive, maintainedList, maintainedNpcs, discovery, { mapWins: true });
          maintainedActive = await reconcileCampaignLocationWithAtlas(reconciled.campaign, reconciled.discovery);
          maintainedList = reconciled.characters;
          maintainedNpcs = reconciled.npcs;
          maintainedActive = await saveMasterTransaction(maintainedActive, maintainedList, [], 'reconciliação de inicialização 3.0.0', maintainedNpcs, reconciled.discovery);
        }
        const catalog = await syncBundledNpcTokens(false).catch(() => []);
        const libraryAssigned = autoAssignNpcTokens(maintainedNpcs, catalog);
        const assignedNpcs = await enrichNpcPopulation(libraryAssigned);
        const changedNpcs = assignedNpcs.filter((npc, index) => JSON.stringify(npc) !== JSON.stringify(maintainedNpcs[index]));
        if (changedNpcs.length) {
          maintainedActive = await saveCanonicalTransaction({ campaign: maintainedActive, characters: maintainedList, npcs: assignedNpcs, reason: 'atribuição automática de personalidade e retrato de NPC' });
        }
        markBootStage('supervisor:start');
        const supervised = await runStartupSupervisor(maintainedActive, maintainedList, assignedNpcs, true, false);
        markBootStage('supervisor:done');
        maintainedActive = supervised.campaign;
        maintainedList = supervised.characters;
        const latest = campaignRef.current;
        if (!latest || latest.id !== maintainedActive.id || Number(latest.revision || 0) > Number(maintainedActive.revision || 0)) return;
        setHealthReport(supervised.report);
        setCharacters(maintainedList);
        setAllNpcs(supervised.npcs);
        setPresentNpcs(supervised.npcs.filter(npc => maintainedActive.presentNpcIds.includes(npc.id) || npc.presentInScene));
        setCampaign(maintainedActive);
        campaignRef.current = maintainedActive;
        const last = maintainedActive.history.at(-1);
        setCurrentImage(last?.imageData || null);
        await ensureSoundKeywordLibrary().catch(error => console.warn('[BRASEIRO_AUDIO] biblioteca de palavras-chave:', error));
        markBootStage('maintenance:done');
      });
    } catch (error) {
      window.clearTimeout(bootWatchdog);
      markBootStage('initialize:error');
      console.error('[BRASEIRO_INIT]', error);
      setBootError(error instanceof Error ? error.message : 'Falha ao abrir os dados locais.');
    }
  };

  useEffect(() => { void initialize(); }, []);

  const runHealthCheck = async (repair: boolean): Promise<void> => {
    const active = campaignRef.current;
    if (!active || isHealthChecking) return;
    setIsHealthChecking(true);
    try {
      const freshCharacters = await getCharacters().catch(() => characters);
      const freshNpcs = await getStoreData<Npc>('npcs').catch(() => allNpcs);
      const supervised = await runStartupSupervisor(active, freshCharacters, freshNpcs, repair, true);
      setHealthReport(supervised.report);
      if (repair) {
        campaignRef.current = supervised.campaign;
        setCampaign(supervised.campaign);
        setCharacters(supervised.characters);
        setAllNpcs(supervised.npcs);
        setPresentNpcs(supervised.npcs.filter(npc => supervised.campaign.presentNpcIds.includes(npc.id) || npc.presentInScene));
      }
    } catch (error) {
      console.error('[BRASEIRO_HEALTH]', error);
      setStoryError(error instanceof Error ? error.message : 'A verificação de saúde não pôde ser concluída.');
    } finally {
      setIsHealthChecking(false);
    }
  };

  const clearSfxTimers = () => {
    sfxTimersRef.current.forEach(timer => window.clearTimeout(timer));
    sfxTimersRef.current = [];
  };

  const cancelNarrationPlayback = () => {
    narrationRunRef.current += 1;
    clearSfxTimers();
    stopAndroidSpeech();
    AudioEngine.stopChannel('voice');
    AudioEngine.stopChannel('sfx');
    setIsNarrationLoading(false);
  };

  const getAudioLibrary = async (force = false): Promise<{ sounds: SoundItem[]; rules: SoundKeywordRule[] }> => {
    const cached = audioLibraryCacheRef.current;
    if (!force && cached && Date.now() - cached.loadedAt < 5 * 60_000) return cached;
    await ensureSoundKeywordLibrary().catch(error => console.warn('[BRASEIRO_AUDIO] biblioteca de palavras-chave:', error));
    const [native, local, rules] = await Promise.all([
      listNativeSounds().catch(() => []),
      listSoundMetadata().catch(() => []),
      listSoundKeywordRules().catch(() => []),
    ]);
    const unique = new Map<string, SoundItem>();
    [...native, ...local].forEach(sound => unique.set(sound.id, sound));
    const result = {
      loadedAt: Date.now(),
      rules,
      sounds: Array.from(unique.values()).map(sound => applyRulesToSound(sound, rules)),
    };
    audioLibraryCacheRef.current = result;
    return result;
  };

  const getAllSounds = async (): Promise<SoundItem[]> => (await getAudioLibrary()).sounds;

  // Carrega o índice sonoro em segundo plano assim que a campanha abre. O botão
  // Ouvir não precisa esperar a leitura inicial de centenas de arquivos.
  useEffect(() => {
    if (!campaign?.id) return;
    AudioEngine.init();
    void getAudioLibrary().catch(error => console.warn('[BRASEIRO_AUDIO] aquecimento:', error));
  }, [campaign?.id]);

  const requestTest = async (test: PendingTest, turn?: { id: string; idempotencyKey: string; inputChannel: 'PLAYER_ACTION' | 'GM_HELP' }) => {
    const base = campaignRef.current;
    if (!base) return;
    const requestedCharacter = test.characterId ? characters.find(character => character.id === test.characterId) : undefined;
    const updated: CampaignState = {
      ...base,
      activeCharacterId: requestedCharacter?.id || base.activeCharacterId,
      characterSheet: requestedCharacter ? structuredClone(requestedCharacter.sheet) : base.characterSheet,
      pendingTest: test,
      pendingDiceRoll: null,
      events: [...(base.events || []), makeEvent('test_requested', undefined, { skill: test.skill, reason: test.reason, characterId: test.characterId || base.activeCharacterId || '' })],
      updatedAt: Date.now(),
    };
    const event = updated.events?.at(-1);
    const persisted = turn
      ? await TurnCoordinator.commitCanonical({ turnId: turn.id, idempotencyKey: turn.idempotencyKey, campaign: updated, sceneId: `pending-test-${test.id}`, events: event ? [event] : [], inputChannel: turn.inputChannel, payload: { inputChannel: turn.inputChannel, skill: test.skill, status: 'pending-test' }, reason: 'teste solicitado e turno suspenso' })
      : await persistCampaign(updated, 'teste solicitado');
    campaignRef.current = persisted;
    setCampaign(persisted);
    setActiveTab('jogo');
    window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 80);
  };

  const handleChoice = async (choice: StoryChoice) => {
    const active = campaignRef.current;
    if (isStoryLoading || !active) return;
    if (active.pendingTest) {
      setActiveTab('jogo');
      window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 40);
      return;
    }
    // A sugestão é apenas uma ação livre pré-escrita. O Diretor decide depois,
    // no contexto completo, se a ação realmente exige uma rolagem.
    cancelNarrationPlayback();
    setIsStoryLoading(true); setStoryError(null); setNarrationError(null);
    await processTurn(choice.text, false);
  };

  const handleCustomActionSubmit = async (event: React.FormEvent, isDmMode: boolean) => {
    event.preventDefault();
    const action = customAction.trim();
    const active = campaignRef.current;
    if (!action || isStoryLoading || !active) return;
    if (active.pendingTest && !isDmMode) {
      setActiveTab('jogo');
      window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 40);
      return;
    }
    cancelNarrationPlayback();
    setCustomAction('');
    setIsStoryLoading(true); setStoryError(null); setNarrationError(null);
    await processTurn(isDmMode ? `[DÚVIDA PARA O MESTRE]: ${action}` : action, isDmMode);
  };

  const processTurn = async (userAction: string, isDmMode: boolean, sourceCampaign?: CampaignState) => {
    let base = sourceCampaign || campaignRef.current;
    if (!base) return;
    userAction = String(userAction || '').trim();
    if (!userAction) {
      setStoryError('A ação estava vazia. A campanha e qualquer resultado pendente continuam preservados.');
      setIsStoryLoading(false);
      return;
    }
    base = await reconcileCampaignLocationWithAtlas(base);
    if (campaignRef.current?.id === base.id && campaignRef.current !== base) {
      campaignRef.current = base;
      setCampaign(base);
    }
    // O espelho é gravado antes de qualquer chamada externa. Toda preparação da
    // próxima cena ocorre em memória e só substitui esta revisão após o commit.
    saveEmergencyCampaignMirror(base);
    const inputChannel: 'PLAYER_ACTION' | 'GM_HELP' = isDmMode ? 'GM_HELP' : 'PLAYER_ACTION';
    const cleanUserAction = userAction.replace(/^\[DÚVIDA PARA O MESTRE\]:\s*/i, '');
    const narrativeIntent = classifyNarrativeIntent(cleanUserAction, inputChannel);
    const rulesOnlyTurn = inputChannel === 'GM_HELP';
    let activeTurnId: string | undefined;
    let activeTurnKey: string | undefined;
    try {
      if (!rulesOnlyTurn) {
        const begun = await TurnCoordinator.begin(base, userAction, inputChannel);
        if (begun.duplicateCommitted) {
          setIsStoryLoading(false);
          return;
        }
        if (begun.duplicateInFlight) {
          setStoryError('Este mesmo turno já está sendo processado. O segundo toque foi ignorado para não duplicar consequências.');
          setIsStoryLoading(false);
          return;
        }
        activeTurnId = begun.transaction.id;
        activeTurnKey = begun.transaction.idempotencyKey;
      }
      const barbaraMapDiscovery = await loadMapDiscovery(base.id).catch(() => null);
      const previousFiction = base.history.filter(node => node.narrativeMeta?.advancesFiction !== false);
      const currentBarbaraLocation = base.masterState?.position.locationName || base.currentLocation || '';
      const isHostOpening = /^\[INICIAR AVENTURA GUIADA\]/i.test(userAction) || previousFiction.length === 0;
      const hasVisitedBarbaraLocation = previousFiction.some(node => String(node.location || '').trim().toLocaleLowerCase('pt-BR') === String(currentBarbaraLocation).trim().toLocaleLowerCase('pt-BR'));
      const hostStoryOccasion: 'campaign_opening' | 'first_arrival' | 'continuation' = inputChannel === 'GM_HELP'
        ? 'continuation'
        : isHostOpening
          ? 'campaign_opening'
          : (!hasVisitedBarbaraLocation && currentBarbaraLocation ? 'first_arrival' : 'continuation');
      const currentPacing = base.masterState?.sessionDirector?.pacing || 'quiet';
      const barbaraImportance: 'routine' | 'normal' | 'meaningful' | 'climax' = isDmMode
        ? 'routine'
        : currentPacing === 'climax'
          ? 'climax'
          : hostStoryOccasion !== 'continuation' || currentPacing === 'tense' || currentPacing === 'rising'
            ? 'meaningful'
            : 'normal';
      const projectedStateValidation = await validateBarbaraState(base, characters, allNpcs, barbaraMapDiscovery);
      const barbaraProjectedState = projectCampaignForBarbara(base, characters, allNpcs, barbaraMapDiscovery, hostStoryOccasion);
      const barbaraInspection = await inspectBarbaraTurn(barbaraProjectedState, cleanUserAction || userAction, barbaraImportance, inputChannel);
      if (projectedStateValidation.valid === false) {
        throw new Error(`Barbara recusou a projeção canônica antes do turno: ${projectedStateValidation.reason || projectedStateValidation.error || 'estado incompatível'}`);
      }
      const barbaraPlan = await getBarbaraPlan({
        action: cleanUserAction,
        inputChannel,
        location: base.masterState?.position.locationName || base.currentLocation,
        imprint: base.masterState?.position.currentHexId || '',
        narrative: inputChannel === 'PLAYER_ACTION',
        nonAdvancing: inputChannel === 'GM_HELP',
        importance: barbaraImportance,
        storyOccasion: hostStoryOccasion,
      });
      const barbaraOccasion = String(barbaraPlan.story_occasion || 'continuation');
      const orchestrator = await buildOrchestratorContext(base, userAction);
      const { selectedManuals, environment, rulesConsultation } = orchestrator;
      const continuityAnchor = buildSceneContinuityAnchor(base, userAction, allNpcs);
      const continuityContext = continuityPrompt(continuityAnchor);
      const narrativeCheckpoint = buildNarrativeCheckpoint(base, allNpcs);
      const narrativeContext = narrativeCheckpointPrompt(narrativeCheckpoint, narrativeIntent, userAction);
      const isOpeningScene = /^\[INICIAR AVENTURA GUIADA\]/i.test(userAction);
      const initialRhythm = buildNarrativeRhythmPlan(base, narrativeIntent, { isOpening: isOpeningScene, isArrival: !isOpeningScene && (hostStoryOccasion === 'first_arrival' || barbaraOccasion === 'first_arrival' || barbaraOccasion === 'changed_return') });
      const rhythmContext = narrativeRhythmPrompt(initialRhythm);
      let directorContext = `${orchestrator.directorContext}${continuityContext}${narrativeContext}${rhythmContext}`;
      let publicContext = `${orchestrator.publicContext}${continuityContext}${narrativeContext}`;
      if (barbaraPlan.available) {
        const policy = barbaraPlan.narrative_policy || {};
        const supervisorAuthority = barbaraInspection?.authority || {};
        const supervisorWorldIntro = barbaraInspection?.world_introduction || {};
        const supervisorTurnPlan = barbaraInspection?.turn_plan || {};
        const commonContract = `
--- SUPERVISÃO BARBARA 1.1.3 (CONTRATO; HOST CONTINUA AUTORIDADE) ---
Canal vinculante: ${inputChannel}.
Ocasião narrativa: ${barbaraOccasion}.
Política de agência: ${String(policy.agency || policy.player_agency || 'preserve a agência do jogador')}.
Contrato de descoberta: ${JSON.stringify(barbaraPlan.world_discovery_contract || {})}.
Autoridade por domínio: ${JSON.stringify(supervisorAuthority)}.
Obrigação de apresentação do mundo: ${JSON.stringify(supervisorWorldIntro)}.
Plano supervisor do turno: ${JSON.stringify(supervisorTurnPlan)}.
Perfil narrativo humano anexado: OBRIGATÓRIO no Narrador. Para cenas imersivas marcadas pela cadência do host, o mínimo de densidade é vinculante e pode ser superior ao target_chars padrão do Barbara; isso aprofunda apresentação sem transferir autoridade.
Regras, estado canônico, Atlas, RAG, viagem, tempo, mundo, inventário e combate pertencem ao host.
A narrativa é somente proposta: nunca devolva state_patch, effects, events, commands ou canonical_state.
`;
        directorContext += commonContract;
        publicContext += commonContract;
      }
      setActiveSources(selectedManuals.map(source => ({ name: source.manualName, pageNum: source.pageNum, physicalPageNum: source.physicalPageNum, pageLabel: source.pageLabel })));
      if (rulesConsultation.readyManualCount === 0) {
        console.warn('[BRASEIRO_RAG] nenhum manual indexado; gameplay continua com regras determinísticas locais.');
      }
      if (selectedManuals.length === 0) {
        console.info('[BRASEIRO_RAG] nenhum trecho aplicável; RAG opcional, regras locais permanecem ativas.');
      }

      if (rulesOnlyTurn) {
        const answer = await answerRulesQuestion(publicContext, userAction.replace(/^\[DÚVIDA PARA O MESTRE\]:\s*/i, ''));
        setRuleAnswer(answer);
        setIsStoryLoading(false);
        return;
      }

      setRuleAnswer(null);
      const declaredHero = characters.find(character => character.id === base.activeCharacterId);
      const profile = getAdventureProfile(adventureProfile);
      const actionForDirectorBase = declaredHero
        ? `[HERÓI ATIVO CONFIRMADO: ${declaredHero.id} — ${declaredHero.sheet.identity.name || declaredHero.name}] ${userAction}`
        : userAction;
      const actionForDirector = `[PERFIL DA AVENTURA: ${profile.label}. ${profile.prompt}] ${actionForDirectorBase}`;
      let localFallbackActive = false;
      let plan: Awaited<ReturnType<typeof generateDirectorPlan>>;
      try {
        plan = await generateDirectorPlan(directorContext, actionForDirector);
      } catch (error) {
        const detail = error instanceof Error ? error.message : String(error);
        throw new Error('O Gemini não respondeu. A campanha foi preservada e nenhum fallback offline foi usado. ' + detail);
      }
      const groundedPlan = groundDirectorPlanInManuals(plan, selectedManuals as any);
      plan = enforceDirectorContinuity(groundedPlan.plan, continuityAnchor);
      plan = enforceNarrativeIntent(plan, narrativeIntent);
      if (groundedPlan.warning) console.warn('[BRASEIRO_RULE_EVIDENCE]', groundedPlan.warning);
      const rollAlreadyAvailable = Boolean(base.pendingDiceRoll && base.pendingTest);
      if (plan.requiredTest && !rollAlreadyAvailable && shouldSuppressRoutineTest(userAction, plan.requiredTest)) {
        console.info('[BRASEIRO_RULES] teste rotineiro suprimido: a ação não possui incerteza ou consequência relevante.', plan.requiredTest);
        plan.requiredTest = null;
      }
      const partyIds = base.partyCharacterIds || [];
      const requestedActorId = [plan.requiredTest?.characterId, plan.actingCharacterId, base.activeCharacterId, partyIds[0]]
        .find((id): id is string => Boolean(id) && partyIds.includes(String(id)));
      const requestedActor = characters.find(character => character.id === requestedActorId);
      if (isOpeningScene) plan.requiredTest = null;
      const deterministicTest = plan.requiredTest && requestedActor
        ? createSkillTest(requestedActor.sheet, plan.requiredTest.skill, plan.requiredTest.reason, requestedActor.id)
        : null;
      if (plan.requiredTest && !deterministicTest && !rollAlreadyAvailable) throw new Error('O Diretor pediu um teste sem identificar um Herói-jogador válido da Companhia.');
      const requestedTest = plan.requiredTest && deterministicTest && !rollAlreadyAvailable
        ? {
            ...deterministicTest,
            rollMode: plan.requiredTest.rollMode && plan.requiredTest.rollMode !== 'normal' ? plan.requiredTest.rollMode : deterministicTest.rollMode,
            consequences: plan.requiredTest.consequences,
            originalChoice: userAction,
            createdAt: Date.now(),
          }
        : null;
      if (requestedTest) {
        setIsStoryLoading(false);
        await requestTest(requestedTest, activeTurnId && activeTurnKey ? { id: activeTurnId, idempotencyKey: activeTurnKey, inputChannel } : undefined);
        return;
      }

      // Movimento do mapa nunca é autorizado por texto do Gemini. A máquina local
      // também controla toda transição de fase: fechar → mudar modo → abrir/atualizar.
      const currentMaster = normalizeMasterState(base);
      const formalNormalization = normalizeUnstartedFormalPhase(plan, currentMaster.activeMode);
      plan = formalNormalization.plan;
      if (formalNormalization.warning) console.warn(`[BRASEIRO_MASTER] ${formalNormalization.warning}`);
      const proposedKind = plan.sceneKind || (plan.mode === 'adventure' ? 'exploration' : plan.mode);
      const desiredKind = plan.mode === 'adventure'
        ? (['exploration', 'dialogue', 'rest'].includes(proposedKind) ? proposedKind : 'exploration')
        : plan.mode;
      const modelCommands = [...(plan.commands || [])].filter((command: MasterCommand) => command.type !== 'move_party');
      const transitionNormalized = normalizeSceneTransitionCommands(currentMaster, plan.mode, desiredKind, modelCommands);
      const terminalPhase = new Set(['complete_journey', 'resolve_council', 'end_combat', 'end_fellowship_phase']);
      const startingPhase = new Set(['start_journey', 'start_council', 'start_combat', 'start_fellowship_phase']);
      const commandPriority = (command: MasterCommand) => command.type === 'upsert_npc' ? 0
        : command.type === 'update_npc_actor' ? 1
        : terminalPhase.has(command.type) ? 2
        : command.type === 'close_scene' ? 3
        : command.type === 'set_mode' ? 4
        : command.type === 'set_present_npcs' ? 5
        : startingPhase.has(command.type) ? 6
        : command.type === 'open_scene' ? 7
        : command.type === 'set_spotlight' ? 8 : 9;
      const sanitizedPlan = sanitizeDirectorCommands(transitionNormalized, base, allNpcs, requestedActorId || base.activeCharacterId || partyIds[0]);
      if (sanitizedPlan.warnings.length) console.warn('[BRASEIRO_MASTER] plano reconciliado:', sanitizedPlan.warnings);
      const plannedCommands = sanitizedPlan.commands.sort((a, b) => commandPriority(a) - commandPriority(b));
      const actionHeroId = requestedActorId || base.activeCharacterId || partyIds[0];
      const openScene = plannedCommands.find((command): command is Extract<MasterCommand, { type: 'open_scene' }> => command.type === 'open_scene');
      if (openScene) {
        openScene.participantHeroIds = openScene.participantHeroIds?.length ? openScene.participantHeroIds : partyIds;
        openScene.objective = openScene.objective || plan.intent || 'Resolver a situação atual';
      }
      if (narrativeIntent.advancesFiction && actionHeroId && !plannedCommands.some(command => command.type === 'set_spotlight')) {
        plannedCommands.push({ type: 'set_spotlight', targetCharacterId: actionHeroId, reason: 'Herói que declarou a ação atual.' });
      }
      const safeCommands = plannedCommands;
      const sceneId = `scene_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
      const mechanical = applyMasterCommands(base, characters, allNpcs, safeCommands);
      const fatalRejections = mechanical.rejected.filter(item => !isAdvisoryMasterRejection(item.command));
      if (fatalRejections.length) {
        throw new Error(`Turno rejeitado sem alterar a campanha: ${fatalRejections.map(item => item.reason).join(' | ')}`);
      }
      if (mechanical.rejected.length) {
        console.warn('[BRASEIRO_MASTER] comandos narrativos ignorados sem cancelar a ação:', mechanical.rejected);
      }
      if (activeTurnId) {
        await TurnCoordinator.mechanicsApplied(activeTurnId, {
          inputChannel,
          appliedCommands: mechanical.audits.filter(item => item.status === 'applied').map(item => item.command.type),
          rejectedCommands: mechanical.rejected.length,
          localFallback: localFallbackActive,
        });
      }
      const npcCatalog = narrativeIntent.advancesFiction ? await getNpcTokenCatalog().catch(() => []) : [];
      let mechanicallyUpdatedNpcs = narrativeIntent.advancesFiction
        ? await enrichNpcPopulation(autoAssignNpcTokens(mechanical.npcs, npcCatalog))
        : allNpcs;
      let mechanicallyUpdated = mechanical.campaign;
      if (mechanicallyUpdated.masterState && narrativeIntent.advancesFiction) {
        mechanicallyUpdated.masterState = rememberDirectorPlan(mechanicallyUpdated.masterState, plan, sceneId);
        if (actionHeroId) mechanicallyUpdated.masterState = recordHeroSpotlight(mechanicallyUpdated.masterState, actionHeroId, partyIds);
      }
      const mechanicallyUpdatedCharacters = mechanical.characters;
      const activeAfterMechanics = mechanicallyUpdatedCharacters.find(character => character.id === mechanicallyUpdated.activeCharacterId);
      if (activeAfterMechanics) mechanicallyUpdated.characterSheet = structuredClone(activeAfterMechanics.sheet);

      const finalClock = mechanicallyUpdated.masterState?.worldClock || normalizeMasterState(mechanicallyUpdated).worldClock;
      // Releia o ambiente usando o relógio e a posição já validados deste turno.
      // A função apenas reconcilia tempo/posição: clima não avança por narração.
      // Assim cabeçalho, prosa, imagem e áudio partem do MESMO estado canônico.
      const sceneEnvironment = await buildAutonomousEnvironmentContext(mechanicallyUpdated).catch(() => environment);
      const environmentBinding = buildSceneEnvironmentBinding(sceneEnvironment, finalClock.hour, finalClock.minute);
      const sensoryContext = environmentBindingPrompt(environmentBinding);
      const finalSceneKind = plan.sceneKind || mechanicallyUpdated.masterState?.scene?.kind || initialRhythm.sceneKind;
      const turnRhythm = buildNarrativeRhythmPlan(mechanicallyUpdated, narrativeIntent, { isOpening: isOpeningScene, isArrival: !isOpeningScene && (hostStoryOccasion === 'first_arrival' || barbaraOccasion === 'first_arrival' || barbaraOccasion === 'changed_return'), sceneKind: finalSceneKind });
      const finalRhythmContext = narrativeRhythmPrompt(turnRhythm);
      const validatedPublicContext = `${publicContext}${sensoryContext}${finalRhythmContext}\n\n--- RESULTADO MECÂNICO VALIDADO LOCALMENTE ---\nModo: ${mechanicallyUpdated.masterState?.activeMode || 'adventure'}\nLocal canônico: ${mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation}\nCena formal: ${JSON.stringify(mechanicallyUpdated.masterState?.scene || null)}\nPróximo herói sugerido para protagonismo: ${mechanicallyUpdated.masterState?.sessionDirector.nextSuggestedHeroId || 'nenhum'}\nComandos aplicados: ${JSON.stringify(mechanical.audits.filter(item => item.status === 'applied').map(item => item.command))}\nCalendário canônico: ano ${finalClock.year}; dia ${finalClock.day}; estação ${finalClock.season}; horário ${String(finalClock.hour).padStart(2,'0')}:${String(finalClock.minute).padStart(2,'0')}. REGRA DURA: nunca mencione outra estação, outro dia ou outro período do dia.\nComandos rejeitados: ${JSON.stringify(mechanical.rejected)}\nNPCs presentes validados: ${JSON.stringify(mechanicallyUpdatedNpcs.filter(npc => mechanicallyUpdated.presentNpcIds.includes(npc.id)).map(npc => ({ id: npc.id, name: npc.name, role: npc.role, appearance: npc.appearance, attitude: npc.attitude, emotionalState: npc.emotionalState, speechStyle: npc.speechStyle, tokenAssigned: Boolean(npc.tokenId) })))}\nRolagem disponível: ${rollAlreadyAvailable ? JSON.stringify(base.pendingDiceRoll) : 'não'}\nNarre apenas o estado validado acima.`;
      let response: GeminiStoryResponse;
      if (localFallbackActive) {
        response = await generateNarrationFromPlan(validatedPublicContext, userAction, plan);
      } else {
        response = await generateNarrationFromPlan(validatedPublicContext, userAction, plan);
        if (narrationNeedsContinuityRepair(response, continuityAnchor)) {
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO DE CONTINUIDADE — NÃO MUDE A CENA]`, {
            ...plan,
            narrationBrief: `${plan.narrationBrief || ''}
A resposta anterior perdeu a continuidade. Comece pela reação imediata dos NPCs já presentes à ação do jogador, preserve o assunto e só depois avance a situação.`,
          });
        }
        if (isOpeningScene && openingNarrationNeedsRepair(response, adventureProfile)) {
          const repairedPlan = {
            ...plan,
            requiredTest: null,
            narrationBrief: `${plan.narrationBrief || ''}
REESCREVA A ABERTURA: ela veio curta demais. atmospheric_narration deve ter no mínimo 6.500 caracteres (alvo 6.500–9.000; no perfil cinematográfico, prefira 7.200+), em 9–13 parágrafos substanciais. Aprofunde imagem espacial, mundo já em movimento, luz, som, ar/temperatura, cheiro/textura quando pertinentes, identidade cultural, detalhe memorável, situação concreta e reação do mundo. Não solicite teste, não despeje lore não percebido e não resolva a decisão do jogador.`,
          };
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO DE ABERTURA CURTA — NÃO AVANCE O ESTADO]`, repairedPlan);
        }
        let quality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);
        for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2; qualityAttempt += 1) {
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO DE QUALIDADE — PRESERVE O ESTADO — RESPONDA DIRETAMENTE À AÇÃO ATUAL]`, {
            ...plan,
            narrationBrief: `${plan.narrationBrief || ''}
${professionalRepairBrief(quality.flags, narrativeIntent)}`,
          });
          quality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);
        }
        let rhythm = assessNarrativeRhythm(response, turnRhythm);
        for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2; rhythmAttempt += 1) {
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO DE CADÊNCIA — NÃO AVANCE ALÉM DO JÁ VALIDADO]`, {
            ...plan,
            narrationBrief: `${plan.narrationBrief || ''}
${rhythmRepairBrief(turnRhythm, rhythm.flags)}`,
          });
          rhythm = assessNarrativeRhythm(response, turnRhythm);
        }
      }
      if (narrativeIntent.advancesFiction) {
        let barbaraNarrativeValidation = await validateBarbaraNarration({
          response: response as unknown as Record<string, unknown>, action: cleanUserAction, inputChannel, occasion: barbaraOccasion, importance: barbaraImportance,
        });
        if (barbaraNarrativeValidation.valid === false && !localFallbackActive) {
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}\n[REPARO BARBARA — PRESERVE EXATAMENTE O ESTADO JÁ VALIDADO]`, {
            ...plan,
            narrationBrief: `${plan.narrationBrief || ''}\nA proposta anterior violou agência/cobertura/obrigação narrativa. Reescreva sem decidir ações pelo jogador, cubra integralmente a ação declarada e preserve posição, tempo, NPCs, fatos e consequências já validados.`,
          });
          barbaraNarrativeValidation = await validateBarbaraNarration({
            response: response as unknown as Record<string, unknown>, action: cleanUserAction, inputChannel, occasion: barbaraOccasion, importance: barbaraImportance,
          });
        }
        if (barbaraNarrativeValidation.valid === false) {
          throw new Error(`Barbara rejeitou a proposta narrativa sem alterar a campanha: ${barbaraNarrativeValidation.code || barbaraNarrativeValidation.reason || 'violação de agência/cobertura'}`);
        }
        const supervisorFinal = await finalizeBarbaraNarrative({
          hostState: barbaraProjectedState, text: cleanUserAction, requestId: activeTurnId || `oua-${Date.now()}`,
          narration: String((response as any).atmospheric_narration || (response as any).action_result || ''),
          importance: barbaraImportance, inputChannel, presentation: (response as any).presentation || null,
        });
        if (supervisorFinal.valid === false) {
          throw new Error(`Motor Barbara 1.1.3 Supervisor rejeitou a narrativa: ${supervisorFinal.reason || 'falha estrutural do supervisor'}`);
        }
        if (supervisorFinal.needs_repair && !localFallbackActive) {
          const supervisorReason = String(supervisorFinal.narrative_fallback_reason || 'qualidade/continuidade');
          response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}\n[REPARO SUPERVISOR BARBARA 1.1.3 — ${supervisorReason} — NÃO MUDE O ESTADO VALIDADO]`, {
            ...plan,
            narrationBrief: `${plan.narrationBrief || ''}\nAtenda a obrigação de apresentação/continuidade indicada pelo Supervisor 1.1.3 sem inventar lore, sem alterar regras e sem decidir pelo jogador.`,
          });
          const repairedSupervisor = await finalizeBarbaraNarrative({
            hostState: barbaraProjectedState, text: cleanUserAction, requestId: `${activeTurnId || `oua-${Date.now()}`}-repair`,
            narration: String((response as any).atmospheric_narration || (response as any).action_result || ''),
            importance: barbaraImportance, inputChannel, presentation: (response as any).presentation || null,
          });
          if (repairedSupervisor.valid === false) {
            throw new Error(`Motor Barbara 1.1.3 Supervisor falhou após reparo: ${repairedSupervisor.reason || 'falha estrutural'}`);
          }
          if (repairedSupervisor.needs_repair) console.warn('[BARBARA 1.1.3] reparo ainda recomendado:', repairedSupervisor.narrative_fallback_reason);
        }
      }
      if (!narrativeIntent.advancesFiction) {
        response = {
          ...response,
          atmospheric_narration: '',
          choices: [],
          voiceScript: [],
          events: [],
          imageRecommended: false,
          audioPlan: { environment: [], ambience: [], ambienceDetails: [], musicMood: [], sfx: [], stopPreviousAmbience: false },
        };
      }
      if (isOpeningScene && !localFallbackActive && openingNarrationNeedsRepair(response, adventureProfile)) {
        response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO FINAL DE ABERTURA — TEXTO AINDA CURTO — NÃO MUDE O ESTADO]`, {
          ...plan,
          requiredTest: null,
          narrationBrief: `${plan.narrationBrief || ''}
A abertura ainda está curta para o perfil narrativo obrigatório. Entregue NO MÍNIMO 6.500 caracteres em atmospheric_narration, preferencialmente 6.500–9.000, com 9–13 parágrafos substanciais: âncora temporal/cultural, orientação espacial do panorama ao limiar e à Companhia, mundo já em atividade, nome canônico da região/local, luz/visibilidade, sons, ar/temperatura/cheiro/textura quando pertinentes, cultura observável e detalhes concretos suficientes para o jogador imaginar onde está. Não faça lista sensorial e não avance além da primeira decisão real.`,
        });
      }
      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);

      let finalRhythmAssessment = assessNarrativeRhythm(response, turnRhythm);
      if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive) {
        response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO FINAL OBRIGATÓRIO DE DENSIDADE — NÃO MUDE O ESTADO NEM AS DECISÕES DO JOGADOR]`, {
          ...plan,
          narrationBrief: `${plan.narrationBrief || ''}
${rhythmRepairBrief(turnRhythm, finalRhythmAssessment.flags)}
O perfil técnico do Mestre é obrigatório. atmospheric_narration deve ultrapassar ${turnRhythm.targetCharMin} caracteres sem inventar acontecimentos futuros; aprofunde a MESMA batida por espaço, percepção sensorial, movimento, cultura observável, comportamento de NPCs e consequências já validadas.`,
        });
        finalRhythmAssessment = assessNarrativeRhythm(response, turnRhythm);
      }
      // Se o provedor remoto ainda devolver texto curto após os reparos, mantenha o texto remoto.
      // Nunca substitua silenciosamente a cena por uma narração offline.
      const canonicalLocation = mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation;
      response = sanitizeNarrativeResponse(response, sceneEnvironment, canonicalLocation);
      if (narrativeIntent.advancesFiction) {
        response = ensureEnvironmentAnchoredResponse(response, environmentBinding);
        response.audioPlan = enrichAudioPlanWithEnvironment(response.audioPlan, sceneEnvironment);
      }
      const usedRoll = Boolean(base.pendingDiceRoll && base.pendingTest);
      const modelEvents = narrativeIntent.advancesFiction ? sanitizeModelEvents({ ...response, location: canonicalLocation }, mechanicallyUpdated, sceneId) : [];
      const events: CampaignEvent[] = narrativeIntent.advancesFiction ? [makeEvent('scene_started', sceneId), ...modelEvents] : [];
      if (usedRoll) events.push(makeEvent('test_resolved', sceneId, {
        skill: base.pendingTest!.skill,
        success: base.pendingDiceRoll!.success ?? false,
        characterId: base.pendingDiceRoll!.characterId || base.pendingTest!.characterId || base.activeCharacterId || '',
      }));
      const synchronizedNpcIds = narrativeIntent.advancesFiction
        ? reconcileSceneNpcPresence({
            canonicalIds: mechanicallyUpdated.presentNpcIds || [],
            npcs: mechanicallyUpdatedNpcs,
            narration: response.atmospheric_narration || '',
            actionResult: response.action_result || '',
            voiceScript: response.voiceScript || [],
            events,
            location: canonicalLocation,
          })
        : mechanicallyUpdated.presentNpcIds || [];
      if (narrativeIntent.advancesFiction) {
        mechanicallyUpdatedNpcs = synchronizeNpcPresenceFlags(mechanicallyUpdatedNpcs, synchronizedNpcIds);
        mechanicallyUpdated = { ...mechanicallyUpdated, presentNpcIds: synchronizedNpcIds, updatedAt: Date.now() };
      }
      const sceneVisualPrompt = buildStoredSceneVisualPrompt({
        modelPrompt: response.sceneVisualPrompt || response.visualPrompt,
        narration: response.atmospheric_narration,
        actionResult: response.action_result,
        location: canonicalLocation,
        weather: environmentBinding.weatherLabel,
        timeOfDay: environmentBinding.period,
        presentNpcs: mechanicallyUpdatedNpcs.filter(npc => synchronizedNpcIds.includes(npc.id)),
      });
      const sourceNumbers = Array.from(new Set([...(response.usedSourceNumbers || []), ...(plan.ruleSourceNumbers || [])]));
      const newNode: SceneNode = {
        id: sceneId,
        timestamp: Date.now(),
        playerAction: userAction,
        atmospheric_narration: response.atmospheric_narration,
        action_result: response.action_result,
        visualPrompt: response.visualPrompt,
        sceneVisualPrompt,
        sceneVisualPromptCreatedAt: Date.now(),
        location: canonicalLocation,
        weather: environmentBinding.weatherLabel,
        timeOfDay: environmentBinding.period,
        worldHour: environmentBinding.hour,
        worldMinute: environmentBinding.minute,
        narrativeMeta: {
          intentClass: narrativeIntent.intentClass,
          advancesFiction: narrativeIntent.advancesFiction,
          checkpointSceneId: narrativeCheckpoint.sceneId,
          openingSignature: finalQuality.signature || undefined,
          qualityFlags: finalQuality.flags,
          rhythmCadence: turnRhythm.cadence,
          rhythmFlags: finalRhythmAssessment.flags,
          rhythmWordCount: finalRhythmAssessment.words,
          rhythmParagraphCount: finalRhythmAssessment.paragraphs,
          visibleFacts: Array.from(new Set((plan.visibleFacts || []).map(item => String(item || '').trim()).filter(Boolean))).slice(0, 20),
        },
        choices: response.choices || [],
        voiceScript: response.voiceScript || [],
        requiredTest: null,
        statUpdates: {},
        presentNpcIds: synchronizedNpcIds,
        audioPlan: response.audioPlan,
        events,
        sources: selectedManuals.slice(0, 5).map((source, index) => {
          const sourceNumber = index + 1;
          const wasApplied = sourceNumbers.includes(sourceNumber);
          return {
            ...source,
            usage: wasApplied
              ? (plan.ruleSummary || response.sourceUsageSummary || source.usage || 'Regra consultada e aplicada nesta resolução.')
              : 'Fonte consultada obrigatoriamente antes da resolução; nenhuma alteração mecânica foi derivada deste trecho.',
          };
        }),
      };
      Object.assign(newNode.narrativeMeta as any, {
        generationSource: localFallbackActive ? 'local-fallback' : String((response as any)._braseiroGenerationSource || 'gemini'),
        generationModel: localFallbackActive ? 'mestre-local' : String((response as any)._braseiroModel || ''),
        narrationWordCount: String(response.atmospheric_narration || '').trim().split(/\s+/).filter(Boolean).length,
      });

      const diversified = narrativeIntent.advancesFiction
        ? diversifySceneChoices(mechanicallyUpdated, newNode, response.choices || [], mechanicallyUpdatedNpcs.filter(npc => newNode.presentNpcIds.includes(npc.id)))
        : { choices: [], families: [] };
      newNode.choices = diversified.choices;
      if (narrativeIntent.advancesFiction) {
        newNode.imageAspect = '16:9';
        newNode.imageFingerprint = sceneImageFingerprint(newNode);
      }
      const rememberedCampaign = narrativeIntent.advancesFiction
        ? rememberChoices(mechanicallyUpdated, diversified.choices, diversified.families)
        : mechanicallyUpdated;
      const memory = narrativeIntent.advancesFiction
        ? updateCampaignMemory(rememberedCampaign, newNode, events, response.narrativeLog)
        : {
            campaignSummary: rememberedCampaign.campaignSummary,
            recentEvents: rememberedCampaign.recentEvents,
            clues: rememberedCampaign.clues,
            presentNpcIds: rememberedCampaign.presentNpcIds,
          };
      let updated: CampaignState = {
        ...rememberedCampaign,
        ...memory,
        presentNpcIds: newNode.presentNpcIds,
        characterSheet: activeAfterMechanics?.sheet || mechanicallyUpdated.characterSheet,
        currentLocation: canonicalLocation,
        history: [...mechanicallyUpdated.history, newNode],
        pendingDiceRoll: narrativeIntent.advancesFiction ? null : mechanicallyUpdated.pendingDiceRoll,
        pendingTest: narrativeIntent.advancesFiction ? null : mechanicallyUpdated.pendingTest,
        events: [...(mechanicallyUpdated.events || []), ...events],
        updatedAt: Date.now(),
      };
      if (updated.masterState && narrativeIntent.advancesFiction) {
        updated.masterState = {
          ...updated.masterState,
          memory: { ...updated.masterState.memory, campaignSummary: memory.campaignSummary },
          updatedAt: Date.now(),
        };
      }
      if (narrativeIntent.advancesFiction && !userAction.startsWith('[MOVIMENTO DE JORNADA JÁ APLICADO]')) {
        updated = attachResolutionSummary(base, updated, characters, mechanicallyUpdatedCharacters, userAction);
      }

      const finalMapDiscovery = await loadMapDiscovery(updated.id).catch(() => barbaraMapDiscovery);
      const finalProjectedStateValidation = await validateBarbaraState(updated, mechanicallyUpdatedCharacters, mechanicallyUpdatedNpcs, finalMapDiscovery);
      if (finalProjectedStateValidation.valid === false) {
        throw new Error(`Barbara recusou a projeção do estado antes do commit; nenhuma consequência foi persistida: ${finalProjectedStateValidation.reason || finalProjectedStateValidation.error || 'estado incompatível'}`);
      }

      const environmentMemory = narrativeIntent.advancesFiction
        ? await prepareEnvironmentNarrativeMemory(
            sceneEnvironment, sceneId, `${response.atmospheric_narration || ''} ${response.action_result || ''}`,
          ).catch(error => { console.warn('[BRASEIRO_AMBIENTE] memória narrativa não pôde ser preparada:', error); return []; })
        : [];
      const committed = activeTurnId && activeTurnKey
        ? await TurnCoordinator.commitCanonical({
            turnId: activeTurnId, idempotencyKey: activeTurnKey, inputChannel, campaign: updated, sceneId, events,
            characters: mechanicallyUpdatedCharacters, npcs: mechanicallyUpdatedNpcs, audits: mechanical.audits,
            environmentMemory, reason: narrativeIntent.advancesFiction ? (localFallbackActive ? 'novo turno local degradado' : 'novo turno orquestrado') : 'consulta narrativa sem avanço ficcional',
            payload: { inputChannel, localFallback: localFallbackActive, narrativeIntent: narrativeIntent.intentClass, advancesFiction: narrativeIntent.advancesFiction, currentHexId: updated.masterState?.position.currentHexId || null, weather: environmentBinding.weatherLabel, timeOfDay: environmentBinding.period, worldHour: environmentBinding.hour },
          })
        : await saveCanonicalTransaction({ campaign: updated, characters: mechanicallyUpdatedCharacters, npcs: mechanicallyUpdatedNpcs, audits: mechanical.audits, environmentMemory, campaignEvents: events, reason: narrativeIntent.advancesFiction ? 'turno sem identificador recuperado' : 'consulta narrativa sem avanço ficcional' });
      saveEmergencyCampaignMirror(committed);
      campaignRef.current = committed;
      setCampaign(committed);
      setCharacters(mechanicallyUpdatedCharacters);
      setAllNpcs(mechanicallyUpdatedNpcs);
      setPresentNpcs(mechanicallyUpdatedNpcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
      if (narrativeIntent.advancesFiction) {
        setCurrentImage(null);
        // Uma nova batida ficcional substitui a mesa sonora anterior, mas nunca
        // inicia voz, música, ambiência ou SFX automaticamente. Consultas e
        // planejamento preservam imagem e áudio do checkpoint.
        cancelNarrationPlayback();
        // New scenes replace only scene-owned sound beds. A track selected
        // manually in the sound library uses slot "library-preview" and must
        // survive scene transitions until the user stops or replaces it.
        AudioEngine.stopChannel('ambience', 'base');
        AudioEngine.stopChannel('ambience', 'distant');
        AudioEngine.stopChannel('ambience', 'detail0');
        AudioEngine.stopChannel('ambience', 'detail1');
        AudioEngine.stopChannel('weather', 'base');
        AudioEngine.stopChannel('music', 'base');
        AudioEngine.stopChannel('sfx');
        setAudioActive(AudioEngine.isLoopActive('music', 'library-preview'));
      }
      setIsStoryLoading(false);
    } catch (error) {
      if (activeTurnId) await TurnCoordinator.abort(activeTurnId, error).catch(() => undefined);
      console.error('[BRASEIRO_TURN]', error);
      saveEmergencyCampaignMirror(base);
      campaignRef.current = base;
      setCampaign(base);
      setStoryError(friendlyTurnError(error));
      setIsStoryLoading(false);
    }
  };

  const handleCompanySave = async (company: CompanyState) => {
    const base = campaignRef.current;
    if (!base) return;
    const masterState = normalizeMasterState(base);
    const patronChanged = String(company.patron || '').trim() !== String(masterState.company.patron || '').trim()
      || (patronProfile(company.patron || '').id === 'elrond' && Number(masterState.company.basePatronFellowshipBonus || 0) < 1);
    const patronResolution = patronChanged
      ? applyMasterCommands(base, characters, allNpcs, [{ type: 'set_company_patron', patron: company.patron || '', reason: 'Patrono alterado na tela da Companhia.' }])
      : null;
    if (patronResolution?.rejected.length) throw new Error(patronResolution.rejected[0].reason);
    const resolvedState = patronResolution ? normalizeMasterState(patronResolution.campaign) : masterState;
    const reconciledCompany = reconcileCompanyDraft(company, masterState.company, resolvedState.company);
    const party = new Set(base.partyCharacterIds || []);
    const validRoles = Object.fromEntries(Object.entries(company.journeyRoles || {}).filter(([, id]) => Boolean(id) && party.has(String(id))));
    const validRoleAssignments = Object.fromEntries(Object.entries(company.journeyRoleAssignments || {}).map(([role,ids])=>[role,Array.from(new Set((ids||[]).filter(id=>party.has(id))))]).filter(([,ids])=>(ids as string[]).length>0));
    const validFocus = Object.fromEntries(Object.entries(company.fellowshipFocus || {}).filter(([heroId, focusId]) => party.has(heroId) && party.has(String(focusId)) && heroId !== focusId));
    const updated: CampaignState = {
      ...(patronResolution?.campaign || base),
      masterState: {
        ...resolvedState,
        company: { ...reconciledCompany, journeyRoles: validRoles, journeyRoleAssignments: validRoleAssignments, fellowshipFocus: validFocus },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };
    const committed = await saveMasterTransaction(updated, patronResolution?.characters || characters, patronResolution?.audits || [], 'configuração da Companhia');
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
  };

  const handleSpendHope = async (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired = false, useDesperateCourage = false): Promise<number> => {
    const base = campaignRef.current;
    const character = characters.find(item => item.id === characterId);
    if (!base || !character || !(base.partyCharacterIds || []).includes(characterId)) return 0;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'spend_hope', targetCharacterId: characterId, attribute,
      useDesperateCourage,
      reason: `Esperança gasta antes de uma jogada de ${attribute}.`, source: 'O Um Anel 2e — gasto de Esperança',
    }]);
    if (mechanical.rejected.length) return 0;
    const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'gasto de Esperança');
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    return inspired||useDesperateCourage ? 2 : 1;
  };

  const handleAskMasterRules = async (question: string): Promise<void> => {
    if (isStoryLoading || !question.trim()) return;
    setIsStoryLoading(true); setStoryError(null);
    await processTurn(`[DÚVIDA PARA O MESTRE]: ${question.trim()}`, true);
  };

  const handleCombatMasterAction = async (action: string): Promise<void> => {
    if (isStoryLoading || !action.trim()) return;
    setIsStoryLoading(true); setStoryError(null);
    await processTurn(`[AÇÃO TÁTICA DA COMPANHIA] ${action.trim()}`, false);
  };

  const handleWorldCommands = async (commands: MasterCommand[], reason: string): Promise<void> => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, reason);
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
    setPresentNpcs(mechanical.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
  };


  const commitJourneyAction = async (commands: MasterCommand[], reason: string, actionLabel: string) => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, actionLabel);
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
    setPresentNpcs(mechanical.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
    return committed;
  };

  const handleSetEyeAwarenessEnabled = async (enabled: boolean) => {
    const base = campaignRef.current;
    if (!base) return;
    const partyIds = new Set(base.partyCharacterIds || []);
    const party = characters.filter(character => partyIds.has(character.id));
    const initial = calculateInitialStriderEyeAwareness(party);
    await commitJourneyAction([{
      type: 'set_eye_awareness_rule',
      enabled,
      initial,
      huntThreshold: 16,
      reason: enabled ? 'Jogador ativou a regra opcional Vigilância do Olho.' : 'Jogador desativou a regra opcional Vigilância do Olho.',
    }], 'preferência da regra opcional Vigilância do Olho', enabled ? 'Ativar Vigilância do Olho' : 'Desativar Vigilância do Olho');
  };

  const handleAdjustEyeAwareness = async (amount: number, reason: string) => {
    await commitJourneyAction([{
      type: 'adjust_eye_awareness',
      amount,
      source: amount >= 3 ? 'magic-powerful' : amount === 2 ? 'magic-major' : 'magic-minor',
      reason,
    }], 'ajuste de Vigilância do Olho', reason);
  };

  const handleJourneyStartManual = async (roles: Partial<Record<JourneyRole,string>>, mounted: boolean, forcedMarch: boolean, roleAssignments?:Partial<Record<JourneyRole,string[]>>) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    const route = (master.journey.plannedRouteHexIds || []).filter(Boolean);
    await commitJourneyAction([{
      type: 'player_start_journey',
      destinationHexId: route.at(-1) || master.journey.destinationHexId,
      roles,
      roleAssignments,
      routeHexIds: route,
      mounted,
      forcedMarch,
      reason: 'Papéis e opções de Jornada escolhidos explicitamente pelo jogador.',
    }], 'preparação manual da Jornada', 'Preparar a Jornada');
  };

  const handleJourneyRollManual = async (
    kind: 'march'|'event'|'end',
    characterId: string,
    skill: string,
    extraDice = 0,
    forcedMode?: 'favoured'|'normal'|'ill-favoured',
    mountVigour = 0,
    spendHope = false,
    useMagicalSuccess = false,
    useDesperateCourage = false,
    miruvorDoseId?: string,
    useBirdsTongueInspired = false,
    supporterCharacterId?:string,
    supporterSkill?:string,
    risk?:'standard'|'dangerous'|'foolhardy',
  ) => {
    const base = campaignRef.current;
    if (!base) return;
    const hero = characters.find(character => character.id === characterId);
    if (!hero) throw new Error('Herói da rolagem não encontrado.');
    const master = normalizeMasterState(base);
    if ((spendHope || useMagicalSuccess) && hero.sheet.attributes.hopeCurrent <= 0) throw new Error('Esperança insuficiente.');
    if (spendHope && useMagicalSuccess) throw new Error('Escolha entre o bônus normal de Esperança e o Sucesso Mágico.');
    if(useDesperateCourage&&(!spendHope||!hasVirtue(hero.sheet,'Coragem Desesperada'))) throw new Error('Coragem Desesperada exige a Virtude e o gasto de Esperança na Jornada.');
    if(useBirdsTongueInspired&&(!spendHope||!hasVirtue(hero.sheet,'A Língua dos Pássaros')||master.scene.status!=='active'||master.scene.kind!=='journey'||!master.scene.openAir||(master.scene.birdsTongueInspiredUsedHeroIds||[]).includes(hero.id))) throw new Error('A Língua dos Pássaros exige Esperança, a Virtude e uso ainda disponível nesta cena de Jornada ao ar livre.');
    const heroForRoll=useDesperateCourage?{...hero,sheet:spendHopeMechanically(hero.sheet,attributeForSkill(skill),true).sheet}:hero;
    const terrain = master.journey.segments?.at(-1)?.terrain || '';
    const isNight = master.worldClock.hour < 6 || master.worldClock.hour >= 20;
    const journeyMode=master.company.rulesMode==='strider'&&(base.partyCharacterIds||[]).length===1?'strider' as const:'standard' as const;
    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode };
    const supplementSource = magicalSuccessSource(hero.sheet, magicalContext);
    const treasureSource = blessingSource(hero.sheet,skill);
    const source = useMagicalSuccess ? (supplementSource||treasureSource) : null;
    const canMagic = canSpendHopeForSupplementMagicalSuccess(hero.sheet, magicalContext) || identifiedBlessingCanMagicalSuccess(hero.sheet,skill);
    if (useMagicalSuccess && !canMagic) throw new Error('Sucesso Mágico indisponível para esta Perícia/situação.');
    if ((spendHope||useMagicalSuccess) && curseBlocksHopeForSkill(hero.sheet,skill)) throw new Error('Malícia: o item impede gastar Esperança nesta rolagem.');
    const treasureDice=magicalBlessingDice(hero.sheet,skill);
    const darknessPenalty=curseDarkeningDicePenalty(hero.sheet);
    const loneLandsDice=forodwaithJourneyDiceModifier(master.journey,skill);
    const supporter=supporterCharacterId?characters.find(character=>character.id===supporterCharacterId):undefined;
    const chosenSupportSkill=supporter?.sheet.skills.find(item=>item.name===supporterSkill&&Number(item.rank)>0);
    if(supporterCharacterId){
      const event=master.journey.pendingEvent;
      const eligible=event?journeySupporterIds(master,base.partyCharacterIds||[],event.targetRole,hero.id):[];
      if(kind!=='event'||!supporter||supporter.id===hero.id||!eligible.includes(supporter.id)||!chosenSupportSkill||supporter.sheet.attributes.hopeCurrent<=0) throw new Error('O Suporte da Jornada exige outro Herói do mesmo papel, com Perícia treinada e Esperança.');
    }
    const focusDice=supporterCharacterId?supportSuccessDice(master.company,supporterCharacterId,hero.id):0;
    const effectiveExtra=Math.trunc(extraDice)+treasureDice+darknessPenalty+loneLandsDice+focusDice;
    const baseMode=forcedMode || (shadowAtLimit(hero.sheet) ? 'ill-favoured' : findSkill(hero.sheet,skill)?.favorite ? 'favoured' : 'normal');
    const loneLandsMode=forodwaithRollMode(master.journey,skill,baseMode);
    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + effectiveExtra, skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(heroForRoll, skill, effectiveExtra, loneLandsMode, spendHope ? (useDesperateCourage?2:1) : 0,{isNight:Boolean(master.scene.isDark)||isNight||master.combat.environment==='underground',environment:master.combat.environment,inspired:useBirdsTongueInspired,journeyMode});
    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(heroForRoll.sheet));
    roll=applyIllLuckToResult(heroForRoll.sheet,roll);
    roll=applyEldarAutomaticMagicalSuccess(heroForRoll.sheet,roll);
    const commands: MasterCommand[] = [];
    if ((spendHope&&!useBirdsTongueInspired) || useMagicalSuccess) {
      commands.push({
        type: 'spend_hope',
        targetCharacterId: hero.id,
        attribute: attributeForSkill(skill),
        useDesperateCourage,
        reason: useMagicalSuccess ? 'Esperança gasta para obter Sucesso Mágico na Jornada.' : 'Esperança gasta explicitamente na rolagem de Jornada.',
      });
    }
    commands.push({
      type: 'player_record_journey_roll',
      kind,
      targetCharacterId: hero.id,
      skill,
      targetNumber: targetNumberForSkill(heroForRoll.sheet, skill),
      roll,
      mountVigour,
      miruvorDoseId,
      spendHope,
      useDesperateCourage,
      useBirdsTongueInspired,
      supporterCharacterId,
      supporterSkill,
      risk:kind==='event'&&master.company.rulesMode==='strider'?risk:undefined,
      reason: skill + ' rolada manualmente pelo jogador.',
    });
    const eyeRolled = roll.featResult === 11 || (Array.isArray(roll.featResults) && roll.featResults.includes(11));
    if (master.company.eyeAwarenessEnabled && master.activeMode !== 'combat' && eyeRolled) {
      commands.push({
        type: 'adjust_eye_awareness',
        amount: 1,
        source: 'eye-roll',
        reason: '+1: uma rolagem do jogador fora de combate mostrou o Olho de Sauron.',
      });
    }
    await commitJourneyAction(commands, 'rolagem manual de Jornada', 'Rolar ' + skill + ' · ' + (hero.sheet.identity.name || hero.name));
  };

  const handleApplyJourneyMarch = async () => {
    await commitJourneyAction([{ type: 'player_confirm_journey_march', reason: 'Resultado da Jogada de Marcha confirmado pelo jogador.' }], 'aplicar Marcha confirmada', 'Confirmar Jogada de Marcha');
  };

  const handleSetEyeHuntModifiers = async (modifiers: { blessed: boolean; hidden: boolean; renowned: boolean; activelyHunted: boolean }) => {
    await commitJourneyAction([{
      type: 'set_eye_hunt_modifiers',
      modifiers,
      reason: 'Atualizar modificadores do Limiar de Caça do Modo Peregrino.',
    }], 'modificadores do Limiar de Caça', 'Atualizar Limiar de Caça');
  };

  const handlePremonitionJourney = async (sourceCharacterId:string,targetCharacterId:string) => {
    await commitJourneyAction([{type:'reroll_journey_roll',sourceCharacterId,targetCharacterId,reason:'Premonição de seu Povo: repetir todos os dados da Jornada antes de aplicar a consequência.'}],'repetir rolagem de Jornada com Premonição','Premonição de seu Povo');
  };

  const handleChooseJourneyEventTarget=async(targetCharacterId:string)=>{
    await commitJourneyAction([{type:'player_choose_journey_event_target',targetCharacterId,reason:'Jogador escolheu quem entre os Heróis do papel enfrentará o Evento de Jornada.'}],'escolher alvo entre os ocupantes do papel','Escolher alvo do Evento');
  };

  const handleGenerateJourneyEventManual = async (memoryChoice?:'normal'|'border-table'|'two-best-feat-dice',selectedTargetCharacterId?:string) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    if(memoryChoice){
      const staged=master.journey.pendingEventTarget;
      if(!staged) throw new Error('A escolha de Memória dos Tempos Antigos não está pendente.');
      const hasStriderJourney=master.company.rulesMode==='strider';
      const detailDie=hasStriderJourney?secureRoll(1,6):undefined;
      const eventRegion=memoryChoice==='border-table'?'border':staged.region;
      const specialForodwaith=Boolean(master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable);
      const rawFeatResults=memoryChoice==='two-best-feat-dice'||memoryChoice==='border-table'?Array.from({length:2},()=>secureRoll(1,12)):specialForodwaith?[secureRoll(1,12)]:journeyFeatResults(eventRegion);
      const featResults=rawFeatResults;
      await commitJourneyAction([{type:'player_generate_journey_event_with_memory',choice:memoryChoice,targetCharacterId:selectedTargetCharacterId,detailDie,featResults,reason:'Jogador escolheu explicitamente o alvo e a tabela/regra de Façanha de Memória dos Tempos Antigos.'}],'resolver escolha de Memória dos Tempos Antigos','Rolar Evento de Jornada');
      return;
    }
    const discovery = await loadMapDiscovery(base.id).catch(() => null);
    const manifest = await loadRuntimeMapManifest().catch(() => null);
    const currentHexId = discovery?.currentHexId || master.position.currentHexId;
    const currentHex = currentHexId ? manifest?.hexes.find(hex => hex.id === currentHexId) : undefined;
    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    const targetDie = secureRoll(1, 6);
    const detailDie=master.company.rulesMode==='strider'?secureRoll(1,6):undefined;
    const targetRole=targetDie<=2?'scout':targetDie<=4?'lookout':'hunter';
    const partyIds=base.partyCharacterIds||[];
    const targetCharacterId=master.company.rulesMode==='strider'&&partyIds.length<=2?partyIds[(targetDie-1)%Math.max(1,partyIds.length)]:(master.company.journeyRoleAssignments?.[targetRole]?.[0]||master.company.journeyRoles[targetRole]);
    const targetHero=characters.find(hero=>hero.id===targetCharacterId);
    const roleMembers=master.company.rulesMode==='strider'&&partyIds.length<=2?[targetCharacterId].filter(Boolean) as string[]:(master.company.journeyRoleAssignments?.[targetRole]?.length?master.company.journeyRoleAssignments[targetRole]!: [master.company.journeyRoles[targetRole]].filter(Boolean) as string[]);
    const hasMemoryChoice=roleMembers.some(id=>{const member=characters.find(hero=>hero.id===id);return Boolean(member&&coreCultureId(member.sheet.identity.culture)==='elves'&&hasVirtue(member.sheet,'Memória dos Tempos Antigos'))});
    if(roleMembers.length>1||hasMemoryChoice){
      await commitJourneyAction([{type:'player_stage_journey_event_target',region,targetDie,targetCharacterId,terrain:currentHex?.terrain,reason:'Alvo e papel do Evento rolados; escolha persistida antes de revelar a Façanha.'}],'escolher alvo do Evento','Alvo do Evento');
      return;
    }
    const rawFeatResults = master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable
      ?[secureRoll(1,12)]
      :journeyFeatResults(region);
    const featResults = rawFeatResults;
    await commitJourneyAction([{
      type: 'player_generate_journey_event',
      region,
      targetDie,
      detailDie,
      featResults,
      terrain: currentHex?.terrain,
      reason: 'Jogador rolou explicitamente o alvo e o Dado de Façanha do Evento de Jornada.',
    }], 'gerar Evento de Jornada manualmente', 'Rolar Evento de Jornada');
  };

  const handleOpenJourneyNotable = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const event = normalizeMasterState(base).journey.pendingEvent;
    if (!event?.notable) throw new Error('O Evento atual não é um Encontro Notável.');
    setIsStoryLoading(true);
    setStoryError(null);
    await processTurn('[ENCONTRO NOTÁVEL DA JORNADA] Desenvolva este encontro como uma cena estendida e significativa. Não reduza o encontro a uma única rolagem, não resolva automaticamente o resultado geral e não avance Marcha/Evento/Jornada. O jogador decidirá depois se o desfecho global foi favorável ou desfavorável.', false, base);
  };

  const handleResolveJourneyNotable = async (success: boolean, selectedTargetCharacterId?:string) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    const event = master.journey.pendingEvent;
    if (!event?.notable) throw new Error('O Evento atual não é um Encontro Notável.');
    const heroId = selectedTargetCharacterId || event.targetCharacterId || master.company.journeyRoleAssignments?.[event.targetRole]?.[0] || master.company.journeyRoles[event.targetRole] || base.partyCharacterIds?.[0] || base.activeCharacterId;
    const hero = characters.find(character => character.id === heroId);
    if (!hero) throw new Error('Herói do Encontro Notável não encontrado.');
    const syntheticRoll = {
      id: 'notable_' + String(Date.now()),
      type: 'one-ring',
      notation: 'Encontro Notável',
      results: [],
      modifier: 0,
      total: success ? targetNumberForSkill(hero.sheet, event.skill) : 0,
      timestamp: Date.now(),
      featResult: success ? 12 : 11,
      successResults: [],
      targetNumber: targetNumberForSkill(hero.sheet, event.skill),
      skillName: event.skill,
      success,
      successLevel: success ? 1 : 0,
      tengwarCount: 0,
      characterId: hero.id,
      characterName: hero.sheet.identity.name || hero.name,
    };
    await commitJourneyAction([
      {
        type: 'player_record_journey_roll',
        kind: 'event',
        targetCharacterId: hero.id,
        skill: event.skill,
        targetNumber: targetNumberForSkill(hero.sheet, event.skill),
        roll: syntheticRoll,
        reason: success ? 'Encontro Notável resolvido favoravelmente.' : 'Encontro Notável resolvido desfavoravelmente.',
      },
      {
        type: 'player_confirm_journey_event',
        reason: 'Consequência do Encontro Notável confirmada pelo jogador.',
      },
    ], 'resolver Encontro Notável', success ? 'Encontro Notável · favorável' : 'Encontro Notável · desfavorável');
  };

  const handleApplyJourneyEvent = async (riskOutcome?:'simple-failure'|'success-with-misfortune'|'failure-with-misfortune'|'disaster',riskNote?:string) => {
    const base = campaignRef.current;
    if (!base) return;
    const event = normalizeMasterState(base).journey.pendingEvent;
    if (!event) throw new Error('Nenhum Evento aguarda confirmação.');
    await commitJourneyAction([{ type: 'player_confirm_journey_event', riskOutcome, riskNote:riskOutcome?riskNote:undefined, reason: 'Consequência do Evento confirmada pelo jogador.' }], 'resolver Evento de Jornada confirmado', 'Resolver ' + event.description);
  };

  const handleResolveForodwaithSpecial = async (choice:'continue'|'start-encounter'|'hide'|'face-hunters'|'press-on'|'take-shelter',targetCharacterId?:string) => {
    const base=campaignRef.current;
    if(!base) return;
    await commitJourneyAction([{type:'resolve_forodwaith_special_event',choice,targetCharacterId,reason:'Resultado da tabela especial de Forodwaith resolvido pela escolha do jogador.'}],'resolver evento especial de Forodwaith','Forodwaith: '+choice);
  };

  const handleContinueJourneyStage = async () => {
    await commitJourneyAction([{ type: 'player_continue_journey_stage', reason: 'Jogador confirmou a continuação para a próxima etapa.' }], 'continuar etapa de Jornada', 'Continuar Jornada');
  };

  const handleCompleteJourneyManual = async () => {
    await commitJourneyAction([{ type: 'player_complete_journey', reason: 'Jogador confirmou as rolagens finais e o encerramento da Jornada.' }], 'encerrar Jornada manualmente', 'Encerrar Jornada');
  };

  const handleJourneyHerbalRecovery = async (characterId:string,risk?:'standard'|'dangerous'|'foolhardy') => {
    await commitJourneyAction([{type:'record_journey_herbal_roll',targetCharacterId:characterId,risk,reason:'Remédios Herbais usado no encerramento da Jornada.'}], 'Remédios Herbais no fim da Jornada', 'Remédios Herbais');
  };

  const handleResolveJourneyHerbalRisk = async (characterId:string,outcome:'simple-failure'|'success-with-misfortune'|'failure-with-misfortune'|'disaster',note?:string) => {
    await commitJourneyAction([{type:'resolve_journey_herbal_risk',targetCharacterId:characterId,outcome,note,reason:'Aplicar o Nível de Risco do teste de Remédios Herbais.'}],'resolver risco de Remédios Herbais','Consequência de Remédios Herbais');
  };

  const handleOpenJourneyEncounter = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const beforeSceneId = base.history.at(-1)?.id || '';
    setIsStoryLoading(true);
    setStoryError(null);
    await processTurn('[ENCONTRO FORTUITO DA JORNADA] O Evento foi resolvido com sucesso. Apresente agora um encontro favorável e contextual ao local. Crie ou reutilize NPCs concretos com upsert_npc e marque explicitamente quem está presente com set_present_npcs. Não resolva outra Marcha, outro Evento ou movimento.', false, base);
    const after = campaignRef.current;
    if (!after) return;
    const sceneAdvanced = (after.history.at(-1)?.id || '') !== beforeSceneId;
    if (sceneAdvanced && (after.presentNpcIds || []).length > 0) {
      await commitJourneyAction([{ type: 'player_acknowledge_journey_encounter', reason: 'NPCs do Encontro Fortuito foram apresentados na cena.' }], 'confirmar Encontro Fortuito apresentado', 'Encontro Fortuito apresentado');
    }
  };

  const handleOpenEyeRevelation = async (featResult?: number, customResult?: string) => {
    const base = campaignRef.current;
    if (!base) return;
    const current=normalizeMasterState(base);
    if(current.company.rulesMode==='strider'&&(!featResult&&!customResult?.trim()||Boolean(featResult&&(featResult<1||featResult>12)))) throw new Error('Modo Peregrino exige resultado d12 válido ou ameaça imaginada.');
    await commitJourneyAction([{type:'resolve_eye_revelation',featResult,customResult,reason:'Registrar o resultado da Tabela de Episódios de Revelação antes de narrar.'}],'registrar Episódio de Revelação','Episódio de Revelação');
    const prepared=campaignRef.current;
    if(!prepared)return;
    const saved=normalizeMasterState(prepared).company.eyeRevelationHistory?.at(-1);
    setIsStoryLoading(true);
    setStoryError(null);
    const tableContext=saved&&saved.featResult>0?' Resultado '+saved.featResult+' da tabela: '+saved.result:saved?.result?' Ameaça imaginada e registrada: '+saved.result:' O Historiador escolheu um episódio narrativo apropriado.';
    await processTurn('[EPISODIO DE REVELACAO] A Vigilancia do Olho alcancou o Limiar de Caca.'+tableContext+' Apresente e desenvolva esse episodio coerente com a situacao atual, sem escolher silenciosamente recursos ou decisoes dos jogadores. Preserve o resultado registrado e faca as consequencias surgirem na ficcao.', false, prepared);
  };

  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
    const base = campaignRef.current;
    if (!base) return;
    const masterState = normalizeMasterState(base);
    const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
    const synced: CampaignState = {
      ...base,
      masterState: {
        ...masterState,
        journey: {
          ...masterState.journey,
          plannedRouteHexIds: route,
          destinationHexId: route.at(-1) || masterState.journey.destinationHexId,
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };
    const committed = await saveCanonicalTransaction({ campaign: synced, mapDiscovery: discovery, reason: 'estado do atlas: ' + reason });
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
    }));
  };

  const handlePartyRested = async (kind: 'short' | 'long') => {
    const base = campaignRef.current;
    if (!base) return;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'resolve_rest',
      kind,
      reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
      source: 'O Um Anel 2e — Descanso',
    }]);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, kind === 'short' ? 'Descanso Curto' : 'Descanso Prolongado');
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, kind === 'short' ? 'descanso curto' : 'descanso prolongado', mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
  };

  const handlePartyMoved = async (move: PartyMapMove) => {
    const base = campaignRef.current;
    if (!base) return;

    const beforeMaster = normalizeMasterState(base);
    const initialPlacement = !beforeMaster.position.currentHexId;
    if (!initialPlacement) {
      const activeJourney = ['planned', 'travelling', 'paused'].includes(beforeMaster.journey.status);
      const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
      if (!activeJourney || !nextRouteHexId) throw new Error('Movimento livre bloqueado: selecione um destino e use Planejar rota. Depois avance a Companhia pela Jornada.');
      if (move.toHexId !== nextRouteHexId) throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
      if (beforeMaster.journey.pendingRoll) throw new Error('Movimento bloqueado: confirme primeiro a rolagem de Jornada exibida.');
      if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) throw new Error('Movimento bloqueado: resolva primeiro o Evento de Jornada atual.');
      if (beforeMaster.journey.awaitingEndRoll) throw new Error('Movimento bloqueado: a Companhia alcançou o destino e precisa encerrar a Jornada.');
      if (beforeMaster.journey.stageComplete) throw new Error('Movimento bloqueado: conclua a etapa atual da Jornada antes de avançar.');
      if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) throw new Error('Movimento bloqueado: a Jogada de Marcha do Guia precisa ser resolvida antes do próximo hex.');
    }

    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'move_party',
      toHexId: move.toHexId,
      locationName: move.locationName,
      distanceKm: move.distanceKm,
      terrain: move.terrain,
      reason: 'Movimento confirmado no Atlas pelo jogador.',
    }]);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));

    const manifest = await loadRuntimeMapManifest().catch(() => null);
    const currentHex = manifest?.hexes.find(hex => hex.id === move.toHexId);
    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    let movedCampaign = mechanical.campaign;
    if (normalizeMasterState(movedCampaign).company.eyeAwarenessEnabled) {
      const movedMaster = normalizeMasterState(movedCampaign);
      const threshold = region === 'border' ? 18 : region === 'dark' ? 14 : 16;
      movedCampaign = {
        ...movedCampaign,
        masterState: {
          ...movedMaster,
          company: { ...movedMaster.company, eyeHuntThreshold: threshold },
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      };
    }

    const locationEvent = makeEvent('location_changed', undefined, { hexId: move.toHexId, location: move.locationName, terrain: move.terrain });
    movedCampaign = { ...movedCampaign, currentLocation: move.locationName, events: [...(movedCampaign.events || []), locationEvent], updatedAt: Date.now() };
    const reconciled = reconcileCanonicalState(movedCampaign, mechanical.characters, mechanical.npcs, move.discovery, { mapWins: false, preservePresenceUnion: true });
    const summarized = attachResolutionSummary(base, reconciled.campaign, characters, reconciled.characters, 'Viajar para ' + move.locationName);
    const environmentAdvance = await prepareEnvironmentAdvance(summarized, locationEvent.id);
    const committed = await saveCanonicalTransaction({
      campaign: summarized,
      characters: reconciled.characters,
      npcs: reconciled.npcs,
      audits: mechanical.audits,
      mapDiscovery: reconciled.discovery,
      environmentState: environmentAdvance.state,
      autonomousEvents: environmentAdvance.autonomousEvents,
      campaignEvents: [locationEvent],
      reason: 'movimento de Jornada confirmado sem automação de rolagens',
    });

    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(reconciled.characters);
    setAllNpcs(reconciled.npcs);
    setPresentNpcs(reconciled.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
    setCurrentImage(null);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: reconciled.discovery.campaignId, mapId: reconciled.discovery.mapId, currentHexId: reconciled.discovery.currentHexId },
    }));

    setIsStoryLoading(true);
    setStoryError(null);
    setNarrationError(null);
    await processTurn(
      '[MOVIMENTO DE JORNADA JÁ APLICADO] A Companhia chegou a ' + move.locationName + '. O movimento, o tempo e a Marcha já foram aplicados localmente. NÃO faça Jogada de Marcha, NÃO gere nem resolva Evento de Jornada e NÃO encerre a Jornada. Reavalie explicitamente os NPCs: preserve quem plausivelmente viajou ou pertence ao novo local; retire somente quem de fato saiu; introduza NPCs apenas quando a ficção atual justificar.',
      false,
      committed,
    );
  };


  type PreparedSceneAudio = {
    sounds: SoundItem[];
    rules: SoundKeywordRule[];
    plan: SoundPlan;
    context: SceneAudioContext;
    ambience: SoundItem | null;
    distantAmbience: SoundItem | null;
    weather: SoundItem | null;
    music: SoundItem | null;
    eventSounds: Map<string, SoundItem>;
  };

  type NarrationUnit = {
    text: string;
    start: number;
    segment: VoiceSegment;
    events: SoundEffectEvent[];
  };

  const eventKey = (event: SoundEffectEvent) => `${event.id}:${event.charIndex}`;

  const deriveCanonicalAmbience = async (node: SceneNode): Promise<{ base?: string; evidence: string }> => {
    const base = campaignRef.current;
    const discovery = base ? await loadMapDiscovery(base.id).catch(() => null) : null;
    const [runtime, manifest] = discovery?.currentHexId
      ? await Promise.all([
          loadTerraMediaRuntimeContext(discovery.currentHexId).catch(() => null),
          loadRuntimeMapManifest().catch(() => null),
        ])
      : [null, null];
    const currentHex = discovery?.currentHexId ? manifest?.hexes.find(hex => hex.id === discovery.currentHexId) : undefined;
    const mapped = [
      currentHex?.terrain, currentHex?.region, currentHex?.name, currentHex?.poi, currentHex?.placeType,
      ...(currentHex?.tags || []),
      ...(runtime?.locations || []).flatMap(location => [
        location.name, location.nameOriginal, location.type,
        ...(location.categories || []), ...(location.tags || []), ...(location.regions || []),
      ]),
    ].filter(Boolean).join(' ');
    const scene = normalizeMasterState(base || newCampaign()).scene;
    const formal = `${node.location || ''} ${scene.title || ''} ${scene.objective || ''}`;
    const evidence = normalizeText(`${mapped} ${formal}`);
    const exact = (pattern: RegExp, id: string) => pattern.test(evidence) ? id : undefined;
    // Ambientes de estrutura forte ganham prioridade. Taverna só é aceita se
    // estiver no local/título formal, nunca por uma lembrança na narração.
    const forced =
      exact(/ruina|ruins|torre abandonada|fortaleza abandonada|pedras antigas|amon sul|weathertop|topo do vento/, 'ruins') ||
      exact(/caverna|gruta|mina|subterraneo|tunel|moria/, 'cave') ||
      exact(/pantano|brejo|charco|midgewater|mosquitos/, 'swamp') ||
      exact(/floresta|bosque|mata|chetwood|old forest|floresta velha/, 'forest') ||
      exact(/rio|riacho|correnteza|bruinen|brandivino|brandywine/, 'river') ||
      exact(/cidade|vila|aldeia|mercado|bri|hobbiton|condado|shire/, 'town') ||
      exact(/estrada|caminho|trilha|jornada|road|path/, 'road');
    const formalOnly = normalizeText(formal);
    const tavern = /taverna|estalagem|taberna|sala comum|pousada|inn|tavern/.test(formalOnly) ? 'tavern' : undefined;
    return { base: forced || tavern, evidence: `${discovery?.currentHexId || 'sem-hex'}: ${mapped || formal || 'sem evidência cartográfica'}` };
  };

  const prepareSceneAudio = async (node: SceneNode): Promise<PreparedSceneAudio> => {
    const library = await getAudioLibrary();
    const sounds = library.sounds;
    const rules = library.rules;
    const canonicalAmbience = await deriveCanonicalAmbience(node);
    const plan = planSounds(
      node.location || campaignRef.current?.currentLocation || 'Eriador',
      node.atmospheric_narration || '',
      node.playerAction || '',
      node.audioPlan,
      node.events || [],
      sounds,
      rules,
      canonicalAmbience.base,
    );

    // O JSON grande e as regras de palavras-chave alimentam o catálogo na
    // importação. A escolha de cada PLAY vem do SQLite/rotação local.
    const selected = await selectSceneAudioFromCatalog(node, plan, sounds, eventKey);
    AudioEngine.configureBarbaraAudioMix((selected.barbaraPolicy as any)?.mix || null);
    const ambience = selected.ambience
      || (plan.ambience ? findBestSoundForQuery(sounds, 'ambience', plan.ambience, ambienceSearchAliases(plan.ambience)) : null);
    const distantAmbience = selected.distantAmbience
      || (selected.distantAmbienceId ? findBestSoundForQuery(sounds, 'ambience', selected.distantAmbienceId, ambienceSearchAliases(selected.distantAmbienceId)) : null);
    const music = selected.music
      || (plan.music ? findBestSoundForQuery(sounds, 'music', plan.music, musicSearchAliases(plan.music)) : null);
    const eventSounds = selected.eventSounds;
    for (const event of plan.sfx) {
      if (eventSounds.has(eventKey(event))) continue;
      const fallback = findBestSoundForEvent(sounds, event);
      if (fallback) eventSounds.set(eventKey(event), fallback);
    }

    AudioDiagnostics.log({
      action: 'scene-audio-selection',
      channel: 'system',
      level: (!ambience && plan.ambience) || (!music && plan.music) || eventSounds.size < plan.sfx.length ? 'warning' : 'success',
      message: `Catálogo selecionou ${eventSounds.size}/${plan.sfx.length} SFX, ambiência ${ambience?.name || 'nenhuma'}, clima ${selected.weather?.name || 'nenhum'} e música ${music?.name || 'nenhuma'}.`,
      details: {
        location: node.location,
        mapEvidence: canonicalAmbience.evidence,
        context: selected.context,
        ambienceQuery: plan.ambience,
        ambienceFile: ambience?.name || null,
        weatherFile: selected.weather?.name || null,
        distantAmbienceId: selected.distantAmbienceId || null,
        distantAmbienceFile: distantAmbience?.name || null,
        musicQuery: plan.music,
        musicFile: music?.name || null,
        barbara113: selected.barbaraPolicy || null,
        sfx: plan.sfx.map(event => ({
          id: event.id,
          phrase: event.matchedPhrase,
          charIndex: event.charIndex,
          selectedFile: eventSounds.get(eventKey(event))?.name || null,
        })),
      },
    });

    for (const event of plan.sfx) {
      const sound = eventSounds.get(eventKey(event));
      AudioDiagnostics.log({
        action: sound ? 'sfx-match-found' : 'sfx-match-missing',
        channel: 'sfx',
        level: sound ? 'success' : 'error',
        message: sound ? `SFX “${event.matchedPhrase}” vinculado a “${sound.name}”.` : `Nenhum áudio encontrado para “${event.matchedPhrase}”.`,
        phrase: event.matchedPhrase,
        searchTerms: [event.keyword, ...event.aliases],
        soundName: sound?.name,
        soundId: sound?.nativeId || sound?.id,
        plannedAtMs: event.charIndex,
        result: sound ? 'vinculado' : 'ausente',
        details: { semanticId: event.id, charIndex: event.charIndex, source: event.source, preferredSoundNames: event.preferredSoundNames },
      });
    }

    void AudioEngine.preloadSounds([...(distantAmbience ? [distantAmbience] : []), ...eventSounds.values()]).catch(error =>
      console.warn('[BRASEIRO_AUDIO] pré-carga de SFX:', error));
    return { sounds, rules, plan, context: selected.context, ambience, distantAmbience, weather: selected.weather, music, eventSounds };
  };

  const startPreparedSceneAudio = async (prepared: PreparedSceneAudio, expectedSessionId: string, runId: number) => {
    if (narrationRunRef.current !== runId || !AudioEngine.isNarrationSessionActive(expectedSessionId)) return;
    const startedAt = Date.now();
    AudioDiagnostics.log({
      action: 'scene-bed-start',
      channel: 'system',
      level: 'info',
      message: 'Iniciando as camadas independentes de ambiência, clima e música.',
      scheduledAtMs: startedAt,
      result: 'solicitado',
      details: { context: prepared.context },
    });
    const starts: Promise<unknown>[] = [];
    if (prepared.ambience) starts.push(AudioEngine.playLoop('ambience', prepared.ambience, undefined, 'base'));
    else AudioEngine.stopChannel('ambience', 'base');
    if (prepared.distantAmbience) starts.push(AudioEngine.playLoop('ambience', prepared.distantAmbience, undefined, 'distant'));
    else AudioEngine.stopChannel('ambience', 'distant');
    // Encerra os antigos slots de detalhes para não duplicar chuva ou ambiente.
    AudioEngine.stopChannel('ambience', 'detail0');
    AudioEngine.stopChannel('ambience', 'detail1');
    if (prepared.weather) starts.push(AudioEngine.playLoop('weather', prepared.weather, undefined, 'base'));
    else AudioEngine.stopChannel('weather', 'base');
    const manualMusicPinned = AudioEngine.isLoopActive('music', 'library-preview');
    if (manualMusicPinned) {
      AudioEngine.stopChannel('music', 'base');
      AudioDiagnostics.log({
        action: 'scene-music-preserved-manual',
        channel: 'music',
        level: 'info',
        message: 'Música escolhida manualmente foi preservada durante a mudança de cena.',
        slot: 'library-preview',
        result: 'preservada',
      });
    } else if (prepared.music) starts.push(AudioEngine.playLoop('music', prepared.music, undefined, 'base'));
    else AudioEngine.stopChannel('music', 'base');
    setAudioActive(Boolean(prepared.ambience || prepared.distantAmbience || prepared.weather || prepared.music));
    await Promise.allSettled(starts);
    if (narrationRunRef.current !== runId || !AudioEngine.isNarrationSessionActive(expectedSessionId)) return;
    AudioDiagnostics.log({
      action: 'scene-bed-ready',
      channel: 'system',
      level: 'success',
      message: 'Mesa sonora de cinco canais preparada.',
      scheduledAtMs: startedAt,
      actualAtMs: Date.now(),
      latencyMs: Date.now() - startedAt,
      result: 'pronta',
    });
  };

  const narratorSegment = (text: string): VoiceSegment => ({
    speakerType: 'narrator', speakerName: 'Narrador', text, emotion: 'contida',
  });
  const CHARON_NARRATOR_STYLE = 'narrador profundo, sábio e humano; atuação contida, calor constante, ritmo sereno e timbre idêntico durante toda a cena';

  const voiceRangesForText = (text: string, voiceScript: VoiceSegment[]) => {
    const ranges: Array<{ start: number; end: number; segment: VoiceSegment }> = [];
    let cursor = 0;
    for (const raw of voiceScript) {
      const segmentText = String(raw.text || '').trim();
      if (!segmentText) continue;
      const found = text.indexOf(segmentText, cursor);
      if (found < 0) continue;
      if (found > cursor && text.slice(cursor, found).trim()) ranges.push({ start: cursor, end: found, segment: narratorSegment(text.slice(cursor, found)) });
      ranges.push({ start: found, end: found + segmentText.length, segment: raw });
      cursor = found + segmentText.length;
    }
    if (cursor < text.length && text.slice(cursor).trim()) ranges.push({ start: cursor, end: text.length, segment: narratorSegment(text.slice(cursor)) });
    if (!ranges.length) ranges.push({ start: 0, end: text.length, segment: narratorSegment(text) });
    return ranges;
  };

  const splitRange = (text: string, start: number, end: number, maxChars = 680): Array<{ start: number; text: string }> => {
    const result: Array<{ start: number; text: string }> = [];
    let cursor = start;
    while (cursor < end) {
      while (cursor < end && /\s/.test(text[cursor])) cursor += 1;
      if (cursor >= end) break;
      let cut = Math.min(end, cursor + maxChars);
      if (cut < end) {
        const candidate = text.slice(cursor, cut);
        const preferred = [
          candidate.lastIndexOf('\n\n'),
          candidate.lastIndexOf('. '),
          candidate.lastIndexOf('! '),
          candidate.lastIndexOf('? '),
          candidate.lastIndexOf('… '),
          candidate.lastIndexOf('; '),
          candidate.lastIndexOf(': '),
          candidate.lastIndexOf(', '),
        ];
        const punctuation = Math.max(...preferred);
        if (punctuation >= 420) cut = cursor + punctuation + (candidate.slice(punctuation, punctuation + 2) === '\n\n' ? 2 : 1);
        else {
          const space = candidate.lastIndexOf(' ');
          if (space >= 420) cut = cursor + space;
        }
      }
      const raw = text.slice(cursor, cut);
      const trimmed = raw.replace(/\s+$/, '');
      if (trimmed) result.push({ start: cursor, text: trimmed });
      cursor = Math.max(cut, cursor + 1);
    }
    return result;
  };

  const buildNarrationUnits = (text: string, voiceScript: VoiceSegment[], events: SoundEffectEvent[]): NarrationUnit[] => {
    const ranges = voiceRangesForText(text, voiceScript);
    const units: NarrationUnit[] = [];
    for (const range of ranges) {
      for (const piece of splitRange(text, range.start, range.end, 680)) {
        const pieceEnd = piece.start + piece.text.length;
        units.push({
          ...piece,
          segment: range.segment,
          events: events.filter(event => event.charIndex >= piece.start && event.charIndex < pieceEnd),
        });
      }
    }
    return units.filter(unit => unit.text.trim());
  };

  const triggerPreparedEvents = (prepared: PreparedSceneAudio, events: SoundEffectEvent[], runId: number, expectedSessionId: string) => {
    for (const event of events) {
      const sound = prepared.eventSounds.get(eventKey(event));
      if (!sound || narrationRunRef.current !== runId || !AudioEngine.isNarrationSessionActive(expectedSessionId)) continue;
      const scheduledAtMs = Date.now();
      AudioDiagnostics.log({
        action: 'sfx-triggered',
        channel: 'sfx',
        level: 'info',
        message: `SFX sincronizado com “${event.matchedPhrase}”.`,
        phrase: event.matchedPhrase,
        searchTerms: [event.keyword, ...event.aliases],
        soundName: sound.name,
        soundId: sound.nativeId || sound.id,
        plannedAtMs: event.charIndex,
        scheduledAtMs,
        result: 'disparado',
        details: { semanticId: event.id, runId },
      });
      void AudioEngine.playSfx(sound, sound.name, {
        expectedSessionId,
        semanticId: event.id,
        eventId: `${runId}:${event.id}:${event.charIndex}`,
        priority: event.priority,
        phrase: event.matchedPhrase,
        searchTerms: [event.keyword, ...event.aliases],
        plannedAtMs: event.charIndex,
        scheduledAtMs,
      });
    }
  };

  const voiceForSegment = (segment: VoiceSegment): { voice: TtsVoiceName; style: string } => {
    if (segment.speakerType === 'narrator') return { voice: 'Charon', style: CHARON_NARRATOR_STYLE };
    const npc = segment.speakerId ? allNpcs.find(item => item.id === segment.speakerId) : allNpcs.find(item => normalizeText(item.name) === normalizeText(segment.speakerName));
    if (npc?.voiceProfile) return { voice: npc.voiceProfile, style: `voz estável de ${npc.name}; ${npc.speechStyle || npc.personality || npc.attitude || 'natural'}; emoção ${segment.emotion || npc.emotionalState || 'contida'}` };
    const voices: TtsVoiceName[] = ['Kore', 'Puck', 'Fenrir', 'Aoede', 'Gacrux', 'Algenib', 'Sulafat'];
    const key = segment.speakerId || segment.speakerName || 'personagem';
    const index = Array.from(key).reduce((sum, char) => sum + char.charCodeAt(0), 0) % voices.length;
    return { voice: voices[index], style: `voz distinta e consistente de ${segment.speakerName}; emoção ${segment.emotion || 'natural'}` };
  };

  const playCharonNarration = async (node: SceneNode) => {
    const text = node.atmospheric_narration || '';
    AudioDiagnostics.startSession('narracao-charon', { sceneId: node.id, location: node.location, textLength: text.length });
    if (!text.trim()) return;
    cancelNarrationPlayback();
    const audioSessionId = AudioEngine.beginNarrationSession(node.id, 'charon');
    const audioReadyPromise = AudioEngine.ready();
    const runId = narrationRunRef.current;
    let sessionStatus = 'finished';
    let narrationActive = true;
    setIsNarrationLoading(true); setNarrationError(null);
    try {
      // A voz começa com um bloco curto o bastante para reduzir a latência.
      // Enquanto esse bloco toca, o próximo é sintetizado em paralelo.
      // Todos os blocos usam Charon + o mesmo perfil fixo, preservando a voz,
      // mas uma resposta truncada da API não encerra silenciosamente a cena.
      const provisionalPlan = planSounds(node.location || '', text, node.playerAction || '', node.audioPlan, node.events || []);
      const provisionalUnits: NarrationUnit[] = splitRange(text, 0, text.length, 680).map(piece => {
        const end = piece.start + piece.text.length;
        return {
          ...piece,
          segment: narratorSegment(piece.text),
          events: provisionalPlan.sfx.filter(event => Number.isFinite(event.charIndex) && event.charIndex >= piece.start && event.charIndex < end),
        };
      });
      if (!provisionalUnits.length) return;
      let prepared: PreparedSceneAudio | null = null;
      let soundBedStarted = false;
      let voiceStarted = false;
      const startSoundBedWhenReady = () => {
        if (!prepared || soundBedStarted || !voiceStarted || !narrationActive || narrationRunRef.current !== runId) return;
        soundBedStarted = true;
        void startPreparedSceneAudio(prepared, audioSessionId, runId).catch(error => console.warn('[BRASEIRO_AUDIO] fundo da cena:', error));
      };
      const preparedPromise = prepareSceneAudio(node)
        .then(value => {
          if (narrationRunRef.current !== runId || !narrationActive) return null;
          prepared = value;
          startSoundBedWhenReady();
          return value;
        })
        .catch(error => {
          console.warn('[BRASEIRO_AUDIO] a voz continuará sem o catálogo da cena:', error);
          return null;
        });

      const firstProfile = voiceForSegment(provisionalUnits[0].segment);
      const firstSynthesisStartedAt = Date.now();
      const firstAudioPromise = generateNarration(provisionalUnits[0].text, firstProfile.voice, firstProfile.style);
      AudioDiagnostics.log({ action: 'charon-synthesis-request', channel: 'voice', level: 'info', message: `Solicitando a cena inteira ao Gemini TTS (${provisionalUnits[0].text.length} caracteres).`, backend: 'Gemini TTS', result: 'solicitado', details: { voice: firstProfile.voice, units: provisionalUnits.length, continuousSceneVoice: true } });
      const firstAudio = await firstAudioPromise;
      AudioDiagnostics.log({ action: 'charon-synthesis-ready', channel: 'voice', level: firstAudio ? 'success' : 'error', message: firstAudio ? 'Primeiro bloco do Gemini TTS recebido.' : 'Gemini TTS não devolveu áudio.', backend: 'Gemini TTS', actualAtMs: Date.now(), latencyMs: Date.now() - firstSynthesisStartedAt, result: firstAudio ? 'pronto' : 'vazio' });
      if (narrationRunRef.current !== runId) return;
      if (!firstAudio) throw new Error('O Gemini TTS não devolveu áudio.');
      await audioReadyPromise;
      let currentAudio: string | null = firstAudio;
      for (let index = 0; index < provisionalUnits.length; index += 1) {
        const unitEnd = provisionalUnits[index].start + provisionalUnits[index].text.length;
        const unit = prepared
          ? { ...provisionalUnits[index], events: prepared.plan.sfx.filter(event => Number.isFinite(event.charIndex) && event.charIndex >= provisionalUnits[index].start && event.charIndex < unitEnd) }
          : provisionalUnits[index];
        if (narrationRunRef.current !== runId) return;
        if (!currentAudio) throw new Error('O Gemini TTS não devolveu áudio.');
        const nextAudio = index + 1 < provisionalUnits.length
          ? generateNarration(
              provisionalUnits[index + 1].text,
              voiceForSegment(provisionalUnits[index + 1].segment).voice,
              voiceForSegment(provisionalUnits[index + 1].segment).style,
            )
          : Promise.resolve(null);
        await AudioEngine.playVoice(currentAudio, {
          onStarted: () => {
            voiceStarted = true;
            startSoundBedWhenReady();
          },
          timedCues: unit.events.map(event => ({
            id: eventKey(event),
            relativePosition: Math.max(0, Math.min(1, (event.charIndex - unit.start) / Math.max(1, unit.text.length))),
            // Se o catálogo ainda não estiver pronto, o efeito é descartado.
            // Reproduzi-lo depois da frase quebraria a sincronização.
            trigger: () => { if (prepared) triggerPreparedEvents(prepared, [event], runId, audioSessionId); },
          })),
        });
        currentAudio = await nextAudio;
        clearSfxTimers();
      }
      void preparedPromise;
    } catch (error) {
      sessionStatus = 'failed';
      AudioDiagnostics.log({ action: 'charon-narration-error', channel: 'voice', level: 'error', message: error instanceof Error ? error.message : 'Falha na narração.', backend: 'Gemini TTS/WebAudio', result: 'falha' });
      if (narrationRunRef.current === runId) setNarrationError(error instanceof Error ? error.message : 'Falha na narração.');
    } finally {
      narrationActive = false;
      AudioEngine.setNarrationDucking(false);
      AudioEngine.finishNarrationSession(narrationRunRef.current === runId ? sessionStatus : 'cancelled', audioSessionId);
      if (narrationRunRef.current === runId) setIsNarrationLoading(false);
    }
  };

  const playAndroidNarration = async (sceneNode?: SceneNode) => {
    const node = sceneNode || campaignRef.current?.history.at(-1);
    AudioDiagnostics.startSession('narracao-android', { sceneId: node?.id, location: node?.location, textLength: node?.atmospheric_narration?.length || 0 });
    const text = node?.atmospheric_narration || '';
    if (!node || !text.trim()) return;
    cancelNarrationPlayback();
    const audioSessionId = AudioEngine.beginNarrationSession(node.id, 'android-tts');
    const audioReadyPromise = AudioEngine.ready();
    const runId = narrationRunRef.current;
    let sessionStatus = 'finished';
    let narrationActive = true;
    setIsNarrationLoading(true); setNarrationError(null);
    try {
      let prepared: PreparedSceneAudio | null = null;
      let soundBedStarted = false;
      let voiceStarted = false;
      const startSoundBedWhenReady = () => {
        if (!prepared || soundBedStarted || !voiceStarted || !narrationActive || narrationRunRef.current !== runId) return;
        soundBedStarted = true;
        void startPreparedSceneAudio(prepared, audioSessionId, runId).catch(error => console.warn('[BRASEIRO_AUDIO] fundo da cena:', error));
      };
      void prepareSceneAudio(node).then(value => {
        if (!narrationActive || narrationRunRef.current !== runId) return;
        prepared = value;
        startSoundBedWhenReady();
      }).catch(error => console.warn('[BRASEIRO_AUDIO] voz Android continuará sem o catálogo:', error));
      const provisionalPlan = planSounds(node.location || '', text, node.playerAction || '', node.audioPlan, node.events || []);
      const entries = provisionalPlan.sfx.map((event, index) => ({ event, cueId: `${event.id}_${event.charIndex}_${index}` }));
      const byCue = new Map(entries.map(entry => [entry.cueId, entry.event]));
      const played = new Set<string>();
      const trigger = (cueId: string) => {
        if (played.has(cueId) || narrationRunRef.current !== runId) return;
        const event = byCue.get(cueId);
        if (!event || !prepared) return;
        played.add(cueId);
        triggerPreparedEvents(prepared, [event], runId, audioSessionId);
      };
      const triggerUntil = (position: number) => entries.forEach(entry => { if (entry.event.charIndex <= position) trigger(entry.cueId); });
      AudioEngine.setNarrationDucking(true);
      await audioReadyPromise;
      await speakWithAndroid(text, {
        cues: entries.map(entry => ({ id: entry.cueId, charIndex: entry.event.charIndex })),
        onStart: () => { voiceStarted = true; startSoundBedWhenReady(); triggerUntil(0); },
        onCue: cueId => trigger(cueId),
        onBoundary: index => triggerUntil(index),
      });
    } catch (error) {
      sessionStatus = 'failed';
      AudioDiagnostics.log({ action: 'android-narration-error', channel: 'voice', level: 'error', message: error instanceof Error ? error.message : 'A voz Android falhou.', backend: 'Android TextToSpeech', result: 'falha' });
      if (narrationRunRef.current === runId) setNarrationError(error instanceof Error ? error.message : 'A voz Android falhou.');
    } finally {
      narrationActive = false;
      clearSfxTimers(); AudioEngine.setNarrationDucking(false);
      AudioEngine.finishNarrationSession(narrationRunRef.current === runId ? sessionStatus : 'cancelled', audioSessionId);
      if (narrationRunRef.current === runId) setIsNarrationLoading(false);
    }
  };


  const handleRegenerateImage = async (nodeIndex?: number) => {
    const current = campaignRef.current;
    if (!current?.history.length || isImageLoading) return;
    const index = nodeIndex ?? current.history.length - 1;
    const node = current.history[index];
    setIsImageLoading(true); setImageError(null);
    try {
      const prompt = composeVisualPrompt(node.sceneVisualPrompt || node.visualPrompt);
      const generated = await generateSceneImage(prompt, imageQuality);
      if (!generated) throw new Error('O modelo não retornou imagem.');
      const image = await compressSceneImage(generated);
      const latest = campaignRef.current || current;
      const history = latest.history.map(item => item.id === node.id ? { ...item, imageData: image } : item);
      const updated = { ...latest, history, updatedAt: Date.now() };
      if (history.at(-1)?.id === node.id) setCurrentImage(image);
      await persistCampaign(updated, 'imagem de cena');
    } catch (error) { setImageError(error instanceof Error ? error.message : 'Não foi possível gerar esta imagem.'); }
    finally { setIsImageLoading(false); }
  };

  const handlePlayNarration = async (sceneNode?: SceneNode) => {
    const node = sceneNode || campaignRef.current?.history.at(-1);
    if (!node?.atmospheric_narration) return;
    const backend = localStorage.getItem('braseiro.narrationVoiceBackend') === 'kokoro' ? 'kokoro' : 'charon';
    if (backend === 'kokoro') await playAndroidNarration(node);
    else await playCharonNarration(node);
  };

  const handleStopNarration = () => {
    cancelNarrationPlayback();
    AudioEngine.stopChannel('sfx');
    AudioEngine.setNarrationDucking(false);
    setIsNarrationLoading(false);
  };

  const handleStopAll = () => {
    cancelNarrationPlayback(); AudioEngine.stopAll(); setIsNarrationLoading(false); setAudioActive(false);
  };

  const handleSendRollToMaster = async (roll: RollResult) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    let workingCampaign = base;
    let workingCharacters = characters;
    let workingNpcs = allNpcs;
    let audits: any[] = [];

    const ruinsLocation=/\bancient ruins\b|\bruinas antigas\b|\bamon guruthos\b/.test(String(master.position.locationName||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase());
    const ruinsFeatOne=roll.featResult===1||(Array.isArray(roll.featResults)&&roll.featResults.includes(1));
    const collapseHeroId=String(roll.characterId||base.activeCharacterId||'');
    if(ruinsLocation&&ruinsFeatOne&&collapseHeroId&&!master.company.loneLands?.ancientRuinsTunnelCollapsed){
      const collapse=applyMasterCommands(workingCampaign,workingCharacters,workingNpcs,[{type:'resolve_ancient_ruins_tunnel_collapse',targetCharacterId:collapseHeroId,featResult:1,rollId:roll.id,reason:'Dado de Façanha 1 rolado pelo Herói nas Ruínas Antigas.'}]);
      if(collapse.rejected.length)throw new Error(collapse.rejected.map(item=>item.reason).join(' | '));
      workingCampaign=collapse.campaign;workingCharacters=collapse.characters;workingNpcs=collapse.npcs;audits=collapse.audits;
    }

    const eyeRolled = roll.featResult === 11 || (Array.isArray(roll.featResults) && roll.featResults.includes(11));
    if (master.company.eyeAwarenessEnabled && master.activeMode !== 'combat' && eyeRolled) {
      const mechanical = applyMasterCommands(workingCampaign, workingCharacters, workingNpcs, [{
        type: 'adjust_eye_awareness',
        amount: 1,
        source: 'eye-roll',
        reason: '+1: uma rolagem do jogador fora de combate mostrou o Olho de Sauron.',
      }]);
      if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
      workingCampaign = mechanical.campaign;
      workingCharacters = mechanical.characters;
      workingNpcs = mechanical.npcs;
      audits = [...audits,...mechanical.audits];
    }

    const updated: CampaignState = {
      ...workingCampaign,
      pendingDiceRoll: base.pendingTest ? {
        notation: roll.notation,
        results: roll.results,
        modifier: roll.modifier,
        total: roll.total,
        timestamp: roll.timestamp,
        type: roll.type,
        featResult: roll.featResult,
        featResults: roll.featResults,
        rollMode: roll.rollMode,
        successResults: roll.successResults,
        targetNumber: roll.targetNumber,
        skillName: roll.skillName || workingCampaign.pendingTest?.skill,
        success: roll.success,
        successLevel: roll.successLevel,
        tengwarCount: roll.tengwarCount,
        magicalSuccess: roll.magicalSuccess,
        magicalSource: roll.magicalSource,
        characterId: roll.characterId || workingCampaign.activeCharacterId || undefined,
        characterName: roll.characterName || workingCampaign.characterSheet.identity.name,
      } : (base.pendingDiceRoll || null),
      updatedAt: Date.now(),
    };
    const summarized = attachResolutionSummary(base, updated, characters, workingCharacters, 'Rolar ' + String(roll.skillName || workingCampaign.pendingTest?.skill || 'dados'));
    const persisted = await saveMasterTransaction(summarized, workingCharacters, audits, 'resultado da rolagem aguardando confirmação', workingNpcs);
    campaignRef.current = persisted;
    setCampaign(persisted);
    setCharacters(workingCharacters);
    setAllNpcs(workingNpcs);
    setActiveTab('jogo');
    setIsStoryLoading(false);
    window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 80);
  };

  const handleResolvePendingRoll = async () => {
    let base = campaignRef.current;
    if (!base?.pendingTest || !base.pendingDiceRoll || isStoryLoading) return;
    const formal = normalizeMasterState(base);
    if (formal.activeMode === 'council' && (formal.council.status === 'introduction' || formal.council.status === 'interaction')) {
      const mechanical = applyMasterCommands(base, characters, allNpcs, [{
        type: 'record_council_result',
        success: Boolean(base.pendingDiceRoll.success),
        successIcons: Math.max(0, Number(base.pendingDiceRoll.tengwarCount) || 0),
        introduction: formal.council.status === 'introduction',
        reason: formal.council.status === 'introduction' ? 'Rolagem real da Introdução do Conselho.' : 'Rolagem real da Interação do Conselho.',
      }]);
      if (!mechanical.rejected.length) {
        const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'resultado mecânico do Conselho', mechanical.npcs);
        saveEmergencyCampaignMirror(committed);
        campaignRef.current = committed; setCampaign(committed); setCharacters(mechanical.characters); setAllNpcs(mechanical.npcs);
        base = committed;
      }
    }
    const originalChoice = String(base.pendingTest?.originalChoice || base.pendingTest?.reason || `Resolver teste de ${base.pendingTest?.skill || 'perícia'}`).trim();
    setStoryError(null);
    setIsStoryLoading(true);
    await processTurn(`[RESOLVER TESTE PENDENTE] ${originalChoice}`, false, base);
  };

  const handleClearPendingRoll = async () => {
    const base = campaignRef.current;
    if (!base) return;
    await persistCampaign({ ...base, pendingDiceRoll: null, updatedAt: Date.now() }, 'limpar rolagem');
  };

  const activateCharacter = async (characterId: string) => {
    const base = campaignRef.current;
    const selected = characters.find(character => character.id === characterId) || (await getCharacters()).find(character => character.id === characterId);
    if (!base || !selected) return;
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds || []), selected.id]));
    const allCharacters=[...characters.filter(character=>character.id!==selected.id),selected];
    const nextCharacters=applySoloCharacterRules(allCharacters,partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const nextSelected=nextCharacters.find(character=>character.id===selected.id)||selected;
    const updated = reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId: selected.id,
      partyCharacterIds,
      characterSheet: normalizeCharacterSheet(nextSelected.sheet),
      updatedAt: Date.now(),
    },nextCharacters,partyCharacterIds);
    const persisted=await saveCanonicalTransaction({campaign:updated,characters:nextCharacters,reason:'trocar personagem ativo'});
    campaignRef.current=persisted;setCampaign(persisted);setCharacters(nextCharacters);
  };

  const toggleCharacterInGame = async (characterId: string) => {
    const base = campaignRef.current;
    const list = characters.length ? characters : await getCharacters();
    const selected = list.find(character => character.id === characterId);
    if (!base || !selected) return;

    const validIds = new Set(list.map(character => character.id));
    const currentParty:string[] = Array.from(new Set<string>((base.partyCharacterIds || []).map(String).filter(id => validIds.has(id))));
    const isInGame = currentParty.includes(characterId);
    if (isInGame && currentParty.length <= 1) return;

    const partyCharacterIds = isInGame
      ? currentParty.filter(id => id !== characterId)
      : [...currentParty, characterId];
    let activeCharacterId = base.activeCharacterId;
    let activeSheet = base.characterSheet;

    if (!activeCharacterId || !partyCharacterIds.includes(activeCharacterId)) {
      activeCharacterId = partyCharacterIds[0] || characterId;
      const nextActive = list.find(character => character.id === activeCharacterId) || selected;
      activeSheet = normalizeCharacterSheet(nextActive.sheet);
    }

    const nextCharacters=applySoloCharacterRules(list,partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const normalizedActiveSheet=nextCharacters.find(character=>character.id===activeCharacterId)?.sheet||activeSheet;
    const updatedPartyCampaign=reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId,
      partyCharacterIds,
      characterSheet: normalizeCharacterSheet(normalizedActiveSheet),
      updatedAt: Date.now(),
    },nextCharacters,partyCharacterIds);
    const persisted=await saveCanonicalTransaction({campaign:updatedPartyCampaign,characters:nextCharacters,reason:isInGame?'retirar herói da companhia':'adicionar herói à companhia'});
    campaignRef.current=persisted;setCampaign(persisted);setCharacters(nextCharacters);
  };

  const handleSaveSheet = async (sheet: CharacterSheet) => {
    const base = campaignRef.current;
    if (!base) return;
    const activeId = base.activeCharacterId || characters[0]?.id;
    const normalized = normalizeCharacterSheet(sheet);
    const changedCharacters = [...characters];
    if (activeId) {
      const prior = characters.find(character => character.id === activeId);
      const updatedCharacter: Character = {
        id: activeId,
        name: normalized.identity.name || prior?.name || 'Herói',
        culture: normalized.identity.culture || prior?.culture || '',
        vocation: normalized.identity.vocation || prior?.vocation || '',
        sheet: normalized,
        createdAt: prior?.createdAt || Date.now(), updatedAt: Date.now(),
      };
      const index = changedCharacters.findIndex(character => character.id === activeId);
      if (index >= 0) changedCharacters[index] = updatedCharacter; else changedCharacters.push(updatedCharacter);
    }
    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, characterSheet: normalized, updatedAt: Date.now() },
      characters: changedCharacters,
      reason: 'editar ficha',
    });
    campaignRef.current = persisted;
    setCampaign(persisted);
    setCharacters(changedCharacters);
  };

  const createBlankCharacter = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const now = Date.now();
    const sheet = normalizeCharacterSheet({ ...structuredClone(DEFAULT_CHARACTER_SHEET), identity: { ...DEFAULT_CHARACTER_SHEET.identity, name: `Novo Herói ${characters.length + 1}` } });
    const character: Character = { id: `char_${now}_${Math.random().toString(36).slice(2, 7)}`, name: sheet.identity.name, culture: '', vocation: '', sheet, createdAt: now, updatedAt: now };
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds || []), character.id]));
    const list = applySoloCharacterRules([...characters, character],partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const activeCharacter=list.find(item=>item.id===character.id)||character;
    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, activeCharacterId: character.id, partyCharacterIds, characterSheet: activeCharacter.sheet, updatedAt: now },
      characters: list,
      reason: 'criar novo herói',
    });
    campaignRef.current = persisted; setCampaign(persisted); setCharacters(list); setActiveTab('personagens');
  };

  const createCompleteRandomCharacter = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const startingSolo=normalizeMasterState(base).company.rulesMode==='strider'&&(base.partyCharacterIds||[]).length===0;
    const character = createRandomCharacter(characters.map(item => item.sheet.identity.name || item.name), { rulesMode: startingSolo ? 'strider' : 'standard' });
    character.createdAt = Date.now(); character.updatedAt = Date.now();
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds || []), character.id]));
    const list = applySoloCharacterRules([...characters, character],partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const activeCharacter=list.find(item=>item.id===character.id)||character;
    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, activeCharacterId: character.id, partyCharacterIds, characterSheet: activeCharacter.sheet, updatedAt: Date.now() },
      characters: list,
      reason: 'criar herói completo',
    });
    campaignRef.current = persisted; setCampaign(persisted); setCharacters(list); setActiveTab('personagens');
  };

  const createCharacterPathCharacter = async (options:CharacterPathCreationOptions) => {
    const base=campaignRef.current;
    if(!base)return;
    const master=normalizeMasterState(base);
    const character=createCharacterFromPath({
      ...options,
      existingNames:characters.map(item=>item.sheet.identity.name||item.name),
      rulesMode:master.company.rulesMode==='strider'&&(base.partyCharacterIds||[]).length===0?'strider':'standard',
    });
    character.createdAt=Date.now();
    character.updatedAt=Date.now();
    const list=[...characters,character];
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds||[]),character.id]));
    const normalizedCharacters=applySoloCharacterRules(list,partyCharacterIds,master.company.rulesMode==='strider'?'strider':'standard');
    const activeCharacter=normalizedCharacters.find(item=>item.id===character.id)||character;
    const updatedCampaign=reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId:character.id,
      partyCharacterIds,
      characterSheet:activeCharacter.sheet,
      updatedAt:Date.now(),
    },normalizedCharacters,partyCharacterIds);
    const persisted=await saveCanonicalTransaction({
      campaign:updatedCampaign,
      characters:normalizedCharacters,
      reason:'criar herói por Trilhas de Personagens',
    });
    campaignRef.current=persisted;
    setCampaign(persisted);
    setCharacters(normalizedCharacters);
    setActiveTab('personagens');
  };

  const importRegistryCharacterJson = async (json: string) => {
    const base = campaignRef.current;
    if (!base) return;
    const character = importCharacterJson(json, characters.map(item => item.sheet.identity.name || item.name));
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds||[]),character.id]));
    const list=applySoloCharacterRules([...characters,character],partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const activeCharacter=list.find(item=>item.id===character.id)||character;
    const importedCampaign=reconcileCharacterPathPartyEffects({...base,activeCharacterId:character.id,partyCharacterIds,characterSheet:activeCharacter.sheet,updatedAt:Date.now()},list,partyCharacterIds);
    const persisted = await saveCanonicalTransaction({
      campaign: importedCampaign,
      characters: list,
      reason: 'importar ficha JSON validada',
    });
    campaignRef.current = persisted; setCampaign(persisted); setCharacters(list); setActiveTab('personagens');
  };

  const saveRegistryCharacter = async (character: Character, sheet: CharacterSheet) => {
    const base = campaignRef.current;
    if (!base) return;
    const updated = { ...character, name: sheet.identity.name || character.name, culture: sheet.identity.culture || character.culture, vocation: sheet.identity.vocation || character.vocation, sheet: normalizeCharacterSheet(sheet), updatedAt: Date.now() };
    const list = characters.map(item => item.id === character.id ? updated : item);
    const campaignUpdate = base.activeCharacterId === character.id ? { ...base, characterSheet: updated.sheet, updatedAt: Date.now() } : { ...base, updatedAt: Date.now() };
    const persisted = await saveCanonicalTransaction({ campaign: campaignUpdate, characters: list, reason: 'editar personagem do registro' });
    campaignRef.current = persisted; setCampaign(persisted); setCharacters(list);
  };

  const removeRegistryCharacter = async (characterId: string) => {
    const base = campaignRef.current;
    if (!base) return;
    const list = characters.filter(character => character.id !== characterId);
    if (!list.length) return;
    const validIds = new Set(list.map(character => character.id));
    let partyCharacterIds = (base.partyCharacterIds || []).filter(id => validIds.has(id));
    if (!partyCharacterIds.length) partyCharacterIds = [list[0].id];
    const activeCharacterId = base.activeCharacterId && validIds.has(base.activeCharacterId) ? base.activeCharacterId : partyCharacterIds[0];
    const normalizedList=applySoloCharacterRules(list,partyCharacterIds,normalizeMasterState(base).company.rulesMode==='strider'?'strider':'standard');
    const activeCharacter = normalizedList.find(character => character.id === activeCharacterId) || normalizedList[0];
    const finalParty=Array.from(new Set([activeCharacter.id,...partyCharacterIds]));
    const removedCampaign=reconcileCharacterPathPartyEffects({...base,activeCharacterId:activeCharacter.id,partyCharacterIds:finalParty,characterSheet:normalizeCharacterSheet(activeCharacter.sheet),updatedAt:Date.now()},normalizedList,finalParty);
    const persisted = await saveCanonicalTransaction({
      campaign: removedCampaign,
      characters: normalizedList,
      deleteCharacterIds: [characterId],
      reason: 'apagar personagem',
    });
    campaignRef.current = persisted; setCampaign(persisted); setCharacters(normalizedList);
  };

  const handleStartAdventure = async () => {
    if (isStoryLoading) return;
    setActiveTab('jogo');
    const activeCampaign = campaignRef.current;
    if (!activeCampaign) return;
    const discovery = await loadMapDiscovery(activeCampaign.id);
    if (!discovery.currentHexId) {
      setStoryError('Antes de começar, escolha no Atlas o hex onde a Companhia está. A aventura não será iniciada numa posição inventada.');
      document.getElementById('atlas-terra-media')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    setIsStoryLoading(true); setStoryError(null);
    const partySet = new Set(activeCampaign.partyCharacterIds || []);
    const party = characters.filter(character => partySet.has(character.id));
    const companyNames = party.map(character => character.sheet.identity.name || character.name);
    const companyText = companyNames.length > 1 ? ` A Companhia em jogo é formada por ${companyNames.join(', ')}.` : '';
    const profile = getAdventureProfile(adventureProfile);
    const architecture = chooseOpeningArchitecture(activeCampaign, party, allNpcs, adventureProfile);
    const openingClock = chooseOpeningClock(activeCampaign, architecture.id);
    const remembered = rememberOpening(activeCampaign, architecture.id);
    const rememberedMaster = normalizeMasterState(remembered);
    const sourceCampaign: CampaignState = {
      ...remembered,
      masterState: {
        ...rememberedMaster,
        worldClock: { ...rememberedMaster.worldClock, hour: openingClock.hour, minute: openingClock.minute },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };
    const canonicalOpeningLocation = sourceCampaign.masterState?.position.locationName || sourceCampaign.currentLocation || discovery.currentHexId;
    await processTurn(`[INICIAR AVENTURA GUIADA] A Companhia já está no hex ${discovery.currentHexId}; use exclusivamente o contexto cartográfico desse hex como ponto inicial.${companyText} O NOME CANÔNICO do lugar/região inicial é “${canonicalOpeningLocation}”: mencione-o naturalmente e NÃO o substitua por um tipo genérico de terreno. Perfil: ${profile.label}. ${profile.prompt} ${openingInstruction(architecture)} O horário inicial canônico é ${String(openingClock.hour).padStart(2, '0')}:${String(openingClock.minute).padStart(2, '0')} (${timePeriodFromHour(openingClock.hour)}). Não substitua esse horário por manhã por hábito.`, false, sourceCampaign);
  };

  const handleImportNpcPortrait = async (dataUrl: string, npcId: string) => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const latestNpcs = await getStoreData<Npc>('npcs');
    const target = latestNpcs.find(npc => npc.id === npcId);
    if (!target) throw new Error('O NPC solicitado não existe mais.');

    const blob = await sharedImageToBlob(dataUrl);
    const safeNpcId = npcId.replace(/[^A-Za-z0-9._-]+/g, '_');
    const tokenId = 'npc_portrait_' + safeNpcId;
    const previous = (await getStoreData<IndividualToken>('individual_tokens').catch(() => []))
      .find(token => token.id === tokenId);
    const token: IndividualToken = {
      id: tokenId,
      sheetId: 'npc:' + npcId,
      tokenIndex: 0,
      blob,
      category: 'npc-generated',
      createdAt: previous?.createdAt || Date.now(),
    };
    await saveStoreData('individual_tokens', token, 'retrato Gemini persistente de NPC');

    const updatedNpc: Npc = {
      ...target,
      tokenId,
      tokenAssignmentMode: 'manual',
      tokenMatchScore: undefined,
      tokenMatchReason: 'Retrato individual recebido do Gemini e vinculado ao ID persistente do NPC.',
      tokenDescriptorId: null,
      portraitUpdatedAt: Date.now(),
      updatedAt: Date.now(),
    };
    const updatedNpcs = latestNpcs.map(npc => npc.id === npcId ? updatedNpc : npc);
    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, updatedAt: Date.now() },
      npcs: updatedNpcs,
      reason: 'retrato persistente do NPC ' + target.name,
    });
    campaignRef.current = persisted;
    setCampaign(persisted);
    const verifiedToken = (await getStoreData<IndividualToken>('individual_tokens')).find(item => item.id === tokenId && item.blob?.size > 0);
    const verifiedNpc = (await getStoreData<Npc>('npcs')).find(item => item.id === npcId && item.tokenId === tokenId);
    if (!verifiedToken || !verifiedNpc) throw new Error('O retrato foi recebido, mas a verificação de persistência falhou. A imagem não foi descartada; tente importar novamente.');
    const verifiedNpcs = await getStoreData<Npc>('npcs');
    setAllNpcs(verifiedNpcs);
    setPresentNpcs(verifiedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
    window.dispatchEvent(new CustomEvent('braseiro-npc-portrait-updated', { detail: { npcId, tokenId, updatedAt: Date.now() } }));
    setImageImportStatus('Retrato de ' + target.name + ' recebido, salvo e vinculado ao NPC.');
  };

  const handleImportSceneImage = async (dataUrl: string, index = -1) => {
    const base = campaignRef.current;
    if (!base || !base.history.length) throw new Error('Crie uma cena antes de importar a imagem.');
    const safeIndex = index >= 0 ? Math.min(index, base.history.length - 1) : base.history.length - 1;
    const image = await compressSceneImage(dataUrl);
    const targetScene = base.history[safeIndex];
    const history = base.history.map((node, nodeIndex) => nodeIndex === safeIndex ? { ...node, imageData: image, imageAspect: '16:9' as const, imageImportedAt: Date.now(), imageFingerprint: sceneImageFingerprint(targetScene) } : node);
    await persistCampaign({ ...base, history, updatedAt: Date.now() }, 'importar imagem da cena');
    if (safeIndex === history.length - 1) setCurrentImage(image);
    setImageImportStatus('Imagem recebida e vinculada à cena atual.');
  };

  useEffect(() => {
    const importPending = async (provided?: string | null) => {
      if (sharedImageImportingRef.current) return;
      sharedImageImportingRef.current = true;
      try {
        const bridge = window.BraseiroAndroid;
        const peeked = bridge?.peekSharedImage ? await bridge.peekSharedImage().catch(() => null) : null;
        const consumed = !peeked && bridge?.consumeSharedImage ? await bridge.consumeSharedImage().catch(() => null) : null;
        const dataUrl = provided || peeked || consumed;
        if (!isImportableSharedImage(dataUrl)) return;
        const active = campaignRef.current;
        // A Activity pode receber a imagem antes de o IndexedDB terminar de
        // restaurar a campanha. Nesse caso o bridge conserva a imagem e este
        // efeito tenta novamente quando campaign.id estiver disponível.
        if (!active?.history.length) return;
        setActiveTab('jogo');
        let targetIndex = -1;
        let npcTargetId = '';
        try {
          const nativeTarget = bridge?.getSharedImageTarget ? await bridge.getSharedImageTarget().catch(() => null) : null;
          const target = JSON.parse(nativeTarget || localStorage.getItem('braseiro.pendingSharedImageTarget') || 'null') as {
            kind?: 'scene' | 'npc'; campaignId?: string; sceneId?: string; npcId?: string; fingerprint?: string; createdAt?: number;
          } | null;
          if (target?.campaignId === active.id && target.kind === 'npc' && target.npcId) {
            npcTargetId = target.npcId;
          } else if (target?.campaignId === active.id && target.sceneId) {
            const found = active.history.findIndex(node => node.id === target.sceneId);
            if (found >= 0 && targetMatchesScene(target, active.id, active.history[found])) targetIndex = found;
          }
        } catch { targetIndex = -1; npcTargetId = ''; }
        if (npcTargetId) await handleImportNpcPortrait(dataUrl, npcTargetId);
        else {
          if (targetIndex < 0) throw new Error('A imagem recebida não pertence à cena ou ao NPC solicitado, ou o pedido expirou. Gere novamente a partir do destino correto.');
          await handleImportSceneImage(dataUrl, targetIndex);
        }
        await bridge?.clearSharedImage?.().catch(() => undefined);
        await bridge?.clearSharedImageTarget?.().catch(() => undefined);
        localStorage.removeItem('braseiro.pendingSharedImageTarget');
      } catch (error) {
        setImageImportStatus(error instanceof Error ? error.message : 'Não foi possível anexar a imagem recebida.');
      } finally { sharedImageImportingRef.current = false; }
    };
    const receiveSharedImage = (event: Event) => void importPending((event as CustomEvent<string>).detail);
    const receiveSharedImageError = (event: Event) => {
      const message = String((event as CustomEvent<string>).detail || 'Não foi possível importar a imagem compartilhada.');
      setImageImportStatus(message);
    };
    const checkPending = () => void importPending();
    const onVisibility = () => { if (document.visibilityState === 'visible') checkPending(); };
    window.addEventListener('braseiro-shared-image', receiveSharedImage as EventListener);
    window.addEventListener('braseiro-shared-image-error', receiveSharedImageError as EventListener);
    window.addEventListener('focus', checkPending);
    window.addEventListener('pageshow', checkPending);
    window.addEventListener('braseiro-native-ready', checkPending);
    document.addEventListener('visibilitychange', onVisibility);
    checkPending();
    return () => {
      window.removeEventListener('braseiro-shared-image', receiveSharedImage as EventListener);
      window.removeEventListener('braseiro-shared-image-error', receiveSharedImageError as EventListener);
      window.removeEventListener('focus', checkPending);
      window.removeEventListener('pageshow', checkPending);
      window.removeEventListener('braseiro-native-ready', checkPending);
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [campaign?.id]);

  const handleRestoreCampaign = async (restored: CampaignState) => {
    const migrationSource = { campaignSchema: Number(restored.schemaVersion || 0), masterSchema: Number(restored.masterState?.schemaVersion || 0) };
    const migrationRequired = needsMigration300(restored);
    const safe = normalizeCampaignState(restored);
    const [restoredCharacters, restoredNpcs, discovery] = await Promise.all([
      getCharacters(), getStoreData<Npc>('npcs'), loadMapDiscovery(safe.id),
    ]);
    if (migrationRequired) {
      await createCampaignSnapshot(safe, 'antes de migrar backup restaurado para 3.0.0');
      const migrated = migrateCampaignTo300(safe, restoredCharacters, restoredNpcs, discovery, migrationSource);
      const atlasReconciled = await reconcileCampaignLocationWithAtlas(migrated.campaign, migrated.discovery);
      const committed = await saveMasterTransaction(atlasReconciled, migrated.characters, [], 'restaurar e migrar backup 3.0.0', migrated.npcs, migrated.discovery);
      await saveStoreData('migration_reports', migrated.report, 'relatório de migração do backup');
      campaignRef.current = committed;
      setCampaign(committed);
      setCharacters(migrated.characters);
      setAllNpcs(migrated.npcs);
      setCurrentImage(committed.history.at(-1)?.imageData || null);
    } else {
      const reconciled = reconcileCanonicalState(safe, restoredCharacters, restoredNpcs, discovery, { mapWins: true, preservePresenceUnion: true });
      const atlasReconciled = await reconcileCampaignLocationWithAtlas(reconciled.campaign, reconciled.discovery);
      const committed = await saveMasterTransaction(atlasReconciled, reconciled.characters, [], 'restaurar backup 3.0.0', reconciled.npcs, reconciled.discovery);
      campaignRef.current = committed;
      setCampaign(committed);
      setCharacters(reconciled.characters);
      setAllNpcs(reconciled.npcs);
      setCurrentImage(committed.history.at(-1)?.imageData || null);
    }
    await reloadNpcs();
  };

  const handleNpcTokenChange = async (npcId: string, tokenId: string | null) => {
    const base = campaignRef.current;
    if (!base) return;
    const latestNpcs = allNpcs.length ? allNpcs : await getStoreData<Npc>('npcs');
    const target = latestNpcs.find(npc => npc.id === npcId);
    if (!target) throw new Error('NPC não encontrado para substituir o retrato.');
    const updatedNpc: Npc = {
      ...target,
      tokenId,
      tokenAssignmentMode: 'manual',
      tokenMatchScore: undefined,
      tokenMatchReason: tokenId ? 'Retrato escolhido manualmente na tela da cena.' : 'Retrato removido manualmente na tela da cena.',
      tokenDescriptorId: null,
      updatedAt: Date.now(),
    };
    const updatedNpcs = latestNpcs.map(npc => npc.id === npcId ? updatedNpc : npc);
    const persisted = await saveCanonicalTransaction({ campaign: { ...base, updatedAt: Date.now() }, npcs: updatedNpcs, reason: 'substituir retrato de NPC na cena' });
    campaignRef.current = persisted;
    setCampaign(persisted);
    setAllNpcs(updatedNpcs);
    setPresentNpcs(updatedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
  };

  const handleResetCampaign = async () => {
    if (!confirm('Criar uma nova crônica? A campanha atual continuará protegida pelos snapshots locais e poderá ser exportada antes.')) return;
    const fresh = newCampaign();
    await persistCampaign(fresh, 'nova crônica');
    setCurrentImage(null); setCustomAction(''); setActiveTab('jogo');
  };

  if (bootError) {
    return (
      <div className="min-h-[100dvh] bg-[#040810] p-6 text-slate-100 flex items-center justify-center">
        <div className="medieval-panel max-w-lg p-6 text-center">
          <h1 className="medieval-title text-xl">O banco local não abriu</h1>
          <p className="mt-3 text-sm text-slate-300">{bootError}</p>
          <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-center">
            <button onClick={() => void initialize()} className="rounded border border-blue-500 bg-blue-950 px-4 py-2">Tentar novamente</button>
            <button onClick={async () => {
              const active = await getActiveCampaign().catch(() => null);
              const snapshot = active ? await recoverLatestCampaignSnapshot(active.id) : null;
              if (snapshot) await handleRestoreCampaign(snapshot);
            }} className="rounded border border-slate-600 px-4 py-2">Recuperar último snapshot</button>
          </div>
        </div>
      </div>
    );
  }

  if (!campaign) return <div className="min-h-screen bg-[#040810] flex items-center justify-center text-blue-200">Abrindo a crônica…</div>;

  const immersiveJourneyMode = activeTab === 'jogo' && immersiveJourneyUiEnabled();

  const renderActiveView = () => {
    const configuredPartyCharacters = characters.filter(character => (campaign.partyCharacterIds || []).includes(character.id));
    const partyCharacters = configuredPartyCharacters.length ? configuredPartyCharacters : characters;
    const sceneNpcs = allNpcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id) || npc.presentInScene);
    const masterState = normalizeMasterState(campaign);
    switch (activeTab) {
      case 'jogo': return (
        <>
          <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={isStoryLoading} isImageLoading={isImageLoading} isNarrationLoading={isNarrationLoading} storyError={storyError} onDismissStoryError={() => setStoryError(null)} imageError={imageError} narrationError={narrationError} customAction={customAction} setCustomAction={setCustomAction} onChoice={handleChoice} onCustomActionSubmit={handleCustomActionSubmit} onRegenerateImage={handleRegenerateImage} onImportSceneImage={handleImportSceneImage} onStartAdventure={handleStartAdventure} canStartAdventure={Boolean(normalizeMasterState(campaign).position.currentHexId)} adventureProfile={adventureProfile} onAdventureProfileChange={setAdventureProfile} imageImportStatus={imageImportStatus} onCreateRandomCharacter={createCompleteRandomCharacter} onPlayNarration={handlePlayNarration} onPlayAndroidNarration={playAndroidNarration} onStopNarration={handleStopNarration} partyCharacterIds={partyCharacters.map(character => character.id)} partyCharacters={partyCharacters} activeCharacterId={campaign.activeCharacterId} onActiveCharacterChange={activateCharacter} onCompanySave={handleCompanySave} onRunCommands={handleWorldCommands} pendingTest={campaign.pendingTest} pendingRoll={campaign.pendingDiceRoll} onOpenDice={() => setActiveTab('dados')} onResolvePendingRoll={handleResolvePendingRoll} onNpcTokenChange={handleNpcTokenChange} allNpcs={allNpcs} presentNpcs={allNpcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id))} masterState={normalizeMasterState(campaign)} ruleAnswer={ruleAnswer} onCloseRuleAnswer={() => setRuleAnswer(null)} onPartyMoved={handlePartyMoved} onMapDiscoveryChanged={handleMapDiscoveryChanged} onPartyRested={handlePartyRested} immersiveMode={immersiveJourneyMode} onNavigate={tab => { setActiveTab(tab); AudioEngine.init(); }} />
          <MechanicalStatusStrip masterState={masterState} characters={partyCharacters} activeCharacterId={campaign.activeCharacterId} immersive={immersiveJourneyMode} />
          <JourneyResolutionPanel
            masterState={masterState}
            characters={partyCharacters}
            busy={isStoryLoading}
            onSetEyeEnabled={handleSetEyeAwarenessEnabled}
            onStartJourney={handleJourneyStartManual}
            onRollJourney={handleJourneyRollManual}
            onApplyMarch={handleApplyJourneyMarch}
            onPremonitionJourney={handlePremonitionJourney}
            onGenerateEvent={handleGenerateJourneyEventManual}
            onSelectEventTarget={handleChooseJourneyEventTarget}
            onApplyEvent={handleApplyJourneyEvent}
            onResolveForodwaith={handleResolveForodwaithSpecial}
            onResolveNotableEvent={handleResolveJourneyNotable}
            onOpenNotableEvent={handleOpenJourneyNotable}
            onContinueStage={handleContinueJourneyStage}
            onCompleteJourney={handleCompleteJourneyManual}
            onHerbalRecovery={handleJourneyHerbalRecovery}
            onResolveHerbalRisk={handleResolveJourneyHerbalRisk}
            onOpenEncounter={handleOpenJourneyEncounter}
            onOpenEyeRevelation={handleOpenEyeRevelation}
            onAdjustEye={handleAdjustEyeAwareness}
            onSetEyeHuntModifiers={handleSetEyeHuntModifiers}
          />
        </>
      );
      case 'mestre': return <MasterHubView masterState={masterState} characters={partyCharacters} presentNpcs={sceneNpcs} activeCharacterId={campaign.activeCharacterId} onActiveCharacterChange={activateCharacter} onCompanySave={handleCompanySave} onRunCommands={handleWorldCommands} ruleAnswer={ruleAnswer} onCloseRuleAnswer={() => setRuleAnswer(null)} onAskRules={handleAskMasterRules} activeSources={activeSources} busy={isStoryLoading} />;
      case 'combate': return <CombatTacticalView masterState={masterState} characters={partyCharacters} npcs={sceneNpcs} onApplyCommands={handleWorldCommands} onAskMasterAction={handleCombatMasterAction} onOpenDice={() => setActiveTab('dados')} />;
      case 'companhia': return <><CompanyHubView masterState={masterState} characters={partyCharacters} onSave={handleCompanySave} /><div className="mt-4"><MechanicsHub masterState={masterState} characters={partyCharacters} npcs={allNpcs} onApplyCommands={handleWorldCommands} /></div></>;
      case 'mundo': return <WorldAliveView masterState={masterState} characters={characters} npcs={allNpcs} onApplyCommands={handleWorldCommands} />;
      case 'poderes': return <OrganizationsView masterState={masterState} characters={partyCharacters} npcs={allNpcs} onApplyCommands={handleWorldCommands} />;
      case 'ficha': return <CharacterSheetView sheet={campaign.characterSheet} onSaveSheet={handleSaveSheet} />;
      case 'personagens': return <><CharacterPathsCreator rulesMode={masterState.company.rulesMode==='strider'&&(campaign.partyCharacterIds||[]).length===0?'strider':'standard'} onCreate={createCharacterPathCharacter}/><CharacterRegistryView characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onSelectCharacter={activateCharacter} onToggleCharacterInGame={toggleCharacterInGame} onCreateRandom={createCompleteRandomCharacter} onCreateBlank={createBlankCharacter} onSaveCharacter={saveRegistryCharacter} onDeleteCharacter={removeRegistryCharacter} onImportCharacterJson={importRegistryCharacterJson} /></>;
      case 'dados': return <DiceView onSendRollToMaster={handleSendRollToMaster} pendingRoll={campaign.pendingDiceRoll} onClearPendingRoll={handleClearPendingRoll} pendingTest={campaign.pendingTest} characters={characters.filter(character => (campaign.partyCharacterIds || []).includes(character.id))} activeCharacterId={campaign.activeCharacterId} onActiveCharacterChange={activateCharacter} onSpendHope={handleSpendHope} masterState={masterState} />;
      case 'npcs': return <NpcRegistryView onNpcChange={() => reloadNpcs(true)} />;
      case 'sons': return <SoundLibraryView onAudioChange={() => setAudioActive(true)} audioActive={audioActive} />;
      case 'manuais': return <ManualLibraryView campaign={campaign} onManualsChange={() => undefined} activeSources={activeSources} />;
      case 'campanha': return <CampaignView campaign={campaign} onRestoreCampaign={handleRestoreCampaign} onResetCampaign={handleResetCampaign} />;
      case 'configuracoes': return <SettingsView atmosphereActive={atmosphereActive} onAtmosphereChange={setAtmosphereActive} imageQuality={imageQuality} onImageQualityChange={quality => { setImageQuality(quality); localStorage.setItem('braseiro.imageQuality', quality); }} healthReport={healthReport} isHealthChecking={isHealthChecking} onRunHealthCheck={runHealthCheck} onOpenBackup={() => setActiveTab('campanha')} />;
      case 'tokens': return <TokensView />;
      default: return <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={false} isImageLoading={false} isNarrationLoading={false} storyError={null} imageError={null} narrationError={null} customAction={customAction} setCustomAction={setCustomAction} onChoice={handleChoice} onCustomActionSubmit={handleCustomActionSubmit} onRegenerateImage={handleRegenerateImage} onImportSceneImage={handleImportSceneImage} onStartAdventure={handleStartAdventure} canStartAdventure={Boolean(normalizeMasterState(campaign).position.currentHexId)} adventureProfile={adventureProfile} onAdventureProfileChange={setAdventureProfile} imageImportStatus={imageImportStatus} onCreateRandomCharacter={createCompleteRandomCharacter} onPlayNarration={handlePlayNarration} onPlayAndroidNarration={playAndroidNarration} onStopNarration={handleStopNarration} partyCharacterIds={partyCharacters.map(character => character.id)} partyCharacters={partyCharacters} activeCharacterId={campaign.activeCharacterId} onActiveCharacterChange={activateCharacter} onCompanySave={handleCompanySave} onRunCommands={handleWorldCommands} pendingTest={campaign.pendingTest} pendingRoll={campaign.pendingDiceRoll} onOpenDice={() => setActiveTab('dados')} onResolvePendingRoll={handleResolvePendingRoll} onNpcTokenChange={handleNpcTokenChange} allNpcs={allNpcs} presentNpcs={allNpcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id))} masterState={normalizeMasterState(campaign)} ruleAnswer={ruleAnswer} onCloseRuleAnswer={() => setRuleAnswer(null)} onPartyMoved={handlePartyMoved} onMapDiscoveryChanged={handleMapDiscoveryChanged} onPartyRested={handlePartyRested} immersiveMode={immersiveJourneyMode} onNavigate={tab => { setActiveTab(tab); AudioEngine.init(); }} />;
    }
  };

  return (
    <div className="min-h-screen bg-[#040810] text-slate-100 flex flex-col relative font-serif selection:bg-blue-950 selection:text-blue-100">
      {atmosphereActive && <div className="fixed inset-0 medieval-background pointer-events-none" />}
      {!immersiveJourneyMode && <div className="sticky top-0 z-50 app-sticky-shell">
        <AppHeader characterSheet={campaign.characterSheet} characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onActiveCharacterChange={activateCharacter} locationName={campaign.currentLocation} weather={campaign.history.at(-1)?.weather} timeLabel={`${timePeriodFromHour(normalizeMasterState(campaign).worldClock.hour)} ${String(normalizeMasterState(campaign).worldClock.hour).padStart(2, '0')}:${String(normalizeMasterState(campaign).worldClock.minute).padStart(2, '0')}`} onStopAll={handleStopAll} onOpenAudio={() => setQuickAudioOpen(true)} audioActive={audioActive} onGoToSheet={() => setActiveTab('ficha')} />
        <MainNavigation activeTab={activeTab} onTabChange={tab => { setActiveTab(tab); AudioEngine.init(); }} />
      </div>}
      {campaign.pendingTest && activeTab !== 'jogo' && (
        <button onClick={() => { setActiveTab('jogo'); window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 60); }} className="mx-auto mt-3 w-[calc(100%-2rem)] max-w-7xl rounded border border-cyan-600 bg-cyan-950/80 px-4 py-3 text-left text-sm text-cyan-100 z-30">
          <strong>{campaign.pendingDiceRoll ? `Resultado pronto: ${campaign.pendingDiceRoll.success ? 'sucesso' : 'falha'} em ${campaign.pendingTest.skill}` : `Ação aguardando teste: ${campaign.pendingTest.skill}`}</strong> — toque para voltar ao bloco da ação.
        </button>
      )}
      <main className={immersiveJourneyMode ? 'fixed inset-0 z-20 h-[100dvh] w-full overflow-hidden p-0' : 'flex-1 max-w-7xl w-full mx-auto px-3 sm:px-4 py-5 z-20 relative'}>{renderActiveView()}</main>
      <QuickAudioPanel open={quickAudioOpen} onClose={() => setQuickAudioOpen(false)} onStopAll={handleStopAll} />
    </div>
  );
}
