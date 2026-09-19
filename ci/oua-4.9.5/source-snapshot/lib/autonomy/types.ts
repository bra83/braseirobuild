export type HealthStatus = 'ok' | 'warning' | 'error' | 'repairing' | 'untested';

export interface HealthCheckItem {
  id: string;
  label: string;
  status: HealthStatus;
  detail: string;
  repaired?: boolean;
  updatedAt: number;
}

export interface HealthReport {
  id: string;
  campaignId: string;
  createdAt: number;
  overall: HealthStatus;
  items: HealthCheckItem[];
  repairs: string[];
}

export interface AssetRegistryEntry {
  id: string;
  assetType: 'map' | 'manual' | 'token' | 'audio' | 'tts-cache' | 'campaign' | 'snapshot';
  sourceId: string;
  name: string;
  size?: number;
  hash?: string;
  available: boolean;
  origin: 'bundled' | 'indexeddb' | 'native' | 'generated';
  version?: string;
  lastVerifiedAt: number;
  metadata?: Record<string, unknown>;
}

export type InputChannel = 'PLAYER_ACTION' | 'GM_HELP';

export type TurnTransactionStatus = 'preparing' | 'mechanics-applied' | 'committed' | 'aborted';

export interface TurnTransaction {
  id: string;
  idempotencyKey: string;
  campaignId: string;
  action: string;
  inputChannel: InputChannel;
  baseSceneId?: string;
  status: TurnTransactionStatus;
  createdAt: number;
  updatedAt: number;
  committedSceneId?: string;
  error?: string;
  resultDigest?: string;
}

export interface EventJournalEntry {
  id: string;
  campaignId: string;
  turnId?: string;
  eventType: string;
  entityType: 'campaign' | 'character' | 'npc' | 'map' | 'audio' | 'system';
  entityId?: string;
  payload: Record<string, unknown>;
  idempotencyKey?: string;
  createdAt: number;
  applied: boolean;
}

export type GameSystemScope = string;
export type TerrainClass = 'snow' | 'mountain' | 'forest' | 'wetland' | 'desert' | 'coast' | 'fields' | 'road' | 'urban' | 'ruins' | 'water' | 'unknown';
export type WeatherStage = 'stable' | 'indications' | 'forming' | 'active' | 'dissipating';
export type ClimateZone = 'arctic' | 'alpine' | 'cold-temperate' | 'temperate' | 'wetland' | 'forest' | 'coastal' | 'dry' | 'unknown';
export type SettlementKind = 'city' | 'town' | 'village' | 'fortress' | 'inn' | 'port' | 'ruin' | 'none';

export interface NearbySettlement {
  id: string;
  name: string;
  kind: SettlementKind;
  hexId: string;
  distanceHexes: number;
  confidence: number;
  movementTrend?: 'approaching' | 'leaving' | 'nearby' | 'unknown';
}

export interface WeatherForecast {
  stage?: WeatherStage;
  stageStep?: number;
  activeStepsRemaining?: number;
  id: string;
  kind: 'clear' | 'cloudy' | 'rain' | 'storm' | 'snow' | 'blizzard' | 'fog' | 'wind';
  intensity: 'light' | 'medium' | 'heavy';
  probability: number;
  reason: string;
  eligible: boolean;
}

export interface RoutePreviewStep {
  step: number;
  hexId: string;
  terrain: string;
  terrainClass: TerrainClass;
  previousTerrainClass?: TerrainClass;
  transition?: string;
  region: string;
  locations: string[];
  settlementKind: SettlementKind;
  roadEvidence: boolean;
  confidence?: number;
}

export interface EncounterOpportunity {
  id: string;
  category: 'merchant' | 'messenger' | 'wagon' | 'patrol' | 'guide' | 'resident' | 'traveller' | 'fauna' | 'tracks' | 'hazard' | 'resource' | 'exploration';
  title: string;
  description: string;
  trigger: string;
  probability: number;
  requiredConditions: string[];
  ambienceLayers?: string[];
}

export interface EnvironmentContext {
  campaignId: string;
  systemScope: GameSystemScope;
  settingScope: string;
  rulesProfileId: string;
  rulesSourceFolderId?: string;
  currentHexId: string | null;
  generatedAt: number;
  worldTimeKey: string;
  season: 'spring' | 'summer' | 'autumn' | 'winter';
  hour: number;
  terrain: string;
  terrainClass: TerrainClass;
  region: string;
  climateZone: ClimateZone;
  elevationClass: 'lowland' | 'upland' | 'mountain' | 'unknown';
  snowEligible: boolean;
  currentWeather: WeatherForecast;
  possibleWeather: WeatherForecast[];
  nearbySettlements: NearbySettlement[];
  approachSignals: string[];
  ambienceHints: string[];
  encounterHints: string[];
  encounterOpportunities: EncounterOpportunity[];
  faunaHints: string[];
  trackHints: string[];
  travelRiskHints: string[];
  naturalResourceHints: string[];
  explorationHints: string[];
  futureRoute: RoutePreviewStep[];
  adjacentHexSummary: Array<{
    hexId: string;
    ring: number;
    terrain: string;
    terrainClass: TerrainClass;
    region: string;
    locations: string[];
    settlementKind: SettlementKind;
  }>;
  evidence: string[];
}

export interface EnvironmentMemoryRecord {
  id: string;
  campaignId: string;
  systemScope: GameSystemScope;
  kind: 'signal' | 'encounter' | 'fauna' | 'track' | 'risk' | 'resource' | 'exploration' | 'ambience';
  key: string;
  text: string;
  useCount: number;
  lastUsedAt: number;
  lastSceneId?: string;
  recentHexIds: string[];
  metadata?: Record<string, unknown>;
}

export interface AutonomousEventRecord {
  systemScope?: GameSystemScope;
  id: string;
  campaignId: string;
  hexId: string | null;
  kind: 'weather' | 'approach-settlement' | 'road-traffic' | 'regional-sign' | 'recovery';
  title: string;
  description: string;
  probability: number;
  status: 'suggested' | 'used' | 'dismissed';
  notBefore: number;
  expiresAt: number;
  createdAt: number;
  metadata?: Record<string, unknown>;
}
