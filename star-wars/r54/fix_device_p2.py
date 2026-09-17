#!/usr/bin/env python3
from pathlib import Path
import sys
if len(sys.argv)!=2: raise SystemExit('usage: fix_device_p2.py <android-project-root>')
root=Path(sys.argv[1]).resolve(); vtt=root/'app/src/main/assets/vtt'; shell=vtt/'r46-shell.html'; p2=vtt/'r54-device-p2.js'
for p in (shell,vtt/'r46/final-runtime.js',vtt/'image-library/local/r54/bindings.json'):
    if not p.is_file(): raise SystemExit(f'missing {p}')
js=r'''/* R54_DEVICE_P2 — fixes verified from physical-device screenshots. */
(()=>{'use strict';if(window.__R54_DEVICE_P2__)return;window.__R54_DEVICE_P2__=true;
const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
const route=()=>new URLSearchParams(location.search).get('screen')||'00';
function unwrap(v){if(typeof v==='string'){try{v=JSON.parse(v)}catch{}}if(v?.ok===false)throw new Error(v.message||v.error||'Operação recusada.');return v?.ok===true?v.data:v}
function game(){return window.BraseiroGameBridge||null}
function controller(){const b=game();if(!b)return null;if(typeof b.resolveCheck==='function')return b;try{const r=unwrap(b._controllerForExistingCampaign?.());if(r&&typeof r.resolveCheck==='function')return r}catch{}return null}
function repairDice(){document.addEventListener('click',e=>{const btn=e.target?.closest?.('.r53-roll');if(!btn)return;e.preventDefault();e.stopImmediatePropagation();const ov=btn.closest('.r53-dice-overlay'),out=ov?.querySelector('.r53-result');if(!ov||!out)return;const c=controller();if(!c){out.innerHTML='<span class="bad">Motor de dados não conectado à campanha.</span>';return}const n=k=>Math.max(0,Math.min(5,Number(ov.querySelector(`[data-die="${k}"]`)?.value)||0));try{const d=unwrap(c.resolveCheck({characteristic:n('characteristic'),skill:n('skill'),difficulty:n('difficulty'),challenge:n('challenge'),boost:n('boost'),setback:n('setback')}))||{};const s=Number(d.netSuccess||0),a=Number(d.netAdvantage||0),t=Number(d.triumph||0),x=Number(d.despair||0);out.innerHTML=`<strong>Resultado</strong><br><span class="${s>0?'ok':'bad'}">${s>0?'SUCESSO':'FALHA'} · ${s} sucesso(s) líquido(s)</span><br>${a} vantagem(ns) líquida(s) · ${t} triunfo(s) · ${x} desespero(s)`}catch(err){out.innerHTML='<span class="bad">'+String(err?.message||err)+'</span>'}},true)}
let bindP;function bindings(){return bindP||(bindP=fetch('image-library/local/r54/bindings.json').then(r=>{if(!r.ok)throw new Error('bindings '+r.status);return r.json()}))}
function itemHosts(){const sels=['button'];const nodes=[...document.querySelectorAll('strong,h2,h3,h4,.item-name,.equipment-name')];return nodes.map(n=>{let h=n;for(let i=0;i<8&&h;i++,h=h.parentElement){const text=norm(h.textContent);if(text.includes('propor ao mestre')||text.includes('aguardando mestre')||h.matches?.('article,li,.card,.panel,.equipment-item,.list-item'))return [n,h]}return null}).filter(Boolean)}
async function repairImages(){if(!['08','09'].includes(route()))return;const b=await bindings();const idx=new Map();for(const group of Object.keys(b||{}))for(const it of Object.values(b[group]||{})){if(!it?.file)continue;const src='image-library/local/r54/'+it.file;for(const a of (it.aliases||[]))idx.set(norm(a),src);if(it.name)idx.set(norm(it.name),src)}for(const f of document.querySelectorAll('.r53-item-visual.is-fallback'))f.remove();for(const [n,h] of itemHosts()){const name=norm(n.textContent),src=idx.get(name);if(!src||h.querySelector('.r54-item-visual:not(.r53-item-visual),.r54-p2-item-visual'))continue;const f=document.createElement('figure');f.className='r54-item-visual r54-p2-item-visual';f.innerHTML=`<img src="${src}" alt="${String(n.textContent||'Item').replace(/["<&]/g,'')}">`;h.insertBefore(f,h.firstChild)}}
function tick(){repairImages().catch(()=>{})}repairDice();let q=0;const schedule=()=>{clearTimeout(q);q=setTimeout(tick,40)};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true});addEventListener('popstate',schedule);document.addEventListener('click',schedule,true);
})();'''
p2.write_text(js,'utf-8'); s=shell.read_text('utf-8'); tag='<script src="r54-device-p2.js"></script>'
if tag not in s: s=s.replace('</body>',tag+'\n</body>')
shell.write_text(s,'utf-8')
print('R54_DEVICE_P2_PATCH_PASS')
