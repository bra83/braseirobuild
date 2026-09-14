#!/usr/bin/env python3
from __future__ import annotations

import base64
import hashlib
import io
import re
import sys
import tarfile
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: block01_repair.py <android-project-root>")

ROOT = Path(sys.argv[1]).resolve()
VTT = ROOT / "app/src/main/assets/vtt"
JAVA = ROOT / "app/src/main/java/com/braseiro/starwarsedge"
REPO_PATCH_DIR = Path(__file__).resolve().parent


def read(path: Path) -> str:
    if not path.is_file():
        raise SystemExit(f"missing required file: {path}")
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    out, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one regex match, got {count}")
    return out


# ---------------------------------------------------------------------------
# 1. Recover scene images that already existed in the user's Star Wars work.
#    These are crops/normalizations of prior project assets, not new images.
# ---------------------------------------------------------------------------
ARCHIVE_SHA = "2ca16b2cd0d3f489fe10f1df718be1243febeef38fb88a52bd50502528f60711"
SCENE_FILES = {
    "scene_blaster_shootout.webp": "a6e620f9bfdc8a32470daec4cc66e73ecc7201db7d6e1af3f8a0a6b7386bb0d1",
    "scene_briefing_social.webp": "34a6442e0f06b00ebc85a7385d8b9ef1ce3b6291a0e16d8eb0cda153fd8834c1",
    "scene_exploration.webp": "643aec53e1ad108860251886cde530fcb83440ac13e0f970feff8167d0e67270",
    "scene_imperial_infiltration.webp": "7949359eba2364f82ee2b496184021b1d0055c0e9bf7ffc1d45a025ff0c16edf",
    "scene_outer_rim_space_combat.webp": "0b0c430dcd2d339bcd1f1ebe59e801881e856dd12dfa0ad57f58ea6845138cde",
    "scene_ship_travel.webp": "5cf06a2dbce014f3aa081a8bb27f1169a25d990d6689dec4484d1cc70ee135b7",
}
asset_b64_path = REPO_PATCH_DIR / "assets/scene-assets-v1.tar.gz.b64"
raw_b64 = "".join(read(asset_b64_path).split())
try:
    archive = base64.b64decode(raw_b64, validate=True)
except Exception as exc:
    raise SystemExit(f"scene asset archive base64 invalid: {exc}")
if hashlib.sha256(archive).hexdigest() != ARCHIVE_SHA:
    raise SystemExit("scene asset archive SHA mismatch")

scene_dir = VTT / "image-library/local/scenes/recovered"
scene_dir.mkdir(parents=True, exist_ok=True)
with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as tf:
    members = tf.getmembers()
    names = {m.name for m in members if m.isfile()}
    if names != set(SCENE_FILES):
        raise SystemExit(f"unexpected scene archive members: {sorted(names)}")
    for member in members:
        if not member.isfile() or "/" in member.name or "\\" in member.name or member.name.startswith("."):
            raise SystemExit(f"unsafe scene asset member: {member.name}")
        source = tf.extractfile(member)
        if source is None:
            raise SystemExit(f"cannot read scene asset member: {member.name}")
        data = source.read()
        if hashlib.sha256(data).hexdigest() != SCENE_FILES[member.name]:
            raise SystemExit(f"scene asset SHA mismatch: {member.name}")
        (scene_dir / member.name).write_bytes(data)


# ---------------------------------------------------------------------------
# 2. Android/system insets: expose real system-bar insets to CSS.
#    Android 16 / target 36 is edge-to-edge, so the WebView must not rely on
#    native view padding for fixed-position web chrome.
# ---------------------------------------------------------------------------
main_path = JAVA / "MainActivity.java"
main = read(main_path)
main = replace_once(
    main,
    "    private ValueCallback<Uri[]> pendingJsonChooser;\n",
    "    private ValueCallback<Uri[]> pendingJsonChooser;\n"
    "    private int systemInsetLeft = 0;\n"
    "    private int systemInsetTop = 0;\n"
    "    private int systemInsetRight = 0;\n"
    "    private int systemInsetBottom = 0;\n",
    "MainActivity inset fields",
)
main = replace_once(
    main,
    "        webView.setFitsSystemWindows(true);\n"
    "        webView.setOnApplyWindowInsetsListener((view, insets) -> {\n"
    "            int left = insets.getSystemWindowInsetLeft();\n"
    "            int top = insets.getSystemWindowInsetTop();\n"
    "            int right = insets.getSystemWindowInsetRight();\n"
    "            int bottom = insets.getSystemWindowInsetBottom();\n"
    "            view.setPadding(left, top, right, bottom);\n"
    "            return insets;\n"
    "        });\n",
    "        webView.setFitsSystemWindows(false);\n"
    "        webView.setOnApplyWindowInsetsListener((view, insets) -> {\n"
    "            systemInsetLeft = Math.max(0, insets.getSystemWindowInsetLeft());\n"
    "            systemInsetTop = Math.max(0, insets.getSystemWindowInsetTop());\n"
    "            systemInsetRight = Math.max(0, insets.getSystemWindowInsetRight());\n"
    "            systemInsetBottom = Math.max(0, insets.getSystemWindowInsetBottom());\n"
    "            view.setPadding(0, 0, 0, 0);\n"
    "            applySystemInsetsToWeb();\n"
    "            return insets;\n"
    "        });\n",
    "MainActivity inset listener",
)
main = replace_once(
    main,
    "            @Override\n"
    "            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {\n",
    "            @Override\n"
    "            public void onPageFinished(WebView view, String url) {\n"
    "                super.onPageFinished(view, url);\n"
    "                applySystemInsetsToWeb();\n"
    "            }\n\n"
    "            @Override\n"
    "            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {\n",
    "MainActivity onPageFinished",
)
main = replace_once(
    main,
    "    @Override\n"
    "    @SuppressWarnings(\"deprecation\")\n"
    "    public void onBackPressed() {\n",
    "    private void applySystemInsetsToWeb() {\n"
    "        if (webView == null) return;\n"
    "        final String script = \"(function(){var r=document.documentElement;if(!r)return;\" +\n"
    "                \"r.style.setProperty('--android-safe-left','\" + systemInsetLeft + \"px');\" +\n"
    "                \"r.style.setProperty('--android-safe-top','\" + systemInsetTop + \"px');\" +\n"
    "                \"r.style.setProperty('--android-safe-right','\" + systemInsetRight + \"px');\" +\n"
    "                \"r.style.setProperty('--android-safe-bottom','\" + systemInsetBottom + \"px');\" +\n"
    "                \"})();\";\n"
    "        webView.post(() -> {\n"
    "            if (webView != null && !webView.isDestroyed()) webView.evaluateJavascript(script, null);\n"
    "        });\n"
    "    }\n\n"
    "    @Override\n"
    "    @SuppressWarnings(\"deprecation\")\n"
    "    public void onBackPressed() {\n",
    "MainActivity CSS inset injector",
)
write(main_path, main)


# ---------------------------------------------------------------------------
# 3. CSS safe-area bridge + bottom navigation icon hardening.
#    Keep R46 geometry; only correct system inset inputs and icon rendering.
# ---------------------------------------------------------------------------
globals_path = VTT / "r46/globals.css"
globals = read(globals_path)
globals = replace_once(
    globals,
    "--top-h:4.25rem; --nav-h:5.2rem; --safe-top:env(safe-area-inset-top,0px); --safe-bottom:env(safe-area-inset-bottom,0px);",
    "--top-h:4.25rem; --nav-h:5.2rem; --android-safe-left:0px; --android-safe-top:0px; --android-safe-right:0px; --android-safe-bottom:0px; --safe-left:max(env(safe-area-inset-left,0px),var(--android-safe-left)); --safe-top:max(env(safe-area-inset-top,0px),var(--android-safe-top)); --safe-right:max(env(safe-area-inset-right,0px),var(--android-safe-right)); --safe-bottom:max(env(safe-area-inset-bottom,0px),var(--android-safe-bottom));",
    "globals safe variables",
)
globals = globals.replace("max(1rem,env(safe-area-inset-left))", "max(1rem,var(--safe-left))")
globals = globals.replace("max(.4rem,env(safe-area-inset-left))", "max(.4rem,var(--safe-left))")
write(globals_path, globals)

r46_path = VTT / "r46/r46.css"
r46 = read(r46_path)
r46 = replace_once(
    r46,
    "  --r46-safe-bottom:env(safe-area-inset-bottom,0px);",
    "  --r46-safe-bottom:var(--safe-bottom);",
    "r46 safe bottom alias",
)
r46 = replace_once(
    r46,
    ".app-shell.scene-mode{\n  padding-top:0;",
    ".app-shell.scene-mode{\n  padding-top:var(--safe-top);",
    "scene top safe area",
)
ICON_HARDENING = r"""

/* R38 BLOCK01 — Android WebView icon fallback. Keep the approved R46 icon art,
   but render the PNG itself instead of relying on CSS mask support. */
.bottom-nav-button .reference-icon-mask{
  background-color:transparent!important;
  background-image:var(--ref-image)!important;
  background-repeat:no-repeat!important;
  background-position:center!important;
  background-size:contain!important;
  -webkit-mask-image:none!important;
  mask-image:none!important;
}
"""
if "R38 BLOCK01 — Android WebView icon fallback" not in r46:
    r46 += ICON_HARDENING
write(r46_path, r46)


# ---------------------------------------------------------------------------
# 4. CENA: semantic recovered-image fallback + external Gemini handoff.
# ---------------------------------------------------------------------------
runtime_path = VTT / "r46/final-runtime.js"
runtime = read(runtime_path)
RECOVERED_HELPERS = r'''const RECOVERED_SCENE_VISUALS=Object.freeze([
 {id:'space-combat',src:'image-library/local/scenes/recovered/scene_outer_rim_space_combat.webp',terms:['combate espacial','combate veicular','órbita','orbita','tie','caça','caca','patrulha imperial','artilharia','batalha espacial'],weight:8},
 {id:'shootout',src:'image-library/local/scenes/recovered/scene_blaster_shootout.webp',terms:['tiroteio','blaster','stormtrooper','trooper','combate','corredor','cobertura','ataque imperial'],weight:6},
 {id:'infiltration',src:'image-library/local/scenes/recovered/scene_imperial_infiltration.webp',terms:['infiltra','furtiv','disfar','segurança','seguranca','guarda','base imperial','instalação imperial','instalacao imperial'],weight:6},
 {id:'travel',src:'image-library/local/scenes/recovered/scene_ship_travel.webp',terms:['hiperespaço','hiperespaco','salto','viagem','rota','voo','hangar','espaçoporto','espacoporto','doca','nave'],weight:5},
 {id:'exploration',src:'image-library/local/scenes/recovered/scene_exploration.webp',terms:['explora','deserto','ruína','ruina','ermo','jundland','jedha','tatooine','mercado','persegui','rastro'],weight:5},
 {id:'briefing',src:'image-library/local/scenes/recovered/scene_briefing_social.webp',terms:['briefing','contato','informante','negocia','conversa','cantina','bar','social','reunião','reuniao'],weight:5}
]);
function recoveredSceneVisual(view){
 const scene=currentSceneRecord()||{};
 const location=view?.scene?.location||{};
 const context=[scene.title,scene.narration,view?.situation,location.name,location.description,...(Array.isArray(view?.participants)?view.participants.flatMap(p=>[p?.name,p?.meta,p?.status]):[])].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR');
 let best=null,bestScore=0;
 for(const visual of RECOVERED_SCENE_VISUALS){let score=0;for(const term of visual.terms){if(context.includes(term.toLocaleLowerCase('pt-BR')))score+=visual.weight+(term.includes(' ')?2:0)}if(score>bestScore){best=visual;bestScore=score}}
 if(bestScore>0)return best;
 if(view?.vehicle)return RECOVERED_SCENE_VISUALS.find(v=>v.id==='travel')||null;
 if(/combate/i.test(String(view?.situation||'')))return RECOVERED_SCENE_VISUALS.find(v=>v.id==='shootout')||null;
 return RECOVERED_SCENE_VISUALS.find(v=>v.id==='briefing')||null;
}
function syncRecoveredSceneVisual(root,view){
 const media=root.querySelector('.scene-media-slot');if(!media)return;
 const live=media.querySelector('img.scene-media-image:not(.final-scene-library-image)');
 let fallback=media.querySelector('.final-scene-library-image');
 if(live){if(fallback)fallback.remove();media.classList.remove('has-recovered-scene');return}
 const selected=recoveredSceneVisual(view);
 if(!selected){if(fallback)fallback.remove();media.classList.remove('has-recovered-scene');return}
 if(!fallback){fallback=document.createElement('img');fallback.className='scene-media-image final-scene-library-image';fallback.alt='Imagem de referência compatível com a cena atual';fallback.decoding='async';media.prepend(fallback)}
 if(fallback.getAttribute('src')!==selected.src)fallback.setAttribute('src',selected.src);
 fallback.dataset.recoveredScene=selected.id;
 media.classList.add('has-scene-image','has-recovered-scene');media.classList.remove('is-empty');
 const surface=media.querySelector('.scene-media-surface'),caption=media.querySelector('figcaption');if(surface)surface.hidden=true;if(caption)caption.hidden=true;
}
'''
if "RECOVERED_SCENE_VISUALS" not in runtime:
    runtime = replace_once(runtime, "function enhanceSceneFromEngine(){\n", RECOVERED_HELPERS + "function enhanceSceneFromEngine(){\n", "recovered scene helper insertion")
runtime = replace_once(
    runtime,
    " const view=playerView();if(!view)return;\n",
    " const view=playerView();if(!view)return;\n syncRecoveredSceneVisual(root,view);\n",
    "recovered scene visual call",
)

NEW_PROMPT = r'''function sceneGeminiPrompt(){
 const view=playerView()||{},scene=currentSceneRecord()||{},narration=narrationText(),situation=String(view.situation||document.querySelector('.scene-situation-paper p')?.textContent||'').trim();
 const location=view.scene?.location||{};
 const title=String(scene.title||view.scene?.title||'Cena').trim()||'Cena';
 const participants=Array.isArray(view.participants)?view.participants.filter(Boolean).map(p=>`${String(p.name||'').trim()}${p.meta?` — ${p.meta}`:''}${p.status?` (${p.status})`:''}`).filter(Boolean).join('; '):'';
 const vehicle=view.vehicle?`Nave/veículo visível: ${view.vehicle.name||'não nomeado'}; velocidade ${view.vehicle.speed||'—'}; defesa ${view.vehicle.defense||'—'}.`:'';
 const facts=[`Título: ${title}.`,location.name?`Local: ${location.name}.`:'',location.description?`Descrição visível do local: ${location.description}`:'',situation?`Situação imediata: ${situation}`:'',narration?`Narração visível do Mestre: ${narration}`:'',participants?`Participantes visíveis: ${participants}.`:'',vehicle].filter(Boolean);
 if(facts.length<=1&&!narration&&!situation)return '';
 return [
  'Crie UMA ilustração cinematográfica horizontal 16:9 para o quadro de CENA de um VTT ambientado em Star Wars, com atmosfera de Edge of the Empire / fronteira galáctica.',
  'A imagem deve parecer um frame de filme: composição clara, profundidade, luz cinematográfica, materiais usados e realistas, escala coerente e ação legível no celular. Preserve o clima descrito pela cena.',
  'Use SOMENTE os fatos visíveis abaixo. Não revele informação de Mestre, não invente personagens nomeados, facções, veículos, ferimentos, armas ou acontecimentos que não estejam descritos. Pessoas genéricas de fundo só quando forem necessárias para o ambiente e não contradisserem a cena.',
  'NÃO coloque texto, letras, legendas, molduras, HUD, menus, botões, marcas d’água ou interface na imagem.',
  `FATOS VISÍVEIS DA CENA:\n${facts.join('\n')}`,
  'Enquadre o elemento mais importante da situação; para ação use plano amplo/dinâmico, para conversa use plano médio com ambiente legível, para exploração priorize o local. Não faça colagem nem ficha de personagem.'
 ].join('\n\n');
}'''
runtime = regex_once(runtime, r"function sceneGeminiPrompt\(\)\{.*?\}\nfunction sceneKeyFor", NEW_PROMPT + "\nfunction sceneKeyFor", "scene Gemini prompt")

NEW_BUTTON = r'''function enhanceSceneGeminiButton(){
 const media=document.querySelector('.scene-media-slot');if(!media||media.querySelector('.final-scene-gemini'))return;
 const wrap=document.createElement('div');wrap.className='final-scene-gemini';wrap.innerHTML='<div class="final-scene-button-row"><button type="button" data-action="gemini">✦ ILUSTRAÇÃO</button><button type="button" data-action="import">⇧ IMPORTAR</button></div><small aria-live="polite"></small>';media.appendChild(wrap);
 const openBtn=wrap.querySelector('[data-action="gemini"]'),importBtn=wrap.querySelector('[data-action="import"]'),msg=wrap.querySelector('small');
 openBtn.onclick=async()=>{const g=window.BraseiroGemini,prompt=sceneGeminiPrompt();if(!prompt){msg.textContent='Inicie a aventura para descrever a cena.';return}openBtn.disabled=true;msg.textContent='Preparando prompt e abrindo Gemini…';
  try{
   if(g&&typeof g.openGeminiWithPrompt==='function'){const r=safeParse(g.openGeminiWithPrompt(prompt),{});if(!r.ok){msg.textContent=r.message||'Não foi possível abrir o Gemini.';openBtn.disabled=false;return}msg.textContent=r.target==='gemini_app'?'Prompt enviado ao app Gemini. Depois use IMPORTAR.':'Prompt copiado. Gemini aberto; depois use IMPORTAR.';setTimeout(()=>{openBtn.disabled=false},900);return}
   if(navigator.clipboard&&typeof navigator.clipboard.writeText==='function')await navigator.clipboard.writeText(prompt);window.open('https://gemini.google.com/app','_blank');msg.textContent='Prompt copiado. Cole no Gemini e depois use IMPORTAR.';setTimeout(()=>{openBtn.disabled=false},900);
  }catch{msg.textContent='Não foi possível abrir o Gemini. O prompt permanece disponível nesta cena.';openBtn.disabled=false}
 };
 importBtn.onclick=()=>{const g=window.BraseiroGemini;if(!g||typeof g.pickSceneImage!=='function'){msg.textContent='Importação de imagem indisponível neste host.';return}importBtn.disabled=true;msg.textContent='Escolha a imagem criada no Gemini…';try{const r=safeParse(g.pickSceneImage(currentSceneKey()),{});if(!r.ok){msg.textContent=r.message||'Não foi possível abrir suas imagens.';importBtn.disabled=false}else{msg.textContent='Selecione a imagem desta cena.';setTimeout(()=>{importBtn.disabled=false},1000)}}catch{msg.textContent='Falha ao abrir suas imagens.';importBtn.disabled=false}};
}'''
runtime = regex_once(runtime, r"function enhanceSceneGeminiButton\(\)\{.*?\n\}\nfunction enhanceGeminiModels", NEW_BUTTON + "\nfunction enhanceGeminiModels", "scene Gemini external handoff")
write(runtime_path, runtime)

runtime_css_path = VTT / "r46/final-runtime.css"
runtime_css = read(runtime_css_path)
RUNTIME_CSS_APPEND = r'''

/* R38 BLOCK01 — recovered scene fallback + external Gemini/import controls. */
.scene-media-slot.has-recovered-scene>.scene-media-surface,
.scene-media-slot.has-recovered-scene>figcaption{display:none!important}
.final-scene-library-image{width:100%;height:100%;object-fit:cover;display:block}
.final-scene-gemini{align-items:flex-end;flex-wrap:wrap}
.final-scene-button-row{display:flex;gap:5px;flex:0 0 auto}
.final-scene-button-row button[data-action="import"]{border-color:rgba(100,169,186,.85);color:#bfe8f2}
.final-scene-gemini>small{max-width:15rem}
'''
if "R38 BLOCK01 — recovered scene fallback" not in runtime_css:
    runtime_css += RUNTIME_CSS_APPEND
write(runtime_css_path, runtime_css)


# ---------------------------------------------------------------------------
# 5. Native Gemini handoff: copy prompt and open Gemini app if resolvable,
#    otherwise open Gemini web. Existing in-app API generation remains intact
#    for settings/backward compatibility but CENA no longer requires it.
# ---------------------------------------------------------------------------
gemini_path = JAVA / "GeminiBridge.java"
gemini = read(gemini_path)
gemini = replace_once(
    gemini,
    "import android.content.Intent;\n",
    "import android.content.ClipData;\nimport android.content.ClipboardManager;\nimport android.content.Context;\nimport android.content.Intent;\n",
    "GeminiBridge clipboard imports",
)
OPEN_GEMINI_METHOD = r'''        @JavascriptInterface
        public String openGeminiWithPrompt(String prompt) {
            String cleanPrompt = String.valueOf(prompt == null ? "" : prompt).trim();
            if (cleanPrompt.length() < 20 || cleanPrompt.length() > 12000) return fail("Prompt de ilustração inválido.");
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Prompt de ilustração da cena", cleanPrompt));

                Intent geminiApp = new Intent(Intent.ACTION_SEND);
                geminiApp.setType("text/plain");
                geminiApp.putExtra(Intent.EXTRA_TEXT, cleanPrompt);
                geminiApp.putExtra(Intent.EXTRA_TITLE, "Ilustração da cena do VTT");
                geminiApp.setPackage("com.google.android.apps.bard");
                final boolean hasGeminiApp = geminiApp.resolveActivity(activity.getPackageManager()) != null;
                final Intent launchIntent = hasGeminiApp
                        ? geminiApp
                        : new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"));
                final String target = hasGeminiApp ? "gemini_app" : "gemini_web";
                activity.runOnUiThread(() -> {
                    try {
                        activity.startActivity(launchIntent);
                        notifyEvent("external_prompt_opened", jsonObject("target", target));
                    } catch (Exception first) {
                        try {
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app")));
                            notifyEvent("external_prompt_opened", jsonObject("target", "gemini_web"));
                        } catch (Exception ignored) {
                            notifyEvent("image_error", jsonObject("message", "Prompt copiado, mas não foi possível abrir o Gemini."));
                        }
                    }
                });
                return new JSONObject().put("ok", true).put("status", "opened").put("target", target).put("copied", true).toString();
            } catch (Exception e) {
                return fail("Não foi possível copiar o prompt e abrir o Gemini.");
            }
        }

'''
if "public String openGeminiWithPrompt" not in gemini:
    gemini = replace_once(
        gemini,
        "        @JavascriptInterface\n        public String generateSceneImage(String prompt, String sceneKey) {\n",
        OPEN_GEMINI_METHOD + "        @JavascriptInterface\n        public String generateSceneImage(String prompt, String sceneKey) {\n",
        "GeminiBridge external prompt method",
    )
write(gemini_path, gemini)


# ---------------------------------------------------------------------------
# 6. Static postconditions. These deliberately fail closed if the baseline
#    changes underneath the patch.
# ---------------------------------------------------------------------------
checks = {
    "scene assets": all((scene_dir / name).is_file() for name in SCENE_FILES),
    "external Gemini bridge": "openGeminiWithPrompt" in read(gemini_path),
    "scene import": "pickSceneImage(currentSceneKey())" in read(runtime_path),
    "scene semantic fallback": "RECOVERED_SCENE_VISUALS" in read(runtime_path),
    "Android CSS insets": "--android-safe-bottom" in read(globals_path) and "applySystemInsetsToWeb" in read(main_path),
    "icon mask fallback": "Android WebView icon fallback" in read(r46_path),
    "R46 shell preserved": (VTT / "r46-shell.html").is_file(),
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("postcondition failed: " + ", ".join(failed))

print("BLOCK01_PATCH_OK")
for name in checks:
    print("PASS", name)
print("RECOVERED_SCENE_ASSETS", len(SCENE_FILES))
