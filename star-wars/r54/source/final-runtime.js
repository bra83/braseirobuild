(function(){
'use strict';
const PREF_KEY='braseiro.starwars.ui.preferences.v1';
const defaults={textScale:'100',highContrast:false,reducedMotion:false,ttsEnabled:true,ttsRate:'1'};
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
function speak(text){
 if(!prefs.ttsEnabled)return {ok:false,message:'TTS desativado em Ajustes.'};
 const clean=String(text||'').trim();
 if(!clean)return {ok:false,message:'Não há narração visível para ler.'};
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
 const b=window.BraseiroTts;if(b&&typeof b.stop==='function'){try{return safeParse(b.stop(),{ok:true})}catch{}}
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
 const st=nativeStatus();
 content.innerHTML=`<div class="final-settings-grid">
 <div class="final-setting-row"><span>TTS da narração</span><input id="final-tts-enabled" type="checkbox" aria-label="Ativar TTS da narração"></div>
 <div class="final-setting-row"><label for="final-tts-rate">Velocidade</label><select id="final-tts-rate"><option value="0.8">0,8×</option><option value="1">1,0×</option><option value="1.1">1,1×</option><option value="1.2">1,2×</option><option value="1.4">1,4×</option></select></div>
 <div class="final-setting-actions"><button type="button" id="final-tts-test">Testar voz</button><button type="button" id="final-tts-stop">Parar</button></div>
 <div class="final-setting-status" id="final-tts-status"></div></div>`;
 const enabled=content.querySelector('#final-tts-enabled'),rate=content.querySelector('#final-tts-rate'),status=content.querySelector('#final-tts-status');
 enabled.checked=!!prefs.ttsEnabled;rate.value=String(prefs.ttsRate||'1');status.textContent=st.available?`Android TTS pronto · ${st.language||'pt-BR'}`:(st.detail||'Fallback do WebView será usado se disponível.');
 enabled.addEventListener('change',()=>{prefs.ttsEnabled=enabled.checked;save()});
 rate.addEventListener('change',()=>{prefs.ttsRate=rate.value;save()});
 content.querySelector('#final-tts-test').addEventListener('click',()=>{const r=speak('Teste de narração do Mestre.');status.textContent=r.ok?'Voz reproduzida.':(r.message||'Não foi possível reproduzir a voz.')});
 content.querySelector('#final-tts-stop').addEventListener('click',()=>{stop();status.textContent='Voz interrompida.'});
 const stateCard=cardByTitle('Estado');
 if(stateCard){const body=stateCard.children[1];if(body)body.textContent=st.available?'Narração por voz disponível pelo Android TTS. Somente o texto visível da Narração do Mestre é lido.':'TTS Android não está pronto; o app tenta o sintetizador do WebView quando disponível.'}
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
function sandboxReady(){const r=bridgeResult('livingSandboxStatus');return Boolean(r&&r.ok&&r.data?.sandbox?.initialized)}
function setupOverlay(){
 if(document.getElementById('braseiro-sandbox-setup')||sandboxReady())return;
 const modeResult=bridgeResult('campaignModeSummary');if(!modeResult.ok)return;
 const host=document.createElement('div');host.id='braseiro-sandbox-setup';host.setAttribute('role','dialog');host.setAttribute('aria-modal','true');host.setAttribute('aria-label','Configurar campanha sandbox');
 host.style.cssText='position:fixed;inset:0;z-index:2147483600;background:rgba(5,7,9,.94);display:grid;place-items:center;padding:18px;color:#D5D8DC;font-family:inherit';
 host.innerHTML=`<div style="width:min(100%,390px);max-height:88vh;overflow:auto;background:#181C1F;border:1px solid #D49D42;border-radius:10px;padding:18px;box-shadow:0 20px 70px #000">
  <div style="color:#D49D42;font-weight:900;letter-spacing:.08em;font-size:13px">MUNDO LIVRE</div>
  <h2 style="margin:7px 0 8px;font-size:24px">Começar sem roteiro</h2>
  <p style="margin:0 0 16px;line-height:1.5;color:#BCC3C8">Escolha apenas o perfil da campanha e o ponto inicial. O Motor Barbara cria facções, NPCs, conflitos, rumores e oportunidades persistentes; nenhuma aventura é obrigatória.</p>
  <label style="display:grid;gap:6px;margin-bottom:12px"><span style="font-weight:800">Campanha</span><select id="sandbox-mode" style="min-height:46px;background:#252B2F;color:#fff;border:1px solid #697279;border-radius:7px;padding:8px"><option value="EOTE">Edge of the Empire</option><option value="AOR">Age of Rebellion</option><option value="MIXED">Mista · EotE + AoR</option></select></label>
  <label style="display:grid;gap:6px"><span style="font-weight:800">Ponto inicial</span><input id="sandbox-start" maxlength="120" value="Porto de Fronteira" placeholder="Ex.: Mos Eisley, Ord Mantell..." style="min-height:46px;background:#252B2F;color:#fff;border:1px solid #697279;border-radius:7px;padding:8px"></label>
  <div id="sandbox-setup-status" role="status" style="min-height:22px;margin:10px 0;color:#E5A28F"></div>
  <button id="sandbox-start-button" type="button" style="width:100%;min-height:50px;border:1px solid #D49D42;background:#652514;color:#fff;border-radius:7px;font-weight:900;font-size:16px">CRIAR MUNDO VIVO</button>
  <p style="font-size:12px;color:#8E979D;line-height:1.4;margin:12px 0 0">Seed inicial: 6 facções, 36 NPCs persistentes, 12 locais, relógios, mercados e oportunidades. Aventuras publicadas entram apenas como conteúdo opcional.</p>
 </div>`;
 document.body.appendChild(host);
 const select=host.querySelector('#sandbox-mode'),input=host.querySelector('#sandbox-start'),button=host.querySelector('#sandbox-start-button'),status=host.querySelector('#sandbox-setup-status');select.value=String(modeResult.data?.campaignMode||'EOTE');
 button.onclick=()=>{const point=String(input.value||'').trim();if(!point){status.textContent='Informe um ponto inicial.';return}button.disabled=true;status.textContent='Criando mundo persistente…';const m=bridgeResult('setCampaignMode',select.value);if(!m.ok){status.textContent=m.message||'Modo inválido.';button.disabled=false;return}const r=bridgeResult('initializeLivingSandbox',{startingPoint:point,regionLabel:`Região de ${point}`});if(!r.ok){status.textContent=r.message||'Falha ao criar mundo.';button.disabled=false;return}host.remove();setTimeout(()=>{relabelCreationForCampaign();enhanceSceneFromEngine()},0)};
}
function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay()}
let scheduled=false;const observer=new MutationObserver(()=>{if(scheduled)return;scheduled=true;requestAnimationFrame(()=>{scheduled=false;enhance()})});
observer.observe(document.documentElement,{subtree:true,childList:true});
window.addEventListener('popstate',()=>setTimeout(enhance,0));
window.addEventListener('DOMContentLoaded',enhance);setTimeout(enhance,0);
window.BraseiroFinalRuntime={version:'1.0.0',speakVisibleNarration:()=>speak(narrationText()),stopNarration:stop,refreshScene:enhanceSceneFromEngine,preferences:prefs};
})();
