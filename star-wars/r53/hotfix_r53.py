#!/usr/bin/env python3
from __future__ import annotations
import json,re,sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: hotfix_r53.py <android-project-root>')
ROOT=Path(sys.argv[1]).resolve()
VTT=ROOT/'app/src/main/assets/vtt'
MAIN=ROOT/'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
GLOB=VTT/'r46/globals.css'
R46CSS=VTT/'r46/r46.css'
RUNTIME=VTT/'r46/final-runtime.js'
RUNTIMECSS=VTT/'r46/final-runtime.css'
BUNDLE=VTT/'r46/r46-shell.bundle.js'
MANIFEST=VTT/'image-library/manifest.json'
LIBDATA=VTT/'js/library/imageLibraryData.mjs'
UIBRIDGE=VTT/'js/core/uiBridge.bundle.js'
for p in [MAIN,GLOB,R46CSS,RUNTIME,RUNTIMECSS,BUNDLE,MANIFEST,LIBDATA,UIBRIDGE]:
    if not p.is_file(): raise SystemExit(f'missing required R53 input: {p}')

def write(p:Path,s:str): p.write_text(s,encoding='utf-8')
def replace_once(s,old,new,label):
    n=s.count(old)
    if n!=1: raise SystemExit(f'{label}: expected exactly 1 match, got {n}')
    return s.replace(old,new,1)

def replace_js_function(src:str,name:str,new_body:str):
    m=re.search(rf'function\s+{re.escape(name)}\s*\([^)]*\)\s*\{{',src)
    if not m: raise SystemExit(f'function not found: {name}')
    brace=src.find('{',m.start()); depth=0; ins=False; esc=False; quote=''
    end=None
    for i in range(brace,len(src)):
        ch=src[i]
        if ins:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch==quote: ins=False
            continue
        if ch in ('"',"'",'`'):
            ins=True; quote=ch; continue
        if ch=='{': depth+=1
        elif ch=='}':
            depth-=1
            if depth==0: end=i+1; break
    if end is None: raise SystemExit(f'unbalanced function {name}')
    return src[:m.start()]+f'function {name}(){{{new_body}}}'+src[end:]

# 1) Android 15/16 edge-to-edge: pass native insets into CSS instead of padding the WebView.
main=MAIN.read_text(encoding='utf-8')
if '--android-safe-top' not in main:
    main=replace_once(main,
        '    private ValueCallback<Uri[]> pendingJsonChooser;\n    private final ExecutorService libraryExecutor',
        '    private ValueCallback<Uri[]> pendingJsonChooser;\n    private int systemInsetLeft = 0;\n    private int systemInsetTop = 0;\n    private int systemInsetRight = 0;\n    private int systemInsetBottom = 0;\n    private final ExecutorService libraryExecutor',
        'native inset fields')
    old='''        webView.setFitsSystemWindows(true);\n        webView.setOnApplyWindowInsetsListener((view, insets) -> {\n            int left = insets.getSystemWindowInsetLeft();\n            int top = insets.getSystemWindowInsetTop();\n            int right = insets.getSystemWindowInsetRight();\n            int bottom = insets.getSystemWindowInsetBottom();\n            view.setPadding(left, top, right, bottom);\n            return insets;\n        });'''
    new='''        webView.setFitsSystemWindows(false);\n        webView.setOnApplyWindowInsetsListener((view, insets) -> {\n            systemInsetLeft = insets.getSystemWindowInsetLeft();\n            systemInsetTop = insets.getSystemWindowInsetTop();\n            systemInsetRight = insets.getSystemWindowInsetRight();\n            systemInsetBottom = insets.getSystemWindowInsetBottom();\n            applySystemInsetsToWeb();\n            return insets;\n        });'''
    main=replace_once(main,old,new,'native inset listener')
    anchor='''            @Override\n            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {'''
    injected='''            @Override\n            public void onPageFinished(WebView view, String url) {\n                super.onPageFinished(view, url);\n                applySystemInsetsToWeb();\n            }\n\n            @Override\n            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {'''
    main=replace_once(main,anchor,injected,'native onPageFinished')
    anchor2='''    @Override\n    @SuppressWarnings("deprecation")\n    public void onBackPressed() {'''
    injected2='''    private void applySystemInsetsToWeb() {\n        if (webView == null) return;\n        final String js = "(function(){var r=document.documentElement;if(!r)return;"\n                + "r.style.setProperty('--android-safe-left','" + systemInsetLeft + "px');"\n                + "r.style.setProperty('--android-safe-top','" + systemInsetTop + "px');"\n                + "r.style.setProperty('--android-safe-right','" + systemInsetRight + "px');"\n                + "r.style.setProperty('--android-safe-bottom','" + systemInsetBottom + "px');"\n                + "})();";\n        runOnUiThread(() -> webView.evaluateJavascript(js, null));\n    }\n\n    @Override\n    @SuppressWarnings("deprecation")\n    public void onBackPressed() {'''
    main=replace_once(main,anchor2,injected2,'native css inset bridge')
write(MAIN,main)

# 2) CSS safe areas + reliable PNG icons in Android WebView.
g=GLOB.read_text(encoding='utf-8')
old='--top-h:4.25rem; --nav-h:5.2rem; --safe-top:env(safe-area-inset-top,0px); --safe-bottom:env(safe-area-inset-bottom,0px);'
new='--top-h:4.25rem; --nav-h:5.2rem; --android-safe-left:0px; --android-safe-top:0px; --android-safe-right:0px; --android-safe-bottom:0px; --safe-left:max(env(safe-area-inset-left,0px),var(--android-safe-left)); --safe-top:max(env(safe-area-inset-top,0px),var(--android-safe-top)); --safe-right:max(env(safe-area-inset-right,0px),var(--android-safe-right)); --safe-bottom:max(env(safe-area-inset-bottom,0px),var(--android-safe-bottom));'
if old in g: g=g.replace(old,new,1)
g=g.replace('padding:calc(var(--safe-top) + .65rem) max(1rem,env(safe-area-inset-left)) .65rem;', 'padding:calc(var(--safe-top) + .65rem) max(1rem,var(--safe-left)) .65rem;',1)
g=g.replace('padding:0 max(.4rem,env(safe-area-inset-left)) var(--safe-bottom);', 'padding:0 max(.4rem,var(--safe-left)) var(--safe-bottom);',1)
write(GLOB,g)

r=R46CSS.read_text(encoding='utf-8')
if '--r46-safe-bottom:var(--safe-bottom)' not in r:
    r=r.replace('--r46-safe-bottom:env(safe-area-inset-bottom,0px)', '--r46-safe-bottom:var(--safe-bottom)',1)
if '.app-shell.scene-mode{padding-top:var(--safe-top)' not in r:
    r += '\n.app-shell.scene-mode{padding-top:var(--safe-top);padding-bottom:calc(var(--r46-nav-h) + var(--safe-bottom))}\n'
write(R46CSS,r)

css=RUNTIMECSS.read_text(encoding='utf-8')
css=css.replace('padding:calc(12px + env(safe-area-inset-top)) 14px calc(12px + env(safe-area-inset-bottom));','padding:calc(12px + var(--safe-top)) max(14px,var(--safe-left)) calc(12px + var(--safe-bottom));',1)
css=css.replace('max-height:calc(100dvh - 34px);','max-height:calc(100dvh - var(--safe-top) - var(--safe-bottom) - 24px);',1)
css=css.replace('bottom:calc(82px + env(safe-area-inset-bottom));','bottom:calc(82px + var(--safe-bottom));',1)
if 'R53 real-device hardening' not in css:
    css += r'''

/* R53 real-device hardening — preserves R46 visual language. */
.bottom-nav-button .reference-icon-mask{
  background-color:transparent!important;background-image:var(--ref-image)!important;
  background-repeat:no-repeat!important;background-position:center!important;background-size:contain!important;
  -webkit-mask-image:none!important;mask-image:none!important;
}
.final-utility-shortcuts{display:none!important}
.r53-fab{position:fixed;bottom:calc(var(--nav-h) + var(--safe-bottom) + 10px);z-index:2147483500;min-height:42px;border:1px solid #d49d42;background:rgba(20,25,28,.96);color:#f0e4cf;border-radius:999px;padding:7px 11px;font-weight:900;font-size:.68rem;letter-spacing:.06em;box-shadow:0 5px 22px #000;display:flex;align-items:center;gap:7px}
.r53-dice-fab{left:max(10px,var(--safe-left))}.r53-restart-fab{right:max(10px,var(--safe-right));background:#652514}
.r53-fab img{width:22px;height:22px;object-fit:contain}
.r53-overlay{position:fixed;inset:0;z-index:2147483640;background:rgba(6,9,11,.985);padding:calc(var(--safe-top) + 12px) max(12px,var(--safe-right)) calc(var(--safe-bottom) + 12px) max(12px,var(--safe-left));overflow:auto;color:#d5d8dc;font-family:inherit}
.r53-modal{width:min(100%,440px);margin:0 auto;background:#181c1f;border:1px solid #d49d42;border-radius:10px;padding:14px;box-shadow:0 18px 70px #000;display:grid;gap:11px}
.r53-modal-head{display:flex;align-items:center;justify-content:space-between;gap:10px;border-bottom:1px solid rgba(212,157,66,.35);padding-bottom:9px}.r53-modal-head h2{margin:0;font-size:1.15rem}.r53-close{width:42px;height:42px;border:1px solid #596269;background:#252b2f;color:#fff;border-radius:7px;font-size:1.4rem}
.r53-dice-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}.r53-dice-grid label{display:grid;gap:4px;font-size:.69rem;color:#b8bec2;font-weight:800}.r53-dice-grid input{min-height:42px;width:100%;background:#0d1113;color:#fff;border:1px solid #596269;border-radius:6px;padding:7px}
.r53-primary{min-height:44px;border:1px solid #d49d42;background:#652514;color:#fff;border-radius:7px;font-weight:900}.r53-secondary{min-height:42px;border:1px solid #6b7378;background:#252b2f;color:#fff;border-radius:7px;font-weight:800}
.r53-result{min-height:70px;background:#0d1316;border:1px solid rgba(100,169,186,.45);border-radius:7px;padding:10px;line-height:1.45}.r53-result strong{color:#d49d42}.r53-result .ok{color:#8ec8a0}.r53-result .bad{color:#e5a28f}
.r53-library-tools{display:grid;grid-template-columns:1fr auto;gap:7px}.r53-library-tools input{min-height:44px;background:#0d1113;color:#fff;border:1px solid #596269;border-radius:6px;padding:7px 9px}.r53-docs{display:grid;gap:7px}.r53-doc{border:1px solid #596269;background:#22272b;border-radius:7px;padding:9px;display:grid;gap:3px}.r53-doc strong{font-size:.9rem}.r53-doc small{color:#a9b0b5}.r53-empty{color:#a9b0b5;padding:16px 4px;text-align:center}
.r53-item-visual{margin:0 0 10px;width:100%;min-height:96px;max-height:150px;display:grid;place-items:center;background:#0a0d0f;border:1px solid rgba(100,169,186,.3);border-radius:7px;overflow:hidden}.r53-item-visual img{width:100%;height:118px;object-fit:contain}.r53-item-visual.is-fallback img{width:46px;height:46px;opacity:.5}.r53-item-visual figcaption{display:none}
'''
write(RUNTIMECSS,css)

# 3) The overlay manifest is content, not shell geometry. Make the runtime bridge see the actual merged manifest.
manifest=json.loads(MANIFEST.read_text(encoding='utf-8'))
compact=json.dumps(manifest,ensure_ascii=False,separators=(',',':'))
def replace_frozen_manifest(path:Path):
    s=path.read_text(encoding='utf-8'); pos=s.find('IMAGE_LIBRARY_MANIFEST')
    if pos<0: raise SystemExit(f'{path}: IMAGE_LIBRARY_MANIFEST marker missing')
    freeze=s.find('Object.freeze(',pos); start=s.find('{',freeze)
    if freeze<0 or start<0: raise SystemExit(f'{path}: frozen manifest start missing')
    depth=0;ins=False;esc=False;q='';end=None
    for i in range(start,len(s)):
        ch=s[i]
        if ins:
            if esc:esc=False
            elif ch=='\\':esc=True
            elif ch==q:ins=False
            continue
        if ch in ('"',"'"):
            ins=True;q=ch;continue
        if ch=='{':depth+=1
        elif ch=='}':
            depth-=1
            if depth==0:end=i+1;break
    if end is None: raise SystemExit(f'{path}: frozen manifest unbalanced')
    write(path,s[:start]+compact+s[end:])
for p in [LIBDATA,UIBRIDGE]: replace_frozen_manifest(p)

# 4) R46 shell bundle: route-30 defensive render, contextual maps, direct remote display.
b=BUNDLE.read_text(encoding='utf-8')
b=b.replace('...asset.tags].filter(Boolean)', '...(Array.isArray(asset.tags) ? asset.tags : [])].filter(Boolean)')
old="""        if (route === '16')\n            setSelectedImageAssetId('local.map.galaxy');"""
new="""        if (route === '16') {\n            const context = JSON.stringify(campaignRead ?? {}).toLocaleLowerCase('pt-BR');\n            const contextualMap = context.includes('mos eisley') ? 'external.kainrath.map.mos-eisley-city' : context.includes('tatooine') ? 'external.kainrath.map.tatooine-2021' : 'external.kainrath.map.galaxy-v24';\n            setSelectedImageAssetId(contextualMap);\n        }"""
if old in b: b=b.replace(old,new,1)
old2="""        if (selectedImageCache?.cached && selectedImageCache.runtimeUrl)\n            return selectedImageCache.runtimeUrl;\n        return imageLibraryNative === 'unavailable' ? (0, imageLibraryBridgeClient_1.browserExternalFallbackUrl)(selectedImageAsset) : null;"""
new2="""        if (selectedImageCache?.cached && selectedImageCache.runtimeUrl)\n            return selectedImageCache.runtimeUrl;\n        if (selectedImageAsset.remoteReady && selectedImageAsset.remoteUrl)\n            return selectedImageAsset.remoteUrl;\n        return imageLibraryNative === 'unavailable' ? (0, imageLibraryBridgeClient_1.browserExternalFallbackUrl)(selectedImageAsset) : null;"""
if old2 in b: b=b.replace(old2,new2,1)
write(BUNDLE,b)

# 5) R46 runtime: remove the duplicate top-left shortcuts and install focused R53 controls.
rt=RUNTIME.read_text(encoding='utf-8')
rt=replace_js_function(rt,'enhanceUtilityShortcuts',"document.querySelectorAll('.final-utility-shortcuts').forEach(el=>el.remove());")
# Start-point MAP: use the catalogued v2.4 galaxy map online, with the packaged map as offline fallback.
rt=rt.replace('<img src="assets/maps/galaxy_map.jpg"', '<img src="https://kainrath.wordpress.com/wp-content/uploads/2016/06/galaxy_map-v2.4-1.png" onerror="this.onerror=null;this.src=\'assets/maps/galaxy_map.jpg\'"',1)

r53_js=r'''

/* R53 targeted runtime fixes: no R46 redesign. */
const R53_PATCH_ID='r53-real-device-2026-09-16';
function r53Norm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/\s+/g,' ').trim()}
function r53Bridge(){return (typeof window!=='undefined'&&window.BraseiroGameBridge&&typeof window.BraseiroGameBridge==='object')?window.BraseiroGameBridge:null}
function r53Unwrap(raw){if(raw&&raw.ok===false)throw new Error(raw.message||raw.error||'Operação recusada pelo motor.');let data=raw&&raw.ok===true?raw.data:raw;if(data&&data.check)data=data.check;return data}
function r53Close(selector){document.querySelector(selector)?.remove()}
function r53OpenDice(){
 if(document.querySelector('.r53-dice-overlay'))return;
 const ov=document.createElement('div');ov.className='r53-overlay r53-dice-overlay';
 ov.innerHTML='<div class="r53-modal"><div class="r53-modal-head"><div><small>DADOS NARRATIVOS</small><h2>Rolagem livre</h2></div><button class="r53-close" aria-label="Fechar">×</button></div><p style="margin:0;color:#aeb7bc;font-size:.78rem">Usa o mesmo motor de dados narrativos do jogo. Característica + perícia montam os dados positivos; dificuldade, desafio, bônus e contratempo completam a parada.</p><div class="r53-dice-grid"><label>Característica<input data-die="characteristic" type="number" min="0" max="5" value="2"></label><label>Perícia<input data-die="skill" type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-die="difficulty" type="number" min="0" max="5" value="2"></label><label>Desafio<input data-die="challenge" type="number" min="0" max="5" value="0"></label><label>Bônus<input data-die="boost" type="number" min="0" max="5" value="0"></label><label>Contratempo<input data-die="setback" type="number" min="0" max="5" value="0"></label></div><button class="r53-primary r53-roll">ROLAR DADOS</button><div class="r53-result" aria-live="polite">Aguardando rolagem.</div></div>';
 document.body.appendChild(ov);
 ov.querySelector('.r53-close').onclick=()=>ov.remove();
 ov.addEventListener('click',e=>{if(e.target===ov)ov.remove()});
 ov.querySelector('.r53-roll').onclick=()=>{
  const out=ov.querySelector('.r53-result');const b=r53Bridge();
  if(!b||typeof b.resolveCheck!=='function'){out.innerHTML='<span class="bad">Motor de dados indisponível.</span>';return}
  const n=k=>Math.max(0,Math.min(5,Number(ov.querySelector(`[data-die="${k}"]`).value)||0));
  try{
   const d=r53Unwrap(b.resolveCheck({characteristic:n('characteristic'),skill:n('skill'),difficulty:n('difficulty'),challenge:n('challenge'),boost:n('boost'),setback:n('setback')}))||{};
   const success=Number(d.netSuccess||0),adv=Number(d.netAdvantage||0),tri=Number(d.triumph||0),des=Number(d.despair||0);
   out.innerHTML=`<strong>Resultado</strong><br><span class="${success>0?'ok':'bad'}">${success>0?'SUCESSO':'FALHA'} · ${success} sucesso(s) líquido(s)</span><br>${adv} vantagem(ns) líquida(s) · ${tri} triunfo(s) · ${des} desespero(s)`;
  }catch(err){out.innerHTML='<span class="bad">'+String(err?.message||err)+'</span>'}
 };
}
function r53EnsureDiceFab(){
 if(document.querySelector('.r53-dice-fab'))return;
 const b=document.createElement('button');b.type='button';b.className='r53-fab r53-dice-fab';b.innerHTML='<img src="assets/ui/icons-v2/dice.png" alt="">DADOS';b.onclick=r53OpenDice;document.body.appendChild(b);
}
function r53RestartAdventure(){
 const b=r53Bridge();if(!b||typeof b.newCampaign!=='function'){alert('O motor não expôs o comando de nova campanha.');return}
 if(!confirm('Reiniciar a aventura? Isto apaga o estado atual da campanha neste aparelho e volta à criação do mundo.'))return;
 try{const raw=b.newCampaign('Edge of the Galaxy');if(raw&&raw.ok===false)throw new Error(raw.message||'Falha ao reiniciar campanha.');location.reload()}catch(err){alert(String(err?.message||err))}
}
function r53EnsureRestart(){
 const old=document.querySelector('.r53-restart-fab');
 if(!adventureStarted()){old?.remove();return}
 if(old)return;
 const b=document.createElement('button');b.type='button';b.className='r53-fab r53-restart-fab';b.textContent='↻ REINICIAR';b.onclick=r53RestartAdventure;document.body.appendChild(b);
}
function r53OpenLibrary(){
 if(document.querySelector('.r53-library-overlay'))return;
 const ov=document.createElement('div');ov.className='r53-overlay r53-library-overlay';
 ov.innerHTML='<div class="r53-modal"><div class="r53-modal-head"><div><small>BIBLIOTECA LOCAL</small><h2>Fontes e PDFs</h2></div><button class="r53-close" aria-label="Fechar">×</button></div><div class="r53-library-tools"><input type="search" placeholder="Buscar nas fontes locais…" aria-label="Buscar na biblioteca"><button class="r53-secondary r53-search">BUSCAR</button></div><div style="display:grid;grid-template-columns:1fr 1fr;gap:7px"><button class="r53-primary r53-import">IMPORTAR PDF</button><button class="r53-secondary r53-refresh">ATUALIZAR</button></div><div class="r53-lib-status" style="font-size:.72rem;color:#9ccbd7"></div><div class="r53-docs"></div></div>';
 document.body.appendChild(ov);const docs=ov.querySelector('.r53-docs'),status=ov.querySelector('.r53-lib-status');
 const native=()=>window.BraseiroLibrary;
 const render=(rows)=>{docs.innerHTML='';if(!Array.isArray(rows)||!rows.length){docs.innerHTML='<div class="r53-empty">Nenhum PDF importado.</div>';return}for(const d of rows){const el=document.createElement('div');el.className='r53-doc';const title=String(d.title||d.fileName||'PDF');const st=String(d.status||'ANEXADO');const pages=Number(d.pageCount||0);const auth=String(d.authorityType||'SUPLEMENTO');el.innerHTML=`<strong>${title.replace(/[<&]/g,'')}</strong><small>${st} · ${pages?pages+' pág. · ':''}${auth}</small>`;docs.appendChild(el)}};
 const refresh=()=>{try{const api=native();if(!api||typeof api.listDocuments!=='function'){status.textContent='Biblioteca Android indisponível neste host.';render([]);return}const raw=api.listDocuments();render(typeof raw==='string'?JSON.parse(raw):raw);status.textContent='Biblioteca pronta.'}catch(err){status.textContent=String(err?.message||err);render([])}};
 ov.querySelector('.r53-close').onclick=()=>ov.remove();ov.addEventListener('click',e=>{if(e.target===ov)ov.remove()});ov.querySelector('.r53-refresh').onclick=refresh;
 ov.querySelector('.r53-import').onclick=()=>{try{const api=native();if(!api||typeof api.pickPdf!=='function')throw new Error('Importação de PDF indisponível.');api.pickPdf();status.textContent='Selecione o PDF…'}catch(err){status.textContent=String(err?.message||err)}};
 ov.querySelector('.r53-search').onclick=()=>{try{const q=ov.querySelector('input').value.trim();const api=native();if(!q){refresh();return}if(!api||typeof api.searchDocuments!=='function')throw new Error('Busca local indisponível.');const raw=api.searchDocuments(q,25),data=typeof raw==='string'?JSON.parse(raw):raw;render(data?.results||[]);status.textContent=data?.grounded?'Resultados encontrados nas fontes locais.':'Busca concluída.'}catch(err){status.textContent=String(err?.message||err)}};
 const previous=window.braseiroLibraryNativeEvent;window.braseiroLibraryNativeEvent=(raw)=>{try{if(typeof previous==='function')previous(raw)}finally{setTimeout(refresh,120)}};
 refresh();
}
function r53InterceptLibrary(){
 if(document.documentElement.dataset.r53LibraryCapture)return;document.documentElement.dataset.r53LibraryCapture='1';
 document.addEventListener('click',e=>{const hit=e.target?.closest?.('button,a,[role="button"],.list-item');if(!hit)return;const t=r53Norm(hit.textContent);if(!t.startsWith('biblioteca'))return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();r53OpenLibrary()},true);
}
function r53FindItemHost(button){let el=button.parentElement,best=null;for(let i=0;el&&i<7;i++,el=el.parentElement){const proposes=[...el.querySelectorAll('button')].filter(x=>r53Norm(x.textContent).includes('propor ao mestre'));const strong=el.querySelector('strong');if(proposes.length===1&&strong)best=el;if(proposes.length>1)break}return best||button.parentElement}
const r53ImageCache=new Map();
function r53PickAsset(name){
 const key=r53Norm(name);if(r53ImageCache.has(key))return r53ImageCache.get(key);const b=r53Bridge();let assets=[];
 try{if(b&&typeof b.searchImageLibrary==='function'){const raw=b.searchImageLibrary({query:name,category:'equipment',limit:30});const d=raw?.ok===true?raw.data:raw;if(Array.isArray(d))assets=d}}catch(_){ }
 const words=key.split(/[^a-z0-9]+/).filter(x=>x.length>2);let best=null,score=-1;
 for(const a of assets){if(a?.category!=='equipment')continue;const title=r53Norm(a.title),tags=r53Norm(Array.isArray(a.tags)?a.tags.join(' '):'');let s=0;if(title===key)s+=100;if(title.includes(key)||key.includes(title))s+=35;for(const w of words)if((title+' '+tags).includes(w))s+=5;if(a.sourceType!=='external_reference')s+=3;if(s>score){score=s;best=a}}
 const picked=score>=10?best:null;r53ImageCache.set(key,picked);return picked;
}
function r53AssetUrl(a){if(!a)return null;if(a.sourceType==='external_reference')return a.remoteReady&&a.remoteUrl?a.remoteUrl:null;return a.localPath||null}
function r53EnhanceItemImages(){
 const params=new URLSearchParams(location.search),route=params.get('screen')||'00';if(route!=='09'&&route!=='08')return;
 for(const btn of [...document.querySelectorAll('button')].filter(x=>r53Norm(x.textContent).includes('propor ao mestre'))){const host=r53FindItemHost(btn);if(!host||host.querySelector('.r53-item-visual'))continue;const strong=host.querySelector('strong');const name=strong?.textContent?.trim();if(!name)continue;const asset=r53PickAsset(name),src=r53AssetUrl(asset)||'assets/ui/icons-v2/inventory.png';const fig=document.createElement('figure');fig.className='r53-item-visual'+(asset?'':' is-fallback');fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${src}"><figcaption>${asset?asset.title:'Sem imagem específica associada'}</figcaption>`;host.insertBefore(fig,host.firstChild)}
}
function r53RemoveBadShortcuts(){document.querySelectorAll('.final-utility-shortcuts').forEach(el=>el.remove())}
function r53Tick(){r53RemoveBadShortcuts();r53EnsureDiceFab();r53EnsureRestart();r53EnhanceItemImages()}
r53InterceptLibrary();r53Tick();new MutationObserver(()=>r53Tick()).observe(document.documentElement,{subtree:true,childList:true});addEventListener('popstate',()=>setTimeout(r53Tick,0));
'''
if R53_PATCH_ID if False else False: pass
if 'R53 targeted runtime fixes' not in rt:
    idx=rt.rfind('})();')
    if idx<0: raise SystemExit('final-runtime IIFE end not found')
    rt=rt[:idx]+r53_js+'\n'+rt[idx:]
write(RUNTIME,rt)

# Final static gates.
checks={
 'native safe top':'--android-safe-top' in MAIN.read_text('utf-8'),
 'icon fallback':'background-image:var(--ref-image)!important' in RUNTIMECSS.read_text('utf-8'),
 'duplicate shortcuts disabled':"function enhanceUtilityShortcuts(){document.querySelectorAll('.final-utility-shortcuts').forEach(el=>el.remove());}" in RUNTIME.read_text('utf-8'),
 'dice UI':'R53 targeted runtime fixes' in RUNTIME.read_text('utf-8') and 'ROLAR DADOS' in RUNTIME.read_text('utf-8'),
 'restart UI':'REINICIAR' in RUNTIME.read_text('utf-8'),
 'library safe overlay':'r53OpenLibrary' in RUNTIME.read_text('utf-8'),
 'context map':'external.kainrath.map.mos-eisley-city' in BUNDLE.read_text('utf-8'),
 'remote map display':'selectedImageAsset.remoteReady && selectedImageAsset.remoteUrl' in BUNDLE.read_text('utf-8'),
 'library tags hardened':'Array.isArray(asset.tags)' in BUNDLE.read_text('utf-8'),
}
missing=[k for k,v in checks.items() if not v]
if missing: raise SystemExit('R53 gate failed: '+', '.join(missing))
print('R53 hotfix PASS:',', '.join(checks))
