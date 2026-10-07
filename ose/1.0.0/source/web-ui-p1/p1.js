(() => {
'use strict';
const A='../assets/ose-user-pack-v1/source/TALES_VTT_ASSET_PACK/assets_individuais/';
const q=new URLSearchParams(window.__P1_CAPTURE_QUERY||location.search);
let screen=q.get('screen')||'session';
let stateName=q.get('state')||'active';
let viewState=null;
let geminiState={textSceneConfigured:false,ttsAudioConfigured:false,textModel:'',imageModel:''};
let ttsState={state:'PREPARING',ready:false,enginePackage:'',voiceName:'',speechRate:1,engines:[],voices:[],mode:'ANDROID_INSTALLED_ENGINE'};
let mediaTarget=null;
let mapSelection=null;
let diceHistory=[];
let backupBuffer='';
let ruleLibrary=[];
let ruleLibraryLoaded=false;
let libraryQuery='';
let lastRuleTrace=null;
let localLog=['Campanha carregada.'];
let editingCharacter=false;
const el=id=>document.getElementById(id);
const main=el('screen');
const hasNative=()=>!!(window.BraseiroBridge&&typeof window.BraseiroBridge.postMessage==='function');
const postNative=(type,payload={})=>{if(!hasNative())return false;window.BraseiroBridge.postMessage(JSON.stringify({version:1,type,payload}));return true};
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const img=(src,alt='',cls='')=>`<img ${cls?`class="${cls}"`:''} src="${esc(src)}" alt="${esc(alt)}" draggable="false">`;
const asset=p=>A+p;
const fallbackNarration={active:'A alvorada cobre as colinas e a luz pálida encontra o caminho entre pedras antigas, capim úmido e manchas de mata. Ao longe, o terreno se dobra em pequenas elevações e some atrás de uma linha escura de árvores. O ar ainda está frio, e nenhum caminho parece completamente seguro. O que você faz?',encounter:'Há movimento adiante. Entre o vento e os sons do terreno, alguma coisa quebra o ritmo natural da paisagem. O grupo ainda tem tempo de observar e decidir como reagir.',combat:'A ameaça ocupa a passagem e obriga a companhia a medir distância, abrigo e intenção. Ninguém age por vocês; diga como pretende reagir.',recovery:'O lugar volta a ficar relativamente quieto. A companhia pode cuidar dos ferimentos, examinar o entorno e decidir o próximo passo.'};
const fallbackDecisions={active:[{label:'LER O TERRENO',reaction:'Examinamos o terreno e os sinais visíveis antes de escolher o caminho.'},{label:'SEGUIR COM CAUTELA',reaction:'Seguimos pelo caminho visível com cautela, atentos a qualquer mudança.'},{label:'OBSERVAR MELHOR',reaction:'Procuramos um ponto de onde seja possível observar melhor os arredores.'}],encounter:[{label:'OBSERVAR',reaction:'Observamos com atenção sem avançar ainda.'},{label:'ESCUTAR',reaction:'Ficamos em silêncio e escutamos antes de agir.'},{label:'RECUAR',reaction:'Recuamos até a última posição que parecia segura.'}],combat:[{label:'OBSERVAR A AMEAÇA',reaction:'Mantemos a formação e observamos a ameaça antes de agir.'},{label:'BUSCAR COBERTURA',reaction:'Procuramos uma posição defensável ao nosso alcance.'},{label:'TENTAR FALAR',reaction:'Tentamos comunicação antes de outra ação hostil.'}],recovery:[{label:'VERIFICAR O GRUPO',reaction:'Verificamos a condição do grupo e os equipamentos.'},{label:'EXAMINAR A ÁREA',reaction:'Examinamos a área visível em busca de mudanças recentes.'},{label:'REORGANIZAR',reaction:'Reorganizamos a companhia antes de seguir.'}]};
const classLabels={ACROBAT:'Acrobata',ASSASSIN:'Assassino',BARBARIAN:'Bárbaro',BARD:'Bardo',CLERIC:'Clérigo',DRUID:'Druida',FIGHTER:'Guerreiro',ILLUSIONIST:'Ilusionista',KNIGHT:'Cavaleiro',MAGIC_USER:'Mago',PALADIN:'Paladino',RANGER:'Patrulheiro',THIEF:'Ladrão',DWARF:'Anão',ELF:'Elfo',HALFLING:'Halfling'};
const raceLabels={HUMAN:'Humano',DROW:'Drow',DUERGAR:'Duergar',DWARF:'Anão',ELF:'Elfo',GNOME:'Gnomo',HALF_ELF:'Meio-elfo',HALFLING:'Halfling',HALF_ORC:'Meio-orc',SVIRFNEBLIN:'Svirfneblin'};
const phaseLabels={ACTIVE:'EM AVENTURA',CHARACTER_LOADOUT:'EQUIPAMENTO INICIAL',READY_TO_START:'PRONTO PARA COMEÇAR',COMBAT:'COMBATE',RECOVERY:'RECUPERAÇÃO'};
const terrainLabels={CLEAR_GRASSLANDS:'Campina',HILLS:'Colinas',MOUNTAINS:'Montanhas',FOREST:'Floresta',DESERT:'Terreno árido',SWAMP:'Pântano',JUNGLE:'Mata densa',BARREN_ADV:'Terras estéreis',BROKEN_LANDS_ADV:'Terreno acidentado',UNKNOWN:'Desconhecido'};

window.BraseiroCreation=Object.freeze({start:draftId=>postNative('CharacterCreationStart',{draftId}),commit:payload=>postNative('CharacterCreationCommit',payload)});
window.BraseiroReceive=(raw)=>{
 try{
  const m=typeof raw==='string'?JSON.parse(raw):raw,p=m.payload||{};
  if(m.type==='ViewState'){viewState=p;geminiState=p.gemini||geminiState;ttsState=p.tts?{...ttsState,...p.tts}:ttsState;lastRuleTrace=p.lastRuleTrace||lastRuleTrace;render();}
  else if(m.type==='SessionUpdate'){
   const previous=viewState;
   if(p.viewState){viewState=p.viewState;geminiState=viewState.gemini||geminiState;}
   lastRuleTrace=p.ruleTrace||viewState?.lastRuleTrace||lastRuleTrace;
   localLog.push(p.mechanicalMutation?'A ação foi resolvida e o mundo foi atualizado.':'O Mestre respondeu sem alterar o estado mecânico.');
   render();showToast('Ação concluída.');announceStateChanges(previous,viewState,p.feedback||'');
  }
  else if(m.type==='CharacterCreationDraft'||m.type==='CharacterCreationResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok===false?'Não foi possível concluir a criação.':'Criação atualizada.');}
  else if(m.type==='UiCommandResult'){if(p.viewState)viewState=p.viewState;render();showToast(uiStatusMessage(p));}
  else if(m.type==='GMHelpResponse')showToast(p.answer||'Sem resposta do Mestre.');
  else if(m.type==='TtsState'){ttsState={...ttsState,...p};updateTtsSettings();showToast(p.state==='ERROR'?`TTS: ${p.message||'falha ao reproduzir o áudio.'}`:ttsMessage(p.state));if(p.state==='IDLE')postNative('TtsSettingsGet',{});}
  else if(m.type==='TtsSettingsState'){ttsState={...ttsState,...p};updateTtsSettings();}
  else if(m.type==='GeminiSettingsState'){geminiState=p;updateSettingsStatus();}
  else if(m.type==='GeminiCredentialTestResult')showToast(p.ok?'Conexão Gemini: OK':'Não foi possível usar a configuração informada.');
  else if(m.type==='SceneImageResult'){postNative('ViewState',{request:'after-scene-image'});showToast('Ilustração da cena atualizada.');}
  else if(m.type==='ScenePromptShareResult')showToast('Abrindo o Gemini com o prompt da cena…');
  else if(m.type==='PortraitPromptShareResult')showToast('Abrindo o Gemini com o prompt do retrato…');
  else if(m.type==='PresentationImageSaved'){postNative('ViewState',{request:'after-media-save'});showToast('Imagem salva e vinculada ao personagem.');}
  else if(m.type==='ManualDiceResult'){diceHistory.unshift({rolls:p.rolls||[],total:p.total,sides:p.sides,count:p.count,modifier:p.modifier||0});diceHistory=diceHistory.slice(0,12);render();showToast(`Resultado: ${p.total}`);}
  else if(m.type==='RuleEvidenceListResult'){ruleLibrary=p.rules||[];ruleLibraryLoaded=true;render();}
  else if(m.type==='RuleEvidenceResult'){showRuleDetail(p);}
  else if(m.type==='ResetAdventureResult'){if(p.viewState)viewState=p.viewState;lastRuleTrace=null;ruleLibraryLoaded=false;ruleLibrary=[];screen='session';render();showToast('A aventura foi recomeçada.');}
  else if(m.type==='CharacterSheetExportResult'){if(p.ok)showToast(`Ficha salva em ${p.fileName||'arquivo JSON'}.`);}
  else if(m.type==='CharacterSheetImportResult'){if(p.viewState)viewState=p.viewState;render();if(p.ok)showToast('Ficha importada.');}
  else if(m.type==='CharacterSheetUpdateResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;render();showToast('Ficha atualizada.');}
  else if(m.type==='CharacterRecreateResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;screen='sheet';render();showToast('Personagem anterior removido. Crie o novo aventureiro.');}
  else if(m.type==='BackupExportResult'){backupBuffer=p.backup||'';const t=el('backupText');if(t)t.value=backupBuffer;showToast('Backup gerado. Copie e guarde o texto.');}
  else if(m.type==='BackupImportResult'){if(p.viewState)viewState=p.viewState;render();showToast('Backup restaurado.');}
  else if(m.type==='BridgeError')showToast(friendlyBridgeError(p.message));
 }catch(_e){showToast('Não foi possível ler a resposta do aplicativo.');}
};

function uiStatusMessage(p){if(p.ok===false)return 'A ação não pôde ser concluída pelas regras atuais.';if(p.status==='READY_TO_START')return 'Equipamento concluído. Agora você pode começar a aventura.';return 'Equipamento atualizado.'}
function friendlyBridgeError(code){const m={GEMINI_KEY_MISSING:'Configure a chave Gemini.',INVALID_GEMINI_MODEL:'Confira o nome do modelo Gemini.',INVALID_TTS_VOICE:'Confira o nome da voz TTS.',EMPTY_VISIBLE_NARRATION:'Ainda não há narração para ouvir.',NO_CHARACTER:'Crie ou importe um personagem primeiro.',CHARACTER_ALREADY_EXISTS:'Já existe um personagem nesta campanha. Recomece a aventura para criar outro.',RANDOM_CHARACTER_NO_VALID_CLASS:'Não foi possível gerar uma classe válida para estes atributos.',INVALID_CHARACTER_SHEET:'A ficha JSON não é válida para o BRASEIRO OSE.',CHARACTER_PROFILE_MISMATCH:'A ficha pertence a outro perfil OSE.',CHARACTER_ID_EXISTS:'Já existe um personagem com este identificador.',CHARACTER_ITEM_PROFILE_MISMATCH:'Um item da ficha pertence a outro perfil OSE.',ITEM_ID_EXISTS:'Já existe um item com este identificador.',INVALID_CHARACTER_NAME:'Digite um nome válido para o personagem.',CHARACTER_RECREATE_AFTER_START:'Depois que a aventura começou, use RECOMEÇAR A AVENTURA para trocar toda a ficha.'};return m[code]||'O aplicativo encontrou um erro ao executar esta ação.'}
function ttsMessage(s){return s==='PREPARING'?'Preparando a narração em voz.':s==='PLAYING'?'Narração em andamento.':s==='STOPPED'?'Áudio interrompido.':s==='IDLE'?'Motor de voz pronto.':s==='ERROR'?'Falha no motor de voz.':'TTS atualizado.'}
function showToast(msg){const t=el('toast');if(!t)return;t.textContent=msg;t.classList.remove('hidden');clearTimeout(showToast.timer);showToast.timer=setTimeout(()=>t.classList.add('hidden'),2600)}
function rationValue(vs){const xs=vs?.resources||[];const hit=xs.find(x=>String(x).toLowerCase().startsWith('rations:'));return hit?Number(String(hit).split(':').pop())||0:0}
function actionCostSummary(intent){if(!intent)return'';const n=Math.max(1,(viewState?.party||[]).length),r=rationValue(viewState);if(intent.startsWith('WILD_TRAVEL_FORAGE_DAY'))return `Dia inteiro de viagem. O motor fará e exibirá os testes autoritativos de perder-se, forrageio e encontro. O grupo pode percorrer vários hexes conforme movimento/terreno. Necessidade de comida: até ${n}; rações atuais: ${r}. Se faltar comida, o déficit ficará registrado.`;if(intent.startsWith('WILD_TRAVEL_DAY')||intent.startsWith('WILD_FORCED_MARCH_DAY'))return `Dia inteiro de viagem. O motor fará e exibirá os testes autoritativos de perder-se e encontro. O grupo pode percorrer vários hexes conforme movimento/terreno${intent.startsWith('WILD_FORCED_MARCH_DAY')?' e aplicará a regra de marcha forçada':''}. Necessidade de comida: até ${n}; rações atuais: ${r}. Se faltar comida, o déficit ficará registrado.`;if(intent.startsWith('WILD_REST_DAY'))return `Descansar consumirá 1 dia. Necessidade de comida: até ${n}; rações atuais: ${r}. Se faltar comida, o déficit ficará registrado.`;return''}
function showEventPopup(title,lines){if(!lines?.length)return;document.getElementById('eventPopup')?.remove();const o=document.createElement('div');o.id='eventPopup';o.className='event-popup-overlay';o.innerHTML=`<div class="event-popup panel"><div class="kicker">Atualização da campanha</div><h3>${esc(title)}</h3>${lines.map(x=>`<p>${esc(x)}</p>`).join('')}<button class="primary" data-event-ok>CONTINUAR</button></div>`;document.getElementById('phone').appendChild(o);o.querySelector('[data-event-ok]').onclick=()=>o.remove()}
function confirmWildernessAction(text,onConfirm){document.getElementById('travelConfirm')?.remove();const o=document.createElement('div');o.id='travelConfirm';o.className='event-popup-overlay';o.innerHTML=`<div class="event-popup panel"><div class="kicker">Procedimento de viagem OSE</div><h3>Preparar testes</h3><p>${esc(text)}</p><div class="row"><button class="secondary" data-travel-cancel>CANCELAR</button><button class="primary" data-travel-roll>ROLAR TESTES E EXECUTAR</button></div></div>`;document.getElementById('phone').appendChild(o);o.querySelector('[data-travel-cancel]').onclick=()=>o.remove();o.querySelector('[data-travel-roll]').onclick=()=>{o.remove();onConfirm()}}
function announceStateChanges(prev,next,feedback=''){if(!prev||!next)return;const lines=[];const pd=Number(prev?.time?.days||0),nd=Number(next?.time?.days||0);if(nd>pd)lines.push(`O tempo avançou: agora é o Dia ${nd+1}.`);const pp=prev?.position||{},np=next?.position||{};if(JSON.stringify(pp)!==JSON.stringify(np))lines.push('A companhia mudou de posição no mundo.');const pr=rationValue(prev),nr=rationValue(next);if(pr!==nr)lines.push(`Rações: ${pr} → ${nr}.`);const lost=feedback.match(/teste de perder-se d6=([^;]+)/i);if(lost)lines.push(`Perder-se: ${lost[1]}.`);const forage=feedback.match(/forragear d6=([^;]+)/i);if(forage)lines.push(`Forrageio: ${forage[1]}.`);const encounter=feedback.match(/encontro d6=([^;]+)/i);if(encounter)lines.push(`Encontro: ${encounter[1]}.`);const deficit=feedback.match(/déficit(?: de rações)?=(\d+)/i);if(deficit&&Number(deficit[1])>0)lines.push(`Faltaram ${deficit[1]} ração(ões); o déficit ficou registrado.`);if(lines.length)showEventPopup(nd>pd?'Um novo dia começa':'O mundo mudou',lines)}
function resource(prefix,fallback='0'){const xs=viewState?.resources||[];const hit=xs.find(x=>String(x).toLowerCase().startsWith(prefix+':'));return hit?String(hit).split(':').slice(1).join(':'):fallback}
function phase(){return viewState?.session?.phase||'ACTIVE'}
function phaseLabel(){if(hasNative()&&!viewState?.selectedCharacter)return (viewState?.creation?.drafts||[]).length?'CRIANDO PERSONAGEM':'CRIAR PERSONAGEM';return phaseLabels[phase()]||'EM AVENTURA'}
function profileLabel(){const p=viewState?.campaign?.profile||'OSE_CLASSIC_FANTASY';return p.includes('ADVANCED')?'FANTASIA AVANÇADA':'FANTASIA CLÁSSICA'}
function classLabel(id){return classLabels[id]||String(id||'').replaceAll('_',' ')}
function raceLabel(id){return raceLabels[id]||String(id||'').replaceAll('_',' ')}
function updateHeader(){
 const titles={session:'SESSÃO',map:'MAPA',sheet:'FICHA',dice:'DADOS',library:'BIBLIOTECA',world:'MUNDO VIVO',company:'COMPANHIA'};
 el('headerTitle').textContent=`${titles[screen]} · BRASEIRO OSE`;
 const tm=viewState?.time||{};el('timeStatus').textContent=`DIA ${(tm.days??0)+1} · T${tm.turns??0}`;
 el('phaseStatus').textContent=phaseLabel();el('torchStatus').textContent=resource('torch','0');el('rationStatus').textContent=resource('rations','0');
 document.querySelectorAll('.navbtn').forEach(b=>b.classList.toggle('active',b.dataset.screen===screen));
}
function go(s){screen=s;q.set('screen',s);try{history.replaceState(null,'',`${location.pathname}?${q}`)}catch(_e){}render()}
function render(){updateHeader();main.innerHTML=screen==='session'?renderSession():screen==='map'?renderMap():screen==='sheet'?renderSheet():screen==='dice'?renderDiceScreen():screen==='library'?renderLibrary():screen==='world'?renderWorld():renderCompany();wireCurrentScreen();if(screen==='library'&&hasNative()&&!ruleLibraryLoaded)postNative('RuleEvidenceList',{});}

function renderSession(){
 if(hasNative()&&!viewState?.selectedCharacter&&phase()==='ACTIVE'&&!(viewState?.creation?.drafts||[]).length)return renderCreationStart();
 if(hasNative()&&!viewState?.selectedCharacter&&(viewState?.creation?.drafts||[]).length)return renderCreationDraft(viewState.creation.drafts[0]);
 if(phase()==='CHARACTER_LOADOUT'&&hasNative())return `<div class="kicker">Criação ainda não concluída</div><div class="panel"><h2>${esc(viewState?.selectedCharacter?.name||'Aventureiro')}</h2><p>Antes de começar, escolha o equipamento inicial na ficha do personagem.</p><button class="primary" data-go="sheet">Abrir equipamento inicial</button></div>`;
 if(hasNative()&&viewState?.selectedCharacter&&(phase()==='READY_TO_START'||!String(viewState?.session?.visibleNarration||'').trim()))return renderPrestart();
 const narration=viewState?.session?.visibleNarration||fallbackNarration[stateName]||fallbackNarration.active;
 const sceneUrl=viewState?.presentation?.sceneImageUrl||'';
 const sceneId=viewState?.presentation?.sceneId||'';
 const raw=viewState?.sceneDecisions||[];
 const ds=raw.length===3?raw:(fallbackDecisions[stateName]||fallbackDecisions.active);
 const presentNpcs=viewState?.presentation?.presentNpcs||[];
 const npcPanel=presentNpcs.length?`<div class="panel"><h3>NPCs presentes</h3><div class="text-entity-list">${presentNpcs.map(n=>`<div class="text-entity"><b>${esc(n.name)}</b></div>`).join('')}</div></div>`:'';
 return `<div class="kicker">Sessão ativa</div>
  <div class="panel"><div class="row"><div><span class="pill">${profileLabel()}</span><h2 style="margin-top:7px">Cena atual</h2></div></div>
  ${sceneUrl?img(sceneUrl,'Ilustração da cena atual','scene-image'):''}
  <div id="visibleNarration" class="narrative-text">${paragraphs(narration)}</div>
  <div class="session-tools"><button class="small" data-tts="play">▶ OUVIR</button><button class="small" data-tts="stop">■ PARAR</button><button class="small" data-tts="replay">↻ REPETIR</button><button class="small" data-scene-prompt>▧ GERAR CENA</button><button class="small" data-scene-import data-scene-id="${esc(sceneId)}">⇩ IMPORTAR</button></div></div>
  ${npcPanel}
  ${renderRuleTrace(lastRuleTrace||viewState?.lastRuleTrace)}
  <div class="panel"><h3>O que você faz?</h3><div class="suggestions">${ds.map(x=>`<button class="suggestion" data-suggest="${esc(x.reaction||x[1]||'')}" data-intent="${esc(x.mechanicalIntent||'')}">${esc(x.label||x[0]||'AÇÃO')}</button>`).join('')}</div><textarea id="reactionInput" placeholder="Ex.: examino as pegadas, pergunto o que há por perto, sigo com cuidado..."></textarea><button class="primary" data-react>Enviar ação</button><button class="secondary" style="margin-top:7px" data-go="dice">Abrir rolagem de dados</button></div>
  <div class="panel"><h3>Diário</h3><div class="log friendly-log">${esc(localLog.slice(-8).join('\n')||'Sem eventos registrados.')}</div></div>`;
}
function paragraphs(text){return String(text||'').split(/\n\s*\n/).filter(Boolean).map(p=>`<p>${esc(p)}</p>`).join('')}
function traceStatusLabel(s){const m={CANONICAL_RULE:'Regra canônica',CANONICAL_PROCEDURE:'Procedimento canônico',OPTIONAL_RULE:'Regra opcional',PRODUCT_PRESET:'Contrato do produto',PROCEDURAL_TABLE:'Tabela/procedimento',ADVENTURE_LOCAL_RULE:'Regra da aventura',REFEREE_JUDGMENT:'Julgamento do Mestre',MISSING_EVIDENCE:'Evidência insuficiente'};return m[s]||String(s||'Sem rastreio')}
function renderRuleTrace(t){if(!t)return `<div class="panel rule-trace"><h3>Regras consultadas</h3><p class="note">Nenhuma resolução mecânica foi executada nesta cena ainda.</p></div>`;const ev=t.evidence||[];return `<details class="panel rule-trace" open><summary><b>Regras consultadas nesta ação</b> <span class="trace-status ${esc(String(t.status||'').toLowerCase())}">${esc(traceStatusLabel(t.status))}</span></summary>${ev.length?`<div class="trace-list">${ev.map(e=>`<button class="trace-row" data-rule-id="${esc(e.ruleId)}"><b>${esc(e.ruleId)}</b><span>${esc(e.source||'Fonte')} · ${esc(e.location||'')}</span></button>`).join('')}</div>`:'<p class="note">Nenhuma regra mecânica específica foi aplicada; a resposta ficou no âmbito narrativo.</p>'}${t.note?`<p class="trace-note">${esc(t.note)}</p>`:''}</details>`}
function showRuleDetail(p){const t=p.trace||{};const ev=t.evidence||[];const msg=ev.length?`${p.ruleId}: ${ev.map(e=>`${e.source} — ${e.location}`).join(' | ')}`:`${p.ruleId}: ${traceStatusLabel(t.status)}`;showToast(msg)}

function renderPrestart(){return `<div class="kicker">Pronto para começar</div><div class="panel prestart-card"><span class="pill">${profileLabel()}</span><h2>A aventura ainda não começou</h2><p>Seu personagem e o mundo já estão preparados. Quando você tocar no botão abaixo, Barbara apresentará a cena inicial usando o estado atual da campanha.</p><button class="primary start-adventure" data-start-adventure>COMEÇAR A AVENTURA</button><button class="secondary" style="margin-top:7px" data-go="dice">ROLAR DADOS</button></div>`}
function renderCreationStart(){return `<div class="kicker">Primeiro aventureiro</div><div class="panel"><h2>Criar personagem</h2><p>Os seis atributos serão rolados pelo próprio aplicativo conforme as regras OSE e ficarão registrados na campanha.</p><span class="pill">${profileLabel()}</span><button class="primary" style="margin-top:10px" data-start-draft>CRIAR PASSO A PASSO · ROLAR 3D6</button><button class="secondary" style="margin-top:7px" data-random-character>CRIAR PERSONAGEM ALEATÓRIO</button><button class="secondary" style="margin-top:7px" data-import-character>IMPORTAR FICHA.JSON</button></div>`}
function renderCreationDraft(d){
 const a=d.attributes||{};const isAdv=String(d.profile||'').includes('ADVANCED');
 const classes=isAdv?['ACROBAT','ASSASSIN','BARBARIAN','BARD','CLERIC','DRUID','FIGHTER','ILLUSIONIST','KNIGHT','MAGIC_USER','PALADIN','RANGER','THIEF']:['DWARF','CLERIC','ELF','FIGHTER','HALFLING','THIEF','MAGIC_USER'];
 const races=['HUMAN','DROW','DUERGAR','DWARF','ELF','GNOME','HALF_ELF','HALFLING','HALF_ORC','SVIRFNEBLIN'];
 return `<div class="kicker">Primeiro aventureiro</div><div class="panel"><h3>Criar personagem</h3><div class="attrs">${Object.entries({FOR:a.str,INT:a.int,SAB:a.wis,DES:a.dex,CON:a.con,CAR:a.cha}).map(([k,v])=>`<div class="attr"><b>${v??'—'}</b><span>${k}</span></div>`).join('')}</div>
 ${isAdv?`<label class="field">Método de criação</label><select id="creationMethod"><option value="ADVANCED_BASIC">Básico</option><option value="ADVANCED_METHOD">Avançado</option></select><label class="field">Raça (método avançado)</label><select id="creationRace"><option value="">Nenhuma / conforme método</option>${races.map(x=>`<option value="${x}">${raceLabel(x)}</option>`).join('')}</select>`:''}
 <label class="field">Classe</label><select id="creationClass">${classes.map(x=>`<option value="${x}">${classLabel(x)}</option>`).join('')}</select><label class="field">Nome</label><input id="creationName" placeholder="Nome do aventureiro"><button class="primary" style="margin-top:9px" data-commit-draft="${esc(d.draftId)}">Criar personagem</button></div>`;
}

const geom=Object.freeze({hexW:64,hexH:73.90,xStep:62.8,yStep:54.8,oddOffset:31.4,originX:24,originY:28});
function terrainAsset(c){if(!c||!c.known||c.terrain==='UNKNOWN')return null;if(c.hydrology==='SEA_OCEAN'||c.hydrology==='LAKE_RIVER')return 'agua.png';const m={CLEAR_GRASSLANDS:'planicie.png',HILLS:'colinas.png',MOUNTAINS:'montanha.png',FOREST:'floresta.png',DESERT:'planicie.png',SWAMP:'pantano.png',JUNGLE:'floresta.png',BARREN_ADV:'planicie.png',BROKEN_LANDS_ADV:'colinas.png'};return m[c.terrain]||'planicie.png'}
function terrainName(c){if(!c||!c.known||c.terrain==='UNKNOWN')return 'Área desconhecida';if(c.hydrology==='SEA_OCEAN')return 'Mar';if(c.hydrology==='LAKE_RIVER')return 'Rio ou lago';return terrainLabels[c.terrain]||'Terreno'}
function currentMapCell(){const p=viewState?.position,m=viewState?.map?.cells||[];return m.find(c=>c.q===p?.q&&c.r===p?.r)||null}
function renderMap(){
 const cells=viewState?.map?.cells||[];const pos=viewState?.position||{q:0,r:0};const selected=mapSelection?cells.find(c=>c.q===mapSelection.q&&c.r===mapSelection.r):currentMapCell();
 const width=viewState?.map?.width||6,height=viewState?.map?.height||5;
 const cellHtml=c=>{const x=geom.originX+c.q*geom.xStep+(c.r%2?geom.oddOffset:0),y=geom.originY+c.r*geom.yStep,a=terrainAsset(c),sel=selected&&selected.q===c.q&&selected.r===c.r,dq=c.q-pos.q,dr=c.r-pos.r,frontier=!c.known&&['1,0','0,1','1,-1','-1,0','-1,1','0,-1'].includes(`${dq},${dr}`);return `<button class="hex-cell ${c.known?'known':'unknown'} ${frontier?'frontier':''} ${sel?'selected':''}" data-map-q="${c.q}" data-map-r="${c.r}" aria-label="${esc(terrainName(c))}" style="left:${x}px;top:${y}px;${a?`background-image:url('hexes-runtime/${a}')`:''}">${frontier?'·':''}</button>`};
 const all=(cells.length?cells:Array.from({length:30},(_,i)=>({q:i%6,r:Math.floor(i/6),known:false,terrain:'UNKNOWN',hydrology:'UNKNOWN'}))).map(cellHtml).join('');
 const pq=Number.isFinite(pos.q)?pos.q:0,pr=Number.isFinite(pos.r)?pos.r:0,px=geom.originX+pq*geom.xStep+(pr%2?geom.oddOffset:0)+16,py=geom.originY+pr*geom.yStep+17;
 const travel=selected?travelForSelection(pos,selected):null;
 const gridW=Math.ceil(geom.originX*2+(Math.max(1,width)-1)*geom.xStep+geom.oddOffset+geom.hexW+92),gridH=Math.ceil(geom.originY*2+(Math.max(1,height)-1)*geom.yStep+geom.hexH+28);
 const knownText=selected?.known?`Terreno conhecido: ${terrainName(selected)}.`:'Esta área ainda não foi revelada à companhia.';
 return `<section class="map-shell"><div class="map-head"><div><div class="kicker">Mapa da campanha</div><div class="map-title">Ermos do Norte</div></div><div class="camera-tools"><button id="zoomOut">−</button><button id="zoomIn">+</button></div></div><div id="mapViewport" class="map-viewport" aria-label="Mapa da campanha"><div id="mapGrid" class="map-grid" style="width:${gridW}px;height:${gridH}px">${all}<div class="canonical-ring" style="left:${px-9}px;top:${py-9}px"></div><div class="party-marker" style="left:${px}px;top:${py}px" aria-label="Companhia">◆</div><div class="map-label" style="left:${px+30}px;top:${py+2}px">COMPANHIA</div></div></div><div class="map-inspect panel"><div>${selected&&terrainAsset(selected)?img('hexes-runtime/'+terrainAsset(selected),terrainName(selected),'inspect-hex'):'<div class="unknown-inspect">✦</div>'}</div><div><div class="inspect-title">${esc(selected?terrainName(selected):'Selecione um hex')}</div><div class="inspect-copy">${esc(knownText)}</div>${travel?`<button class="small map-travel" data-map-travel="${esc(travel.intent)}" data-map-text="${esc(travel.text)}">Preparar dia de viagem para ${travel.label}</button>`:'<div class="inspect-copy">Selecione um hex vizinho à companhia para preparar a viagem.</div>'}</div></div></section>`
}
function travelForSelection(pos,c){if(pos?.kind!=='HEX')return null;const dq=c.q-pos.q,dr=c.r-pos.r;const m={'1,0':['leste','WILD_TRAVEL_DAY:E'],'0,1':['sudeste','WILD_TRAVEL_DAY:SE'],'1,-1':['nordeste','WILD_TRAVEL_DAY:NE'],'-1,0':['oeste','WILD_TRAVEL_DAY:W'],'-1,1':['sudoeste','WILD_TRAVEL_DAY:SW'],'0,-1':['noroeste','WILD_TRAVEL_DAY:NW']};const x=m[`${dq},${dr}`];return x?{label:x[0],intent:x[1],text:`Viajamos para ${x[0]} com cautela, observando o terreno durante o caminho.`}:null}

function hpText(c){if(!c)return'—';const d=c.hpDenominator||1,cur=c.hpCurrentNumerator??0,max=c.hpMaxNumerator??0;return d===1?`${cur}/${max}`:`${cur}/${d} / ${max}/${d}`}
function attrPairs(a={}){return [['FOR',a.str],['INT',a.int],['SAB',a.wis],['DES',a.dex],['CON',a.con],['CAR',a.cha]]}
function canRecreateCharacter(){return !!viewState?.selectedCharacter && !String(viewState?.session?.visibleNarration||'').trim() && Number(viewState?.time?.turns||0)===0}
function renderSheet(){
 const c=viewState?.selectedCharacter;
 if(hasNative()&&!c){
  const draft=(viewState?.creation?.drafts||[])[0];
  return `<div class="kicker">Ficha do personagem</div>${draft?renderCreationDraft(draft):renderCreationStart()}`;
 }
 const demo=c||{characterId:'demo-character',name:'Aventureiro',raceId:'HUMAN',classIds:['FIGHTER'],levelByClass:{FIGHTER:1},hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1,armorClassDescending:9,attributes:{str:11,int:9,wis:8,dex:10,con:6,cha:9}};
 const inv=viewState?.inventory||(!hasNative()?[{itemId:'demo-mace',catalogId:'ADV_EQUIP_WEAPON_PURCHASE_09',quantity:1,equipped:false,kind:'WEAPON'}]:[]);
 const editPanel=editingCharacter?`<div class="panel edit-character"><h3>Editar ficha</h3><label class="field">Nome do personagem</label><input id="editCharacterName" maxlength="80" value="${esc(demo.name||'')}"><p class="note">O nome pode ser alterado a qualquer momento. Raça, classe, atributos, HP inicial e valores derivados continuam protegidos pelas regras OSE. Antes da primeira cena, use “Criar outro personagem” para refazer a parte mecânica.</p><div class="row"><button class="primary" data-save-character>Salvar</button><button class="secondary" data-cancel-character-edit>Cancelar</button></div></div>`:'';
 const recreate=canRecreateCharacter()?`<button class="secondary" data-recreate-character>CRIAR OUTRO PERSONAGEM</button>`:'';
 return `<div class="hero panel text-first-hero"><div><div class="kicker">Personagem</div><h2>${esc(demo.name||'Aventureiro')}</h2><p><b>${esc(raceLabel(demo.raceId||'HUMAN'))}</b> · ${esc((demo.classIds||[]).map(classLabel).join(' / ')||'—')} · ${profileLabel()}</p><div class="vitals"><div class="vital"><span>HP</span><b>${hpText(demo)}</b></div><div class="vital"><span>CA</span><b>${demo.armorClassDescending??'—'}</b></div><div class="vital"><span>NÍVEL</span><b>${Object.values(demo.levelByClass||{})[0]??1}</b></div></div></div></div>
 ${editPanel}
 <div class="panel"><div class="attrs">${attrPairs(demo.attributes).map(([k,v])=>`<div class="attr"><b>${v??'—'}</b><span>${k}</span></div>`).join('')}</div><div class="sheet-actions"><button class="small" data-edit-character>EDITAR FICHA</button>${recreate}<button class="small" data-export-character>EXPORTAR FICHA.JSON</button><button class="small" data-import-character>IMPORTAR FICHA.JSON</button></div></div>
 <div class="panel"><h3>Inventário</h3><div class="inventory-grid">${inv.length?inv.map(renderInventoryItem).join(''):'<p class="note">Inventário vazio.</p>'}</div></div>${hasNative()?renderNativeLoadout():renderDemoShop()}`;
}
function loadoutState(){return viewState?.loadout&&viewState.loadout.characterId?viewState.loadout:null}
function catalogEntry(id){return loadoutState()?.catalog?.find(x=>x.catalogId===id)||null}
function catalogLabel(id){const e=catalogEntry(id);if(e?.name)return e.name;return 'Item'}
function itemArt(catalogId,name='',kind=''){if(/^(CLASSIC|ADV)_EQUIP_/.test(String(catalogId||'')))return img(`equipment-icons/${esc(catalogId)}.png`,name,'item-thumb equipment-art');const p=kind==='SHIELD'?'tesouros_escudos/escudo_redondo.png':kind==='ARMOR'?'equipamentos/cota_malha.png':'equipamentos_variantes/equipamentos_variantes_01.png';return img(asset(p),name,'item-thumb')}
function renderInventoryItem(it){const name=catalogLabel(it.catalogId),custom=viewState?.presentation?.itemThumbnails?.[it.itemId],canEquip=['WEAPON','ARMOR','SHIELD'].includes(it.kind)&&phase()==='CHARACTER_LOADOUT'&&loadoutState()&&!loadoutState().completed;return `<div class="item">${custom?img(custom,name,'item-thumb'):itemArt(it.catalogId,name,it.kind)}<b>${esc(name)}</b><small>×${it.quantity??1}${it.equipped?' · EQUIPADO':''}</small>${hasNative()?`<div class="actions">${canEquip?`<button data-loadout-equip="${esc(it.itemId)}" data-equipped="${it.equipped?'1':'0'}">${it.equipped?'TIRAR':'EQUIPAR'}</button>`:''}<button class="photo-action" title="Trocar imagem" data-photo-item="${esc(it.itemId)}">📷</button></div>`:''}</div>`}
function renderNativeLoadout(){const l=loadoutState();if(!hasNative()||phase()!=='CHARACTER_LOADOUT'||!l||l.completed)return'';const groups=[['Aventura',['ADVENTURING_GEAR','FOOD','LIGHT_SOURCE','OIL_FLASK']],['Armas',['WEAPON']],['Armaduras e munição',['ARMOR','SHIELD','AMMUNITION']]];return `<div class="panel"><div class="kicker">Equipamento inicial</div><h3>${l.remainingWealthGp} po restantes de ${l.startingWealthGp} po</h3><p class="note">Os preços, quantidades e estatísticas são aplicados automaticamente pelas regras OSE.</p>${groups.map(([title,kinds])=>`<h3>${title}</h3><div class="inventory-grid">${(l.catalog||[]).filter(e=>kinds.includes(e.kind)).map(e=>`<div class="item">${itemArt(e.catalogId,e.name,e.kind)}<b>${esc(e.name)}</b><small>${e.priceGp==null?'Preço não comprovado':`${e.priceGp} po`}${e.purchaseQuantity>1?` · ×${e.purchaseQuantity}`:''}${e.ownedQuantity?` · possui ${e.ownedQuantity}`:''}</small><div class="actions"><button data-loadout-buy="${esc(e.catalogId)}" ${e.priceGp==null||e.priceGp>l.remainingWealthGp?'disabled':''}>COMPRAR</button></div></div>`).join('')}</div>`).join('')}<button class="primary" data-loadout-finish="1" style="margin-top:9px">Concluir equipamento</button></div>`}
function renderDemoShop(){return ''}
function renderCompany(){const party=viewState?.party||(!hasNative()?[{characterId:'demo-character',name:'Aventureiro',classIds:['FIGHTER'],hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1}]:[]);return `<div class="kicker">Companhia</div><div class="panel"><h2>${party.length?'Companhia atual':'Ainda vazia'}</h2>${party.length?`<div class="text-entity-list">${party.map((m,i)=>`<div class="text-entity ${i===0?'active':''}"><b>${esc(m.name)}</b><span>${esc((m.classIds||[]).map(classLabel).join(' / '))} · HP ${hpText(m)}</span></div>`).join('')}<div class="text-entity"><span>Espaço para próximo aventureiro</span></div></div><p class="note">A companhia viaja e ocupa o mundo como um único grupo.</p>`:'<p>Crie seu primeiro personagem para formar a companhia.</p>'}</div>`}

function renderDicePanel(){return `<div class="panel dice-panel"><div class="row"><h3>🎲 Dados</h3><span class="note">Rolagem manual; não altera a campanha sozinha.</span></div><div class="dice-buttons">${[4,6,8,10,12,20,100].map(s=>`<button class="small" data-die="${s}">d${s}</button>`).join('')}</div><div class="dice-controls"><label>Quantidade<input id="diceCount" type="number" min="1" max="20" value="1"></label><label>Modificador<input id="diceModifier" type="number" min="-100" max="100" value="0"></label></div>${diceHistory.length?`<div class="dice-history">${diceHistory.map(d=>`<div><b>${d.count}d${d.sides}${d.modifier?`${d.modifier>0?'+':''}${d.modifier}`:''} = ${d.total}</b><small>${esc((d.rolls||[]).join(', '))}</small></div>`).join('')}</div>`:''}</div>`}
function renderDiceScreen(){return `<div class="kicker">Rolagem de dados</div>${renderDicePanel()}<div class="panel"><p class="note">Rolagens manuais são livres e não alteram a campanha sozinhas. Quando uma regra OSE exige um teste, o motor usa a rolagem autoritativa e registra a regra consultada na sessão.</p></div>`}
function renderLibrary(){const lib=viewState?.libraryStatus||{};const qx=libraryQuery.trim().toLowerCase();const filtered=ruleLibrary.filter(r=>!qx||String(r.ruleId).toLowerCase().includes(qx)||JSON.stringify(r.trace||{}).toLowerCase().includes(qx));return `<div class="kicker">Biblioteca de regras</div><div class="panel"><h2>${profileLabel()}</h2><p class="note">Autoridade: ${esc(lib.authority||'Fonte OSE incorporada')} · ${lib.ruleCount||ruleLibrary.length||0} regras catalogadas.</p><input id="librarySearch" type="search" placeholder="Buscar regra, fonte ou página" value="${esc(libraryQuery)}"></div><div class="rule-library">${ruleLibraryLoaded?(filtered.slice(0,100).map(r=>{const t=r.trace||{},e=(t.evidence||[])[0]||{};return `<button class="panel library-rule" data-rule-id="${esc(r.ruleId)}"><b>${esc(r.ruleId)}</b><span>${esc(traceStatusLabel(t.status))}</span><small>${esc(e.source||'')} ${esc(e.location||'')}</small></button>`}).join('')||'<div class="panel"><p>Nenhuma regra encontrada.</p></div>'):'<div class="panel"><p>Carregando catálogo de regras…</p></div>'}</div>`}
function renderWorld(){const w=viewState?.worldStatus||{},p=viewState?.position||{},cell=currentMapCell();const weather={CLEAR:'Céu limpo',OVERCAST:'Encoberto',RAIN:'Chuva',FOG:'Neblina',STORM:'Tempestade',COLD:'Frio intenso',HOT:'Calor forte'}[w.weather]||w.weather||'—';return `<div class="kicker">Mundo Vivo</div><div class="panel"><h2>${w.active?'Mundo ativo':'Mundo ainda não iniciado'}</h2><div class="world-grid"><div><span>Clima</span><b>${esc(weather)}</b></div><div><span>Pulsos autônomos</span><b>${w.worldPulseCount??0}</b></div><div><span>NPCs persistentes</span><b>${w.npcCount??0}</b></div><div><span>Atividade de NPCs</span><b>${w.npcActivityCount??0}</b></div><div><span>Hexes conhecidos</span><b>${w.knownHexes??0}</b></div><div><span>Eventos pendentes</span><b>${w.scheduledEventCount??0}</b></div><div><span>Eventos resolvidos</span><b>${w.resolvedEventCount??0}</b></div><div><span>Revisão do mundo</span><b>${w.revision??0}</b></div></div><p class="note">O mundo e as rotinas dos NPCs avançam automaticamente quando o tempo canônico passa. Clima e atividade de fundo são persistidos sem inventar modificadores de regra.</p></div><div class="panel"><h3>Posição atual</h3><p>${esc(cell?terrainName(cell):'Local atual')} · Dia ${(viewState?.time?.days??0)+1}, turno ${viewState?.time?.turns??0}</p><button class="secondary" data-go="map">ABRIR MAPA</button></div>${renderRuleTrace(lastRuleTrace||viewState?.lastRuleTrace)}`}


function wireCurrentScreen(){
 document.querySelectorAll('[data-go]').forEach(b=>b.onclick=()=>go(b.dataset.go));
 document.querySelector('[data-start-adventure]')?.addEventListener('click',()=>{if(postNative('StartAdventure',{}))showToast('Barbara está preparando a cena inicial…')});
 document.querySelectorAll('[data-suggest]').forEach(b=>b.onclick=()=>{const t=el('reactionInput');if(t){t.value=b.dataset.suggest;t.dataset.mechanicalIntent=b.dataset.intent||'';t.focus()}});
 el('reactionInput')?.addEventListener('input',e=>{if(e.isTrusted)delete e.currentTarget.dataset.mechanicalIntent});
 document.querySelector('[data-react]')?.addEventListener('click',()=>{const t=el('reactionInput'),v=t?.value.trim();if(!v)return showToast('Escreva o que seu personagem faz ou pergunta.');const mechanicalIntent=t.dataset.mechanicalIntent||'';const execute=()=>{if(postNative('PlayerReaction',{reactionId:'ui-'+Date.now(),text:v,mechanicalIntent}))showToast('Enviado ao Mestre.');else{localLog.push(v);showToast('Preview sem motor nativo.')}};const warning=actionCostSummary(mechanicalIntent);if(warning&&mechanicalIntent.startsWith('WILD_'))return confirmWildernessAction(warning,execute);execute()});
 document.querySelectorAll('[data-tts]').forEach(b=>b.onclick=()=>{if(!postNative('TtsCommand',{command:b.dataset.tts}))showToast('A narração em voz requer o app Android.');});
 document.querySelector('[data-scene-prompt]')?.addEventListener('click',()=>{if(postNative('ScenePromptShare',{explicitUserAction:true}))showToast('Abrindo o Gemini com o prompt da cena…');else showToast('Geração externa requer o app Android.')});
 document.querySelector('[data-scene-import]')?.addEventListener('click',b=>chooseMedia('SCENE',b.currentTarget.dataset.sceneId));
 document.querySelectorAll('[data-die]').forEach(b=>b.onclick=()=>{const count=Math.max(1,Math.min(20,Number(el('diceCount')?.value||1))),modifier=Math.max(-100,Math.min(100,Number(el('diceModifier')?.value||0)));if(!postNative('ManualDiceRoll',{sides:Number(b.dataset.die),count,modifier})){const rolls=Array.from({length:count},()=>1+Math.floor(Math.random()*Number(b.dataset.die))),total=rolls.reduce((a,x)=>a+x,0)+modifier;diceHistory.unshift({rolls,total,sides:Number(b.dataset.die),count,modifier});render()}});
 document.querySelector('[data-start-draft]')?.addEventListener('click',()=>postNative('CharacterCreationStart',{draftId:'draft-'+Date.now()}));
 document.querySelector('[data-random-character]')?.addEventListener('click',()=>{if(postNative('CharacterCreationRandom',{}))showToast('Criando personagem aleatório pelas regras…')});
 document.querySelectorAll('[data-export-character]').forEach(b=>b.onclick=()=>postNative('CharacterSheetExportRequest',{}));
 document.querySelectorAll('[data-import-character]').forEach(b=>b.onclick=()=>postNative('CharacterSheetImportRequest',{}));
 document.querySelectorAll('[data-edit-character]').forEach(b=>b.onclick=()=>{editingCharacter=true;render()});
 document.querySelectorAll('[data-cancel-character-edit]').forEach(b=>b.onclick=()=>{editingCharacter=false;render()});
 document.querySelectorAll('[data-save-character]').forEach(b=>b.onclick=()=>{const name=el('editCharacterName')?.value.trim();if(!name)return showToast('Digite um nome válido.');postNative('CharacterSheetUpdate',{name})});
 document.querySelectorAll('[data-recreate-character]').forEach(b=>b.onclick=()=>{if(!confirm('CRIAR OUTRO PERSONAGEM?\n\nA ficha atual, o equipamento inicial e o inventário deste personagem serão removidos. O mundo e as configurações serão mantidos.'))return;postNative('CharacterRecreateBeforeStart',{})});
 document.querySelectorAll('[data-rule-id]').forEach(b=>b.onclick=()=>postNative('RuleEvidenceQuery',{ruleId:b.dataset.ruleId}));
 el('librarySearch')?.addEventListener('input',e=>{libraryQuery=e.currentTarget.value;render()});
 document.querySelector('[data-commit-draft]')?.addEventListener('click',e=>{const d=e.currentTarget.dataset.commitDraft,name=el('creationName')?.value.trim();if(!name)return showToast('Digite um nome.');const adv=String(viewState?.campaign?.profile||'').includes('ADVANCED');const method=adv?(el('creationMethod')?.value||'ADVANCED_BASIC'):'CLASSIC';const race=adv&&method==='ADVANCED_METHOD'?(el('creationRace')?.value||null):null;postNative('CharacterCreationCommit',{draftId:d,characterId:'char-'+Date.now(),name,method,classIds:[el('creationClass').value],raceId:race||'',adjustments:[]})});
 document.querySelectorAll('[data-photo-item]').forEach(b=>b.onclick=()=>chooseMedia('ITEM',b.dataset.photoItem));
 document.querySelectorAll('[data-loadout-buy]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_purchase',commandId:'buy-'+Date.now()+'-'+b.dataset.loadoutBuy,characterId:loadoutState()?.characterId||'',catalogId:b.dataset.loadoutBuy}));
 document.querySelectorAll('[data-loadout-equip]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_equip',commandId:'equip-'+Date.now()+'-'+b.dataset.loadoutEquip,characterId:loadoutState()?.characterId||'',itemId:b.dataset.loadoutEquip,equipped:b.dataset.equipped!=='1'}));
 document.querySelector('[data-loadout-finish]')?.addEventListener('click',()=>postNative('UiCommand',{command:'loadout_finish',commandId:'finish-'+Date.now(),characterId:loadoutState()?.characterId||''}));
 if(screen==='map')wireMap();
}
function wireMap(){
 let cam={x:0,y:0,s:1},drag=null;const grid=el('mapGrid'),vp=el('mapViewport');if(!grid||!vp)return;
 const apply=()=>grid.style.transform=`translate(${cam.x}px,${cam.y}px) scale(${cam.s})`;
 const fit=()=>{
  const nodes=[...grid.querySelectorAll('.hex-cell.known,.hex-cell.frontier')];
  const xs=nodes.map(n=>parseFloat(n.style.left)||0),ys=nodes.map(n=>parseFloat(n.style.top)||0);
  const pos=viewState?.position||{};if(pos.kind==='HEX'){xs.push(geom.originX+pos.q*geom.xStep+(pos.r%2?geom.oddOffset:0));ys.push(geom.originY+pos.r*geom.yStep)}
  if(!xs.length){cam={x:8,y:8,s:1};apply();return}
  const minX=Math.min(...xs)-8,maxX=Math.max(...xs)+geom.hexW+62,minY=Math.min(...ys)-8,maxY=Math.max(...ys)+geom.hexH+8;
  const bw=Math.max(1,maxX-minX),bh=Math.max(1,maxY-minY),pad=14;
  cam.s=Math.max(.64,Math.min(1.12,(vp.clientWidth-pad*2)/bw,(vp.clientHeight-pad*2)/bh));
  cam.x=(vp.clientWidth-bw*cam.s)/2-minX*cam.s;cam.y=(vp.clientHeight-bh*cam.s)/2-minY*cam.s;apply();
 };
 requestAnimationFrame(fit);
 el('zoomIn').onclick=()=>{cam.s=Math.min(1.55,+(cam.s+.1).toFixed(2));apply()};el('zoomOut').onclick=()=>{cam.s=Math.max(.55,+(cam.s-.1).toFixed(2));apply()};
 vp.onpointerdown=e=>{if(e.target.closest('.hex-cell'))return;drag={x:e.clientX,y:e.clientY,cx:cam.x,cy:cam.y};vp.setPointerCapture(e.pointerId)};
 vp.onpointermove=e=>{if(!drag)return;cam.x=drag.cx+e.clientX-drag.x;cam.y=drag.cy+e.clientY-drag.y;apply()};vp.onpointerup=vp.onpointercancel=()=>drag=null;
 document.querySelectorAll('[data-map-q]').forEach(b=>b.onclick=e=>{e.stopPropagation();mapSelection={q:Number(b.dataset.mapQ),r:Number(b.dataset.mapR)};render()});
 document.querySelector('[data-map-travel]')?.addEventListener('click',b=>{const text=b.currentTarget.dataset.mapText,intent=b.currentTarget.dataset.mapTravel;go('session');setTimeout(()=>{const t=el('reactionInput');if(t){t.value=text;t.dataset.mechanicalIntent=intent;t.focus()}},0)})
}
function chooseMedia(scope,id){mediaTarget={scope,id};el('mediaPicker').value='';el('mediaPicker').click()}
el('mediaPicker').addEventListener('change',()=>{const f=el('mediaPicker').files?.[0];if(!f||!mediaTarget)return;if(f.size>5*1024*1024)return showToast('Imagem deve ter no máximo 5 MB.');const r=new FileReader();r.onload=()=>postNative('PresentationImageSave',{scope:mediaTarget.scope,id:mediaTarget.id,dataUrl:r.result});r.readAsDataURL(f)});

document.querySelectorAll('.navbtn').forEach(b=>b.onclick=()=>go(b.dataset.screen));
el('settingsButton').onclick=()=>{el('settingsOverlay').classList.remove('hidden');postNative('GeminiSettingsGet',{});postNative('TtsSettingsGet',{});updateSettingsStatus();updateTtsSettings()};el('settingsClose').onclick=()=>el('settingsOverlay').classList.add('hidden');
function updateSettingsStatus(){if(!el('textSceneStatus'))return;el('textSceneStatus').textContent=geminiState.textSceneConfigured?'Chave configurada':'Chave não configurada';if(geminiState.textModel&&!el('textModel').dataset.dirty)el('textModel').value=geminiState.textModel;if(geminiState.imageModel&&!el('imageModel').dataset.dirty)el('imageModel').value=geminiState.imageModel;const ws=viewState?.worldStatus||{};el('worldStatus').textContent=ws.active?'Ativo — mapa, NPCs persistentes e agenda de eventos carregados.':'Ainda não inicializado.';const lib=viewState?.libraryStatus||{};el('libraryStatus').textContent=lib.embedded?`Regras incorporadas: ${profileLabel()}. Autoridade carregada.`:'Biblioteca não carregada.'}
function updateTtsSettings(){
 const engine=el('ttsEngine'),voice=el('ttsVoiceSelect'),rate=el('ttsRate'),rateValue=el('ttsRateValue'),status=el('ttsLocalStatus');if(!engine||!voice)return;
 const selectedEngine=String(ttsState.enginePackage||'');const selectedVoice=String(ttsState.voiceName||'');
 engine.innerHTML=(ttsState.engines||[]).map(e=>`<option value="${esc(e.packageName)}" ${e.packageName===selectedEngine?'selected':''}>${esc(e.label||e.packageName)}${e.preferredKokoro?' · Kokoro':''}</option>`).join('')||`<option value="">Nenhum motor detectado</option>`;
 voice.innerHTML=(ttsState.voices||[]).map(v=>`<option value="${esc(v.name)}" ${v.name===selectedVoice?'selected':''}>${esc(v.name)} · ${esc(v.localeTag||'pt')} ${v.networkRequired?'· rede':'· local'}</option>`).join('')||`<option value="">Nenhuma voz PT detectada</option>`;
 if(rate&&!rate.dataset.dirty)rate.value=String(ttsState.speechRate||1);if(rateValue)rateValue.textContent=Number(rate?.value||ttsState.speechRate||1).toFixed(2).replace('.',',')+'×';
 if(status)status.textContent=ttsState.ready?`${(ttsState.enginePackage||'Motor Android')} · ${(ttsState.voiceName||'voz automática')} · pronto`:(ttsState.error?`Erro: ${ttsState.error}`:'Inicializando motor de voz…');
}
['textModel','imageModel'].forEach(id=>el(id)?.addEventListener('input',e=>e.currentTarget.dataset.dirty='1'));
el('saveModels')?.addEventListener('click',()=>{const payload={textModel:el('textModel').value.trim(),imageModel:el('imageModel').value.trim()};if(postNative('GeminiRuntimeSettingsSet',payload)){['textModel','imageModel'].forEach(id=>delete el(id).dataset.dirty);showToast('Modelos salvos.')}});
document.querySelectorAll('[data-key-save]').forEach(b=>b.onclick=()=>{const input=el('textSceneKey'),key=input.value.trim();if(!key)return showToast('Cole uma chave antes de salvar.');if(postNative('GeminiCredentialSet',{slot:'TEXT_SCENE',key})){input.value='';showToast('Chave salva no armazenamento seguro.')}});
document.querySelectorAll('[data-key-test]').forEach(b=>b.onclick=()=>{if(postNative('GeminiCredentialTest',{slot:'TEXT_SCENE'}))showToast('Testando modelo e conexão…')});
document.querySelectorAll('[data-key-delete]').forEach(b=>b.onclick=()=>postNative('GeminiCredentialDelete',{slot:'TEXT_SCENE'}));
el('ttsRate')?.addEventListener('input',e=>{e.currentTarget.dataset.dirty='1';el('ttsRateValue').textContent=Number(e.currentTarget.value).toFixed(2).replace('.',',')+'×'});
el('ttsEngine')?.addEventListener('change',()=>{postNative('TtsSettingsSet',{enginePackage:el('ttsEngine').value,voiceName:'',speechRate:Number(el('ttsRate')?.value||1)});showToast('Trocando motor de voz…')});
el('saveTts')?.addEventListener('click',()=>{if(postNative('TtsSettingsSet',{enginePackage:el('ttsEngine')?.value||'',voiceName:el('ttsVoiceSelect')?.value||'',speechRate:Number(el('ttsRate')?.value||1)})){delete el('ttsRate').dataset.dirty;showToast('Motor e voz salvos.')}});
el('testTts')?.addEventListener('click',()=>{if(postNative('TtsTest',{}))showToast('Testando a voz selecionada…')});
el('refreshTts')?.addEventListener('click',()=>postNative('TtsSettingsGet',{}));
el('resetAdventure')?.addEventListener('click',()=>{const ok=confirm('RECOMEÇAR A AVENTURA?\n\nO mundo, tempo, mapa revelado e progresso da campanha serão reiniciados. Sua ficha, inventário, equipamento e configurações Gemini serão preservados.');if(!ok)return;if(postNative('ResetAdventure',{})){el('settingsOverlay').classList.add('hidden');showToast('Recomeçando a aventura com o mesmo personagem…')}});
el('backupExport')?.addEventListener('click',()=>postNative('BackupExport',{}));el('backupImport')?.addEventListener('click',()=>{const backup=el('backupText').value.trim();if(!backup)return showToast('Cole um backup antes de restaurar.');postNative('BackupImport',{backup})});el('backupCopy')?.addEventListener('click',async()=>{const text=el('backupText').value||backupBuffer;if(!text)return showToast('Gere um backup primeiro.');try{await navigator.clipboard.writeText(text);showToast('Backup copiado.')}catch(_e){el('backupText').focus();el('backupText').select();document.execCommand('copy');showToast('Backup copiado.')}});
render();document.body.dataset.ready='true';postNative('ViewState',{request:'initial'});
})();
