(() => {
'use strict';
const A='../assets/ose-user-pack-v1/source/TALES_VTT_ASSET_PACK/assets_individuais/';
const q=new URLSearchParams(window.__P1_CAPTURE_QUERY||location.search);
let screen=q.get('screen')||'session';
let stateName=q.get('state')||'active';
let viewState=null;
let geminiState={textSceneConfigured:false,ttsAudioConfigured:false};
let mediaTarget=null;
let localLog=['Personagem criado. Entre em FICHA > Inventário para comprar/equipar.','Aventura iniciada.'];
const el=id=>document.getElementById(id);
const main=el('screen');
const hasNative=()=>!!(window.BraseiroBridge&&typeof window.BraseiroBridge.postMessage==='function');
const postNative=(type,payload={})=>{if(!hasNative())return false;window.BraseiroBridge.postMessage(JSON.stringify({version:1,type,payload}));return true};
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const img=(src,alt='',cls='')=>`<img ${cls?`class="${cls}"`:''} src="${esc(src)}" alt="${esc(alt)}" draggable="false">`;
const asset=(p)=>A+p;
const fallbackNarration={active:'A alvorada cobre as colinas. O caminho à frente desaparece entre pedras antigas e mata fechada. O que você faz?',encounter:'Um ruído distante sugere movimento além da próxima elevação. O grupo percebe apenas sinais visíveis e ainda não declarou como reagirá.',combat:'Silhuetas armadas ocupam a passagem à frente. O Mestre aguarda a declaração do grupo antes de resolver qualquer consequência.',recovery:'O local volta a ficar silencioso. O grupo pode observar o que está visível antes de declarar a próxima ação.'};
const fallbackDecisions={active:[['LER O TERRENO','Examinamos o terreno e sinais visíveis antes de escolher o caminho.'],['SEGUIR COM CAUTELA','Seguimos pelo caminho visível com cautela.'],['BUSCAR VANTAGEM','Procuramos um ponto visível que permita observar melhor os arredores.']],encounter:[['OBSERVAR','Observamos com atenção sem avançar ainda.'],['ESCUTAR','Ficamos em silêncio e escutamos antes de agir.'],['RECUAR','Recuamos à última posição conhecida.']],combat:[['OBSERVAR A AMEAÇA','Mantemos a formação e observamos a ameaça.'],['BUSCAR COBERTURA','Procuramos uma posição defensável ao nosso alcance.'],['TENTAR FALAR','Tentamos comunicação antes de outra ação hostil.']],recovery:[['VERIFICAR O GRUPO','Verificamos a condição do grupo.'],['EXAMINAR A ÁREA','Examinamos a área visível.'],['REORGANIZAR','Reorganizamos a companhia antes de seguir.']]};

window.BraseiroCreation=Object.freeze({start:draftId=>postNative('CharacterCreationStart',{draftId}),commit:payload=>postNative('CharacterCreationCommit',payload)});
window.BraseiroReceive=(raw)=>{
 try{
  const m=typeof raw==='string'?JSON.parse(raw):raw,p=m.payload||{};
  if(m.type==='ViewState'){viewState=p;geminiState=p.gemini||geminiState;render();}
  else if(m.type==='SessionUpdate'){if(p.viewState){viewState=p.viewState;geminiState=viewState.gemini||geminiState;}localLog.push(p.feedback||'Estado confirmado e salvo.');render();showToast('PLAYER_REACTION processado pelo motor.');}
  else if(m.type==='CharacterCreationDraft'||m.type==='CharacterCreationResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok===false?`Criação rejeitada: ${p.code||''}`:'Criação nativa atualizada.');}
  else if(m.type==='UiCommandResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok===false?`Ação rejeitada: ${p.status||''}`:(p.status==='ACTIVE'?'Criação concluída. A aventura está ativa.':`Equipamento: ${p.status||'OK'}`));}
  else if(m.type==='GMHelpResponse')showToast(p.answer||'GM_HELP sem resposta.');
  else if(m.type==='TtsState')showToast(ttsMessage(p.state));
  else if(m.type==='GeminiSettingsState'){geminiState=p;updateSettingsStatus();}
  else if(m.type==='GeminiCredentialTestResult')showToast(p.ok?`Conexão ${p.slot}: OK`:`Conexão ${p.slot}: falhou`);
  else if(m.type==='SceneImageResult'){postNative('ViewState',{request:'after-scene-image'});showToast('Ilustração vinculada à cena sem delta mecânico.');}
  else if(m.type==='PresentationImageSaved'){postNative('ViewState',{request:'after-media-save'});showToast('Imagem salva como apresentação; regras inalteradas.');}
  else if(m.type==='BridgeError')showToast(`Erro: ${p.message||'bridge'}`);
 }catch(_e){showToast('Resposta nativa inválida.');}
};

function ttsMessage(s){return s==='MISSING_KEY'?'Configure a chave Gemini TTS.':s==='STARTING'?'Gemini TTS: preparando narração visível.':s==='REPLAYING'?'Repetindo narração.':s==='STOPPED'?'Áudio interrompido.':'TTS indisponível.'}
function showToast(msg){const t=el('toast');t.textContent=msg;t.classList.remove('hidden');clearTimeout(showToast.timer);showToast.timer=setTimeout(()=>t.classList.add('hidden'),2200)}
function resource(prefix,fallback='0'){const xs=viewState?.resources||[];const hit=xs.find(x=>String(x).toLowerCase().startsWith(prefix+':'));return hit?String(hit).split(':').slice(1).join(':'):fallback}
function phase(){return viewState?.session?.phase||'ACTIVE'}
function profileLabel(){const p=viewState?.campaign?.profile||'OSE_CLASSIC_FANTASY';return p.includes('ADVANCED')?'ADVANCED FANTASY':'CLASSIC FANTASY'}
function updateHeader(){
 const titles={session:'SESSÃO',map:'MAPA',sheet:'FICHA',company:'COMPANHIA'};
 el('headerTitle').textContent=`${titles[screen]} · BRASEIRO OSE`;
 const tm=viewState?.time||{};el('timeStatus').textContent=`DIA ${(tm.days??0)+1} · T${tm.turns??0}`;
 el('phaseStatus').textContent=phase();el('torchStatus').textContent=resource('torch','0');el('rationStatus').textContent=resource('rations','0');
 document.querySelectorAll('.navbtn').forEach(b=>b.classList.toggle('active',b.dataset.screen===screen));
}
function go(s){screen=s;q.set('screen',s);try{history.replaceState(null,'',`${location.pathname}?${q}`)}catch(_e){}render()}
function render(){updateHeader();main.innerHTML=screen==='session'?renderSession():screen==='map'?renderMap():screen==='sheet'?renderSheet():renderCompany();wireCurrentScreen();}

function renderSession(){
 if(hasNative() && !viewState?.selectedCharacter && phase()==='ACTIVE' && !(viewState?.creation?.drafts||[]).length) return renderCreationStart();
 if(hasNative() && !viewState?.selectedCharacter && (viewState?.creation?.drafts||[]).length) return renderCreationDraft(viewState.creation.drafts[0]);
 if(phase()==='CHARACTER_LOADOUT' && hasNative()) return `<div class="kicker">Criação ainda não concluída</div><div class="panel"><h2>${esc(viewState?.selectedCharacter?.name||'Aventureiro')}</h2><p>O personagem existe e está em CHARACTER_LOADOUT. Use a FICHA para inspecionar o inventário antes de iniciar a aventura pelo fluxo autoritativo.</p><button class="primary" data-go="sheet">Ir para Ficha / Inventário</button></div>`;
 const narration=viewState?.session?.visibleNarration||fallbackNarration[stateName]||fallbackNarration.active;
 const sceneUrl=viewState?.presentation?.sceneImageUrl;
 const decisions=(viewState?.sceneDecisions||[]).map(x=>[x.label,x.reaction]);
 const ds=decisions.length===3?decisions:(fallbackDecisions[stateName]||fallbackDecisions.active);
 return `<div class="kicker">Sessão ativa</div>
  <div class="panel"><div class="row"><div><span class="pill">${profileLabel()}</span><h2 style="margin-top:7px">A aventura começa</h2></div></div>
  ${sceneUrl?img(sceneUrl,'Ilustração da cena atual','scene-image'):''}<p id="visibleNarration">${esc(narration||'Barbara aguarda sua primeira reação.')}</p>
  <div class="session-tools"><button class="small" data-tts="play">▶ OUVIR</button><button class="small" data-tts="stop">■ PARAR</button><button class="small" data-tts="replay">↻ REPETIR</button><button class="small" data-scene-image>▧ ILUSTRAR CENA</button></div></div>
  <div class="panel"><h3>O que você faz?</h3><div class="suggestions">${ds.map(([l,r])=>`<button class="suggestion" data-suggest="${esc(r)}">${esc(l)}</button>`).join('')}</div><textarea id="reactionInput" placeholder="Ex.: examino as pegadas e sigo devagar..."></textarea><button class="primary" data-react>Enviar PLAYER_REACTION</button></div>
  <div class="panel"><h3>Registro</h3><div class="log">${esc(localLog.slice(-8).join('\n')||'Sem eventos.')}</div></div>`;
}
function renderCreationStart(){return `<div class="kicker">Primeiro aventureiro</div><div class="panel"><h2>Criação nativa</h2><p>Os atributos serão rolados pelo RULES_DICE autoritativo e persistidos; a interface não fornece resultados de dados.</p><span class="pill">${profileLabel()}</span><button class="primary" style="margin-top:10px" data-start-draft>Rolar 6 atributos · 3d6 cada</button></div>`}
function renderCreationDraft(d){
 const a=d.attributes||{};const isAdv=String(d.profile||'').includes('ADVANCED');
 const classes=isAdv?['ACROBAT','ASSASSIN','BARBARIAN','BARD','CLERIC','DRUID','FIGHTER','ILLUSIONIST','KNIGHT','MAGIC_USER','PALADIN','RANGER','THIEF']:['DWARF','CLERIC','ELF','FIGHTER','HALFLING','THIEF','MAGIC_USER'];
 const races=['HUMAN','DROW','DUERGAR','DWARF','ELF','GNOME','HALF_ELF','HALFLING','HALF_ORC','SVIRFNEBLIN'];
 return `<div class="kicker">Primeiro aventureiro</div><div class="panel"><h3>Criar personagem <span class="pill">RULES_DICE</span></h3><div class="attrs">${Object.entries({FOR:a.str,INT:a.int,SAB:a.wis,DES:a.dex,CON:a.con,CAR:a.cha}).map(([k,v])=>`<div class="attr"><b>${v??'—'}</b><span>${k}</span></div>`).join('')}</div>
 ${isAdv?`<label class="field">Método</label><select id="creationMethod"><option value="ADVANCED_BASIC">BASIC</option><option value="ADVANCED_METHOD">ADVANCED</option></select><label class="field">Raça (método Advanced)</label><select id="creationRace"><option value="">Nenhuma / conforme método</option>${races.map(x=>`<option>${x}</option>`).join('')}</select>`:''}
 <label class="field">Classe</label><select id="creationClass">${classes.map(x=>`<option>${x}</option>`).join('')}</select><label class="field">Nome</label><input id="creationName" placeholder="Nome do aventureiro"><button class="primary" style="margin-top:9px" data-commit-draft="${esc(d.draftId)}">Criar pelas regras OSE</button></div>`;
}

const terrain=['planicie','colinas','floresta','planicie','montanha','desconhecido','floresta','colinas','planicie','agua','desconhecido','montanha','planicie','pantano','floresta','colinas','planicie','desconhecido','agua','planicie','colinas','floresta','desconhecido','montanha','planicie','floresta','pantano','desconhecido','colinas','planicie'];
const geom=Object.freeze({hexW:60,hexH:69.282,xStep:60,yStep:51.962,oddOffset:30,originX:42,originY:40});
function renderMap(){
 let cells='',i=0;for(let r=0;r<5;r++)for(let c=0;c<6;c++){const x=geom.originX+c*geom.xStep+(r%2?geom.oddOffset:0),y=geom.originY+r*geom.yStep,t=terrain[i++%terrain.length];cells+=`<div class="hex-cell" data-q="${c}" data-r="${r}" data-terrain="${t}" data-inspection-only="true" aria-label="hex ${t}" style="left:${x}px;top:${y}px;background-image:url('${asset('hexes_compactos/'+t+'.png')}')"></div>`}
 const pos=viewState?.position||{q:3,r:2},pq=Number.isFinite(pos.q)?pos.q:3,pr=Number.isFinite(pos.r)?pos.r:2;
 const px=geom.originX+pq*geom.xStep+(pr%2?geom.oddOffset:0)+16,py=geom.originY+pr*geom.yStep+17;
 return `<section class="map-shell" data-screen="MAP" data-geometry-version="1" data-direct-movement="forbidden"><div class="map-head"><div><div class="kicker">Mapa · inspeção/exploração</div><div class="map-title">Ermos do Norte</div></div><div class="camera-tools"><button id="zoomOut" data-camera-only="true">−</button><button id="zoomIn" data-camera-only="true">+</button></div></div><div id="mapViewport" class="map-viewport" aria-label="Mapa com pan e zoom de câmera; sem movimento direto"><div id="mapGrid" class="map-grid" data-geometry-version="1">${cells}<div class="canonical-ring" style="left:${px-9}px;top:${py-9}px"></div>${img(asset('tokens_compactos/guerreiro.png'),'Posição canônica da companhia','party-pin').replace('<img ','<img style="left:'+px+'px;top:'+py+'px" ')}<div class="map-label" style="left:${px+30}px;top:${py+2}px">POSIÇÃO CANÔNICA · Q${pq},R${pr}</div></div></div><div class="map-inspect panel"><img class="inspect-hex" src="${asset('hexes_destaque/hex_colinas_arborizadas.png')}" alt="Colinas arborizadas"><div><div class="inspect-title">Colinas do Outeiro</div><div class="inspect-copy">Conhecido pelo grupo. Seleção de mapa altera apenas o foco de inspeção; não move a companhia, não gasta tempo e não resolve encontros.</div><div class="geometry-note">GEOMETRY V1 · HEXES IGUAIS · SEM STRETCH · SEM NUDGE POR CÉLULA · CÂMERA ≠ POSIÇÃO</div></div></div></section>`
}

function hpText(c){if(!c)return'—';const d=c.hpDenominator||1;const cur=c.hpCurrentNumerator??0,max=c.hpMaxNumerator??0;return d===1?`${cur}/${max}`:`${cur}/${d} / ${max}/${d}`}
function attrPairs(a={}){return [['FOR',a.str],['INT',a.int],['SAB',a.wis],['DES',a.dex],['CON',a.con],['CAR',a.cha]]}
function renderSheet(){
 const c=viewState?.selectedCharacter;if(hasNative()&&!c)return `<div class="panel"><h2>Nenhum personagem ainda</h2><button class="primary" data-go="session">Abrir criação</button></div>`;
 const demo=c||{characterId:'demo-character',name:'Aventureiro',raceId:'HUMAN',classIds:['FIGHTER'],levelByClass:{FIGHTER:1},hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1,armorClassDescending:9,attributes:{str:11,int:9,wis:8,dex:10,con:6,cha:9}};
 const portrait=viewState?.presentation?.characterPortraits?.[demo.characterId];
 const portraitHtml=portrait?img(portrait,demo.name):'<span>⚔</span>';
 const inv=viewState?.inventory||(!hasNative()?[{itemId:'demo-mace',catalogId:'ADV_EQUIP_WEAPON_PURCHASE_09',quantity:1,equipped:false}]:[]);
 return `<div class="hero panel"><div class="portrait">${portraitHtml}${hasNative()?'<button data-photo-character="'+esc(demo.characterId)+'">FOTO</button>':''}</div><div><div class="kicker">Personagem</div><h2>${esc(demo.name||'Aventureiro')}</h2><p><b>${esc(demo.raceId||'HUMAN')}</b> · ${esc((demo.classIds||[]).join(' / ')||'—')} · ${profileLabel()}</p><div class="vitals"><div class="vital"><span>HP</span><b>${hpText(demo)}</b></div><div class="vital"><span>CA</span><b>${demo.armorClassDescending??'—'}</b></div><div class="vital"><span>NÍVEL</span><b>${Object.values(demo.levelByClass||{})[0]??1}</b></div></div></div></div><div class="panel"><div class="attrs">${attrPairs(demo.attributes).map(([k,v])=>`<div class="attr"><b>${v??'—'}</b><span>${k}</span></div>`).join('')}</div></div><div class="panel"><h3>Inventário operacional</h3><div class="inventory-grid">${inv.length?inv.map(renderInventoryItem).join(''):'<p class="note">Inventário vazio.</p>'}</div></div>${hasNative()?renderNativeLoadout():renderDemoShop()}`;
}
function loadoutState(){return viewState?.loadout&&viewState.loadout.characterId?viewState.loadout:null}
function catalogEntry(id){return loadoutState()?.catalog?.find(x=>x.catalogId===id)||null}
function catalogLabel(id){const e=catalogEntry(id);if(e?.name)return e.name;const m={ADV_EQUIP_WEAPON_PURCHASE_09:'Maça',ADV_EQUIP_ARMOR_02:'Cota de malha',ADV_EQUIP_ARMOR_04:'Escudo',ADV_EQUIP_GEAR_21:'Tochas (6)',ADV_EQUIP_GEAR_13:'Rações (7 dias)',ADV_EQUIP_GEAR_15:'Corda (15m)',ADV_EQUIP_GEAR_11:'Óleo'};return m[id]||String(id||'Item').replace(/^.*_/, '').replaceAll('_',' ')}
function fallbackThumb(id){const x=String(id||'').toLowerCase();let p=x.includes('armor_02')?'equipamentos/cota_malha.png':x.includes('gear_21')?'marca_navegacao/tocha.png':x.includes('gear_15')?'equipamentos/corda.png':x.includes('weapon')?'equipamentos/espada_longa.png':null;return p?img(asset(p),catalogLabel(id),'item-thumb'):'<div class="item-thumb-fallback">⚒</div>'}
function renderInventoryItem(it){const custom=viewState?.presentation?.itemThumbnails?.[it.itemId],canEquip=['WEAPON','ARMOR','SHIELD'].includes(it.kind)&&phase()==='CHARACTER_LOADOUT'&&loadoutState()&&!loadoutState().completed;return `<div class="item">${custom?img(custom,catalogLabel(it.catalogId),'item-thumb'):fallbackThumb(it.catalogId)}<b>${esc(catalogLabel(it.catalogId))}</b><small>×${it.quantity??1}${it.equipped?' · EQUIPADO':''}</small>${hasNative()?`<div class="actions">${canEquip?`<button data-loadout-equip="${esc(it.itemId)}" data-equipped="${it.equipped?'1':'0'}">${it.equipped?'TIRAR':'EQUIPAR'}</button>`:''}<button class="photo-action" title="Foto do equipamento" data-photo-item="${esc(it.itemId)}">📷</button></div>`:''}</div>`}
function renderNativeLoadout(){const l=loadoutState();if(!hasNative()||phase()!=='CHARACTER_LOADOUT'||!l||l.completed)return'';const groups=[['Aventura',['ADVENTURING_GEAR','FOOD','LIGHT_SOURCE','OIL_FLASK']],['Armas',['WEAPON']],['Armaduras e munição',['ARMOR','SHIELD','AMMUNITION']]];return `<div class="panel"><div class="kicker">Equipamento inicial</div><h3>${l.remainingWealthGp} po restantes de ${l.startingWealthGp} po</h3><p class="note">Preços, quantidades, peso e estatísticas vêm do perfil OSE ativo. A interface envia somente IDs canônicos.</p>${groups.map(([title,kinds])=>`<h3>${title}</h3><div class="inventory-grid">${(l.catalog||[]).filter(e=>kinds.includes(e.kind)).map(e=>`<div class="item"><b>${esc(e.name)}</b><small>${e.priceGp==null?'evidência insuficiente':`${e.priceGp} po`}${e.purchaseQuantity>1?` · ×${e.purchaseQuantity}`:''}${e.ownedQuantity?` · possui ${e.ownedQuantity}`:''}</small><div class="actions"><button data-loadout-buy="${esc(e.catalogId)}" ${e.priceGp==null||e.priceGp>l.remainingWealthGp?'disabled':''}>COMPRAR</button></div></div>`).join('')}</div>`).join('')}<button class="primary" data-loadout-finish="1" style="margin-top:9px">Concluir criação e iniciar aventura</button></div>`}

function renderDemoShop(){const shop=[['Espada longa','10 po'],['Maça','5 po'],['Cota de malha','40 po'],['Escudo','10 po'],['Tocha ×6','1 po'],['Rações ×7','15 po'],['Corda 15m','1 po'],['Óleo ×2','2 po']];return `<div class="panel"><h3>Comprar equipamento</h3><div class="inventory-grid">${shop.map(([n,p])=>`<div class="item"><b>${n}</b><small>${p}</small><div class="actions"><button>COMPRAR</button></div></div>`).join('')}</div><button class="primary" style="margin-top:9px">Concluir criação e iniciar aventura</button></div>`}

function renderCompany(){const party=viewState?.party||(!hasNative()?[{characterId:'demo-character',name:'Aventureiro',classIds:['FIGHTER'],hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1}]:[]);return `<div class="kicker">Companhia</div><div class="panel"><h2>${party.length?'Companhia atual':'Ainda vazia'}</h2>${party.length?`<div class="choice-grid">${party.map((m,i)=>{const p=viewState?.presentation?.characterPortraits?.[m.characterId];return `<div class="choice ${i===0?'active':''}">${p?img(p,m.name,'item-thumb'):''}<b>${esc(m.name)}</b><br>${esc((m.classIds||[]).join(' / '))}<br>HP ${hpText(m)}</div>`}).join('')}<div class="choice">Espaço para próximo aventureiro</div></div><p class="note">Todos compartilham a mesma posição canônica.</p>`:'<p>Crie seu primeiro personagem para formar a companhia.</p>'}</div>`}

function wireCurrentScreen(){
 document.querySelectorAll('[data-go]').forEach(b=>b.onclick=()=>go(b.dataset.go));
 document.querySelectorAll('[data-suggest]').forEach(b=>b.onclick=()=>{const t=el('reactionInput');if(t){t.value=b.dataset.suggest;t.focus()}});
 document.querySelector('[data-react]')?.addEventListener('click',()=>{const t=el('reactionInput'),v=t?.value.trim();if(!v)return showToast('Escreva uma reação antes de enviar.');if(postNative('PlayerReaction',{reactionId:'ui-'+Date.now(),text:v,surface:String(stateName).toUpperCase()}))showToast('PLAYER_REACTION enviado ao Rules/Session Engine.');else{localLog.push(`PLAYER_REACTION: ${v}`);showToast('Preview sem bridge nativa.')}});
 document.querySelectorAll('[data-tts]').forEach(b=>b.onclick=()=>{if(!postNative('TtsCommand',{command:b.dataset.tts}))showToast('Gemini TTS requer o app Android e uma chave configurada.');});
 document.querySelector('[data-scene-image]')?.addEventListener('click',()=>{if(!postNative('SceneImageGenerate',{explicitUserAction:true}))showToast('Geração de cena requer o app Android.');else showToast('Solicitando ilustração da cena ao Gemini…')});
 document.querySelector('[data-start-draft]')?.addEventListener('click',()=>postNative('CharacterCreationStart',{draftId:'draft-'+Date.now()}));
 document.querySelector('[data-commit-draft]')?.addEventListener('click',e=>{const d=e.currentTarget.dataset.commitDraft,name=el('creationName')?.value.trim();if(!name)return showToast('Digite um nome.');const adv=profileLabel().startsWith('ADVANCED');const method=adv?(el('creationMethod')?.value||'ADVANCED_BASIC'):'CLASSIC';const race=adv&&method==='ADVANCED_METHOD'?(el('creationRace')?.value||null):null;postNative('CharacterCreationCommit',{draftId:d,characterId:'char-'+Date.now(),name,method,classIds:[el('creationClass').value],raceId:race||'',adjustments:[]})});
 document.querySelectorAll('[data-photo-character]').forEach(b=>b.onclick=()=>chooseMedia('CHARACTER',b.dataset.photoCharacter));
 document.querySelectorAll('[data-photo-item]').forEach(b=>b.onclick=()=>chooseMedia('ITEM',b.dataset.photoItem));
 document.querySelectorAll('[data-loadout-buy]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_purchase',commandId:'buy-'+Date.now()+'-'+b.dataset.loadoutBuy,characterId:loadoutState()?.characterId||'',catalogId:b.dataset.loadoutBuy}));
 document.querySelectorAll('[data-loadout-equip]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_equip',commandId:'equip-'+Date.now()+'-'+b.dataset.loadoutEquip,characterId:loadoutState()?.characterId||'',itemId:b.dataset.loadoutEquip,equipped:b.dataset.equipped!=='1'}));
 document.querySelector('[data-loadout-finish]')?.addEventListener('click',()=>postNative('UiCommand',{command:'loadout_finish',commandId:'finish-'+Date.now(),characterId:loadoutState()?.characterId||''}));
 if(screen==='map')wireMap();
}
function wireMap(){let cam={x:-18,y:10,s:1},drag=null;const grid=el('mapGrid'),vp=el('mapViewport');if(!grid||!vp)return;const apply=()=>grid.style.transform=`translate(${cam.x}px,${cam.y}px) scale(${cam.s})`;apply();el('zoomIn').onclick=()=>{cam.s=Math.min(1.45,+(cam.s+.1).toFixed(2));apply()};el('zoomOut').onclick=()=>{cam.s=Math.max(.75,+(cam.s-.1).toFixed(2));apply()};vp.onpointerdown=e=>{drag={x:e.clientX,y:e.clientY,cx:cam.x,cy:cam.y};vp.setPointerCapture(e.pointerId)};vp.onpointermove=e=>{if(!drag)return;cam.x=drag.cx+e.clientX-drag.x;cam.y=drag.cy+e.clientY-drag.y;apply()};vp.onpointerup=vp.onpointercancel=()=>drag=null}
function chooseMedia(scope,id){mediaTarget={scope,id};el('mediaPicker').value='';el('mediaPicker').click()}
el('mediaPicker').addEventListener('change',()=>{const f=el('mediaPicker').files?.[0];if(!f||!mediaTarget)return;if(f.size>5*1024*1024)return showToast('Imagem deve ter no máximo 5 MB.');const r=new FileReader();r.onload=()=>postNative('PresentationImageSave',{scope:mediaTarget.scope,id:mediaTarget.id,dataUrl:r.result});r.readAsDataURL(f)});

document.querySelectorAll('.navbtn').forEach(b=>b.onclick=()=>go(b.dataset.screen));
el('settingsButton').onclick=()=>{el('settingsOverlay').classList.remove('hidden');postNative('GeminiSettingsGet',{});updateSettingsStatus()};el('settingsClose').onclick=()=>el('settingsOverlay').classList.add('hidden');
function updateSettingsStatus(){el('textSceneStatus').textContent=geminiState.textSceneConfigured?'Configurada no armazenamento seguro':'Não configurada';el('ttsAudioStatus').textContent=geminiState.ttsAudioConfigured?'Configurada no armazenamento seguro':'Não configurada'}
document.querySelectorAll('[data-key-save]').forEach(b=>b.onclick=()=>{const slot=b.dataset.keySave,input=slot==='TEXT_SCENE'?el('textSceneKey'):el('ttsAudioKey'),key=input.value.trim();if(!key)return showToast('Cole uma chave antes de salvar.');if(postNative('GeminiCredentialSet',{slot,key})){input.value='';showToast('Chave enviada ao armazenamento seguro.')}});
document.querySelectorAll('[data-key-test]').forEach(b=>b.onclick=()=>{if(postNative('GeminiCredentialTest',{slot:b.dataset.keyTest}))showToast('Testando conexão…')});
document.querySelectorAll('[data-key-delete]').forEach(b=>b.onclick=()=>postNative('GeminiCredentialDelete',{slot:b.dataset.keyDelete}));
render();document.body.dataset.ready='true';postNative('ViewState',{request:'initial'});
})();
