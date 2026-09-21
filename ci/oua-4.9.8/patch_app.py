from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_app.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

app = read("App.tsx")

app = app.replace(
    "CampaignState, Character, CharacterSheet, getActiveCampaign, getCharacters, getStoreData, listSoundMetadata, Npc, PendingTest,",
    "CampaignState, Character, CharacterSheet, getActiveCampaign, getCharacters, getStoreData, IndividualToken, listSoundMetadata, Npc, PendingTest,",
    1,
)
import_anchor = "import { assessNarrativeRhythm, buildNarrativeRhythmPlan, narrativeRhythmPrompt, rhythmRepairBrief } from './lib/narrativeRhythm';\n"
if import_anchor not in app:
    raise SystemExit("App import anchor missing")
if "sceneEntitySync" not in app:
    app = app.replace(import_anchor, import_anchor + """import { reconcileSceneNpcPresence, synchronizeNpcPresenceFlags } from './lib/sceneEntitySync';
import { buildStoredSceneVisualPrompt } from './lib/sceneVisualPrompt';
""", 1)

app = app.replace("      const mechanicallyUpdatedNpcs = narrativeIntent.advancesFiction\n", "      let mechanicallyUpdatedNpcs = narrativeIntent.advancesFiction\n", 1)

events_anchor = """      if (usedRoll) events.push(makeEvent('test_resolved', sceneId, {
        skill: base.pendingTest!.skill,
        success: base.pendingDiceRoll!.success ?? false,
        characterId: base.pendingDiceRoll!.characterId || base.pendingTest!.characterId || base.activeCharacterId || '',
      }));
      const sourceNumbers = Array.from(new Set([...(response.usedSourceNumbers || []), ...(plan.ruleSourceNumbers || [])]));
"""
if events_anchor not in app:
    raise SystemExit("App scene events anchor missing")
events_replacement = """      if (usedRoll) events.push(makeEvent('test_resolved', sceneId, {
        skill: base.pendingTest!.skill,
        success: base.pendingDiceRoll!.success ?? false,
        characterId: base.pendingDiceRoll!.characterId || base.pendingTest!.characterId || base.activeCharacterId || '',
      }));
      const synchronizedNpcIds = narrativeIntent.advancesFiction
        ? reconcileSceneNpcPresence({
            canonicalIds: mechanicallyUpdated.presentNpcIds || [],
            npcs: mechanicallyUpdatedNpcs,
            narration: response.atmospheric_narration || '',
            actionResult: response.action_result || '',
            voiceScript: response.voiceScript || [],
            events,
            location: canonicalLocation,
          })
        : mechanicallyUpdated.presentNpcIds || [];
      if (narrativeIntent.advancesFiction) {
        mechanicallyUpdatedNpcs = synchronizeNpcPresenceFlags(mechanicallyUpdatedNpcs, synchronizedNpcIds);
        mechanicallyUpdated = { ...mechanicallyUpdated, presentNpcIds: synchronizedNpcIds, updatedAt: Date.now() };
      }
      const sceneVisualPrompt = buildStoredSceneVisualPrompt({
        modelPrompt: response.sceneVisualPrompt || response.visualPrompt,
        narration: response.atmospheric_narration,
        actionResult: response.action_result,
        location: canonicalLocation,
        weather: environmentBinding.weatherLabel,
        timeOfDay: environmentBinding.period,
        presentNpcs: mechanicallyUpdatedNpcs.filter(npc => synchronizedNpcIds.includes(npc.id)),
      });
      const sourceNumbers = Array.from(new Set([...(response.usedSourceNumbers || []), ...(plan.ruleSourceNumbers || [])]));
"""
app = app.replace(events_anchor, events_replacement, 1)

app = app.replace(
    "        sceneVisualPrompt: response.sceneVisualPrompt,\n",
    "        sceneVisualPrompt,\n        sceneVisualPromptCreatedAt: Date.now(),\n",
    1,
)
app = app.replace(
    "        presentNpcIds: mechanicallyUpdated.presentNpcIds || [],\n",
    "        presentNpcIds: synchronizedNpcIds,\n",
    1,
)
app = app.replace("        newNode.imageAspect = '9:16';", "        newNode.imageAspect = '16:9';", 1)
updated_anchor = """        ...memory,
        characterSheet: activeAfterMechanics?.sheet || mechanicallyUpdated.characterSheet,
"""
if updated_anchor not in app:
    raise SystemExit("App updated campaign anchor missing")
app = app.replace(updated_anchor, """        ...memory,
        presentNpcIds: newNode.presentNpcIds,
        characterSheet: activeAfterMechanics?.sheet || mechanicallyUpdated.characterSheet,
""", 1)

app = app.replace("imageAspect: '9:16' as const", "imageAspect: '16:9' as const", 1)

helper_anchor = "async function compressSceneImage(source: string): Promise<string> {\n"
if helper_anchor not in app:
    raise SystemExit("shared image helper anchor missing")
if "async function sharedImageToBlob" not in app:
    app = app.replace(helper_anchor, """async function sharedImageToBlob(source: string): Promise<Blob> {
  if (!isImportableSharedImage(source)) throw new Error('A imagem compartilhada não é válida.');
  const response = await fetch(source);
  if (!response.ok) throw new Error('O Android não conseguiu ler a imagem compartilhada.');
  const blob = await response.blob();
  if (!blob.size || (blob.type && !blob.type.startsWith('image/'))) throw new Error('O conteúdo compartilhado não é uma imagem válida.');
  return blob;
}

""" + helper_anchor, 1)

import_scene_anchor = "  const handleImportSceneImage = async (dataUrl: string, index = -1) => {\n"
if import_scene_anchor not in app:
    raise SystemExit("handleImportSceneImage anchor missing")
if "const handleImportNpcPortrait" not in app:
    npc_import = """  const handleImportNpcPortrait = async (dataUrl: string, npcId: string) => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const latestNpcs = allNpcs.length ? allNpcs : await getStoreData<Npc>('npcs');
    const target = latestNpcs.find(npc => npc.id === npcId);
    if (!target) throw new Error('O NPC solicitado não existe mais.');

    const blob = await sharedImageToBlob(dataUrl);
    const safeNpcId = npcId.replace(/[^A-Za-z0-9._-]+/g, '_');
    const tokenId = 'npc_portrait_' + safeNpcId;
    const previous = (await getStoreData<IndividualToken>('individual_tokens').catch(() => []))
      .find(token => token.id === tokenId);
    const token: IndividualToken = {
      id: tokenId,
      sheetId: 'npc:' + npcId,
      tokenIndex: 0,
      blob,
      category: 'npc-generated',
      createdAt: previous?.createdAt || Date.now(),
    };
    await saveStoreData('individual_tokens', token, 'retrato Gemini persistente de NPC');

    const updatedNpc: Npc = {
      ...target,
      tokenId,
      tokenAssignmentMode: 'manual',
      tokenMatchScore: undefined,
      tokenMatchReason: 'Retrato individual recebido do Gemini e vinculado ao ID persistente do NPC.',
      tokenDescriptorId: null,
      portraitUpdatedAt: Date.now(),
      updatedAt: Date.now(),
    };
    const updatedNpcs = latestNpcs.map(npc => npc.id === npcId ? updatedNpc : npc);
    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, updatedAt: Date.now() },
      npcs: updatedNpcs,
      reason: 'retrato persistente do NPC ' + target.name,
    });
    campaignRef.current = persisted;
    setCampaign(persisted);
    setAllNpcs(updatedNpcs);
    setPresentNpcs(updatedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
    setImageImportStatus('Retrato de ' + target.name + ' recebido, salvo e vinculado ao NPC.');
  };

"""
    app = app.replace(import_scene_anchor, npc_import + import_scene_anchor, 1)

old_target = """        let targetIndex = -1;
        try {
          const target = JSON.parse(localStorage.getItem('braseiro.pendingSharedImageTarget') || 'null') as { campaignId?: string; sceneId?: string; fingerprint?: string; createdAt?: number } | null;
          if (target?.campaignId === active.id && target.sceneId) {
            const found = active.history.findIndex(node => node.id === target.sceneId);
            if (found >= 0 && targetMatchesScene(target, active.id, active.history[found])) targetIndex = found;
          }
        } catch { targetIndex = -1; }
        if (targetIndex < 0) throw new Error('A imagem recebida não pertence à cena atual ou o pedido expirou. Gere novamente a partir desta cena.');
        await handleImportSceneImage(dataUrl, targetIndex);
"""
new_target = """        let targetIndex = -1;
        let npcTargetId = '';
        try {
          const target = JSON.parse(localStorage.getItem('braseiro.pendingSharedImageTarget') || 'null') as {
            kind?: 'scene' | 'npc'; campaignId?: string; sceneId?: string; npcId?: string; fingerprint?: string; createdAt?: number;
          } | null;
          if (target?.campaignId === active.id && target.kind === 'npc' && target.npcId) {
            npcTargetId = target.npcId;
          } else if (target?.campaignId === active.id && target.sceneId) {
            const found = active.history.findIndex(node => node.id === target.sceneId);
            if (found >= 0 && targetMatchesScene(target, active.id, active.history[found])) targetIndex = found;
          }
        } catch { targetIndex = -1; npcTargetId = ''; }
        if (npcTargetId) await handleImportNpcPortrait(dataUrl, npcTargetId);
        else {
          if (targetIndex < 0) throw new Error('A imagem recebida não pertence à cena ou ao NPC solicitado, ou o pedido expirou. Gere novamente a partir do destino correto.');
          await handleImportSceneImage(dataUrl, targetIndex);
        }
"""
if old_target not in app:
    raise SystemExit("shared image target routing anchor missing")
app = app.replace(old_target, new_target, 1)

write("App.tsx", app)

checks = {
    "entity sync": "reconcileSceneNpcPresence" in app,
    "scene prompt stored": "sceneVisualPromptCreatedAt: Date.now()" in app,
    "16:9 metadata": "newNode.imageAspect = '16:9'" in app,
    "npc stable token": "'npc_portrait_' + safeNpcId" in app,
    "npc import route": "handleImportNpcPortrait(dataUrl, npcTargetId)" in app,
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
if not all(checks.values()):
    raise SystemExit("PATCH_APP_498_FAILED")
print("PATCH_APP_498_OK")
