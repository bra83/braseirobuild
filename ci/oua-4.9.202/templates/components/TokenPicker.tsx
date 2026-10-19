import React, { useEffect, useMemo, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { IndividualToken, getStoreData } from '../lib/db';
import {
  installPartyTokenById,
  loadPartyTokenManifest,
  partyCultureKey,
  partyRaceFromCulture,
  partyTokenMatchesCulture,
  resolvePartyTokenAsset,
  type PartyTokenEntry,
  type PartyTokenManifest,
} from '../lib/partyTokenLibrary';

interface TokenPickerProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (tokenId: string | null) => void;
  selectedTokenId: string | null;
  culture?: string;
  playerOnly?: boolean;
}

export const TokenPicker: React.FC<TokenPickerProps> = ({
  isOpen, onClose, onSelect, selectedTokenId, culture, playerOnly = false,
}) => {
  const [tokens, setTokens] = useState<IndividualToken[]>([]);
  const [partyManifest, setPartyManifest] = useState<PartyTokenManifest | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tokenUrls, setTokenUrls] = useState<Record<string, string>>({});
  const [showAll, setShowAll] = useState(false);
  const urlsRef = useRef<string[]>([]);

  const loadTokens = async () => {
    try { setTokens(await getStoreData<IndividualToken>('individual_tokens')); }
    catch (e) { console.error('Failed to load tokens:', e); }
  };

  useEffect(() => {
    if (!isOpen) return;
    setShowAll(false);
    void loadTokens();
    if (playerOnly) {
      void loadPartyTokenManifest().then(async manifest => {
        setPartyManifest(manifest);
        if (selectedTokenId && manifest?.tokens.some(entry => entry.id === selectedTokenId)) {
          try {
            await installPartyTokenById(selectedTokenId);
            await loadTokens();
          } catch (error) {
            console.warn('[PARTY TOKEN] não foi possível recortar novamente o token selecionado:', error);
          }
        }
      });
    }
  }, [isOpen, playerOnly, culture, selectedTokenId]);

  useEffect(() => {
    urlsRef.current.forEach(url => URL.revokeObjectURL(url));
    const next: Record<string, string> = {};
    for (const token of tokens) next[token.id] = URL.createObjectURL(token.blob);
    urlsRef.current = Object.values(next);
    setTokenUrls(next);
    return () => urlsRef.current.forEach(url => URL.revokeObjectURL(url));
  }, [tokens]);

  const builtIns = useMemo(() => {
    const source = (partyManifest?.tokens || []).filter(entry => entry.race !== 'enemy');
    if (!playerOnly || showAll) return source;
    const matching = source.filter(entry => partyTokenMatchesCulture(entry, culture));
    const cultureKey = partyCultureKey(culture);
    return [...matching].sort((a, b) => Number(b.culture === cultureKey) - Number(a.culture === cultureKey));
  }, [partyManifest, playerOnly, showAll, culture]);

  // Os tokens do ZIP também usam categorias party:*. O manifesto embutido e o
  // banco de tokens importados são duas fontes distintas; exibir ambas sem duplicar.
  const importedTokens = useMemo(() => {
    const builtInIds = new Set((partyManifest?.tokens || []).map(entry => entry.id));
    const external = tokens.filter(token => !builtInIds.has(token.id));
    if (!playerOnly) return external.filter(token => !token.category?.startsWith('party:'));
    const race = partyRaceFromCulture(culture);
    return external.filter(token => {
      const category = String(token.category || '').toLowerCase();
      // Tokens não classificados só aparecem mediante escolha explícita.
      if (!category.startsWith('party:')) return showAll;
      const tokenRace = category.split(':')[1];
      if (tokenRace === 'enemy') return false;
      return showAll || !race || tokenRace === race;
    });
  }, [tokens, partyManifest, playerOnly, showAll, culture]);

  const selectBuiltIn = async (entry: PartyTokenEntry) => {
    setIsImporting(true); setError(null);
    try {
      await installPartyTokenById(entry.id);
      await loadTokens();
      onSelect(entry.id);
    } catch (e) { setError(`Não foi possível preparar o asset: ${String(e)}`); }
    finally { setIsImporting(false); }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 p-4 font-serif select-none">
      <div className="relative flex h-[85vh] w-full max-w-4xl flex-col rounded-lg border border-[#745b3b]/70 bg-[#100d0a]/95 p-5 shadow-[0_0_50px_rgba(0,0,0,.65)]">
        <div className="mb-3 flex items-center justify-between border-b border-[#745b3b]/45 pb-3">
          <div>
            <h2 className="text-lg font-bold tracking-wider text-[#dbc59a]">Escolha visual do personagem</h2>
            {playerOnly && <p className="mt-1 text-xs text-[#9f8a67]">{culture || 'Cultura não definida'} · assets compatíveis pela raça</p>}
          </div>
          <button onClick={onClose} className="text-[#aa9878] hover:text-[#ead9b7]">Fechar</button>
        </div>

        <div className="mb-3 flex items-center gap-2">
          {playerOnly && <button type="button" onClick={() => setShowAll(value => !value)} className="rounded border border-[#745b3b]/60 px-3 py-1.5 text-xs text-[#dbc59a]">{showAll ? 'Somente minha raça' : 'Mostrar biblioteca inteira'}</button>}
          {isImporting && <span className="flex items-center gap-1 text-xs text-[#c3ad83]"><RefreshCw className="h-3.5 w-3.5 animate-spin"/>Preparando recorte…</span>}
        </div>
        {error && <div className="mb-3 rounded border border-red-900 bg-red-950/30 p-2 text-xs text-red-300">{error}</div>}

        <div className="flex-1 overflow-y-auto pr-2 [scrollbar-width:thin]">
          {builtIns.length > 0 && <>
            <h3 className="sticky top-0 z-10 mb-2 bg-[#100d0a]/95 py-1 text-xs uppercase tracking-[.18em] text-[#8d7958]">Biblioteca da Companhia</h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 md:grid-cols-6">
              {builtIns.map(entry => {
                const src = resolvePartyTokenAsset(partyManifest, entry);
                const prepared = tokenUrls[entry.id];
                return <button type="button" key={entry.id} disabled={isImporting} onClick={() => void selectBuiltIn(entry)} className={`relative flex h-40 items-center justify-center overflow-hidden rounded border bg-black p-0 ${selectedTokenId === entry.id ? 'border-[#d5b36b] ring-1 ring-[#d5b36b]/40' : 'border-[#5f503b]/45'}`}>
                  {prepared
                    ? <img src={prepared} alt="Personagem recortado" className="h-full w-full object-contain object-center drop-shadow-[0_3px_2px_rgba(0,0,0,.75)]" draggable={false}/>
                    : src && <img src={src} alt="Personagem" className="h-full w-full scale-[1.18] object-contain object-center drop-shadow-[0_3px_2px_rgba(0,0,0,.75)]" draggable={false}/>}
                </button>;
              })}
            </div>
          </>}

          {importedTokens.length > 0 && <>
            <h3 className="mb-2 mt-5 text-xs uppercase tracking-[.18em] text-[#8d7958]">Tokens importados ({importedTokens.length})</h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 md:grid-cols-6">
              {importedTokens.map(t => <button type="button" key={t.id} onClick={() => onSelect(t.id)} className={`flex h-32 items-center justify-center overflow-hidden rounded border p-2 ${selectedTokenId === t.id ? 'border-[#d5b36b]' : 'border-[#5f503b]/35'}`}>
                {tokenUrls[t.id] && <img src={tokenUrls[t.id]} alt="Token importado" className="h-full w-full object-contain"/>}
              </button>)}
            </div>
          </>}
        </div>
      </div>
    </div>
  );
};
