from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

game = read("components/GameView.tsx")

type_import = "import type { MasterState, RuleAnswer, CompanyState } from '../lib/master/types';\n"
if "buildNpcPortraitPrompt" not in game:
    if type_import not in game:
        raise SystemExit("GameView import anchor missing")
    game = game.replace(type_import, type_import + "import { buildNpcPortraitPrompt } from '../lib/sceneVisualPrompt';\n", 1)

game = game.replace(
    "  const [npcTokenStatus, setNpcTokenStatus] = useState('');\n",
    "  const [npcTokenStatus, setNpcTokenStatus] = useState('');\n  const [npcPortraitOpen, setNpcPortraitOpen] = useState(false);\n",
    1,
)

share_start = game.find("  const sceneImagePrompt = activeNode?.sceneVisualPrompt")
share_end = game.find("  const importGeneratedImage", share_start)
if share_start < 0 or share_end < 0:
    raise SystemExit("GameView scene share section markers missing")
new_share = """  const sceneImagePrompt = activeNode?.sceneVisualPrompt || activeNode?.visualPrompt || '';

  const sharePromptWithGemini = async (title: string, prompt: string, target: Record<string, unknown>, successMessage: string) => {
    try {
      localStorage.setItem('braseiro.pendingSharedImageTarget', JSON.stringify(target));
      if (window.BraseiroAndroid?.shareText) {
        await window.BraseiroAndroid.shareText(title, prompt);
        setImageShareMessage(successMessage);
      } else if (navigator.share) {
        await navigator.share({ title, text: prompt });
        setImageShareMessage(successMessage);
      } else {
        await navigator.clipboard.writeText(prompt);
        setImageShareMessage('Prompt copiado. Abra o Gemini, gere a imagem e compartilhe-a de volta para O Um Anel.');
      }
    } catch (error) {
      if ((error as DOMException)?.name !== 'AbortError') {
        await navigator.clipboard.writeText(prompt).catch(() => undefined);
        setImageShareMessage('Não foi possível abrir o Gemini. O prompt foi copiado.');
      }
    }
  };

  const sendPromptToGemini = async () => {
    if (!sceneImagePrompt || !activeNode?.id) return;
    const prompt = 'Gere exatamente UMA ilustração horizontal 16:9 para a cena abaixo. Preserve a composição horizontal e deixe o foco principal na área central, pois a imagem será exibida acima do texto do Mestre. Sem texto, legenda, interface, moldura ou colagem.\\n\\n' + sceneImagePrompt;
    await sharePromptWithGemini(
      'Ilustração 16:9 da cena — O Um Anel',
      prompt,
      { kind: 'scene', ...imageTargetFor(activeNode, campaignId) },
      'Prompt da cena aberto no Gemini. Gere em 16:9 e use Compartilhar → O Um Anel; a imagem será ligada automaticamente a esta cena.',
    );
  };

  const sendNpcPortraitPromptToGemini = async (npc: Npc) => {
    const sceneContext = activeNode ? activeNode.location + '. ' + visibleNarration + ' ' + visibleActionResult : npc.location;
    const prompt = buildNpcPortraitPrompt(npc, sceneContext);
    await sharePromptWithGemini(
      'Retrato de ' + npc.name + ' — O Um Anel',
      prompt,
      { kind: 'npc', campaignId, npcId: npc.id, createdAt: Date.now() },
      'Prompt do retrato de ' + npc.name + ' aberto no Gemini. Gere a imagem e use Compartilhar → O Um Anel; ela ficará vinculada permanentemente a este NPC.',
    );
  };

"""
game = game[:share_start] + new_share + game[share_end:]

sync_anchor = """  useEffect(() => {
    setShowSources(false);
  }, [activeNode?.id]);
"""
if sync_anchor not in game:
    raise SystemExit("GameView sync anchor missing")
game = game.replace(sync_anchor, sync_anchor + """
  useEffect(() => {
    if (!selectedNpc) return;
    const refreshed = allNpcs.find(npc => npc.id === selectedNpc.id);
    if (refreshed && (refreshed.tokenId !== selectedNpc.tokenId || refreshed.updatedAt !== selectedNpc.updatedAt)) setSelectedNpc(refreshed);
  }, [allNpcs, selectedNpc?.id, selectedNpc?.tokenId, selectedNpc?.updatedAt]);
""", 1)

immersive_anchor = """      currentImage={currentImage}
      characters={partyCharacters}
      masterState={masterState}
"""
if immersive_anchor not in game:
    raise SystemExit("GameView immersive prop anchor missing")
game = game.replace(immersive_anchor, """      currentImage={currentImage}
      characters={partyCharacters}
      presentNpcs={presentNpcs}
      masterState={masterState}
      onShareSceneImagePrompt={() => void sendPromptToGemini()}
      onShareNpcPortraitPrompt={npc => void sendNpcPortraitPromptToGemini(npc)}
""", 1)

game = game.replace(
    "          onNpcSelect={setSelectedNpc}\n",
    """          onNpcSelect={npc => {
            setSelectedNpc(npc);
            setNpcTokenStatus('');
            if (!npc.tokenId) void sendNpcPortraitPromptToGemini(npc);
          }}
""",
    1,
)

narration_anchor = '            <div className="flex-1 flex flex-col gap-4">\n'
if narration_anchor not in game:
    raise SystemExit("GameView narration panel anchor missing")
scene_panel = """            <div className="flex-1 flex flex-col gap-4">
              {activeNode && (
                <div className="flex flex-col gap-2 rounded border border-blue-900/50 bg-black/25 p-2">
                  <SceneVisualizer imageData={activeImage} isLoading={!isViewingPast && isImageLoading} />
                  {!isViewingPast && (
                    <>
                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                        <button type="button" onClick={() => void sendPromptToGemini()} disabled={!sceneImagePrompt} className="flex min-h-11 items-center justify-center gap-2 rounded border border-violet-500 bg-violet-950/35 px-3 py-2 text-xs font-black text-violet-100 disabled:opacity-40">
                          <WandSparkles className="h-4 w-4" /> Gerar ilustração
                        </button>
                        <button type="button" onClick={() => onRegenerateImage(selectedHistoryIndex !== null ? selectedHistoryIndex : history.length - 1)} disabled={isImageLoading || !activeNode} className="flex min-h-11 items-center justify-center gap-2 rounded border border-blue-800 bg-blue-950/20 px-3 py-2 text-xs font-bold text-blue-200 disabled:opacity-40">
                          {isImageLoading ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                          {isImageLoading ? 'Gerando…' : 'Gerar no app'}
                        </button>
                        <button type="button" onClick={() => imageInputRef.current?.click()} className="flex min-h-11 items-center justify-center gap-2 rounded border border-cyan-800 bg-cyan-950/20 px-3 py-2 text-xs font-bold text-cyan-200">
                          <Upload className="h-4 w-4" /> Escolher imagem
                        </button>
                      </div>
                      <input ref={imageInputRef} type="file" accept="image/*" className="hidden" onChange={event => { void importGeneratedImage(event.target.files?.[0]); event.currentTarget.value = ''; }} />
                      {(imageShareMessage || imageImportStatus || imageError) && (
                        <p className={imageError ? 'rounded bg-red-950/30 p-2 text-[11px] text-red-200' : 'rounded bg-black/40 p-2 text-[11px] text-cyan-200'}>
                          {imageError || imageImportStatus || imageShareMessage}
                        </p>
                      )}
                    </>
                  )}
                </div>
              )}
"""
game = game.replace(narration_anchor, scene_panel, 1)

start_marker = "        {/* Imagem da cena — sempre abaixo das escolhas, com ações visíveis. */}"
end_marker = "        {/* Se estiver no passado, mostrar aviso elegante de impossibilidade de ações */}"
start = game.find(start_marker)
end = game.find(end_marker)
if start < 0 or end < 0 or end <= start:
    raise SystemExit("GameView duplicate image block markers missing")
game = game[:start] + end_marker + game[end + len(end_marker):]

old_portrait_button = """              <button
                type="button"
                onClick={() => { setNpcTokenStatus(''); setIsNpcTokenPickerOpen(true); }}
                className="mb-4 flex w-full items-center gap-3 rounded border border-violet-800/70 bg-violet-950/20 p-3 text-left hover:border-violet-500"
                title="Trocar o retrato deste NPC"
              >"""
new_portrait_button = """              <button
                type="button"
                onClick={() => {
                  setNpcTokenStatus('');
                  if (selectedNpc.tokenId && tokens[selectedNpc.tokenId]) setNpcPortraitOpen(true);
                  else void sendNpcPortraitPromptToGemini(selectedNpc);
                }}
                className="mb-3 flex w-full items-center gap-3 rounded border border-violet-800/70 bg-violet-950/20 p-3 text-left hover:border-violet-500"
                title={selectedNpc.tokenId ? 'Abrir o retrato deste NPC' : 'Gerar retrato deste NPC no Gemini'}
              >"""
if old_portrait_button not in game:
    raise SystemExit("GameView NPC portrait button missing")
game = game.replace(old_portrait_button, new_portrait_button, 1)

old_label = """                  <strong className="block text-sm text-violet-100">Trocar retrato/token</strong>
                  <span className="mt-1 block text-[10px] leading-relaxed text-violet-300">Toque aqui para escolher outra imagem da biblioteca. A escolha será salva para este NPC.</span>"""
new_label = """                  <strong className="block text-sm text-violet-100">{selectedNpc.tokenId ? 'Abrir retrato' : 'Gerar retrato no Gemini'}</strong>
                  <span className="mt-1 block text-[10px] leading-relaxed text-violet-300">{selectedNpc.tokenId ? 'Este retrato pertence ao ID persistente deste NPC e será reutilizado sempre que ele voltar.' : 'O Gemini receberá um prompt individual do peito para cima; ao compartilhar de volta, o retrato será salvo para este NPC.'}</span>"""
if old_label not in game:
    raise SystemExit("GameView NPC portrait label missing")
game = game.replace(old_label, new_label, 1)

status_anchor = '              {npcTokenStatus && <p className="mb-3 rounded border border-emerald-900/60 bg-emerald-950/20 p-2 text-[10px] text-emerald-200">{npcTokenStatus}</p>}\n'
if status_anchor not in game:
    raise SystemExit("GameView NPC status anchor missing")
game = game.replace(status_anchor, """              <div className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                <button type="button" onClick={() => void sendNpcPortraitPromptToGemini(selectedNpc)} className="min-h-10 rounded border border-violet-600 bg-violet-950/35 px-3 text-xs font-bold text-violet-100">
                  {selectedNpc.tokenId ? 'Regenerar no Gemini' : 'Gerar no Gemini'}
                </button>
                <button type="button" onClick={() => { setNpcTokenStatus(''); setIsNpcTokenPickerOpen(true); }} className="min-h-10 rounded border border-slate-700 bg-black/30 px-3 text-xs font-bold text-slate-200">
                  Escolher da biblioteca
                </button>
              </div>
""" + status_anchor, 1)

game = game.replace("onClick={() => setSelectedNpc(null)}", "onClick={() => { setNpcPortraitOpen(false); setSelectedNpc(null); }}", 2)

token_picker_anchor = "        <TokenPicker\n"
if token_picker_anchor not in game:
    raise SystemExit("GameView TokenPicker anchor missing")
portrait_overlay = """        {npcPortraitOpen && selectedNpc?.tokenId && tokens[selectedNpc.tokenId] && (
          <div className="fixed inset-0 z-[90] flex items-center justify-center bg-black/95 p-4" onClick={() => setNpcPortraitOpen(false)}>
            <button type="button" onClick={() => setNpcPortraitOpen(false)} className="absolute right-4 top-4 rounded-full border border-violet-700 bg-black/70 p-3 text-violet-100"><X className="h-5 w-5" /></button>
            <img src={tokens[selectedNpc.tokenId]} alt={'Retrato de ' + selectedNpc.name} className="max-h-[88vh] max-w-full rounded-lg object-contain" />
          </div>
        )}

"""
game = game.replace(token_picker_anchor, portrait_overlay + token_picker_anchor, 1)
write("components/GameView.tsx", game)

imm = read("components/immersive/ImmersiveJourneyView.tsx")
imm = imm.replace(
    "import type { Character, SceneNode } from '../../lib/db';\n",
    "import type { Character, IndividualToken, Npc, SceneNode } from '../../lib/db';\nimport { getStoreData } from '../../lib/db';\n",
    1,
)
imm = imm.replace(
    "  characters: Character[];\n",
    """  characters: Character[];
  presentNpcs: Npc[];
  onShareSceneImagePrompt: () => void | Promise<void>;
  onShareNpcPortraitPrompt: (npc: Npc) => void | Promise<void>;
""",
    1,
)
imm = imm.replace(
    "  const [moreOpen, setMoreOpen] = useState(false);\n",
    """  const [moreOpen, setMoreOpen] = useState(false);
  const [npcTokenUrls, setNpcTokenUrls] = useState<Record<string, string>>({});
  const [selectedNpc, setSelectedNpc] = useState<Npc | null>(null);
""",
    1,
)

asset_effect = "  useEffect(() => { void loadImmersiveAssetManifest().then(setAssets); }, []);\n"
if asset_effect not in imm:
    raise SystemExit("Immersive asset effect missing")
imm = imm.replace(asset_effect, asset_effect + """  useEffect(() => {
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
    return () => { disposed = true; urls.forEach(url => URL.revokeObjectURL(url)); };
  }, [props.presentNpcs.map(npc => npc.id + ':' + (npc.tokenId || '')).join('|')]);
""", 1)

imm = imm.replace(
    "maxHeight: choices.length ? '43dvh' : '34dvh', minHeight: choices.length ? 250 : 190,",
    "maxHeight: '72dvh', minHeight: choices.length ? 390 : 330,",
    1,
)

scroll_anchor = """      <div style={{ flex: '1 1 auto', minHeight: 0, overflowY: 'auto', paddingRight: 3, fontSize: 15, lineHeight: 1.48 }}>
"""
if scroll_anchor not in imm:
    raise SystemExit("Immersive narrative scroll anchor missing")
top_content = """      {node && <div style={{ flex: '0 0 auto', marginBottom: 10 }}>
        <div style={{ position: 'relative', width: '100%', aspectRatio: '16 / 9', overflow: 'hidden', borderRadius: 10, border: '1px solid rgba(93,63,31,.48)', background: 'rgba(31,22,13,.22)' }}>
          {props.currentImage
            ? <img src={props.currentImage} alt="Ilustração da cena" draggable={false} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#795d38', fontSize: 12, fontWeight: 700 }}>Cena sem ilustração</div>}
        </div>
        <button type="button" onClick={() => void props.onShareSceneImagePrompt()} style={{ marginTop: 7, width: '100%', minHeight: 42, borderRadius: 8, border: '1px solid #7b562d', background: 'rgba(91,61,32,.13)', color: '#4b321b', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}>
          {props.currentImage ? 'Trocar ilustração no Gemini' : 'Gerar ilustração 16:9 no Gemini'}
        </button>
      </div>}

      {props.presentNpcs.length > 0 && <div style={{ flex: '0 0 auto', marginBottom: 10 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 800, letterSpacing: '.08em', textTransform: 'uppercase', color: '#735633' }}>NPCs presentes</div>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 3 }}>
          {props.presentNpcs.map(npc => {
            const portrait = npc.tokenId ? npcTokenUrls[npc.tokenId] : null;
            return <button key={npc.id} type="button" onClick={() => {
              setSelectedNpc(npc);
              if (!portrait) void props.onShareNpcPortraitPrompt(npc);
            }} style={{ width: 70, flex: '0 0 70px', border: 0, padding: 0, background: 'transparent', color: '#3d2a18', fontFamily: 'inherit', textAlign: 'center' }}>
              <span style={{ width: 54, height: 54, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: 999, border: '2px solid #9a713e', background: '#21170e' }}>
                {portrait ? <img src={portrait} alt={npc.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: '#ddb879', fontSize: 23 }}>+</span>}
              </span>
              <span style={{ display: 'block', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 10, fontWeight: 800 }}>{npc.name}</span>
            </button>;
          })}
        </div>
      </div>}

"""
imm = imm.replace(scroll_anchor, top_content + scroll_anchor, 1)

nav_anchor = "    <nav style={{\n"
if nav_anchor not in imm:
    raise SystemExit("Immersive nav anchor missing")
npc_modal = """    {selectedNpc && <div onClick={() => setSelectedNpc(null)} style={{ position: 'absolute', inset: 0, zIndex: 130, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18, background: 'rgba(0,0,0,.82)' }}>
      <div onClick={event => event.stopPropagation()} style={{ width: 'min(92vw, 390px)', maxHeight: '84dvh', overflowY: 'auto', borderRadius: 14, border: '1px solid rgba(183,145,91,.62)', background: '#100c08', color: '#ead9b7', padding: 14, boxShadow: '0 20px 50px rgba(0,0,0,.65)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 94, height: 118, overflow: 'hidden', borderRadius: 9, border: '1px solid rgba(183,145,91,.55)', background: '#050403', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {selectedNpc.tokenId && npcTokenUrls[selectedNpc.tokenId]
              ? <img src={npcTokenUrls[selectedNpc.tokenId]} alt={selectedNpc.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              : <span style={{ fontSize: 30, color: '#c59a61' }}>+</span>}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <strong style={{ display: 'block', fontSize: 18 }}>{selectedNpc.name}</strong>
            <span style={{ display: 'block', marginTop: 3, fontSize: 11, opacity: .72 }}>{selectedNpc.role || 'NPC'} · {selectedNpc.culture || selectedNpc.peoples || 'Terra-média'}</span>
            <p style={{ margin: '8px 0 0', fontSize: 11, lineHeight: 1.4, opacity: .82 }}>{selectedNpc.appearance || 'Aparência ainda não descrita.'}</p>
          </div>
        </div>
        <button type="button" onClick={() => void props.onShareNpcPortraitPrompt(selectedNpc)} style={{ width: '100%', minHeight: 44, marginTop: 12, borderRadius: 8, border: '1px solid #936638', background: '#5b3d20', color: '#f6e7c6', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}>
          {selectedNpc.tokenId ? 'Regenerar retrato no Gemini' : 'Gerar retrato no Gemini'}
        </button>
        <button type="button" onClick={() => setSelectedNpc(null)} style={{ width: '100%', minHeight: 40, marginTop: 7, borderRadius: 8, border: '1px solid rgba(183,145,91,.35)', background: 'transparent', color: '#d7bd91', fontFamily: 'inherit', fontSize: 11, fontWeight: 700 }}>Fechar</button>
      </div>
    </div>}

"""
imm = imm.replace(nav_anchor, npc_modal + nav_anchor, 1)
write("components/immersive/ImmersiveJourneyView.tsx", imm)

checks = {
    "scene share 16:9": "ilustração horizontal 16:9" in game,
    "scene above text": game.find("Gerar ilustração") < game.find("Texto da Atmosfera Imersiva"),
    "npc external prompt": "buildNpcPortraitPrompt" in game,
    "npc regenerate": "Regenerar no Gemini" in game,
    "immersive scene image": "Gerar ilustração 16:9 no Gemini" in imm,
    "immersive entities": "NPCs presentes" in imm,
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
if not all(checks.values()):
    raise SystemExit("PATCH_UI_498_FAILED")
print("PATCH_UI_498_OK")
