/* R54 DEVICE P6 — physical-device closure: DADOS, visuals, Gemini status, Mundo. */
(function(){
"use strict";
if(window.__R54_DEVICE_P6__) return;
window.__R54_DEVICE_P6__=true;

var INDEX_URL="image-library/local/r54/p6-visual-index.json";
var indexPromise=null;
var norm=function(v){return String(v||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().replace(/[^a-z0-9]+/g," ").trim();};
var route=function(){return new URLSearchParams(location.search).get("screen")||"00";};
var esc=function(v){return String(v==null?"":v).replace(/[&<>"']/g,function(c){return {"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c];});};
var parse=function(v){if(v&&typeof v==="object")return v;try{return JSON.parse(String(v||""));}catch(e){return null;}};
var idx=function(){return indexPromise||(indexPromise=fetch(INDEX_URL).then(function(r){if(!r.ok)throw new Error("P6 visual index "+r.status);return r.json();}));};

function game(){return window.BraseiroGameBridge||null;}
function state(){
  try{
    var g=game(); if(!g||typeof g.readState!=="function")return null;
    var r=parse(g.readState()); return r&&r.ok?r.data:(r&&r.data?r.data:r);
  }catch(e){return null;}
}
function controller(){
  var g=game(); if(!g)return null;
  if(typeof g.resolveCheck==="function")return g;
  try{
    var r=g._controllerForExistingCampaign&&g._controllerForExistingCampaign();
    r=parse(r)||r; if(r&&r.ok)r=r.data; if(r&&typeof r.resolveCheck==="function")return r;
  }catch(e){}
  return null;
}
function srcPath(p){return p||"";}
function labelsForCard(){
  return Array.prototype.slice.call(document.querySelectorAll("strong,h2,h3,h4,.item-name,.equipment-name,[data-item-name]"));
}
function cardHost(n){
  var h=n;
  for(var i=0;i<9&&h;i++,h=h.parentElement){
    var t=norm(h.textContent);
    if(h.matches&&h.matches("article,li,.card,.panel,.equipment-item,.list-item,[class*=item],[class*=weapon]"))return h;
    if(t.indexOf("propor ao mestre")>=0||t.indexOf("aguardando mestre")>=0)return h;
  }
  return null;
}
function pickVisual(data,name,prefer){
  var key=norm(name), ex=data.exactByName&&data.exactByName[key];
  if(ex&&ex.path)return ex.path;
  var rec=(data.records||[]).find(function(r){
    var cat=norm(r.category), labels=(r.labels||[]);
    if(prefer&&cat&&cat.indexOf(prefer)<0)return false;
    return labels.some(function(l){return l===key;});
  });
  return rec&&rec.path?rec.path:"";
}
function badImage(src){
  src=String(src||"").toLowerCase();
  return /inventory\.png|placeholder|fallback|generic|\/robot(?:[_.\/-]|$)/.test(src);
}
async function repairVehicleImage(){
  if(route()!=="10")return;
  var data=await idx(),root=document.querySelector(".app-content,main")||document.body;
  var title=[...root.querySelectorAll("h1,h2,h3")].find(function(x){return norm(x.textContent).indexOf("resumo da nave")>=0;});
  if(!title)return;
  var host=title.closest("section,article,.card,.panel")||title.parentElement;if(!host)return;
  var name=String(title.textContent||"").replace(/^Resumo da nave\s*[·:–-]?\s*/i,"").trim();
  var src=pickVisual(data,name,"vehicle"),old=host.querySelector(".r54-p6-vehicle-visual");
  if(src){
    if(old){var image=old.querySelector("img");if(image&&image.getAttribute("src")!==src)image.src=src;return;}
    var figure=document.createElement("figure");figure.className="r54-p6-vehicle-visual";
    var image=document.createElement("img");image.src=src;image.alt="Imagem de "+name;image.loading="lazy";image.decoding="async";figure.appendChild(image);host.insertBefore(figure,host.children[1]||null);
  }else if(!old&&!host.querySelector(".r54-p6-vehicle-unavailable")){
    var note=document.createElement("p");note.className="r54-p6-vehicle-unavailable";note.textContent="Nenhum asset visual local específico está integrado para esta nave.";host.insertBefore(note,host.children[1]||null);
  }
}
async function repairParticipantImages(){
  if(route()!=="03")return;
  var data=await idx();
  document.querySelectorAll(".scene-participant-slot").forEach(function(slot){
    if(slot.dataset.persistentNpcToken==="1")return;
    var label=slot.querySelector("strong"),frame=slot.querySelector(".scene-participant-frame");if(!label||!frame)return;
    var name=String(label.textContent||"").trim(),src=pickVisual(data,name,"portrait")||pickVisual(data,name,"creature")||pickVisual(data,name,"vehicle"),old=frame.querySelector(".r54-p6-participant-image");
    if(src){
      if(old){if(old.getAttribute("src")!==src)old.src=src;return;}
      var image=document.createElement("img");image.className="r54-p6-participant-image";image.src=src;image.alt="Imagem de "+name;image.loading="lazy";image.decoding="async";frame.replaceChildren(image);
    }else if(old)old.remove();
  });
}
async function repairItemImages(){
  if(["08","09"].indexOf(route())<0)return;
  var data=await idx();
  document.querySelectorAll("img").forEach(function(im){if(badImage(im.getAttribute("src")||im.src))im.remove();});
  labelsForCard().forEach(function(n){
    var host=cardHost(n); if(!host)return;
    var name=String(n.getAttribute("data-item-name")||n.textContent||"").trim(); if(!name)return;
    var src=pickVisual(data,name,"weapon")||pickVisual(data,name,"equipment");
    if(!src)return;
    var old=host.querySelector(".r54-p6-item-visual img");
    if(old){if(old.getAttribute("src")!==src)old.setAttribute("src",src);return;}
    var f=document.createElement("figure");f.className="r54-p6-item-visual";
    var im=document.createElement("img");im.src=srcPath(src);im.alt=name;im.loading="lazy";im.decoding="async";
    f.appendChild(im);host.insertBefore(f,host.firstChild);
  });
}
async function bindStartLocations(){
  var data=await idx();
  document.querySelectorAll("#sandbox-list button[data-point]").forEach(function(b){
    if(b.querySelector(".r54-p6-location-thumb"))return;
    var pointIndex=Number(b.dataset.point);
    var name=String(b.querySelector("strong")?.textContent||b.textContent||"").trim();
    var rec=(Number.isInteger(pointIndex)&&(data.startLocations||[])[pointIndex])||(data.locations||{})[norm(name)];
    var visual=(data.locations||{})[norm(name)];
    if(!rec||!visual||!['local_map','generated_play_map','galactic_locator_map'].includes(visual.strategy)||!visual.path)return;
    var im=document.createElement("img"); im.className="r54-p6-location-thumb"; im.loading="lazy"; im.decoding="async"; im.src=visual.path; im.alt=(visual.strategy==='local_map'?"Mapa local de ":(visual.strategy==='generated_play_map'?"Esquema local de jogo — ":"Mapa de localização galáctica de "))+name;
    b.insertBefore(im,b.firstChild); b.dataset.p6Visual=rec.strategy||"real";
  });
}
function diceResultText(r){
  r=r||{};var s=Number(r.netSuccess||0),a=Number(r.netAdvantage||0),t=Number(r.triumph||0),d=Number(r.despair||0);
  return (s>0?"SUCESSO":"FALHA")+" · "+s+" sucesso(s) líquido(s) · "+a+" vantagem(ns) líquida(s) · "+t+" triunfo(s) · "+d+" desespero(s)";
}
function diceSpec(type){
  var specs={
    ability:{sides:8,label:"Habilidade",cls:"ability"},proficiency:{sides:12,label:"Proficiência",cls:"proficiency"},
    difficulty:{sides:8,label:"Dificuldade",cls:"difficulty"},challenge:{sides:12,label:"Desafio",cls:"challenge"},
    boost:{sides:6,label:"Bônus",cls:"boost"},setback:{sides:6,label:"Contratempo",cls:"setback"}
  };return specs[type]||{sides:6,label:type||"Dado",cls:"setback"};
}
function diceSeed(i,face){var x=((i+1)*2654435761+((face&&face.index)||0)*2246822519)>>>0;return function(){x=(x*1664525+1013904223)>>>0;return x/4294967296;};}
function faceSymbols(face){face=face||{};var items=[];function add(cls,label,count){count=Math.max(0,Number(count)||0);for(var i=0;i<count;i++)items.push('<span class="sym '+cls+'">'+label+'</span>');}add('success','S',face.success);add('advantage','A',face.advantage);add('failure','F',face.failure);add('threat','T',face.threat);add('triumph','TRI',face.triumph);add('despair','DES',face.despair);return items.length?items.join(''):'<span class="sym blank">—</span>';}
function rollDicePhysicsCss(result,options){
  result=result||{};options=options||{};var faces=Array.isArray(result.faces)?result.faces:[];
  var old=document.getElementById("r54-p22-dice-physics");if(old)old.remove();
  if(!faces.length)return Promise.resolve(result);
  var host=document.createElement("div");host.id="r54-p22-dice-physics";var sh=host.attachShadow({mode:"open"});
  sh.innerHTML='<style>'+ 
   ':host{all:initial}.veil{position:fixed;inset:0;z-index:2147483645;background:rgba(2,6,9,.72);backdrop-filter:blur(2px);overflow:hidden;font-family:system-ui,sans-serif;color:#fff;touch-action:none}'+
   '.title{position:absolute;left:16px;right:16px;top:calc(12px + env(safe-area-inset-top));text-align:center;font-weight:900;letter-spacing:.08em;text-transform:uppercase;text-shadow:0 2px 8px #000;font-size:15px}.stage{position:absolute;inset:58px 10px 132px;overflow:hidden;border-radius:22px;background:radial-gradient(circle at 50% 35%,rgba(44,70,82,.38),rgba(3,8,12,.12) 55%,rgba(0,0,0,.38));border:1px solid rgba(133,190,211,.18);box-shadow:inset 0 -26px 45px rgba(0,0,0,.42)}'+
   '.die{position:absolute;width:58px;height:58px;display:grid;place-items:center;transform-style:preserve-3d;will-change:transform;filter:drop-shadow(0 12px 10px rgba(0,0,0,.58));user-select:none}.die::before{content:"";position:absolute;inset:4px;border:2px solid rgba(255,255,255,.34);border-radius:inherit;box-shadow:inset 8px 8px 16px rgba(255,255,255,.15),inset -10px -12px 18px rgba(0,0,0,.4)}'+
   '.die.d6{border-radius:14px}.die.d8{clip-path:polygon(50% 0,91% 18%,100% 58%,72% 100%,28% 100%,0 58%,9% 18%)}.die.d12{clip-path:polygon(50% 0,79% 9%,97% 34%,97% 66%,79% 91%,50% 100%,21% 91%,3% 66%,3% 34%,21% 9%)}'+
   '.ability{background:linear-gradient(145deg,#2fca82,#0b6947)}.proficiency{background:linear-gradient(145deg,#f2c94c,#9a6911);color:#17120b}.difficulty{background:linear-gradient(145deg,#9564dd,#48217e)}.challenge{background:linear-gradient(145deg,#e65a50,#8e1f1b)}.boost{background:linear-gradient(145deg,#60d8ef,#1a7e9c);color:#071116}.setback{background:linear-gradient(145deg,#5f6871,#15191e)}'+
   '.face{position:relative;z-index:2;display:flex;flex-wrap:wrap;align-items:center;justify-content:center;gap:3px;max-width:48px;backface-visibility:hidden}.sym{display:inline-flex;align-items:center;justify-content:center;min-width:17px;height:17px;padding:0 3px;border-radius:999px;background:rgba(8,12,18,.35);border:1px solid rgba(255,255,255,.28);font-size:8px;font-weight:1000}.sym.success{background:rgba(14,74,50,.7)}.sym.triumph{background:rgba(124,81,6,.82)}.kind{position:absolute;bottom:5px;left:0;right:0;text-align:center;font-size:8px;font-weight:900;opacity:.76;letter-spacing:.04em;text-transform:uppercase}'+
   '.result{position:absolute;left:12px;right:12px;bottom:calc(18px + env(safe-area-inset-bottom));min-height:72px;border:1px solid rgba(213,173,95,.55);border-radius:15px;background:rgba(8,14,18,.95);padding:12px 14px;box-sizing:border-box;text-align:center;opacity:0;transform:translateY(16px);transition:opacity .22s ease,transform .22s ease}.result.show{opacity:1;transform:none}.result strong{display:block;font-size:18px;margin-bottom:5px}.result span{font-size:13px;color:#d4dde1}.result .status{display:block;color:#83dff5;margin-top:5px;font-size:12px}'+
   '</style><div class="veil"><div class="title"></div><div class="stage"></div><div class="result"><strong></strong><span></span><em class="status"></em></div></div>';
  document.documentElement.appendChild(host);var stage=sh.querySelector('.stage'),title=sh.querySelector('.title'),resultBox=sh.querySelector('.result');title.textContent=options.title||'ROLAGEM';
  var dice=faces.map(function(face,i){var spec=diceSpec(face.type),rnd=diceSeed(i,face),el=document.createElement('div');el.className='die d'+spec.sides+' '+spec.cls;el.innerHTML='<span class="face">'+faceSymbols(face.face)+'</span><small class="kind">'+spec.label+'</small>';stage.appendChild(el);return {el:el,spec:spec,x:18+rnd()*Math.max(24,stage.clientWidth-92),y:-20-rnd()*110,vx:(rnd()-.5)*15,vy:3+rnd()*5,ang:rnd()*360,omega:(rnd()-.5)*28,rnd:rnd};});
  return new Promise(function(resolve){var start=performance.now(),last=start,settled=false;
    function frame(now){var dt=Math.min(34,Math.max(8,now-last))/16.667;last=now;var w=Math.max(120,stage.clientWidth),h=Math.max(180,stage.clientHeight),elapsed=now-start;
      dice.forEach(function(d){if(elapsed<980){d.vy+=.42*dt;d.x+=d.vx*dt;d.y+=d.vy*dt;d.ang+=d.omega*dt;d.vx*=.994;d.omega*=.992;if(d.x<2){d.x=2;d.vx=Math.abs(d.vx)*.76;d.omega*=-.82}if(d.x>w-60){d.x=w-60;d.vx=-Math.abs(d.vx)*.76;d.omega*=-.82}if(d.y>h-60){d.y=h-60;d.vy=-Math.abs(d.vy)*.58;d.vx*=.86;d.omega*=.76}if(d.y<0){d.y=0;d.vy=Math.abs(d.vy)*.72}}else{d.vx*=.80;d.vy*=.80;d.omega*=.72;d.x+=d.vx*dt;d.y=Math.min(h-60,Math.max(0,d.y+d.vy*dt));d.ang+=d.omega*dt}
        var tiltX=14+Math.sin((d.ang+iSafe(dice,d))*0.017)*9,tiltY=-12+Math.cos((d.ang-iSafe(dice,d))*0.013)*10;d.el.style.transform='translate3d('+d.x.toFixed(1)+'px,'+d.y.toFixed(1)+'px,0) perspective(520px) rotateX('+tiltX.toFixed(1)+'deg) rotateY('+tiltY.toFixed(1)+'deg) rotateZ('+d.ang.toFixed(1)+'deg)';});
      // colisões simples em tela; depois do amortecimento final os dados congelam sem jitter.
      if(elapsed<940){for(var a=0;a<dice.length;a++)for(var b=a+1;b<dice.length;b++){var A=dice[a],B=dice[b],dx=B.x-A.x,dy=B.y-A.y,dist=Math.hypot(dx,dy)||1;if(dist<52){var nx=dx/dist,ny=dy/dist,over=(52-dist)/2;A.x-=nx*over;A.y-=ny*over;B.x+=nx*over;B.y+=ny*over;var av=A.vx*nx+A.vy*ny,bv=B.vx*nx+B.vy*ny,p=(av-bv)*.72;A.vx-=p*nx;A.vy-=p*ny;B.vx+=p*nx;B.vy+=p*ny}}}
      if(elapsed<1480){requestAnimationFrame(frame);return}if(settled)return;settled=true;dice.forEach(function(d){d.vx=d.vy=d.omega=0;});var text=diceResultText(result);resultBox.querySelector('strong').textContent=Number(result.netSuccess||0)>0?'SUCESSO':'FALHA';resultBox.querySelector('span').textContent=text;resultBox.classList.add('show');var status=resultBox.querySelector('.status');status.textContent=options.status||'Resultado registrado.';setTimeout(function(){resolve({host:host,result:result,status:status,close:function(){host.remove();}})},340);}
    function iSafe(arr,d){var x=arr.indexOf(d);return x<0?0:x;}
    requestAnimationFrame(frame);
  });
}
function rollDicePhysics(result,options){
  result=result||{};options=options||{};var nativeDice=window.BraseiroDice;
  if(nativeDice&&typeof nativeDice.roll==='function'&&Array.isArray(result.faces)&&result.faces.length){var requestId='sw-'+Date.now()+'-'+Math.random().toString(36).slice(2);return new Promise(function(resolve){var done=false;function finish(ev){var d=ev&&ev.detail||{};if(d.requestId!==requestId)return;removeEventListener('braseiroStarWarsDiceEvent',finish);done=true;resolve({host:null,result:result,status:null,close:function(){}})}addEventListener('braseiroStarWarsDiceEvent',finish);var raw;try{raw=nativeDice.roll(requestId,JSON.stringify(result.faces),String(options.title||'ROLAGEM'),diceResultText(result),String(options.status||'Resultado registrado.'))}catch(e){raw='{"ok":false}'}var pr=parse(raw)||{};if(!pr.ok){removeEventListener('braseiroStarWarsDiceEvent',finish);done=true;return resolve(rollDicePhysicsCss(result,options))}setTimeout(function(){if(!done){removeEventListener('braseiroStarWarsDiceEvent',finish);resolve({host:null,result:result,status:null,close:function(){}})}},18000)})}
  return rollDicePhysicsCss(result,options);
}
function dicePanel(){
  document.documentElement.dataset.p6DicePhase="enter";document.documentElement.dataset.p6DiceError="";
  var existing=document.getElementById("r54-p6-dice");if(existing)return;
  document.querySelectorAll(".r53-dice-overlay").forEach(function(x){x.remove();});
  var host=document.createElement("div");host.id="r54-p6-dice";var sh=host.attachShadow({mode:"open"});
  sh.innerHTML='<style>'+':host{all:initial}.back{position:fixed;inset:0;z-index:2147483000;background:rgba(0,0,0,.86);display:flex;align-items:flex-end;justify-content:center;padding-top:env(safe-area-inset-top)}.sheet{box-sizing:border-box;width:100%;max-width:680px;max-height:88svh;overflow-y:auto;overscroll-behavior:contain;-webkit-overflow-scrolling:touch;background:#0b1014;color:#f5f0e6;border:1px solid #53636b;border-radius:18px 18px 0 0;padding:18px 16px calc(22px + env(safe-area-inset-bottom));font:14px/1.4 system-ui,sans-serif}.head{display:flex;justify-content:space-between;align-items:center;gap:10px;position:sticky;top:-18px;background:#0b1014;padding:10px 0;z-index:2}.head h2{margin:0;font-size:22px}.close{width:44px;height:44px;border-radius:50%;border:1px solid #6d7d85;background:#141b20;color:#fff;font-size:24px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:14px 0}.grid label{display:grid;gap:5px;color:#d9af5f;font-weight:800}.grid input{box-sizing:border-box;width:100%;min-height:46px;border:1px solid #52616a;border-radius:8px;background:#12191e;color:#fff;padding:8px;font:inherit}.roll{width:100%;min-height:48px;border:1px solid #b58942;border-radius:9px;background:#1b2024;color:#fff;font-weight:900}.result{min-height:70px;padding:16px 0 6px}.ok{color:#7ee7a3}.bad{color:#ff8c8c}</style><div class="back"><section class="sheet" role="dialog" aria-modal="true" aria-label="Dados narrativos"><div class="head"><div><small>DADOS NARRATIVOS</small><h2>Rolagem livre</h2></div><button class="close" aria-label="Fechar">×</button></div><div class="grid"><label>Característica<input data-die="characteristic" type="number" min="0" max="5" value="2"></label><label>Perícia<input data-die="skill" type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-die="difficulty" type="number" min="0" max="5" value="2"></label><label>Desafio<input data-die="challenge" type="number" min="0" max="5" value="0"></label><label>Bônus<input data-die="boost" type="number" min="0" max="5" value="0"></label><label>Contratempo<input data-die="setback" type="number" min="0" max="5" value="0"></label></div><button class="roll">ROLAR DADOS</button><div class="result">Aguardando rolagem.</div></section></div>';
  document.documentElement.appendChild(host);document.documentElement.dataset.p6DicePhase="mounted";
  var close=function(){host.remove();};sh.querySelector(".close").onclick=close;sh.querySelector(".back").addEventListener("click",function(e){if(e.target===e.currentTarget)close();});
  sh.querySelector(".roll").onclick=async function(){var out=sh.querySelector(".result"),c=controller();if(!c){out.innerHTML='<span class="bad">Motor de dados não conectado à campanha.</span>';return;}var n=function(k){return Math.max(0,Math.min(5,Number(sh.querySelector('[data-die="'+k+'"]').value)||0));};try{var r=c.resolveCheck({characteristic:n("characteristic"),skill:n("skill"),difficulty:n("difficulty"),challenge:n("challenge"),boost:n("boost"),setback:n("setback")});r=parse(r)||r;if(r&&r.ok)r=r.data;r=r||{};out.textContent='Rolando na tela…';var visual=await rollDicePhysics(r,{title:'Rolagem livre',status:'Resultado da rolagem livre.'});out.innerHTML='<strong>Resultado</strong><br><span class="'+(Number(r.netSuccess||0)>0?'ok':'bad')+'">'+esc(diceResultText(r))+'</span>';setTimeout(function(){visual.close();},1100);}catch(err){out.innerHTML='<span class="bad">'+esc(err&&err.message||err)+'</span>';}};
}
window.__R54P6DicePanel=dicePanel;window.__R54DicePhysics={roll:rollDicePhysics,summary:diceResultText};
function repairDiceButton(){
  document.querySelectorAll(".r53-dice-fab").forEach(function(old){old.remove();});var host=document.querySelector(".app-content")||document.querySelector("main")||document.body;var b=document.querySelector(".r54-p6-dice-fab");
  if(!b){b=document.createElement("button");b.type="button";b.textContent="DADOS";b.className="r54-p6-dice-fab";b.setAttribute("aria-label","Abrir dados narrativos");}
  if(b.parentElement!==host){var world=host.querySelector(".r54-p6-world-open");if(world&&world.nextSibling)host.insertBefore(b,world.nextSibling);else if(world)world.insertAdjacentElement("afterend",b);else host.prepend(b);}
  b.onclick=function(e){document.documentElement.dataset.p6DiceEvent="target-click";if(e){e.preventDefault();e.stopPropagation();}dicePanel();};
}
function geminiConfigured(){
  try{var r=parse(window.BraseiroGemini&&window.BraseiroGemini.getStatus&&window.BraseiroGemini.getStatus())||{};return !!r.configured;}catch(e){return false;}
}
function repairGeminiStatus(){
  if(route()!=="29")return;
  var cards=Array.prototype.slice.call(document.querySelectorAll("section,article,[class*=card],div"));
  var card=cards.find(function(x){return norm(x.textContent).indexOf("chave da api gemini")>=0&&x.querySelector("input");});
  if(!card)return;
  var out=card.querySelector(".r54-p6-gemini-status");
  if(!out){out=document.createElement("div");out.className="r54-p6-gemini-status";card.appendChild(out);}
  out.textContent=geminiConfigured()?"Chave: configurada e persistida no aparelho.":"Chave: não configurada.";
  out.dataset.configured=geminiConfigured()?"true":"false";
}
function livingWorldOverview(){
  var st=state()||{},w=st.world||{},d=w.livingDepth||{},time=d.time||{},sceneLoc=st.scene&&st.scene.locationId,ls=sceneLoc&&d.locations&&d.locations[sceneLoc];
  var all=Object.values(d.npcs||{}), alive=all.filter(function(n){return n.lifeState!=="dead"}).length, dead=all.filter(function(n){return n.lifeState==="dead"}).length, rich=all.filter(function(n){return n.wealthTier==="rico"||n.wealthTier==="próspero"}).length;
  var npcs=Object.values(w.npcs||{}).filter(function(n){return !sceneLoc||n.locationId===sceneLoc;}).slice(0,8).map(function(n){var x=(d.npcs||{})[n.id]||{};return '<div class="r54-live-npc"><strong>'+esc(n.name)+'</strong><small>'+esc(x.currentActivity||n.goal||'rotina')+' · '+esc(x.wealthTier||'estável')+' · '+esc(x.lifeState==='dead'?'morto':'ativo')+'</small></div>';}).join('');
  return '<div class="r54-live-overview"><div><strong>Dia '+esc(time.day||1)+' · '+esc(time.period||'')+'</strong><small>Clima: '+esc(ls&&ls.weather||'estável')+' · Pressão de mercado: '+esc(ls&&Math.round(ls.marketPressure)||50)+'</small><small>NPCs: '+alive+' ativos · '+dead+' mortos · '+rich+' prósperos/ricos</small></div>'+ (npcs?'<div class="r54-live-npcs">'+npcs+'</div>':'') +'</div>';
}
function worldRows(data){
  var st=state()||{},w=st.world||{},locs=Array.isArray(w.locations)?w.locations:Object.values(w.locations||{});
  if(!locs.length){
    return '<div class="r54-p6-world-row"><strong>Mundo ainda sem locais carregados.</strong></div>';
  }
  return locs.map(function(x){
    var name=String(x.name||x.title||x.label||x.id||"Local"),rec=(data.locations||{})[norm(name)],hasMap=rec&&['local_map','generated_play_map','galactic_locator_map'].includes(rec.strategy)&&rec.path;
    var img=hasMap?'<img src="'+esc(rec.path)+'" alt="'+(rec.strategy==='local_map'?'Mapa local de ':(rec.strategy==='generated_play_map'?'Esquema local de jogo — ':'Mapa de localização galáctica de '))+esc(name)+'">':'';
    var mapNote=hasMap?(rec.strategy==='local_map'?'Mapa local dedicado integrado.':(rec.strategy==='generated_play_map'?'Esquema local de jogo gerado; não é mapa real/cartografia oficial.':'Mapa real de localização galáctica integrado.')):'Mapa não disponível no projeto.';
    return '<article class="r54-p6-world-row"'+(hasMap?' data-map-src="'+esc(rec.path)+'" data-map-name="'+esc(name)+'" data-map-kind="'+esc(rec.strategy)+'" tabindex="0" role="button" aria-label="Abrir mapa de '+esc(name)+'"':'')+'>'+img+'<div><strong>'+esc(name)+'</strong><small>'+esc(x.summary||x.description||x.type||x.kind||"")+'</small><small class="r54-p6-map-note">'+mapNote+(hasMap?' · Toque para ampliar.':'')+'</small></div></article>';
  }).join("");
}
function openMapViewer(src,name,kind){
  document.querySelector('.r54-p6-map-viewer')?.remove();
  if(!src)return;
  var v=document.createElement('div');v.className='r54-p6-map-viewer';
  var label=kind==='local_map'?'Mapa local dedicado':(kind==='generated_play_map'?'Esquema local de jogo':'Localização galáctica');
  v.innerHTML='<section role="dialog" aria-modal="true" aria-label="'+esc(label+' — '+name)+'"><header><div><small>'+esc(label)+'</small><strong>'+esc(name)+'</strong></div><button type="button" aria-label="Fechar mapa">×</button></header><div class="r54-p6-map-canvas"><img src="'+esc(src)+'" alt="'+esc(label+' de '+name)+'"></div></section>';
  document.body.appendChild(v);var close=function(){v.remove();};v.querySelector('button').onclick=close;v.onclick=function(e){if(e.target===v)close();};
}
async function openWorld(){
  document.querySelector(".r54-p6-world-modal")?.remove();
  var data=await idx(),m=document.createElement("div");m.className="r54-p6-world-modal";
  m.innerHTML='<section><header><h2>MUNDO VIVO</h2><button type="button" data-close>×</button></header>'+livingWorldOverview()+'<div class="r54-p6-world-list">'+worldRows(data)+'</div></section>';
  document.body.appendChild(m);m.querySelector("[data-close]").onclick=function(){m.remove();};m.addEventListener("click",function(e){if(e.target===m)m.remove();});m.querySelectorAll('[data-map-src]').forEach(function(row){var open=function(){openMapViewer(row.dataset.mapSrc,row.dataset.mapName,row.dataset.mapKind);};row.onclick=open;row.onkeydown=function(e){if(e.key==='Enter'||e.key===' '){e.preventDefault();open();}};});
}
function mountWorldEntry(){
  document.querySelectorAll(".r54-p1-world-open").forEach(function(b){b.style.setProperty("display","none","important");b.setAttribute("aria-hidden","true");});
  if(!["00","12","16"].includes(route()))return;
  if(document.querySelector(".r54-p6-world-open"))return;
  var host=document.querySelector(".app-content,main")||document.body;
  var b=document.createElement("button");b.type="button";b.className="r54-p6-world-open";b.textContent="MUNDO VIVO";b.onclick=openWorld;
  var h=host.querySelector("h1,h2"); if(h)h.insertAdjacentElement("afterend",b); else host.prepend(b);
}
function addStyle(){
  if(document.getElementById("r54-p6-style"))return;
  var s=document.createElement("style");s.id="r54-p6-style";s.textContent=
  '.r54-p1-world-open,.r53-dice-fab{display:none!important}.r54-p6-dice-fab{display:block!important;position:static!important;margin:10px 18px!important;min-height:44px!important;padding:0 14px!important;border:1px solid #d49d42!important;border-radius:9px!important;background:#1b2024!important;color:#f0e4cf!important;font-weight:900!important;pointer-events:auto!important;visibility:visible!important;opacity:1!important}.r54-p6-world-open{display:block!important;position:static!important;margin:10px 18px!important;min-height:44px!important;padding:0 14px!important;border:1px solid #45c9f5!important;border-radius:9px!important;background:#081820!important;color:#78ddff!important;font-weight:900!important}'+
  '#sandbox-list button[data-point]{display:flex!important;align-items:center!important;gap:9px!important}.r54-p6-location-thumb{width:58px!important;height:42px!important;flex:0 0 58px!important;object-fit:cover!important;border-radius:6px!important;border:1px solid rgba(255,255,255,.24)!important}'+
  '.r54-p6-item-visual{margin:0 0 10px!important;width:100%!important;max-height:180px!important;overflow:hidden!important;border-radius:8px!important;background:#050709!important}.r54-p6-item-visual img{display:block!important;width:100%!important;height:150px!important;object-fit:contain!important;background:#050709!important}'+
  '.r54-p6-gemini-status{margin-top:10px!important;padding:9px 10px!important;border:1px solid #42545e!important;border-radius:8px!important;color:#8ee9b0!important;background:#0e1714!important;font-weight:800!important}'+
  '.r54-p6-world-modal{position:fixed!important;inset:0!important;z-index:2147482000!important;background:rgba(0,0,0,.86)!important;display:flex!important;align-items:flex-end!important}.r54-p6-world-modal>section{box-sizing:border-box!important;width:100%!important;max-height:88svh!important;overflow:auto!important;background:#0b1014!important;color:#eee!important;border-radius:18px 18px 0 0!important;padding:14px 14px calc(18px + env(safe-area-inset-bottom))!important}.r54-p6-world-modal header{position:sticky!important;top:-14px!important;background:#0b1014!important;display:flex!important;justify-content:space-between!important;align-items:center!important;z-index:2!important}.r54-p6-world-modal header button{width:44px!important;height:44px!important;border-radius:50%!important;background:#151c21!important;color:white!important;border:1px solid #566!important;font-size:22px!important}.r54-p6-world-list{display:grid!important;gap:9px!important}.r54-p6-world-row{display:grid!important;grid-template-columns:92px 1fr!important;gap:10px!important;align-items:center!important;border:1px solid #34434b!important;border-radius:10px!important;background:#11171b!important;padding:8px!important}.r54-p6-world-row img{width:92px!important;height:68px!important;object-fit:cover!important;border-radius:7px!important;background:#000!important}.r54-p6-world-row strong,.r54-p6-world-row small{display:block!important}.r54-p6-world-row small{margin-top:4px!important;color:#aebbc0!important}';
  s.textContent+='.r54-p6-vehicle-visual{margin:0 0 12px!important;border-radius:8px!important;overflow:hidden!important;background:#050709!important;border:1px solid rgba(84,197,234,.28)!important}.r54-p6-vehicle-visual img{display:block!important;width:100%!important;height:180px!important;object-fit:contain!important;background:#050709!important}.r54-p6-vehicle-unavailable,.r54-p6-map-note{color:#aebbc0!important;font-size:12px!important}.scene-participant-frame{overflow:hidden!important}.r54-p6-participant-image,.r54-p23-npc-token-image{display:block!important;width:118%!important;height:118%!important;object-fit:cover!important;object-position:50% 20%!important;border-radius:inherit!important;max-width:none!important}.scene-participant-slot.npc-token-action strong{text-decoration:underline;text-decoration-color:rgba(84,197,234,.55);text-underline-offset:2px}.scene-participant-slot.has-persistent-token .scene-participant-frame{box-shadow:0 0 0 1px rgba(84,197,234,.55),0 0 14px rgba(84,197,234,.18)!important}';
  s.textContent+='.r54-p6-map-viewer{position:fixed!important;inset:0!important;z-index:2147483100!important;background:#000!important;display:flex!important;align-items:center!important;justify-content:center!important}.r54-p6-map-viewer>section{width:100%!important;height:100%!important;display:grid!important;grid-template-rows:auto 1fr!important;background:#050709!important;color:#fff!important}.r54-p6-map-viewer header{display:flex!important;justify-content:space-between!important;align-items:center!important;padding:calc(10px + env(safe-area-inset-top)) 12px 10px!important;background:#0b1014!important}.r54-p6-map-viewer header small,.r54-p6-map-viewer header strong{display:block!important}.r54-p6-map-viewer header small{color:#9fb0b8!important}.r54-p6-map-viewer header button{width:44px!important;height:44px!important;border-radius:50%!important;border:1px solid #697279!important;background:#151c21!important;color:#fff!important;font-size:24px!important}.r54-p6-map-canvas{min-height:0!important;overflow:auto!important;display:flex!important;align-items:center!important;justify-content:center!important;padding:8px!important}.r54-p6-map-canvas img{display:block!important;max-width:none!important;width:auto!important;height:auto!important;min-width:100%!important;object-fit:contain!important}.r54-p6-world-row[data-map-src]{cursor:pointer!important}.r54-p6-world-row[data-map-src]:focus{outline:2px solid #45c9f5!important;outline-offset:2px!important}';document.head.appendChild(s);
}
var lastImageCount=-1;
function markBroken(){
  var images=document.images;if(images.length===lastImageCount)return;lastImageCount=images.length;
  Array.prototype.forEach.call(images,function(im){
    if(badImage(im.getAttribute("src")||im.src))im.dataset.p6Invalid="placeholder";
    if(im.complete&&im.naturalWidth===0)im.dataset.p6Invalid="broken";
    if(!im.dataset.p6Err){im.dataset.p6Err="1";im.addEventListener("error",function(){im.dataset.p6Invalid="broken";});}
  });
}
function tick(){
  addStyle();repairDiceButton();mountWorldEntry();repairGeminiStatus();markBroken();
  bindStartLocations().catch(function(){});
  repairVehicleImage().catch(function(){});
  repairParticipantImages().catch(function(){});
}
if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('device-p6',tick);
else {if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",tick,{once:true});else tick();addEventListener("popstate",tick);}
})();
