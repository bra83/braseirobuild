#!/usr/bin/env python3
from __future__ import annotations
import json,re,shutil,sys
from pathlib import Path

if len(sys.argv)!=3:
    raise SystemExit('usage: hotfix_r54.py <android-project-root> <r54-pack-dir>')
ROOT=Path(sys.argv[1]).resolve(); PACK=Path(sys.argv[2]).resolve()
VTT=ROOT/'app/src/main/assets/vtt'
MAIN=ROOT/'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
GEMINI=ROOT/'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
RUNTIME=VTT/'r46/final-runtime.js'; CSS=VTT/'r46/final-runtime.css'; R46CSS=VTT/'r46/r46.css'; GLOBALS=VTT/'r46/globals.css'
for p in [MAIN,GEMINI,RUNTIME,CSS,R46CSS,GLOBALS,PACK/'bindings.json',PACK/'r54-start-locations.json']:
    if not p.is_file(): raise SystemExit(f'missing R54 input: {p}')

def write(p,s): p.write_text(s,encoding='utf-8')
def replace_once(s,old,new,label,required=True):
    n=s.count(old)
    if n!=1:
        if required: raise SystemExit(f'{label}: expected 1, got {n}')
        return s
    return s.replace(old,new,1)

def replace_function(src,name,replacement):
    m=re.search(rf'function\s+{re.escape(name)}\s*\([^)]*\)\s*\{{',src)
    if not m: raise SystemExit(f'function not found: {name}')
    brace=src.find('{',m.start()); depth=0; ins=False; esc=False; quote=''; end=None
    for i in range(brace,len(src)):
        c=src[i]
        if ins:
            if esc: esc=False
            elif c=='\\': esc=True
            elif c==quote: ins=False
            continue
        if c in ('"',"'",'`'): ins=True; quote=c; continue
        if c=='{': depth+=1
        elif c=='}':
            depth-=1
            if depth==0: end=i+1; break
    if end is None: raise SystemExit(f'unbalanced JS function: {name}')
    return src[:m.start()]+replacement+src[end:]

# ------------------------------------------------------------------
# A. Restore visual/data assets the user had already supplied.
# ------------------------------------------------------------------
dest=VTT/'image-library/local/r54'
if dest.exists(): shutil.rmtree(dest)
shutil.copytree(PACK,dest)
starts=json.loads((PACK/'r54-start-locations.json').read_text('utf-8'))
if len(starts)<70: raise SystemExit(f'R54 start catalog unexpectedly small: {len(starts)}')

# ------------------------------------------------------------------
# B. Real-device safe areas: R53 used PHYSICAL pixels as CSS pixels.
#    Convert Android px -> CSS dp, which removes the huge top/bottom void.
# ------------------------------------------------------------------
main=MAIN.read_text('utf-8')
needle='''        if (webView == null) return;\n        final String js = "(function(){var r=document.documentElement;if(!r)return;"'''
replacement='''        if (webView == null) return;\n        final float density = Math.max(1.0f, getResources().getDisplayMetrics().density);\n        final int cssLeft = Math.min(72, Math.round(systemInsetLeft / density));\n        final int cssTop = Math.min(72, Math.round(systemInsetTop / density));\n        final int cssRight = Math.min(72, Math.round(systemInsetRight / density));\n        final int cssBottom = Math.min(72, Math.round(systemInsetBottom / density));\n        final String js = "(function(){var r=document.documentElement;if(!r)return;"'''
main=replace_once(main,needle,replacement,'density conversion')
main=main.replace("+ systemInsetLeft + \"px');\"", "+ cssLeft + \"px');\"")
main=main.replace("+ systemInsetTop + \"px');\"", "+ cssTop + \"px');\"")
main=main.replace("+ systemInsetRight + \"px');\"", "+ cssRight + \"px');\"")
main=main.replace("+ systemInsetBottom + \"px');\"", "+ cssBottom + \"px');\"")
write(MAIN,main)

# ------------------------------------------------------------------
# C. Gemini handoff: prompt is built by the VTT but image generation occurs
#    in Gemini. Prompt is copied and sent to the Gemini app when available.
# ------------------------------------------------------------------
g=GEMINI.read_text('utf-8')
for imp,after in [
 ('import android.content.ClipData;','import android.app.Activity;'),
 ('import android.content.ClipboardManager;','import android.content.ClipData;'),
 ('import android.content.Context;','import android.content.ClipboardManager;')]:
    if imp not in g: g=g.replace(after,after+'\n'+imp,1)
if 'public String openGeminiWithPrompt(String prompt)' not in g:
    marker='''        @JavascriptInterface\n        public String generateSceneImage(String prompt, String sceneKey) {'''
    method=r'''        @JavascriptInterface
        public String openGeminiWithPrompt(String prompt) {
            String cleanPrompt = String.valueOf(prompt == null ? "" : prompt).trim();
            if (cleanPrompt.length() < 20 || cleanPrompt.length() > 12000) return fail("Prompt de ilustração inválido.");
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Prompt da cena — Braseiro VTT", cleanPrompt));
                Intent geminiApp = new Intent(Intent.ACTION_SEND);
                geminiApp.setType("text/plain");
                geminiApp.putExtra(Intent.EXTRA_TEXT, cleanPrompt);
                geminiApp.putExtra(Intent.EXTRA_TITLE, "Ilustração da cena do VTT");
                geminiApp.setPackage("com.google.android.apps.bard");
                final boolean hasGeminiApp = geminiApp.resolveActivity(activity.getPackageManager()) != null;
                final Intent launchIntent = hasGeminiApp ? geminiApp : new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"));
                activity.runOnUiThread(() -> activity.startActivity(launchIntent));
                return new JSONObject().put("ok", true).put("target", hasGeminiApp ? "gemini_app" : "gemini_web").put("clipboard", true).toString();
            } catch (Exception e) { return fail("Não foi possível abrir o Gemini."); }
        }

'''
    g=replace_once(g,marker,method+marker,'Gemini external handoff injection')
write(GEMINI,g)

# ------------------------------------------------------------------
# D. Runtime corrections.
# ------------------------------------------------------------------
rt=RUNTIME.read_text('utf-8')

# D1. All 82 catalogued start points, not eight hardcoded places.
js_starts=[]
for x in starts:
    name=str(x.get('name') or x.get('key') or 'Local')
    region=str(x.get('region') or 'Galáxia')
    grid=str(x.get('grid') or '')
    desc=str(x.get('description') or '').strip()
    if not desc: desc=f'{region}' + (f' · grade {grid}' if grid else '')
    js_starts.append({'name':name,'x':float(x['x']),'y':float(x['y']),'desc':desc,'region':region,'grid':grid,'key':x.get('key')})
start_literal='const START_POINTS='+json.dumps(js_starts,ensure_ascii=False,separators=(',',':'))+';'
rt,n=re.subn(r'const START_POINTS=\[.*?\n\];',lambda m:start_literal,rt,count=1,flags=re.S)
if n!=1: raise SystemExit(f'START_POINTS replacement failed: {n}')
# Correct region passed to sandbox when catalog knows it.
rt=rt.replace("const r=bridgeResult('initializeLivingSandbox',{startingPoint:point,regionLabel:`Região de ${point}`});", "const chosen=START_POINTS[pointIndex];const r=bridgeResult('initializeLivingSandbox',{startingPoint:point,regionLabel:chosen.region||`Região de ${point}`});",1)
# 82 markers must be compact dots, not a wall of giant numbered circles.
rt=rt.replace("${i+1}</button>`).join('')}<div class=\"final-map-caption\"", "<span class=\"sr-only\">${i+1}</span></button>`).join('')}<div class=\"final-map-caption\"",1)
# Use user's packaged galaxy map as the coordinate authority instead of forcing the English Kainrath map.
rt=rt.replace('<img src="https://kainrath.wordpress.com/wp-content/uploads/2016/06/galaxy_map-v2.4-1.png" onerror="this.onerror=null;this.src=\'assets/maps/galaxy_map.jpg\'"', '<img src="assets/maps/galaxy_map.jpg"',1)
# Search field for 82 places.
old='<div id="sandbox-list" class="final-start-list">${START_POINTS.map((p,i)=>`'
new='<input id="sandbox-point-search" class="r54-start-search" type="search" placeholder="Buscar planeta ou local…" aria-label="Buscar ponto inicial"><div id="sandbox-list" class="final-start-list">${START_POINTS.map((p,i)=>`'
rt=replace_once(rt,old,new,'start search field')
needle="const choose=i=>{pointIndex=Math.max(0,Math.min(START_POINTS.length-1,Number(i)||0));"
search_logic="const search=host.querySelector('#sandbox-point-search');search?.addEventListener('input',()=>{const q=r53Norm(search.value);list.querySelectorAll('[data-point]').forEach(el=>{const p=START_POINTS[Number(el.dataset.point)];el.hidden=Boolean(q&&!r53Norm(`${p?.name||''} ${p?.region||''} ${p?.grid||''}`).includes(q))})});\n "+needle
rt=replace_once(rt,needle,search_logic,'start search listener')

# D2. Real scene prompt + external Gemini handoff. No in-app image generation.
new_prompt=r'''function sceneGeminiPrompt(){
 const view=playerView()||{},scene=currentSceneRecord()||{};
 const narration=narrationText(),situation=String(document.querySelector('.scene-situation-paper p')?.textContent||view?.situation||'').trim();
 const location=String(view?.scene?.location?.name||scene?.locationName||scene?.title||'').trim();
 const participants=(Array.isArray(view?.participants)?view.participants:[]).map(p=>String(p?.name||'').trim()).filter(Boolean);
 const facts=[];if(location)facts.push(`Local: ${location}`);if(situation)facts.push(`Situação visível: ${situation}`);if(narration)facts.push(`Narração visível ao jogador: ${narration}`);if(participants.length)facts.push(`Participantes visíveis: ${participants.join(', ')}`);
 if(!facts.length)return '';
 return [
  'Crie uma ilustração horizontal cinematográfica para uma cena de STAR WARS — EDGE OF THE GALAXY, usada em um VTT.',
  'Estética: concept art cinematográfica, retro-industrial Star Wars, materiais gastos, iluminação dramática, atmosfera crível; não parecer captura de videogame e não parecer fotografia moderna.',
  'Use SOMENTE os fatos visíveis abaixo. Não revele informação do Mestre e não invente personagens nomeados, facções, armas, veículos ou acontecimentos não citados.',
  'Não coloque texto, letras, legendas, molduras, HUD, menus, botões, marcas d’água ou interface na imagem.',
  `FATOS VISÍVEIS DA CENA:\n${facts.join('\n')}`,
  'Composição horizontal 16:9, cenário legível e espaço visual suficiente para uso como imagem de cena.'
 ].join('\n\n');
}'''
rt=replace_function(rt,'sceneGeminiPrompt',new_prompt)
new_button=r'''function enhanceSceneGeminiButton(){
 const media=document.querySelector('.scene-media-slot');if(!media||media.querySelector('.final-scene-gemini'))return;
 const wrap=document.createElement('div');wrap.className='final-scene-gemini';wrap.innerHTML='<button type="button">✦ GERAR ILUSTRAÇÃO</button><small></small>';media.appendChild(wrap);
 const btn=wrap.querySelector('button'),msg=wrap.querySelector('small');
 btn.onclick=()=>{const g=window.BraseiroGemini,prompt=sceneGeminiPrompt();if(!prompt){msg.textContent='Inicie a aventura para montar o prompt da cena.';return}if(!g||typeof g.openGeminiWithPrompt!=='function'){msg.textContent='Handoff para Gemini indisponível.';return}btn.disabled=true;msg.textContent='Abrindo Gemini com o prompt da cena…';try{const r=safeParse(g.openGeminiWithPrompt(prompt),{});btn.disabled=false;msg.textContent=r.ok?'Prompt copiado e enviado ao Gemini.':(r.message||'Não foi possível abrir o Gemini.')}catch(err){btn.disabled=false;msg.textContent='Não foi possível abrir o Gemini.'}};
}'''
rt=replace_function(rt,'enhanceSceneGeminiButton',new_button)

# D3. Restart truly clears persistent campaign state; R53 called a method the public bridge never exposed.
new_restart=r'''function r53RestartAdventure(){
 if(!confirm('Reiniciar a aventura? Isto apaga o estado atual da campanha neste aparelho e volta à criação do mundo.'))return;
 try{
  localStorage.removeItem('braseiro.starwars.edge.state.v1');
  localStorage.removeItem('braseiro.starwars.edge.snapshots.v1');
  sessionStorage.clear();
  location.href='r46-shell.html';
 }catch(err){alert('Falha ao reiniciar: '+String(err?.message||err))}
}'''
rt=replace_function(rt,'r53RestartAdventure',new_restart)

# D4. R54 asset binding / player portrait / map context / bottom icon repair.
r54=r'''

/* R54 — recover the user's visual library and real-device affordances. */
const R54_ASSET_ROOT='image-library/local/r54/';
let r54BindingsPromise=null;
function r54Bindings(){if(!r54BindingsPromise)r54BindingsPromise=fetch(R54_ASSET_ROOT+'bindings.json').then(r=>{if(!r.ok)throw new Error('bindings');return r.json()}).catch(()=>({weapons:{},equipment:{},portraits:{},maps:{}}));return r54BindingsPromise}
function r54Norm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/[^a-z0-9]+/g,' ').trim()}
function r54AssetIndex(bind){const map=new Map();for(const group of ['weapons','equipment'])for(const item of Object.values(bind?.[group]||{}))for(const a of item.aliases||[])map.set(r54Norm(a),R54_ASSET_ROOT+item.file);return map}
async function r54EnhanceItemImages(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='09'&&route!=='08')return;
 const bind=await r54Bindings(),idx=r54AssetIndex(bind);
 for(const btn of [...document.querySelectorAll('button')].filter(x=>r54Norm(x.textContent).includes('propor ao mestre'))){
  const host=r53FindItemHost(btn);if(!host)continue;const strong=host.querySelector('strong');const name=strong?.textContent?.trim();if(!name)continue;
  const exact=idx.get(r54Norm(name));if(!exact)continue;
  host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove());const fig=document.createElement('figure');fig.className='r54-item-visual';fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${exact}">`;host.insertBefore(fig,host.firstChild);
 }
}
function r54CharacterPortrait(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06')return;
 if(document.querySelector('.r54-character-portrait'))return;const state=currentState(),c=state?.player?.character;if(!c)return;
 const species=r54Norm(c.species||c.speciesName||'');let src='';if(species.includes('twi lek')||species.includes('twilek'))src=R54_ASSET_ROOT+'portraits/twilek_female_blue.webp';
 if(!src)return;const heading=[...document.querySelectorAll('h2,h3,div')].find(el=>r54Norm(el.textContent)==='identidade');const card=heading?.closest('section,article,.card')||heading?.parentElement;if(!card)return;
 const fig=document.createElement('figure');fig.className='r54-character-portrait';fig.innerHTML=`<img src="${src}" alt="Retrato visual de ${String(c.name||'personagem').replace(/["<&]/g,'')}"><figcaption>${String(c.name||'Personagem')}</figcaption>`;heading.insertAdjacentElement('afterend',fig);
}
function r54FixSceneIcon(){
 const btn=document.querySelector('.bottom-navigation .bottom-nav-button:first-child');if(!btn)return;const icon=btn.querySelector('.reference-icon-mask');if(!icon||icon.dataset.r54Fixed)return;icon.dataset.r54Fixed='1';icon.classList.add('r54-scene-icon');icon.style.backgroundImage='none';icon.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8h16v11H4zM5 4l3 4M10 4l3 4M15 4l3 4M4 4h16v4H4z"/></svg>';
}
function r54ContextMapPreview(){
 const overlay=document.getElementById('braseiro-sandbox-setup');if(!overlay||overlay.querySelector('.r54-map-hint'))return;const selected=overlay.querySelector('#sandbox-selected-point');if(!selected)return;
 const box=document.createElement('div');box.className='r54-map-hint';box.innerHTML='<small>82 locais catalogados disponíveis. O mapa galáctico serve para localização; mapas locais aparecem quando existe asset confirmado.</small><figure hidden><img alt="Mapa local"><figcaption></figcaption></figure>';selected.parentElement.insertAdjacentElement('afterend',box);
 const refresh=()=>{const name=String(selected.textContent||'');const fig=box.querySelector('figure'),img=fig.querySelector('img'),cap=fig.querySelector('figcaption');let src='',label='';if(/shantipole/i.test(name)){src=R54_ASSET_ROOT+'maps/shantipole_base.webp';label='Base Rebelde Shantipole — mapa do acervo'}else if(/mos eisley/i.test(name)){src='https://kainrath.wordpress.com/wp-content/uploads/2024/07/mos-eisley-city-1.png';label='Mos Eisley — mapa local'}else if(/tatooine/i.test(name)){src='https://kainrath.wordpress.com/wp-content/uploads/2021/01/tatooine-map.png';label='Tatooine — mapa local'}if(src){img.src=src;cap.textContent=label;fig.hidden=false}else fig.hidden=true};
 const mo=new MutationObserver(refresh);mo.observe(selected,{childList:true,subtree:true,characterData:true});refresh();
}
function r54Tick(){r54FixSceneIcon();r54CharacterPortrait();r54ContextMapPreview();r54EnhanceItemImages()}
r54Tick();new MutationObserver(()=>r54Tick()).observe(document.documentElement,{subtree:true,childList:true});addEventListener('popstate',()=>setTimeout(r54Tick,0));
'''
if '/* R54 — recover the user' not in rt:
    end=rt.rfind('})();')
    if end<0: raise SystemExit('runtime IIFE end missing')
    rt=rt[:end]+r54+'\n'+rt[end:]
write(RUNTIME,rt)

# ------------------------------------------------------------------
# E. R54 CSS: compact real device chrome, exact icon geometry, 82 map dots,
#    proper item assets and player portrait.
# ------------------------------------------------------------------
css=CSS.read_text('utf-8')
css += r'''

/* R54 physical-device corrections */
.r53-fab{bottom:calc(var(--nav-h) + var(--safe-bottom) + 5px);min-height:36px;padding:5px 9px;font-size:.58rem}
.r53-fab img{width:18px;height:18px}
.bottom-navigation .bottom-nav-button{transform:none!important;padding-top:.48rem!important;padding-bottom:.38rem!important;gap:.18rem!important}
.bottom-navigation .bottom-nav-button[aria-current="page"]{transform:none!important}
.bottom-navigation .reference-icon-mask{width:1.45rem!important;height:1.45rem!important;flex:0 0 1.45rem!important;margin:0!important;transform:none!important;background-position:center!important;background-size:contain!important}
.bottom-navigation .r54-scene-icon{display:grid!important;place-items:center!important;background-image:none!important}
.bottom-navigation .r54-scene-icon svg{width:1.45rem;height:1.45rem;display:block;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round;overflow:visible}
.final-start-map button[data-point]{width:13px!important;height:13px!important;min-width:13px!important;min-height:13px!important;padding:0!important;border-width:1px!important;font-size:0!important;box-shadow:0 0 4px #000!important}
.final-start-map button[data-point].is-selected{width:21px!important;height:21px!important;background:#35b9e8!important;border:2px solid #fff!important;z-index:4}
.r54-start-search{width:100%;min-height:42px;margin:0 0 7px;background:#0b1013;color:#fff;border:1px solid rgba(100,169,186,.55);border-radius:7px;padding:7px 10px}
.final-start-list{max-height:255px!important}
.r54-map-hint{display:grid;gap:7px;margin-top:7px}.r54-map-hint>small{color:#9aa8ae;font-size:.64rem;line-height:1.3}.r54-map-hint figure{margin:0;border:1px solid rgba(100,169,186,.5);background:#05090c;border-radius:7px;overflow:hidden}.r54-map-hint figure[hidden]{display:none}.r54-map-hint img{display:block;width:100%;max-height:185px;object-fit:contain}.r54-map-hint figcaption{padding:5px 7px;font-size:.62rem;color:#b9c7cc}
.r54-item-visual{margin:0 0 10px;width:100%;height:118px;display:grid;place-items:center;background:#050708;border:1px solid rgba(100,169,186,.38);border-radius:7px;overflow:hidden}.r54-item-visual img{width:100%;height:100%;object-fit:contain}
.r54-character-portrait{margin:8px auto 14px;width:118px;display:grid;gap:5px;text-align:center}.r54-character-portrait img{width:118px;height:118px;border-radius:50%;object-fit:cover;border:2px solid #d49d42;box-shadow:0 0 0 4px rgba(212,157,66,.12),0 8px 24px #000}.r54-character-portrait figcaption{font-size:.7rem;font-weight:800;color:#d5d8dc}
'''
write(CSS,css)

# Prevent R46 scene layout from double-counting safe top after Android px->dp conversion.
r46=R46CSS.read_text('utf-8')
r46=r46.replace('.app-shell.scene-mode{padding-top:var(--safe-top);padding-bottom:calc(var(--r46-nav-h) + var(--safe-bottom))}', '.app-shell.scene-mode{padding-top:var(--safe-top);padding-bottom:calc(var(--r46-nav-h) + var(--safe-bottom))}')
write(R46CSS,r46)

# Gates
checks={
 '82 starts':len(starts)==82,
 'density px/dp':'systemInsetTop / density' in MAIN.read_text('utf-8'),
 'gemini handoff':'openGeminiWithPrompt' in GEMINI.read_text('utf-8') and 'openGeminiWithPrompt(prompt)' in RUNTIME.read_text('utf-8'),
 'no scene in-app call':'g.generateSceneImage(prompt,currentSceneKey())' not in RUNTIME.read_text('utf-8'),
 'restart storage clear':"localStorage.removeItem('braseiro.starwars.edge.state.v1')" in RUNTIME.read_text('utf-8'),
 'portrait':'twilek_female_blue.webp' in RUNTIME.read_text('utf-8'),
 'weapon assets':len(list((dest/'weapons').glob('*.webp')))>=30,
 'equipment assets':len(list((dest/'equipment').glob('*.webp')))>=20,
 'map asset':(dest/'maps/shantipole_base.webp').is_file(),
 'scene icon':'r54-scene-icon' in RUNTIME.read_text('utf-8'),
}
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit('R54 gates failed: '+', '.join(bad))
print('R54 HOTFIX PASS',checks)
