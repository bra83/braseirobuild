import React, { useEffect, useMemo, useState } from 'react';
import type { Character, IndividualToken, Npc, PendingTest, SceneNode } from '../../lib/db';
import { getStoreData } from '../../lib/db';
import type { MasterState } from '../../lib/master/types';
import type { StoryChoice } from '../../types';
import type { MapDiscovery } from '../../lib/map';
import type { PartyMapMove } from '../EriadorMap';
import { EriadorMap } from '../EriadorMap';
import { loadImmersiveAssetManifest, resolveImmersiveAsset, type ImmersiveAssetManifest } from '../../lib/immersiveAssets';

interface Props {
  campaignId: string;
  currentNode: SceneNode | null;
  currentImage?: string | null;
  characters: Character[];
  presentNpcs: Npc[];
  masterState: MasterState;
  customAction: string;
  setCustomAction: (value: string) => void;
  onChoice: (choice: StoryChoice) => void;
  onCustomActionSubmit: (event: React.FormEvent, isDmMode: boolean) => void;
  onPartyMoved: (move: PartyMapMove) => void | Promise<void>;
  onMapDiscoveryChanged: (discovery: MapDiscovery, reason: string) => void | Promise<void>;
  onPartyRested?: () => void | Promise<void>;
  onOpenDice: () => void;
  onStartAdventure: () => void | Promise<void>;
  canStartAdventure: boolean;
  isStoryLoading?: boolean;
  storyError?: string | null;
  onDismissStoryError?: () => void;
  pendingTest?: PendingTest | null;
  isNarrationLoading?: boolean;
  onPlayNarration: () => void | Promise<void>;
  onStopNarration: () => void;
  onShareSceneImagePrompt: () => void | Promise<void>;
  onShareNpcPortraitPrompt: (npc: Npc) => void | Promise<void>;
  onNavigate?: (tab: 'jogo' | 'combate' | 'ficha' | 'dados' | 'tokens' | 'campanha' | 'configuracoes') => void;
}

const seasonLabel = (value: MasterState['worldClock']['season']) => ({ spring: 'Primavera', summer: 'Verão', autumn: 'Outono', winter: 'Inverno' } as const)[value] || value;

function clockPresentation(hour: number): { label: string; key: string } {
  if (hour < 5) return { label: 'Noite', key: 'night' };
  if (hour < 8) return { label: 'Amanhecer', key: 'dawn' };
  if (hour < 18) return { label: 'Dia', key: 'day' };
  if (hour < 21) return { label: 'Entardecer', key: 'dusk' };
  return { label: 'Noite', key: 'night' };
}

function weatherKey(weather: string): string {
  const text = weather.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  if (/nevoa|neblina|bruma/.test(text)) return 'fog';
  if (/chuva|garoa|tempest/.test(text)) return 'rain';
  return 'clear';
}

function eyeAttention(masterState: MasterState): { current: number; max: number } {
  const threatClock = masterState.threatClocks.find(item => /olho|sauron|inimigo/i.test(item.name));
  if (threatClock) return { current: Math.max(0, threatClock.current), max: Math.max(1, threatClock.max) };
  return { current: Math.max(0, Math.min(12, Math.round((masterState.scene.tension || 0) * 1.2))), max: 12 };
}

const glassPanel: React.CSSProperties = {
  background: 'linear-gradient(180deg, rgba(20,16,11,.95), rgba(9,7,5,.95))',
  border: '1px solid rgba(183,145,91,.48)',
  boxShadow: '0 10px 30px rgba(0,0,0,.48)',
  backdropFilter: 'blur(7px)',
};

const touchButton: React.CSSProperties = {
  touchAction: 'manipulation',
  WebkitTapHighlightColor: 'transparent',
  cursor: 'pointer',
};

export const ImmersiveJourneyView: React.FC<Props> = props => {
  const [assets, setAssets] = useState<ImmersiveAssetManifest | null>(null);
  const [chronicleOpen, setChronicleOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const [npcTokenUrls, setNpcTokenUrls] = useState<Record<string, string>>({});
  const [selectedNpc, setSelectedNpc] = useState<Npc | null>(null);

  useEffect(() => { void loadImmersiveAssetManifest().then(setAssets); }, []);
  useEffect(() => { if (props.currentNode?.id) setChronicleOpen(true); }, [props.currentNode?.id]);

  useEffect(() => {
    let disposed = false;
    const urls: string[] = [];
    void getStoreData<IndividualToken>('individual_tokens').then(tokens => {
      if (disposed) return;
      const next: Record<string, string> = {};
      for (const token of tokens) {
        const url = URL.createObjectURL(token.blob);
        urls.push(url);
        next[token.id] = url;
      }
      setNpcTokenUrls(next);
    }).catch(() => setNpcTokenUrls({}));
    return () => {
      disposed = true;
      urls.forEach(url => URL.revokeObjectURL(url));
    };
  }, [props.presentNpcs.map(npc => npc.id + ':' + (npc.tokenId || '')).join('|')]);

  const node = props.currentNode;
  const weather = node?.weather || 'Céu limpo';
  const clock = clockPresentation(props.masterState.worldClock.hour);
  const eye = eyeAttention(props.masterState);
  const narrative = [node?.atmospheric_narration, node?.action_result].filter(Boolean).join('\n\n');
  const choices = (node?.choices || []).slice(0, 3);
  const locationName = props.masterState.position.locationName || node?.location || 'Eriador';
  const narrativeMeta = (node?.narrativeMeta || {}) as any;
  const generationSource = String(narrativeMeta.generationSource || '');
  const generationModel = String(narrativeMeta.generationModel || '');

  const presetActions = useMemo(() => [
    { id: 'observe', label: 'Observar', text: 'Observar atentamente o ambiente, procurando detalhes, perigos e mudanças ao redor.' },
    { id: 'investigate', label: 'Investigar', text: 'Investigar o local em busca de pistas, rastros, objetos ou sinais relevantes.' },
    { id: 'talk', label: 'Conversar', text: 'Conversar com alguém presente na cena, buscando informação ou reação.' },
    { id: 'explore', label: 'Explorar', text: 'Explorar com cautela os arredores imediatos, sem abandonar a posição da Companhia.' },
  ] as const, []);

  const primaryMenuItems = useMemo(() => [
    ['jogo', 'Mapa', 'menu.map'], ['combate', 'Combate', 'menu.combat'],
    ['ficha', 'Ficha', 'menu.sheet'], ['dados', 'Dados', 'menu.dice'],
  ] as const, []);
  const secondaryMenuItems = useMemo(() => [
    ['tokens', 'Tokens', 'menu.tokens'], ['campanha', 'Campanha', 'menu.campaign'],
    ['configuracoes', 'Configurações', 'menu.settings'],
  ] as const, []);

  const sauronEye = resolveImmersiveAsset(assets, 'sauron.eye');
  const safeTop = 'max(8px, env(safe-area-inset-top, 0px))';
  const safeBottom = 'max(6px, env(safe-area-inset-bottom, 0px))';
  const navHeight = 70;
  const actionPanelBottom = 'calc(' + (navHeight + 8) + 'px + env(safe-area-inset-bottom, 0px))';

  const startAdventureFromMap = async () => {
    if (props.isStoryLoading) return;
    setMoreOpen(false);
    setChronicleOpen(true);
    await props.onStartAdventure();
    // Mantém a intenção de abrir a cena mesmo que a atualização de props chegue no frame seguinte.
    setChronicleOpen(true);
  };

  const dispatchChoice = (choice: StoryChoice) => {
    if (props.isStoryLoading) return;
    if (props.pendingTest) {
      setChronicleOpen(false);
      props.onOpenDice();
      return;
    }
    setChronicleOpen(false);
    props.onChoice(choice);
  };

  const submitCustomAction = (event: React.FormEvent) => {
    if (props.isStoryLoading) {
      event.preventDefault();
      return;
    }
    if (props.pendingTest) {
      event.preventDefault();
      setChronicleOpen(false);
      props.onOpenDice();
      return;
    }
    setChronicleOpen(false);
    props.onCustomActionSubmit(event, false);
  };

  return <section data-braseiro-immersive-root="true" style={{
    position: 'fixed', inset: 0, width: '100%', height: '100dvh', minHeight: '100dvh', overflow: 'hidden',
    background: '#0d0a07', color: '#ead9b7', fontFamily: 'Georgia, Cambria, serif', pointerEvents: 'auto',
  }}>
    <div style={{ position: 'absolute', inset: 0, zIndex: 0, background: '#100d09', pointerEvents: 'auto' }}>
      <EriadorMap
        currentLocation={locationName}
        campaignId={props.campaignId}
        currentHexId={props.masterState.position.currentHexId}
        onPartyMoved={props.onPartyMoved}
        onDiscoveryChanged={props.onMapDiscoveryChanged}
        onPartyRested={props.onPartyRested}
        strongholds={props.masterState.livingWorld.strongholds || []}
        immersive
        partyMarkerMode={node && props.characters.length ? "none" : "footprints"}
        followParty={false}
        partyCharacters={props.characters}
      />
      <div style={{ pointerEvents: 'none', position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(6,5,4,.38) 0%, rgba(8,6,4,.02) 25%, rgba(8,6,4,.02) 72%, rgba(8,6,4,.32) 100%)' }} />
    </div>

    <header style={{ position: 'absolute', left: 10, right: 10, top: safeTop, zIndex: 80, minHeight: 58, display: 'flex', alignItems: 'stretch', gap: 8, pointerEvents: 'none' }}>
      <div style={{ ...glassPanel, flex: 1, minWidth: 0, borderRadius: 12, padding: '7px 10px' }}>
        <div style={{ fontSize: 11, opacity: .72, letterSpacing: '.08em', textTransform: 'uppercase' }}>Local atual</div>
        <div style={{ fontSize: 17, fontWeight: 700, lineHeight: 1.15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{locationName}</div>
        <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, opacity: .82, whiteSpace: 'nowrap', overflow: 'hidden' }}>
          <Hud icon={resolveImmersiveAsset(assets, 'hud.weather.' + weatherKey(weather))} text={weather} />
          <Hud icon={resolveImmersiveAsset(assets, 'hud.time.' + clock.key)} text={clock.label} />
          <span>{seasonLabel(props.masterState.worldClock.season)}</span>
        </div>
      </div>
      <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
          {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain' }} />}
          <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
        </div>
        <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
          <div style={{ height: '100%', width: Math.max(0, Math.min(100, eye.current / eye.max * 100)) + '%', background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
        </div>
      </div>
    </header>

    {props.storyError && <div role="alert" style={{
      position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 96,
      borderRadius: 10, border: '1px solid rgba(185,72,54,.75)', background: 'rgba(52,13,10,.96)',
      color: '#ffe4d6', padding: '10px 12px', pointerEvents: 'auto', boxShadow: '0 8px 24px rgba(0,0,0,.42)'
    }}>
      <div style={{ fontSize: 11, fontWeight: 900, textTransform: 'uppercase', letterSpacing: '.06em' }}>Falha ao iniciar/continuar</div>
      <div style={{ marginTop: 4, fontSize: 12, lineHeight: 1.35 }}>{props.storyError}</div>
      {props.onDismissStoryError && <button type="button" onClick={props.onDismissStoryError} style={{
        ...touchButton, marginTop: 7, minHeight: 34, padding: '5px 10px', borderRadius: 7,
        border: '1px solid rgba(255,225,205,.34)', background: 'rgba(255,255,255,.07)', color: '#ffe4d6',
        fontFamily: 'inherit', fontSize: 11, fontWeight: 800
      }}>Fechar diagnóstico</button>}
    </div>}

    {chronicleOpen && node && <aside data-braseiro-chronicle-sheet="true" style={{
      position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 60, maxHeight: '58dvh',
      borderRadius: 14, padding: '12px 12px 10px', color: '#2a2116',
      background: 'linear-gradient(180deg, rgba(246,225,184,.99), rgba(229,198,143,.99))',
      border: '1px solid rgba(93,63,31,.55)', boxShadow: '0 14px 34px rgba(0,0,0,.42)',
      overflowY: 'auto', overflowX: 'hidden', pointerEvents: 'auto', touchAction: 'pan-y',
      WebkitOverflowScrolling: 'touch',
    }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 5 }}>
        <button type="button" onClick={() => setChronicleOpen(false)} aria-label="Fechar crônica" style={{ ...touchButton, width: 38, height: 38, borderRadius: 999, border: '1px solid #6b4a28', background: '#5b3d20', color: '#fff0cf', fontSize: 24, fontWeight: 900, lineHeight: 1 }}>×</button>
      </div>

      <div style={{ width: 'min(78vw, 300px)', margin: '0 auto 8px', aspectRatio: '16 / 9', overflow: 'hidden', borderRadius: 10, border: '1px solid rgba(93,63,31,.48)', background: 'rgba(31,22,13,.22)' }}>
        {props.currentImage ? <img src={props.currentImage} alt="Ilustração da cena" draggable={false} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#795d38', fontSize: 12, fontWeight: 700 }}>Cena sem ilustração</div>}
      </div>

      <button type="button" onClick={() => void props.onShareSceneImagePrompt()} style={{ ...touchButton, width: '100%', minHeight: 42, borderRadius: 8, border: '1px solid #7b562d', background: 'rgba(91,61,32,.13)', color: '#4b321b', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}>
        {props.currentImage ? 'Trocar ilustração no Gemini' : 'Gerar ilustração 16:9 no Gemini'}
      </button>

      {props.presentNpcs.length > 0 && <div style={{ marginTop: 9 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#735633' }}>NPCs presentes</div>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 3 }}>
          {props.presentNpcs.map(npc => {
            const portrait = npc.tokenId ? npcTokenUrls[npc.tokenId] : null;
            return <button key={npc.id} type="button" onClick={() => portrait ? setSelectedNpc(npc) : void props.onShareNpcPortraitPrompt(npc)} style={{ ...touchButton, width: 68, flex: '0 0 68px', border: 0, padding: 0, background: 'transparent', color: '#3d2a18', fontFamily: 'inherit' }}>
              <span style={{ width: 50, height: 50, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: 999, border: '2px solid #9a713e', background: '#21170e' }}>
                {portrait ? <img src={portrait} alt={npc.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: '#ddb879', fontSize: 22 }}>+</span>}
              </span>
              <span style={{ display: 'block', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 10, fontWeight: 800 }}>{npc.name}</span>
            </button>;
          })}
        </div>
      </div>}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap', marginTop: 10, marginBottom: 5 }}>
        <div style={{ fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
        {(generationSource || generationModel) && <span style={{ marginLeft: 'auto', fontSize: 9, fontWeight: 700, color: generationSource === 'gemini' ? '#365f45' : '#8a3b2f' }}>
          {generationSource === 'gemini' ? 'Gemini' : generationSource === 'local-fallback' ? 'Fallback local' : generationSource}{generationModel ? ' · ' + generationModel : ''}
        </span>
        <button type="button" onClick={() => props.isNarrationLoading ? props.onStopNarration() : void props.onPlayNarration()} style={{ ...touchButton, minHeight: 36, padding: '6px 11px', borderRadius: 999, border: '1px solid #7b562d', background: props.isNarrationLoading ? '#5e2b22' : '#5b3d20', color: '#fff0cf', fontFamily: 'inherit', fontSize: 11, fontWeight: 900, whiteSpace: 'nowrap' }}>
          {props.isNarrationLoading ? '■ Parar narração' : '🔊 Ouvir narração'}
        </button>
      </div>

      <div style={{ minHeight: 92, maxHeight: 160, overflowY: 'auto', paddingRight: 4, fontSize: 15, lineHeight: 1.48 }}>
        {narrative ? narrative.split(/\n\s*\n/).map((paragraph, index) => <p key={index} style={{ margin: index ? '10px 0 0' : 0 }}>{paragraph}</p>) : <p style={{ margin: 0, fontStyle: 'italic', opacity: .72 }}>A crônica aguarda o próximo passo da Companhia.</p>}
      </div>

      {props.pendingTest && <button type="button" onClick={() => { setChronicleOpen(false); props.onOpenDice(); }} style={{ ...touchButton, width: '100%', minHeight: 44, marginTop: 9, borderRadius: 8, border: '1px solid #7b562d', background: '#6a431f', color: '#fff0cf', fontFamily: 'inherit', fontSize: 12, fontWeight: 900 }}>Teste pendente — abrir Dados</button>}

      <div style={{ marginTop: 9 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações rápidas</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6 }}>
          {presetActions.map(action => <button key={action.id} type="button" disabled={Boolean(props.isStoryLoading)} onClick={() => dispatchChoice({ id: 'preset-' + action.id, text: action.text })} style={{ ...touchButton, minHeight: 44, borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.34)', color: '#2d2116', fontFamily: 'inherit', fontSize: 12, fontWeight: 800, opacity: props.isStoryLoading ? .5 : 1 }}>{action.label}</button>)}
        </div>
      </div>

      {choices.length > 0 && <div style={{ marginTop: 9 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Sugestões do Mestre</div>
        <div style={{ display: 'grid', gridTemplateColumns: choices.length === 1 ? '1fr' : 'repeat(2, minmax(0, 1fr))', gap: 6 }}>
          {choices.map((choice, index) => <button key={choice.id} type="button" disabled={Boolean(props.isStoryLoading)} onClick={() => dispatchChoice(choice)} style={{ ...touchButton, minHeight: 40, gridColumn: choices.length === 3 && index === 2 ? '1 / -1' : undefined, borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(92,58,25,.10)', padding: '5px 7px', color: '#2d2116', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, opacity: props.isStoryLoading ? .5 : 1 }}>{choice.text}</button>)}
        </div>
      </div>}

      <form onSubmit={submitCustomAction} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginTop: 10 }}>
        <label style={{ flex: 1, minWidth: 0 }}>
          <span style={{ display: 'block', marginBottom: 4, fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '.06em', opacity: .72 }}>Sua ação</span>
          <textarea value={props.customAction} onChange={event => props.setCustomAction(event.target.value)} rows={2} placeholder="O que a Companhia faz?" style={{ width: '100%', minHeight: 48, maxHeight: 84, resize: 'none', boxSizing: 'border-box', borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.58)', padding: '8px 9px', color: '#2d2116', fontFamily: 'inherit', fontSize: 14, outline: 'none' }} />
        </label>
        <button type="submit" disabled={Boolean(props.isStoryLoading)} style={{ ...touchButton, minWidth: 78, minHeight: 48, borderRadius: 8, border: '1px solid #5f4225', background: '#5b3d20', color: '#f6e7c6', fontFamily: 'inherit', fontWeight: 800, fontSize: 13, textTransform: 'uppercase', opacity: props.isStoryLoading ? .5 : 1 }}>Enviar</button>
      </form>

      <button type="button" onClick={() => setChronicleOpen(false)} style={{ ...touchButton, width: '100%', minHeight: 40, marginTop: 8, borderRadius: 8, border: '1px solid rgba(83,53,25,.35)', background: 'rgba(255,248,231,.28)', color: '#5b3d20', fontFamily: 'inherit', fontSize: 11, fontWeight: 900 }}>Voltar ao mapa</button>
    </aside>}

    {!chronicleOpen && <div style={{ position: 'absolute', left: '50%', bottom: actionPanelBottom, zIndex: 72, transform: 'translateX(-50%)', width: 'min(88vw, 360px)', pointerEvents: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 7 }}>
      {!node && !props.canStartAdventure && <div style={{ ...glassPanel, width: '100%', boxSizing: 'border-box', borderRadius: 10, padding: '8px 12px', textAlign: 'center', fontSize: 12, color: '#ead9b7' }}>Selecione um hex no mapa e toque <strong>Posicionar</strong> para definir onde a Companhia começa.</div>}
      <button type="button" disabled={!node && (!props.canStartAdventure || props.isStoryLoading)} onClick={() => {
        setMoreOpen(false);
        if (node) { setChronicleOpen(true); return; }
        if (props.canStartAdventure && !props.isStoryLoading) void startAdventureFromMap();
      }} style={{ ...touchButton, minWidth: 180, minHeight: 48, borderRadius: 10, border: '1px solid #9a6b34', background: 'linear-gradient(180deg,#6f4a24,#4c3018)', color: '#fff0cf', boxShadow: '0 8px 18px rgba(0,0,0,.42)', fontFamily: 'inherit', fontSize: 13, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '.06em', opacity: !node && (!props.canStartAdventure || props.isStoryLoading) ? .55 : 1 }}>{props.isStoryLoading ? 'Gemini criando a cena…' : node ? 'Abrir crônica' : 'Começar aventura'}</button>
    </div>}

    {moreOpen && <div style={{ ...glassPanel, position: 'absolute', left: 10, right: 10, bottom: actionPanelBottom, zIndex: 95, borderRadius: 12, padding: 8, pointerEvents: 'auto' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 7 }}>
        {secondaryMenuItems.map(([tab, label, key]) => {
          const icon = resolveImmersiveAsset(assets, key);
          return <button key={tab} type="button" onClick={() => { setMoreOpen(false); props.onNavigate?.(tab); }} style={{ ...touchButton, minHeight: 54, borderRadius: 9, border: '1px solid rgba(183,145,91,.32)', background: 'rgba(255,255,255,.04)', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700 }}>
            {icon && <img src={icon} alt="" style={{ width: 22, height: 22, objectFit: 'contain', display: 'block', margin: '0 auto 3px' }} />}
            {label}
          </button>;
        })}
      </div>
    </div>}

    {selectedNpc && selectedNpc.tokenId && npcTokenUrls[selectedNpc.tokenId] && <div onClick={() => setSelectedNpc(null)} style={{ position: 'absolute', inset: 0, zIndex: 130, background: 'rgba(0,0,0,.82)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18, pointerEvents: 'auto' }}>
      <div onClick={event => event.stopPropagation()} style={{ width: 'min(92vw,390px)', borderRadius: 14, border: '1px solid rgba(183,145,91,.62)', background: '#100c08', padding: 14 }}>
        <img src={npcTokenUrls[selectedNpc.tokenId]} alt={selectedNpc.name} style={{ width: '100%', maxHeight: '64dvh', objectFit: 'contain', borderRadius: 9 }} />
        <strong style={{ display: 'block', marginTop: 8, textAlign: 'center' }}>{selectedNpc.name}</strong>
        <button type="button" onClick={() => void props.onShareNpcPortraitPrompt(selectedNpc)} style={{ ...touchButton, width: '100%', minHeight: 42, marginTop: 8, borderRadius: 8, border: '1px solid #936638', background: '#5b3d20', color: '#f6e7c6', fontFamily: 'inherit', fontWeight: 800 }}>Regenerar retrato no Gemini</button>
      </div>
    </div>}

    <nav style={{ ...glassPanel, position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 100, minHeight: navHeight, padding: '6px 7px ' + safeBottom, borderLeft: 0, borderRight: 0, borderBottom: 0, borderRadius: 0, pointerEvents: 'auto', touchAction: 'manipulation' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, minmax(0, 1fr))', gap: 2, minHeight: 58 }}>
        {primaryMenuItems.map(([tab, label, key]) => {
          const icon = resolveImmersiveAsset(assets, key);
          return <button key={tab} type="button" onClick={() => {
            setMoreOpen(false);
            if (tab === 'jogo') { setChronicleOpen(false); return; }
            if (tab === 'dados') { props.onOpenDice(); return; }
            props.onNavigate?.(tab);
          }} style={{ ...touchButton, minWidth: 0, minHeight: 50, border: 0, background: 'transparent', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3 }}>
            {icon && <img src={icon} alt="" style={{ width: 23, height: 23, objectFit: 'contain' }} />}
            <span style={{ maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
          </button>;
        })}
        <button type="button" aria-expanded={moreOpen} aria-label="Mais opções" onClick={() => setMoreOpen(value => !value)} style={{ ...touchButton, minWidth: 0, minHeight: 50, border: 0, background: 'transparent', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
          <span style={{ fontSize: 20, lineHeight: 1 }}>•••</span><span>Mais</span>
        </button>
      </div>
    </nav>
  </section>;
};

const Hud: React.FC<{ icon: string | null; text: string }> = ({ icon, text }) => <span style={{ display: 'inline-flex', minWidth: 0, alignItems: 'center', gap: 4, overflow: 'hidden' }}>
  {icon && <img src={icon} alt="" style={{ width: 16, height: 16, objectFit: 'contain', flex: '0 0 auto' }} />}
  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{text}</span>
</span>;
