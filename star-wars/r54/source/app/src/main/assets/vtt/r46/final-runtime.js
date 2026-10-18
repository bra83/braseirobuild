(function(){
'use strict';
const PREF_KEY='braseiro.starwars.ui.preferences.v1';
const defaults={textScale:'100',highContrast:false,reducedMotion:false,ttsEnabled:true,ttsRate:'1',ttsProvider:'android',geminiVoice:'Kore'};
let lastTtsPrewarmSceneKey='';
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
function nativeTtsCatalog(){
 const b=window.BraseiroTts;
 if(!b||typeof b.getCatalog!=='function')return {ok:false,engines:[]};
 try{return safeParse(b.getCatalog(),{ok:false,engines:[]})}catch{return {ok:false,engines:[]}}
}
function maybePrewarmNativeTtsForScene(){
 if(prefs.ttsProvider!=='android'||!document.querySelector('.scene-narrative-paper'))return;
 const b=window.BraseiroTts,st=nativeStatus();
 if(!b||typeof b.prewarm!=='function'||st.profile!=='kokoro')return;
 const key=currentSceneKey();if(!key||key===lastTtsPrewarmSceneKey)return;
 lastTtsPrewarmSceneKey=key;try{b.prewarm()}catch{}
}
function narrationText(){
 const host=document.querySelector('.scene-narrative-paper:not(.scene-narrative-reader) .scene-paper-copy');
 if(!host)return '';
 const blocks=[...host.querySelectorAll('p,li,blockquote')].map(el=>String(el.textContent||'').replace(/\s+/g,' ').trim()).filter(Boolean);
 return (blocks.length?blocks.join('\n\n'):String(host.textContent||'').replace(/\s+/g,' ').trim()).slice(0,12000);
}
function geminiStatus(){
 const b=window.BraseiroGemini;if(!b||typeof b.getStatus!=='function')return {available:false,configured:false};
 try{return {...safeParse(b.getStatus(),{}),available:true}}catch{return {available:true,configured:false}}
}
const PT_BR_SKILLS={actividad_criminal:'Atividade Criminosa',aguante:'Resistência',alerta:'Alerta / Vigilância',astronavegacion:'Astronavegação',atletismo:'Atletismo',callejeo:'Conhecimento das Ruas',carisma:'Carisma / Charme',coaccion:'Coerção',computadoras:'Computadores',conocimientos:'Conhecimentos',coordinacion:'Coordenação',disciplina:'Disciplina',engano:'Enganação',frialdad:'Frieza',liderazgo:'Liderança',mecanica:'Mecânica',medicina:'Medicina',negociacion:'Negociação',percepcion:'Percepção',pilotar:'Pilotagem',pilotar_vehiculo_espacial:'Pilotagem Espacial',pilotar_vehiculo_planetario:'Pilotagem Planetária',sigilo:'Furtividade',supervivencia:'Sobrevivência',armas_distancia_ligeras:'Armas de Distância Leves',armas_distancia_pesadas:'Armas de Distância Pesadas',armas_cuerpo_a_cuerpo:'Armas Corpo a Corpo',artilleria:'Artilharia',pelea:'Briga / Luta',bajos_fondos:'Submundo',borde_exterior:'Orla Exterior',educacion:'Educação',estrategia_militar:'Estratégia Militar',mundos_del_nucleo:'Mundos do Núcleo',saber_arcaico:'Conhecimento Arcaico',xenologia:'Xenologia'};
const PT_BR_CHARS={fortaleza:'Fortaleza',agilidad:'Agilidade',intelecto:'Intelecto',astucia:'Astúcia',voluntad:'Vontade',presencia:'Presença'};
const trSkill=k=>PT_BR_SKILLS[k]||(k?k.charAt(0).toUpperCase()+k.slice(1).replace(/_/g,' '):'Perícia');
const trChar=k=>PT_BR_CHARS[k]||k;
function speak(text){
 if(!prefs.ttsEnabled)return {ok:false,message:'TTS desativado em Ajustes.'};
 const clean=String(text||'').trim();
 if(!clean)return {ok:false,message:'Não há narração visível para ler.'};
 if(prefs.ttsProvider==='gemini'){
  const g=window.BraseiroGemini;
  if(g&&typeof g.speakTts==='function'){
   try{const r=safeParse(g.speakTts(clean,String(prefs.geminiVoice||'Kore')),{ok:false,message:'Resposta Gemini TTS inválida.'});if(r&&r.ok)return r;}catch{}
  }
 }
 const b=window.BraseiroTts;
 if(b&&typeof b.speak==='function'){
  try{const nativeResult=safeParse(b.speak(clean,String(prefs.ttsRate||'1')),{ok:false,message:'Resposta TTS inválida.'});if(nativeResult&&nativeResult.ok)return nativeResult;}catch{}
 }
 if('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window){
  try{window.speechSynthesis.cancel();const chunks=[];let rest=clean;const max=1800;while(rest.length>max){let cut=rest.lastIndexOf('. ',max);if(cut<700)cut=rest.lastIndexOf(' ',max);if(cut<1)cut=max;else cut+=1;chunks.push(rest.slice(0,cut).trim());rest=rest.slice(cut).trim()}if(rest)chunks.push(rest);const rate=Math.max(.6,Math.min(1.4,Number(prefs.ttsRate)||1));const play=i=>{if(i>=chunks.length)return;const u=new SpeechSynthesisUtterance(chunks[i]);u.lang='pt-BR';u.rate=rate;u.onend=()=>play(i+1);u.onerror=()=>play(i+1);window.speechSynthesis.speak(u)};play(0);return {ok:true,fallback:true,chunks:chunks.length}}catch{}
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
async function bridgeAsync(name,...args){
 const b=window.BraseiroGameBridge;
 if(!b||typeof b[name]!=='function')return {ok:false,error:'BRIDGE_UNAVAILABLE',message:'Motor Barbara indisponível.'};
 try{const r=await b[name](...args);return r&&typeof r==='object'?r:{ok:false,error:'INVALID_BRIDGE',message:'Resposta inválida do Motor Barbara.'}}catch(e){return {ok:false,error:'ENGINE_REJECTED',message:String(e&&e.message||e||'Falha no Motor Barbara.')}}
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
function difficultyLabel(value){
 const n=Math.max(0,Math.min(5,Number(value)||0));
 return ['Simples (0 dados de Dificuldade)','Fácil (1 dado de Dificuldade)','Média (2 dados de Dificuldade)','Difícil (3 dados de Dificuldade)','Desafiadora (4 dados de Dificuldade)','Formidável (5 dados de Dificuldade)'][n];
}

const NPC_TOKEN_PENDING_KEY='braseiro.starwars.pendingNpcToken.v1';
function npcTokenNative(){const b=window.BraseiroNpcTokens;return b&&typeof b==='object'?b:null}
function npcTokenStatus(id){
 const b=npcTokenNative();if(!b||typeof b.getTokenStatus!=='function')return {ok:false,hasToken:false,available:false};
 try{return {...safeParse(b.getTokenStatus(String(id||'')),{}),available:true}}catch{return {ok:false,hasToken:false,available:true}}
}
function participantEvidence(p){
 const name=String(p?.name||'').trim();if(!name)return '';
 const needle=name.toLocaleLowerCase('pt-BR');
 const texts=[narrationText(),String(document.querySelector('.scene-situation-paper p')?.textContent||'')].join(' ');
 const sentences=texts.split(/(?<=[.!?])\s+/).map(x=>x.replace(/\s+/g,' ').trim()).filter(Boolean);
 const exact=sentences.filter(x=>x.toLocaleLowerCase('pt-BR').includes(needle)).slice(0,4).join(' ');
 return exact.slice(0,1400);
}
function npcTokenPrompt(p){
 const facts=(Array.isArray(p?.visualFacts)?p.visualFacts:[]).map(x=>String(x||'').trim()).filter(Boolean);
 const tags=(Array.isArray(p?.visualTags)?p.visualTags:[]).map(x=>String(x||'').trim()).filter(Boolean);
 const evidence=participantEvidence(p);
 const details=[`Nome: ${String(p?.name||'').trim()}`];
 if(p?.species)details.push(`Espécie: ${p.species}`);
 if(p?.sex)details.push(`Sexo: ${p.sex}`);
 if(p?.gender)details.push(`Gênero: ${p.gender}`);
 if(p?.ageYears!==null&&p?.ageYears!==undefined)details.push(`Idade: ${p.ageYears} anos`);
 if(p?.origin)details.push(`Origem: ${p.origin}`);
 if(p?.career)details.push(`Carreira: ${p.career}`);
 if(p?.specialization)details.push(`Especialização: ${p.specialization}`);
 if(p?.occupation)details.push(`Ocupação: ${p.occupation}`);
 if(p?.appearance)details.push(`Aparência persistente: ${p.appearance}`);
 if(p?.meta)details.push(`Relação/função visível: ${String(p.meta).trim()}`);
 if(p?.status)details.push(`Estado visível: ${String(p.status).trim()}`);
 if(tags.length)details.push(`Marcadores conhecidos: ${tags.join(', ')}`);
 if(facts.length)details.push(`Fatos públicos: ${facts.join(' | ')}`);
 if(evidence)details.push(`Descrição desta pessoa na cena atual: ${evidence}`);
 return [
  'Crie um TOKEN DE PERSONAGEM único e permanente para um VTT de STAR WARS — EDGE OF THE GALAXY.',
  'O token deve mostrar SOMENTE esta pessoa, do TORSO PARA CIMA, com o ROSTO inteiro claramente visível e reconhecível. Enquadramento vertical 3:4, personagem centralizado, expressão natural coerente com a situação.',
  'Estética: concept art cinematográfica Star Wars, retro-industrial, materiais e roupas plausíveis do universo, acabamento limpo para uso como token. Fundo preto sólido ou muito escuro e neutro para recorte. Sem cenário complexo.',
  'Não inclua outra pessoa, clone, multidão, criatura extra, nave, texto, letras, nome escrito, moldura, ficha, HUD, ícones ou marca d’água.',
  'Esta imagem será a IDENTIDADE VISUAL EXCLUSIVA E PERMANENTE desta pessoa no VTT. Não misture características de outros personagens.',
  'Use fielmente qualquer espécie, uniforme, roupa, idade aparente, anatomia, cor, cicatriz, acessório ou outra característica que esteja explicitamente descrita abaixo. Se algum detalhe físico não estiver definido, escolha UMA solução plausível e coerente e mantenha essa aparência como a identidade definitiva deste indivíduo.',
  `DADOS VISÍVEIS DO PERSONAGEM:\n${details.join('\n')}`,
  'Resultado esperado: um único retrato/token de meio-corpo, rosto nítido, leitura forte em tamanho pequeno, sem qualquer texto.'
 ].join('\n\n');
}
function pendingNpcTokenId(){try{return localStorage.getItem(NPC_TOKEN_PENDING_KEY)||''}catch{return ''}}
function setPendingNpcTokenId(id){try{id?localStorage.setItem(NPC_TOKEN_PENDING_KEY,String(id)):localStorage.removeItem(NPC_TOKEN_PENDING_KEY)}catch{}}
function bindPersistentNpcToken(slot,p,frame,strong,small){
 if(!p?.id||p?.kind==='vehicle'){slot.dataset.persistentNpcToken='0';return}
 const b=npcTokenNative(), g=window.BraseiroGemini;
 const renderTokenIfAvailable=()=>{
  const latest=npcTokenStatus(p.id);
  if(latest?.hasToken&&latest.url){
   slot.dataset.persistentNpcToken='1';slot.classList.add('has-persistent-token');
   if(frame){frame.classList.remove('is-empty');frame.setAttribute('aria-label',`Token permanente de ${p.name}`);frame.replaceChildren(Object.assign(document.createElement('img'),{className:'r54-p23-npc-token-image',src:latest.url,alt:`Token permanente de ${p.name}`}))}
   if(strong){strong.dataset.npcToken='locked';strong.title='Toque para conversar com este NPC via Gemini.'}
   if(pendingNpcTokenId()===String(p.id))setPendingNpcTokenId('');
   return true;
  }
  return false;
 };
 const hasImg=renderTokenIfAvailable();
 slot.classList.add('npc-token-action');slot.setAttribute('role','button');slot.tabIndex=0;slot.setAttribute('aria-label',`Interagir com ${p.name} via Gemini`);
 const activate=()=>{
  const npcName=String(p.name||'NPC').trim();
  const role=String(p.meta||p.occupation||p.career||'personagem').trim();
  const species=p.species?`Espécie: ${p.species}. `:'';
  const sceneTitle=document.querySelector('.scene-header h1, .scene-concept-shell h1')?.textContent||'Cena Star Wars';
  const situation=document.querySelector('.scene-situation-paper p')?.textContent||'';
  const prompt=[
   `[INTERAÇÃO_NPC_STAR_WARS] O jogador está interagindo diretamente com ${npcName} (${role}).`,
   `${species}Local/Cena: ${sceneTitle}.`,
   situation?`Situação imediata: ${situation}`:'',
   p.appearance?`Aparência visual: ${p.appearance}`:'',
   `Atue como o Mestre de Jogo de Star Wars (Fronteira do Império / Era da Rebelião) interpretando a reação, diálogo e atitude de ${npcName} em português do Brasil com a devida atmosfera da saga. Se for solicitado um retrato/token, gere um concept art cinematográfico de meio-corpo focado no rosto de ${npcName}.`
  ].filter(Boolean).join('\n\n');

  let opened=false;
  if(b&&typeof b.openGeminiTokenPrompt==='function'&&p.id){
   try{
    const r=safeParse(b.openGeminiTokenPrompt(String(p.id),prompt),{});
    if(r?.ok){opened=true;setPendingNpcTokenId(p.id);}
   }catch(_){}
  }
  if(!opened&&g&&typeof g.openGeminiWithPrompt==='function'){
   try{
    const r=safeParse(g.openGeminiWithPrompt(prompt),{});
    if(r?.ok)opened=true;
   }catch(_){}
  }
  if(!opened&&g&&typeof g.openGeminiScenePrompt==='function'){
   try{
    const r=safeParse(g.openGeminiScenePrompt(prompt,currentSceneKey()),{});
    if(r?.ok)opened=true;
   }catch(_){}
  }
  if(small){
   small.textContent=opened?`Gemini aberto · falando com ${npcName}`:`Prompt gerado para ${npcName}`;
  }
 };
 slot.onclick=(e)=>{e.preventDefault();e.stopPropagation();activate()};
 slot.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();activate()}};
 if(strong){strong.style.cursor='pointer';strong.title=`Interagir com ${p.name} via Gemini`}
 if(frame){frame.style.cursor='pointer';frame.title=`Interagir com ${p.name} via Gemini`}
 if(!hasImg&&small&&!small.textContent)small.textContent=p.meta||p.status||'Toque para interagir';
}
function sceneActionStatus(root,message,error=false){
 const composer=root?.querySelector('.scene-composer');if(!composer)return;
 let out=composer.querySelector('.final-scene-action-status');if(!out){out=document.createElement('div');out.className='final-scene-action-status';out.setAttribute('role','status');out.style.cssText='margin-top:6px;font-size:.72rem;line-height:1.25';composer.appendChild(out)}
 out.textContent=String(message||'');out.style.color=error?'#E5A28F':'#9CCBDA';
}
async function submitSuggestedAction(root,slot,action){
 if(slot.dataset.masterSending==='1')return;slot.dataset.masterSending='1';slot.setAttribute('aria-busy','true');const small=slot.querySelector('small'),previous=small?.textContent||'';if(small)small.textContent='Enviando ao Mestre…';
 const textarea=root.querySelector('.scene-composer textarea');sceneActionStatus(root,'Ação enviada ao Mestre; aguardando resposta…');
 const pub=publishedAdventureView();
 if(pub?.active&&Array.isArray(pub.node?.choices)){
  const matchedChoice=pub.node.choices.find(c=>(c.label||c.title)===action);
  if(matchedChoice&&window.BraseiroPublishedActionClient){
   const cr=window.BraseiroPublishedActionClient.execute('choice',matchedChoice.id);
   if(cr?.ok){
    window.dispatchEvent(new Event('braseiro:r21-state-changed'));
    sceneActionStatus(root,'Decisão da aventura executada.');
    slot.dataset.masterSending='0';slot.removeAttribute('aria-busy');if(small)small.textContent=previous||'Sugestão do Mestre';
    setTimeout(()=>{enhance();if(masterNarrationAvailable())void requestPublishedMasterNarration(`consequência de ${action}`)},50);
    return;
   }
  }
 }
 const result=await bridgeAsync('submitPlayerIntent',action);
 slot.dataset.masterSending='0';slot.removeAttribute('aria-busy');if(small)small.textContent=previous||'Sugestão do Mestre';
 if(!result?.ok){sceneActionStatus(root,result?.message||'O Mestre recusou a ação.',true);return}
 sceneActionStatus(root,'Ação entregue ao Mestre.');setTimeout(enhanceSceneFromEngine,0);
}
async function resolveSuggestedCheckWithDice(root,c,btn,out){
 btn.disabled=true;btn.setAttribute('aria-busy','true');const skill=skillLabel(c.skillKey),difficulty=difficultyLabel(c.difficulty);out.textContent=`Preparando ${skill} · ${difficulty}…`;
 const resolved=bridgeResult('resolveSuggestedMasterCheck');
 if(!resolved?.ok){out.textContent=resolved?.message||'Teste rejeitado pelo motor.';btn.disabled=false;btn.removeAttribute('aria-busy');return}
 const check=resolved.data?.check||resolved.data||{},title=`${skill} · ${difficulty}`,summary=checkSummary(check)||'Teste resolvido.';
 out.textContent=`${skill} · ${summary}`;out.classList.add('is-result');
 try{
  const visual=await boundedVisualRoll(check,{title,status:'Resultado mecânico já registrado.'});
  if(visual?.status)visual.status.textContent='Resultado mecânico registrado.';
  if(masterNarrationAvailable()){
   out.textContent=`${skill} · ${summary} · Mestre narrando a consequência…`;
   const masterText=`[RESULTADO_DE_TESTE] O teste solicitado pelo Mestre já foi resolvido pelo motor. Perícia: ${skill}. Dificuldade: ${difficulty}. Resultado público: ${summary}. Narre as consequências em português do Brasil sem refazer a rolagem, sem revelar informação oculta e sem decidir pelo personagem.`;
   const sent=await bridgeAsync('submitPlayerIntent',masterText);
   out.textContent=sent?.ok?`${skill} · ${summary} · Consequência narrada pelo Mestre.`:`${skill} · ${summary} · Resultado preservado; Mestre indisponível.`;
   if(sent?.ok)setTimeout(enhanceSceneFromEngine,0);
  }else out.textContent=`${skill} · ${summary} · Resultado registrado pelo Motor Barbara.`;
  setTimeout(()=>visual?.close?.(),1300);
 }catch(err){out.textContent=`${skill} · ${summary} · A animação falhou, mas o resultado foi registrado.`}finally{btn.disabled=false;btn.removeAttribute('aria-busy')}
}
function enhanceSceneFromEngine(){
 const root=document.querySelector('.scene-concept-shell');if(!root)return;
 const view=playerView();if(!view)return;
 const situation=root.querySelector('.scene-situation-paper p');if(situation&&view.situation)situation.textContent=view.situation;
 const participantSlots=[...root.querySelectorAll('.scene-participant-slot')];
 let participants=Array.isArray(view.participants)&&view.participants.length?[...view.participants]:[];
 if(!participants.length){
  const pub=publishedAdventureView();
  if(Array.isArray(pub?.npcCards)&&pub.npcCards.length){
   participants=pub.npcCards.map(c=>({
    id:c.id,name:c.name,kind:'npc',tokenEligible:true,
    meta:[c.species,c.occupation].filter(Boolean).join(' · ')||c.relation||'Presente',
    status:c.status||'Visível',species:c.species,occupation:c.occupation,appearance:c.appearance
   }));
  }
 }
 if(!participants.length){
  const st=currentState();
  const activeAdv=activePublishedAdventureId();
  if(activeAdv==='under-black-sun'||st?.publishedAdventure?.id==='under-black-sun'){
   participants=[
    {id:'npc_chopper',name:'Chopper',kind:'npc',tokenEligible:true,meta:'Besalisk · Contato dos Pykes',status:'Oficina ativa',species:'Besalisk',career:'Mecânico',appearance:'Besalisk robusto de quatro braços, avental de solda e olhar atento.'},
    {id:'npc_kaato',name:'Kaa’to Leeachos',kind:'npc',tokenEligible:true,meta:'Nikto · Alvo da caçada',status:'Em fuga',species:'Kadas’sa’Nikto',career:'Caçador de recompensas',appearance:'Nikto verde com cicatrizes, prótese na perna e equipamento de caça.'}
   ];
  }else if(activeAdv==='beyond-the-rim'||st?.publishedAdventure?.id==='beyond-the-rim'){
   participants=[
    {id:'npc_reom',name:'Reom',kind:'npc',tokenEligible:true,meta:'IsoTech · Empregador',status:'Estação A Roda',species:'Humano',career:'Representante corporativo',appearance:'Executivo da IsoTech com túnica formal de viagem e datapad seguro.'},
    {id:'npc_it3po',name:'IT-3PO',kind:'npc',tokenEligible:true,meta:'Droide de protocolo · Guia',status:'Aguardando',species:'Droide',career:'Protocolo',appearance:'Droide de protocolo dourado-fosco com insígnia da IsoTech.'}
   ];
  }else{
   const locId=st?.scene?.locationId;
   const allNpcs=Object.values(st?.world?.npcs||{});
   const locNpcs=allNpcs.filter(n=>(locId&&n.locationId===locId)||n.knownToPlayer);
   if(locNpcs.length){
    participants=locNpcs.slice(0,3).map(n=>({
     id:n.id,name:n.name,kind:'npc',tokenEligible:true,
     meta:[n.species,n.occupation||n.career].filter(Boolean).join(' · ')||n.relation||'NPC',
     status:n.status||'Visível',species:n.species,career:n.career,occupation:n.occupation,appearance:n.appearance
    }));
   }
  }
 }
 participantSlots.forEach((slot,i)=>{
  const p=participants[i],frame=slot.querySelector('.scene-participant-frame'),strong=slot.querySelector('strong'),small=slot.querySelector('small');
  if(p){
   slot.dataset.participantId=String(p.id||'');slot.dataset.participantKind=String(p.kind||'npc');
   if(frame){frame.classList.remove('is-empty');frame.textContent=initials(p.name);frame.setAttribute('aria-label',`Participante: ${p.name}`)}
   if(strong)strong.textContent=p.name;if(small)small.textContent=p.meta||p.status||'Visível';
   bindPersistentNpcToken(slot,p,frame,strong,small);
  }else{
   delete slot.dataset.participantId;delete slot.dataset.participantKind;delete slot.dataset.persistentNpcToken;slot.classList.remove('has-persistent-token','npc-token-action');slot.onclick=null;slot.onkeydown=null;slot.removeAttribute('role');slot.removeAttribute('tabindex');
   if(frame){frame.classList.add('is-empty');frame.innerHTML='<span class="scene-participant-empty-mark" aria-hidden="true"></span>';frame.removeAttribute('aria-label')}
   if(strong)strong.textContent='—';if(small)small.textContent='Aguardando participante';
  }
 });
 const actionSlots=[...root.querySelectorAll('.scene-action-slot')];
 let actions=Array.isArray(view.suggestedActions)&&view.suggestedActions.length?[...view.suggestedActions]:[];
 if(!actions.length){
  const pub=publishedAdventureView();
  if(Array.isArray(pub?.node?.choices)&&pub.node.choices.length){
   actions=pub.node.choices.map(c=>c.label||c.title||String(c)).slice(0,6);
  }else if(Array.isArray(pub?.suggestedActions)&&pub.suggestedActions.length){
   actions=pub.suggestedActions.slice(0,6);
  }
 }
 if(!actions.length){
  const st=currentState();
  const activeAdv=activePublishedAdventureId();
  if(activeAdv==='under-black-sun'||st?.publishedAdventure?.id==='under-black-sun'){
   actions=[
    'Assumir os controles do speeder e despistar perseguidores',
    'Fazer manobra evasiva em direção à oficina de Chopper',
    'Abrir fogo defensivo contra os swoops da Black Sun'
   ];
  }else if(activeAdv==='beyond-the-rim'||st?.publishedAdventure?.id==='beyond-the-rim'){
   actions=[
    'Procurar os escritórios da IsoTech na estação A Roda',
    'Investigar rumores sobre a Sa Nalaor nos hangares',
    'Inspecionar a nave e checar os sistemas de hipermotor'
   ];
  }else{
   actions=[
    'Examinar o local e verificar rotas de fuga ou coberturas',
    'Consultar o datapad sobre facções e oportunidades locais',
    'Abordar contatos locais ou buscar informações'
   ];
  }
 }
 actionSlots.forEach((slot,i)=>{
  const action=actions[i],strong=slot.querySelector('strong'),small=slot.querySelector('small'),end=slot.querySelector('.scene-action-empty-end');
  if(action){
   slot.classList.remove('is-empty');slot.classList.add('is-live');slot.setAttribute('role','button');slot.tabIndex=0;slot.setAttribute('aria-label',`Usar ação sugerida: ${action}`);
   if(strong)strong.textContent=action;if(small)small.textContent='Sugestão do Mestre';if(end){end.textContent='›';end.classList.add('scene-action-chevron')}
   const activate=()=>{void submitSuggestedAction(root,slot,action)};
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
   checkBox.innerHTML=`<div><strong>${skillLabel(c.skillKey)}</strong><small>${c.reason||'Teste sugerido pelo Mestre'} · Dificuldade: ${difficultyLabel(diff)}${chal?` · ${chal} melhoria(s) para Desafio`:''}${boost?` · ${boost} Bônus`:''}${setback?` · ${setback} Contratempo`:''}</small></div><button type="button">ROLAR DADOS</button><output aria-live="polite">Toque em ROLAR DADOS para ver a rolagem.</output>`;
   const btn=checkBox.querySelector('button'),out=checkBox.querySelector('output');
   btn.onclick=()=>{void resolveSuggestedCheckWithDice(root,c,btn,out)};
  }else if(checkBox){
   const last=view.lastCheck;checkBox.innerHTML=last?`<div><strong>Último teste</strong><small>${checkSummary(last)}</small></div>`:'';
   if(!last)checkBox.remove();
  }
 }
}
function enhanceTtsSettings(){
 const card=cardByTitle('Narração / voz');if(!card||card.dataset.finalEnhanced==='1')return;
 card.dataset.finalEnhanced='1';const content=card.children[1];if(!content)return;
 content.innerHTML=`<div class="final-settings-grid">
 <div class="final-setting-row"><span>TTS da narração</span><input id="final-tts-enabled" type="checkbox" aria-label="Ativar TTS da narração"></div>
 <div class="final-setting-row"><label for="final-tts-provider">Provedor</label><select id="final-tts-provider"><option value="android">Android TTS / Kokoro local</option><option value="gemini">Gemini TTS</option></select></div>
 <div class="final-setting-row" id="final-tts-engine-row"><label for="final-tts-engine">Motor Android</label><select id="final-tts-engine"></select></div>
 <div class="final-setting-row" id="final-tts-voice-row"><label for="final-tts-voice">Voz do motor</label><select id="final-tts-voice"></select></div>
 <div class="final-setting-row"><label for="final-tts-rate" id="final-tts-rate-label">Velocidade Android</label><select id="final-tts-rate"><option value="0.8">0,8×</option><option value="1">1,0×</option><option value="1.1">1,1×</option><option value="1.2">1,2×</option><option value="1.4">1,4×</option></select></div>
 <div class="final-setting-row" id="final-gemini-voice-row"><label for="final-gemini-voice">Voz Gemini</label><select id="final-gemini-voice"><option>Kore</option><option>Aoede</option><option>Charon</option><option>Fenrir</option><option>Puck</option></select></div>
 <div class="final-setting-status" id="final-tts-profile"></div>
 <div class="final-setting-actions"><button type="button" id="final-tts-test">Testar voz</button><button type="button" id="final-tts-stop">Parar</button></div>
 <div class="final-setting-status" id="final-tts-status"></div></div>`;
 const enabled=content.querySelector('#final-tts-enabled'),provider=content.querySelector('#final-tts-provider'),rate=content.querySelector('#final-tts-rate'),voice=content.querySelector('#final-gemini-voice'),status=content.querySelector('#final-tts-status'),profile=content.querySelector('#final-tts-profile'),engine=content.querySelector('#final-tts-engine'),nativeVoice=content.querySelector('#final-tts-voice'),engineRow=content.querySelector('#final-tts-engine-row'),nativeVoiceRow=content.querySelector('#final-tts-voice-row'),geminiVoiceRow=content.querySelector('#final-gemini-voice-row'),rateLabel=content.querySelector('#final-tts-rate-label');
 enabled.checked=!!prefs.ttsEnabled;provider.value=String(prefs.ttsProvider||'android');rate.value=String(prefs.ttsRate||'1');voice.value=String(prefs.geminiVoice||'Kore');
 const setOptions=(select,items,value,placeholder)=>{select.replaceChildren();if(!items.length){const o=document.createElement('option');o.value='';o.textContent=placeholder||'Nenhuma opção';select.appendChild(o);select.disabled=true;return}select.disabled=false;for(const item of items){const o=document.createElement('option');o.value=item.value;o.textContent=item.label;select.appendChild(o)}if(items.some(x=>x.value===value))select.value=value;else select.selectedIndex=0};
 const rebuildNative=()=>{
  const st=nativeStatus(),cat=nativeTtsCatalog(),engines=Array.isArray(cat.engines)?cat.engines:[];
  setOptions(engine,engines.map(e=>({value:String(e.packageName||''),label:`${e.label||e.packageName}${e.isDefault?' · padrão':''}`})),String(st.enginePackage||cat.activeEnginePackage||''),'Carregando motores…');
  const active=engines.find(e=>String(e.packageName||'')===String(st.enginePackage||cat.activeEnginePackage||''));
  const voices=Array.isArray(active?.voices)?active.voices:[];
  setOptions(nativeVoice,[{value:'',label:'Automática / pt-BR'},...voices.map(v=>({value:String(v.name||''),label:`${v.locale||'sem idioma'} · ${v.name||'voz'}${v.networkRequired?' · rede':''}`}))],String(st.voiceName||cat.activeVoiceName||''),'Carregando vozes…');
  const neural=st.profile==='kokoro';rateLabel.textContent=neural?'Velocidade Kokoro':'Velocidade Android';
  profile.textContent=neural?'Braseiro Kokoro PT-BR · neural · blocos naturais ~450 (250–650) · próximo bloco somente após onDone real.':'Perfil Android normal · Samsung/Google preservam a fila TTS padrão.';
  return st;
 };
 const refreshStatus=()=>{
  const gst=geminiStatus(),st=rebuildNative(),isGemini=provider.value==='gemini';
  engineRow.style.display=isGemini?'none':'';nativeVoiceRow.style.display=isGemini?'none':'';geminiVoiceRow.style.display=isGemini?'':'none';profile.style.display=isGemini?'none':'';
  status.textContent=isGemini?(gst.available?(gst.configured?`Gemini TTS pronto · ${gst.ttsModel||'modelo configurado'}${gst.ttsPendingChunks?` · ${gst.ttsPendingChunks} bloco(s) preparando`:''}`:'Gemini disponível; configure a chave API na aba Gemini.'):'Bridge Gemini indisponível.'):(st.available?`${st.detail||'Android TTS pronto.'}${st.voiceName?` · voz ${st.voiceName}`:''}`:(st.detail||'Fallback do WebView será usado se disponível.'));
 };
 refreshStatus();
 try{window.BraseiroTts?.refreshCatalog?.()}catch{}
 let catalogPolls=0;const catalogTimer=setInterval(()=>{catalogPolls++;if(!document.body.contains(content)){clearInterval(catalogTimer);return}refreshStatus();const cat=nativeTtsCatalog();if((cat.engines||[]).length&&cat.engines.every(e=>e.status!=='probing'))clearInterval(catalogTimer);else if(catalogPolls>16)clearInterval(catalogTimer)},500);
 enabled.addEventListener('change',()=>{prefs.ttsEnabled=enabled.checked;save()});
 provider.addEventListener('change',()=>{prefs.ttsProvider=provider.value;save();refreshStatus()});
 rate.addEventListener('change',()=>{prefs.ttsRate=rate.value;save()});
 voice.addEventListener('change',()=>{prefs.geminiVoice=voice.value;save()});
 engine.addEventListener('change',()=>{const b=window.BraseiroTts;if(!b||typeof b.selectEngine!=='function')return;status.textContent='Inicializando motor TTS…';try{const r=safeParse(b.selectEngine(engine.value),{});if(!r.ok)status.textContent=r.message||'Falha ao selecionar motor.'}catch{status.textContent='Falha ao selecionar motor.'}setTimeout(()=>{try{b.refreshCatalog?.()}catch{}refreshStatus()},700)});
 nativeVoice.addEventListener('change',()=>{const b=window.BraseiroTts;if(!b||typeof b.selectVoice!=='function')return;try{const r=safeParse(b.selectVoice(nativeVoice.value),{});status.textContent=r.ok?'Voz selecionada.':(r.message||'Falha ao selecionar voz.');setTimeout(()=>{try{b.prewarm?.()}catch{}refreshStatus()},350)}catch{status.textContent='Falha ao selecionar voz.'}});
 content.querySelector('#final-tts-test').addEventListener('click',()=>{const r=speak('A porta range. Há marcas no chão. Algo se move no corredor.');status.textContent=r.ok?(provider.value==='gemini'?'Gemini TTS iniciado.':`Voz iniciada${r.chunks?` · ${r.chunks} bloco(s)`:''}.`):(r.message||'Não foi possível reproduzir a voz.')});
 content.querySelector('#final-tts-stop').addEventListener('click',()=>{stop();status.textContent='Voz interrompida.'});
 const stateCard=cardByTitle('Estado');if(stateCard){const body=stateCard.children[1];if(body)body.textContent='Somente o texto visível da Narração do Mestre é lido. Android lista dinamicamente os motores/vozes instalados; Kokoro/Sherpa usa perfil neural encadeado por onDone. Gemini TTS usa a chave configurada no aparelho.'}
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
function publishedAdventureCatalogView(){const r=bridgeResult('publishedAdventureCatalog');return r&&r.ok&&Array.isArray(r.data)?r.data:[]}
function activePublishedAdventureId(){const st=currentState();const id=String(st?.world?.publishedAdventure?.id||'').trim();return id||null}
function publishedAdventureView(){const id=activePublishedAdventureId();if(!id)return null;const r=bridgeResult('publishedAdventureStatus',id);return r&&r.ok?r.data:null}
function adventureStarted(){
 const published=publishedAdventureView();if(published?.active||published?.completed)return true;
 const st=currentState();return st?.session?.status==='ativa';
}
function masterNarrationAvailable(){
 const st=currentState();if(st?.settings?.masterProvider==='http')return true;
 try{return Boolean(geminiStatus()?.configured)}catch{return false}
}
async function requestPublishedMasterNarration(reason='cena atual'){
 if(!masterNarrationAvailable())return {ok:false,code:'MASTER_LOCAL_ONLY',message:'Mestre local ativo; Gemini/remoto não configurado.'};
 const status=publishedAdventureView();if(!status?.active)return {ok:false,code:'NO_PUBLISHED_ADVENTURE',message:'Nenhuma aventura publicada ativa.'};
 const node=status.node||{},tracker=status.tracker||{};
 const title=String(node.title||'Cena atual'),summary=String(node.summary||'').trim(),objective=String(tracker.objective||'').trim();
 const intent=`[AVENTURA_PUBLICADA] O jogador iniciou/continuou explicitamente a aventura publicada. Motivo: ${reason}. Cena pública atual: ${title}. ${summary?`Resumo público: ${summary}.`:''} ${objective?`Objetivo público: ${objective}.`:''} Narre esta cena em português do Brasil com riqueza suficiente para jogar: 8 a 14 parágrafos, normalmente 900 a 1400 palavras em abertura/cena principal. Use apenas fatos já públicos do estado e do contexto autorizado pelo Motor Barbara; preserve anti-spoiler, não revele nós futuros, soluções ocultas nem consequências ainda desconhecidas. Não aja pelo personagem. Termine com uma situação clara e espaço para decisão do jogador.`;
 const result=await bridgeAsync('submitPlayerIntent',intent);
 if(result?.ok)setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();badge()},0);
 return result;
}
function boundedVisualRoll(check,options={},timeoutMs=5200){
 if(!window.__R54DicePhysics?.roll)return Promise.resolve(null);
 let timer=null;const timeout=new Promise(resolve=>{timer=setTimeout(()=>resolve(null),timeoutMs)});
 return Promise.race([Promise.resolve(window.__R54DicePhysics.roll(check,options)).catch(()=>null),timeout]).finally(()=>{if(timer)clearTimeout(timer)});
}
async function startBarbaraOpening(){
 // R54_DEVICE_P0_START: local bootstrap is authoritative and never waits for network.
 const prep=bridgeResult('startSandboxSession',{});if(!prep?.ok)return prep;
 navigateRoute('00');
 const game=globalThis.BraseiroGameBridge;
 if(game&&typeof game.submitPlayerIntent==='function'){
  const req=prep.data?.masterOpeningRequest||{};const location=String(req.locationName||'o ponto inicial');
  const opportunity=String(req.opportunity?.summary||req.opportunity?.title||'').trim();
  const npc=String(req.npc?.name||'').trim();const npcProfile=req.npc?[req.npc.species,req.npc.career,req.npc.specialization,req.npc.occupation].filter(Boolean).join(' · '):'';const faction=String(req.faction?.name||'').trim();const angle=String(req.openingAngle||'').trim();
  const facts=[`Local inicial: ${location}.`,opportunity?`Situação ativa: ${opportunity}.`:'',npc?`NPC disponível no local, use somente se fizer sentido na cena: ${npc}${npcProfile?` (${npcProfile})`:''}.`:'',faction?`Facção relevante: ${faction}.`:'',angle?`Ângulo narrativo desta abertura: ${angle}.`:''].filter(Boolean).join(' ');
  const intentText=`[ABERTURA_BARBARA] Inicie o sandbox agora como Mestre Barbara. ${facts} Gere uma abertura diferente das anteriores mesmo quando o local inicial se repetir, mas permaneça estritamente coerente com os fatos do mundo vivo. Não invente uma missão obrigatória. Não resuma, não escolha nem aja pelo personagem. Narre a situação em português do Brasil conforme o contrato do Mestre e termine deixando a decisão ao jogador.`;
  try{Promise.resolve(game.submitPlayerIntent(intentText)).catch(()=>{});}catch(_){/* offline: campaign remains started */}
 }
 setTimeout(()=>{navigateRoute('00');enhance()},50);
 return prep;
}
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
const START_POINTS=[{"name":"Coruscant","x":53.4,"y":51.2,"desc":"ecumenopolis","region":"Core Worlds","grid":"L10","key":"coruscant"},{"name":"Alderaan","x":57.8,"y":54.7,"desc":"Core Worlds","region":"Core Worlds","grid":"M10","key":"alderaan"},{"name":"Corellia","x":61.7,"y":50.4,"desc":"Forests, hills, plains","region":"Core Worlds","grid":"N10","key":"corellia"},{"name":"Kashyyyk","x":71.1,"y":46.7,"desc":"Mid Rim","region":"Mid Rim","grid":"Q10","key":"kashyyyk"},{"name":"Naboo","x":76.5,"y":77.1,"desc":"Mid Rim","region":"Mid Rim","grid":"R16","key":"naboo"},{"name":"Tatooine","x":73.2,"y":78.4,"desc":"desert","region":"Outer Rim","grid":"R16","key":"tatooine"},{"name":"Hoth","x":43.6,"y":83.9,"desc":"Outer Rim","region":"Outer Rim","grid":"J18","key":"hoth"},{"name":"Bespin","x":45.1,"y":17.8,"desc":"gas clouds","region":"Outer Rim","grid":"J4","key":"bespin"},{"name":"Dagobah","x":59.0,"y":87.8,"desc":"Outer Rim","region":"Outer Rim","grid":"M18","key":"dagobah"},{"name":"Mustafar","x":51.0,"y":91.1,"desc":"Outer Rim","region":"Outer Rim","grid":"K19","key":"mustafar"},{"name":"Endor","x":37.7,"y":75.6,"desc":"Outer Rim","region":"Outer Rim","grid":"H16","key":"endor"},{"name":"Geonosis","x":78.1,"y":82.2,"desc":"Outer Rim","region":"Outer Rim","grid":"R17","key":"geonosis"},{"name":"Dantooine","x":48.2,"y":16.2,"desc":"Outer Rim","region":"Outer Rim","grid":"K4","key":"dantooine"},{"name":"Mandalore","x":66.8,"y":35.5,"desc":"Outer Rim","region":"Outer Rim","grid":"P7","key":"mandalore"},{"name":"Kessel","x":82.5,"y":48.6,"desc":"alkali flats, mountains, caves","region":"Outer Rim","grid":"S10","key":"kessel"},{"name":"Jakku","x":31.3,"y":61.5,"desc":"Unknown Regions","region":"Unknown Regions","grid":"G13","key":"jakku"},{"name":"Ahch-To","x":29.2,"y":66.2,"desc":"Unknown Regions","region":"Unknown Regions","grid":"F14","key":"ahch_to"},{"name":"Rakata Prime","x":29.8,"y":49.6,"desc":"Unknown Regions","region":"Unknown Regions","grid":"G11","key":"rakata_prime"},{"name":"Mon Calamari","x":94.7,"y":29.4,"desc":"Outer Rim","region":"Outer Rim","grid":"V6","key":"mon_calamari"},{"name":"Ryloth","x":77.1,"y":84.4,"desc":"caves, deserts, mountains, plains","region":"Outer Rim","grid":"R17","key":"ryloth"},{"name":"Montross","x":43.5,"y":9.6,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"montross"},{"name":"Sernpidal","x":52.7,"y":8.1,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"sernpidal"},{"name":"Birgis","x":54.9,"y":10.6,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"birgis"},{"name":"Maltha Obex","x":59.9,"y":12.4,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"maltha_obex"},{"name":"Kalee","x":39.9,"y":14.5,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"kalee"},{"name":"Bimmiel","x":56.6,"y":14.8,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"bimmiel"},{"name":"Kadavo","x":83.1,"y":15.4,"desc":"Corporate Sector","region":"Corporate Sector","grid":"","key":"kadavo"},{"name":"Naraka","x":74.6,"y":16.1,"desc":"Corporate Sector","region":"Corporate Sector","grid":"","key":"naraka"},{"name":"Rekkiad","x":85.2,"y":16.3,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"rekkiad"},{"name":"Valce","x":40.4,"y":16.7,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"valce"},{"name":"Akuria","x":55.0,"y":18.3,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"akuria"},{"name":"Ord Radama","x":72.3,"y":18.3,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"ord_radama"},{"name":"Dromund Kaas","x":78.2,"y":18.2,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"dromund_kaas"},{"name":"Karkaris","x":83.5,"y":18.4,"desc":"Corporate Sector","region":"Corporate Sector","grid":"","key":"karkaris"},{"name":"Toofa","x":82.8,"y":19.4,"desc":"Corporate Sector","region":"Corporate Sector","grid":"","key":"toofa"},{"name":"Phaeda","x":51.7,"y":24.1,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"phaeda"},{"name":"Raydonia","x":65.0,"y":23.9,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"raydonia"},{"name":"Munto Codru","x":88.8,"y":24.1,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"munto_codru"},{"name":"Ossus","x":79.6,"y":26.4,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"ossus"},{"name":"Lothal","x":67.0,"y":28.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"lothal"},{"name":"Parnassos","x":36.0,"y":28.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"parnassos"},{"name":"Bogano","x":79.0,"y":31.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"bogano"},{"name":"Garel","x":72.0,"y":31.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"garel"},{"name":"Paatus","x":25.0,"y":31.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"paatus"},{"name":"Crustai","x":34.0,"y":33.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"crustai"},{"name":"Exegol","x":24.0,"y":34.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"exegol"},{"name":"Karthon","x":69.0,"y":34.0,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"karthon"},{"name":"Toongl","x":85.0,"y":35.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"toongl"},{"name":"Targonn","x":91.0,"y":35.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"targonn"},{"name":"Avidich","x":20.0,"y":38.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"avidich"},{"name":"Naporar","x":27.0,"y":38.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"naporar"},{"name":"Brask Oto","x":32.0,"y":38.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"brask_oto"},{"name":"Schesa","x":27.0,"y":42.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"schesa"},{"name":"Gand","x":83.0,"y":42.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"gand"},{"name":"Centrality","x":91.0,"y":42.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"centrality"},{"name":"Csilla","x":23.0,"y":45.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"csilla"},{"name":"Jamiron","x":27.0,"y":45.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"jamiron"},{"name":"Shantipole","x":80.0,"y":45.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"shantipole"},{"name":"Renatasia","x":90.0,"y":45.0,"desc":"Outer Rim","region":"Outer Rim","grid":"","key":"renatasia"},{"name":"Kinoss","x":22.0,"y":48.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"kinoss"},{"name":"Pesfari","x":31.0,"y":48.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"pesfari"},{"name":"Jetha","x":43.0,"y":50.0,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"jetha"},{"name":"Vakkar","x":46.0,"y":50.0,"desc":"Mid Rim","region":"Mid Rim","grid":"","key":"vakkar"},{"name":"Borleias","x":57.0,"y":51.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"borleias"},{"name":"Castell","x":66.0,"y":53.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"castell"},{"name":"Corulag","x":71.0,"y":53.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"corulag"},{"name":"Caamas","x":76.0,"y":53.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"caamas"},{"name":"Bardram Scott","x":60.0,"y":57.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"bardram_scott"},{"name":"Nikardun","x":29.0,"y":58.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"nikardun"},{"name":"Zakuul","x":39.0,"y":57.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"zakuul"},{"name":"Wortan","x":43.0,"y":60.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"wortan"},{"name":"Aargau","x":51.0,"y":59.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"aargau"},{"name":"Kattada","x":66.0,"y":59.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"kattada"},{"name":"Fedalle","x":59.0,"y":62.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"fedalle"},{"name":"Vulpter","x":48.0,"y":62.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"vulpter"},{"name":"Balmorra","x":62.0,"y":63.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"balmorra"},{"name":"Tython","x":49.0,"y":65.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"tython"},{"name":"Sarapin","x":53.0,"y":65.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"sarapin"},{"name":"Gatalenta","x":57.0,"y":65.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"gatalenta"},{"name":"Humbarine","x":56.0,"y":68.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"humbarine"},{"name":"Commenor","x":67.0,"y":68.0,"desc":"Core Worlds","region":"Core Worlds","grid":"","key":"commenor"},{"name":"Rapacc","x":26.0,"y":69.0,"desc":"Unknown Regions","region":"Unknown Regions","grid":"","key":"rapacc"}];
const PLANET_START_CARDS=[
 {name:'Bespin',file:'PLANETA_01.png'},{name:'Corellia',file:'PLANETA_02.png'},{name:'Fondor',file:'PLANETA_03.png'},{name:'Kessel',file:'PLANETA_04.png'},
 {name:'Nal Hutta',file:'PLANETA_05.png'},{name:'Ord Mantell',file:'PLANETA_06.png'},{name:'Ryloth',file:'PLANETA_07.png'},{name:'Tatooine',file:'PLANETA_08.png'},
 {name:'Coruscant',file:'PLANETA_09.png'},{name:'The Wheel',file:'PLANETA_10.png'},{name:'Cholganna',file:'PLANETA_11.png'},{name:'Raxus Prime',file:'PLANETA_12.png'},
 {name:'Drall',file:'PLANETA_13.png'},{name:'Selonia',file:'PLANETA_14.png'},{name:'Tralus & Talus',file:'PLANETA_15.png'},{name:'Tralus',file:'PLANETA_16.png'},
 {name:'Talus',file:'PLANETA_17.png'},{name:'Centerpoint Station',file:'PLANETA_18.png'},{name:'Duro',file:'PLANETA_19.png'},{name:'Nubia',file:'PLANETA_20.png'},
 {name:'Aurea',file:'PLANETA_21.png'},{name:'Sacorria',file:'PLANETA_22.png'},{name:'Corfai',file:'PLANETA_23.png'},{name:'Froz',file:'PLANETA_24.png'},
 {name:'Vagran',file:'PLANETA_25.png'},{name:'Xyquine II',file:'PLANETA_26.png'}
];
function planetStartPoint(card){
 const exact=START_POINTS.find(p=>r53Norm(p.name)===r53Norm(card.name));
 return exact?{...exact,planetFile:card.file,planetCard:true}:{name:card.name,key:r53Norm(card.name).replace(/ /g,'_'),desc:'Ficha planetária',region:`Região de ${card.name}`,grid:'',x:null,y:null,planetFile:card.file,planetCard:true};
}
function openPlanetCardViewer(host,point,onSelect){
 host.querySelector('.r54-planet-card-viewer')?.remove();
 const viewer=document.createElement('div');viewer.className='r54-planet-card-viewer';viewer.innerHTML=`<section role="dialog" aria-modal="true" aria-label="Ficha de ${point.name}">
 <header><div><small>FICHA DO PLANETA</small><strong>${point.name}</strong></div><button type="button" data-close aria-label="Fechar">×</button></header>
 <div class="r54-planet-card-zoom"><img src="assets/planet-cards/${point.planetFile}" alt="Ficha planetária de ${point.name}"></div>
 <footer><div class="r54-planet-zoom-controls"><button type="button" data-zout>−</button><span>ZOOM</span><button type="button" data-zin>+</button></div><button type="button" class="r54-planet-use" data-use>USAR ESTE PLANETA</button></footer>
 </section>`;
 host.appendChild(viewer);const scroller=viewer.querySelector('.r54-planet-card-zoom'),img=viewer.querySelector('img');let zoom=1;
 const apply=()=>{zoom=Math.max(1,Math.min(3,zoom));img.style.width=`${zoom*100}%`;img.style.maxWidth='none'};
 viewer.querySelector('[data-zin]').onclick=()=>{zoom+=.35;apply()};viewer.querySelector('[data-zout]').onclick=()=>{zoom-=.35;apply()};viewer.querySelector('[data-close]').onclick=()=>viewer.remove();viewer.addEventListener('click',e=>{if(e.target===viewer)viewer.remove()});
 viewer.querySelector('[data-use]').onclick=()=>{onSelect();viewer.remove()};
 let lastTap=0;img.addEventListener('click',()=>{const now=Date.now();if(now-lastTap<360){zoom=zoom>1?1:2;apply()}lastTap=now});
}
function setupOverlay(){
 const url=new URL(location.href),forceSetup=url.searchParams.get('campaignSetup')==='1';
 if(forceSetup){url.searchParams.delete('campaignSetup');url.searchParams.set('screen','00');history.replaceState({},'',url)}
 if(document.getElementById('braseiro-sandbox-setup')||(!forceSetup&&sandboxReady())||!hasCharacter())return;
 const modeResult=bridgeResult('campaignModeSummary');if(!modeResult.ok)return;
 const points=PLANET_START_CARDS.map(planetStartPoint);
 const host=document.createElement('div');host.id='braseiro-sandbox-setup';host.setAttribute('role','dialog');host.setAttribute('aria-modal','true');host.setAttribute('aria-label','Configurar campanha sandbox');host.className='final-sandbox-overlay';
 host.innerHTML=`<div class="final-sandbox-card"><div class="final-kicker">COMEÇAR A JOGAR</div><h2>Escolha sua entrada</h2><p>Entre direto em <b>Sob um Sol Negro</b> ou crie um mundo livre.</p>
 <label class="final-field"><span>Campanha</span><select id="sandbox-mode"><option value="EOTE">Edge of the Empire</option><option value="AOR">Age of Rebellion</option><option value="MIXED">Mista · EotE + AoR</option></select></label>
 <div class="final-field r54-planet-picker"><div class="r54-planet-picker-head"><span>Planeta inicial</span><button type="button" data-random-planet>ALEATÓRIO</button></div>
 <div id="sandbox-planet-carousel" class="r54-planet-carousel" aria-label="Fichas de planetas">${points.map((p,i)=>`<button type="button" data-planet-point="${i}" class="r54-planet-thumb ${i===0?'is-selected':''}" aria-label="Selecionar e ampliar ${p.name}"><img src="assets/planet-cards/${p.planetFile}" alt="Ficha de ${p.name}" loading="${i<4?'eager':'lazy'}"><strong>${p.name}</strong></button>`).join('')}</div></div>
 <div class="final-selected-point"><span>Selecionado</span><strong id="sandbox-selected-point">${points[0].name}</strong><small>Toque numa ficha para ampliar e ler.</small></div>
 <div id="sandbox-setup-status" role="status" class="final-status"></div><div class="r54-start-actions"><button id="published-start-button" type="button" class="final-primary-button">JOGAR SOB UM SOL NEGRO</button><button id="sandbox-start-button" type="button">CRIAR MUNDO LIVRE</button></div>
 <p class="final-footnote">Seed inicial: 6 facções, 36 NPCs persistentes, 12 locais, relógios, mercados e oportunidades. Sem roteiro obrigatório; aventuras publicadas são opcionais. As fichas de planeta são referência visual; o Motor Barbara continua sendo a autoridade de regras e estado do mundo.</p></div>`;
 document.body.appendChild(host);
 const select=host.querySelector('#sandbox-mode'),button=host.querySelector('#sandbox-start-button'),publishedButton=host.querySelector('#published-start-button'),status=host.querySelector('#sandbox-setup-status'),selected=host.querySelector('#sandbox-selected-point'),carousel=host.querySelector('#sandbox-planet-carousel');select.value=String(modeResult.data?.campaignMode||'EOTE');let pointIndex=0;
 const choose=i=>{pointIndex=Math.max(0,Math.min(points.length-1,Number(i)||0));host.querySelectorAll('[data-planet-point]').forEach(x=>x.classList.toggle('is-selected',Number(x.dataset.planetPoint)===pointIndex));selected.textContent=points[pointIndex].name;host.querySelector(`[data-planet-point="${pointIndex}"]`)?.scrollIntoView({behavior:'smooth',block:'nearest',inline:'center'})};
 host.querySelectorAll('[data-planet-point]').forEach(x=>x.onclick=()=>{const i=Number(x.dataset.planetPoint);choose(i);openPlanetCardViewer(host,points[i],()=>choose(i))});
 host.querySelector('[data-random-planet]').onclick=()=>{const i=Math.floor(Math.random()*points.length);choose(i);openPlanetCardViewer(host,points[i],()=>choose(i))};
 publishedButton.onclick=()=>{if(!hasCharacter()){status.textContent='Crie ou carregue um personagem para jogar Sob um Sol Negro.';navigateRoute('18');return}publishedButton.disabled=true;button.disabled=true;status.textContent='Preparando Coruscant e iniciando Sob um Sol Negro…';const m=bridgeResult('setCampaignMode','EOTE');if(!m.ok){status.textContent=m.message||'Não foi possível ativar EotE.';publishedButton.disabled=false;button.disabled=false;return}const init=bridgeResult('initializeLivingSandbox',{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'});if(!init.ok){status.textContent=init.message||'Não foi possível preparar o mundo persistente.';publishedButton.disabled=false;button.disabled=false;return}const quickAdventureId='under-black-sun';const r=window.BraseiroPublishedActionClient.execute('start',quickAdventureId);if(!r.ok){status.textContent=r.message||'Não foi possível iniciar Sob um Sol Negro.';publishedButton.disabled=false;button.disabled=false;return}status.textContent=masterNarrationAvailable()?'Sob um Sol Negro iniciada. Mestre preparando a abertura…':'Sob um Sol Negro iniciada com narração local da aventura.';host.remove();setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();enhancePublishedAdventurePanel();enhanceSceneGeminiButton();if(masterNarrationAvailable())void requestPublishedMasterNarration('abertura de Sob um Sol Negro')},0)};
 button.onclick=()=>{const chosen=points[pointIndex],point=chosen.name;button.disabled=true;status.textContent='Criando mundo persistente…';const m=bridgeResult('setCampaignMode',select.value);if(!m.ok){status.textContent=m.message||'Modo inválido.';button.disabled=false;return}const mapPoint=Number.isFinite(chosen.x)&&Number.isFinite(chosen.y)?{x:chosen.x,y:chosen.y}:null;const r=bridgeResult('initializeLivingSandbox',{startingPoint:point,regionLabel:chosen.region||`Região de ${point}`,startMapPoint:mapPoint});if(!r.ok){status.textContent=r.message||'Falha ao criar mundo.';button.disabled=false;return}status.textContent='Mundo criado.';host.remove();setTimeout(()=>{relabelCreationForCampaign();enhance()},0)};
}
function enhanceAdventureGate(){
 const pubView=publishedAdventureView();
 if(pubView?.active&&pubView?.node){document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());return}
 const route=new URLSearchParams(location.search).get('screen')||'00';const character=hasCharacter(),gateKey=`${route}:${character?'character':'empty'}`;
 const existing=document.querySelector('.final-adventure-gate');
 if(existing&&existing.dataset.gateKey===gateKey&&existing.parentNode){return}
 document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());
 if(route==='00'){
  const root=document.querySelector('.scene-concept-shell');if(!root)return;const gate=document.createElement('section');gate.className='final-adventure-gate final-adventure-card';gate.dataset.gateKey=gateKey;
  const mode=campaignMode(),catalog=publishedAdventureCatalogView().filter(a=>Array.isArray(a.campaignModes)&&a.campaignModes.includes(mode));
  const publishedButtons=catalog.map(a=>`<button type="button" data-start-published="${String(a.id||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}">🚀 INICIAR: ${String(a.title||a.originalTitle||'AVENTURA').toUpperCase()}</button>`).join('');
  const finalButtons=publishedButtons||`<button type="button" data-start-published="under-black-sun">🚀 INICIAR: SOB UM SOL NEGRO (CORUSCANT)</button><button type="button" data-start-published="beyond-the-rim">🌌 INICIAR: ALÉM DA ORLA (ORD MANTELL)</button>`;
  const st=currentState();
  const openingNarration=String(st?.scene?.narration||'');
  const isBlackSunOpening=openingNarration.includes('Alarmes ecoam')||openingNarration.includes('Black Sun')||openingNarration.includes('Sob um Sol Negro');
  const quickChoices=isBlackSunOpening?`
    <div class="final-published-choices" style="margin-top:12px;border-top:1px solid #334e68;padding-top:10px;">
      <div style="font-size:0.75rem;font-weight:900;color:#f59e0b;letter-spacing:0.08em;margin-bottom:6px;">⚡ ESCOLHA SUA REAÇÃO IMEDIATA AO TIROTEIO:</div>
      <button type="button" data-quick-black-sun="escape-clean">➔ 1. Assumir o controle do speeder e despistar</button>
      <button type="button" data-quick-black-sun="escape-fight">➔ 2. Abrir caminho revidando contra a Black Sun</button>
      <button type="button" data-quick-black-sun="escape-crash">➔ 3. Manobra evasiva (escapar após pouso forçado)</button>
    </div>
  `:'';
  gate.innerHTML=character?`<div><span>CENTRAL DE MISSÕES GALÁCTICAS</span><strong>Escolha sua aventura ou explore o mundo livre</strong><small>As aventuras oficiais utilizam regras e narrativas completas de Star Wars: Fronteira do Império (EotE). Modo ativo: <b>${mode}</b>.</small></div><div class="final-adventure-actions ${catalog.length>1?'final-adventure-actions-many':'final-adventure-actions-two'}">${finalButtons}<button type="button" class="r54-hero-btn is-secondary" data-start-sandbox>★ INICIAR SANDBOX (MUNDO LIVRE)</button><button type="button" class="r53-secondary" data-open-campaign-hub style="background:#1e293b;border-color:#eab308;color:#fef08a;font-weight:800;">☰ GERENCIAR CAMPANHA / REINICIAR</button></div>${quickChoices}<output></output>`:`<div><span>ANTES DE COMEÇAR</span><strong>Crie ou carregue um personagem.</strong><small>Você pode criar manualmente, gerar uma ficha completa aleatória ou importar .json.</small></div><div class="final-adventure-actions"><button type="button" data-manual-character>CRIAR FICHA</button><button type="button" data-random-character>ALEATÓRIA</button><button type="button" data-import-character>IMPORTAR JSON</button></div><output></output>`;
  const targetSlot=root.querySelector('.scene-narrative-paper')||root.querySelector('.scene-media-slot');
  if(targetSlot&&targetSlot.parentNode===root&&targetSlot.nextSibling)root.insertBefore(gate,targetSlot.nextSibling);else root.prepend(gate);
  const out=gate.querySelector('output');
  gate.querySelector('[data-open-campaign-hub]')?.addEventListener('click',r54OpenCampaignHub);
  gate.querySelector('[data-manual-character]')?.addEventListener('click',()=>navigateRoute('18'));
  gate.querySelector('[data-random-character]')?.addEventListener('click',()=>quickRandomCharacter(out));
  gate.querySelector('[data-import-character]')?.addEventListener('click',()=>quickImportCharacter(out));
  gate.querySelectorAll('[data-start-published]').forEach(b=>b.addEventListener('click',()=>{
    const id=String(b.dataset.startPublished||''),entry=catalog.find(x=>x.id===id),label=String(entry?.title||entry?.originalTitle||(id==='under-black-sun'?'Sob um Sol Negro':'Além da Orla'));
    b.disabled=true;const original=b.textContent;b.textContent='INICIANDO…';
    if(!hasCharacter()){
      bridgeResult('loadStarterCharacter','cael');
    }
    if(!sandboxReady()){
      const origin=id==='under-black-sun'?{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'}:{startingPoint:'Ord Mantell — Praça Central',regionLabel:'Ord Mantell'};
      bridgeResult('initializeLivingSandbox',origin);
    }
    r54ClearTransientUiState();
    let r=window.BraseiroPublishedActionClient.execute('start',id);
    if(!r?.ok){
      const direct=bridgeResult('restartPublishedAdventure',id,{});
      if(direct&&direct.ok) r={ok:true};
    }
    if(!r?.ok){b.disabled=false;b.textContent=original;if(out)out.textContent=r?.message||'Não foi possível iniciar a aventura.';return}
    window.dispatchEvent(new Event('braseiro:r21-state-changed'));
    gate.remove();
    setTimeout(()=>{
      enhance();
      if(masterNarrationAvailable())void requestPublishedMasterNarration(`abertura de ${label}`);
    },50);
  }));
  gate.querySelectorAll('[data-quick-black-sun]').forEach(b=>b.addEventListener('click',()=>{
    const choiceId=String(b.dataset.quickBlackSun||'');
    b.disabled=true;b.textContent='EXECUTANDO ESCOLHA…';
    if(!hasCharacter()){
      bridgeResult('loadStarterCharacter','cael');
    }
    if(!sandboxReady()){
      bridgeResult('initializeLivingSandbox',{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'});
    }
    r54ClearTransientUiState();
    const startRes=window.BraseiroPublishedActionClient.execute('start','under-black-sun');
    if(!startRes.ok&&startRes.code!=='ALREADY_ACTIVE'){
      bridgeResult('restartPublishedAdventure','under-black-sun',{});
    }
    const choiceRes=window.BraseiroPublishedActionClient.execute('choice',choiceId);
    window.dispatchEvent(new Event('braseiro:r21-state-changed'));
    gate.remove();
    setTimeout(()=>{
      enhance();
      if(masterNarrationAvailable())void requestPublishedMasterNarration('reação imediata do grupo em Coruscant');
    },50);
  }));
  gate.querySelector('[data-start-sandbox]')?.addEventListener('click',async()=>{
    const b=gate.querySelector('[data-start-sandbox]');b.disabled=true;b.textContent='INICIANDO…';
    const r=await startBarbaraOpening();
    if(!r.ok){b.disabled=false;b.textContent='INICIAR SANDBOX';alert(r.message||'Não foi possível iniciar o sandbox.');return}
    setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();enhanceSceneGeminiButton()},0);
  });
 }else if(character){
  const bar=document.createElement('button');bar.type='button';bar.className='final-adventure-gate final-adventure-fab';bar.dataset.gateKey=gateKey;bar.textContent='▶ INICIAR SANDBOX';bar.onclick=async()=>{const r=await startBarbaraOpening();if(!r.ok){alert(r.message||'Não foi possível iniciar o sandbox.');return}navigateRoute('00')};document.body.appendChild(bar);
 }
}
function enhancePublishedAdventurePanel(){
 const h=v=>String(v??'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));
 const old=document.querySelector('.final-published-adventure-panel');const status=publishedAdventureView();
 if(!status?.active&&!status?.completed){old?.remove();return}
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='00'){old?.remove();return}
 const root=document.querySelector('.scene-concept-shell');if(!root)return;const node=status.node;if(!node){old?.remove();return}
 let panel=old;if(!panel){panel=document.createElement('section');panel.className='final-published-adventure-panel final-adventure-card';}
 if(panel.parentNode!==root){const targetSlot=root.querySelector('.scene-narrative-paper')||root.querySelector('.scene-media-slot');if(targetSlot&&targetSlot.parentNode===root&&targetSlot.nextSibling)root.insertBefore(panel,targetSlot.nextSibling);else root.prepend(panel)}
 const last=status?.progress?.flags?.lastCheck||null;
 const checks=(node.checks||[]).map((c,i)=>{const isLast=last?.checkId===`${node.id}:check:${i}`;return `<li class="${isLast?'is-resolved':''}"><div><strong>${h(c.skill)}</strong> · ${h(c.difficulty)}<small>${h(c.purpose)}</small></div><button type="button" data-published-check="${i}">ROLAR</button></li>`}).join('');
 const choices=(node.choices||[]).map(c=>`<button type="button" data-published-choice="${h(c.id)}">${h(c.label)}</button>`).join('');
 const systems=(status?.progress?.systems||[]).map(x=>{let value='';if(x.id==='chase')value=`Avanço ${Number(x.value)>0?'+':''}${Number(x.value)||0} · rodada ${Number(x.rounds)||0}`;else if(x.id==='yiyar-chase')value=`Vantagem ${Number(x.value)>0?'+':''}${Number(x.value)||0} · rodada ${Number(x.rounds)||0}`;else if(x.id==='imperial-exposure')value=`Exposição ${Number(x.value)||0}/6`;else if(x.id==='sabacc')value=`${Number(x.value)>=0?'+':''}${Number(x.value)||0} cr · rodada ${Number(x.rounds)||0}`;else if(x.id==='zelcomm')value=`Alerta ${Number(x.value)||0}/6${x.status==='lockdown'?' · LOCKDOWN':''}`;else if(x.id==='novacore')value=`Risco ${Number(x.value)||0}/6${x.status==='critical'?' · CRÍTICO':''}`;return `<div class="final-published-system ${x.status==='lockdown'||x.status==='critical'||x.status==='high'?'is-danger':''}"><small>${h(x.label)}</small><strong>${h(value)}</strong></div>`}).join('');
 const consequences=(status?.knownConsequences||[]).slice(-3).map(c=>`<div class="final-published-consequence"><small>CONSEQUÊNCIA NO MUNDO</small><strong>${h(c.summary)}</strong></div>`).join('');
 const npcCards=(status?.npcCards||[]).map(n=>{const initials=String(n.name||'?').split(/\s+/).slice(0,2).map(x=>x[0]||'').join('').toUpperCase();const portrait=n.portraitAsset?`<img class="final-published-npc-portrait" src="${h(n.portraitAsset)}" alt="">`:`<span class="final-published-npc-fallback" aria-hidden="true">${h(initials)}</span>`;return `<article class="final-published-npc">${portrait}<div><small>NPC PRESENTE</small><strong>${h(n.name)}</strong><span>${h([n.species,n.occupation].filter(Boolean).join(' · '))}</span>${n.appearance?`<em>${h(n.appearance)}</em>`:''}</div></article>`}).join('');
 const knownClues=(status?.knownClues||[]).slice(-5).map(x=>`<article class="final-published-clue"><small>${h(x.title||'PISTA CONHECIDA')}</small><strong>${h(x.summary)}</strong></article>`).join('');
 const effectLabel={advantage:'VANTAGEM',threat:'AMEAÇA',triumph:'TRIUNFO',despair:'DESESPERO',clue:'PISTA',social:'SOCIAL',resource:'RECURSO',tactical:'TÁTICO',hazard:'RISCO',scene:'CENA'};
 const checkFeedback=(status?.checkFeedback||[]).slice(-3).map(x=>{const effects=(x.effects||[]).map(e=>`<li class="is-${h(e.kind||'scene')}"><span>${h(effectLabel[e.kind]||String(e.kind||'EFEITO').toUpperCase())}${Number(e.count)>1?` ×${Number(e.count)}`:''}</span><strong>${h(e.summary)}</strong>${Number(e.obligationDelta)>0?`<em>Obligation +${Number(e.obligationDelta)}</em>`:''}</li>`).join('');return `<div class="final-published-feedback ${x.success?'is-success':'is-complication'}"><small>CONSEQUÊNCIA DO TESTE</small><strong>${h(x.summary)}</strong>${effects?`<ul class="final-published-narrative-effects">${effects}</ul>`:''}</div>`}).join('');
 const sceneJournal=(status?.sceneJournal||[]).slice(-6);
 const sceneTitleById=new Map(sceneJournal.flatMap(x=>[[x.fromNodeId,x.fromTitle||x.fromNodeId],[x.toNodeId,x.toTitle||x.toNodeId]]));
 const timeline=sceneJournal.map((x,i)=>{const label=x.choiceLabel||'Avançou na investigação';const destination=x.toTitle||sceneTitleById.get(x.toNodeId)||String(x.toNodeId||'Cena').replace(/[-_]+/g,' ');return `<article class="final-published-timeline-item"><span aria-hidden="true">${i===sceneJournal.length-1?'●':'○'}</span><div><small>${h(destination)}</small><strong>${h(label)}</strong></div></article>`}).join('');
 const timelineUi=timeline?`<details class="final-published-timeline" ${sceneJournal.length<=3?'open':''}><summary>CAMINHO PERCORRIDO <b>${sceneJournal.length}</b></summary><div>${timeline}</div></details>`:'';
 const tracker=status?.tracker||null;
 const trackerUi=tracker?(()=>{
  if(tracker.kind==='beyond-the-rim'){const signals=tracker.signals||{},stages=(tracker.stages||[]).filter(x=>x?.reached);const exposure=Math.max(0,Math.min(6,Number(signals.imperialExposure)||0));const lead=Number(signals.yiyarLead)||0;const signalUi=`<div class="final-published-pressure"><span class="${lead<0?'is-danger':lead>0?'is-good':''}">DISPUTA YIYAR <b>${lead>0?'+':''}${lead}</b></span><span class="${exposure>=4?'is-danger':''}">EXPOSIÇÃO IMPERIAL <b>${exposure}/6</b></span>${signals.droidRecovered?'<span class="is-good">IT-3PO SEGURO</span>':''}${signals.survivorsSecured?'<span class="is-good">SOBREVIVENTES RETIRADOS</span>':''}</div>`;const stageUi=stages.length?`<div class="final-published-milestones">${stages.map((x,i)=>`<span class="${i===stages.length-1?'is-current':''}">${h(x.label)}</span>`).join('')}</div>`:'';const trail=Array.isArray(tracker.trail)?tracker.trail:[];const trailUi=trail.length?`<div class="final-published-trail" aria-label="Caminho percorrido">${trail.map((x,i)=>`<span class="${i===trail.length-1?'is-current':''}"><small>E${Number(x.episode)||1}</small>${h(x.title)}</span>`).join('')}</div>`:'';return `<section class="final-published-tracker" aria-label="Progresso de Além da Orla"><small>OBJETIVO ATUAL · EPISÓDIO ${Number(tracker.episode)||1}</small><strong>${h(tracker.objective||'Avançar na expedição.')}</strong>${signalUi}${stageUi}${trailUi}</section>`}
  const inv=tracker.investigation||{},pressure=tracker.pressure||{},milestones=tracker.milestones||{};
  const progress=Math.max(0,Math.min(100,Math.round((Number(inv.found)||0)/Math.max(1,Number(inv.total)||3)*100)));
  const investigation=milestones.investigationOpened?`<div class="final-published-tracker-row"><span>INVESTIGAÇÃO</span><div class="final-published-meter"><i style="width:${progress}%"></i></div><strong>${Number(inv.found)||0}/${Number(inv.total)||3}${inv.ready?' · PISTAS SUFICIENTES':''}</strong></div>`:'';
  const alert=Math.max(0,Math.min(6,Number(pressure.alert)||0)),momentum=Math.max(0,Math.min(6,Number(pressure.momentum)||0));
  const pressureUi=`<div class="final-published-pressure"><span class="${alert>=4?'is-danger':''}">ALERTA <b>${alert}/6</b></span><span class="${momentum>=4?'is-good':''}">MOMENTUM <b>${momentum}/6</b></span>${pressure.kaatoAlerted?'<span class="is-danger">KAA’TO ALERTADO</span>':''}</div>`;
  const reached=[milestones.escaped&&'FUGA',milestones.investigationOpened&&'INVESTIGAÇÃO',milestones.endgameReached&&'DESFECHO',milestones.kaatoFound&&'ALVO LOCALIZADO',milestones.completed&&'CONCLUÍDA'].filter(Boolean);
  const milestoneUi=reached.length?`<div class="final-published-milestones">${reached.map((x,i)=>`<span class="${i===reached.length-1?'is-current':''}">${h(x)}</span>`).join('')}</div>`:'';
  return `<section class="final-published-tracker" aria-label="Progresso da aventura"><small>OBJETIVO ATUAL · EPISÓDIO ${Number(tracker.episode)||1}</small><strong>${h(tracker.objective||'Avançar na missão.')}</strong>${investigation}${pressureUi}${milestoneUi}</section>`;
 })():'';
 const epilogue=status?.epilogueSummary||null;
 const epilogueLines=Array.isArray(status?.epilogue)?status.epilogue.filter(x=>x&&x.summary).map(x=>x.summary):[];
 const epilogueUi=epilogue?`<section class="final-published-epilogue"><div class="final-published-clues-title">MISSÃO CONCLUÍDA</div>${(epilogue.lines||[]).map(x=>`<p>${h(x)}</p>`).join('')}${epilogue.reward?`<strong>Recompensa: +${Number(epilogue.reward.credits)||0} cr${Number(epilogue.reward.obligationAdded)?` · Obligation +${Number(epilogue.reward.obligationAdded)}`:''}${Number(epilogue.reward.obligationReduced)?` · Obligation -${Number(epilogue.reward.obligationReduced)}`:''}</strong>`:''}</section>`:epilogueLines.length?`<section class="final-published-epilogue"><div class="final-published-clues-title">MISSÃO CONCLUÍDA · CONSEQUÊNCIAS</div>${epilogueLines.map(x=>`<p>${h(x)}</p>`).join('')}</section>`:'';
 const uaeObjectives=(status?.uae?.objectives||[]).filter(x=>x&&x.title).slice(-4);
 const uaeObjectivesUi=uaeObjectives.length?`<section class="final-published-uae-objectives" aria-label="Objetivos da aventura"><div class="final-published-action-title">OBJETIVOS DA AVENTURA</div>${uaeObjectives.map(x=>`<div class="final-published-uae-objective ${x.status==='COMPLETED'?'is-completed':''}"><span aria-hidden="true">${x.status==='COMPLETED'?'✓':'○'}</span><strong>${h(x.title)}</strong><small>${x.status==='COMPLETED'?'CONCLUÍDO':`${Math.max(0,Number(x.progress)||0)}/${Math.max(1,Number(x.target)||1)}`}</small></div>`).join('')}</section>`:'';
 const imperialClock=status?.imperialDeployment;
 const imperialClockUi=imperialClock?.available?`<section class="final-published-action-zone final-p156-imperial-clock"><div class="final-published-action-title">CERCO IMPERIAL · RELÓGIO OFICIAL</div><strong>${Math.max(0,Number(imperialClock.elapsedMinutes)||0)}/30 MIN</strong><small> ${h(imperialClock.events?.slice(-1)[0]?.summary||'A primeira patrulha está se aproximando.')}</small>${imperialClock.critical?'<strong class="is-danger">REFORÇOS CHEGARAM</strong>':'<button type="button" data-p156-imperial-tick>AVANÇAR 5 MINUTOS</button>'}</section>`:'';
 const jewelPatrols=Array.isArray(status?.wingGuardInvestigations)?status.wingGuardInvestigations:[];
 const jewelPatrolUi=jewelPatrols.length?`<section class="final-published-action-zone"><div class="final-published-action-title">GUARDA ALADA · RASTREAMENTO DO BANCO</div>${jewelPatrols.map(p=>`<small>Equipe ${Number(p.teamIndex)+1}: ${p.guardCount} oficiais · ${p.status==='en_route'?`a caminho (${p.remainingMinutes} min)`:p.status==='ready'?'CHEGOU — encontro disponível':p.status==='active'?'EM COMBATE':p.status==='investigating'?'investigando o local da invasão':`resolvido (${h(p.outcome||'')})`}</small>`).join('<br>')}</section>`:'';
 const encounter=status?.encounter||null;
 const encounterUi=encounter?.id==='holo-park-wing-guard'?'':encounter?.available?(encounter.status==='active'?`<div class="final-published-encounter is-active"><strong>COMBATE EM ANDAMENTO</strong><small>${h(encounter.label)}</small>${encounter.preparation?`<em>${h(encounter.preparation)}</em>`:''}</div>`:`<div class="final-published-encounter-wrap"><button type="button" class="final-published-encounter" data-published-encounter="${h(encounter.id)}">⚔ ${h(encounter.label)}</button>${encounter.preparation?`<small>${h(encounter.preparation)}</small>`:''}</div>`):'';
 const implementationBoundary=status?.implementation?.episode1Complete&&node.id==='episode1-boundary';
 const noChoiceUi=status.completed?`${epilogueUi}<div class="final-published-complete">Aventura concluída. As consequências permanecem no mundo vivo.</div><div class="final-published-choices"><button type="button" data-continue-published>CONTINUAR NO MUNDO LIVRE</button></div>`:implementationBoundary?`<div class="final-published-complete">Episódio I concluído e persistido. O Episódio II ainda não está habilitado nesta baseline; nenhum conteúdo futuro foi revelado.</div>`:`<div class="final-published-complete">Resolva o encontro ou o teste desta cena para liberar o próximo desfecho.</div>`;
 const adventureId=activePublishedAdventureId()||status?.progress?.id||'under-black-sun',adventureTitle=status?.progress?.title||'Aventura publicada';
 const escapeBomb=status?.escapeBomb||null;
 const escapeImpact=status?.escapeImpact||null;
 const escapeCompanionImpact=status?.escapeCompanionImpact||null;
 const jewelCompanions=currentState()?.group?.companions||[];
 const arrow7Combat=currentState()?.combat;
 const arrow7Goon=arrow7Combat?.adversaries?.find(a=>a.profileKey==='shens_goons'&&a.kind==='minion');
 const arrow7Active=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&arrow7Combat?.active&&arrow7Combat?.publishedAdventureContext?.encounterId==='arend-shen-four-goons'&&arrow7Goon;
 const arrow7Order=arrow7Combat?.arrow7TurretOrder;
 const arrow7Ui=arrow7Active?`<section class="final-published-action-zone"><div class="final-published-action-title">ARROW-7 · BLASTER TRASEIRO · ESCALA PESSOAL</div><small>Fonte SWE09 p.90: dano 11, crítico 3, alcance Extremo, Auto-Fire, Perfuração 1. Artilharia usa o grupo real de capangas na iniciativa NPC. Não é arma de escala planetária. Distância atual: ${h(arrow7Goon.range)}. ${arrow7Order?.status==='QUEUED'?'Tiro agendado nesta rodada.':arrow7Order?.status==='FIRED'?'Tiro já executado.':''}</small><label><input type="checkbox" data-jewel-arrow7-gunner> Mestre confirma artilheiro presente e operando o canhão</label><label><input type="checkbox" data-jewel-arrow7-aft> Mestre confirma alvo atrás do veículo, no arco traseiro</label><button type="button" data-jewel-arrow7-queue>PREPARAR DISPARO NA INICIATIVA DOS CAPANGAS</button><small>Não movimenta o veículo, não inventa velocidade/tempo e não aciona Auto-Fire automaticamente. A distância é a posição real do combate.</small></section>`:'';
 const chaseState=status?.arrow7Chase||null;
 const arrow7ChaseUi=arrow7Active?`<section class="final-published-action-zone"><div class="final-published-action-title">ARROW-7 · PERSEGUIÇÃO COMPETITIVA · ESCALA PLANETÁRIA</div><small>Edge of the Empire, The Chase: ambos pilotos rolam Pilotagem Planetária no INÍCIO da rodada. Vitória muda uma faixa; velocidades verificadas permitem faixas adicionais. ${chaseState?`Rodada ${chaseState.round}: ${h(chaseState.range)} (${h(chaseState.status)}).`:'Sem perseguição iniciada.'} Não inventar velocidade ou ficha do piloto de Arend.</small><label>Jogador é <select data-jewel-chase-role><option value="evader">Perseguido</option><option value="pursuer">Perseguidor</option></select></label><label>Distância planetária inicial <select data-jewel-chase-range><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label>Ficha confirmada do piloto NPC (ID) <input data-jewel-chase-npc-id placeholder="ID da ficha do piloto"></label><label>Agilidade do piloto NPC <input data-jewel-chase-npc-agility type="number" min="0" max="7" placeholder="conferir ficha"></label><label>Pilotagem Planetária do NPC <input data-jewel-chase-npc-rank type="number" min="0" max="7" placeholder="conferir ficha"></label><label>Dificuldade do terreno (0 = simples) <input data-jewel-chase-difficulty type="number" min="0" max="5" value="0"></label><label>Revés do terreno <input data-jewel-chase-setback type="number" min="0" max="6" value="0"></label><label><input type="checkbox" data-jewel-chase-both> Mestre confirma DOIS veículos na perseguição</label><label><input type="checkbox" data-jewel-chase-terrain> Mestre confirma terreno e dificuldade</label><label><input type="checkbox" data-jewel-chase-start> Mestre confirma distância planetária inicial real</label><label><input type="checkbox" data-jewel-chase-pilot> Mestre confirma ficha real do piloto NPC (não publicada em SWE09)</label><label><input type="checkbox" data-jewel-chase-speeds> Velocidades atuais de AMBOS os veículos verificadas</label><label>Velocidade atual do jogador <input data-jewel-chase-player-speed type="number" min="0" max="6" placeholder="somente se verificada"></label><label>Velocidade atual Arrow-7 <input data-jewel-chase-arrow-speed type="number" min="0" max="6" placeholder="somente se verificada"></label><button type="button" data-jewel-chase-roll>ROLAR PILOTAGEM COMPETITIVA DA RODADA</button><small>Empates mantêm distância. Alcance e velocidades são independentes da distância PESSOAL do canhão. Contato/fuga exigem desfecho do Mestre; sem teleporte, sem blindagem/casco inventados.</small>${chaseState&&['CONTACT_PENDING_GM','ESCAPE_PENDING_GM'].includes(chaseState.status)?`<label><input type="checkbox" data-jewel-chase-end-gm> Mestre confirma desfecho da perseguição</label><button type="button" data-jewel-chase-end>CONFIRMAR ${chaseState.status==='CONTACT_PENDING_GM'?'CONTATO':'FUGA'}</button>`:''}</section>`:'';
 const arendSearch=status?.arendRevenge||null;
 const arendReady=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&escapeBomb&&
  !arrow7Combat?.active&&status?.progress?.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome);
 const arendRevengeUi=arendReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 · VINGANÇA CONTRA AREND · ENDEREÇOS REAIS</div><small>Após a traição, o banco de dados de Cloud City revela quatro propriedades com Computadores ou Conhecimento das Ruas Média. A localização ATUAL de Arend não aparece no terminal. ${arendSearch?.leadsKnown?'Quatro endereços encontrados.':`Pesquisas: ${arendSearch?.checks?.length||0}.`}</small>${!arendSearch?.leadsKnown?`<label>Perícia <select data-arend-skill><option value="computadoras">Computadores</option><option value="callejeo">Conhecimento das Ruas</option></select></label><label><input type="checkbox" data-arend-mainframe> Mestre confirma acesso ao terminal conectado à rede de Cloud City</label><label><input type="checkbox" data-arend-retry> Mestre autoriza nova tentativa após falha</label><button type="button" data-arend-search>ROLAR PESQUISA DE ENDEREÇOS</button>`:arendSearch.foundAt?`<strong>Arend localizado em ${h((status.arendHoldings||[]).find(x=>x.id===arendSearch.foundAt)?.label||arendSearch.foundAt)}. O encontro/desfecho ainda exige o Mestre.</strong>`:`<small>Visitas: ${arendSearch.visits.length}/4. Chances oficiais: 25%, 50%, 75%, quarta automática. Cada local é visitado apenas uma vez.</small><label>Imóvel <select data-arend-holding>${(status.arendHoldings||[]).filter(x=>arendSearch.remainingHoldings.includes(x.id)).map(x=>`<option value="${h(x.id)}">${h(x.label)}</option>`).join('')}</select></label><label><input type="checkbox" data-arend-visit-gm> Mestre confirma chegada física ao imóvel selecionado</label><button type="button" data-arend-visit>PROCURAR AREND · ROLAR D100</button>`}<small>Sem teleporte, sem localização fictícia, sem morte ou captura automática.</small></section>`:'';
 const holoWingReady=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&
  status?.encounter?.id==='holo-park-wing-guard'&&status.encounter.status==='ready'&&!arrow7Combat?.active;
 const holoFactionReady=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&
  status?.progress?.encounterHistory?.some(x=>x.encounterId==='holo-park-wing-guard'&&x.outcome)&&
  status?.holoPark&&!currentState()?.combat?.active;
 const landoPardonReady=adventureId==='jewel-of-yavin'&&!status?.landoPardon&&!currentState()?.combat?.active&&
  ['ACTIVE','COMPLETED'].includes(status?.progress?.status);
 const landoPardonUi=landoPardonReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.90 · PERDÃO DE LANDO · OBRIGAÇÃO REAL</div><small>Via alternativa "Going Respectable": Lando somente perdoa os crimes após ouvir e aceitar um plano concreto para reparar a reputação turística de Cloud City. Cada PJ confirmado recebe +5 de Obrigação: Favor a Lando. Não captura Arend, não entrega a joia, não concede XP e não cancela outras recompensas automaticamente.</small><label>Nomes dos personagens JOGADORES, separados por vírgulas (não incluir NPCs)<input data-p218-party-names maxlength="600" value="${h(currentState()?.player?.character?.name||'')}"></label><label><input type="checkbox" data-p218-roster> Mestre confirma a lista integral de personagens jogadores, não de companheiros NPC</label><label><input type="checkbox" data-p218-audience> Mestre confirma audiência efetiva com Lando</label><label><input type="checkbox" data-p218-plan> Mestre confirma que Lando aceitou o plano de reparar a reputação de Cloud City</label><label><input type="checkbox" data-p218-pardon> Mestre confirma que Lando efetivamente concedeu perdão</label><button type="button" data-p218-lando-grant>REGISTRAR PERDÃO E +5 FAVOR POR PJ</button></section>`:status?.landoPardon?`<section class="final-published-action-zone"><div class="final-published-action-title">LANDO · PERDÃO REGISTRADO</div><small>+5 Obrigação: Favor por personagem jogador confirmado (${(status.landoPardon.members||[]).map(h).join(', ')}). Sem concessão duplicada.</small></section>`:'';
 const shenAftermath=status?.shenAftermath||null;
 const shenReady=adventureId==='jewel-of-yavin'&&['ACTIVE','COMPLETED'].includes(status?.progress?.status)&&
  status?.progress?.operations?.escapeBomb&&status?.progress?.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome)&&
  !currentState()?.combat?.active;
 const shenUi=shenReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91,95 · DESTINOS DE AREND E ARIS</div><small>Registre apenas acontecimentos efetivamente jogados. A morte de Arend encerra seus planos pessoais; se livre ou preso, continua inimigo recorrente. Aliança com Aris requer consentimento mútuo. Não concede XP, recompensa, teleporte ou captura automaticamente.</small>${shenAftermath?.arend?`<strong>Arend: ${h(shenAftermath.arend.status)} · local ${h(shenAftermath.arend.holdingId)}.</strong>`:status?.arendRevenge?.foundAt?`<label>Destino após confronto <select data-p219-arend-status><option value="DEAD">Morto — comprovado pelo Mestre</option><option value="INCARCERATED">Preso — custódia confirmada</option><option value="AT_LARGE">Livre — fuga/sobrevivência confirmada</option></select></label><label><input type="checkbox" data-p219-arend-identity> Mestre confirma que era realmente Arend Shen</label><label><input type="checkbox" data-p219-arend-confront> Mestre confirma confronto físico ocorrido no local encontrado</label><label><input type="checkbox" data-p219-arend-result> Mestre confirma o resultado selecionado (morte/custódia/fuga)</label><button type="button" data-p219-arend-record>REGISTRAR DESTINO DE AREND</button>`:'<small>Para registrar Arend, localize-o primeiro pelos quatro imóveis oficiais.</small>'}${shenAftermath?.aris?'<strong>Aris: aliança confirmada e persistida.</strong>':`<label><input type="checkbox" data-p219-aris-meeting> Mestre confirma encontro real com Aris</label><label><input type="checkbox" data-p219-aris-agree> Mestre confirma que Aris concordou com a aliança</label><label><input type="checkbox" data-p219-aris-party> Mestre confirma que os PJs aceitaram a aliança</label><label><input type="checkbox" data-p219-aris-peace> Mestre confirma que Aris não está mais hostil</label><button type="button" data-p219-aris-record>REGISTRAR ALIANÇA COM ARIS</button>`}</section>`:'';
 const arendProperty=status?.arendProperty||null;
 const arendHolding=status?.arendRevenge?.foundAt||null;
 const arendPropertyReady=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&
   arendHolding&&!shenAftermath?.arend&&!arrow7Combat?.active;
 const arendPropertyUi=arendPropertyReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91 · CONFRONTO REAL COM AREND</div><small>Local encontrado: ${h(arendHolding)}. A segurança e Arend são encontros SEPARADOS. Residência: 3 droides K4 na entrada, depois 6 capangas na sala; escritório: 4 guardas de Grayson; Port Town: 10 capangas; iate Venture: 3 grupos de 3 tripulantes. Somente indivíduos presentes, sem teleporte, recompensa ou vitória automática.</small><label>Etapa <select data-p220-stage><option value="security">Segurança do imóvel</option>${arendHolding==='plaza-residence'?'<option value="interior">Capangas na sala da residência</option>':''}<option value="arend">Arend Shen — nemesis individual</option></select></label><label>Distância confirmada <select data-p220-range><option>Engajado</option><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option></select></label><label><input type="checkbox" data-p220-presence> Mestre confirma presença física dos adversários</label><label><input type="checkbox" data-p220-distinct> Mestre confirma indivíduos distintos, sem reaproveitar NPCs</label><label><input type="checkbox" data-p220-access> Mestre confirma acesso real ao local/compartimento</label><label><input type="checkbox" data-p220-bypass> Mestre confirma que os guardas foram evitados fisicamente (etapas posteriores, se não houve combate de segurança)</label><button type="button" data-p220-start>INICIAR COMBATE OFICIAL NO IMÓVEL</button><small>Etapas registradas: ${h((arendProperty?.stages||[]).map(x=>x.stage).join(', ')||'nenhuma')}. Patrulhas e alerta de dez minutos usam o relógio real no painel abaixo.</small></section>`:'';
 const p222Awards=status?.episode3Rewards||[];
 const p222XpUi=status?.progress?.id==='jewel-of-yavin'&&status?.progress?.currentNodeId==='cloud-city-pursuit'?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.95 · EXPERIÊNCIA EPISÓDIO III</div><small>Somente marcos reais concluídos. PE são adicionados à ficha atual; não se repetem por marco e personagem.</small><label>Marco oficial <select data-p222-xp-milestone><option value="HOLO_PARK_ESCAPE">Fuga real do Holo-Park · 10 PE</option><option value="ARIS_ALLIANCE">Aliança confirmada com Aris · 5 PE</option><option value="AREND_DEFEATED">Arend localizado e derrotado em combate · 15 PE</option><option value="CLOUD_CAR_PATROLS">Quatro cloud cars derrotados ou evadidos · 5 PE</option><option value="BESPIN_ESCAPE">Fuga real de Bespin com a Joia · 5 PE</option></select></label><label><input type="checkbox" data-p222-xp-gm> Mestre confirma concessão ao PJ atual</label><button type="button" data-p222-xp-grant>CONCEDER PE COM PROVA E RECIBO</button><small>Concedidos: ${p222Awards.length}. Cloud cars exigem quatro recibos reais; fuga de Bespin exige posse física, embarque e salto hiperespacial real; sem XP antecipado.</small></section>`:'';
 const arendPatrol=status?.arendPatrols||null;
 const patrolResidence=arendPatrol?.residence||null,patrolVenture=arendPatrol?.venture||null;
 const arendPatrolUi=arendPropertyReady&&['plaza-residence','plaza-yacht'].includes(arendHolding)?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91 · GUARDA ALADA · RELÓGIO REAL</div><small>Tempo confirmado: ${Number(arendPatrol?.elapsedMinutes??0)} minutos (tarefas persistidas). O relógio NÃO avança sozinho; só tarefas concluídas e contabilizadas contam.</small>${arendHolding==='plaza-residence'?(!patrolResidence?`<label><input type="checkbox" data-p221-outdoors> Mestre confirma grupo fisicamente na rua da residência</label><button type="button" data-p221-residence-start>INICIAR EXPOSIÇÃO ÀS PATRULHAS</button>`:patrolResidence.endedAt!==null?`<strong>Exposição encerrada aos ${patrolResidence.endedAt} minutos.</strong>`:`<small>Patrulha a cada 15 minutos. Próximo teste no minuto ${patrolResidence.nextDueMinute}. ${patrolResidence.due?'PATRULHA DEVIDA — teste Difícil obrigatório.':`Faltam ${patrolResidence.minutesUntilDue} minutos reais.`}</small><label>Perícia do PJ <select data-p221-patrol-skill><option value="engano">Engano</option><option value="sigilo">Furtividade</option><option value="callejeo">Conhecimento das Ruas</option></select></label><button type="button" data-p221-patrol-roll ${patrolResidence.due?'':'disabled'}>ROLAR TESTE DIFÍCIL REAL CONTRA PATRULHA</button><label><input type="checkbox" data-p221-outdoors-end> Mestre confirma saída física da rua</label><button type="button" data-p221-residence-end ${patrolResidence.due?'disabled':''}>ENCERRAR EXPOSIÇÃO</button><small>Falha e símbolos narrativos ficam registrados; chegada de guardas e consequências exigem decisão do Mestre, sem teleporte.</small>`):(!patrolVenture?`<label><input type="checkbox" data-p221-hostilities> Mestre confirma hostilidades reais contra a tripulação do Venture</label><label><input type="checkbox" data-p221-transponder> Mestre confirma Arend reativando o transponder</label><button type="button" data-p221-venture-alert>REGISTRAR ALERTA DOS CLOUD CARS</button>`:`<small>Alerta no minuto ${patrolVenture.alertAt}; prazo oficial até minuto ${patrolVenture.deadlineMinute}. Estado: ${h(patrolVenture.status)}. Dois voos de dois cloud cars (quatro veículos).</small>${!patrolVenture.arrival?`<label><input type="checkbox" data-p221-physical-arrival> Mestre confirma chegada FÍSICA dos voos ao iate</label><label><input type="checkbox" data-p221-four-vehicles> Mestre confirma quatro veículos distintos (dois voos de dois)</label><button type="button" data-p221-venture-arrival>REGISTRAR CHEGADA REAL</button>`:'<strong>Chegada registrada com recibo, sem combate ou movimentação inventados.</strong>'}`)}</section>`:'';
 const residenceWingPending=status?.residenceWingPending||[];
 const residenceWingResponses=status?.residenceWingResponse||[];
 const wingFirst=residenceWingPending[0]||null;
 const residenceWingUi=adventureId==='jewel-of-yavin'&&arendHolding==='plaza-residence'&&status.active&&wingFirst?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91 · FALHA REAL · GUARDA ALADA</div><small>Teste falhou: intervalo ${wingFirst.sequence}, minuto ${wingFirst.atMinute}; recibo ${h(wingFirst.id)}. A ficha dos oficiais é SWE09 p.12. O livro não fixa o tamanho desta patrulha: confirme a quantidade FÍSICA. Outras facções perseguidoras NÃO aparecem automaticamente.</small><label>Quantidade real de oficiais <input type="number" min="1" max="8" value="2" data-p225-wing-count></label><label>Alcance físico <select data-p225-wing-range><option>Engajado</option><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option></select></label><label><input type="checkbox" data-p225-wing-arrival> Mestre confirma que a patrulha CHEGOU fisicamente</label><label><input type="checkbox" data-p225-wing-distinct> Mestre confirma oficiais distintos dos outros encontros</label><label><input type="checkbox" data-p225-wing-choice> Mestre escolheu a Guarda Alada como um dos grupos perseguidores</label><button type="button" data-p225-wing-start>INICIAR COMBATE REAL DA GUARDA ALADA</button><small>Falhas pendentes: ${residenceWingPending.length}; respostas registradas: ${residenceWingResponses.length}. Sem movimentação, dano ou vitória automática.</small></section>`:'';
 const p226Travel=status?.pursuitTravel||null;
 const p226Visit=p226Travel?.lastVisit||null,p226Check=p226Travel?.lastCheck||null;
 const p226TravelUi=adventureId==='jewel-of-yavin'&&status.active&&p226Visit?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91 · PERSEGUIÇÃO ENTRE ENDEREÇOS</div><small>Visita física ${h(p226Visit.holdingId)} (${h(p226Visit.id)}). O teste Difícil de Engano/Furtividade/Conhecimento das Ruas é feito UMA vez por novo endereço. Falha: 1 grupo; duas Ameaças: 2; Desespero: 3. Sem rolagem retroativa ou teleporte. Diferente da patrulha de 15 minutos na residência.</small>${!p226Check?`<label>Perícia real <select data-p226-skill><option value="engano">Engano</option><option value="sigilo">Furtividade</option><option value="callejeo">Conhecimento das Ruas</option></select></label><label><input type="checkbox" data-p226-location> Mestre confirma chegada física ao endereço</label><button type="button" data-p226-roll>ROLAR TESTE DIFÍCIL DA PERSEGUIÇÃO</button>`:`<strong>Rolagem real: ${p226Check.success?'evasão bem-sucedida':`falha · ${p226Check.slots} grupo(s) possível(is)`}. Recibo ${h(p226Check.id)}.</strong>${p226Travel.nextSlot!==null?`<small>Grupo ${p226Travel.nextSlot}/${p226Check.slots}: selecione facção realmente presente, sem duplicar pessoal. Grupos são resolvidos sequencialmente, sem vitória automática.</small><label>Facção <select data-p226-faction><option value="wing_guard">Guarda Alada</option><option value="grayson_security">Segurança privada de Grayson</option><option value="arend_goons">Capangas de Arend (sem Aris automática)</option></select></label><label>Quantidade física <input type="number" min="1" max="8" value="2" data-p226-count></label><label>Alcance físico <select data-p226-range><option>Engajado</option><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option></select></label><label><input type="checkbox" data-p226-arrival> Mestre confirma chegada física</label><label><input type="checkbox" data-p226-distinct> Mestre confirma indivíduos distintos de encontros anteriores</label><label><input type="checkbox" data-p226-choice> Mestre confirma a facção escolhida</label><button type="button" data-p226-start>INICIAR COMBATE REAL DA FACÇÃO</button>`:'<small>Não há outro grupo autorizado nesta visita, ou os encontros previstos já foram registrados.</small>'}`}</section>`:'';
 const stormIv=status?.stormIvPatrol||null;
 const stormIvOptions=['jewel-venture-storm-iv-1','jewel-venture-storm-iv-2','jewel-venture-storm-iv-3','jewel-venture-storm-iv-4'];
 const stormIvPending=stormIvOptions.filter(id=>!stormIv?.outcomes?.some(o=>o.vehicleId===id));
 const stormIvUi=patrolVenture?.arrival&&status?.progress?.currentNodeId==='cloud-city-pursuit'?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.95 · QUATRO STORM IV REAIS</div><small>Ficha oficial: Silhueta 2, Velocidade 4, Manejo +1, Blindagem 2, Casco 5, Tensão 7. Piloto minion: Agilidade 3, Artilharia apenas de grupo; sem graduação individual inventada. Quatro veículos separados, cada um com casco e combate persistentes.</small>${!stormIv?`<small>Selecione primeiro a nave do PJ na seção VEÍCULOS. A chegada física já deve estar confirmada.</small><label>Alcance real <select data-p223-range><option>Curto</option><option>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label><input type="checkbox" data-p223-positions> Mestre confirma posições físicas dos quatro carros</label><label><input type="checkbox" data-p223-pilots> Mestre confirma pilotos e artilheiros presentes nos veículos</label><button type="button" data-p223-deploy>REGISTRAR QUATRO VEÍCULOS REAIS</button>`:stormIv.cleared?'<strong>Patrulha resolvida com quatro provas individuais. PE ainda bloqueados até integração da recompensa.</strong>':`<small>Carros resolvidos: ${stormIv.outcomes.length}/4. ${stormIv.engaged?'Combate em curso: '+h(stormIv.engaged.vehicleId):'Nenhum combate da patrulha ativo.'}</small>${stormIvPending.length?`<label>Cloud car <select data-p223-target>${stormIvPending.map(id=>`<option value="${h(id)}">${h(id)}</option>`).join('')}</select></label><label>Alcance atual <select data-p223-range><option>Curto</option><option>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label><input type="checkbox" data-p223-range-gm> Mestre confirma posição e alcance atuais</label>${!stormIv.engaged?'<button type="button" data-p223-engage>INICIAR COMBATE VEICULAR DO CARRO SELECIONADO</button>':`<small>Use os ataques reais na seção VEÍCULOS. Para inutilizar, casco precisa EXCEDER o limiar após ataques registrados. Para evasão, role Pilotagem com dificuldade definida pelo Mestre.</small><label>Desfecho <select data-p223-outcome><option value="DISABLED">Inutilizado por dano real</option><option value="EVADED">Evadido após teste de Pilotagem real</option></select></label><label>Dificuldade adjudicada (somente evasão) <select data-p223-difficulty><option value="1">Fácil</option><option value="2">Média</option><option value="3">Difícil</option><option value="4">Formidável</option><option value="5">Desafiadora</option></select></label><label><input type="checkbox" data-p223-difficulty-gm> Mestre confirmou dificuldade da evasão</label><label><input type="checkbox" data-p223-outcome-gm> Mestre confirma o desfecho real do combate</label><button type="button" data-p223-resolve>REGISTRAR DESFECHO COM PROVA</button>`}`:''}${stormIv.outcomes.length===4&&!stormIv.engaged?`<label><input type="checkbox" data-p223-clear-gm> Mestre confirma os quatro carros resolvidos</label><button type="button" data-p223-clear>FECHAR PATRULHA COM QUATRO RECIBOS</button>`:''}`}</section>`:'';
 const bespinDeparture=status?.bespinDeparture||{custody:null,board:null,escape:null};
 const jewelHolder=currentState()?.world?.publishedAdventure?.operations?.jewelCustodyTransfers?.holderId||bespinDeparture.custody?.holderId||null;
 const playerJewelHolder=currentState()?.player?.character?.id||'player-slot';
 const jewelRecipients=[{id:playerJewelHolder,name:'Personagem jogador ativo'},...jewelCompanions.map(c=>({id:c.id,name:c.character?.name||c.id}))];
 const bespinShip=currentState()?.vehicles?.vehicles?.[currentState()?.vehicles?.activeVehicleId]||null;
 const bespinDestinations=Object.values(currentState()?.world?.locations||{}).filter(x=>x?.id&&!x.id.startsWith('joy.')&&!/bespin|cloud.city|cidade.das.nuvens/i.test(x.id+' '+x.name));
 const starportAttempts=status?.starportSecurity||[];
 const starportShipId=currentState()?.vehicles?.activeVehicleId||null;
 const starportAccess=starportAttempts.find(x=>x.shipId===starportShipId&&x.success)||null;
 const starportLast=starportAttempts.filter(x=>x.shipId===starportShipId).at(-1)||null;
 const p231=status?.starportCloudCars||null;
 const p231Wave=p231?.waves?.at(-1)||null;
 const p231Pending=p231Wave?.arrival?.vehicleIds?.filter(id=>!p231Wave.outcomes.some(o=>o.vehicleId===id))||[];
 const p231CanArrive=Boolean(bespinDeparture.board&&!bespinDeparture.escape&&(!p231Wave||p231Wave.cleared)&&(!p231||p231.waves.length<3));
 const p231Ui=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&bespinDeparture.board?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.94 · PERSEGUIÇÃO AÉREA · 12 STORM IV</div><small>Três esquadrilhas de QUATRO veículos distintos dos quatro do Venture. Somente após decolagem real e chegada física confirmadas pelo Mestre. Combate veicular individual; não há dano, teleporte, XP nem vitória automáticos. Piloto minion oficial: Agilidade 3 e Artilharia de grupo (sozinho, graduação 0).</small><strong>Esquadrilhas: ${p231?.waves?.length||0}/3 · veículos resolvidos: ${p231?.waves?.reduce((n,w)=>n+w.outcomes.length,0)||0}/12 · ${p231?.allTwelveResolved?'12 RESOLVIDOS COM PROVA':'EM ANDAMENTO'}</strong>${p231CanArrive?`<label>Alcance físico <select data-p231-range><option>Curto</option><option>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label><input type="checkbox" data-p231-airborne> Mestre confirma nave do PJ realmente no ar</label><label><input type="checkbox" data-p231-arrived> Mestre confirma chegada física da próxima esquadrilha</label><label><input type="checkbox" data-p231-positions> Mestre confirma posição real dos quatro carros</label><label><input type="checkbox" data-p231-pilots> Mestre confirma quatro tripulações distintas</label><button type="button" data-p231-arrival>REGISTRAR PRÓXIMA ESQUADRILHA (4 CARROS)</button>`:''}${p231Wave&&!p231Wave.cleared?`<strong>Esquadrilha ${p231Wave.arrival.wave} · ${p231Wave.outcomes.length}/4 resolvidos</strong>${p231Pending.length?`<label>Carro presente <select data-p231-target>${p231Pending.map(id=>`<option value="${h(id)}">${h(id)}</option>`).join('')}</select></label><label>Alcance <select data-p231-range><option>Curto</option><option>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label><input type="checkbox" data-p231-range-gm> Mestre confirma alcance real do alvo</label>${!p231.engaged?'<button type="button" data-p231-engage>INICIAR COMBATE REAL DO CARRO</button>':`<small>Resolva ataques na seção VEÍCULOS. Dano exige log real e casco acima do limiar; evasão exige rolagem de Pilotagem Planetária e dificuldade do Mestre.</small><label>Desfecho <select data-p231-outcome><option value="DISABLED">Inutilizado por dano real</option><option value="EVADED">Evadido por rolagem real</option></select></label><label>Dificuldade da evasão <select data-p231-difficulty><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option></select></label><label><input type="checkbox" data-p231-difficulty-gm> Mestre confirma dificuldade</label><label><input type="checkbox" data-p231-outcome-gm> Mestre confirma desfecho real</label><button type="button" data-p231-resolve>REGISTRAR DESFECHO INDIVIDUAL</button>`}`:''}${p231Wave.outcomes.length===4&&!p231.engaged?'<label><input type="checkbox" data-p231-four-gm> Mestre confirma quatro desfechos reais</label><button type="button" data-p231-clear>ENCERRAR ESQUADRILHA</button>':''}`:''}</section>`:'';
 const starportRoute=status?.starportRoutes||{route:null,destruction:null};
 const starportRouteContent=!starportRoute.route?`<label>Rota física <select data-p230-route><option value="MAINTENANCE">Passagens de manutenção</option><option value="SEWERS">Esgotos</option><option value="DEEPER_TO_SHIP">Avançar até a nave</option><option value="BLAST_OUT">Abrir caminho à força</option></select></label><label><input type="checkbox" data-p230-at-port> Mestre confirma chegada física ao porto</label><label><input type="checkbox" data-p230-route-physical> Mestre confirma rota fisicamente acessível</label><label><input type="checkbox" data-p230-security-position> Mestre confirma posição real da segurança</label><button type="button" data-p230-route-confirm>REGISTRAR ROTA REAL DO PORTO</button>`:
  starportRoute.destruction?`<strong>Destruição comprovada: +10 Obrigação Criminal · ${h(starportRoute.destruction.id)}</strong>`:
  starportRoute.route.route==='BLAST_OUT'?`<strong>Rota física: ${h(starportRoute.route.route)}</strong><label>ID do encontro publicado encerrado <input data-p230-combat-id value="holo-park-wing-guard"></label><label><input type="checkbox" data-p230-destroyed> Mestre confirma destruição FÍSICA do porto</label><label><input type="checkbox" data-p230-combat-damage> Mestre confirma destruição durante o combate real</label><button type="button" data-p230-destruction>REGISTRAR +10 OBRIGAÇÃO CRIMINAL COM PROVA</button>`:
  `<strong>Rota física confirmada: ${h(starportRoute.route.route)} · recibo ${h(starportRoute.route.id)}</strong>`;
 const starportRouteUi=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.94 · ROTAS FÍSICAS DO PORTO</div><small>Sem movimento, guarda ou combate inventados. +10 Obrigação Criminal apenas após destruição comprovada durante combate publicado real. A Guarda Alada do Holo-Park não é duplicada.</small>${starportRouteContent}</section>`:'';
 const starportSecurityUi=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&!bespinDeparture?.board?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.94 · SEGURANÇA REAL DO PORTO · NAVE DE TERCEIRO</div><small>Para tomar uma nave que não é sua: teste DIFÍCIL (3 dados roxos) de Computadores no terminal físico. A ficha, os dados e o resultado são persistidos. Falha NÃO concede acesso. A nave já deve existir no catálogo do grupo; nenhuma nave é criada aqui. A partida sem preparação aumenta UMA vez a dificuldade de Pilotagem Espacial durante a fuga (penalidade oficial aplicada às ações de Pilotagem Espacial da nave durante a fuga).</small><strong>Nave selecionada: ${h(bespinShip?.name||'nenhuma')}. ${starportAccess?'ACESSO APROVADO · '+h(starportAccess.id):starportLast?'Última tentativa: '+(starportLast.success?'sucesso':'falha'):'Sem tentativa'}</strong>${!starportAccess?`<label><input type="checkbox" data-p229-ship-physical> Mestre confirma nave de terceiro fisicamente acessível no porto</label><label><input type="checkbox" data-p229-ship-third-party> Mestre confirma que a nave NÃO pertence ao grupo</label><label><input type="checkbox" data-p229-terminal> Mestre confirma acesso real ao terminal de segurança</label>${starportLast?'<label><input type="checkbox" data-p229-retry> Mestre autoriza nova tentativa após falha</label>':''}<button type="button" data-p229-ship-access>ROLAR COMPUTADORES DIFÍCIL · ACESSO À NAVE</button>`:''}</section>`:'';
 const bespinDepartureUi=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.92–95 · FUGA REAL DE BESPIN COM A JOIA</div><small>Não confundir chegar ao hangar com escapar do planeta. O Mestre confirma a Joia física obtida no cofre, a segurança e o embarque. A nave REAL executa Astronavegação e avança o relógio do mundo. Sem salto bem-sucedido, nenhum recibo de fuga ou PE.</small>${!bespinDeparture.custody?`<label><input type="checkbox" data-p224-jewel-physical> Mestre confirma que o PJ realmente retirou e possui a Joia do cofre</label><label><input type="checkbox" data-p224-vault> Mestre confirma aquisição no cofre, não somente intenção narrativa</label><button type="button" data-p224-custody>REGISTRAR JOIA FÍSICA NO INVENTÁRIO</button>`:!bespinDeparture.board?`<strong>Joia física com ${h(jewelRecipients.find(x=>x.id===jewelHolder)?.name||jewelHolder)} (recibo ${h(bespinDeparture.custody.id)}).</strong><small>Nave ativa: ${h(bespinShip?.name||'nenhuma')}; hiperpropulsor: ${bespinShip?.hyperdrive?'SIM':'NÃO'}. Selecione uma nave real na seção VEÍCULOS.</small><label>Modo de embarque <select data-p229-ship-mode><option value="OWN_SHIP">Nave própria / passagem acordada</option><option value="BORROWED_SHIP">Nave de terceiro (exige Computadores Difícil aprovado)</option></select></label><label>Porto de embarque <select data-p224-port><option value="COMMERCIAL_STARPORT">Porto comercial de Cloud City</option><option value="PORT_TOWN">Port Town</option><option value="ARRANGED_PASSAGE">Passagem combinada com terceiro</option></select></label><label><input type="checkbox" data-p224-board-gm> Mestre confirma PJ fisicamente a bordo da nave selecionada</label>${jewelHolder&&jewelHolder!==(currentState()?.player?.character?.id||'player-slot')?`<label><input type="checkbox" data-p228-companion-present> Mestre confirma companheiro portador PRESENTE no porto com a Joia</label><label><input type="checkbox" data-p228-companion-boarded> Mestre confirma companheiro portador FISICAMENTE a bordo da MESMA nave</label>`:''}<label><input type="checkbox" data-p224-security-gm> Mestre confirma segurança e perseguição resolvidas na cena, sem vitória automática</label><button type="button" data-p224-board>REGISTRAR EMBARQUE REAL</button>`:!bespinDeparture.escape?`<strong>Joia a bordo da nave ${h(bespinDeparture.board.shipId)}; segurança resolvida pelo Mestre.</strong><label>Destino REAL fora de Bespin <select data-p224-destination>${bespinDestinations.map(x=>`<option value="${h(x.id)}">${h(x.name||x.id)}</option>`).join('')}</select></label>${!bespinDestinations.length?'<small>Cadastre primeiro uma localização externa a Bespin no Mundo Vivo; nenhum destino será inventado.</small>':''}<label>Rota <select data-p224-route><option value="MAJOR">Principal</option><option value="MINOR">Menor</option><option value="SECONDARY">Secundária</option><option value="UNCHARTED">Não mapeada</option><option value="HAZARDOUS">Perigosa</option></select></label><label><input type="checkbox" data-p224-launch-gm> Mestre confirma partida física, nave livre para decolar e Joia a bordo</label><label><input type="checkbox" data-p224-nav-gm> Mestre confirma rota e destino; executar dados REAIS de Astronavegação</label><button type="button" data-p224-escape>EXECUTAR SALTO REAL E VERIFICAR FUGA</button>`:`<strong>FUGA COMPROVADA · destino ${h(bespinDeparture.escape.destinationLocationId)} · salto e tempo persistidos.</strong><small>Recibo ${h(bespinDeparture.escape.id)}. Os 5 PE são concedidos separadamente, somente pelo botão de experiência e com confirmação do Mestre.</small>`}</section>`:'';
 const jewelTransferUi=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&bespinDeparture.custody&&!bespinDeparture.board&&jewelRecipients.length>1?`<section class="final-published-action-zone"><div class="final-published-action-title">P227 · CADEIA FÍSICA DE CUSTÓDIA DA JOIA</div><small>Portador atual: ${h(jewelRecipients.find(x=>x.id===jewelHolder)?.name||jewelHolder)}. A Joia existe UMA vez, no inventário real do portador. A transferência requer entrega física, ambos presentes e confirmação do Mestre. O embarque com companheiro portador exige confirmar separadamente sua presença e embarque na mesma nave.</small><label>Transferir para <select data-p227-jewel-recipient>${jewelRecipients.filter(x=>x.id!==jewelHolder).map(x=>`<option value="${h(x.id)}">${h(x.name)}</option>`).join('')}</select></label><label><input type="checkbox" data-p227-jewel-colocated> Mestre confirma os dois personagens fisicamente no mesmo local</label><label><input type="checkbox" data-p227-jewel-handoff> Mestre confirma entrega da Joia de mão em mão</label><button type="button" data-p227-jewel-transfer>TRANSFERIR JOIA ENTRE INVENTÁRIOS REAIS</button></section>`:'';
 const holoFactionUi=holoFactionReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.78,92–94 · OUTRAS FACÇÕES NO HOLO-PARK</div><small>Somente depois da Guarda Alada. Confirme indivíduos fisicamente presentes e DIFERENTES dos capangas anteriores. A quantidade não é fixa no texto: Mestre deve confirmar. Não inclui automaticamente Burr Naxis ou Gantel Dro.</small><label>Facção <select data-p217-faction><option value="arend_goons">Capangas de Arend (até 4 distintos)</option><option value="grayson_security">Segurança privada de Grayson (até 6)</option></select></label><label>Número presente <input type="number" min="1" max="6" value="2" data-p217-count></label><label>Distância física <select data-p217-range><option>Engajado</option><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option></select></label><label><input type="checkbox" data-p217-presence> Mestre confirma presença física nesta cena</label><label><input type="checkbox" data-p217-distinct> Mestre confirma indivíduos distintos, sem duplicação dos anteriores</label><label><input type="checkbox" data-p217-wing-clear> Mestre confirma que a Guarda Alada NÃO está engajada neste confronto</label><button type="button" data-p217-faction-start>INICIAR COMBATE REAL DA FACÇÃO</button></section>`:'';
 const holoWingUi=holoWingReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 P.94 · INTERCEPTAÇÃO REAL DA GUARDA ALADA</div><small>Dois grupos de DOIS oficiais e UM sargento; combate por iniciativa e ficha oficial. Somente a Guarda Alada entra automaticamente. Outras facções não são teleportadas nem duplicadas.</small><label><input type="checkbox" data-p209-holo-arrival> Mestre confirma chegada FÍSICA ao Holo-Park</label><label><input type="checkbox" data-p209-holo-wing> Mestre confirma Guarda Alada presente</label><button type="button" data-p209-holo-start>INICIAR ENCONTRO REAL + HOLO-PARK</button></section>`:'';
 const holoPark=status?.holoPark||null;
 const holoParkReady=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&escapeBomb&&arrow7Combat?.active;
 const holoParkUi=holoParkReady?`<section class="final-published-action-zone"><div class="final-published-action-title">GRAND GALAXY HOLO-PARK · SWE09 PP.92–94</div><small>Paisagem: ${h(holoPark?.landscape||'não iniciada')} · Rodadas reais: ${Number(holoPark?.roundCount||0)}/10 · ${h(holoPark?.status||'aguardando entrada')}. O cenário muda após cada DUAS rodadas. Sem avanço de tempo, teleporte ou fuga automática.</small>${!holoPark?`<label><input type="checkbox" data-holo-park-arrival> Mestre confirma chegada física ao Holo-Park</label><label><input type="checkbox" data-holo-park-pursuers> Mestre confirma perseguidores presentes</label><label>Facções presentes <select data-holo-park-factions multiple size="3"><option value="wing_guard">Guarda Alada</option><option value="arend_goons">Capangas de Arend</option><option value="grayson_security">Segurança de Grayson</option></select></label><button type="button" data-holo-park-enter>ENTRAR · ROLAR D100 DA PAISAGEM</button>`:holoPark.status==='ACTIVE'?`<label>Alcance real após movimentos do combate <select data-holo-park-range><option>Engajado</option><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option></select></label><label><input type="checkbox" data-holo-park-range-gm> Mestre confirma distância da rodada REAL (sem movimentação automática)</label><label><input type="checkbox" data-holo-park-gone> Mestre confirma perseguidores derrotados/dispersos</label><button type="button" data-holo-park-round>REGISTRAR RODADA ATUAL E PAISAGEM</button>`:!['ESCAPED','STILL_PURSUED'].includes(holoPark.status)?`<label><input type="checkbox" data-holo-park-end-gm> Mestre confirma desfecho e posição reais</label>${holoPark.status==='ROUND_LIMIT_PENDING_GM'?'<label>Após dez rodadas <select data-holo-park-end-choice><option value="STILL_PURSUED">Ainda perseguido</option><option value="ESCAPED">Escapou com confirmação do Mestre</option></select></label>':''}<button type="button" data-holo-park-end>REGISTRAR DESFECHO DO HOLO-PARK</button>`:'<strong>Desfecho persistido. Nenhum XP ou fuga de Bespin concedidos automaticamente.</strong>'}<label>Empate editorial no d100=47 <select data-holo-park-47><option value="christophsis">Christophsis</option><option value="coruscant">Coruscant</option></select></label><small>Tabela 3-1 imprime 47 em dois intervalos; decisão expressa do Mestre. Alguns efeitos de símbolos de combate permanecem sob adjudicação manual.</small></section>`:'';
 const holoNarrative=status?.holoNarrativeClock||null;
 const holoNarrativeNow=Number(currentState()?.world?.clock?.elapsedMinutes)||0;
 const holoNarrativeDue=holoNarrative?.nextDueMinute??null;
 const holoNarrativeUi=adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&!holoPark&&!(currentState()?.combat?.active)?`<section class="final-published-action-zone"><div class="final-published-action-title">P232 · HOLO-PARK NARRATIVO · SWE09 P.92</div><small>Relógio de tarefas confirmado: ${h(holoNarrativeNow)} min. Paisagem ${h(holoNarrative?.landscape||'ainda não iniciada')}. ${h(holoNarrative?.ended?'Saída confirmada':holoNarrativeDue===null?'Entrada pendente':holoNarrativeNow>=holoNarrativeDue?'MUDANÇA DE PAISAGEM PENDENTE':'Próxima mudança aos '+holoNarrativeDue+' min')}. Nenhum minuto é acrescentado por este painel. Não concede XP, fuga ou combate.</small>${!holoNarrative?`<label><input type="checkbox" data-p232-arrival> Mestre confirma chegada física ao Holo-Park</label><label><input type="checkbox" data-p232-narrative> Mestre confirma que a cena está em modo narrativo, sem combate estruturado</label><button type="button" data-p232-start>INICIAR PAISAGEM NARRATIVA (D100)</button>`:!holoNarrative.ended?`<label><input type="checkbox" data-p232-present> Mestre confirma que o grupo ainda está fisicamente no Holo-Park</label><button type="button" data-p232-shift ${holoNarrativeNow<holoNarrativeDue?'disabled':''}>REGISTRAR PRÓXIMO INTERVALO DE 2 MINUTOS</button><label><input type="checkbox" data-p232-exit-gm> Mestre confirma saída física do parque</label><button type="button" data-p232-exit>ENCERRAR MODO NARRATIVO</button>`:'<strong>Modo narrativo encerrado, sem desfecho de perseguição presumido.</strong>'}<label>Se o D100 cair em 47 <select data-p232-47><option value="christophsis">Christophsis</option><option value="coruscant">Coruscant</option></select></label><small>A Tabela 3-1 repete o resultado 47. Processar um intervalo pendente por vez; o relógio só avança por tarefas reais confirmadas.</small></section>`:'';
 const p233Vertigo=currentState()?.combat?.holoParkVertigo;
 const p233VertigoEvents=(currentState()?.combat?.log||[]).filter(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE');
 const p233VertigoUi=adventureId==='jewel-of-yavin'&&status.active&&
   (p233Vertigo?.active||p233VertigoEvents.length)?`<section class="final-published-action-zone"><div class="final-published-action-title">P233 · VERTIGEM DE KASHYYYK · SWE09</div><small>${p233Vertigo?.active?'PENALIDADE PENDENTE: o próximo teste REAL de ação do PJ rebaixa um dado amarelo para verde.':'Penalidade consumida; não reaplicar.'} Agora inclui Medicina e Aplicação de Estimulantes, além de perícias e ataques. A rolagem utiliza faces reais; nenhum NPC ou companheiro é penalizado automaticamente. ${p233VertigoEvents.length} consumo(s) registrados com prova de pool e salvamento.</small></section>`:'';
 const holoEffectsUi=holoParkReady&&holoPark?.status==='ACTIVE'&&holoPark?.landscape!=='projectors_off'?(()=>{
  const names={alderaan_thranta:'Alderaan · thranta: +1 dado de Benefício em ataques à distância por duas rodadas (Vantagem)',
   christophsis_shadow:'Christophsis · sombra: alvo recebe Defesa à distância 1 por uma rodada (Vantagem)',
   coruscant_building:'Coruscant · abrigo holográfico: Defesa à distância +2 até mover (Triunfo)',
   alderaan_nerfs:'Alderaan · manada de nerfs: Furtividade Média como manobra (Triunfo)',
   alderaan_upheaval:'Alderaan · abalo: 2 Tensão e perde uma manobra (Desespero do jogador)',
   christophsis_shatter:'Christophsis · estilhaços: atordoa alvo por uma rodada (Triunfo)',
   coruscant_speeder:'Coruscant · speeder: desorienta alvo por uma rodada (Vantagem)',
   kashyyyk_wroshyr:'Kashyyyk · folhas wroshyr: cobertura à distância por uma rodada (Vantagem)',
   christophsis_spotlight:'Christophsis · holofote: +1 Contratempo aos ataques contra o PJ até o fim do próximo turno (Ameaça NPC)',
   coruscant_transport:'Coruscant · transporte: +1 Contratempo nos ataques do PJ/companheiros presentes até mover (Ameaça NPC)',
   alderaan_lost_weapon:'Alderaan · arma cai na grama: recuperar próxima rodada/manobra ou 1 Tensão imediata (Ameaça NPC)',
   kashyyyk_lost_weapon:'Kashyyyk · pássaros agr: derrubam arma real (Ameaça NPC)',
   kashyyyk_vertigo:'Kashyyyk · vertigem: Disciplina Difícil e downgrade da próxima ação (Desespero NPC)',
   kashyyyk_insect:'Kashyyyk · inseto aliado: +2 Benefício na MESMA rolagem (Triunfo PJ; escolher ANTES de atacar)',
   christophsis_fountain:'Christophsis · fonte: NPC recebe Defesa à distância 2 até mover (Desespero NPC)',
   coruscant_vanish:'Coruscant · alvo desaparece: Furtividade NPC oposta à Percepção do PJ (Desespero NPC)'};
  const pendingNpc=(arrow7Combat?.holoParkPendingNpcThreat||[]).filter(x=>x.landscape===holoPark.landscape&&x.round===arrow7Combat.round);
  const pendingDespair=(arrow7Combat?.holoParkPendingNpcDespair||[]).filter(x=>x.landscape===holoPark.landscape&&x.round===arrow7Combat.round);
  const fountainTargets=arrow7Combat?.adversaries||[];
  const blockedCompanions=arrow7Combat?.holoParkTransport?.companionIds||[];
  const available=Object.entries(status.holoEffectRules||{}).filter(([id,r])=>id!=='kashyyyk_insect'&&r.landscape===holoPark.landscape);
  return `<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.93–94 · EFEITO REAL DA PAISAGEM</div><small>Aplica somente símbolos realmente rolados e não gastos: Vantagem/Triunfo do jogador, Ameaça/Desespero NPC. A posição física deve ser confirmada pelo Mestre.</small><label>Efeito da paisagem <select data-p210-holo-effect>${available.map(([id])=>`<option value="${h(id)}">${h(names[id]||id)}</option>`).join('')}</select></label><label><input type="checkbox" data-p212-herd> Mestre confirma manada holográfica e posição física para Furtividade (Alderaan)</label><label>Manobra de Furtividade <select data-p212-maneuver><option value="normal">Gratuita</option><option value="strain">Extra (2 Tensão)</option><option value="action">Trocar ação</option></select></label><label><input type="checkbox" data-p211-thranta-board> Mestre confirma que o personagem embarcou fisicamente na plataforma holográfica da thranta (somente Alderaan)</label><label>Ameaça REAL de ataque NPC <select data-p213-npc-attack><option value="">Selecionar ataque (somente efeitos de Ameaça)</option>${pendingNpc.map(x=>`<option value="${h(x.attackId)}">${h(x.attackerId)} · rodada ${x.round} · ${-x.check.netAdvantage} Ameaça(s)</option>`).join('')}</select></label><label>Desespero REAL de ataque NPC <select data-p215-npc-despair><option value="">Selecionar ataque com Desespero</option>${pendingDespair.map(x=>`<option value="${h(x.attackId)}">${h(x.attackerId)} · rodada ${x.round} · ${x.check.despair} Desespero(s)</option>`).join('')}</select></label><label>NPC alvo do efeito (presente fisicamente) <select data-p215-target>${fountainTargets.map(x=>`<option value="${h(x.id)}">${h(x.name||x.id)}</option>`).join('')}</select></label><label><input type="checkbox" data-p215-target-gm> Mestre confirma alvo presente e posição real junto à fonte/multidão</label>${fountainTargets.some(x=>x.holoParkFountain?.active||x.holoParkHiddenFromPlayer?.active)?`<section><label><input type="checkbox" data-p215-end-gm> Mestre confirma movimento real da fonte ou linha de visão restabelecida</label><label>NPC <select data-p215-end-target>${fountainTargets.filter(x=>x.holoParkFountain?.active||x.holoParkHiddenFromPlayer?.active).map(x=>`<option value="${h(x.id)}">${h(x.name||x.id)}</option>`).join('')}</select></label><button type="button" data-p215-end-fountain>REGISTRAR MOVIMENTO DA FONTE</button><button type="button" data-p215-end-hidden>CONFIRMAR NPC VISÍVEL</button></section>`:''}${pendingDespair.length?`<section><label><input type="checkbox" data-p215-despair-fallback-gm> Mestre confirma que o Desespero NPC não foi gasto na paisagem e escolhe Tensão do atacante</label><button type="button" data-p215-despair-fallback>APLICAR FALLBACK DE DESESPERO NPC</button></section>`:''}<label>Arma empunhada no momento do ataque NPC <select data-p214-weapon-index>${(currentState()?.player?.character?.weapons||[]).map((w,i)=>`<option value="${i}">${h(w.name)}</option>`).join('')}</select></label><label><input type="checkbox" data-p214-weapon-gm> Mestre confirma que a arma selecionada estava realmente empunhada</label><label>Companheiros realmente presentes sob o transporte (Mestre) <select data-p213-companions multiple size="${Math.max(2,Math.min(4,jewelCompanions.length))}">${jewelCompanions.map(c=>`<option value="${h(c.id)}">${h(c.character?.name||c.id)}</option>`).join('')}</select></label><label><input type="checkbox" data-p213-companion-gm> Mestre confirma presença dos companheiros selecionados na área do transporte (sem seleção = nenhum presente)</label>${arrow7Combat?.holoParkDroppedWeapon?.active?`<section><strong>Arma caída: ${h(arrow7Combat.holoParkDroppedWeapon.weaponName)}</strong><label><input type="checkbox" data-p214-recovery-gm> Mestre confirma recuperação física da arma</label><label>Recuperação <select data-p214-recovery-method><option value="strain">Imediata: 1 Tensão</option><option value="maneuver">Manobra na próxima rodada</option></select></label><button type="button" data-p214-recover>RECUPERAR ARMA REAL</button></section>`:''}${pendingNpc.length?`<section><label><input type="checkbox" data-p214-fallback-gm> Mestre decide gastar Ameaça NPC reservada como Tensão do atacante</label><button type="button" data-p214-fallback>APLICAR FALLBACK DE AMEAÇA NPC</button></section>`:''}${blockedCompanions.length?`<label>Companheiro que executou movimento <select data-p213-companion-move>${blockedCompanions.map(id=>`<option value="${h(id)}">${h(jewelCompanions.find(x=>x.id===id)?.character?.name||id)}</option>`).join('')}</select></label><label><input type="checkbox" data-p213-companion-moved> Mestre confirma manobra real de movimento desse companheiro</label><button type="button" data-p213-companion-clear>REGISTRAR MOVIMENTO DO COMPANHEIRO</button>`:''}<label><input type="checkbox" data-p210-holo-symbol> Mestre confirma símbolo narrativo e personagem/alvo presentes na paisagem atual</label><button type="button" data-p210-holo-apply>APLICAR EFEITO COM RECIBO</button><small>Recibos: ${status.holoEffects?.history?.length||0}. Sem criação de dados ou símbolos fictícios.</small></section>`;
 })():'';
 const companionImpactUi=(adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active&&escapeBomb&&jewelCompanions.length)?`<section class="final-published-action-zone"><div class="final-published-action-title">IMPACTOS DOS COMPANHEIROS · POSIÇÃO CONFIRMADA</div><label>Companheiro presente <select data-jewel-companion-target>${jewelCompanions.map(c=>`<option value="${h(c.id)}">${h(c.character?.name||c.id)}</option>`).join('')}</select></label><label><input type="checkbox" data-jewel-companion-gm> Mestre confirma presença real do companheiro no airspeeder (explosão) ou na trajetória de queda</label><label>Faixa de queda do companheiro (Mestre) <select data-jewel-companion-fall-range><option value="short">Curta</option><option value="medium">Média</option><option value="long">Longa</option><option value="extreme">Extrema</option></select></label><label>Perícia <select data-jewel-companion-fall-skill><option value="atletismo">Atletismo</option><option value="coordinacion">Coordenação</option></select></label><label><input type="checkbox" data-jewel-companion-fall-gm> Mestre confirma faixa real de queda do companheiro</label><label><input type="checkbox" data-jewel-companion-fall-triumph> Autorizar redução com Triunfo real</label><button type="button" data-jewel-companion-impact-blast>APLICAR EXPLOSÃO AO COMPANHEIRO PRESENTE</button><button type="button" data-jewel-companion-impact-fall>APLICAR QUEDA AO COMPANHEIRO PRESENTE</button><small>Recibos independentes por personagem; ausentes não recebem dano. Uma explosão e uma queda por companheiro. Queda usa a faixa e perícia selecionadas acima. Já resolvidos: ${Object.keys(escapeCompanionImpact?.byCompanion||{}).length} companheiro(s).</small></section>`:'';
 const escapeBombUi=(adventureId==='jewel-of-yavin'&&node.id==='cloud-city-pursuit'&&status.active)?(()=>{
  const st=escapeBomb?.status||'UNEXAMINED';
  const options=st==='UNEXAMINED'||st==='SUSPECTED'?[['detect','PROCURAR BOMBA (Mecânica / Computadores Média; Percepção Difícil)'],['steer','APROXIMAR PLATAFORMA (Pilotagem Média)']]:
   st==='DETECTED'?[['disarm','DESARMAR (Mecânica / Frieza Difícil)'],['steer','APROXIMAR PLATAFORMA (Pilotagem Média)'],['jump','SALTAR (Atletismo / Coordenação Média)']]:
   ['NEAR_PLATFORM','IN_FLIGHT'].includes(st)?[['jump','SALTAR (Atletismo / Coordenação)']]:
   st==='JEWEL_GRIP_PENDING'?[['secure_jewel','SEGURAR A JOIA (Coordenação Difícil)']]:[];
  const skillMap={detect:[['mecanica','Mecânica · Média'],['computadoras','Computadores · Média'],['percepcion','Percepção · Difícil']],disarm:[['mecanica','Mecânica · Difícil'],['frialdad','Frieza · Difícil']],steer:[['pilotar_vehiculo_planetario','Pilotagem (Planetária) · Média']],jump:[['atletismo','Atletismo'],['coordinacion','Coordenação']],secure_jewel:[['coordinacion','Coordenação · Difícil']]};
  return `<section class="final-published-action-zone"><div class="final-published-action-title">ARMADILHA DE AREND · AIRSPEEDER · SWE09</div><small>O explosivo é acionado 30 segundos após a entrada. Tempo narrativo crítico; nenhuma duração de rolagem foi inventada. Estado: ${h(st)}. ${escapeBomb?.blastDamageBeforeSoak?'Explosão: 10 de dano antes da Absorção. ':''}${escapeBomb?.fallDamagePending?'Queda: resolver pelas regras oficiais. ':''}</small>${(st==='EXPLODED'&&!escapeImpact?.blastApplied)?'<button type="button" data-jewel-impact-blast>APLICAR 10 DANO DA EXPLOSÃO (ABSORÇÃO REAL)</button>':''}${(escapeBomb?.fallDamagePending&&!escapeImpact?.fallApplied)?`<div><label>Faixa real da queda (Mestre) <select data-jewel-fall-range><option value="short">Curto · 10 dano / 10 Tensão</option><option value="medium">Médio · 30 dano / 20 Tensão</option><option value="long">Longo · incapacitação + crítico +50 / 30 Tensão</option><option value="extreme">Extremo · incapacitação + crítico +75 / 40 Tensão</option></select></label><label>Amortecer a queda <select data-jewel-fall-skill><option value="atletismo">Atletismo</option><option value="coordinacion">Coordenação</option></select></label><label><input type="checkbox" data-jewel-fall-gm> Mestre confirma distância real e plataforma de impacto</label><label><input type="checkbox" data-jewel-fall-triumph> Se houver Triunfo real, Mestre autoriza reduzir uma faixa</label><button type="button" data-jewel-impact-fall>ROLAR E APLICAR QUEDA (CRÍTICO + TENSÃO)</button><small>Longo/Extremo incapacitam e geram crítico +50/+75; morte em Extremo depende do Mestre. Triunfo sem resultado real não reduz faixa.</small></div>`:''}${options.length?`<label>Ação <select data-jewel-bomb-step>${options.map(([v,l])=>`<option value="${v}">${h(l)}</option>`).join('')}</select></label><label>Perícia <select data-jewel-bomb-skill></select></label><label><input type="checkbox" data-jewel-bomb-suspicion> Mestre confirma suspeita narrativa (apenas para dirigir sem detecção)</label><label><input type="checkbox" data-jewel-bomb-held> Personagem segura a joia durante o salto</label><label><input type="checkbox" data-jewel-bomb-stowed> Joia acondicionada em recipiente seguro</label><button type="button" data-jewel-bomb-roll>ROLAR E REGISTRAR CONSEQUÊNCIA</button>`:'<small>Etapa concluída ou consequências pendentes de adjudicação pelo Mestre.</small>'}</section>`;
 })():'';
 const jewelBankUi=(adventureId==='jewel-of-yavin'&&node.id==='central-bank-slice'&&status.active)?(()=>{
  const prep=status.bankPreparation||{},bank=status.bankStatus||{},evidence=status.bankEvidence||{},liveDroid=status.liveDroid||{},hiredSlicer=status.hiredSlicer||null;
  const credits=Number(currentState()?.player?.character?.credits)||0;
  const stage=String(bank.stage||'SEARCH');
  const receipts=Object.values(status.progress?.operations?.receipts||{});
  const lastBankStep=receipts.filter(r=>r?.kind==='BANK_STEP').at(-1);
  const bankSpend=status.bankSpending||{};
  const bankTools=status.bankTools||{};
  const bankSettlement=status.bankSettlement||null;
  const ownedBolt=(currentState()?.player?.character?.gear||[]).filter(x=>(x?.sourceCatalogId||x?.id)==='restraining_bolt').reduce((n,x)=>n+(Number(x?.quantity)||1),0);
  const adv=Math.max(0,Number(lastBankStep?.evidence?.roll?.netAdvantage)||0);
  const threats=Math.max(0,-Number(lastBankStep?.evidence?.roll?.netAdvantage)||0);
  const triumphs=Math.max(0,Number(lastBankStep?.evidence?.roll?.triumph)||0);
  const despairs=Math.max(0,Number(lastBankStep?.evidence?.roll?.despair)||0);
  const currentStrain=currentState()?.player?.character?.derived?.strain||{};
  const canSufferStrain=!lastBankStep?.slicerId&&Number.isInteger(currentStrain.current)&&Number.isInteger(currentStrain.threshold)&&currentStrain.threshold>currentStrain.current;
  const eligibleBidders=Object.values(status.progress?.operations?.auction?.bidders||{}).filter(b=>b?.id!==status.progress?.operations?.auction?.highBidderId&&b?.id!=='pos_podura'&&!bankSpend.additionalBidderIds?.includes(b.id));
  const spendPending=!bankSettlement&&lastBankStep?.step==='slice'&&!receipts.some(r=>r?.kind==='BANK_SLICE_SPEND'&&r.bankStepId===lastBankStep.id)&&((!lastBankStep.success&&adv>=1)||threats>=1||despairs>=1||(lastBankStep.success&&eligibleBidders.length&&(adv>=3||triumphs>=1)));
  const bankSpendUi=spendPending&&(adv||threats)?`<hr><div class="final-published-action-title">TABELA 2-1 · GASTAR SÍMBOLOS DA ÚLTIMA ROLAGEM</div>
    <small>Vantagens ${adv} · Ameaças ${threats} · Triunfos ${triumphs} · Desesperos ${despairs}. Cada escolha consome símbolos REAIS da última rolagem. Bônus de licitante é apenas direito condicionado, NÃO créditos pagos.</small>
    ${!lastBankStep.success?`<label>Reforços (1 Vantagem) <input type="number" data-jewel-spend-boost min="0" max="${adv}" value="0"></label>
    <label>Upgrades (2 Vantagens) <input type="number" data-jewel-spend-upgrade min="0" max="${Math.floor(adv/2)}" value="0"></label>`:''}
    ${lastBankStep.success&&eligibleBidders.length&&(adv>=3||triumphs>=1)?`<label>Outro licitante (+10.000 cr potenciais; 3 Vantagens ou 1 Triunfo) <select data-jewel-spend-bidder><option value="">Nenhum</option>${eligibleBidders.map(b=>`<option value="${h(b.id)}">${h(b.name)}</option>`).join('')}</select></label><label><input type="checkbox" data-jewel-spend-triumph ${triumphs?'':'disabled'}> Usar 1 Triunfo em vez de 3 Vantagens</label>`:''}
    <label>Atrasos de 15 min (2 Ameaças) <input type="number" data-jewel-spend-delay min="0" max="${Math.floor(threats/2)}" value="0"></label>
    ${canSufferStrain?`<label>Tensão no slicer (1 Ameaça) <input type="number" data-jewel-spend-strain min="0" max="${Math.min(threats,currentStrain.threshold-currentStrain.current)}" value="0"></label>`:''}
    <label>Patrulhas Wing Guard (3 Ameaças) <input type="number" data-jewel-spend-guards min="0" max="${Math.floor(threats/3)}" value="0"></label>
    <label>Acelerar patrulha (-1h, 3 Ameaças) <input type="number" data-jewel-spend-trace min="0" max="${Math.floor(threats/3)}" value="0"></label>
    ${despairs?`<label>Patrulhas (1 Desespero) <input type="number" data-jewel-spend-despair-guards min="0" max="${despairs}" value="0"></label><label>Acelerar patrulha (-1h, 1 Desespero) <input type="number" data-jewel-spend-despair-trace min="0" max="${despairs}" value="0"></label><label><input type="checkbox" data-jewel-spend-signature> Assinatura eletrônica (1 Desespero; upgrade de slicing futuro)</label>`:''}
    <button type="button" data-jewel-spend="${h(lastBankStep.id)}">APLICAR CONSEQUÊNCIAS</button>`:'';

  const detail=`Amostra: ${prep.sample==='full'?'completa':prep.sample==='basic'?'básica':'ausente'} · Lobot: ${prep.lobot==='neutralized'?'neutralizado':prep.lobot==='distracted'?'distraído':'ativo'} · Spike: ${prep.spike?(prep.spike.success?`+${prep.spike.boost} reforços`:'falhou'):'não preparado'} · Créditos: ${credits}`;
  return `<section class="final-published-action-zone final-jewel-bank">
   <div class="final-published-action-title">BANCO CENTRAL · PREPARAÇÃO E SLICING</div>
   <small>${h(detail)}</small><p>Preparações narrativas exigem confirmação explícita do Mestre. Fabricar spike custa 300 créditos e avança 10 horas no relógio persistente de tarefas; períodos do dia ainda não são sincronizados.</p>
   <label>Origem da amostra <select data-jewel-sample><option value="public_office_distraction">Escritório público (básica)</option><option value="arend_access_card">Cartão de Arend (completa)</option><option value="government_infiltration">Infiltração (completa)</option></select></label>
   <button type="button" data-jewel-prep="sample" ${prep.sample==='full'?'disabled':''}>REGISTRAR AMOSTRA</button>
   <label>Lobot <select data-jewel-lobot><option value="active">Ativo</option><option value="distracted">Distraído</option><option value="neutralized">Neutralizado</option></select></label>
   <label><input type="checkbox" data-jewel-bea> Bea alertou Lobot</label>
   <button type="button" data-jewel-prep="lobot" ${prep.lobot==='neutralized'&&prep.beaAlerted?'disabled':''}>REGISTRAR LOBOT</button>
   <label><input type="checkbox" data-jewel-gm> Mestre confirma os fatos acima</label>
   <button type="button" data-jewel-prep="spike" ${prep.spike||credits<300?'disabled':''}>FABRICAR SPIKE (300 CR)</button>
   <hr><div class="final-published-action-title">SLICER DE PORT TOWN · SWE09</div>
   <small>${h(hiredSlicer?`${hiredSlicer.name} contratado · ${hiredSlicer.paidCredits} créditos pagos`:'Contratar R3-8C (500–1.000 cr) ou Naboka (2.000–3.000 cr). Teste real de Manha; R3-8C exige Negociação Difícil. Preço final definido pelo Mestre.')}</small>
   ${!hiredSlicer?`<label>Slicer <select data-jewel-slicer><option value="r3_8c">R3-8C (Intelecto/Computadores 3/3)</option><option value="naboka">Naboka (4/5)</option></select></label><label>Preço confirmado pelo Mestre <input type="number" data-jewel-hire-price min="500" max="3000" value="1000" step="100"></label><label><input type="checkbox" data-jewel-port-town> Contato em Port Town confirmado</label><button type="button" data-jewel-hire>CONTRATAR SLICER</button>`:''}
   ${hiredSlicer?`<label><input type="checkbox" data-jewel-use-slicer ${status.slicerAvailability?.refused?'disabled':'checked'}> Usar o slicer contratado no slicing bancário</label>${status.slicerAvailability?.refused?'<strong>Naboka recusou novos testes após sua primeira falha (SWE09 p.69). O personagem pode tentar pessoalmente.</strong>':''}`:''}
   <hr><small>G-100: ${h(stage)} · ${Number(bank.attempts)||0}/4 tentativas · ${Number(bank.minutesSpent)||0} min de repetição · backdoor pendente: ${Number(bankSpend.pendingBoosts)||0} reforços / ${Number(bankSpend.pendingUpgrades)||0} upgrades · atraso adicional ${Number(bankSpend.delayMinutes)||0} min · licitantes extras ${bankSpend.additionalBidderIds?.length||0} (+${Number(bankSpend.bonusEntitlementCredits)||0} cr potenciais, NÃO pagos) · patrulhas ${bankSpend.wingGuardTeams?.length||0} (próxima chegada em ${bankSpend.wingGuardTeams?.length?Math.max(0,Math.min(...bankSpend.wingGuardTeams.map(x=>x.arrivalElapsedMinutes))-(Number(currentState()?.world?.clock?.elapsedMinutes)||0)):0} min) · assinatura ${bankSpend.signatureCaptured?(bankTools.signatureCleared?'ferramentas substituídas':'capturada — +1 upgrade em slicing'):'não capturada'}</small>
   ${bankSpendUi}
   ${stage==='FINISHED'&&!bankSettlement?`<hr><div class="final-published-action-title">LIQUIDAÇÃO BANCÁRIA · SWE09</div><small>Reconciliar a transferência com o lance vencedor real e os bônus de licitantes comprovados. O valor será lançado na conta bancária de destino, NÃO na carteira dos personagens. Sem saque ou pagamento automático de Arend.</small><label>Conta de destino <input data-jewel-settlement-account value="arend_shen" maxlength="80" placeholder="arend_shen"></label><label><input type="checkbox" data-jewel-settlement-gm> Mestre confirma conta bancária e encerramento dos gastos de símbolos</label><button type="button" data-jewel-settle>REGISTRAR LIQUIDAÇÃO DO BANCO</button>`:''}
   ${bankSettlement?`<hr><strong>LIQUIDAÇÃO ${h(bankSettlement.status)} · ${Number(bankSettlement.totalCredits)||0} CR</strong><small>Lance vencedor: ${Number(bankSettlement.winningBidCredits)||0} cr · licitantes adicionais: ${Number(bankSettlement.additionalBidderCredits)||0} cr · conta: ${h(bankSettlement.destinationAccount)}. Livro-razão do banco, sem créditos na carteira.</small>`:''}
   ${bankSpend.signatureCaptured&&!bankTools.signatureCleared?`<hr><strong>ASSINATURA ELETRÔNICA CAPTURADA</strong><p>Substituir integralmente o equipamento de slicing (500 cr, Tabela 5-8 EotE). O rastro histórico não é apagado; só cessa o upgrade em testes futuros.</p><label><input type="checkbox" data-jewel-replace-gm> Mestre confirma disponibilidade do novo kit</label><button type="button" data-jewel-replace-tools ${credits<500?'disabled':''}>COMPRAR E SUBSTITUIR FERRAMENTAS (500 CR)</button>`:''}
   ${stage==='SEARCH'?`<label>Posto <select data-jewel-target><option value="industrial_docks">Docas Industriais</option><option value="port_town">Port Town</option><option value="tourist_district">Distrito Turístico</option></select></label><small>Restraining bolts no inventário: ${ownedBolt} · preço oficial 35 cr (EotE Core, Tabela 5-8). Não é o dispositivo de retenção.</small><label><input type="checkbox" data-jewel-bolt ${ownedBolt?'':'disabled'}> Instalar restraining bolt físico do inventário no G-100</label><label><input type="checkbox" data-jewel-spot-welder> Mestre confirma soldador para fixação (dispensável se possui kit de ferramentas)</label><label><input type="checkbox" data-jewel-bolt-gm> Mestre confirma disponibilidade comercial</label><button type="button" data-jewel-buy-bolt ${credits<35?'disabled':''}>COMPRAR BOLT (35 CR)</button><button type="button" data-jewel-bank="acquire">CAPTURAR G-100</button>`:''}
   ${stage==='SECURED'?'<label>Contenção física (opcional): <select data-jewel-muscle><option value="">Sem ajuda</option><option value="atletismo">Atletismo · Difícil (sucesso: +2 Reforços)</option><option value="pelea">Briga · Difícil (sucesso: +2 Reforços)</option></select></label><button type="button" data-jewel-bank="disable">INCAPACITAR G-100</button>':''}
   ${stage==='READY'?'<button type="button" data-jewel-bank="slice">INVADIR BANCO</button>':''}
   ${stage==='FINISHED'&&!bank.evidenceCleared&&liveDroid.status!=='persuaded'?'<button type="button" data-jewel-bank="clean">DESMONTAR G-100 (2H) · EXTRAIR CAIXA-PRETA</button>':''}
   ${stage==='FINISHED'&&!bank.evidenceCleared&&liveDroid.status!=='persuaded'?'<label><input type="checkbox" data-jewel-berth> Mestre confirma alojamento real na nave</label><button type="button" data-jewel-live-droid>CONVENCER G-100 VIVO · CARISMA MÉDIA + NOBODY’S FOOL 3 (FRIEZA SEM GRAU)</button>':''}
   ${liveDroid.status==='persuaded'?'<strong>G-100 preservado vivo; caixa-preta permanece intacta. Não desmontar.</strong>':''}
   ${stage==='FINISHED'&&bank.evidenceCleared?`<hr><div class="final-published-action-title">PROVAS DO G-100 · SWE09</div><small>Caixa-preta: ${h(evidence.blackBox||'extraída; destino pendente')} · peças: ${h(evidence.parts||'destino pendente')}${evidence.blackBoxRisk?' · caixa roubada é prova incriminadora':''}. Nenhum pagamento automático.</small>
     ${!evidence.blackBox?'<button type="button" data-jewel-evidence="destroy_box">DESTRUIR CAIXA-PRETA</button><button type="button" data-jewel-evidence="take_box">LEVAR CAIXA-PRETA (RISCO)</button>':''}
     ${evidence.blackBox&&!evidence.parts?'<label><input type="checkbox" data-jewel-edge> Mestre confirma viagem até a borda de Cloud City</label><button type="button" data-jewel-evidence="dump_parts">DESCARTAR PEÇAS NA BORDA</button><button type="button" data-jewel-evidence="fence_parts">DISTRIBUIR PEÇAS · MANHA DIFÍCIL</button>':''}`:''}
   </section>`;
 })():'';
 const supportingUi=[systems?`<div class="final-published-systems">${systems}</div>`:'',npcCards?`<div class="final-published-npcs">${npcCards}</div>`:'',knownClues?`<section class="final-published-clues" aria-label="Pistas conhecidas"><div class="final-published-clues-title">PISTAS CONHECIDAS</div>${knownClues}</section>`:'',checkFeedback?`<div class="final-published-feedbacks">${checkFeedback}</div>`:'',consequences?`<div class="final-published-consequences">${consequences}</div>`:'',timelineUi].filter(Boolean).join('');
 const supportingDetails=supportingUi?`<details class="final-published-support"><summary>INFORMAÇÕES DA CENA <b>＋</b></summary><div>${supportingUi}</div></details>`:'';
 const lastHistory=(status?.progress?.checkHistory||[]).slice(-1)[0]||null;const lastFeedback=(status?.checkFeedback||[]).slice(-1)[0]||null;const immediateEffects=lastFeedback?.id===lastHistory?.checkId?(lastFeedback.effects||[]):[];const immediateEffectsUi=immediateEffects.length?`<ul class="final-published-last-roll-effects">${immediateEffects.map(e=>`<li class="is-${h(e.kind||'scene')}"><span>${h(effectLabel[e.kind]||String(e.kind||'EFEITO').toUpperCase())}${Number(e.count)>1?` ×${Number(e.count)}`:''}</span><strong>${h(e.summary)}</strong>${Number(e.obligationDelta)>0?`<em>Obligation +${Number(e.obligationDelta)}</em>`:''}</li>`).join('')}</ul>`:'';const lastRollUi=lastHistory?`<div class="final-published-last-roll ${lastHistory.success?'is-success':'is-failure'}" role="status"><small>ÚLTIMO TESTE · ${h(lastHistory.skillKey||'PERÍCIA')}</small><strong>${lastHistory.success?'SUCESSO':'FALHA'} · Sucessos ${Number(lastHistory.netSuccess)||0} · ${Number(lastHistory.netAdvantage)>=0?'Vantagem':'Ameaça'} ${Math.abs(Number(lastHistory.netAdvantage)||0)}</strong>${immediateEffectsUi}</div>`:'';
 panel.innerHTML=`<div class="final-published-scene-head"><div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;"><span>${h(adventureTitle).toUpperCase()} · EPISÓDIO ${Number(node.episode)||1}</span><button type="button" data-panel-campaign-hub style="min-height:28px;padding:3px 8px;font-size:0.68rem;background:#131d24;border:1px solid #eab308;color:#fef08a;border-radius:5px;font-weight:800;cursor:pointer;">☰ CAMPANHA</button></div><strong>${h(node.title)}</strong><small>${h(node.summary)}</small></div>${trackerUi}${uaeObjectivesUi}${imperialClockUi}${jewelBankUi}${escapeBombUi}${companionImpactUi}${arrow7Ui}${arrow7ChaseUi}${holoWingUi}${holoFactionUi}${arendPropertyUi}${arendPatrolUi}${residenceWingUi}${p226TravelUi}${stormIvUi}${p231Ui}${starportRouteUi}${starportSecurityUi}${bespinDepartureUi}${jewelTransferUi}${p222XpUi}${shenUi}${landoPardonUi}${holoParkUi}${holoNarrativeUi}${p233VertigoUi}${holoEffectsUi}${arendRevengeUi}${jewelPatrolUi}${encounterUi}${checks?`<section class="final-published-action-zone"><div class="final-published-action-title">TESTES DISPONÍVEIS</div><ul class="final-published-checks">${checks}</ul></section>`:''}${lastRollUi}${choices?`<section class="final-published-action-zone"><div class="final-published-action-title">O QUE FAZER</div><div class="final-published-choices">${choices}</div></section>`:noChoiceUi}${supportingDetails}<output aria-live="polite"></output>`;
 panel.querySelector("[data-panel-campaign-hub]")?.addEventListener("click",r54OpenCampaignHub);const recovery=window.__BRASEIRO_PUBLISHED_RECOVERY__;if(recovery&&!recovery.ok){const notice=panel.querySelector('output');if(notice)notice.textContent='RECUPERAÇÃO PENDENTE: '+String(recovery.message||'Repita a ação anterior antes de continuar.')}
 panel.querySelectorAll('[data-jewel-prep]').forEach(btn=>btn.addEventListener('click',()=>{
  const step=btn.dataset.jewelPrep,gmConfirmed=Boolean(panel.querySelector('[data-jewel-gm]')?.checked);
  if(step!=='spike'&&!gmConfirmed){panel.querySelector('output').textContent='Confirmação explícita do Mestre obrigatória.';return}
  const details={actionId:'p188-'+step+'-'+Date.now()+'-'+Math.floor(Math.random()*1e6),step,gmConfirmed};
  if(step==='sample')details.method=panel.querySelector('[data-jewel-sample]')?.value;
  if(step==='lobot'){details.mode=panel.querySelector('[data-jewel-lobot]')?.value;details.beaAlerted=Boolean(panel.querySelector('[data-jewel-bea]')?.checked)}
  btn.disabled=true;const r=bridgeResult('prepareJewelBank',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Preparação não registrada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Preparação bancária registrada e salva. '+(r.data?.adjudication?.success===false?'Teste de fabricação falhou.':'');
 }));
 panel.querySelector('[data-jewel-hire]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-hire]');
  const details={actionId:'p190-hire-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   slicerId:panel.querySelector('[data-jewel-slicer]')?.value,
   finalPrice:Number(panel.querySelector('[data-jewel-hire-price]')?.value),
   gmConfirmed:Boolean(panel.querySelector('[data-jewel-gm]')?.checked),
   portTownContact:Boolean(panel.querySelector('[data-jewel-port-town]')?.checked)};
  btn.disabled=true;const r=bridgeResult('hireJewelBankSlicer',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Contratação recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=r.data?.hiredSlicer?`Slicer ${r.data.hiredSlicer.name} contratado, custo debitado e recibo persistido.`:'Contato não localizado; nenhum crédito debitado.';
 });
 panel.querySelector('[data-jewel-spend]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-spend]');
  const bidder=panel.querySelector('[data-jewel-spend-bidder]')?.value||'';
  const choice={backdoorBoosts:Number(panel.querySelector('[data-jewel-spend-boost]')?.value)||0,
   backdoorUpgrades:Number(panel.querySelector('[data-jewel-spend-upgrade]')?.value)||0,
   delay15:Number(panel.querySelector('[data-jewel-spend-delay]')?.value)||0,
   extraBidderIds:bidder?[bidder]:[],extraBidderTriumphs:bidder&&panel.querySelector('[data-jewel-spend-triumph]')?.checked?1:0,
   slicerStrain:Number(panel.querySelector('[data-jewel-spend-strain]')?.value)||0,
   wingGuardTeams:Number(panel.querySelector('[data-jewel-spend-guards]')?.value)||0,
   wingGuardAccelerations:Number(panel.querySelector('[data-jewel-spend-trace]')?.value)||0,
   wingGuardDespairTeams:Number(panel.querySelector('[data-jewel-spend-despair-guards]')?.value)||0,
   wingGuardDespairAccelerations:Number(panel.querySelector('[data-jewel-spend-despair-trace]')?.value)||0,
   signatureDespair:panel.querySelector('[data-jewel-spend-signature]')?.checked?1:0};
  const details={actionId:'p191-spend-'+Date.now()+'-'+Math.floor(Math.random()*1e6),bankStepId:btn.dataset.jewelSpend,choice};
  btn.disabled=true;const r=bridgeResult('spendJewelBankSymbols',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Gasto de símbolos recusado.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Tabela 2-1: símbolos, efeitos, patrulhas e bônus condicionais registrados e persistidos.';
 });
 const bombStep=panel.querySelector('[data-jewel-bomb-step]'),bombSkill=panel.querySelector('[data-jewel-bomb-skill]');
 if(bombStep&&bombSkill){
  const skills={detect:[['mecanica','Mecânica'],['computadoras','Computadores'],['percepcion','Percepção']],disarm:[['mecanica','Mecânica'],['frialdad','Frieza']],steer:[['pilotar_vehiculo_planetario','Pilotagem Planetária']],jump:[['atletismo','Atletismo'],['coordinacion','Coordenação']],secure_jewel:[['coordinacion','Coordenação']]};
  const refresh=()=>{bombSkill.replaceChildren(...(skills[bombStep.value]||[]).map(([value,label])=>{const o=document.createElement('option');o.value=value;o.textContent=label;return o}))};
  bombStep.addEventListener('change',refresh);refresh();
  panel.querySelector('[data-jewel-bomb-roll]')?.addEventListener('click',()=>{
   const btn=panel.querySelector('[data-jewel-bomb-roll]');btn.disabled=true;
   const details={actionId:'p201-bomb-'+Date.now()+'-'+Math.floor(Math.random()*1e6),step:bombStep.value,skill:bombSkill.value,
    gmSuspicionConfirmed:Boolean(panel.querySelector('[data-jewel-bomb-suspicion]')?.checked),
    jewelHeld:Boolean(panel.querySelector('[data-jewel-bomb-held]')?.checked),
    jewelStowed:Boolean(panel.querySelector('[data-jewel-bomb-stowed]')?.checked)};
   const r=bridgeResult('resolveJewelEscapeBomb',details);
   if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Rolagem da bomba recusada.';return}
   enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
   if(out)out.textContent=`${details.step}: ${r.data?.adjudication?.success?'SUCESSO':'FALHA'} · ${r.data?.escapeBomb?.status} · salvo com recibo.`;
  });
 }
 for(const mode of ['blast','fall'])panel.querySelector(`[data-jewel-impact-${mode}]`)?.addEventListener('click',()=>{
  const btn=panel.querySelector(`[data-jewel-impact-${mode}]`);btn.disabled=true;
  const details={actionId:'p202-impact-'+Date.now()+'-'+Math.floor(Math.random()*1e6),mode};
  if(mode==='fall'){
   details.fallRange=panel.querySelector('[data-jewel-fall-range]')?.value;
   details.skill=panel.querySelector('[data-jewel-fall-skill]')?.value;
   details.gmConfirmed=Boolean(panel.querySelector('[data-jewel-fall-gm]')?.checked);
   details.gmTriumphReduce=Boolean(panel.querySelector('[data-jewel-fall-triumph]')?.checked);
  }
  const r=bridgeResult('resolveJewelEscapeImpact',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Impacto recusado.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`${mode==='blast'?'Explosão':'Queda'}: ${r.data?.adjudication?.woundsAdded||0} ferimentos e ${r.data?.adjudication?.strainAdded||0} Tensão aplicados na ficha e persistidos.`;
 });
 const p221Id=(label)=>'p221-'+label+'-'+Date.now()+'-'+Math.floor(Math.random()*1e6);
 const p221Action=(selector,method,details,message)=>panel.querySelector(selector)?.addEventListener('click',()=>{
  const btn=panel.querySelector(selector);btn.disabled=true;
  const r=bridgeResult(method,{actionId:p221Id(selector.replace(/[^a-z0-9]/gi,'')),...details()});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Ação temporal recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=message;
 });
 p221Action('[data-p221-residence-start]','recordJewelArendPatrol',()=>({type:'RESIDENCE_START',gmOutdoorsConfirmed:Boolean(panel.querySelector('[data-p221-outdoors]')?.checked)}),'Exposição à Guarda Alada registrada no relógio persistente.');
 p221Action('[data-p221-residence-end]','recordJewelArendPatrol',()=>({type:'RESIDENCE_END',gmLeftOutdoorsConfirmed:Boolean(panel.querySelector('[data-p221-outdoors-end]')?.checked)}),'Saída da rua registrada sem apagar patrulhas devidas.');
 p221Action('[data-p221-patrol-roll]','resolveJewelResidencePatrolCheck',()=>({skillKey:panel.querySelector('[data-p221-patrol-skill]')?.value}),'Teste Difícil rolado no motor de dados real; consequências aguardam Mestre.');
 p221Action('[data-p225-wing-start]','beginJewelResidenceWingResponse',()=>({checkId:wingFirst?.id,count:Number(panel.querySelector('[data-p225-wing-count]')?.value),range:panel.querySelector('[data-p225-wing-range]')?.value,gmPhysicalArrivalConfirmed:Boolean(panel.querySelector('[data-p225-wing-arrival]')?.checked),gmDistinctOfficersConfirmed:Boolean(panel.querySelector('[data-p225-wing-distinct]')?.checked),gmWingGuardChosenConfirmed:Boolean(panel.querySelector('[data-p225-wing-choice]')?.checked)}),'Guarda Alada fisicamente presente: iniciativa e combate reais iniciados.');
 p221Action('[data-p226-roll]','resolveJewelPursuitTravelCheck',()=>({visitId:p226Visit?.id,skillKey:panel.querySelector('[data-p226-skill]')?.value,gmNewLocationConfirmed:Boolean(panel.querySelector('[data-p226-location]')?.checked)}),'Rolagem real de perseguição registrada; sem NPCs automáticos.');
 p221Action('[data-p226-start]','beginJewelPursuitGroup',()=>({checkId:p226Check?.id,slot:p226Travel?.nextSlot,faction:panel.querySelector('[data-p226-faction]')?.value,count:Number(panel.querySelector('[data-p226-count]')?.value),range:panel.querySelector('[data-p226-range]')?.value,gmPhysicalArrivalConfirmed:Boolean(panel.querySelector('[data-p226-arrival]')?.checked),gmDistinctPersonnelConfirmed:Boolean(panel.querySelector('[data-p226-distinct]')?.checked),gmFactionChosenConfirmed:Boolean(panel.querySelector('[data-p226-choice]')?.checked)}),'Facção fisicamente confirmada: iniciativa e combate reais.');
 p221Action('[data-p221-venture-alert]','recordJewelArendPatrol',()=>({type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:Boolean(panel.querySelector('[data-p221-hostilities]')?.checked),gmTransponderActivatedConfirmed:Boolean(panel.querySelector('[data-p221-transponder]')?.checked)}),'Transponder e prazo oficial dos cloud cars registrados.');
 p221Action('[data-p221-venture-arrival]','recordJewelArendPatrol',()=>({type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:Boolean(panel.querySelector('[data-p221-physical-arrival]')?.checked),gmFourDistinctVehiclesConfirmed:Boolean(panel.querySelector('[data-p221-four-vehicles]')?.checked)}),'Dois voos de dois cloud cars chegaram fisicamente, sem combate automático.');
 const p223Action=(selector,method,details,success)=>panel.querySelector(selector)?.addEventListener('click',()=>{
  const btn=panel.querySelector(selector);btn.disabled=true;
  const r=bridgeResult(method,{actionId:'p223-'+Date.now()+'-'+Math.floor(Math.random()*1e6),...details()});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Ação Storm IV recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');if(out)out.textContent=success;
 });
 p223Action('[data-p223-deploy]','deployJewelStormIvPatrol',()=>({range:panel.querySelector('[data-p223-range]')?.value,
  gmPositionsConfirmed:Boolean(panel.querySelector('[data-p223-positions]')?.checked),
  gmPilotsPresentConfirmed:Boolean(panel.querySelector('[data-p223-pilots]')?.checked)}),'Quatro Storm IV e pilotos oficiais registrados no motor veicular.');
 p223Action('[data-p223-engage]','beginJewelStormIvCombat',()=>({vehicleId:panel.querySelector('[data-p223-target]')?.value,
  range:panel.querySelector('[data-p223-range]')?.value,
  gmRangeConfirmed:Boolean(panel.querySelector('[data-p223-range-gm]')?.checked)}),'Combate veicular iniciado contra UM carro físico.');
 p223Action('[data-p223-resolve]','resolveJewelStormIvCombat',()=>({outcome:panel.querySelector('[data-p223-outcome]')?.value,
  difficulty:Number(panel.querySelector('[data-p223-difficulty]')?.value),
  gmDifficultyConfirmed:Boolean(panel.querySelector('[data-p223-difficulty-gm]')?.checked),
  gmOutcomeConfirmed:Boolean(panel.querySelector('[data-p223-outcome-gm]')?.checked)}),'Desfecho veicular registrado com dados ou danos reais; sem PE antecipados.');
 p223Action('[data-p223-clear]','clearJewelStormIvPatrol',()=>({gmAllFourResolvedConfirmed:Boolean(panel.querySelector('[data-p223-clear-gm]')?.checked)}),
  'Patrulha encerrada após quatro desfechos comprovados; XP ainda exige integração específica.');
 const p224Action=(selector,method,details,success)=>panel.querySelector(selector)?.addEventListener('click',()=>{
  const btn=panel.querySelector(selector);btn.disabled=true;
  const r=bridgeResult(method,{actionId:'p224-'+Date.now()+'-'+Math.floor(Math.random()*1e6),...details()});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Fuga de Bespin não comprovada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=method==='executeJewelBespinEscape'&&!r.data?.escaped?'Salto real não concluiu a fuga. Dados, dano e consequência persistidos; PE bloqueados.':success;
 });
 p224Action('[data-p232-start]','enterJewelHoloNarrativeClock',()=>({gmParkArrivalConfirmed:Boolean(panel.querySelector('[data-p232-arrival]')?.checked),gmNarrativeModeConfirmed:Boolean(panel.querySelector('[data-p232-narrative]')?.checked),gm47Choice:panel.querySelector('[data-p232-47]')?.value}),'Holo-Park narrativo iniciado no relógio persistente.');
 p224Action('[data-p232-shift]','advanceJewelHoloNarrativeClock',()=>({gmStillPresentConfirmed:Boolean(panel.querySelector('[data-p232-present]')?.checked),gm47Choice:panel.querySelector('[data-p232-47]')?.value}),'Paisagem alterada após dois minutos reais confirmados.');
 p224Action('[data-p232-exit]','endJewelHoloNarrativeClock',()=>({gmExitConfirmed:Boolean(panel.querySelector('[data-p232-exit-gm]')?.checked)}),'Saída física do Holo-Park narrativo confirmada.');
 p224Action('[data-p231-arrival]','arriveJewelStarportSquadron',()=>({range:panel.querySelector('[data-p231-range]')?.value,
  gmAirborneConfirmed:Boolean(panel.querySelector('[data-p231-airborne]')?.checked),
  gmSquadronArrivedConfirmed:Boolean(panel.querySelector('[data-p231-arrived]')?.checked),
  gmPositionsConfirmed:Boolean(panel.querySelector('[data-p231-positions]')?.checked),
  gmDistinctPilotsConfirmed:Boolean(panel.querySelector('[data-p231-pilots]')?.checked)}),'Esquadrilha física de quatro Storm IV registrada.');
 p224Action('[data-p231-engage]','beginJewelStarportCloudCombat',()=>({vehicleId:panel.querySelector('[data-p231-target]')?.value,
  range:panel.querySelector('[data-p231-range]')?.value,gmRangeConfirmed:Boolean(panel.querySelector('[data-p231-range-gm]')?.checked)}),'Combate veicular real iniciado contra um Storm IV do porto.');
 p224Action('[data-p231-resolve]','resolveJewelStarportCloudCombat',()=>({outcome:panel.querySelector('[data-p231-outcome]')?.value,
  difficulty:Number(panel.querySelector('[data-p231-difficulty]')?.value),
  gmDifficultyConfirmed:Boolean(panel.querySelector('[data-p231-difficulty-gm]')?.checked),
  gmOutcomeConfirmed:Boolean(panel.querySelector('[data-p231-outcome-gm]')?.checked)}),'Desfecho do carro persistido com prova de dano ou rolagem.');
 p224Action('[data-p231-clear]','clearJewelStarportSquadron',()=>({gmFourResolvedConfirmed:Boolean(panel.querySelector('[data-p231-four-gm]')?.checked)}),'Esquadrilha encerrada após quatro provas individuais.');
 p224Action('[data-p230-route-confirm]','confirmJewelStarportRoute',()=>({route:panel.querySelector('[data-p230-route]')?.value,gmAtStarportConfirmed:Boolean(panel.querySelector('[data-p230-at-port]')?.checked),gmRoutePhysicallyAccessibleConfirmed:Boolean(panel.querySelector('[data-p230-route-physical]')?.checked),gmSecurityPositionConfirmed:Boolean(panel.querySelector('[data-p230-security-position]')?.checked)}),'Rota real confirmada; nenhum combate ou movimento inventado.');
 p224Action('[data-p230-destruction]','registerJewelStarportDestruction',()=>({routeReceiptId:starportRoute.route?.id,combatEncounterId:panel.querySelector('[data-p230-combat-id]')?.value,gmPhysicalDestructionConfirmed:Boolean(panel.querySelector('[data-p230-destroyed]')?.checked),gmDestructionDuringCombatConfirmed:Boolean(panel.querySelector('[data-p230-combat-damage]')?.checked)}),'Obrigação Criminal +10 registrada com combate real e prova física.');
 p224Action('[data-p229-ship-access]','attemptJewelStarportShipAccess',()=>({shipId:currentState()?.vehicles?.activeVehicleId,
  gmShipPhysicallyAccessibleConfirmed:Boolean(panel.querySelector('[data-p229-ship-physical]')?.checked),
  gmThirdPartyShipConfirmed:Boolean(panel.querySelector('[data-p229-ship-third-party]')?.checked),
  gmTerminalAccessConfirmed:Boolean(panel.querySelector('[data-p229-terminal]')?.checked),
  gmRetryAuthorized:Boolean(panel.querySelector('[data-p229-retry]')?.checked)}),'Teste de Computadores real registrado. Verifique sucesso antes de embarcar.');
 p224Action('[data-p224-custody]','registerJewelCustody',()=>({gmPhysicalJewelConfirmed:Boolean(panel.querySelector('[data-p224-jewel-physical]')?.checked),gmVaultAcquisitionConfirmed:Boolean(panel.querySelector('[data-p224-vault]')?.checked)}),'Joia única registrada na ficha real, com recibo de custódia.');
 p224Action('[data-p227-jewel-transfer]','transferJewelCustody',()=>({toHolderId:panel.querySelector('[data-p227-jewel-recipient]')?.value,
  gmCoLocatedConfirmed:Boolean(panel.querySelector('[data-p227-jewel-colocated]')?.checked),
  gmHandOffConfirmed:Boolean(panel.querySelector('[data-p227-jewel-handoff]')?.checked)}),'Joia transferida fisicamente entre inventários reais; recibo persistido.');
 p224Action('[data-p224-board]','registerJewelBoarding',()=>({port:panel.querySelector('[data-p224-port]')?.value,gmBoardedConfirmed:Boolean(panel.querySelector('[data-p224-board-gm]')?.checked),gmSecurityResolvedConfirmed:Boolean(panel.querySelector('[data-p224-security-gm]')?.checked),...(panel.querySelector('[data-p229-ship-mode]')?.value==='BORROWED_SHIP'?{accessMode:'BORROWED_SHIP',accessReceiptId:starportAccess?.id}:{}),...(panel.querySelector('[data-p228-companion-present]')?{carrierCompanionId:currentState()?.world?.publishedAdventure?.operations?.jewelCustodyTransfers?.holderId,gmCompanionPresentConfirmed:Boolean(panel.querySelector('[data-p228-companion-present]')?.checked),gmCompanionBoardedConfirmed:Boolean(panel.querySelector('[data-p228-companion-boarded]')?.checked)}:{})}),'Embarque real registrado na nave selecionada, sem salto inventado.');
 p224Action('[data-p224-escape]','executeJewelBespinEscape',()=>({destinationLocationId:panel.querySelector('[data-p224-destination]')?.value,routeType:panel.querySelector('[data-p224-route]')?.value,gmDepartureConfirmed:Boolean(panel.querySelector('[data-p224-launch-gm]')?.checked),gmNavigationConfirmed:Boolean(panel.querySelector('[data-p224-nav-gm]')?.checked)}),'Fuga de Bespin comprovada por Astronavegação real, nave e relógio persistidos.');
 panel.querySelector('[data-p222-xp-grant]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p222-xp-grant]');btn.disabled=true;
  const milestone=panel.querySelector('[data-p222-xp-milestone]')?.value;
  const r=bridgeResult('grantJewelEpisode3Reward',{actionId:'p222-xp-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   milestone,gmAwardConfirmed:Boolean(panel.querySelector('[data-p222-xp-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'PE recusados: marco ainda não comprovado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`+${r.data?.adjudication?.xp} PE oficiais aplicados à ficha do PJ, com recibo persistido. Outros marcos continuam independentes.`;
 });
 panel.querySelector('[data-p220-start]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p220-start]');btn.disabled=true;
  const r=bridgeResult('beginJewelArendPropertyEncounter',{
   actionId:'p220-property-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   holdingId:arendHolding,stage:panel.querySelector('[data-p220-stage]')?.value,
   range:panel.querySelector('[data-p220-range]')?.value,
   gmPhysicalPresenceConfirmed:Boolean(panel.querySelector('[data-p220-presence]')?.checked),
   gmDistinctPersonnelConfirmed:Boolean(panel.querySelector('[data-p220-distinct]')?.checked),
   gmAccessConfirmed:Boolean(panel.querySelector('[data-p220-access]')?.checked),
   gmSecurityBypassedConfirmed:Boolean(panel.querySelector('[data-p220-bypass]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Combate não iniciado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Combate oficial do imóvel iniciado com fichas e iniciativa reais. Nenhuma vitória ou recompensa presumida.';
 });
 panel.querySelector('[data-p219-arend-record]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p219-arend-record]');btn.disabled=true;
  const status=panel.querySelector('[data-p219-arend-status]')?.value;
  const outcome=Boolean(panel.querySelector('[data-p219-arend-result]')?.checked);
  const r=bridgeResult('recordJewelArendOutcome',{
   actionId:'p219-arend-'+Date.now()+'-'+Math.floor(Math.random()*1e6),status,
   gmArendIdentified:Boolean(panel.querySelector('[data-p219-arend-identity]')?.checked),
   gmPhysicalConfrontationConfirmed:Boolean(panel.querySelector('[data-p219-arend-confront]')?.checked),
   gmOutcomeConfirmed:outcome,gmDeathConfirmed:outcome&&status==='DEAD',
   gmCustodyConfirmed:outcome&&status==='INCARCERATED',gmEscapeConfirmed:outcome&&status==='AT_LARGE'});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Desfecho não registrado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Destino de Arend registrado com recibo. Nenhum XP ou recompensa concedido automaticamente.';
 });
 panel.querySelector('[data-p219-aris-record]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p219-aris-record]');btn.disabled=true;
  const r=bridgeResult('recordJewelArisAlliance',{
   actionId:'p219-aris-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   gmArisMetInPerson:Boolean(panel.querySelector('[data-p219-aris-meeting]')?.checked),
   gmArisAgreedToAlliance:Boolean(panel.querySelector('[data-p219-aris-agree]')?.checked),
   gmPartyAcceptedAlliance:Boolean(panel.querySelector('[data-p219-aris-party]')?.checked),
   gmArisNoLongerHostile:Boolean(panel.querySelector('[data-p219-aris-peace]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Aliança não registrada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Aliança com Aris registrada com recibo. XP depende de adjudicação separada.';
 });
 panel.querySelector('[data-p218-lando-grant]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p218-lando-grant]');btn.disabled=true;
  const members=(panel.querySelector('[data-p218-party-names]')?.value||'').split(',').map(x=>x.trim()).filter(Boolean);
  const roster=Boolean(panel.querySelector('[data-p218-roster]')?.checked);
  const r=bridgeResult('grantJewelLandoPardon',{
   actionId:'p218-lando-'+Date.now()+'-'+Math.floor(Math.random()*1e6),members,
   gmPartyRosterConfirmed:roster,gmMembersArePlayerCharactersConfirmed:roster,
   gmLandoAudienceConfirmed:Boolean(panel.querySelector('[data-p218-audience]')?.checked),
   gmRestorationPlanAccepted:Boolean(panel.querySelector('[data-p218-plan]')?.checked),
   gmPardonGranted:Boolean(panel.querySelector('[data-p218-pardon]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Perdão recusado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Perdão de Lando registrado; +5 de Obrigação: Favor para cada PJ confirmado, sem duplicação.';
 });
 panel.querySelector('[data-p217-faction-start]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p217-faction-start]');btn.disabled=true;
  const r=bridgeResult('beginJewelHoloParkFaction',{
   actionId:'p217-holo-faction-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   faction:panel.querySelector('[data-p217-faction]')?.value,
   count:Number(panel.querySelector('[data-p217-count]')?.value),
   range:panel.querySelector('[data-p217-range]')?.value,
   gmParkPresenceConfirmed:Boolean(panel.querySelector('[data-p217-presence]')?.checked),
   gmDistinctPersonnelConfirmed:Boolean(panel.querySelector('[data-p217-distinct]')?.checked),
   gmWingGuardNotEngagedConfirmed:Boolean(panel.querySelector('[data-p217-wing-clear]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Facção recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Facção confirmada pelo Mestre: combate real iniciado, com iniciativa e ficha publicada.';
 });
 panel.querySelector('[data-p209-holo-start]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p209-holo-start]');btn.disabled=true;
  const r=bridgeResult('beginJewelHoloParkWingGuard',{
   actionId:'p209-holo-wing-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   gmParkArrivalConfirmed:Boolean(panel.querySelector('[data-p209-holo-arrival]')?.checked),
   gmPursuersConfirmed:Boolean(panel.querySelector('[data-p209-holo-wing]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Encontro recusado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Guarda Alada: dois grupos de dois oficiais e sargento. Iniciativa e Holo-Park registrados.';
 });
 panel.querySelector('[data-holo-park-enter]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-holo-park-enter]');btn.disabled=true;
  const r=bridgeResult('enterJewelHoloPark',{actionId:'p208-holo-enter-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   gmParkArrivalConfirmed:Boolean(panel.querySelector('[data-holo-park-arrival]')?.checked),
   gmPursuersConfirmed:Boolean(panel.querySelector('[data-holo-park-pursuers]')?.checked),
   factions:Array.from(panel.querySelector('[data-holo-park-factions]')?.selectedOptions||[]).map(x=>x.value),
   gm47Choice:panel.querySelector('[data-holo-park-47]')?.value});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Entrada recusada.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Holo-Park iniciado: '+r.data.adjudication.landscape+'.';
 });
 panel.querySelector('[data-holo-park-round]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-holo-park-round]');btn.disabled=true;
  const r=bridgeResult('recordJewelHoloParkRound',{actionId:'p208-holo-round-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   range:panel.querySelector('[data-holo-park-range]')?.value,
   gmDistanceConfirmed:Boolean(panel.querySelector('[data-holo-park-range-gm]')?.checked),
   gmPursuersNeutralizedConfirmed:Boolean(panel.querySelector('[data-holo-park-gone]')?.checked),
   gm47Choice:panel.querySelector('[data-holo-park-47]')?.value});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Rodada recusada.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Holo-Park: rodada '+r.data.adjudication.round+' · '+r.data.adjudication.landscape+'.';
 });
 panel.querySelector('[data-holo-park-end]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-holo-park-end]');btn.disabled=true;
  const r=bridgeResult('confirmJewelHoloParkOutcome',{actionId:'p208-holo-end-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   gmOutcomeConfirmed:Boolean(panel.querySelector('[data-holo-park-end-gm]')?.checked),
   outcome:panel.querySelector('[data-holo-park-end-choice]')?.value||'ESCAPED'});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Desfecho recusado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Holo-Park: '+r.data.adjudication.outcome+' confirmado.';
 });
 panel.querySelector('[data-p210-holo-apply]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p210-holo-apply]');btn.disabled=true;
  const r=bridgeResult('applyJewelHoloParkEffect',{actionId:'p210-holo-effect-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   effect:panel.querySelector('[data-p210-holo-effect]')?.value,
   gmSymbolConfirmed:Boolean(panel.querySelector('[data-p210-holo-symbol]')?.checked),
   gmBoardedThrantaConfirmed:Boolean(panel.querySelector('[data-p211-thranta-board]')?.checked),
   gmHerdConfirmed:Boolean(panel.querySelector('[data-p212-herd]')?.checked),
   maneuverMethod:panel.querySelector('[data-p212-maneuver]')?.value||'normal',
   npcAttackId:['christophsis_fountain','coruscant_vanish','kashyyyk_vertigo'].includes(panel.querySelector('[data-p210-holo-effect]')?.value)?panel.querySelector('[data-p215-npc-despair]')?.value||null:panel.querySelector('[data-p213-npc-attack]')?.value||null,
   targetId:panel.querySelector('[data-p215-target]')?.value||null,
   gmTargetPresentConfirmed:Boolean(panel.querySelector('[data-p215-target-gm]')?.checked),
   weaponIndex:Number(panel.querySelector('[data-p214-weapon-index]')?.value??0),
   gmWeaponHeldConfirmed:Boolean(panel.querySelector('[data-p214-weapon-gm]')?.checked),
   gmCompanionsPresentConfirmed:Boolean(panel.querySelector('[data-p213-companion-gm]')?.checked),
   affectedCompanionIds:[...panel.querySelector('[data-p213-companions]')?.selectedOptions||[]].map(x=>x.value)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Efeito recusado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Efeito real aplicado: '+r.data.adjudication.effect+'.';
 });
 panel.querySelector('[data-p215-end-fountain]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p215-end-fountain]');btn.disabled=true;
  const r=bridgeResult('confirmJewelHoloNpcFountainMovement',{targetId:panel.querySelector('[data-p215-end-target]')?.value,
   gmMovementConfirmed:Boolean(panel.querySelector('[data-p215-end-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Movimento recusado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='NPC moveu-se fisicamente: cobertura da fonte encerrada.';
 });
 panel.querySelector('[data-p215-end-hidden]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p215-end-hidden]');btn.disabled=true;
  const r=bridgeResult('confirmJewelHoloNpcVisibility',{targetId:panel.querySelector('[data-p215-end-target]')?.value,
   gmVisibleConfirmed:Boolean(panel.querySelector('[data-p215-end-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Visibilidade recusada.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Linha de visão real confirmada pelo Mestre.';
 });
 panel.querySelector('[data-p215-despair-fallback]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p215-despair-fallback]');btn.disabled=true;
  const r=bridgeResult('settleJewelHoloNpcDespair',{npcAttackId:panel.querySelector('[data-p215-npc-despair]')?.value,
   gmFallbackConfirmed:Boolean(panel.querySelector('[data-p215-despair-fallback-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Fallback recusado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Desespero NPC não gasto: Tensão/Ferimento do atacante registrado.';
 });
 panel.querySelector('[data-p214-recover]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p214-recover]');btn.disabled=true;
  const r=bridgeResult('recoverJewelHoloWeapon',{method:panel.querySelector('[data-p214-recovery-method]')?.value,
    gmRecoveryConfirmed:Boolean(panel.querySelector('[data-p214-recovery-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Recuperação recusada.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Arma recuperada com consequência real.';
 });
 panel.querySelector('[data-p214-fallback]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p214-fallback]');btn.disabled=true;
  const r=bridgeResult('settleJewelHoloNpcThreat',{npcAttackId:panel.querySelector('[data-p213-npc-attack]')?.value,
    gmFallbackConfirmed:Boolean(panel.querySelector('[data-p214-fallback-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Fallback recusado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Ameaça NPC aplicada à Tensão/Ferimentos do atacante real.';
 });
 panel.querySelector('[data-p213-companion-clear]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p213-companion-clear]');btn.disabled=true;
  const r=bridgeResult('confirmJewelHoloCompanionMovement',{
   companionId:panel.querySelector('[data-p213-companion-move]')?.value,
   gmMovementConfirmed:Boolean(panel.querySelector('[data-p213-companion-moved]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Movimento não autorizado.';return;}
  enhancePublishedAdventurePanel();document.querySelector('.final-published-adventure-panel output').textContent='Movimento real do companheiro registrado.';
 });
 panel.querySelector('[data-arend-search]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-arend-search]');btn.disabled=true;
  const r=bridgeResult('investigateJewelArendHoldings',{actionId:'p207-arend-leads-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   skill:panel.querySelector('[data-arend-skill]')?.value,gmMainframeAccessConfirmed:Boolean(panel.querySelector('[data-arend-mainframe]')?.checked),
   gmRetryAuthorized:Boolean(panel.querySelector('[data-arend-retry]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Pesquisa recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=r.data?.adjudication?.discovered?'Pesquisa bem-sucedida: quatro imóveis encontrados. Arend ainda não foi localizado.':'Pesquisa falhou. Nova tentativa depende do Mestre.';
 });
 panel.querySelector('[data-arend-visit]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-arend-visit]');btn.disabled=true;
  const r=bridgeResult('visitJewelArendHolding',{actionId:'p207-arend-visit-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   holdingId:panel.querySelector('[data-arend-holding]')?.value,gmSearchConfirmed:Boolean(panel.querySelector('[data-arend-visit-gm]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Visita recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=r.data?.adjudication?.found?'Arend encontrado: confirmação da localização persistida. Mestre decide encontro.':`Arend ausente; d100 ${r.data?.adjudication?.d100}, limite ${r.data?.adjudication?.threshold}.`;
 });
 panel.querySelector('[data-jewel-chase-roll]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-chase-roll]');btn.disabled=true;
  const v=key=>panel.querySelector(`[data-jewel-chase-${key}]`),n=key=>v(key)?.value===''?null:Number(v(key)?.value),yes=key=>Boolean(v(key)?.checked);
  const details={actionId:'p206-arrow7-chase-'+Date.now()+'-'+Math.floor(Math.random()*1e6),playerRole:v('role')?.value,initialRange:v('range')?.value,
   arrow7Pilot:{id:v('npc-id')?.value,agility:n('npc-agility'),rank:n('npc-rank')},terrainDifficulty:n('difficulty'),terrainSetback:n('setback'),
   gmBothVehiclesConfirmed:yes('both'),gmTerrainConfirmed:yes('terrain'),gmStartingRangeConfirmed:yes('start'),gmNpcPilotSheetConfirmed:yes('pilot'),
   gmSpeedsVerified:yes('speeds'),playerSpeed:n('player-speed'),arrow7Speed:n('arrow-speed')};
  const r=bridgeResult('resolveJewelArrow7Chase',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Perseguição recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Perseguição rodada ${r.data?.adjudication?.round}: ${r.data?.adjudication?.newRange} (escala planetária), ${r.data?.adjudication?.status}.`;
 });
 panel.querySelector('[data-jewel-chase-end]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-chase-end]');btn.disabled=true;
  const details={actionId:'p206-arrow7-end-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   outcome:chaseState?.status==='CONTACT_PENDING_GM'?'CONTACT':'ESCAPED',gmConfirmed:Boolean(panel.querySelector('[data-jewel-chase-end-gm]')?.checked)};
  const r=bridgeResult('confirmJewelArrow7ChaseOutcome',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Desfecho recusado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Desfecho ${r.data?.adjudication?.outcome} confirmado pelo Mestre e persistido. Encerre o encontro separadamente.`;
 });
 panel.querySelector('[data-jewel-arrow7-queue]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-arrow7-queue]');btn.disabled=true;
  const st=currentState(),goon=st?.combat?.adversaries?.find(a=>a.profileKey==='shens_goons'&&a.kind==='minion');
  const details={actionId:'p205-arrow7-'+Date.now()+'-'+Math.floor(Math.random()*1e6),adversaryId:goon?.id,targetRange:goon?.range,
   gmGunnerConfirmed:Boolean(panel.querySelector('[data-jewel-arrow7-gunner]')?.checked),gmAftArcConfirmed:Boolean(panel.querySelector('[data-jewel-arrow7-aft]')?.checked)};
  const r=bridgeResult('queueJewelArrow7Turret',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Canhão Arrow-7 recusado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Canhão Arrow-7 preparado com arco traseiro confirmado. Será disparado na iniciativa real dos capangas.';
 });
 for(const mode of ['blast','fall'])panel.querySelector(`[data-jewel-companion-impact-${mode}]`)?.addEventListener('click',()=>{
  const btn=panel.querySelector(`[data-jewel-companion-impact-${mode}]`);btn.disabled=true;
  const details={actionId:'p204-companion-'+Date.now()+'-'+Math.floor(Math.random()*1e6),mode,
   companionId:panel.querySelector('[data-jewel-companion-target]')?.value,
   gmPresenceConfirmed:Boolean(panel.querySelector('[data-jewel-companion-gm]')?.checked),
   position:mode==='blast'?'inside-speeder':'falling'};
  if(mode==='fall'){
   details.fallRange=panel.querySelector('[data-jewel-companion-fall-range]')?.value;
   details.skill=panel.querySelector('[data-jewel-companion-fall-skill]')?.value;
   details.gmConfirmed=Boolean(panel.querySelector('[data-jewel-companion-fall-gm]')?.checked);
   details.gmTriumphReduce=Boolean(panel.querySelector('[data-jewel-companion-fall-triumph]')?.checked);
  }
  const r=bridgeResult('resolveJewelCompanionImpact',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Impacto de companheiro recusado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Companheiro ${details.companionId}: ${r.data?.adjudication?.impactReceipt?.woundsAdded||0} ferimentos, ${r.data?.adjudication?.impactReceipt?.strainAdded||0} Tensão. Recibo individual persistido.`;
 });
 panel.querySelector('[data-jewel-settle]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-settle]');btn.disabled=true;
  const details={actionId:'p200-settle-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   destinationAccount:panel.querySelector('[data-jewel-settlement-account]')?.value?.trim()||'',
   gmConfirmed:Boolean(panel.querySelector('[data-jewel-settlement-gm]')?.checked)};
  const r=bridgeResult('settleJewelBank',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Liquidação bancária recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Banco: ${r.data?.settlement?.totalCredits||0} cr registrados na conta ${r.data?.settlement?.destinationAccount||''}. Nenhum crédito concedido à carteira.`;
 });
 panel.querySelector('[data-jewel-buy-bolt]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-buy-bolt]');btn.disabled=true;
  const details={actionId:'p199-bolt-'+Date.now()+'-'+Math.floor(Math.random()*1e6),gmApproved:Boolean(panel.querySelector('[data-jewel-bolt-gm]')?.checked)};
  const r=bridgeResult('buyJewelRestrainingBolt',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Compra do bolt recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Bolt físico adquirido por 35 créditos e adicionado ao inventário.';
 });
 panel.querySelector('[data-jewel-replace-tools]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-replace-tools]');btn.disabled=true;
  const details={actionId:'p199-tools-'+Date.now()+'-'+Math.floor(Math.random()*1e6),gmApproved:Boolean(panel.querySelector('[data-jewel-replace-gm]')?.checked)};
  const r=bridgeResult('replaceJewelSlicerTools',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Substituição das ferramentas recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Equipamento de slicing novo: 500 créditos debitados, assinatura antiga não penaliza futuras rolagens.';
 });
 panel.querySelectorAll('[data-jewel-bank]').forEach(btn=>btn.addEventListener('click',()=>{
  const step=btn.dataset.jewelBank,details={actionId:'p188-bank-'+Date.now()+'-'+Math.floor(Math.random()*1e6),step};
  if(step==='disable')details.muscleAssistSkill=panel.querySelector('[data-jewel-muscle]')?.value||null;
  if(step==='slice')details.useHiredSlicer=Boolean(panel.querySelector('[data-jewel-use-slicer]')?.checked);
  if(step==='acquire'){details.target=panel.querySelector('[data-jewel-target]')?.value;details.restrainingBolt=Boolean(panel.querySelector('[data-jewel-bolt]')?.checked);details.spotWelderConfirmed=Boolean(panel.querySelector('[data-jewel-spot-welder]')?.checked)}
  btn.disabled=true;const r=bridgeResult('resolveJewelBankStep',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Operação bancária não registrada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`G-100 · ${step} · ${r.data?.adjudication?.success?'SUCESSO':'FALHA'} · resultado persistido.`;
 }));
 panel.querySelector('[data-jewel-live-droid]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-live-droid]');btn.disabled=true;
  const details={actionId:'p195-droid-'+Date.now()+'-'+Math.floor(Math.random()*1e6),berthConfirmed:Boolean(panel.querySelector('[data-jewel-berth]')?.checked)};
  const r=bridgeResult('resolveJewelLiveDroid',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Negociação com G-100 recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=r.data?.adjudication?.success?'G-100 aceitou a nave; droid preservado e recibo persistido.':'G-100 recusou; a alternativa de desmontagem continua disponível.';
 });
 panel.querySelectorAll('[data-jewel-evidence]').forEach(btn=>btn.addEventListener('click',()=>{
  const action=btn.dataset.jewelEvidence,details={actionId:'p194-evidence-'+Date.now()+'-'+Math.floor(Math.random()*1e6),action};
  if(action==='dump_parts')details.reachedCityEdge=Boolean(panel.querySelector('[data-jewel-edge]')?.checked);
  btn.disabled=true;const r=bridgeResult('resolveJewelBankEvidence',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Decisão sobre provas recusada.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Provas G-100 · ${action} · ${r.data?.adjudication?.success===false?'FALHA':'registrado'} · sem pagamento automático.`;
 }));
 panel.querySelector('[data-p156-imperial-tick]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p156-imperial-tick]');btn.disabled=true;
  const r=window.BraseiroPublishedActionClient.execute('imperial-clock',adventureId,{minutes:5});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Não foi possível avançar o cerco imperial.';return}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=(r.data?.eventsAdded||[]).map(e=>e.summary).join(' · ')||'Relógio imperial avançado e salvo.';
 });
 panel.querySelectorAll('[data-published-check]').forEach(btn=>btn.addEventListener('click',async()=>{const out=panel.querySelector('output'),index=Number(btn.dataset.publishedCheck),spec=(node.checks||[])[index]||{};btn.disabled=true;btn.textContent='ROLANDO…';out.textContent=`Rolando ${String(spec.skill||'teste')} · ${String(spec.difficulty||'dificuldade definida')}…`;const r=window.BraseiroPublishedActionClient.execute('check',adventureId,{checkIndex:index});if(!r.ok){btn.disabled=false;btn.textContent='ROLAR';out.textContent=r.message||'Não foi possível resolver o teste.';return}const x=r.data?.check||{},a=r.data?.adjudication||{},ce=r.data?.characterEffects||{};const verdict=x.success?'SUCESSO':'FALHA';const parts=[verdict,`Sucessos ${Number(x.netSuccess)||0}`,`${Number(x.netAdvantage)>=0?'Vantagem':'Ameaça'} ${Math.abs(Number(x.netAdvantage)||0)}`];if(Number(x.triumph)>0)parts.push(`Triunfo ${x.triumph}`);if(Number(x.despair)>0)parts.push(`Desespero ${x.despair}`);if(a.chase)parts.push(`Perseguição ${Number(a.chase.lead)>0?'+':''}${Number(a.chase.lead)||0}`);if(a.systems?.chase&&adventureId==='beyond-the-rim')parts.push(`Perseguição Yiyar ${Number(a.systems.chase.lead)>0?'+':''}${Number(a.systems.chase.lead)||0}`);if(Number(a.systems?.imperialExposure)>0)parts.push(`Exposição ${Number(a.systems.imperialExposure)}/6`);if(a.sabacc)parts.push(`Sabacc ${Number(a.sabacc.creditsDelta)>=0?'+':''}${Number(a.sabacc.creditsDelta)||0} cr`);if(a.zelcomm)parts.push(`Zelcomm ${Number(a.zelcomm.alert)||0}/6${a.zelcomm.lockdown?' LOCKDOWN':''}`);if(a.novacore)parts.push(`NovaCORE ${Number(a.novacore.hazard)||0}/6${a.novacore.critical?' CRÍTICO':''}`);if(Number(ce.creditsDelta))parts.push(`${Number(ce.creditsDelta)>0?'+':''}${Number(ce.creditsDelta)} cr`);if(Number(ce.strainDelta)>0)parts.push(`Tensão +${Number(ce.strainDelta)}`);if(Number(a.momentum)>0)parts.push(`Momentum ${a.momentum}/6`);if(Number(a.alertClock)>0)parts.push(`Alerta ${a.alertClock}/6`);const immediate=(Array.isArray(a.narrativeEffects)?a.narrativeEffects:[]).map(e=>`${effectLabel[e?.kind]||String(e?.kind||'EFEITO').toUpperCase()}: ${String(e?.summary||'').trim()}`).filter(x=>!x.endsWith(': '));const summary=parts.join(' · ');out.textContent=`${String(spec.skill||'Teste')} · ${summary}`;if(immediate.length)out.textContent+=` · ${immediate.join(' · ')}`;out.classList.add('is-result');btn.textContent='ROLADO';const visual=await boundedVisualRoll(x,{title:`${String(spec.skill||'Teste')} · ${String(spec.difficulty||'')}`,status:'Resultado mecânico registrado.'});setTimeout(()=>visual?.close?.(),1300);enhancePublishedAdventurePanel();const refreshed=document.querySelector('.final-published-adventure-panel output');if(refreshed)refreshed.textContent=`${String(spec.skill||'Teste')} · ${summary}`;if(masterNarrationAvailable()){if(refreshed)refreshed.textContent+=` · Mestre narrando a consequência…`;const sent=await bridgeAsync('submitPlayerIntent',`[RESULTADO_DE_TESTE_PUBLICADO] A aventura publicada já resolveu o teste ${String(spec.skill||'')} (${String(spec.difficulty||'')}). Resultado público: ${summary}. Narre apenas a consequência desta cena em PT-BR, sem refazer a rolagem, sem revelar informação oculta ou nós futuros e sem decidir pelo personagem.`);const finalOut=document.querySelector('.final-published-adventure-panel output');if(finalOut)finalOut.textContent=`${String(spec.skill||'Teste')} · ${summary}${sent?.ok?' · Consequência narrada pelo Mestre.':' · Resultado preservado; Mestre indisponível.'}`;if(sent?.ok)setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration()},0)}}));
 panel.querySelectorAll('[data-published-choice]').forEach(btn=>btn.addEventListener('click',()=>{btn.disabled=true;const r=window.BraseiroPublishedActionClient.execute('choice',adventureId,{choiceId:btn.dataset.publishedChoice});if(!r.ok){btn.disabled=false;panel.querySelector('output').textContent=r.message||'Não foi possível avançar.';return}const reward=r.data?.rewards;if(reward)panel.querySelector('output').textContent=`Recompensa aplicada: +${reward.credits||0} créditos${reward.obligationReduced?` · Obligation -${reward.obligationReduced}`:''}.`;setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();enhancePublishedAdventurePanel();enhanceSceneGeminiButton()},0)}));
 panel.querySelectorAll('[data-published-encounter]').forEach(btn=>btn.addEventListener('click',()=>{const out=panel.querySelector('output');btn.disabled=true;btn.textContent='INICIANDO…';const r=window.BraseiroPublishedActionClient.execute('encounter',adventureId,{encounterId:btn.dataset.publishedEncounter});if(!r.ok){btn.disabled=false;btn.textContent='TENTAR NOVAMENTE';out.textContent=r.message||'Não foi possível iniciar o encontro.';return}const n=Array.isArray(r.data?.combat?.adversaries)?r.data.combat.adversaries.length:0;out.textContent=`Combate iniciado${n?` · ${n} grupo(s)/adversário(s)`:''}.`;setTimeout(()=>navigateRoute('02'),250)}));
 panel.querySelector('[data-continue-published]')?.addEventListener('click',()=>{const btn=panel.querySelector('[data-continue-published]'),out=panel.querySelector('output');btn.disabled=true;btn.textContent='ABRINDO MUNDO LIVRE…';const r=bridgeResult('continuePublishedAdventureToSandbox',{adventureId});if(!r.ok){btn.disabled=false;btn.textContent='CONTINUAR NO MUNDO LIVRE';out.textContent=r.message||'Não foi possível continuar no mundo livre.';return}out.textContent='Missão arquivada. As consequências continuam ativas no mundo vivo.';setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();enhancePublishedAdventurePanel();enhanceSceneGeminiButton()},0)});
}

function currentSceneRecord(){try{const state=safeParse(localStorage.getItem('braseiro.starwars.edge.state.v1'),null);return state&&state.scene&&typeof state.scene==='object'?state.scene:null}catch{return null}}
function sceneGeminiPrompt(){
 const view=playerView()||{},scene=currentSceneRecord()||{};
 const clip=(v,n)=>{let s=String(v||'').replace(/\s+/g,' ').trim();if(!s)return'';return s.length<=n?s:s.slice(0,n).replace(/\s+\S*$/,'')+'…'};
 const neutral=(v,n=110)=>clip(String(v||'')
  .replace(/star\s*wars/ig,'space opera original')
  .replace(/edge\s*of\s*the\s*empire/ig,'fronteira espacial')
  .replace(/age\s*of\s*rebellion/ig,'resistência espacial')
  .replace(/galactic\s*empire|imp[eé]rio\s*gal[aá]ctico/ig,'regime interestelar')
  .replace(/rebel\s*alliance|alian[cç]a\s*rebelde/ig,'resistência clandestina')
  .replace(/stormtroopers?/ig,'soldados futuristas de armadura clara e desenho original')
  .replace(/jedi/ig,'místico guerreiro')
  .replace(/sith/ig,'adepto sombrio')
  .replace(/lightsabers?|sabres?\s*de\s*luz/ig,'arma luminosa de energia com desenho original')
  .replace(/tatooine/ig,'mundo desértico de dois sóis')
  .replace(/mos\s*eisley/ig,'porto espacial poeirento')
  .replace(/nar\s*shaddaa/ig,'metrópole vertical decadente')
  .replace(/bespin/ig,'cidade suspensa nas nuvens')
  .replace(/darth\s*vader/ig,'comandante mascarado de desenho original')
  .replace(/luke\s*skywalker/ig,'jovem aventureiro')
  .replace(/leia\s*organa/ig,'líder diplomática da resistência')
  .replace(/han\s*solo/ig,'piloto contrabandista')
  .replace(/chewbacca/ig,'companheiro alienígena peludo de grande porte')
  .replace(/millennium\s*falcon/ig,'cargueiro espacial civil modificado')
  .replace(/x[- ]?wing/ig,'caça estelar leve original')
  .replace(/tie\s*fighters?/ig,'caça militar compacto original')
  .replace(/star\s*destroyers?|destr[oó]ieres?\s*estelares?/ig,'cruzador militar colossal original')
  .replace(/death\s*star|estrela\s*da\s*morte/ig,'estação espacial militar colossal original')
  .replace(/at[- ]?ats?|at[- ]?sts?/ig,'andador militar articulado original')
  .replace(/blasters?/ig,'arma portátil de energia')
  .replace(/droids?|dr[oó]ides?/ig,'robô utilitário')
  .replace(/the\s*force|a\s*for[cç]a/ig,'energia mística'),n);
 const loc=neutral(view?.scene?.location?.description||view?.scene?.location?.name||scene?.locationName||scene?.title||'',115);
 const moment=neutral(document.querySelector('.scene-situation-paper p')?.textContent||view?.situation||narrationText(),150);
 const n=Array.isArray(view?.participants)?Math.min(view.participants.length,5):0;
 return [
  'Ilustração original 16:9, space opera retrofuturista cinematográfica.',
  loc&&('Local: '+loc+'.'),
  moment&&('Momento: '+moment+'.'),
  n&&('Pessoas/seres visíveis: '+n+'.'),
  'Tecnologia industrial usada, composição clara, luz natural da cena.',
  'Sem texto, HUD ou logos. Não reproduza personagens, naves, uniformes, armas ou designs de franquias existentes.'
 ].filter(Boolean).join(' ').slice(0,620);
}
function sceneKeyFor(value){const text=String(value??'Cena').trim()||'Cena';let h=2166136261;for(let i=0;i<text.length;i++){h^=text.charCodeAt(i);h=Math.imul(h,16777619)}return `scene_${(h>>>0).toString(16).padStart(8,'0')}`}
function currentSceneKey(){const scene=currentSceneRecord();return sceneKeyFor(scene?.id??scene?.sceneId??scene?.title??'Cena')}
function enhanceSceneGeminiButton(){
 const media=document.querySelector('.scene-media-slot');if(!media)return;
 let wrap=media.querySelector('.final-scene-gemini');
 if(!wrap){
  wrap=document.createElement('div');wrap.className='final-scene-gemini';
  wrap.innerHTML='<button type="button" class="final-gemini-info-icon" data-r54-scene-illustration="1" title="ILUSTRAÇÃO DA CENA COM GEMINI" aria-label="ILUSTRAÇÃO DA CENA COM GEMINI">ℹ</button><small aria-live="polite" class="final-gemini-msg"></small>';
  media.appendChild(wrap);
 }
 const btn=wrap.querySelector('[data-r54-scene-illustration]'),msg=wrap.querySelector('small');
 const apply=()=>{
  const st=geminiStatus(),key=currentSceneKey();
  if(!st?.imageUrl||st?.imageSceneKey!==key)return false;
  let img=media.querySelector('img.scene-media-image:not(.final-scene-library-image)');
  if(!img){img=document.createElement('img');img.className='scene-media-image';img.alt='Ilustração da cena atual';img.decoding='async';media.prepend(img)}
  img.src=String(st.imageUrl)+(String(st.imageUrl).includes('?')?'&':'?')+'v='+Date.now();
  media.querySelector('.final-scene-library-image')?.remove();
  media.classList.add('has-scene-image');media.classList.remove('has-recovered-scene','is-empty');
  const surface=media.querySelector('.scene-media-surface'),caption=media.querySelector('figcaption');if(surface)surface.hidden=true;if(caption)caption.hidden=true;
  if(btn)btn.disabled=false;if(msg)msg.textContent='Ilustração aplicada à cena.';
  return true;
 };
 apply();
 if(!window.__r54SceneImageNativeReturn){
  window.__r54SceneImageNativeReturn=true;
  addEventListener('braseiroGeminiNativeEvent',()=>{
   requestAnimationFrame(()=>{try{document.querySelectorAll('.scene-media-slot').forEach(()=>enhanceSceneGeminiButton())}catch(_){}});
  });
  document.addEventListener('click',ev=>{
   const target=ev.target?.closest?.('[data-r54-scene-illustration]');
   if(!target)return;
   ev.preventDefault();ev.stopPropagation();
   const holder=target.closest('.final-scene-gemini'),note=holder?.querySelector('small');
   const g=window.BraseiroGemini,prompt=sceneGeminiPrompt(),sceneKey=currentSceneKey();
   if(!g||typeof g.openGeminiScenePrompt!=='function'){if(note)note.textContent='Pipeline externo do Gemini indisponível.';return}
   if(!prompt){if(note)note.textContent='A cena ainda não tem contexto visual suficiente.';return}
   target.disabled=true;
   try{
    if(note)note.textContent='Abrindo o app do Gemini com o prompt compacto da cena…';
    const r=safeParse(g.openGeminiScenePrompt(prompt,sceneKey),{});
    target.disabled=false;
    if(!r?.ok){if(note)note.textContent=r?.message||'Não foi possível abrir o Gemini.';return}
    window.__r54SceneImagePendingKey=sceneKey;
    if(note)note.textContent='Gemini aberto. Gere a imagem e compartilhe com o app Star Wars para ela aparecer nesta cena.';
   }catch(err){target.disabled=false;if(note)note.textContent='Falha ao abrir o fluxo externo do Gemini.'}
  },true);
 }
}
function enhanceGeminiModels(){
 const card=cardByTitle('Chave da API Gemini');if(!card||card.querySelector('.final-gemini-models'))return;const g=window.BraseiroGemini;if(!g||typeof g.configureModels!=='function')return;const st=geminiStatus();const box=document.createElement('div');box.className='final-gemini-models';box.innerHTML=`<div class="final-model-title">Modelos Gemini</div><label>Texto / Mestre<input data-model="master" value="${String(st.masterModel||'gemini-3.8-flash')}"></label><label>Imagem de cena<input data-model="image" value="${String(st.model||'gemini-3.1-flash-image')}"></label><label>TTS<input data-model="tts" value="${String(st.ttsModel||'gemini-3.1-flash-tts-preview')}"></label><button type="button">SALVAR MODELOS</button><output></output>`;card.children[1]?.appendChild(box);box.querySelector('button').onclick=()=>{const vals=[...box.querySelectorAll('input')].map(x=>x.value.trim());try{const r=safeParse(g.configureModels(vals[0],vals[1],vals[2]),{});box.querySelector('output').textContent=r.ok?'Modelos salvos no aparelho.':(r.message||'Falha ao salvar modelos.')}catch{box.querySelector('output').textContent='Falha ao salvar modelos.'}};
}
function enhanceCharacterEmptyActions(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06')return;const headings=[...document.querySelectorAll('section')].find(s=>String(s.firstElementChild?.textContent||'').trim()==='Personagem');if(!headings||!String(headings.textContent||'').includes('Nenhum personagem carregado'))return;if(document.querySelector('.final-empty-character-actions'))return;const existing=[...document.querySelectorAll('button')].find(b=>String(b.textContent||'').includes('Abrir fluxo de criação'));if(!existing)return;const box=document.createElement('div');box.className='final-empty-character-actions';box.innerHTML='<button type="button" data-random>GERAR FICHA ALEATÓRIA</button><button type="button" data-import>IMPORTAR .JSON</button><output></output>';existing.insertAdjacentElement('afterend',box);const out=box.querySelector('output');box.querySelector('[data-random]').onclick=()=>quickRandomCharacter(out);box.querySelector('[data-import]').onclick=()=>quickImportCharacter(out);
}
function enhanceUtilityShortcuts(){document.querySelectorAll('.final-utility-shortcuts').forEach(el=>el.remove());const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='29'||document.querySelector('.final-library-shortcut'))return;const host=document.createElement('div');host.innerHTML='<button type="button" class="r53-fab final-library-shortcut">BIBLIOTECA</button>';const b=host.firstElementChild;b.setAttribute('aria-label','Abrir Biblioteca');b.onclick=()=>r53OpenLibrary();document.body.appendChild(b);}

function enhanceSceneGeminiMasterBar(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='00')return;
 const root=document.querySelector('.scene-concept-shell');if(!root)return;
 let bar=root.querySelector('.r54-scene-gemini-bar');
 if(!bar){
  bar=document.createElement('div');bar.className='r54-scene-gemini-bar';
  bar.innerHTML='<button type="button" class="r54-btn-gemini-trigger" id="r54-trigger-gemini-btn"><span>✦</span> NARRAR CENA COM MESTRE GEMINI</button><button type="button" class="r54-btn-holotable-trigger" onclick="window.BraseiroHolotable?.open()"><span>⬡</span> TERMINAL TÁTICO HOLONET</button>';
 }
 if(bar.parentNode!==root){
  const anchor=root.querySelector('.scene-situation-grid')||root.querySelector('.scene-narrative-paper');
  if(anchor&&anchor.parentNode===root&&anchor.nextSibling){root.insertBefore(bar,anchor.nextSibling)}else{root.appendChild(bar)}
 }
 const btn=bar.querySelector('#r54-trigger-gemini-btn');
 if(btn){
  btn.onclick=async()=>{
   btn.disabled=true;const oldHtml=btn.innerHTML;btn.innerHTML='<span>⏳</span> MESTRE GEMINI CONSULTANDO A GALÁXIA…';
   try{
    const pub=publishedAdventureView();
    if(pub?.active){
      await requestPublishedMasterNarration('pedido explícito do jogador para narrar a cena');
    }else if(!adventureStarted()){
      await startBarbaraOpening();
    }else{
      const game=globalThis.BraseiroGameBridge;
      if(game&&typeof game.submitPlayerIntent==='function'){
        await game.submitPlayerIntent('[NARRACAO_MESTRE] O jogador pediu que você assuma a narração desta cena atual com toda a imersão sensorial e regras dos 5 Pilares.');
      }
    }
   }catch(e){console.error(e)}
   finally{
    btn.disabled=false;btn.innerHTML=oldHtml;
    setTimeout(()=>{enhanceSceneFromEngine();enhanceNarration();badge()},100);
   }
  };
 }
}

function enhance(){
 const safe=(name,fn)=>{try{fn()}catch(e){console.warn('[final-enhance] error in '+name+':',e)}};
 safe('enhanceSceneGeminiMasterBar',enhanceSceneGeminiMasterBar);
 safe('enhanceNarration',enhanceNarration);
 safe('enhanceSceneFromEngine',enhanceSceneFromEngine);
 safe('maybePrewarmNativeTtsForScene',maybePrewarmNativeTtsForScene);
 safe('enhanceAccessibility',enhanceAccessibility);
 safe('enhanceTtsSettings',enhanceTtsSettings);
 safe('relabelCreationForCampaign',relabelCreationForCampaign);
 safe('setupOverlay',setupOverlay);
 safe('enhanceAdventureGate',enhanceAdventureGate);
 safe('enhancePublishedAdventurePanel',enhancePublishedAdventurePanel);
 safe('enhanceSceneGeminiButton',enhanceSceneGeminiButton);
 safe('enhanceGeminiModels',enhanceGeminiModels);
 safe('enhanceCharacterEmptyActions',enhanceCharacterEmptyActions);
 safe('enhanceUtilityShortcuts',enhanceUtilityShortcuts);
 safe('r53EnsureRestart',r53EnsureRestart);
}
if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('final-enhance',enhance);
else {window.addEventListener('popstate',()=>setTimeout(enhance,0));window.addEventListener('DOMContentLoaded',enhance);setTimeout(enhance,0);}
window.braseiroNpcTokenNativeEvent=function(raw){const evt=safeParse(raw,{});if(evt?.type==='token_imported'){const id=String(evt?.payload?.npcId||'');if(id&&pendingNpcTokenId()===id)setPendingNpcTokenId('');setTimeout(enhanceSceneFromEngine,0)}};
window.BraseiroFinalRuntime={version:'1.0.113',speakVisibleNarration:()=>speak(narrationText()),stopNarration:stop,refreshScene:enhanceSceneFromEngine,preferences:prefs};


/* R53 targeted runtime fixes: no R46 redesign. */
const R53_PATCH_ID='r53-real-device-2026-09-16';
function r53Norm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/\s+/g,' ').trim()}
function r53Bridge(){return (typeof window!=='undefined'&&window.BraseiroGameBridge&&typeof window.BraseiroGameBridge==='object')?window.BraseiroGameBridge:null}
function r53Unwrap(raw){if(raw&&raw.ok===false)throw new Error(raw.message||raw.error||'Operação recusada pelo motor.');let data=raw&&raw.ok===true?raw.data:raw;if(data&&data.check)data=data.check;return data}
function r53Close(selector){document.querySelector(selector)?.remove()}
function r53OpenDice(){
 if(document.querySelector('.r53-dice-overlay'))return;
 const ov=document.createElement('div');ov.className='r53-overlay r53-dice-overlay';
 ov.innerHTML='<div class="r53-modal"><div class="r53-modal-head"><div><small>DADOS NARRATIVOS</small><h2>Rolagem livre</h2></div><button class="r53-close" aria-label="Fechar">×</button></div><p style="margin:0;color:#aeb7bc;font-size:.78rem">Usa o mesmo motor de dados narrativos do jogo. Característica + perícia montam os dados positivos; dificuldade, desafio, bônus e contratempo completam a parada.</p><div class="r53-dice-grid"><label>Característica<input data-die="characteristic" type="number" min="0" max="5" value="2"></label><label>Perícia<input data-die="skill" type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-die="difficulty" type="number" min="0" max="5" value="2"></label><label>Desafio<input data-die="challenge" type="number" min="0" max="5" value="0"></label><label>Bônus<input data-die="boost" type="number" min="0" max="5" value="0"></label><label>Contratempo<input data-die="setback" type="number" min="0" max="5" value="0"></label></div><button class="r53-primary r53-roll">ROLAR DADOS</button><div class="r53-result" aria-live="polite">Aguardando rolagem.</div></div>';
 document.body.appendChild(ov);
 ov.querySelector('.r53-close').onclick=()=>ov.remove();
 ov.addEventListener('click',e=>{if(e.target===ov)ov.remove()});
 ov.querySelector('.r53-roll').onclick=()=>{
  const out=ov.querySelector('.r53-result');const b=r53Bridge();
  if(!b||typeof b.resolveCheck!=='function'){out.innerHTML='<span class="bad">Motor de dados indisponível.</span>';return}
  const n=k=>Math.max(0,Math.min(5,Number(ov.querySelector(`[data-die="${k}"]`).value)||0));
  try{
   const d=r53Unwrap(b.resolveCheck({characteristic:n('characteristic'),skill:n('skill'),difficulty:n('difficulty'),challenge:n('challenge'),boost:n('boost'),setback:n('setback')}))||{};
   const success=Number(d.netSuccess||0),adv=Number(d.netAdvantage||0),tri=Number(d.triumph||0),des=Number(d.despair||0);
   out.innerHTML=`<strong>Resultado</strong><br><span class="${success>0?'ok':'bad'}">${success>0?'SUCESSO':'FALHA'} · ${success} sucesso(s) líquido(s)</span><br>${adv} vantagem(ns) líquida(s) · ${tri} triunfo(s) · ${des} desespero(s)`;
  }catch(err){out.innerHTML='<span class="bad">'+String(err?.message||err)+'</span>'}
 };
}
function r53EnsureDiceFab(){
 // R54 P22: r54-device-p6 é a única autoridade do botão DADOS; evita dois observers disputando o mesmo controle.
 if(window.__R54_DEVICE_P6__){document.querySelectorAll('.r53-dice-fab').forEach(el=>el.remove());return}
 if(document.querySelector('.r53-dice-fab'))return;
 const b=document.createElement('button');b.type='button';b.className='r53-fab r53-dice-fab';b.innerHTML='<img src="assets/ui/icons-v2/dice.png" alt="">DADOS';b.onclick=r53OpenDice;document.body.appendChild(b);
}
function r54StartOrRestartPublishedAdventure(adventureId='under-black-sun'){
 if(!hasCharacter()){
  bridgeResult('loadStarterCharacter','cael');
 }
 r54ClearTransientUiState();
 navigateRoute('00');
 const origin=adventureId==='beyond-the-rim'?{startingPoint:'Ord Mantell — Praça Central',regionLabel:'Ord Mantell'}:{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'};
 if(!sandboxReady()){
  bridgeResult('initializeLivingSandbox',origin);
 }
 const r=bridgeResult('restartPublishedAdventure',adventureId,{});
 if(!r?.ok){
  bridgeResult('restartSandboxPreservingParty');
  bridgeResult('initializeLivingSandbox',origin);
  bridgeResult('startPublishedAdventureSession',adventureId,{});
 }
 window.dispatchEvent(new Event('braseiro:r21-state-changed'));
 document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());
 setTimeout(()=>{
  navigateRoute('00');
  enhance();
  if(masterNarrationAvailable())void requestPublishedMasterNarration(`abertura de ${adventureId==='under-black-sun'?'Sob um Sol Negro':'Além da Orla'}`);
 },50);
}
function r53RestartAdventure(){
 if(!confirm('Reiniciar a aventura mantendo personagem, grupo, nave e ajustes deste aparelho?'))return;
 try{
  r54ClearTransientUiState();
  navigateRoute('00');
  if(!hasCharacter()){
   bridgeResult('loadStarterCharacter','cael');
  }
  const publishedId=activePublishedAdventureId()||'under-black-sun';
  const r=bridgeResult('restartPublishedAdventure',publishedId,{});
  if(!r?.ok){
   bridgeResult('restartSandboxPreservingParty');
   const origin=publishedId==='beyond-the-rim'?{startingPoint:'Ord Mantell — Praça Central',regionLabel:'Ord Mantell'}:{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'};
   bridgeResult('initializeLivingSandbox',origin);
   bridgeResult('startPublishedAdventureSession',publishedId,{});
  }
  window.dispatchEvent(new Event('braseiro:r21-state-changed'));
  document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());
  setTimeout(()=>{
   navigateRoute('00');
   enhance();
   if(masterNarrationAvailable())void requestPublishedMasterNarration(`reabertura de ${publishedId==='under-black-sun'?'Sob um Sol Negro':'Além da Orla'}`);
  },50);
 }catch(err){alert('Falha ao reiniciar: '+String(err?.message||err))}
}
function r54ClearTransientUiState(){
 try{localStorage.removeItem('braseiro.starwars.edge.snapshots.v1')}catch(_){ }
 try{localStorage.removeItem('braseiro.starwars.published.pending.v1')}catch(_){ }
 try{sessionStorage.clear()}catch(_){ }
}
function r54ReturnToCampaignSelection(mode){
 if(!confirm('Criar um novo mundo e voltar à seleção de campanha? Personagem, grupo, nave e ajustes serão mantidos; o mundo/aventura atual será encerrado.'))return;
 const reset=bridgeResult('restartSandboxPreservingParty');
 if(!reset?.ok){alert(reset?.message||'Não foi possível preparar um novo mundo.');return}
 const selected=['EOTE','AOR','MIXED'].includes(String(mode||'').toUpperCase())?String(mode).toUpperCase():campaignMode();
 const changed=bridgeResult('setCampaignMode',selected);
 if(!changed?.ok){alert(changed?.message||'Não foi possível alterar o modo de campanha.');return}
 r54ClearTransientUiState();
 const target=new URL(location.href);target.search='';target.searchParams.set('screen','00');target.searchParams.set('campaignSetup','1');location.replace(target.toString());
}
function r54ZeroCampaign(){
 if(!confirm('ZERAR A CAMPANHA INTEIRA? Isso apaga personagem, grupo, nave, mundo, aventura e progresso deste aparelho.'))return;
 if(!confirm('Confirme novamente: iniciar uma campanha totalmente nova?'))return;
 const fresh=bridgeResult('newCampaign','Nova campanha');
 if(!fresh?.ok){alert(fresh?.message||'Não foi possível zerar a campanha.');return}
 r54ClearTransientUiState();
 navigateRoute('00');
 bridgeResult('loadStarterCharacter','cael');
 bridgeResult('initializeLivingSandbox',{startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'});
 bridgeResult('restartPublishedAdventure','under-black-sun',{});
 window.dispatchEvent(new Event('braseiro:r21-state-changed'));
 document.querySelectorAll('.final-adventure-gate').forEach(el=>el.remove());
 setTimeout(()=>{
  navigateRoute('00');
  enhance();
  if(masterNarrationAvailable())void requestPublishedMasterNarration('abertura de Sob um Sol Negro');
 },50);
}
function r54OpenCampaignHub(){
 if(document.querySelector('.r54-campaign-hub'))return;
 const current=campaignMode();
 const ov=document.createElement('div');ov.className='r53-overlay r54-campaign-hub';
 ov.innerHTML=`<div class="r53-modal r54-campaign-modal"><div class="r53-modal-head"><div><small>CAMPANHA E MUNDO</small><h2>Gerenciar jogo</h2></div><button class="r53-close" aria-label="Fechar">×</button></div>
 <p class="r54-campaign-copy">Escolha o que reiniciar. <b>Reiniciar aventura</b> mantém o mundo. <b>Novo mundo / seleção</b> mantém personagem, grupo, nave e ajustes. <b>Zerar campanha</b> apaga tudo.</p>
 <label class="r54-campaign-mode"><span>MODO PARA O PRÓXIMO MUNDO</span><select data-campaign-mode><option value="EOTE">Edge of the Empire · Obligation</option><option value="AOR">Age of Rebellion · Duty</option><option value="MIXED">Mista · EotE + AoR</option></select></label>
 <div class="r54-campaign-actions"><button type="button" class="r53-secondary" data-restart-adventure>REINICIAR AVENTURA</button><button type="button" class="r53-primary" data-new-world>NOVO MUNDO / VOLTAR À SELEÇÃO</button><button type="button" class="r54-danger" data-zero-campaign>ZERAR CAMPANHA INTEIRA</button></div>
 <small class="r54-campaign-note">Duty e Obligation continuam separados; trocar o modo não converte um no outro.</small></div>`;
 document.body.appendChild(ov);
 const select=ov.querySelector('[data-campaign-mode]');select.value=current;
 const close=()=>ov.remove();ov.querySelector('.r53-close').onclick=close;ov.addEventListener('click',e=>{if(e.target===ov)close()});
 ov.querySelector('[data-restart-adventure]').onclick=()=>{close();r53RestartAdventure()};
 ov.querySelector('[data-new-world]').onclick=()=>r54ReturnToCampaignSelection(select.value);
 ov.querySelector('[data-zero-campaign]').onclick=r54ZeroCampaign;
}
function enhanceBottomSheetMenu(){
 const sheet=document.querySelector('.bottom-sheet');
 if(!sheet)return;
 if(sheet.querySelector('.r54-sheet-campaign-zone'))return;
 const header=sheet.querySelector('header');
 if(!header)return;
 const zone=document.createElement('div');
 zone.className='r54-sheet-campaign-zone';
 zone.innerHTML=`
  <div class="r54-sheet-campaign-header">
   <span>CAMPANHA & AVENTURA</span>
   <small>Modo: <b>${campaignMode()}</b></small>
  </div>
  <div class="r54-sheet-campaign-buttons">
   <button type="button" class="r54-sheet-btn is-highlight" data-r54-sheet-restart>
    <span class="r54-sheet-btn-icon">🚀</span>
    <div class="r54-sheet-btn-text">
     <strong>REINICIAR AVENTURA</strong>
     <small>Reinicia do início (Sob um Sol Negro)</small>
    </div>
   </button>
   <button type="button" class="r54-sheet-btn" data-r54-sheet-beyond>
    <span class="r54-sheet-btn-icon">🌌</span>
    <div class="r54-sheet-btn-text">
     <strong>INICIAR: ALÉM DA ORLA</strong>
     <small>Expedição ao redor de Ord Mantell (Ep. 1)</small>
    </div>
   </button>
   <button type="button" class="r54-sheet-btn" data-r54-sheet-sandbox>
    <span class="r54-sheet-btn-icon">★</span>
    <div class="r54-sheet-btn-text">
     <strong>MODO LIVRE (SANDBOX)</strong>
     <small>Exploração galáctica sem roteiro</small>
    </div>
   </button>
   <button type="button" class="r54-sheet-btn" data-r54-sheet-hub>
    <span class="r54-sheet-btn-icon">🌐</span>
    <div class="r54-sheet-btn-text">
     <strong>NOVO MUNDO / VOLTAR À SELEÇÃO</strong>
     <small>Trocar modo (EotE / AoR / Mista) ou criar mundo</small>
    </div>
   </button>
   <button type="button" class="r54-sheet-btn is-danger" data-r54-sheet-zero>
    <span class="r54-sheet-btn-icon">⚠️</span>
    <div class="r54-sheet-btn-text">
     <strong>ZERAR CAMPANHA INTEIRA</strong>
     <small>Apagar tudo e reiniciar do zero</small>
    </div>
   </button>
  </div>
  <div class="r54-sheet-divider"><span>UTILIDADES DO SISTEMA</span></div>
 `;
 if(header.nextSibling)header.parentNode.insertBefore(zone,header.nextSibling);else header.parentNode.appendChild(zone);
 const closeSheet=()=>{
  try{
   const c=sheet.querySelector('button[aria-label="Fechar"]');
   if(c)c.click();
   else{
    sheet.closest('dialog')?.removeAttribute('open');
    document.querySelectorAll('dialog.overlay[open]').forEach(d=>d.removeAttribute('open'));
   }
  }catch(_){}
 };
 const handleSheetAction=(e,fn)=>{
  if(e){
   e.preventDefault();
   e.stopPropagation();
   e.stopImmediatePropagation();
  }
  closeSheet();
  navigateRoute('00');
  setTimeout(()=>{try{fn()}catch(err){console.error(err)}},20);
 };
 zone.querySelector('[data-r54-sheet-restart]')?.addEventListener('click',e=>handleSheetAction(e,r53RestartAdventure));
 zone.querySelector('[data-r54-sheet-beyond]')?.addEventListener('click',e=>handleSheetAction(e,()=>r54StartOrRestartPublishedAdventure('beyond-the-rim')));
 zone.querySelector('[data-r54-sheet-sandbox]')?.addEventListener('click',e=>handleSheetAction(e,async()=>await startBarbaraOpening()));
 zone.querySelector('[data-r54-sheet-hub]')?.addEventListener('click',e=>handleSheetAction(e,r54OpenCampaignHub));
 zone.querySelector('[data-r54-sheet-zero]')?.addEventListener('click',e=>handleSheetAction(e,r54ZeroCampaign));
}
function r53EnsureRestart(){
 document.querySelectorAll('.r53-restart-fab').forEach(el=>el.remove());
 enhanceBottomSheetMenu();
}
function r53OpenLibrary(){
 if(document.querySelector('.r53-library-overlay'))return;
 const ov=document.createElement('div');ov.className='r53-overlay r53-library-overlay';
 ov.innerHTML='<div class="r53-modal"><div class="r53-modal-head"><div><small>BIBLIOTECA LOCAL</small><h2>Fontes e PDFs</h2></div><button class="r53-close" aria-label="Fechar">×</button></div><div class="r53-library-tools"><input type="search" placeholder="Buscar nas fontes locais…" aria-label="Buscar na biblioteca"><button class="r53-secondary r53-search">BUSCAR</button></div><div style="display:grid;grid-template-columns:1fr 1fr;gap:7px"><button class="r53-primary r53-import">IMPORTAR PDF</button><button class="r53-secondary r53-refresh">ATUALIZAR</button></div><div class="r53-lib-status" style="font-size:.72rem;color:#9ccbd7"></div><div class="r53-docs"></div></div>';
 document.body.appendChild(ov);const docs=ov.querySelector('.r53-docs'),status=ov.querySelector('.r53-lib-status');
 const native=()=>window.BraseiroLibrary;
 const render=(rows)=>{docs.innerHTML='';if(!Array.isArray(rows)||!rows.length){docs.innerHTML='<div class="r53-empty">Nenhum PDF importado.</div>';return}for(const d of rows){const el=document.createElement('div');el.className='r53-doc';const title=String(d.title||d.fileName||'PDF');const st=String(d.status||'ANEXADO');const pages=Number(d.pageCount||0);const auth=String(d.authorityType||'SUPLEMENTO');el.innerHTML=`<strong>${title.replace(/[<&]/g,'')}</strong><small>${st} · ${pages?pages+' pág. · ':''}${auth}</small>`;docs.appendChild(el)}};
 const refresh=()=>{try{const api=native();if(!api||typeof api.listDocuments!=='function'){status.textContent='Biblioteca Android indisponível neste host.';render([]);return}const raw=api.listDocuments();render(typeof raw==='string'?JSON.parse(raw):raw);status.textContent='Biblioteca pronta.'}catch(err){status.textContent=String(err?.message||err);render([])}};
 ov.querySelector('.r53-close').onclick=()=>ov.remove();ov.addEventListener('click',e=>{if(e.target===ov)ov.remove()});ov.querySelector('.r53-refresh').onclick=refresh;
 ov.querySelector('.r53-import').onclick=()=>{try{const api=native();if(!api||typeof api.pickPdf!=='function')throw new Error('Importação de PDF indisponível.');api.pickPdf();status.textContent='Selecione o PDF…'}catch(err){status.textContent=String(err?.message||err)}};
 ov.querySelector('.r53-search').onclick=()=>{try{const q=ov.querySelector('input').value.trim();const api=native();if(!q){refresh();return}if(!api||typeof api.searchDocuments!=='function')throw new Error('Busca local indisponível.');const raw=api.searchDocuments(q,25),data=typeof raw==='string'?JSON.parse(raw):raw;render(data?.results||[]);status.textContent=data?.grounded?'Resultados encontrados nas fontes locais.':'Busca concluída.'}catch(err){status.textContent=String(err?.message||err)}};
 const previous=window.braseiroLibraryNativeEvent;window.braseiroLibraryNativeEvent=(raw)=>{try{if(typeof previous==='function')previous(raw)}finally{setTimeout(refresh,120)}};
 refresh();
}
function r53InterceptLibrary(){
 if(document.documentElement.dataset.r53LibraryCapture)return;document.documentElement.dataset.r53LibraryCapture='1';
 document.addEventListener('click',e=>{const hit=e.target?.closest?.('button,a,[role="button"],.list-item');if(!hit)return;const t=r53Norm(hit.textContent);if(!t.startsWith('biblioteca'))return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();r53OpenLibrary()},true);
}
function r53FindItemHost(button){let el=button.parentElement,best=null;for(let i=0;el&&i<7;i++,el=el.parentElement){const proposes=[...el.querySelectorAll('button')].filter(x=>r53Norm(x.textContent).includes('propor ao mestre'));const strong=el.querySelector('strong');if(proposes.length===1&&strong)best=el;if(proposes.length>1)break}return best||button.parentElement}
const r53ImageCache=new Map();
function r53PickAsset(name){
 const key=r53Norm(name);if(r53ImageCache.has(key))return r53ImageCache.get(key);const b=r53Bridge();let assets=[];
 try{if(b&&typeof b.searchImageLibrary==='function'){const raw=b.searchImageLibrary({query:name,category:'equipment',limit:30});const d=raw?.ok===true?raw.data:raw;if(Array.isArray(d))assets=d}}catch(_){ }
 const words=key.split(/[^a-z0-9]+/).filter(x=>x.length>2);let best=null,score=-1;
 for(const a of assets){if(a?.category!=='equipment')continue;const title=r53Norm(a.title),tags=r53Norm(Array.isArray(a.tags)?a.tags.join(' '):'');let s=0;if(title===key)s+=100;if(title.includes(key)||key.includes(title))s+=35;for(const w of words)if((title+' '+tags).includes(w))s+=5;if(a.sourceType!=='external_reference')s+=3;if(s>score){score=s;best=a}}
 const picked=score>=10?best:null;r53ImageCache.set(key,picked);return picked;
}
function r53AssetUrl(a){if(!a)return null;if(a.sourceType==='external_reference')return a.remoteReady&&a.remoteUrl?a.remoteUrl:null;return a.localPath||null}
function r53EnhanceItemImages(){
 const params=new URLSearchParams(location.search),route=params.get('screen')||'00';if(route!=='09'&&route!=='08')return;
 // R54_P22: o runtime exato (bindings/P6) é a única autoridade visual de itens.
 // Remova fallbacks antigos e nunca reinsira inventory.png/fuzzy em cards sem binding 1:1.
 document.querySelectorAll('.r53-item-visual.is-fallback').forEach(el=>el.remove());
}
function r53RemoveBadShortcuts(){document.querySelectorAll('.final-utility-shortcuts').forEach(el=>el.remove())}
function r53Tick(){r53RemoveBadShortcuts();r53EnsureDiceFab();r53EnsureRestart();r53EnhanceItemImages()}
r53InterceptLibrary();if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('r53-tick',r53Tick);else {r53Tick();addEventListener('popstate',()=>setTimeout(r53Tick,0));}



/* R54 — recover the user's visual library and real-device affordances. */
const R54_ASSET_ROOT='image-library/local/r54/';
let r54BindingsPromise=null;
function r54Bindings(){if(!r54BindingsPromise)r54BindingsPromise=fetch(R54_ASSET_ROOT+'bindings.json').then(r=>{if(!r.ok)throw new Error('bindings');return r.json()}).catch(()=>({weapons:{},equipment:{},portraits:{},maps:{}}));return r54BindingsPromise}
function r54Norm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/[^a-z0-9]+/g,' ').trim()}
function r54AssetIndex(bind){const map=new Map();for(const group of ['weapons','equipment'])for(const item of Object.values(bind?.[group]||{})){const src=item?.path||(item?.file?R54_ASSET_ROOT+item.file:'');if(!src)continue;for(const a of item.aliases||[])map.set(r54Norm(a),src)}return map}
async function r54EnhanceItemImages(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='09'&&route!=='08')return;
 const bind=await r54Bindings(),idx=r54AssetIndex(bind);
 for(const btn of [...document.querySelectorAll('button')].filter(x=>r54Norm(x.textContent).includes('propor ao mestre'))){
  const host=r53FindItemHost(btn);if(!host)continue;const strong=host.querySelector('strong');const name=strong?.textContent?.trim();if(!name)continue;
  const exact=idx.get(r54Norm(name));if(!exact)continue;
  const key=r54Norm(name),current=host.querySelector(':scope > .r54-item-visual');
  if(current){const img=current.querySelector('img');if(current.dataset.r54VisualBound===key&&img?.getAttribute('src')===exact)continue;current.remove()}
  host.querySelectorAll(':scope > .r53-item-visual').forEach(x=>x.remove());
  const fig=document.createElement('figure');fig.className='r54-item-visual';fig.dataset.r54VisualBound=key;fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${exact}">`;host.insertBefore(fig,host.firstChild);
 }
}
function r54CharacterPortrait(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06')return;
 if(document.querySelector('.r54-character-portrait'))return;const state=currentState(),c=state?.player?.character;if(!c)return;
 const species=r54Norm(c.species||c.speciesName||'');let src='';if(species.includes('twi lek')||species.includes('twilek'))src=R54_ASSET_ROOT+'portraits/twilek_female_blue.webp';
 if(!src)return;const heading=[...document.querySelectorAll('h2,h3,div')].find(el=>r54Norm(el.textContent)==='identidade');const card=heading?.closest('section,article,.card')||heading?.parentElement;if(!card)return;
 const fig=document.createElement('figure');fig.className='r54-character-portrait';fig.innerHTML=`<img src="${src}" alt="Retrato visual de ${String(c.name||'personagem').replace(/["<&]/g,'')}"><figcaption>${String(c.name||'Personagem')}</figcaption>`;heading.insertAdjacentElement('afterend',fig);
}
function r54FixSceneIcon(){
 const btn=document.querySelector('.bottom-navigation .bottom-nav-button:first-child');if(!btn)return;const icon=btn.querySelector('.reference-icon-mask');if(!icon||icon.dataset.r54Fixed)return;icon.dataset.r54Fixed='1';icon.classList.add('r54-scene-icon');icon.style.backgroundImage='none';icon.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8h16v11H4zM5 4l3 4M10 4l3 4M15 4l3 4M4 4h16v4H4z"/></svg>';
}
var r54VisualIndexPromise=null;
function r54VisualIndex(){
 if(!r54VisualIndexPromise)r54VisualIndexPromise=fetch('image-library/local/r54/p6-visual-index.json').then(r=>{if(!r.ok)throw new Error('visual index');return r.json()}).catch(()=>({locations:{}}));
 return r54VisualIndexPromise;
}
function r54ContextMapPreview(){
 const overlay=document.getElementById('braseiro-sandbox-setup');if(!overlay||overlay.querySelector('.r54-map-hint'))return;const selected=overlay.querySelector('#sandbox-selected-point');if(!selected)return;
 const box=document.createElement('div');box.className='r54-map-hint';box.innerHTML='<small>82 locais catalogados. O VTT mostra mapa local dedicado quando existe; nos demais, usa esquema local de jogo claramente identificado.</small><figure hidden><img alt="Mapa local"><figcaption></figcaption></figure>';selected.parentElement.insertAdjacentElement('afterend',box);
 let seq=0;const refresh=async()=>{const mine=++seq,name=String(selected.textContent||'').trim(),fig=box.querySelector('figure'),img=fig.querySelector('img'),cap=fig.querySelector('figcaption');const data=await r54VisualIndex();if(mine!==seq)return;const rec=data?.locations?.[r54Norm(name)]||null;if(rec?.path&&(rec?.strategy==='local_map'||rec?.strategy==='generated_play_map')){img.src=rec.path;const dedicated=rec.strategy==='local_map'&&rec.mapStatus==='map_local_integrado_dedicado';img.alt=(dedicated?'Mapa local de ':'Esquema local de jogo — ')+name;cap.textContent=(dedicated?'Mapa local dedicado — ':'Esquema local de jogo — ')+name;fig.hidden=false}else{img.removeAttribute('src');cap.textContent='Mapa local indisponível.';fig.hidden=false}};
 const mo=new MutationObserver(()=>{refresh()});mo.observe(selected,{childList:true,subtree:true,characterData:true});refresh();
}
function r54Tick(){r54FixSceneIcon();r54CharacterPortrait();r54ContextMapPreview();r54SupplementSpeciesPanel()}
if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('r54-tick',r54Tick);else {r54Tick();addEventListener('popstate',()=>setTimeout(r54Tick,0));}


/* R54_SUPPLEMENT_SPECIES_REFERENCE */
var r54SupplementSpeciesPromise=null;
function r54SupplementSpeciesCatalog(){
 if(!r54SupplementSpeciesPromise)r54SupplementSpeciesPromise=fetch('js/reference/speciesCreationReferenceCatalog.json').then(r=>{if(!r.ok)throw new Error('species supplement reference');return r.json()}).catch(()=>[]);
 return r54SupplementSpeciesPromise;
}
async function r54SupplementSpeciesForMode(mode='MIXED',query=''){
 if(!['EOTE','AOR','MIXED'].includes(mode))throw new Error('campaign mode');
 const q=r54Norm(query),rows=await r54SupplementSpeciesCatalog();
 return rows.filter(x=>(mode==='MIXED'||(x.activeLines||[]).includes(mode))&&(!q||r54Norm(`${x.name||''} ${(x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' ')}`).includes(q)));
}
async function r54SupplementSpeciesCoverage(){
 const rows=await r54SupplementSpeciesCatalog(),byLine={EOTE:0,AOR:0},books={};
 for(const x of rows){for(const l of x.activeLines||[])if(l in byLine)byLine[l]++;for(const r of (x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR')){const b=String(r.citation||'').replace(/ p\.\d+.*$/,'').replace(/,$/,'');books[b]=(books[b]||0)+1}}
 return {uniqueSpecies:rows.length,byLine,books};
}
window.BraseiroSupplementReference=Object.freeze({speciesForMode:r54SupplementSpeciesForMode,coverage:r54SupplementSpeciesCoverage,mechanicalPolicy:'REFERENCE_ONLY_UNTIL_SOURCE_BINDING'});
async function r54SupplementSpeciesPanel(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06'||document.querySelector('[data-r54-supplement-species]'))return;
 const rows=await r54SupplementSpeciesCatalog();if(!rows.length)return;
 const state=(typeof currentState==='function'?currentState():null)||{};const raw=String(state?.campaign?.mode||state?.mode||'MIXED').toUpperCase();const mode=['EOTE','AOR','MIXED'].includes(raw)?raw:'MIXED';
 const host=document.querySelector('main,.screen-content,.page-content,.r46-content')||document.body;const panel=document.createElement('section');panel.dataset.r54SupplementSpecies='1';panel.className='r54-supplement-species';
 panel.innerHTML='<h3>ESPÉCIES · SUPLEMENTOS</h3><p><small>Referências EotE/AoR recuperadas. Entradas sem bloco mecânico vinculado continuam apenas como referência.</small></p><input type="search" aria-label="Buscar espécie ou suplemento" placeholder="Buscar espécie ou suplemento…"><div class="r54-supplement-species-results"></div>';
 const input=panel.querySelector('input'),results=panel.querySelector('.r54-supplement-species-results');host.appendChild(panel);
 const render=async()=>{const found=await r54SupplementSpeciesForMode(mode,input.value);results.innerHTML=found.slice(0,12).map(x=>`<article><strong>${String(x.name||'').replace(/[<>&]/g,'')}</strong><small>${(x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' · ').replace(/[<>&]/g,'')}</small></article>`).join('')||'<small>Nenhuma referência nesse modo.</small>'};
 input.addEventListener('input',render);render();
}

})();

/* R54 HISTORICAL VISUAL RESTORE — approved R38 assets, visual-only. */
const FINAL_SCENE_VISUALS=Object.freeze({"customs_checkpoint":["image-library/local/final/scenes/customs_checkpoint_01.webp","image-library/local/final/scenes/customs_checkpoint_02.webp","image-library/local/final/scenes/customs_checkpoint_03.webp","image-library/local/final/scenes/customs_checkpoint_04.webp","image-library/local/final/scenes/customs_checkpoint_05.webp","image-library/local/final/scenes/customs_checkpoint_06.webp"],"medbay":["image-library/local/final/scenes/medbay_01.webp","image-library/local/final/scenes/medbay_02.webp","image-library/local/final/scenes/medbay_03.webp","image-library/local/final/scenes/medbay_04.webp"],"cargo_warehouse":["image-library/local/final/scenes/cargo_warehouse_01.webp","image-library/local/final/scenes/cargo_warehouse_02.webp","image-library/local/final/scenes/cargo_warehouse_03.webp","image-library/local/final/scenes/cargo_warehouse_04.webp"],"outer_rim_settlement":["image-library/local/final/scenes/outer_rim_settlement_01.webp","image-library/local/final/scenes/outer_rim_settlement_02.webp","image-library/local/final/scenes/outer_rim_settlement_03.webp","image-library/local/final/scenes/outer_rim_settlement_04.webp","image-library/local/final/scenes/outer_rim_settlement_05.webp"],"relay_station":["image-library/local/final/scenes/relay_station_01.webp","image-library/local/final/scenes/relay_station_02.webp"],"mining_complex":["image-library/local/final/scenes/mining_complex_01.webp"],"remote_hideout":["image-library/local/final/scenes/remote_hideout_01.webp"],"repair_dock":["image-library/local/final/scenes/repair_dock_01.webp"],"industrial_district":["image-library/local/final/scenes/industrial_district_01.webp"],"wreck_field":["image-library/local/final/scenes/wreck_field_01.webp"]});
const FINAL_SCENE_KEYWORDS=Object.freeze({"customs_checkpoint":["alfand","aduana","customs","inspeção","inspecao","checkpoint","contrabando","controle imperial"],"medbay":["medbay","centro médico","centro medico","médico","medico","hospital","bacta","enfermaria","ferido"],"cargo_warehouse":["armaz","entreposto","carga","warehouse","depósito","deposito","contêiner","conteiner","mercadoria"],"relay_station":["retransmiss","relay","antena","comunicaç","comunicac","transmissor","sinal"],"mining_complex":["minera","mining","mina","minério","minerio","escava","perfur"],"remote_hideout":["esconderijo","refúgio","refugio","hideout","fugitiv","clandestin","abrigo remoto"],"repair_dock":["doca de repar","hangar-oficina","hangar oficina","oficina","manutenção","manutencao","reparo"],"industrial_district":["distrito industrial","zona industrial","fábrica","fabrica","tubulaç","tubulac","fuligem"],"wreck_field":["destroço","destroco","wreck","sucata de nave","nave destruída","nave destruida"],"outer_rim_settlement":["assentamento","orla exterior","outer rim","colônia","colonia","povoado","fronteira","mercado","rua"]});
const FINAL_EXPLICIT_VISUALS=Object.freeze([{"id":"vehicle.digger_crawler","kind":"vehicle","title":"Digger Crawler","aliases":["Digger Crawler","digger crawler"],"src":"image-library/local/final/vehicles/digger_crawler.png"},{"id":"vehicle.at_est","kind":"vehicle","title":"AT-EST Exploration and Survey Walker","aliases":["AT-EST Exploration and Survey Walker","at est"],"src":"image-library/local/final/vehicles/at_est.png"},{"id":"vehicle.cloakshape","kind":"vehicle","title":"CloakShape","aliases":["CloakShape","cloakshape"],"src":"image-library/local/final/vehicles/cloakshape.png"},{"id":"vehicle.jumpmaster_jm5000","kind":"vehicle","title":"JM-5000 JumpMaster","aliases":["JM-5000 JumpMaster","jumpmaster jm5000","JM-5000 JumpMaster","JumpMaster"],"src":"image-library/local/final/vehicles/jumpmaster_jm5000.png"},{"id":"vehicle.firespray","kind":"vehicle","title":"Firespray","aliases":["Firespray","firespray"],"src":"image-library/local/final/vehicles/firespray.png"},{"id":"vehicle.skipray_gat12h","kind":"vehicle","title":"GAT-12h Skipray Blastboat","aliases":["GAT-12h Skipray Blastboat","skipray gat12h","GAT-12h Skipray","Skipray"],"src":"image-library/local/final/vehicles/skipray_gat12h.png"},{"id":"vehicle.alpha_xg1_star_wing","kind":"vehicle","title":"Alpha-class XG-1 Star Wing","aliases":["Alpha-class XG-1 Star Wing","alpha xg1 star wing","XG-1 Star Wing","Alpha-class XG-1"],"src":"image-library/local/final/vehicles/alpha_xg1_star_wing.png"},{"id":"vehicle.vt49_decimator","kind":"vehicle","title":"VT-49 Decimator","aliases":["VT-49 Decimator","vt49 decimator","VT-49 Decimator"],"src":"image-library/local/final/vehicles/vt49_decimator.png"},{"id":"vehicle.wud500","kind":"vehicle","title":"CEC WUD-500","aliases":["CEC WUD-500","wud500","WUD-500","CEC WUD-500"],"src":"image-library/local/final/vehicles/wud500.png"},{"id":"creature.kowakian_monkey_lizard","kind":"creature","title":"Kowakian Monkey-Lizard","aliases":["Kowakian Monkey-Lizard"],"src":"image-library/local/final/creatures/kowakian_monkey_lizard.png"},{"id":"creature.lurking_scrange","kind":"creature","title":"Lurking Scrange","aliases":["Lurking Scrange"],"src":"image-library/local/final/creatures/lurking_scrange.png"},{"id":"creature.gigantic_dragonsnake","kind":"creature","title":"Gigantic Dragonsnake","aliases":["Gigantic Dragonsnake"],"src":"image-library/local/final/creatures/gigantic_dragonsnake.png"},{"id":"npc.son_barab","kind":"npc","title":"Capitão Son Barab, ISB","aliases":["Son Barab","Capitão Son Barab","Captain Son Barab"],"src":"image-library/local/final/npcs/son_barab_portrait.webp"},{"id":"equipment.spacesuit","kind":"equipment","title":"Traje espacial","aliases":["Traje espacial","traje espacial","spacesuit","space suit"],"src":"image-library/local/final/equipment/spacesuit.webp"},{"id":"equipment.reload_power_pack","kind":"equipment","title":"Recarga adicional / Power Pack","aliases":["Recarga adicional / Power Pack","recarga adicional","power pack","ammo reload","recarga"],"src":"image-library/local/final/equipment/power_pack_reload.webp"}]);
function r54HistNorm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR')}
function r54HistHash(text){let h=2166136261>>>0;for(let i=0;i<text.length;i++){h^=text.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function r54HistoricalSceneVisual(){
 const media=document.querySelector('.scene-media-slot');if(!media)return;
 const live=media.querySelector('img.scene-media-image:not(.final-scene-library-image)');let fallback=media.querySelector('img.final-scene-library-image[data-r54-historical]');
 if(live){if(fallback)fallback.remove();return}
 const root=document.querySelector('main')||document.body,context=r54HistNorm(root?.innerText||'');let best='outer_rim_settlement',score=0;
 for(const [arch,terms] of Object.entries(FINAL_SCENE_KEYWORDS)){let s=0;for(const term of terms){const n=r54HistNorm(term);if(n&&context.includes(n))s+=n.length+(n.includes(' ')?8:3)}if(s>score){score=s;best=arch}}
 const variants=FINAL_SCENE_VISUALS[best]||FINAL_SCENE_VISUALS.outer_rim_settlement;if(!variants?.length)return;
 const seed=(document.querySelector('.scene-context-title')?.textContent||'')+'|'+(document.querySelector('.scene-situation-paper')?.textContent||'')+'|'+location.search;
 const src=variants[r54HistHash(seed||best)%variants.length];
 if(!fallback){fallback=document.createElement('img');fallback.className='scene-media-image final-scene-library-image';fallback.dataset.r54Historical='1';fallback.alt='Imagem de referência compatível com a cena atual';fallback.decoding='async';media.prepend(fallback)}
 if(fallback.getAttribute('src')!==src)fallback.setAttribute('src',src);media.classList.add('has-scene-image','has-recovered-scene');media.classList.remove('is-empty');
 const surface=media.querySelector('.scene-media-surface'),caption=media.querySelector('figcaption');if(surface)surface.hidden=true;if(caption)caption.hidden=true;
}
function r54HistSmallest(alias){const n=r54HistNorm(alias),nodes=[...document.querySelectorAll('main section,main article,main [class*=card],main [class*=panel],section,article')];const hits=nodes.filter(el=>{const t=r54HistNorm(el.textContent);return n&&t.includes(n)&&t.length<7000});hits.sort((a,b)=>(a.textContent||'').length-(b.textContent||'').length);return hits[0]||null}
function r54HistMount(v){const host=v.aliases.map(r54HistSmallest).find(Boolean);if(!host||host.querySelector(`[data-final-visual="${v.id}"]`))return;const wrap=document.createElement('figure');wrap.className='final-explicit-visual final-explicit-'+v.kind;wrap.dataset.finalVisual=v.id;const img=document.createElement('img');img.src=v.src;img.alt=v.title;img.decoding='async';wrap.append(img);const cap=document.createElement('figcaption');cap.textContent=v.title;wrap.append(cap);const target=host.querySelector(':scope > div:last-child')||host;target.prepend(wrap)}
function r54HistoricalVisualTick(){if((new URLSearchParams(location.search).get('screen')||'00')==='00')r54HistoricalSceneVisual();for(const v of FINAL_EXPLICIT_VISUALS)r54HistMount(v)}
(function(){if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('historical-visual',r54HistoricalVisualTick);else if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',r54HistoricalVisualTick,{once:true});else r54HistoricalVisualTick();window.BraseiroFinalVisualCompletion=Object.freeze({scenes:26,vehicles:9,creatures:3,npcs:1,equipment:2,policy:'explicit-validated-r54'});})();

/* R54_P26 — fonte real da narração + prompt visual seguro + enquadramento de token + estado vazio da nave */
(function(){
'use strict';
if(window.__R54_P26_RUNTIME__)return;window.__R54_P26_RUNTIME__=true;
const SOURCE_KEY='braseiro.starwars.lastMasterSource.v1';
const parse=(v,f=null)=>{try{return typeof v==='string'?JSON.parse(v):v??f}catch{return f}};
const clip=(v,n)=>String(v||'').replace(/\s+/g,' ').trim().slice(0,n);
function readScene(){try{const b=window.BraseiroGameBridge;if(!b||typeof b.scenePlayerView!=='function')return null;const r=parse(b.scenePlayerView());return r?.ok?r.data:(r?.data||r)}catch{return null}}
function saveSource(mode,notice=''){try{localStorage.setItem(SOURCE_KEY,JSON.stringify({mode:String(mode||'local'),notice:String(notice||''),at:new Date().toISOString()}))}catch{}}
function sourceInfo(){const m=String(parse(localStorage.getItem(SOURCE_KEY),{})?.mode||'local');try{if(geminiStatus()?.configured)return ['GEMINI','is-gemini']}catch{}if(m==='gemini-native'||m==='gemini')return ['GEMINI','is-gemini'];if(m==='http'||m==='remote')return ['MESTRE REMOTO','is-remote'];return ['MESTRE LOCAL','is-local']}
function style(){if(document.getElementById('r54-p26-runtime-style'))return;const s=document.createElement('style');s.id='r54-p26-runtime-style';s.textContent=`.scene-master-source-badge{display:inline-flex!important;align-items:center!important;justify-content:center!important;min-height:26px!important;padding:4px 8px!important;margin-left:8px!important;border:1px solid rgba(170,190,200,.35)!important;border-radius:999px!important;background:rgba(8,15,20,.78)!important;color:#dbe8ee!important;font:800 10px/1 system-ui,sans-serif!important;letter-spacing:.08em!important;white-space:nowrap!important}.scene-master-source-badge.is-gemini{color:#74d8ff!important;border-color:rgba(74,201,246,.55)!important}.scene-master-source-badge.is-remote{color:#f1d28b!important;border-color:rgba(218,179,87,.48)!important}.scene-master-source-badge.is-offline{color:#f0b29e!important;border-color:rgba(220,135,106,.48)!important}.scene-participant-frame{overflow:hidden!important}.r54-p23-npc-token-image,.r54-p6-participant-image{width:112%!important;height:112%!important;max-width:none!important;object-fit:cover!important;object-position:50% 18%!important;transform:translate(-5.5%,-2%)!important}.r54-p26-ship-empty-note{margin:10px 18px!important;padding:13px!important;border:1px solid rgba(74,198,239,.4)!important;border-radius:10px!important;background:#0a1217!important;color:#dbe7ec!important}.r54-p26-ship-empty-note strong,.r54-p26-ship-empty-note small{display:block!important}.r54-p26-ship-empty-note small{margin-top:5px!important;color:#aebbc1!important;line-height:1.45!important}`;document.head.appendChild(s)}
function trackMaster(){const b=window.BraseiroGameBridge;if(!b||b.__r54P26MasterTrack||typeof b.submitPlayerIntent!=='function')return;b.__r54P26MasterTrack=true;const original=b.submitPlayerIntent.bind(b);b.submitPlayerIntent=function(text){let out;try{out=original(text)}catch(err){saveSource('local',err?.message||'');throw err}return Promise.resolve(out).then(res=>{const d=res?.ok&&res?.data?res.data:res;saveSource(d?.mode||d?.provider||'local',d?.masterNotice||d?.notice||'');setTimeout(badge,0);return res},err=>{saveSource('local',err?.message||'');setTimeout(badge,0);throw err})}}
function badge(){style();const [label,cls]=sourceInfo();document.querySelectorAll('.scene-narrative-heading-row').forEach(row=>{if(row.closest('.scene-narrative-reader'))return;let b=row.querySelector('.scene-master-source-badge');if(!b){b=document.createElement('span');row.appendChild(b)}b.className='scene-master-source-badge '+cls;b.textContent=label})}
function safePrompt(){const v=readScene()||{},scene=v.scene||{},location=clip(scene.location?.name||scene.locationName||scene.title||'',120),title=clip(scene.title||location||'Cena',120),situation=clip(v.situation||'',360);const people=(Array.isArray(v.participants)?v.participants:[]).slice(0,3).map(p=>clip(p?.name,60)).filter(Boolean).join(', ');return [
'Crie UMA ÚNICA ilustração de cena para um VTT de space opera retro-industrial.',
'Use apenas informação visual pública deste instante. Não copie a narração nem reproduza parágrafos do jogo.',
'Priorize ambiente, arquitetura, clima, iluminação, escala, objetos, circulação e atmosfera.',
'Composição cinematográfica em plano aberto ou médio-aberto, adequada a fundo de RPG.',
'Não inclua violência gráfica, gore, tortura, nudez, ferimentos explícitos, texto, legendas, interface, logotipos, marca-dágua, molduras ou colagem.',
'Não represente personagens famosos; pessoas, se necessárias, devem ser figuras genéricas coerentes com os participantes públicos.',
`Cena: ${title}`,location?`Local: ${location}`:'',situation?`Situação pública: ${situation}`:'',people?`Pessoas visíveis: ${people}`:'',
'Resultado: imagem única, limpa, atmosférica e fiel ao clima da cena sem copiar o texto narrativo.'
].filter(Boolean).join('\n\n').slice(0,2200)}
function sanitizeGemini(){const b=window.BraseiroGemini;if(!b||b.__r54P26Prompt)return;b.__r54P26Prompt=true}
function tokenPrompt(){const b=window.BraseiroNpcTokens;if(!b||b.__r54P26Token||typeof b.openGeminiTokenPrompt!=='function')return;b.__r54P26Token=true;const original=b.openGeminiTokenPrompt.bind(b);b.openGeminiTokenPrompt=function(id,prompt){return original(id,String(prompt||'')+'\n\nENQUADRAMENTO OBRIGATÓRIO: retrato de UMA pessoa, cabeça e ombros / peito para cima. Rosto ocupando cerca de 65% da área útil. Cabeça inteira, queixo, ombros, cabelo, orelhas, chifres, lekku e acessórios essenciais sempre dentro do quadro, com pequena margem de segurança. Olhos próximos ao terço superior. Fundo simples. Nenhuma outra pessoa.')} }
function shipEmpty(){const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='10'){document.querySelector('.r54-p26-ship-empty-note')?.remove();return}const text=document.body.innerText||'';if(!/Nenhuma nave ativa registrada\./i.test(text)){document.querySelector('.r54-p26-ship-empty-note')?.remove();return}if(document.querySelector('.r54-p26-ship-empty-note'))return;const host=document.querySelector('.app-content,main')||document.body;const note=document.createElement('section');note.className='r54-p26-ship-empty-note';note.innerHTML='<strong>Você ainda não possui uma nave ativa.</strong><small>Quando uma nave for adquirida ou vinculada, esta tela passa a mostrar o resumo técnico e a imagem específica daquela nave quando houver asset correspondente.</small>';const target=[...host.querySelectorAll('section,article,div,p')].find(x=>/Nenhuma nave ativa registrada\./i.test(x.textContent||''));(target?.parentElement||host).insertBefore(note,target||null)}
function tick(){style();trackMaster();sanitizeGemini();tokenPrompt();badge();shipEmpty()}
if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p26-runtime',tick);else {if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',tick,{once:true});else tick();addEventListener('popstate',()=>setTimeout(tick,0));}
})();

/* R54_P27_COMPACT_GEMINI_PROMPT: <=900 chars, original space-opera wording, no full-scene dump. */

/* R54_P28_SCENE_IMAGE_NATIVE_RETURN: scene image uses generateSceneImage and is applied directly to scene media. */

/* R54_P29_SCENE_IMAGE_NATIVE_EVENT: delegated button + native generated event, no import button. */


/* R54 P30 — S23 Ultra containment, diagnostics and map hardening. */
(()=>{'use strict';
 if(window.__R54_P30_DEVICE_FIX__)return;window.__R54_P30_DEVICE_FIX__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const parse=(v,f={})=>{try{return typeof v==='string'?JSON.parse(v):(v??f)}catch{return f}};
 const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();

 function localMapUnavailable(img){
  if(!img||img.dataset.p30Broken==='1')return;
  img.dataset.p30Broken='1';
  img.hidden=true;img.style.display='none';
  const host=img.closest('figure,section,div')||img.parentElement;
  if(host&&!host.querySelector('.r54-p30-map-note')){
   const n=document.createElement('p');n.className='r54-p30-map-note';
   n.textContent='Mapa local indisponível para este local.';
   n.style.cssText='margin:8px 0;color:#9eabb2;font-size:.86rem;line-height:1.4';
   host.appendChild(n);
  }
 }
 function repairLocalMaps(){
  document.querySelectorAll('img').forEach(img=>{
   if(!/mapa local/i.test(img.alt||''))return;
   const src=String(img.getAttribute('src')||'').trim();
   const note=(img.closest('figure,section,div')||img.parentElement)?.querySelector?.('.r54-p30-map-note');
   const badSrc=!src||/^(undefined|null|about:blank)$/i.test(src);
   if(badSrc||(img.complete&&img.naturalWidth===0)){localMapUnavailable(img);return}
   img.addEventListener('error',()=>localMapUnavailable(img),{once:true});
   img.addEventListener('load',()=>{
    if(img.naturalWidth>0){img.hidden=false;img.style.display='';img.dataset.p30Broken='0';note?.remove()}
   },{once:true});
  });
 }

 function bridgeState(){
  const out={};
  out.motor=!!window.BraseiroGameBridge;
  out.gemini=!!window.BraseiroGemini;
  out.tts=!!window.BraseiroTts;
  out.library=!!window.BraseiroLibrary;
  out.images=!!window.BraseiroImages;
  try{out.geminiStatus=window.BraseiroGemini?.getStatus?parse(window.BraseiroGemini.getStatus(),{}):{}}catch(e){out.geminiError=String(e?.message||e)}
  try{const raw=window.BraseiroGameBridge?.readState?.();const state=parse(raw,{});out.campaign=state?.ok===false?'erro':(state?.data||state)?'pronta':'sem estado'}catch(e){out.campaign='erro'}
  try{const docs=window.BraseiroLibrary?.listDocuments?parse(window.BraseiroLibrary.listDocuments(),[]):[];out.documents=Array.isArray(docs)?docs.length:0}catch(e){out.documents=0}
  return out;
 }
 function statusLine(label,value,tone='ok'){
  return '<div class="r54-p30-row"><span>'+label+'</span><strong class="'+tone+'">'+value+'</strong></div>';
 }
 function mountDiagnostics(){
  if(route()!=='30')return;
  const host=document.querySelector('.app-content,main')||document.body;
  let panel=document.querySelector('.r54-p30-diagnostics');
  if(!panel){panel=document.createElement('section');panel.className='r54-p30-diagnostics';host.prepend(panel)}
  const st=bridgeState(),g=st.geminiStatus||{};
  const yes=v=>v?'DISPONÍVEL':'INDISPONÍVEL';
  panel.innerHTML='<div class="r54-p30-head"><div><small>DIAGNÓSTICO REAL DO APARELHO</small><h2>Estado do aplicativo</h2></div><button type="button" data-p30-refresh>ATUALIZAR</button></div>'+
   statusLine('Motor / dados',yes(st.motor),st.motor?'ok':'bad')+
   statusLine('Gemini bridge',yes(st.gemini),st.gemini?'ok':'bad')+
   statusLine('Gemini chave',g.configured?'CONFIGURADA':'NÃO CONFIGURADA',g.configured?'ok':'warn')+
   statusLine('Gemini geração',g.generating?'EM ANDAMENTO':'OCIOSO',g.generating?'warn':'ok')+
   statusLine('TTS Android',yes(st.tts),st.tts?'ok':'bad')+
   statusLine('Biblioteca PDF',yes(st.library),st.library?'ok':'bad')+
   statusLine('Documentos locais',String(st.documents||0),'ok')+
   statusLine('Bridge de imagens',yes(st.images),st.images?'ok':'bad')+
   statusLine('Campanha',String(st.campaign||'sem estado').toUpperCase(),st.campaign==='erro'?'bad':'ok')+
   '<p class="r54-p30-version">R54 P244 · Android 1.0.244 · diagnóstico calculado neste aparelho.</p>';
  panel.querySelector('[data-p30-refresh]')?.addEventListener('click',mountDiagnostics,{once:true});
 }
 function style(){
  if(document.getElementById('r54-p30-style'))return;
  const e=document.createElement('style');e.id='r54-p30-style';e.textContent=
  '.r54-p30-diagnostics{margin:12px 16px 110px;padding:14px;border:1px solid #4b6570;border-radius:12px;background:#0b1115;color:#e9eef0;box-shadow:0 10px 28px #0008}'+
  '.r54-p30-head{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:10px}.r54-p30-head small{color:#64cce6;font-weight:900;letter-spacing:.08em}.r54-p30-head h2{margin:3px 0 0;font-size:1.25rem}.r54-p30-head button{min-height:38px;padding:0 10px;border:1px solid #94713d;border-radius:8px;background:#25170f;color:#f1d29b;font-weight:900}'+
  '.r54-p30-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;padding:9px 0;border-top:1px solid #26343a;font-size:.86rem}.r54-p30-row strong{font-size:.77rem;letter-spacing:.05em}.r54-p30-row .ok{color:#79e6ad}.r54-p30-row .warn{color:#f0c567}.r54-p30-row .bad{color:#ff8e8e}.r54-p30-version{margin:12px 0 0;color:#84969e;font-size:.72rem}'+
  '.r54-p1-map-canvas>img{image-rendering:auto!important;object-fit:contain!important}.r54-p1-map-viewport{aspect-ratio:5042/3263!important;background:#030507!important}';
  document.head.appendChild(e);
 }
 function tick(){style();repairLocalMaps();mountDiagnostics()}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p30-runtime',tick);else {tick();addEventListener('popstate',()=>setTimeout(tick,0));}
})();


/* R54 P92 — P0 MECHANICAL REACHABILITY: combat, equipment, Force, vehicles, hyperspace and Contribution. */
(()=>{'use strict';
 if(window.__R54_P92_MECHANICS__)return;window.__R54_P92_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function resultText(r){return r?.ok?'Ação aplicada e estado persistido.':String(r?.message||r?.error||'Ação rejeitada pelo motor.');}
 function action(panel,fn){return ev=>{const btn=ev?.currentTarget;if(btn)btn.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}const out=panel.querySelector('[data-p92-output]');if(out){out.textContent=resultText(r);out.dataset.ok=r?.ok?'1':'0'}if(btn)btn.disabled=false;if(r?.ok)setTimeout(()=>{panel.remove();mount()},0)}}
 function style(){if(document.getElementById('r54-p92-style'))return;const e=document.createElement('style');e.id='r54-p92-style';e.textContent=
 '.r54-p92{margin:12px 14px 112px;padding:13px;border:1px solid #42545d;border-radius:12px;background:linear-gradient(180deg,#0e1519,#090d10);color:#e9eef0;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p92 *{box-sizing:border-box}.r54-p92 h2{margin:2px 0 4px;font-size:1.12rem;color:#f1d29b}.r54-p92 h3{margin:14px 0 7px;font-size:.88rem;color:#7bd5e8;letter-spacing:.06em}.r54-p92 p{margin:5px 0;color:#aebdc3;line-height:1.42}.r54-p92-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p92-card{padding:9px;border:1px solid #26363d;border-radius:9px;background:#111a1f}.r54-p92-row{display:flex;align-items:center;justify-content:space-between;gap:9px;padding:6px 0;border-top:1px solid #223038}.r54-p92-row:first-child{border-top:0}.r54-p92 small{display:block;color:#81949d}.r54-p92 button,.r54-p92 select,.r54-p92 input{min-height:39px;border:1px solid #51646d;border-radius:8px;background:#111a20;color:#eef5f7;padding:7px 9px}.r54-p92 button{font-weight:900;letter-spacing:.025em;border-color:#87683c;background:#24180f;color:#f1d29b}.r54-p92 button.secondary{border-color:#45626f;background:#0e2028;color:#bfeafa}.r54-p92 button:disabled{opacity:.45}.r54-p92 .danger{border-color:#7f4747;color:#ffb0a8}.r54-p92-controls{display:flex;flex-wrap:wrap;gap:7px;margin:8px 0}.r54-p92-controls>*{flex:1 1 135px}.r54-p92-output{margin-top:9px;padding:8px;border-radius:8px;background:#081014;color:#9ddfb8;min-height:34px}.r54-p92-output[data-ok="0"]{color:#ff9b9b}.r54-p92-badge{display:inline-block;padding:3px 7px;border-radius:999px;background:#1d2a30;color:#9cdcec;font-size:.72rem;font-weight:800}.r54-p92-warn{padding:9px;border-left:3px solid #b78b46;background:#20180d;color:#e9cb98}.r54-p92-wide{grid-column:1/-1}@media(max-width:430px){.r54-p92-grid{grid-template-columns:1fr}.r54-p92-controls>*{flex-basis:100%}}';document.head.appendChild(e)}
 function base(title,subtitle){const p=document.createElement('section');p.className='r54-p92';p.dataset.p92Mechanics=route();p.innerHTML=`<small>P92 · MECÂNICA EXECUTÁVEL · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(subtitle)}</p><div data-p92-body></div><output class="r54-p92-output" data-p92-output>Pronto.</output>`;return p}
 function mountCombat(){
  const r=call('combatRuntimeStatus'),p=base('COMBATE PESSOAL','Ações abaixo passam pelo Rules Engine e salvam o estado. Vantagens e Triunfos são gastos uma única vez após a rolagem.');
  const b=p.querySelector('[data-p92-body]');if(!r.ok){b.innerHTML=`<p class="r54-p92-warn">${esc(r.message)}</p>`;return p}
  const d=r.data||{},c=d.combat||{},eq=d.equipment||{},weapons=eq.weapons||[],adv=c.adversaries||[];
  if(!c.active){b.innerHTML='<p class="r54-p92-warn">Nenhum encontro ativo. Inicie o encontro pela cena, Mestre ou aventura publicada; esta tela não cria inimigos gratuitamente.</p>';return p}
  const spend=c.pendingPlayerSpend||null,targetPending=spend?adv.find(a=>String(a.id)===String(spend.adversaryId)):null,acts=spend?.activations||{};
  const blastQuality=spend?(spend.qualities||[]).find(q=>q.key==='blast'):null;
  const selectiveRank=(d.character?.talentPurchases||[]).filter(x=>x.originalName==='DETONACIÓN SELECTIVA').length;
  const blastCandidates=spend?adv.filter(a=>String(a.id)!==String(spend.adversaryId)&&!['derrotado','incapacitado'].includes(String(a.status||'').toLowerCase())):[];
  const blastHtml=blastQuality?`<div class="r54-p92-card"><strong>EXPLOSÃO · ALVOS ENGAJADOS</strong><small>${blastQuality.missBlast?'No ataque que errou, o alvo original recebe Explosão automaticamente se a qualidade for ativada. ':''}Marque SOMENTE personagens que estejam Engajados com o alvo original; o motor não presume proximidade entre NPCs.</small>${selectiveRank&&blastQuality.missBlast?`<label class="r54-p92-row">Excluir alvo original <input type="checkbox" data-p158-blast-exclude value="${esc(spend.adversaryId)}"></label>`:''}${blastCandidates.map(a=>`<label class="r54-p92-row"><span>${esc(a.name)}<small>${esc(a.range||'alcance?')}</small></span><input type="checkbox" data-p97-blast-target value="${esc(a.id)}">${selectiveRank?`<small>Excluir (${selectiveRank} máx.)</small><input type="checkbox" data-p158-blast-exclude value="${esc(a.id)}">`:''}</label>`).join('')||'<small>Nenhum outro adversário disponível para selecionar.</small>'}${selectiveRank?`<label class="r54-p92-row">Pagar exclusões com 1 Triunfo<input type="checkbox" data-p158-blast-triumph></label>`:''}</div>`:'';
  const qualityHtml=spend?`<h3>GASTAR RESULTADOS DO ÚLTIMO ATAQUE</h3><div class="r54-p92-card"><strong>Vantagens restantes: ${Number(spend.remainingAdvantage||0)} · Triunfos restantes: ${Number(spend.remainingTriumph||0)}</strong><small>Cada ativação consome estes mesmos resultados; não há gasto duplicado.</small></div><div class="r54-p92-controls">${(spend.qualities||[]).map(q=>{const used=Number(acts[q.key]||0),blocked=Number(spend.remainingAdvantage||0)<Number(q.cost||0)||(!q.repeatable&&used>0)||(['linked','aor_overwhelm'].includes(q.key)&&used>=Number(q.rank||0));return `<button class="secondary" data-p97-quality="${esc(q.key)}" ${blocked?'disabled':''}>${esc(q.label)} · ${Number(q.cost||0)} Vant.${used?` · x${used}`:''}</button>`}).join('')}${(d.character?.talentPurchases||[]).some(x=>x.originalName==='DESORIENTAR'||x.originalName==='DISOR')&&spend.attackSnapshot?.hit&&!acts.talentDisorient?`<button class="secondary" data-p149-disorient ${Number(spend.remainingAdvantage||0)<2?'disabled':''}>TALENTO DESORIENTAR · 2 VANT.</button>`:''}${(d.character?.talentPurchases||[]).some(x=>x.originalName==='DERRIBO')&&spend.attackSnapshot?.hit&&['pelea','armas_cuerpo_a_cuerpo'].includes(spend.attackSnapshot?.skillKey)&&!acts.talentKnockdown?`<button class="secondary" data-p154-knockdown ${Number(spend.remainingTriumph||0)<1?'disabled':''}>TALENTO DERRUBADA · 1 TRIUNFO</button>`:''}${c.pendingPlayerCritical?`<button data-p92-critical>CRÍTICO · ${Number(c.pendingPlayerCritical.cost||0)} VANT.${c.pendingPlayerCritical.byTriumph?' / 1 TRIUNFO':''}</button>`:''}</div>${(spend.qualities||[]).some(q=>q.key==='sunder')&&targetPending?.weapons?.length?`<label class="r54-p92-card">ALVO DA HENDEDURA<select data-p97-sunder-item>${targetPending.weapons.map((w,i)=>`<option value="${i}">${esc(w.name)} · ${esc(w.damageState||'íntegro')}</option>`).join('')}</select></label>`:''}${blastHtml}`:'';
  b.innerHTML=`<div class="r54-p92-grid"><div class="r54-p92-card"><strong>Rodada ${Number(c.round||1)}</strong><small>${c.encounter?.waitingForPlayer?'Turno do jogador aguardando ação':'Sequência de iniciativa em andamento'}</small></div><div class="r54-p92-card"><strong>${esc(d.health?.label||'Saúde')}</strong><small>Ferimentos ${Number(d.health?.wounds?.current??d.character?.derived?.wounds?.current??0)}/${Number(d.health?.wounds?.threshold??d.character?.derived?.wounds?.threshold??0)} · Tensão ${Number(d.health?.strain?.current??d.character?.derived?.strain?.current??0)}/${Number(d.health?.strain?.threshold??d.character?.derived?.strain?.threshold??0)}</small></div></div><h3>ATAQUE</h3><div class="r54-p92-controls"><select data-p92-weapon>${weapons.map((w,i)=>`<option value="${i}" ${w.active?'selected':''}>${esc(w.name)}${w.active?' · equipada':''}</option>`).join('')}</select><select data-p92-target>${adv.map(a=>`<option value="${esc(a.id)}">${esc(a.name)} · ${esc(a.range||'alcance?')} · ${Number(a.wounds?.current||0)}/${Number(a.wounds?.threshold||0)}</option>`).join('')}</select><label class="r54-p92-card"><input type="checkbox" data-p92-autofire> Automática</label><label class="r54-p92-card"><input type="checkbox" data-p216-insect> Kashyyyk: inseto (+2 Benefício, consome Triunfo da MESMA rolagem se obtido; Mestre confirma antes do ataque)</label></div><div class="r54-p92-controls"><select data-p104-aim-mode><option value="boost">Mirar · bônus no ataque</option><option value="targeted">Mirar · item/parte específica</option>${(d.talents?.trueAim?.available||d.character?.creation?.rulesProfile==='AOR_CORE'&&(d.character?.talentPurchases||[]).some(x=>x.originalName==='DISPARO CERTERO'))?'<option value="true_aim">Mira Verdadeira · aprimorar ataque à distância</option>':''}</select><input data-p104-aim-target placeholder="Item/parte visada (modo específico)"><button data-p92-aim>MIRAR</button><button data-p92-attack>ATACAR</button><button class="secondary" data-p92-next>ENCERRAR TURNO</button><button class="danger" data-p92-finish>ENCERRAR COMBATE</button></div>${qualityHtml}<h3>ADVERSÁRIOS</h3>${adv.map(a=>`<div class="r54-p92-row"><span>${esc(a.name)}<small>${esc(a.range||'')} · ${esc(a.status||'ativo')}${(a.statusEffects||[]).length?` · ${(a.statusEffects||[]).map(x=>esc(x.type)).join(', ')}`:''}</small></span><strong>${Number(a.wounds?.current||0)}/${Number(a.wounds?.threshold||0)}</strong></div>`).join('')||'<p>Nenhum adversário ativo.</p>'}`;
  p.querySelector('[data-p92-aim]')?.addEventListener('click',action(p,()=>call('aim',{maneuverMethod:document.querySelector('[data-p94-method]')?.value||'normal',mode:p.querySelector('[data-p104-aim-mode]')?.value||'boost',targetDetail:p.querySelector('[data-p104-aim-target]')?.value||''})));
  p.querySelector('[data-p92-attack]')?.addEventListener('click',action(p,()=>call('resolveWeaponAttack',{weaponIndex:Number(p.querySelector('[data-p92-weapon]')?.value||0),targetAdversaryId:p.querySelector('[data-p92-target]')?.value||null,useAutoFire:Boolean(p.querySelector('[data-p92-autofire]')?.checked),useKashyyykInsect:Boolean(p.querySelector('[data-p216-insect]')?.checked),gmInsectConfirmed:Boolean(p.querySelector('[data-p216-insect]')?.checked)})));
  p.querySelector('[data-p149-disorient]')?.addEventListener('click',action(p,()=>call('activatePendingDisorientTalent')));
  p.querySelector('[data-p154-knockdown]')?.addEventListener('click',action(p,()=>call('activatePendingKnockdownTalent')));
  p.querySelector('[data-p92-critical]')?.addEventListener('click',action(p,()=>call('activatePendingPlayerCritical',{})));
  // P158: excluded targets are explicit, counted and paid from the same receipt.
  p.querySelectorAll('[data-p97-quality]').forEach(x=>x.addEventListener('click',action(p,()=>call('activatePendingWeaponQuality',{qualityKey:x.dataset.p97Quality,targetWeaponIndex:Number(p.querySelector('[data-p97-sunder-item]')?.value||0),secondaryTargetIds:[...p.querySelectorAll('[data-p97-blast-target]:checked')].map(y=>y.value),excludedTargetIds:[...p.querySelectorAll('[data-p158-blast-exclude]:checked')].map(y=>y.value),useSelectiveTriumph:Boolean(p.querySelector('[data-p158-blast-triumph]')?.checked)}))));
  p.querySelector('[data-p92-next]')?.addEventListener('click',action(p,()=>call('advancePlayerTurn',{})));
  p.querySelector('[data-p92-finish]')?.addEventListener('click',action(p,()=>window.BraseiroPublishedActionClient.finishCombat({})));
  return p}
 function mountVehicle(){const r=call('vehicleRuntimeStatus');const p=base('COMBATE DE NAVE / VEÍCULO','Manobras, ação, tensão de sistemas, alcance e dano usam o estado persistente da nave ativa.');const b=p.querySelector('[data-p92-body]');if(!r.ok){b.innerHTML=`<p class="r54-p92-warn">${esc(r.message)}</p>`;return p}const d=r.data||{},v=d.activeVehicle,vs=d.vehicles||{},c=vs.combat||{};if(!v){b.innerHTML='<p class="r54-p92-warn">Nenhuma nave ativa. Adquira/vincule uma nave pelo fluxo da campanha; esta tela não concede veículos.</p>';return p}const target=c.targetVehicleId?vs.vehicles?.[c.targetVehicleId]:null;b.innerHTML=`<div class="r54-p92-grid"><div class="r54-p92-card"><strong>${esc(v.name)}</strong><small>Velocidade ${Number(v.speed||0)}/${Number(v.speedMax||0)} · Casco ${Number(v.hullDamage||0)}/${Number(v.hullThreshold||0)} · Sistema ${Number(v.systemStrain||0)}/${Number(v.systemStrainThreshold||0)}</small></div><div class="r54-p92-card"><strong>${c.active?`Rodada ${Number(c.round||1)} · ${esc(c.range||'')}`:'Fora de combate'}</strong><small>${target?`Alvo: ${esc(target.name)}`:'Nenhum alvo veicular'}</small></div></div>${c.pendingIncomingCritical?`<div class="r54-p92-card"><strong>CRÍTICO INIMIGO PENDENTE</strong><small>Gasto: ${Number(c.pendingIncomingCritical.cost||0)} Vantagens ou 1 Triunfo. O mesmo resultado não pode ser gasto duas vezes.</small><select data-p150-crit-component><option value="">Escolher componente se exigido</option>${['support_droid','ejection_system','landing_gear','weapon_system','sensors','comms','hyperdrive','shield_generator','engines','life_support','sublight_engines','navigation','bridge','hangar','power_core'].map(x=>`<option value="${x}">${esc(x)}</option>`).join('')}</select><button data-p150-crit>ATIVAR CRÍTICO INIMIGO</button><button class="secondary" data-p150-crit-decline>DISPENSAR CRÍTICO</button></div>`:''}${c.active?`<div class="r54-p92-controls"><button data-p92-v="accelerate">ACELERAR</button><button data-p92-v="brake">FREAR</button><button data-p92-v="advance">APROXIMAR</button><button data-p92-away>AFASTAR</button><button data-p92-v="evasive">EVASIVAS</button><button data-p92-v="stay_on_target">MANTER NA MIRA</button><button data-p92-full>ACELERAÇÃO MÁXIMA</button><button data-p92-vattack>DISPARAR</button><label>AGI inimiga (Mestre)<input data-p150-vnpc-agi type="number" min="0" max="6" value="3"></label><label>Artilharia inimiga (Mestre)<input data-p150-vnpc-gun type="number" min="0" max="5" value="2"></label>${d.aorBrilliantEvasionAvailable?`<label>Pilotagem inimiga (Mestre)<input data-p175-vnpc-piloting type="number" min="0" max="5" value="2"></label><button class="secondary" data-p175-evasion ${c.actionUsed||c.brilliantEvasionAttempted?'disabled':''}>EVASÃO IMPECÁVEL · TESTE OPOSTO</button>`:''}<button class="secondary" data-p150-vincoming>ATAQUE INIMIGO</button><button class="secondary" data-p92-vnext>PRÓXIMA RODADA</button><button class="danger" data-p92-vend>ENCERRAR COMBATE</button></div>`:'<div class="r54-p92-controls"><button data-p92-vstart>INICIAR CONTRA TIE/LN</button></div><p><small>O alvo TIE/ln é o encontro veicular de teste explícito do motor; aventuras podem iniciar outros alvos.</small></p>'}`;
 p.querySelectorAll('[data-p92-v]').forEach(x=>x.addEventListener('click',action(p,()=>call('useVehiclePilotManeuver',x.dataset.p92V,{}))));
 p.querySelector('[data-p92-away]')?.addEventListener('click',action(p,()=>call('useVehiclePilotManeuver','advance',{direction:'farther'})));
 p.querySelector('[data-p92-full]')?.addEventListener('click',action(p,()=>call('useFullThrottle',{})));
 p.querySelector('[data-p92-vattack]')?.addEventListener('click',action(p,()=>call('resolveVehicleAttack',{})));
 p.querySelector('[data-p175-evasion]')?.addEventListener('click',action(p,()=>call('useAoRBrilliantEvasion',{npcAgility:Number(p.querySelector('[data-p150-vnpc-agi]')?.value),npcPiloting:Number(p.querySelector('[data-p175-vnpc-piloting]')?.value)})));
 p.querySelector('[data-p150-vincoming]')?.addEventListener('click',action(p,()=>call('resolveIncomingVehicleAttack',{npcAgility:Number(p.querySelector('[data-p150-vnpc-agi]')?.value),npcGunnery:Number(p.querySelector('[data-p150-vnpc-gun]')?.value),playerPiloting:true})));
 p.querySelector('[data-p150-crit]')?.addEventListener('click',action(p,()=>call('activateIncomingVehicleCritical',{component:p.querySelector('[data-p150-crit-component]')?.value||null})));
 p.querySelector('[data-p150-crit-decline]')?.addEventListener('click',action(p,()=>call('declineIncomingVehicleCritical')));
 const swe02=c.publishedVehicleContext?.id==='swe02-thwheek-p239';
 const crash=d.publishedAdventure?.id==='long-arm-of-the-hutt'&&d.publishedAdventure.currentNodeId==='thwheek-crash';
 if(swe02||crash){
  const el=document.createElement('div');el.className='r54-p92-card';
  el.innerHTML='<strong>SWE02 · THWHEEK · FONTE OFICIAL P.12</strong><small>O desfecho só é liberado após dano veicular registrado pelo motor. A Krayt Fang abatida exige Pilotagem e Aguante de toda a tripulação.</small><div class="r54-p92-controls">'+
   (swe02&&c.active?'<button data-p239-thwheek-end>RESOLVER CONFRONTO</button><button data-p239-thwheek-pursue class="secondary">PERSEGUIR THWHEEK</button><button data-p239-thwheek-linked class="secondary">ATIVAR VINCULADA · 2 VANT.</button>':'')+
   (crash?'<button data-p239-crash-pilot>PILOTAGEM DIFÍCIL</button><button data-p239-crash-crew>AGUANTE DA TRIPULAÇÃO</button>':'')+'</div>';
  b.appendChild(el);
  el.querySelector('[data-p239-thwheek-end]')?.addEventListener('click',action(p,()=>call('resolveSWE02ThwheekCombat',{})));
  el.querySelector('[data-p239-thwheek-pursue]')?.addEventListener('click',action(p,()=>call('resolveSWE02ThwheekCombat',{pursue:true})));
  el.querySelector('[data-p239-thwheek-linked]')?.addEventListener('click',action(p,()=>call('activateSWE02ThwheekLinked')));
  el.querySelector('[data-p239-crash-pilot]')?.addEventListener('click',action(p,()=>call('resolveSWE02CrashPilot')));
  el.querySelector('[data-p239-crash-crew]')?.addEventListener('click',action(p,()=>call('resolveSWE02CrashResilience')));
 }
 p.querySelector('[data-p92-vnext]')?.addEventListener('click',action(p,()=>call('nextVehicleRound')));
 p.querySelector('[data-p92-vend]')?.addEventListener('click',action(p,()=>call('endVehicleCombat')));
 p.querySelector('[data-p92-vstart]')?.addEventListener('click',action(p,()=>call('beginVehicleCombat',{targetProfileKey:'tie_ln',range:'Curto'})));
 return p}
 function mountAstrogation(){const vr=call('vehicleRuntimeStatus');const p=base('ASTRONAVEGAÇÃO / HIPERESPAÇO','O salto usa Astronavegação da ficha, nave ativa, classe do hiperpropulsor e modificadores escolhidos; o resultado e danos persistem.');const b=p.querySelector('[data-p92-body]');if(!vr.ok||!vr.data?.activeVehicle){b.innerHTML='<p class="r54-p92-warn">Selecione uma nave com hiperpropulsor antes de calcular um salto.</p>';return p}const v=vr.data.activeVehicle;b.innerHTML=`<div class="r54-p92-card"><strong>${esc(v.name)}</strong><small>Hiperpropulsor classe ${esc(v.hyperdriveClass??'?')} · ${v.hyperdrive?'operacional/configurado':'ausente'}</small></div><div class="r54-p92-controls"><input data-p92-dest placeholder="Destino" value="Próximo sistema"><select data-p92-route><option value="MAJOR">Rota principal</option><option value="MINOR">Rota secundária</option><option value="SECONDARY">Pouco frequentada</option><option value="UNCHARTED">Inexplorada</option><option value="HAZARDOUS">Perigosa</option></select><label class="r54-p92-card"><input type="checkbox" data-p92-astro> Astromete</label><label class="r54-p92-card"><input type="checkbox" data-p92-rush> Salto apressado</label><label class="r54-p92-card"><input type="checkbox" data-p92-hazard> Perigo estelar</label></div><div class="r54-p92-controls"><button class="secondary" data-p92-plan>CALCULAR</button><button data-p92-jump>EXECUTAR SALTO</button></div><div class="r54-p92-card" data-p92-planout>Nenhum cálculo feito nesta tela.</div>`;
 const opts=()=>({destinationLabel:p.querySelector('[data-p92-dest]')?.value||'',routeType:p.querySelector('[data-p92-route]')?.value||'MAJOR',hasAstromech:Boolean(p.querySelector('[data-p92-astro]')?.checked),rushedJump:Boolean(p.querySelector('[data-p92-rush]')?.checked),stellarHazard:Boolean(p.querySelector('[data-p92-hazard]')?.checked)});
 p.querySelector('[data-p92-plan]')?.addEventListener('click',ev=>{const r=call('astrogationPlan',opts());const o=p.querySelector('[data-p92-planout]');if(r.ok){const d=r.data;o.innerHTML=`<strong>Dificuldade ${Number(d.difficulty?.difficulty??d.difficulty?.pool?.difficulty??0)}</strong><small>Bônus ${Number(d.difficulty?.pool?.boost||0)} · Revés ${Number(d.difficulty?.pool?.setback||0)} · ${esc((d.difficulty?.modifiers||[]).join(' · '))}</small>`}else o.textContent=r.message||'Falha no cálculo.'});
 p.querySelector('[data-p92-jump]')?.addEventListener('click',action(p,()=>call('resolveHyperspaceJump',opts())));
 return p}
 function mountEquipment(){const r=call('equipmentOwnedSummary');const p=base('EQUIPAMENTO ATIVO','Equipar muda o estado real da ficha; nenhuma arma ou armadura é criada por esta tela.');const b=p.querySelector('[data-p92-body]');if(!r.ok){b.innerHTML=`<p class="r54-p92-warn">${esc(r.message)}</p>`;return p}const d=r.data||{};b.innerHTML=`<h3>ARMAS</h3>${(d.weapons||[]).map(w=>`<div class="r54-p92-row"><span>${esc(w.name)}<small>${w.active?'ATIVA · ':''}${esc((w.qualities||[]).map(q=>typeof q==='string'?q:q?.name||q?.id||'').filter(Boolean).join(', '))}</small></span>${w.active?'<span class="r54-p92-badge">EQUIPADA</span>':`<button data-p92-equip-weapon="${esc(w.id||w.name)}">EQUIPAR</button>`}</div>`).join('')||'<p>Nenhuma arma no inventário.</p>'}<h3>ARMADURAS</h3>${(d.armor||[]).map(a=>`<div class="r54-p92-row"><span>${esc(a.name)}</span>${a.worn?`<button class="secondary" data-p92-unequip-armor="${esc(a.id||a.name)}">REMOVER</button>`:`<button data-p92-equip-armor="${esc(a.id||a.name)}">VESTIR</button>`}</div>`).join('')||'<p>Nenhuma armadura no inventário.</p>'}${(()=>{const r=call('utilityBeltStatus');const u=r.ok?r.data:null;if(!u?.talent?.available)return '';return `<div class="r54-p92-card"><strong>CINTO DE ACESSÓRIOS · TALENTO AoR</strong><small>Revela item pequeno não registrado; custa 1 Ponto de Destino luminoso. Não gasta créditos.</small><select data-p155-utility-item>${(u.items||[]).map(i=>`<option value="${esc(i.id)}">${esc(i.name)} · raridade ${Number(i.rarity)}</option>`).join('')}</select><button data-p155-utility-use ${Number(u.destiny?.light||0)<1?'disabled':''}>REVELAR ITEM · 1 DESTINO</button></div>`})()}${(()=>{const enc=d.encumbrance||{};return `<div class="r54-p92-card r54-enc-card"><strong>SOBRECARGA / CAPACIDADE DE CARGA</strong><div class="r54-enc-stats"><span class="r54-stat-pill"><b>Carga Atual:</b> ${Number(enc.total??enc.value??0)} / ${Number(enc.threshold??8)}</span><span class="r54-stat-pill ${Number(enc.over||0)>0?'is-danger':''}"><b>Excesso:</b> ${Number(enc.over||0)}</span><span class="r54-stat-pill ${Number(enc.setback||0)>0?'is-danger':''}"><b>Penalidade:</b> ${Number(enc.setback||0)} dado(s) de Revés</span></div></div>`})()}`;
 p.querySelector('[data-p155-utility-use]')?.addEventListener('click',action(p,()=>call('useUtilityBelt',{itemId:p.querySelector('[data-p155-utility-item]')?.value,actionId:'p155:'+Date.now()+':'+Math.floor(Math.random()*1000000000)})));
 p.querySelectorAll('[data-p92-equip-weapon]').forEach(x=>x.addEventListener('click',action(p,()=>call('equipWeapon',x.dataset.p92EquipWeapon))));
 p.querySelectorAll('[data-p92-equip-armor]').forEach(x=>x.addEventListener('click',action(p,()=>call('equipArmor',x.dataset.p92EquipArmor))));
 p.querySelectorAll('[data-p92-unequip-armor]').forEach(x=>x.addEventListener('click',action(p,()=>call('unequipArmor',x.dataset.p92UnequipArmor))));
 return p}
 function mountForce(){const r=call('forceStatus'),cat=call('forceCatalog');const p=base('FORÇA','Compra de poderes e aprimoramentos consome PE no Rules Engine. Resultados do dado da Força usam o estado de Destino e Tensão da ficha.');const b=p.querySelector('[data-p92-body]');if(!r.ok||!cat.ok){b.innerHTML='<p class="r54-p92-warn">Estado da Força indisponível para esta ficha.</p>';return p}const d=r.data||{},c=cat.data||{};if(!d.sensitive){b.innerHTML='<p class="r54-p92-warn">Esta personagem ainda não possui Force Rating. A interface não concede sensibilidade à Força fora das regras de progressão.</p>';return p}b.innerHTML=`<div class="r54-p92-grid"><div class="r54-p92-card"><strong>Force Rating ${Number(d.force?.rating||0)}</strong><small>Disponível ${Number(d.availableRating||0)} · comprometido ${Number(d.force?.committed||0)}</small></div><div class="r54-p92-card"><strong>PE ${Number((call('readState').data?.player?.character?.xp?.available)||0)}</strong><small>Compras abaixo são persistentes.</small></div></div><div class="r54-p92-controls"><button data-p92-force-roll>ROLAR DADOS DA FORÇA</button></div><h3>PODERES</h3>${Object.entries(c).map(([key,power])=>{const owned=Boolean(d.force?.powers?.[key]?.purchased);const upgrades=power.upgrades||[];const ownedSet=new Set(d.force?.powers?.[key]?.upgrades||[]);return `<div class="r54-p92-card"><div class="r54-p92-row"><span><strong>${esc(power.label||key)}</strong><small>Custo base ${Number(power.baseCost||0)} PE</small></span>${owned?'<span class="r54-p92-badge">ADQUIRIDO</span>':`<button data-p92-force-buy="${esc(key)}">COMPRAR</button>`}</div>${owned?upgrades.map(u=>`<div class="r54-p92-row"><span>${esc(u.label)}<small>${Number(u.cost||0)} PE · ${esc(u.type||'')}</small></span>${ownedSet.has(u.id)?'<span class="r54-p92-badge">OK</span>':`<button class="secondary" data-p92-force-upgrade="${esc(key)}|${esc(u.id)}">ADQUIRIR</button>`}</div>`).join(''):''}</div>`}).join('')}`;
 p.querySelector('[data-p92-force-roll]')?.addEventListener('click',ev=>{const rr=call('rollForcePowerCheck',{});const out=p.querySelector('[data-p92-output]');if(rr.ok){out.dataset.ok='1';out.textContent=`Força: ${Number(rr.data?.roll?.light||0)} luminoso · ${Number(rr.data?.roll?.dark||0)} sombrio · ${Number(rr.data?.points||0)} ponto(s) utilizável(is).`}else{out.dataset.ok='0';out.textContent=rr.message||'Falha na rolagem.'}});
 p.querySelectorAll('[data-p92-force-buy]').forEach(x=>x.addEventListener('click',action(p,()=>call('buyForcePower',x.dataset.p92ForceBuy))));
 p.querySelectorAll('[data-p92-force-upgrade]').forEach(x=>x.addEventListener('click',action(p,()=>{const [a,b]=x.dataset.p92ForceUpgrade.split('|');return call('buyForceUpgrade',a,b)})));
 return p}
 function mountProgression(){const g=call('groupSummary'),rw=call('allianceRewardOptions');const p=base('PROGRESSÃO / CONTRIBUIÇÃO','Duty e Contribution usam o motor AoR; cada marco de 100 gera uma recompensa real da Aliança.');const b=p.querySelector('[data-p92-body]');if(!g.ok){b.innerHTML=`<p class="r54-p92-warn">${esc(g.message)}</p>`;return p}const d=g.data||{},entries=d.group?.dutyEntries||d.group?.duty||d.group?.duties||[],pending=rw?.ok?rw.data?.pending:null;const opts=rw?.ok?rw.data:{};b.innerHTML=`<div class="r54-p92-grid"><div class="r54-p92-card"><strong>Duty total ${Number(d.dutyTotal||0)}</strong><small>Modo ${esc(d.campaignMode||'')}</small></div><div class="r54-p92-card"><strong>Contribution Rank ${Number(d.contributionRank||0)}</strong><small>${pending?'Recompensa da Aliança pendente':(d.contributionMilestoneReady?'Marco de 100 pronto para resolução':'Sem marco pendente')}</small></div></div>${Array.isArray(entries)&&entries.length?`<h3>DUTY</h3>${entries.map(x=>`<div class="r54-p92-row"><span>${esc(x.name||x.type||x.id||'Duty')}</span><strong>${Number(x.value??x.amount??0)}</strong></div>`).join('')}`:''}${d.contributionMilestoneReady&&!pending?'<div class="r54-p92-controls"><button data-p92-contribution>RESOLVER MARCO DE CONTRIBUIÇÃO</button></div>':''}${pending?`<h3>RECOMPENSA DA ALIANÇA · RARIDADE MÁX. ${Number(opts.maxRarity||pending.maxRarity||0)}</h3><div class="r54-p92-controls"><select data-p113-reward-kind><option value="equipment">Equipamento</option><option value="vehicle">Veículo/Nave</option><option value="strategic">Ativo estratégico</option></select><select data-p113-reward-item>${(opts.equipment||[]).map(x=>`<option value="${esc(x.id)}">${esc(x.name)} · R${Number(x.rarity)}</option>`).join('')}</select><input data-p113-strategic placeholder="Nome do ativo estratégico"><button data-p113-claim>RECEBER RECOMPENSA</button></div>`:''}`;
 p.querySelector('[data-p92-contribution]')?.addEventListener('click',action(p,()=>call('resolveCampaignMilestone')));
 const kind=p.querySelector('[data-p113-reward-kind]'),item=p.querySelector('[data-p113-reward-item]'),strategic=p.querySelector('[data-p113-strategic]');
 const refresh=()=>{if(!kind||!item||!strategic)return;const k=kind.value;strategic.style.display=k==='strategic'?'block':'none';item.style.display=k==='strategic'?'none':'block';const list=k==='vehicle'?(opts.vehicles||[]):k==='equipment'?(opts.equipment||[]):[];if(k!=='strategic')item.innerHTML=list.map(x=>`<option value="${esc(x.id)}">${esc(x.name)} · R${Number(x.rarity)}</option>`).join('')};kind?.addEventListener('change',refresh);refresh();
 p.querySelector('[data-p113-claim]')?.addEventListener('click',action(p,()=>call('claimAllianceReward',{kind:kind?.value||'equipment',itemId:item?.value||null,name:strategic?.value||''})));
 return p}
 function mount(){style();const r=route(),wanted=['02','03','04','06','07','08'];document.querySelectorAll('[data-p92-mechanics]').forEach(x=>{if(x.dataset.p92Mechanics!==r)x.remove()});if(!wanted.includes(r)||document.querySelector(`[data-p92-mechanics="${r}"]`))return;let p=null;if(r==='02')p=mountCombat();else if(r==='03')p=mountVehicle();else if(r==='04')p=mountAstrogation();else if(r==='06')p=mountForce();else if(r==='07')p=mountProgression();else if(r==='08')p=mountEquipment();if(p)host().appendChild(p)}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p92-mechanical-reachability',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();


/* R54 P93 — P0 WAVE 2: hyperspace arrival, talent progression, recovery and transactional cybernetics. */
(()=>{'use strict';
 if(window.__R54_P93_MECHANICS__)return;window.__R54_P93_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 const result=(panel,r)=>{const o=panel.querySelector('[data-p93-output]');if(o){o.textContent=r?.ok?'Ação aplicada e persistida.':String(r?.message||r?.error||'Ação rejeitada.');o.dataset.ok=r?.ok?'1':'0'}};
 const act=(panel,fn)=>ev=>{const btn=ev?.currentTarget;if(btn)btn.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}result(panel,r);if(btn)btn.disabled=false;if(r?.ok)setTimeout(()=>{panel.remove();mount()},0)};
 function style(){if(document.getElementById('r54-p94-style'))return;const e=document.createElement('style');e.id='r54-p94-style';e.textContent=
 '.r54-p94{margin:10px 14px 112px;padding:13px;border:1px solid #5b5a42;border-radius:12px;background:linear-gradient(180deg,#15150d,#0d0d09);color:#edf0e7;box-shadow:0 10px 28px #0008;font-size:.86rem}.r54-p94 h2{margin:2px 0 4px;color:#e7d59a;font-size:1.08rem}.r54-p94 h3{margin:13px 0 6px;color:#94d7e7;font-size:.86rem}.r54-p94 p{color:#aeb9b1;line-height:1.4}.r54-p94 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p94 .card{padding:9px;border:1px solid #30382d;border-radius:9px;background:#12160f}.r54-p94 .row{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:6px 0;border-top:1px solid #293128}.r54-p94 .row:first-child{border-top:0}.r54-p94 small{display:block;color:#84948a}.r54-p94 button,.r54-p94 select,.r54-p94 input{min-height:38px;border:1px solid #59685d;border-radius:8px;background:#111813;color:#eef4ee;padding:7px 9px}.r54-p94 button{font-weight:900;border-color:#826f41;background:#251d0f;color:#f0d99c}.r54-p94 button.secondary{border-color:#426275;background:#0f2028;color:#c5eaf4}.r54-p94 .controls{display:flex;flex-wrap:wrap;gap:7px;margin:8px 0}.r54-p94 .controls>*{flex:1 1 135px}.r54-p94 output{display:block;margin-top:8px;padding:8px;border-radius:8px;background:#09110b;color:#9edab1;min-height:34px}.r54-p94 output[data-ok="0"]{color:#ff9d9d}.r54-p94 .badge{padding:3px 7px;border-radius:999px;background:#1e2a20;color:#a8e2b2;font-size:.7rem;font-weight:900}.r54-p94 .warn{padding:8px;border-left:3px solid #a67b44;background:#21180e;color:#ebcc9c}@media(max-width:430px){.r54-p94 .grid{grid-template-columns:1fr}.r54-p94 .controls>*{flex-basis:100%}}';document.head.appendChild(e)}
 function base(title,sub){const p=document.createElement('section');p.className='r54-p94';p.dataset.p93Mechanics=route();p.innerHTML=`<small>P93 · MECÂNICA EXECUTÁVEL · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(sub)}</p><div data-p93-body></div><output data-p93-output>Pronto.</output>`;return p}
 function mountHyperspace(){
   const state=call('readState'),veh=call('vehicleRuntimeStatus');const p=base('HIPERESPAÇO → TEMPO → LOCAL','Salto bem-sucedido avança o Mundo Vivo e só muda a cena se o destino for alcançado sem desvio.');const b=p.querySelector('[data-p93-body]');
   if(!state.ok||!veh.ok||!veh.data?.activeVehicle){b.innerHTML='<p class="warn">Nave ou estado indisponível.</p>';return p}
   const locs=Object.values(state.data?.world?.locations||{}).filter(x=>x&&x.id&&x.knownToPlayer!==false);const current=state.data?.scene?.locationId||'';
   b.innerHTML=`<div class="card"><strong>${esc(veh.data.activeVehicle.name||'Nave')}</strong><small>Local atual: ${esc(state.data?.world?.locations?.[current]?.name||current||'desconhecido')}</small></div><div class="controls"><select data-p93-dest>${locs.filter(x=>x.id!==current).map(x=>`<option value="${esc(x.id)}">${esc(x.name||x.id)}</option>`).join('')}</select><select data-p93-route><option value="MAJOR">Rota principal</option><option value="MINOR">Rota secundária</option><option value="SECONDARY">Pouco frequentada</option><option value="UNCHARTED">Inexplorada</option><option value="HAZARDOUS">Perigosa</option></select><label class="card"><input type="checkbox" data-p93-astro> Astromete</label><label class="card"><input type="checkbox" data-p93-rush> Apressado</label><label class="card"><input type="checkbox" data-p93-hazard> Perigo estelar</label></div><div class="controls"><button data-p93-plan>CALCULAR</button><button data-p93-jump>EXECUTAR SALTO REAL</button></div><div class="card" data-p93-hyper-out>O resultado mostrará tempo avançado e local alcançado.</div>`;
   const opts=()=>{const id=p.querySelector('[data-p93-dest]')?.value||null;return {destinationLocationId:id,destinationLabel:state.data?.world?.locations?.[id]?.name||id,routeType:p.querySelector('[data-p93-route]')?.value||'MAJOR',hasAstromech:Boolean(p.querySelector('[data-p93-astro]')?.checked),rushedJump:Boolean(p.querySelector('[data-p93-rush]')?.checked),stellarHazard:Boolean(p.querySelector('[data-p93-hazard]')?.checked)}};
   p.querySelector('[data-p93-plan]')?.addEventListener('click',ev=>{const r=call('astrogationPlan',opts());result(p,r);if(r.ok)p.querySelector('[data-p93-hyper-out]').textContent=`Dificuldade efetiva ${Number(r.data?.difficulty?.effectiveDifficultyRating||0)} · Boost ${Number(r.data?.difficulty?.pool?.boost||0)} · Setback ${Number(r.data?.difficulty?.pool?.setback||0)}.`});
   p.querySelector('[data-p93-jump]')?.addEventListener('click',ev=>{const r=call('resolveHyperspaceJump',opts());result(p,r);if(r.ok){const d=r.data||{};p.querySelector('[data-p93-hyper-out]').textContent=`${d.outcome?.narrativeSummary||'Salto resolvido.'} Mundo Vivo +${Number(d.timeAdvancedTicks||0)} turno(s) de 6h. ${d.arrivedLocationId?'Chegada confirmada.':d.outcome?.offCourse?'DESVIO: destino não aplicado.':'Sem mudança de local.'}`;setTimeout(()=>{p.remove();mount()},80)}});
   return p
 }
 function mountTalents(){
   const r=call('talentProgressionStatus');const p=base('TALENTOS / ESPECIALIZAÇÕES POSSUÍDAS','Compra somente nós desbloqueados de árvores que a ficha realmente possui; PE e efeitos são persistidos pelo engine.');const b=p.querySelector('[data-p93-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const d=r.data||{};
   const p163Skills=["actividad_criminal", "aguante", "alerta", "astronavegacion", "atletismo", "callejeo", "carisma", "coaccion", "computadoras", "coordinacion", "disciplina", "engano", "frialdad", "liderazgo", "mecanica", "medicina", "negociacion", "percepcion", "pilotar", "pilotar_vehiculo_espacial", "pilotar_vehiculo_planetario", "sigilo", "supervivencia", "armas_distancia_ligeras", "armas_distancia_pesadas", "armas_cuerpo_a_cuerpo", "artilleria", "pelea", "bajos_fondos", "borde_exterior", "educacion", "estrategia_militar", "mundos_del_nucleo", "saber_arcaico", "xenologia"];
   b.innerHTML=`<div class="card"><strong>PE disponíveis ${Number(d.xp?.available||0)}</strong><small>Árvores possuídas: ${(d.ownedSpecializations||[]).length}</small></div>${(d.trees||[]).map((t,ti)=>`<h3>${esc(t.specializationLabel||t.specializationKey)}</h3><div class="card">${(t.eligible||[]).map(n=>`<div class="row"><span>${esc(n.name)}<small>${Number(n.cost||0)} PE · linha ${Number(n.row||0)}${n.unlocked?' · desbloqueado':' · bloqueado'}</small></span>${n.purchased?'<span class="badge">ADQUIRIDO</span>':n.available?`<span>${n.choiceType==='twoSkills'?`<select data-p163-skill-a="${esc(n.id)}">${p163Skills.map(x=>`<option value="${esc(x)}">${esc(x.replaceAll('_',' '))}</option>`).join('')}</select><select data-p163-skill-b="${esc(n.id)}">${p163Skills.map((x,i)=>`<option value="${esc(x)}" ${i===1?'selected':''}>${esc(x.replaceAll('_',' '))}</option>`).join('')}</select>`:n.choiceType==='combatSkill'?`<select data-p93-skill="${esc(n.id)}"><option value="pelea">Briga</option><option value="armas_cuerpo_a_cuerpo">Corpo a Corpo</option><option value="armas_distancia_ligeras">Armas à Distância (Leves)</option><option value="armas_distancia_pesadas">Armas à Distância (Pesadas)</option><option value="artilleria">Artilharia</option></select>`:n.choiceType==='socialSkill'?`<select data-p93-skill="${esc(n.id)}"><option value="carisma">Carisma</option><option value="coaccion">Coação</option><option value="engano">Enganação</option><option value="negociacion">Negociação</option></select>`:n.choiceType==='knowledgeSkill'?`<select data-p93-skill="${esc(n.id)}"><option value="bajos_fondos">Submundo</option><option value="borde_exterior">Orla Exterior</option><option value="educacion">Educação</option><option value="estrategia_militar">Estratégia militar</option><option value="mundos_del_nucleo">Mundos do Núcleo</option><option value="saber_arcaico">Saber arcaico</option><option value="xenologia">Xenologia</option></select>`:n.choiceType==='characteristic'?`<select data-p93-ded="${esc(n.id)}"><option value="fortaleza">Fortaleza</option><option value="agilidad">Agilidade</option><option value="intelecto">Intelecto</option><option value="astucia">Astúcia</option><option value="voluntad">Vontade</option><option value="presencia">Presença</option></select>`:''}<button data-p93-talent="${esc(n.id)}" data-career="${esc(t.careerKey)}" data-spec="${esc(t.specializationKey)}">COMPRAR</button></span>`:'<span class="badge">INDISPONÍVEL</span>'}</div>`).join('')}</div>`).join('')||'<p class="warn">Nenhuma árvore de talentos possuída foi encontrada.</p>'}`;
   p.querySelectorAll('[data-p93-talent]').forEach(btn=>btn.addEventListener('click',act(p,()=>{const id=btn.dataset.p93Talent;const ch=p.querySelector(`[data-p93-ded="${CSS.escape(id)}"]`)?.value||null;const sk=p.querySelector(`[data-p93-skill="${CSS.escape(id)}"]`)?.value||null;const sa=p.querySelector(`[data-p163-skill-a="${CSS.escape(id)}"]`)?.value;const sb=p.querySelector(`[data-p163-skill-b="${CSS.escape(id)}"]`)?.value;return call('buyTalent',id,{careerKey:btn.dataset.career,specializationKey:btn.dataset.spec,characteristicKey:ch,skillKey:sk,skillKeys:sa&&sb?[sa,sb]:null})})));
   return p
 }
 function mountRecovery(){
   const r=call('recoveryRuntimeStatus');const p=base('RECUPERAÇÃO / CONDIÇÕES','Medicina, estimulante e descanso usam o Rules Engine; a tela não limpa Ferimentos/Tensão diretamente.');const b=p.querySelector('[data-p93-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const d=r.data||{},h=d.health||{},effects=d.character?.statusEffects||[],companions=d.companions||[];
   b.innerHTML=`<div class="grid"><div class="card"><strong>Ferimentos ${Number(h.wounds?.current??d.character?.derived?.wounds?.current??0)}/${Number(h.wounds?.threshold??d.character?.derived?.wounds?.threshold??0)}</strong><small>Tensão ${Number(h.strain?.current??d.character?.derived?.strain?.current??0)}/${Number(h.strain?.threshold??d.character?.derived?.strain?.threshold??0)}</small></div><div class="card"><strong>${d.combat?.active?'EM COMBATE':'FORA DE COMBATE'}</strong><small>${effects.length} condição(ões) ativa(s)</small></div></div><h3>CONDIÇÕES</h3>${effects.map(x=>`<div class="row"><span>${esc(x.type||x.name||'Condição')}</span><small>${esc(x.duration||'')} ${x.rounds!=null?'· '+Number(x.rounds)+' rodada(s)':''}</small></div>`).join('')||'<p>Nenhuma condição ativa.</p>'}<div class="controls"><button data-p93-stim>USAR ESTIMULANTE</button><button data-p93-med>MEDICINA</button>${d.combat?.active?'':'<button class="secondary" data-p93-rest>DESCANSO COMPLETO</button>'}</div>`;
   const party=document.createElement('div');party.className='card';party.dataset.p164Companions='1';
   party.innerHTML=`<strong>COMPANHEIROS · FICHAS PERSONALIZADAS</strong><p>Fichas simplificadas criadas pelo jogador, não NPCs oficiais. O tratamento usa Medicina do personagem e salva os ferimentos do companheiro.</p><label>Paciente</label><select data-p164-target><option value="">Meu personagem</option>${companions.map(m=>`<option value="${esc(m.id)}">${esc(m.character.name)} · ${Number(m.character.derived?.wounds?.current||0)}/${Number(m.character.derived?.wounds?.threshold||0)} ferimentos</option>`).join('')}</select><label>Nome do novo companheiro</label><input data-p164-name maxlength="120" placeholder="Nome"><label>Limite de Ferimentos</label><input type="number" data-p164-wounds min="1" max="100" value="12"><label>Limite de Tensão</label><input type="number" data-p164-strain min="1" max="100" value="12"><div class="controls"><button data-p164-add>ADICIONAR COMPANHEIRO</button><button class="secondary" data-p164-remove>REMOVER SELECIONADO</button></div><label>Dano bruto recebido pelo companheiro</label><input type="number" data-p164-damage min="1" max="100" value="3"><button data-p164-injure>APLICAR DANO</button>`;
   b.append(party);
   const targetCompanionId=()=>party.querySelector('[data-p164-target]')?.value||null;
   party.querySelector('[data-p164-add]')?.addEventListener('click',act(p,()=>call('addCompanion',{name:party.querySelector('[data-p164-name]').value,woundsThreshold:Number(party.querySelector('[data-p164-wounds]').value),strainThreshold:Number(party.querySelector('[data-p164-strain]').value)})));
   party.querySelector('[data-p164-remove]')?.addEventListener('click',act(p,()=>call('removeCompanion',targetCompanionId())));
   party.querySelector('[data-p164-injure]')?.addEventListener('click',act(p,()=>call('applyCompanionDamage',{companionId:targetCompanionId(),damage:Number(party.querySelector('[data-p164-damage]').value)})));
   const p163=document.createElement('div');p163.className='card';p163.dataset.p163Medical='1';
   p163.innerHTML=`<strong>P164 · MEDICINA AoR / BACTA + COMPANHEIROS</strong><p>Aplicação exige encontro ativo, talento e injetor/medpac no inventário. Bacta exige tanque confirmado.</p><label>Característica</label><select data-p163-characteristic>${['fortaleza','agilidad','intelecto','astucia','voluntad','presencia'].map(x=>`<option value="${x}">${esc(trChar(x))}</option>`).join('')}</select><label>Extra (Aplicação suprema: exige Triunfo)</label><select data-p163-extra><option value="">Nenhuma</option>${['fortaleza','agilidad','intelecto','astucia','voluntad','presencia'].map(x=>`<option value="${x}">${esc(trChar(x))}</option>`).join('')}</select><label><input type="checkbox" data-p163-improved> Aplicação aprimorada (dificuldade 3; 1 Tensão)</label><button data-p163-apply>APLICAR ESTIMULANTES</button><label><input type="checkbox" data-p163-master> Médico especialista (2 Tensão; dificuldade -1)</label><label>Horas em bacta</label><input type="number" data-p163-hours min="2" max="72" step="2" value="2"><label><input type="checkbox" data-p163-facility> Tanque de bacta presente na cena</label><button data-p163-bacta ${d.combat?.active?'disabled':''}>RECUPERAR EM BACTA</button>`;
   b.append(p163);
   p163.querySelector('[data-p163-apply]')?.addEventListener('click',act(p,()=>{const characteristicKey=p163.querySelector('[data-p163-characteristic]').value;const extra=p163.querySelector('[data-p163-extra]').value;return call('applyStimApplication',{characteristicKey,additionalCharacteristics:extra?[extra]:[],improved:p163.querySelector('[data-p163-improved]').checked,targetCompanionId:targetCompanionId()})}));
   p163.querySelector('[data-p163-bacta]')?.addEventListener('click',act(p,()=>call('recoverInBacta',{hours:Number(p163.querySelector('[data-p163-hours]').value),facilityConfirmed:p163.querySelector('[data-p163-facility]').checked,targetCompanionId:targetCompanionId()})));
   p.querySelector('[data-p93-stim]')?.addEventListener('click',act(p,()=>call('usePlayerStimpack',{maneuverMethod:document.querySelector('[data-p94-method]')?.value||'normal'})));
   p.querySelector('[data-p93-med]')?.addEventListener('click',act(p,()=>call('useMedicine',{medicalSupplies:true,useMasterDoctor:Boolean(p163.querySelector('[data-p163-master]')?.checked),targetCompanionId:targetCompanionId()})));
   p.querySelector('[data-p93-rest]')?.addEventListener('click',act(p,()=>call('restFullNight')));
   return p
 }
 function mountCyber(){
   const r=call('cyberneticsSummary');const p=base('CIBERNÉTICOS','Aquisição passa pelo mercado/Mestre. Só implante já possuído pode ser instalado; nenhum botão concede item ou ignora créditos.');const b=p.querySelector('[data-p93-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const d=r.data||{},catalog=Object.values(d.catalog||{});
   b.innerHTML=`<div class="grid"><div class="card"><strong>${Number(d.totalCost||0)}/${Number(d.cap||0)} slots usados</strong><small>${Number(d.remainingSlots||0)} disponíveis</small></div><div class="card"><strong>${(d.owned||[]).length} adquirido(s)</strong><small>${(d.installed||[]).length} instalado(s)</small></div></div><h3>ADQUIRIDOS</h3>${(d.owned||[]).map(x=>`<div class="row"><span>${esc(x.name)}<small>${x.installed?'INSTALADO':'em posse'}</small></span>${x.installed?`<button class="secondary" data-p93-cyber-remove="${esc(x.assetId)}">REMOVER</button>`:`<button data-p93-cyber-install="${esc(x.assetId)}">INSTALAR</button>`}</div>`).join('')||'<p>Nenhum implante adquirido.</p>'}<h3>CATÁLOGO MECÂNICO</h3>${catalog.map(x=>`<div class="row"><span>${esc(x.name)}<small>${Number(x.cost||0)} cr · raridade ${Number(x.rarity||0)}${x.restricted?' · restrito':''}</small></span><button data-p93-cyber-buy="${esc(x.id)}">PROPOR COMPRA</button></div>`).join('')}`;
   p.querySelectorAll('[data-p93-cyber-buy]').forEach(btn=>btn.addEventListener('click',act(p,()=>call('proposeCyberneticAcquisition',btn.dataset.p93CyberBuy))));
   p.querySelectorAll('[data-p93-cyber-install]').forEach(btn=>btn.addEventListener('click',act(p,()=>call('installOwnedCybernetic',btn.dataset.p93CyberInstall))));
   p.querySelectorAll('[data-p93-cyber-remove]').forEach(btn=>btn.addEventListener('click',act(p,()=>call('removeOwnedCybernetic',btn.dataset.p93CyberRemove))));
   return p
 }
 function mount(){style();const r=route(),wanted=['02','04','07','08'];document.querySelectorAll('[data-p93-mechanics]').forEach(x=>{if(x.dataset.p93Mechanics!==r)x.remove()});if(r==='04')document.querySelector('[data-p92-mechanics="04"]')?.remove();if(!wanted.includes(r)||document.querySelector(`[data-p93-mechanics="${r}"]`))return;let p=null;if(r==='02')p=mountRecovery();else if(r==='04')p=mountHyperspace();else if(r==='07')p=mountTalents();else if(r==='08')p=mountCyber();if(p)host().appendChild(p)}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p93-mechanical-wave2',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P94 — P0 WAVE 3: Destiny visibility/spend, real range maneuvers, conditions visibility, attachment hard-point audit. */
(()=>{'use strict';
 if(window.__R54_P94_MECHANICS__)return;window.__R54_P94_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function style(){if(document.getElementById('r54-p94-style'))return;const e=document.createElement('style');e.id='r54-p94-style';e.textContent='.r54-p94{margin:12px 14px 112px;padding:13px;border:1px solid #665945;border-radius:12px;background:linear-gradient(180deg,#15120d,#0b0a08);color:#edf0ed;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p94 h2{margin:2px 0 4px;font-size:1.1rem;color:#f1d29b}.r54-p94 h3{margin:13px 0 7px;font-size:.86rem;color:#e5be79}.r54-p94 p{color:#b8b4a7}.r54-p94 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p94 .card{padding:9px;border:1px solid #40382b;border-radius:9px;background:#15120d}.r54-p94 .row{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:7px 0;border-top:1px solid #332d24}.r54-p94 .row:first-child{border-top:0}.r54-p94 small{display:block;color:#938d7e}.r54-p94 .controls{display:flex;flex-wrap:wrap;gap:7px;margin:8px 0}.r54-p94 button,.r54-p94 select{min-height:39px;border:1px solid #725f3d;border-radius:8px;background:#21170d;color:#f4d79d;padding:7px 9px;font-weight:800}.r54-p94 .secondary{border-color:#475f67;background:#0e1b20;color:#bee5ee}.r54-p94 output{display:block;margin-top:8px;padding:8px;border-radius:8px;background:#0c120e;color:#9bd9b0}.r54-p94 output[data-ok="0"]{color:#ff9d9d}.r54-p94 .badge{display:inline-block;padding:3px 7px;border-radius:999px;background:#2a241a;color:#ebca8c;font-size:.72rem;font-weight:900}.r54-p94 .warn{padding:8px;border-left:3px solid #9f7540;background:#22180d;color:#ebcb98}@media(max-width:430px){.r54-p94 .grid{grid-template-columns:1fr}.r54-p94 .controls>*{flex:1 1 100%}}';document.head.appendChild(e)}
 function base(title,sub){const p=document.createElement('section');p.className='r54-p94';p.dataset.p94Mechanics=route();p.innerHTML=`<small>P94 · MECÂNICA EXECUTÁVEL · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(sub)}</p><div data-p94-body></div><output data-p94-output>Pronto.</output>`;return p}
 function report(p,r){const o=p.querySelector('[data-p94-output]');if(o){o.textContent=r?.ok?'Ação aplicada e persistida.':String(r?.message||r?.error||'Ação rejeitada.');o.dataset.ok=r?.ok?'1':'0'}}
 function act(p,fn){return ev=>{const b=ev?.currentTarget;if(b)b.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}report(p,r);if(b)b.disabled=false;if(r?.ok)setTimeout(()=>{p.remove();mount()},80)}}
 function mountCombat(){
   const r=call('combatRuntimeStatus'),p=base('MANOBRAS / CONDIÇÕES','Todas as manobras pessoais usam o mesmo orçamento real. Se a manobra gratuita estiver indisponível por carga/Ferimento Crítico, ou se for a segunda manobra, escolha 2 de Tensão ou trocar a ação quando permitido.');const b=p.querySelector('[data-p94-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const d=r.data||{},c=d.combat||{},st=d.status||{},adv=c.adversaries||[];
   const effects=d.character?.statusEffects||[];
   if(!c.active){b.innerHTML=`<p class="warn">Nenhum combate ativo.</p><h3>CONDIÇÕES PERSISTENTES</h3>${effects.map(x=>`<div class="row"><span>${esc(x.type)}</span><small>${esc(x.duration||'')}</small></div>`).join('')||'<p>Nenhuma condição ativa.</p>'}`;return p}
   const weapons=d.equipment?.weapons||[];
   b.innerHTML=`<div class="grid"><div class="card"><strong>Manobras ${Number(c.playerTurn?.maneuversUsed||0)}/2</strong><small>Ação ${c.playerTurn?.actionUsed?'usada':'disponível'}</small></div><div class="card"><strong>${st.cannotAct?'SEM AÇÃO':'AÇÃO POSSÍVEL'} · ${st.cannotManeuver?'SEM MANOBRA':'MANOBRA POSSÍVEL'}</strong><small>Contratempo por estado: ${Number(st.setback||0)} · Cobertura ${Number(d.character?.combatEffects?.coverRangedDefense||0)}</small></div></div><h3>CONDIÇÕES ATIVAS</h3>${effects.map(x=>`<div class="row"><span>${esc(x.type)}</span><span class="badge">${esc(x.duration||'ativa')}${x.roundsRemaining!=null?' · '+Number(x.roundsRemaining):''}</span></div>`).join('')||'<p>Nenhuma condição ativa.</p>'}<h3>MANOBRAS</h3><div class="controls"><select data-p94-method><option value="normal">Manobra normal</option><option value="strain">Pagar 2 de Tensão</option><option value="action">Trocar a ação por manobra</option></select>${st.prone?'<button class="secondary" data-p99-stand>LEVANTAR-SE</button>':'<button class="secondary" data-p104-drop-prone>FICAR CAÍDO</button>'}<button data-p94-cover>BUSCAR COBERTURA</button><select data-p94-draw>${weapons.map((w,i)=>`<option value="${esc(w.id??i)}" ${w.active?'selected':''}>${esc(w.name)}${w.active?' · ativa':''}</option>`).join('')}</select><button data-p94-draw-btn>SACAR / TROCAR ARMA</button></div><h3>MOVER ENTRE FAIXAS</h3><div class="controls"><select data-p94-target>${adv.map(a=>`<option value="${esc(a.id)}">${esc(a.name)} · ${esc(a.range||'?')}</option>`).join('')}</select><button data-p94-closer>APROXIMAR</button><button class="secondary" data-p94-farther>AFASTAR</button></div>`;
   const opts=direction=>({targetAdversaryId:p.querySelector('[data-p94-target]')?.value||null,direction,maneuverMethod:p.querySelector('[data-p94-method]')?.value||'normal'});
   p.querySelector('[data-p94-closer]')?.addEventListener('click',act(p,()=>call('changeCombatRange',opts('closer'))));
   p.querySelector('[data-p94-farther]')?.addEventListener('click',act(p,()=>call('changeCombatRange',opts('farther'))));
   p.querySelector('[data-p99-stand]')?.addEventListener('click',act(p,()=>call('standUpFromProne',{maneuverMethod:p.querySelector('[data-p94-method]')?.value||'normal'})));
   p.querySelector('[data-p104-drop-prone]')?.addEventListener('click',act(p,()=>call('dropProne',{maneuverMethod:p.querySelector('[data-p94-method]')?.value||'normal'})));
   p.querySelector('[data-p94-cover]')?.addEventListener('click',act(p,()=>call('takeCover',{maneuverMethod:p.querySelector('[data-p94-method]')?.value||'normal'})));
   p.querySelector('[data-p94-draw-btn]')?.addEventListener('click',act(p,()=>call('drawWeaponInCombat',p.querySelector('[data-p94-draw]')?.value||'',{maneuverMethod:p.querySelector('[data-p94-method]')?.value||'normal'})));
   return p
 }
 function mountDestiny(){
   const r=call('destinySummary'),p=base('DESTINO','A reserva exibida é o estado persistente real. O jogador só pode gastar um ponto luminoso; ele vira sombrio pelo Rules Engine.');const b=p.querySelector('[data-p94-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const d=r.data||{},pool=d.pool||{};
   b.innerHTML=`<div class="grid"><div class="card"><strong>LUMINOSO ${Number(pool.light||0)}</strong><small>Disponível ao jogador</small></div><div class="card"><strong>SOMBRIO ${Number(pool.dark||0)}</strong><small>Reserva do Mestre</small></div></div><div class="controls"><button data-p94-destiny ${Number(pool.light||0)<1?'disabled':''}>GASTAR 1 DESTINO LUMINOSO</button></div>`;
   p.querySelector('[data-p94-destiny]')?.addEventListener('click',act(p,()=>call('spendDestiny','light')));return p
 }
 function mountAttachments(){
   const r=call('equipmentOwnedSummary'),p=base('HARD POINTS / ATTACHMENTS','Auditoria do estado real dos itens. P94 NÃO inventa attachment nem deixa instalar sem perfil mecânico e fonte; a instalação continua bloqueada até catálogo autorizado.');const b=p.querySelector('[data-p94-body]');
   if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;return p}const items=[...(r.data?.weapons||[]),...(r.data?.armor||[])].filter(x=>Number(x.hardPoints||0)>0||Number(x.attachments?.length||0)>0);
   b.innerHTML=items.map(x=>`<div class="row"><span>${esc(x.name)}<small>${esc(x.kind)} · ${Number(x.attachments?.length||0)} attachment(s)</small></span><strong>${Number(x.hardPointsAvailable||0)}/${Number(x.hardPoints||0)} HP livres</strong></div>`).join('')||'<p>Nenhum item com hard points disponível.</p>';return p
 }
 function mount(){style();const r=route(),wanted=['02','07','08'];document.querySelectorAll('[data-p94-mechanics]').forEach(x=>{if(x.dataset.p94Mechanics!==r)x.remove()});if(!wanted.includes(r)||document.querySelector(`[data-p94-mechanics="${r}"]`))return;let p=null;if(r==='02')p=mountCombat();else if(r==='07')p=mountDestiny();else if(r==='08')p=mountAttachments();if(p)host().appendChild(p)}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p94-mechanical-wave3',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();


/* R54 P95 — CHECK TYPES + SOCIAL RESOLUTION + VEHICLE DAMAGE CONTROL/CRITICAL REPAIR. */
(()=>{'use strict';
 if(window.__R54_P95_MECHANICS__)return;window.__R54_P95_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function style(){if(document.getElementById('r54-p99-style'))return;const e=document.createElement('style');e.id='r54-p99-style';e.textContent='.r54-p99{margin:12px 14px 112px;padding:13px;border:1px solid #315a64;border-radius:12px;background:linear-gradient(180deg,#0d171b,#081014);color:#edf4f5;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p99 h2{margin:2px 0 4px;font-size:1.1rem;color:#9cdeec}.r54-p99 h3{margin:13px 0 7px;font-size:.88rem;color:#b6e5ed}.r54-p99 p{color:#aabdc1}.r54-p99 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p99 .card{padding:10px;border:1px solid #28444c;border-radius:9px;background:#0e1a1e}.r54-p99 label{display:block;color:#c7dadd;font-weight:800;margin:6px 0 3px}.r54-p99 select,.r54-p99 input,.r54-p99 button{width:100%;min-height:39px;border:1px solid #3c6872;border-radius:8px;background:#10252b;color:#e9f6f8;padding:7px 9px;font-weight:750}.r54-p99 button{cursor:pointer;background:#153942;color:#bdebf4}.r54-p99 .secondary{background:#211b10;border-color:#75613c;color:#f0d59e}.r54-p99 output{display:block;margin-top:9px;padding:9px;border-radius:8px;background:#08191b;color:#a9e8c0;white-space:pre-wrap}.r54-p99 output[data-ok="0"]{color:#ff9d9d}.r54-p99 .row{padding:8px 0;border-top:1px solid #243b40}.r54-p99 .row:first-child{border-top:0}.r54-p99 small{display:block;color:#809ca2;line-height:1.35}.r54-p99 .tabs{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p99 .warn{padding:8px;border-left:3px solid #9d7741;background:#231a0d;color:#efcf98}@media(max-width:430px){.r54-p99 .grid,.r54-p99 .tabs{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function base(title,sub){const p=document.createElement('section');p.className='r54-p99';p.dataset.p95Mechanics=route();p.innerHTML=`<small>P95 · MECÂNICA EXECUTÁVEL · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(sub)}</p><div data-p95-body></div><output data-p95-output>Pronto.</output>`;return p}
 function report(p,r){const o=p.querySelector('[data-p95-output]');if(!o)return;const d=r?.data??r;if(r?.ok){const check=d?.check??d?.player??d;o.textContent=check&&typeof check==='object'?`Resolvido. Sucesso líquido: ${Number(check.netSuccess||0)} · Vantagem líquida: ${Number(check.netAdvantage||0)}${check.triumph?` · Triunfo ${Number(check.triumph)}`:''}${check.despair?` · Desespero ${Number(check.despair)}`:''}`:'Ação aplicada e persistida.';o.dataset.ok='1'}else{o.textContent=String(r?.message||r?.error||'Ação rejeitada.');o.dataset.ok='0'}}
 function act(p,fn,{refresh=false}={}){return ev=>{const b=ev?.currentTarget;if(b)b.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}report(p,r);if(b)b.disabled=false;if(refresh&&r?.ok)setTimeout(()=>{p.remove();mount()},80)}}
 function n(p,sel,def=0){return Number(p.querySelector(sel)?.value??def)}
 function optionsSkill(keys,labels){return keys.map(k=>`<option value="${esc(k)}">${esc(labels[k]||k.replaceAll('_',' '))}</option>`).join('')}
 function mountChecks(){
   const sr=call('readState'),cr=call('creationCatalog'),p=base('TESTES OPOSTOS / COMPETITIVOS / SOCIAIS','Estes testes usam o motor determinístico. Oposição converte a característica/perícia do oponente em Difficulty/Challenge; competição desempata por sucessos, Triunfos e Vantagens. Testes sociais usam a perícia de oposição própria da abordagem.');const b=p.querySelector('[data-p95-body]');
   if(!sr.ok||!sr.data?.player?.character){b.innerHTML=`<p class="warn">Crie ou carregue um personagem antes de resolver testes avançados.</p>`;return p}
   const state=sr.data,ch=state.player.character,labels=cr?.data?.skillLabels||{},skills=Object.keys(ch.skills||{}).sort((a,z)=>String(labels[a]||a).localeCompare(String(labels[z]||z),'pt-BR'));
   const social=['coaccion','engano','liderazgo','carisma','negociacion'].filter(k=>k in (ch.skills||{}));
   const last=state.social?.lastInfluence;
   b.innerHTML=`
   ${last?`<div class="card"><strong>ÚLTIMA RESOLUÇÃO SOCIAL</strong><small>${esc(labels[last.actingSkill]||last.actingSkill)} vs ${esc(labels[last.opposingSkill]||last.opposingSkill)} · sucesso líquido ${Number(last.check?.netSuccess||0)} · vantagem ${Number(last.check?.netAdvantage||0)}</small></div>`:''}
   <h3>CONFLITO SOCIAL</h3><div class="grid">
    <div><label>Abordagem</label><select data-p95-social-skill>${optionsSkill(social,labels)}</select></div>
    <div><label>Oposição em Negociação</label><select data-p95-social-opp><option value="">Automática</option><option value="negociacion">Negociação</option><option value="frialdad">Frialdade</option></select></div>
    <div><label>Característica do oponente</label><input data-p95-social-char type="number" min="0" max="7" value="3"></div>
    <div><label>Perícia do oponente</label><input data-p95-social-rank type="number" min="0" max="7" value="2"></div>
   </div><button data-p95-social>RESOLVER TESTE SOCIAL</button>
   <h3>INFLUÊNCIA RECEBIDA · NPC CONTRA JOGADOR</h3><p>Aplica automaticamente Nem um Pouco Tolo (AoR) quando adquirido. Não modifica os testes sociais iniciados pelo jogador.</p>
   <div class="grid"><div><label>Abordagem do NPC</label><select data-p149-incoming-skill><option value="carisma">Charme</option><option value="coaccion">Coerção</option><option value="engano">Enganação</option></select></div>
   <div><label>Característica do NPC</label><input data-p149-incoming-char type="number" min="0" max="6" value="3"></div>
   <div><label>Perícia do NPC</label><input data-p149-incoming-rank type="number" min="0" max="5" value="2"></div></div>
   <button data-p149-incoming>RESOLVER INFLUÊNCIA RECEBIDA</button>
   <h3>DEFESA INFORMÁTICA · AoR</h3><p>Invasão contra sistema do personagem. Programação defensiva adiciona Contratempos por graduação; a versão aprimorada troca esses Contratempos por melhorias de Dificuldade.</p>
   <div class="grid"><div><label>Intelecto do invasor</label><input data-p150-hack-char type="number" min="0" max="6" value="3"></div>
   <div><label>Computadores do invasor</label><input data-p150-hack-rank type="number" min="0" max="5" value="2"></div>
   <div><label>Dificuldade do sistema</label><input data-p150-hack-diff type="number" min="0" max="5" value="2"></div></div>
   <button data-p150-hack>RESOLVER INVASÃO CONTRA MEU SISTEMA</button>
   <h3>TESTE OPOSTO</h3><div class="grid">
    <div><label>Perícia</label><select data-p95-opposed-skill>${optionsSkill(skills,labels)}</select></div>
    <div><label>Característica da oposição</label><input data-p95-opposed-char type="number" min="0" max="7" value="3"></div>
    <div><label>Perícia da oposição</label><input data-p95-opposed-rank type="number" min="0" max="7" value="2"></div>
   </div><button data-p95-opposed>RESOLVER OPOSTO</button>
   <h3>TESTE COMPETITIVO</h3><div class="grid">
    <div><label>Perícia</label><select data-p95-comp-skill>${optionsSkill(skills,labels)}</select></div>
    <div><label>Dificuldade comum</label><input data-p95-comp-diff type="number" min="0" max="5" value="2"></div>
    <div><label>Característica do rival</label><input data-p95-comp-char type="number" min="0" max="7" value="3"></div>
    <div><label>Perícia do rival</label><input data-p95-comp-rank type="number" min="0" max="7" value="2"></div>
   </div><button data-p95-comp>RESOLVER COMPETIÇÃO</button>
   <h3>ASSISTÊNCIA</h3><div class="grid">
    <div><label>Perícia</label><select data-p95-assist-skill>${optionsSkill(skills,labels)}</select></div>
    <div><label>Dificuldade</label><input data-p95-assist-diff type="number" min="0" max="5" value="2"></div>
    <div><label>Característica do assistente</label><input data-p95-assist-char type="number" min="0" max="7" value="3"></div>
    <div><label>Perícia do assistente</label><input data-p95-assist-rank type="number" min="0" max="7" value="1"></div>
    <div><label>Tipo</label><select data-p95-assist-mode><option value="auto">Automático</option><option value="skilled">Hábil</option><option value="unskilled">Não-hábil</option></select></div>
   </div><button data-p95-assist>RESOLVER COM ASSISTÊNCIA</button>`;
   p.querySelector('[data-p95-social]')?.addEventListener('click',act(p,()=>{const opp=p.querySelector('[data-p95-social-opp]')?.value||null;return call('resolveSocialInfluenceCheck',{skillKey:p.querySelector('[data-p95-social-skill]')?.value,opponentSkillKey:opp,opponentCharacteristic:n(p,'[data-p95-social-char]'),opponentSkill:n(p,'[data-p95-social-rank]')})},{refresh:true}));
   p.querySelector('[data-p149-incoming]')?.addEventListener('click',act(p,()=>call('resolveIncomingSocialInfluenceCheck',{skillKey:p.querySelector('[data-p149-incoming-skill]')?.value,actorCharacteristic:n(p,'[data-p149-incoming-char]'),actorSkill:n(p,'[data-p149-incoming-rank]')}),{refresh:true}));
   p.querySelector('[data-p150-hack]')?.addEventListener('click',act(p,()=>call('resolveIncomingComputerIntrusionCheck',{actorCharacteristic:n(p,'[data-p150-hack-char]'),actorSkill:n(p,'[data-p150-hack-rank]'),baseDifficulty:n(p,'[data-p150-hack-diff]'),defendingOwnedSystem:true}),{refresh:true}));
   p.querySelector('[data-p95-opposed]')?.addEventListener('click',act(p,()=>call('resolveOpposedSkillCheck',{skillKey:p.querySelector('[data-p95-opposed-skill]')?.value,opponentCharacteristic:n(p,'[data-p95-opposed-char]'),opponentSkill:n(p,'[data-p95-opposed-rank]')})));
   p.querySelector('[data-p95-comp]')?.addEventListener('click',act(p,()=>call('resolveCompetitiveSkillCheck',{skillKey:p.querySelector('[data-p95-comp-skill]')?.value,difficulty:n(p,'[data-p95-comp-diff]'),opponentCharacteristic:n(p,'[data-p95-comp-char]'),opponentSkill:n(p,'[data-p95-comp-rank]')})));
   p.querySelector('[data-p95-assist]')?.addEventListener('click',act(p,()=>{const m=p.querySelector('[data-p95-assist-mode]')?.value||'auto';return call('resolveAssistedSkillCheck',{skillKey:p.querySelector('[data-p95-assist-skill]')?.value,difficulty:n(p,'[data-p95-assist-diff]'),assistantCharacteristic:n(p,'[data-p95-assist-char]'),assistantSkill:n(p,'[data-p95-assist-rank]'),skilled:m==='auto'?null:m==='skilled'})}));
   return p
 }
 function mountVehicle(){
   const vr=call('vehicleRuntimeStatus'),p=base('AÇÕES / CRÍTICOS DA NAVE','Controle de Danos usa Mecânica e consome a ação de combate quando aplicável. Críticos só podem ser reparados pelo fluxo de reparo; não existe botão para infligir crítico à própria nave.');const b=p.querySelector('[data-p95-body]');
   if(!vr.ok||!vr.data?.activeVehicle){b.innerHTML='<p class="warn">Nenhuma nave/veículo ativo.</p>';return p}
   const d=vr.data,v=d.activeVehicle,combat=d.vehicles?.combat||{},hits=Array.isArray(v.criticalHits)?v.criticalHits:[];
   b.innerHTML=`<div class="grid"><div class="card"><strong>${esc(v.name||v.shortName||'Veículo')}</strong><small>Casco ${Number(v.hullDamage||0)} · Tensão ${Number(v.systemStrain||0)}</small></div><div class="card"><strong>${combat.active?'EM COMBATE':'FORA DE COMBATE'}</strong><small>Ação ${combat.actionUsed?'usada':'disponível'}</small></div></div>
   ${route()==='03'?'<h3>CONTROLE DE DANOS</h3><label><input data-p95-hull-allowed type="checkbox"> O contexto permite acessar/reparar o casco</label><div class="grid"><button data-p95-damage-control="system">RECUPERAR TENSÃO DE SISTEMAS</button><button data-p95-damage-control="hull">REPARAR TRAUMA DE CASCO</button></div>':''}
   <h3>DANOS CRÍTICOS</h3>${hits.length?hits.map((h,i)=>`<div class="row"><strong>${esc(h.title||h.key||'Crítico')} ${h.repaired?'· REPARADO':''}</strong><small>${esc(h.effect||'')} · dificuldade de reparo ${esc(h.repairDifficulty??'?')}</small>${!h.repaired?`<button class="secondary" data-p95-repair="${i}">REPARAR CRÍTICO</button>`:''}</div>`).join(''):'<p>Nenhum dano crítico registrado.</p>'}`;
   p.querySelectorAll('[data-p95-damage-control]').forEach(btn=>btn.addEventListener('click',act(p,()=>call('vehicleDamageControl',{mode:btn.dataset.p95DamageControl,gmAllowed:Boolean(p.querySelector('[data-p95-hull-allowed]')?.checked)}),{refresh:true})));
   p.querySelectorAll('[data-p95-repair]').forEach(btn=>btn.addEventListener('click',act(p,()=>call('attemptRepairActiveVehicleCritical',Number(btn.dataset.p95Repair),{encounterEnded:!combat.active}),{refresh:true})));
   return p
 }
 function mount(){style();const r=route(),wanted=['01','03','11'];document.querySelectorAll('[data-p95-mechanics]').forEach(x=>{if(x.dataset.p95Mechanics!==r)x.remove()});if(!wanted.includes(r)||document.querySelector(`[data-p95-mechanics="${r}"]`))return;let p=null;if(r==='01')p=mountChecks();else p=mountVehicle();if(p)host().appendChild(p)}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p95-mechanical-wave4',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P96 — SOURCE-BOUND WEAPON QUALITIES + VEHICLE CRITICAL ACTIVATION. */
(()=>{'use strict';
 if(window.__R54_P96_MECHANICS__)return;window.__R54_P96_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function style(){if(document.getElementById('r54-p99-style'))return;const e=document.createElement('style');e.id='r54-p99-style';e.textContent='.r54-p99{margin:12px 14px 112px;padding:13px;border:1px solid #5e4c2a;border-radius:12px;background:linear-gradient(180deg,#17130c,#0d0a07);color:#f4eee0;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p99 h2{margin:2px 0 4px;font-size:1.08rem;color:#f0d08d}.r54-p99 h3{margin:13px 0 7px;font-size:.88rem;color:#e4c990}.r54-p99 p{color:#c0b49b}.r54-p99 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p99 .card{padding:10px;border:1px solid #4c4027;border-radius:9px;background:#1b170e}.r54-p99 button,.r54-p99 select{width:100%;min-height:39px;border:1px solid #7e6738;border-radius:8px;background:#2a2213;color:#f6e5b4;padding:7px 9px;font-weight:800}.r54-p99 .secondary{background:#142228;border-color:#355f6a;color:#bfeaf2}.r54-p99 output{display:block;margin-top:9px;padding:9px;border-radius:8px;background:#111009;color:#bde9c7;white-space:pre-wrap}.r54-p99 output[data-ok="0"]{color:#ffaaaa}.r54-p99 small{display:block;color:#9f947d;line-height:1.35}.r54-p99 .warn{padding:8px;border-left:3px solid #a26a45;background:#28170f;color:#f2c8a7}@media(max-width:430px){.r54-p99 .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function base(title,sub){const p=document.createElement('section');p.className='r54-p99';p.dataset.p96Mechanics=route();p.innerHTML=`<small>P96 · QUALIDADES / CRÍTICOS VEICULARES · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(sub)}</p><div data-p96-body></div><output data-p96-output>Pronto.</output>`;return p}
 function report(p,r){const o=p.querySelector('[data-p96-output]');if(!o)return;if(r?.ok){const d=r.data??{};o.textContent=d?.componentRequired?`O resultado crítico exige escolher um componente (${esc(d.preview?.title||d.preview?.key||'crítico')}).`:'Ação aplicada e persistida.';o.dataset.ok='1'}else{o.textContent=String(r?.message||r?.error||'Ação rejeitada.');o.dataset.ok='0'}}
 function act(p,fn,{refresh=false}={}){return ev=>{const b=ev?.currentTarget;if(b)b.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}report(p,r);if(b)b.disabled=false;if((refresh||r?.data?.componentRequired)&&r?.ok)setTimeout(()=>{p.remove();mount()},80)}}
 function mountWeapon(){const er=call('equipmentOwnedSummary'),p=base('QUALIDADES DA ARMA','Preparação, Munição Limitada, Cadência Lenta, Imprecisa, Aparatosa/Desajeitada e bônus defensivos são aplicados pelo motor — esta interface apenas expõe ações legítimas.');const b=p.querySelector('[data-p96-body]');if(!er.ok){b.innerHTML=`<p class="warn">${esc(er.message)}</p>`;return p}const e=er.data||{},weapons=e.weapons||[],w=weapons.find(x=>x.active||x.equipped)||weapons[0];if(!w){b.innerHTML='<p class="warn">Nenhuma arma possuída.</p>';return p}const u=w.usage||{},rt=w.runtimeState||{},q=(w.qualities||[]).join(' · '),stunSetting=/modo atordoante|stun setting/i.test(q);const prep=Number(u.prepare||0),ammo=Number(u.limitedAmmo||0),slow=Number(u.slowFiring||0);b.innerHTML=`<div class="grid"><div class="card"><strong>${esc(w.name)}</strong><small>${esc(q||'Sem qualidades especiais')}</small></div><div class="card"><strong>ESTADO MECÂNICO</strong><small>${prep?`Preparação ${Number(rt.preparedManeuvers||0)}/${prep}`:'Sem Preparação'} · ${ammo?`Munição ${rt.limitedAmmoRemaining===undefined?ammo:Number(rt.limitedAmmoRemaining)}/${ammo}`:'Munição padrão'}${slow?` · Cadência Lenta ${slow}`:''}</small></div></div><div class="grid">${prep?'<button data-p96-prepare>PREPARAR ARMA</button>':''}${ammo?'<button class="secondary" data-p96-reload>USAR RECARGA ADICIONAL</button>':''}${stunSetting?`<button class="secondary" data-p97-stun-setting>${rt.stunSettingActive?'DESATIVAR':'ATIVAR'} MODO ATORDOANTE</button>`:''}</div><small>Imprecisa: ${Number(u.inaccurate||0)} Contratempo(s) · penalidade Aparatosa: +${Number(u.cumbersomeDifficulty||0)} dificuldade · penalidade Desajeitada: +${Number(u.unwieldyDifficulty||0)} dificuldade · Defesa corpo a corpo: +${Number(u.defensive||0)} · Defesa à distância: +${Number(u.deflection||0)}</small>`;p.querySelector('[data-p96-prepare]')?.addEventListener('click',act(p,()=>call('prepareWeaponForAttack',w.id??w.name,{maneuverMethod:document.querySelector('[data-p94-method]')?.value||'normal'}),{refresh:true}));p.querySelector('[data-p96-reload]')?.addEventListener('click',act(p,()=>call('reloadLimitedAmmoWeapon',w.id??w.name,{maneuverMethod:document.querySelector('[data-p94-method]')?.value||'normal'}),{refresh:true}));p.querySelector('[data-p97-stun-setting]')?.addEventListener('click',act(p,()=>call('toggleWeaponStunSetting',w.id??w.name,{}),{refresh:true}));return p}
 function mountVehicle(){const vr=call('vehicleRuntimeStatus'),p=base('CRÍTICO VEICULAR DO ALVO','Um Critical Hit só aparece após um ataque veicular real que cause dano e produza vantagem/Triunfo suficiente. A rolagem usa a tabela veicular, críticos anteriores e escolha de componente quando exigida.');const b=p.querySelector('[data-p96-body]');if(!vr.ok||!vr.data?.vehicles){b.innerHTML='<p class="warn">Estado veicular indisponível.</p>';return p}const combat=vr.data.vehicles.combat||{},pending=combat.pendingTargetCritical;if(!pending){b.innerHTML='<p>Nenhum Critical Hit veicular aguardando ativação.</p>';return p}const choices=pending.componentChoices||[];b.innerHTML=`<div class="card"><strong>${esc(pending.weapon||'Arma veicular')}</strong><small>Crítico disponível ${pending.byTriumph?'por Triunfo':`por custo ${Number(pending.cost||0)}`} · alvo ${esc(pending.targetVehicleId||'')}</small></div>${pending.componentRequired?`<label>Componente atingido</label><select data-p96-component>${choices.map(c=>`<option value="${esc(c)}">${esc(c.replaceAll('_',' '))}</option>`).join('')}</select><small>A rolagem crítica já foi preservada; escolher o componente não rerrola o d100.</small>`:''}<button data-p96-activate>ATIVAR CRÍTICO VEICULAR</button>`;p.querySelector('[data-p96-activate]')?.addEventListener('click',act(p,()=>call('activatePendingVehicleCritical',{component:p.querySelector('[data-p96-component]')?.value||null}),{refresh:true}));return p}
 function mount(){style();const r=route(),wanted=['02','03'];document.querySelectorAll('[data-p96-mechanics]').forEach(x=>{if(x.dataset.p96Mechanics!==r)x.remove()});if(!wanted.includes(r)||document.querySelector(`[data-p96-mechanics="${r}"]`))return;const p=r==='02'?mountWeapon():mountVehicle();if(p)host().appendChild(p)}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p96-quality-vehicle-critical',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P98 — VEHICLE QUALITY SPEND: Guided/Linked/Ion/Tractor + vehicle ammo/cadence. */
(()=>{'use strict';
 if(window.__R54_P98_VEHICLE_QUALITIES__)return;window.__R54_P98_VEHICLE_QUALITIES__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function style(){if(document.getElementById('r54-p99-style'))return;const e=document.createElement('style');e.id='r54-p99-style';e.textContent='.r54-p99{margin:12px 14px 112px;padding:13px;border:1px solid #4a5b70;border-radius:12px;background:linear-gradient(180deg,#0d151d,#080d12);color:#eaf2f7;font-size:.87rem;box-shadow:0 10px 28px #0008}.r54-p99 h2{margin:2px 0 4px;color:#91d7ff;font-size:1.08rem}.r54-p99 h3{margin:13px 0 7px;color:#e4c990;font-size:.88rem}.r54-p99 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p99 .card{padding:9px;border:1px solid #304353;border-radius:9px;background:#111b24}.r54-p99 button,.r54-p99 select{width:100%;min-height:39px;border:1px solid #4b6f86;border-radius:8px;background:#102532;color:#dff5ff;padding:7px 9px;font-weight:800}.r54-p99 button.secondary{border-color:#806a3d;background:#2a2112;color:#f5dda1}.r54-p99 button:disabled{opacity:.45}.r54-p99 small{display:block;color:#91a8b5;line-height:1.35}.r54-p99 output{display:block;margin-top:9px;padding:9px;border-radius:8px;background:#081117;color:#aee7c1;white-space:pre-wrap}.r54-p99 output[data-ok="0"]{color:#ffabab}.r54-p99 .row{display:flex;justify-content:space-between;gap:8px;align-items:center;padding:7px 0;border-top:1px solid #263743}.r54-p99 .row:first-child{border-top:0}@media(max-width:430px){.r54-p99 .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function report(p,r){const o=p.querySelector('[data-p98-output]');if(!o)return;o.dataset.ok=r?.ok?'1':'0';o.textContent=r?.ok?'Ação aplicada e persistida.':String(r?.message||r?.error||'Ação rejeitada.');}
 function act(p,fn,{refresh=true}={}){return ev=>{const b=ev?.currentTarget;if(b)b.disabled=true;let r;try{r=fn()}catch(e){r={ok:false,message:String(e?.message||e)}}report(p,r);if(b)b.disabled=false;if(refresh&&r?.ok)setTimeout(()=>{p.remove();mount()},80)}}
 function mount(){style();if(route()!=='03'){document.querySelectorAll('[data-p98-vehicle]').forEach(x=>x.remove());return}if(document.querySelector('[data-p98-vehicle]'))return;const vr=call('vehicleRuntimeStatus');const p=document.createElement('section');p.className='r54-p99';p.dataset.p98Vehicle='1';p.innerHTML='<small>P165 · QUALIDADES VEICULARES / TALENTOS AoR · SEM RAG</small><h2>ARMAS / MÍSSEIS / RAIO TRATOR</h2><div data-p98-body></div><output data-p98-output>Pronto.</output>';const b=p.querySelector('[data-p98-body]');if(!vr.ok||!vr.data?.activeVehicle){b.innerHTML='<p>Nenhuma nave/veículo ativo.</p>';host().appendChild(p);return}const d=vr.data,active=d.activeVehicle,combat=d.vehicles?.combat||{},target=d.vehicles?.vehicles?.[combat.targetVehicleId]||null,weapons=active.weapons||[],spend=combat.pendingVehicleSpend||null,retries=combat.guidedReactivations||[],status=active.tractorLocks?.length?{tractorLocked:true,tractorDifficulty:Math.max(...active.tractorLocks.map(x=>Number(x.rating)||0))}:{tractorLocked:false,tractorDifficulty:0};
 const weaponRows=weapons.map((w,i)=>{const q=w.qualities||{},rt=w.runtimeState||{},ammo=Number(q.limitedAmmo||0),slow=Number(q.slowFiring||0);return `<option value="${i}">${esc(w.name)}${q.ion?' · IÔNICA':''}${q.guided?` · GUIADA ${q.guided}`:''}${q.linked?` · VINCULADA ${q.linked}`:''}${q.tractor?` · TRATOR ${q.tractor}`:''}${ammo?` · MUNIÇÃO ${rt.limitedAmmoRemaining===undefined?ammo:rt.limitedAmmoRemaining}/${ammo}`:''}${slow?` · CADÊNCIA ${slow}`:''}</option>`}).join('');
 const qualityButtons=spend?(spend.qualities||[]).map(q=>{const used=Number(spend.activations?.[q.key]||0),blocked=Number(spend.remainingAdvantage||0)<Number(q.cost||0)||(!q.repeatable&&used>0)||(q.key==='linked'&&used>=Number(q.rank||0));return `<button class="secondary" data-p98-quality="${esc(q.key)}" ${blocked?'disabled':''}>${esc(q.label)} · ${Number(q.cost||0)} Vant.${used?` · x${used}`:''}</button>`}).join(''):'';
 const braceStatus=call('aorBraceStatus'),brace=braceStatus?.ok?braceStatus.data:{rank:0,prepared:false,pending:null};
 const braceControls=Number(brace.rank||0)>0?`<h3>BRACE · ${Number(brace.rank)} graduação(ões)</h3>${brace.prepared?`<div class="card"><strong>BRACE PREPARADO</strong><small>A próxima ação veicular pode remover Setbacks ambientais até o limite de graduação.</small></div>`:`<select data-p98-brace-method><option value="normal">Usar a manobra disponível</option><option value="strain">2ª manobra: sofrer 2 de Tensão de sistemas</option><option value="action">2ª manobra: trocar a ação</option></select><button data-p98-brace>PREPARAR BRACE · MANOBRA</button>`}`:'';
 b.innerHTML=`<div class="grid"><div class="card"><strong>${esc(active.name||active.shortName||'Nave')}</strong><small>Casco ${Number(active.hullDamage||0)} · Tensão de sistemas ${Number(active.systemStrain||0)}</small></div><div class="card"><strong>${target?esc(target.name||target.shortName||'Alvo'):'SEM ALVO'}</strong><small>${target?`Casco ${Number(target.hullDamage||0)} · Tensão ${Number(target.systemStrain||0)} · Vel. máx. efetiva ${Number(d.targetStatus?.effectiveSpeedMax??target.speedMax)} · Defesa proa ${Number(d.targetStatus?.defense?.proa??target.defense?.proa??0)}`:'Inicie combate veicular'}</small></div></div>${combat.active?`<h3>ATACAR</h3><select data-p98-weapon>${weaponRows}</select>${braceControls}<label>Setbacks ambientais confirmados pelo Mestre<input data-p98-brace-environment type="number" min="0" max="20" value="0"></label><button data-p98-attack>DISPARAR ARMA SELECIONADA</button>`:''}${spend?`<h3>RESULTADOS DO ÚLTIMO ATAQUE</h3><div class="card"><strong>Vantagens ${Number(spend.remainingAdvantage||0)} · Triunfos ${Number(spend.remainingTriumph||0)}</strong><small>Crítico, Guiada, Vinculada, Sobrecarregar defesas e Disparo debilitante compartilham o mesmo orçamento. Símbolos gastos não podem ser reutilizados.</small></div><div class="grid">${qualityButtons}</div>`:''}${retries.length?`<h3>GUIADA — NOVA PASSAGEM</h3>${retries.map(x=>`<div class="row"><span>Projétil ${esc(x.id)}<small>${Number(x.remainingAdvantage||0)} Vantagens da tentativa anterior</small></span><button class="secondary" data-p98-reguided="${esc(x.id)}">REATIVAR GUIADA · 3 VANT.</button></div>`).join('')}`:''}${status.tractorLocked?`<h3>RAIO TRATOR</h3><div class="card"><strong>Nave presa · dificuldade ${status.tractorDifficulty}</strong><small>Movimento permanece bloqueado até um teste de Pilotagem bem-sucedido.</small></div><button data-p98-tractor>TESTAR FUGA DO TRATOR</button>`:''}`;
 p.querySelector('[data-p98-brace]')?.addEventListener('click',act(p,()=>call('prepareAoRBrace',{maneuverMethod:p.querySelector('[data-p98-brace-method]')?.value||'normal'})));
 p.querySelector('[data-p98-attack]')?.addEventListener('click',act(p,()=>call('resolveVehicleAttack',{weaponIndex:Number(p.querySelector('[data-p98-weapon]')?.value||0),braceEnvironmentalSetback:Number(p.querySelector('[data-p98-brace-environment]')?.value||0)})));
 p.querySelectorAll('[data-p98-quality]').forEach(x=>x.addEventListener('click',act(p,()=>call('activatePendingVehicleQuality',{qualityKey:x.dataset.p98Quality}))));
 p.querySelectorAll('[data-p98-reguided]').forEach(x=>x.addEventListener('click',act(p,()=>call('reactivateGuidedProjectile',x.dataset.p98Reguided))));
 p.querySelector('[data-p98-tractor]')?.addEventListener('click',act(p,()=>call('attemptTractorEscape',{})));
 host().appendChild(p);document.querySelector('[data-p92-vattack]')?.setAttribute('style','display:none!important');}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p98-vehicle-qualities',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();


/* R54 P99 — ATTACHMENTS SOURCE-BOUND + KNOCKDOWN/PRONE CLOSURE. */
(()=>{'use strict';
 if(window.__R54_P99_ATTACHMENTS__)return;window.__R54_P99_ATTACHMENTS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 function style(){if(document.getElementById('r54-p99-attachment-style'))return;const e=document.createElement('style');e.id='r54-p99-attachment-style';e.textContent='.r54-p99-att{margin:12px 14px 112px;padding:13px;border:1px solid #534a31;border-radius:12px;background:linear-gradient(180deg,#17150e,#0d0c08);color:#f1efe7;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p99-att h2{margin:2px 0 5px;color:#ead59f}.r54-p99-att h3{font-size:.9rem;color:#d9c28f;margin:13px 0 7px}.r54-p99-att .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p99-att .row{padding:9px 0;border-top:1px solid #3a3423}.r54-p99-att select,.r54-p99-att button{width:100%;min-height:39px;border:1px solid #665a36;border-radius:8px;background:#252114;color:#f2e7c9;padding:7px 9px;font-weight:750}.r54-p99-att button{cursor:pointer}.r54-p99-att small{display:block;color:#a89d82}.r54-p99-att output{display:block;margin-top:9px;padding:9px;border-radius:8px;background:#111008;color:#a9e8c0;white-space:pre-wrap}.r54-p99-att .warn{color:#f0bd8e}.r54-p99-att .installed{color:#9ed0ff}@media(max-width:430px){.r54-p99-att .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function report(p,r){const o=p.querySelector('output');if(!o)return;o.textContent=r?.ok?'Ação aplicada e persistida.':String(r?.message||'Ação rejeitada.');o.style.color=r?.ok?'#a9e8c0':'#ff9d9d'}
 function mount(){
   style();if(route()!=='08'){document.querySelectorAll('[data-p99-attachments]').forEach(x=>x.remove());return}
   document.querySelector('[data-p94-mechanics="08"]')?.remove();
   if(document.querySelector('[data-p99-attachments]'))return;
   const r=call('attachmentSummary'),p=document.createElement('section');p.className='r54-p99-att';p.dataset.p99Attachments='1';p.innerHTML='<small>P99 · ATTACHMENTS MECÂNICOS · SEM RAG</small><h2>ATTACHMENTS / HARD POINTS</h2><p>Somente perfis com efeito mecânico explícito e validado entram aqui. Compra usa a aquisição normal; instalação exige propriedade, compatibilidade e hard points. Mods source-bound com teste/custo completos aparecem no painel P100; perfis sem regra estruturada continuam bloqueados.</p><div data-body></div><output>Pronto.</output>';
   const b=p.querySelector('[data-body]');if(!r.ok){b.innerHTML=`<p class="warn">${esc(r.message)}</p>`;host().appendChild(p);return}
   const d=r.data||{},catalog=d.catalog||[],owned=d.owned||[],weapons=d.weapons||[];
   b.innerHTML=`<h3>ADQUIRIR</h3><div class="grid"><select data-cat>${catalog.map(a=>`<option value="${esc(a.id)}">${esc(a.name)} · ${Number(a.price||0)} cr · R${Number(a.rarity||0)} · ${Number(a.hardPointsRequired||0)} HP</option>`).join('')}</select><button data-buy>PROPOR AQUISIÇÃO AO MESTRE</button></div><h3>INSTALAR / REMOVER</h3>${owned.length?owned.map(a=>{const installed=Boolean(a.state?.installed);return `<div class="row"><strong>${esc(a.name)}</strong><small class="${installed?'installed':''}">${installed?`Instalado em ${esc(a.state?.targetWeaponId||'arma')}`:'Possuído · não instalado'}</small>${installed?`<button data-remove="${esc(a.assetId)}">REMOVER</button>`:`<div class="grid"><select data-weapon="${esc(a.assetId)}">${weapons.map(w=>`<option value="${esc(w.id)}">${esc(w.name)} · ${Number(w.hardPointsAvailable||0)}/${Number(w.hardPoints||0)} HP livres</option>`).join('')}</select><button data-install="${esc(a.assetId)}">INSTALAR</button></div>`}</div>`}).join(''):'<p>Nenhum attachment adquirido.</p>'}<h3>ARMAS / HARD POINTS</h3>${weapons.map(w=>`<div class="row"><strong>${esc(w.name)}</strong><small>${Number(w.hardPointsAvailable||0)}/${Number(w.hardPoints||0)} HP livres · ${(w.attachments||[]).map(a=>esc(a.name)).join(', ')||'sem attachments'}</small></div>`).join('')}`;
   p.querySelector('[data-buy]')?.addEventListener('click',()=>{const btn=p.querySelector('[data-buy]');btn.disabled=true;const x=call('proposeAttachmentAcquisition',p.querySelector('[data-cat]')?.value||'');report(p,x);btn.disabled=false});
   p.querySelectorAll('[data-install]').forEach(btn=>btn.addEventListener('click',()=>{const id=btn.dataset.install,sel=p.querySelector(`[data-weapon="${CSS.escape(id)}"]`);const x=call('installOwnedAttachment',id,sel?.value||'');report(p,x);if(x.ok){p.remove();setTimeout(mount,60)}}));
   p.querySelectorAll('[data-remove]').forEach(btn=>btn.addEventListener('click',()=>{const x=call('removeOwnedAttachment',btn.dataset.remove||'');report(p,x);if(x.ok){p.remove();setTimeout(mount,60)}}));
   host().appendChild(p)
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p99-attachments-source-bound',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();


/* R54 P100 — GUARDED STANCE + SOURCE-BOUND ATTACHMENT MODS. */
(()=>{'use strict';
 if(window.__R54_P100_MECHANICS__)return;window.__R54_P100_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)} }};
 function style(){if(document.getElementById('r54-p101-style'))return;const e=document.createElement('style');e.id='r54-p101-style';e.textContent='.r54-p101-mods{margin:8px 0;padding:10px;border:1px solid #4d462f;border-radius:9px;background:#121008}.r54-p101-mods h3{margin:0 0 7px;color:#ead59f}.r54-p101-mods .modrow{padding:8px 0;border-top:1px solid #342f21}.r54-p101-mods button{width:100%;min-height:36px;margin-top:5px;border:1px solid #665a36;border-radius:7px;background:#262114;color:#f2e7c9;font-weight:750}.r54-p101-guard{margin-top:7px;width:100%;min-height:39px;border:1px solid #665a36;border-radius:8px;background:#2a2416;color:#f3e7c5;font-weight:800}.r54-p101-note{font-size:.78rem;color:#bdb093}';document.head.appendChild(e)}
 function refresh(){document.querySelector('[data-p99-attachments]')?.remove();document.querySelector('[data-p100-mods]')?.remove();setTimeout(()=>window.dispatchEvent(new Event('popstate')),30)}
 function mountGuard(){
   if(route()!=='02')return;
   const host=document.querySelector('[data-p94-mechanics="02"]');if(!host||host.querySelector('[data-p100-guard]'))return;
   const btn=document.createElement('button');btn.className='r54-p101-guard';btn.dataset.p100Guard='1';btn.textContent='POSTURA DEFENSIVA';
   const note=document.createElement('small');note.className='r54-p101-note';note.textContent='Consome uma manobra: +1 Setback nos seus testes de combate e nos ataques corpo a corpo contra você até o fim do próximo turno.';
   btn.addEventListener('click',()=>{btn.disabled=true;const method=host.querySelector('[data-p94-method]')?.value||'normal';const r=call('takeGuardedStance',{maneuverMethod:method});const out=host.querySelector('output');if(out)out.textContent=r?.ok?'Postura defensiva ativada e persistida.':String(r?.message||'Ação rejeitada.');btn.disabled=false});
   host.querySelector('[data-p94-body]')?.append(btn,note);
 }
 function mountMods(){
   if(route()!=='08')return;
   const panel=document.querySelector('[data-p99-attachments]');if(!panel||panel.querySelector('[data-p100-mods]'))return;
   const r=call('attachmentSummary');if(!r?.ok)return;
   const installed=(r.data?.owned||[]).filter(a=>a.state?.installed&&Array.isArray(a.modStatus)&&a.modStatus.length);
   const box=document.createElement('div');box.className='r54-p101-mods';box.dataset.p100Mods='1';
   box.innerHTML=`<h3>MODS DE ATTACHMENTS</h3><p class="r54-p101-note">Cada tentativa custa 100 créditos (50 com Faz-tudo AoR) e exige Kit de ferramentas. A primeira é Difícil 3; cada Mod já instalado aumenta a dificuldade em +1. Falha consome aquela opção; somente efeitos comprovados pela rolagem e pelo perfil source-bound são persistidos.</p>${installed.length?installed.map(a=>`<div class="modrow"><strong>${esc(a.name)}</strong><small>${a.state?.useless?'INUTILIZADO':'instalado'}</small>${a.modStatus.map(m=>`<div><span>${esc(m.name)} · instalados ${Number(m.installed||0)}/${Number(m.max||1)} · falhas ${Number(m.failed||0)} · Dif. ${Number(m.difficulty||3)}</span>${Number(m.remaining||0)>0&&!a.state?.useless?`<button data-p100-mod="${esc(a.assetId)}" data-mod-id="${esc(m.id)}">TENTAR MOD · ${Number(m.cost||100)} cr</button>`:''}</div>`).join('')}</div>`).join(''):'<p>Nenhum attachment instalado com Mods source-bound disponíveis.</p>'}<output>Pronto.</output>`;
   panel.appendChild(box);
   box.querySelectorAll('[data-p100-mod]').forEach(btn=>btn.addEventListener('click',()=>{btn.disabled=true;const x=call('attemptAttachmentMod',btn.dataset.p100Mod||'',btn.dataset.modId||'');box.querySelector('output').textContent=x?.ok?(x.data?.success?'Mod instalado.':'Falha: a tentativa foi consumida.'):String(x?.message||'Mod rejeitado.');btn.disabled=false;if(x?.ok)setTimeout(refresh,80)}));
 }
 function mount(){style();mountGuard();mountMods()}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p100-guarded-mods',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();



/* R54 P101 — FEAR + ENCUMBRANCE + OWNERSHIP-SAFE GEAR ASSIST + DROID REPAIR. */
(()=>{'use strict';
 if(window.__R54_P101_MECHANICS__)return;window.__R54_P101_MECHANICS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{const r=b[name](...args);return r&&typeof r==='object'?r:{ok:false,message:'Resposta inválida do motor.'}}catch(e){return {ok:false,message:String(e?.message||e||'Falha mecânica.')}}};
 const host=()=>document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
 const num=(p,q,d=0)=>Number(p.querySelector(q)?.value??d)||0;
 function style(){if(document.getElementById('r54-p101-style'))return;const e=document.createElement('style');e.id='r54-p101-style';e.textContent=
 '.r54-p101{margin:12px 14px 112px;padding:13px;border:1px solid #4e655e;border-radius:12px;background:linear-gradient(180deg,#101825,#090d0b);color:#edf0ed;box-shadow:0 10px 28px #0008;font-size:.87rem}.r54-p101 h2{margin:2px 0 5px;color:#b7e1cc;font-size:1.08rem}.r54-p101 h3{margin:13px 0 6px;color:#d8c384;font-size:.86rem}.r54-p101 p,.r54-p101 small{color:#aeb9b1;line-height:1.35}.r54-p101 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p101 .card{padding:9px;border:1px solid #2d3935;border-radius:9px;background:#0b100e}.r54-p101 label{display:grid;gap:3px;color:#c8d0ca}.r54-p101 select,.r54-p101 input,.r54-p101 button{min-height:42px;border-radius:8px;border:1px solid #4b5b55;background:#111916;color:#eff4f0;padding:7px}.r54-p101 button{font-weight:700}.r54-p101 output{display:block;margin-top:9px;color:#cfe9d9;white-space:pre-wrap}.r54-p101 .warn{color:#e0b58a}@media(max-width:560px){.r54-p101 .grid{grid-template-columns:1fr}}';
 document.head.appendChild(e)}
 function base(title,sub){const p=document.createElement('section');p.className='r54-p101';p.dataset.p101Mechanics=route();p.innerHTML=`<small>P101 · MECÂNICA EXECUTÁVEL · SEM RAG</small><h2>${esc(title)}</h2><p>${esc(sub)}</p><div data-p101-body></div><output data-p101-output>Pronto.</output>`;return p}
 function report(p,r,okText='Ação aplicada e persistida.'){const o=p.querySelector('[data-p101-output]');if(!o)return;o.dataset.ok=r?.ok?'1':'0';o.textContent=r?.ok?okText:String(r?.message||r?.error||'Ação rejeitada.')}
 function mountSession(){
   const sr=call('readState'),er=call('encumbranceStatus'),p=base('MEDO · CARGA · TESTES COM EQUIPAMENTO','Medo, Sobrecarga e assistência de equipamento passam pelo Rules Engine oficial. Equipamento precisa estar realmente possuído; esta tela não concede itens nem aplica estados manualmente.');const b=p.querySelector('[data-p101-body]');
   if(!sr.ok||!sr.data?.player?.character){b.innerHTML='<p class="warn">Crie ou carregue um personagem antes de usar estas regras.</p>';return p}
   const st=sr.data,ch=st.player.character,gear=(ch.gear||[]).filter(x=>Number(x?.quantity??1)>0),knownGear=gear.filter(x=>['tool_kit','electrobinoculars','macrobinoculars'].includes(String(x?.id??x?.sourceCatalogId??'')));
   const skills=Object.keys(ch.skills||{}).sort((a,b)=>a.localeCompare(b,'pt-BR'));
   const enc=er.ok?er.data:null;const sceneId=String(st.scene?.id||st.scene?.locationId||'scene');
   b.innerHTML=`<h3>MEDO</h3><div class="grid"><label>Intensidade<select data-p101-fear-level><option value="minimal">Mínimo</option><option value="moderate" selected>Moderado</option><option value="very">Muito assustador</option><option value="mortal">Mortal</option><option value="utter">Terror absoluto</option></select></label><label><span>Preparado?</span><select data-p101-fear-prepared><option value="0">Não · Disciplina</option><option value="1">Sim · Frialdade</option></select></label></div><button data-p101-fear>RESOLVER TESTE DE MEDO</button>
   <h3>SOBRECARGA / CAPACIDADE DE CARGA</h3>${enc?`<div class="card r54-enc-card"><strong>${Number(enc.total||0)} / ${Number(enc.threshold||0)} Sobrecarga</strong><small>Excesso: ${Number(enc.over||0)} · Revés For/Ag: ${Number(enc.setbackOnBrawnAgility||0)}${enc.noFreeManeuver?' · sem manobra gratuita':''}</small></div>`:'<p class="warn">Estado de carga indisponível.</p>'}
   <div class="grid"><label>Carga do objeto<input data-p101-lift type="number" min="0" max="99" value="5"></label><label>Fortaleza do ajudante<input data-p101-helper type="number" min="0" max="7" value="0"></label></div><button data-p101-lift-check>CALCULAR LEVANTAMENTO</button>
   <h3>TESTE COM EQUIPAMENTO POSSUÍDO</h3>${knownGear.length?`<div class="grid"><label>Perícia<select data-p101-gear-skill>${skills.map(k=>`<option value="${esc(k)}">${esc(trSkill(k))}</option>`).join('')}</select></label><label>Equipamento<select data-p101-gear-item>${knownGear.map(g=>`<option value="${esc(String(g.id??g.sourceCatalogId))}">${esc(g.name||g.id)}</option>`).join('')}</select></label><label>Dificuldade<input data-p101-gear-diff type="number" min="0" max="5" value="2"></label><label>Dados de Revés<input data-p101-gear-setback type="number" min="0" max="8" value="1"></label><label>Por alcance longo<input data-p101-long type="number" min="0" max="8" value="0"></label><label>Por iluminação ruim<input data-p101-light type="number" min="0" max="8" value="0"></label><label>Por falta de ferramentas<input data-p101-tools type="number" min="0" max="8" value="0"></label></div><button data-p101-gear-check>ROLAR TESTE COM EQUIPAMENTO</button>`:'<p class="warn">Nenhum Kit de ferramentas, Eletrobinóculos ou Macrobinóculos possuído.</p>'}`;
   // P137: bypassing electronic/physical security is a contextual test, never
   // a blanket bonus to all Computers or Skulduggery actions.
   if((ch.talentPurchases||[]).some(x=>x.originalName==='ELUDIR SEGURIDAD')){
     const panel=document.createElement('div');panel.dataset.p137Bypass='1';
     panel.innerHTML='<h3>BURLAR SEGURANÇA · TALENTO AoR</h3><p>Remove um dado de Contratempo por nível apenas ao contornar sistemas de segurança ou fechaduras.</p><div class="grid"><label>Perícia<select data-p137-security-skill><option value="computadoras">Computadores</option><option value="actividad_criminal">Atividade criminosa</option></select></label><label>Dificuldade<input data-p137-security-diff type="number" min="1" max="5" value="2"></label><label>Contratempo<input data-p137-security-setback type="number" min="0" max="8" value="2"></label></div><button data-p137-security-check>RESOLVER INFILTRAÇÃO</button>';
     b.appendChild(panel);
     panel.querySelector('[data-p137-security-check]')?.addEventListener('click',()=>{
       const r=call('resolveSkillCheck',{skillKey:panel.querySelector('[data-p137-security-skill]').value,difficulty:num(panel,'[data-p137-security-diff]',2),setback:num(panel,'[data-p137-security-setback]'),securityBypass:true});
       report(p,r,r?.ok?`Burlar segurança: ${Number(r.data?.talentModifiers?.setbackRemoved||0)} Contratempo(s) removido(s).`:'');
     });
   }
   // P138: only expose contextual AoR task talents if actually purchased.
   // The estimated elapsed time is reported, never silently applied twice.
   const p138Names=new Set((ch.talentPurchases||[]).map(x=>x.originalName));
   if(['INVESTIGADOR','RASTREADOR EXPERTO','APTITUD TECNOLÓGICA'].some(x=>p138Names.has(x))){
     const panel=document.createElement('div');panel.dataset.p138Tasks='1';
     const options=[];
     if(p138Names.has('INVESTIGADOR'))options.push('<option value="educacion" data-context="research">Conhecimento / pesquisa</option>');
     if(p138Names.has('RASTREADOR EXPERTO'))options.push('<option value="supervivencia" data-context="tracking">Sobrevivência / rastreamento</option>');
     if(p138Names.has('APTITUD TECNOLÓGICA'))options.push('<option value="computadoras" data-context="computers">Computadores / tarefa técnica</option>');
     panel.innerHTML=`<h3>TAREFAS ESPECIALIZADAS · AoR</h3><p>Aplicar apenas ao contexto indicado. A duração é estimada após o teste; confirme separadamente para registrar minutos no mundo (sem avançar turnos).</p><div class="grid"><label>Atividade<select data-p138-task-skill>${options.join('')}</select></label><label>Duração-base (min)<input data-p138-task-time type="number" min="0" max="100000" value="60"></label><label>Dificuldade<input data-p138-task-diff type="number" min="0" max="5" value="2"></label><label>Contratempo<input data-p138-task-setback type="number" min="0" max="8" value="2"></label></div><button data-p138-task-check>RESOLVER TAREFA</button><button data-p139-apply-time hidden>CONFIRMAR TEMPO NO MUNDO</button><small data-p139-time-status>O relógio não será alterado automaticamente.</small>`;
     b.appendChild(panel);
     const pending=st.lastCheck;
     const recorded=st.world?.taskTimeApplications||{};
     const elapsed=Number(st.world?.clock?.elapsedMinutes||0);
     const taskStatus=panel.querySelector('[data-p139-time-status]');
     taskStatus.textContent=`Tempo confirmado no mundo: ${elapsed} min (turnos independentes).`;
     if(pending?.taskCheckId && Number.isFinite(pending?.taskTime?.durationMinutes) && !Object.prototype.hasOwnProperty.call(recorded,pending.taskCheckId)){
       const button=panel.querySelector('[data-p139-apply-time]');
       button.dataset.checkId=pending.taskCheckId;button.hidden=false;
       taskStatus.textContent=`Tarefa pendente de confirmação: ${Number(pending.taskTime.durationMinutes)} min. Total atual: ${elapsed} min.`;
     }
     panel.querySelector('[data-p138-task-check]')?.addEventListener('click',()=>{
       const sel=panel.querySelector('[data-p138-task-skill]');const skillKey=sel.value;
       const context=sel.selectedOptions[0]?.dataset.context;
       const r=call('resolveSkillCheck',{skillKey,difficulty:num(panel,'[data-p138-task-diff]',2),setback:num(panel,'[data-p138-task-setback]'),tracking:context==='tracking',research:context==='research',taskDurationMinutes:num(panel,'[data-p138-task-time]',60)});
       const confirm=panel.querySelector('[data-p139-apply-time]');
       confirm.hidden=true;confirm.dataset.checkId='';
       report(p,r,r?.ok?`Tarefa resolvida. Duração estimada: ${Number(r.data?.taskTime?.durationMinutes??0)} min. Contratempos removidos: ${Number(r.data?.talentModifiers?.setbackRemoved||0)}.`:'');
       if(r?.ok && r.data?.taskCheckId){
         confirm.dataset.checkId=r.data.taskCheckId;
         confirm.hidden=false;
         panel.querySelector('[data-p139-time-status]').textContent=`Aguardando confirmação: ${Number(r.data.taskTime.durationMinutes)} minuto(s).`;
       }
     });
     panel.querySelector('[data-p139-apply-time]')?.addEventListener('click',()=>{
       const button=panel.querySelector('[data-p139-apply-time]');
       const checkId=button.dataset.checkId;
       if(!checkId)return;
       const r=call('commitLastTaskTime',{checkId});
       report(p,r,r?.ok?`Tempo ${r.data.applied?'registrado':'já registrado'}: ${Number(r.data.elapsedMinutes)} minuto(s) acumulados. Nenhum turno avançado.`:'');
       if(r?.ok){button.hidden=true;panel.querySelector('[data-p139-time-status]').textContent=`Relógio de tarefas: ${Number(r.data.elapsedMinutes)} minuto(s) acumulados.`;}
     });
   }
   p.querySelector('[data-p101-fear]')?.addEventListener('click',()=>{const level=p.querySelector('[data-p101-fear-level]')?.value||'moderate',prepared=p.querySelector('[data-p101-fear-prepared]')?.value==='1',r=call('resolveFearCheck',{level,prepared,sourceId:`scene:${sceneId}:fear:${level}`});report(p,r,r?.data?.skipped?'Este medo já foi enfrentado nesta cena.':`Medo resolvido por ${r?.data?.skillKey||'perícia'}; efeitos persistidos.`)});
   p.querySelector('[data-p101-lift-check]')?.addEventListener('click',()=>{const helper=num(p,'[data-p101-helper]'),r=call('liftingDifficulty',num(p,'[data-p101-lift]'),{helperBrawn:helper>0?[helper]:[]});report(p,r,r?.ok?`Levantamento: ${r.data?.possible===false?'impossível sem mais ajuda':`dificuldade ${Number(r.data?.difficulty||0)}`}.`:'')});
   p.querySelector('[data-p101-gear-check]')?.addEventListener('click',()=>{const itemId=p.querySelector('[data-p101-gear-item]')?.value||'',skillKey=p.querySelector('[data-p101-gear-skill]')?.value||'',setback=num(p,'[data-p101-gear-setback]'),gearAssist=itemId==='tool_kit'?{itemId,missingToolsSetback:num(p,'[data-p101-tools]')}:{itemId,longRangeSetback:num(p,'[data-p101-long]'),poorLightingSetback:num(p,'[data-p101-light]')};const r=call('resolveSkillCheck',{skillKey,difficulty:num(p,'[data-p101-gear-diff]',2),setback,gearAssist});report(p,r,r?.ok?`Teste resolvido. Setback removido pelo item: ${Number(r.data?.gearModifier?.removedSetback||0)}.`:'')});
   return p
 }
 function mountRecovery(){
   const sr=call('readState'),p=base('REPARO DE EMERGÊNCIA PARA DROIDE','Remendo de reparo é uma ação real, consome um item possuído e usa o limite diário do Rules Engine. Não é um botão de cura direta.');const b=p.querySelector('[data-p101-body]');
   if(!sr.ok||!sr.data?.player?.character){b.innerHTML='<p class="warn">Personagem indisponível.</p>';return p}
   const ch=sr.data.player.character,isDroid=String(ch.creation?.speciesKey||ch.speciesKey||ch.species||'').toLocaleLowerCase('pt-BR').includes('droid')||String(ch.species||'').toLocaleLowerCase('pt-BR').includes('droide');
   const patches=(ch.gear||[]).filter(x=>String(x?.id??x?.sourceCatalogId??'')==='emergency_repair_patch').reduce((n,x)=>n+Number(x?.quantity??1),0);
   if(!isDroid){b.innerHTML='<p>Personagem orgânico: use Medicina/Injetor no painel de recuperação existente.</p>';return p}
   b.innerHTML=`<div class="card"><strong>${patches} remendo(s) disponível(is)</strong><small>Usos hoje: ${Number(ch.repairPatchUsesToday||0)}/5 · cura 3 Ferimentos por uso.</small></div><button data-p101-patch ${patches<=0?'disabled':''}>USAR REMENDO DE REPARO</button>`;
   p.querySelector('[data-p101-patch]')?.addEventListener('click',()=>{const r=call('useEmergencyRepairPatch');report(p,r,r?.ok?`Remendo aplicado: ${Number(r.data?.healed||0)} Ferimentos reparados; ${Number(r.data?.remaining||0)} restante(s).`:'');if(r?.ok)setTimeout(()=>{p.remove();mount()},80)});
   return p
 }
 function mount(){style();const r=route();document.querySelectorAll('.r54-tactical-collapsible').forEach(el=>el.remove());document.querySelectorAll('[data-p101-mechanics]').forEach(x=>{if(x.dataset.p101Mechanics!==r)x.remove()});if(!['00','02'].includes(r)||document.querySelector(`[data-p101-mechanics="${r}"]`))return;const p=r==='00'?mountSession():mountRecovery();if(p){if(r==='00'){const wrap=document.createElement('details');wrap.className='r54-tactical-collapsible';wrap.innerHTML='<summary>⚙️ REGRAS TÁTICAS · MEDO, CARGA E REPAROS</summary>';wrap.appendChild(p);host().appendChild(wrap);}else{host().appendChild(p);}}}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p101-fear-load-gear',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P102 — EOTE CORE VEHICLE CREW ACTIONS: Gain Advantage + crew checks/effects. */
(()=>{'use strict';
 if(window.__R54_P102_VEHICLE_ACTIONS__)return;window.__R54_P102_VEHICLE_ACTIONS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p102-vehicle-style'))return;const e=document.createElement('style');e.id='r54-p102-vehicle-style';e.textContent='.r54-p102-vehicle{margin-top:12px;padding:10px;border:1px solid #4d5331;border-radius:9px;background:#11140c}.r54-p102-vehicle h3{margin:0 0 5px;color:#dce6a8}.r54-p102-vehicle p,.r54-p102-vehicle small{color:#b8bea2}.r54-p102-vehicle .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p102-vehicle button,.r54-p102-vehicle select{min-height:38px;width:100%;border:1px solid #697046;border-radius:7px;background:#222713;color:#edf3cc;padding:7px;font-weight:750}.r54-p102-vehicle output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#0a0d08;color:#a9e8c0;white-space:pre-wrap}@media(max-width:430px){.r54-p102-vehicle .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mount(){
  style();if(route()!=='03'){document.querySelectorAll('[data-p102-vehicle]').forEach(x=>x.remove());return}
  const host=document.querySelector('[data-p92-mechanics="03"]');if(!host||host.querySelector('[data-p102-vehicle]'))return;
  const s=call('vehicleRuntimeStatus');if(!s?.ok||!s.data?.vehicles?.combat?.active)return;
  const box=document.createElement('div');box.className='r54-p102-vehicle';box.dataset.p102Vehicle='1';
  box.innerHTML='<small>P103 · EotE CORE · AÇÕES DE TRIPULAÇÃO</small><h3>AÇÕES VEICULARES</h3><p>Estas ações consomem a ação do assalto e rolam a perícia real da ficha. Gastos opcionais abaixo só são consumidos se os símbolos necessários realmente saírem.</p><div class="grid"><select data-p102-zone><option value="proa">Proa</option><option value="babor">Babor</option><option value="estribor">Estribor</option><option value="popa">Popa</option></select><button data-p102-gain>GANHAR A VANTAGEM</button><button data-p102-act="copilot">COPILOTO</button><button data-p102-act="plot_course">TRAÇAR CURSO</button><button data-p102-act="boost_shields">REFORÇAR ESCUDOS</button><button data-p102-act="fire_discipline">DISCIPLINA DE FOGO</button><button data-p102-act="scan_enemy">ESCANEAR INIMIGO</button><button data-p102-act="slice_enemy">FATIAR SISTEMAS</button><button data-p102-act="spoof_missiles">DESPISTAR MÍSSEIS</button><button data-p102-act="manual_repairs">REPARO MANUAL</button></div><div class="card"><label><input data-p103-spend-adv type="checkbox"> Gastar Vantagens opcionais se houver</label><label><input data-p103-spend-tri type="checkbox"> Gastar Triunfo para desativar arma ao Fatiar</label><label><input data-p103-gm-allow type="checkbox"> Contexto permite Reparo Manual no casco</label><label>Arma-alvo no Fatiar <input data-p103-weapon type="number" min="0" value="0"></label></div><output>Pronto.</output>';
  const out=box.querySelector('output'),zone=()=>box.querySelector('[data-p102-zone]')?.value||'proa';
  const report=r=>{if(!r?.ok){out.textContent=String(r?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}const d=r.data||{};out.style.color='#a9e8c0';if(d.kind==='scan_enemy'&&d.effect)out.textContent=`Escaneamento concluído: casco ${d.effect.hullThreshold}, sistemas ${d.effect.systemStrainThreshold}, armas ${d.effect.weapons?.map(w=>w.name).join(', ')||'nenhuma'}.`;else out.textContent=d.success===false?'Teste falhou; a ação foi consumida.':'Teste bem-sucedido; efeito aplicado e persistido.'};
  box.querySelector('[data-p102-gain]')?.addEventListener('click',e=>{e.currentTarget.disabled=true;const r=call('gainVehicleAdvantage',{zone:zone()});report(r);e.currentTarget.disabled=false});
  box.querySelectorAll('[data-p102-act]').forEach(btn=>btn.addEventListener('click',e=>{e.currentTarget.disabled=true;const kind=e.currentTarget.dataset.p102Act;const r=call('useVehicleCrewAction',kind,{zone:zone(),spendAdvantage:Boolean(box.querySelector('[data-p103-spend-adv]')?.checked),spendTriumph:Boolean(box.querySelector('[data-p103-spend-tri]')?.checked),gmAllowed:Boolean(box.querySelector('[data-p103-gm-allow]')?.checked),weaponIndex:Number(box.querySelector('[data-p103-weapon]')?.value||0)});report(r);e.currentTarget.disabled=false}));
  host.querySelector('[data-p92-body]')?.appendChild(box);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p102-vehicle-actions',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P104 — UNIFIED PERSONAL MANEUVER COSTS / ENCUMBRANCE + CRITICAL PAYMENT + PRONE / TARGETED AIM. */

/* R54 P105 — EOTE CORE FORCE ACTIONS: Sense ongoing + Influence basic. */
(()=>{'use strict';
 if(window.__R54_P105_FORCE_ACTIONS__)return;window.__R54_P105_FORCE_ACTIONS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p105-force-style'))return;const e=document.createElement('style');e.id='r54-p105-force-style';e.textContent='.r54-p105-force{margin-top:12px;padding:11px;border:1px solid #564a78;border-radius:9px;background:#11101a;color:#eeeaf8}.r54-p105-force h3{margin:0 0 5px;color:#d7c7ff}.r54-p105-force p,.r54-p105-force small{color:#aaa2bd}.r54-p105-force .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p105-force button,.r54-p105-force select,.r54-p105-force input{width:100%;min-height:38px;border:1px solid #66598a;border-radius:7px;background:#201d2d;color:#f3edff;padding:7px;font-weight:750}.r54-p105-force output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#0b0a11;color:#afe2c0;white-space:pre-wrap}@media(max-width:430px){.r54-p105-force .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mount(){
  style();if(route()!=='02'){document.querySelectorAll('[data-p105-force]').forEach(x=>x.remove());return}
  const host=document.querySelector('[data-p94-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p105-force]'))return;
  const fs=call('forceStatus'),cs=call('combatRuntimeStatus');if(!fs?.ok||!fs.data?.sensitive)return;
  const f=fs.data.force||{},powers=fs.data.powers||{},ongoing=Array.isArray(f.ongoing)?f.ongoing:[],combat=cs?.ok?cs.data?.combat||{}:{},adv=Array.isArray(combat.adversaries)?combat.adversaries:[];
  const box=document.createElement('div');box.className='r54-p105-force';box.dataset.p105Force='1';
  const sense=powers.sense||{},influence=powers.influence||{};const defOn=ongoing.some(x=>x.id==='sense-defense'),offOn=ongoing.some(x=>x.id==='sense-offense');
  box.innerHTML=`<small>P105 · EotE CORE · AÇÕES DA FORÇA</small><h3>FORÇA</h3><p>Ativar um poder consome a ação do turno, salvo regra específica. O painel só expõe poderes e aprimoramentos realmente adquiridos.</p><div class="grid"><div><strong>Force Rating ${Number(f.rating||0)}</strong><small>Disponível ${Number(fs.data.availableRating||0)} · comprometido ${Number(f.committed||0)}</small></div><div><strong>Ação ${combat.actionUsed||combat.playerTurn?.actionUsed?'usada':'disponível'}</strong><small>${combat.active?'Encontro ativo':'Fora de encontro'}</small></div></div>${sense.purchased?`<h3>SENTIR</h3><div class="grid">${sense.defense?`<button data-p105-sense="defense" ${defOn?'disabled':''}>${defOn?'DEFESA ATIVA':'ATIVAR DEFESA'}</button>`:''}${sense.offense?`<button data-p105-sense="offense" ${offOn?'disabled':''}>${offOn?'OFENSIVA ATIVA':'ATIVAR OFENSIVA'}</button>`:''}</div><small>Efeitos contínuos comprometem 1 dado da Força e são aplicados automaticamente quando o gatilho válido ocorre.</small>`:''}${influence.purchased?`<h3>INFLUENCIAR · EFEITO BÁSICO</h3>${combat.active&&adv.length?`<div class="grid"><label>Alvo<select data-p105-influence-target>${adv.map(a=>`<option value="${esc(a.id)}">${esc(a.name)} · ${esc(a.range||'?')}</option>`).join('')}</select></label><label>Pontos no efeito<input data-p105-influence-activations type="number" min="1" max="5" value="1"></label><label>Ativações de Alcance<input data-p105-influence-range type="number" min="0" max="5" value="0"></label><label>Ativações de Magnitude<input data-p105-influence-mag type="number" min="0" max="5" value="0"></label></div><button data-p105-influence>USAR INFLUÊNCIA</button><small>Resultados sombrios não são usados automaticamente. Este botão usa somente pontos nativos da rolagem.</small>`:'<p>Nenhum alvo persistente de encontro disponível.</p>'}`:''}<output>Pronto.</output>`;
  const out=box.querySelector('output'),report=r=>{if(!r?.ok){out.textContent=String(r?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}const d=r.data||{};out.style.color='#afe2c0';if(d.success===false)out.textContent=`Ação consumida, mas os ${Number(d.points||0)} ponto(s) da Força não cobriram o custo ${Number(d.forcePointCost||0)}.`;else if(d.affected)out.textContent=`Influência aplicada: ${d.affected.map(x=>`${x.name}: ${x.strain} Tensão`).join(' · ')}`;else out.textContent='Efeito da Força ativado e persistido.';setTimeout(()=>{box.remove();mount()},90)};
  box.querySelectorAll('[data-p105-sense]').forEach(btn=>btn.addEventListener('click',()=>report(call('activateSenseOngoing',{mode:btn.dataset.p105Sense}))));
  box.querySelector('[data-p105-influence]')?.addEventListener('click',()=>report(call('activateInfluenceBasic',{targetAdversaryIds:[box.querySelector('[data-p105-influence-target]')?.value||''],activations:Number(box.querySelector('[data-p105-influence-activations]')?.value||1),rangeActivations:Number(box.querySelector('[data-p105-influence-range]')?.value||0),magnitudeActivations:Number(box.querySelector('[data-p105-influence-mag]')?.value||0),useOppositePoints:0})));
  host.appendChild(box)
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p105-force-actions',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P106 — EOTE CORE ACTIVE TALENTS: Intense Focus / Sense Danger / Touch of Fate / Jump Up / Balance. */
/* R54 P110 — skill masters/intimidating extension on the same deterministic panel. */
(()=>{'use strict';
 if(window.__R54_P106_ACTIVE_TALENTS__)return;window.__R54_P106_ACTIVE_TALENTS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p106-talents-style'))return;const e=document.createElement('style');e.id='r54-p106-talents-style';e.textContent='.r54-p106-talents{margin:12px 14px 112px;padding:12px;border:1px solid #6a5937;border-radius:10px;background:#15120b;color:#f2eee5}.r54-p106-talents h3{margin:2px 0 7px;color:#f0cf8b}.r54-p106-talents small,.r54-p106-talents p{color:#aea58f}.r54-p106-talents .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p106-talents label{display:block;font-weight:750}.r54-p106-talents select,.r54-p106-talents input,.r54-p106-talents button{width:100%;min-height:39px;border:1px solid #735f37;border-radius:7px;background:#241c0d;color:#f5db9c;padding:7px;font-weight:750}.r54-p106-talents .checks{display:grid;gap:7px;margin:8px 0}.r54-p106-talents .checks label{display:flex;align-items:center;gap:8px;padding:7px;border:1px solid #3f3522;border-radius:7px}.r54-p106-talents .checks input{width:auto;min-height:auto}.r54-p106-talents output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#0b0c09;color:#afe2c0;white-space:pre-wrap}@media(max-width:430px){.r54-p106-talents .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mountSkillTalents(){
  const host=document.querySelector('[data-p95-mechanics="01"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p106-talents="skill"]'))return;
  const sr=call('readState'),cr=call('creationCatalog'),tr=call('talentActionStatus');if(!sr?.ok||!sr.data?.player?.character||!tr?.ok)return;
  const ch=sr.data.player.character,labels=cr?.data?.skillLabels||{},skills=Object.keys(ch.skills||{}).sort((a,b)=>String(labels[a]||a).localeCompare(String(labels[b]||b),'pt-BR')),t=tr.data||{};
  const box=document.createElement('section');box.className='r54-p106-talents';box.dataset.p106Talents='skill';
  box.innerHTML=`<small>P106 · EotE CORE · TALENTOS ATIVOS</small><h3>TESTE DE PERÍCIA + TALENTOS</h3><p>O motor aplica custo, limite por sessão e modificação do pool. Nenhum talento é concedido se não estiver adquirido.</p><div class="grid"><label>Perícia<select data-p106-skill>${skills.map(k=>`<option value="${esc(k)}">${esc(labels[k]||k.replaceAll('_',' '))}</option>`).join('')}</select></label><label>Dificuldade<input data-p106-diff type="number" min="0" max="5" value="2"></label><label>Boost base<input data-p106-boost type="number" min="0" max="10" value="0"></label><label>Setback base<input data-p106-setback type="number" min="0" max="10" value="0"></label><label>Pagamento de manobra<select data-p106-method><option value="normal">Manobra normal</option><option value="strain">2 de Tensão</option><option value="action">Trocar a ação</option></select></label></div><div class="checks">${t.intenseFocus?.available?'<label><input type="checkbox" data-p106-focus> Foco Intenso · manobra + 1 Tensão · +1 upgrade</label>':''}${t.senseDanger?.available?`<label><input type="checkbox" data-p106-danger ${t.senseDanger.usedThisSession?'disabled':''}> Sentir o Perigo · remove 2 Setbacks · ${t.senseDanger.usedThisSession?'JÁ USADO':'1×/sessão'}</label>`:''}${t.touchOfFate?.available?`<label><input type="checkbox" data-p106-fate ${t.touchOfFate.usedThisSession?'disabled':''}> Toque do Destino · +2 Boost · ${t.touchOfFate.usedThisSession?'JÁ USADO':'1×/sessão'}</label>`:''}${t.masterOfShadows?.available?'<label><input type="checkbox" data-p110-shadows> Mestre das Sombras / Sombra furtiva · 2 Tensão · -1 dificuldade em Sigilo/Atividade Criminal</label>':''}${t.masterDoctor?.available?'<label><input type="checkbox" data-p110-doctor> Médico Mestre · 2 Tensão · -1 dificuldade em Medicina</label>':''}${t.masterSlicer?.available?'<label><input type="checkbox" data-p110-slicer> Slicer Mestre · 2 Tensão · -1 dificuldade em Computadores</label>':''}${t.intimidating?.available?`<label>Intimidador · Tensão para downgrade de Coerção<input data-p110-intim type="number" min="0" max="${Number(t.intimidating.rank||0)}" value="0"></label>`:''}</div><label style="display:flex;gap:6px;align-items:center"><input type="checkbox" data-p111-natural> <span data-p159-natural-label>Rerrolagem Nato: verificando perícia…</span></label><button data-p106-roll>RESOLVER TESTE</button><output>Pronto.</output>`;
  const refreshNatural=()=>{const skillKey=box.querySelector('[data-p106-skill]')?.value;const status=call('naturalRerollStatus',{skillKey});const info=status?.ok?status.data:null;const check=box.querySelector('[data-p111-natural]');const label=box.querySelector('[data-p159-natural-label]');if(!check||!label)return;const enabled=Boolean(info?.available&&info?.sessionActive&&!info?.usedThisSession);check.disabled=!enabled;if(!enabled)check.checked=false;label.textContent=info?.available?`${info.label} · ${info.usedThisSession?'JÁ USADO':info.sessionActive?'1× por sessão':'inicie a sessão'}`:'Nenhuma rerrolagem Nato disponível nesta perícia';};
  box.querySelector('[data-p106-skill]')?.addEventListener('change',refreshNatural);refreshNatural();
  const out=box.querySelector('output');box.querySelector('[data-p106-roll]')?.addEventListener('click',()=>{const r=call('resolveSkillCheck',{skillKey:box.querySelector('[data-p106-skill]')?.value,difficulty:Number(box.querySelector('[data-p106-diff]')?.value||0),boost:Number(box.querySelector('[data-p106-boost]')?.value||0),setback:Number(box.querySelector('[data-p106-setback]')?.value||0),useIntenseFocus:Boolean(box.querySelector('[data-p106-focus]')?.checked),intenseFocusManeuverMethod:box.querySelector('[data-p106-method]')?.value||'normal',useSenseDanger:Boolean(box.querySelector('[data-p106-danger]')?.checked),useTouchOfFate:Boolean(box.querySelector('[data-p106-fate]')?.checked),useMasterOfShadows:Boolean(box.querySelector('[data-p110-shadows]')?.checked),useMasterDoctor:Boolean(box.querySelector('[data-p110-doctor]')?.checked),useMasterSlicer:Boolean(box.querySelector('[data-p110-slicer]')?.checked),intimidatingStrain:Number(box.querySelector('[data-p110-intim]')?.value||0),useNaturalReroll:Boolean(box.querySelector('[data-p111-natural]')?.checked)});if(!r?.ok){out.textContent=String(r?.message||'Teste rejeitado.');out.style.color='#ff9d9d';return}const d=r.data||{};out.style.color='#afe2c0';out.textContent=`Sucesso líquido ${Number(d.netSuccess||0)} · Vantagem ${Number(d.netAdvantage||0)} · Boost ${Number(d.pool?.boost||0)} · Setback ${Number(d.pool?.setback||0)}`;setTimeout(()=>{box.remove();mount()},100)});
  host.appendChild(box)
 }
 function mountCombatTalents(){
  const host=document.querySelector('[data-p94-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p106-talents="combat"]'))return;
  const tr=call('talentActionStatus'),cs=call('combatRuntimeStatus');if(!tr?.ok||!cs?.ok)return;const t=tr.data||{},st=cs.data?.status||{},combat=cs.data?.combat||{};if(!combat.active)return;
  if(!(t.jumpUp?.available&&st.prone)&&!t.balance?.available)return;
  const box=document.createElement('section');box.className='r54-p106-talents';box.dataset.p106Talents='combat';box.innerHTML=`<small>P106 · TALENTOS DE ENCONTRO</small><h3>AÇÕES CONTEXTUAIS</h3>${t.jumpUp?.available&&st.prone?`<button data-p106-jump ${t.jumpUp.usedThisTurn?'disabled':''}>LEVANTAR RÁPIDO · INCIDENTAL</button>`:''}${t.balance?.available?'<button data-p106-balance>ENCERRAR COMBATE + EQUILÍBRIO</button><small>Equilíbrio acrescenta a rolagem de dados da Força à recuperação de Tensão do fim do encontro.</small>':''}<output>Pronto.</output>`;
  const out=box.querySelector('output'),report=r=>{if(!r?.ok){out.textContent=String(r?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}out.style.color='#afe2c0';out.textContent='Talento aplicado e persistido.';setTimeout(()=>{box.remove();mount()},100)};
  box.querySelector('[data-p106-jump]')?.addEventListener('click',()=>report(call('standUpFromProne',{useJumpUp:true})));
  box.querySelector('[data-p106-balance]')?.addEventListener('click',()=>report(window.BraseiroPublishedActionClient.finishCombat({useBalance:true})));
  host.appendChild(box)
 }
 function mount(){style();document.querySelectorAll('[data-p106-talents]').forEach(x=>{if((route()==='01'&&x.dataset.p106Talents!=='skill')||(route()==='02'&&x.dataset.p106Talents!=='combat')||!['01','02'].includes(route()))x.remove()});if(route()==='01')mountSkillTalents();if(route()==='02')mountCombatTalents()}
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p106-active-talents',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P107 — EOTE CORE COMBAT TALENTS: Frenzied Attack / Defensive Stance / Side Step / Second Wind / Quick Draw + passive combat talents. */
(()=>{'use strict';
 if(window.__R54_P107_COMBAT_TALENTS__)return;window.__R54_P107_COMBAT_TALENTS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p107-talents-style'))return;const e=document.createElement('style');e.id='r54-p107-talents-style';e.textContent='.r54-p107-talents{margin:12px 14px 112px;padding:12px;border:1px solid #6a5937;border-radius:10px;background:#12100a;color:#f2eee5}.r54-p107-talents h3{margin:2px 0 7px;color:#f0cf8b}.r54-p107-talents small,.r54-p107-talents p{color:#aea58f}.r54-p107-talents .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p107-talents .card{padding:8px;border:1px solid #3f3522;border-radius:8px;background:#18140d}.r54-p107-talents select,.r54-p107-talents input,.r54-p107-talents button{width:100%;min-height:39px;border:1px solid #735f37;border-radius:7px;background:#241c0d;color:#f5db9c;padding:7px;font-weight:750}.r54-p107-talents button[disabled]{opacity:.45}.r54-p107-talents output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#0b0c09;color:#afe2c0;white-space:pre-wrap}@media(max-width:430px){.r54-p107-talents .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mount(){
  document.querySelectorAll('[data-p107-talents]').forEach(x=>{if(route()!=='02')x.remove()});if(route()!=='02')return;style();
  const host=document.querySelector('[data-p94-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p107-talents]'))return;
  const tr=call('talentActionStatus'),cs=call('combatRuntimeStatus');if(!tr?.ok||!cs?.ok||!cs.data?.combat?.active)return;
  const t=tr.data||{},ch=cs.data?.character||{},weapons=(ch.weapons||[]);const actionable=Boolean(t.frenziedAttack?.available||t.defensiveStance?.available||t.sideStep?.available||t.secondWind?.available||t.quickDraw?.available||Number(t.lethalBlows?.rank||0)>0);if(!actionable)return;
  const method='<option value="normal">Manobra normal</option><option value="strain">Pagar 2 de Tensão</option><option value="action">Trocar a ação</option>';
  const box=document.createElement('section');box.className='r54-p107-talents';box.dataset.p107Talents='combat';
  box.innerHTML=`<small>P107 · EotE CORE · TALENTOS DE COMBATE</small><h3>EFEITOS EXECUTÁVEIS</h3><p>Custos, limites por turno/encontro, upgrades e dano são aplicados pelo Rules Engine. Efeitos passivos entram automaticamente no ataque/crítico.</p><div class="grid">${t.frenziedAttack?.available?`<div class="card"><strong>ATAQUE FRENÉTICO</strong><small>Incidental · Tensão ≤ rank (${Number(t.frenziedAttack.rank||0)}) · aprimora Briga/Corpo a Corpo.</small><input data-p107-frenzy type="number" min="1" max="${Number(t.frenziedAttack.rank||1)}" value="1"><button data-p107-frenzy-btn ${t.frenziedAttack.pending?'disabled':''}>${t.frenziedAttack.pending?'PREPARADO':'PREPARAR PRÓXIMO ATAQUE'}</button></div>`:''}${t.sideStep?.available?`<div class="card"><strong>PASSO LATERAL</strong><small>1×/rodada · ranged contra você recebe upgrades.</small><input data-p107-side-strain type="number" min="1" max="${Number(t.sideStep.rank||1)}" value="1"><select data-p107-side-method>${method}</select><button data-p107-side ${t.sideStep.active?'disabled':''}>${t.sideStep.active?'ATIVO':'ATIVAR'}</button></div>`:''}${t.defensiveStance?.available?`<div class="card"><strong>POSTURA DEFENSIVA</strong><small>1×/rodada · melee contra você recebe upgrades.</small><input data-p107-def-strain type="number" min="1" max="${Number(t.defensiveStance.rank||1)}" value="1"><select data-p107-def-method>${method}</select><button data-p107-def ${t.defensiveStance.active?'disabled':''}>${t.defensiveStance.active?'ATIVA':'ATIVAR'}</button></div>`:''}${t.secondWind?.available?`<div class="card"><strong>SEGUNDO FÔLEGO</strong><small>1×/encontro · recupera ${Number(t.secondWind.rank||0)} de Tensão.</small><button data-p107-wind ${t.secondWind.usedThisEncounter?'disabled':''}>${t.secondWind.usedThisEncounter?'JÁ USADO':'RECUPERAR TENSÃO'}</button></div>`:''}${t.quickDraw?.available&&weapons.length?`<div class="card"><strong>SAQUE RÁPIDO</strong><small>1×/rodada · sacar/guardar como incidental.</small><select data-p107-weapon>${weapons.map((w,i)=>`<option value="${esc(w.id??w.name??i)}">${esc(w.name||`Arma ${i+1}`)}</option>`).join('')}</select><button data-p107-draw ${t.quickDraw.usedThisTurn?'disabled':''}>${t.quickDraw.usedThisTurn?'JÁ USADO':'SACAR COMO INCIDENTAL'}</button></div>`:''}${Number(t.lethalBlows?.rank||0)>0?`<div class="card"><strong>GOLPES LETAIS</strong><small>Passivo · +${Number(t.lethalBlows.modifier||0)} em rolagens de Ferimento Crítico infligidas.</small></div>`:''}</div><output>Pronto.</output>`;
  const out=box.querySelector('output'),report=r=>{if(!r?.ok){out.textContent=String(r?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}out.style.color='#afe2c0';out.textContent='Talento aplicado e persistido no estado do encontro.';setTimeout(()=>{box.remove();mount()},90)};
  box.querySelector('[data-p107-frenzy-btn]')?.addEventListener('click',()=>report(call('activateFrenziedAttack',{strain:Number(box.querySelector('[data-p107-frenzy]')?.value||1)})));
  box.querySelector('[data-p107-side]')?.addEventListener('click',()=>report(call('useSideStep',{strain:Number(box.querySelector('[data-p107-side-strain]')?.value||1),maneuverMethod:box.querySelector('[data-p107-side-method]')?.value||'normal'})));
  box.querySelector('[data-p107-def]')?.addEventListener('click',()=>report(call('useDefensiveStanceTalent',{strain:Number(box.querySelector('[data-p107-def-strain]')?.value||1),maneuverMethod:box.querySelector('[data-p107-def-method]')?.value||'normal'})));
  box.querySelector('[data-p107-wind]')?.addEventListener('click',()=>report(call('useSecondWind')));
  box.querySelector('[data-p107-draw]')?.addEventListener('click',()=>report(call('drawWeaponInCombat',box.querySelector('[data-p107-weapon]')?.value||'',{useQuickDraw:true})));
  host.appendChild(box)
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p107-combat-talents',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();


/* R54 P108 — ASSASSIN / COMBAT TALENTS: Dodge, Precise Aim, Sniper Shot, Targeted Blow, Anatomy Lessons, Deadly Accuracy. */
(()=>{'use strict';
 if(window.__R54_P108_ASSASSIN_TALENTS__)return;window.__R54_P108_ASSASSIN_TALENTS__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p108-style'))return;const e=document.createElement('style');e.id='r54-p108-style';e.textContent='.r54-p108{margin:12px 14px 118px;padding:12px;border:1px solid #74613c;border-radius:10px;background:#100e09;color:#f4efe5}.r54-p108 h3{margin:2px 0 8px;color:#e6c47d}.r54-p108 .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p108 .card{padding:8px;border:1px solid #3d3320;border-radius:8px;background:#18140d}.r54-p108 small{display:block;color:#aaa08b;margin:3px 0 7px}.r54-p108 input,.r54-p108 select,.r54-p108 button{width:100%;min-height:39px;border:1px solid #735f37;border-radius:7px;background:#241c0d;color:#f5db9c;padding:7px;font-weight:750}.r54-p108 button[disabled]{opacity:.45}.r54-p108 output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#0b0c09;color:#afe2c0;white-space:pre-wrap}@media(max-width:430px){.r54-p108 .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mount(){
  document.querySelectorAll('[data-p108-talents]').forEach(x=>{if(route()!=='02')x.remove()});if(route()!=='02')return;style();
  const host=document.querySelector('[data-p94-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p108-talents]'))return;
  const tr=call('talentActionStatus'),cs=call('combatRuntimeStatus');if(!tr?.ok||!cs?.ok||!cs.data?.combat?.active)return;const t=tr.data||{};
  const actionable=Boolean(t.dodge?.available||t.preciseAim?.available||t.sniperShot?.available||t.targetedBlow?.available||t.anatomyLessons?.available||t.deadlyAccuracy?.available);if(!actionable)return;
  const method='<option value="normal">Manobra normal</option><option value="strain">Pagar 2 de Tensão</option><option value="action">Trocar a ação</option>';
  const box=document.createElement('section');box.className='r54-p108';box.dataset.p108Talents='combat';
  box.innerHTML=`<small>P108 · EotE CORE · BLOCO ASSASSINO/COMBATE</small><h3>TALENTOS ATIVOS E PASSIVOS</h3><div class="grid">${t.dodge?.available?`<div class="card"><strong>ESQUIVA</strong><small>Prepara a reação para o próximo ataque recebido. Sofre Tensão ≤ rank (${Number(t.dodge.rank||0)}).</small><input data-p108-dodge-n type="number" min="1" max="${Number(t.dodge.rank||1)}" value="1"><button data-p108-dodge ${t.dodge.prepared?'disabled':''}>${t.dodge.prepared?'REAÇÃO PREPARADA':'PREPARAR ESQUIVA'}</button></div>`:''}${t.preciseAim?.available?`<div class="card"><strong>MIRA PRECISA</strong><small>Manobra · Tensão ≤ rank (${Number(t.preciseAim.rank||0)}) · reduz defesa melee/ranged do próximo alvo.</small><input data-p108-precise-n type="number" min="1" max="${Number(t.preciseAim.rank||1)}" value="1"><select data-p108-precise-method>${method}</select><button data-p108-precise ${t.preciseAim.pending?'disabled':''}>${t.preciseAim.pending?'PREPARADA':'USAR MIRA PRECISA'}</button></div>`:''}${t.sniperShot?.available?`<div class="card"><strong>TIRO DE PRECISÃO</strong><small>Manobra · aumenta alcance em até ${Number(t.sniperShot.rank||0)} banda(s) e aprimora dificuldade pelo mesmo valor.</small><input data-p108-sniper-n type="number" min="1" max="${Number(t.sniperShot.rank||1)}" value="1"><select data-p108-sniper-method>${method}</select><button data-p108-sniper ${t.sniperShot.pending?'disabled':''}>${t.sniperShot.pending?'PREPARADO':'PREPARAR TIRO'}</button></div>`:''}${t.targetedBlow?.available?`<div class="card"><strong>GOLPE DIRECIONADO</strong><small>Após acerto, gasta 1 Destiny luminoso e soma Agilidade ao dano de um hit.</small><button data-p108-targeted ${t.targetedBlow.pending?'disabled':''}>${t.targetedBlow.pending?'PREPARADO':'PREPARAR'}</button></div>`:''}${t.anatomyLessons?.available?`<div class="card"><strong>ANATOMIA</strong><small>Após acerto, gasta 1 Destiny luminoso e soma Intelecto ao dano de um hit.</small><button data-p108-anatomy ${t.anatomyLessons.pending?'disabled':''}>${t.anatomyLessons.pending?'PREPARADA':'PREPARAR'}</button></div>`:''}${t.deadlyAccuracy?.available?`<div class="card"><strong>PRECISÃO LETAL</strong><small>Passivo · ${String(t.deadlyAccuracy.skillKey||'perícia escolhida')} · +${Number(t.deadlyAccuracy.bonus||0)} dano em um hit bem-sucedido.</small></div>`:''}</div><output>Pronto.</output>`;
  const out=box.querySelector('output'),report=r=>{if(!r?.ok){out.textContent=String(r?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}out.style.color='#afe2c0';out.textContent='Talento preparado/aplicado no estado real do combate.';setTimeout(()=>{box.remove();mount()},90)};
  box.querySelector('[data-p108-dodge]')?.addEventListener('click',()=>report(call('prepareDodge',{strain:Number(box.querySelector('[data-p108-dodge-n]')?.value||1)})));
  box.querySelector('[data-p108-precise]')?.addEventListener('click',()=>report(call('usePreciseAim',{strain:Number(box.querySelector('[data-p108-precise-n]')?.value||1),maneuverMethod:box.querySelector('[data-p108-precise-method]')?.value||'normal'})));
  box.querySelector('[data-p108-sniper]')?.addEventListener('click',()=>report(call('useSniperShot',{bands:Number(box.querySelector('[data-p108-sniper-n]')?.value||1),maneuverMethod:box.querySelector('[data-p108-sniper-method]')?.value||'normal'})));
  box.querySelector('[data-p108-targeted]')?.addEventListener('click',()=>report(call('prepareTargetedBlow')));
  box.querySelector('[data-p108-anatomy]')?.addEventListener('click',()=>report(call('prepareAnatomyLessons')));
  host.appendChild(box)
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p108-assassin-talents',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P113 — ENVIRONMENTAL HAZARDS: terrain, gravity, water, vacuum, fire/acid/corrosive, suffocation and falls. */
(()=>{'use strict';
 if(window.__R54_P113_ENVIRONMENT__)return;window.__R54_P113_ENVIRONMENT__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const call=(name,...args)=>{const b=window.BraseiroGameBridge;if(!b||typeof b[name]!=='function')return {ok:false,message:`Mecânica ${name} indisponível no bridge.`};try{return b[name](...args)||{ok:false,message:'Resposta vazia.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function style(){if(document.getElementById('r54-p113-env-style'))return;const e=document.createElement('style');e.id='r54-p113-env-style';e.textContent='.r54-p113-env{margin:12px 14px 118px;padding:12px;border:1px solid #55706f;border-radius:10px;background:#0d1514;color:#edf6f4}.r54-p113-env h3{margin:2px 0 8px;color:#96d4cf}.r54-p113-env .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.r54-p113-env .card{padding:8px;border:1px solid #294442;border-radius:8px;background:#111e1c}.r54-p113-env small{display:block;color:#9db2ae;margin:3px 0 7px}.r54-p113-env input,.r54-p113-env select,.r54-p113-env button{width:100%;min-height:39px;border:1px solid #416e69;border-radius:7px;background:#132d29;color:#d9fff9;padding:7px;font-weight:750}.r54-p113-env label.check{display:flex;align-items:center;gap:7px}.r54-p113-env label.check input{width:auto;min-height:auto}.r54-p113-env output{display:block;margin-top:8px;padding:8px;border-radius:7px;background:#07100f;color:#afe2c0;white-space:pre-wrap}@media(max-width:430px){.r54-p113-env .grid{grid-template-columns:1fr}}';document.head.appendChild(e)}
 function mount(){
  const r=route();document.querySelectorAll('[data-p113-environment]').forEach(x=>{if(!['01','02'].includes(r))x.remove()});if(!['01','02'].includes(r))return;style();
  const host=document.querySelector(r==='01'?'[data-p95-mechanics="01"]':'[data-p94-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;if(host.querySelector('[data-p113-environment]'))return;
  const sr=call('environmentStatus');if(!sr?.ok)return;const d=sr.data||{},env=d.environment||{},g=env.gravity||{},w=env.water||{},h=env.hazards||{};
  const box=document.createElement('section');box.className='r54-p113-env';box.dataset.p113Environment=r;
  box.innerHTML=`<small>P113 · EotE CORE · PERIGOS AMBIENTAIS · SEM RAG</small><h3>AMBIENTE MECÂNICO</h3><div class="grid"><label>Terreno<select data-p113-terrain><option value="normal">Normal</option><option value="difficult">Difícil</option><option value="impassable">Intransponível</option></select></label><label>Gravidade<select data-p113-gravity><option value="normal">Normal</option><option value="heavy">Alta</option><option value="light">Baixa</option><option value="zero">Zero-G</option></select></label><label>Nível gravidade (1–3)<input data-p113-gravity-level type="number" min="1" max="3" value="${Number(g.level||1)}"></label><label>Ocultação (0–3)<input data-p113-conceal type="number" min="0" max="3" value="${Number(env.concealment||0)}"></label><label class="check"><input data-p113-water type="checkbox" ${w.present?'checked':''}> Água / natação</label><label class="check"><input data-p113-submerged type="checkbox" ${w.submerged?'checked':''}> Submerso</label><label class="check"><input data-p113-vacuum type="checkbox" ${env.vacuum?'checked':''}> Vácuo</label><label class="check"><input data-p113-suffocating type="checkbox" ${env.suffocating?'checked':''}> Sufocando diretamente</label><label>Fogo · rating<input data-p113-fire type="number" min="0" max="20" value="${Number(h.fire||0)}"></label><label>Ácido · rating<input data-p113-acid type="number" min="0" max="20" value="${Number(h.acid||0)}"></label><label>Atmosfera corrosiva · rating<input data-p113-corrosive type="number" min="0" max="20" value="${Number(h.corrosive||0)}"></label></div><div class="grid"><button data-p113-apply>APLICAR AMBIENTE</button><button data-p113-clear>LIMPAR AMBIENTE</button></div><div class="card"><strong>ESTADO</strong><small data-p113-status>Respiração ${Number(env.exposure?.breathRounds||0)} rodada(s) · movimento ×${Number(d.movement?.multiplier||1)}${d.movement?.blocked?' · INTRANSPONÍVEL BLOQUEADO':''}</small></div><div class="grid"><label>Superar intransponível<select data-p113-imp-skill><option value="atletismo">Atletismo</option><option value="coordinacion">Coordenação</option></select></label><label>Dificuldade<input data-p113-imp-diff type="number" min="1" max="5" value="2"></label><button data-p113-imp>TESTAR PASSAGEM</button><span></span><label>Queda<select data-p113-fall-range><option value="Curto">Curta</option><option value="Médio">Média</option><option value="Longo">Longa</option><option value="Extremo">Extrema</option></select></label><label>Mitigar com<select data-p113-fall-skill><option value="atletismo">Atletismo</option><option value="coordinacion">Coordenação</option></select></label><label class="check"><input data-p113-fall-triumph type="checkbox"> GM permite Triunfo reduzir 1 faixa</label><button data-p113-fall>RESOLVER QUEDA</button></div><output>Pronto.</output>`;
  box.querySelector('[data-p113-terrain]').value=env.terrain||'normal';box.querySelector('[data-p113-gravity]').value=g.mode||'normal';
  const out=box.querySelector('output'),report=(res,okText='Ação aplicada e persistida.')=>{if(!res?.ok){out.textContent=String(res?.message||'Ação rejeitada.');out.style.color='#ff9d9d';return}out.style.color='#afe2c0';out.textContent=okText;setTimeout(()=>{box.remove();mount()},100)};
  box.querySelector('[data-p113-apply]').addEventListener('click',()=>report(call('configureEnvironment',{terrain:box.querySelector('[data-p113-terrain]').value,gravityMode:box.querySelector('[data-p113-gravity]').value,gravityLevel:Number(box.querySelector('[data-p113-gravity-level]').value||1),concealment:Number(box.querySelector('[data-p113-conceal]').value||0),waterPresent:box.querySelector('[data-p113-water]').checked,submerged:box.querySelector('[data-p113-submerged]').checked,vacuum:box.querySelector('[data-p113-vacuum]').checked,suffocating:box.querySelector('[data-p113-suffocating]').checked,fire:Number(box.querySelector('[data-p113-fire]').value||0),acid:Number(box.querySelector('[data-p113-acid]').value||0),corrosive:Number(box.querySelector('[data-p113-corrosive]').value||0)})));
  box.querySelector('[data-p113-clear]').addEventListener('click',()=>report(call('clearEnvironment')));
  box.querySelector('[data-p113-imp]').addEventListener('click',()=>{const res=call('resolveImpassableTerrain',{skillKey:box.querySelector('[data-p113-imp-skill]').value,difficulty:Number(box.querySelector('[data-p113-imp-diff]').value||2)});report(res,res?.ok?`Teste: ${Number(res.data?.check?.netSuccess||0)} sucesso(s) líquido(s) · ${res.data?.success?'passagem liberada':'passagem não superada'}.`:'')});
  box.querySelector('[data-p113-fall]').addEventListener('click',()=>{const res=call('resolveFall',{range:box.querySelector('[data-p113-fall-range]').value,skillKey:box.querySelector('[data-p113-fall-skill]').value,useTriumphRangeReduction:box.querySelector('[data-p113-fall-triumph]').checked});report(res,res?.ok?`Queda ${res.data?.effectiveRange||''}: ${Number(res.data?.wounds||0)} Ferimento(s), ${Number(res.data?.strain||0)} Tensão${res.data?.critical?' + Ferimento Crítico':''}.`:'')});
  host.appendChild(box)
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p113-environment',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P151 — AoR contextual skill, NPC droid, and expert hyperspace controls. */
(()=>{'use strict';
 if(window.__BRASEIRO_P151_CONTEXT__)return;window.__BRASEIRO_P151_CONTEXT__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(name,args)=>{const b=window.BraseiroGameBridge;try{return b&&typeof b[name]==='function'?b[name](args):{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 const n=(p,key)=>Number(p.querySelector(key)?.value||0);
 function mount(){
  const screen=route();if(!['01','02','08'].includes(screen))return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p151-context]'))return;
  const state=call('readState');const char=state?.data?.player?.character;if(!char)return;
  const skills=Object.keys(char.skills||{}).sort((a,b)=>a.localeCompare(b,'pt-BR'));
  const panel=document.createElement('section');panel.className='r54-p151-context';panel.dataset.p151Context='1';
  panel.innerHTML=`<small>P151 · AGE OF REBELLION · EFEITOS CONTEXTUAIS</small><h3>TESTES DE PERÍCIA</h3>
   <p>O Mestre confirma o contexto. Bônus só se aplicam aos talentos adquiridos.</p>
   <div class="grid"><label>Perícia<select data-p151-skill>${skills.map(k=>`<option value="${esc(k)}">${esc(trSkill(k))}</option>`).join('')}</select></label>
   <label>Dificuldade<input data-p151-diff type="number" min="0" max="5" value="2"></label>
   <label>Contexto<select data-p151-kind><option value="none">Teste comum</option><option value="chase">Perseguição / atalho</option><option value="crafting">Construir / modificar item</option><option value="scholar">Instituição acadêmica</option><option value="terrain">Pilotagem planetária em terreno difícil</option></select></label>
   <label>Contratempos do terreno<input data-p151-terrain type="number" min="0" max="20" value="2"></label></div>
   <label><input data-p151-natural type="checkbox"> <span data-p159-p151-natural-label>Rerrolagem Nato: verificando perícia…</span></label>
   <button data-p151-roll>ROLAR TESTE CONTEXTUAL</button>
   <h3>DROIDE NPC SOB COMANDO</h3><p>Informe os atributos reais do droide. Falar Binário adiciona bônus apenas ao teste dele.</p>
   <div class="grid"><label>Perícia<select data-p151-droid-skill>${skills.map(k=>`<option value="${esc(k)}">${esc(trSkill(k))}</option>`).join('')}</select></label>
   <label>Característica<input data-p151-droid-char type="number" min="0" max="6" value="3"></label>
   <label>Graduação<input data-p151-droid-rank type="number" min="0" max="5" value="2"></label>
   <label>Dificuldade<input data-p151-droid-diff type="number" min="0" max="5" value="2"></label></div>
   <label><input data-p151-confirm type="checkbox"> Confirmo que este NPC é um droide sob comando</label>
   <button data-p151-droid>ROLAR TESTE DO DROIDE</button>
   ${screen==='08'?'<h3>ASTRONAVEGAÇÃO ESPECIALISTA</h3><label>Dificuldade da rota<input data-p151-nav-diff type="number" min="1" max="5" value="3"></label><label><input data-p151-nav-expert type="checkbox"> Gastar 2 de Tensão para reduzir a dificuldade (se possuir talento)</label><button data-p151-nav>CALCULAR ROTA</button>':''}
   <output aria-live="polite">Pronto.</output>`;
  const css=document.getElementById('r54-p151-css')||document.createElement('style');if(!css.id){css.id='r54-p151-css';css.textContent='.r54-p151-context{margin:12px 14px 110px;padding:14px;background:#111914;border:1px solid #627c69;border-radius:12px;color:#e9eee8}.r54-p151-context .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.r54-p151-context label{display:grid;gap:4px;margin:6px 0}.r54-p151-context input,.r54-p151-context select,.r54-p151-context button{min-height:44px;padding:8px;background:#1b2921;color:#fff;border:1px solid #6c806c;border-radius:8px}.r54-p151-context button{display:block;width:100%;margin:9px 0}.r54-p151-context output{display:block;white-space:pre-wrap;margin-top:8px;color:#bfe9c7}.r54-p151-context h3{color:#e1c98b}@media(max-width:540px){.r54-p151-context .grid{grid-template-columns:1fr}}';document.head.append(css)}
  const out=panel.querySelector('output'),report=r=>{out.textContent=r?.ok?`Teste persistido · ${r.data?.check?.netSuccess??r.data?.netSuccess??0} sucessos líquidos · bônus ${r.data?.binaryBoost??r.data?.talentModifiers?.talentBoost??0}`:String(r?.message||'Ação rejeitada.');out.style.color=r?.ok?'#bfe9c7':'#ffabab'};
  const refreshP151Natural=()=>{const skillKey=panel.querySelector('[data-p151-skill]')?.value;const status=call('naturalRerollStatus',{skillKey});const info=status?.ok?status.data:null;const check=panel.querySelector('[data-p151-natural]');const label=panel.querySelector('[data-p159-p151-natural-label]');if(!check||!label)return;const enabled=Boolean(info?.available&&info?.sessionActive&&!info?.usedThisSession);check.disabled=!enabled;if(!enabled)check.checked=false;label.textContent=info?.available?`${info.label} · ${info.usedThisSession?'JÁ USADO':info.sessionActive?'1× por sessão':'inicie a sessão'}`:'Nenhuma rerrolagem Nato disponível nesta perícia';};
  panel.querySelector('[data-p151-skill]').addEventListener('change',refreshP151Natural);refreshP151Natural();
  panel.querySelector('[data-p151-roll]').addEventListener('click',()=>{const kind=panel.querySelector('[data-p151-kind]').value;report(call('resolveSkillCheck',{skillKey:panel.querySelector('[data-p151-skill]').value,difficulty:n(panel,'[data-p151-diff]'),chase:kind==='chase',crafting:kind==='crafting',scholarlyInstitution:kind==='scholar',terrainSetback:kind==='terrain'?n(panel,'[data-p151-terrain]'):0,useNaturalReroll:panel.querySelector('[data-p151-natural]').checked}))});
  panel.querySelector('[data-p151-droid]').addEventListener('click',()=>report(call('resolveDirectedDroidSkillCheck',{skillKey:panel.querySelector('[data-p151-droid-skill]').value,droidCharacteristic:n(panel,'[data-p151-droid-char]'),droidSkill:n(panel,'[data-p151-droid-rank]'),difficulty:n(panel,'[data-p151-droid-diff]'),confirmedDroid:panel.querySelector('[data-p151-confirm]').checked})));
  panel.querySelector('[data-p151-nav]')?.addEventListener('click',()=>report(call('plotHyperspaceCourse',{difficulty:n(panel,'[data-p151-nav-diff]'),useExpertTalent:panel.querySelector('[data-p151-nav-expert]').checked})));
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p151-contextual-talents',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P157 — verified AoR Commander/Diplomat mechanics, Android PT-BR. */
(()=>{'use strict';
 if(window.__BRASEIRO_P157_AOR__)return;window.__BRASEIRO_P157_AOR__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const call=(name,args)=>{try{const b=window.BraseiroGameBridge;return b?.[name]?.(args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 function mount(){
  const screen=route();if(!['01','02'].includes(screen))return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p157-panel]'))return;
  const state=call('readState')?.data;
  if(!state?.player?.character)return;
  const assets=state.group?.ownedAssets||[];
  const box=document.createElement('section');box.dataset.p157Panel='1';box.className='r54-p157-panel';
  box.innerHTML=`<small>AGE OF REBELLION · P157 · MECÂNICAS OFICIAIS</small>
   <h3>FINTA — DEFESA CONTRA TIROS</h3><p>Manobra uma vez por rodada. Sofra Tensão até o número de graduações; eleva a dificuldade de todos os ataques à distância até seu próximo turno.</p>
   <label>Tensão<input data-p157-feint-strain type="number" min="1" max="6" value="1"></label>
   <button data-p157-feint>ATIVAR FINTA</button>
   <h3>IDENTIFICAÇÃO DO PERSONAGEM</h3><p>O Mestre informa os dados do NPC. Indistinguível aumenta a dificuldade de reconhecer sua identidade, nunca de outros testes.</p>
   <div class="row"><label>Característica NPC<input data-p157-id-char type="number" min="0" max="6" value="2"></label><label>Perícia NPC<input data-p157-id-skill type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-p157-id-diff type="number" min="0" max="6" value="2"></label></div>
   <button data-p157-identify>ROLAR IDENTIFICAÇÃO NPC</button>
   <h3>VENDA LEGAL</h3><p>Chanchullero concede +10% do valor-base por graduação apenas a vendas legais. O Mestre continua aprovando a transação.</p>
   <label>Ativo possuído<select data-p157-asset>${assets.map(a=>`<option value="${esc(a.assetId)}">${esc(a.name)}</option>`).join('')}</select></label>
   <label>Preço-base autorizado<input data-p157-price type="number" min="0" value="100"></label>
   <label><input data-p172-merchant type="checkbox"> Mercador especialista (Negociação bem-sucedida: 2 Tensão, +25% do valor-base)</label>
   <button data-p157-quote>CALCULAR VALOR LEGAL</button>
   <h3>P173 · NEGOCIAÇÃO COM RECIBO PERSISTENTE</h3>
   <p>Primeiro role Negociação (gasta 2 de Tensão mesmo em falha). O recibo vincula o teste ao ativo e impede bônus repetidos. Concluir a venda exige autorização do Mestre.</p>
   <label>Dificuldade<input data-p173-diff type="number" min="0" max="5" value="2"></label>
   <button data-p173-roll>ROLAR NEGOCIAÇÃO DE VENDA</button>
   <label>Recibo válido<select data-p173-receipt><option value="">Nenhum recibo selecionado</option></select></label>
   <label><input data-p173-authorized type="checkbox"> Mestre autorizou esta venda e o preço-base informado</label>
   <button data-p173-sell>CONFIRMAR VENDA COM RECIBO</button>
   ${state.master?.pendingAcquisition?`<h3>COMPRA PENDENTE</h3><p>${esc(state.master.pendingAcquisition.itemName)} · pedido ${esc(state.master.pendingAcquisition.requestId)}</p>
   <label>Preço-base aprovado pelo Mestre<input data-p173-buy-price type="number" min="0" value="${Number(state.master.pendingAcquisition.listedPrice)||0}"></label>
   <button data-p173-buy-roll>ROLAR NEGOCIAÇÃO DE COMPRA</button>
   <label>Recibo de compra<select data-p173-buy-receipt><option value="">Nenhum recibo selecionado</option></select></label>
   <label><input data-p173-buy-approved type="checkbox"> Mestre aprovou a compra</label>
   <button data-p173-buy>RESOLVER COMPRA COM RECIBO</button>`:''}
   <h3>P174 · MECÂNICO AoR · AÇÕES DE CAMPO</h3>
   <p>Sistemas redundantes: teste Fácil de Mecânica, uma vez por sessão. Repara um dispositivo danificado usando outro IDÊNTICO e funcional, sem consumir o doador. Peça defeituosa: teste Difícil; uma falha espontânea só é aplicada a um dispositivo catalogado após autorização do Mestre. Dispositivos externos ainda exigem representação no inventário.</p>
   <label>Dispositivo alvo<select data-p174-target>${assets.filter(a=>['gear','weapon','armor','cybernetic','attachment'].includes(a.kind)).map(a=>`<option value="${esc(a.assetId)}">${esc(a.name)} · ${esc(a.state?.condition)}</option>`).join('')}</select></label>
   <label>Dispositivo doador (idêntico e operacional)<select data-p174-donor>${assets.filter(a=>['gear','weapon','armor','cybernetic','attachment'].includes(a.kind)).map(a=>`<option value="${esc(a.assetId)}">${esc(a.name)} · ${esc(a.state?.condition)}</option>`).join('')}</select></label>
   <label><input data-p174-gm type="checkbox"> Mestre aprovou a aplicação do efeito ao alvo escolhido</label>
   <button data-p174-redundant>USAR SISTEMAS REDUNDANTES</button>
   <button data-p174-motivator>USAR PEÇA DEFEITUOSA</button>
   <h3>P158 · SOBREVIVÊNCIA E TOXINAS</h3><p>Aclimatação remove até dois Contratempos apenas ao procurar comida, água ou abrigo. Resistência a venenos acrescenta um dado de Ampliação apenas para resistir ou recuperar-se de toxinas.</p>
   <div class="row"><label>Contratempos<input data-p158-setback type="number" min="0" max="20" value="2"></label><label>Dificuldade<input data-p158-diff type="number" min="0" max="5" value="2"></label></div>
   <button data-p158-forage>TESTAR BUSCA DE RECURSOS</button><button data-p158-poison>TESTAR RESISTÊNCIA A TOXINAS</button>
   <output aria-live="polite">Pronto.</output>`;
  if(!document.getElementById('r54-p157-css')){const style=document.createElement('style');style.id='r54-p157-css';style.textContent='.r54-p157-panel{margin:14px;padding:14px 12px 120px;background:#131813;color:#eee8d9;border:1px solid #786b47;border-radius:10px}.r54-p157-panel h3{margin:15px 0 6px;color:#e5c68b}.r54-p157-panel small,.r54-p157-panel p{color:#c5c0ac}.r54-p157-panel label{display:grid;gap:4px;margin:7px 0}.r54-p157-panel .row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px}.r54-p157-panel input,.r54-p157-panel select,.r54-p157-panel button{min-height:44px;width:100%;padding:8px;background:#202920;border:1px solid #746c54;color:#fff;border-radius:7px}.r54-p157-panel button{font-weight:750;color:#ffe3a5}.r54-p157-panel output{display:block;white-space:pre-wrap;margin-top:10px;color:#b8e9bd}@media(max-width:430px){.r54-p157-panel .row{grid-template-columns:1fr}}';document.head.append(style)}
  const out=box.querySelector('output'),show=(result,render)=>{out.textContent=result?.ok?render(result.data):String(result?.message||'Ação rejeitada.');out.style.color=result?.ok?'#b8e9bd':'#ffa4a4'};
  const num=key=>Number(box.querySelector(key)?.value||0);
  box.querySelector('[data-p157-feint]').addEventListener('click',()=>show(call('useFeint',{strain:num('[data-p157-feint-strain]')}),d=>`Finta aplicada: +${d.difficultyUpgrades} melhoria(s) na dificuldade de ataques à distância.`));
  box.querySelector('[data-p157-identify]').addEventListener('click',()=>show(call('resolveIncomingIdentificationCheck',{actorCharacteristic:num('[data-p157-id-char]'),actorSkill:num('[data-p157-id-skill]'),difficulty:num('[data-p157-id-diff]')}),d=>`Teste registrado: ${d.check?.netSuccess??0} sucesso(s) líquidos; Indistinguível ${d.talent?.rank??0}.`));
  box.querySelector('[data-p157-quote]').addEventListener('click',()=>show(call('legalSaleQuote',{assetId:box.querySelector('[data-p157-asset]').value,basePrice:num('[data-p157-price]'),masterMerchant:box.querySelector('[data-p172-merchant]').checked}),d=>`Valor autorizado: ${d.finalPrice} créditos (base ${d.basePrice}, Chanchullero +${d.bonus}, Mercador +${d.masterMerchant?.bonus??0}, Tensão ${d.masterMerchant?.strainCost??0}).`));

const refreshReceipts=()=>{
  const latest=call('readState')?.data;
  const all=latest?.master?.merchantNegotiationReceipts||[];
  const saleAsset=box.querySelector('[data-p157-asset]')?.value;
  const fill=(selector,kind,targetId)=>{
    const sel=box.querySelector(selector);if(!sel)return;
    const selected=sel.value;
    const receipts=all.filter(r=>!r.consumed&&r.kind===kind&&r.targetId===targetId);
    sel.innerHTML='<option value="">Selecione o recibo</option>'+receipts.map(r=>`<option value="${esc(r.id)}">${r.success?'SUCESSO':'FALHA'} · ${esc(r.id.slice(-8))} · ${esc(r.at)}</option>`).join('');
    if(receipts.some(r=>r.id===selected))sel.value=selected;
  };
  fill('[data-p173-receipt]','sell',saleAsset);
  fill('[data-p173-buy-receipt]','buy',latest?.master?.pendingAcquisition?.requestId);
};
box.querySelector('[data-p157-asset]').addEventListener('change',refreshReceipts);
box.querySelector('[data-p173-roll]').addEventListener('click',()=>{
  const assetId=box.querySelector('[data-p157-asset]').value;
  const result=call('resolveMerchantNegotiation',{kind:'sell',targetId:assetId,difficulty:num('[data-p173-diff]')});
  show(result,d=>`Negociação ${d.check.success?'bem-sucedida':'fracassada'}; ${d.check.netSuccess} sucesso(s) líquidos. 2 Tensão pagos. Recibo ${d.receipt.id}.`);
  refreshReceipts();
  if(result?.ok)box.querySelector('[data-p173-receipt]').value=result.data.receipt.id;
});
box.querySelector('[data-p173-sell]').addEventListener('click',()=>{
  if(!box.querySelector('[data-p173-authorized]').checked){show({ok:false,message:'Confirmação do Mestre obrigatória.'});return;}
  const result=call('completeMerchantSale',{assetId:box.querySelector('[data-p157-asset]').value,price:num('[data-p157-price]'),receiptId:box.querySelector('[data-p173-receipt]').value,legalSale:true});
  show(result,d=>`Venda concluída por ${d.record.price} créditos; recibo consumido sem cobrar Tensão novamente.`);
  refreshReceipts();
});
box.querySelector('[data-p173-buy-roll]')?.addEventListener('click',()=>{
  const current=call('readState')?.data?.master?.pendingAcquisition;
  const result=call('resolveMerchantNegotiation',{kind:'buy',targetId:current?.requestId,difficulty:num('[data-p173-diff]')});
  show(result,d=>`Negociação de compra ${d.check.success?'bem-sucedida':'fracassada'}; 2 Tensão pagos. Recibo ${d.receipt.id}.`);
  refreshReceipts();
  if(result?.ok)box.querySelector('[data-p173-buy-receipt]').value=result.data.receipt.id;
});
box.querySelector('[data-p173-buy]')?.addEventListener('click',()=>{
  const approved=box.querySelector('[data-p173-buy-approved]').checked;
  const result=call('completeMerchantAcquisition',{receiptId:box.querySelector('[data-p173-buy-receipt]').value,approved,finalPrice:approved?num('[data-p173-buy-price]'):null});
  show(result,d=>`Aquisição ${d.record.status}: ${d.record.finalPrice??0} créditos; recibo consumido.`);
  refreshReceipts();
});
refreshReceipts();

  const p174=(kind)=>{
    const approved=box.querySelector('[data-p174-gm]').checked;
    if(!approved){show({ok:false,message:'Confirmação do Mestre obrigatória para o efeito.'});return;}
    const targetId=box.querySelector('[data-p174-target]').value;
    const donorId=kind==='redundant'?box.querySelector('[data-p174-donor]').value:null;
    const actionId=`p174_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,9)}`;
    show(call('useAoRMechanicFieldAction',{kind,targetId,donorId,actionId,approved}),
      d=>`${kind==='redundant'?'Sistemas redundantes':'Peça defeituosa'}: ${d.check.success?'SUCESSO':'FALHA'} · efeito ${d.receipt.applied?'aplicado':'não aplicado'} · recibo ${d.receipt.actionId}.`);
  };
  box.querySelector('[data-p174-redundant]').addEventListener('click',()=>p174('redundant'));
  box.querySelector('[data-p174-motivator]').addEventListener('click',()=>p174('motivator'));
  box.querySelector('[data-p158-forage]').addEventListener('click',()=>show(call('resolveSkillCheck',{skillKey:'supervivencia',foraging:true,setback:num('[data-p158-setback]'),difficulty:num('[data-p158-diff]')}),d=>`Busca de recursos: ${d.netSuccess??0} sucesso(s); Contratempos finais ${d.talentModifiers?.setback??0}.`));
  box.querySelector('[data-p158-poison]').addEventListener('click',()=>show(call('resolveSkillCheck',{skillKey:'aguante',poisonResistance:true,setback:num('[data-p158-setback]'),difficulty:num('[data-p158-diff]')}),d=>`Resistência a toxinas: ${d.netSuccess??0} sucesso(s); dados de Ampliação ${d.talentModifiers?.boost??0}.`));
  host.append(box);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p157-aor-integrated',mount);else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* R54 P160 — source-backed AoR ammunition, piloting and Heroic Fortitude. */
(()=>{'use strict';
 if(window.__BRASEIRO_P160_AOR__)return;window.__BRASEIRO_P160_AOR__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const call=(method,...args)=>{try{const bridge=window.BraseiroGameBridge;return typeof bridge?.[method]==='function'?bridge[method](...args):{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){
  if(!['01','02'].includes(route()))return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p160-panel]'))return;
  const state=call('readState')?.data,character=state?.player?.character;
  if(!character)return;
  const weapons=character.weapons||[];
  const panel=document.createElement('section');panel.dataset.p160Panel='1';panel.className='r54-p160-panel';
  panel.innerHTML=`<small>AGE OF REBELLION · P160 · TALENTOS E COMBATE</small>
   <h3>FIBRA DE HERÓI</h3><p>Gasta 1 Destino luminoso e ignora penalidades de Ferimentos Críticos em testes de Fortaleza e Agilidade até o fim do encontro. Os ferimentos e demais efeitos permanecem.</p>
   <button data-p160-heroic>ATIVAR FIBRA DE HERÓI</button>
   <h3>DESESPERO DE MUNIÇÃO</h3><p>O Mestre decide aplicar o Desespero do último ataque. Munição de reserva impede esgotamento por Desespero, mas não evita o consumo de Munição Limitada.</p>
   <label>Arma<select data-p160-weapon>${weapons.map((w,i)=>`<option value="${i}">${esc(w.name||w.id||'Arma '+(i+1))}</option>`).join('')}</select></label>
   <button data-p160-despair>APLICAR DESESPERO (MESTRE)</button><button data-p160-reload>USAR RECARGA ADICIONAL</button>
   <p>Piloto especialista remove um Contratempo por graduação em testes de Pilotagem Planetária e Espacial.</p>
   <output aria-live="polite">Pronto.</output>`;
  if(!document.getElementById('r54-p160-css')){
   const style=document.createElement('style');style.id='r54-p160-css';style.textContent='.r54-p160-panel{margin:14px;padding:14px 12px 110px;background:#151b1c;color:#f3ead7;border:1px solid #627b83;border-radius:10px}.r54-p160-panel h3{margin:12px 0 6px;color:#e9cb8f}.r54-p160-panel button{margin:6px 4px 6px 0;min-height:43px}.r54-p160-panel output{display:block;white-space:pre-wrap;margin:9px 0;color:#b8e9bd}';document.head.append(style);
  }
  const out=panel.querySelector('output');
  const report=(r,success)=>{out.textContent=r?.ok?success(r.data):String(r?.message||'Ação rejeitada.');out.style.color=r?.ok?'#b8e9bd':'#ffa4a4';};
  const weapon=()=>Number(panel.querySelector('[data-p160-weapon]').value);
  panel.querySelector('[data-p160-heroic]').addEventListener('click',()=>report(call('activateHeroicFortitude'),()=> 'Fibra de herói ativa: 1 Destino gasto. Efeito persistido até o fim do encontro.'));
  panel.querySelector('[data-p160-despair]').addEventListener('click',()=>report(call('adjudicateAttackDespairAmmoLoss',{weaponIndex:weapon()}),d=>d.prevented?'Munição de reserva impediu o esgotamento.':'Arma sem munição; recarregue antes de atacar.'));
  panel.querySelector('[data-p160-reload]').addEventListener('click',()=>report(call('reloadLimitedAmmoWeapon',weapons[weapon()]?.id||weapons[weapon()]?.name,{maneuverMethod:'normal'}),()=> 'Recarga adicional consumida e munição restaurada.'));
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p160-aor-talents',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* R54 P161 — AoR check, vehicle and legal-market talent actions; PT-BR. */
(()=>{'use strict';
 if(window.__BRASEIRO_P161_AOR__)return;window.__BRASEIRO_P161_AOR__=true;
 const call=(name,...args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[name]==='function'?b[name](...args):{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){
  const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p161-panel]'))return;
  const state=call('readState')?.data,c=state?.player?.character;if(!c)return;
  const panel=document.createElement('section');panel.dataset.p161Panel='1';panel.className='r54-p160-panel';
  panel.innerHTML=`<small>AGE OF REBELLION · P161 · AÇÕES INTEGRADAS</small>
   <h3>NERVOS DE AÇO</h3><p>Gasta 1 Destino para ignorar penalidades de críticos nos testes de Presença/Vontade até o fim do encontro.</p><button data-p161-steely>ATIVAR NERVOS DE AÇO</button>
   <h3>EMBARQUE RÁPIDO</h3><p>Com talento adquirido, embarca ou desembarca como incidental uma vez por rodada. Sem ele, consome manobra.</p>
   <label>Posto<select data-p161-station><option value="cockpit">Pilotagem</option><option value="weapon_station">Artilharia</option><option value="passenger">Passageiro</option></select></label>
   <button data-p161-board>EMBARCAR</button><button data-p161-dismount>DESEMBARCAR</button>
   <h3>CONHECIMENTO ESPECIALIZADO</h3><p>Com perícia escolhida na compra, gaste 1 Triunfo na rolagem para acrescentar sucessos iguais às graduações.</p>
   <label>Conhecimento<select data-p161-skill>${['bajos_fondos','borde_exterior','educacion','estrategia_militar','mundos_del_nucleo','saber_arcaico','xenologia'].map(k=>`<option value="${k}">${k.replaceAll('_',' ')}</option>`).join('')}</select></label>
   <button data-p161-check>TESTE COM TRIUNFO (DIFICULDADE 2)</button>
   <h3>PROVEDOR HABITUAL</h3><p>Uma vez por sessão, reduz a raridade de item legalmente disponível por graduação. A aquisição continua sujeita ao Mestre.</p>
   <label>Tipo<select data-p161-kind><option value="gear">Equipamento</option><option value="weapon">Arma</option><option value="armor">Armadura</option><option value="attachment">Acessório</option></select></label>
   <label>ID oficial do item<input data-p161-item placeholder="ID do catálogo" autocomplete="off" maxlength="80"></label>
   <button data-p161-market>SOLICITAR COMPRA LEGAL</button><output aria-live="polite">Pronto.</output>`;
  const pending=(c.talentPurchases||[]).filter(p=>p.originalName==='CONOCIMIENTO ESPECIALIZADO'&&!p.choice&&typeof p.nodeId==='string');
  if(pending.length){
   const row=document.createElement('div');const label=document.createElement('label');label.textContent='Talento comprado sem perícia vinculada';
   const select=document.createElement('select');select.dataset.p161BindNode='1';
   for(const p of pending){const option=document.createElement('option');option.value=p.nodeId;option.textContent=p.name||p.nodeId;select.append(option)}
   label.append(select);row.append(label);
   const button=document.createElement('button');button.textContent='VINCULAR PERÍCIA DE CONHECIMENTO';button.dataset.p161Bind='1';row.append(button);
   panel.querySelector('[data-p161-check]').before(row);
  }
  const out=panel.querySelector('output');const report=(r,success)=>{out.textContent=r?.ok?success(r.data):String(r?.message||'Ação rejeitada.');out.style.color=r?.ok?'#b8e9bd':'#ffa4a4';};
  panel.querySelector('[data-p161-bind]')?.addEventListener('click',()=>report(call('assignKnowledgeSpecialization',{
   nodeId:panel.querySelector('[data-p161-bind-node]').value,skillKey:panel.querySelector('[data-p161-skill]').value
  }),()=> 'Perícia vinculada permanentemente ao talento comprado.'));
  panel.querySelector('[data-p161-steely]').addEventListener('click',()=>report(call('activateSteelyNerves'),()=> 'Nervos de aço ativado; Destino e estado persistidos.'));
  const board=dismount=>report(call('boardActiveVehicle',{station:panel.querySelector('[data-p161-station]').value,dismount}),d=>d.incidental?'Embarque incidental realizado.':'Manobra de embarque registrada.');
  panel.querySelector('[data-p161-board]').addEventListener('click',()=>board(false));panel.querySelector('[data-p161-dismount]').addEventListener('click',()=>board(true));
  panel.querySelector('[data-p161-check]').addEventListener('click',()=>report(call('resolveSkillCheck',{skillKey:panel.querySelector('[data-p161-skill]').value,difficulty:2,spendKnowledgeTriumph:true}),d=>`Resultado: ${d.netSuccess} sucesso(s). Conhecimento especializado: +${d.knowledgeSpecialization?.bonusSuccesses||0}.`));
  panel.querySelector('[data-p161-market]').addEventListener('click',()=>{const item=panel.querySelector('[data-p161-item]').value.trim();if(!item){out.textContent='Informe um ID existente no catálogo.';return;}report(call('proposeAorAcquisition',panel.querySelector('[data-p161-kind]').value,item,{useKnowSomebody:true}),d=>`Aquisição enviada ao Mestre. Raridade efetiva: ${d.pendingAcquisition?.availability?.effectiveRarity??'a definir'}.`)});
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p161-aor-integrated',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();


/* R54 P162 — AoR Lábia/Smooth Talker & Sound Investments, Android PT-BR. */
(()=>{'use strict';
 if(window.__BRASEIRO_P162_AOR__)return;window.__BRASEIRO_P162_AOR__=true;
 const call=(name,...args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[name]==='function'?b[name](...args):{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){
  const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p162-panel]'))return;
  const state=call('readState')?.data,c=state?.player?.character;if(!c)return;
  const purchased=(c.talentPurchases||[]).filter(p=>p.originalName==='INVERSIONES RENTABLES'&&p.nodeId);
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p162Panel='1';
  panel.innerHTML=`<small>AGE OF REBELLION · P162 · ECONOMIA E DIPLOMACIA</small>
    <h3>INVESTIMENTOS RENTÁVEIS</h3><p>Ao abrir uma nova sessão, concede 100 créditos por graduação adquirida. O crédito e a sessão são salvos numa única operação.</p>
    <p>Posições compradas: ${purchased.length}. Crédito previsto: ${purchased.length*100} por sessão.</p>
    <h3>LÁBIA</h3><p>Gaste 1 Triunfo numa perícia social vinculada para adicionar sucessos iguais às graduações. Sem Triunfo, nenhum bônus é criado.</p>
    <label>Perícia<select data-p162-skill><option value="carisma">Carisma</option><option value="coaccion">Coação</option><option value="engano">Enganação</option><option value="negociacion">Negociação</option></select></label>
    <button data-p162-check>ROLAR TESTE SOCIAL (DIFICULDADE 2)</button><output aria-live="polite">Pronto.</output>`;
  const pending=(c.talentPurchases||[]).filter(p=>p.originalName==='LABIA'&&!p.choice&&typeof p.nodeId==='string');
  if(pending.length){
   const row=document.createElement('div'),label=document.createElement('label');label.textContent='Lábia antiga sem perícia';
   const select=document.createElement('select');select.dataset.p162BindNode='1';
   for(const p of pending){const option=document.createElement('option');option.value=p.nodeId;option.textContent=p.name||p.nodeId;select.append(option)}
   label.append(select);row.append(label);const button=document.createElement('button');button.textContent='VINCULAR LÁBIA';button.dataset.p162Bind='1';row.append(button);panel.querySelector('[data-p162-check]').before(row);
  }
  const out=panel.querySelector('output'),report=(r,success)=>{out.textContent=r?.ok?success(r.data):String(r?.message||'Ação rejeitada.');out.style.color=r?.ok?'#b8e9bd':'#ffa4a4';};
  panel.querySelector('[data-p162-bind]')?.addEventListener('click',()=>report(call('assignSmoothTalker',{nodeId:panel.querySelector('[data-p162-bind-node]').value,skillKey:panel.querySelector('[data-p162-skill]').value}),()=> 'Lábia vinculada à perícia escolhida e persistida.'));
  panel.querySelector('[data-p162-check]').addEventListener('click',()=>report(call('resolveSkillCheck',{skillKey:panel.querySelector('[data-p162-skill]').value,difficulty:2,spendSmoothTalkerTriumph:true}),d=>`Resultado: ${d.netSuccess} sucesso(s). Lábia: +${d.smoothTalker?.bonusSuccesses||0}.`));
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p162-aor-income-social',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();


/* R54 P167 — Familiar Suns AoR, no invented planetary facts. */
(()=>{'use strict';if(window.__BRASEIRO_P167_SUNS__)return;window.__BRASEIRO_P167_SUNS__=true;
 const call=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const status=call('familiarSunsStatus');if(!status?.ok||!status.data?.available)return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p167-suns]'))return;
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p167Suns='1';
  panel.innerHTML='<small>AGE OF REBELLION · P167</small><h3>SÓIS FAMILIARES</h3><p>Uma vez por sessão, faça um teste Difícil (3) de Conhecimento para reconhecer características do planeta. O Mestre revela os detalhes somente após sucesso.</p><label>Conhecimento<select data-p167-skill><option value="borde_exterior">Borda Exterior</option><option value="mundos_del_nucleo">Mundos do Núcleo</option></select></label><button data-p167-activate>EXAMINAR AMBIENTE PLANETÁRIO</button><output aria-live="polite">Pronto.</output>';
  const button=panel.querySelector('[data-p167-activate]'),out=panel.querySelector('output');
  button.disabled=Boolean(status.data.usedThisSession||!status.data.sessionActive);
  if(button.disabled)out.textContent=status.data.usedThisSession?'Talento já usado nesta sessão.':'Inicie uma sessão para usar o talento.';
  button.addEventListener('click',()=>{const result=call('useFamiliarSuns',{skillKey:panel.querySelector('[data-p167-skill]').value});
   if(!result?.ok){out.textContent=String(result?.message||'Ação rejeitada.');return;}
   button.disabled=true;out.textContent=result.data.success?'Teste bem-sucedido. Mestre: revele habitação, ambiente, predadores, riscos e oportunidades presentes neste planeta.':'Teste falhou. Uso da sessão consumido; nenhuma informação é inventada.';});
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p167-familiar-suns',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();


/* R54 P167 — Mental Fortress AoR: critical check suppression, not injury healing. */
(()=>{'use strict';if(window.__BRASEIRO_P167_MENTAL__)return;window.__BRASEIRO_P167_MENTAL__=true;
 const call=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const status=call('talentActionStatus');if(!status?.ok||!status.data?.mentalFortress?.available)return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p167-mental]'))return;
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p167Mental='1';
  panel.innerHTML='<small>AGE OF REBELLION · P167</small><h3>FORTALEZA MENTAL</h3><p>Durante um encontro, gaste 1 ponto luminoso de Destino para ignorar penalidades de Ferimentos Críticos em testes de Intelecto e Astúcia. Não cura ferimentos nem remove incapacitação.</p><button data-p167-activate>ATIVAR FORTALEZA MENTAL</button><output aria-live="polite">Pronto.</output>';
  const button=panel.querySelector('[data-p167-activate]'),out=panel.querySelector('output');
  button.disabled=Boolean(status.data.mentalFortress.active||!status.data.combatActive);
  if(button.disabled)out.textContent=status.data.mentalFortress.active?'Já ativo neste encontro.':'Inicie um encontro para ativar.';
  button.addEventListener('click',()=>{const result=call('activateMentalFortress');out.textContent=result?.ok?'Destino gasto; proteção mental ativa até o fim do encontro.':String(result?.message||'Ação rejeitada.');if(result?.ok)button.disabled=true;});
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p167-mental-fortress',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* R54 P168 — integrated AoR Diplomat social talents and companion checks. */
(()=>{'use strict';if(window.__BRASEIRO_P168_SOCIAL__)return;window.__BRASEIRO_P168_SOCIAL__=true;
 const call=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){
  const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const status=call('aorSocialStatus');if(!status?.ok)return;
  const d=status.data;if(!d.inspiring.available&&!d.scathing.available&&!d.intensePresence.available)return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p168-social]'))return;
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p168Social='1';
  const heading=document.createElement('h3');heading.textContent='AGE OF REBELLION · TALENTOS SOCIAIS P168';panel.append(heading);
  const description=document.createElement('p');description.textContent='Retórica: recupera Tensão de companheiros próximos. Língua afiada: inflige Tensão a inimigos em alcance Curto. A rolagem e os custos são registrados no mesmo salvamento.';panel.append(description);
  const label=(title,element)=>{const row=document.createElement('label');row.textContent=title;row.append(element);panel.append(row);return element};
  const selector=document.createElement('select');
  for(const [key,title,enabled] of [['inspiring','Retórica inspiradora',d.inspiring.available],['scathing','Língua afiada',d.scathing.available]])if(enabled){const o=document.createElement('option');o.value=key;o.textContent=title;selector.append(o)}
  if(selector.options.length)label('Talento',selector);
  const targets=document.createElement('div');panel.append(targets);
  const close=document.createElement('input');close.type='checkbox';close.checked=false;
  const closeLabel=label('Confirmo que os companheiros estão em alcance Curto ',close);
  const supreme=document.createElement('input');supreme.type='checkbox';label('Usar como manobra (versão suprema, custa 1 Tensão) ',supreme);
  const method=document.createElement('select');for(const [key,name] of [['normal','Manobra livre'],['strain','Manobra extra: 2 Tensão'],['action','Trocar ação por manobra']]){const o=document.createElement('option');o.value=key;o.textContent=name;method.append(o)}label('Pagamento da manobra ',method);
  const out=document.createElement('output');out.setAttribute('aria-live','polite');out.textContent=d.combatActive?'Selecione os alvos.':'Inicie um encontro para usar os talentos sociais.';
  const action=document.createElement('button');action.textContent='APLICAR TALENTO SOCIAL';action.disabled=!d.combatActive||!selector.options.length;
  panel.append(action,out);
  const report=(r,onSuccess)=>{out.textContent=r?.ok?onSuccess(r.data):String(r?.message||'Ação rejeitada.');out.style.color=r?.ok?'#b8e9bd':'#ffa4a4';};
  function refreshTargets(){targets.replaceChildren();const kind=selector.value,items=kind==='inspiring'?d.companions:d.enemies;
   closeLabel.parentElement.style.display=kind==='inspiring'?'':'none';
   supreme.disabled=!(d[kind]?.supreme);if(supreme.disabled)supreme.checked=false;
   const tip=document.createElement('p');tip.textContent=kind==='inspiring'?'Selecione os companheiros afetados (uma pessoa por sucesso).':'Selecione os inimigos próximos (um inimigo por sucesso).';targets.append(tip);
   for(const item of items){if(kind==='scathing'&&!item.eligible)continue;
    const row=document.createElement('label'),input=document.createElement('input');input.type='checkbox';input.value=item.id;input.dataset.p168Target='1';row.append(input,document.createTextNode(' '+item.name+(kind==='inspiring'?` · Tensão ${item.strain}`:` · ${item.range}`)));targets.append(row)}
   if(!targets.querySelector('input')){const empty=document.createElement('p');empty.textContent='Nenhum alvo válido disponível nesta cena.';targets.append(empty)}
  }
  selector.addEventListener('change',refreshTargets);refreshTargets();
  action.addEventListener('click',()=>{
   const targetIds=[...targets.querySelectorAll('[data-p168-target]:checked')].map(x=>x.value);
   const r=call('performAoRSocialTalent',{kind:selector.value,targetIds,confirmedShortRange:close.checked,asManeuver:supreme.checked,maneuverMethod:method.value});
   report(r,x=>`${x.affected.length} alvo(s). Êxitos: ${x.check.netSuccess}; Vantagens gastas: ${x.advantageSpent}. Efeitos e custos persistidos.`);
   if(r?.ok){setTimeout(()=>{panel.remove();mount()},4000)}
  });
  if(d.intensePresence.available){const button=document.createElement('button');button.textContent='PRESENÇA IMPONENTE · 1 DESTINO LUMINOSO';button.disabled=d.intensePresence.strain<1;button.addEventListener('click',()=>report(call('useIntensePresence'),x=>`Recuperados ${x.recovered} de Tensão; Destino luminoso gasto.`));panel.append(button)}
  if(d.inspiring.available&&d.companions.length){const title=document.createElement('p');title.textContent='Teste de perícia de companheiro (inclui Impulso da Retórica aprimorada enquanto durar):';panel.append(title);
   const member=document.createElement('select');for(const c of d.companions){const o=document.createElement('option');o.value=c.id;o.textContent=c.name;member.append(o)}label('Companheiro ',member);
   const skill=document.createElement('select');for(const [key,name] of [['percepcion','Percepção'],['alerta','Vigilância'],['mecanica','Mecânica'],['medicina','Medicina'],['liderazgo','Liderança']]){const o=document.createElement('option');o.value=key;o.textContent=name;skill.append(o)}label('Perícia ',skill);
   const check=document.createElement('button');check.textContent='ROLAR TESTE DO COMPANHEIRO';check.addEventListener('click',()=>report(call('resolveCompanionSkillCheck',{companionId:member.value,skillKey:skill.value,difficulty:2}),x=>`Sucessos: ${x.netSuccess}; Impulsos da inspiração: ${x.inspiringBoost}.`));panel.append(check)}
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p168-aor-social',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* R54 P170 — AoR source-backed gear/condition/protection panel. */
(()=>{'use strict';
 if(window.__BRASEIRO_P170_PANEL__)return;window.__BRASEIRO_P170_PANEL__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 const call=(method,args)=>{try{const bridge=window.BraseiroGameBridge;if(typeof bridge?.[method]!=='function')return {ok:false,message:`Ação ${method} indisponível.`};return bridge[method](args??{})}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 function mount(){
  const screen=route();document.querySelectorAll('[data-p170-panel]').forEach(el=>{if(el.dataset.p170Panel!==screen)el.remove()});
  if(!['02','08'].includes(screen)||document.querySelector(`[data-p170-panel="${screen}"]`))return;
  const response=call('aorP170Status');if(!response?.ok)return;const data=response.data;
  if(screen==='02'&&!data.hardHeaded.available&&!data.bodyGuard.available&&!data.companions.length)return;
  if(screen==='08'&&!data.juryRigged.nodes.length)return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector(`[data-p92-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const panel=document.createElement('section');panel.dataset.p170Panel=screen;panel.className='r54-p170-panel';
  panel.style.cssText='margin:12px 14px 100px;padding:12px;border:1px solid #877146;border-radius:10px;background:#14130f;color:#f2e9d3;display:grid;gap:8px';
  const label=(text,child)=>{const row=document.createElement('label');row.textContent=text;row.append(child);panel.append(row);return child};
  const select=(rows)=>{const element=document.createElement('select');for(const [value,title] of rows){const option=document.createElement('option');option.value=String(value);option.textContent=String(title);element.append(option)}element.style.cssText='width:100%;min-height:38px;background:#292419;color:#f1e1bb';return element};
  const button=(title,fn)=>{const b=document.createElement('button');b.textContent=title;b.style.cssText='min-height:42px;background:#46341a;color:#ffe2a3;border:1px solid #99804e;border-radius:7px';b.addEventListener('click',()=>{const r=fn();output.textContent=r?.ok?'Aplicado e salvo. '+(r.data?.success===undefined?'':`Teste: ${r.data.success?'sucesso':'falha'}.`):String(r?.message||'Ação recusada.');output.style.color=r?.ok?'#b4e7bb':'#ffb2b2';if(r?.ok)setTimeout(()=>{panel.remove();mount()},300)});panel.append(b);return b};
  const heading=document.createElement('h3');heading.textContent=screen==='02'?'P171 · COMBATE / GUARDA-COSTAS':'P171 · MELHORIAS CASEIRAS';panel.append(heading);
  const output=document.createElement('output');output.style.cssText='display:block;min-height:35px;padding:8px;background:#080b09;white-space:pre-wrap';output.textContent='Ações autenticadas pelo motor e persistidas.';
  if(screen==='02'){
   if(data.combatActive&&data.companions.length){
    const target=label('Alvo da iniciativa inimiga (Mestre) ',select([['','Jogador'],...data.companions.map(c=>[c.id,c.name])]));
    target.value=data.npcCompanionTargetId??'';
    button('DEFINIR ALVO DOS PRÓXIMOS TURNOS NPC',()=>call('setNpcCompanionTarget',{companionId:target.value||null}));
    const note=document.createElement('p');note.textContent='O alvo escolhido recebe ataques nos turnos reais dos NPCs. Guarda-costas aumenta a dificuldade enquanto a proteção estiver ativa.';panel.append(note);
   }
   if(data.hardHeadedRecovery?.ready){const note=document.createElement('p');note.textContent='Tenacidade aprimorada: última oportunidade de recuperar a consciência neste turno.';panel.append(note)}
   if(data.hardHeaded.available){
    const info=document.createElement('p');info.textContent=`Tenacidade: grau ${data.hardHeaded.rank}, dificuldade ${data.hardHeaded.difficulty}. Só pode ser usada enquanto Atordoado ou Desorientado.`;panel.append(info);
    const basic=button('TENACIDADE · TESTE DE DISCIPLINA',()=>call('useHardHeaded',{improved:false}));basic.disabled=!(data.status.staggered||data.status.disoriented);
    if(data.improvedHardHeaded.available){const advanced=button('TENACIDADE APRIMORADA · RECUPERAR TENSÃO',()=>call('useHardHeaded',{improved:true}));advanced.disabled=!(data.combatActive&&data.health.strainIncapacitated)}
   }
   if(data.bodyGuard.available){
    const member=label('Companheiro protegido ',select(data.companions.map(c=>[c.id,c.name])));
    const amount=label('Tensão (até o grau do talento) ',select(Array.from({length:data.bodyGuard.rank},(_,i)=>[i+1,String(i+1)])));
    const engaged=document.createElement('input');engaged.type='checkbox';label('Confirmo alcance de Interação ',engaged);
    const guard=button('ATIVAR GUARDA-COSTAS',()=>call('useBodyGuard',{companionId:member.value,strain:Number(amount.value),confirmedEngaged:engaged.checked}));guard.disabled=!data.combatActive||data.bodyGuardUsed||!data.companions.length;
    if(data.combatActive&&data.companions.length){
     const attack=button('MESTRE: ATAQUE CONTRA COMPANHEIRO (3/2, DANO 5)',()=>call('resolveIncomingCompanionAttack',{companionId:member.value,attackerCharacteristic:3,attackerSkill:2,weaponDamage:5,difficulty:2}));
     attack.title='Ataque de NPC iniciado explicitamente pelo Mestre; não substitui a IA de iniciativa.';
    }
   }
  }else{
   const node=label('Grau adquirido ',select(data.juryRigged.nodes.map((n,i)=>[n,`Melhorias caseiras ${i+1}`])));
   const weapon=label('Arma pessoal possuída ',select(data.weapons.map(w=>[w.index,w.name+(w.effect?` · ${w.effect}`:'')])));
   const effect=label('Modificação ',select([['damage','+1 dano'],['crit','-1 custo de Crítico (mín. 1)']]));
   const apply=button('VINCULAR MODIFICAÇÃO AO ITEM',()=>call('assignJuryRigged',{nodeId:node.value,weaponIndex:Number(weapon.value),effect:effect.value}));apply.disabled=!data.weapons.length;
  }
  panel.append(output);host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p170-source-talents',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* P175 — Source-backed AoR squad command: real check/strain/round state. */
(()=>{'use strict';
 if(window.__BRASEIRO_P175_COMMAND_PANEL__)return;window.__BRASEIRO_P175_COMMAND_PANEL__=true;
 const call=(method,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[method]==='function'?b[method](args??{}):{ok:false,message:'Ponte de jogo indisponível.'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p175-command]').forEach(p=>{if(p.dataset.p175Command!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p175-command="${screen}"]`))return;
  const response=call('aorCommandStatus');if(!response?.ok)return;
  const d=response.data;if(!(d.fieldCommander||d.masterLeader||d.coordinatedAssault||d.situationalAwareness||d.cleverSolution))return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const panel=document.createElement('section');panel.dataset.p175Command=screen;
  panel.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #6b89a1;border-radius:10px;background:#0c1720;color:#e3f2ff;display:grid;gap:8px';
  const title=document.createElement('h3');title.textContent='P175 · COMANDO REBELDE AoR';panel.append(title);
  const out=document.createElement('output');out.style.cssText='display:block;white-space:pre-wrap;padding:8px;background:#071018';out.textContent=`Rodada ${d.round} · ações reais, custos e recibos persistentes.`;
  const row=(label,child)=>{const r=document.createElement('label');r.textContent=label;r.append(child);panel.append(r);return child};
  const check=(text)=>{const c=document.createElement('input');c.type='checkbox';row(text+' ',c);return c};
  const button=(label,action)=>{const b=document.createElement('button');b.textContent=label;b.style.cssText='min-height:42px;background:#243d56;color:#fff;border:1px solid #6288a3;border-radius:6px';b.addEventListener('click',()=>{const r=action();out.textContent=r?.ok?`Registrado: ${JSON.stringify(r.data?.receipt??r.data?.check??r.data).slice(0,300)}`:String(r?.message??'Ação recusada.');out.style.color=r?.ok?'#b9f6c2':'#ffb8b8';if(r?.ok)setTimeout(()=>{panel.remove();mount()},800)});panel.append(b);return b};
  const allies=document.createElement('div');allies.style.cssText='display:grid;gap:4px';
  const choices=[];for(const c of d.companions){const label=document.createElement('label'),input=document.createElement('input');input.type='checkbox';input.value=c.id;label.append(input,document.createTextNode(' '+c.name+' · Tensão '+c.strain));allies.append(label);choices.push(input)}
  panel.append(allies);const selected=()=>choices.filter(c=>c.checked).map(c=>c.value);
  if(d.fieldCommander&&d.active){
   const range=check('Mestre: todos os selecionados em alcance Médio');
   const improved=d.improved?check('Usar Coordenação tática aprimorada (Presença ×2)'):null;
   const action=d.improved?check('Triunfo: conceder ação em vez de manobra'):null;
   const b=button('COORDENAÇÃO TÁTICA · LIDERANÇA (D2)',()=>call('useAoRFieldCommander',{
    actionId:'aor-command-'+Date.now()+'-'+Math.floor(Math.random()*100000),companionIds:selected(),
    confirmedMedium:range.checked,useImproved:Boolean(improved?.checked),grantFreeAction:Boolean(action?.checked)}));
   b.disabled=d.actionUsed||d.fieldCommanderUsed||!d.companions.length;
  }
  if(d.masterLeader&&d.active){const b=button('LÍDER ESPECIALISTA · 2 TENSÃO / −1 DIFICULDADE',()=>call('resolveSkillCheck',{skillKey:'liderazgo',difficulty:2,useMasterLeader:true}));b.disabled=d.actionUsed||d.masterLeaderUsed}
  if(d.coordinatedAssault&&d.active){const engaged=check('Mestre: aliados selecionados em Interação');button('ATAQUE COORDENADO · MANOBRA',()=>call('useAoRCoordinatedAssault',{companionIds:selected(),confirmedEngaged:engaged.checked}))}
  if(d.cleverSolution&&d.sessionActive){
   const skill=document.createElement('select');for(const [key,title] of [['mecanica','Mecânica'],['percepcion','Percepção'],['liderazgo','Liderança'],['computadoras','Computadores'],['sigilo','Furtividade']]){const option=document.createElement('option');option.value=key;option.textContent=title;skill.append(option)}row('Perícia para Solução engenhosa ',skill);
   const b=button('SOLUÇÃO ENGENHOSA · ASTÚCIA (1/SESSÃO)',()=>call('resolveSkillCheck',{skillKey:skill.value,difficulty:2,useCleverSolution:true}));b.disabled=d.actionUsed;
  }
  if(d.situationalAwareness&&d.companions.length){
   const companion=document.createElement('select');for(const member of d.companions){const o=document.createElement('option');o.value=member.id;o.textContent=member.name;companion.append(o)}row('Aliado próximo ao veículo ',companion);
   const skill=document.createElement('select');for(const [key,title] of [['percepcion','Percepção'],['alerta','Vigilância']]){const o=document.createElement('option');o.value=key;o.textContent=title;skill.append(o)}row('Perícia ',skill);
   const range=document.createElement('select');for(const [key,title] of [['short','Curto · 1 Impulso'],['close','Bocajarro · 2 Impulsos']]){const o=document.createElement('option');o.value=key;o.textContent=title;range.append(o)}row('Alcance confirmado ',range);
   const confirmed=check('Mestre: aliado realmente perto do veículo ativo');
   button('CONHECIMENTO DO AMBIENTE · TESTE DO ALIADO',()=>call('resolveCompanionSkillCheck',{companionId:companion.value,skillKey:skill.value,difficulty:2,situationalRange:range.value,confirmedNearVehicle:confirmed.checked}));
  }
  for(const token of d.pendingTokens){if(token.kind!=='maneuver')continue;
   const member=d.companions.find(c=>c.id===token.companionId);
   button('COBERTURA GRATUITA · '+(member?.name??token.companionId),()=>call('useAoRCommandedCover',{companionId:token.companionId}));
  }
  panel.append(out);host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p175-aor-command',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0))}
})();

/* P176 — AoR Engineering, Intelligence and Slicing. PT-BR, no invented results. */
(()=>{'use strict';
 if(window.__BRASEIRO_P176_ADVANCED__)return;window.__BRASEIRO_P176_ADVANCED__=true;
 const call=(method,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[method]==='function'?b[method](args??{}):{ok:false,message:'Ponte indisponível'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const uid=()=>`p176-${Date.now()}-${Math.floor(Math.random()*1e8)}`;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p176-advanced]').forEach(p=>{if(p.dataset.p176Advanced!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p176-advanced="${screen}"]`))return;
  const r=call('aorP176Status');if(!r?.ok)return;const s=r.data;
  if(!s?.sessionActive||!(s.boughtInfo||s.knownSchematic||s.fireControl||s.skilledSlicer||s.masterSlicer))return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const p=document.createElement('section');p.dataset.p176Advanced=screen;p.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #557b91;border-radius:10px;background:#0c1720;color:#e3f2ff;display:grid;gap:8px';
  const title=document.createElement('h3');title.textContent='P176 · ENGENHARIA, INFORMAÇÃO E SLICER AoR';p.append(title);
  const out=document.createElement('output');out.style.cssText='display:block;white-space:pre-wrap;padding:8px;background:#071018';out.textContent='Escolha uma ação. O Mestre deve confirmar fatos do cenário; o jogo não inventa informação.';
  const row=(label,control)=>{const e=document.createElement('label');e.textContent=label+' ';e.append(control);p.append(e);return control};
  const field=(label,placeholder)=>{const i=document.createElement('input');i.type='text';i.placeholder=placeholder;i.maxLength=120;return row(label,i)};
  const check=(label)=>{const i=document.createElement('input');i.type='checkbox';return row(label,i)};
  const button=(label,run)=>{const b=document.createElement('button');b.textContent=label;b.style.cssText='min-height:44px;background:#23445b;color:white;border:1px solid #668ba2;border-radius:6px';b.addEventListener('click',()=>{const r=run();out.textContent=r?.ok?JSON.stringify(r.data,null,2).slice(0,1000):String(r?.message||'Ação recusada');out.style.color=r?.ok?'#b9f6c2':'#ffb8b8';});p.append(b);return b};
  if(s.knownSchematic){const subject=field('Nave/edifício conhecido pelo Mestre','identificador_do_local'),approved=check('Mestre confirma alvo arquitetônico real');button('PROJETO CONHECIDO · CONHECIMENTO DIFÍCIL (3)',()=>call('useAoRKnownSchematic',{actionId:uid(),subjectId:subject.value,approved:approved.checked}));}
  if(s.boughtInfo){const subject=field('Assunto pesquisado','identificador_da_informacao'),approved=check('Mestre confirma informação disponível para compra');const d=document.createElement('select');for(let i=1;i<=5;i++){const o=document.createElement('option');o.value=i;o.textContent=`Dificuldade ${i} · ${i*50} créditos`;d.append(o)}row('Dificuldade da informação',d);const skills=document.createElement('select');for(const [key,label] of [['educacion','Educação'],['borde_exterior','Borda Exterior'],['mundos_del_nucleo','Mundos do Núcleo'],['bajos_fondos','Submundo'],['estrategia_militar','Estratégia militar'],['xenologia','Xenologia']]){const o=document.createElement('option');o.value=key;o.textContent=label;skills.append(o)}row('Perícia de Conhecimento',skills);button('COMPRAR INFORMAÇÃO · 1 SUCESSO',()=>call('useAoRBoughtInfo',{actionId:uid(),subjectId:subject.value,skillKey:skills.value,difficulty:Number(d.value),approved:approved.checked}));}
  if(s.fireControl&&s.vehicleCombatActive)button('FOGO CONTROLADO · MANOBRA VEICULAR',()=>call('useAoRFireControl',{}));
  if(s.vehicleCombatActive&&(s.exhaustPort||s.deadToRights)){
    const exhaust=s.exhaustPort?check('Usar Saída térmica: 1 Destino, ignora Massive'):null;
    const direct=s.deadToRights?check('Usar Impacto direto: 1 Destino se acertar'):null;
    button('ATAQUE VEICULAR · TALENTOS AoR',()=>call('resolveVehicleAttack',{useExhaustPort:Boolean(exhaust?.checked),useDeadToRights:Boolean(direct?.checked)}));
  }
  if(s.skilledSlicer){const system=field('Sistema informático real','identificador_do_sistema'),confirmed=check('Mestre confirma sistema real e acessível');button('SLICER CONSUMADO · GASTAR TRIUNFO',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,slicingSystemId:system.value,confirmedSlicingSystem:confirmed.checked,spendSkilledSlicerTriumph:true}));button('SLICER CONSUMADO · COMPUTADORES COMO MANOBRA',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,slicingSystemId:system.value,useSkilledSlicerManeuver:true}));}
  if(s.masterSlicer)button('SLICER EXPERTO · 2 TENSÃO / −1 DIFICULDADE',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,useMasterSlicer:true}));
  p.append(out);host.append(p);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p176-aor-advanced',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* P177 — AoR source-backed social/engineering/combat actions; no generated NPCs or lore. */
(()=>{'use strict';
 if(window.__BRASEIRO_P177_ACTIONS__)return;window.__BRASEIRO_P177_ACTIONS__=true;
 const call=(name,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[name]==='function'?b[name](args??{}):{ok:false,message:'Ponte Android indisponível'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const uid=()=>`p177_${Date.now()}_${Math.floor(Math.random()*1e8)}`;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p177-actions]').forEach(p=>{if(p.dataset.p177Actions!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p177-actions="${screen}"]`))return;
  const r=call('aorP177Status');if(!r?.ok||!r.data?.sessionActive)return;const s=r.data;
  if(!Object.keys(s).some(k=>typeof s[k]==='number'&&s[k]>0))return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const panel=document.createElement('section');panel.dataset.p177Actions=screen;
  panel.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #607f99;border-radius:10px;background:#0b1922;color:#e3f2ff;display:grid;gap:8px';
  const heading=document.createElement('h3');heading.textContent='AoR · TALENTOS ATIVOS E CONSEQUÊNCIAS';panel.append(heading);
  const output=document.createElement('output');output.style.cssText='white-space:pre-wrap;display:block;background:#06121b;padding:8px';
  output.textContent='Confirme fatos, itens e alvos reais com o Mestre. Ações não inventam conteúdo.';
  const row=(label,control)=>{const el=document.createElement('label');el.textContent=label+' ';el.append(control);panel.append(el);return control};
  const input=(label,hint)=>{const el=document.createElement('input');el.type='text';el.maxLength=300;el.placeholder=hint;return row(label,el)};
  const confirm=(label)=>{const el=document.createElement('input');el.type='checkbox';return row(label,el)};
  const button=(label,fn)=>{const el=document.createElement('button');el.textContent=label;el.style.cssText='min-height:44px;color:white;background:#23445b;border:1px solid #668ba2;border-radius:6px';el.addEventListener('click',()=>{const result=fn();output.textContent=result?.ok?JSON.stringify(result.data,null,2).slice(0,1100):String(result?.message||'Ação recusada');output.style.color=result?.ok?'#b9f6c2':'#ffb8b8';});panel.append(el)};
  const invoke=(kind,subjectId,approved,extra={})=>call('useAoRP177Action',{kind,actionId:uid(),subjectId,approved,...extra});
  if(s.carefulPlanning){const fact=input('Fato narrativo aprovado pelo Mestre','fato concreto, sem revelar segredos');const gm=confirm('Mestre aprova o fato');button('PLANEJAMENTO CUIDADOSO · SEM DESTINO',()=>invoke('careful_planning','scene_fact',gm.checked,{fact:fact.value}));}
  if(s.contraption){const tools=input('ID das ferramentas possuídas','kit'),parts=input('ID das peças possuídas','parts'),gm=confirm('Mestre aprova dispositivo temporário');button('ARTILUGIO · MECÂNICA DIFÍCIL',()=>invoke('contraption','device',gm.checked,{toolsId:tools.value,partsId:parts.value}));}
  if(s.inciteRebellion){const ids=input('IDs de lacaios/rivais separados por vírgula','npc_1,npc_2'),gm=confirm('Mestre confirma alvos presentes');button('INCITAR À REBELIÃO · COERÇÃO DIFÍCIL',()=>invoke('incite_rebellion','encounter',gm.checked,{targetIds:ids.value.split(',').map(x=>x.trim()).filter(Boolean)}));}
  if(s.hiddenStorage){const item=input('ID do item na mochila','cargo'),container=input('ID do veículo ou equipamento possuído','pack'),gm=confirm('Mestre confirma compartimento apropriado');button('COMPARTIMENTO OCULTO · CARGA LIMITADA',()=>invoke('hidden_storage','storage',gm.checked,{itemId:item.value,containerId:container.value}));}
  if(s.greasedPalms){const skill=document.createElement('select');for(const [key,label] of [['negociacion','Negociação'],['carisma','Carisma'],['coaccion','Coerção'],['engano','Enganação'],['liderazgo','Liderança']]){const o=document.createElement('option');o.value=key;o.textContent=label;skill.append(o)}row('Perícia social',skill);
    const amount=document.createElement('select');for(let n=1;n<=s.greasedPalms;n++){const o=document.createElement('option');o.value=n*50;o.textContent=`${n*50} créditos · ${n} aprimoramento(s)`;amount.append(o)}row('Valor do suborno',amount);
    button('UNTAR MÃOS · TESTE SOCIAL REAL',()=>call('resolveSkillCheck',{skillKey:skill.value,difficulty:2,greasedPalmsCredits:Number(amount.value),greasedPalmsActionId:uid()}));}
  if(s.softSpot){const target=input('ID do adversário para Ponto débil','adv_1');button('PONTO DÉBIL · 1 DESTINO SE ACERTAR',()=>call('resolveWeaponAttack',{weaponIndex:0,targetAdversaryId:target.value,useSoftSpot:true}));}
  if(s.stunningBlow){const target=input('ID do adversário corpo a corpo','adv_1');button('GOLPE CONMOCIONADOR · DANO DE TENSÃO',()=>call('resolveWeaponAttack',{weaponIndex:0,targetAdversaryId:target.value,useStunningBlow:true}));}
  if(s.stunningBlowImproved)button('GOLPE CONMOCIONADOR APRIMORADO · GASTAR TRIUNFO',()=>call('activatePendingImprovedStunningBlow',{triumphs:1}));
  panel.append(output);host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p177-aor-actions',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();


/* P178 — six source-backed AoR consumers; real skill, attack and vehicle actions. */
(()=>{'use strict';
 if(window.__BRASEIRO_P178_ACTIONS__)return;window.__BRASEIRO_P178_ACTIONS__=true;
 const call=(name,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[name]==='function'?b[name](args??{}):{ok:false,message:'Ponte Android indisponível'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p178-actions]').forEach(p=>{if(p.dataset.p178Actions!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p178-actions="${screen}"]`))return;
  const r=call('aorP178Status');if(!r?.ok||!r.data?.sessionActive)return;const s=r.data;
  if(!['worksLikeACharm','strokeOfGenius','quickFix','unstoppable','cripplingBlow','formOnMe'].some(k=>s[k]))return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const p=document.createElement('section');p.dataset.p178Actions=screen;
  p.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #6187a3;border-radius:10px;background:#0b1922;color:#e3f2ff;display:grid;gap:8px';
  const h=document.createElement('h3');h.textContent='AoR · ATRIBUTOS, CRÍTICOS E FORMAÇÃO';p.append(h);
  const out=document.createElement('output');out.style.cssText='white-space:pre-wrap;background:#06121b;padding:8px';out.textContent='Talentos exigem aquisição real; efeitos e uso por sessão são persistidos.';
  const row=(label,control)=>{const l=document.createElement('label');l.textContent=label+' ';l.append(control);p.append(l);return control};
  const button=(label,fn)=>{const b=document.createElement('button');b.textContent=label;b.style.cssText='min-height:44px;color:#fff;background:#23445b;border:1px solid #668ba2;border-radius:6px';b.addEventListener('click',()=>{const r=fn();out.textContent=r?.ok?JSON.stringify(r.data).slice(0,950):String(r?.message??'Ação recusada');out.style.color=r?.ok?'#b9f6c2':'#ffb8b8';if(r?.ok)setTimeout(()=>{p.remove();mount()},500)});p.append(b)};
  const skill=document.createElement('select');for(const [key,label] of [['atletismo','Atletismo'],['mecanica','Mecânica'],['negociacion','Negociação'],['percepcion','Percepção'],['liderazgo','Liderança'],['computadoras','Computadores'],['sigilo','Furtividade']]){const o=document.createElement('option');o.value=key;o.textContent=label;skill.append(o)}
  if(s.worksLikeACharm||s.strokeOfGenius||s.quickFix)row('Perícia para substituir característica',skill);
  if(s.worksLikeACharm)button('MÃO SANTA · PRESENÇA (1/SESSÃO)',()=>call('resolveSkillCheck',{skillKey:skill.value,difficulty:2,useWorksLikeACharm:true}));
  if(s.strokeOfGenius)button('ENGENHOSO · INTELECTO (1/SESSÃO)',()=>call('resolveSkillCheck',{skillKey:skill.value,difficulty:2,useStrokeOfGenius:true}));
  if(s.quickFix)button('SOLUÇÃO IMPROVISADA · AGILIDADE (1/SESSÃO)',()=>call('resolveSkillCheck',{skillKey:skill.value,difficulty:2,useQuickFix:true}));
  if(s.cripplingBlow){const id=document.createElement('input');id.placeholder='ID de adversário real';id.maxLength=120;row('Alvo do encontro',id);button('GOLPE INCAPACITANTE · +1 DIFICULDADE',()=>call('resolveWeaponAttack',{weaponIndex:0,targetAdversaryId:id.value,useCripplingBlow:true}));}
  if(s.formOnMe&&s.vehicleCombatActive){
    const confirmId=document.createElement('input');confirmId.placeholder='ID da nave aliada registrada';confirmId.maxLength=120;row('Nave aliada a confirmar pelo Mestre',confirmId);
    const gm=document.createElement('input');gm.type='checkbox';row('Mestre confirma aliança e alcance Curto',gm);
    button('CONFIRMAR NAVE ALIADA REAL',()=>call('confirmAoRFormationAlly',{vehicleId:confirmId.value,approved:gm.checked,closeToActive:gm.checked}));
    const ids=document.createElement('input');ids.placeholder='IDs confirmados, separados por vírgula';ids.maxLength=240;row('Formação aliada em Curto',ids);
    button('FORMAÇÃO DE COMBATE · GANHAR VANTAGEM',()=>call('gainVehicleAdvantage',{alliedVehicleIds:ids.value.split(',').map(x=>x.trim()).filter(Boolean)}));
  }
  if(s.unstoppable){const t=document.createElement('p');t.textContent='IMPARÁVEL ativo: crítico cujo resultado final é 1 não é aplicado.';p.append(t)}
  p.append(out);host.append(p);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p178-aor-attributes',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* P179 — AoR reactive Medicine, NPC Blast receipts, and Time to Go. */
(()=>{'use strict';
 if(window.__BRASEIRO_P179_REACTIONS__)return;window.__BRASEIRO_P179_REACTIONS__=true;
 const call=(name,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[name]==='function'?b[name](args??{}):{ok:false,message:'Ponte Android indisponível'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p179-reactions]').forEach(p=>{if(p.dataset.p179Reactions!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p179-reactions="${screen}"]`))return;
  const r=call('aorP179Status');if(!r?.ok||!r.data?.sessionActive)return;const s=r.data;
  if(!s.timeToGo&&!s.itsNotThatBad&&!s.naturalOutdoorsman&&!(s.pendingBlasts||[]).length)return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const p=document.createElement('section');p.dataset.p179Reactions=screen;
  p.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #63869b;border-radius:10px;background:#0b1922;color:#e3f2ff;display:grid;gap:8px';
  const h=document.createElement('h3');h.textContent='AoR · REAÇÕES A EXPLOSÕES E FERIMENTOS';p.append(h);
  const out=document.createElement('output');out.style.cssText='white-space:pre-wrap;background:#06121b;padding:8px';out.textContent='Reações exigem talento adquirido, evento real e confirmação do Mestre.';
  const row=(label,control)=>{const l=document.createElement('label');l.textContent=label+' ';l.append(control);p.append(l);return control};
  const check=(label)=>{const i=document.createElement('input');i.type='checkbox';return row(label,i)};
  const companion=(label)=>{const s0=document.createElement('select');const none=document.createElement('option');none.value='';none.textContent='Selecione';s0.append(none);for(const c of s.companions||[]){const o=document.createElement('option');o.value=c.id;o.textContent=c.name;s0.append(o)}return row(label,s0)};
  const button=(label,fn)=>{const b=document.createElement('button');b.textContent=label;b.style.cssText='min-height:44px;color:#fff;background:#23445b;border:1px solid #668ba2;border-radius:6px';b.addEventListener('click',()=>{const v=fn();out.textContent=v?.ok?JSON.stringify(v.data,null,2).slice(0,1300):String(v?.message??'Ação recusada');out.style.color=v?.ok?'#b9f6c2':'#ffb8b8';if(v?.ok)setTimeout(()=>{p.remove();mount()},400)});p.append(b)};
  if(s.naturalOutdoorsman){const note=document.createElement('p');note.textContent='SOBREVIVENTE NATO · rerrolagem de Resistência/Sobrevivência disponível no teste de perícia, 1 vez por sessão.';p.append(note)}
  if(s.itsNotThatBad&&!s.medicUsed){const ally=companion('Aliado para reação médica');const gm=check('Mestre confirma aliado real');button('PREPARAR NÃO É TÃO GRAVE · MEDICINA DIFÍCIL',()=>call('prepareItsNotThatBad',{companionId:ally.value,approved:gm.checked}));}
  if(s.medicPrepared){const note=document.createElement('p');note.textContent='Não é tão grave preparado para '+s.medicPrepared.companionId+'; será rolado quando ocorrer ferimento crítico real.';p.append(note)}
  for(const blast of s.pendingBlasts||[]){
   const title=document.createElement('p');title.textContent=`Explosão pendente · ${blast.weaponName} · dano ${blast.blastDamage} · ataque ${blast.attackId}`;p.append(title);
   const gm=check('Mestre confirma alvos Engajados na zona de Explosão');
   const pc=check('Jogador na zona de Explosão');pc.checked=!blast.hit||Boolean(blast.originalTargetCompanionId);
   const affected=companion('Aliado atingido (opcional)');
   const time=s.timeToGo?check('Time to Go · gastar 1 Destino para sair da zona'):null;
   const escape=time?check('Mestre confirma saída real da zona'):null;
   const improved=s.improvedTimeToGo?check('Time to Go aprimorado · aliado Engajado também sai'):null;
   const engaged=improved?check('Mestre confirma aliado Engajado e fuga'):null;
   button('RESOLVER EXPLOSÃO NPC · RECIBO REAL',()=>call('resolveIncomingBlast',{
    attackId:blast.attackId,approved:gm.checked,includePlayer:pc.checked,
    affectedCompanionIds:affected.value?[affected.value]:[],useTimeToGo:Boolean(time?.checked),
    escapeConfirmed:Boolean(escape?.checked),improvedCompanionId:improved?.checked?affected.value:null,
    companionEscapeConfirmed:Boolean(improved?.checked&&engaged?.checked),engagedConfirmed:Boolean(improved?.checked&&engaged?.checked)
   }));
  }
  p.append(out);host.append(p);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p179-aor-reactions',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

/* P197 — SWE09 Wing Guard tactics, explicit GM ally-distance confirmation. */
(()=>{'use strict';
 if(window.__BRASEIRO_P197_WING_GUARD__)return;window.__BRASEIRO_P197_WING_GUARD__=true;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p197-wing-tactics]').forEach(p=>{if(p.dataset.p197WingTactics!==screen)p.remove()});
  if(screen!=='02'||document.querySelector('[data-p197-wing-tactics="02"]'))return;
  const bridge=window.BraseiroGameBridge;
  if(typeof bridge?.combatRuntimeStatus!=='function'||typeof bridge?.queueWingGuardTactic!=='function')return;
  const result=bridge.combatRuntimeStatus(),combat=result?.ok?result.data?.combat:null;
  if(!combat?.active||!combat?.encounter?.slots?.length)return;
  const actors=(combat.adversaries||[]).filter(a=>['bespin_wing_guard_officer','bespin_wing_guard_sergeant'].includes(a.profileKey)&&a.status!=='incapacitated');
  if(actors.length<2)return;
  const host=document.querySelector('[data-p92-mechanics="02"]')||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const p=document.createElement('section');p.dataset.p197WingTactics='02';p.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #6987aa;border-radius:10px;background:#101b29;color:#e3f2ff;display:grid;gap:8px';
  const h=document.createElement('h3');h.textContent='SWE09 · MANOBRAS DA GUARDA ALADA';p.append(h);
  const note=document.createElement('p');note.textContent='O Mestre escolhe o oficial/sargento e confirma o alcance real entre aliados. A manobra ocorre no turno do sargento. A manobra livre do grupo é imediata, sem teleporte.';p.append(note);
  const row=(label,control)=>{const el=document.createElement('label');el.textContent=label+' ';el.append(control);p.append(el);return control};
  const select=(items)=>{const el=document.createElement('select');for(const item of items){const o=document.createElement('option');o.value=item.id;o.textContent=item.name;el.append(o)}return el};
  const source=row('NPC que realizará a manobra',select(actors));
  const kind=row('Manobra',select([{id:'covering_fire',name:'Covering Fire — +1 Defesa à distância (até 2 aliados)'},{id:'tactical_direction',name:'Tactical Direction — 1 Reforço no próximo teste de grupo minion'}]));
  const mode=row('Tactical Direction',select([{id:'boost',name:'Reforço no próximo ataque'},{id:'free_maneuver',name:'Manobra livre imediata: mover 1 faixa no turno do sargento'}]));
  const direction=row('Direção da manobra livre',select([{id:'approach',name:'Aproximar dos personagens'},{id:'withdraw',name:'Afastar dos personagens'}]));
  const path=row('Mestre confirma trajeto transitável, sem obstáculos e movimento de uma faixa',document.createElement('input'));path.type='checkbox';
  const target1=row('Aliado 1',select(actors));const target2=row('Aliado 2 (somente Covering Fire)',select([{id:'',name:'Nenhum'},...actors]));
  const confirm=row('Mestre confirma alcance Curto (Covering Fire) ou Médio (Tactical Direction)',document.createElement('input'));confirm.type='checkbox';
  const out=document.createElement('output');out.style.cssText='white-space:pre-wrap;padding:8px;background:#08121d';out.textContent='Nenhuma manobra solicitada.';
  const refresh=()=>{target2.disabled=kind.value!=='covering_fire';if(target2.disabled)target2.value='';mode.disabled=kind.value!=='tactical_direction';direction.disabled=mode.disabled||mode.value!=='free_maneuver';path.disabled=direction.disabled;if(path.disabled)path.checked=false;};kind.addEventListener('change',refresh);mode.addEventListener('change',refresh);refresh();
  const button=document.createElement('button');button.type='button';button.textContent='AGENDAR MANOBRA NO TURNO REAL';button.style.cssText='min-height:44px;background:#23445b;color:white;border:1px solid #6987aa;border-radius:6px';
  button.addEventListener('click',()=>{
    if(!confirm.checked){out.textContent='Confirme o alcance real entre os NPCs.';return;}
    const targets=[target1.value,...(kind.value==='covering_fire'&&target2.value?[target2.value]:[])];
    const r=bridge.queueWingGuardTactic({actorId:source.value,kind:kind.value,targetIds:targets,confirmedRange:kind.value==='covering_fire'?'short':'medium',mode:kind.value==='tactical_direction'?mode.value:'boost',movementDirection:direction.value,confirmedMovement:!direction.disabled&&path.checked});
    out.textContent=r?.ok?'Manobra registrada. O motor a aplicará no turno do NPC.':String(r?.message||'Manobra recusada.');
    if(r?.ok){button.disabled=true;setTimeout(()=>{p.remove();mount()},450);}
  });
  p.append(button,out);host.append(p);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p197-wing-guard-tactics',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();

if(!window.__r54BottomSheetObserverInstalled){
 window.__r54BottomSheetObserverInstalled=true;
 try{
  const observer = new MutationObserver(()=>{
   if(document.querySelector('.bottom-sheet')) enhanceBottomSheetMenu();
  });
  observer.observe(document.documentElement,{childList:true,subtree:true});
 }catch(_){}
 document.addEventListener('click',e=>{
  if(e.target.closest('.scene-context-menu, [aria-label="Abrir utilidades"]')){
   setTimeout(enhanceBottomSheetMenu,30);
   setTimeout(enhanceBottomSheetMenu,150);
  }
 },true);
}
