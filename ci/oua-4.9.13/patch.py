from pathlib import Path
import json, re, shutil, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4913.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")

workspace = Path(__file__).resolve().parent
for src_name, dest in [
    ("geminiModels.ts", "lib/geminiModels.ts"),
    ("SettingsView.tsx", "components/SettingsView.tsx"),
    ("ImmersiveJourneyView.tsx", "components/immersive/ImmersiveJourneyView.tsx"),
]:
    src = workspace / src_name
    if not src.exists():
        raise SystemExit(f"missing 4.9.13 source: {src_name}")
    shutil.copyfile(src, root / dest)

svc_rel = "services/geminiService.ts"
svc = read(svc_rel)
svc = svc.replace(
    "import { getGeminiTextModel } from '../lib/geminiModels';",
    "import { getGeminiTextModel, getGeminiTtsModel } from '../lib/geminiModels';",
)
if "getGeminiTtsModel" not in svc:
    anchor = "import { generateGemini, responseText } from '../lib/geminiTransport';"
    if anchor not in svc:
        raise SystemExit("geminiService transport import anchor missing")
    svc = svc.replace(anchor, anchor + "\nimport { getGeminiTextModel, getGeminiTtsModel } from '../lib/geminiModels';", 1)
svc = svc.replace(
    "const TTS_MODELS = ['gemini-3.1-flash-tts-preview', 'gemini-2.5-flash-preview-tts'];",
    """const DEFAULT_TTS_MODELS = ['gemini-3.1-flash-tts-preview', 'gemini-2.5-flash-preview-tts'];

function getTtsModels(): string[] {
  return Array.from(new Set([getGeminiTtsModel(), ...DEFAULT_TTS_MODELS].map(value => String(value || '').trim()).filter(Boolean)));
}""",
)
old_cinematic = "  const cinematic = shouldUseCinematicModel(publicContext, userAction, false);\n  const models = cinematic ? [getGeminiTextModel('cinematic'), getGeminiTextModel('economy')] : [getGeminiTextModel('economy'), getGeminiTextModel('cinematic')];"
if old_cinematic not in svc:
    old_cinematic = "  const cinematic = shouldUseCinematicModel(publicContext, userAction, false);\n  const models = cinematic ? [CINEMATIC_MODEL, ECONOMY_MODEL] : [ECONOMY_MODEL, CINEMATIC_MODEL];"
new_cinematic = """  const importantScene = /^\\[INICIAR AVENTURA GUIADA\\]/i.test(userAction)
    || /\\b(primeira chegada|chegada importante|mudan[cç]a de local|nova regi[aã]o|revela[cç][aã]o|cl[ií]max|descoberta importante|cena importante)\\b/i.test(userAction + ' ' + publicContext.slice(-2400));
  const cinematic = importantScene || shouldUseCinematicModel(publicContext, userAction, false);
  const models = cinematic
    ? [getGeminiTextModel('cinematic'), getGeminiTextModel('economy')]
    : [getGeminiTextModel('economy'), getGeminiTextModel('cinematic')];"""
if old_cinematic not in svc:
    raise SystemExit("narrator model routing anchor missing")
svc = svc.replace(old_cinematic, new_cinematic, 1)
prompt_anchor = "- Varie extensão e ritmo: rotina curta; exploração média; descoberta/perigo/revelação mais ampla. A diretriz CADÊNCIA DE MESA enviada no contexto tem prioridade sobre qualquer impulso de escrever mais.\n"
if prompt_anchor not in svc:
    raise SystemExit("narrator prompt rhythm anchor missing")
svc = svc.replace(prompt_anchor, prompt_anchor + """- ABERTURA, PRIMEIRA CHEGADA, MUDANÇA DE LOCAL, REVELAÇÃO OU CENA IMPORTANTE: descreva o bastante para o jogador se sentir presente. Mire 4 a 6 parágrafos e aproximadamente 280 a 520 palavras, salvo se o contexto exigir menos. Inclua orientação espacial, luz/visibilidade, temperatura/ar, sons, cheiros ou textura quando fisicamente pertinentes, atividade de pessoas/animais/ambiente e pelo menos um detalhe concreto que ajude a decidir. Não faça lista de sentidos; integre-os organicamente.
- CENAS ROTINEIRAS continuam curtas. O texto longo é padrão apenas quando o jogador precisa construir mentalmente um lugar/situação nova ou importante.
""", 1)
svc = svc.replace(
    "responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: cinematic ? 0.72 : 0.5, maxOutputTokens: 4200,",
    "responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: cinematic ? 0.72 : 0.5, maxOutputTokens: importantScene ? 6800 : 4200,",
    1,
)
return_anchor = "      return normalizeResponse(parseJsonResponse<GeminiStoryResponse>(response as any, 'Narração'), isFrozenReply);"
if return_anchor not in svc:
    raise SystemExit("narrator return anchor missing")
svc = svc.replace(return_anchor, """      const normalized = normalizeResponse(parseJsonResponse<GeminiStoryResponse>(response as any, 'Narração'), isFrozenReply);
      (normalized as any)._braseiroModel = model;
      (normalized as any)._braseiroGenerationSource = 'gemini';
      return normalized;""", 1)
legacy_return = "      return normalizeResponse(parsed);"
if legacy_return in svc:
    svc = svc.replace(legacy_return, """      const normalized = normalizeResponse(parsed);
      (normalized as any)._braseiroModel = model;
      (normalized as any)._braseiroGenerationSource = 'gemini';
      return normalized;""", 1)
svc = svc.replace("for (const model of TTS_MODELS) {", "for (const model of getTtsModels()) {")
svc = svc.replace("${TTS_MODELS.join(',')}", "${getTtsModels().join(',')}")
write(svc_rel, svc)

app_rel = "App.tsx"
app = read(app_rel)
old_minimums = """    quick: { words: 120, paragraphs: 2 },
    balanced: { words: 190, paragraphs: 3 },
    cinematic: { words: 240, paragraphs: 4 },
    investigation: { words: 210, paragraphs: 3 },
    exploration: { words: 210, paragraphs: 3 },"""
new_minimums = """    quick: { words: 200, paragraphs: 3 },
    balanced: { words: 280, paragraphs: 4 },
    cinematic: { words: 360, paragraphs: 5 },
    investigation: { words: 320, paragraphs: 4 },
    exploration: { words: 320, paragraphs: 4 },"""
if old_minimums not in app:
    raise SystemExit("opening minimums anchor missing")
app = app.replace(old_minimums, new_minimums, 1)
old_rag = """      if (selectedManuals.length === 0) {
        throw new Error('Os manuais estão indexados, mas a busca obrigatória não recuperou um trecho aplicável. A cena não foi avançada; refine a ação ou reindexe os manuais.');
      }"""
new_rag = """      if (selectedManuals.length === 0 && !isOpeningScene) {
        throw new Error('Os manuais estão indexados, mas a busca obrigatória não recuperou um trecho aplicável. A cena não foi avançada; refine a ação.');
      }
      if (selectedManuals.length === 0 && isOpeningScene) {
        console.info('[BRASEIRO_RAG] abertura narrativa sem regra aplicável: prosseguindo sem alteração mecânica derivada de manual.');
      }"""
if old_rag not in app:
    raise SystemExit("selectedManuals gate anchor missing")
app = app.replace(old_rag, new_rag, 1)
old_fallback = """      } catch (error) {
        if (!isRecoverableGeminiFailure(error)) throw error;
        localFallbackActive = true;
        plan = createLocalDirectorPlan(base, userAction, selectedManuals as any);
        console.warn('[BRASEIRO_LOCAL_FALLBACK] Diretor local ativado:', error);
      }"""
new_fallback = """      } catch (error) {
        if (!isRecoverableGeminiFailure(error)) throw error;
        if (isOpeningScene) {
          throw new Error('O Gemini não respondeu à abertura. Nenhum texto local curto foi usado no lugar dele. Confira a chave e o modelo de texto em Configurações. Detalhe: ' + (error instanceof Error ? error.message : String(error)));
        }
        localFallbackActive = true;
        plan = createLocalDirectorPlan(base, userAction, selectedManuals as any);
        console.warn('[BRASEIRO_LOCAL_FALLBACK] Diretor local ativado:', error);
      }"""
if old_fallback not in app:
    raise SystemExit("director fallback anchor missing")
app = app.replace(old_fallback, new_fallback, 1)
final_quality_anchor = "      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);"
if final_quality_anchor not in app:
    raise SystemExit("final quality anchor missing")
app = app.replace(final_quality_anchor, """      if (isOpeningScene && !localFallbackActive && openingNarrationNeedsRepair(response, adventureProfile)) {
        response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
[REPARO FINAL DE ABERTURA — TEXTO AINDA CURTO — NÃO MUDE O ESTADO]`, {
          ...plan,
          requiredTest: null,
          narrationBrief: `${plan.narrationBrief || ''}
A abertura ainda está curta para uma mesa remota. Entregue 4 a 6 parágrafos substanciais, com orientação espacial, atividade do mundo, luz/visibilidade, sons, ar/temperatura/cheiro/textura quando pertinentes e detalhes concretos suficientes para o jogador imaginar onde está. Não faça lista sensorial e não avance além da primeira decisão real.`,
        });
      }
      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);""", 1)
node_close = """        sources: selectedManuals.slice(0, 5).map((source, index) => {
          const sourceNumber = index + 1;
          const wasApplied = sourceNumbers.includes(sourceNumber);
          return {
            ...source,
            usage: wasApplied
              ? (plan.ruleSummary || response.sourceUsageSummary || source.usage || 'Regra consultada e aplicada nesta resolução.')
              : 'Fonte consultada obrigatoriamente antes da resolução; nenhuma alteração mecânica foi derivada deste trecho.',
          };
        }),
      };"""
if node_close not in app:
    raise SystemExit("newNode close anchor missing")
app = app.replace(node_close, node_close + """
      Object.assign(newNode.narrativeMeta as any, {
        generationSource: localFallbackActive ? 'local-fallback' : String((response as any)._braseiroGenerationSource || 'gemini'),
        generationModel: localFallbackActive ? 'mestre-local' : String((response as any)._braseiroModel || ''),
        narrationWordCount: String(response.atmospheric_narration || '').trim().split(/\\s+/).filter(Boolean).length,
      });""", 1)
write(app_rel, app)

game_rel = "components/GameView.tsx"
game = read(game_rel)
anchor = """      isStoryLoading={isStoryLoading}
      pendingTest={pendingTest}"""
if anchor not in game:
    raise SystemExit("GameView immersive loading anchor missing")
game = game.replace(anchor, """      isStoryLoading={isStoryLoading}
      storyError={storyError}
      onDismissStoryError={onDismissStoryError}
      pendingTest={pendingTest}""", 1)
write(game_rel, game)

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)
toast_anchor = """        <div className={`absolute bottom-4 left-4 right-4 z-[100] rounded-lg border px-4 py-3 shadow-2xl backdrop-blur-md transition-all sm:left-auto sm:right-4 sm:w-80 ${"""
if toast_anchor not in emap:
    raise SystemExit("map toast anchor missing")
emap = emap.replace(toast_anchor, """        <div style={immersive ? { bottom: 92, zIndex: 115 } : undefined} className={`absolute bottom-4 left-4 right-4 z-[100] rounded-lg border px-4 py-3 shadow-2xl backdrop-blur-md transition-all sm:left-auto sm:right-4 sm:w-80 ${""", 1)
write(map_rel, emap)

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.13"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.12"', '"version": "4.9.13"', t, count=2)
    lock.write_text(t, encoding="utf-8")
version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.12'", "APP_VERSION = '4.9.13'", version)
version = re.sub(r"APP_VERSION_CODE = 49012", "APP_VERSION_CODE = 49013", version)
write(version_rel, version)
gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49012", "versionCode = 49013", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.12"', 'versionName = "4.9.13"', gradle)
write(gradle_rel, gradle)
main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49012&immersive=1", "index.html?v=49013&immersive=1")
write(main_rel, main)

imm = read("components/immersive/ImmersiveJourneyView.tsx")
settings = read("components/SettingsView.tsx")
svc = read(svc_rel)
app = read(app_rel)
emap = read(map_rel)
checks = {
    "manual text model": "Texto principal / cenas importantes — ID manual" in settings,
    "manual tts model": "TTS — ID manual" in settings and "Testar modelo TTS com Charon" in settings,
    "tts runtime override": "getGeminiTtsModel" in svc and "getTtsModels()" in svc,
    "important scene main model": "const importantScene" in svc and "maxOutputTokens: importantScene ? 6800 : 4200" in svc,
    "opening no silent local fallback": "Nenhum texto local curto foi usado" in app,
    "opening rag safe": "abertura narrativa sem regra aplicável" in app,
    "opening depth": "balanced: { words: 280, paragraphs: 4 }" in app and "REPARO FINAL DE ABERTURA" in app,
    "provenance": "generationModel" in app and "generationModel" in imm,
    "auto open after start": "await props.onStartAdventure();" in imm and "startAdventureFromMap" in imm,
    "footprint pre-start party post-start": 'partyMarkerMode={node && props.characters.length ? "none" : "footprints"}' in imm,
    "story error visible": "Falha ao iniciar/continuar" in imm and "storyError={storyError}" in game,
    "map toast above nav": "bottom: 92, zIndex: 115" in emap,
    "version": "4.9.13" in read(version_rel) and "49013" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4913_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.13_OK")
