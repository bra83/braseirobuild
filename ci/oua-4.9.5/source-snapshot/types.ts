export enum GamePhase {
  INIT,
  GENERATING,
  PLAYING,
  GAME_OVER
}

export interface PlayerStats {
  hp: number;
  sanity: number;
  supplies: number;
}

export interface TestRequirement {
  id: string;
  skill: string;
  targetNumber?: number;
  rollMode?: 'normal' | 'favoured' | 'ill-favoured';
  reason: string;
  consequences?: string;
  characterId?: string;
}

export interface StoryChoice {
  id: string;
  text: string;
  requiresTest?: boolean;
  test?: TestRequirement;
}

export type TtsVoiceName = 'Charon' | 'Kore' | 'Puck' | 'Fenrir' | 'Aoede' | 'Gacrux' | 'Algenib' | 'Sulafat';

export interface VoiceSegment {
  speakerType: 'narrator' | 'npc' | 'hero';
  speakerId?: string;
  speakerName: string;
  text: string;
  emotion?: string;
}

export interface AudioCue {
  keyword: string;
  triggerText?: string;
  aliases?: string[];
  channel?: 'sfx' | 'ambience-detail';
  priority?: number;
}

export interface SceneAudioPlan {
  environment: string[];
  ambience: string[];
  ambienceDetails?: string[];
  musicMood?: string[];
  sfx?: AudioCue[];
  stopPreviousAmbience?: boolean;
}

export type CampaignEventType =
  | 'scene_started'
  | 'location_changed'
  | 'journey_day_completed'
  | 'combat_started'
  | 'combat_ended'
  | 'npc_encountered'
  | 'rest_completed'
  | 'test_requested'
  | 'test_resolved'
  | 'clue_discovered'
  | 'custom';

export interface CampaignEvent {
  id: string;
  type: CampaignEventType;
  timestamp: number;
  sceneId?: string;
  data?: Record<string, string | number | boolean | null>;
}

export interface SceneSourceReference {
  manualId: string;
  manualName: string;
  pageNum: number;
  physicalPageNum?: number;
  pageLabel?: string;
  excerpt: string;
  usage?: string;
}

export interface StoryNode {
  atmospheric_narration: string;
  action_result: string;
  visualPrompt: string;
  sceneVisualPrompt?: string;
  choices: StoryChoice[];
  statUpdates?: Partial<PlayerStats>;
  location: string;
  weather?: string;
  timeOfDay?: string;
  worldHour?: number;
  worldMinute?: number;
  requiredTest?: TestRequirement | null;
  audioPlan?: SceneAudioPlan;
  events?: CampaignEvent[];
  sources?: SceneSourceReference[];
}

export interface MapNode {
  id: string;
  name: string;
  x: number;
  y: number;
}

export interface MapPath {
  from: string;
  to: string;
}

export interface GameState {
  history: StoryNode[];
  currentNode: StoryNode | null;
  stats: PlayerStats;
  phase: GamePhase;
}

export interface GeminiStoryResponse {
  atmospheric_narration: string;
  action_result: string;
  visualPrompt: string;
  sceneVisualPrompt?: string;
  location: string;
  weather?: string;
  choices: StoryChoice[];
  requiredTest?: TestRequirement | null;
  audioPlan?: SceneAudioPlan;
  events?: CampaignEvent[];
  hpChange?: number;
  sanityChange?: number;
  suppliesChange?: number;
  inventoryChanges?: { item: string; change: number }[];
  narrativeLog?: string;
  imageRecommended?: boolean;
  usedSourceNumbers?: number[];
  sourceUsageSummary?: string;
  voiceScript?: VoiceSegment[];
}
