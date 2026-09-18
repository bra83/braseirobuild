#!/usr/bin/env python3
from pathlib import Path
import sys
if len(sys.argv)!=2: raise SystemExit("usage: fix_device_p5.py <root>")
root=Path(sys.argv[1]); vtt=root/"app/src/main/assets/vtt"
g=vtt.parent.parent/"java/com/braseiro/starwarsedge/GeminiBridge.java"
shell=vtt/"r46-shell.html"; p5=vtt/"r54-device-p5.js"
for p in (g,shell,vtt/"r54-device-p1.js",vtt/"r54-device-p2.js"):
    if not p.is_file(): raise SystemExit("missing "+str(p))
s=g.read_text("utf-8")
old='''            if (secureStored) {
                keyPreferences.edit().remove(KEY_FALLBACK).commit();
            } else {
                if (!keyPreferences.edit().putString(KEY_FALLBACK, clean).commit())
                    return fail("Não foi possível salvar a chave Gemini neste dispositivo.");
            }'''
new='''            // R54_DEVICE_P5_KEY_PERSIST: OEM-safe process-restart fallback.
            if (!keyPreferences.edit().putString(KEY_FALLBACK, clean).commit())
                return fail("Não foi possível salvar a chave Gemini neste dispositivo.");'''
if old in s: s=s.replace(old,new,1)
elif "R54_DEVICE_P5_KEY_PERSIST" not in s: raise SystemExit("key block changed")
g.write_text(s,"utf-8")
js=r"""(()=>{'use strict';if(window.__R54P5)return;window.__R54P5=1;
var css=document.createElement('style');css.textContent='.r54-p1-world-open{top:auto!important;right:auto!important;left:18px!important;bottom:calc(94px + env(safe-area-inset-bottom))!important;min-height:36px!important;padding:0 10px!important;font-size:12px!important}.r53-overlay.r53-dice-overlay{position:fixed!important;inset:0!important;z-index:30000!important;display:flex!important;align-items:flex-end!important;justify-content:center!important;background:rgba(0,0,0,.82)!important}.r53-dice-overlay .r53-modal{display:block!important;visibility:visible!important;opacity:1!important;width:100%!important;max-height:86vh!important;overflow:auto!important;background:#0b1014!important;color:#eee!important;border:1px solid #53636b!important;border-radius:18px 18px 0 0!important;padding:18px 16px calc(20px + env(safe-area-inset-bottom))!important;box-sizing:border-box!important}.r53-dice-grid{display:grid!important;grid-template-columns:1fr 1fr!important;gap:10px!important;margin:14px 0!important}.r53-dice-grid input{width:100%!important;min-height:44px!important;background:#12191e!important;color:#fff!important;border:1px solid #52616a!important}.r53-result{padding:14px 0!important}';document.head.appendChild(css);
function dice(){document.querySelector('.r53-dice-overlay')?.remove();var o=document.createElement('div');o.className='r53-overlay r53-dice-overlay';o.innerHTML='<div class="r53-modal"><div class="r53-modal-head"><div><small>DADOS NARRATIVOS</small><h2>Rolagem livre</h2></div><button class="r53-close">×</button></div><div class="r53-dice-grid"><label>Característica<input data-die="characteristic" type="number" min="0" max="5" value="2"></label><label>Perícia<input data-die="skill" type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-die="difficulty" type="number" min="0" max="5" value="2"></label><label>Desafio<input data-die="challenge" type="number" min="0" max="5" value="0"></label><label>Bônus<input data-die="boost" type="number" min="0" max="5" value="0"></label><label>Contratempo<input data-die="setback" type="number" min="0" max="5" value="0"></label></div><button class="r53-primary r53-roll">ROLAR DADOS</button><div class="r53-result">Aguardando rolagem.</div></div>';document.body.appendChild(o);o.querySelector('.r53-close').onclick=function(){o.remove()};}
document.addEventListener('click',function(e){var b=e.target&&e.target.closest&&e.target.closest('.r53-dice-fab');if(!b)return;e.preventDefault();e.stopImmediatePropagation();dice()},true);
function maps(){document.querySelectorAll('img').forEach(function(i){if(!/mapa local/i.test(i.alt||''))return;var bad=function(){i.style.display='none';if(i.parentElement&&!i.parentElement.querySelector('.r54-p5-map-note')){var n=document.createElement('div');n.className='r54-p5-map-note';n.textContent='Mapa local ainda não disponível para este local.';i.insertAdjacentElement('afterend',n)}};if(i.complete&&i.naturalWidth===0)bad();else i.addEventListener('error',bad,{once:true})})}
maps();new MutationObserver(maps).observe(document.documentElement,{subtree:true,childList:true});
})();"""
p5.write_text(js,"utf-8")
h=shell.read_text("utf-8"); tag='<script src="r54-device-p5.js"></script>'
if tag not in h: h=h.replace("</body>",tag+"\n</body>",1)
shell.write_text(h,"utf-8")
print("R54_DEVICE_P5_PATCH_PASS key=1 dice=1 world_relocated=1 local_map_broken_state=1")
