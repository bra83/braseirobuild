from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4918.py <project-root>")
root = Path(sys.argv[1]).resolve()
BT = chr(96)

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# ONE text model: migrate only the old principal value; economy is never used.
models_rel = "lib/geminiModels.ts"
write(models_rel, """export const GEMINI_TEXT_MODEL_OPTIONS = [
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash — Mestre' },
  { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash' },
  { id: 'gemini-3.7-flash', label: 'Gemini 3.7 Flash' },
  { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash' },
  { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash' },
  { id: 'gemini-flash-latest', label: 'Gemini Flash Latest — alias' },
] as const;

export const GEMINI_TTS_MODEL_OPTIONS = [
  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },
  { id: 'gemini-2.5-flash-preview-tts', label: 'Gemini 2.5 Flash Preview TTS — compatibilidade' },
  { id: 'gemini-2.5-pro-preview-tts', label: 'Gemini 2.5 Pro Preview TTS' },
] as const;

const TEXT_KEY = 'braseiro.geminiModel.text';
const LEGACY_PRINCIPAL_KEY = 'braseiro.geminiModel.cinematic';
const LEGACY_ECONOMY_KEY = 'braseiro.geminiModel.economy';
const TEXT_DEFAULT = 'gemini-3.5-flash';
const TTS_KEY = 'braseiro.geminiModel.tts';
const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';

function cleanModel(value: unknown): string {
  return String(value || '').trim().replace(/^models\\//, '');
}

export function getGeminiTextModel(): string {
  if (typeof window === 'undefined') return TEXT_DEFAULT;
  const current = cleanModel(window.localStorage.getItem(TEXT_KEY));
  if (current) return current;
  const migrated = cleanModel(window.localStorage.getItem(LEGACY_PRINCIPAL_KEY));
  if (migrated) {
    window.localStorage.setItem(TEXT_KEY, migrated);
    return migrated;
  }
  return TEXT_DEFAULT;
}

export function saveGeminiTextModel(model: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TEXT_KEY, cleanModel(model) || TEXT_DEFAULT);
  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);
  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);
}

export function getGeminiTtsModel(): string {
  if (typeof window === 'undefined') return TTS_DEFAULT;
  return cleanModel(window.localStorage.getItem(TTS_KEY)) || TTS_DEFAULT;
}

export function saveGeminiTtsModel(model: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TTS_KEY, cleanModel(model) || TTS_DEFAULT);
}

export function resetGeminiModels(): void {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(TEXT_KEY);
  window.localStorage.removeItem(LEGACY_PRINCIPAL_KEY);
  window.localStorage.removeItem(LEGACY_ECONOMY_KEY);
  window.localStorage.removeItem(TTS_KEY);
}
""")

# Settings: one Master model field. Use structural regexes because the 4.9.17
# source can differ slightly in spacing from the older snapshot.
settings_rel = "components/SettingsView.tsx"
settings = read(settings_rel)

# State: delete economy entirely and rename the former principal state.
settings = re.sub(
    r"^\s*const \[economyModel,\s*setEconomyModel\]\s*=\s*useState\([^\n]*\);\s*$\n?",
    "",
    settings,
    count=1,
    flags=re.M,
)
settings = re.sub(
    r"^\s*const \[cinematicModel,\s*setCinematicModel\]\s*=\s*useState\([^\n]*\);\s*$",
    "  const [textModel, setTextModel] = useState(() => getGeminiTextModel());",
    settings,
    count=1,
    flags=re.M,
)

# Saving: remove economy route; principal becomes the single text route.
settings = re.sub(
    r"^\s*saveGeminiTextModel\(\s*['\"]economy['\"]\s*,\s*economyModel\s*\);\s*$\n?",
    "",
    settings,
    count=1,
    flags=re.M,
)
settings = re.sub(
    r"^\s*saveGeminiTextModel\(\s*['\"]cinematic['\"]\s*,\s*cinematicModel\s*\);\s*$",
    "    saveGeminiTextModel(textModel);",
    settings,
    count=1,
    flags=re.M,
)

# Remove the whole economy field regardless of className/spacing drift.
settings = re.sub(
    r'\s*<label[^>]*>\s*Texto econ[oô]mico\s*—\s*ID manual\s*</label>\s*<input[\s\S]*?value=\{economyModel\}[\s\S]*?/>\s*',
    "\n",
    settings,
    count=1,
    flags=re.I,
)

# Former principal field becomes the only Master field.
settings = settings.replace('Texto principal / cenas importantes — ID manual', 'Modelo do Mestre — texto — ID manual')
settings = settings.replace("cinematicModel", "textModel")
settings = settings.replace("setCinematicModel", "setTextModel")
settings = re.sub(r"getGeminiTextModel\(\s*['\"]cinematic['\"]\s*\)", "getGeminiTextModel()", settings)

# Reset handler: economy line is deleted; principal/text is kept once.
settings = re.sub(
    r"^\s*setEconomyModel\([^\n]*\);\s*$\n?",
    "",
    settings,
    flags=re.M,
)
settings = re.sub(
    r"setTextModel\(getGeminiTextModel\(\)\);(?:\s*setTextModel\(getGeminiTextModel\(\)\);)+",
    "setTextModel(getGeminiTextModel());",
    settings,
)

settings = settings.replace(
    "Aberturas, mudanças de local, revelações e cenas importantes usam o modelo principal e recebem uma meta narrativa maior. Cena rotineira continua compacta.",
    "Todas as cenas usam o mesmo modelo do Mestre. A narração segue o Motor Barbara: cinematográfica, substancial, sensorial e falada como Mestre para os jogadores, sem decidir ações pela Companhia.",
)

# If any economy UI/runtime symbol survived, remove only its own line/block
# rather than silently shipping a second route.
survivors = [token for token in ("economyModel", "setEconomyModel", "Texto econômico", "Texto econômico") if token in settings]
if survivors:
    raise SystemExit("dual text-model UI survived: " + ", ".join(survivors))
if "const [textModel, setTextModel]" not in settings or "saveGeminiTextModel(textModel)" not in settings:
    raise SystemExit("single Master text-model state/save missing")
write(settings_rel, settings)

# Service: collapse all role lookups and text fallback routing to one selected model.
svc_rel = "services/geminiService.ts"
svc = read(svc_rel)
svc = re.sub(r"getGeminiTextModel\(\s*['\"](?:economy|cinematic)['\"]\s*\)", "getGeminiTextModel()", svc)
svc = re.sub(r"\nconst ECONOMY_MODEL = ['\"][^'\"]+['\"];\nconst CINEMATIC_MODEL = ['\"][^'\"]+['\"];", "", svc, count=1)
svc = re.sub(
    r"\nfunction shouldUseCinematicModel\([\s\S]*?\n\}\n\nfunction errorText",
    "\nfunction errorText",
    svc,
    count=1,
)
svc = svc.replace("generateGemini(ECONOMY_MODEL,", "generateGemini(getGeminiTextModel(),")
svc = svc.replace("generateGemini(CINEMATIC_MODEL,", "generateGemini(getGeminiTextModel(),")

# Any text model array becomes one model. TTS/image arrays are untouched.
svc = re.sub(
    r"const models = cinematic\s*\? \[getGeminiTextModel\(\), getGeminiTextModel\(\)\]\s*:\s*\[getGeminiTextModel\(\), getGeminiTextModel\(\)\];",
    "const models = [getGeminiTextModel()];",
    svc,
)
svc = re.sub(
    r"const models = isDmMode\s*\?[\s\S]*?\s*:\s*\[getGeminiTextModel\(\), getGeminiTextModel\(\)\];",
    "const models = [getGeminiTextModel()];",
    svc,
    count=1,
)
# Prior 4.9.13 narrator routing with importantScene.
svc = re.sub(
    r"const importantScene = [\s\S]*?const cinematic = importantScene \|\| shouldUseCinematicModel\([^;]+;\s*const models = [^;]+;",
    "const importantScene = true;\n  const cinematic = true;\n  const models = [getGeminiTextModel()];",
    svc,
    count=1,
)
# Legacy story cinematic classifier, if still present.
svc = re.sub(
    r"const cinematic = shouldUseCinematicModel\([^;]+;\s*const models = [^;]+;",
    "const cinematic = !isDmMode;\n  const models = [getGeminiTextModel()];",
    svc,
    count=1,
)

svc = svc.replace(
"responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: cinematic ? 0.72 : 0.5, maxOutputTokens: importantScene ? 6800 : 4200,",
"responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: 0.78, maxOutputTokens: 6800,",
)
svc = svc.replace(
"responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: cinematic ? 0.72 : 0.5, maxOutputTokens: 4200,",
"responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: 0.78, maxOutputTokens: 6800,",
)

old = "- Varie extensão e ritmo: rotina curta; exploração média; descoberta/perigo/revelação mais ampla. A diretriz CADÊNCIA DE MESA enviada no contexto tem prioridade sobre qualquer impulso de escrever mais.\n"
new = """- Toda resposta que AVANÇA A FICÇÃO deve ser substancial: normalmente 4 a 6 parágrafos e aproximadamente 280 a 520 palavras. Abertura, primeira chegada, investigação, descoberta, perigo ou revelação podem chegar a 5 a 7 parágrafos e 380 a 650 palavras. Nunca ultrapasse aproximadamente 4.800 caracteres em atmospheric_narration.
- Fale como um Mestre sentado à mesa: dirija-se aos jogadores/à Companhia em segunda pessoa, descreva o que eles veem, ouvem e sentem no espaço, reaja diretamente ao verbo da ação declarada e pare antes de decidir a próxima ação por eles. Cinematográfico não significa romance autônomo.
- SENSORIALIDADE ORGÂNICA: integre luz, distância, temperatura, vento/ar, sons, cheiro, textura, peso, esforço e movimento apenas quando fisicamente pertinentes. Nunca faça uma lista dos cinco sentidos.
- NÃO REPITA a narração anterior. O checkpoint contém o final da cena anterior apenas para continuidade. A nova atmospheric_narration deve começar pela consequência/reação da AÇÃO ATUAL e acrescentar informação, movimento, resposta, descoberta ou mudança perceptível nova.
"""
if old in svc:
    svc = svc.replace(old, new, 1)
else:
    anchor = "- Responda todas as perguntas pertinentes contidas na fala do jogador.\n"
    if anchor not in svc:
        raise SystemExit("narrator prompt anchor missing")
    svc = svc.replace(anchor, anchor + new, 1)

svc = svc.replace(
"- atmospheric_narration: tamanho adaptativo. Continuação social ou rotina: 1 a 3 parágrafos; exploração: 2 a 4; abertura de capítulo, revelação ou perigo importante: 4 a 6. Parágrafos reais, com extensão variada conforme o ritmo. Não estique uma ação simples só para atingir contagem.",
"- atmospheric_narration: quando a ficção avança, entregue uma fala de Mestre substancial e sensorial em 4 a 6 parágrafos (5 a 7 em abertura/revelação importante), normalmente 280 a 520 palavras e sempre abaixo de ~4.800 caracteres. Cada parágrafo responde ao estado real do mundo e à ação atual; nunca recarregue a narração anterior.",
)
write(svc_rel, svc)

# Rhythm: richer text, same agency/beat limits.
rhythm_rel = "lib/narrativeRhythm.ts"
rhythm = read(rhythm_rel)
pairs = [
("targetParagraphMin: 3, targetParagraphMax: 5,\n      targetWordMin: 210, targetWordMax: 430,", "targetParagraphMin: 5, targetParagraphMax: 7,\n      targetWordMin: 380, targetWordMax: 650,"),
("targetParagraphMin: 1, targetParagraphMax: 2,\n      targetWordMin: 55, targetWordMax: 180,", "targetParagraphMin: 4, targetParagraphMax: 6,\n      targetWordMin: 280, targetWordMax: 440,"),
("targetParagraphMin: 1, targetParagraphMax: 3,\n      targetWordMin: 90, targetWordMax: 240,", "targetParagraphMin: 4, targetParagraphMax: 6,\n      targetWordMin: 300, targetWordMax: 480,"),
("targetParagraphMin: 1, targetParagraphMax: 3,\n      targetWordMin: 90, targetWordMax: 260,", "targetParagraphMin: 4, targetParagraphMax: 6,\n      targetWordMin: 320, targetWordMax: 520,"),
("targetParagraphMin: 1, targetParagraphMax: 2,\n      targetWordMin: 55, targetWordMax: 175,", "targetParagraphMin: 3, targetParagraphMax: 5,\n      targetWordMin: 240, targetWordMax: 400,"),
("targetParagraphMin: 1, targetParagraphMax: 3,\n      targetWordMin: 80, targetWordMax: 230,", "targetParagraphMin: 4, targetParagraphMax: 6,\n      targetWordMin: 280, targetWordMax: 460,"),
("targetParagraphMin: 2, targetParagraphMax: 4,\n      targetWordMin: 130, targetWordMax: 330,", "targetParagraphMin: 4, targetParagraphMax: 6,\n      targetWordMin: 340, targetWordMax: 600,"),
("targetParagraphMin: 2, targetParagraphMax: 4,\n    targetWordMin: 120, targetWordMax: 320,", "targetParagraphMin: 4, targetParagraphMax: 6,\n    targetWordMin: 320, targetWordMax: 560,"),
]
for a,b in pairs:
    if a not in rhythm:
        raise SystemExit("rhythm anchor missing: " + a.splitlines()[0])
    rhythm = rhythm.replace(a,b,1)

rhythm = rhythm.replace(
"  if (plan.cadence === 'conversation' && words > 210) flags.push('interação social simples ficou longa demais antes da réplica do jogador');",
"  if (plan.cadence === 'conversation' && words > 520) flags.push('interação social ultrapassou a janela de fala do Mestre antes da réplica do jogador');",
)
rhythm = rhythm.replace(
"  if (plan.cadence === 'combat' && words > 210) flags.push('combate narrou mais do que a ação atual deveria resolver');",
"  if (plan.cadence === 'combat' && words > 460) flags.push('combate narrou mais do que a ação atual deveria resolver');",
)
anchor = """  const hardWordMax = Math.max(plan.targetWordMax + 80, Math.round(plan.targetWordMax * 1.35));
  const hardParagraphMax = plan.targetParagraphMax + 1;"""
replacement = """  const hardWordMax = Math.max(plan.targetWordMax + 100, Math.round(plan.targetWordMax * 1.30));
  const hardParagraphMax = plan.targetParagraphMax + 1;
  const softWordMin = Math.max(1, Math.round(plan.targetWordMin * 0.78));
  if (words < softWordMin) flags.push('narração curta demais para a cadência: ' + words + ' palavras; mínimo substancial ' + softWordMin);
  if (paragraphs < Math.max(1, plan.targetParagraphMin - 1)) flags.push('narração com poucos parágrafos para a presença de mesa: ' + paragraphs);"""
if anchor not in rhythm:
    raise SystemExit("rhythm assessment anchor missing")
rhythm = rhythm.replace(anchor, replacement, 1)
rhythm = rhythm.replace(
"    'A faixa serve para controlar ritmo e impedir monólogo. Se a ação já estiver resolvida em menos texto, pare antes. Se uma revelação realmente exigir mais espaço, expanda somente uma batida e devolva agência.',",
"    'Use a faixa para dar presença, imagem, sensação e reação sem adicionar acontecimentos que o jogador ainda poderia interromper. Texto maior descreve melhor a mesma batida; não toma decisões pela Companhia.',",
)
write(rhythm_rel, rhythm)

# Repetition detector.
engine_rel = "lib/narrativeEngine.ts"
engine = read(engine_rel)
anchor = """function wordSet(value: string): Set<string> {
  return new Set(normalize(value).split(/\\s+/).filter(word => word.length > 3).slice(0, 45));
}"""
replacement = anchor + """

function sceneWordSet(value: string): Set<string> {
  return new Set(normalize(value).split(/\\s+/).filter(word => word.length > 3).slice(0, 220));
}"""
if anchor not in engine:
    raise SystemExit("wordSet anchor missing")
engine = engine.replace(anchor, replacement, 1)

marker = "  if (isOpening && signature) {"
if marker not in engine:
    raise SystemExit("quality marker missing")
insert = """  const previousFiction = latestFictionScene(campaign);
  if (previousFiction?.atmospheric_narration && narration) {
    const previousNormalized = normalize(previousFiction.atmospheric_narration);
    const similarity = jaccard(sceneWordSet(narration), sceneWordSet(previousFiction.atmospheric_narration));
    if (normalize(narration) === previousNormalized || similarity >= 0.78) {
      flags.push('narração repetiu substancialmente a cena anterior em vez de responder à nova ação');
    }
  }

"""
engine = engine.replace(marker, insert + marker, 1)
write(engine_rel, engine)

# Two repair attempts + final block against committing repeated text.
app_rel = "App.tsx"
app = read(app_rel)
app = app.replace(
"        if (quality.repair) {",
"        for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2; qualityAttempt += 1) {",
1,
)
app = app.replace(
"[REPARO DE QUALIDADE — PRESERVE O ESTADO]",
"[REPARO DE QUALIDADE — PRESERVE O ESTADO — NÃO REPITA A CENA ANTERIOR]",
1,
)
app = app.replace(
"        if (rhythm.repair) {",
"        for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2; rhythmAttempt += 1) {",
1,
)
final_anchor = "      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);"
final_replacement = final_anchor + """
      if (narrativeIntent.advancesFiction && finalQuality.flags.includes('narração repetiu substancialmente a cena anterior em vez de responder à nova ação')) {
        throw new Error('O Mestre devolveu a cena anterior em vez de responder à ação atual. O turno foi preservado sem duplicar a narração; tente novamente.');
      }"""
if final_anchor not in app:
    raise SystemExit("final quality anchor missing")
app = app.replace(final_anchor, final_replacement, 1)
write(app_rel, app)

# Version.
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.18"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.17"', '"version": "4.9.18"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.17'", "APP_VERSION = '4.9.18'", version)
version = re.sub(r"APP_VERSION_CODE = 49017", "APP_VERSION_CODE = 49018", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49017", "versionCode = 49018", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.17"', 'versionName = "4.9.18"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49017&immersive=1", "index.html?v=49018&immersive=1")
write(main_rel, main)

# Gates.
models = read(models_rel)
settings = read(settings_rel)
svc = read(svc_rel)
engine = read(engine_rel)
rhythm = read(rhythm_rel)
app = read(app_rel)
emap = read("components/EriadorMap.tsx")
db = read("lib/db.ts")
checks = {
    "one text model": "const TEXT_KEY = 'braseiro.geminiModel.text'" in models and "getGeminiTextModel(role" not in models,
    "no economy UI": "Texto econômico" not in settings and "economyModel" not in settings,
    "single Master field": "Modelo do Mestre — texto — ID manual" in settings and "saveGeminiTextModel(textModel)" in settings,
    "no role lookups": "getGeminiTextModel('economy')" not in svc and "getGeminiTextModel('cinematic')" not in svc,
    "one narrator model": "const models = [getGeminiTextModel()];" in svc,
    "long cinematic": "temperature: 0.78, maxOutputTokens: 6800" in svc and "280 a 520 palavras" in svc,
    "table Master voice": "Fale como um Mestre sentado à mesa" in svc,
    "sensory": "SENSORIALIDADE ORGÂNICA" in svc,
    "anti-repeat prompt": "NÃO REPITA a narração anterior" in svc,
    "short repair": "softWordMin" in rhythm and "narração curta demais para a cadência" in rhythm,
    "repeat detector": "narração repetiu substancialmente a cena anterior" in engine,
    "double repair": "qualityAttempt < 2" in app and "rhythmAttempt < 2" in app,
    "final repeat block": "O Mestre devolveu a cena anterior em vez de responder à ação atual" in app,
    "party preserved": 'data-party-world-anchored="true"' in emap and "mapX: hex.centerX, mapY: hex.centerY" in emap,
    "db preserved": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
    "version": "4.9.18" in read(version_rel) and "49018" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4918_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.18_ONE_MASTER_MODEL_OK")
