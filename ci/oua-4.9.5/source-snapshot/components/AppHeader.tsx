import React, { useEffect, useState } from 'react';
import { Heart, MapPin, Shield, Sparkles, UserRound, Volume2, VolumeX } from 'lucide-react';
import { getStoreData, type Character, type CharacterSheet } from '../lib/db';

interface AppHeaderProps {
  characterSheet: CharacterSheet;
  characters?: Character[];
  activeCharacterId?: string | null;
  partyCharacterIds?: string[];
  onActiveCharacterChange?: (characterId: string) => void | Promise<void>;
  locationName: string;
  weather?: string;
  timeLabel?: string;
  onStopAll: () => void;
  onOpenAudio?: () => void;
  audioActive: boolean;
  onGoToSheet?: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  characterSheet,
  characters = [],
  activeCharacterId,
  partyCharacterIds = [],
  onActiveCharacterChange,
  locationName,
  weather,
  timeLabel,
  onStopAll,
  onOpenAudio,
  audioActive,
  onGoToSheet,
}) => {
  const [portrait, setPortrait] = useState<string | null>(null);

  useEffect(() => {
    let revoked: string | null = null;
    const load = async () => {
      if (!characterSheet.tokenId) { setPortrait(null); return; }
      try {
        const tokens = await getStoreData<any>('individual_tokens');
        const token = tokens.find(item => item?.id === characterSheet.tokenId && item?.blob);
        if (!token) { setPortrait(null); return; }
        revoked = URL.createObjectURL(token.blob);
        setPortrait(revoked);
      } catch { setPortrait(null); }
    };
    void load();
    return () => { if (revoked) URL.revokeObjectURL(revoked); };
  }, [characterSheet.tokenId]);

  const partySet = new Set(partyCharacterIds);
  const activePartyCharacters = characters.filter(character => partySet.has(character.id));
  const selectableCharacters = activePartyCharacters.length ? activePartyCharacters : characters;
  const identity = characterSheet.identity;
  const attributes = characterSheet.attributes;
  const enduranceMax = Math.max(1, Number(attributes.enduranceMax) || 1);
  const hopeMax = Math.max(1, Number(attributes.hopeMax) || 1);
  const endurancePct = Math.max(0, Math.min(100, Number(attributes.enduranceCurrent) / enduranceMax * 100));
  const hopePct = Math.max(0, Math.min(100, Number(attributes.hopeCurrent) / hopeMax * 100));

  return (
    <header className="app-compact-header w-full border-b border-blue-950/80 bg-[#020713]/98 px-2 py-2 font-serif text-blue-100 shadow-[0_4px_18px_rgba(0,0,0,.55)] backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center gap-2">
        <button onClick={onGoToSheet} className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-full border border-cyan-700/70 bg-cyan-950/25" title="Abrir ficha">
          {portrait ? <img src={portrait} className="h-full w-full object-cover" alt={identity.name} /> : <UserRound className="h-5 w-5 text-cyan-300" />}
        </button>

        <div className="min-w-0 flex-1">
          {selectableCharacters.length > 1 ? (
            <select
              value={activeCharacterId || ''}
              onChange={event => void onActiveCharacterChange?.(event.target.value)}
              className="block max-w-full bg-transparent text-[12px] font-bold text-blue-100 outline-none"
              aria-label="Personagem ativo"
            >
              {selectableCharacters.map(character => <option key={character.id} value={character.id} className="bg-[#030914]">{character.sheet.identity.name || character.name}</option>)}
            </select>
          ) : <button onClick={onGoToSheet} className="block max-w-full truncate text-left text-[12px] font-bold text-blue-100">{identity.name || 'Novo Herói'}</button>}
          <div className="mt-0.5 flex min-w-0 items-center gap-1 text-[9px] text-slate-500">
            <MapPin className="h-2.5 w-2.5 shrink-0 text-blue-500" />
            <span className="truncate">{locationName}{timeLabel ? ` · ${timeLabel}` : ''}{weather ? ` · ${weather}` : ''}{partyCharacterIds.length > 1 ? ` · ${partyCharacterIds.length} heróis` : ''}</span>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <div className="rounded border border-red-950/70 bg-black/30 px-2 py-1" title="Resistência">
            <div className="flex items-center gap-1 text-[10px]"><Heart className="h-3 w-3 text-red-500" /><strong>{attributes.enduranceCurrent}/{attributes.enduranceMax}</strong></div>
            <div className="mt-1 h-1 w-12 overflow-hidden rounded bg-zinc-900"><div className="h-full bg-red-600" style={{width:`${endurancePct}%`}} /></div>
          </div>
          <div className="rounded border border-cyan-950/70 bg-black/30 px-2 py-1" title="Esperança">
            <div className="flex items-center gap-1 text-[10px]"><Sparkles className="h-3 w-3 text-cyan-400" /><strong>{attributes.hopeCurrent}/{attributes.hopeMax}</strong></div>
            <div className="mt-1 h-1 w-12 overflow-hidden rounded bg-zinc-900"><div className="h-full bg-cyan-500" style={{width:`${hopePct}%`}} /></div>
          </div>
          <button onClick={onGoToSheet} className="hidden rounded border border-blue-900 p-2 text-blue-300 sm:block" title="Ficha"><Shield className="h-4 w-4" /></button>
          <button onClick={onOpenAudio} className="rounded border border-cyan-900/70 bg-cyan-950/20 p-2 text-cyan-300" title="Controles rápidos de volume"><Volume2 className="h-4 w-4" /></button>
          <button onClick={onStopAll} className="rounded border border-red-900/60 bg-red-950/20 p-2 text-red-400" title="Parar toda a voz e os sons">{audioActive ? <Volume2 className="h-4 w-4 animate-pulse" /> : <VolumeX className="h-4 w-4" />}</button>
        </div>
      </div>
    </header>
  );
};
