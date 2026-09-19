import React, { useState, useEffect, useRef } from 'react';
import { Manual, ManualChunk, type CampaignState, getStoreData, deleteStoreData, searchChunks, applyStoreChangesAtomically } from '../lib/db';
import { importManualFile } from '../lib/manualIndex';
import { Scroll, Upload, Trash2, Search, AlertCircle, CheckCircle2, ListFilter, BookOpen, Clock, FolderPlus } from 'lucide-react';
import { downloadDriveFile, type DriveTreeFileInfo } from '../lib/drive';
import { resolveRulesProfile } from '../lib/autonomy/rulesProfile';
import { markRulesFolderSynced, verifyRulesFolder, type RulesFolderManifest } from '../lib/rulesSync';
import { GoogleDriveExplorer } from './GoogleDriveExplorer';

interface ManualLibraryViewProps {
  onManualsChange: () => void;
  activeSources: Array<{ name: string; pageNum: number; physicalPageNum?: number; pageLabel?: string }>;
  campaign: CampaignState;
}

export const ManualLibraryView: React.FC<ManualLibraryViewProps> = ({
  onManualsChange,
  activeSources,
  campaign,
}) => {
  const profile = resolveRulesProfile(campaign);
  const [manuals, setManuals] = useState<Manual[]>([]);
  const [uploadStatus, setUploadStatus] = useState("");
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDriveExplorerOpen, setIsDriveExplorerOpen] = useState(false);
  const [folderManifest, setFolderManifest] = useState<RulesFolderManifest | null>(null);

  // Manual priority and system selectors
  const [priority, setPriority] = useState<number>(3); // Default to 3: Manual Principal (Core Rules)
  const system = profile.displayName;

  // Local Search Tester
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<ManualChunk[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadManuals = async () => {
    try {
      const data = await getStoreData<Manual>('manuals');
      const invalid = data.filter(item => (item.profileId === profile.id || (!item.profileId && item.system === profile.displayName)) && item.type !== 'pdf');
      if (invalid.length) {
        const invalidIds = new Set(invalid.map(item => item.id));
        const chunks = (await getStoreData<ManualChunk>('manual_chunks')).filter(chunk => invalidIds.has(chunk.manualId));
        await applyStoreChangesAtomically({
          manuals: { delete: invalid.map(item => item.id) },
          manual_chunks: { delete: chunks.map(chunk => chunk.id) },
          manual_search_index: { delete: chunks.map(chunk => chunk.id) },
          manual_import_jobs: { delete: invalid.map(item => `manual-import:${item.id}`) },
        });
      }
      const scoped = data.filter(item => !invalid.some(row => row.id === item.id) && (item.profileId === profile.id || (!item.profileId && item.system === profile.displayName)));
      setManuals(scoped.sort((a, b) => a.priority - b.priority || a.name.localeCompare(b.name)));
      if (profile.sourceFolderId) setFolderManifest(await verifyRulesFolder(profile.id, profile.sourceFolderId, data));
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadManuals();
  }, [profile.id]);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []) as File[];
    if (!files.length) return;
    for (const file of files) await processFile(file);
  };

  const processFile = async (file: File, drive?: DriveTreeFileInfo) => {
    setIsUploading(true);
    setUploadError(null);
    setUploadProgress(0);
    setUploadStatus(`Indexando ${file.name}...`);
    
    try {
      await importManualFile(file, priority, system, (status, percent) => {
        setUploadStatus(status);
        setUploadProgress(percent);
      }, {
        profileId: profile.id, systemProfileId: profile.systemId, settingProfileId: profile.settingId,
        rulesSourceFolderId: profile.sourceFolderId, driveFileId: drive?.id, driveModifiedTime: drive?.modifiedTime,
        driveMd5Checksum: drive?.md5Checksum, drivePath: drive?.relativePath,
      });
      
      await loadManuals();
      onManualsChange();
    } catch (err) {
      console.error("Indexation failed:", err);
      setUploadStatus("Falha na indexação do PDF.");
      setUploadError(err instanceof Error ? err.message : String(err || 'O arquivo não pôde ser lido.'));
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDriveFileSelected = async (blob: Blob, fileName: string, mimeType: string) => {
    const file = new File([blob], fileName, { type: mimeType });
    await processFile(file);
  };

  const handleDriveImport = async () => {
    const folderId = profile.sourceFolderId;
    if (!folderId) {
      setUploadStatus('Este perfil ainda não possui uma pasta de regras configurada. Nenhuma pasta de outro sistema será usada automaticamente.');
      return;
    }
    setIsUploading(true);
    setUploadError(null);
    setUploadStatus('Verificando versões da pasta de regras no Google Drive…');
    setUploadProgress(5);
    try {
      const currentManuals = await getStoreData<Manual>('manuals');
      const verification = await verifyRulesFolder(profile.id, folderId, currentManuals);
      if (verification.status === 'failed') throw new Error(verification.error || 'Não foi possível verificar a pasta de regras.');

      const missingIds = new Set(verification.missingFileIds);
      const obsoleteManuals = currentManuals.filter(item => item.profileId === profile.id && item.driveFileId && missingIds.has(item.driveFileId));
      if (obsoleteManuals.length) {
        const obsoleteIds = new Set(obsoleteManuals.map(item => item.id));
        const allChunks = await getStoreData<ManualChunk>('manual_chunks');
        const obsoleteChunks = allChunks.filter(chunk => obsoleteIds.has(chunk.manualId));
        await applyStoreChangesAtomically({
          manuals: { delete: obsoleteManuals.map(item => item.id) },
          manual_chunks: { delete: obsoleteChunks.map(chunk => chunk.id) },
          manual_search_index: { delete: obsoleteChunks.map(chunk => chunk.id) },
          manual_import_jobs: { delete: obsoleteManuals.map(item => `manual-import:${item.id}`) },
        });
      }

      const changed = new Set(verification.changedFileIds);
      const accepted = verification.files.filter(file => changed.has(file.id));
      if (!accepted.length) {
        await markRulesFolderSynced(profile.id);
        await loadManuals();
        setUploadStatus('A pasta de regras já está sincronizada; nenhum download foi necessário.');
        setUploadProgress(100);
        return;
      }
      let done = 0;
      for (const metadata of accepted) {
        setUploadStatus(`Baixando atualização: ${metadata.name}…`);
        const blob = await downloadDriveFile(metadata.id, metadata.mimeType);
        const isDoc = metadata.mimeType === 'application/vnd.google-apps.document';
        const finalName = isDoc && !/\.txt$/i.test(metadata.name) ? `${metadata.name}.txt` : metadata.name;
        const finalType = isDoc ? 'text/plain' : metadata.mimeType;
        const file = new File([blob], finalName, { type: finalType });
        await processFile(file, metadata as DriveTreeFileInfo);
        done += 1;
        setUploadProgress(Math.round((done / accepted.length) * 100));
      }
      await markRulesFolderSynced(profile.id);
      await loadManuals();
      onManualsChange();
      setUploadStatus(`${accepted.length} manual(is) novo(s) ou atualizado(s); versões removidas foram limpas sem afetar outros perfis.`);
    } catch (err: any) {
      console.error('Drive import failed:', err);
      setUploadStatus('Falha na sincronização do Google Drive.');
      setUploadError(err?.message || 'Falha na sincronização do Google Drive.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeleteManual = async (id: string) => {
    if (!confirm("Excluir este manual e apagar todos os trechos indexados?")) return;
    try {
      const chunks = await getStoreData<ManualChunk>('manual_chunks');
      const filteredChunks = chunks.filter(c => c.manualId === id);
      await applyStoreChangesAtomically({
        manuals: { delete: [id] },
        manual_chunks: { delete: filteredChunks.map(chunk => chunk.id) },
        manual_search_index: { delete: filteredChunks.map(chunk => chunk.id) },
        manual_import_jobs: { delete: [`manual-import:${id}`] },
      });
      
      await loadManuals();
      onManualsChange();
    } catch (err) {
      console.error(err);
    }
  };

  const handleSearchTest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    try {
      const keywords = searchQuery.split(' ').filter(w => w.length > 2);
      const results = await searchChunks(keywords, [profile.id, profile.systemId, profile.settingId, profile.displayName], 'narrative');
      setSearchResults(results);
    } catch (err) {
      console.error(err);
    } finally {
      setIsSearching(false);
    }
  };

  const getPriorityLabel = (p: number) => {
    switch (p) {
      case 1: return 'Regras da Casa (Prioridade Máxima)';
      case 2: return 'Campanha e Diário';
      case 3: return 'Livro Básico / Mestre';
      case 4: return 'Suplementos / Expansões';
      default: return 'Suplemento';
    }
  };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col md:flex-row gap-6 text-blue-100 font-serif pb-12 select-none">
      
      {/* Left Column: Index uploader & List */}
      <div className="flex-1 flex flex-col gap-6">
        
        {/* Card 1: Upload Manual */}
        <div className="bg-zinc-950/90 border-2 border-blue-900 p-5 rounded-md ambient-glow relative">
          <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-500"></div>
          <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-4">
            <Scroll className="w-4 h-4 text-blue-500" />
            <span className="text-sm font-black text-blue-500 font-serif tracking-wider">BIBLIOTECA DE REGRAS & MANUAIS</span>
          </div>

          <p className="text-xs text-blue-700/90 mb-4 leading-relaxed">
            Importe somente <strong>PDFs de regras e suplementos</strong>. Mapas, músicas, tokens, imagens e arquivos auxiliares ficam fora do índice do Mestre.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs mb-4">
            <div>
              <label className="text-[10px] text-blue-600 font-bold font-serif uppercase">Hierarquia de Prioridade</label>
              <select
                value={priority}
                onChange={(e) => setPriority(parseInt(e.target.value) || 3)}
                className="w-full mt-1.5 py-1.5 px-2 bg-zinc-900 border border-blue-900/40 rounded text-blue-300 focus:outline-none cursor-pointer"
              >
                <option value="1">1. Regras da Casa</option>
                <option value="2">2. Documentos da Campanha</option>
                <option value="3">3. Manual Básico principal</option>
                <option value="4">4. Suplementos extras</option>
              </select>
            </div>
            <div>
              <label className="text-[10px] text-blue-600 font-bold font-serif uppercase">Sistema de RPG</label>
              <div className="w-full mt-1.5 px-2 py-1.5 bg-zinc-900 border border-blue-900/30 rounded text-blue-300" title={profile.id}>
                {system}
              </div>
            </div>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
              className="flex-1 py-3 bg-blue-950/20 hover:bg-blue-600/20 border-2 border-blue-600 text-blue-400 font-bold hover:text-blue-100 uppercase tracking-widest font-serif rounded cursor-pointer transition-all"
            >
              SELECIONAR ARQUIVO
            </button>
            <button
              onClick={() => setIsDriveExplorerOpen(true)}
              disabled={isUploading}
              className="px-4 py-3 bg-blue-950/20 hover:bg-blue-600/20 border-2 border-blue-600 text-blue-400 font-bold hover:text-blue-100 uppercase tracking-widest font-serif rounded cursor-pointer transition-all flex items-center gap-2"
              title="Explorar Google Drive"
            >
              <FolderPlus className="w-4 h-4" />
              <span className="text-[10px] hidden sm:inline">Drive</span>
            </button>
          </div>

          <div className="mt-2 flex justify-between items-center text-[10px] text-blue-700">
            <span>Somente PDF válido</span>
            <button
              onClick={handleDriveImport}
              disabled={isUploading}
              className="hover:text-blue-400 underline cursor-pointer"
              title="Baixa automaticamente todos os arquivos da pasta de campanha compartilhada"
            >
              Importar pasta padrão de campanha
            </button>
          </div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept=".pdf,application/pdf"
            multiple
            className="hidden"
          />

          {/* Upload Status / Progress Bar */}
          {folderManifest && (
            <div className={`mt-3 p-2 rounded border text-[10px] ${folderManifest.status === 'current' ? 'border-emerald-900 text-emerald-400' : folderManifest.status === 'failed' ? 'border-red-900 text-red-400' : 'border-amber-900 text-amber-400'}`}>
              Pasta de regras: {folderManifest.status === 'current' ? 'sincronizada' : folderManifest.status === 'changes-detected' ? `${folderManifest.changedFileIds.length} alteração(ões) pendente(s)` : 'não verificada'} · {folderManifest.files.length} arquivo(s) · verificada em {new Date(folderManifest.checkedAt).toLocaleString('pt-BR')}.
            </div>
          )}

          {isUploading && (
            <div className="mt-4 p-3 bg-zinc-900 border border-blue-900/40 rounded text-xs space-y-2">
              <div className="flex justify-between items-center font-bold text-blue-400 font-serif">
                <span>{uploadStatus}</span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full h-2 bg-blue-950/30 border border-blue-900/20 rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 rounded-full transition-all duration-300" style={{ width: `${uploadProgress}%` }}></div>
              </div>
            </div>
          )}
          {!isUploading && uploadError && (
            <div className="mt-4 flex items-start gap-2 rounded border border-red-800 bg-red-950/30 p-3 text-xs text-red-200" role="alert">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <div className="min-w-0">
                <p className="font-bold">O manual não foi importado</p>
                <p className="mt-1 break-words text-red-300">{uploadError}</p>
                <button type="button" onClick={() => setUploadError(null)} className="mt-2 underline">Fechar aviso</button>
              </div>
            </div>
          )}
        </div>

        {/* Card 2: Manual list */}
        <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md">
          <span className="text-[10px] text-blue-700 font-bold tracking-widest font-serif uppercase block border-b border-blue-900/30 pb-2 mb-3">
            Manuais Cadastrados
          </span>

          {manuals.length === 0 ? (
            <div className="py-8 text-center text-blue-950 text-xs">
              <Scroll className="w-8 h-8 mx-auto mb-2 opacity-20" />
              <p className="font-serif font-bold">Sem Manuais de Referência</p>
              <p className="italic mt-1 font-serif">O mestre decidirá apenas com base no lore primordial.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {manuals.map((man) => (
                <div key={man.id} className="p-3 bg-zinc-900/40 border border-blue-950 hover:border-blue-900 rounded-md flex justify-between items-center gap-3 text-xs">
                  <div className="min-w-0">
                    <span className="font-bold text-blue-200 block truncate" title={man.name}>
                      {man.name}
                    </span>
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[9px] text-blue-700/80 font-serif font-semibold mt-0.5 uppercase">
                      <span className="text-blue-500">Prio {man.priority}: {man.system}</span>
                      <span>•</span>
                      <span>{man.chunkCount} trechos</span>
                      {man.pageCount && (
                        <>
                          <span>•</span>
                          <span>{man.pageCount} pág</span>
                        </>
                      )}
                      <span>•</span>
                      <span className="lowercase">{(man.size / 1024).toFixed(0)}kb</span>
                    </div>
                  </div>

                  <button
                    onClick={() => handleDeleteManual(man.id)}
                    className="p-1.5 bg-zinc-900 border border-blue-950 hover:border-red-900 text-blue-800 hover:text-red-500 rounded cursor-pointer shrink-0"
                    title="Excluir manual indexado"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

      {/* Right Column: Search tester & Current Sources */}
      <div className="w-full md:w-80 flex flex-col gap-6 shrink-0">
        
        {/* Card 3: Sources Sent to Gemini on last action */}
        <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md flex flex-col min-h-[160px]">
          <span className="text-[10px] text-blue-500 font-bold tracking-widest font-serif uppercase border-b border-blue-900/40 pb-2 mb-3">
            Fontes enviadas ao Mestre
          </span>

          {activeSources.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-3 text-blue-950 text-[10px]">
              <Clock className="w-5 h-5 mb-1 opacity-20" />
              <p className="font-bold uppercase font-serif">Sem fontes de regras</p>
              <p className="font-serif italic mt-0.5">Nenhum trecho de regra coincidiu com a última ação.</p>
            </div>
          ) : (
            <div className="space-y-1.5 max-h-[150px] overflow-y-auto scrollbar-thin">
              {activeSources.map((src, idx) => (
                <div key={idx} className="p-1.5 bg-blue-950/10 border border-blue-900/20 rounded text-[10px] flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <span className="font-bold text-blue-300 block truncate" title={src.name}>
                      {src.name}
                    </span>
                    <span className="text-[8px] text-blue-700 uppercase font-bold font-serif">
                      {src.pageLabel && src.pageLabel !== String(src.pageNum) ? `Página ${src.pageLabel} (PDF ${src.physicalPageNum || src.pageNum})` : `Página ${src.pageNum}`}
                    </span>
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-blue-500 shrink-0" />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Card 4: Local Search Tester */}
        <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md flex flex-col h-[320px]">
          <span className="text-[10px] text-blue-700 font-bold tracking-widest font-serif uppercase border-b border-blue-900/40 pb-2 mb-3">
            Testador de Busca Local
          </span>

          <form onSubmit={handleSearchTest} className="relative mb-3 shrink-0">
            <Search className="w-4 h-4 text-blue-800 absolute left-2.5 top-2.5" />
            <input
              type="text"
              placeholder="Digite palavra-chave..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-12 py-1.5 bg-zinc-900 border border-blue-900/30 rounded text-xs focus:outline-none"
            />
            <button
              type="submit"
              className="absolute right-1 top-1.5 px-2 bg-blue-950/20 text-[10px] text-blue-400 font-bold rounded"
            >
              Buscar
            </button>
          </form>

          {/* Search results list */}
          <div className="flex-1 overflow-y-auto pr-1 scrollbar-thin space-y-2">
            {isSearching ? (
              <p className="text-center py-6 text-blue-500 text-[10px] animate-pulse">Consultando IndexedDB...</p>
            ) : searchResults.length === 0 ? (
              <p className="text-center py-6 text-blue-950 text-[10px] italic">Pesquise para testar a indexação local.</p>
            ) : (
              searchResults.map((chk, idx) => (
                <div key={chk.id} className="p-2 bg-zinc-900 border border-blue-950 rounded text-[10px] space-y-1">
                  <div className="flex justify-between items-center text-[8px] text-blue-600 font-bold font-serif uppercase">
                    <span className="truncate max-w-[150px]">{chk.manualName}</span>
                    <span>Pág {chk.pageNum}</span>
                  </div>
                  <p className="text-blue-100/80 leading-snug line-clamp-3">
                    {chk.text}
                  </p>
                </div>
              ))
            )}
          </div>
        </div>

      </div>

      <GoogleDriveExplorer
        isOpen={isDriveExplorerOpen}
        onClose={() => setIsDriveExplorerOpen(false)}
        onFileSelected={handleDriveFileSelected}
        allowedExtensions={['pdf']}
        title="Biblioteca de Manuais :: Google Drive"
      />

    </div>
  );
};
