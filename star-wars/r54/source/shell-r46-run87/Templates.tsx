'use client';


import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import type { QaState, ScreenDefinition } from '@/src/config/screens';
import { qaStateLabels, screensForDomain } from '@/src/config/screens';
import { ReferenceIcon } from './ReferenceIcon';
import type { DisplayField, DisplayItem, R21ReadModel } from '@/src/state/r21Projection';
import type { BridgeCatalogWeapon, BridgeCreationCatalog, BridgeCreationOptions, BridgeCreationPreview, BridgeDataState, BridgeSnapshotItem, BridgeImageAsset, BridgeImageLibraryInfo } from '@/src/state/gameBridgeClient';
import type { BridgeAvailability, BridgeSubmitState } from '@/src/state/gameBridgeClient';
import { libraryAuthorityLabel, libraryStatusLabel, type LibraryAuthorityType, type LibraryBridgeAvailability, type LibraryDocument, type LibrarySearchHit } from '@/src/state/libraryBridgeClient';
import type { GeminiBridgeAvailability, GeminiScenePrompt, GeminiStatus } from '@/src/state/geminiBridgeClient';
import type { ImageCacheStatus, ImageLibraryNativeAvailability } from '@/src/state/imageLibraryBridgeClient';


type TemplateProps = {
  screen: ScreenDefinition;
  qaState: QaState;
  navigate: (route: string) => void;
  sceneDraft: string;
  setSceneDraft: (value: string) => void;
  openMenu: () => void;
  campaignRead: R21ReadModel;
  masterBridge: BridgeAvailability;
  intentSubmitState: BridgeSubmitState;
  intentSubmitMessage: string;
  submitSceneIntent: () => Promise<void>;
  dataCapabilities: { characterJson: boolean; campaignBackup: boolean; snapshots: boolean; randomCharacter: boolean };
  dataActionState: BridgeDataState;
  dataActionMessage: string;
  snapshots: BridgeSnapshotItem[];
  exportCharacterJson: () => void;
  importCharacterJsonFile: (file: File) => Promise<void>;
  exportCampaignBackup: () => void;
  importCampaignBackupFile: (file: File) => Promise<void>;
  refreshSnapshots: () => boolean;
  createManualSnapshot: () => void;
  restoreSnapshot: (id: string) => void;
  generateRandomCharacter: (name: string, motivation: string) => void;
  creationCapabilities: { catalog: boolean; preview: boolean; create: boolean };
  creationCatalog: BridgeCreationCatalog | null;
  creationDraft: BridgeCreationOptions;
  setCreationDraft: (value: BridgeCreationOptions | ((current: BridgeCreationOptions) => BridgeCreationOptions)) => void;
  creationPreview: BridgeCreationPreview | null;
  creationPreviewError: string;
  creationActionState: BridgeDataState;
  creationActionMessage: string;
  refreshCreationCatalog: () => boolean;
  createManualCharacter: () => boolean;
  equipmentCapabilities: { catalog: boolean; proposeWeapon: boolean };
  equipmentWeapons: BridgeCatalogWeapon[];
  equipmentActionState: BridgeDataState;
  equipmentActionMessage: string;
  refreshEquipmentCatalog: () => boolean;
  proposeWeaponAcquisition: (itemId: string) => void;
  imageLibraryCapabilities: { info:boolean; search:boolean; maps:boolean; visuals:boolean; asset:boolean; runtime:boolean };
  imageLibraryInfo: BridgeImageLibraryInfo | null;
  imageMaps: BridgeImageAsset[];
  imageMapQuery: string;
  imageVisualEntities: BridgeImageAsset[];
  imageVisualQuery: string;
  setImageVisualQuery: (value: string) => void;
  imageVisualKind: 'all'|'npc'|'droid'|'beast'|'vehicle';
  setImageVisualKind: (value: 'all'|'npc'|'droid'|'beast'|'vehicle') => void;
  setImageMapQuery: (value: string) => void;
  selectedImageAsset: BridgeImageAsset | null;
  selectedImageUrl: string | null;
  selectedImageCache: ImageCacheStatus | null;
  imageCacheStatuses: Record<string, ImageCacheStatus>;
  imageLibraryNative: ImageLibraryNativeAvailability;
  imageActionState: BridgeDataState;
  imageActionMessage: string;
  refreshImageLibrary: () => boolean;
  selectImageMap: (assetId: string) => void;
  cacheSelectedImageMap: () => boolean;
  clearSelectedImageMapCache: () => boolean;
  cacheVisualEntity: (assetId: string) => boolean;
  clearVisualEntityCache: (assetId: string) => boolean;
  libraryBridge: LibraryBridgeAvailability;
  libraryCapabilities: { list: boolean; search: boolean; importPdf: boolean; policy: boolean; reindex: boolean; remove: boolean };
  libraryDocuments: LibraryDocument[];
  libraryActionState: BridgeDataState;
  libraryActionMessage: string;
  librarySearchResults: LibrarySearchHit[];
  librarySearchGrounded: boolean;
  refreshLibraryDocuments: () => boolean;
  importPdfToLibrary: () => void;
  searchLibrary: (query: string) => void;
  updateLibraryPolicy: (docId: string, authorityType: LibraryAuthorityType, enabled: boolean) => void;
  reindexLibraryDocument: (docId: string) => void;
  removeLibraryDocument: (docId: string) => void;
  geminiBridge: GeminiBridgeAvailability;
  geminiCapabilities: { status: boolean; configure: boolean; clearKey: boolean; generate: boolean; importImage: boolean; clearImage: boolean };
  geminiStatus: GeminiStatus | null;
  geminiActionState: BridgeDataState;
  geminiActionMessage: string;
  sceneImageUrl: string | null;
  sceneImagePrompt: GeminiScenePrompt | null;
  configureGeminiApiKey: (apiKey: string) => boolean;
  clearGeminiApiKey: () => boolean;
  generateSceneImage: () => boolean;
  importSceneImage: () => boolean;
  clearSceneImage: () => boolean;
};


const C = {
  spaceBlack: '#111315',
  lead: '#2B3034',
  durasteel: '#D5D8DC',
  oldGold: '#D49D42',
  rust: '#652514',
  paper: '#D8CDB8',
  paperInk: '#26211B',
  cyan: '#64A9BA',
  red: '#9D4132',
};


const page: CSSProperties = {
  display: 'grid',
  gap: 12,
  padding: '12px 12px 96px',
  maxWidth: 560,
  margin: '0 auto',
  color: C.durasteel,
};


const panel: CSSProperties = {
  border: `1px solid ${C.oldGold}66`,
  background: 'rgba(17,19,21,.90)',
  padding: 14,
  borderRadius: 8,
  boxShadow: '0 8px 24px rgba(0,0,0,.22)',
};


const softPanel: CSSProperties = {
  ...panel,
  borderColor: '#596269',
  background: 'rgba(43,48,52,.76)',
};


const narrative: CSSProperties = {
  ...panel,
  background: C.paper,
  color: C.paperInk,
  borderColor: '#8B7450',
  padding: 16,
};


const label: CSSProperties = {
  textTransform: 'uppercase',
  letterSpacing: '.08em',
  fontSize: 13,
  fontWeight: 800,
  color: C.oldGold,
};


const button: CSSProperties = {
  minHeight: 46,
  border: `1px solid ${C.oldGold}99`,
  background: 'rgba(43,48,52,.96)',
  color: C.durasteel,
  borderRadius: 7,
  padding: '10px 12px',
  fontWeight: 800,
  letterSpacing: '.025em',
};


const mutedButton: CSSProperties = {
  ...button,
  borderColor: '#596269',
  color: '#A9B0B5',
};

function Header({ screen, qaState }: { screen: ScreenDefinition; qaState: QaState }) {
  return (
    <>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        <div className="screen-domain-icon" style={{ width: 42, height: 42, display: 'grid', placeItems: 'center', border: `1px solid ${C.oldGold}66`, borderRadius: 8 }}>
          <ReferenceIcon name={screen.icon} />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={label}>{screen.domain}</div>
          <h1 style={{ margin: '2px 0 0', fontSize: 24, lineHeight: 1.05 }}>{screen.title.replace(`${screen.domain} — `, '')}</h1>
        </div>
        {qaState !== 'default' && (
          <span style={{ ...label, border: `1px solid ${C.oldGold}`, padding: '6px 8px', borderRadius: 999 }}>{(qaStateLabels[qaState] ?? qaState).toUpperCase()}</span>
        )}
      </div>
      <p style={{ margin: 0, color: '#B8BEC2', fontSize: 16, lineHeight: 1.45 }}>{screen.description}</p>
    </>
  );
}


function SiblingNav({ screen, navigate }: Pick<TemplateProps, 'screen' | 'navigate'>) {
  const siblings = screensForDomain(screen.domain);
  const activeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  }, [screen.route]);

  if (siblings.length < 2) return null;
  return (
    <nav aria-label={`Estados de ${screen.domain}`} style={{ display: 'flex', gap: 7, overflowX: 'auto', paddingBottom: 2 }}>
      {siblings.map((item) => (
        <button
          key={item.route}
          ref={item.route === screen.route ? activeRef : undefined}
          type="button"
          onClick={() => navigate(item.route)}
          style={{
            ...button,
            flex: '0 0 auto',
            minHeight: 44,
            padding: '7px 10px',
            background: item.route === screen.route ? C.rust : 'rgba(43,48,52,.9)',
          }}
          aria-current={item.route === screen.route ? 'page' : undefined}
        >
          {item.title.replace(`${item.domain} — `, '')}
        </button>
      ))}
    </nav>
  );
}


function Card({ title, children, tone = 'dark' }: { title: string; children: ReactNode; tone?: 'dark' | 'soft' | 'paper' | 'warning' }) {
  const style = tone === 'paper' ? narrative : tone === 'soft' ? softPanel : tone === 'warning' ? { ...panel, borderColor: C.red, background: '#251817' } : panel;
  return (
    <section style={{ ...style, minWidth: 0 }}>
      <div style={{ ...label, overflowWrap: 'anywhere', wordBreak: 'break-word', color: tone === 'paper' ? '#684B25' : tone === 'warning' ? '#E5A28F' : C.oldGold }}>{title}</div>
      <div style={{ marginTop: 8, minWidth: 0, overflowWrap: 'anywhere', fontSize: tone === 'paper' ? 18 : 16, lineHeight: tone === 'paper' ? 1.55 : 1.5 }}>{children}</div>
    </section>
  );
}


function DataField({ field }: { field: DisplayField }) {
  return (
    <div style={{ ...softPanel, padding: 10, minHeight: 66 }}>
      <div style={label}>{field.label}</div>
      <div style={{ marginTop: 8, fontSize: 18, overflowWrap: 'anywhere' }}>{field.value || '—'}</div>
    </div>
  );
}


function DataGrid({ fields }: { fields: Array<string | DisplayField> }) {
  const normalized = fields.map((field) => typeof field === 'string' ? { label: field, value: '—' } : field);
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,minmax(0,1fr))', gap: 8 }}>
      {normalized.map((field, index) => <DataField key={`${field.label}-${index}`} field={field} />)}
    </div>
  );
}


function DisplayList({ items, emptyText }: { items: DisplayItem[]; emptyText: string }) {
  if (!items.length) return <p style={{ margin: 0, color: '#A9B0B5' }}>{emptyText}</p>;
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      {items.map((item, index) => (
        <div key={`${item.title}-${index}`} style={{ ...softPanel, padding: 10 }}>
          <strong>{item.title}</strong>
          {item.meta && <div style={{ marginTop: 4, color: '#A9B0B5', fontSize: 14 }}>{item.meta}</div>}
        </div>
      ))}
    </div>
  );
}

function meaningfulFields(fields: DisplayField[] | null | undefined): DisplayField[] {
  return (fields ?? []).filter((field) => field.value.trim() !== '' && field.value !== '—');
}


function CampaignReadNotice({ model }: { model: R21ReadModel }) {
  if (model.status === 'ready') return null;
  if (model.status === 'missing') return <Card title="Campanha" tone="soft">Nenhuma campanha local carregada.</Card>;
  return <Card title="Estado da campanha" tone="warning">{model.detail || 'Não foi possível ler o estado local da campanha.'}</Card>;
}



function SceneContextHeader({ screen, qaState, openMenu }: Pick<TemplateProps, 'screen' | 'qaState' | 'openMenu'>) {
  const stateTitle = screen.title.replace('Cena — ', '');
  return (
    <header className="scene-context-header">
      <button type="button" className="scene-context-menu" aria-label="Abrir utilidades" onClick={openMenu}>
        <span />
        <span />
        <span />
      </button>
      <div className="scene-context-title" aria-label={`Cena — ${stateTitle}`}>
        <strong>{stateTitle}</strong>
      </div>
      <span className="scene-context-spacer" aria-hidden="true" />
    </header>
  );
}


function SceneStatusGlyph({ kind }: { kind: 'hull' | 'strain' | 'speed' | 'defense' }) {
  const paths = {
    hull: <><path d="M12 3c3 3 5 7 5 11l3 3-4 1-1 3-3-4-3 4-1-3-4-1 3-3c0-4 2-8 5-11z" /><path d="M9 13h6" /></>,
    strain: <path d="M2 13h4l2-6 3 12 3-9 2 3h6" />,
    speed: <><path d="M4 16a8 8 0 0 1 16 0" /><path d="m12 15 4-6" /><path d="M7 15h.01M17 15h.01" /></>,
    defense: <><path d="m12 3 7 3v5c0 5-3.5 8-7 10-3.5-2-7-5-7-10V6z" /><path d="M12 7v9" /></>,
  } as const;
  return <svg className="scene-status-glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[kind]}</svg>;
}


function SceneVehicleStatusStrip({ qaState }: Pick<TemplateProps, 'qaState'>) {
  const cells = [
    { label: 'Casco', kind: 'hull' as const, tone: 'cyan' },
    { label: 'Tensão', kind: 'strain' as const, tone: 'amber' },
    { label: 'Velocidade', kind: 'speed' as const, tone: 'cyan' },
    { label: 'Defesa', kind: 'defense' as const, tone: 'neutral' },
  ];
  return (
    <section className={`scene-status-strip ${qaState === 'demo' ? 'is-demo' : ''}`} aria-label="Estado imediato da nave">
      {cells.map((item) => (
        <div key={item.label} className={`scene-status-cell tone-${item.tone}`}>
          <SceneStatusGlyph kind={item.kind} />
          <div className="scene-status-copy">
            <span>{item.label}</span>
            <span aria-label={`${item.label}: valor não fornecido`} style={{ color: '#82939A', fontSize: '.72rem', lineHeight: 1 }}>—</span>
          </div>
        </div>
      ))}
    </section>
  );
}


function SceneMediaSlot({ qaState, sceneImageUrl }: Pick<TemplateProps, 'qaState' | 'sceneImageUrl'>) {
  const qaVisual = qaState === 'demo';
  const hasImage = Boolean(sceneImageUrl);
  return (
    <figure className={`scene-media-slot ${hasImage ? 'has-scene-image' : 'is-empty'}`}>
      {hasImage
        ? <img className="scene-media-image" src={sceneImageUrl ?? undefined} alt="Imagem cinematográfica da cena atual" />
        : <div className="scene-media-surface" aria-hidden="true" />}
      {!hasImage && <figcaption className={qaVisual ? 'is-demo-caption' : undefined}>
        <span>{qaVisual ? 'QA VISUAL — SEM ASSET DE CENA' : 'IMAGEM DE CENA NÃO FORNECIDA'}</span>
        <small>{qaVisual ? 'SEM CONTEÚDO FICTÍCIO' : 'AGUARDANDO IMAGEM'}</small>
      </figcaption>}
    </figure>
  );
}


function SceneSectionGlyph({ icon }: { icon: 'speaker' | 'target' | 'group' | 'chat' }) {
  const paths = {
    speaker: <><path d="M4 10v4h4l5 4V6L8 10z" /><path d="M16.5 9c1.7 1.8 1.7 4.2 0 6M19 6.5c3.4 3.2 3.4 7.8 0 11" /></>,
    target: <><circle cx="12" cy="12" r="7" /><circle cx="12" cy="12" r="2" /><path d="M12 2v4M12 18v4M2 12h4M18 12h4" /></>,
    group: <><circle cx="9" cy="9" r="3" /><circle cx="17" cy="10" r="2" /><path d="M3 20c.7-4 2.8-6 6-6s5.3 2 6 6M15 15c3.2 0 5 1.7 5.5 5" /></>,
    chat: <><path d="M4 5h16v11H9l-5 4z" /><path d="M8 9h.01M12 9h.01M16 9h.01" /></>,
  } as const;
  return <svg className="scene-heading-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[icon]}</svg>;
}

function SceneSectionHeading({ title, tone = 'cyan', icon }: { title: string; tone?: 'cyan' | 'rust'; icon: 'speaker' | 'target' | 'group' | 'chat' }) {
  return (
    <div className={`scene-section-heading ${tone}`}>
      <span className="scene-section-icon" aria-hidden="true"><SceneSectionGlyph icon={icon} /></span>
      <h2>{title}</h2>
    </div>
  );
}


function SceneNarrativePanel({ qaState, campaignRead }: Pick<TemplateProps, 'qaState' | 'campaignRead'>) {
  const [expanded, setExpanded] = useState(false);
  const expandButtonRef = useRef<HTMLButtonElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!expanded) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setExpanded(false);
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
      requestAnimationFrame(() => expandButtonRef.current?.focus());
    };
  }, [expanded]);

  const sceneNarration = campaignRead.status === 'ready' ? campaignRead.scene?.narration : null;
  const sceneQuestion = campaignRead.status === 'ready' ? campaignRead.scene?.question : null;
  const narrativeContent = sceneNarration
    ? <><p>{sceneNarration}</p>{sceneQuestion && <p>{sceneQuestion}</p>}</>
    : qaState === 'demo'
      ? <p>QA VISUAL — aguardando conteúdo de narração.</p>
      : <p>Aguardando narração do Mestre.</p>;

  return (
    <>
      <section className="scene-paper-panel scene-narrative-paper">
        <div className="scene-narrative-heading-row">
          <SceneSectionHeading title="Narração do Mestre" tone="rust" icon="speaker" />
          <button
            ref={expandButtonRef}
            type="button"
            className="scene-narrative-expand-button"
            aria-label="Maximizar Narração do Mestre"
            aria-expanded={expanded}
            onClick={() => setExpanded(true)}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M8 3H3v5M16 3h5v5M8 21H3v-5M21 16v5h-5" />
            </svg>
          </button>
        </div>
        <div className="scene-paper-copy">{narrativeContent}</div>
      </section>

      {expanded && (
        <div className="scene-narrative-reader-overlay" role="dialog" aria-modal="true" aria-label="Leitura ampliada da Narração do Mestre">
          <section className="scene-paper-panel scene-narrative-paper scene-narrative-reader">
            <div className="scene-narrative-heading-row scene-reader-heading">
              <SceneSectionHeading title="Narração do Mestre" tone="rust" icon="speaker" />
              <button
                ref={closeButtonRef}
                type="button"
                className="scene-narrative-expand-button is-minimize"
                aria-label="Minimizar Narração do Mestre"
                onClick={() => setExpanded(false)}
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M9 9H4V4M15 9h5V4M9 15H4v5M20 20v-5h-5" />
                </svg>
              </button>
            </div>
            <div className="scene-paper-copy scene-reader-copy" tabIndex={0}>
              {narrativeContent}
            </div>
          </section>
        </div>
      )}
    </>
  );
}


function SceneSituationAndParticipants() {
  return (
    <div className="scene-situation-grid">
      <section className="scene-paper-panel scene-situation-paper">
        <SceneSectionHeading title="Situação imediata" tone="rust" icon="target" />
        <p>Aguardando contexto público e já revelado.</p>
      </section>

      <section className="scene-metal-panel scene-participants-panel">
        <SceneSectionHeading title="Participantes visíveis" icon="group" />
        <div className="scene-participant-strip" aria-label="Participantes visíveis">
          {Array.from({ length: 3 }, (_, index) => (
            <div className="scene-participant-slot" key={index}>
              <span className="scene-participant-frame is-empty" aria-hidden="true"><span className="scene-participant-empty-mark" /></span>
              <strong>—</strong>
              <small>Aguardando participante</small>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}


function SceneActionGrid() {
  return (
    <section className="scene-metal-panel scene-actions-panel">
      <SceneSectionHeading title="Ações disponíveis" icon="target" />
      <div className="scene-action-grid" aria-label="Ações contextuais">
        {Array.from({ length: 6 }, (_, index) => (
          <div className="scene-action-slot is-empty" key={index}>
            <span className="scene-action-glyph" aria-hidden="true" />
            <span className="scene-action-copy">
              <strong>—</strong>
              <small>Aguardando ação disponível</small>
            </span>
            <span className="scene-action-empty-end" aria-hidden="true" />
          </div>
        ))}
      </div>
    </section>
  );
}


function SceneIntentComposer({
  sceneDraft,
  setSceneDraft,
  masterBridge,
  campaignRead,
  intentSubmitState,
  intentSubmitMessage,
  submitSceneIntent,
}: Pick<TemplateProps, 'sceneDraft' | 'setSceneDraft' | 'masterBridge' | 'campaignRead' | 'intentSubmitState' | 'intentSubmitMessage' | 'submitSceneIntent'>) {
  const bridgeReady = masterBridge === 'available' && campaignRead.status === 'ready';
  const sending = intentSubmitState === 'sending';
  const canSend = bridgeReady && !sending && sceneDraft.trim().length > 0;
  const buttonLabel = sending ? 'Enviando…' : 'Propor ao Mestre';

  return (
    <section className="scene-metal-panel scene-composer">
      <SceneSectionHeading title="O que você faz?" icon="chat" />
      <div className="scene-composer-row">
        <textarea
          value={sceneDraft}
          onChange={(event) => setSceneDraft(event.target.value)}
          placeholder="Descreva sua ação neste momento…"
          rows={2}
          aria-label="Rascunho da intenção do personagem"
          disabled={sending}
        />
        {bridgeReady ? (
          <button
            type="button"
            className="scene-submit-guard"
            disabled={!canSend}
            aria-busy={sending || undefined}
            onClick={() => { void submitSceneIntent(); }}
            style={{ appearance: 'none', fontFamily: 'inherit', cursor: canSend ? 'pointer' : 'default', opacity: canSend || sending ? 1 : .62 }}
          >
            {buttonLabel}
          </button>
        ) : (
          <div role="note" className="scene-submit-guard">Envio indisponível</div>
        )}
      </div>
      {intentSubmitMessage && (
        <div role={intentSubmitState === 'error' ? 'alert' : 'status'} style={{ marginTop: 6, color: intentSubmitState === 'error' ? '#E5A28F' : '#9CCBDA', fontSize: '.7rem', lineHeight: 1.25 }}>
          {intentSubmitMessage}
        </div>
      )}
      <span className="sr-only">
        {bridgeReady ? 'Canal do Mestre disponível.' : 'Rascunho local. O envio ao Mestre está indisponível.'}
      </span>
    </section>
  );
}


function SceneScreen(props: TemplateProps) {
  const { screen, qaState, sceneDraft, setSceneDraft, openMenu, campaignRead, masterBridge, intentSubmitState, intentSubmitMessage, submitSceneIntent } = props;
  const vehicleCombat = screen.route === '03';

  return (
    <main className={`scene-concept-shell scene-route-${screen.route}`}>
      <SceneContextHeader screen={screen} qaState={qaState} openMenu={openMenu} />
      {vehicleCombat && <SceneVehicleStatusStrip qaState={qaState} />}
      <SceneMediaSlot qaState={qaState} sceneImageUrl={props.sceneImageUrl} />
      <SceneNarrativePanel qaState={qaState} campaignRead={campaignRead} />
      <SceneSituationAndParticipants />
      <SceneActionGrid />
      <SceneIntentComposer
        sceneDraft={sceneDraft}
        setSceneDraft={setSceneDraft}
        masterBridge={masterBridge}
        campaignRead={campaignRead}
        intentSubmitState={intentSubmitState}
        intentSubmitMessage={intentSubmitMessage}
        submitSceneIntent={submitSceneIntent}
      />
    </main>
  );
}


function DataActionNotice({ state, message }: { state: BridgeDataState; message: string }) {
  if (state === 'idle' || (!message && state !== 'working')) return null;
  const body = state === 'working' ? 'Processando…' : message;
  return <Card title="Dados" tone={state === 'error' ? 'warning' : 'soft'}>{body}</Card>;
}


function CharacterScreen(props: TemplateProps) {
  const progress = props.screen.route === '07';
  const model = props.campaignRead;
  const character = model.status === 'ready' ? model.character : null;
  const canCharacterJson = props.dataCapabilities.characterJson;
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <CampaignReadNotice model={model} />
      {character ? progress ? (
        <>
          <Card title="Progressão"><DataGrid fields={character.progression} /></Card>
          <Card title="Progressão" tone="soft">Valores exibidos são do estado atual. Nenhuma compra é aplicada por esta tela.</Card>
        </>
      ) : (
        <>
          <Card title="Identidade"><DataGrid fields={character.identity} /></Card>
          <Card title="Estado"><DataGrid fields={character.state} /></Card>
          <Card title="Características"><DataGrid fields={character.characteristics} /></Card>
          <Card title="Perícias treinadas" tone="soft"><DataGrid fields={character.trainedSkills.length ? character.trainedSkills : [{ label: 'Perícias treinadas', value: 'Nenhuma' }]} /></Card>
          <Card title="Arquivo da ficha" tone="soft">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <button type="button" style={{ ...button, opacity: canCharacterJson ? 1 : .45 }} disabled={!canCharacterJson} onClick={props.exportCharacterJson}>Exportar JSON</button>
              <label style={{ ...button, display: 'grid', placeItems: 'center', textAlign: 'center', opacity: canCharacterJson ? 1 : .45, cursor: canCharacterJson ? 'pointer' : 'default' }}>
                Importar JSON
                <input type="file" accept="application/json,.json" hidden disabled={!canCharacterJson} onChange={(event) => { const file = event.currentTarget.files?.[0]; if (file) void props.importCharacterJsonFile(file); event.currentTarget.value = ''; }} />
              </label>
            </div>
            {!canCharacterJson && <p style={{ margin: '8px 0 0', color: '#A9B0B5' }}>Importação/exportação exige o Motor Barbara R23 ou posterior.</p>}
          </Card>
          <DataActionNotice state={props.dataActionState} message={props.dataActionMessage} />
        </>
      ) : (
        <>
          <Card title="Personagem" tone="soft">Nenhum personagem carregado.</Card>
          <button type="button" style={button} onClick={() => props.navigate('18')}>Abrir fluxo de criação</button>
        </>
      )}
    </div>
  );
}


function ItemsScreen(props: TemplateProps) {
  const acquisition = props.screen.route === '09';
  const model = props.campaignRead;
  const inventory = model.status === 'ready' ? model.inventory : null;
  const pending = inventory?.pending ?? null;
  const canPropose = model.status === 'ready' && Boolean(model.character) && props.equipmentCapabilities.proposeWeapon && !pending && props.equipmentActionState !== 'working';
  const weaponMeta = (item: BridgeCatalogWeapon) => {
    const damage = item.damageBonus !== null ? `Fortaleza ${item.damageBonus >= 0 ? '+' : ''}${item.damageBonus}` : item.damage === null ? '—' : String(item.damage);
    return `Dano ${damage} · Crítico ${item.crit ?? '—'} · ${item.range} · ${item.price.toLocaleString('pt-BR')} créditos · raridade ${item.rarity}${item.restricted ? ' · restrita' : ''}`;
  };
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <CampaignReadNotice model={model} />
      {model.status !== 'ready' ? null : acquisition ? (
        <>
          <Card title="Pedido atual" tone={pending ? 'warning' : 'soft'}>
            {pending
              ? <DisplayList items={[pending]} emptyText="Nenhum item selecionado." />
              : <p style={{ margin: 0 }}>Nenhum pedido de aquisição pendente.</p>}
          </Card>
          <Card title="Catálogo sustentado" tone="soft">
            {!props.equipmentCapabilities.catalog ? (
              <>
                <p style={{ margin: 0 }}>O Motor Barbara conectado ainda não expõe o catálogo auditado para esta shell.</p>
                <button type="button" style={{ ...mutedButton, width: '100%', marginTop: 10 }} onClick={props.refreshEquipmentCatalog}>Tentar novamente</button>
              </>
            ) : !props.equipmentWeapons.length ? (
              <p style={{ margin: 0, color: '#A9B0B5' }}>Nenhuma arma disponível no catálogo auditado.</p>
            ) : (
              <div style={{ display: 'grid', gap: 8 }}>
                {props.equipmentWeapons.map((item) => (
                  <div key={item.id} style={{ ...softPanel, padding: 10, display: 'grid', gap: 8 }}>
                    <div>
                      <strong>{item.name}</strong>
                      <div style={{ marginTop: 4, color: '#A9B0B5', fontSize: 14, overflowWrap: 'anywhere' }}>{weaponMeta(item)}</div>
                      {item.qualities.length > 0 && <div style={{ marginTop: 4, color: '#A9B0B5', fontSize: 14, overflowWrap: 'anywhere' }}>{item.qualities.join(' · ')}</div>}
                    </div>
                    <button type="button" style={{ ...button, opacity: canPropose ? 1 : .45 }} disabled={!canPropose} onClick={() => props.proposeWeaponAcquisition(item.id)}>
                      {pending ? 'Aguardando Mestre' : 'Propor ao Mestre'}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </Card>
          <Card title="Regra de segurança" tone="soft">A seleção cria somente um pedido ao Mestre. Disponibilidade local, preço final, aprovação, créditos e inventário só mudam após resolução confirmada pelo motor.</Card>
          {props.equipmentActionState !== 'idle' && (
            <Card title="Aquisição" tone={props.equipmentActionState === 'error' ? 'warning' : 'soft'}>
              {props.equipmentActionState === 'working' ? 'Enviando pedido ao Mestre…' : props.equipmentActionMessage}
            </Card>
          )}
        </>
      ) : !inventory ? (
        <Card title="Inventário" tone="soft">Nenhum inventário de personagem disponível.</Card>
      ) : (
        <>
          <Card title="Créditos">
            {inventory.credits === null
              ? <p style={{ margin: 0, color: '#A9B0B5' }}>Saldo não registrado.</p>
              : <DataGrid fields={[{ label: 'Saldo', value: String(inventory.credits) }]} />}
          </Card>
          <Card title="Inventário"><DisplayList items={inventory.owned} emptyText="Nenhum item registrado." /></Card>
        </>
      )}
    </div>
  );
}


function ShipScreen(props: TemplateProps) {
  const critical = props.screen.route === '11';
  const model = props.campaignRead;
  const vehicle = model.status === 'ready' ? model.vehicle : null;
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <CampaignReadNotice model={model} />
      {model.status !== 'ready' ? null : !vehicle?.active ? (
        <Card title="Nave" tone="soft">Nenhuma nave ativa registrada.</Card>
      ) : critical ? (
        <Card title="Danos críticos"><DisplayList items={vehicle.criticalHits} emptyText="Nenhum dano crítico registrado." /></Card>
      ) : (
        <>
          <Card title={vehicle.name ? `Resumo da nave · ${vehicle.name}` : 'Resumo da nave'}>
            {meaningfulFields(vehicle.summary).length
              ? <DataGrid fields={meaningfulFields(vehicle.summary)} />
              : <p style={{ margin: 0, color: '#A9B0B5' }}>Nenhum dado técnico da nave registrado.</p>}
          </Card>
          <Card title="Armamentos"><DisplayList items={vehicle.weapons} emptyText="Nenhum armamento registrado." /></Card>
          <Card title="Tripulação" tone="soft"><DisplayList items={vehicle.crew} emptyText="Nenhuma tripulação registrada." /></Card>
        </>
      )}
    </div>
  );
}


function RecordsScreen(props: TemplateProps) {
  const model = props.campaignRead;
  const records = model.status === 'ready' ? model.records : null;
  let content: ReactNode;
  let title = 'Registros';
  switch (props.screen.route) {
    case '12':
      title = 'Missão';
      content = <DisplayList items={records?.mission ?? []} emptyText="Nenhum objetivo de missão registrado." />;
      break;
    case '13':
      title = 'Grupo / Base';
      content = meaningfulFields(records?.group).length
        ? <DataGrid fields={meaningfulFields(records?.group)} />
        : <p style={{ margin: 0, color: '#A9B0B5' }}>Nenhum dado de grupo ou base registrado.</p>;
      break;
    case '14':
      title = 'Contatos conhecidos';
      content = <DisplayList items={records?.contacts ?? []} emptyText="Nenhum contato conhecido registrado." />;
      break;
    case '15':
      title = 'Histórico';
      content = <DisplayList items={records?.history ?? []} emptyText="Nenhuma sessão encerrada registrada." />;
      break;
    default:
      content = <p style={{ margin: 0 }}>Nenhum registro disponível.</p>;
  }
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <CampaignReadNotice model={model} />
      {model.status === 'ready' && <Card title={title}>{content}</Card>}
    </div>
  );
}


function MapScreen(props: TemplateProps) {
  const galactic = props.screen.route === '16';
  const query = props.imageMapQuery.trim().toLocaleLowerCase('pt-BR');
  const filteredMaps = props.imageMaps
    .filter(item => galactic ? item.mapKind === 'galaxy' : item.mapKind !== 'galaxy')
    .filter(item => !query || [item.title,item.mapKind,item.subtype,item.sourceName,...item.tags].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(query));
  const selected = props.selectedImageAsset;
  const selectedIsExternal = selected?.sourceType === 'external_reference';
  const canCache = selectedIsExternal && selected?.remoteReady === true && props.imageLibraryNative === 'available';
  const cached = Boolean(props.selectedImageCache?.cached);
  const sourceLabel = selected?.sourceType === 'external_reference' ? (selected.sourceName ?? 'Fonte externa') : 'Biblioteca local';
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <div style={{ ...softPanel, minHeight: 260, display: 'grid', placeItems: 'center', textAlign: 'center', overflow: 'hidden' }}>
        {selected && props.selectedImageUrl ? (
          <div style={{width:'100%',display:'grid',gap:8}}>
            <img src={props.selectedImageUrl} alt={selected.title} style={{display:'block',width:'100%',maxHeight:420,objectFit:'contain',borderRadius:6,background:'#080A0B'}} />
            <div style={{textAlign:'left'}}><strong>{selected.title}</strong><div style={{fontSize:13,color:'#A9B0B5',marginTop:2}}>{sourceLabel}{cached ? ' · cache privado' : selectedIsExternal ? ' · online' : ' · offline'}</div></div>
          </div>
        ) : selected ? (
          <div style={{display:'grid',gap:8,placeItems:'center'}}><ReferenceIcon name="map" /><h2 style={{margin:0}}>{selected.title}</h2><p style={{margin:0,color:'#A9B0B5'}}>{selectedIsExternal && props.imageLibraryNative==='available' ? 'Mapa externo encontrado. Armazene no cache privado para exibir no aplicativo.' : 'Imagem ainda não disponível neste host.'}</p></div>
        ) : (
          <div><ReferenceIcon name="map" /><h2 style={{ marginBottom: 6 }}>{galactic ? 'Mapa galáctico' : 'Mapa da missão'}</h2><p style={{ margin: 0, color: '#A9B0B5' }}>{props.imageLibraryCapabilities.maps ? 'Selecione um mapa da biblioteca.' : 'Biblioteca de mapas indisponível neste host.'}</p></div>
        )}
      </div>
      {selected && <Card title="Mapa selecionado" tone="soft">
        <div style={{display:'grid',gap:7}}>
          <div><strong>Tipo:</strong> {selected.mapKind ?? selected.subtype ?? 'mapa'}</div>
          <div><strong>Origem:</strong> {sourceLabel}</div>
          <div><strong>Offline:</strong> {selectedIsExternal ? cached ? 'sim, no cache privado' : 'não' : 'sim, empacotado'}</div>
          {selected.sourcePageUrl && <a href={selected.sourcePageUrl} target="_blank" rel="noreferrer" style={{color:'#9CCBDA',overflowWrap:'anywhere'}}>Abrir página da fonte</a>}
          {selectedIsExternal && <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}>
            <button type="button" style={{...button,opacity:canCache && !cached ? 1:.45}} disabled={!canCache || cached || props.imageActionState==='working'} onClick={props.cacheSelectedImageMap}>{cached?'Em cache':'Guardar offline'}</button>
            <button type="button" style={{...mutedButton,opacity:cached && props.imageLibraryNative==='available'?1:.45}} disabled={!cached || props.imageLibraryNative!=='available'} onClick={props.clearSelectedImageMapCache}>Remover cache</button>
          </div>}
          {props.imageActionMessage && <div role={props.imageActionState==='error'?'alert':'status'} style={{color:props.imageActionState==='error'?'#E5A28F':'#9CCBDA',fontSize:13}}>{props.imageActionState==='working'?'Processando… ':''}{props.imageActionMessage}</div>}
        </div>
      </Card>}
      <Card title={`Biblioteca de mapas · ${props.imageMaps.length}`}>
        <div style={{display:'grid',gap:8}}>
          <div style={{display:'grid',gridTemplateColumns:'minmax(0,1fr) auto',gap:8}}>
            <input value={props.imageMapQuery} onChange={event=>props.setImageMapQuery(event.currentTarget.value)} placeholder={galactic?'Buscar mapa galáctico':'Buscar Tatooine, Mos Shuuta, Hoth…'} style={{width:'100%',minWidth:0,minHeight:42,border:'1px solid #596269',borderRadius:6,background:'#171A1C',color:C.durasteel,padding:'8px 10px',fontSize:15}} />
            <button type="button" style={{...mutedButton,minWidth:84}} onClick={props.refreshImageLibrary}>Atualizar</button>
          </div>
          <div style={{display:'grid',gap:7,maxHeight:330,overflowY:'auto',paddingRight:2}}>
            {filteredMaps.map(item => <button key={item.assetId} type="button" onClick={()=>props.selectImageMap(item.assetId)} style={{...softPanel,padding:10,textAlign:'left',color:C.durasteel,cursor:'pointer',borderColor:selected?.assetId===item.assetId?C.oldGold:'#596269'}}>
              <div style={{display:'grid',gridTemplateColumns:'minmax(0,1fr) auto',gap:8,alignItems:'start'}}><strong style={{overflowWrap:'anywhere'}}>{item.title}</strong><span style={{fontSize:11,color:item.sourceType==='external_reference'?C.cyan:'#91C9A0',textTransform:'uppercase'}}>{item.sourceType==='external_reference'?'externo':'local'}</span></div>
              <div style={{fontSize:13,color:'#A9B0B5',marginTop:3}}>{item.mapKind ?? item.subtype ?? 'mapa'} · {(item.tags||[]).filter(tag=>!['star-wars','map','kainrath'].includes(tag)).slice(0,4).join(' · ') || 'sem tags adicionais'}</div>
            </button>)}
            {!filteredMaps.length && <p style={{margin:0,color:'#A9B0B5'}}>Nenhum mapa corresponde ao filtro atual.</p>}
          </div>
        </div>
      </Card>
      <Card title={galactic ? 'Deslocamento' : 'Áreas visíveis'} tone="warning">{galactic ? 'Selecionar um destino ou trocar o mapa exibido não confirma a viagem; o deslocamento só muda após confirmação do Mestre.' : 'A biblioteca é uma superfície de consulta. Selecionar ou armazenar um mapa não revela local, não move o grupo e não altera o estado da campanha.'}</Card>
    </div>
  );
}


function CreationScreen(props: TemplateProps) {
  const step = Number(props.screen.route) - 17;
  const route = props.screen.route;
  const isFirstStep = route === '18';
  const isLastStep = route === '26';
  const canRandom = props.dataCapabilities.randomCharacter;
  const [randomName, setRandomName] = useState('');
  const [randomMotivation, setRandomMotivation] = useState('');
  const catalog = props.creationCatalog;
  const draft = props.creationDraft;
  const isAorOrigin = catalog?.creationOrigin === 'AOR';
  const linkName = isAorOrigin ? 'Duty' : 'Obligation';
  const species = catalog?.species.find(item => item.key === draft.speciesKey) ?? null;
  const career = catalog?.careers.find(item => item.key === draft.careerKey) ?? null;
  const specialization = career?.specializations.find(item => item.key === draft.specializationKey) ?? null;
  const fieldStyle: CSSProperties = { width: '100%', minWidth: 0, minHeight: 42, border: '1px solid #596269', borderRadius: 6, background: '#171A1C', color: C.durasteel, padding: '8px 10px', fontSize: 16 };
  const labelForSkill = (key: string) => catalog?.skillLabels[key] ?? key;
  const setDraft = (patch: Partial<BridgeCreationOptions>) => props.setCreationDraft(current => ({ ...current, ...patch }));
  const toggleLimited = (list: string[], key: string, limit: number) => list.includes(key) ? list.filter(item => item !== key) : list.length < limit ? [...list, key] : list;
  const renderSkillChoices = (title: string, skills: string[], selected: string[], limit: number, setter: (next: string[]) => void) => (
    <Card title={title} tone="soft">
      <p style={{ margin: '0 0 8px', color: '#A9B0B5' }}>Escolha {limit}. Selecionadas: {selected.length}/{limit}.</p>
      <div style={{ display: 'grid', gap: 6 }}>{skills.map(key => <label key={key} style={{ display: 'flex', gap: 8, alignItems: 'center', minHeight: 36 }}><input type="checkbox" checked={selected.includes(key)} onChange={() => setter(toggleLimited(selected, key, limit))} /><span>{labelForSkill(key)}</span></label>)}</div>
    </Card>
  );

  const manualUnavailable = !props.creationCapabilities.catalog || !catalog;
  let manualContent: ReactNode = null;
  if (isFirstStep) {
    manualContent = <>
      {catalog?.campaignMode === 'MIXED' && <Card title="Origem da ficha · campanha mista" tone="soft"><label style={{display:'grid',gap:5}}><span>Regras de criação</span><select value={draft.creationOrigin ?? 'EOTE'} style={fieldStyle} onChange={event=>setDraft({creationOrigin:event.currentTarget.value as 'EOTE'|'AOR',speciesKey:'',careerKey:'',specializationKey:'',obligationType:'',obligationBenefits:{xp5:false,xp10:false,credits1000:false,credits2500:false},characteristics:{},careerFreeSkills:[],specializationFreeSkills:[],speciesChoiceSkills:[]})}><option value="EOTE">Edge of the Empire · Obligation</option><option value="AOR">Age of Rebellion · Duty</option></select></label><p style={{margin:'8px 0 0',color:'#A9B0B5',fontSize:13}}>A campanha continua MISTA: esta escolha define apenas a origem mecânica desta ficha. Obrigação e Dever permanecem separados no grupo.</p></Card>}
      <Card title="Identidade manual" tone="soft">
        <div style={{ display: 'grid', gap: 8 }}>
          <label style={{ display: 'grid', gap: 5 }}><span>Nome</span><input value={draft.name} onChange={event => setDraft({ name: event.currentTarget.value })} placeholder="Informe o nome" style={fieldStyle} /></label>
          <label style={{ display: 'grid', gap: 5 }}><span>Apresentação / histórico</span><textarea value={draft.background ?? ''} onChange={event => setDraft({ background: event.currentTarget.value })} rows={4} placeholder="Texto livre; não altera regras." style={{ ...fieldStyle, resize: 'vertical' }} /></label>
          <p style={{ margin: 0, color: '#A9B0B5', fontSize: 13 }}>O schema atual não possui campo canônico de retrato; a shell não inventa persistência para imagem de personagem.</p>
        </div>
      </Card>
      <Card title="Criação mecânica aleatória" tone="soft">
        <p style={{ margin: '0 0 10px', color: '#A9B0B5' }}>Alternativa: o motor escolhe somente entre opções auditadas. Nome e motivação continuam fornecidos por você.</p>
        <div style={{ display: 'grid', gap: 8 }}>
          <input value={randomName} onChange={(e) => setRandomName(e.target.value)} placeholder="Nome para geração aleatória" style={fieldStyle} />
          <textarea value={randomMotivation} onChange={(e) => setRandomMotivation(e.target.value)} placeholder="Motivação" rows={3} style={{ ...fieldStyle, resize: 'vertical' }} />
          <button type="button" style={{ ...button, opacity: canRandom ? 1 : .45 }} disabled={!canRandom || props.dataActionState === 'working'} onClick={() => props.generateRandomCharacter(randomName, randomMotivation)}>Gerar ficha mecânica</button>
        </div>
        {!canRandom && <p style={{ margin: '8px 0 0', color: '#A9B0B5' }}>Geração aleatória indisponível neste host.</p>}
      </Card>
      <DataActionNotice state={props.dataActionState} message={props.dataActionMessage} />
    </>;
  } else if (route === '19') {
    manualContent = <Card title="Espécie" tone="soft">
      <select value={draft.speciesKey} style={fieldStyle} onChange={event => {
        const next = catalog?.species.find(item => item.key === event.currentTarget.value);
        setDraft({ speciesKey: event.currentTarget.value, speciesVariant: next?.speciesChoice?.type === 'gandRespiration' ? (next.speciesChoice.variants?.[0] ?? null) : null, characteristics: next ? { ...next.characteristics } : {}, careerFreeSkills: [], specializationFreeSkills: [], speciesChoiceSkills: [] });
      }}><option value="">Selecione uma espécie</option>{catalog?.species.map(item => <option key={item.key} value={item.key}>{item.label}</option>)}</select>
      {species && <div style={{ marginTop: 10, display: 'grid', gap: 6 }}><DataGrid fields={[{label:'PE inicial',value:String(species.xp)},{label:'Ferimentos base',value:String(species.woundBase)},{label:'Tensão base',value:String(species.strainBase)}]} />{species.abilities.map(text => <p key={text} style={{ margin: 0, color: '#C3C8CB' }}>{text}</p>)}</div>}{species?.speciesChoice?.type === 'gandRespiration' && <label style={{display:'grid',gap:5,marginTop:10}}><span>Fisiologia respiratória</span><select value={draft.speciesVariant ?? ''} style={fieldStyle} onChange={event=>setDraft({speciesVariant:event.currentTarget.value})}>{(species.speciesChoice.variants ?? []).map(value=><option key={value} value={value}>{value==='lungs'?'Com pulmões (+10 PE; respirador de amônia)':'Sem pulmões (não precisa respirar)'}</option>)}</select></label>}
    </Card>;
  } else if (route === '20') {
    manualContent = <Card title="Carreira" tone="soft"><select value={draft.careerKey} style={fieldStyle} onChange={event => setDraft({ careerKey:event.currentTarget.value, specializationKey:'', careerFreeSkills:[], specializationFreeSkills:[], speciesChoiceSkills:[] })}><option value="">Selecione uma carreira</option>{catalog?.careers.map(item => <option key={item.key} value={item.key}>{item.label}</option>)}</select>{career && <p style={{ margin:'10px 0 0', color:'#C3C8CB' }}>Perícias de carreira: {career.skills.map(labelForSkill).join(' · ')}</p>}</Card>;
  } else if (route === '21') {
    manualContent = <Card title="Especialização" tone="soft"><select value={draft.specializationKey} disabled={!career} style={{...fieldStyle,opacity:career?1:.5}} onChange={event => setDraft({ specializationKey:event.currentTarget.value, specializationFreeSkills:[], speciesChoiceSkills:[] })}><option value="">Selecione uma especialização</option>{career?.specializations.map(item => <option key={item.key} value={item.key}>{item.label}</option>)}</select>{specialization && <p style={{ margin:'10px 0 0', color:'#C3C8CB' }}>Perícias: {specialization.skills.map(labelForSkill).join(' · ')}</p>}</Card>;
  } else if (route === '22') {
    const careerCount = species?.careerFreeCount ?? 0;
    const specCount = species?.specializationFreeCount ?? 0;
    const allCareer = new Set([...(career?.skills ?? []), ...(specialization?.skills ?? [])]);
    const speciesSkills = species?.speciesChoice?.type === 'oneSkill' ? (species.speciesChoice.skills ?? []) : species?.speciesChoice?.type === 'twoNonCareer' ? Object.keys(catalog?.skillLabels ?? {}).filter(key => !allCareer.has(key)) : [];
    const speciesLimit = species?.speciesChoice?.type === 'oneSkill' ? 1 : species?.speciesChoice?.type === 'twoNonCareer' ? 2 : 0;
    manualContent = <>{career && species ? renderSkillChoices('Perícias gratuitas da carreira',career.skills,draft.careerFreeSkills,careerCount,next=>setDraft({careerFreeSkills:next})) : <Card title="Perícias" tone="warning">Selecione espécie e carreira primeiro.</Card>}{specialization && species ? renderSkillChoices('Perícias gratuitas da especialização',specialization.skills,draft.specializationFreeSkills,specCount,next=>setDraft({specializationFreeSkills:next})) : null}{species && speciesLimit>0 ? renderSkillChoices(species.speciesChoice?.type==='twoNonCareer'?'Escolha da espécie — Humano':'Escolha da espécie',speciesSkills,draft.speciesChoiceSkills,speciesLimit,next=>setDraft({speciesChoiceSkills:next})) : null}</>;
  } else if (route === '23') {
    manualContent = <><Card title="Características" tone="soft">{species ? <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}>{Object.entries(species.characteristics).map(([key,base]) => <label key={key} style={{display:'grid',gap:4}}><span>{catalog?.characteristicLabels[key] ?? key}</span><select value={draft.characteristics[key] ?? base} style={fieldStyle} onChange={event => setDraft({characteristics:{...draft.characteristics,[key]:Number(event.currentTarget.value)}})}>{Array.from({length:6-base},(_,i)=>base+i).map(value=><option key={value} value={value}>{value}</option>)}</select><small style={{color:'#899196'}}>base {base}</small></label>)}</div> : <p style={{margin:0}}>Selecione uma espécie primeiro.</p>}</Card>{props.creationPreview && <Card title="Orçamento de PE"><DataGrid fields={[{label:'PE disponível',value:String(props.creationPreview.xpBudget)},{label:'Gasto em características',value:String(props.creationPreview.characteristicCost)},{label:'PE restante',value:String(props.creationPreview.xpRemaining)}]} /></Card>}{props.creationPreviewError && <Card title="Preview" tone="warning">{props.creationPreviewError}</Card>}</>;
  } else if (route === '24') {
    const benefitRows = isAorOrigin ? ([['xp5','−5 Duty → +5 PE'],['xp10','−10 Duty → +10 PE'],['credits1000','−5 Duty → +1.000 créditos'],['credits2500','−10 Duty → +2.500 créditos']] as const) : ([['xp5','+5 Obligation → +5 PE'],['xp10','+10 Obligation → +10 PE'],['credits1000','+5 Obligation → +1.000 créditos'],['credits2500','+10 Obligation → +2.500 créditos']] as const);
    manualContent = <><Card title={`${linkName} / vínculo de campanha`} tone="soft"><label style={{display:'grid',gap:5}}><span>{`Tipo de ${linkName}`}</span><select value={draft.obligationType} style={fieldStyle} onChange={event=>setDraft({obligationType:event.currentTarget.value})}><option value="">{`Selecione ${isAorOrigin?'um Duty':'uma Obligation'}`}</option>{catalog?.obligationTypes.map(value=><option key={value} value={value}>{value}</option>)}</select></label><div style={{display:'grid',gap:6,marginTop:10}}>{benefitRows.map(([key,text])=><label key={key} style={{display:'flex',gap:8,alignItems:'center'}}><input type="checkbox" checked={Boolean(draft.obligationBenefits[key])} onChange={event=>setDraft({obligationBenefits:{...draft.obligationBenefits,[key]:event.currentTarget.checked}})} /><span>{text}</span></label>)}</div></Card>{props.creationPreview && <Card title="Resultado do vínculo"><DataGrid fields={[{label:`${linkName} inicial`,value:String(props.creationPreview.startingObligation)},{label:isAorOrigin?'Duty gasto':'Obligation adicional',value:String(props.creationPreview.additionalObligation)},{label:`${linkName} final`,value:String(props.creationPreview.obligation)},{label:'Créditos iniciais',value:String(props.creationPreview.credits)},{label:'PE total',value:String(props.creationPreview.xpBudget)}]} />{props.creationPreview.obligationNote && <p style={{margin:'8px 0 0',color:'#A9B0B5'}}>{props.creationPreview.obligationNote}</p>}</Card>}{props.creationPreviewError && <Card title="Preview" tone="warning">{props.creationPreviewError}</Card>}</>;
  } else if (route === '25') {
    manualContent = <><Card title="Motivação" tone="soft"><textarea value={draft.motivation} onChange={event=>setDraft({motivation:event.currentTarget.value})} rows={5} placeholder="Motivação é obrigatória; o motor não a inventa." style={{...fieldStyle,resize:'vertical'}} /></Card><Card title="Confirmação" tone="soft"><DataGrid fields={[{label:'Nome',value:draft.name||'—'},{label:'Espécie',value:species?.label??'—'},{label:'Carreira',value:career?.label??'—'},{label:'Especialização',value:specialization?.label??'—'},{label:linkName,value:draft.obligationType||'—'},{label:'PE restante',value:props.creationPreview?String(props.creationPreview.xpRemaining):'—'},{label:'Créditos',value:props.creationPreview?String(props.creationPreview.credits):'—'}]} /><button type="button" style={{...button,width:'100%',marginTop:10,opacity:props.creationCapabilities.create?1:.45}} disabled={!props.creationCapabilities.create||props.creationActionState==='working'} onClick={props.createManualCharacter}>{props.creationActionState==='working'?'Validando no motor…':'Criar ficha manual'}</button>{props.creationActionMessage && <div role={props.creationActionState==='error'?'alert':'status'} style={{marginTop:8,color:props.creationActionState==='error'?'#E5A28F':'#9CCBDA'}}>{props.creationActionMessage}</div>}</Card></>;
  } else if (isLastStep) {
    manualContent = <>
      <Card title="Orçamento atual" tone="soft">{props.campaignRead.status === 'ready' && props.campaignRead.character ? (props.campaignRead.inventory?.credits === null || props.campaignRead.inventory?.credits === undefined ? <p style={{ margin: 0, color: '#A9B0B5' }}>Saldo de créditos não registrado.</p> : <DataGrid fields={[{ label: 'Créditos disponíveis', value: String(props.campaignRead.inventory.credits) }]} />) : <p style={{ margin: 0, color: '#A9B0B5' }}>Crie ou carregue uma ficha para consultar o orçamento.</p>}</Card>
      <Card title="Equipamento auditado">{props.equipmentCapabilities.catalog ? <p style={{ margin: 0 }}>{props.equipmentWeapons.length} arma(s) do catálogo auditado disponíveis para consulta.</p> : <p style={{ margin: 0, color: '#A9B0B5' }}>Catálogo auditado indisponível neste host.</p>}<button type="button" style={{ ...button, width: '100%', marginTop: 10 }} onClick={() => props.navigate('09')}>Abrir aquisição em ITENS</button></Card>
      <Card title="Regra de segurança" tone="warning">A etapa de criação não desconta créditos nem adiciona equipamento diretamente. Toda aquisição permanece no fluxo canônico de ITENS e só muda a ficha após resolução confirmada pelo Mestre.</Card>
    </>;
  }

  return <div style={page}>
    <Header screen={props.screen} qaState={props.qaState} />
    <SiblingNav screen={props.screen} navigate={props.navigate} />
    <Card title={`Etapa ${step} de 9`}><p style={{ margin: 0 }}>{props.screen.description}</p></Card>
    {manualUnavailable && !isFirstStep && !isLastStep ? <Card title="Criação manual indisponível" tone="warning"><p style={{margin:0}}>Esta etapa exige o bridge de criação do Motor Barbara R33/R34. A shell não simula opções locais.</p><button type="button" style={{...mutedButton,width:'100%',marginTop:10}} onClick={props.refreshCreationCatalog}>Tentar novamente</button></Card> : manualContent}
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
      <button type="button" style={{ ...mutedButton, opacity: isFirstStep ? .45 : 1 }} disabled={isFirstStep} onClick={isFirstStep ? undefined : () => props.navigate(String(Number(props.screen.route) - 1).padStart(2, '0'))}>Anterior</button>
      <button type="button" style={{ ...button, opacity: isLastStep ? .45 : 1 }} disabled={isLastStep} onClick={isLastStep ? undefined : () => props.navigate(String(Number(props.screen.route) + 1).padStart(2, '0'))}>Próxima</button>
    </div>
  </div>;
}

function SettingsScreen(props: TemplateProps) {
  const visualQuery = props.imageVisualQuery.trim().toLocaleLowerCase('pt-BR');
  const visualKindMatches = (asset: BridgeImageAsset) => props.imageVisualKind === 'all' || (props.imageVisualKind === 'npc' && asset.category === 'portrait') || (props.imageVisualKind === 'droid' && asset.category === 'droid') || (props.imageVisualKind === 'beast' && asset.category === 'creature') || (props.imageVisualKind === 'vehicle' && asset.category === 'vehicle');
  const visualItems = props.imageVisualEntities.filter(visualKindMatches).filter(asset => !visualQuery || [asset.title,asset.assetId,asset.category,asset.subtype,...asset.tags].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(visualQuery)).slice(0,30);
  const route = props.screen.route;
  const canBackup = props.dataCapabilities.campaignBackup;
  const canSnapshots = props.dataCapabilities.snapshots;
  const canLibrary = props.libraryBridge === 'available' && props.libraryCapabilities.list;
  const [libraryQuery, setLibraryQuery] = useState('');
  const [geminiApiKey, setGeminiApiKey] = useState('');
  const [showScenePrompt, setShowScenePrompt] = useState(false);
  const authorityOptions: LibraryAuthorityType[] = ['ERRATA', 'BASE', 'SUPLEMENTO', 'AVENTURA', 'REFERENCIA'];
  const fieldStyle: CSSProperties = { width: '100%', minWidth: 0, minHeight: 42, border: '1px solid #596269', borderRadius: 6, background: '#171A1C', color: C.durasteel, padding: '8px 10px', fontSize: 15 };
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      {route === '27' && <Card title="Interface"><DataGrid fields={['Escala de texto', 'Contraste', 'Movimento reduzido', 'Preferência visual']} /></Card>}
      {route === '28' && <><Card title="Narração / voz"><DataGrid fields={['Voz PT-BR', 'TTS Android / Gemini', 'Saída de áudio em blocos', 'Fallback Web Speech']} /></Card><Card title="Estado" tone="soft">TTS integrado: no Android, a narração pode usar a voz nativa PT-BR ou Gemini/Charon quando configurado; no navegador, usa Web Speech quando disponível. Narrações longas são encadeadas em múltiplos blocos.</Card></>}
      {route === '29' && <>
        <Card title="Gemini / imagem de cena">
          <div style={{ display: 'grid', gap: 7 }}>
            <div><strong>Host:</strong> {props.geminiBridge === 'available' ? 'bridge Android disponível' : props.geminiBridge === 'invalid' ? 'bridge incompatível' : 'indisponível neste host'}</div>
            <div><strong>Modelo:</strong> {props.geminiStatus?.model ?? 'gemini-3.1-flash-image'}</div>
            <div><strong>Chave:</strong> {props.geminiStatus?.configured ? 'configurada e protegida no Android' : 'não configurada'}</div>
            <div><strong>Imagem da cena atual:</strong> {props.sceneImageUrl ? 'disponível' : props.geminiStatus?.imageUrl ? 'existe para outra cena' : 'não fornecida'}</div>
          </div>
        </Card>
        <Card title="Chave da API Gemini" tone="soft">
          <p style={{ margin: '0 0 10px', color: '#C3C8CB' }}>A chave é enviada diretamente ao host Android e protegida pelo Android Keystore. Ela não entra no estado da campanha, backup, RAG nem armazenamento da interface.</p>
          <div style={{ display: 'grid', gap: 8 }}>
            <input
              type="password"
              value={geminiApiKey}
              onChange={(event) => setGeminiApiKey(event.currentTarget.value)}
              disabled={!props.geminiCapabilities.configure || props.geminiActionState === 'working'}
              autoComplete="new-password"
              spellCheck={false}
              placeholder="Cole sua chave Gemini"
              style={fieldStyle}
            />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <button type="button" style={{ ...button, opacity: props.geminiCapabilities.configure && geminiApiKey.trim() ? 1 : .45 }} disabled={!props.geminiCapabilities.configure || !geminiApiKey.trim() || props.geminiActionState === 'working'} onClick={() => { if (props.configureGeminiApiKey(geminiApiKey)) setGeminiApiKey(''); }}>Salvar chave</button>
              <button type="button" style={{ ...mutedButton, opacity: props.geminiCapabilities.clearKey && props.geminiStatus?.configured ? 1 : .45 }} disabled={!props.geminiCapabilities.clearKey || !props.geminiStatus?.configured || props.geminiActionState === 'working'} onClick={props.clearGeminiApiKey}>Remover chave</button>
            </div>
          </div>
        </Card>
        <Card title="Imagem cinematográfica da cena" tone="soft">
          <p style={{ margin: '0 0 10px', color: '#C3C8CB' }}>O prompt usa somente título, narração e pergunta já projetados publicamente para a CENA. Memória secreta de NPC, RAG privado e notas do Mestre não são enviados.</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <button type="button" style={{ ...button, opacity: props.geminiCapabilities.generate && props.geminiStatus?.configured && props.sceneImagePrompt && !props.geminiStatus?.generating ? 1 : .45 }} disabled={!props.geminiCapabilities.generate || !props.geminiStatus?.configured || !props.sceneImagePrompt || Boolean(props.geminiStatus?.generating)} onClick={props.generateSceneImage}>{props.geminiStatus?.generating ? 'Gerando…' : 'Gerar com Gemini'}</button>
            <button type="button" style={{ ...mutedButton, opacity: props.geminiCapabilities.importImage && props.sceneImagePrompt ? 1 : .45 }} disabled={!props.geminiCapabilities.importImage || !props.sceneImagePrompt} onClick={props.importSceneImage}>Importar imagem</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginTop: 8 }}>
            <button type="button" style={{ ...mutedButton, opacity: props.sceneImagePrompt ? 1 : .45 }} disabled={!props.sceneImagePrompt} onClick={() => setShowScenePrompt(value => !value)}>{showScenePrompt ? 'Ocultar prompt' : 'Mostrar prompt'}</button>
            <button type="button" style={{ ...mutedButton, opacity: props.geminiCapabilities.clearImage && props.geminiStatus?.imageUrl ? 1 : .45 }} disabled={!props.geminiCapabilities.clearImage || !props.geminiStatus?.imageUrl} onClick={props.clearSceneImage}>Remover imagem</button>
          </div>
          {showScenePrompt && props.sceneImagePrompt && <textarea readOnly value={props.sceneImagePrompt.prompt} rows={8} aria-label="Prompt de imagem da cena" style={{ ...fieldStyle, minHeight: 150, resize: 'vertical', marginTop: 8, lineHeight: 1.35 }} />}
          {props.geminiActionMessage && <div role={props.geminiActionState === 'error' ? 'alert' : 'status'} style={{ marginTop: 9, color: props.geminiActionState === 'error' ? '#E5A28F' : '#9CCBDA', fontSize: 13 }}>{props.geminiActionState === 'working' ? 'Processando… ' : ''}{props.geminiActionMessage}</div>}
          {props.geminiBridge !== 'available' && <p style={{ margin: '9px 0 0', color: '#A9B0B5' }}>A web shell não armazena chave nem chama Gemini diretamente. Geração e importação exigem o host Android compatível.</p>}
        </Card>
      </>}
      {route === '30' && <>
        <Card title="Dados / diagnóstico">
          <div style={{ display: 'grid', gap: 6 }}>
            <div><strong>Motor / dados:</strong> {props.masterBridge === 'available' ? 'ponte canônica disponível' : props.masterBridge === 'invalid' ? 'ponte incompatível' : 'indisponível neste host'}</div>
            <div><strong>Biblioteca PDF:</strong> {props.libraryBridge === 'available' ? 'bridge Android disponível' : props.libraryBridge === 'invalid' ? 'bridge incompatível' : 'disponível somente no host Android compatível'}</div>
            <div><strong>Biblioteca visual:</strong> {props.imageLibraryInfo ? `${props.imageLibraryInfo.counts.total ?? props.imageMaps.length} assets · ${props.imageLibraryInfo.counts.maps ?? props.imageMaps.length} mapas` : 'não consultada'}</div>
            <div><strong>Cache visual Android:</strong> {props.imageLibraryNative === 'available' ? 'disponível' : props.imageLibraryNative === 'invalid' ? 'bridge incompatível' : 'indisponível neste host'}</div>
            <div><strong>Fontes locais:</strong> {canLibrary ? `${props.libraryDocuments.length} documento(s)` : 'não consultadas'}</div>
          </div>
        </Card>
        <Card title="Biblioteca visual" tone="soft">
          <p style={{margin:'0 0 10px',color:'#C3C8CB'}}>Assets locais ficam offline. Mapas externos indexados do Kainrath só são baixados por ação explícita e vão para cache privado; imagem nunca cria regra, facção, espécie ou identidade por inferência.</p>
          <div style={{display:'grid',gap:6}}>
            <div><strong>Manifesto:</strong> {props.imageLibraryInfo?.libraryId ?? 'indisponível'}</div>
            <div><strong>Locais:</strong> {props.imageLibraryInfo?.counts.local ?? '—'} · <strong>Externos:</strong> {props.imageLibraryInfo?.counts.external ?? '—'} · <strong>Mapas:</strong> {props.imageLibraryInfo?.counts.maps ?? '—'} · <strong>Veículos/Naves:</strong> {props.imageLibraryInfo?.counts.vehicles ?? 0}</div>
            <button type="button" style={{...mutedButton,marginTop:4,opacity:props.imageLibraryCapabilities.info?1:.45}} disabled={!props.imageLibraryCapabilities.info} onClick={props.refreshImageLibrary}>Atualizar biblioteca visual</button>
          </div>
        </Card>
        <Card title={`Biblioteca de entidades visuais${props.imageLibraryInfo ? ` · ${(props.imageLibraryInfo.counts.portraits ?? 0)+(props.imageLibraryInfo.counts.droids ?? 0)+(props.imageLibraryInfo.counts.creatures ?? 0)+(props.imageLibraryInfo.counts.vehicles ?? 0)}` : ''}`} tone="soft">
          <p style={{margin:'0 0 10px',color:'#C3C8CB'}}>Referências visuais externas para NPCs, droides, criaturas e veículos/naves. Nenhuma ficha, perfil de nave ou estatística é criada a partir destas imagens.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr auto',gap:8}}>
            <input value={props.imageVisualQuery} onChange={event=>props.setImageVisualQuery(event.currentTarget.value)} placeholder="Buscar Cratala, 2-1B, Krayt…" style={{...fieldStyle,minWidth:0}} />
            <select value={props.imageVisualKind} onChange={event=>props.setImageVisualKind(event.currentTarget.value as 'all'|'npc'|'droid'|'beast'|'vehicle')} style={{...fieldStyle,minWidth:112}}>
              <option value="all">Todos</option><option value="npc">NPCs</option><option value="droid">Droides</option><option value="beast">Criaturas</option><option value="vehicle">Veículos/Naves</option>
            </select>
          </div>
          {!props.imageLibraryCapabilities.visuals && <p style={{margin:'9px 0 0',color:'#A9B0B5'}}>O host atual ainda não expõe o catálogo visual R37.</p>}
          {props.imageLibraryCapabilities.visuals && visualItems.length===0 && <p style={{margin:'9px 0 0',color:'#A9B0B5'}}>Nenhuma referência visual encontrada.</p>}
          <div style={{display:'grid',gap:8,marginTop:10}}>
            {visualItems.map(asset=>{ const cached=props.imageCacheStatuses?.[asset.assetId]?.cached===true; return <div key={asset.assetId} style={{border:'1px solid #40484D',borderRadius:6,padding:9,display:'grid',gap:6,minWidth:0}}>
              <div style={{display:'grid',gridTemplateColumns:'1fr auto',gap:8,minWidth:0}}><strong style={{overflowWrap:'anywhere'}}>{asset.title}</strong><span style={{color:'#A9B0B5',fontSize:12}}>{asset.category==='portrait'?'NPC':asset.category==='droid'?'DROIDE':asset.category==='vehicle'?'VEÍCULO/NAVE':'CRIATURA'}</span></div>
              <div style={{fontSize:12,color:'#A9B0B5',overflowWrap:'anywhere'}}>{asset.sourceName ?? 'Kainrath'} · {cached?'offline':'online'}</div>
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}><button type="button" style={{...mutedButton,opacity:props.imageLibraryNative==='available'&&!cached?1:.45}} disabled={props.imageLibraryNative!=='available'||cached} onClick={()=>props.cacheVisualEntity(asset.assetId)}>Guardar offline</button><button type="button" style={{...mutedButton,opacity:props.imageLibraryNative==='available'&&cached?1:.45}} disabled={props.imageLibraryNative!=='available'||!cached} onClick={()=>props.clearVisualEntityCache(asset.assetId)}>Remover cache</button></div>
              {asset.sourcePageUrl && <a href={asset.sourcePageUrl} target="_blank" rel="noreferrer" style={{fontSize:12,color:'#9CCBDA'}}>Abrir fonte</a>}
            </div>;})}
          </div>
        </Card>
        <Card title="Biblioteca / PDFs" tone="soft">
          <p style={{ margin: '0 0 10px', color: '#C3C8CB' }}>PDFs importados são indexados no armazenamento privado do aplicativo. Apenas fontes indexadas e habilitadas entram na consulta documental.</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <button type="button" style={{ ...button, opacity: props.libraryCapabilities.importPdf ? 1 : .45 }} disabled={!props.libraryCapabilities.importPdf} onClick={props.importPdfToLibrary}>Importar PDF</button>
            <button type="button" style={{ ...mutedButton, opacity: canLibrary ? 1 : .45 }} disabled={!canLibrary} onClick={() => props.refreshLibraryDocuments()}>Atualizar lista</button>
          </div>
          {!canLibrary && <p style={{ margin: '9px 0 0', color: '#A9B0B5' }}>A importação/indexação física exige o bridge <code>BraseiroLibrary</code> do aplicativo Android. A web shell não finge esse recurso.</p>}
        </Card>
        {canLibrary && props.libraryDocuments.length === 0 && <Card title="Fontes locais">Nenhum PDF importado neste armazenamento.</Card>}
        {props.libraryDocuments.map((document) => <Card key={document.docId} title={document.fileName} tone="soft">
          <div style={{ display: 'grid', gap: 8 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, alignItems: 'start' }}>
              <div>
                <strong>{libraryStatusLabel(document.status)}</strong>
                <div style={{ marginTop: 3, color: '#A9B0B5', fontSize: 13 }}>
                  {document.pageCount !== null ? `${document.pageCount} pág.` : 'páginas não informadas'}
                  {document.indexedPages !== null ? ` · ${document.indexedPages} indexadas` : ''}
                  {document.textChars !== null ? ` · ${document.textChars.toLocaleString('pt-BR')} caracteres` : ''}
                </div>
              </div>
              <span style={{ ...label, color: document.status === 'INDEXADO' ? '#91C9A0' : document.status === 'ERRO' ? '#E5A28F' : C.oldGold }}>{document.enabled ? 'ATIVA' : 'DESATIVADA'}</span>
            </div>
            {document.lastError && <div style={{ color: '#E5A28F', fontSize: 13 }}>{document.lastError}</div>}
            <label style={{ display: 'grid', gap: 4, fontSize: 13, color: '#B8BEC2' }}>
              Autoridade da fonte
              <select
                value={document.authorityType}
                disabled={!props.libraryCapabilities.policy}
                style={fieldStyle}
                onChange={(event) => props.updateLibraryPolicy(document.docId, event.currentTarget.value as LibraryAuthorityType, document.enabled)}
              >
                {authorityOptions.map((authority) => <option key={authority} value={authority}>{libraryAuthorityLabel(authority)}</option>)}
              </select>
            </label>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 14 }}>
              <input type="checkbox" checked={document.enabled} disabled={!props.libraryCapabilities.policy} onChange={(event) => props.updateLibraryPolicy(document.docId, document.authorityType, event.currentTarget.checked)} />
              Usar esta fonte nas consultas do Mestre
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <button type="button" style={{ ...mutedButton, minHeight: 40, opacity: props.libraryCapabilities.reindex ? 1 : .45 }} disabled={!props.libraryCapabilities.reindex} onClick={() => props.reindexLibraryDocument(document.docId)}>Reindexar</button>
              <button type="button" style={{ ...mutedButton, minHeight: 40, borderColor: '#81483E', color: '#E5A28F', opacity: props.libraryCapabilities.remove ? 1 : .45 }} disabled={!props.libraryCapabilities.remove} onClick={() => props.removeLibraryDocument(document.docId)}>Remover</button>
            </div>
          </div>
        </Card>)}
        <Card title="Consultar PDFs indexados">
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: 8 }}>
            <input value={libraryQuery} onChange={(event) => setLibraryQuery(event.currentTarget.value)} onKeyDown={(event) => { if (event.key === 'Enter' && props.libraryCapabilities.search) props.searchLibrary(libraryQuery); }} disabled={!props.libraryCapabilities.search} placeholder="Buscar nos manuais importados" style={fieldStyle} />
            <button type="button" style={{ ...button, minWidth: 78, opacity: props.libraryCapabilities.search ? 1 : .45 }} disabled={!props.libraryCapabilities.search} onClick={() => props.searchLibrary(libraryQuery)}>Buscar</button>
          </div>
          {props.librarySearchResults.length > 0 && <div style={{ display: 'grid', gap: 8, marginTop: 10 }}>
            {props.librarySearchResults.map((hit) => <div key={hit.id} style={{ ...softPanel, padding: 10 }}>
              <strong>{hit.title}</strong>
              <div style={{ marginTop: 3, color: '#A9B0B5', fontSize: 13 }}>{hit.page !== null ? `Página ${hit.page}` : 'Página não informada'} · {libraryAuthorityLabel(hit.authorityType)}</div>
              <div style={{ marginTop: 6, fontSize: 14 }}>{hit.excerpt}</div>
            </div>)}
          </div>}
          {props.librarySearchResults.length > 0 && <p style={{ margin: '9px 0 0', color: props.librarySearchGrounded ? '#91C9A0' : '#E5A28F', fontSize: 13 }}>{props.librarySearchGrounded ? 'Trechos recuperados do índice local.' : 'Resultado não deve ser tratado como regra conhecida.'}</p>}
        </Card>
        <DataActionNotice state={props.libraryActionState} message={props.libraryActionMessage} />
        <Card title="Backup e recuperação" tone="soft">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <button type="button" style={{ ...button, opacity: canBackup ? 1 : .45 }} disabled={!canBackup} onClick={props.exportCampaignBackup}>Exportar backup</button>
            <label style={{ ...button, display: 'grid', placeItems: 'center', textAlign: 'center', opacity: canBackup ? 1 : .45, cursor: canBackup ? 'pointer' : 'default' }}>
              Importar backup
              <input type="file" accept="application/json,.json" hidden disabled={!canBackup} onChange={(event) => { const file = event.currentTarget.files?.[0]; if (file) void props.importCampaignBackupFile(file); event.currentTarget.value = ''; }} />
            </label>
            <button type="button" style={{ ...button, opacity: canSnapshots ? 1 : .45 }} disabled={!canSnapshots} onClick={props.createManualSnapshot}>Criar snapshot</button>
            <button type="button" style={{ ...mutedButton, opacity: canSnapshots ? 1 : .45 }} disabled={!canSnapshots} onClick={() => props.refreshSnapshots()}>Listar snapshots</button>
          </div>
          {!canBackup && !canSnapshots && <p style={{ margin: '8px 0 0', color: '#A9B0B5' }}>Backup e snapshots exigem o Motor Barbara R23 ou posterior.</p>}
        </Card>
        {props.snapshots.length > 0 && <Card title="Snapshots">
          <div style={{ display: 'grid', gap: 8 }}>
            {props.snapshots.map((snapshot) => <div key={snapshot.id} style={{ ...softPanel, padding: 10, display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, alignItems: 'center' }}>
              <div><strong>{snapshot.label || 'Snapshot'}</strong><div style={{ marginTop: 3, color: '#A9B0B5', fontSize: 13 }}>{snapshot.createdAt}</div></div>
              <button type="button" style={{ ...mutedButton, minHeight: 38, padding: '6px 10px' }} onClick={() => props.restoreSnapshot(snapshot.id)}>Restaurar</button>
            </div>)}
          </div>
        </Card>}
        <DataActionNotice state={props.dataActionState} message={props.dataActionMessage} />
        <Card title="Validação" tone="warning">Integrações de código Barbara/Gemini/TTS e fallbacks estão ativas e cobertas por testes. Ainda dependem de aparelho, chave ou rede reais: build/instalação Android, áudio/latência Gemini/Charon, OCR e validação dos bridges nativos. A Biblioteca só é marcada disponível quando o bridge Android real existe.</Card>
      </>}
    </div>
  );
}


function renderQaState(props: TemplateProps) {
  if (props.qaState === 'default' || props.qaState === 'demo') return null;
  const copy: Record<string, { title: string; body: string; tone: 'dark' | 'soft' | 'warning' }> = {
    loading: { title: 'Carregando · estado de QA', body: 'Simulação visual de carregamento; não comprova sincronização, rede ou serviço externo real.', tone: 'soft' },
    skeleton: { title: 'Estrutura · estado de QA', body: 'Espaços reservados testam apenas a geometria; nenhum conteúdo ou carregamento real é afirmado.', tone: 'soft' },
    empty: { title: 'Vazio · estado de QA', body: 'Simulação visual de vazio; não afirma ausência real de dados da campanha.', tone: 'dark' },
    error: { title: 'Erro · estado de QA', body: 'Simulação visual de erro; não comprova falha real de carregamento, rede ou serviço.', tone: 'warning' },
    offline: { title: 'Offline · estado de QA', body: 'Este estado apenas testa a apresentação sem conexão; não comprova funcionamento offline real.', tone: 'warning' },
    partial: { title: 'Parcial · estado de QA', body: 'Simulação visual de dados incompletos; campos ausentes permanecem vazios e nenhum sincronismo é presumido.', tone: 'soft' },
  };
  const current = copy[props.qaState] ?? copy.empty;
  return (
    <div style={page}>
      <Header screen={props.screen} qaState={props.qaState} />
      <SiblingNav screen={props.screen} navigate={props.navigate} />
      <Card title={current.title} tone={current.tone}>{current.body}</Card>
    </div>
  );
}


export function renderTemplate(props: TemplateProps) {
  const qa = renderQaState(props);
  if (qa) return qa;
  switch (props.screen.domain) {
    case 'Cena': return <SceneScreen {...props} />;
    case 'Personagem': return <CharacterScreen {...props} />;
    case 'Itens': return <ItemsScreen {...props} />;
    case 'Nave': return <ShipScreen {...props} />;
    case 'Registros': return <RecordsScreen {...props} />;
    case 'Mapa': return <MapScreen {...props} />;
    case 'Criação': return <CreationScreen {...props} />;
    case 'Ajustes': return <SettingsScreen {...props} />;
    default: return <div style={page}><Header screen={props.screen} qaState={props.qaState} /><Card title="Estado">Tela sem template associado.</Card></div>;
  }
}