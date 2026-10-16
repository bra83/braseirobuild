'use client';


import { useCallback, useEffect, useMemo, useState } from 'react';
import type { QaState } from '@/src/config/screens';
import { getScreen, isQaState, mainNavigation, qaStateLabels, screenRegistry, utilityNavigation } from '@/src/config/screens';
import { AppShell, BottomNavigation, BottomSheet, ListItem, TopBar } from './primitives';
import { DebugGallery } from './DebugGallery';
import { renderTemplate } from './Templates';
import { EMPTY_R21_READ_MODEL, R21_STATE_CHANGED_EVENT, R21_STORAGE_KEY, readR21CampaignState, type R21ReadModel } from '@/src/state/r21Projection';
import {
  createSnapshotThroughBridge, downloadBridgeJson, exportCampaignBackupThroughBridge, exportCharacterJsonThroughBridge,
  generateRandomCharacterThroughBridge, getBridgeAvailability, getDataBridgeCapabilities, importCampaignBackupThroughBridge, importCharacterJsonThroughBridge,
  listSnapshotsThroughBridge, restoreSnapshotThroughBridge, submitPlayerIntentThroughBridge,
  getEquipmentBridgeCapabilities, loadEquipmentCatalogThroughBridge, proposeWeaponAcquisitionThroughBridge,
  getCreationBridgeCapabilities, loadCreationCatalogThroughBridge, previewCharacterCreationThroughBridge, createCustomCharacterThroughBridge,
  getImageLibraryBridgeCapabilities, loadImageLibraryInfoThroughBridge, loadImageMapCatalogThroughBridge, loadImageVisualEntityCatalogThroughBridge,
  type BridgeAvailability, type BridgeCatalogWeapon, type BridgeDataState, type BridgeSnapshotItem, type BridgeSubmitState,
  type BridgeCreationCatalog, type BridgeCreationOptions, type BridgeCreationPreview, type BridgeImageAsset, type BridgeImageLibraryInfo,
} from '@/src/state/gameBridgeClient';
import {
  getLibraryBridgeAvailability, getLibraryCapabilities, listLibraryDocumentsThroughBridge, parseLibraryNativeEvent,
  reindexLibraryDocumentThroughBridge, removeLibraryDocumentThroughBridge, requestPdfImportThroughBridge, searchLibraryThroughBridge,
  updateLibraryDocumentPolicyThroughBridge, type LibraryAuthorityType, type LibraryBridgeAvailability, type LibraryDocument, type LibrarySearchHit,
} from '@/src/state/libraryBridgeClient';
import {
  buildGeminiScenePrompt, clearGeminiApiKeyThroughBridge, clearGeminiSceneImageThroughBridge, configureGeminiApiKeyThroughBridge,
  generateGeminiSceneImageThroughBridge, getGeminiBridgeAvailability, getGeminiCapabilities, getGeminiStatusThroughBridge,
  parseGeminiNativeEvent, requestGeminiSceneImageImportThroughBridge,
  type GeminiBridgeAvailability, type GeminiStatus,
} from '@/src/state/geminiBridgeClient';
import {
  browserExternalFallbackUrl, cacheExternalImage, clearCachedImage, getImageCacheStatus, getImageLibraryNativeAvailability,
  parseImageLibraryNativeEvent, packagedImageUrl, type ImageCacheStatus, type ImageLibraryNativeAvailability,
} from '@/src/state/imageLibraryBridgeClient';


const allowedRoutes = new Set(screenRegistry.map((item) => item.route));


function readRoute() {
  if (typeof window === 'undefined') return '00';
  const requested = new URLSearchParams(window.location.search).get('screen') ?? '00';
  return allowedRoutes.has(requested) ? requested : '00';
}


function readState(): QaState {
  if (typeof window === 'undefined') return 'default';
  const requested = new URLSearchParams(window.location.search).get('state') ?? 'default';
  return isQaState(requested) ? requested : 'default';
}


function readDebugMode() {
  if (typeof window === 'undefined') return false;
  return new URLSearchParams(window.location.search).get('debug') === '1';
}


const rootRouteByDomain: Record<string, string> = {
  Cena: '00',
  Personagem: '06',
  Itens: '08',
  Nave: '10',
  Registros: '12',
};


const MAX_JSON_IMPORT_BYTES = 16 * 1024 * 1024;

const EMPTY_CREATION_DRAFT: BridgeCreationOptions = {
  creationOrigin: 'EOTE', name: '', background: '', speciesKey: '', careerKey: '', specializationKey: '', partySize: 1,
  speciesVariant: null, obligationType: '', obligationBenefits: { xp5: false, xp10: false, credits1000: false, credits2500: false },
  characteristics: {}, careerFreeSkills: [], specializationFreeSkills: [], speciesChoiceSkills: [], motivation: '',
};

export function VTTApp() {
  const [route, setRoute] = useState('00');
  const [qaState, setQaStateValue] = useState<QaState>('default');
  const [sceneDraft, setSceneDraft] = useState('');
  const [statusOpen, setStatusOpen] = useState(false);
  const [debugMode, setDebugMode] = useState(false);
  const [campaignRead, setCampaignRead] = useState<R21ReadModel>(EMPTY_R21_READ_MODEL);
  const [masterBridge, setMasterBridge] = useState<BridgeAvailability>('unavailable');
  const [intentSubmitState, setIntentSubmitState] = useState<BridgeSubmitState>('idle');
  const [intentSubmitMessage, setIntentSubmitMessage] = useState('');
  const [dataCapabilities, setDataCapabilities] = useState({ characterJson: false, campaignBackup: false, snapshots: false, randomCharacter: false });
  const [dataActionState, setDataActionState] = useState<BridgeDataState>('idle');
  const [dataActionMessage, setDataActionMessage] = useState('');
  const [snapshots, setSnapshots] = useState<BridgeSnapshotItem[]>([]);
  const [equipmentCapabilities, setEquipmentCapabilities] = useState({ catalog: false, proposeWeapon: false });
  const [equipmentWeapons, setEquipmentWeapons] = useState<BridgeCatalogWeapon[]>([]);
  const [equipmentActionState, setEquipmentActionState] = useState<BridgeDataState>('idle');
  const [equipmentActionMessage, setEquipmentActionMessage] = useState('');
  const [creationCapabilities, setCreationCapabilities] = useState({ catalog: false, preview: false, create: false });
  const [creationCatalog, setCreationCatalog] = useState<BridgeCreationCatalog | null>(null);
  const [creationDraft, setCreationDraft] = useState<BridgeCreationOptions>(EMPTY_CREATION_DRAFT);
  const [creationPreview, setCreationPreview] = useState<BridgeCreationPreview | null>(null);
  const [creationPreviewError, setCreationPreviewError] = useState('');
  const [creationActionState, setCreationActionState] = useState<BridgeDataState>('idle');
  const [creationActionMessage, setCreationActionMessage] = useState('');
  const [imageLibraryCapabilities, setImageLibraryCapabilities] = useState({ info:false, search:false, maps:false, visuals:false, asset:false, runtime:false });
  const [imageLibraryInfo, setImageLibraryInfo] = useState<BridgeImageLibraryInfo | null>(null);
  const [imageMaps, setImageMaps] = useState<BridgeImageAsset[]>([]);
  const [imageMapQuery, setImageMapQuery] = useState('');
  const [imageVisualEntities, setImageVisualEntities] = useState<BridgeImageAsset[]>([]);
  const [imageVisualQuery, setImageVisualQuery] = useState('');
  const [imageVisualKind, setImageVisualKind] = useState<'all'|'npc'|'droid'|'beast'|'vehicle'>('all');
  const [selectedImageAssetId, setSelectedImageAssetId] = useState<string | null>(null);
  const [imageCacheStatuses, setImageCacheStatuses] = useState<Record<string, ImageCacheStatus>>({});
  const [imageLibraryNative, setImageLibraryNative] = useState<ImageLibraryNativeAvailability>('unavailable');
  const [imageActionState, setImageActionState] = useState<BridgeDataState>('idle');
  const [imageActionMessage, setImageActionMessage] = useState('');
  const [libraryBridge, setLibraryBridge] = useState<LibraryBridgeAvailability>('unavailable');
  const [libraryCapabilities, setLibraryCapabilities] = useState({ list: false, search: false, importPdf: false, policy: false, reindex: false, remove: false });
  const [libraryDocuments, setLibraryDocuments] = useState<LibraryDocument[]>([]);
  const [libraryActionState, setLibraryActionState] = useState<BridgeDataState>('idle');
  const [libraryActionMessage, setLibraryActionMessage] = useState('');
  const [librarySearchResults, setLibrarySearchResults] = useState<LibrarySearchHit[]>([]);
  const [librarySearchGrounded, setLibrarySearchGrounded] = useState(false);
  const [geminiBridge, setGeminiBridge] = useState<GeminiBridgeAvailability>('unavailable');
  const [geminiCapabilities, setGeminiCapabilities] = useState({ status: false, configure: false, clearKey: false, generate: false, importImage: false, clearImage: false });
  const [geminiStatus, setGeminiStatus] = useState<GeminiStatus | null>(null);
  const [geminiActionState, setGeminiActionState] = useState<BridgeDataState>('idle');
  const [geminiActionMessage, setGeminiActionMessage] = useState('');

  const sceneImagePrompt = useMemo(() => campaignRead.status === 'ready' ? buildGeminiScenePrompt(campaignRead.scene) : null, [campaignRead]);
  const sceneImageUrl = geminiStatus?.imageUrl && sceneImagePrompt && geminiStatus.imageSceneKey === sceneImagePrompt.sceneKey ? geminiStatus.imageUrl : null;
  const selectedImageAsset = useMemo(() => selectedImageAssetId ? imageMaps.find(item => item.assetId === selectedImageAssetId) ?? null : null, [imageMaps, selectedImageAssetId]);
  const selectedImageCache = selectedImageAssetId ? imageCacheStatuses[selectedImageAssetId] ?? null : null;
  const selectedImageUrl = useMemo(() => {
    if (!selectedImageAsset) return null;
    if (selectedImageAsset.sourceType !== 'external_reference') {
      if (typeof window !== 'undefined' && window.location.protocol !== 'file:') return selectedImageAsset.localPath ? `/${selectedImageAsset.localPath.replace(/^\/+/, '')}` : null;
      return packagedImageUrl(selectedImageAsset);
    }
    if (selectedImageCache?.cached && selectedImageCache.runtimeUrl) return selectedImageCache.runtimeUrl;
    return imageLibraryNative === 'unavailable' ? browserExternalFallbackUrl(selectedImageAsset) : null;
  }, [selectedImageAsset, selectedImageCache, imageLibraryNative]);


  useEffect(() => {
    const sync = () => {
      setRoute(readRoute());
      setQaStateValue(readState());
      setDebugMode(readDebugMode());
    };
    sync();
    addEventListener('popstate', sync);
    return () => removeEventListener('popstate', sync);
  }, []);


  const refreshCampaignRead = useCallback(() => {
    setCampaignRead(readR21CampaignState());
  }, []);

  const refreshMasterBridge = useCallback(() => {
    setMasterBridge(getBridgeAvailability());
    setDataCapabilities(getDataBridgeCapabilities());
    setEquipmentCapabilities(getEquipmentBridgeCapabilities());
    setCreationCapabilities(getCreationBridgeCapabilities());
    setImageLibraryCapabilities(getImageLibraryBridgeCapabilities());
    setImageLibraryNative(getImageLibraryNativeAvailability());
  }, []);

  const refreshEquipmentCatalog = useCallback((silent = false) => {
    const capabilities = getEquipmentBridgeCapabilities();
    setEquipmentCapabilities(capabilities);
    if (!capabilities.catalog) {
      setEquipmentWeapons([]);
      if (!silent && getBridgeAvailability() === 'invalid') {
        setEquipmentActionState('error');
        setEquipmentActionMessage('Ponte de equipamento incompatível com esta shell.');
      }
      return false;
    }
    const result = loadEquipmentCatalogThroughBridge();
    if (!result.ok) {
      setEquipmentWeapons([]);
      if (!silent) { setEquipmentActionState('error'); setEquipmentActionMessage(result.message); }
      return false;
    }
    setEquipmentWeapons((result.data as { weapons: BridgeCatalogWeapon[] }).weapons);
    return true;
  }, []);

  const refreshCreationCatalog = useCallback((silent = false) => {
    const capabilities = getCreationBridgeCapabilities();
    setCreationCapabilities(capabilities);
    if (!capabilities.catalog) {
      setCreationCatalog(null); setCreationPreview(null);
      if (!silent && getBridgeAvailability() === 'invalid') { setCreationActionState('error'); setCreationActionMessage('Ponte de criação incompatível com esta shell.'); }
      return false;
    }
    const result = loadCreationCatalogThroughBridge(creationDraft.creationOrigin ?? 'EOTE');
    if (!result.ok) {
      setCreationCatalog(null); setCreationPreview(null);
      if (!silent) { setCreationActionState('error'); setCreationActionMessage(result.message); }
      return false;
    }
    setCreationCatalog(result.data as BridgeCreationCatalog);
    return true;
  }, [creationDraft.creationOrigin]);

  const refreshImageLibrary = useCallback((silent = false) => {
    const capabilities = getImageLibraryBridgeCapabilities();
    setImageLibraryCapabilities(capabilities);
    setImageLibraryNative(getImageLibraryNativeAvailability());
    if (!capabilities.info || !capabilities.maps) {
      setImageLibraryInfo(null); setImageMaps([]); setImageVisualEntities([]);
      if (!silent && getBridgeAvailability() === 'invalid') { setImageActionState('error'); setImageActionMessage('Ponte da biblioteca de imagens incompatível com esta shell.'); }
      return false;
    }
    const info = loadImageLibraryInfoThroughBridge();
    const catalog = loadImageMapCatalogThroughBridge({ limit: 500, includePlayableLocations: true });
    const visuals = capabilities.visuals ? loadImageVisualEntityCatalogThroughBridge({ kind:'all', limit:500 }) : {ok:true,data:[]} as const;
    if (!info.ok || !catalog.ok || !visuals.ok) {
      setImageLibraryInfo(null); setImageMaps([]); setImageVisualEntities([]);
      if (!silent) { setImageActionState('error'); setImageActionMessage(!info.ok ? info.message : !catalog.ok ? catalog.message : visuals.message); }
      return false;
    }
    const maps = catalog.data as BridgeImageAsset[];
    const visualItems = visuals.data as BridgeImageAsset[];
    setImageLibraryInfo(info.data as BridgeImageLibraryInfo);
    setImageMaps(maps);
    setImageVisualEntities(visualItems);
    setSelectedImageAssetId(current => current && maps.some(item=>item.assetId===current) ? current : null);
    return true;
  }, []);

  const refreshImageCacheStatus = useCallback((assetId: string, silent = false) => {
    const availability = getImageLibraryNativeAvailability();
    setImageLibraryNative(availability);
    if (availability !== 'available') return false;
    const result = getImageCacheStatus(assetId);
    if (!result.ok) { if (!silent) { setImageActionState('error'); setImageActionMessage(result.message); } return false; }
    setImageCacheStatuses(current => ({...current,[assetId]:result.data}));
    return true;
  }, []);

  const refreshLibraryBridge = useCallback(() => {
    setLibraryBridge(getLibraryBridgeAvailability());
    setLibraryCapabilities(getLibraryCapabilities());
  }, []);

  const refreshGeminiBridge = useCallback((silent = false) => {
    const availability = getGeminiBridgeAvailability();
    setGeminiBridge(availability);
    setGeminiCapabilities(getGeminiCapabilities());
    if (availability !== 'available') {
      setGeminiStatus(null);
      if (!silent && availability === 'invalid') {
        setGeminiActionState('error');
        setGeminiActionMessage('Ponte Gemini incompatível com esta shell.');
      }
      return false;
    }
    const result = getGeminiStatusThroughBridge();
    if (!result.ok) {
      setGeminiStatus(null);
      if (!silent) { setGeminiActionState('error'); setGeminiActionMessage(result.message); }
      return false;
    }
    setGeminiStatus(result.data);
    return true;
  }, []);

  const refreshLibraryDocuments = useCallback((silent = false) => {
    const availability = getLibraryBridgeAvailability();
    setLibraryBridge(availability);
    setLibraryCapabilities(getLibraryCapabilities());
    if (availability !== 'available') {
      setLibraryDocuments([]);
      if (!silent && availability === 'invalid') {
        setLibraryActionState('error');
        setLibraryActionMessage('Ponte da Biblioteca incompatível com esta shell.');
      }
      return false;
    }
    const result = listLibraryDocumentsThroughBridge();
    if (!result.ok) {
      setLibraryDocuments([]);
      if (!silent) { setLibraryActionState('error'); setLibraryActionMessage(result.message); }
      return false;
    }
    setLibraryDocuments(result.data);
    return true;
  }, []);


  useEffect(() => {
    refreshCampaignRead();
    refreshMasterBridge();
    refreshImageLibrary(true);
    refreshLibraryBridge();
    refreshGeminiBridge(true);
    const onStorage = (event: StorageEvent) => {
      if (event.key === null || event.key === R21_STORAGE_KEY) refreshCampaignRead();
    };
    addEventListener('storage', onStorage);
    const onStateChanged = () => {
      refreshCampaignRead();
      refreshMasterBridge();
      refreshImageLibrary(true);
      refreshLibraryBridge();
      refreshGeminiBridge(true);
    };
    addEventListener(R21_STATE_CHANGED_EVENT, onStateChanged);
    const onFocus = () => {
      refreshCampaignRead();
      refreshMasterBridge();
      refreshImageLibrary(true);
      refreshLibraryBridge();
      refreshGeminiBridge(true);
      if (getLibraryBridgeAvailability() === 'available') refreshLibraryDocuments(true);
    };
    addEventListener('focus', onFocus);
    return () => {
      removeEventListener('storage', onStorage);
      removeEventListener(R21_STATE_CHANGED_EVENT, onStateChanged);
      removeEventListener('focus', onFocus);
    };
  }, [refreshCampaignRead, refreshMasterBridge, refreshImageLibrary, refreshLibraryBridge, refreshLibraryDocuments, refreshGeminiBridge]);

  useEffect(() => {
    if ((route === '16' || route === '17') && masterBridge === 'available') refreshImageLibrary(true);
    if (route === '16') setSelectedImageAssetId('local.map.galaxy');
  }, [route, masterBridge, refreshImageLibrary]);

  useEffect(() => {
    if (selectedImageAssetId && imageLibraryNative === 'available') refreshImageCacheStatus(selectedImageAssetId, true);
  }, [selectedImageAssetId, imageLibraryNative, refreshImageCacheStatus]);

  useEffect(() => {
    if ((route === '09' || route === '26') && masterBridge === 'available') refreshEquipmentCatalog(true);
  }, [route, masterBridge, refreshEquipmentCatalog]);

  useEffect(() => {
    const n = Number(route);
    if (n >= 18 && n <= 26 && masterBridge === 'available') refreshCreationCatalog(true);
  }, [route, masterBridge, refreshCreationCatalog]);

  useEffect(() => {
    if (!creationCapabilities.preview || !creationDraft.speciesKey) { setCreationPreview(null); setCreationPreviewError(''); return; }
    const result = previewCharacterCreationThroughBridge(creationDraft);
    if (!result.ok) { setCreationPreview(null); setCreationPreviewError(result.message); return; }
    setCreationPreview(result.data as BridgeCreationPreview); setCreationPreviewError('');
  }, [creationCapabilities.preview, creationDraft]);

  useEffect(() => {
    if (route === '30' && libraryBridge === 'available') refreshLibraryDocuments(true);
  }, [route, libraryBridge, refreshLibraryDocuments]);

  useEffect(() => {
    if ((route === '29' || screenRegistry.find(item => item.route === route)?.domain === 'Cena') && geminiBridge === 'available') refreshGeminiBridge(true);
  }, [route, geminiBridge, refreshGeminiBridge]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const previous = window.braseiroLibraryNativeEvent;
    window.braseiroLibraryNativeEvent = (raw: string) => {
      if (typeof previous === 'function') previous(raw);
      const event = parseLibraryNativeEvent(raw);
      refreshLibraryBridge();
      refreshLibraryDocuments(true);
      if (!event) {
        setLibraryActionState('error');
        setLibraryActionMessage('Evento inválido recebido da Biblioteca Android.');
        return;
      }
      const document = event.payload.document && typeof event.payload.document === 'object' ? event.payload.document as Record<string, unknown> : null;
      const fileName = document && typeof document.fileName === 'string' ? document.fileName : 'PDF';
      const messages: Record<string, string> = {
        cancelado: 'Importação cancelada.',
        indexando: `Anexando e indexando ${fileName}…`,
        concluido: `Biblioteca atualizada: ${fileName}.`,
        duplicado: 'Este PDF já existe na Biblioteca; nenhuma cópia foi criada.',
        removido: 'PDF removido da Biblioteca local.',
        politica: `Autoridade de ${fileName} atualizada.`,
        erro: typeof event.payload.message === 'string' ? event.payload.message : 'Falha ao importar ou indexar o PDF.',
      };
      setLibraryActionState(event.type === 'erro' ? 'error' : 'success');
      setLibraryActionMessage(messages[event.type] ?? 'Biblioteca atualizada.');
    };
    return () => { window.braseiroLibraryNativeEvent = previous; };
  }, [refreshLibraryBridge, refreshLibraryDocuments]);


  useEffect(() => {
    if (typeof window === 'undefined') return;
    const previous = window.braseiroImageLibraryNativeEvent;
    window.braseiroImageLibraryNativeEvent = (raw: string) => {
      if (typeof previous === 'function') previous(raw);
      const event = parseImageLibraryNativeEvent(raw);
      setImageLibraryNative(getImageLibraryNativeAvailability());
      if (!event) { setImageActionState('error'); setImageActionMessage('Evento inválido recebido do cache de imagens.'); return; }
      const assetId = typeof event.data.assetId === 'string' ? event.data.assetId : '';
      if (assetId) refreshImageCacheStatus(assetId, true);
      const messages: Record<string,string> = {
        queued: 'Download do mapa colocado na fila.',
        cached: 'Mapa armazenado no cache privado do aplicativo.',
        cleared: 'Mapa removido do cache privado.',
        error: typeof event.data.message === 'string' ? event.data.message : 'Falha ao obter o mapa externo.',
      };
      setImageActionState(event.type === 'error' ? 'error' : event.type === 'queued' ? 'working' : 'success');
      setImageActionMessage(messages[event.type] ?? 'Biblioteca de imagens atualizada.');
    };
    return () => { window.braseiroImageLibraryNativeEvent = previous; };
  }, [refreshImageCacheStatus]);


  useEffect(() => {
    if (typeof window === 'undefined') return;
    const previous = window.braseiroGeminiNativeEvent;
    window.braseiroGeminiNativeEvent = (raw: string) => {
      if (typeof previous === 'function') previous(raw);
      const event = parseGeminiNativeEvent(raw);
      refreshGeminiBridge(true);
      if (!event) {
        setGeminiActionState('error');
        setGeminiActionMessage('Evento inválido recebido do host Gemini.');
        return;
      }
      const messages: Record<string, string> = {
        configured: 'Chave Gemini protegida no host Android.',
        cleared: 'Chave Gemini removida do dispositivo.',
        generating: 'Gemini está gerando a imagem da cena…',
        generated: 'Imagem da cena gerada e vinculada ao contexto atual.',
        imported: 'Imagem importada e vinculada à cena atual.',
        image_cleared: 'Imagem da cena removida.',
        cancelado: 'Importação de imagem cancelada.',
        erro: typeof event.payload.message === 'string' ? event.payload.message : 'Falha no host Gemini.',
      };
      setGeminiActionState(event.type === 'erro' ? 'error' : event.type === 'generating' ? 'working' : 'success');
      setGeminiActionMessage(messages[event.type] ?? 'Gemini atualizado.');
    };
    return () => { window.braseiroGeminiNativeEvent = previous; };
  }, [refreshGeminiBridge]);


  const navigate = useCallback((next: string) => {
    const safeNext = allowedRoutes.has(next) ? next : '00';
    const url = new URL(window.location.href);
    url.searchParams.set('screen', safeNext);
    url.searchParams.set('state', qaState);
    url.searchParams.delete('debug');
    history.pushState({}, '', url);
    setRoute(safeNext);
    setDebugMode(false);
    window.scrollTo(0, 0);
  }, [qaState]);


  const setQaState = useCallback((next: QaState) => {
    const safeNext = isQaState(next) ? next : 'default';
    const url = new URL(window.location.href);
    url.searchParams.set('state', safeNext);
    history.replaceState({}, '', url);
    setQaStateValue(safeNext);
  }, []);



  const submitSceneIntent = useCallback(async () => {
    if (intentSubmitState === 'sending') return;
    const text = sceneDraft.trim();
    if (!text) {
      setIntentSubmitState('error');
      setIntentSubmitMessage('Digite uma ação.');
      return;
    }

    setIntentSubmitState('sending');
    setIntentSubmitMessage('');
    const result = await submitPlayerIntentThroughBridge(text);
    if (!result.ok) {
      setIntentSubmitState('error');
      setIntentSubmitMessage(result.message);
      refreshMasterBridge();
      return;
    }

    setIntentSubmitState('sent');
    setIntentSubmitMessage('Ação entregue ao Mestre.');
    setSceneDraft('');
    refreshMasterBridge();
    refreshCampaignRead();
  }, [intentSubmitState, sceneDraft, refreshMasterBridge, refreshCampaignRead]);


  const finishDataAction = useCallback((result: { ok: boolean; data?: unknown; message?: string }, successMessage: string, refreshState = false) => {
    if (!result.ok) {
      setDataActionState('error');
      setDataActionMessage(result.message || 'Operação rejeitada pelo motor.');
      refreshMasterBridge();
      return false;
    }
    setDataActionState('success');
    setDataActionMessage(successMessage);
    if (refreshState) refreshCampaignRead();
    refreshMasterBridge();
    return true;
  }, [refreshCampaignRead, refreshMasterBridge]);

  const exportCharacterJson = useCallback(() => {
    setDataActionState('working'); setDataActionMessage('');
    const result = exportCharacterJsonThroughBridge();
    if (!result.ok) return void finishDataAction(result, '');
    const downloaded = downloadBridgeJson(result.data);
    finishDataAction(downloaded, 'Ficha exportada em JSON.');
  }, [finishDataAction]);

  const importCharacterJsonFile = useCallback(async (file: File) => {
    setDataActionState('working'); setDataActionMessage('');
    try {
      if (file.size > MAX_JSON_IMPORT_BYTES) throw new Error('O arquivo JSON excede 16 MB.');
      const result = importCharacterJsonThroughBridge(await file.text());
      finishDataAction(result, 'Ficha importada. A proteção por snapshot foi solicitada ao motor.', true);
    } catch (error) {
      setDataActionState('error');
      setDataActionMessage(error instanceof Error ? error.message : 'Falha ao ler o arquivo JSON.');
    }
  }, [finishDataAction]);

  const exportCampaignBackup = useCallback(() => {
    setDataActionState('working'); setDataActionMessage('');
    const result = exportCampaignBackupThroughBridge();
    if (!result.ok) return void finishDataAction(result, '');
    const downloaded = downloadBridgeJson(result.data);
    finishDataAction(downloaded, 'Backup da campanha exportado.');
  }, [finishDataAction]);

  const importCampaignBackupFile = useCallback(async (file: File) => {
    setDataActionState('working'); setDataActionMessage('');
    try {
      if (file.size > MAX_JSON_IMPORT_BYTES) throw new Error('O backup excede 16 MB.');
      const result = importCampaignBackupThroughBridge(await file.text());
      finishDataAction(result, 'Backup restaurado. A proteção por snapshot foi solicitada ao motor.', true);
    } catch (error) {
      setDataActionState('error');
      setDataActionMessage(error instanceof Error ? error.message : 'Falha ao ler o backup.');
    }
  }, [finishDataAction]);

  const refreshSnapshots = useCallback(() => {
    const result = listSnapshotsThroughBridge();
    if (!result.ok) {
      setSnapshots([]);
      return finishDataAction(result, '');
    }
    const items = result.data as BridgeSnapshotItem[];
    setSnapshots(items);
    setDataActionState('idle');
    setDataActionMessage('');
    return true;
  }, [finishDataAction]);

  const createManualSnapshot = useCallback(() => {
    setDataActionState('working'); setDataActionMessage('');
    const result = createSnapshotThroughBridge('Backup manual');
    if (finishDataAction(result, 'Snapshot criado.')) refreshSnapshots();
  }, [finishDataAction, refreshSnapshots]);

  const restoreSnapshot = useCallback((id: string) => {
    if (!window.confirm('Restaurar este snapshot? O estado atual será substituído.')) return;
    setDataActionState('working'); setDataActionMessage('');
    const result = restoreSnapshotThroughBridge(id);
    if (finishDataAction(result, 'Snapshot restaurado.', true)) refreshSnapshots();
  }, [finishDataAction, refreshSnapshots]);

  const generateRandomCharacter = useCallback((name: string, motivation: string) => {
    const cleanName = name.trim();
    const cleanMotivation = motivation.trim();
    if (!cleanName || !cleanMotivation) {
      setDataActionState('error');
      setDataActionMessage('Informe nome e motivação. O gerador não inventa esses dados.');
      return;
    }
    if (campaignRead.status === 'ready' && campaignRead.character && !window.confirm('Gerar uma nova ficha mecânica e substituir a ficha atual? A operação solicitará um snapshot antes da substituição.')) return;
    setDataActionState('working'); setDataActionMessage('');
    const result = generateRandomCharacterThroughBridge({ name: cleanName, motivation: cleanMotivation });
    finishDataAction(result, 'Ficha mecânica criada. Revise em PERSONAGEM.', true);
  }, [campaignRead, finishDataAction]);

  const createManualCharacter = useCallback(() => {
    if (!creationCapabilities.create) { setCreationActionState('error'); setCreationActionMessage('Criação manual indisponível neste host.'); return false; }
    if (campaignRead.status === 'ready' && campaignRead.character && !window.confirm('Criar esta ficha manual e substituir a ficha atual? O motor solicitará um snapshot antes da substituição.')) return false;
    setCreationActionState('working'); setCreationActionMessage('');
    const result = createCustomCharacterThroughBridge(creationDraft);
    if (!result.ok) { setCreationActionState('error'); setCreationActionMessage(result.message); refreshMasterBridge(); return false; }
    setCreationActionState('success'); setCreationActionMessage('Ficha manual criada pelo Motor Barbara.');
    refreshCampaignRead(); refreshMasterBridge();
    navigate('26');
    return true;
  }, [campaignRead, creationCapabilities.create, creationDraft, refreshCampaignRead, refreshMasterBridge, navigate]);

  const proposeWeaponAcquisition = useCallback((itemId: string) => {
    if (campaignRead.status !== 'ready' || !campaignRead.character) {
      setEquipmentActionState('error'); setEquipmentActionMessage('Carregue um personagem antes de propor uma aquisição.'); return;
    }
    if (campaignRead.inventory?.pending) {
      setEquipmentActionState('error'); setEquipmentActionMessage('Já existe uma aquisição aguardando decisão do Mestre.'); return;
    }
    setEquipmentActionState('working'); setEquipmentActionMessage('');
    const result = proposeWeaponAcquisitionThroughBridge(itemId);
    if (!result.ok) {
      setEquipmentActionState('error'); setEquipmentActionMessage(result.message); refreshMasterBridge(); return;
    }
    setEquipmentActionState('success'); setEquipmentActionMessage('Pedido entregue ao Mestre. Créditos e inventário não foram alterados.');
    refreshMasterBridge(); refreshCampaignRead(); refreshEquipmentCatalog(true);
  }, [campaignRead, refreshMasterBridge, refreshCampaignRead, refreshEquipmentCatalog]);

  const selectImageMap = useCallback((assetId: string) => {
    const id=String(assetId??'').trim();
    const asset=imageMaps.find(item=>item.assetId===id);
    if (!id || !asset) return;
    setSelectedImageAssetId(id); setImageActionState('idle'); setImageActionMessage('');
    if (asset.sourceType==='external_reference' && imageLibraryNative==='available') {
      const result=cacheExternalImage(asset);
      if (!result.ok) { setImageActionState('error'); setImageActionMessage(result.message); }
      else {
        setImageActionState(result.status==='already_cached'?'success':'working');
        setImageActionMessage(result.status==='already_cached'?'Mapa já está no cache privado.':'Baixando mapa para uso offline…');
        if (result.status==='already_cached') refreshImageCacheStatus(id,true);
      }
    }
  }, [imageMaps,imageLibraryNative,refreshImageCacheStatus]);

  const cacheSelectedImageMap = useCallback(() => {
    if (!selectedImageAsset) { setImageActionState('error'); setImageActionMessage('Selecione um mapa externo.'); return false; }
    setImageActionState('working'); setImageActionMessage('');
    const result=cacheExternalImage(selectedImageAsset);
    if (!result.ok) { setImageActionState('error'); setImageActionMessage(result.message); return false; }
    setImageActionMessage(result.status==='already_cached'?'Mapa já está no cache privado.':'Baixando mapa para o cache privado…');
    if (result.status==='already_cached') { setImageActionState('success'); refreshImageCacheStatus(selectedImageAsset.assetId,true); }
    return true;
  }, [selectedImageAsset, refreshImageCacheStatus]);

  const clearSelectedImageMapCache = useCallback(() => {
    if (!selectedImageAssetId) return false;
    const result=clearCachedImage(selectedImageAssetId);
    if (!result.ok) { setImageActionState('error'); setImageActionMessage(result.message); return false; }
    setImageCacheStatuses(current=>({...current,[selectedImageAssetId]:{assetId:selectedImageAssetId,cached:false}}));
    setImageActionState('success'); setImageActionMessage('Mapa removido do cache privado.'); return true;
  }, [selectedImageAssetId]);

  const cacheVisualEntity = useCallback((assetId: string) => {
    const asset=imageVisualEntities.find(item=>item.assetId===assetId);
    if (!asset) { setImageActionState('error'); setImageActionMessage('Asset visual não encontrado.'); return false; }
    setImageActionState('working'); setImageActionMessage('');
    const result=cacheExternalImage(asset);
    if (!result.ok) { setImageActionState('error'); setImageActionMessage(result.message); return false; }
    setImageActionMessage(result.status==='already_cached'?'Imagem já está no cache privado.':'Baixando imagem para o cache privado…');
    if (result.status==='already_cached') { setImageActionState('success'); refreshImageCacheStatus(asset.assetId,true); }
    return true;
  }, [imageVisualEntities, refreshImageCacheStatus]);

  const clearVisualEntityCache = useCallback((assetId: string) => {
    const result=clearCachedImage(assetId);
    if (!result.ok) { setImageActionState('error'); setImageActionMessage(result.message); return false; }
    setImageCacheStatuses(current=>({...current,[assetId]:{assetId,cached:false}}));
    setImageActionState('success'); setImageActionMessage('Imagem removida do cache privado.'); return true;
  }, []);

  const finishLibraryAction = useCallback((result: { ok: boolean; message?: string }, successMessage: string, refresh = false) => {
    if (!result.ok) {
      setLibraryActionState('error');
      setLibraryActionMessage(result.message || 'Operação rejeitada pela Biblioteca Android.');
      refreshLibraryBridge();
      return false;
    }
    setLibraryActionState('success');
    setLibraryActionMessage(successMessage);
    refreshLibraryBridge();
    if (refresh) refreshLibraryDocuments(true);
    return true;
  }, [refreshLibraryBridge, refreshLibraryDocuments]);

  const importPdfToLibrary = useCallback(() => {
    setLibraryActionState('working'); setLibraryActionMessage('');
    const result = requestPdfImportThroughBridge();
    finishLibraryAction(result, 'Seletor de PDF aberto. A indexação ocorrerá no armazenamento privado do aplicativo.');
  }, [finishLibraryAction]);

  const searchLibrary = useCallback((query: string) => {
    setLibraryActionState('working'); setLibraryActionMessage('');
    const result = searchLibraryThroughBridge(query, 8);
    if (!result.ok) {
      setLibrarySearchResults([]); setLibrarySearchGrounded(false);
      finishLibraryAction(result, '');
      return;
    }
    setLibrarySearchResults(result.data.results);
    setLibrarySearchGrounded(result.data.grounded);
    setLibraryActionState('success');
    setLibraryActionMessage(result.data.results.length ? `${result.data.results.length} trecho(s) encontrado(s) no índice offline.` : 'Nenhum trecho encontrado nos PDFs indexados.');
  }, [finishLibraryAction]);

  const updateLibraryPolicy = useCallback((docId: string, authorityType: LibraryAuthorityType, enabled: boolean) => {
    setLibraryActionState('working'); setLibraryActionMessage('');
    const result = updateLibraryDocumentPolicyThroughBridge(docId, authorityType, enabled);
    finishLibraryAction(result, 'Autoridade da fonte atualizada.', true);
  }, [finishLibraryAction]);

  const reindexLibraryDocument = useCallback((docId: string) => {
    setLibraryActionState('working'); setLibraryActionMessage('');
    const result = reindexLibraryDocumentThroughBridge(docId);
    finishLibraryAction(result, 'Reindexação iniciada.', true);
  }, [finishLibraryAction]);

  const removeLibraryDocument = useCallback((docId: string) => {
    if (!window.confirm('Remover este PDF e seu índice offline da Biblioteca?')) return;
    setLibraryActionState('working'); setLibraryActionMessage('');
    const result = removeLibraryDocumentThroughBridge(docId);
    if (finishLibraryAction(result, 'PDF removido da Biblioteca local.', true)) {
      setLibrarySearchResults(current => current.filter(item => item.docId !== docId));
    }
  }, [finishLibraryAction]);


  const finishGeminiAction = useCallback((result: { ok: boolean; message?: string }, successMessage: string) => {
    if (!result.ok) {
      setGeminiActionState('error');
      setGeminiActionMessage(result.message || 'Operação rejeitada pelo host Gemini.');
      refreshGeminiBridge(true);
      return false;
    }
    setGeminiActionState('success');
    setGeminiActionMessage(successMessage);
    refreshGeminiBridge(true);
    return true;
  }, [refreshGeminiBridge]);

  const configureGeminiApiKey = useCallback((apiKey: string) => {
    setGeminiActionState('working'); setGeminiActionMessage('');
    return finishGeminiAction(configureGeminiApiKeyThroughBridge(apiKey), 'Chave Gemini protegida no host Android.');
  }, [finishGeminiAction]);

  const clearGeminiApiKey = useCallback(() => {
    if (!window.confirm('Remover a chave Gemini protegida deste dispositivo?')) return false;
    setGeminiActionState('working'); setGeminiActionMessage('');
    return finishGeminiAction(clearGeminiApiKeyThroughBridge(), 'Chave Gemini removida do dispositivo.');
  }, [finishGeminiAction]);

  const generateSceneImage = useCallback(() => {
    if (!sceneImagePrompt) {
      setGeminiActionState('error');
      setGeminiActionMessage('A cena ainda não tem narração pública suficiente para gerar imagem.');
      return false;
    }
    setGeminiActionState('working'); setGeminiActionMessage('');
    return finishGeminiAction(generateGeminiSceneImageThroughBridge(sceneImagePrompt), 'Geração iniciada. A imagem só será exibida quando o host confirmar a conclusão.');
  }, [sceneImagePrompt, finishGeminiAction]);

  const importSceneImage = useCallback(() => {
    if (!sceneImagePrompt) {
      setGeminiActionState('error');
      setGeminiActionMessage('A cena atual ainda não possui contexto visual identificável.');
      return false;
    }
    setGeminiActionState('working'); setGeminiActionMessage('');
    return finishGeminiAction(requestGeminiSceneImageImportThroughBridge(sceneImagePrompt.sceneKey), 'Seletor de imagem aberto.');
  }, [sceneImagePrompt, finishGeminiAction]);

  const clearSceneImage = useCallback(() => {
    if (!window.confirm('Remover a imagem vinculada à cena?')) return false;
    setGeminiActionState('working'); setGeminiActionMessage('');
    return finishGeminiAction(clearGeminiSceneImageThroughBridge(), 'Imagem da cena removida.');
  }, [finishGeminiAction]);


  const screen = getScreen(route);
  const activeNav = useMemo(() => rootRouteByDomain[screen.domain] ?? '', [screen.domain]);


  const props = {
    screen,
    qaState,
    navigate,
    sceneDraft,
    setSceneDraft,
    openMenu: () => setStatusOpen(true),
    campaignRead,
    masterBridge,
    intentSubmitState,
    intentSubmitMessage,
    submitSceneIntent,
    dataCapabilities,
    dataActionState,
    dataActionMessage,
    snapshots,
    exportCharacterJson,
    importCharacterJsonFile,
    exportCampaignBackup,
    importCampaignBackupFile,
    refreshSnapshots,
    createManualSnapshot,
    restoreSnapshot,
    generateRandomCharacter,
    creationCapabilities,
    creationCatalog,
    creationDraft,
    setCreationDraft,
    creationPreview,
    creationPreviewError,
    creationActionState,
    creationActionMessage,
    refreshCreationCatalog: () => refreshCreationCatalog(false),
    createManualCharacter,
    equipmentCapabilities,
    equipmentWeapons,
    equipmentActionState,
    equipmentActionMessage,
    refreshEquipmentCatalog: () => refreshEquipmentCatalog(false),
    proposeWeaponAcquisition,
    imageLibraryCapabilities,
    imageLibraryInfo,
    imageMaps,
    imageMapQuery,
    setImageMapQuery,
    imageVisualEntities,
    imageVisualQuery,
    setImageVisualQuery,
    imageVisualKind,
    setImageVisualKind,
    selectedImageAsset,
    selectedImageUrl,
    selectedImageCache,
    imageCacheStatuses,
    imageLibraryNative,
    imageActionState,
    imageActionMessage,
    refreshImageLibrary: () => refreshImageLibrary(false),
    selectImageMap,
    cacheSelectedImageMap,
    clearSelectedImageMapCache,
    cacheVisualEntity,
    clearVisualEntityCache,
    libraryBridge,
    libraryCapabilities,
    libraryDocuments,
    libraryActionState,
    libraryActionMessage,
    librarySearchResults,
    librarySearchGrounded,
    refreshLibraryDocuments: () => refreshLibraryDocuments(false),
    importPdfToLibrary,
    searchLibrary,
    updateLibraryPolicy,
    reindexLibraryDocument,
    removeLibraryDocument,
    geminiBridge,
    geminiCapabilities,
    geminiStatus,
    geminiActionState,
    geminiActionMessage,
    sceneImageUrl,
    sceneImagePrompt,
    configureGeminiApiKey,
    clearGeminiApiKey,
    generateSceneImage,
    importSceneImage,
    clearSceneImage,
  };


  return (
    <AppShell className={screen.domain === 'Cena' ? 'scene-mode' : ''}>
      <TopBar onStatus={() => setStatusOpen(true)} />
      <div className="app-content">
        {debugMode
          ? <DebugGallery qaState={qaState} setQaState={setQaState} navigate={navigate} />
          : renderTemplate(props)}
      </div>
      <BottomNavigation items={mainNavigation} current={debugMode ? '' : activeNav} onNavigate={navigate} />


      <BottomSheet open={statusOpen} title="Utilidades" onClose={() => setStatusOpen(false)}>
        {utilityNavigation.map((item) => (
          <ListItem
            key={item.route}
            icon={item.icon}
            title={item.label}
            meta="Utilidade"
            text={item.route === '16' ? 'Consultar mapas disponíveis.' : 'Configurar interface e recursos disponíveis.'}
            onClick={() => {
              setStatusOpen(false);
              navigate(item.route);
            }}
          />
        ))}
        {debugMode && (
          <div className="qa-state-control">
            <label htmlFor="qa-state">Estado de QA</label>
            <select id="qa-state" value={qaState} onChange={(event) => { if (isQaState(event.target.value)) setQaState(event.target.value); }}>
              {screen.availableQaStates.map((state) => <option key={state} value={state}>{qaStateLabels[state] ?? state}</option>)}
            </select>
          </div>
        )}
      </BottomSheet>
    </AppShell>
  );
}