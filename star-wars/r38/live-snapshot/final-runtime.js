(function(){
'use strict';
const PREF_KEY='braseiro.starwars.ui.preferences.v1';
const defaults={textScale:'100',highContrast:false,reducedMotion:false,ttsEnabled:true,ttsRate:'1',ttsProvider:'android',geminiVoice:'Kore'};
const safeParse=(v,f)=>{try{return JSON.parse(v)}catch{return f}};
const prefs={...defaults,...safeParse(localStorage.getItem(PREF_KEY),{})};
function save(){try{localStorage.setItem(PREF_KEY,JSON.stringify(prefs))}catch{}}
function applyPrefs(){
 const root=document.documentElement;
 root.classList.toggle('braseiro-text-110',prefs.textScale==='110');
 root.classList.toggle('braseiro-text-120',prefs.textScale==='120');
 root.classList.toggle('braseiro-high-contrast',!!prefs.highContrast);
 root.classList.toggle('braseiro-reduced-motion',!!prefs.reducedMotion);
}
applyPrefs();
function nativeStatus(){
 const b=window.BraseiroTts;
 if(!b||typeof b.getStatus!=='function')return {available:false,detail:'Bridge TTS Android indisponível.'};
 try{return safeParse(b.getStatus(),{available:false,detail:'Resposta TTS inválida.'})}catch{return {available:false,detail:'Falha ao consultar TTS.'}}
}
function narrationText(){
 const host=document.querySelector('.scene-narrative-paper:not(.scene-narrative-reader) .scene-paper-copy');
 return String(host?.textContent||'').replace(/\s+/g,' ').trim();
}
function geminiStatus(){
 const b=window.BraseiroGemini;if(!b||typeof b.getStatus!=='function')return {available:false,configured:false};
 try{return {...safeParse(b.getStatus(),{}),available:true}}catch{return {available:true,configured:false}}
}
function speak(text){
 if(!prefs.ttsEnabled)return {ok:false,message:'TTS desativado em Ajustes.'};
 const clean=String(text||'').trim();
 if(!clean)return {ok:false,message:'Não há narração visível para ler.'};
 if(prefs.ttsProvider==='gemini'){
  const g=window.BraseiroGemini;
  if(g&&typeof g.speakTts==='function'){
   try{const r=safeParse(g.speakTts(clean,String(prefs.geminiVoice||'Kore')),{ok:false,message:'Resposta Gemini TTS inválida.'});if(r&&r.ok)return r;return r||{ok:false,message:'Gemini TTS indisponível.'}}catch{}
  }
 }
 const b=window.BraseiroTts;
 if(b&&typeof b.speak==='function'){
  try{return safeParse(b.speak(clean,String(prefs.ttsRate||'1')),{ok:false,message:'Resposta TTS inválida.'})}catch{return {ok:false,message:'Falha ao iniciar TTS Android.'}}
 }
 if('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window){
  try{window.speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(clean);u.lang='pt-BR';u.rate=Math.max(.6,Math.min(1.4,Number(prefs.ttsRate)||1));window.speechSynthesis.speak(u);return {ok:true,fallback:true}}catch{}
 }
 return {ok:false,message:'TTS não está disponível neste dispositivo.'};
}
function stop(){
 const g=window.BraseiroGemini;if(g&&typeof g.stopTts==='function'){try{g.stopTts()}catch{}}
 const b=window.BraseiroTts;if(b&&typeof b.stop==='function'){try{b.stop()}catch{}}
 try{window.speechSynthesis?.cancel()}catch{}
 return {ok:true};
}
function speakerSvg(){return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 10v4h4l5 4V6L8 10z"/><path d="M16.5 9c1.7 1.8 1.7 4.2 0 6M19 6.5c3.4 3.2 3.4 7.8 0 11"/></svg>'}
function enhanceNarration(){
 document.querySelectorAll('.scene-narrative-paper:not(.scene-narrative-reader) .scene-narrative-heading-row').forEach(row=>{
  if(row.querySelector('.scene-narrative-tts-button'))return;
  const btn=document.createElement('button');btn.type='button';btn.className='scene-narrative-tts-button';btn.setAttribute('aria-label','Ouvir narração do Mestre');btn.title='Ouvir narração do Mestre';btn.innerHTML=speakerSvg();
  btn.addEventListener('click',()=>{const r=speak(narrationText());btn.setAttribute('aria-pressed',r&&r.ok?'true':'false');setTimeout(()=>btn.setAttribute('aria-pressed','false'),900)});
  const expand=row.querySelector('.scene-narrative-expand-button');row.insertBefore(btn,expand||null);
 });
}
function cardByTitle(title){
 return [...document.querySelectorAll('section')].find(s=>{
  const first=s.firstElementChild;return first&&String(first.textContent||'').trim()===title;
 });
}
function enhanceAccessibility(){
 const card=cardByTitle('Interface');if(!card||card.dataset.finalEnhanced==='1')return;
 card.dataset.finalEnhanced='1';const content=card.children[1];if(!content)return;
 content.innerHTML=`<div class="final-settings-grid">
 <div class="final-setting-row"><label for="final-text-scale">Escala de texto</label><select id="final-text-scale"><option value="100">100%</option><option value="110">110%</option><option value="120">120%</option></select></div>
 <div class="final-setting-row"><span>Alto contraste</span><input id="final-high-contrast" type="checkbox" aria-label="Alto contraste"></div>
 <div class="final-setting-row"><span>Reduzir movimento</span><input id="final-reduced-motion" type="checkbox" aria-label="Reduzir movimento"></div>
 <div class="final-setting-status">Preferências salvas localmente neste aparelho.</div></div>`;
 const scale=content.querySelector('#final-text-scale'), contrast=content.querySelector('#final-high-contrast'), motion=content.querySelector('#final-reduced-motion');
 scale.value=prefs.textScale;contrast.checked=!!prefs.highContrast;motion.checked=!!prefs.reducedMotion;
 scale.addEventListener('change',()=>{prefs.textScale=scale.value;save();applyPrefs()});
 contrast.addEventListener('change',()=>{prefs.highContrast=contrast.checked;save();applyPrefs()});
 motion.addEventListener('change',()=>{prefs.reducedMotion=motion.checked;save();applyPrefs()});
}
function bridgeResult(name,...args){
 const b=window.BraseiroGameBridge;
 if(!b||typeof b[name]!=='function')return {ok:false,error:'BRIDGE_UNAVAILABLE',message:'Motor Barbara indisponível.'};
 try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,error:'INVALID_BRIDGE',message:'Resposta inválida do Motor Barbara.'}}catch(e){return {ok:false,error:'ENGINE_REJECTED',message:String(e&&e.message||e||'Falha no Motor Barbara.')}}
}
function playerView(){
 const r=bridgeResult('scenePlayerView');
 return r&&r.ok?r.data:null;
}
function controlledText(el,value){
 if(!el)return;
 const proto=window.HTMLTextAreaElement&&window.HTMLTextAreaElement.prototype;
 const setter=proto&&Object.getOwnPropertyDescriptor(proto,'value')?.set;
 if(setter)setter.call(el,String(value??''));else el.value=String(value??'');
 el.dispatchEvent(new Event('input',{bubbles:true}));
 el.focus();
}
function initials(name){return String(name||'').trim().split(/\s+/).slice(0,2).map(x=>x[0]||'').join('').toUpperCase()||'•'}
function skillLabel(key){
 const map={actividad_criminal:'Atividade criminosa',aguante:'Resistência',alerta:'Alerta',astronavegacion:'Astronavegação',atletismo:'Atletismo',callejeo:'Vida nas ruas',carisma:'Carisma',coaccion:'Coerção',computadoras:'Computadores',conocimientos:'Conhecimentos',coordinacion:'Coordenação',disciplina:'Disciplina',engano:'Enganação',frialdad:'Frieza',liderazgo:'Liderança',mecanica:'Mecânica',medicina:'Medicina',negociacion:'Negociação',percepcion:'Percepção',pilotar:'Pilotagem',pilotar_vehiculo_espacial:'Pilotagem espacial',pilotar_vehiculo_planetario:'Pilotagem planetária',sigilo:'Furtividade',supervivencia:'Sobrevivência',armas_distancia_ligeras:'Armas de distância leves',armas_distancia_pesadas:'Armas de distância pesadas',armas_cuerpo_a_cuerpo:'Armas corpo a corpo',artilleria:'Artilharia',pelea:'Briga'};
 return map[String(key||'')]||String(key||'Teste').replace(/_/g,' ').replace(/^./,m=>m.toUpperCase());
}
function checkSummary(check){
 if(!check)return '';
 const main=check.success?'SUCESSO':'FALHA';
 const parts=[main,`${Math.abs(Number(check.netSuccess)||0)} ${Number(check.netSuccess)>=0?'sucesso(s) líquido(s)':'falha(s) líquida(s)'}`];
 const adv=Number(check.netAdvantage)||0;if(adv)parts.push(`${Math.abs(adv)} ${adv>0?'vantagem(ns)':'ameaça(s)'}`);
 const tri=Number(check.triumph)||0,des=Number(check.despair)||0;if(tri)parts.push(`${tri} triunfo(s)`);if(des)parts.push(`${des} desespero(s)`);
 return parts.join(' · ');
}
function enhanceSceneFromEngine(){
 const root=document.querySelector('.scene-concept-shell');if(!root)return;
 const view=playerView();if(!view)return;
 const situation=root.querySelector('.scene-situation-paper p');if(situation&&view.situation)situation.textContent=view.situation;
 const participantSlots=[...root.querySelectorAll('.scene-participant-slot')];
 const participants=Array.isArray(view.participants)?view.participants:[];
 participantSlots.forEach((slot,i)=>{
  const p=participants[i],frame=slot.querySelector('.scene-participant-frame'),strong=slot.querySelector('strong'),small=slot.querySelector('small');
  if(p){
   if(frame){frame.classList.remove('is-empty');frame.textContent=initials(p.name);frame.setAttribute('aria-label',`Participante: ${p.name}`)}
   if(strong)strong.textContent=p.name;if(small)small.textContent=p.meta||p.status||'Visível';
  }else{
   if(frame){frame.classList.add('is-empty');frame.innerHTML='<span class="scene-participant-empty-mark" aria-hidden="true"></span>';frame.removeAttribute('aria-label')}
   if(strong)strong.textContent='—';if(small)small.textContent='Aguardando participante';
  }
 });
 const actionSlots=[...root.querySelectorAll('.scene-action-slot')];
 const actions=Array.isArray(view.suggestedActions)?view.suggestedActions.slice(0,6):[];
 actionSlots.forEach((slot,i)=>{
  const action=actions[i],strong=slot.querySelector('strong'),small=slot.querySelector('small'),end=slot.querySelector('.scene-action-empty-end');
  if(action){
   slot.classList.remove('is-empty');slot.classList.add('is-live');slot.setAttribute('role','button');slot.tabIndex=0;slot.setAttribute('aria-label',`Usar ação sugerida: ${action}`);
   if(strong)strong.textContent=action;if(small)small.textContent='Sugestão do Mestre';if(end){end.textContent='›';end.classList.add('scene-action-chevron')}
   const activate=()=>controlledText(root.querySelector('.scene-composer textarea'),action);
   slot.onclick=activate;slot.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();activate()}};
  }else{
   slot.classList.add('is-empty');slot.classList.remove('is-live');slot.removeAttribute('role');slot.removeAttribute('tabindex');slot.removeAttribute('aria-label');slot.onclick=null;slot.onkeydown=null;
   if(strong)strong.textContent='—';if(small)small.textContent='Aguardando ação disponível';if(end){end.textContent='';end.classList.remove('scene-action-chevron')}
  }
 });
 const statusCells=[...root.querySelectorAll('.scene-status-cell')];
 if(statusCells.length&&view.vehicle){
  const vals=[view.vehicle.hull,view.vehicle.strain,view.vehicle.speed,view.vehicle.defense];
  statusCells.forEach((cell,i)=>{const spans=cell.querySelectorAll('.scene-status-copy>span');if(spans[1]){spans[1].textContent=String(vals[i]??'—');spans[1].style.color='#D5D8DC';spans[1].style.fontWeight='800'}});
 }
 const panel=root.querySelector('.scene-actions-panel');
 if(panel){
  let checkBox=panel.querySelector('.final-suggested-check');
  const c=view.suggestedCheck;
  if(c){
   if(!checkBox){checkBox=document.createElement('div');checkBox.className='final-suggested-check';panel.appendChild(checkBox)}
   const diff=Number(c.difficulty||0),chal=Number(c.challenge||0),boost=Number(c.boost||0),setback=Number(c.setback||0);
   checkBox.innerHTML=`<div><strong>${skillLabel(c.skillKey)}</strong><small>${c.reason||'Teste sugerido pelo Mestre'} · D${diff}${chal?` · C${chal}`:''}${boost?` · +${boost} impulso`:''}${setback?` · +${setback} contratempo`:''}</small></div><button type="button">ROLAR</button><output aria-live="polite"></output>`;
   const btn=checkBox.querySelector('button'),out=checkBox.querySelector('output');
   btn.onclick=()=>{btn.disabled=true;out.textContent='Rolando…';const r=bridgeResult('resolveSuggestedMasterCheck');if(r.ok){out.textContent=checkSummary(r.data?.check)||'Teste resolvido.';setTimeout(enhanceSceneFromEngine,0)}else{out.textContent=r.message||'Teste rejeitado pelo motor.';btn.disabled=false}};
  }else if(checkBox){
   const last=view.lastCheck;checkBox.innerHTML=last?`<div><strong>Último teste</strong><small>${checkSummary(last)}</small></div>`:'';
   if(!last)checkBox.remove();
  }
 }
}
function enhanceTtsSettings(){
 const card=cardByTitle('Narração / voz');if(!card||card.dataset.finalEnhanced==='1')return;
 card.dataset.finalEnhanced='1';const content=card.children[1];if(!content)return;
 const st=nativeStatus(),gst=geminiStatus();
 content.innerHTML=`<div class="final-settings-grid">
 <div class="final-setting-row"><span>TTS da narração</span><input id="final-tts-enabled" type="checkbox" aria-label="Ativar TTS da narração"></div>
 <div class="final-setting-row"><label for="final-tts-provider">Provedor</label><select id="final-tts-provider"><option value="android">Android TTS</option><option value="gemini">Gemini TTS</option></select></div>
 <div class="final-setting-row"><label for="final-tts-rate">Velocidade Android</label><select id="final-tts-rate"><option value="0.8">0,8×</option><option value="1">1,0×</option><option value="1.1">1,1×</option><option value="1.2">1,2×</option><option value="1.4">1,4×</option></select></div>
 <div class="final-setting-row"><label for="final-gemini-voice">Voz Gemini</label><select id="final-gemini-voice"><option>Kore</option><option>Aoede</option><option>Charon</option><option>Fenrir</option><option>Puck</option></select></div>
 <div class="final-setting-actions"><button type="button" id="final-tts-test">Testar voz</button><button type="button" id="final-tts-stop">Parar</button></div>
 <div class="final-setting-status" id="final-tts-status"></div></div>`;
 const enabled=content.querySelector('#final-tts-enabled'),provider=content.querySelector('#final-tts-provider'),rate=content.querySelector('#final-tts-rate'),voice=content.querySelector('#final-gemini-voice'),status=content.querySelector('#final-tts-status');
 enabled.checked=!!prefs.ttsEnabled;provider.value=String(prefs.ttsProvider||'android');rate.value=String(prefs.ttsRate||'1');voice.value=String(prefs.geminiVoice||'Kore');
 const refreshStatus=()=>{status.textContent=provider.value==='gemini'?(gst.available?(gst.configured?`Gemini TTS pronto · ${gst.ttsModel||'modelo configurado'}`:'Gemini disponível; configure a chave API na aba Gemini.'):'Bridge Gemini indisponível.'):(st.available?`Android TTS pronto · ${st.language||'pt-BR'}`:(st.detail||'Fallback do WebView será usado se disponível.'))};refreshStatus();
 enabled.addEventListener('change',()=>{prefs.ttsEnabled=enabled.checked;save()});
 provider.addEventListener('change',()=>{prefs.ttsProvider=provider.value;save();refreshStatus()});
 rate.addEventListener('change',()=>{prefs.ttsRate=rate.value;save()});
 voice.addEventListener('change',()=>{prefs.geminiVoice=voice.value;save()});
 content.querySelector('#final-tts-test').addEventListener('click',()=>{const r=speak('Teste de narração do Mestre.');status.textContent=r.ok?(provider.value==='gemini'?'Gemini TTS iniciado.':'Voz reproduzida.'):(r.message||'Não foi possível reproduzir a voz.')});
 content.querySelector('#final-tts-stop').addEventListener('click',()=>{stop();status.textContent='Voz interrompida.'});
 const stateCard=cardByTitle('Estado');if(stateCard){const body=stateCard.children[1];if(body)body.textContent='Somente o texto visível da Narração do Mestre é lido. Android TTS funciona offline quando instalado; Gemini TTS usa a chave configurada no aparelho.'}
}
function campaignMode(){const r=bridgeResult('campaignModeSummary');return r&&r.ok?String(r.data?.campaignMode||'EOTE').toUpperCase():'EOTE'}
function relabelCreationForCampaign(){
 const mode=campaignMode();if(mode!=='AOR')return;
 const replacements=new Map([
  ['Obligation / vínculo de campanha','Duty / vínculo com a Aliança'],['Tipo de Obligation','Tipo de Duty'],['Selecione uma Obligation','Selecione um Duty'],
  ['+5 Obligation → +5 PE','−5 Duty → +5 PE'],['+10 Obligation → +10 PE','−10 Duty → +10 PE'],['+5 Obligation → +1.000 créditos','−5 Duty → +1.000 créditos'],['+10 Obligation → +2.500 créditos','−10 Duty → +2.500 créditos'],
  ['Obligation inicial','Duty inicial'],['Obligation adicional','Duty gasto'],['Obligation final','Duty final'],['Obligation','Duty']
 ]);
 const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let n;
 while((n=walker.nextNode())){const v=String(n.nodeValue||'').trim();if(replacements.has(v))n.nodeValue=String(n.nodeValue).replace(v,replacements.get(v));}
}
function currentState(){const r=bridgeResult('readState');return r&&r.ok?r.data:null}
function sandboxReady(){const r=bridgeResult('livingSandboxStatus');return Boolean(r&&r.ok&&r.data?.sandbox?.initialized)}
function hasCharacter(){return Boolean(currentState()?.player?.character)}
function adventureStarted(){const st=currentState();return Boolean(String(st?.master?.narration||st?.scene?.narration||'').trim())}
function navigateRoute(route){const u=new URL(location.href);u.searchParams.set('screen',String(route));history.pushState({},'',u);window.dispatchEvent(new PopStateEvent('popstate'));window.scrollTo(0,0)}
function randomCharacterIdentity(){
 const names=['Nera Vos','Kellan Rook','Syla Venn','Dax Marr','Tarin Vale','Mira Kest','Joren Nox','Kael Sorn','Veya Tann','Rian Korr','Lysa Renn','Bren Tal'];
 const motivations=['Pagar uma dívida sem perder a própria liberdade.','Proteger alguém que ficou para trás.','Encontrar uma oportunidade grande o bastante para mudar de vida.','Descobrir quem sabotou seu antigo grupo.','Construir reputação entre contrabandistas e pilotos.','Ajudar pessoas esmagadas pelo poder do Império.'];
 return {name:names[Math.floor(Math.random()*names.length)],motivation:motivations[Math.floor(Math.random()*motivations.length)]};
}
function quickRandomCharacter(statusEl){const id=randomCharacterIdentity();const r=bridgeResult('generateRandomCharacter',id,{createSnapshot:true});if(!r.ok){if(statusEl)statusEl.textContent=r.message||'Não foi possível gerar a ficha.';return false}if(statusEl)statusEl.textContent=`Ficha criada: ${id.name}.`;setTimeout(enhance,0);return true}
function quickImportCharacter(statusEl){
 const input=document.createElement('input');input.type='file';input.accept='application/json,.json';input.hidden=true;document.body.appendChild(input);
 input.onchange=()=>{const f=input.files?.[0];if(!f){input.remove();return}if(f.size>16*1024*1024){if(statusEl)statusEl.textContent='O JSON excede 16 MB.';input.remove();return}const reader=new FileReader();reader.onload=()=>{const r=bridgeResult('importCharacterJson',String(reader.result||''),{createSnapshot:true});if(statusEl)statusEl.textContent=r.ok?'Ficha importada.':(r.message||'Falha ao importar ficha.');input.remove();setTimeout(enhance,0)};reader.onerror=()=>{if(statusEl)statusEl.textContent='Falha ao ler o arquivo JSON.';input.remove()};reader.readAsText(f)};input.click();
}
const START_POINTS=[
 {name:'Mos Eisley · Tatooine',x:65,y:55,desc:'Porto do deserto, contrabando e oportunidades no Borda Exterior.'},
 {name:'Nar Shaddaa',x:48,y:43,desc:'Lua dos contrabandistas, cartéis, caçadores e mercados clandestinos.'},
 {name:'Ord Mantell',x:39,y:31,desc:'Sucata, mercenários, rotas de guerra e negócios cinzentos.'},
 {name:'Corellia',x:43,y:48,desc:'Estaleiros, pilotos, sindicatos e tráfego interestelar intenso.'},
 {name:'Ryloth',x:57,y:52,desc:'Rotas de especiarias, ocupação imperial e redes locais.'},
 {name:'Eriadu',x:60,y:67,desc:'Mundo industrial estratégico entre o Núcleo e a Borda Exterior.'},
 {name:'Socorro',x:71,y:62,desc:'Refúgio periférico de pilotos, oficinas e contrabandistas.'},
 {name:'Bespin',x:51,y:58,desc:'Cidade das Nuvens, tibanna, comércio e interesses imperiais.'}
];
function setupOverlay(){
 if(document.getElementById('braseiro-sandbox-setup')||sandboxReady())return;
 const modeResult=bridgeResult('campaignModeSummary');if(!modeResult.ok)return;
 const host=document.createElement('div');host.id='braseiro-sandbox-setup';host.setAttribute('role','dialog');host.setAttribute('aria-modal','true');host.setAttribute('aria-label','Configurar campanha sandbox');host.className='final-sandbox-overlay';
 host.innerHTML=`<div class="final-sandbox-card"><div class="final-kicker">MUNDO LIVRE</div><h2>Começar sem roteiro</h2><p>Escolha o perfil da campanha e onde a história começa. Você não precisa escrever o ponto inicial.</p>
 <label class="final-field"><span>Campanha</span><select id="sandbox-mode"><option value="EOTE">Edge of the Empire</option><option value="AOR">Age of Rebellion</option><option value="MIXED">Mista · EotE + AoR</option></select></label>
 <div class="final-field"><span>Ponto inicial</span><div class="final-mode-tabs"><button type="button" data-start-mode="list" class="is-active">LISTA</button><button type="button" data-start-mode="map">MAPA</button><button type="button" data-start-mode="random">ALEATÓRIO</button></div></div>
 <div id="sandbox-list" class="final-start-list">${START_POINTS.map((p,i)=>`<button type="button" data-point="${i}" class="${i===0?'is-selected':''}"><strong>${p.name}</strong><small>${p.desc}</small></button>`).join('')}</div>
 <div id="sandbox-map" class="final-start-map" hidden><img src="assets/maps/galaxy_map.jpg" alt="Mapa da galáxia para escolher o ponto inicial">${START_POINTS.map((p,i)=>`<button type="button" data-point="${i}" style="left:${p.x}%;top:${p.y}%" aria-label="Escolher ${p.name}">${i+1}</button>`).join('')}<div class="final-map-caption" id="sandbox-map-caption">${START_POINTS[0].name}</div></div>
 <div class="final-selected-point"><span>Selecionado</span><strong id="sandbox-selected-point">${START_POINTS[0].name}</strong></div>
 <div id="sandbox-setup-status" role="status" class="final-status"></div><button id="sandbox-start-button" type="button" class="final-primary-button">CRIAR MUNDO VIVO</button>
 <p class="final-footnote">Seed inicial: 6 facções, 36 NPCs persistentes, 12 locais, relógios, mercados e oportunidades. Sem roteiro obrigatório; aventuras publicadas são opcionais.</p></div>`;
 document.body.appendChild(host);
 const select=host.querySelector('#sandbox-mode'),button=host.querySelector('#sandbox-start-button'),status=host.querySelector('#sandbox-setup-status'),list=host.querySelector('#sandbox-list'),map=host.querySelector('#sandbox-map'),selected=host.querySelector('#sandbox-selected-point'),caption=host.querySelector('#sandbox-map-caption');select.value=String(modeResult.data?.campaignMode||'EOTE');let pointIndex=0,mode='list';
 const choose=i=>{pointIndex=Math.max(0,Math.min(START_POINTS.length-1,Number(i)||0));host.querySelectorAll('[data-point]').forEach(x=>x.classList.toggle('is-selected',Number(x.dataset.point)===pointIndex));selected.textContent=START_POINTS[pointIndex].name;caption.textContent=START_POINTS[pointIndex].name};host.querySelectorAll('[data-point]').forEach(x=>x.onclick=()=>choose(x.dataset.point));
 host.querySelectorAll('[data-start-mode]').forEach(x=>x.onclick=()=>{mode=x.dataset.startMode;host.querySelectorAll('[data-start-mode]').forEach(y=>y.classList.toggle('is-active',y===x));if(mode==='random'){choose(Math.floor(Math.random()*START_POINTS.length));list.hidden=false;map.hidden=true}else{list.hidden=mode!=='list';map.hidden=mode!=='map'}});
 button.onclick=()=>{const point=START_POINTS[pointIndex].name;button.disabled=true;status.textContent='Criando mundo persistente…';const m=bridgeResult('setCampaignMode',select.value);if(!m.ok){status.textContent=m.message||'Modo inválido.';button.disabled=false;return}const r=bridgeResult('initializeLivingSandbox',{startingPoint:point,regionLabel:`Região de ${point}`});if(!r.ok){status.textContent=r.message||'Falha ao criar mundo.';button.disabled=false;return}status.textContent='Mundo criado.';host.remove();setTimeout(()=>{relabelCreationForCampaign();enhance()},0)};
}
function enhanceAdventureGate(){
 const existing=document.querySelector('.final-adventure-gate');
 if(!sandboxReady()||adventureStarted()){document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());return}
 const route=new URLSearchParams(location.search).get('screen')||'00';const character=hasCharacter(),gateKey=`${route}:${character?'character':'empty'}`;
 if(existing?.dataset?.gateKey===gateKey)return;
 document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());
 if(route==='00'){
  const root=document.querySelector('.scene-concept-shell');if(!root)return;const gate=document.createElement('section');gate.className='final-adventure-gate final-adventure-card';gate.dataset.gateKey=gateKey;
  gate.innerHTML=character?`<div><span>PRONTO PARA JOGAR</span><strong>Seu mundo vivo está criado.</strong><small>Barbara vai gerar a situação inicial a partir do local, facções, NPCs e oportunidades persistentes.</small></div><button type="button" data-start-adventure>INICIAR AVENTURA</button>`:`<div><span>ANTES DE COMEÇAR</span><strong>Crie ou carregue um personagem.</strong><small>Você pode criar manualmente, gerar uma ficha completa aleatória ou importar .json.</small></div><div class="final-adventure-actions"><button type="button" data-manual-character>CRIAR FICHA</button><button type="button" data-random-character>ALEATÓRIA</button><button type="button" data-import-character>IMPORTAR JSON</button></div><output></output>`;
  root.prepend(gate);const out=gate.querySelector('output');gate.querySelector('[data-manual-character]')?.addEventListener('click',()=>navigateRoute('18'));gate.querySelector('[data-random-character]')?.addEventListener('click',()=>quickRandomCharacter(out));gate.querySelector('[data-import-character]')?.addEventListener('click',()=>quickImportCharacter(out));gate.querySelector('[data-start-adventure]')?.addEventListener('click',()=>{const b=gate.querySelector('[data-start-adventure]');b.disabled=true;b.textContent='INICIANDO…';const r=bridgeResult('startLivingAdventure',{});if(!r.ok){b.disabled=false;b.textContent='INICIAR AVENTURA';alert(r.message||'Não foi possível iniciar a aventura.');return}setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();enhanceSceneGeminiButton()},0)});
 }else if(character){
  const bar=document.createElement('button');bar.type='button';bar.className='final-adventure-gate final-adventure-fab';bar.dataset.gateKey=gateKey;bar.textContent='▶ INICIAR AVENTURA';bar.onclick=()=>{const r=bridgeResult('startLivingAdventure',{});if(!r.ok){alert(r.message||'Não foi possível iniciar a aventura.');return}navigateRoute('00')};document.body.appendChild(bar);
 }
}
function currentSceneRecord(){try{const state=safeParse(localStorage.getItem('braseiro.starwars.edge.state.v1'),null);return state&&state.scene&&typeof state.scene==='object'?state.scene:null}catch{return null}}
function sceneGeminiPrompt(){const narration=narrationText(),situation=String(document.querySelector('.scene-situation-paper p')?.textContent||'').trim(),scene=currentSceneRecord(),title=String(scene?.title||'Cena').trim()||'Cena';const text=[title,narration,situation].filter(Boolean).join('\n\n');return text?`Ilustração cinematográfica de uma cena de Star Wars para VTT. Sem texto na imagem. Preserve exatamente os fatos visíveis desta cena, sem inventar personagens nomeados ou eventos: ${text}`:''}
function sceneKeyFor(value){const text=String(value??'Cena').trim()||'Cena';let h=2166136261;for(let i=0;i<text.length;i++){h^=text.charCodeAt(i);h=Math.imul(h,16777619)}return `scene_${(h>>>0).toString(16).padStart(8,'0')}`}
function currentSceneKey(){const scene=currentSceneRecord();return sceneKeyFor(scene?.id??scene?.sceneId??scene?.title??'Cena')}
function enhanceSceneGeminiButton(){
 const media=document.querySelector('.scene-media-slot');if(!media||media.querySelector('.final-scene-gemini'))return;const wrap=document.createElement('div');wrap.className='final-scene-gemini';wrap.innerHTML='<button type="button">✦ GERAR ILUSTRAÇÃO</button><small></small>';media.appendChild(wrap);const btn=wrap.querySelector('button'),msg=wrap.querySelector('small');btn.onclick=()=>{const g=window.BraseiroGemini,st=geminiStatus(),prompt=sceneGeminiPrompt();if(!g||typeof g.generateSceneImage!=='function'){msg.textContent='Gemini indisponível neste host.';return}if(!st.configured){msg.textContent='Configure a chave Gemini em AJUSTES → Gemini.';return}if(!prompt){msg.textContent='Inicie a aventura para gerar a cena.';return}btn.disabled=true;msg.textContent='Enviando prompt ao Gemini…';try{const r=safeParse(g.generateSceneImage(prompt,currentSceneKey()),{});if(!r.ok){btn.disabled=false;msg.textContent=r.message||'Falha ao iniciar geração.'}else{msg.textContent='Gerando imagem…'}}catch{btn.disabled=false;msg.textContent='Falha ao chamar Gemini.'}};
}
function enhanceGeminiModels(){
 const card=cardByTitle('Chave da API Gemini');if(!card||card.querySelector('.final-gemini-models'))return;const g=window.BraseiroGemini;if(!g||typeof g.configureModels!=='function')return;const st=geminiStatus();const box=document.createElement('div');box.className='final-gemini-models';box.innerHTML=`<div class="final-model-title">Modelos Gemini</div><label>Texto / Mestre<input data-model="master" value="${String(st.masterModel||'gemini-3.8-flash')}"></label><label>Imagem de cena<input data-model="image" value="${String(st.model||'gemini-3.1-flash-image')}"></label><label>TTS<input data-model="tts" value="${String(st.ttsModel||'gemini-3.1-flash-tts-preview')}"></label><button type="button">SALVAR MODELOS</button><output></output>`;card.children[1]?.appendChild(box);box.querySelector('button').onclick=()=>{const vals=[...box.querySelectorAll('input')].map(x=>x.value.trim());try{const r=safeParse(g.configureModels(vals[0],vals[1],vals[2]),{});box.querySelector('output').textContent=r.ok?'Modelos salvos no aparelho.':(r.message||'Falha ao salvar modelos.')}catch{box.querySelector('output').textContent='Falha ao salvar modelos.'}};
}
function enhanceCharacterEmptyActions(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06')return;const headings=[...document.querySelectorAll('section')].find(s=>String(s.firstElementChild?.textContent||'').trim()==='Personagem');if(!headings||!String(headings.textContent||'').includes('Nenhum personagem carregado'))return;if(document.querySelector('.final-empty-character-actions'))return;const existing=[...document.querySelectorAll('button')].find(b=>String(b.textContent||'').includes('Abrir fluxo de criação'));if(!existing)return;const box=document.createElement('div');box.className='final-empty-character-actions';box.innerHTML='<button type="button" data-random>GERAR FICHA ALEATÓRIA</button><button type="button" data-import>IMPORTAR .JSON</button><output></output>';existing.insertAdjacentElement('afterend',box);const out=box.querySelector('output');box.querySelector('[data-random]').onclick=()=>quickRandomCharacter(out);box.querySelector('[data-import]').onclick=()=>quickImportCharacter(out);
}
function enhanceUtilityShortcuts(){
 if(document.querySelector('.final-utility-shortcuts'))return;const route=new URLSearchParams(location.search).get('screen')||'00';if(!['27','28','29','30'].includes(route))return;const main=document.querySelector('main')||document.querySelector('#root>div>div');if(!main)return;const nav=document.createElement('nav');nav.className='final-utility-shortcuts';nav.innerHTML='<button type="button" data-r="27">INTERFACE</button><button type="button" data-r="28">VOZ</button><button type="button" data-r="29">GEMINI</button><button type="button" data-r="30">BIBLIOTECA</button>';nav.querySelectorAll('button').forEach(b=>{b.classList.toggle('is-active',b.dataset.r===route);b.onclick=()=>navigateRoute(b.dataset.r)});main.prepend(nav);
}
function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay();enhanceAdventureGate();enhanceSceneGeminiButton();enhanceGeminiModels();enhanceCharacterEmptyActions();enhanceUtilityShortcuts()}
let scheduled=false;const observer=new MutationObserver(()=>{if(scheduled)return;scheduled=true;requestAnimationFrame(()=>{scheduled=false;enhance()})});
observer.observe(document.documentElement,{subtree:true,childList:true});
window.addEventListener('popstate',()=>setTimeout(enhance,0));
window.addEventListener('DOMContentLoaded',enhance);setTimeout(enhance,0);
window.BraseiroFinalRuntime={version:'1.0.0',speakVisibleNarration:()=>speak(narrationText()),stopNarration:stop,refreshScene:enhanceSceneFromEngine,preferences:prefs};
})();
