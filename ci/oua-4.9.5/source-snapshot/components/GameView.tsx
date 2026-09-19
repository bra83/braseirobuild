import React, { useRef, useEffect, useState } from 'react';
import { SceneVisualizer } from './SceneVisualizer';
import { imageTargetFor } from '../lib/sceneImageBinding';
import { AdventureCastAndHexes } from './AdventureCastAndHexes';
import { Controls } from './Controls';
import { EriadorMap, PartyMapMove } from './EriadorMap';
import type { MapDiscovery } from '../lib/map';
import { MasterVirtualPanel } from './MasterVirtualPanel';
import { StoryChoice, PlayerStats } from '../types';
import { User, Eye, EyeOff, Sparkles, MessageSquare, AlertCircle, RefreshCw, Volume2, VolumeX, MapPin, History, Compass, Crown, Smartphone, Upload, ExternalLink, WandSparkles, X } from 'lucide-react';
import { Npc, IndividualToken, getStoreData, SceneNode, Character, PendingRoll, PendingTest } from '../lib/db';
import type { MasterState, RuleAnswer, CompanyState } from '../lib/master/types';
import { ADVENTURE_PROFILES, AdventureProfileId } from '../lib/adventureProfile';
import { TokenPicker } from './TokenPicker';
import { stripOperationalNarrative } from '../lib/narrativeSanitizer';
import { ImmersiveJourneyView } from './immersive/ImmersiveJourneyView';

interface GameViewProps {
  campaignId: string;
  history?: SceneNode[];
  currentNode: SceneNode | null;
  currentImage: string | null;
  isStoryLoading: boolean;
  isImageLoading: boolean;
  isNarrationLoading: boolean;
  storyError: string | null;
  onDismissStoryError?: () => void;
  imageError: string | null;
  narrationError: string | null;
  customAction: string;
  setCustomAction: (action: string) => void;
  onChoice: (choice: StoryChoice) => void;
  onCustomActionSubmit: (e: React.FormEvent, isDmMode: boolean) => void;
  onRegenerateImage: (index?: number) => void;
  onImportSceneImage: (dataUrl: string, index?: number) => void | Promise<void>;
  onStartAdventure: () => void | Promise<void>;
  canStartAdventure: boolean;
  adventureProfile: AdventureProfileId;
  onAdventureProfileChange: (profile: AdventureProfileId) => void;
  imageImportStatus?: string;
  onCreateRandomCharacter: () => void | Promise<void>;
  onPlayNarration: (node?: SceneNode) => void;
  onPlayAndroidNarration: (node?: SceneNode) => void;
  onStopNarration: () => void;
  partyCharacterIds: string[];
  partyCharacters: Character[];
  activeCharacterId?: string | null;
  onActiveCharacterChange?: (characterId: string) => void | Promise<void>;
  onCompanySave: (company: CompanyState) => void | Promise<void>;
  pendingTest?: PendingTest | null;
  pendingRoll?: PendingRoll | null;
  onOpenDice: () => void;
  onResolvePendingRoll: () => void | Promise<void>;
  onNpcTokenChange: (npcId: string, tokenId: string | null) => void | Promise<void>;
  allNpcs: Npc[];
  presentNpcs: Npc[];
  masterState: MasterState;
  ruleAnswer: RuleAnswer | null;
  onCloseRuleAnswer: () => void;
  onPartyMoved: (move: PartyMapMove) => void | Promise<void>;
  onMapDiscoveryChanged: (discovery: MapDiscovery, reason: string) => void | Promise<void>;
  onPartyRested?: () => void | Promise<void>;
  immersiveMode?: boolean;
  onNavigate?: (tab: 'jogo' | 'combate' | 'ficha' | 'dados' | 'tokens' | 'campanha' | 'configuracoes') => void;
}

function splitNarrativeParagraphs(value: string): string[] {
  const normalized = String(value || '').replace(/\r/g, '').trim();
  if (!normalized) return [];
  const explicit = normalized.split(/\n\s*\n+/).map(item => item.replace(/\n+/g, ' ').trim()).filter(Boolean);
  if (explicit.length > 1) return explicit;
  const sentences = normalized.match(/[^.!?…]+[.!?…]+(?:[”"']+)?|[^.!?…]+$/g)?.map(item => item.trim()).filter(Boolean) || [normalized];
  const paragraphs: string[] = [];
  let current = '';
  for (const sentence of sentences) {
    const next = current ? `${current} ${sentence}` : sentence;
    if (current && (next.length > 390 || current.split(/(?<=[.!?…])\s+/).length >= 3)) {
      paragraphs.push(current.trim());
      current = sentence;
    } else current = next;
  }
  if (current.trim()) paragraphs.push(current.trim());
  return paragraphs;
}

function isSoloModeSource(source: { manualName?: string; excerpt?: string; usage?: string }): boolean {
  const text = `${source.manualName || ''} ${source.excerpt || ''} ${source.usage || ''}`
    .normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  return /modo peregrino|jornada solo|aventura solo|heroi jogador solitario|herói jogador solitário|jogar sozinho|regras solo/.test(text);
}

export const GameView: React.FC<GameViewProps> = ({
  campaignId,
  history = [],
  currentNode,
  currentImage,
  isStoryLoading,
  isImageLoading,
  isNarrationLoading,
  storyError,
  onDismissStoryError,
  imageError,
  narrationError,
  customAction,
  setCustomAction,
  onChoice,
  onCustomActionSubmit,
  onRegenerateImage,
  onImportSceneImage,
  onStartAdventure,
  canStartAdventure,
  adventureProfile,
  onAdventureProfileChange,
  imageImportStatus,
  onCreateRandomCharacter,
  onPlayNarration,
  onPlayAndroidNarration,
  onStopNarration,
  partyCharacterIds,
  partyCharacters,
  activeCharacterId,
  onActiveCharacterChange,
  onCompanySave,
  pendingTest,
  pendingRoll,
  onOpenDice,
  onResolvePendingRoll,
  onNpcTokenChange,
  allNpcs,
  presentNpcs,
  masterState,
  ruleAnswer,
  onCloseRuleAnswer,
  onPartyMoved,
  onMapDiscoveryChanged,
  onPartyRested,
  immersiveMode = false,
  onNavigate,
}) => {
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [selectedNpc, setSelectedNpc] = useState<Npc | null>(null);
  const [isNpcTokenPickerOpen, setIsNpcTokenPickerOpen] = useState(false);
  const [npcTokenStatus, setNpcTokenStatus] = useState('');
  const [isAskDmMode, setIsAskDmMode] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const [imageShareMessage, setImageShareMessage] = useState('');
  const [showSources, setShowSources] = useState(false);
  const sourceOpenButtonRef = useRef<HTMLButtonElement>(null);
  const sourceDialogRef = useRef<HTMLElement>(null);
  const sourcePreviousFocusRef = useRef<HTMLElement | null>(null);

  const [selectedHistoryIndex, setSelectedHistoryIndex] = useState<number | null>(null);

  // Auto reset selection to latest node on history expansion
  useEffect(() => {
    setSelectedHistoryIndex(null);
  }, [history.length]);

  const isViewingHistory = selectedHistoryIndex !== null;
  const activeNode = isViewingHistory ? history[selectedHistoryIndex!] : currentNode;
  const visibleNarration = activeNode ? stripOperationalNarrative(activeNode.atmospheric_narration || '') : '';
  const visibleActionResult = activeNode ? stripOperationalNarrative(activeNode.action_result || '') : '';
  const actionResultLabel = activeNode?.narrativeMeta?.advancesFiction === false ? 'Esclarecimento do Mestre' : 'Consequência';
  const activeImage = isViewingHistory ? (history[selectedHistoryIndex!]?.imageData || null) : currentImage;
  const visibleSources = (activeNode?.sources || []).filter(source => !isSoloModeSource(source));
  const activePresentNpcs = isViewingHistory 
    ? (activeNode?.presentNpcIds ? allNpcs.filter(n => activeNode.presentNpcIds.includes(n.id)) : [])
    : presentNpcs;
  const isViewingPast = isViewingHistory && selectedHistoryIndex! < history.length - 1;
  const pendingTestCharacter = pendingTest
    ? partyCharacters.find(character => character.id === pendingTest.characterId)
      || partyCharacters.find(character => character.id === activeCharacterId)
      || partyCharacters[0]
    : null;
  const pendingRollModeLabel = pendingTest?.rollMode === 'favoured'
    ? 'Favorecida'
    : pendingTest?.rollMode === 'ill-favoured'
      ? 'Desfavorecida'
      : 'Normal';
  const pendingRollSuccessLabel = pendingRoll?.successLevel === 'extraordinary-success'
    ? 'Sucesso extraordinário'
    : pendingRoll?.successLevel === 'great-success'
      ? 'Grande sucesso'
      : pendingRoll?.success
        ? 'Sucesso'
        : 'Falha';
  const pendingRollDiceLabel = pendingRoll
    ? [
        pendingRoll.featResults?.length ? `Façanha: ${pendingRoll.featResults.join(' / ')}` : pendingRoll.featResult != null ? `Façanha: ${pendingRoll.featResult}` : '',
        pendingRoll.successResults?.length ? `Sucesso: ${pendingRoll.successResults.join(', ')}` : pendingRoll.results?.length ? `Dados: ${pendingRoll.results.join(', ')}` : '',
        `Total ${pendingRoll.total}`,
      ].filter(Boolean).join(' · ')
    : '';

  useEffect(() => {
    setShowSources(false);
  }, [activeNode?.id]);

  useEffect(() => {
    if (!showSources) return;
    const previousOverflow = document.body.style.overflow;
    const historyMarker = `source-help-${Date.now()}`;
    let closedByHistory = false;
    document.body.style.overflow = 'hidden';
    window.history.pushState({ ...(window.history.state || {}), braseiroModal: historyMarker }, '');
    sourcePreviousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : sourceOpenButtonRef.current;
    window.requestAnimationFrame(() => {
      const dialog = sourceDialogRef.current;
      const first = dialog?.querySelector<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
      (first || dialog)?.focus();
    });
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setShowSources(false);
      if (event.key !== 'Tab') return;
      const dialog = sourceDialogRef.current;
      if (!dialog) return;
      const focusable: HTMLElement[] = [...dialog.querySelectorAll<HTMLElement>('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')]
        .filter(element => element.offsetParent !== null);
      if (!focusable.length) {
        event.preventDefault();
        dialog.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    const closeOnAndroidBack = () => {
      closedByHistory = true;
      setShowSources(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    window.addEventListener('popstate', closeOnAndroidBack);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', closeOnEscape);
      window.removeEventListener('popstate', closeOnAndroidBack);
      if (!closedByHistory && window.history.state?.braseiroModal === historyMarker) window.history.back();
      window.requestAnimationFrame(() => (sourcePreviousFocusRef.current || sourceOpenButtonRef.current)?.focus());
    };
  }, [showSources]);


  // Load only the token store used by the modern importer and always revoke old object URLs.
  useEffect(() => {
    if (immersiveMode && !isViewingHistory) {
      setTokens({});
      return;
    }
    let disposed = false;
    const createdUrls: string[] = [];
    const fetchTokens = async () => {
      try {
        const storedTokens = await getStoreData<IndividualToken>('individual_tokens');
        const tokenMap: Record<string, string> = {};
        storedTokens.forEach(token => {
          const url = URL.createObjectURL(token.blob);
          createdUrls.push(url);
          tokenMap[token.id] = url;
        });
        if (!disposed) setTokens(tokenMap);
      } catch (error) {
        console.error('BRASEIRO_TOKENS: falha ao carregar tokens da cena', error);
      }
    };
    void fetchTokens();
    return () => {
      disposed = true;
      createdUrls.forEach(url => URL.revokeObjectURL(url));
    };
  }, [immersiveMode, isViewingHistory, activePresentNpcs.map(npc => npc.tokenId).join('|')]);

  const sceneImagePrompt = activeNode?.sceneVisualPrompt || activeNode?.visualPrompt || '';

  const sendPromptToGemini = async () => {
    if (!sceneImagePrompt) return;
    const prompt = `Crie uma imagem vertical 9:16 para esta cena de O Um Anel, em pintura fantástica medieval, sem texto nem interface:\n\n${sceneImagePrompt}`;
    try {
      if (activeNode?.id) {
        localStorage.setItem('braseiro.pendingSharedImageTarget', JSON.stringify(imageTargetFor(activeNode, campaignId)));
      }
      if (window.BraseiroAndroid?.shareText) {
        await window.BraseiroAndroid.shareText('Imagem da cena — O Um Anel', prompt);
        setImageShareMessage('Prompt enviado. Gere a imagem no Gemini e compartilhe-a para O Um Anel; ela será colocada automaticamente nesta cena.');
      } else if (navigator.share) {
        await navigator.share({ title: 'Imagem da cena — O Um Anel', text: prompt });
        setImageShareMessage('Prompt enviado. Gere a imagem e compartilhe-a para O Um Anel.');
      } else {
        await navigator.clipboard.writeText(prompt);
        setImageShareMessage('Prompt copiado. Abra o Gemini, gere a imagem e compartilhe-a para O Um Anel.');
      }
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError') {
        await navigator.clipboard.writeText(prompt).catch(() => undefined);
        setImageShareMessage('Não foi possível abrir o compartilhamento. O prompt foi copiado.');
      }
    }
  };

  const importGeneratedImage = async (file?: File) => {
    if (!file || !file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onload = async () => {
      if (typeof reader.result !== 'string') return;
      await onImportSceneImage(reader.result, selectedHistoryIndex !== null ? selectedHistoryIndex : history.length - 1);
      setImageShareMessage('Imagem importada e ligada à cena.');
    };
    reader.readAsDataURL(file);
  };

  // Submit on Enter, Newline on Shift+Enter
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      const event = new Event('submit', { cancelable: true }) as any;
      onCustomActionSubmit(event, isAskDmMode);
    }
  };

  if (immersiveMode && !isViewingHistory) {
    return <ImmersiveJourneyView
      campaignId={campaignId}
      currentNode={currentNode}
      currentImage={currentImage}
      characters={partyCharacters}
      masterState={masterState}
      customAction={customAction}
      setCustomAction={setCustomAction}
      onChoice={onChoice}
      onCustomActionSubmit={onCustomActionSubmit}
      onPartyMoved={onPartyMoved}
      onMapDiscoveryChanged={onMapDiscoveryChanged}
      onPartyRested={onPartyRested}
      onOpenDice={onOpenDice}
      onStartAdventure={onStartAdventure}
      canStartAdventure={canStartAdventure}
      isStoryLoading={isStoryLoading}
      onNavigate={onNavigate}
    />;
  }

  return (
    <div className="w-full max-w-5xl min-w-0 mx-auto grid grid-cols-1 md:grid-cols-12 gap-6 font-serif overflow-x-hidden">
      
      {/* Coluna Principal da Cena */}
      <div className="md:col-span-8 min-w-0 flex flex-col gap-6">
        
        {/* Banner de Aviso de Lembrança */}
        {isViewingPast && (
          <div className="bg-blue-950/30 border-2 border-blue-600/60 rounded p-3 flex flex-wrap justify-between items-center gap-3 animate-fadeIn">
            <div className="flex items-center gap-2">
              <History className="w-4 h-4 text-blue-500 shrink-0" />
              <div className="text-left">
                <p className="text-xs font-bold text-blue-400 font-serif uppercase tracking-wider">
                  Visualizando Lembrança
                </p>
                <p className="text-[10px] text-blue-200/85 leading-snug">
                  Você está revisitando crônicas passadas de sua jornada. Retorne ao presente para fazer novas escolhas.
                </p>
              </div>
            </div>
            <button
              onClick={() => setSelectedHistoryIndex(null)}
              className="px-3 py-1 bg-blue-600 hover:bg-blue-500 text-zinc-950 font-bold text-[10px] uppercase font-serif rounded cursor-pointer transition-colors shadow-md shrink-0 animate-pulse"
            >
              Retornar ao Presente
            </button>
          </div>
        )}

        {/* 1. Cabeçalho da Cena */}
        <div key={selectedHistoryIndex} className="border-b border-blue-900/20 pb-1.5 flex flex-wrap items-center justify-between bg-zinc-950/15 px-2.5 py-1 rounded gap-x-2 gap-y-0.5">
          <span className="text-[8px] sm:text-[9px] text-blue-700/80 font-serif font-bold tracking-[0.15em] uppercase">
            Crônica de Eriador
          </span>
          <div className="text-[9px] sm:text-[11px] font-black text-blue-500 tracking-wider uppercase font-serif text-glow flex items-center gap-1 flex-wrap justify-end">
            <span className="text-blue-100/90 font-bold">{activeNode?.location || 'Nova crônica'}</span>
          </div>
        </div>

        <div className="flex min-w-0 flex-wrap items-center justify-between gap-2 rounded border border-blue-950/60 bg-black/25 px-3 py-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-blue-600">Estilo da aventura</span>
          <select value={adventureProfile} onChange={event => onAdventureProfileChange(event.target.value as AdventureProfileId)} className="min-w-0 max-w-full rounded border border-blue-800 bg-zinc-950 px-3 py-2 text-xs font-bold text-blue-100">
            {ADVENTURE_PROFILES.map(profile => <option key={profile.id} value={profile.id}>{profile.label}</option>)}
          </select>
        </div>

        <AdventureCastAndHexes
          campaignId={campaignId}
          npcs={activePresentNpcs}
          history={history}
          currentNode={activeNode}
          partyCharacterIds={partyCharacterIds}
          onNpcSelect={setSelectedNpc}
          onOpenAtlas={() => document.getElementById('atlas-terra-media')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
        />

        <MasterVirtualPanel
          masterState={masterState}
          partySize={partyCharacterIds.length}
          partyCharacters={partyCharacters}
          presentNpcs={activePresentNpcs}
          activeCharacterId={activeCharacterId}
          onActiveCharacterChange={onActiveCharacterChange}
          onCompanySave={onCompanySave}
          ruleAnswer={ruleAnswer}
          onCloseRuleAnswer={onCloseRuleAnswer}
        />

        {/* 3. Narração */}
        <div key={`narration-${selectedHistoryIndex}`} className="bg-zinc-950/80 border-2 border-blue-900 rounded-md p-4 md:p-6 shadow-lg relative ambient-glow flex flex-col min-h-[140px]">
          {/* Subtle glowing paper details */}
          <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60 pointer-events-none"></div>
          <div className="absolute top-2 right-2 w-3 h-3 border-r-2 border-t-2 border-blue-700/60 pointer-events-none"></div>
          {storyError && (
            <div className="mb-4 rounded border border-red-800/80 bg-red-950/30 px-4 py-3 text-center text-red-300 font-serif" role="alert">
              <AlertCircle className="mx-auto mb-2 h-7 w-7 text-red-500" />
              <p className="text-sm font-bold tracking-wide">{storyError}</p>
              <p className="mt-2 text-xs italic text-red-200/70">
                A cena visível e qualquer resultado de rolagem abaixo continuam preservados.
              </p>
              {onDismissStoryError && <button type="button" onClick={onDismissStoryError} className="mt-3 rounded border border-red-700 px-4 py-2 text-xs font-bold text-red-100">Fechar aviso</button>}
            </div>
          )}
          {isStoryLoading ? (
            <div className="flex-1 flex flex-col items-center justify-center py-6 text-center">
              <div className="w-10 h-10 border-4 border-blue-950 border-t-blue-500 rounded-full animate-spin mb-3"></div>
              <p className="text-blue-500 text-xs font-serif tracking-widest animate-pulse font-bold">
                ESCREVENDO OS DIÁRIOS DE JORNADA...
              </p>
            </div>
          ) : (
            <div className="flex-1 flex flex-col gap-4">
              {visibleNarration || visibleActionResult ? (
                <>
                  {/* Texto da Atmosfera Imersiva */}
                  {visibleNarration && (
                    <div className="min-w-0 space-y-3 text-blue-100/95 text-sm md:text-base leading-7">
                      {splitNarrativeParagraphs(visibleNarration).map((paragraph, idx) => (
                        <p key={`atm-${idx}`} className="rounded-md border-l-2 border-blue-700/60 bg-blue-950/10 px-3 py-3 break-words [overflow-wrap:anywhere]">
                          {paragraph}
                        </p>
                      ))}
                    </div>
                  )}

                  {/* Linha Divisória Sutil */}
                  {visibleNarration && visibleActionResult && (
                    <div className="w-full flex justify-center py-2">
                      <div className="w-1/3 h-px bg-blue-900/50 rounded-full"></div>
                    </div>
                  )}

                  {/* Resultado da Ação e Fatos Mecânicos */}
                  {visibleActionResult && (
                    <div className="min-w-0 rounded-md border border-cyan-900/50 bg-cyan-950/10 p-3 text-blue-200/95 text-sm md:text-base leading-7 font-medium">
                      <span className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-cyan-500">{actionResultLabel}</span>
                      {splitNarrativeParagraphs(visibleActionResult).map((paragraph, idx) => (
                        <p key={`act-${idx}`} className="mb-2 last:mb-0 break-words [overflow-wrap:anywhere]">{paragraph}</p>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div className="mx-auto max-w-xl py-7 text-center">
                  <Compass className="mx-auto mb-3 h-8 w-8 text-blue-400/80" />
                  <h2 className="font-serif text-sm font-bold tracking-wider text-blue-200">Uma nova crônica aguarda</h2>
                  <p className="mt-2 text-sm leading-relaxed text-slate-400">Escolha o estilo da crônica. A aventura só começa depois que a Companhia tiver um hex definido no Atlas.</p>
                  <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {ADVENTURE_PROFILES.map(profile => (
                      <button key={profile.id} type="button" onClick={() => onAdventureProfileChange(profile.id)} className={`min-w-0 rounded border p-3 text-left ${adventureProfile === profile.id ? 'border-cyan-500 bg-cyan-950/35 text-cyan-100' : 'border-blue-950 bg-black/25 text-slate-300'}`}>
                        <span className="block text-xs font-bold">{profile.label}</span>
                        <span className="mt-1 block text-[11px] leading-relaxed text-slate-400">{profile.description}</span>
                      </button>
                    ))}
                  </div>
                  {!canStartAdventure && <p className="mt-4 rounded border border-amber-800/60 bg-amber-950/20 p-3 text-xs leading-relaxed text-amber-200">Defina primeiro a posição da Companhia no Atlas. O Mestre não inventará um hex inicial.</p>}
                  <div className="mt-5 flex flex-col justify-center gap-2 sm:flex-row">
                    <button disabled={!canStartAdventure} onClick={() => void onStartAdventure()} className="flex items-center justify-center gap-2 rounded border border-cyan-600 bg-cyan-950/35 px-4 py-3 text-sm font-bold text-cyan-100 disabled:cursor-not-allowed disabled:opacity-35"><Compass className="h-4 w-4" /> Começar aventura</button>
                    {!canStartAdventure && <button type="button" onClick={() => document.getElementById('atlas-terra-media')?.scrollIntoView({ behavior: 'smooth', block: 'start' })} className="flex items-center justify-center gap-2 rounded border border-amber-700 bg-amber-950/25 px-4 py-3 text-sm font-bold text-amber-100"><MapPin className="h-4 w-4" /> Abrir Atlas</button>}
                    <button onClick={() => void onCreateRandomCharacter()} className="flex items-center justify-center gap-2 rounded border border-blue-700 bg-blue-950/30 px-4 py-3 text-sm font-bold text-blue-100"><User className="h-4 w-4" /> Criar outro herói</button>
                  </div>
                </div>
              )}

              {activeNode && (
                <div className="flex justify-end border-t border-blue-950/30 pt-3">
                  <button ref={sourceOpenButtonRef} type="button" onClick={() => { sourcePreviousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : sourceOpenButtonRef.current; setShowSources(true); }} className="inline-flex items-center gap-2 rounded border border-blue-800 bg-blue-950/25 px-3 py-2 text-xs font-bold text-blue-200">
                    <span className="flex h-5 w-5 items-center justify-center rounded-full border border-blue-600 text-[11px] font-black">i</span>
                    Fontes e regras
                  </button>
                </div>
              )}

              {/* Controles explícitos de voz: Charon, Android e parar. */}
              {visibleNarration && (
                <div className="mt-auto border-t border-blue-950/40 pt-4">
                  <span className="mb-2 block text-[10px] font-semibold uppercase tracking-wider text-blue-700">
                    Mesa sonora e narração
                  </span>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                    <button
                      onClick={() => onPlayNarration(activeNode)}
                      disabled={isStoryLoading}
                      className="flex min-h-11 items-center justify-center gap-2 rounded border border-blue-700 bg-blue-950/30 px-3 py-2 text-xs font-bold text-blue-100 disabled:opacity-40"
                    >
                      <Volume2 className={`h-4 w-4 ${isNarrationLoading ? 'animate-pulse' : ''}`} /> Ouvir com Charon
                    </button>
                    <button
                      onClick={() => onPlayAndroidNarration(activeNode)}
                      disabled={isStoryLoading}
                      className="flex min-h-11 items-center justify-center gap-2 rounded border border-cyan-700 bg-cyan-950/25 px-3 py-2 text-xs font-bold text-cyan-100 disabled:opacity-40"
                    >
                      <Smartphone className="h-4 w-4" /> Ouvir com voz Android
                    </button>
                    <button
                      onClick={onStopNarration}
                      className="flex min-h-11 items-center justify-center gap-2 rounded border border-red-900 bg-red-950/20 px-3 py-2 text-xs font-bold text-red-200"
                    >
                      <VolumeX className="h-4 w-4" /> Parar narração
                    </button>
                  </div>
                  <p className="mt-2 text-[11px] leading-relaxed text-slate-500">A voz, a música, a ambiência e os efeitos só começam quando você toca em Ouvir. A cena pode ser reproduzida novamente quantas vezes desejar.</p>
                </div>
              )}
            </div>
          )}
        </div>

        {!isViewingPast && pendingTest && (
          <section id="pending-action-test" className="scroll-mt-40 rounded-md border-2 border-cyan-600/80 bg-cyan-950/20 p-4 shadow-[0_0_24px_rgba(8,145,178,0.12)]" aria-live="polite">
            <div className="flex items-start gap-3">
              <div className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-cyan-500 bg-black/35">
                <AlertCircle className="h-5 w-5 text-cyan-300" />
              </div>
              <div className="min-w-0 flex-1">
                <span className="text-[10px] font-black uppercase tracking-[0.18em] text-cyan-400">{pendingRoll ? 'Resultado pronto — a consequência ainda não foi aplicada' : 'A ação parou antes da consequência'}</span>
                <h3 className="mt-1 break-words font-serif text-base font-bold text-cyan-100">Teste de {pendingTest.skill}</h3>
                <div className="mt-3 rounded border border-cyan-900/60 bg-black/25 p-3">
                  <span className="block text-[10px] font-bold uppercase tracking-wider text-slate-500">Ação que provocou o teste</span>
                  <p className="mt-1 break-words text-sm leading-relaxed text-slate-100">{pendingTest.originalChoice}</p>
                </div>
                <dl className="mt-3 grid grid-cols-1 gap-2 text-xs sm:grid-cols-2">
                  <div className="rounded border border-slate-800 bg-black/20 p-2"><dt className="text-[9px] font-bold uppercase text-slate-500">Quem rola</dt><dd className="mt-1 font-bold text-slate-100">{pendingTestCharacter?.sheet.identity.name || pendingTestCharacter?.name || 'Herói ativo'}</dd></div>
                  <div className="rounded border border-slate-800 bg-black/20 p-2"><dt className="text-[9px] font-bold uppercase text-slate-500">Rolagem</dt><dd className="mt-1 font-bold text-slate-100">{pendingTest.skill}{pendingTest.targetNumber ? ` · NA ${pendingTest.targetNumber}` : ''} · {pendingRollModeLabel}</dd></div>
                </dl>
                <div className="mt-3 rounded border border-blue-900/60 bg-blue-950/15 p-3 text-xs leading-relaxed text-blue-100">
                  <strong className="text-cyan-300">Por que rolar:</strong> {pendingTest.reason}
                  {pendingTest.consequences && <p className="mt-2"><strong className="text-amber-300">O que está em risco:</strong> {pendingTest.consequences}</p>}
                </div>
                {pendingRoll ? (
                  <div className={`mt-3 rounded border p-3 ${pendingRoll.success ? 'border-emerald-600/70 bg-emerald-950/25' : 'border-red-700/70 bg-red-950/25'}`}>
                    <span className="block text-[10px] font-black uppercase tracking-[0.18em] text-slate-400">Resultado da rolagem</span>
                    <strong className={`mt-1 block font-serif text-lg ${pendingRoll.success ? 'text-emerald-300' : 'text-red-300'}`}>{pendingRollSuccessLabel}</strong>
                    <p className="mt-1 break-words text-xs leading-relaxed text-slate-200">{pendingRollDiceLabel}</p>
                    <p className="mt-2 text-[11px] leading-relaxed text-slate-400">O Mestre ainda não avançou a cena. Confira o resultado e toque em continuar para aplicar a consequência uma única vez.</p>
                    <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                      <button type="button" onClick={() => void onResolvePendingRoll()} disabled={isStoryLoading} className="flex min-h-11 items-center justify-center gap-2 rounded border border-emerald-500 bg-emerald-950/35 px-4 py-3 text-sm font-bold text-emerald-100 disabled:opacity-40">
                        {isStoryLoading ? 'Aplicando resultado…' : 'Continuar com este resultado'}
                      </button>
                      <button type="button" onClick={onOpenDice} disabled={isStoryLoading} className="flex min-h-11 items-center justify-center gap-2 rounded border border-slate-600 bg-black/25 px-4 py-3 text-sm font-bold text-slate-200 disabled:opacity-40">
                        Ver tela de dados
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <p className="mt-3 text-[11px] leading-relaxed text-slate-400">Nada foi resolvido ainda. A tela de dados só será aberta quando você tocar no botão abaixo.</p>
                    <button type="button" onClick={onOpenDice} className="mt-3 flex min-h-11 w-full items-center justify-center gap-2 rounded border border-cyan-500 bg-cyan-950/45 px-4 py-3 text-sm font-bold text-cyan-100 sm:w-auto">
                      Abrir dados e fazer este teste
                    </button>
                  </>
                )}
              </div>
            </div>
          </section>
        )}

        {showSources && activeNode && (
          <div
            className="source-help-overlay"
            role="presentation"
            onMouseDown={() => setShowSources(false)}
          >
            <section
              ref={sourceDialogRef}
              className="source-help-dialog"
              role="dialog"
              tabIndex={-1}
              aria-modal="true"
              aria-labelledby="source-help-title"
              aria-describedby="source-help-description"
              onMouseDown={event => event.stopPropagation()}
            >
              <header className="source-help-header">
                <div className="min-w-0">
                  <h3 id="source-help-title" className="flex min-w-0 items-center gap-2 font-serif text-base font-bold text-blue-200 sm:text-lg">
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-blue-500 text-sm font-black">i</span>
                    <span className="break-words">Fontes e regras consultadas</span>
                  </h3>
                  <p id="source-help-description" className="mt-1 text-xs leading-relaxed text-slate-400">
                    {visibleSources.length
                      ? `${visibleSources.length} ${visibleSources.length === 1 ? 'referência consultada' : 'referências consultadas'} nesta cena. O texto de cada fonte indica se ela apenas orientou a resolução ou se sustentou uma consequência mecânica.`
                      : 'Esta cena antiga não possui consulta de regras registrada. Cenas novas não avançam sem a busca obrigatória nos manuais indexados.'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowSources(false)}
                  className="source-help-close"
                  aria-label="Fechar fontes e regras"
                >
                  <X className="h-5 w-5" />
                  <span>Fechar</span>
                </button>
              </header>

              <div className="source-help-status" aria-live="polite">
                <span>{visibleSources.length ? `Consulta: ${visibleSources.length} ${visibleSources.length === 1 ? 'fonte' : 'fontes'}` : 'Consulta ausente'}</span>
                <span>{visibleSources.length ? 'Deslize para ler' : 'Sem texto escondido'}</span>
              </div>

              <div className="source-help-body" tabIndex={0}>
                {visibleSources.length ? (
                  <div className="space-y-4">
                    {visibleSources.map((source, index) => (
                      <article key={`${source.manualId}_${source.pageNum}_${index}`} className="min-w-0 overflow-hidden rounded border border-blue-950 bg-slate-950/75 p-3 sm:p-4">
                        <div className="flex flex-wrap items-baseline justify-between gap-2">
                          <h4 className="min-w-0 break-words font-serif font-bold text-blue-100 [overflow-wrap:anywhere]">{source.manualName}</h4>
                          <span className="rounded bg-blue-950/70 px-2 py-1 text-[11px] font-bold uppercase tracking-wide text-cyan-200">{source.pageLabel && source.pageLabel !== String(source.pageNum) ? `Página ${source.pageLabel} · PDF ${source.physicalPageNum || source.pageNum}` : `Página ${source.pageNum}`}</span>
                        </div>
                        {source.usage && <p className="mt-3 text-sm font-semibold text-blue-300">Regra utilizada: {source.usage}</p>}
                        <div className="mt-3 min-w-0 space-y-2 border-l-2 border-blue-700 pl-3 text-sm leading-6 text-slate-300">{splitNarrativeParagraphs(source.excerpt).map((paragraph, paragraphIndex) => <p key={paragraphIndex} className="break-words [overflow-wrap:anywhere]">{paragraph}</p>)}</div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="rounded border border-amber-800/70 bg-amber-950/25 p-4 text-sm leading-7 text-amber-50">
                    <p className="font-bold text-amber-200">Esta cena foi criada antes da consulta obrigatória de regras.</p>
                    <p className="mt-2">Nas cenas novas, o Mestre só prossegue depois de recuperar trechos do perfil O Um Anel 2e; consequências mecânicas sem evidência compatível são bloqueadas.</p>
                  </div>
                )}
              </div>

              <footer className="source-help-footer">
                <span className="text-[11px] leading-relaxed text-slate-500">Feche pelo botão acima, pelo botão abaixo, tocando fora da caixa ou pelo Voltar do Android.</span>
                <button type="button" onClick={() => setShowSources(false)} className="source-help-footer-button">
                  Fechar janela
                </button>
              </footer>
            </section>
          </div>
        )}

        {/* NPC Quick Sheet Modal Popup */}
        {selectedNpc && (
          <div className="fixed inset-0 bg-black/75 flex items-center justify-center p-4 z-50 animate-fadeIn">
            <div className="w-full max-w-md bg-zinc-950 border-2 border-blue-900 rounded-lg p-5 shadow-[0_0_40px_rgba(217,119,6,0.2)]">
              <div className="flex justify-between items-start border-b border-blue-900/40 pb-2 mb-3">
                <div>
                  <h3 className="text-base font-bold text-blue-400 font-serif">{selectedNpc.name}</h3>
                  <p className="text-[10px] text-blue-700 uppercase font-serif font-bold">
                    {selectedNpc.culture || 'Cultura desconhecida'} // {selectedNpc.role || 'Papel'}
                  </p>
                </div>
                <button
                  onClick={() => setSelectedNpc(null)}
                  className="text-blue-800 hover:text-blue-400 cursor-pointer"
                >
                  X
                </button>
              </div>
              <button
                type="button"
                onClick={() => { setNpcTokenStatus(''); setIsNpcTokenPickerOpen(true); }}
                className="mb-4 flex w-full items-center gap-3 rounded border border-violet-800/70 bg-violet-950/20 p-3 text-left hover:border-violet-500"
                title="Trocar o retrato deste NPC"
              >
                <span className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-violet-600 bg-black">
                  {selectedNpc.tokenId && tokens[selectedNpc.tokenId]
                    ? <img src={tokens[selectedNpc.tokenId]} alt={selectedNpc.name} className="h-full w-full object-cover" />
                    : <User className="h-8 w-8 text-violet-300" />}
                </span>
                <span className="min-w-0">
                  <strong className="block text-sm text-violet-100">Trocar retrato/token</strong>
                  <span className="mt-1 block text-[10px] leading-relaxed text-violet-300">Toque aqui para escolher outra imagem da biblioteca. A escolha será salva para este NPC.</span>
                </span>
              </button>
              {npcTokenStatus && <p className="mb-3 rounded border border-emerald-900/60 bg-emerald-950/20 p-2 text-[10px] text-emerald-200">{npcTokenStatus}</p>}
              <div className="space-y-2.5 text-xs text-blue-100/90 leading-relaxed pr-1 max-h-[50vh] overflow-y-auto scrollbar-thin">
                <p><strong>Aparência:</strong> {selectedNpc.appearance || 'Não descrita.'}</p>
                <p><strong>Impressão percebida:</strong> {selectedNpc.attitude || 'Neutra.'}</p>
                <p><strong>Estado visível:</strong> {selectedNpc.state || 'Normal.'}</p>
                <p className="text-[10px] italic text-slate-500">Objetivos, segredos e notas do Historiador permanecem ocultos nesta tela.</p>
                <div className="flex justify-between items-center bg-blue-950/10 border border-blue-900/30 p-2 rounded text-[10px]">
                  <span className="text-blue-700 font-bold uppercase font-serif">Confiança com o herói</span>
                  <span className="text-blue-400 font-bold">{selectedNpc.trust}%</span>
                </div>
              </div>
              <div className="text-right mt-4 pt-2 border-t border-blue-900/30">
                <button
                  onClick={() => setSelectedNpc(null)}
                  className="px-4 py-1 bg-blue-950/20 border border-blue-900 hover:bg-blue-600/20 text-blue-400 font-bold text-[10px] uppercase font-serif rounded cursor-pointer"
                >
                  Fechar
                </button>
              </div>
            </div>
          </div>
        )}

        <TokenPicker
          isOpen={isNpcTokenPickerOpen && Boolean(selectedNpc)}
          selectedTokenId={selectedNpc?.tokenId || null}
          onClose={() => setIsNpcTokenPickerOpen(false)}
          onSelect={tokenId => {
            const npc = selectedNpc;
            if (!npc) return;
            void Promise.resolve(onNpcTokenChange(npc.id, tokenId)).then(() => {
              setSelectedNpc({ ...npc, tokenId, tokenAssignmentMode: 'manual', updatedAt: Date.now() });
              setNpcTokenStatus(tokenId ? 'Retrato substituído e salvo.' : 'Retrato removido e salvo.');
              setIsNpcTokenPickerOpen(false);
            }).catch(error => setNpcTokenStatus(error instanceof Error ? error.message : 'Não foi possível substituir o retrato.'));
          }}
        />

        {/* 5. Escolhas Sugeridas */}
        {!isViewingPast && !isStoryLoading && activeNode?.choices && activeNode.choices.length > 0 && (
          <details className="rounded border border-blue-900/50 bg-zinc-950/45 p-3">
            <summary className="cursor-pointer text-[9px] font-bold uppercase tracking-widest text-blue-600">
              Sugestões opcionais do Mestre — a ação livre continua principal
            </summary>
            <div className="mt-3">
              <Controls choices={activeNode.choices} onChoose={onChoice} disabled={isStoryLoading} />
            </div>
          </details>
        )}

        {/* Imagem da cena — sempre abaixo das escolhas, com ações visíveis. */}
        <div key={`img-${selectedHistoryIndex}`} className="flex flex-col gap-3 rounded-md border border-blue-900/50 bg-zinc-950/60 p-3">
          <SceneVisualizer imageData={activeImage} isLoading={!isViewingPast && isImageLoading} />
          {!isViewingPast && (
            <>
              {imageError && (
                <div className="flex items-center gap-2 rounded border border-red-900/60 bg-red-950/20 p-2 text-xs text-red-300">
                  <AlertCircle className="h-4 w-4" /> {imageError}
                </div>
              )}
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                <button
                  onClick={() => onRegenerateImage(selectedHistoryIndex !== null ? selectedHistoryIndex : history.length - 1)}
                  disabled={isImageLoading || !activeNode}
                  className="flex min-h-11 items-center justify-center gap-2 rounded border border-blue-700 bg-blue-950/30 px-3 py-2 text-xs font-bold text-blue-100 disabled:opacity-40"
                >
                  {isImageLoading ? <RefreshCw className="h-4 w-4 animate-spin" /> : <WandSparkles className="h-4 w-4" />}
                  {isImageLoading ? 'Gerando...' : 'Gerar no app'}
                </button>
                <button
                  onClick={() => void sendPromptToGemini()}
                  disabled={!sceneImagePrompt}
                  className="flex min-h-11 items-center justify-center gap-2 rounded border border-violet-700 bg-violet-950/25 px-3 py-2 text-xs font-bold text-violet-100 disabled:opacity-40"
                >
                  <ExternalLink className="h-4 w-4" /> Abrir Gemini com prompt
                </button>
                <button
                  onClick={() => imageInputRef.current?.click()}
                  className="flex min-h-11 items-center justify-center gap-2 rounded border border-cyan-700 bg-cyan-950/25 px-3 py-2 text-xs font-bold text-cyan-100"
                >
                  <Upload className="h-4 w-4" /> Escolher imagem
                </button>
              </div>
              <input ref={imageInputRef} type="file" accept="image/*" className="hidden" onChange={event => { void importGeneratedImage(event.target.files?.[0]); event.currentTarget.value = ''; }} />
              <p className="text-[11px] leading-relaxed text-slate-400">
                No Gemini, gere a imagem e use Compartilhar → O Um Anel. O app recebe a imagem e a liga automaticamente à cena atual.
              </p>
              {(imageShareMessage || imageImportStatus) && <p className="rounded bg-black/40 p-2 text-[11px] text-cyan-200">{imageImportStatus || imageShareMessage}</p>}
            </>
          )}
        </div>

        {/* Se estiver no passado, mostrar aviso elegante de impossibilidade de ações */}
        {isViewingPast && (
          <div className="p-4 bg-zinc-950/40 border border-blue-900/30 rounded text-center text-xs text-blue-500/70 italic font-serif animate-fadeIn">
            Os caminhos do destino já foram trilhados nesta época. Retorne ao presente para prosseguir com sua crônica.
          </div>
        )}

        {/* 6. Ação Personalizada */}
        {!isViewingPast && !isStoryLoading && (
          <form onSubmit={(e) => onCustomActionSubmit(e, isAskDmMode)} className="flex flex-col gap-2 mt-2">
            <div className="flex flex-wrap justify-between items-center gap-2">
              <span className="text-[9px] text-blue-700 font-bold tracking-widest font-serif uppercase">
                Decidir Outro Destino (Ação Livre)
              </span>
              <button
                type="button"
                onClick={() => setIsAskDmMode(!isAskDmMode)}
                className={`text-[10px] px-3 py-1.5 rounded flex items-center gap-1.5 cursor-pointer border ${isAskDmMode ? 'bg-sky-950/60 text-sky-300 border-sky-700 shadow-md shadow-sky-900/30' : 'bg-blue-950/40 text-blue-500 border-blue-800'}`}
              >
                <MessageSquare className="w-3.5 h-3.5" />
                {isAskDmMode ? 'MODO DÚVIDAS ATIVO' : 'MODO AÇÃO'}
              </button>
              <span className="text-[8px] text-blue-900/60 italic font-semibold">
                Pressione Enter para enviar, Shift+Enter para quebrar linha
              </span>
            </div>

            <div className="relative border-2 border-blue-900/70 bg-zinc-950 rounded-md p-3 focus-within:border-blue-500 shadow-md transition-colors">
              <textarea
                ref={textareaRef}
                rows={2}
                value={customAction}
                onChange={(e) => setCustomAction(e.target.value)}
                onKeyDown={handleKeyDown}
                data-braseiro-voice-action="true"
                placeholder="Descreva detalhadamente o que deseja fazer ou falar..."
                className="w-full min-w-0 bg-transparent resize-none border-none text-sm text-blue-100 placeholder-blue-900/70 focus:outline-none scrollbar-thin select-text"
                disabled={isStoryLoading}
              />
              
              <div className="flex justify-between items-center border-t border-blue-950/50 mt-2 pt-2 text-[10px]">
                <span className="text-blue-700 font-bold uppercase font-serif tracking-wider">
                  O QUE O HERÓI FAZ?
                </span>
                <button
                  type="submit"
                  disabled={isStoryLoading || isNarrationLoading || !customAction.trim()}
                  className="flex items-center gap-1.5 px-4 py-2 bg-blue-950/40 hover:bg-blue-600/30 border-2 border-blue-600/50 hover:border-blue-400 text-blue-300 hover:text-white font-bold uppercase font-serif rounded-md cursor-pointer tracking-wider text-[10px] disabled:opacity-30 disabled:pointer-events-none transition-all shadow-md hover:shadow-blue-900/40"
                >
                  <Crown className="w-3.5 h-3.5" />
                  Enviar Ação
                </button>
              </div>
            </div>
          </form>
        )}
      </div>

      {/* Coluna Lateral de Locais Visitados */}
      <div className="md:col-span-4 min-w-0 flex flex-col gap-4">
        
        {/* Mapa de Eriador */}
        <EriadorMap
          currentLocation={masterState.position.locationName || activeNode?.location || 'Nova crônica'}
          campaignId={campaignId}
          currentHexId={masterState.position.currentHexId}
          onPartyMoved={onPartyMoved}
          onDiscoveryChanged={onMapDiscoveryChanged}
          onPartyRested={onPartyRested}
          strongholds={masterState.livingWorld.strongholds || []}
        />

        <div className="bg-zinc-950/85 border-2 border-blue-900/80 rounded-md p-4 shadow-lg relative ambient-glow flex flex-col gap-3">
          <div className="absolute top-2 left-2 w-3.5 h-3.5 border-l-2 border-t-2 border-blue-800/45 pointer-events-none"></div>
          
          <div className="flex items-center gap-2 border-b border-blue-900/35 pb-2">
            <Compass className="w-4 h-4 text-blue-500" />
            <span className="text-[10px] font-black text-blue-400 font-serif tracking-wider uppercase">
              Locais Visitados
            </span>
          </div>

          <p className="text-[10px] text-blue-700/80 leading-relaxed font-semibold uppercase font-serif mb-1">
            Selecione uma parada para ler as crônicas daquele local:
          </p>

          <div className="flex md:flex-col gap-2 overflow-x-auto md:overflow-y-auto no-scrollbar md:max-h-[480px] pr-0 md:pr-1 scrollbar-thin">
            {history.map((node, index) => {
              const isActive = (selectedHistoryIndex === null && index === history.length - 1) || selectedHistoryIndex === index;
              return (
                <button
                  key={node.id}
                  onClick={() => setSelectedHistoryIndex(index)}
                  className={`w-[180px] md:w-full shrink-0 text-left p-2.5 rounded border transition-all duration-300 cursor-pointer flex flex-col gap-1 relative overflow-hidden ${
                    isActive
                      ? 'bg-blue-950/25 border-blue-500/80 shadow-[0_0_12px_rgba(217,119,6,0.15)]'
                      : 'bg-zinc-950/40 border-blue-950/40 hover:border-blue-900/50 hover:bg-blue-950/5'
                  }`}
                >
                  {/* Decorative faint background pattern for active card */}
                  {isActive && (
                    <div className="absolute right-[-10px] bottom-[-10px] opacity-[0.06] pointer-events-none">
                      <Compass className="w-16 h-16 text-blue-500" />
                    </div>
                  )}

                  <div className="flex justify-between items-center text-[9px] font-bold font-serif tracking-wider leading-none">
                    <span className={isActive ? 'text-blue-400' : 'text-blue-700'}>
                      Cena {index + 1}
                    </span>
                    <span className="text-blue-800 text-[8px] font-mono">
                      {new Date(node.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  
                  <div className="flex items-start gap-1.5 mt-0.5">
                    <MapPin className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${isActive ? 'text-blue-400' : 'text-blue-800'}`} />
                    <span className={`text-[11px] font-black font-serif leading-tight truncate ${isActive ? 'text-blue-200' : 'text-blue-100/70'}`}>
                      {node.location || 'Estrada Antiga'}
                    </span>
                  </div>
                  
                  <p className="text-[10px] text-blue-100/40 line-clamp-2 md:line-clamp-2 italic font-serif mt-1 leading-snug">
                    {stripOperationalNarrative(node.atmospheric_narration || node.action_result || '')}
                  </p>
                </button>
              );
            })}
          </div>
          
          {selectedHistoryIndex !== null && selectedHistoryIndex !== history.length - 1 && (
            <button
              onClick={() => setSelectedHistoryIndex(null)}
              className="mt-2 w-full py-2 bg-blue-950/20 hover:bg-blue-600/20 border border-blue-600 text-blue-400 hover:text-blue-100 rounded text-[10px] font-serif font-bold uppercase tracking-wider transition-colors cursor-pointer flex items-center justify-center gap-1.5"
            >
              <History className="w-3.5 h-3.5" />
              <span>Voltar ao Presente</span>
            </button>
          )}
        </div>
      </div>

    </div>
  );
};
