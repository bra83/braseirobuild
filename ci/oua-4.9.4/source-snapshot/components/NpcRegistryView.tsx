import React, { useState, useEffect, useRef } from 'react';
import { Npc, IndividualToken, getStoreData, getActiveCampaign, saveCanonicalTransaction } from '../lib/db';
import { Plus, Trash2, Edit3, Search, User, Eye, EyeOff, UserPlus, ShieldAlert, Heart, Clipboard, HelpCircle, RefreshCw, ImagePlus } from 'lucide-react';
import { TokenPicker } from './TokenPicker';
import { autoAssignNpcToken, getNpcTokenCatalog } from '../lib/npcTokenLibrary';
import { clearNpcTokenLibrary, getNpcTokenImportStates, importNpcTokensFromDevice, syncNpcTokensFromConfiguredDrive, type NpcTokenImportState } from '../lib/npcTokenImportService';

interface NpcRegistryViewProps {
  onNpcChange: () => void;
}

export const NpcRegistryView: React.FC<NpcRegistryViewProps> = ({ onNpcChange }) => {
  const [npcs, setNpcs] = useState<Npc[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterPresence, setFilterPresence] = useState<'all' | 'present' | 'absent'>('all');
  
  // Tokens cache
  const [tokens, setTokens] = useState<Record<string, string>>({});
  
  // Selected/editing NPC state
  const [editingNpc, setEditingNpc] = useState<Npc | null>(null);
  
  // Token picker modal state
  const [isPickerOpen, setIsPickerOpen] = useState(false);
  const [targetTokenNpcId, setTargetTokenNpcId] = useState<string | null>(null);
  const [catalogCount, setCatalogCount] = useState(0);
  const [catalogBySource, setCatalogBySource] = useState<Record<string, number>>({});
  const [importStates, setImportStates] = useState<NpcTokenImportState[]>([]);
  const [tokenStatus, setTokenStatus] = useState('');
  const [isTokenSyncing, setIsTokenSyncing] = useState(false);
  const npcFileInputRef = useRef<HTMLInputElement>(null);
  const tokenUrlsRef = useRef<string[]>([]);

  const loadData = async () => {
    try {
      const allNpcs = await getStoreData<Npc>('npcs');
      setNpcs(allNpcs);
      
      const allTokens = await getStoreData<IndividualToken>('individual_tokens');
      const tokenMap: Record<string, string> = {};
      allTokens.forEach(t => {
        tokenMap[t.id] = URL.createObjectURL(t.blob);
      });
      tokenUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
      tokenUrlsRef.current = Object.values(tokenMap);
      setTokens(tokenMap);
      const catalog = await getNpcTokenCatalog();
      setCatalogCount(catalog.length);
      setCatalogBySource(catalog.reduce<Record<string, number>>((acc, item) => {
        acc[item.source] = (acc[item.source] || 0) + 1;
        return acc;
      }, {}));
      setImportStates(await getNpcTokenImportStates());
    } catch (e) {
      console.error("Failed to load NPCs data:", e);
    }
  };

  useEffect(() => {
    void loadData();
    return () => tokenUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
  }, []);

  const commitNpcs = async (nextNpcs: Npc[], reason: string, deleteNpcIds: string[] = []) => {
    const campaign = await getActiveCampaign();
    if (!campaign) throw new Error('Nenhuma campanha ativa para gravar NPCs.');
    await saveCanonicalTransaction({ campaign: { ...campaign, updatedAt: Date.now() }, npcs: nextNpcs, deleteNpcIds, reason });
    setNpcs(nextNpcs);
    onNpcChange();
  };

  const replaceNpc = (npc: Npc): Npc[] => {
    const index = npcs.findIndex(item => item.id === npc.id);
    if (index < 0) return [...npcs, npc];
    return npcs.map(item => item.id === npc.id ? npc : item);
  };

  const handleCreateNpc = async () => {
    const id = 'npc_' + Date.now();
    const newNpc: Npc = {
      id,
      name: "Novo Personagem",
      culture: "Desconhecida",
      peoples: "Povos Livres",
      role: "Viajante",
      faction: "Nenhuma",
      appearance: "",
      personality: "",
      attitude: "Neutro",
      objectives: "",
      fears: "",
      secrets: "",
      notes: "",
      trust: 50,
      firstMeeting: "Local da jornada",
      lastMeeting: "Agora",
      location: "Eriador",
      state: "Ativo",
      tokenId: null,
      presentInScene: false,
      voiceProfile: 'Kore'
    };

    const catalog = await getNpcTokenCatalog();
    const withToken = autoAssignNpcToken(newNpc, catalog, npcs);
    await commitNpcs([...npcs, withToken], 'criar NPC com token automático');
    setEditingNpc(withToken);
  };

  const handleSaveNpc = async (npc: Npc) => {
    const catalog = await getNpcTokenCatalog();
    const prepared = npc.tokenAssignmentMode === 'manual' ? npc : autoAssignNpcToken({ ...npc, tokenId: null, tokenAssignmentMode: 'automatic' }, catalog, npcs, true);
    await commitNpcs(replaceNpc(prepared), 'salvar NPC');
    setEditingNpc(null);
  };

  const handleDeleteNpc = async (id: string) => {
    if (!confirm("Deseja realmente banir este personagem dos registros da campanha?")) return;
    const next = npcs.filter(item => item.id !== id);
    await commitNpcs(next, 'apagar NPC', [id]);
    if (editingNpc && editingNpc.id === id) setEditingNpc(null);
  };

  const handleTogglePresence = async (npc: Npc, e: React.MouseEvent) => {
    e.stopPropagation();
    const updated = { ...npc, presentInScene: !npc.presentInScene };
    await commitNpcs(replaceNpc(updated), 'alterar presença de NPC');
  };

  const handleOpenTokenPicker = (npcId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setTargetTokenNpcId(npcId);
    setIsPickerOpen(true);
  };

  const handleSelectToken = async (tokenId: string | null) => {
    if (targetTokenNpcId) {
      // Find NPC
      const targetNpc = npcs.find(n => n.id === targetTokenNpcId);
      if (targetNpc) {
        const updated = { ...targetNpc, tokenId, tokenAssignmentMode: 'manual' as const, tokenMatchScore: undefined, tokenMatchReason: tokenId ? 'Retrato escolhido manualmente.' : 'Sem retrato por escolha manual.', tokenDescriptorId: null, updatedAt: Date.now() };
        await commitNpcs(replaceNpc(updated), 'atribuir retrato manual ao NPC');
        if (editingNpc && editingNpc.id === targetTokenNpcId) {
          setEditingNpc(updated);
        }
      }
    }
    setIsPickerOpen(false);
    setTargetTokenNpcId(null);
  };


  const handleSyncNpcDrive = async () => {
    if (isTokenSyncing) return;
    setIsTokenSyncing(true);
    setTokenStatus('Localizando a subpasta npc no Google Drive...');
    try {
      const summary = await syncNpcTokensFromConfiguredDrive(progress => {
        const count = progress.total ? ` (${progress.current || 0}/${progress.total})` : '';
        setTokenStatus(`${progress.message}${count}`);
      });
      setTokenStatus(`${summary.activeCount} tokens ativos no Drive; ${summary.downloadedCount} baixados, ${summary.unchangedCount} já estavam atualizados e ${summary.assignedNpcCount} NPCs receberam retrato.${summary.failedCount ? ` ${summary.failedCount} arquivo(s) falharam.` : ''}`);
      await loadData();
      onNpcChange();
    } catch (error) {
      setTokenStatus(error instanceof Error ? error.message : 'Falha ao sincronizar a subpasta npc do Google Drive.');
    } finally {
      setIsTokenSyncing(false);
    }
  };

  const handleImportNpcFiles = async (files: File[]) => {
    if (!files.length || isTokenSyncing) return;
    setIsTokenSyncing(true);
    try {
      const summary = await importNpcTokensFromDevice(files, progress => setTokenStatus(progress.message));
      setTokenStatus(`${summary.activeCount} tokens importados do aparelho; ${summary.assignedNpcCount} NPCs receberam retrato.`);
      await loadData();
      onNpcChange();
    } catch (error) {
      setTokenStatus(error instanceof Error ? error.message : 'Falha ao importar os tokens de NPC.');
    } finally {
      setIsTokenSyncing(false);
      if (npcFileInputRef.current) npcFileInputRef.current.value = '';
    }
  };

  const handleReassignAutomatic = async () => {
    const catalog = await getNpcTokenCatalog();
    let updatedCount = 0;
    const recalculated = npcs.map(npc => {
      if (npc.tokenAssignmentMode === 'manual') return npc;
      const updated = autoAssignNpcToken({ ...npc, tokenId: null, tokenAssignmentMode: 'automatic' }, catalog, npcs, true);
      if (updated.tokenId !== npc.tokenId || updated.tokenDescriptorId !== npc.tokenDescriptorId) updatedCount += 1;
      return updated;
    });
    await commitNpcs(recalculated, 'recalcular retratos automáticos dos NPCs');
    setTokenStatus(`${updatedCount} atribuições automáticas foram recalculadas; escolhas manuais foram preservadas.`);
  };

  const handleClearNpcTokens = async () => {
    if (!confirm('Limpar somente a biblioteca de retratos de NPC? A campanha, os NPCs, os tokens dos heróis e o mapa serão preservados. Retratos poderão ser restaurados sincronizando o Drive novamente.')) return;
    await clearNpcTokenLibrary();
    setTokenStatus('Biblioteca de retratos de NPC limpa. A campanha e os demais tokens foram preservados.');
    await loadData();
    onNpcChange();
  };

  const handleAutoAssignOne = async (npc: Npc) => {
    const catalog = await getNpcTokenCatalog();
    const updated = autoAssignNpcToken({ ...npc, tokenId: null, tokenAssignmentMode: 'automatic' }, catalog, npcs, true);
    await commitNpcs(replaceNpc(updated), 'reatribuir token automático');
    setEditingNpc(updated);
  };

  const filteredNpcs = npcs.filter(n => {
    const matchesSearch = n.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          n.culture.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          n.role.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesPresence = filterPresence === 'all' ||
                            (filterPresence === 'present' && n.presentInScene) ||
                            (filterPresence === 'absent' && !n.presentInScene);

    return matchesSearch && matchesPresence;
  });

  return (
    <div className="w-full max-w-6xl mx-auto flex flex-col gap-6 text-blue-100 font-serif pb-12 select-none">
      
      {/* Upper Control Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 mb-2 shrink-0">
        <div className="sm:col-span-6 relative">
          <Search className="w-4 h-4 text-blue-800 absolute left-3 top-3.5" />
          <input
            type="text"
            placeholder="Buscar por nome, cultura ou papel..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="min-h-12 w-full pl-9 pr-4 py-2.5 bg-zinc-950 border border-blue-900/40 rounded text-sm text-blue-100 placeholder-blue-900 focus:outline-none"
          />
        </div>
        <div className="sm:col-span-3">
          <select
            value={filterPresence}
            onChange={(e) => setFilterPresence(e.target.value as any)}
            className="min-h-12 w-full py-2.5 px-3 bg-zinc-950 border border-blue-900/40 rounded text-sm text-blue-300 focus:outline-none cursor-pointer"
          >
            <option value="all">Presença: Todos</option>
            <option value="present">Presentes na Cena</option>
            <option value="absent">Ausentes</option>
          </select>
        </div>
        <div className="sm:col-span-3">
          <button
            onClick={handleCreateNpc}
            className="min-h-12 w-full py-2.5 bg-blue-950/20 hover:bg-blue-600/20 border border-blue-600 text-blue-400 font-bold uppercase tracking-wider text-xs rounded flex items-center justify-center gap-2 cursor-pointer transition-colors font-serif"
          >
            <UserPlus className="w-4 h-4" />
            <span>Cadastrar NPC</span>
          </button>
        </div>
      </div>

      <div className="rounded border border-violet-900/50 bg-violet-950/15 p-3 text-xs text-violet-100">
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div><strong>Biblioteca de NPCs:</strong> {catalogCount} retratos ativos. O app lê <code>tokens_manifest.json</code>, recorta as folhas aprovadas e aplica o firewall visual de O Um Anel antes da seleção automática.</div>
              <div className="mt-1 text-[10px] text-violet-300">
                Folhas recortadas: {catalogBySource['token-sheet'] || 0} · Drive legado: {catalogBySource['google-drive'] || 0} · Aparelho/ZIP: {catalogBySource.device || 0} · Incorporados: {catalogBySource['bundled-npc-folder'] || 0}
              </div>
              {importStates.length > 0 && <div className="mt-1 text-[10px] text-violet-400">Última sincronização: {new Date(Math.max(...importStates.map(item => item.lastSyncAt))).toLocaleString('pt-BR')}</div>}
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" disabled={isTokenSyncing} onClick={() => void handleSyncNpcDrive()} className="min-h-12 flex items-center gap-2 rounded border border-violet-700 bg-violet-950/30 px-3 py-2 text-[11px] font-bold uppercase disabled:opacity-50"><RefreshCw className={`h-3.5 w-3.5 ${isTokenSyncing ? 'animate-spin' : ''}`} /> Sincronizar Google Drive</button>
              <button type="button" disabled={isTokenSyncing} onClick={() => npcFileInputRef.current?.click()} className="min-h-12 flex items-center gap-2 rounded border border-violet-800 bg-zinc-950/40 px-3 py-2 text-[11px] font-bold uppercase disabled:opacity-50"><ImagePlus className="h-3.5 w-3.5" /> ZIP ou aparelho</button>
              <button type="button" disabled={isTokenSyncing || !catalogCount} onClick={() => void handleReassignAutomatic()} className="min-h-12 flex items-center gap-2 rounded border border-blue-800 bg-blue-950/20 px-3 py-2 text-[11px] font-bold uppercase disabled:opacity-50"><RefreshCw className="h-3.5 w-3.5" /> Recalcular automáticos</button>
              <button type="button" disabled={isTokenSyncing || !catalogCount} onClick={() => void handleClearNpcTokens()} className="min-h-12 flex items-center gap-2 rounded border border-red-900 bg-red-950/20 px-3 py-2 text-[11px] font-bold uppercase text-red-300 disabled:opacity-50"><Trash2 className="h-3.5 w-3.5" /> Limpar biblioteca NPC</button>
            </div>
          </div>
          <input ref={npcFileInputRef} type="file" multiple accept=".zip,.json,image/png,image/jpeg,image/webp" className="hidden" onChange={event => void handleImportNpcFiles(Array.from(event.target.files || []))} />
          <p className="text-[10px] text-violet-300">O nome do arquivo fornece povo, gênero, idade, papel, aparência, equipamento e ambiente. <code>npc_manifest.json</code> é opcional. Arquivos novos ou modificados são baixados; os demais permanecem em cache e funcionam sem internet.</p>
          {tokenStatus && <p className="rounded border border-violet-900/40 bg-black/20 p-2 text-[10px] text-violet-200">{tokenStatus}</p>}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left/Middle Column: NPC Grid */}
        <div className={`${editingNpc ? 'lg:col-span-6' : 'lg:col-span-12'} grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4`}>
          {filteredNpcs.length === 0 ? (
            <div className="col-span-full h-64 bg-zinc-950/40 border border-blue-900/20 rounded-md flex flex-col items-center justify-center text-center p-8">
              <User className="w-10 h-10 text-blue-950/60 mb-2" />
              <p className="text-xs font-bold text-blue-700/80 font-serif">Nenhum personagem registrado</p>
              <p className="text-[10px] text-blue-900/40 mt-1 max-w-[280px]">Cadastre aliados, patronos ou monstros e marque-os como presentes para inseri-los na narrativa do mestre.</p>
            </div>
          ) : (
            filteredNpcs.map((npc) => {
              const url = npc.tokenId ? tokens[npc.tokenId] : null;
              return (
                <div
                  key={npc.id}
                  onClick={() => setEditingNpc(npc)}
                  className={`
                    p-3.5 border rounded-md flex flex-col justify-between cursor-pointer transition-all duration-200 relative group
                    ${npc.presentInScene
                      ? 'border-blue-500 bg-blue-950/20 shadow-[0_0_12px_rgba(217,119,6,0.1)]'
                      : 'border-blue-900/30 bg-zinc-950/80 hover:border-blue-700'
                    }
                    ${editingNpc?.id === npc.id ? 'ring-2 ring-blue-500' : ''}
                  `}
                >
                  <div className="flex gap-3">
                    {/* Portrait Frame */}
                    <div
                      onClick={(e) => handleOpenTokenPicker(npc.id, e)}
                      className="relative flex h-14 w-14 max-h-14 max-w-14 shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded-full border-2 border-blue-800/80 bg-zinc-950 shadow-md hover:border-blue-400"
                      title="Atribuir retrato"
                    >
                      {url ? (
                        <img src={url} alt={npc.name} className="block h-14 w-14 max-h-14 max-w-14 object-cover object-center" />
                      ) : (
                        <User className="w-6 h-6 text-blue-800" />
                      )}
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity">
                        <Edit3 className="w-3 h-3 text-blue-100" />
                      </div>
                    </div>

                    <div className="flex-1 min-w-0">
                      <h4 className="text-sm font-bold text-blue-100 truncate pr-6">{npc.name}</h4>
                      <p className="text-[9px] text-blue-700 font-bold uppercase tracking-wider font-serif truncate">
                        {npc.culture} // {npc.role}
                      </p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[10px] text-blue-500 font-semibold">{npc.location}</span>
                        <span className="text-blue-900 text-[10px]">•</span>
                        <span className="text-[10px] text-blue-600/90 italic">{npc.state}</span>
                      </div>
                    </div>
                  </div>

                  {/* Presence indicator and action row */}
                  <div className="border-t border-blue-950/30 mt-3 pt-2.5 flex justify-between items-center text-xs">
                    <button
                      onClick={(e) => handleTogglePresence(npc, e)}
                      className={`
                        flex items-center gap-1 px-2 py-0.5 rounded text-[9px] font-bold tracking-wider font-serif border cursor-pointer transition-colors
                        ${npc.presentInScene
                          ? 'bg-blue-500 text-black border-blue-400 font-bold'
                          : 'bg-zinc-900 text-blue-800 border-blue-950 hover:text-blue-500'
                        }
                      `}
                      title="Clique para alternar presença no cenário do Mestre"
                    >
                      {npc.presentInScene ? <Eye className="w-3 h-3" /> : <EyeOff className="w-3 h-3" />}
                      <span>{npc.presentInScene ? 'PRESENTE' : 'AUSENTE'}</span>
                    </button>

                    <div className="flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={(e) => { e.stopPropagation(); setEditingNpc(npc); }}
                        className="p-1 bg-zinc-900 border border-blue-950 hover:border-blue-600 rounded text-blue-500 cursor-pointer"
                        title="Editar"
                      >
                        <Edit3 className="w-3 h-3" />
                      </button>
                      <button
                        onClick={(e) => { e.stopPropagation(); handleDeleteNpc(npc.id); }}
                        className="p-1 bg-zinc-900 border border-blue-950 hover:border-red-900 text-blue-800 hover:text-red-500 rounded cursor-pointer"
                        title="Excluir"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Right Column: Full Editor Panel */}
        {editingNpc && (
          <div className="lg:col-span-6 bg-zinc-950/95 border-2 border-blue-900 p-5 rounded-md ambient-glow relative flex flex-col gap-4">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-500"></div>
            
            <div className="flex justify-between items-center border-b border-blue-900/40 pb-2">
              <span className="text-xs font-black text-blue-500 font-serif tracking-wider uppercase">
                Editor Detalhado de NPC
              </span>
              <button
                onClick={() => setEditingNpc(null)}
                className="px-2 py-0.5 border border-blue-900 hover:bg-blue-950 text-blue-700 hover:text-blue-400 rounded text-[9px] uppercase font-bold font-serif cursor-pointer"
              >
                Recolher
              </button>
            </div>

            {editingNpc.tokenId && tokens[editingNpc.tokenId] && (
              <div className="flex justify-center">
                <div className="h-24 w-24 max-h-24 max-w-24 overflow-hidden rounded-full border-2 border-violet-700 bg-black">
                  <img src={tokens[editingNpc.tokenId]} alt={editingNpc.name} className="block h-24 w-24 max-h-24 max-w-24 object-cover object-center" />
                </div>
              </div>
            )}

            <div className="rounded border border-violet-900/50 bg-violet-950/15 p-3 text-[10px] text-violet-100">
              <div className="flex items-center justify-between gap-2">
                <div><strong>Retrato:</strong> {editingNpc.tokenAssignmentMode === 'manual' ? 'manual' : editingNpc.tokenId ? 'automático' : 'aguardando token compatível'}{editingNpc.tokenMatchScore != null ? ` · compatibilidade ${editingNpc.tokenMatchScore}` : ''}</div>
                <button type="button" onClick={() => void handleAutoAssignOne(editingNpc)} className="flex items-center gap-1 rounded border border-violet-700 px-2 py-1 font-bold"><ImagePlus className="h-3.5 w-3.5" /> Reatribuir</button>
              </div>
              {editingNpc.tokenMatchReason && <p className="mt-1 text-violet-300">{editingNpc.tokenMatchReason}</p>}
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Nome</label>
                <input
                  type="text"
                  value={editingNpc.name}
                  onChange={(e) => setEditingNpc({ ...editingNpc, name: e.target.value })}
                  className="w-full mt-1 px-2.5 py-1.5 bg-zinc-900 border border-blue-900/30 rounded font-bold"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Cultura</label>
                <input
                  type="text"
                  value={editingNpc.culture}
                  onChange={(e) => setEditingNpc({ ...editingNpc, culture: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Função/Papel</label>
                <input
                  type="text"
                  value={editingNpc.role}
                  onChange={(e) => setEditingNpc({ ...editingNpc, role: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Povoamento</label>
                <input
                  type="text"
                  value={editingNpc.peoples}
                  onChange={(e) => setEditingNpc({ ...editingNpc, peoples: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Facção</label>
                <input
                  type="text"
                  value={editingNpc.faction}
                  onChange={(e) => setEditingNpc({ ...editingNpc, faction: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Localização</label>
                <input
                  type="text"
                  value={editingNpc.location}
                  onChange={(e) => setEditingNpc({ ...editingNpc, location: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Atitude Inicial</label>
                <input
                  type="text"
                  value={editingNpc.attitude}
                  onChange={(e) => setEditingNpc({ ...editingNpc, attitude: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Confiança (1-100)</label>
                <input
                  type="number"
                  value={editingNpc.trust}
                  onChange={(e) => setEditingNpc({ ...editingNpc, trust: parseInt(e.target.value) || 50 })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Estado Atual</label>
                <input
                  type="text"
                  value={editingNpc.state}
                  onChange={(e) => setEditingNpc({ ...editingNpc, state: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded"
                />
              </div>

              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Voz estável do NPC</label>
                <select value={editingNpc.voiceProfile || 'Kore'} onChange={(e) => setEditingNpc({ ...editingNpc, voiceProfile: e.target.value as Npc['voiceProfile'] })} className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded">
                  <option value="Kore">Kore — firme</option><option value="Puck">Puck — animada</option><option value="Fenrir">Fenrir — intensa</option><option value="Aoede">Aoede — leve</option><option value="Gacrux">Gacrux — madura</option><option value="Algenib">Algenib — áspera</option><option value="Sulafat">Sulafat — calorosa</option><option value="Charon">Charon — narrativa</option>
                </select>
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Forma de falar</label>
                <input type="text" value={editingNpc.speechStyle || ''} onChange={(e) => setEditingNpc({ ...editingNpc, speechStyle: e.target.value })} className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded" placeholder="Ex.: voz baixa, frases curtas, humor seco" />
              </div>
              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Emoção atual</label>
                <input type="text" value={editingNpc.emotionalState || ''} onChange={(e) => setEditingNpc({ ...editingNpc, emotionalState: e.target.value })} className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded" />
              </div>
              <div>
                <label className="text-[10px] text-blue-700 font-bold font-serif">Próxima ação planejada</label>
                <input type="text" value={editingNpc.nextAction || ''} onChange={(e) => setEditingNpc({ ...editingNpc, nextAction: e.target.value })} className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded" />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Aparência do Personagem</label>
                <textarea
                  rows={2}
                  value={editingNpc.appearance}
                  onChange={(e) => setEditingNpc({ ...editingNpc, appearance: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded resize-none"
                />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Personalidade & Traços</label>
                <textarea
                  rows={2}
                  value={editingNpc.personality}
                  onChange={(e) => setEditingNpc({ ...editingNpc, personality: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded resize-none"
                />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Objetivos & Medos</label>
                <textarea
                  rows={2}
                  value={editingNpc.objectives}
                  onChange={(e) => setEditingNpc({ ...editingNpc, objectives: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded resize-none"
                />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Segredos do Personagem (Sombra)</label>
                <textarea
                  rows={2}
                  value={editingNpc.secrets}
                  onChange={(e) => setEditingNpc({ ...editingNpc, secrets: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded resize-none text-red-400"
                />
              </div>

              <div className="col-span-2">
                <label className="text-[10px] text-blue-700 font-bold font-serif">Notas do Mestre</label>
                <textarea
                  rows={2}
                  value={editingNpc.notes}
                  onChange={(e) => setEditingNpc({ ...editingNpc, notes: e.target.value })}
                  className="w-full mt-1 px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded resize-none"
                />
              </div>
            </div>

            <div className="border-t border-blue-900/30 pt-3 flex justify-between items-center mt-2">
              <button
                onClick={() => handleDeleteNpc(editingNpc.id)}
                className="px-4 py-1.5 bg-red-950/20 hover:bg-red-950 border border-red-900 hover:border-red-600 text-red-500 hover:text-red-100 rounded text-[10px] uppercase font-bold font-serif cursor-pointer"
              >
                Banir NPC
              </button>
              <button
                onClick={() => handleSaveNpc(editingNpc)}
                className="px-6 py-1.5 bg-blue-950/40 hover:bg-blue-600/20 border border-blue-600 text-blue-400 hover:text-blue-100 rounded text-[10px] uppercase font-bold font-serif cursor-pointer"
              >
                Gravar Registro
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Shared Token Picker Modal for NPCs */}
      <TokenPicker
        isOpen={isPickerOpen}
        onClose={() => setIsPickerOpen(false)}
        selectedTokenId={npcs.find(n => n.id === targetTokenNpcId)?.tokenId || null}
        onSelect={handleSelectToken}
      />

    </div>
  );
};
