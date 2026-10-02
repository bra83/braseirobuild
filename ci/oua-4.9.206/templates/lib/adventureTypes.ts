import type { CampaignState } from './db';

export type CampaignStartMode = 'star-of-mist'|'lone-lands'|'strider'|'sandbox'|'tavern';

export interface AdventureSourceReference {
  sourceBook: string;
  printedPages?: string;
  driveFileId?: string;
}

export interface AdventureLocationDefinition {
  canonicalName: string;
  venueName?: string;
  aliases: string[];
  regionAliases?: string[];
  bias?: 'none'|'southeast';
}

export interface AdventureModule {
  id: string;
  title: string;
  campaignTitle: string;
  source: AdventureSourceReference;
  start: AdventureLocationDefinition;
  openingFacts: string[];
  gmFacts: string[];
  campaignSequence?: string[];
}

export interface CampaignStartSelection {
  mode: CampaignStartMode;
  adventureId?: string;
  patronId?: string;
  missionIndex?: number;
  resolvedHexId?: string;
  resolvedLocationName?: string;
  venueName?: string;
  resolutionSource?: string;
}

export interface AdventureRuntimeState {
  moduleId: string;
  status: 'not_started'|'active'|'paused'|'completed'|'failed';
  currentSceneId?: string;
  currentLocationName?: string;
  currentHexId?: string;
  patronId?: string;
  missionIndex?: number;
  missionSeed?: string;
  discoveredClueIds: string[];
  revealedSecretIds: string[];
  completedObjectiveIds: string[];
  failedObjectiveIds: string[];
  triggeredEventIds: string[];
  flags: Record<string, boolean>;
  history: Array<{ sceneId:string; location:string; timestamp:number }>;
}

export interface ResolvedCampaignStart {
  ok: boolean;
  requiresManualAtlas: boolean;
  mode: CampaignStartMode;
  selection: CampaignStartSelection;
  module: AdventureModule|null;
  hexId: string|null;
  locationName: string;
  venueName?: string;
  openingFacts: string[];
  missionSeed?: string;
  resolutionSource: string;
  error?: string;
}

export type CampaignWithAdventure = CampaignState & {
  campaignStart?: CampaignStartSelection;
  adventureRuntime?: AdventureRuntimeState;
};
