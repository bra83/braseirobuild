import React, { useEffect, useRef, useState } from 'react';
import { RefreshCw, Upload, Trash2, CheckCircle2, AlertCircle } from 'lucide-react';
import { IndividualToken, getStoreData } from '../lib/db';
import { clearTokenSheetLibrary, importOneRingTokenLibraryFromDrive, importOneRingTokenPackage } from '../lib/tokenSheetLibrary';
import { rebuildNpcTokenCatalogFromSheetLibrary } from '../lib/npcTokenImportService';

const TokenPreview: React.FC<{ token: IndividualToken }> = ({ token }) => {
  const [url, setUrl] = useState('');
  useEffect(() => {
    const next = URL.createObjectURL(token.blob);
    setUrl(next);
    return () => URL.revokeObjectURL(next);
  }, [token.id, token.blob]);
  return url ? <img src={url} alt={token.id} loading="lazy" className="w-full h-full object-cover rounded-full" /> : null;
};

export const TokensView: React.FC = () => {
  const [tokens, setTokens] = useState<IndividualToken[]>([]);
  const [isImporting, setIsImporting] = useState(false);
  const [status, setStatus] = useState('');
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [visibleCount, setVisibleCount] = useState(120);

  const loadTokens = async () => {
    const all = await getStoreData<IndividualToken>('individual_tokens');
    setTokens(all.sort((a, b) => a.id.localeCompare(b.id)));
    setVisibleCount(120);
  };

  useEffect(() => { void loadTokens(); }, []);

  const importPackage = async (selected: File[]) => {
    setIsImporting(true);
    setError(null);
    try {
      const result = await importOneRingTokenPackage(selected, progress => setStatus(progress.message));
      const rebuilt = await rebuildNpcTokenCatalogFromSheetLibrary(false);
      await loadTokens();
      setStatus(`${result.tokenCount} tokens importados; ${rebuilt.catalogCount} elegíveis para NPCs; ${result.blockedSheets.length} folhas externas bloqueadas.`);
    } catch (reason: any) {
      console.error(reason);
      setError(reason?.message || String(reason));
      setStatus('');
    } finally {
      setIsImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const importFromDrive = async () => {
    setIsImporting(true);
    setError(null);
    try {
      const result = await importOneRingTokenLibraryFromDrive(progress => setStatus(progress.message));
      const rebuilt = await rebuildNpcTokenCatalogFromSheetLibrary(false);
      await loadTokens();
      setStatus(`${result.tokenCount} tokens atualizados; ${rebuilt.catalogCount} elegíveis para NPCs; ${result.blockedSheets.length} folhas externas bloqueadas.`);
    } catch (reason: any) {
      console.error(reason);
      setError(reason?.message || String(reason));
      setStatus('');
    } finally {
      setIsImporting(false);
    }
  };

  const clearTokens = async () => {
    const removed = await clearTokenSheetLibrary();
    await rebuildNpcTokenCatalogFromSheetLibrary(false);
    await loadTokens();
    setStatus(`${removed} tokens da biblioteca removidos. Retratos de heróis/NPCs gerados foram preservados.`);
  };

  return (
    <div className="p-5 font-serif select-none">
      <div className="flex flex-wrap justify-between items-center gap-3 border-b border-blue-900/40 pb-3 mb-4">
        <h2 className="text-lg font-bold text-blue-500 font-serif tracking-wider">Biblioteca de Tokens</h2>
        <span className="text-xs text-blue-700">{tokens.length} tokens</span>
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        <button onClick={importFromDrive} disabled={isImporting} className="min-h-12 bg-blue-900 text-blue-100 px-3 py-2 rounded flex items-center gap-2 disabled:opacity-50">
          <RefreshCw className={`w-4 h-4 ${isImporting ? 'animate-spin' : ''}`} />
          {isImporting ? 'Importando...' : 'Importar/Atualizar do Drive'}
        </button>
        <button onClick={() => fileInputRef.current?.click()} disabled={isImporting} className="min-h-12 border border-blue-700 bg-blue-950/30 text-blue-300 px-3 py-2 rounded flex items-center gap-2 disabled:opacity-50">
          <Upload className="w-4 h-4" /> ZIP ou arquivos do aparelho
        </button>
        <button onClick={clearTokens} disabled={isImporting} className="min-h-12 border border-red-900 bg-red-950/20 text-red-400 px-3 py-2 rounded flex items-center gap-2 disabled:opacity-50">
          <Trash2 className="w-4 h-4" /> Limpar cache
        </button>
        <input ref={fileInputRef} type="file" multiple accept=".zip,.json,image/*" className="hidden" onChange={event => void importPackage(Array.from(event.target.files || []))} />
      </div>

      {status && <div className="mb-4 p-2 bg-emerald-950/20 border border-emerald-900 text-emerald-300 rounded flex items-center gap-2"><CheckCircle2 className="w-4 h-4" />{status}</div>}
      {error && <div className="mb-4 p-2 bg-red-950/30 border border-red-900 text-red-400 rounded flex items-start gap-2"><AlertCircle className="w-4 h-4 mt-0.5" />{error}</div>}

      <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3 pr-2 scrollbar-thin">
        {tokens.slice(0, visibleCount).map(token => (
          <div key={token.id} className="p-2 border rounded-md border-blue-900/30 bg-zinc-900/20 aspect-square" title={token.id}>
            <TokenPreview token={token} />
          </div>
        ))}
      </div>
      {visibleCount < tokens.length && (
        <button onClick={() => setVisibleCount(count => Math.min(tokens.length, count + 120))} className="mt-4 min-h-12 w-full border border-blue-800 bg-blue-950/30 text-blue-200 rounded px-4 py-3 text-sm">
          Mostrar mais ({tokens.length - visibleCount} restantes)
        </button>
      )}
    </div>
  );
};
