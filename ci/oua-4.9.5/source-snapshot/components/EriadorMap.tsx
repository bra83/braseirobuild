import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Check, ChevronRight, Crosshair, Download, Eye, EyeOff, Footprints, Map as MapIcon, MapPin,
  Maximize, Minimize, Scissors, Search, Settings, Trash2, Upload, X, ZoomIn, ZoomOut,
} from 'lucide-react';
import { currentBarbaraRenderPolicy } from '../lib/barbaraBridge';
import {
  areManifestNeighbors,
  clearHexCropCache,
  createHexAnalysisCrop,
  createHexCrop,
  EriadorHex,
  EriadorMapManifest,
  findHexAtPoint,
  importTerraMediaMapPackage,
  loadMapDiscovery,
  loadMapManifest,
  loadRuntimeMapManifest,
  loadRuntimeTerraMediaLocations,
  loadStoredMapImageBlob,
  loadTerraMediaLocations,
  MapDiscovery,
  MapPoint,
  TerraMediaLocation,
  saveMapManifest,
  resolveHexNarrativeLocation,
  shortHexLabel,
  validateEriadorManifest,
} from '../lib/map';
import { identifyHex } from '../geminiService';
import { exportBackupNative } from '../lib/native';
import type { StrongholdState } from '../lib/master/types';
import type { Character } from '../lib/db';
import { PartyCluster } from './immersive/PartyCluster';
import { loadImmersiveAssetManifest, normalizePoiAssetKey, resolvePoiAsset, resolvePoiAssetsForHex, type ImmersiveAssetManifest } from '../lib/immersiveAssets';

export interface PartyMapMove {
  toHexId: string;
  locationName: string;
  terrain: string;
  distanceKm?: number;
  discovery: MapDiscovery;
}

interface EriadorMapProps {
  currentLocation: string;
  campaignId: string;
  currentHexId?: string | null;
  onPartyMoved: (move: PartyMapMove) => void | Promise<void>;
  onDiscoveryChanged: (discovery: MapDiscovery, reason: string) => void | Promise<void>;
  onPartyRested?: () => void | Promise<void>;
  strongholds?: StrongholdState[];
  immersive?: boolean;
  partyMarkerMode?: 'footprints' | 'none';
  followParty?: boolean;
  partyCharacters?: Character[];
}

interface Camera { x: number; y: number; scale: number }
type MapViewMode = 'player' | 'master';

type MapSearchResult =
  | { kind: 'location'; location: TerraMediaLocation; score: number }
  | { kind: 'hex'; hex: EriadorHex; score: number };

const TERRAIN = [
  ['unknown', 'Não classificado'], ['plains', 'Planícies'], ['hills', 'Colinas'],
  ['forest', 'Floresta'], ['dense_forest', 'Floresta densa'], ['mountains', 'Montanhas'],
  ['swamp', 'Pântano'], ['water', 'Água'], ['settlement', 'Povoado'],
  ['road', 'Estrada'], ['ruins', 'Ruínas'], ['impassable', 'Intransponível'],
] as const;

function download(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

function mapViewModeStorageKey(campaignId: string): string {
  return `braseiro.map.viewMode.${campaignId || 'default'}`;
}

function readMapViewMode(campaignId: string): MapViewMode {
  try {
    return localStorage.getItem(mapViewModeStorageKey(campaignId)) === 'master' ? 'master' : 'player';
  } catch {
    // Jogador é o padrão seguro: uma remontagem nunca deve revelar o mapa.
    return 'player';
  }
}

function normalizeSearchText(value: string): string {
  return value
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[_\-.\/]+/g, ' ')
    .replace(/[^a-z0-9 ]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function searchableHexText(hex: EriadorHex): string {
  return normalizeSearchText([
    hex.id,
    shortHexLabel(hex),
    `q${hex.q} r${hex.r}`,
    `q=${hex.q} r=${hex.r}`,
    hex.name || '',
    hex.poi || '',
    hex.region || '',
    hex.placeType || '',
    hex.terrain || '',
    ...(hex.tags || []),
  ].join(' '));
}

function searchableLocationText(location: TerraMediaLocation): string {
  return normalizeSearchText([
    location.id,
    location.name,
    location.nameOriginal,
    ...(location.mapNames || []),
    ...(location.aliases || []),
    location.type,
    ...(location.categories || []),
    ...(location.regions || []),
    location.state2965,
    location.description,
    location.description2,
    ...(location.tags || []),
    location.hexId || '',
  ].join(' '));
}

function searchScore(text: string, query: string, tokens: string[]): number {
  if (text === query) return 1000;
  if (text.startsWith(query)) return 800;
  if (text.includes(` ${query} `) || text.endsWith(` ${query}`)) return 700;
  if (text.includes(query)) return 500;
  return tokens.reduce((score, token) => score + (text.includes(token) ? 50 : 0), 0);
}

const IMMERSIVE_MAP_WIDTH = 10630;
const IMMERSIVE_MAP_HEIGHT = 7678;
const IMMERSIVE_TILE_SIZE = 1024;
const IMMERSIVE_TILE_COLS = Math.ceil(IMMERSIVE_MAP_WIDTH / IMMERSIVE_TILE_SIZE);
const IMMERSIVE_TILE_ROWS = Math.ceil(IMMERSIVE_MAP_HEIGHT / IMMERSIVE_TILE_SIZE);
const IMMERSIVE_TILE_ZOOM_THRESHOLD = 0.12;

export const EriadorMap: React.FC<EriadorMapProps> = ({ currentLocation, campaignId, currentHexId, onPartyMoved, onDiscoveryChanged, onPartyRested, strongholds = [], immersive = false, partyMarkerMode = 'footprints', followParty = false, partyCharacters = [] }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const packageImportRef = useRef<HTMLInputElement>(null);
  const pointers = useRef<Map<number, MapPoint>>(new Map());
  const pointerStart = useRef<MapPoint | null>(null);
  const wasGesture = useRef(false);

  const [size, setSize] = useState({ width: 0, height: 0 });
  const [manifest, setManifest] = useState<EriadorMapManifest | null>(null);
  const [discovery, setDiscovery] = useState<MapDiscovery | null>(null);
  const [selected, setSelected] = useState<EriadorHex | null>(null);
  const [locations, setLocations] = useState<TerraMediaLocation[]>([]);
  const [selectedLocation, setSelectedLocation] = useState<TerraMediaLocation | null>(null);
  const [camera, setCamera] = useState<Camera>({ x: 0, y: 0, scale: 0.05 });
  const [expanded, setExpanded] = useState(false);
  const [gmMode, setGmMode] = useState(() => readMapViewMode(campaignId) === 'master');
  const [showGrid, setShowGrid] = useState(false);
  const [showIds, setShowIds] = useState(false);
  const [editingArea, setEditingArea] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isImportingPackage, setIsImportingPackage] = useState(false);
  const [mapImageRevision, setMapImageRevision] = useState(0);
  const [immersiveAssets, setImmersiveAssets] = useState<ImmersiveAssetManifest | null>(null);
  const immersiveTileCacheRef = useRef<Map<string, HTMLImageElement>>(new Map());
  const immersiveTileLoadingRef = useRef<Set<string>>(new Set());
  const [immersiveTileRevision, setImmersiveTileRevision] = useState(0);

  const [newlyDiscoveredIds, setNewlyDiscoveredIds] = useState<Set<string>>(new Set());
  const byId = useMemo(() => new Map(manifest?.hexes.map(hex => [hex.id, hex]) || []), [manifest]);

  const preferredLocationForHex = useCallback((hexId: string): TerraMediaLocation | null => {
    const candidates = locations.filter(location => location.hexId === hexId || location.hexIds?.includes(hexId));
    if (!candidates.length) return null;
    const importance = (location: TerraMediaLocation) => {
      const text = `${location.type} ${(location.categories || []).join(' ')} ${(location.tags || []).join(' ')}`.toLowerCase();
      const major = /cidade|fortaleza|refugio|refúgio|ruina|ruína|ponte|estalagem|taverna|torre|portao|portão|casa|salao|salão|vila|povoado|santuario|santuário/.test(text) ? 3 : 0;
      const verified = location.placement?.status === 'verified-label' ? 2 : 0;
      const visual = resolvePoiAsset(immersiveAssets, normalizePoiAssetKey(location.name)) ? 2 : 0;
      return major + verified + visual;
    };
    return [...candidates].sort((a, b) => importance(b) - importance(a) || a.name.localeCompare(b.name))[0] || null;
  }, [locations, immersiveAssets]);

  useEffect(() => {
    setGmMode(readMapViewMode(campaignId) === 'master');
  }, [campaignId]);

  useEffect(() => {
    if (!immersive) return;
    void loadImmersiveAssetManifest().then(setImmersiveAssets);
  }, [immersive]);

  const toggleMapViewMode = useCallback(() => {
    setGmMode(current => {
      const next = !current;
      try { localStorage.setItem(mapViewModeStorageKey(campaignId), next ? 'master' : 'player'); } catch {}
      return next;
    });
  }, [campaignId]);

  useEffect(() => {
    if (!discovery || !currentHexId || discovery.currentHexId === currentHexId) return;
    const updated = {
      ...discovery,
      currentHexId,
      discoveredHexIds: Array.from(new Set([...discovery.discoveredHexIds, currentHexId])),
      visitedHexIds: Array.from(new Set([...discovery.visitedHexIds, currentHexId])),
      plannedRouteHexIds: discovery.plannedRouteHexIds?.[0] === currentHexId
        ? discovery.plannedRouteHexIds.slice(1)
        : discovery.plannedRouteHexIds || [],
      updatedAt: Date.now(),
    };
    setDiscovery(updated);
    void Promise.resolve(onDiscoveryChanged(updated, 'sincronização da posição canónica no atlas')).catch(reason => {
      console.error('[BRASEIRO_MAPA] falha ao sincronizar posição:', reason);
      setError(reason instanceof Error ? reason.message : 'A posição não pôde ser sincronizada.');
    });
  }, [currentHexId, discovery?.currentHexId, onDiscoveryChanged]);

  const clamp = useCallback((value: Camera): Camera => {
    const box = containerRef.current;
    if (!box || !size.width || !size.height) return value;
    const vw = box.clientWidth;
    const vh = box.clientHeight;
    const fit = Math.min(vw / size.width, vh / size.height) * 0.98;
    const scale = Math.max(fit * 0.7, Math.min(value.scale, fit * 35));
    const sw = size.width * scale;
    const sh = size.height * scale;
    const margin = 80;
    const x = sw <= vw ? (vw - sw) / 2 : Math.min(margin, Math.max(vw - sw - margin, value.x));
    const y = sh <= vh ? (vh - sh) / 2 : Math.min(margin, Math.max(vh - sh - margin, value.y));
    return { x, y, scale };
  }, [size]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsSearching(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const searchResults = useMemo<MapSearchResult[]>(() => {
    if (!searchQuery.trim() || !manifest) return [];
    const query = normalizeSearchText(searchQuery);
    const tokens = query.split(' ').filter(Boolean);
    const results: MapSearchResult[] = [];

    for (const location of locations) {
      const visible = gmMode || location.hexIds.some(hexId =>
        discovery?.discoveredHexIds.includes(hexId)
        || discovery?.visitedHexIds.includes(hexId)
        || discovery?.currentHexId === hexId,
      );
      if (!visible) continue;
      const text = searchableLocationText(location);
      if (!tokens.every(token => text.includes(token))) continue;
      results.push({ kind: 'location', location, score: searchScore(text, query, tokens) + 100 });
    }

    for (const hex of manifest.hexes) {
      const visible = gmMode
        || discovery?.discoveredHexIds.includes(hex.id)
        || discovery?.visitedHexIds.includes(hex.id)
        || discovery?.currentHexId === hex.id;
      if (!visible) continue;
      const currentLocationAlias = discovery?.currentHexId === hex.id ? normalizeSearchText(currentLocation) : '';
      const text = `${searchableHexText(hex)} ${currentLocationAlias}`;
      if (!tokens.every(token => text.includes(token))) continue;
      results.push({ kind: 'hex', hex, score: searchScore(text, query, tokens) });
    }

    return results.sort((a, b) => b.score - a.score).slice(0, 16);
  }, [searchQuery, manifest, locations, discovery, gmMode, currentLocation]);

  const jumpToHex = useCallback((hex: EriadorHex, preserveLocation = false) => {
    const box = containerRef.current;
    if (!box) return;
    const scale = 0.6;
    setCamera(clamp({
      scale,
      x: box.clientWidth / 2 - hex.centerX * scale,
      y: box.clientHeight / 2 - hex.centerY * scale,
    }));
    setSelected(hex);
    if (!preserveLocation) setSelectedLocation(null);
    setSearchQuery('');
    setIsSearching(false);
  }, [clamp]);

  const jumpToLocation = useCallback((location: TerraMediaLocation) => {
    const box = containerRef.current;
    if (!box || !manifest) return;
    const point = location.representativePoint;
    const hex = location.hexId ? byId.get(location.hexId) : null;
    if (!point && !hex) {
      setError(`O catálogo não possui uma posição cartográfica para ${location.name}.`);
      return;
    }
    const targetX = point?.[0] ?? hex!.centerX;
    const targetY = point?.[1] ?? hex!.centerY;
    const scale = location.type === 'regiao' || location.type === 'rio' ? 0.42 : 0.72;
    setCamera(clamp({
      scale,
      x: box.clientWidth / 2 - targetX * scale,
      y: box.clientHeight / 2 - targetY * scale,
    }));
    setSelectedLocation(location);
    setSelected(hex || findHexAtPoint(manifest, { x: targetX, y: targetY }));
    setSearchQuery('');
    setIsSearching(false);
    setError(null);
  }, [byId, clamp, manifest]);

  const openSearchResult = useCallback((result: MapSearchResult) => {
    if (result.kind === 'location') jumpToLocation(result.location);
    else jumpToHex(result.hex);
  }, [jumpToHex, jumpToLocation]);

  const submitSearch = useCallback(() => {
    if (searchResults[0]) {
      openSearchResult(searchResults[0]);
      setError(null);
      return;
    }
    if (searchQuery.trim()) {
      setError('Local não encontrado. Tente o nome em português, o nome original, um alias, uma região ou o ID do hex.');
    }
  }, [openSearchResult, searchQuery, searchResults]);

  const fit = useCallback(() => {
    const box = containerRef.current;
    if (!box || !size.width || !size.height) return;
    const scale = Math.min(box.clientWidth / size.width, box.clientHeight / size.height) * 0.98;
    setCamera({
      scale,
      x: (box.clientWidth - size.width * scale) / 2,
      y: (box.clientHeight - size.height * scale) / 2,
    });
  }, [size]);

  useEffect(() => {
    let disposed = false;
    let objectUrl: string | null = null;
    const loadImage = async () => {
      const image = new Image();
      image.decoding = 'async';
      image.onload = () => {
        if (disposed) return;
        imageRef.current = image;
        setSize(immersive
          ? { width: IMMERSIVE_MAP_WIDTH, height: IMMERSIVE_MAP_HEIGHT }
          : { width: image.naturalWidth, height: image.naturalHeight });
        setError(null);
      };
      image.onerror = () => { if (!disposed) setError('Não foi possível carregar a imagem-base do mapa.'); };
      try {
        if (immersive) {
          // A imagem 10630×7678 decodifica para mais de 300 MB em RGBA e pode
          // congelar o WebView no primeiro frame. No modo imersivo usamos uma
          // prévia leve para o primeiro paint e tiles 1:1 apenas na área visível.
          image.src = './terra-media-map/map_preview_boot_1536.webp';
        } else {
          const stored = await loadStoredMapImageBlob();
          if (stored) {
            objectUrl = URL.createObjectURL(stored);
            image.src = objectUrl;
          } else {
            image.src = './map.png';
          }
        }
      } catch (reason) {
        console.error('BRASEIRO_MAPA: falha ao abrir imagem importada', reason);
        image.src = immersive ? './terra-media-map/map_preview_boot_1536.webp' : './map.png';
      }
    };
    void loadImage();
    return () => {
      disposed = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [immersive, mapImageRevision]);

  useEffect(() => {
    if (!size.width) return;
    fit();
    Promise.all([
      immersive ? loadRuntimeMapManifest() : loadMapManifest(),
      loadMapDiscovery(campaignId),
      immersive ? loadRuntimeTerraMediaLocations() : loadTerraMediaLocations(),
    ])
      .then(([savedManifest, savedDiscovery, savedLocations]) => {
        if (savedManifest) validateEriadorManifest(savedManifest, size.width, size.height);
        if (savedManifest) {
          const validIds = new Set(savedManifest.hexes.map(hex => hex.id));
          const validRoute = (savedDiscovery.routeHexIds || []).filter(id => validIds.has(id));
          const validVisited = (savedDiscovery.visitedHexIds || []).filter(id => validIds.has(id));
          if (!savedDiscovery.currentHexId || !validIds.has(savedDiscovery.currentHexId)) {
            const validDiscovered = (savedDiscovery.discoveredHexIds || []).filter(id => validIds.has(id));
            savedDiscovery.currentHexId = validRoute.at(-1) || validVisited.at(-1) || validDiscovered.at(-1) || null;
          }
          savedDiscovery.routeHexIds = validRoute;
          savedDiscovery.plannedRouteHexIds = (savedDiscovery.plannedRouteHexIds || []).filter(id => validIds.has(id));
          savedDiscovery.visitedHexIds = validVisited;
          savedDiscovery.discoveredHexIds = (savedDiscovery.discoveredHexIds || []).filter(id => validIds.has(id));
        }
        setManifest(savedManifest);
        setDiscovery(savedDiscovery);
        setLocations(savedLocations);
      })
      .catch(reason => setError(reason instanceof Error ? reason.message : String(reason)));
  }, [campaignId, fit, immersive, size]);

  useEffect(() => {
    const observer = new ResizeObserver(() => setCamera(current => clamp(current)));
    if (containerRef.current) observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, [clamp, expanded]);

  const polygonPath = (ctx: CanvasRenderingContext2D, polygon: MapPoint[]) => {
    ctx.beginPath();
    ctx.moveTo(polygon[0].x, polygon[0].y);
    polygon.slice(1).forEach(point => ctx.lineTo(point.x, point.y));
    ctx.closePath();
  };

  const toMapPoint = useCallback((clientX: number, clientY: number): MapPoint => {
    if (!containerRef.current) return { x: 0, y: 0 };
    const rect = containerRef.current.getBoundingClientRect();
    return { x: (clientX - rect.left - camera.x) / camera.scale, y: (clientY - rect.top - camera.y) / camera.scale };
  }, [camera]);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    const box = containerRef.current;
    const image = imageRef.current;
    if (!canvas || !box || !image) return;
    const barbaraRender = currentBarbaraRenderPolicy();
    const dpr = Math.max(1, Math.min(window.devicePixelRatio || 1, 4, barbaraRender.maxDpr) * barbaraRender.renderScale);
    const vw = box.clientWidth;
    const vh = box.clientHeight;
    canvas.width = Math.round(vw * dpr);
    canvas.height = Math.round(vh * dpr);
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';
    ctx.clearRect(0, 0, vw, vh);
    ctx.save();
    ctx.translate(camera.x, camera.y);
    ctx.scale(camera.scale, camera.scale);
    ctx.drawImage(image, 0, 0, size.width || image.naturalWidth, size.height || image.naturalHeight);

    // No modo imersivo, sobrepõe somente os tiles originais que cruzam a
    // viewport quando o zoom já justifica alta resolução. O carregamento é
    // assíncrono e nunca bloqueia o primeiro frame.
    if (immersive && camera.scale >= IMMERSIVE_TILE_ZOOM_THRESHOLD && size.width > 0) {
      const left = Math.max(0, -camera.x / camera.scale);
      const top = Math.max(0, -camera.y / camera.scale);
      const right = Math.min(IMMERSIVE_MAP_WIDTH, left + vw / camera.scale);
      const bottom = Math.min(IMMERSIVE_MAP_HEIGHT, top + vh / camera.scale);
      const c0 = Math.max(0, Math.floor(left / IMMERSIVE_TILE_SIZE) - 1);
      const c1 = Math.min(IMMERSIVE_TILE_COLS - 1, Math.floor(right / IMMERSIVE_TILE_SIZE) + 1);
      const r0 = Math.max(0, Math.floor(top / IMMERSIVE_TILE_SIZE) - 1);
      const r1 = Math.min(IMMERSIVE_TILE_ROWS - 1, Math.floor(bottom / IMMERSIVE_TILE_SIZE) + 1);
      for (let row = r0; row <= r1; row += 1) {
        for (let col = c0; col <= c1; col += 1) {
          const key = `${col}:${row}`;
          const cached = immersiveTileCacheRef.current.get(key);
          const x = col * IMMERSIVE_TILE_SIZE;
          const y = row * IMMERSIVE_TILE_SIZE;
          const width = Math.min(IMMERSIVE_TILE_SIZE, IMMERSIVE_MAP_WIDTH - x);
          const height = Math.min(IMMERSIVE_TILE_SIZE, IMMERSIVE_MAP_HEIGHT - y);
          if (cached?.complete && cached.naturalWidth > 0) {
            ctx.drawImage(cached, x, y, width, height);
          } else if (!immersiveTileLoadingRef.current.has(key)) {
            immersiveTileLoadingRef.current.add(key);
            const tile = new Image();
            tile.decoding = 'async';
            tile.onload = () => {
              immersiveTileCacheRef.current.set(key, tile);
              immersiveTileLoadingRef.current.delete(key);
              setImmersiveTileRevision(value => value + 1);
            };
            tile.onerror = () => immersiveTileLoadingRef.current.delete(key);
            tile.src = `./terra-media-map/tiles-hi/t_${col}_${row}.webp`;
          }
        }
      }
    }

    if (manifest && discovery && !gmMode) {
      ctx.save();
      ctx.fillStyle = 'rgba(0,0,0,.95)';
      // A névoa é um caminho com furos (evenodd). Usar destination-out neste
      // mesmo canvas apagava também os pixels do mapa e deixava os hexes
      // explorados transparentes sobre o fundo escuro.
      const revealed = new Set([...(discovery.discoveredHexIds || []), ...(discovery.visitedHexIds || [])]);
      if (discovery.currentHexId) revealed.add(discovery.currentHexId);
      ctx.beginPath();
      ctx.rect(0, 0, size.width || image.naturalWidth, size.height || image.naturalHeight);
      revealed.forEach(id => {
        const hex = byId.get(id);
        if (!hex) return;
        ctx.moveTo(hex.polygon[0].x, hex.polygon[0].y);
        hex.polygon.slice(1).forEach(point => ctx.lineTo(point.x, point.y));
        ctx.closePath();
      });
      ctx.fill('evenodd');
      ctx.restore();
    }

    if (manifest && (showGrid || editingArea)) {
      const left = -camera.x / camera.scale;
      const top = -camera.y / camera.scale;
      const right = left + vw / camera.scale;
      const bottom = top + vh / camera.scale;
      
      ctx.save();
      ctx.lineWidth = 1 / camera.scale;
      
      const visibleHexes = manifest.hexes.filter(hex => {
        const c = hex.centerX;
        return c > left - 100 && c < right + 100 && hex.centerY > top - 100 && hex.centerY < bottom + 100;
      });

      visibleHexes.forEach(hex => {
        const isSelected = selected?.id === hex.id;
        ctx.beginPath();
        hex.polygon.forEach((p, i) => { if (i === 0) ctx.moveTo(p.x, p.y); else ctx.lineTo(p.x, p.y); });
        ctx.closePath();
        
        if (isSelected) {
          ctx.fillStyle = 'rgba(251, 191, 36, 0.2)';
          ctx.fill();
          ctx.strokeStyle = '#fbbf24';
          ctx.lineWidth = 2 / camera.scale;
        } else if (editingArea) {
          ctx.fillStyle = hex.playable ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)';
          ctx.fill();
          ctx.strokeStyle = hex.playable ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)';
          ctx.lineWidth = 0.5 / camera.scale;
        } else {
          ctx.strokeStyle = 'rgba(251, 191, 36, 0.15)';
          ctx.lineWidth = 1 / camera.scale;
        }
        ctx.stroke();
        
        if (showIds && camera.scale > 0.12) {
          ctx.save();
          ctx.fillStyle = '#fff3c4';
          ctx.font = `${10 / camera.scale}px monospace`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(shortHexLabel(hex), hex.centerX, hex.centerY);
          ctx.restore();
        }
      });
      ctx.restore();
    }

    if (selected) {
      polygonPath(ctx, selected.polygon);
      ctx.fillStyle = 'rgba(250,190,30,.32)';
      ctx.fill();
      ctx.lineWidth = 3 / camera.scale;
      ctx.strokeStyle = '#fbbf24';
      ctx.stroke();
    }

    if (manifest && discovery) {
      const routeIds = (discovery.routeHexIds?.length ? discovery.routeHexIds : discovery.visitedHexIds) || [];
      const route = routeIds.map(id => byId.get(id)).filter((hex): hex is EriadorHex => Boolean(hex));
      if (route.length > 0) {
        ctx.save();
        if (route.length > 1) {
          ctx.beginPath();
          route.forEach((hex, index) => {
            if (index === 0) ctx.moveTo(hex.centerX, hex.centerY);
            else ctx.lineTo(hex.centerX, hex.centerY);
          });
          ctx.strokeStyle = 'rgba(220, 38, 38, 0.92)';
          ctx.lineWidth = 4 / camera.scale;
          ctx.setLineDash([10 / camera.scale, 7 / camera.scale]);
          ctx.shadowColor = 'rgba(0,0,0,.9)';
          ctx.shadowBlur = 3 / camera.scale;
          ctx.stroke();
          ctx.setLineDash([]);
        }

        route.forEach((hex, index) => {
          const previous = route[Math.max(0, index - 1)];
          const angle = index ? Math.atan2(hex.centerY - previous.centerY, hex.centerX - previous.centerX) + Math.PI / 2 : 0;
          ctx.save();
          ctx.translate(hex.centerX, hex.centerY);
          ctx.rotate(angle);
          ctx.fillStyle = '#fff1c7';
          ctx.strokeStyle = '#7f1d1d';
          ctx.lineWidth = 1.2 / camera.scale;
          for (const side of [-1, 1]) {
            ctx.beginPath();
            ctx.ellipse(side * 5 / camera.scale, side * 2 / camera.scale, 3.2 / camera.scale, 6 / camera.scale, side * .16, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
          }
          ctx.restore();
        });
        ctx.restore();
      }

      const plannedIds = (discovery.plannedRouteHexIds || []).filter(id => byId.has(id));
      const planned = [discovery.currentHexId, ...plannedIds]
        .map(id => id ? byId.get(id) : undefined)
        .filter((hex): hex is EriadorHex => Boolean(hex));
      if (planned.length > 1) {
        ctx.save();
        ctx.beginPath();
        planned.forEach((hex, index) => index === 0 ? ctx.moveTo(hex.centerX, hex.centerY) : ctx.lineTo(hex.centerX, hex.centerY));
        ctx.strokeStyle = 'rgba(34, 211, 238, .95)';
        ctx.lineWidth = 3 / camera.scale;
        ctx.setLineDash([5 / camera.scale, 5 / camera.scale]);
        ctx.shadowColor = 'rgba(0,0,0,.85)';
        ctx.shadowBlur = 3 / camera.scale;
        ctx.stroke();
        ctx.setLineDash([]);
        planned.slice(1, 6).forEach((hex, index) => {
          ctx.beginPath();
          ctx.arc(hex.centerX, hex.centerY, 9 / camera.scale, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(8, 47, 73, .94)';
          ctx.fill();
          ctx.strokeStyle = '#a5f3fc';
          ctx.lineWidth = 1.5 / camera.scale;
          ctx.stroke();
          ctx.fillStyle = '#ecfeff';
          ctx.font = `bold ${9 / camera.scale}px sans-serif`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(String(index + 1), hex.centerX, hex.centerY);
        });
        ctx.restore();
      }
    }

    // Pulsing Glow for newly discovered hexes
    if (manifest && newlyDiscoveredIds.size > 0) {
      const time = Date.now();
      const pulse = (Math.sin(time / 400) + 1) / 2; // 0 to 1
      ctx.save();
      newlyDiscoveredIds.forEach(id => {
        const hex = byId.get(id);
        if (hex) {
          ctx.beginPath();
          hex.polygon.forEach((p, i) => { if (i === 0) ctx.moveTo(p.x, p.y); else ctx.lineTo(p.x, p.y); });
          ctx.closePath();
          
          // Outer shadow glow
          ctx.shadowBlur = (25 + pulse * 15) / camera.scale;
          ctx.shadowColor = `rgba(251, 191, 36, ${0.4 + pulse * 0.4})`;
          
          // Pulsing border
          ctx.strokeStyle = `rgba(251, 191, 36, ${0.6 + pulse * 0.4})`;
          ctx.lineWidth = (4 + pulse * 4) / camera.scale;
          ctx.stroke();
          
          // Inner radial gradient
          const grad = ctx.createRadialGradient(hex.centerX, hex.centerY, 0, hex.centerX, hex.centerY, hex.crop.width * 0.7);
          grad.addColorStop(0, `rgba(251, 191, 36, ${0.15 * pulse})`);
          grad.addColorStop(1, 'rgba(251, 191, 36, 0)');
          ctx.fillStyle = grad;
          ctx.fill();
        }
      });
      ctx.restore();
    }

    ctx.restore();

    // Marcador de posição em tamanho fixo de tela: permanece visível em qualquer zoom.
    if (partyMarkerMode === 'footprints' && discovery?.currentHexId) {
      const current = byId.get(discovery.currentHexId);
      if (current) {
        const screenX = camera.x + current.centerX * camera.scale;
        const screenY = camera.y + current.centerY * camera.scale;
        const pulse = (Math.sin(Date.now() / 450) + 1) / 2;
        ctx.save();
        ctx.translate(screenX, screenY);
        ctx.beginPath();
        ctx.arc(0, 0, 18 + pulse * 3, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(127, 29, 29, .82)';
        ctx.fill();
        ctx.lineWidth = 3;
        ctx.strokeStyle = '#fff1c7';
        ctx.shadowColor = '#000';
        ctx.shadowBlur = 8;
        ctx.stroke();

        const drawFoot = (x: number, y: number, rotation: number) => {
          ctx.save();
          ctx.translate(x, y);
          ctx.rotate(rotation);
          ctx.beginPath();
          ctx.ellipse(0, 2.5, 3.2, 5.2, 0, 0, Math.PI * 2);
          ctx.ellipse(0, -3.8, 4.1, 6.3, 0, 0, Math.PI * 2);
          ctx.fillStyle = '#fff7d6';
          ctx.fill();
          ctx.restore();
        };
        drawFoot(-5, -1, -0.18);
        drawFoot(5, 2, 0.18);
        ctx.restore();
      }
    }
    // Refúgios e fortalezas persistentes conhecidos pela Companhia.
    for (const stronghold of strongholds.filter(item => item.knownToCompany && item.hexId)) {
      const hex = stronghold.hexId ? byId.get(stronghold.hexId) : undefined;
      if (!hex) continue;
      const screenX = camera.x + hex.centerX * camera.scale;
      const screenY = camera.y + hex.centerY * camera.scale;
      ctx.save();
      ctx.translate(screenX + 24, screenY - 24);
      ctx.fillStyle = stronghold.ownerKind === 'company' ? 'rgba(22,101,52,.96)' : stronghold.status === 'contested' ? 'rgba(153,27,27,.96)' : 'rgba(120,53,15,.96)';
      ctx.strokeStyle = '#fff1c7'; ctx.lineWidth = 2; ctx.shadowColor = '#000'; ctx.shadowBlur = 6;
      ctx.beginPath(); ctx.rect(-9,-7,18,14); ctx.fill(); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(-11,-7); ctx.lineTo(0,-15); ctx.lineTo(11,-7); ctx.closePath(); ctx.fill(); ctx.stroke();
      if (camera.scale > .1) { ctx.shadowBlur=2; ctx.fillStyle='#fff7d6'; ctx.font='bold 10px serif'; ctx.textAlign='center'; ctx.fillText(stronghold.name,0,27); }
      ctx.restore();
    }

    if (selectedLocation?.representativePoint) {
      const [mapX, mapY] = selectedLocation.representativePoint;
      const screenX = camera.x + mapX * camera.scale;
      const screenY = camera.y + mapY * camera.scale;
      ctx.save();
      ctx.translate(screenX, screenY);
      ctx.beginPath();
      ctx.arc(0, 0, 12, 0, Math.PI * 2);
      ctx.fillStyle = selectedLocation.placement.status === 'verified-label' ? 'rgba(37, 99, 235, .92)' : 'rgba(126, 34, 206, .88)';
      ctx.fill();
      ctx.lineWidth = 3;
      ctx.strokeStyle = '#fff7d6';
      ctx.shadowColor = '#000';
      ctx.shadowBlur = 8;
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, 22);
      ctx.lineTo(-5, 10);
      ctx.lineTo(5, 10);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }

  }, [byId, camera, discovery, editingArea, gmMode, immersive, immersiveTileRevision, manifest, newlyDiscoveredIds, selected, selectedLocation, showGrid, showIds, size, strongholds]);

  useEffect(() => {
    const frame = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(frame);
  }, [draw]);

  const zoom = (factor: number, clientX?: number, clientY?: number) => {
    const box = containerRef.current;
    if (!box) return;
    const rect = box.getBoundingClientRect();
    const px = (clientX ?? rect.left + rect.width / 2) - rect.left;
    const py = (clientY ?? rect.top + rect.height / 2) - rect.top;
    setCamera(current => {
      const scale = current.scale * factor;
      const actual = scale / current.scale;
      return clamp({ scale, x: px - (px - current.x) * actual, y: py - (py - current.y) * actual });
    });
  };

  const handleClick = (clientX: number, clientY: number) => {
    const point = toMapPoint(clientX, clientY);
    if (point.x < 0 || point.y < 0 || point.x > size.width || point.y > size.height) return;
    if (!manifest) return;
    const hex = findHexAtPoint(manifest, point);


    if (!hex) { setSelected(null); setSelectedLocation(null); return; }
    if (editingArea) {
      const updated = { ...hex, playable: !hex.playable, enabled: !hex.playable };
      setManifest({ ...manifest, hexes: manifest.hexes.map(item => item.id === hex.id ? updated : item), updatedAt: Date.now() });
      setSelected(updated);
    } else if (hex.playable || gmMode) {
      setSelected(hex);
      setSelectedLocation(preferredLocationForHex(hex.id));
    }
  };

  const pointerDown = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (event.pointerType === 'mouse' && event.button !== 0) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    pointerStart.current = { x: event.clientX, y: event.clientY };
    if (pointers.current.size > 1) wasGesture.current = true;

  };

  const pointerMove = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const previous = pointers.current.get(event.pointerId);
    if (!previous) return;
    const before: MapPoint[] = Array.from(pointers.current.values());
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    const after: MapPoint[] = Array.from(pointers.current.values());


    if (after.length === 1) {
      if (followParty) return;
      setCamera(current => clamp({ ...current, x: current.x + event.clientX - previous.x, y: current.y + event.clientY - previous.y }));
    } else if (after.length === 2 && before.length === 2) {
      wasGesture.current = true;
      const a = Math.hypot(before[0].x - before[1].x, before[0].y - before[1].y);
      const b = Math.hypot(after[0].x - after[1].x, after[0].y - after[1].y);
      if (a > 0) zoom(b / a, (after[0].x + after[1].x) / 2, (after[0].y + after[1].y) / 2);
    }
  };

  const pointerUp = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const start = pointerStart.current;
    const moved = start ? Math.hypot(event.clientX - start.x, event.clientY - start.y) : 999;
    pointers.current.delete(event.pointerId);
    if (!wasGesture.current && moved < 6) handleClick(event.clientX, event.clientY);
    if (!pointers.current.size) {
      wasGesture.current = false; pointerStart.current = null;
      if (followParty && discovery?.currentHexId) centerOnHex(discovery.currentHexId);
    }
  };

  useEffect(() => {
    if (!manifest || !selected) return;
    const refreshed = manifest.hexes.find(hex => hex.id === selected.id);
    if (refreshed && (refreshed.centerX !== selected.centerX || refreshed.centerY !== selected.centerY)) setSelected(refreshed);
  }, [manifest, selected]);


  const TERRAIN_COSTS: Record<string, number> = {
    grassland: 1, plains: 1, coast: 1, road: 1, settlement: 1,
    hills: 2, forest: 2, ruins: 2, unknown: 2,
    waste: 3, dense_forest: 3, marsh: 3, swamp: 3,
    mountains: 4, water: 5, sea: Number.POSITIVE_INFINITY,
  };

  const findPlannedRoute = (startId: string, targetId: string): string[] => {
    if (startId === targetId) return [];
    const target = byId.get(targetId);
    const start = byId.get(startId);
    if (!target || !start || !target.playable) return [];

    type OpenNode = { id: string; score: number };
    const open: OpenNode[] = [{ id: startId, score: 0 }];
    const cameFrom = new Map<string, string>();
    const cost = new Map<string, number>([[startId, 0]]);
    const closed = new Set<string>();
    const mapStep = Math.max(1, manifest?.layout.horizontalStep || 70);
    const heuristic = (hex: EriadorHex) => Math.hypot(hex.centerX - target.centerX, hex.centerY - target.centerY) / mapStep;

    while (open.length) {
      let bestIndex = 0;
      for (let index = 1; index < open.length; index += 1) if (open[index].score < open[bestIndex].score) bestIndex = index;
      const currentId = open.splice(bestIndex, 1)[0].id;
      if (currentId === targetId) {
        const route: string[] = [];
        let cursor = targetId;
        while (cursor !== startId) {
          route.push(cursor);
          const previous = cameFrom.get(cursor);
          if (!previous) return [];
          cursor = previous;
        }
        return route.reverse();
      }
      if (closed.has(currentId)) continue;
      closed.add(currentId);
      const current = byId.get(currentId);
      if (!current) continue;
      for (const neighborId of current.neighbors || []) {
        const neighbor = byId.get(neighborId);
        if (!neighbor || !neighbor.playable || closed.has(neighborId)) continue;
        const terrainCost = TERRAIN_COSTS[neighbor.terrain] ?? 2;
        if (!Number.isFinite(terrainCost)) continue;
        // Estradas e caminhos são favorecidos no planejamento, mas nenhuma
        // consequência mecânica é aplicada aqui; a Jornada formal resolve isso.
        const roadFactor = neighbor.terrain === 'road' || neighbor.tags?.some(tag => /road|estrada|caminho/i.test(tag)) ? 0.7 : 1;
        const nextCost = (cost.get(currentId) || 0) + Math.max(0.65, terrainCost * roadFactor);
        if (nextCost >= (cost.get(neighborId) ?? Number.POSITIVE_INFINITY)) continue;
        cameFrom.set(neighborId, currentId);
        cost.set(neighborId, nextCost);
        open.push({ id: neighborId, score: nextCost + heuristic(neighbor) });
      }
    }
    return [];
  };

  const planRouteToSelected = async () => {
    if (!selected || !discovery?.currentHexId) {
      setError('Posicione primeiro a Companhia em um hex para planejar a rota.');
      return;
    }
    const route = findPlannedRoute(discovery.currentHexId, selected.id);
    if (!route.length) {
      setError(selected.id === discovery.currentHexId ? 'A Companhia já está neste hex.' : 'Não foi encontrada uma rota jogável até o hex selecionado.');
      return;
    }
    const updated: MapDiscovery = { ...discovery, plannedRouteHexIds: route, updatedAt: Date.now() };
    await onDiscoveryChanged(updated, 'planeamento da rota de viagem');
    setDiscovery(updated);
    setError(null);
    setMessage(`Rota planejada com ${route.length} etapa(s). O núcleo ambiental analisará o hex atual e as próximas ${Math.min(5, route.length)} etapas.`);
  };

  const clearPlannedRoute = async () => {
    if (!discovery) return;
    const updated: MapDiscovery = { ...discovery, plannedRouteHexIds: [], updatedAt: Date.now() };
    await onDiscoveryChanged(updated, 'limpeza da rota planeada');
    setDiscovery(updated);
    setMessage('Rota futura limpa. O histórico já percorrido foi preservado.');
  };

  const restGroup = async () => {
    if (!onPartyRested) {
      setError('O descanso deve ser resolvido pelo Mestre Virtual, pois Fadiga pertence às fichas dos Heróis.');
      return;
    }
    await onPartyRested();
    if (discovery) {
      const updated = { ...discovery, fatigue: 0, updatedAt: Date.now() };
      await onDiscoveryChanged(updated, 'sincronização visual após descanso');
      setDiscovery(updated);
    }
    setMessage('Descanso enviado ao Mestre Virtual para resolução da Companhia.');
  };

  const moveGroup = async () => {
    if (!selected || !selected.playable || !discovery) return;
    const current = discovery.currentHexId ? byId.get(discovery.currentHexId) : null;
    
    if (current) {
      console.log(`Attempting move from ${shortHexLabel(current)} (ID: ${current.id}) to ${shortHexLabel(selected)} (ID: ${selected.id})`);
      console.log(`Current neighbors: ${JSON.stringify(current.neighbors)}`);
      
      if (current.id === selected.id) {
        setError("A comitiva já está neste local.");
        return;
      }
      
      const isNeighbor = current.neighbors?.includes(selected.id) || areManifestNeighbors(manifest, current, selected);
      if (!isNeighbor) {
        setError(`Movimento inválido: ${selected.name || shortHexLabel(selected)} não é adjacente à sua posição.`);
        console.warn(`Neighbor check failed. Selected ${selected.id} is not a direct neighbor.`);
        return;
      }
    }

    const cost = TERRAIN_COSTS[selected.terrain] ?? 2;
    if (!Number.isFinite(cost)) {
      setError('Este hex é marítimo e não pode ser atravessado sem um meio apropriado.');
      return;
    }
    const discovered = new Set(discovery.discoveredHexIds);
    const visited = new Set(discovery.visitedHexIds);
    
    // Check if this is a new discovery
    const isNewDiscovery = !discovered.has(selected.id);
    
    discovered.add(selected.id);
    visited.add(selected.id);
    
    if (isNewDiscovery) {
      console.log(`First time discovering hex: ${selected.id}`);
      setNewlyDiscoveredIds(prev => new Set(prev).add(selected.id));
      // A análise por IA é manual para não consumir cota nem disparar chamadas sem pedido.
    }
    
    const route = [...(discovery.routeHexIds?.length ? discovery.routeHexIds : discovery.visitedHexIds || [])];
    if (route[route.length - 1] !== selected.id) route.push(selected.id);
    const updated = {
      ...discovery,
      currentHexId: selected.id,
      discoveredHexIds: [...discovered],
      visitedHexIds: [...visited],
      routeHexIds: route,
      plannedRouteHexIds: discovery.plannedRouteHexIds?.[0] === selected.id
        ? discovery.plannedRouteHexIds.slice(1)
        : [],
      // Compatibilidade visual apenas. A Fadiga canônica pertence a cada ficha e é
      // alterada somente pelo motor determinístico de Jornadas.
      fatigue: 0,
      updatedAt: Date.now(),
    };

    const resolvedLocation = resolveHexNarrativeLocation(manifest, locations, selected.id);
    const locationName = resolvedLocation.name;
    try {
      await onPartyMoved({ toHexId: selected.id, locationName, terrain: selected.terrain, discovery: updated });
      setDiscovery(updated);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'O movimento não pôde ser gravado de forma atômica.');
      return;
    }
    centerOnHex(selected.id);
    if (!current) {
      // Na posição inicial, devolva imediatamente o controle à tela principal
      // para que o botão Começar aventura apareça sem o cartão do hex por cima.
      setSelected(null);
      setSelectedLocation(null);
    }
    setMessage(`Comitiva deslocada para ${locationName}. A distância e a Fadiga serão resolvidas pela Jornada.`);
    setError(null);
  };

  const toggleDiscovery = async () => {
    if (!selected || !discovery) return;
    const ids = new Set(discovery.discoveredHexIds);
    const wasDiscovered = ids.has(selected.id);
    if (wasDiscovered) {
      ids.delete(selected.id);
      setNewlyDiscoveredIds(prev => {
        const next = new Set(prev);
        next.delete(selected.id);
        return next;
      });
    } else {
      ids.add(selected.id);
      setNewlyDiscoveredIds(prev => new Set(prev).add(selected.id));
      // A análise por IA é manual para não consumir cota nem disparar chamadas sem pedido.
    }
    const updated = { ...discovery, discoveredHexIds: [...ids] };
    await onDiscoveryChanged(updated, wasDiscovered ? 'ocultação manual de hex' : 'revelação manual de hex');
    setDiscovery(updated);
  };

  const updateTerrain = async (terrain: string) => {
    if (!selected || !manifest) return;
    const changed = { ...selected, terrain };
    const next = { ...manifest, hexes: manifest.hexes.map(hex => hex.id === changed.id ? changed : hex), updatedAt: Date.now() };
    setSelected(changed);
    setManifest(next);
    await saveMapManifest(next);
  };

  const identifyHexWithAI = async (hexId?: string) => {
    const targetHex = hexId ? byId.get(hexId) : selected;
    if (!targetHex || !imageRef.current || !manifest) return;
    
    try {
      setMessage(`IA analisando ${shortHexLabel(targetHex)}...`);
      const [hexBlob, contextBlob] = await Promise.all([
        createHexCrop(imageRef.current, targetHex),
        createHexAnalysisCrop(imageRef.current, targetHex),
      ]);

      const blobToBase64 = (blob: Blob) => new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => reject(reader.error || new Error('Falha ao ler imagem.'));
        reader.onloadend = () => resolve(String(reader.result).split(',')[1] || '');
        reader.readAsDataURL(blob);
      });
      const [base64, contextBase64] = await Promise.all([
        blobToBase64(hexBlob),
        blobToBase64(contextBlob),
      ]);
      const result = await identifyHex(base64, contextBase64);
      
      if (result.terrain || result.name || result.poi) {
        const nextHex = { ...targetHex };
        if (result.terrain && TERRAIN.some(t => t[0] === result.terrain)) {
          nextHex.terrain = result.terrain as any;
        }
        if (result.name && !targetHex.name) {
          nextHex.name = result.name;
        }
        if (result.poi) nextHex.poi = result.poi;
        if (result.region) nextHex.region = result.region;
        if (result.placeType) nextHex.placeType = result.placeType;
        if (typeof result.confidence === 'number') nextHex.confidence = result.confidence;
        if (result.tags?.length) nextHex.tags = Array.from(new Set([...(nextHex.tags || []), ...result.tags]));
        
        if (selected?.id === targetHex.id) {
          setSelected(nextHex);
        }
        
        const nextManifest = { ...manifest };
        nextManifest.hexes = nextManifest.hexes.map(h => h.id === targetHex.id ? nextHex : h);
        setManifest(nextManifest);
        await saveMapManifest(nextManifest);
        
        let msg = `IA identificou: ${result.terrain}`;
        if (result.name) msg += ` em ${result.name}`;
        if (result.poi) msg += ` [POI: ${result.poi}]`;
        setMessage(msg);
      } else {
        setMessage("IA não identificou detalhes novos.");
      }
    } catch (e) {
      console.error('AI identification error:', e);
      setMessage('');
      const detail = e instanceof Error ? e.message : 'Falha ao consultar a IA.';
      setError(detail === 'Unexpected end of JSON input'
        ? 'A IA respondeu sem concluir o JSON. O aplicativo tentou novamente, mas não recebeu uma resposta válida.'
        : detail);
    }
  };

  const centerOnHex = useCallback((hexId: string, customScale?: number) => {
    if (!manifest || size.width === 0) return;
    const hex = manifest.hexes.find(h => h.id === hexId);
    if (!hex) return;
    setCamera(prev => {
      const scale = customScale ?? prev.scale;
      // Use container dimensions if available, otherwise fallback to reasonable defaults
      const vw = containerRef.current?.clientWidth || 800;
      const vh = containerRef.current?.clientHeight || 600;
      return {
        scale,
        x: vw / 2 - hex.centerX * scale,
        y: vh / 2 - hex.centerY * scale
      };
    });
  }, [manifest, size]);

  const hasInitialCentered = useRef(false);
  useEffect(() => {
    if (manifest && discovery?.currentHexId && size.width > 0 && !hasInitialCentered.current) {
      // Small timeout to ensure container dimensions are settled
      const timer = setTimeout(() => {
        centerOnHex(discovery.currentHexId!, 0.7);
        hasInitialCentered.current = true;
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [manifest, discovery?.currentHexId, size.width, centerOnHex]);

  useEffect(() => {
    if (!followParty || !manifest || !discovery?.currentHexId || size.width <= 0) return;
    const timer = window.setTimeout(() => centerOnHex(discovery.currentHexId!), 60);
    return () => window.clearTimeout(timer);
  }, [followParty, manifest, discovery?.currentHexId, size.width, centerOnHex]);

  const cropHex = async () => {
    if (!selected || !imageRef.current) return;
    try {
      const blob = await createHexCrop(imageRef.current, selected);
      download(blob, `${shortHexLabel(selected).replace('/', '-')}.png`);
      setMessage('Recorte transparente criado e guardado no cache.');
    } catch (reason) { setError(reason instanceof Error ? reason.message : String(reason)); }
  };

  const exportManifest = async () => {
    if (!manifest) return;
    const json = JSON.stringify(manifest, null, 2);
    try {
      if (window.BraseiroAndroid?.exportBackup) {
        const destination = await exportBackupNative('map_manifest.json', json);
        setMessage(`Manifesto salvo em ${destination || 'local escolhido'}.`);
      } else {
        download(new Blob([json], { type: 'application/json' }), 'map_manifest.json');
        setMessage('Manifesto exportado.');
      }
    } catch (reason) { setError(reason instanceof Error ? reason.message : String(reason)); }
  };

  const importMapPackage = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setIsImportingPackage(true);
    setError(null);
    setMessage('Lendo e validando o pacote completo do mapa...');
    try {
      const result = await importTerraMediaMapPackage(file);
      const fixedManifest = await loadMapManifest();
      if (!fixedManifest) throw new Error('A geometria fixa integrada não pôde ser carregada após a importação.');
      const savedDiscovery = await loadMapDiscovery(campaignId);
      const validIds = new Set(fixedManifest.hexes.map(hex => hex.id));
      const normalizedDiscovery: MapDiscovery = {
        ...savedDiscovery,
        currentHexId: savedDiscovery.currentHexId && validIds.has(savedDiscovery.currentHexId) ? savedDiscovery.currentHexId : null,
        discoveredHexIds: (savedDiscovery.discoveredHexIds || []).filter(id => validIds.has(id)),
        visitedHexIds: (savedDiscovery.visitedHexIds || []).filter(id => validIds.has(id)),
        routeHexIds: (savedDiscovery.routeHexIds || []).filter(id => validIds.has(id)),
        plannedRouteHexIds: (savedDiscovery.plannedRouteHexIds || []).filter(id => validIds.has(id)),
      };
      await onDiscoveryChanged(normalizedDiscovery, 'normalização do estado após importação do mapa');
      const reloadedLocations = await loadTerraMediaLocations();
      setManifest(fixedManifest);
      setDiscovery(normalizedDiscovery);
      setLocations(reloadedLocations);
      setSelected(null);
      setSelectedLocation(null);
      setMapImageRevision(value => value + 1);
      setMessage(`Pacote importado: ${result.importedFiles.length} arquivos, ${fixedManifest.hexCount} hexes, ${result.locationCount} locais, imagem ${result.imageWidth}×${result.imageHeight}.`);
    } catch (reason) {
      console.error('BRASEIRO_MAPA: importação do pacote falhou', reason);
      setMessage('');
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      setIsImportingPackage(false);
    }
  };


  const currentPoiPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    const locationKey = normalizePoiAssetKey(currentLocation);
    const asset = resolvePoiAsset(immersiveAssets, locationKey) || resolvePoiAssetsForHex(immersiveAssets, discovery.currentHexId)[0] || null;
    if (!asset) return null;
    return {
      asset,
      left: camera.x + hex.centerX * camera.scale,
      top: camera.y + hex.centerY * camera.scale,
    };
  }, [immersive, discovery?.currentHexId, byId, immersiveAssets, currentLocation, camera.x, camera.y, camera.scale]);

  const selectedPoiPresentation = useMemo(() => {
    if (!immersive || !selected) return null;
    const location = selectedLocation || preferredLocationForHex(selected.id);
    const keys = [location?.name, location?.nameOriginal, ...(location?.mapNames || []), ...(location?.aliases || []), selected.poi, selected.name]
      .map(normalizePoiAssetKey).filter((value): value is string => Boolean(value));
    const asset = keys.map(key => resolvePoiAsset(immersiveAssets, key)).find(Boolean)
      || resolvePoiAssetsForHex(immersiveAssets, selected.id)[0]
      || null;
    if (!asset) return null;
    return { asset, location, left: camera.x + selected.centerX * camera.scale, top: camera.y + selected.centerY * camera.scale };
  }, [immersive, selected, selectedLocation, preferredLocationForHex, immersiveAssets, camera.x, camera.y, camera.scale]);

  const currentPartyPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId || !partyCharacters.length) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    return { left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, discovery?.currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);

  return (
    <section id="atlas-terra-media" className={immersive ? 'relative h-full w-full overflow-hidden bg-transparent' : `${expanded ? 'fixed inset-0 z-[250] p-2 sm:p-4' : 'relative h-[360px] w-full'} flex flex-col overflow-hidden rounded-lg border-2 border-blue-900 bg-[#050403] shadow-xl`}>
      <input ref={packageImportRef} type="file" accept=".zip,application/zip,application/x-zip-compressed" className="hidden" onChange={importMapPackage} />
      {!immersive && <header className="flex flex-wrap items-center justify-between gap-2 border-b border-blue-900/60 bg-black/80 px-3 py-2">
        <div className="flex min-w-0 items-center gap-2">
          <MapIcon className="h-4 w-4 shrink-0 text-blue-500" />
          <div className="min-w-0">
            <div className="truncate font-serif text-[10px] font-black uppercase tracking-wider text-blue-400">Atlas de Eriador</div>
            <div className="truncate text-[9px] text-blue-700">{currentLocation} · {size.width ? `${size.width}×${size.height}` : 'carregando'}</div>
          </div>
        </div>

        <div className="order-3 flex w-full items-center sm:order-none sm:w-auto sm:flex-1 sm:justify-center sm:px-4">
          <div ref={searchRef} className="relative w-full max-w-sm">
            <div className="flex items-center gap-2 rounded-lg bg-blue-950/20 px-3 py-1.5 ring-1 ring-blue-900/40 transition-all focus-within:bg-blue-950/40 focus-within:ring-blue-500/50">
              <Search className="h-3.5 w-3.5 text-blue-700" />
              <input
                type="text"
                placeholder="Buscar local..."
                className="w-full bg-transparent text-[11px] text-blue-100 placeholder:text-blue-900 focus:outline-none"
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setIsSearching(true); }}
                onFocus={() => setIsSearching(true)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    submitSearch();
                  }
                }}
              />
              {searchQuery && (
                <button type="button" onClick={() => setSearchQuery('')} aria-label="Limpar busca">
                  <X className="h-3 w-3 text-blue-700 hover:text-blue-500" />
                </button>
              )}
              <button
                type="button"
                onClick={submitSearch}
                className="rounded border border-blue-800/70 px-2 py-1 text-[9px] font-bold uppercase text-blue-300 hover:bg-blue-800/30"
                aria-label="Ir ao local encontrado"
              >
                Ir
              </button>
            </div>
            
            {isSearching && Boolean(searchQuery.trim()) && (
                <div
                  className="absolute left-0 right-0 top-full z-[300] mt-1 overflow-hidden rounded-lg border border-blue-900/60 bg-black/95 shadow-2xl backdrop-blur-md"
                >
                  <div className="max-h-[240px] overflow-y-auto">
                    {searchResults.length === 0 && (
                      <div className="px-3 py-3 text-[10px] leading-relaxed text-blue-500">
                        Nenhum local corresponde à busca. Pesquise pelo nome em português, nome original, alias, região ou ID do hex, por exemplo <strong>AP27</strong>.
                      </div>
                    )}
                    {searchResults.map(result => {
                      const location = result.kind === 'location' ? result.location : null;
                      const hex = result.kind === 'hex' ? result.hex : null;
                      return (
                        <button
                          key={location ? `location:${location.id}` : `hex:${hex!.id}`}
                          onClick={() => openSearchResult(result)}
                          className="group flex w-full items-center justify-between border-b border-blue-900/10 px-3 py-2 text-left last:border-0 hover:bg-blue-500/10"
                        >
                          <div className="min-w-0">
                            <div className="truncate text-[11px] font-bold text-blue-200 group-hover:text-blue-100">
                              {location ? location.name : (hex!.name || hex!.poi || shortHexLabel(hex!))}
                            </div>
                            <div className="flex items-center gap-1.5 text-[9px] uppercase tracking-tighter text-blue-800 group-hover:text-blue-600">
                              {location ? (
                                <>
                                  <span>{location.nameOriginal || location.type}</span>
                                  <span className="opacity-30">•</span>
                                  <span>{location.regions[0] || location.type}</span>
                                  <span className="opacity-30">•</span>
                                  <span>{location.hexId || 'sem hex'}</span>
                                  {location.placement.status !== 'verified-label' && <span className="text-purple-500">estimado</span>}
                                </>
                              ) : (
                                <>
                                  <span>{hex!.region || hex!.terrain}</span>
                                  <span className="opacity-30">•</span>
                                  <span>{shortHexLabel(hex!)}</span>
                                </>
                              )}
                            </div>
                          </div>
                          <ChevronRight className="h-3 w-3 text-blue-900 opacity-0 transition-all group-hover:translate-x-0.5 group-hover:opacity-100" />
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
          </div>
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={() => packageImportRef.current?.click()}
            disabled={isImportingPackage}
            className="flex items-center gap-1 rounded border border-emerald-700/80 bg-emerald-950/35 px-2 py-1 text-[9px] font-bold text-emerald-300 disabled:opacity-50"
            title="Importar o ZIP completo com mapa, grade, terrenos e locais"
          >
            <Upload className={`h-3 w-3 ${isImportingPackage ? 'animate-pulse' : ''}`} />
            {isImportingPackage ? 'IMPORTANDO...' : 'IMPORTAR ZIP'}
          </button>
          {manifest && <button onClick={toggleMapViewMode} className="rounded border border-blue-800 px-2 py-1 text-[9px] font-bold text-blue-400" title={gmMode ? 'Mapa completo do Mestre' : 'Fog of war do Jogador'}>{gmMode ? 'MESTRE' : 'JOGADOR'}</button>}
          <button onClick={() => setExpanded(value => !value)} className="rounded border border-blue-900 p-1.5 text-blue-500">{expanded ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}</button>
        </div>
      </header>}

      {!immersive && editingArea && (
          <div
            className="absolute inset-x-2 bottom-3 z-50 sm:left-1/2 sm:right-auto sm:w-[min(92vw,720px)] sm:-translate-x-1/2"
          >
            <div className="overflow-hidden rounded-xl border border-blue-500/30 bg-black/92 p-3 shadow-2xl backdrop-blur-md">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-[11px] font-bold uppercase tracking-wide text-blue-200">Edição da área jogável</div>
                  <p className="mt-1 text-[10px] leading-relaxed text-blue-100/75">Toque num hex para ativá-lo ou excluí-lo da área jogável. A geometria fixa do mapa não será alterada.</p>
                </div>
                <button
                  onClick={async () => { if (manifest) await saveMapManifest(manifest); setEditingArea(false); }}
                  className="flex shrink-0 items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-[11px] font-bold text-white hover:bg-emerald-500"
                >
                  <Check className="h-4 w-4" /> Concluir
                </button>
              </div>
            </div>
          </div>
        )}

      <div ref={containerRef} className={immersive ? 'relative h-full w-full overflow-hidden bg-transparent' : 'relative min-h-0 flex-1 overflow-hidden bg-black'} style={{ touchAction: 'none' }}>
        <canvas
          ref={canvasRef}
          className="absolute inset-0 h-full w-full cursor-crosshair"
          onPointerDown={pointerDown}
          onPointerMove={pointerMove}
          onPointerUp={pointerUp}
          onPointerCancel={pointerUp}
          onWheel={event => { event.preventDefault(); zoom(Math.exp(-event.deltaY * .001), event.clientX, event.clientY); }}
        />
        {immersive && currentPoiPresentation && (!selectedPoiPresentation || selected?.id !== discovery?.currentHexId) && (
          <img
            src={currentPoiPresentation.asset}
            alt="Ilustração do local atual"
            draggable={false}
            className="pointer-events-none absolute z-[12] max-h-[30vh] w-[min(48vw,250px)] select-none object-contain opacity-[0.82]"
            style={{
              left: currentPoiPresentation.left,
              top: currentPoiPresentation.top,
              transform: 'translate(-50%, -72%)',
              mixBlendMode: 'multiply',
              filter: 'sepia(.18) saturate(.72) contrast(1.02)',
            }}
          />
        )}
        {immersive && selectedPoiPresentation && (
          <img
            src={selectedPoiPresentation.asset}
            alt={`Ilustração de ${selectedPoiPresentation.location?.name || selected?.name || 'local'}`}
            draggable={false}
            style={{
              pointerEvents: 'none', position: 'absolute', zIndex: 22, maxHeight: '24vh', width: 'min(42vw, 210px)',
              objectFit: 'contain', opacity: .94, left: selectedPoiPresentation.left, top: selectedPoiPresentation.top,
              transform: 'translate(-50%, -86%)', mixBlendMode: 'multiply',
              filter: 'sepia(.12) saturate(.78) contrast(1.04) drop-shadow(0 5px 5px rgba(0,0,0,.45))',
            }}
          />
        )}
        {immersive && currentPartyPresentation && (
          <PartyCluster
            characters={partyCharacters}
            manifest={immersiveAssets}
            position={{ left: currentPartyPresentation.left, top: currentPartyPresentation.top }}
            mapMarker
          />
        )}
        {!immersive && <div className="absolute left-2 top-2 flex flex-col rounded bg-black/75 p-1">
          <button onClick={() => zoom(1.35)} className="p-1.5 text-blue-400" title="Aumentar Zoom"><ZoomIn className="h-4 w-4" /></button>
          <button onClick={() => zoom(1 / 1.35)} className="p-1.5 text-blue-400" title="Diminuir Zoom"><ZoomOut className="h-4 w-4" /></button>
          <button onClick={fit} className="p-1.5 text-blue-400" title="Ver Mapa Inteiro"><Crosshair className="h-4 w-4" /></button>
          {discovery?.currentHexId && (
            <button 
              onClick={() => centerOnHex(discovery.currentHexId!)} 
              className="p-1.5 text-red-500 hover:text-red-400 transition-colors"
              title="Focar na Comitiva"
            >
              <Footprints className="h-4 w-4" />
            </button>
          )}
        </div>}
        {!immersive && manifest && <div className="absolute right-2 top-2 flex flex-col rounded bg-black/75 p-1">
          <button onClick={() => setShowGrid(value => !value)} className={`p-1.5 ${showGrid ? 'text-blue-300' : 'text-blue-800'}`}><Settings className="h-4 w-4" /></button>
          <button onClick={() => setShowIds(value => !value)} className={`p-1.5 ${showIds ? 'text-blue-300' : 'text-blue-800'}`}>{showIds ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}</button>
        </div>}

        {!immersive && !manifest && <div className="absolute inset-x-4 bottom-4 rounded border border-blue-800 bg-black/90 p-3 text-center text-[10px] text-blue-300">
          A malha fixa do mapa não pôde ser carregada. Importe novamente o pacote ZIP oficial.
        </div>}

        {immersive && selected && !editingArea && (
          <div style={{
            position: 'absolute', left: 10, right: 10, bottom: 150, zIndex: 80,
            borderRadius: 12, padding: 10, color: '#f5e8c7',
            background: 'linear-gradient(180deg, rgba(20,15,10,.96), rgba(8,6,4,.96))',
            border: '1px solid rgba(195,151,85,.62)', boxShadow: '0 10px 28px rgba(0,0,0,.55)',
          }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ minWidth: 0, flex: 1 }}>
                <div style={{ fontSize: 11, opacity: .7, textTransform: 'uppercase', letterSpacing: '.06em' }}>{shortHexLabel(selected)}{discovery?.currentHexId === selected.id ? ' · Companhia aqui' : ''}</div>
                <div style={{ fontSize: 16, fontWeight: 800, lineHeight: 1.15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {selectedLocation?.name || selected.name || selected.poi || 'Hex selecionado'}
                </div>
                <div style={{ marginTop: 3, fontSize: 11, opacity: .74 }}>
                  {[selected.region, selected.terrain, selectedLocation?.type].filter(Boolean).join(' · ')}
                </div>
              </div>
              <button type="button" onClick={() => { setSelected(null); setSelectedLocation(null); }} style={{ width: 34, height: 34, borderRadius: 8, border: '1px solid rgba(195,151,85,.35)', background: 'rgba(255,255,255,.04)', color: '#ead9b7' }}>×</button>
            </div>
            {selectedLocation?.description && <div style={{ marginTop: 7, maxHeight: 54, overflow: 'hidden', fontSize: 11, lineHeight: 1.35, opacity: .82 }}>{selectedLocation.description}</div>}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 7, marginTop: 9 }}>
              <button type="button" onClick={() => void moveGroup()} disabled={!selected.playable} style={{ minHeight: 44, borderRadius: 8, border: '1px solid #8a5f2f', background: '#5a3a1d', color: '#fff0cd', fontWeight: 800, opacity: selected.playable ? 1 : .45 }}>
                {discovery?.currentHexId ? 'Mover aqui' : 'Posicionar'}
              </button>
              <button type="button" onClick={() => void planRouteToSelected()} disabled={!discovery?.currentHexId || discovery.currentHexId === selected.id} style={{ minHeight: 44, borderRadius: 8, border: '1px solid rgba(195,151,85,.5)', background: 'rgba(255,255,255,.06)', color: '#ead9b7', fontWeight: 700, opacity: !discovery?.currentHexId || discovery.currentHexId === selected.id ? .4 : 1 }}>
                Planejar rota
              </button>
            </div>
          </div>
        )}

        {!immersive && selected && !editingArea && <div className="absolute inset-x-2 bottom-2 rounded border border-blue-700 bg-black/95 p-3 text-[10px] text-blue-100 sm:left-auto sm:w-80">
          <div className="mb-2 flex items-center justify-between">
            <div className="flex flex-col gap-0.5 font-mono font-bold text-blue-400">
              <div className="flex items-center gap-2"><MapPin className="h-4 w-4" />{shortHexLabel(selected)}</div>
              {discovery?.currentHexId === selected.id && (
                <div className="mt-0.5 flex items-center gap-1 text-[9px] uppercase tracking-wider text-red-300">
                  <Footprints className="h-3 w-3" /> Você está aqui
                </div>
              )}
              {selected.name && <div className="text-[11px] uppercase tracking-wider text-blue-200">{selected.name}</div>}
              {selected.region && <div className="text-[9px] text-blue-500/80">Região: {selected.region}</div>}
              {selected.poi && <div className="text-[9px] italic text-blue-500/80 mt-0.5">POI: {selected.poi}</div>}
              {(selected.placeType || typeof selected.confidence === 'number') && (
                <div className="text-[8px] uppercase tracking-wide text-purple-300/80">
                  {selected.placeType ? `Tipo: ${selected.placeType}` : ''}
                  {selected.placeType && typeof selected.confidence === 'number' ? ' • ' : ''}
                  {typeof selected.confidence === 'number' ? `Confiança IA: ${Math.round(selected.confidence * 100)}%` : ''}
                </div>
              )}
              {selected.tags?.length > 0 && <div className="text-[8px] text-blue-700">{selected.tags.join(' • ')}</div>}
            </div>
            <button onClick={() => { setSelected(null); setSelectedLocation(null); }}><X className="h-4 w-4 text-blue-700" /></button>
          </div>

          {selectedLocation && (
            <div className="mb-3 rounded border border-blue-800/60 bg-blue-950/25 p-2">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="font-serif text-[13px] font-black text-blue-100">{selectedLocation.name}</div>
                  {selectedLocation.nameOriginal && selectedLocation.nameOriginal !== selectedLocation.name && (
                    <div className="text-[9px] italic text-blue-500">{selectedLocation.nameOriginal}</div>
                  )}
                </div>
                <span className={`rounded px-1.5 py-0.5 text-[8px] font-bold uppercase ${
                  selectedLocation.placement.status === 'verified-label'
                    ? 'bg-emerald-950/70 text-emerald-300'
                    : 'bg-purple-950/70 text-purple-300'
                }`}>
                  {selectedLocation.placement.precision === 'printed-symbol-center'
                    ? 'marcador conferido'
                    : selectedLocation.placement.status === 'verified-label'
                      ? 'rótulo conferido'
                      : 'posição derivada'}
                </span>
              </div>
              <div className="mt-1 text-[9px] uppercase tracking-wide text-blue-600">
                {selectedLocation.type}{selectedLocation.regions.length ? ` • ${selectedLocation.regions.join(' / ')}` : ''}
              </div>
              {selectedLocation.description && <p className="mt-1 text-[10px] leading-relaxed text-blue-100/85">{selectedLocation.description}</p>}
              {selectedLocation.description2 && <p className="mt-1 text-[10px] leading-relaxed text-blue-200/65">{selectedLocation.description2}</p>}
              {selectedLocation.placement.status !== 'verified-label' && (
                <p className="mt-1.5 border-t border-purple-900/40 pt-1.5 text-[8px] leading-relaxed text-purple-300/80">
                  O mapa não imprime este nome como rótulo independente; a posição foi derivada de relações geográficas do catálogo e permanece identificada como estimativa.
                </p>
              )}
            </div>
          )}
          
            <>
              <div className="mb-2 space-y-1">
                <select value={selected.terrain} onChange={event => updateTerrain(event.target.value)} className="w-full rounded border border-blue-900 bg-zinc-950 p-2 text-[11px] text-blue-200">
                  {TERRAIN.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
                <div className="flex items-center justify-between px-1 text-[9px] text-blue-500 uppercase font-bold">
                  <span>Dificuldade estimada</span>
                  <span>{TERRAIN_COSTS[selected.terrain] || 1} · Jornada</span>
                </div>
                {discovery?.plannedRouteHexIds?.length ? (
                  <div className="rounded border border-cyan-900/60 bg-cyan-950/20 px-2 py-1.5 text-[9px] leading-relaxed text-cyan-200">
                    Rota futura: {discovery.plannedRouteHexIds.length} etapa(s). Pré-leitura ambiental: próximas {Math.min(5, discovery.plannedRouteHexIds.length)}.
                  </div>
                ) : null}
              </div>
              <div className="grid grid-cols-2 gap-2">
                <button 
                  onClick={moveGroup} 
                  disabled={!selected.playable} 
                  className="flex items-center justify-center gap-2 rounded border border-red-800 bg-red-950/70 px-2 py-2 font-bold text-red-200 disabled:opacity-40 hover:bg-red-900/80 transition-colors"
                >
                  <Footprints className="h-3.5 w-3.5" />
                  {discovery?.currentHexId ? 'Mover aqui' : 'Posicionar'}
                </button>
                {gmMode && <button onClick={toggleDiscovery} className="rounded border border-blue-800 px-2 py-2 text-blue-300 hover:bg-blue-900/20 transition-colors">{discovery?.discoveredHexIds.includes(selected.id) ? 'Ocultar' : 'Descobrir'}</button>}
                <button
                  onClick={() => void planRouteToSelected()}
                  disabled={!discovery?.currentHexId || discovery.currentHexId === selected.id}
                  className="col-span-2 flex items-center justify-center gap-1.5 rounded border border-cyan-700/70 bg-cyan-950/25 px-2 py-2 font-bold text-cyan-200 disabled:opacity-40 hover:bg-cyan-900/40 transition-colors"
                >
                  <ChevronRight className="h-3.5 w-3.5" /> Planejar rota até aqui
                </button>
                {Boolean(discovery?.plannedRouteHexIds?.length) && (
                  <button onClick={() => void clearPlannedRoute()} className="col-span-2 rounded border border-cyan-950 px-2 py-1.5 text-cyan-500 hover:bg-cyan-950/30">
                    Limpar somente a rota futura
                  </button>
                )}
                <button 
                  onClick={() => void identifyHexWithAI()} 
                  className="col-span-2 flex items-center justify-center gap-1.5 rounded border border-purple-800/60 bg-purple-950/20 px-2 py-2 text-purple-300 hover:bg-purple-900/40 transition-colors"
                >
                  <Search className="h-3.5 w-3.5" />
                  Auto-identificar com IA (Terreno/Cidades)
                </button>
                <button onClick={cropHex} className="col-span-2 flex items-center justify-center gap-1 rounded border border-blue-900 px-2 py-1.5 text-blue-500 hover:bg-blue-900/20 transition-colors"><Scissors className="h-3 w-3" />Gerar Recorte PNG</button>
              </div>
            </>
        </div>}
      </div>

      {!immersive && expanded && <footer className="flex flex-wrap items-center gap-2 border-t border-blue-900/60 bg-black/90 px-3 py-2 text-[9px]">
        {manifest && <button onClick={() => { setEditingArea(true); setShowGrid(true); setGmMode(true); }} className="flex items-center gap-1 rounded border border-blue-800 px-2 py-1.5 text-blue-400"><Trash2 className="h-3 w-3" />Editar área jogável</button>}
        {manifest && <button onClick={exportManifest} className="flex items-center gap-1 rounded border border-blue-800 px-2 py-1.5 text-blue-400"><Download className="h-3 w-3" />Exportar manifesto</button>}
        <button onClick={() => packageImportRef.current?.click()} disabled={isImportingPackage} className="flex items-center gap-1 rounded border border-emerald-700 px-2 py-1.5 text-emerald-300 disabled:opacity-50"><Upload className="h-3 w-3" />Importar ZIP completo</button>
        <button onClick={async () => { await clearHexCropCache(); setMessage('Cache de recortes limpo.'); }} className="rounded border border-blue-950 px-2 py-1.5 text-blue-700">Limpar recortes</button>
        {manifest && <span className="ml-auto flex items-center gap-3 text-blue-700">
          {discovery && (
            <div className="flex items-center gap-2 border-r border-blue-900/40 pr-3">
              <span className="text-blue-500/50 uppercase font-bold tracking-tighter">Fadiga:</span>
              <span className="text-[10px] font-bold text-cyan-400">nas fichas</span>
              {onPartyRested && (
                <button
                  onClick={restGroup}
                  className="ml-1 rounded bg-blue-950/50 px-1.5 py-0.5 text-blue-200 hover:bg-blue-900/80 transition-colors border border-blue-800/40"
                >
                  Resolver descanso
                </button>
              )}
            </div>
          )}
          <span>{manifest.hexes.filter(hex => hex.playable).length}/{manifest.hexCount} jogáveis</span>
        </span>}
      </footer>}

      {(message || error) && (
        <div className={`absolute bottom-4 left-4 right-4 z-[100] rounded-lg border px-4 py-3 shadow-2xl backdrop-blur-md transition-all sm:left-auto sm:right-4 sm:w-80 ${
          error ? 'border-red-500/50 bg-red-950/90 text-red-200' : 'border-blue-500/50 bg-blue-950/90 text-blue-200'
        }`}>
          <div className="flex items-start justify-between gap-3">
            <p className="text-[11px] leading-relaxed font-medium">
              {error || message}
            </p>
            <button onClick={() => { setError(null); setMessage(''); }} className="shrink-0 text-current opacity-50 hover:opacity-100">
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </section>
  );
};
