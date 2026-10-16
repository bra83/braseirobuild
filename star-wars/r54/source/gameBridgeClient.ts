export type BridgeAvailability = 'available' | 'unavailable' | 'invalid';
export type BridgeSubmitState = 'idle' | 'sending' | 'sent' | 'error';
export type BridgeDataState = 'idle' | 'working' | 'success' | 'error';

export type BridgeErrorCode =
  | 'BRIDGE_UNAVAILABLE'
  | 'INVALID_BRIDGE'
  | 'UNSUPPORTED'
  | 'INVALID_INPUT'
  | 'NO_CAMPAIGN'
  | 'STATE_READ_FAILED'
  | 'ENGINE_REJECTED'
  | string;

export type BridgeSubmitResult =
  | { ok: true; data: unknown }
  | { ok: false; error: BridgeErrorCode; message: string };

export type BridgeFilePayload = { fileName: string; json: string };
export type BridgeSnapshotItem = { id: string; label: string; createdAt: string };
export type BridgeCatalogWeapon = {
  id: string; name: string; price: number; rarity: number; restricted: boolean;
  damage: number | null; damageBonus: number | null; crit: number | null; range: string; qualities: string[];
};
export type BridgeEquipmentCatalog = { weapons: BridgeCatalogWeapon[] };
export type BridgeImageAsset = {
  assetId: string; title: string; category: string; subtype?: string; mapKind?: string; sourceType: string;
  sourceName?: string; sourceRef?: string; sourcePageUrl?: string; remoteUrl?: string; remoteReady?: boolean; offlineReady?: boolean;
  cachePolicy?: string; localPath?: string; tags: string[]; usage?: string; mechanicalBinding: Record<string, unknown> | null;
  publishedAt?: string; variants?: Array<{ variantId: string; title?: string; remoteUrl: string }>;
};
export type BridgeImageLibraryInfo = {
  libraryId: string; schemaVersion: number; counts: Record<string, number>; policy: Record<string, unknown>;
};
export type BridgeImageRuntimeDescriptor = {
  assetId: string; mode: 'packaged' | 'external_cache_required'; offlineReady: boolean; localPath?: string;
  remoteReady?: boolean; remoteUrl?: string | null; sourcePageUrl?: string | null; cachePolicy?: string;
};
export type BridgeCreationSpecies = {
  key: string; label: string; characteristics: Record<string, number>; woundBase: number; strainBase: number; xp: number;
  careerFreeCount: number; specializationFreeCount: number; freeSkill: { skill: string } | null; freeTalents: string[];
  speciesChoice: { type: 'oneSkill' | 'twoNonCareer' | 'gandRespiration'; skills?: string[]; variants?: string[] } | null; abilities: string[];
};
export type BridgeCreationSpecialization = { key: string; label: string; skills: string[] };
export type BridgeCreationCareer = { key: string; label: string; skills: string[]; specializations: BridgeCreationSpecialization[] };
export type BridgeCreationCatalog = {
  profile: 'EOTE_CORE'; species: BridgeCreationSpecies[]; careers: BridgeCreationCareer[]; obligationTypes: string[];
  characteristicLabels: Record<string, string>; skillLabels: Record<string, string>;
};
export type BridgeCreationPreview = {
  xpBudget: number; characteristicCost: number; xpRemaining: number; credits: number; obligation: number; startingObligation: number; additionalObligation: number; obligationOfficial: boolean; obligationNote: string | null; speciesXpBonus: number;
};
export type BridgeCreationOptions = {
  name: string; background?: string; speciesKey: string; speciesVariant?: string | null; careerKey: string; specializationKey: string; partySize?: number; startingObligation?: number;
  obligationType: string; obligationBenefits: { xp5?: boolean; xp10?: boolean; credits1000?: boolean; credits2500?: boolean };
  characteristics: Record<string, number>; careerFreeSkills: string[]; specializationFreeSkills: string[]; speciesChoiceSkills: string[]; motivation: string;
};

export interface BraseiroGameBridgeContract {
  readState?: () => unknown;
  initializeCampaign?: () => BridgeSubmitResult;
  campaignModeSummary?: () => BridgeSubmitResult;
  setCampaignMode?: (mode: 'EOTE' | 'AOR' | 'MIXED') => BridgeSubmitResult;
  initializeLivingSandbox?: (options?: { startingPoint?: string; regionLabel?: string }) => BridgeSubmitResult;
  livingSandboxStatus?: () => BridgeSubmitResult;
  sandboxOpening?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  travelToLocation?: (locationId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  scenePlayerView?: () => BridgeSubmitResult;
  resolveSuggestedMasterCheck?: () => BridgeSubmitResult;
  submitPlayerIntent: (text: string) => Promise<BridgeSubmitResult>;
  exportCharacterJson?: () => BridgeSubmitResult;
  importCharacterJson?: (text: string, options?: { createSnapshot?: boolean }) => BridgeSubmitResult;
  exportCampaignBackup?: () => BridgeSubmitResult;
  importCampaignBackup?: (text: string, options?: { createSnapshot?: boolean }) => BridgeSubmitResult;
  listSnapshots?: () => BridgeSubmitResult;
  createSnapshot?: (label?: string) => BridgeSubmitResult;
  restoreSnapshot?: (id: string) => BridgeSubmitResult;
  generateRandomCharacter?: (options: { name: string; motivation: string; partySize?: number }, settings?: { createSnapshot?: boolean }) => BridgeSubmitResult;
  creationCatalog?: () => BridgeSubmitResult;
  creationPreview?: (options: Partial<BridgeCreationOptions>) => BridgeSubmitResult;
  createCustomCharacter?: (options: BridgeCreationOptions, settings?: { createSnapshot?: boolean }) => BridgeSubmitResult;
  ownedSpecializations?: () => BridgeSubmitResult;
  specializationPurchaseCost?: (careerKey: string, specializationKey: string) => BridgeSubmitResult;
  buySpecialization?: (careerKey: string, specializationKey: string) => BridgeSubmitResult;
  talentTree?: (selector?: Record<string, unknown>) => BridgeSubmitResult;
  eligibleTalents?: (selector?: Record<string, unknown>) => BridgeSubmitResult;
  buyTalent?: (nodeId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  talentProgressionStatus?: () => BridgeSubmitResult;
  forceStatus?: () => BridgeSubmitResult;
  forceCatalog?: () => BridgeSubmitResult;
  buyForcePower?: (powerKey: string) => BridgeSubmitResult;
  buyForceUpgrade?: (powerKey: string, upgradeId: string) => BridgeSubmitResult;
  rollForcePowerCheck?: (options?: { useOppositePoints?: number }) => BridgeSubmitResult;
  commitForce?: (effectId: string, count?: number) => BridgeSubmitResult;
  releaseForce?: (effectId: string) => BridgeSubmitResult;
  equipmentOwnedSummary?: () => BridgeSubmitResult;
  attachmentSummary?: () => BridgeSubmitResult;
  proposeAttachmentAcquisition?: (itemId: string) => BridgeSubmitResult;
  installOwnedAttachment?: (assetId: string, weaponId: string) => BridgeSubmitResult;
  removeOwnedAttachment?: (assetId: string) => BridgeSubmitResult;
  attemptAttachmentMod?: (assetId: string, modId: string) => BridgeSubmitResult;
  equipWeapon?: (weaponId: string) => BridgeSubmitResult;
  equipArmor?: (itemId: string) => BridgeSubmitResult;
  unequipArmor?: (itemId?: string | null) => BridgeSubmitResult;
  cyberneticsSummary?: () => BridgeSubmitResult;
  proposeCyberneticAcquisition?: (itemId: string) => BridgeSubmitResult;
  installOwnedCybernetic?: (assetId: string) => BridgeSubmitResult;
  removeOwnedCybernetic?: (assetId: string) => BridgeSubmitResult;
  combatRuntimeStatus?: () => BridgeSubmitResult;
  changeCombatRange?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  takeCover?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  takeGuardedStance?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  drawWeaponInCombat?: (weaponId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  prepareWeaponForAttack?: (weaponId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  reloadLimitedAmmoWeapon?: (weaponId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  advancePlayerTurn?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  finishCombat?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  aim?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveWeaponAttack?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  activatePendingPlayerCritical?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  activatePendingWeaponQuality?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  activatePendingDisorientTalent?: () => BridgeSubmitResult;
  dropProne?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  standUpFromProne?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  toggleWeaponStunSetting?: (weaponId: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  vehicleRuntimeStatus?: () => BridgeSubmitResult;
  selectVehicle?: (vehicleId: string) => BridgeSubmitResult;
  beginVehicleCombat?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  nextVehicleRound?: () => BridgeSubmitResult;
  endVehicleCombat?: () => BridgeSubmitResult;
  useVehiclePilotManeuver?: (kind: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  useFullThrottle?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  gainVehicleAdvantage?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  useVehicleCrewAction?: (kind: string, options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveVehicleAttack?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  activatePendingVehicleCritical?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  activatePendingVehicleQuality?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  reactivateGuidedProjectile?: (projectileId: string) => BridgeSubmitResult;
  attemptTractorEscape?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  vehicleDamageControl?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  astrogationPlan?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveHyperspaceJump?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  recoveryRuntimeStatus?: () => BridgeSubmitResult;
  usePlayerStimpack?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  useEmergencyRepairPatch?: () => BridgeSubmitResult;
  useMedicine?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  restFullNight?: () => BridgeSubmitResult;
  groupSummary?: () => BridgeSubmitResult;
  resolveCampaignMilestone?: () => BridgeSubmitResult;
  startSession?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  closeSession?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  encumbranceStatus?: () => BridgeSubmitResult;
  liftingDifficulty?: (objectEncumbrance: number, options?: Record<string, unknown>) => BridgeSubmitResult;
  statusSummary?: () => BridgeSubmitResult;
  applyStatus?: (payload?: Record<string, unknown>) => BridgeSubmitResult;
  destinySummary?: () => BridgeSubmitResult;
  spendDestiny?: (side: 'light' | 'dark') => BridgeSubmitResult;
  resolveFearCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveOpposedSkillCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveAssistedSkillCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveCompetitiveSkillCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveSocialInfluenceCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveIncomingSocialInfluenceCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  resolveIncomingIdentificationCheck?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  useFeint?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  legalSaleQuote?: (options?: Record<string, unknown>) => BridgeSubmitResult;
  attemptRepairActiveVehicleCritical?: (index: number, options?: Record<string, unknown>) => BridgeSubmitResult;
  equipmentCatalog?: () => BridgeSubmitResult;
  proposeWeaponAcquisition?: (itemId: string) => BridgeSubmitResult;
  imageLibraryInfo?: () => BridgeSubmitResult;
  searchImageLibrary?: (criteria?: Record<string, unknown>) => BridgeSubmitResult;
  imageMapCatalog?: (criteria?: Record<string, unknown>) => BridgeSubmitResult;
  imageVisualEntityCatalog?: (criteria?: Record<string, unknown>) => BridgeSubmitResult;
  imageAsset?: (assetId: string) => BridgeSubmitResult;
  imageAssetRuntime?: (assetId: string) => BridgeSubmitResult;
}

declare global {
  interface Window {
    BraseiroGameBridge?: BraseiroGameBridgeContract;
  }
}

function currentBridge(): unknown {
  if (typeof window === 'undefined') return undefined;
  return window.BraseiroGameBridge;
}

function isBridge(value: unknown): value is BraseiroGameBridgeContract {
  return !!value && typeof value === 'object' && typeof (value as { submitPlayerIntent?: unknown }).submitPlayerIntent === 'function';
}

function bridgeOrError(): BraseiroGameBridgeContract | BridgeSubmitResult {
  const bridge = currentBridge();
  if (bridge === undefined) return { ok: false, error: 'BRIDGE_UNAVAILABLE', message: 'Motor Barbara indisponível neste ambiente.' };
  if (!isBridge(bridge)) return { ok: false, error: 'INVALID_BRIDGE', message: 'Ponte do motor incompatível com esta shell.' };
  return bridge;
}

function normalizeResult(result: unknown, invalidMessage: string): BridgeSubmitResult {
  if (!result || typeof result !== 'object') {
    return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
  }

  const candidate = result as { ok?: unknown; data?: unknown; error?: unknown; message?: unknown };
  if (candidate.ok === true) {
    if (!Object.prototype.hasOwnProperty.call(candidate, 'data')) {
      return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
    }
    return { ok: true, data: candidate.data };
  }

  if (candidate.ok === false && typeof candidate.error === 'string' && candidate.error.trim() && typeof candidate.message === 'string' && candidate.message.trim()) {
    return { ok: false, error: candidate.error, message: candidate.message };
  }

  return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function invalidPayload(message: string): BridgeSubmitResult {
  return { ok: false, error: 'INVALID_BRIDGE', message };
}

function hasValidSnapshotId(value: Record<string, unknown>): boolean {
  if (!Object.prototype.hasOwnProperty.call(value, 'snapshotId')) return false;
  const snapshotId = value.snapshotId;
  return snapshotId === null || (typeof snapshotId === 'string' && !!snapshotId.trim());
}

function validateFilePayloadResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || typeof data.fileName !== 'string' || !data.fileName.trim() || typeof data.json !== 'string' || !data.json.trim()) {
    return invalidPayload(message);
  }
  try { JSON.parse(data.json); }
  catch { return invalidPayload(message); }
  return { ok: true, data: { fileName: data.fileName, json: data.json } satisfies BridgeFilePayload };
}

function validateCharacterMutationResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || !isRecord(data.character) || !isRecord(data.state) || !hasValidSnapshotId(data)) return invalidPayload(message);
  return result;
}

function validateCampaignImportResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || !isRecord(data.state) || !hasValidSnapshotId(data)) return invalidPayload(message);
  return result;
}

function validateSnapshotItemResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data)) return invalidPayload(message);
  const id = typeof data.id === 'string' ? data.id.trim() : '';
  const label = typeof data.label === 'string' ? data.label.trim() : '';
  const createdAt = typeof data.createdAt === 'string' ? data.createdAt.trim() : '';
  if (!id || !label || !createdAt) return invalidPayload(message);
  return { ok: true, data: { id, label, createdAt } satisfies BridgeSnapshotItem };
}

function validateRestoreResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || !isRecord(data.state)) return invalidPayload(message);
  return result;
}

function callOptional(method: keyof BraseiroGameBridgeContract, args: unknown[] = []): BridgeSubmitResult {
  const bridge = bridgeOrError();
  if (!isBridge(bridge)) return bridge as BridgeSubmitResult;
  const fn = bridge[method];
  if (typeof fn !== 'function') return { ok: false, error: 'UNSUPPORTED', message: 'Esta capacidade exige uma versão compatível do Motor Barbara.' };
  try {
    return normalizeResult((fn as (...values: unknown[]) => unknown).apply(bridge, args), 'Resposta inválida da ponte do motor.');
  } catch (error) {
    return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : String(error ?? 'Falha no motor.') };
  }
}

export function getBridgeAvailability(): BridgeAvailability {
  const bridge = currentBridge();
  if (bridge === undefined) return 'unavailable';
  return isBridge(bridge) ? 'available' : 'invalid';
}

export function getDataBridgeCapabilities() {
  const bridge = currentBridge();
  if (!isBridge(bridge)) return { characterJson: false, campaignBackup: false, snapshots: false, randomCharacter: false };
  return {
    characterJson: typeof bridge.exportCharacterJson === 'function' && typeof bridge.importCharacterJson === 'function',
    campaignBackup: typeof bridge.exportCampaignBackup === 'function' && typeof bridge.importCampaignBackup === 'function',
    snapshots: typeof bridge.listSnapshots === 'function' && typeof bridge.createSnapshot === 'function' && typeof bridge.restoreSnapshot === 'function',
    randomCharacter: typeof bridge.generateRandomCharacter === 'function',
  };
}

export async function submitPlayerIntentThroughBridge(text: string): Promise<BridgeSubmitResult> {
  const intent = String(text ?? '').trim();
  if (!intent) return { ok: false, error: 'INVALID_INPUT', message: 'Digite uma ação.' };

  const bridge = bridgeOrError();
  if (!isBridge(bridge)) return bridge as BridgeSubmitResult;

  try {
    return normalizeResult(await bridge.submitPlayerIntent(intent), 'Resposta inválida do canal do Mestre.');
  } catch (error) {
    return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : String(error ?? 'Falha ao enviar a ação.') };
  }
}

export function exportCharacterJsonThroughBridge(): BridgeSubmitResult {
  return validateFilePayloadResult(callOptional('exportCharacterJson'), 'Arquivo de ficha inválido retornado pelo motor.');
}
function validJsonInput(text: string, label: string): BridgeSubmitResult | null {
  const value = String(text ?? '').trim();
  if (!value) return { ok: false, error: 'INVALID_INPUT', message: `${label} está vazio.` };
  try {
    JSON.parse(value);
    return null;
  } catch {
    return { ok: false, error: 'INVALID_INPUT', message: `${label} não contém JSON válido.` };
  }
}

export function importCharacterJsonThroughBridge(text: string): BridgeSubmitResult {
  const invalid = validJsonInput(text, 'O arquivo da ficha');
  if (invalid) return invalid;
  return validateCharacterMutationResult(
    callOptional('importCharacterJson', [text, { createSnapshot: true }]),
    'Resultado de importação de ficha inválido retornado pelo motor.',
  );
}
export function exportCampaignBackupThroughBridge(): BridgeSubmitResult {
  return validateFilePayloadResult(callOptional('exportCampaignBackup'), 'Arquivo de backup inválido retornado pelo motor.');
}
export function importCampaignBackupThroughBridge(text: string): BridgeSubmitResult {
  const invalid = validJsonInput(text, 'O backup da campanha');
  if (invalid) return invalid;
  return validateCampaignImportResult(
    callOptional('importCampaignBackup', [text, { createSnapshot: true }]),
    'Resultado de importação de backup inválido retornado pelo motor.',
  );
}
export function listSnapshotsThroughBridge(): BridgeSubmitResult {
  const result = callOptional('listSnapshots');
  if (!result.ok) return result;
  if (!Array.isArray(result.data)) return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };

  const items: BridgeSnapshotItem[] = [];
  const ids = new Set<string>();
  for (const value of result.data) {
    if (!value || typeof value !== 'object') return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };
    const candidate = value as Partial<BridgeSnapshotItem>;
    const id = typeof candidate.id === 'string' ? candidate.id.trim() : '';
    const label = typeof candidate.label === 'string' ? candidate.label.trim() : '';
    const createdAt = typeof candidate.createdAt === 'string' ? candidate.createdAt.trim() : '';
    if (!id || !label || !createdAt || ids.has(id)) return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };
    ids.add(id);
    items.push({ id, label, createdAt });
  }
  return { ok: true, data: items };
}
export function createSnapshotThroughBridge(label = 'Backup manual'): BridgeSubmitResult {
  return validateSnapshotItemResult(callOptional('createSnapshot', [label]), 'Snapshot inválido retornado pelo motor.');
}
export function restoreSnapshotThroughBridge(id: string): BridgeSubmitResult {
  return validateRestoreResult(callOptional('restoreSnapshot', [id]), 'Estado restaurado inválido retornado pelo motor.');
}
export function generateRandomCharacterThroughBridge(options: { name: string; motivation: string; partySize?: number }): BridgeSubmitResult {
  return validateCharacterMutationResult(
    callOptional('generateRandomCharacter', [options, { createSnapshot: true }]),
    'Ficha aleatória inválida retornada pelo motor.',
  );
}


export function startSessionThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('startSession', [options]); }
export function closeSessionThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('closeSession', [options]); }
export function encumbranceStatusThroughBridge(): BridgeSubmitResult { return callOptional('encumbranceStatus'); }
export function liftingDifficultyThroughBridge(objectEncumbrance: number, options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('liftingDifficulty', [objectEncumbrance, options]); }
export function statusSummaryThroughBridge(): BridgeSubmitResult { return callOptional('statusSummary'); }
export function applyStatusThroughBridge(payload: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('applyStatus', [payload]); }
export function destinySummaryThroughBridge(): BridgeSubmitResult { return callOptional('destinySummary', []); }
export function changeCombatRangeThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('changeCombatRange', [options]); }
export function takeCoverThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('takeCover', [options]); }
export function takeGuardedStanceThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('takeGuardedStance', [options]); }
export function dropProneThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('dropProne', [options]); }
export function drawWeaponInCombatThroughBridge(weaponId: string, options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('drawWeaponInCombat', [weaponId, options]); }
export function spendDestinyThroughBridge(side: 'light' | 'dark'): BridgeSubmitResult { return callOptional('spendDestiny', [side]); }
export function resolveFearCheckThroughBridge(options: Record<string, unknown> = {}): BridgeSubmitResult { return callOptional('resolveFearCheck', [options]); }


function normalizeCatalogWeapon(value: unknown): BridgeCatalogWeapon | null {
  if (!isRecord(value)) return null;
  const id = typeof value.id === 'string' ? value.id.trim() : '';
  const name = typeof value.name === 'string' ? value.name.trim() : '';
  const price = Number(value.price);
  const rarity = Number(value.rarity);
  const damageRaw = value.damage;
  const damageBonusRaw = value.damageBonus;
  const critRaw = value.crit;
  const range = typeof value.range === 'string' ? value.range.trim() : '';
  if (!id || !name || !Number.isInteger(price) || price < 0 || !Number.isInteger(rarity) || rarity < 0 || rarity > 10 || typeof value.restricted !== 'boolean' || !range) return null;
  const nullableInt = (raw: unknown) => raw === null || raw === undefined ? null : Number.isInteger(Number(raw)) ? Number(raw) : NaN;
  const damage = nullableInt(damageRaw); const damageBonus = nullableInt(damageBonusRaw); const crit = nullableInt(critRaw);
  if (Number.isNaN(damage) || Number.isNaN(damageBonus) || Number.isNaN(crit)) return null;
  if (!Array.isArray(value.qualities) || value.qualities.some((item) => typeof item !== 'string')) return null;
  return { id, name, price, rarity, restricted: value.restricted, damage, damageBonus, crit, range, qualities: [...value.qualities] };
}

function validStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every(item => typeof item === 'string' && !!item.trim());
}

function validateCreationCatalogResult(result: BridgeSubmitResult): BridgeSubmitResult {
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || data.profile !== 'EOTE_CORE' || !Array.isArray(data.species) || !Array.isArray(data.careers) || !validStringArray(data.obligationTypes) || !isRecord(data.characteristicLabels) || !isRecord(data.skillLabels)) {
    return invalidPayload('Catálogo de criação inválido retornado pelo motor.');
  }
  const species: BridgeCreationSpecies[] = [];
  const speciesKeys = new Set<string>();
  for (const raw of data.species) {
    if (!isRecord(raw) || typeof raw.key !== 'string' || !raw.key.trim() || speciesKeys.has(raw.key) || typeof raw.label !== 'string' || !raw.label.trim() || !isRecord(raw.characteristics)) return invalidPayload('Catálogo de espécies inválido retornado pelo motor.');
    const characteristics: Record<string, number> = {};
    for (const [key, value] of Object.entries(raw.characteristics)) { if (!Number.isInteger(value) || Number(value) < 1 || Number(value) > 5) return invalidPayload('Características de espécie inválidas.'); characteristics[key] = Number(value); }
    const numeric = ['woundBase','strainBase','xp','careerFreeCount','specializationFreeCount'] as const;
    if (numeric.some(key => !Number.isInteger(raw[key]) || Number(raw[key]) < 0)) return invalidPayload('Metadados de espécie inválidos.');
    const freeSkill = raw.freeSkill === null ? null : isRecord(raw.freeSkill) && typeof raw.freeSkill.skill === 'string' && raw.freeSkill.skill.trim() ? { skill: raw.freeSkill.skill } : undefined;
    if (freeSkill === undefined || !validStringArray(raw.freeTalents) || !validStringArray(raw.abilities)) return invalidPayload('Metadados de espécie inválidos.');
    let speciesChoice: BridgeCreationSpecies['speciesChoice'] = null;
    if (raw.speciesChoice !== null) {
      if (!isRecord(raw.speciesChoice) || (raw.speciesChoice.type !== 'oneSkill' && raw.speciesChoice.type !== 'twoNonCareer' && raw.speciesChoice.type !== 'gandRespiration')) return invalidPayload('Escolha de espécie inválida.');
      if (raw.speciesChoice.skills !== undefined && !validStringArray(raw.speciesChoice.skills)) return invalidPayload('Escolha de espécie inválida.');
      if (raw.speciesChoice.variants !== undefined && !validStringArray(raw.speciesChoice.variants)) return invalidPayload('Escolha de espécie inválida.');
      speciesChoice = { type: raw.speciesChoice.type, ...(raw.speciesChoice.skills ? { skills: [...raw.speciesChoice.skills] as string[] } : {}), ...(raw.speciesChoice.variants ? { variants: [...raw.speciesChoice.variants] as string[] } : {}) };
    }
    speciesKeys.add(raw.key);
    species.push({ key: raw.key, label: raw.label, characteristics, woundBase:Number(raw.woundBase), strainBase:Number(raw.strainBase), xp:Number(raw.xp), careerFreeCount:Number(raw.careerFreeCount), specializationFreeCount:Number(raw.specializationFreeCount), freeSkill, freeTalents:[...raw.freeTalents] as string[], speciesChoice, abilities:[...raw.abilities] as string[] });
  }
  const careers: BridgeCreationCareer[] = [];
  const careerKeys = new Set<string>();
  for (const raw of data.careers) {
    if (!isRecord(raw) || typeof raw.key !== 'string' || !raw.key.trim() || careerKeys.has(raw.key) || typeof raw.label !== 'string' || !raw.label.trim() || !validStringArray(raw.skills) || !Array.isArray(raw.specializations)) return invalidPayload('Catálogo de carreiras inválido retornado pelo motor.');
    const specializations: BridgeCreationSpecialization[] = [];
    const specKeys = new Set<string>();
    for (const spec of raw.specializations) { if (!isRecord(spec) || typeof spec.key !== 'string' || !spec.key.trim() || specKeys.has(spec.key) || typeof spec.label !== 'string' || !spec.label.trim() || !validStringArray(spec.skills)) return invalidPayload('Catálogo de especializações inválido retornado pelo motor.'); specKeys.add(spec.key); specializations.push({key:spec.key,label:spec.label,skills:[...spec.skills] as string[]}); }
    careerKeys.add(raw.key); careers.push({key:raw.key,label:raw.label,skills:[...raw.skills] as string[],specializations});
  }
  const characteristicLabels = Object.fromEntries(Object.entries(data.characteristicLabels).filter((entry): entry is [string,string] => typeof entry[1] === 'string' && !!entry[1].trim()));
  const skillLabels = Object.fromEntries(Object.entries(data.skillLabels).filter((entry): entry is [string,string] => typeof entry[1] === 'string' && !!entry[1].trim()));
  if (!species.length || !careers.length || !Object.keys(characteristicLabels).length || !Object.keys(skillLabels).length) return invalidPayload('Catálogo de criação incompleto retornado pelo motor.');
  return { ok:true, data:{ profile:'EOTE_CORE', species, careers, obligationTypes:[...data.obligationTypes], characteristicLabels, skillLabels } satisfies BridgeCreationCatalog };
}

function validateCreationPreviewResult(result: BridgeSubmitResult): BridgeSubmitResult {
  if (!result.ok) return result;
  const data=result.data;
  if (!isRecord(data)) return invalidPayload('Preview de criação inválido retornado pelo motor.');
  for (const key of ['xpBudget','characteristicCost','xpRemaining','credits','obligation','startingObligation','additionalObligation','speciesXpBonus']) if (typeof data[key] !== 'number' || !Number.isFinite(data[key])) return invalidPayload('Preview de criação inválido retornado pelo motor.');
  if (typeof data.obligationOfficial !== 'boolean' || !(data.obligationNote === null || typeof data.obligationNote === 'string')) return invalidPayload('Preview de criação inválido retornado pelo motor.');
  return {ok:true,data:data as BridgeCreationPreview};
}

export function getCreationBridgeCapabilities() {
  const bridge=currentBridge();
  if (!isBridge(bridge)) return { catalog:false, preview:false, create:false };
  return { catalog:typeof bridge.creationCatalog==='function', preview:typeof bridge.creationPreview==='function', create:typeof bridge.createCustomCharacter==='function' };
}

export function loadCreationCatalogThroughBridge(): BridgeSubmitResult { return validateCreationCatalogResult(callOptional('creationCatalog')); }
export function previewCharacterCreationThroughBridge(options: Partial<BridgeCreationOptions>): BridgeSubmitResult { return validateCreationPreviewResult(callOptional('creationPreview',[options])); }
export function createCustomCharacterThroughBridge(options: BridgeCreationOptions): BridgeSubmitResult {
  if (!options.name?.trim() || !options.motivation?.trim()) return {ok:false,error:'INVALID_INPUT',message:'Nome e motivação são obrigatórios.'};
  return validateCharacterMutationResult(callOptional('createCustomCharacter',[options,{createSnapshot:true}]),'Resultado de criação manual inválido retornado pelo motor.');
}

export function getEquipmentBridgeCapabilities() {
  const bridge = currentBridge();
  if (!isBridge(bridge)) return { catalog: false, proposeWeapon: false };
  return { catalog: typeof bridge.equipmentCatalog === 'function', proposeWeapon: typeof bridge.proposeWeaponAcquisition === 'function' };
}

export function loadEquipmentCatalogThroughBridge(): BridgeSubmitResult {
  const result = callOptional('equipmentCatalog');
  if (!result.ok) return result;
  if (!isRecord(result.data) || !Array.isArray(result.data.weapons)) return invalidPayload('Catálogo de equipamento inválido retornado pelo motor.');
  const weapons: BridgeCatalogWeapon[] = [];
  const ids = new Set<string>();
  for (const raw of result.data.weapons) {
    const item = normalizeCatalogWeapon(raw);
    if (!item || ids.has(item.id)) return invalidPayload('Catálogo de armas inválido retornado pelo motor.');
    ids.add(item.id); weapons.push(item);
  }
  return { ok: true, data: { weapons } satisfies BridgeEquipmentCatalog };
}

export function proposeWeaponAcquisitionThroughBridge(itemId: string): BridgeSubmitResult {
  const id = String(itemId ?? '').trim();
  if (!id) return { ok: false, error: 'INVALID_INPUT', message: 'Selecione uma arma do catálogo auditado.' };
  const result = callOptional('proposeWeaponAcquisition', [id]);
  if (!result.ok) return result;
  const data = result.data;
  if (!isRecord(data) || !isRecord(data.state) || !isRecord(data.pendingAcquisition)) return invalidPayload('Pedido de aquisição inválido retornado pelo motor.');
  const pending = data.pendingAcquisition;
  if (typeof pending.itemId !== 'string' || pending.itemId !== id || typeof pending.itemName !== 'string' || !pending.itemName.trim() || pending.status !== 'aguardando_mestre') {
    return invalidPayload('Pedido de aquisição inválido retornado pelo motor.');
  }
  return result;
}


function validateImageAssetValue(value: unknown): BridgeImageAsset | null {
  if (!isRecord(value)) return null;
  const assetId = typeof value.assetId === 'string' ? value.assetId.trim() : '';
  const title = typeof value.title === 'string' ? value.title.trim() : '';
  const category = typeof value.category === 'string' ? value.category.trim() : '';
  const sourceType = typeof value.sourceType === 'string' ? value.sourceType.trim() : '';
  if (!assetId || !title || !category || !sourceType || !Array.isArray(value.tags) || value.tags.some((tag) => typeof tag !== 'string')) return null;
  if (!(value.mechanicalBinding === null || isRecord(value.mechanicalBinding))) return null;
  const stringOpt = (raw: unknown) => typeof raw === 'string' && raw.trim() ? raw.trim() : undefined;
  const asset: BridgeImageAsset = {
    assetId, title, category, sourceType, tags: [...value.tags] as string[], mechanicalBinding: value.mechanicalBinding as Record<string, unknown> | null,
  };
  for (const [key, raw] of Object.entries(value)) {
    if (['subtype','mapKind','sourceName','sourceRef','sourcePageUrl','remoteUrl','cachePolicy','localPath','usage','publishedAt'].includes(key)) {
      const clean = stringOpt(raw); if (clean) (asset as Record<string, unknown>)[key] = clean;
    }
  }
  if (typeof value.remoteReady === 'boolean') asset.remoteReady = value.remoteReady;
  if (typeof value.offlineReady === 'boolean') asset.offlineReady = value.offlineReady;
  if (value.variants !== undefined) {
    if (!Array.isArray(value.variants)) return null;
    const variants: Array<{variantId:string; title?:string; remoteUrl:string}> = [];
    for (const raw of value.variants) {
      if (!isRecord(raw) || typeof raw.variantId !== 'string' || !raw.variantId.trim() || typeof raw.remoteUrl !== 'string' || !raw.remoteUrl.trim()) return null;
      variants.push({variantId:raw.variantId.trim(), remoteUrl:raw.remoteUrl.trim(), ...(typeof raw.title === 'string' && raw.title.trim() ? {title:raw.title.trim()} : {})});
    }
    asset.variants = variants;
  }
  if (sourceType === 'external_reference' && (!asset.sourcePageUrl || (asset.remoteReady && !asset.remoteUrl))) return null;
  if (sourceType !== 'external_reference' && !asset.localPath) return null;
  return asset;
}

function validateImageAssetListResult(result: BridgeSubmitResult, message: string): BridgeSubmitResult {
  if (!result.ok) return result;
  if (!Array.isArray(result.data)) return invalidPayload(message);
  const items: BridgeImageAsset[] = [];
  const ids = new Set<string>();
  for (const raw of result.data) {
    const item = validateImageAssetValue(raw);
    if (!item || ids.has(item.assetId)) return invalidPayload(message);
    ids.add(item.assetId); items.push(item);
  }
  return { ok: true, data: items };
}

export function getImageLibraryBridgeCapabilities() {
  const bridge = currentBridge();
  if (!isBridge(bridge)) return { info:false, search:false, maps:false, visuals:false, asset:false, runtime:false };
  return {
    info: typeof bridge.imageLibraryInfo === 'function', search: typeof bridge.searchImageLibrary === 'function', maps: typeof bridge.imageMapCatalog === 'function', visuals: typeof bridge.imageVisualEntityCatalog === 'function',
    asset: typeof bridge.imageAsset === 'function', runtime: typeof bridge.imageAssetRuntime === 'function',
  };
}

export function loadImageLibraryInfoThroughBridge(): BridgeSubmitResult {
  const result = callOptional('imageLibraryInfo');
  if (!result.ok) return result;
  const data=result.data;
  if (!isRecord(data) || typeof data.libraryId !== 'string' || !data.libraryId.trim() || !Number.isInteger(data.schemaVersion) || !isRecord(data.counts) || !isRecord(data.policy)) return invalidPayload('Informações da biblioteca de imagens inválidas.');
  const counts: Record<string,number> = {};
  for (const [key,value] of Object.entries(data.counts)) { if (typeof value !== 'number' || !Number.isInteger(value) || value < 0) return invalidPayload('Contadores da biblioteca de imagens inválidos.'); counts[key]=value; }
  return {ok:true,data:{libraryId:data.libraryId,schemaVersion:Number(data.schemaVersion),counts,policy:data.policy} satisfies BridgeImageLibraryInfo};
}

export function searchImageLibraryThroughBridge(criteria: Record<string, unknown> = {}): BridgeSubmitResult {
  return validateImageAssetListResult(callOptional('searchImageLibrary',[criteria]), 'Busca da biblioteca de imagens inválida.');
}

export function loadImageMapCatalogThroughBridge(criteria: Record<string, unknown> = {}): BridgeSubmitResult {
  const result = validateImageAssetListResult(callOptional('imageMapCatalog',[criteria]), 'Catálogo de mapas inválido retornado pelo motor.');
  if (!result.ok) return result;
  if ((result.data as BridgeImageAsset[]).some((item)=>item.category!=='map')) return invalidPayload('Catálogo de mapas contém asset de categoria incorreta.');
  return result;
}

export function loadImageVisualEntityCatalogThroughBridge(criteria: Record<string, unknown> = {}): BridgeSubmitResult {
  const result = validateImageAssetListResult(callOptional('imageVisualEntityCatalog',[criteria]), 'Catálogo visual de NPCs/droides/criaturas/veículos inválido retornado pelo motor.');
  if (!result.ok) return result;
  const allowed = new Set(['portrait','droid','creature','vehicle']);
  if ((result.data as BridgeImageAsset[]).some((item)=>!allowed.has(item.category) || item.mechanicalBinding !== null)) return invalidPayload('Catálogo visual contém asset incompatível ou vínculo mecânico implícito.');
  return result;
}

export function getImageAssetThroughBridge(assetId: string): BridgeSubmitResult {
  const id=String(assetId??'').trim();
  if (!id) return {ok:false,error:'INVALID_INPUT',message:'Informe o assetId da imagem.'};
  const result=callOptional('imageAsset',[id]);
  if (!result.ok) return result;
  const asset=validateImageAssetValue(result.data);
  return asset ? {ok:true,data:asset} : invalidPayload('Asset de imagem inválido retornado pelo motor.');
}

export function getImageAssetRuntimeThroughBridge(assetId: string): BridgeSubmitResult {
  const id=String(assetId??'').trim();
  if (!id) return {ok:false,error:'INVALID_INPUT',message:'Informe o assetId da imagem.'};
  const result=callOptional('imageAssetRuntime',[id]);
  if (!result.ok) return result;
  const data=result.data;
  if (!isRecord(data) || data.assetId!==id || (data.mode!=='packaged' && data.mode!=='external_cache_required') || typeof data.offlineReady!=='boolean') return invalidPayload('Descritor de runtime de imagem inválido.');
  if (data.mode==='packaged' && (typeof data.localPath!=='string' || !data.localPath.trim())) return invalidPayload('Descritor de imagem local inválido.');
  if (data.mode==='external_cache_required' && typeof data.remoteReady!=='boolean') return invalidPayload('Descritor de imagem externa inválido.');
  return result as BridgeSubmitResult;
}


export function downloadBridgeJson(payload: unknown): BridgeSubmitResult {
  if (typeof window === 'undefined') return { ok: false, error: 'UNSUPPORTED', message: 'Download indisponível neste ambiente.' };
  const value = payload as Partial<BridgeFilePayload> | null;
  if (!value || typeof value.fileName !== 'string' || !value.fileName.trim() || typeof value.json !== 'string' || !value.json.trim()) {
    return { ok: false, error: 'INVALID_BRIDGE', message: 'Arquivo retornado pelo motor é inválido.' };
  }
  try {
    JSON.parse(value.json);
  } catch {
    return { ok: false, error: 'INVALID_BRIDGE', message: 'O motor retornou um arquivo que não contém JSON válido.' };
  }
  try {
    const blob = new Blob([value.json], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = value.fileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    return { ok: true, data: { fileName: value.fileName } };
  } catch (error) {
    return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : 'Falha ao exportar arquivo.' };
  }
}
