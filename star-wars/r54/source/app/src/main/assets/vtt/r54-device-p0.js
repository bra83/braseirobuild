/* R54_DEVICE_P0 — actual-device repair: map, portrait, weapon visuals. */
(()=>{'use strict';
 if(window.__R54_DEVICE_P0__)return;window.__R54_DEVICE_P0__=true;
 const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function state(){try{const r=window.BraseiroGameBridge?.readState?.();return r?.ok?r.data:null}catch{return null}}
 function style(){if(document.getElementById('r54-device-p0-style'))return;const e=document.createElement('style');e.id='r54-device-p0-style';e.textContent=`
 /* startup map: never allow the 82 points to become giant touch circles */
 #sandbox-map.final-start-map{position:relative!important;width:100%!important;max-width:100%!important;aspect-ratio:5042/3263!important;height:auto!important;max-height:min(66vh,620px)!important;overflow:hidden!important;background:#02060a!important;border-radius:10px!important}
 #sandbox-map.final-start-map>img{display:block!important;width:100%!important;height:100%!important;max-width:100%!important;max-height:100%!important;object-fit:contain!important}
 #sandbox-map button[data-point]{position:absolute!important;width:11px!important;height:11px!important;min-width:11px!important;min-height:11px!important;max-width:11px!important;max-height:11px!important;padding:0!important;margin:0!important;border:2px solid #d8e7ef!important;border-radius:50%!important;background:#35b7df!important;box-shadow:0 0 0 2px rgba(0,0,0,.55),0 0 7px rgba(53,183,223,.8)!important;font-size:0!important;line-height:0!important;overflow:visible!important;transform:translate(-50%,-50%)!important;z-index:4!important}
 #sandbox-map button[data-point][aria-pressed="true"],#sandbox-map button[data-point].selected{width:15px!important;height:15px!important;min-width:15px!important;min-height:15px!important;max-width:15px!important;max-height:15px!important;background:#f0b44f!important;border-color:#fff3ce!important;z-index:6!important}
 .r54-character-portrait{margin:10px 0 14px!important;width:100%!important;max-width:330px!important;border:1px solid rgba(212,157,66,.55)!important;border-radius:10px!important;overflow:hidden!important;background:#080b0d!important}
 .r54-character-portrait img{display:block!important;width:100%!important;aspect-ratio:4/3!important;object-fit:cover!important;background:#090c0f!important}
 .r54-character-portrait figcaption{padding:7px 10px!important;color:#d7b26b!important;font-weight:800!important}
 .r54-item-visual{margin:0 0 8px!important;border-radius:8px!important;overflow:hidden!important;background:#080b0d!important;border:1px solid rgba(84,197,234,.28)!important}
 .r54-item-visual img{display:block!important;width:100%!important;height:118px!important;object-fit:contain!important;background:#050708!important}
 `;document.head.appendChild(e)}
 function fixStartMap(){const map=document.getElementById('sandbox-map');if(!map)return;const img=map.querySelector(':scope>img');if(img&&!(img.getAttribute('src')||'').includes('9752360iy2h61')){img.src='assets/maps/galaxy_map_mobile.webp';img.removeAttribute('onerror');img.alt='Mapa galáctico aprovado com nomes de sistemas e regiões';}map.querySelectorAll('button[data-point]').forEach(b=>{b.textContent='';b.setAttribute('aria-label',b.getAttribute('title')||'Ponto inicial');});}
 function hash(s){let h=0;for(const c of String(s||''))h=((h<<5)-h+c.charCodeAt(0))|0;return Math.abs(h)}
 function fixPortrait(){if(!['06','07'].includes(route())&&!document.querySelector('.r54-character-portrait'))return;const st=state(),c=st?.player?.character;if(!c)return;let fig=document.querySelector('.r54-character-portrait');const species=norm(c.species||c.speciesName||'');let src=localStorage.getItem('braseiro.starwars.character.photo.v1')||'';if(!src&&(species.includes('twi lek')||species.includes('twilek')))src='image-library/local/r54/portraits/twilek_female_blue.webp';else if(!src)src=`image-library/local/tokens/heroes/heroes_${String((hash((c.name||'')+'|'+species)%15)+1).padStart(2,'0')}.png`;
  if(!fig){const h=[...document.querySelectorAll('h1,h2')].find(x=>norm(x.textContent).includes('ficha'))||document.querySelector('.app-content h1,.app-content h2');if(!h)return;fig=document.createElement('figure');fig.className='r54-character-portrait';h.insertAdjacentElement('afterend',fig)}
  const sig=src+'|'+String(c.name||'Personagem');if(fig.dataset.sig===sig)return;fig.dataset.sig=sig;fig.innerHTML=`<img src="${src}" alt="Retrato de ${String(c.name||'Personagem').replace(/["<&]/g,'')}"><figcaption>${String(c.name||'Personagem')}</figcaption>`;
 }
 let bindP=null;function bindings(){if(!bindP)bindP=fetch('image-library/local/r54/bindings.json').then(r=>r.ok?r.json():{}).catch(()=>({}));return bindP}
 async function fixWeapons(){const r=route();if(r!=='08'&&r!=='09')return;const b=await bindings(),idx=new Map();for(const group of ['weapons','equipment'])for(const item of Object.values(b?.[group]||{})){const src=item?.path||(item?.file?'image-library/local/r54/'+item.file:'');if(!src)continue;for(const a of item.aliases||[])idx.set(norm(a),src)}if(!idx.size)return;
  const names=[...document.querySelectorAll('strong,h3,h4,.item-name,.equipment-name')];for(const n of names){const key=norm(n.textContent),src=idx.get(key);if(!src)continue;const host=n.closest('article,li,.card,.panel,.equipment-item,.list-item,div');if(!host||host.querySelector('.r54-item-visual'))continue;const f=document.createElement('figure');f.className='r54-item-visual';f.innerHTML=`<img src="${src}" alt="${String(n.textContent||'Arma').replace(/["<&]/g,'')}">`;host.insertBefore(f,host.firstChild)}
 }
 function tick(){style();fixStartMap();fixPortrait()}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('device-p0',tick);
 else { if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',tick,{once:true});else tick(); addEventListener('popstate',tick); }
})();
