(function(){
'use strict';
const bridge=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)}catch{return null}};
const state=()=>bridge('readState')?.data;
const el=(tag,text,cls)=>{const e=document.createElement(tag);if(text)e.textContent=text;if(cls)e.className=cls;return e};
const css=document.createElement('style');css.textContent=`
.sw-map-viewport{position:relative;overflow:hidden;touch-action:none;background:#080a0b;border:1px solid #596269;min-height:220px;border-radius:6px}
.sw-map-plane{position:relative;transform-origin:0 0;width:100%;will-change:transform}.sw-map-plane>img{display:block;width:100%;user-select:none;pointer-events:none}
.sw-map-pin{position:absolute;transform:translate(-50%,-50%);width:20px;height:20px;border:2px solid white;background:#be3838;border-radius:50%;pointer-events:none;box-shadow:0 0 0 3px #0008}
.sw-map-tools{display:flex;gap:6px;flex-wrap:wrap;margin:8px 0}.sw-map-tools button,.sw-map-panel button{min-height:44px;background:#22272b;color:#e7e1d7;border:1px solid #596269;border-radius:5px;padding:8px 12px}
.sw-map-panel{padding:12px;border:1px solid #596269;background:#121619;border-radius:6px;margin:12px 0;color:#e7e1d7}.sw-map-panel p{font-size:13px;color:#c3c8cb}.sw-map-locations{display:flex;gap:8px;flex-wrap:wrap}.sw-map-locations button[aria-current=location]{border:2px solid #cfac68;background:#3c3322}
#sandbox-custom-start{box-sizing:border-box;width:100%;min-height:44px;background:#171a1c;color:#eee;border:1px solid #596269;border-radius:5px;padding:8px;font-size:16px}
`;document.head.append(css);
function interactive(image,options={}){
 const viewport=el('div',null,'sw-map-viewport'),plane=el('div',null,'sw-map-plane');image.replaceWith(viewport);plane.append(image);viewport.append(plane);
 const pin=el('span',null,'sw-map-pin');pin.hidden=true;plane.append(pin);
 const tools=el('div',null,'sw-map-tools');viewport.before(tools);
 let zoom=1,tx=0,ty=0,moved=false,down=null;const pointers=new Map();let pinch=null;
 const draw=()=>{const w=viewport.clientWidth,h=plane.offsetHeight;tx=Math.max(w-w*zoom,Math.min(0,tx));ty=Math.max(h-h*zoom,Math.min(0,ty));plane.style.transform=`translate(${tx}px,${ty}px) scale(${zoom})`;viewport.style.height=h+'px';viewport.dataset.zoom=String(zoom)};
 const scale=z=>{zoom=Math.max(1,Math.min(6,z));draw()};
 for(const [name,fn] of [['−',()=>scale(zoom/1.35)],['+',()=>scale(zoom*1.35)],['Centralizar',()=>{zoom=1;tx=ty=0;draw()}]]){const b=el('button',name);b.type='button';b.setAttribute('aria-label',name==='+'?'Ampliar mapa':name==='−'?'Reduzir mapa':name);b.onclick=fn;tools.append(b)}
 viewport.onpointerdown=e=>{if(e.target.closest('button'))return;pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});viewport.setPointerCapture(e.pointerId);down={x:e.clientX,y:e.clientY,tx,ty};moved=false;if(pointers.size===2){const a=[...pointers.values()];pinch={distance:Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y),zoom};moved=true}};
 viewport.onpointermove=e=>{if(!pointers.has(e.pointerId))return;pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pointers.size===2&&pinch){const a=[...pointers.values()];scale(pinch.zoom*Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y)/pinch.distance);moved=true;return}if(!down)return;const dx=e.clientX-down.x,dy=e.clientY-down.y;if(Math.hypot(dx,dy)>5)moved=true;if(moved){tx=down.tx+dx;ty=down.ty+dy;draw()}};
 viewport.onpointerup=e=>{const click=!moved&&pointers.size===1&&down;pointers.delete(e.pointerId);if(click&&options.onPoint){const r=plane.getBoundingClientRect();options.onPoint({x:Math.max(0,Math.min(100,(e.clientX-r.left)/r.width*100)),y:Math.max(0,Math.min(100,(e.clientY-r.top)/r.height*100))})}down=null;pinch=null};
 viewport.onpointercancel=()=>{pointers.clear();down=null;pinch=null};
 image.addEventListener('load',draw);new ResizeObserver(draw).observe(viewport);requestAnimationFrame(draw);
 return {mark(point){pin.hidden=!point;if(point){pin.style.left=point.x+'%';pin.style.top=point.y+'%';pin.setAttribute('aria-label','Posição da região escolhida')}},viewport,plane,tools};
}
function setup(host,initial,onSelect){
 let chosen={...initial};const field=el('label');field.append(el('span','Nome do planeta ou local inicial'));const input=el('input');input.id='sandbox-custom-start';input.maxLength=120;input.value=chosen.name;field.append(input);host.querySelector('#sandbox-list').before(field);
 input.oninput=()=>{chosen={...chosen,name:input.value};onSelect(chosen)};
 const map=host.querySelector('#sandbox-map'),image=map.querySelector('img');
 // Keep familiar suggestions, and permit every map coordinate instead of only eight pins.
 const mapUI=interactive(image,{onPoint:p=>{chosen={...p,name:input.value.trim()||'Ponto escolhido no mapa'};mapUI.mark(chosen);onSelect(chosen)}});
 mapUI.mark(chosen);mapUI.tools.append(el('span','Toque para escolher; amplie e arraste para explorar.'));
 map.querySelectorAll('[data-point]').forEach(button=>mapUI.plane.append(button));
 host.querySelectorAll('[data-point]').forEach(button=>button.addEventListener('click',()=>{chosen={name:input.value,x:parseFloat(map.querySelector(`[data-point="${button.dataset.point}"]`).style.left),y:parseFloat(map.querySelector(`[data-point="${button.dataset.point}"]`).style.top)};mapUI.mark(chosen)}));
}
function routeTo(route){const u=new URL(location.href);u.searchParams.set('screen',route);history.pushState({},'',u);window.dispatchEvent(new PopStateEvent('popstate'))}
let panel=null,last='',galaxyUI=null;
function render(){
 const route=new URLSearchParams(location.search).get('screen')||'00';
 if(route==='00'&&!document.querySelector('.sw-open-map')){const host=document.querySelector('.scene-concept-shell');if(host){const b=el('button','MAPA','sw-open-map');b.type='button';b.style.cssText='min-height:44px;margin:8px 0';b.onclick=()=>routeTo('16');host.prepend(b)}}
 if(!['16','17'].includes(route)){panel=null;last='';galaxyUI=null;return}
 const main=document.querySelector('main');if(!main)return;
 if(panel?.isConnected&&panel.dataset.route!==route){panel.remove();panel=null}if(!panel?.isConnected){panel=el('section',null,'sw-map-panel');panel.id='sw-live-map';panel.dataset.route=route;panel.append(el('strong','Posição da aventura'));const status=el('p');status.id='sw-map-position';panel.append(status);const slots=el('div',null,'sw-map-locations');panel.append(slots);main.prepend(panel);last='';galaxyUI=null;
  if(route==='16'){const img=el('img');img.src='assets/maps/galaxy_map.jpg';img.alt='Mapa galáctico interativo';panel.append(img);galaxyUI=interactive(img);panel.append(el('p','Marcador galáctico: região inicial escolhida. Os deslocamentos locais aparecem abaixo.'))}
  panel.append(el('p','Locais conhecidos — selecione um destino para propor a viagem ao Mestre.'));
 }
 const s=state();if(!s)return;const world=s.world||{},current=s.scene?.locationId,locations=Object.values(world.locations||{}).filter(l=>l.knownToPlayer||l.id===current);const signature=JSON.stringify([route,current,locations.map(l=>[l.id,l.name]),world.sandbox?.startMapPoint]);if(signature===last)return;last=signature;
 panel.querySelector('#sw-map-position').textContent='Você está em: '+(world.locations?.[current]?.name||s.scene?.title||'Local ainda não definido');panel.dataset.locationId=current||'';galaxyUI?.mark(world.sandbox?.startMapPoint||null);
 const slots=panel.querySelector('.sw-map-locations');slots.replaceChildren();
 const oldGraph=panel.querySelector('.sw-map-graph');if(oldGraph)oldGraph.remove();
 const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.classList.add('sw-map-graph');svg.setAttribute('viewBox','0 0 360 220');svg.setAttribute('role','img');svg.setAttribute('aria-label','Rede dos locais conhecidos; posição atual destacada');svg.style.cssText='width:100%;background:#080a0b;border-radius:6px';
 const coords=new Map(locations.map((l,i)=>[l.id,{x:50+(i%3)*130,y:40+Math.floor(i/3)*65}]));svg.setAttribute('viewBox',`0 0 360 ${Math.max(120,Math.ceil(locations.length/3)*65+20)}`);
 const node=(tag,attrs)=>{const n=document.createElementNS(svg.namespaceURI,tag);for(const [k,v] of Object.entries(attrs))n.setAttribute(k,String(v));return n};
 for(const r of world.routes||[]){const a=coords.get(r.fromLocationId),b=coords.get(r.toLocationId);if(a&&b)svg.append(node('line',{x1:a.x,y1:a.y,x2:b.x,y2:b.y,stroke:'#596269','stroke-width':2}))}
 for(const l of locations){const p=coords.get(l.id),circle=node('circle',{cx:p.x,cy:p.y,r:l.id===current?12:7,fill:l.id===current?'#cfac68':'#85929a',stroke:l.id===current?'#fff':'none','stroke-width':2});circle.dataset.locationId=l.id;if(l.id===current)circle.dataset.current='true';svg.append(circle);const t=node('text',{x:p.x,y:p.y+26,fill:'#e7e1d7','font-size':10,'text-anchor':'middle'});t.textContent=l.name.length>20?l.name.slice(0,18)+'…':l.name;svg.append(t)}
 slots.before(svg);
 for(const l of locations){const b=el('button',(l.id===current?'● ':'')+l.name);b.type='button';b.dataset.locationId=l.id;if(l.id===current)b.setAttribute('aria-current','location');b.onclick=()=>{if(l.id===current)return;const prompt=`Quero viajar para ${l.name}. Avalie a rota, o tempo e as consequências antes de confirmar o deslocamento.`;routeTo('00');setTimeout(()=>{const t=document.querySelector('.scene-composer textarea');if(t){Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(t,prompt);t.dispatchEvent(new Event('input',{bubbles:true}));t.focus()}},100)};slots.append(b)}
}
window.BraseiroMapUI={setup,refresh:render};window.addEventListener('popstate',()=>setTimeout(render,0));setInterval(render,500);
})();
