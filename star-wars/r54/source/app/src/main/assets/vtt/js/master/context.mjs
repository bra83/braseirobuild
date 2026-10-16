import { RULE_CORPUS_VERSION } from '../rag/lexicalSearch.mjs';
import { searchGroundedSources } from '../rag/sourceSearch.mjs';
import { FIVE_PUBLISHED_ADVENTURE_IDS, hybridDecision } from '../adventure/hybridFiveAdventurePolicy.mjs';
import { getHealthStatus } from '../rules/health.mjs';
import { adversaryHealthStatus } from '../rules/adversaries.mjs';
import { masterWorldSummary, causalEncounterCandidates } from '../world/world.mjs';
import { sandboxOpening, sandboxSummary } from '../world/sandbox.mjs';
import { economySummary } from '../rules/economy.mjs';

function hasMasterNarrator(state) {
  if (state.settings?.masterProvider === 'http') return true;
  const bridge = globalThis.BraseiroGemini;
  if (!bridge || typeof bridge.getStatus !== 'function' || typeof bridge.requestMasterProposal !== 'function') return false;
  try {
    const raw = bridge.getStatus();
    const status = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return status?.configured === true;
  } catch { return false; }
}

function characterSummary(character) {
  if (!character) return null;
  const health=getHealthStatus(character);
  return {
    id:character.id ?? null,
    name:character.name,
    species:character.species ?? null,
    profession:character.profession ?? null,
    health:{label:health.label,wounds:structuredClone(character.derived?.wounds),strain:structuredClone(character.derived?.strain),soak:character.derived?.soak},
    criticalCount:character.criticalInjuries?.length ?? 0,
    credits:character.credits ?? 0
  };
}

function combatSummary(combat) {
  if (!combat?.active) return {active:false};
  return {
    active:true,
    round:combat.round,
    playerTurn:structuredClone(combat.playerTurn),
    encounter:combat.encounter ? {cursor:combat.encounter.cursor,waitingForPlayer:combat.encounter.waitingForPlayer,outcome:combat.encounter.outcome,slots:structuredClone(combat.encounter.slots)} : null,
    causalHookId:combat.causalHookId ?? null,
    causalHookSummary:combat.causalHookSummary ?? null,
    adversaries:(combat.adversaries ?? []).map(a=>({id:a.id,name:a.name,kind:a.kind,range:a.range,factionId:a.factionId??null,worldNpcId:a.worldNpcId??null,health:adversaryHealthStatus(a).label,wounds:structuredClone(a.wounds)}))
  };
}

export function buildMasterContext(state,{intentText='',ruleQuery=intentText,maxRules=5}={}) {
  const searched=searchGroundedSources(ruleQuery,{limit:maxRules,includePending:true});
  const rules=searched.results;
  const retrieval=searched.retrieval;
  const world=masterWorldSummary(state.world);
  const sandbox=sandboxSummary(state.world);
  const opening=sandboxOpening(state.world,{locationId:state.scene?.locationId??null,limit:12});
  const causalEncounters=causalEncounterCandidates(state.world,state.scene?.locationId,{limit:12});
  return {
    contextVersion:8,
    rulesProfile:state.rulesProfile,
    campaignMode:state.campaignMode??'EOTE',
    corpusVersion:RULE_CORPUS_VERSION,
    hybrid: {policyVersion:1,adventureScope:[...FIVE_PUBLISHED_ADVENTURE_IDS],mechanicsAuthority:'DETERMINISTIC_CODE_ONLY',ragAuthority:'REFERENCE_ONLY',geminiAuthority:'NARRATIVE_PROPOSAL_ONLY',reference:hybridDecision({purpose:'reference',sources:rules}),narrative:hybridDecision({purpose:'narrative',providerAvailable:hasMasterNarrator(state)})},
    campaign:{id:state.campaignId,name:state.name,sessionId:state.sessionId},
    session:state.session ? {number:state.session.number,status:state.session.status,phase:state.session.phase,startedAt:state.session.startedAt,checksResolved:state.session.checksResolved,combatEncounters:state.session.combatEncounters} : null,
    scene:{title:state.scene?.title ?? '',locationId:state.scene?.locationId ?? null,narration:state.scene?.narration ?? '',lastPlayerIntent:state.scene?.lastPlayerIntent ?? null},
    player:characterSummary(state.player?.character),
    pendingAcquisition:state.master?.pendingAcquisition ? structuredClone(state.master.pendingAcquisition) : null,
    pendingEncounter:state.master?.pendingEncounter ? structuredClone(state.master.pendingEncounter) : null,
    combat:combatSummary(state.combat),
    world,
    sandbox,
    opening,
    livingWorld:opening.living ?? null,
    economy:economySummary(state,{market:opening.market??null}),
    causalEncounters,
    recentEvents:(state.events ?? []).slice(-12).map(e=>({id:e.id,type:e.type,at:e.at,payload:e.payload})),
    request:{intentText:String(intentText ?? ''),ruleQuery:String(ruleQuery ?? '')},
    retrieval,
    rules
  };
}

export function contextDigest(context) {
  return {
    contextVersion:context.contextVersion,
    corpusVersion:context.corpusVersion,
    hybridPolicyVersion:context.hybrid?.policyVersion??null,
    publishedAdventureCount:context.hybrid?.adventureScope?.length??0,
    grounded:context.retrieval.grounded,
    ruleIds:context.rules.map(r=>r.id),
    sceneTitle:context.scene.title,
    combatActive:context.combat.active,
    playerName:context.player?.name ?? null,
    sessionNumber:context.session?.number ?? null,
    sessionPhase:context.session?.phase ?? null,
    pendingAcquisitionId:context.pendingAcquisition?.requestId ?? null,
    pendingEncounter:Boolean(context.pendingEncounter),
    factionCount:context.world?.factions?.length ?? 0,
    pendingWorldConsequences:context.world?.hookCounts?.pending ?? 0,
    activeAftermaths:context.world?.aftermathCounts?.active ?? 0,
    recentWorldEncounters:context.world?.recentEncounters?.length ?? 0,
    campaignMode:context.campaignMode??'EOTE',
    sandboxReady:Boolean(context.opening?.ready),
    activeOpportunities:context.sandbox?.activeOpportunityCount??0,
    activeWorldClocks:context.sandbox?.activeClockCount??0,
    livingWorldDay:context.livingWorld?.time?.day??null,
    livingWorldPeriod:context.livingWorld?.time?.period??null,
    livingWorldNpcCount:context.livingWorld?.npcs?.length??0,
    ownedAssetCount:context.economy?.ownedAssets?.length??0,
    marketAvailable:Boolean(context.economy?.market)
  };
}
