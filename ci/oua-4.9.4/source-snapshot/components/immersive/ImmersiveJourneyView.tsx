import React, { useEffect, useMemo, useState } from 'react';
import type { Character, SceneNode } from '../../lib/db';
import type { MasterState } from '../../lib/master/types';
import type { StoryChoice } from '../../types';
import type { MapDiscovery } from '../../lib/map';
import type { PartyMapMove } from '../EriadorMap';
import { EriadorMap } from '../EriadorMap';
import { PartyCluster } from './PartyCluster';
import { loadImmersiveAssetManifest, resolveImmersiveAsset, type ImmersiveAssetManifest } from '../../lib/immersiveAssets';

interface Props {
  campaignId: string;
  currentNode: SceneNode | null;
  currentImage?: string | null;
  characters: Character[];
  masterState: MasterState;
  customAction: string;
  setCustomAction: (value: string) => void;
  onChoice: (choice: StoryChoice) => void;
  onCustomActionSubmit: (event: React.FormEvent, isDmMode: boolean) => void;
  onPartyMoved: (move: PartyMapMove) => void | Promise<void>;
  onMapDiscoveryChanged: (discovery: MapDiscovery, reason: string) => void | Promise<void>;
  onPartyRested?: () => void | Promise<void>;
  onOpenDice: () => void;
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
  background: 'linear-gradient(180deg, rgba(20,16,11,.94), rgba(9,7,5,.94))',
  border: '1px solid rgba(183,145,91,.48)',
  boxShadow: '0 10px 30px rgba(0,0,0,.48)',
  backdropFilter: 'blur(7px)',
};

export const ImmersiveJourneyView: React.FC<Props> = props => {
  const [assets, setAssets] = useState<ImmersiveAssetManifest | null>(null);
  const [atlasActive, setAtlasActive] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  useEffect(() => { void loadImmersiveAssetManifest().then(setAssets); }, []);

  const node = props.currentNode;
  const weather = node?.weather || 'Céu limpo';
  const clock = clockPresentation(props.masterState.worldClock.hour);
  const eye = eyeAttention(props.masterState);
  const narrative = [node?.atmospheric_narration, node?.action_result].filter(Boolean).join('\n\n');
  const choices = (node?.choices || []).slice(0, 3);
  const locationName = props.masterState.position.locationName || node?.location || 'Eriador';

  const primaryMenuItems = useMemo(() => [
    ['jogo', atlasActive ? 'Cena' : 'Mapa', 'menu.map'], ['combate', 'Combate', 'menu.combat'],
    ['ficha', 'Ficha', 'menu.sheet'], ['dados', 'Dados', 'menu.dice'],
  ] as const, [atlasActive]);
  const secondaryMenuItems = useMemo(() => [
    ['tokens', 'Tokens', 'menu.tokens'], ['campanha', 'Campanha', 'menu.campaign'],
    ['configuracoes', 'Configurações', 'menu.settings'],
  ] as const, []);

  const sauronEye = resolveImmersiveAsset(assets, 'sauron.eye');

  const safeTop = 'max(8px, env(safe-area-inset-top, 0px))';
  const safeBottom = 'max(6px, env(safe-area-inset-bottom, 0px))';
  const navHeight = 70;
  const actionPanelBottom = `calc(${navHeight + 8}px + env(safe-area-inset-bottom, 0px))`;

  return <section
    data-braseiro-immersive-root="true"
    style={{
      position: 'fixed', inset: 0, width: '100%', height: '100dvh', minHeight: '100dvh', overflow: 'hidden',
      background: '#0d0a07', color: '#ead9b7', fontFamily: 'Georgia, Cambria, serif',
    }}
  >
    <div style={{ position: 'absolute', inset: 0, background: '#100d09', zIndex: 0 }}>
      {atlasActive
        ? <EriadorMap currentLocation={locationName} campaignId={props.campaignId} currentHexId={props.masterState.position.currentHexId} onPartyMoved={props.onPartyMoved} onDiscoveryChanged={props.onMapDiscoveryChanged} onPartyRested={props.onPartyRested} strongholds={props.masterState.livingWorld.strongholds || []} immersive partyMarkerMode="none" followParty />
        : <img src={props.currentImage || './terra-media-map/map_preview_boot_1536.webp'} alt="" draggable={false} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />}
      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(6,5,4,.50) 0%, rgba(8,6,4,.08) 31%, rgba(8,6,4,.18) 56%, rgba(8,6,4,.72) 100%)' }} />
    </div>

    {!atlasActive && <PartyCluster characters={props.characters} manifest={assets} />}

    <header style={{
      position: 'absolute', left: 10, right: 10, top: safeTop, zIndex: 80, minHeight: 58,
      display: 'flex', alignItems: 'stretch', gap: 8, pointerEvents: 'none',
    }}>
      <div style={{ ...glassPanel, flex: 1, minWidth: 0, borderRadius: 12, padding: '7px 10px' }}>
        <div style={{ fontSize: 11, opacity: .72, letterSpacing: '.08em', textTransform: 'uppercase' }}>Local atual</div>
        <div style={{ fontSize: 17, fontWeight: 700, lineHeight: 1.15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{locationName}</div>
        <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, opacity: .82, whiteSpace: 'nowrap', overflow: 'hidden' }}>
          <Hud icon={resolveImmersiveAsset(assets, `hud.weather.${weatherKey(weather)}`)} text={weather} />
          <Hud icon={resolveImmersiveAsset(assets, `hud.time.${clock.key}`)} text={clock.label} />
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{seasonLabel(props.masterState.worldClock.season)}</span>
        </div>
      </div>

      <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
          {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain', flex: '0 0 auto' }} />}
          <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
        </div>
        <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
          <div style={{ height: '100%', width: `${Math.max(0, Math.min(100, eye.current / eye.max * 100))}%`, background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
        </div>
      </div>
    </header>

    <div style={{
      position: 'absolute', left: 10, right: 10, bottom: actionPanelBottom, zIndex: 70,
      maxHeight: choices.length ? '43dvh' : '34dvh', minHeight: choices.length ? 250 : 190,
      borderRadius: 14, padding: '14px 14px 12px', color: '#2a2116',
      background: 'linear-gradient(180deg, rgba(246,225,184,.98), rgba(229,198,143,.98))',
      border: '1px solid rgba(93,63,31,.55)', boxShadow: '0 14px 34px rgba(0,0,0,.42)',
      display: 'flex', flexDirection: 'column', overflow: 'hidden',
    }}>
      <div style={{ flex: '1 1 auto', minHeight: 0, overflowY: 'auto', paddingRight: 3, fontSize: 15, lineHeight: 1.48 }}>
        {narrative
          ? narrative.split(/\n\s*\n/).map((paragraph, index) => <p key={index} style={{ margin: index ? '10px 0 0' : 0 }}>{paragraph}</p>)
          : <p style={{ margin: 0, fontStyle: 'italic', opacity: .72 }}>A crônica aguarda o próximo passo da Companhia.</p>}
      </div>

      {choices.length > 0 && <div style={{ display: 'grid', gridTemplateColumns: choices.length === 1 ? '1fr' : 'repeat(2, minmax(0, 1fr))', gap: 7, marginTop: 10 }}>
        {choices.map((choice, index) => <button
          key={choice.id}
          type="button"
          onClick={() => props.onChoice(choice)}
          style={{
            minHeight: 44, gridColumn: choices.length === 3 && index === 2 ? '1 / -1' : undefined,
            borderRadius: 9, border: '1px solid rgba(83,53,25,.45)', background: 'rgba(92,58,25,.10)',
            padding: '7px 9px', color: '#2d2116', fontFamily: 'inherit', fontSize: 13, fontWeight: 700, lineHeight: 1.2,
          }}
        >{choice.text}</button>)}
      </div>}

      <form onSubmit={event => props.onCustomActionSubmit(event, false)} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginTop: 10 }}>
        <label style={{ flex: 1, minWidth: 0 }}>
          <span style={{ display: 'block', marginBottom: 4, fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '.06em', opacity: .72 }}>Sua ação</span>
          <textarea
            value={props.customAction}
            onChange={event => props.setCustomAction(event.target.value)}
            rows={2}
            placeholder="O que a Companhia faz?"
            style={{ width: '100%', minHeight: 48, maxHeight: 84, resize: 'none', boxSizing: 'border-box', borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.58)', padding: '8px 9px', color: '#2d2116', fontFamily: 'inherit', fontSize: 14, outline: 'none' }}
          />
        </label>
        <button type="submit" style={{ minWidth: 78, minHeight: 48, borderRadius: 8, border: '1px solid #5f4225', background: '#5b3d20', color: '#f6e7c6', fontFamily: 'inherit', fontWeight: 800, fontSize: 13, textTransform: 'uppercase' }}>Enviar</button>
      </form>
    </div>

    {moreOpen && <div style={{ ...glassPanel, position: 'absolute', left: 10, right: 10, bottom: actionPanelBottom, zIndex: 95, borderRadius: 12, padding: 8 }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 7 }}>
        {secondaryMenuItems.map(([tab, label, key]) => {
          const icon = resolveImmersiveAsset(assets, key);
          return <button key={tab} type="button" onClick={() => { setMoreOpen(false); props.onNavigate?.(tab); }} style={{ minHeight: 54, borderRadius: 9, border: '1px solid rgba(183,145,91,.32)', background: 'rgba(255,255,255,.04)', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700 }}>
            {icon && <img src={icon} alt="" style={{ width: 22, height: 22, objectFit: 'contain', display: 'block', margin: '0 auto 3px' }} />}
            {label}
          </button>;
        })}
      </div>
    </div>}

    <nav style={{
      ...glassPanel, position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 100,
      minHeight: navHeight, padding: `6px 7px ${safeBottom}`, borderLeft: 0, borderRight: 0, borderBottom: 0,
      borderRadius: 0,
    }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, minmax(0, 1fr))', gap: 2, minHeight: 58 }}>
        {primaryMenuItems.map(([tab, label, key]) => {
          const icon = resolveImmersiveAsset(assets, key);
          return <button key={tab} type="button" onClick={() => {
            setMoreOpen(false);
            if (tab === 'jogo') { setAtlasActive(value => !value); return; }
            if (tab === 'dados') { props.onOpenDice(); return; }
            props.onNavigate?.(tab);
          }} style={{ minWidth: 0, minHeight: 50, border: 0, background: 'transparent', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3 }}>
            {icon && <img src={icon} alt="" style={{ width: 23, height: 23, objectFit: 'contain' }} />}
            <span style={{ maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
          </button>;
        })}
        <button type="button" aria-expanded={moreOpen} aria-label="Mais opções" onClick={() => setMoreOpen(value => !value)} style={{ minWidth: 0, minHeight: 50, border: 0, background: 'transparent', color: '#ead9b7', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
          <span style={{ fontSize: 20, lineHeight: 1 }}>•••</span><span>Mais</span>
        </button>
      </div>
    </nav>
  </section>;
};

const Hud: React.FC<{ icon: string | null; text: string }> = ({ icon, text }) => <span style={{ display: 'inline-flex', minWidth: 0, alignItems: 'center', gap: 4, overflow: 'hidden' }}>{icon && <img src={icon} alt="" style={{ width: 16, height: 16, objectFit: 'contain', flex: '0 0 auto' }} />}<span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{text}</span></span>;
