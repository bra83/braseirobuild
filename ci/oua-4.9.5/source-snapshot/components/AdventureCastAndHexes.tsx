import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Cloud,
  CloudFog,
  CloudLightning,
  CloudRain,
  Diamond,
  MapPin,
  Moon,
  Skull,
  Snowflake,
  Sun,
  Sunrise,
  Sunset,
  User,
} from 'lucide-react';
import { currentBarbaraRenderPolicy } from '../lib/barbaraBridge';
import { Character, IndividualToken, Npc, SceneNode, getStoreData } from '../lib/db';
import type { AutonomousEventRecord } from '../lib/autonomy/types';
import {
  EriadorHex,
  EriadorMapManifest,
  loadMapDiscovery,
  loadRuntimeMapManifest,
  MAP_DISCOVERY_CHANGED_EVENT,
  loadStoredMapImageBlob,
} from '../lib/map';

interface Props {
  campaignId: string;
  npcs: Npc[];
  history: SceneNode[];
  currentNode: SceneNode | null;
  partyCharacterIds: string[];
  onNpcSelect?: (npc: Npc) => void;
  onOpenAtlas?: () => void;
}

interface MiniMapState {
  manifest: EriadorMapManifest;
  current: EriadorHex;
  hexes: EriadorHex[];
  revealed: Set<string>;
}

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function weatherPresentation(weather = '') {
  const text = normalize(weather);
  if (/tempest|trovao|relamp/.test(text)) return { label: 'Tempestade', details: weather, Icon: CloudLightning };
  if (/chuva|garoa/.test(text)) return { label: 'Chuva', details: weather, Icon: CloudRain };
  if (/neve|granizo/.test(text)) return { label: 'Neve', details: weather, Icon: Snowflake };
  if (/nevoa|neblina|bruma/.test(text)) return { label: 'Névoa', details: weather, Icon: CloudFog };
  if (/frio/.test(text) && /nubl|encoberto/.test(text)) return { label: 'Frio e nublado', details: weather, Icon: Cloud };
  if (/nubl|encoberto/.test(text)) return { label: 'Nublado', details: weather, Icon: Cloud };
  if (/frio/.test(text)) return { label: 'Frio', details: weather, Icon: Cloud };
  return { label: 'Céu limpo', details: weather, Icon: Sun };
}

function periodPresentation(node: SceneNode | null) {
  const explicit = normalize(node?.timeOfDay || '');
  if (/madrugada|amanhecer|aurora/.test(explicit)) return { label: node?.timeOfDay || 'Amanhecer', Icon: Sunrise };
  if (/entardecer|crepusculo|poente/.test(explicit)) return { label: node?.timeOfDay || 'Entardecer', Icon: Sunset };
  if (/noite/.test(explicit)) return { label: node?.timeOfDay || 'Noite', Icon: Moon };
  if (/manha|meio dia|meio-dia|tarde/.test(explicit)) return { label: node?.timeOfDay || 'Dia', Icon: Sun };
  const text = normalize(`${node?.atmospheric_narration || ''} ${node?.action_result || ''} ${node?.weather || ''}`);
  if (/madrugada|amanhecer|aurora/.test(text)) return { label: 'Amanhecer', Icon: Sunrise };
  if (/entardecer|crepusculo|poente|por do sol/.test(text)) return { label: 'Entardecer', Icon: Sunset };
  if (/noite|luar|lua |estrelas|escuridao/.test(text)) return { label: 'Noite', Icon: Moon };
  return { label: 'Dia', Icon: Sun };
}

function journeyDay(history: SceneNode[]): number {
  let day = 1;
  for (const node of history) {
    for (const event of node.events || []) {
      if (event.type === 'journey_day_completed') day += Math.max(1, Number(event.data?.days || 1));
    }
  }
  return Math.max(1, day);
}

function mapPoint(point: { x: number; y: number }, bounds: { left: number; top: number; width: number; height: number }, width: number, height: number) {
  return {
    x: (point.x - bounds.left) / bounds.width * width,
    y: (point.y - bounds.top) / bounds.height * height,
  };
}

function tracePolygon(
  ctx: CanvasRenderingContext2D,
  polygon: Array<{ x: number; y: number }>,
  bounds: { left: number; top: number; width: number; height: number },
  width: number,
  height: number,
) {
  const points = polygon.map(point => mapPoint(point, bounds, width, height));
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  points.slice(1).forEach(point => ctx.lineTo(point.x, point.y));
  ctx.closePath();
}

function HexCluster({ state, onOpenAtlas }: { state: MiniMapState; onOpenAtlas?: () => void }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const objectUrlRef = useRef<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let disposed = false;
    const image = new Image();
    image.decoding = 'async';
    image.onload = () => {
      if (disposed) return;
      imageRef.current = image;
      setReady(true);
    };
    image.onerror = () => { if (!disposed) setReady(false); };
    void loadStoredMapImageBlob().then(blob => {
      if (disposed) return;
      if (blob) {
        objectUrlRef.current = URL.createObjectURL(blob);
        image.src = objectUrlRef.current;
      } else image.src = './map.png';
    }).catch(() => { image.src = './map.png'; });
    return () => {
      disposed = true;
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    };
  }, [state.manifest.updatedAt]);

  useEffect(() => {
    const canvas = canvasRef.current;
    const image = imageRef.current;
    if (!canvas || !image || !ready) return;
    const draw = () => {
      const rect = canvas.getBoundingClientRect();
      const cssWidth = Math.max(280, rect.width || 560);
      const cssHeight = Math.max(205, Math.round(cssWidth * 0.66));
      const barbaraRender = currentBarbaraRenderPolicy();
    const dpr = Math.max(1, Math.min(window.devicePixelRatio || 1, 3, barbaraRender.maxDpr) * barbaraRender.renderScale);
      canvas.width = Math.round(cssWidth * dpr);
      canvas.height = Math.round(cssHeight * dpr);
      canvas.style.height = `${cssHeight}px`;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.imageSmoothingEnabled = true;
      ctx.imageSmoothingQuality = 'high';
      ctx.fillStyle = '#000';
      ctx.fillRect(0, 0, cssWidth, cssHeight);

      const { vectorQ, vectorR } = state.manifest.layout;
      const neighborVectors = [
        vectorQ,
        { x: -vectorQ.x, y: -vectorQ.y },
        vectorR,
        { x: -vectorR.x, y: -vectorR.y },
        { x: vectorQ.x - vectorR.x, y: vectorQ.y - vectorR.y },
        { x: vectorR.x - vectorQ.x, y: vectorR.y - vectorQ.y },
      ];
      const availableNeighbors = state.hexes.filter(hex => hex.id !== state.current.id);
      const claimed = new Set<string>();
      const displayCells: Array<{ hex: EriadorHex | null; polygon: Array<{ x: number; y: number }>; current: boolean }> = [
        { hex: state.current, polygon: state.current.polygon, current: true },
        ...neighborVectors.map(vector => {
          const expected = { x: state.current.centerX + vector.x, y: state.current.centerY + vector.y };
          const nearest = availableNeighbors
            .filter(hex => !claimed.has(hex.id))
            .map(hex => ({ hex, distance: Math.hypot(hex.centerX - expected.x, hex.centerY - expected.y) }))
            .sort((a, b) => a.distance - b.distance)[0];
          const tolerance = Math.max(6, Math.hypot(vector.x, vector.y) * 0.46);
          if (nearest && nearest.distance <= tolerance) {
            claimed.add(nearest.hex.id);
            return { hex: nearest.hex, polygon: nearest.hex.polygon, current: false };
          }
          return {
            hex: null,
            current: false,
            polygon: state.current.polygon.map(point => ({ x: point.x + vector.x, y: point.y + vector.y })),
          };
        }),
      ];

      const allPoints = displayCells.flatMap(cell => cell.polygon);
      const minX = Math.min(...allPoints.map(point => point.x));
      const maxX = Math.max(...allPoints.map(point => point.x));
      const minY = Math.min(...allPoints.map(point => point.y));
      const maxY = Math.max(...allPoints.map(point => point.y));
      const padX = Math.max(2, (maxX - minX) * 0.08);
      const padY = Math.max(2, (maxY - minY) * 0.08);
      const bounds = { left: minX - padX, top: minY - padY, width: maxX - minX + padX * 2, height: maxY - minY + padY * 2 };

      for (const cell of displayCells) {
        const isRevealed = Boolean(cell.current || (cell.hex && state.revealed.has(cell.hex.id)));
        tracePolygon(ctx, cell.polygon, bounds, cssWidth, cssHeight);
        if (cell.hex && isRevealed) {
          ctx.save();
          ctx.clip();
          // A imagem e os polígonos usam a mesma transformação canônica.
          ctx.translate((-bounds.left / bounds.width) * cssWidth, (-bounds.top / bounds.height) * cssHeight);
          ctx.scale(cssWidth / bounds.width, cssHeight / bounds.height);
          ctx.drawImage(image, 0, 0);
          ctx.restore();
          tracePolygon(ctx, cell.polygon, bounds, cssWidth, cssHeight);
          ctx.strokeStyle = cell.current ? '#f2bd52' : 'rgba(247,224,164,.80)';
          ctx.lineWidth = cell.current ? 3 : 1.25;
          ctx.setLineDash([]);
          ctx.stroke();
        } else {
          ctx.fillStyle = '#000';
          ctx.fill();
          tracePolygon(ctx, cell.polygon, bounds, cssWidth, cssHeight);
          ctx.strokeStyle = '#b93434';
          ctx.lineWidth = 1.8;
          ctx.setLineDash([7, 6]);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }

      const center = mapPoint({ x: state.current.centerX, y: state.current.centerY }, bounds, cssWidth, cssHeight);
      ctx.font = `700 ${Math.max(17, cssWidth * 0.043)}px ui-serif, Georgia, serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.lineWidth = 5;
      ctx.strokeStyle = 'rgba(0,0,0,.85)';
      ctx.strokeText(state.current.id, center.x, center.y);
      ctx.fillStyle = '#fff9e9';
      ctx.fillText(state.current.id, center.x, center.y);
    };
    draw();
    const observer = new ResizeObserver(draw);
    observer.observe(canvas);
    return () => observer.disconnect();
  }, [ready, state]);

  return (
    <button type="button" onClick={onOpenAtlas} className="block w-full text-left" aria-label="Abrir o Atlas no hex atual">
      <canvas ref={canvasRef} className="block w-full rounded-sm bg-black shadow-inner" />
      <div className="pt-2 text-center text-xs text-[#c8a56d]">toque no mapa para abrir o Atlas</div>
    </button>
  );
}


function UnpositionedHexCluster({ onOpenAtlas }: { onOpenAtlas?: () => void }) {
  const points = '0,-28 24,-14 24,14 0,28 -24,14 -24,-14';
  const centers = [
    [100, 82], [100, 30], [145, 56], [145, 108], [100, 134], [55, 108], [55, 56],
  ];
  return (
    <button type="button" onClick={onOpenAtlas} className="block w-full text-left" aria-label="Abrir o Atlas e definir a posição">
      <svg viewBox="0 0 200 164" className="block w-full rounded-sm bg-black" role="img" aria-label="Sete hexágonos ainda cobertos pela névoa">
        {centers.map(([x, y], index) => (
          <g key={`${x}_${y}`} transform={`translate(${x} ${y})`}>
            <polygon points={points} fill="#000" stroke="#b93434" strokeWidth="2" strokeDasharray="6 5" />
            {index === 0 && <text x="0" y="5" textAnchor="middle" fill="#fff1cf" fontSize="20" fontFamily="Georgia, serif" fontWeight="700">?</text>}
          </g>
        ))}
      </svg>
      <div className="pt-2 text-center text-xs text-[#c8a56d]">posição ainda não definida — toque para abrir o Atlas</div>
    </button>
  );
}

export function AdventureCastAndHexes({
  campaignId,
  npcs,
  history,
  currentNode,
  partyCharacterIds,
  onNpcSelect,
  onOpenAtlas,
}: Props) {
  const [characters, setCharacters] = useState<Character[]>([]);
  const [tokenUrls, setTokenUrls] = useState<Record<string, string>>({});
  const [miniMap, setMiniMap] = useState<MiniMapState | null>(null);
  const [autonomousSignals, setAutonomousSignals] = useState<AutonomousEventRecord[]>([]);

  useEffect(() => {
    let dead = false;
    const urls: string[] = [];
    Promise.all([
      getStoreData<Character>('characters'),
      getStoreData<IndividualToken>('individual_tokens'),
    ]).then(([allCharacters, tokens]) => {
      if (dead) return;
      const party = partyCharacterIds.length
        ? allCharacters.filter(character => partyCharacterIds.includes(character.id))
        : allCharacters.slice(0, 1);
      setCharacters(party);
      const tokenMap: Record<string, string> = {};
      tokens.forEach(token => {
        const url = URL.createObjectURL(token.blob);
        urls.push(url);
        tokenMap[token.id] = url;
      });
      setTokenUrls(tokenMap);
    }).catch(error => console.error('BRASEIRO_CENA: falha ao montar presentes', error));
    return () => {
      dead = true;
      urls.forEach(url => URL.revokeObjectURL(url));
    };
  }, [partyCharacterIds.join('|'), npcs.map(npc => `${npc.id}:${npc.tokenId || ''}`).join('|')]);

  useEffect(() => {
    let dead = false;
    const refreshMap = async () => {
      try {
        const [manifest, discovery, signalRows] = await Promise.all([
          loadRuntimeMapManifest(),
          loadMapDiscovery(campaignId),
          getStoreData<AutonomousEventRecord>('autonomous_events').catch(() => []),
        ]);
        if (dead || !manifest) return;
        const now = Date.now();
        const byId = new Map(manifest.hexes.map(hex => [hex.id, hex]));
        const candidates = [
          discovery.currentHexId,
          ...(discovery.routeHexIds || []).slice().reverse(),
          ...(discovery.visitedHexIds || []).slice().reverse(),
          ...(discovery.discoveredHexIds || []).slice().reverse(),
        ].filter((id): id is string => Boolean(id));
        const current = candidates.map(id => byId.get(id)).find((hex): hex is EriadorHex => Boolean(hex));
        if (!current) { setMiniMap(null); setAutonomousSignals([]); return; }
        setAutonomousSignals(signalRows
          .filter(item => item.campaignId === campaignId && item.hexId === current.id && item.status === 'suggested' && item.expiresAt > now)
          .sort((a, b) => b.createdAt - a.createdAt)
          .slice(0, 3));
        const neighbors = current.neighbors.map(id => byId.get(id)).filter((hex): hex is EriadorHex => Boolean(hex)).slice(0, 6);
        const revealed = new Set([...(discovery.discoveredHexIds || []), ...(discovery.visitedHexIds || []), current.id]);
        setMiniMap({ manifest, current, hexes: [current, ...neighbors], revealed });
      } catch (error) {
        if (!dead) console.error('BRASEIRO_CENA: falha ao atualizar posição', error);
      }
    };
    const onDiscoveryChanged = (event: Event) => {
      const detail = (event as CustomEvent<{ campaignId?: string }>).detail;
      if (!detail?.campaignId || detail.campaignId === campaignId) void refreshMap();
    };
    void refreshMap();
    window.addEventListener(MAP_DISCOVERY_CHANGED_EVENT, onDiscoveryChanged);
    return () => {
      dead = true;
      window.removeEventListener(MAP_DISCOVERY_CHANGED_EVENT, onDiscoveryChanged);
    };
  }, [campaignId, currentNode?.id]);


  const weather = weatherPresentation(currentNode?.weather || '');
  const period = periodPresentation(currentNode);
  const day = journeyDay(history);
  const cast = useMemo(() => [
    ...characters.map(character => ({
      id: character.id,
      name: character.name,
      subtitle: character.vocation || 'Aventureiro',
      tokenId: character.sheet.tokenId,
      monster: false,
      category: 'Aventureiros' as const,
      npc: null as Npc | null,
    })),
    ...npcs.map(npc => ({
      id: npc.id,
      name: npc.name,
      subtitle: npc.role || 'NPC',
      tokenId: npc.tokenId,
      monster: /orc|troll|lobo|aranha|monstro|criatura|inimigo|espectro|nazgul|warg|dragao|fera/i.test(`${npc.role} ${npc.culture} ${npc.peoples}`),
      category: (/orc|troll|lobo|aranha|monstro|criatura|inimigo|espectro|nazgul|warg|dragao|fera/i.test(`${npc.role} ${npc.culture} ${npc.peoples}`) ? 'Monstros' : 'NPCs') as 'NPCs' | 'Monstros',
      npc,
    })),
  ], [characters, npcs]);

  return (
    <div className="space-y-4">
      <section className="rounded-md border-2 border-[#7b542b] bg-[#2a180d]/95 px-4 py-5 shadow-lg">
        <div className="grid grid-cols-3 gap-3 text-center text-[#f0d6a4]">
          {[
            { label: weather.label, Icon: weather.Icon },
            { label: `Dia ${day}`, Icon: Diamond },
            { label: period.label, Icon: period.Icon },
          ].map(({ label, Icon }) => (
            <div key={label} className="flex min-w-0 flex-col items-center gap-2">
              <Icon className="h-8 w-8 text-[#f0b84f]" strokeWidth={1.8} />
              <span className="max-w-full break-words text-center text-[10px] font-black uppercase leading-tight tracking-wide sm:text-xs">{label}</span>
            </div>
          ))}
        </div>
      </section>

      {autonomousSignals.length > 0 && (
        <section className="rounded-md border border-cyan-900/70 bg-slate-950/85 p-4 shadow-lg">
          <h2 className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-cyan-200">
            <Diamond className="h-4 w-4" /> Leitura autônoma do caminho
          </h2>
          <p className="mt-1 text-[11px] leading-relaxed text-slate-400">Possibilidades calculadas pelo hex, estação, relevo e rota. São prenúncios plausíveis, não acontecimentos obrigatórios.</p>
          <div className="mt-3 space-y-2">
            {autonomousSignals.map(signal => (
              <div key={signal.id} className="rounded border border-slate-800 bg-black/30 p-3">
                <div className="flex items-start justify-between gap-3">
                  <strong className="text-xs text-slate-100">{signal.title}</strong>
                  <span className="shrink-0 text-[10px] font-black text-cyan-300">{Math.round(signal.probability * 100)}%</span>
                </div>
                <p className="mt-1 text-[11px] leading-relaxed text-slate-300">{signal.description}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="rounded-md border-2 border-[#7b542b] bg-[#2a180d]/95 p-4 shadow-lg">
        <h2 className="text-sm font-black uppercase tracking-wide text-[#f0b84f] sm:text-base">Presentes na cena e posição</h2>
        {cast.length > 0 && (
          <div className="mt-3 space-y-4">
            {(['Aventureiros', 'NPCs', 'Monstros'] as const).map(category => {
              const entities = cast.filter(entity => entity.category === category);
              if (!entities.length) return null;
              return (
                <div key={category}>
                  <div className="text-xs font-bold uppercase tracking-wide text-[#caa36a]">{category}</div>
                  <div className="mt-3 flex gap-4 overflow-x-auto pb-2">
                    {entities.map(entity => {
                      const content = entity.tokenId && tokenUrls[entity.tokenId]
                        ? (
                          <img
                            src={tokenUrls[entity.tokenId]}
                            alt={entity.name}
                            className="scene-cast-token-image"
                            draggable={false}
                            style={{
                              display: 'block',
                              width: '100%',
                              height: '100%',
                              minWidth: 0,
                              minHeight: 0,
                              maxWidth: '100%',
                              maxHeight: '100%',
                              objectFit: 'cover',
                              objectPosition: 'center',
                            }}
                          />
                        )
                        : entity.monster
                          ? <Skull className="h-9 w-9 text-red-400" />
                          : <User className="h-9 w-9 text-[#e8b75a]" />;
                      return (
                        <button
                          type="button"
                          key={entity.id}
                          onClick={() => entity.npc && onNpcSelect?.(entity.npc)}
                          className="scene-cast-entity-button w-24 shrink-0 text-center disabled:cursor-default"
                          style={{ width: 96, minWidth: 96, maxWidth: 96 }}
                          disabled={!entity.npc}
                        >
                          <span
                            className={`scene-cast-token-frame mx-auto flex items-center justify-center overflow-hidden rounded-full bg-black shadow-md ${entity.monster ? 'border-red-700' : 'border-[#e8b75a]'}`}
                            style={{
                              display: 'flex',
                              width: 76,
                              height: 76,
                              minWidth: 76,
                              minHeight: 76,
                              maxWidth: 76,
                              maxHeight: 76,
                              flex: '0 0 76px',
                              borderWidth: 3,
                              borderStyle: 'solid',
                              borderColor: entity.monster ? '#b91c1c' : '#e8b75a',
                              borderRadius: '9999px',
                              overflow: 'hidden',
                              boxSizing: 'border-box',
                            }}
                          >
                            {content}
                          </span>
                          <span className="mt-2 block truncate text-sm font-bold text-[#f4dfba]">{entity.name}</span>
                          <span className="mt-1 block truncate text-[10px] font-bold uppercase tracking-wide text-[#c49a5a]">{entity.subtitle}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <div className="mt-5">
          <div className="mb-3 flex items-center justify-center gap-2 text-xs font-black uppercase tracking-wide text-[#e8b75a] sm:text-sm">
            <MapPin className="h-4 w-4" /> {miniMap ? `Posição • Hex ${miniMap.current.id}` : 'Posição • não definida'}
          </div>
          <div className="mx-auto max-w-[620px] bg-black p-0">
            {miniMap ? <HexCluster state={miniMap} onOpenAtlas={onOpenAtlas} /> : <UnpositionedHexCluster onOpenAtlas={onOpenAtlas} />}
          </div>
        </div>
      </section>
    </div>
  );
}
