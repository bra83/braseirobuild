#!/usr/bin/env python3
from __future__ import annotations
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: final_map_travel_integration.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
vtt = root / 'app/src/main/assets/vtt'
html = vtt / 'r46-shell.html'
js_path = vtt / 'r54-final-map-travel.js'
if not html.is_file():
    raise SystemExit(f'missing shell: {html}')

js = r'''/* R54_FINAL_MAP_TRAVEL_V1 — map is a live campaign surface, not a detached gallery. */
(()=>{'use strict';
const GUARD='__R54_FINAL_MAP_TRAVEL_V1__';if(window[GUARD])return;window[GUARD]=true;
const STATE_EVENT='braseiro:r21-state-changed';
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const route=()=>new URLSearchParams(location.search).get('screen')||'00';
const bridge=()=>window.BraseiroGameBridge&&typeof window.BraseiroGameBridge.readState==='function'?window.BraseiroGameBridge:null;
function state(){try{const b=bridge(),r=b?.readState?.();return r?.ok?r.data:null}catch{return null}}
function arr(v){return Array.isArray(v)?v:[]}
function txt(o,...keys){if(typeof o==='string')return o.trim();for(const k of keys){const v=o?.[k];if(typeof v==='string'&&v.trim())return v.trim()}return ''}
function locationById(s,id){return arr(s?.world?.locations).find(x=>String(x?.id??x?.locationId??'')===String(id??''))||null}
function locName(loc,id){return txt(loc,'name','title','label','shortName')||String(id||'Local atual')}
function pointOf(s,loc){
 const candidates=[loc?.mapPoint,loc?.coordinates,loc?.position,Number.isFinite(loc?.x)&&Number.isFinite(loc?.y)?{x:loc.x,y:loc.y}:null];
 const start=s?.world?.sandbox?.startMapPoint,startId=s?.world?.sandbox?.startLocationId,current=s?.scene?.locationId;
 if(String(startId??'')===String(current??''))candidates.push(start);
 for(const p of candidates){if(p&&Number.isFinite(Number(p.x))&&Number.isFinite(Number(p.y))){const x=Number(p.x),y=Number(p.y);if(x>=0&&x<=100&&y>=0&&y<=100)return{x,y}}}
 return null;
}
function activeObjectives(s){
 const pools=[s?.objectives,s?.missions,s?.world?.objectives,s?.world?.missions,s?.records?.mission,s?.records?.missions,s?.group?.missions,s?.scene?.objectives,s?.master?.objectives];
 const out=[],seen=new Set();
 for(const pool of pools)for(const raw of arr(pool)){
  const status=txt(raw,'status','state').toLocaleLowerCase('pt-BR');if(/conclu|complete|done|resolvid|closed|falh|failed/.test(status))continue;
  const label=txt(raw,'title','name','objective','text','summary','description');if(!label)continue;const key=label.toLocaleLowerCase('pt-BR');if(seen.has(key))continue;seen.add(key);out.push({label,status});
 }
 return out.slice(0,6);
}
function connected(s){
 const from=s?.scene?.locationId,locations=arr(s?.world?.locations),routes=arr(s?.world?.routes);if(!from)return[];
 return routes.filter(r=>String(r?.fromLocationId??'')===String(from)||String(r?.toLocationId??'')===String(from)).map(r=>{
  const to=String(r?.fromLocationId??'')===String(from)?r?.toLocationId:r?.fromLocationId,loc=locations.find(x=>String(x?.id??x?.locationId??'')===String(to));
  const meta=[txt(r,'distance','duration','travelTime'),txt(r,'danger','risk'),txt(r,'type','routeType')].filter(Boolean).join(' · ');
  return{to:String(to??''),name:locName(loc,to),meta};
 }).filter(x=>x.to);
}
function injectStyle(){if(document.getElementById('r54-final-map-travel-style'))return;const s=document.createElement('style');s.id='r54-final-map-travel-style';s.textContent=`
#r54-map-live-panel{display:grid;gap:9px;border:1px solid rgba(84,197,234,.42);background:linear-gradient(160deg,rgba(9,18,23,.98),rgba(16,22,25,.98));border-radius:10px;padding:11px;box-shadow:inset 0 0 0 1px rgba(212,157,66,.08),0 8px 22px rgba(0,0,0,.26);color:#d9e2e5}
#r54-map-live-panel .r54-live-head{display:flex;align-items:center;justify-content:space-between;gap:8px;border-bottom:1px solid rgba(84,197,234,.24);padding-bottom:7px}
#r54-map-live-panel .r54-live-head strong{font-size:.82rem;letter-spacing:.08em;color:#58d0f2}#r54-map-live-panel .r54-live-head span{font-size:.68rem;color:#d49d42}
#r54-map-live-panel .r54-current{display:grid;grid-template-columns:1fr auto;gap:8px;align-items:center;padding:9px;border:1px solid rgba(212,157,66,.28);border-radius:8px;background:rgba(39,45,48,.72)}
#r54-map-live-panel .r54-current small,#r54-map-live-panel .r54-section small{display:block;color:#8fa3ab;font-size:.66rem;line-height:1.25}#r54-map-live-panel .r54-current b{font-size:.95rem;color:#f0e7d6}
#r54-map-live-panel .r54-coord{font-size:.64rem;color:#58d0f2;border:1px solid rgba(84,197,234,.35);border-radius:999px;padding:5px 7px;white-space:nowrap}
#r54-map-live-panel .r54-section{display:grid;gap:6px}#r54-map-live-panel .r54-section>strong{font-size:.72rem;letter-spacing:.07em;color:#d6b06b;text-transform:uppercase}
#r54-map-live-panel .r54-route{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px;align-items:center;padding:8px;border:1px solid rgba(130,147,154,.26);border-radius:8px;background:rgba(24,31,35,.86)}
#r54-map-live-panel .r54-route b{display:block;font-size:.78rem}#r54-map-live-panel .r54-route button{min-height:38px!important;padding:7px 10px;border:1px solid #54c5ea;border-radius:7px;background:linear-gradient(145deg,#2d9fc2,#216f89)!important;color:#061015!important;font-size:.67rem;font-weight:900;letter-spacing:.03em}
#r54-map-live-panel .r54-objective{padding:7px 8px;border-left:3px solid #d49d42;background:rgba(212,157,66,.075);font-size:.73rem;line-height:1.3}
#r54-map-live-panel .r54-message{font-size:.68rem;min-height:1em;color:#8fd8ed}#r54-map-live-panel .r54-empty{font-size:.72rem;color:#8fa3ab}
.r54-live-map-anchor{position:absolute;z-index:8;transform:translate(-50%,-50%);width:18px;height:18px;border-radius:50%;border:3px solid #e4f7ff;background:#23bce9;box-shadow:0 0 0 4px rgba(35,188,233,.22),0 0 16px rgba(35,188,233,.9);pointer-events:none}.r54-live-map-anchor:after{content:'VOCÊ';position:absolute;left:50%;top:20px;transform:translateX(-50%);padding:2px 5px;border-radius:4px;background:rgba(3,10,14,.9);color:#8de5ff;font:800 9px/1 sans-serif;letter-spacing:.05em;white-space:nowrap}
`;document.head.appendChild(s)}
function mountMarker(root,s,loc){root.querySelectorAll('.r54-live-map-anchor').forEach(x=>x.remove());if(route()!=='16')return;const p=pointOf(s,loc);if(!p)return;const img=[...root.querySelectorAll('img')].find(i=>i.offsetWidth>120&&i.offsetHeight>80);if(!img)return;const host=img.parentElement;if(!host)return;const pos=getComputedStyle(host).position;if(pos==='static')host.style.position='relative';const m=document.createElement('span');m.className='r54-live-map-anchor';m.style.left=p.x+'%';m.style.top=p.y+'%';m.title=`Localização atual · ${p.x.toFixed(0)}, ${p.y.toFixed(0)}`;host.appendChild(m)}
async function travel(to,name,btn,msg){const b=bridge();if(!b||typeof b.travelToLocation!=='function'){msg.textContent='Viagem indisponível no Motor Barbara.';return}btn.disabled=true;msg.textContent='Confirmando rota…';try{const r=await Promise.resolve(b.travelToLocation(to,{source:'map_ui'}));if(!r?.ok){msg.textContent=r?.message||'O motor recusou a viagem.';return}window.dispatchEvent(new Event(STATE_EVENT));msg.textContent=`Chegada confirmada: ${name}. Barbara atualizando a cena…`;if(typeof b.submitPlayerIntent==='function'){Promise.resolve(b.submitPlayerIntent(`[VIAGEM_BARBARA] A viagem para ${name} foi confirmada pelo motor. Narre a chegada, consequências imediatas e mudanças visíveis do mundo sem decidir pelo personagem.`)).catch(()=>{})}setTimeout(render,120)}catch(e){msg.textContent='Falha ao viajar: '+String(e?.message||e)}finally{btn.disabled=false}}
function render(){
 const r=route();if(r!=='16'&&r!=='17'){document.getElementById('r54-map-live-panel')?.remove();document.querySelectorAll('.r54-live-map-anchor').forEach(x=>x.remove());return}
 injectStyle();const b=bridge(),s=state(),root=document.querySelector('.app-content>div,.app-content main,main');if(!root||!b||!s)return;
 const currentId=s?.scene?.locationId||s?.world?.sandbox?.startLocationId||'',loc=locationById(s,currentId),name=locName(loc,currentId),p=pointOf(s,loc),routes=connected(s),objs=activeObjectives(s);
 let panel=document.getElementById('r54-map-live-panel');if(!panel){panel=document.createElement('section');panel.id='r54-map-live-panel';panel.dataset.r54MapTravel='1';root.appendChild(panel)}
 panel.innerHTML=`<div class="r54-live-head"><strong>MAPA · ESTADO VIVO</strong><span>${r==='16'?'GALÁXIA':'MISSÃO'}</span></div><div class="r54-current"><div><small>LOCALIZAÇÃO ATUAL</small><b>${esc(name)}</b></div>${p?`<span class="r54-coord">${p.x.toFixed(0)} · ${p.y.toFixed(0)}</span>`:''}</div><div class="r54-section"><strong>OBJETIVOS PÚBLICOS</strong>${objs.length?objs.map(o=>`<div class="r54-objective">${esc(o.label)}${o.status?`<small>${esc(o.status)}</small>`:''}</div>`).join(''):'<div class="r54-empty">Nenhum objetivo público registrado.</div>'}</div><div class="r54-section"><strong>ROTAS A PARTIR DAQUI</strong>${routes.length?routes.map((x,i)=>`<div class="r54-route"><div><b>${esc(x.name)}</b>${x.meta?`<small>${esc(x.meta)}</small>`:''}</div><button type="button" class="r54-route-travel" data-to="${esc(x.to)}" data-index="${i}">VIAJAR</button></div>`).join(''):'<div class="r54-empty">Nenhuma rota conectada registrada.</div>'}</div><div class="r54-message" role="status"></div>`;
 const msg=panel.querySelector('.r54-message');panel.querySelectorAll('.r54-route-travel').forEach(btn=>btn.addEventListener('click',()=>{const x=routes[Number(btn.dataset.index)];if(x)travel(x.to,x.name,btn,msg)}));mountMarker(root,s,loc);
}
let queued=false;const schedule=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;render()})};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();window.addEventListener('popstate',schedule);window.addEventListener(STATE_EVENT,schedule);document.addEventListener('click',()=>setTimeout(schedule,0),true);new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true});
})();
'''
js_path.write_text(js, encoding='utf-8')

text = html.read_text(encoding='utf-8')
tag = '<script src="r54-final-map-travel.js"></script>'
if tag not in text:
    if '</body>' in text:
        text = text.replace('</body>', tag + '\n</body>', 1)
    else:
        text += '\n' + tag + '\n'
    html.write_text(text, encoding='utf-8')

final_html = html.read_text(encoding='utf-8')
final_js = js_path.read_text(encoding='utf-8')
checks = {
    'script mounted': tag in final_html,
    'current location': 'LOCALIZAÇÃO ATUAL' in final_js,
    'objectives': 'OBJETIVOS PÚBLICOS' in final_js,
    'routes': 'ROTAS A PARTIR DAQUI' in final_js,
    'travel bridge': 'b.travelToLocation(to' in final_js,
    'barbara travel narration': '[VIAGEM_BARBARA]' in final_js,
    'map marker': 'r54-live-map-anchor' in final_js,
}
bad=[k for k,v in checks.items() if not v]
if bad:
    raise SystemExit('R54 map/travel integration gates failed: '+', '.join(bad))
print('R54_FINAL_MAP_TRAVEL_PASS', checks)
