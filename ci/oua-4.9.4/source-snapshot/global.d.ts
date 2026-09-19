/// <reference types="vite/client" />

import type { SoundItem } from './lib/db';

declare module '*?url' {
  const url: string;
  export default url;
}

declare global {
  interface Window {
    __BRASEIRO_DEVICE_PROFILE__?: Record<string, any>;
    BraseiroAndroid?: {
      requestDriveToken?: () => Promise<string> | string;
      logoutDrive?: () => Promise<void> | void;
      hasGeminiApiKey?: (tier: 'free' | 'paid') => boolean;
      setGeminiApiKey?: (tier: 'free' | 'paid', value: string) => void;
      geminiRequest?: (model: string, body: string, paidOnly?: boolean) => Promise<string>;
      generateTts?: (model: string, text: string, voice: string, style: string, paidOnly?: boolean) => Promise<{ token: string; durationMs: number; model: string; tier: string }>;
      playGeneratedVoice?: (token: string, volume: number) => Promise<void>;
      isNativeTtsAvailable?: () => boolean;
      speakText?: (text: string, rate?: number) => Promise<void>;
      speakTextWithCues?: (text: string, rate?: number, cues?: Array<{ id: string; charIndex: number }>) => Promise<void>;
      stopSpeech?: () => void;
      importAudioFiles?: () => Promise<{ imported: number; skipped: number; failed: number; errors?: string[]; keywordLibraryText?: string; catalogCount?: number }>;
      importAudioFolder?: () => Promise<{ jobId?: string; scheduled?: boolean; resumed?: boolean; imported: number; skipped: number; failed: number; errors?: string[]; keywordLibraryText?: string; catalogCount?: number }>;
      getNativeSoundKeywordLibrary?: () => Promise<string>;
      selectCatalogAudio?: (request: import('./lib/audioCatalog').AudioCatalogRequest) => Promise<SoundItem | null>;
      getAudioCatalogStatus?: () => Promise<Record<string, unknown>>;
      reindexAudioCatalog?: () => Promise<{ catalogCount: number; database: string }>;
      markAudioFailure?: (id: string) => void;
      markAudioAvailable?: (id: string) => void;
      beginAudioSession?: (id: string, sceneId?: string, backend?: string) => boolean;
      finishAudioSession?: (id: string, status?: string) => boolean;
      preloadNativeSounds?: (ids: string[]) => Promise<void>;
      listNativeSounds?: () => Promise<SoundItem[]>;
      playNativeSound?: (id: string, channel: string, loop: boolean, volume: number, slot?: string, scheduledAtMs?: number) => Promise<boolean>;
      stopNativeAudio?: (channel?: string, slot?: string) => void;
      setNativeAudioVolume?: (channel: string, volume: number) => void;
      getNativeAudioStatus?: () => Promise<Record<string, unknown>>;
      getNativeHealthStatus?: () => Promise<Record<string, any>>;
      saveRecoveryBackup?: (campaignId: string, json: string) => boolean;
      getLatestRecoveryBackup?: (campaignId: string) => string | null;
      deleteNativeSound?: (id: string) => Promise<void>;
      clearNativeSounds?: () => Promise<void>;
      exportBackup?: (fileName: string, json: string) => Promise<string>;
      shareText?: (title: string, text: string) => Promise<void>;
      consumeSharedImage?: () => Promise<string | null>;
      peekSharedImage?: () => Promise<string | null>;
      clearSharedImage?: () => Promise<boolean>;
      barbaraHealth?: () => Promise<Record<string, any>>;
      barbaraDeviceProfile?: () => Promise<Record<string, any>>;
      barbaraSupervisorInspect?: (hostState: Record<string, unknown>, text: string, importance?: string, inputChannel?: 'PLAYER_ACTION' | 'GM_HELP') => Promise<Record<string, any>>;
      barbaraSupervisorFinalize?: (hostState: Record<string, unknown>, text: string, requestId: string, narration: string, importance?: string, inputChannel?: 'PLAYER_ACTION' | 'GM_HELP', resolution?: Record<string, unknown> | null, presentation?: Record<string, unknown> | null) => Promise<Record<string, any>>;
      barbaraAudioScenePlan?: (events: Record<string, unknown>[], context?: Record<string, unknown>, catalog?: Record<string, unknown>, mix?: Record<string, unknown>, narrationActive?: boolean) => Promise<Record<string, any>>;
      barbaraVisiblePois?: (canonical?: Record<string, unknown>[], campaign?: Record<string, unknown>[], procedural?: Record<string, unknown>[], gm?: boolean) => Promise<Record<string, any>[]>;
      barbaraContract?: () => Promise<Record<string, any>>;
      barbaraPlan?: (action: string, importance?: string, narrative?: boolean, nonAdvancing?: boolean, location?: string, imprint?: string, inputChannel?: 'PLAYER_ACTION' | 'GM_HELP', storyOccasion?: 'campaign_opening' | 'first_arrival' | 'changed_return' | 'continuation') => Promise<Record<string, any>>;
      barbaraValidate?: (envelope: Record<string, unknown>, action: string, importance?: string, occasion?: string, inputChannel?: 'PLAYER_ACTION' | 'GM_HELP') => Promise<Record<string, any>>;
      barbaraValidateState?: (hostState: Record<string, unknown>) => Promise<Record<string, any>>;
      barbaraInterpretRoll?: (featResults: number[], successResults: number[], target: number, mode: 'normal' | 'favoured' | 'ill-favoured', weary: boolean, modifier: number, hostRoll: Record<string, unknown>) => Promise<Record<string, any>>;
      rollOneRingPhysical?: (successDiceCount: number, rollMode: 'normal' | 'favoured' | 'ill-favoured', actorId?: string, actorName?: string) => Promise<{ engine: string; mode: 'physical_3d'; feat_results: number[]; success_results: number[]; physics_seed_feat: string; physics_seed_success: string; physics_steps_feat: number; physics_steps_success: number; forced_settle_feat: boolean; forced_settle_success: boolean }>;
      rollPhysicalDice?: (formula: string, actorId?: string, actorName?: string) => Promise<{ engine: string; mode: 'physical_3d'; dice: number[]; total: number; modifier: number; physics_seed: string; physics_steps: number; forced_settle: boolean }>;
    };
  }
}

export {};
