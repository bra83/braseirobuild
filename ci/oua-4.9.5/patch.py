from pathlib import Path
import json, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root/rel).read_text(encoding="utf-8")
def write(rel, text):
    (root/rel).write_text(text, encoding="utf-8")

(root/"lib/geminiModels.ts").write_text("""export type GeminiTextRole = 'economy' | 'cinematic';

export const GEMINI_TEXT_MODEL_OPTIONS = [
  { id: 'gemini-3.5-flash-lite', label: 'Gemini 3.5 Flash-Lite — econômico' },
  { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash — principal' },
  { id: 'gemini-3.1-flash-lite', label: 'Gemini 3.1 Flash-Lite' },
  { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash' },
  { id: 'gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash-Lite' },
  { id: 'gemini-flash-latest', label: 'Gemini Flash Latest — alias' },
] as const;

const KEYS: Record<GeminiTextRole, string> = {
  economy: 'braseiro.geminiModel.economy',
  cinematic: 'braseiro.geminiModel.cinematic',
};

const DEFAULTS: Record<GeminiTextRole, string> = {
  economy: 'gemini-3.5-flash-lite',
  cinematic: 'gemini-3.5-flash',
};

export function getGeminiTextModel(role: GeminiTextRole): string {
  if (typeof window === 'undefined') return DEFAULTS[role];
  const saved = String(window.localStorage.getItem(KEYS[role]) || '').trim();
  return saved || DEFAULTS[role];
}

export function saveGeminiTextModel(role: GeminiTextRole, model: string): void {
  if (typeof window === 'undefined') return;
  const clean = String(model || '').trim();
  window.localStorage.setItem(KEYS[role], clean || DEFAULTS[role]);
}
""", encoding="utf-8")

svc="services/geminiService.ts"
s=read(svc)
needle="import { generateGemini, responseText } from '../lib/geminiTransport';"
if needle not in s: raise SystemExit("geminiService import anchor missing")
s=s.replace(needle, needle+"\nimport { getGeminiTextModel } from '../lib/geminiModels';",1)
old_models="const ECONOMY_MODEL = 'gemini-3.1-flash-lite';\nconst CINEMATIC_MODEL = 'gemini-3.5-flash';\n"
if old_models not in s: raise SystemExit("geminiService model constants missing")
s=s.replace(old_models,"",1)
s=s.replace("ECONOMY_MODEL","getGeminiTextModel('economy')")
s=s.replace("CINEMATIC_MODEL","getGeminiTextModel('cinematic')")
write(svc,s)

settings="components/SettingsView.tsx"
s=read(settings)
old_import="import { getBrowserGeminiApiKey, hasGeminiApiKey, saveGeminiApiKey } from '../lib/apiKey';"
if old_import not in s: raise SystemExit("Settings import anchor missing")
s=s.replace(old_import, old_import+"\nimport { GEMINI_TEXT_MODEL_OPTIONS, getGeminiTextModel, saveGeminiTextModel } from '../lib/geminiModels';\nimport { generateGemini, responseText } from '../lib/geminiTransport';",1)
state_anchor="  const [keySaved, setKeySaved] = useState(false);\n"
if state_anchor not in s: raise SystemExit("Settings state anchor missing")
s=s.replace(state_anchor, state_anchor+"""  const [economyModel, setEconomyModel] = useState(() => getGeminiTextModel('economy'));
  const [cinematicModel, setCinematicModel] = useState(() => getGeminiTextModel('cinematic'));
  const [modelStatus, setModelStatus] = useState('');
  const [modelTesting, setModelTesting] = useState(false);

  const testSelectedModel = async () => {
    if (!hasGeminiApiKey()) {
      setModelStatus('Salve ao menos uma chave Gemini antes do teste.');
      return;
    }
    setModelTesting(true);
    setModelStatus('Testando modelo principal…');
    try {
      const response = await generateGemini(cinematicModel, [{ parts: [{ text: 'Responda apenas com OK.' }] }], { temperature: 0, maxOutputTokens: 16 });
      const text = responseText(response);
      setModelStatus(text ? 'Modelo respondeu: ' + text.slice(0, 80) : 'O modelo respondeu sem texto.');
    } catch (error) {
      setModelStatus(error instanceof Error ? error.message : 'Falha ao testar o modelo Gemini.');
    } finally {
      setModelTesting(false);
    }
  };
""",1)

old="""            <button type="button" onClick={() => {
              if (freeKey.trim()) { saveGeminiApiKey('free', freeKey); setFreeConfigured(true); setFreeKey(''); }
              if (paidKey.trim()) { saveGeminiApiKey('paid', paidKey); setPaidConfigured(true); setPaidKey(''); }
              setKeySaved(true);
            }} className="mt-3 min-h-12 w-full rounded border border-blue-500 bg-blue-950/60 font-bold text-blue-100">
              {keySaved ? 'Chaves salvas neste aparelho' : 'Salvar chaves preenchidas'}
            </button>
"""
new="""            <label className="mt-4 block text-xs font-bold text-cyan-200">Modelo econômico</label>
            <select value={economyModel} onChange={event => { setEconomyModel(event.target.value); setKeySaved(false); }} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-sm text-blue-50">
              {GEMINI_TEXT_MODEL_OPTIONS.map(model => <option key={'economy-' + model.id} value={model.id}>{model.label}</option>)}
            </select>
            <label className="mt-3 block text-xs font-bold text-amber-200">Modelo principal / cenas decisivas</label>
            <select value={cinematicModel} onChange={event => { setCinematicModel(event.target.value); setKeySaved(false); setModelStatus(''); }} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-sm text-blue-50">
              {GEMINI_TEXT_MODEL_OPTIONS.map(model => <option key={'cinematic-' + model.id} value={model.id}>{model.label}</option>)}
            </select>
            <button type="button" disabled={modelTesting} onClick={() => void testSelectedModel()} className="mt-3 min-h-11 w-full rounded border border-cyan-700 bg-cyan-950/30 text-xs font-bold text-cyan-100 disabled:opacity-50">
              {modelTesting ? 'Testando Gemini…' : 'Testar modelo principal'}
            </button>
            {modelStatus && <p className="mt-2 rounded border border-slate-800 bg-black/25 px-3 py-2 text-xs leading-relaxed text-slate-300">{modelStatus}</p>}
            <button type="button" onClick={() => {
              if (freeKey.trim()) { saveGeminiApiKey('free', freeKey); setFreeConfigured(true); setFreeKey(''); }
              if (paidKey.trim()) { saveGeminiApiKey('paid', paidKey); setPaidConfigured(true); setPaidKey(''); }
              saveGeminiTextModel('economy', economyModel);
              saveGeminiTextModel('cinematic', cinematicModel);
              setKeySaved(true);
              setModelStatus('Chaves e modelos salvos. O próximo turno já usa esta seleção.');
            }} className="mt-3 min-h-12 w-full rounded border border-blue-500 bg-blue-950/60 font-bold text-blue-100">
              {keySaved ? 'Chaves e modelos salvos neste aparelho' : 'Salvar chaves e modelos'}
            </button>
"""
if old not in s: raise SystemExit("Settings Gemini button block missing")
s=s.replace(old,new,1)
write(settings,s)

imm="components/immersive/ImmersiveJourneyView.tsx"
s=read(imm)
prop_anchor="  currentImage?: string | null;\n"
if prop_anchor not in s: raise SystemExit("Immersive prop anchor missing")
s=s.replace(prop_anchor, prop_anchor+"""  isStoryLoading?: boolean;
  storyError?: string | null;
  onDismissStoryError?: () => void;
  onStartAdventure?: () => void | Promise<void>;
  canStartAdventure?: boolean;
""",1)

old_bg="""      {atlasActive
        ? <EriadorMap currentLocation={locationName} campaignId={props.campaignId} currentHexId={props.masterState.position.currentHexId} onPartyMoved={props.onPartyMoved} onDiscoveryChanged={props.onMapDiscoveryChanged} onPartyRested={props.onPartyRested} strongholds={props.masterState.livingWorld.strongholds || []} immersive partyMarkerMode="none" followParty />
        : <img src={props.currentImage || './terra-media-map/map_preview_boot_1536.webp'} alt="" draggable={false} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />}
"""
new_bg="""      {atlasActive
        ? <EriadorMap currentLocation={locationName} campaignId={props.campaignId} currentHexId={props.masterState.position.currentHexId} onPartyMoved={props.onPartyMoved} onDiscoveryChanged={props.onMapDiscoveryChanged} onPartyRested={props.onPartyRested} strongholds={props.masterState.livingWorld.strongholds || []} immersive partyMarkerMode="none" followParty />
        : <>
            <img src="./terra-media-map/map_preview_boot_1536.webp" alt="" draggable={false} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
            {props.currentImage && <img
              src={props.currentImage}
              alt=""
              draggable={false}
              style={{
                pointerEvents: 'none', position: 'absolute', right: '-5%', top: '16%', width: '78%', height: '43%',
                objectFit: 'cover', objectPosition: 'center', opacity: .48, filter: 'sepia(.12) saturate(.82) contrast(.92)',
                WebkitMaskImage: 'radial-gradient(ellipse at center, #000 48%, transparent 78%)',
                maskImage: 'radial-gradient(ellipse at center, #000 48%, transparent 78%)',
              }}
            />}
          </>}
"""
if old_bg not in s: raise SystemExit("Immersive background block missing")
s=s.replace(old_bg,new_bg,1)

old_fallback=": <p style={{ margin: 0, fontStyle: 'italic', opacity: .72 }}>A crônica aguarda o próximo passo da Companhia.</p>}"
new_fallback=""": <div>
              <p style={{ margin: 0, fontStyle: 'italic', opacity: .72 }}>A crônica aguarda o próximo passo da Companhia.</p>
              {props.storyError && <div role="alert" style={{ marginTop: 10, border: '1px solid rgba(135,35,25,.55)', borderRadius: 8, background: 'rgba(95,20,15,.10)', padding: '9px 10px', color: '#6f2119', fontSize: 12, fontWeight: 700 }}>
                {props.storyError}
                {props.onDismissStoryError && <button type="button" onClick={props.onDismissStoryError} style={{ marginLeft: 8, border: '1px solid rgba(95,40,30,.35)', borderRadius: 6, background: 'rgba(255,255,255,.25)', padding: '4px 7px', color: '#5f271f', fontFamily: 'inherit', fontWeight: 700 }}>Fechar</button>}
              </div>}
              {props.onStartAdventure && <button
                type="button"
                disabled={!props.canStartAdventure || props.isStoryLoading}
                onClick={() => void props.onStartAdventure?.()}
                style={{ marginTop: 14, width: '100%', minHeight: 52, borderRadius: 9, border: '1px solid #6d4726', background: '#6b4525', color: '#f6e7c6', fontFamily: 'inherit', fontWeight: 800, fontSize: 15, letterSpacing: '.05em', textTransform: 'uppercase', opacity: (!props.canStartAdventure || props.isStoryLoading) ? .55 : 1 }}
              >{props.isStoryLoading ? 'Abrindo crônica…' : 'Começar aventura'}</button>}
              {!props.canStartAdventure && <p style={{ margin: '8px 0 0', fontSize: 11, opacity: .72 }}>Defina primeiro a posição da Companhia no Atlas.</p>}
            </div>}"""
if old_fallback not in s: raise SystemExit("Immersive empty narrative fallback missing")
s=s.replace(old_fallback,new_fallback,1)
write(imm,s)

game="components/GameView.tsx"
s=read(game)
anchor="""      currentImage={currentImage}
      characters={partyCharacters}
"""
replacement="""      currentImage={currentImage}
      isStoryLoading={isStoryLoading}
      storyError={storyError}
      onDismissStoryError={onDismissStoryError}
      onStartAdventure={onStartAdventure}
      canStartAdventure={canStartAdventure}
      characters={partyCharacters}
"""
if anchor not in s: raise SystemExit("GameView immersive prop anchor missing")
s=s.replace(anchor,replacement,1)
write(game,s)

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.5"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8").replace('"version": "4.9.4"','"version": "4.9.5"')
    lock.write_text(t,encoding="utf-8")

for rel in ["lib/version.ts","android/app/build.gradle.kts","android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"]:
    p=root/rel
    t=p.read_text(encoding="utf-8")
    t=t.replace("4.9.4","4.9.5").replace("49004","49005")
    p.write_text(t,encoding="utf-8")

(root/"CHANGELOG_OUA_4.9.5_GEMINI_LOCATION_ART.md").write_text(
"""# O Um Anel 4.9.5 — Gemini + ilustração contextual

- adiciona seleção persistente de modelo Gemini econômico e principal;
- atualiza o padrão econômico para gemini-3.5-flash-lite;
- adiciona teste do modelo diretamente em Configurações;
- mantém as chaves protegidas pelo bridge Android;
- mostra erro/carregamento/início da aventura também na UI imersiva;
- restaura a ilustração da cena/local como camada contextual translúcida sobre o mapa, sem substituir o mapa;
- preserva mapa, Companhia, Motor Barbara 1.1.3 e correções visuais 4.9.4.
""",encoding="utf-8")

assert "GEMINI_TEXT_MODEL_OPTIONS" in read(settings)
assert "getGeminiTextModel('economy')" in read(svc)
assert "getGeminiTextModel('cinematic')" in read(svc)
assert "currentImage && <img" in read(imm)
assert "onStartAdventure={onStartAdventure}" in read(game)
print("PATCH_4.9.5_OK")
