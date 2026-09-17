#!/usr/bin/env python3
from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_device_p0.py <android-project-root>')
root = Path(sys.argv[1]).resolve()
vtt = root / 'app/src/main/assets/vtt'
main = root / 'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
runtime = vtt / 'r46/final-runtime.js'
shell = vtt / 'r46-shell.html'
p0js = vtt / 'r54-device-p0.js'
for p in (main, runtime, shell):
    if not p.is_file(): raise SystemExit(f'missing {p}')

# 1) REAL ANDROID FILE ORIGIN: R54 loads local JSON via fetch(). The previous
# host explicitly disabled file->file XHR/fetch, so it worked under CI HTTP but
# failed in the actual APK (file://). Keep universal/remote file access disabled.
s = main.read_text('utf-8')
s = s.replace('settings.setAllowFileAccessFromFileURLs(false);', 'settings.setAllowFileAccessFromFileURLs(true); // R54_DEVICE_P0: allow packaged file:// JSON/assets', 1)
s = s.replace('settings.setAllowContentAccess(false);', 'settings.setAllowContentAccess(true); // R54_DEVICE_P0: document-provider content URIs', 1)
# Persist PDF URI permission when the document provider grants it.
old = '''        Uri uri = data.getData();\n        libraryExecutor.submit(() -> importPdf(uri));'''
new = '''        Uri uri = data.getData();\n        try {\n            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);\n            if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)\n                getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);\n        } catch (Exception ignored) { }\n        libraryExecutor.submit(() -> importPdf(uri));'''
if old in s: s = s.replace(old, new, 1)
# Add persistent/read flags to PDF picker.
picker = '''                        intent.addCategory(Intent.CATEGORY_OPENABLE);\n                        intent.setType("application/pdf");'''
picker_new = '''                        intent.addCategory(Intent.CATEGORY_OPENABLE);\n                        intent.setType("application/pdf");\n                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);'''
if picker in s: s = s.replace(picker, picker_new, 1)
if 'R54_DEVICE_P0' not in s: raise SystemExit('MainActivity P0 patch not applied')
main.write_text(s, 'utf-8')

# 2) STARTUP MUST NEVER DEPEND ON GEMINI/NETWORK. Bootstrap the local campaign
# synchronously, then ask Barbara for narration in the background. A network
# failure must not prevent the game from starting.
rt = runtime.read_text('utf-8')
start = rt.find('async function startBarbaraOpening(){')
if start < 0: raise SystemExit('startBarbaraOpening not found')
brace = rt.find('{', start); depth=0; quote=None; esc=False; end=None
for i in range(brace, len(rt)):
    ch=rt[i]
    if quote:
        if esc: esc=False
        elif ch=='\\': esc=True
        elif ch==quote: quote=None
        continue
    if ch in ('"', "'", '`'): quote=ch; continue
    if ch=='{': depth += 1
    elif ch=='}':
        depth -= 1
        if depth == 0: end=i+1; break
if end is None: raise SystemExit('startBarbaraOpening unbalanced')
helper = r'''async function startBarbaraOpening(){
 // R54_DEVICE_P0_START: local bootstrap is authoritative and never waits for network.
 const prep=bridgeResult('startLivingAdventure',{});if(!prep?.ok)return prep;
 const game=globalThis.BraseiroGameBridge;
 if(game&&typeof game.submitPlayerIntent==='function'){
  const req=prep.data?.masterOpeningRequest||{};const location=String(req.locationName||'o ponto inicial');
  const opportunity=String(req.opportunity?.summary||req.opportunity?.title||'').trim();
  const npc=String(req.npc?.name||'').trim();const faction=String(req.faction?.name||'').trim();
  const facts=[`Local inicial: ${location}.`,opportunity?`Situação ativa: ${opportunity}.`:'',npc?`NPC conhecido/presente: ${npc}.`:'',faction?`Facção relevante: ${faction}.`:''].filter(Boolean).join(' ');
  const intentText=`[ABERTURA_BARBARA] Inicie a aventura agora como Mestre Barbara. ${facts} Gere a primeira cena a partir do mundo vivo persistente, sem inventar uma missão obrigatória. Não resuma, não escolha nem aja pelo personagem. Narre a situação em português do Brasil conforme o contrato do Mestre e termine deixando a decisão ao jogador.`;
  try{Promise.resolve(game.submitPlayerIntent(intentText)).catch(()=>{});}catch(_){/* offline: campaign remains started */}
 }
 return prep;
}'''
rt = rt[:start] + helper + rt[end:]
runtime.write_text(rt, 'utf-8')

# 3/4/5) Device visual layer: requested approved blue/white galaxy map,
# compact start points, actual character portrait fallback, and weapon images.
js = r'''/* R54_DEVICE_P0 — actual-device repair: map, portrait, weapon visuals. */
(()=>{'use strict';
 if(window.__R54_DEVICE_P0__)return;window.__R54_DEVICE_P0__=true;
 const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function state(){try{const r=window.BraseiroGameBridge?.readState?.();return r?.ok?r.data:null}catch{return null}}
 function style(){if(document.getElementById('r54-device-p0-style'))return;const e=document.createElement('style');e.id='r54-device-p0-style';e.textContent=`
 /* startup map: never allow the 82 points to become giant touch circles */
 #sandbox-map.final-start-map{position:relative!important;width:100%!important;max-width:100%!important;aspect-ratio:2700/2425!important;height:auto!important;max-height:min(66vh,620px)!important;overflow:hidden!important;background:#02060a!important;border-radius:10px!important}
 #sandbox-map.final-start-map>img{display:block!important;width:100%!important;height:100%!important;max-width:100%!important;max-height:100%!important;object-fit:contain!important}
 #sandbox-map button[data-point]{position:absolute!important;width:11px!important;height:11px!important;min-width:11px!important;min-height:11px!important;max-width:11px!important;max-height:11px!important;padding:0!important;margin:0!important;border:2px solid #d8e7ef!important;border-radius:50%!important;background:#35b7df!important;box-shadow:0 0 0 2px rgba(0,0,0,.55),0 0 7px rgba(53,183,223,.8)!important;font-size:0!important;line-height:0!important;overflow:visible!important;transform:translate(-50%,-50%)!important;z-index:4!important}
 #sandbox-map button[data-point][aria-pressed="true"],#sandbox-map button[data-point].selected{width:15px!important;height:15px!important;min-width:15px!important;min-height:15px!important;max-width:15px!important;max-height:15px!important;background:#f0b44f!important;border-color:#fff3ce!important;z-index:6!important}
 .r54-character-portrait{margin:10px 0 14px!important;width:100%!important;max-width:330px!important;border:1px solid rgba(212,157,66,.55)!important;border-radius:10px!important;overflow:hidden!important;background:#080b0d!important}
 .r54-character-portrait img{display:block!important;width:100%!important;aspect-ratio:4/3!important;object-fit:cover!important;background:#090c0f!important}
 .r54-character-portrait figcaption{padding:7px 10px!important;color:#d7b26b!important;font-weight:800!important}
 .r54-item-visual{margin:0 0 8px!important;border-radius:8px!important;overflow:hidden!important;background:#080b0d!important;border:1px solid rgba(84,197,234,.28)!important}
 .r54-item-visual img{display:block!important;width:100%!important;height:118px!important;object-fit:contain!important;background:#050708!important}
 `;document.head.appendChild(e)}
 function fixStartMap(){const map=document.getElementById('sandbox-map');if(!map)return;const img=map.querySelector(':scope>img');if(img&&!(img.getAttribute('src')||'').includes('9752360iy2h61')){img.src='assets/maps/9752360iy2h61.png';img.removeAttribute('onerror');img.alt='Mapa galáctico aprovado com nomes de sistemas e regiões';}map.querySelectorAll('button[data-point]').forEach(b=>{b.textContent='';b.setAttribute('aria-label',b.getAttribute('title')||'Ponto inicial');});}
 function hash(s){let h=0;for(const c of String(s||''))h=((h<<5)-h+c.charCodeAt(0))|0;return Math.abs(h)}
 function fixPortrait(){if(route()!=='06')return;const st=state(),c=st?.player?.character;if(!c)return;let fig=document.querySelector('.r54-character-portrait');const species=norm(c.species||c.speciesName||'');let src='';if(species.includes('twi lek')||species.includes('twilek'))src='image-library/local/r54/portraits/twilek_female_blue.webp';else src=`image-library/local/tokens/heroes/heroes_${String((hash((c.name||'')+'|'+species)%15)+1).padStart(2,'0')}.png`;
  if(!fig){const h=[...document.querySelectorAll('h1,h2')].find(x=>norm(x.textContent).includes('ficha'))||document.querySelector('.app-content h1,.app-content h2');if(!h)return;fig=document.createElement('figure');fig.className='r54-character-portrait';h.insertAdjacentElement('afterend',fig)}
  const sig=src+'|'+String(c.name||'Personagem');if(fig.dataset.sig===sig)return;fig.dataset.sig=sig;fig.innerHTML=`<img src="${src}" alt="Retrato de ${String(c.name||'Personagem').replace(/["<&]/g,'')}"><figcaption>${String(c.name||'Personagem')}</figcaption>`;
 }
 let bindP=null;function bindings(){if(!bindP)bindP=fetch('image-library/local/r54/bindings.json').then(r=>r.ok?r.json():{}).catch(()=>({}));return bindP}
 async function fixWeapons(){const r=route();if(r!=='08'&&r!=='09')return;const b=await bindings(),idx=new Map();for(const group of ['weapons','equipment'])for(const item of Object.values(b?.[group]||{})){const file=item?.file;if(!file)continue;for(const a of item.aliases||[])idx.set(norm(a),'image-library/local/r54/'+file)}if(!idx.size)return;
  const names=[...document.querySelectorAll('strong,h3,h4,.item-name,.equipment-name')];for(const n of names){const key=norm(n.textContent),src=idx.get(key);if(!src)continue;const host=n.closest('article,li,.card,.panel,.equipment-item,.list-item,div');if(!host||host.querySelector('.r54-item-visual'))continue;const f=document.createElement('figure');f.className='r54-item-visual';f.innerHTML=`<img src="${src}" alt="${String(n.textContent||'Arma').replace(/["<&]/g,'')}">`;host.insertBefore(f,host.firstChild)}
 }
 function tick(){style();fixStartMap();fixPortrait();fixWeapons()}
 let q=false;function schedule(){if(q)return;q=true;requestAnimationFrame(()=>{q=false;tick()})}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
 new MutationObserver(schedule).observe(document.documentElement,{childList:true,subtree:true});addEventListener('popstate',schedule);addEventListener('braseiro:r21-state-changed',schedule);document.addEventListener('click',()=>setTimeout(schedule,0),true);
})();
'''
p0js.write_text(js, 'utf-8')
sh = shell.read_text('utf-8')
tag = '<script src="r54-device-p0.js"></script>'
if tag not in sh:
    if '</body>' not in sh: raise SystemExit('shell body end missing')
    sh = sh.replace('</body>', tag+'\n</body>', 1)
shell.write_text(sh, 'utf-8')

# hard gates
checks = {
 'file fetch enabled': 'setAllowFileAccessFromFileURLs(true)' in main.read_text('utf-8'),
 'startup nonblocking': 'R54_DEVICE_P0_START' in runtime.read_text('utf-8') and 'Promise.resolve(game.submitPlayerIntent(intentText)).catch(()=>{})' in runtime.read_text('utf-8'),
 'device layer mounted': tag in shell.read_text('utf-8'),
 'approved map ref': '9752360iy2h61.png' in p0js.read_text('utf-8'),
 'portrait fallback': 'local/tokens/heroes/heroes_' in p0js.read_text('utf-8'),
 'weapon binding': "bindings.json" in p0js.read_text('utf-8'),
}
if not all(checks.values()): raise SystemExit(checks)
print('R54_DEVICE_P0_PATCH_PASS', checks)
