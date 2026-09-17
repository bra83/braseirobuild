#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_device_p1.py <android-project-root>')
root = Path(sys.argv[1]).resolve()
vtt = root / 'app/src/main/assets/vtt'
main = root / 'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
gemini = root / 'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
runtime = vtt / 'r46/final-runtime.js'
shell = vtt / 'r46-shell.html'
p0js = vtt / 'r54-device-p0.js'
p1js = vtt / 'r54-device-p1.js'
for p in (main, gemini, runtime, shell, p0js):
    if not p.is_file():
        raise SystemExit(f'missing {p}')

def replace_braced_function(src: str, signature: str, replacement: str) -> str:
    start = src.find(signature)
    if start < 0:
        raise SystemExit(f'signature not found: {signature}')
    brace = src.find('{', start)
    if brace < 0:
        raise SystemExit(f'opening brace missing: {signature}')
    depth = 0
    quote = None
    esc = False
    for i in range(brace, len(src)):
        ch = src[i]
        if quote:
            if esc: esc = False
            elif ch == '\\': esc = True
            elif ch == quote: quote = None
            continue
        if ch in ('"', "'", '`'):
            quote = ch
            continue
        if ch == '{': depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return src[:start] + replacement + src[i+1:]
    raise SystemExit(f'unbalanced function: {signature}')

# ---------------------------------------------------------------------------
# 1. Android file chooser: character photo must be selectable from gallery.
# The R54 host forced every HTML <input type=file> to application/json.
# ---------------------------------------------------------------------------
ms = main.read_text('utf-8')
old = '''                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);\n                    intent.addCategory(Intent.CATEGORY_OPENABLE);\n                    intent.setType("application/json");\n                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain"});\n                    startActivityForResult(intent, PICK_JSON_REQUEST);'''
new = '''                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);\n                    intent.addCategory(Intent.CATEGORY_OPENABLE);\n                    String[] acceptTypes = params == null ? null : params.getAcceptTypes();\n                    boolean wantsImage = false;\n                    if (acceptTypes != null) for (String type : acceptTypes)\n                        if (type != null && type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) wantsImage = true;\n                    if (wantsImage) {\n                        intent.setType("image/*");\n                    } else {\n                        intent.setType("application/json");\n                        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain"});\n                    }\n                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);\n                    startActivityForResult(intent, PICK_JSON_REQUEST);'''
if old not in ms:
    if 'boolean wantsImage' not in ms:
        raise SystemExit('MainActivity file chooser block not found')
else:
    ms = ms.replace(old, new, 1)
main.write_text(ms, 'utf-8')

# ---------------------------------------------------------------------------
# 2. Gemini key persistence: AndroidKeyStore remains primary, with an app-private
# SharedPreferences fallback for devices where the AES/GCM keystore path fails.
# Also verify the key immediately after save and clear both stores on removal.
# ---------------------------------------------------------------------------
gs = gemini.read_text('utf-8')
if 'R54_DEVICE_P1_KEY_FALLBACK' not in gs:
    gs = gs.replace('private static final String KEY_ALIAS = "braseiro.starwars.gemini.api.v1";',
                    'private static final String KEY_ALIAS = "braseiro.starwars.gemini.api.v1";\n    private static final String KEY_PREFS = "braseiro.starwars.gemini.key.fallback.v1"; // R54_DEVICE_P1_KEY_FALLBACK\n    private static final String KEY_FALLBACK = "apiKey";', 1)
    gs = gs.replace('private final SharedPreferences modelPreferences;',
                    'private final SharedPreferences modelPreferences;\n    private final SharedPreferences keyPreferences;', 1)
    gs = gs.replace('this.modelPreferences = activity.getSharedPreferences(MODEL_PREFS, Activity.MODE_PRIVATE);',
                    'this.modelPreferences = activity.getSharedPreferences(MODEL_PREFS, Activity.MODE_PRIVATE);\n        this.keyPreferences = activity.getSharedPreferences(KEY_PREFS, Activity.MODE_PRIVATE);', 1)

configure = r'''public String configureApiKey(String apiKey) {
            String clean = String.valueOf(apiKey == null ? "" : apiKey).trim();
            if (clean.length() < 20 || clean.length() > 256 || clean.matches(".*\\s+.*"))
                return fail("Informe uma chave Gemini válida, sem espaços.");
            boolean secureStored = false;
            try {
                encryptAndStore(clean);
                String verify = decryptStoredKey();
                secureStored = clean.equals(verify);
            } catch (Exception ignored) { secureStored = false; }
            if (secureStored) {
                keyPreferences.edit().remove(KEY_FALLBACK).commit();
            } else {
                if (!keyPreferences.edit().putString(KEY_FALLBACK, clean).commit())
                    return fail("Não foi possível salvar a chave Gemini neste dispositivo.");
            }
            try {
                String verify = readApiKey();
                if (!clean.equals(verify)) return fail("A chave Gemini não permaneceu salva no dispositivo.");
                notifyEvent("configured", new JSONObject().put("configured", true));
                return new JSONObject().put("ok", true).put("configured", true)
                        .put("storage", secureStored ? "android_keystore" : "app_private_fallback").toString();
            } catch (Exception e) { return fail("Não foi possível confirmar a chave Gemini salva."); }
        }'''
gs = replace_braced_function(gs, 'public String configureApiKey(String apiKey) {', configure)

clear = r'''public String clearApiKey() {
            try {
                if (secretFile.exists() && !secretFile.delete()) throw new IllegalStateException("Falha ao remover segredo local.");
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS);
                keyPreferences.edit().remove(KEY_FALLBACK).commit();
                notifyEvent("cleared", new JSONObject().put("configured", false));
                return new JSONObject().put("ok", true).put("configured", false).toString();
            } catch (Exception e) { return fail("Não foi possível remover a chave Gemini."); }
        }'''
gs = replace_braced_function(gs, 'public String clearApiKey() {', clear)

if 'private String readApiKey()' not in gs:
    marker = '    private boolean hasStoredKey() {'
    helper = '''    private String readApiKey() {\n        try {\n            String secure = decryptStoredKey();\n            if (secure != null && !secure.isBlank()) return secure;\n        } catch (Exception ignored) { }\n        String fallback = keyPreferences.getString(KEY_FALLBACK, null);\n        return fallback == null || fallback.isBlank() ? null : fallback.trim();\n    }\n\n'''
    if marker not in gs: raise SystemExit('hasStoredKey marker missing')
    gs = gs.replace(marker, helper + marker, 1)

has = '''private boolean hasStoredKey() {\n        String key = readApiKey();\n        return key != null && !key.isBlank();\n    }'''
gs = replace_braced_function(gs, 'private boolean hasStoredKey() {', has)
gs = gs.replace('String apiKey = decryptStoredKey();', 'String apiKey = readApiKey();')
gemini.write_text(gs, 'utf-8')

# ---------------------------------------------------------------------------
# 3. Existing P0 portrait binder must respect a user-selected portrait.
# ---------------------------------------------------------------------------
ps = p0js.read_text('utf-8')
oldp = "const species=norm(c.species||c.speciesName||'');let src='';if(species.includes('twi lek')||species.includes('twilek'))src='image-library/local/r54/portraits/twilek_female_blue.webp';else src=`image-library/local/tokens/heroes/heroes_${String((hash((c.name||'')+'|'+species)%15)+1).padStart(2,'0')}.png`;"
newp = "const species=norm(c.species||c.speciesName||'');let src=localStorage.getItem('braseiro.starwars.character.photo.v1')||'';if(!src&&(species.includes('twi lek')||species.includes('twilek')))src='image-library/local/r54/portraits/twilek_female_blue.webp';else if(!src)src=`image-library/local/tokens/heroes/heroes_${String((hash((c.name||'')+'|'+species)%15)+1).padStart(2,'0')}.png`;"
if oldp in ps:
    ps = ps.replace(oldp, newp, 1)
elif 'braseiro.starwars.character.photo.v1' not in ps:
    raise SystemExit('P0 portrait binding pattern changed')
p0js.write_text(ps, 'utf-8')

# ---------------------------------------------------------------------------
# 4. A provider-placeholder narration is NOT a started adventure. This makes the
# real INICIAR AVENTURA gate reappear after the Gemini key is configured.
# ---------------------------------------------------------------------------
rt = runtime.read_text('utf-8')
adventure = r'''function adventureStarted(){
 const st=currentState();const n=String(st?.master?.narration||st?.scene?.narration||'').trim();
 if(!n)return false;
 if(/sess[aã]o iniciada.*mestre local|narra[cç][aã]o livre exige provedor|provedor configurado/i.test(n))return false;
 return true;
}'''
rt = replace_braced_function(rt, 'function adventureStarted(){', adventure)
runtime.write_text(rt, 'utf-8')

# ---------------------------------------------------------------------------
# 5. P1 mobile interaction layer: in-game approved map with pinch/drag/zoom and
# current-location marker; portrait 3:4 + change photo; edit ficha; reliable API
# key save UI; visible MUNDO VIVO / NPCS / ROBÔS menu.
# ---------------------------------------------------------------------------
js = r'''/* R54_DEVICE_P1 — real Android interaction repair. */
(()=>{'use strict';
 if(window.__R54_DEVICE_P1__)return;window.__R54_DEVICE_P1__=true;
 const KEY='braseiro.starwars.edge.state.v1',PHOTO='braseiro.starwars.character.photo.v1';
 const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const arr=v=>Array.isArray(v)?v:(v&&typeof v==='object'?Object.values(v):[]);
 function parse(v){if(v&&typeof v==='object')return v;try{return JSON.parse(String(v||''))}catch{return null}}
 function call(name,...args){try{const b=window.BraseiroGameBridge,f=b?.[name];if(typeof f!=='function')return null;return parse(f.apply(b,args))||f.apply(b,args)}catch{return null}}
 function state(){const r=call('readState');return r?.ok?r.data:null}
 function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
 function addStyle(){if(document.getElementById('r54-device-p1-style'))return;const e=document.createElement('style');e.id='r54-device-p1-style';e.textContent=`
 .r54-character-portrait{width:min(54vw,230px)!important;max-width:230px!important;margin:12px auto 18px!important;overflow:visible!important;background:transparent!important;border:0!important}
 .r54-character-portrait img{width:100%!important;aspect-ratio:3/4!important;height:auto!important;object-fit:cover!important;border:1px solid rgba(212,157,66,.6)!important;border-radius:10px!important;background:#070a0d!important}
 .r54-character-portrait figcaption{padding:7px 0!important;text-align:center!important;background:transparent!important}
 .r54-p1-char-actions{display:grid!important;grid-template-columns:1fr 1fr!important;gap:8px!important;margin-top:8px!important}
 .r54-p1-char-actions button,.r54-p1-btn{min-height:44px!important;border:1px solid #9e7737!important;border-radius:10px!important;background:#151a1e!important;color:#eee6d8!important;font-weight:800!important;padding:8px 10px!important}
 .r54-p1-world-map{margin:12px 18px 22px!important;border:1px solid rgba(79,197,238,.45)!important;border-radius:12px!important;background:#05090c!important;overflow:hidden!important;position:relative!important;z-index:2!important}
 .r54-p1-map-head{display:flex!important;align-items:center!important;justify-content:space-between!important;gap:8px!important;padding:10px!important;background:#0b1217!important;color:#e9e2d5!important}
 .r54-p1-map-head strong{font-size:15px!important}.r54-p1-map-controls{display:flex!important;gap:6px!important}.r54-p1-map-controls button{min-width:42px!important;height:42px!important;border:1px solid #547989!important;border-radius:8px!important;background:#101a20!important;color:#dff6ff!important;font-weight:900!important}
 .r54-p1-map-viewport{position:relative!important;width:100%!important;aspect-ratio:2700/2425!important;overflow:hidden!important;background:#000!important;touch-action:none!important;user-select:none!important}
 .r54-p1-map-canvas{position:absolute!important;left:0!important;top:0!important;width:100%!important;height:100%!important;transform-origin:0 0!important;will-change:transform!important}
 .r54-p1-map-canvas>img{display:block!important;width:100%!important;height:100%!important;object-fit:fill!important;pointer-events:none!important}
 .r54-p1-you{position:absolute!important;width:18px!important;height:18px!important;border-radius:50%!important;background:#ffb33b!important;border:3px solid #fff!important;box-shadow:0 0 0 4px rgba(0,0,0,.7),0 0 18px #ffb33b!important;transform:translate(-50%,-50%)!important;z-index:4!important}
 .r54-p1-you::after{content:'VOCÊ';position:absolute;left:14px;top:-9px;background:rgba(0,0,0,.88);color:#fff;padding:3px 6px;border-radius:5px;font-size:8px;font-weight:900;white-space:nowrap}
 .r54-p1-map-status{padding:9px 12px!important;color:#cddbe1!important;font-size:12px!important;background:#091015!important}
 .r54-p1-world-open{position:fixed!important;right:12px!important;top:calc(env(safe-area-inset-top,0px) + 72px)!important;z-index:9000!important;min-height:40px!important;padding:0 12px!important;border-radius:20px!important;border:1px solid #45c9f5!important;background:#081820!important;color:#6ddcff!important;font-weight:900!important;box-shadow:0 4px 16px rgba(0,0,0,.45)!important}
 .r54-p1-modal{position:fixed!important;inset:0!important;z-index:12000!important;background:rgba(0,0,0,.86)!important;display:flex!important;align-items:flex-end!important;justify-content:center!important;padding-top:max(12px,env(safe-area-inset-top))!important}
 .r54-p1-sheet{width:100%!important;max-width:680px!important;max-height:88vh!important;overflow:auto!important;background:#0b1014!important;border:1px solid #41535d!important;border-radius:18px 18px 0 0!important;padding:16px 16px calc(18px + env(safe-area-inset-bottom))!important;color:#eee!important}
 .r54-p1-sheet-head{display:flex!important;justify-content:space-between!important;align-items:center!important;gap:8px!important;margin-bottom:12px!important}.r54-p1-sheet-head h2{margin:0!important;font-size:22px!important}.r54-p1-close{width:44px!important;height:44px!important;border:1px solid #566!important;border-radius:50%!important;background:#141b20!important;color:#fff!important;font-size:22px!important}
 .r54-p1-tabs{display:flex!important;gap:6px!important;overflow:auto!important;margin-bottom:12px!important}.r54-p1-tabs button{white-space:nowrap!important;min-height:40px!important;padding:0 12px!important;border:1px solid #475861!important;border-radius:8px!important;background:#12191e!important;color:#ddd!important;font-weight:800!important}.r54-p1-tabs button.active{border-color:#4dccf5!important;color:#4dccf5!important}
 .r54-p1-list{display:grid!important;gap:8px!important}.r54-p1-row{border:1px solid #33424a!important;border-radius:10px!important;padding:10px!important;background:#11171b!important}.r54-p1-row strong{display:block!important;color:#e8d3aa!important}.r54-p1-row small{display:block!important;margin-top:4px!important;color:#abb8bd!important}
 .r54-p1-form{display:grid!important;gap:10px!important}.r54-p1-form label{display:grid!important;gap:5px!important;color:#d9af5f!important;font-weight:800!important}.r54-p1-form input,.r54-p1-form textarea{width:100%!important;box-sizing:border-box!important;min-height:44px!important;border:1px solid #52616a!important;border-radius:8px!important;background:#11171b!important;color:#fff!important;padding:10px!important;font:inherit!important}.r54-p1-form textarea{min-height:92px!important}
 .r54-p1-save-status{display:block!important;margin-top:8px!important;color:#63d5ff!important;font-weight:800!important}
 body.r54-p1-map-route .r54-p1-old-galaxy{display:none!important}
 `;document.head.appendChild(e)}

 function currentPoint(st){
  const w=st?.world||{},s=w?.sandbox||{};let p=s.currentMapPoint||s.mapPoint||s.startMapPoint||null;const id=st?.scene?.locationId||s.currentLocationId||s.startLocationId||'';
  if(id){const loc=arr(w.locations).find(x=>String(x?.id||x?.locationId||x?.key||'')===String(id));if(loc)p=loc.mapPoint||loc.coordinates||loc.point||((loc.x!=null&&loc.y!=null)?{x:loc.x,y:loc.y}:p)}
  let x=Number(p?.x),y=Number(p?.y);if(!Number.isFinite(x)||!Number.isFinite(y))return null;if(x<=1&&y<=1){x*=100;y*=100}return{x:Math.max(0,Math.min(100,x)),y:Math.max(0,Math.min(100,y)),label:String(st?.scene?.locationName||s.currentLocationName||s.startingPoint||s.startLocationName||id||'Local atual')};
 }
 function hideOldGalaxy(){if(route()!=='16')return;document.body.classList.add('r54-p1-map-route');for(const img of document.querySelectorAll('main img,.app-content img')){if(img.closest('.r54-p1-world-map'))continue;const t=(img.currentSrc||img.src||'')+' '+(img.alt||'');if(/galaxy[_ -]?map|kainrath|galaxy-v2|mapa da gal[aá]xia/i.test(t)){const h=img.closest('figure,[class*=image],[class*=preview]');(h||img).classList.add('r54-p1-old-galaxy')}}}
 function mountWorldMap(){
  if(route()!=='16')return;hideOldGalaxy();let card=document.querySelector('.r54-p1-world-map');if(card){updateMarker(card);return}
  const host=document.querySelector('.app-content,main')||document.body;card=document.createElement('section');card.className='r54-p1-world-map';card.innerHTML=`<div class="r54-p1-map-head"><strong>MAPA GALÁCTICO · LOCAL ATUAL</strong><div class="r54-p1-map-controls"><button type="button" data-zout>−</button><button type="button" data-zin>+</button><button type="button" data-center aria-label="Centralizar em você">◎</button></div></div><div class="r54-p1-map-viewport"><div class="r54-p1-map-canvas"><img src="assets/maps/9752360iy2h61.png" alt="Mapa galáctico aprovado"><span class="r54-p1-you" hidden></span></div></div><div class="r54-p1-map-status">Arraste com um dedo. Faça pinça ou use +/− para ampliar.</div>`;
  const title=[...host.querySelectorAll('h1,h2')].find(x=>norm(x.textContent).includes('galactico'));if(title)title.insertAdjacentElement('afterend',card);else host.prepend(card);
  const vp=card.querySelector('.r54-p1-map-viewport'),canvas=card.querySelector('.r54-p1-map-canvas');let z=1,tx=0,ty=0;const pointers=new Map();let pinch=null;
  const clamp=()=>{const w=vp.clientWidth,h=vp.clientHeight;tx=Math.min(0,Math.max(w-w*z,tx));ty=Math.min(0,Math.max(h-h*z,ty))};const apply=()=>{clamp();canvas.style.transform=`translate(${tx}px,${ty}px) scale(${z})`;card.dataset.zoom=String(z)};
  const zoom=(nz,cx=vp.clientWidth/2,cy=vp.clientHeight/2)=>{nz=Math.max(1,Math.min(6,nz));const wx=(cx-tx)/z,wy=(cy-ty)/z;tx=cx-wx*nz;ty=cy-wy*nz;z=nz;apply()};
  vp.addEventListener('wheel',e=>{e.preventDefault();const r=vp.getBoundingClientRect();zoom(z*(e.deltaY<0?1.2:1/1.2),e.clientX-r.left,e.clientY-r.top)},{passive:false});
  vp.addEventListener('pointerdown',e=>{vp.setPointerCapture?.(e.pointerId);pointers.set(e.pointerId,{x:e.clientX,y:e.clientY,ox:tx,oy:ty});if(pointers.size===2){const a=[...pointers.values()];pinch={d:Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y),z,tx,ty,mx:(a[0].x+a[1].x)/2,my:(a[0].y+a[1].y)/2}}});
  vp.addEventListener('pointermove',e=>{if(!pointers.has(e.pointerId))return;const p=pointers.get(e.pointerId);p.x=e.clientX;p.y=e.clientY;if(pointers.size===1){tx=p.ox+(e.clientX-p.x+0);ty=p.oy+(e.clientY-p.y+0);/* overwritten below using stored start */}const vals=[...pointers.values()];if(vals.length===1){const q=vals[0];if(q.sx==null){q.sx=q.x;q.sy=q.y;q.stx=tx;q.sty=ty}tx=q.stx+(q.x-q.sx);ty=q.sty+(q.y-q.sy);apply()}else if(vals.length===2&&pinch){const d=Math.hypot(vals[0].x-vals[1].x,vals[0].y-vals[1].y);const r=vp.getBoundingClientRect();zoom(pinch.z*(d/Math.max(1,pinch.d)),pinch.mx-r.left,pinch.my-r.top)}});
  const up=e=>{pointers.delete(e.pointerId);pinch=null;if(pointers.size===1){const q=[...pointers.values()][0];q.sx=q.x;q.sy=q.y;q.stx=tx;q.sty=ty}};vp.addEventListener('pointerup',up);vp.addEventListener('pointercancel',up);
  card.querySelector('[data-zin]').onclick=()=>zoom(z*1.35);card.querySelector('[data-zout]').onclick=()=>zoom(z/1.35);card.querySelector('[data-center]').onclick=()=>centerCurrent(card,zoom,()=>({z,tx,ty}),v=>{z=v.z;tx=v.tx;ty=v.ty;apply()});card._zoom=zoom;card._setTransform=v=>{z=v.z;tx=v.tx;ty=v.ty;apply()};apply();updateMarker(card);setTimeout(()=>centerCurrent(card,zoom,()=>({z,tx,ty}),v=>{z=v.z;tx=v.tx;ty=v.ty;apply()}),120);
 }
 function updateMarker(card){const p=currentPoint(state()),m=card?.querySelector('.r54-p1-you'),s=card?.querySelector('.r54-p1-map-status');if(!m)return;if(!p){m.hidden=true;if(s)s.textContent='Local atual ainda sem coordenadas no mapa.';return}m.hidden=false;m.style.left=p.x+'%';m.style.top=p.y+'%';m.title=p.label;if(s)s.textContent=`Você está em: ${p.label}. Arraste e amplie para conferir a região.`}
 function centerCurrent(card,zoom,get,set){const p=currentPoint(state()),vp=card?.querySelector('.r54-p1-map-viewport');if(!p||!vp)return;const z=2.8,w=vp.clientWidth,h=vp.clientHeight;set({z,tx:w/2-(p.x/100)*w*z,ty:h/2-(p.y/100)*h*z})}

 function resizePhoto(file,done){const r=new FileReader();r.onload=()=>{const im=new Image();im.onload=()=>{const cv=document.createElement('canvas');cv.width=600;cv.height=800;const c=cv.getContext('2d');const scale=Math.max(600/im.naturalWidth,800/im.naturalHeight),sw=600/scale,sh=800/scale,sx=(im.naturalWidth-sw)/2,sy=(im.naturalHeight-sh)/2;c.drawImage(im,sx,sy,sw,sh,0,0,600,800);done(cv.toDataURL('image/jpeg',.82))};im.src=String(r.result)};r.readAsDataURL(file)}
 function choosePhoto(){const inp=document.createElement('input');inp.type='file';inp.accept='image/*';inp.style.display='none';document.body.appendChild(inp);inp.onchange=()=>{const f=inp.files?.[0];if(!f){inp.remove();return}if(f.size>15*1024*1024){alert('Escolha uma imagem menor que 15 MB.');inp.remove();return}resizePhoto(f,data=>{try{localStorage.setItem(PHOTO,data);fixCharacterTools(true)}catch{alert('Não foi possível salvar a foto.')}inp.remove()})};inp.click()}
 function charValue(c,names){for(const n of names){const v=c?.[n];if(v!=null&&typeof v!=='object')return v}return''}
 function setText(c,names,v){for(const n of names)if(Object.prototype.hasOwnProperty.call(c,n)){c[n]=v;return}c[names[0]]=v}
 function setNumber(c,names,v){const n=Number(v);if(!Number.isFinite(n))return;for(const k of names)if(Object.prototype.hasOwnProperty.call(c,k)){c[k]=n;return}c[names[0]]=n}
 function editCharacter(){const st=state(),c=st?.player?.character;if(!c)return;const modal=document.createElement('div');modal.className='r54-p1-modal';modal.innerHTML=`<section class="r54-p1-sheet"><div class="r54-p1-sheet-head"><h2>EDITAR FICHA</h2><button class="r54-p1-close">×</button></div><form class="r54-p1-form"><label>Nome<input name="name" value="${esc(charValue(c,['name']))}"></label><label>Espécie<input name="species" value="${esc(charValue(c,['species','speciesName']))}"></label><label>Carreira<input name="career" value="${esc(charValue(c,['career','careerName']))}"></label><label>Especialização<input name="specialization" value="${esc(charValue(c,['specialization','specializationName','primarySpecialization']))}"></label><label>Motivação<textarea name="motivation">${esc(typeof c.motivation==='string'?c.motivation:(c.motivation?.text||c.motivation?.name||''))}</textarea></label><label>Ferimentos atuais<input name="wounds" inputmode="numeric" value="${esc(charValue(c,['wounds','currentWounds']))}"></label><label>Tensão atual<input name="strain" inputmode="numeric" value="${esc(charValue(c,['strain','currentStrain']))}"></label><label>Créditos<input name="credits" inputmode="numeric" value="${esc(charValue(c,['credits']))}"></label><button class="r54-p1-btn" type="submit">SALVAR FICHA</button><output class="r54-p1-save-status"></output></form></section>`;document.body.appendChild(modal);modal.querySelector('.r54-p1-close').onclick=()=>modal.remove();modal.addEventListener('click',e=>{if(e.target===modal)modal.remove()});modal.querySelector('form').onsubmit=e=>{e.preventDefault();try{const raw=JSON.parse(localStorage.getItem(KEY)||'null');const ch=raw?.player?.character;if(!ch)throw new Error('Ficha não encontrada');const fd=new FormData(e.currentTarget);setText(ch,['name'],String(fd.get('name')||'').trim());setText(ch,['species','speciesName'],String(fd.get('species')||'').trim());setText(ch,['career','careerName'],String(fd.get('career')||'').trim());setText(ch,['specialization','specializationName','primarySpecialization'],String(fd.get('specialization')||'').trim());const mot=String(fd.get('motivation')||'').trim();if(ch.motivation&&typeof ch.motivation==='object'){if('text'in ch.motivation)ch.motivation.text=mot;else if('name'in ch.motivation)ch.motivation.name=mot;else ch.motivation.text=mot}else ch.motivation=mot;setNumber(ch,['wounds','currentWounds'],fd.get('wounds'));setNumber(ch,['strain','currentStrain'],fd.get('strain'));setNumber(ch,['credits'],fd.get('credits'));localStorage.setItem(KEY,JSON.stringify(raw));e.currentTarget.querySelector('output').textContent='Ficha salva.';setTimeout(()=>location.reload(),250)}catch(err){e.currentTarget.querySelector('output').textContent='Erro: '+String(err?.message||err)}}}
 function fixCharacterTools(force=false){if(route()!=='06')return;const fig=document.querySelector('.r54-character-portrait'),st=state(),c=st?.player?.character;if(!fig||!c)return;const custom=localStorage.getItem(PHOTO);const img=fig.querySelector('img');if(custom&&img&&img.src!==custom)img.src=custom;if(force)fig.querySelector('.r54-p1-char-actions')?.remove();if(fig.querySelector('.r54-p1-char-actions'))return;const box=document.createElement('div');box.className='r54-p1-char-actions';box.innerHTML='<button type="button" data-photo>TROCAR FOTO</button><button type="button" data-edit>EDITAR FICHA</button>';fig.appendChild(box);box.querySelector('[data-photo]').onclick=choosePhoto;box.querySelector('[data-edit]').onclick=editCharacter}

 function geminiStatus(){try{return parse(window.BraseiroGemini?.getStatus?.())||{}}catch{return{}}}
 function fixGeminiSettings(){if(route()!=='29')return;const cards=[...document.querySelectorAll('section,article,[class*=card],div')];const card=cards.find(x=>norm(x.textContent).includes('chave da api gemini')&&x.querySelector('input'));if(!card)return;const input=[...card.querySelectorAll('input')].find(x=>/chave|gemini/i.test(x.placeholder||''))||card.querySelector('input');const save=[...card.querySelectorAll('button')].find(x=>norm(x.textContent).includes('salvar chave'));if(!input||!save)return;let out=card.querySelector('.r54-p1-save-status');if(!out){out=document.createElement('output');out.className='r54-p1-save-status';save.insertAdjacentElement('afterend',out)}const enable=()=>{save.disabled=String(input.value||'').trim().length<20};if(!input.dataset.p1){input.dataset.p1='1';input.addEventListener('input',enable)}enable();if(!save.dataset.p1){save.dataset.p1='1';save.addEventListener('click',e=>{e.preventDefault();e.stopImmediatePropagation();const key=String(input.value||'').trim();if(key.length<20){out.textContent='Cole uma chave Gemini válida.';return}let r;try{r=parse(window.BraseiroGemini?.configureApiKey?.(key))}catch(err){r={ok:false,message:String(err)}}if(r?.ok){input.value='';out.textContent='Chave salva no aparelho.';const text=[...card.querySelectorAll('p,div')].find(x=>/^chave:/i.test(String(x.textContent||'').trim()));if(text)text.innerHTML='<strong>Chave:</strong> configurada';setTimeout(()=>window.dispatchEvent(new CustomEvent('braseiroGeminiNativeEvent',{detail:{type:'configured'}})),0)}else out.textContent=r?.message||'Falha ao salvar a chave.';enable()},true)}}

 function extractCatalog(){const r=call('imageVisualEntityCatalog');const d=r?.data??r;return arr(d?.entities||d?.items||d)}
 function worldData(){const st=state()||{},w=st.world||{},catalog=extractCatalog();let droids=arr(w.npcs).filter(x=>/droid|droide|robot|astromech|protocol|r2-|c-3/i.test(JSON.stringify(x)));if(!droids.length)droids=catalog.filter(x=>/droid|droide|robot|astromech|protocol|r2-|c-3/i.test(JSON.stringify(x)));return{state:st,world:w,factions:arr(w.factions),npcs:arr(w.npcs),locations:arr(w.locations),droids}}
 function row(x){if(typeof x==='string')return`<div class="r54-p1-row"><strong>${esc(x)}</strong></div>`;const name=x?.name||x?.title||x?.label||x?.id||x?.key||'Registro';const sub=x?.summary||x?.description||x?.type||x?.kind||x?.status||'';return`<div class="r54-p1-row"><strong>${esc(name)}</strong>${sub?`<small>${esc(sub)}</small>`:''}</div>`}
 function openWorld(){document.querySelector('.r54-p1-modal[data-world]')?.remove();const d=worldData(),m=document.createElement('div');m.className='r54-p1-modal';m.dataset.world='1';m.innerHTML=`<section class="r54-p1-sheet"><div class="r54-p1-sheet-head"><h2>MUNDO VIVO</h2><button class="r54-p1-close">×</button></div><div class="r54-p1-tabs"><button data-tab="world" class="active">MUNDO</button><button data-tab="npcs">NPCS (${d.npcs.length})</button><button data-tab="droids">ROBÔS (${d.droids.length})</button><button data-tab="factions">FACÇÕES (${d.factions.length})</button><button data-tab="locations">LOCAIS (${d.locations.length})</button></div><div class="r54-p1-list" data-body></div></section>`;document.body.appendChild(m);const body=m.querySelector('[data-body]');const render=tab=>{m.querySelectorAll('[data-tab]').forEach(b=>b.classList.toggle('active',b.dataset.tab===tab));if(tab==='world'){const s=d.world?.sandbox||{};body.innerHTML=[row({name:'Campanha persistente',description:s.initialized?'Inicializada':'Ainda não inicializada'}),row({name:'Local atual',description:d.state?.scene?.locationName||s.currentLocationName||s.startingPoint||'—'}),row({name:'Conteúdo vivo',description:`${d.factions.length} facções · ${d.npcs.length} NPCs · ${d.locations.length} locais · ${arr(d.world.routes).length} rotas`})].join('')}else{const list=tab==='npcs'?d.npcs:tab==='droids'?d.droids:tab==='factions'?d.factions:d.locations;body.innerHTML=list.length?list.slice(0,200).map(row).join(''):'<div class="r54-p1-row"><strong>Nenhum registro disponível ainda.</strong></div>'}};m.querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>render(b.dataset.tab));m.querySelector('.r54-p1-close').onclick=()=>m.remove();m.addEventListener('click',e=>{if(e.target===m)m.remove()});render('world')}
 function worldButton(){let b=document.querySelector('.r54-p1-world-open');if(!b){b=document.createElement('button');b.className='r54-p1-world-open';b.type='button';b.textContent='MUNDO';b.onclick=openWorld;document.body.appendChild(b)}}

 function tick(){addStyle();mountWorldMap();fixCharacterTools();fixGeminiSettings();worldButton()}
 let q=false;function schedule(){if(q)return;q=true;requestAnimationFrame(()=>{q=false;tick()})}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
 new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true});addEventListener('popstate',schedule);addEventListener('braseiro:r21-state-changed',schedule);addEventListener('braseiroGeminiNativeEvent',schedule);document.addEventListener('click',()=>setTimeout(schedule,0),true);
})();
'''
p1js.write_text(js, 'utf-8')
sh = shell.read_text('utf-8')
tag = '<script src="r54-device-p1.js"></script>'
if tag not in sh:
    if '</body>' not in sh: raise SystemExit('shell body end missing')
    sh = sh.replace('</body>', tag + '\n</body>', 1)
shell.write_text(sh, 'utf-8')

checks = {
    'image chooser': 'boolean wantsImage' in main.read_text('utf-8'),
    'gemini fallback': 'R54_DEVICE_P1_KEY_FALLBACK' in gemini.read_text('utf-8') and 'readApiKey()' in gemini.read_text('utf-8'),
    'adventure retry': 'narra[cç][aã]o livre exige provedor' in runtime.read_text('utf-8'),
    'p1 mounted': tag in shell.read_text('utf-8'),
    'map zoom': 'r54-p1-map-viewport' in p1js.read_text('utf-8') and 'pointermove' in p1js.read_text('utf-8'),
    'portrait edit': 'TROCAR FOTO' in p1js.read_text('utf-8') and 'EDITAR FICHA' in p1js.read_text('utf-8'),
    'world menu': 'MUNDO VIVO' in p1js.read_text('utf-8') and 'ROBÔS' in p1js.read_text('utf-8'),
    'api save': 'configureApiKey' in p1js.read_text('utf-8'),
}
if not all(checks.values()): raise SystemExit(checks)
print('R54_DEVICE_P1_PATCH_PASS', checks)
