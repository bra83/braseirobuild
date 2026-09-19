import type { TestRequirement } from '../../types';

export type AdventureMode = 'adventure' | 'journey' | 'council' | 'combat' | 'fellowship';
export type KnowledgeVisibility = 'master' | 'company' | 'hero';
export type KnowledgeKind = 'truth' | 'rumor' | 'lie' | 'clue' | 'interpretation';
export type JourneyRole = 'guide' | 'scout' | 'hunter' | 'lookout';
export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward';

export type SceneKind = 'exploration' | 'dialogue' | 'journey' | 'council' | 'combat' | 'rest' | 'fellowship';
export type SceneStatus = 'idle' | 'active' | 'resolving' | 'closed';

export interface SceneState {
  id: string;
  kind: SceneKind;
  status: SceneStatus;
  title: string;
  objective: string;
  locationName: string;
  participantHeroIds: string[];
  participantNpcIds: string[];
  spotlightCharacterId: string | null;
  spotlightQueue: string[];
  pendingActions: Record<string, string>;
  tension: number;
  endCondition: string;
  startedAt: number;
  updatedAt: number;
}

export interface SessionDirectorState {
  sessionObjective: string;
  pacing: 'quiet' | 'rising' | 'tense' | 'climax' | 'aftermath';
  spotlightDebtByHero: Record<string, number>;
  sceneCountByHero: Record<string, number>;
  lastActedAtByHero: Record<string, number>;
  nextSuggestedHeroId: string | null;
  openBeats: Array<{ id: string; label: string; status: 'open' | 'resolved'; updatedAt: number }>;
  updatedAt: number;
}

export interface NpcDraft {
  id?: string;
  name: string;
  culture?: string;
  peoples?: string;
  role?: string;
  faction?: string;
  appearance?: string;
  personality?: string;
  attitude?: string;
  objectives?: string;
  fears?: string;
  secrets?: string;
  notes?: string;
  trust?: number;
  location?: string;
  state?: string;
  speechStyle?: string;
  emotionalState?: string;
  nextAction?: string;
  lifeStatus?: 'alive' | 'missing' | 'dead';
  wealthIndex?: number;
  commitments?: Array<{ id: string; title: string; dueDay?: number; status: 'open' | 'fulfilled' | 'failed'; updatedAt: number }>;
  voiceProfile?: string;
  presentInScene?: boolean;
}

export interface PartyPosition {
  currentHexId: string | null;
  locationName: string;
  routeHexIds: string[];
  lastMovedAt?: number;
}

export interface WorldClock {
  year: number;
  season: 'spring' | 'summer' | 'autumn' | 'winter';
  day: number;
  hour: number;
  minute: number;
  elapsedMinutes: number;
}

export interface CompanyState {
  fellowshipCurrent: number;
  fellowshipMax: number;
  patron: string;
  sanctuaryIds: string[];
  eyeAwareness: number;
  journeyRoles: Partial<Record<JourneyRole, string>>;
  fellowshipFocus: Record<string, string>;
  spotlightByHero: Record<string, number>;
}

export interface JourneySegment {
  id: string;
  fromHexId: string | null;
  toHexId: string;
  distanceKm?: number;
  terrain?: string;
  startedAt: number;
  completedAt: number;
}

export interface JourneyState {
  status: 'idle' | 'planned' | 'travelling' | 'paused' | 'completed';
  originHexId: string | null;
  destinationHexId: string | null;
  startedAt?: number;
  day: number;
  segments: JourneySegment[];
  pendingEvent?: {
    id: string;
    targetRole: JourneyRole;
    skill: string;
    fatigueOnFailure: number;
    description: string;
  } | null;
}

export interface CouncilState {
  status: 'idle' | 'introduction' | 'interaction' | 'resolved';
  id?: string;
  objective?: string;
  tolerance: number;
  successes: number;
  failures: number;
  participants: string[];
  interlocutorNpcIds: string[];
  outcome?: 'success' | 'partial' | 'failure';
}

export interface CombatPosition { x: number; y: number; }

export interface CombatNarrativeBlock {
  id: string;
  round: number;
  text: string;
  createdAt: number;
}

export interface CombatRollRecord {
  id: string;
  actorId?: string;
  label: string;
  featDie: number | 'gandalf' | 'eye';
  successDice: number[];
  total: number;
  targetNumber?: number;
  success?: boolean;
  tengwar: number;
  createdAt: number;
}

export interface CombatActorState {
  actorId: string;
  actorType: 'hero' | 'npc' | 'adversary';
  stance?: CombatStance;
  engagedWith: string[];
  endurance: number;
  enduranceMax: number;
  hate?: number;
  wounded: boolean;
  defeated: boolean;
  initiative?: number;
  tokenId?: string | null;
  position?: CombatPosition;
  parry?: number;
  protection?: number;
  shadow?: number;
  hope?: number;
  fatigue?: number;
  stateNotes?: string[];
  quickSheet?: Record<string, unknown>;
}

export interface CombatState {
  status: 'idle' | 'opening-volley' | 'round' | 'resolved';
  id?: string;
  round: number;
  actors: CombatActorState[];
  advantageDice: number;
  complicationDice: number;
  turnIndex?: number;
  backgroundImage?: string | null;
  narrativeBlocks?: CombatNarrativeBlock[];
  rolls?: CombatRollRecord[];
  updatedAt?: number;
}

export interface FellowshipPhaseState {
  status: 'idle' | 'active' | 'resolved';
  locationId?: string;
  startedAt?: number;
  durationDays: number;
  undertakings: Record<string, string>;
}

export interface QuestState {
  id: string;
  title: string;
  status: 'active' | 'completed' | 'failed' | 'dormant';
  currentStep: string;
  steps: string[];
  deadline?: string;
  consequences: string;
  rewards: string;
  updatedAt: number;
}

export interface ThreatClock {
  id: string;
  name: string;
  current: number;
  max: number;
  consequence: string;
  hidden: boolean;
  updatedAt: number;
}

export interface KnowledgeEntry {
  id: string;
  subjectId: string;
  kind: KnowledgeKind;
  statement: string;
  visibility: KnowledgeVisibility;
  heroId?: string;
  confirmed: boolean;
  revealCondition?: string;
  source?: string;
  createdAt: number;
  updatedAt: number;
}

export interface NpcActorState {
  npcId: string;
  emotionalState: string;
  nextAction: string;
  location: string;
  faction: string;
  beliefs: string[];
  suspicions: string[];
  allies: string[];
  enemies: string[];
  relationshipByHero: Record<string, number>;
  lastUpdatedAt: number;
}

export type LivingCompanyStyle = 'fellowship' | 'itinerant-troupe' | 'settled-household' | 'mixed';
export type HeroLifestyle = 'adventurer' | 'wanderer' | 'troupe' | 'settled';
export type LifeProjectKind = 'hunting' | 'livelihood' | 'home' | 'family' | 'craft' | 'travel' | 'study' | 'other';

export interface HouseholdMember {
  id: string;
  name: string;
  relation: string;
  age: number;
  notes: string;
}

export interface HeroLifeState {
  characterId: string;
  lifestyle: HeroLifestyle;
  occupation: string;
  home: string;
  troupeRole: string;
  householdName: string;
  householdMembers: HouseholdMember[];
  notes: string;
  updatedAt: number;
}

export interface LifeProject {
  id: string;
  kind: LifeProjectKind;
  title: string;
  ownerCharacterIds: string[];
  location: string;
  progress: number;
  target: number;
  status: 'active' | 'completed' | 'abandoned';
  lastResolution: string;
  notes: string;
  startedAtYear: number;
  startedAtDay: number;
  updatedAt: number;
}


export type OrganizationKind = 'patron-household' | 'ranger-company' | 'clan' | 'merchant-house' | 'craft-guild' | 'order' | 'cult' | 'enemy-band' | 'settlement-council';
export interface OrganizationMember { id: string; entityType: 'hero' | 'npc'; entityId: string; role: string; rank: string; reputation: number; status: 'active' | 'absent' | 'expelled'; joinedAt: number; }
export interface OrganizationContract { id: string; title: string; description: string; status: 'offered' | 'accepted' | 'completed' | 'failed' | 'expired'; reward: string; consequence: string; deadline?: string; deadlineDay?: number; linkedQuestId?: string; updatedAt: number; }
export interface OrganizationProject { id: string; title: string; description: string; progress: number; target: number; status: 'active' | 'completed' | 'abandoned'; location: string; stakes: string; updatedAt: number; }
export interface OrganizationRelation { organizationId: string; attitude: number; status: 'allied' | 'friendly' | 'neutral' | 'rival' | 'hostile'; notes: string; }
export interface OrganizationState { id: string; name: string; kind: OrganizationKind; culture: string; agenda: string; resources: string; territory: string[]; influence: number; partyReputation: number; members: OrganizationMember[]; contracts: OrganizationContract[]; projects: OrganizationProject[]; relations: OrganizationRelation[]; secrets: string[]; nextAction: string; active: boolean; archived?: boolean; generatedBy?: 'master' | 'player' | 'migration'; createdAt?: number; updatedAt: number; }

export type StrongholdStatus = 'rumored' | 'discovered' | 'contested' | 'controlled' | 'abandoned' | 'ruined';
export type StrongholdOwnerKind = 'organization' | 'company' | 'unclaimed';
export type StrongholdLootState = 'stored' | 'claimed' | 'lost';
export interface StrongholdLootItem {
  id: string; catalogId?: string; name: string; category: string; quantity: number; load: number; value?: number; description: string; tags: string[]; rarity: 'common' | 'uncommon' | 'notable'; state: StrongholdLootState; assignedCharacterId?: string; source: string;
}
export interface StrongholdOwnershipEvent { ownerKind: StrongholdOwnerKind; ownerOrganizationId?: string; label: string; day: number; reason: string; createdAt: number; }
export interface StrongholdState {
  id: string; name: string; locationId: string; hexId: string | null; ownerOrganizationId?: string; previousOwnerOrganizationId?: string; ownerKind: StrongholdOwnerKind; status: StrongholdStatus; knownToCompany: boolean; autoGenerated: boolean; facilities: string[]; defenders: number; supplies: number; treasure: number; threats: string[]; projects: OrganizationProject[]; loot: StrongholdLootItem[]; ownershipHistory: StrongholdOwnershipEvent[]; discoveredDay?: number; capturedDay?: number; lastWorldTickDay?: number; notes?: string; updatedAt: number;
}
export interface LivingWorldActivity { id: string; kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'world'; title: string; detail: string; day: number; createdAt: number; sourceId?: string; }
export interface LivingWorldPending { id: string; title: string; detail: string; kind: string; status: 'pending-review' | 'blocked' | 'awaiting-roll'; createdAt: number; }

export interface LivingWorldState {
  companyStyle: LivingCompanyStyle;
  troupeName: string;
  baseLocation: string;
  heroLives: Record<string, HeroLifeState>;
  projects: LifeProject[];
  organizations?: OrganizationState[];
  strongholds?: StrongholdState[];
  activityLog?: LivingWorldActivity[];
  pendingReview?: LivingWorldPending[];
  automationEnabled?: boolean;
  lastSimulatedAt?: number;
  updatedAt: number;
}

export interface HierarchicalMemory {
  campaignSummary: string;
  sessionSummaries: Array<{ id: string; title: string; summary: string; startedAt: number; endedAt?: number }>;
  canonicalFacts: Array<{ id: string; fact: string; tags: string[]; updatedAt: number }>;
  promises: Array<{ id: string; fromId: string; toId: string; promise: string; status: 'open' | 'fulfilled' | 'broken'; updatedAt: number }>;
  openThreads: Array<{ id: string; title: string; status: 'open' | 'closed'; updatedAt: number }>;
}

export interface MasterState {
  schemaVersion: 3;
  activeMode: AdventureMode;
  position: PartyPosition;
  company: CompanyState;
  worldClock: WorldClock;
  journey: JourneyState;
  council: CouncilState;
  combat: CombatState;
  fellowshipPhase: FellowshipPhaseState;
  quests: QuestState[];
  threatClocks: ThreatClock[];
  knowledge: KnowledgeEntry[];
  npcActors: NpcActorState[];
  livingWorld: LivingWorldState;
  scene: SceneState;
  sessionDirector: SessionDirectorState;
  memory: HierarchicalMemory;
  currentSessionId: string;
  updatedAt: number;
}

export type MasterCommand =
  | { type: 'adjust_endurance'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'adjust_hope'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'adjust_shadow'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'adjust_fatigue'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'spend_hope'; targetCharacterId: string; attribute: 'strength' | 'heart' | 'wits'; reason: string; source?: string }
  | { type: 'spend_fellowship_for_hope'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'adjust_supplies'; targetCharacterId: string; amount: number; reason: string; source?: string }
  | { type: 'set_condition'; targetCharacterId: string; condition: 'wounded' | 'weary' | 'miserable'; value: boolean; reason: string; source?: string }
  | { type: 'move_party'; toHexId: string; locationName: string; distanceKm?: number; terrain?: string; reason: string }
  | { type: 'set_present_npcs'; npcIds: string[]; reason: string }
  | { type: 'upsert_npc'; npc: NpcDraft; reason: string }
  | { type: 'update_npc_actor'; npcId: string; emotionalState?: string; nextAction?: string; beliefs?: string[]; suspicions?: string[]; location?: string; reason: string }
  | { type: 'open_scene'; kind: SceneKind; title: string; objective: string; participantHeroIds?: string[]; participantNpcIds?: string[]; tension?: number; endCondition?: string; reason: string }
  | { type: 'close_scene'; reason: string }
  | { type: 'set_spotlight'; targetCharacterId: string; reason: string }
  | { type: 'upsert_threat'; clock: Partial<ThreatClock> & Pick<ThreatClock, 'id' | 'name'>; reason: string }
  | { type: 'advance_threat'; clockId: string; amount: number; reason: string }
  | { type: 'upsert_quest'; quest: Partial<QuestState> & Pick<QuestState, 'id' | 'title'>; reason: string }
  | { type: 'add_knowledge'; entry: Omit<KnowledgeEntry, 'createdAt' | 'updatedAt'>; reason: string }
  | { type: 'set_living_company'; companyStyle: LivingCompanyStyle; troupeName?: string; baseLocation?: string; reason: string }
  | { type: 'upsert_hero_life'; characterId: string; life: Partial<Omit<HeroLifeState, 'characterId' | 'updatedAt'>>; reason: string }
  | { type: 'upsert_life_project'; project: Partial<LifeProject> & Pick<LifeProject, 'id' | 'kind' | 'title'>; reason: string }
  | { type: 'advance_life_project'; projectId: string; amount: number; resolution: string; reason: string }
  | { type: 'advance_time'; minutes: number; reason: string; source?: string }
  | { type: 'set_mode'; mode: AdventureMode; reason: string }
  | { type: 'start_journey'; destinationHexId: string | null; roles?: Partial<Record<JourneyRole, string>>; reason: string }
  | { type: 'complete_journey'; reason: string }
  | { type: 'start_council'; objective: string; tolerance: number; participants: string[]; npcIds: string[]; reason: string }
  | { type: 'record_council_result'; success: boolean; reason: string }
  | { type: 'resolve_council'; outcome: 'success' | 'partial' | 'failure'; reason: string }
  | { type: 'start_combat'; actorIds: string[]; reason: string }
  | { type: 'set_combat_stance'; targetCharacterId: string; stance: CombatStance; reason: string }
  | { type: 'next_combat_round'; reason: string }
  | { type: 'end_combat'; reason: string }
  | { type: 'upsert_combat_actor'; actor: CombatActorState; reason: string }
  | { type: 'move_combat_actor'; actorId: string; x: number; y: number; reason: string }
  | { type: 'record_combat_narrative'; text: string; reason: string }
  | { type: 'record_combat_roll'; roll: CombatRollRecord; reason: string }
  | { type: 'set_combat_background'; imageData: string | null; reason: string }
  | { type: 'upsert_organization'; organization: Partial<OrganizationState> & Pick<OrganizationState, 'id' | 'name' | 'kind'>; reason: string }
  | { type: 'archive_organization'; organizationId: string; archived: boolean; reason: string }
  | { type: 'delete_organization'; organizationId: string; reason: string }
  | { type: 'advance_organization_project'; organizationId: string; projectId: string; amount: number; reason: string }
  | { type: 'upsert_stronghold'; stronghold: Partial<StrongholdState> & Pick<StrongholdState, 'id' | 'name'>; reason: string }
  | { type: 'claim_stronghold'; strongholdId: string; reason: string }
  | { type: 'abandon_stronghold'; strongholdId: string; reason: string }
  | { type: 'delete_stronghold'; strongholdId: string; reason: string }
  | { type: 'transfer_stronghold_loot'; strongholdId: string; lootItemId: string; targetCharacterId: string; quantity: number; reason: string }
  | { type: 'transfer_stronghold_treasure'; strongholdId: string; targetCharacterId: string; amount: number; reason: string }
  | { type: 'record_living_activity'; activity: Omit<LivingWorldActivity, 'createdAt'> & { createdAt?: number }; reason: string }
  | { type: 'start_fellowship_phase'; locationId?: string; reason: string }
  | { type: 'end_fellowship_phase'; reason: string };

export interface DirectorPlan {
  intent: string;
  narrativeIntentClass?: 'action-diegetic' | 'speech-diegetic' | 'test-mechanic' | 'rule-query' | 'context-query' | 'recap' | 'spatial-clarification' | 'planning';
  advancesFiction?: boolean;
  actingCharacterId?: string;
  scope: 'hero' | 'company';
  mode: AdventureMode;
  sceneKind?: SceneKind;
  spotlightCharacterId?: string;
  requiredTest?: (TestRequirement & { characterId?: string }) | null;
  commands: MasterCommand[];
  npcDecisions: Array<{ npcId: string; decision: string; visibleNow: boolean }>;
  revelationsAllowed: string[];
  visibleFacts: string[];
  hiddenNotes: string[];
  narrationBrief: string;
  ruleSourceNumbers: number[];
  ruleSummary: string;
}

export interface RuleAnswer {
  answer: string;
  ruleApplied: string;
  examples: string[];
  uncertainty: string;
  usedSourceNumbers: number[];
}

export interface CommandAuditEntry {
  id: string;
  campaignId: string;
  timestamp: number;
  command: MasterCommand;
  status: 'applied' | 'rejected';
  reason: string;
  before?: unknown;
  after?: unknown;
}
